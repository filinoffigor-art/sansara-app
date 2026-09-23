package ru.sansara.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.sansara.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private enum class Role { CLIENT, ADMIN, PRODUCTION }
private enum class Screen {
    WELCOME, LOGIN, REGISTER, PENDING,
    CLIENT_HOME, CATALOG, CART, CHECKOUT, ORDER_SUCCESS, ORDERS, PROFILE,
    ADMIN_HOME, ADMIN_CLIENTS, ADMIN_CLIENT, ADMIN_ORDERS, ADMIN_STOCK, ADMIN_SETTINGS,
    PRODUCTION_HOME, PRODUCTION_HISTORY
}

enum class ClientType(val label: String) { AGENT("Агент"), TRADING("Торгующая организация") }
enum class ClientStatus(val label: String) {
    ACTIVE("Активный"), WHOLESALER("Оптовик"), VIP("VIP"), SUSPENDED("Приостановлен")
}
enum class OrderStatus(val label: String) {
    NEW("Получен"), CONFIRMED("Подтверждён"), PICKING("Собирается"), DELIVERY("Доставляется"), DELIVERED("Доставлен")
}

data class Client(
    val id: String,
    val name: String,
    val type: ClientType,
    val phone: String,
    val email: String,
    val inn: String = "",
    val city: String = "",
    val address: String = "",
    val status: ClientStatus = ClientStatus.ACTIVE,
    val discount: Int = 0,
    val orderingEnabled: Boolean = true
)

data class Registration(
    val id: String,
    val name: String,
    val inn: String,
    val contact: String,
    val phone: String,
    val email: String,
    val city: String,
    val address: String,
    val type: ClientType
)

data class ProductModel(val id: String, val name: String, val category: String)

data class Product(
    val sku: String,
    val modelId: String,
    val name: String,
    val category: String,
    val size: String,
    val price: Int,
    val physical: Int,
    val reserved: Int = 0,
    val productionLeadDays: Int = 3
) {
    val available: Int get() = (physical - reserved).coerceAtLeast(0)
}

data class OrderLine(val sku: String, val name: String, val qty: Int, val unitPrice: Int)
data class Order(
    val id: String,
    val clientName: String,
    val lines: List<OrderLine>,
    val total: Int,
    val status: OrderStatus,
    val delivery: String,
    val comment: String
)
data class ProductionOperation(val time: String, val user: String, val qty: Int)

private object Store {
    var currentClient by mutableStateOf(
        Client(
            id = "C-1024",
            name = "ООО Ритуал-Сервис",
            type = ClientType.TRADING,
            phone = "+7 999 123-45-67",
            email = "info@ritual-service.ru",
            inn = "7701234567",
            city = "Москва",
            address = "г. Москва, ул. Ленинская, д. 10, стр. 2",
            status = ClientStatus.WHOLESALER,
            discount = 10,
            orderingEnabled = true
        )
    )

    val registrations: SnapshotStateList<Registration> = mutableStateListOf(
        Registration("R-1001", "ООО Мемориал", "7712345678", "Анна Сергеева", "+7 999 456-12-12", "office@memorial.ru", "Москва", "ул. Центральная, 7", ClientType.TRADING),
        Registration("R-1002", "Сергей Иванов", "", "Сергей Иванов", "+7 916 555-10-20", "agent@example.ru", "Тула", "", ClientType.AGENT)
    )

    val models: SnapshotStateList<ProductModel> = mutableStateListOf(
        ProductModel("M-001", "Венок Классик", "Венки"),
        ProductModel("M-002", "Венок Премиум", "Венки"),
        ProductModel("M-003", "Лента траурная", "Ленты"),
        ProductModel("M-004", "Гроб Классик", "Гробы"),
        ProductModel("M-005", "Одежда мужская", "Одежда")
    )

    val products: SnapshotStateList<Product> = mutableStateListOf(
        Product("V-125-037", "M-001", "Венок Классик", "Венки", "125 см", 4850, 12),
        Product("V-200-018", "M-002", "Венок Премиум", "Венки", "200 см", 7900, 0),
        Product("L-90-014", "M-003", "Лента траурная", "Ленты", "9 см × 50 м", 680, 5),
        Product("C-01", "M-004", "Гроб Классик", "Гробы", "190 см", 18500, 2),
        Product("O-54-011", "M-005", "Одежда мужская", "Одежда", "54", 3400, 8)
    )

