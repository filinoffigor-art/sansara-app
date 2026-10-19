package ru.sansara.app

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
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
    onNotifications: () -> Unit,
    approvals: Int = 0,
    onApprovals: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(containerColor = Color.Transparent, bottomBar = { ProtoAdminBottomBar(ProtoScreen.AdminHome, onHome = {}, onClients, onOrders, onStock, onProfile = onSettings) }) { pad ->
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ProtoBrandHeader(unreadCount = unreadCount, onBell = onNotifications, edge = 0.dp) }
                item {
                    Text("Здравствуйте" + if (adminName.isNotBlank()) ", $adminName" else "", color = ProtoText, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Администратор · " + currentDateLong(), color = ProtoGoldSoft, fontSize = 14.sp)
                }
                item { ProtoSearchBar("", onSearch, "Поиск по клиентам, заказам, товарам") }
                item {
                    ProtoSectionCard {
                        Text("Сегодня", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        if (approvals > 0) TodayRow(Icons.Outlined.Gavel, "Заказы на согласовании", approvals, ProtoOrange, onApprovals)
                        TodayRow(Icons.Outlined.ReceiptLong, "Новые заказы — подтвердить", newOrders, ProtoGold, onNewOrders)
                        TodayRow(Icons.Outlined.LocalShipping, "Получение сегодня", deliveriesToday, ProtoGold, onDeliveriesToday)
                        TodayRow(Icons.Outlined.PersonAdd, "Заявки на регистрацию", newRegistrations, ProtoGold, onRegistrations)
                        TodayRow(Icons.Outlined.Payments, "Оплаты ждут подтверждения", paymentsPending, ProtoGold, onPayments)
                        TodayRow(Icons.Outlined.ChatBubbleOutline, "Непрочитанные сообщения", unreadChats, ProtoGold, onChats)
                        TodayRow(Icons.Outlined.Warning, "Низкие остатки", lowStockCount, ProtoOrange, onLowStock)
                    }
                }
                item {
                    // Три плашки одной высоты: подписи в две строки, значения на одной линии.
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProtoMetricCard("Заказы сегодня", ordersToday.toString(), "", Modifier.weight(1f).fillMaxHeight(), titleLines = 2, onClick = onOrders)
                        ProtoMetricCard("Выпуск сегодня", producedToday.toString(), "шт.", Modifier.weight(1f).fillMaxHeight(), titleLines = 2, onClick = onProduction)
                        ProtoMetricCard("Клиенты", activeClients.toString(), "активные", Modifier.weight(1f).fillMaxHeight(), titleLines = 2, onClick = onClients)
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
    onOpenOrder: (ProtoOrder) -> Unit,
    canDeleteHistory: Boolean = false,
    onDeleteHistory: (List<String>?) -> Unit = {},
    onDeferral: (Boolean, Int) -> Unit = { _, _ -> }
) {
    val c = client ?: return
    val settlement = remember(clientOrders, c) { protoSettlement(clientOrders, { c }) }
    // История изменений свёрнута; главный администратор может удалить отдельные записи или всю историю.
    var historyOpen by remember(c.id) { mutableStateOf(false) }
    var selecting by remember(c.id) { mutableStateOf(false) }
    var picked by remember(c.id) { mutableStateOf(setOf<String>()) }
    var confirmClear by remember(c.id) { mutableStateOf(false) }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false }, containerColor = ProtoPanel,
        title = { Text("Очистить всю историю?", color = ProtoText) },
        text = { Text("Все записи об изменениях клиента будут удалены без возможности восстановления.", color = ProtoMuted) },
        confirmButton = { TextButton(onClick = { confirmClear = false; selecting = false; picked = emptySet(); onDeleteHistory(null) }) { Text("Очистить", color = ProtoRed) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена", color = ProtoMuted) } }
    )
    val context = LocalContext.current
    var confirmReissue by remember { mutableStateOf(false) }
    var suspendDialog by remember { mutableStateOf(false) }
    var suspendReason by remember { mutableStateOf("") }
    var drill by remember(c.id) { mutableStateOf<String?>(null) }
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
                    OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, ProtoGold), contentPadding = PaddingValues(horizontal = 10.dp)) { Icon(Icons.Outlined.Call, null, tint = ProtoGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Позвонить", color = ProtoGold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, ProtoGold), contentPadding = PaddingValues(horizontal = 10.dp)) { Icon(Icons.Outlined.ChatBubbleOutline, null, tint = ProtoGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Написать", color = ProtoGold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        item { ProtoSettlementCard(settlement, "Расчёты", "Оборот = оплачено + в отсрочке" + (if (settlement.overdue > 0) " + просрочено" else "") + (if (settlement.due > 0) " + к оплате" else ""), onOpen = { k -> val want = when (k) { "turnover" -> "turnover"; "paid" -> "paid"; else -> "debt" }; drill = if (drill == want) null else want }) }
        item { ProtoDeferralRuleCard(c.deferralEnabled, c.deferralDays, onDeferral) }
        // Раскрытие показателя до первичных записей: заказы или отгрузки, из которых он посчитан.
        when (drill) {
            "turnover", "orders" -> {
                val list = clientOrders.filter { it.status !in protoNotInTurnover }.sortedByDescending { protoOrderEpoch(it.dateTime) }
                item { Text("Заказы в обороте: " + list.size + " · " + protoMoney(list.sumOf { it.total }), color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold) }
                if (list.isEmpty()) item { Text("Записей нет", color = ProtoMuted) }
                items(list, key = { "d-" + it.id }) { o -> ProtoOrderRow(o) { onOpenOrder(o) } }
            }
            "paid" -> {
                val list = clientOrders.filter { it.status !in protoNotInTurnover && it.paid > 0 }.sortedByDescending { protoOrderEpoch(it.dateTime) }
                item { Text("Оплаченные заказы: " + list.size + " · получено " + protoMoney(settlement.paid), color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold) }
                if (list.isEmpty()) item { Text("Записей нет", color = ProtoMuted) }
                items(list, key = { "pd-" + it.id }) { o -> ProtoSettleOrderRow(o, protoOrderTerms(o, c)) { onOpenOrder(o) } }
            }
            "debt" -> {
                val unpaid = clientOrders.filter { it.status !in protoNotInTurnover && it.due > 0 }.map { it to protoOrderTerms(it, c) }
                    .sortedWith(compareBy({ it.second.state != ProtoPayState.OVERDUE }, { it.second.dueDate ?: LocalDate.MAX }))
                item { Text("Неоплаченные заказы: " + unpaid.size + " · " + protoMoney(settlement.unpaid), color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold) }
                if (unpaid.isEmpty()) item { Text("Записей нет", color = ProtoMuted) }
                items(unpaid, key = { "u-" + it.first.id }) { (o, t) -> ProtoSettleOrderRow(o, t) { onOpenOrder(o) } }
                val open = shipments.filter { it.clientId == c.id && it.moneyStatus != "CONFIRMED" }.sortedBy { protoParseDate(it.dueDate) ?: LocalDate.MAX }
                if (open.isNotEmpty()) item { Text("Отгрузки отдела продаж без подтверждённых денег: " + open.size + " · " + protoMoney(open.sumOf { it.amount }), color = ProtoMuted, fontWeight = FontWeight.SemiBold) }
                items(open, key = { "s-" + it.id }) { sh ->
                    val order = clientOrders.firstOrNull { it.id == sh.orderId }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ProtoPanel).clickable(enabled = order != null) { order?.let(onOpenOrder) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Отгрузка " + sh.shipDate + " · заказ " + sh.orderId, color = ProtoText, fontSize = 14.sp)
                            Text((if (sh.paymentType == "DEFERRED") "отсрочка до " + sh.dueDate else "оплата при отгрузке") + " · " + protoMoneyLabel(sh), color = ProtoMuted, fontSize = 12.sp)
                        }
                        Text(protoMoney(sh.amount), color = ProtoRed, fontWeight = FontWeight.Bold)
                    }
                }
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
        item { ProtoSectionTitle("Контакты и коды входа", "У каждого контакта свой код, права в кабинете компании одинаковые") }
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
                Row {
                    if (code.isNotBlank()) TextButton(onClick = {
                        protoSms(context, u.phone, "SANSARA: ваш код доступа $code. Откройте приложение и нажмите «Войти».")
                    }) { Icon(Icons.Outlined.Sms, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text("Отправить код по SMS", color = ProtoGold) }
                    if (u.phone.isNotBlank()) TextButton(onClick = { protoDialNumber(context, u.phone) }) { Icon(Icons.Outlined.Call, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text("Позвонить", color = ProtoGold) }
                }
            }
        }
        item { OutlinedButton(onClick = { confirmReissue = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, ProtoBorder)) { Text("Выдать новый код основному пользователю", color = ProtoText) } }
        if (shipments.any { it.clientId == c.id }) item { ProtoClientFinanceBlock(c.id, shipments, title = "Отгрузки отдела продаж") }
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
        item {
            ProtoSectionCard(Modifier.clickable { historyOpen = !historyOpen; if (!historyOpen) { selecting = false; picked = emptySet() } }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("История изменений", color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(if (history.isEmpty()) "Изменений пока не было" else protoPlural(history.size, "запись", "записи", "записей"), color = ProtoMuted, fontSize = 12.sp)
                    }
                    Icon(if (historyOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (historyOpen) "Свернуть" else "Раскрыть", tint = ProtoGold)
                }
            }
        }
        if (historyOpen && history.isNotEmpty()) {
            if (canDeleteHistory) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!selecting) {
                        OutlinedButton(onClick = { selecting = true }, border = BorderStroke(1.dp, ProtoGold), modifier = Modifier.weight(1f)) { Text("Выбрать", color = ProtoGold) }
                        OutlinedButton(onClick = { confirmClear = true }, border = BorderStroke(1.dp, ProtoRed), modifier = Modifier.weight(1f)) { Text("Очистить всю историю", color = ProtoRed, maxLines = 1) }
                    } else {
                        Button(onClick = { onDeleteHistory(picked.toList()); picked = emptySet(); selecting = false }, enabled = picked.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = ProtoRed), modifier = Modifier.weight(1f)) { Text("Удалить (" + picked.size + ")", color = Color.White) }
                        OutlinedButton(onClick = { selecting = false; picked = emptySet() }, border = BorderStroke(1.dp, ProtoBorder), modifier = Modifier.weight(1f)) { Text("Отмена", color = ProtoMuted) }
                    }
                }
            }
            items(history, key = { it.id }) { h ->
                val checked = h.id in picked
                Row(Modifier.fillMaxWidth().then(if (selecting) Modifier.clickable { picked = protoToggle(picked, h.id) } else Modifier).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selecting) Checkbox(checked = checked, onCheckedChange = { picked = protoToggle(picked, h.id) }, colors = CheckboxDefaults.colors(checkedColor = ProtoGold, checkmarkColor = Color.Black))
                    Text(protoChatTime(h.createdAt), color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.width(92.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.text, color = ProtoText, fontSize = 13.sp)
                        Text(h.actor, color = ProtoMuted, fontSize = 12.sp)
                    }
                    if (canDeleteHistory && !selecting) IconButton(onClick = { onDeleteHistory(listOf(h.id)) }) { Icon(Icons.Outlined.DeleteOutline, "Удалить запись", tint = ProtoMuted) }
                }
            }
        }
    }
}

