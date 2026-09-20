package com.example.papereyes.ocr

import com.example.papereyes.data.model.ScholarlyIdentifiers
import com.example.papereyes.domain.citation.JournalCitation
import com.example.papereyes.domain.citation.JournalCitationParser
import com.google.mlkit.vision.text.Text
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

/**
 * Lightweight document-layout analysis built on top of ML Kit's OCR geometry.
 *
 * ML Kit does not expose the font family or typographic point size. For a
 * single photographed page, however, the line bounding-box height is a useful
 * relative font-size proxy. We cluster those heights, infer the dominant body
 * size, group adjacent same-size lines, and keep bibliographic evidence (DOI,
 * arXiv and journal citations) separate from title candidates.
 */
object DocumentLayoutAnalyzer {

    enum class EvidenceType {
        DOI,
        ARXIV,
        JOURNAL_CITATION,
        TITLE
    }

    data class ResolutionEvidence(
        val type: EvidenceType,
        val query: String,
        val score: Double
    )

    data class TextCandidate(
        val text: String,
        val score: Double,
        val topRatio: Double,
        val sizeRatioToBody: Double,
        val lineCount: Int
    )

    data class JournalCandidate(
        val rawText: String,
        val citation: JournalCitation,
        val score: Double,
        val topRatio: Double
    )

    data class FontSizeCluster(
        val medianHeightPx: Double,
        val lineCount: Int,
        val characterCount: Int
    )

    data class DocumentEvidence(
        val dois: List<ResolutionEvidence>,
        val arxivIds: List<ResolutionEvidence>,
        val journalCitations: List<JournalCandidate>,
        val titleCandidates: List<TextCandidate>,
        val fontSizeClusters: List<FontSizeCluster>,
        val dominantBodyHeightPx: Double?,
        val analyzedLineCount: Int
    ) {
        val bestTitle: TextCandidate?
            get() = titleCandidates.maxWithOrNull(
                compareBy<TextCandidate> { it.score }
                    .thenBy { it.lineCount }
                    .thenBy { it.text.length }
            )

        val preferred: ResolutionEvidence?
            get() {
                dois.maxByOrNull(ResolutionEvidence::score)?.let { return it }
                arxivIds.maxByOrNull(ResolutionEvidence::score)?.let { return it }

                val bestJournal = journalCitations.maxByOrNull(JournalCandidate::score)
                val bestTitle = bestTitle

                // A complete journal citation is bibliographically stronger than
                // fuzzy title OCR, even when it is printed in a small header font.
                if (
                    bestJournal != null &&
                    isCompleteJournalCitation(bestJournal.citation) &&
                    bestJournal.score >= STRONG_JOURNAL_SCORE
                ) {
                    return ResolutionEvidence(
                        type = EvidenceType.JOURNAL_CITATION,
                        query = bestJournal.rawText,
                        score = bestJournal.score
                    )
                }

                if (bestTitle != null && bestTitle.score >= MIN_TITLE_SCORE) {
                    return ResolutionEvidence(
                        type = EvidenceType.TITLE,
                        query = bestTitle.text,
                        score = bestTitle.score
                    )
                }

                if (
                    bestJournal != null &&
                    bestJournal.score >= MIN_JOURNAL_SCORE &&
                    bestJournal.topRatio < 0.55
                ) {
                    return ResolutionEvidence(
                        type = EvidenceType.JOURNAL_CITATION,
                        query = bestJournal.rawText,
                        score = bestJournal.score
                    )
                }

                return null
            }
    }

    /** Pure layout input used by unit tests and by the ML Kit adapter. */
    data class LayoutLine(
        val text: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val blockIndex: Int = 0,
        val lineIndex: Int = 0
    ) {
        val width: Int get() = (right - left).coerceAtLeast(0)
        val height: Int get() = (bottom - top).coerceAtLeast(0)
        val centerX: Double get() = (left + right) / 2.0
    }

    private data class WorkingLine(
        val source: LayoutLine,
        val text: String,
        val height: Double,
        val clusterIndex: Int,
        val topRatio: Double,
        val widthRatio: Double
    )

