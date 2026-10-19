#!/usr/bin/env bash
mkdir -p shots
# Размер экрана, плотность и масштаб шрифта задаются снаружи (проверка разных телефонов); по умолчанию — S24 Ultra.
adb shell wm size ${WM_SIZE:-1080x2340} || true
adb shell wm density ${WM_DENSITY:-450} || true
adb shell settings put system font_scale ${FONT_SCALE:-1.0} || true
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant ru.sansara.app android.permission.POST_NOTIFICATIONS || true
shot() { # code screen
  if [ -n "${ONLY:-}" ] && ! echo " $ONLY " | grep -q " $2 "; then return; fi
  adb shell am force-stop ru.sansara.app
  adb shell am start -n ru.sansara.app/.MainActivity ${1:+--es debug_code $1} --es debug_screen "$2" >/dev/null
  sleep 6
  adb exec-out screencap -p > "shots/$3_$2.png"
}
adb shell am start -n ru.sansara.app/.MainActivity >/dev/null; sleep ${WARM:-15}
n=0
run() { code=$1; shift; for s in "$@"; do n=$((n+1)); shot "$code" "$s" "$(printf %02d $n)"; done; }
run "" Welcome Login Registration RegistrationSent
run 1024 Home Popular Catalog Filter ProductList ProductDetail Cart Checkout OrderSent OrderList OrderDetail Notifications ClientChat ClientReports ClientSettings AgentClients AgentClientDetail RetailHome RetailCatalog RetailFilter RetailProductList RetailProductDetail RetailCart RetailCheckout RetailOrderSent RetailOrderList RetailOrderDetail RetailNotifications Profile Suspended AgentBase AgentMarkup AgentProductMarkup AgentOrders
run 9001 AdminHome AdminSearch AdminClients AdminClient AdminOrders AdminOrderDetail AdminNotifications AdminCatalog AdminSettings AdminSettingsDetail AdminAttention OnlineController LowStockList AdminChats AdminChat AdminProductionChat AdminReports AdminWorkshop AdminAdmins AdminAttendance Server StockList ReserveList NewClients Export AdminAssemblers
run 9002 Production ProductionCategory ProductionCatalog ProductionEntry ProductionHistory ProductionReport ProductionPayments ProductionProfile ProductionWorkshop ProductionAttendance ProductionChat
run 9003 SalesHome SalesShipmentNew SalesShipments SalesClients SalesClient SalesProfile
run 9001 AdminPayments AdminDashboard
run 1024 OwnStock
# Акция и расчёты: демо-акция, акционный каталог, корзина со смешанными товарами, сводка расчётов.
promo_shot() { # code screen name [cart]
  adb shell am force-stop ru.sansara.app
  adb shell am start -n ru.sansara.app/.MainActivity --es debug_code $1 --es debug_screen $2 --es debug_promo demo ${4:+--es debug_cart $4} >/dev/null
  sleep 6
  adb exec-out screencap -p > "shots/$3_$2.png"
}
if [ -z "${ONLY:-}" ] || echo " $ONLY " | grep -q " Promo "; then
  promo_shot 9001 AdminPromo 88
  promo_shot 1024 Home 89
  promo_shot 1024 PromoCatalog 90
  promo_shot 1024 ProductList 91
  promo_shot 1024 Cart 93 mixed
  promo_shot 9001 AdminSettlements 92
fi
# Приветственный экран: настройки оформления надписей и результат на приветствии.
if [ -z "${ONLY:-}" ] || echo " $ONLY " | grep -q " WelcomeStyle "; then
  adb shell am force-stop ru.sansara.app
  adb shell am start -n ru.sansara.app/.MainActivity --es debug_code 9001 --es debug_screen AdminSettingsDetail --es debug_section welcome --es debug_welcome demo >/dev/null
  sleep 6
  adb exec-out screencap -p > "shots/85_WelcomeStyleSettings.png"
  adb shell input swipe 540 1900 540 500 400; sleep 2
  adb exec-out screencap -p > "shots/86_WelcomeStyleEditor.png"
  adb shell am force-stop ru.sansara.app
  adb shell am start -n ru.sansara.app/.MainActivity >/dev/null
  sleep 6
  adb exec-out screencap -p > "shots/87_WelcomeStyled.png"
fi
if [ -z "${ONLY:-}" ] || echo " $ONLY " | grep -q " FilterPolyanki "; then
  adb shell am force-stop ru.sansara.app
  adb shell am start -n ru.sansara.app/.MainActivity --es debug_code 1024 --es debug_screen Filter --es debug_types "Полянки" >/dev/null
  sleep 6
  adb exec-out screencap -p > "shots/07b_FilterPolyanki.png"
fi
ls shots | wc -l
