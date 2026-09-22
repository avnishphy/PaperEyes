package com.example.papereyes.ocr

import com.example.papereyes.data.model.ScholarlyIdentifiers
import com.example.papereyes.domain.citation.JournalCitationParser
import com.example.papereyes.domain.evidence.OcrLine
import com.example.papereyes.domain.evidence.ReferenceEvidence
import java.util.Locale

internal data class ReferenceDetection(
    val references: List<ReferenceEvidence>,
    val referenceLineIndexes: Set<Int>
)

internal object ReferenceDetector {

    fun fromText(
        rawText: String
    ): ReferenceDetection {
        val lines = rawText
            .take(MAX_INPUT_CHARS)
            .lines()
            .take(MAX_INPUT_LINES)
            .mapIndexed { index, text -> SourceLine(index, clean(text)) }

        return detect(lines, paragraphGroups(lines))
    }

    fun fromLayout(
        lines: List<OcrLine>,
        imageWidth: Int,
        imageHeight: Int
    ): ReferenceDetection {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return fromText(lines.joinToString("\n") { it.text })
        }

        val valid = lines
            .withIndex()
            .asSequence()
            .filter { (_, line) ->
                line.left.isFinite() && line.top.isFinite() &&
                    line.right.isFinite() && line.bottom.isFinite() &&
                    line.width > 0f && line.height > 0f
            }
            .sortedWith(compareBy<IndexedValue<OcrLine>> { it.value.top }.thenBy { it.value.left })
            .take(MAX_INPUT_LINES)
            .map { (index, line) ->
                SourceLine(
                    index = index,
                    text = clean(line.text),
                    blockId = line.blockId,
                    left = line.left,
                    top = line.top,
                    bottom = line.bottom,
                    height = line.height
                )
            }
            .toList()

        val blockGroups = valid
            .groupBy { it.blockId }
            .values
            .flatMap(::layoutParagraphGroups)

