package ru.sansara.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Проверка логики без телефона: поиск, цены со скидкой, фильтр, правила отмены. Запускается в каждой сборке CI. */
class SansaraLogicTest {

    private fun product(sku: String, name: String, type: String = "Венки", quality: String = "Премиум", size: String = "110 см", price: Int = 2550, stock: Int = 5) =
        ProtoCatalogProduct(sku, name, type, quality, size, price, stock, "В наличии", 3)

    private val wreath = product("САНВ110П-10", "Венок ритуальный")
    private val basket = product("KOR-070-004", "Корзина траурная", type = "Корзины", quality = "Стандарт", size = "70 см", price = 3200)

    // ── Поиск ──

    @Test fun wrongLayoutRussianWordIsFound() {
        assertEquals("венок", protoSwapLayout("dtyjr", toRussian = true))
        assertTrue(protoSearchMatches(wreath, "dtyjr"))       // «венок», набранный в латинской раскладке
        assertTrue(protoSearchMatches(wreath, "ВЕНОК"))       // регистр не важен
    }

    @Test fun cyrillicSkuTypedInWrongLayout() {
        assertTrue(protoSearchMatches(wreath, "cfyd110g"))    // «санв110п» в латинской раскладке
        assertTrue(protoSearchMatches(wreath, "санв110п-10"))
    }

    @Test fun latinSkuStillSearchable() {
        assertTrue(protoSearchMatches(basket, "kor-070"))
        assertTrue(protoSearchMatches(basket, "лщк-070"))     // латинский артикул, набранный в русской раскладке
        assertFalse(protoSearchMatches(basket, "венок"))
    }

    @Test fun yoAndSeveralWords() {
        val p = product("X-1", "Венок чёрный")
        assertTrue(protoSearchMatches(p, "черный венок"))
    }

    // ── Цены ──

    @Test fun lineTotalEqualsUnitPriceTimesQty() {
        // Цена в каталоге уже со скидкой; сумма строки = эта цена × количество (без расхождений в рублях).
        listOf(0, 3, 5, 7, 10, 15).forEach { d ->
            val unit = protoUnitPrice(2550, d)
            assertEquals(unit * 7, protoLineTotal(2550, 7, d))
        }
        assertEquals(2295, protoUnitPrice(2550, 10))
        assertEquals(2550, protoUnitPrice(2550, 0))
    }

    @Test fun orderTotalUsesDiscount() {
        val o = ProtoOrder("S-1", "Тест", "14.10.2026 10:00", listOf(ProtoOrderLine("A", "a", 3, 1000, 10), ProtoOrderLine("B", "b", 2, 2550, 10)), "Получен")
        assertEquals(3 * 900 + 2 * 2295, o.total)
        assertEquals(3 * 1000 + 2 * 2550, o.baseTotal)
    }

    // ── Фильтр: счётчик и список считаются одной функцией ──

    @Test fun filterCountMatchesList() {
        val items = listOf(wreath, basket, product("V-2", "Венок", quality = "Эконом", size = "60 см", stock = 0))
        val f = ProtoCatalogFilter(types = setOf("Венки"), qualities = setOf("Премиум"))
        val matched = items.filter { protoMatches(it, f, it.stock) }
        assertEquals(listOf(wreath), matched)
        val inStock = ProtoCatalogFilter(availability = setOf("В наличии"))
        assertEquals(2, items.count { protoMatches(it, inStock, it.stock) })
    }

    // ── Правила отмены ──

    private fun stamp(minutesAgo: Long) = LocalDateTime.now().minusMinutes(minutesAgo).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    @Test fun cancelWindowsByKind() {
        val stock = ProtoOrderLine("A", "a", 2, 100, stockQty = 2)
        val made = ProtoOrderLine("B", "b", 2, 100, stockQty = 0)
        val mixed = ProtoOrderLine("C", "c", 3, 100, stockQty = 1)
        assertEquals("stock", protoOrderKind(listOf(stock)))
        assertEquals("order", protoOrderKind(listOf(made)))
        assertEquals("mixed", protoOrderKind(listOf(stock, made)))
        assertEquals("mixed", protoOrderKind(listOf(mixed)))

        assertTrue(protoCancelState(ProtoOrder("1", "x", stamp(10), listOf(stock), "Получен")).allowed)
        assertFalse(protoCancelState(ProtoOrder("2", "x", stamp(40), listOf(stock), "Получен")).allowed)
        assertTrue(protoCancelState(ProtoOrder("3", "x", stamp(50), listOf(mixed), "Подтверждён")).allowed)
        assertFalse(protoCancelState(ProtoOrder("4", "x", stamp(70), listOf(mixed), "Получен")).allowed)
        assertTrue(protoCancelState(ProtoOrder("5", "x", stamp(23 * 60), listOf(made), "Получен")).allowed)
        // В сборке отменить нельзя, даже если срок не прошёл.
        assertFalse(protoCancelState(ProtoOrder("6", "x", stamp(1), listOf(made), "Собирается")).allowed)
        assertTrue(protoCancelState(ProtoOrder("7", "x", stamp(1), listOf(made), "Собирается")).message.contains("производство"))
    }
}
