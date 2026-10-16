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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
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

internal val ProtoBg = Bg
internal val ProtoPanel = Panel
internal val ProtoPanel2 = Panel2
internal val ProtoGold = Gold
internal val ProtoGoldSoft = GoldSoft
internal val ProtoText = Text
internal val ProtoMuted = Muted
internal val ProtoGreen = Green
internal val ProtoRed = Red
internal val ProtoBorder = Border
internal val ProtoOrange = Warning

internal enum class ProtoScreen {
    Welcome, Login, Registration, RegistrationSent,
    Home, Popular, Catalog, Filter, ProductList, ProductDetail, Cart, Checkout, OrderSent, OrderList, OrderDetail, Notifications, ClientChat, ClientReports, ClientSettings, AgentClients, AgentClientDetail, RetailHome, RetailCatalog, RetailFilter, RetailProductList, RetailProductDetail, RetailCart, RetailCheckout, RetailOrderSent, RetailOrderList, RetailOrderDetail, RetailNotifications, Profile, Suspended,
    AdminHome, AdminSearch, AdminClients, AdminClient, AdminOrders, AdminOrderDetail, AdminNotifications, AdminCatalog, AdminSettings, AdminSettingsDetail, AdminAttention, OnlineController, LowStockList, AdminChats, AdminChat, AdminProductionChat, AdminReports, AdminWorkshop, AdminAdmins, AdminAttendance, AdminProduct, AgentBase, AgentMarkup, AgentProductMarkup, AgentOrders,
    Production, ProductionCategory, ProductionCatalog, ProductionEntry, ProductionHistory, ProductionReport, ProductionPayments, ProductionProfile, ProductionWorkshop, ProductionAttendance, ProductionChat,
    Server, StockList, ReserveList, NewClients, Export, AdminAssemblers,
    SalesHome, SalesShipmentNew, SalesShipments, SalesClients, SalesClient, AdminPayments, AdminDashboard, SalesProfile,
    OwnStock
}

internal enum class ProtoClientType(val label: String) { AGENT("Агент"), TRADING("Торгующая организация") }

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
    val externalId: String = "",
    val retailPrice: Int = 0
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

internal data class ProtoProductionDraft(
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
    val discountPct: Int = 0,
    /** Цена для частного клиента агента (режим «Для клиентов»); 0 — обычный заказ. */
    val retailPrice: Int = 0,
    /** Сколько штук было в наличии на момент заказа (−1 — неизвестно, старые заказы). От этого зависит срок отмены. */
    val stockQty: Int = -1
) {
    val lineTotal: Int get() = protoLineTotal(price, qty, discountPct)
}

/** Единая формула суммы строки: цена × количество − скидка, округление вниз по строке. Ею считают карточка, корзина, оформление, заказ и отчёты. */
fun protoLineTotal(price: Int, qty: Int, discountPct: Int): Int = qty * protoUnitPrice(price, discountPct)

/** Цена за штуку со скидкой партнёра — её клиент видит в каталоге; сумма строки = цена × количество. */
fun protoUnitPrice(price: Int, discountPct: Int): Int = price * (100 - discountPct.coerceIn(0, 100)) / 100
data class ProtoOrderEvent(val status: String, val dateTime: String, val actor: String, val note: String = "")

/** Платёж по заказу, отмеченный администратором (позже — автоматически по банку). */
data class ProtoPayment(val amount: Int, val date: String, val method: String, val comment: String = "", val by: String = "")

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
    val deliveryTime: String = "",
    val clientId: String = "",
    val retailCustomerId: String = "",
    val retailCustomerName: String = "",
    val payments: List<ProtoPayment> = emptyList(),
    /** «Агент» или «Торгующая организация» на момент оформления. */
    val clientType: String = ""
) {
    val paid: Int get() = payments.sumOf { it.amount }
    val due: Int get() = (total - paid).coerceAtLeast(0)
    val paymentStatus: String get() = when {
        paid <= 0 -> "Не оплачен"
        paid < total -> "Оплачен частично"
        else -> "Оплачен полностью"
    }
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
    val read: Boolean = false,
    val orderId: String = ""
)

internal data class ProtoProductGroup(
    val key: String,
    val title: String,
    val imageType: String,
    val enabled: Boolean
)

/** Группы, которые сейчас продаются под плиткой «Венки». */
internal val protoWreathGroups = setOf("Венки","Корзины","Флоретки","Полянки")
internal val protoWreathGroupsOrdered = listOf("Венки","Корзины","Флоретки","Полянки")
/** Разделы, которые появятся после подключения каталогов Tilda. Пока в разделе нет товаров — плашка «В разработке»; появятся товары — плашка оживёт сама. */
/** Статусы, с которыми заказ уходит из «Мои заказы» во вкладку «История». */
internal val protoHistoryStatuses = setOf("Доставлен","Отменён","Отклонён")
internal val protoUpcomingGroups = listOf("Гробы","Кресты","Одежда","Ленты","Услуги")
internal val protoAllGroupsOrdered = protoWreathGroupsOrdered + protoUpcomingGroups

/** Статус товара, снятого с публикации в Tilda: скрыт из каталога, но остаётся в старых заказах и на складе. */
const val PRODUCT_UNPUBLISHED = "Снят с публикации"

internal val protoProductGroups = listOf(
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
    val status: String = "Новая",
    val lastName: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val rejectReason: String = "",
    /** Примерный общий объём продаж клиента в месяц, ₽ — из анкеты. */
    val monthlyVolume: Long = 0L
)