    private data class MutableCluster(
        val heights: MutableList<Double> = mutableListOf(),
        var lineCount: Int = 0,
        var characterCount: Int = 0
    ) {
        val median: Double
            get() = medianOf(heights)
    }

    private val whitespaceRegex = Regex("\\s+")
    private val referenceHeadingRegex = Regex(
        pattern = """^(?:references?(?:\s+(?:and|&)\s+notes)?|reference\s+list|bibliography|bibliographic\s+references|works\s+cited|literature\s+cited|references?\s+(?:and|&)\s+further\s+reading)$""",
        option = RegexOption.IGNORE_CASE
    )
    private val sectionNumberRegex = Regex("""^\s*(?:\d+(?:\.\d+)*|[IVX]+)\s*[.)]?\s+""", RegexOption.IGNORE_CASE)
    private val authorMetadataRegex = Regex(
        """\b(?:university|department|institute|laboratory|laboratoire|collaboration|corresponding author|email|orcid)\b""",
        RegexOption.IGNORE_CASE
    )
    private val bodyLeadRegex = Regex(
        """^(?:the|this|we|in|for|from|our|these|it|a|an|as|using|recent|however|therefore|where|which)\b""",
        RegexOption.IGNORE_CASE
    )
    private val fourDigitYearRegex = Regex("""\b(?:19|20)\d{2}\b""")
    private val explicitIdentifierMetadataRegex = Regex(
        pattern = """^\s*(?:(?:arxiv|doi)\s*[:=]|https?://(?:www\.)?(?:arxiv\.org|doi\.org)/)""",
        option = RegexOption.IGNORE_CASE
    )

    private const val HEIGHT_CLUSTER_RELATIVE_TOLERANCE = 0.16
    private const val MIN_TITLE_SCORE = 0.56
    private const val MIN_JOURNAL_SCORE = 0.68
    private const val STRONG_JOURNAL_SCORE = 0.84

    fun analyze(
        result: Text,
        imageWidth: Int,
        imageHeight: Int
    ): DocumentEvidence {
        if (imageWidth <= 0 || imageHeight <= 0 || result.text.isBlank()) {
            return emptyEvidence()
        }

        val lines = result.textBlocks.flatMapIndexed { blockIndex, block ->
            block.lines.mapIndexedNotNull { lineIndex, line ->
                val box = line.boundingBox ?: return@mapIndexedNotNull null
                LayoutLine(
                    text = line.text,
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom,
                    blockIndex = blockIndex,
                    lineIndex = lineIndex
                )
            }
        }

        return analyzeLines(lines, imageWidth, imageHeight)
    }

    fun analyzeLines(
        inputLines: List<LayoutLine>,
        imageWidth: Int,
        imageHeight: Int
    ): DocumentEvidence {
        if (imageWidth <= 0 || imageHeight <= 0) return emptyEvidence()

        val cleanedLines = inputLines
            .mapNotNull { line ->
                val text = clean(line.text)
                if (text.isBlank() || line.height < 4 || line.width < 2) null
                else line.copy(text = text)
            }
            .sortedWith(compareBy<LayoutLine>({ it.top }, { it.left }))

        if (cleanedLines.isEmpty()) return emptyEvidence()

        val referenceTop = cleanedLines
            .firstOrNull { isReferenceHeading(it.text) }
            ?.top

        val lines = if (referenceTop != null) {
            cleanedLines.filter { it.top < referenceTop }
        } else {
            cleanedLines
        }

        if (lines.isEmpty()) return emptyEvidence()

        val clusters = clusterHeights(lines)
        val bodyClusterIndex = inferBodyClusterIndex(clusters)
        val bodyHeight = bodyClusterIndex?.let { clusters[it].medianHeightPx }

        val workingLines = lines.map { line ->
            val clusterIndex = closestClusterIndex(line.height.toDouble(), clusters)
            WorkingLine(
                source = line,
                text = line.text,
                height = line.height.toDouble(),
                clusterIndex = clusterIndex,
                topRatio = line.top.toDouble() / imageHeight.toDouble(),
                widthRatio = line.width.toDouble() / imageWidth.toDouble()
            )
        }

        val identifiers = buildIdentifierEvidence(workingLines)
        val journals = buildJournalEvidence(
            workingLines = workingLines,
            imageHeight = imageHeight
        )
        val titles = buildTitleEvidence(
            workingLines = workingLines,
            clusters = clusters,
            bodyClusterIndex = bodyClusterIndex,
            bodyHeight = bodyHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            journalCandidates = journals
        )

        return DocumentEvidence(
            dois = identifiers.first,
            arxivIds = identifiers.second,
            journalCitations = journals.sortedByDescending(JournalCandidate::score),
            titleCandidates = titles.sortedWith(
                compareByDescending<TextCandidate> { it.score }
                    .thenByDescending { it.lineCount }
                    .thenByDescending { it.text.length }
            ),
            fontSizeClusters = clusters,
            dominantBodyHeightPx = bodyHeight,
            analyzedLineCount = workingLines.size
        )
    }

