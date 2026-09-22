package com.example.papereyes.domain.evidence

import java.util.Locale

/**
 * Combines two independently OCR'd views of the same reference list.
 * Numbered entries are keyed by their printed label; unnumbered entries use
 * conservative token overlap so OCR variations do not inflate the count.
 */
fun mergeReferenceEvidence(
    first: List<ReferenceEvidence>,
    second: List<ReferenceEvidence>
): List<ReferenceEvidence> {
    val merged = first.toMutableList()

    second.forEach { candidate ->
        val existingIndex = merged.indexOfFirst { existing ->
            sameReference(existing, candidate)
        }

        if (existingIndex < 0) {
            merged += candidate
        } else if (quality(candidate) > quality(merged[existingIndex])) {
            merged[existingIndex] = candidate
        }
    }

    return merged
}

private fun sameReference(
    first: ReferenceEvidence,
    second: ReferenceEvidence
): Boolean {
    val firstLabel = first.label?.trim()?.takeIf(String::isNotEmpty)
    val secondLabel = second.label?.trim()?.takeIf(String::isNotEmpty)

    if (firstLabel != null && secondLabel != null) {
        if (firstLabel == secondLabel) return true
        return tokenSimilarity(first.text, second.text) >= LABEL_DRIFT_SIMILARITY
    }

    return tokenSimilarity(first.text, second.text) >= MIN_TOKEN_SIMILARITY
}

private fun tokenSimilarity(first: String, second: String): Double {
    val firstTokens = tokens(first)
    val secondTokens = tokens(second)
    if (firstTokens.isEmpty() || secondTokens.isEmpty()) return 0.0

    val shared = firstTokens intersect secondTokens
    val union = firstTokens union secondTokens
    if (shared.size < MIN_SHARED_TOKENS) return 0.0
    return shared.size.toDouble() / union.size
}

private fun tokens(text: String): Set<String> = text
    .lowercase(Locale.ROOT)
    .split(nonAlphaNumeric)
    .asSequence()
    .filter { it.length >= MIN_TOKEN_LENGTH }
    .toSet()

private fun quality(reference: ReferenceEvidence): Int {
    val structuredBonus = when {
        reference.query.startsWith("10.") -> 2_000
        reference.query.startsWith("arXiv:", ignoreCase = true) -> 2_000
        else -> 0
    }
    return structuredBonus + reference.text.length.coerceAtMost(1_000)
}

private val nonAlphaNumeric = Regex("[^\\p{L}\\p{N}]+")
private const val MIN_TOKEN_LENGTH = 3
private const val MIN_SHARED_TOKENS = 3
private const val MIN_TOKEN_SIMILARITY = 0.62
private const val LABEL_DRIFT_SIMILARITY = 0.85
