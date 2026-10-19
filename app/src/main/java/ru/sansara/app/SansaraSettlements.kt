package ru.sansara.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ───────────── Расчёты с клиентами: оплаты, отсрочка, итоги ─────────────
//
// Оборот = Оплачено + В отсрочке + Просрочено + К оплате (без отсрочки).
// «Оплачено» — только подтверждённые деньги. Сообщённое клиентом ждёт подтверждения отдельно.
// Отсрочка считается со дня отгрузки (заказ перешёл в «Доставляется» или сразу в «Доставлен»).

enum class ProtoPayState(val label: String) { PAID("Оплачен"), DEFERRED("Отсрочка"), OVERDUE("Просрочен"), DUE("К оплате") }

data class ProtoOrderTerms(
    /** 0 — без отсрочки. */
    val days: Int,
    /** "order" — задано в заказе, "client" — правило клиента, "none" — без отсрочки. */
    val source: String,
    val shipDate: LocalDate?,
    val dueDate: LocalDate?,
    val state: ProtoPayState,
    /** До срока оплаты, дней (отрицательное — просрочка). null — срок ещё не начался (не отгружен) или без отсрочки. */
    val daysLeft: Int?
) {
    val label: String get() = when {
        state == ProtoPayState.PAID -> "Оплачен"
        days <= 0 -> "Без отсрочки"
        dueDate == null -> "Отсрочка $days дн. после отгрузки"
        state == ProtoPayState.OVERDUE -> "Просрочен на ${-(daysLeft ?: 0)} дн. (срок ${dueDate.format(protoSettleFmt)})"
        else -> "Отсрочка до ${dueDate.format(protoSettleFmt)}" + (daysLeft?.let { if (it == 0) " — сегодня" else " · $it дн." } ?: "")
    }
}

data class ProtoSettlement(
    val turnover: Int = 0,
    val paid: Int = 0,
    val deferred: Int = 0,
    val overdue: Int = 0,
    val due: Int = 0,
    val pending: Int = 0,
    val orders: Int = 0,
    val overdueOrders: Int = 0,
    val deferredOrders: Int = 0
) {
    /** Всё, что клиент ещё должен. */
    val unpaid: Int get() = deferred + overdue + due
    operator fun plus(o: ProtoSettlement) = ProtoSettlement(turnover + o.turnover, paid + o.paid, deferred + o.deferred, overdue + o.overdue, due + o.due,
        pending + o.pending, orders + o.orders, overdueOrders + o.overdueOrders, deferredOrders + o.deferredOrders)
}

internal val protoSettleFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
/** Статусы, которые не входят в оборот. */
internal val protoNotInTurnover = setOf("Отменён", "Отклонён", "На согласовании")
internal val protoDeferralPresets = listOf(7, 14, 21, 30, 45, 60)

/** День отгрузки: первый переход в «Доставляется» или «Доставлен». */
internal fun protoShipDate(o: ProtoOrder): LocalDate? =
    o.history.firstOrNull { it.status == "Доставляется" || it.status == "Доставлен" }?.dateTime
        ?.let { runCatching { LocalDate.parse(it.trim().substringBefore(" "), protoSettleFmt) }.getOrNull() }

internal fun protoOrderTerms(o: ProtoOrder, client: ProtoClient?, today: LocalDate = LocalDate.now()): ProtoOrderTerms {
    val (days, source) = when {
        o.deferralDays >= 0 -> o.deferralDays to (if (o.deferralDays > 0) "order" else "none")
        client != null && client.deferralEnabled && client.deferralDays > 0 -> client.deferralDays to "client"
        else -> 0 to "none"
    }
    val ship = protoShipDate(o)
    val dueDate = if (days > 0 && ship != null) ship.plusDays(days.toLong()) else null
    val left = dueDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }
    val state = when {
        o.total > 0 && o.paid >= o.total -> ProtoPayState.PAID
        days <= 0 -> ProtoPayState.DUE
        left != null && left < 0 -> ProtoPayState.OVERDUE
        else -> ProtoPayState.DEFERRED
    }
    return ProtoOrderTerms(days, source, ship, dueDate, state, left)
}

