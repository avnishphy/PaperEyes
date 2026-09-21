package com.example.papereyes.ocr

/**
 * Power-of-two sampling using ceiling division, without intermediate overflow.
 * Int can represent at most 2^30 as a positive power of two; only pathological
 * dimensions needing a larger sample saturate at that limit.
 */
fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
    val longest = maxOf(width, height).toLong()
    var sample = 1
    while ((longest + sample - 1) / sample > maxDimension && sample < (1 shl 30)) {
        sample *= 2
    }
    return sample
}
