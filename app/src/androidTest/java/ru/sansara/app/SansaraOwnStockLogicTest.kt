package ru.sansara.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** «Мой склад», коды сотрудников и «Глаз бога» на настоящей базе телефона. */
@RunWith(AndroidJUnit4::class)
class SansaraOwnStockLogicTest {
    private val repo get() = SansaraRepository.get(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun ownStockReceiveSellAndSerialProtection(): Unit = runBlocking {
        repo.seedDebugIfNeeded()
        val c = "C-1024"
        repo.addOwnStockOp(c, "SKU-T1", "Тестовый венок", 5, "Приход")
        repo.addOwnStockOp(c, "SKU-T1", "Тестовый венок", 1, "Приход", serial = "X1")
        assertEquals(6, repo.ownStock(c)["SKU-T1"])
        // Тот же экземпляр второй раз не оприходуется.
        assertTrue(runCatching { repo.addOwnStockOp(c, "SKU-T1", "Тестовый венок", 1, "Приход", serial = "X1") }.isFailure)
        // Продать больше, чем есть, нельзя.
        assertTrue(runCatching { repo.addOwnStockOp(c, "SKU-T1", "Тестовый венок", -7, "Продажа") }.isFailure)
        repo.addOwnStockOp(c, "SKU-T1", "Тестовый венок", -4, "Продажа", customer = "Иванов")
        assertEquals(2, repo.ownStock(c)["SKU-T1"])
        // Внешний учёт: продажа без остатка записывается в историю, остатки не меняет.
        repo.addOwnStockOp(c, "SKU-T2", "Корзина", -3, OWN_EXTERNAL_SALE, customer = "Петров")
        assertEquals(null, repo.ownStock(c)["SKU-T2"])
        assertEquals(1, repo.ownStockOps(c).count { it.kind == OWN_EXTERNAL_SALE })
    }

    @Test
    fun staffCodeAndGodEyeOnlyForMainAdmin(): Unit = runBlocking {
        repo.seedDebugIfNeeded()
        val (prod, code) = repo.addAdmin("Цех Тест", "9160001122", "U-ADMIN", SansaraRole.PRODUCTION)
        assertEquals(code, repo.staffCode(prod.userId, "U-ADMIN"))
        val newCode = repo.reissueStaffCode(prod.userId, "U-ADMIN")
        assertTrue(newCode != code)
        assertEquals(null, repo.authenticate(code))
        assertEquals(prod.userId, repo.authenticate(newCode)?.userId)
        // Глаз бога: главный видит клиентов и производство, остальные — нет.
        val targets = repo.godEyeTargets("U-ADMIN")
        assertTrue(targets.any { it.role == SansaraRole.CLIENT } && targets.any { it.role == SansaraRole.PRODUCTION })
        assertTrue(runCatching { repo.godEyeTargets(prod.userId) }.isFailure)
    }
}
