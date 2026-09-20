package com.example.papereyes.domain.citation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalCitationParserTest {

    @Test
    fun parsesRepresentativeJournalFormats() {
        val cases = listOf(
            Triple("Phys. Rev. D 112, 034009 (2025)", "Physical Review D", "034009"),
            Triple("JHEP 09 (2024) 123", "Journal of High Energy Physics", "123"),
            Triple("N Engl J Med 2024;390:123-130", "The New England Journal of Medicine", "123-130"),
            Triple("JINST 19 (2024) P05001", "Journal of Instrumentation", "P05001")
        )

        for ((input, journal, locator) in cases) {
            val parsed = JournalCitationParser.parse(input)
            assertEquals(journal, parsed?.journal)
            assertEquals(locator, parsed?.locator)
            assertTrue((parsed?.confidence ?: 0.0) >= 0.95)
        }
    }

    @Test
    fun preservesIssueWhenPresent() {
        val parsed = JournalCitationParser.parse("Phys.Rev.D 112 (2025) 3, 034009")
        assertEquals("112", parsed?.volume)
        assertEquals("3", parsed?.issue)
        assertEquals(2025, parsed?.year)
        assertEquals("034009", parsed?.locator)
    }

    @Test
    fun rejectsStandaloneIdentifiersAndOrdinaryProse() {
        assertNull(JournalCitationParser.parse("10.1103/PhysRevD.112.034009"))
        assertNull(JournalCitationParser.parse("arXiv:2609.20448"))
        assertNull(JournalCitationParser.parse("This is an ordinary sentence about a paper."))
    }
}
