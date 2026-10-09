#!/usr/bin/env bash
# Снимает скриншоты всех экранов через debug-хуки debug_code/debug_screen. Запуск: на работающем эмуляторе с установленным APK.
set -u
OUT="${1:-screens}"; mkdir -p "$OUT"
SRC=app/src/main/java/ru/sansara/app/SansaraVisualPrototype.kt
SCREENS=$(sed -n '/private enum class ProtoScreen/,/^}/p' "$SRC" | grep -v 'enum class' | tr -d ' \n}' | tr ',' ' ')
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
code_for() {
  case "$1" in
    Welcome|Login|Registration|RegistrationSent) echo "";;
    Admin*|OnlineController|LowStockList|Server|StockList|ReserveList|NewClients|Export) echo 9001;;
    Production*) echo 9002;;
    *) echo 1024;;
  esac
}
n=0
for s in $SCREENS; do
  n=$((n+1)); idx=$(printf "%02d" $n)
  code=$(code_for "$s")
  adb shell am force-stop ru.sansara.app
  if [ -z "$code" ]; then
    adb shell run-as ru.sansara.app sh -c 'rm -rf shared_prefs/*session* 2>/dev/null' >/dev/null 2>&1 || true
    adb shell am start -n ru.sansara.app/.MainActivity --es debug_screen "$s" >/dev/null
  else
    adb shell am start -n ru.sansara.app/.MainActivity --es debug_code "$code" --es debug_screen "$s" >/dev/null
  fi
  sleep 4
  adb exec-out screencap -p > "$OUT/${idx}_${s}.png"
  echo "shot $idx $s code=${code:-none}"
done
