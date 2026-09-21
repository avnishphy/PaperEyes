# PaperEyes — engineering audit, implementation, and verification

**Audit date:** September 20, 2026, America/New_York. UTC logs may show September 21.

**Release decision:** **Do not publish this as a public beta yet.** The updated source has passing portable checks and documented improvements, but Android compilation, Android lint, minified release behavior, Room instrumentation, real OCR/camera accuracy, and live provider performance are not established by this environment.

## A. Executive summary

This handoff contains implemented source changes, regression tests, verification tools, execution logs, and a complete project archive. It is not only a review. The strongest improvements are prevention of false identification, shared structured document evidence, conservative interior-page candidate verification, resource ownership during cancellation, and consistent database identity normalization.

The sole implementation baseline was the uploaded `PaperEyes-main(3).zip`, SHA-256 `53aed788dd4329ea6e66c2e6e827d5122232658f8e104e86d39adf07a5bc793e`. No prior-chat implementation or public repository was substituted. The supplied archive contains 42 production Kotlin files, nine JVM test classes, and three instrumentation classes. The final tree contains 59 production Kotlin files, 26 JVM test classes, and three instrumentation classes.

**Important baseline discrepancy:** the archive does not contain `DocumentLayoutAnalyzer` or `DocumentEvidence`, and its Live Scan normal path is preview OCR triggering still capture. It therefore does not substantiate the prompt's description of an already layout-aware, direct preview-frame lookup baseline. It also contains no Git history authenticating commit `83ff1ed`. The new work is relative to the actual ZIP, not a presumed newer project.

No mandatory paid service, embedded API credential, cloud OCR, paid proxy, or citation-manager ecosystem was introduced. Identifying what the user can see remains the product focus. The new interior-page implementation is a foundation, not a demonstrated solution for arbitrary pages.

## B. Baseline verification

Before production changes, the normal verification script failed a source invariant because Unix executable permissions were missing. Gradle bootstrap also failed DNS resolution for `services.gradle.org`; the distribution was not cached. There was no Android SDK, `adb`, connected device, or emulator. Explicit baseline attempts at unit tests, lint, debug/release assembly and instrumentation are retained in `verification/baseline_*.log`.

A separate portable runner compiled actual selected Kotlin production files and source test methods, with explicit external-type compatibility stubs. Baseline testing plus added reproductions yielded **23 passes and six failures**: body prose selected as a title, section headings selected as titles, damaged arXiv metadata leaking into a title query, spaced arXiv versions being lost, and two image-sampling bounds errors. These are genuine local assertion failures; they are not claims that the unexecuted Android Gradle suite failed.

The original numbered-reference fixture already passed. It was retained as a regression, not relabeled as a newly discovered defect. Further citation and SQLite reproductions were recorded before their respective fixes. Historical failing logs remain in the archive intentionally.

## C. Architecture map and source review

The source flow is:

```text
Compose screens / MainActivity
  ├─ Import → bounded bitmap decode + EXIF → ML Kit
  └─ Live Scan → CameraX preview text trigger → still JPEG → ML Kit
                         ↓
             TextCandidateExtractor adapter
                         ↓
       DocumentLayoutAnalyzer → DocumentEvidence
          identifiers / journal / title / fingerprints
                         ↓
                     PaperResolver
  DOI → Crossref direct metadata
  arXiv → Semantic Scholar work identity → official arXiv fallback
  journal → verified Crossref candidates → INSPIRE fallback
  title → Crossref title candidates
  interior → PaperDiscoveryProvider → OpenAlex → local corroboration
                         ↓
          result/candidate/uncertain/unavailable state
                         ↓
               LibraryRepository → Room v4
                  Paper / Project / membership
```

All supplied production Kotlin, tests, Gradle/catalog/wrapper configuration, manifest, XML resources, exported schemas, keep rules and verification scripts were inspected. `FILE_INVENTORY.md` enumerates source/configuration scope; launcher artwork is inventoried, not claimed to have undergone a visual design or accessibility study. New Kotlin is additionally syntax-parsed across the complete application/test tree.

