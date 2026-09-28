package ru.sansara.app

import android.content.Context
import android.content.Intent
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
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ProtoBg = Color(0xFF0A0A09)
private val ProtoPanel = Color(0xFF171613)
private val ProtoPanel2 = Color(0xFF211E19)
private val ProtoGold = Color(0xFFD8B07C)
private val ProtoGoldSoft = Color(0xFFE7C59C)
private val ProtoText = Color(0xFFF5F0E8)
private val ProtoMuted = Color(0xFF9D9992)
private val ProtoGreen = Color(0xFF4BE087)
private val ProtoRed = Color(0xFFFF746C)
private val ProtoBorder = Color(0xFF4B4033)
private val ProtoOrange = Color(0xFFFFB15A)

private enum class ProtoScreen {
    Welcome, Login, Registration, RegistrationSent,
    Home, Filter, ProductList, ProductDetail, Cart, Checkout, OrderList, OrderDetail, Profile, Suspended,
    AdminHome, AdminClients, AdminClient, AdminOrders, AdminOrderDetail, AdminCatalog, AdminSettings, AdminAttention, OnlineController,
    Production, ProductionCategory, ProductionCatalog, ProductionEntry, ProductionHistory,
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
    val assembler: String
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
    val lastSeen: String
)

private data class ProtoOrderLine(val sku: String, val name: String, val qty: Int, val price: Int)

