package com.example.papereyes.util.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Per-resolver bounded, short-lived cache. The owning coroutine owns the load.
 * Exceptions and cancelled loads are never cached. Serial loads deduplicate
 * repeated live evidence without launching an orphan/background coroutine.
 */
class SuspendQueryCache<K : Any, V : Any>(
    private val capacity: Int = 64,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val ttlMillis: (V) -> Long = { 30_000L }
) {
    private data class Entry<V>(val value: V, val expires: Long)
    private val mutex = Mutex()
    private val entries = LinkedHashMap<K, Entry<V>>(16, .75f, true)
    init { require(capacity > 0) }

    suspend fun getOrLoad(key: K, load: suspend () -> V): V = mutex.withLock {
        val now = clockMillis()
        entries.entries.removeAll { it.value.expires <= now }
        entries[key]?.let { return@withLock it.value }
        val value = load()
        val ttl = ttlMillis(value).coerceIn(0L, 60_000L)
        if (ttl > 0) {
            entries[key] = Entry(value, clockMillis() + ttl)
            while (entries.size > capacity) entries.remove(entries.keys.first())
        }
        value
    }
}
