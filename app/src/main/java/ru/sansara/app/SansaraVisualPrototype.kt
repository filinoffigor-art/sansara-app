package ru.sansara.app

import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
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
import java.text.NumberFormat
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import ru.sansara.app.ui.theme.*

private val ProtoBg = Bg
private val ProtoPanel = Panel
private val ProtoPanel2 = Panel2
private val ProtoGold = Gold
private val ProtoGoldSoft = GoldSoft
private val ProtoText = TextPrimary
private val ProtoMuted = TextSecondary
private val ProtoGreen = Success
private val ProtoRed = Error
private val ProtoBorder = Border
private val ProtoOrange = Warning

private enum class ProtoScreen {
    Welcome, Login, Registration, RegistrationSent,
    Home, Catalog, Filter, ProductList, ProductDetail, Cart, Checkout, OrderSent, OrderList, OrderDetail, Profile, Suspended,
    AdminHome, AdminSearch, AdminClients, AdminClient, AdminOrders, AdminOrderDetail, AdminCatalog, AdminSettings, AdminSettingsDetail, AdminAttention, OnlineController, LowStockList,
    Production, ProductionCategory, ProductionCatalog, ProductionEntry, ProductionHistory, ProductionReport, ProductionProfile,
    Server, StockList, ReserveList, NewClients, Export
}

private enum class ProtoClientType(val label: String) { AGENT("Агент"), TRADING("Торгующая организация") }

