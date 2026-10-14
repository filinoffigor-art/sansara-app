package ru.sansara.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val photoBackdrop = Color(0xFFF3EEE6)

// ───────────── Вход ─────────────

@Composable
internal fun ProtoLoginScreen(onBack: () -> Unit, lockedMessage: String?, error: String?, onCodeChange: () -> Unit, onLogin: (String) -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // Новые коды — 6 символов; коды, выданные прежними версиями, были из 4 цифр и должны работать после обновления.
    val canSubmit = code.length >= 4 && lockedMessage == null
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Column(Modifier.fillMaxSize()) {
            ProtoTopBar("Вход", "Код доступа из SMS после одобрения заявки", onBack)
            Column(Modifier.padding(horizontal = 16.dp)) {
                ProtoSectionCard {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { v -> code = v.filter { it.isLetterOrDigit() }.uppercase().take(6); onCodeChange() },
                        label = { Text("Код доступа") },
                        placeholder = { Text("6 символов", color = ProtoMuted) },
                        singleLine = true,
                        isError = error != null,
                        supportingText = { Text(error ?: lockedMessage ?: "Код уникален для каждого сотрудника компании", color = if (error != null || lockedMessage != null) ProtoRed else ProtoMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters),
                        keyboardActions = KeyboardActions(onDone = { if (canSubmit) onLogin(code) }),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, letterSpacing = 6.sp, color = ProtoText),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        colors = protoFieldColors()
                    )
                    if (BuildConfig.DEBUG) Text("Тестовая сборка: клиент 1024 · админ 9001 · производство 9002", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(12.dp))
                    ProtoPrimaryButton("Войти", { onLogin(code) }, enabled = canSubmit)
                }
                Spacer(Modifier.height(8.dp))
                ProtoClickableRow("Нет кода? Позвонить менеджеру", Icons.Outlined.HeadsetMic) { protoDial(context) }
            }
        }
    }
}

// ───────────── Регистрация ─────────────