    val cart: SnapshotStateMap<String, Int> = mutableStateMapOf()
    val orders: SnapshotStateList<Order> = mutableStateListOf(
        Order(
            "S-002384", "ООО Ритуал-Сервис",
            listOf(OrderLine("V-125-037", "Венок Классик", 4, 4850)),
            17460, OrderStatus.PICKING,
            "Доставка: Москва, до 12:00", "Позвонить за час"
        )
    )
    val productionHistory: SnapshotStateList<ProductionOperation> = mutableStateListOf(
        ProductionOperation("23.09.2026 10:20", "Петров И.А.", 53),
        ProductionOperation("22.09.2026 15:10", "Смирнова Е.В.", 28)
    )

    fun discountedPrice(product: Product): Int = product.price * (100 - currentClient.discount) / 100

    fun addToCart(product: Product, qty: Int = 1) {
        if (!currentClient.orderingEnabled || currentClient.status == ClientStatus.SUSPENDED) return
        cart[product.sku] = (cart[product.sku] ?: 0) + qty
    }

    fun cartTotal(): Int = cart.entries.sumOf { (sku, qty) ->
        products.find { it.sku == sku }?.let { discountedPrice(it) * qty } ?: 0
    }

    fun submitOrder(delivery: String, comment: String): Order {
        val lines = cart.mapNotNull { (sku, qty) ->
            products.find { it.sku == sku }?.let { p -> OrderLine(sku, p.name, qty, discountedPrice(p)) }
        }
        val order = Order(
            id = "S-${(2384 + orders.size).toString().padStart(6, '0')}",
            clientName = currentClient.name,
            lines = lines,
            total = lines.sumOf { it.qty * it.unitPrice },
            status = OrderStatus.NEW,
            delivery = delivery,
            comment = comment
        )
        lines.forEach { line ->
            val index = products.indexOfFirst { it.sku == line.sku }
            if (index >= 0) {
                val p = products[index]
                products[index] = p.copy(reserved = p.reserved + line.qty)
            }
        }
        orders.add(0, order)
        cart.clear()
        return order
    }

    fun postProduction(lines: Map<String, Int>, user: String = "Производство") {
        var total = 0
        lines.forEach { (sku, qty) ->
            if (qty <= 0) return@forEach
            val index = products.indexOfFirst { it.sku == sku }
            if (index >= 0) {
                val p = products[index]
                products[index] = p.copy(physical = p.physical + qty)
                total += qty
            }
        }
        if (total > 0) productionHistory.add(0, ProductionOperation("23.09.2026 сейчас", user, total))
    }
}

