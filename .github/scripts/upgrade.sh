#!/usr/bin/env bash
# Проверка обновления: ставим прошлую рабочую версию, создаём в ней данные (код администратора),
# ставим новую версию ПОВЕРХ (без удаления) и проверяем, что данные на месте и приложение не падает.
mkdir -p shots
OUT=shots/00_upgrade.txt
adb shell wm size 1080x2340 || true
adb shell wm density 450 || true
UI="python3 .github/scripts/ui.py"
{
echo "Обновление поверх прошлой версии"
adb uninstall ru.sansara.app >/dev/null 2>&1 || true
adb install prev/prev.apk && echo "старая версия установлена: $(cat prev/PREV.txt)"
adb shell pm grant ru.sansara.app android.permission.POST_NOTIFICATIONS || true
adb shell am start -n ru.sansara.app/.MainActivity >/dev/null; sleep 8
# Первый запуск рабочей сборки: задаём код главного администратора.
$UI tap "Код администратора" && adb shell input text "UPG123"
$UI tap "Повторите код" && adb shell input text "UPG123"
adb shell input keyevent 111 || true
$UI tap "Сохранить" && sleep 2
adb exec-out screencap -p > shots/upgrade_1_old.png
adb shell am force-stop ru.sansara.app
echo "--- ставим новую версию поверх ---"
adb logcat -c
if adb install -r app/build/outputs/apk/release/app-release.apk; then echo "УСТАНОВКА ПОВЕРХ: успешно"; else echo "УСТАНОВКА ПОВЕРХ: ОШИБКА"; fi
adb shell dumpsys package ru.sansara.app | grep -m1 -E "versionName" | sed 's/^ */версия после обновления: /'
adb shell am start -n ru.sansara.app/.MainActivity >/dev/null; sleep 8
if $UI find "Первый запуск" 4 >/dev/null; then echo "ДАННЫЕ ПОТЕРЯНЫ: снова спрашивает код администратора"; else echo "окно первого запуска не появилось — администратор сохранился"; fi
$UI tap "Войти" && sleep 2
adb shell input text "UPG123"; sleep 1
adb shell input keyevent 66; sleep 1
$UI tap "Войти" 5 || true
sleep 4
if $UI find "BOSS" 15; then echo "ВХОД ПОСЛЕ ОБНОВЛЕНИЯ: успешно (Здравствуйте, BOSS) — данные сохранились"; else echo "ВХОД ПОСЛЕ ОБНОВЛЕНИЯ: не удалось"; fi
adb exec-out screencap -p > shots/upgrade_2_new.png
if adb logcat -d | grep -q "FATAL EXCEPTION"; then echo "ПАДЕНИЕ после обновления:"; adb logcat -d | grep -A 30 "FATAL EXCEPTION" | grep AndroidRuntime | head -40; else echo "падений нет"; fi
} > $OUT 2>&1
cat $OUT
