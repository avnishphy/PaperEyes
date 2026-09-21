// Compile-only ML Kit shape; portable tests do not perform OCR.
package com.google.mlkit.vision.text
import android.graphics.Rect
class Text(val text: String, val textBlocks: List<TextBlock> = emptyList()) {
 class TextBlock(val text: String, val boundingBox: Rect?, val lines: List<Line> = emptyList())
 class Line(val text: String, val boundingBox: Rect?)
}
