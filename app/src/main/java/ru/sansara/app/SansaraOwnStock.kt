package ru.sansara.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * «Мой склад» агента или торгующей организации: оприходование по артикулу и по QR-коду, остатки,
 * корректировки, история за период и режим «склад ведётся в другой программе».
 */
@Composable
internal fun ProtoOwnStockScreen(
    products: List<ProtoCatalogProduct>,
    ops: List<OwnStockOpEntity>,
    external: Boolean,
    onBack: () -> Unit,
    onExternal: (Boolean) -> Unit,
    onReceive: (ProtoCatalogProduct, Int, String) -> Unit,
    onAdjust: (ProtoCatalogProduct, Int, String) -> Unit,
    onScanError: (String) -> Unit
) {
    var tab by remember { mutableStateOf("Приход") }
    var query by remember { mutableStateOf("") }
    var receiving by remember { mutableStateOf<Pair<ProtoCatalogProduct, String>?>(null) }
    var adjusting by remember { mutableStateOf<ProtoCatalogProduct?>(null) }
    var period by remember { mutableStateOf<ProtoPeriod?>(ProtoPeriod.thisMonth()) }
    val stock = ops.filter { it.kind != OWN_EXTERNAL_SALE }.groupBy { it.sku }.mapValues { (_, l) -> l.sumOf { it.qty } }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@rememberLauncherForActivityResult
        val (sku, serial) = protoParseProductQr(raw)
        val p = products.firstOrNull { protoSkuKey(it.sku) == protoSkuKey(sku) }
        if (p == null) onScanError("Товар с кодом «$sku» не найден в каталоге") else receiving = p to serial
    }
    receiving?.let { (p, serial) ->
        var qty by remember(p.sku, serial) { mutableStateOf(if (serial.isNotBlank()) "1" else "") }
        AlertDialog(
            onDismissRequest = { receiving = null }, containerColor = ProtoPanel,
            title = { Text("Оприходовать", color = ProtoText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(ProtoPanel2)) { ProtoProductImage(p, Modifier.fillMaxSize().padding(3.dp), ContentScale.Fit) }
                        Spacer(Modifier.width(10.dp))
                        Column { Text(protoTitle(p), color = ProtoText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("Арт. " + p.sku, color = ProtoGoldSoft, fontSize = 12.sp) }
                    }
                    if (serial.isNotBlank()) Text("Экземпляр: $serial — оприходуется один раз", color = ProtoMuted, fontSize = 12.sp)
                    Text("Сейчас на складе: " + (stock[p.sku] ?: 0) + " шт.", color = ProtoMuted, fontSize = 13.sp)
                    OutlinedTextField(qty, { qty = it.filter(Char::isDigit).take(4) }, label = { Text("Количество, шт.") }, singleLine = true, enabled = serial.isBlank(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(enabled = (qty.toIntOrNull() ?: 0) > 0, onClick = { onReceive(p, qty.toInt(), serial); receiving = null; query = "" }) { Text("Оприходовать", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { receiving = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }
    adjusting?.let { p ->
        var delta by remember(p.sku) { mutableStateOf("") }
        var minus by remember(p.sku) { mutableStateOf(true) }
        var reason by remember(p.sku) { mutableStateOf("") }
        val have = stock[p.sku] ?: 0
        val d = (delta.toIntOrNull() ?: 0) * if (minus) -1 else 1
        AlertDialog(
            onDismissRequest = { adjusting = null }, containerColor = ProtoPanel,
            title = { Text("Корректировка", color = ProtoText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(protoTitle(p) + " · сейчас $have шт.", color = ProtoMuted, fontSize = 13.sp)
                    ProtoChipRow(listOf("Списать", "Добавить"), if (minus) "Списать" else "Добавить") { minus = it == "Списать" }
                    OutlinedTextField(delta, { delta = it.filter(Char::isDigit).take(4) }, label = { Text("Количество, шт.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(reason, { reason = it.take(150) }, label = { Text("Причина (обязательно)") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                    if (minus && -d > have) Text("Нельзя списать больше, чем есть ($have шт.)", color = ProtoRed, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(enabled = d != 0 && reason.isNotBlank() && have + d >= 0, onClick = { onAdjust(p, d, reason.trim()); adjusting = null }) { Text("Провести", color = ProtoGold) } },
            dismissButton = { TextButton(onClick = { adjusting = null }) { Text("Отмена", color = ProtoMuted) } }
        )
    }

    val suggestions = if (query.trim().length < 2) emptyList() else products.filter { protoSkuMatches(it, query) }.take(12)
    val inStock = stock.filterValues { it > 0 }.mapNotNull { (sku, q) -> products.firstOrNull { it.sku == sku }?.let { it to q } }.sortedBy { it.first.sku }
    val history = ops.filter { period == null || period!!.contains(Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()) }
    ProtoScaffold("Мой склад", if (external) "Остатки ведутся в другой программе" else "Остатки: " + inStock.sumOf { it.second } + " шт. · " + protoPlural(inStock.size, "позиция", "позиции", "позиций"), onBack) {
        item {
            ProtoSectionCard {
                ProtoSwitchRow("Склад ведётся в другой программе", external) { onExternal(it) }
                Text(if (external) "Продажи со своего склада разрешены без оприходования. Продажи записываются в историю, остатки здесь не считаются."
                    else "Продать со своего склада можно только оприходованный товар.", color = ProtoMuted, fontSize = 12.sp)
            }
        }
        item { ProtoChipRow(listOf("Приход", "Остатки", "История"), tab) { tab = it } }
        when (tab) {
            "Приход" -> {
                item {
                    Button(onClick = {
                        scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Наведите камеру на QR-код венка").setBeepEnabled(true).setOrientationLocked(false))
                    }, colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
                        Icon(Icons.Outlined.QrCodeScanner, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text("Сканировать QR-код", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                item { OutlinedTextField(query, { query = it }, label = { Text("Артикул или название") }, placeholder = { Text("например санв110п10 или kor-070", color = ProtoMuted) }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth()) }
                item { Text("Регистр, дефисы, пробелы и раскладка клавиатуры не важны.", color = ProtoMuted, fontSize = 12.sp) }
                if (query.trim().length >= 2 && suggestions.isEmpty()) item { Text("Ничего не найдено", color = ProtoMuted) }
                items(suggestions, key = { "s-" + it.sku }) { p ->
                    ProtoSectionCard(Modifier.clickable { receiving = p to "" }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(ProtoPanel2)) { ProtoProductImage(p, Modifier.fillMaxSize().padding(3.dp), ContentScale.Fit) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) { Text(protoTitle(p), color = ProtoText, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("Арт. " + p.sku + " · на складе " + (stock[p.sku] ?: 0), color = ProtoGoldSoft, fontSize = 12.sp) }
                            Icon(Icons.Outlined.AddCircleOutline, "Оприходовать", tint = ProtoGold)
                        }
                    }
                }
            }
            "Остатки" -> {
                if (inStock.isEmpty()) item { ProtoEmptyState(Icons.Outlined.Inventory2, "Склад пуст", if (external) "Остатки ведутся в другой программе" else "Оприходуйте товар на вкладке «Приход»") }
                items(inStock, key = { "o-" + it.first.sku }) { (p, q) ->
                    ProtoSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(protoTitle(p), color = ProtoText, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("Арт. " + p.sku, color = ProtoGoldSoft, fontSize = 12.sp) }
                            Text("$q шт.", color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = { adjusting = p }) { Text("Корр.", color = ProtoGold) }
                        }
                    }
                }
            }
            else -> {
                item { ProtoPeriodBar(period, { period = it }, allowAll = true, maxDate = java.time.LocalDate.now()) }
                if (history.isEmpty()) item { Text("За период движений нет", color = ProtoMuted) }
                items(history, key = { it.id }) { op ->
                    ProtoSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(op.kind + " · " + op.name, color = ProtoText, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(Instant.ofEpochMilli(op.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " · арт. " + op.sku, color = ProtoMuted, fontSize = 12.sp)
                                val extra = listOf(op.customer.takeIf { it.isNotBlank() }?.let { "клиент: $it" }, op.reason.takeIf { it.isNotBlank() }, op.serial.takeIf { it.isNotBlank() }?.let { "код $it" }, op.actor.takeIf { it.isNotBlank() }).filterNotNull()
                                if (extra.isNotEmpty()) Text(extra.joinToString(" · "), color = ProtoGoldSoft, fontSize = 12.sp)
                            }
                            Text((if (op.qty > 0) "+" else "") + op.qty, color = if (op.qty > 0) ProtoGreen else ProtoOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
