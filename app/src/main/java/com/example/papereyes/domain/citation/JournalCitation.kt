package com.example.papereyes.domain.citation


data class JournalCitation(
    val raw: String,

    /*
     * Canonical name when PaperEyes recognizes the journal.
     *
     * "Phys.Rev.D" -> "Physical Review D"
     */
    val journal: String,

    /*
     * Journal text as it appeared in the citation.
     */
    val journalRaw: String,

    val volume: String?,

    val issue: String?,

    val year: Int?,

    /*
     * Page, page range, article number, or e-locator.
     *
     * Examples:
     * 034009
     * 123-130
     * P05001
     * L012345
     * e0299999
     * eabc1234
     */
    val locator: String?,

    /*
     * Parser confidence only.
     *
     * This is NOT Crossref match confidence.
     */
    val confidence: Double
) {

    fun toBibliographicQuery(): String {

        return buildList {

            add(journal)

            volume
                ?.takeIf { it.isNotBlank() }
                ?.let { add(it) }

            issue
                ?.takeIf { it.isNotBlank() }
                ?.let { add(it) }

            year
                ?.let {
                    add(it.toString())
                }

            locator
                ?.takeIf { it.isNotBlank() }
                ?.let { add(it) }

        }.joinToString(" ")
    }
}