package com.example.papereyes.ocr

import com.example.papereyes.domain.evidence.OcrLine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceCropPlannerTest {

    @Test
    fun cropsToTheDetectedBibliographyEnvelopeAndIgnoresSingleKeyLabels() {
        val lines = listOf(
            line("Z", 100f, 20f, 180f, 50f),
            line("[53] B. Kriesten, S. Liuti, and A. Meyer, Phys. Lett. B 829, 137051 (2022).", 120f, 620f, 900f, 650f),
            line("[54] J. Grigsby and B. Kriesten, Phys. Rev. D 104, 016001 (2021).", 120f, 660f, 900f, 690f),
            line("[67] S. V. Goloskokov and P. Kroll, Eur. Phys. J. C 65, 137 (2010).", 1050f, 620f, 1810f, 650f),
            line("[68] I. V. Musatov and A. V. Radyushkin, Phys. Rev. D 61, 074027 (2000).", 1050f, 660f, 1810f, 690f)
        )

        val crop = ReferenceCropPlanner.plan(lines, 2000, 2600)

        assertNotNull(crop)
        crop!!
        assertTrue(crop.left > 0.02f)
        assertTrue(crop.right < 0.98f)
        assertTrue(crop.top > 0.15f)
        assertTrue(crop.bottom < 0.40f)
    }

    @Test
    fun refusesToCropFromOneRecognizedLine() {
        val crop = ReferenceCropPlanner.plan(
            listOf(line("Only one readable citation", 100f, 100f, 900f, 130f)),
            1000,
            1400
        )

        assertNull(crop)
    }

    private fun line(text: String, left: Float, top: Float, right: Float, bottom: Float) =
        OcrLine(text, left, top, right, bottom)
}
