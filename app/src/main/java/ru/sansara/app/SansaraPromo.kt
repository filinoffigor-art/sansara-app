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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ───────────── Акции ─────────────
//
// Администратор указывает YML-ссылку акционного каталога Tilda. Товары оттуда — «по акции»:
// цена из ссылки окончательная (личная скидка клиента на них не действует), старая цена — из основного
// каталога или из <oldprice>. Пока акция идёт, акционная цена действует везде: в разделе «Акции»,
// в обычном каталоге (с красной плашкой) и в общей корзине.

internal val ProtoPromoRed = Color(0xFFE5484D)
private val promoFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

data class ProtoPromoItem(
    val sku: String, val name: String, val category: String, val quality: String, val size: String,
    val price: Int, val oldPrice: Int, val imageUrl: String, val productionDays: Int
)

data class ProtoPromo(
    val title: String = "",
    val url: String = "",
    val from: String = "",
    val to: String = "",
    val enabled: Boolean = false,
    val items: List<ProtoPromoItem> = emptyList(),
    val loadedAt: String = ""
) {
    private fun date(s: String) = runCatching { LocalDate.parse(s.trim(), promoFmt) }.getOrNull()
    fun isActive(today: LocalDate = LocalDate.now()): Boolean =
        enabled && items.isNotEmpty() && (date(from)?.let { !today.isBefore(it) } ?: true) && (date(to)?.let { !today.isAfter(it) } ?: true)
    val displayTitle: String get() = title.ifBlank { "Акция" }
    val periodLabel: String get() = when {
        to.isNotBlank() && from.isNotBlank() -> "с ${from.take(5)} по ${to.take(5)}"
        to.isNotBlank() -> "до " + to.take(5)
        from.isNotBlank() -> "с " + from.take(5)
        else -> "пока действует"
    }

    fun toJson(): String = org.json.JSONObject().apply {
        put("title", title); put("url", url); put("from", from); put("to", to); put("enabled", enabled); put("loadedAt", loadedAt)
        put("items", org.json.JSONArray().apply {
            items.forEach { i ->
                put(org.json.JSONObject().apply {
                    put("sku", i.sku); put("name", i.name); put("category", i.category); put("quality", i.quality); put("size", i.size)
                    put("price", i.price); put("oldPrice", i.oldPrice); put("imageUrl", i.imageUrl); put("productionDays", i.productionDays)
                })
            }
        })
    }.toString()

    companion object {
        const val PREF_KEY = "promo_campaign"
        fun fromJson(raw: String?): ProtoPromo {
            if (raw.isNullOrBlank()) return ProtoPromo()
            return runCatching {
                val o = org.json.JSONObject(raw)
                val arr = o.optJSONArray("items") ?: org.json.JSONArray()
                ProtoPromo(
                    title = o.optString("title"), url = o.optString("url"), from = o.optString("from"), to = o.optString("to"),
                    enabled = o.optBoolean("enabled", false), loadedAt = o.optString("loadedAt"),
                    items = (0 until arr.length()).mapNotNull { k ->
                        arr.optJSONObject(k)?.let { i ->
                            ProtoPromoItem(i.optString("sku"), i.optString("name"), i.optString("category"), i.optString("quality"), i.optString("size"),
                                i.optInt("price"), i.optInt("oldPrice"), i.optString("imageUrl"), i.optInt("productionDays", 3))
                        }
                    }.filter { it.sku.isNotBlank() && it.price > 0 }
                )
            }.getOrDefault(ProtoPromo())
        }
    }
}

internal fun protoPromoItems(items: List<TildaCatalogItem>): List<ProtoPromoItem> = items.filter { it.price > 0 }.map {
    ProtoPromoItem(it.sku, it.name, it.category, it.quality, it.size, it.price, it.oldPrice, it.imageUrl, it.productionDays)
}

/**
 * Каталог для клиента с учётом акции: у акционных артикулов цена по акции и отметка promo,
 * товары, которых нет в основном каталоге, добавляются в конец («только по акции»).
 */
internal fun protoApplyPromo(catalog: List<ProtoCatalogProduct>, promo: ProtoPromo, today: LocalDate = LocalDate.now()): List<ProtoCatalogProduct> {
    if (!promo.isActive(today)) return catalog
    val bySku = promo.items.associateBy { it.sku.lowercase() }
    val known = mutableSetOf<String>()
    val applied = catalog.map { p ->
        val item = bySku[p.sku.lowercase()] ?: return@map p
        known += p.sku.lowercase()
        val old = listOf(p.price, item.oldPrice).filter { it > item.price }.maxOrNull() ?: 0
        p.copy(price = item.price, promo = true, promoOldPrice = old)
    }
    val extra = promo.items.filter { it.sku.lowercase() !in known }.map { i ->
        ProtoCatalogProduct(i.sku, i.name, i.category, i.quality.ifBlank { "—" }, i.size.ifBlank { "—" }, i.price, 0, "Под заказ", i.productionDays,
            i.imageUrl, promo = true, promoOldPrice = if (i.oldPrice > i.price) i.oldPrice else 0)
    }
    return applied + extra
}