private data class ProtoOrder(
    val id: String,
    val clientName: String,
    val dateTime: String,
    val lines: List<ProtoOrderLine>,
    var status: String
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
    val phone2: String = ""
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
    var productionCategory by remember { mutableStateOf("Венки") }
    var productionProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var productionQty by remember { mutableIntStateOf(1) }
    var productionAssembler by remember { mutableStateOf("Анна К.") }
    var lowStockThreshold by remember { mutableIntStateOf(5) }
    var notificationsRegistration by remember { mutableStateOf(true) }
    var notificationsOrders by remember { mutableStateOf(true) }
    var notificationsProduction by remember { mutableStateOf(true) }
    var notificationsLowStock by remember { mutableStateOf(true) }
    val prefs = remember { context.getSharedPreferences("sansara", Context.MODE_PRIVATE) }
    var tildaFeedUrl by remember { mutableStateOf(prefs.getString("tilda_yml_url", BuildConfig.TILDA_YML_URL).orEmpty()) }
    var catalogSyncStatus by remember { mutableStateOf("Тестовый каталог · локальные данные") }
    var catalogSyncInProgress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val clients = remember {
        mutableStateListOf(
            ProtoClient("C-1024", "ООО Ритуал-Сервис", "Игорь Петров", "+7 999 123-45-67", "Оптовик", 10, 286_400, 7, "1024", true, "сейчас"),
            ProtoClient("C-1025", "Агент Смирнов А.А.", "Алексей Смирнов", "+7 916 222-18-44", "Активный", 5, 94_800, 3, "1025", true, "2 мин назад"),
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
            ), "Собирается"),
            ProtoOrder("S-002383", "Ритуал-Тула", "28.09.2026 08:47", listOf(
                ProtoOrderLine("V-060-007", "Венок Стандарт 60 см №07", 8, 2150)
            ), "Подтверждён"),
            ProtoOrder("S-002382", "Агент Смирнов А.А.", "27.09.2026 16:05", listOf(
                ProtoOrderLine("V-090-011", "Венок Премиум 90 см №11", 3, 3850),
                ProtoOrderLine("KOR-070-004", "Корзина 70 см №04", 2, 2450)
            ), "Доставляется")
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
    fun availableStock(p: ProtoCatalogProduct) = stockOverrides[p.sku] ?: p.stock
    fun postDrafts() {
        if (productionDrafts.isEmpty()) { toast("Добавьте позиции в выпуск дня"); return }
        productionDrafts.forEach { d ->
            stockOverrides[d.product.sku] = availableStock(d.product) + d.qty
            productionOps.add(0, ProtoProductionOp(currentDateShort(), currentTimeShort(), d.product.sku, d.product.name, d.qty, d.assembler, "Игорь Ф."))
        }
        val total = productionDrafts.sumOf { it.qty }
        productionDrafts.clear()
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
                catalogSyncStatus = "${result.message} · остатки и резервы сохранены"
                if (showToast) toast(catalogSyncStatus)
            } catch (e: Exception) {
                catalogSyncStatus = "Ошибка синхронизации: ${e.message ?: "неизвестная ошибка"}"
                if (showToast) toast(catalogSyncStatus)
            } finally { catalogSyncInProgress = false }
        }
    }

    LaunchedEffect(Unit) { if (tildaFeedUrl.isNotBlank()) syncTildaCatalog(showToast = false) }

    BackHandler(enabled = screen != ProtoScreen.Welcome) { back() }

    MaterialTheme(colorScheme = darkColorScheme(primary = ProtoGold, background = ProtoBg, surface = ProtoPanel)) {
        when (screen) {
            ProtoScreen.Welcome -> ProtoWelcomeScreen(onLogin = { go(ProtoScreen.Login) }, onRegister = { go(ProtoScreen.Registration) }, onRole = { showRolePicker = true })
            ProtoScreen.Login -> ProtoLoginScreen(onBack = { back() }, onLogin = { code ->
                val client = clients.firstOrNull { it.accessCode == code }
                if (client != null) { selectedClientId = client.id; if (client.status == "Приостановлен") go(ProtoScreen.Suspended) else go(ProtoScreen.Home) }
                else toast("Код доступа не найден")
            })
            ProtoScreen.Registration -> ProtoRegistrationScreen(onBack = { back() }, onSubmit = { reg -> registrations.add(0, reg); go(ProtoScreen.RegistrationSent) })
            ProtoScreen.RegistrationSent -> ProtoSimpleMessageScreen("Заявка отправлена", "Администратор проверит данные и после подтверждения выдаст код доступа.", onBack = { history.clear(); screen = ProtoScreen.Welcome })

            ProtoScreen.Home -> ProtoClientHomeScreen(
                client = clients.firstOrNull { it.id == selectedClientId } ?: clients.first(),
                products = products,
                stockOverrides = stockOverrides,
                cartCount = cart.values.sum(),
                query = searchQuery,
                onQuery = { searchQuery = it },
                onSearch = {
                    resetFilters();
                    go(ProtoScreen.ProductList)
                },
                onAvailability = { status -> resetFilters(availability = status); go(ProtoScreen.ProductList) },
                onCategory = { type ->
                    if (type in setOf("Гробы", "Кресты")) toast("Раздел «$type» в разработке")
                    else { resetFilters(type = type); go(ProtoScreen.Filter) }
                },
                onOpenProduct = { openProduct(it) },
                onCart = { go(ProtoScreen.Cart) },
                onOrders = { go(ProtoScreen.OrderList) },
                onProfile = { go(ProtoScreen.Profile) },
                onCatalog = { resetFilters(); go(ProtoScreen.Filter) }
            )
            ProtoScreen.Filter -> ProtoFilterScreen(
                selectedTypes, selectedQualities, selectedSizes, selectedAvailability,
                onToggleType = { if (it in setOf("Гробы","Кресты")) toast("Раздел «$it» в разработке") else selectedTypes = protoToggle(selectedTypes, it) },
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
                ProtoProductListScreen(filtered, cart, stockOverrides, onBack = { back() }, onOpenFilter = { go(ProtoScreen.Filter) }, onOpenProduct = { openProduct(it) }, onCart = { go(ProtoScreen.Cart) })
            }
            ProtoScreen.ProductDetail -> ProtoProductDetailScreen(
                product = selectedProduct,
                currentStock = selectedProduct?.let { availableStock(it) } ?: 0,
                qty = detailQty,
                onBack = { back() },
                onMinus = { detailQty = (detailQty - 1).coerceAtLeast(1) },
                onPlus = { detailQty += 1 },
                onAdd = {
                    selectedProduct?.let { p -> cart[p.sku] = (cart[p.sku] ?: 0) + detailQty; toast("Добавлено в корзину: ${detailQty} шт.") }
                    back()
                }
            )
            ProtoScreen.Cart -> ProtoCartScreen(products, cart, onBack = { back() }, onPlus = { p -> cart[p.sku] = (cart[p.sku] ?: 0) + 1 }, onMinus = { p -> val n = (cart[p.sku] ?: 1) - 1; if (n <= 0) cart.remove(p.sku) else cart[p.sku] = n }, onDelete = { cart.remove(it.sku) }, onCheckout = { go(ProtoScreen.Checkout) })
            ProtoScreen.Checkout -> ProtoCheckoutScreen(cartCount = cart.values.sum(), total = cart.entries.sumOf { (sku, q) -> (products.firstOrNull { it.sku == sku }?.price ?: 0) * q }, onBack = { back() }, onSubmit = {
                val lines = cart.mapNotNull { (sku,q) -> products.firstOrNull { it.sku == sku }?.let { ProtoOrderLine(it.sku,it.name,q,it.price) } }
                val id = "S-${(2385 + orders.size).toString().padStart(6,'0')}"
                orders.add(0, ProtoOrder(id, clients.first { it.id == selectedClientId }.name, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), lines, "Получен"))
                selectedOrderId = id; cart.clear(); go(ProtoScreen.OrderDetail)
            })
            ProtoScreen.OrderList -> ProtoOrderListScreen(orders.filter { it.clientName == (clients.firstOrNull { c -> c.id == selectedClientId }?.name ?: "") }, onBack = { back() }, onOpen = { selectedOrderId = it.id; go(ProtoScreen.OrderDetail) })
            ProtoScreen.OrderDetail -> ProtoOrderDetailScreen(orders.firstOrNull { it.id == selectedOrderId }, isAdmin = false, onBack = { back() }, onStatus = {})
            ProtoScreen.Profile -> ProtoProfileScreen(clients.firstOrNull { it.id == selectedClientId } ?: clients.first(), onBack = { back() }, onCall = { protoDial(context) }, onLogout = { history.clear(); screen = ProtoScreen.Welcome })
            ProtoScreen.Suspended -> ProtoSuspendedScreen(onCall = { protoDial(context) }, onMessage = { protoMessage(context) }, onBack = { history.clear(); screen = ProtoScreen.Welcome })

            ProtoScreen.AdminHome -> ProtoAdminHomeScreen(
                clients = clients, orders = orders, productionOps = productionOps, products = products, stockOverrides = stockOverrides,
                onSearch = { toast("Поиск активен: клиенты / заказы / товары") },
                onRegistrations = { go(ProtoScreen.AdminAttention) }, onClients = { go(ProtoScreen.AdminClients) }, onOrders = { go(ProtoScreen.AdminOrders) },
                onProduction = { go(ProtoScreen.ProductionHistory) }, onStock = { go(ProtoScreen.Server) }, onCatalog = { go(ProtoScreen.AdminCatalog) }, onSettings = { go(ProtoScreen.AdminSettings) },
                onAttention = { go(ProtoScreen.AdminAttention) }, onOnline = { go(ProtoScreen.OnlineController) }
            )
            ProtoScreen.AdminClients -> ProtoAdminClientsScreen(clients, onBack = { back() }, onOpen = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.AdminClient -> ProtoAdminClientScreen(clients.firstOrNull { it.id == selectedClientId }, onBack = { back() }, onToggleBlock = {
                val i = clients.indexOfFirst { it.id == selectedClientId }; if (i >= 0) { val c = clients[i]; clients[i] = c.copy(status = if (c.status == "Приостановлен") "Активный" else "Приостановлен") }
            }, onDiscount = { delta -> val i = clients.indexOfFirst { it.id == selectedClientId }; if (i >= 0) { val c = clients[i]; clients[i] = c.copy(discount = (c.discount + delta).coerceIn(0,50)) } })
            ProtoScreen.AdminOrders -> ProtoAdminOrdersScreen(orders, onBack = { back() }, onOpen = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) })
            ProtoScreen.AdminOrderDetail -> ProtoOrderDetailScreen(orders.firstOrNull { it.id == selectedOrderId }, isAdmin = true, onBack = { back() }, onStatus = { st -> orders.firstOrNull { it.id == selectedOrderId }?.status = st })
            ProtoScreen.AdminCatalog -> ProtoAdminCatalogScreen(products, stockOverrides, onBack = { back() })
            ProtoScreen.AdminSettings -> ProtoAdminSettingsScreen(lowStockThreshold, notificationsRegistration, notificationsOrders, notificationsProduction, notificationsLowStock, tildaFeedUrl, catalogSyncStatus, catalogSyncInProgress, onBack = { back() }, onThreshold = { lowStockThreshold = it.coerceIn(1,50) }, onReg = { notificationsRegistration = it }, onOrders = { notificationsOrders = it }, onProd = { notificationsProduction = it }, onLow = { notificationsLowStock = it }, onTildaUrl = { tildaFeedUrl = it }, onSync = { syncTildaCatalog() })
            ProtoScreen.AdminAttention -> ProtoAttentionScreen(registrations, clients.filter { it.status == "Приостановлен" }, orders.filter { it.status == "Собирается" }, onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) }, onOrder = { selectedOrderId = it.id; go(ProtoScreen.AdminOrderDetail) })
            ProtoScreen.OnlineController -> ProtoOnlineControllerScreen(clients, onBack = { back() }, onClient = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })

            ProtoScreen.Production -> ProtoProductionHomeScreen(
                drafts = productionDrafts, opsToday = productionOps.filter { it.date == currentDateShort() }, products = products, stockOverrides = stockOverrides,
                onAdd = { go(ProtoScreen.ProductionCategory) }, onPostAll = { postDrafts() }, onHistory = { go(ProtoScreen.ProductionHistory) }, onStock = { go(ProtoScreen.Server) }
            )
            ProtoScreen.ProductionCategory -> ProtoProductionCategoryScreen(onBack = { back() }, onCategory = { productionCategory = it; go(ProtoScreen.ProductionCatalog) })
            ProtoScreen.ProductionCatalog -> ProtoProductionCatalogScreen(products.filter { it.type == productionCategory }, onBack = { back() }, onSelect = { productionProduct = it; productionQty = 1; go(ProtoScreen.ProductionEntry) })
            ProtoScreen.ProductionEntry -> ProtoProductionEntryScreen(productionProduct, productionQty, productionAssembler, onBack = { back() }, onMinus = { productionQty = (productionQty - 1).coerceAtLeast(1) }, onPlus = { productionQty += 1 }, onAssembler = { productionAssembler = it }, onAddDraft = {
                productionProduct?.let { p -> productionDrafts.add(ProtoProductionDraft(p, productionQty, productionAssembler)); toast("Позиция добавлена в выпуск дня") }; history.clear(); screen = ProtoScreen.Production
            }, onPostNow = {
                productionProduct?.let { p -> stockOverrides[p.sku] = availableStock(p) + productionQty; productionOps.add(0, ProtoProductionOp(currentDateShort(), currentTimeShort(), p.sku, p.name, productionQty, productionAssembler, "Игорь Ф.")); toast("Оприходовано: ${p.name} +$productionQty шт.") }; history.clear(); screen = ProtoScreen.Production
            })
            ProtoScreen.ProductionHistory -> ProtoProductionHistoryScreen(productionOps, onBack = { back() })

            ProtoScreen.Server -> ProtoServerScreen(products, stockOverrides, productionOps, orders, clients, onBack = { back() }, onStock = { go(ProtoScreen.StockList) }, onReserve = { go(ProtoScreen.ReserveList) }, onNewClients = { go(ProtoScreen.NewClients) }, onOnline = { go(ProtoScreen.OnlineController) }, onExport = { go(ProtoScreen.Export) })
            ProtoScreen.StockList -> ProtoStockListScreen(products, stockOverrides, onBack = { back() })
            ProtoScreen.ReserveList -> ProtoReserveListScreen(orders, onBack = { back() })
            ProtoScreen.NewClients -> ProtoNewClientsScreen(clients, onBack = { back() }, onOpen = { selectedClientId = it.id; go(ProtoScreen.AdminClient) })
            ProtoScreen.Export -> ProtoExportScreen(onBack = { back() }, onExport = { toast("Тестовый экспорт сформирован. В коммерческой версии: XLSX/PDF") })
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
private fun ProtoWelcomeScreen(onLogin:()->Unit,onRegister:()->Unit,onRole:()->Unit) {
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_welcome), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        Box(Modifier.align(Alignment.TopCenter).padding(top=36.dp).width(220.dp).height(80.dp).clickable(onClick=onRole))
        Column(Modifier.align(Alignment.BottomCenter).padding(horizontal=28.dp, vertical=72.dp).fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Button(onClick=onLogin, modifier=Modifier.fillMaxWidth().height(56.dp), colors=ButtonDefaults.buttonColors(containerColor=ProtoGold), shape=RoundedCornerShape(14.dp)) { Text("Войти", color=Color.Black, fontWeight=FontWeight.Bold, fontSize=17.sp) }
            OutlinedButton(onClick=onRegister, modifier=Modifier.fillMaxWidth().height(56.dp), border=BorderStroke(1.dp,ProtoGold), shape=RoundedCornerShape(14.dp)) { Text("Стать партнёром", color=ProtoGold, fontWeight=FontWeight.Bold) }
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
private fun ProtoRegistrationScreen(onBack:()->Unit,onSubmit:(ProtoRegistration)->Unit) {
    var organization by remember { mutableStateOf("") }
    var inn by remember { mutableStateOf("") }
    var contact1 by remember { mutableStateOf("") }
    var phone1 by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ProtoClientType.AGENT.label) }
    var consent by remember { mutableStateOf(false) }
    var showSecond by remember { mutableStateOf(false) }
    var contact2 by remember { mutableStateOf("") }
    var phone2 by remember { mutableStateOf("") }
    val valid = organization.isNotBlank() && inn.isNotBlank() && contact1.isNotBlank() && phone1.isNotBlank() && email.isNotBlank() && city.isNotBlank() && consent

    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_registration), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            @Composable
            fun visualField(value:String,onChange:(String)->Unit,x:Float,y:Float,w:Float,h:Float,keyboardType:KeyboardType=KeyboardType.Text) {
                BasicTextField(
                    value=value,onValueChange=onChange,singleLine=true,
                    textStyle=LocalTextStyle.current.copy(color=ProtoText,fontSize=14.sp),
                    keyboardOptions=KeyboardOptions(keyboardType=keyboardType),
                    modifier=Modifier.offset(maxWidth*x,maxHeight*y).size(maxWidth*w,maxHeight*h).padding(horizontal=4.dp)
                )
            }
            visualField(organization,{organization=it},.18f,.218f,.73f,.045f)
            visualField(inn,{inn=it},.18f,.282f,.73f,.045f,KeyboardType.Number)
            visualField(contact1,{contact1=it},.18f,.346f,.67f,.045f)
            visualField(phone1,{phone1=it},.18f,.409f,.73f,.048f,KeyboardType.Phone)
            visualField(email,{email=it},.18f,.475f,.73f,.045f,KeyboardType.Email)
            visualField(city,{city=it},.18f,.539f,.64f,.045f)
            visualField(address,{address=it},.18f,.603f,.73f,.045f)

            Box(Modifier.offset(maxWidth*.83f,maxHeight*.342f).size(42.dp).clip(CircleShape).background(Color(0xAA1A1815)).border(1.dp,ProtoGold,CircleShape).clickable{showSecond=true},contentAlignment=Alignment.Center){Text("+",color=ProtoGold,fontSize=23.sp)}

            Box(Modifier.offset(maxWidth*.045f,maxHeight*.689f).size(maxWidth*.45f,maxHeight*.047f).clickable{type=ProtoClientType.AGENT.label})
            Box(Modifier.offset(maxWidth*.50f,maxHeight*.689f).size(maxWidth*.45f,maxHeight*.047f).clickable{type=ProtoClientType.TRADING.label})
            if(type==ProtoClientType.TRADING.label){
                Box(Modifier.offset(maxWidth*.50f,maxHeight*.689f).size(maxWidth*.45f,maxHeight*.047f).background(ProtoGold.copy(alpha=.18f),RoundedCornerShape(14.dp)).border(1.dp,ProtoGold,RoundedCornerShape(14.dp)))
            }
            if(consent){
                Box(Modifier.offset(maxWidth*.05f,maxHeight*.765f).size(32.dp).background(ProtoGold,RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Check,null,tint=Color.Black,modifier=Modifier.size(20.dp))}
            }
            Box(Modifier.offset(maxWidth*.04f,maxHeight*.755f).size(maxWidth*.90f,maxHeight*.055f).clickable{consent=!consent})
            Box(Modifier.offset(maxWidth*.05f,maxHeight*.817f).size(maxWidth*.90f,maxHeight*.065f).clickable{
                if(valid) onSubmit(ProtoRegistration(organization,organization,inn,contact1,phone1,email,city,address,type,contact2,phone2))
            })
            Box(Modifier.offset(maxWidth*.28f,maxHeight*.918f).size(maxWidth*.44f,maxHeight*.045f).clickable{onBack()})
        }
        ProtoBackButton(onClick=onBack)
    }

    if(showSecond){
        AlertDialog(
            onDismissRequest={showSecond=false}, containerColor=ProtoPanel,
            title={Text("Дополнительное контактное лицо",color=ProtoText)},
            text={Column{ProtoField(contact2,{contact2=it},"ФИО");ProtoField(phone2,{phone2=it},"Телефон",KeyboardType.Phone)}},
            confirmButton={TextButton(onClick={showSecond=false}){Text("Сохранить",color=ProtoGold)}},
            dismissButton={TextButton(onClick={contact2="";phone2="";showSecond=false}){Text("Удалить",color=ProtoMuted)}}
        )
    }
}
@Composable
private fun ProtoClientHomeScreen(client:ProtoClient,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,cartCount:Int,query:String,onQuery:(String)->Unit,onSearch:()->Unit,onAvailability:(String)->Unit,onCategory:(String)->Unit,onOpenProduct:(ProtoCatalogProduct)->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit,onCatalog:()->Unit) {
    var searchOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_client_home), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.offset(maxWidth*.035f,maxHeight*.195f).size(maxWidth*.93f,maxHeight*.065f).clickable{searchOpen=true})
            Box(Modifier.offset(maxWidth*.035f,maxHeight*.268f).size(maxWidth*.29f,maxHeight*.048f).clickable{onAvailability("В наличии")})
            Box(Modifier.offset(maxWidth*.34f,maxHeight*.268f).size(maxWidth*.30f,maxHeight*.048f).clickable{onAvailability("Под заказ")})
            Box(Modifier.offset(maxWidth*.03f,maxHeight*.315f).size(maxWidth*.16f,maxHeight*.115f).clickable{onCategory("Венки")})
            Box(Modifier.offset(maxWidth*.20f,maxHeight*.315f).size(maxWidth*.15f,maxHeight*.115f).clickable{onCategory("Гробы")})
            Box(Modifier.offset(maxWidth*.36f,maxHeight*.315f).size(maxWidth*.15f,maxHeight*.115f).clickable{onCategory("Одежда")})
            Box(Modifier.offset(maxWidth*.52f,maxHeight*.315f).size(maxWidth*.15f,maxHeight*.115f).clickable{onCategory("Ленты")})
            Box(Modifier.offset(maxWidth*.68f,maxHeight*.315f).size(maxWidth*.15f,maxHeight*.115f).clickable{onCategory("Флоретки")})
            Box(Modifier.offset(maxWidth*.84f,maxHeight*.315f).size(maxWidth*.13f,maxHeight*.115f).clickable{onCatalog()})

            // Approved placement is preserved; only the popular-products window becomes a real vertical scroller.
            Box(Modifier.offset(maxWidth*.025f,maxHeight*.485f).size(maxWidth*.95f,maxHeight*.395f).background(ProtoBg)) {
                LazyColumn(contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(products.take(20).chunked(2)) { row ->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            row.forEach { p ->
                                Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)) {
                                    ProtoProductImage(p,Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(topStart=14.dp,topEnd=14.dp)))
                                    Column(Modifier.padding(8.dp)) {
                                        Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=12.sp)
                                        Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=9.sp)
                                        Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold,fontSize=16.sp)
                                        Button(onClick={onOpenProduct(p)},modifier=Modifier.fillMaxWidth().height(34.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),contentPadding=PaddingValues(horizontal=3.dp)) {Text("В корзину",color=Color.Black,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)}
                                    }
                                }
                            }
                            if(row.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){}
            PrototypeClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){onCatalog()}
            PrototypeClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){onCart()}
            PrototypeClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){onOrders()}
            PrototypeClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){onProfile()}
        }
    }

    if(searchOpen){
        AlertDialog(onDismissRequest={searchOpen=false},containerColor=ProtoPanel,title={Text("Поиск по каталогу",color=ProtoText)},text={ProtoField(query,onQuery,"Артикул или название")},confirmButton={TextButton(onClick={searchOpen=false;onSearch()}){Text("Найти",color=ProtoGold)}},dismissButton={TextButton(onClick={searchOpen=false}){Text("Отмена",color=ProtoMuted)}})
    }
}
@Composable
private fun ProtoFilterScreen(selectedTypes:Set<String>,selectedQualities:Set<String>,selectedSizes:Set<String>,selectedAvailability:Set<String>,onToggleType:(String)->Unit,onToggleQuality:(String)->Unit,onToggleSize:(String)->Unit,onToggleAvailability:(String)->Unit,onShow:()->Unit,onBack:()->Unit) {
    Column(Modifier.fillMaxSize().background(ProtoBg).padding(horizontal=16.dp)) {
        Spacer(Modifier.height(22.dp)); ProtoHeader("Фильтр каталога","Выберите параметры и нажмите «Показать товары»",onBack)
        Spacer(Modifier.height(8.dp)); ProtoCompactGrid("Продукция",listOf("Венки","Венки круглые","Корзины","Полянки","Флоретки","Ленты","Гробы","Кресты"),selectedTypes,onToggleType,disabled=setOf("Гробы","Кресты"))
        ProtoCompactGrid("Качество",listOf("Премиум","Стандарт","Эконом"),selectedQualities,onToggleQuality)
        ProtoCompactGrid("Размер",listOf("60 см","90 см","110 см","125 см","140 см"),selectedSizes,onToggleSize)
        ProtoCompactGrid("Наличие",listOf("В наличии","Под заказ"),selectedAvailability,onToggleAvailability)
        Spacer(Modifier.weight(1f)); Button(onClick=onShow,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(14.dp)){Icon(Icons.Outlined.Search,null,tint=Color.Black);Spacer(Modifier.width(8.dp));Text("Показать товары",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=17.sp)};Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ProtoProductListScreen(products:List<ProtoCatalogProduct>,cart:SnapshotStateMap<String,Int>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit,onOpenFilter:()->Unit,onOpenProduct:(ProtoCatalogProduct)->Unit,onCart:()->Unit) {
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        Row(Modifier.padding(16.dp,22.dp,16.dp,8.dp),verticalAlignment=Alignment.CenterVertically){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("Каталог",color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold);Text("Найдено: ${products.size} позиций",color=ProtoMuted)};BadgedBox(badge={if(cart.values.sum()>0)Badge(containerColor=ProtoGold){Text(cart.values.sum().toString(),color=Color.Black)}}){IconButton(onClick=onCart){Icon(Icons.Outlined.ShoppingCart,null,tint=ProtoGold)}}}
        OutlinedButton(onClick=onOpenFilter,modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth(),border=BorderStroke(1.dp,ProtoBorder)){Icon(Icons.Outlined.Tune,null,tint=ProtoGold);Spacer(Modifier.width(8.dp));Text("Изменить фильтр",color=ProtoGold)}
        if(products.isEmpty())Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("По выбранным параметрам товаров нет",color=ProtoMuted)} else LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(products,key={it.sku}){p->ProtoProductCard(p,stockOverrides[p.sku]?:p.stock){onOpenProduct(p)}}}
    }
}

