package com.example.papereyes.domain.evidence

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanSubjectTest {
    private val paperAndReference = DocumentEvidence(
        titles = listOf(TitleEvidence("The scanned journal paper", 2.0)),
        references = listOf(
            ReferenceEvidence(
                label = null,
                text = "A cited conference paper"
            )
        )
    )

    @Test
    fun journalPaperUsesDocumentIdentityAndIgnoresCitations() {
        assertTrue(paperAndReference.hasRequestedEvidence(ScanSubject.JOURNAL_PAPER))
        assertTrue(paperAndReference.referencesFor(ScanSubject.JOURNAL_PAPER).isEmpty())
    }

    @Test
    fun referenceListRequiresDetectedReferences() {
        assertTrue(paperAndReference.hasRequestedEvidence(ScanSubject.REFERENCES))
        assertFalse(
            DocumentEvidence(titles = paperAndReference.titles)
                .hasRequestedEvidence(ScanSubject.REFERENCES)
        )
    }

    @Test
    fun conferenceSlideFocusesOnCitationsInsteadOfSlideHeading() {
        assertTrue(paperAndReference.hasRequestedEvidence(ScanSubject.CONFERENCE_SLIDE))
        assertFalse(
            DocumentEvidence(titles = paperAndReference.titles)
                .hasRequestedEvidence(ScanSubject.CONFERENCE_SLIDE)
        )
    }
}
