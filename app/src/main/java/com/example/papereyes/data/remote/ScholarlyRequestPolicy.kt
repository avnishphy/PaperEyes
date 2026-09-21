package com.example.papereyes.data.remote

import com.example.papereyes.util.concurrency.RequestGate
import retrofit2.HttpException

enum class ScholarlyProvider { CROSSREF, ARXIV, SEMANTIC_SCHOLAR, INSPIRE, OPENALEX }

/** Process-wide pacing; no key, paid proxy, or background retry loop. */
class ScholarlyRequestPolicy(
    gateFactory: (ScholarlyProvider) -> RequestGate = { provider ->
        RequestGate(if (provider == ScholarlyProvider.ARXIV) 3_000L else 1_050L)
    }
) {
    private val gates = ScholarlyProvider.entries.associateWith(gateFactory)
    companion object { val shared = ScholarlyRequestPolicy() }
    suspend fun <T> request(provider: ScholarlyProvider, action: suspend () -> T): T {
        val gate = gates.getValue(provider)
        return gate.execute {
            try { action() } catch (error: HttpException) {
                if (error.code() == 429) {
                    val headers = error.response()?.headers()
                    val retry = RetryAfter.millis(headers?.get("Retry-After"))
                    val reset = if (provider == ScholarlyProvider.OPENALEX &&
                        headers?.get("X-RateLimit-Remaining")?.toDoubleOrNull()?.let { it <= 0.0 } == true
                    ) RetryAfter.millis(headers["X-RateLimit-Reset"]) else null
                    gate.coolDown(maxOf(retry ?: 60_000L, reset ?: 0L))
                }
                throw error
            }
        }
    }
}
