#!/usr/bin/env bash
# Экран как у Samsung Galaxy S24 Ultra (разрешение по умолчанию FHD+ 1080×2340, плотность 450 dpi).
adb shell wm size 1080x2340 || true
adb shell wm density 450 || true
mkdir -p shots
if [ "${SKIP_TESTS:-}" != "1" ]; then
  gradle :app:connectedDebugAndroidTest --continue > uitests.log 2>&1
  echo "exit=$?" >> uitests.log
  {
    echo "UI-тесты на эмуляторе (экран 1080x2340, 450 dpi):"
    for f in $(find app/build/outputs/androidTest-results -name '*.xml' 2>/dev/null); do
      grep -o '<testcase [^>]*name="[^"]*"' "$f" | sed 's/.*name="\([^"]*\)"/\1/' | while read -r t; do
        if grep -A3 "name=\"$t\"" "$f" | grep -q '<failure'; then echo "FAIL $t"; else echo "OK   $t"; fi
      done
      grep -o '<failure[^>]*message="[^"]*' "$f" | sed 's/<failure[^>]*message="/  причина: /' | cut -c1-400
    done
    grep -E "tests completed|FAILED|BUILD" uitests.log | tail -5
  } > shots/00_tests.txt
  cat shots/00_tests.txt
fi
bash .github/scripts/shots.sh
