package com.example.papereyes.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.util.Size
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.compose.runtime.withFrameNanos
import com.example.papereyes.domain.ResolutionStatus
import com.example.papereyes.domain.telemetry.ScanStage
import com.example.papereyes.domain.telemetry.ScanTrace
import com.example.papereyes.util.concurrency.CompletionGate
import java.util.concurrent.Executor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraControl
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.papereyes.BuildConfig
import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.local.LibraryRepository
import com.example.papereyes.domain.PaperResolver
import com.example.papereyes.domain.isConnectivityFailure
import com.example.papereyes.domain.evidence.ReferenceEvidence
import com.example.papereyes.domain.evidence.ScanSubject
import com.example.papereyes.domain.evidence.hasRequestedEvidence
import com.example.papereyes.domain.evidence.mergeReferenceEvidence
import com.example.papereyes.domain.evidence.referencesFor
import com.example.papereyes.domain.reference.ReferenceBatchProgress
import com.example.papereyes.domain.reference.ReferenceBatchResolver
import com.example.papereyes.domain.reference.ReferenceResolution
import com.example.papereyes.domain.reference.ReferenceResolutionStatus
import com.example.papereyes.ocr.TextRecognizerService
import com.example.papereyes.ocr.OcrResult
import com.example.papereyes.ui.common.ReferenceSelectionDialog
import com.example.papereyes.ui.common.SaveIdentifiedPapersDialog
import com.example.papereyes.ui.common.toUserFacingMessage
import com.example.papereyes.util.network.NetworkAvailability
import com.example.papereyes.util.network.OFFLINE_MESSAGE
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


private const val LIVE_ANALYSIS_INTERVAL_MS =
    650L

private const val CAPTURE_COOLDOWN_MS =
    1700L

private const val LIVE_ANALYSIS_WIDTH =
    1280

private const val LIVE_ANALYSIS_HEIGHT =
    720

private const val FALLBACK_BURST_FRAME_COUNT =
    2

private const val SHARPNESS_MAX_DIMENSION =
    900

private const val PAPER_MATCH_THRESHOLD =
    0.65

private fun ScanSubject.scanGuidance(): String = when (this) {
    ScanSubject.JOURNAL_PAPER -> "Point PaperEyes toward the paper title"
    ScanSubject.REFERENCES -> "Point PaperEyes toward the reference list"
    ScanSubject.CONFERENCE_SLIDE -> "Point PaperEyes toward the slide citation"
}

private fun ScanSubject.noEvidenceMessage(): String = when (this) {
    ScanSubject.JOURNAL_PAPER -> "Couldn't read enough of the paper title"
    ScanSubject.REFERENCES -> "No references found — try a tighter frame"
    ScanSubject.CONFERENCE_SLIDE -> "No slide citation found — try the citation footer"
}

private fun ScanSubject.readingMessage(): String = when (this) {
    ScanSubject.JOURNAL_PAPER -> "Reading paper…"
    ScanSubject.REFERENCES -> "Reading references…"
    ScanSubject.CONFERENCE_SLIDE -> "Reading slide citations…"
}


/*
 * ================================================================
 * SUPER-BURST HIGH-RES CAPTURE
 * ================================================================
 *
 * Preview OCR is deliberately only a cheap trigger. Once enough text is
 * visible, capture several full-resolution JPEGs, score a small downsampled
 * copy of each for sharpness, and OCR only the sharpest original JPEG.
 *
 * Only the tiny sharpness copies are decoded here. The selected JPEG is kept
 * at camera resolution for ML Kit.
 */
private data class SharpFrame(
    val file: File,
    val score: Double
)

private fun captureBurst(
    cacheDir: File,
    imageCapture: ImageCapture,
    executor: Executor,
    frameCount: Int,
    onProgress: (captured: Int, total: Int) -> Unit,
    onCaptured: (List<File>) -> Unit,
    onError: (String) -> Unit,
    isCancelled: () -> Boolean = { false }
) {
    val files = mutableListOf<File>()
    var finished = false

    fun fail(message: String, currentFile: File? = null) {
        if (finished) return
        finished = true
        currentFile?.delete()
        files.forEach(File::delete)
        onError(message)
    }

    fun captureNext() {
        if (finished) return
        if (isCancelled()) { fail("Capture cancelled"); return }

        val outputFile =
            try {
                File.createTempFile(
                    "papereyes_live_",
                    ".jpg",
                    cacheDir
                )
            } catch (_: Exception) {
                fail("Could not create temporary image.")
                return
            }

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(outputFile)
                .build()

        try {
        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    if (finished || isCancelled()) {
                        outputFile.delete()
                        fail("Capture cancelled")
                        return
                    }

                    files += outputFile
                    onProgress(files.size, frameCount)

                    if (files.size >= frameCount) {
                        finished = true
                        onCaptured(files.toList())
                    } else {
                        captureNext()
                    }
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    fail(
                        message = "Image capture failed.",
                        currentFile = outputFile
                    )
                }
            }
        )
        } catch (_: Exception) {
            fail("Image capture failed.", outputFile)
        }
    }

    captureNext()
}