private data class ProtoCatalogProduct(
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

private data class ProtoProductionOp(
    val date: String,
    val time: String,
    val sku: String,
    val name: String,
    val qty: Int,
    val assembler: String,
    val postedBy: String,
    val status: String = "Проведен"
)

private data class ProtoProductionDraft(
    val product: ProtoCatalogProduct,
    val qty: Int,
    val assembler: String,
    val date: String = currentDateShort()
)

private data class ProtoClient(
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
    val address: String = ""
)

private data class ProtoOrderLine(val sku: String, val name: String, val qty: Int, val price: Int)
private data class ProtoOrderEvent(val status: String, val dateTime: String, val actor: String)

private data class ProtoOrder(
    val id: String,
    val clientName: String,
    val dateTime: String,
    val lines: List<ProtoOrderLine>,
    val status: String,
    val history: List<ProtoOrderEvent> = listOf(ProtoOrderEvent(status, dateTime, "Система")),
    val deliveryMethod: String = "Доставка",
    val deliveryAddress: String = "",
    val comment: String = ""
) {
    val pieces: Int get() = lines.sumOf { it.qty }
    val total: Int get() = lines.sumOf { it.qty * it.price }
}

private data class ProtoRegistration(
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
private fun currentMonthLabel(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() }

@Composable
fun SansaraVisualPrototype() {
    val context = LocalContext.current
    val products = remember { protoLoadProducts(context).toMutableStateList() }
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
    var selectedProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var detailQty by remember { mutableIntStateOf(1) }
    var selectedOrderId by remember { mutableStateOf("S-002384") }
    var selectedClientId by remember { mutableStateOf("C-1024") }
    var lastRegistration by remember { mutableStateOf<ProtoRegistration?>(null) }
    var productionCategory by remember { mutableStateOf("Венки") }
    var productionProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var productionQty by remember { mutableIntStateOf(1) }
    var productionAssembler by remember { mutableStateOf("Анна К.") }
    var selectedProductionDate by remember { mutableStateOf(LocalDate.now()) }
    var lowStockThreshold by remember { mutableIntStateOf(5) }
    var notificationsRegistration by remember { mutableStateOf(true) }
    var notificationsOrders by remember { mutableStateOf(true) }
    var notificationsProduction by remember { mutableStateOf(true) }
    var notificationsLowStock by remember { mutableStateOf(true) }
    var settingsSection by remember { mutableStateOf("Профиль компании") }
    var backupStatus by remember { mutableStateOf("Резервная копия ещё не создавалась") }
    var registrationsTotalThisMonth by remember { mutableIntStateOf(12) }
    val prefs = remember { context.getSharedPreferences("sansara", Context.MODE_PRIVATE) }
    var tildaFeedUrl by remember { mutableStateOf(prefs.getString("tilda_yml_url", BuildConfig.TILDA_YML_URL).orEmpty()) }
    var catalogSyncStatus by remember { mutableStateOf("Тестовый каталог · локальные данные") }
    var catalogSyncInProgress by remember { mutableStateOf(false) }
    var lastCatalogSync by remember { mutableStateOf(prefs.getString("last_catalog_sync", "Не выполнялась").orEmpty()) }
    var backendStatus by remember { mutableStateOf(if (BuildConfig.BACKEND_API_URL.isBlank()) "Backend API не настроен · события сохраняются локально" else "Backend API настроен") }
    val scope = rememberCoroutineScope()

    val clients = remember {
        mutableStateListOf(
            ProtoClient("C-1024", "ООО Ритуал-Сервис", "Игорь Петров", "+7 999 123-45-67", "Оптовик", 10, 286_400, 7, "1024", true, "сейчас", "info@ritual-service.ru", "Торгующая организация", "12.03.2023", true, "Москва", "г. Москва, ул. Ленинская, д. 10, стр. 2"),
            ProtoClient("C-1025", "Агент Смирнов А.А.", "Алексей Смирнов", "+7 916 222-18-44", "Активный", 5, 94_800, 3, "1025", true, "2 мин назад", "smirnov@example.ru", "Агент", "05.09.2026", true, "Тула", ""),
            ProtoClient("C-1026", "ООО Мемориал", "Анна Сергеева", "+7 903 555-34-11", "Приостановлен", 7, 121_500, 4, "1026", false, "вчера 18:41"),
            ProtoClient("C-1027", "Ритуал-Тула", "Сергей Орлов", "+7 920 701-20-80", "VIP", 15, 418_300, 11, "1027", true, "сейчас"),
            ProtoClient("C-1028", "Агент Ковалёв", "Дмитрий Ковалёв", "+7 905 330-45-21", "Приостановлен", 5, 48_200, 2, "1028", false, "3 дня назад"),
            ProtoClient("C-1029", "ООО Вечная память", "Ольга Волкова", "+7 977 300-19-50", "Приостановлен", 10, 173_900, 6, "1029", false, "5 дней назад")
        )
    }

    val registrations = remember {
        mutableStateListOf(
            ProtoRegistration("ООО Ритуал-Плюс", "Иванов Иван Иванович", "7100000001", "Иван Иванов", "+7 900 111-11-11", "ivanov@example.ru", "Тула", "ул. Ленина, 10", "Торгующая организация"),
            ProtoRegistration("Агент Петров", "Петров Пётр Петрович", "", "Пётр Петров", "+7 900 222-22-22", "petrov@example.ru", "Москва", "", "Агент"),
            ProtoRegistration("ООО Память", "Соколова Елена Викторовна", "7100000003", "Елена Соколова", "+7 900 333-33-33", "sokolova@example.ru", "Калуга", "ул. Мира, 5", "Торгующая организация")
        )
    }

    val orders = remember {
        mutableStateListOf(
            ProtoOrder("S-002384", "ООО Ритуал-Сервис", "28.09.2026 09:12", listOf(
                ProtoOrderLine("V-060-001", "Венок Премиум 60 см №01", 5, 3750),
                ProtoOrderLine("V-140-005", "Венок Эконом 140 см №05", 4, 2350)
            ), "Собирается", listOf(
                ProtoOrderEvent("Получен", "28.09.2026 09:12", "Игорь Петров"),
                ProtoOrderEvent("Подтверждён", "28.09.2026 09:24", "Администратор"),
                ProtoOrderEvent("Собирается", "28.09.2026 09:40", "Администратор")
            )),
            ProtoOrder("S-002383", "Ритуал-Тула", "28.09.2026 08:47", listOf(
                ProtoOrderLine("V-060-007", "Венок Стандарт 60 см №07", 8, 2150)
            ), "Подтверждён", listOf(
                ProtoOrderEvent("Получен", "28.09.2026 08:47", "Сергей Орлов"),
                ProtoOrderEvent("Подтверждён", "28.09.2026 08:58", "Администратор")
            )),
            ProtoOrder("S-002382", "Агент Смирнов А.А.", "27.09.2026 16:05", listOf(
                ProtoOrderLine("V-090-011", "Венок Премиум 90 см №11", 3, 3850),
                ProtoOrderLine("KOR-070-004", "Корзина 70 см №04", 2, 2450)
            ), "Доставляется", listOf(
                ProtoOrderEvent("Получен", "27.09.2026 16:05", "Алексей Смирнов"),
                ProtoOrderEvent("Подтверждён", "27.09.2026 16:18", "Администратор"),
                ProtoOrderEvent("Собирается", "27.09.2026 16:41", "Администратор"),
                ProtoOrderEvent("Доставляется", "27.09.2026 18:10", "Администратор")
            ))
        )
    }

    val productionOps = remember {
        mutableStateListOf(
            ProtoProductionOp(currentDateShort(), "08:12", "V-060-001", "Венок Премиум 60 см №01", 6, "Анна К.", "Игорь Ф."),
            ProtoProductionOp(currentDateShort(), "08:24", "V-060-007", "Венок Стандарт 60 см №07", 8, "Мария С.", "Игорь Ф."),
            ProtoProductionOp(currentDateShort(), "08:41", "V-140-005", "Венок Эконом 140 см №05", 5, "Елена П.", "Игорь Ф."),
            ProtoProductionOp("27.09.2026", "14:32", "V-060-010", "Венок Стандарт 60 см №10", 10, "Анна К.", "Петров И.А."),
            ProtoProductionOp("27.09.2026", "13:18", "V-125-009", "Венок Эконом 125 см №09", 7, "Мария С.", "Петров И.А.")
        )
    }
    val productionDrafts = remember { mutableStateListOf<ProtoProductionDraft>() }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun go(target: ProtoScreen) { if (target != screen) { history.add(screen); screen = target } }
    fun back() { screen = if (history.isNotEmpty()) history.removeAt(history.lastIndex) else ProtoScreen.Welcome }
    fun resetFilters(type: String? = null, availability: String? = null) {
        selectedTypes = type?.let { setOf(it) } ?: emptySet()
        selectedQualities = emptySet(); selectedSizes = emptySet()
        selectedAvailability = availability?.let { setOf(it) } ?: emptySet()
    }
    fun openProduct(p: ProtoCatalogProduct) { selectedProduct = p; detailQty = 1; go(ProtoScreen.ProductDetail) }
    fun physicalStock(p: ProtoCatalogProduct) = stockOverrides[p.sku] ?: p.stock
    fun reservedForSku(sku: String): Int = orders.filter { it.status != "Доставлен" }.sumOf { o -> o.lines.filter { it.sku == sku }.sumOf { it.qty } }
    fun availableStock(p: ProtoCatalogProduct) = (physicalStock(p) - reservedForSku(p.sku)).coerceAtLeast(0)
    fun currentClient(): ProtoClient = clients.firstOrNull { it.id == selectedClientId } ?: clients.first()
    fun discountedPrice(p: ProtoCatalogProduct): Int = p.price * (100 - currentClient().discount) / 100
    fun nextAccessCode(): String = ((clients.mapNotNull { it.accessCode.toIntOrNull() }.maxOrNull() ?: 1029) + 1).toString()
    fun postDrafts(date: String) {
        val selectedDrafts = productionDrafts.filter { it.date == date }
        if (selectedDrafts.isEmpty()) { toast("Добавьте позиции в выпуск выбранного дня"); return }
        selectedDrafts.forEach { d ->
            stockOverrides[d.product.sku] = physicalStock(d.product) + d.qty
            productionOps.add(0, ProtoProductionOp(date, currentTimeShort(), d.product.sku, d.product.name, d.qty, d.assembler, "Игорь Ф."))
        }
        val total = selectedDrafts.sumOf { it.qty }
        productionDrafts.removeAll(selectedDrafts.toSet())
        toast("Оприходовано на склад: $total шт.")
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
                if (showToast) toast(catalogSyncStatus)
            } catch (e: Exception) {
                catalogSyncStatus = "Ошибка синхронизации: ${e.message ?: "неизвестная ошибка"}"
                if (showToast) toast(catalogSyncStatus)
            } finally { catalogSyncInProgress = false }
        }
    }

    fun sendRegistrationEvent(reg: ProtoRegistration) {
        if (BuildConfig.BACKEND_API_URL.isBlank()) return
        scope.launch {
            val result = SansaraBackend.postEvent(BuildConfig.BACKEND_API_URL, "registration", mapOf(
                "registrationId" to reg.id, "organization" to reg.organization, "inn" to reg.inn, "type" to reg.type,
                "contact1" to reg.contact1, "phone1" to reg.phone1, "email" to reg.email, "contact2" to reg.contact2, "phone2" to reg.phone2, "email2" to reg.email2,
                "city" to reg.city, "address" to reg.address, "createdAt" to reg.createdAt
            ))
            backendStatus = if (result.ok) "Регистрация передана backend / Telegram" else result.message
        }
    }

    fun sendOrderEvent(order: ProtoOrder) {
        if (BuildConfig.BACKEND_API_URL.isBlank()) return
        scope.launch {
            val result = SansaraBackend.postEvent(BuildConfig.BACKEND_API_URL, "order", mapOf(
                "orderId" to order.id, "client" to order.clientName, "dateTime" to order.dateTime, "status" to order.status,
                "pieces" to order.pieces, "total" to order.total, "deliveryMethod" to order.deliveryMethod, "deliveryAddress" to order.deliveryAddress, "comment" to order.comment,
                "lines" to order.lines.joinToString(" | ") { "${it.sku}:${it.qty}:${it.price}" }
            ))
            backendStatus = if (result.ok) "Заказ передан backend / Telegram" else result.message
        }
    }

    LaunchedEffect(Unit) {
        prefs.getString("last_client_id", null)?.let { savedId ->
            if (clients.any { it.id == savedId }) { selectedClientId = savedId; screen = ProtoScreen.Home }
        }
        if (tildaFeedUrl.isNotBlank()) syncTildaCatalog(showToast = false)
    }

    BackHandler(enabled = screen != ProtoScreen.Welcome) { back() }

    SansaraTheme {
        when (screen) {
            ProtoScreen.Welcome -> ProtoWelcomeScreen(onLogin = { go(ProtoScreen.Login) }, onRegister = { go(ProtoScreen.Registration) }, onRole = { showRolePicker = true })
            ProtoScreen.Login -> ProtoLoginScreen(onBack = { back() }, onLogin = { code ->
                val client = clients.firstOrNull { it.accessCode == code }
                if (client != null) {
                    selectedClientId = client.id
                    prefs.edit().putString("last_client_id", client.id).apply()
                    go(ProtoScreen.Home)
                } else toast("Код доступа не найден")
            })
            ProtoScreen.Registration -> ProtoRegistrationScreen(onBack = { back() }, onSubmit = { reg ->
                registrations.add(0, reg)
                lastRegistration = reg
                registrationsTotalThisMonth += 1
                sendRegistrationEvent(reg)
                go(ProtoScreen.RegistrationSent)
            })
            ProtoScreen.RegistrationSent -> ProtoRegistrationSentScreen(lastRegistration, onBack = { history.clear(); screen = ProtoScreen.Welcome })

            ProtoScreen.Home -> ProtoClientHomeScreen(
                client = clients.firstOrNull { it.id == selectedClientId } ?: clients.first(),
                products = products,
                stockOverrides = stockOverrides,
                availableStock = { availableStock(it) },
                cartCount = cart.values.sum(),
                query = searchQuery,
                onQuery = { searchQuery = it },
                onSearch = {
                    resetFilters();
                    go(ProtoScreen.ProductList)
                },
                onAvailability = { status -> resetFilters(availability = status); go(ProtoScreen.ProductList) },
                onCategory = { type ->
                    if (type != "Венки") toast("Раздел «$type» в разработке")
                    else { resetFilters(type = type); go(ProtoScreen.Filter) }
                },
                onOpenProduct = { openProduct(it) },
                onCart = { go(ProtoScreen.Cart) },
                onOrders = { go(ProtoScreen.OrderList) },
                onProfile = { if (currentClient().status == "Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Profile) },
                onCatalog = { go(ProtoScreen.Catalog) },
                onSeeAll = { searchQuery = ""; resetFilters(); go(ProtoScreen.ProductList) }
            )
            ProtoScreen.Catalog -> ProtoCatalogHomeScreen(
                cartCount = cart.values.sum(),
                onBack = { back() },
                onSearch = { value -> searchQuery = value; resetFilters(); go(ProtoScreen.ProductList) },
                onCategory = { type ->
                    if (type != "Венки") toast("Раздел «$type» в разработке")
                    else { resetFilters(type = "Венки"); go(ProtoScreen.Filter) }
                },
                onAvailability = { status -> searchQuery = ""; if (status == "Все") resetFilters() else resetFilters(availability = status); go(ProtoScreen.ProductList) },
                onHome = { history.clear(); screen = ProtoScreen.Home }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { if (currentClient().status == "Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Profile) }
            )
            ProtoScreen.Filter -> ProtoFilterScreen(
                selectedTypes, selectedQualities, selectedSizes, selectedAvailability,
                onToggleType = { if (it != "Венки") toast("Раздел «$it» в разработке") else selectedTypes = protoToggle(selectedTypes, it) },
                onToggleQuality = { selectedQualities = protoToggle(selectedQualities, it) },
                onToggleSize = { selectedSizes = protoToggle(selectedSizes, it) },
                onToggleAvailability = { selectedAvailability = protoToggle(selectedAvailability, it) },
                onShow = { searchQuery = ""; go(ProtoScreen.ProductList) },
                onBack = { back() }
            )
            ProtoScreen.ProductList -> {
                val filtered = products.filter { p ->
                    (searchQuery.isBlank() || p.sku.contains(searchQuery, true) || p.name.contains(searchQuery, true)) &&
                    (selectedTypes.isEmpty() || p.type in selectedTypes) &&
                    (selectedQualities.isEmpty() || p.quality == "—" || p.quality in selectedQualities) &&
                    (selectedSizes.isEmpty() || p.size in selectedSizes) &&
                    (selectedAvailability.isEmpty() || (if (availableStock(p) > 0) "В наличии" else "Под заказ") in selectedAvailability)
                }
                ProtoProductListScreen(filtered, cart, stockOverrides, availableStock = { availableStock(it) }, discount = currentClient().discount, onBack = { back() }, onOpenFilter = { go(ProtoScreen.Filter) }, onOpenProduct = { openProduct(it) }, onCart = { go(ProtoScreen.Cart) }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { history.clear(); screen = ProtoScreen.Catalog }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { go(ProtoScreen.Profile) })
            }
            ProtoScreen.ProductDetail -> ProtoProductDetailScreen(
                product = selectedProduct,
                currentStock = selectedProduct?.let { availableStock(it) } ?: 0,
                qty = detailQty,
                discount = currentClient().discount,
                cartCount = cart.values.sum(),
                onBack = { back() },
                onMinus = { detailQty = (detailQty - 1).coerceAtLeast(1) },
                onPlus = { detailQty += 1 },
                onAdd = {
                    if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) {
                        toast("Оформление заказов временно приостановлено")
                        go(ProtoScreen.Suspended)
                    } else {
                        selectedProduct?.let { p -> cart[p.sku] = (cart[p.sku] ?: 0) + detailQty; toast("Добавлено в корзину: ${detailQty} шт.") }
                        back()
                    }
                },
                onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { go(ProtoScreen.Catalog) }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { if (currentClient().status == "Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Profile) }
            )
            ProtoScreen.Cart -> ProtoCartScreen(products, cart, discount = currentClient().discount, onBack = { back() }, onPlus = { p -> cart[p.sku] = (cart[p.sku] ?: 0) + 1 }, onMinus = { p -> val n = (cart[p.sku] ?: 1) - 1; if (n <= 0) cart.remove(p.sku) else cart[p.sku] = n }, onDelete = { cart.remove(it.sku) }, onCheckout = {
                if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) go(ProtoScreen.Suspended) else go(ProtoScreen.Checkout)
            }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { go(ProtoScreen.Catalog) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { if (currentClient().status == "Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Profile) })
            ProtoScreen.Checkout -> ProtoCheckoutScreen(cartCount = cart.values.sum(), total = cart.entries.sumOf { (sku, q) -> products.firstOrNull { it.sku == sku }?.let { discountedPrice(it) * q } ?: 0 }, discount = currentClient().discount, defaultAddress = currentClient().address, onBack = { back() }, onSubmit = { method, address, comment ->
                if (currentClient().status == "Приостановлен" || !currentClient().orderingEnabled) {
                    go(ProtoScreen.Suspended)
                } else {
                    val lines = cart.mapNotNull { (sku,q) -> products.firstOrNull { it.sku == sku }?.let { ProtoOrderLine(it.sku,it.name,q,discountedPrice(it)) } }
                    val id = "S-${(2385 + orders.size).toString().padStart(6,'0')}"
                    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    val newOrder = ProtoOrder(id, currentClient().name, now, lines, "Получен", listOf(ProtoOrderEvent("Получен", now, currentClient().contact)), method, address, comment)
                    orders.add(0, newOrder)
                    sendOrderEvent(newOrder)
                    selectedOrderId = id; cart.clear(); go(ProtoScreen.OrderSent)
                }
            }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { go(ProtoScreen.Catalog) }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { if (currentClient().status == "Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Profile) })
            ProtoScreen.OrderSent -> ProtoOrderSentScreen(orders.firstOrNull { it.id == selectedOrderId }, onView = { go(ProtoScreen.OrderDetail) }, onCatalog = { history.clear(); screen = ProtoScreen.Catalog }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) }, onProfile = { if (currentClient().status == "Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Profile) })
            ProtoScreen.OrderList -> ProtoOrderListScreen(orders.filter { it.clientName == (clients.firstOrNull { c -> c.id == selectedClientId }?.name ?: "") }, onBack = { back() }, onOpen = { selectedOrderId = it.id; go(ProtoScreen.OrderDetail) }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { history.clear(); screen = ProtoScreen.Catalog }, onCart = { go(ProtoScreen.Cart) }, onProfile = { go(ProtoScreen.Profile) })
            ProtoScreen.OrderDetail -> ProtoOrderDetailScreen(
                order = orders.firstOrNull { it.id == selectedOrderId },
                isAdmin = false,
                onBack = { back() },
                onStatus = {},
                onRepeat = {
                    orders.firstOrNull { it.id == selectedOrderId }?.let { order ->
                        cart.clear()
                        order.lines.forEach { line -> cart[line.sku] = line.qty }
                        toast("Заказ " + order.id + " добавлен повторно")
                        go(ProtoScreen.Checkout)
                    }
                },
                onEdit = {
                    orders.firstOrNull { it.id == selectedOrderId }?.let { order ->
                        cart.clear()
                        order.lines.forEach { line -> cart[line.sku] = line.qty }
                        toast("Состав заказа перенесён в корзину")
                        go(ProtoScreen.Cart)
                    }
                }
            )
            ProtoScreen.Profile -> ProtoProfileScreen(clients.firstOrNull { it.id == selectedClientId } ?: clients.first(), onBack = { back() }, onCall = { protoDial(context) }, onLogout = { prefs.edit().remove("last_client_id").apply(); history.clear(); screen = ProtoScreen.Welcome }, onHome = { history.clear(); screen = ProtoScreen.Home }, onCatalog = { history.clear(); screen = ProtoScreen.Catalog }, onCart = { go(ProtoScreen.Cart) }, onOrders = { go(ProtoScreen.OrderList) })
            ProtoScreen.Suspended -> ProtoSuspendedScreen(onCall = { protoDial(context) }, onMessage = { protoMessage(context) }, onBack = { back() }, onCatalog = { go(ProtoScreen.Catalog) }, onHome = { history.clear(); screen = ProtoScreen.Home }, onOrders = { go(ProtoScreen.OrderList) })

            ProtoScreen.AdminHome -> ProtoAdminHomeScreen(
                registrationsTotal = registrationsTotalThisMonth, clients = clients, orders = orders, productionOps = productionOps, products = products, stockOverrides = stockOverrides, lowStockThreshold = lowStockThreshold,
                reservedForSku = { reservedForSku(it) },
                onSearch = { go(ProtoScreen.AdminSearch) },
                onRegistrations = { go(ProtoScreen.AdminAttention) }, onClients = { go(ProtoScreen.AdminClients) }, onOrders = { go(ProtoScreen.AdminOrders) },
                onProduction = { go(ProtoScreen.Production) }, onStock = { go(ProtoScreen.Server) }, onCatalog = { go(ProtoScreen.AdminCatalog) }, onSettings = { go(ProtoScreen.AdminSettings) },
                onAttention = { go(ProtoScreen.AdminAttention) }, onOnline = { go(ProtoScreen.OnlineController) }, onLowStock = { go(ProtoScreen.LowStockList) }
            )
            ProtoScreen.AdminSearch -> ProtoAdminSearchScreen(clients, orders, products, stockOverrides, onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) }, onOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) }, onProduct = { selectedProduct = it; go(ProtoScreen.ProductDetail) })
            ProtoScreen.AdminClients -> ProtoAdminClientsScreen(clients, onBack = { back() }, onOpen = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.AdminClient -> ProtoAdminClientScreen(
                client = clients.firstOrNull { it.id == selectedClientId }, onBack = { back() },
                onStatus = { status -> val i=clients.indexOfFirst{it.id==selectedClientId};if(i>=0){val c=clients[i];clients[i]=c.copy(status=status,orderingEnabled=if(status=="Приостановлен")false else c.orderingEnabled)} },
                onToggleBlock = { val i=clients.indexOfFirst{it.id==selectedClientId};if(i>=0){val c=clients[i];clients[i]=c.copy(status=if(c.status=="Приостановлен")"Активный" else "Приостановлен",orderingEnabled=c.status=="Приостановлен")} },
                onToggleOrdering = { val i=clients.indexOfFirst{it.id==selectedClientId};if(i>=0){val c=clients[i];clients[i]=c.copy(orderingEnabled=!c.orderingEnabled)} },
                onDiscount = { delta -> val i = clients.indexOfFirst { it.id == selectedClientId }; if (i >= 0) { val c = clients[i]; clients[i] = c.copy(discount = (c.discount + delta).coerceIn(0,50)) } },
                onSave = { toast("Карточка клиента сохранена") },
                onCall = { protoDialNumber(context, clients.firstOrNull { it.id == selectedClientId }?.phone ?: BuildConfig.ADMIN_PHONE) }
            )
            ProtoScreen.AdminOrders -> ProtoAdminOrdersScreen(orders, onBack = { back() }, onOpen = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) })
            ProtoScreen.AdminOrderDetail -> ProtoOrderDetailScreen(orders.firstOrNull { it.id == selectedOrderId }, isAdmin = true, onBack = { back() }, onStatus = { st ->
                val idx = orders.indexOfFirst { it.id == selectedOrderId }
                if (idx >= 0) {
                    val oldOrder = orders[idx]
                    val statuses = listOf("Получен","Подтверждён","Собирается","Доставляется","Доставлен")
                    val oldIndex = statuses.indexOf(oldOrder.status)
                    val newIndex = statuses.indexOf(st)
                    if (st == oldOrder.status) {
                        toast("Статус уже установлен")
                    } else if (newIndex >= oldIndex && newIndex >= 0) {
                        if (st == "Доставлен" && oldOrder.status != "Доставлен") {
                            oldOrder.lines.forEach { line ->
                                products.firstOrNull { it.sku == line.sku }?.let { p -> stockOverrides[p.sku] = (physicalStock(p) - line.qty).coerceAtLeast(0) }
                            }
                        }
                        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        orders[idx] = oldOrder.copy(status = st, history = oldOrder.history + ProtoOrderEvent(st, now, "Администратор"))
                    } else toast("Статус заказа нельзя переводить назад")
                }
            })
            ProtoScreen.AdminCatalog -> ProtoAdminCatalogScreen(products, stockOverrides, onBack = { back() })
            ProtoScreen.AdminSettings -> ProtoAdminSettingsMenuScreen(
                syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, lowStockThreshold = lowStockThreshold,
                onBack = { back() }, onOpen = { section ->
                    when (section) {
                        "Клиенты" -> go(ProtoScreen.AdminClients)
                        "Экспорт данных" -> go(ProtoScreen.Export)
                        else -> { settingsSection = section; go(ProtoScreen.AdminSettingsDetail) }
                    }
                }, onCall = { protoDial(context) }
            )
            ProtoScreen.AdminSettingsDetail -> ProtoAdminSettingsDetailScreen(
                section = settingsSection, threshold = lowStockThreshold, reg = notificationsRegistration, orders = notificationsOrders, prod = notificationsProduction, low = notificationsLowStock,
                tildaUrl = tildaFeedUrl, syncStatus = catalogSyncStatus, lastSync = lastCatalogSync, syncing = catalogSyncInProgress, backupStatus = backupStatus, backendStatus = backendStatus,
                onBack = { back() }, onThreshold = { lowStockThreshold = it.coerceIn(1,50) }, onReg = { notificationsRegistration = it }, onOrders = { notificationsOrders = it }, onProd = { notificationsProduction = it }, onLow = { notificationsLowStock = it },
                onTildaUrl = { tildaFeedUrl = it }, onSync = { syncTildaCatalog() }, onBackup = { backupStatus = "Последняя копия: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}" }, onClients = { go(ProtoScreen.AdminClients) }
            )
            ProtoScreen.AdminAttention -> ProtoAttentionScreen(
                regs = registrations, blocked = clients.filter { it.status == "Приостановлен" }, picking = orders.filter { it.status == "Собирается" },
                lowStock = products.filter { availableStock(it) <= lowStockThreshold }, stockValue = { availableStock(it) },
                onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) }, onOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) },
                onApprove = { reg ->
                    val code = nextAccessCode()
                    val id = "C-${code}"
                    clients.add(0, ProtoClient(id, reg.organization, reg.contact1, reg.phone1, "Активный", 0, 0, 0, code, false, "ещё не входил", reg.email, reg.type, currentDateShort(), true, reg.city, reg.address))
                    registrations.remove(reg)
                    toast("Клиент подтверждён. Код доступа: $code")
                },
                onClose = { reg -> registrations.remove(reg) }, onLowStock = { go(ProtoScreen.LowStockList) }
            )
            ProtoScreen.OnlineController -> ProtoOnlineControllerScreen(clients, onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.LowStockList -> ProtoLowStockListScreen(products, stockOverrides, reservedForSku = { reservedForSku(it) }, threshold = lowStockThreshold, onBack = { back() })

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
                    onStock = { go(ProtoScreen.Server) },
                    onHome = { toast("Главный экран производства") },
                    onProfile = { go(ProtoScreen.ProductionProfile) }
                )
            }
            ProtoScreen.ProductionCategory -> ProtoProductionCategoryScreen(onBack = { back() }, onCategory = { productionCategory = it; go(ProtoScreen.ProductionCatalog) })
            ProtoScreen.ProductionCatalog -> ProtoProductionCatalogScreen(products.filter { it.type == productionCategory }, onBack = { back() }, onSelect = { productionProduct = it; productionQty = 1; go(ProtoScreen.ProductionEntry) })
            ProtoScreen.ProductionEntry -> {
                val productionDateKey = selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ProtoProductionEntryScreen(
                    productionProduct, productionQty, productionAssembler,
                    selectedDate = selectedProductionDate,
                    onBack = { back() },
                    onMinus = { productionQty = (productionQty - 1).coerceAtLeast(1) },
                    onPlus = { productionQty += 1 },
                    onAssembler = { productionAssembler = it },
                    onAddDraft = {
                        productionProduct?.let { p ->
                            productionDrafts.add(ProtoProductionDraft(p, productionQty, productionAssembler, productionDateKey))
                            toast("Позиция добавлена в выпуск выбранного дня")
                        }
                        history.clear(); screen = ProtoScreen.Production
                    },
                    onPostNow = {
                        productionProduct?.let { p ->
                            stockOverrides[p.sku] = physicalStock(p) + productionQty
                            productionOps.add(0, ProtoProductionOp(productionDateKey, currentTimeShort(), p.sku, p.name, productionQty, productionAssembler, "Игорь Ф."))
                            toast("Оприходовано: " + p.name + " +" + productionQty + " шт.")
                        }
                        history.clear(); screen = ProtoScreen.Production
                    }
                )
            }
            ProtoScreen.ProductionHistory -> ProtoProductionHistoryScreen(productionOps, products, onBack = { back() })
            ProtoScreen.ProductionReport -> {
                val productionDateKey = selectedProductionDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ProtoProductionReportScreen(selectedProductionDate, productionOps.filter { it.date == productionDateKey }, products, onBack = { back() })
            }
            ProtoScreen.ProductionProfile -> ProtoStaffProfileScreen(role = "Производство", onBack = { back() }, onCall = { protoDial(context) }, onLogout = { history.clear(); screen = ProtoScreen.Welcome })

            ProtoScreen.Server -> ProtoServerScreen(products, stockOverrides, productionOps, orders, clients, reservedForSku = { reservedForSku(it) }, onBack = { back() }, onProduced = { go(ProtoScreen.ProductionHistory) }, onStock = { go(ProtoScreen.StockList) }, onReserve = { go(ProtoScreen.ReserveList) }, onNewClients = { go(ProtoScreen.NewClients) }, onOnline = { go(ProtoScreen.OnlineController) }, onExport = { go(ProtoScreen.Export) })
            ProtoScreen.StockList -> ProtoStockListScreen(products, stockOverrides, reservedForSku = { reservedForSku(it) }, onBack = { back() })
            ProtoScreen.ReserveList -> ProtoReserveListScreen(orders, onBack = { back() })
            ProtoScreen.NewClients -> ProtoNewClientsScreen(clients, onBack = { back() }, onOpen = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.Export -> ProtoExportScreen(onBack = { back() }, onExport = { report -> protoExportCsv(context, report, products, stockOverrides, orders, clients, productionOps, reservedForSku = { reservedForSku(it) }, toast = { toast(it) }) })
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

@Composable
private fun ProtoWelcomeScreen(onLogin:()->Unit,onRegister:()->Unit,onRole:()->Unit){
    val context=LocalContext.current
    ProtoBackground{
        Column(Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally){
            ProtoBrandHeader(onLogoClick=onRole)
            Card(Modifier.fillMaxWidth().weight(.52f),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder)){
                Box(Modifier.fillMaxSize()){Image(painterResource(R.drawable.mock_wreath),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.25f)));Text("УВАЖЕНИЕ\nВ КАЖДОЙ\nДЕТАЛИ",color=ProtoGoldSoft,fontSize=11.sp,letterSpacing=2.sp,lineHeight=17.sp,modifier=Modifier.align(Alignment.CenterStart).padding(22.dp))}
            }
            Spacer(Modifier.height(14.dp))
            Text("Добро пожаловать",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth())
            Text("Работаем с агентами и торговыми организациями. Заказывайте продукцию, отслеживайте наличие и оформляйте поставки в одном приложении.",color=ProtoMuted,fontSize=14.sp,lineHeight=20.sp,modifier=Modifier.fillMaxWidth().padding(top=6.dp,bottom=13.dp))
            ProtoPrimaryButton("Войти",onLogin);Spacer(Modifier.height(9.dp));ProtoSecondaryButton("Стать партнёром",onRegister)
            Text("Доступ к ценам и заказам — после регистрации и подтверждения.",color=ProtoMuted,fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(top=8.dp))
            TextButton(onClick={protoDial(context)}){Icon(Icons.Outlined.HeadsetMic,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Связаться с менеджером",color=ProtoGoldSoft)}
        }
    }
}

@Composable
private fun ProtoLoginScreen(onBack:()->Unit,onLogin:(String)->Unit) {
    var code by remember { mutableStateOf("") }
    ProtoScaffold(title="Вход", subtitle="Код доступа выдаёт администратор после подтверждения регистрации", onBack=onBack) {
        item { Spacer(Modifier.height(18.dp)); ProtoSectionCard { Text("Введите код доступа",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); ProtoField(code,{code=it},"Код доступа",keyboardType=KeyboardType.Number); Text("Для теста: 1024",color=ProtoMuted,fontSize=12.sp,modifier=Modifier.padding(top=8.dp)); Spacer(Modifier.height(14.dp)); Button(onClick={onLogin(code.trim())},enabled=code.isNotBlank(),modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Войти",color=Color.Black,fontWeight=FontWeight.Bold)} } }
    }
}

@Composable
private fun ProtoRegistrationScreen(onBack:()->Unit,onSubmit:(ProtoRegistration)->Unit){
    var organization by remember{mutableStateOf("")};var inn by remember{mutableStateOf("")};var contact1 by remember{mutableStateOf("")};var phone1 by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var type by remember{mutableStateOf(ProtoClientType.AGENT.label)};var consent by remember{mutableStateOf(false)};var second by remember{mutableStateOf(false)};var contact2 by remember{mutableStateOf("")};var phone2 by remember{mutableStateOf("")}
    val valid=organization.isNotBlank()&&inn.isNotBlank()&&contact1.isNotBlank()&&phone1.isNotBlank()&&email.isNotBlank()&&city.isNotBlank()&&consent
    ProtoBackground{
        Column(Modifier.fillMaxSize()){
            ProtoBrandHeader(onBack=onBack)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=18.dp,vertical=5.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                item{Text("Регистрация партнёра",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Заполните данные для доступа к каталогу и заказам",color=ProtoMuted,fontSize=13.sp)}
                item{ProtoField(organization,{organization=it},"Название организации / ФИО")}
                item{ProtoField(inn,{inn=it},"ИНН",KeyboardType.Number)}
                item{Box{ProtoField(contact1,{contact1=it},"Контактное лицо");IconButton(onClick={second=!second},modifier=Modifier.align(Alignment.CenterEnd).padding(end=8.dp).size(38.dp).background(ProtoGold,CircleShape)){Icon(if(second)Icons.Outlined.Remove else Icons.Outlined.Add,null,tint=Color.Black)}}}
                if(second)item{ProtoSectionCard{Text("Дополнительное контактное лицо",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);ProtoField(contact2,{contact2=it},"ФИО");ProtoField(phone2,{phone2=it},"Телефон",KeyboardType.Phone)}}
                item{ProtoField(phone1,{phone1=it},"Телефон",KeyboardType.Phone)}
                item{ProtoField(email,{email=it},"E-mail",KeyboardType.Email)}
                item{ProtoField(city,{city=it},"Город")}
                item{ProtoField(address,{address=it},"Адрес доставки")}
                item{Text("Тип клиента",color=ProtoText,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(5.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(ProtoClientType.AGENT.label,ProtoClientType.TRADING.label).forEach{option->val sel=type==option;Surface(Modifier.weight(1f).height(50.dp).clickable{type=option},color=if(sel)ProtoGold else ProtoPanel,border=BorderStroke(1.dp,if(sel)ProtoGold else ProtoBorder),shape=RoundedCornerShape(24.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(option,color=if(sel)Color.Black else ProtoText,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}}}}
                item{Row(Modifier.fillMaxWidth().clickable{consent=!consent}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(26.dp).background(if(consent)ProtoGold else ProtoPanel,RoundedCornerShape(7.dp)).border(1.dp,if(consent)ProtoGold else ProtoBorder,RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center){if(consent)Icon(Icons.Outlined.Check,null,tint=Color.Black,modifier=Modifier.size(18.dp))};Spacer(Modifier.width(10.dp));Text("Согласен с условиями обработки данных",color=ProtoText,fontSize=12.sp)}}
                item{Button(onClick={if(valid)onSubmit(ProtoRegistration(organization,organization,inn,contact1,phone1,email,city,address,type,contact2,phone2,""))},enabled=valid,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold,disabledContainerColor=ProtoPanel2),shape=RoundedCornerShape(24.dp)){Text("Отправить заявку",color=if(valid)Color.Black else ProtoMuted,fontWeight=FontWeight.Bold)}}
            }
        }
    }
}

@Composable
private fun ProtoClientHomeScreen(client:ProtoClient,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,availableStock:(ProtoCatalogProduct)->Int,cartCount:Int,query:String,onQuery:(String)->Unit,onSearch:()->Unit,onAvailability:(String)->Unit,onCategory:(String)->Unit,onOpenProduct:(ProtoCatalogProduct)->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit,onCatalog:()->Unit,onSeeAll:()->Unit){
    var mode by remember{mutableStateOf("Все")};var searchOpen by remember{mutableStateOf(false)}
    val visible=products.filter{p->when(mode){"В наличии"->availableStock(p)>0;"Под заказ"->availableStock(p)<=0;else->true}}
    val cats=listOf("Венки","Гробы","Одежда","Ленты","Цветы","Услуги")
    ProtoBackground{
        Column(Modifier.fillMaxSize()){
            ProtoBrandHeader(showBell=true)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=5.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
                item{Text("Здравствуйте, "+client.contact.split(" ").firstOrNull().orEmpty(),color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold);Text("Статус: "+client.status+"  ·  Скидка "+client.discount.toString()+"%",color=ProtoGoldSoft,fontSize=14.sp)}
                item{Surface(Modifier.fillMaxWidth().height(58.dp).clickable{searchOpen=true},color=ProtoPanel,border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(29.dp)){Row(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Search,null,tint=ProtoGold);Spacer(Modifier.width(12.dp));Text(if(query.isBlank())"Поиск по артикулу, названию" else query,color=if(query.isBlank())ProtoMuted else ProtoText)}}}
                item{ProtoAvailabilityChips(mode){mode=it}}
                item{LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(cats){cat->val enabled=cat=="Венки";Card(Modifier.width(118.dp).height(144.dp).clickable(enabled=enabled){onCategory(cat)},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder)){Box(Modifier.fillMaxSize()){Image(painterResource(protoPlaceholderForType(cat)),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(enabled).22f else .48f)));Text(cat,color=ProtoText,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp));if(!enabled)Text("В разработке",color=ProtoMuted,fontSize=10.sp,modifier=Modifier.align(Alignment.CenterEnd).padding(end=2.dp).width(74.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}}}}
                item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Популярные товары",color=ProtoText,fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));TextButton(onClick=onSeeAll){Text("Смотреть все →",color=ProtoGoldSoft)}}}
                items(visible.take(8).chunked(2)){row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{p->Box(Modifier.weight(1f)){ProtoProductCard(p,availableStock(p),client.discount){onOpenProduct(p)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}
            }
            ProtoClientBottomBar(ProtoScreen.Home,cartCount,onHome={},onCatalog,onCart,onOrders,onProfile)
        }
    }
    if(searchOpen)AlertDialog(onDismissRequest={searchOpen=false},containerColor=ProtoPanel,title={Text("Поиск",color=ProtoText)},text={ProtoField(query,onQuery,"Артикул или название")},confirmButton={TextButton(onClick={searchOpen=false;onSearch()}){Text("Найти",color=ProtoGold)}})
}

@Composable
private fun ProtoCatalogHomeScreen(cartCount:Int,onBack:()->Unit,onSearch:(String)->Unit,onCategory:(String)->Unit,onAvailability:(String)->Unit,onHome:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    var q by remember{mutableStateOf("")};var show by remember{mutableStateOf(false)};var mode by remember{mutableStateOf("Все")}
    val cats=listOf(Pair("Венки",126),Pair("Гробы",48),Pair("Одежда",73),Pair("Ленты",92),Pair("Цветы",64),Pair("Услуги",28))
    ProtoBackground{
        Column(Modifier.fillMaxSize()){
            ProtoBrandHeader(onBack=onBack,showBell=true)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=5.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
                item{Text("Каталог",color=ProtoText,fontSize=34.sp,fontWeight=FontWeight.Bold)}
                item{Surface(Modifier.fillMaxWidth().height(58.dp).clickable{show=true},color=ProtoPanel,border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(29.dp)){Row(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Search,null,tint=ProtoGold);Spacer(Modifier.width(12.dp));Text("Поиск по категориям",color=ProtoMuted)}}}
                item{ProtoAvailabilityChips(mode){mode=it;if(it!="Все")onAvailability(it)}}
                items(cats.chunked(2)){row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{pair->val enabled=pair.first=="Венки";Card(Modifier.weight(1f).height(224.dp).clickable(enabled=enabled){onCategory(pair.first)},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder)){Box(Modifier.fillMaxSize()){Image(painterResource(protoPlaceholderForType(pair.first)),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(enabled).20f else .45f)));Column(Modifier.align(Alignment.BottomStart).padding(12.dp)){Text(pair.first,color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);Text(pair.second.toString()+" позиций",color=ProtoMuted,fontSize=13.sp)};if(!enabled)Text("В разработке",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.align(Alignment.CenterEnd).padding(end=3.dp).width(76.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}}}}
            }
            ProtoClientBottomBar(ProtoScreen.Catalog,cartCount,onHome,onCatalog={},onCart,onOrders,onProfile)
        }
    }
    if(show)AlertDialog(onDismissRequest={show=false},containerColor=ProtoPanel,title={Text("Поиск по каталогу",color=ProtoText)},text={ProtoField(q,{q=it},"Категория, артикул или название")},confirmButton={TextButton(onClick={if(q.isNotBlank()){show=false;onSearch(q)}}){Text("Найти",color=ProtoGold)}})
}

@Composable
private fun ProtoFilterScreen(selectedTypes:Set<String>,selectedQualities:Set<String>,selectedSizes:Set<String>,selectedAvailability:Set<String>,onToggleType:(String)->Unit,onToggleQuality:(String)->Unit,onToggleSize:(String)->Unit,onToggleAvailability:(String)->Unit,onShow:()->Unit,onBack:()->Unit) {
    var step by remember { mutableIntStateOf(0) }
    val titles = listOf("Выберите продукцию","Качество","Размер","Наличие")
    val hints = listOf("Выберите категорию для продолжения","Можно отметить несколько вариантов","Выберите один или несколько размеров","Какие позиции показать")
    val sizeOptions = when {
        selectedTypes == setOf("Корзины") -> listOf("30 см","70 см","100 см")
        "Корзины" in selectedTypes -> listOf("30 см","60 см","70 см","90 см","100 см","110 см","125 см","140 см")
        else -> listOf("60 см","90 см","110 см","125 см","140 см")
    }

    @Composable
    fun largeTile(label:String,selected:Boolean,enabled:Boolean=true,onClick:()->Unit){
        val bg = if(selected) ProtoGold else ProtoPanel
        val borderColor = if(selected) ProtoGold else if(enabled) ProtoBorder else ProtoBorder.copy(alpha=.45f)
        Surface(
            color=if(enabled)bg else ProtoPanel.copy(alpha=.45f),
            border=BorderStroke(1.dp,borderColor),
            shape=RoundedCornerShape(16.dp),
            modifier=Modifier.fillMaxWidth().height(66.dp).clickable(enabled=enabled,onClick=onClick)
        ){
            Row(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(22.dp).border(1.dp,if(selected)Color.Black else if(enabled)ProtoGold else ProtoMuted,CircleShape).background(if(selected)Color.Black else Color.Transparent,CircleShape),contentAlignment=Alignment.Center){
                    if(selected) Icon(Icons.Outlined.Check,null,tint=ProtoGold,modifier=Modifier.size(15.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)){
                    Text(label,color=if(selected)Color.Black else if(enabled)ProtoText else ProtoMuted,fontSize=15.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                    if(!enabled) Text("В разработке",color=ProtoMuted,fontSize=10.sp)
                }
                if(enabled) Icon(Icons.Outlined.ChevronRight,null,tint=if(selected)Color.Black else ProtoGold)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(ProtoBg).padding(horizontal=18.dp)) {
        Spacer(Modifier.height(22.dp))
        ProtoHeader("Фильтр каталога","Шаг " + (step+1) + " из 4",onBack)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            repeat(4){i->Box(Modifier.weight(1f).height(5.dp).background(if(i<=step)ProtoGold else ProtoBorder,RoundedCornerShape(3.dp)))}
        }
        Spacer(Modifier.height(22.dp))
        Text(titles[step],color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold)
        Text(hints[step],color=ProtoMuted,fontSize=13.sp,modifier=Modifier.padding(top=5.dp,bottom=16.dp))

        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
            when(step){
                0 -> {
                    largeTile("Венки","Венки" in selectedTypes,true){onToggleType("Венки")}
                    listOf("Венки круглые","Корзины","Полянки","Флоретки","Ленты","Гробы","Кресты").forEach{label->
                        largeTile(label,false,false){}
                    }
                }
                1 -> listOf("Премиум","Стандарт","Эконом").forEach{label->
                    largeTile(label,label in selectedQualities,true){onToggleQuality(label)}
                }
                2 -> {
                    sizeOptions.chunked(2).forEach{row->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                            row.forEach{label->
                                val selected=label in selectedSizes
                                Surface(
                                    color=if(selected)ProtoGold else ProtoPanel,
                                    border=BorderStroke(1.dp,if(selected)ProtoGold else ProtoBorder),
                                    shape=RoundedCornerShape(16.dp),
                                    modifier=Modifier.weight(1f).height(62.dp).clickable{onToggleSize(label)}
                                ){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(label,color=if(selected)Color.Black else ProtoText,fontSize=16.sp,fontWeight=FontWeight.SemiBold)}}
                            }
                            if(row.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                else -> listOf("В наличии","Под заказ").forEach{label->
                    largeTile(label,label in selectedAvailability,true){onToggleAvailability(label)}
                }
            }
        }

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedButton(onClick={if(step>0)step-- else onBack()},modifier=Modifier.weight(.36f).height(56.dp),border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(14.dp)){Text("Назад",color=ProtoGold,fontWeight=FontWeight.SemiBold)}
            Button(onClick={if(step<3)step++ else onShow()},modifier=Modifier.weight(.64f).height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(14.dp)){
                Text(if(step<3)"Продолжить" else "Показать товары",color=Color.Black,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ProtoProductListScreen(products:List<ProtoCatalogProduct>,cart:SnapshotStateMap<String,Int>,stockOverrides:SnapshotStateMap<String,Int>,availableStock:(ProtoCatalogProduct)->Int,discount:Int,onBack:()->Unit,onOpenFilter:()->Unit,onOpenProduct:(ProtoCatalogProduct)->Unit,onCart:()->Unit,onHome:()->Unit,onCatalog:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Венки",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Найдено: "+products.size.toString(),color=ProtoMuted)};OutlinedButton(onClick=onOpenFilter,border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(22.dp)){Text("Изменить фильтр",color=ProtoGold,fontSize=11.sp)}};LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){if(products.isEmpty())item{ProtoSectionCard{Text("Ничего не найдено",color=ProtoMuted)}}else items(products.chunked(2)){row->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{p->Box(Modifier.weight(1f)){ProtoProductCard(p,availableStock(p),discount){onOpenProduct(p)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}};ProtoClientBottomBar(ProtoScreen.ProductList,cart.values.sum(),onHome,onCatalog,onCart,onOrders,onProfile)}}
}

@Composable
private fun ProtoProductCard(p:ProtoCatalogProduct,currentStock:Int,discount:Int,onOpen:()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(15.dp)){
        Box(Modifier.fillMaxWidth().height(118.dp).background(ProtoPanel2).clickable(onClick=onOpen)){ProtoProductImage(p,Modifier.fillMaxSize());Surface(Modifier.align(Alignment.BottomStart).padding(7.dp),color=if(currentStock>0)Color(0xDD123A27)else Color(0xDD4A2220),shape=RoundedCornerShape(18.dp)){Text(if(currentStock>0)"В наличии $currentStock" else "Под заказ · ${p.productionDays} дн.",color=if(currentStock>0)ProtoGreen else ProtoGoldSoft,fontSize=9.sp,modifier=Modifier.padding(horizontal=7.dp,vertical=3.dp))}}
        Column(Modifier.padding(9.dp)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=10.sp);Text(protoMoney(p.price*(100-discount)/100),color=ProtoGoldSoft,fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.padding(vertical=4.dp));Button(onClick=onOpen,modifier=Modifier.fillMaxWidth().height(38.dp),contentPadding=PaddingValues(horizontal=4.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(9.dp)){Icon(Icons.Outlined.AddShoppingCart,null,tint=Color.Black,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("В корзину",color=Color.Black,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)}}
    }
}

@Composable
private fun ProtoProductDetailScreen(product:ProtoCatalogProduct?,currentStock:Int,qty:Int,discount:Int,cartCount:Int,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAdd:()->Unit,onHome:()->Unit,onCatalog:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    val p=product?:return;var zoom by remember(p.sku){mutableStateOf(false)}
    ProtoBackground{
        Column(Modifier.fillMaxSize()){
            ProtoBrandHeader(onBack=onBack,showBell=true)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=5.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                item{Card(Modifier.fillMaxWidth().height(390.dp).clickable{zoom=true},shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder)){ProtoProductImage(p,Modifier.fillMaxSize(),ContentScale.Fit)}}
                item{Text(p.name,color=ProtoText,fontSize=27.sp,fontWeight=FontWeight.Bold);Text("Арт. "+p.sku,color=ProtoMuted);Text(if(currentStock>0)"В наличии "+currentStock.toString()+" шт." else "Под заказ · от "+p.productionDays.toString()+" дней",color=if(currentStock>0)ProtoGreen else ProtoGoldSoft)}
                item{Text(protoMoney(p.price*(100-discount)/100),color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold)}
                item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Surface(Modifier.width(148.dp).height(54.dp),color=ProtoPanel,border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(22.dp)){Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly){ProtoQtyButton(Icons.Outlined.Remove,onMinus);Text(qty.toString(),color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);ProtoQtyButton(Icons.Outlined.Add,onPlus)}};Button(onClick=onAdd,modifier=Modifier.weight(1f).height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(22.dp)){Text("В корзину",color=Color.Black,fontWeight=FontWeight.Bold)}}}
                item{ProtoSectionCard{Text("Характеристики",color=ProtoGoldSoft,fontWeight=FontWeight.Bold);ProtoInfoRow("Категория",p.type);ProtoInfoRow("Качество",p.quality);ProtoInfoRow("Размер",p.size)}}
            }
            ProtoClientBottomBar(ProtoScreen.ProductDetail,cartCount,onHome,onCatalog,onCart,onOrders,onProfile)
        }
    }
    if(zoom)ProtoProductImagePreview(p){zoom=false}
}

@Composable
private fun ProtoCartScreen(products:List<ProtoCatalogProduct>,cart:SnapshotStateMap<String,Int>,discount:Int,onBack:()->Unit,onPlus:(ProtoCatalogProduct)->Unit,onMinus:(ProtoCatalogProduct)->Unit,onDelete:(ProtoCatalogProduct)->Unit,onCheckout:()->Unit,onHome:()->Unit,onCatalog:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    val lines=cart.mapNotNull{entry->products.firstOrNull{it.sku==entry.key}?.let{Pair(it,entry.value)}};val base=lines.sumOf{it.first.price*it.second};val total=base*(100-discount)/100
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);Text("Корзина",color=ProtoText,fontSize=32.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp));LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){if(lines.isEmpty())item{ProtoSectionCard{Text("Корзина пуста",color=ProtoMuted)}}else items(lines,key={it.first.sku}){line->val p=line.first;val q=line.second;ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=2);Text(p.sku,color=ProtoMuted,fontSize=10.sp);Row(verticalAlignment=Alignment.CenterVertically){ProtoQtyButton(Icons.Outlined.Remove,{onMinus(p)});Text(q.toString(),color=ProtoText,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=8.dp));ProtoQtyButton(Icons.Outlined.Add,{onPlus(p)})}};IconButton(onClick={onDelete(p)}){Icon(Icons.Outlined.Delete,null,tint=ProtoGold)}}}};if(lines.isNotEmpty())item{ProtoSectionCard{ProtoInfoRow("Товары",protoMoney(base));ProtoInfoRow("Скидка клиента "+discount.toString()+"%","−"+protoMoney(base-total));HorizontalDivider(color=ProtoBorder,modifier=Modifier.padding(vertical=8.dp));ProtoInfoRow("Итого",protoMoney(total));Spacer(Modifier.height(10.dp));ProtoPrimaryButton("Оформить заказ",onCheckout)}}};ProtoClientBottomBar(ProtoScreen.Cart,cart.values.sum(),onHome,onCatalog,onCart={},onOrders,onProfile)}}
}

