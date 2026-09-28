# SANSARA 0.9.0 — QA checklist

## Перед сборкой
- [x] MainActivity запускает SansaraVisualPrototype
- [x] 3 роли сохранены: Клиент / Администратор / Производство
- [x] Старый телефон администратора отсутствует
- [x] Телефон поддержки +7 926 304-60-19
- [x] Статическая дата 23.09.2026 удалена
- [x] 120 тестовых товаров: 20 × 6 категорий
- [x] Все ProtoScreen имеют when-route
- [x] Все drawable refs существуют
- [x] Нет дублирующихся функций в SansaraVisualPrototype
- [x] Базовая синтаксическая проверка Kotlin: нет expecting/redeclaration/too many args/no value passed/type mismatch

## Проверить после GitHub Actions / на APK
### Клиент
Welcome → Registration → Sent → Login(1024) → Home → Catalog/Filter → Product → Cart → Checkout → Order sent → Orders/Profile.

### Администратор
Role picker → Admin → Search → Clients → Client card → Orders/status → Production → Stock → Attention → Online → Settings → Tilda/Export.

### Производство
Role picker → Production → Add item → Category → Search → Product → Assembler/Qty → Add daily draft → Post → History → Stock.

## Visual Lock
Сверить экраны Welcome/Registration/Home/Catalog/Product/Cart/Checkout/OrderSent/AdminHome/ClientCard/Production/Server с утверждёнными reference JPEG.
