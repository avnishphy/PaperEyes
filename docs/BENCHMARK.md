# Benchmark contract and measurement status

## What was and was not measured

`verification/provider_preflight.json` records real DNS preflight failures for OpenAlex, Semantic Scholar, and Crossref from the execution container. This is **not** an API accuracy, availability, error-rate, or latency benchmark: no provider HTTP request was completed. Web documentation retrieval is a different network path and is not an Android/client performance measurement.

No real 50-paper image/crop corpus was supplied or assembled. No invented papers, fabricated OCR percentages, synthetic 100% accuracy, or provider winner is reported.

`tools/benchmark_providers.py` is an executable **query replay harness**, not a finished Android OCR or strategy benchmark. Nine deterministic self-tests cover its metrics, identity parsing, request encoding, invalid manifests, Retry-After parsing, error classification, and writing an output file after a blocked preflight. They do not establish real provider success.

## Required real corpus

Curate at least 50 real scholarly papers, predominantly physics/nuclear/particle with additional general science. For each, provide first page, title crop, journal header, identifier/title-free interior page, and figure/caption page. Record source, reuse basis, paper identity, image/crop coordinates, OCR engine/version, and evidence-extractor version. Use 0%, 5%, 10%, and 20% corruption conditions. The 0% condition still needs a clearly specified reference: clean transcription versus actual OCR are different experiments.

Define corruption before running: e.g. character substitutions/deletions/insertions, a fixed seed, actual changed-character count, and denominator excluding whitespace. Do not call typographic image degradation “10% OCR corruption” without measuring the resulting OCR. Keep corrupted queries paired across providers and retain the uncorrupted ground truth. The current harness **does not generate** images, OCR, or corruptions; those are corpus-preparation work.

The JSON manifest has a `papers` array. Every paper must carry `id`, `title`, `source_url`, `reuse_basis`, `domain`, `expected_ids`, and exactly 20 `cases`. Each case has `mode`, `corruption_percent`, `ocr_provenance`, and non-empty provider-specific `queries`. Interior cases additionally require `no_visible_identifier_or_title: true`.

Illustration of the shape only — these placeholders are **not a valid real corpus**:

```json
{
  "synthetic": false,
  "papers": [{
    "id": "curated-corpus-id",
    "title": "ACTUAL VERIFIED TITLE REQUIRED",
    "source_url": "ACTUAL LICENSED SOURCE REQUIRED",
    "reuse_basis": "ACTUAL PERMISSION OR LICENSE REQUIRED",
    "domain": "particle physics",
    "expected_ids": ["doi:ACTUAL_DOI_REQUIRED"],
    "cases": [{
      "mode": "interior_page",
      "corruption_percent": 0,
      "no_visible_identifier_or_title": true,
      "ocr_provenance": "Actual crop, OCR engine/version, seed, and extractor revision",
      "queries": {
        "openalex": "\"actual selected distinctive phrase\" OR \"second selected phrase\"",
        "semantic_scholar": "provider-appropriate query from the same evidence",
        "crossref": "provider-appropriate query from the same evidence"
      }
    }]
  }]
}
```

Supported modes are `first_page`, `title_crop`, `journal_header`, `interior_page`, `caption_page`; supported corruption percentages are 0, 5, 10, 20. Identifiers use canonical `doi:...`, `arxiv:...`, `openalex:w...`, or `s2:...`. Record preprint/version-of-record equivalence explicitly rather than granting arbitrary title matches. The manifest validator checks structure, not truth or licensing of a claimed paper.

## Running the harness

```bash
python3 tools/benchmark_providers.py --preflight --output results/preflight.json
python3 tools/benchmark_providers.py --manifest corpus.json --output results/validation.json
python3 tools/benchmark_providers.py --manifest corpus.json --run --output results/replay.json
```

No network replay occurs without `--run`. A blocked preflight returns exit code 2 and writes the diagnostics. The replay sends one request at a time, uses explicit pacing, limits responses, honors cooldowns, and records unattempted cooldown cases separately. It does not evade quotas or silently add credentials. Check current official API contracts and permissions before supplying a corpus, especially the Semantic Scholar search endpoint whose dynamic documentation was not fully extractable during this audit.

The replay requests top five results from OpenAlex search, Semantic Scholar relevance search, and Crossref bibliographic search. It deliberately does **not** imply all three support full-text evidence or identical query syntax. Choose comparable provider-appropriate queries and retain them as experiment inputs. The app itself uses different evidence-aware routes, including exact lookup and INSPIRE fallback; replay is not a benchmark of that complete cascade.

Metrics are grouped by provider/mode/corruption. They include planned/attempted/valid response counts, Top-1 and Top-5 over all planned cases, Top-1 over valid responses, request-latency median/P90/P95, API/transport/contract error rate over attempted requests, and not-found rate over valid responses. Percentiles use linear interpolation. Request latency includes response decoding/errors but excludes deliberate inter-request pacing, OCR, and camera work.

`false_confident_match_rate` is deliberately `null`: a search rank is not an app verification decision. To complete the interior-page benchmark, additionally replay each provider response through the actual `InteriorPageDiscovery` verifier and record verified/uncertain decisions and ground-truth agreement. Measure retrieval coverage and verification coverage separately. Include failures, unrelated papers sharing boilerplate, references pages, and competing candidates; report confidence intervals and per-domain results, not just a pooled mean.

## Actual Android Live Scan measurement

Debug builds emit numeric-only `PaperEyesTiming` JSON after the result reaches the next Compose frame. Events include preview-frame callback, preview OCR, still capture, high-resolution OCR, layout, lookup, and result display. No image, title, identifier, or OCR text is included in these timing logs.

```bash
adb logcat -c
adb logcat -s PaperEyesTiming:I > papereyes-timing.txt
```

Use real devices and the same clean/noisy targets under documented light, distance, network, focus and orientation. Separate first launch/warm runtime and cold/warm query cache. Measure at least a meaningful repeated sample per condition; preserve failures and timeouts, not just successes. The current logging records successful displayed identifications and is **not by itself** a complete failure-rate dataset: maintain an external attempt ledger.

`total_ms` runs from the analyzed preview callback to the next Compose frame containing the result. It is not sensor exposure time, physical display photon time, or time from entering the screen. `lookup_including_pacing_ms` includes cache/provider waits and is not pure wire latency. Repeated OCR/layout pairs are summed. Some stages overlap, so do not simply add all stage totals. Compare the measured successful-identification median and P90 against <2,000 ms and <3,000 ms, alongside success/false-identification rates. Profile the responsible stage before reducing resolution or relaxing matching.

The current architecture still performs still capture on the normal identification path. A preview-frame evidence/lookup path remains future device-validated work, not an achieved optimization.

## CPU diagnostic that actually ran

`python3 tools/verify_portable.py --microbenchmark` ran 5,000 evaluations of five fixed synthetic text fixtures after warm-up through actual `DocumentLayoutAnalyzer.fromText`. Recorded median 0.076896 ms, P90 0.277137 ms, P95 0.339731 ms on this container/JVM; quantiles here are indexed order statistics, not the replay harness's interpolation. This measures only warmed text-analysis CPU cost. It says **nothing** about ML Kit accuracy, image import, Android latency, camera performance, or live provider response time.
