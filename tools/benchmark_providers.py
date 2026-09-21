#!/usr/bin/env python3
"""Replay curated provider queries; not an OCR or end-to-end Android benchmark.

No API keys or paid services. Requires --run and a complete provenance-bearing
50-paper x 5-mode x 4-corruption manifest. Never treats API errors as not-found.
See docs/BENCHMARK.md. All live-network results remain unmeasured in this handoff.
"""
from __future__ import annotations
import argparse, collections, datetime as dt, email.utils, json, math, pathlib
import socket, statistics, time, urllib.error, urllib.parse, urllib.request
from typing import Any

MODES={"first_page","title_crop","journal_header","interior_page","caption_page"}
RATES={0,5,10,20}
HOSTS={"openalex":"api.openalex.org","semantic_scholar":"api.semanticscholar.org","crossref":"api.crossref.org"}


def percentile(values:list[float], p:float)->float|None:
    if not values:return None
    ordered=sorted(values); position=(len(ordered)-1)*p
    low=math.floor(position);high=math.ceil(position)
    return ordered[low]+(ordered[high]-ordered[low])*(position-low)


def validate_manifest(data:dict[str,Any])->list[dict[str,Any]]:
    if data.get("synthetic",False):raise ValueError("Synthetic cases cannot be presented as a live paper benchmark")
    papers=data.get("papers",[])
    if len(papers)<50:raise ValueError("Need at least 50 real papers; no fabricated completion")
    if len({p.get("id") for p in papers})!=len(papers):raise ValueError("Paper IDs must be unique")
    cases=[]
    for paper in papers:
        if not all(paper.get(k) for k in ["id","title","source_url","reuse_basis","expected_ids","domain"]):
            raise ValueError("Missing paper identity/provenance/reuse basis/domain")
        observed={(c.get("mode"),c.get("corruption_percent")) for c in paper.get("cases",[])}
        if observed != {(m,r) for m in MODES for r in RATES} or len(paper["cases"])!=20:
            raise ValueError(f"Incomplete/duplicate 5x4 matrix for {paper['id']}")
        for case in paper["cases"]:
            if not case.get("ocr_provenance") or not case.get("queries"):
                raise ValueError("Record actual OCR/evidence extraction provenance and selected queries")
            if case["mode"]=="interior_page" and not case.get("no_visible_identifier_or_title"):
                raise ValueError("Interior cases must exclude visible title/identifiers")
            for provider,query in case["queries"].items():
                if provider not in HOSTS or not isinstance(query,str) or not 1<=len(query)<=800:
                    raise ValueError("Unsupported provider or unbounded/empty query")
            cases.append({**case,"paper_id":paper["id"],"domain":paper["domain"],"expected_ids":paper["expected_ids"]})
    return cases


def build_url(provider:str,query:str)->str:
    if provider=="openalex":
        path="/works";params={"search":query,"per_page":5,"select":"id,doi,title"}
    elif provider=="semantic_scholar":
        path="/graph/v1/paper/search";params={"query":query,"limit":5,"fields":"title,externalIds"}
    elif provider=="crossref":
        path="/works";params={"query.bibliographic":query,"rows":5,"select":"DOI,title"}
    else:raise ValueError("Unsupported provider")
    return "https://"+HOSTS[provider]+path+"?"+urllib.parse.urlencode(params)


def normalized_id(value:str)->str:
    value=value.strip().lower()
    for prefix in ("https://doi.org/","http://doi.org/","http://dx.doi.org/"):
        if value.startswith(prefix):return "doi:"+value[len(prefix):]
    if value.startswith("10."):return "doi:"+value
    if value.startswith("https://openalex.org/"):return "openalex:"+value.rsplit("/",1)[-1]
    return value


def parse_results(provider:str,data:dict[str,Any])->list[set[str]]:
    if provider=="openalex":
        items=data["results"]
        return [{normalized_id(x) for x in (item.get("id"),item.get("doi")) if x} for item in items[:5]]
    if provider=="crossref":
        items=data["message"]["items"]
        return [{normalized_id(item["DOI"])} for item in items[:5]]
    if provider=="semantic_scholar":
        results=[]
        for item in data["data"][:5]:
            ids=item.get("externalIds") or {};values=set()
            if item.get("paperId"):values.add("s2:"+item["paperId"].lower())
            if ids.get("DOI"):values.add(normalized_id(ids["DOI"]))
            if ids.get("ArXiv"):values.add("arxiv:"+ids["ArXiv"].lower())
            results.append(values)
        return results
    raise ValueError("Unsupported provider")


def retry_after(value:str|None, now:float)->float:
    if value:
        if value.strip().isdigit():return min(float(value),86400*7)
        try:return max(0.,email.utils.parsedate_to_datetime(value).timestamp()-now)
        except (ValueError,TypeError,OverflowError):pass
    return 60.


