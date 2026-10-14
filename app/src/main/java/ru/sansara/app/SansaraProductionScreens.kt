package ru.sansara.app

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ───────────── Черновики выпуска: переживают закрытие приложения ─────────────

internal fun protoSaveDrafts(prefs: SharedPreferences, drafts: List<ProtoProductionDraft>) {
    val arr = JSONArray()
    drafts.forEach { d -> arr.put(JSONObject().put("sku", d.product.sku).put("qty", d.qty).put("assembler", d.assembler).put("rate", d.rateRub).put("date", d.date)) }
    prefs.edit().putString("production_drafts", arr.toString()).apply()
}

internal fun protoLoadDrafts(prefs: SharedPreferences, products: List<ProtoCatalogProduct>): List<ProtoProductionDraft> = runCatching {
    val arr = JSONArray(prefs.getString("production_drafts", "[]").orEmpty())
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.getJSONObject(i)
        val p = products.firstOrNull { it.sku == o.optString("sku") } ?: return@mapNotNull null
        ProtoProductionDraft(p, o.optInt("qty", 1), o.optString("assembler"), o.optInt("rate", 0), o.optString("date"))
    }
}.getOrDefault(emptyList())

// ───────────── Главный экран производства ─────────────

@Composable
internal fun ProtoProductionHomeScreen(
    selectedDate: LocalDate,
    drafts: List<ProtoProductionDraft>,
    opsForDay: List<ProtoProductionOp>,
    products: List<ProtoCatalogProduct>,
    openTasks: Int,
    unreadChat: Int,
    onDateChange: (LocalDate) -> Unit,
    onAdd: () -> Unit,
    onRemoveDraft: (ProtoProductionDraft) -> Unit,
    onPostAll: () -> Unit,
    onReport: () -> Unit,
    onPayments: () -> Unit,
    onWorkshop: () -> Unit,
    onAttendance: () -> Unit,
    onChat: () -> Unit,
    onHistory: () -> Unit,
    onStock: () -> Unit,
    onProfile: () -> Unit
) {
    val produced = opsForDay.sumOf { it.qty }
    val draftQty = drafts.sumOf { it.qty }
    val zeroRate = drafts.count { it.rateRub <= 0 }
    val isToday = selectedDate == LocalDate.now()
    var showCalendar by remember { mutableStateOf(false) }
    var confirmPost by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { ProtoProductionBottomBar(ProtoScreen.Production, onRelease = {}, onHistory = onHistory, onStock = onStock, onProfile = onProfile) }
        ) { pad ->
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ProtoBrandHeader(edge = 0.dp) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Выпуск", color = ProtoText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", ruLocale)) + if (isToday) " · сегодня" else "", color = if (isToday) ProtoMuted else ProtoOrange, fontSize = 13.sp)
                        }
                        OutlinedButton(onClick = { showCalendar = true }, border = BorderStroke(1.dp, ProtoGold), shape = RoundedCornerShape(22.dp)) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = ProtoGold, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Дата", color = ProtoGold)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProtoMetricCard("Проведено", produced.toString(), "шт. за день", Modifier.weight(1f)) {}
                        ProtoMetricCard("В черновике", draftQty.toString(), "шт. не проведено", Modifier.weight(1f)) {}
                    }
                }
                item { ProtoPrimaryButton("+ Добавить выпуск", onAdd) }

                item { ProtoSectionTitle("Черновик — ещё не на складе", if (drafts.isEmpty()) "Добавьте позиции, затем оприходуйте всё одной кнопкой" else "Сохраняется, даже если закрыть приложение") }
                items(drafts, key = { it.date + it.product.sku + it.assembler + it.qty + it.rateRub }) { d ->
                    ProtoSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProtoProductImage(d.product, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(protoTitle(d.product), color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                ProtoSkuText(d.product.sku, extra = " · " + d.assembler, fontSize = 12)
                                Text(if (d.rateRub > 0) "Ставка " + protoMoney(d.rateRub) + " / шт." else "Ставка не задана", color = if (d.rateRub > 0) ProtoMuted else ProtoOrange, fontSize = 12.sp)
                            }
                            Text("${d.qty} шт.", color = ProtoGoldSoft, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onRemoveDraft(d) }) { Icon(Icons.Outlined.Close, contentDescription = "Убрать", tint = ProtoMuted) }
                        }
                    }
                }
                if (drafts.isNotEmpty()) item {
                    if (zeroRate > 0) Text("У $zeroRate позиций нет ставки — сборщице начислится 0 ₽. Ставки задаёт администратор.", color = ProtoOrange, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                    ProtoPrimaryButton("Оприходовать $draftQty шт. на склад", { confirmPost = true })
                }

                item { ProtoSectionTitle("Проведено за день", if (opsForDay.isEmpty()) "Пока ничего" else null) }
                items(opsForDay, key = { it.opId.ifBlank { it.time + it.sku + it.assembler } }) { op ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ProtoPanel).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(op.name, color = ProtoText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(op.time + " · " + op.sku + " · " + op.assembler, color = ProtoMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${op.qty} шт.", color = ProtoGreen, fontWeight = FontWeight.Bold)
                    }
                }

                item { ProtoSectionTitle("Цех") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProtoQuickButton(if (openTasks > 0) "Задания · $openTasks" else "Задания", Icons.Outlined.Assignment, Modifier.weight(1f), onWorkshop)
                        ProtoQuickButton("Табель", Icons.Outlined.EventAvailable, Modifier.weight(1f), onAttendance)
                        ProtoQuickButton(if (unreadChat > 0) "Чат · $unreadChat" else "Чат", Icons.Outlined.ChatBubbleOutline, Modifier.weight(1f), onChat)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProtoQuickButton("Отчёт за день", Icons.Outlined.Assessment, Modifier.weight(1f), onReport)
                        ProtoQuickButton("Выплаты", Icons.Outlined.Payments, Modifier.weight(1f), onPayments)
                    }
                }
            }
        }
    }
    if (showCalendar) ProtoProductionCalendarDialog(selectedDate, onDismiss = { showCalendar = false }, maxDate = LocalDate.now(), onSelect = { onDateChange(it); showCalendar = false })
    if (confirmPost) AlertDialog(
        onDismissRequest = { confirmPost = false }, containerColor = ProtoPanel,
        title = { Text("Оприходовать выпуск?", color = ProtoText) },
        text = {
            Column {
                Text("Дата: " + selectedDate.format(protoDateFmt), color = ProtoGoldSoft)
                drafts.groupBy { it.assembler }.forEach { (a, list) -> ProtoInfoRow(a, list.sumOf { it.qty }.toString() + " шт.") }
                Text("Товар появится на складе и станет доступен клиентам. Исправить сможет только администратор.", color = ProtoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = { TextButton(onClick = { confirmPost = false; onPostAll() }) { Text("Оприходовать", color = ProtoGold) } },
        dismissButton = { TextButton(onClick = { confirmPost = false }) { Text("Отмена", color = ProtoMuted) } }
    )
}

// ───────────── Один экран добавления выпуска ─────────────

@Composable
internal fun ProtoProductionEntryScreen(
    products: List<ProtoCatalogProduct>,
    assemblers: List<SansaraAssembler>,
    rates: Map<String, Int>,
    selectedDate: LocalDate,
    initialAssembler: String,
    onBack: () -> Unit,
    onAddDraft: (ProtoCatalogProduct, Int, String, Int) -> Unit
) {
    val groups = protoWreathGroupsOrdered.filter { g -> products.any { it.type == g } }.ifEmpty { products.map { it.type }.distinct() }
    var group by remember { mutableStateOf(groups.firstOrNull().orEmpty()) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var qty by remember { mutableIntStateOf(1) }
    var assembler by remember { mutableStateOf(initialAssembler.ifBlank { assemblers.firstOrNull()?.name.orEmpty() }) }
    val shown = products.filter { p -> (query.isNotBlank() || p.type == group) && (query.isBlank() || p.sku.contains(query, true) || p.name.contains(query, true)) }.sortedBy { it.sku }
    Box(Modifier.fillMaxSize()) {
        ProtoLiveBackground()
        Column(Modifier.fillMaxSize()) {
            ProtoTopBar("Добавить выпуск", selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale)), onBack)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ProtoField(query, { query = it }, "Поиск по артикулу или названию") }
                if (query.isBlank()) item { ProtoChipRow(groups, group) { group = it } }
                if (shown.isEmpty()) item { ProtoEmptyState(Icons.Outlined.Inventory2, "Ничего не найдено") }
                items(shown.take(80), key = { it.sku }) { p ->
                    val active = selected?.sku == p.sku
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ProtoPanel),
                        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) ProtoGold else ProtoBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable { selected = p; qty = 1 }
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProtoProductImage(p, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                ProtoSkuText(p.sku, fontSize = 12)
                            }
                            if (active) Icon(Icons.Outlined.CheckCircle, null, tint = ProtoGold)
                        }
                    }
                }
                if (shown.size > 80) item { Text("Показаны первые 80 — уточните поиск", color = ProtoMuted, fontSize = 12.sp) }
            }
            selected?.let { p ->
                val rate = rates[p.sku] ?: 0
                Surface(color = ProtoPanel, border = BorderStroke(1.dp, ProtoBorder), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(protoTitle(p) + " · " + p.sku, color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Сборщица", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        if (assemblers.isEmpty()) Text("Справочник пуст — администратор добавляет сборщиц в настройках.", color = ProtoRed, fontSize = 13.sp)
                        else ProtoChipRow(assemblers.map { it.name }, assembler) { assembler = it }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            ProtoQtyButton(Icons.Outlined.Remove, { qty = (qty - 1).coerceAtLeast(1) }, 46.dp)
                            Text(qty.toString(), color = ProtoText, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(72.dp))
                            ProtoQtyButton(Icons.Outlined.Add, { qty += 1 }, 46.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(if (rate > 0) protoMoney(rate) + " / шт." else "Ставка не задана", color = if (rate > 0) ProtoMuted else ProtoOrange, fontSize = 13.sp)
                                Text(protoMoney(rate * qty), color = ProtoGoldSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        ProtoPrimaryButton("Добавить в черновик", { onAddDraft(p, qty, assembler, rate); selected = null; qty = 1 }, enabled = assembler.isNotBlank())
                    }
                }
            }
        }
    }
}

