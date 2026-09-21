package com.example.papereyes.ocr

import com.example.papereyes.data.model.ScholarlyIdentifiers
import com.example.papereyes.domain.citation.JournalCitationParser
import com.example.papereyes.domain.evidence.DocumentEvidence
import com.example.papereyes.domain.evidence.FingerprintExtractor
import com.example.papereyes.domain.evidence.LayoutProfile
import com.example.papereyes.domain.evidence.LayoutRole
import com.example.papereyes.domain.evidence.OcrLine
import com.example.papereyes.domain.evidence.TitleEvidence
import kotlin.math.abs

/** Line-height hierarchy is probabilistic; it does not infer actual font names. */
object DocumentLayoutAnalyzer {
    private val space = Regex("\\s+")
    private val headingPrefix = Regex("(?i)^(?:(?:[IVXLCDM]+|\\d+(?:\\.\\d+)*)(?:[.)])?\\s+)")
    private val references = Regex("(?i)^(?:references?(?:\\s+(?:and|&)\\s+(?:notes|further reading))?|reference list|bibliography|bibliographic references|works cited|literature cited)$")
    private val numberedReference = Regex("^\\s*(?:\\[\\s*\\d+\\s*]|\\d+[.)])\\s+")
    private val sections = Regex("(?i)^(?:abstract|introduction|conclusions?|summary|acknowledg(?:e)?ments?|references|results(?: and discussion)?|discussion|methods?|materials and methods|supplementary material)$")
    private val caption = Regex("(?i)^(?:fig(?:ure)?\\.?|table)\\s*\\d+")
    private val metadata = Regex("(?i)(?:\\barxiv\\s*:|\\bdoi\\s*:|https?://|www\\.|copyright|all rights reserved|received:|accepted:|published:|keywords:|corresponding author|email:|university|department of|institute of)")
    private val conference = Regex("(?i)^(?:.*\\bconference\\b|proceedings of|.*\\bworkshop\\b|symposium on)")
    private val proseStart = Regex("(?i)^(?:we |our results |these |this (?:is|shows|paper|work)|the results |in this |it is |as shown |to (?:calculate|obtain|evaluate) )")

    fun isReferenceHeading(text: String): Boolean = references.matches(
        clean(text).replace(headingPrefix, "").trimEnd(':', '.')
    )

    private fun clean(text: String) = text.replace(space, " ").trim()
    private fun words(text: String) = text.split(space).size
    private fun isBody(text: String): Boolean = proseStart.containsMatchIn(text) ||
        (text.endsWith('.') && words(text) >= 10) || words(text) > 25

    private fun titleLine(text: String): Boolean {
        if (text.length !in 6..250 || text.count(Char::isLetter) < 5 || words(text) < 2) return false
        if (metadata.containsMatchIn(text) || conference.containsMatchIn(text)) return false
        if (isReferenceHeading(text) || numberedReference.containsMatchIn(text)) return false
        if (headingPrefix.containsMatchIn(text) || sections.matches(text.trimEnd(':', '.'))) return false
        if (caption.containsMatchIn(text) || isBody(text)) return false
        if (text.count { it == ',' } >= 4 || text.count { it in "=∫∑{}^_" } >= 2) return false
        if (ScholarlyIdentifiers.looksLikeIdentifier(text)) return false
        if (text.any(Char::isDigit) && JournalCitationParser.parse(text) != null) return false
        return true
    }

    fun fromText(rawText: String): DocumentEvidence {
        val lines = rawText.take(100000).lines().map(::clean).filter(String::isNotBlank)
        val boundary = lines.indexOfFirst(::isReferenceHeading)
        val before = if (boundary >= 0) lines.take(boundary) else lines
        val usable = before.filterNot { numberedReference.containsMatchIn(it) }
        val titles = mutableListOf<TitleEvidence>()
        val head = usable.take(12)
        // Without geometry, only an initial contiguous title-like run is eligible.
        val first = head.indexOfFirst(::titleLine)
        if (first >= 0 && head.take(first).none(::isBody)) {
            val group = head.drop(first).takeWhile(::titleLine).take(3)
            val joined = group.joinToString(" ")
            if (words(joined) in 3..35) titles += TitleEvidence(joined.take(300), 1.0)
        }
        return buildEvidence(usable, titles, LayoutProfile(
            role = when { usable.isEmpty() && lines.isNotEmpty() -> LayoutRole.REFERENCES
                titles.isNotEmpty() -> LayoutRole.TITLE_LIKE
                usable.any(::isBody) -> LayoutRole.BODY
                else -> LayoutRole.UNKNOWN }
        ), usable)
    }

