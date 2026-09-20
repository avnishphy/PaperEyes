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

    private val modernArxivRegex = Regex(
        pattern = """(?<![A-Z0-9])(\d{4}\.\d{4,5})(?:\s*v\s*(\d+))?(?![A-Z0-9])""",
        option = RegexOption.IGNORE_CASE
    )

    private val legacyArxivRegex = Regex(
        pattern = """(?<![A-Z0-9])[A-Z][A-Z0-9.-]*/\d{7}(?:v\d+)?(?![A-Z0-9])""",
        option = RegexOption.IGNORE_CASE
    )

    private val arxivDoiRegex = Regex(
        pattern = """10\.48550/arxiv\.(\d{4}\.\d{4,5}(?:v\d+)?)""",
        option = RegexOption.IGNORE_CASE
    )

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
        arxivDoiRegex.find(text.trim())
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase(Locale.ROOT)

    fun extractArxivId(raw: String): String? {
        var value = raw.trim()
            .replace(arxivPrefixRegex, "")
            .trim()

        if (value.endsWith(".pdf", ignoreCase = true)) {
            value = value.dropLast(4).trim()
        }

        return modernArxivRegex.find(value)?.let(::canonicalizeModernArxivMatch)
            ?: legacyArxivRegex.find(value)?.value
                ?.lowercase(Locale.ROOT)
    }

    fun extractAllArxivIds(text: String): List<String> = buildList {
        modernArxivRegex.findAll(text).forEach { add(canonicalizeModernArxivMatch(it)) }
        legacyArxivRegex.findAll(text).forEach { add(it.value.lowercase(Locale.ROOT)) }
    }.distinct()

    fun looksLikeIdentifier(text: String): Boolean =
        extractDoi(text) != null ||
                extractArxivDoiId(text) != null ||
                extractArxivId(text) != null

    private fun canonicalizeModernArxivMatch(match: MatchResult): String {
        val base = match.groupValues.getOrNull(1).orEmpty()
        val version = match.groupValues.getOrNull(2).orEmpty()

        return buildString {
            append(base)
            if (version.isNotBlank()) {
                append('v')
                append(version)
            }
        }.lowercase(Locale.ROOT)
    }

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
