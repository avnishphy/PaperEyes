package com.example.papereyes.data.remote

import com.example.papereyes.domain.citation.JournalCitation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercise actual repository ranking helpers without network ranking or response mocks. */
class CitationSafetyRegressionTest {
 private val citation=JournalCitation("Phys.Rev.D 112 (2025) 034009","Physical Review D","Phys.Rev.D","112",null,2025,"034009",.99)
 private val crossref=PaperRepository(object:CrossrefApi {
  override suspend fun searchWorks(query:String?,containerTitle:String?,filter:String?,rows:Int,select:String):CrossrefResponse=error("not used")
  override suspend fun getWorkByDoi(doi:String):CrossrefSingleResponse=error("not used")
 })
 private fun item(locator:String?="034009",volume:String?="112",year:Int=2025)=CrossrefItem(
  title=listOf("A paper"),author=emptyList(),doi="10.1000/test",url=null,
  containerTitle=listOf("Physical Review D"),shortContainerTitle=null,volume=volume,issue=null,
  page=null,articleNumber=locator,publishedPrint=CrossrefDate(listOf(listOf(year))),publishedOnline=null,published=null)
 private fun accepted(item:CrossrefItem):Boolean {
  val calculate=PaperRepository::class.java.getDeclaredMethod("calculateCitationEvidence",JournalCitation::class.java,CrossrefItem::class.java).apply{isAccessible=true}
  val evidence=calculate.invoke(crossref,citation,item)
  val accept=PaperRepository::class.java.declaredMethods.single{it.name=="isAcceptableCitationMatch"}.apply{isAccessible=true}
  return accept.invoke(crossref,evidence) as Boolean
 }
 @Test fun matchingJournalVolumeYearDoesNotReplaceMissingArticleNumber(){assertFalse(accepted(item(locator=null)))}
 @Test fun matchingJournalVolumeYearDoesNotOverrideWrongArticleNumber(){assertFalse(accepted(item(locator="034010")))}
 @Test fun articleNumberPrefixDoesNotIdentifyLongerDifferentNumber(){assertFalse(accepted(item(locator="0340099")))}
 @Test fun knownVolumeDisagreementBlocksIdentity(){assertFalse(accepted(item(volume="113")))}
 @Test fun exactArticleAndBibliographyRemainAccepted(){assertTrue(accepted(item()))}
 @Test fun actualPageRangeRemainsAccepted(){assertTrue(accepted(item(locator="034009-18")))}
 @Test fun missingVolumeCannotVerifyARepeatedArticleNumber(){assertFalse(accepted(item(volume=null)))}
 @Test fun knownShortJournalAliasStillMatches(){assertTrue(accepted(item().copy(containerTitle=listOf("Phys. Rev. D"))))}
 @Test fun differentPhysicalReviewSeriesCannotMatch(){assertFalse(accepted(item().copy(containerTitle=listOf("Physical Review C"))))}
}
