#!/usr/bin/env python3
"""Fast source-level release guards that do not require Android SDK/network."""
from pathlib import Path
import os
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
errors: list[str] = []
warnings: list[str] = []


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

all_main = "\n".join(
    p.read_text(encoding="utf-8", errors="replace")
    for p in MAIN.rglob("*")
    if p.is_file() and p.suffix in {".kt", ".xml"}
)

database = text("app/src/main/java/com/example/papereyes/data/local/PaperDatabase.kt")
manifest = text("app/src/main/AndroidManifest.xml")
gradle = text("app/build.gradle.kts")
scan_screen = text("app/src/main/java/com/example/papereyes/ui/scan/ScanScreen.kt")
live_scan = text("app/src/main/java/com/example/papereyes/ui/live/LiveScanScreen.kt")

ocr_service = text("app/src/main/java/com/example/papereyes/ocr/TextRecognizerService.kt")
semantic_api = text("app/src/main/java/com/example/papereyes/data/remote/SemanticScholarApi.kt")

require("version = 4" in database, "Room schema version must remain monotonic at the current pre-release baseline (v4)")
require("exportSchema = true" in database, "Room schema export must be enabled")
require("fallbackToDestructiveMigration" not in all_main, "Destructive Room migration fallback is forbidden")
require('android:allowBackup="false"' in manifest, "Application backup must remain disabled")
require('android:usesCleartextTraffic="false"' in manifest, "Cleartext network traffic must remain disabled")
require("HttpURLConnection" not in all_main, "Use shared Retrofit/OkHttp stack; raw HttpURLConnection found")
require("org.opencv" not in all_main and "opencv" not in gradle.lower(), "Unused OpenCV must not be shipped")
require("encoded = true" not in all_main, "Pre-encoded Retrofit path parameters are forbidden")
require("GlobalScope" not in all_main, "GlobalScope is forbidden")
require("runBlocking" not in "\n".join(p.read_text(encoding='utf-8', errors='replace') for p in (MAIN / 'java').rglob('*.kt')), "runBlocking is forbidden in production source")
require("printStackTrace" not in all_main, "printStackTrace is forbidden in production source")
require("android.permission.READ_EXTERNAL_STORAGE" not in manifest, "Photo Picker should not require storage permission")
require("android.permission.WRITE_EXTERNAL_STORAGE" not in manifest, "Photo Picker should not require storage permission")
require("org.opencv:opencv" not in gradle, "OpenCV dependency must remain removed until actually used")
require("42.dp" not in all_main, "Interactive 42dp controls remain; use at least 48dp touch targets")
require("BuildConfig.DEBUG" in scan_screen and "rawOcrText.isNotBlank()" in scan_screen, "Imported-image raw OCR must be debug-only")
require("BuildConfig.DEBUG" in live_scan, "Live scanner debug OCR must be gated to debug builds")
require("STRATEGY_KEEP_ONLY_LATEST" in live_scan, "Live scan must keep CameraX backpressure on the latest frame")
require("LIVE_ANALYSIS_INTERVAL_MS" not in live_scan, "Fixed live OCR polling delays must not return to the fast path")
require("recognizeMediaImage" in live_scan, "Live scan must OCR CameraX media images directly on the fast path")
require('paper/search/match' in semantic_api, "Semantic Scholar title-match fast path is missing")
require("InputImage.fromFilePath" in ocr_service, "Imported images must use ML Kit's URI loading path")
require(not (ROOT / "gradle/gradle-daemon-jvm.properties").exists(), "Do not force a separately downloaded daemon JVM")
if os.name != "nt":
    require((ROOT / "gradlew").stat().st_mode & 0o111 != 0, "gradlew must be executable on Unix/macOS")

if 'applicationId = "com.example.papereyes"' in gradle:
    warnings.append("Application ID is still com.example.papereyes; choose the permanent ID before the first store release.")
if "mailto:" not in all_main:
    warnings.append("Crossref has no contact email configured, so PaperEyes must remain on the public request pool/rate limit.")

# Catch common accidentally committed credentials. Values in tests/examples are excluded.
secret_patterns = [
    r"AIza[0-9A-Za-z_-]{30,}",
    r"sk-[0-9A-Za-z]{20,}",
    r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",
]
for pattern in secret_patterns:
    require(re.search(pattern, all_main) is None, f"Potential committed secret matched {pattern!r}")

unit_tests = list((ROOT / "app/src/test").rglob("*Test.kt"))
android_tests = list((ROOT / "app/src/androidTest").rglob("*Test.kt"))
require(len(unit_tests) >= 7, f"Expected stringent JVM suite; found only {len(unit_tests)} test classes")
require(len(android_tests) >= 3, f"Expected instrumentation suite; found only {len(android_tests)} test classes")

if errors:
    print("Release invariant checks FAILED:")
    for error in errors:
        print(f"  - {error}")
    sys.exit(1)

print(f"Release invariant checks PASS ({len(unit_tests)} JVM test classes, {len(android_tests)} instrumentation test classes)")
for warning in warnings:
    print(f"WARNING: {warning}")
