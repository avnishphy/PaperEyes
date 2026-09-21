package com.example.papereyes.domain.evidence

import com.example.papereyes.domain.citation.JournalCitation

/** Geometry is in the upright OCR image's pixel coordinate system, not preview coordinates. */
data class OcrLine(
    val text: String, val left: Float, val top: Float,
    val right: Float, val bottom: Float, val blockId: Int = 0
) {
    val height: Float get() = bottom - top
    val width: Float get() = right - left
}

data class TitleEvidence(val text: String, val score: Double)
data class TextFingerprint(val text: String, val sourceGroup: Int, val score: Double, val caption: Boolean = false)
enum class LayoutRole { TITLE_LIKE, BODY, REFERENCES, MIXED, UNKNOWN }
data class LayoutProfile(val role: LayoutRole = LayoutRole.UNKNOWN, val bodyLineHeight: Float? = null, val hasGeometry: Boolean = false)

/** Evidence is local-only unless a selected, bounded query is sent to a provider. */
data class DocumentEvidence(
    val dois: List<String> = emptyList(),
    val arxivIds: List<String> = emptyList(),
    val journalCitations: List<JournalCitation> = emptyList(),
    val titles: List<TitleEvidence> = emptyList(),
    val fingerprints: List<TextFingerprint> = emptyList(),
    val layout: LayoutProfile = LayoutProfile()
) {
    val bestQuery: String get() = when {
        dois.size == 1 -> dois.single()
        arxivIds.size == 1 -> "arXiv:${arxivIds.single()}"
        journalCitations.size == 1 -> journalCitations.single().raw
        else -> titles.maxByOrNull { it.score }?.text.orEmpty()
    }
    val hasStructuredEvidence: Boolean get() = dois.size == 1 || arxivIds.size == 1 || journalCitations.size == 1
}
