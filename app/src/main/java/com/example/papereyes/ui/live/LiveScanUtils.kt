package com.example.papereyes.ui.live

import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.ScholarlyIdentifiers
import kotlin.math.max


data class MatchResult(
    val paper: Paper,
    val score: Double
)


/*
 * ================================================================
 * OCR-AWARE TITLE MATCHING CONSTANTS
 * ================================================================
 *
 * Final score:
 *
 * 35% exact token overlap
 * 45% fuzzy OCR-aware token similarity
 * 20% whole-title character similarity
 *
 * Automatic acceptance also requires the runner-up margin below.
 */
private const val EXACT_TOKEN_WEIGHT = 0.35

private const val FUZZY_TOKEN_WEIGHT = 0.45

private const val WHOLE_TITLE_WEIGHT = 0.20

private val TITLE_WHITESPACE_REGEX = Regex("\\s+")

const val DEFAULT_PAPER_MATCH_THRESHOLD = 0.65
const val DEFAULT_PAPER_MATCH_MARGIN = 0.08


/*
 * Common title words are useful, but should not contribute as much
 * as scientific/content-heavy words.
 */
private val COMMON_TITLE_WORDS =
    setOf(
        "a",
        "an",
        "the",
        "and",
        "or",
        "of",
        "in",
        "on",
        "for",
        "to",
        "from",
        "with",
        "by",
        "using",
        "via",
        "at",
        "as",
        "is",
        "are",
        "be",
        "into"
    )


/*
 * ================================================================
 * OCR CANDIDATE QUALITY
 * ================================================================
 */
fun isGoodCandidate(
    candidate: String
): Boolean {

    val cleaned =
        candidate.trim()


    if (cleaned.isBlank()) {

        return false
    }


    /*
     * DOI / arXiv identifiers do not need to look like titles.
     */
    if (
        looksLikeIdentifier(
            cleaned
        )
    ) {

        return true
    }


    val letterCount =
        cleaned.count {

            it.isLetter()
        }


    val words =
        cleaned
            .split(TITLE_WHITESPACE_REGEX)
            .filter {

                it.any(
                    Char::isLetter
                )
            }


    return (
            cleaned.length >= 12 &&
                    letterCount >= 10 &&
                    words.size >= 3
            )
}


/*
 * ================================================================
 * DOI / ARXIV DETECTION
 * ================================================================
 */
fun looksLikeIdentifier(
    text: String
): Boolean = ScholarlyIdentifiers.looksLikeIdentifier(text)



/*
 * ================================================================
 * BEST PAPER MATCH
 * ================================================================
 */
fun findBestPaperMatch(
    query: String,
    papers: List<Paper>
): MatchResult? {

    if (papers.isEmpty()) {

        return null
    }


    var bestMatch: MatchResult? =
        null


    for (paper in papers) {

        val score =
            titleSimilarity(
                query,
                paper.title
            )


        if (
            bestMatch == null ||
            score > bestMatch.score
        ) {

            bestMatch =
                MatchResult(
                    paper = paper,
                    score = score
                )
        }
    }


    return bestMatch
}

/** Ranked title matches, highest confidence first. */
fun rankPaperMatches(
    query: String,
    papers: List<Paper>
): List<MatchResult> = papers
    .map { MatchResult(it, titleSimilarity(query, it.title)) }
    .sortedByDescending(MatchResult::score)

/**
 * Auto-identification requires both an adequate absolute score and enough
 * separation from the runner-up. A near tie is shown as ambiguous rather than
 * silently selecting whichever result happened to sort first.
 */
fun findConfidentPaperMatch(
    query: String,
    papers: List<Paper>,
    minimumScore: Double = DEFAULT_PAPER_MATCH_THRESHOLD,
    minimumMargin: Double = DEFAULT_PAPER_MATCH_MARGIN
): MatchResult? {
    val ranked = rankPaperMatches(query, papers)
    val best = ranked.firstOrNull() ?: return null
    if (best.score < minimumScore) return null

    val runnerUp = ranked.getOrNull(1) ?: return best
    return best.takeIf { best.score - runnerUp.score >= minimumMargin }
}



/*
 * ================================================================
 * OCR-AWARE TITLE SIMILARITY
 * ================================================================
 *
 * This replaces the old pure Jaccard comparison.
 *
 * Example:
 *
 * OCR:
 *     Deep inelastic scatterlng from nuclei
 *
 * Crossref:
 *     Deep Inelastic Scattering from Nuclei
 *
 * Old Jaccard treats:
 *
 *     scatterlng != scattering
 *
 * New matcher sees that only one character is wrong.
 */