@Composable
fun SansaraApp() {
    var screen by remember { mutableStateOf(Screen.WELCOME) }
    var role by remember { mutableStateOf<Role?>(null) }
    var lastOrder by remember { mutableStateOf<Order?>(null) }

    when (screen) {
        Screen.WELCOME -> WelcomeScreen(onLogin = { screen = Screen.LOGIN }, onRegister = { screen = Screen.REGISTER })
        Screen.LOGIN -> LoginScreen(
            onBack = { screen = Screen.WELCOME },
            onLoggedIn = { newRole ->
                role = newRole
                screen = when (newRole) {
                    Role.CLIENT -> Screen.CLIENT_HOME
                    Role.ADMIN -> Screen.ADMIN_HOME
                    Role.PRODUCTION -> Screen.PRODUCTION_HOME
                }
            }
        )
        Screen.REGISTER -> RegistrationScreen(onBack = { screen = Screen.WELCOME }, onSubmitted = { screen = Screen.PENDING })
        Screen.PENDING -> PendingScreen(onDone = { screen = Screen.WELCOME })

        Screen.CLIENT_HOME -> ClientHomeScreen(
            onCatalog = { screen = Screen.CATALOG }, onCart = { screen = Screen.CART },
            onOrders = { screen = Screen.ORDERS }, onProfile = { screen = Screen.PROFILE }
        )
        Screen.CATALOG -> CatalogScreen(onHome = { screen = Screen.CLIENT_HOME }, onCart = { screen = Screen.CART }, onOrders = { screen = Screen.ORDERS }, onProfile = { screen = Screen.PROFILE })
        Screen.CART -> CartScreen(onBack = { screen = Screen.CATALOG }, onCheckout = { screen = Screen.CHECKOUT })
        Screen.CHECKOUT -> CheckoutScreen(onBack = { screen = Screen.CART }, onSubmitted = {
            lastOrder = it
            screen = Screen.ORDER_SUCCESS
        })
        Screen.ORDER_SUCCESS -> OrderSuccessScreen(order = lastOrder, onOrders = { screen = Screen.ORDERS }, onCatalog = { screen = Screen.CATALOG })
        Screen.ORDERS -> ClientOrdersScreen(onBack = { screen = Screen.CLIENT_HOME })
        Screen.PROFILE -> ProfileScreen(onBack = { screen = Screen.CLIENT_HOME }, onLogout = { role = null; screen = Screen.WELCOME })

        Screen.ADMIN_HOME -> AdminHomeScreen(
            onClients = { screen = Screen.ADMIN_CLIENTS },
            onOrders = { screen = Screen.ADMIN_ORDERS },
            onProduction = { screen = Screen.ADMIN_STOCK },
            onStock = { screen = Screen.ADMIN_STOCK },
            onSettings = { screen = Screen.ADMIN_SETTINGS },
            onLogout = { role = null; screen = Screen.WELCOME }
        )
        Screen.ADMIN_CLIENTS -> AdminClientsScreen(onBack = { screen = Screen.ADMIN_HOME }, onClient = { screen = Screen.ADMIN_CLIENT })
        Screen.ADMIN_CLIENT -> AdminClientScreen(onBack = { screen = Screen.ADMIN_CLIENTS })
        Screen.ADMIN_ORDERS -> AdminOrdersScreen(onBack = { screen = Screen.ADMIN_HOME })
        Screen.ADMIN_STOCK -> AdminStockScreen(onBack = { screen = Screen.ADMIN_HOME })
        Screen.ADMIN_SETTINGS -> SimpleScreen("Настройки", "Статусы, категории, способы доставки и бизнес-правила будут управляться здесь.") { screen = Screen.ADMIN_HOME }

        Screen.PRODUCTION_HOME -> ProductionScreen(onHistory = { screen = Screen.PRODUCTION_HISTORY }, onLogout = { role = null; screen = Screen.WELCOME })
        Screen.PRODUCTION_HISTORY -> ProductionHistoryScreen(onBack = { screen = Screen.PRODUCTION_HOME })
    }
}

@Composable
private fun BaseScreen(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = Gold) }
            }
            Column(Modifier.weight(1f)) {
                Text("SANSARA", color = GoldSoft, fontSize = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = 5.sp)
                Text("Оптовая платформа ритуальных товаров", color = Muted, fontSize = 12.sp)
            }
        }
        Text(title, color = Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 14.sp)
        content()
    }
}

@Composable
private fun GoldButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF15100B))
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun DarkCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val clickModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Card(
        modifier = clickModifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Border)
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content) }
}

