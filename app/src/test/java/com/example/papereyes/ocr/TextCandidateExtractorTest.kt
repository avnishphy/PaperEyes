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
        assertEquals(
            "",
            TextCandidateExtractor.extractBestQuery(
                "References and Notes\n[1] Example Author DOI: 10.1000/reference\n[2] Another reference"
            )
        )
    }

    @Test
    fun numberedReferenceWithoutHeadingDoesNotBecomePaperIdentifier() {
        assertEquals(
            "",
            TextCandidateExtractor.extractBestQuery("[1] Example Author DOI: 10.1000/reference")
        )
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
    @Test
    fun explicitModernArxivMetadataBeatsNoisyTitleText() {
        val ocr = """
            arXiv:2609.20448 v1 | nucl-th | 17 Sep 2026
            A comprehensive theory framework for perturbative calculations of oc in superallowed beta decays
            Chien-Yeah Seng
            Department of Physics and Astronomy
            Abstract
        """.trimIndent()

        assertEquals(
            "arXiv:2609.20448v1",
            TextCandidateExtractor.extractBestQuery(ocr)
        )
    }

}