@Composable
private fun ProtoProductCard(p:ProtoCatalogProduct,currentStock:Int,onOpen:()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(15.dp)){
        Box(Modifier.fillMaxWidth().height(118.dp).background(ProtoPanel2).clickable(onClick=onOpen)){ProtoProductImage(p,Modifier.fillMaxSize());Surface(Modifier.align(Alignment.BottomStart).padding(7.dp),color=if(currentStock>0)Color(0xDD123A27)else Color(0xDD4A2220),shape=RoundedCornerShape(18.dp)){Text(if(currentStock>0)"В наличии $currentStock" else "Под заказ · ${p.productionDays} дн.",color=if(currentStock>0)ProtoGreen else ProtoGoldSoft,fontSize=9.sp,modifier=Modifier.padding(horizontal=7.dp,vertical=3.dp))}}
        Column(Modifier.padding(9.dp)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=10.sp);Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.padding(vertical=4.dp));Button(onClick=onOpen,modifier=Modifier.fillMaxWidth().height(38.dp),contentPadding=PaddingValues(horizontal=4.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(9.dp)){Icon(Icons.Outlined.AddShoppingCart,null,tint=Color.Black,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("В корзину",color=Color.Black,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)}}
    }
}

@Composable
private fun ProtoProductDetailScreen(product:ProtoCatalogProduct?,currentStock:Int,qty:Int,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAdd:()->Unit) {
    val p=product?:return
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_product),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The approved composition and sizes stay fixed; data and photo are live overlays.
            Box(Modifier.offset(maxWidth*.10f,maxHeight*.105f).size(maxWidth*.80f,maxHeight*.31f).clip(RoundedCornerShape(16.dp)).background(ProtoPanel2)) {
                ProtoProductImage(p,Modifier.fillMaxSize())
            }
            Box(Modifier.offset(maxWidth*.045f,maxHeight*.438f).size(maxWidth*.90f,maxHeight*.13f).background(ProtoBg.copy(alpha=.96f)))
            Text(p.name,color=ProtoText,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(maxWidth*.05f,maxHeight*.444f))
            Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=12.sp,modifier=Modifier.offset(maxWidth*.05f,maxHeight*.479f))
            Text(protoMoney(p.price),color=ProtoText,fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(maxWidth*.05f,maxHeight*.522f))
            Text(if(currentStock>0)"● В наличии $currentStock шт." else "● Под заказ · от ${p.productionDays} дней",color=if(currentStock>0)ProtoGreen else ProtoGoldSoft,fontSize=11.sp,modifier=Modifier.offset(maxWidth*.70f,maxHeight*.482f))
            Box(Modifier.offset(maxWidth*.045f,maxHeight*.571f).size(maxWidth*.29f,maxHeight*.055f).background(ProtoBg.copy(alpha=.88f),RoundedCornerShape(12.dp)))
            Text(qty.toString(),color=ProtoText,fontSize=22.sp,modifier=Modifier.offset(maxWidth*.175f,maxHeight*.584f))
            PrototypeClickArea(maxWidth,maxHeight,.04f,.57f,.10f,.06f){onMinus()}
            PrototypeClickArea(maxWidth,maxHeight,.24f,.57f,.10f,.06f){onPlus()}
            PrototypeClickArea(maxWidth,maxHeight,.36f,.57f,.59f,.06f){onAdd()}
            Box(Modifier.offset(maxWidth*.045f,maxHeight*.655f).size(maxWidth*.91f,maxHeight*.145f).background(ProtoBg.copy(alpha=.92f),RoundedCornerShape(14.dp))) {
                Column(Modifier.padding(12.dp)){ProtoInfoRow("Тип",p.type);ProtoInfoRow("Качество",p.quality);ProtoInfoRow("Размер",p.size);ProtoInfoRow("Источник",if(p.imageUrl.isBlank())"Тестовый каталог" else "Tilda")}
            }
            PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){}
            PrototypeClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){onBack()}
        }
        ProtoBackButton(onBack)
    }
}
@Composable
private fun ProtoCartScreen(products:List<ProtoCatalogProduct>,cart:SnapshotStateMap<String,Int>,onBack:()->Unit,onPlus:(ProtoCatalogProduct)->Unit,onMinus:(ProtoCatalogProduct)->Unit,onDelete:(ProtoCatalogProduct)->Unit,onCheckout:()->Unit) {
    val lines=cart.mapNotNull{(sku,q)->products.firstOrNull{it.sku==sku}?.let{it to q}}
    val total=lines.sumOf{it.first.price*it.second}
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_cart),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.offset(maxWidth*.03f,maxHeight*.19f).size(maxWidth*.94f,maxHeight*.45f).background(ProtoBg)) {
                if(lines.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Корзина пока пуста",color=ProtoMuted)}
                else LazyColumn(contentPadding=PaddingValues(vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(lines,key={it.first.sku}) { (p,q) ->
                        Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().height(108.dp)) {
                            Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                                ProtoProductImage(p,Modifier.size(90.dp).clip(RoundedCornerShape(10.dp)))
                                Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=9.sp);Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={onMinus(p)},modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(q.toString(),color=ProtoText);IconButton(onClick={onPlus(p)},modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}}}
                                Column(horizontalAlignment=Alignment.End){Text(protoMoney(p.price*q),color=ProtoGoldSoft,fontWeight=FontWeight.Bold);IconButton(onClick={onDelete(p)},modifier=Modifier.size(34.dp)){Icon(Icons.Outlined.Delete,null,tint=ProtoGold)}}
                            }
                        }
                    }
                }
            }
            Box(Modifier.offset(maxWidth*.04f,maxHeight*.655f).size(maxWidth*.92f,maxHeight*.105f).background(ProtoBg.copy(alpha=.95f),RoundedCornerShape(14.dp))) {
                Column(Modifier.padding(12.dp)){ProtoInfoRow("Товары","${lines.sumOf{it.second}} шт.");ProtoInfoRow("Итого",protoMoney(total))}
            }
            PrototypeClickArea(maxWidth,maxHeight,.04f,.79f,.92f,.07f){if(lines.isNotEmpty())onCheckout()}
            PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){onBack()}
        }
        ProtoBackButton(onBack)
    }
}
@Composable
private fun ProtoCheckoutScreen(cartCount:Int,total:Int,onBack:()->Unit,onSubmit:()->Unit){
    var method by remember{mutableStateOf("Доставка")}
    var address by remember{mutableStateOf("")}
    var comment by remember{mutableStateOf("")}
    var editAddress by remember{mutableStateOf(false)}
    var editComment by remember{mutableStateOf(false)}
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_checkout),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            PrototypeClickArea(maxWidth,maxHeight,.05f,.335f,.31f,.06f){method="Доставка"}
            PrototypeClickArea(maxWidth,maxHeight,.36f,.335f,.31f,.06f){method="Самовывоз"}
            PrototypeClickArea(maxWidth,maxHeight,.67f,.335f,.28f,.06f){method="ТК"}
            PrototypeClickArea(maxWidth,maxHeight,.17f,.425f,.78f,.07f){editAddress=true}
            PrototypeClickArea(maxWidth,maxHeight,.17f,.52f,.78f,.09f){editComment=true}
            Box(Modifier.offset(maxWidth*.56f,maxHeight*.69f).size(maxWidth*.39f,maxHeight*.04f).background(ProtoBg.copy(alpha=.92f)))
            Text(protoMoney(total),color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(maxWidth*.70f,maxHeight*.70f))
            PrototypeClickArea(maxWidth,maxHeight,.04f,.775f,.92f,.065f){onSubmit()}
        }
        ProtoBackButton(onBack)
    }
    if(editAddress)AlertDialog(onDismissRequest={editAddress=false},containerColor=ProtoPanel,title={Text("Адрес / транспортная компания",color=ProtoText)},text={ProtoField(address,{address=it},"Адрес получения")},confirmButton={TextButton(onClick={editAddress=false}){Text("Сохранить",color=ProtoGold)}})
    if(editComment)AlertDialog(onDismissRequest={editComment=false},containerColor=ProtoPanel,title={Text("Комментарий к заказу",color=ProtoText)},text={ProtoField(comment,{comment=it},"Комментарий")},confirmButton={TextButton(onClick={editComment=false}){Text("Сохранить",color=ProtoGold)}})
}
@Composable
private fun ProtoOrderListScreen(orders:List<ProtoOrder>,onBack:()->Unit,onOpen:(ProtoOrder)->Unit){ProtoScaffold("Заказы","История и текущий статус",onBack){if(orders.isEmpty())item{Text("Заказов пока нет",color=ProtoMuted)}else items(orders,key={it.id}){o->ProtoOrderRow(o){onOpen(o)}}}}

