package ru.sansara.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SansaraCrashLog.install(this)
        SansaraReminderScheduler.ensureChannel(this)
        SansaraNotify.ensureChannels(this)
        SansaraAdminOpsReminderScheduler.ensureChannel(this)
        SansaraAdminOpsReminderScheduler.scheduleAll(this)
        // Системные панели больше не скрываются: часы, заряд и жест «назад» остаются на месте,
        // а содержимое отступает от них через safeDrawingPadding() в корне интерфейса.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0B0A09"))
        )
        pendingOpen.value = intent?.getStringExtra(SansaraNotify.EXTRA_OPEN_ORDER)
            ?: if (intent?.getBooleanExtra(SansaraNotify.EXTRA_OPEN_NOTIFICATIONS, false) == true) "" else null
        setContent { SansaraVisualPrototype() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Нажатие на уведомление при открытом приложении: передаём заказ в интерфейс.
        setIntent(intent)
        pendingOpen.value = intent.getStringExtra(SansaraNotify.EXTRA_OPEN_ORDER)
            ?: if (intent.getBooleanExtra(SansaraNotify.EXTRA_OPEN_NOTIFICATIONS, false)) "" else null
    }

    /** Заказ из нажатого уведомления ("" — открыть список уведомлений). */
    val pendingOpen = androidx.compose.runtime.mutableStateOf<String?>(null)

    /** Разрешение на уведомления спрашиваем в момент, когда оно понятно пользователю: после первого заказа или входа сотрудника. */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