private class LiveCaptureException(
    message: String
) : Exception(message)

private suspend fun captureBurstAwait(
    cacheDir: File,
    imageCapture: ImageCapture,
    executor: Executor,
    frameCount: Int,
    onProgress: (captured: Int, total: Int) -> Unit
): List<File> {
    val delivered = AtomicReference<List<File>?>(null)
    val cancelled = AtomicBoolean(false)
    return try {
        val files = suspendCancellableCoroutine<List<File>> { continuation ->
            continuation.invokeOnCancellation {
                cancelled.set(true)
                delivered.getAndSet(null)?.forEach(File::delete)
            }
            captureBurst(cacheDir, imageCapture, executor, frameCount, onProgress,
                onCaptured = { captured ->
                    delivered.set(captured)
                    if (continuation.isActive) continuation.resume(captured)
                    else delivered.getAndSet(null)?.forEach(File::delete)
                },
                onError = { message ->
                    if (continuation.isActive) continuation.resumeWithException(LiveCaptureException(message))
                },
                isCancelled = cancelled::get)
        }
        delivered.set(null)
        files
    } catch (error: CancellationException) {
        cancelled.set(true)
        delivered.getAndSet(null)?.forEach(File::delete)
        throw error
    }
}

private fun selectSharpestFrame(
    files: List<File>
): SharpFrame? =
    files
        .asSequence()
        .mapNotNull { file ->
            runCatching {
                SharpFrame(
                    file = file,
                    score = scoreSharpness(file)
                )
            }.getOrNull()
        }
        .maxByOrNull { it.score }

private fun scoreSharpness(
    file: File
): Double {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeFile(
        file.absolutePath,
        bounds
    )

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return Double.NEGATIVE_INFINITY
    }

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > SHARPNESS_MAX_DIMENSION ||
        bounds.outHeight / sampleSize > SHARPNESS_MAX_DIMENSION
    ) {
        sampleSize *= 2
    }

    val bitmap =
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
        ) ?: return Double.NEGATIVE_INFINITY

    try {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 3 || height < 3) {
            return Double.NEGATIVE_INFINITY
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        fun luminance(color: Int): Int {
            val red = color shr 16 and 0xFF
            val green = color shr 8 and 0xFF
            val blue = color and 0xFF
            return (77 * red + 150 * green + 29 * blue) shr 8
        }

        var sum = 0.0
        var sumSquares = 0.0
        var count = 0

        // A Laplacian-variance style score is inexpensive and works well for
        // choosing the sharpest frame of the same document scene.
        for (y in 1 until height - 1 step 2) {
            val row = y * width

            for (x in 1 until width - 1 step 2) {
                val index = row + x

                val center = luminance(pixels[index])
                val left = luminance(pixels[index - 1])
                val right = luminance(pixels[index + 1])
                val up = luminance(pixels[index - width])
                val down = luminance(pixels[index + width])

                val laplacian =
                    4 * center -
                            left -
                            right -
                            up -
                            down

                val value = laplacian.toDouble()
                sum += value
                sumSquares += value * value
                count += 1
            }
        }

        if (count == 0) {
            return Double.NEGATIVE_INFINITY
        }

        val mean = sum / count
        return sumSquares / count - mean * mean
    } finally {
        bitmap.recycle()
    }
}


/*
 * ================================================================
 * CHEAP LIVE OCR
 * ================================================================
 *
 * This pass does not identify the paper.
 *
 * It answers only:
 *
 *      "Is there enough text visible to justify a burst?"
 */
