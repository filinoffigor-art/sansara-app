package ru.sansara.app

import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONObject
import org.json.JSONArray
import java.text.NumberFormat
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import ru.sansara.app.ui.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private val ProtoBg = Bg
private val ProtoPanel = Panel
private val ProtoPanel2 = Panel2
private val ProtoGold = Gold
private val ProtoGoldSoft = GoldSoft
private val ProtoText = Text
private val ProtoMuted = Muted
private val ProtoGreen = Green
private val ProtoRed = Red
private val ProtoBorder = Border
private val ProtoOrange = Warning

private enum class ProtoScreen {
    Welcome, Login, Registration, RegistrationSent,
    Home, Catalog, Filter, ProductList, ProductDetail, Cart, Checkout, OrderSent, OrderList, OrderDetail, Notifications, ClientChat, ClientReports, ClientSettings, AgentClients, AgentClientDetail, RetailHome, RetailCatalog, RetailFilter, RetailProductList, RetailProductDetail, RetailCart, RetailCheckout, RetailOrderSent, RetailOrderList, RetailOrderDetail, RetailNotifications, Profile, Suspended,
    AdminHome, AdminSearch, AdminClients, AdminClient, AdminOrders, AdminOrderDetail, AdminNotifications, AdminCatalog, AdminSettings, AdminSettingsDetail, AdminAttention, OnlineController, LowStockList, AdminChats, AdminChat, AdminProductionChat, AdminReports, AdminWorkshop, AdminAdmins, AdminAttendance,
    Production, ProductionCategory, ProductionCatalog, ProductionEntry, ProductionHistory, ProductionReport, ProductionPayments, ProductionProfile, ProductionWorkshop, ProductionAttendance, ProductionChat,
    Server, StockList, ReserveList, NewClients, Export, AdminAssemblers
}

private enum class ProtoClientType(val label: String) { AGENT("Агент"), TRADING("Торгующая организация") }

