package com.example.papereyes.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.papereyes.BuildConfig
import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.PaperResolver
import com.example.papereyes.ocr.DocumentLayoutAnalyzer
import com.example.papereyes.ocr.TextRecognizerService
import com.example.papereyes.ui.common.toUserFacingMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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

private const val LIVE_ANALYSIS_WIDTH = 1280
private const val LIVE_ANALYSIS_HEIGHT = 720
private const val FALLBACK_CAPTURE_WIDTH = 1920
private const val FALLBACK_CAPTURE_HEIGHT = 1080
private const val STABLE_TITLE_OBSERVATIONS = 2
private const val SAME_QUERY_COOLDOWN_MS = 1_500L
private const val HIGH_RES_FALLBACK_AFTER_MS = 2_000L
private const val PAPER_MATCH_THRESHOLD = 0.60
private const val TAG = "PaperEyesLiveScan"

private class LiveCaptureException(message: String) : Exception(message)

private fun captureSingleFrame(
    cacheDir: File,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onCaptured: (File) -> Unit,
    onError: (String) -> Unit
) {
    val outputFile =
        try {
            File.createTempFile("papereyes_live_", ".jpg", cacheDir)
        } catch (_: Exception) {
            onError("Could not create temporary image.")
            return
        }

    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(outputFile)
        .build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(
                outputFileResults: ImageCapture.OutputFileResults
            ) {
                onCaptured(outputFile)
            }

            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
                onError("Image capture failed.")
            }
        }
    )
}

private suspend fun captureSingleFrameAwait(
    cacheDir: File,
    imageCapture: ImageCapture,
    executor: ExecutorService
): File = suspendCancellableCoroutine { continuation ->
    var capturedFile: File? = null

    continuation.invokeOnCancellation {
        capturedFile?.delete()
    }

    captureSingleFrame(
        cacheDir = cacheDir,
        imageCapture = imageCapture,
        executor = executor,
        onCaptured = { file ->
            capturedFile = file
            if (continuation.isActive) {
                continuation.resume(file)
            } else {
                file.delete()
            }
        },
        onError = { message ->
            if (continuation.isActive) {
                continuation.resumeWithException(LiveCaptureException(message))
            }
        }
    )
}

