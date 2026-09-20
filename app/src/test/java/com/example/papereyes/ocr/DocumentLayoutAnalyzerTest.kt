package com.example.papereyes.ocr

import com.example.papereyes.ocr.DocumentLayoutAnalyzer.EvidenceType
import com.example.papereyes.ocr.DocumentLayoutAnalyzer.LayoutLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentLayoutAnalyzerTest {

    @Test
    fun `complete PRD header is preserved as strong bibliographic evidence`() {
        val evidence = DocumentLayoutAnalyzer.analyzeLines(
            inputLines = listOf(
                line("PHYSICAL REVIEW D 108, 036027 (2023)", 80, 36, 720, 50),
                line("Shedding light on shadow generalized parton distributions", 70, 88, 730, 122),
                line("Adam Freese, Ian Cloet, Thomas Donohoe", 110, 145, 690, 163),
                line("Abstract", 90, 205, 190, 226),
                line("We study generalized parton distributions using deeply virtual", 70, 240, 740, 258),
                line("Compton scattering and discuss constraints from available data.", 70, 264, 740, 282),
                line("The formalism is developed for phenomenological applications.", 70, 288, 740, 306),
                line("Additional discussion follows in the subsequent sections.", 70, 312, 740, 330)
            ),
            imageWidth = 800,
            imageHeight = 1000
        )

        val journal = evidence.journalCitations.first()
        assertEquals("Physical Review D", journal.citation.journal)
        assertEquals("108", journal.citation.volume)
        assertEquals("036027", journal.citation.locator)
        assertEquals(2023, journal.citation.year)
        assertEquals(EvidenceType.JOURNAL_CITATION, evidence.preferred?.type)
        assertEquals(
            "Shedding light on shadow generalized parton distributions",
            evidence.bestTitle?.text
        )
    }

    @Test
    fun `multiline largest-font title is reconstructed over body baseline`() {
        val evidence = DocumentLayoutAnalyzer.analyzeLines(
            inputLines = listOf(
                line("A comprehensive theory framework for", 80, 90, 720, 124),
                line("perturbative calculations of delta C in", 75, 130, 725, 164),
                line("superallowed beta decays", 180, 170, 620, 204),
                line("Chien-Yeah Seng", 300, 226, 500, 245),
                line("Abstract", 90, 280, 180, 302),
                line("The present high precision determination requires a consistent", 75, 320, 730, 338),
                line("treatment of nuclear corrections and electroweak effects.", 75, 344, 730, 362),
                line("We present the ingredients of the calculation in detail.", 75, 368, 730, 386),
                line("The resulting framework is suitable for phenomenology.", 75, 392, 730, 410)
            ),
            imageWidth = 800,
            imageHeight = 1000
        )

        assertEquals(EvidenceType.TITLE, evidence.preferred?.type)
        assertEquals(
            "A comprehensive theory framework for perturbative calculations of delta C in superallowed beta decays",
            evidence.preferred?.query
        )
        assertTrue((evidence.bestTitle?.sizeRatioToBody ?: 0.0) > 1.4)
    }

    @Test
    fun `single-size prose frame is treated as body rather than a title`() {
        val evidence = DocumentLayoutAnalyzer.analyzeLines(
            inputLines = listOf(
                line("The cross section can be written in terms of the relevant amplitudes.", 70, 120, 740, 140),
                line("We use the measured data to constrain the model parameters.", 70, 145, 740, 165),
                line("The resulting distributions are shown in the following figures.", 70, 170, 740, 190),
                line("This procedure provides a stable description of the observations.", 70, 195, 740, 215),
                line("The uncertainty is propagated through the numerical analysis.", 70, 220, 740, 240),
                line("Further details are provided in the supplemental discussion.", 70, 245, 740, 265)
            ),
            imageWidth = 800,
            imageHeight = 1000
        )

        assertNull(evidence.preferred)
        assertEquals(1, evidence.fontSizeClusters.size)
    }

    @Test
    fun `explicit arxiv identifier outranks typography`() {
        val evidence = DocumentLayoutAnalyzer.analyzeLines(
            inputLines = listOf(
                line("arXiv:2609.20448 v1 [nucl-th] 17 Sep 2026", 80, 35, 720, 51),
                line("A comprehensive theory framework for", 70, 90, 730, 126),
                line("perturbative calculations of delta C in", 70, 132, 730, 168),
                line("superallowed beta decays", 180, 174, 620, 210),
                line("The present analysis determines the correction consistently.", 70, 310, 735, 328),
                line("The method is described in detail in the following sections.", 70, 334, 735, 352)
            ),
            imageWidth = 800,
            imageHeight = 1000
        )

        assertEquals(EvidenceType.ARXIV, evidence.preferred?.type)
        assertEquals("arXiv:2609.20448v1", evidence.preferred?.query)
    }

    @Test
    fun `section heading does not become paper title`() {
        val evidence = DocumentLayoutAnalyzer.analyzeLines(
            inputLines = listOf(
                line("I. INTRODUCTION", 90, 100, 330, 128),
                line("The study of generalized parton distributions provides access", 70, 160, 740, 180),
                line("to spatial and momentum correlations inside hadrons.", 70, 185, 740, 205),
                line("We consider deeply virtual Compton scattering in this work.", 70, 210, 740, 230),
                line("The rest of this article is organized as follows.", 70, 235, 740, 255),
                line("Several observables are discussed in the next section.", 70, 260, 740, 280)
            ),
            imageWidth = 800,
            imageHeight = 1000
        )

        assertNull(evidence.preferred)
    }

    private fun line(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) = LayoutLine(
        text = text,
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}