data class ProtoCatalogProduct(
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

data class ProtoProductionOp(
    val date: String,
    val time: String,
    val sku: String,
    val name: String,
    val qty: Int,
    val assembler: String,
    val postedBy: String,
    val status: String = "Проведен",
    val rateRub: Int = 0,
    val amountRub: Int = qty * rateRub,
    val documentId: String = "",
    val opId: String = ""
)

private data class ProtoProductionDraft(
    val product: ProtoCatalogProduct,
    val qty: Int,
    val assembler: String,
    val rateRub: Int,
    val date: String = currentDateShort()
)

data class ProtoClient(
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
    val firstName: String = contact.substringBefore(" ").ifBlank { name.substringBefore(" ") }
)

data class ProtoOrderLine(
    val sku: String,
    val name: String,
    val qty: Int,
    val price: Int,
    val discountPct: Int = 0
) {
    val lineTotal: Int get() = qty * price * (100 - discountPct) / 100
}
data class ProtoOrderEvent(val status: String, val dateTime: String, val actor: String)

data class ProtoOrder(
    val id: String,
    val clientName: String,
    val dateTime: String,
    val lines: List<ProtoOrderLine>,
    val status: String,
    val history: List<ProtoOrderEvent> = listOf(ProtoOrderEvent(status, dateTime, "Система")),
    val deliveryMethod: String = "Доставка",
    val deliveryAddress: String = "",
    val comment: String = "",
    val recipient: String = "",
    val contactPhone: String = "",
    val deliveryDate: String = "",
    val deliveryTime: String = ""
) {
    val pieces: Int get() = lines.sumOf { it.qty }
    val baseTotal: Int get() = lines.sumOf { it.qty * it.price }
    val total: Int get() = lines.sumOf { it.lineTotal }
    val discountPct: Int get() = lines.firstOrNull()?.discountPct ?: 0
}

data class ProtoNotification(
    val id: String,
    val audienceRole: String,
    val audienceKey: String,
    val title: String,
    val message: String,
    val dateTime: String,
    val read: Boolean = false
)

private data class ProtoProductGroup(
    val key: String,
    val title: String,
    val imageType: String,
    val enabled: Boolean
)

private val protoProductGroups = listOf(
    ProtoProductGroup("wreaths", "Венки", "Венки", true),
    ProtoProductGroup("coffins", "Гробы", "Гробы", false),
    ProtoProductGroup("crosses", "Кресты", "Кресты", false),
    ProtoProductGroup("clothes", "Одежда", "Одежда", false),
    ProtoProductGroup("ribbons", "Ленты", "Ленты", false),
    ProtoProductGroup("flowers", "Цветы", "Цветы", false),
    ProtoProductGroup("services", "Услуги", "Услуги", false)
)

data class ProtoRegistration(
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
    val status: String = "Новая"
)

private val ruLocale = Locale("ru", "RU")
private fun currentDateLong(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale))
private fun currentDateShort(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
private fun currentTimeShort(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
private fun nextWorkingDay(from: LocalDate): LocalDate {
    var day = from.plusDays(1)
    while (day.dayOfWeek == java.time.DayOfWeek.SATURDAY || day.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
        day = day.plusDays(1)
    }
    return day
}
private fun currentMonthLabel(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() }

@Composable
fun SansaraVisualPrototype() {
    val context = LocalContext.current
    val repository = remember { SansaraRepository.get(context) }
    val authProvider: AuthProvider = remember { LocalAuthProvider(repository, SecureSessionStore(context)) }
    var session by remember { mutableStateOf<SansaraSession?>(null) }
    var dataReady by remember { mutableStateOf(false) }
    val products = remember { mutableStateListOf<ProtoCatalogProduct>() }
    val stockOverrides: SnapshotStateMap<String, Int> = remember { mutableStateMapOf() }
    val cart: SnapshotStateMap<String, Int> = remember { mutableStateMapOf() }
    val history: SnapshotStateList<ProtoScreen> = remember { mutableStateListOf() }

    var screen by remember { mutableStateOf(ProtoScreen.Welcome) }
    var showRolePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedQualities by remember { mutableStateOf(setOf<String>()) }
    var selectedSizes by remember { mutableStateOf(setOf<String>()) }
    var selectedAvailability by remember { mutableStateOf(setOf<String>()) }
    var minPriceFilter by remember { mutableStateOf<Int?>(null) }
    var maxPriceFilter by remember { mutableStateOf<Int?>(null) }
    val companyContacts = remember { mutableStateListOf<SansaraCompanyContact>() }
    var selectedProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var detailQty by remember { mutableIntStateOf(1) }
    var cartEditingSku by remember { mutableStateOf<String?>(null) }
    var productListIndex by remember { mutableIntStateOf(0) }
    var productListOffset by remember { mutableIntStateOf(0) }
    var selectedOrderId by remember { mutableStateOf("S-002384") }
    var selectedClientId by remember { mutableStateOf("C-1024") }
    var selectedChatClientId by remember { mutableStateOf("C-1024") }
    var selectedAgentCustomerId by remember { mutableStateOf<String?>(null) }
    var showClientMenu by remember { mutableStateOf(false) }
    val chatMessages = remember { mutableStateListOf<SansaraChatMessage>() }
    val agentCustomers = remember { mutableStateListOf<SansaraAgentCustomer>() }
    val agentSettings = remember { mutableStateListOf<SansaraAgentSettings>() }
    val agentMarkups = remember { mutableStateListOf<SansaraAgentMarkup>() }
    val agentReminders = remember { mutableStateListOf<SansaraAgentReminder>() }
    val adminAccounts = remember { mutableStateListOf<SansaraAdminAccount>() }
    val workshopTasks = remember { mutableStateListOf<WorkshopTaskEntity>() }
    val attendanceRows = remember { mutableStateListOf<AttendanceEntity>() }
    val presenceSessions = remember { mutableStateListOf<PresenceSessionEntity>() }
    val productionAudits = remember { mutableStateListOf<ProductionAuditEntity>() }
    var adminDailyStatus by remember { mutableStateOf<AdminDailyStatusEntity?>(null) }
    val retailCart = remember { mutableStateMapOf<String,Int>() }
    var retailSelectedProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var retailDetailQty by remember { mutableIntStateOf(1) }
    var retailLastTotal by remember { mutableIntStateOf(0) }
    var lastRegistration by remember { mutableStateOf<ProtoRegistration?>(null) }
    var productionCategory by remember { mutableStateOf("Венки") }
    var productionProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var productionQty by remember { mutableIntStateOf(1) }
    var productionAssembler by remember { mutableStateOf("") }
    var productionRate by remember { mutableIntStateOf(0) }
    val assemblers = remember { mutableStateListOf<SansaraAssembler>() }
    val productionRates: SnapshotStateMap<String,Int> = remember { mutableStateMapOf() }
    var selectedProductionDate by remember { mutableStateOf(LocalDate.now()) }
    var lowStockThreshold by remember { mutableIntStateOf(5) }
    var notificationsRegistration by remember { mutableStateOf(true) }
    var notificationsOrders by remember { mutableStateOf(true) }
    var notificationsProduction by remember { mutableStateOf(true) }
    var notificationsLowStock by remember { mutableStateOf(true) }
    var settingsSection by remember { mutableStateOf("Профиль компании") }
    var backupStatus by remember { mutableStateOf("Резервная копия ещё не создавалась") }
    var registrationsTotalThisMonth by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("sansara", Context.MODE_PRIVATE) }
    val notifications = remember {
        mutableStateListOf<ProtoNotification>().apply { addAll(protoLoadNotifications(prefs)) }
    }
    var tildaFeedUrl by remember { mutableStateOf(prefs.getString("tilda_yml_url", BuildConfig.TILDA_YML_URL).orEmpty()) }
    var catalogSyncStatus by remember { mutableStateOf("Тестовый каталог · локальные данные") }
    var catalogSyncInProgress by remember { mutableStateOf(false) }
    var lastCatalogSync by remember { mutableStateOf(prefs.getString("last_catalog_sync", "Не выполнялась").orEmpty()) }
    var backendApiUrl by remember { mutableStateOf(prefs.getString("backend_api_url", BuildConfig.BACKEND_API_URL).orEmpty()) }
    var telegramEnabled by remember { mutableStateOf(prefs.getBoolean("telegram_enabled", false)) }
    var telegramRetryEnabled by remember { mutableStateOf(prefs.getBoolean("telegram_retry_enabled", true)) }
    var telegramTemplate by remember {
        mutableStateOf(
            prefs.getString(
                "telegram_template",
                "Заказ №{number}\nКлиент: {client}\nСостав: {items}\nСумма: {total}\nКонтакты: {contact}\nПолучение: {date} {time}"
            ).orEmpty()
        )
    }
    var telegramLastLog by remember { mutableStateOf(prefs.getString("telegram_last_log", "Отправок ещё не было").orEmpty()) }
    var backendStatus by remember { mutableStateOf(if (backendApiUrl.isBlank()) "Backend API не настроен · события сохраняются локально" else "Backend API настроен") }
    val scope = rememberCoroutineScope()

    val clients = remember { mutableStateListOf<ProtoClient>() }

    val registrations = remember { mutableStateListOf<ProtoRegistration>() }

    val orders = remember { mutableStateListOf<ProtoOrder>() }

    val productionOps = remember { mutableStateListOf<ProtoProductionOp>() }

    val productionDrafts = remember { mutableStateListOf<ProtoProductionDraft>() }

    fun applySnapshot(snapshot: SansaraPersistedSnapshot) {
        products.clear(); products.addAll(snapshot.products)
        stockOverrides.clear(); stockOverrides.putAll(snapshot.stockOverrides)
        clients.clear(); clients.addAll(snapshot.clients)
        registrations.clear(); registrations.addAll(snapshot.registrations)
        orders.clear(); orders.addAll(snapshot.orders)
        productionOps.clear(); productionOps.addAll(snapshot.productionOps)
        cart.clear(); cart.putAll(snapshot.cart)
        assemblers.clear(); assemblers.addAll(snapshot.assemblers)
        productionRates.clear(); productionRates.putAll(snapshot.productionRates)
        if (productionAssembler.isBlank()) {
            productionAssembler = snapshot.assemblers.firstOrNull { it.enabled }?.name.orEmpty()
        }
        registrationsTotalThisMonth = snapshot.registrations.size
    }

    fun startScreenForRole(role: SansaraRole): ProtoScreen = when (role) {
        SansaraRole.CLIENT -> ProtoScreen.Home
        SansaraRole.ADMIN -> ProtoScreen.AdminHome
        SansaraRole.PRODUCTION -> ProtoScreen.Production
    }

    fun persistAll() {
        val p = products.toList()
        val so = stockOverrides.toMap()
        val c = clients.toList()
        val r = registrations.toList()
        val o = orders.toList()
        val po = productionOps.toList()
        val ca = cart.toMap()
        scope.launch {
            repository.persistSnapshot(p, so, c, r, o, po, ca)
        }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun persistNotifications() = protoSaveNotifications(prefs, notifications)
    fun addNotification(audienceRole:String,audienceKey:String,title:String,message:String) {
        notifications.add(
            0,
            ProtoNotification(
                id = "N-" + java.util.UUID.randomUUID().toString().replace("-","").take(12).uppercase(),
                audienceRole = audienceRole,
                audienceKey = audienceKey,
                title = title,
                message = message,
                dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            )
        )
        persistNotifications()
    }
    fun go(target: ProtoScreen) { if (target != screen) { history.add(screen); screen = target } }
    fun back() { screen = if (history.isNotEmpty()) history.removeAt(history.lastIndex) else ProtoScreen.Welcome }
    fun resetFilters(type: String? = null, availability: String? = null) {
        selectedTypes = type?.let { setOf(it) } ?: emptySet()
        selectedQualities = emptySet(); selectedSizes = emptySet()
        selectedAvailability = availability?.let { setOf(it) } ?: emptySet()
        minPriceFilter = null
        maxPriceFilter = null
    }
    fun openProduct(p: ProtoCatalogProduct, fromCart:Boolean = false) {
        selectedProduct = p
        cartEditingSku = if (fromCart) p.sku else null
        detailQty = if (fromCart) (cart[p.sku] ?: 1).coerceAtLeast(1) else 1
        go(ProtoScreen.ProductDetail)
    }
    fun physicalStock(p: ProtoCatalogProduct) = stockOverrides[p.sku] ?: p.stock
    fun reservedForSku(sku: String): Int = orders
        .filter { it.status in setOf("Получен","Подтверждён") }
        .sumOf { o -> o.lines.filter { it.sku == sku }.sumOf { it.qty } }
    fun availableStock(p: ProtoCatalogProduct) = (physicalStock(p) - reservedForSku(p.sku)).coerceAtLeast(0)
    fun currentClient(): ProtoClient = clients.firstOrNull { it.id == selectedClientId }
        ?: clients.firstOrNull()
        ?: ProtoClient("C-OFFLINE","SANSARA","Партнёр","", "Активный",0,0,0,"",false,"ещё не входил",firstName="Партнёр")
    fun discountedPrice(p: ProtoCatalogProduct): Int = p.price * (100 - currentClient().discount) / 100
    fun postDrafts(date: String) {
        val selectedDrafts = productionDrafts.filter { it.date == date }
        if (selectedDrafts.isEmpty()) {
            toast("Добавьте позиции в выпуск выбранного дня")
            return
        }
        val activeAssemblers=assemblers.filter { it.enabled }
        scope.launch {
            runCatching {
                repository.postProductionReceipt(
                    date=date,
                    userId=session?.userId ?: "U-PRODUCTION",
                    lines=selectedDrafts.map { draft ->
                        val assembler=activeAssemblers.firstOrNull { it.name==draft.assembler }
                        ProductionPostingLine(
                            sku=draft.product.sku,
                            name=draft.product.name,
                            assemblerId=assembler?.id.orEmpty(),
                            assemblerName=draft.assembler,
                            qty=draft.qty,
                            rateRub=draft.rateRub
                        )
                    }
                )
            }.onSuccess { result ->
                productionDrafts.removeAll(selectedDrafts.toSet())
                applySnapshot(repository.snapshot())
                toast("Оприходовано "+result.totalQty+" шт. · "+protoMoney(result.totalAmount)+" · "+result.documentId)
            }.onFailure { error ->
                toast("Ошибка прихода: "+(error.message ?: "неизвестная ошибка"))
            }
        }
    }

    fun syncTildaCatalog(showToast: Boolean = true) {
        val url = tildaFeedUrl.trim()
        if (url.isBlank()) { if (showToast) toast("Укажите YML-ссылку каталога Tilda в настройках") ; return }
        if (catalogSyncInProgress) return
        catalogSyncInProgress = true
        catalogSyncStatus = "Синхронизация с Tilda…"
        prefs.edit().putString("tilda_yml_url", url).apply()
        scope.launch {
            try {
                val result = TildaCatalogSync.fetchYml(url)
                result.items.forEach { item ->
                    val idx = products.indexOfFirst { it.sku.equals(item.sku, true) || (it.externalId.isNotBlank() && it.externalId == item.externalId) }
                    if (idx >= 0) {
                        val old = products[idx]
                        products[idx] = old.copy(
                            name = item.name, type = item.category, quality = item.quality, size = item.size,
                            price = if (item.price > 0) item.price else old.price, productionDays = item.productionDays,
                            imageUrl = item.imageUrl, externalId = item.externalId
                        )
                    } else {
                        products.add(ProtoCatalogProduct(item.sku,item.name,item.category,item.quality,item.size,item.price,0,"Под заказ",item.productionDays,item.imageUrl,item.externalId))
                    }
                }
                lastCatalogSync = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                prefs.edit().putString("last_catalog_sync", lastCatalogSync).apply()
                catalogSyncStatus = "${result.message} · остатки и резервы сохранены"
                persistAll()
                if (showToast) toast(catalogSyncStatus)
            } catch (e: Exception) {
                catalogSyncStatus = "Ошибка синхронизации: ${e.message ?: "неизвестная ошибка"}"
                if (showToast) toast(catalogSyncStatus)
            } finally { catalogSyncInProgress = false }
        }
    }

    fun refreshAgentData() {
        scope.launch {
            agentCustomers.clear()
            agentCustomers.addAll(repository.allAgentCustomers())
            agentSettings.clear()
            agentSettings.addAll(repository.allAgentSettings())
            agentMarkups.clear()
            agentMarkups.addAll(repository.allAgentMarkups())
            agentReminders.clear()
            agentReminders.addAll(repository.activeAgentReminders())
        }
    }

    fun refreshOperationsData() {
        scope.launch {
            adminAccounts.clear(); adminAccounts.addAll(repository.adminAccounts())
            workshopTasks.clear(); workshopTasks.addAll(repository.workshopTasks())
            attendanceRows.clear(); attendanceRows.addAll(repository.allAttendance())
            presenceSessions.clear(); presenceSessions.addAll(repository.presenceToday())
            productionAudits.clear(); productionAudits.addAll(repository.productionAudit())
            adminDailyStatus = repository.adminDailyStatus(currentDateShort())
        }
    }

    fun ownerAgentSettings(ownerId:String):SansaraAgentSettings =
        agentSettings.firstOrNull { it.ownerClientId==ownerId } ?: SansaraAgentSettings(ownerId,true,30)

    fun markupForProduct(ownerId:String,customerId:String,product:ProtoCatalogProduct):Int {
        val category=protoProductCategoryKey(product.type)
        val individual=agentMarkups.firstOrNull { it.customerId==customerId && it.category==category }
        if(individual!=null)return individual.markupPct
        val general=ownerAgentSettings(ownerId)
        return if(general.generalMarkupEnabled)general.generalMarkupPct else 0
    }

    fun retailProducts(ownerId:String,customerId:String):List<ProtoCatalogProduct> =
        products.map { product ->
            val markup=markupForProduct(ownerId,customerId,product)
            product.copy(price=(product.price*(1.0+markup/100.0)).roundToInt())
        }

    fun clientConversationId(clientId:String) = "CLIENT:" + clientId

    fun sendChat(
        conversationId:String,
        senderRole:String,
        senderId:String,
        body:String,
        attachmentUri:String="",
        attachmentName:String="",
        attachmentMime:String=""
    ) {
        scope.launch {
            runCatching {
                repository.sendChatMessage(
                    conversationId=conversationId,
                    senderRole=senderRole,
                    senderId=senderId,
                    body=body,
                    attachmentUri=attachmentUri,
                    attachmentName=attachmentName,
                    attachmentMime=attachmentMime
                )
            }.onSuccess { message ->
                chatMessages.add(message)
            }.onFailure { error ->
                toast(error.message ?: "Не удалось отправить сообщение")
            }
        }
    }

    fun markChatRead(conversationId:String,readerRole:String) {
        scope.launch {
            repository.markConversationRead(conversationId,readerRole)
            chatMessages.clear()
            chatMessages.addAll(repository.allChatMessages())
        }
    }

    fun sendRegistrationEvent(reg: ProtoRegistration) {
        if (backendApiUrl.isBlank()) return
        scope.launch {
            val result = SansaraBackend.postEvent(backendApiUrl, "registration", mapOf(
                "registrationId" to reg.id, "organization" to reg.organization, "inn" to reg.inn, "type" to reg.type,
                "contact1" to reg.contact1, "phone1" to reg.phone1, "email" to reg.email, "contact2" to reg.contact2, "phone2" to reg.phone2, "email2" to reg.email2,
                "city" to reg.city, "address" to reg.address, "createdAt" to reg.createdAt
            ))
            backendStatus = if (result.ok) "Регистрация передана backend / Telegram" else result.message
        }
    }

    fun sendOrderEvent(order: ProtoOrder) {
        if (backendApiUrl.isBlank()) {
            telegramLastLog = "Заказ "+order.id+": серверный прокси не настроен"
            prefs.edit().putString("telegram_last_log",telegramLastLog).apply()
            return
        }
        scope.launch {
            val payload = mapOf(
                "orderId" to order.id, "client" to order.clientName, "dateTime" to order.dateTime, "status" to order.status,
                "pieces" to order.pieces, "baseTotal" to order.baseTotal, "discountPct" to order.discountPct, "total" to order.total,
                "recipient" to order.recipient, "contactPhone" to order.contactPhone,
                "deliveryMethod" to order.deliveryMethod, "deliveryAddress" to order.deliveryAddress,
                "deliveryDate" to order.deliveryDate, "deliveryTime" to order.deliveryTime, "comment" to order.comment,
                "lines" to order.lines.joinToString(" | ") { "${it.sku}:${it.qty}:${it.price}:${it.discountPct}" },
                "telegramEnabled" to telegramEnabled,
                "telegramTemplate" to telegramTemplate,
                "telegramRetryEnabled" to telegramRetryEnabled
            )
            val attempts = if (telegramRetryEnabled) 2 else 1
            var result = BackendPostResult(false, "Отправка не выполнена")
            var usedAttempts = 0
            for (attempt in 1..attempts) {
                usedAttempts = attempt
                result = runCatching { SansaraBackend.postEvent(backendApiUrl, "order", payload) }
                    .getOrElse { BackendPostResult(false, it.message ?: "Ошибка соединения с Backend API") }
                if (result.ok) break
                if (attempt < attempts) delay(1200)
            }
            backendStatus = if (result.ok) "Заказ передан backend"+if(telegramEnabled)" / Telegram" else "" else result.message
            telegramLastLog = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))+" · "+order.id+" · "+
                if(result.ok)"успешно · попыток: "+usedAttempts else result.message+" · попыток: "+usedAttempts
            prefs.edit().putString("telegram_last_log",telegramLastLog).apply()
        }
    }

    LaunchedEffect(Unit) {
        repository.seedDebugIfNeeded()
        applySnapshot(repository.snapshot())
        chatMessages.clear()
        chatMessages.addAll(repository.allChatMessages())
        agentCustomers.clear()
        agentCustomers.addAll(repository.allAgentCustomers())
        agentSettings.clear()
        agentSettings.addAll(repository.allAgentSettings())
        agentMarkups.clear()
        agentMarkups.addAll(repository.allAgentMarkups())
        agentReminders.clear()
        agentReminders.addAll(repository.activeAgentReminders())
        adminAccounts.clear(); adminAccounts.addAll(repository.adminAccounts())
        workshopTasks.clear(); workshopTasks.addAll(repository.workshopTasks())
        attendanceRows.clear(); attendanceRows.addAll(repository.allAttendance())
        presenceSessions.clear(); presenceSessions.addAll(repository.presenceToday())
        productionAudits.clear(); productionAudits.addAll(repository.productionAudit())
        adminDailyStatus = repository.adminDailyStatus(currentDateShort())
        session = authProvider.currentSession()
        session?.let { saved ->
            saved.clientId?.let { clientId ->
                selectedClientId = clientId
                companyContacts.clear()
                companyContacts.addAll(repository.contactsForClient(clientId))
            }
            screen = startScreenForRole(saved.role)
        }
        if (BuildConfig.DEBUG) {
            val dbgIntent = (context as? android.app.Activity)?.intent
            val dbgCode = dbgIntent?.getStringExtra("debug_code").orEmpty()
            val dbgScreen = dbgIntent?.getStringExtra("debug_screen").orEmpty()
            if (dbgCode.isNotBlank()) {
                val res = authProvider.signInWithAccessCode(dbgCode)
                if (res.ok) {
                    session = res.session
                    res.session?.clientId?.let { clientId ->
                        selectedClientId = clientId
                        companyContacts.clear()
                        companyContacts.addAll(repository.contactsForClient(clientId))
                    }
                    screen = startScreenForRole(res.session!!.role)
                }
            }
            runCatching { ProtoScreen.valueOf(dbgScreen) }.getOrNull()?.let { target ->
                selectedProduct = products.firstOrNull()
                retailSelectedProduct = products.firstOrNull()
                productionProduct = products.firstOrNull()
                if (target == ProtoScreen.ProductDetail || target == ProtoScreen.RetailProductDetail) detailQty = 1
                screen = target
            }
        }
        dataReady = true
        if (tildaFeedUrl.isNotBlank()) syncTildaCatalog(showToast = false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var appForeground by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> appForeground = true
                Lifecycle.Event.ON_STOP -> appForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        appForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(session, appForeground, dataReady) {
        val active = session ?: return@LaunchedEffect
        if (!appForeground || !dataReady) return@LaunchedEffect
        while (appForeground) {
            repository.updatePresence(active.userId,active.clientId,active.role.name)
            active.clientId?.let { clientId ->
                val index = clients.indexOfFirst { it.id == clientId }
                if (index >= 0) clients[index] = clients[index].copy(online = true, lastSeen = "сейчас")
            }
            if (BuildConfig.BACKEND_API_URL.isNotBlank()) {
                SansaraBackend.postEvent(
                    BuildConfig.BACKEND_API_URL,
                    "presence",
                    mapOf(
                        "userId" to active.userId,
                        "clientId" to active.clientId,
                        "role" to active.role.name,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
            }
            delay(60_000L)
        }
    }

    if (!dataReady) {
        SansaraTheme { ProtoLoadingState() }
        return
    }

    BackHandler(enabled = screen != ProtoScreen.Welcome) { back() }

    SansaraTheme {
        Box(Modifier.fillMaxSize()) {
        when (screen) {
            ProtoScreen.Welcome -> ProtoWelcomeScreen(
                onLogin = { go(ProtoScreen.Login) },
                onRegister = { go(ProtoScreen.Registration) },
                onRole = { if (BuildConfig.DEBUG) showRolePicker = true }
            )
            ProtoScreen.Login -> ProtoLoginScreen(onBack = { back() }, onLogin = { code ->
                scope.launch {
                    val result = authProvider.signInWithAccessCode(code)
                    if (result.ok) {
                        session = result.session
                        result.session?.clientId?.let { clientId ->
                            selectedClientId = clientId
                            companyContacts.clear()
                            companyContacts.addAll(repository.contactsForClient(clientId))
                        }
                        history.clear()
                        screen = startScreenForRole(result.session!!.role)
                    } else toast(result.message)
                }
            })
            ProtoScreen.Registration -> ProtoRegistrationScreen(onBack = { back() }, onSubmit = { reg ->
                registrations.add(0, reg)
                lastRegistration = reg
                registrationsTotalThisMonth += 1
                persistAll()
                sendRegistrationEvent(reg)
                go(ProtoScreen.RegistrationSent)
            })
            ProtoScreen.RegistrationSent -> ProtoRegistrationSentScreen(lastRegistration, onBack = { history.clear(); screen = ProtoScreen.Welcome })

            ProtoScreen.Home -> ProtoClientHomeScreen(
                client = clients.firstOrNull { it.id == selectedClientId } ?: clients.first(),
                products = products,
                stockOverrides = stockOverrides,
                availableStock = { availableStock(it) },
                cartCount = cart.size,
                query = searchQuery,
                onQuery = { searchQuery = it },
                onSearch = {
                    resetFilters();
                    go(ProtoScreen.ProductList)
                },
                unreadCount = notifications.count { it.audienceRole == "CLIENT" && it.audienceKey == currentClient().name && !it.read },
                onNotifications = { go(ProtoScreen.Notifications) },
                onAvailability = { status -> if (status == "Все") resetFilters() else resetFilters(availability = status); go(ProtoScreen.ProductList) },
                onCategory = { type ->
                    if (type != "Венки") toast("Раздел «$type» в разработке")
                    else { resetFilters(type = type); go(ProtoScreen.Filter) }
                },
                onOpenProduct = { openProduct(it) },
                onCart = { go(ProtoScreen.Cart) },
                onOrders = { go(ProtoScreen.OrderList) },
                onProfile = { go(ProtoScreen.ClientChat) },
                onCatalog = { go(ProtoScreen.Catalog) },
                onSeeAll = { searchQuery = ""; resetFilters(); go(ProtoScreen.ProductList) },
                onAddToCart = { p, q ->
                    if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) {
                        toast("Оформление заказов временно приостановлено")
                        go(ProtoScreen.Suspended)
                    } else {
                        cart[p.sku] = (cart[p.sku] ?: 0) + q
                        toast("Добавлено в корзину: ${q} шт.")
                        persistAll()
                    }
                }
            )
            ProtoScreen.Catalog -> ProtoCatalogHomeScreen(
                cartCount = cart.size,
                onBack = { back() },
                onSearch = { value -> searchQuery = value; resetFilters(); go(ProtoScreen.ProductList) },
                onCategory = { type ->
                    if (type != "Венки") toast("Раздел «$type» в разработке")
                    else { resetFilters(type = "Венки"); go(ProtoScreen.Filter) }
                },
                onAvailability = { status -> searchQuery = ""; if (status == "Все") resetFilters() else resetFilters(availability = status); go(ProtoScreen.ProductList) },
                onHome = { history.clear(); screen = ProtoScreen.Home }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { go(ProtoScreen.ClientChat) }
            )
            ProtoScreen.Filter -> ProtoFilterScreen(
                products = products.filter { it.type == "Венки" },
                selectedTypes = selectedTypes,
                selectedQualities = selectedQualities,
                selectedSizes = selectedSizes,
                selectedAvailability = selectedAvailability,
                minPrice = minPriceFilter,
                maxPrice = maxPriceFilter,
                availableStock = { availableStock(it) },
                onApply = { types, qualities, sizes, availability, minPrice, maxPrice ->
                    selectedTypes = types
                    selectedQualities = qualities
                    selectedSizes = sizes
                    selectedAvailability = availability
                    minPriceFilter = minPrice
                    maxPriceFilter = maxPrice
                    searchQuery = ""
                    go(ProtoScreen.ProductList)
                },
                onBack = { back() }
            )
            ProtoScreen.ProductList -> {
                val filtered = products.filter { p ->
                    (searchQuery.isBlank() || p.sku.contains(searchQuery, true) || p.name.contains(searchQuery, true)) &&
                    (selectedTypes.isEmpty() || p.type in selectedTypes) &&
                    (selectedQualities.isEmpty() || p.quality == "—" || p.quality in selectedQualities) &&
                    (selectedSizes.isEmpty() || p.size in selectedSizes) &&
                    (selectedAvailability.isEmpty() || (if (availableStock(p) > 0) "В наличии" else "Под заказ") in selectedAvailability) &&
                    (minPriceFilter == null || p.price >= minPriceFilter!!) &&
                    (maxPriceFilter == null || p.price <= maxPriceFilter!!)
                }
                ProtoProductListScreen(
                    products = filtered,
                    cart = cart,
                    stockOverrides = stockOverrides,
                    availableStock = { availableStock(it) },
                    discount = currentClient().discount,
                    initialIndex = productListIndex,
                    initialOffset = productListOffset,
                    onBack = { back() },
                    onOpenFilter = { go(ProtoScreen.Filter) },
                    onOpenProduct = { product, index, offset ->
                        productListIndex = index
                        productListOffset = offset
                        openProduct(product)
                    },
                    onHome = { history.clear(); screen = ProtoScreen.Home },
                    onCatalog = { history.clear(); screen = ProtoScreen.Catalog },
                    onCart = { go(ProtoScreen.Cart) },
                    onOrders = { go(ProtoScreen.OrderList) },
                    onProfile = { go(ProtoScreen.ClientChat) },
                onAddToCart = { p, q ->
                    if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) {
                        toast("Оформление заказов временно приостановлено")
                        go(ProtoScreen.Suspended)
                    } else {
                        cart[p.sku] = (cart[p.sku] ?: 0) + q
                        toast("Добавлено в корзину: ${q} шт.")
                        persistAll()
                    }
                }
                )
            }
            ProtoScreen.ProductDetail -> ProtoProductDetailScreen(
                product = selectedProduct,
                currentStock = selectedProduct?.let { availableStock(it) } ?: 0,
                qty = detailQty,
                discount = currentClient().discount,
                cartCount = cart.size,
                editingCart = cartEditingSku == selectedProduct?.sku,
                onBack = { cartEditingSku = null; back() },
                onMinus = { detailQty = (detailQty - 1).coerceAtLeast(1) },
                onPlus = { detailQty += 1 },
                onAdd = {
                    if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) {
                        toast("Оформление заказов временно приостановлено")
                        go(ProtoScreen.Suspended)
                    } else {
                        selectedProduct?.let { p ->
                            if (cartEditingSku == p.sku) {
                                cart[p.sku] = detailQty
                                toast("Количество в корзине обновлено: ${detailQty} шт.")
                            } else {
                                cart[p.sku] = (cart[p.sku] ?: 0) + detailQty
                                toast("Добавлено в корзину: ${detailQty} шт.")
                            }
                            persistAll()
                        }
                        cartEditingSku = null
                        back()
                    }
                },
                onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { go(ProtoScreen.Catalog) }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { go(ProtoScreen.ClientChat) }
            )
            ProtoScreen.Cart -> ProtoCartScreen(products, cart, discount = currentClient().discount, availableStock = { availableStock(it) }, onBack = { back() }, onPlus = { p -> cart[p.sku] = (cart[p.sku] ?: 0) + 1; persistAll() }, onMinus = { p -> val n = (cart[p.sku] ?: 1) - 1; if (n <= 0) cart.remove(p.sku) else cart[p.sku] = n; persistAll() }, onDelete = { cart.remove(it.sku); persistAll() }, onOpenProduct = { openProduct(it, fromCart = true) }, onCheckout = {
                if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) go(ProtoScreen.Suspended) else go(ProtoScreen.Checkout)
            }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { go(ProtoScreen.Catalog) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { go(ProtoScreen.ClientChat) })
            ProtoScreen.Checkout -> {
                val baseTotal = cart.entries.sumOf { (sku, q) -> products.firstOrNull { it.sku == sku }?.let { it.price * q } ?: 0 }
                val finalTotal = baseTotal * (100 - currentClient().discount) / 100
                val totalPieces = cart.values.sum()
                val shortages = cart.mapNotNull { (sku, qty) ->
                    products.firstOrNull { it.sku == sku }?.let { product ->
                        val available = availableStock(product)
                        if (qty > available) product to (qty - available) else null
                    }
                }
                val productionDays = shortages.maxOfOrNull { it.first.productionDays } ?: 0
                val earliestDate = if (productionDays > 0) LocalDate.now().plusDays(productionDays.toLong()) else nextWorkingDay(LocalDate.now())
                ProtoCheckoutScreen(
                    cartPositions = cart.size,
                    totalPieces = totalPieces,
                    baseTotal = baseTotal,
                    finalTotal = finalTotal,
                    discount = currentClient().discount,
                    defaultAddress = currentClient().address,
                    contacts = companyContacts.ifEmpty {
                        listOf(SansaraCompanyContact("fallback", currentClient().contact, currentClient().phone, currentClient().email))
                    },
                    clientStatus = currentClient().status,
                    earliestDate = earliestDate,
                    shortages = shortages.map { pair -> pair.first.name + ": " + pair.second + " шт. под заказ" },
                    onBack = { back() },
                    onSubmit = { method, address, comment, recipient, contactPhone, deliveryDate, deliveryTime ->
                        if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) {
                            go(ProtoScreen.Suspended)
                        } else {
                            scope.launch {
                                val lines = cart.mapNotNull { (sku,q) ->
                                    products.firstOrNull { it.sku == sku }?.let {
                                        ProtoOrderLine(it.sku,it.name,q,it.price,currentClient().discount)
                                    }
                                }
                                val id = repository.nextOrderId()
                                val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                                val newOrder = ProtoOrder(
                                    id=id,
                                    clientName=currentClient().name,
                                    dateTime=now,
                                    lines=lines,
                                    status="Получен",
                                    history=listOf(ProtoOrderEvent("Получен",now,currentClient().contact)),
                                    deliveryMethod=method,
                                    deliveryAddress=address,
                                    comment=comment,
                                    recipient=recipient,
                                    contactPhone=contactPhone,
                                    deliveryDate=deliveryDate,
                                    deliveryTime=deliveryTime
                                )
                                orders.add(0,newOrder)
                                selectedOrderId=id
                                cart.clear()
                                addNotification(
                                    audienceRole = "ADMIN",
                                    audienceKey = "ADMIN",
                                    title = "Новый заказ " + id,
                                    message = currentClient().name + " · " + protoMoney(newOrder.total)
                                )
                                persistAll()
                                sendOrderEvent(newOrder)
                                go(ProtoScreen.OrderSent)
                            }
                        }
                    },
                    onHome = { history.clear(); screen = ProtoScreen.Home },
                    onCatalog = { go(ProtoScreen.Catalog) },
                    onCart = { go(ProtoScreen.Cart) },
                    onOrders = { go(ProtoScreen.OrderList) },
                    onProfile = { go(ProtoScreen.ClientChat) }
                )
            }
            ProtoScreen.OrderSent -> ProtoOrderSentScreen(orders.firstOrNull { it.id == selectedOrderId }, onView = { go(ProtoScreen.OrderDetail) }, onCatalog = { history.clear(); screen = ProtoScreen.Catalog }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { go(ProtoScreen.ClientChat) })
            ProtoScreen.OrderList -> ProtoOrderListScreen(
                orders = orders.filter { it.clientName == (clients.firstOrNull { c -> c.id == selectedClientId }?.name ?: "") },
                products = products,
                onBack = { back() },
                onOpen = { selectedOrderId = it.id; go(ProtoScreen.OrderDetail) },
                onHome = { history.clear(); screen = ProtoScreen.Home },
                onCatalog = { go(ProtoScreen.Catalog) },
                onCart = { go(ProtoScreen.Cart) },
                onProfile = { go(ProtoScreen.ClientChat) }
            )
            ProtoScreen.OrderDetail -> ProtoOrderDetailScreen(
                order = orders.firstOrNull { it.id == selectedOrderId },
                products = products,
                isAdmin = false,
                onBack = { back() },
                onStatus = {},
                onRepeat = {
                    orders.firstOrNull { it.id == selectedOrderId }?.let { order ->
                        cart.clear()
                        order.lines.forEach { line -> cart[line.sku] = line.qty }
                        persistAll()
                        toast("Заказ " + order.id + " подготовлен к повтору")
                        go(ProtoScreen.Checkout)
                    }
                },
                onEditRepeat = {
                    orders.firstOrNull { it.id == selectedOrderId }?.let { order ->
                        cart.clear()
                        order.lines.forEach { line -> cart[line.sku] = line.qty }
                        persistAll()
                        toast("Можно изменить состав и количество")
                        go(ProtoScreen.Cart)
                    }
                },
                onCancel = {
                    val idx = orders.indexOfFirst { it.id == selectedOrderId }
                    if (idx >= 0 && orders[idx].status == "Получен") {
                        val old = orders[idx]
                        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        orders[idx] = old.copy(
                            status = "Отменён",
                            history = old.history + ProtoOrderEvent("Отменён", now, currentClient().contact)
                        )
                        persistAll()
                        toast("Заказ " + old.id + " отменён")
                    }
                }
            )
            ProtoScreen.Notifications -> {
                val clientName = currentClient().name
                ProtoNotificationsScreen(
                    notifications = notifications.filter { it.audienceRole == "CLIENT" && it.audienceKey == clientName },
                    onBack = { back() },
                    onReadAll = {
                        var changed = false
                        notifications.indices.forEach { index ->
                            val n = notifications[index]
                            if (n.audienceRole == "CLIENT" && n.audienceKey == clientName && !n.read) {
                                notifications[index] = n.copy(read = true)
                                changed = true
                            }
                        }
                        if (changed) persistNotifications()
                    },
                    onDelete = { id ->
                        notifications.removeAll { it.id == id }
                        persistNotifications()
                    }
                )
            }
            ProtoScreen.ClientChat -> {
                val cid = currentClient().id
                val conversationId = clientConversationId(cid)
                ProtoChatScreen(
                    title = "Чат с менеджером",
                    subtitle = currentClient().name,
                    messages = chatMessages.filter { it.conversationId == conversationId },
                    currentRole = "CLIENT",
                    onBack = { back() },
                    onRead = { markChatRead(conversationId,"CLIENT") },
                    onSend = { body, uri, name, mime ->
                        sendChat(conversationId,"CLIENT",session?.userId ?: cid,body,uri,name,mime)
                    }
                )
            }
            ProtoScreen.ClientReports -> ProtoClientReportsScreen(
                orders = orders.filter { it.clientName == currentClient().name },
                onBack = { back() }
            )
            ProtoScreen.ClientSettings -> ProtoClientSettingsScreen(
                notificationsEnabled = notificationsOrders,
                onNotifications = { notificationsOrders = it },
                onBack = { back() }
            )
            ProtoScreen.Profile -> ProtoProfileScreen(clients.firstOrNull { it.id == selectedClientId } ?: clients.first(), onBack = { back() }, onCall = { protoDial(context) }, onLogout = { authProvider.signOut(); session = null; history.clear(); screen = ProtoScreen.Welcome }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { go(ProtoScreen.Catalog) }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) })
            ProtoScreen.Suspended -> ProtoSuspendedScreen(onCall = { protoDial(context) }, onMessage = { protoMessage(context) }, onBack = { back() }, onCatalog = { go(ProtoScreen.Catalog) }, onHome = { history.clear(); screen = ProtoScreen.Home }, onOrders = { go(ProtoScreen.OrderList) })

            ProtoScreen.AdminHome -> ProtoAdminHomeScreen(
                registrationsTotal = registrationsTotalThisMonth, clients = clients, orders = orders, productionOps = productionOps, products = products, stockOverrides = stockOverrides, lowStockThreshold = lowStockThreshold,
                reservedForSku = { reservedForSku(it) },
                onSearch = { go(ProtoScreen.AdminSearch) },
                onRegistrations = { go(ProtoScreen.AdminAttention) }, onClients = { go(ProtoScreen.AdminClients) }, onOrders = { go(ProtoScreen.AdminOrders) },
                onProduction = { go(ProtoScreen.Production) }, onStock = { go(ProtoScreen.Server) }, onCatalog = { go(ProtoScreen.AdminCatalog) }, onSettings = { go(ProtoScreen.AdminSettings) },
                onAttention = { go(ProtoScreen.AdminAttention) }, onOnline = { go(ProtoScreen.OnlineController) }, onLowStock = { go(ProtoScreen.LowStockList) },
                onChats = { go(ProtoScreen.AdminChats) },
                onWorkshop = { refreshOperationsData(); go(ProtoScreen.AdminWorkshop) },
                onReports = { refreshOperationsData(); go(ProtoScreen.AdminReports) },
                unreadCount = notifications.count { it.audienceRole == "ADMIN" && !it.read },
                onNotifications = { go(ProtoScreen.AdminNotifications) }
            )
            ProtoScreen.AdminSearch -> ProtoAdminSearchScreen(clients, orders, products, stockOverrides, onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) }, onOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) }, onProduct = { selectedProduct = it; go(ProtoScreen.ProductDetail) })
            ProtoScreen.AdminClients -> ProtoAdminClientsScreen(clients, onBack = { back() }, onOpen = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.AdminClient -> ProtoAdminClientScreen(
                client = clients.firstOrNull { it.id == selectedClientId },
                retailCustomers = agentCustomers.filter { it.ownerClientId == selectedClientId },
                retailMarkups = agentMarkups.filter { it.ownerClientId == selectedClientId },
                onBack = { back() },
                onStatus = { status -> val i=clients.indexOfFirst{it.id==selectedClientId};if(i>=0){val c=clients[i];clients[i]=c.copy(status=status,orderingEnabled=if(status=="Приостановлен")false else c.orderingEnabled);persistAll()} },
                onToggleBlock = { val i=clients.indexOfFirst{it.id==selectedClientId};if(i>=0){val c=clients[i];clients[i]=c.copy(status=if(c.status=="Приостановлен")"Активный" else "Приостановлен",orderingEnabled=c.status=="Приостановлен");persistAll()} },
                onToggleOrdering = { val i=clients.indexOfFirst{it.id==selectedClientId};if(i>=0){val c=clients[i];clients[i]=c.copy(orderingEnabled=!c.orderingEnabled);persistAll()} },
                onDiscount = { delta -> val i = clients.indexOfFirst { it.id == selectedClientId }; if (i >= 0) { val c = clients[i]; clients[i] = c.copy(discount = (c.discount + delta).coerceIn(0,50)); persistAll() } },
                onSave = { toast("Карточка клиента сохранена") },
                onCall = { protoDialNumber(context, clients.firstOrNull { it.id == selectedClientId }?.phone ?: BuildConfig.ADMIN_PHONE) }
            )
            ProtoScreen.AdminOrders -> ProtoAdminOrdersScreen(orders, onBack = { back() }, onOpen = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) })
            ProtoScreen.AdminOrderDetail -> ProtoOrderDetailScreen(orders.firstOrNull { it.id == selectedOrderId }, products = products, isAdmin = true, onBack = { back() }, onStatus = { st ->
                val idx = orders.indexOfFirst { it.id == selectedOrderId }
                if (idx >= 0) {
                    val oldOrder = orders[idx]
                    val statuses = listOf("Получен","Подтверждён","Собирается","Доставляется","Доставлен")
                    val oldIndex = statuses.indexOf(oldOrder.status)
                    val newIndex = statuses.indexOf(st)
                    if (st == oldOrder.status) {
                        toast("Статус уже установлен")
                    } else if (newIndex >= oldIndex && newIndex >= 0) {
                        if (st == "Собирается" && oldOrder.status in setOf("Получен","Подтверждён")) {
                            oldOrder.lines.forEach { line ->
                                products.firstOrNull { it.sku == line.sku }?.let { p ->
                                    stockOverrides[p.sku] = (physicalStock(p) - line.qty).coerceAtLeast(0)
                                }
                            }
                        }
                        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        orders[idx] = oldOrder.copy(status = st, history = oldOrder.history + ProtoOrderEvent(st, now, "Администратор"))
                        addNotification(
                            audienceRole = "CLIENT",
                            audienceKey = oldOrder.clientName,
                            title = "Заказ " + oldOrder.id,
                            message = "Статус изменён: " + st
                        )
                        persistAll()
                    } else toast("Статус заказа нельзя переводить назад")
                }
            })
            ProtoScreen.AdminChats -> ProtoAdminChatsScreen(
                clients = clients,
                messages = chatMessages,
                onBack = { back() },
                onOpen = { client ->
                    selectedChatClientId = client.id
                    go(ProtoScreen.AdminChat)
                },
                onProduction = { go(ProtoScreen.AdminProductionChat) }
            )
            ProtoScreen.AdminChat -> {
                val client = clients.firstOrNull { it.id == selectedChatClientId }
                val conversationId = clientConversationId(selectedChatClientId)
                ProtoChatScreen(
                    title = client?.name ?: "Чат с клиентом",
                    subtitle = client?.contact ?: "Клиент",
                    messages = chatMessages.filter { it.conversationId == conversationId },
                    currentRole = "ADMIN",
                    onBack = { back() },
                    onRead = { markChatRead(conversationId,"ADMIN") },
                    onSend = { body, uri, name, mime ->
                        sendChat(conversationId,"ADMIN",session?.userId ?: "U-ADMIN",body,uri,name,mime)
                    }
                )
            }
            ProtoScreen.AdminProductionChat -> {
                val conversationId="STAFF:ADMIN_PRODUCTION"
                ProtoChatScreen(
                    title="Производство",
                    subtitle="Чат администратора с цехом",
                    messages=chatMessages.filter{it.conversationId==conversationId},
                    currentRole="ADMIN",
                    onBack={back()},
                    onRead={markChatRead(conversationId,"ADMIN")},
                    onSend={body,uri,name,mime->sendChat(conversationId,"ADMIN",session?.userId?:"U-ADMIN",body,uri,name,mime)}
                )
            }
            ProtoScreen.AdminNotifications -> ProtoNotificationsScreen(
                notifications = notifications.filter { it.audienceRole == "ADMIN" },
                onBack = { back() },
                onReadAll = {
                    var changed = false
                    notifications.indices.forEach { index ->
                        val n = notifications[index]
                        if (n.audienceRole == "ADMIN" && !n.read) {
                            notifications[index] = n.copy(read = true)
                            changed = true
                        }
                    }
                    if (changed) persistNotifications()
                },
                onDelete = { id ->
                    notifications.removeAll { it.id == id }
                    persistNotifications()
                }
            )
            ProtoScreen.AdminCatalog -> ProtoAdminCatalogScreen(products, stockOverrides, syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, onSync = { syncTildaCatalog() }, onBack = { back() })
            ProtoScreen.AdminSettings -> ProtoAdminSettingsMenuScreen(
                syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, lowStockThreshold = lowStockThreshold,
                onBack = { back() }, onOpen = { section ->
                    when (section) {
                        "Клиенты" -> go(ProtoScreen.AdminClients)
                        "Сборщицы" -> go(ProtoScreen.AdminAssemblers)
                        "Администраторы" -> { refreshOperationsData(); go(ProtoScreen.AdminAdmins) }
                        "Задание в цех" -> { refreshOperationsData(); go(ProtoScreen.AdminWorkshop) }
                        "Табель рабочего времени" -> { refreshOperationsData(); go(ProtoScreen.AdminAttendance) }
                        "Отчёты" -> { refreshOperationsData(); go(ProtoScreen.AdminReports) }
                        "Экспорт данных" -> go(ProtoScreen.Export)
                        else -> { settingsSection = section; go(ProtoScreen.AdminSettingsDetail) }
                    }
                }, onCall = { protoDial(context) }
            )
            ProtoScreen.AdminSettingsDetail -> ProtoAdminSettingsDetailScreen(
                section = settingsSection, threshold = lowStockThreshold, reg = notificationsRegistration, orders = notificationsOrders, prod = notificationsProduction, low = notificationsLowStock,
                tildaUrl = tildaFeedUrl, syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, syncing = catalogSyncInProgress, backupStatus = backupStatus, backendStatus = backendStatus,
                telegramEnabled=telegramEnabled,telegramRetryEnabled=telegramRetryEnabled,telegramApiUrl=backendApiUrl,telegramTemplate=telegramTemplate,telegramLastLog=telegramLastLog,
                onBack = { back() }, onThreshold = { lowStockThreshold = it.coerceIn(1,50) }, onReg = { notificationsRegistration = it }, onOrders = { notificationsOrders = it }, onProd = { notificationsProduction = it }, onLow = { notificationsLowStock = it },
                onTildaUrl = { tildaFeedUrl = it }, onSync = { syncTildaCatalog() }, onBackup = { backupStatus = "Последняя копия: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}" }, onClients = { go(ProtoScreen.AdminClients) },
                onTelegramSave={enabled,retry,url,template->
                    telegramEnabled=enabled
                    telegramRetryEnabled=retry
                    backendApiUrl=url.trim()
                    telegramTemplate=template
                    backendStatus=if(backendApiUrl.isBlank())"Backend API не настроен · события сохраняются локально" else "Backend API настроен"
                    prefs.edit()
                        .putBoolean("telegram_enabled",enabled)
                        .putBoolean("telegram_retry_enabled",retry)
                        .putString("backend_api_url",backendApiUrl)
                        .putString("telegram_template",template)
                        .apply()
                    toast("Настройки Telegram сохранены")
                }
            )
            ProtoScreen.AdminAttention -> ProtoAttentionScreen(
                regs = registrations, blocked = clients.filter { it.status == "Приостановлен" }, picking = orders.filter { it.status == "Собирается" },
                lowStock = products.filter { availableStock(it) <= lowStockThreshold }, stockValue = { availableStock(it) },
                onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) }, onOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) },
                onApprove = { reg ->
                    scope.launch {
                        val approval = repository.approve(reg)
                        applySnapshot(repository.snapshot())
                        val second = approval.secondaryCode?.let { " · второй контакт: " + it }.orEmpty()
                        toast("Клиент подтверждён. Код: " + approval.primaryCode + second)
                    }
                },
                onClose = { reg ->
                    scope.launch {
                        repository.reject(reg.id)
                        registrations.remove(reg)
                        persistAll()
                    }
                }, onLowStock = { go(ProtoScreen.LowStockList) }
            )
            ProtoScreen.OnlineController -> ProtoOnlineControllerScreen(clients, onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.LowStockList -> ProtoLowStockListScreen(products, stockOverrides, reservedForSku = { reservedForSku(it) }, threshold = lowStockThreshold, onBack = { back() })
            ProtoScreen.AdminAssemblers -> ProtoAssemblerAdminScreen(
                assemblers=assemblers,
                onBack={back()},
                onAdd={name->
                    scope.launch {
                        runCatching { repository.saveAssembler(null,name) }
                            .onSuccess { applySnapshot(repository.snapshot()) }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                },
                onToggle={assembler->
                    scope.launch {
                        repository.setAssemblerEnabled(assembler.id,!assembler.enabled)
                        applySnapshot(repository.snapshot())
                    }
                }
            )

            ProtoScreen.AdminAdmins -> ProtoAdminAccountsScreen(
                admins=adminAccounts,
                currentUserId=session?.userId.orEmpty(),
                onBack={back()},
                onAdd={name->
                    scope.launch {
                        runCatching{repository.addAdmin(name,session?.userId?:"")}
                            .onSuccess{refreshOperationsData();toast("Администратор добавлен под общим паролем")}
                            .onFailure{toast(it.message?:"Ошибка")}
                    }
                },
                onToggle={admin->
                    scope.launch {
                        runCatching{repository.setAdminEnabled(admin.userId,!admin.enabled,session?.userId?:"")}
                            .onSuccess{refreshOperationsData()}
                            .onFailure{toast(it.message?:"Ошибка")}
                    }
                }
            )
            ProtoScreen.AdminWorkshop -> ProtoWorkshopTasksScreen(
                tasks=workshopTasks,
                productionMode=false,
                onBack={back()},
                onCreate={date,linesJson,comment,commentOnly->
                    scope.launch {
                        runCatching{repository.createWorkshopTask(date,linesJson,comment,commentOnly,session?.userId?:"U-ADMIN")}
                            .onSuccess{task->refreshOperationsData();addNotification("PRODUCTION","", "Задание получено", task.id+" · "+task.taskDate);toast("Задание отправлено в цех")}
                            .onFailure{toast(it.message?:"Ошибка")}
                    }
                },
                onFact={_,_,_->}
            )
            ProtoScreen.AdminAttendance -> ProtoAttendanceScreen(
                selectedDate=selectedProductionDate,
                assemblers=assemblers,
                rows=attendanceRows,
                editable=true,
                onBack={back()},
                onDate={selectedProductionDate=it;refreshOperationsData()},
                onSave={person,status,comment->
                    scope.launch {
                        repository.saveAttendance(selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),person.id,person.name,status,comment)
                        refreshOperationsData()
                    }
                }
            )
            ProtoScreen.AdminReports -> ProtoAdminReportsScreen(
                tasks=workshopTasks,
                attendance=attendanceRows,
                presence=presenceSessions,
                productionOps=productionOps,
                dailyStatus=adminDailyStatus,
                onBack={back()},
                onDailyStatus={dayOff,reason->
                    scope.launch {
                        repository.saveAdminDailyStatus(currentDateShort(),dayOff,reason)
                        refreshOperationsData()
                    }
                }
            )

            ProtoScreen.Production -> {
                val productionDateKey = selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ProtoProductionHomeScreen(
                    selectedDate = selectedProductionDate,
                    drafts = productionDrafts.filter { it.date == productionDateKey },
                    opsForDay = productionOps.filter { it.date == productionDateKey },
                    products = products,
                    stockOverrides = stockOverrides,
                    onDateChange = { selectedProductionDate = it },
                    onAdd = { go(ProtoScreen.ProductionCategory) },
                    onPostAll = { postDrafts(productionDateKey) },
                    onHistory = { go(ProtoScreen.ProductionHistory) },
                    onReport = { go(ProtoScreen.ProductionReport) },
                    onPayments = { go(ProtoScreen.ProductionPayments) },
                    onStock = { go(ProtoScreen.Server) },
                    onWorkshop = { refreshOperationsData(); go(ProtoScreen.ProductionWorkshop) },
                    onAttendance = { refreshOperationsData(); go(ProtoScreen.ProductionAttendance) },
                    onChat = { go(ProtoScreen.ProductionChat) },
                    onHome = { toast("Главный экран производства") },
                    onProfile = { go(ProtoScreen.ProductionProfile) }
                )
            }
            ProtoScreen.ProductionCategory -> ProtoProductionCategoryScreen(onBack = { back() }, onCategory = { productionCategory = it; go(ProtoScreen.ProductionCatalog) })
            ProtoScreen.ProductionCatalog -> ProtoProductionCatalogScreen(
                products.filter { it.type == productionCategory },
                onBack = { back() },
                onSelect = {
                    productionProduct = it
                    productionQty = 1
                    productionRate = productionRates[it.sku] ?: 0
                    if (productionAssembler.isBlank()) productionAssembler = assemblers.firstOrNull { a -> a.enabled }?.name.orEmpty()
                    go(ProtoScreen.ProductionEntry)
                }
            )
            ProtoScreen.ProductionEntry -> {
                val productionDateKey = selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ProtoProductionEntryScreen(
                    product=productionProduct,
                    qty=productionQty,
                    assembler=productionAssembler,
                    rateRub=productionRate,
                    assemblers=assemblers.filter { it.enabled },
                    selectedDate=selectedProductionDate,
                    onBack={back()},
                    onMinus={productionQty=(productionQty-1).coerceAtLeast(1)},
                    onPlus={productionQty+=1},
                    onAssembler={productionAssembler=it},
                    onRate={
                        productionRate=it.coerceAtLeast(0)
                        productionProduct?.let { p ->
                            productionRates[p.sku]=productionRate
                            scope.launch { repository.setProductionRate(p.sku,productionRate) }
                        }
                    },
                    onAddDraft={
                        productionProduct?.let { p ->
                            if(productionAssembler.isBlank()){
                                toast("Выберите сборщицу")
                            } else {
                                productionDrafts.add(ProtoProductionDraft(p,productionQty,productionAssembler,productionRate,productionDateKey))
                                toast("Позиция добавлена в выпуск выбранного дня")
                                history.clear()
                                screen=ProtoScreen.Production
                            }
                        }
                    },
                    onPostNow={
                        val p=productionProduct
                        val assembler=assemblers.firstOrNull { it.name==productionAssembler }
                        if(p==null || assembler==null){
                            toast("Выберите товар и сборщицу")
                        }else{
                            scope.launch {
                                runCatching {
                                    repository.postProductionReceipt(
                                        date=productionDateKey,
                                        userId=session?.userId ?: "U-PRODUCTION",
                                        lines=listOf(
                                            ProductionPostingLine(
                                                sku=p.sku,
                                                name=p.name,
                                                assemblerId=assembler.id,
                                                assemblerName=assembler.name,
                                                qty=productionQty,
                                                rateRub=productionRate
                                            )
                                        )
                                    )
                                }.onSuccess { result ->
                                    applySnapshot(repository.snapshot())
                                    toast("Оприходовано "+result.totalQty+" шт. · "+protoMoney(result.totalAmount))
                                    history.clear()
                                    screen=ProtoScreen.Production
                                }.onFailure { error ->
                                    toast("Ошибка прихода: "+(error.message ?: "неизвестная ошибка"))
                                }
                            }
                        }
                    }
                )
            }
            ProtoScreen.ProductionHistory -> ProtoProductionHistoryScreen(
                productionOps,
                products,
                canEdit=true,
                onBack={back()},
                onExport={go(ProtoScreen.Export)},
                onCorrect={op,newQty->
                    scope.launch {
                        runCatching{repository.changeProductionOp(op.opId,newQty,session?.userId?:"U-PRODUCTION",false)}
                            .onSuccess{applySnapshot(repository.snapshot());refreshOperationsData();toast("Выпуск скорректирован")}
                            .onFailure{toast(it.message?:"Ошибка корректировки")}
                    }
                },
                onDelete={op->
                    scope.launch {
                        runCatching{repository.changeProductionOp(op.opId,0,session?.userId?:"U-PRODUCTION",true)}
                            .onSuccess{applySnapshot(repository.snapshot());refreshOperationsData();toast("Выпуск удалён, запись сохранена в журнале")}
                            .onFailure{toast(it.message?:"Ошибка удаления")}
                    }
                }
            )
            ProtoScreen.ProductionReport -> {
                val productionDateKey = selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ProtoProductionReportScreen(selectedProductionDate, productionOps.filter { it.date == productionDateKey }, products, onBack = { back() })
            }
            ProtoScreen.ProductionPayments -> ProtoProductionPaymentsScreen(productionOps,onBack={back()},onExport={go(ProtoScreen.Export)})
            ProtoScreen.ProductionProfile -> ProtoStaffProfileScreen(role = "Производство", onBack = { back() }, onCall = { protoDial(context) }, onLogout = { authProvider.signOut(); session = null; history.clear(); screen = ProtoScreen.Welcome })

            ProtoScreen.ProductionWorkshop -> ProtoWorkshopTasksScreen(
                tasks=workshopTasks,
                productionMode=true,
                onBack={back()},
                onCreate={_,_,_,_->},
                onFact={task,qty,status->
                    scope.launch {
                        repository.updateWorkshopTaskFact(task.id,qty,status)
                        refreshOperationsData()
                    }
                }
            )
            ProtoScreen.ProductionAttendance -> ProtoAttendanceScreen(
                selectedDate=selectedProductionDate,
                assemblers=assemblers,
                rows=attendanceRows,
                editable=true,
                onBack={back()},
                onDate={selectedProductionDate=it;refreshOperationsData()},
                onSave={person,status,comment->
                    scope.launch {
                        repository.saveAttendance(selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),person.id,person.name,status,comment)
                        refreshOperationsData()
                    }
                }
            )
            ProtoScreen.ProductionChat -> {
                val conversationId="STAFF:ADMIN_PRODUCTION"
                ProtoChatScreen(
                    title="Чат с администратором",
                    subtitle="Производство",
                    messages=chatMessages.filter{it.conversationId==conversationId},
                    currentRole="PRODUCTION",
                    onBack={back()},
                    onRead={markChatRead(conversationId,"PRODUCTION")},
                    onSend={body,uri,name,mime->sendChat(conversationId,"PRODUCTION",session?.userId?:"U-PRODUCTION",body,uri,name,mime)}
                )
            }

            ProtoScreen.Server -> ProtoServerScreen(products, stockOverrides, productionOps, orders, clients, reservedForSku = { reservedForSku(it) }, onBack = { back() }, onProduced = { go(ProtoScreen.ProductionHistory) }, onStock = { go(ProtoScreen.StockList) }, onReserve = { go(ProtoScreen.ReserveList) }, onNewClients = { go(ProtoScreen.NewClients) }, onOnline = { go(ProtoScreen.OnlineController) }, onExport = { go(ProtoScreen.Export) })
            ProtoScreen.StockList -> ProtoStockListScreen(
                products=products,
                stockOverrides=stockOverrides,
                reservedForSku={reservedForSku(it)},
                canAdjust=session?.role==SansaraRole.ADMIN,
                onBack={back()},
                onAdjust={product,delta,reason->
                    scope.launch {
                        runCatching {
                            repository.adjustStock(product.sku,delta,reason,session?.userId ?: "U-ADMIN")
                        }.onSuccess {
                            applySnapshot(repository.snapshot())
                            toast("Остаток скорректирован")
                        }.onFailure { error ->
                            toast("Ошибка: "+(error.message ?: "неизвестная ошибка"))
                        }
                    }
                }
            )
            ProtoScreen.ReserveList -> ProtoReserveListScreen(orders, onBack = { back() })
            ProtoScreen.NewClients -> ProtoNewClientsScreen(clients, onBack = { back() }, onOpen = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.Export -> ProtoExportScreen(onBack = { back() }, onExport = { report -> protoExportCsv(context, report, products, stockOverrides, orders, clients, productionOps, reservedForSku = { reservedForSku(it) }, toast = { toast(it) }) })
            ProtoScreen.AgentClients -> {
                val ownerId=currentClient().id
                ProtoAgentClientsScreen(
                    customers=agentCustomers.filter{it.ownerClientId==ownerId},
                    settings=ownerAgentSettings(ownerId),
                    onBack={back()},
                    onSaveSettings={enabled,pct->
                        scope.launch {
                            runCatching{repository.saveAgentSettings(ownerId,enabled,pct)}
                                .onSuccess{refreshAgentData();toast("Настройки наценки сохранены")}
                                .onFailure{toast(it.message?:"Ошибка сохранения")}
                        }
                    },
                    onAdd={name,phone,email,city,address,comment->
                        scope.launch {
                            runCatching{repository.saveAgentCustomer(null,ownerId,name,phone,email,city,address,comment)}
                                .onSuccess{customer->selectedAgentCustomerId=customer.id;refreshAgentData();toast("Клиент сохранён")}
                                .onFailure{toast(it.message?:"Ошибка сохранения")}
                        }
                    },
                    onOpen={customer->selectedAgentCustomerId=customer.id;go(ProtoScreen.AgentClientDetail)}
                )
            }
            ProtoScreen.AgentClientDetail -> {
                val ownerId=currentClient().id
                val customer=agentCustomers.firstOrNull{it.id==selectedAgentCustomerId}
                ProtoAgentClientDetailScreen(
                    customer=customer,
                    markups=agentMarkups.filter{it.customerId==selectedAgentCustomerId},
                    reminders=agentReminders.filter{it.customerId==selectedAgentCustomerId},
                    onBack={back()},
                    onSaveComment={comment->
                        val c=customer ?: return@ProtoAgentClientDetailScreen
                        scope.launch {
                            runCatching{repository.saveAgentCustomer(c.id,c.ownerClientId,c.fullName,c.phone,c.email,c.city,c.address,comment)}
                                .onSuccess{refreshAgentData();toast("Комментарий сохранён")}
                                .onFailure{toast(it.message?:"Ошибка сохранения")}
                        }
                    },
                    onMarkup={category,pct->
                        val c=customer ?: return@ProtoAgentClientDetailScreen
                        scope.launch {
                            runCatching{repository.setAgentCustomerMarkup(c.id,c.ownerClientId,category,pct)}
                                .onSuccess{refreshAgentData()}
                                .onFailure{toast(it.message?:"Ошибка наценки")}
                        }
                    },
                    onReminder={epoch,note->
                        val c=customer ?: return@ProtoAgentClientDetailScreen
                        scope.launch {
                            runCatching{repository.saveAgentReminder(c.ownerClientId,c.id,c.fullName,epoch,note)}
                                .onSuccess{reminder->SansaraReminderScheduler.schedule(context,reminder);refreshAgentData();toast("Напоминание установлено")}
                                .onFailure{toast(it.message?:"Ошибка напоминания")}
                        }
                    },
                    onRetail={
                        if(customer==null)toast("Клиент не выбран")
                        else{retailCart.clear();retailSelectedProduct=null;history.add(screen);screen=ProtoScreen.RetailHome}
                    }
                )
            }
            ProtoScreen.RetailHome -> {
                val ownerId=currentClient().id
                val customer=agentCustomers.firstOrNull{it.id==selectedAgentCustomerId}
                val retail=retailProducts(ownerId,customer?.id.orEmpty())
                val retailClient=ProtoClient(customer?.id?:"RETAIL",customer?.fullName?:"Частный клиент",customer?.fullName.orEmpty(),customer?.phone.orEmpty(),"Активный",0,0,0,"",false,"сейчас",email=customer?.email.orEmpty(),city=customer?.city.orEmpty(),address=customer?.address.orEmpty(),firstName=customer?.fullName?.substringBefore(" ").orEmpty().ifBlank{"Клиент"})
                val retailAudienceKey=currentClient().name+" / "+(customer?.fullName?:"Частный клиент")
                ProtoClientHomeScreen(
                    client=retailClient,products=retail,stockOverrides=stockOverrides,
                    availableStock={rp->products.firstOrNull{it.sku==rp.sku}?.let{availableStock(it)}?:0},
                    cartCount=retailCart.size,query=searchQuery,onQuery={searchQuery=it},onSearch={resetFilters();go(ProtoScreen.RetailProductList)},
                    unreadCount=notifications.count{it.audienceRole=="CLIENT"&&it.audienceKey==retailAudienceKey&&!it.read},onNotifications={go(ProtoScreen.RetailNotifications)},onAvailability={st->if(st=="Все")resetFilters() else resetFilters(availability=st);go(ProtoScreen.RetailProductList)},
                    onCategory={type->if(type!="Венки")toast("Раздел «$type» в разработке") else{resetFilters(type="Венки");go(ProtoScreen.RetailFilter)}},
                    onOpenProduct={p->retailSelectedProduct=p;retailDetailQty=1;go(ProtoScreen.RetailProductDetail)},
                    onCart={go(ProtoScreen.RetailCart)},onOrders={go(ProtoScreen.RetailOrderList)},onProfile={},onCatalog={go(ProtoScreen.RetailCatalog)},
                    onSeeAll={searchQuery="";resetFilters();go(ProtoScreen.RetailProductList)},onAddToCart={p,q->retailCart[p.sku]=(retailCart[p.sku]?:0)+q;toast("Добавлено в корзину: ${q} шт.")},retailMode=true,onRetailExit={history.clear();screen=ProtoScreen.AgentClientDetail}
                )
            }
            ProtoScreen.RetailCatalog -> ProtoCatalogHomeScreen(
                cartCount=retailCart.size,onBack={back()},onSearch={q->searchQuery=q;resetFilters();go(ProtoScreen.RetailProductList)},
                onCategory={type->if(type!="Венки")toast("Раздел «$type» в разработке") else{resetFilters(type="Венки");go(ProtoScreen.RetailFilter)}},
                onAvailability={st->searchQuery="";if(st=="Все")resetFilters() else resetFilters(availability=st);go(ProtoScreen.RetailProductList)},
                onHome={history.clear();screen=ProtoScreen.RetailHome},onCart={go(ProtoScreen.RetailCart)},onOrders={go(ProtoScreen.RetailOrderList)},onProfile={},
                retailMode=true,onRetailExit={history.clear();screen=ProtoScreen.AgentClientDetail}
            )
            ProtoScreen.RetailFilter -> {
                val ownerId=currentClient().id
                val customerId=selectedAgentCustomerId.orEmpty()
                val retail=retailProducts(ownerId,customerId)
                ProtoFilterScreen(
                    products=retail.filter{it.type=="Венки"},selectedTypes=selectedTypes,selectedQualities=selectedQualities,selectedSizes=selectedSizes,
                    selectedAvailability=selectedAvailability,minPrice=minPriceFilter,maxPrice=maxPriceFilter,
                    availableStock={rp->products.firstOrNull{it.sku==rp.sku}?.let{availableStock(it)}?:0},
                    onApply={types,qualities,sizes,availability,minPrice,maxPrice->selectedTypes=types;selectedQualities=qualities;selectedSizes=sizes;selectedAvailability=availability;minPriceFilter=minPrice;maxPriceFilter=maxPrice;searchQuery="";go(ProtoScreen.RetailProductList)},
                    onBack={back()}
                )
            }
            ProtoScreen.RetailProductList -> {
                val ownerId=currentClient().id
                val customerId=selectedAgentCustomerId.orEmpty()
                val retail=retailProducts(ownerId,customerId)
                val filtered=retail.filter{p->
                    (searchQuery.isBlank()||p.sku.contains(searchQuery,true)||p.name.contains(searchQuery,true)) &&
                    (selectedTypes.isEmpty()||p.type in selectedTypes) &&
                    (selectedQualities.isEmpty()||p.quality=="—"||p.quality in selectedQualities) &&
                    (selectedSizes.isEmpty()||p.size in selectedSizes) &&
                    (selectedAvailability.isEmpty()||(if((products.firstOrNull{it.sku==p.sku}?.let{availableStock(it)}?:0)>0)"В наличии" else "Под заказ") in selectedAvailability) &&
                    (minPriceFilter==null||p.price>=minPriceFilter!!) && (maxPriceFilter==null||p.price<=maxPriceFilter!!)
                }
                ProtoProductListScreen(
                    products=filtered,cart=retailCart,stockOverrides=stockOverrides,
                    availableStock={rp->products.firstOrNull{it.sku==rp.sku}?.let{availableStock(it)}?:0},discount=0,
                    initialIndex=productListIndex,initialOffset=productListOffset,onBack={back()},onOpenFilter={go(ProtoScreen.RetailFilter)},
                    onOpenProduct={p,index,offset->productListIndex=index;productListOffset=offset;retailSelectedProduct=p;retailDetailQty=retailCart[p.sku]?:1;go(ProtoScreen.RetailProductDetail)},
                    onHome={history.clear();screen=ProtoScreen.RetailHome},onCatalog={go(ProtoScreen.RetailCatalog)},onCart={go(ProtoScreen.RetailCart)},onOrders={go(ProtoScreen.RetailOrderList)},onProfile={},
                    onAddToCart={p,q->retailCart[p.sku]=(retailCart[p.sku]?:0)+q;toast("Добавлено в корзину: ${q} шт.")},retailMode=true,onRetailExit={history.clear();screen=ProtoScreen.AgentClientDetail}
                )
            }
            ProtoScreen.RetailProductDetail -> {
                val p=retailSelectedProduct
                ProtoProductDetailScreen(
                    product=p,currentStock=p?.let{rp->products.firstOrNull{it.sku==rp.sku}?.let{availableStock(it)}?:0}?:0,qty=retailDetailQty,discount=0,cartCount=retailCart.size,editingCart=p?.sku?.let{retailCart.containsKey(it)}==true,
                    onBack={back()},onMinus={retailDetailQty=(retailDetailQty-1).coerceAtLeast(1)},onPlus={retailDetailQty+=1},
                    onAdd={p?.let{retailCart[it.sku]=retailDetailQty};back()},onHome={history.clear();screen=ProtoScreen.RetailHome},onCatalog={go(ProtoScreen.RetailCatalog)},onCart={go(ProtoScreen.RetailCart)},onOrders={go(ProtoScreen.RetailOrderList)},onProfile={},
                    retailMode=true,onRetailExit={history.clear();screen=ProtoScreen.AgentClientDetail}
                )
            }
            ProtoScreen.RetailCart -> {
                val ownerId=currentClient().id
                val customerId=selectedAgentCustomerId.orEmpty()
                val retail=retailProducts(ownerId,customerId)
                ProtoCartScreen(
                    products=retail,cart=retailCart,discount=0,availableStock={rp->products.firstOrNull{it.sku==rp.sku}?.let{availableStock(it)}?:0},
                    onBack={back()},onPlus={p->retailCart[p.sku]=(retailCart[p.sku]?:0)+1},onMinus={p->val n=(retailCart[p.sku]?:1)-1;if(n<=0)retailCart.remove(p.sku)else retailCart[p.sku]=n},
                    onDelete={retailCart.remove(it.sku)},onOpenProduct={p->retailSelectedProduct=p;retailDetailQty=retailCart[p.sku]?:1;go(ProtoScreen.RetailProductDetail)},onCheckout={go(ProtoScreen.RetailCheckout)},
                    onHome={history.clear();screen=ProtoScreen.RetailHome},onCatalog={go(ProtoScreen.RetailCatalog)},onOrders={go(ProtoScreen.RetailOrderList)},onProfile={},retailMode=true,onRetailExit={history.clear();screen=ProtoScreen.AgentClientDetail}
                )
            }
            ProtoScreen.RetailCheckout -> {
                val ownerId=currentClient().id
                val customer=agentCustomers.firstOrNull{it.id==selectedAgentCustomerId}
                val retail=retailProducts(ownerId,customer?.id.orEmpty())
                val baseTotal=retailCart.entries.sumOf{(sku,q)->retail.firstOrNull{it.sku==sku}?.price?.times(q)?:0}
                val totalPieces=retailCart.values.sum()
                val shortages=retailCart.mapNotNull{(sku,qty)->retail.firstOrNull{it.sku==sku}?.let{rp->val available=products.firstOrNull{it.sku==sku}?.let{availableStock(it)}?:0;if(qty>available)"${rp.sku}: ${qty-available} шт." else null}}
                val maxDays=retailCart.keys.mapNotNull{sku->retail.firstOrNull{it.sku==sku}?.productionDays}.maxOrNull()?:0
                val contact=SansaraCompanyContact(customer?.id?:"RETAIL",customer?.fullName?:"Частный клиент",customer?.phone.orEmpty(),customer?.email.orEmpty())
                ProtoCheckoutScreen(
                    cartPositions=retailCart.size,totalPieces=totalPieces,baseTotal=baseTotal,finalTotal=baseTotal,discount=0,defaultAddress=customer?.address.orEmpty(),contacts=listOf(contact),
                    clientStatus="Режим «Для клиентов»",earliestDate=LocalDate.now().plusDays(maxDays.toLong()),shortages=shortages,onBack={back()},
                    onSubmit={method,address,comment,recipient,phone,date,time->
                        scope.launch {
                            val id=repository.nextOrderId()
                            val lines=retailCart.mapNotNull{(sku,q)->retail.firstOrNull{it.sku==sku}?.let{ProtoOrderLine(it.sku,it.name,q,it.price,0)}}
                            val order=ProtoOrder(id,currentClient().name+" / "+(customer?.fullName?:"Частный клиент"),LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),lines,"Получен",deliveryMethod=method,deliveryAddress=address,comment=comment,recipient=recipient,contactPhone=phone,deliveryDate=date,deliveryTime=time)
                            orders.add(0,order);retailLastTotal=order.total;retailCart.clear();persistAll();sendOrderEvent(order)
                            addNotification("ADMIN","", "Новый заказ "+order.id, order.clientName+" · "+protoMoney(order.total))
                            history.clear();screen=ProtoScreen.RetailOrderSent
                        }
                    },
                    onHome={history.clear();screen=ProtoScreen.RetailHome},onCatalog={go(ProtoScreen.RetailCatalog)},onCart={go(ProtoScreen.RetailCart)},onOrders={go(ProtoScreen.RetailOrderList)},onProfile={},retailMode=true,onRetailExit={history.clear();screen=ProtoScreen.AgentClientDetail}
                )
            }
            ProtoScreen.RetailOrderList -> {
                val ownerId=currentClient().id
                val customer=agentCustomers.firstOrNull{it.id==selectedAgentCustomerId}
                val retail=retailProducts(ownerId,customer?.id.orEmpty())
                val audienceKey=currentClient().name+" / "+(customer?.fullName?:"Частный клиент")
                ProtoOrderListScreen(
                    orders=orders.filter{it.clientName==audienceKey},
                    products=retail,
                    onBack={back()},
                    onOpen={selectedOrderId=it.id;go(ProtoScreen.RetailOrderDetail)},
                    onHome={history.clear();screen=ProtoScreen.RetailHome},
                    onCatalog={go(ProtoScreen.RetailCatalog)},
                    onCart={go(ProtoScreen.RetailCart)},
                    onProfile={},
                    retailMode=true,
                    onRetailExit={history.clear();screen=ProtoScreen.AgentClientDetail},
                    cartCount=retailCart.size
                )
            }
            ProtoScreen.RetailOrderDetail -> {
                val ownerId=currentClient().id
                val customer=agentCustomers.firstOrNull{it.id==selectedAgentCustomerId}
                val retail=retailProducts(ownerId,customer?.id.orEmpty())
                ProtoOrderDetailScreen(
                    order=orders.firstOrNull{it.id==selectedOrderId},
                    products=retail,
                    isAdmin=false,
                    onBack={back()},
                    onStatus={},
                    onRepeat={
                        orders.firstOrNull{it.id==selectedOrderId}?.let{order->
                            retailCart.clear()
                            order.lines.forEach{line->retailCart[line.sku]=line.qty}
                            toast("Заказ "+order.id+" подготовлен к повтору")
                            go(ProtoScreen.RetailCheckout)
                        }
                    },
                    onEditRepeat={
                        orders.firstOrNull{it.id==selectedOrderId}?.let{order->
                            retailCart.clear()
                            order.lines.forEach{line->retailCart[line.sku]=line.qty}
                            toast("Можно изменить состав и количество")
                            go(ProtoScreen.RetailCart)
                        }
                    },
                    onCancel={
                        val idx=orders.indexOfFirst{it.id==selectedOrderId}
                        if(idx>=0&&orders[idx].status=="Получен"){
                            val old=orders[idx]
                            val now=LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                            orders[idx]=old.copy(status="Отменён",history=old.history+ProtoOrderEvent("Отменён",now,currentClient().contact))
                            persistAll()
                            back()
                        }
                    }
                )
            }
            ProtoScreen.RetailNotifications -> {
                val customer=agentCustomers.firstOrNull{it.id==selectedAgentCustomerId}
                val audienceKey=currentClient().name+" / "+(customer?.fullName?:"Частный клиент")
                ProtoNotificationsScreen(
                    notifications=notifications.filter{it.audienceRole=="CLIENT"&&it.audienceKey==audienceKey},
                    onBack={back()},
                    onReadAll={
                        var changed=false
                        notifications.indices.forEach{index->
                            val n=notifications[index]
                            if(n.audienceRole=="CLIENT"&&n.audienceKey==audienceKey&&!n.read){
                                notifications[index]=n.copy(read=true)
                                changed=true
                            }
                        }
                        if(changed)persistNotifications()
                    },
                    onDelete={id->notifications.removeAll{it.id==id};persistNotifications()}
                )
            }
            ProtoScreen.RetailOrderSent -> ProtoSimpleMessageScreen(
                "Заказ отправлен",
                "Заказ на "+protoMoney(retailLastTotal)+" передан администратору. Закупочные цены клиенту не показываются.",
                onBack={history.clear();screen=ProtoScreen.AgentClientDetail}
            )
        }

        if (session?.role == SansaraRole.CLIENT && screen == ProtoScreen.Home) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start=16.dp,top=20.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC11100E))
                    .border(1.dp,ProtoBorder,CircleShape)
                    .clickable { showClientMenu = true },
                contentAlignment=Alignment.Center
            ){
                Icon(Icons.Outlined.Menu,contentDescription="Меню",tint=ProtoGold,modifier=Modifier.size(25.dp))
            }
        }

        if (showClientMenu) {
            ProtoClientMenuOverlay(
                onDismiss = { showClientMenu = false },
                onChat = { showClientMenu=false; go(ProtoScreen.ClientChat) },
                onProfile = {
                    showClientMenu=false
                    if(currentClient().status=="Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Profile)
                },
                onCustomers = { showClientMenu=false; refreshAgentData(); go(ProtoScreen.AgentClients) },
                onReports = { showClientMenu=false; go(ProtoScreen.ClientReports) },
                onSettings = { showClientMenu=false; go(ProtoScreen.ClientSettings) }
            )
        }

        if (showRolePicker) {
            AlertDialog(
                onDismissRequest = { showRolePicker = false }, containerColor = ProtoPanel,
                title = { Text("Тестовый режим SANSARA", color = ProtoText) },
                text = { Text("Выберите роль для проверки интерфейса", color = ProtoMuted) },
                confirmButton = {
                    Column {
                        TextButton(onClick = { history.clear(); screen = ProtoScreen.Home; showRolePicker = false }) { Text("Клиент", color = ProtoGold) }
                        TextButton(onClick = { history.clear(); screen = ProtoScreen.AdminHome; showRolePicker = false }) { Text("Администратор", color = ProtoGold) }
                        TextButton(onClick = { history.clear(); screen = ProtoScreen.Production; showRolePicker = false }) { Text("Производство", color = ProtoGold) }
                    }
                }
            )
        }
        }
    }
}

