package com.example.papereyes.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCandidateExtractorTest {

    @Test
    fun parenthesizedDoiIsCanonicalized() {
        assertEquals(
            "10.1000/xyz",
            TextCandidateExtractor.extractBestQuery("DOI: 10.1000/xyz)\nOther metadata")
        )
    }

    @Test
    fun legacyArxivCanBeSelectedFromExplicitEarlyMetadata() {
        assertEquals(
            "arXiv:hep-ph/9901234",
            TextCandidateExtractor.extractBestQuery("arXiv: hep-ph/9901234\nA short line")
        )
    }

    @Test
    fun referencesHeadingSuppressesFollowingIdentifiers() {
        val evidence = DocumentLayoutAnalyzer.fromText(
            "References and Notes\n" +
                "[1] Example Author DOI: 10.1000/reference\n" +
                "[2] Another Author DOI: 10.1000/second"
        )

        assertEquals("", evidence.bestQuery)
        assertEquals(2, evidence.references.size)
        assertEquals("10.1000/reference", evidence.references.first().query)
    }

    @Test
    fun numberedReferenceWithoutHeadingDoesNotBecomePaperIdentifier() {
        assertEquals(
            "",
            TextCandidateExtractor.extractBestQuery("[1] Example Author DOI: 10.1000/reference")
        )
    }

    @Test
    fun detectedReferencesDoNotOverrideTheScannedPaperTitle() {
        val evidence = DocumentLayoutAnalyzer.fromText(
            """
            Reliable Identification of Scientific Papers
            References
            [1] A. Author. First cited work. doi:10.1000/first
            [2] B. Author. Second cited work. doi:10.1000/second
            """.trimIndent()
        )

        assertEquals("Reliable Identification of Scientific Papers", evidence.bestQuery)
        assertEquals(2, evidence.references.size)
    }

    @Test
    fun frameSimilarityToleratesSmallOcrChangesButRejectsDifferentText() {
        assertTrue(
            TextCandidateExtractor.areSimilar(
                "Deep inelastic scattering from nuclei",
                "Deep inelastic scatterlng from nuclei"
            )
        )
        assertFalse(
            TextCandidateExtractor.areSimilar(
                "Deep inelastic scattering from nuclei",
                "Quantum gravity and black hole entropy"
            )
        )
    }
}
