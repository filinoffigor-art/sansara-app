package ru.sansara.app.data

import ru.sansara.app.BuildConfig

sealed class AuthResult {
    data class Success(val session: SansaraSession): AuthResult()
    data class Error(val message: String): AuthResult()
}

interface AuthProvider {
    suspend fun login(phone: String, code: String): AuthResult
}

class AccessCodeAuthProvider(
    private val clientsProvider: () -> List<SansaraClient>
): AuthProvider {
    override suspend fun login(phone: String, code: String): AuthResult {
        val normalized = normalizePhone(phone)
        val cleanCode = code.trim()

        val client = clientsProvider().firstOrNull {
            normalizePhone(it.phone) == normalized && it.accessCode == cleanCode
        }
        if(client != null) {
            if(client.status == "Приостановлен" || !client.orderingEnabled) {
                return AuthResult.Error("Доступ приостановлен. Свяжитесь с администратором.")
            }
            return AuthResult.Success(
                SansaraSession(
                    userId="U-${client.id}-1",
                    clientId=client.id,
                    role=SansaraRole.CLIENT,
                    phone=client.phone,
                    displayName=client.firstName.ifBlank { client.contact }
                )
            )
        }

        if(BuildConfig.DEBUG) {
            if(normalized == normalizePhone(BuildConfig.ADMIN_PHONE) && cleanCode == "9001") {
                return AuthResult.Success(
                    SansaraSession(
                        userId="U-ADMIN-001",
                        clientId=null,
                        role=SansaraRole.ADMIN,
                        phone=BuildConfig.ADMIN_PHONE,
                        displayName="Игорь"
                    )
                )
            }
            if(normalized == normalizePhone("+7 999 000-00-01") && cleanCode == "9002") {
                return AuthResult.Success(
                    SansaraSession(
                        userId="U-PROD-001",
                        clientId=null,
                        role=SansaraRole.PRODUCTION,
                        phone="+7 999 000-00-01",
                        displayName="Производство"
                    )
                )
            }
        }

        return AuthResult.Error("Телефон или код доступа не найден")
    }

    private fun normalizePhone(value: String): String =
        value.filter(Char::isDigit).takeLast(10)
}
