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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val productionDays: Int
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
            ProtoScreen.AdminSettings -> ProtoAdminSettingsScreen(lowStockThreshold, notificationsRegistration, notificationsOrders, notificationsProduction, notificationsLowStock, onBack = { back() }, onThreshold = { lowStockThreshold = it.coerceIn(1,50) }, onReg = { notificationsRegistration = it }, onOrders = { notificationsOrders = it }, onProd = { notificationsProduction = it }, onLow = { notificationsLowStock = it })
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
    var organization by remember { mutableStateOf("") }; var fio by remember { mutableStateOf("") }; var inn by remember { mutableStateOf("") }
    var contact1 by remember { mutableStateOf("") }; var phone1 by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; var type by remember { mutableStateOf(ProtoClientType.TRADING.label) }
    var consent by remember { mutableStateOf(false) }; var showSecond by remember { mutableStateOf(false) }; var contact2 by remember { mutableStateOf("") }; var phone2 by remember { mutableStateOf("") }
    val valid = organization.isNotBlank() && fio.isNotBlank() && contact1.isNotBlank() && phone1.isNotBlank() && email.isNotBlank() && city.isNotBlank() && consent
    ProtoScaffold(title="Регистрация партнёра",subtitle="Заполните данные организации и контактных лиц",onBack=onBack) {
        item {
            ProtoSectionCard {
                Text("Тип партнёра",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(ProtoClientType.AGENT.label,ProtoClientType.TRADING.label).forEach { t -> FilterChip(selected=type==t,onClick={type=t},label={Text(t,maxLines=1)},modifier=Modifier.weight(1f),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText)) }
                }
                Spacer(Modifier.height(8.dp)); ProtoField(organization,{organization=it},"Название организации")
                ProtoField(fio,{fio=it},"Фамилия, имя, отчество")
                ProtoField(inn,{inn=it},"ИНН",keyboardType=KeyboardType.Number)
                ProtoField(contact1,{contact1=it},"Контактное лицо")
                ProtoField(phone1,{phone1=it},"Телефон",keyboardType=KeyboardType.Phone)
                ProtoField(email,{email=it},"E-mail",keyboardType=KeyboardType.Email)
                ProtoField(city,{city=it},"Город")
                ProtoField(address,{address=it},"Адрес доставки")
                TextButton(onClick={showSecond=!showSecond},contentPadding=PaddingValues(0.dp)){Icon(if(showSecond) Icons.Outlined.Remove else Icons.Outlined.Add,null,tint=ProtoGold);Spacer(Modifier.width(6.dp));Text(if(showSecond)"Убрать второе контактное лицо" else "Добавить контактное лицо",color=ProtoGold)}
                if(showSecond){ ProtoField(contact2,{contact2=it},"Второе контактное лицо"); ProtoField(phone2,{phone2=it},"Телефон второго контакта",keyboardType=KeyboardType.Phone) }
                Row(Modifier.fillMaxWidth().clickable{consent=!consent}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(consent,{consent=it},colors=CheckboxDefaults.colors(checkedColor=ProtoGold,checkmarkColor=Color.Black));Text("Согласен на обработку персональных данных",color=ProtoText,fontSize=13.sp)}
                Button(onClick={onSubmit(ProtoRegistration(organization,fio,inn,contact1,phone1,email,city,address,type,contact2,phone2))},enabled=valid,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Отправить заявку",color=Color.Black,fontWeight=FontWeight.Bold)}
                Text("Тестовая версия: заявка сразу появляется у администратора. Telegram-передача подключается при добавлении токена бота.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
            }
        }
    }
}