internal val ruLocale = Locale("ru", "RU")
internal fun currentDateLong(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale))
internal fun currentDateShort(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
internal fun currentTimeShort(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

internal fun currentMonthLabel(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() }

@Composable
fun SansaraVisualPrototype() {
    val context = LocalContext.current
    val repository = remember { SansaraRepository.get(context) }
    val authProvider: AuthProvider = remember { LocalAuthProvider(repository, SecureSessionStore(context)) }
    var session by remember { mutableStateOf<SansaraSession?>(null) }
    var dataReady by remember { mutableStateOf(false) }
    val products = remember { mutableStateListOf<ProtoCatalogProduct>() }
    // Что видит клиент: без товаров, снятых с публикации в Tilda.
    val catalogProducts by remember { derivedStateOf { products.filter { it.status != PRODUCT_UNPUBLISHED } } }
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
    var listSort by remember { mutableStateOf(protoSortOptions.first()) }
    var focusSearch by remember { mutableStateOf(false) }
    var orderSendState by remember { mutableStateOf("ok") }
    var loginLockedUntil by remember { mutableLongStateOf(0L) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var rejectionNotice by remember { mutableStateOf<String?>(null) }
    var adminSetupNeeded by remember { mutableStateOf(false) }
    var crashReport by remember { mutableStateOf(SansaraCrashLog.pending(context)) }
    var adminSetupError by remember { mutableStateOf<String?>(null) }
    var homeFilter by remember { mutableStateOf(protoLoadFilter(context.getSharedPreferences("sansara", Context.MODE_PRIVATE), "home_filter")) }
    var filterTarget by remember { mutableStateOf("catalog") }
    var approvedCodes by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) }
    val smsSent = remember { mutableStateListOf<String>() }
    var adminOrdersStatus by remember { mutableStateOf("Все") }
    var adminOrdersDate by remember { mutableStateOf("Все даты") }
    val adminClientHistory = remember { mutableStateListOf<ClientHistoryEntity>() }
    val adminCompanyUsers = remember { mutableStateListOf<Pair<SansaraCompanyContact, String>>() }
    var issuedAdminCode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var workshopPrefill by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var workshopPrefillComment by remember { mutableStateOf("") }
    var selectedAdminSku by remember { mutableStateOf("") }
    val agentSkuMarkups = remember { mutableStateMapOf<String, Int>() }
    var agentDiscountTick by remember { mutableIntStateOf(0) }
    var showAgentNewCustomer by remember { mutableStateOf(false) }
    val clientExpenses = remember { mutableStateListOf<ProtoClientExpense>() }
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
    val shipments = remember { mutableStateListOf<ShipmentEntity>() }
    val expenses = remember { mutableStateListOf<ExpenseEntity>() }
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
    var lowStockThreshold by remember { mutableIntStateOf(context.getSharedPreferences("sansara", Context.MODE_PRIVATE).getInt("low_stock_threshold", 5)) }
    var notificationsRegistration by remember { mutableStateOf(true) }
    var notificationsOrders by remember { mutableStateOf(true) }
    var notificationsProduction by remember { mutableStateOf(true) }
    var notificationsLowStock by remember { mutableStateOf(true) }
    var settingsSection by remember { mutableStateOf("Профиль компании") }
    var backupStatus by remember { mutableStateOf("Резервная копия ещё не создавалась") }
    var registrationsTotalThisMonth by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("sansara", Context.MODE_PRIVATE) }
    var welcomeImageVersion by remember { mutableStateOf(prefs.getLong("welcome_promo_image_v", 0L)) }
    var minOrderTrading by remember { mutableStateOf(prefs.getInt("min_order_trading", 35_000)) }
    /** Минимальная сумма заказа: только для торгующих организаций, у агентов минимума нет. */
    fun minOrderFor(c: ProtoClient): Int = if (c.clientType.contains("Торг", ignoreCase = true)) minOrderTrading else 0
    var belowMinPrompt by remember { mutableStateOf<Triple<Int, Int, () -> Unit>?>(null) }
    /** «Глаз бога»: настоящая сессия главного администратора, пока он смотрит из чужого аккаунта. */
    var godEyeAdmin by remember { mutableStateOf<SansaraSession?>(null) }
    var godEyePicker by remember { mutableStateOf<List<SansaraSession>?>(null) }
    var godEyeConfirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var adminDialog by remember { mutableStateOf<String?>(null) }
    /** Откуда отгружается заказ своему клиенту: «SANSARA» или «OWN» (мой склад). */
    var retailWarehouse by remember { mutableStateOf("SANSARA") }
    /** Нехватка на своём складе: (со своего склада, заказом в SANSARA, действие «разделить», действие «всё в SANSARA»). */
    var ownSplitPrompt by remember { mutableStateOf<Triple<String, () -> Unit, () -> Unit>?>(null) }
    var ownSaleDone by remember { mutableStateOf<String?>(null) }
    /** «Мой склад» текущего клиента. */
    val ownOps = remember { mutableStateListOf<OwnStockOpEntity>() }
    var ownExternalTick by remember { mutableStateOf(0) }
    /** Режим «склад ведётся в другой программе» хранится для каждой компании отдельно. */
    fun ownExternal(clientId: String): Boolean = ownExternalTick >= 0 && context.getSharedPreferences("sansara", Context.MODE_PRIVATE).getBoolean("own_external_$clientId", false)
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf(0) }
    /** В режиме «Глаз бога» перед любым изменением спрашиваем подтверждение. */
    fun guarded(what: String, action: () -> Unit) { if (godEyeAdmin != null) godEyeConfirm = what to action else action() }
    /** Кто сделал действие: в режиме «Глаз бога» помечаем, что это BOSS. */
    fun actor(default: String): String = if (godEyeAdmin != null) "BOSS (Глаз бога) за $default" else default
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
        SansaraRole.SALES -> ProtoScreen.SalesHome
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

    fun toast(message: String) { android.os.Handler(android.os.Looper.getMainLooper()).post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() } }
    fun persistNotifications() = protoSaveNotifications(prefs, notifications)
    fun addNotification(audienceRole:String,audienceKey:String,title:String,message:String,orderId:String="",system:Boolean=true) {
        notifications.add(
            0,
            ProtoNotification(
                id = "N-" + java.util.UUID.randomUUID().toString().replace("-","").take(12).uppercase(),
                audienceRole = audienceRole,
                audienceKey = audienceKey,
                title = title,
                message = message,
                dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                orderId = orderId
            )
        )
        persistNotifications()
        // Дублируем системным уведомлением телефона (всплывает и остаётся в шторке).
        if (system) SansaraNotify.post(context, title, message, orderId)
    }
    fun go(target: ProtoScreen) { if (target != screen) { history.add(screen); screen = target } }
    fun openTab(home: ProtoScreen, target: ProtoScreen) {
        history.clear()
        if (target != home) history.add(home)
        screen = target
    }

    fun back() {
        val active = session
        when {
            history.isNotEmpty() -> screen = history.removeAt(history.lastIndex)
            // Вошедший пользователь не попадает на «Добро пожаловать» кнопкой «назад»:
            // с любого экрана — на главный экран роли, с главного — выход из приложения.
            active != null && screen != startScreenForRole(active.role) -> screen = startScreenForRole(active.role)
            active != null -> (context as? android.app.Activity)?.finish()
            else -> screen = ProtoScreen.Welcome
        }
    }
    fun onSignedIn() {
        SansaraAdminOpsReminderScheduler.scheduleAll(context)
        if (session?.role == SansaraRole.ADMIN) scope.launch { runCatching { SansaraNotify.birthdayCheck(context) } }
        // Разрешение на уведомления — для всех ролей: оповещения дублируются на экран телефона.
        (context as? MainActivity)?.requestNotificationPermission()
    }
    fun signOut() {
        // В режиме «Глаз бога» «Выйти» возвращает в аккаунт BOSS, а не завершает его сессию.
        godEyeAdmin?.let { boss ->
            session = boss; godEyeAdmin = null; companyContacts.clear()
            history.clear(); screen = startScreenForRole(boss.role)
            return
        }
        authProvider.signOut()
        session = null
        SansaraAdminOpsReminderScheduler.cancelAll(context)
        history.clear()
        screen = ProtoScreen.Welcome
    }
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
    fun nowStamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    /** Изменить заказ по номеру, сохранить и вернуть новую версию (null — заказ не найден). */
    fun updateOrder(id: String?, transform: (ProtoOrder) -> ProtoOrder): ProtoOrder? {
        val idx = orders.indexOfFirst { it.id == id }
        if (idx < 0) return null
        val updated = transform(orders[idx])
        orders[idx] = updated
        persistAll()
        return updated
    }
    fun adminActorName(): String = session?.firstName?.ifBlank { null } ?: "Администратор"
    /** Тип клиента заказа: сохранённый при оформлении, иначе — из карточки клиента. */
    fun orderClientType(o: ProtoOrder): String = o.clientType.ifBlank { clients.firstOrNull { it.id == o.clientId || it.name == o.clientName }?.clientType.orEmpty() }
    fun orderingBlocked(): Boolean { val c = currentClient(); return c.status == "Приостановлен" || !c.orderingEnabled }
    fun setCartQty(p: ProtoCatalogProduct, q: Int) {
        if (orderingBlocked()) { toast("Оформление заказов временно приостановлено"); go(ProtoScreen.Suspended); return }
        if (q <= 0) cart.remove(p.sku) else cart[p.sku] = q
        persistAll()
    }
    fun currentFilter() = ProtoCatalogFilter(searchQuery, selectedTypes, selectedQualities, selectedSizes, selectedAvailability, minPriceFilter, maxPriceFilter)
    fun applyFilter(f: ProtoCatalogFilter) {
        searchQuery = f.query; selectedTypes = f.types; selectedQualities = f.qualities; selectedSizes = f.sizes
        selectedAvailability = f.availability; minPriceFilter = f.minPrice; maxPriceFilter = f.maxPrice
    }
    fun activeFilterChips(): List<Pair<String, () -> Unit>> {
        val chips = mutableListOf<Pair<String, () -> Unit>>()
        if (selectedTypes.isNotEmpty() && selectedTypes != protoWreathGroups) selectedTypes.forEach { t -> chips.add(Pair(t) { selectedTypes = selectedTypes - t }) }
        selectedAvailability.forEach { a -> chips.add(Pair(a) { selectedAvailability = selectedAvailability - a }) }
        selectedQualities.forEach { q -> chips.add(Pair(q) { selectedQualities = selectedQualities - q }) }
        selectedSizes.forEach { z -> chips.add(Pair(z) { selectedSizes = selectedSizes - z }) }
        minPriceFilter?.let { v -> chips.add(Pair("от " + protoMoney(v)) { minPriceFilter = null }) }
        maxPriceFilter?.let { v -> chips.add(Pair("до " + protoMoney(v)) { maxPriceFilter = null }) }
        return chips
    }
    fun openProductList(query: String = "", type: String? = null, availability: String? = null, focus: Boolean = false, target: ProtoScreen = ProtoScreen.ProductList) {
        resetFilters(type = type, availability = availability)
        searchQuery = query
        focusSearch = focus
        listSort = protoSortOptions.first()
        productListIndex = 0; productListOffset = 0
        go(target)
    }
    /** Популярность: продано единиц по всей SANSARA за 30 дней — только завершённые (доставленные) заказы. Возвратов в системе пока нет. */
    fun popularity(): Map<String, Int> {
        val since = System.currentTimeMillis() - 30L * 24 * 60 * 60_000L
        return orders.filter { it.status == "Доставлен" && protoOrderEpoch(it.dateTime) >= since }
            .flatMap { it.lines }.groupBy { it.sku }.mapValues { (_, l) -> l.sumOf { it.qty } }
    }
    fun ordersOf(c: ProtoClient): List<ProtoOrder> =
        orders.filter { (it.clientId == c.id || (it.clientId.isBlank() && it.clientName == c.name)) && it.retailCustomerId.isBlank() }
    fun isClientNotification(n: ProtoNotification, c: ProtoClient): Boolean =
        n.audienceRole == "CLIENT" && (n.audienceKey == c.id || n.audienceKey == c.name)
    /** Ближайшая дата получения: всё в наличии — следующий рабочий день; иначе — самый долгий срок изготовления, только рабочие дни. */
    fun earliestDeliveryDate(shortItems: List<ProtoCatalogProduct>): LocalDate =
        protoAddWorkdays(LocalDate.now(), (shortItems.maxOfOrNull { it.productionDays } ?: 1).coerceAtLeast(1))
    fun pickupPoints(): List<String> = prefs.getString("pickup_points", "").orEmpty().lines().map { it.trim() }.filter { it.isNotBlank() }
    /** Все заказы клиента, включая заказы его частных покупателей (режим «Для клиентов»). */
    fun ordersOfClientAll(c: ProtoClient): List<ProtoOrder> =
        orders.filter { o -> o.clientId == c.id || (o.clientId.isBlank() && o.clientName.substringBefore(" / ") == c.name) }
    fun clientStats(): Map<String, ProtoClientStats> {
        val month = currentDateShort().substringAfter('.')
        return clients.associate { c ->
            val list = ordersOfClientAll(c).filter { it.status != "Отменён" }
            val turnover = list.filter { protoMonthKey(it.dateTime.substringBefore(" ")) == month }.sumOf { it.total }
            val last = list.maxByOrNull { protoOrderEpoch(it.dateTime) }?.dateTime.orEmpty()
            val debt = shipments.filter { it.clientId == c.id && it.moneyStatus != "CONFIRMED" }.sumOf { it.amount }
            c.id to ProtoClientStats(turnover, list.size, last, debt)
        }
    }
    fun loadClientDetails(clientId: String) {
        scope.launch {
            val h = repository.clientHistory(clientId)
            val u = repository.companyUsers(clientId)
            adminClientHistory.clear(); adminClientHistory.addAll(h.sortedByDescending { it.createdAt })
            adminCompanyUsers.clear(); adminCompanyUsers.addAll(u)
        }
    }
    fun openAdminClient(clientId: String) {
        selectedClientId = clientId
        adminClientHistory.clear(); adminCompanyUsers.clear()
        loadClientDetails(clientId)
        scope.launch { shipments.clear(); shipments.addAll(repository.shipments()) }
        go(ProtoScreen.AdminClient)
    }
    fun updateClient(clientId: String, historyText: String, transform: (ProtoClient) -> ProtoClient) {
        val i = clients.indexOfFirst { it.id == clientId }
        if (i < 0) return
        clients[i] = transform(clients[i])
        persistAll()
        scope.launch {
            repository.addClientHistory(clientId, historyText, session?.firstName?.ifBlank { null } ?: "Администратор")
            loadClientDetails(clientId)
        }
    }
    fun adjustStock(p: ProtoCatalogProduct, delta: Int, reason: String) {
        scope.launch {
            runCatching { repository.adjustStock(p.sku, delta, reason, session?.userId ?: "U-ADMIN") }
                .onSuccess { applySnapshot(repository.snapshot()); toast("Остаток скорректирован: " + (if (delta > 0) "+" else "") + delta + " шт.") }
                .onFailure { toast("Ошибка: " + (it.message ?: "неизвестная ошибка")) }
        }
    }
    fun repeatOrder(order: ProtoOrder, edit: Boolean) {
        if (orderingBlocked()) { go(ProtoScreen.Suspended); return }
        // Корзину не очищаем молча: позиции заказа добавляются к тому, что уже лежит в корзине.
        val hadItems = cart.isNotEmpty()
        var missing = 0
        order.lines.forEach { line ->
            if (catalogProducts.any { it.sku == line.sku }) cart[line.sku] = (cart[line.sku] ?: 0) + line.qty else missing += 1
        }
        persistAll()
        toast(listOfNotNull(
            if (hadItems) "Позиции заказа добавлены к товарам в корзине" else "Заказ " + order.id + " перенесён в корзину",
            if (missing > 0) "не добавлено $missing — сняты с продажи" else null
        ).joinToString(" · "))
        go(if (edit || hadItems || cart.isEmpty()) ProtoScreen.Cart else ProtoScreen.Checkout)
    }
    fun saveDrafts() = protoSaveDrafts(prefs, productionDrafts)
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
                saveDrafts()
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
                val feedSkus = result.items.map { it.sku.lowercase() }.toSet()
                val publishedBefore = products.count { it.status != PRODUCT_UNPUBLISHED }
                result.items.forEach { item ->
                    val idx = products.indexOfFirst { it.sku.equals(item.sku, true) }
                    if (idx >= 0) {
                        val old = products[idx]
                        products[idx] = old.copy(
                            name = item.name, type = item.category, quality = item.quality, size = item.size,
                            price = if (item.price > 0) item.price else old.price, productionDays = item.productionDays,
                            imageUrl = item.imageUrl.ifBlank { old.imageUrl }, externalId = item.externalId,
                            retailPrice = item.retailPrice,
                            status = if (old.status == PRODUCT_UNPUBLISHED) "Под заказ" else old.status
                        )
                    } else {
                        // Новый артикул: остаток 0 / «Под заказ» до первого прихода. В debug-сборке — демо-остаток, чтобы экраны были живыми.
                        val demoStock = if (BuildConfig.DEBUG) (item.sku.hashCode().let { kotlin.math.abs(it) } % 5).let { if (it == 0) 0 else 4 + it * 6 } else 0
                        products.add(ProtoCatalogProduct(item.sku,item.name,item.category,item.quality,item.size,item.price,demoStock,if (demoStock > 0) "В наличии" else "Под заказ",item.productionDays,item.imageUrl,item.externalId,item.retailPrice))
                    }
                }
                // Демо-позиции старых сборок (V-060-001, KOR-070-004 …) заменяются каталогом Tilda, когда фид полный.
                if (result.items.size >= 100) {
                    val demoSku = Regex("^(V|VK|KOR|POL|FL|L)-\\d{3}(-\\d{3})?$")
                    products.removeAll { demoSku.matches(it.sku) && it.sku.lowercase() !in feedSkus }
                }
                // Снятые с публикации в Tilda товары скрываем из каталога (в заказах и на складе они остаются).
                // Защита от неполного ответа: скрываем, только если фид не меньше половины прежнего каталога.
                if (result.items.isNotEmpty() && result.items.size * 2 >= publishedBefore) {
                    products.indices.forEach { i ->
                        val p = products[i]
                        if (p.sku.lowercase() !in feedSkus && p.status != PRODUCT_UNPUBLISHED) products[i] = p.copy(status = PRODUCT_UNPUBLISHED)
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

    fun refreshShipments() {
        scope.launch { shipments.clear(); shipments.addAll(repository.shipments()) }
    }

    fun refreshExpenses() {
        scope.launch { expenses.clear(); expenses.addAll(repository.expenses()) }
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
        agentSettings.firstOrNull { it.ownerClientId==ownerId } ?: SansaraAgentSettings(ownerId,false,0)

    // Порядок: индивидуальная наценка клиента по группе → наценка агента на позицию → общая наценка.
    fun markupForProduct(ownerId:String,customerId:String,product:ProtoCatalogProduct):Int {
        val category=protoProductCategoryKey(product.type)
        val individual=agentMarkups.firstOrNull { it.customerId==customerId && it.category==category }
        if(individual!=null && individual.markupPct>0)return individual.markupPct
        agentSkuMarkups[product.sku]?.let { return it }
        val general=ownerAgentSettings(ownerId)
        return if(general.generalMarkupEnabled)general.generalMarkupPct else 0
    }

    fun retailProducts(ownerId:String,customerId:String):List<ProtoCatalogProduct> {
        val discount = if (customerId.isBlank()) 0 else (agentDiscountTick.let { protoCustomerDiscount(prefs, customerId) })
        return catalogProducts.map { product ->
            val markup=markupForProduct(ownerId,customerId,product)
            val withMarkup=product.price*(1.0+markup/100.0)
            product.copy(price=(withMarkup*(100-discount)/100.0).roundToInt())
        }
    }

    /** «Новый» клиент агента — пока у него нет ни одной завершённой (доставленной) продажи. */
    fun isNewAgentCustomer(c: SansaraAgentCustomer): Boolean = orders.none { it.retailCustomerId == c.id && it.status == "Доставлен" }

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
        orderSendState = if (backendApiUrl.isBlank()) "offline" else "sending"
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
            orderSendState = if (result.ok) "ok" else "failed"
            backendStatus = if (result.ok) "Заказ передан backend"+if(telegramEnabled)" / Telegram" else "" else result.message
            telegramLastLog = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))+" · "+order.id+" · "+
                if(result.ok)"успешно · попыток: "+usedAttempts else result.message+" · попыток: "+usedAttempts
            prefs.edit().putString("telegram_last_log",telegramLastLog).apply()
        }
    }

    LaunchedEffect(Unit) {
        repository.seedDebugIfNeeded()
        repository.ensureSalesAccount()
        repository.ensureMainAdmin()
        adminSetupNeeded = repository.adminMissing()
        shipments.clear(); shipments.addAll(repository.shipments())
        expenses.clear(); expenses.addAll(repository.expenses())
        applySnapshot(repository.snapshot())
        productionDrafts.clear(); productionDrafts.addAll(protoLoadDrafts(prefs, products))
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
            // Авторизация сохраняется, но каждый запуск начинается с приветственного экрана;
            // «Войти» у вошедшего пользователя сразу открывает его главный экран.
            screen = ProtoScreen.Welcome
            onSignedIn()
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
                    onSignedIn()
                }
            }
            runCatching { ProtoScreen.valueOf(dbgScreen) }.getOrNull()?.let { target ->
                selectedProduct = products.firstOrNull()
                retailSelectedProduct = products.firstOrNull()
                productionProduct = products.firstOrNull()
                if (target == ProtoScreen.ProductDetail || target == ProtoScreen.RetailProductDetail) detailQty = 1
                dbgIntent?.getStringExtra("debug_types")?.takeIf { it.isNotBlank() }?.let { selectedTypes = it.split(",").toSet() }
                screen = target
            }
        }
        dataReady = true
        prefs.getString("my_registration_id", null)?.let { myId ->
            if (registrations.any { it.id == myId && it.status == "Отклонена" } && !prefs.getBoolean("reject_seen_$myId", false)) rejectionNotice = myId
        }
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
        // В режиме «Глаз бога» клиент не должен выглядеть «в сети».
        if (godEyeAdmin != null) return@LaunchedEffect
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

    LaunchedEffect(Unit) { loginLockedUntil = prefs.getLong("login_locked_until", 0L) }
    LaunchedEffect(selectedClientId) {
        agentSkuMarkups.clear(); agentSkuMarkups.putAll(protoLoadSkuMarkups(prefs, selectedClientId))
        clientExpenses.clear(); clientExpenses.addAll(protoLoadClientExpenses(prefs, selectedClientId))
    }
    LaunchedEffect(loginLockedUntil) {
        val wait = loginLockedUntil - System.currentTimeMillis()
        if (wait > 0) { delay(wait); loginLockedUntil = 0L }
    }

    if (!dataReady) {
        SansaraTheme { ProtoLoadingState() }
        return
    }

    @Composable fun protoGroupA() {
        val client = currentClient()
        val goHome: () -> Unit = { history.clear(); screen = ProtoScreen.Home }
        val goCatalog: () -> Unit = { go(ProtoScreen.Catalog) }
        val goCart: () -> Unit = { go(ProtoScreen.Cart) }
        val goOrders: () -> Unit = { go(ProtoScreen.OrderList) }
        val goChat: () -> Unit = { go(ProtoScreen.ClientChat) }
        val myOrders = ordersOf(client)
        when (screen) {
            ProtoScreen.Home -> ProtoClientHomeScreen(
                client = client,
                products = catalogProducts,
                popularity = popularity(),
                availableStock = { availableStock(it) },
                cart = cart,
                lastOrder = myOrders.filter { it.status != "Отменён" }.maxByOrNull { protoOrderEpoch(it.dateTime) },
                unreadCount = notifications.count { isClientNotification(it, client) && !it.read },
                onNotifications = { go(ProtoScreen.Notifications) },
                onSearch = { q -> openProductList(query = q, focus = q.isBlank()) },
                onFilter = { filterTarget = "home"; go(ProtoScreen.Filter) },
                greetingName = session?.firstName?.takeIf { it.isNotBlank() && session?.clientId == client.id } ?: client.firstName,
                discount = client.discount,
                homeFilter = homeFilter,
                onPopular = { go(ProtoScreen.Popular) },
                onGroup = { g -> openProductList(type = g) },
                onOpenProduct = { openProduct(it) },
                onSetQty = { p, q -> setCartQty(p, q) },
                onSeeAll = { mode -> openProductList(availability = mode.takeIf { it != "Все" }) },
                onOpenOrder = { selectedOrderId = it.id; go(ProtoScreen.OrderDetail) },
                onHome = goHome, onCatalog = goCatalog, onCart = goCart, onOrders = goOrders, onProfile = goChat,
                onMenu = { showClientMenu = true }
            )
            ProtoScreen.Catalog -> ProtoCatalogHomeScreen(
                cartCount = cart.size,
                groupCounts = protoAllGroupsOrdered.associateWith { g -> catalogProducts.count { it.type == g } }.filterValues { it > 0 },
                onBack = { back() },
                onSearch = { value -> openProductList(query = value, focus = value.isBlank()) },
                onGroup = { g -> openProductList(type = g) },
                onAvailability = { st -> openProductList(availability = st.takeIf { it != "Все" }) },
                onHome = goHome, onCart = goCart, onOrders = goOrders, onProfile = goChat
            )
            ProtoScreen.Filter -> if (filterTarget == "home") ProtoFilterScreen(
                products = catalogProducts.filter { it.type in protoWreathGroups },
                filter = homeFilter,
                availableStock = { availableStock(it) },
                onApply = { f ->
                    // Фильтр главной независим от каталога и сохраняется между запусками.
                    homeFilter = f.copy(query = "")
                    protoSaveFilter(prefs, "home_filter", homeFilter)
                    filterTarget = "catalog"
                    back()
                },
                onBack = { filterTarget = "catalog"; back() }
            ) else ProtoFilterScreen(
                products = catalogProducts.filter { it.type in protoWreathGroups },
                filter = currentFilter(),
                availableStock = { availableStock(it) },
                onApply = { f ->
                    applyFilter(f)
                    productListIndex = 0; productListOffset = 0
                    if (history.lastOrNull() == ProtoScreen.ProductList) back() else go(ProtoScreen.ProductList)
                },
                onBack = { back() }
            )
            ProtoScreen.ProductList -> {
                val f = currentFilter()
                ProtoProductListScreen(
                    products = catalogProducts.filter { protoMatches(it, f, availableStock(it)) },
                    cart = cart,
                    availableStock = { availableStock(it) },
                    query = searchQuery,
                    onQuery = { searchQuery = it; productListIndex = 0; productListOffset = 0 },
                    activeChips = activeFilterChips(),
                    sort = listSort,
                    onSort = { listSort = it; productListIndex = 0; productListOffset = 0 },
                    initialIndex = productListIndex,
                    initialOffset = productListOffset,
                    focusSearch = focusSearch,
                    onBack = { focusSearch = false; back() },
                    onOpenFilter = { focusSearch = false; go(ProtoScreen.Filter) },
                    onOpenProduct = { product, index, offset ->
                        productListIndex = index
                        productListOffset = offset
                        focusSearch = false
                        openProduct(product)
                    },
                    onSetQty = { p, q -> setCartQty(p, q) },
                    onHome = goHome,
                    onCatalog = { history.clear(); history.add(ProtoScreen.Home); screen = ProtoScreen.Catalog },
                    onCart = goCart, onOrders = goOrders, onProfile = goChat,
                    discount = client.discount
                )
            }
            ProtoScreen.Popular -> ProtoPopularScreen(
                products = catalogProducts,
                soldUnits = popularity(),
                cart = cart,
                availableStock = { availableStock(it) },
                discount = client.discount,
                onBack = { back() },
                onOpenProduct = { openProduct(it) },
                onSetQty = { p, q -> setCartQty(p, q) },
                onHome = goHome, onCatalog = goCatalog, onCart = goCart, onOrders = goOrders, onProfile = goChat
            )
            ProtoScreen.ProductDetail -> {
                val p = selectedProduct
                ProtoProductDetailScreen(
                    product = p,
                    currentStock = p?.let { availableStock(it) } ?: 0,
                    qty = detailQty,
                    discount = client.discount,
                    inCartQty = p?.let { cart[it.sku] } ?: 0,
                    editingCart = p != null && cartEditingSku == p.sku,
                    onBack = { cartEditingSku = null; back() },
                    onMinus = { detailQty = (detailQty - 1).coerceAtLeast(1) },
                    onPlus = { detailQty += 1 },
                    onAdd = {
                        if (orderingBlocked()) {
                            toast("Оформление заказов временно приостановлено")
                            go(ProtoScreen.Suspended)
                        } else p?.let { prod ->
                            if (cartEditingSku == prod.sku) {
                                cart[prod.sku] = detailQty
                                persistAll()
                                toast("Количество в корзине: ${detailQty} шт.")
                                cartEditingSku = null
                                back()
                            } else {
                                cart[prod.sku] = (cart[prod.sku] ?: 0) + detailQty
                                persistAll()
                                toast("Добавлено в корзину: ${detailQty} шт.")
                                detailQty = 1
                            }
                        }
                    },
                    onGoCart = { cartEditingSku = null; go(ProtoScreen.Cart) },
                    onHome = goHome, onCatalog = goCatalog, onCart = goCart, onOrders = goOrders, onProfile = goChat
                )
            }
            ProtoScreen.Cart -> ProtoCartScreen(
                products = products,
                cart = cart,
                discount = client.discount,
                availableStock = { availableStock(it) },
                onBack = { back() },
                onPlus = { p -> cart[p.sku] = (cart[p.sku] ?: 0) + 1; persistAll() },
                onMinus = { p -> val n = (cart[p.sku] ?: 1) - 1; if (n <= 0) cart.remove(p.sku) else cart[p.sku] = n; persistAll() },
                onDelete = { cart.remove(it.sku); persistAll() },
                onRestore = { p, q -> cart[p.sku] = q; persistAll() },
                onOpenProduct = { openProduct(it, fromCart = true) },
                onCheckout = { if (orderingBlocked()) go(ProtoScreen.Suspended) else go(ProtoScreen.Checkout) },
                onHome = goHome, onCatalog = goCatalog, onOrders = goOrders, onProfile = goChat,
                minOrder = minOrderFor(client)
            )
            ProtoScreen.Checkout -> {
                val cartLines = cart.mapNotNull { (sku, q) -> products.firstOrNull { it.sku == sku }?.let { it to q } }
                val baseTotal = cartLines.sumOf { (p, q) -> p.price * q }
                val finalTotal = cartLines.sumOf { (p, q) -> protoLineTotal(p.price, q, client.discount) }
                val shortages = cartLines.mapNotNull { (p, q) -> val a = availableStock(p); if (q > a) p to (q - a) else null }
                ProtoCheckoutScreen(
                    cartPositions = cartLines.size,
                    totalPieces = cartLines.sumOf { it.second },
                    baseTotal = baseTotal,
                    finalTotal = finalTotal,
                    discount = client.discount,
                    defaultAddress = client.address,
                    contacts = companyContacts.ifEmpty {
                        listOf(SansaraCompanyContact("fallback", client.contact, client.phone, client.email))
                    },
                    clientStatus = client.status,
                    earliestDate = earliestDeliveryDate(shortages.map { it.first }),
                    shortages = shortages.map { (p, n) -> protoTitle(p) + ": " + n + " шт. под заказ · " + protoReadyLabel(p.productionDays) },
                    pickupPoints = pickupPoints(),
                    cancelKind = protoOrderKind(cartLines.map { (p, q) -> ProtoOrderLine(p.sku, p.name, q, p.price, stockQty = availableStock(p).coerceAtMost(q)) }),
                    onBack = { back() },
                    onSubmit = { method, address, comment, recipient, contactPhone, deliveryDate, deliveryTime ->
                        if (orderingBlocked()) {
                            go(ProtoScreen.Suspended)
                        } else {
                            // Заказ торгующей организации меньше минимальной суммы уходит на согласование администратору.
                            fun place(needsApproval: Boolean) = scope.launch {
                                val lines = cartLines.map { (p, q) -> ProtoOrderLine(p.sku, p.name, q, p.price, client.discount, stockQty = availableStock(p).coerceAtMost(q)) }
                                val id = repository.nextOrderId()
                                val now = nowStamp()
                                val firstStatus = if (needsApproval) "На согласовании" else "Получен"
                                val newOrder = ProtoOrder(
                                    id = id,
                                    clientName = client.name,
                                    dateTime = now,
                                    lines = lines,
                                    status = firstStatus,
                                    history = listOf(ProtoOrderEvent(firstStatus, now, actor(client.contact), if (needsApproval) "Сумма ниже минимальной (" + protoMoney(minOrderFor(client)) + ")" else "")),
                                    clientType = client.clientType,
                                    deliveryMethod = method,
                                    deliveryAddress = address,
                                    comment = comment,
                                    recipient = recipient,
                                    contactPhone = contactPhone,
                                    deliveryDate = deliveryDate,
                                    deliveryTime = deliveryTime,
                                    clientId = client.id
                                )
                                orders.add(0, newOrder)
                                selectedOrderId = id
                                cart.clear()
                                if (needsApproval) addNotification("ADMIN", "ADMIN", "Согласовать заказ " + id, client.name + " · " + protoMoney(newOrder.total) + " — ниже минимальной суммы " + protoMoney(minOrderFor(client)), orderId = id)
                                else addNotification("ADMIN", "ADMIN", "Новый заказ " + id, client.name + " · " + protoMoney(newOrder.total) + " · " + deliveryDate, orderId = id)
                                persistAll()
                                sendOrderEvent(newOrder)
                                (context as? MainActivity)?.requestNotificationPermission()
                                history.clear(); history.add(ProtoScreen.Home)
                                screen = ProtoScreen.OrderSent
                            }
                            val minimum = minOrderFor(client)
                            guarded("оформить заказ на " + protoMoney(finalTotal)) {
                                if (minimum > 0 && finalTotal < minimum) belowMinPrompt = Triple(finalTotal, minimum) { place(true) }
                                else place(false)
                            }
                        }
                    },
                    onHome = goHome, onCatalog = goCatalog, onCart = goCart, onOrders = goOrders, onProfile = goChat
                )
            }
            ProtoScreen.OrderSent -> ProtoOrderSentScreen(
                order = orders.firstOrNull { it.id == selectedOrderId },
                sendState = orderSendState,
                onView = { go(ProtoScreen.OrderDetail) },
                onCall = { protoDial(context) },
                onHome = goHome,
                onCatalog = { history.clear(); history.add(ProtoScreen.Home); screen = ProtoScreen.Catalog },
                onCart = goCart, onOrders = goOrders, onProfile = goChat
            )
            ProtoScreen.OrderList -> ProtoOrderListScreen(
                orders = myOrders.sortedByDescending { protoOrderEpoch(it.dateTime) },
                products = products,
                onBack = { back() },
                onOpen = { selectedOrderId = it.id; go(ProtoScreen.OrderDetail) },
                onHome = goHome, onCatalog = goCatalog, onCart = goCart, onProfile = goChat,
                cartCount = cart.size,
                clientOrders = orders.filter { it.clientId == client.id && it.retailCustomerId.isNotBlank() }.takeIf { it.isNotEmpty() || client.clientType.contains("Агент", true) },
                onOpenClientOrder = { o -> selectedAgentCustomerId = o.retailCustomerId; selectedOrderId = o.id; go(ProtoScreen.RetailOrderDetail) }
            )
            ProtoScreen.OrderDetail -> ProtoOrderDetailScreen(
                order = orders.firstOrNull { it.id == selectedOrderId },
                products = products,
                isAdmin = false,
                onBack = { back() },
                onStatus = {},
                onRepeat = { orders.firstOrNull { it.id == selectedOrderId }?.let { repeatOrder(it, edit = false) } },
                onEditRepeat = { orders.firstOrNull { it.id == selectedOrderId }?.let { repeatOrder(it, edit = true) } },
                onCancel = { guarded("отменить заказ") {
                    val idx = orders.indexOfFirst { it.id == selectedOrderId }
                    if (idx >= 0 && protoCancelState(orders[idx]).allowed) {
                        val old = orders[idx]
                        orders[idx] = old.copy(status = "Отменён", history = old.history + ProtoOrderEvent("Отменён", nowStamp(), actor(client.contact)))
                        addNotification("ADMIN", "ADMIN", "Заказ " + old.id + " отменён клиентом", client.name + " · " + protoMoney(old.total), orderId = old.id)
                        persistAll()
                        toast("Заказ " + old.id + " отменён")
                    }
                } },
                cartCount = cart.size,
                onChat = goChat
            )
            else -> Unit
        }
    }

    @Composable fun protoGroupB() {
        when (screen) {
            ProtoScreen.Notifications -> {
                val c = currentClient()
                ProtoNotificationsScreen(
                    notifications = notifications.filter { isClientNotification(it, c) },
                    onBack = { back() },
                    onReadAll = {
                        var changed = false
                        notifications.indices.forEach { index ->
                            val n = notifications[index]
                            if (isClientNotification(n, c) && !n.read) { notifications[index] = n.copy(read = true); changed = true }
                        }
                        if (changed) persistNotifications()
                    },
                    onDelete = { id -> notifications.removeAll { it.id == id }; persistNotifications() },
                    onOpen = { n ->
                        val i = notifications.indexOfFirst { it.id == n.id }
                        if (i >= 0 && !n.read) { notifications[i] = n.copy(read = true); persistNotifications() }
                        if (n.orderId.isNotBlank() && orders.any { it.id == n.orderId }) { selectedOrderId = n.orderId; go(ProtoScreen.OrderDetail) }
                    }
                )
            }
            ProtoScreen.ClientChat -> {
                val c = currentClient()
                val conversationId = clientConversationId(c.id)
                ProtoChatScreen(
                    title = "Чат с менеджером",
                    subtitle = c.name,
                    messages = chatMessages.filter { it.conversationId == conversationId },
                    currentRole = "CLIENT",
                    onBack = { back() },
                    onRead = { markChatRead(conversationId,"CLIENT") },
                    onSend = { body, uri, name, mime ->
                        guarded("отправить сообщение от имени клиента") { sendChat(conversationId,"CLIENT",session?.userId ?: c.id,body,uri,name,mime) }
                    },
                    banner = "Менеджер отвечает в рабочие дни с 9:00 до 18:00. Срочно — позвоните.",
                    onCall = { protoDial(context) },
                    bottomBar = {
                        ProtoClientBottomBar(ProtoScreen.ClientChat, cart.size,
                            onHome = { history.clear(); screen = ProtoScreen.Home },
                            onCatalog = { go(ProtoScreen.Catalog) }, onCart = { go(ProtoScreen.Cart) },
                            onOrders = { go(ProtoScreen.OrderList) }, onProfile = {})
                    }
                )
            }
            ProtoScreen.ClientReports -> {
                val c = currentClient()
                val retail = orders.filter { it.clientId == c.id && it.retailCustomerId.isNotBlank() }
                ProtoClientReportsScreen(
                    isAgent = c.clientType.contains("Агент", true) || retail.isNotEmpty(),
                    orders = ordersOf(c),
                    retailOrders = retail,
                    shipments = shipments.filter { it.clientId == c.id },
                    expenses = clientExpenses,
                    typeOf = { sku -> products.firstOrNull { it.sku == sku }?.type ?: protoGuessType(orders.flatMap { o -> o.lines }.firstOrNull { it.sku == sku }?.name.orEmpty()) },
                    onBack = { back() },
                    onOpenOrder = { o ->
                        selectedOrderId = o.id
                        if (o.retailCustomerId.isNotBlank()) { selectedAgentCustomerId = o.retailCustomerId; go(ProtoScreen.RetailOrderDetail) } else go(ProtoScreen.OrderDetail)
                    },
                    onOpenProduct = { sku -> catalogProducts.firstOrNull { it.sku == sku }?.let { openProduct(it) } ?: toast("Позиция снята с продажи") },
                    onAddExpense = { cat, amount, comment ->
                        clientExpenses.add(0, ProtoClientExpense("E-" + System.currentTimeMillis(), currentDateShort(), cat, amount, comment))
                        protoSaveClientExpenses(prefs, c.id, clientExpenses.toList())
                    },
                    onDeleteExpense = { e -> clientExpenses.removeAll { it.id == e.id }; protoSaveClientExpenses(prefs, c.id, clientExpenses.toList()) }
                )
            }
            ProtoScreen.ClientSettings -> ProtoClientSettingsScreen(
                notificationsEnabled = notificationsOrders,
                onNotifications = { notificationsOrders = it },
                onBack = { back() }
            )
            ProtoScreen.Profile -> ProtoProfileScreen(currentClient(), onBack = { back() }, onCall = { protoDial(context) }, onLogout = { signOut() }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { go(ProtoScreen.Catalog) }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onAnalytics = { refreshShipments(); go(ProtoScreen.ClientReports) })
            ProtoScreen.Suspended -> ProtoSuspendedScreen(onCall = { protoDial(context) }, onMessage = { protoMessage(context) }, onBack = { back() }, onCatalog = { go(ProtoScreen.Catalog) }, onHome = { history.clear(); screen = ProtoScreen.Home }, onOrders = { go(ProtoScreen.OrderList) })

            else -> Unit
        }
    }

    @Composable fun protoGroupC() {
        when (screen) {
            ProtoScreen.AdminHome -> {
                val today = LocalDate.now()
                ProtoAdminHomeScreen(
                    // Главный администратор — «BOSS», остальные — имя, которое задал главный.
                    adminName = adminAccounts.firstOrNull { it.userId == session?.userId }?.let { if (it.isMain) "BOSS" else it.displayName } ?: session?.firstName.orEmpty(),
                    newOrders = orders.count { it.status == "Получен" },
                    deliveriesToday = orders.count { protoParseDate(it.deliveryDate) == today && it.status !in setOf("Доставлен", "Отменён") },
                    newRegistrations = registrations.count { it.status != "Отклонена" },
                    paymentsPending = shipments.count { it.moneyStatus == "REQUESTED" },
                    unreadChats = chatMessages.count { !it.read && it.senderRole != "ADMIN" },
                    lowStockCount = catalogProducts.count { availableStock(it) <= lowStockThreshold },
                    activeClients = clients.count { it.status != "Приостановлен" },
                    producedToday = productionOps.filter { it.date == currentDateShort() }.sumOf { it.qty },
                    ordersToday = orders.count { it.dateTime.startsWith(currentDateShort()) },
                    unreadCount = notifications.count { it.audienceRole == "ADMIN" && !it.read },
                    onSearch = { go(ProtoScreen.AdminSearch) },
                    onNewOrders = { adminOrdersStatus = "Получен"; adminOrdersDate = "Все даты"; go(ProtoScreen.AdminOrders) },
                    approvals = orders.count { it.status == "На согласовании" },
                    onApprovals = { adminOrdersStatus = "На согласовании"; adminOrdersDate = "Все даты"; go(ProtoScreen.AdminOrders) },
                    onDeliveriesToday = { adminOrdersStatus = "Все"; adminOrdersDate = "Сегодня"; go(ProtoScreen.AdminOrders) },
                    onRegistrations = { go(ProtoScreen.AdminAttention) },
                    onPayments = { refreshShipments(); go(ProtoScreen.AdminPayments) },
                    onChats = { go(ProtoScreen.AdminChats) },
                    onLowStock = { go(ProtoScreen.LowStockList) },
                    onClients = { openTab(ProtoScreen.AdminHome, ProtoScreen.AdminClients) },
                    onOrders = { adminOrdersStatus = "Все"; adminOrdersDate = "Все даты"; openTab(ProtoScreen.AdminHome, ProtoScreen.AdminOrders) },
                    onProduction = { go(ProtoScreen.Production) },
                    onStock = { openTab(ProtoScreen.AdminHome, ProtoScreen.Server) },
                    onCatalog = { go(ProtoScreen.AdminCatalog) },
                    onSettings = { openTab(ProtoScreen.AdminHome, ProtoScreen.AdminSettings) },
                    onOnline = { refreshOperationsData(); go(ProtoScreen.OnlineController) },
                    onWorkshop = { workshopPrefill = emptyMap(); workshopPrefillComment = ""; refreshOperationsData(); go(ProtoScreen.AdminWorkshop) },
                    onAttendance = { refreshOperationsData(); go(ProtoScreen.AdminAttendance) },
                    onReports = { refreshOperationsData(); go(ProtoScreen.AdminReports) },
                    onDashboard = { refreshShipments(); refreshExpenses(); go(ProtoScreen.AdminDashboard) },
                    onNotifications = { go(ProtoScreen.AdminNotifications) }
                )
            }
            ProtoScreen.AdminSearch -> ProtoAdminSearchScreen(clients, orders, products, stockOverrides, onBack = { back() }, onClient = { openAdminClient(it.id) }, onOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) }, onProduct = { selectedAdminSku = it.sku; go(ProtoScreen.AdminProduct) })
            ProtoScreen.AdminClients -> ProtoAdminClientsScreen(clients, clientStats(), onBack = { back() }, onOpen = { openAdminClient(it.id) })
            ProtoScreen.AdminClient -> {
                val c = clients.firstOrNull { it.id == selectedClientId }
                ProtoAdminClientScreen(
                    client = c,
                    stats = clientStats()[selectedClientId] ?: ProtoClientStats(),
                    history = adminClientHistory,
                    users = adminCompanyUsers,
                    retailCustomers = agentCustomers.filter { it.ownerClientId == selectedClientId },
                    retailMarkups = agentMarkups.filter { it.ownerClientId == selectedClientId },
                    shipments = shipments.toList(),
                    clientOrders = c?.let { ordersOfClientAll(it) }.orEmpty(),
                    typeOf = { sku -> products.firstOrNull { it.sku == sku }?.type ?: "Прочее" },
                    onBack = { back() },
                    onStatus = { st -> updateClient(selectedClientId, "Статус: $st") { it.copy(status = st) } },
                    onSuspend = { reason ->
                        val prev = c?.status ?: "Активный"
                        prefs.edit().putString("prev_status_" + selectedClientId, prev).apply()
                        updateClient(selectedClientId, "Приостановлен: $reason") { it.copy(status = "Приостановлен", orderingEnabled = false) }
                    },
                    onRestore = {
                        val prev = prefs.getString("prev_status_" + selectedClientId, "Активный").orEmpty().ifBlank { "Активный" }.takeIf { it != "Приостановлен" } ?: "Активный"
                        updateClient(selectedClientId, "Доступ восстановлен · статус $prev") { it.copy(status = prev, orderingEnabled = true) }
                    },
                    onToggleOrdering = { updateClient(selectedClientId, if (c?.orderingEnabled == true) "Оформление заказов выключено" else "Оформление заказов включено") { it.copy(orderingEnabled = !it.orderingEnabled) } },
                    onDiscount = { d -> if (c?.discount != d) updateClient(selectedClientId, "Скидка: $d%") { it.copy(discount = d.coerceIn(0, 50)) } },
                    onCall = { protoDialNumber(context, c?.phone?.ifBlank { null } ?: BuildConfig.ADMIN_PHONE) },
                    onChat = { selectedChatClientId = selectedClientId; go(ProtoScreen.AdminChat) },
                    onReissueCode = {
                        val clientId = selectedClientId
                        scope.launch {
                            runCatching { repository.reissueAccessCode(clientId) }
                                .onSuccess { code ->
                                    repository.addClientHistory(clientId, "Выдан новый код входа", session?.firstName ?: "Администратор")
                                    applySnapshot(repository.snapshot()); loadClientDetails(clientId)
                                    toast("Новый код доступа: $code")
                                }
                                .onFailure { toast(it.message ?: "Не удалось выдать код") }
                        }
                    },
                    onOpenOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) },
                    canDeleteHistory = adminAccounts.firstOrNull { it.userId == session?.userId }?.isMain == true,
                    onDeleteHistory = { ids ->
                        selectedClientId?.let { cid ->
                            scope.launch {
                                runCatching { repository.deleteClientHistory(cid, ids, session?.userId ?: "") }
                                    .onSuccess { loadClientDetails(cid); toast(if (ids == null) "История очищена" else "Удалено записей: " + ids.size) }
                                    .onFailure { toast(it.message ?: "Ошибка") }
                            }
                        }
                    }
                )
            }
            ProtoScreen.AdminOrders -> ProtoAdminOrdersScreen(orders, initialStatus = adminOrdersStatus, initialDate = adminOrdersDate, onBack = { back() }, onOpen = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) }, clientTypeOf = { orderClientType(it) }, clientPhoneOf = { o -> o.contactPhone.ifBlank { clients.firstOrNull { c -> c.id == o.clientId }?.phone.orEmpty() } })
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
                        if (newIndex >= 2 && oldIndex in 0..1) {
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
                            audienceKey = oldOrder.clientId.ifBlank { oldOrder.clientName },
                            title = "Заказ " + oldOrder.id,
                            message = "Статус изменён: " + st,
                            orderId = oldOrder.id
                        )
                        persistAll()
                    } else toast("Статус заказа нельзя переводить назад")
                }
            }, onCancel = {
                val idx = orders.indexOfFirst { it.id == selectedOrderId }
                if (idx >= 0) {
                    val oldOrder = orders[idx]
                    val statuses = listOf("Получен","Подтверждён","Собирается","Доставляется","Доставлен")
                    val oldIndex = statuses.indexOf(oldOrder.status)
                    if (oldIndex in 0..3) {
                        // Товар уже списан со склада (статус дальше «Подтверждён») — возвращаем его.
                        if (oldIndex >= 2) {
                            oldOrder.lines.forEach { line ->
                                products.firstOrNull { it.sku == line.sku }?.let { p -> stockOverrides[p.sku] = physicalStock(p) + line.qty }
                            }
                        }
                        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        orders[idx] = oldOrder.copy(status = "Отменён", history = oldOrder.history + ProtoOrderEvent("Отменён", now, "Администратор"))
                        addNotification(audienceRole = "CLIENT", audienceKey = oldOrder.clientId.ifBlank { oldOrder.clientName }, title = "Заказ " + oldOrder.id, message = "Заказ отменён администратором", orderId = oldOrder.id)
                        persistAll()
                        toast("Заказ " + oldOrder.id + " отменён")
                    }
                }
            }, clientTypeLabel = orders.firstOrNull { it.id == selectedOrderId }?.let { orderClientType(it) }.orEmpty(),
            onPayment = { amount, method, date, comment ->
                updateOrder(selectedOrderId) { o ->
                    val paid = o.copy(payments = o.payments + ProtoPayment(amount, date, method, comment, adminActorName()))
                    paid.copy(history = paid.history + ProtoOrderEvent("Оплата", nowStamp(), adminActorName(), protoMoney(amount) + " · " + method + " · " + paid.paymentStatus.lowercase()))
                }?.let { o ->
                    addNotification("CLIENT", o.clientId.ifBlank { o.clientName }, "Оплата по заказу " + o.id, "Получено " + protoMoney(amount) + ". " + o.paymentStatus + if (o.due > 0) ", осталось " + protoMoney(o.due) else "", orderId = o.id)
                    toast("Оплата отмечена: " + o.paymentStatus.lowercase())
                }
            },
            onApprove = {
                updateOrder(selectedOrderId) { o -> o.copy(status = "Получен", history = o.history + ProtoOrderEvent("Согласован", nowStamp(), adminActorName(), "Сумма ниже минимальной — согласовано")) }?.let { o ->
                    addNotification("CLIENT", o.clientId.ifBlank { o.clientName }, "Заказ " + o.id + " согласован", "Заказ принят в работу", orderId = o.id)
                    toast("Заказ согласован")
                }
            },
            onReject = { reason ->
                updateOrder(selectedOrderId) { o -> o.copy(status = "Отклонён", history = o.history + ProtoOrderEvent("Отклонён", nowStamp(), adminActorName(), reason)) }?.let { o ->
                    addNotification("CLIENT", o.clientId.ifBlank { o.clientName }, "Заказ " + o.id + " не согласован", reason + ". Товары можно вернуть в корзину из заказа.", orderId = o.id)
                    toast("Заказ отклонён")
                }
            },
            onShiftDelivery = { days ->
                updateOrder(selectedOrderId) { o ->
                    val newDate = protoParseDate(o.deliveryDate)?.plusDays(days.toLong())?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: o.deliveryDate
                    o.copy(deliveryDate = newDate, history = o.history + ProtoOrderEvent("Доставка перенесена", nowStamp(), adminActorName(), o.deliveryDate + " → " + newDate))
                }?.let { o ->
                    addNotification("CLIENT", o.clientId.ifBlank { o.clientName }, "Заказ " + o.id + ": новая дата", "Доставка перенесена на " + o.deliveryDate, orderId = o.id)
                    toast("Доставка перенесена на " + o.deliveryDate)
                }
            },
            onEdit = { newLines, reason ->
                val before = orders.firstOrNull { it.id == selectedOrderId }
                updateOrder(selectedOrderId) { o ->
                    // Если товар уже списан со склада (сборка), возвращаем/списываем разницу.
                    if (protoOrderFlow.indexOf(o.status) >= 2) {
                        (o.lines.map { it.sku } + newLines.map { it.sku }).distinct().forEach { sku ->
                            val diff = (newLines.firstOrNull { it.sku == sku }?.qty ?: 0) - (o.lines.firstOrNull { it.sku == sku }?.qty ?: 0)
                            if (diff != 0) products.firstOrNull { it.sku == sku }?.let { p -> stockOverrides[p.sku] = (physicalStock(p) - diff).coerceAtLeast(0) }
                        }
                    }
                    val summary = protoOrderDiff(o.lines, newLines)
                    o.copy(lines = newLines, history = o.history + ProtoOrderEvent("Изменён", nowStamp(), adminActorName(), "$reason. $summary. Сумма " + protoMoney(o.total) + " → " + protoMoney(newLines.sumOf { it.lineTotal })))
                }?.let { o ->
                    addNotification("CLIENT", o.clientId.ifBlank { o.clientName }, "Заказ " + o.id + " изменён", reason + ". Новая сумма " + protoMoney(o.total) + (if (before != null) " (было " + protoMoney(before.total) + ")" else ""), orderId = o.id)
                    toast("Заказ изменён")
                }
            }, onCallClient = {
                orders.firstOrNull { it.id == selectedOrderId }?.let { o ->
                    val phone = o.contactPhone.ifBlank { clients.firstOrNull { c -> c.id == o.clientId || c.name == o.clientName }?.phone.orEmpty() }
                    if (phone.isNotBlank()) protoDialNumber(context, phone) else toast("Телефон клиента не указан")
                }
            }, onToWorkshop = {
                orders.firstOrNull { it.id == selectedOrderId }?.let { o ->
                    workshopPrefill = o.lines.associate { it.sku to it.qty }
                    workshopPrefillComment = "Под заказ № " + o.id + " · " + o.clientName + if (o.deliveryDate.isNotBlank()) " · к " + o.deliveryDate else ""
                }
                refreshOperationsData(); go(ProtoScreen.AdminWorkshop)
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
                },
                onOpen = { n ->
                    val i = notifications.indexOfFirst { it.id == n.id }
                    if (i >= 0 && !n.read) { notifications[i] = n.copy(read = true); persistNotifications() }
                    when {
                        n.orderId.isNotBlank() && orders.any { it.id == n.orderId } -> { selectedOrderId = n.orderId; go(ProtoScreen.AdminOrderDetail) }
                        n.title.contains("заявк", true) -> go(ProtoScreen.AdminAttention)
                        n.title.contains("оплат", true) || n.title.contains("отгрузк", true) -> { refreshShipments(); go(ProtoScreen.AdminPayments) }
                    }
                }
            )
            ProtoScreen.AdminCatalog -> ProtoAdminCatalogScreen(
                products = products,
                physicalOf = { physicalStock(it) },
                availableOf = { availableStock(it) },
                syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, syncing = catalogSyncInProgress,
                onSync = { syncTildaCatalog() }, onBack = { back() },
                onOpen = { selectedAdminSku = it.sku; go(ProtoScreen.AdminProduct) }
            )
            ProtoScreen.AdminProduct -> {
                val p = products.firstOrNull { it.sku == selectedAdminSku }
                ProtoAdminProductScreen(
                    product = p,
                    physical = p?.let { physicalStock(it) } ?: 0,
                    reserved = p?.let { reservedForSku(it.sku) } ?: 0,
                    rate = p?.let { productionRates[it.sku] } ?: 0,
                    orders = orders,
                    canAdjust = session?.role != SansaraRole.PRODUCTION,
                    onBack = { back() },
                    onAdjust = { delta, reason -> p?.let { adjustStock(it, delta, reason) } },
                    onOpenOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) }
                )
            }
            ProtoScreen.AdminSettings -> ProtoAdminSettingsMenuScreen(
                syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, lowStockThreshold = lowStockThreshold, pickupCount = pickupPoints().size,
                onBack = { back() }, onOpen = { section ->
                    when (section) {
                        "Клиенты" -> go(ProtoScreen.AdminClients)
                        "Сборщицы" -> go(ProtoScreen.AdminAssemblers)
                        "Сотрудники и коды" -> { refreshOperationsData(); go(ProtoScreen.AdminAdmins) }
                        "Рассылка" -> adminDialog = "Рассылка"
                        "Обновление приложения" -> adminDialog = "Обновление"
                        "Глаз бога" -> scope.launch {
                            runCatching { repository.godEyeTargets(session?.userId ?: "") }
                                .onSuccess { godEyePicker = it }
                                .onFailure { toast(it.message ?: "Недоступно") }
                        }
                        "Задание в цех" -> { workshopPrefill = emptyMap(); workshopPrefillComment = ""; refreshOperationsData(); go(ProtoScreen.AdminWorkshop) }
                        "Табель рабочего времени" -> { refreshOperationsData(); go(ProtoScreen.AdminAttendance) }
                        "Отчёты" -> { refreshOperationsData(); go(ProtoScreen.AdminReports) }
                        "Экспорт данных" -> go(ProtoScreen.Export)
                        else -> { settingsSection = section; go(ProtoScreen.AdminSettingsDetail) }
                    }
                }, onCall = { protoDial(context) }, onLogout = { signOut() },
                minOrder = minOrderTrading,
                showGodEye = adminAccounts.firstOrNull { it.userId == session?.userId }?.isMain == true
            )
            ProtoScreen.AdminSettingsDetail -> ProtoAdminSettingsDetailScreen(
                section = settingsSection, threshold = lowStockThreshold,
                tildaUrl = tildaFeedUrl, syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, syncing = catalogSyncInProgress, backendStatus = backendStatus,
                telegramEnabled = telegramEnabled, telegramRetryEnabled = telegramRetryEnabled, telegramApiUrl = backendApiUrl, telegramTemplate = telegramTemplate, telegramLastLog = telegramLastLog,
                pickupPoints = pickupPoints(),
                products = catalogProducts,
                rates = productionRates,
                onBack = { back() },
                onThreshold = { lowStockThreshold = it.coerceIn(1, 50); prefs.edit().putInt("low_stock_threshold", lowStockThreshold).apply() },
                onTildaUrl = { tildaFeedUrl = it }, onSync = { syncTildaCatalog() },
                onTelegramSave = { enabled, retry, url, template ->
                    telegramEnabled = enabled
                    telegramRetryEnabled = retry
                    backendApiUrl = url.trim()
                    telegramTemplate = template
                    backendStatus = if (backendApiUrl.isBlank()) "Backend API не настроен · события сохраняются локально" else "Backend API настроен"
                    prefs.edit()
                        .putBoolean("telegram_enabled", enabled)
                        .putBoolean("telegram_retry_enabled", retry)
                        .putString("backend_api_url", backendApiUrl)
                        .putString("telegram_template", template)
                        .apply()
                    toast("Настройки Telegram сохранены")
                },
                onPickupSave = { list -> prefs.edit().putString("pickup_points", list.joinToString("\n")).apply(); toast("Точки самовывоза сохранены: " + list.size); back() },
                promoTitle = prefs.getString("welcome_promo_title", "").orEmpty(),
                promoText = prefs.getString("welcome_promo_text", "").orEmpty(),
                onPromoSave = { t, x -> prefs.edit().putString("welcome_promo_title", t).putString("welcome_promo_text", x).apply(); toast("Приветственный экран обновлён"); back() },
                promoImageVersion = welcomeImageVersion,
                minOrder = minOrderTrading,
                onMinOrder = { v -> minOrderTrading = v; prefs.edit().putInt("min_order_trading", v).apply(); toast(if (v > 0) "Минимальный заказ: " + protoMoney(v) else "Минимальный заказ отключён"); back() },
                onPromoImage = { uri ->
                    scope.launch {
                        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { protoSaveWelcomeImage(context, uri) }
                        if (ok) { welcomeImageVersion = System.currentTimeMillis(); prefs.edit().putLong("welcome_promo_image_v", welcomeImageVersion).apply(); toast("Картинка сохранена") }
                        else toast("Не удалось открыть картинку")
                    }
                },
                onPromoImageReset = { protoWelcomeImageFile(context).delete(); welcomeImageVersion = 0L; prefs.edit().putLong("welcome_promo_image_v", 0L).apply(); toast("Возвращена стандартная картинка") },
                onRate = { skus, rate ->
                    skus.forEach { productionRates[it] = rate }
                    scope.launch { skus.forEach { repository.setProductionRate(it, rate) } }
                }
            )
            ProtoScreen.AdminAttention -> ProtoAttentionScreen(
                regs = registrations.filter { it.status != "Отклонена" }, blocked = clients.filter { it.status == "Приостановлен" }, picking = orders.filter { it.status == "Собирается" }.sortedWith(compareByDescending<ProtoOrder> { orderClientType(it).contains("Агент", true) }.thenBy { protoParseDate(it.deliveryDate)?.toEpochDay() ?: Long.MAX_VALUE }),
                lowStock = catalogProducts.filter { availableStock(it) <= lowStockThreshold }, stockValue = { availableStock(it) },
                onBack = { back() }, onClient = { openAdminClient(it.id) }, onOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) },
                onApprove = { reg, status, discount ->
                    scope.launch {
                        runCatching { repository.approve(reg, status, discount, session?.firstName ?: "Администратор") }
                            .onSuccess { approval ->
                                applySnapshot(repository.snapshot())
                                toast("Клиент создан со статусом «Активный»")
                                openAdminClient(approval.client.id)
                                // Каждому контакту — свой код отдельным SMS.
                                approvedCodes = listOfNotNull(
                                    Triple(reg.contact1.ifBlank { reg.organization }, reg.phone1, approval.primaryCode),
                                    approval.secondaryCode?.let { Triple(reg.contact2, reg.phone2, it) }
                                )
                            }
                            .onFailure { toast(it.message ?: "Не удалось одобрить заявку") }
                    }
                },
                onReject = { reg, reason ->
                    scope.launch {
                        repository.reject(reg, reason)
                        applySnapshot(repository.snapshot())
                        toast("Заявка отклонена")
                        protoSms(context, reg.phone1, "SANSARA: " + PROTO_REJECT_TEXT)
                    }
                },
                onLowStock = { go(ProtoScreen.LowStockList) }
            )
            ProtoScreen.OnlineController -> ProtoOnlineControllerScreen(clients, presenceSessions, onBack = { back() }, onClient = { openAdminClient(it.id) })
            ProtoScreen.LowStockList -> ProtoLowStockListScreen(products, physicalOf = { physicalStock(it) }, reservedForSku = { reservedForSku(it) }, threshold = lowStockThreshold, onBack = { back() }, onOpen = { selectedAdminSku = it.sku; go(ProtoScreen.AdminProduct) })
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
                },
                onSave={a->
                    scope.launch {
                        runCatching { repository.saveAssemblerDetails(a.id,a.name,a.phone,a.birthDate,a.comment) }
                            .onSuccess { applySnapshot(repository.snapshot()); toast("Карточка сохранена"); runCatching { SansaraNotify.birthdayCheck(context) } }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                }
            )

            ProtoScreen.AdminAdmins -> ProtoAdminAccountsScreen(
                admins = adminAccounts,
                currentUserId = session?.userId.orEmpty(),
                issued = issuedAdminCode,
                onBack = { issuedAdminCode = null; back() },
                onAdd = { name, phone, production ->
                    scope.launch {
                        runCatching { repository.addAdmin(name, phone, session?.userId ?: "", if (production) SansaraRole.PRODUCTION else SansaraRole.ADMIN) }
                            .onSuccess { (_, code) -> refreshOperationsData(); issuedAdminCode = name to code }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                },
                onToggle = { admin ->
                    scope.launch {
                        runCatching { repository.setAdminEnabled(admin.userId, !admin.enabled, session?.userId ?: "") }
                            .onSuccess { refreshOperationsData() }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                },
                onIssuedSeen = { issuedAdminCode = null },
                onLoadCode = { admin, done -> scope.launch { runCatching { repository.staffCode(admin.userId, session?.userId ?: "") }.onSuccess(done).onFailure { toast(it.message ?: "Ошибка") } } },
                onReissue = { admin, done -> scope.launch { runCatching { repository.reissueStaffCode(admin.userId, session?.userId ?: "") }.onSuccess { done(it); toast("Новый код выдан") }.onFailure { toast(it.message ?: "Ошибка") } } },
                onPhone = { admin, phone -> scope.launch { runCatching { repository.setStaffPhone(admin.userId, phone, session?.userId ?: "") }.onSuccess { refreshOperationsData(); toast("Телефон сохранён") }.onFailure { toast(it.message ?: "Ошибка") } } },
                onRename = { admin, name ->
                    scope.launch {
                        runCatching { repository.renameStaff(admin.userId, name, session?.userId ?: "") }
                            .onSuccess { refreshOperationsData(); toast("Имя сохранено") }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                }
            )
            ProtoScreen.AdminWorkshop -> ProtoWorkshopTasksScreen(
                tasks = workshopTasks,
                products = catalogProducts,
                productionMode = false,
                prefill = workshopPrefill,
                prefillComment = workshopPrefillComment,
                onBack = { back() },
                onCreate = { date, linesJson, comment, commentOnly ->
                    scope.launch {
                        runCatching { repository.createWorkshopTask(date, linesJson, comment, commentOnly, session?.userId ?: "U-ADMIN") }
                            .onSuccess { task ->
                                refreshOperationsData()
                                addNotification("PRODUCTION", "", "Новое задание в цех", task.taskDate + " · " + protoWorkshopPlanQty(task.linesJson) + " шт.")
                                workshopPrefill = emptyMap(); workshopPrefillComment = ""
                                toast("Задание отправлено в цех")
                            }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                },
                onFact = { _, _, _ -> }
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
            else -> Unit
        }
    }

    @Composable fun protoGroupD() {
        when (screen) {
            ProtoScreen.SalesHome -> ProtoSalesHomeScreen(
                name = session?.firstName.orEmpty().ifBlank { "Продажи" },
                shipments = shipments,
                onNew = { go(ProtoScreen.SalesShipmentNew) },
                onList = { refreshShipments(); go(ProtoScreen.SalesShipments) },
                onClients = { go(ProtoScreen.SalesClients) },
                onLogout = { signOut() }
            )
            ProtoScreen.SalesShipmentNew -> ProtoSalesShipmentForm(
                clients = clients, orders = orders, shipments = shipments, onBack = { back() },
                onSave = { client, order, amount, type, due ->
                    scope.launch {
                        runCatching { repository.createShipment(order.id, client.id, client.name, amount, type, due, session?.userId ?: "U-SALES") }
                            .onSuccess {
                                refreshShipments()
                                val oi = orders.indexOfFirst { it.id == order.id }
                                if (oi >= 0 && orders[oi].status == "Собирается") {
                                    val o = orders[oi]
                                    orders[oi] = o.copy(status = "Доставляется", history = o.history + ProtoOrderEvent("Доставляется", nowStamp(), session?.firstName?.ifBlank { null } ?: "Отдел продаж"))
                                    addNotification("CLIENT", o.clientId.ifBlank { o.clientName }, "Заказ " + o.id, "Статус изменён: Доставляется", orderId = o.id)
                                    persistAll()
                                }
                                addNotification("ADMIN", "", if (type == "PAID") "Запрос на подтверждение оплаты" else "Новая отгрузка с отсрочкой",
                                    client.name + " · " + order.id + " · " + protoMoney(amount) + if (type == "PAID") "" else " · оплата до " + due)
                                toast("Отгрузка оформлена")
                                back()
                            }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                }
            )
            ProtoScreen.SalesShipments -> ProtoShipmentsScreen(
                items = shipments, adminMode = false, onBack = { back() },
                onRequest = { sh ->
                    scope.launch {
                        runCatching { repository.requestMoneyConfirmation(sh.id) }
                            .onSuccess { refreshShipments(); addNotification("ADMIN", "", "Запрос на подтверждение оплаты", sh.clientName + " · " + protoMoney(sh.amount)); toast("Запрос отправлен администратору") }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                },
                onConfirm = {}, onReject = { _, _ -> }
            )
            ProtoScreen.AdminPayments -> ProtoShipmentsScreen(
                items = shipments, adminMode = true, onBack = { back() }, onRequest = {},
                onConfirm = { sh ->
                    scope.launch {
                        runCatching { repository.confirmMoney(sh.id, session?.userId ?: "U-ADMIN") }
                            .onSuccess { refreshShipments(); toast("Оплата подтверждена") }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                },
                onReject = { sh, reason ->
                    scope.launch {
                        runCatching { repository.rejectMoney(sh.id, reason) }
                            .onSuccess { refreshShipments(); toast("Запрос отклонён") }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                }
            )
            ProtoScreen.AdminDashboard -> ProtoManagerDashboardScreen(
                shipments = shipments, expenses = expenses, onBack = { back() },
                onPayments = { refreshShipments(); go(ProtoScreen.AdminPayments) },
                onAddExpense = { cat, amount, comment ->
                    scope.launch {
                        runCatching { repository.addExpense(currentDateShort(), cat, amount, comment, session?.userId ?: "U-ADMIN") }
                            .onSuccess { refreshExpenses(); toast("Расход добавлен") }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                },
                onDeleteExpense = { ex -> scope.launch { repository.deleteExpense(ex.id); refreshExpenses(); toast("Расход удалён") } }
            )
            ProtoScreen.SalesProfile -> ProtoStaffProfileScreen(role = "Продажи", onBack = { back() }, onCall = { protoDial(context) }, onLogout = { signOut() })
            ProtoScreen.SalesClients -> ProtoSalesClientsScreen(
                clients = clients, shipments = shipments, onBack = { back() },
                onOpen = { selectedClientId = it.id; go(ProtoScreen.SalesClient) }
            )
            ProtoScreen.SalesClient -> ProtoSalesClientScreen(
                client = clients.firstOrNull { it.id == selectedClientId }, shipments = shipments, orders = orders, onBack = { back() }
            )
            ProtoScreen.AdminReports -> ProtoAdminReportsScreen(
                tasks = workshopTasks,
                attendance = attendanceRows,
                presence = presenceSessions,
                productionOps = productionOps,
                orders = orders,
                clientName = { id -> clients.firstOrNull { it.id == id }?.name ?: when { id == null -> "" ; else -> id } },
                dailyStatus = adminDailyStatus,
                onBack = { back() },
                onDailyStatus = { dayOff, reason ->
                    scope.launch {
                        repository.saveAdminDailyStatus(currentDateShort(), dayOff, reason)
                        refreshOperationsData()
                    }
                }
            )
            else -> Unit
        }
    }

    @Composable fun protoGroupE() {
        when (screen) {
            ProtoScreen.OwnStock -> {
                val c = currentClient()
                LaunchedEffect(c.id) { ownOps.clear(); ownOps.addAll(repository.ownStockOps(c.id)) }
                fun reload() = scope.launch { ownOps.clear(); ownOps.addAll(repository.ownStockOps(c.id)) }
                ProtoOwnStockScreen(
                    products = catalogProducts,
                    ops = ownOps,
                    external = ownExternal(c.id),
                    onBack = { back() },
                    onExternal = { on -> guarded(if (on) "включить внешний учёт склада" else "выключить внешний учёт склада") { prefs.edit().putBoolean("own_external_" + c.id, on).apply(); ownExternalTick++ } },
                    onReceive = { p, qty, serial -> guarded("оприходовать $qty шт.") {
                        scope.launch {
                            runCatching { repository.addOwnStockOp(c.id, p.sku, p.name, qty, "Приход", serial = serial, actor = actor(session?.firstName ?: c.contact)) }
                                .onSuccess { reload(); toast("Оприходовано: " + protoTitle(p) + " · " + qty + " шт.") }
                                .onFailure { toast(it.message ?: "Ошибка") }
                        }
                    } },
                    onAdjust = { p, delta, reason -> guarded("провести корректировку") {
                        scope.launch {
                            runCatching { repository.addOwnStockOp(c.id, p.sku, p.name, delta, "Корректировка", reason = reason, actor = actor(session?.firstName ?: c.contact)) }
                                .onSuccess { reload(); toast("Корректировка проведена") }
                                .onFailure { toast(it.message ?: "Ошибка") }
                        }
                    } },
                    onScanError = { toast(it) }
                )
            }
            ProtoScreen.Production -> {
                val productionDateKey = selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ProtoProductionHomeScreen(
                    selectedDate = selectedProductionDate,
                    drafts = productionDrafts.filter { it.date == productionDateKey },
                    opsForDay = productionOps.filter { it.date == productionDateKey && it.qty > 0 },
                    products = products,
                    openTasks = workshopTasks.count { it.status != "Выполнено" },
                    unreadChat = chatMessages.count { it.conversationId == "STAFF:ADMIN_PRODUCTION" && !it.read && it.senderRole == "ADMIN" },
                    onDateChange = { selectedProductionDate = it },
                    onAdd = { go(ProtoScreen.ProductionEntry) },
                    onRemoveDraft = { d -> productionDrafts.remove(d); saveDrafts() },
                    onPostAll = { guarded("оприходовать выпуск") { postDrafts(productionDateKey) } },
                    onReport = { go(ProtoScreen.ProductionReport) },
                    onPayments = { go(ProtoScreen.ProductionPayments) },
                    onWorkshop = { refreshOperationsData(); go(ProtoScreen.ProductionWorkshop) },
                    onAttendance = { refreshOperationsData(); go(ProtoScreen.ProductionAttendance) },
                    onChat = { go(ProtoScreen.ProductionChat) },
                    onHistory = { openTab(ProtoScreen.Production, ProtoScreen.ProductionHistory) },
                    onStock = { openTab(ProtoScreen.Production, ProtoScreen.Server) },
                    onProfile = { openTab(ProtoScreen.Production, ProtoScreen.ProductionProfile) }
                )
            }
            ProtoScreen.ProductionCategory -> { LaunchedEffect(Unit) { screen = ProtoScreen.ProductionEntry } }
            ProtoScreen.ProductionCatalog -> { LaunchedEffect(Unit) { screen = ProtoScreen.ProductionEntry } }
            ProtoScreen.ProductionEntry -> {
                val productionDateKey = selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ProtoProductionEntryScreen(
                    products = catalogProducts,
                    assemblers = assemblers.filter { it.enabled },
                    rates = productionRates,
                    selectedDate = selectedProductionDate,
                    initialAssembler = productionAssembler,
                    onBack = { back() },
                    onAddDraft = { p, qty, assembler, rate ->
                        productionAssembler = assembler
                        productionDrafts.add(ProtoProductionDraft(p, qty, assembler, rate, productionDateKey))
                        saveDrafts()
                        toast("В черновике: " + protoTitle(p) + " × " + qty)
                    }
                )
            }
            ProtoScreen.ProductionHistory -> ProtoProductionHistoryScreen(
                productionOps,
                products,
                canEdit=session?.role==SansaraRole.ADMIN,
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
            ProtoScreen.ProductionPayments -> ProtoProductionPaymentsScreen(productionOps, onBack = { back() }, onExport = { go(ProtoScreen.Export) })
            ProtoScreen.ProductionProfile -> ProtoStaffProfileScreen(role = "Производство", onBack = { back() }, onCall = { protoDial(context) }, onLogout = { signOut() }, onUpdate = { showUpdateDialog = true })

            ProtoScreen.ProductionWorkshop -> ProtoWorkshopTasksScreen(
                tasks=workshopTasks,
                products=catalogProducts,
                productionMode=true,
                onBack={back()},
                onCreate={_,_,_,_->},
                onFact={task,qty,status->
                    scope.launch {
                        repository.updateWorkshopTaskFact(task.id,qty,status)
                        refreshOperationsData()
                    }
                },
                assemblers=assemblers,
                onRun={task,run-> guarded(if (run) "взять задание в работу" else "поставить задание на паузу") {
                    scope.launch {
                        runCatching { repository.setWorkshopTaskRunning(task.id,run) }
                            .onSuccess { refreshOperationsData(); if (run && task.startedAt <= 0L) addNotification("ADMIN","ADMIN","Задание в цех взято в работу","Задание на "+task.taskDate) }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                } },
                onPost={task,done,comment,who-> guarded("провести задание") {
                    scope.launch {
                        runCatching { repository.postWorkshopTask(task.id,done,comment,who?.id.orEmpty(),who?.name.orEmpty(),productionRates.toMap(),session?.userId ?: "U-PRODUCTION") }
                            .onSuccess { (closed,carried) ->
                                applySnapshot(repository.snapshot())
                                refreshOperationsData()
                                val time=protoDuration(closed.workMs)
                                if (carried!=null) {
                                    toast("Проведено частично. Остаток перенесён на "+carried.taskDate)
                                    addNotification("ADMIN","ADMIN","Задание собрано частично","Задание на "+task.taskDate+": собрано "+closed.actualQty+" шт. за "+time+", остаток перенесён на "+carried.taskDate)
                                } else {
                                    toast("Задание собрано за "+time)
                                    addNotification("ADMIN","ADMIN","Задание в цех собрано","Задание на "+task.taskDate+": "+closed.actualQty+" шт., время сборки "+time)
                                }
                            }
                            .onFailure { toast(it.message ?: "Ошибка") }
                    }
                } }
            )
            ProtoScreen.ProductionAttendance -> ProtoAttendanceScreen(
                selectedDate=selectedProductionDate,
                assemblers=assemblers,
                rows=attendanceRows,
                editable=true,
                onBack={back()},
                onDate={selectedProductionDate=it;refreshOperationsData()},
                onSave={person,status,comment-> guarded("изменить табель") {
                    scope.launch {
                        repository.saveAttendance(selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),person.id,person.name,status,comment)
                        refreshOperationsData()
                    }
                } }
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

            ProtoScreen.Server -> ProtoServerScreen(
                products = products,
                physicalOf = { physicalStock(it) },
                reservedForSku = { reservedForSku(it) },
                productionOps = productionOps,
                clients = clients,
                canAdjust = session?.role != SansaraRole.PRODUCTION,
                showClients = session?.role != SansaraRole.PRODUCTION,
                onBack = { back() },
                onProduced = { go(ProtoScreen.ProductionHistory) },
                onReserve = { go(ProtoScreen.ReserveList) },
                onNewClients = { go(ProtoScreen.NewClients) },
                onExport = { go(ProtoScreen.Export) },
                onOpen = { selectedAdminSku = it.sku; go(ProtoScreen.AdminProduct) },
                onAdjust = { p, delta, reason -> adjustStock(p, delta, reason) }
            )
            ProtoScreen.StockList -> { LaunchedEffect(Unit) { screen = ProtoScreen.Server } }
            ProtoScreen.ReserveList -> ProtoReserveListScreen(orders, onBack = { back() }, onOpen = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) }, showClients = session?.role != SansaraRole.PRODUCTION)
            ProtoScreen.NewClients -> ProtoNewClientsScreen(clients, onBack = { back() }, onOpen = { openAdminClient(it.id) })
            ProtoScreen.Export -> ProtoExportScreen(onBack = { back() }, onExport = { report, period -> protoExportCsv(context, report, period, products, stockOverrides, orders, clients, productionOps, reservedForSku = { reservedForSku(it) }, toast = { toast(it) }) })
            ProtoScreen.AgentClients -> {
                val ownerId = currentClient().id
                val mine = agentCustomers.filter { it.ownerClientId == ownerId }
                val st = ownerAgentSettings(ownerId)
                ProtoAgentHubScreen(
                    newCount = mine.count { isNewAgentCustomer(it) },
                    totalCount = mine.size,
                    ordersCount = orders.count { it.clientId == ownerId && it.retailCustomerId.isNotBlank() },
                    markupLabel = (if (st.generalMarkupEnabled) "Общая ${st.generalMarkupPct}%" else "Общая выключена") + if (agentSkuMarkups.isNotEmpty()) " · на позиции: ${agentSkuMarkups.size}" else "",
                    onBack = { back() },
                    onNew = { showAgentNewCustomer = true },
                    onBase = { go(ProtoScreen.AgentBase) },
                    onMarkup = { go(ProtoScreen.AgentMarkup) },
                    onOrders = { go(ProtoScreen.AgentOrders) }
                )
            }
            ProtoScreen.AgentBase -> {
                val ownerId = currentClient().id
                ProtoAgentBaseScreen(
                    customers = agentCustomers.filter { it.ownerClientId == ownerId },
                    isNew = { isNewAgentCustomer(it) },
                    ordersOf = { c -> orders.count { it.retailCustomerId == c.id && it.status != "Отменён" } },
                    onBack = { back() },
                    onOpen = { c -> selectedAgentCustomerId = c.id; go(ProtoScreen.AgentClientDetail) },
                    onAdd = { showAgentNewCustomer = true }
                )
            }
            ProtoScreen.AgentMarkup -> {
                val ownerId = currentClient().id
                ProtoAgentMarkupScreen(
                    settings = ownerAgentSettings(ownerId),
                    skuMarkupCount = agentSkuMarkups.size,
                    onBack = { back() },
                    onSave = { enabled, pct ->
                        scope.launch {
                            runCatching { repository.saveAgentSettings(ownerId, enabled, pct) }
                                .onSuccess { refreshAgentData(); toast(if (enabled) "Общая наценка $pct% сохранена" else "Наценка выключена — продажа по закупочной цене") }
                                .onFailure { toast(it.message ?: "Ошибка сохранения") }
                        }
                    },
                    onSpecific = { go(ProtoScreen.AgentProductMarkup) }
                )
            }
            ProtoScreen.AgentProductMarkup -> {
                val ownerId = currentClient().id
                val st = ownerAgentSettings(ownerId)
                ProtoAgentProductMarkupScreen(
                    products = catalogProducts,
                    markups = agentSkuMarkups,
                    generalPct = if (st.generalMarkupEnabled) st.generalMarkupPct else 0,
                    onBack = { back() },
                    onSet = { sku, pct ->
                        if (pct == null) agentSkuMarkups.remove(sku) else agentSkuMarkups[sku] = pct
                        protoSaveSkuMarkups(prefs, ownerId, agentSkuMarkups.toMap())
                    }
                )
            }
            ProtoScreen.AgentOrders -> {
                val ownerId = currentClient().id
                ProtoAgentOrdersScreen(
                    orders = orders.filter { it.clientId == ownerId && it.retailCustomerId.isNotBlank() },
                    onBack = { back() },
                    onOpen = { o -> selectedAgentCustomerId = o.retailCustomerId; selectedOrderId = o.id; go(ProtoScreen.RetailOrderDetail) }
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
                    },
                    discount=agentDiscountTick.let{customer?.let{protoCustomerDiscount(prefs,it.id)}?:0},
                    onDiscount={pct->customer?.let{protoSetCustomerDiscount(prefs,it.id,pct);agentDiscountTick+=1}},
                    ordersCount=orders.count{it.retailCustomerId==selectedAgentCustomerId&&it.status!="Отменён"},
                    onOrders={go(ProtoScreen.AgentOrders)}
                )
            }
            else -> Unit
        }
    }

    @Composable fun protoGroupF() {
        // Режим «Для клиентов»: агент показывает каталог своему покупателю по ценам с наценкой,
        // а в базу заказ уходит по закупочной цене агента (его скидка), с пометкой конечного покупателя.
        val owner = currentClient()
        val customer = agentCustomers.firstOrNull { it.id == selectedAgentCustomerId }
        val customerName = customer?.fullName ?: "Частный клиент"
        val retail = retailProducts(owner.id, customer?.id.orEmpty())
        val baseOf: (ProtoCatalogProduct) -> ProtoCatalogProduct? = { rp -> products.firstOrNull { it.sku == rp.sku } }
        val retailStock: (ProtoCatalogProduct) -> Int = { rp -> baseOf(rp)?.let { availableStock(it) } ?: 0 }
        val retailOrders = orders.filter { it.retailCustomerId.isNotBlank() && it.retailCustomerId == customer?.id }
        val exitRetail: () -> Unit = { history.clear(); screen = ProtoScreen.AgentClientDetail }
        val rHome: () -> Unit = { history.clear(); screen = ProtoScreen.RetailHome }
        val rCatalog: () -> Unit = { go(ProtoScreen.RetailCatalog) }
        val rCart: () -> Unit = { go(ProtoScreen.RetailCart) }
        val rOrders: () -> Unit = { go(ProtoScreen.RetailOrderList) }
        fun setRetailQty(p: ProtoCatalogProduct, q: Int) { if (q <= 0) retailCart.remove(p.sku) else retailCart[p.sku] = q }
        fun asRetailView(o: ProtoOrder): ProtoOrder =
            o.copy(lines = o.lines.map { l -> if (l.retailPrice > 0) l.copy(price = l.retailPrice, discountPct = 0) else l })
        when (screen) {
            ProtoScreen.RetailHome -> {
                val retailClient = ProtoClient(customer?.id ?: "RETAIL", customerName, customer?.fullName.orEmpty(), customer?.phone.orEmpty(), "Активный", 0, 0, 0, "", false, "сейчас",
                    email = customer?.email.orEmpty(), city = customer?.city.orEmpty(), address = customer?.address.orEmpty(),
                    firstName = customer?.fullName?.substringBefore(" ").orEmpty().ifBlank { "Клиент" })
                ProtoClientHomeScreen(
                    client = retailClient,
                    products = retail,
                    popularity = popularity(),
                    availableStock = retailStock,
                    cart = retailCart,
                    lastOrder = retailOrders.maxByOrNull { protoOrderEpoch(it.dateTime) }?.let { asRetailView(it) },
                    unreadCount = 0,
                    onNotifications = { go(ProtoScreen.RetailNotifications) },
                    onSearch = { q -> openProductList(query = q, focus = q.isBlank(), target = ProtoScreen.RetailProductList) },
                    onFilter = { resetFilters(); searchQuery = ""; go(ProtoScreen.RetailFilter) },
                    onGroup = { g -> openProductList(type = g, target = ProtoScreen.RetailProductList) },
                    onOpenProduct = { p -> retailSelectedProduct = p; retailDetailQty = 1; go(ProtoScreen.RetailProductDetail) },
                    onSetQty = { p, q -> setRetailQty(p, q) },
                    onSeeAll = { mode -> openProductList(availability = mode.takeIf { it != "Все" }, target = ProtoScreen.RetailProductList) },
                    onOpenOrder = { selectedOrderId = it.id; go(ProtoScreen.RetailOrderDetail) },
                    onHome = rHome, onCatalog = rCatalog, onCart = rCart, onOrders = rOrders, onProfile = {},
                    retailMode = true, onRetailExit = exitRetail
                )
            }
            ProtoScreen.RetailCatalog -> ProtoCatalogHomeScreen(
                cartCount = retailCart.size,
                groupCounts = protoAllGroupsOrdered.associateWith { g -> retail.count { it.type == g } }.filterValues { it > 0 },
                onBack = { back() },
                onSearch = { q -> openProductList(query = q, focus = q.isBlank(), target = ProtoScreen.RetailProductList) },
                onGroup = { g -> openProductList(type = g, target = ProtoScreen.RetailProductList) },
                onAvailability = { st -> openProductList(availability = st.takeIf { it != "Все" }, target = ProtoScreen.RetailProductList) },
                onHome = rHome, onCart = rCart, onOrders = rOrders, onProfile = {},
                retailMode = true, onRetailExit = exitRetail
            )
            ProtoScreen.RetailFilter -> ProtoFilterScreen(
                products = retail.filter { it.type in protoWreathGroups },
                filter = currentFilter(),
                availableStock = retailStock,
                onApply = { f ->
                    applyFilter(f)
                    productListIndex = 0; productListOffset = 0
                    if (history.lastOrNull() == ProtoScreen.RetailProductList) back() else go(ProtoScreen.RetailProductList)
                },
                onBack = { back() }
            )
            ProtoScreen.RetailProductList -> {
                val f = currentFilter()
                ProtoProductListScreen(
                    products = retail.filter { protoMatches(it, f, retailStock(it)) },
                    cart = retailCart,
                    availableStock = retailStock,
                    query = searchQuery,
                    onQuery = { searchQuery = it; productListIndex = 0; productListOffset = 0 },
                    activeChips = activeFilterChips(),
                    sort = listSort,
                    onSort = { listSort = it },
                    initialIndex = productListIndex,
                    initialOffset = productListOffset,
                    focusSearch = focusSearch,
                    onBack = { focusSearch = false; back() },
                    onOpenFilter = { focusSearch = false; go(ProtoScreen.RetailFilter) },
                    onOpenProduct = { p, index, offset ->
                        productListIndex = index; productListOffset = offset; focusSearch = false
                        retailSelectedProduct = p; retailDetailQty = 1
                        go(ProtoScreen.RetailProductDetail)
                    },
                    onSetQty = { p, q -> setRetailQty(p, q) },
                    onHome = rHome, onCatalog = rCatalog, onCart = rCart, onOrders = rOrders, onProfile = {},
                    retailMode = true, onRetailExit = exitRetail
                )
            }
            ProtoScreen.RetailProductDetail -> {
                val p = retailSelectedProduct?.let { sel -> retail.firstOrNull { it.sku == sel.sku } ?: sel }
                ProtoProductDetailScreen(
                    product = p,
                    currentStock = p?.let { retailStock(it) } ?: 0,
                    qty = retailDetailQty,
                    discount = 0,
                    inCartQty = p?.let { retailCart[it.sku] } ?: 0,
                    editingCart = false,
                    onBack = { back() },
                    onMinus = { retailDetailQty = (retailDetailQty - 1).coerceAtLeast(1) },
                    onPlus = { retailDetailQty += 1 },
                    onAdd = { p?.let { retailCart[it.sku] = (retailCart[it.sku] ?: 0) + retailDetailQty; toast("Добавлено в корзину: ${retailDetailQty} шт."); retailDetailQty = 1 } },
                    onGoCart = rCart,
                    onHome = rHome, onCatalog = rCatalog, onCart = rCart, onOrders = rOrders, onProfile = {},
                    retailMode = true, onRetailExit = exitRetail
                )
            }
            ProtoScreen.RetailCart -> ProtoCartScreen(
                products = retail,
                cart = retailCart,
                discount = 0,
                availableStock = retailStock,
                onBack = { back() },
                onPlus = { p -> retailCart[p.sku] = (retailCart[p.sku] ?: 0) + 1 },
                onMinus = { p -> val n = (retailCart[p.sku] ?: 1) - 1; if (n <= 0) retailCart.remove(p.sku) else retailCart[p.sku] = n },
                onDelete = { retailCart.remove(it.sku) },
                onRestore = { p, q -> retailCart[p.sku] = q },
                onOpenProduct = { p -> retailSelectedProduct = p; retailDetailQty = 1; go(ProtoScreen.RetailProductDetail) },
                onCheckout = { if (orderingBlocked()) go(ProtoScreen.Suspended) else go(ProtoScreen.RetailCheckout) },
                onHome = rHome, onCatalog = rCatalog, onOrders = rOrders, onProfile = {},
                retailMode = true, onRetailExit = exitRetail
            )
            ProtoScreen.RetailCheckout -> {
                val cartLines = retailCart.mapNotNull { (sku, q) -> retail.firstOrNull { it.sku == sku }?.let { it to q } }
                val retailTotal = cartLines.sumOf { (rp, q) -> rp.price * q }
                val shortItems = cartLines.mapNotNull { (rp, q) -> if (q > retailStock(rp)) rp else null }
                val contact = SansaraCompanyContact(customer?.id ?: "RETAIL", customerName, customer?.phone.orEmpty(), customer?.email.orEmpty())
                ProtoCheckoutScreen(
                    cartPositions = cartLines.size,
                    totalPieces = cartLines.sumOf { it.second },
                    baseTotal = retailTotal,
                    finalTotal = retailTotal,
                    discount = 0,
                    defaultAddress = customer?.address.orEmpty(),
                    contacts = listOf(contact),
                    clientStatus = "Заказ для клиента: $customerName",
                    earliestDate = earliestDeliveryDate(shortItems),
                    shortages = shortItems.map { rp -> protoTitle(rp) + ": под заказ · " + protoReadyLabel(rp.productionDays) },
                    pickupPoints = pickupPoints(),
                    cancelKind = protoOrderKind(cartLines.map { (rp, q) -> ProtoOrderLine(rp.sku, rp.name, q, rp.price, stockQty = retailStock(rp).coerceAtMost(q)) }),
                    onBack = { back() },
                    header = {
                        LaunchedEffect(owner.id) { ownOps.clear(); ownOps.addAll(repository.ownStockOps(owner.id)) }
                        val external = ownExternal(owner.id)
                        val own = ownOps.filter { it.kind != OWN_EXTERNAL_SALE }.groupBy { it.sku }.mapValues { (_, l) -> l.sumOf { it.qty } }
                        ProtoSectionCard {
                            Text("Откуда отгружаем", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            ProtoChipRow(listOf("Склад SANSARA", "Мой склад"), if (retailWarehouse == "OWN") "Мой склад" else "Склад SANSARA") { retailWarehouse = if (it == "Мой склад") "OWN" else "SANSARA" }
                            if (retailWarehouse == "OWN") {
                                val short = cartLines.filter { (rp, q) -> q > (own[rp.sku] ?: 0) }
                                Text(when {
                                    external -> "Остатки ведутся в другой программе — продажа без проверки остатков."
                                    short.isEmpty() -> "На вашем складе всё есть."
                                    else -> "Не хватает на вашем складе: " + short.joinToString { (rp, q) -> protoTitle(rp) + " — " + (q - (own[rp.sku] ?: 0)) + " шт." } + ". Можно разделить заказ."
                                }, color = if (!external && short.isNotEmpty()) ProtoOrange else ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                            } else Text("Заказ уйдёт в SANSARA, сроки — как обычно.", color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    },
                    onSubmit = { method, address, comment, recipient, phone, date, time ->
                        // Продажа со своего склада: списываем с «Моего склада», в SANSARA ничего не уходит.
                        fun sellOwn(items: List<Pair<ProtoCatalogProduct, Int>>, externalSale: Boolean) = scope.launch {
                            val ref = "П-" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMHHmm"))
                            items.forEach { (rp, q) -> runCatching { repository.addOwnStockOp(owner.id, rp.sku, rp.name, -q, if (externalSale) OWN_EXTERNAL_SALE else "Продажа", customer = customerName, orderRef = ref, actor = actor(session?.firstName ?: owner.contact)) } }
                            ownOps.clear(); ownOps.addAll(repository.ownStockOps(owner.id))
                            items.forEach { (rp, q) -> retailCart[rp.sku]?.let { left -> if (left - q <= 0) retailCart.remove(rp.sku) else retailCart[rp.sku] = left - q } }
                            ownSaleDone = customerName + ": " + items.sumOf { it.second } + " шт. на " + protoMoney(items.sumOf { (rp, q) -> rp.price * q }) + " — со своего склада (" + ref + ")"
                        }
                        fun placeSansara(items: List<Pair<ProtoCatalogProduct, Int>>) = scope.launch {
                            val id = repository.nextOrderId()
                            val now = nowStamp()
                            // В заказ — закупочная цена агента (базовая цена со скидкой агента), розничная цена — справочно.
                            val lines = items.mapNotNull { (rp, q) -> baseOf(rp)?.let { base -> ProtoOrderLine(base.sku, base.name, q, base.price, owner.discount, retailPrice = rp.price, stockQty = availableStock(base).coerceAtMost(q)) } }
                            val order = ProtoOrder(
                                id = id,
                                clientName = owner.name + " / " + customerName,
                                dateTime = now,
                                lines = lines,
                                status = "Получен",
                                history = listOf(ProtoOrderEvent("Получен", now, owner.contact)),
                                deliveryMethod = method, deliveryAddress = address, comment = comment,
                                recipient = recipient, contactPhone = phone, deliveryDate = date, deliveryTime = time,
                                clientId = owner.id,
                                retailCustomerId = customer?.id.orEmpty(),
                                retailCustomerName = customerName
                            )
                            orders.add(0, order)
                            selectedOrderId = id
                            retailLastTotal = lines.sumOf { it.retailPrice * it.qty }
                            retailCart.clear()
                            persistAll()
                            sendOrderEvent(order)
                            addNotification("ADMIN", "ADMIN", "Новый заказ " + id, order.clientName + " · " + protoMoney(order.total), orderId = id)
                            history.clear(); history.add(ProtoScreen.RetailHome)
                            screen = ProtoScreen.RetailOrderSent
                        }
                        if (orderingBlocked() && retailWarehouse != "OWN") go(ProtoScreen.Suspended)
                        else if (retailWarehouse != "OWN") guarded("оформить заказ для клиента") { placeSansara(cartLines) }
                        else guarded("провести продажу со своего склада") {
                            val external = ownExternal(owner.id)
                            val own = ownOps.filter { it.kind != OWN_EXTERNAL_SALE }.groupBy { it.sku }.mapValues { (_, l) -> l.sumOf { it.qty } }
                            if (external) sellOwn(cartLines, true)
                            else {
                                val fromOwn = cartLines.mapNotNull { (rp, q) -> minOf(q, own[rp.sku] ?: 0).takeIf { it > 0 }?.let { rp to it } }
                                val rest = cartLines.mapNotNull { (rp, q) -> (q - (own[rp.sku] ?: 0)).takeIf { it > 0 }?.let { rp to it } }
                                if (rest.isEmpty()) sellOwn(cartLines, false)
                                else ownSplitPrompt = Triple(
                                    "Со своего склада: " + fromOwn.sumOf { it.second } + " шт.\nЗаказом в SANSARA: " + rest.joinToString { (rp, q) -> protoTitle(rp) + " — " + q + " шт." },
                                    { if (fromOwn.isNotEmpty()) sellOwn(fromOwn, false); placeSansara(rest) },
                                    { placeSansara(cartLines) }
                                )
                            }
                        }
                    },
                    onHome = rHome, onCatalog = rCatalog, onCart = rCart, onOrders = rOrders, onProfile = {},
                    retailMode = true, onRetailExit = exitRetail
                )
            }
            ProtoScreen.RetailOrderList -> ProtoOrderListScreen(
                orders = retailOrders.sortedByDescending { protoOrderEpoch(it.dateTime) }.map { asRetailView(it) },
                products = retail,
                onBack = { back() },
                onOpen = { selectedOrderId = it.id; go(ProtoScreen.RetailOrderDetail) },
                onHome = rHome, onCatalog = rCatalog, onCart = rCart, onProfile = {},
                retailMode = true, onRetailExit = exitRetail,
                cartCount = retailCart.size
            )
            ProtoScreen.RetailOrderDetail -> ProtoOrderDetailScreen(
                order = orders.firstOrNull { it.id == selectedOrderId }?.let { asRetailView(it) },
                products = retail,
                isAdmin = false,
                onBack = { back() },
                onStatus = {},
                onRepeat = {
                    orders.firstOrNull { it.id == selectedOrderId }?.let { order ->
                        retailCart.clear()
                        order.lines.forEach { line -> if (retail.any { it.sku == line.sku }) retailCart[line.sku] = line.qty }
                        go(ProtoScreen.RetailCheckout)
                    }
                },
                onEditRepeat = {
                    orders.firstOrNull { it.id == selectedOrderId }?.let { order ->
                        retailCart.clear()
                        order.lines.forEach { line -> if (retail.any { it.sku == line.sku }) retailCart[line.sku] = line.qty }
                        go(ProtoScreen.RetailCart)
                    }
                },
                onCancel = {
                    val idx = orders.indexOfFirst { it.id == selectedOrderId }
                    if (idx >= 0 && protoCancelState(orders[idx]).allowed) {
                        val old = orders[idx]
                        orders[idx] = old.copy(status = "Отменён", history = old.history + ProtoOrderEvent("Отменён", nowStamp(), owner.contact))
                        addNotification("ADMIN", "ADMIN", "Заказ " + old.id + " отменён клиентом", old.clientName, orderId = old.id)
                        persistAll()
                        toast("Заказ " + old.id + " отменён")
                    }
                },
                cartCount = retailCart.size
            )
            ProtoScreen.RetailNotifications -> {
                val ids = retailOrders.map { it.id }.toSet()
                val legacyKey = owner.name + " / " + customerName
                val mine: (ProtoNotification) -> Boolean = { n -> n.audienceRole == "CLIENT" && (n.audienceKey == legacyKey || (n.orderId.isNotBlank() && n.orderId in ids)) }
                ProtoNotificationsScreen(
                    notifications = notifications.filter(mine),
                    onBack = { back() },
                    onReadAll = {
                        var changed = false
                        notifications.indices.forEach { index ->
                            val n = notifications[index]
                            if (mine(n) && !n.read) { notifications[index] = n.copy(read = true); changed = true }
                        }
                        if (changed) persistNotifications()
                    },
                    onDelete = { id -> notifications.removeAll { it.id == id }; persistNotifications() },
                    onOpen = { n -> if (n.orderId in ids) { selectedOrderId = n.orderId; go(ProtoScreen.RetailOrderDetail) } }
                )
            }
            ProtoScreen.RetailOrderSent -> {
                val last = orders.firstOrNull { it.id == selectedOrderId }
                ProtoRetailOrderSentScreen(
                    orderId = last?.id.orEmpty(),
                    customerName = customerName,
                    total = retailLastTotal,
                    onContinue = { history.clear(); screen = ProtoScreen.RetailHome },
                    onOpenOrder = { go(ProtoScreen.RetailOrderDetail) },
                    onMyApp = { history.clear(); screen = ProtoScreen.Home }
                )
            }
            else -> Unit
        }
    }

    /** Нижняя навигация для экранов первого уровня сотрудников: тот же набор вкладок, что на главном экране роли. */
    @Composable fun roleBottomBar() {
        val role = session?.role
        val adminTabs = setOf(ProtoScreen.AdminClients, ProtoScreen.AdminOrders, ProtoScreen.AdminSettings)
        val productionTabs = setOf(ProtoScreen.ProductionHistory, ProtoScreen.ProductionProfile)
        val salesTabs = setOf(ProtoScreen.SalesHome, ProtoScreen.SalesShipments, ProtoScreen.SalesClients, ProtoScreen.SalesProfile)
        when {
            screen in adminTabs || (screen == ProtoScreen.Server && role != SansaraRole.PRODUCTION) -> ProtoAdminBottomBar(
                screen,
                onHome = { history.clear(); screen = ProtoScreen.AdminHome },
                onClients = { openTab(ProtoScreen.AdminHome, ProtoScreen.AdminClients) },
                onOrders = { openTab(ProtoScreen.AdminHome, ProtoScreen.AdminOrders) },
                onStock = { openTab(ProtoScreen.AdminHome, ProtoScreen.Server) },
                onProfile = { openTab(ProtoScreen.AdminHome, ProtoScreen.AdminSettings) }
            )
            screen in productionTabs || (screen == ProtoScreen.Server && role == SansaraRole.PRODUCTION) -> ProtoProductionBottomBar(
                screen,
                onRelease = { history.clear(); screen = ProtoScreen.Production },
                onHistory = { openTab(ProtoScreen.Production, ProtoScreen.ProductionHistory) },
                onStock = { openTab(ProtoScreen.Production, ProtoScreen.Server) },
                onProfile = { openTab(ProtoScreen.Production, ProtoScreen.ProductionProfile) }
            )
            screen in salesTabs -> ProtoSalesBottomBar(
                screen,
                onHome = { history.clear(); screen = ProtoScreen.SalesHome },
                onShipments = { refreshShipments(); openTab(ProtoScreen.SalesHome, ProtoScreen.SalesShipments) },
                onClients = { openTab(ProtoScreen.SalesHome, ProtoScreen.SalesClients) },
                onProfile = { openTab(ProtoScreen.SalesHome, ProtoScreen.SalesProfile) }
            )
            else -> Unit
        }
    }

    BackHandler(enabled = screen != ProtoScreen.Welcome) { back() }

    SansaraTheme {
        Box(Modifier.fillMaxSize().background(ProtoBg).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
        godEyeAdmin?.let { boss ->
            Surface(color = ProtoGold) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Visibility, null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    val who = session?.let { s -> s.clientId?.let { cid -> clients.firstOrNull { it.id == cid }?.name?.let { "$it · ${s.firstName}" } } ?: (if (s.role == SansaraRole.PRODUCTION) "Производство · ${s.firstName}" else s.firstName) }.orEmpty()
                    Text("Глаз бога: вы видите как $who", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        session = boss; godEyeAdmin = null
                        companyContacts.clear()
                        history.clear(); screen = startScreenForRole(boss.role)
                        toast("Вы вернулись в BOSS")
                    }) { Text("В BOSS", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Box(Modifier.weight(1f)) {
        when (screen) {
            ProtoScreen.Welcome -> ProtoWelcomeScreen(
                onLogin = {
                    val active = session
                    if (active != null) { history.clear(); screen = startScreenForRole(active.role) } else { loginError = null; go(ProtoScreen.Login) }
                },
                onRegister = { go(ProtoScreen.Registration) },
                onRole = { if (BuildConfig.DEBUG) showRolePicker = true },
                promoTitle = prefs.getString("welcome_promo_title", "").orEmpty(),
                promoText = prefs.getString("welcome_promo_text", "").orEmpty(),
                signedIn = session != null,
                promoImageVersion = welcomeImageVersion
            )
            ProtoScreen.Login -> {
                val lockedMessage = if (loginLockedUntil > System.currentTimeMillis())
                    "Слишком много неверных попыток. Повторите через " + ((loginLockedUntil - System.currentTimeMillis()) / 60_000 + 1) + " мин или позвоните менеджеру."
                else null
                ProtoLoginScreen(onBack = { back() }, lockedMessage = lockedMessage, error = loginError, onCodeChange = { loginError = null }, onLogin = { code ->
                    scope.launch {
                        val result = authProvider.signInWithAccessCode(code)
                        if (result.ok) {
                            loginError = null
                            prefs.edit().putInt("login_fails", 0).putLong("login_locked_until", 0L).apply()
                            session = result.session
                            result.session?.clientId?.let { clientId ->
                                selectedClientId = clientId
                                companyContacts.clear()
                                companyContacts.addAll(repository.contactsForClient(clientId))
                            }
                            history.clear()
                            screen = startScreenForRole(result.session!!.role)
                            onSignedIn()
                        } else {
                            val fails = prefs.getInt("login_fails", 0) + 1
                            if (fails >= 5) {
                                loginLockedUntil = System.currentTimeMillis() + 15 * 60_000L
                                prefs.edit().putInt("login_fails", 0).putLong("login_locked_until", loginLockedUntil).apply()
                                loginError = "Неверный код. Вход заблокирован на 15 минут."
                            } else {
                                prefs.edit().putInt("login_fails", fails).apply()
                                loginError = "Неверный код. Проверьте код из SMS. Осталось попыток: " + (5 - fails)
                            }
                        }
                    }
                })
            }
            ProtoScreen.Registration -> ProtoRegistrationScreen(onBack = { back() }, onSubmit = { reg ->
                // Запоминаем заявку этого телефона, чтобы показать результат проверки на приветственном экране.
                prefs.edit().putString("my_registration_id", reg.id).putString("my_registration_phone", reg.phone1).apply()
                registrations.add(0, reg)
                lastRegistration = reg
                registrationsTotalThisMonth += 1
                persistAll()
                sendRegistrationEvent(reg)
                addNotification("ADMIN", "ADMIN", "Новая заявка на регистрацию", reg.organization + " · " + reg.city + if (reg.monthlyVolume > 0) " · ≈" + protoMoney(reg.monthlyVolume.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) + "/мес" else "")
                history.clear(); history.add(ProtoScreen.Welcome)
                screen = ProtoScreen.RegistrationSent
            })
            ProtoScreen.RegistrationSent -> ProtoRegistrationSentScreen(lastRegistration,
                onBack = { history.clear(); screen = ProtoScreen.Welcome },
                onHaveCode = { history.clear(); history.add(ProtoScreen.Welcome); screen = ProtoScreen.Login })

            ProtoScreen.Home, ProtoScreen.Popular, ProtoScreen.Catalog, ProtoScreen.Filter, ProtoScreen.ProductList, ProtoScreen.ProductDetail, ProtoScreen.Cart, ProtoScreen.Checkout, ProtoScreen.OrderSent, ProtoScreen.OrderList, ProtoScreen.OrderDetail -> protoGroupA()
            ProtoScreen.Notifications, ProtoScreen.ClientChat, ProtoScreen.ClientReports, ProtoScreen.ClientSettings, ProtoScreen.Profile, ProtoScreen.Suspended -> protoGroupB()
            ProtoScreen.AdminHome, ProtoScreen.AdminSearch, ProtoScreen.AdminClients, ProtoScreen.AdminClient, ProtoScreen.AdminOrders, ProtoScreen.AdminOrderDetail, ProtoScreen.AdminChats, ProtoScreen.AdminChat, ProtoScreen.AdminProductionChat, ProtoScreen.AdminNotifications, ProtoScreen.AdminCatalog, ProtoScreen.AdminSettings, ProtoScreen.AdminSettingsDetail, ProtoScreen.AdminAttention, ProtoScreen.OnlineController, ProtoScreen.LowStockList, ProtoScreen.AdminAssemblers, ProtoScreen.AdminAdmins, ProtoScreen.AdminWorkshop, ProtoScreen.AdminAttendance, ProtoScreen.AdminProduct -> protoGroupC()
            ProtoScreen.SalesHome, ProtoScreen.SalesShipmentNew, ProtoScreen.SalesShipments, ProtoScreen.AdminPayments, ProtoScreen.SalesClients, ProtoScreen.SalesClient, ProtoScreen.AdminReports, ProtoScreen.AdminDashboard, ProtoScreen.SalesProfile -> protoGroupD()
            ProtoScreen.Production, ProtoScreen.ProductionCategory, ProtoScreen.ProductionCatalog, ProtoScreen.ProductionEntry, ProtoScreen.ProductionHistory, ProtoScreen.ProductionReport, ProtoScreen.ProductionPayments, ProtoScreen.ProductionProfile, ProtoScreen.ProductionWorkshop, ProtoScreen.ProductionAttendance, ProtoScreen.ProductionChat, ProtoScreen.Server, ProtoScreen.StockList, ProtoScreen.ReserveList, ProtoScreen.NewClients, ProtoScreen.Export, ProtoScreen.AgentClients, ProtoScreen.AgentClientDetail, ProtoScreen.AgentBase, ProtoScreen.AgentMarkup, ProtoScreen.AgentProductMarkup, ProtoScreen.AgentOrders, ProtoScreen.OwnStock -> protoGroupE()
            ProtoScreen.RetailHome, ProtoScreen.RetailCatalog, ProtoScreen.RetailFilter, ProtoScreen.RetailProductList, ProtoScreen.RetailProductDetail, ProtoScreen.RetailCart, ProtoScreen.RetailCheckout, ProtoScreen.RetailOrderList, ProtoScreen.RetailOrderDetail, ProtoScreen.RetailNotifications, ProtoScreen.RetailOrderSent -> protoGroupF()
        }
        }
        roleBottomBar()
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
                onOwnStock = { showClientMenu=false; go(ProtoScreen.OwnStock) },
                onReports = { showClientMenu=false; go(ProtoScreen.ClientReports) },
                onSettings = { showClientMenu=false; go(ProtoScreen.ClientSettings) },
                onUpdate = { showClientMenu=false; showUpdateDialog = true },
                updateAvailable = remember(updateVersion) { SansaraUpdates.available(context) != null }
            )
        }

        approvedCodes?.let { codes ->
            AlertDialog(
                onDismissRequest = {}, containerColor = ProtoPanel,
                title = { Text("Клиент создан — отправьте коды", color = ProtoText) },
                text = {
                    Column {
                        Text("Каждому контакту — свой код. Нажмите «SMS»: откроется SMS-приложение телефона с готовым текстом, останется нажать «Отправить».", color = ProtoMuted, fontSize = 13.sp)
                        codes.forEach { (name, phone, code) ->
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(protoPhone(phone) + " · код " + code, color = ProtoGoldSoft, fontSize = 13.sp)
                                }
                                TextButton(onClick = { protoSms(context, phone, "SANSARA: ваша заявка одобрена. Ваш код доступа: $code. Откройте приложение и нажмите «Войти»."); smsSent.add(code) }) {
                                    Text(if (code in smsSent) "SMS ✓" else "SMS", color = ProtoGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { approvedCodes = null; smsSent.clear() }) { Text("Готово", color = ProtoGold) } }
            )
        }

        if (showUpdateDialog) ProtoUpdateDialog(onDismiss = { showUpdateDialog = false })
        ownSplitPrompt?.let { (text, split, allSansara) ->
            AlertDialog(
                onDismissRequest = { ownSplitPrompt = null }, containerColor = ProtoPanel,
                title = { Text("На вашем складе не хватает", color = ProtoText) },
                text = { Text(text + "\n\nРазделить заказ: что есть — продать со своего склада, недостающее — заказать в SANSARA?", color = ProtoMuted, lineHeight = 20.sp) },
                confirmButton = { TextButton(onClick = { ownSplitPrompt = null; split() }) { Text("Разделить", color = ProtoGold) } },
                dismissButton = { Row { TextButton(onClick = { ownSplitPrompt = null; allSansara() }) { Text("Всё в SANSARA", color = ProtoGoldSoft) }; TextButton(onClick = { ownSplitPrompt = null }) { Text("Отмена", color = ProtoMuted) } } }
            )
        }
        ownSaleDone?.let { text ->
            AlertDialog(
                onDismissRequest = { ownSaleDone = null }, containerColor = ProtoPanel,
                title = { Text("Продажа проведена", color = ProtoText) },
                text = { Text(text + ". Списано с «Моего склада».", color = ProtoMuted, lineHeight = 20.sp) },
                confirmButton = { TextButton(onClick = { ownSaleDone = null; if (screen == ProtoScreen.RetailCheckout) { history.clear(); screen = ProtoScreen.RetailHome } }) { Text("Готово", color = ProtoGold) } }
            )
        }
        when (adminDialog) {
            "Рассылка" -> ProtoBroadcastDialog(
                clients = clients,
                history = remember(adminDialog, notifications.size) { protoLoadBroadcasts(prefs) },
                onDismiss = { adminDialog = null },
                onSend = { audience, title, text, recipients ->
                    recipients.forEach { c -> addNotification("CLIENT", c.id, title, text, system = false) }
                    // Одно системное уведомление на этот телефон вместо десятков одинаковых.
                    SansaraNotify.post(context, title, text, channel = SansaraNotify.Channel.OTHER)
                    protoSaveBroadcast(prefs, ProtoBroadcast(nowStamp(), audience, title, text, recipients.size))
                    toast("Рассылка отправлена: " + recipients.size)
                }
            )
            "Обновление" -> ProtoPublishUpdateDialog(
                current = remember(updateVersion) { SansaraUpdates.published(context) },
                onDismiss = { adminDialog = null },
                onPublish = { uri, notes ->
                    scope.launch {
                        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { SansaraUpdates.publish(context, uri, notes) }
                        result.onSuccess { info ->
                            updateVersion++
                            adminDialog = null
                            val text = "Версия " + info.versionName + (if (info.notes.isNotBlank()) ". " + info.notes else "") + ". Откройте меню → «Обновление»."
                            clients.forEach { c -> addNotification("CLIENT", c.id, "Доступно обновление SANSARA", text, system = false) }
                            SansaraNotify.post(context, "Доступно обновление SANSARA", text, channel = SansaraNotify.Channel.OTHER)
                            protoSaveBroadcast(prefs, ProtoBroadcast(nowStamp(), "Обновление", "Доступна версия " + info.versionName, info.notes, clients.size))
                            toast("Опубликована версия " + info.versionName)
                        }.onFailure { toast(it.message ?: "Не удалось опубликовать") }
                    }
                },
                onUnpublish = { SansaraUpdates.unpublish(context); updateVersion++; toast("Публикация снята") }
            )
        }
        godEyeConfirm?.let { (what, action) ->
            AlertDialog(
                onDismissRequest = { godEyeConfirm = null }, containerColor = ProtoPanel,
                title = { Text("Глаз бога: изменить данные?", color = ProtoText) },
                text = { Text("Вы действуете от имени «" + (session?.firstName ?: "") + "». Точно " + what + "? Действие будет записано как сделанное BOSS.", color = ProtoMuted, lineHeight = 20.sp) },
                confirmButton = { TextButton(onClick = { godEyeConfirm = null; action() }) { Text("Да, изменить", color = ProtoGold) } },
                dismissButton = { TextButton(onClick = { godEyeConfirm = null }) { Text("Отмена", color = ProtoMuted) } }
            )
        }
        godEyePicker?.let { targets ->
            ProtoGodEyePicker(
                targets = targets,
                clientName = { cid -> clients.firstOrNull { it.id == cid }?.name.orEmpty() },
                onDismiss = { godEyePicker = null },
                onPick = { target ->
                    godEyePicker = null
                    val boss = session ?: return@ProtoGodEyePicker
                    godEyeAdmin = boss
                    session = target
                    target.clientId?.let { cid ->
                        selectedClientId = cid
                        scope.launch { companyContacts.clear(); companyContacts.addAll(repository.contactsForClient(cid)) }
                    }
                    history.clear(); screen = startScreenForRole(target.role)
                }
            )
        }
        belowMinPrompt?.let { (sum, minimum, send) ->
            AlertDialog(
                onDismissRequest = {}, containerColor = ProtoPanel,
                title = { Text("Сумма заказа ниже минимальной", color = ProtoText) },
                text = { Text("Минимальная сумма заказа для торгующих организаций — " + protoMoney(minimum) + ". В вашем заказе — " + protoMoney(sum) + ".\n\nСумма не соответствует условиям работы с компанией SANSARA. Вы можете дополнить заказ или отправить его администратору на согласование.", color = ProtoMuted, lineHeight = 20.sp) },
                confirmButton = { TextButton(onClick = { belowMinPrompt = null; send() }) { Text("Отправить на согласование", color = ProtoGold) } },
                dismissButton = { TextButton(onClick = { belowMinPrompt = null; history.clear(); history.add(ProtoScreen.Home); screen = ProtoScreen.Catalog }) { Text("Дополнить заказ", color = ProtoMuted) } }
            )
        }
        // Открытие из системного уведомления: нужный заказ или список уведомлений (если пользователь вошёл).
        val pendingOpen = (context as? MainActivity)?.pendingOpen?.value
        LaunchedEffect(pendingOpen, session, dataReady) {
            val target = pendingOpen ?: return@LaunchedEffect
            val active = session ?: return@LaunchedEffect
            if (!dataReady) return@LaunchedEffect
            (context as? MainActivity)?.pendingOpen?.value = null
            history.clear(); history.add(startScreenForRole(active.role))
            val admin = active.role == SansaraRole.ADMIN || active.role == SansaraRole.SALES
            if (target.isNotBlank() && orders.any { it.id == target }) {
                selectedOrderId = target
                screen = if (admin) ProtoScreen.AdminOrderDetail else ProtoScreen.OrderDetail
            } else screen = if (admin) ProtoScreen.AdminNotifications else if (active.role == SansaraRole.PRODUCTION) startScreenForRole(active.role) else ProtoScreen.Notifications
        }
        crashReport?.let { report ->
            AlertDialog(
                onDismissRequest = { SansaraCrashLog.markSeen(context); crashReport = null }, containerColor = ProtoPanel,
                title = { Text("Приложение было закрыто из-за ошибки", color = ProtoText) },
                text = { Text("Мы записали причину. Отправьте отчёт разработчику — это поможет быстрее исправить ошибку. Ваши данные не пострадали.", color = ProtoMuted, lineHeight = 20.sp) },
                confirmButton = { TextButton(onClick = { protoShare(context, report, "Отправить отчёт об ошибке"); SansaraCrashLog.markSeen(context); crashReport = null }) { Text("Отправить отчёт", color = ProtoGold) } },
                dismissButton = { TextButton(onClick = { SansaraCrashLog.markSeen(context); crashReport = null }) { Text("Закрыть", color = ProtoMuted) } }
            )
        }

        if (adminSetupNeeded && screen == ProtoScreen.Welcome) {
            var c1 by remember { mutableStateOf("") }
            var c2 by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = {}, containerColor = ProtoPanel,
                title = { Text("Первый запуск", color = ProtoText) },
                text = {
                    Column {
                        Text("На этом телефоне ещё нет администратора. Придумайте код главного администратора — 6 букв или цифр. Его нельзя будет посмотреть повторно, запишите его.", color = ProtoMuted, lineHeight = 20.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(c1, { v -> c1 = v.filter { it.isLetterOrDigit() }.uppercase().take(6); adminSetupError = null }, label = { Text("Код администратора") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(c2, { v -> c2 = v.filter { it.isLetterOrDigit() }.uppercase().take(6); adminSetupError = null }, label = { Text("Повторите код") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth(),
                            isError = adminSetupError != null, supportingText = { adminSetupError?.let { Text(it, color = ProtoRed) } })
                    }
                },
                confirmButton = { TextButton(enabled = c1.length == 6, onClick = {
                    if (c1 != c2) { adminSetupError = "Коды не совпадают"; return@TextButton }
                    scope.launch {
                        runCatching { repository.createMainAdmin(c1) }
                            .onSuccess { adminSetupNeeded = false; applySnapshot(repository.snapshot()); toast("Администратор создан. Войдите с этим кодом") }
                            .onFailure { adminSetupError = it.message ?: "Не удалось сохранить код" }
                    }
                }) { Text("Сохранить", color = ProtoGold) } },
                dismissButton = { TextButton(onClick = { adminSetupNeeded = false }) { Text("Позже", color = ProtoMuted) } }
            )
        }

        rejectionNotice?.let { regId ->
            AlertDialog(
                onDismissRequest = {}, containerColor = ProtoPanel,
                title = { Text("Заявка не одобрена", color = ProtoText) },
                text = { Text(PROTO_REJECT_TEXT, color = ProtoMuted, lineHeight = 20.sp) },
                confirmButton = { TextButton(onClick = { prefs.edit().putBoolean("reject_seen_$regId", true).apply(); rejectionNotice = null }) { Text("Понятно", color = ProtoGold) } },
                dismissButton = { TextButton(onClick = { protoDial(context) }) { Text("Позвонить администратору", color = ProtoGold) } }
            )
        }

        if (showAgentNewCustomer) {
            ProtoAgentCustomerDialog(
                onDismiss = { showAgentNewCustomer = false },
                onSave = { n, ph, e, city, a, comment ->
                    showAgentNewCustomer = false
                    val ownerId = currentClient().id
                    scope.launch {
                        runCatching { repository.saveAgentCustomer(null, ownerId, n, ph, e, city, a, comment) }
                            .onSuccess { cust -> selectedAgentCustomerId = cust.id; refreshAgentData(); toast("Клиент сохранён — он в списке «Новые клиенты»"); go(ProtoScreen.AgentClientDetail) }
                            .onFailure { toast(it.message ?: "Ошибка сохранения") }
                    }
                }
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
                        TextButton(onClick = { history.clear(); screen = ProtoScreen.SalesHome; showRolePicker = false }) { Text("Продажи", color = ProtoGold) }
                    }
                }
            )
        }
        }
    }
}

@Composable
internal fun ProtoLoadingState() {
    Box(Modifier.fillMaxSize().background(ProtoBg),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            CircularProgressIndicator(color=ProtoGold,strokeWidth=2.dp,modifier=Modifier.size(34.dp))
            Spacer(Modifier.height(12.dp))
            Text("Загрузка SANSARA…",color=ProtoMuted,fontSize=13.sp)
        }
    }
}

@Composable
internal fun ProtoLiveBackground() {
    // По правкам заказчика: без фоновых картинок и узоров — ровный тёмный фон.
    Box(Modifier.fillMaxSize().background(ProtoBg))
}

@Composable
internal fun ProtoSearchBar(text:String,onClick:()->Unit,placeholder:String="Поиск по артикулу, названию") {
    Surface(color=ProtoPanel,border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth().height(48.dp).clickable{onClick()}){
        Row(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Outlined.Search,null,tint=ProtoGold,modifier=Modifier.size(25.dp));Spacer(Modifier.width(11.dp))
            Text(if(text.isBlank())placeholder else text,color=if(text.isBlank())ProtoMuted else ProtoText,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun ProtoAvailabilityChips(selected:String,onSelect:(String)->Unit){
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
internal fun ProtoPrimaryButton(text:String,onClick:()->Unit,enabled:Boolean=true,modifier:Modifier=Modifier){
    Button(onClick=onClick,enabled=enabled,modifier=modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(28.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold,contentColor=Color.Black,disabledContainerColor=ProtoPanel2,disabledContentColor=ProtoMuted)){Text(text,fontWeight=FontWeight.Bold,fontSize=15.sp)}
}

@Composable
internal fun ProtoSecondaryButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier){
    OutlinedButton(onClick=onClick,modifier=modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(27.dp),border=BorderStroke(1.dp,ProtoGold)){Text(text,color=ProtoGold,fontWeight=FontWeight.SemiBold)}
}

@Composable
internal fun ProtoWelcomeScreen(onLogin:()->Unit,onRegister:()->Unit,onRole:()->Unit,promoTitle:String="УВАЖЕНИЕ\nВ КАЖДОЙ\nДЕТАЛИ",promoText:String="",signedIn:Boolean=false,promoImageVersion:Long=0L) {
    val context=LocalContext.current
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){
        Spacer(Modifier.height(18.dp));Image(painterResource(R.drawable.sansara_wordmark_large),"SANSARA",Modifier.fillMaxWidth().height(96.dp).padding(horizontal=6.dp).clickable{onRole()},contentScale=ContentScale.Fit);Spacer(Modifier.height(14.dp))
        Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth().weight(1f)){Box(Modifier.fillMaxSize()){val promoFile=remember(promoImageVersion){protoWelcomeImageFile(context).takeIf{promoImageVersion>0L&&it.exists()}};if(promoFile!=null)AsyncImage(model=coil.request.ImageRequest.Builder(context).data(promoFile).memoryCacheKey("welcome-$promoImageVersion").diskCacheKey("welcome-$promoImageVersion").build(),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop,error=painterResource(R.drawable.mock_wreath)) else Image(painterResource(R.drawable.mock_wreath),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.38f)));Column(Modifier.align(Alignment.CenterStart).padding(24.dp)){Text(promoTitle.ifBlank{"УВАЖЕНИЕ\nВ КАЖДОЙ\nДЕТАЛИ"},color=ProtoGoldSoft,fontSize=22.sp,lineHeight=32.sp,fontWeight=FontWeight.Medium,letterSpacing=3.sp);if(promoText.isNotBlank())Text(promoText,color=ProtoText,fontSize=15.sp,lineHeight=21.sp,modifier=Modifier.padding(top=12.dp))}}}
        Spacer(Modifier.height(18.dp));Text("Добро пожаловать",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold)
        Text("Работаем с агентами и торговыми организациями. Заказывайте продукцию, отслеживайте наличие и оформляйте поставки в одном приложении.",color=ProtoMuted,fontSize=14.sp,lineHeight=20.sp,modifier=Modifier.padding(top=10.dp,bottom=18.dp))
        ProtoPrimaryButton(if(signedIn)"Войти в кабинет" else "Войти",onLogin);Spacer(Modifier.height(10.dp));ProtoSecondaryButton("Стать партнёром",onRegister)
        TextButton(onClick={protoDial(context)},modifier=Modifier.align(Alignment.CenterHorizontally).padding(vertical=10.dp)){Icon(Icons.Outlined.HeadsetMic,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Связаться с менеджером",color=ProtoGoldSoft)};Spacer(Modifier.height(10.dp))
    }}
}

@Composable
internal fun ProtoOrderListScreen(
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
    cartCount:Int=0,
    clientOrders: List<ProtoOrder>? = null,
    onOpenClientOrder: (ProtoOrder)->Unit = {}
) {
    var tab by remember { mutableStateOf("Мои заказы") }
    var search by remember { mutableStateOf("") }
    val qn = search.trim()
    fun matches(o: ProtoOrder) = qn.isBlank() || o.id.contains(qn, true) || o.id.filter(Char::isDigit).endsWith(qn.filter(Char::isDigit).ifBlank { "§" }) || o.retailCustomerName.contains(qn, true)
    var historyRange by remember { mutableStateOf<ProtoPeriod?>(null) }
    var expandedMonths by remember { mutableStateOf(setOf<String>()) }

    val currentOrders=orders.filter { it.status !in protoHistoryStatuses && matches(it) }
    val allHistory=orders.filter { it.status in protoHistoryStatuses && matches(it) }
    val historyTotal=orders.count { it.status in protoHistoryStatuses }
    val clientList=clientOrders.orEmpty().filter { matches(it) }.sortedByDescending { protoOrderEpoch(it.dateTime) }
    val history=allHistory.filter { historyRange == null || historyRange!!.containsText(it.dateTime) }
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
                ProtoTopBar(
                    title = if (retailMode) "Заказы клиента" else "Мои заказы",
                    subtitle = "Текущих: " + currentOrders.size + " · в истории: " + allHistory.size,
                    onBack = onBack,
                    action = null
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { ProtoSearchField(search, { search = it }, {}, "Номер заказа, например 2384") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (if (clientOrders != null) listOf("Мои заказы", "Клиентские", "История") else listOf("Мои заказы", "История")).forEach { label ->
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
                    if(tab=="Клиентские"){
                        if(clientList.isEmpty()) item{ProtoEmptyState(Icons.Outlined.People,"Клиентских заказов пока нет","Сюда попадают заказы, оформленные в разделе «Продажи»")}
                        else items(clientList,key={"c-"+it.id}){order->ProtoOrderRow(order.copy(clientName=order.retailCustomerName.ifBlank{order.clientName})){onOpenClientOrder(order)}}
                    }else if(tab=="Мои заказы"){
                        if(currentOrders.isEmpty()){
                            item{
                                when {
                                    qn.isNotBlank() -> ProtoSectionCard{Text("Заказ с номером «$qn» не найден",color=ProtoMuted)}
                                    historyTotal > 0 -> ProtoEmptyState(Icons.Outlined.History,"Активных заказов нет","Все доставленные заказы попадают в раздел «История».",actionLabel="Перейти в историю заказов",onAction={ tab="История" })
                                    else -> ProtoEmptyState(Icons.Outlined.ReceiptLong,"Заказов пока нет","Выберите товары в каталоге и оформите первый заказ.",actionLabel="Перейти в каталог",onAction=onCatalog)
                                }
                            }
                        }else{
                            items(currentOrders,key={it.id}){order->ProtoOrderRow(order){onOpen(order)}}
                        }
                    }else{
                        item{ProtoPeriodBar(historyRange,{historyRange=it},allowAll=true,maxDate=LocalDate.now())}
                        if(grouped.isEmpty()){
                            item{ProtoSectionCard{Text(if(historyRange!=null)"За выбранный период заказов нет" else "Завершённых заказов пока нет",color=ProtoMuted)}}
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
                                                Text(monthOrders.size.toString()+" заказов · "+protoMoney(monthOrders.sumOf{it.total}),color=ProtoMuted,fontSize=12.sp)
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
}

@Composable
internal fun ProtoOrderHistoryFilterDialog(
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
internal fun ProtoProfileScreen(
    client:ProtoClient,
    onBack:()->Unit,
    onCall:()->Unit,
    onLogout:()->Unit,
    onHome:()->Unit,
    onCatalog:()->Unit,
    onCart:()->Unit,
    onOrders:()->Unit,
    onAnalytics:()->Unit={}
){
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Scaffold(
            containerColor=Color.Transparent,
            bottomBar={ProtoClientBottomBar(ProtoScreen.Profile,0,onHome,onCatalog,onCart,onOrders,onProfile={})}
        ){pad->
            Column(Modifier.fillMaxSize().padding(pad)){
                ProtoTopBar("Профиль",subtitle="Компания, контакты и расчёты",onBack=onBack)
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
                            ProtoInfoRow("Телефон",protoPhone(client.phone))
                            ProtoInfoRow("Администратор",protoPhone(BuildConfig.ADMIN_PHONE))
                        }
                    }
                    item{ProtoPrimaryButton("Моя аналитика и расчёты",onAnalytics)}
                    item{ProtoSecondaryButton("Позвонить администратору",onCall)}
                    item{TextButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Выйти",color=ProtoMuted)}}
                }
            }
        }
    }
}

@Composable
internal fun ProtoSuspendedScreen(onCall:()->Unit,onMessage:()->Unit,onBack:()->Unit,onCatalog:()->Unit,onHome:()->Unit,onOrders:()->Unit){
    Box(Modifier.fillMaxSize()){ProtoLiveBackground();Scaffold(containerColor=Color.Transparent,bottomBar={ProtoClientBottomBar(ProtoScreen.Suspended,0,onHome,onCatalog,onCart={},onOrders=onOrders,onProfile={})}){pad->Column(Modifier.fillMaxSize().padding(pad)){ProtoTopBar("Профиль",onBack=onBack);Column(Modifier.padding(horizontal=16.dp)){ProtoSectionCard{Text("Ваш статус временно приостановлен.",color=ProtoRed,fontSize=24.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Text("Самостоятельное оформление заказов недоступно. Пожалуйста, свяжитесь с администратором.",color=ProtoMuted,lineHeight=20.sp)};Spacer(Modifier.height(12.dp));ProtoSectionCard{ProtoInfoRow("Телефон администратора",protoPhone(BuildConfig.ADMIN_PHONE))};Spacer(Modifier.height(12.dp));ProtoPrimaryButton("Позвонить администратору",onCall);Spacer(Modifier.height(10.dp));ProtoSecondaryButton("Написать менеджеру",onMessage)}}}}
}

@Composable
internal fun ProtoClientMenuOverlay(
    onDismiss:()->Unit,
    onChat:()->Unit,
    onProfile:()->Unit,
    onCustomers:()->Unit,
    onReports:()->Unit,
    onSettings:()->Unit,
    onUpdate:()->Unit={},
    updateAvailable:Boolean=false,
    onOwnStock:()->Unit={}
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
                    Triple("Продажи",Icons.Outlined.Storefront,onCustomers),
                    Triple("Мой склад",Icons.Outlined.Warehouse,onOwnStock),
                    Triple("Формирование отчётности",Icons.Outlined.Assessment,onReports),
                    Triple("Настройки",Icons.Outlined.Settings,onSettings),
                    Triple(if(updateAvailable)"Обновление · доступно" else "Обновление",Icons.Outlined.SystemUpdate,onUpdate)
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
                Text("SANSARA · ${BuildConfig.VERSION_NAME}",color=ProtoMuted,fontSize=12.sp)
            }
        }
    }
}

@Composable
internal fun ProtoClientSettingsScreen(notificationsEnabled:Boolean,onNotifications:(Boolean)->Unit,onBack:()->Unit){
    ProtoScaffold("Настройки","Параметры клиентского приложения",onBack){
        item{ProtoSectionCard{ProtoSwitchRow("Уведомления о заказах",notificationsEnabled,onNotifications);Text("Уведомления о смене статуса отображаются на колокольчике главного экрана.",color=ProtoMuted,fontSize=12.sp)}}
        item{ProtoSectionCard{ProtoInfoRow("Версия",BuildConfig.VERSION_NAME);ProtoInfoRow("Режим","Клиент")}}
    }
}

@Composable
internal fun ProtoAgentCustomerDialog(onDismiss:()->Unit,onSave:(String,String,String,String,String,String)->Unit){
    var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var comment by remember{mutableStateOf("")}
    Dialog(onDismissRequest=onDismiss){
        Surface(color=ProtoPanel,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,ProtoBorder)){
            Column(Modifier.padding(16.dp).heightIn(max=620.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text("Новый клиент",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=onDismiss){Icon(Icons.Outlined.Close,null,tint=ProtoGold)}}
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
internal fun ProtoAgentClientDetailScreen(
    customer:SansaraAgentCustomer?,
    markups:List<SansaraAgentMarkup>,
    reminders:List<SansaraAgentReminder>,
    onBack:()->Unit,
    onSaveComment:(String)->Unit,
    onMarkup:(String,Int)->Unit,
    onReminder:(Long,String)->Unit,
    onRetail:()->Unit,
    discount:Int=0,
    onDiscount:(Int)->Unit={},
    ordersCount:Int=0,
    onOrders:()->Unit={}
){
    val haptic=androidx.compose.ui.platform.LocalHapticFeedback.current
    val c=customer
    if(c==null){ProtoSimpleMessageScreen("Клиент","Клиент не найден",onBack);return}
    val context=LocalContext.current
    var comment by remember(c.id,c.comment){mutableStateOf(c.comment)}
    var reminderDateTime by remember{mutableStateOf(LocalDateTime.now().plusHours(2))}
    var reminderNote by remember{mutableStateOf("")}
    val categories=protoWreathGroupsOrdered
    ProtoScaffold(c.fullName,"Карточка частного клиента",onBack){
        item{ProtoSectionCard{ProtoInfoRow("Телефон",protoPhone(c.phone));if(c.email.isNotBlank())ProtoInfoRow("E-mail",c.email);if(c.city.isNotBlank())ProtoInfoRow("Город",c.city);if(c.address.isNotBlank())ProtoInfoRow("Адрес",c.address);Row(Modifier.fillMaxWidth().clickable{onOrders()}.padding(vertical=4.dp)){Text("Заказы клиента",color=ProtoMuted);Spacer(Modifier.weight(1f));Text(ordersCount.toString()+" ›",color=ProtoGold,fontWeight=FontWeight.SemiBold)}}}
        item{ProtoPrimaryButton("Перейти к заказу",onRetail)}
        item{
            ProtoSectionCard{
                Text("Скидка клиенту",color=ProtoGoldSoft,fontSize=17.sp,fontWeight=FontWeight.Bold)
                Text("Применяется к цене с наценкой во всём каталоге этого клиента.",color=ProtoMuted,fontSize=12.sp)
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
                    ProtoQtyButton(Icons.Outlined.Remove,{onDiscount((discount-1).coerceAtLeast(0));haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)},46.dp)
                    Text("$discount%",color=if(discount>0)ProtoGold else ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.width(100.dp))
                    ProtoQtyButton(Icons.Outlined.Add,{onDiscount((discount+1).coerceAtMost(90));haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)},46.dp)
                }
            }
        }
        item{OutlinedTextField(value=comment,onValueChange={comment=it},label={Text("Комментарий")},modifier=Modifier.fillMaxWidth(),minLines=3,maxLines=5,colors=protoFieldColors());Spacer(Modifier.height(8.dp));ProtoPrimaryButton("Сохранить комментарий",{onSaveComment(comment)})}
        item{Text("Изменить наценку индивидуально",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Пусто или 0 — действует наценка на позицию или общая.",color=ProtoMuted,fontSize=12.sp)}
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
                reminders.filter{it.active}.take(3).forEach{r->Text("• "+protoReminderTime(r.remindAtEpochMs)+" · "+r.note,color=ProtoMuted,fontSize=12.sp,modifier=Modifier.padding(top=4.dp))}
            }
        }
    }
}

internal fun protoReminderTime(epoch:Long):String=
    Instant.ofEpochMilli(epoch).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

internal fun protoWorkshopPlanQty(linesJson:String):Int=runCatching{
    val a=JSONArray(linesJson)
    (0 until a.length()).sumOf{i->a.getJSONObject(i).optInt("qty",0)}
}.getOrDefault(0)

internal fun protoWorkshopLinesLabel(linesJson:String):String=runCatching{
    val a=JSONArray(linesJson)
    (0 until a.length()).joinToString("\n"){i->
        val o=a.getJSONObject(i)
        val name=o.optString("name")
        if(name.isNotBlank())name+" ("+o.optString("sku")+") — "+o.optInt("qty",0)+" шт."
        else o.optString("category","Позиция")+" — "+o.optInt("qty",0)+" шт."
    }
}.getOrDefault(linesJson.ifBlank{"Без позиций"})

@Composable
internal fun ProtoAttendanceScreen(
    selectedDate:LocalDate,
    assemblers:List<SansaraAssembler>,
    rows:List<AttendanceEntity>,
    editable:Boolean,
    onBack:()->Unit,
    onDate:(LocalDate)->Unit,
    onSave:(SansaraAssembler,String,String)->Unit
){
    var showCalendar by remember{mutableStateOf(false)}
    // «День» — отметки за выбранный день; «Период» — итоги по каждому сотруднику за выбранные дни/месяцы.
    var mode by remember{mutableStateOf("День")}
    var period by remember{mutableStateOf(ProtoPeriod.thisMonth())}
    var expandedPerson by remember{mutableStateOf<String?>(null)}
    val dateKey=selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val dayRows=rows.filter{it.date==dateKey}.associateBy{it.personId}
    ProtoScaffold("Табель рабочего времени",if(mode=="День")selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy",ruLocale)) else period.label,onBack){
        item{ProtoChipRow(listOf("День","Период"),mode){mode=it}}
        if(mode=="Период"){
            item{ProtoPeriodBar(period,{period=it?:ProtoPeriod.thisMonth()},maxDate=LocalDate.now())}
            val inPeriod=rows.filter{period.containsText(it.date)}
            val days=(java.time.temporal.ChronoUnit.DAYS.between(period.from,period.to)+1).toInt()
            item{
                ProtoSectionCard{
                    ProtoInfoRow("Дней в периоде",days.toString())
                    ProtoInfoRow("Выходов всего",inPeriod.count{it.status=="Работал"}.toString())
                    ProtoInfoRow("Выходных отмечено",inPeriod.count{it.status=="Выходной"}.toString())
                }
            }
            items(assemblers,key={"p-"+it.id}){person->
                val mine=inPeriod.filter{it.personId==person.id}.sortedBy{protoParseDate(it.date)}
                val worked=mine.count{it.status=="Работал"}
                val off=mine.count{it.status=="Выходной"}
                val open=expandedPerson==person.id
                ProtoSectionCard(Modifier.clickable{expandedPerson=if(open)null else person.id}){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(person.name+if(!person.enabled)" (отключена)" else "",color=ProtoText,fontWeight=FontWeight.Bold)
                            Text("Работала: $worked · выходных: $off · не отмечено: ${(days-mine.size).coerceAtLeast(0)}",color=ProtoMuted,fontSize=12.sp)
                        }
                        Icon(if(open)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null,tint=ProtoGold)
                    }
                    if(open){
                        if(mine.isEmpty())Text("За период отметок нет",color=ProtoMuted,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))
                        mine.forEach{r->Text(r.date+" · "+r.status+if(r.comment.isNotBlank())" · "+r.comment else "",color=if(r.status=="Работал")ProtoText else ProtoGoldSoft,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))}
                    }
                }
            }
        }else{
        item{ProtoSecondaryButton("Выбрать дату",{showCalendar=true})}
        val unmarked=assemblers.filter{it.enabled&&dayRows[it.id]==null}
        if(editable&&unmarked.isNotEmpty())item{ProtoPrimaryButton("Отметить остальных: работали ("+unmarked.size+")",{unmarked.forEach{onSave(it,"Работал","")}})}
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
                Text(existing?.status?:"Не заполнено",color=when(existing?.status){"Работал"->ProtoGreen;"Выходной"->ProtoGoldSoft;else->ProtoMuted},fontSize=12.sp)
                if(editable){
                    Spacer(Modifier.height(6.dp))
                    ProtoField(comment,{comment=it},"Комментарий / причина")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        // Обе кнопки равного веса; выбранный вариант подсвечен, второй — контурный.
                        listOf("Работал","Выходной").forEach{st->
                            val chosen=existing?.status==st
                            if(chosen)Button(onClick={onSave(person,st,comment)},modifier=Modifier.weight(1f).height(44.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text(st,color=Color.Black,fontWeight=FontWeight.SemiBold)}
                            else OutlinedButton(onClick={onSave(person,st,comment)},modifier=Modifier.weight(1f).height(44.dp),border=BorderStroke(1.dp,ProtoGold.copy(alpha=.6f))){Text(st,color=ProtoText)}
                        }
                    }
                }
            }
        }
        }
    }
    if(showCalendar)ProtoProductionCalendarDialog(selectedDate,onDismiss={showCalendar=false},maxDate=LocalDate.now(),onSelect={onDate(it);showCalendar=false})
}

internal fun protoDuration(ms:Long):String{
    val totalMinutes=(ms/60_000L).coerceAtLeast(0L)
    val h=totalMinutes/60
    val m=totalMinutes%60
    return if(h>0)"${h} ч ${m} мин" else "${m} мин"
}

@Composable
internal fun ProtoChatScreen(
    title:String,
    subtitle:String,
    messages:List<SansaraChatMessage>,
    currentRole:String,
    onBack:()->Unit,
    onRead:()->Unit,
    onSend:(String,String,String,String)->Unit,
    banner:String?=null,
    onCall:(()->Unit)?=null,
    bottomBar:(@Composable ()->Unit)?=null
){
    val context=LocalContext.current
    var text by remember{mutableStateOf("")}
    val listState=androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(messages.size){if(messages.isNotEmpty())listState.animateScrollToItem(messages.size)}
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
            ProtoTopBar(title=title,subtitle=subtitle,onBack=onBack,action=if(onCall!=null){{IconButton(onClick=onCall){Icon(Icons.Outlined.Call,contentDescription="Позвонить",tint=ProtoGold)}}}else null)
            if(!banner.isNullOrBlank())Text(banner,color=ProtoMuted,fontSize=12.sp,lineHeight=16.sp,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp).background(ProtoPanel2,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=8.dp))
            // Переписка — в рамке, поле серое, как нижняя панель ввода.
            LazyColumn(
                state=listState,
                modifier=Modifier.weight(1f).padding(start=12.dp,end=12.dp,top=8.dp,bottom=8.dp).clip(RoundedCornerShape(18.dp)).background(ProtoPanel).border(1.dp,ProtoBorder,RoundedCornerShape(18.dp)),
                contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ){
                if(messages.isEmpty())item{ProtoEmptyState(Icons.Outlined.ChatBubbleOutline,"Сообщений пока нет","Напишите вопрос — ответим в рабочее время")}
                items(messages,key={it.id}){msg->
                    val mine=msg.senderRole==currentRole
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start){
                        Surface(
                            color=if(mine)ProtoGold.copy(alpha=.20f) else ProtoPanel2,
                            border=BorderStroke(1.dp,if(mine)ProtoGold.copy(alpha=.55f) else ProtoBorder),
                            shape=RoundedCornerShape(16.dp),
                            modifier=Modifier.fillMaxWidth(.82f)
                        ){
                            Column(Modifier.padding(11.dp)){
                                if(msg.body.isNotBlank())Text(msg.body,color=ProtoText,fontSize=15.sp,lineHeight=20.sp)
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
                                        Text(msg.attachmentName.ifBlank{"Вложение"},color=ProtoGoldSoft,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    }
                                }
                                Text(protoChatTime(msg.createdAt),color=ProtoMuted,fontSize=12.sp,modifier=Modifier.align(Alignment.End).padding(top=5.dp))
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
            if(bottomBar!=null)bottomBar()
        }
    }
}

@Composable
internal fun ProtoAdminSearchScreen(clients:List<ProtoClient>,orders:List<ProtoOrder>,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit,onClient:(ProtoClient)->Unit,onOrder:(ProtoOrder)->Unit,onProduct:(ProtoCatalogProduct)->Unit){
    var query by remember{mutableStateOf("")};val q=query.trim();val fc=if(q.isBlank())emptyList() else clients.filter{it.name.contains(q,true)||it.contact.contains(q,true)||it.id.contains(q,true)};val fo=if(q.isBlank())emptyList() else orders.filter{it.id.contains(q,true)||it.clientName.contains(q,true)};val fp=if(q.isBlank())emptyList() else products.filter{it.sku.contains(q,true)||it.name.contains(q,true)}.take(20)
    ProtoScaffold("Поиск","Клиенты · заказы · товары",onBack){item{ProtoField(query,{query=it},"Введите название, ID или артикул")};if(q.isBlank())item{Text("Начните ввод — поиск работает сразу по всей системе.",color=ProtoMuted)};if(fc.isNotEmpty())item{Text("Клиенты",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fc,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onClient(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.id} · ${c.status}",color=ProtoMuted,fontSize=12.sp)}};if(fo.isNotEmpty())item{Text("Заказы",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fo,key={it.id}){o->ProtoOrderRow(o){onOrder(o)}};if(fp.isNotEmpty())item{Text("Товары",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fp,key={it.sku}){p->ProtoSectionCard(Modifier.clickable{onProduct(p)}){Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);ProtoSkuText(p.sku,fontSize=13)};Text("${stockOverrides[p.sku]?:p.stock} шт.",color=ProtoGoldSoft)}}}}
}

@Composable
internal fun ProtoAssemblerAdminScreen(assemblers:List<SansaraAssembler>,onBack:()->Unit,onAdd:(String)->Unit,onToggle:(SansaraAssembler)->Unit,onSave:(SansaraAssembler)->Unit={}){
    var name by remember{mutableStateOf("")}
    var editing by remember{mutableStateOf<SansaraAssembler?>(null)}
    editing?.let{a->
        var fio by remember(a.id){mutableStateOf(a.name)}
        var phone by remember(a.id){mutableStateOf(protoPhoneDigits(a.phone))}
        var birth by remember(a.id){mutableStateOf(a.birthDate)}
        var comment by remember(a.id){mutableStateOf(a.comment)}
        var pickBirth by remember{mutableStateOf(false)}
        val birthOk=birth.isBlank()||protoParseDate(birth)!=null
        if(pickBirth)ProtoBirthDateDialog(protoParseDate(birth),onDismiss={pickBirth=false}){birth=it.format(protoDateFmt);pickBirth=false}
        androidx.compose.ui.window.Dialog(onDismissRequest={editing=null},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
            Surface(color=ProtoPanel,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth(0.94f)){
                Column(Modifier.padding(18.dp).then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("Сборщица",color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold)
                    OutlinedTextField(fio,{fio=it.take(80)},label={Text("ФИО")},singleLine=true,colors=protoFieldColors(),modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(phone,{phone=protoPhoneDigits(it)},label={Text("Телефон")},prefix={Text("+7 ",color=ProtoText)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),colors=protoFieldColors(),modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(birth,{birth=it.take(10)},label={Text("Дата рождения (дд.мм.гггг)")},singleLine=true,isError=!birthOk,colors=protoFieldColors(),modifier=Modifier.fillMaxWidth(),
                        trailingIcon={IconButton(onClick={pickBirth=true}){Icon(Icons.Outlined.CalendarMonth,"Выбрать дату",tint=ProtoGold)}})
                    OutlinedTextField(comment,{comment=it.take(300)},label={Text("Комментарий")},minLines=2,colors=protoFieldColors(),modifier=Modifier.fillMaxWidth())
                    Text("О дне рождения придёт напоминание за 1–3 дня и в сам день.",color=ProtoMuted,fontSize=12.sp)
                    if(a.phone.isNotBlank())ProtoContactActions(a.phone,"Здравствуйте, "+a.name.substringBefore(" ")+"!")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(onClick={editing=null},border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.weight(1f)){Text("Отмена",color=ProtoMuted)}
                        Button(onClick={onSave(a.copy(name=fio.trim(),phone=if(phone.length==10)protoPhone(phone) else "",birthDate=birth.trim(),comment=comment.trim()));editing=null},enabled=fio.isNotBlank()&&birthOk&&(phone.isEmpty()||phone.length==10),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),modifier=Modifier.weight(1f)){Text("Сохранить",color=Color.Black)}
                    }
                }
            }
        }
    }
    ProtoScaffold("Сборщицы","Нажмите на сборщицу, чтобы открыть карточку",onBack){
        item{ProtoSectionCard{ProtoField(name,{name=it},"ФИО сборщицы");ProtoPrimaryButton("Добавить",{if(name.isNotBlank()){onAdd(name);name=""}},enabled=name.isNotBlank())}}
        val soon=assemblers.filter{it.enabled}.mapNotNull{a->a.daysToBirthday()?.let{d->a to d}}.filter{it.second<=7}.sortedBy{it.second}
        if(soon.isNotEmpty())item{
            ProtoSectionCard{
                Text("Дни рождения на этой неделе",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                soon.forEach{(a,d)->Text(a.name+" — "+(if(d==0L)"сегодня!" else a.birthDate.take(5)+", через "+d+" дн."),color=if(d==0L)ProtoGold else ProtoText,fontSize=14.sp,modifier=Modifier.padding(top=4.dp))}
            }
        }
        items(assemblers,key={it.id}){assembler->
            ProtoSectionCard(Modifier.clickable{editing=assembler}){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(assembler.name,color=ProtoText,fontWeight=FontWeight.SemiBold)
                        val extra=listOfNotNull(assembler.phone.takeIf{it.isNotBlank()},assembler.birthDate.takeIf{it.isNotBlank()}?.let{"д.р. "+it}).joinToString(" · ")
                        if(extra.isNotBlank())Text(extra,color=ProtoMuted,fontSize=12.sp)
                        if(assembler.comment.isNotBlank())Text(assembler.comment,color=ProtoGoldSoft,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(if(assembler.enabled)"Активна" else "Отключена",color=if(assembler.enabled)ProtoGreen else ProtoMuted,fontSize=12.sp)
                    }
                    Switch(checked=assembler.enabled,onCheckedChange={onToggle(assembler)},colors=SwitchDefaults.colors(checkedTrackColor=ProtoGold,checkedThumbColor=Color.Black))
                }
            }
        }
    }
}

/** Выбор даты рождения: год, месяц и день колесом через календарь с быстрым переходом по годам. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProtoBirthDateDialog(initial:LocalDate?,onDismiss:()->Unit,onPick:(LocalDate)->Unit){
    val zone=java.time.ZoneOffset.UTC
    val state=rememberDatePickerState(
        initialSelectedDateMillis=(initial?:LocalDate.now().minusYears(30)).atStartOfDay(zone).toInstant().toEpochMilli(),
        yearRange=1940..LocalDate.now().year,
        initialDisplayMode=DisplayMode.Picker
    )
    DatePickerDialog(
        onDismissRequest=onDismiss,
        confirmButton={TextButton(enabled=state.selectedDateMillis!=null,onClick={state.selectedDateMillis?.let{onPick(java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate())}}){Text("Выбрать",color=ProtoGold)}},
        dismissButton={TextButton(onClick=onDismiss){Text("Отмена",color=ProtoMuted)}}
    ){ DatePicker(state=state,title={Text("Дата рождения",modifier=Modifier.padding(start=24.dp,top=16.dp))},showModeToggle=true) }
}

@Composable
internal fun ProtoProductionCalendarDialog(selectedDate:LocalDate,onDismiss:()->Unit,minDate:LocalDate?=null,workdaysOnly:Boolean=false,maxDate:LocalDate?=null,onSelect:(LocalDate)->Unit){
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
                    listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach{d->Text(d,color=ProtoMuted,fontSize=12.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
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
                                val enabled=(minDate==null||!date.isBefore(minDate))&&(maxDate==null||!date.isAfter(maxDate))&&(!workdaysOnly||protoIsWorkday(date))
                                val today=date==LocalDate.now()
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if(selected)ProtoGold else Color.Transparent)
                                        .then(if(today&&!selected)Modifier.border(1.dp,ProtoGold.copy(alpha=.6f),CircleShape) else Modifier)
                                        .clickable(enabled=enabled){onSelect(date)},
                                    contentAlignment=Alignment.Center
                                ){Text(day.toString(),color=when{selected->Color.Black;!enabled->ProtoMuted.copy(alpha=.35f);else->ProtoText},fontSize=13.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)}
                            } else Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        },
        confirmButton={if(minDate==null||!LocalDate.now().isBefore(minDate))TextButton(onClick={onSelect(LocalDate.now())}){Text("Сегодня",color=ProtoGold)}},
        dismissButton={TextButton(onClick=onDismiss){Text("Закрыть",color=ProtoMuted)}}
    )
}

@Composable
internal fun ProtoProductionHistoryScreen(
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
    var period by remember{mutableStateOf<ProtoPeriod?>(null)}
    var editing by remember{mutableStateOf<ProtoProductionOp?>(null)}
    var deleteCandidate by remember{mutableStateOf<ProtoProductionOp?>(null)}
    val assemblers=listOf("Все")+ops.map{it.assembler}.filter{it.isNotBlank()}.distinct().sorted()
    val filtered=ops.filter{op->
        op.status!="Удален" &&
        (query.isBlank()||op.sku.contains(query,true)||op.name.contains(query,true)||op.date.contains(query,true)) &&
        (assembler=="Все"||op.assembler==assembler) &&
        (period==null||period!!.containsText(op.date))
    }
    val byMonth=filtered.groupBy{op->
        runCatching{LocalDate.parse(op.date,DateTimeFormatter.ofPattern("dd.MM.yyyy")).format(DateTimeFormatter.ofPattern("MM.yyyy"))}.getOrDefault("Без даты")
    }.toSortedMap(compareByDescending{it})
    ProtoScaffold("История приходов",(period?.label?:"Всё время")+" · "+filtered.sumOf{it.qty}+" шт.",onBack){
        item{ProtoPeriodBar(period,{period=it},allowAll=true,maxDate=LocalDate.now())}
        item{ProtoField(query,{query=it},"Артикул / наименование")}
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
                                Text(op.time,color=ProtoMuted,fontSize=12.sp);ProtoSkuText(op.sku,fontSize=13)
                                Text("Сборщица: "+op.assembler,color=ProtoMuted,fontSize=12.sp)
                                if(op.documentId.isNotBlank())Text(op.documentId,color=ProtoGoldSoft,fontSize=11.sp)
                            }
                            Column(horizontalAlignment=Alignment.End){
                                Text(op.qty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                                Text(protoMoney(op.amountRub),color=ProtoGreen,fontWeight=FontWeight.Bold,fontSize=12.sp)
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
            text={Column{Text(op.name,color=ProtoGoldSoft);ProtoField(qtyText,{qtyText=it.filter(Char::isDigit)},"Количество",KeyboardType.Number);Text("Изменение попадёт в журнал и скорректирует склад.",color=ProtoMuted,fontSize=12.sp)}},
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

internal fun protoReportMonthLabel(key:String):String{
    val parts=key.split(".")
    if(parts.size!=2)return key
    val ym=runCatching{YearMonth.of(parts[1].toInt(),parts[0].toInt())}.getOrNull()?:return key
    return protoMonthLabel(ym.toString())
}

@Composable
internal fun ProtoProductionReportScreen(selectedDate:LocalDate,ops:List<ProtoProductionOp>,products:List<ProtoCatalogProduct>,onBack:()->Unit){
    val totalQty=ops.sumOf{it.qty}
    // Категория — из каталога, а если товар оттуда уже убран — по названию в строке прихода.
    val wreathQty=ops.filter{op->(products.firstOrNull{it.sku==op.sku}?.type ?: protoGuessType(op.name)).contains("Вен",true)}.sumOf{it.qty}
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
        items(bySku){row->ProtoSectionCard{Text(row.second,color=ProtoText,fontWeight=FontWeight.SemiBold);Row{Text(row.first,color=ProtoMuted,fontSize=12.sp);Spacer(Modifier.weight(1f));Text(row.third.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}
        item{Text("По качеству",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
        items(byQuality){row->ProtoSectionCard{Row{Text(row.first,color=ProtoText,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(row.second.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}
    }
}

@Composable
internal fun ProtoStaffProfileScreen(role:String,onBack:()->Unit,onCall:()->Unit,onLogout:()->Unit,onUpdate:()->Unit={}){
    ProtoScaffold("Профиль",role,onBack){
        item{ProtoSectionCard{ProtoInfoRow("Роль",role);ProtoInfoRow("Дата",currentDateLong());ProtoInfoRow("Версия",BuildConfig.VERSION_NAME);ProtoInfoRow("Поддержка",protoPhone(BuildConfig.ADMIN_PHONE));Spacer(Modifier.height(8.dp));OutlinedButton(onClick=onCall,modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Phone,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Позвонить в поддержку",color=ProtoGold)};OutlinedButton(onClick=onUpdate,modifier=Modifier.fillMaxWidth().padding(top=8.dp),border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.SystemUpdate,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Обновление",color=ProtoGold)};TextButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Выйти",color=ProtoMuted)}}}
    }
}

@Composable
internal fun ProtoNewClientsScreen(clients:List<ProtoClient>,onBack:()->Unit,onOpen:(ProtoClient)->Unit){val monthKey=LocalDate.now().format(DateTimeFormatter.ofPattern("MM.yyyy"));val current=clients.filter{it.registeredAt.endsWith(monthKey)};ProtoScaffold("Новые клиенты",currentMonthLabel(),onBack){items(current,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onOpen(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.orderCount} заказов · оборот ${protoMoney(c.monthTurnover)}",color=ProtoMuted,fontSize=12.sp)}}}}

@Composable
internal fun ProtoSimpleMessageScreen(title:String,text:String,onBack:()->Unit){
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
internal fun ProtoScaffold(
    title:String,
    subtitle:String?=null,
    onBack:(()->Unit)?=null,
    action:(@Composable ()->Unit)?=null,
    bottomBar:(@Composable ()->Unit)?=null,
    content:LazyListScope.()->Unit
){
    // Внутренние экраны: компактная шапка вместо крупного логотипа — больше места под содержимое.
    Box(Modifier.fillMaxSize()){
        ProtoLiveBackground()
        Column(Modifier.fillMaxSize()){
            ProtoTopBar(title=title,subtitle=subtitle,onBack=onBack,action=action)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=4.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
            if(bottomBar!=null)bottomBar()
        }
    }
}

@Composable
internal fun ProtoQtyButton(icon: androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit,size:Dp=34.dp){
    Surface(color=ProtoPanel2,border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(10.dp),modifier=Modifier.size(size).clickable(onClick=onClick)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(icon,null,tint=ProtoGold,modifier=Modifier.size((size.value*.48f).dp))}}
}

@Composable
internal fun ProtoProductImagePreview(product:ProtoCatalogProduct,onDismiss:()->Unit){
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        BackHandler { onDismiss() }
        var scale by remember{mutableFloatStateOf(1f)}
        var offX by remember{mutableFloatStateOf(0f)}
        var offY by remember{mutableFloatStateOf(0f)}
        Box(Modifier.fillMaxSize().background(Color.Black)){
            ProtoProductImage(
                product,
                Modifier.fillMaxSize()
                    .androidx_pointerInput_zoom({sc,ox,oy->scale=sc;offX=ox;offY=oy},{scale},{offX},{offY})
                    .graphicsLayer(scaleX=scale,scaleY=scale,translationX=offX,translationY=offY),
                ContentScale.Fit
            )
            IconButton(onClick=onDismiss,modifier=Modifier.align(Alignment.TopEnd).padding(18.dp).size(50.dp).background(Color.Black.copy(alpha=.72f),CircleShape).border(1.dp,ProtoGold,CircleShape)){Icon(Icons.Outlined.Close,contentDescription="Закрыть",tint=ProtoGold)}
        }
    }
}

internal fun Modifier.androidx_pointerInput_zoom(
    set:(Float,Float,Float)->Unit,
    scale:()->Float,
    offX:()->Float,
    offY:()->Float
):Modifier=this
    .pointerInput(Unit){
        detectTransformGestures{_,pan,zoom,_->
            val ns=(scale()*zoom).coerceIn(1f,5f)
            if(ns<=1f)set(1f,0f,0f) else set(ns,offX()+pan.x,offY()+pan.y)
        }
    }
    .pointerInput(Unit){
        detectTapGestures(onDoubleTap={ if(scale()>1f)set(1f,0f,0f) else set(2.5f,0f,0f) })
    }

@Composable
internal fun ProtoSectionCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),content=content)}}

@Composable
internal fun ProtoField(value:String,onChange:(String)->Unit,label:String,keyboardType:KeyboardType=KeyboardType.Text){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=keyboardType),colors=protoFieldColors())}

@Composable
internal fun protoChatFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=ProtoGold,unfocusedBorderColor=ProtoBorder,focusedTextColor=ProtoText,unfocusedTextColor=ProtoText,focusedLabelColor=ProtoGold,unfocusedLabelColor=ProtoMuted,focusedContainerColor=ProtoPanel2,unfocusedContainerColor=ProtoPanel2,cursorColor=ProtoGold,focusedPlaceholderColor=ProtoMuted,unfocusedPlaceholderColor=ProtoMuted)

@Composable
internal fun protoFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=ProtoGold,unfocusedBorderColor=ProtoBorder,focusedTextColor=ProtoText,unfocusedTextColor=ProtoText,focusedLabelColor=ProtoGold,unfocusedLabelColor=ProtoMuted,focusedLeadingIconColor=ProtoGold,unfocusedLeadingIconColor=ProtoMuted,disabledBorderColor=ProtoBorder,disabledTextColor=ProtoMuted)

@Composable
internal fun ProtoCompactGrid(title:String,options:List<String>,selected:Set<String>,toggle:(String)->Unit,disabled:Set<String> = emptySet()){Text(title,color=ProtoGoldSoft,fontSize=17.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=7.dp,bottom=6.dp));options.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{o->val off=o in disabled;FilterChip(selected=o in selected,onClick={if(!off)toggle(o)},enabled=!off,label={Text(if(off)"$o · в разработке" else o,fontSize=if(off)12.sp else 15.sp,maxLines=2)},modifier=Modifier.weight(1f).height(48.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText,disabledLabelColor=ProtoMuted))};if(row.size==1)Spacer(Modifier.weight(1f))};Spacer(Modifier.height(8.dp))}
}

@Composable
internal fun ProtoOrderRow(o:ProtoOrder,onClick:()->Unit){
    ProtoSectionCard(Modifier.clickable(onClick=onClick)){
        Row{
            Column(Modifier.weight(1f)){
                Text("№ "+o.id,color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                Text(o.clientName,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("${o.pieces} шт. · оформлен ${o.dateTime.substringBefore(" ")}",color=ProtoMuted,fontSize=12.sp)
                if(o.deliveryDate.isNotBlank())Text((if(o.deliveryMethod=="Самовывоз")"Самовывоз " else "Получение ")+o.deliveryDate+(if(o.deliveryTime.isNotBlank())" · "+o.deliveryTime else ""),color=ProtoText,fontSize=12.sp)
            }
            Column(horizontalAlignment=Alignment.End){
                Text(protoMoney(o.total),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                ProtoPill(o.status,protoStatusColor(o.status))
            }
        }
    }
}

@Composable
internal fun ProtoMetricCard(title:String,value:String,subtitle:String,modifier:Modifier,onClick:()->Unit)=ProtoMetricCard(title,value,subtitle,modifier,1,onClick)

@Composable
internal fun ProtoMetricCard(title:String,value:String,subtitle:String,modifier:Modifier,titleLines:Int,onClick:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=modifier.clickable(onClick=onClick)){Column(Modifier.padding(12.dp)){Text(title,color=ProtoMuted,fontSize=12.sp,minLines=titleLines,maxLines=titleLines.coerceAtLeast(2));Text(value,color=ProtoText,fontSize=25.sp,fontWeight=FontWeight.Bold);Text(subtitle,color=ProtoGoldSoft,fontSize=12.sp)}}}

@Composable
internal fun ProtoQuickButton(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,modifier=modifier.height(64.dp),border=BorderStroke(1.dp,ProtoBorder),contentPadding=PaddingValues(3.dp)){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=ProtoGold,modifier=Modifier.size(20.dp));Text(label,color=ProtoText,fontSize=11.sp,maxLines=1)}}}

@Composable
internal fun ProtoSwitchRow(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,color=ProtoText,modifier=Modifier.weight(1f));Switch(checked=value,onCheckedChange=onChange,colors=SwitchDefaults.colors(checkedThumbColor=Color.Black,checkedTrackColor=ProtoGold))}}

@Composable
internal fun ProtoSkuText(sku:String,extra:String="",fontSize:Int=13){
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
internal fun ProtoInfoRow(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(label,color=ProtoMuted);Spacer(Modifier.weight(1f));Text(value,color=ProtoText,fontWeight=FontWeight.Medium)}}

@Composable
internal fun ProtoPill(text:String,color:Color){Surface(color=color.copy(alpha=.16f),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,color.copy(alpha=.55f))){Text(text,color=color,fontSize=12.sp,modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp))}}

@Composable
internal fun protoNavColors()=NavigationBarItemDefaults.colors(selectedIconColor=Color.Black,selectedTextColor=ProtoGold,indicatorColor=ProtoGold,unselectedIconColor=ProtoMuted,unselectedTextColor=ProtoMuted)

@Composable
internal fun ProtoProductImage(p:ProtoCatalogProduct,modifier:Modifier=Modifier,contentScale:ContentScale=ContentScale.Crop){
    val placeholder=painterResource(protoPlaceholderForType(p.type))
    AsyncImage(model=p.imageUrl.takeIf{it.isNotBlank()}?:protoPlaceholderForType(p.type),contentDescription=p.name,modifier=modifier,contentScale=contentScale,placeholder=placeholder,error=placeholder,fallback=placeholder)
}

internal fun protoPlaceholderForType(type:String)=when(type){
    "Венки","Венки круглые","Корзины","Полянки","Флоретки"->R.drawable.mock_wreath
    "Ленты"->R.drawable.mock_ribbon
    "Гробы"->R.drawable.mock_coffin
    "Одежда"->R.drawable.mock_clothes
    "Кресты"->R.drawable.mock_cross
    "Цветы"->R.drawable.mock_flowers
    "Услуги"->R.drawable.mock_service
    else->R.drawable.mock_generic
}

internal fun protoParseOrderDate(value:String):LocalDateTime? =
    runCatching { LocalDateTime.parse(value,DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) }.getOrNull()

internal fun protoOrderMonthKey(value:String):String =
    protoParseOrderDate(value)?.let { YearMonth.from(it).toString() } ?: "0000-00"

internal fun protoMonthLabel(key:String):String {
    val ym=runCatching{YearMonth.parse(key)}.getOrNull() ?: return key
    val month=ym.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE,ruLocale)
    return month.replaceFirstChar{if(it.isLowerCase())it.titlecase(ruLocale) else it.toString()}+" "+ym.year
}

internal fun protoFilterOrdersByPeriod(orders:List<ProtoOrder>,period:String):List<ProtoOrder>{
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

internal fun protoChatTime(epoch:Long):String =
    Instant.ofEpochMilli(epoch).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))

internal fun protoUriName(context:Context,uri:Uri):String {
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

internal fun protoOpenAttachment(context:Context,uriValue:String,mime:String) {
    runCatching {
        val uri=Uri.parse(uriValue)
        val intent=Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri,mime.ifBlank{"*/*"})
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}

internal fun protoLoadNotifications(prefs:android.content.SharedPreferences):List<ProtoNotification>{
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
                read=o.optBoolean("read",false),
                orderId=o.optString("orderId","")
            )
        }
    }.getOrDefault(emptyList())
}

internal fun protoSaveNotifications(prefs:android.content.SharedPreferences,items:List<ProtoNotification>){
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
            put("orderId",n.orderId)
        })
    }
    prefs.edit().putString("client_notifications",array.toString()).apply()
}

internal fun protoProductCategoryKey(type:String):String = when {
    type.contains("Вен", ignoreCase=true) -> "Венки"
    type.contains("Гроб", ignoreCase=true) -> "Гробы"
    type.contains("Крест", ignoreCase=true) -> "Кресты"
    type.contains("Лент", ignoreCase=true) -> "Ленты"
    type.contains("Одеж", ignoreCase=true) -> "Одежда"
    else -> type.ifBlank { "Прочее" }
}

internal fun protoToggle(set:Set<String>,value:String)=if(value in set)set-value else set+value
internal fun protoMoney(value:Int)=NumberFormat.getCurrencyInstance(ruLocale).format(value).replace(",00","")
internal fun protoStatusColor(status:String):Color = when(status){
    "Получен"->ProtoMuted
    "Подтверждён"->ProtoGold
    "Собирается","Доставляется"->ProtoOrange
    "Доставлен"->ProtoGreen
    "Отменён","Отклонён"->ProtoRed
    "На согласовании"->ProtoOrange
    else->ProtoGoldSoft
}
internal fun protoIconForType(type:String)=when(type){"Венки","Венки круглые"->Icons.Outlined.LocalFlorist;"Корзины"->Icons.Outlined.ShoppingBasket;"Полянки","Флоретки"->Icons.Outlined.Spa;"Ленты"->Icons.Outlined.ReceiptLong;else->Icons.Outlined.Inventory2}

internal fun protoExportCsv(
    context: Context,
    report: String,
    period: String,
    products: List<ProtoCatalogProduct>,
    stockOverrides: SnapshotStateMap<String, Int>,
    orders: List<ProtoOrder>,
    clients: List<ProtoClient>,
    productionOps: List<ProtoProductionOp>,
    reservedForSku: (String) -> Int,
    toast: (String) -> Unit
) {
    val nowMonth = java.time.YearMonth.now()
    val periodMonth: java.time.YearMonth? = when (period) { "Этот месяц" -> nowMonth; "Прошлый месяц" -> nowMonth.minusMonths(1); else -> null }
    // Произвольный период из календаря: «дд.мм.гггг-дд.мм.гггг».
    val customPeriod = ProtoPeriod.fromKey(period)
    fun inPeriod(date: String): Boolean = when {
        customPeriod != null -> customPeriod.containsText(date)
        else -> periodMonth == null || protoParseDate(date.substringBefore(" "))?.let { java.time.YearMonth.from(it) == periodMonth } == true
    }
    @Suppress("NAME_SHADOWING") val orders = orders.filter { inPeriod(it.dateTime) || report == "Резерв по клиентам" }
    @Suppress("NAME_SHADOWING") val productionOps = productionOps.filter { inPeriod(it.date) }
    val safeName = (report + if (customPeriod != null) "_" + customPeriod.key.replace(".", "") else if (periodMonth != null) "_" + periodMonth else "").lowercase(ruLocale).replace(" ", "_").replace("/", "-")
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
            "Выпуск за период", "Выпуск за месяц" -> {
                appendLine("Дата;Время;SKU;Наименование;Количество;Сборщица;Ставка;Сумма;Документ;Оприходовал;Статус")
                productionOps.forEach { op -> appendLine("${op.date};${op.time};${op.sku};${op.name};${op.qty};${op.assembler};${op.rateRub};${op.amountRub};${op.documentId};${op.postedBy};${op.status}") }
            }
            "Производство по сборщицам" -> {
                appendLine("Сборщица;Количество;Сумма")
                productionOps.groupBy { it.assembler }.forEach { (assembler, ops) -> appendLine("$assembler;${ops.sumOf { it.qty }};${ops.sumOf { it.amountRub }}") }
            }
            "Заказы за период", "Заказы за месяц" -> {
                appendLine("Заказ;Клиент;Покупатель агента;Дата;Получение;Количество;Сумма;Статус")
                orders.forEach { o -> appendLine("${o.id};${o.clientName.substringBefore(" / ")};${o.retailCustomerName};${o.dateTime};${o.deliveryDate};${o.pieces};${o.total};${o.status}") }
            }
            else -> {
                appendLine("Клиент;Статус;Заказы;Оборот")
                clients.forEach { c -> appendLine("${c.name};${c.status};${c.orderCount};${c.monthTurnover}") }
            }
        }
    }
    var savedUri: Uri? = null
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SANSARA")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Не удалось создать файл")
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write("\uFEFF" + csv) } ?: error("Не удалось открыть файл")
            savedUri = uri
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SANSARA").apply { mkdirs() }
            File(dir, fileName).writeText("\uFEFF" + csv, Charsets.UTF_8)
        }
    }.onSuccess {
        toast("Отчёт сохранён: Загрузки/SANSARA/$fileName")
        savedUri?.let { uri ->
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SANSARA · $report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(Intent.createChooser(share, "Отправить отчёт")) }
        }
    }
        .onFailure { toast("Ошибка экспорта: ${it.message ?: "неизвестная ошибка"}") }
}

