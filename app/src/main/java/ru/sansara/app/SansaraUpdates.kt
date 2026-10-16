package ru.sansara.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Опубликованное администратором обновление приложения. */
data class SansaraUpdateInfo(val versionName: String, val versionCode: Long, val notes: String, val publishedAt: String)

/**
 * Обновление приложения: администратор публикует APK, клиенты и сотрудники видят «Доступно обновление» и ставят его.
 * Без сервера файл хранится на этом телефоне; с сервером он будет скачиваться с сервера — остальное не изменится.
 * Android требует подтверждения установки пользователем: полностью без нажатия обновляются только приложения из магазина.
 */
object SansaraUpdates {
    private fun prefs(context: Context) = context.getSharedPreferences("sansara", Context.MODE_PRIVATE)
    private fun file(context: Context) = File(File(context.filesDir, "updates").apply { mkdirs() }, "sansara-update.apk")

    fun installedVersionCode(context: Context): Long = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrDefault(BuildConfig.VERSION_CODE.toLong())

    fun published(context: Context): SansaraUpdateInfo? {
        val p = prefs(context)
        val code = p.getLong("update_version_code", 0L)
        if (code <= 0L || !file(context).exists()) return null
        return SansaraUpdateInfo(p.getString("update_version_name", "").orEmpty(), code, p.getString("update_notes", "").orEmpty(), p.getString("update_published_at", "").orEmpty())
    }

    /** Обновление новее установленной версии (null — обновлений нет). */
    fun available(context: Context): SansaraUpdateInfo? = published(context)?.takeIf { it.versionCode > installedVersionCode(context) }

    /** Проверяет и сохраняет APK: это должен быть SANSARA и новее установленной версии. */
    fun publish(context: Context, uri: Uri, notes: String): Result<SansaraUpdateInfo> = runCatching {
        val tmp = File(context.cacheDir, "incoming-update.apk")
        context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } } ?: error("Не удалось открыть файл")
        val archive = context.packageManager.getPackageArchiveInfo(tmp.absolutePath, 0) ?: error("Это не файл приложения (APK)")
        require(archive.packageName == context.packageName) { "Это APK другого приложения (${archive.packageName})" }
        val code = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else @Suppress("DEPRECATION") archive.versionCode.toLong()
        require(code > installedVersionCode(context)) { "Версия ${archive.versionName} не новее установленной ${BuildConfig.VERSION_NAME}" }
        tmp.copyTo(file(context), overwrite = true)
        tmp.delete()
        val info = SansaraUpdateInfo(archive.versionName.orEmpty(), code, notes.trim(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
        prefs(context).edit()
            .putLong("update_version_code", info.versionCode).putString("update_version_name", info.versionName)
            .putString("update_notes", info.notes).putString("update_published_at", info.publishedAt).apply()
        info
    }

    fun unpublish(context: Context) {
        file(context).delete()
        prefs(context).edit().remove("update_version_code").remove("update_version_name").remove("update_notes").remove("update_published_at").apply()
    }

    /** Разрешено ли приложению ставить обновления (один раз включается в настройках телефона). */
    fun canInstall(context: Context): Boolean = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Открывает системную установку обновления (пользователь нажимает «Установить»). */
    fun install(context: Context): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file(context))
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