Provider-specific details remain outside the UI. The new discovery interface has explicit capabilities; only the OpenAlex interior adapter implements it today. Existing exact/journal/title providers retain their repository boundaries rather than undergoing an unnecessary whole-app framework rewrite. Constructor injection supports fake providers and deterministic policy clocks. A limitation remains: `bestQuery` selects a preferred structured route rather than performing a fully general combination of every author/year/title/journal signal.

## D. Verified bugs and demonstrated risks

| Finding | Evidence | Implemented correction |
|---|---|---|
| Body/section text and damaged arXiv lines could become paper titles | Baseline portable failing fixtures | Role-aware suppression and shared evidence analysis |
| OCR spaces around arXiv versions lost version information | Baseline identifier regression | Strict version grammar with bounded horizontal spacing |
| Sampling rounded down or stopped at 128 | Baseline 4801-pixel and very-large-image tests | Ceiling division, Long arithmetic, larger powers of two |
| Journal/volume/year could pass without the requested article | Citation reproductions | Require journal + volume + article/page locator |
| Wrong locator, contradictory volume, or longer numeric prefix could pass | Citation reproductions | Reject contradictions and non-boundary numeric prefixes |
| Known Physical Review series C and D could fuzzy-match | Final failing regression | Different known journal aliases cannot fall through to fuzzy equality |
| Missing volume could verify repeated article numbers | Final failing regression | Require volume for automatic citation identity |
| SQL title filtering missed canonical whitespace variants | Actual baseline SQLite query returned zero rows | Kotlin canonical-title fallback consistent with identity generation |
| New benchmark tool could fail writing blocked-DNS diagnostics into a new directory | Harness failing regression | Create output parent before the early failure branch |

Additional changes address risks demonstrated by source/control-flow inspection and deterministic ownership tests: releasing image inputs on coroutine cancellation while native OCR is still running, closing recognizers before pending work completes, late capture-file callbacks after navigation, accepting mismatched identifier responses, and mixing fields across Atom feed entries. These are not represented as reproduced physical-device crashes.

Two original INSPIRE expectations were strengthened: journal/volume/year without the requested locator no longer identifies a paper, and journal+locator without volume no longer suffices. A fully corroborated positive case remains. No assertion was disabled to produce a passing result.

## E. Changes implemented

Identifier handling now preserves valid modern and legacy arXiv syntax, including OCR spaces around version markers, without global `l→1` or `O→0` replacement. Malformed metadata is rejected as a title candidate. DOI canonicalization and persistence identity remain centralized; punctuation/idempotence cases were expanded. The syntactic fixture `2609.20448` is not a claim that a live paper with that ID was retrieved.

A pure layout/evidence layer now serves Import and Live Scan. It uses line geometry, relative height, alignment, location and lexical exclusions; reconstructs aligned multiline titles; protects column boundaries; and preserves small structured journal evidence. Deterministic, bounded fingerprint extraction runs when strong structured/title evidence is absent. No cloud language model is needed.

The resolver now distinguishes verified identity, candidates, uncertainty, not-found and unavailable-provider states. Exact lookup responses are checked against requested identifiers. Official arXiv parsing is entry-scoped and version-aware. Search rank does not by itself establish identity.

Bounded short-lived caching, request pacing, server cooldown handling, native completion gates, capture cancellation cleanup and numeric timing instrumentation were added. Canonical-title upgrades preserve saved-paper IDs and project membership. The schema is unchanged; this is not a database reset.

## F. Live Scan results

**Actual device latency and accuracy: not measured.** Neither median <2 seconds nor P90 <3 seconds can be claimed. CameraX and ML Kit could not be run in this container.

The original `KEEP_ONLY_LATEST` analysis strategy, 1280×720 preview target, recognizer reuse, high-resolution still OCR, one initial capture and adaptive two-frame fallback remain. Preview OCR still acts as a trigger, not the final evidence/lookup path. Replacing that pipeline without device regression evidence would create an unmeasured risk, so it was not done.

The integration now accepts identifier-only preview text without an arbitrary letter-count obstacle, uses shared structured evidence, avoids treating a verified journal citation as a fuzzy title, applies the existing ambiguity-aware title matcher, and does not assign fake 100% certainty to exact/journal results. Interior candidates require local corroboration; otherwise Live Scan requests a better view rather than auto-locking on a search hit.

