#!/usr/bin/env python3
"""Synthetic harness tests; never report them as scholarly provider accuracy."""
import json, unittest, tempfile, pathlib
from unittest.mock import patch
import benchmark_providers
from benchmark_providers import validate_manifest, percentile, parse_results, summarize, retry_after, build_url
class BenchmarkToolsTest(unittest.TestCase):
 def test_percentile_definition(self):self.assertAlmostEqual(9.,percentile([0.,10.],.9));self.assertIsNone(percentile([],.5))
 def test_errors_are_not_not_found(self):
  rows=[dict(status="ok",result_count=0,top1=False,top5=False,latency_ms=10),dict(status="http_error",latency_ms=20),dict(status="not_attempted_cooldown")]
  result=summarize(rows)
  self.assertEqual(.5,result["api_error_rate_attempted"]);self.assertEqual(1.,result["not_found_rate_valid_responses"])
  self.assertEqual(2,result["attempted"]);self.assertIsNone(result["false_confident_match_rate"])
 def test_openalex_and_crossref_ids(self):
  self.assertEqual([{"doi:10.1000/a","openalex:w1"}],parse_results("openalex",{"results":[{"id":"https://openalex.org/W1","doi":"https://doi.org/10.1000/A"}]}))
  self.assertEqual([{"doi:10.1000/a"}],parse_results("crossref",{"message":{"items":[{"DOI":"10.1000/A"}]}}))
 def test_semantic_scholar_ids(self):
  self.assertEqual([{"doi:10.1000/a","s2:hash","arxiv:2609.20448"}],parse_results("semantic_scholar",{"data":[{"paperId":"HASH","externalIds":{"DOI":"10.1000/A","ArXiv":"2609.20448"}}]}))
 def test_contract_error_is_not_empty_success(self):
  with self.assertRaises(KeyError):parse_results("openalex",{"error":"forbidden"})
 def test_short_or_synthetic_corpus_is_rejected(self):
  for manifest in ({"papers":[]},{"synthetic":True,"papers":[{}]*50}):
   with self.assertRaises(ValueError):validate_manifest(manifest)
 def test_quoting_and_no_key(self):
  url=build_url("openalex",'"rare phrase" OR "another phrase"')
  self.assertNotIn("api_key",url);self.assertIn("%22rare+phrase%22",url)
 def test_retry_after(self):
  self.assertEqual(3.,retry_after("3",0));self.assertEqual(1.,retry_after("Thu, 01 Jan 1970 00:00:01 GMT",0))
 def test_dns_failure_writes_nested_output_directory(self):
  with tempfile.TemporaryDirectory() as temp:
   base=pathlib.Path(temp); manifest=base/"corpus.json";manifest.write_text("{}")
   output=base/"new"/"deep"/"results.json"
   checks={"kind":"connectivity_preflight_not_benchmark","providers":{"openalex":{"dns":"blocked"}}}
   with patch("sys.argv",["benchmark_providers.py","--run","--manifest",str(manifest),"--output",str(output)]), patch.object(benchmark_providers,"validate_manifest",return_value=[]), patch.object(benchmark_providers,"preflight",return_value=checks):
    self.assertEqual(2,benchmark_providers.main())
   self.assertEqual(checks,json.loads(output.read_text()))
if __name__=="__main__":unittest.main(verbosity=2)
