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
        assertEquals("2609.20448v1", ScholarlyIdentifiers.extractArxivId("arXiv:2609.20448 v1 | nucl-th"))
        assertEquals(
            listOf("2609.20448v1"),
            ScholarlyIdentifiers.extractAllArxivIds("arXiv:2609.20448 v1 | nucl-th | 17 Sep 2026")
        )
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
}