    private fun buildIdentifierEvidence(
        lines: List<WorkingLine>
    ): Pair<List<ResolutionEvidence>, List<ResolutionEvidence>> {
        val dois = linkedMapOf<String, ResolutionEvidence>()
        val arxiv = linkedMapOf<String, ResolutionEvidence>()

        lines.forEachIndexed { index, line ->
            val earlyReadingOrderBoost = if (index < 8) 0.08 else 0.0
            val positionBoost = when {
                line.topRatio < 0.30 -> 0.10
                line.topRatio < 0.60 -> 0.04
                else -> 0.0
            }
            val explicitDoi = line.text.contains("doi", ignoreCase = true) ||
                    line.text.contains("doi.org", ignoreCase = true)
            val explicitArxiv = line.text.contains("arxiv", ignoreCase = true)

            ScholarlyIdentifiers.extractAllDois(line.text).forEach { doi ->
                val score = (0.86 + earlyReadingOrderBoost + positionBoost +
                        if (explicitDoi) 0.12 else 0.0).coerceAtMost(1.0)
                val candidate = ResolutionEvidence(EvidenceType.DOI, doi, score)
                if ((dois[doi]?.score ?: -1.0) < score) dois[doi] = candidate
            }

            ScholarlyIdentifiers.extractAllArxivIds(line.text).forEach { id ->
                val score = (0.86 + earlyReadingOrderBoost + positionBoost +
                        if (explicitArxiv) 0.12 else 0.0).coerceAtMost(1.0)
                val query = "arXiv:$id"
                val candidate = ResolutionEvidence(EvidenceType.ARXIV, query, score)
                if ((arxiv[query]?.score ?: -1.0) < score) arxiv[query] = candidate
            }
        }

        return dois.values.toList() to arxiv.values.toList()
    }

    private fun buildJournalEvidence(
        workingLines: List<WorkingLine>,
        imageHeight: Int
    ): List<JournalCandidate> {
        if (workingLines.isEmpty()) return emptyList()

        val results = linkedMapOf<String, JournalCandidate>()

        for (start in workingLines.indices) {
            for (count in 1..3) {
                val endExclusive = start + count
                if (endExclusive > workingLines.size) break

                val group = workingLines.subList(start, endExclusive)
                if (!areSpatiallyAdjacent(group)) continue

                val joined = group.joinToString(" ") { it.text }
                val top = group.minOf { it.source.top }
                val topRatio = top.toDouble() / imageHeight.toDouble()

                if (!looksLikeJournalCitationInput(joined, topRatio)) continue

                val citation = JournalCitationParser.parse(joined) ?: continue
                val completeness = journalCompleteness(citation)
                val compactness = if (joined.length <= 110) 0.08 else 0.0
                val headerBoost = when {
                    topRatio < 0.18 -> 0.18
                    topRatio < 0.35 -> 0.10
                    topRatio < 0.55 -> 0.03
                    else -> -0.08
                }
                val score = (
                        0.20 +
                                citation.confidence * 0.30 +
                                completeness * 0.25 +
                                compactness +
                                headerBoost
                        ).coerceIn(0.0, 1.0)

                val key = citation.toBibliographicQuery().lowercase(Locale.ROOT)
                val candidate = JournalCandidate(
                    rawText = joined,
                    citation = citation,
                    score = score,
                    topRatio = topRatio
                )
                if ((results[key]?.score ?: -1.0) < score) results[key] = candidate
            }
        }

        return results.values.toList()
    }