@Composable
private fun ProtoClientHomeScreen(client:ProtoClient,products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,cartCount:Int,query:String,onQuery:(String)->Unit,onSearch:()->Unit,onAvailability:(String)->Unit,onCategory:(String)->Unit,onOpenProduct:(ProtoCatalogProduct)->Unit,onCart:()->Unit,onOrders:()->Unit,onProfile:()->Unit,onCatalog:()->Unit) {
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item { Text("SANSARA",color=ProtoGoldSoft,letterSpacing=4.sp,fontSize=20.sp,modifier=Modifier.clickable{ }); Text("Здравствуйте, ${client.contact.substringBefore(' ')}",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoPill(client.status,ProtoGreen);ProtoPill("Скидка ${client.discount}%",ProtoGold)} }
            item { OutlinedTextField(value=query,onValueChange=onQuery,modifier=Modifier.fillMaxWidth(),placeholder={Text("Поиск по артикулу или названию")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={IconButton(onClick=onSearch){Icon(Icons.Outlined.ArrowForward,null)}},singleLine=true,colors=protoFieldColors()) }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoActionChip("В наличии",Icons.Outlined.Inventory2,Modifier.weight(1f)){onAvailability("В наличии")};ProtoActionChip("Под заказ",Icons.Outlined.Schedule,Modifier.weight(1f)){onAvailability("Под заказ")}} }
            item { Text("Каталог",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(6.dp)); val cats=listOf("Венки","Венки круглые","Корзины","Полянки","Флоретки","Ленты","Гробы","Кресты"); cats.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{cat->ProtoCategoryButton(cat,cat in setOf("Гробы","Кресты"),Modifier.weight(1f)){onCategory(cat)}};if(row.size==1)Spacer(Modifier.weight(1f))};Spacer(Modifier.height(7.dp))} }
            item { Row(verticalAlignment=Alignment.CenterVertically){Text("Популярные товары",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));TextButton(onClick=onCatalog){Text("Все товары",color=ProtoGold)}} }
            items(products.take(10),key={it.sku}) { p -> ProtoPopularRow(p,stockOverrides[p.sku]?:p.stock){onOpenProduct(p)} }
        }
        ProtoClientBottomBar(ProtoScreen.Home,cartCount,onHome={},onCatalog=onCatalog,onCart=onCart,onOrders=onOrders,onProfile=onProfile)
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
        Box(Modifier.fillMaxWidth().height(118.dp).background(ProtoPanel2).clickable(onClick=onOpen),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(54.dp));Surface(Modifier.align(Alignment.BottomStart).padding(7.dp),color=if(currentStock>0)Color(0xDD123A27)else Color(0xDD4A2220),shape=RoundedCornerShape(18.dp)){Text(if(currentStock>0)"В наличии $currentStock" else "Под заказ · ${p.productionDays} дн.",color=if(currentStock>0)ProtoGreen else ProtoGoldSoft,fontSize=9.sp,modifier=Modifier.padding(horizontal=7.dp,vertical=3.dp))}}
        Column(Modifier.padding(9.dp)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=10.sp);Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.padding(vertical=4.dp));Button(onClick=onOpen,modifier=Modifier.fillMaxWidth().height(38.dp),contentPadding=PaddingValues(horizontal=4.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),shape=RoundedCornerShape(9.dp)){Icon(Icons.Outlined.AddShoppingCart,null,tint=Color.Black,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("В корзину",color=Color.Black,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)}}
    }
}

@Composable
private fun ProtoProductDetailScreen(product:ProtoCatalogProduct?,currentStock:Int,qty:Int,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAdd:()->Unit) {
    val p=product?:return
    Column(Modifier.fillMaxSize().background(ProtoBg)){Box(Modifier.fillMaxWidth().height(300.dp).background(ProtoPanel2)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(110.dp))};Box(Modifier.padding(14.dp,22.dp)){ProtoCircleBack(onBack)}};Column(Modifier.padding(18.dp)){Text(p.name,color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold);Text("Арт. ${p.sku}",color=ProtoMuted);Spacer(Modifier.height(8.dp));Row{Text(protoMoney(p.price),color=ProtoGoldSoft,fontSize=27.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(if(currentStock>0)"● В наличии $currentStock шт." else "● Под заказ · от ${p.productionDays} дней",color=if(currentStock>0)ProtoGreen else ProtoGoldSoft,fontSize=12.sp)};Spacer(Modifier.height(12.dp));ProtoSectionCard{ProtoInfoRow("Тип",p.type);ProtoInfoRow("Качество",p.quality);ProtoInfoRow("Размер",p.size)};Spacer(Modifier.height(14.dp));Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick=onMinus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(qty.toString(),color=ProtoText,fontSize=23.sp,modifier=Modifier.padding(horizontal=18.dp));OutlinedButton(onClick=onPlus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)};Spacer(Modifier.width(10.dp));Button(onClick=onAdd,modifier=Modifier.weight(1f).height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("В корзину",color=Color.Black,fontWeight=FontWeight.Bold,maxLines=1)}};Text("После добавления вы вернётесь к предыдущему экрану и сможете продолжить набор заказа.",color=ProtoMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))}}
}

