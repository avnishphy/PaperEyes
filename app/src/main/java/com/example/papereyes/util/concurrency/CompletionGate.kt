package com.example.papereyes.util.concurrency

/** Tracks native asynchronous operations whose coroutine await can be cancelled.
 * Closing is deferred until completion callbacks release the last operation.
 */
class CompletionGate(private val closeResource: () -> Unit) {
    private var active = 0
    private var closing = false
    private var closed = false

    @Synchronized fun acquire(): Boolean {
        if (closing) return false
        active++
        return true
    }

    fun release() {
        val shouldClose = synchronized(this) {
            check(active > 0) { "Unbalanced resource completion" }
            active--
            markClosedIfReady()
        }
        if (shouldClose) closeResource()
    }

    fun close() {
        val shouldClose = synchronized(this) {
            closing = true
            markClosedIfReady()
        }
        if (shouldClose) closeResource()
    }

    private fun markClosedIfReady(): Boolean {
        if (!closing || active != 0 || closed) return false
        closed = true
        return true
    }
}
