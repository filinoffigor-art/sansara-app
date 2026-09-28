package ru.sansara.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BackendPostResult(val ok: Boolean, val message: String)

/**
 * Optional bridge for the commercial test. The APK never stores Telegram bot tokens.
 * A configured Apps Script/backend receives events and routes registrations/orders
 * to the two separate Telegram bots and the central data store.
 */
object SansaraBackend {
    suspend fun postEvent(baseUrl: String, event: String, payload: Map<String, Any?>): BackendPostResult = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext BackendPostResult(false, "Backend API не настроен")
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) { "Некорректный BACKEND_API_URL" }

        val body = JSONObject().apply {
            put("event", event)
            put("source", "SANSARA_ANDROID")
            put("payload", JSONObject(payload))
        }.toString()

        val connection = (URL(baseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SANSARA-App/0.9")
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            val code = connection.responseCode
            val response = runCatching {
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            if (code in 200..299) BackendPostResult(true, response.ifBlank { "OK" })
            else BackendPostResult(false, "HTTP $code${if (response.isBlank()) "" else ": $response"}")
        } finally {
            connection.disconnect()
        }
    }
}