        return detect(valid, blockGroups)
    }

    private fun detect(
        lines: List<SourceLine>,
        unnumberedGroups: List<List<SourceLine>>
    ): ReferenceDetection {
        if (lines.isEmpty()) return ReferenceDetection(emptyList(), emptySet())

        val headingPosition = lines.indexOfFirst { DocumentLayoutAnalyzer.isReferenceHeading(it.text) }
        val eligible = if (headingPosition >= 0) lines.drop(headingPosition + 1) else lines
        val starts = eligible.mapIndexedNotNull { position, line ->
            parseStart(line.text)?.let { start -> NumberedStart(position, line, start.first, start.second) }
        }

        val numbered = mutableListOf<DetectedReference>()
        if (starts.size >= MIN_UNHEADED_NUMBERED_REFERENCES || headingPosition >= 0) {
            starts.forEachIndexed { index, start ->
                val end = starts.getOrNull(index + 1)?.position ?: eligible.size
                val members = eligible.subList(start.position, end)
                val bodyParts = buildList {
                    if (start.body.isNotBlank()) add(start.body)
                    addAll(members.drop(1).map { it.text }.filter(String::isNotBlank))
                }
                val text = joinWrapped(bodyParts).take(MAX_REFERENCE_CHARS)
                if (isUsableReference(text)) {
                    numbered += DetectedReference(
                        evidence = ReferenceEvidence(start.label, text, bestQuery(text)),
                        lineIndexes = members.mapTo(linkedSetOf()) { it.index }
                    )
                }
            }
        }

        val detected = if (numbered.isNotEmpty()) {
            numbered
        } else {
            val candidates = unnumberedGroups.mapNotNull { group ->
                val relevant = if (headingPosition >= 0) {
                    group.filter { candidate -> lines.indexOf(candidate) > headingPosition }
                } else {
                    group
                }
                val text = joinWrapped(relevant.map { it.text }).take(MAX_REFERENCE_CHARS)
                if (text.isBlank()) null else UnnumberedCandidate(
                    detected = DetectedReference(
                        evidence = ReferenceEvidence(null, text, bestQuery(text)),
                        lineIndexes = relevant.mapTo(linkedSetOf()) { it.index }
                    ),
                    strong = looksLikeUnnumberedReference(text),
                    structural = looksLikeBibliographyParagraph(text)
                )
            }

            val repeatedBibliographyLayout =
                headingPosition >= 0 ||
                    (candidates.count { it.strong } >= 1 &&
                        candidates.count { it.structural } >= MIN_STRUCTURAL_REFERENCES)

            candidates
                .filter { it.strong || (repeatedBibliographyLayout && it.structural) }
                .map { it.detected }
        }

        val unique = detected
            .filter { it.evidence.text.isNotBlank() }
            .distinctBy { canonical(it.evidence.query) }
            .take(MAX_REFERENCES)

        return ReferenceDetection(
            references = unique.map { it.evidence },
            referenceLineIndexes = unique.flatMapTo(linkedSetOf()) { it.lineIndexes }
        )
    }

    private fun paragraphGroups(lines: List<SourceLine>): List<List<SourceLine>> {
        val groups = mutableListOf<MutableList<SourceLine>>()
        lines.forEach { line ->
            if (line.text.isBlank()) return@forEach
            if (groups.isEmpty() || (line.index > 0 && lines[line.index - 1].text.isBlank())) {
                groups += mutableListOf<SourceLine>()
            }
            groups.last() += line
        }
        // ML Kit plain text frequently has no blank separator. In that case,
        // retain each strong citation line as an independently selectable item.
        return if (groups.size == 1 && groups.single().size > 1) {
            groups.single().map(::listOf)
        } else groups
    }

    /**
     * ML Kit often returns an entire unnumbered bibliography page as one block.
     * Recover its visual paragraphs from vertical spacing and hanging indents.
     */
    private fun layoutParagraphGroups(
        block: List<SourceLine>
    ): List<List<SourceLine>> {
        val ordered = block.sortedWith(compareBy<SourceLine> { it.top }.thenBy { it.left })
        if (ordered.size <= 1) return listOf(ordered)

        val medianHeight = ordered.map { it.height }.sorted()[ordered.size / 2]
        val groups = mutableListOf<MutableList<SourceLine>>()

        ordered.forEach { line ->
            val current = groups.lastOrNull()
            if (current == null) {
                groups += mutableListOf(line)
                return@forEach
            }

            val previous = current.last()
            val first = current.first()
            val verticalGap = line.top - previous.bottom
            val returnsToParagraphMargin =
                previous.left - first.left >= medianHeight * HANGING_INDENT_HEIGHT_RATIO &&
                    line.left <= first.left + medianHeight * MARGIN_TOLERANCE_HEIGHT_RATIO
            val completedCitation =
                referenceEnding.containsMatchIn(joinWrapped(current.map { it.text }))
            val visibleParagraphGap =
                verticalGap >= medianHeight * PARAGRAPH_GAP_HEIGHT_RATIO

            if (completedCitation || returnsToParagraphMargin || visibleParagraphGap) {
                groups += mutableListOf(line)
            } else {
                current += line
            }
        }

        return groups
    }

    private fun parseStart(text: String): Pair<String, String>? {
        bracketedReferenceStart.matchEntire(text)?.let { match ->
            return match.groupValues[1] to match.groupValues[2]
        }
        plainReferenceStart.matchEntire(text)?.let { match ->
            return match.groupValues[1] to match.groupValues[2]
        }
        return null
    }

    private fun isUsableReference(text: String): Boolean =
        text.count(Char::isLetter) >= 4 && text.length >= 8

    private fun looksLikeUnnumberedReference(text: String): Boolean {
        if (!isUsableReference(text) || text.length > MAX_REFERENCE_CHARS) return false
        if (ScholarlyIdentifiers.extractDoi(text) != null ||
            ScholarlyIdentifiers.extractArxivId(text) != null ||
            JournalCitationParser.parse(text) != null
        ) return true

        val hasYear = publicationYear.containsMatchIn(text)
        val hasAuthorSignal = authorSignal.containsMatchIn(text)
        val hasPublicationSignal = quotedTitle.containsMatchIn(text) || publicationVenue.containsMatchIn(text)
        return hasYear && hasAuthorSignal && hasPublicationSignal
    }

    private fun looksLikeBibliographyParagraph(text: String): Boolean {
        if (!isUsableReference(text) || text.length < MIN_STRUCTURAL_REFERENCE_CHARS) return false
        if (!referenceEnding.containsMatchIn(text)) return false

        val hasAuthorList =
            text.contains(" and ", ignoreCase = true) ||
                text.count { it == ',' } >= 2 ||
                authorSignal.containsMatchIn(text)
        val hasCitationPunctuation =
            text.count { it == '.' } >= 2 ||
                text.count { it == ',' } >= 3
        return hasAuthorList && hasCitationPunctuation
    }

    private fun bestQuery(text: String): String {
        ScholarlyIdentifiers.extractDoi(text)?.let { return it.take(MAX_QUERY_CHARS) }
        ScholarlyIdentifiers.extractArxivId(text)?.let { return "arXiv:$it" }
        JournalCitationParser.parse(text)?.let { return it.raw.take(MAX_QUERY_CHARS) }
        quotedTitle.find(text)?.groupValues?.getOrNull(1)?.let { title ->
            clean(title).takeIf { it.length >= MIN_QUOTED_TITLE_CHARS }?.let {
                return it.take(MAX_QUERY_CHARS)
            }
        }
        return text.take(MAX_QUERY_CHARS)
    }

    private fun joinWrapped(parts: List<String>): String = parts
        .filter(String::isNotBlank)
        .fold("") { result, part ->
            when {
                result.isEmpty() -> part
                result.endsWith('-') && part.firstOrNull()?.isLowerCase() == true -> result + part
                else -> "$result $part"
            }
        }
        .replace(whitespace, " ")
        .trim()

    private fun clean(text: String): String = text.replace(whitespace, " ").trim()

    private fun canonical(text: String): String = text
        .lowercase(Locale.ROOT)
        .replace(canonicalNoise, " ")
        .replace(whitespace, " ")
        .trim()

    private data class SourceLine(
        val index: Int,
        val text: String,
        val blockId: Int = index,
        val left: Float = 0f,
        val top: Float = index.toFloat(),
        val bottom: Float = top,
        val height: Float = 1f
    )

    private data class NumberedStart(
        val position: Int,
        val line: SourceLine,
        val label: String,
        val body: String
    )

    private data class DetectedReference(
        val evidence: ReferenceEvidence,
        val lineIndexes: Set<Int>
    )

    private data class UnnumberedCandidate(
        val detected: DetectedReference,
        val strong: Boolean,
        val structural: Boolean
    )

    private const val MIN_UNHEADED_NUMBERED_REFERENCES = 2
    private const val MIN_QUOTED_TITLE_CHARS = 8
    private const val MIN_STRUCTURAL_REFERENCES = 3
    private const val MIN_STRUCTURAL_REFERENCE_CHARS = 45
    private const val MAX_REFERENCES = 50
    private const val MAX_REFERENCE_CHARS = 800
    private const val MAX_QUERY_CHARS = 500
    private const val MAX_INPUT_LINES = 600
    private const val MAX_INPUT_CHARS = 100_000
    private const val HANGING_INDENT_HEIGHT_RATIO = 0.55f
    private const val MARGIN_TOLERANCE_HEIGHT_RATIO = 0.45f
    private const val PARAGRAPH_GAP_HEIGHT_RATIO = 0.55f
}