// ───────────── Выплаты сборщицам ─────────────

@Composable
internal fun ProtoProductionPaymentsScreen(ops: List<ProtoProductionOp>, onBack: () -> Unit, onExport: () -> Unit) {
    var period by remember { mutableStateOf("Месяц") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val range: Pair<LocalDate, LocalDate>? = when (period) {
        "День" -> today to today
        "Неделя" -> today.minusDays(6) to today
        "Месяц" -> today.withDayOfMonth(1) to today
        "Прошлый месяц" -> YearMonth.from(today).minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
        else -> null
    }
    val filtered = ops.filter { op ->
        if (op.status == "Удалён" || op.status == "Удален") return@filter false
        val d = protoParseDate(op.date) ?: return@filter range == null
        range == null || (!d.isBefore(range.first) && !d.isAfter(range.second))
    }
    val zeroRate = filtered.filter { it.rateRub <= 0 }
    val grouped = filtered.groupBy { it.assembler }.entries.sortedByDescending { e -> e.value.sumOf { it.amountRub } }
    ProtoScaffold("Выплаты сборщицам", "По проведённому выпуску", onBack, action = { IconButton(onClick = onExport) { Icon(Icons.Outlined.FileDownload, contentDescription = "Экспорт", tint = ProtoGold) } }) {
        item { ProtoChipRow(listOf("День", "Неделя", "Месяц", "Прошлый месяц", "Всё"), period) { period = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProtoMetricCard("Собрано", filtered.sumOf { it.qty }.toString(), "шт.", Modifier.weight(1f)) {}
                ProtoMetricCard("К выплате", protoMoney(filtered.sumOf { it.amountRub }), "", Modifier.weight(1f)) {}
            }
        }
        if (zeroRate.isNotEmpty()) item {
            Surface(color = ProtoOrange.copy(alpha = .12f), border = BorderStroke(1.dp, ProtoOrange.copy(alpha = .5f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("${zeroRate.sumOf { it.qty }} шт. проведены без ставки — по ним начислено 0 ₽. Попросите администратора задать ставки.", color = ProtoText, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }
        if (grouped.isEmpty()) item { ProtoEmptyState(Icons.Outlined.Payments, "За период выпуска нет") }
        items(grouped, key = { it.key }) { (name, list) ->
            val open = expanded == name
            ProtoSectionCard(Modifier.clickable { expanded = if (open) null else name }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name.ifBlank { "Без сборщицы" }, color = ProtoText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(list.sumOf { it.qty }.toString() + " шт. · " + protoPlural(list.map { it.date }.distinct().size, "день", "дня", "дней"), color = ProtoMuted, fontSize = 12.sp)
                    }
                    Text(protoMoney(list.sumOf { it.amountRub }), color = ProtoGoldSoft, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = ProtoMuted)
                }
                if (open) {
                    HorizontalDivider(color = ProtoBorder, modifier = Modifier.padding(vertical = 6.dp))
                    list.groupBy { it.date }.entries.sortedByDescending { e -> protoParseDate(e.key) ?: LocalDate.MIN }.forEach { (day, dayOps) ->
                        Text(day, color = ProtoGoldSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                        dayOps.forEach { op ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(op.sku + " × " + op.qty, color = ProtoText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text(if (op.rateRub > 0) protoMoney(op.amountRub) else "нет ставки", color = if (op.rateRub > 0) ProtoMuted else ProtoOrange, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
