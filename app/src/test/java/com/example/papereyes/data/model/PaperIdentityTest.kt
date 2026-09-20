package com.example.papereyes.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaperIdentityTest {

    @Test
    fun doiFormsProduceSameIdentity() {
        assertEquals(
            buildPaperIdentityKey("10.1000/ABC", "Title", 2024),
            buildPaperIdentityKey("https://doi.org/10.1000/abc", "Different metadata title", 2025)
        )
    }

    @Test
    fun titleFallbackIncludesKnownYear() {
        assertNotEquals(
            buildPaperIdentityKey(null, "A Shared Title", 2023),
            buildPaperIdentityKey(null, "A Shared Title", 2024)
        )
    }

    @Test
    fun titleFallbackNormalizesWhitespaceAndCase() {
        assertEquals(
            buildPaperIdentityKey(null, "  Deep   Inelastic Scattering ", 2024),
            buildPaperIdentityKey(null, "deep inelastic scattering", 2024)
        )
    }
}
