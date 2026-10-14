#!/usr/bin/env bash
# Экран как у Samsung Galaxy S24 Ultra (разрешение по умолчанию FHD+ 1080×2340, плотность 450 dpi).
adb shell wm size 1080x2340 || true
adb shell wm density 450 || true
timeout 1800 gradle :app:connectedDebugAndroidTest --continue --info > uitests.log 2>&1 || true
echo "exit=$?" >> uitests.log
