#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

python3 tools/check_release_invariants.py

./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease

if command -v adb >/dev/null 2>&1 && adb devices | awk 'NR>1 && $2=="device" { found=1 } END { exit !found }'; then
  ./gradlew connectedDebugAndroidTest
else
  echo "No connected Android device/emulator; skipping connectedDebugAndroidTest."
fi