@Composable
private fun ProtoCheckoutScreen(cartCount:Int,total:Int,discount:Int,defaultAddress:String,onBack:()->Unit,onSubmit:(String,String,String)->Unit,onHome:()->Unit,onCatalog:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    var method by remember{mutableStateOf("Доставка")};var address by remember(defaultAddress){mutableStateOf(defaultAddress)};var comment by remember{mutableStateOf("")}
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);Text("Оформление заказа",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp));LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ProtoSectionCard{ProtoInfoRow("Получатель","ООО Ритуал-Сервис");ProtoInfoRow("Контакт","Игорь Петров")}};item{ProtoSectionCard{Text("Способ получения",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("Доставка","Самовывоз","ТК").forEach{m->val sel=method==m;Surface(Modifier.weight(1f).height(46.dp).clickable{method=m},color=if(sel)ProtoGold else ProtoPanel2,border=BorderStroke(1.dp,if(sel)ProtoGold else ProtoBorder),shape=RoundedCornerShape(22.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(m,color=if(sel)Color.Black else ProtoText,fontSize=11.sp)}}}}}};item{ProtoField(address,{address=it},"Адрес / точка получения")};item{ProtoField(comment,{if(it.length<=500)comment=it},"Комментарий к заказу")};item{ProtoSectionCard{ProtoInfoRow("Скидка",discount.toString()+"%");ProtoInfoRow("Всего",cartCount.toString()+" позиций");ProtoInfoRow("Итого к оплате",protoMoney(total))}};item{ProtoPrimaryButton("Отправить заказ"){onSubmit(method,address,comment)}}};ProtoClientBottomBar(ProtoScreen.Checkout,cartCount,onHome,onCatalog,onCart,onOrders,onProfile)}}
}

