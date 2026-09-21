package com.example.papereyes.domain.evidence

data class ReferenceEvidence(
    val label: String?,
    val text: String,
    /** Bounded identity query chosen locally; this is the only value sent to a provider. */
    val query: String = text
)