@Composable
private fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    BaseScreen("Добро пожаловать", "Работаем с агентами и торговыми организациями") {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.LocalFlorist, null, tint = Gold, modifier = Modifier.size(110.dp).align(Alignment.CenterHorizontally))
        Text("Ритуальные товары и услуги в одном приложении", color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Каталог, наличие, индивидуальные условия и заказы — после регистрации и подтверждения.", color = Muted)
        Spacer(Modifier.weight(1f))
        GoldButton("Войти", onClick = onLogin)
        OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(54.dp), border = BorderStroke(1.dp, Gold), shape = RoundedCornerShape(14.dp)) {
            Text("Стать партнёром", color = Text)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LoginScreen(onBack: () -> Unit, onLoggedIn: (Role) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    BaseScreen("Вход", "На сервере роль определяется по вашему аккаунту", onBack) {
        OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        OutlinedTextField(code, { code = it }, label = { Text("Код подтверждения") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Text("Alpha: для администратора используйте 8 961 126-66-75; для производства — 8 999 000-00-01; любой другой номер — клиент.", color = Muted, fontSize = 12.sp)
        GoldButton("Войти") {
            val digits = phone.filter(Char::isDigit).takeLast(10)
            val adminDigits = BuildConfig.ADMIN_PHONE.filter(Char::isDigit).takeLast(10)
            val resolved = when (digits) {
                adminDigits -> Role.ADMIN
                "9990000001" -> Role.PRODUCTION
                else -> Role.CLIENT
            }
            onLoggedIn(resolved)
        }
    }
}

@Composable
private fun RegistrationScreen(onBack: () -> Unit, onSubmitted: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var inn by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ClientType.AGENT) }

    BaseScreen("Регистрация партнёра", "После проверки администратор назначит статус и скидку", onBack) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("Название организации / ФИО") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(inn, { inn = it }, label = { Text("ИНН") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
            item { OutlinedTextField(contact, { contact = it }, label = { Text("Контактное лицо") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) }
            item { OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)) }
            item { OutlinedTextField(city, { city = it }, label = { Text("Город") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(address, { address = it }, label = { Text("Адрес доставки") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(type == ClientType.AGENT, { type = ClientType.AGENT }, label = { Text("Агент") })
                    FilterChip(type == ClientType.TRADING, { type = ClientType.TRADING }, label = { Text("Торгующая организация") })
                }
            }
            item {
                GoldButton("Отправить заявку", enabled = name.isNotBlank() && phone.isNotBlank()) {
                    Store.registrations.add(0, Registration("R-${1000 + Store.registrations.size + 1}", name, inn, contact, phone, email, city, address, type))
                    onSubmitted()
                }
            }
        }
    }
}

@Composable
private fun PendingScreen(onDone: () -> Unit) {
    BaseScreen("Заявка отправлена", "Администратор проверит данные и откроет доступ") {
        Spacer(Modifier.height(30.dp))
        Icon(Icons.Outlined.CheckCircle, null, tint = Gold, modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally))
        DarkCard {
            Text("Что дальше", color = Text, fontWeight = FontWeight.Bold)
            Text("1. Администратор видит новую регистрацию.", color = Muted)
            Text("2. Назначает статус: Активный / Оптовик / VIP / Приостановлен.", color = Muted)
            Text("3. Назначает скидку и включает доступ к заказам.", color = Muted)
        }
        GoldButton("Понятно", onClick = onDone)
    }
}

@Composable
private fun ClientHomeScreen(onCatalog: () -> Unit, onCart: () -> Unit, onOrders: () -> Unit, onProfile: () -> Unit) {
    val client = Store.currentClient
    BaseScreen("Здравствуйте", "${client.name} · ${client.status.label} · скидка ${client.discount}%") {
        if (client.status == ClientStatus.SUSPENDED || !client.orderingEnabled) SuspendedBanner()
        DarkCard(onClick = onCatalog) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Storefront, null, tint = Gold); Spacer(Modifier.width(12.dp)); Text("Открыть каталог", fontSize = 20.sp) } }
        Text("Популярные товары", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Store.products.take(3), key = { it.sku }) { p -> ProductCard(p, onAdd = { Store.addToCart(p) }) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCart, modifier = Modifier.weight(1f)) { Text("Корзина (${Store.cart.values.sum()})") }
                    OutlinedButton(onClick = onOrders, modifier = Modifier.weight(1f)) { Text("Заказы") }
                    IconButton(onClick = onProfile) { Icon(Icons.Outlined.Person, null) }
                }
            }
        }
    }
}

@Composable
private fun SuspendedBanner() {
    val context = LocalContext.current
    DarkCard {
        Text("Ваш статус временно приостановлен", color = Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Самостоятельное оформление заказов недоступно. Свяжитесь с администратором.", color = Muted)
        GoldButton("Позвонить администратору") {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${BuildConfig.ADMIN_PHONE}")))
        }
        Text("8 961 126-66-75", color = Gold)
    }
}

@Composable
private fun ProductCard(product: Product, onAdd: () -> Unit) {
    DarkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(categoryIcon(product.category), null, tint = Gold, modifier = Modifier.size(54.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, color = Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Арт. ${product.sku} · ${product.size}", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.FavoriteBorder, null, tint = Gold)
        }
        val customerPrice = Store.discountedPrice(product)
        Text("${money(customerPrice)} ₽", color = GoldSoft, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (product.available > 0) {
            Text("● В наличии ${product.available} шт.", color = Green)
            Button(onClick = onAdd, enabled = Store.currentClient.orderingEnabled && Store.currentClient.status != ClientStatus.SUSPENDED) { Text("В корзину") }
        } else {
            Text("● Нет в наличии", color = Red)
            Text("Срок производства: от ${product.productionLeadDays} дней", color = Gold)
            Button(onClick = onAdd, enabled = Store.currentClient.orderingEnabled && Store.currentClient.status != ClientStatus.SUSPENDED) { Text("Под заказ") }
        }
    }
}

@Composable
private fun CatalogScreen(onHome: () -> Unit, onCart: () -> Unit, onOrders: () -> Unit, onProfile: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Все") }
    val filtered = Store.products.filter { p ->
        (query.isBlank() || p.name.contains(query, true) || p.sku.contains(query, true)) &&
            when (mode) { "В наличии" -> p.available > 0; "Под заказ" -> p.available == 0; else -> true }
    }
    BaseScreen("Каталог", "Остатки обновляются после оприходования на складе") {
        OutlinedTextField(query, { query = it }, label = { Text("Поиск по названию или артикулу") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Search, null) })
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Все", "В наличии", "Под заказ")) { x -> FilterChip(mode == x, { mode = x }, label = { Text(x) }) }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered, key = { it.sku }) { p -> ProductCard(p) { Store.addToCart(p) } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onHome) { Text("Главная") }
                    TextButton(onClick = onCart) { Text("Корзина (${Store.cart.values.sum()})") }
                    TextButton(onClick = onOrders) { Text("Заказы") }
                    TextButton(onClick = onProfile) { Text("Профиль") }
                }
            }
        }
    }
}

