package ru.sansara.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

private enum class Screen(val drawable: Int) {
    Welcome(R.drawable.screen_welcome),
    Registration(R.drawable.screen_registration),
    RegistrationSent(R.drawable.screen_reg_sent),
    Home(R.drawable.screen_client_home),
    Catalog(R.drawable.screen_catalog),
    Product(R.drawable.screen_product),
    Cart(R.drawable.screen_cart),
    Checkout(R.drawable.screen_checkout),
    OrderSent(R.drawable.screen_order_sent),
    Suspended(R.drawable.screen_suspended),
    AdminHome(R.drawable.screen_admin_home),
    ClientCard(R.drawable.screen_client_card),
    Production(R.drawable.screen_production),
    Server(R.drawable.screen_server)
}

@Composable
fun SansaraVisualPrototype() {
    var screen by remember { mutableStateOf(Screen.Welcome) }
    var showRolePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(screen.drawable),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        fun go(target: Screen) { screen = target }
        @Composable
        fun hotspot(x: Float, y: Float, w: Float, h: Float, action: () -> Unit) {
            ClickArea(maxWidth, maxHeight, x, y, w, h, action)
        }

        // Скрытый тестовый выбор роли: долго искать не надо — нажать на логотип SANSARA.
        hotspot(0.20f, 0.045f, 0.60f, 0.10f) { showRolePicker = true }

        when (screen) {
            Screen.Welcome -> {
                hotspot(.08f,.70f,.84f,.075f){ go(Screen.Home) }               // Войти
                hotspot(.08f,.78f,.84f,.075f){ go(Screen.Registration) }       // Стать партнёром
                hotspot(.20f,.91f,.60f,.06f){ dial(context) }
            }
            Screen.Registration -> {
                hotspot(.16f,.67f,.34f,.055f){ toast("Тип клиента: Агент") }
                hotspot(.50f,.67f,.40f,.055f){ toast("Тип клиента: Торгующая организация") }
                hotspot(.08f,.82f,.84f,.075f){ go(Screen.RegistrationSent) }
                hotspot(.35f,.91f,.35f,.055f){ go(Screen.Home) }
                hotspot(.02f,.06f,.12f,.08f){ go(Screen.Welcome) }
            }
            Screen.RegistrationSent -> {
                hotspot(.08f,.78f,.84f,.075f){ go(Screen.Home) }
                hotspot(.20f,.87f,.64f,.055f){ dial(context) }
            }
            Screen.Home -> {
                // Поиск/фильтры
                hotspot(.08f,.20f,.84f,.06f){ toast("Поиск по артикулу и названию") }
                hotspot(.08f,.27f,.28f,.05f){ toast("Фильтр: В наличии") }
                hotspot(.38f,.27f,.30f,.05f){ toast("Фильтр: Под заказ") }
                // Категории
                hotspot(.06f,.33f,.15f,.11f){ go(Screen.Catalog) }
                hotspot(.22f,.33f,.15f,.11f){ go(Screen.Catalog) }
                hotspot(.38f,.33f,.15f,.11f){ go(Screen.Catalog) }
                hotspot(.54f,.33f,.15f,.11f){ go(Screen.Catalog) }
                hotspot(.70f,.33f,.13f,.11f){ go(Screen.Catalog) }
                hotspot(.84f,.33f,.12f,.11f){ go(Screen.Catalog) }
                // Популярные товары
                hotspot(.06f,.49f,.43f,.36f){ go(Screen.Product) }
                hotspot(.51f,.49f,.43f,.36f){ go(Screen.Product) }
                clientBottomNav { go(it) }
            }
            Screen.Catalog -> {
                hotspot(.08f,.18f,.84f,.06f){ toast("Поиск по категориям") }
                hotspot(.05f,.24f,.22f,.05f){ toast("Показаны все товары") }
                hotspot(.29f,.24f,.28f,.05f){ toast("Показаны товары в наличии") }
                hotspot(.59f,.24f,.30f,.05f){ toast("Показаны товары под заказ") }
                // Карточки категорий — тестово ведём в товар/каталог
                hotspot(.05f,.31f,.46f,.22f){ go(Screen.Product) }
                hotspot(.52f,.31f,.43f,.22f){ toast("Категория: Гробы") }
                hotspot(.05f,.54f,.46f,.18f){ toast("Категория: Одежда") }
                hotspot(.52f,.54f,.43f,.18f){ toast("Категория: Ленты") }
                hotspot(.05f,.73f,.46f,.16f){ toast("Категория: Цветы") }
                hotspot(.52f,.73f,.43f,.16f){ toast("Категория: Услуги") }
                clientBottomNav { go(it) }
            }
            Screen.Product -> {
                hotspot(.02f,.055f,.12f,.08f){ go(Screen.Catalog) }
                hotspot(.84f,.12f,.12f,.08f){ toast("Добавлено в избранное") }
                hotspot(.18f,.56f,.18f,.06f){ toast("Количество уменьшено") }
                hotspot(.35f,.56f,.18f,.06f){ toast("Количество увеличено") }
                hotspot(.42f,.55f,.53f,.075f){ go(Screen.Cart) }
                hotspot(.08f,.83f,.84f,.07f){ toast("Похожие товары") }
                clientBottomNav { go(it) }
            }
            Screen.Cart -> {
                hotspot(.38f,.30f,.27f,.06f){ toast("Количество изменено") }
                hotspot(.38f,.45f,.27f,.06f){ toast("Количество изменено") }
                hotspot(.38f,.60f,.27f,.06f){ toast("Количество изменено") }
                hotspot(.82f,.28f,.12f,.09f){ toast("Товар удалён из корзины") }
                hotspot(.82f,.43f,.12f,.09f){ toast("Товар удалён из корзины") }
                hotspot(.82f,.58f,.12f,.09f){ toast("Товар удалён из корзины") }
                hotspot(.08f,.78f,.84f,.075f){ go(Screen.Checkout) }
                clientBottomNav { go(it) }
            }
            Screen.Checkout -> {
                hotspot(.02f,.055f,.12f,.08f){ go(Screen.Cart) }
                hotspot(.08f,.32f,.28f,.06f){ toast("Способ получения: Доставка") }
                hotspot(.37f,.32f,.28f,.06f){ toast("Способ получения: Самовывоз") }
                hotspot(.66f,.32f,.26f,.06f){ toast("Способ получения: ТК") }
                hotspot(.08f,.79f,.84f,.075f){ go(Screen.OrderSent) }
                clientBottomNav { go(it) }
            }
            Screen.OrderSent -> {
                hotspot(.08f,.72f,.84f,.075f){ toast("Заказ S-002384: передан менеджеру") }
                hotspot(.08f,.81f,.84f,.07f){ go(Screen.Catalog) }
                clientBottomNav { go(it) }
            }
            Screen.Suspended -> {
                hotspot(.08f,.70f,.84f,.075f){ dial(context) }
                hotspot(.08f,.79f,.84f,.07f){ message(context) }
                clientBottomNav { go(it) }
            }
            Screen.AdminHome -> {
                // KPI
                hotspot(.07f,.23f,.43f,.09f){ go(Screen.ClientCard) }
                hotspot(.51f,.23f,.42f,.09f){ go(Screen.ClientCard) }
                hotspot(.07f,.32f,.43f,.09f){ go(Screen.OrderSent) }
                hotspot(.51f,.32f,.42f,.09f){ go(Screen.Production) }
                hotspot(.07f,.41f,.86f,.07f){ go(Screen.Server) }
                // Быстрые действия
                hotspot(.06f,.49f,.18f,.10f){ go(Screen.ClientCard) }
                hotspot(.25f,.49f,.18f,.10f){ go(Screen.OrderSent) }
                hotspot(.44f,.49f,.18f,.10f){ go(Screen.Production) }
                hotspot(.63f,.49f,.18f,.10f){ go(Screen.Catalog) }
                hotspot(.82f,.49f,.14f,.10f){ toast("Настройки администратора") }
                // Требует внимания
                hotspot(.05f,.62f,.90f,.06f){ go(Screen.ClientCard) }
                hotspot(.05f,.69f,.90f,.06f){ go(Screen.Suspended) }
                hotspot(.05f,.76f,.90f,.06f){ go(Screen.OrderSent) }
                adminBottomNav { go(it) }
            }
            Screen.ClientCard -> {
                // Статусы
                hotspot(.05f,.35f,.23f,.05f){ toast("Статус: Активный") }
                hotspot(.29f,.35f,.23f,.05f){ toast("Статус: Оптовик") }
                hotspot(.53f,.35f,.20f,.05f){ toast("Статус: VIP") }
                hotspot(.74f,.35f,.23f,.05f){ go(Screen.Suspended) }
                hotspot(.67f,.41f,.26f,.06f){ toast("Скидка изменена") }
                hotspot(.76f,.48f,.18f,.06f){ toast("Доступ к заказам изменён") }
                hotspot(.08f,.52f,.27f,.06f){ toast("Данные клиента сохранены") }
                hotspot(.36f,.52f,.31f,.06f){ go(Screen.Suspended) }
                hotspot(.68f,.52f,.27f,.06f){ dial(context) }
                hotspot(.02f,.055f,.12f,.08f){ go(Screen.AdminHome) }
                adminBottomNav { go(it) }
            }
            Screen.Production -> {
                hotspot(.06f,.22f,.88f,.06f){ toast("Поиск товара по артикулу") }
                hotspot(.07f,.48f,.86f,.055f){ toast("Добавить позицию выпуска") }
                hotspot(.10f,.63f,.80f,.07f){ toast("Оприходовано 53 шт. Остатки клиентов обновлены") }
                hotspot(.10f,.71f,.80f,.06f){ toast("История приходов") }
                productionBottomNav { go(it) }
            }
            Screen.Server -> {
                hotspot(.02f,.055f,.12f,.08f){ go(Screen.AdminHome) }
                hotspot(.20f,.20f,.22f,.05f){ toast("Август 2026") }
                hotspot(.42f,.20f,.32f,.05f){ toast("Сентябрь 2026") }
                hotspot(.75f,.20f,.20f,.05f){ toast("Октябрь 2026") }
                hotspot(.08f,.82f,.26f,.06f){ toast("Экспорт данных") }
                hotspot(.35f,.82f,.26f,.06f){ toast("Фильтры") }
                hotspot(.62f,.82f,.32f,.06f){ toast("Открыть месяц") }
                adminBottomNav { go(it) }
            }
        }
    }

    if (showRolePicker) {
        AlertDialog(
            onDismissRequest = { showRolePicker = false },
            title = { Text("Тестовый режим SANSARA") },
            text = { Text("Выберите роль для проверки интерфейса") },
            confirmButton = {
                Column {
                    TextButton(onClick = { screen = Screen.Welcome; showRolePicker=false }) { Text("Клиент") }
                    TextButton(onClick = { screen = Screen.AdminHome; showRolePicker=false }) { Text("Администратор") }
                    TextButton(onClick = { screen = Screen.Production; showRolePicker=false }) { Text("Производство") }
                }
            }
        )
    }
}

