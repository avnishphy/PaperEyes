package com.example.papereyes.ocr

import com.example.papereyes.domain.evidence.OcrLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceDetectorTest {

    @Test
    fun numberedBibliographyJoinsWrappedAndHyphenatedLines() {
        val detection = ReferenceDetector.fromText(
            """
            References
            [15] M. Diehl, Th. Feldmann, R. Jakob, and P. Kroll. Generalized parton distributions from
            nucleon form-factor data. Eur. Phys. J., C39:1-39, 2005.
            [16] Alessandro Bacchetta et al. Single-
            spin asymmetries: The Trento conventions. Phys. Rev. D70:117504, 2004.
            [17] A. V. Belitsky and D. Mueller. Exclusive electroproduction revisited. Phys. Rev. D82:074010, 2010.
            """.trimIndent()
        )

        assertEquals(listOf("15", "16", "17"), detection.references.map { it.label })
        assertTrue(detection.references[0].text.contains("nucleon form-factor data"))
        assertTrue(detection.references[1].text.contains("Single-spin asymmetries"))
    }

    @Test
    fun numberedReferencesDoNotRequireAHeadingWhenSeveralAreVisible() {
        val detection = ReferenceDetector.fromText(
            """
            [57] Belitsky, A.V., Mueller, D.: Exclusive electroproduction of lepton pairs as a probe of nucleon structure. Phys. Rev. Lett. 90, 022001 (2003) https://doi.org/10.1103/PhysRevLett.90.022001
            [58] Guidal, M., Vanderhaeghen, M.: Double deeply virtual Compton scattering off the nucleon. Phys. Rev. Lett. 90, 012001 (2003) arXiv:hep-ph/0208275
            """.trimIndent()
        )

        assertEquals(2, detection.references.size)
        assertEquals("10.1103/physrevlett.90.022001", detection.references[0].query)
        assertEquals("arXiv:hep-ph/0208275", detection.references[1].query)
    }

    @Test
    fun slideFooterCitationUsesQuotedTitleAsQuery() {
        val detection = ReferenceDetector.fromText(
            """Wan et al. “Regularization of Neural Networks using DropConnect”, ICML 2013"""
        )

        assertEquals(1, detection.references.size)
        assertEquals(
            "Regularization of Neural Networks using DropConnect",
            detection.references.single().query
        )
    }

    @Test
    fun ordinaryProseAndAHeadingAreNotReferences() {
        val detection = ReferenceDetector.fromText(
            """
            Regularization: A common pattern
            Training: Add random noise
            In this work we show that the 2013 result remains useful for testing.
            """.trimIndent()
        )

        assertTrue(detection.references.isEmpty())
    }

    @Test
    fun layoutReportsOriginalReferenceLineIndexes() {
        val lines = listOf(
            line("Unrelated slide heading", 10f, block = 0),
            line("[3] A. Author. First useful paper. Phys. Rev. D 10, 100 (2020).", 100f, block = 1),
            line("continued details", 120f, block = 1),
            line("[4] B. Author. Second useful paper. Phys. Rev. C 11, 101 (2021).", 150f, block = 2)
        )

        val detection = ReferenceDetector.fromLayout(lines, 1000, 800)

        assertEquals(2, detection.references.size)
        assertEquals(setOf(1, 2, 3), detection.referenceLineIndexes)
        assertFalse(0 in detection.referenceLineIndexes)
    }

    @Test
    fun unnumberedHangingIndentBibliographySplitsOneMlKitBlock() {
        val lines = listOf(
            line("PAPAMAKARIOS, NALISNICK, REZENDE, MOHAMED AND LAKSHMINARAYANAN", 20f, block = 7),
            line("Martin J. Wainwright and Michael I. Jordan. Graphical models, exponential families, and", 100f, block = 7),
            line("variational inference. Foundations and Trends in Machine Learning, 1(1-2):1-305, 2008.", 118f, block = 7, left = 42f),
            line("Prince Zizhuang Wang and William Yang Wang. Riemannian normalizing flow on variational", 146f, block = 7),
            line("Wasserstein autoencoder for text modeling. ArXiv Preprint arXiv:1904.02399,", 164f, block = 7, left = 42f),
            line("2019.", 182f, block = 7, left = 42f),
            line("Patrick Nadeem Ward, Ariella Smofsky, and Avishek Joey Bose. Improving exploration in", 210f, block = 7),
            line("soft-actor-critic with normalizing flows policies. ICML Workshop on Invertible Neural", 228f, block = 7, left = 42f),
            line("Networks and Normalizing Flows, 2019.", 246f, block = 7, left = 42f),
            line("Antoine Wehenkel and Gilles Louppe. Unconstrained monotonic neural networks. In Advances", 274f, block = 7),
            line("in Neural Information Processing Systems, 2019.", 292f, block = 7, left = 42f)
        )

        val detection = ReferenceDetector.fromLayout(lines, 1000, 800)

        assertEquals(4, detection.references.size)
        assertTrue(detection.references[0].text.startsWith("Martin J. Wainwright"))
        assertEquals("arXiv:1904.02399", detection.references[1].query)
        assertTrue(detection.references[3].text.contains("Neural Information Processing Systems"))
        assertFalse(0 in detection.referenceLineIndexes)
    }

    @Test
    fun outputIsBoundedAndDuplicateQueriesCollapse() {
        val repeated = (1..60).joinToString("\n") { number ->
            "[$number] A. Author. A bounded paper title. Journal $number, 1 (2020). doi:10.1000/$number"
        } + "\n[61] A. Author. Duplicate. Journal 1, 1 (2020). doi:10.1000/1"

        val detection = ReferenceDetector.fromText(repeated)

        assertEquals(50, detection.references.size)
        assertEquals(detection.references.size, detection.references.map { it.query }.distinct().size)
        assertTrue(detection.references.all { it.text.length <= 800 && it.query.length <= 500 })
    }

    private fun line(text: String, y: Float, block: Int, left: Float = 20f) =
        OcrLine(text, left, y, 980f, y + 16f, block)
}