@Composable
private fun ProtoOrderSentScreen(order:ProtoOrder?,onView:()->Unit,onCatalog:()->Unit,onHome:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    val o=order?:return
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(showBell=true);LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp),horizontalAlignment=Alignment.CenterHorizontally){item{Box(Modifier.size(96.dp).border(2.dp,ProtoGold,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Check,null,tint=ProtoGold,modifier=Modifier.size(52.dp))}};item{Text("Заказ отправлен",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold)};item{ProtoSectionCard{Text("Заказ № "+o.id,color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold);Text("Статус: "+o.status,color=ProtoGoldSoft);ProtoInfoRow("Получатель",o.clientName);ProtoInfoRow("Сумма",protoMoney(o.total));ProtoInfoRow("Получение",o.deliveryMethod)}};item{ProtoPrimaryButton("Смотреть заказ",onView)};item{ProtoSecondaryButton("Вернуться в каталог",onCatalog)}};ProtoClientBottomBar(ProtoScreen.OrderSent,0,onHome,onCatalog,onCart,onOrders,onProfile)}}
}

@Composable
private fun ProtoOrderListScreen(orders:List<ProtoOrder>,onBack:()->Unit,onOpen:(ProtoOrder)->Unit,onHome:()->Unit,onCatalog:()->Unit,onCart:()->Unit,onProfile:()->Unit){
    var tab by remember{mutableStateOf("Текущие")};val visible=if(tab=="История")orders.filter{it.status=="Доставлен"}else orders.filter{it.status!="Доставлен"}
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);Text("Мои заказы",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp));Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Текущие","История").forEach{l->val sel=tab==l;Surface(Modifier.weight(1f).height(46.dp).clickable{tab=l},color=if(sel)ProtoGold else ProtoPanel,border=BorderStroke(1.dp,if(sel)ProtoGold else ProtoBorder),shape=RoundedCornerShape(22.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(l,color=if(sel)Color.Black else ProtoText,fontWeight=FontWeight.SemiBold)}}}};LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){if(visible.isEmpty())item{ProtoSectionCard{Text("Заказов нет",color=ProtoMuted)}}else items(visible,key={it.id}){o->ProtoOrderRow(o){onOpen(o)}}};ProtoClientBottomBar(ProtoScreen.OrderList,0,onHome,onCatalog,onCart,onOrders={},onProfile)}}
}