fun titleSimilarity(
    first: String,
    second: String
): Double {

    val firstTokens =
        tokenizeTitle(
            first
        )


    val secondTokens =
        tokenizeTitle(
            second
        )


    if (
        firstTokens.isEmpty() ||
        secondTokens.isEmpty()
    ) {

        return 0.0
    }


    /*
     * ---------------------------------------------------------------
     * 1. EXACT TOKEN OVERLAP
     * ---------------------------------------------------------------
     *
     * Similar to Jaccard, but scientific/long words contribute more,
     * while words such as "the", "of", and "and" contribute less.
     */
    val exactScore =
        weightedExactTokenJaccard(
            firstTokens,
            secondTokens
        )


    /*
     * ---------------------------------------------------------------
     * 2. FUZZY TOKEN SCORE
     * ---------------------------------------------------------------
     *
     * Compare every token against its closest token in the other
     * title using normalized Levenshtein similarity.
     *
     * We calculate it in both directions so a tiny matching fragment
     * cannot produce an artificially high score.
     */
    val firstToSecond =
        fuzzyTokenCoverage(
            sourceTokens =
                firstTokens,

            targetTokens =
                secondTokens
        )


    val secondToFirst =
        fuzzyTokenCoverage(
            sourceTokens =
                secondTokens,

            targetTokens =
                firstTokens
        )


    val fuzzyScore =
        (
                firstToSecond +
                        secondToFirst
                ) / 2.0


    /*
     * ---------------------------------------------------------------
     * 3. WHOLE-TITLE EDIT SIMILARITY
     * ---------------------------------------------------------------
     *
     * Useful when OCR makes several small character errors while the
     * overall title remains almost identical.
     */
    val normalizedFirst =
        normalizeTitle(
            first
        )


    val normalizedSecond =
        normalizeTitle(
            second
        )


    val wholeTitleScore =
        normalizedEditSimilarity(
            normalizedFirst,
            normalizedSecond
        )


    /*
     * ---------------------------------------------------------------
     * FINAL HYBRID SCORE
     * ---------------------------------------------------------------
     */
    val finalScore =
        EXACT_TOKEN_WEIGHT *
                exactScore +

                FUZZY_TOKEN_WEIGHT *
                fuzzyScore +

                WHOLE_TITLE_WEIGHT *
                wholeTitleScore


    return finalScore
        .coerceIn(
            0.0,
            1.0
        )
}


/*
 * ================================================================
 * EXACT WEIGHTED TOKEN JACCARD
 * ================================================================
 */
private fun weightedExactTokenJaccard(
    firstTokens: List<String>,
    secondTokens: List<String>
): Double {

    val firstSet =
        firstTokens.toSet()

    val secondSet =
        secondTokens.toSet()


    val union =
        firstSet.union(
            secondSet
        )


    if (union.isEmpty()) {

        return 0.0
    }


    val intersection =
        firstSet.intersect(
            secondSet
        )


    var unionWeight =
        0.0


    for (token in union) {

        unionWeight +=
            tokenWeight(
                token
            )
    }


    if (unionWeight <= 0.0) {

        return 0.0
    }


    var intersectionWeight =
        0.0


    for (token in intersection) {

        intersectionWeight +=
            tokenWeight(
                token
            )
    }


    return intersectionWeight /
            unionWeight
}


/*
 * ================================================================
 * FUZZY TOKEN COVERAGE
 * ================================================================
 *
 * For every source token, find its closest target token.
 *
 * Example:
 *
 *     scatterlng
 *          vs
 *     scattering
 *
 * gives a very high character similarity even though it is not an
 * exact token match.
 */
private fun fuzzyTokenCoverage(
    sourceTokens: List<String>,
    targetTokens: List<String>
): Double {

    if (
        sourceTokens.isEmpty() ||
        targetTokens.isEmpty()
    ) {

        return 0.0
    }


    var weightedScore =
        0.0

    var totalWeight =
        0.0


    for (sourceToken in sourceTokens) {

        val weight =
            tokenWeight(
                sourceToken
            )


        var bestSimilarity =
            0.0


        for (targetToken in targetTokens) {

            val similarity =
                normalizedEditSimilarity(
                    sourceToken,
                    targetToken
                )


            if (
                similarity >
                bestSimilarity
            ) {

                bestSimilarity =
                    similarity
            }
        }


        /*
         * Short words must be extremely close because otherwise
         * unrelated tokens can match accidentally.
         *
         * Longer scientific words are allowed slightly more OCR noise.
         */
        val requiredSimilarity =
            minimumUsefulTokenSimilarity(
                sourceToken.length
            )


        val acceptedSimilarity =
            if (
                bestSimilarity >=
                requiredSimilarity
            ) {

                bestSimilarity

            } else {

                0.0
            }


        weightedScore +=
            acceptedSimilarity *
                    weight


        totalWeight +=
            weight
    }


    if (totalWeight <= 0.0) {

        return 0.0
    }


    return weightedScore /
            totalWeight
}


