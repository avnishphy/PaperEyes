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
- Existing user work found untracked at the initial audit and intentionally preserved before being completed in the first feature slice:
  - `src/main/java/com/example/papereyes/domain/evidence/ReferenceEvidence.kt`
  - `src/main/java/com/example/papereyes/ocr/ReferenceDetector.kt`
- The initial `ReferenceDetector.kt` skeleton had two bodyless methods and a public/internal visibility mismatch. Slice 1 completes it and tests it; the note is retained so the original dirty-tree baseline is not confused with commit `1d61997`.
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

The manifest requests camera, internet, and network-state permissions, disables cleartext traffic and backup, and uses Android Photo Picker for imports. `MainActivity` is `singleTop` so launcher re-entry cannot stack duplicate Compose/OCR owners. Room is schema version 4. No destructive migration fallback is present.

Launcher assets use the user-selected papers-on-wood artwork, with density-specific legacy square/round icons, an adaptive full-color icon, and a paper-stack monochrome icon for themed launchers.

## Current identification pipeline

Both Import and Live Scan produce `DocumentEvidence` through `TextRecognizerService` and `TextCandidateExtractor`.

Before starting Import or Live Scan, PaperEyes asks whether the image is a journal paper, references, or a conference slide. Journal-paper mode uses only document identity evidence and ignores incidental citations. References and conference-slide modes require detected citation evidence, use that evidence to decide whether a fallback capture is needed, and never resolve a slide heading or surrounding paper as the requested work.

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
4. Paper scans use two fallback JPEGs only when evidence is weak. Reference-focused scans always verify against the sharpest fallback and conservatively merge both OCR observations.
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

1. **Reference extraction core (complete):** finished the reference model/detector with pure JVM fixtures for numbered, wrapped, DOI/arXiv, slide-footer, false-positive, layout-index, de-duplication, and bounds cases. It remains separate from document identity.
2. **Evidence integration (complete):** detected references now travel on `DocumentEvidence` without participating in `bestQuery`; focused tests preserve title-page and reference-suppression behavior.
3. **Batch domain model (complete):** selected references resolve in reading order with bounded candidates, a progress event per item, cancellation propagation, and explicit identified/ambiguous/not-found/unavailable/error outcomes.
4. **Selection/results UI (complete in source):** the reusable Compose all/subset dialog and per-reference progress/failure summary now drive both Import and Live Scan. Successful Import papers use the existing detail/save cards; identified Live items can be opened from the scrollable summary; failures stay named and visible.
5. **Camera zoom (revised after device test):** zoom is pinch-only. The explicit slider/buttons/ratio panel and aiming-frame corners are no longer rendered. Pinch still uses camera-reported limits through the bound `CameraControl`.
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
- 2026-09-21: reference extraction slice completed. Six focused detector tests pass, followed by the full `testDebugUnitTest` suite.
- 2026-09-21: evidence integration slice completed. Detector, layout, and candidate-extractor tests pass; references remain parallel to single-paper identity.
- 2026-09-21: batch resolution slice completed. Five focused tests cover order/progress, partial failures, exact-title selection, ambiguity, cancellation, exceptions, and bounds.
- 2026-09-21: Import reference UI slice compiles and the full JVM suite passes. Multiple detected references prompt for all/subset; single reference-only images resolve directly; per-item outcomes are retained.
- 2026-09-21: Live Scan reference UI slice compiles and the full JVM suite passes. Multiple references lock scanning while the user selects all/subset; cancel safely resumes scanning; single slide/footer references resolve directly; Scan again clears batch state.
- 2026-09-21: CameraX zoom slice compiles and three focused zoom tests pass. Preview, analysis, and JPEG capture remain bound to the same camera and therefore share its sensor crop. Real-device gesture/framing behavior remains to be verified in Android Studio.
- 2026-09-21: final Gradle verification passed: 122 JVM tests, 0 failures/errors/skips; `assembleDebug` produced `app-debug.apk`; `lintDebug` completed with dependency/update warnings and the pre-existing `UseKtx` warning, but no lint errors.
- 2026-09-21 device feedback: reference parsing is fast and accurate, but many lookups remain ambiguous; ambiguous candidates were not exposed; the explicit zoom panel and scan corners should be removed; the camera continued behind results; batch results need Save all and state preservation through paper detail; the DropConnect slide citation remained ambiguous.
- 2026-09-21 device feedback: Import of “Shedding light on shadow generalized parton distributions” selected a bleed-through/noisy Physical Review header instead of the clean title/DOI. The OCR evidence rule now rejects noisy journal headers as titles and accepts a unique standalone DOI from anywhere before a references section. Focused regressions pass.
- 2026-09-21: title/OCR resolution now consults bounded OpenAlex metadata when Crossref has no exact title. This specifically covers conference papers such as “Regularization of Neural Networks using DropConnect,” whose authoritative PMLR record may not resolve cleanly through Crossref. Focused provider/resolver/batch tests pass.
- 2026-09-21: Live Scan now removes the explicit zoom panel and frame corners. When scan/reference results lock the session, analysis is cleared, CameraX is unbound, and the preview is replaced by a black result background until Scan again (or selection cancellation) recreates the camera session.
- 2026-09-21: ambiguous reference rows now open their bounded candidate list; choosing a candidate opens normal paper detail. MainActivity keeps the underlying Import/Live/Library/Project screen composed beneath paper detail, so Back returns to the same remaining results. When a batch identifies more than one paper, Import and Live Scan ask whether to save all; saving continues across per-paper database failures and reports the saved count.
- 2026-09-21: final verification after device-feedback fixes passed: 125 JVM tests, 0 failures/errors/skips; `assembleDebug` succeeded; `lintDebug` has 17 warnings and no errors (dependency/version notices plus the pre-existing `UseKtx` warning).
- 2026-09-21: replaced the default launcher artwork with the user-selected paper-stack image and added legacy, adaptive, round, and monochrome launcher variants.
- 2026-09-21: launcher resource verification passed: `assembleDebug` and `lintDebug` succeeded; lint reports 0 errors and 23 warnings.
- 2026-09-21 device feedback: the initial adaptive launcher artwork appeared oversized. The stack was reframed with additional wood margin, while legacy density icons use a calibrated center crop so their apparent size remains consistent with the adaptive icon.
- 2026-09-21: added an explicit scan-subject chooser before both Import and Live Scan. Journal-paper, reference-list, and conference-slide choices now constrain which OCR evidence can trigger scholarly lookup; three focused policy regressions and the full JVM suite pass.
- 2026-09-21 crash investigation: Android's crash buffer and `ApplicationExitInfo` contained no PaperEyes crash or ANR, only explicit force-stops. A clean launch and Live Scan session remained alive on the connected A069P; the crash report still needs a reproduction or PaperEyes stack trace before a root-cause fix can be made safely.
- 2026-09-21: final scan-subject verification passed: 128 JVM tests with 0 failures/errors/skips, `assembleDebug` succeeded, and `lintDebug` completed successfully.
- 2026-09-21 live monitoring: cold start was about 1.05 s. Two successful Live Scan traces completed in 2.30 s and 3.26 s. No PaperEyes crash, ANR, or process death was recorded. Repeated launcher entry had stacked three `MainActivity` instances, raising memory to about 261 MB PSS / 414 MB RSS; `singleTop` now reuses the existing activity.
- 2026-09-21 black-screen root cause fixed: camera locking had incorrectly removed `LiveScanOverlay` together with `PreviewView`. The camera can now stop on a result or lookup failure while status, errors, results, and retry controls remain visible over the black background.
- 2026-09-21 connectivity guardrails added: lookups require a validated network, Live Scan locks on a visible offline/interrupted state with `Try again`, and a reference batch stops network requests after its first transport failure while preserving an outcome for every selected reference.
- 2026-09-21: final stability verification passed: 131 JVM tests with 0 failures/errors/skips, `assembleDebug` succeeded, and `lintDebug` completed successfully with 0 errors.
- 2026-09-21: Live Scan now explicitly observes the activity lifecycle. `ON_STOP` invalidates pending camera binds, cancels active capture/lookup work, clears analysis and camera control, and calls `unbindAll`; `ON_START` creates a fresh session only when scanning is not result-locked. Full JVM tests and `assembleDebug` pass. The planned hardware-level background-camera check could not run because the A069P disconnected before installation.
- Device baseline supplied by user: OCR works well; positive live matches take about 2–3 seconds; some clean frames are still missed.

