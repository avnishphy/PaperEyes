package com.example.papereyes.domain.evidence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceEvidenceMergerTest {

    @Test
    fun combinesComplementaryNumberedFramesWithoutDuplicatingLabels() {
        val first = listOf(
            reference("15", "M. Diehl. Generalized parton distributions. 2005."),
            reference("16", "A. Bacchetta. The Trento conventions. 2004.")
        )
        val second = listOf(
            reference("16", "A. Bacchetta et al. The Trento conventions. Phys. Rev. D, 2004."),
            reference("17", "A. Belitsky. Exclusive electroproduction revisited. 2010.")
        )

        val merged = mergeReferenceEvidence(first, second)

        assertEquals(listOf("15", "16", "17"), merged.map { it.label })
        assertTrue(merged[1].text.contains("Phys. Rev."))
    }

    @Test
    fun collapsesUnnumberedOcrVariantsByTokenOverlap() {
        val first = listOf(
            ReferenceEvidence(
                null,
                "Wan et al. Regularization of Neural Networks using DropConnect, ICML 2013"
            )
        )
        val second = listOf(
            ReferenceEvidence(
                null,
                "Wan et al Regularization of Neural Networks using DropConnect ICML, 2013."
            )
        )

        assertEquals(1, mergeReferenceEvidence(first, second).size)
    }

    @Test
    fun preservesDistinctNumberedReferencesEvenWhenAuthorsOverlap() {
        val first = listOf(reference("34", "S. Goloskokov and P. Kroll. First paper. 2010."))
        val second = listOf(reference("35", "S. Goloskokov and P. Kroll. Second paper. 2011."))

        assertEquals(2, mergeReferenceEvidence(first, second).size)
    }

    @Test
    fun collapsesAnOcrLabelMistakeWhenTheReferenceTextIsNearlyIdentical() {
        val first = listOf(
            reference("34", "S. Goloskokov and P. Kroll. Transversity in hard electroproduction. 2011.")
        )
        val second = listOf(
            reference("84", "S. Goloskokov and P. Kroll. Transversity in hard electroproduction, 2011.")
        )

        assertEquals(1, mergeReferenceEvidence(first, second).size)
    }

    private fun reference(label: String, text: String) =
        ReferenceEvidence(label = label, text = text, query = text)
}