@Composable
internal fun ProtoRegistrationScreen(onBack: () -> Unit, onSubmit: (ProtoRegistration) -> Unit) {
    var type by remember { mutableStateOf(ProtoClientType.AGENT.label) }
    var organization by remember { mutableStateOf("") }
    var inn by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var volume by remember { mutableStateOf("") }
    var showSecond by remember { mutableStateOf(false) }
    var contact2 by remember { mutableStateOf("") }
    var phone2 by remember { mutableStateOf("") }
    var email2 by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var showConsentText by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val trading = type == ProtoClientType.TRADING.label
    val emailOk = email.trim().let { it.contains("@") && it.substringAfter("@").contains(".") }

    // Ошибки по полям; индекс = позиция элемента в списке, к нему прокручиваем.
    val errors = linkedMapOf<Int, String>()
    if (organization.isBlank()) errors[2] = if (trading) "Укажите название организации" else "Укажите ФИО агента или ИП"
    if (inn.length !in setOf(10, 12)) errors[3] = "ИНН — 10 цифр (организация) или 12 цифр (ИП, агент)"
    if (lastName.isBlank()) errors[5] = "Укажите фамилию"
    if (firstName.isBlank()) errors[6] = "Укажите имя"
    if (phone.length != 10) errors[8] = "Номер из 10 цифр после +7"
    if (!emailOk) errors[9] = "Проверьте e-mail"
    if (city.isBlank()) errors[10] = "Укажите город"
    if (address.isBlank()) errors[11] = "Укажите адрес доставки"
    if ((volume.toLongOrNull() ?: 0L) <= 0L) errors[12] = "Укажите примерный объём продаж в месяц"
    if (showSecond && contact2.isBlank()) errors[13] = "Укажите ФИО второго контакта"
    if (showSecond && phone2.length != 10) errors[14] = "Номер из 10 цифр после +7"
    if (!consent) errors[15] = "Нужно согласие на обработку данных"
    fun err(i: Int) = if (attempted) errors[i] else null

    @Composable
    fun field(value: String, onChange: (String) -> Unit, label: String, index: Int, keyboard: KeyboardType = KeyboardType.Text, prefix: String? = null) {
        OutlinedTextField(
            value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
            isError = err(index) != null,
            supportingText = err(index)?.let { msg -> { Text(msg, color = ProtoRed) } },
            prefix = prefix?.let { p -> { Text(p, color = ProtoText) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth(), colors = protoFieldColors()
        )
    }

    if (showConsentText) AlertDialog(
        onDismissRequest = { showConsentText = false }, containerColor = ProtoPanel,
        title = { Text("Обработка персональных данных", color = ProtoText) },
        text = { Text("Мы используем ваши ФИО, телефон, e-mail и адрес только для проверки заявки, входа в приложение, оформления и доставки заказов. Данные не передаются третьим лицам, кроме служб доставки по вашему заказу. Отозвать согласие можно, написав менеджеру.", color = ProtoMuted) },
        confirmButton = { TextButton(onClick = { showConsentText = false }) { Text("Понятно", color = ProtoGold) } }
    )

    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Column(Modifier.fillMaxSize()) {
            ProtoTopBar("Регистрация партнёра", "Заполните данные для доступа к каталогу и заказам", onBack)
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Text("Тип клиента", color = ProtoText, fontWeight = FontWeight.SemiBold) }                         // 0
                item {                                                                                                    // 1
                    Row(Modifier.fillMaxWidth().height(52.dp).border(1.dp, ProtoGold, RoundedCornerShape(26.dp))) {
                        listOf(ProtoClientType.AGENT.label, ProtoClientType.TRADING.label).forEach { option ->
                            val active = type == option
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(if (active) ProtoGold else Color.Transparent).clickable { type = option }, contentAlignment = Alignment.Center) {
                                Text(option, color = if (active) Color.Black else ProtoText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
                item { field(organization, { organization = it }, if (trading) "Название организации" else "ФИО агента или ИП", 2) }             // 2
                item { field(inn, { inn = it.filter(Char::isDigit).take(12) }, "ИНН", 3, KeyboardType.Number) }          // 3
                item { Text("Контактное лицо", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) } // 4
                item { field(lastName, { lastName = it }, "Фамилия", 5) }                                                 // 5
                item { field(firstName, { firstName = it }, "Имя", 6) }                                                   // 6
                item { field(middleName, { middleName = it }, "Отчество (если есть)", 7) }                                // 7
                item { field(phone, { phone = protoPhoneDigits(it) }, "Телефон", 8, KeyboardType.Phone, "+7 ") }          // 8
                item { field(email, { email = it.trim() }, "E-mail", 9, KeyboardType.Email) }                             // 9
                item { field(city, { city = it }, "Город", 10) }                                                          // 10
                item { field(address, { address = it }, "Адрес доставки", 11) }                                         // 11
                item { field(volume, { volume = it.filter(Char::isDigit).take(10) }, "Примерный объём продаж в месяц, ₽", 12, KeyboardType.Number) } // 12
                item {                                                                                                    // 13
                    if (!showSecond) ProtoClickableRow("+ Добавить второе контактное лицо", Icons.Outlined.PersonAdd) { showSecond = true }
                    else ProtoSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Второе контактное лицо", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showSecond = false; contact2 = ""; phone2 = ""; email2 = "" }) { Icon(Icons.Outlined.Close, "Убрать", tint = ProtoMuted) }
                        }
                        Text("Получит свой код и будет работать в том же кабинете компании.", color = ProtoMuted, fontSize = 12.sp)
                        field(contact2, { contact2 = it }, "ФИО второго контакта", 13)
                    }
                }
                item { if (showSecond) { field(phone2, { phone2 = protoPhoneDigits(it) }, "Телефон второго контакта", 14, KeyboardType.Phone, "+7 "); field(email2, { email2 = it.trim() }, "E-mail второго контакта (необязательно)", -1, KeyboardType.Email) } } // 14
                item {                                                                                                    // 15
                    Row(Modifier.fillMaxWidth().clickable { consent = !consent }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (consent) ProtoGold else ProtoPanel).border(1.dp, if (err(15) != null) ProtoRed else if (consent) ProtoGold else ProtoBorder, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            if (consent) Icon(Icons.Outlined.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Согласен на обработку персональных данных", color = ProtoText, fontSize = 13.sp)
                            Text("Прочитать условия", color = ProtoGold, fontSize = 13.sp, modifier = Modifier.clickable { showConsentText = true })
                        }
                    }
                    err(15)?.let { Text(it, color = ProtoRed, fontSize = 12.sp) }
                }
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("Уже есть код?", color = ProtoMuted); TextButton(onClick = onBack) { Text("Войти", color = ProtoGold) } } }
            }
            Surface(color = ProtoBg) {
                ProtoPrimaryButton("Отправить заявку", {
                    attempted = true
                    val first = errors.keys.firstOrNull()
                    if (first != null) {
                        scope.launch { listState.animateScrollToItem(first) }
                    } else {
                        val fio = listOf(lastName, firstName, middleName).map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
                        onSubmit(
                            ProtoRegistration(
                                organization = organization.trim(), fio = fio, inn = inn, contact1 = fio,
                                phone1 = protoPhone(phone), email = email.trim(), city = city.trim(), address = address.trim(), type = type,
                                contact2 = contact2.trim(), phone2 = if (phone2.length == 10) protoPhone(phone2) else "", email2 = email2.trim(),
                                lastName = lastName.trim(), firstName = firstName.trim(), middleName = middleName.trim(),
                                monthlyVolume = volume.toLongOrNull() ?: 0L
                            )
                        )
                    }
                }, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
internal fun ProtoRegistrationSentScreen(reg: ProtoRegistration?, onBack: () -> Unit, onHaveCode: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Column(Modifier.fillMaxSize()) {
            ProtoBrandHeader(showBell = false)
            // Всё содержимое экрана, включая кнопки, — в одной рамке с равными отступами (как экран фильтра).
            Surface(
                color = ProtoPanel, border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(96.dp).border(3.dp, ProtoGold, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = ProtoGold, modifier = Modifier.size(56.dp)) }
                    Text("Заявка отправлена", color = ProtoText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                    Text(
                        "Проверим данные в течение рабочего дня. Код для входа придёт SMS-сообщением" + (reg?.phone1?.takeIf { it.isNotBlank() }?.let { " на номер " + protoPhone(it) } ?: "") + ".",
                        color = ProtoMuted, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp)
                    )
                    if (reg != null) Column(Modifier.fillMaxWidth().background(ProtoPanel2, RoundedCornerShape(14.dp)).padding(12.dp)) {
                        ProtoInfoLine("Тип клиента", reg.type); ProtoInfoLine("Организация", reg.organization); ProtoInfoLine("Контакт", reg.contact1); ProtoInfoLine("Телефон", protoPhone(reg.phone1))
                    }
                    Spacer(Modifier.weight(1f))
                    ProtoPrimaryButton("Понятно", onBack)
                    Spacer(Modifier.height(10.dp))
                    ProtoSecondaryButton("У меня уже есть код", onHaveCode)
                }
            }
        }
    }
}

// ───────────── Карточка товара в списке ─────────────

@Composable
internal fun ProtoProductCard(
    p: ProtoCatalogProduct,
    currentStock: Int,
    inCart: Int,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onSetQty: (Int) -> Unit,
    discount: Int = 0
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProtoPanel),
        border = BorderStroke(1.dp, if (inCart > 0) ProtoGold.copy(alpha = 0.6f) else ProtoBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onOpen)
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(photoBackdrop)) {
            ProtoProductImage(p, Modifier.fillMaxSize().padding(6.dp), ContentScale.Fit)
            Surface(Modifier.align(Alignment.BottomStart).padding(7.dp), color = if (currentStock > 0) Color(0xEE123A27) else Color(0xEE4A2220), shape = RoundedCornerShape(16.dp)) {
                Text(
                    if (currentStock > 0) "В наличии $currentStock" else "Под заказ · ${p.productionDays} дн.",
                    color = if (currentStock > 0) ProtoGreen else ProtoGoldSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Арт. " + p.sku, color = ProtoGoldSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            // Цена сразу со скидкой партнёра; базовая — зачёркнутой мелкой строкой.
            Row(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                Text(protoMoney(protoUnitPrice(p.price, discount)), color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                if (discount > 0) Text(protoMoney(p.price), color = ProtoMuted, fontSize = 12.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, modifier = Modifier.padding(start = 6.dp, bottom = 2.dp))
            }
            if (inCart <= 0) {
                Button(
                    onClick = { onSetQty(1) },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProtoGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Add, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("В корзину", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            } else {
                Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    ProtoQtyButton(Icons.Outlined.Remove, { onSetQty(inCart - 1) }, 42.dp)
                    Text("$inCart шт.", color = ProtoGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    ProtoQtyButton(Icons.Outlined.Add, { onSetQty(inCart + 1) }, 42.dp)
                }
            }
        }
    }
}

// ───────────── Главная ─────────────

@Composable
internal fun ProtoClientHomeScreen(
    client: ProtoClient,
    products: List<ProtoCatalogProduct>,
    popularity: Map<String, Int>,
    availableStock: (ProtoCatalogProduct) -> Int,
    cart: Map<String, Int>,
    lastOrder: ProtoOrder?,
    unreadCount: Int,
    onNotifications: () -> Unit,
    onSearch: (String) -> Unit,
    onGroup: (String) -> Unit,
    onOpenProduct: (ProtoCatalogProduct) -> Unit,
    onSetQty: (ProtoCatalogProduct, Int) -> Unit,
    onSeeAll: (String) -> Unit,
    onOpenOrder: (ProtoOrder) -> Unit,
    onHome: () -> Unit,
    onCatalog: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    retailMode: Boolean = false,
    onRetailExit: () -> Unit = {},
    onMenu: (() -> Unit)? = null,
    onFilter: () -> Unit = {},
    greetingName: String = client.firstName,
    discount: Int = 0,
    homeFilter: ProtoCatalogFilter = ProtoCatalogFilter(),
    onPopular: () -> Unit = {}
) {
    var mode by remember { mutableStateOf("Все") }
    var searchText by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }
    val suggestions = remember(searchText, products) { if (searchText.trim().length < 2) emptyList() else products.filter { protoSearchMatches(it, searchText) }.take(6) }
    // «Наш ассортимент»: применённый фильтр главной + чипы наличия (они действуют только здесь).
    val assortment = products
        .filter { protoMatches(it, homeFilter.copy(query = ""), availableStock(it)) }
        .filter { p -> when (mode) { "В наличии" -> availableStock(p) > 0; "Под заказ" -> availableStock(p) <= 0; else -> true } }
        .sortedWith(compareByDescending<ProtoCatalogProduct> { availableStock(it) > 0 }.thenBy { it.sku })
    val groupCounts = protoWreathGroupsOrdered.associateWith { g -> products.count { it.type == g } }.filterValues { it > 0 }
    val filterActive = (homeFilter.types.isNotEmpty() && homeFilter.types != protoWreathGroups) || homeFilter.qualities.isNotEmpty() || homeFilter.sizes.isNotEmpty() || homeFilter.availability.isNotEmpty() || homeFilter.minPrice != null || homeFilter.maxPrice != null

    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (retailMode) ProtoRetailBottomBar(ProtoScreen.RetailHome, cart.size, onHome = onHome, onCatalog = onCatalog, onCart = onCart, onExit = onRetailExit, onOrders = onOrders)
                else ProtoClientBottomBar(ProtoScreen.Home, cart.size, onHome = onHome, onCatalog = onCatalog, onCart = onCart, onOrders = onOrders, onProfile = onProfile)
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
            // Неподвижная верхняя часть: бренд, приветствие, поиск, фильтр наличия.
            ProtoBrandHeader(unreadCount = unreadCount, onBell = onNotifications, onMenu = onMenu)
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Здравствуйте, " + greetingName.ifBlank { client.firstName }, color = ProtoText, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (retailMode) Text("Режим клиента · закупочные цены скрыты", color = ProtoGoldSoft, fontSize = 13.sp)
                else Text("Статус: " + client.status + "  ·  Скидка " + client.discount + "%", color = ProtoGoldSoft, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                ProtoSearchField(searchText, { searchText = it }, { if (searchText.isNotBlank()) showResults = true }, "Поиск по артикулу, названию")
                // Подсказки при вводе.
                if (suggestions.isNotEmpty() && !showResults) Surface(color = ProtoPanel2, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ProtoBorder), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Column {
                        suggestions.forEach { p ->
                            Row(Modifier.fillMaxWidth().clickable { searchText = ""; onOpenProduct(p) }.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Search, null, tint = ProtoMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(protoTitle(p), color = ProtoText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Арт. " + p.sku, color = ProtoGoldSoft, fontSize = 12.sp)
                                }
                                Text(protoMoney(protoUnitPrice(p.price, discount)), color = ProtoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ProtoAvailabilityChips(mode) { mode = it } }
                    Spacer(Modifier.width(8.dp))
                    ProtoRoundIconButton(Icons.Outlined.Tune, "Фильтр", onFilter, active = filterActive)
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(groupCounts.keys.toList()) { g ->
                            Card(colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(18.dp), modifier = Modifier.width(132.dp).height(132.dp).clickable { onGroup(g) }) {
                                Box(Modifier.fillMaxSize()) {
                                    androidx.compose.foundation.Image(painterResource(protoPlaceholderForType(g)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
                                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                                        Text(g, color = ProtoText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text(protoPlural(groupCounts[g] ?: 0, "позиция", "позиции", "позиций"), color = ProtoGoldSoft, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        // Разделы в разработке — неактивны.
                        items(listOf("Гробы", "Кресты")) { g ->
                            Card(colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(18.dp), modifier = Modifier.width(132.dp).height(132.dp)) {
                                Box(Modifier.fillMaxSize()) {
                                    androidx.compose.foundation.Image(painterResource(protoPlaceholderForType(g)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.35f)
                                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                                        Text(g, color = ProtoMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("В разработке", color = ProtoMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                if (lastOrder != null && !retailMode) item {
                    ProtoSectionCard(Modifier.padding(horizontal = 16.dp).clickable { onOpenOrder(lastOrder) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Ваш последний заказ", color = ProtoMuted, fontSize = 12.sp)
                                Text("№ " + lastOrder.id + " · " + protoMoney(lastOrder.total), color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (lastOrder.deliveryDate.isNotBlank()) Text("Получение " + lastOrder.deliveryDate, color = ProtoMuted, fontSize = 12.sp)
                            }
                            ProtoStatusPill(lastOrder.status)
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Наш ассортимент", color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(protoPlural(assortment.size, "позиция", "позиции", "позиций") + (if (filterActive) " · по фильтру" else "") + if (mode != "Все") " · " + mode.lowercase() else "", color = ProtoMuted, fontSize = 13.sp)
                    }
                }
                if (assortment.isEmpty()) item {
                    ProtoEmptyState(Icons.Outlined.Inventory2, "По фильтру ничего нет", "Измените фильтр или наличие", actionLabel = "Открыть фильтр", onAction = onFilter)
                } else items(assortment.chunked(2), key = { row -> row.joinToString { it.sku } }) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { p ->
                            ProtoProductCard(p, availableStock(p), cart[p.sku] ?: 0, Modifier.weight(1f), onOpen = { onOpenProduct(p) }, onSetQty = { onSetQty(p, it) }, discount = discount)
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onPopular) { Text("Смотреть популярные", color = ProtoGold, fontSize = 14.sp) }
                    }
                }
            }
            }
        }
    }
    if (showResults) {
        val results = products.filter { protoSearchMatches(it, searchText) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showResults = false }) {
            Surface(color = ProtoPanel, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, ProtoBorder), modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                Column(Modifier.padding(vertical = 14.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Найдено: " + results.size, color = ProtoText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text("по запросу «" + searchText.trim() + "»", color = ProtoMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { showResults = false }) { Icon(Icons.Outlined.Close, contentDescription = "Закрыть", tint = ProtoGold) }
                    }
                    if (results.isEmpty()) Text("Ничего не нашли. Проверьте артикул или название.", color = ProtoMuted, modifier = Modifier.padding(16.dp))
                    LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(results, key = { "sr-" + it.sku }) { p ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ProtoPanel2).clickable { showResults = false; searchText = ""; onOpenProduct(p) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(photoBackdrop)) { ProtoProductImage(p, Modifier.fillMaxSize().padding(3.dp), ContentScale.Fit) }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(protoTitle(p), color = ProtoText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("Арт. " + p.sku + " · " + (if (availableStock(p) > 0) "в наличии" else "под заказ"), color = ProtoGoldSoft, fontSize = 12.sp)
                                }
                                Text(protoMoney(protoUnitPrice(p.price, discount)), color = ProtoText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── Каталог ─────────────

@Composable
internal fun ProtoCatalogHomeScreen(
    cartCount: Int,
    groupCounts: Map<String, Int>,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onGroup: (String) -> Unit,
    onAvailability: (String) -> Unit,
    onHome: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    retailMode: Boolean = false,
    onRetailExit: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Все") }
    val soon = protoProductGroups.filter { !it.enabled }
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(containerColor = Color.Transparent, bottomBar = {
            if (retailMode) ProtoRetailBottomBar(ProtoScreen.RetailCatalog, cartCount, onHome, onCatalog = {}, onCart, onRetailExit, onOrders)
            else ProtoClientBottomBar(ProtoScreen.Catalog, cartCount, onHome, onCatalog = {}, onCart, onOrders, onProfile)
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
            ProtoTopBar("Каталог", "Сейчас в продаже — венки и композиции", onBack)
            Column(Modifier.padding(horizontal = 16.dp)) {
                ProtoSearchField(searchText, { searchText = it }, { onSearch(searchText.trim()) })
                Spacer(Modifier.height(10.dp))
                ProtoAvailabilityChips(mode) { mode = it; onAvailability(it) }
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                gridItems(groupCounts.keys.toList()) { g ->
                    Card(colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(20.dp), modifier = Modifier.height(190.dp).clickable { onGroup(g) }) {
                        Box(Modifier.fillMaxSize()) {
                            androidx.compose.foundation.Image(painterResource(protoPlaceholderForType(g)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
                            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                Text(g, color = ProtoText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(protoPlural(groupCounts[g] ?: 0, "позиция", "позиции", "позиций"), color = ProtoGoldSoft, fontSize = 13.sp)
                            }
                            Icon(Icons.Outlined.ArrowForward, null, tint = ProtoGold, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
                        }
                    }
                }
                if (soon.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) { Text("Скоро в каталоге", color = ProtoMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
                    item(span = { GridItemSpan(2) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(soon) { g ->
                                Surface(color = ProtoPanel, border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(14.dp)) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.foundation.Image(painterResource(protoPlaceholderForType(g.imageType)), null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop, alpha = 0.6f)
                                        Spacer(Modifier.width(8.dp))
                                        Text(g.title, color = ProtoMuted, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

// ───────────── Фильтр ─────────────

@Composable
internal fun ProtoFilterScreen(
    products: List<ProtoCatalogProduct>,
    filter: ProtoCatalogFilter,
    availableStock: (ProtoCatalogProduct) -> Int,
    onApply: (ProtoCatalogFilter) -> Unit,
    onBack: () -> Unit
) {
    var types by remember { mutableStateOf(filter.types.let { if (it.containsAll(protoWreathGroups)) emptySet() else it }) }
    var qualities by remember { mutableStateOf(filter.qualities) }
    var sizes by remember { mutableStateOf(filter.sizes) }
    var availability by remember { mutableStateOf(filter.availability) }
    var minText by remember { mutableStateOf(filter.minPrice?.toString().orEmpty()) }
    var maxText by remember { mutableStateOf(filter.maxPrice?.toString().orEmpty()) }
    val draft = ProtoCatalogFilter("", types, qualities, sizes, availability, minText.toIntOrNull(), maxText.toIntOrNull())
    val groupTypes = protoWreathGroupsOrdered.filter { g -> products.any { it.type == g } }
    fun scope(except: String) = products.filter { protoMatches(it, draft, availableStock(it), except) }
    val allQualities = products.map { it.quality }.filter(::protoValidValue).distinct().sorted()
    val allSizes = products.map { it.size }.filter(::protoValidValue).distinct().sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 9999 }
    val okTypes = scope("type").map { it.type }.toSet()
    val okQualities = scope("quality").map { it.quality }.toSet()
    val okSizes = scope("size").map { it.size }.toSet()
    val okAvail = scope("availability").map { protoAvailabilityLabel(availableStock(it)) }.toSet()
    val matched = products.count { protoMatches(it, draft, availableStock(it)) }
    val priceScope = scope("price")
    val catalogMin = priceScope.minOfOrNull { it.price } ?: 0
    val catalogMax = priceScope.maxOfOrNull { it.price } ?: 0

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .78f)), contentAlignment = Alignment.Center) {
        Surface(color = ProtoBg, border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(horizontal = 12.dp)) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(42.dp).border(1.dp, ProtoBorder, CircleShape)) { Icon(Icons.Outlined.Close, "Закрыть", tint = ProtoGold) }
                    Spacer(Modifier.width(10.dp))
                    Text("Фильтр", color = ProtoText, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { types = emptySet(); qualities = emptySet(); sizes = emptySet(); availability = emptySet(); minText = ""; maxText = "" }) { Text("Сбросить", color = ProtoGold) }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                    item { ProtoOptionGrid("Продукция", groupTypes, types, okTypes) { types = protoToggle(types, it) } }
                    item { ProtoOptionGrid("Качество", allQualities, qualities, okQualities) { qualities = protoToggle(qualities, it) } }
                    item { ProtoOptionGrid("Размер", allSizes, sizes, okSizes) { sizes = protoToggle(sizes, it) } }
                    item { ProtoOptionGrid("Наличие", listOf("В наличии", "Под заказ"), availability, okAvail) { availability = protoToggle(availability, it) } }
                    item {
                        Text("Цена, ₽", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(minText, { minText = it.filter(Char::isDigit).take(7) }, label = { Text("от $catalogMin") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors())
                            OutlinedTextField(maxText, { maxText = it.filter(Char::isDigit).take(7) }, label = { Text("до $catalogMax") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors())
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                ProtoPrimaryButton(
                    if (matched == 0) "Ничего не найдено" else "Показать " + protoPlural(matched, "позицию", "позиции", "позиций"),
                    { onApply(draft.copy(types = if (types.isEmpty()) protoWreathGroups else types)) },
                    enabled = matched > 0
                )
            }
        }
    }
}

// ───────────── Список товаров ─────────────

internal val protoSortOptions = listOf("Сначала в наличии", "Цена ↑", "Цена ↓", "Размер")

@Composable
internal fun ProtoProductListScreen(
    products: List<ProtoCatalogProduct>,
    cart: Map<String, Int>,
    availableStock: (ProtoCatalogProduct) -> Int,
    query: String,
    onQuery: (String) -> Unit,
    activeChips: List<Pair<String, () -> Unit>>,
    sort: String,
    onSort: (String) -> Unit,
    initialIndex: Int,
    initialOffset: Int,
    focusSearch: Boolean,
    onBack: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenProduct: (ProtoCatalogProduct, Int, Int) -> Unit,
    onSetQty: (ProtoCatalogProduct, Int) -> Unit,
    onHome: () -> Unit,
    onCatalog: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    retailMode: Boolean = false,
    onRetailExit: () -> Unit = {},
    discount: Int = 0
) {
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex.coerceAtLeast(0), initialFirstVisibleItemScrollOffset = initialOffset.coerceAtLeast(0))
    var sortOpen by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusSearch) { if (focusSearch) runCatching { focus.requestFocus() } }
    val sorted = when (sort) {
        "Цена ↑" -> products.sortedBy { it.price }
        "Цена ↓" -> products.sortedByDescending { it.price }
        "Размер" -> products.sortedBy { it.size.filter(Char::isDigit).toIntOrNull() ?: 9999 }
        else -> products.sortedByDescending { availableStock(it) > 0 }
    }
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(containerColor = Color.Transparent, bottomBar = {
            if (retailMode) ProtoRetailBottomBar(ProtoScreen.RetailProductList, cart.size, onHome, onCatalog, onCart, onRetailExit, onOrders)
            else ProtoClientBottomBar(ProtoScreen.ProductList, cart.size, onHome, onCatalog, onCart, onOrders, onProfile)
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
            // Неподвижная часть: шапка, поиск, «Изменить фильтр» и сортировка; список прокручивается ниже.
            ProtoTopBar("Каталог", "Найдено: " + protoPlural(products.size, "позиция", "позиции", "позиций"), onBack)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProtoSearchField(query, onQuery, {}, focusRequester = focus)
                run {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenFilter, modifier = Modifier.weight(1.15f), border = BorderStroke(1.dp, ProtoGold), shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Icon(Icons.Outlined.Tune, null, tint = ProtoGold, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Изменить фильтр", color = ProtoGold, maxLines = 1, softWrap = false, fontSize = 13.sp)
                        }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { sortOpen = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                                Icon(Icons.Outlined.SwapVert, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text(sort, color = ProtoText, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                                protoSortOptions.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSort(o); sortOpen = false }) }
                            }
                        }
                    }
                }
                if (activeChips.isNotEmpty()) run {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(activeChips) { (label, remove) ->
                            InputChip(
                                selected = true, onClick = remove, label = { Text(label, fontSize = 13.sp) },
                                trailingIcon = { Icon(Icons.Outlined.Close, "Убрать", modifier = Modifier.size(16.dp)) },
                                colors = InputChipDefaults.inputChipColors(selectedContainerColor = ProtoPanel2, selectedLabelColor = ProtoGoldSoft, selectedTrailingIconColor = ProtoGold)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), state = gridState, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (sorted.isEmpty()) item(span = { GridItemSpan(2) }) {
                    ProtoEmptyState(Icons.Outlined.SearchOff, "Ничего не найдено", "Измените запрос или фильтр", actionLabel = "Открыть фильтр", onAction = onOpenFilter)
                } else gridItems(sorted, key = { it.sku }) { p ->
                    ProtoProductCard(
                        p, availableStock(p), cart[p.sku] ?: 0,
                        onOpen = { onOpenProduct(p, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) },
                        onSetQty = { onSetQty(p, it) }, discount = discount
                    )
                }
            }
            }
        }
    }
}

// ───────────── Карточка товара ─────────────

@Composable
internal fun ProtoProductDetailScreen(
    product: ProtoCatalogProduct?,
    currentStock: Int,
    qty: Int,
    discount: Int,
    inCartQty: Int,
    editingCart: Boolean,
    onBack: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onAdd: () -> Unit,
    onGoCart: () -> Unit,
    onHome: () -> Unit,
    onCatalog: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    retailMode: Boolean = false,
    onRetailExit: () -> Unit = {}
) {
    val p = product ?: return
    var preview by remember { mutableStateOf(false) }
    val base = p.price * qty
    val total = protoLineTotal(p.price, qty, discount)
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(containerColor = Color.Transparent, bottomBar = {
            Column {
                Surface(color = ProtoPanel, border = BorderStroke(1.dp, ProtoBorder)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (inCartQty > 0 && !editingCart) Row(Modifier.fillMaxWidth().clickable(onClick = onGoCart).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ShoppingCart, null, tint = ProtoGreen, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text("В корзине уже $inCartQty шт.", color = ProtoGreen, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("Перейти →", color = ProtoGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Итого · $qty шт.", color = ProtoMuted, fontSize = 13.sp)
                                Text(protoMoney(total), color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), shape = RoundedCornerShape(26.dp), modifier = Modifier.height(52.dp)) {
                                Text(if (editingCart) "Сохранить" else "В корзину", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
                if (retailMode) ProtoRetailBottomBar(ProtoScreen.RetailProductDetail, 0, onHome, onCatalog, onCart, onRetailExit, onOrders)
                else ProtoClientBottomBar(ProtoScreen.ProductDetail, 0, onHome, onCatalog, onCart, onOrders, onProfile)
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
            ProtoTopBar("Товар", null, onBack)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = photoBackdrop), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().height(260.dp).clickable { preview = true }) {
                        ProtoProductImage(p, Modifier.fillMaxSize().padding(10.dp), ContentScale.Fit)
                    }
                }
                item {
                    Text(protoTitle(p), color = ProtoText, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                    Text("Арт. " + p.sku, color = ProtoGoldSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                            Text(protoMoney(protoUnitPrice(p.price, discount)), color = ProtoText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            if (discount > 0) Text(protoMoney(p.price), color = ProtoMuted, fontSize = 14.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                        }
                        Text(
                            if (currentStock > 0) "В наличии $currentStock шт." else "Под заказ · готов к " + protoReadyLabel(p.productionDays),
                            color = if (currentStock > 0) ProtoGreen else ProtoGoldSoft, fontSize = 13.sp, textAlign = TextAlign.End
                        )
                    }
                    if (currentStock in 1 until qty) Text(
                        "В наличии $currentStock шт., остальные " + (qty - currentStock) + " шт. будут готовы к " + protoReadyLabel(p.productionDays),
                        color = ProtoOrange, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
                    )
                }
                item {
                    ProtoSectionCard {
                        Text("Количество", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            ProtoQtyButton(Icons.Outlined.Remove, onMinus, size = 44.dp)
                            Text(qty.toString(), color = ProtoText, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                            ProtoQtyButton(Icons.Outlined.Add, onPlus, size = 44.dp)
                        }
                        HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
                        ProtoInfoRow("Цена за 1 шт." + if (discount > 0) " с вашей скидкой $discount%" else "", protoMoney(protoUnitPrice(p.price, discount)))
                        if (discount > 0) ProtoInfoRow("Экономия", protoMoney(base - total))
                    }
                }
                item { ProtoSectionCard { Text("Характеристики", color = ProtoGoldSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold); ProtoInfoRow("Категория", p.type); if (protoValidValue(p.quality)) ProtoInfoRow("Качество", p.quality); if (protoValidValue(p.size)) ProtoInfoRow("Размер", p.size); ProtoInfoRow("Срок под заказ", protoPlural(p.productionDays, "рабочий день", "рабочих дня", "рабочих дней")) } }
            }
            }
        }
    }
    if (preview) ProtoProductImagePreview(p) { preview = false }
}

// ───────────── Корзина ─────────────

@Composable
internal fun ProtoCartScreen(
    products: List<ProtoCatalogProduct>,
    cart: Map<String, Int>,
    discount: Int,
    availableStock: (ProtoCatalogProduct) -> Int,
    onBack: () -> Unit,
    onPlus: (ProtoCatalogProduct) -> Unit,
    onMinus: (ProtoCatalogProduct) -> Unit,
    onDelete: (ProtoCatalogProduct) -> Unit,
    onRestore: (ProtoCatalogProduct, Int) -> Unit,
    onOpenProduct: (ProtoCatalogProduct) -> Unit,
    onCheckout: () -> Unit,
    onHome: () -> Unit,
    onCatalog: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    retailMode: Boolean = false,
    onRetailExit: () -> Unit = {}
) {
    val lines = cart.mapNotNull { (sku, q) -> products.firstOrNull { it.sku == sku }?.let { it to q } }
    val base = lines.sumOf { it.first.price * it.second }
    val total = lines.sumOf { protoLineTotal(it.first.price, it.second, discount) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (retailMode) ProtoRetailBottomBar(ProtoScreen.RetailCart, cart.size, onHome, onCatalog, onCart = {}, onRetailExit, onOrders)
                else ProtoClientBottomBar(ProtoScreen.Cart, cart.size, onHome, onCatalog, onCart = {}, onOrders, onProfile)
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                ProtoTopBar("Корзина", if (lines.isEmpty()) null else protoPlural(lines.size, "позиция", "позиции", "позиций") + " · " + lines.sumOf { it.second } + " шт.", onBack)
                if (lines.isEmpty()) {
                    ProtoEmptyState(Icons.Outlined.ShoppingCart, "Корзина пуста", "Добавьте венки из каталога", actionLabel = "Перейти в каталог", onAction = onCatalog, modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(lines, key = { it.first.sku }) { (p, q) ->
                            val available = availableStock(p)
                            ProtoSectionCard {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(photoBackdrop).clickable { onOpenProduct(p) }) {
                                        ProtoProductImage(p, Modifier.fillMaxSize().padding(4.dp), ContentScale.Fit)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f).clickable { onOpenProduct(p) }) {
                                        Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("Арт. " + p.sku, color = ProtoGoldSoft, fontSize = 12.sp)
                                        Text("$q × " + protoMoney(protoUnitPrice(p.price, discount)) + " = " + protoMoney(protoLineTotal(p.price, q, discount)), color = ProtoMuted, fontSize = 13.sp)
                                        if (q > available) Text(if (available > 0) "В наличии $available шт., остальное под заказ" else "Под заказ · готов к " + protoReadyLabel(p.productionDays), color = ProtoOrange, fontSize = 12.sp)
                                    }
                                    IconButton(onClick = {
                                        onDelete(p)
                                        scope.launch {
                                            val r = snackbar.showSnackbar("Удалено: " + protoTitle(p), actionLabel = "Вернуть", duration = SnackbarDuration.Short)
                                            if (r == SnackbarResult.ActionPerformed) onRestore(p, q)
                                        }
                                    }) { Icon(Icons.Outlined.DeleteOutline, "Удалить", tint = ProtoMuted) }
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ProtoQtyButton(Icons.Outlined.Remove, { onMinus(p) }, 38.dp)
                                    Text(q.toString(), color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 14.dp))
                                    ProtoQtyButton(Icons.Outlined.Add, { onPlus(p) }, 38.dp)
                                    Spacer(Modifier.weight(1f))
                                    Text(protoMoney(protoLineTotal(p.price, q, discount)), color = ProtoGoldSoft, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                    ProtoSectionCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        if (discount > 0) ProtoInfoRow("Без скидки", protoMoney(base))
                        if (discount > 0) ProtoInfoRow("Ваша скидка $discount% уже в ценах", "−" + protoMoney(base - total))
                        HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text("Итого", color = ProtoText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(protoMoney(total), color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ProtoPrimaryButton("Оформить заказ · " + protoMoney(total), onCheckout, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
        }
    }
}

// ───────────── Оформление ─────────────

@Composable
internal fun ProtoCheckoutScreen(
    cartPositions: Int,
    totalPieces: Int,
    baseTotal: Int,
    finalTotal: Int,
    discount: Int,
    defaultAddress: String,
    contacts: List<SansaraCompanyContact>,
    clientStatus: String,
    earliestDate: LocalDate,
    shortages: List<String>,
    pickupPoints: List<String>,
    cancelKind: String = "stock",
    onBack: () -> Unit,
    onSubmit: (String, String, String, String, String, String, String) -> Unit,
    onHome: () -> Unit,
    onCatalog: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    retailMode: Boolean = false,
    onRetailExit: () -> Unit = {}
) {
    var method by remember { mutableStateOf("Доставка") }
    var address by remember(defaultAddress) { mutableStateOf(defaultAddress) }
    var pickup by remember(pickupPoints) { mutableStateOf(pickupPoints.firstOrNull().orEmpty()) }
    var tk by remember { mutableStateOf("") }
    var tkCity by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedContact by remember(contacts) { mutableStateOf(contacts.firstOrNull()) }
    var selectedDate by remember(earliestDate) { mutableStateOf(earliestDate) }
    var selectedTime by remember { mutableStateOf("до 12:00") }
    var showCalendar by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    val destination = when (method) {
        "Самовывоз" -> pickup.trim()
        "ТК" -> listOf(tk.trim(), tkCity.trim()).filter { it.isNotBlank() }.joinToString(", ")
        else -> address.trim()
    }
    val destinationOk = when (method) { "ТК" -> tk.isNotBlank() && tkCity.isNotBlank(); else -> destination.isNotBlank() }
    val canSubmit = selectedContact != null && destinationOk && !submitting

    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(containerColor = Color.Transparent, bottomBar = {
            if (retailMode) ProtoRetailBottomBar(ProtoScreen.RetailCheckout, cartPositions, onHome, onCatalog, onCart, onRetailExit, onOrders)
            else ProtoClientBottomBar(ProtoScreen.Checkout, cartPositions, onHome, onCatalog, onCart, onOrders, onProfile)
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                ProtoTopBar("Оформление заказа", protoPlural(cartPositions, "позиция", "позиции", "позиций") + " · $totalPieces шт. · " + protoMoney(finalTotal), onBack)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        ProtoSectionCard {
                            Text("Контактное лицо", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                            contacts.forEach { contact ->
                                val active = selectedContact?.userId == contact.userId
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { selectedContact = contact }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = active, onClick = { selectedContact = contact }, colors = RadioButtonDefaults.colors(selectedColor = ProtoGold))
                                    Column { Text(contact.name, color = ProtoText, fontWeight = FontWeight.SemiBold); Text(protoPhone(contact.phone), color = ProtoMuted, fontSize = 13.sp) }
                                }
                            }
                        }
                    }
                    item {
                        ProtoSectionCard {
                            Text("Способ получения", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Доставка", "Самовывоз", "ТК").forEach { label ->
                                    val active = method == label
                                    Surface(color = if (active) ProtoGold else ProtoPanel2, border = BorderStroke(1.dp, if (active) ProtoGold else ProtoBorder), shape = RoundedCornerShape(22.dp), modifier = Modifier.weight(1f).height(44.dp).clickable { method = label }) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, color = if (active) Color.Black else ProtoText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            when (method) {
                                "Самовывоз" -> if (pickupPoints.isEmpty()) ProtoField(pickup, { pickup = it }, "Точка самовывоза") else pickupPoints.forEach { point ->
                                    Row(Modifier.fillMaxWidth().clickable { pickup = point }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = pickup == point, onClick = { pickup = point }, colors = RadioButtonDefaults.colors(selectedColor = ProtoGold))
                                        Text(point, color = ProtoText, fontSize = 14.sp)
                                    }
                                }
                                "ТК" -> { ProtoField(tk, { tk = it }, "Транспортная компания"); ProtoField(tkCity, { tkCity = it }, "Город получения") }
                                else -> ProtoField(address, { address = it }, "Адрес доставки")
                            }
                        }
                    }
                    item {
                        ProtoSectionCard(Modifier.clickable { showCalendar = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = ProtoGold)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (method == "Доставка") "Дата доставки" else "Дата получения", color = ProtoMuted, fontSize = 12.sp)
                                    Text(selectedDate.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", ruLocale)), color = ProtoText, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.Outlined.ChevronRight, null, tint = ProtoGold)
                            }
                        }
                    }
                    item {
                        Text("Время", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("до 12:00", "12:00–15:00", "15:00–18:00").forEach { slot ->
                                val active = selectedTime == slot
                                Surface(color = if (active) ProtoGold else ProtoPanel, border = BorderStroke(1.dp, if (active) ProtoGold else ProtoBorder), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).height(44.dp).clickable { selectedTime = slot }) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(slot, color = if (active) Color.Black else ProtoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                                }
                            }
                        }
                    }
                    if (shortages.isNotEmpty()) item {
                        ProtoSectionCard {
                            Text("Часть заказа будет произведена", color = ProtoOrange, fontWeight = FontWeight.Bold)
                            shortages.forEach { Text(it, color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp)) }
                            Text("Ранняя дата: " + earliestDate.format(DateTimeFormatter.ofPattern("d MMMM", ruLocale)), color = ProtoGoldSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    item {
                        OutlinedTextField(value = comment, onValueChange = { if (it.length <= 500) comment = it }, label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, colors = protoFieldColors())
                        Text(comment.length.toString() + "/500", color = ProtoMuted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                    }
                    item {
                        ProtoSectionCard {
                            ProtoInfoRow("Статус", clientStatus)
                            ProtoInfoRow("Товары", protoMoney(baseTotal))
                            if (discount > 0) ProtoInfoRow("Скидка клиента $discount%", "−" + protoMoney(baseTotal - finalTotal))
                            ProtoInfoRow("Всего", "$totalPieces шт.")
                            HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 7.dp))
                            ProtoInfoRow("Итого", protoMoney(finalTotal))
                        }
                    }
                }
                ProtoPrimaryButton(
                    "Отправить заказ · " + protoMoney(finalTotal),
                    { if (selectedContact != null && canSubmit) showRules = true },
                    enabled = canSubmit,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
    if (showRules) AlertDialog(
        onDismissRequest = { showRules = false }, containerColor = ProtoPanel,
        title = { Text("Правила отмены заказа", color = ProtoText) },
        text = { Text(protoCancelRulesText(cancelKind), color = ProtoMuted, fontSize = 14.sp, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = {
                showRules = false
                val contact = selectedContact
                // Защита от двойного нажатия: второй тап создавал второй заказ.
                if (contact != null && !submitting) {
                    submitting = true
                    onSubmit(method, destination, comment, contact.name, contact.phone, selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), selectedTime)
                }
            }) { Text("Согласен, отправить", color = ProtoGold, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = { showRules = false }) { Text("Назад", color = ProtoMuted) } }
    )
    if (showCalendar) {
        ProtoProductionCalendarDialog(
            selectedDate = selectedDate, onDismiss = { showCalendar = false },
            onSelect = { date -> selectedDate = date; showCalendar = false },
            minDate = earliestDate, workdaysOnly = true
        )
    }
}

// ───────────── Заказ отправлен ─────────────

@Composable
internal fun ProtoOrderSentScreen(
    order: ProtoOrder?,
    sendState: String,
    onView: () -> Unit,
    onCall: () -> Unit,
    onHome: () -> Unit,
    onCatalog: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit
) {
    val o = order ?: return
    val (text, color) = when (sendState) {
        "ok" -> "Заказ принят. Менеджер подтвердит его и сообщит, если что-то изменится." to ProtoMuted
        "sending" -> "Отправляем заказ менеджеру…" to ProtoMuted
        "failed" -> "Заказ сохранён, но не дошёл до менеджера: нет связи с сервером. Позвоните, чтобы подтвердить." to ProtoOrange
        else -> "Заказ сохранён в приложении. Сервер ещё не подключён — позвоните менеджеру, чтобы подтвердить." to ProtoOrange
    }
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(containerColor = Color.Transparent, bottomBar = { ProtoClientBottomBar(ProtoScreen.OrderSent, 0, onHome, onCatalog, onCart, onOrders, onProfile) }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ProtoBrandHeader(showBell = false)
                Spacer(Modifier.height(20.dp))
                Box(Modifier.size(104.dp).border(3.dp, ProtoGold, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = ProtoGold, modifier = Modifier.size(60.dp)) }
                Text("Заказ отправлен", color = ProtoText, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                Text(text, color = color, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                ProtoSectionCard {
                    Text("Заказ № " + o.id, color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    ProtoInfoLine("Статус", o.status)
                    ProtoInfoLine("Контактное лицо", o.recipient.ifBlank { o.clientName })
                    ProtoInfoLine("Сумма", protoMoney(o.total))
                    ProtoInfoLine("Получение", listOf(o.deliveryMethod, o.deliveryDate, o.deliveryTime).filter { it.isNotBlank() }.joinToString(" · "))
                }
                Spacer(Modifier.weight(1f))
                ProtoPrimaryButton("Смотреть заказ", onView)
                Spacer(Modifier.height(10.dp))
                if (sendState == "ok") ProtoSecondaryButton("На главную", onHome) else ProtoSecondaryButton("Позвонить менеджеру", onCall)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

// ───────────── Заказ ─────────────

internal val protoOrderFlow = listOf("Получен", "Подтверждён", "Собирается", "Доставляется", "Доставлен")

@Composable
internal fun ProtoOrderDetailScreen(
    order: ProtoOrder?,
    products: List<ProtoCatalogProduct>,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onStatus: (String) -> Unit,
    onRepeat: (() -> Unit)? = null,
    onEditRepeat: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    cartCount: Int = 0,
    onChat: (() -> Unit)? = null,
    onCallClient: (() -> Unit)? = null,
    onToWorkshop: (() -> Unit)? = null
) {
    val o = order ?: return
    val finished = o.status == "Доставлен" || o.status == "Отменён"
    val next = protoOrderFlow.getOrNull(protoOrderFlow.indexOf(o.status) + 1)
    var confirmCancel by remember(o.id) { mutableStateOf(false) }
    var refusal by remember(o.id) { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(o.id) { while (true) { kotlinx.coroutines.delay(30_000); now = System.currentTimeMillis() } }
    val cancelState = protoCancelState(o, now)
    refusal?.let { msg ->
        AlertDialog(
            onDismissRequest = { refusal = null }, containerColor = ProtoPanel,
            title = { Text("Отмена невозможна", color = ProtoText) },
            text = { Text(msg, color = ProtoMuted, lineHeight = 20.sp) },
            confirmButton = { TextButton(onClick = { refusal = null }) { Text("Понятно", color = ProtoGold) } },
            dismissButton = { if (onChat != null) TextButton(onClick = { refusal = null; onChat() }) { Text("Написать менеджеру", color = ProtoMuted) } }
        )
    }
    var confirmRepeat by remember(o.id) { mutableStateOf<(() -> Unit)?>(null) }
    if (confirmCancel && onCancel != null) AlertDialog(
        onDismissRequest = { confirmCancel = false }, containerColor = ProtoPanel,
        title = { Text("Отменить заказ № " + o.id + "?", color = ProtoText) },
        text = { Text(if (isAdmin) "Резерв снимется, списанный товар вернётся на склад. Клиент получит уведомление." else "Заказ будет отменён. Вернуть его нельзя — только оформить заново.", color = ProtoMuted) },
        confirmButton = { TextButton(onClick = { confirmCancel = false; onCancel.invoke() }) { Text("Отменить заказ", color = ProtoRed) } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Назад", color = ProtoMuted) } }
    )
    confirmRepeat?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmRepeat = null }, containerColor = ProtoPanel,
            title = { Text("Добавить к корзине?", color = ProtoText) },
            text = { Text("В корзине уже " + protoPlural(cartCount, "позиция", "позиции", "позиций") + ". Позиции заказа добавятся к ним по текущим ценам.", color = ProtoMuted) },
            confirmButton = { TextButton(onClick = { confirmRepeat = null; action() }) { Text("Добавить", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { confirmRepeat = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
    fun guarded(action: () -> Unit) { if (cartCount > 0) confirmRepeat = action else action() }

    ProtoScaffold("Заказ № " + o.id, o.clientName + (if (o.retailCustomerName.isNotBlank()) " → " + o.retailCustomerName else "") + " · " + o.dateTime, onBack) {
        item {
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Статус", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    ProtoStatusPill(o.status)
                }
                Spacer(Modifier.height(6.dp))
                ProtoOrderTimeline(o)
                if (isAdmin && !finished && next != null) {
                    Spacer(Modifier.height(10.dp))
                    ProtoPrimaryButton("Перевести: $next", { onStatus(next) })
                }
            }
        }
        item {
            ProtoSectionCard {
                if (o.recipient.isNotBlank()) ProtoInfoLine("Контактное лицо", o.recipient)
                if (o.contactPhone.isNotBlank()) ProtoInfoLine("Телефон", protoPhone(o.contactPhone))
                ProtoInfoLine("Получение", o.deliveryMethod)
                if (o.deliveryAddress.isNotBlank()) ProtoInfoLine(if (o.deliveryMethod == "Самовывоз") "Точка" else "Адрес", o.deliveryAddress)
                if (o.deliveryDate.isNotBlank()) ProtoInfoLine("Дата", o.deliveryDate + if (o.deliveryTime.isNotBlank()) ", " + o.deliveryTime else "")
                if (o.comment.isNotBlank()) { Text("Комментарий", color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)); Text(o.comment, color = ProtoText) }
            }
        }
        item { Text("Состав заказа", color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(o.lines, key = { it.sku }) { line ->
            val product = products.firstOrNull { it.sku == line.sku }
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product != null) { Box(Modifier.size(64.dp).clip(RoundedCornerShape(11.dp)).background(photoBackdrop)) { ProtoProductImage(product, Modifier.fillMaxSize().padding(3.dp), ContentScale.Fit) }; Spacer(Modifier.width(10.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(line.name, color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Арт. " + line.sku, color = ProtoGoldSoft, fontSize = 12.sp)
                        Text(line.qty.toString() + " шт. × " + protoMoney(line.price), color = ProtoMuted, fontSize = 13.sp)
                    }
                    Text(protoMoney(line.price * line.qty), color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            ProtoSectionCard {
                ProtoInfoRow("Товары", protoMoney(o.baseTotal))
                if (o.discountPct > 0) ProtoInfoRow("Скидка клиента " + o.discountPct + "%", "−" + protoMoney(o.baseTotal - o.total))
                HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 7.dp))
                ProtoInfoRow("Итого", protoMoney(o.total))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (isAdmin) {
                    if (onCallClient != null) ProtoSecondaryButton("Позвонить клиенту", onCallClient)
                    if (!finished && onToWorkshop != null) ProtoSecondaryButton("Задание в цех из этого заказа", onToWorkshop)
                    if (!finished && onCancel != null) OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, ProtoRed), shape = RoundedCornerShape(24.dp)) { Text("Отменить заказ", color = ProtoRed) }
                } else if (finished) {
                    if (onRepeat != null) ProtoPrimaryButton("Заказать повторно", { guarded(onRepeat) })
                    if (onEditRepeat != null) ProtoSecondaryButton("Изменить и заказать", { guarded(onEditRepeat) })
                } else {
                    if (onChat != null) ProtoSecondaryButton("Написать менеджеру", onChat)
                    if (onCancel != null) {
                        OutlinedButton(
                            onClick = { if (cancelState.allowed) confirmCancel = true else refusal = cancelState.message },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            border = BorderStroke(1.dp, if (cancelState.allowed) ProtoRed else ProtoBorder), shape = RoundedCornerShape(24.dp)
                        ) { Text("Отменить заказ", color = if (cancelState.allowed) ProtoRed else ProtoMuted) }
                        Text(
                            if (cancelState.allowed) cancelState.message else "Срок отмены (" + protoCancelWindowLabel(cancelState.kind) + ") прошёл",
                            color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
                    }
                    if (onRepeat != null) TextButton(onClick = { guarded(onRepeat) }, modifier = Modifier.fillMaxWidth()) { Text("Заказать такой же ещё раз", color = ProtoGold) }
                }
            }
        }
        if (isAdmin) {
            item { Text("История заказа", color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(o.history.asReversed()) { event ->
                ProtoSectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.History, null, tint = if (event.status == "Отменён") ProtoRed else ProtoGold)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) { Text(event.status, color = if (event.status == "Отменён") ProtoRed else ProtoText, fontWeight = FontWeight.SemiBold); Text(event.dateTime, color = ProtoMuted, fontSize = 12.sp) }
                        Text(event.actor, color = ProtoGoldSoft, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ───────────── Уведомления ─────────────

@Composable
internal fun ProtoNotificationsScreen(
    notifications: List<ProtoNotification>,
    onBack: () -> Unit,
    onReadAll: () -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (ProtoNotification) -> Unit = {}
) {
    LaunchedEffect(Unit) { onReadAll() }
    val today = currentDateShort()
    val (fresh, earlier) = notifications.partition { it.dateTime.startsWith(today) }
    ProtoScaffold("Уведомления", "Статусы заказов и сообщения", onBack) {
        if (notifications.isEmpty()) item { ProtoEmptyState(Icons.Outlined.NotificationsNone, "Уведомлений пока нет", "Здесь появятся изменения статусов ваших заказов") }
        listOf("Сегодня" to fresh, "Ранее" to earlier).forEach { (label, list) ->
            if (list.isNotEmpty()) {
                item { Text(label, color = ProtoGoldSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp)) }
                items(list, key = { it.id }) { n ->
                    val orderId = n.orderId.ifBlank { Regex("S-\\d+").find(n.title + " " + n.message)?.value.orEmpty() }
                    ProtoSectionCard(if (orderId.isNotBlank()) Modifier.clickable { onOpen(n.copy(orderId = orderId)) } else Modifier) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!n.read) { Box(Modifier.size(8.dp).background(ProtoGold, CircleShape)); Spacer(Modifier.width(7.dp)) }
                                    Text(n.title, color = ProtoText, fontWeight = FontWeight.Bold)
                                }
                                Text(n.message, color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                                Text(n.dateTime, color = ProtoGoldSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                            IconButton(onClick = { onDelete(n.id) }) { Icon(Icons.Outlined.DeleteOutline, "Удалить", tint = ProtoMuted) }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── Популярные ─────────────

@Composable
internal fun ProtoPopularScreen(
    products: List<ProtoCatalogProduct>,
    soldUnits: Map<String, Int>,
    cart: Map<String, Int>,
    availableStock: (ProtoCatalogProduct) -> Int,
    discount: Int,
    onBack: () -> Unit,
    onOpenProduct: (ProtoCatalogProduct) -> Unit,
    onSetQty: (ProtoCatalogProduct, Int) -> Unit,
    onHome: () -> Unit, onCatalog: () -> Unit, onCart: () -> Unit, onOrders: () -> Unit, onProfile: () -> Unit
) {
    val ranked = products.filter { (soldUnits[it.sku] ?: 0) > 0 }.sortedByDescending { soldUnits[it.sku] ?: 0 }.take(30)
    ProtoScaffold("Популярные", "Больше всего продано по SANSARA за 30 дней", onBack, bottomBar = {
        ProtoClientBottomBar(ProtoScreen.Popular, cart.size, onHome, onCatalog, onCart, onOrders, onProfile)
    }) {
        if (ranked.isEmpty()) item { ProtoEmptyState(Icons.Outlined.TrendingUp, "Пока нет продаж за 30 дней", "Подборка появится после первых доставленных заказов") }
        items(ranked.chunked(2), key = { row -> "pop-" + row.joinToString { it.sku } }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { p ->
                    Box(Modifier.weight(1f)) {
                        ProtoProductCard(p, availableStock(p), cart[p.sku] ?: 0, Modifier.fillMaxWidth(), onOpen = { onOpenProduct(p) }, onSetQty = { onSetQty(p, it) }, discount = discount)
                        Surface(color = ProtoGold, shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                            Text("${soldUnits[p.sku] ?: 0} шт.", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