internal fun protoDialNumber(context:Context,phone:String){val normalized=phone.filter{it.isDigit()||it=='+'};context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:$normalized")))}
internal fun protoDial(context:Context){protoDialNumber(context,BuildConfig.ADMIN_PHONE)}
/** Открывает SMS-приложение телефона с готовым текстом. Отправка — одним нажатием, без платного SMS-сервиса. */
internal fun protoSms(context:Context,phone:String,body:String){
    val digits=phone.filter{it.isDigit()||it=='+'}
    runCatching{context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:$digits")).putExtra("sms_body",body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
}
internal const val PROTO_REJECT_TEXT = "Ваша компания не прошла проверку службы безопасности. Приносим свои извинения. Для уточнения информации свяжитесь с администратором компании САНСАРА."
internal fun protoMessage(context:Context){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${BuildConfig.ADMIN_PHONE}")))}

// ───────────── Продажи: отгрузки, оплаты, клиенты ─────────────

internal val protoDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

internal fun protoParseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim(), protoDateFmt) }.getOrNull()
internal fun protoDaysTo(due: String): Int? = protoParseDate(due)?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }
internal fun protoMonthKey(date: String): String = date.substringAfter('.', "")

internal fun protoMoneyColor(s: ShipmentEntity): Color = if (s.moneyStatus == "CONFIRMED") ProtoGreen else ProtoRed
internal fun protoMoneyLabel(s: ShipmentEntity): String = when (s.moneyStatus) {
    "CONFIRMED" -> "Оплата подтверждена"
    "REQUESTED" -> "Ждёт подтверждения админа"
    else -> if (s.paymentType == "DEFERRED") "Отсрочка · деньги не получены" else "Деньги не подтверждены"
}

