package com.example.papereyes.ui.live

import com.example.papereyes.ocr.TextCandidateExtractor

/**
 * Small non-UI state machine for deciding when a live OCR title is stable
 * enough to query and for suppressing immediate duplicate network requests.
 */
class LiveScanCandidateGate(
    private val stableObservationsRequired: Int = 2,
    private val sameQueryCooldownMs: Long = 1_500L
) {
    private var lastCandidate: String = ""
    private var stableCount: Int = 0
    private var lastLookupCandidate: String = ""
    private var lastLookupAtMs: Long = 0L

    @Synchronized
    fun observe(candidate: String): Boolean {
        val cleaned = candidate.trim()
        if (cleaned.isBlank()) {
            lastCandidate = ""
            stableCount = 0
            return false
        }

        stableCount =
            if (
                lastCandidate.isNotBlank() &&
                TextCandidateExtractor.areSimilar(lastCandidate, cleaned)
            ) {
                stableCount + 1
            } else {
                lastCandidate = cleaned
                1
            }

        return stableCount >= stableObservationsRequired
    }

    @Synchronized
    fun canLookup(candidate: String, nowMs: Long): Boolean {
        if (lastLookupCandidate.isBlank()) return true

        val sameCandidate = TextCandidateExtractor.areSimilar(
            lastLookupCandidate,
            candidate
        )

        return !sameCandidate || nowMs - lastLookupAtMs >= sameQueryCooldownMs
    }

    @Synchronized
    fun markLookup(candidate: String, nowMs: Long) {
        lastLookupCandidate = candidate.trim()
        lastLookupAtMs = nowMs
    }

    @Synchronized
    fun resetCandidate() {
        lastCandidate = ""
        stableCount = 0
    }

    @Synchronized
    fun reset() {
        lastCandidate = ""
        stableCount = 0
        lastLookupCandidate = ""
        lastLookupAtMs = 0L
    }
}
