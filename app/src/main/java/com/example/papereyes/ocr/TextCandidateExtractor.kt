package com.example.papereyes.ocr

import com.example.papereyes.data.model.ScholarlyIdentifiers
import com.google.mlkit.vision.text.Text
import kotlin.math.max

object TextCandidateExtractor {

    private val whitespaceRegex = Regex("\\s+")
    private val comparisonCleanupRegex = Regex("[^a-z0-9 ]")

    private val numberedReferencePrefixRegex = Regex(
        """^\s*(?:\[\s*\d+\s*]|\d+[.)])\s+"""
    )

    private val explicitDoiContextRegex = Regex(
        """(?:\bdoi\s*:|https?://(?:dx\.)?doi\.org/)""",
        RegexOption.IGNORE_CASE
    )

    private val explicitArxivContextRegex = Regex(
        """(?:\barxiv\s*:|https?://(?:www\.)?arxiv\.org/(?:abs|pdf)/)""",
        RegexOption.IGNORE_CASE
    )

    private val referenceHeadingRegex = Regex(
        pattern = """^(?:references?(?:\s+(?:and|&)\s+notes)?|reference\s+list|bibliography|bibliographic\s+references|works\s+cited|literature\s+cited|references?\s+(?:and|&)\s+further\s+reading)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val badPhrases = listOf(
        "http://",
        "https://",
        "www.",
        "copyright",
        "all rights reserved",
        "received:",
        "accepted:",
        "published:",
        "keywords:",
        "corresponding author",
        "email:",
        "university",
        "department of",
        "institute of"
    )

    /*
     * ================================================================
     * PLAIN-TEXT OCR PATH
     * ================================================================
     *
     * Without geometry we must be conservative. We no longer return the
     * first DOI/arXiv string anywhere in the OCR output.
     */
    fun extractBestQuery(
        rawText: String
    ): String {

        if (rawText.isBlank()) {
            return ""
        }

        val contentLines = linesBeforeReferenceSection(rawText)

        if (contentLines.isEmpty()) {
            return ""
        }

        /*
         * Only identifiers near the beginning of the OCR stream, or clearly
         * labelled DOI/arXiv metadata in the early part of the document, are
         * considered strong enough to beat title extraction.
         */
        findHighConfidencePlainTextIdentifier(contentLines)?.let {
            return it
        }

        val title = extractTitleFromLines(contentLines)

        if (title.isNotBlank()) {
            return title
        }

        /*
         * Last resort when no plausible title could be extracted. This is
         * still restricted to content before a detected references section.
         */
        return contentLines
            .asSequence()
            .filterNot(::looksLikeReferenceLine)
            .mapNotNull(::extractIdentifier)
            .firstOrNull()
            .orEmpty()
    }

    /*
     * ================================================================
     * GEOMETRY-AWARE ML KIT PATH
     * ================================================================
     *
     * This is the normal image OCR path. Identifiers and title candidates are
     * evaluated independently, then the identifier is allowed to win only if
     * its page position/context makes it trustworthy.
     */
    fun extractBestQuery(
        result: Text,
        imageWidth: Int,
        imageHeight: Int
    ): String {
        if (result.text.isBlank()) return ""
        if (imageWidth <= 0 || imageHeight <= 0) {
            return extractBestQuery(result.text)
        }

        return DocumentLayoutAnalyzer
            .analyze(result, imageWidth, imageHeight)
            .preferred
            ?.query
            .orEmpty()
    }

    private fun linesBeforeReferenceSection(
        rawText: String
    ): List<String> {

        val lines = rawText
            .lines()
            .map(::cleanText)
            .filter { it.isNotBlank() }

        val referenceIndex =
            lines.indexOfFirst(::isReferenceHeading)

        return if (referenceIndex >= 0) {
            lines.take(referenceIndex)
        } else {
            lines
        }
    }

    private fun isReferenceHeading(
        text: String
    ): Boolean {

        val cleaned = cleanText(text)
            .lowercase()
            .trimEnd(':', '.')

        return referenceHeadingRegex.matches(cleaned)
    }

    private fun looksLikeReferenceLine(
        text: String
    ): Boolean {

        val cleaned = text.trim()

        if (isReferenceHeading(cleaned)) {
            return true
        }

        return numberedReferencePrefixRegex
            .containsMatchIn(cleaned)
    }

    /*
     * ================================================================
     * PLAIN-TEXT IDENTIFIER CONFIDENCE
     * ================================================================
     */
    private fun findHighConfidencePlainTextIdentifier(
        lines: List<String>
    ): String? {

        for ((index, line) in lines.take(12).withIndex()) {

            if (looksLikeReferenceLine(line)) {
                continue
            }

            val identifier =
                extractIdentifier(line)
                    ?: continue

            val isVeryEarly =
                index <= 3

            val isExplicitEarlyMetadata =
                index <= 10 &&
                        hasExplicitIdentifierContext(line)

            if (isVeryEarly || isExplicitEarlyMetadata) {
                return identifier
            }
        }

        return null
    }

    /*
     * ================================================================
     * TITLE EXTRACTION
     * ================================================================
     */
    private fun extractTitleFromLines(
        lines: List<String>
    ): String {

        val titleLines = lines
            .map(::cleanText)
            .filter(::isReasonableTitleText)

        if (titleLines.isEmpty()) {
            return ""
        }

        return titleLines
            .take(3)
            .joinToString(" ")
            .take(300)
            .trim()
    }

    /*
     * ================================================================
     * IDENTIFIER EXTRACTION
     * ================================================================
     */
    private fun extractIdentifiers(
        text: String
    ): List<String> {

        val values = mutableListOf<String>()

        values += ScholarlyIdentifiers.extractAllDois(text)

        values += ScholarlyIdentifiers.extractAllArxivIds(text)
            .map { "arXiv:$it" }

        return values.distinct()
    }

    private fun extractIdentifier(
        text: String
    ): String? {

        return extractIdentifiers(text)
            .firstOrNull()
    }

    private fun hasExplicitIdentifierContext(
        text: String
    ): Boolean {

        return explicitDoiContextRegex
            .containsMatchIn(text) ||
                explicitArxivContextRegex
                    .containsMatchIn(text)
    }

    /*
     * ================================================================
     * TITLE QUALITY / NOISE FILTERING
     * ================================================================
     */
    private fun isReasonableTitleText(
        text: String
    ): Boolean {

        if (text.length < 10 || text.length > 350) {
            return false
        }

        if (numberedReferencePrefixRegex.containsMatchIn(text)) {
            return false
        }

        if (text.count { it.isLetter() } < 8) {
            return false
        }

        val words = text.split(whitespaceRegex)

        if (words.size < 3) {
            return false
        }

        return !looksLikeNoise(text)
    }

    private fun looksLikeNoise(
        text: String
    ): Boolean {

        val lower = text.lowercase()

        if (badPhrases.any { lower.contains(it) }) {
            return true
        }

        /*
         * Author lists often contain lots of commas and relatively short
         * name fragments.
         */
        val commaCount =
            text.count { it == ',' }

        if (commaCount >= 4 && text.length < 180) {
            return true
        }

        return false
    }

    private fun cleanText(
        text: String
    ): String {

        return text
            .replace(whitespaceRegex, " ")
            .trim()
    }

    /*
     * ================================================================
     * LIVE-SCAN FRAME-TO-FRAME SIMILARITY
     * ================================================================
     *
     * OCR changes slightly from frame to frame. Exact equality is too strict,
     * so live scanning uses token similarity instead.
     */
    fun areSimilar(
        first: String,
        second: String
    ): Boolean {

        val firstNormalized =
            normalizeForComparison(first)

        val secondNormalized =
            normalizeForComparison(second)

        if (
            firstNormalized.isBlank() ||
            secondNormalized.isBlank()
        ) {
            return false
        }

        if (firstNormalized == secondNormalized) {
            return true
        }

        val firstWords = firstNormalized
            .split(" ")
            .filter { it.length >= 3 }
            .toSet()

        val secondWords = secondNormalized
            .split(" ")
            .filter { it.length >= 3 }
            .toSet()

        if (firstWords.isEmpty() || secondWords.isEmpty()) {
            return false
        }

        val common = firstWords
            .intersect(secondWords)
            .size

        val total = firstWords
            .union(secondWords)
            .size

        val similarity =
            common.toDouble() /
                    max(total, 1).toDouble()

        return similarity >= 0.64
    }

    private fun normalizeForComparison(
        value: String
    ): String {

        return value
            .lowercase()
            .replace(comparisonCleanupRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()
    }

}