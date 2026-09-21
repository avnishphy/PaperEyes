# PaperEyes — implementation and verification handoff

Audit date: **September 20, 2026 (America/New_York)**. Some logs are dated September 21 in UTC.

## Start here

The supplied ZIP was the sole implementation baseline. It does **not** contain the described `DocumentLayoutAnalyzer` / preview-frame lookup baseline, and it cannot establish commit `83ff1ed`. Its actual Live Scan path uses preview OCR as a trigger, then still-image capture. This handoff preserves that capture architecture rather than claiming an unmeasured fast-path rewrite.

Implemented changes include shared layout/document evidence, safer identifiers and citation matching, a conservative interior-page discovery foundation, bounded caching and provider pacing, cancellation/resource ownership fixes, timing instrumentation, and canonical-title database upgrades. No paid API, embedded API credential, cloud OCR, or PaperEyes backend was added.

**Release status: not cleared for public beta.** Portable checks pass, but Android builds, lint, instrumentation, actual Import/Live Scan accuracy, and the requested live provider benchmark could not be verified in this environment. Gradle distribution download fails on DNS; an Android SDK/device is also absent. Do not interpret syntax checking or compile-only dependency shims as an Android build.

| Executed check | Result |
|---|---|
| Actual Kotlin core + selected source test methods, portable runner | 100 passed, 0 failed |
| All application/test Kotlin parsed with compiler PSI | 88 files, 0 syntax errors; no Android API/type checking |
| Actual exported schema DDL exercised in SQLite | 7 passed; not Room runtime |
| Provider benchmark harness self-tests | 9 passed; not API accuracy |
| Source release invariant checker | Passed, 2 warnings |
| Android Gradle clean/test/lint/debug/release/instrumentation | Not runnable: Gradle bootstrap DNS failure |
| Live network provider benchmark | Not run: API DNS failures; real 50-paper corpus not assembled |

Room remains **schema version 4**, unchanged from the supplied source. The previous HANDOFF statement that it was version 1 was incorrect. No destructive migration was introduced.

## Reports and evidence

- `docs/AUDIT_REPORT.md`: A–V report and all twelve release questions.
- `docs/CHANGES.md`: file-by-file source/test/tool changes against the supplied archive.
- `docs/FILE_INVENTORY.md`: source/configuration inventory and review scope.
- `docs/WORKLOG.md`: reproduction → regression → implementation → verification history.
- `docs/BENCHMARK.md`: real-corpus contract, provider replay limits, and device timing procedure.
- `docs/verification/`: baseline, intermediate reproduction, and final execution logs. Historical failing logs are intentionally retained; final results are identified by `final_`.
- `docs/PACKAGE_CONTENTS.sha256`: packaged-file content manifest; excludes itself.

The final archive is additionally extracted into a clean directory and checked again. Its archive SHA-256 and the actual round-trip verification report are delivered separately to avoid a self-referential ZIP hash.

## Verification on an Android-capable machine

Keep the provided Gradle/Android/Kotlin dependency versions; they were not downgraded to suit this sandbox.

```bash
./gradlew clean
./tools/verify.sh
```

Windows PowerShell:

```powershell
.\gradlew.bat clean
.\tools\verify.ps1
```

The regular verification scripts retain unit tests, lint, debug and release builds, and device tests when a device is connected. No lint check or Gradle test was disabled.

Additional offline diagnostics, with Python 3 and a standalone Kotlin compiler (portable runner tested with Kotlin 1.9.0 and its bundled coroutine jar):

```bash
python3 tools/verify_portable.py
python3 tools/check_kotlin_syntax.py
python3 tools/verify_schema.py
python3 tools/test_benchmark_tools.py
python3 tools/check_release_invariants.py
```

The portable runner uses explicitly listed compile-only external type stubs. It does not execute Retrofit/Gson, Android, ML Kit, Compose, Room code generation, or the real JUnit runtime. Original MockWebServer tests remain in the project for the genuine Gradle suite.

Before release, resolve the permanent application ID, validate provider contracts/terms and real image/camera cases, run the minified release build, and complete actual device tests. Crossref remains in the public pool; no fictional contact email was inserted.