@Composable
private fun ProtoOrderDetailScreen(order:ProtoOrder?,isAdmin:Boolean,onBack:()->Unit,onStatus:(String)->Unit,onRepeat:(()->Unit)?=null,onEdit:(()->Unit)?=null){
    val o=order?:return
    val statuses=listOf("Получен","Подтверждён","Собирается","Доставляется","Доставлен")
    val currentIndex=statuses.indexOf(o.status).coerceAtLeast(0)
    ProtoScaffold("Заказ " + o.id,o.clientName + " · " + protoMoney(o.total),onBack){
        item{ProtoSectionCard{
            Text("Статус заказа",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            statuses.forEachIndexed{i,status->
                val reached=currentIndex>=i
                Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(12.dp).background(if(reached)ProtoGreen else ProtoBorder,CircleShape));Spacer(Modifier.width(9.dp))
                    Text(status,color=if(reached)ProtoText else ProtoMuted,modifier=Modifier.weight(1f))
                    if(isAdmin) RadioButton(selected=o.status==status,onClick={if(i>=currentIndex)onStatus(status)},enabled=i>=currentIndex,colors=RadioButtonDefaults.colors(selectedColor=ProtoGold))
                }
            }
        }}
        if(!isAdmin && (onRepeat!=null || onEdit!=null)){
            item{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    if(onEdit!=null) OutlinedButton(onClick=onEdit,modifier=Modifier.weight(1f).height(48.dp),border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(12.dp)){Icon(Icons.Outlined.Edit,null,tint=ProtoGold);Spacer(Modifier.width(6.dp));Text("Изменить",color=ProtoGold)}
                    if(onRepeat!=null) Button(onClick=onRepeat,modifier=Modifier.weight(1f).height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(12.dp)){Icon(Icons.Outlined.Replay,null,tint=Color.Black);Spacer(Modifier.width(6.dp));Text("Повторить",color=Color.Black,fontWeight=FontWeight.Bold)}
                }
            }
        }
        item{Text("История заказа",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)}
        items(o.history.asReversed()){event->ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.History,null,tint=ProtoGold);Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(event.status,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(event.dateTime,color=ProtoMuted,fontSize=11.sp)};Text(event.actor,color=ProtoGoldSoft,fontSize=11.sp)}}}
        item{ProtoSectionCard{ProtoInfoRow("Получение",o.deliveryMethod);if(o.deliveryAddress.isNotBlank())ProtoInfoRow("Адрес",o.deliveryAddress);if(o.comment.isNotBlank()){Text("Комментарий",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=6.dp));Text(o.comment,color=ProtoText)}}}
        item{Text("Состав заказа",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)}
        items(o.lines){line->ProtoSectionCard{Text(line.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Арт. " + line.sku + " · " + line.qty + " шт. · " + protoMoney(line.price*line.qty),color=ProtoMuted)}}
    }
}

@Composable
private fun ProtoProfileScreen(client:ProtoClient,onBack:()->Unit,onCall:()->Unit,onLogout:()->Unit,onHome:()->Unit,onCatalog:()->Unit,onCart:()->Unit,onOrders:()->Unit){
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);Text("Профиль",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp));LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ProtoSectionCard{Text(client.name,color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Статус: "+client.status,color=ProtoGoldSoft)}};item{ProtoSectionCard{ProtoInfoRow("Контакт",client.contact);ProtoInfoRow("Телефон",client.phone);ProtoInfoRow("E-mail",client.email.ifBlank{"—"});ProtoInfoRow("Скидка",client.discount.toString()+"%")}};item{ProtoPrimaryButton("Позвонить администратору",onCall)};item{ProtoSecondaryButton("Выйти",onLogout)}};ProtoClientBottomBar(ProtoScreen.Profile,0,onHome,onCatalog,onCart,onOrders,onProfile={})}}
}

@Composable
private fun ProtoSuspendedScreen(onCall:()->Unit,onMessage:()->Unit,onBack:()->Unit,onCatalog:()->Unit,onHome:()->Unit,onOrders:()->Unit){
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{ProtoSectionCard{Icon(Icons.Outlined.ErrorOutline,null,tint=ProtoRed,modifier=Modifier.size(52.dp));Text("Ваш статус временно приостановлен.",color=ProtoText,fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp));Text("Самостоятельное оформление заказов недоступно. Свяжитесь с администратором.",color=ProtoMuted,modifier=Modifier.padding(top=7.dp))}};item{ProtoSectionCard{Text("+7 926 304-60-19",color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold)}};item{ProtoPrimaryButton("Позвонить администратору",onCall)};item{ProtoSecondaryButton("Написать менеджеру",onMessage)}};ProtoClientBottomBar(ProtoScreen.Suspended,0,onHome,onCatalog,onCart={},onOrders,onProfile={})}}
}

@Composable
private fun ProtoAdminHomeScreen(registrationsTotal:Int,clients:List<ProtoClient>,orders:List<ProtoOrder>,productionOps:List<ProtoProductionOp>,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,lowStockThreshold:Int,reservedForSku:(String)->Int,onSearch:()->Unit,onRegistrations:()->Unit,onClients:()->Unit,onOrders:()->Unit,onProduction:()->Unit,onStock:()->Unit,onCatalog:()->Unit,onSettings:()->Unit,onAttention:()->Unit,onOnline:()->Unit,onLowStock:()->Unit){
    val today=currentDateShort();val ordersToday=orders.count{it.dateTime.startsWith(today)};val produced=productionOps.filter{it.date==today}.sumOf{it.qty};val active=clients.count{it.status!="Приостановлен"};val low=products.count{((stockOverrides[it.sku]?:it.stock)-reservedForSku(it.sku)).coerceAtLeast(0)<=lowStockThreshold}
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(showBell=true);LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Здравствуйте, Игорь",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold);Text("Администратор",color=ProtoGoldSoft)};item{Surface(Modifier.fillMaxWidth().height(56.dp).clickable(onClick=onSearch),color=ProtoPanel,border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(27.dp)){Row(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Search,null,tint=ProtoGold);Spacer(Modifier.width(8.dp));Text("Поиск по клиентам, заказам, товарам…",color=ProtoMuted)}}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Всего заявок",registrationsTotal.toString(),"текущий месяц",Modifier.weight(1f),onRegistrations);ProtoMetricCard("Активные клиенты",active.toString(),"клиентов",Modifier.weight(1f),onClients)}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Заказы сегодня",ordersToday.toString(),"заказов",Modifier.weight(1f),onOrders);ProtoMetricCard("Производство сегодня",produced.toString(),"шт.",Modifier.weight(1f),onProduction)}};item{ProtoMetricCard("Остаток на складе",products.sumOf{stockOverrides[it.sku]?:it.stock}.toString(),"низких остатков: "+low.toString(),Modifier.fillMaxWidth(),onStock)};item{Text("Быстрые действия",color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){ProtoQuickButton("Клиенты",Icons.Outlined.Groups,Modifier.weight(1f),onClients);ProtoQuickButton("Заказы",Icons.Outlined.ShoppingCart,Modifier.weight(1f),onOrders);ProtoQuickButton("Производство",Icons.Outlined.Factory,Modifier.weight(1f),onProduction);ProtoQuickButton("Каталог",Icons.Outlined.Inventory2,Modifier.weight(1f),onCatalog);ProtoQuickButton("Настройки",Icons.Outlined.Settings,Modifier.weight(1f),onSettings)}};item{Text("Требует внимания",color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold);ProtoAttentionLine(registrationsTotal.toString()+" регистраций","Требуют проверки",ProtoGold,onAttention);if(low>0)ProtoAttentionLine(low.toString()+" низких остатков","Ниже порога",ProtoOrange,onLowStock)};item{ProtoSectionCard(Modifier.clickable(onClick=onOnline)){Text("Онлайн-контроль",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);ProtoInfoRow("Клиенты онлайн",clients.count{it.online}.toString());ProtoInfoRow("Админка","Онлайн");ProtoInfoRow("Производство","Онлайн")}}};ProtoAdminBottomBar(ProtoScreen.AdminHome,onHome={},onClients,onOrders,onStock,onProfile=onSettings)}}
}

