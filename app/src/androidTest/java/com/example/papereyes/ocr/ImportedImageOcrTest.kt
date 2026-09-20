package com.example.papereyes.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ImportedImageOcrTest {

    @Test
    fun importedImageUriIsReadableByMlKit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 82f
        }

        canvas.drawText("Quantum Chromodynamics", 90f, 260f, paint)
        canvas.drawText("and Hadron Structure", 90f, 380f, paint)

        val file = File(context.cacheDir, "papereyes_import_ocr_test.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()

        val recognizer = TextRecognizerService()
        try {
            val result = recognizer.recognizeImage(
                context = context,
                uri = Uri.fromFile(file)
            )

            assertTrue(
                "Expected ML Kit to read the generated import image, got: ${result.rawText}",
                result.rawText.contains("Quantum", ignoreCase = true) &&
                        result.rawText.contains("Hadron", ignoreCase = true)
            )
        } finally {
            recognizer.close()
            file.delete()
        }
    }
    @Test
    fun explicitArxivMetadataIsPreferredOverNoisyTitle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(1800, 1200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 54f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 70f
        }

        canvas.drawText(
            "arXiv:2609.20448 v1 | nucl-th | 17 Sep 2026",
            90f,
            180f,
            metadataPaint
        )
        canvas.drawText(
            "A comprehensive theory framework for perturbative",
            90f,
            380f,
            titlePaint
        )
        canvas.drawText(
            "calculations of oc in superallowed beta decays",
            90f,
            480f,
            titlePaint
        )
        canvas.drawText("Chien-Yeah Seng", 90f, 610f, metadataPaint)

        val file = File(context.cacheDir, "papereyes_import_arxiv_test.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()

        val recognizer = TextRecognizerService()
        try {
            val result = recognizer.recognizeImage(
                context = context,
                uri = Uri.fromFile(file)
            )

            assertEquals(
                "arXiv:2609.20448v1",
                result.bestQuery
            )
        } finally {
            recognizer.close()
            file.delete()
        }
    }

    @Test
    fun layoutAwareOcrKeepsJournalHeaderAndTitleAsSeparateEvidence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(1800, 1400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 40f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 78f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 38f
        }

        canvas.drawText(
            "PHYSICAL REVIEW D 108, 036027 (2023)",
            100f,
            120f,
            headerPaint
        )
        canvas.drawText(
            "Shedding light on shadow generalized parton distributions",
            90f,
            300f,
            titlePaint
        )
        canvas.drawText(
            "We study generalized parton distributions using deeply virtual scattering.",
            90f,
            520f,
            bodyPaint
        )
        canvas.drawText(
            "The formalism is applied to available experimental data and phenomenology.",
            90f,
            590f,
            bodyPaint
        )
        canvas.drawText(
            "Additional discussion follows in the subsequent sections of the paper.",
            90f,
            660f,
            bodyPaint
        )

        val file = File(context.cacheDir, "papereyes_layout_ocr_test.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()

        val recognizer = TextRecognizerService()
        try {
            val result = recognizer.recognizeImage(
                context = context,
                uri = Uri.fromFile(file)
            )

            assertEquals(
                DocumentLayoutAnalyzer.EvidenceType.JOURNAL_CITATION,
                result.bestEvidenceType
            )
            assertEquals(
                "Physical Review D",
                result.evidence.journalCitations.first().citation.journal
            )
            assertTrue(
                result.evidence.bestTitle?.text
                    ?.contains("shadow generalized parton distributions", ignoreCase = true)
                    == true
            )
        } finally {
            recognizer.close()
            file.delete()
        }
    }

}