/*
 * ================================================================
 * TOKEN IMPORTANCE
 * ================================================================
 *
 * Longer content-heavy words carry more information about the paper.
 *
 * For example:
 *
 *     "electroproduction"
 *
 * should matter considerably more than:
 *
 *     "of"
 */
private fun tokenWeight(
    token: String
): Double {

    if (
        token in
        COMMON_TITLE_WORDS
    ) {

        return 0.30
    }


    return when {

        token.length >= 12 ->
            1.60

        token.length >= 9 ->
            1.45

        token.length >= 7 ->
            1.30

        token.length >= 5 ->
            1.10

        token.length >= 3 ->
            0.90

        else ->
            0.60
    }
}


/*
 * ================================================================
 * TOKEN FUZZY MATCH THRESHOLD
 * ================================================================
 *
 * We are stricter for short words because one character difference
 * can represent a completely different word.
 */
private fun minimumUsefulTokenSimilarity(
    tokenLength: Int
): Double {

    return when {

        tokenLength <= 3 ->
            0.90

        tokenLength <= 5 ->
            0.75

        else ->
            0.65
    }
}


/*
 * ================================================================
 * NORMALIZED CHARACTER EDIT SIMILARITY
 * ================================================================
 *
 * 1.0 = identical
 * 0.0 = completely different
 */
private fun normalizedEditSimilarity(
    first: String,
    second: String
): Double {

    if (
        first == second
    ) {

        return 1.0
    }


    if (
        first.isEmpty() ||
        second.isEmpty()
    ) {

        return 0.0
    }


    val distance =
        levenshteinDistance(
            first,
            second
        )


    val longestLength =
        max(
            first.length,
            second.length
        )


    if (
        longestLength == 0
    ) {

        return 1.0
    }


    return (
            1.0 -
                    distance.toDouble() /
                    longestLength.toDouble()
            )
        .coerceIn(
            0.0,
            1.0
        )
}


/*
 * ================================================================
 * LEVENSHTEIN DISTANCE
 * ================================================================
 *
 * Memory-efficient implementation using two rows rather than a full
 * matrix.
 */
private fun levenshteinDistance(
    first: String,
    second: String
): Int {

    if (first == second) {

        return 0
    }


    if (first.isEmpty()) {

        return second.length
    }


    if (second.isEmpty()) {

        return first.length
    }


    /*
     * Use the shorter string as the columns to reduce memory.
     */
    val longer: String

    val shorter: String


    if (
        first.length >=
        second.length
    ) {

        longer =
            first

        shorter =
            second

    } else {

        longer =
            second

        shorter =
            first
    }


    var previousRow =
        IntArray(
            shorter.length + 1
        ) { index ->

            index
        }


    for (
    longerIndex in
    1..longer.length
    ) {

        val currentRow =
            IntArray(
                shorter.length + 1
            )


        currentRow[0] =
            longerIndex


        for (
        shorterIndex in
        1..shorter.length
        ) {

            val substitutionCost =
                if (
                    longer[longerIndex - 1] ==
                    shorter[shorterIndex - 1]
                ) {

                    0

                } else {

                    1
                }


            val insertion =
                currentRow[shorterIndex - 1] +
                        1


            val deletion =
                previousRow[shorterIndex] +
                        1


            val substitution =
                previousRow[shorterIndex - 1] +
                        substitutionCost


            currentRow[shorterIndex] =
                minOf(
                    insertion,
                    deletion,
                    substitution
                )
        }


        previousRow =
            currentRow
    }


    return previousRow[
        shorter.length
    ]
}


/*
 * ================================================================
 * TITLE TOKENIZATION
 * ================================================================
 */
private fun tokenizeTitle(
    title: String
): List<String> {

    return normalizeTitle(
        title
    )
        .split(
            " "
        )
        .filter {

            it.length > 1
        }
}


/*
 * ================================================================
 * TITLE NORMALIZATION
 * ================================================================
 */
fun normalizeTitle(
    title: String
): String {

    return title
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