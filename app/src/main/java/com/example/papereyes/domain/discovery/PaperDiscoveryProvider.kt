package com.example.papereyes.domain.discovery

import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.evidence.TextFingerprint

enum class ProviderCapability {
    IDENTIFIER_LOOKUP, JOURNAL_LOOKUP, TITLE_SEARCH, AUTHOR_SEARCH,
    ABSTRACT_SEARCH, FULL_TEXT_PHRASE_SEARCH
}

data class RetrievalEvidence(val fingerprints: List<TextFingerprint>)

/** Returned text, not search rank, is eligible for local corroboration. */
data class PaperCandidate(
    val paper: Paper,
    val provider: String,
    val verificationTexts: List<String> = emptyList()
)

interface PaperDiscoveryProvider {
    val id: String
    val capabilities: Set<ProviderCapability>
    suspend fun searchEvidence(evidence: RetrievalEvidence): List<PaperCandidate>
}
