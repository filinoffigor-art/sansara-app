package ru.sansara.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Строка бренда — одинаковая на всех экранах, кроме «Добро пожаловать»:
 * уменьшенная надпись SANSARA и подпись «Ваш оптовый партнёр» по центру,
 * кнопки слева и справа (назад / меню / колокольчик) стоят на линии центра надписи.
 */
@Composable
internal fun ProtoBrandRow(left: (@Composable () -> Unit)? = null, right: (@Composable () -> Unit)? = null, horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp) {
    Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = horizontalPadding)) {
        Column(Modifier.align(Alignment.TopCenter).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.sansara_wordmark_letters),
                contentDescription = "SANSARA",
                modifier = Modifier.height(22.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Text("Ваш оптовый партнёр", color = ProtoGoldSoft, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp))
        }
        // Центр надписи — 10 dp отступа + половина её высоты (11 dp) = 21 dp; кнопки 40 dp → сдвиг 1 dp.
        if (left != null) Box(Modifier.align(Alignment.TopStart).padding(top = 1.dp)) { left() }
        if (right != null) Box(Modifier.align(Alignment.TopEnd).padding(top = 1.dp)) { right() }
    }
}

@Composable
internal fun ProtoCircleBack(onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(ProtoPanel).border(1.dp, ProtoGold, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад", tint = ProtoGold, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, description: String, onClick: () -> Unit, border: Color = ProtoGold) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(ProtoPanel).border(1.dp, border, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = ProtoGold, modifier = Modifier.size(22.dp))
    }
}

/** Шапка главных экранов ролей: строка бренда с меню и колокольчиком. */
@Composable
internal fun ProtoBrandHeader(
    onBack: (() -> Unit)? = null,
    unreadCount: Int = 0,
    onBell: (() -> Unit)? = null,
    showBell: Boolean = onBell != null,
    onMenu: (() -> Unit)? = null,
    edge: androidx.compose.ui.unit.Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth()) {
        ProtoBrandRow(
            horizontalPadding = if (edge == 0.dp) 0.dp else 12.dp,
            left = when {
                onBack != null -> { { ProtoCircleBack(onBack) } }
                onMenu != null -> { { CircleIcon(Icons.Outlined.Menu, "Меню", onMenu, ProtoBorder) } }
                else -> null
            },
            right = if (showBell && onBell != null) { {
                BadgedBox(badge = { if (unreadCount > 0) Badge(containerColor = ProtoGold) { Text(unreadCount.coerceAtMost(99).toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }) {
                    CircleIcon(Icons.Outlined.Notifications, "Уведомления", onBell)
                }
            } } else null
        )
    }
}

/** Шапка внутренних экранов: строка бренда («назад» и одно действие на линии надписи), под ней заголовок экрана. */
@Composable
internal fun ProtoTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        ProtoBrandRow(left = onBack?.let { b -> { ProtoCircleBack(b) } }, right = action)
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)) {
            Text(title, color = ProtoText, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = ProtoMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ───────────── Нижнее меню ─────────────
// Иконка стоит прямо над подписью; золотая «таблетка» активной вкладки не заходит на текст.

@Composable
internal fun ProtoNavRow(content: @Composable RowScope.() -> Unit) {
    Surface(color = ProtoPanel, tonalElevation = 0.dp) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom, content = content)
    }
}

