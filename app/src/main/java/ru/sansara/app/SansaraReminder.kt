package ru.sansara.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun sansaraPostNotification(context:Context,id:Int,notification:android.app.Notification){
    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
    ) return
    NotificationManagerCompat.from(context).notify(id,notification)
}

object SansaraReminderScheduler {
    const val CHANNEL_ID = "sansara_agent_reminders"
    private const val ACTION_REMINDER = "ru.sansara.app.AGENT_REMINDER"

    fun schedule(context: Context, reminder: SansaraAgentReminder) {
        ensureChannel(context)
        val alarmManager=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent=Intent(context,SansaraReminderReceiver::class.java).apply {
            action=ACTION_REMINDER
            putExtra("id",reminder.id)
            putExtra("customerName",reminder.customerName)
            putExtra("note",reminder.note)
        }
        val pending=PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,reminder.remindAtEpochMs,pending)
        }else{
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,reminder.remindAtEpochMs,pending)
        }
    }

    fun ensureChannel(context:Context){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID,"Напоминания SANSARA",NotificationManager.IMPORTANCE_HIGH).apply {
                    description="Напоминания по клиентам агента"
                }
            )
        }
    }
}

class SansaraReminderReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        SansaraReminderScheduler.ensureChannel(context)
        val id=intent.getStringExtra("id").orEmpty()
        val customer=intent.getStringExtra("customerName").orEmpty()
        val note=intent.getStringExtra("note").orEmpty()
        val notification=NotificationCompat.Builder(context,SansaraReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sansara)
            .setContentTitle("SANSARA · "+customer)
            .setContentText(note.ifBlank{"Запланированное напоминание по клиенту"})
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifBlank{"Запланированное напоминание по клиенту"}))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        sansaraPostNotification(context,id.hashCode(),notification)

        val pendingResult=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                SansaraRepository.get(context).disableAgentReminder(id)
            }
            pendingResult.finish()
        }
    }
}

class SansaraBootReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action!=Intent.ACTION_BOOT_COMPLETED)return
        val pendingResult=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val repository=SansaraRepository.get(context)
                repository.activeAgentReminders()
                    .filter{it.remindAtEpochMs>System.currentTimeMillis()}
                    .forEach{SansaraReminderScheduler.schedule(context,it)}
                SansaraAdminOpsReminderScheduler.scheduleAll(context)
            }
            pendingResult.finish()
        }
    }
}


object SansaraAdminOpsReminderScheduler {
    const val CHANNEL_ID = "sansara_admin_operations"
    const val ACTION_CHECK = "ru.sansara.app.ADMIN_OPS_CHECK"
    const val ACTION_EVENING = "ru.sansara.app.ADMIN_EVENING_REPORT"
    private const val REQUEST_CHECK = 8100
    private const val REQUEST_EVENING = 1900

    fun ensureChannel(context:Context){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID,"Контроль администратора",NotificationManager.IMPORTANCE_HIGH).apply {
                    description="Табель, выпуск, приход, задания в цех и вечерний отчёт"
                }
            )
        }
    }

    /** Напоминания «контроль 08:00» и вечерний отчёт нужны только администратору. */
    fun isAdminSession(context:Context):Boolean =
        runCatching { SecureSessionStore(context.applicationContext).load()?.role==SansaraRole.ADMIN }.getOrDefault(false)

    fun isWorkday(date:LocalDate):Boolean =
        date.dayOfWeek!=java.time.DayOfWeek.SATURDAY && date.dayOfWeek!=java.time.DayOfWeek.SUNDAY

    private fun nextWorkdayAt(from:LocalDate,hour:Int):LocalDateTime {
        var day=from
        while(!isWorkday(day)) day=day.plusDays(1)
        return day.atTime(hour,0)
    }

    fun scheduleAll(context:Context){
        if(!isAdminSession(context)){
            cancelAll(context)
            return
        }
        ensureChannel(context)
        scheduleNextCheck(context)
        scheduleEvening(context)
    }

    fun cancelAll(context:Context){
        val alarmManager=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(ACTION_CHECK to REQUEST_CHECK, ACTION_EVENING to REQUEST_EVENING).forEach { (action,code) ->
            alarmManager.cancel(pendingFor(context,action,code))
        }
    }

    fun scheduleNextCheck(context:Context,repeat:Boolean=false){
        val now=LocalDateTime.now()
        val today=now.toLocalDate()
        val target=when{
            !isWorkday(today)->nextWorkdayAt(today,8)
            repeat && now.toLocalTime().isBefore(java.time.LocalTime.of(20,0))->now.plusMinutes(10)
            repeat->nextWorkdayAt(today.plusDays(1),8)
            now.isBefore(today.atTime(8,0))->today.atTime(8,0)
            now.toLocalTime().isBefore(java.time.LocalTime.of(20,0))->now.plusSeconds(15)
            else->nextWorkdayAt(today.plusDays(1),8)
        }
        schedule(context,ACTION_CHECK,REQUEST_CHECK,target)
    }

    fun scheduleTomorrow(context:Context){
        schedule(context,ACTION_CHECK,REQUEST_CHECK,nextWorkdayAt(LocalDate.now().plusDays(1),8))
    }

    fun scheduleEvening(context:Context){
        val now=LocalDateTime.now()
        val today=now.toLocalDate()
        val target=if(isWorkday(today) && now.isBefore(today.atTime(19,0))) today.atTime(19,0) else nextWorkdayAt(today.plusDays(1),19)
        schedule(context,ACTION_EVENING,REQUEST_EVENING,target)
    }

    private fun pendingFor(context:Context,action:String,requestCode:Int):PendingIntent {
        val intent=Intent(context,SansaraAdminOpsReminderReceiver::class.java).apply{this.action=action}
        return PendingIntent.getBroadcast(
            context,requestCode,intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun schedule(context:Context,action:String,requestCode:Int,target:LocalDateTime){
        val alarmManager=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending=pendingFor(context,action,requestCode)
        val epoch=target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,epoch,pending)
        }else{
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,epoch,pending)
        }
    }
}

class SansaraAdminOpsReminderReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        SansaraAdminOpsReminderScheduler.ensureChannel(context)
        val pendingResult=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if(!SansaraAdminOpsReminderScheduler.isAdminSession(context)){
                    SansaraAdminOpsReminderScheduler.cancelAll(context)
                    return@runCatching
                }
                // Дни рождения сборщиц — в любой день, независимо от рабочего календаря.
                runCatching { SansaraNotify.birthdayCheck(context) }
                if(!SansaraAdminOpsReminderScheduler.isWorkday(LocalDate.now())){
                    SansaraAdminOpsReminderScheduler.scheduleAll(context)
                    return@runCatching
                }
                val repository=SansaraRepository.get(context)
                when(intent.action){
                    SansaraAdminOpsReminderScheduler.ACTION_EVENING->{
                        val presence=repository.presenceToday()
                        val unique=presence.map{it.userId}.distinct().size
                        val minutes=presence.sumOf{it.durationMs}/60_000L
                        val notification=NotificationCompat.Builder(context,SansaraAdminOpsReminderScheduler.CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_stat_sansara)
                            .setContentTitle("SANSARA · вечерний отчёт")
                            .setContentText("Сегодня заходили: "+unique+" · время в приложении: "+minutes+" мин.")
                            .setStyle(NotificationCompat.BigTextStyle().bigText("Сегодня заходили: "+unique+" пользователей. Суммарное время в приложении: "+minutes+" минут. Подробности доступны в разделе «Отчёты»."))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .build()
                        sansaraPostNotification(context,1900,notification)
                        SansaraAdminOpsReminderScheduler.scheduleEvening(context)
                    }
                    else->{
                        val date=LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        val daily=repository.adminDailyStatus(date)
                        if(daily?.dayOff==true || !daily?.reason.isNullOrBlank()){
                            SansaraAdminOpsReminderScheduler.scheduleTomorrow(context)
                        }else{
                            val missing=mutableListOf<String>()
                            if(repository.attendance(date).isEmpty())missing+="заполнить табель"
                            if(repository.workshopTasks().none{it.taskDate==date})missing+="отправить задание в цех"
                            val ops=repository.snapshot().productionOps.filter{it.date==date&&it.status!="Удален"}
                            if(ops.isEmpty()){
                                missing+="сделать выпуск"
                                missing+="оприходовать продукцию"
                            }
                            if(missing.isEmpty()){
                                SansaraAdminOpsReminderScheduler.scheduleTomorrow(context)
                            }else{
                                val text="Не выполнено: "+missing.joinToString(", ")
                                val notification=NotificationCompat.Builder(context,SansaraAdminOpsReminderScheduler.CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_stat_sansara)
                                    .setContentTitle("SANSARA · контроль 08:00")
                                    .setContentText(text)
                                    .setStyle(NotificationCompat.BigTextStyle().bigText(text+". Напоминание повторится через 10 минут, пока задачи не будут закрыты."))
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true)
                                    .build()
                                sansaraPostNotification(context,8100,notification)
                                SansaraAdminOpsReminderScheduler.scheduleNextCheck(context,repeat=true)
                            }
                        }
                    }
                }
            }
            pendingResult.finish()
        }
    }
}
