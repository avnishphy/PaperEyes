package com.example.papereyes.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.scale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.papereyes.domain.evidence.DocumentEvidence
import com.example.papereyes.domain.telemetry.ScanStage
import com.example.papereyes.domain.telemetry.ScanTrace
import com.example.papereyes.util.concurrency.CompletionGate
import java.util.concurrent.Executor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min


data class OcrResult(
    val rawText: String,
    val bestQuery: String,
    val evidence: DocumentEvidence = DocumentEvidence(),
    val lines: List<com.example.papereyes.domain.evidence.OcrLine> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

class TextRecognizerService : AutoCloseable {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    private val completionGate = CompletionGate { recognizer.close() }
    private val completionExecutor = Executor { it.run() }

    suspend fun recognizeImage(context: Context, uri: Uri, trace: ScanTrace? = null): OcrResult =
        withContext(Dispatchers.IO) {
            // Decode and ownership transfer share one cancellation boundary.
            val bitmap = decodeForOcr(context, uri)
            var transferred = false
            try {
                currentCoroutineContext().ensureActive()
                val image = InputImage.fromBitmap(bitmap, 0)
                transferred = true
                recognize(image, trace) { bitmap.recycle() }
            } finally {
                if (!transferred) bitmap.recycle()
            }
        }

    /** Keep captured JPEG resolution; Import sampling and all EXIF transforms stay unchanged. */
    suspend fun recognizeCameraCapture(context: Context, uri: Uri, trace: ScanTrace? = null): OcrResult =
        withContext(Dispatchers.IO) { recognize(InputImage.fromFilePath(context, uri), trace) }

    suspend fun recognizeReferenceCrop(
        context: Context,
        uri: Uri,
        baseline: OcrResult,
        trace: ScanTrace? = null
    ): OcrResult = withContext(Dispatchers.IO) {
        val crop = ReferenceCropPlanner.plan(
            baseline.lines,
            baseline.imageWidth,
            baseline.imageHeight
        ) ?: return@withContext baseline

        val decoded = decodeForOcr(context, uri)
        var prepared: Bitmap? = null
        try {
            val left = floor(crop.left * decoded.width).toInt().coerceIn(0, decoded.width - 1)
            val top = floor(crop.top * decoded.height).toInt().coerceIn(0, decoded.height - 1)
            val right = ceil(crop.right * decoded.width).toInt().coerceIn(left + 1, decoded.width)
            val bottom = ceil(crop.bottom * decoded.height).toInt().coerceIn(top + 1, decoded.height)
            val cropped = Bitmap.createBitmap(decoded, left, top, right - left, bottom - top)
            if (cropped !== decoded) decoded.recycle()

            val scale = min(
                MAX_REFERENCE_CROP_UPSCALE,
                MAX_OCR_IMAGE_DIMENSION.toFloat() / maxOf(cropped.width, cropped.height)
            ).coerceAtLeast(1f)
            prepared = if (scale >= MIN_REFERENCE_CROP_UPSCALE) {
                cropped.scale(
                    (cropped.width * scale).toInt(),
                    (cropped.height * scale).toInt()
                ).also { scaled ->
                    if (scaled !== cropped) cropped.recycle()
                }
            } else {
                cropped
            }

            val image = InputImage.fromBitmap(prepared, 0)
            val owned = prepared
            prepared = null
            recognize(image, trace) { owned.recycle() }
        } finally {
            prepared?.recycle()
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    private suspend fun recognize(
        image: InputImage, trace: ScanTrace?, completed: () -> Unit = {}
    ): OcrResult {
        if (!completionGate.acquire()) {
            completed()
            error("Text recognition has closed")
        }
        trace?.mark(ScanStage.OCR_START)
        val task = try { recognizer.process(image) }
        catch (error: Exception) {
            try { completed() } finally { completionGate.release() }
            throw error
        }
        task.addOnCompleteListener(completionExecutor) {
            trace?.mark(ScanStage.OCR_END)
            try { completed() } finally { completionGate.release() }
        }
        // Cancellation stops waiting; it must NOT recycle an input still in use by ML Kit.
        val result = task.await()
        val lines = TextCandidateExtractor.extractLines(result)
        val evidence = DocumentLayoutAnalyzer.analyze(result.text, lines, image.width, image.height)
        trace?.mark(ScanStage.LAYOUT_END)
        return OcrResult(
            rawText = result.text.trim(),
            bestQuery = evidence.bestQuery,
            evidence = evidence,
            lines = lines,
            imageWidth = image.width,
            imageHeight = image.height
        )
    }

    override fun close() = completionGate.close()

    private fun decodeForOcr(
        context: Context,
        uri: Uri
    ): Bitmap {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val boundsStream =
            resolver.openInputStream(uri)
                ?: error("Unable to open image")

        boundsStream.use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                bounds
            )
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            error("Unable to read image dimensions")
        }

        val orientation = resolver.openInputStream(uri)?.use { stream ->
            runCatching {
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxDimension = MAX_OCR_IMAGE_DIMENSION
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Unable to decode image")

        return applyExifOrientation(decoded, orientation)
    }

    private fun applyExifOrientation(
        bitmap: Bitmap,
        orientation: Int
    ): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }

        return try {
            val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (transformed !== bitmap) bitmap.recycle()
            transformed
        } catch (error: Exception) {
            bitmap.recycle()
            throw error
        }
    }

    companion object {
        // Dense paper text benefits from more resolution than typical OCR, but
        // decoding phone-camera originals at 8K+ is unnecessary memory load.
        private const val MAX_OCR_IMAGE_DIMENSION = 2400
        private const val MAX_REFERENCE_CROP_UPSCALE = 2f
        private const val MIN_REFERENCE_CROP_UPSCALE = 1.08f
    }
}
