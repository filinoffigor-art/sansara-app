package ru.sansara.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

data class TildaCatalogItem(
    val externalId: String,
    val sku: String,
    val name: String,
    val category: String,
    val quality: String,
    val size: String,
    val price: Int,
    val imageUrl: String,
    val productionDays: Int = 3,
    val retailPrice: Int = 0,
    val colors: List<String> = emptyList()
)

data class CatalogSyncResult(
    val items: List<TildaCatalogItem>,
    val source: String,
    val message: String
)

/** Одно предложение (offer) из YML Tilda. Один артикул может встречаться несколько раз: опт, розница, цвета. */
private data class RawOffer(
    val id: String,
    val name: String,
    val sku: String,
    val categoryId: String,
    val price: Int,
    val picture: String,
    val params: Map<String, String>
)

object TildaCatalogSync {
    private fun normalizeCategory(raw: String, name: String): String {
        val text = "$raw $name".lowercase()
        return when {
            "вен" in text -> "Венки"
            "корз" in text -> "Корзины"
            "флорет" in text -> "Флоретки"
            "полян" in text -> "Полянки"
            "лент" in text -> "Ленты"
            "гроб" in text -> "Гробы"
            "крест" in text -> "Кресты"
            "одеж" in text || "костюм" in text -> "Одежда"
            "цвет" in text -> "Цветы"
            "услуг" in text -> "Услуги"
            else -> raw.ifBlank { "Каталог" }
        }
    }

    private fun param(params: Map<String, String>, vararg keys: String): String? =
        params.entries.firstOrNull { e -> keys.any { k -> e.key.lowercase().contains(k) } }?.value?.trim()?.takeIf { it.isNotEmpty() }

    /** «110см» → «110 см» (как в остальном приложении). */
    private fun prettySize(raw: String): String =
        raw.replace(Regex("(\\d)\\s*(см|м)\\b", RegexOption.IGNORE_CASE), "$1 $2").trim()

    /** Убираем цвет из названия: «Венок ритуальный - Красный» → «Венок ритуальный». */
    private fun baseName(name: String): String = name.substringBefore(" - ").trim().ifBlank { name }

    /**
     * Сводим предложения к одной позиции на артикул. Цена приложения — оптовая (категория «Опт»),
     * розничная сохраняется отдельно. Фото берём с первого оптового предложения.
     */
    private fun aggregate(offers: List<RawOffer>, categories: Map<String, String>): List<TildaCatalogItem> {
        fun isWholesale(o: RawOffer) = categories[o.categoryId].orEmpty().lowercase().let { "опт" in it && "рознич" !in it }
        fun isRetail(o: RawOffer) = categories[o.categoryId].orEmpty().lowercase().contains("рознич")
        return offers.groupBy { it.sku }.map { (sku, list) ->
            val wholesale = list.filter { isWholesale(it) }
            val retail = list.filter { isRetail(it) }
            val primary = wholesale.firstOrNull() ?: retail.firstOrNull() ?: list.first()
            val withPhoto = (wholesale + retail + list).firstOrNull { it.picture.isNotBlank() }
            val group = param(primary.params, "продукция").orEmpty()
            val colors = list.mapNotNull { param(it.params, "цвет") }.distinct()
            TildaCatalogItem(
                externalId = primary.id,
                sku = sku,
                name = baseName(primary.name),
                category = normalizeCategory(group, primary.name),
                quality = param(primary.params, "качество") ?: "—",
                size = param(primary.params, "размер")?.let { prettySize(it) } ?: "—",
                price = primary.price,
                imageUrl = withPhoto?.picture.orEmpty(),
                retailPrice = retail.firstOrNull()?.price ?: 0,
                colors = colors
            )
        }
    }

    suspend fun fetchYml(feedUrl: String): CatalogSyncResult = withContext(Dispatchers.IO) {
        require(feedUrl.startsWith("https://") || feedUrl.startsWith("http://")) { "Некорректная ссылка YML" }

        val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SANSARA-App/0.23")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Tilda вернула HTTP $code")

            connection.inputStream.use { input ->
                val parser = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(input, "UTF-8") }

                val categories = linkedMapOf<String, String>()
                val offers = mutableListOf<RawOffer>()
                var inOffer = false
                var currentCategoryId: String? = null
                var currentParamName: String? = null
                var tag: String? = null
                var id = ""; var name = ""; var sku = ""; var categoryId = ""; var price = 0; var picture = ""
                var params = linkedMapOf<String, String>()

                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            tag = parser.name
                            when (parser.name) {
                                "category" -> currentCategoryId = parser.getAttributeValue(null, "id")
                                "offer" -> {
                                    inOffer = true
                                    id = parser.getAttributeValue(null, "id").orEmpty()
                                    name = ""; sku = ""; categoryId = ""; price = 0; picture = ""; params = linkedMapOf()
                                }
                                "param" -> currentParamName = parser.getAttributeValue(null, "name")
                            }
                        }
                        XmlPullParser.TEXT -> {
                            val text = parser.text?.trim().orEmpty()
                            if (text.isNotEmpty()) {
                                if (!inOffer && tag == "category" && currentCategoryId != null) {
                                    categories[currentCategoryId!!] = text
                                } else if (inOffer) {
                                    when (tag) {
                                        "name" -> name = text
                                        "vendorCode" -> sku = text
                                        "categoryId" -> categoryId = text
                                        "price" -> price = text.replace(',', '.').toDoubleOrNull()?.toInt() ?: price
                                        "picture" -> if (picture.isBlank()) picture = text
                                        "param" -> currentParamName?.let { params[it] = text }
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "category" -> { currentCategoryId = null; tag = null }
                                "param" -> { currentParamName = null; tag = null }
                                "offer" -> {
                                    val resolvedSku = sku.ifBlank { id }
                                    if (resolvedSku.isNotBlank() && name.isNotBlank()) {
                                        offers += RawOffer(id, name, resolvedSku, categoryId, price, picture, params)
                                    }
                                    inOffer = false
                                    tag = null
                                }
                                else -> tag = null
                            }
                        }
                    }
                    event = parser.next()
                }

                val items = aggregate(offers, categories)
                CatalogSyncResult(
                    items = items,
                    source = feedUrl,
                    message = "Получено ${items.size} артикулов из Tilda"
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