@Composable
private fun CartScreen(onBack: () -> Unit, onCheckout: () -> Unit) {
    BaseScreen("Корзина", "${Store.cart.values.sum()} товаров", onBack) {
        if (Store.cart.isEmpty()) {
            DarkCard { Text("Корзина пока пуста", color = Muted) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Store.cart.keys.toList(), key = { it }) { sku ->
                    val p = Store.products.first { it.sku == sku }
                    val qty = Store.cart[sku] ?: 0
                    DarkCard {
                        Text(p.name, fontWeight = FontWeight.Bold)
                        Text("${p.sku} · ${money(Store.discountedPrice(p))} ₽", color = Muted)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = { if (qty <= 1) Store.cart.remove(sku) else Store.cart[sku] = qty - 1 }) { Icon(Icons.Outlined.Remove, null) }
                            Text(qty.toString(), fontSize = 20.sp)
                            IconButton(onClick = { Store.cart[sku] = qty + 1 }) { Icon(Icons.Outlined.Add, null) }
                            Spacer(Modifier.weight(1f))
                            Text("${money(Store.discountedPrice(p) * qty)} ₽", color = Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item { DarkCard { Text("Итого", color = Muted); Text("${money(Store.cartTotal())} ₽", fontSize = 28.sp, color = Text, fontWeight = FontWeight.Bold) } }
                item { GoldButton("Оформить заказ", enabled = Store.currentClient.orderingEnabled && Store.currentClient.status != ClientStatus.SUSPENDED, onClick = onCheckout) }
            }
        }
    }
}

@Composable
private fun CheckoutScreen(onBack: () -> Unit, onSubmitted: (Order) -> Unit) {
    var delivery by remember { mutableStateOf("Доставка") }
    var comment by remember { mutableStateOf("") }
    BaseScreen("Оформление заказа", "Проверьте получателя и способ получения", onBack) {
        DarkCard { Text(Store.currentClient.name, fontWeight = FontWeight.Bold); Text(Store.currentClient.phone, color = Muted); Text(Store.currentClient.address.ifBlank { "Адрес не указан" }, color = Muted) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Доставка", "Самовывоз", "ТК").forEach { d -> FilterChip(delivery == d, { delivery = d }, label = { Text(d) }) }
        }
        OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий к заказу") }, modifier = Modifier.fillMaxWidth())
        DarkCard {
            Text("Ваш статус: ${Store.currentClient.status.label}", color = Gold)
            Text("Скидка: ${Store.currentClient.discount}%", color = Gold)
            Text("Итого: ${money(Store.cartTotal())} ₽", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        GoldButton("Отправить заказ", enabled = Store.cart.isNotEmpty()) {
            onSubmitted(Store.submitOrder("$delivery · ${Store.currentClient.address}", comment))
        }
        Text("После отправки менеджер получит заявку на сборку и доставку.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun OrderSuccessScreen(order: Order?, onOrders: () -> Unit, onCatalog: () -> Unit) {
    BaseScreen("Заказ отправлен", "Менеджер получил заявку на сборку и доставку") {
        Icon(Icons.Outlined.CheckCircle, null, tint = Gold, modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally))
        DarkCard {
            Text("Заказ № ${order?.id ?: "—"}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Статус: ${order?.status?.label ?: "Получен"}", color = Gold)
            Text("Сумма: ${money(order?.total ?: 0)} ₽", color = Text)
        }
        GoldButton("Смотреть заказ", onClick = onOrders)
        OutlinedButton(onClick = onCatalog, modifier = Modifier.fillMaxWidth()) { Text("Вернуться в каталог") }
    }
}

@Composable
private fun ClientOrdersScreen(onBack: () -> Unit) {
    BaseScreen("Мои заказы", "Все этапы заказа отображаются здесь", onBack) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Store.orders, key = { it.id }) { o ->
                DarkCard {
                    Text("Заказ ${o.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(o.status.label, color = Gold)
                    Text("${o.lines.sumOf { it.qty }} поз. · ${money(o.total)} ₽", color = Muted)
                    LinearProgressIndicator(progress = { (o.status.ordinal + 1) / OrderStatus.entries.size.toFloat() }, modifier = Modifier.fillMaxWidth(), color = Gold)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val c = Store.currentClient
    BaseScreen("Профиль", c.name, onBack) {
        DarkCard {
            Text("Тип клиента: ${c.type.label}")
            Text("Статус: ${c.status.label}", color = if (c.status == ClientStatus.SUSPENDED) Red else Gold)
            Text("Скидка: ${c.discount}%")
            Text("Client ID: ${c.id}", color = Muted)
        }
        if (c.status == ClientStatus.SUSPENDED || !c.orderingEnabled) SuspendedBanner()
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Выйти") }
    }
}

@Composable
private fun AdminHomeScreen(onClients: () -> Unit, onOrders: () -> Unit, onProduction: () -> Unit, onStock: () -> Unit, onSettings: () -> Unit, onLogout: () -> Unit) {
    BaseScreen("Здравствуйте, Игорь", "Администратор") {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard("Новые заявки", Store.registrations.size.toString(), Modifier.weight(1f), onClients)
                    KpiCard("Активные клиенты", "146", Modifier.weight(1f), onClients)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard("Заказы сегодня", Store.orders.size.toString(), Modifier.weight(1f), onOrders)
                    KpiCard("Производство", Store.productionHistory.firstOrNull()?.qty?.toString() ?: "0", Modifier.weight(1f), onProduction)
                }
            }
            item { KpiCard("Низкие остатки", Store.products.count { it.available in 0..3 }.toString(), Modifier.fillMaxWidth(), onStock) }
            item { Text("Быстрые действия", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { QuickAction("Клиенты", Icons.Outlined.Groups, onClients) }
                    item { QuickAction("Заказы", Icons.Outlined.ShoppingCart, onOrders) }
                    item { QuickAction("Склад", Icons.Outlined.Inventory2, onStock) }
                    item { QuickAction("Настройки", Icons.Outlined.Settings, onSettings) }
                }
            }
            item { Text("Требует внимания", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            item { DarkCard(onClick = onClients) { Text("${Store.registrations.size} новых регистраций"); Text("Требуют проверки", color = Muted) } }
            item { DarkCard(onClick = onOrders) { Text("${Store.orders.count { it.status == OrderStatus.PICKING }} заказов на сборке"); Text("Ожидают контроля", color = Muted) } }
            item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Выйти") } }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    DarkCard(modifier = modifier, onClick = onClick) { Text(label, color = Muted, fontSize = 12.sp); Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.width(115.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Gold); Spacer(Modifier.height(8.dp)); Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun AdminClientsScreen(onBack: () -> Unit, onClient: () -> Unit) {
    BaseScreen("Клиенты", "Новые регистрации и действующие клиенты", onBack) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Store.registrations, key = { it.id }) { r ->
                DarkCard {
                    Text(r.name, fontWeight = FontWeight.Bold)
                    Text("${r.type.label} · ${r.phone}", color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            Store.currentClient = Client("C-${1100 + Store.registrations.size}", r.name, r.type, r.phone, r.email, r.inn, r.city, r.address, ClientStatus.ACTIVE, 0, true)
                            Store.registrations.remove(r)
                        }) { Text("Одобрить") }
                        OutlinedButton(onClick = { Store.registrations.remove(r) }) { Text("Отклонить") }
                    }
                }
            }
            item { Text("Действующие", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            item { DarkCard(onClick = onClient) { Text(Store.currentClient.name, fontWeight = FontWeight.Bold); Text("${Store.currentClient.status.label} · скидка ${Store.currentClient.discount}%", color = Gold) } }
        }
    }
}

@Composable
private fun AdminClientScreen(onBack: () -> Unit) {
    var client by remember { mutableStateOf(Store.currentClient) }
    val context = LocalContext.current
    BaseScreen("Карточка клиента", client.name, onBack) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Статус клиента", fontWeight = FontWeight.Bold) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ClientStatus.entries) { s -> FilterChip(client.status == s, { client = client.copy(status = s, orderingEnabled = s != ClientStatus.SUSPENDED) }, label = { Text(s.label) }) }
                }
            }
            item {
                DarkCard {
                    Text("Скидка", color = Muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { client = client.copy(discount = (client.discount - 1).coerceAtLeast(0)) }) { Icon(Icons.Outlined.Remove, null) }
                        Text("${client.discount}%", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { client = client.copy(discount = (client.discount + 1).coerceAtMost(50)) }) { Icon(Icons.Outlined.Add, null) }
                    }
                }
            }
            item {
                DarkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Доступ к заказам", fontWeight = FontWeight.Bold); Text("Клиент может оформлять заказы", color = Muted) }
                        Switch(client.orderingEnabled, { client = client.copy(orderingEnabled = it) })
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { Store.currentClient = client }, modifier = Modifier.weight(1f)) { Text("Сохранить") }
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))) }, modifier = Modifier.weight(1f)) { Text("Позвонить") }
                }
            }
            item {
                DarkCard {
                    Text("Пользователи компании", fontWeight = FontWeight.Bold)
                    UserRow("Иван Петров", "Закупщик", true)
                    UserRow("Мария Смирнова", "Бухгалтер", false)
                    UserRow("Алексей Орлов", "Менеджер", true)
                    OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Подключить нового пользователя") }
                }
            }
            item {
                DarkCard {
                    Text("История действий", fontWeight = FontWeight.Bold)
                    Text("Сегодня · изменён статус клиента", color = Muted)
                    Text("Сегодня · изменена скидка", color = Muted)
                }
            }
        }
    }
}

