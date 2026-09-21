package com.example.papereyes.util.concurrency

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProviderRateLimitedException(val retryInMillis: Long) : Exception(
    "This scholarly service is temporarily rate-limited. Please try again later."
)

/** Single connection, completion-to-start pacing and persistent in-process cooldown.
 * Long server cooldowns fail fast rather than holding a screen coroutine for hours.
 */
class RequestGate(
    private val intervalMillis: Long,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val sleep: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutex = Mutex()
    private var nextRequest = Long.MIN_VALUE
    @Volatile private var cooldownUntil = Long.MIN_VALUE
    init { require(intervalMillis >= 0) }

    @Synchronized fun coolDown(millis: Long) {
        val now = clockMillis()
        val positive = millis.coerceAtLeast(0)
        val deadline = if (positive > Long.MAX_VALUE - now.coerceAtLeast(0)) Long.MAX_VALUE else now + positive
        cooldownUntil = maxOf(cooldownUntil, deadline)
    }

    suspend fun <T> execute(block: suspend () -> T): T = mutex.withLock {
        val now = clockMillis()
        if (cooldownUntil > now && cooldownUntil - now > 5_000L) {
            throw ProviderRateLimitedException(cooldownUntil - now)
        }
        val until = maxOf(nextRequest, cooldownUntil)
        if (until > now) sleep(until - now)
        try { block() } finally { nextRequest = clockMillis() + intervalMillis }
    }
}
