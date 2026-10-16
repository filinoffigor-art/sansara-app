package ru.sansara.app

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Журнал сбоев: если приложение падает, причина записывается в файл, а при следующем запуске
 * пользователь видит окно «Приложение было закрыто из-за ошибки» и может отправить отчёт.
 * Хранятся три последних сбоя.
 */
object SansaraCrashLog {
    private const val FILE = "crash_log.txt"
    private const val PENDING = "crash_pending"
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record(app, thread.name, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun record(context: Context, threadName: String, error: Throwable) {
        val sw = StringWriter(); error.printStackTrace(PrintWriter(sw))
        val rt = Runtime.getRuntime()
        val entry = buildString {
            append("=== ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))).append(" ===\n")
            append("SANSARA ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
            append("Телефон: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(", Android ").append(Build.VERSION.RELEASE).append('\n')
            append("Память: занято ").append((rt.totalMemory() - rt.freeMemory()) / 1_048_576).append(" МБ из ").append(rt.maxMemory() / 1_048_576).append(" МБ\n")
            append("Поток: ").append(threadName).append('\n')
            append(sw.toString().lines().take(60).joinToString("\n")).append('\n')
        }
        val file = File(context.filesDir, FILE)
        val old = if (file.exists()) file.readText() else ""
        val kept = old.split("=== ").filter { it.isNotBlank() }.takeLast(2).joinToString("") { "=== $it" }
        file.writeText(entry + kept)
        File(context.filesDir, PENDING).writeText("1")
    }

    /** Отчёт о сбое, случившемся в прошлый запуск (null, если сбоя не было или он уже показан). */
    fun pending(context: Context): String? {
        val flag = File(context.filesDir, PENDING)
        if (!flag.exists()) return null
        return File(context.filesDir, FILE).takeIf { it.exists() }?.readText()?.split("=== ")?.firstOrNull { it.isNotBlank() }?.let { "=== $it" }
    }

    fun markSeen(context: Context) { File(context.filesDir, PENDING).delete() }

    fun all(context: Context): String = File(context.filesDir, FILE).takeIf { it.exists() }?.readText().orEmpty()
}