internal fun protoSettlement(orders: List<ProtoOrder>, clientOf: (ProtoOrder) -> ProtoClient?, today: LocalDate = LocalDate.now()): ProtoSettlement =
    orders.filter { it.status !in protoNotInTurnover }.fold(ProtoSettlement()) { acc, o ->
        val t = protoOrderTerms(o, clientOf(o), today)
        val paid = o.paid.coerceAtMost(o.total)
        val rest = o.total - paid
        acc + ProtoSettlement(
            turnover = o.total, paid = paid,
            deferred = if (t.state == ProtoPayState.DEFERRED) rest else 0,
            overdue = if (t.state == ProtoPayState.OVERDUE) rest else 0,
            due = if (t.state == ProtoPayState.DUE) rest else 0,
            pending = o.pendingPaid, orders = 1,
            overdueOrders = if (t.state == ProtoPayState.OVERDUE) 1 else 0,
            deferredOrders = if (t.state == ProtoPayState.DEFERRED && rest > 0) 1 else 0
        )
    }

internal fun protoNewPaymentId(): String = "PAY-" + java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

internal fun protoPayStateColor(s: ProtoPayState): Color = when (s) {
    ProtoPayState.PAID -> ProtoGreen
    ProtoPayState.DEFERRED -> ProtoGoldSoft
    ProtoPayState.OVERDUE -> ProtoRed
    ProtoPayState.DUE -> ProtoOrange
}

// ───────────── Блок итогов ─────────────

@Composable
internal fun ProtoSettlementCard(s: ProtoSettlement, title: String = "Расчёты", subtitle: String? = null, onOpen: ((String) -> Unit)? = null) {
    ProtoSectionCard {
        Text(title, color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = ProtoMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        SettleRow("Оборот", s.turnover, ProtoText, bold = true, hint = s.orders.toString() + " " + protoPluralWord(s.orders, "заказ", "заказа", "заказов"), onClick = onOpen?.let { { it("turnover") } })
        HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
        SettleRow("Оплачено", s.paid, ProtoGreen, onClick = onOpen?.let { { it("paid") } })
        SettleRow("В отсрочке", s.deferred, ProtoGoldSoft, hint = if (s.deferredOrders > 0) s.deferredOrders.toString() + " зак." else null, onClick = onOpen?.let { { it("deferred") } })
        if (s.overdue > 0) SettleRow("Просрочено", s.overdue, ProtoRed, hint = s.overdueOrders.toString() + " зак.", onClick = onOpen?.let { { it("overdue") } })
        if (s.due > 0) SettleRow("К оплате (без отсрочки)", s.due, ProtoOrange, onClick = onOpen?.let { { it("due") } })
        if (s.pending > 0) SettleRow("Ждут подтверждения", s.pending, ProtoMuted, hint = "сообщил клиент", onClick = onOpen?.let { { it("pending") } })
    }
}

@Composable
private fun SettleRow(label: String, value: Int, color: Color, bold: Boolean = false, hint: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (bold) ProtoText else ProtoMuted, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
            if (hint != null) Text(hint, color = ProtoMuted, fontSize = 11.sp)
        }
        Text(protoMoney(value), color = color, fontWeight = FontWeight.Bold, fontSize = if (bold) 18.sp else 15.sp)
        if (onClick != null) Text(" ›", color = ProtoMuted)
    }
}

internal fun protoPluralWord(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10; val m100 = n % 100
    return when { m10 == 1 && m100 != 11 -> one; m10 in 2..4 && m100 !in 12..14 -> few; else -> many }
}

// ───────────── Отсрочка: правило клиента ─────────────

