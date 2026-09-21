package com.example.papereyes.domain.reference

import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.PaperResolveResult
import com.example.papereyes.domain.PaperResolver
import com.example.papereyes.domain.ResolutionStatus
import com.example.papereyes.domain.evidence.ReferenceEvidence
import java.util.Locale
import kotlinx.coroutines.CancellationException

enum class ReferenceResolutionStatus {
    IDENTIFIED,
    AMBIGUOUS,
    NOT_FOUND,
    PROVIDERS_UNAVAILABLE,
    ERROR
}

data class ReferenceResolution(
    val reference: ReferenceEvidence,
    val status: ReferenceResolutionStatus,
    val paper: Paper? = null,
    val candidates: List<Paper> = emptyList(),
    val message: String? = null
)

data class ReferenceBatchProgress(
    val completed: Int,
    val total: Int,
    val latest: ReferenceResolution
)

/**
 * Resolves selected references in reading order. A progress event is emitted
 * after every item so partial failures never disappear behind the final batch.
 */
class ReferenceBatchResolver(
    private val resolveQuery: suspend (String) -> PaperResolveResult
) {
    constructor(resolver: PaperResolver) : this(resolver::resolve)

    suspend fun resolveAll(
        references: List<ReferenceEvidence>,
        onProgress: (ReferenceBatchProgress) -> Unit = {}
    ): List<ReferenceResolution> {
        val selected = references.take(MAX_BATCH_SIZE)
        val outcomes = ArrayList<ReferenceResolution>(selected.size)

        selected.forEach { reference ->
            val outcome = try {
                classify(reference, resolveQuery(reference.query))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReferenceResolution(
                    reference = reference,
                    status = ReferenceResolutionStatus.ERROR,
                    message = error.message?.take(MAX_ERROR_CHARS)
                )
            }

            outcomes += outcome
            onProgress(ReferenceBatchProgress(outcomes.size, selected.size, outcome))
        }

        return outcomes
    }

    private fun classify(
        reference: ReferenceEvidence,
        result: PaperResolveResult
    ): ReferenceResolution {
        val candidates = result.papers.distinctBy(::paperKey).take(MAX_CANDIDATES_PER_REFERENCE)

        if (result.status == ResolutionStatus.VERIFIED && candidates.isNotEmpty()) {
            return ReferenceResolution(
                reference,
                ReferenceResolutionStatus.IDENTIFIED,
                candidates.first(),
                candidates
            )
        }

        if (candidates.isNotEmpty()) {
            val exactMatches = candidates.filter { titleMatches(reference.query, it.title) }
            if (exactMatches.size == 1) {
                return ReferenceResolution(
                    reference,
                    ReferenceResolutionStatus.IDENTIFIED,
                    exactMatches.single(),
                    candidates
                )
            }
            return ReferenceResolution(
                reference,
                ReferenceResolutionStatus.AMBIGUOUS,
                candidates = candidates,
                message = "Multiple possible papers were found."
            )
        }

        return when (result.status) {
            ResolutionStatus.PROVIDERS_UNAVAILABLE -> ReferenceResolution(
                reference,
                ReferenceResolutionStatus.PROVIDERS_UNAVAILABLE,
                message = "Scholarly services were unavailable."
            )
            ResolutionStatus.UNCERTAIN, ResolutionStatus.CANDIDATES -> ReferenceResolution(
                reference,
                ReferenceResolutionStatus.AMBIGUOUS,
                message = "No confident match was found."
            )
            else -> ReferenceResolution(
                reference,
                ReferenceResolutionStatus.NOT_FOUND,
                message = "No matching paper was found."
            )
        }
    }

    private fun titleMatches(query: String, title: String): Boolean {
        val normalizedQuery = normalize(query)
        val normalizedTitle = normalize(title)
        if (normalizedQuery.isBlank() || normalizedTitle.isBlank()) return false
        if (normalizedQuery == normalizedTitle) return true

        val titleWords = normalizedTitle.split(' ').filter { it.length >= 3 }
        return titleWords.size >= 3 && normalizedQuery.contains(normalizedTitle)
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(nonAlphaNumeric, " ")
        .replace(whitespace, " ")
        .trim()

    private fun paperKey(paper: Paper): String =
        paper.doi?.lowercase(Locale.ROOT) ?: "${normalize(paper.title)}:${paper.year.orEmpty()}"

    private fun Int?.orEmpty(): String = this?.toString().orEmpty()

    companion object {
        const val MAX_BATCH_SIZE = 50
        private const val MAX_CANDIDATES_PER_REFERENCE = 10
        private const val MAX_ERROR_CHARS = 240
        private val whitespace = Regex("\\s+")
        private val nonAlphaNumeric = Regex("[^\\p{L}\\p{N}]+")
    }
}