@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
private fun analyzeForTextPresence(
    imageProxy: ImageProxy,
    recognizer: TextRecognizer,
    completionGate: CompletionGate,
    onText: (String, ScanTrace) -> Unit
) {
    val trace = ScanTrace().also { it.mark(ScanStage.FRAME_TIME) }
    if (!completionGate.acquire()) { imageProxy.close(); return }
    val image = try {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close(); completionGate.release(); return
        }
        InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    } catch (_: Exception) {
        imageProxy.close(); completionGate.release(); return
    }
    trace.mark(ScanStage.PREVIEW_OCR_START)
    val task = try { recognizer.process(image) }
    catch (_: Exception) { imageProxy.close(); completionGate.release(); return }
    // Frame release is independent of a screen callback or executor shutdown.
    task.addOnCompleteListener(Executor { it.run() }) {
        trace.mark(ScanStage.PREVIEW_OCR_END)
        try { imageProxy.close() } finally { completionGate.release() }
    }
    task.addOnSuccessListener { result -> onText(result.text, trace) }
}


@Composable
fun LiveScanScreen(
    libraryRepository: LibraryRepository,
    scanSubject: ScanSubject,
    detailOpen: Boolean = false,
    onBack: () -> Unit,
    onPaperClick: (Paper) -> Unit
) {

    val context =
        LocalContext.current


    val lifecycleOwner =
        LocalLifecycleOwner.current


    val coroutineScope =
        rememberCoroutineScope()


    /*
     * ================================================================
     * NAVIGATION
     * ================================================================
     */
    BackHandler(enabled = !detailOpen) {

        onBack()
    }


    /*
     * ================================================================
     * STATE
     * ================================================================
     */

    var hasCameraPermission by remember {

        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }


    var statusMessage by remember {

        mutableStateOf(
            scanSubject.scanGuidance()
        )
    }


    var highResCandidate by remember {

        mutableStateOf("")
    }


    var matchScore by remember {

        mutableStateOf<Double?>(null)
    }


    var captureInProgress by remember {

        mutableStateOf(false)
    }


    var resolving by remember {

        mutableStateOf(false)
    }


    var scanningLocked by remember {

        mutableStateOf(false)
    }

    var cameraSessionActive by remember(lifecycleOwner) {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        )
    }


    var papers by remember {

        mutableStateOf<List<Paper>>(
            emptyList()
        )
    }


    var errorMessage by remember {

        mutableStateOf<String?>(
            null
        )
    }

    var pendingReferences by remember {
        mutableStateOf<List<ReferenceEvidence>>(emptyList())
    }

    var referenceOutcomes by remember {
        mutableStateOf<List<ReferenceResolution>>(emptyList())
    }

    var referenceProgress by remember {
        mutableStateOf<ReferenceBatchProgress?>(null)
    }

    var savePromptPapers by remember {
        mutableStateOf<List<Paper>>(emptyList())
    }

    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minimumZoomRatio by remember { mutableFloatStateOf(1f) }
    var maximumZoomRatio by remember { mutableFloatStateOf(1f) }


    /*
     * Debug information is useful during scanner development,
     * but should not dominate the normal UI.
     */
    var showDebug by remember {

        mutableStateOf(false)
    }


    /*
     * ================================================================
     * SERVICES
     * ================================================================
     */

    val resolver =
        remember {

            PaperResolver()
        }


    val highResRecognizer =
        remember {

            TextRecognizerService()
        }

    val referenceResolver =
        remember(resolver) {
            ReferenceBatchResolver(resolver)
        }


    val liveRecognizer =
        remember {

            TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )
        }


    val liveCompletionGate = remember(liveRecognizer) { CompletionGate { liveRecognizer.close() } }
    var displayTrace by remember { mutableStateOf<ScanTrace?>(null) }

    LaunchedEffect(displayTrace) {
        val trace = displayTrace ?: return@LaunchedEffect
        withFrameNanos { }
        trace.mark(ScanStage.RESULT_DISPLAY)
        if (BuildConfig.DEBUG) Log.i("PaperEyesTiming", trace.toNumericJson())
        displayTrace = null
    }

    val cameraExecutor =
        remember {

            Executors.newSingleThreadExecutor()
        }


    val disposedRef = remember {
        AtomicBoolean(false)
    }

    val foregroundRef = remember {
        AtomicBoolean(cameraSessionActive)
    }

    val cameraGenerationRef = remember {
        AtomicLong(0L)
    }

    val activeScanJobRef = remember {
        AtomicReference<Job?>(null)
    }

    val cameraProviderRef = remember {
        AtomicReference<ProcessCameraProvider?>(null)
    }

    val imageAnalysisRef = remember {
        AtomicReference<ImageAnalysis?>(null)
    }

    val cameraControlRef = remember {
        AtomicReference<CameraControl?>(null)
    }

    fun stopCameraSession() {
        cameraGenerationRef.incrementAndGet()
        imageAnalysisRef.getAndSet(null)?.clearAnalyzer()
        cameraControlRef.set(null)
        cameraProviderRef.getAndSet(null)?.let { provider ->
            runCatching { provider.unbindAll() }
        }
    }

    fun launchTrackedScan(block: suspend CoroutineScope.() -> Unit) {
        val job = coroutineScope.launch(start = CoroutineStart.LAZY, block = block)
        activeScanJobRef.getAndSet(job)?.cancel()
        job.invokeOnCompletion {
            activeScanJobRef.compareAndSet(job, null)
        }
        job.start()
    }

    fun applyZoom(requested: Float) {
        val clamped = clampZoomRatio(requested, minimumZoomRatio, maximumZoomRatio)
        zoomRatio = clamped
        cameraControlRef.get()?.let { control ->
            runCatching { control.setZoomRatio(clamped) }
        }
    }

    LaunchedEffect(scanningLocked) {
        if (scanningLocked) {
            stopCameraSession()
        }
    }

    LaunchedEffect(cameraSessionActive) {
        if (cameraSessionActive && !scanningLocked) {
            captureInProgress = false
            resolving = false
            statusMessage = scanSubject.scanGuidance()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                foregroundRef.set(true)
                cameraSessionActive = true
            }

            override fun onStop(owner: LifecycleOwner) {
                foregroundRef.set(false)
                cameraSessionActive = false
                activeScanJobRef.getAndSet(null)?.cancel()
                stopCameraSession()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    suspend fun resolveReferences(selected: List<ReferenceEvidence>) {
        if (!NetworkAvailability.hasValidatedInternet(context)) {
            errorMessage = OFFLINE_MESSAGE
            statusMessage = "No internet connection — tap Try again"
            scanningLocked = true
            return
        }

        resolving = true
        errorMessage = null
        papers = emptyList()
        referenceOutcomes = emptyList()
        referenceProgress = null
        statusMessage = "Searching ${selected.size} ${if (selected.size == 1) "reference" else "references"}…"

        try {
            val outcomes = referenceResolver.resolveAll(selected) { progress ->
                referenceProgress = progress
                referenceOutcomes = referenceOutcomes + progress.latest
                papers = referenceOutcomes.mapNotNull { it.paper }.distinctBy { it.identityKey }
                statusMessage = "Searching references ${progress.completed}/${progress.total}…"
            }
            referenceOutcomes = outcomes
            papers = outcomes.mapNotNull { it.paper }.distinctBy { it.identityKey }
            if (papers.size > 1) savePromptPapers = papers
            scanningLocked = true
            val failures = outcomes.count { it.status != ReferenceResolutionStatus.IDENTIFIED }
            val servicesUnavailable = outcomes.any {
                it.status == ReferenceResolutionStatus.PROVIDERS_UNAVAILABLE
            }
            if (servicesUnavailable) {
                errorMessage = "Internet connection or scholarly services became unavailable. Reconnect, then try again."
            }
            statusMessage = when {
                servicesUnavailable -> "Connection interrupted — tap Try again"
                failures == 0 -> "All selected references identified"
                papers.isNotEmpty() -> "Reference search complete — $failures not identified"
                else -> "Selected references could not be identified"
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            resolving = false
        }
    }


    /*
     * ================================================================
     * CLEANUP
     * ================================================================
     */
    DisposableEffect(Unit) {

        onDispose {
            disposedRef.set(true)
            foregroundRef.set(false)
            activeScanJobRef.getAndSet(null)?.cancel()

            // CameraX is bound to the Activity lifecycle, which can outlive this
            // composable. Explicitly detach the analyzer/use cases first.
            stopCameraSession()

            liveCompletionGate.close()
            highResRecognizer.close()
            cameraExecutor.shutdown()
        }
    }


    /*
     * ================================================================
     * CAMERA PERMISSION
     * ================================================================
     */

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCameraPermission =
                granted
        }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            context.cacheDir
                .listFiles { file -> file.name.startsWith("papereyes_live_") }
                ?.forEach(File::delete)
        }

        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    /*
     * ================================================================
     * PERMISSION SCREEN
     * ================================================================
     */
    if (!hasCameraPermission) {

        LiveScanPermissionScreen(
            onBack =
                onBack,

            onAllowCamera = {

                permissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        )

        return
    }


    /*
     * ================================================================
     * LIVE SCANNER
     * ================================================================
     */
    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        /*
         * ------------------------------------------------------------
         * CAMERA PREVIEW
         * ------------------------------------------------------------
         */
        if (!scanningLocked && cameraSessionActive) {
        AndroidView(
            modifier =
                Modifier.fillMaxSize(),

            factory = { previewContext ->

                val cameraGeneration = cameraGenerationRef.incrementAndGet()

                val previewView =
                    PreviewView(
                        previewContext
                    )


                previewView.implementationMode =
                    PreviewView
                        .ImplementationMode
                        .COMPATIBLE


                previewView.scaleType =
                    PreviewView
                        .ScaleType
                        .FILL_CENTER

                val scaleGestureDetector = ScaleGestureDetector(
                    previewContext,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            applyZoom(
                                scaleZoomRatio(
                                    zoomRatio,
                                    detector.scaleFactor,
                                    minimumZoomRatio,
                                    maximumZoomRatio
                                )
                            )
                            return true
                        }
                    }
                )

                previewView.isClickable = true
                previewView.setOnTouchListener { view, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                    true
                }


                val cameraProviderFuture =
                    ProcessCameraProvider
                        .getInstance(
                            previewContext
                        )


                /*
                 * These values belong to this CameraX session.
                 */
                var lastAnalysisTime =
                    0L


                var lastCaptureTime =
                    0L


                cameraProviderFuture.addListener(
                    {

                        try {

                            val cameraProvider =
                                cameraProviderFuture
                                    .get()

                            if (
                                disposedRef.get() ||
                                !foregroundRef.get() ||
                                cameraGeneration != cameraGenerationRef.get()
                            ) {
                                return@addListener
                            }

                            cameraProviderRef.set(cameraProvider)


                            /*
                             * ========================================
                             * PREVIEW
                             * ========================================
                             */
                            val preview =
                                Preview
                                    .Builder()
                                    .build()


                            preview.setSurfaceProvider(
                                previewView
                                    .surfaceProvider
                            )


                            /*
                             * ========================================
                             * HIGH-RES IMAGE CAPTURE
                             * ========================================
                             */
                            val imageCapture =
                                ImageCapture
                                    .Builder()
                                    .setCaptureMode(
                                        ImageCapture
                                            .CAPTURE_MODE_MINIMIZE_LATENCY
                                    )
                                    .build()


                            /*
                             * ========================================
                             * LOW-COST LIVE ANALYSIS
                             * ========================================
                             */
                            val imageAnalysis =
                                ImageAnalysis
                                    .Builder()
                                    .setTargetResolution(
                                        Size(
                                            LIVE_ANALYSIS_WIDTH,
                                            LIVE_ANALYSIS_HEIGHT
                                        )
                                    )
                                    .setBackpressureStrategy(
                                        ImageAnalysis
                                            .STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .build()

                            imageAnalysisRef.set(imageAnalysis)

                            imageAnalysis.setAnalyzer(
                                cameraExecutor
                            ) { imageProxy ->

                                if (
                                    disposedRef.get() ||
                                    !foregroundRef.get() ||
                                    cameraGeneration != cameraGenerationRef.get()
                                ) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val now =
                                    SystemClock
                                        .elapsedRealtime()


                                /*
                                 * Do not OCR every preview frame.
                                 */
                                if (
                                    now -
                                    lastAnalysisTime <
                                    LIVE_ANALYSIS_INTERVAL_MS
                                ) {

                                    imageProxy.close()

                                    return@setAnalyzer
                                }


                                lastAnalysisTime =
                                    now


                                analyzeForTextPresence(
                                    imageProxy =
                                        imageProxy,

                                    recognizer =
                                        liveRecognizer,

                                    completionGate = liveCompletionGate,
                                    onText = { text, trace ->

                                        coroutineScope.launch {

                                            if (!foregroundRef.get()) {
                                                return@launch
                                            }

                                            /*
                                             * Once a paper has been found,
                                             * stop initiating new bursts.
                                             */
                                            if (scanningLocked) {

                                                return@launch
                                            }


                                            val letterCount =
                                                text.count { character ->
                                                    character.isLetter()
                                                }

                                            // Preview OCR is intentionally permissive.
                                            // It only decides whether a high-resolution
                                            // burst is worth taking.
                                            if (letterCount < 20 && !looksLikeIdentifier(text)) {
                                                statusMessage =
                                                    scanSubject.scanGuidance()
                                                return@launch
                                            }

                                            val captureTime =
                                                SystemClock
                                                    .elapsedRealtime()


                                            /*
                                             * Prevent overlapping capture
                                             * and lookup operations.
                                             */
                                            if (
                                                captureInProgress ||
                                                resolving ||
                                                captureTime -
                                                lastCaptureTime <
                                                CAPTURE_COOLDOWN_MS
                                            ) {

                                                return@launch
                                            }


                                            lastCaptureTime =
                                                captureTime


                                            /*
                                             * =================================
                                             * START CAPTURE
                                             * =================================
                                             */
                                            captureInProgress =
                                                true


                                            errorMessage =
                                                null


                                            highResCandidate =
                                                ""


                                            matchScore =
                                                null


                                            statusMessage =
                                                "Text detected — capturing…"


                                            val scanJob = coroutineScope.launch {
                                                val capturedFiles =
                                                    mutableListOf<File>()

                                                try {
                                                    /*
                                                     * Fast path: one full-resolution frame.
                                                     * Most clean scans should never pay for the
                                                     * two extra CameraX captures.
                                                     */
                                                    trace.mark(ScanStage.CAPTURE_START)
                                                    val firstFrame =
                                                        captureBurstAwait(
                                                            cacheDir = context.cacheDir,
                                                            imageCapture = imageCapture,
                                                            executor = ContextCompat.getMainExecutor(context),
                                                            frameCount = 1,
                                                            onProgress = { _, _ ->
                                                                statusMessage =
                                                                    "Captured — ${scanSubject.readingMessage().lowercase()}"
                                                            }
                                                        ).single()

                                                    trace.mark(ScanStage.CAPTURE_END)
                                                    capturedFiles += firstFrame

                                                    statusMessage =
                                                        scanSubject.readingMessage()

                                                    var ocrResult = highResRecognizer.recognizeCameraCapture(
                                                        context, Uri.fromFile(firstFrame), trace)
                                                    var candidate = ocrResult.bestQuery.trim()

                                                    /*
                                                     * Adaptive Super Burst fallback. Reference scans
                                                     * always confirm against a second OCR observation;
                                                     * paper scans only pay for it when the first native-
                                                     * resolution result is weak. The two reference lists
                                                     * are merged conservatively to stabilize their count.
                                                     */
                                                    if (
                                                        scanSubject.isReferenceFocused ||
                                                        !ocrResult.evidence.hasRequestedEvidence(scanSubject)
                                                    ) {
                                                        statusMessage =
                                                            if (scanSubject.isReferenceFocused) {
                                                                "Confirming reference list — capturing 2 more…"
                                                            } else {
                                                                "First frame unclear — capturing 2 more…"
                                                            }

                                                        trace.mark(ScanStage.CAPTURE_START)
                                                        val fallbackFrames =
                                                            captureBurstAwait(
                                                                cacheDir = context.cacheDir,
                                                                imageCapture = imageCapture,
                                                                executor = ContextCompat.getMainExecutor(context),
                                                                frameCount = FALLBACK_BURST_FRAME_COUNT,
                                                                onProgress = { captured, total ->
                                                                    statusMessage =
                                                                        "Improving capture $captured/$total…"
                                                                }
                                                            )

                                                        trace.mark(ScanStage.CAPTURE_END)
                                                        capturedFiles += fallbackFrames

                                                        statusMessage =
                                                            "Selecting sharpest frame…"

                                                        val sharpestFallback =
                                                            withContext(Dispatchers.Default) {
                                                                selectSharpestFrame(fallbackFrames)
                                                            }

                                                        if (sharpestFallback == null) {
                                                            errorMessage =
                                                                "Could not evaluate captured images."
                                                            statusMessage =
                                                                "Capture failed"
                                                            return@launch
                                                        }

                                                        statusMessage =
                                                            scanSubject.readingMessage()

                                                        val fallbackResult =
                                                            if (scanSubject.isReferenceFocused) {
                                                                highResRecognizer.recognizeReferenceCrop(
                                                                    context,
                                                                    Uri.fromFile(sharpestFallback.file),
                                                                    ocrResult,
                                                                    trace
                                                                )
                                                            } else {
                                                                highResRecognizer.recognizeCameraCapture(
                                                                    context,
                                                                    Uri.fromFile(sharpestFallback.file),
                                                                    trace
                                                                )
                                                            }

                                                        ocrResult =
                                                            if (scanSubject.isReferenceFocused) {
                                                                val primaryResult = ocrResult
                                                                val preferredResult =
                                                                    listOf(primaryResult, fallbackResult)
                                                                        .maxWithOrNull(
                                                                            compareBy<OcrResult> {
                                                                                it.evidence.references.size
                                                                            }.thenBy {
                                                                                it.rawText.length
                                                                            }
                                                                        ) ?: primaryResult
                                                                val combinedReferences =
                                                                    mergeReferenceEvidence(
                                                                        primaryResult.evidence.references,
                                                                        fallbackResult.evidence.references
                                                                    )

                                                                preferredResult.copy(
                                                                    evidence = preferredResult.evidence.copy(
                                                                        references = combinedReferences
                                                                    )
                                                                )
                                                            } else {
                                                                fallbackResult
                                                            }
                                                        candidate = ocrResult.bestQuery.trim()
                                                    }

                                                    highResCandidate =
                                                        candidate

                                                    val references =
                                                        ocrResult.evidence.referencesFor(scanSubject)
                                                    if (references.size > 1) {
                                                        pendingReferences = references
                                                        scanningLocked = true
                                                        statusMessage = "${references.size} references found — choose which to search"
                                                        return@launch
                                                    }
                                                    if (references.size == 1) {
                                                        scanningLocked = true
                                                        resolveReferences(references)
                                                        return@launch
                                                    }

                                                    if (!ocrResult.evidence.hasRequestedEvidence(scanSubject)) {
                                                        statusMessage =
                                                            scanSubject.noEvidenceMessage()
                                                        return@launch
                                                    }

                                                    resolving =
                                                        true

                                                    if (!NetworkAvailability.hasValidatedInternet(context)) {
                                                        errorMessage = OFFLINE_MESSAGE
                                                        statusMessage = "No internet connection — tap Try again"
                                                        scanningLocked = true
                                                        return@launch
                                                    }

                                                    statusMessage =
                                                        "Checking scholarly sources…"

                                                    val result =
                                                        try {
                                                            trace.mark(ScanStage.LOOKUP_START)
                                                            try { resolver.resolveEvidence(ocrResult.evidence) }
                                                            finally { trace.mark(ScanStage.LOOKUP_END) }
                                                        } catch (
                                                            exception: CancellationException
                                                        ) {
                                                            throw exception
                                                        } catch (
                                                            exception: Exception
                                                        ) {
                                                            errorMessage =
                                                                exception.toUserFacingMessage(
                                                                    "Paper lookup failed. Check your connection and try again."
                                                                )
                                                            statusMessage =
                                                                if (exception.isConnectivityFailure()) {
                                                                    "Connection interrupted — tap Try again"
                                                                } else {
                                                                    "Lookup unavailable — tap Try again"
                                                                }
                                                            scanningLocked = true
                                                            null
                                                        }

                                                    if (result == null) {
                                                        return@launch
                                                    }

                                                    if (result.papers.isEmpty()) {
                                                        papers = emptyList()
                                                        statusMessage =
                                                            "No convincing match — trying another scan"
                                                        return@launch
                                                    }

                                                    if (result.status == ResolutionStatus.VERIFIED) {
                                                        papers =
                                                            listOf(result.papers.first())
                                                        matchScore = null
                                                        scanningLocked = true
                                                        displayTrace = trace
                                                        statusMessage =
                                                            "Paper identified"
                                                        return@launch
                                                    }

                                                    if (result.inputType == com.example.papereyes.domain.PaperInputType.INTERIOR_TEXT) {
                                                        papers = emptyList()
                                                        statusMessage = "Interior text is uncertain — try the title or import the page"
                                                        return@launch
                                                    }

                                                    val bestMatch =
                                                        findConfidentPaperMatch(
                                                            query = candidate,
                                                            papers = result.papers
                                                        )

                                                    matchScore =
                                                        bestMatch?.score

                                                    if (
                                                        bestMatch != null &&
                                                        bestMatch.score >=
                                                        PAPER_MATCH_THRESHOLD
                                                    ) {
                                                        papers =
                                                            listOf(bestMatch.paper)
                                                        scanningLocked = true
                                                        displayTrace = trace
                                                        statusMessage =
                                                            "Paper identified"
                                                    } else {
                                                        papers = emptyList()
                                                        statusMessage =
                                                            "Title unclear — trying another scan"
                                                    }
                                                } catch (
                                                    exception: CancellationException
                                                ) {
                                                    throw exception
                                                } catch (
                                                    exception: LiveCaptureException
                                                ) {
                                                    errorMessage =
                                                        exception.message ?:
                                                                "Image capture failed."
                                                    statusMessage =
                                                        "Capture failed"
                                                } catch (
                                                    exception: Exception
                                                ) {
                                                    errorMessage =
                                                        exception.toUserFacingMessage(
                                                            "OCR failed. Try another scan."
                                                        )
                                                    statusMessage =
                                                        "OCR failed — trying again"
                                                } finally {
                                                    capturedFiles.forEach(File::delete)
                                                    captureInProgress = false
                                                    resolving = false
                                                }
                                            }
                                            activeScanJobRef.getAndSet(scanJob)?.cancel()
                                            scanJob.invokeOnCompletion {
                                                activeScanJobRef.compareAndSet(scanJob, null)
                                            }
                                        }
                                    }
                                )
                            }


                            /*
                             * ========================================
                             * BIND CAMERA
                             * ========================================
                             */
                            cameraProvider
                                .unbindAll()


                            val camera = cameraProvider
                                .bindToLifecycle(
                                    lifecycleOwner,

                                    CameraSelector
                                        .DEFAULT_BACK_CAMERA,

                                    preview,

                                    imageAnalysis,

                                    imageCapture
                                )

                            cameraControlRef.set(camera.cameraControl)
                            camera.cameraInfo.zoomState.value?.let { state ->
                                minimumZoomRatio = state.minZoomRatio
                                maximumZoomRatio = state.maxZoomRatio
                                applyZoom(zoomRatio)
                            }

                        } catch (
                            exception:
                            Exception
                        ) {

                            errorMessage =
                                "Could not start camera."
                        }
                    },

                    ContextCompat
                        .getMainExecutor(
                            previewContext
                        )
                )


                previewView
            }
        )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }


        /*
         * ============================================================
         * PRESENTATION LAYER
         * ============================================================
         *
         * Everything visible on top of the preview now lives in
         * LiveScanUi.kt.
         */
        LiveScanOverlay(
            scanSubject = scanSubject,
            statusMessage =
                statusMessage,

            highResCandidate =
                highResCandidate,

            matchScore =
                matchScore,

            captureInProgress =
                captureInProgress,

            resolving =
                resolving,

            scanningLocked =
                scanningLocked,

            errorMessage =
                errorMessage,

            paper =
                papers.firstOrNull(),

            referenceOutcomes =
                referenceOutcomes,

            referenceCompleted =
                referenceProgress?.completed ?: 0,

            referenceTotal =
                referenceProgress?.total ?: 0,

            showDebug =
                BuildConfig.DEBUG && showDebug,

            debugAvailable =
                BuildConfig.DEBUG,

            onBack =
                onBack,

            onToggleDebug = {
                if (BuildConfig.DEBUG) {
                    showDebug = !showDebug
                }
            },

            onOpenPaper = { paper ->

                onPaperClick(
                    paper
                )
            },

            onScanAgain = {

                papers =
                    emptyList()


                highResCandidate =
                    ""


                matchScore =
                    null


                errorMessage =
                    null


                captureInProgress =
                    false


                resolving =
                    false


                scanningLocked =
                    false


                pendingReferences =
                    emptyList()


                referenceOutcomes =
                    emptyList()


                referenceProgress =
                    null


                savePromptPapers =
                    emptyList()


                statusMessage =
                    scanSubject.scanGuidance()
            }
        )
    }

    if (pendingReferences.isNotEmpty()) {
        ReferenceSelectionDialog(
            references = pendingReferences,
            onDismiss = {
                pendingReferences = emptyList()
                scanningLocked = false
                statusMessage = scanSubject.scanGuidance()
            },
            onSearch = { selected ->
                pendingReferences = emptyList()
                launchTrackedScan { resolveReferences(selected) }
            }
        )
    }

    if (savePromptPapers.isNotEmpty()) {
        val papersToSave = savePromptPapers
        SaveIdentifiedPapersDialog(
            papers = papersToSave,
            onDismiss = { savePromptPapers = emptyList() },
            onSaveAll = {
                savePromptPapers = emptyList()
                coroutineScope.launch {
                    var saved = 0
                    papersToSave.forEach { paper ->
                        try {
                            if (libraryRepository.savePaper(paper)) saved += 1
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Throwable) {
                            // Keep saving the remaining identified papers.
                        }
                    }
                    Toast.makeText(
                        context,
                        if (saved > 0) "Saved $saved of ${papersToSave.size} papers"
                        else "Papers were already saved or could not be saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