    fun analyze(rawText: String, input: List<OcrLine>, imageWidth: Int, imageHeight: Int): DocumentEvidence {
        if (imageWidth <= 0 || imageHeight <= 0) return fromText(rawText)
        val all = input.asSequence().filter {
            it.left.isFinite() && it.top.isFinite() && it.right.isFinite() && it.bottom.isFinite() && it.height > 0 && it.width > 0
        }.map { it.copy(text = clean(it.text)) }.filter { it.text.isNotBlank() }.take(600)
            .sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left }).toList()
        if (all.isEmpty()) return fromText(rawText)
        val boundary = all.filter { isReferenceHeading(it.text) }.minOfOrNull { it.top }
        val eligible = all.filter { (boundary == null || it.top < boundary) && !numberedReference.containsMatchIn(it.text) }
        val bodyLines = eligible.filter { isBody(it.text) }
        val heightSample = bodyLines.ifEmpty { eligible }
        val bodyHeight = heightSample.map { it.height }.sorted().let { if (it.isEmpty()) 1f else it[it.size / 2] }
        val crop = eligible.size in 1..4 && eligible.sumOf { words(it.text) } <= 35 && bodyLines.isEmpty()
        val candidateLines = eligible.filter {
            titleLine(it.text) && it.top / imageHeight < 0.68f && (it.height >= bodyHeight * 1.25f || crop)
        }
        val titles = mutableListOf<TitleEvidence>()
        val consumed = mutableSetOf<OcrLine>()
        for (line in candidateLines) {
            if (line in consumed) continue
            val group = mutableListOf(line)
            consumed += line
            while (group.size < 4) {
                val previous = group.last()
                val next = candidateLines.firstOrNull {
                    it !in consumed && joins(previous, it, imageWidth)
                } ?: break
                group += next
                consumed += next
            }
            val title = group.joinToString(" ") { it.text }
            if (words(title) !in 3..35 || title.length > 300) continue
            val score = group.map { it.height }.average() / bodyHeight +
                2.0 * (1.0 - line.top / imageHeight) + minOf(group.size, 3) * 0.15
            titles += TitleEvidence(title, score)
        }
        // No fallback to arbitrary prose when usable geometry contradicts a title.
        val role = when {
            eligible.isEmpty() -> LayoutRole.REFERENCES
            titles.isNotEmpty() && bodyLines.isNotEmpty() -> LayoutRole.MIXED
            titles.isNotEmpty() -> LayoutRole.TITLE_LIKE
            bodyLines.isNotEmpty() || eligible.size > 4 -> LayoutRole.BODY
            else -> LayoutRole.UNKNOWN
        }
        val identityLines = eligible.filter { it.top / imageHeight < 0.72f }.map { it.text }
        val blocks = eligible.groupBy { it.blockId }.values.map { block ->
            block.sortedBy { it.top }.joinToString(" ") { it.text }
        }
        return buildEvidence(identityLines, titles.sortedByDescending { it.score }, LayoutProfile(role, bodyHeight, true), blocks)
    }

    private fun joins(first: OcrLine, second: OcrLine, width: Int): Boolean {
        val gap = second.top - first.bottom
        if (gap < -minOf(first.height, second.height) * 0.12f || gap > maxOf(first.height, second.height) * 1.1f) return false
        val ratio = second.height / first.height
        if (ratio !in 0.75f..1.33f) return false
        val overlap = minOf(first.right, second.right) - maxOf(first.left, second.left)
        val sameCenter = abs((first.left + first.right - second.left - second.right) / 2f) < width * 0.06f
        return overlap / minOf(first.width, second.width) > 0.55f &&
            (sameCenter || abs(first.left - second.left) < width * 0.04f)
    }

    private fun buildEvidence(
        lines: List<String>, titles: List<TitleEvidence>, profile: LayoutProfile, blocks: List<String>
    ): DocumentEvidence {
        // A DOI in a paragraph or a reference is not the document's own ID.
        val metadataLines = lines.take(12).filter { !isBody(it) && words(it) <= 12 }
        val doiCandidates = metadataLines.flatMap(ScholarlyIdentifiers::extractAllDois).distinct()
        val arxivCandidates = metadataLines.flatMap(ScholarlyIdentifiers::extractAllArxivIds).distinct()
        val journal = metadataLines.mapNotNull { if (it.any(Char::isDigit)) JournalCitationParser.parse(it) else null }
            .distinctBy { it.toBibliographicQuery() }
        // Multiple distinct IDs are ambiguous, not a licence to select the first reference.
        val ambiguousIds = doiCandidates.size > 1 || arxivCandidates.size > 1
        val dois = doiCandidates.takeIf { !ambiguousIds && it.size == 1 }.orEmpty()
        val arxiv = arxivCandidates.takeIf { !ambiguousIds && it.size == 1 }.orEmpty()
        val fingerprints = if (profile.role == LayoutRole.REFERENCES || dois.isNotEmpty() || arxiv.isNotEmpty() || journal.isNotEmpty() || titles.isNotEmpty()) emptyList()
            else FingerprintExtractor.extract(blocks.filterNot { metadata.containsMatchIn(it) || isReferenceHeading(it) })
        return DocumentEvidence(dois, arxiv, journal, titles, fingerprints, profile)
    }
}
