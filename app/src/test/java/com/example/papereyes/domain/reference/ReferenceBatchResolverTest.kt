package com.example.papereyes.domain.reference

import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.PaperInputType
import com.example.papereyes.domain.PaperResolveResult
import com.example.papereyes.domain.ResolutionStatus
import com.example.papereyes.domain.evidence.ReferenceEvidence
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferenceBatchResolverTest {

    @Test
    fun preservesOrderAndReportsProgressForEverySelectedReference() = runBlocking {
        val references = listOf(reference("1", "first"), reference("2", "second"))
        val progress = mutableListOf<ReferenceBatchProgress>()
        val resolver = ReferenceBatchResolver { query ->
            PaperResolveResult(
                PaperInputType.DOI,
                listOf(paper(query)),
                ResolutionStatus.VERIFIED
            )
        }

        val outcomes = resolver.resolveAll(references, progress::add)

        assertEquals(references, outcomes.map { it.reference })
        assertEquals(listOf(1, 2), progress.map { it.completed })
        assertEquals(listOf(2, 2), progress.map { it.total })
        assertEquals(listOf("first", "second"), outcomes.map { it.paper?.title })
    }

    @Test
    fun classifiesPartialFailureWithoutDiscardingSuccess() = runBlocking {
        val resolver = ReferenceBatchResolver { query ->
            when (query) {
                "found" -> PaperResolveResult(
                    PaperInputType.DOI,
                    listOf(paper("Found paper")),
                    ResolutionStatus.VERIFIED
                )
                "offline" -> PaperResolveResult(
                    PaperInputType.TITLE_OR_OCR,
                    emptyList(),
                    ResolutionStatus.PROVIDERS_UNAVAILABLE
                )
                else -> PaperResolveResult(PaperInputType.TITLE_OR_OCR, emptyList())
            }
        }

        val outcomes = resolver.resolveAll(
            listOf(reference("1", "found"), reference("2", "missing"), reference("3", "offline"))
        )

        assertEquals(
            listOf(
                ReferenceResolutionStatus.IDENTIFIED,
                ReferenceResolutionStatus.NOT_FOUND,
                ReferenceResolutionStatus.PROVIDERS_UNAVAILABLE
            ),
            outcomes.map { it.status }
        )
        assertEquals("Found paper", outcomes.first().paper?.title)
        assertNull(outcomes[1].paper)
    }

    @Test
    fun uniqueExactTitleCanResolveCandidateListButNearResultsStayAmbiguous() = runBlocking {
        val exactTitle = "Regularization of Neural Networks using DropConnect"
        val resolver = ReferenceBatchResolver { query ->
            PaperResolveResult(
                PaperInputType.TITLE_OR_OCR,
                listOf(paper(exactTitle), paper("Regularization in Deep Learning")),
                ResolutionStatus.CANDIDATES
            )
        }

        val exact = resolver.resolveAll(listOf(reference("1", exactTitle))).single()
        val vague = resolver.resolveAll(listOf(reference("2", "Regularization neural networks"))).single()

        assertEquals(ReferenceResolutionStatus.IDENTIFIED, exact.status)
        assertEquals(exactTitle, exact.paper?.title)
        assertEquals(ReferenceResolutionStatus.AMBIGUOUS, vague.status)
    }

    @Test
    fun exceptionsArePerReferenceButCancellationEscapes() = runBlocking {
        val errorResolver = ReferenceBatchResolver { error("network down") }
        val error = errorResolver.resolveAll(listOf(reference("1", "query"))).single()
        assertEquals(ReferenceResolutionStatus.ERROR, error.status)
        assertEquals("network down", error.message)

        val cancellingResolver = ReferenceBatchResolver { throw CancellationException("leave screen") }
        var cancelled = false
        try {
            cancellingResolver.resolveAll(listOf(reference("1", "query")))
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertEquals(true, cancelled)
    }

    @Test
    fun batchAndCandidateListsAreBounded() = runBlocking {
        val resolver = ReferenceBatchResolver {
            PaperResolveResult(
                PaperInputType.TITLE_OR_OCR,
                (1..20).map { paper("Candidate $it") },
                ResolutionStatus.CANDIDATES
            )
        }
        val outcomes = resolver.resolveAll((1..60).map { reference(it.toString(), "query $it") })

        assertEquals(ReferenceBatchResolver.MAX_BATCH_SIZE, outcomes.size)
        assertEquals(10, outcomes.first().candidates.size)
    }

    private fun reference(label: String, query: String) =
        ReferenceEvidence(label, "Reference $label", query)

    private fun paper(title: String) = Paper(
        title = title,
        authors = "A. Author",
        year = 2020,
        doi = null,
        url = null
    )
}