@Composable
private fun ProtoLoadingState() {
    Box(Modifier.fillMaxSize().background(ProtoBg),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            CircularProgressIndicator(color=ProtoGold,strokeWidth=2.dp,modifier=Modifier.size(34.dp))
            Spacer(Modifier.height(12.dp))
            Text("Загрузка SANSARA…",color=ProtoMuted,fontSize=13.sp)
        }
    }
}

@Composable
private fun ProtoLiveBackground() {
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painter=painterResource(R.drawable.mock_flowers),contentDescription=null,alpha=.045f,contentScale=ContentScale.Crop,modifier=Modifier.align(Alignment.BottomStart).fillMaxWidth().height(230.dp))
        Image(painter=painterResource(R.drawable.mock_wreath),contentDescription=null,alpha=.025f,contentScale=ContentScale.Crop,modifier=Modifier.align(Alignment.TopEnd).size(180.dp))
    }
}

@Composable
private fun ProtoBrandHeader(
    onBack:(()->Unit)?=null,
    showBell:Boolean=true,
    unreadCount:Int=0,
    onBell:(()->Unit)?=null,
    modifier:Modifier=Modifier
) {
    Row(modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(48.dp),contentAlignment=Alignment.CenterStart){if(onBack!=null)ProtoCircleBack(onBack)}
        Image(painter=painterResource(R.drawable.sansara_wordmark_large),contentDescription="SANSARA",contentScale=ContentScale.Fit,modifier=Modifier.weight(1f).height(68.dp))
        Box(Modifier.width(48.dp),contentAlignment=Alignment.CenterEnd){
            if(showBell){
                BadgedBox(
                    badge={
                        if(unreadCount>0)Badge(containerColor=ProtoGold){
                            Text(unreadCount.coerceAtMost(99).toString(),color=Color.Black,fontSize=9.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                ){
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).then(if(onBell!=null)Modifier.clickable{onBell()} else Modifier),
                        contentAlignment=Alignment.Center
                    ){
                        Icon(Icons.Outlined.Notifications,contentDescription="Уведомления",tint=ProtoGold,modifier=Modifier.size(27.dp))
                    }
                }
            }
        }
    }
}
@Composable
private fun ProtoSearchBar(text:String,onClick:()->Unit,placeholder:String="Поиск по артикулу, названию") {
    Surface(color=ProtoPanel,border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth().height(48.dp).clickable{onClick()}){
        Row(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Outlined.Search,null,tint=ProtoGold,modifier=Modifier.size(25.dp));Spacer(Modifier.width(11.dp))
            Text(if(text.isBlank())placeholder else text,color=if(text.isBlank())ProtoMuted else ProtoText,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProtoAvailabilityChips(selected:String,onSelect:(String)->Unit){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        listOf("Все","В наличии","Под заказ").forEach{label->
            val active=label==selected
            Surface(color=if(active)ProtoGold else ProtoPanel,border=BorderStroke(1.dp,if(active)ProtoGold else ProtoBorder),shape=RoundedCornerShape(28.dp),modifier=Modifier.weight(1f).height(42.dp).clickable{onSelect(label)}){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(label,color=if(active)Color.Black else ProtoText,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=1)}
            }
        }
    }
}

@Composable
private fun ProtoPrimaryButton(text:String,onClick:()->Unit,enabled:Boolean=true,modifier:Modifier=Modifier){
    Button(onClick=onClick,enabled=enabled,modifier=modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(28.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold,contentColor=Color.Black,disabledContainerColor=ProtoPanel2,disabledContentColor=ProtoMuted)){Text(text,fontWeight=FontWeight.Bold,fontSize=15.sp)}
}

@Composable
private fun ProtoSecondaryButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier){
    OutlinedButton(onClick=onClick,modifier=modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(27.dp),border=BorderStroke(1.dp,ProtoGold)){Text(text,color=ProtoGold,fontWeight=FontWeight.SemiBold)}
}

@Composable
private fun ProtoWelcomeScreen(onLogin:()->Unit,onRegister:()->Unit,onRole:()->Unit) {
    val context=LocalContext.current
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){
        Spacer(Modifier.height(18.dp));Image(painterResource(R.drawable.sansara_wordmark_large),"SANSARA",Modifier.fillMaxWidth().height(96.dp).padding(horizontal=6.dp).clickable{onRole()},contentScale=ContentScale.Fit);Spacer(Modifier.height(14.dp))
        Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth().weight(1f)){Box(Modifier.fillMaxSize()){Image(painterResource(R.drawable.mock_wreath),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.38f)));Text("УВАЖЕНИЕ\nВ КАЖДОЙ\nДЕТАЛИ",color=ProtoGoldSoft,fontSize=22.sp,lineHeight=32.sp,fontWeight=FontWeight.Medium,letterSpacing=3.sp,modifier=Modifier.align(Alignment.CenterStart).padding(24.dp))}}
        Spacer(Modifier.height(18.dp));Text("Добро пожаловать",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold)
        Text("Работаем с агентами и торговыми организациями. Заказывайте продукцию, отслеживайте наличие и оформляйте поставки в одном приложении.",color=ProtoMuted,fontSize=14.sp,lineHeight=20.sp,modifier=Modifier.padding(top=10.dp,bottom=18.dp))
        ProtoPrimaryButton("Войти",onLogin);Spacer(Modifier.height(10.dp));ProtoSecondaryButton("Стать партнёром",onRegister)
        TextButton(onClick={protoDial(context)},modifier=Modifier.align(Alignment.CenterHorizontally).padding(vertical=10.dp)){Icon(Icons.Outlined.HeadsetMic,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Связаться с менеджером",color=ProtoGoldSoft)};Spacer(Modifier.height(10.dp))
    }}
}

@Composable
private fun ProtoLoginScreen(onBack:()->Unit,onLogin:(String)->Unit) {
    var code by remember { mutableStateOf("") }
    ProtoScaffold(title="Вход", subtitle="Код доступа выдаёт администратор после подтверждения регистрации", onBack=onBack) {
        item { Spacer(Modifier.height(18.dp)); ProtoSectionCard { Text("Введите код доступа",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); ProtoField(code,{code=it},"Код доступа",keyboardType=KeyboardType.Number); if (BuildConfig.DEBUG) Text("Тест: клиент 1024 · админ 9001 · производство 9002",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp)); Spacer(Modifier.height(14.dp)); Button(onClick={onLogin(code.trim())},enabled=code.isNotBlank(),modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Войти",color=Color.Black,fontWeight=FontWeight.Bold)} } }
    }
}

@Composable
private fun ProtoRegistrationScreen(onBack:()->Unit,onSubmit:(ProtoRegistration)->Unit) {
    var organization by remember{mutableStateOf("")};var inn by remember{mutableStateOf("")};var contact1 by remember{mutableStateOf("")};var phone1 by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var type by remember{mutableStateOf(ProtoClientType.AGENT.label)};var consent by remember{mutableStateOf(false)};var showSecond by remember{mutableStateOf(false)};var contact2 by remember{mutableStateOf("")};var phone2 by remember{mutableStateOf("")}
    val valid=organization.isNotBlank()&&inn.isNotBlank()&&contact1.isNotBlank()&&phone1.isNotBlank()&&email.isNotBlank()&&city.isNotBlank()&&consent
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=false);LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Регистрация партнёра",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Заполните данные для доступа к каталогу и заказам",color=ProtoMuted,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))}
        item{ProtoField(organization,{organization=it},"Название организации / ФИО")};item{ProtoField(inn,{inn=it},"ИНН",KeyboardType.Number)}
        item{Box{ProtoField(contact1,{contact1=it},"Контактное лицо");IconButton(onClick={showSecond=true},modifier=Modifier.align(Alignment.CenterEnd).padding(end=6.dp).size(42.dp).background(ProtoGold,CircleShape)){Icon(Icons.Outlined.Add,null,tint=Color.Black)}}}
        if(showSecond)item{ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){Text("Дополнительное контактное лицо",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));IconButton(onClick={showSecond=false}){Icon(Icons.Outlined.Close,null,tint=ProtoMuted)}};ProtoField(contact2,{contact2=it},"ФИО");Spacer(Modifier.height(8.dp));ProtoField(phone2,{phone2=it},"Телефон",KeyboardType.Phone)}}
        item{ProtoField(phone1,{phone1=it},"Телефон",KeyboardType.Phone)};item{ProtoField(email,{email=it},"E-mail",KeyboardType.Email)};item{ProtoField(city,{city=it},"Город")};item{ProtoField(address,{address=it},"Адрес доставки")}
        item{Text("Тип клиента",color=ProtoText,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(bottom=7.dp));Row(Modifier.fillMaxWidth().height(52.dp).border(1.dp,ProtoGold,RoundedCornerShape(26.dp))){listOf(ProtoClientType.AGENT.label,ProtoClientType.TRADING.label).forEach{option->val active=type==option;Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(if(active)ProtoGold else Color.Transparent).clickable{type=option},contentAlignment=Alignment.Center){Text(option,color=if(active)Color.Black else ProtoText,fontWeight=FontWeight.SemiBold,fontSize=12.sp,maxLines=1)}}}}
        item{Row(Modifier.fillMaxWidth().clickable{consent=!consent},verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if(consent)ProtoGold else ProtoPanel).border(1.dp,if(consent)ProtoGold else ProtoBorder,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){if(consent)Icon(Icons.Outlined.Check,null,tint=Color.Black,modifier=Modifier.size(18.dp))};Spacer(Modifier.width(10.dp));Text("Согласен с условиями обработки данных",color=ProtoText,fontSize=12.sp)}}
        item{ProtoPrimaryButton("Отправить заявку",{if(valid)onSubmit(ProtoRegistration(organization,organization,inn,contact1,phone1,email,city,address,type,contact2,phone2,""))},valid)}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){Text("Уже есть аккаунт?",color=ProtoMuted);TextButton(onClick=onBack){Text("Войти",color=ProtoGold)}}}
    }}}
}

