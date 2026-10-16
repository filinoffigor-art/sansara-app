package ru.sansara.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Задание в цех на настоящей базе телефона: взять в работу, пауза, частичное проведение с переносом остатка, оприходование. */
@RunWith(AndroidJUnit4::class)
class SansaraWorkshopLogicTest {
    private val repo get() = SansaraRepository.get(InstrumentationRegistry.getInstrumentation().targetContext)
    private val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    @Test
    fun partialPostingCarriesRemainderToNextDayAndCountsTime(): Unit = runBlocking {
        repo.seedDebugIfNeeded()
        suspend fun physical(sku: String) = repo.snapshot().let { s -> s.stockOverrides[sku] ?: s.products.first { it.sku == sku }.stock }
        val product = repo.snapshot().products.first()
        val stockBefore = physical(product.sku)
        val lines = JSONArray().put(JSONObject().put("sku", product.sku).put("name", product.name).put("qty", 5)).toString()
        val today = LocalDate.now().format(fmt)
        val task = repo.createWorkshopTask(today, lines, "Тест", false, "U-ADMIN")

        repo.setWorkshopTaskRunning(task.id, true)
        Thread.sleep(1200)
        repo.setWorkshopTaskRunning(task.id, false)                    // пауза: время зафиксировано
        val paused = repo.workshopTasks().first { it.id == task.id }
        assertEquals("На паузе", paused.status)
        assertTrue("время сборки должно копиться", paused.workMs >= 1000)
        repo.setWorkshopTaskRunning(task.id, true)

        val assembler = repo.saveAssembler(null, "Тест Сборщица")
        val (closed, carried) = repo.postWorkshopTask(task.id, mapOf(product.sku to 3), "Не хватило лент", assembler.id, assembler.name, mapOf(product.sku to 50), "U-PRODUCTION")
        assertEquals("Частично собрано", closed.status)
        assertEquals(3, closed.actualQty)
        assertEquals("Не хватило лент", closed.masterComment)
        assertNotNull("остаток должен перейти на следующий день", carried)
        carried!!
        assertEquals(LocalDate.now().plusDays(1).format(fmt), carried.taskDate)
        assertEquals(today, carried.carriedFrom)
        assertEquals(2, protoWorkshopPlanQty(carried.linesJson))
        assertTrue("время переносится вместе с остатком", carried.workMs >= closed.workMs)

        // Собранное оприходовано на склад.
        val stockAfter = physical(product.sku)
        assertEquals(stockBefore + 3, stockAfter)

        // Остаток собран полностью — задание закрыто без нового переноса.
        repo.setWorkshopTaskRunning(carried.id, true)
        val (done, again) = repo.postWorkshopTask(carried.id, mapOf(product.sku to 2), "", assembler.id, assembler.name, emptyMap(), "U-PRODUCTION")
        assertEquals("Собрано", done.status)
        assertNull(again)
    }
}