    private fun buildTitleEvidence(
        workingLines: List<WorkingLine>,
        clusters: List<FontSizeCluster>,
        bodyClusterIndex: Int?,
        bodyHeight: Double?,
        imageWidth: Int,
        imageHeight: Int,
        journalCandidates: List<JournalCandidate>
    ): List<TextCandidate> {
        if (workingLines.isEmpty()) return emptyList()

        val journalTexts = journalCandidates.map { normalizeComparison(it.rawText) }.toSet()
        val candidateGroups = mutableListOf<List<WorkingLine>>()

        // Single lines are important for journal titles such as the PRD example.
        workingLines.forEach { candidateGroups += listOf(it) }

        // Reconstruct wrapped titles from adjacent lines of similar visual size.
        for (start in workingLines.indices) {
            var group = listOf(workingLines[start])
            for (nextIndex in (start + 1)..min(start + 2, workingLines.lastIndex)) {
                val next = workingLines[nextIndex]
                val previous = group.last()
                if (!canBelongToSameVisualRegion(previous, next, imageWidth)) break
                group = group + next
                candidateGroups += group
            }
        }

        val distinctMeaningfulClusters = clusters.count { it.characterCount >= 8 }
        val bodyOnlyLookingFrame =
            distinctMeaningfulClusters <= 1 && workingLines.size >= 5

        return candidateGroups.mapNotNull { group ->
            val text = clean(group.joinToString(" ") { it.text })
            if (!isTitleLikeText(text)) return@mapNotNull null
            if (journalTexts.contains(normalizeComparison(text))) return@mapNotNull null
            // Explicit identifier metadata should never be demoted into a fuzzy
            // title query merely because OCR damaged one character or inserted
            // whitespace. If parsing succeeds, identifier evidence already owns
            // it; if parsing fails, let another visual region (usually the title)
            // drive lookup instead of searching the metadata line as prose.
            if (explicitIdentifierMetadataRegex.containsMatchIn(text)) return@mapNotNull null
            if (ScholarlyIdentifiers.looksLikeIdentifier(text)) return@mapNotNull null

            val averageHeight = group.map { it.height }.average()
            val sizeRatio = if (bodyHeight != null && bodyHeight > 0.0) {
                averageHeight / bodyHeight
            } else {
                val medianAll = medianOf(workingLines.map { it.height })
                if (medianAll > 0.0) averageHeight / medianAll else 1.0
            }

            val top = group.minOf { it.source.top }
            val left = group.minOf { it.source.left }
            val right = group.maxOf { it.source.right }
            val topRatio = top.toDouble() / imageHeight.toDouble()
            val widthRatio = (right - left).toDouble() / imageWidth.toDouble()
            val groupCluster = group.groupingBy { it.clusterIndex }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
            val isBodyCluster = bodyClusterIndex != null && groupCluster == bodyClusterIndex

            var score = 0.0

            score += when {
                sizeRatio >= 1.75 -> 0.44
                sizeRatio >= 1.45 -> 0.36
                sizeRatio >= 1.25 -> 0.27
                sizeRatio >= 1.10 -> 0.16
                else -> 0.04
            }

            score += when {
                topRatio < 0.18 -> 0.24
                topRatio < 0.34 -> 0.18
                topRatio < 0.52 -> 0.09
                topRatio < 0.70 -> 0.02
                else -> -0.08
            }

            score += when {
                widthRatio >= 0.58 -> 0.12
                widthRatio >= 0.36 -> 0.07
                else -> 0.0
            }

            score += when (text.length) {
                in 30..180 -> 0.13
                in 18..240 -> 0.07
                else -> -0.05
            }

            if (group.size in 2..3) score += 0.04 * (group.size - 1)
            if (looksLikeHeading(text)) score -= 0.12
            if (sizeRatio < 1.18 && looksLikeBodySentence(text)) score -= 0.22
            if (looksLikeAuthorMetadata(text)) score -= 0.25
            if (isBodyCluster) score -= 0.14
            if (bodyOnlyLookingFrame && sizeRatio < 1.20) score -= 0.32

            // A visually distinct largest cluster remains informative even when
            // there is no reliable body-size baseline in the current frame.
            val largestClusterHeight = clusters.maxOfOrNull { it.medianHeightPx }
            if (
                largestClusterHeight != null &&
                abs(averageHeight - largestClusterHeight) /
                max(largestClusterHeight, 1.0) <= 0.15 &&
                distinctMeaningfulClusters >= 2
            ) {
                score += 0.10
            }

            TextCandidate(
                text = text.take(300),
                score = score.coerceAtLeast(0.0),
                topRatio = topRatio,
                sizeRatioToBody = sizeRatio,
                lineCount = group.size
            )
        }
            .distinctBy { normalizeComparison(it.text) }
            .filter { it.score >= 0.30 }
    }

