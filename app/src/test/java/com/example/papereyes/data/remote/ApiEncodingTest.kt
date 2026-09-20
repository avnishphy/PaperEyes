package com.example.papereyes.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiEncodingTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After fun tearDown() {
        server.shutdown()
    }

    @Test
    fun semanticScholarEncodesLegacyArxivSlashAsPathData() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"title":"Paper","year":1999,"url":null,"authors":[],"externalIds":{"ArXiv":"hep-ph/9901234"}}"""
            ).addHeader("Content-Type", "application/json")
        )

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SemanticScholarApi::class.java)

        api.getPaper("ARXIV:hep-ph/9901234")
        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("ARXIV:hep-ph%2F9901234"))
        assertTrue(!path.contains("ARXIV:hep-ph/9901234"))
    }

    @Test
    fun arxivQueryEncodesLegacyIdAsQueryParameter() = runBlocking {
        server.enqueue(
            MockResponse().setBody("<feed></feed>")
                .addHeader("Content-Type", "application/atom+xml")
        )

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .build()
            .create(ArxivApi::class.java)

        api.queryById("hep-ph/9901234").close()
        val requestUrl = server.takeRequest().requestUrl!!
        assertEquals("hep-ph/9901234", requestUrl.queryParameter("id_list"))
    }
    @Test
    fun semanticScholarTitleMatchEncodesQueryAsQueryParameter() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"data":[{"matchScore":101.0,"title":"Paper","year":2026,"url":null,"authors":[],"externalIds":{}}]}"""
            ).addHeader("Content-Type", "application/json")
        )

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SemanticScholarApi::class.java)

        api.searchPaperMatch("Deep virtual Compton scattering & pions")
        val request = server.takeRequest()
        assertEquals("/paper/search/match", request.requestUrl?.encodedPath)
        assertEquals(
            "Deep virtual Compton scattering & pions",
            request.requestUrl?.queryParameter("query")
        )
    }

}