// ───────────── Заказы ─────────────

@Composable
internal fun ProtoAdminOrdersScreen(orders: List<ProtoOrder>, initialStatus: String, initialDate: String, onBack: () -> Unit, onOpen: (ProtoOrder) -> Unit, clientTypeOf: (ProtoOrder) -> String = { it.clientType }, clientPhoneOf: (ProtoOrder) -> String = { it.contactPhone }) {
    var q by remember { mutableStateOf("") }
    var status by remember(initialStatus) { mutableStateOf(initialStatus) }
    var dateFilter by remember(initialDate) { mutableStateOf(initialDate) }
    var kind by remember { mutableStateOf("Все") }
    val statuses = listOf("Все", "На согласовании", "Получен", "Подтверждён", "Собирается", "Доставляется", "Доставлен", "Отменён", "Отклонён")
    val today = LocalDate.now()
    fun isAgent(o: ProtoOrder) = clientTypeOf(o).contains("Агент", ignoreCase = true)
    val qq = q.trim()
    val qDigits = qq.filter(Char::isDigit)
    // Быстрый поиск: последние цифры номера («2384» → S-002384), клиент, получатель или телефон.
    fun queryOk(o: ProtoOrder) = qq.isBlank() ||
        (qDigits.isNotEmpty() && qDigits.length == qq.length && (o.id.filter(Char::isDigit).endsWith(qDigits) || clientPhoneOf(o).filter(Char::isDigit).endsWith(qDigits))) ||
        o.id.contains(qq, true) || o.clientName.contains(qq, true) || o.retailCustomerName.contains(qq, true) || o.recipient.contains(qq, true)
    val base = orders.filter { o ->
        val statusOk = status == "Все" || o.status == status
        val d = protoParseDate(o.deliveryDate)
        val dateOk = when (dateFilter) {
            "Сегодня" -> d == today
            "Завтра" -> d == today.plusDays(1)
            "Неделя" -> d != null && !d.isBefore(today) && d.isBefore(today.plusDays(7))
            "Просрочены" -> d != null && d.isBefore(today) && o.status !in protoHistoryStatuses
            else -> true
        }
        queryOk(o) && statusOk && dateOk
    }
    val agents = base.count { isAgent(it) }
    val filtered = base.filter { when (kind) { "Агенты" -> isAgent(it); "Торг. орг." -> !isAgent(it); else -> true } }
        // Сначала заказы в работе по сроку доставки (просроченные и сегодняшние — вверху), потом завершённые — новые сверху.
        .sortedWith(compareBy<ProtoOrder> { it.status in protoHistoryStatuses }
            .thenBy { if (it.status in protoHistoryStatuses) 0L else (protoParseDate(it.deliveryDate)?.toEpochDay() ?: Long.MAX_VALUE) }
            .thenByDescending { protoOrderEpoch(it.dateTime) })
    ProtoScaffold("Заказы", protoPlural(filtered.size, "заказ", "заказа", "заказов") + " · " + protoMoney(filtered.filter { it.status !in setOf("Отменён", "Отклонён") }.sumOf { it.total }), onBack) {
        item { ProtoSearchField(q, { q = it }, {}, "Последние 4 цифры номера, клиент или телефон") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Все" to base.size, "Агенты" to agents, "Торг. орг." to base.size - agents).forEach { (label, n) ->
                    val sel = kind == label
                    Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(if (sel) ProtoGold else ProtoPanel).border(1.dp, if (sel) ProtoGold else ProtoBorder, RoundedCornerShape(21.dp)).clickable { kind = label }, contentAlignment = Alignment.Center) {
                        Text("$label · $n", color = if (sel) Color.Black else ProtoText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
        item { ProtoChipRow(statuses, status) { status = it } }
        item { ProtoChipRow(listOf("Все даты", "Сегодня", "Завтра", "Неделя", "Просрочены"), dateFilter) { dateFilter = it } }
        if (filtered.isEmpty()) item { ProtoEmptyState(Icons.Outlined.ReceiptLong, if (qq.isNotBlank()) "Заказ «$qq» не найден" else "Заказов нет", "Измените поиск или фильтры") }
        else items(filtered, key = { it.id }) { o -> ProtoAdminOrderRow(o, clientTypeOf(o)) { onOpen(o) } }
    }
}

/** Строка заказа у администратора: тип клиента, срок доставки (просрочен/сегодня выделены), оплата. */
@Composable
internal fun ProtoAdminOrderRow(o: ProtoOrder, clientType: String, onClick: () -> Unit) {
    val today = LocalDate.now()
    val d = protoParseDate(o.deliveryDate)
    val active = o.status !in protoHistoryStatuses
    val overdue = active && d != null && d.isBefore(today)
    val dueToday = active && d == today
    ProtoSectionCard(Modifier.clickable(onClick = onClick).then(if (overdue) Modifier.border(1.dp, ProtoRed, RoundedCornerShape(18.dp)) else if (dueToday) Modifier.border(1.dp, ProtoOrange, RoundedCornerShape(18.dp)) else Modifier)) {
        Row {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("№ " + o.id, color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    if (clientType.isNotBlank()) ProtoClientTypeBadge(clientType)
                }
                Text(o.clientName + if (o.retailCustomerName.isNotBlank()) " → " + o.retailCustomerName else "", color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${o.pieces} шт. · оформлен ${o.dateTime.substringBefore(" ")}", color = ProtoMuted, fontSize = 12.sp)
                if (o.deliveryDate.isNotBlank()) Text(
                    (if (overdue) "Просрочен · " else if (dueToday) "Сегодня · " else "") + (if (o.deliveryMethod == "Самовывоз") "самовывоз " else "доставка ") + o.deliveryDate + (if (o.deliveryTime.isNotBlank()) " · " + o.deliveryTime else ""),
                    color = if (overdue) ProtoRed else if (dueToday) ProtoOrange else ProtoText, fontSize = 12.sp, fontWeight = if (overdue || dueToday) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(protoMoney(o.total), color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                ProtoPill(o.status, protoStatusColor(o.status))
                if (o.status !in setOf("Отменён", "Отклонён")) {
                    Spacer(Modifier.height(4.dp))
                    Text(when (o.paymentStatus) { "Оплачен полностью" -> "оплачен"; "Оплачен частично" -> "оплачен частично"; else -> "не оплачен" }, color = when (o.paymentStatus) { "Оплачен полностью" -> ProtoGreen; "Оплачен частично" -> ProtoOrange; else -> ProtoMuted }, fontSize = 11.sp)
                }
            }
        }
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
internal fun ProtoAdminSettingsMenuScreen(syncStatus: String, lastSync: String, lowStockThreshold: Int, pickupCount: Int, onBack: () -> Unit, onOpen: (String) -> Unit, onCall: () -> Unit, onLogout: () -> Unit, minOrder: Int = 35_000, showGodEye: Boolean = false) {
    val rows = listOfNotNull(
        if (showGodEye) Triple("Глаз бога", Icons.Outlined.Visibility, "Войти в аккаунт любого клиента или производства") else null,
        Triple("Профиль компании", Icons.Outlined.Business, "Данные SANSARA и контакты"),
        Triple("Рассылка", Icons.Outlined.Send, "Сообщение всем клиентам, агентам или торговым организациям"),
        Triple("Обновление приложения", Icons.Outlined.SystemUpdate, "Опубликовать новую версию для клиентов и сотрудников"),
        Triple("Проведение акций", Icons.Outlined.LocalOffer, "Акционный каталог из Tilda, сроки, уведомление клиентам"),
        Triple("Приветственный экран", Icons.Outlined.Campaign, "Рекламный текст при запуске приложения"),
        Triple("Сотрудники и коды", Icons.Outlined.AdminPanelSettings, "Коды входа администраторов и производства"),
        Triple("Сборщицы", Icons.Outlined.Badge, "Справочник производства"),
        Triple("Ставки сборки", Icons.Outlined.Payments, "Оплата сборщицам за штуку"),
        Triple("Точки самовывоза", Icons.Outlined.Store, if (pickupCount > 0) protoPlural(pickupCount, "точка", "точки", "точек") else "Не заданы — клиент вводит адрес сам"),
        Triple("Задание в цех", Icons.Outlined.Assignment, "План и факт цеха"),
        Triple("Табель рабочего времени", Icons.Outlined.EventAvailable, "Кто и когда выходил"),
        Triple("Отчёты", Icons.Outlined.Assessment, "Показатели за период"),
        Triple("Клиенты", Icons.Outlined.PersonSearch, "Доступ, статусы и скидки"),
        Triple("Каталог и синхронизация", Icons.Outlined.Sync, "$syncStatus · $lastSync"),
        Triple("Telegram интеграция", Icons.Outlined.Send, "Заказы через серверный прокси"),
        Triple("Минимальный заказ", Icons.Outlined.ShoppingCartCheckout, "Торгующие организации: " + protoMoney(minOrder)),
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
    onRate: (List<String>, Int) -> Unit,
    promoTitle: String = "",
    promoText: String = "",
    onPromoSave: (String, String, ProtoPromoStyle) -> Unit = { _, _, _ -> },
    promoStyle: ProtoPromoStyle = ProtoPromoStyle(),
    promoImageVersion: Long = 0L,
    minOrder: Int = 35_000,
    onMinOrder: (Int) -> Unit = {},
    onPromoImage: (android.net.Uri) -> Unit = {},
    onPromoImageReset: () -> Unit = {}
) {
    val pickImage = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPromoImage(uri) }
    // «Ставки сборки»: какие разделы раскрыты и строка поиска.
    var expandedRateGroups by remember { mutableStateOf(setOf<String>()) }
    var rateQuery by remember { mutableStateOf("") }
    ProtoScaffold(section, null, onBack) {
        when (section) {
            "Минимальный заказ" -> item {
                var v by remember(minOrder) { mutableStateOf(minOrder.toString()) }
                ProtoSectionCard {
                    Text("Минимальная сумма заказа для торгующих организаций (со скидкой). Заказ меньше этой суммы клиент может отправить только на согласование. У агентов минимума нет.", color = ProtoMuted, fontSize = 13.sp)
                    OutlinedTextField(v, { v = it.filter(Char::isDigit).take(7) }, label = { Text("Сумма, ₽ (0 — без минимума)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors(), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    ProtoPrimaryButton("Сохранить", { onMinOrder(v.toIntOrNull() ?: 0) })
                }
            }
            "Приветственный экран" -> item {
                var title by remember(promoTitle) { mutableStateOf(promoTitle) }
                var text by remember(promoText) { mutableStateOf(promoText) }
                var style by remember(promoStyle) { mutableStateOf(promoStyle) }
                val ctx = LocalContext.current
                ProtoSectionCard {
                    Text("Картинка экрана «Добро пожаловать»", color = ProtoText, fontWeight = FontWeight.SemiBold)
                    Text("Рекламная или информационная. Лучше вертикальная. Шрифт, размер и место надписей — ниже, в «Оформлении надписей».", color = ProtoMuted, fontSize = 12.sp)
                    // Предпросмотр в пропорциях карточки на экране приветствия.
                    Box(Modifier.fillMaxWidth().aspectRatio(0.9f).padding(vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(ProtoPanel2)) {
                        val file = protoWelcomeImageFile(ctx).takeIf { promoImageVersion > 0L && it.exists() }
                        if (file != null) coil.compose.AsyncImage(model = coil.request.ImageRequest.Builder(ctx).data(file).memoryCacheKey("welcome-$promoImageVersion").build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        else androidx.compose.foundation.Image(painterResource(R.drawable.mock_wreath), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)))
                        // Предпросмотр: те же шрифты, размеры и места, что на экране приветствия.
                        ProtoPromoOverlay(title, text, style, padding = 18.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { pickImage.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), modifier = Modifier.weight(1f)) { Text("Выбрать картинку", color = Color.Black) }
                        if (promoImageVersion > 0L) OutlinedButton(onClick = onPromoImageReset, border = BorderStroke(1.dp, ProtoGold), modifier = Modifier.weight(1f)) { Text("Вернуть стандартную", color = ProtoGold, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                ProtoSectionCard {
                    Text("Текст на фото экрана «Добро пожаловать». Пусто — «Уважение в каждой детали».", color = ProtoMuted, fontSize = 13.sp)
                    OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, label = { Text("Заголовок (до 3 строк)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2, maxLines = 3, colors = protoFieldColors())
                    OutlinedTextField(value = text, onValueChange = { text = it.take(140) }, label = { Text("Акция или новость") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), minLines = 2, maxLines = 4, colors = protoFieldColors())
                    Text("Изменения сразу видны на картинке выше.", color = ProtoMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                ProtoSectionCard {
                    Text("Оформление надписей", color = ProtoText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    ProtoPromoTextStyleEditor("Заголовок", style.titleFont, style.titleSize, style.titlePos, ProtoPromoStyle.TITLE_SIZES,
                        onFont = { style = style.copy(titleFont = it) }, onSize = { style = style.copy(titleSize = it) }, onPos = { style = style.copy(titlePos = it) })
                    HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(top = 12.dp))
                    ProtoPromoTextStyleEditor("Акция или новость", style.textFont, style.textSize, style.textPos, ProtoPromoStyle.TEXT_SIZES,
                        onFont = { style = style.copy(textFont = it) }, onSize = { style = style.copy(textSize = it) }, onPos = { style = style.copy(textPos = it) })
                    if (style != ProtoPromoStyle()) TextButton(onClick = { style = ProtoPromoStyle() }) { Text("Вернуть оформление по умолчанию", color = ProtoGold) }
                }
                Spacer(Modifier.height(10.dp))
                ProtoPrimaryButton("Сохранить", { onPromoSave(title.trim(), text.trim(), style) })
                Text("Без сервера текст и оформление меняются только на этом телефоне.", color = ProtoOrange, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
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
                item { Text("Ставка — сколько получает сборщица за одну штуку. Производство видит ставку, но не меняет её. Нажмите на раздел, чтобы раскрыть список.", color = ProtoMuted, fontSize = 13.sp) }
                item { ProtoSearchField(rateQuery, { rateQuery = it }, {}, "Артикул или название") }
                val q = rateQuery.trim()
                val source = if (q.length >= 2) products.filter { protoSearchMatches(it, q) } else products
                val order = protoAllGroupsOrdered
                val groups = source.groupBy { it.type.ifBlank { "Прочее" } }.toList().sortedBy { (t, _) -> order.indexOf(t).let { i -> if (i < 0) 99 else i } }
                if (groups.isEmpty()) item { Text("Ничего не найдено", color = ProtoMuted) }
                groups.forEach { (type, list) ->
                    // При поиске разделы с найденными товарами раскрываются сами.
                    val expanded = type in expandedRateGroups || q.length >= 2
                    item(key = "rg-$type") {
                        var groupRate by remember(type) { mutableStateOf("") }
                        val rated = list.count { (rates[it.sku] ?: 0) > 0 }
                        ProtoSectionCard(Modifier.clickable { expandedRateGroups = protoToggle(expandedRateGroups, type) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("$type · ${list.size}", color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(if (rated < list.size) "без ставки ${list.size - rated}" else "ставка задана у всех", color = if (rated < list.size) ProtoOrange else ProtoMuted, fontSize = 12.sp)
                                }
                                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Свернуть" else "Раскрыть", tint = ProtoGold)
                            }
                            if (expanded) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                OutlinedTextField(value = groupRate, onValueChange = { groupRate = it.filter(Char::isDigit).take(5) }, label = { Text("Ставка для всех, ₽") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors())
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { groupRate.toIntOrNull()?.let { onRate(list.map { it.sku }, it) }; groupRate = "" }, enabled = groupRate.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) { Text("Применить", color = Color.Black) }
                            }
                        }
                    }
                    if (expanded) items(list.sortedBy { it.sku }, key = { "r-" + it.sku }) { p ->
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
                if (r.monthlyVolume > 0) Text("Объём продаж ≈ " + protoMoneyLong(r.monthlyVolume) + " в месяц", color = ProtoGoldSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (r.contact2.isNotBlank()) Text("Второй контакт: " + r.contact2 + " · " + protoPhone(r.phone2) + if (r.email2.isNotBlank()) " · " + r.email2 else "", color = ProtoMuted, fontSize = 12.sp)
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
    var period by remember { mutableStateOf(ProtoPeriod.today()) }
    val range = period.from to period.to
    val periodTasks = tasks.filter { inRange(it.taskDate, range) }
    val plan = periodTasks.sumOf { protoWorkshopPlanQty(it.linesJson) }
    val fact = periodTasks.sumOf { it.actualQty }
    val ops = productionOps.filter { inRange(it.date, range) && it.status != "Удален" }
    val periodOrders = orders.filter { inRange(it.dateTime, range) && it.status != "Отменён" }
    val worked = attendance.filter { inRange(it.date, range) && it.status == "Работал" }
    var dayOff by remember(dailyStatus?.date, dailyStatus?.dayOff) { mutableStateOf(dailyStatus?.dayOff ?: false) }
    var reason by remember(dailyStatus?.date, dailyStatus?.reason) { mutableStateOf(dailyStatus?.reason.orEmpty()) }
    val rangeLabel = period.label
    ProtoScaffold("Отчёты", rangeLabel, onBack) {
        item { ProtoPeriodBar(period, { period = it ?: ProtoPeriod.today() }, maxDate = LocalDate.now()) }
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
        if (period == ProtoPeriod.today()) item {
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
    onFact: (WorkshopTaskEntity, Int, String) -> Unit,
    assemblers: List<SansaraAssembler> = emptyList(),
    onRun: (WorkshopTaskEntity, Boolean) -> Unit = { _, _ -> },
    onPost: (WorkshopTaskEntity, Map<String, Int>, String, SansaraAssembler?) -> Unit = { _, _, _, _ -> }
) {
    var openTask by remember { mutableStateOf<WorkshopTaskEntity?>(null) }
    // Тик раз в 30 секунд, чтобы время сборки на карточках шло.
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); tick = System.currentTimeMillis() } }
    openTask?.let { t0 ->
        val t = tasks.firstOrNull { it.id == t0.id } ?: t0
        ProtoWorkshopTaskDialog(t, products, assemblers, productionMode, tick, onDismiss = { openTask = null }, onRun = { run -> onRun(t, run) }, onPost = { done, comment, who -> onPost(t, done, comment, who); openTask = null })
    }
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
            val plan = protoWorkshopPlanQty(task.linesJson)
            val finishedTask = task.status in setOf("Собрано", "Выполнено", "Частично собрано")
            ProtoSectionCard(Modifier.clickable { openTask = task }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.taskDate, color = ProtoText, fontWeight = FontWeight.SemiBold)
                        if (task.carriedFrom.isNotBlank()) Text("Перенесено с " + task.carriedFrom + " · осталось " + plan + " шт.", color = ProtoOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        else Text(task.id, color = ProtoMuted, fontSize = 12.sp)
                    }
                    ProtoPill(protoWorkshopStatusLabel(task.status), protoWorkshopStatusColor(task.status))
                }
                if (task.commentOnly) Text("Комментарий: " + task.comment, color = ProtoText, modifier = Modifier.padding(top = 7.dp))
                else {
                    Text(protoWorkshopLinesLabel(task.linesJson), color = ProtoText, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                    if (task.comment.isNotBlank()) Text("Комментарий: " + task.comment, color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    if (task.masterComment.isNotBlank()) Text("Мастер: " + task.masterComment, color = ProtoGoldSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    ProtoInfoRow("План / собрано", "$plan / ${task.actualQty} шт.")
                    val ms = protoWorkshopElapsed(task, tick)
                    if (ms > 0) ProtoInfoRow(if (finishedTask) "Собрано за" else "Время в работе", protoDuration(ms))
                }
                if (productionMode && !task.commentOnly && !finishedTask) {
                    Spacer(Modifier.height(8.dp))
                    if (task.startedAt <= 0L) ProtoPrimaryButton("Взять в работу", { onRun(task, true) })
                    else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRun(task, task.runningSince <= 0L) }, border = BorderStroke(1.dp, ProtoGold), modifier = Modifier.weight(1f)) { Text(if (task.runningSince > 0L) "Пауза" else "Продолжить", color = ProtoGold) }
                        Button(onClick = { openTask = task }, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), modifier = Modifier.weight(1f)) { Text("Провести", color = Color.Black) }
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
    onAdd: (String, String, Boolean) -> Unit,
    onToggle: (SansaraAdminAccount) -> Unit,
    onIssuedSeen: () -> Unit,
    onRename: (SansaraAdminAccount, String) -> Unit = { _, _ -> },
    onLoadCode: (SansaraAdminAccount, (String) -> Unit) -> Unit = { _, _ -> },
    onReissue: (SansaraAdminAccount, (String) -> Unit) -> Unit = { _, _ -> },
    onPhone: (SansaraAdminAccount, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var card by remember { mutableStateOf<SansaraAdminAccount?>(null) }
    card?.let { opened ->
        val a = admins.firstOrNull { it.userId == opened.userId } ?: opened
        var code by remember(a.userId) { mutableStateOf<String?>(null) }
        var confirmReissue by remember(a.userId) { mutableStateOf(false) }
        var phoneEdit by remember(a.userId, a.phone) { mutableStateOf(protoPhoneDigits(a.phone)) }
        LaunchedEffect(a.userId) { onLoadCode(a) { code = it } }
        if (confirmReissue) AlertDialog(
            onDismissRequest = { confirmReissue = false }, containerColor = ProtoPanel,
            title = { Text("Выдать новый код?", color = ProtoText) },
            text = { Text("Старый код «" + a.displayName + "» перестанет работать.", color = ProtoMuted) },
            confirmButton = { TextButton(onClick = { confirmReissue = false; onReissue(a) { code = it } }) { Text("Выдать", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { confirmReissue = false }) { Text("Отмена", color = ProtoMuted) } }
        )
        androidx.compose.ui.window.Dialog(onDismissRequest = { card = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(color = ProtoPanel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(0.94f)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(a.displayName, color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text((if (a.isMain) "Главный аккаунт" else if (a.role == SansaraRole.PRODUCTION.name) "Производство" else "Администратор") + " · " + (if (a.enabled) "активен" else "отключён"), color = ProtoGoldSoft, fontSize = 13.sp)
                    Surface(color = ProtoGold.copy(alpha = .1f), border = BorderStroke(1.dp, ProtoGold.copy(alpha = .5f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Код входа", color = ProtoMuted, fontSize = 12.sp)
                            Text(code ?: "…", color = ProtoText, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { code?.let { protoShare(context, "SANSARA: ваш код входа $it. Откройте приложение, нажмите «Войти» и введите этот код.", "Отправить код") } }, enabled = !code.isNullOrBlank()) { Text("Отправить код", color = ProtoGold) }
                                TextButton(onClick = { confirmReissue = true }) { Text("Выдать новый", color = ProtoGold) }
                            }
                        }
                    }
                    OutlinedTextField(phoneEdit, { phoneEdit = protoPhoneDigits(it) }, label = { Text("Телефон") }, prefix = { Text("+7 ", color = ProtoText) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = protoFieldColors(), modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { if (phoneEdit != protoPhoneDigits(a.phone) && phoneEdit.length == 10) TextButton(onClick = { onPhone(a, protoPhone(phoneEdit)) }) { Text("Сохранить", color = ProtoGold) } })
                    ProtoContactActions(a.phone, "Здравствуйте, " + a.displayName + "!")
                    TextButton(onClick = { card = null }, modifier = Modifier.fillMaxWidth()) { Text("Закрыть", color = ProtoMuted) }
                }
            }
        }
    }
    var name by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<SansaraAdminAccount?>(null) }
    var newName by remember { mutableStateOf("") }
    renaming?.let { target ->
        AlertDialog(
            onDismissRequest = { renaming = null }, containerColor = ProtoPanel,
            title = { Text("Имя сотрудника", color = ProtoText) },
            text = {
                Column {
                    Text(if (target.isMain) "Главный аккаунт на главной экране всегда видит «Здравствуйте, BOSS»." else "Это имя будет в приветствии на главном экране сотрудника.", color = ProtoMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(newName, { newName = it.take(40) }, label = { Text("Имя") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(enabled = newName.isNotBlank(), onClick = { onRename(target, newName.trim()); renaming = null }) { Text("Сохранить", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
    var phone by remember { mutableStateOf("") }
    var production by remember { mutableStateOf(false) }
    val main = admins.firstOrNull { it.isMain }
    val canManage = main?.userId == currentUserId || currentUserId.isBlank()
    ProtoScaffold("Сотрудники и коды", "У каждого — свой код входа", onBack) {
        issued?.let { (who, code) ->
            item {
                Surface(color = ProtoGold.copy(alpha = .12f), border = BorderStroke(1.dp, ProtoGold), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Код для «$who»", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                        Text(code, color = ProtoText, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Код показывается один раз. Передайте его сотруднику.", color = ProtoMuted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = { protoShare(context, "SANSARA: ваш код входа $code. Откройте приложение, нажмите «Войти» и введите этот код.", "Отправить код") }, border = BorderStroke(1.dp, ProtoGold), modifier = Modifier.weight(1f)) { Text("Отправить", color = ProtoGold) }
                            TextButton(onClick = onIssuedSeen, modifier = Modifier.weight(1f)) { Text("Готово", color = ProtoMuted) }
                        }
                    }
                }
            }
        }
        item {
            ProtoSectionCard {
                if (canManage) {
                    Text("Новый сотрудник", color = ProtoText, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        listOf(false to "Администратор", true to "Производство").forEach { (value, label) ->
                            val sel = production == value
                            Surface(
                                onClick = { production = value },
                                shape = RoundedCornerShape(20.dp),
                                color = if (sel) ProtoGold else Color.Transparent,
                                border = BorderStroke(1.dp, if (sel) ProtoGold else ProtoBorder)
                            ) { Text(label, color = if (sel) Color.Black else ProtoText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
                        }
                    }
                    ProtoField(name, { name = it.take(60) }, "Имя")
                    ProtoField(phone, { phone = protoPhoneDigits(it) }, "Телефон (10 цифр)", KeyboardType.Phone)
                    ProtoPrimaryButton("Добавить и выдать код", { onAdd(name.trim(), phone, production); name = ""; phone = "" }, enabled = name.isNotBlank() && phone.length == 10)
                } else Text("Добавлять и отключать сотрудников может только главный аккаунт.", color = ProtoOrange, fontSize = 13.sp)
            }
        }
        items(admins, key = { it.userId }) { admin ->
            ProtoSectionCard(if (canManage) Modifier.clickable { card = admin } else Modifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) { Icon(if (admin.isMain) Icons.Outlined.VerifiedUser else if (admin.role == SansaraRole.PRODUCTION.name) Icons.Outlined.Factory else Icons.Outlined.AdminPanelSettings, null, tint = ProtoGold) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(admin.displayName, color = ProtoText, fontWeight = FontWeight.Bold)
                        if (admin.phone.isNotBlank()) Text(protoPhone(admin.phone), color = ProtoMuted, fontSize = 12.sp)
                        Text((if (admin.isMain) "Главный аккаунт" else if (admin.role == SansaraRole.PRODUCTION.name) "Производство" else "Администратор") + " · " + (if (admin.enabled) "активен" else "отключён"), color = if (admin.enabled) ProtoGoldSoft else ProtoRed, fontSize = 12.sp)
                    }
                    if (canManage) IconButton(onClick = { newName = admin.displayName; renaming = admin }) { Icon(Icons.Outlined.Edit, "Изменить имя", tint = ProtoGoldSoft) }
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
    var period by remember { mutableStateOf<ProtoPeriod?>(ProtoPeriod.thisMonth()) }
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
        if (periodic) item { ProtoPeriodBar(period, { period = it }, allowAll = true, maxDate = LocalDate.now()) }
        item {
            Button(onClick = { onExport(report, if (periodic) period?.key ?: "Всё время" else "Всё время") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) {
                Icon(Icons.Outlined.FileDownload, null, tint = Color.Black); Spacer(Modifier.width(7.dp)); Text("Выгрузить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────── Задание в цех: детали, проведение, время сборки ─────────────

internal fun protoWorkshopStatusLabel(status: String) = when (status) { "Получено" -> "Новое"; "Выполнено" -> "Собрано"; else -> status }
internal fun protoWorkshopStatusColor(status: String) = when (status) {
    "Собрано", "Выполнено" -> ProtoGreen
    "Частично собрано", "На паузе" -> ProtoOrange
    "В работе" -> ProtoGold
    else -> ProtoMuted
}
/** Время сборки без пауз на момент now. */
internal fun protoWorkshopElapsed(t: WorkshopTaskEntity, now: Long) = t.workMs + if (t.runningSince > 0) (now - t.runningSince).coerceAtLeast(0) else 0L

private data class WorkshopLine(val sku: String, val name: String, val plan: Int, val done: Int)

private fun workshopLines(json: String): List<WorkshopLine> = runCatching {
    val a = JSONArray(json.ifBlank { "[]" })
    (0 until a.length()).map { i -> a.getJSONObject(i).let { WorkshopLine(it.optString("sku"), it.optString("name").ifBlank { it.optString("category", "Позиция") }, it.optInt("qty", 0), it.optInt("done", 0)) } }
}.getOrDefault(emptyList())

@Composable
internal fun ProtoWorkshopTaskDialog(
    task: WorkshopTaskEntity,
    products: List<ProtoCatalogProduct>,
    assemblers: List<SansaraAssembler>,
    productionMode: Boolean,
    now: Long,
    onDismiss: () -> Unit,
    onRun: (Boolean) -> Unit,
    onPost: (Map<String, Int>, String, SansaraAssembler?) -> Unit
) {
    val lines = remember(task.linesJson) { workshopLines(task.linesJson) }
    val done = remember(task.id, task.linesJson) { mutableStateMapOf<String, Int>() }
    var comment by remember(task.id) { mutableStateOf(task.masterComment) }
    val active = assemblers.filter { it.enabled }
    var who by remember(task.id) { mutableStateOf(active.firstOrNull()) }
    var cardProduct by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var confirmPartial by remember { mutableStateOf(false) }
    val finished = task.status in setOf("Собрано", "Выполнено", "Частично собрано")
    val leftTotal = lines.sumOf { (it.plan - it.done).coerceAtLeast(0) }
    val nowTotal = lines.sumOf { (done[it.sku] ?: 0).coerceAtMost((it.plan - it.done).coerceAtLeast(0)) }
    cardProduct?.let { p ->
        AlertDialog(
            onDismissRequest = { cardProduct = null }, containerColor = ProtoPanel,
            title = { Text(protoTitle(p), color = ProtoText) },
            text = {
                Column {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(ProtoPanel2)) { ProtoProductImage(p, Modifier.fillMaxSize().padding(6.dp), androidx.compose.ui.layout.ContentScale.Fit) }
                    Spacer(Modifier.height(8.dp))
                    ProtoInfoRow("Артикул", p.sku)
                    if (p.size.isNotBlank()) ProtoInfoRow("Размер", p.size)
                    if (p.quality.isNotBlank()) ProtoInfoRow("Качество", p.quality)
                    if (p.type.isNotBlank()) ProtoInfoRow("Группа", p.type)
                }
            },
            confirmButton = { TextButton(onClick = { cardProduct = null }) { Text("Закрыть", color = ProtoGold) } }
        )
    }
    if (confirmPartial) AlertDialog(
        onDismissRequest = { confirmPartial = false }, containerColor = ProtoPanel,
        title = { Text("Провести частично?", color = ProtoText) },
        text = { Text("Собрано $nowTotal из $leftTotal шт. Собранное будет оприходовано, задание получит статус «Частично собрано», а остаток (${leftTotal - nowTotal} шт.) перейдёт на следующий день с отметкой о переносе.", color = ProtoMuted, lineHeight = 20.sp) },
        confirmButton = { TextButton(onClick = { confirmPartial = false; onPost(done.toMap(), comment, who) }) { Text("Провести частично", color = ProtoGold) } },
        dismissButton = { TextButton(onClick = { confirmPartial = false }) { Text("Назад", color = ProtoMuted) } }
    )
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = ProtoPanel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Задание на " + task.taskDate, color = ProtoText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (task.carriedFrom.isNotBlank()) Text("Перенесено с " + task.carriedFrom, color = ProtoOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    ProtoPill(protoWorkshopStatusLabel(task.status), protoWorkshopStatusColor(task.status))
                }
                val ms = protoWorkshopElapsed(task, now)
                if (ms > 0) Text((if (finished) "Собрано за " else "В работе: ") + protoDuration(ms) + if (task.runningSince <= 0 && !finished) " · пауза" else "", color = ProtoGoldSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                if (task.comment.isNotBlank()) Text("Задание: " + task.comment, color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lines, key = { it.sku.ifBlank { it.name } }) { line ->
                        val product = products.firstOrNull { it.sku == line.sku }
                        val left = (line.plan - line.done).coerceAtLeast(0)
                        val q = done[line.sku] ?: 0
                        ProtoSectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(ProtoPanel2).clickable(enabled = product != null) { cardProduct = product }) {
                                    if (product != null) ProtoProductImage(product, Modifier.fillMaxSize().padding(3.dp), androidx.compose.ui.layout.ContentScale.Fit)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f).clickable(enabled = product != null) { cardProduct = product }) {
                                    Text(line.name, color = ProtoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (line.sku.isNotBlank()) Text("Арт. " + line.sku, color = ProtoGoldSoft, fontSize = 12.sp)
                                    Text("Нужно ${line.plan} · собрано ${line.done}" + if (left > 0) " · осталось $left" else " · готово", color = if (left > 0) ProtoMuted else ProtoGreen, fontSize = 12.sp)
                                }
                            }
                            if (productionMode && !finished && left > 0) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                Text("Собрано сейчас", color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                ProtoQtyButton(Icons.Outlined.Remove, { done[line.sku] = (q - 1).coerceAtLeast(0) }, 36.dp)
                                Text(q.toString(), color = if (q > 0) ProtoGold else ProtoMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                ProtoQtyButton(Icons.Outlined.Add, { done[line.sku] = (q + 1).coerceAtMost(left) }, 36.dp)
                                TextButton(onClick = { done[line.sku] = left }) { Text("Все", color = ProtoGold) }
                            }
                        }
                    }
                    if (productionMode && !finished) item {
                        OutlinedTextField(comment, { comment = it.take(300) }, label = { Text("Комментарий мастера") }, minLines = 2, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                    }
                    if (productionMode && !finished && active.isNotEmpty()) item {
                        Text("Кто собрал (оплата по ставке сборки)", color = ProtoMuted, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 4.dp)) {
                            items(active, key = { it.id }) { a ->
                                FilterChip(selected = who?.id == a.id, onClick = { who = a }, label = { Text(a.name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText))
                            }
                        }
                    }
                    if (task.masterComment.isNotBlank() && (finished || !productionMode)) item { Text("Комментарий мастера: " + task.masterComment, color = ProtoGoldSoft, fontSize = 13.sp) }
                }
                if (productionMode && !finished && !task.commentOnly) {
                    if (task.startedAt <= 0L) ProtoPrimaryButton("Взять в работу", { onRun(true) })
                    else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRun(task.runningSince <= 0L) }, border = BorderStroke(1.dp, ProtoGold), modifier = Modifier.weight(1f).height(48.dp)) { Text(if (task.runningSince > 0L) "Пауза" else "Продолжить", color = ProtoGold) }
                        Button(
                            onClick = { if (nowTotal < leftTotal) confirmPartial = true else onPost(done.toMap(), comment, who) },
                            enabled = nowTotal > 0, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text(if (nowTotal in 1 until leftTotal) "Провести частично" else "Провести", color = Color.Black, maxLines = 1) }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть", color = ProtoMuted) }
            }
        }
    }
}

// ───────────── «Глаз бога»: выбор аккаунта ─────────────

@Composable
internal fun ProtoGodEyePicker(targets: List<SansaraSession>, clientName: (String) -> String, onDismiss: () -> Unit, onPick: (SansaraSession) -> Unit) {
    var tab by remember { mutableStateOf("Клиенты") }
    var q by remember { mutableStateOf("") }
    val shown = targets.filter { t ->
        (if (tab == "Клиенты") t.role == SansaraRole.CLIENT else t.role == SansaraRole.PRODUCTION) &&
            (q.isBlank() || t.firstName.contains(q, true) || clientName(t.clientId.orEmpty()).contains(q, true))
    }.sortedWith(compareBy({ clientName(it.clientId.orEmpty()) }, { it.firstName }))
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = ProtoPanel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.85f)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Visibility, null, tint = ProtoGold)
                    Spacer(Modifier.width(8.dp))
                    Text("Глаз бога", color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text("Войдите в любой аккаунт без кода. Сверху будет плашка и кнопка «В BOSS». Перед каждым изменением приложение переспросит.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                ProtoChipRow(listOf("Клиенты", "Производство"), tab) { tab = it }
                OutlinedTextField(q, { q = it }, label = { Text("Поиск: компания или имя") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (shown.isEmpty()) item { Text("Аккаунтов нет", color = ProtoMuted) }
                    items(shown, key = { it.userId }) { t ->
                        ProtoSectionCard(Modifier.clickable { onPick(t) }) {
                            val company = clientName(t.clientId.orEmpty())
                            Text(if (company.isNotBlank()) company else t.firstName, color = ProtoText, fontWeight = FontWeight.Bold)
                            Text(if (company.isNotBlank()) "Контакт: " + t.firstName else "Производство", color = ProtoMuted, fontSize = 12.sp)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть", color = ProtoMuted) }
            }
        }
    }
}