@Composable
private fun ProtoAdminSearchScreen(clients:List<ProtoClient>,orders:List<ProtoOrder>,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit,onClient:(ProtoClient)->Unit,onOrder:(ProtoOrder)->Unit,onProduct:(ProtoCatalogProduct)->Unit){
    var query by remember{mutableStateOf("")};val q=query.trim();val fc=if(q.isBlank())emptyList() else clients.filter{it.name.contains(q,true)||it.contact.contains(q,true)||it.id.contains(q,true)};val fo=if(q.isBlank())emptyList() else orders.filter{it.id.contains(q,true)||it.clientName.contains(q,true)};val fp=if(q.isBlank())emptyList() else products.filter{it.sku.contains(q,true)||it.name.contains(q,true)}.take(20)
    ProtoScaffold("Поиск","Клиенты · заказы · товары",onBack){item{ProtoField(query,{query=it},"Введите название, ID или артикул")};if(q.isBlank())item{Text("Начните ввод — поиск работает сразу по всей системе.",color=ProtoMuted)};if(fc.isNotEmpty())item{Text("Клиенты",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fc,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onClient(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.id} · ${c.status}",color=ProtoMuted,fontSize=11.sp)}};if(fo.isNotEmpty())item{Text("Заказы",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fo,key={it.id}){o->ProtoOrderRow(o){onOrder(o)}};if(fp.isNotEmpty())item{Text("Товары",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(fp,key={it.sku}){p->ProtoSectionCard(Modifier.clickable{onProduct(p)}){Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(p.sku,color=ProtoMuted,fontSize=11.sp)};Text("${stockOverrides[p.sku]?:p.stock} шт.",color=ProtoGoldSoft)}}}}
}

@Composable
private fun ProtoAdminClientsScreen(clients:List<ProtoClient>,onBack:()->Unit,onOpen:(ProtoClient)->Unit){var q by remember{mutableStateOf("")};val filtered=clients.filter{q.isBlank()||it.name.contains(q,true)||it.contact.contains(q,true)};ProtoScaffold("Клиенты","Оборот за ${currentMonthLabel().lowercase(ruLocale)}",onBack){item{ProtoField(q,{q=it},"Поиск клиента")};items(filtered,key={it.id}){c->Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.fillMaxWidth().clickable{onOpen(c)}){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.status} · ${c.orderCount} заказов",color=ProtoMuted,fontSize=12.sp)};Column(horizontalAlignment=Alignment.End){Text(protoMoney(c.monthTurnover),color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}}

@Composable
private fun ProtoAdminClientScreen(client:ProtoClient?,onBack:()->Unit,onStatus:(String)->Unit,onToggleBlock:()->Unit,onToggleOrdering:()->Unit,onDiscount:(Int)->Unit,onSave:()->Unit,onCall:()->Unit){
    val c=client?:return;var code by remember{mutableStateOf(false)}
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Карточка клиента",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold)};item{ProtoSectionCard{Text(c.name,color=ProtoText,fontSize=22.sp,fontWeight=FontWeight.Bold);Text(c.clientType,color=ProtoMuted);ProtoInfoRow("ID",c.id);ProtoInfoRow("Телефон",c.phone);ProtoInfoRow("E-mail",c.email.ifBlank{"—"});TextButton(onClick={code=true}){Text("Показать код доступа",color=ProtoGold)}}};item{ProtoSectionCard{Text("Статус клиента",color=ProtoText,fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Активный","Оптовик","VIP","Приостановлен").forEach{s->FilterChip(selected=c.status==s,onClick={onStatus(s)},label={Text(s,fontSize=9.sp)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))}};Row(verticalAlignment=Alignment.CenterVertically){Text("Скидка",color=ProtoMuted);Spacer(Modifier.weight(1f));ProtoQtyButton(Icons.Outlined.Remove,{onDiscount(-1)});Text(c.discount.toString()+"%",color=ProtoText,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=10.dp));ProtoQtyButton(Icons.Outlined.Add,{onDiscount(1)})};ProtoSwitchRow("Доступ к заказам",c.orderingEnabled,{onToggleOrdering()})}};item{ProtoPrimaryButton("Сохранить",onSave)};item{ProtoSecondaryButton(if(c.status=="Приостановлен")"Разблокировать" else "Приостановить",onToggleBlock)};item{ProtoSecondaryButton("Позвонить",onCall)}}}}
    if(code)AlertDialog(onDismissRequest={code=false},containerColor=ProtoPanel,title={Text("Код доступа клиента",color=ProtoText)},text={Text(c.accessCode,color=ProtoGoldSoft,fontSize=32.sp,fontWeight=FontWeight.Bold)},confirmButton={TextButton(onClick={code=false}){Text("Закрыть",color=ProtoGold)}})
}

@Composable
private fun ProtoAdminOrdersScreen(orders:List<ProtoOrder>,onBack:()->Unit,onOpen:(ProtoOrder)->Unit){ProtoScaffold("Заказы",currentMonthLabel(),onBack){items(orders,key={it.id}){o->ProtoOrderRow(o){onOpen(o)}}}}

