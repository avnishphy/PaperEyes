# PaperEyes project context

Last updated: 2026-09-21 (America/New_York)

This is the single living context file for day-to-day PaperEyes development. Update it whenever architecture, behavior, verification status, or the active implementation slice changes. The repository-level `HANDOFF.md` and `docs/` contain the deeper September 2026 audit; this file records the concise current state needed for future edits.

## Working agreement

- Work in small, reviewable vertical slices and keep commits focused.
- Preserve unrelated or pre-existing work. Always inspect `git status` before editing or committing.
- Add regression tests before or with behavior changes.
- Run the narrowest relevant tests first, then `testDebugUnitTest`; use Android Studio for device/instrumentation checks.
- VS Code is used for source changes. Android Studio is used for the Android UI, builds, and real-device testing.
- Do not claim camera/OCR/provider behavior from JVM tests alone. Record device observations separately.

## Git and build baseline

- Repository: `C:\Users\as8oc\AndroidStudioProjects\PaperEyes`
- App module: `C:\Users\as8oc\AndroidStudioProjects\PaperEyes\app`
- Active branch at this update: `feat/reference-detection`
- `HEAD`: `1d61997` (`feat: harden PaperEyes paper identification and verification pipeline`), also `main` and `origin/main` at audit time.
- Existing user work found untracked and intentionally preserved:
  - `src/main/java/com/example/papereyes/domain/evidence/ReferenceEvidence.kt`
  - `src/main/java/com/example/papereyes/ocr/ReferenceDetector.kt`
- The untracked `ReferenceDetector.kt` is an incomplete skeleton. On 2026-09-21, `testDebugUnitTest` reached Kotlin compilation and failed only on its two bodyless methods plus public/internal visibility mismatch. Do not mistake this dirty-tree failure for the committed baseline.
- This VS Code shell does not set `JAVA_HOME`. The installed Android Studio runtime is `C:\Program Files\Android\Android Studio\jbr` (OpenJDK 25.0.3). A one-command PowerShell setup is:

  ```powershell
  $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
  ..\gradlew.bat testDebugUnitTest
  ```

- Android SDK path is configured in the ignored root `local.properties`.

## Product state verified from source

PaperEyes is a single-activity Jetpack Compose Android app with simple state-based navigation:

- Home/import/manual search: `ui/scan/ScanScreen.kt`
- Live camera scan: `ui/live/LiveScanScreen.kt` and `LiveScanUi.kt`
- Paper detail and local library/projects: `ui/detail/` and `ui/library/`
- Navigation owner and Room construction: `MainActivity.kt`

Main stack: Kotlin 2.2.10, AGP 9.4.1, compile/target SDK 37, min SDK 24, Compose Material 3, CameraX 1.6.2, on-device ML Kit Latin text recognition 16.0.1, Retrofit/Gson, Room 2.8.5, and coroutines.

The manifest requests only camera and internet permissions, disables cleartext traffic and backup, and uses Android Photo Picker for imports. Room is schema version 4. No destructive migration fallback is present.

## Current identification pipeline

Both Import and Live Scan produce `DocumentEvidence` through `TextRecognizerService` and `TextCandidateExtractor`.

`DocumentLayoutAnalyzer` separates DOI, arXiv ID, journal citation, title, and interior-text fingerprint evidence. It deliberately:

- stops normal document-identity extraction at a References/Bibliography heading;
- rejects numbered references as the scanned document's title or identifier;
- treats multiple distinct early identifiers as ambiguous rather than choosing the first;
- avoids sending raw body text as a title query;
- preserves a conservative interior-page discovery route through OpenAlex.

`PaperResolver` keeps exact DOI, arXiv, journal citation, title/OCR, and experimental interior-text routes separate. It uses bounded short-lived caching, provider pacing, identity checks, and uncertainty statuses. External services currently include Crossref, Semantic Scholar, official arXiv, INSPIRE, and OpenAlex.

Live Scan currently works as follows:

1. CameraX preview analysis runs roughly every 650 ms at 1280x720.
2. Preview OCR only decides whether enough text is visible.
3. One full-resolution JPEG is captured and OCR'd.
4. If evidence is weak, two fallback JPEGs are captured and the sharpest fallback is OCR'd.
5. Evidence is resolved; only a verified or ambiguity-safe high-score result locks scanning.
6. Temporary captures are deleted and recognizer/camera ownership is guarded across cancellation.

