package ru.sansara.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            .setSmallIcon(R.drawable.sansara_app_icon)
            .setContentTitle("SANSARA · "+customer)
            .setContentText(note.ifBlank{"Запланированное напоминание по клиенту"})
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifBlank{"Запланированное напоминание по клиенту"}))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id.hashCode(),notification) }

        val pendingResult=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                SansaraRepository(SansaraDatabase.get(context)).disableAgentReminder(id)
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
                val repository=SansaraRepository(SansaraDatabase.get(context))
                repository.activeAgentReminders()
                    .filter{it.remindAtEpochMs>System.currentTimeMillis()}
                    .forEach{SansaraReminderScheduler.schedule(context,it)}
            }
            pendingResult.finish()
        }
    }
}