private val bracketedReferenceStart =
    Regex("""^\s*\[\s*(\d{1,4})\s*]\s*(.*)$""")

private val plainReferenceStart =
    Regex("""^\s*(\d{1,4})[.)]\s*(.*)$""")

private val whitespace = Regex("""\s+""")
private val canonicalNoise = Regex("""[^\p{L}\p{N}]+""")
private val publicationYear = Regex("""\b(?:18|19|20)\d{2}[a-z]?\b""", RegexOption.IGNORE_CASE)
private val referenceEnding = Regex("""(?:18|19|20)\d{2}[a-z]?[.)]?\s*$""", RegexOption.IGNORE_CASE)
private val authorSignal = Regex(
    """(?i)(?:\bet\s+al\.?\b|\b[A-Z][\p{L}'-]+\s*,\s*(?:[A-Z]\.|[A-Z][\p{L}'-]+)|(?:\b[A-Z]\.){1,3}\s*[A-Z][\p{L}'-]+|\b[A-Z][\p{L}'-]+(?:\s+[A-Z](?:\.|[\p{L}'-]+)){1,3}\s+(?:and|&|,))"""
)
private val publicationVenue = Regex(
    """(?i)\b(?:proceedings|conference|workshop|symposium|journal|transactions|letters|review|rev\.|phys\.|machine learning|neural information processing systems|computer vision|computational linguistics|chemical physics|arxiv|ICML|NeurIPS|NIPS|CVPR|ACL|EMNLP|AAAI|IJCAI|Springer|Elsevier|IEEE|ACM)\b"""
)
private val quotedTitle = Regex("""[\"“‘']([^\"”’']{8,300})[\"”’']""")
