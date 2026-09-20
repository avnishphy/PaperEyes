# PaperEyes production-hardening handoff

This archive is based on the supplied PaperEyes repository and contains the production-hardening changes plus the expanded test suite.

## Current development/release baseline

- Room schema is version 4 and remains monotonic during development.
- Room schema export is enabled.
- No destructive migration fallback is present.
- There is no released legacy schema to preserve yet; clear/uninstall incompatible pre-release app data when necessary.
- `gradlew` is executable on Unix/macOS.
- OpenCV and the unused perspective-correction implementation were removed.

## Live Scan architecture

The normal clean-scan path is now real-time:

1. CameraX `ImageAnalysis` at a preferred 1280x720 resolution.
2. `STRATEGY_KEEP_ONLY_LATEST`; no fixed 650 ms polling delay.
3. ML Kit consumes the current `media.Image` directly.
4. OCR geometry is analyzed as a document layout rather than flattened into one text block.
5. Relative line heights are clustered as a font-size proxy; the dominant prose cluster becomes the body-text baseline.
6. DOI/arXiv identifiers can resolve immediately.
7. Complete journal headers/citations (journal + volume + locator + year) are retained as strong structured evidence even when printed smaller than the title.
8. Large-font adjacent lines are reconstructed into multi-line title candidates; single-size body-only frames intentionally produce no lookup query.
9. Title OCR requires two rapidly agreeing observations.
10. Semantic Scholar `/paper/search/match` is the interactive title fast path.
11. Crossref/INSPIRE remain structured/fallback resolvers.
12. A single 1080p `ImageCapture` still is used only after the live path has failed for about two seconds.

Debug builds log OCR and final scan timing to Logcat using the tag `PaperEyesLiveScan`. No recognized title text is written to Logcat.

## Import architecture

Photo Picker remains the source-selection API. Imported images are passed directly from their `content://`/file URI to ML Kit via `InputImage.fromFilePath`, avoiding the previous custom 2400-pixel downsample that could remove small title detail.

Import and Live Scan now share the same `DocumentLayoutAnalyzer`, so identifiers, journal headers, relative font sizes and title grouping follow one policy instead of diverging.

## Verification performed in the handoff environment

- Source-level release invariant checker: PASS.
- Pure Kotlin harness for identifier parsing, title extraction, OCR similarity, and live candidate gating: PASS.
- Semantic Scholar Retrofit DTO/interface compile harness with stubs: PASS.
- Android Gradle tasks could not execute in the handoff sandbox because the Gradle 9.6 distribution is not cached and DNS/network access to `services.gradle.org` is unavailable. This is an environment limitation, not a claimed Android build pass for these latest live-scan changes.

The uploaded baseline immediately before these changes had already passed on the user's machine:

- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- `assembleRelease`
- `connectedDebugAndroidTest` on a Pixel 7 Android 13 AVD

Run the suite again after these live/import changes.

## Run on your machine

macOS/Linux:

```bash
./tools/verify.sh
```

Windows PowerShell:

```powershell
.\tools\verify.ps1
```

The verification script runs:

1. release/source invariants
2. `testDebugUnitTest`
3. `lintDebug`
4. `assembleDebug`
5. `assembleRelease`
6. `connectedDebugAndroidTest` when an Android device/emulator is connected

## Before the first store release

Two intentional product-owner decisions remain:

- Replace `com.example.papereyes` with your permanent application ID if that package name is not intended for release.
- Crossref currently has no user-provided contact email, so PaperEyes remains on Crossref's public request-pool etiquette/rate limit rather than inventing contact information.
