# Implementation work log

## Supplied baseline
The ZIP has no commit metadata and lacks the described DocumentLayoutAnalyzer.
Live Scan uses low-resolution text presence -> still JPEG OCR -> lookup. The
provided commit 83ff1ed cannot be authenticated from this archive. No files from
prior chats or public repositories were substituted.

Baseline verify failed Unix execute-bit invariants. All explicit Gradle tasks
failed bootstrap DNS resolution; no Android SDK/device/emulator is installed.
A separate portable assertion runner compiled actual core Kotlin with clearly
labelled compile-only external-type stubs, then ran the original pure test
methods and added regressions: 23 passed, 6 failed. The six failures reproduce
body/section-as-title, damaged metadata, spaced arXiv versions, sample rounding,
and the sample=128 cap. The numbered-reference fixture already passed and is
not claimed as a newly found bug. See verification/baseline_portable.log.

## 1. Identifier and image-bounds fixes
Added regression assertions before production changes. Canonicalization accepts
horizontal spacing around arXiv versions, rejects malformed suffixes, and adds
work-level IDs without global character substitutions. Sampling now uses ceiling
division and Long arithmetic, not an arbitrary 128 cap. Restored executable modes.
Focused portable checks: 9 model tests and 4 sampling tests pass. Static release
invariants pass with the original application-ID/contact warnings.

## 2. Shared evidence/layout core
Replaced duplicated block-height/plain-text title heuristics with one pure,
line-based analyzer and a small ML Kit adapter. Preserves separate DOI/arXiv,
journal and title evidence; explicitly avoids body/section/reference titles,
groups aligned multiline titles without crossing columns, and retains a title-
crop path. Added deterministic bounded fingerprint selection, deferred off clean
identifier/title paths. Caption-prefix sentence splitting initially failed its
new regression test and was fixed; no assertion was weakened. All 49 portable
assertion methods pass. These are synthetic text/geometry fixtures, NOT OCR or
camera accuracy measurements. No Android build has passed in this environment.

## 3. Provider-independent discovery and request ownership
Added capability-aware discovery, bounded phrase construction and abstract
reconstruction, independent-evidence verification, and the anonymous OpenAlex
adapter. Duplicate/overlapping phrases and competing verified candidates remain
uncertain. Abstract gaps/conflicting positions cannot be joined into invented
text. Added cancel-safe bounded TTL caching, deferred native-task completion
ownership, conservative single-connection pacing, and HTTP cooldown parsing.
Focused fake-provider/pure-core tests pass. These do not execute live HTTP.

## 4. Citation identity safety
Reproduced four unsafe Crossref cases before fixing them: missing/wrong article
number, numeric prefix collision, and contradictory volume. Retained positive
exact locator/page-range cases. Later final review reproduced two more failures:
a missing volume could accept a repeated article number, and token matching
could confuse Physical Review C with D. The final rule requires journal + volume
+ locator, rejects supplied contradictions, and prevents fuzzy equality of known
different journals. Positive short-alias matching remains tested.

Two supplied INSPIRE assertions were deliberately strengthened, not weakened:
journal/volume/year without the requested article is now rejected, and
journal+locator without volume is rejected because article numbers repeat across
volumes. A fully corroborated positive case is retained/added. The original
MockWebServer tests remain; their clocks/policy instances were isolated for
predictable pacing, but the actual Gradle suite cannot run here.

## 5. Resolver and official arXiv parsing
Exact DOI responses must match the requested DOI. Semantic Scholar metadata must
supply the requested arXiv work ID; official Atom is the fallback. Entry-scoped
Atom parsing rejects API-error entries, unrelated IDs, and explicit version
mismatches without leaking titles/authors between entries. The actual parser
state machine is tested through a JDK-StAX XmlPullParser adapter, not Android's
parser runtime. Resolver fake-API tests exercise equivalent-query caching,
body-text routing, unavailable providers, and cancellation boundaries.

## 6. OCR, Import, Live Scan and lifecycle integration
Shared DocumentEvidence now flows from OCR into both image routes. Native ML Kit
input ownership ends at task completion, not coroutine cancellation. Capture
callbacks use a lifecycle-surviving main executor and clean cancelled/late files.
Preview ImageProxy closure and recognizer close use deferred-completion gates.
Live matching uses the existing ambiguity-aware title matcher; verified journal
identity no longer needs to resemble a title. No artificial 100% confidence is
assigned to identifiers/journal matches. Added bounded numeric-only debug stage
timing and clearer candidate/privacy text. Existing still-capture resolution,
EXIF transforms and import sampling target are retained. Actual camera/import
regression tests remain unexecuted, not presumed successful.

## 7. Database correctness
An actual SQLite baseline query returned zero candidates for equivalent titles
with repeated internal whitespace, preventing title-only → DOI upgrade. Using
one Kotlin canonicalizer for the fallback candidate comparison fixes whitespace
and Unicode-case behavior; indexed identity lookup remains the fast path.
Four portable actual-DAO-logic tests pass. Seven exported-schema SQLite tests
pass. Added two real Room instrumentation cases for rapid concurrent duplicate
save and membership-preserving upgrade; those require Android and were not run.
Schema version 4 and its DDL are unchanged; the stale version-1 HANDOFF was fixed.

## 8. Final harness and packaging preparation
Added real-corpus query-replay scaffolding and explicitly separate metric/error
categories. Provider DNS preflight failed; no real 50-paper benchmark or winner
was fabricated. The final harness review reproduced and fixed failure to write
DNS diagnostics into a new nested output directory. Nine harness self-tests now
pass. The portable floating-point assertion shim was also tightened to reject
unexpected NaN rather than accidentally treating it as within tolerance.

Final source checks: 100 portable assertion methods passed, 88 Kotlin files
parsed without syntax errors, 7 SQLite schema tests passed, 9 benchmark harness
tests passed, release invariants passed with two existing warnings, and tracked
source whitespace checks passed. Every requested Gradle task was explicitly
attempted again, including clean, and stopped at bootstrap DNS failure. There
is no SDK or device. Logs classify these as NOT RUNNABLE, never Android passes.

The archive excludes Git/build/IDE/cache state, carries a per-file content
manifest and executable scripts, and is round-trip checked from a clean extract.
The actual archive hash and post-archive verification results are provided as
separate handoff artifacts because they cannot be embedded self-referentially.