/** Цена для клиента: акционная — окончательная, обычная — со скидкой клиента. */
internal fun protoShopPrice(p: ProtoCatalogProduct, discount: Int): Int = if (p.promo) p.price else protoUnitPrice(p.price, discount)
internal fun protoShopLine(p: ProtoCatalogProduct, qty: Int, discount: Int): Int = qty * protoShopPrice(p, discount)
/** Цена «до»: для акции — старая цена, иначе — базовая (без скидки клиента). */
internal fun protoListPrice(p: ProtoCatalogProduct): Int = if (p.promo && p.promoOldPrice > 0) p.promoOldPrice else p.price
internal fun protoPromoPercent(p: ProtoCatalogProduct): Int =
    if (p.promo && p.promoOldPrice > p.price) ((p.promoOldPrice - p.price) * 100.0 / p.promoOldPrice).toInt() else 0

/** Демо-акция для отладочной сборки и автотестов (ссылка «demo»): 6 венков и 2 корзины −20 %. */
internal fun protoDemoPromoItems(catalog: List<ProtoCatalogProduct>): List<ProtoPromoItem> =
    (catalog.filter { it.type == "Венки" }.take(6) + catalog.filter { it.type == "Корзины" }.take(2)).map { p ->
        ProtoPromoItem(p.sku, p.name, p.type, p.quality, p.size, (p.price * 0.8 / 10).toInt() * 10, p.price, p.imageUrl, p.productionDays)
    }

// ───────────── Элементы интерфейса ─────────────

