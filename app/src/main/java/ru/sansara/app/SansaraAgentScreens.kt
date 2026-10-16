package ru.sansara.app

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ───────────── Локальное хранение настроек агента и расходов клиента ─────────────

internal data class ProtoClientExpense(val id: String, val date: String, val category: String, val amount: Int, val comment: String)

internal fun protoLoadClientExpenses(prefs: SharedPreferences, clientId: String): List<ProtoClientExpense> = runCatching {
    val a = JSONArray(prefs.getString("client_expenses_$clientId", "[]").orEmpty())
    (0 until a.length()).map { i -> a.getJSONObject(i).let { ProtoClientExpense(it.optString("id"), it.optString("date"), it.optString("category"), it.optInt("amount"), it.optString("comment")) } }
}.getOrDefault(emptyList())

internal fun protoSaveClientExpenses(prefs: SharedPreferences, clientId: String, list: List<ProtoClientExpense>) {
    val a = JSONArray()
    list.forEach { e -> a.put(JSONObject().put("id", e.id).put("date", e.date).put("category", e.category).put("amount", e.amount).put("comment", e.comment)) }
    prefs.edit().putString("client_expenses_$clientId", a.toString()).apply()
}

/** Наценка агента на конкретные артикулы: sku → %. */
internal fun protoLoadSkuMarkups(prefs: SharedPreferences, ownerId: String): Map<String, Int> = runCatching {
    val o = JSONObject(prefs.getString("agent_sku_markup_$ownerId", "{}").orEmpty())
    o.keys().asSequence().associateWith { o.optInt(it) }
}.getOrDefault(emptyMap())

internal fun protoSaveSkuMarkups(prefs: SharedPreferences, ownerId: String, map: Map<String, Int>) {
    val o = JSONObject(); map.forEach { (k, v) -> o.put(k, v) }
    prefs.edit().putString("agent_sku_markup_$ownerId", o.toString()).apply()
}

/** Скидка, которую агент даёт своему частному клиенту, %. */
internal fun protoCustomerDiscount(prefs: SharedPreferences, customerId: String): Int = prefs.getInt("agent_customer_discount_$customerId", 0)
internal fun protoSetCustomerDiscount(prefs: SharedPreferences, customerId: String, pct: Int) = prefs.edit().putInt("agent_customer_discount_$customerId", pct.coerceIn(0, 90)).apply()

// ───────────── Периоды ─────────────

internal val protoReportPeriods = listOf("Этот месяц", "Прошлый месяц", "Квартал", "Год", "Всё время")

internal fun protoPeriodRange(period: String): Pair<LocalDate, LocalDate>? {
    val today = LocalDate.now()
    return when (period) {
        "Этот месяц" -> today.withDayOfMonth(1) to today
        "Прошлый месяц" -> YearMonth.from(today).minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
        "Квартал" -> today.minusMonths(3).plusDays(1) to today
        "Год" -> today.withDayOfYear(1) to today
        else -> null
    }
}

internal fun protoInRange(date: String, range: Pair<LocalDate, LocalDate>?): Boolean {
    if (range == null) return true
    val d = protoParseDate(date.substringBefore(" ")) ?: return false
    return !d.isBefore(range.first) && !d.isAfter(range.second)
}

internal fun protoRangeLabel(range: Pair<LocalDate, LocalDate>?): String {
    if (range == null) return "за всё время"
    val f = DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale)
    return range.first.format(f) + " — " + range.second.format(f)
}

private val clientExpenseCategories = listOf("Аренда", "Зарплата", "Доставка", "Реклама", "Связь", "Прочее")

@Composable
private fun StatCard(title: String, value: String, subtitle: String, modifier: Modifier, valueColor: Color = ProtoText, onClick: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, if (onClick != null) ProtoGold.copy(alpha = .5f) else ProtoBorder),
        shape = RoundedCornerShape(14.dp), modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (onClick != null) Icon(Icons.Outlined.ChevronRight, null, tint = ProtoGold, modifier = Modifier.size(16.dp))
            }
            Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = ProtoGoldSoft, fontSize = 12.sp, maxLines = 1)
        }
    }
}

// ───────────── 17 · Отчётность клиента ─────────────

