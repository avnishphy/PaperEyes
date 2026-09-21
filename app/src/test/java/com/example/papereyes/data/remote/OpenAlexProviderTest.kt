package com.example.papereyes.data.remote
import com.example.papereyes.domain.discovery.RetrievalEvidence
import com.example.papereyes.domain.evidence.TextFingerprint
import com.example.papereyes.util.concurrency.RequestGate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class OpenAlexProviderTest {
 @Test fun mapsOnlyBoundedMetadataAndReturnedAbstract() = runBlocking {
  var captured=""
  val api=object:OpenAlexApi {
   override suspend fun searchWorks(search:String,perPage:Int,select:String):OpenAlexResponse {
    captured=search;assertEquals(5,perPage)
    return OpenAlexResponse(listOf(OpenAlexWork("https://openalex.org/W123","https://doi.org/10.1000/ABC","Title",2023,
     listOf(OpenAlexAuthorship(OpenAlexAuthor("Author"))),mapOf("Distinctive" to listOf(0),"text" to listOf(1)))))
   }
  }
  val provider=OpenAlexProvider(api,ScholarlyRequestPolicy{RequestGate(0)})
  val result=provider.searchEvidence(RetrievalEvidence(listOf(TextFingerprint("Distinctive nonperturbative distributions constrain universal transverse coefficients",1,9.0))))
  assertEquals("10.1000/abc",result.single().paper.doi)
  assertEquals(listOf("Distinctive text"),result.single().verificationTexts)
  assertTrue(captured.startsWith("\""));assertTrue(captured.endsWith("\""))
 }
 @Test fun invalidIdentityAndMissingTitleAreDiscarded() = runBlocking {
  val api=object:OpenAlexApi {
   override suspend fun searchWorks(search:String,perPage:Int,select:String)=OpenAlexResponse(listOf(
    OpenAlexWork("javascript:bad",null,"Title",null,null,null),
    OpenAlexWork("https://openalex.org/W1",null,null,null,null,null)))
  }
  assertTrue(OpenAlexProvider(api,ScholarlyRequestPolicy{RequestGate(0)}).searchEvidence(RetrievalEvidence(
   listOf(TextFingerprint("Distinctive nonperturbative distributions constrain universal transverse coefficients",1,9.0)))).isEmpty())
 }
}
