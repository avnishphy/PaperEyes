package com.example.papereyes.data.remote

import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.citation.JournalAliases
import com.example.papereyes.domain.citation.JournalCitation
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException


class PaperRepository(
    private val api: CrossrefApi = CrossrefClient.api
) {


    companion object {

        /*
         * Citation matching is structured and therefore much safer
         * than a generic title result.
         */
        private const val MIN_NORMALIZED_CITATION_SCORE =
            0.72


        private const val CITATION_SEARCH_ROWS =
            5


        private const val TITLE_SEARCH_ROWS =
            5


        /*
         * Crossref's public pool currently permits one list/query request
         * per second. Serializing list calls here prevents rapid OCR/manual
         * retries from producing avoidable HTTP 429 responses.
         */
        private const val MIN_LIST_REQUEST_INTERVAL_MS =
            1_050L


        // Do not keep a foreground lookup blocked for an arbitrarily long
        // server backoff. Longer Retry-After values are surfaced instead of
        // retrying earlier than the server requested.
        private const val MAX_RETRY_AFTER_MS =
            10_000L


        private const val HIGH_CONFIDENCE_SCORE =
            0.95


        private val crossrefListMutex =
            Mutex()


        private var lastCrossrefListRequestNanos =
            0L
    }


    /*
     * ================================================================
     * NORMAL TITLE / OCR SEARCH
     * ================================================================
     */
    suspend fun searchPaper(
        query: String
    ): List<Paper> {

        val response =
            crossrefListRequest {

                api.searchWorks(
                    query =
                        query,

                    rows =
                        TITLE_SEARCH_ROWS
                )
            }


        return response
            .message
            ?.items
            .orEmpty()
            .mapNotNull { item ->

                item.toPaper()
            }
    }


    /*
     * ================================================================
     * JOURNAL CITATION SEARCH
     * ================================================================
     *
     * Instead of trusting ONE Crossref query, try several strategies.
     *
     *
     * Example input:
     *
     * Phys.Rev.D 112 (2025) 3, 034009
     *
     *
     * SEARCH A
     * --------
     *
     * Raw citation:
     *
     *     Phys.Rev.D 112 (2025) 3, 034009
     *
     *
     * SEARCH B
     * --------
     *
     * Canonical citation:
     *
     *     Physical Review D 112 3 2025 034009
     *
     *
     * SEARCH C
     * --------
     *
     * Structured journal search:
     *
     *     container = Physical Review D
     *     query     = 112 034009
     *     year      = 2025
     *
     *
     * SEARCH D
     * --------
     *
     * Exact article-number filter when possible:
     *
     *     article-number = 034009
     *     journal        = Physical Review D
     *     year           = 2025
     *
     *
     * Results are merged and then independently verified.
     */
    suspend fun searchPaperByCitation(
        citation: JournalCitation
    ): List<Paper> {

        val candidates =
            mutableListOf<CrossrefItem>()


        val yearFilter =
            buildYearFilter(
                citation.year
            )


        val locator =
            citation.locator


        /*
         * ============================================================
         * SEARCH 1 — MOST SPECIFIC: ARTICLE NUMBER + JOURNAL + YEAR
         * ============================================================
         *
         * This is the cheapest/highest-value request for article-number
         * citations such as PRD 112, 034009 or JINST P05001.
         */
        if (
            !locator.isNullOrBlank() &&
            !locator.contains("-")
        ) {

            val articleFilter =
                buildFilter(
                    year =
                        citation.year,

                    articleNumber =
                        locator
                )


            val response =
                crossrefListRequest {

                    api.searchWorks(
                        query =
                            citation.volume
                                ?.takeIf {
                                    it.isNotBlank()
                                },

                        containerTitle =
                            citation.journal,

                        filter =
                            articleFilter,

                        rows =
                            CITATION_SEARCH_ROWS
                    )
                }


            candidates.addAll(
                response.message?.items.orEmpty()
            )


            highConfidenceCitationResult(
                citation =
                    citation,

                candidates =
                    candidates
            )?.let {

                return it
            }
        }


        /*
         * ============================================================
         * SEARCH 2 — STRUCTURED JOURNAL SEARCH
         * ============================================================
         */
        val structuredQuery =
            listOfNotNull(
                citation.volume,
                citation.locator
            )
                .joinToString(
                    " "
                )
                .trim()


        if (structuredQuery.isNotBlank()) {

            val response =
                crossrefListRequest {

                    api.searchWorks(
                        query =
                            structuredQuery,

                        containerTitle =
                            citation.journal,

                        filter =
                            yearFilter,

                        rows =
                            CITATION_SEARCH_ROWS
                    )
                }


            candidates.addAll(
                response.message?.items.orEmpty()
            )


            highConfidenceCitationResult(
                citation =
                    citation,

                candidates =
                    candidates
            )?.let {

                return it
            }
        }


        /*
         * ============================================================
         * SEARCH 3 — RAW CITATION
         * ============================================================
         */
        val rawResponse =
            crossrefListRequest {

                api.searchWorks(
                    query =
                        citation.raw,

                    filter =
                        yearFilter,

                    rows =
                        CITATION_SEARCH_ROWS
                )
            }


        candidates.addAll(
            rawResponse.message?.items.orEmpty()
        )


        highConfidenceCitationResult(
            citation =
                citation,

            candidates =
                candidates
        )?.let {

            return it
        }


        /*
         * ============================================================
         * SEARCH 4 — CANONICAL CITATION FALLBACK
         * ============================================================
         *
         * Only run when it is meaningfully different from the OCR/raw
         * citation and the previous strategies did not already establish a
         * high-confidence match.
         */
        val canonicalQuery =
            citation.toBibliographicQuery()


        if (
            normalizeQuery(
                canonicalQuery
            ) !=
            normalizeQuery(
                citation.raw
            )
        ) {

            val response =
                crossrefListRequest {

                    api.searchWorks(
                        query =
                            canonicalQuery,

                        filter =
                            yearFilter,

                        rows =
                            CITATION_SEARCH_ROWS
                    )
                }


            candidates.addAll(
                response.message?.items.orEmpty()
            )
        }


        return rankCitationCandidates(
            citation =
                citation,

            candidates =
                candidates
        )
            .let(
                ::papersFromRankedCitationCandidates
            )
    }


    private fun highConfidenceCitationResult(
        citation: JournalCitation,
        candidates: List<CrossrefItem>
    ): List<Paper>? {

        val ranked =
            rankCitationCandidates(
                citation =
                    citation,

                candidates =
                    candidates
            )


        val best =
            ranked.firstOrNull()
                ?: return null


        val evidence =
            best.evidence


        val strongLocatorIdentity =
            evidence.locatorMatched &&
                    listOf(
                        evidence.journalMatched,
                        evidence.volumeMatched,
                        evidence.yearMatched
                    )
                        .count { it } >= 2


        val strongBibliographicIdentity =
            evidence.journalMatched &&
                    evidence.volumeMatched &&
                    evidence.yearMatched


        if (
            evidence.score < HIGH_CONFIDENCE_SCORE ||
            !(strongLocatorIdentity || strongBibliographicIdentity)
        ) {

            return null
        }


        return papersFromRankedCitationCandidates(
            ranked
        )
    }


    private fun rankCitationCandidates(
        citation: JournalCitation,
        candidates: List<CrossrefItem>
    ): List<CitationCandidate> {

        val uniqueCandidates =
            candidates
                .distinctBy { item ->

                    item.doi
                        ?.lowercase()
                        ?.trim()

                        ?: item.title
                            ?.firstOrNull()
                            ?.lowercase()
                            ?.trim()

                        ?: item.hashCode()
                            .toString()
                }


        return uniqueCandidates
            .mapNotNull { item ->

                val paper =
                    item.toPaper()
                        ?: return@mapNotNull null


                val evidence =
                    calculateCitationEvidence(
                        citation =
                            citation,

                        item =
                            item
                    )


                CitationCandidate(
                    paper =
                        paper,

                    evidence =
                        evidence
                )
            }
            .filter { candidate ->

                isAcceptableCitationMatch(
                    candidate.evidence
                )
            }
            .sortedByDescending { candidate ->

                candidate.evidence.score
            }
    }


    private fun papersFromRankedCitationCandidates(
        ranked: List<CitationCandidate>
    ): List<Paper> {

        if (ranked.isEmpty()) {
            return emptyList()
        }


        val bestScore =
            ranked.first().evidence.score


        return ranked
            .filter { candidate ->

                candidate.evidence.score >=
                        bestScore - 0.08
            }
            .take(3)
            .map { candidate ->

                candidate.paper
            }
    }


    /*
     * ================================================================
     * DOI LOOKUP
     * ================================================================
     */
    suspend fun getPaperByDoi(
        doi: String
    ): Paper {

        val response =
            crossrefSingleRequest {

                api.getWorkByDoi(
                    doi
                )
            }


        return response
            .message
            ?.toPaper()
            ?: throw Exception(
                "Crossref returned incomplete paper metadata."
            )
    }


    /*
     * ================================================================
     * CITATION EVIDENCE
     * ================================================================
     *
     * CRITICAL DIFFERENCE FROM THE PREVIOUS VERSION:
     *
     * If Crossref does NOT provide a field, that field is not counted
     * against the result.
     *
     * Missing metadata != wrong metadata.
     *
     *
     * Available weights:
     *
     * journal     0.30
     * volume      0.20
     * year        0.20
     * locator     0.25
     * issue       0.05
     *
     * total       1.00
     */
    private fun calculateCitationEvidence(
        citation: JournalCitation,
        item: CrossrefItem
    ): CitationEvidence {

        var matchedWeight =
            0.0


        var comparableWeight =
            0.0


        /*
         * ------------------------------------------------------------
         * JOURNAL
         * ------------------------------------------------------------
         */
        val journalNames =
            itemJournalNames(
                item
            )


        var journalMatched =
            false


        if (
            journalNames.isNotEmpty()
        ) {

            comparableWeight +=
                0.30


            val similarity =
                journalSimilarity(
                    citationJournal =
                        citation.journal,

                    item =
                        item
                )


            matchedWeight +=
                0.30 *
                        similarity


            journalMatched =
                similarity >=
                        0.65
        }


        /*
         * ------------------------------------------------------------
         * VOLUME
         * ------------------------------------------------------------
         */
        var volumeMatched =
            false


        if (
            citation.volume != null &&
            !item.volume.isNullOrBlank()
        ) {

            comparableWeight +=
                0.20


            volumeMatched =
                normalizedField(
                    citation.volume
                ) ==
                        normalizedField(
                            item.volume
                        )


            if (
                volumeMatched
            ) {

                matchedWeight +=
                    0.20
            }
        }


        /*
         * ------------------------------------------------------------
         * YEAR
         * ------------------------------------------------------------
         */
        val crossrefYear =
            item.publicationYear()


        var yearMatched =
            false


        if (
            citation.year != null &&
            crossrefYear != null
        ) {

            comparableWeight +=
                0.20


            yearMatched =
                citation.year ==
                        crossrefYear


            if (
                yearMatched
            ) {

                matchedWeight +=
                    0.20
            }
        }


        /*
         * ------------------------------------------------------------
         * LOCATOR
         * ------------------------------------------------------------
         */
        val crossrefLocators =
            buildList {

                item.page
                    ?.takeIf {

                        it.isNotBlank()
                    }
                    ?.let {

                        add(it)
                    }


                item.articleNumber
                    ?.takeIf {

                        it.isNotBlank()
                    }
                    ?.let {

                        add(it)
                    }
            }


        var locatorMatched =
            false


        if (
            citation.locator != null &&
            crossrefLocators.isNotEmpty()
        ) {

            comparableWeight +=
                0.25


            val bestLocatorSimilarity =
                crossrefLocators
                    .maxOfOrNull { actual ->

                        locatorSimilarity(
                            expected =
                                citation.locator,

                            actual =
                                actual
                        )
                    }
                    ?: 0.0


            locatorMatched =
                bestLocatorSimilarity >=
                        0.85


            matchedWeight +=
                0.25 *
                        bestLocatorSimilarity
        }


        /*
         * ------------------------------------------------------------
         * ISSUE
         * ------------------------------------------------------------
         */
        var issueMatched =
            false


        if (
            citation.issue != null &&
            !item.issue.isNullOrBlank()
        ) {

            comparableWeight +=
                0.05


            issueMatched =
                normalizedField(
                    citation.issue
                ) ==
                        normalizedField(
                            item.issue
                        )


            if (
                issueMatched
            ) {

                matchedWeight +=
                    0.05
            }
        }


        val score =
            if (
                comparableWeight >
                0.0
            ) {

                matchedWeight /
                        comparableWeight

            } else {

                0.0
            }


        return CitationEvidence(
            score =
                score.coerceIn(
                    0.0,
                    1.0
                ),

            comparableWeight =
                comparableWeight,

            matchedWeight =
                matchedWeight,

            journalMatched =
                journalMatched,

            volumeMatched =
                volumeMatched,

            yearMatched =
                yearMatched,

            locatorMatched =
                locatorMatched,

            issueMatched =
                issueMatched
        )
    }


    /*
     * ================================================================
     * ACCEPT / REJECT
     * ================================================================
     *
     * We require both:
     *
     * 1. High agreement among the metadata that Crossref actually
     *    provided.
     *
     * 2. A sufficiently distinctive combination of fields.
     *
     *
     * Strong identity cases:
     *
     * locator + journal
     * locator + volume
     * locator + year
     *
     * OR:
     *
     * journal + volume + year
     */
    private fun isAcceptableCitationMatch(
        evidence: CitationEvidence
    ): Boolean {

        if (
            evidence.score <
            MIN_NORMALIZED_CITATION_SCORE
        ) {

            return false
        }


        val locatorIdentity =
            evidence.locatorMatched &&
                    (
                            evidence.journalMatched ||
                                    evidence.volumeMatched ||
                                    evidence.yearMatched
                            )


        val bibliographicIdentity =
            evidence.journalMatched &&
                    evidence.volumeMatched &&
                    evidence.yearMatched


        return locatorIdentity ||
                bibliographicIdentity
    }


    /*
     * ================================================================
     * JOURNAL COMPARISON
     * ================================================================
     */
    private fun journalSimilarity(
        citationJournal: String,
        item: CrossrefItem
    ): Double {

        val candidateNames =
            itemJournalNames(
                item
            )


        if (
            candidateNames.isEmpty()
        ) {

            return 0.0
        }


        val expectedCanonical =
            JournalAliases.canonicalize(
                citationJournal
            )


        val expectedNormalized =
            JournalAliases.normalizeKey(
                expectedCanonical
            )


        var best =
            0.0


        for (
        candidateName in
        candidateNames
        ) {

            val candidateCanonical =
                JournalAliases.canonicalize(
                    candidateName
                )


            val candidateNormalized =
                JournalAliases.normalizeKey(
                    candidateCanonical
                )


            if (
                candidateNormalized ==
                expectedNormalized
            ) {

                return 1.0
            }


            /*
             * Unknown journal aliases still get a generic token
             * comparison.
             */
            val similarity =
                tokenSimilarity(
                    expectedNormalized,
                    candidateNormalized
                )


            if (
                similarity >
                best
            ) {

                best =
                    similarity
            }
        }


        return best
    }


    private fun itemJournalNames(
        item: CrossrefItem
    ): List<String> {

        return buildList {

            item.containerTitle
                ?.forEach {

                    if (
                        it.isNotBlank()
                    ) {

                        add(it)
                    }
                }


            item.shortContainerTitle
                ?.forEach {

                    if (
                        it.isNotBlank()
                    ) {

                        add(it)
                    }
                }
        }
    }


    /*
     * ================================================================
     * TOKEN SIMILARITY
     * ================================================================
     */
    private fun tokenSimilarity(
        first: String,
        second: String
    ): Double {

        val firstTokens =
            first
                .split(
                    " "
                )
                .filter {

                    it.length >
                            1
                }
                .toSet()


        val secondTokens =
            second
                .split(
                    " "
                )
                .filter {

                    it.length >
                            1
                }
                .toSet()


        if (
            firstTokens.isEmpty() ||
            secondTokens.isEmpty()
        ) {

            return 0.0
        }


        val intersection =
            firstTokens
                .intersect(
                    secondTokens
                )
                .size


        val union =
            firstTokens
                .union(
                    secondTokens
                )
                .size


        if (
            union ==
            0
        ) {

            return 0.0
        }


        return intersection.toDouble() /
                union.toDouble()
    }


    /*
     * ================================================================
     * LOCATOR COMPARISON
     * ================================================================
     */
    private fun locatorSimilarity(
        expected: String,
        actual: String
    ): Double {

        val expectedNormalized =
            normalizeLocator(
                expected
            )


        val actualNormalized =
            normalizeLocator(
                actual
            )


        /*
         * 034009 == 034009
         */
        if (
            expectedNormalized ==
            actualNormalized
        ) {

            return 1.0
        }


        val expectedStart =
            expectedNormalized
                .substringBefore(
                    "-"
                )


        val actualStart =
            actualNormalized
                .substringBefore(
                    "-"
                )


        /*
         * 123 vs 123-130
         *
         * or:
         *
         * 034009 vs 034009-18
         */
        if (
            expectedStart ==
            actualStart
        ) {

            return 0.95
        }


        /*
         * Leading-zero tolerance:
         *
         * 034009
         * 34009
         */
        if (
            expectedStart.all {
                it.isDigit()
            } &&
            actualStart.all {
                it.isDigit()
            }
        ) {

            val expectedWithoutZero =
                expectedStart
                    .trimStart(
                        '0'
                    )
                    .ifBlank {
                        "0"
                    }


            val actualWithoutZero =
                actualStart
                    .trimStart(
                        '0'
                    )
                    .ifBlank {
                        "0"
                    }


            if (
                expectedWithoutZero ==
                actualWithoutZero
            ) {

                return 0.95
            }
        }


        /*
         * Some metadata sources append information:
         *
         * 034009
         * 034009-18PP
         */
        if (
            expectedStart.length >=
            5 &&
            actualNormalized.startsWith(
                expectedStart
            )
        ) {

            return 0.90
        }


        return 0.0
    }


    /*
     * ================================================================
     * FILTER BUILDERS
     * ================================================================
     */
    private fun buildYearFilter(
        year: Int?
    ): String? {

        if (
            year ==
            null
        ) {

            return null
        }


        return buildFilter(
            year =
                year,

            articleNumber =
                null
        )
    }


    private fun buildFilter(
        year: Int?,
        articleNumber: String?
    ): String? {

        val filters =
            mutableListOf<String>()


        if (
            year !=
            null
        ) {

            filters.add(
                "from-pub-date:$year-01-01"
            )


            filters.add(
                "until-pub-date:$year-12-31"
            )
        }


        if (
            !articleNumber.isNullOrBlank()
        ) {

            filters.add(
                "article-number:$articleNumber"
            )
        }


        return filters
            .takeIf {

                it.isNotEmpty()
            }
            ?.joinToString(
                ","
            )
    }


    /*
     * ================================================================
     * NORMALIZATION
     * ================================================================
     */
    private fun normalizedField(
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
            ?.trim()
            ?: ""
    }


    private fun normalizeLocator(
        value: String
    ): String {

        return value
            .uppercase()
            .replace(
                '–',
                '-'
            )
            .replace(
                '—',
                '-'
            )
            .replace(
                Regex(
                    """\s+"""
                ),
                ""
            )
            .replace(
                Regex(
                    """[^A-Z0-9-]"""
                ),
                ""
            )
            .trim()
    }


    private fun normalizeQuery(
        value: String
    ): String {

        return value
            .lowercase()
            .replace(
                Regex(
                    """[^\p{L}\p{N}]+"""
                ),
                " "
            )
            .replace(
                Regex(
                    """\s+"""
                ),
                " "
            )
            .trim()
    }


    /*
     * ================================================================
     * PUBLICATION YEAR
     * ================================================================
     */
    private fun CrossrefItem.publicationYear(): Int? {

        return publishedPrint
            ?.dateParts
            ?.firstOrNull()
            ?.firstOrNull()

            ?: publishedOnline
                ?.dateParts
                ?.firstOrNull()
                ?.firstOrNull()

            ?: published
                ?.dateParts
                ?.firstOrNull()
                ?.firstOrNull()
    }


    /*
     * ================================================================
     * CROSSREF → PAPER
     * ================================================================
     */
    private fun CrossrefItem.toPaper(): Paper? {

        val paperTitle =
            title
                ?.firstOrNull()
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }
                ?: return null


        val paperAuthors =
            author
                ?.map { author ->

                    listOfNotNull(
                        author.given,
                        author.family
                    )
                        .joinToString(
                            " "
                        )
                        .trim()
                }
                ?.filter {

                    it.isNotBlank()
                }
                ?.joinToString(
                    ", "
                )
                ?: ""


        return Paper(
            title =
                paperTitle,

            authors =
                paperAuthors,

            year =
                publicationYear(),

            doi =
                doi,

            url =
                url
        )
    }


    /*
     * ================================================================
     * CROSSREF REQUEST CONTROL
     * ================================================================
     */
    private suspend fun <T> crossrefListRequest(
        block: suspend () -> T
    ): T {

        return crossrefListMutex.withLock {

            val nowNanos =
                System.nanoTime()


            if (
                lastCrossrefListRequestNanos != 0L
            ) {

                val elapsedMs =
                    (
                            nowNanos -
                                    lastCrossrefListRequestNanos
                            ) /
                            1_000_000L


                val waitMs =
                    MIN_LIST_REQUEST_INTERVAL_MS -
                            elapsedMs


                if (waitMs > 0L) {
                    delay(waitMs)
                }
            }


            try {

                retryCrossrefRequest(
                    block
                )

            } finally {

                lastCrossrefListRequestNanos =
                    System.nanoTime()
            }
        }
    }


    private suspend fun <T> crossrefSingleRequest(
        block: suspend () -> T
    ): T {

        return retryCrossrefRequest(
            block
        )
    }


    private suspend fun <T> retryCrossrefRequest(
        block: suspend () -> T
    ): T {

        try {

            return block()

        } catch (
            exception: HttpException
        ) {

            if (
                exception.code() !in
                setOf(
                    429,
                    502,
                    503,
                    504
                )
            ) {

                throw exception
            }


            val retryDelay = retryDelayMillis(exception)
                ?: throw exception

            delay(retryDelay)
            return block()

        } catch (
            exception: IOException
        ) {

            /*
             * All Crossref calls here are GETs, so one short retry is safe
             * for a transient connection reset or DNS/socket interruption.
             */
            delay(
                350L
            )


            return block()
        }
    }


    internal fun retryDelayMillis(
        exception: HttpException
    ): Long? {

        val retryAfterSeconds = exception
            .response()
            ?.headers()
            ?.get("Retry-After")
            ?.trim()
            ?.toLongOrNull()

        if (retryAfterSeconds == null) {
            return 1_050L
        }

        val requestedMillis = retryAfterSeconds
            .coerceAtLeast(0L)
            .times(1_000L)

        return requestedMillis
            .takeIf { it <= MAX_RETRY_AFTER_MS }
            ?.coerceAtLeast(250L)
    }


    /*
     * ================================================================
     * INTERNAL MODELS
     * ================================================================
     */
    private data class CitationCandidate(

        val paper: Paper,

        val evidence: CitationEvidence
    )


    private data class CitationEvidence(

        val score: Double,

        val comparableWeight: Double,

        val matchedWeight: Double,

        val journalMatched: Boolean,

        val volumeMatched: Boolean,

        val yearMatched: Boolean,

        val locatorMatched: Boolean,

        val issueMatched: Boolean
    )
}