Debug timing records analyzed-frame callback, preview OCR, capture, high-resolution OCR, layout, lookup and next Compose result frame. Lookup duration includes pacing/cache/provider waits, not just network transport. Timing logs contain no OCR/query content. Successful-display logs alone are insufficient for a failure-rate benchmark; an external attempt ledger is required. See `BENCHMARK.md` for the real-device procedure.

## G. Import results

Import now passes `DocumentEvidence` directly into the same resolver as Live Scan. Bitmap ownership is retained until native OCR completion even if the waiting coroutine is cancelled. Existing EXIF transforms, content-URI handling, on-device OCR and the 2400-pixel sampling target are preserved; sample computation itself is corrected.

The following require actual Android testing and **were not executed**: PNG, JPEG, screenshots, large/small photos, portrait/landscape, all EXIF orientations, arXiv/journal/body/reference-page images, cancelled picker, broken URI and navigation during decode/OCR/lookup. Source review and synthetic geometry are not substitutes for this matrix. A possible single search result is labeled as requiring confirmation unless the resolver has verified identity.

## H. Layout and OCR regression results

Synthetic text/geometry fixtures pass for spaced and damaged arXiv metadata, multiline titles, the PRD header/title case, body-only text, section headings, references, conference banners and unrelated columns. DOI tests cover prefixes, DOI URLs, balanced/unbalanced parentheses, terminal punctuation and stable canonicalization. Identifier tests include modern, legacy and explicit-version forms.

Bounding-box height is treated as relative font-size evidence, not actual font family. Same-size paragraph-like text is not promoted to a title simply because it fills the image. Geometry that contradicts a title is not bypassed by a permissive prose fallback. Journal evidence is not discarded merely because it is smaller than the title.

Remaining limitations include skewed or complex layouts, multi-script/non-Latin material, unusual conference banners, incomplete journals, figures without useful text and formula-heavy pages. The title/layout rules are deterministic heuristics with fixture coverage, not a trained or calibrated document classifier. Actual OCR word error rate and image-level identification recall are unknown.

## I. Interior-page research and implementation

The implemented cascade extracts up to five bounded distinctive phrases, generally 6–15 words, with lexical-specificity, stop-word, equation/numeric contamination and caption heuristics. The rarity signal is a deterministic proxy, not corpus-derived inverse document frequency. Repeated windows from the same sentence and highly overlapping phrases do not count as independent evidence.

`PaperDiscoveryProvider` models capabilities and returns candidate metadata plus text eligible for verification. The OpenAlex adapter sends at most three selected quoted phrases in one bounded OR query; it does not transmit the entire page. Candidates are combined by canonical identity. Verification requires at least two independent local phrase matches and rejects an ambiguous situation in which another candidate has sufficient support.

**Crucial limitation:** the current OpenAlex adapter receives abstract text via its inverted index, not the matching full-text body/caption snippets. Indexed full text can retrieve a candidate, but a body-only hit with no corroborating returned abstract remains uncertain. Abstract reconstruction preserves gaps/conflicts rather than inventing continuous text. Exact normalized phrase matching is intentionally conservative; OCR corruption can reduce verification coverage.

Consequently, PaperEyes can now attempt interior-page candidate retrieval and exercise a safe verifier, but it has not demonstrated reliable arbitrary “Page 7” identification. Cross-paper boilerplate, cited text, incomplete metadata, preprint/version duplication and inaccessible text remain important real-corpus tests. Author/year evidence fusion, alternative full-text providers, true snippet retrieval and figure matching are not implemented.

## J. Provider/API benchmark

The requested ≥50-paper, five-view, four-corruption live benchmark was **not completed**. The supplied files contain no such real corpus, and container DNS preflight failed for all three benchmark hosts. Top-1, Top-5, false-confident-match rate, provider latency and live API error rate are therefore **unmeasured**, not zero.

