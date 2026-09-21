# File-by-file changes

Compared against the supplied archive, not against an assumed remote commit. Logs are indexed separately under `verification/`. No production Kotlin files were removed. Newly added type shims live only under `tools/offline/stubs`, outside Android source sets.

| Path | Status | Change / reason |
|---|---|---|
| `HANDOFF.md` | modified | Replaces stale schema/version claims with actual changes, test scope and release limitations. |
| `app/src/androidTest/java/com/example/papereyes/data/local/PaperDatabaseTest.kt` | modified | Adds concurrent duplicate-save and membership-preserving upgrade instrumentation tests; unrun here. |
| `app/src/main/java/com/example/papereyes/data/local/PaperDao.kt` | modified | Canonical-title fallback matches Kotlin identity normalization; indexed exact lookup retained. |
| `app/src/main/java/com/example/papereyes/data/model/ScholarlyIdentifiers.kt` | modified | Strict OCR-tolerant arXiv version parsing and work-level identity helper; DOI normalization retained. |
| `app/src/main/java/com/example/papereyes/data/remote/InspireRepository.kt` | modified | Shared pacing and stronger journal/volume/locator identity with contradiction rejection. |
| `app/src/main/java/com/example/papereyes/data/remote/OpenAlexApi.kt` | added | Typed minimal response DTOs and bounded anonymous search endpoint. |
| `app/src/main/java/com/example/papereyes/data/remote/OpenAlexProvider.kt` | added | Capability-limited phrase discovery with local abstract verification texts; no key or full-text download. |
| `app/src/main/java/com/example/papereyes/data/remote/PaperRepository.kt` | modified | Shared provider policy, DOI response identity, Retry-After handling and safer citation/series matching. |
| `app/src/main/java/com/example/papereyes/data/remote/RetryAfter.kt` | added | Safe seconds/date parsing for server cooldowns. |
| `app/src/main/java/com/example/papereyes/data/remote/ScholarlyRequestPolicy.kt` | added | Process-wide per-provider request gates and rate-limit response handling. |
| `app/src/main/java/com/example/papereyes/domain/ArxivFeedParser.kt` | added | Entry-scoped streaming Atom metadata parsing, requested identity/version checks. |
| `app/src/main/java/com/example/papereyes/domain/PaperResolver.kt` | modified | Evidence-aware routes, bounded cache, validated identifier results, official Atom fallback and conservative interior status. |
| `app/src/main/java/com/example/papereyes/domain/discovery/AbstractReconstruction.kt` | added | Bounded inverted-index reconstruction without invented text across gaps/conflicts. |
| `app/src/main/java/com/example/papereyes/domain/discovery/ArxivEntryIdentity.kt` | added | Strict returned arXiv identity validation, including requested versions. |
| `app/src/main/java/com/example/papereyes/domain/discovery/InteriorPageDiscovery.kt` | added | Candidate merge, independent phrase corroboration and ambiguous-candidate rejection. |
| `app/src/main/java/com/example/papereyes/domain/discovery/PaperDiscoveryProvider.kt` | added | Provider interface, evidence/candidate models and explicit capabilities. |
| `app/src/main/java/com/example/papereyes/domain/discovery/PhraseQuery.kt` | added | Sanitized, bounded quoted phrase query construction. |
| `app/src/main/java/com/example/papereyes/domain/evidence/DocumentEvidence.kt` | added | Immutable local identifiers/journal/title/fingerprint evidence and layout role model. |
| `app/src/main/java/com/example/papereyes/domain/evidence/FingerprintExtractor.kt` | added | Bounded deterministic distinctive-phrase extraction with independent source groups. |
| `app/src/main/java/com/example/papereyes/domain/telemetry/ScanTrace.kt` | added | Bounded numeric-only stage events and duration summaries. |
| `app/src/main/java/com/example/papereyes/ocr/DocumentLayoutAnalyzer.kt` | added | Shared pure line/geometry role classification and multiline/column-safe title reconstruction. |
| `app/src/main/java/com/example/papereyes/ocr/ImageSampling.kt` | modified | Overflow-safe ceiling-based power-of-two sampling without arbitrary 128 cap. |
| `app/src/main/java/com/example/papereyes/ocr/TextCandidateExtractor.kt` | modified | Small ML Kit line adapter and compatibility API over the shared analyzer. |
| `app/src/main/java/com/example/papereyes/ocr/TextRecognizerService.kt` | modified | Shared evidence results; bitmap/native task ownership; deferred close; stage timing. |
| `app/src/main/java/com/example/papereyes/ui/live/LiveScanScreen.kt` | modified | Shared evidence lookup, safe capture cancellation/completion, ambiguity-aware title matching and result timing; still-capture path retained. |
| `app/src/main/java/com/example/papereyes/ui/live/LiveScanUi.kt` | modified | Truthful on-device/selected-text privacy copy; heuristic title score rather than probability. |
| `app/src/main/java/com/example/papereyes/ui/scan/ScanScreen.kt` | modified | Import uses structured evidence; possible/uncertain/unavailable result copy. |
| `app/src/main/java/com/example/papereyes/util/concurrency/CompletionGate.kt` | added | Defers resource close until all acquired native tasks complete. |
| `app/src/main/java/com/example/papereyes/util/concurrency/RequestGate.kt` | added | Cancelable single-connection pacing and in-process server cooldown. |
| `app/src/main/java/com/example/papereyes/util/concurrency/SuspendQueryCache.kt` | added | Bounded TTL/load deduplication without orphaned coroutine work. |
| `app/src/main/keepRules/openalex.keep` | added | Keeps reflected OpenAlex DTOs for release shrinking; real R8 build remains unverified. |
| `app/src/test/java/com/example/papereyes/data/local/PaperDaoIdentityTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/data/model/ScholarlyIdentifiersTest.kt` | modified | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/data/remote/CitationSafetyRegressionTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/data/remote/InspireEvidenceTest.kt` | modified | Strengthens two unsafe positive expectations; includes fully corroborated positive identity. |
| `app/src/test/java/com/example/papereyes/data/remote/OpenAlexProviderTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/data/remote/PaperRepositoryNetworkTest.kt` | modified | Retains actual network tests with isolated policy/test clocks; requires genuine Gradle dependencies. |
| `app/src/test/java/com/example/papereyes/domain/ArxivFeedParserTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/PaperResolverEvidenceTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/discovery/AbstractReconstructionTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/discovery/ArxivEntryIdentityTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/discovery/InteriorPageDiscoveryTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/discovery/PhraseQueryTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/discovery/RetryAfterTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/evidence/FingerprintExtractorTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/domain/telemetry/ScanTraceTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/ocr/AuditRegressionTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/ocr/DocumentLayoutAnalyzerTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/ocr/ImageSamplingTest.kt` | modified | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/util/concurrency/CompletionGateTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/util/concurrency/RequestGateTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `app/src/test/java/com/example/papereyes/util/concurrency/SuspendQueryCacheTest.kt` | added | Deterministic regression coverage for the corresponding production component; execution scope is listed in the audit report. |
| `docs/AUDIT_REPORT.md` | added | A–V audit, implemented fixes, limitations, official research and twelve release answers. |
| `docs/BENCHMARK.md` | added | Real-corpus contract, replay limitations and actual-device timing instructions. |
| `docs/CHANGES.md` | added | Generated complete file-level change inventory. |
| `docs/FILE_INVENTORY.md` | added | Generated source/configuration scope and SHA-256 inventory. |
| `docs/WORKLOG.md` | added | Logical change groups and reproduced-before/fixed-after verification history. |
| `gradlew` | mode restored | Official supplied wrapper retained; executable permission restored. |
| `tools/benchmark_providers.py` | added | Provenance-bearing query-replay scaffolding, safe error categories/cooldowns, honest unmeasured metrics. |
| `tools/check_kotlin_syntax.py` | added | Parses all app Kotlin with compiler PSI; no Android symbol/API resolution. |
| `tools/check_release_invariants.py` | mode restored | Content retained; executable permission restored. |
| `tools/offline/CoreMicrobenchmark.kt` | added | Warmed synthetic text-only CPU diagnostic; excludes OCR/camera/network. |
| `tools/offline/Runner.kt` | added | Reflection invocation of selected source @Test methods with explicit pass/fail exit status. |
| `tools/offline/SyntaxCheck.kt` | added | Kotlin compiler PSI traversal for syntax errors. |
| `tools/offline/stubs/AndroidXml.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/GsonFactory.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/Junit.kt` | added | Minimal assertion/annotation shim; explicitly not real JUnit, rejects unexpected NaN. |
| `tools/offline/stubs/MlKitText.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/NetworkClients.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/Rect.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/ResponseBody.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/Retrofit.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/RetrofitAnnotations.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/Room.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/SerializedName.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/offline/stubs/XmlPull.kt` | added | Compile-only external type shape for portable tests; never used in the Android app or as a live integration substitute. |
| `tools/test_benchmark_tools.py` | added | Nine synthetic self-tests of replay accounting, IDs, encoding, manifest/error output; not provider accuracy. |
| `tools/verify.sh` | mode restored | Content and required Gradle gates retained; executable permission restored. |
| `tools/verify_portable.py` | added | Explicit actual-source/test allowlist and portable Kotlin runner; does not replace Android verification. |
| `tools/verify_schema.py` | added | Seven real SQLite tests against exported Room v4 DDL; not Room runtime. |
