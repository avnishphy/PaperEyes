package com.example.papereyes.data.remote

import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.citation.JournalCitation


class InspireRepository(
    private val api: InspireApi = InspireClient.api,
    private val requestPolicy: ScholarlyRequestPolicy = ScholarlyRequestPolicy.shared
) {


    private suspend fun searchLiterature(query: String, size: Int): InspireSearchResponse =
        requestPolicy.request(ScholarlyProvider.INSPIRE) { api.searchLiterature(query, size) }

    /*
     * ================================================================
     * INTERNAL TYPES
     * ================================================================
     *
     * Keep these near the top of the class so every helper below can
     * reference them cleanly.
     */
    private enum class InspireJournalStyle {

        NORMAL,

        SERIES,

        JHEP_STYLE
    }


    private data class InspireJournalSpec(

        val legacyName: String,

        val metadataName: String,

        val series: String? = null,

        val style: InspireJournalStyle =
            InspireJournalStyle.NORMAL
    )


    private data class ScoredPaper(

        val paper: Paper,

        val score: Double
    )


    /*
     * ================================================================
     * PUBLIC CITATION SEARCH
     * ================================================================
     */
    suspend fun searchPaperByCitation(
        citation: JournalCitation
    ): List<Paper> {

        val journalSpec =
            inspireJournalSpec(
                citation
            )
                ?: return emptyList()


        /*
         * ============================================================
         * SEARCH 1 — INSPIRE JOURNAL SYNTAX
         * ============================================================
         *
         * Example:
         *
         * Phys.Rev.D 112 (2025) 3, 034009
         *
         * becomes:
         *
         * j Phys.Rev.,D112,034009 and jy 2025
         */
        val exactQuery =
            buildExactJournalQuery(
                citation =
                    citation,

                journalSpec =
                    journalSpec
            )


        if (
            exactQuery != null
        ) {

            val response =
                searchLiterature(
                    query =
                        exactQuery,

                    size =
                        10
                )


            val papers =
                rankHits(
                    citation =
                        citation,

                    hits =
                        response
                            .hits
                            ?.hits
                            .orEmpty()
                )


            if (
                papers.isNotEmpty()
            ) {

                return papers
            }
        }


        /*
         * ============================================================
         * SEARCH 2 — METADATA / ARTICLE ID
         * ============================================================
         */
        val metadataQuery =
            buildMetadataQuery(
                citation =
                    citation,

                journalSpec =
                    journalSpec,

                locatorField =
                    "artid"
            )


        if (
            metadataQuery != null
        ) {

            val response =
                searchLiterature(
                    query =
                        metadataQuery,

                    size =
                        10
                )


            val papers =
                rankHits(
                    citation =
                        citation,

                    hits =
                        response
                            .hits
                            ?.hits
                            .orEmpty()
                )


            if (
                papers.isNotEmpty()
            ) {

                return papers
            }
        }


        /*
         * ============================================================
         * SEARCH 3 — PAGE START
         * ============================================================
         */
        val pageQuery =
            buildMetadataQuery(
                citation =
                    citation,

                journalSpec =
                    journalSpec,

                locatorField =
                    "page_start"
            )


        if (
            pageQuery != null
        ) {

            val response =
                searchLiterature(
                    query =
                        pageQuery,

                    size =
                        10
                )


            val papers =
                rankHits(
                    citation =
                        citation,

                    hits =
                        response
                            .hits
                            ?.hits
                            .orEmpty()
                )


            if (
                papers.isNotEmpty()
            ) {

                return papers
            }
        }


        return emptyList()
    }


    /*
     * ================================================================
     * EXACT INSPIRE JOURNAL QUERY
     * ================================================================
     */
    private fun buildExactJournalQuery(
        citation: JournalCitation,
        journalSpec: InspireJournalSpec
    ): String? {

        val locator =
            citation
                .locator
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }
                ?: return null


        val publicationReference =
            when (
                journalSpec.style
            ) {

                /*
                 * Example:
                 *
                 * Physical Review D
                 *
                 * human:
                 *
                 *     Phys.Rev.D 112, 034009
                 *
                 * INSPIRE:
                 *
                 *     Phys.Rev.,D112,034009
                 */
                InspireJournalStyle.SERIES -> {

                    val volume =
                        citation
                            .volume
                            ?.trim()
                            ?.takeIf {

                                it.isNotBlank()
                            }
                            ?: return null


                    val series =
                        journalSpec.series
                            ?: return null


                    "${journalSpec.legacyName}," +
                            "$series$volume," +
                            locator
                }


                /*
                 * Example:
                 *
                 * JHEP 09 (2025) 123
                 *
                 * ->
                 *
                 * JHEP,2509,123
                 */
                InspireJournalStyle.JHEP_STYLE -> {

                    val year =
                        citation.year
                            ?: return null


                    val issue =
                        citation
                            .issue
                            ?.trim()
                            ?.padStart(
                                2,
                                '0'
                            )
                            ?: return null


                    val shortYear =
                        (
                                year %
                                        100
                                )
                            .toString()
                            .padStart(
                                2,
                                '0'
                            )


                    "${journalSpec.legacyName}," +
                            "$shortYear$issue," +
                            locator
                }


                /*
                 * Conventional journal:
                 *
                 * journal,volume,locator
                 */
                InspireJournalStyle.NORMAL -> {

                    val volume =
                        citation
                            .volume
                            ?.trim()
                            ?.takeIf {

                                it.isNotBlank()
                            }
                            ?: return null


                    "${journalSpec.legacyName}," +
                            "$volume," +
                            locator
                }
            }


        return citation
            .year
            ?.let { year ->

                "j $publicationReference and jy $year"
            }
            ?: "j $publicationReference"
    }


    /*
     * ================================================================
     * METADATA QUERY
     * ================================================================
     */
    private fun buildMetadataQuery(
        citation: JournalCitation,
        journalSpec: InspireJournalSpec,
        locatorField: String
    ): String? {

        val locator =
            citation
                .locator
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }
                ?: return null


        val parts =
            mutableListOf<String>()


        parts.add(
            "publication_info.journal_title:" +
                    "\"${journalSpec.metadataName}\""
        )


        citation
            .volume
            ?.takeIf {

                it.isNotBlank()
            }
            ?.let { volume ->

                parts.add(
                    "publication_info.journal_volume:" +
                            "\"$volume\""
                )
            }


        parts.add(
            "publication_info.$locatorField:" +
                    "\"$locator\""
        )


        citation
            .year
            ?.let { year ->

                parts.add(
                    "publication_info.year:$year"
                )
            }


        return parts.joinToString(
            " and "
        )
    }


    /*
     * ================================================================
     * RANK INSPIRE RESULTS
     * ================================================================
     */
    private fun rankHits(
        citation: JournalCitation,
        hits: List<InspireHit>
    ): List<Paper> {

        if (
            hits.isEmpty()
        ) {

            return emptyList()
        }


        return hits
            .mapNotNull { hit ->

                val metadata =
                    hit.metadata
                        ?: return@mapNotNull null


                val publications =
                    metadata
                        .publicationInfo
                        ?: emptyList()


                val bestPublication =
                    publications
                        .maxByOrNull { publication ->

                            publicationScore(
                                citation =
                                    citation,

                                publication =
                                    publication
                            )
                        }


                val score =
                    bestPublication
                        ?.let { publication ->

                            publicationScore(
                                citation =
                                    citation,

                                publication =
                                    publication
                            )
                        }
                        ?: 0.0


                if (
                    score < 0.65 ||
                    bestPublication == null ||
                    !hasDistinctiveCitationIdentity(
                        citation = citation,
                        publication = bestPublication
                    )
                ) {
                    return@mapNotNull null
                }


                val paper =
                    metadataToPaper(
                        metadata =
                            metadata,

                        publication =
                            bestPublication,

                        fallbackUrl =
                            hit
                                .links
                                ?.self
                    )
                        ?: return@mapNotNull null


                ScoredPaper(
                    paper =
                        paper,

                    score =
                        score
                )
            }
            .sortedByDescending {

                it.score
            }
            .distinctBy {

                it.paper.doi
                    ?.lowercase()

                    ?: it.paper.title
                        .lowercase()
            }
            .take(
                3
            )
            .map {

                it.paper
            }
    }


    /*
     * ================================================================
     * PUBLICATION MATCH SCORE
     * ================================================================
     */
    private fun publicationScore(
        citation: JournalCitation,
        publication: InspirePublicationInfo
    ): Double {

        var matched =
            0.0


        var possible =
            0.0


        /*
         * ------------------------------------------------------------
         * JOURNAL
         * ------------------------------------------------------------
         */
        publication
            .journalTitle
            ?.takeIf {

                it.isNotBlank()
            }
            ?.let { actualJournal ->

                possible +=
                    0.20


                if (
                    journalMatches(
                        citation =
                            citation,

                        actual =
                            actualJournal
                    )
                ) {

                    matched +=
                        0.20
                }
            }


        /*
         * ------------------------------------------------------------
         * VOLUME
         * ------------------------------------------------------------
         */
        if (
            citation.volume != null &&
            !publication
                .journalVolume
                .isNullOrBlank()
        ) {

            possible +=
                0.25


            if (
                normalizeField(
                    citation.volume
                ) ==
                normalizeField(
                    publication.journalVolume
                )
            ) {

                matched +=
                    0.25
            }
        }


        /*
         * ------------------------------------------------------------
         * YEAR
         * ------------------------------------------------------------
         */
        if (
            citation.year != null &&
            publication.year !=
            null
        ) {

            possible +=
                0.20


            if (
                citation.year ==
                publication.year
            ) {

                matched +=
                    0.20
            }
        }


        /*
         * ------------------------------------------------------------
         * ISSUE
         * ------------------------------------------------------------
         */
        if (
            citation.issue != null &&
            !publication
                .journalIssue
                .isNullOrBlank()
        ) {

            possible +=
                0.05


            if (
                normalizeField(
                    citation.issue
                ) ==
                normalizeField(
                    publication.journalIssue
                )
            ) {

                matched +=
                    0.05
            }
        }


        /*
         * ------------------------------------------------------------
         * ARTICLE / PAGE
         * ------------------------------------------------------------
         */
        citation
            .locator
            ?.let { expectedLocator ->

                val actualLocators =
                    listOfNotNull(
                        publication.artid,
                        publication.pageStart
                    )
                        .filter {

                            it.isNotBlank()
                        }


                if (
                    actualLocators.isNotEmpty()
                ) {

                    possible +=
                        0.30


                    val locatorMatched =
                        actualLocators.any { actual ->

                            locatorMatches(
                                expected =
                                    expectedLocator,

                                actual =
                                    actual
                            )
                        }


                    if (
                        locatorMatched
                    ) {

                        matched +=
                            0.30
                    }
                }
            }


        if (
            possible ==
            0.0
        ) {

            return 0.0
        }


        return (
                matched /
                        possible
                )
            .coerceIn(
                0.0,
                1.0
            )
    }

    /**
     * A high normalized percentage is not enough when only weak fields were
     * available. Require journal + volume + locator, and no supplied
     * contradictory field. Article numbers repeat between volumes, and
     * journal/volume/year alone is not paper identity.
     */
    internal fun hasDistinctiveCitationIdentity(
        citation: JournalCitation,
        publication: InspirePublicationInfo
    ): Boolean {
        val journalMatched = publication.journalTitle
            ?.takeIf(String::isNotBlank)
            ?.let { journalMatches(citation, it) }
            ?: false

        val volumeMatched =
            citation.volume != null &&
                !publication.journalVolume.isNullOrBlank() &&
                normalizeField(citation.volume) == normalizeField(publication.journalVolume)

        val yearMatched =
            citation.year != null &&
                publication.year != null &&
                citation.year == publication.year

        val locatorMatched = citation.locator?.let { expected ->
            listOfNotNull(publication.artid, publication.pageStart)
                .filter(String::isNotBlank)
                .any { locatorMatches(expected, it) }
        } ?: false

        val contradiction =
            (!publication.journalTitle.isNullOrBlank() && !journalMatched) ||
            (citation.volume != null && !publication.journalVolume.isNullOrBlank() && !volumeMatched) ||
            (citation.year != null && publication.year != null && !yearMatched) ||
            (citation.issue != null && !publication.journalIssue.isNullOrBlank() &&
                normalizeField(citation.issue) != normalizeField(publication.journalIssue))
        return locatorMatched && journalMatched && volumeMatched && !contradiction
    }



    /*
     * ================================================================
     * JOURNAL MATCH
     * ================================================================
     */
    private fun journalMatches(
        citation: JournalCitation,
        actual: String
    ): Boolean {

        val expectedSpec =
            inspireJournalSpec(
                citation
            )


        val possibleNames =
            buildList {

                expectedSpec
                    ?.metadataName
                    ?.let {

                        add(it)
                    }


                expectedSpec
                    ?.legacyName
                    ?.let {

                        add(it)
                    }


                add(
                    citation.journal
                )


                add(
                    citation.journalRaw
                )
            }


        val normalizedActual =
            normalizeJournal(
                actual
            )


        return possibleNames.any { expected ->

            normalizeJournal(
                expected
            ) ==
                    normalizedActual
        }
    }


    /*
     * ================================================================
     * JOURNAL DEFINITIONS
     * ================================================================
     */
    private fun inspireJournalSpec(
        citation: JournalCitation
    ): InspireJournalSpec? {

        return when (
            citation.journal
        ) {

            /*
             * APS
             */
            "Physical Review Letters" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Rev.Lett.",

                    metadataName =
                        "Phys.Rev.Lett."
                )


            "Physical Review A" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Rev.",

                    metadataName =
                        "Phys.Rev.A",

                    series =
                        "A",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Physical Review B" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Rev.",

                    metadataName =
                        "Phys.Rev.B",

                    series =
                        "B",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Physical Review C" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Rev.",

                    metadataName =
                        "Phys.Rev.C",

                    series =
                        "C",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Physical Review D" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Rev.",

                    metadataName =
                        "Phys.Rev.D",

                    series =
                        "D",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Physical Review E" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Rev.",

                    metadataName =
                        "Phys.Rev.E",

                    series =
                        "E",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Reviews of Modern Physics" ->

                InspireJournalSpec(
                    legacyName =
                        "Rev.Mod.Phys.",

                    metadataName =
                        "Rev.Mod.Phys."
                )


            /*
             * Elsevier / nuclear
             */
            "Physics Letters A" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Lett.",

                    metadataName =
                        "Phys.Lett.A",

                    series =
                        "A",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Physics Letters B" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Lett.",

                    metadataName =
                        "Phys.Lett.B",

                    series =
                        "B",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Nuclear Physics A" ->

                InspireJournalSpec(
                    legacyName =
                        "Nucl.Phys.",

                    metadataName =
                        "Nucl.Phys.A",

                    series =
                        "A",

                    style =
                        InspireJournalStyle.SERIES
                )


            "Nuclear Physics B" ->

                InspireJournalSpec(
                    legacyName =
                        "Nucl.Phys.",

                    metadataName =
                        "Nucl.Phys.B",

                    series =
                        "B",

                    style =
                        InspireJournalStyle.SERIES
                )


            /*
             * EPJ
             */
            "The European Physical Journal A" ->

                InspireJournalSpec(
                    legacyName =
                        "Eur.Phys.J.A",

                    metadataName =
                        "Eur.Phys.J.A"
                )


            "The European Physical Journal C" ->

                InspireJournalSpec(
                    legacyName =
                        "Eur.Phys.J.C",

                    metadataName =
                        "Eur.Phys.J.C"
                )


            /*
             * JHEP / JCAP
             */
            "Journal of High Energy Physics" ->

                InspireJournalSpec(
                    legacyName =
                        "JHEP",

                    metadataName =
                        "JHEP",

                    style =
                        InspireJournalStyle.JHEP_STYLE
                )


            "Journal of Cosmology and Astroparticle Physics" ->

                InspireJournalSpec(
                    legacyName =
                        "JCAP",

                    metadataName =
                        "JCAP",

                    style =
                        InspireJournalStyle.JHEP_STYLE
                )


            /*
             * Other HEP journals
             */
            "Journal of Instrumentation" ->

                InspireJournalSpec(
                    legacyName =
                        "JINST",

                    metadataName =
                        "JINST"
                )


            "Journal of Physics G" ->

                InspireJournalSpec(
                    legacyName =
                        "J.Phys.G",

                    metadataName =
                        "J.Phys.G"
                )


            "Chinese Physics C" ->

                InspireJournalSpec(
                    legacyName =
                        "Chin.Phys.C",

                    metadataName =
                        "Chin.Phys.C"
                )


            "Physics Reports" ->

                InspireJournalSpec(
                    legacyName =
                        "Phys.Rept.",

                    metadataName =
                        "Phys.Rept."
                )


            "Proceedings of Science" ->

                InspireJournalSpec(
                    legacyName =
                        "PoS",

                    metadataName =
                        "PoS"
                )


            /*
             * Unknown but abbreviation-looking journal.
             */
            else -> {

                val raw =
                    citation
                        .journalRaw
                        .trim()


                if (
                    raw.contains(".") &&
                    raw.length >=
                    4
                ) {

                    InspireJournalSpec(
                        legacyName =
                            raw,

                        metadataName =
                            raw
                    )

                } else {

                    null
                }
            }
        }
    }


    /*
     * ================================================================
     * INSPIRE METADATA → PAPER
     * ================================================================
     */
    private fun metadataToPaper(
        metadata: InspireMetadata,
        publication: InspirePublicationInfo?,
        fallbackUrl: String?
    ): Paper? {

        val title =
            metadata
                .titles
                ?.firstOrNull()
                ?.title
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }
                ?: return null


        val authors =
            metadata
                .authors
                ?.mapNotNull { author ->

                    author
                        .fullName
                        ?.trim()
                        ?.takeIf {

                            it.isNotBlank()
                        }
                }
                ?.joinToString(
                    ", "
                )
                ?: ""


        val doi =
            metadata
                .dois
                ?.firstOrNull()
                ?.value
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }


        val arxiv =
            metadata
                .arxivEprints
                ?.firstOrNull()
                ?.value
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }


        val url =
            when {

                doi != null ->

                    "https://doi.org/$doi"


                arxiv != null ->

                    "https://arxiv.org/abs/$arxiv"


                else ->

                    fallbackUrl
            }


        return Paper(
            title =
                title,

            authors =
                authors,

            year =
                publication
                    ?.year,

            doi =
                doi,

            url =
                url
        )
    }


    /*
     * ================================================================
     * NORMALIZATION
     * ================================================================
     */
    private fun normalizeJournal(
        value: String
    ): String {

        return value
            .lowercase()
            .replace(
                Regex(
                    """[^a-z0-9]+"""
                ),
                ""
            )
    }


    private fun normalizeField(
        value: String?
    ): String {

        return value
            ?.uppercase()
            ?.replace(
                Regex(
                    """[^A-Z0-9]+"""
                ),
                ""
            )
            ?: ""
    }


    private fun locatorMatches(
        expected: String,
        actual: String
    ): Boolean {

        val expectedClean =
            normalizeField(
                expected
            )


        val actualClean =
            normalizeField(
                actual
            )


        if (
            expectedClean ==
            actualClean
        ) {

            return true
        }


        /*
         * Leading zero difference.
         *
         * 034009
         * 34009
         */
        if (
            expectedClean.all {

                it.isDigit()
            } &&
            actualClean.all {

                it.isDigit()
            }
        ) {

            val expectedNumber =
                expectedClean
                    .trimStart(
                        '0'
                    )
                    .ifBlank {

                        "0"
                    }


            val actualNumber =
                actualClean
                    .trimStart(
                        '0'
                    )
                    .ifBlank {

                        "0"
                    }


            return expectedNumber ==
                    actualNumber
        }


        return false
    }
}