A reusable query-replay harness was implemented and its nine self-tests pass. It validates corpus structure/provenance fields, records transport/contract/rate-limit failures separately from valid not-found responses, computes requested rank/latency metrics, and deliberately leaves false-confident-match rate null unless actual verification decisions are measured. It does not generate OCR/crops or benchmark the complete Android resolver cascade. No empirical best provider can be named.

## K. Free/open service architecture and current official research

External research below is separate from repository inspection and local execution. Sources were consulted on September 20, 2026. Documentation availability is not proof of runtime availability.

| Service | Role in this tree | Current verified information / boundary |
|---|---|---|
| ML Kit | On-device OCR retained | Official product overview describes on-device mobile processing [S1]. No metered OCR service is configured or added. |
| Crossref | DOI, journal and title metadata | Public REST access needs no registration/key; polite access uses a real email [S2]. July 21, 2026 limits distinguish public singleton 5/s from public list 1/s [S3]. |
| arXiv | Official exact-identifier fallback | Official API terms allow free use and require legacy API calls no more than once per three seconds, single connection. Metadata and article-content rights differ [S4]. |
| OpenAlex | New experimental interior discovery | Basic anonymous queries are supported; a free key increases budget, but access is not unlimited [S5]. Search covers titles, abstracts and indexed full text, with quoted phrases/Boolean syntax [S6]. |
| Semantic Scholar | Existing arXiv metadata attempt retained | Dynamic official Graph documentation was not fully extractable [S9]. Anonymous availability, current snippet entitlement and redistribution terms were not conclusively verified; official arXiv remains the fallback. |
| INSPIRE | Existing journal fallback retained | No new broad search/full-text contract is claimed. Current detailed documentation access was incomplete; production terms/contract revalidation remains open. |
| CORE / Europe PMC / OpenAIRE | Investigated as possible alternatives, not integrated | Access/coverage/licensing information was incomplete or insufficient to validate arbitrary-page search and a no-payment production contract. No ranking or blanket commercial/free-access claim is made. |

OpenAlex's current pricing documentation gives a free account a $1 daily allowance without payment details [S7]. That is not the anonymous app's allowance, and the app has no account/key or prepaid usage feature. Its metadata licensing must not be treated as permission to redistribute arbitrary paper PDFs. Its work dictionary describes abstract indexes and separately metered authenticated content downloads [S8]; the new adapter does not download paper content.

The chosen strategy is a **conservative capability-based implementation**, not a benchmark winner: keep exact and journal routes, avoid unrelated provider fan-out, and use OpenAlex only for the new phrase-discovery mode. There is no mandatory user subscription, developer secret inside the APK, billing integration or paid fallback. Provider quota exhaustion can still prevent identification. Full service-contract and redistribution clearance is unfinished for providers whose official material was inaccessible; public release should not assume that gap is resolved.

## L. Networking

One shared policy serializes requests per provider and paces completion-to-next-start. It uses 3 seconds for arXiv and a conservative 1.05 seconds elsewhere; first requests are immediate. Crossref public concurrency is kept to one. Long server cooldowns fail promptly rather than leaving a UI coroutine waiting for hours. `Retry-After` seconds/date forms and OpenAlex exhausted-budget reset hints are handled. Existing bounded transient Crossref retry behavior remains; no perpetual/background retry loop or quota evasion was introduced.

The resolver cache is bounded, short-lived and cancellation-aware. Successful candidates expire after 30 seconds, empty results after five seconds, and provider failures are not cached as not-found. Equivalent identifiers share keys; arXiv versions remain distinct where needed. A single cache mutex also serializes unrelated loads, acceptable at current app scale but a potential future latency improvement.

Existing HTTPS endpoints, common OkHttp client and timeouts remain. A 20-second call timeout plus sequential fallback can dominate tails; the new code does not guarantee a 2-second identification. The old Crossref article-number filter was not removed on an unverified assumption that it is invalid. Actual Retrofit encoding, HTTP cancellation, Gson response handling and remote contracts require the retained Gradle/MockWebServer tests and live smoke tests.

## M. Database / Room

Room remains **version 4**, with the supplied exported schema and no destructive migration fallback. The old HANDOFF statement that the schema was version 1 was corrected. No hypothetical historical production migration chain was added.