def summarize(rows:list[dict[str,Any]])->dict[str,Any]:
    successful=[r for r in rows if r["status"]=="ok"]
    attempted=[r for r in rows if r["status"]!="not_attempted_cooldown"]
    latencies=[r["latency_ms"] for r in attempted if r.get("latency_ms") is not None]
    rate=lambda numerator,denominator: numerator/denominator if denominator else None
    return {
        "planned":len(rows),"attempted":len(attempted),"valid_responses":len(successful),
        "top1_accuracy_all_planned":rate(sum(bool(r.get("top1")) for r in rows),len(rows)),
        "top5_recall_all_planned":rate(sum(bool(r.get("top5")) for r in rows),len(rows)),
        "top1_accuracy_valid_responses":rate(sum(bool(r.get("top1")) for r in successful),len(successful)),
        "api_error_rate_attempted":rate(sum(r["status"]!="ok" for r in attempted),len(attempted)),
        "not_found_rate_valid_responses":rate(sum(r["result_count"]==0 for r in successful),len(successful)),
        "median_latency_ms":percentile(latencies,.5),"p90_latency_ms":percentile(latencies,.9),"p95_latency_ms":percentile(latencies,.95),
        "false_confident_match_rate":None,
        "confidence_note":"Not measured: API search rank is not the app's verification decision",
        "latency_note":"Request round trip incl. decoding/errors; excludes deliberate inter-request pacing, camera and OCR"
    }


def preflight()->dict[str,Any]:
    results={}
    for provider,host in HOSTS.items():
        try:socket.getaddrinfo(host,443,type=socket.SOCK_STREAM);results[provider]={"dns":"ok"}
        except OSError as error:results[provider]={"dns":"blocked","error_type":type(error).__name__,"message":str(error)}
    return {"kind":"connectivity_preflight_not_benchmark","utc":dt.datetime.now(dt.timezone.utc).isoformat(),"providers":results}


def run(cases:list[dict[str,Any]])->list[dict[str,Any]]:
    rows=[];next_allowed=collections.defaultdict(float);cooldown=collections.defaultdict(float)
    for case in cases:
        for provider,query in case["queries"].items():
            row={"paper_id":case["paper_id"],"provider":provider,"domain":case["domain"],"mode":case["mode"],"corruption_percent":case["corruption_percent"]}
            if cooldown[provider]>time.monotonic():
                row["status"]="not_attempted_cooldown";rows.append(row);continue
            time.sleep(max(0.,next_allowed[provider]-time.monotonic()))
            request=urllib.request.Request(build_url(provider,query),headers={"User-Agent":"PaperEyesAuditBenchmark/1.0","Accept":"application/json"})
            started=time.perf_counter()
            try:
                with urllib.request.urlopen(request,timeout=20) as response:
                    raw=response.read(2_000_001)
                    if len(raw)>2_000_000:raise ValueError("Oversized provider response")
                    candidates=parse_results(provider,json.loads(raw))
                truth={normalized_id(i) for i in case["expected_ids"]}
                row.update(status="ok",result_count=len(candidates),top1=bool(candidates and candidates[0]&truth),top5=any(c&truth for c in candidates))
            except urllib.error.HTTPError as error:
                row.update(status="http_error",http_status=error.code)
                if error.code==429:
                    delay=retry_after(error.headers.get("Retry-After"),time.time())
                    if provider=="openalex" and error.headers.get("X-RateLimit-Remaining")=="0":
                        delay=max(delay,retry_after(error.headers.get("X-RateLimit-Reset"),time.time()))
                    cooldown[provider]=time.monotonic()+delay
            except (OSError,ValueError,KeyError,TypeError) as error:
                row.update(status="transport_or_contract_error",error_type=type(error).__name__)
            row["latency_ms"]=(time.perf_counter()-started)*1000
            next_allowed[provider]=time.monotonic()+1.05
            rows.append(row)
    return rows


def main()->int:
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preflight",action="store_true")
    parser.add_argument("--manifest",type=pathlib.Path)
    parser.add_argument("--run",action="store_true")
    parser.add_argument("--output",type=pathlib.Path,required=True)
    args=parser.parse_args()
    args.output.parent.mkdir(parents=True,exist_ok=True)
    if args.preflight:data=preflight()
    else:
        if not args.manifest:parser.error("--manifest required")
        try:cases=validate_manifest(json.loads(args.manifest.read_text()))
        except (OSError,ValueError,KeyError,TypeError) as error:parser.error(str(error))
        if not args.run:data={"kind":"manifest_validation_only","case_count":len(cases),"network_requests":0}
        else:
            checks=preflight()
            if any(v["dns"]!="ok" for v in checks["providers"].values()):
                args.output.write_text(json.dumps(checks,indent=2));return 2
            rows=run(cases)
            groups=collections.defaultdict(list)
            for row in rows:groups[(row["provider"],row["mode"],row["corruption_percent"])].append(row)
            data={"kind":"provider_query_replay_not_android_benchmark","rows":rows,"by_provider_mode_corruption":{
                "/".join(map(str,k)):summarize(v) for k,v in groups.items()}}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(data,indent=2));print(json.dumps(data,indent=2))
    return 0 if not args.preflight or all(v["dns"]=="ok" for v in data["providers"].values()) else 2
if __name__=="__main__":raise SystemExit(main())