@Composable
private fun UserRow(name: String, role: String, initial: Boolean) {
    var enabled by remember { mutableStateOf(initial) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(name); Text(role, color = Muted, fontSize = 12.sp) }
        Text(if (enabled) "Активен" else "Отключен", color = if (enabled) Green else Red, fontSize = 12.sp)
        Switch(enabled, { enabled = it })
    }
}

@Composable
private fun AdminOrdersScreen(onBack: () -> Unit) {
    BaseScreen("Заказы", "Администратор управляет этапами заказа", onBack) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Store.orders, key = { it.id }) { order ->
                var status by remember(order.id) { mutableStateOf(order.status) }
                DarkCard {
                    Text("${order.id} · ${order.clientName}", fontWeight = FontWeight.Bold)
                    Text("${money(order.total)} ₽ · ${order.lines.sumOf { it.qty }} шт.", color = Muted)
                    Text("Статус: ${status.label}", color = Gold)
                    Button(onClick = { if (status.ordinal < OrderStatus.entries.lastIndex) status = OrderStatus.entries[status.ordinal + 1] }) { Text("Следующий этап") }
                }
            }
        }
    }
}

@Composable
private fun AdminStockScreen(onBack: () -> Unit) {
    var month by remember { mutableStateOf("Сентябрь 2026") }
    BaseScreen("Сервер данных", "Учёт по месяцам · операционные данные", onBack) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Август 2026", "Сентябрь 2026", "Октябрь 2026")) { m -> FilterChip(month == m, { month = m }, label = { Text(m) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("Выпуск", Store.productionHistory.sumOf { it.qty }.toString(), Modifier.weight(1f)) { }
            KpiCard("Резерв", Store.products.sumOf { it.reserved }.toString(), Modifier.weight(1f)) { }
            KpiCard("Доступно", Store.products.sumOf { it.available }.toString(), Modifier.weight(1f)) { }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Store.products, key = { it.sku }) { p ->
                DarkCard {
                    Text("${p.sku} · ${p.name}", fontWeight = FontWeight.Bold)
                    Row { Text("Остаток ${p.physical}", Modifier.weight(1f), color = Muted); Text("Резерв ${p.reserved}", Modifier.weight(1f), color = Muted); Text("Доступно ${p.available}", color = Gold) }
                }
            }
        }
    }
}

