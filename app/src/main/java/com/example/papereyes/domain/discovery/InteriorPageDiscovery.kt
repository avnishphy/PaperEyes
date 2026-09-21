package com.example.papereyes.domain.discovery

import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.normalizePaperDoi
import com.example.papereyes.domain.evidence.TextFingerprint
import kotlinx.coroutines.CancellationException
import java.util.Locale

data class VerifiedCandidate(val paper: Paper, val independentMatches: Int, val providers: Set<String>)
data class InteriorDiscoveryResult(
    val candidates: List<VerifiedCandidate>,
    val verifiedPaper: Paper?,
    val unavailableProviders: Set<String> = emptySet()
)

/** Conservative initial verifier. No probabilistic confidence or rank-as-evidence. */
class InteriorPageDiscovery(private val providers: List<PaperDiscoveryProvider>) {
    suspend fun discover(evidence: RetrievalEvidence): InteriorDiscoveryResult {
        val phrases = independentPhrases(evidence.fingerprints)
        if (phrases.isEmpty()) return InteriorDiscoveryResult(emptyList(), null)
        val results = mutableListOf<PaperCandidate>()
        val failed = linkedSetOf<String>()
        for (provider in providers) {
            if (provider.capabilities.none { it == ProviderCapability.FULL_TEXT_PHRASE_SEARCH ||
                        it == ProviderCapability.ABSTRACT_SEARCH }) continue
            try {
                results += provider.searchEvidence(RetrievalEvidence(phrases)).take(10)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A provider outage is not a successful zero-result lookup.
                failed += provider.id
            }
        }
        val candidates = results.groupBy { candidate ->
            normalizePaperDoi(candidate.paper.doi)?.let { "doi:$it" }
                ?: candidate.paper.url?.takeIf { it.isNotBlank() }
                ?: candidate.paper.identityKey
        }.values.map { copies ->
            val texts = copies.flatMap { it.verificationTexts }.map(::tokens)
            val matched = phrases.count { phrase ->
                val needle = tokens(phrase.text)
                texts.any { containsPhrase(it, needle) }
            }
            VerifiedCandidate(copies.first().paper, matched, copies.map { it.provider }.toSet())
        }.sortedWith(compareByDescending<VerifiedCandidate> { it.independentMatches }
            .thenBy { it.paper.identityKey }).take(5)
        // Two plausible papers remains ambiguous, even if one has more hits.
        val verified = candidates.firstOrNull()?.takeIf {
            it.independentMatches >= 2 && candidates.drop(1).none { other -> other.independentMatches >= 2 }
        }?.paper
        return InteriorDiscoveryResult(candidates, verified, failed)
    }

    internal fun independentPhrases(input: List<TextFingerprint>): List<TextFingerprint> {
        val selected = mutableListOf<TextFingerprint>()
        for (phrase in input.sortedByDescending { it.score }.take(20)) {
            val words = tokens(phrase.text)
            if (words.size !in 6..15 || words.count { it.length >= 7 } < 2) continue
            if (selected.any { old ->
                    old.sourceGroup == phrase.sourceGroup || tokens(old.text).toSet().let { previous ->
                        previous.intersect(words.toSet()).size.toDouble() /
                            minOf(previous.size, words.toSet().size).coerceAtLeast(1) >= .6
                    }
                }) continue
            selected += phrase
            if (selected.size == 5) break
        }
        return selected
    }

    private fun tokens(text: String): List<String> = WORD.findAll(text.lowercase(Locale.ROOT))
        .map { it.value }.toList()
    private fun containsPhrase(haystack: List<String>, needle: List<String>): Boolean =
        needle.isNotEmpty() && haystack.size >= needle.size &&
            (0..haystack.size - needle.size).any { start ->
                needle.indices.all { offset -> haystack[start + offset] == needle[offset] }
            }
    private companion object { val WORD = Regex("[\\p{L}\\p{N}]+") }
}
