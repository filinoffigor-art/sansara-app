package ru.sansara.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Акции: акционная цена окончательная, старая цена, товары «только по акции», сроки, фильтр «По акции». */
class SansaraPromoLogicTest {
    private val today = LocalDate.of(2026, 10, 20)
    private fun product(sku: String, price: Int, type: String = "Венки") = ProtoCatalogProduct(sku, "Венок $sku", type, "Премиум", "110 см", price, 5, "В наличии", 3)
    private val catalog = listOf(product("A", 3000), product("B", 2000), product("C", 1500, "Корзины"))
    private val promo = ProtoPromo("Осень", "demo", "", "31.10.2026", true, listOf(
        ProtoPromoItem("A", "Венок A", "Венки", "Премиум", "110 см", 2400, 0, "", 3),
        ProtoPromoItem("X", "Корзина X", "Корзины", "Стандарт", "70 см", 1000, 1300, "", 5)
    ))

    @Test
    fun promoPriceReplacesCatalogPriceAndKeepsOldPrice() {
        val shop = protoApplyPromo(catalog, promo, today)
        val a = shop.first { it.sku == "A" }
        assertTrue(a.promo)
        assertEquals(2400, a.price)
        assertEquals(3000, a.promoOldPrice)
        assertEquals(20, protoPromoPercent(a))
        assertFalse(shop.first { it.sku == "B" }.promo)
    }

    @Test
    fun promoOnlyItemIsAddedWithOldPriceFromFeed() {
        val x = protoApplyPromo(catalog, promo, today).first { it.sku == "X" }
        assertTrue(x.promo)
        assertEquals(1300, x.promoOldPrice)
        assertEquals("Корзины", x.type)
    }

    @Test
    fun clientDiscountDoesNotStackOnPromo() {
        val shop = protoApplyPromo(catalog, promo, today)
        assertEquals(2400, protoShopPrice(shop.first { it.sku == "A" }, 10))
        assertEquals(1800, protoShopPrice(shop.first { it.sku == "B" }, 10))
        assertEquals(3000, protoListPrice(shop.first { it.sku == "A" }))
    }

    @Test
    fun promoRespectsDatesAndSwitch() {
        assertTrue(promo.isActive(today))
        assertFalse(promo.isActive(LocalDate.of(2026, 11, 1)))
        assertFalse(promo.copy(enabled = false).isActive(today))
        assertFalse(promo.copy(from = "25.10.2026").isActive(today))
        assertEquals(catalog, protoApplyPromo(catalog, promo.copy(enabled = false), today))
    }

    @Test
    fun filterByPromo() {
        val shop = protoApplyPromo(catalog, promo, today)
        val f = ProtoCatalogFilter("", emptySet(), emptySet(), emptySet(), setOf(PROTO_PROMO_FILTER), null, null)
        assertEquals(listOf("A", "X"), shop.filter { protoMatches(it, f, 5) }.map { it.sku })
    }
}