@Composable
private fun ProtoClientHomeScreen(
    client:ProtoClient,
    products:List<ProtoCatalogProduct>,
    stockOverrides:SnapshotStateMap<String,Int>,
    availableStock:(ProtoCatalogProduct)->Int,
    cartCount:Int,
    query:String,
    onQuery:(String)->Unit,
    onSearch:()->Unit,
    unreadCount:Int,
    onNotifications:()->Unit,
    onAvailability:(String)->Unit,
    onCategory:(String)->Unit,
    onOpenProduct:(ProtoCatalogProduct)->Unit,
    onCart:()->Unit,
    onOrders:()->Unit,
    onProfile:()->Unit,
    onCatalog:()->Unit,
    onSeeAll:()->Unit,
    retailMode:Boolean=false,
    onRetailExit:()->Unit={},
    onAddToCart:(ProtoCatalogProduct,Int)->Unit={_,_->}
) {
    var searchOpen by remember{mutableStateOf(false)}
    var mode by remember{mutableStateOf("Все")}
    var previewProduct by remember{mutableStateOf<ProtoCatalogProduct?>(null)}
    val popular=products.filter{availableStock(it)>0}.take(8)

    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(
            containerColor=Color.Transparent,
            bottomBar={
                if(retailMode)ProtoRetailBottomBar(ProtoScreen.RetailHome,cartCount,onHome={},onCatalog=onCatalog,onCart=onCart,onExit=onRetailExit,onOrders=onOrders)
                else ProtoClientBottomBar(ProtoScreen.Home,cartCount,onHome={},onCatalog=onCatalog,onCart=onCart,onOrders=onOrders,onProfile=onProfile)
            }
        ){pad->
            Column(Modifier.fillMaxSize().padding(pad)){
                ProtoBrandHeader(unreadCount=unreadCount,onBell=onNotifications)
                Column(Modifier.padding(horizontal=18.dp)){
                    Text("Здравствуйте, "+client.firstName,color=ProtoText,fontSize=26.sp,fontWeight=FontWeight.Bold)
                    if(retailMode)Text("Режим клиента · закупочные цены скрыты",color=ProtoGoldSoft,fontSize=13.sp)
                    else Text("Статус: "+client.status+"  ·  Скидка "+client.discount+"%",color=ProtoGoldSoft,fontSize=14.sp)
                    Spacer(Modifier.height(12.dp))
                    ProtoSearchBar(query,{searchOpen=true})
                    Spacer(Modifier.height(10.dp))
                    ProtoAvailabilityChips(mode){selected->
                        mode=selected
                        onAvailability(selected)
                    }
                    Spacer(Modifier.height(5.dp))
                }
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding=PaddingValues(horizontal=18.dp,vertical=14.dp),
                    verticalArrangement=Arrangement.spacedBy(14.dp)
                ){
                    item{
                        LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                            items(protoProductGroups,key={it.key}){group->
                                Card(
                                    colors=CardDefaults.cardColors(containerColor=ProtoPanel),
                                    border=BorderStroke(1.dp,ProtoBorder),
                                    shape=RoundedCornerShape(20.dp),
                                    modifier=Modifier.width(120.dp).height(150.dp).clickable{onCategory(group.title)}
                                ){
                                    Box(Modifier.fillMaxSize()){
                                        Image(painterResource(protoPlaceholderForType(group.imageType)),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.28f)))
                                        Text(group.title,color=ProtoText,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
                                        if(!group.enabled){
                                            Text(
                                                "В разработке",
                                                color=ProtoText,
                                                fontSize=13.sp,
                                                fontWeight=FontWeight.ExtraBold,
                                                maxLines=1,
                                                softWrap=false,
                                                modifier=Modifier.align(Alignment.CenterEnd).rotate(-90f).offset(x=36.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item{
                        Column{
                            Text("Популярные товары",color=ProtoText,fontSize=24.sp,fontWeight=FontWeight.Bold,maxLines=1,softWrap=false)
                            TextButton(onClick=onSeeAll,contentPadding=PaddingValues(0.dp),modifier=Modifier.height(32.dp)){Text("Смотреть все →",color=ProtoGold)}
                        }
                    }
                    if(popular.isEmpty()){
                        item{ProtoSectionCard{Text("Популярные товары появятся после прихода на склад",color=ProtoMuted)}}
                    }else{
                        items(popular.chunked(2)){row->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                                row.forEach{p->
                                    ProtoProductCard(
                                        p=p,
                                        currentStock=availableStock(p),
                                        discount=client.discount,
                                        modifier=Modifier.weight(1f),
                                        onImage={previewProduct=p},
                                        onOpen={onOpenProduct(p)},
                                        onAddToCart={q->onAddToCart(p,q)}
                                    )
                                }
                                if(row.size==1)Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
    if(searchOpen)AlertDialog(
        onDismissRequest={searchOpen=false},
        containerColor=ProtoPanel,
        title={Text("Поиск по каталогу",color=ProtoText)},
        text={ProtoField(query,onQuery,"Артикул или название")},
        confirmButton={TextButton(onClick={searchOpen=false;onSearch()}){Text("Найти",color=ProtoGold)}},
        dismissButton={TextButton(onClick={searchOpen=false}){Text("Отмена",color=ProtoMuted)}}
    )
    previewProduct?.let{product->ProtoProductImagePreview(product){previewProduct=null}}
}
@Composable
private fun ProtoCatalogHomeScreen(cartCount:Int,onBack:()->Unit,onSearch:(String)->Unit,onCategory:(String)->Unit,onAvailability:(String)->Unit,onHome:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit,retailMode:Boolean=false,onRetailExit:()->Unit={}){
    var searchOpen by remember{mutableStateOf(false)};var searchText by remember{mutableStateOf("")};var mode by remember{mutableStateOf("Все")};val cats=listOf("Венки","Гробы","Одежда","Ленты","Цветы","Услуги")
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Scaffold(containerColor=Color.Transparent,bottomBar={if(retailMode)ProtoRetailBottomBar(ProtoScreen.RetailCatalog,cartCount,onHome,onCatalog={},onCart,onRetailExit,onOrders) else ProtoClientBottomBar(ProtoScreen.Catalog,cartCount,onHome,onCatalog={},onCart,onOrders,onProfile)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=18.dp)){
        ProtoBrandHeader(onBack=onBack);Text("Каталог",color=ProtoText,fontSize=32.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=10.dp));ProtoSearchBar(searchText,{searchOpen=true},"Поиск по категориям");Spacer(Modifier.height(12.dp));ProtoAvailabilityChips(mode){mode=it;onAvailability(it)};Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=12.dp)){items(cats){cat->val enabled=cat=="Венки";Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(20.dp),modifier=Modifier.height(205.dp).clickable(enabled=enabled){onCategory(cat)}){Box(Modifier.fillMaxSize()){Image(painterResource(protoPlaceholderForType(cat)),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.28f)));Column(Modifier.align(Alignment.BottomStart).padding(12.dp)){Text(cat,color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(if(cat=="Венки")"126 позиций" else "",color=ProtoMuted,fontSize=13.sp)};Icon(Icons.Outlined.ArrowForward,null,tint=ProtoGold,modifier=Modifier.align(Alignment.BottomEnd).padding(10.dp));if(!enabled)Text("В разработке",color=ProtoMuted,fontSize=10.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.align(Alignment.CenterEnd).rotate(-90f).offset(x=30.dp))}}}}
    }}}
    if(searchOpen)AlertDialog(onDismissRequest={searchOpen=false},containerColor=ProtoPanel,title={Text("Поиск по каталогу",color=ProtoText)},text={ProtoField(searchText,{searchText=it},"Артикул или название")},confirmButton={TextButton(onClick={if(searchText.isNotBlank()){searchOpen=false;onSearch(searchText.trim())}}){Text("Найти",color=ProtoGold)}},dismissButton={TextButton(onClick={searchOpen=false}){Text("Отмена",color=ProtoMuted)}})
}

@Composable
private fun ProtoFilterScreen(
    products:List<ProtoCatalogProduct>,
    selectedTypes:Set<String>,
    selectedQualities:Set<String>,
    selectedSizes:Set<String>,
    selectedAvailability:Set<String>,
    minPrice:Int?,
    maxPrice:Int?,
    availableStock:(ProtoCatalogProduct)->Int,
    onApply:(Set<String>,Set<String>,Set<String>,Set<String>,Int?,Int?)->Unit,
    onBack:()->Unit
) {
    var draftTypes by remember { mutableStateOf(selectedTypes) }
    var draftQualities by remember { mutableStateOf(selectedQualities) }
    var draftSizes by remember { mutableStateOf(selectedSizes) }
    var draftAvailability by remember { mutableStateOf(selectedAvailability) }
    var draftMin by remember { mutableStateOf(minPrice?.toString().orEmpty()) }
    var draftMax by remember { mutableStateOf(maxPrice?.toString().orEmpty()) }

    val qualities=products.map{it.quality}.filter{it.isNotBlank()&&it!="—"}.distinct().sorted()
    val sizes=products.map{it.size}.filter{it.isNotBlank()}.distinct().sortedBy{it.filter(Char::isDigit).toIntOrNull()?:9999}
    val catalogMin=products.minOfOrNull{it.price} ?: 0
    val catalogMax=products.maxOfOrNull{it.price} ?: 0
    val minValue=draftMin.toIntOrNull()
    val maxValue=draftMax.toIntOrNull()
    val matchedCount=products.count { p ->
        (draftTypes.isEmpty() || p.type in draftTypes) &&
        (draftQualities.isEmpty() || p.quality in draftQualities) &&
        (draftSizes.isEmpty() || p.size in draftSizes) &&
        (draftAvailability.isEmpty() || (if(availableStock(p)>0)"В наличии" else "Под заказ") in draftAvailability) &&
        (minValue==null || p.price>=minValue) &&
        (maxValue==null || p.price<=maxValue)
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.78f)),contentAlignment=Alignment.Center){
        Surface(
            color=ProtoBg,
            border=BorderStroke(1.dp,ProtoBorder),
            shape=RoundedCornerShape(24.dp),
            modifier=Modifier.fillMaxWidth().fillMaxHeight(.93f).padding(horizontal=12.dp)
        ){
            Column(Modifier.fillMaxSize().padding(16.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    IconButton(onClick=onBack,modifier=Modifier.size(42.dp).border(1.dp,ProtoBorder,CircleShape)){
                        Icon(Icons.Outlined.Close,contentDescription="Закрыть",tint=ProtoGold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text("Фильтр",color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold)
                        Text("Все параметры на одном экране",color=ProtoMuted,fontSize=11.sp)
                    }
                    TextButton(onClick={
                        draftTypes=emptySet();draftQualities=emptySet();draftSizes=emptySet();draftAvailability=emptySet();draftMin="";draftMax=""
                    }){Text("Сбросить",color=ProtoGold)}
                }
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding=PaddingValues(vertical=8.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    item{
                        ProtoCompactGrid(
                            title="Продукция",
                            options=listOf("Венки","Венки круглые","Корзины","Полянки","Флоретки","Ленты"),
                            selected=draftTypes,
                            toggle={draftTypes=protoToggle(draftTypes,it)},
                            disabled=setOf("Венки круглые","Корзины","Полянки","Флоретки","Ленты")
                        )
                    }
                    item{
                        if(qualities.isEmpty())Text("Качества будут подгружены из каталога",color=ProtoMuted)
                        else ProtoCompactGrid("Качество",qualities,draftQualities,{draftQualities=protoToggle(draftQualities,it)})
                    }
                    item{
                        if(sizes.isEmpty())Text("Размеры будут подгружены из каталога",color=ProtoMuted)
                        else ProtoCompactGrid("Размер",sizes,draftSizes,{draftSizes=protoToggle(draftSizes,it)})
                    }
                    item{
                        ProtoCompactGrid("Наличие",listOf("В наличии","Под заказ"),draftAvailability,{draftAvailability=protoToggle(draftAvailability,it)})
                    }
                    item{
                        Text("Цена",color=ProtoGoldSoft,fontSize=17.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=7.dp,bottom=6.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                            OutlinedTextField(
                                value=draftMin,
                                onValueChange={draftMin=it.filter(Char::isDigit)},
                                label={Text("от "+catalogMin+" ₽")},
                                modifier=Modifier.weight(1f),
                                singleLine=true,
                                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                                colors=protoFieldColors()
                            )
                            OutlinedTextField(
                                value=draftMax,
                                onValueChange={draftMax=it.filter(Char::isDigit)},
                                label={Text("до "+catalogMax+" ₽")},
                                modifier=Modifier.weight(1f),
                                singleLine=true,
                                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                                colors=protoFieldColors()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                ProtoPrimaryButton("Показать "+matchedCount+" позиций",{
                    onApply(draftTypes,draftQualities,draftSizes,draftAvailability,minValue,maxValue)
                })
            }
        }
    }
}
@Composable
private fun ProtoProductListScreen(
    products:List<ProtoCatalogProduct>,
    cart:SnapshotStateMap<String,Int>,
    stockOverrides:SnapshotStateMap<String,Int>,
    availableStock:(ProtoCatalogProduct)->Int,
    discount:Int,
    initialIndex:Int,
    initialOffset:Int,
    onBack:()->Unit,
    onOpenFilter:()->Unit,
    onOpenProduct:(ProtoCatalogProduct,Int,Int)->Unit,
    onHome:()->Unit,
    onCatalog:()->Unit,
    onCart:()->Unit,
    onOrders:()->Unit,
    onProfile:()->Unit,
    retailMode:Boolean=false,
    onRetailExit:()->Unit={},
    onAddToCart:(ProtoCatalogProduct,Int)->Unit={_,_->}
){
    val gridState=rememberLazyGridState(initialFirstVisibleItemIndex=initialIndex.coerceAtLeast(0),initialFirstVisibleItemScrollOffset=initialOffset.coerceAtLeast(0))
    var previewProduct by remember{mutableStateOf<ProtoCatalogProduct?>(null)}
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(
            containerColor=Color.Transparent,
            bottomBar={if(retailMode)ProtoRetailBottomBar(ProtoScreen.RetailProductList,cart.size,onHome,onCatalog,onCart,onRetailExit,onOrders) else ProtoClientBottomBar(ProtoScreen.ProductList,cart.size,onHome,onCatalog,onCart,onOrders,onProfile)}
        ){pad->
            Column(Modifier.fillMaxSize().padding(pad)){
                ProtoBrandHeader(onBack=onBack)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Column(Modifier.weight(1f)){
                        Text("Каталог",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold)
                        Text("Найдено: " + products.size + " позиций",color=ProtoMuted,fontSize=12.sp)
                    }
                    BadgedBox(badge={if(cart.size>0)Badge(containerColor=ProtoGold){Text(cart.size.toString(),color=Color.Black)}}){
                        IconButton(onClick=onCart){Icon(Icons.Outlined.ShoppingCart,null,tint=ProtoGold)}
                    }
                }
                OutlinedButton(
                    onClick=onOpenFilter,
                    modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth(),
                    border=BorderStroke(1.dp,ProtoBorder),
                    shape=RoundedCornerShape(24.dp)
                ){
                    Icon(Icons.Outlined.Tune,null,tint=ProtoGold)
                    Spacer(Modifier.width(8.dp))
                    Text("Изменить фильтр",color=ProtoGold)
                }
                if(products.isEmpty()){
                    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
                        Text("По выбранным параметрам товаров нет",color=ProtoMuted)
                    }
                }else{
                    LazyVerticalGrid(
                        columns=GridCells.Fixed(2),
                        state=gridState,
                        modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(12.dp),
                        horizontalArrangement=Arrangement.spacedBy(10.dp),
                        verticalArrangement=Arrangement.spacedBy(10.dp)
                    ){
                        items(products,key={it.sku}){p->
                            ProtoProductCard(
                                p=p,
                                currentStock=availableStock(p),
                                discount=discount,
                                onImage={previewProduct=p},
                                onOpen={onOpenProduct(p,gridState.firstVisibleItemIndex,gridState.firstVisibleItemScrollOffset)},
                                onAddToCart={q->onAddToCart(p,q)}
                            )
                        }
                    }
                }
            }
        }
    }
    previewProduct?.let{product->ProtoProductImagePreview(product){previewProduct=null}}
}
@Composable
private fun ProtoProductCard(
    p:ProtoCatalogProduct,
    currentStock:Int,
    discount:Int,
    modifier:Modifier=Modifier,
    onImage:()->Unit={},
    onOpen:()->Unit,
    onAddToCart:(Int)->Unit={}
) {
    var qty by remember(p.sku){mutableStateOf(1)}
    Card(
        colors=CardDefaults.cardColors(containerColor=ProtoPanel),
        border=BorderStroke(1.dp,ProtoBorder),
        shape=RoundedCornerShape(15.dp),
        modifier=modifier.height(372.dp)
    ){
        Box(Modifier.fillMaxWidth().height(150.dp).background(ProtoPanel2).clickable(onClick=onImage)){
            ProtoProductImage(p,Modifier.fillMaxSize())
            Surface(
                Modifier.align(Alignment.BottomStart).padding(7.dp),
                color=if(currentStock>0)Color(0xDD123A27)else Color(0xDD4A2220),
                shape=RoundedCornerShape(18.dp)
            ){
                Text(
                    if(currentStock>0)"В наличии $currentStock" else "Под заказ · ${p.productionDays} дн.",
                    color=if(currentStock>0)ProtoGreen else ProtoGoldSoft,
                    fontSize=9.sp,
                    modifier=Modifier.padding(horizontal=7.dp,vertical=3.dp)
                )
            }
        }
        Column(Modifier.padding(horizontal=9.dp,vertical=8.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(
                p.name,
                color=ProtoText,
                fontWeight=FontWeight.Bold,
                fontSize=14.sp,
                lineHeight=17.sp,
                maxLines=2,
                overflow=TextOverflow.Ellipsis,
                modifier=Modifier.height(34.dp)
            )
            Text("Артикул: "+p.sku,color=ProtoGoldSoft,fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(if(p.size.isBlank()||p.size=="—")" " else "Размер: "+p.size,color=ProtoMuted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(protoMoney(p.price),color=ProtoText,fontWeight=FontWeight.Bold,fontSize=20.sp)
            OutlinedButton(
                onClick=onOpen,
                modifier=Modifier.fillMaxWidth().height(38.dp),
                contentPadding=PaddingValues(horizontal=4.dp),
                border=BorderStroke(1.dp,ProtoGold),
                shape=RoundedCornerShape(10.dp)
            ){
                Text("ПОДРОБНЕЕ",color=ProtoGold,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
            }
            Row(Modifier.fillMaxWidth().height(34.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                ProtoQtyButton(Icons.Outlined.Remove,{qty=(qty-1).coerceAtLeast(1)},34.dp)
                Text(qty.toString(),color=ProtoText,fontSize=16.sp,fontWeight=FontWeight.Bold)
                ProtoQtyButton(Icons.Outlined.Add,{qty+=1},34.dp)
            }
            Button(
                onClick={onAddToCart(qty);qty=1},
                modifier=Modifier.fillMaxWidth().height(38.dp),
                contentPadding=PaddingValues(horizontal=4.dp),
                colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),
                shape=RoundedCornerShape(10.dp)
            ){
                Text("В КОРЗИНУ",color=Color.Black,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
            }
        }
    }
}
@Composable
private fun ProtoProductDetailScreen(
    product:ProtoCatalogProduct?,
    currentStock:Int,
    qty:Int,
    discount:Int,
    cartCount:Int,
    editingCart:Boolean,
    onBack:()->Unit,
    onMinus:()->Unit,
    onPlus:()->Unit,
    onAdd:()->Unit,
    onHome:()->Unit,
    onCatalog:()->Unit,
    onCart:()->Unit,
    onOrders:()->Unit,
    onProfile:()->Unit,
    retailMode:Boolean=false,
    onRetailExit:()->Unit={}
) {
    val p=product?:return
    var preview by remember{mutableStateOf(false)}
    val discountedUnit=p.price*(100-discount)/100
    val base=p.price*qty
    val total=discountedUnit*qty
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(containerColor=Color.Transparent,bottomBar={if(retailMode)ProtoRetailBottomBar(ProtoScreen.RetailProductDetail,cartCount,onHome,onCatalog,onCart,onRetailExit,onOrders) else ProtoClientBottomBar(ProtoScreen.ProductDetail,cartCount,onHome,onCatalog,onCart,onOrders,onProfile)}){pad->
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                item{ProtoBrandHeader(onBack=onBack)}
                item{
                    Card(
                        colors=CardDefaults.cardColors(containerColor=ProtoPanel),
                        border=BorderStroke(1.dp,ProtoBorder),
                        shape=RoundedCornerShape(22.dp),
                        modifier=Modifier.fillMaxWidth().height(365.dp).clickable{preview=true}
                    ){ProtoProductImage(p,Modifier.fillMaxSize(),ContentScale.Fit)}
                }
                item{
                    Text(p.name,color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    ProtoSkuText(p.sku,extra=" · "+p.size,fontSize=15)
                }
                item{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(protoMoney(p.price),color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        Text(if(currentStock>0)"В наличии $currentStock шт." else "Под заказ · от "+p.productionDays+" дней",color=if(currentStock>0)ProtoGreen else ProtoGoldSoft,fontSize=12.sp)
                    }
                }
                if(qty>currentStock)item{Text("В наличии "+currentStock+" шт., остальное под заказ · от "+p.productionDays+" дней",color=ProtoOrange,fontSize=12.sp)}
                item{
                    ProtoSectionCard{
                        Text("Количество",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical=8.dp),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.Center
                        ){
                            ProtoQtyButton(Icons.Outlined.Remove,onMinus,size=42.dp)
                            Text(qty.toString(),color=ProtoText,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=22.dp))
                            ProtoQtyButton(Icons.Outlined.Add,onPlus,size=42.dp)
                        }
                        HorizontalDivider(color=ProtoBorder,modifier=Modifier.padding(vertical=6.dp))
                        ProtoInfoRow("Цена за 1 шт.",protoMoney(p.price))
                        if(discount>0)ProtoInfoRow("Скидка клиента "+discount+"%","−"+protoMoney(base-total))
                        ProtoInfoRow("Количество",qty.toString()+" шт.")
                        ProtoInfoRow("Итого",protoMoney(total))
                        Spacer(Modifier.height(10.dp))
                        ProtoPrimaryButton(if(editingCart)"Сохранить в корзине" else "Добавить в корзину",onAdd)
                    }
                }
                item{ProtoSectionCard{Text("Характеристики",color=ProtoGoldSoft,fontSize=16.sp,fontWeight=FontWeight.Bold);ProtoInfoRow("Категория",p.type);ProtoInfoRow("Качество",p.quality);ProtoInfoRow("Размер",p.size)}}
            }
        }
    }
    if(preview)ProtoProductImagePreview(p){preview=false}
}
@Composable
private fun ProtoCartScreen(
    products:List<ProtoCatalogProduct>,
    cart:SnapshotStateMap<String,Int>,
    discount:Int,
    availableStock:(ProtoCatalogProduct)->Int,
    onBack:()->Unit,
    onPlus:(ProtoCatalogProduct)->Unit,
    onMinus:(ProtoCatalogProduct)->Unit,
    onDelete:(ProtoCatalogProduct)->Unit,
    onOpenProduct:(ProtoCatalogProduct)->Unit,
    onCheckout:()->Unit,
    onHome:()->Unit,
    onCatalog:()->Unit,
    onOrders:()->Unit,
    onProfile:()->Unit,
    retailMode:Boolean=false,
    onRetailExit:()->Unit={}
) {
    val lines=cart.mapNotNull{(sku,q)->products.firstOrNull{it.sku==sku}?.let{it to q}}
    val base=lines.sumOf{it.first.price*it.second}
    val total=base*(100-discount)/100
    var previewProduct by remember{mutableStateOf<ProtoCatalogProduct?>(null)}
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(containerColor=Color.Transparent,bottomBar={if(retailMode)ProtoRetailBottomBar(ProtoScreen.RetailCart,cart.size,onHome,onCatalog,onCart={},onRetailExit,onOrders) else ProtoClientBottomBar(ProtoScreen.Cart,cart.size,onHome,onCatalog,onCart={},onOrders,onProfile)}){pad->
            Column(Modifier.fillMaxSize().padding(pad)){
                ProtoBrandHeader(onBack=onBack)
                Text("Корзина",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=18.dp,vertical=8.dp))
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    if(lines.isEmpty())item{ProtoSectionCard{Text("Корзина пуста",color=ProtoMuted)}}
                    items(lines,key={it.first.sku}){pair->
                        val p=pair.first
                        val q=pair.second
                        val available=availableStock(p)
                        ProtoSectionCard{
                            Row(verticalAlignment=Alignment.CenterVertically){
                                ProtoProductImage(
                                    p,
                                    Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)).clickable{previewProduct=p}
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f).clickable{onOpenProduct(p)}){
                                    Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    ProtoSkuText(p.sku)
                                    if(q>available)Text("В наличии "+available+" шт., остальное под заказ",color=ProtoOrange,fontSize=10.sp)
                                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.padding(top=6.dp)){
                                        ProtoQtyButton(Icons.Outlined.Remove,{onMinus(p)})
                                        Text(q.toString(),color=ProtoText,fontWeight=FontWeight.Bold)
                                        ProtoQtyButton(Icons.Outlined.Add,{onPlus(p)})
                                    }
                                }
                                Column(horizontalAlignment=Alignment.End){
                                    Text(protoMoney(p.price*q*(100-discount)/100),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                                    IconButton(onClick={onDelete(p)}){Icon(Icons.Outlined.Delete,null,tint=ProtoGold)}
                                }
                            }
                        }
                    }
                }
                ProtoSectionCard(Modifier.padding(horizontal=18.dp,vertical=8.dp)){
                    ProtoInfoRow("Товары",protoMoney(base))
                    ProtoInfoRow("Скидка клиента "+discount+"%","−"+protoMoney(base-total))
                    HorizontalDivider(color=ProtoBorder,modifier=Modifier.padding(vertical=8.dp))
                    Row(Modifier.fillMaxWidth()){
                        Text("Итого",color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(protoMoney(total),color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold)
                    }
                }
                ProtoPrimaryButton("Оформить заказ",onCheckout,lines.isNotEmpty(),Modifier.padding(horizontal=18.dp,vertical=8.dp))
            }
        }
    }
    previewProduct?.let{product->ProtoProductImagePreview(product){previewProduct=null}}
}
@Composable
private fun ProtoCheckoutScreen(
    cartPositions:Int,
    totalPieces:Int,
    baseTotal:Int,
    finalTotal:Int,
    discount:Int,
    defaultAddress:String,
    contacts:List<SansaraCompanyContact>,
    clientStatus:String,
    earliestDate:LocalDate,
    shortages:List<String>,
    onBack:()->Unit,
    onSubmit:(String,String,String,String,String,String,String)->Unit,
    onHome:()->Unit,
    onCatalog:()->Unit,
    onCart:()->Unit,
    onOrders:()->Unit,
    onProfile:()->Unit,
    retailMode:Boolean=false,
    onRetailExit:()->Unit={}
){
    var method by remember{mutableStateOf("Доставка")}
    var address by remember(defaultAddress){mutableStateOf(defaultAddress)}
    var comment by remember{mutableStateOf("")}
    var selectedContact by remember(contacts){mutableStateOf(contacts.firstOrNull())}
    var selectedDate by remember(earliestDate){mutableStateOf(earliestDate)}
    var selectedTime by remember{mutableStateOf("до 12:00")}
    var showCalendar by remember{mutableStateOf(false)}

    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(containerColor=Color.Transparent,bottomBar={if(retailMode)ProtoRetailBottomBar(ProtoScreen.RetailCheckout,cartPositions,onHome,onCatalog,onCart,onRetailExit,onOrders) else ProtoClientBottomBar(ProtoScreen.Checkout,cartPositions,onHome,onCatalog,onCart,onOrders,onProfile)}){pad->
            Column(Modifier.fillMaxSize().padding(pad)){
                ProtoBrandHeader(onBack=onBack)
                Text("Оформление заказа",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=18.dp,vertical=8.dp))
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    item{
                        ProtoSectionCard{
                            Text("Получатель",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            contacts.forEach{contact->
                                val active=selectedContact?.userId==contact.userId
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable{selectedContact=contact}.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                                    RadioButton(selected=active,onClick={selectedContact=contact},colors=RadioButtonDefaults.colors(selectedColor=ProtoGold))
                                    Column{Text(contact.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(contact.phone,color=ProtoMuted,fontSize=11.sp)}
                                }
                            }
                        }
                    }
                    item{
                        ProtoSectionCard{
                            Text("Способ получения",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                listOf("Доставка","Самовывоз","ТК").forEach{label->
                                    val active=method==label
                                    Surface(color=if(active)ProtoGold else ProtoPanel2,border=BorderStroke(1.dp,if(active)ProtoGold else ProtoBorder),shape=RoundedCornerShape(22.dp),modifier=Modifier.weight(1f).height(44.dp).clickable{method=label}){
                                        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(label,color=if(active)Color.Black else ProtoText,fontSize=11.sp,fontWeight=FontWeight.SemiBold)}
                                    }
                                }
                            }
                        }
                    }
                    item{ProtoField(address,{address=it},if(method=="Самовывоз")"Точка самовывоза" else if(method=="ТК")"Транспортная компания и город" else "Адрес доставки")}
                    item{
                        ProtoSectionCard(Modifier.clickable{showCalendar=true}){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Icon(Icons.Outlined.CalendarMonth,null,tint=ProtoGold)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)){Text("Дата доставки",color=ProtoMuted,fontSize=11.sp);Text(selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),color=ProtoText,fontWeight=FontWeight.SemiBold)}
                                Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)
                            }
                        }
                    }
                    item{
                        Text("Время",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            listOf("до 12:00","12:00–15:00","15:00–18:00").forEach{slot->
                                val active=selectedTime==slot
                                Surface(color=if(active)ProtoGold else ProtoPanel,border=BorderStroke(1.dp,if(active)ProtoGold else ProtoBorder),shape=RoundedCornerShape(20.dp),modifier=Modifier.weight(1f).height(44.dp).clickable{selectedTime=slot}){
                                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(slot,color=if(active)Color.Black else ProtoText,fontSize=10.sp,fontWeight=FontWeight.SemiBold)}
                                }
                            }
                        }
                    }
                    if(shortages.isNotEmpty())item{
                        ProtoSectionCard{
                            Text("Часть заказа будет произведена",color=ProtoOrange,fontWeight=FontWeight.Bold)
                            shortages.forEach{Text(it,color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=3.dp))}
                            Text("Минимальная дата: "+earliestDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),color=ProtoGoldSoft,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
                        }
                    }
                    item{
                        OutlinedTextField(
                            value=comment,
                            onValueChange={if(it.length<=500)comment=it},
                            label={Text("Комментарий")},
                            modifier=Modifier.fillMaxWidth(),
                            minLines=3,
                            maxLines=5,
                            colors=protoFieldColors()
                        )
                        Text(comment.length.toString()+"/500",color=ProtoMuted,fontSize=10.sp,modifier=Modifier.fillMaxWidth().padding(top=2.dp))
                    }
                    item{
                        ProtoSectionCard{
                            ProtoInfoRow("Статус",clientStatus)
                            ProtoInfoRow("Товары",protoMoney(baseTotal))
                            ProtoInfoRow("Скидка клиента "+discount+"%","−"+protoMoney(baseTotal-finalTotal))
                            ProtoInfoRow("Всего",totalPieces.toString()+" шт.")
                            HorizontalDivider(color=ProtoBorder,modifier=Modifier.padding(vertical=7.dp))
                            ProtoInfoRow("Итого",protoMoney(finalTotal))
                        }
                    }
                }
                ProtoPrimaryButton(
                    "Отправить заказ",
                    {
                        val contact=selectedContact
                        if(contact!=null)onSubmit(method,address,comment,contact.name,contact.phone,selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),selectedTime)
                    },
                    enabled=selectedContact!=null && address.isNotBlank(),
                    modifier=Modifier.padding(18.dp)
                )
            }
        }
    }
    if(showCalendar){
        ProtoProductionCalendarDialog(
            selectedDate=selectedDate,
            onDismiss={showCalendar=false},
            onSelect={date->selectedDate=if(date.isBefore(earliestDate))earliestDate else date;showCalendar=false}
        )
    }
}

