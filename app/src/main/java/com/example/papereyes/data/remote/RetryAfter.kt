package com.example.papereyes.data.remote

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object RetryAfter {
    /** HTTP delta-seconds or RFC 1123 date. Null means malformed/absent. */
    fun millis(value: String?, nowMillis: Long = System.currentTimeMillis()): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (text.all { it in '0'..'9' }) {
            val seconds = text.toLongOrNull() ?: return Long.MAX_VALUE
            return if (seconds > Long.MAX_VALUE / 1000) Long.MAX_VALUE else seconds * 1000
        }
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("GMT")
            }
            val position = java.text.ParsePosition(0)
            val date = format.parse(text, position) ?: return null
            if (position.index != text.length) return null
            (date.time - nowMillis).coerceAtLeast(0)
        } catch (_: IllegalArgumentException) { null }
    }
}