## Latest small-update state

- Commits `b2842ba` and `d641ea6` add New project from paper details and full-screen reference selection/results. Their full JVM test runs passed before commit.
- Launcher artwork and every density-specific bitmap were restored byte-for-byte to the committed original after the proposed redesign and prefilter were rejected.
- Pending reference-stability working tree: reference scans always compare two OCR observations and merge complementary entries using labels plus conservative text similarity. New JVM tests cover complementary frames, unnumbered variants, OCR label drift, and distinct similar references.
- Final verification for commit `fdf6159` passed on 2026-09-21: 135 JVM tests with 0 failures/errors/skips, `assembleDebug` succeeded, and `lintDebug` completed with 0 errors and 23 warnings. The only compile warning shown was the existing CameraX `setTargetResolution` deprecation.
- Device regression: an unnumbered bibliography page was detected as one reference because ML Kit grouped all entries into one block. Layout parsing now splits such blocks by completed year endings, paragraph gaps, and hanging-indent returns; venue recognition also covers the full names commonly printed for ML/AI proceedings and journals. Verification passes with 136 JVM tests, a successful debug build, and lint at 0 errors/23 warnings.
- Follow-up on the same page: only the Patrick Nadeem/ICML entry survived strict citation validation. When one strong citation anchors a page and at least three neighboring paragraphs repeat author-list, citation-punctuation, and terminal-year structure, those structural entries are now accepted even if their italic venue text is garbled. A prose-only control fixture prevents this fallback from activating on ordinary paragraphs. Verification passes with 138 JVM tests, a successful debug build, and lint at 0 errors/23 warnings.

## Next action

Reconnect the A069P and verify its camera privacy indicator turns off immediately when Live Scan is sent to the background. Then verify launcher re-entry keeps one activity, toggle connectivity during lookup, and repeat the supplied bibliography, DropConnect slide, and “Shedding light…” cases in Android Studio.
