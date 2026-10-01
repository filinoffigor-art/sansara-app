package ru.sansara.app

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SansaraRoleSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(text:String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasText(text, substring = true)).onFirst().assertExists()
    }

    private fun login(code:String, expected:String) {
        waitText("Добро пожаловать")
        compose.onNode(hasText("Войти") and hasClickAction()).performClick()
        waitText("Код доступа")
        compose.onNode(hasSetTextAction()).performTextInput(code)
        compose.onNode(hasText("Войти") and hasClickAction()).performClick()
        waitText(expected)
    }

    @Test
    fun clientLoginAndAgentRoutesOpen() {
        login("1024","Популярные товары")
        compose.onNode(hasText("Каталог") and hasClickAction()).performClick()
        waitText("Поиск по категориям")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitText("Популярные товары")
        compose.onNode(androidx.compose.ui.test.hasContentDescription("Меню")).performClick()
        waitText("Для клиентов")
        compose.onNode(hasText("Для клиентов") and hasClickAction()).performClick()
        waitText("Частные клиенты и розничная наценка")
    }

    @Test
    fun adminLoginAndCoreActionsVisible() {
        login("9001","Администратор")
        waitText("Задание в цех")
        waitText("Отчёты")
        waitText("Чаты")
        waitText("Настройки")
    }

    @Test
    fun productionLoginAndCoreActionsVisible() {
        login("9002","Выпуск продукции")
        waitText("История приходов")
        waitText("Задания цеху")
        waitText("Табель")
        waitText("Чат с админом")
    }
}
