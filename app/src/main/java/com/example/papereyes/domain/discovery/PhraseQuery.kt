package com.example.papereyes.domain.discovery

import com.example.papereyes.domain.evidence.TextFingerprint
import java.net.URLEncoder

object PhraseQuery {
    /** Remove operators from OCR and enforce an encoded URL budget, not just characters. */
    fun build(fingerprints: List<TextFingerprint>): String {
        val selected = mutableListOf<String>()
        for (phrase in fingerprints.take(5)) {
            val words = Regex("[\\p{L}\\p{N}]+").findAll(phrase.text).map { it.value }.toList()
            if (words.size !in 6..15) continue
            val quoted = "\"${words.joinToString(" ")}\""
            val next = (selected + quoted).joinToString(" OR ")
            if (URLEncoder.encode(next, "UTF-8").length <= 3000) selected += quoted
            if (selected.size == 3) break
        }
        return selected.joinToString(" OR ")
    }
}