@Composable
private fun BoxScope.ClickArea(maxWidth: Dp, maxHeight: Dp, x:Float,y:Float,w:Float,h:Float,onClick:()->Unit) {
    Box(
        Modifier
            .offset(x = maxWidth * x, y = maxHeight * y)
            .width(maxWidth * w)
            .height(maxHeight * h)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun BoxWithConstraintsScope.clientBottomNav(go:(Screen)->Unit) {
    ClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){go(Screen.Home)}
    ClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){go(Screen.Catalog)}
    ClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){go(Screen.Cart)}
    ClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){go(Screen.OrderSent)}
    ClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){go(Screen.Suspended)}
}

@Composable
private fun BoxWithConstraintsScope.adminBottomNav(go:(Screen)->Unit) {
    ClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){go(Screen.AdminHome)}
    ClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){go(Screen.ClientCard)}
    ClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){go(Screen.OrderSent)}
    ClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){go(Screen.Server)}
    ClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){go(Screen.AdminHome)}
}

@Composable
private fun BoxWithConstraintsScope.productionBottomNav(go:(Screen)->Unit) {
    ClickArea(maxWidth,maxHeight,.00f,.91f,.20f,.09f){go(Screen.AdminHome)}
    ClickArea(maxWidth,maxHeight,.20f,.91f,.20f,.09f){go(Screen.Production)}
    ClickArea(maxWidth,maxHeight,.40f,.91f,.20f,.09f){go(Screen.Production)}
    ClickArea(maxWidth,maxHeight,.60f,.91f,.20f,.09f){go(Screen.Server)}
    ClickArea(maxWidth,maxHeight,.80f,.91f,.20f,.09f){go(Screen.Production)}
}

private fun dial(context: android.content.Context) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${BuildConfig.ADMIN_PHONE}")))
}
private fun message(context: android.content.Context) {
    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${BuildConfig.ADMIN_PHONE}")))
}