@Composable
internal fun ProtoDeferralRuleCard(enabled: Boolean, days: Int, onSave: (Boolean, Int) -> Unit) {
    var on by remember(enabled) { mutableStateOf(enabled) }
    var d by remember(days) { mutableStateOf(if (days > 0) days else 14) }
    var custom by remember(days) { mutableStateOf(if (days > 0 && days !in protoDeferralPresets) days.toString() else "") }
    val changed = on != enabled || (on && d != days)
    ProtoSectionCard {
        Text("Отсрочка платежа", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
        Text("Правило на все отгрузки клиента. В любом заказе можно поставить своё.", color = ProtoMuted, fontSize = 12.sp)
        ProtoSwitchRow(if (on) "Отсрочка включена" else "Отсрочка выключена", on) { on = it }
        if (on) {
            Text("Дней после отгрузки", color = ProtoMuted, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(protoDeferralPresets) { n ->
                    FilterChip(selected = d == n && custom.isBlank(), onClick = { d = n; custom = "" }, label = { Text("$n дн.", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText))
                }
            }
            OutlinedTextField(custom, { v -> custom = v.filter(Char::isDigit).take(3); custom.toIntOrNull()?.takeIf { it in 1..365 }?.let { d = it } },
                label = { Text("Другое количество дней") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
        }
        if (changed) {
            Spacer(Modifier.height(8.dp))
            ProtoPrimaryButton(if (on) "Сохранить: отсрочка $d дн." else "Сохранить: без отсрочки", { onSave(on, if (on) d else 0) })
        }
    }
}

// ───────────── Оплата в заказе ─────────────

/** Условия оплаты конкретного заказа: по правилу клиента, своя отсрочка или без отсрочки. */
@Composable
internal fun ProtoOrderTermsDialog(order: ProtoOrder, client: ProtoClient?, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var mode by remember { mutableStateOf(when { order.deferralDays < 0 -> "client"; order.deferralDays == 0 -> "none"; else -> "days" }) }
    var days by remember { mutableStateOf(if (order.deferralDays > 0) order.deferralDays.toString() else (client?.deferralDays?.takeIf { it > 0 } ?: 14).toString()) }
    val clientRule = if (client?.deferralEnabled == true && client.deferralDays > 0) "отсрочка " + client.deferralDays + " дн." else "без отсрочки"
    val n = days.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ProtoPanel,
        title = { Text("Условия оплаты № " + order.id, color = ProtoText) },
        text = {
            Column {
                listOf("client" to "По правилу клиента ($clientRule)", "days" to "Отсрочка по этому заказу", "none" to "Без отсрочки").forEach { (k, label) ->
                    Row(Modifier.fillMaxWidth().clickable { mode = k }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == k, onClick = { mode = k }, colors = RadioButtonDefaults.colors(selectedColor = ProtoGold))
                        Text(label, color = ProtoText, fontSize = 14.sp)
                    }
                }
                if (mode == "days") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        items(protoDeferralPresets) { p ->
                            FilterChip(selected = n == p, onClick = { days = p.toString() }, label = { Text("$p", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText))
                        }
                    }
                    OutlinedTextField(days, { days = it.filter(Char::isDigit).take(3) }, label = { Text("Дней после отгрузки") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                }
                Text("Срок считается со дня отгрузки.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = mode != "days" || n in 1..365, onClick = { onSave(when (mode) { "client" -> -1; "none" -> 0; else -> n }) }) { Text("Сохранить", color = ProtoGold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = ProtoMuted) } }
    )
}

/** Клиент сообщает об оплате: деньги засчитаются после подтверждения администратором. */
@Composable
internal fun ProtoReportPaymentDialog(order: ProtoOrder, onDismiss: () -> Unit, onSave: (Int, String, String, String) -> Unit) {
    var amount by remember { mutableStateOf((order.due - order.pendingPaid).coerceAtLeast(0).toString()) }
    var method by remember { mutableStateOf("Наличные водителю") }
    var date by remember { mutableStateOf(currentDateShort()) }
    var comment by remember { mutableStateOf("") }
    val value = amount.toIntOrNull() ?: 0
    val dateOk = protoParseDate(date) != null
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ProtoPanel,
        title = { Text("Сообщить об оплате", color = ProtoText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Заказ № " + order.id + " · осталось " + protoMoney(order.due), color = ProtoMuted, fontSize = 13.sp)
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(9) }, label = { Text("Сумма, ₽") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(listOf("Наличные водителю", "Наличные в офисе"), listOf("Перевод / СБП", "Расчётный счёт")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { m ->
                                val sel = method == m
                                Surface(onClick = { method = m }, shape = RoundedCornerShape(16.dp), color = if (sel) ProtoGold else Color.Transparent, border = BorderStroke(1.dp, if (sel) ProtoGold else ProtoBorder)) {
                                    Text(m, color = if (sel) Color.Black else ProtoText, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(date, { date = it.take(10) }, label = { Text("Дата (дд.мм.гггг)") }, singleLine = true, isError = !dateOk, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(comment, { comment = it.take(120) }, label = { Text("Кому передали / комментарий") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                Text("Оплата засчитается, когда администратор подтвердит, что деньги получены.", color = ProtoOrange, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(enabled = value > 0 && dateOk, onClick = { onSave(value, method, date, comment.trim()) }) { Text("Отправить", color = ProtoGold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = ProtoMuted) } }
    )
}

/** Раздел «Оплата» в заказе — у администратора и у клиента. */
@Composable
internal fun ProtoOrderPaymentSection(
    o: ProtoOrder,
    terms: ProtoOrderTerms?,
    isAdmin: Boolean,
    onAdminPayment: (() -> Unit)?,
    onReport: (() -> Unit)?,
    onConfirm: ((String) -> Unit)?,
    onRejectPayment: ((String) -> Unit)?,
    onTerms: (() -> Unit)?
) {
    ProtoSectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Оплата", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            val st = terms?.state
            if (st != null) ProtoPill(if (st == ProtoPayState.PAID) "Оплачен" else if (o.paid > 0) "Частично · " + st.label.lowercase() else st.label, protoPayStateColor(st))
        }
        if (terms != null) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text(terms.label + when (terms.source) { "client" -> " · правило клиента"; "order" -> " · для этого заказа"; else -> "" },
                color = protoPayStateColor(terms.state), fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (isAdmin && onTerms != null && terms.state != ProtoPayState.PAID) TextButton(onClick = onTerms) { Text("Условия", color = ProtoGold) }
        }
        ProtoInfoRow("Сумма заказа", protoMoney(o.total))
        if (o.paid > 0) ProtoInfoRow("Оплачено", protoMoney(o.paid))
        if (o.due > 0) ProtoInfoRow("Осталось оплатить", protoMoney(o.due))
        o.payments.forEach { p ->
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(ProtoPanel2).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(protoMoney(p.amount) + " · " + p.method, color = ProtoText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (p.confirmed) "получено" else "ждёт подтверждения", color = if (p.confirmed) ProtoGreen else ProtoOrange, fontSize = 12.sp)
                }
                Text(p.date + (if (p.fromClient) " · сообщил клиент" else "") + (if (p.by.isNotBlank()) " · " + p.by else "") + (if (p.comment.isNotBlank()) " · " + p.comment else ""),
                    color = ProtoMuted, fontSize = 12.sp)
                if (p.confirmed && p.confirmedBy.isNotBlank()) Text("Подтвердил: " + p.confirmedBy, color = ProtoMuted, fontSize = 12.sp)
                if (!p.confirmed && isAdmin && onConfirm != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Button(onClick = { onConfirm(p.id) }, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), modifier = Modifier.weight(1f)) { Text("Деньги получены", color = Color.Black, maxLines = 1) }
                    if (onRejectPayment != null) OutlinedButton(onClick = { onRejectPayment(p.id) }, border = BorderStroke(1.dp, ProtoRed), modifier = Modifier.weight(1f)) { Text("Не получены", color = ProtoRed, maxLines = 1) }
                }
            }
        }
        val left = o.due - o.pendingPaid
        if (isAdmin && onAdminPayment != null && o.due > 0) { Spacer(Modifier.height(8.dp)); ProtoSecondaryButton("Отметить оплату — деньги получены", onAdminPayment) }
        if (!isAdmin && onReport != null && left > 0) { Spacer(Modifier.height(8.dp)); ProtoSecondaryButton("Сообщить об оплате", onReport) }
    }
}

// ───────────── Сводка администратора ─────────────

@Composable
internal fun ProtoAdminSettlementsScreen(
    orders: List<ProtoOrder>,
    clients: List<ProtoClient>,
    clientOf: (ProtoOrder) -> ProtoClient?,
    salesPending: Int,
    onBack: () -> Unit,
    onOpenOrder: (ProtoOrder) -> Unit,
    onOpenClient: (ProtoClient) -> Unit,
    onConfirm: (ProtoOrder, String) -> Unit,
    onSalesPayments: () -> Unit,
    initialTab: String = "pending"
) {
    var tab by remember { mutableStateOf(initialTab) }
    var period by remember { mutableStateOf<ProtoPeriod?>(null) }
    val inPeriod = orders.filter { period == null || period!!.containsText(it.dateTime) }
    val total = protoSettlement(inPeriod, clientOf)
    val counted = inPeriod.filter { it.status !in protoNotInTurnover }
    val withTerms = counted.map { it to protoOrderTerms(it, clientOf(it)) }
    val pendingList = orders.filter { o -> o.payments.any { !it.confirmed } }
    ProtoScaffold("Расчёты с клиентами", "Оборот · оплачено · отсрочка", onBack) {
        item { ProtoPeriodBar(period, { period = it }, allowAll = true) }
        item { ProtoSettlementCard(total, "Все клиенты", period?.label ?: "За всё время", onOpen = { tab = if (it == "turnover") "clients" else it }) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val tabs = listOf("pending" to "Ждут подтверждения · " + pendingList.size, "overdue" to "Просрочено · " + total.overdueOrders,
                    "deferred" to "В отсрочке · " + total.deferredOrders, "due" to "К оплате", "clients" to "По клиентам")
                items(tabs) { (k, label) ->
                    FilterChip(selected = tab == k, onClick = { tab = k }, label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText))
                }
            }
        }
        when (tab) {
            "pending" -> {
                if (pendingList.isEmpty()) item { Text("Нет оплат, ожидающих подтверждения", color = ProtoMuted) }
                pendingList.forEach { o ->
                    o.payments.filter { !it.confirmed }.forEach { p ->
                        item(key = "p-" + o.id + p.id) {
                            ProtoSectionCard {
                                Text(o.clientName, color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Заказ № " + o.id + " · " + p.date + " · " + p.method + if (p.comment.isNotBlank()) " · " + p.comment else "", color = ProtoMuted, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                    Text(protoMoney(p.amount), color = ProtoGoldSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { onOpenOrder(o) }) { Text("Заказ", color = ProtoMuted) }
                                    Button(onClick = { onConfirm(o, p.id) }, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold)) { Text("Деньги получены", color = Color.Black) }
                                }
                            }
                        }
                    }
                }
                if (salesPending > 0) item { ProtoClickableRow("Отгрузки отдела продаж: ждут подтверждения $salesPending", Icons.Outlined.LocalShipping, onSalesPayments) }
            }
            "overdue", "deferred", "due", "paid" -> {
                val want = when (tab) { "overdue" -> ProtoPayState.OVERDUE; "deferred" -> ProtoPayState.DEFERRED; "due" -> ProtoPayState.DUE; else -> ProtoPayState.PAID }
                val list = withTerms.filter { (o, t) -> t.state == want && (want == ProtoPayState.PAID || o.due > 0) }
                    .sortedBy { (_, t) -> t.dueDate ?: LocalDate.MAX }
                if (list.isEmpty()) item { Text("Заказов нет", color = ProtoMuted) }
                list.forEach { (o, t) -> item(key = "o-" + o.id) { ProtoSettleOrderRow(o, t) { onOpenOrder(o) } } }
            }
            "clients" -> {
                val rows = clients.map { c -> c to protoSettlement(inPeriod.filter { it.clientId == c.id }, clientOf) }
                    .filter { it.second.orders > 0 }.sortedByDescending { it.second.unpaid }
                if (rows.isEmpty()) item { Text("Заказов нет", color = ProtoMuted) }
                rows.forEach { (c, s) ->
                    item(key = "c-" + c.id) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ProtoPanel).clickable { onOpenClient(c) }.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.name, color = ProtoText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (c.deferralEnabled && c.deferralDays > 0) "отсрочка " + c.deferralDays + " дн." else "без отсрочки", color = ProtoMuted, fontSize = 12.sp)
                            }
                            Text("Оборот " + protoMoney(s.turnover) + " · оплачено " + protoMoney(s.paid), color = ProtoMuted, fontSize = 12.sp)
                            Text("В отсрочке " + protoMoney(s.deferred) + (if (s.overdue > 0) " · просрочено " + protoMoney(s.overdue) else "") + (if (s.due > 0) " · к оплате " + protoMoney(s.due) else ""),
                                color = if (s.overdue > 0) ProtoRed else ProtoGoldSoft, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProtoSettleOrderRow(o: ProtoOrder, t: ProtoOrderTerms, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ProtoPanel).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("№ " + o.id + " · " + o.clientName, color = ProtoText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.label, color = protoPayStateColor(t.state), fontSize = 12.sp)
        }
        Text(protoMoney(if (t.state == ProtoPayState.PAID) o.paid else o.due), color = protoPayStateColor(t.state), fontWeight = FontWeight.Bold)
    }
}