@Composable
private fun ProtoOrderSentScreen(
    order: ProtoOrder?,
    onView: () -> Unit,
    onCatalog: () -> Unit,
    onHome: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit
) {
    val currentOrder = order ?: return

    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                ProtoClientBottomBar(
                    current = ProtoScreen.OrderSent,
                    cartCount = 0,
                    onHome = onHome,
                    onCatalog = onCatalog,
                    onCart = onCart,
                    onOrders = onOrders,
                    onProfile = onProfile
                )
            }
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProtoBrandHeader()
                Spacer(Modifier.height(32.dp))
                Box(Modifier.size(112.dp).border(3.dp, ProtoGold, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, null, tint = ProtoGold, modifier = Modifier.size(64.dp))
                }
                Text("Заказ отправлен", color = ProtoText, fontSize = 31.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp))
                Text(
                    "Ваша заявка передана в SANSARA. Менеджер получил заказ на сборку и доставку.",
                    color = ProtoMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                ProtoSectionCard {
                    Text("Заказ № " + currentOrder.id, color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    ProtoInfoRow("Статус", currentOrder.status)
                    ProtoInfoRow("Получатель", currentOrder.clientName)
                    ProtoInfoRow("Сумма", protoMoney(currentOrder.total))
                    ProtoInfoRow("Получение", currentOrder.deliveryMethod)
                }
                Spacer(Modifier.weight(1f))
                ProtoPrimaryButton("Смотреть заказ", onView)
                Spacer(Modifier.height(10.dp))
                ProtoSecondaryButton("Вернуться в каталог", onCatalog)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProtoOrderListScreen(
    orders: List<ProtoOrder>,
    products: List<ProtoCatalogProduct>,
    onBack: () -> Unit,
    onOpen: (ProtoOrder) -> Unit,
    onHome: () -> Unit,
    onCatalog: () -> Unit,
    onCart: () -> Unit,
    onProfile: () -> Unit,
    retailMode:Boolean=false,
    onRetailExit:()->Unit={},
    cartCount:Int=0
) {
    var tab by remember { mutableStateOf("Текущие") }
    var historyFilterOpen by remember { mutableStateOf(false) }
    var historyPeriod by remember { mutableStateOf("Все") }
    var expandedMonths by remember { mutableStateOf(setOf<String>()) }

    val currentOrders=orders.filter { it.status !in setOf("Доставлен","Отменён") }
    val allHistory=orders.filter { it.status in setOf("Доставлен","Отменён") }
    val history=protoFilterOrdersByPeriod(allHistory,historyPeriod)
    val grouped=history.groupBy { protoOrderMonthKey(it.dateTime) }
        .toList()
        .sortedByDescending { it.first }

    LaunchedEffect(grouped.size){
        if(expandedMonths.isEmpty() && grouped.isNotEmpty()) expandedMonths=setOf(grouped.first().first)
    }

    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if(retailMode) {
                    ProtoRetailBottomBar(
                        current=ProtoScreen.RetailOrderList,
                        cartCount=cartCount,
                        onHome=onHome,
                        onCatalog=onCatalog,
                        onCart=onCart,
                        onExit=onRetailExit,
                        onOrders={}
                    )
                } else {
                    ProtoClientBottomBar(
                        current = ProtoScreen.OrderList,
                        cartCount = cartCount,
                        onHome = onHome,
                        onCatalog = onCatalog,
                        onCart = onCart,
                        onOrders = {},
                        onProfile = onProfile
                    )
                }
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                ProtoBrandHeader(onBack = onBack)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal=16.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Text("Мои заказы", color = ProtoText, fontSize = 30.sp, fontWeight = FontWeight.Bold,modifier=Modifier.weight(1f))
                    if(tab=="История"){
                        IconButton(onClick={historyFilterOpen=true}){
                            Icon(Icons.Outlined.Tune,contentDescription="Фильтр истории",tint=ProtoGold)
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Текущие", "История").forEach { label ->
                                val selected = tab == label
                                Box(
                                    Modifier.weight(1f).height(42.dp)
                                        .clip(RoundedCornerShape(21.dp))
                                        .background(if (selected) ProtoGold else ProtoPanel)
                                        .border(1.dp, if (selected) ProtoGold else ProtoBorder, RoundedCornerShape(21.dp))
                                        .clickable { tab = label },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (selected) Color.Black else ProtoText, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    if(tab=="Текущие"){
                        if(currentOrders.isEmpty()){
                            item{ProtoSectionCard{Text("Текущих заказов пока нет",color=ProtoMuted)}}
                        }else{
                            items(currentOrders,key={it.id}){order->ProtoOrderRow(order){onOpen(order)}}
                        }
                    }else{
                        if(grouped.isEmpty()){
                            item{ProtoSectionCard{Text("Завершённых заказов пока нет",color=ProtoMuted)}}
                        }else{
                            grouped.forEach { (monthKey,monthOrders) ->
                                item(key="month-"+monthKey){
                                    val expanded=monthKey in expandedMonths
                                    ProtoSectionCard(Modifier.clickable{
                                        expandedMonths=if(expanded)expandedMonths-monthKey else expandedMonths+monthKey
                                    }){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Column(Modifier.weight(1f)){
                                                Text(protoMonthLabel(monthKey),color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.Bold)
                                                Text(monthOrders.size.toString()+" заказов · "+protoMoney(monthOrders.sumOf{it.total}),color=ProtoMuted,fontSize=11.sp)
                                            }
                                            Icon(if(expanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null,tint=ProtoGold)
                                        }
                                    }
                                }
                                if(monthKey in expandedMonths){
                                    items(monthOrders.sortedByDescending{it.dateTime},key={it.id}){order->ProtoOrderRow(order){onOpen(order)}}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if(historyFilterOpen){
        ProtoOrderHistoryFilterDialog(
            period=historyPeriod,
            orders=history,
            products=products,
            onPeriod={historyPeriod=it},
            onDismiss={historyFilterOpen=false}
        )
    }
}
@Composable
private fun ProtoOrderDetailScreen(
    order:ProtoOrder?,
    products:List<ProtoCatalogProduct>,
    isAdmin:Boolean,
    onBack:()->Unit,
    onStatus:(String)->Unit,
    onRepeat:(()->Unit)?=null,
    onEditRepeat:(()->Unit)?=null,
    onCancel:(()->Unit)?=null
){
    val o=order?:return
    val statuses=listOf("Получен","Подтверждён","Собирается","Доставляется","Доставлен")
    val currentIndex=statuses.indexOf(o.status)
    val latestEvent=o.history.lastOrNull{it.status==o.status} ?: o.history.lastOrNull()

    ProtoScaffold("Заказ №"+o.id,o.clientName+" · "+protoMoney(o.total),onBack){
        item{
            ProtoSectionCard{
                Text("Статус",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if(isAdmin){
                    if(o.status=="Отменён"){
                        Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(12.dp).background(ProtoRed,CircleShape));Spacer(Modifier.width(9.dp));Text("Отменён",color=ProtoRed,fontWeight=FontWeight.Bold)}
                    }else{
                        statuses.forEachIndexed{i,status->
                            val reached=currentIndex>=i
                            Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                                Box(Modifier.size(12.dp).background(if(reached)ProtoGreen else ProtoBorder,CircleShape))
                                Spacer(Modifier.width(9.dp))
                                Text(status,color=if(reached)ProtoText else ProtoMuted,modifier=Modifier.weight(1f))
                                RadioButton(selected=o.status==status,onClick={if(i>=currentIndex)onStatus(status)},enabled=i>=currentIndex,colors=RadioButtonDefaults.colors(selectedColor=ProtoGold))
                            }
                        }
                    }
                }else{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(12.dp).background(if(o.status=="Отменён")ProtoRed else if(o.status=="Доставлен")ProtoGreen else ProtoGold,CircleShape))
                        Spacer(Modifier.width(9.dp))
                        Column{
                            Text(o.status,color=if(o.status=="Отменён")ProtoRed else ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)
                            latestEvent?.let{Text(it.dateTime,color=ProtoMuted,fontSize=11.sp)}
                        }
                    }
                }
            }
        }

        item{Text("Состав заказа",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)}
        items(o.lines,key={it.sku}){line->
            val product=products.firstOrNull{it.sku==line.sku}
            ProtoSectionCard{
                Row(verticalAlignment=Alignment.CenterVertically){
                    if(product!=null){
                        ProtoProductImage(product,Modifier.size(74.dp).clip(RoundedCornerShape(11.dp)))
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)){
                        Text(line.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                        ProtoSkuText(line.sku,fontSize=13)
                        Text(line.qty.toString()+" шт. × "+protoMoney(line.price),color=ProtoMuted,fontSize=11.sp)
                    }
                    Text(protoMoney(line.lineTotal),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                }
            }
        }

        item{
            ProtoSectionCard{
                ProtoInfoRow("Товары",protoMoney(o.baseTotal))
                ProtoInfoRow("Скидка клиента "+o.discountPct+"%","−"+protoMoney(o.baseTotal-o.total))
                HorizontalDivider(color=ProtoBorder,modifier=Modifier.padding(vertical=7.dp))
                ProtoInfoRow("Итого",protoMoney(o.total))
            }
        }

        if(!isAdmin){
            item{
                Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
                    if(onRepeat!=null)ProtoPrimaryButton("Заказать повторно",onRepeat)
                    if(onEditRepeat!=null)ProtoSecondaryButton("Изменить и заказать",onEditRepeat)
                    if(o.status=="Получен" && onCancel!=null){
                        OutlinedButton(onClick=onCancel,modifier=Modifier.fillMaxWidth().height(48.dp),border=BorderStroke(1.dp,ProtoRed),shape=RoundedCornerShape(24.dp)){
                            Text("Отменить заказ",color=ProtoRed)
                        }
                    }
                }
            }
        }

        item{
            ProtoSectionCard{
                if(o.recipient.isNotBlank())ProtoInfoRow("Получатель",o.recipient)
                if(o.contactPhone.isNotBlank())ProtoInfoRow("Контакт",o.contactPhone)
                ProtoInfoRow("Способ получения",o.deliveryMethod)
                if(o.deliveryAddress.isNotBlank())ProtoInfoRow("Адрес",o.deliveryAddress)
                if(o.deliveryDate.isNotBlank())ProtoInfoRow("Дата",o.deliveryDate)
                if(o.deliveryTime.isNotBlank())ProtoInfoRow("Время",o.deliveryTime)
                if(o.comment.isNotBlank()){
                    Text("Комментарий",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
                    Text(o.comment,color=ProtoText)
                }
            }
        }

        if(isAdmin){
            item{Text("История заказа",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)}
            items(o.history.asReversed()){event->
                ProtoSectionCard{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Outlined.History,null,tint=if(event.status=="Отменён")ProtoRed else ProtoGold)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)){Text(event.status,color=if(event.status=="Отменён")ProtoRed else ProtoText,fontWeight=FontWeight.SemiBold);Text(event.dateTime,color=ProtoMuted,fontSize=11.sp)}
                        Text(event.actor,color=ProtoGoldSoft,fontSize=11.sp)
                    }
                }
            }
        }
    }
}
@Composable
private fun ProtoOrderHistoryFilterDialog(
    period:String,
    orders:List<ProtoOrder>,
    products:List<ProtoCatalogProduct>,
    onPeriod:(String)->Unit,
    onDismiss:()->Unit
){
    val groups=orders.flatMap{order->order.lines}.groupBy{line->
        val p=products.firstOrNull{it.sku==line.sku}
        when {
            p==null -> "Прочее"
            p.quality.isBlank() || p.quality=="—" -> p.type
            else -> p.type+" · "+p.quality
        }
    }.mapValues{(_,lines)->lines.sumOf{it.qty}}.toList().sortedByDescending{it.second}
    AlertDialog(
        onDismissRequest=onDismiss,
        containerColor=ProtoBg,
        title={Text("Фильтр истории",color=ProtoText)},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=540.dp)){
                Text("Период",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.padding(vertical=8.dp)){
                    items(listOf("Все","30 дней","90 дней","Год")){label->
                        FilterChip(
                            selected=period==label,
                            onClick={onPeriod(label)},
                            label={Text(label)},
                            colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText)
                        )
                    }
                }
                ProtoSectionCard{
                    ProtoInfoRow("Заказов",orders.size.toString())
                    ProtoInfoRow("Общая сумма",protoMoney(orders.sumOf{it.total}))
                }
                Spacer(Modifier.height(10.dp))
                Text("Отчёт по позициям",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp),contentPadding=PaddingValues(top=6.dp)){
                    if(groups.isEmpty())item{Text("Нет данных за выбранный период",color=ProtoMuted)}
                    else items(groups){(group,qty)->ProtoInfoRow(group,qty.toString()+" шт.")}
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Готово",color=ProtoGold)}}
    )
}

@Composable
private fun ProtoNotificationsScreen(
    notifications:List<ProtoNotification>,
    onBack:()->Unit,
    onReadAll:()->Unit,
    onDelete:(String)->Unit
){
    LaunchedEffect(Unit){onReadAll()}
    ProtoScaffold("Уведомления","Изменения статусов заказов",onBack){
        if(notifications.isEmpty()){
            item{ProtoSectionCard{Text("Новых уведомлений нет",color=ProtoMuted)}}
        }else{
            items(notifications,key={it.id}){notification->
                ProtoSectionCard{
                    Row(verticalAlignment=Alignment.Top){
                        Column(Modifier.weight(1f)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                if(!notification.read){
                                    Box(Modifier.size(8.dp).background(ProtoGold,CircleShape))
                                    Spacer(Modifier.width(7.dp))
                                }
                                Text(notification.title,color=ProtoText,fontWeight=FontWeight.Bold)
                            }
                            Text(notification.message,color=ProtoMuted,fontSize=12.sp,modifier=Modifier.padding(top=5.dp))
                            Text(notification.dateTime,color=ProtoGoldSoft,fontSize=10.sp,modifier=Modifier.padding(top=7.dp))
                        }
                        IconButton(onClick={onDelete(notification.id)}){
                            Icon(Icons.Outlined.DeleteOutline,contentDescription="Удалить",tint=ProtoMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtoProfileScreen(
    client:ProtoClient,
    onBack:()->Unit,
    onCall:()->Unit,
    onLogout:()->Unit,
    onHome:()->Unit,
    onCatalog:()->Unit,
    onCart:()->Unit,
    onOrders:()->Unit
){
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(
            containerColor=Color.Transparent,
            bottomBar={ProtoClientBottomBar(ProtoScreen.Profile,0,onHome,onCatalog,onCart,onOrders,onProfile={})}
        ){pad->
            Column(Modifier.fillMaxSize().padding(pad)){
                ProtoBrandHeader(onBack=onBack)
                Text("Профиль",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)
                ){
                    item{
                        ProtoSectionCard{
                            Text(client.name,color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)
                            ProtoInfoRow("Статус",client.status)
                            ProtoInfoRow("Скидка",client.discount.toString()+"%")
                            ProtoInfoRow("Контакт",client.contact)
                            ProtoInfoRow("Телефон",client.phone)
                            ProtoInfoRow("Администратор",BuildConfig.ADMIN_PHONE)
                        }
                    }
                    item{ProtoSecondaryButton("Позвонить администратору",onCall)}
                    item{TextButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Выйти",color=ProtoMuted)}}
                }
            }
        }
    }
}

@Composable
private fun ProtoSuspendedScreen(onCall:()->Unit,onMessage:()->Unit,onBack:()->Unit,onCatalog:()->Unit,onHome:()->Unit,onOrders:()->Unit){
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Scaffold(containerColor=Color.Transparent,bottomBar={ProtoClientBottomBar(ProtoScreen.Suspended,0,onHome,onCatalog,onCart={},onOrders=onOrders,onProfile={})}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=18.dp)){ProtoBrandHeader(onBack=onBack);Text("Профиль",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=8.dp));ProtoSectionCard{Text("Ваш статус временно приостановлен.",color=ProtoRed,fontSize=24.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Text("Самостоятельное оформление заказов недоступно. Пожалуйста, свяжитесь с администратором.",color=ProtoMuted,lineHeight=20.sp)};Spacer(Modifier.height(12.dp));ProtoSectionCard{ProtoInfoRow("Телефон администратора",BuildConfig.ADMIN_PHONE)};Spacer(Modifier.height(12.dp));ProtoPrimaryButton("Позвонить администратору",onCall);Spacer(Modifier.height(10.dp));ProtoSecondaryButton("Написать менеджеру",onMessage)}}}
}

@Composable
private fun ProtoClientMenuOverlay(
    onDismiss:()->Unit,
    onChat:()->Unit,
    onProfile:()->Unit,
    onCustomers:()->Unit,
    onReports:()->Unit,
    onSettings:()->Unit
){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.72f)).clickable{onDismiss()}){
        Surface(
            color=ProtoBg,
            border=BorderStroke(1.dp,ProtoBorder),
            modifier=Modifier.fillMaxHeight().fillMaxWidth(.82f).clickable(enabled=false){}
        ){
            Column(Modifier.fillMaxSize().padding(20.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Image(painter=painterResource(R.drawable.sansara_brand_header),contentDescription="SANSARA",modifier=Modifier.weight(1f).height(52.dp),contentScale=ContentScale.Fit)
                    IconButton(onClick=onDismiss){Icon(Icons.Outlined.Close,null,tint=ProtoGold)}
                }
                Spacer(Modifier.height(24.dp))
                listOf(
                    Triple("Чат с менеджером",Icons.Outlined.ChatBubbleOutline,onChat),
                    Triple("Профиль",Icons.Outlined.Person,onProfile),
                    Triple("Для клиентов",Icons.Outlined.Storefront,onCustomers),
                    Triple("Формирование отчётности",Icons.Outlined.Assessment,onReports),
                    Triple("Настройки",Icons.Outlined.Settings,onSettings)
                ).forEach { (title,icon,action) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{action()}.padding(horizontal=12.dp,vertical=15.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Box(Modifier.size(40.dp).background(ProtoPanel2,RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){
                            Icon(icon,null,tint=ProtoGold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(title,color=ProtoText,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                        Icon(Icons.Outlined.ChevronRight,null,tint=ProtoMuted)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("SANSARA · ${BuildConfig.VERSION_NAME}",color=ProtoMuted,fontSize=10.sp)
            }
        }
    }
}

@Composable
private fun ProtoClientReportsScreen(orders:List<ProtoOrder>,onBack:()->Unit){
    val delivered=orders.filter{it.status=="Доставлен"}
    val total=orders.sumOf{it.total}
    ProtoScaffold("Отчётность","Ваши заказы и оборот",onBack){
        item{
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                ProtoMetricCard("Заказов",orders.size.toString(),"всего",Modifier.weight(1f)){}
                ProtoMetricCard("Доставлено",delivered.size.toString(),"",Modifier.weight(1f)){}
            }
        }
        item{ProtoMetricCard("Сумма заказов",protoMoney(total),"за весь период",Modifier.fillMaxWidth()){}}
        item{Text("Подробная история",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)}
        items(orders.sortedByDescending{it.dateTime},key={it.id}){order->ProtoOrderRow(order){}}
    }
}

@Composable
private fun ProtoClientSettingsScreen(notificationsEnabled:Boolean,onNotifications:(Boolean)->Unit,onBack:()->Unit){
    ProtoScaffold("Настройки","Параметры клиентского приложения",onBack){
        item{ProtoSectionCard{ProtoSwitchRow("Уведомления о заказах",notificationsEnabled,onNotifications);Text("Уведомления о смене статуса отображаются на колокольчике главного экрана.",color=ProtoMuted,fontSize=10.sp)}}
        item{ProtoSectionCard{ProtoInfoRow("Версия",BuildConfig.VERSION_NAME);ProtoInfoRow("Режим","Клиент")}}
    }
}

@Composable
private fun ProtoAgentClientsScreen(
    customers:List<SansaraAgentCustomer>,
    settings:SansaraAgentSettings,
    onBack:()->Unit,
    onSaveSettings:(Boolean,Int)->Unit,
    onAdd:(String,String,String,String,String,String)->Unit,
    onOpen:(SansaraAgentCustomer)->Unit
){
    var enabled by remember(settings.ownerClientId,settings.generalMarkupEnabled){mutableStateOf(settings.generalMarkupEnabled)}
    var pctText by remember(settings.ownerClientId,settings.generalMarkupPct){mutableStateOf(settings.generalMarkupPct.toString())}
    var showAdd by remember{mutableStateOf(false)}
    ProtoScaffold("Для клиентов","Частные клиенты и розничная наценка",onBack){
        item{
            ProtoSectionCard{
                ProtoSwitchRow("Общая наценка",enabled){enabled=it}
                ProtoField(pctText,{pctText=it.filter(Char::isDigit).take(3)},"Наценка, %",KeyboardType.Number)
                ProtoPrimaryButton("Сохранить наценку",{onSaveSettings(enabled,pctText.toIntOrNull()?:0)})
            }
        }
        item{ProtoPrimaryButton("Добавить частного клиента",{showAdd=true})}
        if(customers.isEmpty())item{ProtoSectionCard{Text("Частных клиентов пока нет",color=ProtoMuted)}}
        items(customers,key={it.id}){c->
            ProtoSectionCard(Modifier.clickable{onOpen(c)}){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(44.dp).background(ProtoPanel2,CircleShape),contentAlignment=Alignment.Center){Text(c.fullName.take(1).uppercase(),color=ProtoGold,fontWeight=FontWeight.Bold)}
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){Text(c.fullName,color=ProtoText,fontWeight=FontWeight.Bold);Text(c.phone,color=ProtoMuted,fontSize=11.sp);if(c.comment.isNotBlank())Text(c.comment,color=ProtoGoldSoft,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
                    Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)
                }
            }
        }
    }
    if(showAdd)ProtoAgentCustomerDialog(onDismiss={showAdd=false},onSave={n,p,e,c,a,comment->onAdd(n,p,e,c,a,comment);showAdd=false})
}

@Composable
private fun ProtoAgentCustomerDialog(onDismiss:()->Unit,onSave:(String,String,String,String,String,String)->Unit){
    var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var comment by remember{mutableStateOf("")}
    Dialog(onDismissRequest=onDismiss){
        Surface(color=ProtoPanel,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,ProtoBorder)){
            Column(Modifier.padding(16.dp).heightIn(max=620.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text("Новый частный клиент",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=onDismiss){Icon(Icons.Outlined.Close,null,tint=ProtoGold)}}
                LazyColumn(Modifier.weight(1f,false)){
                    item{ProtoField(name,{name=it},"ФИО")}
                    item{ProtoField(phone,{phone=it},"Телефон",KeyboardType.Phone)}
                    item{ProtoField(email,{email=it},"E-mail",KeyboardType.Email)}
                    item{ProtoField(city,{city=it},"Город")}
                    item{ProtoField(address,{address=it},"Адрес")}
                    item{OutlinedTextField(value=comment,onValueChange={comment=it},label={Text("Комментарий")},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),minLines=2,maxLines=4,colors=protoFieldColors())}
                }
                Spacer(Modifier.height(8.dp))
                ProtoPrimaryButton("Сохранить",{onSave(name,phone,email,city,address,comment)},name.isNotBlank()&&phone.isNotBlank())
            }
        }
    }
}

@Composable
private fun ProtoAgentClientDetailScreen(
    customer:SansaraAgentCustomer?,
    markups:List<SansaraAgentMarkup>,
    reminders:List<SansaraAgentReminder>,
    onBack:()->Unit,
    onSaveComment:(String)->Unit,
    onMarkup:(String,Int)->Unit,
    onReminder:(Long,String)->Unit,
    onRetail:()->Unit
){
    val c=customer
    if(c==null){ProtoSimpleMessageScreen("Клиент","Клиент не найден",onBack);return}
    val context=LocalContext.current
    var comment by remember(c.id,c.comment){mutableStateOf(c.comment)}
    var reminderDateTime by remember{mutableStateOf(LocalDateTime.now().plusHours(2))}
    var reminderNote by remember{mutableStateOf("")}
    val categories=listOf("Венки","Гробы","Кресты","Ленты","Одежда")
    ProtoScaffold(c.fullName,"Карточка частного клиента",onBack){
        item{ProtoSectionCard{ProtoInfoRow("Телефон",c.phone);if(c.email.isNotBlank())ProtoInfoRow("E-mail",c.email);if(c.city.isNotBlank())ProtoInfoRow("Город",c.city);if(c.address.isNotBlank())ProtoInfoRow("Адрес",c.address)}}
        item{OutlinedTextField(value=comment,onValueChange={comment=it},label={Text("Комментарий")},modifier=Modifier.fillMaxWidth(),minLines=3,maxLines=5,colors=protoFieldColors());Spacer(Modifier.height(8.dp));ProtoPrimaryButton("Сохранить комментарий",{onSaveComment(comment)})}
        item{Text("Индивидуальная наценка",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold)}
        items(categories){category->
            var value by remember(c.id,category,markups){mutableStateOf((markups.firstOrNull{it.category==category}?.markupPct?:0).toString())}
            ProtoSectionCard{
                Text(category,color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                Row(verticalAlignment=Alignment.CenterVertically){
                    OutlinedTextField(value=value,onValueChange={value=it.filter(Char::isDigit).take(3)},label={Text("%")},singleLine=true,modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),colors=protoFieldColors())
                    Spacer(Modifier.width(10.dp))
                    Button(onClick={onMarkup(category,value.toIntOrNull()?:0)},colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Сохранить",color=Color.Black)}
                }
            }
        }
        item{Text("Напоминание",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold)}
        item{
            ProtoSectionCard{
                OutlinedButton(
                    onClick={
                        val initial=reminderDateTime
                        android.app.DatePickerDialog(
                            context,
                            {_,year,month,day->
                                android.app.TimePickerDialog(
                                    context,
                                    {_,hour,minute->
                                        reminderDateTime=LocalDateTime.of(year,month+1,day,hour,minute)
                                    },
                                    initial.hour,
                                    initial.minute,
                                    true
                                ).show()
                            },
                            initial.year,
                            initial.monthValue-1,
                            initial.dayOfMonth
                        ).show()
                    },
                    modifier=Modifier.fillMaxWidth().height(52.dp),
                    border=BorderStroke(1.dp,ProtoGold),
                    shape=RoundedCornerShape(14.dp)
                ){
                    Icon(Icons.Outlined.CalendarMonth,null,tint=ProtoGold)
                    Spacer(Modifier.width(8.dp))
                    Text(reminderDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),color=ProtoGoldSoft)
                }
                ProtoField(reminderNote,{reminderNote=it},"Текст напоминания")
                ProtoPrimaryButton("Установить напоминание",{
                    onReminder(reminderDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),reminderNote)
                })
                reminders.filter{it.active}.take(3).forEach{r->Text("• "+protoReminderTime(r.remindAtEpochMs)+" · "+r.note,color=ProtoMuted,fontSize=10.sp,modifier=Modifier.padding(top=4.dp))}
            }
        }
        item{ProtoPrimaryButton("Открыть режим «Для клиентов»",onRetail)}
    }
}

private fun protoReminderTime(epoch:Long):String=
    Instant.ofEpochMilli(epoch).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

@Composable
private fun ProtoAdminAccountsScreen(
    admins:List<SansaraAdminAccount>,
    currentUserId:String,
    onBack:()->Unit,
    onAdd:(String)->Unit,
    onToggle:(SansaraAdminAccount)->Unit
){
    var name by remember{mutableStateOf("")}
    val main=admins.firstOrNull{it.isMain}
    val canManage=main?.userId==currentUserId
    ProtoScaffold("Администраторы","Один главный аккаунт · общий пароль",onBack){
        item{
            ProtoSectionCard{
                Text("Все дополнительные администраторы используют тот же код доступа, что и главный аккаунт.",color=ProtoMuted,fontSize=11.sp)
                if(canManage){
                    Spacer(Modifier.height(8.dp))
                    ProtoField(name,{name=it},"Имя администратора")
                    ProtoPrimaryButton("Добавить администратора",{if(name.isNotBlank()){onAdd(name);name=""}},enabled=name.isNotBlank())
                }else{
                    Text("Добавлять и отключать администраторов может только главный аккаунт.",color=ProtoOrange,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
                }
            }
        }
        items(admins,key={it.userId}){admin->
            ProtoSectionCard{
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(42.dp).background(ProtoPanel2,CircleShape),contentAlignment=Alignment.Center){
                        Icon(if(admin.isMain)Icons.Outlined.VerifiedUser else Icons.Outlined.AdminPanelSettings,null,tint=ProtoGold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(admin.displayName,color=ProtoText,fontWeight=FontWeight.Bold)
                        Text(if(admin.isMain)"Главный аккаунт" else "Администратор",color=ProtoGoldSoft,fontSize=11.sp)
                        Text(if(admin.enabled)"Активен" else "Отключён",color=if(admin.enabled)ProtoGreen else ProtoRed,fontSize=10.sp)
                    }
                    if(!admin.isMain && canManage){
                        Switch(
                            checked=admin.enabled,
                            onCheckedChange={onToggle(admin)},
                            colors=SwitchDefaults.colors(checkedTrackColor=ProtoGold,checkedThumbColor=Color.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtoWorkshopTasksScreen(
    tasks:List<WorkshopTaskEntity>,
    productionMode:Boolean,
    onBack:()->Unit,
    onCreate:(String,String,String,Boolean)->Unit,
    onFact:(WorkshopTaskEntity,Int,String)->Unit
){
    var category by remember{mutableStateOf("Венки")}
    var qtyText by remember{mutableStateOf("1")}
    var comment by remember{mutableStateOf("")}
    var commentOnly by remember{mutableStateOf(false)}
    var dateText by remember{mutableStateOf(currentDateShort())}
    val sorted=tasks.sortedByDescending{it.createdAt}
    ProtoScaffold(
        if(productionMode)"Задания цеху" else "Задание в цех",
        if(productionMode)"Полученные задания · план / факт" else "План производства и комментарии",
        onBack
    ){
        if(!productionMode){
            item{
                ProtoSectionCard{
                    ProtoField(dateText,{dateText=it},"Дата · ДД.ММ.ГГГГ")
                    ProtoSwitchRow("Только комментарий",commentOnly){commentOnly=it}
                    if(!commentOnly){
                        Text("Категория",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=6.dp))
                        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            items(listOf("Венки","Гробы","Кресты","Ленты","Одежда")){label->
                                FilterChip(
                                    selected=category==label,
                                    onClick={category=label},
                                    label={Text(label)},
                                    colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText)
                                )
                            }
                        }
                        ProtoField(qtyText,{qtyText=it.filter(Char::isDigit).take(4)},"Количество, шт.",KeyboardType.Number)
                    }
                    OutlinedTextField(
                        value=comment,onValueChange={comment=it.take(500)},label={Text("Комментарий")},
                        modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),minLines=3,maxLines=5,colors=protoFieldColors()
                    )
                    val valid=if(commentOnly)comment.isNotBlank() else (qtyText.toIntOrNull()?:0)>0
                    ProtoPrimaryButton("Отправить в цех",{
                        val lines=if(commentOnly)"" else JSONArray().put(
                            JSONObject().put("category",category).put("qty",qtyText.toIntOrNull()?:0)
                        ).toString()
                        onCreate(dateText,lines,comment,commentOnly)
                        if(!commentOnly)qtyText="1"
                        comment=""
                    },enabled=valid)
                }
            }
        }
        if(sorted.isEmpty())item{ProtoSectionCard{Text("Заданий пока нет",color=ProtoMuted)}}
        items(sorted,key={it.id}){task->
            var factText by remember(task.id,task.actualQty){mutableStateOf(task.actualQty.toString())}
            val plan=protoWorkshopPlanQty(task.linesJson)
            ProtoSectionCard{
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(task.id,color=ProtoGoldSoft,fontWeight=FontWeight.Bold,fontSize=12.sp)
                        Text(task.taskDate,color=ProtoText,fontWeight=FontWeight.SemiBold)
                    }
                    ProtoPill(task.status,if(task.status=="Выполнено")ProtoGreen else ProtoOrange)
                }
                if(task.commentOnly){
                    Text("Комментарий: "+task.comment,color=ProtoText,modifier=Modifier.padding(top=7.dp))
                }else{
                    Text(protoWorkshopLinesLabel(task.linesJson),color=ProtoText,modifier=Modifier.padding(top=7.dp))
                    if(task.comment.isNotBlank())Text("Комментарий: "+task.comment,color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=3.dp))
                    ProtoInfoRow("План",plan.toString()+" шт.")
                    ProtoInfoRow("Факт",task.actualQty.toString()+" шт.")
                }
                if(productionMode && !task.commentOnly){
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment=Alignment.CenterVertically){
                        OutlinedTextField(
                            value=factText,onValueChange={factText=it.filter(Char::isDigit).take(4)},
                            label={Text("Факт, шт.")},singleLine=true,modifier=Modifier.weight(1f),
                            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),colors=protoFieldColors()
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick={
                                val fact=factText.toIntOrNull()?:0
                                onFact(task,fact,if(fact>=plan && plan>0)"Выполнено" else "В работе")
                            },
                            colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)
                        ){Text("Сохранить",color=Color.Black)}
                    }
                }
            }
        }
    }
}

private fun protoWorkshopPlanQty(linesJson:String):Int=runCatching{
    val a=JSONArray(linesJson)
    (0 until a.length()).sumOf{i->a.getJSONObject(i).optInt("qty",0)}
}.getOrDefault(0)

private fun protoWorkshopLinesLabel(linesJson:String):String=runCatching{
    val a=JSONArray(linesJson)
    (0 until a.length()).joinToString(" · "){i->
        val o=a.getJSONObject(i)
        o.optString("category","Позиция")+" — "+o.optInt("qty",0)+" шт."
    }
}.getOrDefault(linesJson.ifBlank{"Без позиций"})

@Composable
private fun ProtoAttendanceScreen(
    selectedDate:LocalDate,
    assemblers:List<SansaraAssembler>,
    rows:List<AttendanceEntity>,
    editable:Boolean,
    onBack:()->Unit,
    onDate:(LocalDate)->Unit,
    onSave:(SansaraAssembler,String,String)->Unit
){
    var showCalendar by remember{mutableStateOf(false)}
    val dateKey=selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val dayRows=rows.filter{it.date==dateKey}.associateBy{it.personId}
    ProtoScaffold("Табель рабочего времени",selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy",ruLocale)),onBack){
        item{ProtoSecondaryButton("Выбрать дату",{showCalendar=true})}
        item{
            ProtoSectionCard{
                ProtoInfoRow("Работали",dayRows.values.count{it.status=="Работал"}.toString())
                ProtoInfoRow("Выходной",dayRows.values.count{it.status=="Выходной"}.toString())
                ProtoInfoRow("Не заполнено",(assemblers.count{it.enabled}-dayRows.size).coerceAtLeast(0).toString())
            }
        }
        items(assemblers.filter{it.enabled},key={it.id}){person->
            val existing=dayRows[person.id]
            var comment by remember(person.id,dateKey,existing?.comment){mutableStateOf(existing?.comment.orEmpty())}
            ProtoSectionCard{
                Text(person.name,color=ProtoText,fontWeight=FontWeight.Bold)
                Text(existing?.status?:"Не заполнено",color=when(existing?.status){"Работал"->ProtoGreen;"Выходной"->ProtoGoldSoft;else->ProtoMuted},fontSize=11.sp)
                if(editable){
                    Spacer(Modifier.height(6.dp))
                    ProtoField(comment,{comment=it},"Комментарий / причина")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Button(onClick={onSave(person,"Работал",comment)},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Работал",color=Color.Black)}
                        OutlinedButton(onClick={onSave(person,"Выходной",comment)},modifier=Modifier.weight(1f),border=BorderStroke(1.dp,ProtoBorder)){Text("Выходной",color=ProtoText)}
                    }
                }
            }
        }
    }
    if(showCalendar)ProtoProductionCalendarDialog(selectedDate,onDismiss={showCalendar=false},onSelect={onDate(it);showCalendar=false})
}

