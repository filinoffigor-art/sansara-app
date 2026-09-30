package ru.sansara.app.data

import ru.sansara.app.BuildConfig
import ru.sansara.app.SansaraBackend

class PresenceReporter {
    suspend fun ping(session: SansaraSession): Boolean {
        if(BuildConfig.BACKEND_API_URL.isBlank()) return false
        val result = SansaraBackend.postEvent(
            BuildConfig.BACKEND_API_URL,
            "presence",
            mapOf(
                "userId" to session.userId,
                "clientId" to session.clientId,
                "role" to session.role.name,
                "phone" to session.phone,
                "lastSeenEpochMs" to System.currentTimeMillis()
            )
        )
        return result.ok
    }
}
