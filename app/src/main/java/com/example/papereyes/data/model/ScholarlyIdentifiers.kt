package com.example.papereyes.data.model

import java.util.Locale

/**
 * Canonical parsing for scholarly identifiers used by OCR, networking and
 * persistence. Keeping these rules in one place prevents an identifier from
 * being accepted one way by the resolver and stored another way by Room.
 */
object ScholarlyIdentifiers {

    private val doiRegex = Regex(
        pattern = """10\.\d{4,9}/[-._;()/:A-Z0-9]+""",
        option = RegexOption.IGNORE_CASE
    )

    // Only horizontal OCR spacing is repaired; never substitute l/O globally.
    // A malformed version suffix is rejected instead of becoming a title.
    private const val MODERN_ARXIV = """\d{2}(?:0[1-9]|1[0-2])\.\d{4,5}"""
    private const val LEGACY_ARXIV = """[A-Z][A-Z0-9.-]*/\d{7}"""
    private val arxivRegex = Regex(
        """(?<![A-Z0-9])($MODERN_ARXIV|$LEGACY_ARXIV)(?:[ \t]*v[ \t]*([1-9]\d*))?(?![A-Z0-9])""",
        RegexOption.IGNORE_CASE
    )
    private val arxivDoiRegex = Regex(
        """10\.48550/arxiv\.($MODERN_ARXIV)(?:[ \t]*v[ \t]*([1-9]\d*))?(?![A-Z0-9])""",
        RegexOption.IGNORE_CASE
    )
    private val damagedVersionSuffix = Regex("""^[ \t]*v(?:\b|[ \t])""", RegexOption.IGNORE_CASE)

    private val doiPrefixRegex = Regex(
        pattern = """^(?:doi\s*:\s*|https?://(?:dx\.)?doi\.org/)""",
        option = RegexOption.IGNORE_CASE
    )

    private val arxivPrefixRegex = Regex(
        pattern = """^(?:arxiv\s*:\s*|https?://(?:www\.)?arxiv\.org/(?:abs|pdf)/)""",
        option = RegexOption.IGNORE_CASE
    )

    fun normalizeDoi(raw: String?): String? {
        val value = raw
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return null

        val withoutPrefix = value
            .replace(doiPrefixRegex, "")
            .trim()

        val candidate = doiRegex.find(withoutPrefix)?.value
            ?: return null

        return trimDoiCitationPunctuation(candidate)
            .lowercase(Locale.ROOT)
            .takeIf(String::isNotBlank)
    }

    fun extractDoi(text: String): String? =
        normalizeDoi(text)

    fun extractAllDois(text: String): List<String> =
        doiRegex.findAll(text)
            .mapNotNull { normalizeDoi(it.value) }
            .distinct()
            .toList()

    fun extractArxivDoiId(text: String): String? =
        arxivDoiRegex.findAll(text).mapNotNull { canonicalArxivMatch(it, text) }.firstOrNull()

    fun extractArxivId(raw: String): String? {
        val value = raw.trim().replace(arxivPrefixRegex, "").removeSuffix(".pdf")
        return arxivRegex.findAll(value)
            .mapNotNull { canonicalArxivMatch(it, value) }.firstOrNull()
    }

    fun extractAllArxivIds(text: String): List<String> =
        arxivRegex.findAll(text).mapNotNull { canonicalArxivMatch(it, text) }.distinct().toList()

    /** A work-level key, for services that do not index arXiv versions. */
    fun arxivWorkId(raw: String): String? = extractArxivId(raw)?.replace(Regex("""v[1-9]\d*$"""), "")

    private fun canonicalArxivMatch(match: MatchResult, source: String): String? {
        val version = match.groupValues[2]
        if (version.isEmpty() && damagedVersionSuffix.containsMatchIn(source.substring(match.range.last + 1))) {
            return null
        }
        return match.groupValues[1].lowercase(Locale.ROOT) +
            if (version.isNotEmpty()) "v$version" else ""
    }

    fun looksLikeIdentifier(text: String): Boolean =
        extractDoi(text) != null ||
            extractArxivDoiId(text) != null ||
            extractArxivId(text) != null

    private fun trimDoiCitationPunctuation(raw: String): String {
        var value = raw.trim().trimEnd('.', ',', ';')

        // Parentheses are legal inside DOI suffixes. Remove only an unmatched
        // closing parenthesis introduced by surrounding prose/citation syntax.
        while (
            value.endsWith(')') &&
            value.count { it == ')' } > value.count { it == '(' }
        ) {
            value = value.dropLast(1).trimEnd('.', ',', ';')
        }

        return value
    }
}
