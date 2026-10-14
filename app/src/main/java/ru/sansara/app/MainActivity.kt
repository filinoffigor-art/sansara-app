package ru.sansara.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SansaraReminderScheduler.ensureChannel(this)
        SansaraAdminOpsReminderScheduler.ensureChannel(this)
        SansaraAdminOpsReminderScheduler.scheduleAll(this)
        // Системные панели больше не скрываются: часы, заряд и жест «назад» остаются на месте,
        // а содержимое отступает от них через safeDrawingPadding() в корне интерфейса.
        enableEdgeToEdge()
        setContent { SansaraVisualPrototype() }
    }

    /** Разрешение на уведомления спрашиваем в момент, когда оно понятно пользователю: после первого заказа или входа сотрудника. */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
