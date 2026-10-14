package ru.sansara.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Сквозные сценарии на эмуляторе (debug-сборка с демо-данными, данные очищаются перед каждым тестом).
 * Проверяют вход по коду, сохранение авторизации, регистрацию с двумя контактами и одобрение,
 * поиск с ошибочной раскладкой, фильтр главной и цены со скидкой в корзине.
 */
@RunWith(AndroidJUnit4::class)
class SansaraRoleSmokeTest {
    @get:Rule(order = 0)
    val notifications: androidx.test.rule.GrantPermissionRule = androidx.test.rule.GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String, timeout: Long = 25_000) {
        compose.waitUntil(timeoutMillis = timeout) {
            compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(text: String, substring: Boolean = false) {
        waitText(text)
        compose.onAllNodes(hasText(text, substring = substring) and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun clickDesc(desc: String) {
        compose.waitUntil(20_000) { compose.onAllNodes(hasContentDescription(desc)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasContentDescription(desc)).onFirst().performClick()
        compose.waitForIdle()
    }

    /** Поле ввода по подписи; если оно ниже экрана — прокручиваем список к нему. */
    private fun type(label: String, value: String) {
        val matcher = hasText(label) and hasSetTextAction()
        if (compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()) {
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(label))
        }
        compose.onAllNodes(matcher).onFirst().performScrollTo().performTextInput(value)
        compose.waitForIdle()
    }

    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text, substring = true))
        compose.waitForIdle()
    }

    private fun login(code: String) {
        waitText("Добро пожаловать")
        click("Войти")
        waitText("Код доступа")
        compose.onNode(hasSetTextAction()).performTextInput(code)
        compose.onAllNodes(hasText("Войти") and hasClickAction()).onLast().performClick()
        compose.waitForIdle()
    }

    @Test
    fun wrongCodeShowsClearError() {
        waitText("Добро пожаловать")
        click("Войти")
        waitText("Код доступа")
        compose.onNode(hasSetTextAction()).performTextInput("ZZ9999")
        compose.onAllNodes(hasText("Войти") and hasClickAction()).onLast().performClick()
        waitText("Неверный код")
    }

    @Test
    fun loginPersistsAndWelcomeShownOnEveryLaunch() {
        login("1024")
        waitText("Наш ассортимент")
        waitText("Здравствуйте")
        // Перезапуск экрана: снова «Добро пожаловать», но «Войти» без кода открывает главную.
        compose.activityRule.scenario.recreate()
        waitText("Добро пожаловать")
        click("Войти в кабинет")
        waitText("Наш ассортимент")
    }

    @Test
    fun searchUnderstandsWrongKeyboardLayout() {
        login("1024")
        waitText("Наш ассортимент")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("dtyjr")   // «венок» в латинской раскладке
        compose.waitUntil(15_000) { compose.onAllNodes(hasText("Венок", substring = true, ignoreCase = true)).fetchSemanticsNodes().size > 1 }
        click("Найти")
        waitText("Найдено:")
        val found = compose.onAllNodes(hasText("Найдено:", substring = true)).fetchSemanticsNodes().first()
            .config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty()
        assertTrue("ожидали результаты поиска, получили «$found»", found.filter(Char::isDigit).toInt() > 0)
    }

    @Test
    fun homeFilterAppliesOnlyByButtonAndCloseCancels() {
        login("1024")
        waitText("Наш ассортимент")
        clickDesc("Фильтр")
        waitText("Показать")
        click("Полянки")
        clickDesc("Закрыть")                       // закрытие без применения
        waitText("Наш ассортимент")
        assertTrue(compose.onAllNodes(hasText("по фильтру", substring = true)).fetchSemanticsNodes().isEmpty())
        clickDesc("Фильтр")
        waitText("Показать")
        click("Полянки")
        click("Показать", substring = true)
        waitText("по фильтру")
    }

    @Test
    fun cartPricesIncludePartnerDiscount() {
        login("1024")                              // демо-клиент со скидкой 10 %
        waitText("Наш ассортимент")
        scrollTo("В корзину")
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performTouchInput { swipeUp(startY = centerY, endY = top) }
        compose.waitForIdle()
        compose.onAllNodes(hasText("В корзину") and hasClickAction()).onFirst().performClick()
        waitText("1 шт.")                          // товар добавлен — на карточке счётчик вместо кнопки
        click("Корзина")
        waitText("Оформить заказ")
        waitText("уже в ценах")
    }

    @Test
    fun registrationWithSecondContactApprovedAndBothCodesWork() {
        waitText("Добро пожаловать")
        click("Стать партнёром")
        waitText("Регистрация партнёра")
        type("ФИО агента или ИП", "ИП Тестов Тест")
        type("ИНН", "771234567890")
        type("Фамилия", "Тестов")
        type("Имя", "Тест")
        type("Телефон", "9161112233")
        type("E-mail", "test@example.ru")
        type("Город", "Москва")
        type("Адрес доставки", "ул. Тестовая, 1")
        type("Примерный объём продаж в месяц, ₽", "500000")
        scrollTo("Добавить второе контактное лицо")
        click("Добавить второе контактное лицо", substring = true)
        type("ФИО второго контакта", "Вторая Анна")
        type("Телефон второго контакта", "9162223344")
        scrollTo("Согласен на обработку")
        click("Согласен на обработку персональных данных", substring = true)
        click("Отправить заявку")
        waitText("Заявка отправлена")
        click("Понятно")

        login("9001")
        waitText("Сегодня")
        click("Заявки на регистрацию")
        waitText("ИП Тестов Тест")
        waitText("500 000")                        // объём продаж виден администратору
        click("Одобрить…")
        compose.onAllNodes(hasText("Одобрить") and hasClickAction()).onLast().performClick()
        waitText("отправьте коды")
        val codes = compose.onAllNodes(hasText("· код ", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString()?.substringAfter("· код ")?.trim() }
            .filter { it.length == 6 }.distinct()
        assertEquals("у каждого контакта свой код", 2, codes.size)
        click("Готово")

        // Выход администратора: назад к главной (карточка клиента → заявки → главная), вкладка «Профиль».
        repeat(2) { compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
        waitText("Сегодня")
        click("Профиль")
        scrollTo("Выйти из аккаунта")
        click("Выйти из аккаунта")
        compose.onAllNodes(hasText("Выйти") and hasClickAction()).onLast().performClick()

        // Оба контакта входят в один кабинет компании.
        codes.forEach { code ->
            login(code)
            waitText("Наш ассортимент")
            waitText("Статус: Активный")
            clickDesc("Меню")
            click("Профиль")
            click("Выйти")
        }
    }

    @Test
    fun adminIssuesProductionCodeAndProductionLogsIn() {
        login("9001")
        waitText("Сегодня")
        click("Профиль")
        scrollTo("Сотрудники и коды")
        click("Сотрудники и коды")
        waitText("Новый сотрудник")
        click("Производство")
        type("Имя", "Цех Тестовый")
        type("Телефон (10 цифр)", "9163334455")
        click("Добавить и выдать код")
        waitText("Код для «Цех Тестовый»")
        val code = compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
            .first { it.length == 6 && it.all(Char::isLetterOrDigit) && it.any(Char::isDigit) }
        waitText("Производство · активен")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        scrollTo("Выйти из аккаунта")
        click("Выйти из аккаунта")
        compose.onAllNodes(hasText("Выйти") and hasClickAction()).onLast().performClick()
        login(code)
        waitText("Выпуск")
    }
}