@Composable
private fun ProductionScreen(onHistory: () -> Unit, onLogout: () -> Unit) {
    var quantities by remember { mutableStateOf(Store.products.associate { it.sku to 0 }) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showArticleDialog by remember { mutableStateOf(false) }
    var posted by remember { mutableStateOf(false) }

    BaseScreen("Производство", "После оприходования данные видят клиент и администратор") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showModelDialog = true }, modifier = Modifier.weight(1f)) { Text("Добавить модель") }
            OutlinedButton(onClick = { showArticleDialog = true }, modifier = Modifier.weight(1f)) { Text("Добавить артикул") }
        }
        Text("Ежедневный выпуск", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Store.products, key = { it.sku }) { p ->
                DarkCard {
                    Text(p.name, fontWeight = FontWeight.Bold)
                    Text("${p.sku} · сейчас доступно ${p.available}", color = Muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { quantities = quantities + (p.sku to ((quantities[p.sku] ?: 0) - 1).coerceAtLeast(0)) }) { Icon(Icons.Outlined.Remove, null) }
                        Text((quantities[p.sku] ?: 0).toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { quantities = quantities + (p.sku to ((quantities[p.sku] ?: 0) + 1)) }) { Icon(Icons.Outlined.Add, null) }
                        Spacer(Modifier.weight(1f))
                        Text("● На приход", color = Green)
                    }
                }
            }
            item {
                DarkCard {
                    Text("Итого выпуск за день", color = Muted)
                    Text("${quantities.values.sum()} шт.", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                GoldButton("Оприходовать на склад", enabled = quantities.values.sum() > 0) {
                    Store.postProduction(quantities)
                    quantities = Store.products.associate { it.sku to 0 }
                    posted = true
                }
            }
            if (posted) item { Text("Приход проведён. Товар появился в наличии у клиентов.", color = Green) }
            item { OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("История приходования") } }
            item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Выйти") } }
        }
    }

    if (showModelDialog) AddModelDialog(onDismiss = { showModelDialog = false })
    if (showArticleDialog) AddArticleDialog(onDismiss = { showArticleDialog = false })
}

