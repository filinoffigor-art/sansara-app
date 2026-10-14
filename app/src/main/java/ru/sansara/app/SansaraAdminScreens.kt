package ru.sansara.app

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Показатели клиента, посчитанные по заказам и отгрузкам (а не хранимые цифры). */
internal data class ProtoClientStats(val turnover: Int = 0, val orders: Int = 0, val lastOrder: String = "", val debt: Int = 0)

internal fun protoShare(context: android.content.Context, text: String, title: String = "Отправить") {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    runCatching { context.startActivity(Intent.createChooser(send, title)) }
}

@Composable
internal fun ProtoChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(options) { label ->
            FilterChip(
                selected = selected == label,
                onClick = { onSelect(label) },
                label = { Text(label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText)
            )
        }
    }
}

@Composable
internal fun ProtoSectionTitle(text: String, hint: String? = null) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(text, color = ProtoText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (!hint.isNullOrBlank()) Text(hint, color = ProtoMuted, fontSize = 12.sp)
    }
}

@Composable
private fun TodayRow(icon: ImageVector, label: String, count: Int, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (count > 0) accent else ProtoMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (count > 0) ProtoText else ProtoMuted, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(count.toString(), color = if (count > 0) accent else ProtoMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Outlined.ChevronRight, null, tint = ProtoMuted, modifier = Modifier.size(20.dp))
    }
}

// ───────────── Главная администратора ─────────────

@Composable
internal fun ProtoAdminHomeScreen(
    adminName: String,
    newOrders: Int,
    deliveriesToday: Int,
    newRegistrations: Int,
    paymentsPending: Int,
    unreadChats: Int,
    lowStockCount: Int,
    activeClients: Int,
    producedToday: Int,
    ordersToday: Int,
    unreadCount: Int,
    onSearch: () -> Unit,
    onNewOrders: () -> Unit,
    onDeliveriesToday: () -> Unit,
    onRegistrations: () -> Unit,
    onPayments: () -> Unit,
    onChats: () -> Unit,
    onLowStock: () -> Unit,
    onClients: () -> Unit,
    onOrders: () -> Unit,
    onProduction: () -> Unit,
    onStock: () -> Unit,
    onCatalog: () -> Unit,
    onSettings: () -> Unit,
    onOnline: () -> Unit,
    onWorkshop: () -> Unit,
    onAttendance: () -> Unit,
    onReports: () -> Unit,
    onDashboard: () -> Unit,
    onNotifications: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(containerColor = Color.Transparent, bottomBar = { ProtoAdminBottomBar(ProtoScreen.AdminHome, onHome = {}, onClients, onOrders, onStock, onProfile = onSettings) }) { pad ->
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ProtoBrandHeader(unreadCount = unreadCount, onBell = onNotifications, edge = 0.dp) }
                item {
                    Text("Здравствуйте" + if (adminName.isNotBlank()) ", $adminName" else "", color = ProtoText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Администратор · " + currentDateLong(), color = ProtoGoldSoft, fontSize = 14.sp)
                }
                item { ProtoSearchBar("", onSearch, "Поиск по клиентам, заказам, товарам") }
                item {
                    ProtoSectionCard {
                        Text("Сегодня", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        TodayRow(Icons.Outlined.ReceiptLong, "Новые заказы — подтвердить", newOrders, ProtoGold, onNewOrders)
                        TodayRow(Icons.Outlined.LocalShipping, "Получение сегодня", deliveriesToday, ProtoGold, onDeliveriesToday)
                        TodayRow(Icons.Outlined.PersonAdd, "Заявки на регистрацию", newRegistrations, ProtoGold, onRegistrations)
                        TodayRow(Icons.Outlined.Payments, "Оплаты ждут подтверждения", paymentsPending, ProtoGold, onPayments)
                        TodayRow(Icons.Outlined.ChatBubbleOutline, "Непрочитанные сообщения", unreadChats, ProtoGold, onChats)
                        TodayRow(Icons.Outlined.Warning, "Низкие остатки", lowStockCount, ProtoOrange, onLowStock)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProtoMetricCard("Заказы сегодня", ordersToday.toString(), "", Modifier.weight(1f), onOrders)
                        ProtoMetricCard("Выпуск сегодня", producedToday.toString(), "шт.", Modifier.weight(1f), onProduction)
                        ProtoMetricCard("Клиенты", activeClients.toString(), "активные", Modifier.weight(1f), onClients)
                    }
                }
                item { ProtoSectionTitle("Разделы") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProtoQuickButton("Чаты", Icons.Outlined.ChatBubbleOutline, Modifier.weight(1f), onChats)
                        ProtoQuickButton("Каталог", Icons.Outlined.Inventory2, Modifier.weight(1f), onCatalog)
                        ProtoQuickButton("Производство", Icons.Outlined.Factory, Modifier.weight(1f), onProduction)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProtoQuickButton("Задание в цех", Icons.Outlined.Assignment, Modifier.weight(1f), onWorkshop)
                        ProtoQuickButton("Табель", Icons.Outlined.EventAvailable, Modifier.weight(1f), onAttendance)
                        ProtoQuickButton("Отчёты", Icons.Outlined.Assessment, Modifier.weight(1f), onReports)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProtoQuickButton(if (paymentsPending > 0) "Оплаты · $paymentsPending" else "Оплаты", Icons.Outlined.Payments, Modifier.weight(1f), onPayments)
                        ProtoQuickButton("Денежный поток", Icons.Outlined.ShowChart, Modifier.weight(1f), onDashboard)
                        ProtoQuickButton("Кто в сети", Icons.Outlined.Wifi, Modifier.weight(1f), onOnline)
                    }
                }
            }
        }
    }
}

// ───────────── Клиенты ─────────────

private val clientSorts = listOf("По обороту", "По имени", "По последнему заказу", "По долгу")

