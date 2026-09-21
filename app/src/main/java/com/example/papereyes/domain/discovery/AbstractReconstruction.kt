package com.example.papereyes.domain.discovery

/** Reconstruct only contiguous, unambiguous positions; never bridge missing words. */
object AbstractReconstruction {
    fun segments(index: Map<String, List<Int>>?): List<String> {
        if (index.isNullOrEmpty() || index.size > 10_000) return emptyList()
        val words = sortedMapOf<Int, String>()
        val conflicts = hashSetOf<Int>()
        var count = 0
        for ((word, positions) in index) {
            if (word.isBlank() || word.length > 200 || positions.size > 10_000) return emptyList()
            for (position in positions) {
                if (++count > 20_000 || position !in 0..9_999) return emptyList()
                val existing = words.putIfAbsent(position, word)
                if (existing != null && existing != word) conflicts += position
            }
        }
        conflicts.forEach(words::remove)
        val result = mutableListOf<String>()
        val current = mutableListOf<String>()
        var previous = -2
        for ((position, word) in words) {
            if (position != previous + 1 && current.isNotEmpty()) {
                result += current.joinToString(" "); current.clear()
            }
            current += word
            previous = position
        }
        if (current.isNotEmpty()) result += current.joinToString(" ")
        return result
    }
}
