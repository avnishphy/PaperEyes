package com.example.papereyes.ui.live

internal fun clampZoomRatio(
    requested: Float,
    reportedMin: Float,
    reportedMax: Float
): Float {
    val minimum = reportedMin.takeIf { it.isFinite() && it > 0f } ?: 1f
    val maximum = reportedMax.takeIf { it.isFinite() && it >= minimum } ?: minimum
    val value = requested.takeIf(Float::isFinite) ?: minimum
    return value.coerceIn(minimum, maximum)
}

internal fun scaleZoomRatio(
    current: Float,
    scaleFactor: Float,
    reportedMin: Float,
    reportedMax: Float
): Float {
    val safeScale = scaleFactor.takeIf { it.isFinite() && it > 0f } ?: 1f
    return clampZoomRatio(current * safeScale, reportedMin, reportedMax)
}
