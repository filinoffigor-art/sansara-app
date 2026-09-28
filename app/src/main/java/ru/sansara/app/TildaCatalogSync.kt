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
    val productionDays: Int = 3
)

data class CatalogSyncResult(
    val items: List<TildaCatalogItem>,
    val source: String,
    val message: String
)

object TildaCatalogSync {
    private fun normalizeCategory(raw: String, name: String): String {
        val text = "$raw $name".lowercase()
        return when {
            "круг" in text && "вен" in text -> "Венки круглые"
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

    suspend fun fetchYml(feedUrl: String): CatalogSyncResult = withContext(Dispatchers.IO) {
        require(feedUrl.startsWith("https://") || feedUrl.startsWith("http://")) { "Некорректная ссылка YML" }

        val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SANSARA-App/0.9")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Tilda вернула HTTP $code")

            connection.inputStream.use { input ->
                val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(input, "UTF-8")
                }

                val categories = linkedMapOf<String, String>()
                val products = mutableListOf<TildaCatalogItem>()
                var currentCategoryId: String? = null
                var currentOfferId: String? = null
                var currentTag: String? = null
                var name = ""
                var sku = ""
                var categoryId = ""
                var price = 0
                var image = ""
                var quality = "—"
                var size = "—"
                var productionDays = 3
                var currentParamName: String? = null

                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            currentTag = parser.name
                            when (parser.name) {
                                "category" -> currentCategoryId = parser.getAttributeValue(null, "id")
                                "offer" -> {
                                    currentOfferId = parser.getAttributeValue(null, "id") ?: ""
                                    name = ""; sku = ""; categoryId = ""; price = 0; image = ""
                                    quality = "—"; size = "—"; productionDays = 3
                                }
                                "param" -> currentParamName = parser.getAttributeValue(null, "name")
                            }
                        }
                        XmlPullParser.TEXT -> {
                            val text = parser.text?.trim().orEmpty()
                            if (text.isNotEmpty()) {
                                if (currentOfferId == null && currentTag == "category" && currentCategoryId != null) {
                                    categories[currentCategoryId!!] = text
                                } else if (currentOfferId != null) {
                                    when (currentTag) {
                                        "name" -> name = text
                                        "vendorCode" -> sku = text
                                        "categoryId" -> categoryId = text
                                        "price" -> price = text.replace(',', '.').toDoubleOrNull()?.toInt() ?: price
                                        "picture" -> if (image.isBlank()) image = text
                                        "param" -> when (currentParamName?.lowercase()) {
                                            "качество", "класс", "категория качества" -> quality = text
                                            "размер", "высота", "диаметр" -> size = text
                                            "срок производства", "срок производства, дней" -> productionDays = text.filter { it.isDigit() }.toIntOrNull() ?: 3
                                        }
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "category" -> { currentCategoryId = null; currentTag = null }
                                "param" -> { currentParamName = null; currentTag = null }
                                "offer" -> {
                                    val ext = currentOfferId.orEmpty()
                                    val resolvedSku = sku.ifBlank { ext }
                                    if (resolvedSku.isNotBlank() && name.isNotBlank()) {
                                        products += TildaCatalogItem(
                                            externalId = ext,
                                            sku = resolvedSku,
                                            name = name,
                                            category = normalizeCategory(categories[categoryId].orEmpty(), name),
                                            quality = quality,
                                            size = size,
                                            price = price,
                                            imageUrl = image,
                                            productionDays = productionDays
                                        )
                                    }
                                    currentOfferId = null
                                    currentTag = null
                                }
                                else -> currentTag = null
                            }
                        }
                    }
                    event = parser.next()
                }

                CatalogSyncResult(
                    items = products,
                    source = feedUrl,
                    message = "Получено ${products.size} позиций из Tilda"
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