@Composable
private fun AddModelDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Венки") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая модель") },
        text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Название модели") }); OutlinedTextField(category, { category = it }, label = { Text("Категория") }) } },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) Store.models.add(ProductModel("M-${Store.models.size + 1}", name, category)); onDismiss() }) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun AddArticleDialog(onDismiss: () -> Unit) {
    var sku by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf(Store.models.firstOrNull()?.name.orEmpty()) }
    var size by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый артикул") },
        text = {
            Column {
                OutlinedTextField(sku, { sku = it }, label = { Text("Артикул") })
                OutlinedTextField(modelName, { modelName = it }, label = { Text("Модель") })
                OutlinedTextField(size, { size = it }, label = { Text("Размер") })
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Цена") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (sku.isNotBlank() && modelName.isNotBlank()) {
                    val model = Store.models.find { it.name.equals(modelName, true) } ?: ProductModel("M-${Store.models.size + 1}", modelName, "Венки").also { Store.models.add(it) }
                    if (Store.products.none { it.sku.equals(sku, true) }) Store.products.add(Product(sku, model.id, model.name, model.category, size, price.toIntOrNull() ?: 0, 0))
                }
                onDismiss()
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun ProductionHistoryScreen(onBack: () -> Unit) {
    BaseScreen("История приходования", "Проведённые операции", onBack) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Store.productionHistory) { op -> DarkCard { Text(op.time, fontWeight = FontWeight.Bold); Text("${op.user} · ${op.qty} шт.", color = Muted); Text("● Проведен", color = Green) } }
        }
    }
}

@Composable
private fun SimpleScreen(title: String, text: String, onBack: () -> Unit) {
    BaseScreen(title, null, onBack) { DarkCard { Text(text, color = Muted) } }
}

private fun categoryIcon(category: String) = when (category) {
    "Венки" -> Icons.Outlined.LocalFlorist
    "Ленты" -> Icons.Outlined.BookmarkBorder
    "Гробы" -> Icons.Outlined.Inventory2
    "Одежда" -> Icons.Outlined.Checkroom
    else -> Icons.Outlined.Category
}

private fun money(value: Int): String = NumberFormat.getIntegerInstance(Locale("ru", "RU")).format(value)
