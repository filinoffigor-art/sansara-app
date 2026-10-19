package ru.sansara.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Расчёты: отсрочка со дня отгрузки, правило клиента и заказа, итоги «Оборот = Оплачено + Отсрочка + …». */
class SansaraSettlementLogicTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val client = ProtoClient("C-1", "ООО Тест", "Иван", "", "Оптовик", 0, 0, 0, "", false, "", deferralEnabled = true, deferralDays = 14)
    private val noDeferral = client.copy(deferralEnabled = false, deferralDays = 0)

    private fun order(id: String, total: Int, shipped: String? = null, status: String = "Доставлен", deferral: Int = -1, payments: List<ProtoPayment> = emptyList()): ProtoOrder {
        val history = buildList {
            add(ProtoOrderEvent("Получен", "01.10.2026 10:00", "Клиент"))
            if (shipped != null) add(ProtoOrderEvent("Доставляется", "$shipped 12:00", "Админ"))
        }
        return ProtoOrder(id, "ООО Тест", "01.10.2026 10:00", listOf(ProtoOrderLine("A", "Венок", 1, total)), status, history, clientId = "C-1", payments = payments, deferralDays = deferral)
    }

    @Test
    fun deferralCountsFromShipmentByClientRule() {
        val t = protoOrderTerms(order("1", 10_000, shipped = "10.10.2026"), client, today)
        assertEquals(14, t.days)
        assertEquals("client", t.source)
        assertEquals(LocalDate.of(2026, 10, 24), t.dueDate)
        assertEquals(ProtoPayState.DEFERRED, t.state)
        assertEquals(4, t.daysLeft)
    }

    @Test
    fun notShippedYetHasNoDueDate() {
        val t = protoOrderTerms(order("1", 10_000, shipped = null, status = "Собирается"), client, today)
        assertEquals(ProtoPayState.DEFERRED, t.state)
        assertNull(t.dueDate)
    }

    @Test
    fun overdueAfterTerm() {
        val t = protoOrderTerms(order("1", 10_000, shipped = "01.10.2026"), client, today)
        assertEquals(ProtoPayState.OVERDUE, t.state)
        assertEquals(-5, t.daysLeft)
    }

    @Test
    fun orderOverrideBeatsClientRule() {
        assertEquals(ProtoPayState.DUE, protoOrderTerms(order("1", 10_000, shipped = "10.10.2026", deferral = 0), client, today).state)
        val t = protoOrderTerms(order("2", 10_000, shipped = "10.10.2026", deferral = 30), noDeferral, today)
        assertEquals("order", t.source)
        assertEquals(LocalDate.of(2026, 11, 9), t.dueDate)
    }

    @Test
    fun clientReportedPaymentCountsOnlyAfterConfirmation() {
        val pending = ProtoPayment(4_000, "15.10.2026", "Наличные водителю", id = "p1", confirmed = false, fromClient = true)
        val o = order("1", 10_000, shipped = "10.10.2026", payments = listOf(pending))
        assertEquals(0, o.paid)
        assertEquals(4_000, o.pendingPaid)
        assertEquals(10_000, o.due)
        val confirmed = o.copy(payments = listOf(pending.copy(confirmed = true)))
        assertEquals(4_000, confirmed.paid)
        assertEquals(6_000, confirmed.due)
    }

    @Test
    fun turnoverEqualsPaidPlusDeferredPlusOverduePlusDue() {
        val orders = listOf(
            order("paid", 10_000, shipped = "10.10.2026", payments = listOf(ProtoPayment(10_000, "11.10.2026", "Наличные", id = "a"))),
            order("def", 20_000, shipped = "15.10.2026", payments = listOf(ProtoPayment(5_000, "16.10.2026", "Наличные", id = "b"))),
            order("od", 7_000, shipped = "01.10.2026"),
            order("due", 3_000, shipped = "18.10.2026", deferral = 0),
            order("cancel", 99_000, status = "Отменён"),
            order("approval", 50_000, status = "На согласовании")
        )
        val s = protoSettlement(orders, { client }, today)
        assertEquals(40_000, s.turnover)
        assertEquals(15_000, s.paid)
        assertEquals(15_000, s.deferred)
        assertEquals(7_000, s.overdue)
        assertEquals(3_000, s.due)
        assertEquals(s.turnover, s.paid + s.deferred + s.overdue + s.due)
        assertEquals(1, s.overdueOrders)
    }
}