The title-only→DOI upgrade path previously used a SQL `LOWER(TRIM(...))` filter that could eliminate candidates before Kotlin normalization collapsed internal whitespace. The fallback now compares the same canonical title function used for identity. Existing indexed identity lookup runs first; the fallback reads candidate papers once and normalizes them in Kotlin. This trades an O(n) fallback scan for correctness in a pre-release library. Large-library profiling or a future indexed normalized-title column is a separate, justified schema change—not hidden here.

Four portable tests execute actual DAO decision logic with fake storage, covering whitespace/Unicode-case upgrades, stable IDs, different years/DOIs and ambiguous candidates. Seven tests execute actual exported DDL in SQLite and verify uniqueness, foreign keys, membership/cascades and rollback. Two new Android Room tests cover concurrent duplicate save and membership-preserving upgrade; those are present but unrun. Kotlin DAO tests and Python SQLite do not validate Room-generated code or Android transaction scheduling.

## N. Compose / lifecycle

Native-task completion gates defer recognizer closure and input recycling until outstanding work completes. Preview `ImageProxy` closure covers null images and synchronous failures. Capture callbacks use the main executor rather than the analysis executor that navigation may shut down. Late/cancelled captures delete owned temporary files, and coroutine cancellation is rethrown rather than converted into a user error.

Pure ownership/cache/pacing tests pass, including repeated concurrent completion handling. Actual navigation during OCR, network, capture or database save, permission changes, camera binding/disposal, process death and rotation are not device-tested. Normal and cancellation cleanup do not guarantee deletion after abrupt process termination; a startup stale-cache janitor remains worthwhile. Screen-state architecture was not broadly rewritten into a new framework solely for stylistic consistency.

## O. Performance

| Area | Classification / result |
|---|---|
| Normal-path still JPEG capture, high-resolution OCR, network waits | Major potential latency contributors; device measurement required |
| Repeated equivalent lookup | Moderate; bounded TTL deduplication implemented |
| Bitmap sampling overflow/rounding | Moderate memory/correctness issue; fixed and tested |
| Canonical-title fallback scan | Minor for small libraries; potentially moderate at scale |
| Small retained regex construction / UI formatting | Minor or negligible relative to camera/network; not grounds for broad rewrites |

A warmed, synthetic text-only JVM microbenchmark actually ran 5,000 evaluations: median **0.076896 ms**, P90 **0.277137 ms**, P95 **0.339731 ms**. It measures the text analyzer only, not Android, ML Kit, image decoding, camera, network or end-to-end performance. No Live Scan KPI is inferred from it. Logging bounds, evidence limits, response candidate caps and short-lived caches reduce unbounded retention risks.

## P. Security / privacy

The manifest's disabled cleartext/backup settings, camera/network permissions, Photo Picker instead of broad storage access, and external-link scheme/host checks were preserved. Static credential-pattern checks found no matching embedded credential. No secret key was added. Raw OCR debug UI remains gated; timing logs contain numeric stages only.

Images are processed locally, but identification is not wholly offline: selected identifiers, titles, bibliographic strings or phrases go to scholarly providers. The UI now communicates that distinction. A confidential/unpublished page can still reveal information through a selected phrase; a future explicit offline/network-consent control would be valuable. This implementation does not claim that every third-party SDK performs zero ancillary network activity.

Resolved dependency vulnerability scanning, minified-release inspection, actual exported-component behavior, filesystem/process-death cleanup and store privacy disclosure verification could not be completed without the Android dependency/runtime environment. Static review is not a clean security bill. Existing legitimate URL/XML namespace strings are not treated as evidence of cleartext networking merely because they contain `http://`.

## Q. Tests added and scope

The final portable run executes **100 assertion methods** from actual selected Kotlin source tests, using Kotlin 1.9.0, its bundled coroutine library and explicit external type stubs. Coverage includes identifiers, title identity, sampling, layout roles, multiline/column boundaries, fingerprints, phrase query encoding, abstract reconstruction, independent candidate verification, ambiguity, DOI/arXiv response identity, Atom parsing, citation contradictions, caching, rate policy primitives, native completion ownership, timing and DAO identity decisions.

