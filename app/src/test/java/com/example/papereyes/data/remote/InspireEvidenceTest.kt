package com.example.papereyes.data.remote

import com.example.papereyes.domain.citation.JournalCitation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspireEvidenceTest {
    private val repository = InspireRepository(
        api = object : InspireApi {
            override suspend fun searchLiterature(query: String, size: Int): InspireSearchResponse =
                error("not used")
        }
    )

    private val citation = JournalCitation(
        raw = "Phys.Rev.D 112 (2025) 034009",
        journal = "Physical Review D",
        journalRaw = "Phys.Rev.D",
        volume = "112",
        issue = null,
        year = 2025,
        locator = "034009",
        confidence = 0.99
    )

    @Test
    fun journalAndYearAloneAreNotDistinctiveEnough() {
        assertFalse(
            repository.hasDistinctiveCitationIdentity(
                citation,
                InspirePublicationInfo(
                    journalTitle = "Phys.Rev.D",
                    journalVolume = null,
                    journalIssue = null,
                    year = 2025,
                    artid = null,
                    pageStart = null,
                    pageEnd = null
                )
            )
        )
    }

    @Test
    fun locatorPlusJournalIsDistinctive() {
        assertTrue(
            repository.hasDistinctiveCitationIdentity(
                citation,
                InspirePublicationInfo(
                    journalTitle = "Phys.Rev.D",
                    journalVolume = null,
                    journalIssue = null,
                    year = null,
                    artid = "034009",
                    pageStart = null,
                    pageEnd = null
                )
            )
        )
    }

    @Test
    fun journalVolumeYearCombinationIsDistinctiveWithoutLocator() {
        assertTrue(
            repository.hasDistinctiveCitationIdentity(
                citation,
                InspirePublicationInfo(
                    journalTitle = "Phys.Rev.D",
                    journalVolume = "112",
                    journalIssue = null,
                    year = 2025,
                    artid = null,
                    pageStart = null,
                    pageEnd = null
                )
            )
        )
    }
}
