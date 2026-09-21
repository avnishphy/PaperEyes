package com.example.papereyes.domain.evidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class FingerprintExtractorTest {
    @Test fun genericFigureSentenceIsRejected() {
        assertTrue(FingerprintExtractor.extract(listOf("The results are shown in Fig. 2.")).isEmpty())
    }
    @Test fun distinctiveCaptionProducesBoundedQuery() {
        val text="Figure 7. Transverse momentum correlations reveal coherent diffractive scattering through nuclear shadowing corrections"
        val fingerprints=FingerprintExtractor.extract(listOf(text))
        assertTrue(fingerprints.isNotEmpty())
        assertTrue(fingerprints.single().caption)
        assertTrue(fingerprints.all { it.text.split(' ').size in 6..15 })
    }
    @Test fun overlappingWindowsAreNotIndependentEvidence() {
        val paragraph="Transverse momentum correlations reveal coherent diffractive scattering through nuclear shadowing corrections and generalized distributions"
        val f=FingerprintExtractor.extract(listOf(paragraph, paragraph))
        assertEquals(1, f.size)
    }
    @Test fun selectionIsDeterministicAndBounded() {
        val blocks=listOf("Transverse momentum correlations reveal coherent diffractive scattering through nuclear shadowing corrections.",
            "Renormalization group evolution constrains perturbative coefficient functions within generalized parton distributions.")
        assertEquals(FingerprintExtractor.extract(blocks), FingerprintExtractor.extract(blocks))
        assertEquals(2, FingerprintExtractor.extract(blocks).size)
        assertEquals(1, FingerprintExtractor.extract(blocks, 1).size)
    }
    @Test fun equationContaminationIsRejected() {
        assertTrue(FingerprintExtractor.extract(listOf("coherent perturbative nuclear coefficients = { ∫ ∑ } x_1 = { 42 }")).isEmpty())
    }
}
