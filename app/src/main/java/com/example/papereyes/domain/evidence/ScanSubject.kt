package com.example.papereyes.domain.evidence

/** User-provided context that determines which OCR evidence is actionable. */
enum class ScanSubject {
    JOURNAL_PAPER,
    REFERENCES,
    CONFERENCE_SLIDE;

    val isReferenceFocused: Boolean
        get() = this != JOURNAL_PAPER
}

fun DocumentEvidence.referencesFor(subject: ScanSubject): List<ReferenceEvidence> =
    if (subject.isReferenceFocused) references else emptyList()

fun DocumentEvidence.hasRequestedEvidence(subject: ScanSubject): Boolean =
    if (subject.isReferenceFocused) {
        references.isNotEmpty()
    } else {
        bestQuery.isNotBlank() || fingerprints.isNotEmpty()
    }
