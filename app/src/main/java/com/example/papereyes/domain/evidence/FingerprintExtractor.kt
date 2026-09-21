package com.example.papereyes.domain.evidence

import java.util.Locale
import kotlin.math.ln

/** Deterministic lexical specificity proxy, NOT measured corpus token rarity. */
object FingerprintExtractor {
    private val tokens = Regex("[\\p{L}][\\p{L}\\p{N}'-]*")
    private val stopWords = ("a an and are as at be been being by can could did do does for from had has have " +
        "he her here his how in into is it its may more most not of on or our out over same shall she should " +
        "so some such than that the their them then there these they this those through to under up use used " +
        "using was we were what when where which who will with would shown results figure table paper study").split(' ').toSet()
    private val captionStart = Regex("(?i)^(?:fig(?:ure)?\\.?|table)\\s*\\d+[.:]?\\s*")
    private val generic = Regex("(?i)^(?:the results are shown|as shown in|in this paper we|we present the results)")
    private val sentenceBreak = Regex("(?<=[.!?])\\s+(?=[\\p{Lu}])|\\n\\s*\\n")

    fun normalizedTokens(text: String): List<String> = tokens.findAll(text.lowercase(Locale.ROOT)).map { it.value }.toList()

    fun extract(blocks: List<String>, limit: Int = 5): List<TextFingerprint> {
        if (limit <= 0) return emptyList()
        var group = 0
        val candidates = mutableListOf<TextFingerprint>()
        for (block in blocks.take(80)) {
            // Block boundaries preserve columns; sentence windows never cross them.
            val blockCaption = captionStart.containsMatchIn(block.trim())
            val content = block.take(6000).trim().replace(captionStart, "")
            for ((sentenceIndex, sentence) in content.split(sentenceBreak).withIndex()) {
                val sourceGroup = group++
                val clean = sentence.replace(Regex("\\s+"), " ").trim()
                val caption = (blockCaption && sentenceIndex == 0) || captionStart.containsMatchIn(clean)
                val prose = clean.replace(captionStart, "")
                if (generic.containsMatchIn(prose)) continue
                if (prose.isEmpty() || prose.count { it.isDigit() }.toDouble() / prose.length > 0.16) continue
                if (prose.count { it in "=∫∑{}^_<>" } > 2) continue
                if (Regex("\\[\\s*\\d+").findAll(prose).count() > 1) continue
                val words = normalizedTokens(prose).take(140)
                if (words.size < 6) continue
                // One best window per sentence. Overlapping windows are never counted twice.
                val windows = (6..minOf(15, words.size)).flatMap { size -> words.windowed(size) }
                val best = windows.mapNotNull { window ->
                    val content = window.filterNot { it in stopWords }
                    if (content.size < 4 || content.size.toDouble() / window.size < 0.5) return@mapNotNull null
                    if (window.toSet().size.toDouble() / window.size < 0.75) return@mapNotNull null
                    val longWords = content.count { it.length >= 7 }
                    if (longWords < 2) return@mapNotNull null
                    val specificity = content.sumOf { ln(1.0 + it.length) } / window.size
                    val score = specificity + longWords * 0.10 + (if (caption) 0.3 else 0.0) + minOf(window.size, 10) * 0.02
                    TextFingerprint(window.joinToString(" "), sourceGroup, score, caption)
                }.maxByOrNull { it.score }
                best?.let(candidates::add)
            }
        }
        val selected = mutableListOf<TextFingerprint>()
        for (candidate in candidates.sortedWith(compareByDescending<TextFingerprint> { it.score }.thenBy { it.sourceGroup })) {
            val words = normalizedTokens(candidate.text).toSet()
            if (selected.any { previous ->
                val other = normalizedTokens(previous.text).toSet()
                words.intersect(other).size.toDouble() / maxOf(1, minOf(words.size, other.size)) >= 0.6
            }) continue
            selected += candidate
            if (selected.size >= minOf(limit, 5)) break
        }
        return selected
    }
}
