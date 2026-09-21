package com.example.papereyes.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScholarlyIdentifiersTest {

    @Test
    fun doiCanonicalization_normalizesCommonForms() {
        assertEquals("10.1000/abc", ScholarlyIdentifiers.normalizeDoi("10.1000/ABC"))
        assertEquals("10.1000/abc", ScholarlyIdentifiers.normalizeDoi("https://doi.org/10.1000/ABC"))
        assertEquals("10.1000/abc", ScholarlyIdentifiers.normalizeDoi("doi: 10.1000/ABC"))
    }

    @Test
    fun doiCanonicalization_stripsOnlyUnmatchedClosingParenthesis() {
        assertEquals("10.1000/xyz", ScholarlyIdentifiers.normalizeDoi("10.1000/xyz)"))
        assertEquals("10.1000/foo(bar)", ScholarlyIdentifiers.normalizeDoi("10.1000/foo(bar)"))
        assertEquals("10.1000/foo(bar)", ScholarlyIdentifiers.normalizeDoi("(doi:10.1000/foo(bar))"))
    }

    @Test
    fun arxivExtraction_supportsModernLegacyUrlsAndDoiAliases() {
        assertEquals("2609.20448v2", ScholarlyIdentifiers.extractArxivId("https://arxiv.org/pdf/2609.20448v2.pdf"))
        assertEquals("hep-ph/9901234", ScholarlyIdentifiers.extractArxivId("arXiv:hep-ph/9901234"))
        assertEquals("2609.20448", ScholarlyIdentifiers.extractArxivDoiId("https://doi.org/10.48550/arXiv.2609.20448"))
    }

    @Test
    fun invalidIdentifiers_areRejected() {
        assertNull(ScholarlyIdentifiers.normalizeDoi("not a DOI"))
        assertNull(ScholarlyIdentifiers.extractArxivId("paper 12345"))
        assertFalse(ScholarlyIdentifiers.looksLikeIdentifier("A normal research paper title"))
        assertTrue(ScholarlyIdentifiers.looksLikeIdentifier("DOI: 10.1103/PhysRevD.112.034009"))
    }
    @Test fun spacedVersionsAndWorkKeysAreStable() {
        listOf("2609.20448v1", "2609.20448 v1", "2609.20448v 1", "arXiv:2609.20448v 1").forEach {
            assertEquals("2609.20448v1", ScholarlyIdentifiers.extractArxivId(it))
            assertEquals("2609.20448", ScholarlyIdentifiers.arxivWorkId(it))
        }
        assertEquals("hep-ph/9901234v2", ScholarlyIdentifiers.extractArxivId("hep-ph/9901234 v 2"))
        assertEquals("2609.20448v2", ScholarlyIdentifiers.extractArxivDoiId("10.48550/arXiv.2609.20448 v 2"))
        assertNull(ScholarlyIdentifiers.extractArxivId("arXiv:2609.20448v l"))
        assertNull(ScholarlyIdentifiers.extractArxivId("arXiv:2609.20448 v l"))
        assertNull(ScholarlyIdentifiers.extractArxivId("2699.20448"))
    }

    @Test fun requestedDoiPunctuationCasesAreIdempotent() {
        val cases = mapOf(
            "http://dx.doi.org/10.1000/ABC" to "10.1000/abc",
            "(10.1000/xyz)" to "10.1000/xyz",
            "10.1000/foo(bar))" to "10.1000/foo(bar)",
            "10.1000/foo." to "10.1000/foo",
            "10.1000/foo];" to "10.1000/foo"
        )
        cases.forEach { (raw, expected) ->
            assertEquals(expected, ScholarlyIdentifiers.normalizeDoi(raw))
            assertEquals(expected, ScholarlyIdentifiers.normalizeDoi(expected))
        }
    }
}
