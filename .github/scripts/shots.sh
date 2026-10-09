#!/usr/bin/env bash
mkdir -p shots
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
run 1024 Home Catalog Filter ProductList ProductDetail Cart Checkout OrderSent OrderList OrderDetail Notifications ClientChat ClientReports ClientSettings AgentClients AgentClientDetail RetailHome RetailCatalog RetailFilter RetailProductList RetailProductDetail RetailCart RetailCheckout RetailOrderSent RetailOrderList RetailOrderDetail RetailNotifications Profile Suspended
run 9001 AdminHome AdminSearch AdminClients AdminClient AdminOrders AdminOrderDetail AdminNotifications AdminCatalog AdminSettings AdminSettingsDetail AdminAttention OnlineController LowStockList AdminChats AdminChat AdminProductionChat AdminReports AdminWorkshop AdminAdmins AdminAttendance Server StockList ReserveList NewClients Export AdminAssemblers
run 9002 Production ProductionCategory ProductionCatalog ProductionEntry ProductionHistory ProductionReport ProductionPayments ProductionProfile ProductionWorkshop ProductionAttendance ProductionChat
run 9003 SalesHome SalesShipmentNew SalesShipments SalesClients SalesClient
run 9001 AdminPayments
if [ -z "${ONLY:-}" ] || echo " $ONLY " | grep -q " FilterPolyanki "; then
  adb shell am force-stop ru.sansara.app
  adb shell am start -n ru.sansara.app/.MainActivity --es debug_code 1024 --es debug_screen Filter --es debug_types "Полянки" >/dev/null
  sleep 6
  adb exec-out screencap -p > "shots/07b_FilterPolyanki.png"
fi
ls shots | wc -l
