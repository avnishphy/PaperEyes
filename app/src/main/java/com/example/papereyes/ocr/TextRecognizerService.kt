package com.example.papereyes.ocr

import android.content.Context
import android.media.Image
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


data class OcrResult(
    val rawText: String,
    val bestQuery: String,
    val evidence: DocumentLayoutAnalyzer.DocumentEvidence
) {
    val bestEvidenceType: DocumentLayoutAnalyzer.EvidenceType?
        get() = evidence.preferred?.type
}

/**
 * Owns one ML Kit recognizer and exposes OCR entry points for both imported
 * images and CameraX analysis frames. The recognizer is deliberately reused;
 * creating one per frame is expensive and can leak native resources.
 */
class TextRecognizerService : AutoCloseable {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    /**
     * Gallery/import path.
     *
     * ML Kit's supported URI loader handles content:// URIs from Photo Picker
     * and applies the image metadata expected by InputImage.
     */
    suspend fun recognizeImage(
        context: Context,
        uri: Uri
    ): OcrResult {
        val image = withContext(Dispatchers.IO) {
            InputImage.fromFilePath(context, uri)
        }

        return recognize(image)
    }

    /**
     * CameraX real-time path. The caller must keep the underlying ImageProxy
     * open until this suspend function returns, then close the ImageProxy.
     */
    suspend fun recognizeMediaImage(
        mediaImage: Image,
        rotationDegrees: Int
    ): OcrResult {
        return recognize(
            InputImage.fromMediaImage(
                mediaImage,
                rotationDegrees
            )
        )
    }

    /** Single high-resolution still fallback for difficult scans. */
    suspend fun recognizeCameraCapture(
        context: Context,
        uri: Uri
    ): OcrResult = recognizeImage(context, uri)

    private suspend fun recognize(
        image: InputImage
    ): OcrResult {
        val result = recognizer
            .process(image)
            .await()

        val evidence = DocumentLayoutAnalyzer.analyze(
            result = result,
            imageWidth = image.width,
            imageHeight = image.height
        )

        return OcrResult(
            rawText = result.text.trim(),
            bestQuery = evidence.preferred?.query.orEmpty(),
            evidence = evidence
        )
    }

    override fun close() {
        recognizer.close()
    }
}