@Composable
private fun ProtoAdminReportsScreen(
    tasks:List<WorkshopTaskEntity>,
    attendance:List<AttendanceEntity>,
    presence:List<PresenceSessionEntity>,
    productionOps:List<ProtoProductionOp>,
    dailyStatus:AdminDailyStatusEntity?,
    onBack:()->Unit,
    onDailyStatus:(Boolean,String)->Unit
){
    val today=currentDateShort()
    val tasksToday=tasks.filter{it.taskDate==today}
    val plan=tasksToday.sumOf{protoWorkshopPlanQty(it.linesJson)}
    val fact=tasksToday.sumOf{it.actualQty}
    val attendanceToday=attendance.filter{it.date==today}
    val produced=productionOps.filter{it.date==today}.sumOf{it.qty}
    var dayOff by remember(dailyStatus?.date,dailyStatus?.dayOff){mutableStateOf(dailyStatus?.dayOff?:false)}
    var reason by remember(dailyStatus?.date,dailyStatus?.reason){mutableStateOf(dailyStatus?.reason.orEmpty())}
    ProtoScaffold("Отчёты","Сводные показатели SANSARA",onBack){
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("План цеха",plan.toString(),"шт.",Modifier.weight(1f)){};ProtoMetricCard("Факт",fact.toString(),"шт.",Modifier.weight(1f)){} }}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Выпуск",produced.toString(),"сегодня",Modifier.weight(1f)){};ProtoMetricCard("Табель",attendanceToday.count{it.status=="Работал"}.toString(),"выходов",Modifier.weight(1f)){} }}
        item{
            ProtoSectionCard{
                Text("Задания в цех · план / факт",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                if(tasksToday.isEmpty())Text("Сегодня заданий нет",color=ProtoMuted,modifier=Modifier.padding(top=6.dp))
                tasksToday.forEach{t->ProtoInfoRow(t.id,protoWorkshopPlanQty(t.linesJson).toString()+" / "+t.actualQty.toString()+" шт.")}
            }
        }
        item{
            ProtoSectionCard{
                Text("Клиенты и сотрудники сегодня",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                ProtoInfoRow("Заходили",presence.map{it.userId}.distinct().size.toString())
                presence.sortedByDescending{it.durationMs}.take(12).forEach{p->ProtoInfoRow(p.userId,protoDuration(p.durationMs))}
            }
        }
        item{
            ProtoSectionCard{
                Text("Исключение напоминаний администратора",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                ProtoSwitchRow("Выходной",dayOff){dayOff=it}
                ProtoField(reason,{reason=it},"Причина / комментарий")
                ProtoPrimaryButton("Сохранить",{onDailyStatus(dayOff,reason)})
                Text("Если установлен выходной или указана причина, обязательные напоминания на этот день не повторяются.",color=ProtoMuted,fontSize=10.sp,modifier=Modifier.padding(top=6.dp))
            }
        }
    }
}

private fun protoDuration(ms:Long):String{
    val totalMinutes=(ms/60_000L).coerceAtLeast(0L)
    val h=totalMinutes/60
    val m=totalMinutes%60
    return if(h>0)"${h} ч ${m} мин" else "${m} мин"
}

@Composable
private fun ProtoAdminChatsScreen(
    clients:List<ProtoClient>,
    messages:List<SansaraChatMessage>,
    onBack:()->Unit,
    onOpen:(ProtoClient)->Unit,
    onProduction:()->Unit
){
    var query by remember{mutableStateOf("")}
    val filtered=clients.filter{query.isBlank()||it.name.contains(query,true)||it.contact.contains(query,true)}
        .sortedByDescending { client -> messages.filter{it.conversationId=="CLIENT:"+client.id}.maxOfOrNull{it.createdAt} ?: 0L }
    ProtoScaffold("Чаты","Переписка с клиентами",onBack){
        item{
            val productionThread=messages.filter{it.conversationId=="STAFF:ADMIN_PRODUCTION"}
            val unread=productionThread.count{!it.read&&it.senderRole=="PRODUCTION"}
            ProtoSectionCard(Modifier.clickable{onProduction()}){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(46.dp).background(ProtoPanel2,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Factory,null,tint=ProtoGold)}
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)){Text("Производство",color=ProtoText,fontWeight=FontWeight.Bold);Text(productionThread.maxByOrNull{it.createdAt}?.body?.ifBlank{"Вложение"}?:"Чат с цехом",color=ProtoMuted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
                    if(unread>0)Badge(containerColor=ProtoGold){Text(unread.toString(),color=Color.Black)}
                }
            }
        }
        item{ProtoField(query,{query=it},"Поиск клиента")}
        items(filtered,key={it.id}){client->
            val conversation="CLIENT:"+client.id
            val thread=messages.filter{it.conversationId==conversation}
            val last=thread.maxByOrNull{it.createdAt}
            val unread=thread.count{!it.read&&it.senderRole=="CLIENT"}
            ProtoSectionCard(Modifier.clickable{onOpen(client)}){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(46.dp).background(ProtoPanel2,CircleShape),contentAlignment=Alignment.Center){
                        Text(client.name.take(1).uppercase(),color=ProtoGold,fontWeight=FontWeight.Bold,fontSize=18.sp)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)){
                        Text(client.name,color=ProtoText,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text(last?.let{if(it.body.isNotBlank())it.body else "Вложение: "+it.attachmentName} ?: "Сообщений пока нет",color=ProtoMuted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment=Alignment.End){
                        last?.let{Text(protoChatTime(it.createdAt),color=ProtoMuted,fontSize=9.sp)}
                        if(unread>0)Box(Modifier.padding(top=4.dp).size(23.dp).background(ProtoGold,CircleShape),contentAlignment=Alignment.Center){Text(unread.toString(),color=Color.Black,fontSize=9.sp,fontWeight=FontWeight.Bold)}
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtoChatScreen(
    title:String,
    subtitle:String,
    messages:List<SansaraChatMessage>,
    currentRole:String,
    onBack:()->Unit,
    onRead:()->Unit,
    onSend:(String,String,String,String)->Unit
){
    val context=LocalContext.current
    var text by remember{mutableStateOf("")}
    val chatScope=rememberCoroutineScope()
    var attachMenu by remember{mutableStateOf(false)}
    var photoPreview by remember{mutableStateOf<String?>(null)}
    fun sendPickedImage(uri:Uri){
        chatScope.launch{
            val copied=protoCopyChatImage(context,uri)
            if(copied!=null)onSend("",copied.first,copied.second,copied.third)
            else android.widget.Toast.makeText(context,"Не удалось прикрепить фото",android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            val mime=context.contentResolver.getType(uri).orEmpty()
            if(mime.startsWith("image/")){
                sendPickedImage(uri)
            }else{
                runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
                onSend("",uri.toString(),protoUriName(context,uri),mime)
            }
        }
    }
    val photoLauncher=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null)sendPickedImage(uri)
    }
    LaunchedEffect(Unit){onRead()}
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        photoPreview?.let{previewUri->
            Dialog(onDismissRequest={photoPreview=null},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
                Box(Modifier.fillMaxSize().background(Color.Black).clickable{photoPreview=null}){
                    AsyncImage(model=previewUri,contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
                    IconButton(onClick={photoPreview=null},modifier=Modifier.align(Alignment.TopEnd).padding(18.dp).size(50.dp).background(Color.Black.copy(alpha=.72f),CircleShape).border(1.dp,ProtoGold,CircleShape)){Icon(Icons.Outlined.Close,null,tint=ProtoGold)}
                }
            }
        }
        Column(Modifier.fillMaxSize()){
            ProtoBrandHeader(onBack=onBack,showBell=false)
            Column(Modifier.padding(horizontal=18.dp)){
                Text(title,color=ProtoText,fontSize=26.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(subtitle,color=ProtoMuted,fontSize=11.sp)
            }
            LazyColumn(
                modifier=Modifier.weight(1f),
                contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ){
                if(messages.isEmpty())item{Text("Начните переписку",color=ProtoMuted,modifier=Modifier.padding(12.dp))}
                items(messages,key={it.id}){msg->
                    val mine=msg.senderRole==currentRole
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start){
                        Surface(
                            color=if(mine)ProtoGold.copy(alpha=.20f) else ProtoPanel,
                            border=BorderStroke(1.dp,if(mine)ProtoGold.copy(alpha=.55f) else ProtoBorder),
                            shape=RoundedCornerShape(16.dp),
                            modifier=Modifier.fillMaxWidth(.82f)
                        ){
                            Column(Modifier.padding(11.dp)){
                                if(msg.body.isNotBlank())Text(msg.body,color=ProtoText,fontSize=13.sp,lineHeight=18.sp)
                                if(msg.attachmentUri.isNotBlank()&&msg.attachmentMime.startsWith("image/")){
                                    AsyncImage(
                                        model=msg.attachmentUri,
                                        contentDescription=msg.attachmentName,
                                        contentScale=ContentScale.Crop,
                                        modifier=Modifier.padding(top=if(msg.body.isBlank())0.dp else 8.dp)
                                            .fillMaxWidth().height(180.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable{photoPreview=msg.attachmentUri}
                                    )
                                }else if(msg.attachmentUri.isNotBlank()){
                                    Row(
                                        Modifier.padding(top=if(msg.body.isBlank())0.dp else 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ProtoPanel2)
                                            .clickable{protoOpenAttachment(context,msg.attachmentUri,msg.attachmentMime)}
                                            .padding(10.dp),
                                        verticalAlignment=Alignment.CenterVertically
                                    ){
                                        Icon(if(msg.attachmentMime.startsWith("image/"))Icons.Outlined.Image else Icons.Outlined.AttachFile,null,tint=ProtoGold)
                                        Spacer(Modifier.width(8.dp))
                                        Text(msg.attachmentName.ifBlank{"Вложение"},color=ProtoGoldSoft,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    }
                                }
                                Text(protoChatTime(msg.createdAt),color=ProtoMuted,fontSize=9.sp,modifier=Modifier.align(Alignment.End).padding(top=5.dp))
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(ProtoPanel).padding(horizontal=10.dp,vertical=8.dp),
                verticalAlignment=Alignment.Bottom
            ){
                Box{
                    IconButton(onClick={attachMenu=true}){
                        Icon(Icons.Outlined.AttachFile,contentDescription="Прикрепить файл",tint=ProtoGold)
                    }
                    DropdownMenu(expanded=attachMenu,onDismissRequest={attachMenu=false}){
                        DropdownMenuItem(text={Text("Фото")},onClick={
                            attachMenu=false
                            photoLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                        DropdownMenuItem(text={Text("Файл или документ")},onClick={
                            attachMenu=false
                            launcher.launch(arrayOf("application/pdf","text/*","application/*","image/*"))
                        })
                    }
                }
                OutlinedTextField(
                    value=text,
                    onValueChange={text=it},
                    placeholder={Text("Сообщение",color=ProtoMuted)},
                    modifier=Modifier.weight(1f),
                    maxLines=4,
                    colors=protoChatFieldColors()
                )
                IconButton(onClick={if(text.isNotBlank()){val value=text;text="";onSend(value,"","","")}}){
                    Icon(Icons.Outlined.Send,contentDescription="Отправить",tint=if(text.isNotBlank())ProtoGold else ProtoMuted)
                }
            }
        }
    }
}

@Composable
private fun ProtoAdminHomeScreen(
    registrationsTotal:Int,
    clients:List<ProtoClient>,
    orders:List<ProtoOrder>,
    productionOps:List<ProtoProductionOp>,
    products:List<ProtoCatalogProduct>,
    stockOverrides:SnapshotStateMap<String,Int>,
    lowStockThreshold:Int,
    reservedForSku:(String)->Int,
    onSearch:()->Unit,
    onRegistrations:()->Unit,
    onClients:()->Unit,
    onOrders:()->Unit,
    onProduction:()->Unit,
    onStock:()->Unit,
    onCatalog:()->Unit,
    onSettings:()->Unit,
    onAttention:()->Unit,
    onOnline:()->Unit,
    onLowStock:()->Unit,
    onChats:()->Unit,
    onWorkshop:()->Unit,
    onReports:()->Unit,
    unreadCount:Int,
    onNotifications:()->Unit
){
    val today=currentDateShort()
    val ordersToday=orders.count{it.dateTime.startsWith(today)}
    val producedToday=productionOps.filter{it.date==today}.sumOf{it.qty}
    val activeClients=clients.count{it.status!="Приостановлен"}
    val lowCount=products.count{((stockOverrides[it.sku]?:it.stock)-reservedForSku(it.sku)).coerceAtLeast(0)<=lowStockThreshold}
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(containerColor=Color.Transparent,bottomBar={ProtoAdminBottomBar(ProtoScreen.AdminHome,onHome={},onClients,onOrders,onStock,onProfile=onSettings)}){pad->
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                item{ProtoBrandHeader(unreadCount=unreadCount,onBell=onNotifications)}
                item{Text("Здравствуйте, Игорь",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Администратор",color=ProtoGoldSoft,fontSize=14.sp)}
                item{ProtoSearchBar("",onSearch,"Поиск по клиентам, заказам, товарам")}
                item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ProtoMetricCard("Всего заявок",registrationsTotal.toString(),"текущий месяц",Modifier.weight(1f),onRegistrations);ProtoMetricCard("Активные клиенты",activeClients.toString(),"",Modifier.weight(1f),onClients)}}
                item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ProtoMetricCard("Заказы сегодня",ordersToday.toString(),"",Modifier.weight(1f),onOrders);ProtoMetricCard("Производство",producedToday.toString(),"сегодня",Modifier.weight(1f),onProduction)}}
                item{ProtoMetricCard("Низкие остатки",lowCount.toString(),"требуют внимания",Modifier.fillMaxWidth(),onLowStock)}
                item{Text("Быстрые действия",color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold)}
                item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoQuickButton("Клиенты",Icons.Outlined.Groups,Modifier.weight(1f),onClients);ProtoQuickButton("Заказы",Icons.Outlined.ReceiptLong,Modifier.weight(1f),onOrders);ProtoQuickButton("Чаты",Icons.Outlined.ChatBubbleOutline,Modifier.weight(1f),onChats)}}
                item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoQuickButton("Производство",Icons.Outlined.Factory,Modifier.weight(1f),onProduction);ProtoQuickButton("Каталог",Icons.Outlined.Inventory2,Modifier.weight(1f),onCatalog);ProtoQuickButton("Настройки",Icons.Outlined.Settings,Modifier.weight(1f),onSettings)}}
                item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoQuickButton("Задание в цех",Icons.Outlined.Assignment,Modifier.weight(1f),onWorkshop);ProtoQuickButton("Отчёты",Icons.Outlined.Assessment,Modifier.weight(1f),onReports);Spacer(Modifier.weight(1f))}}
                item{ProtoSectionCard(Modifier.clickable{onAttention()}){ProtoInfoRow("Новые регистрации",registrationsTotal.toString());ProtoInfoRow("Заказы на сборке",orders.count{it.status=="Собирается"}.toString());ProtoInfoRow("Низкие остатки",lowCount.toString())}}
                item{ProtoSectionCard(Modifier.clickable{onOnline()}){ProtoInfoRow("Онлайн сейчас",clients.count{it.online}.toString());ProtoInfoRow("Синхронизация","автоматическая")}}
            }
        }
    }
}
@Composable
private fun ProtoAdminSearchScreen(clients:List<ProtoClient>,orders:List<ProtoOrder>,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit,onClient:(ProtoClient)->Unit,onOrder:(ProtoOrder)->Unit,onProduct:(ProtoCatalogProduct)->Unit){
    var query by remember{mutableStateOf("")};val q=query.trim();val fc=if(q.isBlank())emptyList() else clients.filter{it.name.contains(q,true)||it.contact.contains(q,true)||it.id.contains(q,true)};val fo=if(q.isBlank())emptyList() else orders.filter{it.id.contains(q,true)||it.clientName.contains(q,true)};val fp=if(q.isBlank())emptyList() else products.filter{it.sku.contains(q,true)||it.name.contains(q,true)}.take(20)
    ProtoScaffold("Поиск","Клиенты · заказы · товары",onBack){item{ProtoField(query,{query=it},"Введите название, ID или артикул")};if(q.isBlank())item{Text("Начните ввод — поиск работает сразу по всей системе.",color=ProtoMuted)};if(fc.isNotEmpty())item{Text("Клиенты",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fc,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onClient(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.id} · ${c.status}",color=ProtoMuted,fontSize=11.sp)}};if(fo.isNotEmpty())item{Text("Заказы",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fo,key={it.id}){o->ProtoOrderRow(o){onOrder(o)}};if(fp.isNotEmpty())item{Text("Товары",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fp,key={it.sku}){p->ProtoSectionCard(Modifier.clickable{onProduct(p)}){Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);ProtoSkuText(p.sku,fontSize=13)};Text("${stockOverrides[p.sku]?:p.stock} шт.",color=ProtoGoldSoft)}}}}
}

@Composable
private fun ProtoAdminClientsScreen(clients:List<ProtoClient>,onBack:()->Unit,onOpen:(ProtoClient)->Unit){
    var q by remember{mutableStateOf("")}
    var filter by remember{mutableStateOf("Все")}
    val filters=listOf("Все","Активный","Оптовик","VIP","Приостановлен","Онлайн")
    val filtered=clients.filter{c->
        val queryOk=q.isBlank()||c.name.contains(q,true)||c.contact.contains(q,true)||c.phone.contains(q,true)||c.id.contains(q,true)
        val filterOk=when(filter){
            "Онлайн"->c.online
            "Все"->true
            else->c.status==filter
        }
        queryOk&&filterOk
    }
    ProtoScaffold("Клиенты","Оборот за "+currentMonthLabel().lowercase(ruLocale),onBack){
        item{ProtoField(q,{q=it},"Поиск клиента")}
        item{
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                items(filters){label->
                    FilterChip(
                        selected=filter==label,
                        onClick={filter=label},
                        label={Text(label)},
                        colors=FilterChipDefaults.filterChipColors(
                            selectedContainerColor=ProtoGold,
                            selectedLabelColor=Color.Black,
                            labelColor=ProtoText
                        )
                    )
                }
            }
        }
        if(filtered.isEmpty())item{Text("Клиенты по выбранному фильтру не найдены",color=ProtoMuted)}
        else items(filtered,key={it.id}){c->
            Card(
                colors=CardDefaults.cardColors(containerColor=ProtoPanel),
                border=BorderStroke(1.dp,ProtoBorder),
                modifier=Modifier.fillMaxWidth().clickable{onOpen(c)}
            ){
                Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){
                    Box(
                        Modifier.size(9.dp)
                            .background(if(c.online)ProtoGreen else ProtoMuted,CircleShape)
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)){
                        Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold)
                        Text(c.status+" · "+c.orderCount+" заказов",color=ProtoMuted,fontSize=12.sp)
                        Text(if(c.online)"Онлайн" else "Был в сети "+c.lastSeen,color=if(c.online)ProtoGreen else ProtoMuted,fontSize=10.sp)
                    }
                    Column(horizontalAlignment=Alignment.End){
                        Text(protoMoney(c.monthTurnover),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                        Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtoAdminClientScreen(
    client: ProtoClient?,
    retailCustomers: List<SansaraAgentCustomer>,
    retailMarkups: List<SansaraAgentMarkup>,
    onBack: () -> Unit,
    onStatus: (String) -> Unit,
    onToggleBlock: () -> Unit,
    onToggleOrdering: () -> Unit,
    onDiscount: (Int) -> Unit,
    onSave: () -> Unit,
    onCall: () -> Unit
) {
    val currentClient = client ?: return

    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ProtoBrandHeader(onBack = onBack) }
            item { Text("Карточка клиента", color = ProtoText, fontSize = 30.sp, fontWeight = FontWeight.Bold) }

            item {
                ProtoSectionCard {
                    Text(currentClient.name, color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(currentClient.clientType, color = ProtoMuted)
                    ProtoInfoRow("Телефон", currentClient.phone)
                    ProtoInfoRow("E-mail", currentClient.email)
                    ProtoInfoRow("Клиент с", currentClient.registeredAt)
                }
            }

            item {
                Text("Статус клиента", color = ProtoText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Активный", "Оптовик", "VIP", "Приостановлен").forEach { status ->
                        FilterChip(
                            selected = currentClient.status == status,
                            onClick = { onStatus(status) },
                            label = { Text(status, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ProtoGold,
                                selectedLabelColor = Color.Black,
                                labelColor = ProtoText
                            )
                        )
                    }
                }
            }

            item {
                ProtoSectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Скидка", color = ProtoMuted)
                        Spacer(Modifier.weight(1f))
                        ProtoQtyButton(Icons.Outlined.Remove, { onDiscount(-1) })
                        Text(
                            currentClient.discount.toString() + "%",
                            color = ProtoText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        ProtoQtyButton(Icons.Outlined.Add, { onDiscount(1) })
                    }
                }
            }

            item {
                ProtoSwitchRow(
                    "Доступ к заказам",
                    currentClient.orderingEnabled,
                    { onToggleOrdering() }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)
                    ) {
                        Text("Сохранить", color = Color.Black)
                    }
                    OutlinedButton(
                        onClick = onToggleBlock,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, ProtoGold)
                    ) {
                        Text(
                            if (currentClient.status == "Приостановлен") "Разблокировать" else "Приостановить",
                            color = ProtoGold
                        )
                    }
                }
            }

            item { ProtoSecondaryButton("Позвонить", onCall) }

            if(retailCustomers.isNotEmpty()){
                item { Text("Клиенты агента · наценки", color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                items(retailCustomers,key={it.id}){customer->
                    val marks=retailMarkups.filter{it.customerId==customer.id}.sortedBy{it.category}
                    ProtoSectionCard{
                        Text(customer.fullName,color=ProtoText,fontWeight=FontWeight.Bold)
                        Text(customer.phone,color=ProtoMuted,fontSize=11.sp)
                        if(marks.isEmpty())Text("Индивидуальная наценка не задана",color=ProtoMuted,fontSize=10.sp,modifier=Modifier.padding(top=5.dp))
                        else marks.forEach{m->ProtoInfoRow(m.category,m.markupPct.toString()+"%")}
                    }
                }
            }

            item {
                Text("Пользователи компании", color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            item {
                ProtoSectionCard {
                    Text(currentClient.contact, color = ProtoText, fontWeight = FontWeight.SemiBold)
                    Text(currentClient.email, color = ProtoMuted, fontSize = 11.sp)
                    Text(
                        if (currentClient.online) "Онлайн" else "Был в сети " + currentClient.lastSeen,
                        color = if (currentClient.online) ProtoGreen else ProtoMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtoAdminOrdersScreen(orders:List<ProtoOrder>,onBack:()->Unit,onOpen:(ProtoOrder)->Unit){
    var q by remember{mutableStateOf("")}
    var filter by remember{mutableStateOf("Все")}
    val statuses=listOf("Все","Получен","Подтверждён","Собирается","Доставляется","Доставлен","Отменён")
    val filtered=orders.filter{o->
        val queryOk=q.isBlank()||o.id.contains(q,true)||o.clientName.contains(q,true)
        val filterOk=filter=="Все"||o.status==filter
        queryOk&&filterOk
    }
    ProtoScaffold("Заказы",currentMonthLabel(),onBack){
        item{ProtoField(q,{q=it},"Поиск по номеру или клиенту")}
        item{
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                items(statuses){label->
                    FilterChip(
                        selected=filter==label,
                        onClick={filter=label},
                        label={Text(label)},
                        colors=FilterChipDefaults.filterChipColors(
                            selectedContainerColor=ProtoGold,
                            selectedLabelColor=Color.Black,
                            labelColor=ProtoText
                        )
                    )
                }
            }
        }
        if(filtered.isEmpty())item{Text("Заказы по выбранному фильтру не найдены",color=ProtoMuted)}
        else items(filtered,key={it.id}){o->ProtoOrderRow(o){onOpen(o)}}
    }
}

@Composable
private fun ProtoAdminCatalogScreen(
    products:List<ProtoCatalogProduct>,
    stockOverrides:SnapshotStateMap<String,Int>,
    syncStatus:String,
    lastSync:String,
    onSync:()->Unit,
    onBack:()->Unit
){
    ProtoScaffold("Каталог","Данные каталога синхронизируются с Tilda",onBack){
        item{
            ProtoSectionCard{
                ProtoInfoRow("Синхронизация",syncStatus)
                ProtoInfoRow("Последнее обновление",lastSync.ifBlank{"ещё не выполнялось"})
                OutlinedButton(
                    onClick=onSync,
                    modifier=Modifier.fillMaxWidth().padding(top=8.dp),
                    border=BorderStroke(1.dp,ProtoGold)
                ){
                    Icon(Icons.Outlined.Sync,null,tint=ProtoGold)
                    Spacer(Modifier.width(7.dp))
                    Text("Обновить из Tilda",color=ProtoGold)
                }
                Text("Цена и описание изменяются в источнике каталога, а не в приложении.",color=ProtoMuted,fontSize=10.sp,modifier=Modifier.padding(top=8.dp))
            }
        }
        items(products,key={it.sku}){p->
            ProtoSectionCard{
                Row(verticalAlignment=Alignment.CenterVertically){
                    ProtoProductImage(p,Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold)
                        ProtoSkuText(p.sku,extra=" · "+p.type+" · "+p.size,fontSize=13)
                    }
                    Text((stockOverrides[p.sku]?:p.stock).toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProtoAdminSettingsMenuScreen(syncStatus:String,lastSync:String,lowStockThreshold:Int,onBack:()->Unit,onOpen:(String)->Unit,onCall:()->Unit){
    val rows=listOf(
        Triple("Профиль компании",Icons.Outlined.Business,"Данные SANSARA и контакты"),
        Triple("Пользователи и роли",Icons.Outlined.Groups,"Администраторы и производство"),
        Triple("Администраторы",Icons.Outlined.AdminPanelSettings,"Главный аккаунт и дополнительные админы"),
        Triple("Задание в цех",Icons.Outlined.Assignment,"План, комментарии и план/факт"),
        Triple("Табель рабочего времени",Icons.Outlined.EventAvailable,"Кто и когда выходил"),
        Triple("Отчёты",Icons.Outlined.Assessment,"Все показатели и отчёты"),
        Triple("Клиенты",Icons.Outlined.PersonSearch,"Доступ, статусы и скидки"),
        Triple("Сборщицы",Icons.Outlined.Badge,"Справочник производства и доступ"),
        Triple("Каталог и синхронизация",Icons.Outlined.Sync,"$syncStatus · $lastSync"),
        Triple("Telegram интеграция",Icons.Outlined.Send,"Заказы через защищённый серверный прокси"),
        Triple("Порог низких остатков",Icons.Outlined.Warning,"Сейчас: $lowStockThreshold шт."),
        Triple("Уведомления",Icons.Outlined.Notifications,"Регистрации, заказы, производство"),
        Triple("Резервное копирование",Icons.Outlined.Backup,"Локальная тестовая копия"),
        Triple("Экспорт данных",Icons.Outlined.FileDownload,"CSV сейчас · XLSX/PDF далее"),
        Triple("О приложении",Icons.Outlined.Info,"SANSARA · версия ${BuildConfig.VERSION_NAME}")
    )
    ProtoScaffold("Настройки","Управление системой SANSARA",onBack){
        items(rows){(title,icon,subtitle)->Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{onOpen(title)}){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).background(ProtoPanel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(icon,null,tint=ProtoGold)};Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(title,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(subtitle,color=ProtoMuted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}
        item{Spacer(Modifier.height(8.dp));Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().clickable{onCall()}){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).background(ProtoGreen.copy(alpha=.16f),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Phone,null,tint=ProtoGreen)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("Поддержка",color=ProtoMuted,fontSize=11.sp);Text(BuildConfig.ADMIN_PHONE,color=ProtoGoldSoft,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Ежедневно 09:00–20:00",color=ProtoMuted,fontSize=10.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}
    }
}

@Composable
private fun ProtoAssemblerAdminScreen(assemblers:List<SansaraAssembler>,onBack:()->Unit,onAdd:(String)->Unit,onToggle:(SansaraAssembler)->Unit){
    var name by remember{mutableStateOf("")}
    ProtoScaffold("Сборщицы","Добавлять и отключать может только администратор",onBack){
        item{ProtoSectionCard{ProtoField(name,{name=it},"ФИО / имя сборщицы");ProtoPrimaryButton("Добавить",{if(name.isNotBlank()){onAdd(name);name=""}},enabled=name.isNotBlank())}}
        items(assemblers,key={it.id}){assembler->
            ProtoSectionCard{
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){Text(assembler.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(if(assembler.enabled)"Активна" else "Отключена",color=if(assembler.enabled)ProtoGreen else ProtoMuted,fontSize=11.sp)}
                    Switch(checked=assembler.enabled,onCheckedChange={onToggle(assembler)},colors=SwitchDefaults.colors(checkedTrackColor=ProtoGold,checkedThumbColor=Color.Black))
                }
            }
        }
    }
}

@Composable
private fun ProtoAdminSettingsDetailScreen(
    section:String,threshold:Int,reg:Boolean,orders:Boolean,prod:Boolean,low:Boolean,
    tildaUrl:String,syncStatus:String,lastSync:String,syncing:Boolean,backupStatus:String,backendStatus:String,
    telegramEnabled:Boolean,telegramRetryEnabled:Boolean,telegramApiUrl:String,telegramTemplate:String,telegramLastLog:String,
    onBack:()->Unit,onThreshold:(Int)->Unit,onReg:(Boolean)->Unit,onOrders:(Boolean)->Unit,onProd:(Boolean)->Unit,onLow:(Boolean)->Unit,
    onTildaUrl:(String)->Unit,onSync:()->Unit,onBackup:()->Unit,onClients:()->Unit,
    onTelegramSave:(Boolean,Boolean,String,String)->Unit
){
    ProtoScaffold(section,null,onBack){
        when(section){
            "Профиль компании"->item{ProtoSectionCard{ProtoInfoRow("Компания","SANSARA");ProtoInfoRow("Телефон",BuildConfig.ADMIN_PHONE);ProtoInfoRow("Режим поддержки","09:00–20:00");ProtoInfoRow("Каталог","sansararitual.ru")}}
            "Пользователи и роли"->item{ProtoSectionCard{listOf("Администратор — полный доступ","Производство — выпуск / приход / история","Клиент — каталог / корзина / заказы").forEach{Text(it,color=ProtoText,modifier=Modifier.padding(vertical=5.dp))};Text("Роли фиксированы. Пользователь не выбирает роль самостоятельно.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))}}
            "Каталог и синхронизация"->item{ProtoSectionCard{Text("Каталог Tilda",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Синхронизация обновляет карточки, цены, категории и фотографии. Склад, резерв, заказы и производство не перезаписываются.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(vertical=6.dp));ProtoField(tildaUrl,onTildaUrl,"YML-ссылка каталога Tilda");Button(onClick=onSync,enabled=!syncing&&tildaUrl.isNotBlank(),modifier=Modifier.fillMaxWidth().height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){if(syncing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Color.Black)else Icon(Icons.Outlined.Sync,null,tint=Color.Black);Spacer(Modifier.width(7.dp));Text(if(syncing)"Синхронизация…" else "Синхронизировать каталог",color=Color.Black,fontWeight=FontWeight.Bold)};Text(syncStatus,color=if(syncStatus.startsWith("Ошибка"))ProtoRed else ProtoGreen,fontSize=11.sp,modifier=Modifier.padding(top=7.dp));Text("Последнее обновление: $lastSync",color=ProtoMuted,fontSize=10.sp);Text("Сервер событий: $backendStatus",color=ProtoMuted,fontSize=10.sp,modifier=Modifier.padding(top=4.dp))}}
            "Telegram интеграция"->item{
                var enabled by remember(telegramEnabled){mutableStateOf(telegramEnabled)}
                var retry by remember(telegramRetryEnabled){mutableStateOf(telegramRetryEnabled)}
                var apiUrl by remember(telegramApiUrl){mutableStateOf(telegramApiUrl)}
                var template by remember(telegramTemplate){mutableStateOf(telegramTemplate)}
                ProtoSectionCard{
                    Text("Telegram-бот",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold)
                    Text("Заказы отправляются только через серверный прокси. BOT_TOKEN и CHAT_ID не хранятся в APK и репозитории.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(vertical=6.dp))
                    ProtoSwitchRow("Отправлять заказы в Telegram",enabled){enabled=it}
                    ProtoField(apiUrl,{apiUrl=it},"HTTPS-адрес серверного прокси")
                    ProtoSwitchRow("Повторять отправку при сбое",retry){retry=it}
                    OutlinedTextField(
                        value=template,
                        onValueChange={template=it},
                        label={Text("Шаблон сообщения")},
                        modifier=Modifier.fillMaxWidth().padding(vertical=6.dp),
                        minLines=5,
                        maxLines=9,
                        colors=protoFieldColors()
                    )
                    Text("Переменные: {number}, {client}, {items}, {total}, {contact}, {date}, {time}",color=ProtoMuted,fontSize=10.sp)
                    Spacer(Modifier.height(8.dp))
                    ProtoPrimaryButton("Сохранить настройки",{onTelegramSave(enabled,retry,apiUrl,template)},enabled=!enabled||apiUrl.startsWith("https://"))
                    Spacer(Modifier.height(8.dp))
                    Text("Последняя отправка: "+telegramLastLog,color=ProtoMuted,fontSize=10.sp)
                    Text("BOT_TOKEN/CHAT_ID задаются только на сервере или в его Secrets.",color=ProtoGoldSoft,fontSize=10.sp,modifier=Modifier.padding(top=4.dp))
                }
            }
            "Порог низких остатков"->item{ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center,modifier=Modifier.fillMaxWidth()){ProtoQtyButton(Icons.Outlined.Remove,{onThreshold(threshold-1)});Text("$threshold шт.",color=ProtoGoldSoft,fontSize=28.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=20.dp));ProtoQtyButton(Icons.Outlined.Add,{onThreshold(threshold+1)})};Text("Позиции с доступным остатком ≤ порога автоматически попадают в «Требует внимания».",color=ProtoMuted,fontSize=11.sp)}}
            "Уведомления"->item{ProtoSectionCard{ProtoSwitchRow("Новые регистрации",reg,onReg);ProtoSwitchRow("Новые заказы",orders,onOrders);ProtoSwitchRow("Производство",prod,onProd);ProtoSwitchRow("Низкие остатки",low,onLow)}}
            "Резервное копирование"->item{ProtoSectionCard{Text(backupStatus,color=ProtoMuted);Spacer(Modifier.height(10.dp));Button(onClick=onBackup,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Icon(Icons.Outlined.Backup,null,tint=Color.Black);Spacer(Modifier.width(7.dp));Text("Создать резервную копию",color=Color.Black,fontWeight=FontWeight.Bold)}}}
            "О приложении"->item{ProtoSectionCard{ProtoInfoRow("Приложение","SANSARA");ProtoInfoRow("Версия",BuildConfig.VERSION_NAME);ProtoInfoRow("Роль","Администратор");Text("B2B-платформа ритуальных товаров. Visual Lock: утверждённые reference-экраны не изменяются без отдельного согласования.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))}}
            else->item{ProtoSectionCard{Text("Раздел готов к подключению серверных данных.",color=ProtoText);OutlinedButton(onClick=onClients,modifier=Modifier.fillMaxWidth().padding(top=8.dp),border=BorderStroke(1.dp,ProtoGold)){Text("Открыть клиентов",color=ProtoGold)}}}
        }
    }
}

@Composable
private fun ProtoAttentionScreen(regs:List<ProtoRegistration>,blocked:List<ProtoClient>,picking:List<ProtoOrder>,lowStock:List<ProtoCatalogProduct>,stockValue:(ProtoCatalogProduct)->Int,onBack:()->Unit,onClient:(ProtoClient)->Unit,onOrder:(ProtoOrder)->Unit,onApprove:(ProtoRegistration)->Unit,onClose:(ProtoRegistration)->Unit,onLowStock:()->Unit){
    ProtoScaffold("Требует внимания","Задачи остаются до обработки",onBack){
        item{Text("Новые регистрации",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)}
        if(regs.isEmpty())item{Text("Новых регистраций нет",color=ProtoMuted)} else items(regs,key={it.id}){r->ProtoSectionCard{Text(r.organization,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${r.type} · ${r.contact1} · ${r.phone1}",color=ProtoMuted,fontSize=12.sp);Text("${r.createdAt} · ${r.status}",color=ProtoMuted,fontSize=10.sp);Row(Modifier.padding(top=7.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={onApprove(r)},colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),modifier=Modifier.weight(1f)){Text("Подтвердить",color=Color.Black,fontSize=11.sp)};OutlinedButton(onClick={onClose(r)},border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.weight(1f)){Text("Закрыть",color=ProtoMuted,fontSize=11.sp)}}}}
        item{Text("Приостановленные клиенты",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};if(blocked.isEmpty())item{Text("Нет приостановленных клиентов",color=ProtoMuted)}else items(blocked,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onClient(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Нажмите, чтобы открыть карточку и изменить доступ",color=ProtoMuted,fontSize=12.sp)}}
        item{Text("Заказы на сборке",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};if(picking.isEmpty())item{Text("Нет заказов на сборке",color=ProtoMuted)}else items(picking,key={it.id}){o->ProtoOrderRow(o){onOrder(o)}}
        if(lowStock.isNotEmpty())item{ProtoSectionCard(Modifier.clickable{onLowStock()}){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Warning,null,tint=ProtoOrange);Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text("Низкие остатки: ${lowStock.size}",color=ProtoText,fontWeight=FontWeight.SemiBold);Text(lowStock.take(3).joinToString(" · "){"${it.sku}: ${stockValue(it)}"},color=ProtoMuted,fontSize=10.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}
    }
}

@Composable
private fun ProtoOnlineControllerScreen(clients:List<ProtoClient>,onBack:()->Unit,onClient:(ProtoClient)->Unit){ProtoScaffold("Online controller","Активность приложений",onBack){item{ProtoSectionCard{ProtoInfoRow("Клиенты онлайн","${clients.count{it.online}}");ProtoInfoRow("Администраторы онлайн","1");ProtoInfoRow("Производство онлайн","2");ProtoInfoRow("Синхронизация","автоматическая")}};item{Text("Клиенты сегодня",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold)};items(clients.sortedByDescending{it.online},key={it.id}){c->ProtoSectionCard(Modifier.clickable{onClient(c)}){Row{Box(Modifier.size(9.dp).background(if(c.online)ProtoGreen else ProtoMuted,CircleShape).align(Alignment.CenterVertically));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(c.name,color=ProtoText);Text("Последняя активность: ${c.lastSeen}",color=ProtoMuted,fontSize=11.sp)};Text("${c.orderCount} заказов",color=ProtoGoldSoft,fontSize=11.sp)}}}}}

@Composable
private fun ProtoProductionHomeScreen(
    selectedDate:LocalDate,
    drafts:List<ProtoProductionDraft>,
    opsForDay:List<ProtoProductionOp>,
    products:List<ProtoCatalogProduct>,
    stockOverrides:SnapshotStateMap<String,Int>,
    onDateChange:(LocalDate)->Unit,
    onAdd:()->Unit,
    onPostAll:()->Unit,
    onHistory:()->Unit,
    onReport:()->Unit,
    onPayments:()->Unit,
    onStock:()->Unit,
    onWorkshop:()->Unit,
    onAttendance:()->Unit,
    onChat:()->Unit,
    onHome:()->Unit,
    onProfile:()->Unit
){
    val produced=opsForDay.sumOf{it.qty}
    val draftTotal=drafts.sumOf{it.qty}
    val paid=opsForDay.sumOf{it.amountRub}
    val draftAmount=drafts.sumOf{it.qty*it.rateRub}
    val physical=products.sumOf{stockOverrides[it.sku]?:it.stock}
    val selectedLabel=selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy",ruLocale))
    var showCalendar by remember{mutableStateOf(false)}

    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(
            containerColor=Color.Transparent,
            bottomBar={ProtoProductionBottomBar(onHome,onProduction={},onHistory,onStock,onProfile)}
        ){pad->
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                item{ProtoBrandHeader()}
                item{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("Производство",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold)
                            Text(selectedLabel,color=ProtoMuted,fontSize=12.sp)
                        }
                        Surface(color=ProtoPanel,border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(22.dp),modifier=Modifier.clickable{showCalendar=true}){
                            Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
                                Icon(Icons.Outlined.CalendarMonth,null,tint=ProtoGold,modifier=Modifier.size(20.dp));Spacer(Modifier.width(7.dp));Text("Дата",color=ProtoGoldSoft,fontSize=12.sp);Icon(Icons.Outlined.ExpandMore,null,tint=ProtoGold)
                            }
                        }
                    }
                }
                item{ProtoSectionCard{Text("После проведения данные сразу видят клиент и администратор.",color=ProtoMuted,fontSize=12.sp)}}
                item{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("Выпуск продукции",color=ProtoText,fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        OutlinedButton(onClick=onAdd,border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(22.dp)){Icon(Icons.Outlined.Add,null,tint=ProtoGold);Spacer(Modifier.width(5.dp));Text("Добавить",color=ProtoGold)}
                    }
                }
                if(opsForDay.isEmpty()&&drafts.isEmpty()){
                    item{ProtoSectionCard(Modifier.clickable{onAdd()}){Column(Modifier.fillMaxWidth().padding(vertical=28.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.AddCircleOutline,null,tint=ProtoGold,modifier=Modifier.size(42.dp));Text("Добавить позицию выпуска",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)}}}
                }else{
                    items(opsForDay){op->
                        val p=products.firstOrNull{it.sku==op.sku}
                        ProtoSectionCard{
                            Row(verticalAlignment=Alignment.CenterVertically){
                                if(p!=null)ProtoProductImage(p,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)){
                                    Text(op.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    ProtoSkuText(op.sku,extra=" · "+op.assembler,fontSize=13)
                                    Text("Ставка "+protoMoney(op.rateRub)+" / шт.",color=ProtoMuted,fontSize=10.sp)
                                }
                                Column(horizontalAlignment=Alignment.End){Text(op.qty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Text(protoMoney(op.amountRub),color=ProtoGreen,fontSize=11.sp)}
                            }
                        }
                    }
                    items(drafts){d->
                        ProtoSectionCard{
                            Row(verticalAlignment=Alignment.CenterVertically){
                                ProtoProductImage(d.product,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)){Text(d.product.name,color=ProtoText,fontWeight=FontWeight.SemiBold);ProtoSkuText(d.product.sku,extra=" · "+d.assembler,fontSize=13);Text("Ставка "+protoMoney(d.rateRub)+" / шт.",color=ProtoMuted,fontSize=10.sp)}
                                Column(horizontalAlignment=Alignment.End){Text(d.qty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Text(protoMoney(d.qty*d.rateRub),color=ProtoOrange,fontSize=11.sp)}
                            }
                        }
                    }
                }
                item{
                    ProtoSectionCard{
                        Text("Итого",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)
                        ProtoInfoRow("Количество",(produced+draftTotal).toString()+" шт.")
                        ProtoInfoRow("Проведено к выплате",protoMoney(paid))
                        ProtoInfoRow("На приход",protoMoney(draftAmount))
                        ProtoInfoRow("Физический склад",physical.toString()+" шт.")
                    }
                }
                item{ProtoPrimaryButton("Оприходовать выпуск",onPostAll)}
                item{ProtoSecondaryButton("История приходов",onHistory)}
                item{ProtoSecondaryButton("Отчёт за день",onReport)}
                item{ProtoSecondaryButton("Сборщицы и выплаты",onPayments)}
                item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoSecondaryButton("Задания цеху",onWorkshop,Modifier.weight(1f));ProtoSecondaryButton("Табель",onAttendance,Modifier.weight(1f))}}
                item{ProtoSecondaryButton("Чат с админом",onChat)}
            }
        }
    }
    if(showCalendar)ProtoProductionCalendarDialog(selectedDate,onDismiss={showCalendar=false},onSelect={onDateChange(it);showCalendar=false})
}

@Composable
private fun ProtoProductionCategoryScreen(onBack:()->Unit,onCategory:(String)->Unit){ProtoScaffold("Выпуск продукции","Что произведено в цеху?",onBack){item{listOf("Венки","Венки круглые","Корзины","Флоретки","Полянки").forEach{cat->ProtoSectionCard(Modifier.padding(vertical=4.dp).clickable{onCategory(cat)}){Row(verticalAlignment=Alignment.CenterVertically){Image(painterResource(protoPlaceholderForType(cat)),null,Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop);Spacer(Modifier.width(12.dp));Text(cat,color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}}

@Composable
private fun ProtoProductionCatalogScreen(products:List<ProtoCatalogProduct>,onBack:()->Unit,onSelect:(ProtoCatalogProduct)->Unit){var query by remember{mutableStateOf("")};val filtered=products.filter{query.isBlank()||it.sku.contains(query,true)||it.name.contains(query,true)};ProtoScaffold("Каталог производства","Выберите модель — артикул подставится автоматически",onBack){item{ProtoField(query,{query=it},"Поиск по артикулу или названию")};items(filtered,key={it.sku}){p->ProtoSectionCard(Modifier.clickable{onSelect(p)}){Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Арт. ${p.sku} · ${p.quality} · ${p.size}",color=ProtoMuted,fontSize=11.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}

@Composable
private fun ProtoProductionEntryScreen(
    product:ProtoCatalogProduct?,
    qty:Int,
    assembler:String,
    rateRub:Int,
    assemblers:List<SansaraAssembler>,
    selectedDate:LocalDate,
    onBack:()->Unit,
    onMinus:()->Unit,
    onPlus:()->Unit,
    onAssembler:(String)->Unit,
    onRate:(Int)->Unit,
    onAddDraft:()->Unit,
    onPostNow:()->Unit
){
    val p=product?:return
    val dateLabel=selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy",ruLocale))
    ProtoScaffold("Приход продукции",dateLabel,onBack){
        item{ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.width(12.dp));Column{Text(p.name,color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Арт. "+p.sku,color=ProtoGoldSoft);Text(p.quality+" · "+p.size,color=ProtoMuted)}}}}
        item{
            Text("Сборщица",color=ProtoGoldSoft,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
            if(assemblers.isEmpty())Text("Справочник пуст. Добавьте сборщицу в админке.",color=ProtoRed,fontSize=12.sp)
            else assemblers.forEach{name->
                FilterChip(
                    selected=assembler==name.name,
                    onClick={onAssembler(name.name)},
                    label={Text(name.name,fontSize=14.sp)},
                    modifier=Modifier.fillMaxWidth().height(48.dp),
                    colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText)
                )
            }
        }
        item{Text("Количество",color=ProtoGoldSoft,fontSize=16.sp,fontWeight=FontWeight.SemiBold);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){ProtoQtyButton(Icons.Outlined.Remove,onMinus,56.dp);Text(qty.toString(),color=ProtoText,fontSize=38.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=32.dp));ProtoQtyButton(Icons.Outlined.Add,onPlus,56.dp)}}
        item{
            OutlinedTextField(
                value=if(rateRub==0)"" else rateRub.toString(),
                onValueChange={onRate(it.filter(Char::isDigit).toIntOrNull()?:0)},
                label={Text("Ставка ₽ / шт.")},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                colors=protoFieldColors()
            )
            Text("Сумма: "+protoMoney(qty*rateRub),color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
        }
        item{ProtoPrimaryButton("Добавить в выпуск дня",onAddDraft,enabled=assembler.isNotBlank());Spacer(Modifier.height(8.dp));ProtoSecondaryButton("Оприходовать на склад сразу",onPostNow)}
    }
}

@Composable
private fun ProtoProductionCalendarDialog(selectedDate:LocalDate,onDismiss:()->Unit,onSelect:(LocalDate)->Unit){
    var month by remember(selectedDate){mutableStateOf(YearMonth.from(selectedDate))}
    val first=month.atDay(1)
    val daysInMonth=month.lengthOfMonth()
    val shift=(first.dayOfWeek.value-1).coerceAtLeast(0)
    AlertDialog(
        onDismissRequest=onDismiss,
        containerColor=ProtoPanel,
        title={
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={month=month.minusMonths(1)}){Icon(Icons.Outlined.ChevronLeft,null,tint=ProtoGold)}
                Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",ruLocale)).replaceFirstChar{if(it.isLowerCase())it.titlecase(ruLocale)else it.toString()},color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick={month=month.plusMonths(1)}){Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}
            }
        },
        text={
            Column{
                Row(Modifier.fillMaxWidth()){
                    listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach{d->Text(d,color=ProtoMuted,fontSize=10.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
                }
                Spacer(Modifier.height(6.dp))
                val cells=(shift+daysInMonth).let{((it+6)/7)*7}
                (0 until cells).chunked(7).forEach{week->
                    Row(Modifier.fillMaxWidth()){
                        week.forEach{cell->
                            val day=cell-shift+1
                            if(day in 1..daysInMonth){
                                val date=month.atDay(day)
                                val selected=date==selectedDate
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if(selected)ProtoGold else Color.Transparent)
                                        .clickable{onSelect(date)},
                                    contentAlignment=Alignment.Center
                                ){Text(day.toString(),color=if(selected)Color.Black else ProtoText,fontSize=12.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)}
                            } else Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        },
        confirmButton={TextButton(onClick={onSelect(LocalDate.now())}){Text("Сегодня",color=ProtoGold)}},
        dismissButton={TextButton(onClick=onDismiss){Text("Закрыть",color=ProtoMuted)}}
    )
}

@Composable
private fun ProtoProductionHistoryScreen(
    ops:List<ProtoProductionOp>,
    products:List<ProtoCatalogProduct>,
    canEdit:Boolean,
    onBack:()->Unit,
    onExport:()->Unit,
    onCorrect:(ProtoProductionOp,Int)->Unit,
    onDelete:(ProtoProductionOp)->Unit
){
    var query by remember{mutableStateOf("")}
    var assembler by remember{mutableStateOf("Все")}
    var editing by remember{mutableStateOf<ProtoProductionOp?>(null)}
    var deleteCandidate by remember{mutableStateOf<ProtoProductionOp?>(null)}
    val assemblers=listOf("Все")+ops.map{it.assembler}.filter{it.isNotBlank()}.distinct().sorted()
    val filtered=ops.filter{op->
        op.status!="Удален" &&
        (query.isBlank()||op.sku.contains(query,true)||op.name.contains(query,true)||op.date.contains(query,true)) &&
        (assembler=="Все"||op.assembler==assembler)
    }
    val byMonth=filtered.groupBy{op->
        runCatching{LocalDate.parse(op.date,DateTimeFormatter.ofPattern("dd.MM.yyyy")).format(DateTimeFormatter.ofPattern("MM.yyyy"))}.getOrDefault("Без даты")
    }.toSortedMap(compareByDescending{it})
    ProtoScaffold("История приходов","Месяц → день → позиции",onBack){
        item{ProtoField(query,{query=it},"Дата / артикул / наименование")}
        item{
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                items(assemblers){name->
                    FilterChip(selected=assembler==name,onClick={assembler=name},label={Text(name)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))
                }
            }
        }
        item{OutlinedButton(onClick=onExport,modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(24.dp)){Icon(Icons.Outlined.FileDownload,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Экспорт CSV",color=ProtoGold)}}
        byMonth.forEach{(month,monthOps)->
            item{Text(protoReportMonthLabel(month),color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
            monthOps.groupBy{it.date}.toSortedMap(compareByDescending{it}).forEach{(day,dayOps)->
                item{Text(day+" · "+dayOps.sumOf{it.qty}+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)}
                items(dayOps,key={it.opId.ifBlank{it.date+"-"+it.time+"-"+it.sku+"-"+it.assembler}}){op->
                    val product=products.firstOrNull{it.sku==op.sku}
                    ProtoSectionCard{
                        Row(verticalAlignment=Alignment.CenterVertically){
                            if(product!=null)ProtoProductImage(product,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)){
                                Text(op.name,color=ProtoText,fontWeight=FontWeight.SemiBold)
                                Text(op.time,color=ProtoMuted,fontSize=11.sp);ProtoSkuText(op.sku,fontSize=13)
                                Text("Сборщица: "+op.assembler,color=ProtoMuted,fontSize=11.sp)
                                if(op.documentId.isNotBlank())Text(op.documentId,color=ProtoGoldSoft,fontSize=9.sp)
                            }
                            Column(horizontalAlignment=Alignment.End){
                                Text(op.qty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                                Text(protoMoney(op.amountRub),color=ProtoGreen,fontWeight=FontWeight.Bold,fontSize=11.sp)
                                if(canEdit && op.opId.isNotBlank()){
                                    Row{
                                        IconButton(onClick={editing=op}){Icon(Icons.Outlined.Edit,null,tint=ProtoGold,modifier=Modifier.size(19.dp))}
                                        IconButton(onClick={deleteCandidate=op}){Icon(Icons.Outlined.Delete,null,tint=ProtoRed,modifier=Modifier.size(19.dp))}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let{op->
        var qtyText by remember(op.opId){mutableStateOf(op.qty.toString())}
        AlertDialog(
            onDismissRequest={editing=null},containerColor=ProtoPanel,
            title={Text("Корректировать выпуск",color=ProtoText)},
            text={Column{Text(op.name,color=ProtoGoldSoft);ProtoField(qtyText,{qtyText=it.filter(Char::isDigit)},"Количество",KeyboardType.Number);Text("Изменение попадёт в журнал и скорректирует склад.",color=ProtoMuted,fontSize=10.sp)}},
            confirmButton={TextButton(onClick={val q=qtyText.toIntOrNull()?:0;if(q>0){onCorrect(op,q);editing=null}}){Text("Сохранить",color=ProtoGold)}},
            dismissButton={TextButton(onClick={editing=null}){Text("Отмена",color=ProtoMuted)}}
        )
    }
    deleteCandidate?.let{op->
        AlertDialog(
            onDismissRequest={deleteCandidate=null},containerColor=ProtoPanel,
            title={Text("Удалить выпуск?",color=ProtoText)},
            text={Text("Операция будет помечена удалённой, склад скорректируется, действие сохранится в журнале.",color=ProtoMuted)},
            confirmButton={TextButton(onClick={onDelete(op);deleteCandidate=null}){Text("Удалить",color=ProtoRed)}},
            dismissButton={TextButton(onClick={deleteCandidate=null}){Text("Отмена",color=ProtoMuted)}}
        )
    }
}

private fun protoReportMonthLabel(key:String):String{
    val parts=key.split(".")
    if(parts.size!=2)return key
    val ym=runCatching{YearMonth.of(parts[1].toInt(),parts[0].toInt())}.getOrNull()?:return key
    return protoMonthLabel(ym.toString())
}

@Composable
private fun ProtoProductionReportScreen(selectedDate:LocalDate,ops:List<ProtoProductionOp>,products:List<ProtoCatalogProduct>,onBack:()->Unit){
    val totalQty=ops.sumOf{it.qty}
    val wreathQty=ops.filter{op->products.firstOrNull{it.sku==op.sku}?.type?.contains("Венки",true)==true}.sumOf{it.qty}
    val totalAmount=ops.sumOf{it.amountRub}
    val byAssembler=ops.groupBy{it.assembler}.mapValues{entry->entry.value.sumOf{it.qty} to entry.value.sumOf{it.amountRub}}.toList().sortedByDescending{it.second.second}
    val bySku=ops.groupBy{it.sku}.map{entry->Triple(entry.key,entry.value.firstOrNull()?.name.orEmpty(),entry.value.sumOf{it.qty})}.sortedByDescending{it.third}
    val byQuality=ops.groupBy{op->products.firstOrNull{it.sku==op.sku}?.quality?.ifBlank{"Без категории"}?:"Без категории"}.mapValues{entry->entry.value.sumOf{it.qty}}.toList().sortedByDescending{it.second}
    val dateLabel=selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy",ruLocale))
    ProtoScaffold("Отчёт производства",dateLabel,onBack){
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Всего",totalQty.toString(),"изделий",Modifier.weight(1f),{});ProtoMetricCard("Венки",wreathQty.toString(),"шт.",Modifier.weight(1f),{})}}
        item{ProtoMetricCard("Начислено",protoMoney(totalAmount),"сборщицам за день",Modifier.fillMaxWidth(),{})}
        item{Text("По сборщицам",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
        items(byAssembler){row->ProtoSectionCard{Row{Text(row.first,color=ProtoText,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(row.second.first.toString()+" шт. · "+protoMoney(row.second.second),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}
        item{Text("По артикулам",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
        items(bySku){row->ProtoSectionCard{Text(row.second,color=ProtoText,fontWeight=FontWeight.SemiBold);Row{Text(row.first,color=ProtoMuted,fontSize=11.sp);Spacer(Modifier.weight(1f));Text(row.third.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}
        item{Text("По качеству",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
        items(byQuality){row->ProtoSectionCard{Row{Text(row.first,color=ProtoText,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(row.second.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}
    }
}

@Composable
private fun ProtoProductionPaymentsScreen(ops:List<ProtoProductionOp>,onBack:()->Unit,onExport:()->Unit){
    var period by remember{mutableStateOf("Месяц")}
    val today=LocalDate.now()
    fun parse(value:String)=runCatching{LocalDate.parse(value,DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrNull()
    val filtered=ops.filter{op->
        val d=parse(op.date)?:return@filter period=="Всё"
        when(period){
            "День"->d==today
            "Неделя"->!d.isBefore(today.minusDays(6))&& !d.isAfter(today)
            "Месяц"->d.month==today.month&&d.year==today.year
            else->true
        }
    }
    val grouped=filtered.groupBy{it.assembler}.mapValues{entry->entry.value.sumOf{it.qty} to entry.value.sumOf{it.amountRub}}.toList().sortedByDescending{it.second.second}
    ProtoScaffold("Сборщицы и выплаты","Начисления по проведённому выпуску",onBack){
        item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("День","Неделя","Месяц","Всё").forEach{label->FilterChip(selected=period==label,onClick={period=label},label={Text(label)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))}}}
        item{ProtoSectionCard{ProtoInfoRow("Количество",filtered.sumOf{it.qty}.toString()+" шт.");ProtoInfoRow("Начислено",protoMoney(filtered.sumOf{it.amountRub}))}}
        if(grouped.isEmpty())item{Text("Нет проведённых операций за выбранный период",color=ProtoMuted)}
        else items(grouped){row->ProtoSectionCard{Text(row.first,color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.Bold);ProtoInfoRow("Собрано",row.second.first.toString()+" шт.");ProtoInfoRow("К выплате",protoMoney(row.second.second))}}
        item{ProtoSecondaryButton("Экспорт CSV",onExport)}
    }
}

@Composable
private fun ProtoStaffProfileScreen(role:String,onBack:()->Unit,onCall:()->Unit,onLogout:()->Unit){
    ProtoScaffold("Профиль",role,onBack){
        item{ProtoSectionCard{ProtoInfoRow("Роль",role);ProtoInfoRow("Дата",currentDateLong());ProtoInfoRow("Версия",BuildConfig.VERSION_NAME);ProtoInfoRow("Поддержка",BuildConfig.ADMIN_PHONE);Spacer(Modifier.height(8.dp));OutlinedButton(onClick=onCall,modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Phone,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Позвонить в поддержку",color=ProtoGold)};TextButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Выйти",color=ProtoMuted)}}}
    }
}

@Composable
private fun ProtoServerScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,productionOps:List<ProtoProductionOp>,orders:List<ProtoOrder>,clients:List<ProtoClient>,reservedForSku:(String)->Int,onBack:()->Unit,onProduced:()->Unit,onStock:()->Unit,onReserve:()->Unit,onNewClients:()->Unit,onOnline:()->Unit,onExport:()->Unit){
    val reserve=products.sumOf{reservedForSku(it.sku)};val available=products.sumOf{p->((stockOverrides[p.sku]?:p.stock)-reservedForSku(p.sku)).coerceAtLeast(0)};val monthProduced=productionOps.sumOf{it.qty};val monthKey=LocalDate.now().format(DateTimeFormatter.ofPattern("MM.yyyy"));val newClients=clients.count{it.registeredAt.endsWith(monthKey)}
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{ProtoBrandHeader(onBack=onBack)};item{Text("Сервер данных",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Учёт по месяцам · "+currentMonthLabel(),color=ProtoMuted,fontSize=13.sp)};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Выпуск",monthProduced.toString(),"",Modifier.weight(1f),onProduced);ProtoMetricCard("Резерв",reserve.toString(),"",Modifier.weight(1f),onReserve)}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Доступно",available.toString(),"",Modifier.weight(1f),onStock);ProtoMetricCard("Новые клиенты",newClients.toString(),"",Modifier.weight(1f),onNewClients)}};item{Text("Товары",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)};items(products.take(12)){p->val ph=stockOverrides[p.sku]?:p.stock;val rs=reservedForSku(p.sku);ProtoSectionCard{Row{Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(p.sku,color=ProtoMuted,fontSize=10.sp)};Column(horizontalAlignment=Alignment.End){Text("Остаток $ph",color=ProtoText,fontSize=11.sp);Text("Резерв $rs · Доступно "+(ph-rs).coerceAtLeast(0),color=ProtoGoldSoft,fontSize=11.sp)}}}};item{ProtoSectionCard(Modifier.clickable{onOnline()}){ProtoInfoRow("Клиенты онлайн",clients.count{it.online}.toString());ProtoInfoRow("Администратор","Онлайн");ProtoInfoRow("Производство","Онлайн")}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoSecondaryButton("Экспорт",onExport,Modifier.weight(1f));ProtoSecondaryButton("Остатки",onStock,Modifier.weight(1f))}}}}
}

@Composable
private fun ProtoStockListScreen(
    products:List<ProtoCatalogProduct>,
    stockOverrides:SnapshotStateMap<String,Int>,
    reservedForSku:(String)->Int,
    canAdjust:Boolean,
    onBack:()->Unit,
    onAdjust:(ProtoCatalogProduct,Int,String)->Unit
){
    var category by remember{mutableStateOf<String?>(null)}
    var group by remember{mutableStateOf<String?>(null)}
    var query by remember{mutableStateOf("")}
    var adjustProduct by remember{mutableStateOf<ProtoCatalogProduct?>(null)}
    var deltaText by remember{mutableStateOf("")}
    var reason by remember{mutableStateOf("")}
    fun physical(p:ProtoCatalogProduct)=stockOverrides[p.sku]?:p.stock
    fun available(p:ProtoCatalogProduct)=(physical(p)-reservedForSku(p.sku)).coerceAtLeast(0)
    fun categoryOf(p:ProtoCatalogProduct)=protoProductCategoryKey(p.type)
    fun groupOf(p:ProtoCatalogProduct):String {
        val cat=categoryOf(p)
        return when(cat){
            "Венки"->p.quality.ifBlank{"Без группы"}
            else->p.quality.takeIf{it.isNotBlank()&&it!="—"}?:p.type.ifBlank{"Без группы"}
        }
    }
    val allTotal=products.sumOf{physical(it)}
    val allAvailable=products.sumOf{available(it)}
    val categories=products.groupBy{categoryOf(it)}.toSortedMap()
    val title=when{
        category==null->"Склад"
        group==null->category!!
        else->category+" · "+group
    }
    val subtitle=when{
        category==null->"Все категории · физический остаток "+allTotal+" шт. · доступно "+allAvailable+" шт."
        group==null->"Группы товаров"
        else->"Артикулы и остатки"
    }
    ProtoScaffold(title,subtitle,onBack={
        when{
            group!=null->group=null
            category!=null->category=null
            else->onBack()
        }
    }){
        when{
            category==null->{
                item{ProtoSectionCard{ProtoInfoRow("Остатки всего",allTotal.toString()+" шт.");ProtoInfoRow("Доступно",allAvailable.toString()+" шт.");ProtoInfoRow("Резерв",products.sumOf{reservedForSku(it.sku)}.toString()+" шт.")}}
                items(categories.entries.toList(),key={it.key}){entry->
                    val physicalQty=entry.value.sumOf{physical(it)}
                    val availableQty=entry.value.sumOf{available(it)}
                    ProtoSectionCard(Modifier.clickable{category=entry.key;group=null}){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Box(Modifier.size(44.dp).background(ProtoPanel2,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(protoIconForType(entry.key),null,tint=ProtoGold)}
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)){Text(entry.key,color=ProtoText,fontWeight=FontWeight.Bold);Text(entry.value.size.toString()+" артикулов",color=ProtoMuted,fontSize=10.sp)}
                            Column(horizontalAlignment=Alignment.End){Text(physicalQty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Text("доступно "+availableQty,color=ProtoMuted,fontSize=9.sp)}
                            Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)
                        }
                    }
                }
            }
            group==null->{
                val subset=products.filter{categoryOf(it)==category}
                val groups=subset.groupBy{groupOf(it)}.toSortedMap()
                items(groups.entries.toList(),key={it.key}){entry->
                    ProtoSectionCard(Modifier.clickable{group=entry.key}){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){Text(entry.key,color=ProtoText,fontWeight=FontWeight.Bold);Text(entry.value.size.toString()+" артикулов",color=ProtoMuted,fontSize=10.sp)}
                            Column(horizontalAlignment=Alignment.End){Text(entry.value.sumOf{physical(it)}.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Text("доступно "+entry.value.sumOf{available(it)},color=ProtoMuted,fontSize=9.sp)}
                            Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)
                        }
                    }
                }
            }
            else->{
                item{ProtoField(query,{query=it},"Поиск по артикулу или названию")}
                val filtered=products.filter{categoryOf(it)==category&&groupOf(it)==group&&(query.isBlank()||it.sku.contains(query,true)||it.name.contains(query,true))}
                items(filtered,key={it.sku}){p->
                    val ph=physical(p);val reserved=reservedForSku(p.sku);val av=available(p)
                    ProtoSectionCard(Modifier.clickable(enabled=canAdjust){adjustProduct=p;deltaText="";reason=""}){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            ProtoProductImage(p,Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);ProtoSkuText(p.sku,fontSize=12);Text("Факт: "+ph+" · Резерв: "+reserved,color=ProtoMuted,fontSize=10.sp)}
                            Column(horizontalAlignment=Alignment.End){Text(av.toString()+" шт.",color=if(av>0)ProtoGreen else ProtoRed,fontWeight=FontWeight.Bold);Text("доступно",color=ProtoMuted,fontSize=9.sp);if(canAdjust)Text("Корректировать",color=ProtoGold,fontSize=9.sp)}
                        }
                    }
                }
            }
        }
    }
    adjustProduct?.let{product->
        AlertDialog(
            onDismissRequest={adjustProduct=null},containerColor=ProtoPanel,
            title={Text("Корректировка склада",color=ProtoText)},
            text={Column{Text(product.name,color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);ProtoField(deltaText,{v->deltaText=v.filter{it.isDigit()||it=='-'}},"Изменение количества",KeyboardType.Number);ProtoField(reason,{reason=it},"Причина");Text("Положительное число увеличит остаток, отрицательное уменьшит.",color=ProtoMuted,fontSize=10.sp)}},
            confirmButton={TextButton(onClick={val delta=deltaText.toIntOrNull()?:0;if(delta!=0&&reason.isNotBlank()){onAdjust(product,delta,reason);adjustProduct=null}}){Text("Провести",color=ProtoGold)}},
            dismissButton={TextButton(onClick={adjustProduct=null}){Text("Отмена",color=ProtoMuted)}}
        )
    }
}

@Composable
private fun ProtoLowStockListScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,reservedForSku:(String)->Int,threshold:Int,onBack:()->Unit){val low=products.map{p->Triple(p,stockOverrides[p.sku]?:p.stock,reservedForSku(p.sku))}.filter{(p,physical,reserved)->(physical-reserved).coerceAtLeast(0)<=threshold};ProtoScaffold("Низкие остатки","Порог: ≤ $threshold шт.",onBack){if(low.isEmpty())item{Text("Все остатки выше установленного порога",color=ProtoGreen)}else items(low,key={it.first.sku}){(p,physical,reserved)->val available=(physical-reserved).coerceAtLeast(0);ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${p.sku} · факт $physical · резерв $reserved",color=ProtoMuted,fontSize=10.sp)};Text("$available",color=ProtoOrange,fontSize=20.sp,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoReserveListScreen(orders:List<ProtoOrder>,onBack:()->Unit){ProtoScaffold("Резерв","За кем закреплены товары",onBack){items(orders.filter{it.status!="Доставлен"},key={it.id}){o->ProtoSectionCard{Text(o.clientName,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${o.id} · ${o.pieces} шт. · ${protoMoney(o.total)}",color=ProtoMuted,fontSize=11.sp);o.lines.forEach{l->Text("${l.sku} — ${l.qty} шт.",color=ProtoGoldSoft,fontSize=11.sp)}}}}}

@Composable
private fun ProtoNewClientsScreen(clients:List<ProtoClient>,onBack:()->Unit,onOpen:(ProtoClient)->Unit){val monthKey=LocalDate.now().format(DateTimeFormatter.ofPattern("MM.yyyy"));val current=clients.filter{it.registeredAt.endsWith(monthKey)};ProtoScaffold("Новые клиенты",currentMonthLabel(),onBack){items(current,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onOpen(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.orderCount} заказов · оборот ${protoMoney(c.monthTurnover)}",color=ProtoMuted,fontSize=11.sp)}}}}

@Composable
private fun ProtoExportScreen(onBack:()->Unit,onExport:(String)->Unit){var report by remember{mutableStateOf("Остатки на складе")};ProtoScaffold("Экспорт","Выберите отчёт — CSV сохраняется в Downloads",onBack){item{ProtoSectionCard{listOf("Остатки на складе","Резерв по клиентам","Выпуск за месяц","Производство по сборщицам","Заказы за месяц").forEach{r->Row(Modifier.fillMaxWidth().clickable{report=r}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=report==r,onClick={report=r},colors=RadioButtonDefaults.colors(selectedColor=ProtoGold));Text(r,color=ProtoText)}};Button(onClick={onExport(report)},modifier=Modifier.fillMaxWidth().padding(top=8.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Icon(Icons.Outlined.FileDownload,null,tint=Color.Black);Spacer(Modifier.width(7.dp));Text("Экспортировать CSV",color=Color.Black,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoRegistrationSentScreen(reg:ProtoRegistration?,onBack:()->Unit){
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Column(Modifier.fillMaxSize().padding(horizontal=18.dp),horizontalAlignment=Alignment.CenterHorizontally){ProtoBrandHeader(showBell=false);Spacer(Modifier.height(38.dp));Box(Modifier.size(112.dp).border(3.dp,ProtoGold,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Check,null,tint=ProtoGold,modifier=Modifier.size(64.dp))};Text("Заявка отправлена",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=20.dp));Text("Спасибо за регистрацию. Мы проверим данные и откроем доступ к каталогу и заказам.",color=ProtoMuted,fontSize=14.sp,lineHeight=20.sp,modifier=Modifier.padding(vertical=14.dp));if(reg!=null)ProtoSectionCard{ProtoInfoRow("Тип клиента",reg.type);ProtoInfoRow("Организация",reg.organization);ProtoInfoRow("Телефон",reg.phone1);ProtoInfoRow("E-mail",reg.email)};Spacer(Modifier.weight(1f));ProtoPrimaryButton("Понятно",onBack);Spacer(Modifier.height(22.dp))}}
}

@Composable
private fun ProtoSimpleMessageScreen(title:String,text:String,onBack:()->Unit){
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Column(Modifier.fillMaxSize().padding(horizontal=18.dp),horizontalAlignment=Alignment.CenterHorizontally){
            ProtoBrandHeader(showBell=false)
            Spacer(Modifier.height(64.dp))
            Box(Modifier.size(104.dp).border(3.dp,ProtoGold,CircleShape),contentAlignment=Alignment.Center){
                Icon(Icons.Outlined.CheckCircle,null,tint=ProtoGold,modifier=Modifier.size(58.dp))
            }
            Text(title,color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=22.dp))
            Text(text,color=ProtoMuted,fontSize=14.sp,lineHeight=20.sp,modifier=Modifier.padding(vertical=14.dp))
            Spacer(Modifier.weight(1f))
            ProtoPrimaryButton("Готово",onBack)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProtoScaffold(title:String,subtitle:String?=null,onBack:(()->Unit)?=null,content:LazyListScope.()->Unit){
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack);Column(Modifier.padding(horizontal=16.dp)){Text(title,color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold);if(subtitle!=null)Text(subtitle,color=ProtoMuted,fontSize=12.sp)};LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}
}

@Composable
private fun ProtoHeader(title:String,subtitle:String,onBack:()->Unit){Row(verticalAlignment=Alignment.CenterVertically){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp));Column{Text(title,color=ProtoText,fontSize=27.sp,fontWeight=FontWeight.Bold);Text(subtitle,color=ProtoMuted,fontSize=11.sp)}}}

@Composable
private fun ProtoCircleBack(onClick:()->Unit){Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xCC11100E)).border(1.dp,ProtoGold,CircleShape).clickable(onClick=onClick),contentAlignment=Alignment.Center){Icon(Icons.Outlined.ArrowBack,null,tint=ProtoGold)}}

@Composable
private fun ProtoBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 14.dp, top = 34.dp)
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xCC11100E))
            .border(1.dp, ProtoGold, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.ArrowBack, null, tint = ProtoGold, modifier = Modifier.size(25.dp))
    }
}

@Composable
private fun BoxScope.PrototypeClickArea(
    maxWidth: Dp,
    maxHeight: Dp,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .offset(x = maxWidth * x, y = maxHeight * y)
            .width(maxWidth * w)
            .height(maxHeight * h)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ProtoQtyButton(icon: androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit,size:Dp=34.dp){
    Surface(color=ProtoPanel2,border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(10.dp),modifier=Modifier.size(size).clickable(onClick=onClick)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(icon,null,tint=ProtoGold,modifier=Modifier.size((size.value*.48f).dp))}}
}

@Composable
private fun BoxScope.ProtoDevelopmentMiniBadge(maxWidth:Dp,maxHeight:Dp,x:Float,y:Float,w:Float,h:Float){
    Box(Modifier.offset(maxWidth*x,maxHeight*y).size(maxWidth*w,maxHeight*h).background(Color.Black.copy(alpha=.38f),RoundedCornerShape(10.dp)),contentAlignment=Alignment.BottomCenter){Surface(color=ProtoPanel.copy(alpha=.94f),shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.padding(bottom=4.dp)){Text("В разработке",color=ProtoMuted,fontSize=7.sp,maxLines=1,modifier=Modifier.padding(horizontal=4.dp,vertical=2.dp))}}
}

@Composable
private fun BoxScope.ProtoDevelopmentOverlay(maxWidth:Dp,maxHeight:Dp,x:Float,y:Float,w:Float,h:Float){
    Box(Modifier.offset(maxWidth*x,maxHeight*y).size(maxWidth*w,maxHeight*h).background(Color.Black.copy(alpha=.52f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Surface(color=ProtoPanel.copy(alpha=.94f),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(18.dp)){Text("В разработке",color=ProtoMuted,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp))}}
}

@Composable
private fun BoxWithConstraintsScope.ProtoReferenceCartBadge(count:Int){
    Box(Modifier.offset(maxWidth*.488f,maxHeight*.908f).size(32.dp).background(ProtoPanel,CircleShape))
    if(count>0){
        Box(Modifier.offset(maxWidth*.505f,maxHeight*.912f).size(23.dp).background(ProtoGold,CircleShape),contentAlignment=Alignment.Center){
            Text(count.coerceAtMost(99).toString(),color=Color.Black,fontSize=9.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun ProtoProductImagePreview(product:ProtoCatalogProduct,onDismiss:()->Unit){
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        BackHandler { onDismiss() }
        Box(Modifier.fillMaxSize().background(Color.Black)){
            ProtoProductImage(product,Modifier.fillMaxSize().padding(bottom=190.dp),ContentScale.Fit)
            IconButton(onClick=onDismiss,modifier=Modifier.align(Alignment.TopEnd).padding(18.dp).size(50.dp).background(Color.Black.copy(alpha=.72f),CircleShape).border(1.dp,ProtoGold,CircleShape)){Icon(Icons.Outlined.Close,null,tint=ProtoGold)}
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(190.dp).background(ProtoPanel.copy(alpha=.98f)).border(BorderStroke(1.dp,ProtoBorder))){
                Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=14.dp)){
                    Text(product.name,color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    ProtoSkuText(product.sku,fontSize=14)
                    ProtoInfoRow("Качество",product.quality)
                    ProtoInfoRow("Размер",product.size)
                    ProtoInfoRow("Категория",product.type)
                }
            }
        }
    }
}

@Composable
private fun ProtoSectionCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),content=content)}}

@Composable
private fun ProtoField(value:String,onChange:(String)->Unit,label:String,keyboardType:KeyboardType=KeyboardType.Text){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=keyboardType),colors=protoFieldColors())}

@Composable
private fun protoChatFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=ProtoGold,unfocusedBorderColor=ProtoBorder,focusedTextColor=ProtoText,unfocusedTextColor=ProtoText,focusedLabelColor=ProtoGold,unfocusedLabelColor=ProtoMuted,focusedContainerColor=ProtoPanel2,unfocusedContainerColor=ProtoPanel2,cursorColor=ProtoGold,focusedPlaceholderColor=ProtoMuted,unfocusedPlaceholderColor=ProtoMuted)

@Composable
private fun protoFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=ProtoGold,unfocusedBorderColor=ProtoBorder,focusedTextColor=ProtoText,unfocusedTextColor=ProtoText,focusedLabelColor=ProtoGold,unfocusedLabelColor=ProtoMuted,focusedLeadingIconColor=ProtoGold,unfocusedLeadingIconColor=ProtoMuted,disabledBorderColor=ProtoBorder,disabledTextColor=ProtoMuted)

@Composable
private fun ProtoCompactGrid(title:String,options:List<String>,selected:Set<String>,toggle:(String)->Unit,disabled:Set<String> = emptySet()){Text(title,color=ProtoGoldSoft,fontSize=17.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=7.dp,bottom=6.dp));options.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{o->val off=o in disabled;FilterChip(selected=o in selected,onClick={if(!off)toggle(o)},enabled=!off,label={Text(if(off)"$o · в разработке" else o,fontSize=if(off)12.sp else 15.sp,maxLines=2)},modifier=Modifier.weight(1f).height(48.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText,disabledLabelColor=ProtoMuted))};if(row.size==1)Spacer(Modifier.weight(1f))};Spacer(Modifier.height(8.dp))}
}

@Composable
private fun ProtoCategoryButton(label:String,disabled:Boolean,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,enabled=true,modifier=modifier.height(46.dp),border=BorderStroke(1.dp,if(disabled)ProtoBorder else ProtoGold),contentPadding=PaddingValues(horizontal=5.dp)){Text(if(disabled)"$label · скоро" else label,color=if(disabled)ProtoMuted else ProtoGold,fontSize=11.sp,maxLines=1)}}

@Composable
private fun ProtoPopularRow(p:ProtoCatalogProduct,stock:Int,onOpen:()->Unit){
    Card(
        colors=CardDefaults.cardColors(containerColor=ProtoPanel),
        border=BorderStroke(1.dp,ProtoBorder),
        shape=RoundedCornerShape(14.dp),
        modifier=Modifier.fillMaxWidth().height(112.dp)
    ){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            ProtoProductImage(p,Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)){
                Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                ProtoSkuText(p.sku)
                Text(if(stock>0)"В наличии $stock шт." else "Под заказ · от ${p.productionDays} дней",color=if(stock>0)ProtoGreen else ProtoGoldSoft,fontSize=10.sp)
                Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
            }
            Button(onClick=onOpen,colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.width(92.dp)){
                Text("В корзину",color=Color.Black,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)
            }
        }
    }
}
@Composable
private fun ProtoCartRow(p:ProtoCatalogProduct,qty:Int,onMinus:()->Unit,onPlus:()->Unit,onDelete:()->Unit){
    Card(
        colors=CardDefaults.cardColors(containerColor=ProtoPanel),
        border=BorderStroke(1.dp,ProtoBorder),
        shape=RoundedCornerShape(14.dp),
        modifier=Modifier.height(112.dp)
    ){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            ProtoProductImage(p,Modifier.size(64.dp).clip(RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)){
                Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                ProtoSkuText(p.sku)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    ProtoQtyButton(Icons.Outlined.Remove,onMinus)
                    Text(qty.toString(),color=ProtoText,fontWeight=FontWeight.Bold)
                    ProtoQtyButton(Icons.Outlined.Add,onPlus)
                }
            }
            Column(horizontalAlignment=Alignment.End){
                Text(protoMoney(p.price*qty),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                IconButton(onClick=onDelete){Icon(Icons.Outlined.Delete,null,tint=ProtoMuted)}
            }
        }
    }
}
@Composable
private fun ProtoOrderRow(o:ProtoOrder,onClick:()->Unit){ProtoSectionCard(Modifier.clickable(onClick=onClick)){Row{Column(Modifier.weight(1f)){Text(o.id,color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Text(o.clientName,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${o.pieces} шт. · ${o.dateTime}",color=ProtoMuted,fontSize=11.sp)};Column(horizontalAlignment=Alignment.End){Text(protoMoney(o.total),color=ProtoGoldSoft,fontWeight=FontWeight.Bold);ProtoPill(o.status,ProtoGreen)}}}}

@Composable
private fun ProtoMetricCard(title:String,value:String,subtitle:String,modifier:Modifier,onClick:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=modifier.clickable(onClick=onClick)){Column(Modifier.padding(12.dp)){Text(title,color=ProtoMuted,fontSize=11.sp);Text(value,color=ProtoText,fontSize=25.sp,fontWeight=FontWeight.Bold);Text(subtitle,color=ProtoGoldSoft,fontSize=10.sp)}}}

@Composable
private fun ProtoQuickButton(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,modifier=modifier.height(64.dp),border=BorderStroke(1.dp,ProtoBorder),contentPadding=PaddingValues(3.dp)){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=ProtoGold,modifier=Modifier.size(20.dp));Text(label,color=ProtoText,fontSize=9.sp,maxLines=1)}}}

@Composable
private fun ProtoAttentionLine(title:String,subtitle:String,color:Color,onClick:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.fillMaxWidth().padding(vertical=3.dp).clickable(onClick=onClick)){Row(Modifier.padding(11.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(9.dp).background(color,CircleShape));Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(title,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(subtitle,color=ProtoMuted,fontSize=10.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}

@Composable
private fun ProtoSwitchRow(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,color=ProtoText,modifier=Modifier.weight(1f));Switch(checked=value,onCheckedChange=onChange,colors=SwitchDefaults.colors(checkedThumbColor=Color.Black,checkedTrackColor=ProtoGold))}}

@Composable
private fun ProtoSkuText(sku:String,extra:String="",fontSize:Int=13){
    Text(
        "Арт. "+sku+extra,
        color=ProtoGoldSoft,
        fontSize=fontSize.sp,
        fontWeight=FontWeight.Bold,
        maxLines=1,
        overflow=TextOverflow.Ellipsis
    )
}

@Composable
private fun ProtoInfoRow(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(label,color=ProtoMuted);Spacer(Modifier.weight(1f));Text(value,color=ProtoText,fontWeight=FontWeight.Medium)}}

@Composable
private fun ProtoPill(text:String,color:Color){Surface(color=color.copy(alpha=.16f),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,color.copy(alpha=.55f))){Text(text,color=color,fontSize=10.sp,modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp))}}

@Composable
private fun ProtoActionChip(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,modifier=modifier.height(44.dp),border=BorderStroke(1.dp,ProtoBorder)){Icon(icon,null,tint=ProtoGold,modifier=Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(text,color=ProtoText,fontSize=11.sp)}}

@Composable
private fun ProtoClientBottomBar(current:ProtoScreen,cartCount:Int,onHome:()->Unit,onCatalog:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    val catalog=current in setOf(ProtoScreen.Catalog,ProtoScreen.Filter,ProtoScreen.ProductList,ProtoScreen.ProductDetail)
    val cart=current in setOf(ProtoScreen.Cart,ProtoScreen.Checkout)
    val orders=current in setOf(ProtoScreen.OrderList,ProtoScreen.OrderDetail,ProtoScreen.OrderSent)
    val chat=current==ProtoScreen.ClientChat
    NavigationBar(containerColor=ProtoPanel,tonalElevation=0.dp,modifier=Modifier.height(66.dp)){
        ProtoNavItem(current==ProtoScreen.Home,"Главная",Icons.Outlined.Home,onHome)
        ProtoNavItem(catalog,"Каталог",Icons.Outlined.Inventory2,onCatalog)
        NavigationBarItem(
            selected=cart,
            onClick=onCart,
            icon={
                BadgedBox(badge={if(cartCount>0)Badge(containerColor=ProtoGold){Text(cartCount.toString(),color=Color.Black)}}){
                    Icon(Icons.Outlined.ShoppingCart,null)
                }
            },
            label={Text("Корзина",fontSize=9.sp)},
            colors=NavigationBarItemDefaults.colors(
                selectedIconColor=ProtoGold,
                selectedTextColor=ProtoGold,
                indicatorColor=Color.Black,
                unselectedIconColor=ProtoMuted,
                unselectedTextColor=ProtoMuted
            )
        )
        ProtoNavItem(orders,"Заказы",Icons.Outlined.ReceiptLong,onOrders)
        ProtoNavItem(chat,"Чат",Icons.Outlined.ChatBubbleOutline,onProfile)
    }
}
@Composable
private fun ProtoRetailBottomBar(
    current:ProtoScreen,
    cartCount:Int,
    onHome:()->Unit,
    onCatalog:()->Unit,
    onCart:()->Unit,
    onExit:()->Unit,
    onOrders:()->Unit={}
){
    val catalog=current in setOf(ProtoScreen.RetailCatalog,ProtoScreen.RetailFilter,ProtoScreen.RetailProductList,ProtoScreen.RetailProductDetail)
    val cart=current in setOf(ProtoScreen.RetailCart,ProtoScreen.RetailCheckout)
    val orders=current in setOf(ProtoScreen.RetailOrderList,ProtoScreen.RetailOrderDetail,ProtoScreen.RetailOrderSent)
    NavigationBar(containerColor=ProtoPanel,tonalElevation=0.dp,modifier=Modifier.height(66.dp)){
        ProtoNavItem(current==ProtoScreen.RetailHome,"Главная",Icons.Outlined.Home,onHome)
        ProtoNavItem(catalog,"Каталог",Icons.Outlined.Inventory2,onCatalog)
        NavigationBarItem(
            selected=cart,
            onClick=onCart,
            icon={
                BadgedBox(badge={if(cartCount>0)Badge(containerColor=ProtoGold){Text(cartCount.toString(),color=Color.Black)}}){
                    Icon(Icons.Outlined.ShoppingCart,null)
                }
            },
            label={Text("Корзина",fontSize=9.sp)},
            colors=NavigationBarItemDefaults.colors(
                selectedIconColor=ProtoGold,
                selectedTextColor=ProtoGold,
                indicatorColor=Color.Black,
                unselectedIconColor=ProtoMuted,
                unselectedTextColor=ProtoMuted
            )
        )
        ProtoNavItem(orders,"Заказы",Icons.Outlined.ReceiptLong,onOrders)
        ProtoNavItem(false,"Выйти",Icons.Outlined.ExitToApp,onExit)
    }
}

@Composable
private fun ProtoAdminBottomBar(current:ProtoScreen,onHome:()->Unit,onClients:()->Unit,onOrders:()->Unit,onStock:()->Unit,onProfile:()->Unit){NavigationBar(containerColor=ProtoPanel,tonalElevation=0.dp){ProtoNavItem(current==ProtoScreen.AdminHome,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(current in setOf(ProtoScreen.AdminClients,ProtoScreen.AdminClient),"Клиенты",Icons.Outlined.Groups,onClients);ProtoNavItem(current in setOf(ProtoScreen.AdminOrders,ProtoScreen.AdminOrderDetail),"Заказы",Icons.Outlined.ReceiptLong,onOrders);ProtoNavItem(current in setOf(ProtoScreen.Server,ProtoScreen.StockList,ProtoScreen.ReserveList),"Склад",Icons.Outlined.Inventory2,onStock);ProtoNavItem(current in setOf(ProtoScreen.AdminSettings,ProtoScreen.AdminSettingsDetail),"Профиль",Icons.Outlined.Person,onProfile)}
}

@Composable
private fun ProtoProductionBottomBar(onHome:()->Unit,onProduction:()->Unit,onHistory:()->Unit,onStock:()->Unit,onProfile:()->Unit){NavigationBar(containerColor=ProtoPanel,tonalElevation=0.dp){ProtoNavItem(false,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(true,"Производство",Icons.Outlined.LocalFlorist,onProduction);ProtoNavItem(false,"История",Icons.Outlined.History,onHistory);ProtoNavItem(false,"Склад",Icons.Outlined.Inventory2,onStock);ProtoNavItem(false,"Профиль",Icons.Outlined.Person,onProfile)}}

@Composable
private fun RowScope.ProtoNavItem(selected:Boolean,label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){NavigationBarItem(selected=selected,onClick=onClick,icon={Icon(icon,null)},label={Text(label,fontSize=9.sp)},colors=protoNavColors())}

@Composable
private fun protoNavColors()=NavigationBarItemDefaults.colors(selectedIconColor=Color.Black,selectedTextColor=ProtoGold,indicatorColor=ProtoGold,unselectedIconColor=ProtoMuted,unselectedTextColor=ProtoMuted)

@Composable
private fun ProtoProductImage(p:ProtoCatalogProduct,modifier:Modifier=Modifier,contentScale:ContentScale=ContentScale.Crop){
    val placeholder=painterResource(protoPlaceholderForType(p.type))
    AsyncImage(model=p.imageUrl.takeIf{it.isNotBlank()}?:protoPlaceholderForType(p.type),contentDescription=p.name,modifier=modifier,contentScale=contentScale,placeholder=placeholder,error=placeholder,fallback=placeholder)
}

private fun protoPlaceholderForType(type:String)=when(type){
    "Венки","Венки круглые","Корзины","Полянки","Флоретки"->R.drawable.mock_wreath
    "Ленты"->R.drawable.mock_ribbon
    "Гробы"->R.drawable.mock_coffin
    "Одежда"->R.drawable.mock_clothes
    "Кресты"->R.drawable.mock_cross
    "Цветы"->R.drawable.mock_flowers
    "Услуги"->R.drawable.mock_service
    else->R.drawable.mock_generic
}

private fun protoParseOrderDate(value:String):LocalDateTime? =
    runCatching { LocalDateTime.parse(value,DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) }.getOrNull()

private fun protoOrderMonthKey(value:String):String =
    protoParseOrderDate(value)?.let { YearMonth.from(it).toString() } ?: "0000-00"

private fun protoMonthLabel(key:String):String {
    val ym=runCatching{YearMonth.parse(key)}.getOrNull() ?: return key
    val month=ym.month.getDisplayName(java.time.format.TextStyle.FULL,ruLocale)
    return month.replaceFirstChar{if(it.isLowerCase())it.titlecase(ruLocale) else it.toString()}+" "+ym.year
}

private fun protoFilterOrdersByPeriod(orders:List<ProtoOrder>,period:String):List<ProtoOrder>{
    val cutoff=when(period){
        "30 дней" -> LocalDate.now().minusDays(30)
        "90 дней" -> LocalDate.now().minusDays(90)
        "Год" -> LocalDate.now().minusYears(1)
        else -> null
    }
    return if(cutoff==null)orders else orders.filter { order ->
        protoParseOrderDate(order.dateTime)?.toLocalDate()?.let{!it.isBefore(cutoff)} ?: true
    }
}

private fun protoChatTime(epoch:Long):String =
    Instant.ofEpochMilli(epoch).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))

private fun protoUriName(context:Context,uri:Uri):String {
    var name=uri.lastPathSegment ?: "Вложение"
    runCatching {
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { cursor ->
            val index=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if(cursor.moveToFirst() && index>=0) name=cursor.getString(index)
        }
    }
    return name
}

private suspend fun protoCopyChatImage(context:Context,uri:Uri):Triple<String,String,String>? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
        runCatching{
            val mime=context.contentResolver.getType(uri).orEmpty().ifBlank{"image/jpeg"}
            val ext=when{mime.contains("png")->"png";mime.contains("webp")->"webp";else->"jpg"}
            val dir=File(context.filesDir,"chat_attachments").apply{mkdirs()}
            val target=File(dir,"IMG-"+System.currentTimeMillis()+"."+ext)
            val input=context.contentResolver.openInputStream(uri) ?: return@runCatching null
            input.use{source->target.outputStream().use{out->source.copyTo(out)}}
            Triple(Uri.fromFile(target).toString(),protoUriName(context,uri),mime)
        }.getOrNull()
    }

private fun protoOpenAttachment(context:Context,uriValue:String,mime:String) {
    runCatching {
        val uri=Uri.parse(uriValue)
        val intent=Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri,mime.ifBlank{"*/*"})
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}

private fun protoLoadNotifications(prefs:android.content.SharedPreferences):List<ProtoNotification>{
    return runCatching {
        val raw=prefs.getString("client_notifications","[]").orEmpty()
        val array=JSONArray(raw)
        (0 until array.length()).map { i ->
            val o=array.getJSONObject(i)
            ProtoNotification(
                id=o.getString("id"),
                audienceRole=o.optString("audienceRole","CLIENT"),
                audienceKey=o.optString("audienceKey",""),
                title=o.optString("title","Уведомление"),
                message=o.optString("message",""),
                dateTime=o.optString("dateTime",""),
                read=o.optBoolean("read",false)
            )
        }
    }.getOrDefault(emptyList())
}

private fun protoSaveNotifications(prefs:android.content.SharedPreferences,items:List<ProtoNotification>){
    val array=JSONArray()
    items.forEach { n ->
        array.put(JSONObject().apply {
            put("id",n.id)
            put("audienceRole",n.audienceRole)
            put("audienceKey",n.audienceKey)
            put("title",n.title)
            put("message",n.message)
            put("dateTime",n.dateTime)
            put("read",n.read)
        })
    }
    prefs.edit().putString("client_notifications",array.toString()).apply()
}

private fun protoProductCategoryKey(type:String):String = when {
    type.contains("Вен", ignoreCase=true) -> "Венки"
    type.contains("Гроб", ignoreCase=true) -> "Гробы"
    type.contains("Крест", ignoreCase=true) -> "Кресты"
    type.contains("Лент", ignoreCase=true) -> "Ленты"
    type.contains("Одеж", ignoreCase=true) -> "Одежда"
    else -> type.ifBlank { "Прочее" }
}

private fun protoToggle(set:Set<String>,value:String)=if(value in set)set-value else set+value
private fun protoMoney(value:Int)=NumberFormat.getCurrencyInstance(ruLocale).format(value).replace(",00","")
private fun protoIconForType(type:String)=when(type){"Венки","Венки круглые"->Icons.Outlined.LocalFlorist;"Корзины"->Icons.Outlined.ShoppingBasket;"Полянки","Флоретки"->Icons.Outlined.Spa;"Ленты"->Icons.Outlined.ReceiptLong;else->Icons.Outlined.Inventory2}

private fun protoLoadProducts(context:Context):List<ProtoCatalogProduct>{return try{val root=JSONObject(context.assets.open("demo_data.json").bufferedReader().use{it.readText()});val arr=root.getJSONArray("products");(0 until arr.length()).map{i->val o=arr.getJSONObject(i);ProtoCatalogProduct(o.getString("sku"),o.getString("name"),o.getString("type"),o.optString("quality","—"),o.getString("size"),o.getInt("price"),o.getInt("stock"),o.getString("status"),o.optInt("productionDays",3),o.optString("imageUrl",""),o.optString("externalId",o.optString("sku","")))}}catch(_:Exception){emptyList()}}

private fun protoExportCsv(
    context: Context,
    report: String,
    products: List<ProtoCatalogProduct>,
    stockOverrides: SnapshotStateMap<String, Int>,
    orders: List<ProtoOrder>,
    clients: List<ProtoClient>,
    productionOps: List<ProtoProductionOp>,
    reservedForSku: (String) -> Int,
    toast: (String) -> Unit
) {
    val safeName = report.lowercase(ruLocale).replace(" ", "_").replace("/", "-")
    val fileName = "SANSARA_${safeName}_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
    val csv = buildString {
        when (report) {
            "Остатки на складе" -> {
                appendLine("SKU;Наименование;Физический остаток;Резерв;Доступно")
                products.forEach { p ->
                    val physical = stockOverrides[p.sku] ?: p.stock
                    val reserved = reservedForSku(p.sku)
                    appendLine("${p.sku};${p.name};$physical;$reserved;${(physical-reserved).coerceAtLeast(0)}")
                }
            }
            "Резерв по клиентам" -> {
                appendLine("Заказ;Клиент;SKU;Наименование;Количество;Статус")
                orders.filter { it.status in setOf("Получен","Подтверждён") }.forEach { o -> o.lines.forEach { l -> appendLine("${o.id};${o.clientName};${l.sku};${l.name};${l.qty};${o.status}") } }
            }
            "Выпуск за месяц" -> {
                appendLine("Дата;Время;SKU;Наименование;Количество;Сборщица;Ставка;Сумма;Документ;Оприходовал;Статус")
                productionOps.forEach { op -> appendLine("${op.date};${op.time};${op.sku};${op.name};${op.qty};${op.assembler};${op.rateRub};${op.amountRub};${op.documentId};${op.postedBy};${op.status}") }
            }
            "Производство по сборщицам" -> {
                appendLine("Сборщица;Количество;Сумма")
                productionOps.groupBy { it.assembler }.forEach { (assembler, ops) -> appendLine("$assembler;${ops.sumOf { it.qty }};${ops.sumOf { it.amountRub }}") }
            }
            "Заказы за месяц" -> {
                appendLine("Заказ;Клиент;Дата;Количество;Сумма;Статус")
                orders.forEach { o -> appendLine("${o.id};${o.clientName};${o.dateTime};${o.pieces};${o.total};${o.status}") }
            }
            else -> {
                appendLine("Клиент;Статус;Заказы;Оборот")
                clients.forEach { c -> appendLine("${c.name};${c.status};${c.orderCount};${c.monthTurnover}") }
            }
        }
    }
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SANSARA")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Не удалось создать файл")
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write("\uFEFF" + csv) } ?: error("Не удалось открыть файл")
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SANSARA").apply { mkdirs() }
            File(dir, fileName).writeText("\uFEFF" + csv, Charsets.UTF_8)
        }
    }.onSuccess { toast("Отчёт сохранён: Downloads/SANSARA/$fileName") }
        .onFailure { toast("Ошибка экспорта: ${it.message ?: "неизвестная ошибка"}") }
}

private fun protoDialNumber(context:Context,phone:String){val normalized=phone.filter{it.isDigit()||it=='+'};context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:$normalized")))}
private fun protoDial(context:Context){protoDialNumber(context,BuildConfig.ADMIN_PHONE)}
private fun protoMessage(context:Context){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${BuildConfig.ADMIN_PHONE}")))}
