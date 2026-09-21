package com.example.papereyes.domain

import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.remote.CrossrefApi
import com.example.papereyes.data.remote.CrossrefItem
import com.example.papereyes.data.remote.CrossrefResponse
import com.example.papereyes.data.remote.CrossrefSingleResponse
import com.example.papereyes.data.remote.PaperRepository
import com.example.papereyes.data.remote.ScholarlyRequestPolicy
import com.example.papereyes.util.concurrency.RequestGate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PaperResolverTitleFallbackTest {

    @Test
    fun openAlexAddsExactConferenceTitleWhenCrossrefHasOnlyUncertainCandidates() = runBlocking {
        val crossrefApi = object : CrossrefApi {
            override suspend fun searchWorks(
                query: String?, containerTitle: String?, filter: String?, rows: Int, select: String
            ) = CrossrefResponse(
                message = com.example.papereyes.data.remote.CrossrefMessage(
                    listOf(CrossrefItem(
                        title = listOf("Dropout methods in neural networks"),
                        author = null, doi = null, url = null,
                        containerTitle = null, shortContainerTitle = null,
                        volume = null, issue = null, page = null, articleNumber = null,
                        publishedPrint = null, publishedOnline = null, published = null
                    ))
                )
            )

            override suspend fun getWorkByDoi(doi: String): CrossrefSingleResponse = error("unused")
        }
        val policy = ScholarlyRequestPolicy { RequestGate(0) }
        val exact = Paper(
            title = "Regularization of Neural Networks using DropConnect",
            authors = "Li Wan et al.", year = 2013, doi = null,
            url = "https://openalex.org/W123"
        )
        val resolver = PaperResolver(
            crossrefRepository = PaperRepository(crossrefApi, policy),
            requestPolicy = policy,
            openAlexTitleSearch = { listOf(exact) }
        )

        val result = resolver.resolve("Regularization of Neural Networks using DropConnect")

        assertEquals(PaperInputType.TITLE_OR_OCR, result.inputType)
        assertEquals(exact, result.papers.single { it.title == exact.title })
    }
}