@Composable
private fun ProtoCartScreen(products:List<ProtoCatalogProduct>,cart:SnapshotStateMap<String,Int>,onBack:()->Unit,onPlus:(ProtoCatalogProduct)->Unit,onMinus:(ProtoCatalogProduct)->Unit,onDelete:(ProtoCatalogProduct)->Unit,onCheckout:()->Unit) {
    val lines=cart.mapNotNull{(sku,q)->products.firstOrNull{it.sku==sku}?.let{it to q}};val total=lines.sumOf{it.first.price*it.second}
    Column(Modifier.fillMaxSize().background(ProtoBg)){Row(Modifier.padding(16.dp,22.dp,16.dp,8.dp),verticalAlignment=Alignment.CenterVertically){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp));Column{Text("Корзина",color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold);Text("${lines.sumOf{it.second}} шт. · ${protoMoney(total)}",color=ProtoMuted)}};if(lines.isEmpty())Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Корзина пока пуста",color=ProtoMuted)}else{LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(lines,key={it.first.sku}){(p,q)->ProtoCartRow(p,q,{onMinus(p)},{onPlus(p)},{onDelete(p)})}};ProtoSectionCard(Modifier.padding(horizontal=14.dp)){Row{Text("Итого",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(protoMoney(total),color=ProtoGoldSoft,fontSize=23.sp,fontWeight=FontWeight.Bold)}};Button(onClick=onCheckout,modifier=Modifier.padding(14.dp).fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Оформить заказ",color=Color.Black,fontWeight=FontWeight.Bold)} }}
}