@Composable
private fun ProtoAdminCatalogScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit){ProtoScaffold("Каталог","Все тестовые позиции",onBack){items(products,key={it.sku}){p->ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${p.sku} · ${p.type} · ${p.size}",color=ProtoMuted,fontSize=11.sp)};Text("${stockOverrides[p.sku]?:p.stock} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoAdminSettingsMenuScreen(syncStatus:String,lastSync:String,lowStockThreshold:Int,onBack:()->Unit,onOpen:(String)->Unit,onCall:()->Unit){
    val rows=listOf(
        Triple("Профиль компании",Icons.Outlined.Business,"Данные SANSARA и контакты"),
        Triple("Пользователи и роли",Icons.Outlined.Groups,"Администраторы и производство"),
        Triple("Клиенты",Icons.Outlined.PersonSearch,"Доступ, статусы и скидки"),
        Triple("Каталог и синхронизация",Icons.Outlined.Sync,"$syncStatus · $lastSync"),
        Triple("Порог низких остатков",Icons.Outlined.Warning,"Сейчас: $lowStockThreshold шт."),
        Triple("Уведомления",Icons.Outlined.Notifications,"Регистрации, заказы, производство"),
        Triple("Резервное копирование",Icons.Outlined.Backup,"Локальная тестовая копия"),
        Triple("Экспорт данных",Icons.Outlined.FileDownload,"CSV сейчас · XLSX/PDF далее"),
        Triple("О приложении",Icons.Outlined.Info,"SANSARA · версия ${BuildConfig.VERSION_NAME}")
    )
    ProtoScaffold("Настройки","Управление системой SANSARA",onBack){
        items(rows){(title,icon,subtitle)->Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{onOpen(title)}){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).background(ProtoPanel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(icon,null,tint=ProtoGold)};Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(title,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(subtitle,color=ProtoMuted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}
        item{Spacer(Modifier.height(8.dp));Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().clickable{onCall()}){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).background(ProtoGreen.copy(alpha=.16f),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Phone,null,tint=ProtoGreen)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("Поддержка",color=ProtoMuted,fontSize=11.sp);Text("+7 926 304-60-19",color=ProtoGoldSoft,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Ежедневно 09:00–20:00",color=ProtoMuted,fontSize=10.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}
    }
}

@Composable
private fun ProtoAdminSettingsDetailScreen(section:String,threshold:Int,reg:Boolean,orders:Boolean,prod:Boolean,low:Boolean,tildaUrl:String,syncStatus:String,lastSync:String,syncing:Boolean,backupStatus:String,backendStatus:String,onBack:()->Unit,onThreshold:(Int)->Unit,onReg:(Boolean)->Unit,onOrders:(Boolean)->Unit,onProd:(Boolean)->Unit,onLow:(Boolean)->Unit,onTildaUrl:(String)->Unit,onSync:()->Unit,onBackup:()->Unit,onClients:()->Unit){
    ProtoScaffold(section,null,onBack){
        when(section){
            "Профиль компании"->item{ProtoSectionCard{ProtoInfoRow("Компания","SANSARA");ProtoInfoRow("Телефон","+7 926 304-60-19");ProtoInfoRow("Режим поддержки","09:00–20:00");ProtoInfoRow("Каталог","sansararitual.ru")}}
            "Пользователи и роли"->item{ProtoSectionCard{listOf("Администратор — полный доступ","Производство — выпуск / приход / история","Клиент — каталог / корзина / заказы").forEach{Text(it,color=ProtoText,modifier=Modifier.padding(vertical=5.dp))};Text("Роли фиксированы. Пользователь не выбирает роль самостоятельно.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))}}
            "Каталог и синхронизация"->item{ProtoSectionCard{Text("Каталог Tilda",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Синхронизация обновляет карточки, цены, категории и фотографии. Склад, резерв, заказы и производство не перезаписываются.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(vertical=6.dp));ProtoField(tildaUrl,onTildaUrl,"YML-ссылка каталога Tilda");Button(onClick=onSync,enabled=!syncing&&tildaUrl.isNotBlank(),modifier=Modifier.fillMaxWidth().height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){if(syncing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Color.Black)else Icon(Icons.Outlined.Sync,null,tint=Color.Black);Spacer(Modifier.width(7.dp));Text(if(syncing)"Синхронизация…" else "Синхронизировать каталог",color=Color.Black,fontWeight=FontWeight.Bold)};Text(syncStatus,color=if(syncStatus.startsWith("Ошибка"))ProtoRed else ProtoGreen,fontSize=11.sp,modifier=Modifier.padding(top=7.dp));Text("Последнее обновление: $lastSync",color=ProtoMuted,fontSize=10.sp);Text("Сервер событий: $backendStatus",color=ProtoMuted,fontSize=10.sp,modifier=Modifier.padding(top=4.dp))}}
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
    onStock:()->Unit,
    onHome:()->Unit,
    onProfile:()->Unit
){
    val produced = opsForDay.sumOf { it.qty }
    val draftTotal = drafts.sumOf { it.qty }
    val physical = products.sumOf { stockOverrides[it.sku] ?: it.stock }
    val selectedLabel = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale))
    var showCalendar by remember { mutableStateOf(false) }

    ProtoBackground {
        Column(Modifier.fillMaxSize()){
            ProtoBrandHeader(showBell=true)
            BoxWithConstraints(Modifier.weight(1f)){
            // Clean the whole old production body: old search, buttons, images and "Последние операции" disappear.
            Box(Modifier.offset(maxWidth*.025f,maxHeight*.125f).size(maxWidth*.95f,maxHeight*.775f).background(ProtoBg.copy(alpha=.995f),RoundedCornerShape(16.dp)))

            Column(
                Modifier.offset(maxWidth*.04f,maxHeight*.132f).width(maxWidth*.92f).height(maxHeight*.755f)
            ){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text("Производство",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    Surface(
                        color=ProtoPanel,
                        border=BorderStroke(1.dp,ProtoGold),
                        shape=RoundedCornerShape(14.dp),
                        modifier=Modifier.clickable{showCalendar=true}
                    ){
                        Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
                            Icon(Icons.Outlined.CalendarMonth,null,tint=ProtoGold,modifier=Modifier.size(20.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(selectedLabel,color=ProtoGoldSoft,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.Outlined.ExpandMore,null,tint=ProtoGold,modifier=Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding=PaddingValues(bottom=18.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    item{
                        ProtoSectionCard{
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Icon(Icons.Outlined.Info,null,tint=ProtoGold,modifier=Modifier.size(22.dp))
                                Spacer(Modifier.width(9.dp))
                                Text("После проведения данные сразу видят клиент и администратор.",color=ProtoMuted,fontSize=12.sp)
                            }
                        }
                    }

                    item{
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("Выпуск продукции",color=ProtoText,fontSize=24.sp,fontWeight=FontWeight.Bold)
                                Text(selectedLabel,color=ProtoMuted,fontSize=12.sp)
                            }
                            OutlinedButton(onClick=onAdd,border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(12.dp)){
                                Icon(Icons.Outlined.Add,null,tint=ProtoGold)
                                Spacer(Modifier.width(5.dp))
                                Text("Добавить",color=ProtoGold,fontWeight=FontWeight.SemiBold)
                            }
                        }
                    }

                    item{
                        Card(
                            colors=CardDefaults.cardColors(containerColor=ProtoPanel),
                            border=BorderStroke(1.dp,ProtoBorder),
                            shape=RoundedCornerShape(16.dp),
                            modifier=Modifier.fillMaxWidth()
                        ){
                            Column(Modifier.padding(12.dp)){
                                if(opsForDay.isEmpty() && drafts.isEmpty()){
                                    Column(
                                        Modifier.fillMaxWidth().height(180.dp).clickable{onAdd()},
                                        horizontalAlignment=Alignment.CenterHorizontally,
                                        verticalArrangement=Arrangement.Center
                                    ){
                                        Icon(Icons.Outlined.AddCircleOutline,null,tint=ProtoGold,modifier=Modifier.size(44.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Добавить позицию выпуска",color=ProtoGoldSoft,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
                                        Text("Выберите товар из каталога производства",color=ProtoMuted,fontSize=11.sp)
                                    }
                                } else {
                                    opsForDay.forEach{op->
                                        val p=products.firstOrNull{it.sku==op.sku}
                                        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                                            if(p!=null) ProtoProductImage(p,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))
                                            else Image(painterResource(R.drawable.mock_wreath),null,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)){
                                                Text(op.name,color=ProtoText,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                                Text(op.sku + " · " + op.assembler,color=ProtoMuted,fontSize=11.sp)
                                            }
                                            Column(horizontalAlignment=Alignment.End){
                                                Text(op.qty.toString() + " шт.",color=ProtoGoldSoft,fontSize=15.sp,fontWeight=FontWeight.Bold)
                                                Text("Проведено",color=ProtoGreen,fontSize=10.sp)
                                            }
                                        }
                                        HorizontalDivider(color=ProtoBorder.copy(alpha=.55f))
                                    }
                                    drafts.forEach{d->
                                        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                                            ProtoProductImage(d.product,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)){
                                                Text(d.product.name,color=ProtoText,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                                Text(d.product.sku + " · " + d.assembler,color=ProtoMuted,fontSize=11.sp)
                                            }
                                            Column(horizontalAlignment=Alignment.End){
                                                Text(d.qty.toString() + " шт.",color=ProtoGoldSoft,fontSize=15.sp,fontWeight=FontWeight.Bold)
                                                Text("На приход",color=ProtoOrange,fontSize=10.sp)
                                            }
                                        }
                                        HorizontalDivider(color=ProtoBorder.copy(alpha=.55f))
                                    }
                                }
                            }
                        }
                    }

                    item{
                        Card(
                            colors=CardDefaults.cardColors(containerColor=ProtoPanel),
                            border=BorderStroke(1.dp,ProtoGold.copy(alpha=.55f)),
                            shape=RoundedCornerShape(16.dp),
                            modifier=Modifier.fillMaxWidth()
                        ){
                            Column(Modifier.padding(16.dp)){
                                Text("Итого",color=ProtoGoldSoft,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text((produced+draftTotal).toString() + " шт.",color=ProtoText,fontSize=32.sp,fontWeight=FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text("Проведено: " + produced + " · на приход: " + draftTotal + " · физический склад: " + physical,color=ProtoMuted,fontSize=11.sp)
                            }
                        }
                    }

                    item{
                        Button(
                            onClick=onPostAll,
                            modifier=Modifier.fillMaxWidth().height(58.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),
                            shape=RoundedCornerShape(14.dp)
                        ){
                            Icon(Icons.Outlined.Inventory2,null,tint=Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Оприходовать выпуск",color=Color.Black,fontSize=16.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                    item{
                        OutlinedButton(
                            onClick=onHistory,
                            modifier=Modifier.fillMaxWidth().height(56.dp),
                            border=BorderStroke(1.dp,ProtoGold),
                            shape=RoundedCornerShape(14.dp)
                        ){
                            Icon(Icons.Outlined.History,null,tint=ProtoGold)
                            Spacer(Modifier.width(8.dp))
                            Text("История приходов",color=ProtoGold,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                        }
                    }
                    item{
                        OutlinedButton(
                            onClick=onReport,
                            modifier=Modifier.fillMaxWidth().height(56.dp),
                            border=BorderStroke(1.dp,ProtoGold),
                            shape=RoundedCornerShape(14.dp)
                        ){
                            Icon(Icons.Outlined.Assessment,null,tint=ProtoGold)
                            Spacer(Modifier.width(8.dp))
                            Text("Отчёт",color=ProtoGold,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
            }

            }
            ProtoProductionBottomBar(onHome,onProduction={},onHistory,onStock,onProfile)
        }
    }

    if(showCalendar){
        ProtoProductionCalendarDialog(
            selectedDate=selectedDate,
            onDismiss={showCalendar=false},
            onSelect={date->onDateChange(date);showCalendar=false}
        )
    }
}

@Composable
private fun ProtoProductionCategoryScreen(onBack:()->Unit,onCategory:(String)->Unit){ProtoScaffold("Выпуск продукции","Что произведено в цеху?",onBack){item{listOf("Венки","Венки круглые","Корзины","Флоретки","Полянки").forEach{cat->ProtoSectionCard(Modifier.padding(vertical=4.dp).clickable{onCategory(cat)}){Row(verticalAlignment=Alignment.CenterVertically){Image(painterResource(protoPlaceholderForType(cat)),null,Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop);Spacer(Modifier.width(12.dp));Text(cat,color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}}

@Composable
private fun ProtoProductionCatalogScreen(products:List<ProtoCatalogProduct>,onBack:()->Unit,onSelect:(ProtoCatalogProduct)->Unit){var query by remember{mutableStateOf("")};val filtered=products.filter{query.isBlank()||it.sku.contains(query,true)||it.name.contains(query,true)};ProtoScaffold("Каталог производства","Выберите модель — артикул подставится автоматически",onBack){item{ProtoField(query,{query=it},"Поиск по артикулу или названию")};items(filtered,key={it.sku}){p->ProtoSectionCard(Modifier.clickable{onSelect(p)}){Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Арт. ${p.sku} · ${p.quality} · ${p.size}",color=ProtoMuted,fontSize=11.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}

@Composable
private fun ProtoProductionEntryScreen(product:ProtoCatalogProduct?,qty:Int,assembler:String,selectedDate:LocalDate,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAssembler:(String)->Unit,onAddDraft:()->Unit,onPostNow:()->Unit){
    val p=product?:return
    val dateLabel=selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy",ruLocale))
    ProtoScaffold("Приход продукции",dateLabel,onBack){
        item{ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.width(12.dp));Column{Text(p.name,color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Арт. " + p.sku,color=ProtoGoldSoft);Text(p.quality + " · " + p.size,color=ProtoMuted)}}}}
        item{Text("Сборщица",color=ProtoGoldSoft,fontSize=16.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(6.dp));Column(verticalArrangement=Arrangement.spacedBy(6.dp)){listOf("Анна К.","Мария С.","Елена П.").forEach{name->FilterChip(selected=assembler==name,onClick={onAssembler(name)},label={Text(name,fontSize=14.sp)},modifier=Modifier.fillMaxWidth().height(48.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))}}}
        item{Text("Количество",color=ProtoGoldSoft,fontSize=16.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){ProtoQtyButton(Icons.Outlined.Remove,onMinus,56.dp);Text(qty.toString(),color=ProtoText,fontSize=38.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=32.dp));ProtoQtyButton(Icons.Outlined.Add,onPlus,56.dp)}}
        item{Button(onClick=onAddDraft,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(14.dp)){Text("Добавить в выпуск дня",color=Color.Black,fontSize=15.sp,fontWeight=FontWeight.Bold)};OutlinedButton(onClick=onPostNow,modifier=Modifier.fillMaxWidth().height(54.dp).padding(top=8.dp),border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(14.dp)){Text("Оприходовать на склад сразу",color=ProtoGold,fontWeight=FontWeight.Bold)}}
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
private fun ProtoProductionReportScreen(selectedDate:LocalDate,ops:List<ProtoProductionOp>,products:List<ProtoCatalogProduct>,onBack:()->Unit){
    val totalQty=ops.sumOf{it.qty}
    val wreathQty=ops.filter{op->products.firstOrNull{it.sku==op.sku}?.type?.contains("Венки",true)==true}.sumOf{it.qty}
    val totalValue=ops.sumOf{op->(products.firstOrNull{it.sku==op.sku}?.price?:0)*op.qty}
    val byAssembler=ops.groupBy{it.assembler}.mapValues{(_,items)->items.sumOf{it.qty}}.toList().sortedByDescending{it.second}
    val bySku=ops.groupBy{it.sku}.map{(sku,items)->Triple(sku,items.firstOrNull()?.name.orEmpty(),items.sumOf{it.qty})}.sortedByDescending{it.third}
    val byQuality=ops.groupBy{op->products.firstOrNull{it.sku==op.sku}?.quality?.ifBlank{"Без категории"}?:"Без категории"}.mapValues{(_,items)->items.sumOf{it.qty}}.toList().sortedByDescending{it.second}
    val dateLabel=selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy",ruLocale))

    ProtoScaffold("Отчёт производства",dateLabel,onBack){
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                ProtoMetricCard("Всего",totalQty.toString(),"изделий",Modifier.weight(1f),{})
                ProtoMetricCard("Венки",wreathQty.toString(),"шт.",Modifier.weight(1f),{})
            }
        }
        item{ProtoMetricCard("Сумма выпуска",protoMoney(totalValue),"по текущим ценам каталога",Modifier.fillMaxWidth(),{})}

        item{Text("По сборщицам",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
        if(byAssembler.isEmpty()) item{ProtoSectionCard{Text("За выбранный день операций нет",color=ProtoMuted)}}
        else items(byAssembler){(name,qty)->ProtoSectionCard{Row(Modifier.fillMaxWidth()){Text(name,color=ProtoText,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(qty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}

        item{Text("По артикулам",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
        items(bySku){(sku,name,qty)->ProtoSectionCard{Text(name,color=ProtoText,fontWeight=FontWeight.SemiBold);Row(Modifier.fillMaxWidth()){Text(sku,color=ProtoMuted,fontSize=11.sp);Spacer(Modifier.weight(1f));Text(qty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}

        item{Text("По качеству",color=ProtoText,fontSize=21.sp,fontWeight=FontWeight.Bold)}
        items(byQuality){(quality,qty)->ProtoSectionCard{Row(Modifier.fillMaxWidth()){Text(quality,color=ProtoText,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(qty.toString()+" шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}
    }
}

@Composable
private fun ProtoProductionHistoryScreen(ops:List<ProtoProductionOp>,products:List<ProtoCatalogProduct>,onBack:()->Unit){
    val grouped=ops.groupBy{it.date}
    ProtoScaffold("История приходов","По датам, артикулам и сборщицам",onBack){grouped.forEach{(date,dayOps)->item(key="h$date"){Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){Text(date,color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("${dayOps.sumOf{it.qty}} шт.",color=ProtoMuted)}};items(dayOps){op->val product=products.firstOrNull{it.sku==op.sku};ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){if(product!=null)ProtoProductImage(product,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))else Image(painterResource(R.drawable.mock_wreath),null,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(op.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${op.sku} · ${op.time}",color=ProtoMuted,fontSize=11.sp);Text("Сборщица: ${op.assembler} · ${op.postedBy}",color=ProtoMuted,fontSize=11.sp)};Column(horizontalAlignment=Alignment.End){Text("${op.qty} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Text(op.status,color=ProtoGreen,fontSize=9.sp)}}}}}
    }
}

@Composable
private fun ProtoStaffProfileScreen(role:String,onBack:()->Unit,onCall:()->Unit,onLogout:()->Unit){
    ProtoScaffold("Профиль",role,onBack){
        item{ProtoSectionCard{ProtoInfoRow("Роль",role);ProtoInfoRow("Дата",currentDateLong());ProtoInfoRow("Версия",BuildConfig.VERSION_NAME);ProtoInfoRow("Поддержка","+7 926 304-60-19");Spacer(Modifier.height(8.dp));OutlinedButton(onClick=onCall,modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Phone,null,tint=ProtoGold);Spacer(Modifier.width(7.dp));Text("Позвонить в поддержку",color=ProtoGold)};TextButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Выйти",color=ProtoMuted)}}}
    }
}

@Composable
private fun ProtoServerScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,productionOps:List<ProtoProductionOp>,orders:List<ProtoOrder>,clients:List<ProtoClient>,reservedForSku:(String)->Int,onBack:()->Unit,onProduced:()->Unit,onStock:()->Unit,onReserve:()->Unit,onNewClients:()->Unit,onOnline:()->Unit,onExport:()->Unit){
    val reserve=products.sumOf{reservedForSku(it.sku)};val available=products.sumOf{p->((stockOverrides[p.sku]?:p.stock)-reservedForSku(p.sku)).coerceAtLeast(0)};val produced=productionOps.sumOf{it.qty}
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack,showBell=true);LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Сервер данных",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold);Text("Учёт по месяцам · "+currentMonthLabel(),color=ProtoMuted)};item{Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){ProtoMetricCard("Выпуск",produced.toString(),"шт.",Modifier.weight(1f),onProduced);ProtoMetricCard("Резерв",reserve.toString(),"шт.",Modifier.weight(1f),onReserve);ProtoMetricCard("Доступно",available.toString(),"шт.",Modifier.weight(1f),onStock)}};item{ProtoSectionCard(Modifier.clickable(onClick=onOnline)){Text("Синхронизация",color=ProtoText,fontWeight=FontWeight.Bold);ProtoInfoRow("Клиенты","Онлайн");ProtoInfoRow("Администратор","Онлайн");ProtoInfoRow("Производство","Онлайн")}};items(products.take(10)){p->val ph=stockOverrides[p.sku]?:p.stock;val rs=reservedForSku(p.sku);ProtoSectionCard{Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(p.sku,color=ProtoMuted,fontSize=10.sp);ProtoInfoRow("Остаток",ph.toString());ProtoInfoRow("Резерв",rs.toString());ProtoInfoRow("Доступно",(ph-rs).coerceAtLeast(0).toString())}};item{ProtoSecondaryButton("Экспорт",onExport)}}}}
}

@Composable
private fun ProtoStockListScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,reservedForSku:(String)->Int,onBack:()->Unit){ProtoScaffold("Остатки на складе","Физический остаток − резерв = доступно",onBack){items(products,key={it.sku}){p->val physical=stockOverrides[p.sku]?:p.stock;val reserved=reservedForSku(p.sku);val available=(physical-reserved).coerceAtLeast(0);ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(p.sku,color=ProtoMuted,fontSize=11.sp);Text("Факт: $physical · Резерв: $reserved",color=ProtoMuted,fontSize=10.sp)};Column(horizontalAlignment=Alignment.End){Text("$available шт.",color=if(available>0)ProtoGreen else ProtoRed,fontWeight=FontWeight.Bold);Text("доступно",color=ProtoMuted,fontSize=9.sp)}}}}}}

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
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader();LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp),horizontalAlignment=Alignment.CenterHorizontally){item{Box(Modifier.size(96.dp).border(2.dp,ProtoGold,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Check,null,tint=ProtoGold,modifier=Modifier.size(52.dp))}};item{Text("Заявка отправлена",color=ProtoText,fontSize=31.sp,fontWeight=FontWeight.Bold)};if(reg!=null)item{ProtoSectionCard{ProtoInfoRow("Тип клиента",reg.type);ProtoInfoRow("Организация",reg.organization);ProtoInfoRow("Телефон",reg.phone1);ProtoInfoRow("E-mail",reg.email)}};item{ProtoPrimaryButton("Понятно",onBack)}}}}
}

@Composable
private fun ProtoSimpleMessageScreen(title:String,text:String,onBack:()->Unit){
    ProtoBackground{Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){ProtoSectionCard{Icon(Icons.Outlined.CheckCircle,null,tint=ProtoGreen,modifier=Modifier.size(54.dp).align(Alignment.CenterHorizontally));Text(title,color=ProtoText,fontSize=27.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=12.dp));Text(text,color=ProtoMuted,modifier=Modifier.padding(vertical=14.dp));ProtoPrimaryButton("Готово",onBack)}}}
}

@Composable
private fun ProtoBackground(content:@Composable BoxScope.()->Unit){
    Box(Modifier.fillMaxSize().background(ProtoBg)){
        Image(painterResource(R.drawable.sansara_leaves),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop,alpha=.05f)
        content()
    }
}

@Composable
private fun ProtoBrandHeader(onBack:(()->Unit)?=null,showBell:Boolean=false,onLogoClick:(()->Unit)?=null){
    Row(Modifier.fillMaxWidth().height(94.dp).padding(horizontal=16.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(46.dp),contentAlignment=Alignment.CenterStart){if(onBack!=null)ProtoCircleBack(onBack)}
        Column(Modifier.weight(1f).clickable(enabled=onLogoClick!=null){onLogoClick?.invoke()},horizontalAlignment=Alignment.CenterHorizontally){
            Text("SANSARA",color=ProtoGoldSoft,fontSize=27.sp,fontWeight=FontWeight.Medium,letterSpacing=5.sp,maxLines=1)
            Text("Оптовая платформа ритуальных товаров",color=ProtoMuted,fontSize=10.sp,maxLines=1)
        }
        Box(Modifier.width(46.dp),contentAlignment=Alignment.CenterEnd){
            if(showBell)BadgedBox(badge={Badge(containerColor=ProtoGold)}){Icon(Icons.Outlined.Notifications,null,tint=ProtoGold,modifier=Modifier.size(28.dp))}
        }
    }
}

@Composable
private fun ProtoPrimaryButton(text:String,onClick:()->Unit){
    Button(onClick=onClick,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(24.dp)){
        Text(text,color=Color.Black,fontWeight=FontWeight.Bold,fontSize=15.sp)
    }
}

@Composable
private fun ProtoSecondaryButton(text:String,onClick:()->Unit){
    OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth().height(56.dp),border=BorderStroke(1.dp,ProtoGold),shape=RoundedCornerShape(24.dp)){
        Text(text,color=ProtoText,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
    }
}

@Composable
private fun ProtoAvailabilityChips(selected:String,onSelect:(String)->Unit){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        listOf("Все","В наличии","Под заказ").forEach{label->
            val active=selected==label
            Surface(Modifier.weight(1f).height(48.dp).clickable{onSelect(label)},color=if(active)ProtoGold else ProtoPanel,border=BorderStroke(1.dp,if(active)ProtoGold else ProtoBorder),shape=RoundedCornerShape(24.dp)){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Text(label,color=if(active)Color.Black else ProtoText,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProtoScaffold(title:String,subtitle:String?=null,onBack:(()->Unit)?=null,content:LazyListScope.()->Unit){
    ProtoBackground{Column(Modifier.fillMaxSize()){ProtoBrandHeader(onBack=onBack);Column(Modifier.padding(horizontal=16.dp,vertical=3.dp)){Text(title,color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold);if(subtitle!=null)Text(subtitle,color=ProtoMuted,fontSize=12.sp)};LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}
}

@Composable
private fun ProtoHeader(title:String,subtitle:String,onBack:()->Unit){Column{ProtoBrandHeader(onBack=onBack);Text(title,color=ProtoText,fontSize=29.sp,fontWeight=FontWeight.Bold);Text(subtitle,color=ProtoMuted,fontSize=12.sp)}}

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
                    Text("Арт. " + product.sku,color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(bottom=8.dp))
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
private fun ProtoField(value:String,onChange:(String)->Unit,label:String,keyboardType:KeyboardType=KeyboardType.Text){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=3.dp),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=keyboardType),shape=RoundedCornerShape(20.dp),colors=protoFieldColors())}

@Composable
private fun protoFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=ProtoGold,unfocusedBorderColor=ProtoBorder,focusedTextColor=ProtoText,unfocusedTextColor=ProtoText,focusedLabelColor=ProtoGold,unfocusedLabelColor=ProtoMuted,focusedLeadingIconColor=ProtoGold,unfocusedLeadingIconColor=ProtoMuted,disabledBorderColor=ProtoBorder,disabledTextColor=ProtoMuted)

@Composable
private fun ProtoCompactGrid(title:String,options:List<String>,selected:Set<String>,toggle:(String)->Unit,disabled:Set<String> = emptySet()){Text(title,color=ProtoGoldSoft,fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=7.dp,bottom=3.dp));options.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{o->val off=o in disabled;FilterChip(selected=o in selected,onClick={if(!off)toggle(o)},enabled=!off,label={Text(if(off)"$o · в разработке" else o,fontSize=if(off)9.sp else 11.sp,maxLines=2)},modifier=Modifier.weight(1f).height(40.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText,disabledLabelColor=ProtoMuted))};if(row.size==1)Spacer(Modifier.weight(1f))}}
}

@Composable
private fun ProtoCategoryButton(label:String,disabled:Boolean,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,enabled=true,modifier=modifier.height(46.dp),border=BorderStroke(1.dp,if(disabled)ProtoBorder else ProtoGold),contentPadding=PaddingValues(horizontal=5.dp)){Text(if(disabled)"$label · скоро" else label,color=if(disabled)ProtoMuted else ProtoGold,fontSize=11.sp,maxLines=1)}}

@Composable
private fun ProtoPopularRow(p:ProtoCatalogProduct,stock:Int,onOpen:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=10.sp);Text(if(stock>0)"В наличии $stock шт." else "Под заказ · от ${p.productionDays} дней",color=if(stock>0)ProtoGreen else ProtoGoldSoft,fontSize=10.sp);Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)};Button(onClick=onOpen,colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.width(92.dp)){Text("В корзину",color=Color.Black,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)}}}}

@Composable
private fun ProtoCartRow(p:ProtoCatalogProduct,qty:Int,onMinus:()->Unit,onPlus:()->Unit,onDelete:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(64.dp).clip(RoundedCornerShape(9.dp)));Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(p.sku,color=ProtoMuted,fontSize=10.sp);Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){ProtoQtyButton(Icons.Outlined.Remove,onMinus);Text(qty.toString(),color=ProtoText,fontWeight=FontWeight.Bold);ProtoQtyButton(Icons.Outlined.Add,onPlus)}};Column(horizontalAlignment=Alignment.End){Text(protoMoney(p.price*qty),color=ProtoGoldSoft,fontWeight=FontWeight.Bold);IconButton(onClick=onDelete){Icon(Icons.Outlined.Delete,null,tint=ProtoMuted)}}}}}

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
private fun ProtoInfoRow(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(label,color=ProtoMuted);Spacer(Modifier.weight(1f));Text(value,color=ProtoText,fontWeight=FontWeight.Medium)}}

@Composable
private fun ProtoPill(text:String,color:Color){Surface(color=color.copy(alpha=.16f),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,color.copy(alpha=.55f))){Text(text,color=color,fontSize=10.sp,modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp))}}

@Composable
private fun ProtoActionChip(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,modifier=modifier.height(44.dp),border=BorderStroke(1.dp,ProtoBorder)){Icon(icon,null,tint=ProtoGold,modifier=Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(text,color=ProtoText,fontSize=11.sp)}}

@Composable
private fun ProtoClientBottomBar(current:ProtoScreen,cartCount:Int,onHome:()->Unit,onCatalog:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit){
    val h=current==ProtoScreen.Home;val c=current in setOf(ProtoScreen.Catalog,ProtoScreen.Filter,ProtoScreen.ProductList,ProtoScreen.ProductDetail);val ca=current in setOf(ProtoScreen.Cart,ProtoScreen.Checkout);val o=current in setOf(ProtoScreen.OrderList,ProtoScreen.OrderDetail,ProtoScreen.OrderSent);val p=current in setOf(ProtoScreen.Profile,ProtoScreen.Suspended)
    NavigationBar(containerColor=ProtoPanel.copy(alpha=.98f),tonalElevation=0.dp){ProtoNavItem(h,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(c,"Каталог",Icons.Outlined.GridView,onCatalog);NavigationBarItem(selected=ca,onClick=onCart,icon={BadgedBox(badge={if(cartCount>0)Badge(containerColor=ProtoGold){Text(cartCount.toString(),color=Color.Black)}}){Icon(Icons.Outlined.ShoppingCart,null)}},label={Text("Корзина",fontSize=10.sp)},colors=protoNavColors());ProtoNavItem(o,"Заказы",Icons.Outlined.ReceiptLong,onOrders);ProtoNavItem(p,"Профиль",Icons.Outlined.Person,onProfile)}
}

@Composable
private fun ProtoAdminBottomBar(current:ProtoScreen,onHome:()->Unit,onClients:()->Unit,onOrders:()->Unit,onStock:()->Unit,onProfile:()->Unit){NavigationBar(containerColor=ProtoPanel.copy(alpha=.98f),tonalElevation=0.dp){ProtoNavItem(current==ProtoScreen.AdminHome,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(current in setOf(ProtoScreen.AdminClients,ProtoScreen.AdminClient),"Клиенты",Icons.Outlined.Groups,onClients);ProtoNavItem(current in setOf(ProtoScreen.AdminOrders,ProtoScreen.AdminOrderDetail),"Заказы",Icons.Outlined.ReceiptLong,onOrders);ProtoNavItem(current in setOf(ProtoScreen.Server,ProtoScreen.StockList,ProtoScreen.ReserveList),"Склад",Icons.Outlined.Inventory2,onStock);ProtoNavItem(current in setOf(ProtoScreen.AdminSettings,ProtoScreen.AdminSettingsDetail),"Профиль",Icons.Outlined.Person,onProfile)}}

@Composable
private fun ProtoProductionBottomBar(onHome:()->Unit,onProduction:()->Unit,onHistory:()->Unit,onStock:()->Unit,onProfile:()->Unit){NavigationBar(containerColor=ProtoPanel.copy(alpha=.98f),tonalElevation=0.dp){ProtoNavItem(false,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(true,"Производство",Icons.Outlined.Factory,onProduction);ProtoNavItem(false,"История",Icons.Outlined.History,onHistory);ProtoNavItem(false,"Склад",Icons.Outlined.Inventory2,onStock);ProtoNavItem(false,"Профиль",Icons.Outlined.Person,onProfile)}}

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
    "Цветы"->R.drawable.mock_flowers
    "Услуги"->R.drawable.mock_service
    else->R.drawable.mock_generic
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
                orders.filter { it.status != "Доставлен" }.forEach { o -> o.lines.forEach { l -> appendLine("${o.id};${o.clientName};${l.sku};${l.name};${l.qty};${o.status}") } }
            }
            "Выпуск за месяц" -> {
                appendLine("Дата;Время;SKU;Наименование;Количество;Сборщица;Оприходовал;Статус")
                productionOps.forEach { op -> appendLine("${op.date};${op.time};${op.sku};${op.name};${op.qty};${op.assembler};${op.postedBy};${op.status}") }
            }
            "Производство по сборщицам" -> {
                appendLine("Сборщица;Количество")
                productionOps.groupBy { it.assembler }.forEach { (assembler, ops) -> appendLine("$assembler;${ops.sumOf { it.qty }}") }
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
