package com.example.papereyes.ocr

import com.example.papereyes.domain.evidence.DocumentEvidence
import com.example.papereyes.domain.evidence.OcrLine
import com.google.mlkit.vision.text.Text

/** ML Kit adapter; both Import and Live Scan use the same evidence rules. */
object TextCandidateExtractor {
    private val whitespace = Regex("\\s+")
    private val comparisonCleanup = Regex("[^\\p{L}\\p{N} ]")

    fun extractBestQuery(rawText: String): String = DocumentLayoutAnalyzer.fromText(rawText).bestQuery

    fun extractBestQuery(result: Text, imageWidth: Int, imageHeight: Int): String =
        analyze(result, imageWidth, imageHeight).bestQuery

    fun analyze(result: Text, imageWidth: Int, imageHeight: Int): DocumentEvidence {
        val lines = result.textBlocks.flatMapIndexed { blockId, block ->
            block.lines.mapNotNull { line ->
                line.boundingBox?.let { box ->
                    OcrLine(line.text, box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat(), blockId)
                }
            }
        }
        return DocumentLayoutAnalyzer.analyze(result.text, lines, imageWidth, imageHeight)
    }

    fun areSimilar(first: String, second: String): Boolean {
        fun normalize(value: String) = value.lowercase().replace(comparisonCleanup, " ").replace(whitespace, " ").trim()
        val left = normalize(first)
        val right = normalize(second)
        if (left.isBlank() || right.isBlank()) return false
        if (left == right) return true
        val a = left.split(' ').filter { it.length >= 3 }.toSet()
        val b = right.split(' ').filter { it.length >= 3 }.toSet()
        if (a.isEmpty() || b.isEmpty()) return false
        return a.intersect(b).size.toDouble() / a.union(b).size >= 0.64
    }
}
