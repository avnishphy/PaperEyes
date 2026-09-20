package com.example.papereyes.data.remote

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PaperRepositoryNetworkTest {

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

        PaperRepository(api).searchPaper("test")
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

        val failure = runCatching { PaperRepository(api).searchPaper("test") }.exceptionOrNull()
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

        PaperRepository(api).searchPaper("test")
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

        assertTrue(PaperRepository(api).searchPaper("test").isEmpty())
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

        // Use the single-work endpoint here intentionally. List/search requests are
        // process-wide rate limited, which would make this cancellation test depend
        // on wall-clock state left by earlier tests. Both paths share the same retry
        // wrapper, so this isolates the cancellation behavior we actually care about.
        val job = async { PaperRepository(api).getPaperByDoi("10.1000/test") }
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
