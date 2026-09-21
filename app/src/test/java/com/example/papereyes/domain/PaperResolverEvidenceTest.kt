package com.example.papereyes.domain

import com.example.papereyes.data.remote.*
import com.example.papereyes.domain.discovery.*
import com.example.papereyes.domain.evidence.*
import com.example.papereyes.util.concurrency.RequestGate
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class PaperResolverEvidenceTest {
 private fun resolver(interior:InteriorPageDiscovery=InteriorPageDiscovery(emptyList()),s2:SemanticScholarApi=object:SemanticScholarApi {
  override suspend fun getPaper(paperId:String,fields:String):SemanticScholarPaper=error("unexpected lookup")
 }):PaperResolver {
  val policy=ScholarlyRequestPolicy{RequestGate(0)}
  return PaperResolver(
   PaperRepository(object:CrossrefApi {
    override suspend fun searchWorks(query:String?,containerTitle:String?,filter:String?,rows:Int,select:String):CrossrefResponse=error("must not search body as title")
    override suspend fun getWorkByDoi(doi:String):CrossrefSingleResponse=error("unexpected DOI")
   },policy),
   InspireRepository(object:InspireApi {override suspend fun searchLiterature(query:String,size:Int):InspireSearchResponse=error("unexpected journal")},policy),
   s2,object:ArxivApi {override suspend fun queryById(idList:String):ResponseBody=error("unexpected fallback")},interior,policy)
 }
 @Test fun bodyWithNoFingerprintsDoesNotBecomeATitleQuery()=runBlocking {
  assertEquals(ResolutionStatus.UNCERTAIN,resolver().resolveEvidence(DocumentEvidence(layout=LayoutProfile(LayoutRole.BODY))).status)
 }
 @Test fun interiorProviderFailureRemainsUnavailableAndIsNotCached()=runBlocking {
  var calls=0
  val provider=object:PaperDiscoveryProvider {
   override val id="fake"
   override val capabilities=setOf(ProviderCapability.ABSTRACT_SEARCH)
   override suspend fun searchEvidence(evidence:RetrievalEvidence):List<PaperCandidate> {calls++;error("offline")}
  }
  val resolver=resolver(InteriorPageDiscovery(listOf(provider)))
  val e=DocumentEvidence(fingerprints=listOf(TextFingerprint("Distinctive nonperturbative distributions constrain universal transverse coefficients",1,9.0)))
  repeat(2){assertEquals(ResolutionStatus.PROVIDERS_UNAVAILABLE,resolver.resolveEvidence(e).status)}
  assertEquals(2,calls)
 }
 @Test fun arxivWorkLookupStripsOnlyVersionAndDeduplicatesEquivalentInputs()=runBlocking {
  var calls=0;var query=""
  val s2=object:SemanticScholarApi {
   override suspend fun getPaper(paperId:String,fields:String):SemanticScholarPaper {
    calls++;query=paperId
    return SemanticScholarPaper("Title",2026,null,emptyList(),SemanticScholarExternalIds(null,"2609.20448"))
   }
  }
  val resolver=resolver(s2=s2)
  assertEquals(ResolutionStatus.VERIFIED,resolver.resolve("arXiv:2609.20448v 1").status)
  assertEquals("ARXIV:2609.20448",query)
  resolver.resolve("https://arxiv.org/abs/2609.20448v1")
  assertEquals(1,calls)
 }
}