    private fun clusterHeights(lines: List<LayoutLine>): List<FontSizeCluster> {
        val mutable = mutableListOf<MutableCluster>()

        lines.sortedBy { it.height }.forEach { line ->
            val height = line.height.toDouble()
            val cluster = mutable.minByOrNull { existing ->
                abs(height - existing.median) / max(existing.median, 1.0)
            }

            if (
                cluster != null &&
                abs(height - cluster.median) / max(cluster.median, 1.0) <=
                HEIGHT_CLUSTER_RELATIVE_TOLERANCE
            ) {
                cluster.heights += height
                cluster.lineCount += 1
                cluster.characterCount += line.text.count(Char::isLetterOrDigit)
            } else {
                mutable += MutableCluster(
                    heights = mutableListOf(height),
                    lineCount = 1,
                    characterCount = line.text.count(Char::isLetterOrDigit)
                )
            }
        }

        return mutable
            .map {
                FontSizeCluster(
                    medianHeightPx = it.median,
                    lineCount = it.lineCount,
                    characterCount = it.characterCount
                )
            }
            .sortedBy(FontSizeCluster::medianHeightPx)
    }

    private fun inferBodyClusterIndex(
        clusters: List<FontSizeCluster>
    ): Int? {
        if (clusters.isEmpty()) return null

        // Body text is usually the size cluster carrying the largest amount of
        // prose, not necessarily the cluster with the largest number of OCR
        // boxes. Character count makes this robust to wrapped paragraph lines.
        return clusters.indices.maxByOrNull { index ->
            val cluster = clusters[index]
            val density = cluster.characterCount.toDouble() + cluster.lineCount * 8.0
            val tinyPenalty = if (
                cluster.medianHeightPx < clusters.map { it.medianHeightPx }.average() * 0.55
            ) 0.5 else 1.0
            density * tinyPenalty
        }
    }

    private fun closestClusterIndex(
        height: Double,
        clusters: List<FontSizeCluster>
    ): Int = clusters.indices.minByOrNull { index ->
        abs(height - clusters[index].medianHeightPx)
    } ?: 0

    private fun areSpatiallyAdjacent(group: List<WorkingLine>): Boolean {
        if (group.size <= 1) return true
        for (index in 1 until group.size) {
            val previous = group[index - 1]
            val current = group[index]
            val gap = current.source.top - previous.source.bottom
            val allowed = max(previous.height, current.height) * 1.8
            if (gap < -max(previous.height, current.height) || gap > allowed) return false
        }
        return true
    }

    private fun canBelongToSameVisualRegion(
        previous: WorkingLine,
        current: WorkingLine,
        imageWidth: Int
    ): Boolean {
        val gap = current.source.top - previous.source.bottom
        val maxHeight = max(previous.height, current.height)
        if (gap < -maxHeight * 0.35 || gap > maxHeight * 1.25) return false

        val heightDifference = abs(previous.height - current.height) / max(maxHeight, 1.0)
        if (heightDifference > 0.22) return false

        val overlap = horizontalOverlap(previous.source, current.source)
        val minWidth = min(previous.source.width, current.source.width).coerceAtLeast(1)
        val overlapRatio = overlap.toDouble() / minWidth.toDouble()
        val centersClose = abs(previous.source.centerX - current.source.centerX) <= imageWidth * 0.18

        return overlapRatio >= 0.25 || centersClose
    }

