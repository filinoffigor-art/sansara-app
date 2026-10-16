#!/usr/bin/env bash
# Стресс-прогон: случайные нажатия, прокрутки и ввод во всех ролях. Падения и зависания — в shots/00_monkey.txt.
mkdir -p shots
OUT=shots/00_monkey.txt
adb shell wm size 1080x2340 || true
adb shell wm density 450 || true
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell pm grant ru.sansara.app android.permission.POST_NOTIFICATIONS || true
EVENTS=${MONKEY_EVENTS:-3000}
echo "Стресс-прогон: $EVENTS событий на прогон, экран 1080x2340/450dpi" > $OUT
crashes=0
for spec in "1024 Home 11" "1024 Cart 12" "9001 AdminHome 21" "9001 AdminOrders 22" "9001 AdminSettings 23" "9002 Production 31" "9002 ProductionWorkshop 32" "9003 SalesHome 41"; do
  set -- $spec; code=$1; scr=$2; seed=$3
  adb shell am force-stop ru.sansara.app
  adb logcat -c
  adb shell am start -n ru.sansara.app/.MainActivity --es debug_code $code --es debug_screen $scr >/dev/null
  sleep 6
  timeout 300 adb shell monkey -p ru.sansara.app -s $seed --throttle 80 --pct-syskeys 0 --pct-appswitch 0 --pct-anyevent 0 --ignore-security-exceptions -v $EVENTS > monkey_$seed.log 2>&1
  res=$(grep -E "Monkey finished|// CRASH|NOT RESPONDING|ANR" monkey_$seed.log | head -3 | tr '\n' ' ')
  echo "--- роль $code, старт $scr, seed $seed: ${res:-без итога}" >> $OUT
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then
    crashes=$((crashes+1))
    adb logcat -d | grep -A 45 "FATAL EXCEPTION" | grep -E "AndroidRuntime" | head -60 >> $OUT
    adb exec-out screencap -p > shots/monkey_crash_$seed.png
  fi
  if adb logcat -d | grep -q "ANR in ru.sansara.app"; then
    adb logcat -d | grep -A 12 "ANR in ru.sansara.app" | head -20 >> $OUT
  fi
  # Память приложения в конце прогона.
  adb shell dumpsys meminfo ru.sansara.app | grep -E "TOTAL PSS|TOTAL:" | head -1 | sed 's/^/  память: /' >> $OUT
done
echo "Падений: $crashes" >> $OUT
cat $OUT