internal fun protoDaysLabel(due: String): String {
    val d = protoDaysTo(due) ?: return "—"
    return when {
        d > 0 -> "$d дн."
        d == 0 -> "сегодня"
        else -> "просрочено ${-d} дн."
    }
}

@Composable
internal fun ProtoSalesHomeScreen(
    name: String,
    shipments: List<ShipmentEntity>,
    onNew: () -> Unit,
    onList: () -> Unit,
    onClients: () -> Unit,
    onLogout: () -> Unit
) {
    val today = currentDateShort()
    val todayCount = shipments.count { it.shipDate == today }
    val unconfirmed = shipments.filter { it.moneyStatus != "CONFIRMED" }.sumOf { it.amount }
    val waiting = shipments.count { it.moneyStatus == "REQUESTED" }
    val debt = shipments.filter { it.paymentType == "DEFERRED" && it.moneyStatus != "CONFIRMED" }.sumOf { it.amount }
    ProtoScaffold("Здравствуйте, $name", "Продажи · отгрузка и оплаты", null) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProtoMetricCard("Отгрузок сегодня", todayCount.toString(), "", Modifier.weight(1f), onList)
                ProtoMetricCard("Ждут админа", waiting.toString(), "оплат", Modifier.weight(1f), onList)
            }
        }
        item {
            ProtoSectionCard {
                Text("Деньги не подтверждены", color = ProtoRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(protoMoney(unconfirmed), color = ProtoRed, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                ProtoInfoRow("В том числе по отсрочкам", protoMoney(debt))
            }
        }
        item { Text("Быстрые действия", color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoQuickButton("Новая отгрузка", Icons.Outlined.LocalShipping, Modifier.weight(1f), onNew)
                ProtoQuickButton("Отгрузки и оплаты", Icons.Outlined.Payments, Modifier.weight(1f), onList)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoQuickButton("Клиенты", Icons.Outlined.Groups, Modifier.weight(1f), onClients)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun ProtoSalesShipmentForm(
    clients: List<ProtoClient>,
    orders: List<ProtoOrder>,
    shipments: List<ShipmentEntity>,
    onBack: () -> Unit,
    onSave: (ProtoClient, ProtoOrder, Int, String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var client by remember { mutableStateOf<ProtoClient?>(null) }
    var order by remember { mutableStateOf<ProtoOrder?>(null) }
    var amountText by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("PAID") }
    var dueDate by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }
    // Отгружаем только собранные заказы: статус «Собирается», отгрузка по ним ещё не оформлена.
    val clientOrders = client?.let { c -> orders.filter { (it.clientId == c.id || (it.clientId.isBlank() && it.clientName.substringBefore(" / ") == c.name)) && it.status == "Собирается" && shipments.none { s -> s.orderId == it.id } } }.orEmpty()
    val amount = amountText.toIntOrNull() ?: 0
    val valid = client != null && order != null && amount > 0 && (paymentType == "PAID" || dueDate.isNotBlank())
    ProtoScaffold("Новая отгрузка", "Клиент · заказ · сумма · оплата", onBack) {
        if (client == null) {
            item { ProtoField(query, { query = it }, "Поиск клиента") }
            val ready = orders.filter { it.status == "Собирается" && shipments.none { s -> s.orderId == it.id } }
            val found = clients.filter { query.isBlank() || it.name.contains(query, true) || it.contact.contains(query, true) || it.phone.contains(query, true) }
                .sortedByDescending { c -> ready.count { o -> o.clientId == c.id || o.clientName.substringBefore(" / ") == c.name } }
            items(found.take(12), key = { it.id }) { c ->
                ProtoSectionCard(Modifier.clickable { client = c; order = null; amountText = "" }) {
                    Text(c.name, color = ProtoText, fontWeight = FontWeight.Bold)
                    Text(c.contact + " · " + protoPhone(c.phone), color = ProtoMuted, fontSize = 12.sp)
                }
            }
        } else {
            item {
                ProtoSectionCard {
                    Text("Клиент", color = ProtoGoldSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(client!!.name, color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(client!!.contact + " · " + protoPhone(client!!.phone), color = ProtoMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    ProtoSecondaryButton("Выбрать другого клиента", { client = null; order = null; amountText = "" })
                }
            }
            item { Text("Заказ клиента из приложения", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            if (clientOrders.isEmpty()) item { ProtoSectionCard { Text("Нет собранных заказов. Отгрузить можно заказ в статусе «Собирается» — после отгрузки он перейдёт в «Доставляется».", color = ProtoMuted, fontSize = 13.sp) } }
            items(clientOrders, key = { it.id }) { o ->
                val active = order?.id == o.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = ProtoPanel),
                    border = BorderStroke(if (active) 2.dp else 1.dp, if (active) ProtoGold else ProtoBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { order = o; amountText = o.total.toString() }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(o.id, color = ProtoGoldSoft, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            ProtoPill(o.status, ProtoOrange)
                        }
                        Text(o.dateTime, color = ProtoMuted, fontSize = 12.sp)
                        ProtoInfoRow(o.pieces.toString() + " шт.", protoMoney(o.total))
                    }
                }
            }
            if (order != null) {
                item {
                    ProtoSectionCard {
                        ProtoField(amountText, { amountText = it.filter(Char::isDigit).take(9) }, "Сумма отгрузки, ₽", KeyboardType.Number)
                        Text("Оплата", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
                        Row(Modifier.fillMaxWidth().height(52.dp).border(1.dp, ProtoGold, RoundedCornerShape(26.dp))) {
                            listOf("PAID" to "Оплатил", "DEFERRED" to "Отсрочка").forEach { (key, label) ->
                                val active = paymentType == key
                                Box(
                                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(if (active) ProtoGold else Color.Transparent).clickable { paymentType = key },
                                    contentAlignment = Alignment.Center
                                ) { Text(label, color = if (active) Color.Black else ProtoText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                            }
                        }
                        if (paymentType == "DEFERRED") {
                            Spacer(Modifier.height(10.dp))
                            ProtoSecondaryButton(if (dueDate.isBlank()) "Выбрать дату оплаты" else "Оплата до: $dueDate", { showCalendar = true })
                        }
                        Text(
                            "Деньги считаются подтверждёнными только после подтверждения администратором. До этого сумма отображается красным.",
                            color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        ProtoPrimaryButton("Оформить отгрузку", { onSave(client!!, order!!, amount, paymentType, dueDate) }, enabled = valid)
                    }
                }
            }
        }
    }
    if (showCalendar) ProtoProductionCalendarDialog(
        protoParseDate(dueDate) ?: LocalDate.now().plusDays(7),
        onDismiss = { showCalendar = false },
        onSelect = { dueDate = it.format(protoDateFmt); showCalendar = false }
    )
}

@Composable
internal fun ProtoShipmentsScreen(
    items: List<ShipmentEntity>,
    adminMode: Boolean,
    onBack: () -> Unit,
    onRequest: (ShipmentEntity) -> Unit,
    onConfirm: (ShipmentEntity) -> Unit,
    onReject: (ShipmentEntity, String) -> Unit
) {
    var filter by remember { mutableStateOf(if (adminMode) "Ждут" else "Все") }
    var rejecting by remember { mutableStateOf<ShipmentEntity?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    rejecting?.let { sh ->
        AlertDialog(
            onDismissRequest = { rejecting = null }, containerColor = ProtoPanel,
            title = { Text("Отклонить оплату?", color = ProtoText) },
            text = {
                Column {
                    Text(sh.clientName + " · " + protoMoney(sh.amount), color = ProtoGoldSoft)
                    ProtoField(rejectReason, { rejectReason = it.take(200) }, "Причина — увидит отдел продаж")
                    listOf("Деньги не поступили", "Сумма не совпадает").forEach { v -> Text(v, color = ProtoGold, fontSize = 13.sp, modifier = Modifier.clickable { rejectReason = v }.padding(vertical = 4.dp)) }
                }
            },
            confirmButton = { TextButton(onClick = { onReject(sh, rejectReason.trim()); rejecting = null; rejectReason = "" }, enabled = rejectReason.isNotBlank()) { Text("Отклонить", color = if (rejectReason.isNotBlank()) ProtoRed else ProtoMuted) } },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
    val filters = listOf("Все", "Ждут", "Не оплачено", "Оплачено")
    val shown = items.filter {
        when (filter) {
            "Ждут" -> it.moneyStatus == "REQUESTED"
            "Не оплачено" -> it.moneyStatus != "CONFIRMED"
            "Оплачено" -> it.moneyStatus == "CONFIRMED"
            else -> true
        }
    }
    ProtoScaffold(
        if (adminMode) "Подтверждение оплат" else "Отгрузки и оплаты",
        if (adminMode) "Запросы от отдела продаж" else "Деньги считаются после подтверждения админом",
        onBack
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.forEach { f ->
                    val active = filter == f
                    Surface(
                        color = if (active) ProtoGold else ProtoPanel,
                        border = BorderStroke(1.dp, if (active) ProtoGold else ProtoBorder),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).height(40.dp).clickable { filter = f }
                    ) { Box(contentAlignment = Alignment.Center) { Text(f, color = if (active) Color.Black else ProtoText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) } }
                }
            }
        }
        if (shown.isEmpty()) item { ProtoSectionCard { Text("Здесь пока ничего нет", color = ProtoMuted) } }
        items(shown, key = { it.id }) { sh ->
            val color = protoMoneyColor(sh)
            Card(
                colors = CardDefaults.cardColors(containerColor = ProtoPanel),
                border = BorderStroke(1.dp, color.copy(alpha = .7f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sh.clientName, color = ProtoText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sh.orderId + " · " + sh.shipDate, color = ProtoMuted, fontSize = 12.sp)
                        }
                        Text(protoMoney(sh.amount), color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProtoPill(if (sh.paymentType == "PAID") "Оплатил" else "Отсрочка до " + sh.dueDate, ProtoGoldSoft)
                        ProtoPill(protoMoneyLabel(sh), color)
                    }
                    if (sh.paymentType == "DEFERRED" && sh.moneyStatus != "CONFIRMED") {
                        val d = protoDaysTo(sh.dueDate)
                        ProtoInfoRow("До оплаты", protoDaysLabel(sh.dueDate))
                        if (d != null && d < 0) Text("Просрочка оплаты", color = ProtoRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (adminMode && sh.moneyStatus == "REQUESTED") {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onConfirm(sh) }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) {
                                Text("Подтвердить", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = { rejecting = sh; rejectReason = "" }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, ProtoRed)) {
                                Text("Отклонить", color = ProtoRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (sh.moneyStatus == "UNCONFIRMED" && sh.rejectReason.isNotBlank()) {
                        Text("Администратор отклонил: " + sh.rejectReason, color = ProtoRed, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    if (!adminMode && sh.moneyStatus == "UNCONFIRMED") {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onRequest(sh) }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) {
                            Text(if (sh.rejectReason.isNotBlank()) "Отправить повторно на подтверждение" else "Деньги получены · на подтверждение", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProtoSalesClientsScreen(
    clients: List<ProtoClient>,
    shipments: List<ShipmentEntity>,
    onBack: () -> Unit,
    onOpen: (ProtoClient) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val shown = clients.filter { query.isBlank() || it.name.contains(query, true) || it.contact.contains(query, true) || it.phone.contains(query, true) }
    ProtoScaffold("Клиенты", "Оборот, дебиторка, дни до оплаты", onBack) {
        item { ProtoField(query, { query = it }, "Поиск клиента") }
        items(shown, key = { it.id }) { c ->
            val debt = shipments.filter { it.clientId == c.id && it.moneyStatus != "CONFIRMED" }.sumOf { it.amount }
            ProtoSectionCard(Modifier.clickable { onOpen(c) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = ProtoText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(c.contact + " · " + protoPhone(c.phone), color = ProtoMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (debt > 0) Text(protoMoney(debt), color = ProtoRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun ProtoClientFinanceBlock(clientId: String, shipments: List<ShipmentEntity>, title: String = "Финансы клиента", receivableLabel: String = "Дебиторка", onlyDeferred: Boolean = false) {
    val mine = if (clientId.isBlank()) shipments else shipments.filter { it.clientId == clientId }
    val month = currentDateShort().substringAfter('.')
    val turnover = mine.filter { protoMonthKey(it.shipDate) == month }.sumOf { it.amount }
    val receivable = mine.filter { it.moneyStatus != "CONFIRMED" && (!onlyDeferred || it.paymentType == "DEFERRED") }.sumOf { it.amount }
    val nearest = mine.filter { it.paymentType == "DEFERRED" && it.moneyStatus != "CONFIRMED" }
        .mapNotNull { protoDaysTo(it.dueDate) }.minOrNull()
    ProtoSectionCard {
        Text(title, color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
        ProtoInfoRow("Оборот за месяц", protoMoney(turnover))
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(receivableLabel, color = ProtoMuted); Spacer(Modifier.weight(1f))
            Text(protoMoney(receivable), color = if (receivable > 0) ProtoRed else ProtoText, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Дней до оплаты", color = ProtoMuted); Spacer(Modifier.weight(1f))
            Text(
                when { nearest == null -> "—"; nearest < 0 -> "просрочено ${-nearest} дн."; nearest == 0 -> "сегодня"; else -> "$nearest дн." },
                color = if (nearest != null && nearest < 0) ProtoRed else ProtoText, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun ProtoSalesClientScreen(
    client: ProtoClient?,
    shipments: List<ShipmentEntity>,
    orders: List<ProtoOrder>,
    onBack: () -> Unit
) {
    val c = client ?: return
    ProtoScaffold(c.name, c.contact, onBack) {
        item {
            ProtoSectionCard {
                ProtoInfoRow("Телефон", protoPhone(c.phone))
                if (c.email.isNotBlank()) ProtoInfoRow("E-mail", c.email)
                if (c.city.isNotBlank()) ProtoInfoRow("Город", c.city)
                if (c.address.isNotBlank()) ProtoInfoRow("Адрес", c.address)
                ProtoInfoRow("Тип", c.clientType)
                ProtoInfoRow("Скидка", c.discount.toString() + "%")
                ProtoInfoRow("Заказов в приложении", orders.count { it.clientId == c.id || (it.clientId.isBlank() && it.clientName.substringBefore(" / ") == c.name) }.toString())
            }
        }
        item { ProtoClientFinanceBlock(c.id, shipments) }
        val mine = shipments.filter { it.clientId == c.id }
        item { Text("Отгрузки", color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (mine.isEmpty()) item { ProtoSectionCard { Text("Отгрузок пока нет", color = ProtoMuted) } }
        items(mine, key = { it.id }) { sh ->
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sh.orderId + " · " + sh.shipDate, color = ProtoText, modifier = Modifier.weight(1f))
                    Text(protoMoney(sh.amount), color = protoMoneyColor(sh), fontWeight = FontWeight.Bold)
                }
                Text(protoMoneyLabel(sh), color = protoMoneyColor(sh), fontSize = 12.sp)
            }
        }
    }
}

// ───────────── Финансы: дашборд руководителя, расходы, аналитика клиента ─────────────

internal data class ProtoFinanceTotals(
    val turnover: Int, val received: Int, val expenses: Int, val profit: Int,
    val receivable: Int, val deferred: Int, val overdue: Int, val pendingSum: Int, val pendingCount: Int
)

internal fun protoEpochDate(ms: Long): String =
    if (ms <= 0L) "" else java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(protoDateFmt)

internal fun protoThisMonthKey(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("MM.yyyy"))
internal fun protoPrevMonthKey(): String = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MM.yyyy"))
internal fun protoInPeriod(date: String, month: String?): Boolean = month == null || protoMonthKey(date) == month

/**
 * Оборот = сумма отгрузок за период. Поступления = подтверждённые админом деньги (по дате подтверждения).
 * Чистая прибыль = подтверждённые поступления − расходы. Дебиторка = всё, что не подтверждено админом.
 */
internal fun protoFinanceTotals(shipments: List<ShipmentEntity>, expenses: List<ExpenseEntity>, month: String?): ProtoFinanceTotals {
    val turnover = shipments.filter { protoInPeriod(it.shipDate, month) }.sumOf { it.amount }
    val received = shipments.filter { it.moneyStatus == "CONFIRMED" && protoInPeriod(protoEpochDate(it.confirmedAt), month) }.sumOf { it.amount }
    val spent = expenses.filter { protoInPeriod(it.date, month) }.sumOf { it.amount }
    val open = shipments.filter { it.moneyStatus != "CONFIRMED" }
    val deferred = open.filter { it.paymentType == "DEFERRED" }
    val overdue = deferred.filter { (protoDaysTo(it.dueDate) ?: 0) < 0 }.sumOf { it.amount }
    val pending = shipments.filter { it.moneyStatus == "REQUESTED" }
    return ProtoFinanceTotals(turnover, received, spent, received - spent, open.sumOf { it.amount }, deferred.sumOf { it.amount }, overdue, pending.sumOf { it.amount }, pending.size)
}

internal val protoExpenseCategories = listOf("Сырьё", "Зарплата", "Доставка", "Аренда", "Реклама", "Прочее")

@Composable
internal fun ProtoFinanceCard(title: String, value: String, subtitle: String, valueColor: Color, modifier: Modifier, onClick: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProtoPanel),
        border = BorderStroke(1.dp, ProtoBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = ProtoMuted, fontSize = 12.sp)
            Text(value, color = valueColor, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = ProtoGoldSoft, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun ProtoManagerDashboardScreen(
    shipments: List<ShipmentEntity>,
    expenses: List<ExpenseEntity>,
    onBack: () -> Unit,
    onPayments: () -> Unit,
    onAddExpense: (String, Int, String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    var period by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    val periods = listOf("Месяц", "Прошлый месяц", "Всё время")
    val month: String? = when (period) { 0 -> protoThisMonthKey(); 1 -> protoPrevMonthKey(); else -> null }
    val t = protoFinanceTotals(shipments, expenses, month)
    val inPeriod = shipments.filter { protoInPeriod(it.shipDate, month) }
    val top = inPeriod.groupBy { it.clientName }.map { (n, l) -> n to l.sumOf { it.amount } }.sortedByDescending { it.second }.take(5)
    val topMax = (top.firstOrNull()?.second ?: 1).coerceAtLeast(1)
    val openDeferred = shipments.filter { it.paymentType == "DEFERRED" && it.moneyStatus != "CONFIRMED" }.sortedBy { protoParseDate(it.dueDate) ?: LocalDate.MAX }
    val periodExpenses = expenses.filter { protoInPeriod(it.date, month) }
    var deleting by remember { mutableStateOf<ExpenseEntity?>(null) }
    deleting?.let { ex ->
        AlertDialog(
            onDismissRequest = { deleting = null }, containerColor = ProtoPanel,
            title = { Text("Удалить расход?", color = ProtoText) },
            text = { Text(ex.category + " · " + protoMoney(ex.amount) + " · " + ex.date, color = ProtoMuted) },
            confirmButton = { TextButton(onClick = { onDeleteExpense(ex); deleting = null }) { Text("Удалить", color = ProtoRed) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
    val months = (5 downTo 0).map { java.time.YearMonth.now().minusMonths(it.toLong()) }
    val monthly = months.map { ym ->
        val key = ym.format(DateTimeFormatter.ofPattern("MM.yyyy"))
        Triple(ym, shipments.filter { protoMonthKey(it.shipDate) == key }.sumOf { it.amount }, shipments.filter { it.moneyStatus == "CONFIRMED" && protoMonthKey(protoEpochDate(it.confirmedAt)) == key }.sumOf { it.amount })
    }
    val chartMax = monthly.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1)
    ProtoScaffold("Денежный поток", "Оборот · поступления · расходы · отсрочки", onBack) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEachIndexed { i, label ->
                    FilterChip(
                        selected = period == i, onClick = { period = i }, label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText)
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProtoFinanceCard("Оборот", protoMoney(t.turnover), "отгрузки за период", ProtoText, Modifier.weight(1f))
                ProtoFinanceCard("Поступления", protoMoney(t.received), "подтверждено админом", ProtoGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProtoFinanceCard("Расходы", protoMoney(t.expenses), "за период", ProtoText, Modifier.weight(1f))
                ProtoFinanceCard("Денежный поток", protoMoney(t.profit), "поступления − расходы", if (t.profit >= 0) ProtoGreen else ProtoRed, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProtoFinanceCard("Дебиторка", protoMoney(t.receivable), "деньги не подтверждены", if (t.receivable > 0) ProtoRed else ProtoText, Modifier.weight(1f))
                ProtoFinanceCard("Отсрочки", protoMoney(t.deferred), if (t.overdue > 0) "просрочено " + protoMoney(t.overdue) else "просрочек нет", if (t.deferred > 0) ProtoRed else ProtoText, Modifier.weight(1f))
            }
        }
        item {
            ProtoFinanceCard(
                "Ждут подтверждения админом", protoMoney(t.pendingSum), protoPlural(t.pendingCount, "запрос", "запроса", "запросов") + " от продаж",
                if (t.pendingSum > 0) ProtoRed else ProtoText, Modifier.fillMaxWidth(), onPayments
            )
        }
        item {
            ProtoSectionCard {
                Text("6 месяцев: отгружено и получено", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().height(150.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                    monthly.forEach { (ym, shipped, received) ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(Modifier.width(10.dp).fillMaxHeight(shipped.toFloat() / chartMax).background(ProtoGold.copy(alpha = .45f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                                Box(Modifier.width(10.dp).fillMaxHeight(received.toFloat() / chartMax).background(ProtoGreen, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                            }
                            Text(ym.format(DateTimeFormatter.ofPattern("LLL", ruLocale)).take(3), color = ProtoMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(ProtoGold.copy(alpha = .45f))); Text(" отгружено", color = ProtoMuted, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(10.dp).background(ProtoGreen)); Text(" получено", color = ProtoMuted, fontSize = 12.sp)
                }
            }
        }
        item { Text("Топ клиентов", color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (top.isEmpty()) item { ProtoSectionCard { Text("За период отгрузок нет", color = ProtoMuted) } }
        else item {
            ProtoSectionCard {
                top.forEach { (name, sum) ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(name, color = ProtoText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(protoMoney(sum), color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(ProtoBorder)) {
                        Box(Modifier.fillMaxWidth(sum.toFloat() / topMax).fillMaxHeight().background(ProtoGold))
                    }
                }
            }
        }
        item { Text("Отсрочки по срокам", color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (openDeferred.isEmpty()) item { ProtoSectionCard { Text("Открытых отсрочек нет", color = ProtoMuted) } }
        items(openDeferred, key = { "d" + it.id }) { sh ->
            val days = protoDaysTo(sh.dueDate)
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sh.clientName, color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(sh.orderId + " · оплата до " + sh.dueDate, color = ProtoMuted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(protoMoney(sh.amount), color = ProtoRed, fontWeight = FontWeight.Bold)
                        Text(protoDaysLabel(sh.dueDate), color = if (days != null && days < 0) ProtoRed else ProtoMuted, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Расходы", color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
        item { ProtoPrimaryButton("Добавить расход", { showAdd = true }) }
        if (periodExpenses.isEmpty()) item { ProtoSectionCard { Text("Расходов за период нет", color = ProtoMuted) } }
        items(periodExpenses, key = { "e" + it.id }) { ex ->
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ex.category, color = ProtoText, fontWeight = FontWeight.SemiBold)
                        Text(ex.date + if (ex.comment.isNotBlank()) " · " + ex.comment else "", color = ProtoMuted, fontSize = 12.sp, maxLines = 2)
                    }
                    Text(protoMoney(ex.amount), color = ProtoText, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { deleting = ex }) { Icon(Icons.Outlined.Close, contentDescription = "Удалить", tint = ProtoMuted) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (showAdd) {
        var category by remember { mutableStateOf(protoExpenseCategories.first()) }
        var amountText by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = ProtoPanel,
            title = { Text("Новый расход", color = ProtoText) },
            text = {
                Column {
                    protoExpenseCategories.chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEach { c ->
                                FilterChip(
                                    selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 12.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText)
                                )
                            }
                        }
                    }
                    ProtoField(amountText, { amountText = it.filter(Char::isDigit).take(9) }, "Сумма, ₽", KeyboardType.Number)
                    ProtoField(comment, { comment = it }, "Комментарий")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val a = amountText.toIntOrNull() ?: 0
                    if (a > 0) { onAddExpense(category, a, comment); showAdd = false }
                }) { Text("Сохранить", color = ProtoGold) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
}

internal fun protoPlural(n: Int, one: String, few: String, many: String): String {
    val m100 = n % 100; val m10 = n % 10
    val w = if (m100 in 11..14) many else when (m10) { 1 -> one; 2, 3, 4 -> few; else -> many }
    return "$n $w"
}

internal fun protoGuessType(name: String): String = name.lowercase().let {
    when { "корзин" in it -> "Корзины"; "полян" in it -> "Полянки"; "флорет" in it -> "Флоретки"; "венок" in it || "венк" in it -> "Венки"; else -> "Прочее" }
}

internal data class ProtoSkuStat(val sku: String, val name: String, val qty: Int, val sum: Int)

/** Что и сколько заказывал клиент: по группам товара и по артикулам, на какую сумму. */
@Composable
internal fun ProtoClientAnalyticsBlock(orders: List<ProtoOrder>, typeOf: (String) -> String, title: String = "Что заказывает клиент") {
    var showAll by remember { mutableStateOf(false) }
    val stats = orders.flatMap { it.lines }.groupBy { it.sku }
        .map { (sku, ls) -> ProtoSkuStat(sku, ls.first().name, ls.sumOf { it.qty }, ls.sumOf { it.lineTotal }) }
        .sortedWith(compareByDescending<ProtoSkuStat> { it.qty }.thenByDescending { it.sum })
    val totalQty = stats.sumOf { it.qty }
    val totalSum = stats.sumOf { it.sum }
    val groups = stats.groupBy { st -> typeOf(st.sku).takeIf { it != "Прочее" } ?: protoGuessType(st.name) }.map { (g, l) -> Triple(g, l.sumOf { it.qty }, l.sumOf { it.sum }) }.sortedByDescending { it.second }
    val maxQty = (stats.firstOrNull()?.qty ?: 1).coerceAtLeast(1)
    ProtoSectionCard {
        Text(title, color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
        if (stats.isEmpty()) {
            Text("Заказов пока нет", color = ProtoMuted, modifier = Modifier.padding(top = 6.dp))
        } else {
            ProtoInfoRow("Заказов", orders.size.toString())
            ProtoInfoRow("Заказано штук", totalQty.toString())
            ProtoInfoRow("На сумму", protoMoney(totalSum))
            groups.forEach { (g, q, s) -> ProtoInfoRow(g, q.toString() + " шт. · " + protoMoney(s)) }
            Text("По артикулам", color = ProtoText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            (if (showAll) stats else stats.take(6)).forEach { st ->
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(st.name, color = ProtoText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            ProtoSkuText(st.sku, fontSize = 12)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(st.qty.toString() + " шт.", color = ProtoText, fontWeight = FontWeight.Bold)
                            Text(protoMoney(st.sum), color = ProtoGoldSoft, fontSize = 12.sp)
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ProtoBorder)) {
                        Box(Modifier.fillMaxWidth(st.qty.toFloat() / maxQty).fillMaxHeight().background(ProtoGold))
                    }
                }
            }
            if (stats.size > 6) TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Свернуть" else "Показать все " + stats.size, color = ProtoGold) }
        }
    }
}
