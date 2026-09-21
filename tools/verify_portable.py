#!/usr/bin/env python3
"""Compile actual Kotlin core and run selected source tests without Android SDK.
Stubs cover annotations and external type shapes ONLY. This is not an Android
build, Room/ML Kit/Retrofit integration check, or a replacement for verify.sh.
"""
import argparse, os, pathlib, shutil, subprocess, tempfile, sys
ROOT = pathlib.Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/com/example/papereyes"
TEST = ROOT / "app/src/test/java/com/example/papereyes"
parser=argparse.ArgumentParser()
parser.add_argument("--class-filter", default="")
parser.add_argument("--microbenchmark", action="store_true")
args=parser.parse_args()
kotlinc=shutil.which("kotlinc")
if not kotlinc:
 print("NOT RUNNABLE: install a Kotlin JVM compiler (tested with 1.9.0)", file=sys.stderr); sys.exit(2)
lib=pathlib.Path(kotlinc).resolve().parent.parent / "lib"
coroutines=lib / "kotlinx-coroutines-core-jvm.jar"
production = [MAIN / p for p in [
 "data/model/ScholarlyIdentifiers.kt", "data/model/paper.kt",
 "ocr/ImageSampling.kt", "ocr/TextCandidateExtractor.kt",
 "ui/live/LiveScanUtils.kt", "domain/citation/JournalAliases.kt",
 "domain/citation/JournalCitation.kt", "domain/citation/JournalCitationParser.kt"]]
# Explicit inclusion prevents a shim from accidentally being treated as a real
# integration test. Additional pure core sources are registered here as added.
for directory in ["domain/evidence", "domain/discovery", "domain/telemetry", "util/concurrency"]:
 production.extend(sorted((MAIN/directory).glob("*.kt")))
for p in ["ocr/DocumentLayoutAnalyzer.kt", "data/remote/RetryAfter.kt", "data/remote/CrossrefApi.kt", "data/remote/InspireApi.kt", "data/remote/PaperRepository.kt", "data/remote/InspireRepository.kt", "data/remote/OpenAlexApi.kt", "data/remote/OpenAlexProvider.kt", "data/remote/ScholarlyRequestPolicy.kt", "data/remote/ArxivApi.kt", "data/remote/SemanticScholarApi.kt", "domain/PaperResolver.kt", "domain/ArxivFeedParser.kt", "data/local/PaperDao.kt"]:
 if (MAIN/p).exists(): production.append(MAIN/p)
tests=[]
for directory in ["data/model", "data/local", "ocr", "ui/live", "domain/citation", "domain/evidence", "domain/discovery", "domain/telemetry", "util/concurrency"]:
 tests.extend(sorted((TEST/directory).glob("*.kt")))
for name in ["InspireEvidenceTest.kt", "CitationSafetyRegressionTest.kt", "OpenAlexProviderTest.kt"]:
 p=TEST/"data/remote"/name
 if p.exists(): tests.append(p)
for name in ["PaperResolverEvidenceTest.kt", "ArxivFeedParserTest.kt"]:
 p=TEST/"domain"/name
 if p.exists(): tests.append(p)
classes=["com.example.papereyes."+str(p.relative_to(TEST).with_suffix("")).replace(os.sep,".") for p in tests]
classes=[c for c in classes if args.class_filter in c]
with tempfile.TemporaryDirectory(prefix="papereyes-portable-") as td:
 jar=pathlib.Path(td)/"tests.jar"
 command=[kotlinc, *map(str,production), *map(str,tests), *map(str,sorted((ROOT/"tools/offline/stubs").glob("*.kt"))), str(ROOT/"tools/offline/Runner.kt"), str(ROOT/"tools/offline/CoreMicrobenchmark.kt"), "-cp", str(coroutines), "-include-runtime", "-d", str(jar)]
 subprocess.run(command, check=True, cwd=ROOT)
 result=subprocess.run(["java", "-cp", str(jar)+os.pathsep+str(coroutines), ("CoreMicrobenchmark" if args.microbenchmark else "RunnerKt"), *([] if args.microbenchmark else classes)],cwd=ROOT)
 sys.exit(result.returncode)