@Composable
internal fun RowScope.ProtoNavItem(selected: Boolean, label: String, icon: ImageVector, onClick: () -> Unit, badge: Int = 0) {
    Column(
        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onClick).padding(bottom = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(Modifier.size(width = 50.dp, height = 28.dp).background(if (selected) ProtoGold else Color.Transparent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            BadgedBox(badge = { if (badge > 0) Badge(containerColor = if (selected) Color.Black else ProtoGold) { Text(badge.coerceAtMost(99).toString(), color = if (selected) ProtoGold else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }) {
                Icon(icon, contentDescription = label, tint = if (selected) Color.Black else ProtoMuted, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (selected) ProtoGold else ProtoMuted, fontSize = 11.sp, maxLines = 1, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
internal fun ProtoClientBottomBar(current: ProtoScreen, cartCount: Int, onHome: () -> Unit, onCatalog: () -> Unit, onCart: () -> Unit, onOrders: () -> Unit, onProfile: () -> Unit) {
    ProtoNavRow {
        ProtoNavItem(current == ProtoScreen.Home, "Главная", Icons.Outlined.Home, onHome)
        ProtoNavItem(current in setOf(ProtoScreen.Catalog, ProtoScreen.Filter, ProtoScreen.ProductList, ProtoScreen.ProductDetail), "Каталог", Icons.Outlined.Inventory2, onCatalog)
        ProtoNavItem(current in setOf(ProtoScreen.Cart, ProtoScreen.Checkout), "Корзина", Icons.Outlined.ShoppingCart, onCart, badge = cartCount)
        ProtoNavItem(current in setOf(ProtoScreen.OrderList, ProtoScreen.OrderDetail, ProtoScreen.OrderSent), "Заказы", Icons.Outlined.ReceiptLong, onOrders)
        ProtoNavItem(current == ProtoScreen.ClientChat, "Чат", Icons.Outlined.ChatBubbleOutline, onProfile)
    }
}

@Composable
internal fun ProtoRetailBottomBar(current: ProtoScreen, cartCount: Int, onHome: () -> Unit, onCatalog: () -> Unit, onCart: () -> Unit, onExit: () -> Unit, onOrders: () -> Unit = {}) {
    ProtoNavRow {
        ProtoNavItem(current == ProtoScreen.RetailHome, "Главная", Icons.Outlined.Home, onHome)
        ProtoNavItem(current in setOf(ProtoScreen.RetailCatalog, ProtoScreen.RetailFilter, ProtoScreen.RetailProductList, ProtoScreen.RetailProductDetail), "Каталог", Icons.Outlined.Inventory2, onCatalog)
        ProtoNavItem(current in setOf(ProtoScreen.RetailCart, ProtoScreen.RetailCheckout), "Корзина", Icons.Outlined.ShoppingCart, onCart, badge = cartCount)
        ProtoNavItem(current in setOf(ProtoScreen.RetailOrderList, ProtoScreen.RetailOrderDetail, ProtoScreen.RetailOrderSent), "Заказы", Icons.Outlined.ReceiptLong, onOrders)
        ProtoNavItem(false, "Выйти", Icons.Outlined.ExitToApp, onExit)
    }
}

@Composable
internal fun ProtoAdminBottomBar(current: ProtoScreen, onHome: () -> Unit, onClients: () -> Unit, onOrders: () -> Unit, onStock: () -> Unit, onProfile: () -> Unit) {
    ProtoNavRow {
        ProtoNavItem(current == ProtoScreen.AdminHome, "Главная", Icons.Outlined.Home, onHome)
        ProtoNavItem(current in setOf(ProtoScreen.AdminClients, ProtoScreen.AdminClient), "Клиенты", Icons.Outlined.Groups, onClients)
        ProtoNavItem(current in setOf(ProtoScreen.AdminOrders, ProtoScreen.AdminOrderDetail), "Заказы", Icons.Outlined.ReceiptLong, onOrders)
        ProtoNavItem(current in setOf(ProtoScreen.Server, ProtoScreen.StockList, ProtoScreen.ReserveList), "Склад", Icons.Outlined.Inventory2, onStock)
        ProtoNavItem(current in setOf(ProtoScreen.AdminSettings, ProtoScreen.AdminSettingsDetail), "Профиль", Icons.Outlined.Person, onProfile)
    }
}

@Composable
internal fun ProtoProductionBottomBar(current: ProtoScreen, onRelease: () -> Unit, onHistory: () -> Unit, onStock: () -> Unit, onProfile: () -> Unit) {
    ProtoNavRow {
        ProtoNavItem(current == ProtoScreen.Production, "Выпуск", Icons.Outlined.LocalFlorist, onRelease)
        ProtoNavItem(current == ProtoScreen.ProductionHistory, "История", Icons.Outlined.History, onHistory)
        ProtoNavItem(current in setOf(ProtoScreen.Server, ProtoScreen.StockList, ProtoScreen.ReserveList), "Склад", Icons.Outlined.Inventory2, onStock)
        ProtoNavItem(current == ProtoScreen.ProductionProfile, "Профиль", Icons.Outlined.Person, onProfile)
    }
}

@Composable
internal fun ProtoSalesBottomBar(current: ProtoScreen, onHome: () -> Unit, onShipments: () -> Unit, onClients: () -> Unit, onProfile: () -> Unit) {
    ProtoNavRow {
        ProtoNavItem(current == ProtoScreen.SalesHome, "Главная", Icons.Outlined.Home, onHome)
        ProtoNavItem(current in setOf(ProtoScreen.SalesShipments, ProtoScreen.SalesShipmentNew), "Отгрузки", Icons.Outlined.LocalShipping, onShipments)
        ProtoNavItem(current in setOf(ProtoScreen.SalesClients, ProtoScreen.SalesClient), "Клиенты", Icons.Outlined.Groups, onClients)
        ProtoNavItem(current == ProtoScreen.SalesProfile, "Профиль", Icons.Outlined.Person, onProfile)
    }
}

/** Единое пустое состояние: иконка, фраза, подсказка, действие. */
@Composable
internal fun ProtoEmptyState(
    icon: ImageVector,
    text: String,
    hint: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = ProtoGold, modifier = Modifier.size(30.dp))
        }
        Text(text, color = ProtoText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
        if (!hint.isNullOrBlank()) Text(hint, color = ProtoMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAction, border = BorderStroke(1.dp, ProtoGold), shape = RoundedCornerShape(24.dp)) {
                Text(actionLabel, color = ProtoGold, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** «+7 926 304-60-19» из любой записи номера. */
internal fun protoPhone(raw: String): String {
    val digits = raw.filter(Char::isDigit).let { if (it.length == 11 && (it[0] == '7' || it[0] == '8')) it.drop(1) else it }
    return if (digits.length == 10) "+7 ${digits.substring(0, 3)} ${digits.substring(3, 6)}-${digits.substring(6, 8)}-${digits.substring(8)}" else raw
}

/** Десять цифр номера без +7 / 8. */
internal fun protoPhoneDigits(raw: String): String =
    raw.filter(Char::isDigit).let { if (it.length == 11 && (it[0] == '7' || it[0] == '8')) it.drop(1) else it }.take(10)

internal fun protoIsWorkday(date: LocalDate): Boolean = date.dayOfWeek.value < 6

/** Дата через N рабочих дней (суббота и воскресенье не считаются). */
internal fun protoAddWorkdays(from: LocalDate, days: Int): LocalDate {
    var d = from
    var left = days.coerceAtLeast(0)
    while (left > 0) {
        d = d.plusDays(1)
        if (protoIsWorkday(d)) left--
    }
    while (!protoIsWorkday(d)) d = d.plusDays(1)
    return d
}

/** «20 октября» — когда товар под заказ будет готов. */
internal fun protoReadyLabel(productionDays: Int): String =
    protoAddWorkdays(LocalDate.now(), productionDays).format(DateTimeFormatter.ofPattern("d MMMM", ruLocale))

/** Заголовок товара из параметров: одинаковые «Венок ритуальный» различаются качеством и размером. */
internal fun protoTitle(p: ProtoCatalogProduct): String {
    val q = p.quality.takeIf { it.isNotBlank() && it != "—" && !p.name.contains(it, true) }
    val s = p.size.takeIf { it.isNotBlank() && it != "—" && !p.name.contains(it, true) }
    return listOfNotNull(p.name, q).joinToString(" ") + (s?.let { " · $it" } ?: "")
}

internal fun protoValidValue(v: String) = v.isNotBlank() && v != "—"

/** Один фильтр каталога для счётчика «Показать N» и для списка. */
internal data class ProtoCatalogFilter(
    val query: String = "",
    val types: Set<String> = emptySet(),
    val qualities: Set<String> = emptySet(),
    val sizes: Set<String> = emptySet(),
    val availability: Set<String> = emptySet(),
    val minPrice: Int? = null,
    val maxPrice: Int? = null
)

internal fun protoAvailabilityLabel(available: Int) = if (available > 0) "В наличии" else "Под заказ"

internal fun protoMatches(p: ProtoCatalogProduct, f: ProtoCatalogFilter, available: Int, except: String = ""): Boolean {
    return protoSearchMatches(p, f.query) &&
        (except == "type" || f.types.isEmpty() || p.type in f.types) &&
        (except == "quality" || f.qualities.isEmpty() || p.quality in f.qualities) &&
        (except == "size" || f.sizes.isEmpty() || p.size in f.sizes) &&
        (except == "availability" || f.availability.isEmpty() || protoAvailabilityLabel(available) in f.availability) &&
        (f.minPrice == null || p.price >= f.minPrice) &&
        (f.maxPrice == null || p.price <= f.maxPrice)
}

/** Пилюля статуса заказа с цветом по смыслу. */
@Composable
internal fun ProtoStatusPill(status: String) = ProtoPill(status, protoStatusColor(status))

/** Шаги заказа с датами из истории. */
@Composable
internal fun ProtoOrderTimeline(order: ProtoOrder) {
    val statuses = listOf("Получен", "Подтверждён", "Собирается", "Доставляется", "Доставлен")
    val current = statuses.indexOf(order.status)
    val cancelled = order.status == "Отменён"
    val reachedBeforeCancel = if (cancelled) order.history.map { statuses.indexOf(it.status) }.maxOrNull() ?: 0 else current
    Column {
        statuses.forEachIndexed { i, status ->
            val reached = i <= reachedBeforeCancel
            val isCurrent = !cancelled && i == current
            val event = order.history.lastOrNull { it.status == status }
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(14.dp).background(
                        when { isCurrent -> ProtoGold; reached -> ProtoGreen; else -> ProtoPanel2 }, CircleShape
                    )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    status,
                    color = if (reached) ProtoText else ProtoMuted,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (event != null && reached) Text(event.dateTime, color = ProtoMuted, fontSize = 12.sp)
            }
        }
        if (cancelled) {
            val event = order.history.lastOrNull { it.status == "Отменён" }
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).background(ProtoRed, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text("Отменён", color = ProtoRed, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (event != null) Text(event.dateTime, color = ProtoMuted, fontSize = 12.sp)
            }
        }
    }
}

/** Сетка вариантов фильтра: недоступные при текущем выборе — серые и не нажимаются. */
@Composable
internal fun ProtoOptionGrid(title: String, options: List<String>, selected: Set<String>, available: Set<String>, toggle: (String) -> Unit) {
    if (options.isEmpty()) return
    Text(title, color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
    options.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { o ->
                val on = o in selected
                val enabled = on || o in available
                FilterChip(
                    selected = on,
                    onClick = { if (enabled) toggle(o) },
                    enabled = enabled,
                    label = { Text(o, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black,
                        labelColor = ProtoText, disabledLabelColor = ProtoMuted.copy(alpha = 0.5f)
                    )
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** Строка «подпись — значение», где значение может переноситься. */
@Composable
internal fun ProtoInfoLine(label: String, value: String, valueColor: Color = ProtoText) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = ProtoMuted, fontSize = 14.sp, modifier = Modifier.width(130.dp))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ProtoClickableRow(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ProtoGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = ProtoGoldSoft, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = ProtoGold)
    }
}

/** Время заказа «dd.MM.yyyy HH:mm» → миллисекунды для сортировки (новые сверху). */
internal fun protoOrderEpoch(value: String): Long = runCatching {
    java.time.LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrDefault(0L)

/** Строка поиска прямо на экране: ввод, крестик очистки и золотая кнопка «Найти» в правом углу. */
@Composable
internal fun ProtoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String = "Артикул или название",
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val submit = { focus.clearFocus(); onSearch() }
    Surface(color = ProtoPanel, border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(26.dp), modifier = modifier.fillMaxWidth().height(52.dp)) {
        Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = ProtoGold, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = ProtoMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = ProtoText, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(ProtoGold),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { submit() }),
                    modifier = Modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                )
            }
            if (value.isNotEmpty()) Box(Modifier.size(34.dp).clip(CircleShape).clickable { onValueChange("") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, contentDescription = "Очистить", tint = ProtoMuted, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.height(42.dp).clip(RoundedCornerShape(21.dp)).background(ProtoGold).clickable { submit() }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text("Найти", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Круглая кнопка-иконка 42 dp (настройки фильтра и т. п.). */
@Composable
internal fun ProtoRoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, active: Boolean = false) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(if (active) ProtoGold else ProtoPanel).border(1.dp, if (active) ProtoGold else ProtoBorder, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = if (active) Color.Black else ProtoGold, modifier = Modifier.size(22.dp))
    }
}

// ───────────── Правила отмены заказа клиентом ─────────────
// Из наличия — 30 минут, полностью под заказ — 24 часа, смешанный — 1 час с момента оформления.
// После передачи в сборку («Собирается» и дальше) отменить нельзя в любом случае.

internal fun protoOrderKind(lines: List<ProtoOrderLine>): String {
    if (lines.isEmpty() || lines.any { it.stockQty < 0 }) return "stock"
    return when {
        lines.all { it.stockQty >= it.qty } -> "stock"
        lines.all { it.stockQty <= 0 } -> "order"
        else -> "mixed"
    }
}

internal fun protoCancelWindowMs(kind: String): Long = when (kind) {
    "order" -> 24 * 60 * 60_000L
    "mixed" -> 60 * 60_000L
    else -> 30 * 60_000L
}

internal fun protoCancelWindowLabel(kind: String): String = when (kind) {
    "order" -> "24 часа"
    "mixed" -> "1 час"
    else -> "30 минут"
}

internal fun protoCancelRefusal(kind: String): String =
    if (kind == "stock") "Отменить заказ не получится: ваш заказ отправлен в распределительный центр для осуществления доставки."
    else "Отменить заказ не получится: заказ частично собран и отправлен на производство — проделана большая работа."

/** Можно ли сейчас отменить заказ и что сказать клиенту. */
internal data class ProtoCancelState(val allowed: Boolean, val deadline: Long, val kind: String, val message: String)

internal fun protoCancelState(order: ProtoOrder, now: Long = System.currentTimeMillis()): ProtoCancelState {
    val kind = protoOrderKind(order.lines)
    val deadline = protoOrderEpoch(order.dateTime) + protoCancelWindowMs(kind)
    val inWork = order.status !in setOf("Получен", "Подтверждён")
    return when {
        order.status == "Отменён" || order.status == "Доставлен" -> ProtoCancelState(false, deadline, kind, "")
        inWork || now > deadline -> ProtoCancelState(false, deadline, kind, protoCancelRefusal(kind))
        else -> ProtoCancelState(true, deadline, kind, "Отменить можно до " + java.time.Instant.ofEpochMilli(deadline).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm, d MMMM", java.util.Locale("ru"))))
    }
}

/** Текст предупреждения перед отправкой заказа. */
internal fun protoCancelRulesText(kind: String): String =
    "Отменить заказ можно в течение " + protoCancelWindowLabel(kind) + " после оформления, пока его не начали собирать.\n\n" +
        "Сроки отмены:\n• весь заказ из наличия — 30 минут;\n• часть в наличии, часть под заказ — 1 час;\n• весь заказ под заказ — 24 часа.\n\n" +
        "Позже отменить не получится: заказ уходит в распределительный центр на доставку или на производство."

internal fun protoMoneyLong(v: Long): String = String.format(java.util.Locale("ru"), "%,d ₽", v).replace(',', ' ').replace(' ', ' ')

// ───────────── Поиск: регистр, ё/е и ошибочная раскладка ─────────────

private const val EN_KEYS = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`"
private const val RU_KEYS = "йцукенгшщзхъфывапролджэячсмитьбюё"

/** Текст, набранный не в той раскладке: «dtyjr» → «венок», «руддщ» → «hello». */
internal fun protoSwapLayout(text: String, toRussian: Boolean): String = buildString {
    text.lowercase().forEach { ch ->
        val from = if (toRussian) EN_KEYS else RU_KEYS
        val to = if (toRussian) RU_KEYS else EN_KEYS
        val i = from.indexOf(ch)
        append(if (i >= 0) to[i] else ch)
    }
}

private fun norm(s: String) = s.lowercase().replace('ё', 'е').trim()

/** Варианты запроса: как набрано, в русской и в латинской раскладке (артикулы бывают латиницей). */
internal fun protoQueryVariants(query: String): List<String> {
    val q = norm(query)
    if (q.isBlank()) return emptyList()
    return listOf(q, norm(protoSwapLayout(q, true)), norm(protoSwapLayout(q, false))).distinct()
}

internal fun protoSearchMatches(p: ProtoCatalogProduct, query: String): Boolean {
    val variants = protoQueryVariants(query)
    if (variants.isEmpty()) return true
    val hay = listOf(norm(p.name), norm(protoTitle(p)), norm(p.sku), norm(p.type)).joinToString(" | ")
    return variants.any { v -> v.split(' ').filter { it.isNotBlank() }.all { word -> hay.contains(word) } }
}

/** Применённый фильтр главной сохраняется между запусками. */
internal fun protoSaveFilter(prefs: android.content.SharedPreferences, key: String, f: ProtoCatalogFilter) {
    val o = org.json.JSONObject()
        .put("types", org.json.JSONArray(f.types.toList())).put("qualities", org.json.JSONArray(f.qualities.toList()))
        .put("sizes", org.json.JSONArray(f.sizes.toList())).put("availability", org.json.JSONArray(f.availability.toList()))
        .put("min", f.minPrice ?: -1).put("max", f.maxPrice ?: -1)
    prefs.edit().putString(key, o.toString()).apply()
}

internal fun protoLoadFilter(prefs: android.content.SharedPreferences, key: String): ProtoCatalogFilter = runCatching {
    val o = org.json.JSONObject(prefs.getString(key, null) ?: return ProtoCatalogFilter())
    fun set(name: String): Set<String> = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()
    ProtoCatalogFilter("", set("types"), set("qualities"), set("sizes"), set("availability"), o.optInt("min", -1).takeIf { it >= 0 }, o.optInt("max", -1).takeIf { it >= 0 })
}.getOrDefault(ProtoCatalogFilter())

// ───────────── Картинка приветственного экрана ─────────────

internal fun protoWelcomeImageFile(context: android.content.Context) = java.io.File(context.filesDir, "welcome_promo.jpg")

/** Сохраняет выбранную из галереи картинку для экрана «Добро пожаловать», уменьшив до 1600 точек по длинной стороне. */
internal fun protoSaveWelcomeImage(context: android.content.Context, uri: android.net.Uri): Boolean = runCatching {
    val resolver = context.contentResolver
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    val longSide = maxOf(bounds.outWidth, bounds.outHeight)
    require(longSide > 0) { "Не удалось прочитать картинку" }
    var sample = 1
    while (longSide / (sample * 2) >= 1600) sample *= 2
    val bitmap = resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: error("Не удалось прочитать картинку")
    val scale = 1600f / maxOf(bitmap.width, bitmap.height)
    val out = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
    protoWelcomeImageFile(context).outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
    if (out !== bitmap) out.recycle()
    bitmap.recycle()
    true
}.getOrDefault(false)

/** Коротко «было → стало» по позициям заказа для истории изменений. */
internal fun protoOrderDiff(old: List<ProtoOrderLine>, new: List<ProtoOrderLine>): String {
    val parts = mutableListOf<String>()
    (old.map { it.sku } + new.map { it.sku }).distinct().forEach { sku ->
        val a = old.firstOrNull { it.sku == sku }
        val b = new.firstOrNull { it.sku == sku }
        when {
            a == null && b != null -> parts += "добавлен $sku × ${b.qty}"
            a != null && b == null -> parts += "удалён $sku"
            a != null && b != null && a.qty != b.qty -> parts += "$sku: ${a.qty} → ${b.qty} шт."
        }
    }
    return parts.joinToString("; ").ifBlank { "без изменений" }
}

// ───────────── Общий выбор периода (день или промежуток в календаре) ─────────────

/** Период с первого по последний день включительно. */
data class ProtoPeriod(val from: java.time.LocalDate, val to: java.time.LocalDate) {
    fun contains(date: java.time.LocalDate) = !date.isBefore(from) && !date.isAfter(to)
    /** Дата из строки «дд.мм.гггг» или «дд.мм.гггг чч:мм». */
    fun containsText(text: String) = protoParseDate(text.trim().substringBefore(" "))?.let { contains(it) } == true
    val label: String get() {
        val ru = java.util.Locale.forLanguageTag("ru")
        val full = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", ru)
        return when {
            from == to -> from.format(full)
            from.year == to.year && from.month == to.month -> "${from.dayOfMonth}–" + to.format(full)
            from.year == to.year -> from.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", ru)) + " – " + to.format(full)
            else -> from.format(full) + " – " + to.format(full)
        }
    }
    /** Для сохранения и выгрузки: «дд.мм.гггг-дд.мм.гггг». */
    val key: String get() = from.format(protoDateFmt) + "-" + to.format(protoDateFmt)

    companion object {
        fun today() = java.time.LocalDate.now().let { ProtoPeriod(it, it) }
        fun week() = java.time.LocalDate.now().let { ProtoPeriod(it.minusDays(6), it) }
        fun thisMonth() = java.time.LocalDate.now().let { ProtoPeriod(it.withDayOfMonth(1), it) }
        fun lastMonth() = java.time.YearMonth.now().minusMonths(1).let { ProtoPeriod(it.atDay(1), it.atEndOfMonth()) }
        fun fromKey(key: String?): ProtoPeriod? {
            val parts = key?.split("-") ?: return null
            if (parts.size != 2) return null
            val a = protoParseDate(parts[0]) ?: return null
            val b = protoParseDate(parts[1]) ?: return null
            return ProtoPeriod(minOf(a, b), maxOf(a, b))
        }
    }
}

/**
 * Кнопка с выбранным периодом (открывает календарь: первое касание — начало, второе — конец)
 * и быстрые варианты. period == null — «Всё время» (если allowAll).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProtoPeriodBar(period: ProtoPeriod?, onChange: (ProtoPeriod?) -> Unit, allowAll: Boolean = false, maxDate: java.time.LocalDate? = null) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, ProtoGold), shape = RoundedCornerShape(24.dp)) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = ProtoGold)
            Spacer(Modifier.width(8.dp))
            Text(period?.label ?: "Всё время", color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val quick = buildList {
            add("Сегодня" to ProtoPeriod.today()); add("Неделя" to ProtoPeriod.week()); add("Этот месяц" to ProtoPeriod.thisMonth()); add("Прошлый месяц" to ProtoPeriod.lastMonth())
            if (allowAll) add("Всё время" to null)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(quick) { (label, p) ->
                FilterChip(selected = period == p, onClick = { onChange(p) }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText))
            }
        }
    }
    if (open) {
        val zone = java.time.ZoneOffset.UTC
        fun ms(d: java.time.LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
        fun day(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val limit = maxDate?.let { ms(it) }
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = period?.from?.let(::ms),
            initialSelectedEndDateMillis = period?.to?.let(::ms),
            selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = limit == null || utcTimeMillis <= limit }
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(enabled = state.selectedStartDateMillis != null, onClick = {
                    val a = state.selectedStartDateMillis?.let(::day)
                    val b = state.selectedEndDateMillis?.let(::day) ?: a
                    if (a != null && b != null) onChange(ProtoPeriod(minOf(a, b), maxOf(a, b)))
                    open = false
                }) { Text("Выбрать", color = ProtoGold) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Отмена", color = ProtoMuted) } }
        ) {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = { Text("Выберите день или период", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                headline = { Text("Первое касание — начало, второе — конец", fontSize = 13.sp, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)) },
                showModeToggle = false
            )
        }
    }
}