@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
@Composable
fun LiveScanScreen(
    onBack: () -> Unit,
    onPaperClick: (Paper) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler { onBack() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var statusMessage by remember {
        mutableStateOf("Point PaperEyes toward the paper title")
    }
    var highResCandidate by remember { mutableStateOf("") }
    var matchScore by remember { mutableStateOf<Double?>(null) }
    var captureInProgress by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var scanningLocked by remember { mutableStateOf(false) }
    var papers by remember { mutableStateOf<List<Paper>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDebug by remember { mutableStateOf(false) }

    val resolver = remember { PaperResolver() }
    val recognizer = remember { TextRecognizerService() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val disposedRef = remember { AtomicBoolean(false) }
    val cameraProviderRef = remember {
        AtomicReference<ProcessCameraProvider?>(null)
    }
    val imageAnalysisRef = remember {
        AtomicReference<ImageAnalysis?>(null)
    }

    // Scanner-session state is intentionally non-Compose state; mutating these
    // counters must not trigger recomposition of the camera preview.
    val candidateGate = remember {
        LiveScanCandidateGate(
            stableObservationsRequired = STABLE_TITLE_OBSERVATIONS,
            sameQueryCooldownMs = SAME_QUERY_COOLDOWN_MS
        )
    }
    val fallbackAttemptedRef = remember { AtomicBoolean(false) }
    val lookupInProgressRef = remember { AtomicBoolean(false) }
    val captureInProgressRef = remember { AtomicBoolean(false) }
    val scanningLockedRef = remember { AtomicBoolean(false) }
    val scanStartedAtRef = remember {
        AtomicLong(SystemClock.elapsedRealtime())
    }

    fun resetScanTracking() {
        candidateGate.reset()
        fallbackAttemptedRef.set(false)
        lookupInProgressRef.set(false)
        captureInProgressRef.set(false)
        scanningLockedRef.set(false)
        scanStartedAtRef.set(SystemClock.elapsedRealtime())
    }

    DisposableEffect(Unit) {
        onDispose {
            disposedRef.set(true)
            imageAnalysisRef.getAndSet(null)?.clearAnalyzer()
            cameraProviderRef.getAndSet(null)?.unbindAll()
            recognizer.close()
            cameraExecutor.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
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

    if (!hasCameraPermission) {
        LiveScanPermissionScreen(
            onBack = onBack,
            onAllowCamera = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewContext ->
                val previewView = PreviewView(previewContext).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(previewContext)

                cameraProviderFuture.addListener(
                    {
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            if (disposedRef.get()) {
                                cameraProvider.unbindAll()
                                return@addListener
                            }

                            cameraProviderRef.set(cameraProvider)

                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val analysisResolutionSelector =
                                ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            Size(
                                                LIVE_ANALYSIS_WIDTH,
                                                LIVE_ANALYSIS_HEIGHT
                                            ),
                                            ResolutionStrategy
                                                .FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                        )
                                    )
                                    .build()

                            val captureResolutionSelector =
                                ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            Size(
                                                FALLBACK_CAPTURE_WIDTH,
                                                FALLBACK_CAPTURE_HEIGHT
                                            ),
                                            ResolutionStrategy
                                                .FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                        )
                                    )
                                    .build()

                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(
                                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                                )
                                .setResolutionSelector(captureResolutionSelector)
                                .build()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setResolutionSelector(analysisResolutionSelector)
                                .setBackpressureStrategy(
                                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build()

                            imageAnalysisRef.set(imageAnalysis)

                            suspend fun identifyCandidate(
                                candidate: String,
                                evidenceType: DocumentLayoutAnalyzer.EvidenceType,
                                source: String,
                                corroboratingTitle: String? = null,
                                bypassCooldown: Boolean = false
                            ): Boolean {
                                if (
                                    scanningLockedRef.get() ||
                                    !lookupInProgressRef.compareAndSet(false, true)
                                ) {
                                    return false
                                }

                                val now = SystemClock.elapsedRealtime()
                                if (
                                    !bypassCooldown &&
                                    !candidateGate.canLookup(candidate, now)
                                ) {
                                    lookupInProgressRef.set(false)
                                    return false
                                }
                                candidateGate.markLookup(candidate, now)
                                resolving = true
                                errorMessage = null
                                statusMessage = "Checking scholarly sources…"

                                val networkStartedAt = SystemClock.elapsedRealtime()

                                try {
                                    val identifiedPaper: Paper?
                                    val score: Double

                                    when (evidenceType) {
                                        DocumentLayoutAnalyzer.EvidenceType.DOI,
                                        DocumentLayoutAnalyzer.EvidenceType.ARXIV -> {
                                            val result = resolver.resolve(candidate)
                                            identifiedPaper = result.papers.firstOrNull()
                                            score = if (identifiedPaper != null) 1.0 else 0.0
                                        }

                                        DocumentLayoutAnalyzer.EvidenceType.JOURNAL_CITATION -> {
                                            val result = resolver.resolve(candidate)
                                            val title = corroboratingTitle
                                                ?.trim()
                                                ?.takeIf { it.isNotBlank() }

                                            val titleMatch = title?.let {
                                                findBestPaperMatch(
                                                    query = it,
                                                    papers = result.papers
                                                )
                                            }

                                            identifiedPaper =
                                                titleMatch?.paper ?: result.papers.firstOrNull()
                                            score = titleMatch?.score
                                                ?: if (identifiedPaper != null) 0.95 else 0.0
                                        }

                                        DocumentLayoutAnalyzer.EvidenceType.TITLE -> {
                                            val fastPaper = resolver.resolveFastTitle(candidate)
                                            val fastScore = fastPaper?.let {
                                                titleSimilarity(candidate, it.title)
                                            } ?: 0.0

                                            if (
                                                fastPaper != null &&
                                                fastScore >= PAPER_MATCH_THRESHOLD
                                            ) {
                                                identifiedPaper = fastPaper
                                                score = fastScore
                                            } else {
                                                val result = resolver.resolve(candidate)
                                                val bestMatch = findBestPaperMatch(
                                                    query = candidate,
                                                    papers = result.papers
                                                )

                                                identifiedPaper = bestMatch
                                                    ?.takeIf {
                                                        it.score >= PAPER_MATCH_THRESHOLD
                                                    }
                                                    ?.paper
                                                score = bestMatch?.score ?: 0.0
                                            }
                                        }
                                    }

                                    matchScore = score.takeIf { it > 0.0 }

                                    if (identifiedPaper != null) {
                                        papers = listOf(identifiedPaper)
                                        scanningLockedRef.set(true)
                                        scanningLocked = true
                                        statusMessage = "Paper identified"

                                        if (BuildConfig.DEBUG) {
                                            val totalMs =
                                                SystemClock.elapsedRealtime() -
                                                        scanStartedAtRef.get()
                                            val networkMs =
                                                SystemClock.elapsedRealtime() -
                                                        networkStartedAt
                                            Log.d(
                                                TAG,
                                                "identified source=$source total=${totalMs}ms network=${networkMs}ms"
                                            )
                                        }

                                        return true
                                    }

                                    statusMessage =
                                        "No convincing match — keep the title in view"
                                    return false
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: Exception) {
                                    errorMessage = exception.toUserFacingMessage(
                                        "Paper lookup failed. Check your connection and try again."
                                    )
                                    statusMessage = "Lookup failed — trying again"
                                    return false
                                } finally {
                                    resolving = false
                                    lookupInProgressRef.set(false)
                                }
                            }

                            suspend fun runHighResolutionFallback() {
                                if (
                                    scanningLockedRef.get() ||
                                    lookupInProgressRef.get()
                                ) {
                                    return
                                }

                                if (!fallbackAttemptedRef.compareAndSet(false, true)) {
                                    return
                                }

                                if (!captureInProgressRef.compareAndSet(false, true)) {
                                    fallbackAttemptedRef.set(false)
                                    return
                                }

                                captureInProgress = true
                                statusMessage = "Refining scan…"
                                var file: File? = null

                                try {
                                    file = captureSingleFrameAwait(
                                        cacheDir = context.cacheDir,
                                        imageCapture = imageCapture,
                                        executor = cameraExecutor
                                    )

                                    statusMessage = "Reading high-resolution title…"
                                    val ocrResult = recognizer.recognizeCameraCapture(
                                        context = context,
                                        uri = Uri.fromFile(file)
                                    )
                                    val preferred = ocrResult.evidence.preferred
                                    val candidate = preferred?.query.orEmpty().trim()
                                    highResCandidate = candidate

                                    if (preferred == null || !isGoodCandidate(candidate)) {
                                        statusMessage =
                                            "Couldn't find distinctive paper metadata"
                                        return
                                    }

                                    identifyCandidate(
                                        candidate = candidate,
                                        evidenceType = preferred.type,
                                        source = "high-res-fallback",
                                        corroboratingTitle = ocrResult.evidence.bestTitle?.text,
                                        bypassCooldown = true
                                    )
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: LiveCaptureException) {
                                    errorMessage = exception.message
                                    statusMessage = "Capture failed"
                                } catch (exception: Exception) {
                                    errorMessage = exception.toUserFacingMessage(
                                        "OCR failed. Try another scan."
                                    )
                                    statusMessage = "OCR failed — trying again"
                                } finally {
                                    file?.delete()
                                    captureInProgress = false
                                    captureInProgressRef.set(false)
                                }
                            }

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (
                                    disposedRef.get() ||
                                    scanningLockedRef.get() ||
                                    lookupInProgressRef.get() ||
                                    captureInProgressRef.get()
                                ) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val mediaImage = imageProxy.image
                                if (mediaImage == null) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val frameStartedAt = SystemClock.elapsedRealtime()

                                coroutineScope.launch {
                                    var proxyClosed = false

                                    suspend fun runFallbackAfterClosingProxy() {
                                        if (!proxyClosed) {
                                            imageProxy.close()
                                            proxyClosed = true
                                        }
                                        runHighResolutionFallback()
                                    }

                                    try {
                                        val ocrResult = recognizer.recognizeMediaImage(
                                            mediaImage = mediaImage,
                                            rotationDegrees =
                                                imageProxy.imageInfo.rotationDegrees
                                        )

                                        // ML Kit has finished consuming the media image.
                                        // Release CameraX immediately; network lookup must not
                                        // hold an ImageProxy and stall the analysis stream.
                                        if (!proxyClosed) {
                                            imageProxy.close()
                                            proxyClosed = true
                                        }

                                        val preferred = ocrResult.evidence.preferred
                                        val candidate = preferred?.query.orEmpty().trim()
                                        if (BuildConfig.DEBUG) {
                                            highResCandidate = candidate
                                        }

                                        val letterCount =
                                            ocrResult.rawText.count(Char::isLetter)

                                        if (preferred == null || !isGoodCandidate(candidate)) {
                                            candidateGate.resetCandidate()
                                            statusMessage =
                                                if (letterCount >= 20) {
                                                    "Reading document layout…"
                                                } else {
                                                    "Point PaperEyes toward distinctive metadata"
                                                }

                                            val elapsed =
                                                SystemClock.elapsedRealtime() -
                                                        scanStartedAtRef.get()
                                            if (
                                                letterCount >= 20 &&
                                                elapsed >= HIGH_RES_FALLBACK_AFTER_MS
                                            ) {
                                                runFallbackAfterClosingProxy()
                                            }
                                            return@launch
                                        }

                                        val isExactOrStructuredEvidence =
                                            preferred.type !=
                                                DocumentLayoutAnalyzer.EvidenceType.TITLE

                                        if (isExactOrStructuredEvidence) {
                                            candidateGate.resetCandidate()
                                            identifyCandidate(
                                                candidate = candidate,
                                                evidenceType = preferred.type,
                                                source = "live-${preferred.type.name.lowercase()}",
                                                corroboratingTitle =
                                                    ocrResult.evidence.bestTitle?.text
                                            )
                                            return@launch
                                        }

                                        val stable = candidateGate.observe(candidate)
                                        statusMessage = "Reading title…"

                                        if (stable) {
                                            val identified = identifyCandidate(
                                                candidate = candidate,
                                                evidenceType = preferred.type,
                                                source = "live-title",
                                                corroboratingTitle =
                                                    ocrResult.evidence.bestTitle?.text
                                            )

                                            if (!identified) {
                                                val elapsed =
                                                    SystemClock.elapsedRealtime() -
                                                            scanStartedAtRef.get()
                                                if (
                                                    elapsed >=
                                                    HIGH_RES_FALLBACK_AFTER_MS
                                                ) {
                                                    runFallbackAfterClosingProxy()
                                                }
                                            }
                                        }

                                        if (BuildConfig.DEBUG) {
                                            val ocrMs =
                                                SystemClock.elapsedRealtime() -
                                                        frameStartedAt
                                            Log.d(TAG, "live OCR=${ocrMs}ms")
                                        }
                                    } catch (exception: CancellationException) {
                                        throw exception
                                    } catch (_: Exception) {
                                        // An isolated preview OCR failure should not stop
                                        // the camera stream. The next latest frame is used.
                                    } finally {
                                        if (!proxyClosed) {
                                            imageProxy.close()
                                        }
                                    }
                                }
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                                imageCapture
                            )
                            scanStartedAtRef.set(SystemClock.elapsedRealtime())
                        } catch (_: Exception) {
                            errorMessage = "Could not start camera."
                        }
                    },
                    ContextCompat.getMainExecutor(previewContext)
                )

                previewView
            }
        )

        LiveScanOverlay(
            statusMessage = statusMessage,
            highResCandidate = highResCandidate,
            matchScore = matchScore,
            captureInProgress = captureInProgress,
            resolving = resolving,
            scanningLocked = scanningLocked,
            errorMessage = errorMessage,
            paper = papers.firstOrNull(),
            showDebug = BuildConfig.DEBUG && showDebug,
            debugAvailable = BuildConfig.DEBUG,
            onBack = onBack,
            onToggleDebug = {
                if (BuildConfig.DEBUG) showDebug = !showDebug
            },
            onOpenPaper = onPaperClick,
            onScanAgain = {
                papers = emptyList()
                highResCandidate = ""
                matchScore = null
                errorMessage = null
                captureInProgress = false
                resolving = false
                scanningLocked = false
                statusMessage = "Point PaperEyes toward the paper title"
                resetScanTracking()
            }
        )
    }
}