@Composable
internal fun ProtoAdminClientsScreen(clients: List<ProtoClient>, stats: Map<String, ProtoClientStats>, onBack: () -> Unit, onOpen: (ProtoClient) -> Unit) {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Все") }
    var sort by remember { mutableStateOf(clientSorts.first()) }
    var sortOpen by remember { mutableStateOf(false) }
    val filters = listOf("Все", "Активный", "Оптовик", "VIP", "Приостановлен", "Онлайн", "С долгом")
    val filtered = clients.filter { c ->
        val qq = q.trim()
        val queryOk = qq.isBlank() || c.name.contains(qq, true) || c.contact.contains(qq, true) || c.phone.filter(Char::isDigit).contains(qq.filter(Char::isDigit).ifBlank { "§" }) || c.id.contains(qq, true) || c.city.contains(qq, true)
        val filterOk = when (filter) {
            "Все" -> true
            "Онлайн" -> c.online
            "С долгом" -> (stats[c.id]?.debt ?: 0) > 0
            else -> c.status == filter
        }
        queryOk && filterOk
    }.let { list ->
        when (sort) {
            "По имени" -> list.sortedBy { it.name.lowercase() }
            "По последнему заказу" -> list.sortedByDescending { protoOrderEpoch(stats[it.id]?.lastOrder.orEmpty()) }
            "По долгу" -> list.sortedByDescending { stats[it.id]?.debt ?: 0 }
            else -> list.sortedByDescending { stats[it.id]?.turnover ?: 0 }
        }
    }
    ProtoScaffold("Клиенты", protoPlural(clients.size, "клиент", "клиента", "клиентов") + " · оборот за " + currentMonthLabel().lowercase(ruLocale), onBack, action = {
        Box {
            IconButton(onClick = { sortOpen = true }) { Icon(Icons.Outlined.Sort, contentDescription = "Сортировка", tint = ProtoGold) }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                clientSorts.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { sort = s; sortOpen = false }) }
            }
        }
    }) {
        item { ProtoField(q, { q = it }, "Поиск: название, контакт, телефон, город") }
        item { ProtoChipRow(filters, filter) { filter = it } }
        item { Text(sort + " · найдено " + filtered.size, color = ProtoMuted, fontSize = 12.sp) }
        if (filtered.isEmpty()) item { ProtoEmptyState(Icons.Outlined.PersonSearch, "Клиенты не найдены", "Измените поиск или фильтр") }
        else items(filtered, key = { it.id }) { c ->
            val st = stats[c.id] ?: ProtoClientStats()
            Card(colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, ProtoBorder), modifier = Modifier.fillMaxWidth().clickable { onOpen(c) }) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(if (c.online) ProtoGreen else ProtoMuted.copy(alpha = .5f), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(c.status + " · " + protoPlural(st.orders, "заказ", "заказа", "заказов") + if (c.discount > 0) " · скидка ${c.discount}%" else "", color = ProtoMuted, fontSize = 12.sp)
                        Text(if (st.lastOrder.isNotBlank()) "Последний заказ " + st.lastOrder.substringBefore(" ") else "Заказов ещё не было", color = ProtoMuted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(protoMoney(st.turnover), color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                        if (st.debt > 0) Text("долг " + protoMoney(st.debt), color = ProtoRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProtoAdminClientScreen(
    client: ProtoClient?,
    stats: ProtoClientStats,
    history: List<ClientHistoryEntity>,
    users: List<Pair<SansaraCompanyContact, String>>,
    retailCustomers: List<SansaraAgentCustomer>,
    retailMarkups: List<SansaraAgentMarkup>,
    shipments: List<ShipmentEntity>,
    clientOrders: List<ProtoOrder>,
    typeOf: (String) -> String,
    onBack: () -> Unit,
    onStatus: (String) -> Unit,
    onSuspend: (String) -> Unit,
    onRestore: () -> Unit,
    onToggleOrdering: () -> Unit,
    onDiscount: (Int) -> Unit,
    onCall: () -> Unit,
    onChat: () -> Unit,
    onReissueCode: () -> Unit,
    onOpenOrder: (ProtoOrder) -> Unit
) {
    val c = client ?: return
    val context = LocalContext.current
    var confirmReissue by remember { mutableStateOf(false) }
    var suspendDialog by remember { mutableStateOf(false) }
    var suspendReason by remember { mutableStateOf("") }
    val suspended = c.status == "Приостановлен"
    val lastSuspend = history.firstOrNull { it.text.startsWith("Приостановлен") }
    if (confirmReissue) AlertDialog(
        onDismissRequest = { confirmReissue = false }, containerColor = ProtoPanel,
        title = { Text("Выдать новый код?", color = ProtoText) },
        text = { Text("Старый код основного пользователя перестанет работать. Новый код нужно будет передать клиенту.", color = ProtoMuted) },
        confirmButton = { TextButton(onClick = { confirmReissue = false; onReissueCode() }) { Text("Выдать", color = ProtoGold) } },
        dismissButton = { TextButton(onClick = { confirmReissue = false }) { Text("Отмена", color = ProtoMuted) } }
    )
    if (suspendDialog) AlertDialog(
        onDismissRequest = { suspendDialog = false }, containerColor = ProtoPanel,
        title = { Text("Приостановить клиента?", color = ProtoText) },
        text = {
            Column {
                Text("Клиент сможет смотреть каталог и заказы, но не сможет оформлять новые. Укажите причину — она сохранится в истории.", color = ProtoMuted, fontSize = 13.sp)
                ProtoField(suspendReason, { suspendReason = it.take(200) }, "Причина")
                listOf("Просрочка оплаты", "По просьбе клиента", "Проверка данных").forEach { r ->
                    Text(r, color = ProtoGold, fontSize = 13.sp, modifier = Modifier.clickable { suspendReason = r }.padding(vertical = 4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { suspendDialog = false; onSuspend(suspendReason.trim()); suspendReason = "" }, enabled = suspendReason.isNotBlank()) { Text("Приостановить", color = if (suspendReason.isNotBlank()) ProtoRed else ProtoMuted) } },
        dismissButton = { TextButton(onClick = { suspendDialog = false }) { Text("Отмена", color = ProtoMuted) } }
    )

    ProtoScaffold("Карточка клиента", c.id + " · клиент с " + c.registeredAt, onBack) {
        item {
            ProtoSectionCard {
                Text(c.name, color = ProtoText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(c.clientType + if (c.city.isNotBlank()) " · " + c.city else "", color = ProtoMuted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ProtoInfoRow("Контакт", c.contact.ifBlank { "—" })
                ProtoInfoRow("Телефон", protoPhone(c.phone).ifBlank { "—" })
                if (c.email.isNotBlank()) ProtoInfoRow("E-mail", c.email)
                if (c.address.isNotBlank()) Text(c.address, color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, ProtoGold)) { Icon(Icons.Outlined.Call, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text("Позвонить", color = ProtoGold) }
                    OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, ProtoGold)) { Icon(Icons.Outlined.ChatBubbleOutline, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text("Написать", color = ProtoGold) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("Оборот", protoMoney(stats.turnover), "этот месяц", Modifier.weight(1f)) {}
                ProtoMetricCard("Заказов", stats.orders.toString(), "всего", Modifier.weight(1f)) {}
                ProtoMetricCard("Долг", protoMoney(stats.debt), if (stats.debt > 0) "не оплачено" else "нет", Modifier.weight(1f)) {}
            }
        }
        if (suspended) item {
            Surface(color = ProtoRed.copy(alpha = .12f), border = BorderStroke(1.dp, ProtoRed.copy(alpha = .5f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Клиент приостановлен", color = ProtoRed, fontWeight = FontWeight.Bold)
                    Text(lastSuspend?.text?.removePrefix("Приостановлен")?.trimStart(':', ' ')?.ifBlank { "Причина не указана" } ?: "Причина не указана", color = ProtoText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(10.dp))
                    ProtoPrimaryButton("Восстановить доступ", onRestore)
                }
            }
        }
        item {
            ProtoSectionCard {
                Text("Статус", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf("Активный", "Оптовик", "VIP").forEach { status ->
                        FilterChip(
                            selected = c.status == status, enabled = !suspended,
                            onClick = { onStatus(status) },
                            label = { Text(status, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Скидка: ${c.discount}%", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    items(listOf(0, 3, 5, 7, 10, 15, 20)) { d ->
                        FilterChip(
                            selected = c.discount == d, onClick = { onDiscount(d) }, label = { Text("$d%", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                ProtoSwitchRow("Может оформлять заказы", c.orderingEnabled) { onToggleOrdering() }
                if (!suspended) TextButton(onClick = { suspendDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Приостановить клиента…", color = ProtoRed) }
                Text("Изменения сохраняются сразу и попадают в историю.", color = ProtoMuted, fontSize = 12.sp)
            }
        }
        item { ProtoSectionTitle("Пользователи и коды входа", "Вход: телефон пользователя + код") }
        if (users.isEmpty()) item { ProtoSectionCard { Text("Пользователей пока нет", color = ProtoMuted) } }
        items(users, key = { it.first.userId }) { (u, code) ->
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.name, color = ProtoText, fontWeight = FontWeight.SemiBold)
                        Text(protoPhone(u.phone).ifBlank { "телефон не указан" }, color = ProtoMuted, fontSize = 12.sp)
                    }
                    Text(code.ifBlank { "отключён" }, color = if (code.isBlank()) ProtoMuted else ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
                if (code.isNotBlank()) TextButton(onClick = {
                    protoShare(context, "SANSARA: вход в приложение — телефон " + protoPhone(u.phone) + ", код " + code + ". Откройте приложение и нажмите «Войти».", "Отправить код")
                }) { Icon(Icons.Outlined.Share, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text("Отправить код", color = ProtoGold) }
            }
        }
        item { OutlinedButton(onClick = { confirmReissue = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, ProtoBorder)) { Text("Выдать новый код основному пользователю", color = ProtoText) } }
        item { ProtoClientFinanceBlock(c.id, shipments) }
        if (clientOrders.isNotEmpty()) {
            item { ProtoSectionTitle("Последние заказы") }
            items(clientOrders.sortedByDescending { protoOrderEpoch(it.dateTime) }.take(5), key = { "o-" + it.id }) { o -> ProtoOrderRow(o) { onOpenOrder(o) } }
        }
        item { ProtoClientAnalyticsBlock(clientOrders, typeOf) }
        if (retailCustomers.isNotEmpty()) {
            item { ProtoSectionTitle("Клиенты агента · наценки") }
            items(retailCustomers, key = { it.id }) { customer ->
                val marks = retailMarkups.filter { it.customerId == customer.id }.sortedBy { it.category }
                ProtoSectionCard {
                    Text(customer.fullName, color = ProtoText, fontWeight = FontWeight.Bold)
                    Text(protoPhone(customer.phone), color = ProtoMuted, fontSize = 12.sp)
                    if (marks.isEmpty()) Text("Индивидуальная наценка не задана", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                    else marks.forEach { m -> ProtoInfoRow(m.category, m.markupPct.toString() + "%") }
                }
            }
        }
        item { ProtoSectionTitle("История изменений") }
        if (history.isEmpty()) item { Text("Изменений пока не было", color = ProtoMuted, fontSize = 13.sp) }
        items(history.take(15), key = { it.id }) { h ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(protoChatTime(h.createdAt), color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.width(92.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.text, color = ProtoText, fontSize = 13.sp)
                    Text(h.actor, color = ProtoMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

// ───────────── Заказы ─────────────

@Composable
internal fun ProtoAdminOrdersScreen(orders: List<ProtoOrder>, initialStatus: String, initialDate: String, onBack: () -> Unit, onOpen: (ProtoOrder) -> Unit) {
    var q by remember { mutableStateOf("") }
    var status by remember(initialStatus) { mutableStateOf(initialStatus) }
    var dateFilter by remember(initialDate) { mutableStateOf(initialDate) }
    val statuses = listOf("Все", "Получен", "Подтверждён", "Собирается", "Доставляется", "Доставлен", "Отменён")
    val today = LocalDate.now()
    val filtered = orders.filter { o ->
        val qq = q.trim()
        val queryOk = qq.isBlank() || o.id.contains(qq, true) || o.clientName.contains(qq, true) || o.retailCustomerName.contains(qq, true)
        val statusOk = status == "Все" || o.status == status
        val d = protoParseDate(o.deliveryDate)
        val dateOk = when (dateFilter) {
            "Сегодня" -> d == today
            "Завтра" -> d == today.plusDays(1)
            "Неделя" -> d != null && !d.isBefore(today) && d.isBefore(today.plusDays(7))
            "Просрочены" -> d != null && d.isBefore(today) && o.status !in setOf("Доставлен", "Отменён")
            else -> true
        }
        queryOk && statusOk && dateOk
    }.sortedByDescending { protoOrderEpoch(it.dateTime) }
    ProtoScaffold("Заказы", protoPlural(filtered.size, "заказ", "заказа", "заказов") + " · " + protoMoney(filtered.filter { it.status != "Отменён" }.sumOf { it.total }), onBack) {
        item { ProtoField(q, { q = it }, "Поиск по номеру или клиенту") }
        item { ProtoChipRow(statuses, status) { status = it } }
        item { ProtoChipRow(listOf("Все даты", "Сегодня", "Завтра", "Неделя", "Просрочены"), dateFilter) { dateFilter = it } }
        if (filtered.isEmpty()) item { ProtoEmptyState(Icons.Outlined.ReceiptLong, "Заказов нет", "Измените фильтр статуса или даты") }
        else items(filtered, key = { it.id }) { o -> ProtoOrderRow(o) { onOpen(o) } }
    }
}

// ───────────── Каталог ─────────────

@Composable
internal fun ProtoAdminCatalogScreen(
    products: List<ProtoCatalogProduct>,
    physicalOf: (ProtoCatalogProduct) -> Int,
    availableOf: (ProtoCatalogProduct) -> Int,
    syncStatus: String,
    lastSync: String,
    syncing: Boolean,
    onSync: () -> Unit,
    onBack: () -> Unit,
    onOpen: (ProtoCatalogProduct) -> Unit
) {
    var q by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Все") }
    var flag by remember { mutableStateOf("Все") }
    val types = listOf("Все") + products.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()
    val flags = listOf("Все", "Нет в наличии", "Без фото", "Без цены", "Сняты с публикации")
    val shown = products.filter { p ->
        val qq = q.trim()
        (qq.isBlank() || p.sku.contains(qq, true) || p.name.contains(qq, true)) &&
            (type == "Все" || p.type == type) &&
            when (flag) {
                "Нет в наличии" -> availableOf(p) <= 0 && p.status != PRODUCT_UNPUBLISHED
                "Без фото" -> p.imageUrl.isBlank()
                "Без цены" -> p.price <= 0
                "Сняты с публикации" -> p.status == PRODUCT_UNPUBLISHED
                else -> true
            }
    }.sortedBy { it.sku }
    ProtoScaffold("Каталог", protoPlural(products.count { it.status != PRODUCT_UNPUBLISHED }, "позиция", "позиции", "позиций") + " в продаже", onBack) {
        item {
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Синхронизация с Tilda", color = ProtoText, fontWeight = FontWeight.SemiBold)
                        Text(syncStatus, color = if (syncStatus.startsWith("Ошибка")) ProtoRed else ProtoMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Обновлено: " + lastSync.ifBlank { "ещё не выполнялось" }, color = ProtoMuted, fontSize = 12.sp)
                    }
                    if (syncing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = ProtoGold)
                    else IconButton(onClick = onSync) { Icon(Icons.Outlined.Sync, contentDescription = "Обновить", tint = ProtoGold) }
                }
                Text("Цены, названия и фото меняются в Tilda; склад и заказы приложение не перезаписывает.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item { ProtoField(q, { q = it }, "Поиск по артикулу или названию") }
        item { ProtoChipRow(types, type) { type = it } }
        item { ProtoChipRow(flags, flag) { flag = it } }
        item { Text("Найдено: " + shown.size, color = ProtoMuted, fontSize = 12.sp) }
        if (shown.isEmpty()) item { ProtoEmptyState(Icons.Outlined.Inventory2, "Ничего не найдено", "Измените поиск или фильтр") }
        items(shown.take(300), key = { it.sku }) { p ->
            ProtoSectionCard(Modifier.clickable { onOpen(p) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProtoProductImage(p, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        ProtoSkuText(p.sku, extra = " · " + protoMoney(p.price), fontSize = 12)
                        val issues = listOfNotNull(
                            "снят с публикации".takeIf { p.status == PRODUCT_UNPUBLISHED },
                            "нет фото".takeIf { p.imageUrl.isBlank() },
                            "нет цены".takeIf { p.price <= 0 }
                        )
                        if (issues.isNotEmpty()) Text(issues.joinToString(" · "), color = ProtoOrange, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val av = availableOf(p)
                        Text("$av шт.", color = if (av > 0) ProtoGreen else ProtoMuted, fontWeight = FontWeight.Bold)
                        Text("склад ${physicalOf(p)}", color = ProtoMuted, fontSize = 12.sp)
                    }
                }
            }
        }
        if (shown.size > 300) item { Text("Показаны первые 300 — уточните поиск", color = ProtoMuted, fontSize = 12.sp) }
    }
}

@Composable
internal fun ProtoAdminProductScreen(
    product: ProtoCatalogProduct?,
    physical: Int,
    reserved: Int,
    rate: Int,
    orders: List<ProtoOrder>,
    canAdjust: Boolean,
    onBack: () -> Unit,
    onAdjust: (Int, String) -> Unit,
    onOpenOrder: (ProtoOrder) -> Unit
) {
    val p = product ?: return
    var adjust by remember { mutableStateOf(false) }
    var deltaText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val available = (physical - reserved).coerceAtLeast(0)
    val withSku = orders.filter { o -> o.lines.any { it.sku == p.sku } }.sortedByDescending { protoOrderEpoch(it.dateTime) }
    ProtoScaffold("Товар", "Арт. " + p.sku, onBack) {
        item {
            ProtoSectionCard {
                ProtoProductImage(p, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.height(10.dp))
                Text(protoTitle(p), color = ProtoText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(p.name, color = ProtoMuted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ProtoInfoRow("Тип", p.type.ifBlank { "—" })
                if (protoValidValue(p.size)) ProtoInfoRow("Размер", p.size)
                if (protoValidValue(p.quality)) ProtoInfoRow("Исполнение", p.quality)
                ProtoInfoRow("Оптовая цена", protoMoney(p.price))
                if (p.retailPrice > 0) ProtoInfoRow("Розничная цена", protoMoney(p.retailPrice))
                ProtoInfoRow("Срок изготовления", protoPlural(p.productionDays, "рабочий день", "рабочих дня", "рабочих дней"))
                ProtoInfoRow("Ставка сборки", if (rate > 0) protoMoney(rate) + " / шт." else "не задана")
                ProtoInfoRow("Публикация", if (p.status == PRODUCT_UNPUBLISHED) "снят в Tilda" else "в продаже")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("На складе", physical.toString(), "шт.", Modifier.weight(1f)) {}
                ProtoMetricCard("В резерве", reserved.toString(), "шт.", Modifier.weight(1f)) {}
                ProtoMetricCard("Доступно", available.toString(), "шт.", Modifier.weight(1f)) {}
            }
        }
        if (canAdjust) item { ProtoSecondaryButton("Корректировать остаток", { adjust = true; deltaText = ""; reason = "" }) }
        item { ProtoSectionTitle("Заказы с этим товаром", if (withSku.isEmpty()) "Пока не заказывали" else null) }
        items(withSku.take(10), key = { "ps-" + it.id }) { o -> ProtoOrderRow(o) { onOpenOrder(o) } }
    }
    if (adjust) AlertDialog(
        onDismissRequest = { adjust = false }, containerColor = ProtoPanel,
        title = { Text("Корректировка остатка", color = ProtoText) },
        text = {
            Column {
                Text("Сейчас на складе: $physical шт.", color = ProtoGoldSoft)
                ProtoField(deltaText, { v -> deltaText = v.filter { it.isDigit() || it == '-' }.take(6) }, "Изменение, шт. (например −2 или 5)", KeyboardType.Number)
                ProtoField(reason, { reason = it.take(120) }, "Причина")
                val d = deltaText.toIntOrNull() ?: 0
                if (d != 0) Text("Станет: " + (physical + d).coerceAtLeast(0) + " шт.", color = ProtoMuted, fontSize = 13.sp)
            }
        },
        confirmButton = {
            val d = deltaText.toIntOrNull() ?: 0
            TextButton(onClick = { onAdjust(d, reason.trim()); adjust = false }, enabled = d != 0 && reason.isNotBlank()) { Text("Провести", color = ProtoGold) }
        },
        dismissButton = { TextButton(onClick = { adjust = false }) { Text("Отмена", color = ProtoMuted) } }
    )
}

// ───────────── Настройки ─────────────

@Composable
internal fun ProtoAdminSettingsMenuScreen(syncStatus: String, lastSync: String, lowStockThreshold: Int, pickupCount: Int, onBack: () -> Unit, onOpen: (String) -> Unit, onCall: () -> Unit, onLogout: () -> Unit) {
    val rows = listOf(
        Triple("Профиль компании", Icons.Outlined.Business, "Данные SANSARA и контакты"),
        Triple("Администраторы", Icons.Outlined.AdminPanelSettings, "Личные коды входа администраторов"),
        Triple("Сборщицы", Icons.Outlined.Badge, "Справочник производства"),
        Triple("Ставки сборки", Icons.Outlined.Payments, "Оплата сборщицам за штуку"),
        Triple("Точки самовывоза", Icons.Outlined.Store, if (pickupCount > 0) protoPlural(pickupCount, "точка", "точки", "точек") else "Не заданы — клиент вводит адрес сам"),
        Triple("Задание в цех", Icons.Outlined.Assignment, "План и факт цеха"),
        Triple("Табель рабочего времени", Icons.Outlined.EventAvailable, "Кто и когда выходил"),
        Triple("Отчёты", Icons.Outlined.Assessment, "Показатели за период"),
        Triple("Клиенты", Icons.Outlined.PersonSearch, "Доступ, статусы и скидки"),
        Triple("Каталог и синхронизация", Icons.Outlined.Sync, "$syncStatus · $lastSync"),
        Triple("Telegram интеграция", Icons.Outlined.Send, "Заказы через серверный прокси"),
        Triple("Порог низких остатков", Icons.Outlined.Warning, "Сейчас: $lowStockThreshold шт."),
        Triple("Экспорт данных", Icons.Outlined.FileDownload, "CSV за период"),
        Triple("О приложении", Icons.Outlined.Info, "SANSARA · версия ${BuildConfig.VERSION_NAME}")
    )
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false }, containerColor = ProtoPanel,
        title = { Text("Выйти из аккаунта?", color = ProtoText) },
        text = { Text("Для входа понадобятся телефон и код.", color = ProtoMuted) },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Выйти", color = ProtoRed) } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Отмена", color = ProtoMuted) } }
    )
    ProtoScaffold("Профиль и настройки", "Администратор", onBack) {
        items(rows) { (title, icon, subtitle) ->
            Card(colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { onOpen(title) }) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(ProtoPanel2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ProtoGold) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = ProtoText, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = ProtoMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = ProtoGold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { onCall() }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(ProtoGreen.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Phone, null, tint = ProtoGreen) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Телефон компании", color = ProtoMuted, fontSize = 12.sp)
                        Text(protoPhone(BuildConfig.ADMIN_PHONE), color = ProtoGoldSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { TextButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Logout, null, tint = ProtoMuted); Spacer(Modifier.width(6.dp)); Text("Выйти из аккаунта", color = ProtoMuted) } }
    }
}

@Composable
internal fun ProtoAdminSettingsDetailScreen(
    section: String, threshold: Int,
    tildaUrl: String, syncStatus: String, lastSync: String, syncing: Boolean, backendStatus: String,
    telegramEnabled: Boolean, telegramRetryEnabled: Boolean, telegramApiUrl: String, telegramTemplate: String, telegramLastLog: String,
    pickupPoints: List<String>,
    products: List<ProtoCatalogProduct>,
    rates: Map<String, Int>,
    onBack: () -> Unit, onThreshold: (Int) -> Unit,
    onTildaUrl: (String) -> Unit, onSync: () -> Unit,
    onTelegramSave: (Boolean, Boolean, String, String) -> Unit,
    onPickupSave: (List<String>) -> Unit,
    onRate: (List<String>, Int) -> Unit
) {
    ProtoScaffold(section, null, onBack) {
        when (section) {
            "Профиль компании" -> item {
                ProtoSectionCard {
                    ProtoInfoRow("Компания", "SANSARA")
                    ProtoInfoRow("Телефон", protoPhone(BuildConfig.ADMIN_PHONE))
                    ProtoInfoRow("Каталог", "sansararitual.ru")
                    Text("Телефон компании задаётся при сборке приложения и показывается клиентам на экранах входа и поддержки.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            "Каталог и синхронизация" -> item {
                ProtoSectionCard {
                    Text("Каталог Tilda", color = ProtoText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Синхронизация обновляет карточки, цены, категории и фото. Склад, резерв, заказы и производство не перезаписываются.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                    ProtoField(tildaUrl, onTildaUrl, "YML-ссылка каталога Tilda")
                    Button(onClick = onSync, enabled = !syncing && tildaUrl.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) {
                        if (syncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black) else Icon(Icons.Outlined.Sync, null, tint = Color.Black)
                        Spacer(Modifier.width(7.dp))
                        Text(if (syncing) "Синхронизация…" else "Синхронизировать каталог", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Text(syncStatus, color = if (syncStatus.startsWith("Ошибка")) ProtoRed else ProtoGreen, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
                    Text("Последнее обновление: $lastSync", color = ProtoMuted, fontSize = 12.sp)
                    Text("Сервер событий: $backendStatus", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            "Telegram интеграция" -> item {
                var enabled by remember(telegramEnabled) { mutableStateOf(telegramEnabled) }
                var retry by remember(telegramRetryEnabled) { mutableStateOf(telegramRetryEnabled) }
                var apiUrl by remember(telegramApiUrl) { mutableStateOf(telegramApiUrl) }
                var template by remember(telegramTemplate) { mutableStateOf(telegramTemplate) }
                ProtoSectionCard {
                    Text("Telegram-бот", color = ProtoText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Заказы отправляются только через серверный прокси. BOT_TOKEN и CHAT_ID не хранятся в приложении.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                    ProtoSwitchRow("Отправлять заказы в Telegram", enabled) { enabled = it }
                    ProtoField(apiUrl, { apiUrl = it }, "HTTPS-адрес серверного прокси")
                    ProtoSwitchRow("Повторять отправку при сбое", retry) { retry = it }
                    OutlinedTextField(value = template, onValueChange = { template = it }, label = { Text("Шаблон сообщения") }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), minLines = 5, maxLines = 9, colors = protoFieldColors())
                    Text("Переменные: {number}, {client}, {items}, {total}, {contact}, {date}, {time}", color = ProtoMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    ProtoPrimaryButton("Сохранить настройки", { onTelegramSave(enabled, retry, apiUrl, template) }, enabled = !enabled || apiUrl.startsWith("https://"))
                    Spacer(Modifier.height(8.dp))
                    Text("Последняя отправка: $telegramLastLog", color = ProtoMuted, fontSize = 12.sp)
                }
            }
            "Порог низких остатков" -> item {
                ProtoSectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        ProtoQtyButton(Icons.Outlined.Remove, { onThreshold(threshold - 1) })
                        Text("$threshold шт.", color = ProtoGoldSoft, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                        ProtoQtyButton(Icons.Outlined.Add, { onThreshold(threshold + 1) })
                    }
                    Text("Позиции с доступным остатком не больше порога попадают в «Низкие остатки».", color = ProtoMuted, fontSize = 12.sp)
                }
            }
            "Точки самовывоза" -> item {
                var text by remember(pickupPoints) { mutableStateOf(pickupPoints.joinToString("\n")) }
                ProtoSectionCard {
                    Text("Адреса, из которых клиент выбирает при самовывозе. Одна строка — одна точка.", color = ProtoMuted, fontSize = 13.sp)
                    OutlinedTextField(value = text, onValueChange = { text = it.take(1000) }, label = { Text("Точки самовывоза") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), minLines = 4, maxLines = 8, colors = protoFieldColors())
                    ProtoPrimaryButton("Сохранить", { onPickupSave(text.lines().map { it.trim() }.filter { it.isNotBlank() }) })
                }
            }
            "Ставки сборки" -> {
                item { Text("Ставка — сколько получает сборщица за одну штуку. Производство видит ставку, но не меняет её.", color = ProtoMuted, fontSize = 13.sp) }
                val groups = products.groupBy { it.type.ifBlank { "Прочее" } }.toSortedMap()
                groups.forEach { (type, list) ->
                    item(key = "rg-$type") {
                        var groupRate by remember(type) { mutableStateOf("") }
                        val rated = list.count { (rates[it.sku] ?: 0) > 0 }
                        ProtoSectionCard {
                            Text(type, color = ProtoText, fontWeight = FontWeight.Bold)
                            Text("${list.size} артикулов · ставка задана у $rated", color = if (rated < list.size) ProtoOrange else ProtoMuted, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = groupRate, onValueChange = { groupRate = it.filter(Char::isDigit).take(5) }, label = { Text("Ставка для всех, ₽") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors())
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { groupRate.toIntOrNull()?.let { onRate(list.map { it.sku }, it) }; groupRate = "" }, enabled = groupRate.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) { Text("Применить", color = Color.Black) }
                            }
                        }
                    }
                    items(list.sortedBy { it.sku }.take(80), key = { "r-" + it.sku }) { p ->
                        var v by remember(p.sku, rates[p.sku]) { mutableStateOf((rates[p.sku] ?: 0).takeIf { it > 0 }?.toString().orEmpty()) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(protoTitle(p), color = ProtoText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                ProtoSkuText(p.sku, fontSize = 12)
                            }
                            OutlinedTextField(value = v, onValueChange = { v = it.filter(Char::isDigit).take(5); v.toIntOrNull()?.let { r -> onRate(listOf(p.sku), r) } }, singleLine = true, modifier = Modifier.width(96.dp), suffix = { Text("₽") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors())
                        }
                    }
                }
            }
            else -> item {
                ProtoSectionCard {
                    ProtoInfoRow("Приложение", "SANSARA")
                    ProtoInfoRow("Версия", BuildConfig.VERSION_NAME)
                    Text("B2B-приложение для оптовых клиентов, производства и администратора SANSARA.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

// ───────────── Требует внимания: заявки ─────────────

@Composable
internal fun ProtoAttentionScreen(
    regs: List<ProtoRegistration>,
    blocked: List<ProtoClient>,
    picking: List<ProtoOrder>,
    lowStock: List<ProtoCatalogProduct>,
    stockValue: (ProtoCatalogProduct) -> Int,
    onBack: () -> Unit,
    onClient: (ProtoClient) -> Unit,
    onOrder: (ProtoOrder) -> Unit,
    onApprove: (ProtoRegistration, String, Int) -> Unit,
    onReject: (ProtoRegistration, String) -> Unit,
    onLowStock: () -> Unit
) {
    val context = LocalContext.current
    var approveReg by remember { mutableStateOf<ProtoRegistration?>(null) }
    var rejectReg by remember { mutableStateOf<ProtoRegistration?>(null) }
    var status by remember { mutableStateOf("Активный") }
    var discount by remember { mutableIntStateOf(0) }
    var reason by remember { mutableStateOf("") }
    ProtoScaffold("Требует внимания", "Задачи остаются здесь до обработки", onBack) {
        item { ProtoSectionTitle("Заявки на регистрацию", if (regs.isEmpty()) "Новых заявок нет" else null) }
        items(regs, key = { it.id }) { r ->
            ProtoSectionCard {
                Text(r.organization, color = ProtoText, fontWeight = FontWeight.Bold)
                Text(r.type + (if (r.inn.isNotBlank()) " · ИНН " + r.inn else "") + " · " + r.city, color = ProtoMuted, fontSize = 12.sp)
                Text(listOf(r.lastName, r.firstName, r.middleName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { r.contact1 } + " · " + protoPhone(r.phone1), color = ProtoText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                if (r.email.isNotBlank()) Text(r.email, color = ProtoMuted, fontSize = 12.sp)
                if (r.address.isNotBlank()) Text(r.address, color = ProtoMuted, fontSize = 12.sp)
                if (r.contact2.isNotBlank()) Text("Второй контакт: " + r.contact2 + " · " + protoPhone(r.phone2), color = ProtoMuted, fontSize = 12.sp)
                Text("Заявка от " + r.createdAt, color = ProtoMuted, fontSize = 12.sp)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { approveReg = r; status = "Активный"; discount = 0 }, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), modifier = Modifier.weight(1f)) { Text("Одобрить…", color = Color.Black) }
                    OutlinedButton(onClick = { rejectReg = r; reason = "" }, border = BorderStroke(1.dp, ProtoBorder), modifier = Modifier.weight(1f)) { Text("Отклонить…", color = ProtoText) }
                }
                TextButton(onClick = { protoDialNumber(context, r.phone1) }) { Icon(Icons.Outlined.Call, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text("Позвонить заявителю", color = ProtoGold) }
            }
        }
        item { ProtoSectionTitle("Приостановленные клиенты", if (blocked.isEmpty()) "Нет" else null) }
        items(blocked, key = { it.id }) { c ->
            ProtoSectionCard(Modifier.clickable { onClient(c) }) {
                Text(c.name, color = ProtoText, fontWeight = FontWeight.SemiBold)
                Text("Откройте карточку, чтобы восстановить доступ", color = ProtoMuted, fontSize = 12.sp)
            }
        }
        item { ProtoSectionTitle("Заказы на сборке", if (picking.isEmpty()) "Нет" else null) }
        items(picking, key = { it.id }) { o -> ProtoOrderRow(o) { onOrder(o) } }
        if (lowStock.isNotEmpty()) item {
            ProtoSectionCard(Modifier.clickable { onLowStock() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, null, tint = ProtoOrange)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Низкие остатки: ${lowStock.size}", color = ProtoText, fontWeight = FontWeight.SemiBold)
                        Text(lowStock.take(3).joinToString(" · ") { "${it.sku}: ${stockValue(it)}" }, color = ProtoMuted, fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = ProtoGold)
                }
            }
        }
    }
    approveReg?.let { r ->
        AlertDialog(
            onDismissRequest = { approveReg = null }, containerColor = ProtoPanel,
            title = { Text("Одобрить заявку", color = ProtoText) },
            text = {
                Column {
                    Text(r.organization, color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                    Text("Будет создан клиент и выданы коды входа. Их можно сразу отправить из карточки клиента.", color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                    Text("Статус", color = ProtoText, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Активный", "Оптовик", "VIP").forEach { s ->
                            FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText))
                        }
                    }
                    Text("Скидка", color = ProtoText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(0, 3, 5, 7, 10, 15)) { d ->
                            FilterChip(selected = discount == d, onClick = { discount = d }, label = { Text("$d%", fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { approveReg = null; onApprove(r, status, discount) }) { Text("Одобрить", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { approveReg = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
    rejectReg?.let { r ->
        AlertDialog(
            onDismissRequest = { rejectReg = null }, containerColor = ProtoPanel,
            title = { Text("Отклонить заявку?", color = ProtoText) },
            text = {
                Column {
                    Text(r.organization, color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                    ProtoField(reason, { reason = it.take(200) }, "Причина отказа")
                    listOf("Не наш профиль", "Не удалось связаться", "Дубликат заявки").forEach { v ->
                        Text(v, color = ProtoGold, fontSize = 13.sp, modifier = Modifier.clickable { reason = v }.padding(vertical = 4.dp))
                    }
                    Text("Заявка сохранится в истории со статусом «Отклонена».", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = { TextButton(onClick = { rejectReg = null; onReject(r, reason.trim()) }, enabled = reason.isNotBlank()) { Text("Отклонить", color = if (reason.isNotBlank()) ProtoRed else ProtoMuted) } },
            dismissButton = { TextButton(onClick = { rejectReg = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
}

// ───────────── Кто в сети ─────────────

@Composable
internal fun ProtoOnlineControllerScreen(clients: List<ProtoClient>, presence: List<PresenceSessionEntity>, onBack: () -> Unit, onClient: (ProtoClient) -> Unit) {
    val now = System.currentTimeMillis()
    val byClient = presence.filter { it.clientId != null }.groupBy { it.clientId!! }
    val staff = presence.filter { it.clientId == null }
    val onlineIds = byClient.filterValues { list -> list.any { now - it.lastSeenAt < 3 * 60_000L } }.keys
    ProtoScaffold("Кто в сети", "Сегодня · по данным этого устройства", onBack) {
        item {
            ProtoSectionCard {
                ProtoInfoRow("Сейчас в приложении", onlineIds.size.toString())
                ProtoInfoRow("Заходили сегодня", byClient.size.toString())
                Text("Пока нет общего сервера, приложение видит только входы, сделанные на этом телефоне.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item { ProtoSectionTitle("Клиенты сегодня", if (byClient.isEmpty()) "Сегодня никто не заходил" else null) }
        items(byClient.entries.sortedByDescending { e -> e.value.maxOf { it.lastSeenAt } }, key = { it.key }) { (cid, list) ->
            val c = clients.firstOrNull { it.id == cid }
            val last = list.maxOf { it.lastSeenAt }
            val online = cid in onlineIds
            ProtoSectionCard(Modifier.clickable(enabled = c != null) { c?.let(onClient) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(if (online) ProtoGreen else ProtoMuted.copy(alpha = .5f), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c?.name ?: cid, color = ProtoText, fontWeight = FontWeight.SemiBold)
                        Text((if (online) "в сети" else "был в " + protoChatTime(last)) + " · в приложении " + protoDuration(list.sumOf { it.durationMs }), color = if (online) ProtoGreen else ProtoMuted, fontSize = 12.sp)
                    }
                }
            }
        }
        if (staff.isNotEmpty()) {
            item { ProtoSectionTitle("Сотрудники") }
            items(staff.sortedByDescending { it.lastSeenAt }, key = { it.id }) { s ->
                ProtoInfoRow(when (s.role) { "ADMIN" -> "Администратор"; "PRODUCTION" -> "Производство"; "SALES" -> "Продажи"; else -> s.role }, protoDuration(s.durationMs))
            }
        }
    }
}

// ───────────── Низкие остатки ─────────────

@Composable
internal fun ProtoLowStockListScreen(products: List<ProtoCatalogProduct>, physicalOf: (ProtoCatalogProduct) -> Int, reservedForSku: (String) -> Int, threshold: Int, onBack: () -> Unit, onOpen: (ProtoCatalogProduct) -> Unit) {
    val rows = products.filter { it.status != PRODUCT_UNPUBLISHED }.map { p -> Triple(p, physicalOf(p), reservedForSku(p.sku)) }
    val empty = rows.filter { (_, ph, rs) -> ph - rs <= 0 }
    val low = rows.filter { (_, ph, rs) -> (ph - rs) in 1..threshold }
    var tab by remember { mutableStateOf("Мало") }
    ProtoScaffold("Низкие остатки", "Порог: не больше $threshold шт.", onBack) {
        item { ProtoChipRow(listOf("Мало", "Нет в наличии"), tab) { tab = it } }
        item { Text(if (tab == "Мало") "Мало: ${low.size} · нет в наличии: ${empty.size}" else "Нет в наличии: ${empty.size} — клиенты видят их «под заказ»", color = ProtoMuted, fontSize = 12.sp) }
        val shown = if (tab == "Мало") low.sortedBy { it.second - it.third } else empty.sortedBy { it.first.sku }
        if (shown.isEmpty()) item { ProtoEmptyState(Icons.Outlined.CheckCircle, if (tab == "Мало") "Всё выше порога" else "Всё в наличии") }
        items(shown.take(300), key = { it.first.sku }) { (p, ph, rs) ->
            val av = (ph - rs).coerceAtLeast(0)
            ProtoSectionCard(Modifier.clickable { onOpen(p) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProtoProductImage(p, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${p.sku} · склад $ph · резерв $rs", color = ProtoMuted, fontSize = 12.sp)
                    }
                    Text("$av", color = if (av == 0) ProtoRed else ProtoOrange, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ───────────── Чаты ─────────────

@Composable
internal fun ProtoAdminChatsScreen(clients: List<ProtoClient>, messages: List<SansaraChatMessage>, onBack: () -> Unit, onOpen: (ProtoClient) -> Unit, onProduction: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    data class ChatThread(val client: ProtoClient, val last: SansaraChatMessage?, val unread: Int)
    val threads = clients.filter { query.isBlank() || it.name.contains(query, true) || it.contact.contains(query, true) }.map { c ->
        val t = messages.filter { it.conversationId == "CLIENT:" + c.id }
        ChatThread(c, t.maxByOrNull { it.createdAt }, t.count { !it.read && it.senderRole == "CLIENT" })
    }.sortedWith(compareByDescending<ChatThread> { it.unread > 0 }.thenByDescending { it.last?.createdAt ?: 0L })
    val withMessages = threads.filter { it.last != null }
    val shown = if (showAll || query.isNotBlank()) threads else withMessages
    ProtoScaffold("Чаты", "Непрочитанные — сверху", onBack) {
        item {
            val productionThread = messages.filter { it.conversationId == "STAFF:ADMIN_PRODUCTION" }
            val unread = productionThread.count { !it.read && it.senderRole == "PRODUCTION" }
            ProtoSectionCard(Modifier.clickable { onProduction() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Factory, null, tint = ProtoGold) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Производство", color = ProtoText, fontWeight = FontWeight.Bold)
                        Text(productionThread.maxByOrNull { it.createdAt }?.body?.ifBlank { "Вложение" } ?: "Чат с цехом", color = ProtoMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (unread > 0) Badge(containerColor = ProtoGold) { Text(unread.toString(), color = Color.Black) }
                }
            }
        }
        item { ProtoField(query, { query = it }, "Поиск клиента") }
        if (shown.isEmpty()) item { ProtoEmptyState(Icons.Outlined.ChatBubbleOutline, "Переписки пока нет", "Клиенты пишут из раздела «Чат»") }
        items(shown, key = { it.client.id }) { th ->
            ProtoSectionCard(Modifier.clickable { onOpen(th.client) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) { Text(th.client.name.take(1).uppercase(), color = ProtoGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(th.client.name, color = ProtoText, fontWeight = if (th.unread > 0) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(th.last?.let { if (it.body.isNotBlank()) it.body else "Вложение: " + it.attachmentName } ?: "Сообщений пока нет", color = if (th.unread > 0) ProtoText else ProtoMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        th.last?.let { Text(protoChatTime(it.createdAt), color = ProtoMuted, fontSize = 12.sp) }
                        if (th.unread > 0) Box(Modifier.padding(top = 4.dp).size(24.dp).background(ProtoGold, CircleShape), contentAlignment = Alignment.Center) { Text(th.unread.toString(), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        if (!showAll && query.isBlank() && threads.size > withMessages.size) item {
            TextButton(onClick = { showAll = true }, modifier = Modifier.fillMaxWidth()) { Text("Написать клиенту без переписки (" + (threads.size - withMessages.size) + ")", color = ProtoGold) }
        }
    }
}

// ───────────── Отчёты ─────────────

private fun periodRange(period: String): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    return when (period) {
        "7 дней" -> today.minusDays(6) to today
        "Месяц" -> today.withDayOfMonth(1) to today
        "Прошлый месяц" -> YearMonth.from(today).minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
        else -> today to today
    }
}

private fun inRange(date: String, range: Pair<LocalDate, LocalDate>): Boolean {
    val d = protoParseDate(date.substringBefore(" ")) ?: return false
    return !d.isBefore(range.first) && !d.isAfter(range.second)
}

@Composable
internal fun ProtoAdminReportsScreen(
    tasks: List<WorkshopTaskEntity>,
    attendance: List<AttendanceEntity>,
    presence: List<PresenceSessionEntity>,
    productionOps: List<ProtoProductionOp>,
    orders: List<ProtoOrder>,
    clientName: (String?) -> String,
    dailyStatus: AdminDailyStatusEntity?,
    onBack: () -> Unit,
    onDailyStatus: (Boolean, String) -> Unit
) {
    var period by remember { mutableStateOf("Сегодня") }
    val range = periodRange(period)
    val periodTasks = tasks.filter { inRange(it.taskDate, range) }
    val plan = periodTasks.sumOf { protoWorkshopPlanQty(it.linesJson) }
    val fact = periodTasks.sumOf { it.actualQty }
    val ops = productionOps.filter { inRange(it.date, range) && it.status != "Удален" }
    val periodOrders = orders.filter { inRange(it.dateTime, range) && it.status != "Отменён" }
    val worked = attendance.filter { inRange(it.date, range) && it.status == "Работал" }
    var dayOff by remember(dailyStatus?.date, dailyStatus?.dayOff) { mutableStateOf(dailyStatus?.dayOff ?: false) }
    var reason by remember(dailyStatus?.date, dailyStatus?.reason) { mutableStateOf(dailyStatus?.reason.orEmpty()) }
    val rangeLabel = if (range.first == range.second) range.first.format(protoDateFmt) else range.first.format(protoDateFmt) + " – " + range.second.format(protoDateFmt)
    ProtoScaffold("Отчёты", rangeLabel, onBack) {
        item { ProtoChipRow(listOf("Сегодня", "7 дней", "Месяц", "Прошлый месяц"), period) { period = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("Заказы", periodOrders.size.toString(), protoMoney(periodOrders.sumOf { it.total }), Modifier.weight(1f)) {}
                ProtoMetricCard("Выпуск", ops.sumOf { it.qty }.toString(), "шт.", Modifier.weight(1f)) {}
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("План / факт цеха", "$plan / $fact", "шт.", Modifier.weight(1f)) {}
                ProtoMetricCard("Выходы", worked.size.toString(), "по табелю", Modifier.weight(1f)) {}
            }
        }
        item {
            ProtoSectionCard {
                Text("Выпуск по сборщицам", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                val byAssembler = ops.groupBy { it.assembler }.entries.sortedByDescending { e -> e.value.sumOf { it.qty } }
                if (byAssembler.isEmpty()) Text("За период выпуска нет", color = ProtoMuted, modifier = Modifier.padding(top = 6.dp))
                byAssembler.forEach { (name, list) -> ProtoInfoRow(name.ifBlank { "—" }, list.sumOf { it.qty }.toString() + " шт. · " + protoMoney(list.sumOf { it.amountRub })) }
            }
        }
        item {
            ProtoSectionCard {
                Text("Крупные клиенты за период", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                val top = periodOrders.groupBy { it.clientName.substringBefore(" / ") }.entries.sortedByDescending { e -> e.value.sumOf { it.total } }.take(8)
                if (top.isEmpty()) Text("Заказов нет", color = ProtoMuted, modifier = Modifier.padding(top = 6.dp))
                top.forEach { (name, list) -> ProtoInfoRow(name, protoMoney(list.sumOf { it.total })) }
            }
        }
        item {
            ProtoSectionCard {
                Text("Задания в цех", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                if (periodTasks.isEmpty()) Text("Заданий нет", color = ProtoMuted, modifier = Modifier.padding(top = 6.dp))
                periodTasks.forEach { t -> ProtoInfoRow(t.taskDate + " · " + t.status, protoWorkshopPlanQty(t.linesJson).toString() + " / " + t.actualQty + " шт.") }
            }
        }
        if (period == "Сегодня") item {
            ProtoSectionCard {
                Text("Кто заходил сегодня", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                if (presence.isEmpty()) Text("Нет данных", color = ProtoMuted, modifier = Modifier.padding(top = 6.dp))
                presence.sortedByDescending { it.durationMs }.take(12).forEach { p -> ProtoInfoRow(clientName(p.clientId).ifBlank { when (p.role) { "ADMIN" -> "Администратор"; "PRODUCTION" -> "Производство"; "SALES" -> "Продажи"; else -> p.role } }, protoDuration(p.durationMs)) }
            }
        }
        item {
            ProtoSectionCard {
                Text("Напоминания администратора сегодня", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                ProtoSwitchRow("Сегодня выходной", dayOff) { dayOff = it }
                ProtoField(reason, { reason = it }, "Причина / комментарий")
                ProtoPrimaryButton("Сохранить", { onDailyStatus(dayOff, reason) })
                Text("В выходной обязательные напоминания на этот день не приходят.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// ───────────── Задание в цех ─────────────

@Composable
internal fun ProtoWorkshopTasksScreen(
    tasks: List<WorkshopTaskEntity>,
    products: List<ProtoCatalogProduct> = emptyList(),
    productionMode: Boolean,
    prefill: Map<String, Int> = emptyMap(),
    prefillComment: String = "",
    onBack: () -> Unit,
    onCreate: (String, String, String, Boolean) -> Unit,
    onFact: (WorkshopTaskEntity, Int, String) -> Unit
) {
    val groups = protoWreathGroupsOrdered.filter { g -> products.any { it.type == g } }.ifEmpty { protoWreathGroupsOrdered }
    var category by remember { mutableStateOf(groups.first()) }
    var search by remember { mutableStateOf("") }
    val picked = remember(prefill) { mutableStateMapOf<String, Int>().apply { putAll(prefill) } }
    var comment by remember(prefillComment) { mutableStateOf(prefillComment) }
    var commentOnly by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showCalendar by remember { mutableStateOf(false) }
    var confirmSend by remember { mutableStateOf(false) }
    val sorted = tasks.sortedByDescending { it.createdAt }
    val chosen = picked.entries.mapNotNull { (sku, q) -> products.firstOrNull { it.sku == sku }?.let { it to q } }
    ProtoScaffold(if (productionMode) "Задания цеху" else "Задание в цех", if (productionMode) "План от администратора · отметьте факт" else "Выберите продукцию и дату", onBack) {
        if (!productionMode) {
            item {
                ProtoSectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Дата задания", color = ProtoMuted, fontSize = 12.sp)
                            Text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", ruLocale)), color = ProtoText, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { showCalendar = true }, border = BorderStroke(1.dp, ProtoGold)) { Icon(Icons.Outlined.CalendarMonth, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text("Изменить", color = ProtoGold) }
                    }
                    ProtoSwitchRow("Только комментарий, без позиций", commentOnly) { commentOnly = it }
                }
            }
            if (!commentOnly) {
                item { ProtoChipRow(groups, category) { category = it } }
                item { ProtoField(search, { search = it }, "Поиск по артикулу или названию") }
                val shown = products.filter { p -> p.type == category && (search.isBlank() || p.sku.contains(search, true) || p.name.contains(search, true)) }.sortedBy { it.sku }
                if (shown.isEmpty()) item { ProtoSectionCard { Text("В группе «$category» товаров не найдено", color = ProtoMuted) } }
                items(shown.take(60), key = { "w-" + it.sku }) { p ->
                    val q = picked[p.sku] ?: 0
                    ProtoSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProtoProductImage(p, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                ProtoSkuText(p.sku, fontSize = 12)
                            }
                            ProtoQtyButton(Icons.Outlined.Remove, { if (q <= 1) picked.remove(p.sku) else picked[p.sku] = q - 1 }, 38.dp)
                            Text(q.toString(), color = if (q > 0) ProtoGold else ProtoMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            ProtoQtyButton(Icons.Outlined.Add, { picked[p.sku] = (q + 1).coerceAtMost(9999) }, 38.dp)
                        }
                    }
                }
                if (shown.size > 60) item { Text("Показаны первые 60 из ${shown.size} — уточните поиск", color = ProtoMuted, fontSize = 12.sp) }
            }
            item {
                ProtoSectionCard {
                    if (!commentOnly) {
                        Text("Состав задания", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        if (chosen.isEmpty()) Text("Позиции пока не выбраны", color = ProtoMuted, modifier = Modifier.padding(top = 6.dp))
                        chosen.forEach { (p, q) -> ProtoInfoRow(protoTitle(p) + " · " + p.sku, "$q шт.") }
                        if (chosen.isNotEmpty()) {
                            HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
                            ProtoInfoRow("Итого", chosen.sumOf { it.second }.toString() + " шт.")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(value = comment, onValueChange = { comment = it.take(500) }, label = { Text("Комментарий для цеха") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), minLines = 3, maxLines = 5, colors = protoFieldColors())
                    val valid = if (commentOnly) comment.isNotBlank() else chosen.isNotEmpty()
                    ProtoPrimaryButton("Отправить в цех", { confirmSend = true }, enabled = valid)
                }
            }
        }
        item { ProtoSectionTitle(if (productionMode) "Задания" else "Отправленные задания", if (sorted.isEmpty()) "Заданий пока нет" else null) }
        items(sorted, key = { it.id }) { task ->
            var factText by remember(task.id, task.actualQty) { mutableStateOf(task.actualQty.toString()) }
            val plan = protoWorkshopPlanQty(task.linesJson)
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.taskDate, color = ProtoText, fontWeight = FontWeight.SemiBold)
                        Text(task.id, color = ProtoMuted, fontSize = 12.sp)
                    }
                    ProtoPill(task.status, if (task.status == "Выполнено") ProtoGreen else ProtoOrange)
                }
                if (task.commentOnly) Text("Комментарий: " + task.comment, color = ProtoText, modifier = Modifier.padding(top = 7.dp))
                else {
                    Text(protoWorkshopLinesLabel(task.linesJson), color = ProtoText, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
                    if (task.comment.isNotBlank()) Text("Комментарий: " + task.comment, color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    ProtoInfoRow("План / факт", "$plan / ${task.actualQty} шт.")
                }
                if (productionMode && !task.commentOnly) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = factText, onValueChange = { factText = it.filter(Char::isDigit).take(4) }, label = { Text("Сделано, шт.") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors())
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { val f = factText.toIntOrNull() ?: 0; onFact(task, f, if (f >= plan && plan > 0) "Выполнено" else "В работе") }, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) { Text("Сохранить", color = Color.Black) }
                    }
                }
            }
        }
    }
    if (showCalendar) ProtoProductionCalendarDialog(date, onDismiss = { showCalendar = false }, minDate = LocalDate.now(), onSelect = { date = it; showCalendar = false })
    if (confirmSend) AlertDialog(
        onDismissRequest = { confirmSend = false }, containerColor = ProtoPanel,
        title = { Text("Отправить задание в цех?", color = ProtoText) },
        text = { Text(date.format(protoDateFmt) + if (commentOnly) " · только комментарий" else " · " + chosen.sumOf { it.second } + " шт., " + protoPlural(chosen.size, "позиция", "позиции", "позиций"), color = ProtoMuted) },
        confirmButton = {
            TextButton(onClick = {
                confirmSend = false
                val lines = if (commentOnly) "" else JSONArray().also { arr ->
                    chosen.forEach { (p, q) -> arr.put(JSONObject().put("sku", p.sku).put("name", p.name).put("category", p.type).put("size", p.size).put("qty", q)) }
                }.toString()
                onCreate(date.format(protoDateFmt), lines, comment, commentOnly)
                picked.clear(); comment = ""
            }) { Text("Отправить", color = ProtoGold) }
        },
        dismissButton = { TextButton(onClick = { confirmSend = false }) { Text("Отмена", color = ProtoMuted) } }
    )
}

// ───────────── Администраторы ─────────────

@Composable
internal fun ProtoAdminAccountsScreen(
    admins: List<SansaraAdminAccount>,
    currentUserId: String,
    issued: Pair<String, String>?,
    onBack: () -> Unit,
    onAdd: (String, String) -> Unit,
    onToggle: (SansaraAdminAccount) -> Unit,
    onIssuedSeen: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val main = admins.firstOrNull { it.isMain }
    val canManage = main?.userId == currentUserId || currentUserId.isBlank()
    ProtoScaffold("Администраторы", "У каждого — свой код входа", onBack) {
        issued?.let { (who, code) ->
            item {
                Surface(color = ProtoGold.copy(alpha = .12f), border = BorderStroke(1.dp, ProtoGold), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Код для «$who»", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                        Text(code, color = ProtoText, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Код показывается один раз. Передайте его администратору.", color = ProtoMuted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = { protoShare(context, "SANSARA: ваш код администратора $code. Вход: ваш телефон и этот код.", "Отправить код") }, border = BorderStroke(1.dp, ProtoGold), modifier = Modifier.weight(1f)) { Text("Отправить", color = ProtoGold) }
                            TextButton(onClick = onIssuedSeen, modifier = Modifier.weight(1f)) { Text("Готово", color = ProtoMuted) }
                        }
                    }
                }
            }
        }
        item {
            ProtoSectionCard {
                if (canManage) {
                    Text("Новый администратор", color = ProtoText, fontWeight = FontWeight.SemiBold)
                    ProtoField(name, { name = it.take(60) }, "Имя")
                    ProtoField(phone, { phone = protoPhoneDigits(it) }, "Телефон (10 цифр)", KeyboardType.Phone)
                    ProtoPrimaryButton("Добавить и выдать код", { onAdd(name.trim(), phone); name = ""; phone = "" }, enabled = name.isNotBlank() && phone.length == 10)
                } else Text("Добавлять и отключать администраторов может только главный аккаунт.", color = ProtoOrange, fontSize = 13.sp)
            }
        }
        items(admins, key = { it.userId }) { admin ->
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) { Icon(if (admin.isMain) Icons.Outlined.VerifiedUser else Icons.Outlined.AdminPanelSettings, null, tint = ProtoGold) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(admin.displayName, color = ProtoText, fontWeight = FontWeight.Bold)
                        Text((if (admin.isMain) "Главный аккаунт" else "Администратор") + " · " + (if (admin.enabled) "активен" else "отключён"), color = if (admin.enabled) ProtoGoldSoft else ProtoRed, fontSize = 12.sp)
                    }
                    if (!admin.isMain && canManage) Switch(checked = admin.enabled, onCheckedChange = { onToggle(admin) }, colors = SwitchDefaults.colors(checkedTrackColor = ProtoGold, checkedThumbColor = Color.Black))
                }
            }
        }
    }
}

// ───────────── Склад ─────────────

@Composable
internal fun ProtoServerScreen(
    products: List<ProtoCatalogProduct>,
    physicalOf: (ProtoCatalogProduct) -> Int,
    reservedForSku: (String) -> Int,
    productionOps: List<ProtoProductionOp>,
    clients: List<ProtoClient>,
    canAdjust: Boolean,
    showClients: Boolean = true,
    onBack: () -> Unit,
    onProduced: () -> Unit,
    onReserve: () -> Unit,
    onNewClients: () -> Unit,
    onExport: () -> Unit,
    onOpen: (ProtoCatalogProduct) -> Unit,
    onAdjust: (ProtoCatalogProduct, Int, String) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var q by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Все") }
    var avail by remember { mutableStateOf("Все") }
    var adjustProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var deltaText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val live = products.filter { it.status != PRODUCT_UNPUBLISHED }
    val totalPhysical = live.sumOf { physicalOf(it) }
    val totalReserve = live.sumOf { reservedForSku(it.sku) }
    val totalAvailable = live.sumOf { (physicalOf(it) - reservedForSku(it.sku)).coerceAtLeast(0) }
    val monthKey = month.format(DateTimeFormatter.ofPattern("MM.yyyy"))
    val monthProduced = productionOps.filter { it.date.endsWith(monthKey) && it.status != "Удален" }.sumOf { it.qty }
    val newClients = clients.count { it.registeredAt.endsWith(monthKey) }
    val types = listOf("Все") + live.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()
    val shown = live.filter { p ->
        val qq = q.trim()
        val av = physicalOf(p) - reservedForSku(p.sku)
        (qq.isBlank() || p.sku.contains(qq, true) || p.name.contains(qq, true)) && (type == "Все" || p.type == type) &&
            when (avail) { "В наличии" -> av > 0; "Нет" -> av <= 0; "В резерве" -> reservedForSku(p.sku) > 0; else -> true }
    }.sortedBy { it.sku }
    ProtoScaffold("Склад", "Остатки, резерв и выпуск", onBack, action = { IconButton(onClick = onExport) { Icon(Icons.Outlined.FileDownload, contentDescription = "Экспорт", tint = ProtoGold) } }) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("На складе", totalPhysical.toString(), "шт.", Modifier.weight(1f)) {}
                ProtoMetricCard("Резерв", totalReserve.toString(), "шт.", Modifier.weight(1f), onReserve)
                ProtoMetricCard("Доступно", totalAvailable.toString(), "шт.", Modifier.weight(1f)) {}
            }
        }
        item {
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Outlined.ChevronLeft, null, tint = ProtoGold) }
                    Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale)).replaceFirstChar { it.titlecase(ruLocale) }, color = ProtoText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { if (month < YearMonth.now()) month = month.plusMonths(1) }, enabled = month < YearMonth.now()) { Icon(Icons.Outlined.ChevronRight, null, tint = if (month < YearMonth.now()) ProtoGold else ProtoMuted) }
                }
                Row(Modifier.fillMaxWidth().clickable { onProduced() }.padding(vertical = 4.dp)) { Text("Выпущено за месяц", color = ProtoMuted, modifier = Modifier.weight(1f)); Text("$monthProduced шт.", color = ProtoText, fontWeight = FontWeight.SemiBold) }
                if (showClients) Row(Modifier.fillMaxWidth().clickable { onNewClients() }.padding(vertical = 4.dp)) { Text("Новые клиенты", color = ProtoMuted, modifier = Modifier.weight(1f)); Text(newClients.toString(), color = ProtoText, fontWeight = FontWeight.SemiBold) }
            }
        }
        item { ProtoField(q, { q = it }, "Поиск по артикулу или названию") }
        item { ProtoChipRow(types, type) { type = it } }
        item { ProtoChipRow(listOf("Все", "В наличии", "Нет", "В резерве"), avail) { avail = it } }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("Товар · " + shown.size, color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("склад", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text("резерв", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text("доступно", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.width(68.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
        if (shown.isEmpty()) item { ProtoEmptyState(Icons.Outlined.Inventory2, "Ничего не найдено") }
        items(shown.take(400), key = { it.sku }) { p ->
            val ph = physicalOf(p); val rs = reservedForSku(p.sku); val av = (ph - rs).coerceAtLeast(0)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ProtoPanel).clickable { onOpen(p) }.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(protoTitle(p), color = ProtoText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ProtoSkuText(p.sku, fontSize = 12)
                }
                Text(ph.toString(), color = ProtoText, fontSize = 14.sp, modifier = Modifier.width(52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text(rs.toString(), color = if (rs > 0) ProtoGoldSoft else ProtoMuted, fontSize = 14.sp, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text(av.toString(), color = if (av > 0) ProtoGreen else ProtoRed, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(68.dp).then(if (canAdjust) Modifier.clickable { adjustProduct = p; deltaText = ""; reason = "" } else Modifier), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
        if (shown.size > 400) item { Text("Показаны первые 400 — уточните поиск", color = ProtoMuted, fontSize = 12.sp) }
        if (canAdjust) item { Text("Нажмите на «доступно», чтобы скорректировать остаток; на строку — чтобы открыть товар.", color = ProtoMuted, fontSize = 12.sp) }
    }
    adjustProduct?.let { product ->
        val ph = physicalOf(product)
        AlertDialog(
            onDismissRequest = { adjustProduct = null }, containerColor = ProtoPanel,
            title = { Text("Корректировка остатка", color = ProtoText) },
            text = {
                Column {
                    Text(protoTitle(product) + " · " + product.sku, color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                    Text("Сейчас на складе: $ph шт.", color = ProtoMuted, fontSize = 13.sp)
                    ProtoField(deltaText, { v -> deltaText = v.filter { it.isDigit() || it == '-' }.take(6) }, "Изменение, шт. (−2 или 5)", KeyboardType.Number)
                    ProtoField(reason, { reason = it.take(120) }, "Причина")
                }
            },
            confirmButton = { val d = deltaText.toIntOrNull() ?: 0; TextButton(onClick = { onAdjust(product, d, reason.trim()); adjustProduct = null }, enabled = d != 0 && reason.isNotBlank()) { Text("Провести", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { adjustProduct = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
}

@Composable
internal fun ProtoReserveListScreen(orders: List<ProtoOrder>, onBack: () -> Unit, onOpen: (ProtoOrder) -> Unit, showClients: Boolean = true) {
    // Резерв = заказы «Получен» и «Подтверждён»: со склада они ещё не списаны (как в расчёте доступного остатка).
    val reserved = orders.filter { it.status in setOf("Получен", "Подтверждён") }.sortedBy { protoParseDate(it.deliveryDate) ?: LocalDate.MAX }
    ProtoScaffold("Резерв", protoPlural(reserved.sumOf { it.pieces }, "штука", "штуки", "штук") + " в " + protoPlural(reserved.size, "заказе", "заказах", "заказах"), onBack) {
        item { Text("Товар закреплён за заказом, пока он не передан в сборку. После «Собирается» он списывается со склада.", color = ProtoMuted, fontSize = 12.sp) }
        if (reserved.isEmpty()) item { ProtoEmptyState(Icons.Outlined.Inventory2, "Резерва нет") }
        items(reserved, key = { it.id }) { o ->
            ProtoSectionCard(Modifier.clickable(enabled = showClients) { onOpen(o) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (showClients) o.clientName else "Заказ № " + o.id, color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("№ ${o.id} · получение ${o.deliveryDate.ifBlank { "—" }}", color = ProtoMuted, fontSize = 12.sp)
                    }
                    ProtoStatusPill(o.status)
                }
                o.lines.forEach { l -> Text("${l.sku} — ${l.qty} шт.", color = ProtoGoldSoft, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
internal fun ProtoExportScreen(onBack: () -> Unit, onExport: (String, String) -> Unit) {
    var report by remember { mutableStateOf("Остатки на складе") }
    var period by remember { mutableStateOf("Этот месяц") }
    val periodic = report in setOf("Выпуск за период", "Производство по сборщицам", "Заказы за период")
    ProtoScaffold("Экспорт", "CSV для Excel · сохраняется в «Загрузки» и сразу открывается «Поделиться»", onBack) {
        item {
            ProtoSectionCard {
                listOf("Остатки на складе", "Резерв по клиентам", "Выпуск за период", "Производство по сборщицам", "Заказы за период", "Клиенты").forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { report = r }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = report == r, onClick = { report = r }, colors = RadioButtonDefaults.colors(selectedColor = ProtoGold))
                        Text(r, color = ProtoText)
                    }
                }
            }
        }
        if (periodic) item { ProtoChipRow(listOf("Этот месяц", "Прошлый месяц", "Всё время"), period) { period = it } }
        item {
            Button(onClick = { onExport(report, if (periodic) period else "Всё время") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) {
                Icon(Icons.Outlined.FileDownload, null, tint = Color.Black); Spacer(Modifier.width(7.dp)); Text("Выгрузить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