    private fun horizontalOverlap(first: LayoutLine, second: LayoutLine): Int =
        (min(first.right, second.right) - max(first.left, second.left)).coerceAtLeast(0)

    private fun looksLikeJournalCitationInput(
        text: String,
        topRatio: Double
    ): Boolean {
        if (text.length !in 8..200) return false
        if (topRatio >= 0.62) return false

        val digitCount = text.count(Char::isDigit)
        if (digitCount < 3) return false

        return fourDigitYearRegex.containsMatchIn(text) ||
                text.contains("vol.", ignoreCase = true) ||
                text.contains("volume", ignoreCase = true) ||
                (topRatio < 0.28 && digitCount >= 5)
    }

    private fun journalCompleteness(citation: JournalCitation): Double {
        var fields = 0
        if (citation.journal.isNotBlank()) fields += 1
        if (!citation.volume.isNullOrBlank()) fields += 1
        if (!citation.locator.isNullOrBlank()) fields += 1
        if (citation.year != null) fields += 1
        return fields / 4.0
    }

    private fun isCompleteJournalCitation(citation: JournalCitation): Boolean =
        citation.journal.isNotBlank() &&
                !citation.volume.isNullOrBlank() &&
                !citation.locator.isNullOrBlank() &&
                citation.year != null

    private fun looksLikeHeading(text: String): Boolean {
        val words = text.split(whitespaceRegex).filter(String::isNotBlank)
        if (words.size <= 6 && sectionNumberRegex.containsMatchIn(text)) return true

        val letters = text.filter(Char::isLetter)
        if (letters.length >= 5) {
            val upperRatio = letters.count(Char::isUpperCase).toDouble() / letters.length.toDouble()
            if (upperRatio >= 0.90 && words.size <= 8) return true
        }

        return text.trimEnd(':').lowercase(Locale.ROOT) in setOf(
            "abstract", "introduction", "methods", "results", "discussion",
            "conclusion", "conclusions", "acknowledgments", "acknowledgements"
        )
    }

    private fun looksLikeBodySentence(text: String): Boolean {
        val words = text.split(whitespaceRegex).filter(String::isNotBlank)
        if (words.size >= 13 && (text.endsWith('.') || text.endsWith(',') || text.endsWith(';'))) {
            return true
        }
        if (words.size >= 10 && bodyLeadRegex.containsMatchIn(text)) return true
        return false
    }

    private fun looksLikeAuthorMetadata(text: String): Boolean {
        if (authorMetadataRegex.containsMatchIn(text)) return true
        if ('@' in text) return true
        val words = text.split(whitespaceRegex).filter(String::isNotBlank)
        val commaCount = text.count { it == ',' }
        return words.size >= 4 && commaCount >= 3 && text.length < 180
    }

    private fun isTitleLikeText(text: String): Boolean {
        val letters = text.count(Char::isLetter)
        val words = text.split(whitespaceRegex).filter { it.any(Char::isLetter) }
        if (letters < 10 || words.size < 3 || text.length !in 12..320) return false
        if (text.contains("http://", true) || text.contains("https://", true)) return false
        return true
    }

    private fun isReferenceHeading(text: String): Boolean =
        referenceHeadingRegex.matches(
            clean(text).lowercase(Locale.ROOT).trimEnd(':', '.')
        )

    private fun clean(text: String): String =
        text.replace(whitespaceRegex, " ").trim()

    private fun normalizeComparison(text: String): String = clean(text)
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun emptyEvidence() = DocumentEvidence(
        dois = emptyList(),
        arxivIds = emptyList(),
        journalCitations = emptyList(),
        titleCandidates = emptyList(),
        fontSizeClusters = emptyList(),
        dominantBodyHeightPx = null,
        analyzedLineCount = 0
    )

    private fun medianOf(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }
}
