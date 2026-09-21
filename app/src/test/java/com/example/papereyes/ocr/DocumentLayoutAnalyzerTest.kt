package com.example.papereyes.ocr

import com.example.papereyes.domain.evidence.OcrLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentLayoutAnalyzerTest {
    private fun line(text: String, y: Float, h: Float = 16f, left: Float = 80f, right: Float = 900f, block: Int = 0) =
        OcrLine(text, left, y, right, y + h, block)
    private fun analyze(vararg lines: OcrLine) = DocumentLayoutAnalyzer.analyze(
        lines.joinToString("\n") { it.text }, lines.toList(), 1000, 1400
    )
    private val body = "We calculate the contributions to the cross section using the numerical method described in the previous section."

    @Test fun allArxivSpacingCasesAreIdentifiers() {
        listOf("2609.20448v1", "2609.20448 v1", "2609.20448v 1", "arXiv:2609.20448v 1").forEach {
            assertEquals("arXiv:2609.20448v1", analyze(line(it, 20f)).bestQuery)
        }
    }
    @Test fun multilineTitleCrossesMlKitBlocksButNotColumns() {
        val e = analyze(
            line("A comprehensive theory framework for", 100f, 32f, block = 0),
            line("perturbative calculations of δC in", 137f, 31f, block = 1),
            line("superallowed beta decays", 174f, 32f, block = 2),
            line(body, 350f, block = 3), line(body, 370f, block = 3))
        assertEquals("A comprehensive theory framework for perturbative calculations of δC in superallowed beta decays", e.titles.first().text)
    }
    @Test fun smallPrdHeaderAndTitleBothSurvive() {
        val e = analyze(line("PHYSICAL REVIEW D 108, 036027 (2023)", 15f, 11f),
            line("Shedding light on shadow generalized parton distributions", 100f, 28f),
            line(body, 300f), line(body, 320f))
        assertEquals("036027", e.journalCitations.single().locator)
        assertEquals("Shedding light on shadow generalized parton distributions", e.titles.single().text)
        assertTrue(e.bestQuery.startsWith("PHYSICAL REVIEW"))
    }
    @Test fun sameSizeParagraphsDoNotBecomeTitle() {
        val e = analyze(line(body, 30f), line(body, 50f), line(body, 70f))
        assertTrue(e.titles.isEmpty())
        assertEquals("", e.bestQuery)
    }
    @Test fun sectionHeadingIsNotTitle() {
        assertTrue(analyze(line("II. RESULTS AND DISCUSSION", 20f, 30f), line(body, 200f)).titles.isEmpty())
    }
    @Test fun referencesDoNotIdentifyCitedPapers() {
        val e = analyze(line("V. REFERENCES", 20f, 30f), line("Smith DOI: 10.1000/cited", 100f), line("arXiv:2609.20448", 130f))
        assertEquals("", e.bestQuery)
        assertTrue(e.fingerprints.isEmpty())
    }
    @Test fun multipleUnlabelledReferenceIdsAreNotChosenArbitrarily() {
        val e = analyze(line("10.1000/first", 20f), line("10.1000/second", 40f))
        assertEquals("", e.bestQuery)
    }
    @Test fun conferenceBannerDoesNotOutrankPaperTitle() {
        val e = analyze(line("INTERNATIONAL CONFERENCE ON NUCLEAR PHYSICS", 10f, 45f),
            line("Deep inelastic scattering from nuclei", 100f, 28f), line(body, 300f))
        assertEquals("Deep inelastic scattering from nuclei", e.titles.first().text)
    }
    @Test fun unrelatedColumnsAreNotConcatenated() {
        val e = analyze(line("Deep inelastic scattering", 100f, 30f, 40f, 460f, 0),
            line("Quantum gravity and entropy", 100f, 30f, 540f, 960f, 1),
            line("from atomic nuclei", 137f, 30f, 40f, 460f, 2), line(body, 300f))
        assertTrue(e.titles.any { it.text == "Deep inelastic scattering from atomic nuclei" })
        assertFalse(e.titles.any { it.text.contains("scattering Quantum") || it.text.contains("entropy from") })
    }
    @Test fun damagedMetadataDoesNotLeakToTitle() {
        assertEquals("", analyze(line("arXiv:2609.20448v l [nucl-th] 20 Sep 2026", 30f)).bestQuery)
    }
    @Test fun isolatedTitleCropRemainsSupported() {
        assertEquals("Deep Inelastic Scattering from Nuclei", analyze(line("Deep Inelastic Scattering from Nuclei", 20f, 20f)).bestQuery)
    }
    @Test fun referenceParagraphWithDoiDoesNotBeatTitle() {
        val e = analyze(line("Deep Inelastic Scattering from Nuclei", 20f, 30f),
            line("We compare our work with the calculations in DOI: 10.1000/cited and discuss the differences.", 300f))
        assertTrue(e.dois.isEmpty())
        assertEquals("Deep Inelastic Scattering from Nuclei", e.bestQuery)
    }

    @Test fun noisyJournalHeaderIsNotTheTitleAndFooterDoiWins() {
        val evidence = analyze(
            line("E0,801 g VaA 2YPHYSICAL REVIEW D 108, 036027 (2023)", 30f, 17f),
            line("Shedding light on shadow generalized parton distributions", 180f, 34f),
            line("Moffat, Freese, Cloet, Donohoe, Gamberg, Melnitchouk", 240f, 16f),
            line("The feasibility of extracting generalized parton distributions from data has recently been questioned.", 420f, 16f),
            line("DOI: 10.1103/PhysRevD.108.036027", 850f, 15f)
        )

        assertEquals(listOf("10.1103/physrevd.108.036027"), evidence.dois)
        assertEquals("10.1103/physrevd.108.036027", evidence.bestQuery)
        assertTrue(evidence.titles.any { it.text == "Shedding light on shadow generalized parton distributions" })
        assertFalse(evidence.titles.any { it.text.contains("PHYSICAL REVIEW") })
    }
}