@Composable
private fun ProtoOrderDetailScreen(order:ProtoOrder?,isAdmin:Boolean,onBack:()->Unit,onStatus:(String)->Unit){val o=order?:return;val statuses=listOf("Получен","Подтверждён","Собирается","Доставляется","Доставлен");ProtoScaffold("Заказ ${o.id}","${o.clientName} · ${protoMoney(o.total)}",onBack){item{ProtoSectionCard{Text("Статус заказа",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));statuses.forEachIndexed{i,s->val reached=statuses.indexOf(o.status)>=i;Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(12.dp).background(if(reached)ProtoGreen else ProtoBorder,CircleShape));Spacer(Modifier.width(9.dp));Text(s,color=if(reached)ProtoText else ProtoMuted,modifier=Modifier.weight(1f));if(isAdmin)RadioButton(selected=o.status==s,onClick={onStatus(s)},colors=RadioButtonDefaults.colors(selectedColor=ProtoGold))}}}};item{Text("История заказа",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Каждая смена статуса будет сохранять дату, время и ответственного сотрудника в коммерческой версии.",color=ProtoMuted,fontSize=12.sp)};items(o.lines){l->ProtoSectionCard{Text(l.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Арт. ${l.sku} · ${l.qty} шт. · ${protoMoney(l.price*l.qty)}",color=ProtoMuted)}}}}