@Composable
private fun ProtoCheckoutScreen(cartCount:Int,total:Int,onBack:()->Unit,onSubmit:()->Unit){var method by remember{mutableStateOf("Доставка")};var address by remember{mutableStateOf("")};var comment by remember{mutableStateOf("")};ProtoScaffold("Оформление заказа","$cartCount шт. · ${protoMoney(total)}",onBack){item{ProtoSectionCard{Text("Способ получения",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Доставка","Самовывоз","ТК").forEach{m->FilterChip(selected=method==m,onClick={method=m},label={Text(m)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))}};ProtoField(address,{address=it},"Адрес / ТК");ProtoField(comment,{comment=it},"Комментарий");Spacer(Modifier.height(8.dp));Button(onClick=onSubmit,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Отправить заказ",color=Color.Black,fontWeight=FontWeight.Bold)}}}}
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
    val active=clients.count{it.status!="Приостановлен"}
    val todayOrders=orders.count{it.dateTime.startsWith(currentDateShort())}
    val prodToday=productionOps.filter{it.date==currentDateShort()}.sumOf{it.qty}
    val stock=products.sumOf{stockOverrides[it.sku]?:it.stock}
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp,20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("SANSARA",color=ProtoGoldSoft,letterSpacing=4.sp,fontSize=18.sp)
                Text("Здравствуйте, Игорь",color=ProtoText,fontSize=29.sp,fontWeight=FontWeight.Bold)
                Text("Администратор · ${currentMonthLabel()}",color=ProtoMuted)
            }
            item {
                OutlinedButton(onClick=onSearch,modifier=Modifier.fillMaxWidth().height(48.dp),border=BorderStroke(1.dp,ProtoBorder)) {
                    Icon(Icons.Outlined.Search,null,tint=ProtoGold); Spacer(Modifier.width(8.dp)); Text("Поиск по клиентам, заказам, товарам",color=ProtoMuted)
                }
            }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("Всего заявок","18","за месяц",Modifier.weight(1f),onRegistrations)
                ProtoMetricCard("Активные клиенты",active.toString(),"в базе",Modifier.weight(1f),onClients)
            }}
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("Заказы сегодня",todayOrders.toString(),"открыть",Modifier.weight(1f),onOrders)
                ProtoMetricCard("Производство сегодня",prodToday.toString(),"шт.",Modifier.weight(1f),onProduction)
            }}
            item { ProtoMetricCard("Остаток на складе",stock.toString(),"шт. физический остаток",Modifier.fillMaxWidth(),onStock) }
            item {
                Text("Быстрые действия",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    ProtoQuickButton("Клиенты",Icons.Outlined.Groups,Modifier.weight(1f),onClients)
                    ProtoQuickButton("Заказы",Icons.Outlined.ReceiptLong,Modifier.weight(1f),onOrders)
                    ProtoQuickButton("Каталог",Icons.Outlined.Inventory2,Modifier.weight(1f),onCatalog)
                    ProtoQuickButton("Настройки",Icons.Outlined.Settings,Modifier.weight(1f),onSettings)
                }
            }
            item {
                Text("Требует внимания",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)
                ProtoAttentionLine("5 новых регистраций","Открыть и обработать",ProtoOrange,onAttention)
                ProtoAttentionLine("3 клиента приостановлены","Проверить оплату / разблокировать",ProtoRed,onAttention)
                ProtoAttentionLine("7 заказов на сборке","Контроль статусов",ProtoGold,onOrders)
            }
            item {
                Text("Online controller",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)
                ProtoSectionCard(Modifier.clickable(onClick=onOnline)) {
                    ProtoInfoRow("Клиентские приложения","${clients.count{it.online}} онлайн")
                    ProtoInfoRow("Админка","1 онлайн")
                    ProtoInfoRow("Производство","2 онлайн")
                    Text("Автосинхронизация · тестовый режим",color=ProtoGreen,fontSize=12.sp)
                }
            }
        }
        ProtoAdminBottomBar(ProtoScreen.AdminHome,onHome={},onClients=onClients,onOrders=onOrders,onStock=onStock,onProfile=onSettings)
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
private fun ProtoAdminCatalogScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit){ProtoScaffold("Каталог","Все тестовые позиции",onBack){items(products,key={it.sku}){p->ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(34.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${p.sku} · ${p.type} · ${p.size}",color=ProtoMuted,fontSize=11.sp)};Text("${stockOverrides[p.sku]?:p.stock} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoAdminSettingsScreen(threshold:Int,reg:Boolean,orders:Boolean,prod:Boolean,low:Boolean,onBack:()->Unit,onThreshold:(Int)->Unit,onReg:(Boolean)->Unit,onOrders:(Boolean)->Unit,onProd:(Boolean)->Unit,onLow:(Boolean)->Unit){ProtoScaffold("Настройки","Управление бизнес-правилами тестовой версии",onBack){item{ProtoSectionCard{Text("Уведомления",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);ProtoSwitchRow("Новые регистрации",reg,onReg);ProtoSwitchRow("Новые заказы",orders,onOrders);ProtoSwitchRow("Производство",prod,onProd);ProtoSwitchRow("Низкие остатки",low,onLow)}};item{ProtoSectionCard{Text("Порог низкого остатка",color=ProtoText,fontSize=19.sp,fontWeight=FontWeight.Bold);Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={onThreshold(threshold-1)}){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text("$threshold шт.",color=ProtoGoldSoft,fontSize=22.sp,fontWeight=FontWeight.Bold);IconButton(onClick={onThreshold(threshold+1)}){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}};Text("Позиции с доступным остатком ≤ порога будут попадать в «Требует внимания».",color=ProtoMuted,fontSize=11.sp);ProtoInfoRow("Телефон администратора","+7 926 304-60-19")}}}}

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
    Column(Modifier.fillMaxSize().background(ProtoBg)) {
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp,20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("Производство",color=ProtoText,fontSize=30.sp,fontWeight=FontWeight.Bold)
                Text(currentDateLong(),color=ProtoGoldSoft)
                Text("Данные видит клиент и администратор после проведения",color=ProtoMuted,fontSize=11.sp)
            }
            item {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Выпуск продукции сегодня",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick=onAdd){Icon(Icons.Outlined.Add,null,tint=ProtoGold);Text("Добавить",color=ProtoGold)}
                }
                Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),modifier=Modifier.fillMaxWidth().heightIn(min=150.dp,max=270.dp)) {
                    if(drafts.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(18.dp),contentAlignment=Alignment.Center){Text("Добавьте позиции сегодняшнего выпуска",color=ProtoMuted)}
                    } else {
                        LazyColumn(Modifier.padding(8.dp)) {
                            items(drafts) { d ->
                                Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Icon(protoIconForType(d.product.type),null,tint=ProtoGold)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(d.product.name,color=ProtoText,maxLines=1,overflow=TextOverflow.Ellipsis)
                                        Text("${d.product.sku} · ${d.assembler}",color=ProtoMuted,fontSize=10.sp)
                                    }
                                    Text("${d.qty} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            item {
                ProtoSectionCard {
                    ProtoInfoRow("Уже проведено сегодня","$produced шт.")
                    ProtoInfoRow("Подготовлено к приходу","${drafts.sumOf{it.qty}} шт.")
                    ProtoInfoRow("Физический остаток склада","$physical шт.")
                }
            }
            item {
                Button(onClick=onPostAll,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)) {
                    Icon(Icons.Outlined.Inventory2,null,tint=Color.Black);Spacer(Modifier.width(8.dp));Text("Оприходовать выпуск",color=Color.Black,fontWeight=FontWeight.Bold)
                }
                OutlinedButton(onClick=onHistory,modifier=Modifier.fillMaxWidth().padding(top=8.dp),border=BorderStroke(1.dp,ProtoGold)) {
                    Icon(Icons.Outlined.History,null,tint=ProtoGold);Spacer(Modifier.width(8.dp));Text("История приходов",color=ProtoGold)
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        ProtoProductionBottomBar(onHome={},onProduction={},onHistory=onHistory,onStock=onStock,onProfile={})
    }
}

@Composable
private fun ProtoProductionCategoryScreen(onBack:()->Unit,onCategory:(String)->Unit){ProtoScaffold("Выпуск продукции","Что произведено в цеху?",onBack){item{listOf("Венки","Венки круглые","Корзины","Флоретки","Полянки").forEach{cat->ProtoSectionCard(Modifier.padding(vertical=4.dp).clickable{onCategory(cat)}){Row(verticalAlignment=Alignment.CenterVertically){Icon(protoIconForType(cat),null,tint=ProtoGold,modifier=Modifier.size(34.dp));Spacer(Modifier.width(12.dp));Text(cat,color=ProtoText,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}}

@Composable
private fun ProtoProductionCatalogScreen(products:List<ProtoCatalogProduct>,onBack:()->Unit,onSelect:(ProtoCatalogProduct)->Unit){var query by remember{mutableStateOf("")};val filtered=products.filter{query.isBlank()||it.sku.contains(query,true)||it.name.contains(query,true)};ProtoScaffold("Каталог производства","Выберите модель — артикул подставится автоматически",onBack){item{ProtoField(query,{query=it},"Поиск по артикулу или названию")};items(filtered,key={it.sku}){p->ProtoSectionCard(Modifier.clickable{onSelect(p)}){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(58.dp).background(ProtoPanel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(34.dp))};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("Арт. ${p.sku} · ${p.quality} · ${p.size}",color=ProtoMuted,fontSize=11.sp)};Icon(Icons.Outlined.ChevronRight,null,tint=ProtoGold)}}}}}

@Composable
private fun ProtoProductionEntryScreen(product:ProtoCatalogProduct?,qty:Int,assembler:String,onBack:()->Unit,onMinus:()->Unit,onPlus:()->Unit,onAssembler:(String)->Unit,onAddDraft:()->Unit,onPostNow:()->Unit){val p=product?:return;ProtoScaffold("Приход продукции","Выбранная позиция каталога",onBack){item{ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(82.dp).background(ProtoPanel2,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(46.dp))};Spacer(Modifier.width(12.dp));Column{Text(p.name,color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Арт. ${p.sku}",color=ProtoGoldSoft);Text("${p.quality} · ${p.size}",color=ProtoMuted)}}}};item{Text("Сборщица",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Анна К.","Мария С.","Елена П.").forEach{name->FilterChip(selected=assembler==name,onClick={onAssembler(name)},label={Text(name)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProtoGold,selectedLabelColor=Color.Black,labelColor=ProtoText))}}};item{Text("Количество",color=ProtoGoldSoft,fontWeight=FontWeight.SemiBold);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){OutlinedButton(onClick=onMinus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(qty.toString(),color=ProtoText,fontSize=34.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=28.dp));OutlinedButton(onClick=onPlus,border=BorderStroke(1.dp,ProtoGold)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}}};item{Button(onClick=onAddDraft,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Добавить в выпуск дня",color=Color.Black,fontWeight=FontWeight.Bold)};OutlinedButton(onClick=onPostNow,modifier=Modifier.fillMaxWidth().padding(top=8.dp),border=BorderStroke(1.dp,ProtoGold)){Text("Оприходовать на склад сразу",color=ProtoGold,fontWeight=FontWeight.Bold)}}}}

@Composable
private fun ProtoProductionHistoryScreen(ops:List<ProtoProductionOp>,onBack:()->Unit){val grouped=ops.groupBy{it.date};ProtoScaffold("История приходов","По датам, артикулам и сборщицам",onBack){grouped.forEach{(date,dayOps)->item(key="h$date"){Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){Text(date,color=ProtoGoldSoft,fontSize=18.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("${dayOps.sumOf{it.qty}} шт.",color=ProtoMuted)}};items(dayOps,key={it.date+it.time+it.sku+it.assembler}){op->ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(58.dp).background(ProtoPanel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.LocalFlorist,null,tint=ProtoGold,modifier=Modifier.size(34.dp))};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(op.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${op.sku} · ${op.time}",color=ProtoMuted,fontSize=11.sp);Text("Сборщица: ${op.assembler}",color=ProtoMuted,fontSize=11.sp)};Text("${op.qty} шт.",color=ProtoGoldSoft,fontWeight=FontWeight.Bold)}}}}}}

@Composable
private fun ProtoServerScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,productionOps:List<ProtoProductionOp>,orders:List<ProtoOrder>,clients:List<ProtoClient>,onBack:()->Unit,onStock:()->Unit,onReserve:()->Unit,onNewClients:()->Unit,onOnline:()->Unit,onExport:()->Unit){val physical=products.sumOf{stockOverrides[it.sku]?:it.stock};val reserve=orders.filter{it.status!="Доставлен"}.sumOf{it.pieces};val available=(physical-reserve).coerceAtLeast(0);val monthProduced=productionOps.sumOf{it.qty};ProtoScaffold("Склад и данные",currentMonthLabel(),onBack){item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Выпущено",monthProduced.toString(),"за месяц",Modifier.weight(1f),{});ProtoMetricCard("Резерв",reserve.toString(),"шт.",Modifier.weight(1f),onReserve)}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ProtoMetricCard("Доступно",available.toString(),"шт. сейчас",Modifier.weight(1f),onStock);ProtoMetricCard("Новые клиенты","${clients.size}","за месяц",Modifier.weight(1f),onNewClients)}};item{Text("Синхронизация",color=ProtoText,fontSize=20.sp,fontWeight=FontWeight.Bold);ProtoSectionCard(Modifier.clickable(onClick=onOnline)){ProtoInfoRow("Клиенты онлайн","${clients.count{it.online}}");ProtoInfoRow("Администратор онлайн","1");ProtoInfoRow("Производство онлайн","2");Text("● Автосинхронизация активна",color=ProtoGreen,fontSize=12.sp)}};item{Button(onClick=onExport,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Icon(Icons.Outlined.Download,null,tint=Color.Black);Spacer(Modifier.width(8.dp));Text("Экспорт текущей выборки",color=Color.Black,fontWeight=FontWeight.Bold)}}}}

@Composable
private fun ProtoStockListScreen(products:List<ProtoCatalogProduct>,stockOverrides:SnapshotStateMap<String,Int>,onBack:()->Unit){ProtoScaffold("Остатки на складе","Физический и доступный остаток",onBack){items(products,key={it.sku}){p->val stock=stockOverrides[p.sku]?:p.stock;ProtoSectionCard{Row(verticalAlignment=Alignment.CenterVertically){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(32.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text(p.sku,color=ProtoMuted,fontSize=11.sp)};Text("$stock шт.",color=if(stock>0)ProtoGreen else ProtoRed,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoReserveListScreen(orders:List<ProtoOrder>,onBack:()->Unit){ProtoScaffold("Резерв","За кем закреплены товары",onBack){items(orders.filter{it.status!="Доставлен"},key={it.id}){o->ProtoSectionCard{Text(o.clientName,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${o.id} · ${o.pieces} шт. · ${protoMoney(o.total)}",color=ProtoMuted,fontSize=11.sp);o.lines.forEach{l->Text("${l.sku} — ${l.qty} шт.",color=ProtoGoldSoft,fontSize=11.sp)}}}}}

@Composable
private fun ProtoNewClientsScreen(clients:List<ProtoClient>,onBack:()->Unit,onOpen:(ProtoClient)->Unit){ProtoScaffold("Новые клиенты",currentMonthLabel(),onBack){items(clients,key={it.id}){c->ProtoSectionCard(Modifier.clickable{onOpen(c)}){Text(c.name,color=ProtoText,fontWeight=FontWeight.SemiBold);Text("${c.orderCount} заказов · оборот ${protoMoney(c.monthTurnover)}",color=ProtoMuted,fontSize=11.sp)}}}}

@Composable
private fun ProtoExportScreen(onBack:()->Unit,onExport:()->Unit){var report by remember{mutableStateOf("Остатки на складе")};ProtoScaffold("Экспорт","Сначала выберите параметр отчёта",onBack){item{ProtoSectionCard{listOf("Остатки на складе","Резерв по клиентам","Выпуск за месяц","Производство по сборщицам","Заказы за месяц").forEach{r->Row(Modifier.fillMaxWidth().clickable{report=r}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=report==r,onClick={report=r},colors=RadioButtonDefaults.colors(selectedColor=ProtoGold));Text(r,color=ProtoText)}};Button(onClick=onExport,modifier=Modifier.fillMaxWidth().padding(top=8.dp),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Экспортировать",color=Color.Black,fontWeight=FontWeight.Bold)}}}}}

@Composable
private fun ProtoSimpleMessageScreen(title:String,text:String,onBack:()->Unit){Box(Modifier.fillMaxSize().background(ProtoBg).padding(24.dp),contentAlignment=Alignment.Center){ProtoSectionCard{Icon(Icons.Outlined.CheckCircle,null,tint=ProtoGreen,modifier=Modifier.size(54.dp).align(Alignment.CenterHorizontally));Text(title,color=ProtoText,fontSize=27.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=12.dp));Text(text,color=ProtoMuted,modifier=Modifier.padding(vertical=14.dp));Button(onClick=onBack,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=ProtoGold)){Text("Готово",color=Color.Black,fontWeight=FontWeight.Bold)}}}}

@Composable
private fun ProtoScaffold(title:String,subtitle:String?=null,onBack:(()->Unit)?=null,content:LazyListScope.()->Unit){Column(Modifier.fillMaxSize().background(ProtoBg)){Row(Modifier.padding(16.dp,22.dp,16.dp,8.dp),verticalAlignment=Alignment.CenterVertically){if(onBack!=null){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp))};Column{Text(title,color=ProtoText,fontSize=28.sp,fontWeight=FontWeight.Bold);if(subtitle!=null)Text(subtitle,color=ProtoMuted,fontSize=12.sp)}};LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}

@Composable
private fun ProtoHeader(title:String,subtitle:String,onBack:()->Unit){Row(verticalAlignment=Alignment.CenterVertically){ProtoCircleBack(onBack);Spacer(Modifier.width(12.dp));Column{Text(title,color=ProtoText,fontSize=27.sp,fontWeight=FontWeight.Bold);Text(subtitle,color=ProtoMuted,fontSize=11.sp)}}}

@Composable
private fun ProtoCircleBack(onClick:()->Unit){Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xCC11100E)).border(1.dp,ProtoGold,CircleShape).clickable(onClick=onClick),contentAlignment=Alignment.Center){Icon(Icons.Outlined.ArrowBack,null,tint=ProtoGold)}}

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
private fun ProtoPopularRow(p:ProtoCatalogProduct,stock:Int,onOpen:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(72.dp).background(ProtoPanel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(38.dp))};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Арт. ${p.sku}",color=ProtoMuted,fontSize=10.sp);Text(if(stock>0)"В наличии $stock шт." else "Под заказ · от ${p.productionDays} дней",color=if(stock>0)ProtoGreen else ProtoGoldSoft,fontSize=10.sp);Text(protoMoney(p.price),color=ProtoGoldSoft,fontWeight=FontWeight.Bold)};Button(onClick=onOpen,colors=ButtonDefaults.buttonColors(containerColor=ProtoGold),contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.width(92.dp)){Text("В корзину",color=Color.Black,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)}}}}

@Composable
private fun ProtoCartRow(p:ProtoCatalogProduct,qty:Int,onMinus:()->Unit,onPlus:()->Unit,onDelete:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=ProtoPanel),border=BorderStroke(1.dp,ProtoBorder),shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(64.dp).background(ProtoPanel2,RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center){Icon(protoIconForType(p.type),null,tint=ProtoGold,modifier=Modifier.size(32.dp))};Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(p.name,color=ProtoText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(p.sku,color=ProtoMuted,fontSize=10.sp);Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onMinus,modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.Remove,null,tint=ProtoGold)};Text(qty.toString(),color=ProtoText);IconButton(onClick=onPlus,modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.Add,null,tint=ProtoGold)}}};Column(horizontalAlignment=Alignment.End){Text(protoMoney(p.price*qty),color=ProtoGoldSoft,fontWeight=FontWeight.Bold);IconButton(onClick=onDelete){Icon(Icons.Outlined.Delete,null,tint=ProtoMuted)}}}}}

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

private fun protoToggle(set:Set<String>,value:String)=if(value in set)set-value else set+value
private fun protoMoney(value:Int)=NumberFormat.getCurrencyInstance(ruLocale).format(value).replace(",00","")
private fun protoIconForType(type:String)=when(type){"Венки","Венки круглые"->Icons.Outlined.LocalFlorist;"Корзины"->Icons.Outlined.ShoppingBasket;"Полянки","Флоретки"->Icons.Outlined.Spa;"Ленты"->Icons.Outlined.ReceiptLong;else->Icons.Outlined.Inventory2}

private fun protoLoadProducts(context:Context):List<ProtoCatalogProduct>{return try{val root=JSONObject(context.assets.open("demo_data.json").bufferedReader().use{it.readText()});val arr=root.getJSONArray("products");(0 until arr.length()).map{i->val o=arr.getJSONObject(i);ProtoCatalogProduct(o.getString("sku"),o.getString("name"),o.getString("type"),o.optString("quality","—"),o.getString("size"),o.getInt("price"),o.getInt("stock"),o.getString("status"),o.optInt("productionDays",3))}}catch(_:Exception){emptyList()}}

private fun protoDial(context:Context){context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${BuildConfig.ADMIN_PHONE}")))}
private fun protoMessage(context:Context){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${BuildConfig.ADMIN_PHONE}")))}
