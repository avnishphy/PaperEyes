package com.example.papereyes.domain

import com.example.papereyes.data.remote.SemanticScholarApi
import com.example.papereyes.data.remote.SemanticScholarAuthor
import com.example.papereyes.data.remote.SemanticScholarExternalIds
import com.example.papereyes.data.remote.SemanticScholarMatchResponse
import com.example.papereyes.data.remote.SemanticScholarPaper
import com.example.papereyes.data.remote.SemanticScholarPaperMatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperResolverFastTitleTest {

    @Test
    fun fastTitleMatchMapsSingleSemanticScholarResult() = runTest {
        val api = object : SemanticScholarApi {
            override suspend fun getPaper(
                paperId: String,
                fields: String
            ): SemanticScholarPaper = error("not used")

            override suspend fun searchPaperMatch(
                query: String,
                fields: String
            ): SemanticScholarMatchResponse {
                assertEquals("Deeply Virtual Compton Scattering", query)
                return SemanticScholarMatchResponse(
                    data = listOf(
                        SemanticScholarPaperMatch(
                            matchScore = 123.0,
                            title = "Deeply Virtual Compton Scattering",
                            year = 2025,
                            url = "https://www.semanticscholar.org/paper/test",
                            authors = listOf(
                                SemanticScholarAuthor("A. Researcher"),
                                SemanticScholarAuthor("B. Physicist")
                            ),
                            externalIds = SemanticScholarExternalIds(
                                doi = "10.1000/test.doi",
                                arxiv = "2501.01234"
                            )
                        )
                    )
                )
            }
        }

        val paper = PaperResolver(semanticScholarApi = api)
            .resolveFastTitle("  Deeply Virtual Compton Scattering  ")

        requireNotNull(paper)
        assertEquals("Deeply Virtual Compton Scattering", paper.title)
        assertEquals("A. Researcher, B. Physicist", paper.authors)
        assertEquals(2025, paper.year)
        assertEquals("10.1000/test.doi", paper.doi)
        assertEquals("https://arxiv.org/abs/2501.01234", paper.url)
    }

    @Test
    fun fastTitleMatchIsBestEffortOnServiceFailure() = runTest {
        val api = object : SemanticScholarApi {
            override suspend fun getPaper(
                paperId: String,
                fields: String
            ): SemanticScholarPaper = error("not used")

            override suspend fun searchPaperMatch(
                query: String,
                fields: String
            ): SemanticScholarMatchResponse = error("temporary failure")
        }

        assertNull(
            PaperResolver(semanticScholarApi = api)
                .resolveFastTitle("A valid paper title")
        )
    }

    @Test
    fun fastTitleMatchPropagatesCancellation() = runTest {
        val api = object : SemanticScholarApi {
            override suspend fun getPaper(
                paperId: String,
                fields: String
            ): SemanticScholarPaper = error("not used")

            override suspend fun searchPaperMatch(
                query: String,
                fields: String
            ): SemanticScholarMatchResponse {
                awaitCancellation()
            }
        }

        val job = async {
            PaperResolver(semanticScholarApi = api)
                .resolveFastTitle("A valid paper title")
        }

        testScheduler.runCurrent()
        job.cancel()
        val failure = runCatching { job.await() }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }
}