@Composable
private fun ProtoProfileScreen(client:ProtoClient,onBack:()->Unit,onCall:()->Unit,onLogout:()->Unit) {
    ProtoScaffold("Профиль", client.name, onBack) {
        item {
            ProtoSectionCard {
                ProtoInfoRow("Статус", client.status)
                ProtoInfoRow("Скидка", "${client.discount}%")
                ProtoInfoRow("Контакт", client.contact)
                ProtoInfoRow("Телефон", client.phone)
                ProtoInfoRow("Администратор", "+7 926 304-60-19")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick=onCall, modifier=Modifier.fillMaxWidth(), border=BorderStroke(1.dp,ProtoGold)) {
                    Icon(Icons.Outlined.Phone,null,tint=ProtoGold)
                    Spacer(Modifier.width(8.dp))
                    Text("Позвонить администратору",color=ProtoGold)
                }
                TextButton(onClick=onLogout, modifier=Modifier.fillMaxWidth()) { Text("Выйти",color=ProtoMuted) }
            }
        }
    }
}

@Composable
private fun ProtoSuspendedScreen(onCall:()->Unit,onMessage:()->Unit,onBack:()->Unit) {
    ProtoScaffold("Доступ приостановлен", "Просмотр доступен, оформление заказов временно ограничено", onBack) {
        item {
            ProtoSectionCard {
                Text("Свяжитесь с администратором для восстановления доступа.",color=ProtoText)
                Spacer(Modifier.height(12.dp))
                Button(onClick=onCall,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)) {
                    Text("Позвонить: +7 926 304-60-19",color=Color.Black,fontWeight=FontWeight.Bold)
                }
                OutlinedButton(onClick=onMessage,modifier=Modifier.fillMaxWidth().padding(top=8.dp),border=BorderStroke(1.dp,ProtoGold)) {
                    Text("Написать администратору",color=ProtoGold)
                }
            }
        }
    }
}

