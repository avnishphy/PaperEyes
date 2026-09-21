package com.example.papereyes.domain

import java.io.IOException

/** Detects transport failures even when a resolver wraps the original exception. */
fun Throwable.isConnectivityFailure(): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (current is IOException) return true
        current = current?.cause ?: return false
    }
    return false
}

private const val MAX_CAUSE_DEPTH = 8
