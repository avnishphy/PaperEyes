package com.example.papereyes.ui.live

import com.example.papereyes.data.model.Paper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScanUtilsTest {

    private fun paper(title: String) = Paper(
        title = title,
        authors = "A. Author",
        year = 2025,
        doi = null,
        url = null
    )

    @Test
    fun ocrTypoStillMatchesStrongly() {
        val score = titleSimilarity(
            "Deep inelastic scatterlng from nuclei",
            "Deep Inelastic Scattering from Nuclei"
        )
        assertTrue(score > 0.75)
    }

    @Test
    fun unrelatedTitlesRemainWeak() {
        val score = titleSimilarity(
            "Deep inelastic scattering from nuclei",
            "Quantum gravity and black hole entropy"
        )
        assertTrue(score < 0.25)
    }

    @Test
    fun nearTieIsNotAutoAccepted() {
        val papers = listOf(
            paper("Observation of a New Particle in Proton Collisions"),
            paper("Observation of a New Particle in Proton Collisions")
        )
        assertNull(
            findConfidentPaperMatch(
                query = "Observation of a New Particle in Proton Collisions",
                papers = papers,
                minimumScore = 0.60,
                minimumMargin = 0.08
            )
        )
    }

    @Test
    fun clearWinnerIsAccepted() {
        val expected = paper("Deep Inelastic Scattering from Nuclei")
        val result = findConfidentPaperMatch(
            query = "Deep inelastic scatterlng from nuclei",
            papers = listOf(expected, paper("Quantum Gravity and Black Hole Entropy"))
        )
        assertEquals(expected.title, result?.paper?.title)
    }
}