@Composable
internal fun ProtoPromoBadge(p: ProtoCatalogProduct, modifier: Modifier = Modifier) {
    if (!p.promo) return
    val pct = protoPromoPercent(p)
    Surface(modifier, color = ProtoPromoRed, shape = RoundedCornerShape(16.dp)) {
        Text(if (pct > 0) "Акция −$pct%" else "Акция", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** Красная полоса на главной клиента: ведёт в акционный каталог. */
@Composable
internal fun ProtoPromoBanner(promo: ProtoPromo, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ProtoPromoRed).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.LocalOffer, null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(promo.displayTitle, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(protoPlural(count, "товар", "товара", "товаров") + " по акции · " + promo.periodLabel, color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Color.White)
    }
}

/** Акционный каталог клиента: разделы из Tilda, общая корзина. */
@Composable
internal fun ProtoPromoScreen(
    promo: ProtoPromo,
    products: List<ProtoCatalogProduct>,
    cart: Map<String, Int>,
    cartCount: Int,
    availableStock: (ProtoCatalogProduct) -> Int,
    discount: Int,
    onBack: () -> Unit,
    onOpenProduct: (ProtoCatalogProduct) -> Unit,
    onSetQty: (ProtoCatalogProduct, Int) -> Unit,
    onHome: () -> Unit, onCatalog: () -> Unit, onCart: () -> Unit, onOrders: () -> Unit, onProfile: () -> Unit
) {
    val active = promo.isActive()
    val sections = products.map { it.type }.distinct()
    var section by remember { mutableStateOf("Все") }
    val shown = products.filter { section == "Все" || it.type == section }
    ProtoScaffold("Акции", if (active) promo.displayTitle + " · " + promo.periodLabel else "Сейчас акций нет", onBack,
        bottomBar = { ProtoClientBottomBar(ProtoScreen.PromoCatalog, cartCount, onHome, onCatalog, onCart, onOrders, onProfile) }) {
        if (!active || products.isEmpty()) {
            item { ProtoSectionCard { Text("Сейчас акций нет", color = ProtoText, fontWeight = FontWeight.Bold); Text("Когда администратор запустит акцию, товары появятся здесь и на главной.", color = ProtoMuted, fontSize = 13.sp) } }
            item { ProtoSecondaryButton("Перейти в каталог", onCatalog) }
            return@ProtoScaffold
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ProtoPromoRed.copy(alpha = .14f)).padding(14.dp)) {
                Text(promo.displayTitle, color = ProtoPromoRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(protoPlural(products.size, "товар", "товара", "товаров") + " · " + promo.periodLabel, color = ProtoText, fontSize = 13.sp)
                Text("Цены по акции окончательные. В ту же корзину можно добавить обычные товары — ваша скидка на них сохраняется.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (sections.size > 1) item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Все") + sections) { s ->
                    FilterChip(selected = section == s, onClick = { section = s }, label = { Text(if (s == "Все") "Все · " + products.size else s + " · " + products.count { it.type == s }, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ProtoPromoRed, selectedLabelColor = Color.White, labelColor = ProtoText))
                }
            }
        }
        shown.chunked(2).forEach { pair ->
            item(key = "promo-" + pair.first().sku) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { p -> ProtoProductCard(p, availableStock(p), cart[p.sku] ?: 0, Modifier.weight(1f).fillMaxHeight(), onOpen = { onOpenProduct(p) }, onSetQty = { onSetQty(p, it) }, discount = discount) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { ProtoClickableRow("Добавить обычные товары из каталога", Icons.Outlined.ChevronRight, onCatalog) }
    }
}

// ───────────── Администратор: проведение акций ─────────────

@Composable
internal fun ProtoAdminPromoScreen(
    promo: ProtoPromo,
    catalogSkus: Set<String>,
    loading: Boolean,
    status: String,
    onBack: () -> Unit,
    onLoad: (String) -> Unit,
    onSave: (ProtoPromo) -> Unit,
    onNotify: () -> Unit
) {
    // Поля не сбрасываются после «Загрузить товары»: введённое название и даты остаются до «Сохранить».
    var title by remember { mutableStateOf(promo.title) }
    var url by remember { mutableStateOf(promo.url) }
    var from by remember { mutableStateOf(promo.from) }
    var to by remember { mutableStateOf(promo.to) }
    var enabled by remember { mutableStateOf(promo.enabled) }
    fun okDate(s: String) = s.isBlank() || protoParseDate(s) != null
    val draft = promo.copy(title = title.trim(), url = url.trim(), from = from.trim(), to = to.trim(), enabled = enabled)
    val changed = draft != promo
    ProtoScaffold("Проведение акций", if (promo.isActive()) "Акция идёт · " + promo.periodLabel else "Акция не запущена", onBack) {
        item {
            ProtoSectionCard {
                Text("Акция", color = ProtoGoldSoft, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(title, { title = it.take(40) }, label = { Text("Название, например «Осенняя распродажа»") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                OutlinedTextField(url, { url = it.trim() }, label = { Text("YML-ссылка акционного каталога Tilda") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedTextField(from, { from = it.take(10) }, label = { Text("С (дд.мм.гггг)") }, singleLine = true, isError = !okDate(from), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(to, { to = it.take(10) }, label = { Text("По (дд.мм.гггг)") }, singleLine = true, isError = !okDate(to), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = protoFieldColors(), modifier = Modifier.weight(1f))
                }
                Text("Даты можно не указывать — тогда акция идёт, пока включена.", color = ProtoMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onLoad(url.trim()) }, enabled = !loading && url.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = ProtoGold), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black) else Text("Загрузить товары акции", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                if (status.isNotBlank()) Text(status, color = if (status.startsWith("Ошибка")) ProtoRed else ProtoGreen, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                ProtoSwitchRow(if (enabled) "Акция включена — видна клиентам" else "Акция выключена", enabled) { enabled = it }
                if (enabled && promo.items.isEmpty()) Text("Сначала загрузите товары акции.", color = ProtoOrange, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                ProtoPrimaryButton("Сохранить", { onSave(draft) }, enabled = changed && okDate(from) && okDate(to) && (!enabled || promo.items.isNotEmpty()))
                Text("Цена из ссылки — окончательная: личная скидка клиента на акционные товары не действует. Старая цена — из основного каталога или поля «старая цена» в Tilda.",
                    color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (promo.isActive()) item { OutlinedButton(onClick = onNotify, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, ProtoPromoRed), shape = RoundedCornerShape(24.dp)) { Text("Сообщить клиентам об акции", color = ProtoPromoRed) } }
        if (promo.items.isNotEmpty()) {
            val sections = promo.items.groupBy { it.category }
            item {
                ProtoSectionTitle("Товары акции: " + promo.items.size, "Разделы: " + sections.entries.joinToString { it.key + " · " + it.value.size } + if (promo.loadedAt.isNotBlank()) "\nЗагружено " + promo.loadedAt else "")
            }
            promo.items.forEach { i ->
                item(key = "pi-" + i.sku) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ProtoPanel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(i.name, color = ProtoText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Арт. " + i.sku + " · " + i.category + if (i.sku.lowercase() !in catalogSkus) " · только по акции" else "", color = ProtoMuted, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(protoMoney(i.price), color = ProtoPromoRed, fontWeight = FontWeight.Bold)
                            if (i.oldPrice > i.price) Text(protoMoney(i.oldPrice), color = ProtoMuted, fontSize = 12.sp, textDecoration = TextDecoration.LineThrough)
                        }
                    }
                }
            }
        }
    }
}