Observed on a real phone (reported 2026-09-21): positive Live Scan identification usually takes about 2–3 seconds. OCR itself works well, but some clean frames still fail to identify a paper.

## Current UI/data constraints relevant to upcoming work

- `DocumentEvidence.bestQuery` and `PaperResolver.resolveEvidence` are single-target APIs.
- Import stores one result list/status/error set. Live Scan presents one accepted `Paper`.
- Reference material is intentionally suppressed, so bibliography pages currently yield no searchable paper identities.
- No reference selection or batch progress/result model exists.
- Live Scan binds `Preview`, `ImageAnalysis`, and `ImageCapture`, but discards the returned Camera object. There is no `CameraControl`, pinch gesture, zoom slider, or zoom state in the UI.
- `LiveScanScreen.kt`, `LiveScanUi.kt`, `LiveScanUtils.kt`, `ScanScreen.kt`, the repositories, and citation parsing files are large. Prefer extracting focused models/composables instead of expanding these files further.

## Requested behavior

### Multiple references

- Detect multiple references in bibliography/reference-list images and conference slides.
- Ask whether to search all detected references or a selected subset.
- Keep the selection editable before network lookup.
- Resolve each selected reference independently.
- Report partial success promptly and name every selected reference that could not be identified.
- Never regress the safety rule that a citation in an ordinary paper page must not be mistaken for that page's own identity.
- Example shapes supplied by the user include bracketed, wrapped bibliography entries (`[15]`…`[23]`), DOI/arXiv-rich numbered entries (`[57]`…), and slide-footer citations such as `Wan et al., “Regularization of Neural Networks using DropConnect”, ICML 2013`.

### Live Scan zoom

- Support zoom during live scanning so users can frame distant screens/slides and small paper text.
- The capture and analysis use cases must share the selected CameraX zoom state.
- Provide discoverable controls and pinch-to-zoom where practical; clamp to camera-reported limits and reset state safely on a new camera session.

## Planned small implementation slices

1. **Reference extraction core:** finish the reference model/detector with pure JVM fixtures for numbered, wrapped, DOI/arXiv, slide-footer, false-positive, and bounds cases. Keep it separate from document identity.
2. **Evidence integration:** attach detected references to OCR evidence without changing `bestQuery`; test that single-paper scans retain current behavior.
3. **Batch domain model:** resolve selected references independently with bounded progress and explicit per-reference success/failure/ambiguity.
4. **Selection/results UI:** reusable Compose selection dialog, Import integration, then Live Scan integration. Partial failures remain visible beside successes.
5. **Camera zoom:** retain CameraX `CameraControl`/`CameraInfo`, add clamped zoom state, visible controls, and pinch handling; add pure zoom-mapping tests where possible.
6. **Verification:** unit suite, debug build/lint, Android Studio UI inspection, and real-device cases matching the supplied examples. Record timing and misses here.

## Acceptance checks for the new work

- A bibliography image yields ordered, de-duplicated references with wrapped lines joined correctly.
- A normal title page containing an incidental citation still follows the existing single-paper path.
- The user can choose all, choose a subset, cancel, and retry without stale state.
- Batch lookup does not silently drop a selected item; each ends in success, ambiguous, not found, provider unavailable, or error.
- Successes are usable even when other selected references fail.
- Failure reporting includes recognizable labels/snippets, not only a count.
- Zoom changes preview, analysis, and captured JPEG framing through the bound CameraX camera.
- Navigating away during capture/lookup leaves no camera use case, recognizer task, or temporary capture owned by the disposed screen.

## Verification record

- 2026-09-21: full app-module file inventory and current Git state inspected.
- 2026-09-21: existing repository `HANDOFF.md`, architecture/audit docs, build configuration, manifest, schemas, production package layout, and test inventory reviewed.
- 2026-09-21: real Gradle `testDebugUnitTest` attempted with Android Studio JBR; compilation failed at the pre-existing incomplete untracked `ReferenceDetector.kt` skeleton (four compiler diagnostics). No tests executed.
- Device baseline supplied by user: OCR works well; positive live matches take about 2–3 seconds; some clean frames are still missed.

## Next action

Complete slice 1 with tests, without yet modifying Import or Live Scan UI.