@Composable
private fun ProtoAdminHomeScreen(
    clients:List<ProtoClient>, orders:List<ProtoOrder>, productionOps:List<ProtoProductionOp>, products:List<ProtoCatalogProduct>,
    stockOverrides:SnapshotStateMap<String,Int>, onSearch:()->Unit, onRegistrations:()->Unit, onClients:()->Unit, onOrders:()->Unit,
    onProduction:()->Unit, onStock:()->Unit, onCatalog:()->Unit, onSettings:()->Unit, onAttention:()->Unit, onOnline:()->Unit
) {
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_admin_home),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            PrototypeClickArea(maxWidth,maxHeight,.06f,.18f,.88f,.06f){onSearch()}
            PrototypeClickArea(maxWidth,maxHeight,.06f,.25f,.43f,.10f){onRegistrations()}
            PrototypeClickArea(maxWidth,maxHeight,.51f,.25f,.43f,.10f){onClients()}
            PrototypeClickArea(maxWidth,maxHeight,.06f,.36f,.43f,.10f){onOrders()}
            PrototypeClickArea(maxWidth,maxHeight,.51f,.36f,.43f,.10f){onProduction()}
            PrototypeClickArea(maxWidth,maxHeight,.06f,.47f,.88f,.08f){onStock()}
            PrototypeClickArea(maxWidth,maxHeight,.06f,.57f,.22f,.09f){onClients()}
            PrototypeClickArea(maxWidth,maxHeight,.29f,.57f,.22f,.09f){onOrders()}
            PrototypeClickArea(maxWidth,maxHeight,.52f,.57f,.22f,.09f){onCatalog()}
            PrototypeClickArea(maxWidth,maxHeight,.75f,.57f,.20f,.09f){onSettings()}
            PrototypeClickArea(maxWidth,maxHeight,.06f,.68f,.88f,.13f){onAttention()}
            PrototypeClickArea(maxWidth,maxHeight,.06f,.82f,.88f,.07f){onOnline()}
            PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){}
            PrototypeClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){onClients()}
            PrototypeClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){onOrders()}
            PrototypeClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){onStock()}
            PrototypeClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){onSettings()}
        }
    }
}
@Composable
private fun ProtoAdminClientsScreen(clients:List<ProtoClient>,onBack:()->Unit,onOpen:(ProtoClient)->Unit){var q by remember{mutableStateOf("")};val filtered=clients.filter{q.isBlank()||it.name.contains(q,true)||it.contact.contains(q,true)};ProtoScaffold("Клиенты","Оборот за ${currentMonthLabel().lowercase(ruLocale)}",onBack){item{ProtoField(q,{q=it},"Поиск клиента")};items(filtered,key={it.id}){c->Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.fillMaxWidth().clickable{onOpen(c)}){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.status} · ${c.orderCount} заказов",color=ProtoMuted,fontSize=12.sp)};Column(horizontalAlignment=Alignment.End){Text(protoMoney(c.monthTurnover),color=ProtoGoldSoft,fontWeight=FontWeight.Bold);Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}}

@Composable
private fun ProtoAdminClientScreen(client:ProtoClient?,onBack:()->Unit,onToggleBlock:()->Unit,onDiscount:(Int)->Unit) {
    val c=client?:return
    ProtoScaffold("Карточка клиента",c.name,onBack) {
        item {
            ProtoSectionCard {
                ProtoInfoRow("ID",c.id)
                ProtoInfoRow("Статус",c.status)
                ProtoInfoRow("Контакт",c.contact)
                ProtoInfoRow("Телефон",c.phone)
                ProtoInfoRow("Оборот месяца",protoMoney(c.monthTurnover))
                ProtoInfoRow("Заказов",c.orderCount.toString())
                Divider(color=ProtoBorder,modifier=Modifier.padding(vertical=8.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Скидка",color=ProtoMuted); Spacer(Modifier.weight(1f))
                    IconButton(onClick={onDiscount(-1)}){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)}
                    Text("${c.discount}%",color=ProtoText,fontWeight=FontWeight.Bold)
                    IconButton(onClick={onDiscount(1)}){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}
                }
                ProtoInfoRow("Код доступа",c.accessCode)
                Text("Код виден администратору. В коммерческой версии можно сгенерировать новый код.",color=ProtoMuted,fontSize=11.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick=onToggleBlock,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=if(c.status=="Приостановлен")ProtoGreen else ProtoRed)) {
                    Text(if(c.status=="Приостановлен")"Разблокировать клиента" else "Приостановить клиента",color=Color.Black,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProtoAdminOrdersScreen(orders:List<ProtoOrder>,onBack:()->Unit,onOpen:(ProtoOrder)->Unit){ProtoScaffold("Заказы",currentMonthLabel(),onBack){items(orders,key={it.id}){o->ProtoOrderRow(o){onOpen(o)}}}}

@Composable
private fun ProtoAdminCatalogScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit){ProtoScaffold("Каталог","Все тестовые позиции",onBack){items(products,key={it.sku}){p->ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${p.sku} · ${p.type} · ${p.size}",color=ProtoMuted,fontSize=11.sp)};Text("${stockOverrides[p.sku]?:p.stock} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoAdminSettingsScreen(
    threshold:Int,reg:Boolean,orders:Boolean,prod:Boolean,low:Boolean,tildaUrl:String,syncStatus:String,syncing:Boolean,
    onBack:()->Unit,onThreshold:(Int)->Unit,onReg:(Boolean)->Unit,onOrders:(Boolean)->Unit,onProd:(Boolean)->Unit,onLow:(Boolean)->Unit,
    onTildaUrl:(String)->Unit,onSync:()->Unit
){
    ProtoScaffold("Настройки","Управление бизнес-правилами и синхронизацией",onBack){
        item{ProtoSectionCard{Text("Уведомления",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);ProtoSwitchRow("Новые регистрации",reg,onReg);ProtoSwitchRow("Новые заказы",orders,onOrders);ProtoSwitchRow("Производство",prod,onProd);ProtoSwitchRow("Низкие остатки",low,onLow)}}
        item{ProtoSectionCard{Text("Порог низкого остатка",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={onThreshold(threshold-1)}){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text("$threshold шт.",color=ProtoGoldSoft,fontSize=22.sp,fontWeight=FontWeight.Bold);IconButton(onClick={onThreshold(threshold+1)}){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}};Text("Позиции с доступным остатком ≤ порога попадают в «Требует внимания».",color=ProtoMuted,fontSize=11.sp);ProtoInfoRow("Телефон администратора","+7 926 304-60-19")}}
        item{ProtoSectionCard{
            Text("Каталог Tilda",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold)
            Text("Вставьте публичную YML-ссылку каталога Tilda. Синхронизация обновляет карточки, цены, категории и фотографии, но не перезаписывает склад, резерв, производство и историю заказов.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(vertical=6.dp))
            ProtoField(tildaUrl,onTildaUrl,"YML-ссылка каталога Tilda")
            Button(onClick=onSync,enabled=!syncing && tildaUrl.isNotBlank(),modifier=Modifier.fillMaxWidth().height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){if(syncing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Color.Black)else Icon(Icons.Outlined.Sync,null,tint=Color.Black);Spacer(Modifier.width(7.dp));Text(if(syncing)"Синхронизация…" else "Синхронизировать каталог",color=Color.Black,fontWeight=FontWeight.Bold)}
            Text(syncStatus,color=if(syncStatus.startsWith("Ошибка"))ProtoRed else ProtoGreen,fontSize=11.sp,modifier=Modifier.padding(top=7.dp))
        }}
    }
}
@Composable
private fun ProtoAttentionScreen(regs:List<ProtoRegistration>,blocked:List<ProtoClient>,picking:List<ProtoOrder>,onBack:()->Unit,onClient:(ProtoClient)->Unit,onOrder:(ProtoOrder)->Unit){ProtoScaffold("Требует внимания","Задачи остаются до обработки",onBack){item{Text("Новые регистрации",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(regs){r->ProtoSectionCard{Text(r.organization,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${r.type} · ${r.contact1} · ${r.phone1}",color=ProtoMuted,fontSize=12.sp);Button(onClick={},modifier=Modifier.padding(top=6.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Открыть / закрыть",color=Color.Black)}}};item{Text("Приостановленные клиенты",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(blocked,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onClient(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Нажмите для разблокировки",color=ProtoMuted,fontSize=12.sp)}};item{Text("Заказы на сборке",color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold)};items(picking,key={it.id}){o->ProtoOrderRow(o){onOrder(o)}}}}

@Composable
private fun ProtoOnlineControllerScreen(clients:List<ProtoClient>,onBack:()->Unit,onClient:(ProtoClient)->Unit){ProtoScaffold("Online controller","Активность приложений",onBack){item{ProtoSectionCard{ProtoInfoRow("Клиенты онлайн","${clients.count{it.online}}");ProtoInfoRow("Администраторы онлайн","1");ProtoInfoRow("Производство онлайн","2");ProtoInfoRow("Синхронизация","автоматическая")}};item{Text("Клиенты сегодня",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold)};items(clients.sortedByDescending{it.online},key={it.id}){c->ProtoSectionCard(Modifier.clickable{onClient(c)}){Row{Box(Modifier.size(9.dp).background(if(c.online)ProtoGreen else ProtoMuted,CircleShape).align(Alignment.CenterVertically));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(c.name,color=ProtoText);Text("Последняя активность: ${c.lastSeen}",color=ProtoMuted,fontSize=11.sp)};Text("${c.orderCount} заказов",color=ProtoGoldSoft,fontSize=11.sp)}}}}}

@Composable
private fun ProtoProductionHomeScreen(
    drafts:List<ProtoProductionDraft>,opsToday:List<ProtoProductionOp>,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,
    onAdd:()->Unit,onPostAll:()->Unit,onHistory:()->Unit,onStock:()->Unit
) {
    val produced=opsToday.sumOf{it.qty}
    val physical=products.sumOf{stockOverrides[it.sku]?:it.stock}
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_production),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Replace only the static date text, keeping its original location.
            Box(Modifier.offset(maxWidth*.05f,maxHeight*.155f).size(maxWidth*.55f,maxHeight*.035f).background(ProtoBg))
            Text(currentDateLong(),color=ProtoGoldSoft,fontSize=13.sp,modifier=Modifier.offset(maxWidth*.055f,maxHeight*.158f))

            // Same release window and dimensions, now actually scrollable.
            Box(Modifier.offset(maxWidth*.055f,maxHeight*.285f).size(maxWidth*.89f,maxHeight*.245f).background(ProtoPanel.copy(alpha=.98f),RoundedCornerShape(14.dp)).border(1.dp,ProtoBorder,RoundedCornerShape(14.dp))) {
                if(drafts.isEmpty()) {
                    Column(Modifier.fillMaxSize().clickable{onAdd()}.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Outlined.AddCircleOutline,null,tint=ProtoGold,modifier=Modifier.size(34.dp));Text("Добавить позицию выпуска",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Text("Венки · круглые венки · корзины · флоретки · полянки",color=ProtoMuted,fontSize=9.sp)}
                } else {
                    LazyColumn(contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        items(drafts) { d -> Row(Modifier.fillMaxWidth().padding(5.dp),verticalAlignment=Alignment.CenterVertically){ProtoProductImage(d.product,Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(d.product.name,color=ProtoText,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${d.product.sku} · ${d.assembler}",color=ProtoMuted,fontSize=9.sp)};Text("${d.qty} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold,fontSize=11.sp)} }
                        item{OutlinedButton(onClick=onAdd,modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,ProtoGold)){Text("+ Добавить позицию",color=ProtoGold,fontSize=11.sp)}}
                    }
                }
            }

            Box(Modifier.offset(maxWidth*.08f,maxHeight*.55f).size(maxWidth*.84f,maxHeight*.085f).background(ProtoBg.copy(alpha=.92f),RoundedCornerShape(12.dp))) {
                Column(Modifier.padding(10.dp)){Text("Итого выпуска сегодня: ${produced + drafts.sumOf{it.qty}} шт.",color=ProtoText,fontWeight=FontWeight.Bold,fontSize=12.sp);Text("Проведено: $produced · подготовлено: ${drafts.sumOf{it.qty}} · всего на складе: $physical",color=ProtoMuted,fontSize=9.sp)}
            }
            PrototypeClickArea(maxWidth,maxHeight,.08f,.645f,.84f,.065f){onPostAll()}
            PrototypeClickArea(maxWidth,maxHeight,.08f,.72f,.84f,.06f){onHistory()}
            PrototypeClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){}
            PrototypeClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){onAdd()}
            PrototypeClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){onHistory()}
            PrototypeClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){onStock()}
        }
    }
}
@Composable
private fun ProtoProductionCategoryScreen(onBack:()->Unit,onCategory:(String)->Unit){ProtoScaffold("Выпуск продукции","Что произведено в цеху?",onBack){item{listOf("Венки","Венки круглые","Корзины","Флоретки","Полянки").forEach{cat->ProtoSectionCard(Modifier.padding(vertical=4.dp).clickable{onCategory(cat)}){Row(verticalAlignment=Alignment.CenterVertically){Image(painterResource(protoPlaceholderForType(cat)),null,Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop);Spacer(Modifier.width(12.dp));Text(cat,color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}}

@Composable
private fun ProtoProductionCatalogScreen(products:List<ProtoCatalogProduct>,onBack:()->Unit,onSelect:(ProtoCatalogProduct)->Unit){var query by remember{mutableStateOf("")};val filtered=products.filter{query.isBlank()||it.sku.contains(query,true)||it.name.contains(query,true)};ProtoScaffold("Каталог производства","Выберите модель — артикул подставится автоматически",onBack){item{ProtoField(query,{query=it},"Поиск по артикулу или названию")};items(filtered,key={it.sku}){p->ProtoSectionCard(Modifier.clickable{onSelect(p)}){Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Арт. ${p.sku} · ${p.quality} · ${p.size}",color=ProtoMuted,fontSize=11.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}

@Composable
private fun ProtoProductionEntryScreen(product:ProtoCatalogProduct?,qty:Int,assembler:String,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAssembler:(String)->Unit,onAddDraft:()->Unit,onPostNow:()->Unit){val p=product?:return;ProtoScaffold("Приход продукции","Выбранная позиция каталога",onBack){item{ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.width(12.dp));Column{Text(p.name,color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Арт. ${p.sku}",color=ProtoGoldSoft);Text("${p.quality} · ${p.size}",color=ProtoMuted)}}}};item{Text("Сборщица",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Анна К.","Мария С.","Елена П.").forEach{name->FilterChip(selected=assembler==name,onClick={onAssembler(name)},label={Text(name)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))}}};item{Text("Количество",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){OutlinedButton(onClick=onMinus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(qty.toString(),color=ProtoText,fontSize=34.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=28.dp));OutlinedButton(onClick=onPlus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}}};item{Button(onClick=onAddDraft,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Добавить в выпуск дня",color=Color.Black,fontWeight=FontWeight.Bold)};OutlinedButton(onClick=onPostNow,modifier=Modifier.fillMaxWidth().padding(top=8.dp),border=BorderStroke(1.dp,ProtoGold)){Text("Оприходовать на склад сразу",color=ProtoGold,fontWeight=FontWeight.Bold)}}}}

@Composable
private fun ProtoProductionHistoryScreen(ops:List<ProtoProductionOp>,onBack:()->Unit){val grouped=ops.groupBy{it.date};ProtoScaffold("История приходов","По датам, артикулам и сборщицам",onBack){grouped.forEach{(date,dayOps)->item(key="h$date"){Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){Text(date,color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("${dayOps.sumOf{it.qty}} шт.",color=ProtoMuted)}};items(dayOps,key={it.date+it.time+it.sku+it.assembler}){op->ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){Image(painterResource(R.drawable.mock_wreath),null,Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(op.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${op.sku} · ${op.time}",color=ProtoMuted,fontSize=11.sp);Text("Сборщица: ${op.assembler}",color=ProtoMuted,fontSize=11.sp)};Text("${op.qty} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}}}}

@Composable
private fun ProtoServerScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,productionOps:List<ProtoProductionOp>,orders:List<ProtoOrder>,clients:List<ProtoClient>,onBack:()->Unit,onStock:()->Unit,onReserve:()->Unit,onNewClients:()->Unit,onOnline:()->Unit,onExport:()->Unit){
    Box(Modifier.fillMaxSize().background(ProtoBg)) {
        Image(painterResource(R.drawable.screen_server),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.offset(maxWidth*.55f,maxHeight*.14f).size(maxWidth*.40f,maxHeight*.04f).background(ProtoBg.copy(alpha=.88f)))
            Text(currentMonthLabel(),color=ProtoGoldSoft,fontSize=11.sp,modifier=Modifier.offset(maxWidth*.61f,maxHeight*.148f))
            PrototypeClickArea(maxWidth,maxHeight,.05f,.23f,.43f,.12f){}
            PrototypeClickArea(maxWidth,maxHeight,.52f,.23f,.43f,.12f){onReserve()}
            PrototypeClickArea(maxWidth,maxHeight,.05f,.36f,.43f,.12f){onStock()}
            PrototypeClickArea(maxWidth,maxHeight,.52f,.36f,.43f,.12f){onNewClients()}
            PrototypeClickArea(maxWidth,maxHeight,.06f,.56f,.88f,.17f){onOnline()}
            PrototypeClickArea(maxWidth,maxHeight,.08f,.80f,.84f,.06f){onExport()}
        }
        ProtoBackButton(onBack)
    }
}
@Composable
private fun ProtoStockListScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit){ProtoScaffold("Остатки на складе","Физический и доступный остаток",onBack){items(products,key={it.sku}){p->val stock=stockOverrides[p.sku]?:p.stock;ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(p.sku,color=ProtoMuted,fontSize=11.sp)};Text("$stock шт.",color=if(stock>0)ProtoGreen else ProtoRed,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoReserveListScreen(orders:List<ProtoOrder>,onBack:()->Unit){ProtoScaffold("Резерв","За кем закреплены товары",onBack){items(orders.filter{it.status!="Доставлен"},key={it.id}){o->ProtoSectionCard{Text(o.clientName,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${o.id} · ${o.pieces} шт. · ${protoMoney(o.total)}",color=ProtoMuted,fontSize=11.sp);o.lines.forEach{l->Text("${l.sku} — ${l.qty} шт.",color=ProtoGoldSoft,fontSize=11.sp)}}}}}

@Composable
private fun ProtoNewClientsScreen(clients:List<ProtoClient>,onBack:()->Unit,onOpen:(ProtoClient)->Unit){ProtoScaffold("Новые клиенты",currentMonthLabel(),onBack){items(clients,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onOpen(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.orderCount} заказов · оборот ${protoMoney(c.monthTurnover)}",color=ProtoMuted,fontSize=11.sp)}}}}

@Composable
private fun ProtoExportScreen(onBack:()->Unit,onExport:()->Unit){var report by remember{mutableStateOf("Остатки на складе")};ProtoScaffold("Экспорт","Сначала выберите параметр отчёта",onBack){item{ProtoSectionCard{listOf("Остатки на складе","Резерв по клиентам","Выпуск за месяц","Производство по сборщицам","Заказы за месяц").forEach{r->Row(Modifier.fillMaxWidth().clickable{report=r}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=report==r,onClick={report=r},colors=RadioButtonDefaults.colors(selectedColor=ProtoGold));Text(r,color=ProtoText)}};Button(onClick=onExport,modifier=Modifier.fillMaxWidth().padding(top=8.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Экспортировать",color=Color.Black,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoSimpleMessageScreen(title:String,text:String,onBack:()->Unit){
    if(title=="Заявка отправлена"){
        Box(Modifier.fillMaxSize().background(ProtoBg)){
            Image(painterResource(R.drawable.screen_reg_sent),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Box(Modifier.fillMaxWidth().height(28.dp).background(ProtoBg).align(Alignment.TopCenter))
            BoxWithConstraints(Modifier.fillMaxSize()){PrototypeClickArea(maxWidth,maxHeight,.08f,.78f,.84f,.075f){onBack()}}
        }
    } else Box(Modifier.fillMaxSize().background(ProtoBg).padding(24.dp),contentAlignment=Alignment.Center){ProtoSectionCard{Icon(Icons.Outlined.CheckCircle,null,tint=ProtoGreen,modifier=Modifier.size(54.dp).align(Alignment.CenterHorizontally));Text(title,color=ProtoText,fontSize=27.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=12.dp));Text(text,color=ProtoMuted,modifier=Modifier.padding(vertical=14.dp));Button(onClick=onBack,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Готово",color=Color.Black,fontWeight=FontWeight.Bold)}}}
}
@Composable
private fun ProtoScaffold(title:String,subtitle:String?=null,onBack:(()->Unit)?=null,content:LazyListScope.()->Unit){Column(Modifier.fillMaxSize().background(ProtoBg)){Row(Modifier.padding(16.dp,22.dp,16.dp,8.dp),verticalAlignment=Alignment.CenterVertically){if(onBack!=null){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp))};Column{Text(title,color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold);if(subtitle!=null)Text(subtitle,color=ProtoMuted,fontSize=12.sp)}};LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}

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
private fun ProtoSectionCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),content=content)}}

@Composable
private fun ProtoField(value:String,onChange:(String)->Unit,label:String,keyboardType:KeyboardType=KeyboardType.Text){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=keyboardType),colors=protoFieldColors())}

@Composable
private fun protoFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=ProtoGold,unfocusedBorderColor=ProtoBorder,focusedTextColor=ProtoText,unfocusedTextColor=ProtoText,focusedLabelColor=ProtoGold,unfocusedLabelColor=ProtoMuted,focusedLeadingIconColor=ProtoGold,unfocusedLeadingIconColor=ProtoMuted,disabledBorderColor=ProtoBorder,disabledTextColor=ProtoMuted)

@Composable
private fun ProtoCompactGrid(title:String,options:List<String>,selected:Set<String>,toggle:(String)->Unit,disabled:Set<String> = emptySet()){Text(title,color=ProtoGoldSoft,fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=7.dp,bottom=3.dp));options.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{o->val off=o in disabled;FilterChip(selected=o in selected,onClick={if(!off)toggle(o)},enabled=!off,label={Text(if(off)"$o · скоро" else o,fontSize=11.sp,maxLines=1)},modifier=Modifier.weight(1f).height(34.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText,disabledLabelColor=ProtoMuted))};if(row.size==1)Spacer(Modifier.weight(1f))}}
}

@Composable
private fun ProtoCategoryButton(label:String,disabled:Boolean,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,enabled=true,modifier=modifier.height(46.dp),border=BorderStroke(1.dp,if(disabled)ProtoBorder else ProtoGold),contentPadding=PaddingValues(horizontal=5.dp)){Text(if(disabled)"$label · скоро" else label,color=if(disabled)ProtoMuted else ProtoGold,fontSize=11.sp,maxLines=1)}}

@Composable
private fun ProtoPopularRow(p:ProtoCatalogProduct,stock:Int,onOpen:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=10.sp);Text(if(stock>0)"В наличии $stock шт." else "Под заказ · от ${p.productionDays} дней",color=if(stock>0)ProtoGreen else ProtoGoldSoft,fontSize=10.sp);Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)};Button(onClick=onOpen,colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.width(92.dp)){Text("В корзину",color=Color.Black,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)}}}}

@Composable
private fun ProtoCartRow(p:ProtoCatalogProduct,qty:Int,onMinus:()->Unit,onPlus:()->Unit,onDelete:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){ProtoProductImage(p,Modifier.size(64.dp).clip(RoundedCornerShape(9.dp)));Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(p.sku,color=ProtoMuted,fontSize=10.sp);Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onMinus,modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(qty.toString(),color=ProtoText);IconButton(onClick=onPlus,modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}}};Column(horizontalAlignment=Alignment.End){Text(protoMoney(p.price*qty),color=ProtoGoldSoft,fontWeight=FontWeight.Bold);IconButton(onClick=onDelete){Icon(Icons.Outlined.Delete,null,tint=ProtoMuted)}}}}}

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
private fun ProtoClientBottomBar(current:ProtoScreen,cartCount:Int,onHome:()->Unit,onCatalog:()->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit){NavigationBar(containerColor=ProtoPanel,tonalElevation=0.dp){ProtoNavItem(current==ProtoScreen.Home,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(false,"Каталог",Icons.Outlined.Inventory2,onCatalog);NavigationBarItem(selected=false,onClick=onCart,icon={BadgedBox(badge={if(cartCount>0)Badge(containerColor=ProtoGold){Text(cartCount.toString(),color=Color.Black)}}){Icon(Icons.Outlined.ShoppingCart,null)}},label={Text("Корзина",fontSize=9.sp)},colors=protoNavColors());ProtoNavItem(false,"Заказы",Icons.Outlined.ReceiptLong,onOrders);ProtoNavItem(false,"Профиль",Icons.Outlined.Person,onProfile)}}

@Composable
private fun ProtoAdminBottomBar(current:ProtoScreen,onHome:()->Unit,onClients:()->Unit,onOrders:()->Unit,onStock:()->Unit,onProfile:()->Unit){NavigationBar(containerColor=ProtoPanel,tonalElevation=0.dp){ProtoNavItem(current==ProtoScreen.AdminHome,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(false,"Клиенты",Icons.Outlined.Groups,onClients);ProtoNavItem(false,"Заказы",Icons.Outlined.ReceiptLong,onOrders);ProtoNavItem(false,"Склад",Icons.Outlined.Inventory2,onStock);ProtoNavItem(false,"Профиль",Icons.Outlined.Person,onProfile)}}

@Composable
private fun ProtoProductionBottomBar(onHome:()->Unit,onProduction:()->Unit,onHistory:()->Unit,onStock:()->Unit,onProfile:()->Unit){NavigationBar(containerColor=ProtoPanel,tonalElevation=0.dp){ProtoNavItem(false,"Главная",Icons.Outlined.Home,onHome);ProtoNavItem(true,"Производство",Icons.Outlined.LocalFlorist,onProduction);ProtoNavItem(false,"История",Icons.Outlined.History,onHistory);ProtoNavItem(false,"Склад",Icons.Outlined.Inventory2,onStock);ProtoNavItem(false,"Профиль",Icons.Outlined.Person,onProfile)}}

@Composable
private fun RowScope.ProtoNavItem(selected:Boolean,label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){NavigationBarItem(selected=selected,onClick=onClick,icon={Icon(icon,null)},label={Text(label,fontSize=9.sp)},colors=protoNavColors())}

@Composable
private fun protoNavColors()=NavigationBarItemDefaults.colors(selectedIconColor=Color.Black,selectedTextColor=ProtoGold,indicatorColor=ProtoGold,unselectedIconColor=ProtoMuted,unselectedTextColor=ProtoMuted)

@Composable
private fun ProtoProductImage(p:ProtoCatalogProduct,modifier:Modifier=Modifier){
    val placeholder=painterResource(protoPlaceholderForType(p.type))
    AsyncImage(model=p.imageUrl.takeIf{it.isNotBlank()}?:protoPlaceholderForType(p.type),contentDescription=p.name,modifier=modifier,contentScale=ContentScale.Crop,placeholder=placeholder,error=placeholder,fallback=placeholder)
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

private fun protoDial(context:Context){context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${BuildConfig.ADMIN_PHONE}")))}
private fun protoMessage(context:Context){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${BuildConfig.ADMIN_PHONE}")))}
