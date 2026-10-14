package ru.sansara.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Компактная шапка внутренних экранов: «назад», заголовок, подзаголовок, одно действие справа. */
@Composable
internal fun ProtoTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            ProtoCircleBack(onBack)
            Spacer(Modifier.width(12.dp))
        } else Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ProtoText, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = ProtoMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (action != null) action()
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

/** Едва заметный узор из листьев на фоне всех экранов (рисуется кодом, без картинок). */
@Composable
internal fun ProtoLeafPattern(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val step = 118.dp.toPx()
        val leafLen = 38.dp.toPx()
        val leafW = 11.dp.toPx()
        val fill = Color(0xFFD8B07C).copy(alpha = 0.035f)
        val vein = Color(0xFFD8B07C).copy(alpha = 0.05f)
        var row = 0
        var y = step * 0.4f
        while (y < size.height + step) {
            var col = 0
            var x = if (row % 2 == 0) step * 0.3f else step * 0.8f
            while (x < size.width + step) {
                val angle = ((row * 47 + col * 71) % 120 - 60).toFloat()
                rotate(angle, pivot = Offset(x, y)) {
                    val top = y - leafLen / 2
                    val bottom = y + leafLen / 2
                    val leaf = Path().apply {
                        moveTo(x, top)
                        cubicTo(x + leafW, top + leafLen * 0.25f, x + leafW, bottom - leafLen * 0.25f, x, bottom)
                        cubicTo(x - leafW, bottom - leafLen * 0.25f, x - leafW, top + leafLen * 0.25f, x, top)
                        close()
                    }
                    drawPath(leaf, fill)
                    drawLine(vein, Offset(x, top + 4f), Offset(x, bottom + leafLen * 0.18f), strokeWidth = 1.2f)
                    drawPath(leaf, vein, style = Stroke(width = 1f))
                }
                x += step
                col++
            }
            y += step * 0.78f
            row++
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
    val q = f.query.trim()
    return (q.isBlank() || p.sku.contains(q, true) || p.name.contains(q, true) || protoTitle(p).contains(q, true)) &&
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