@Composable
internal fun ProtoClientReportsScreen(
    isAgent: Boolean,
    orders: List<ProtoOrder>,
    retailOrders: List<ProtoOrder>,
    shipments: List<ShipmentEntity>,
    expenses: List<ProtoClientExpense>,
    typeOf: (String) -> String,
    onBack: () -> Unit,
    onOpenOrder: (ProtoOrder) -> Unit,
    onOpenProduct: (String) -> Unit,
    onAddExpense: (String, Int, String) -> Unit,
    onDeleteExpense: (ProtoClientExpense) -> Unit
) {
    var period by remember { mutableStateOf("Этот месяц") }
    var showOrders by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProtoClientExpense?>(null) }
    val range = protoPeriodRange(period)
    val myOrders = orders.filter { it.status != "Отменён" && protoInRange(it.dateTime, range) }.sortedByDescending { protoOrderEpoch(it.dateTime) }
    val sales = retailOrders.filter { it.status != "Отменён" && protoInRange(it.dateTime, range) }
    val periodExpenses = expenses.filter { protoInRange(it.date, range) }.sortedByDescending { protoParseDate(it.date) ?: LocalDate.MIN }
    val expensesSum = periodExpenses.sumOf { it.amount }
    // Для агента «продажи» — заказы его частных клиентов; для торговой организации — её закупки у SANSARA.
    val analyticsSource = if (isAgent && sales.isNotEmpty()) sales else myOrders
    val wreathPieces = analyticsSource.flatMap { it.lines }.filter { typeOf(it.sku).contains("Вен", true) }.sumOf { it.qty }
    val purchases = myOrders.sumOf { it.total } + sales.sumOf { it.total }
    val retailRevenue = sales.sumOf { o -> o.lines.sumOf { (if (it.retailPrice > 0) it.retailPrice else it.price) * it.qty } }
    val retailCost = sales.sumOf { it.total }
    val profit = retailRevenue - retailCost - expensesSum
    val deferred = shipments.filter { it.paymentType == "DEFERRED" && it.moneyStatus != "CONFIRMED" }
    val nearest = deferred.mapNotNull { protoDaysTo(it.dueDate) }.minOrNull()
    val bySku = analyticsSource.flatMap { it.lines }.groupBy { it.sku }.map { (sku, l) -> ProtoSkuStat(sku, l.first().name, l.sumOf { it.qty }, l.sumOf { (if (isAgent && it.retailPrice > 0) it.retailPrice else it.price) * it.qty }) }.sortedByDescending { it.qty }
    val byType = analyticsSource.flatMap { it.lines }.groupBy { typeOf(it.sku) }.mapValues { (_, l) -> l.sumOf { it.qty } }.entries.sortedByDescending { it.value }
    val totalPieces = byType.sumOf { it.value }.coerceAtLeast(1)

    ProtoScaffold("Отчётность", "Период: " + protoRangeLabel(range), onBack) {
        item { ProtoChipRow(protoReportPeriods, period) { period = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Заказов", (myOrders.size + sales.size).toString(), if (isAgent) "мои и клиентские" else "за период", Modifier.weight(1f), onClick = { showOrders = !showOrders })
                StatCard(if (isAgent) "Продано венков" else "Закуплено венков", wreathPieces.toString(), "шт.", Modifier.weight(1f))
            }
        }
        item { StatCard("Сумма заказов", protoMoney(if (isAgent) retailRevenue + myOrders.sumOf { it.total } else purchases), if (showOrders) "скрыть список" else "нажмите — список заказов", Modifier.fillMaxWidth(), onClick = { showOrders = !showOrders }) }
        if (showOrders) {
            val list = (myOrders + sales).sortedByDescending { protoOrderEpoch(it.dateTime) }
            if (list.isEmpty()) item { Text("За период заказов нет", color = ProtoMuted) }
            items(list, key = { "o-" + it.id }) { o -> ProtoOrderRow(if (o.retailCustomerId.isNotBlank()) o.copy(clientName = "Клиент: " + o.retailCustomerName) else o) { onOpenOrder(o) } }
        }

        item {
            ProtoSectionCard {
                Text("Мои финансы", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (isAgent) {
                    ProtoInfoRow("Оборот (продажи клиентам)", protoMoney(retailRevenue))
                    ProtoInfoRow("Закупка у SANSARA под эти продажи", protoMoney(retailCost))
                    ProtoInfoRow("Мои расходы", protoMoney(expensesSum))
                    HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Чистая прибыль", color = ProtoText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(protoMoney(profit), color = if (profit >= 0) ProtoGreen else ProtoRed, fontWeight = FontWeight.Bold)
                    }
                    Text("Прибыль = продажи клиентам − закупка у SANSARA − ваши расходы.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                } else {
                    ProtoInfoRow("Закуплено у SANSARA", protoMoney(purchases))
                    ProtoInfoRow("Мои расходы", protoMoney(expensesSum))
                    HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
                    ProtoInfoRow("Всего затрат", protoMoney(purchases + expensesSum))
                    Text("Ваши продажи приложение не видит, поэтому прибыль не считается: только закупки и расходы.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("К оплате по отсрочке", color = ProtoMuted, modifier = Modifier.weight(1f))
                    Text(protoMoney(deferred.sumOf { it.amount }), color = if (deferred.isNotEmpty()) ProtoRed else ProtoText, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Дней до оплаты", color = ProtoMuted, modifier = Modifier.weight(1f))
                    Text(when { nearest == null -> "—"; nearest < 0 -> "просрочено ${-nearest} дн."; nearest == 0 -> "сегодня"; else -> "$nearest дн." }, color = if (nearest != null && nearest < 0) ProtoRed else ProtoText, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Мои расходы", color = ProtoText, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAddExpense = true }) { Icon(Icons.Outlined.Add, null, tint = ProtoGold); Text("Добавить", color = ProtoGold) }
            }
        }
        if (periodExpenses.isEmpty()) item { Text("Расходов за период нет. Добавляйте аренду, зарплату, доставку — и отчёт покажет реальный итог.", color = ProtoMuted, fontSize = 13.sp) }
        items(periodExpenses, key = { "e-" + it.id }) { e ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ProtoPanel).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.category, color = ProtoText, fontWeight = FontWeight.SemiBold)
                    Text(e.date + if (e.comment.isNotBlank()) " · " + e.comment else "", color = ProtoMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(protoMoney(e.amount), color = ProtoText, fontWeight = FontWeight.Bold)
                IconButton(onClick = { deleting = e }) { Icon(Icons.Outlined.Close, contentDescription = "Удалить", tint = ProtoMuted) }
            }
        }

        item {
            ProtoSectionCard {
                Text("Аналитика по заказам", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (byType.isEmpty()) Text("За период данных нет", color = ProtoMuted, modifier = Modifier.padding(top = 6.dp))
                byType.forEach { (type, qty) ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(type, color = ProtoText, modifier = Modifier.weight(1f))
                        Text("$qty шт. · " + (qty * 100 / totalPieces) + "%", color = ProtoGoldSoft)
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(ProtoBorder)) {
                        Box(Modifier.fillMaxWidth(qty.toFloat() / totalPieces).fillMaxHeight().background(ProtoGold))
                    }
                }
                Text("Нажмите на артикул ниже — откроется карточка товара.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item { ProtoSectionTitle("Хиты продаж", if (bySku.isEmpty()) "Появятся после первых заказов" else "Рейтинг артикулов за период") }
        itemsIndexed(bySku.take(10), key = { _, s -> "h-" + s.sku }) { i, st ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ProtoPanel).clickable { onOpenProduct(st.sku) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(if (i < 3) ProtoGold else ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) {
                    Text((i + 1).toString(), color = if (i < 3) Color.Black else ProtoText, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(st.name, color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ProtoSkuText(st.sku, fontSize = 12)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${st.qty} шт.", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                    Text(protoMoney(st.sum), color = ProtoMuted, fontSize = 12.sp)
                }
            }
        }
        item { ProtoSectionTitle("Подробная история", "Нажмите на заказ — можно повторить или изменить") }
        val history = (myOrders + sales).sortedByDescending { protoOrderEpoch(it.dateTime) }
        if (history.isEmpty()) item { Text("За период заказов нет", color = ProtoMuted) }
        items(history, key = { "hist-" + it.id }) { o -> ProtoOrderRow(if (o.retailCustomerId.isNotBlank()) o.copy(clientName = "Клиент: " + o.retailCustomerName) else o) { onOpenOrder(o) } }
    }

    if (showAddExpense) {
        var category by remember { mutableStateOf(clientExpenseCategories.first()) }
        var amount by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddExpense = false }, containerColor = ProtoPanel,
            title = { Text("Новый расход", color = ProtoText) },
            text = {
                Column {
                    clientExpenseCategories.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoGold, selectedLabelColor = Color.Black, labelColor = ProtoText)) }
                        }
                    }
                    ProtoField(amount, { amount = it.filter(Char::isDigit).take(9) }, "Сумма, ₽", KeyboardType.Number)
                    ProtoField(comment, { comment = it.take(120) }, "Комментарий")
                    Text("Дата: сегодня. Расход учитывается в периоде, когда он внесён.", color = ProtoMuted, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { amount.toIntOrNull()?.takeIf { it > 0 }?.let { onAddExpense(category, it, comment.trim()); showAddExpense = false } }) { Text("Сохранить", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { showAddExpense = false }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null }, containerColor = ProtoPanel,
            title = { Text("Удалить расход?", color = ProtoText) },
            text = { Text(e.category + " · " + protoMoney(e.amount) + " · " + e.date, color = ProtoMuted) },
            confirmButton = { TextButton(onClick = { onDeleteExpense(e); deleting = null }) { Text("Удалить", color = ProtoRed) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
}

// ───────────── 19 · «Для клиентов»: меню агента ─────────────

@Composable
private fun HubButton(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = ProtoPanel), border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxHeight().heightIn(min = 150.dp).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(44.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ProtoGold) }
            Column {
                Text(title, color = ProtoText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = ProtoMuted, fontSize = 12.sp, maxLines = 3, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
internal fun ProtoAgentHubScreen(newCount: Int, totalCount: Int, ordersCount: Int, markupLabel: String, onBack: () -> Unit, onNew: () -> Unit, onBase: () -> Unit, onMarkup: () -> Unit, onOrders: () -> Unit) {
    ProtoScaffold("Продажи", "Заказы ваших клиентов, наценки и база", onBack) {
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HubButton("Новый клиент", "Завести карточку и сразу перейти к заказу", Icons.Outlined.PersonAdd, Modifier.weight(1f), onNew)
                HubButton("База клиентов", if (newCount > 0) "$totalCount всего · $newCount новых" else "$totalCount всего", Icons.Outlined.Groups, Modifier.weight(1f), onBase)
            }
        }
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HubButton("Наценка", markupLabel, Icons.Outlined.Percent, Modifier.weight(1f), onMarkup)
                HubButton("История заказов", protoPlural(ordersCount, "заказ", "заказа", "заказов") + " клиентов", Icons.Outlined.ReceiptLong, Modifier.weight(1f), onOrders)
            }
        }
        item { Text("Закупочные цены клиенту не показываются. В SANSARA заказ уходит по вашей закупочной цене, розничная остаётся для вашего учёта.", color = ProtoMuted, fontSize = 13.sp) }
    }
}

@Composable
internal fun ProtoAgentBaseScreen(customers: List<SansaraAgentCustomer>, isNew: (SansaraAgentCustomer) -> Boolean, ordersOf: (SansaraAgentCustomer) -> Int, onBack: () -> Unit, onOpen: (SansaraAgentCustomer) -> Unit, onAdd: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val qq = q.trim()
    val found = customers.filter { c ->
        qq.isBlank() || (qq.all(Char::isDigit) && c.phone.filter(Char::isDigit).endsWith(qq)) || c.fullName.split(" ").firstOrNull().orEmpty().startsWith(qq, true) || c.fullName.contains(qq, true)
    }.sortedByDescending { it.createdAt }
    val fresh = found.filter(isNew)
    val rest = found.filterNot(isNew)
    @Composable fun row(c: SansaraAgentCustomer) {
        ProtoSectionCard(Modifier.clickable { onOpen(c) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) { Text(c.fullName.take(1).uppercase(), color = ProtoGold, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.fullName, color = ProtoText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(protoPhone(c.phone) + " · " + protoPlural(ordersOf(c), "заказ", "заказа", "заказов"), color = ProtoMuted, fontSize = 12.sp)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = ProtoGold)
            }
        }
    }
    ProtoScaffold("База клиентов", protoPlural(customers.size, "клиент", "клиента", "клиентов"), onBack, action = { IconButton(onClick = onAdd) { Icon(Icons.Outlined.PersonAdd, contentDescription = "Новый клиент", tint = ProtoGold) } }) {
        item { ProtoSearchField(q, { q = it }, {}, "Последние 4 цифры телефона или фамилия") }
        if (found.isEmpty()) item { ProtoEmptyState(Icons.Outlined.PersonSearch, if (customers.isEmpty()) "Клиентов пока нет" else "Никого не нашли", if (customers.isEmpty()) "Добавьте первого клиента" else null, actionLabel = "Новый клиент", onAction = onAdd) }
        if (fresh.isNotEmpty()) {
            item { ProtoSectionTitle("Новые клиенты", "Здесь до первой продажи") }
            items(fresh, key = { "n-" + it.id }) { row(it) }
        }
        if (rest.isNotEmpty()) {
            item { ProtoSectionTitle("Все клиенты") }
            items(rest, key = { "a-" + it.id }) { row(it) }
        }
    }
}

@Composable
internal fun ProtoAgentMarkupScreen(settings: SansaraAgentSettings, skuMarkupCount: Int, onBack: () -> Unit, onSave: (Boolean, Int) -> Unit, onSpecific: () -> Unit) {
    var enabled by remember(settings.ownerClientId, settings.generalMarkupEnabled) { mutableStateOf(settings.generalMarkupEnabled) }
    var pct by remember(settings.ownerClientId, settings.generalMarkupPct) { mutableIntStateOf(settings.generalMarkupPct) }
    val haptic = LocalHapticFeedback.current
    ProtoScaffold("Наценка", "Как цены видит ваш клиент", onBack) {
        item {
            ProtoSectionCard {
                Text("Общая наценка", color = ProtoGoldSoft, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                ProtoSwitchRow("Включена", enabled) { enabled = it }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    ProtoQtyButton(Icons.Outlined.Remove, { pct = (pct - 1).coerceAtLeast(0); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, 46.dp)
                    Text("$pct%", color = if (enabled) ProtoText else ProtoMuted, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(110.dp))
                    ProtoQtyButton(Icons.Outlined.Add, { pct = (pct + 1).coerceAtMost(300); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, 46.dp)
                }
                Text(if (enabled) "Пример: опт 2 550 ₽ → клиенту " + protoMoney((2550 * (1 + pct / 100.0)).roundToInt()) else "Наценка выключена — клиент видит вашу закупочную цену (продажа по себестоимости).", color = ProtoMuted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                ProtoPrimaryButton("Сохранить наценку", { onSave(enabled, pct) })
            }
        }
        item {
            ProtoSectionCard(Modifier.clickable { onSpecific() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Конкретная наценка", color = ProtoText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(if (skuMarkupCount > 0) "Задана для " + protoPlural(skuMarkupCount, "позиции", "позиций", "позиций") else "Своя наценка на выбранные позиции каталога", color = ProtoMuted, fontSize = 13.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = ProtoGold)
                }
            }
        }
        item { Text("Порядок: индивидуальная наценка клиента → наценка на позицию → общая наценка. Скидку конкретному клиенту задаёте в его карточке.", color = ProtoMuted, fontSize = 12.sp) }
    }
}

@Composable
internal fun ProtoAgentProductMarkupScreen(products: List<ProtoCatalogProduct>, markups: Map<String, Int>, generalPct: Int, onBack: () -> Unit, onSet: (String, Int?) -> Unit) {
    val groups = protoWreathGroupsOrdered.filter { g -> products.any { it.type == g } }
    var group by remember { mutableStateOf(groups.firstOrNull().orEmpty()) }
    var q by remember { mutableStateOf("") }
    var onlySet by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val shown = products.filter { p ->
        (if (onlySet) p.sku in markups else (q.isNotBlank() || p.type == group)) && (q.isBlank() || p.sku.contains(q, true) || p.name.contains(q, true))
    }.sortedBy { it.sku }
    ProtoScaffold("Конкретная наценка", "Общая: $generalPct% · своя задана у " + markups.size, onBack) {
        item { ProtoSearchField(q, { q = it }, {}) }
        item { ProtoChipRow((if (markups.isNotEmpty()) listOf("С наценкой") else emptyList()) + groups, if (onlySet) "С наценкой" else group) { if (it == "С наценкой") onlySet = true else { onlySet = false; group = it } } }
        if (shown.isEmpty()) item { ProtoEmptyState(Icons.Outlined.Inventory2, "Ничего не найдено") }
        items(shown.take(120), key = { it.sku }) { p ->
            val own = markups[p.sku]
            val pct = own ?: generalPct
            ProtoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProtoProductImage(p, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        ProtoSkuText(p.sku, fontSize = 12)
                        Text("опт " + protoMoney(p.price) + " → клиенту " + protoMoney((p.price * (1 + pct / 100.0)).roundToInt()), color = ProtoMuted, fontSize = 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProtoQtyButton(Icons.Outlined.Remove, { onSet(p.sku, (pct - 1).coerceAtLeast(0)); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, 40.dp)
                    Text("$pct%", color = if (own != null) ProtoGold else ProtoMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
                    ProtoQtyButton(Icons.Outlined.Add, { onSet(p.sku, (pct + 1).coerceAtMost(300)); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, 40.dp)
                    Spacer(Modifier.weight(1f))
                    if (own != null) TextButton(onClick = { onSet(p.sku, null) }) { Text("Как общая", color = ProtoMuted) }
                    else Text("общая", color = ProtoMuted, fontSize = 12.sp)
                }
            }
        }
        if (shown.size > 120) item { Text("Показаны первые 120 — уточните поиск", color = ProtoMuted, fontSize = 12.sp) }
    }
}

@Composable
internal fun ProtoAgentOrdersScreen(orders: List<ProtoOrder>, onBack: () -> Unit, onOpen: (ProtoOrder) -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = orders.filter { q.isBlank() || it.id.contains(q.trim(), true) || it.retailCustomerName.contains(q.trim(), true) }.sortedByDescending { protoOrderEpoch(it.dateTime) }
    ProtoScaffold("История заказов", "Заказы ваших клиентов · розничные цены", onBack) {
        item { ProtoSearchField(q, { q = it }, {}, "Номер заказа или имя клиента") }
        if (shown.isEmpty()) item { ProtoEmptyState(Icons.Outlined.ReceiptLong, "Заказов клиентов пока нет", "Оформите первый заказ из карточки клиента") }
        items(shown, key = { it.id }) { o -> ProtoOrderRow(o.copy(clientName = o.retailCustomerName.ifBlank { "Частный клиент" }, lines = o.lines.map { l -> if (l.retailPrice > 0) l.copy(price = l.retailPrice, discountPct = 0) else l })) { onOpen(o) } }
    }
}

@Composable
internal fun ProtoRetailOrderSentScreen(orderId: String, customerName: String, total: Int, onContinue: () -> Unit, onOpenOrder: () -> Unit, onMyApp: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Column(Modifier.fillMaxSize()) {
            ProtoBrandHeader(showBell = false)
            Surface(color = ProtoPanel, border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(96.dp).background(ProtoPanel2, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CheckCircle, null, tint = ProtoGold, modifier = Modifier.size(56.dp)) }
                    Text("Заказ оформлен", color = ProtoText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                    Text("№ $orderId · $customerName · " + protoMoney(total), color = ProtoGoldSoft, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Text("Заказ лежит во вкладке «Клиентские заказы». Закупочные цены клиенту не показываются.", color = ProtoMuted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                    Spacer(Modifier.weight(1f))
                    ProtoPrimaryButton("Продолжить выбор с клиентом", onContinue)
                    Spacer(Modifier.height(10.dp))
                    ProtoSecondaryButton("Смотреть заказ", onOpenOrder)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onMyApp, modifier = Modifier.fillMaxWidth()) { Text("Вернуться в моё приложение", color = ProtoGold) }
                }
            }
        }
    }
}
