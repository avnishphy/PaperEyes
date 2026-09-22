package com.example.papereyes.ocr

import com.example.papereyes.domain.evidence.OcrLine

internal data class NormalizedCrop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/** Plans a tighter second OCR pass from text already located in the full frame. */
internal object ReferenceCropPlanner {

    fun plan(
        lines: List<OcrLine>,
        imageWidth: Int,
        imageHeight: Int
    ): NormalizedCrop? {
        if (imageWidth <= 0 || imageHeight <= 0) return null

        val readable = lines.filter { line ->
            line.text.count(Char::isLetter) >= MIN_LETTERS &&
                line.width > 0f && line.height > 0f
        }
        if (readable.size < MIN_LINES) return null

        val medianHeight = readable.map { it.height }.sorted()[readable.size / 2]
        val bodyLines = readable.filter { line ->
            line.height in (medianHeight * MIN_HEIGHT_RATIO)..(medianHeight * MAX_HEIGHT_RATIO)
        }.ifEmpty { readable }

        val left = (bodyLines.minOf { it.left } / imageWidth - HORIZONTAL_PADDING).coerceIn(0f, 1f)
        val right = (bodyLines.maxOf { it.right } / imageWidth + HORIZONTAL_PADDING).coerceIn(0f, 1f)
        val top = (bodyLines.minOf { it.top } / imageHeight - VERTICAL_PADDING).coerceIn(0f, 1f)
        val bottom = (bodyLines.maxOf { it.bottom } / imageHeight + VERTICAL_PADDING).coerceIn(0f, 1f)

        if (right - left < MIN_WIDTH || bottom - top < MIN_HEIGHT) return null
        if ((right - left) * (bottom - top) > MAX_AREA) return null
        return NormalizedCrop(left, top, right, bottom)
    }

    private const val MIN_LETTERS = 4
    private const val MIN_LINES = 3
    private const val MIN_HEIGHT_RATIO = 0.45f
    private const val MAX_HEIGHT_RATIO = 2.2f
    private const val HORIZONTAL_PADDING = 0.035f
    private const val VERTICAL_PADDING = 0.045f
    private const val MIN_WIDTH = 0.25f
    private const val MIN_HEIGHT = 0.08f
    private const val MAX_AREA = 0.88f
}