The stubs are not the Android SDK, real JUnit, Room, Retrofit, Gson or ML Kit. Network entry points in them do not emulate a successful service. The actual Atom state machine is driven through JDK StAX for deterministic parser tests, not Android's XML implementation. Original network/encoding tests remain for real Gradle execution. The portable assertion shim explicitly rejects unexpected NaN rather than silently passing a tolerance comparison.

Separate checks comprise 88-file Kotlin PSI syntax parsing, seven SQLite-schema tests, nine benchmark-tool tests, release/source invariants and whitespace checks. No lint rule, Android test or release optimization was disabled. Added real instrumentation cases are not counted among executed tests.

## R. Full test/build matrix

| Check | Baseline | Final working tree |
|---|---|---|
| `./tools/verify.sh` | Execute-bit invariant failure | Source invariants pass; stops at Gradle DNS |
| `./gradlew clean` | Not required as initial baseline command | Attempted; Gradle bootstrap DNS failure |
| `./gradlew testDebugUnitTest` | Not runnable: DNS | Not runnable: DNS |
| `./gradlew lintDebug` | Not runnable: DNS | Not runnable: DNS; zero lint errors NOT established |
| `./gradlew assembleDebug` | Not runnable: DNS | Not runnable: DNS |
| `./gradlew assembleRelease` | Not runnable: DNS | Not runnable: DNS |
| `./gradlew connectedDebugAndroidTest` | Not runnable: DNS/no SDK/device | Not runnable: DNS/no SDK/device |
| Portable selected Kotlin assertions | 23 pass / 6 reproduced failures | 100 pass / 0 fail |
| Kotlin PSI parse | Not used as baseline pass | 88 files / 0 syntax errors |
| Exported-schema SQLite | Baseline whitespace bug separately reproduced | 7 pass |
| Benchmark harness self-tests | New tool | 9 pass |
| Release invariant checker | Unix wrapper permission failure | Pass; application-ID and Crossref-contact warnings |
| Source whitespace check | No project defect claimed | Pass |
| Live provider benchmark | No corpus/run | Not run; blocked DNS and missing real corpus |
| Actual Import/Live Scan image/device matrix | Not runnable | Not runnable |

Raw command lines, exit codes and environment information are in `verification/final_android_tasks.json` and corresponding logs. Final archive round-trip results are delivered separately after packaging; they do not alter these Android limitations.

## S. Remaining limitations

The broad task is only partially verified: no successful Android build/lint/device run, no real-image regression corpus, no ≥50-paper live comparison, and no measured scan latency. The new interior verifier lacks returned full-text snippets and broader provider coverage. Identifier-free author/year fusion, robust figure recognition, calibration against real hard negatives and complete provider licensing/contract validation remain unfinished.

The new layout, cancellation and Compose integration must be tested on actual devices even though their pure models pass. Missing volume/locator now deliberately reduces citation auto-identification rather than accepting weak evidence. Legacy private installs with incompatible schemas may still require an explicit migration or deliberate development reset; no automatic production wipe was enabled.

## T. Release readiness — answers to the twelve questions

**1. Would I ship the first public beta?** No. This is a source handoff with tested core logic, not a validated Android release.

**2. What stops release?** Successful genuine Gradle test/lint/debug/minified-release builds, real Room/device integration tests, Import/Live Scan regressions, basic live provider contract checks, remaining terms/privacy decisions, and final package/signing/application-ID decisions.

**3. Does clean Live Scan achieve median <2 s / P90 <3 s?** Unknown. The current still-capture path and provider waits can exceed those goals. Instrumentation was added; an actual measurement was not possible.

**4. Does Import reliably identify DOI/arXiv/journal/title?** These evidence routes and their core regressions are implemented and pass portable tests. Real image-level reliability is unmeasured, so a blanket yes would be unsupported.

**5. Can it make progress on arbitrary interior pages?** Yes as an implemented candidate-retrieval/verification foundation; no demonstrated broad recognition claim. Body-only matches without corroborating returned text stay uncertain.

