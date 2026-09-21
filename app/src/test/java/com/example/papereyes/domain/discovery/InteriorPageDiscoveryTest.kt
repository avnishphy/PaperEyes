package com.example.papereyes.domain.discovery

import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.evidence.TextFingerprint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteriorPageDiscoveryTest {
    private val a = "Nonperturbative generalized distributions reveal distinctive transverse correlations inside composite particles"
    private val b = "Independent lattice simulations constrain universal renormalization coefficients across discretization schemes"
    private fun evidence() = RetrievalEvidence(listOf(TextFingerprint(a, 1, 10.0), TextFingerprint(b, 2, 9.0)))
    private fun paper(doi: String = "10.1000/one") = Paper(title="Example", authors="", year=2023, doi=doi, url=null)
    private fun provider(items: List<PaperCandidate>) = object : PaperDiscoveryProvider {
        override val id = "fake"
        override val capabilities = setOf(ProviderCapability.FULL_TEXT_PHRASE_SEARCH)
        override suspend fun searchEvidence(evidence: RetrievalEvidence) = items
    }
    @Test fun rankWithoutReturnedTextCannotVerify() = runBlocking {
        val result = InteriorPageDiscovery(listOf(provider(listOf(PaperCandidate(paper(), "fake"))))).discover(evidence())
        assertEquals(null, result.verifiedPaper)
        assertEquals(0, result.candidates.single().independentMatches)
    }
    @Test fun twoIndependentExactPhrasesVerify() = runBlocking {
        val p = paper()
        val result = InteriorPageDiscovery(listOf(provider(listOf(PaperCandidate(p,"fake", listOf("$a. $b.")))))).discover(evidence())
        assertEquals(p, result.verifiedPaper)
        assertEquals(2, result.candidates.single().independentMatches)
    }
    @Test fun sameSentenceAndDuplicateProviderHitsCountOnce() = runBlocking {
        val candidate = PaperCandidate(paper(), "fake", listOf(a))
        val input = RetrievalEvidence(listOf(TextFingerprint(a, 1, 9.0), TextFingerprint(a, 2, 8.0), TextFingerprint(b, 1, 7.0)))
        val result = InteriorPageDiscovery(listOf(provider(listOf(candidate)),provider(listOf(candidate)))).discover(input)
        assertEquals(null, result.verifiedPaper)
        assertEquals(1, result.candidates.single().independentMatches)
    }
    @Test fun twoPlausibleSourcesAreAmbiguous() = runBlocking {
        val results = listOf(PaperCandidate(paper(),"fake",listOf(a,b)),PaperCandidate(paper("10.1000/two"),"fake",listOf(a,b)))
        assertEquals(null, InteriorPageDiscovery(listOf(provider(results))).discover(evidence()).verifiedPaper)
    }
    @Test fun partialWordsAndSeparatedTokensDoNotCount() = runBlocking {
        val altered = a.replace("distributions", "distributionschanged") + " " + b.replace("lattice", "lattice extra")
        val result=InteriorPageDiscovery(listOf(provider(listOf(PaperCandidate(paper(),"fake",listOf(altered)))))).discover(evidence())
        assertEquals(0,result.candidates.single().independentMatches)
    }
    @Test fun failuresAreNotNotFoundAndCancellationPropagates() = runBlocking {
        val failing=object:PaperDiscoveryProvider {
            override val id="offline"
            override val capabilities=setOf(ProviderCapability.ABSTRACT_SEARCH)
            override suspend fun searchEvidence(evidence:RetrievalEvidence):List<PaperCandidate> = error("offline")
        }
        assertTrue(InteriorPageDiscovery(listOf(failing)).discover(evidence()).unavailableProviders.contains("offline"))
        val cancelling=object:PaperDiscoveryProvider {
            override val id="cancel"
            override val capabilities=failing.capabilities
            override suspend fun searchEvidence(evidence:RetrievalEvidence):List<PaperCandidate> = throw CancellationException()
        }
        var cancelled=false
        try { InteriorPageDiscovery(listOf(cancelling)).discover(evidence()) } catch (_:CancellationException) {cancelled=true}
        assertTrue(cancelled)
    }
}
