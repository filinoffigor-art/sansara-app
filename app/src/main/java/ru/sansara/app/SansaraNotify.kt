package ru.sansara.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Каждое оповещение приложения дублируется системным уведомлением: всплывает поверх экрана и остаётся в шторке.
 * Каналы раздельные, поэтому любую группу можно отключить в настройках телефона.
 * Без сервера уведомление появляется только на этом телефоне.
 */
object SansaraNotify {
    enum class Channel(val id: String, val title: String, val description: String) {
        ORDERS("sansara_orders", "Заказы", "Новые заказы, статусы, согласование, изменения и доставка"),
        REGISTRATIONS("sansara_registrations", "Заявки", "Заявки на регистрацию партнёров"),
        PAYMENTS("sansara_payments", "Оплаты", "Оплаты по заказам"),
        CHAT("sansara_chat", "Чат", "Сообщения от менеджера и клиентов"),
        OTHER("sansara_other", "Прочее", "Остальные оповещения SANSARA")
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Channel.entries.forEach { ch ->
            manager.createNotificationChannel(NotificationChannel(ch.id, ch.title, NotificationManager.IMPORTANCE_HIGH).apply { description = ch.description })
        }
    }

    /** Канал по смыслу оповещения. */
    fun channelFor(title: String, message: String): Channel {
        val t = (title + " " + message).lowercase()
        return when {
            "оплат" in t -> Channel.PAYMENTS
            "заявк" in t || "регистрац" in t -> Channel.REGISTRATIONS
            "сообщен" in t || "чат" in t -> Channel.CHAT
            "заказ" in t || "доставк" in t -> Channel.ORDERS
            else -> Channel.OTHER
        }
    }

    fun post(context: Context, title: String, message: String, orderId: String = "", channel: Channel = channelFor(title, message)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannels(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (orderId.isNotBlank()) putExtra(EXTRA_OPEN_ORDER, orderId)
            putExtra(EXTRA_OPEN_NOTIFICATIONS, orderId.isBlank())
        }
        val requestCode = (title + message + System.nanoTime()).hashCode()
        val pending = PendingIntent.getActivity(context, requestCode, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_stat_sansara)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(requestCode, notification) }
    }

    const val EXTRA_OPEN_ORDER = "open_order"
    const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
}