**6. Which strategy performed best?** No measured winner. The implemented evidence-aware strategy is a maintainable starting point, not a conclusion from a nonexistent benchmark.

**7. Can normal users use it without paying?** No payment requirement is built into these changes. Free provider access is quota/availability dependent; unlimited anonymous identification is not guaranteed.

**8. Are paid services required?** No paid service or paid backend is required by the implemented paths.

**9. Are API keys required?** No key is embedded, requested or configured by the current paths. Current external service behavior still needs live validation; no promise of permanent anonymous access is made.

**10. What happens when providers are unavailable/rate-limited?** Cooldowns and bounded caching avoid repeated identical requests; arXiv and journal routes have relevant fallbacks. Failures remain unavailable/uncertain rather than being reported as a confirmed identity or cached as a permanent not-found. The app does not bypass quotas or silently bill the user.

**11. Five highest-value remaining improvements:** (a) Android CI and real-device release/instrumentation gates; (b) a licensed ≥50-paper image/evidence corpus with hard negatives and honest confidence metrics; (c) a device-measured direct preview-frame evidence path; (d) lawful free full-text/snippet verification and a second independent discovery provider; (e) confidential-document/network controls plus lifecycle/process-death and accessibility validation.

**12. What should not be changed unnecessarily?** Keep centralized DOI identity, Room uniqueness/transactions/cascades, on-device OCR, Photo Picker, EXIF handling, bounded import decoding, reused recognizers, `KEEP_ONLY_LATEST`, secure external links and evidence-specific lookup routes. Do not exchange those for a heavyweight cloud or citation-manager rewrite.

## U. File-by-file changes

See `CHANGES.md` for every changed/new non-log source, test, tool and document, with status against the supplied archive and a short rationale. `FILE_INVENTORY.md` identifies retained source/configuration coverage. No production source file was dropped merely to make tests pass. The expanded `WORKLOG.md` records conceptual change groups and before/after reproductions.

## V. Final ZIP and integrity

The deliverable contains the complete source project, Gradle wrapper, tests, exported Room schemas, keep rules, verification tools and logs. It excludes `.git`, `.gradle`, build outputs, IDE caches, `local.properties`, Python bytecode and compiled portable-test jars. Unix executable bits are retained.

`PACKAGE_CONTENTS.sha256` records each packaged file except itself. After creating the archive, it is extracted into a clean temporary directory, its manifest/expected files/exclusions/modes are checked, and verification is rerun from that extraction. The **actual** post-archive results and archive SHA-256 are separate delivered artifacts, avoiding recursive modification of the ZIP being hashed. The final response is authoritative for the archive filename/hash and round-trip outcome.

## Official sources consulted

[S1] Google ML Kit overview: `https://developers.google.com/ml-kit`

[S2] Crossref access/authentication: `https://www.crossref.org/documentation/retrieve-metadata/rest-api/access-and-authentication/`

[S3] Crossref July 21, 2026 request-limit announcement (newer than the generic access table): `https://community.crossref.org/t/refining-rest-api-limits-for-improved-stability-and-reliability/16137`

[S4] arXiv API terms: `https://info.arxiv.org/help/api/tou.html`; API manual: `https://info.arxiv.org/help/api/user-manual.html`

[S5] OpenAlex authentication, updated August 19, 2026: `https://help.openalex.org/api/authentication/`

[S6] OpenAlex searching, updated September 19, 2026: `https://help.openalex.org/api/searching/`

[S7] OpenAlex pricing, updated August 11, 2026: `https://help.openalex.org/access/pricing/`

[S8] OpenAlex work attributes, updated September 18, 2026: `https://help.openalex.org/data/works/attributes/`

[S9] Semantic Scholar official Graph documentation (dynamic body not extractable during this audit): `https://api.semanticscholar.org/api-docs/graph`

Other attempted official discovery/terms entry points, not a completed capability or licensing clearance: `https://core.ac.uk/services/api`, `https://core.ac.uk/terms`, `https://europepmc.org/RestfulWebService`, `https://help.inspirehep.net/knowledge-base/inspire-rest-api/`, `https://graph.openaire.eu/docs/`.
