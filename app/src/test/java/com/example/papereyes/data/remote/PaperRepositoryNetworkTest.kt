package com.example.papereyes.data.remote

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import com.example.papereyes.util.concurrency.RequestGate
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PaperRepositoryNetworkTest {
    // Isolated gates retain pacing/cooldown behavior without order-dependent
    // shared state. Both gate clock and delay now use the same test scheduler.
    private fun TestScope.repository(api: CrossrefApi): PaperRepository = PaperRepository(
        api, ScholarlyRequestPolicy { provider ->
            RequestGate(if (provider == ScholarlyProvider.ARXIV) 3000 else 1050,
                clockMillis = { testScheduler.currentTime })
        }
    )


    @Test
    fun transientIOExceptionIsRetriedOnce() = runTest {
        var attempts = 0
        val api = object : CrossrefApi {
            override suspend fun searchWorks(
                query: String?, containerTitle: String?, filter: String?, rows: Int, select: String
            ): CrossrefResponse {
                attempts++
                if (attempts == 1) throw IOException("reset")
                return CrossrefResponse(CrossrefMessage(emptyList()))
            }

            override suspend fun getWorkByDoi(doi: String): CrossrefSingleResponse =
                error("not used")
        }

        repository(api).searchPaper("test")
        assertEquals(2, attempts)
    }

    @Test
    fun longRetryAfterIsNotViolated() = runTest {
        var attempts = 0
        val api = object : CrossrefApi {
            override suspend fun searchWorks(
                query: String?, containerTitle: String?, filter: String?, rows: Int, select: String
            ): CrossrefResponse {
                attempts++
                throw tooManyRequests(retryAfterSeconds = 30)
            }

            override suspend fun getWorkByDoi(doi: String): CrossrefSingleResponse =
                error("not used")
        }

        val failure = runCatching { repository(api).searchPaper("test") }.exceptionOrNull()
        assertTrue(failure is HttpException)
        assertEquals(1, attempts)
    }

    @Test
    fun shortRetryAfterIsHonoredAndRetried() = runTest {
        var attempts = 0
        val api = object : CrossrefApi {
            override suspend fun searchWorks(
                query: String?, containerTitle: String?, filter: String?, rows: Int, select: String
            ): CrossrefResponse {
                attempts++
                if (attempts == 1) throw tooManyRequests(retryAfterSeconds = 2)
                return CrossrefResponse(CrossrefMessage(emptyList()))
            }

            override suspend fun getWorkByDoi(doi: String): CrossrefSingleResponse =
                error("not used")
        }

        repository(api).searchPaper("test")
        assertEquals(2, attempts)
    }


    @Test
    fun missingCrossrefMessageIsTreatedAsEmptySearchResult() = runTest {
        val api = object : CrossrefApi {
            override suspend fun searchWorks(
                query: String?, containerTitle: String?, filter: String?, rows: Int, select: String
            ): CrossrefResponse = CrossrefResponse(message = null)

            override suspend fun getWorkByDoi(doi: String): CrossrefSingleResponse =
                CrossrefSingleResponse(message = null)
        }

        assertTrue(repository(api).searchPaper("test").isEmpty())
    }

    @Test
    fun cancellationPropagatesWithoutBeingMappedOrRetried() = runTest {
        var attempts = 0
        val api = object : CrossrefApi {
            override suspend fun searchWorks(
                query: String?, containerTitle: String?, filter: String?, rows: Int, select: String
            ): CrossrefResponse = error("not used")

            override suspend fun getWorkByDoi(doi: String): CrossrefSingleResponse {
                attempts++
                awaitCancellation()
            }
        }

        // A fresh policy makes this cancellation test independent of earlier calls.
        val job = async { repository(api).getPaperByDoi("10.1000/test") }
        testScheduler.runCurrent()
        assertEquals(1, attempts)

        job.cancel()
        val failure = runCatching { job.await() }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertEquals(1, attempts)
    }

    private fun tooManyRequests(retryAfterSeconds: Int): HttpException {
        val raw = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://api.crossref.org/works").build())
            .protocol(Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests")
            .header("Retry-After", retryAfterSeconds.toString())
            .body("".toResponseBody())
            .build()

        return HttpException(
            Response.error<CrossrefResponse>("".toResponseBody(), raw)
        )
    }
}
