package ru.sansara.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class SansaraRole { CLIENT, ADMIN, PRODUCTION }

data class SansaraProduct(
    val sku: String,
    val name: String,
    val type: String,
    val quality: String,
    val size: String,
    val price: Int,
    val stock: Int,
    val status: String,
    val productionDays: Int,
    val imageUrl: String = "",
    val externalId: String = ""
)

data class SansaraProductionOp(
    val date: String,
    val time: String,
    val sku: String,
    val name: String,
    val qty: Int,
    val assembler: String,
    val postedBy: String,
    val status: String = "Проведен"
)

data class SansaraProductionDraft(
    val product: SansaraProduct,
    val qty: Int,
    val assembler: String,
    val date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
)

data class SansaraClient(
    val id: String,
    val name: String,
    val contact: String,
    val phone: String,
    val status: String,
    val discount: Int,
    val monthTurnover: Int,
    val orderCount: Int,
    val accessCode: String,
    val online: Boolean,
    val lastSeen: String,
    val email: String = "",
    val clientType: String = "Торгующая организация",
    val registeredAt: String = "28.09.2026",
    val orderingEnabled: Boolean = true,
    val city: String = "",
    val address: String = "",
    val firstName: String = contact.trim().split(Regex("\\s+")).let { parts ->
        when {
            parts.size >= 2 -> parts[1]
            parts.isNotEmpty() -> parts[0]
            else -> ""
        }
    },
    val lastSeenEpochMs: Long = 0L
)

data class SansaraContact(
    val fullName: String,
    val phone: String,
    val email: String = "",
    val isPrimary: Boolean = false
)

data class SansaraOrderLine(
    val sku: String,
    val name: String,
    val qty: Int,
    val price: Int
)

data class SansaraOrderEvent(
    val status: String,
    val dateTime: String,
    val actor: String
)

data class SansaraOrder(
    val id: String,
    val clientName: String,
    val dateTime: String,
    val lines: List<SansaraOrderLine>,
    val status: String,
    val history: List<SansaraOrderEvent> = listOf(SansaraOrderEvent(status, dateTime, "Система")),
    val deliveryMethod: String = "Доставка",
    val deliveryAddress: String = "",
    val comment: String = ""
) {
    val pieces: Int get() = lines.sumOf { it.qty }
    val total: Int get() = lines.sumOf { it.qty * it.price }
}

data class SansaraRegistration(
    val organization: String,
    val fio: String,
    val inn: String,
    val contact1: String,
    val phone1: String,
    val email: String,
    val city: String,
    val address: String,
    val type: String,
    val contact2: String = "",
    val phone2: String = "",
    val email2: String = "",
    val id: String = "R-${System.currentTimeMillis()}",
    val createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
    val status: String = "Новая",
    val contacts: List<SansaraContact> = buildList {
        if (contact1.isNotBlank() || phone1.isNotBlank()) add(SansaraContact(contact1, phone1, email, true))
        if (contact2.isNotBlank() || phone2.isNotBlank()) add(SansaraContact(contact2, phone2, email2, false))
    }
)

data class SansaraSession(
    val userId: String,
    val clientId: String?,
    val role: SansaraRole,
    val phone: String,
    val displayName: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class SansaraSnapshot(
    val products: List<SansaraProduct>,
    val clients: List<SansaraClient>,
    val registrations: List<SansaraRegistration>,
    val orders: List<SansaraOrder>,
    val productionOps: List<SansaraProductionOp>,
    val productionDrafts: List<SansaraProductionDraft>,
    val stockOverrides: Map<String, Int>,
    val cart: Map<String, Int>
)
