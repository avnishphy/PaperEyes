package com.example.papereyes.ocr
import com.example.papereyes.data.model.ScholarlyIdentifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
class AuditRegressionTest {
 @Test fun spacedArxivVersionsKeepTheirVersion() {
  listOf("2609.20448v1", "2609.20448 v1", "2609.20448v 1", "arXiv:2609.20448v 1").forEach {
   assertEquals(it, "2609.20448v1", ScholarlyIdentifiers.extractArxivId(it))
  }
 }
 @Test fun sectionIsNotTitle() {
  assertEquals("", TextCandidateExtractor.extractBestQuery("II. RESULTS AND DISCUSSION"))
 }
 @Test fun bodyProseIsNotTitle() {
  assertEquals("", TextCandidateExtractor.extractBestQuery("We calculate the contributions to the cross section using the numerical method described in the previous section."))
 }
 @Test fun damagedArxivMetadataIsNotTitle() {
  val result=TextCandidateExtractor.extractBestQuery("arXiv:2609.20448v l [nucl-th] 20 Sep 2026")
  assertFalse(result.contains("[nucl-th]"))
  assertFalse(result.contains("Sep"))
 }
 @Test fun numberedReferencesHeadingSuppressesDoi() {
  assertEquals("", TextCandidateExtractor.extractBestQuery("V. REFERENCES\n[1] Smith DOI: 10.1000/example"))
 }
 @Test fun veryLargeImageRemainsBounded() {
  val sample=calculateInSampleSize(1000000, 10000, 2400)
  assertTrue(1000000L / sample <= 2400)
 }
 @Test fun fractionalBoundRoundsUp() {
  assertEquals(4, calculateInSampleSize(4801, 100, 2400))
 }
}
