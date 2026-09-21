package com.example.papereyes.ui.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.reference.ReferenceResolution
import com.example.papereyes.ui.common.ReferenceBatchSummary


/*
 * ================================================================
 * FULL LIVE-SCAN OVERLAY
 * ================================================================
 *
 * IMPORTANT:
 *
 * The CameraX PreviewView is an AndroidView underneath this
 * composable.
 *
 * Explicit zIndex() values are used here so that UI controls remain
 * above the camera preview for BOTH drawing and touch handling.
 *
 * This file contains presentation only:
 *
 * - no CameraX
 * - no OCR
 * - no resolver
 * - no burst logic
 */
@Composable
fun LiveScanOverlay(
    statusMessage: String,
    highResCandidate: String,
    matchScore: Double?,
    captureInProgress: Boolean,
    resolving: Boolean,
    scanningLocked: Boolean,
    errorMessage: String?,
    paper: Paper?,
    referenceOutcomes: List<ReferenceResolution>,
    referenceCompleted: Int,
    referenceTotal: Int,
    zoomRatio: Float,
    minimumZoomRatio: Float,
    maximumZoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    showDebug: Boolean,
    debugAvailable: Boolean,
    onBack: () -> Unit,
    onToggleDebug: () -> Unit,
    onOpenPaper: (Paper) -> Unit,
    onScanAgain: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex(100f)
    ) {

        /*
         * ------------------------------------------------------------
         * SUBTLE AIMING GUIDE
         * ------------------------------------------------------------
         */
        LiveScanGuide(
            locked =
                scanningLocked,

            modifier =
                Modifier
                    .align(
                        Alignment.Center
                    )
                    .fillMaxWidth(
                        0.90f
                    )
                    .height(
                        250.dp
                    )
                    .zIndex(
                        1f
                    )
        )


        /*
         * ------------------------------------------------------------
         * TOP CONTROLS
         * ------------------------------------------------------------
         */
        LiveScanTopBar(
            showDebug = showDebug,
            debugAvailable = debugAvailable,

            onBack =
                onBack,

            onToggleDebug =
                onToggleDebug,

            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal =
                            12.dp,

                        vertical =
                            8.dp
                    )
                    .zIndex(
                        50f
                    )
        )


        /*
         * ------------------------------------------------------------
         * SCANNER STATUS
         * ------------------------------------------------------------
         */
        LiveScanStatusPill(
            statusMessage =
                statusMessage,

            captureInProgress =
                captureInProgress,

            resolving =
                resolving,

            scanningLocked =
                scanningLocked,

            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .statusBarsPadding()
                    .padding(
                        top =
                            70.dp
                    )
                    .zIndex(
                        20f
                    )
        )

        if (maximumZoomRatio > minimumZoomRatio + 0.01f) {
            LiveScanZoomControl(
                zoomRatio = zoomRatio,
                minimumZoomRatio = minimumZoomRatio,
                maximumZoomRatio = maximumZoomRatio,
                onZoomChange = onZoomChange,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 122.dp)
                    .zIndex(20f)
            )
        }


        /*
         * ------------------------------------------------------------
         * BOTTOM CONTENT
         * ------------------------------------------------------------
         */
        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    )
                    .zIndex(
                        30f
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            errorMessage
                ?.takeIf {

                    it.isNotBlank()
                }
                ?.let { message ->

                    LiveScanErrorCard(
                        message =
                            message
                    )
                }


            /*
             * Debug data remains available during development,
             * but is hidden by default.
             */
            if (showDebug) {

                LiveScanDebugPanel(
                    statusMessage =
                        statusMessage,

                    highResCandidate =
                        highResCandidate,

                    matchScore =
                        matchScore
                )
            }


            if (referenceTotal > 0) {

                ReferenceBatchSummary(
                    outcomes = referenceOutcomes,
                    completed = referenceCompleted,
                    total = referenceTotal,
                    scrollable = true,
                    onPaperClick = onOpenPaper
                )

                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan again")
                }

            } else if (paper != null) {

                LiveScanResultCard(
                    paper =
                        paper,

                    onOpenPaper = {

                        onOpenPaper(
                            paper
                        )
                    },

                    onScanAgain =
                        onScanAgain
                )

            } else {

                LiveScanHint()
            }
        }
    }
}


/*
 * ================================================================
 * CAMERA PERMISSION SCREEN
 * ================================================================
 */
@Composable
fun LiveScanPermissionScreen(
    onBack: () -> Unit,
    onAllowCamera: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                )
    ) {

        /*
         * Same style of back control used on Live Scan.
         */
        BackControl(
            onClick =
                onBack,

            modifier =
                Modifier
                    .align(
                        Alignment.TopStart
                    )
                    .statusBarsPadding()
                    .padding(
                        12.dp
                    )
        )


        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.Center
                    )
                    .padding(
                        32.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "Camera access",

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Text(
                text =
                    "Images stay on-device. Selected text is sent to scholarly services; temporary camera files are removed after processing.",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Button(
                onClick =
                    onAllowCamera
            ) {

                Text(
                    "Allow camera"
                )
            }
        }
    }
}


/*
 * ================================================================
 * TOP BAR
 * ================================================================
 */
@Composable
private fun LiveScanTopBar(
    showDebug: Boolean,
    debugAvailable: Boolean,
    onBack: () -> Unit,
    onToggleDebug: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier =
            modifier,

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * Do NOT use a tiny TextButton here.
         *
         * Surface(onClick = ...) creates a clear Material clickable
         * surface with a much larger hit target.
         */
        BackControl(
            onClick =
                onBack
        )


        if (debugAvailable) {
            Surface(
                onClick =
                    onToggleDebug,

                color =
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(
                            alpha =
                                0.92f
                        ),

                contentColor =
                    MaterialTheme
                        .colorScheme
                        .onSurface,

                shape =
                    RoundedCornerShape(
                        100.dp
                    ),

                tonalElevation =
                    4.dp,

                shadowElevation =
                    2.dp
            ) {

                Box(
                    modifier =
                        Modifier
                            .height(
                                48.dp
                            )
                            .padding(
                                horizontal =
                                    18.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            if (
                                showDebug
                            ) {

                                "Hide debug"

                            } else {

                                "Debug"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .labelLarge
                    )
                }
            }
        }
    }
}


/*
 * ================================================================
 * BACK CONTROL
 * ================================================================
 *
 * Separate component so the clickable target is explicit and easy
 * to debug independently from the rest of Live Scan.
 */
@Composable
private fun BackControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        onClick =
            onClick,

        modifier =
            modifier
                .zIndex(
                    100f
                ),

        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha =
                        0.94f
                ),

        contentColor =
            MaterialTheme
                .colorScheme
                .onSurface,

        shape =
            RoundedCornerShape(
                100.dp
            ),

        tonalElevation =
            4.dp,

        shadowElevation =
            3.dp
    ) {

        Row(
            modifier =
                Modifier
                    .height(
                        48.dp
                    )
                    .padding(
                        horizontal =
                            18.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(
                    6.dp
                )
        ) {

            Text(
                text =
                    "‹",

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )


            Text(
                text =
                    "Back",

                style =
                    MaterialTheme
                        .typography
                        .labelLarge
            )
        }
    }
}


/*
 * ================================================================
 * STATUS PILL
 * ================================================================
 */
@Composable
private fun LiveScanStatusPill(
    statusMessage: String,
    captureInProgress: Boolean,
    resolving: Boolean,
    scanningLocked: Boolean,
    modifier: Modifier = Modifier
) {

    val shortStatus =
        when {

            scanningLocked ->

                "Paper identified"


            resolving ->

                "Identifying paper…"


            captureInProgress ->

                "Capturing…"


            statusMessage.contains(
                "Reading",
                ignoreCase =
                    true
            ) ->

                "Reading title…"


            statusMessage.contains(
                "detected",
                ignoreCase =
                    true
            ) ->

                "Text detected"


            else ->

                "Looking for a paper"
        }


    Surface(
        modifier =
            modifier,

        color =
            if (
                scanningLocked
            ) {

                MaterialTheme
                    .colorScheme
                    .primaryContainer
                    .copy(
                        alpha =
                            0.95f
                    )

            } else {

                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(
                        alpha =
                            0.90f
                    )
            },

        shape =
            RoundedCornerShape(
                100.dp
            ),

        tonalElevation =
            4.dp
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal =
                        16.dp,

                    vertical =
                        10.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            if (
                captureInProgress ||
                resolving
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            16.dp
                        ),

                    strokeWidth =
                        2.dp
                )
            }


            Text(
                text =
                    shortStatus,

                style =
                    MaterialTheme
                        .typography
                        .labelLarge
            )
        }
    }
}


/*
 * ================================================================
 * AIMING GUIDE
 * ================================================================
 */
@Composable
private fun LiveScanGuide(
    locked: Boolean,
    modifier: Modifier = Modifier
) {

    val guideColor =
        if (
            locked
        ) {

            MaterialTheme
                .colorScheme
                .primary

        } else {

            Color.White.copy(
                alpha =
                    0.72f
            )
        }


    Canvas(
        modifier =
            modifier
    ) {

        val strokeWidth =
            3.dp.toPx()


        val cornerLength =
            32.dp.toPx()


        /*
         * TOP LEFT
         */
        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    0f,
                    cornerLength
                ),

            end =
                Offset(
                    0f,
                    0f
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    0f,
                    0f
                ),

            end =
                Offset(
                    cornerLength,
                    0f
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        /*
         * TOP RIGHT
         */
        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    size.width -
                            cornerLength,
                    0f
                ),

            end =
                Offset(
                    size.width,
                    0f
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    size.width,
                    0f
                ),

            end =
                Offset(
                    size.width,
                    cornerLength
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        /*
         * BOTTOM LEFT
         */
        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    0f,
                    size.height -
                            cornerLength
                ),

            end =
                Offset(
                    0f,
                    size.height
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    0f,
                    size.height
                ),

            end =
                Offset(
                    cornerLength,
                    size.height
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        /*
         * BOTTOM RIGHT
         */
        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    size.width -
                            cornerLength,
                    size.height
                ),

            end =
                Offset(
                    size.width,
                    size.height
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        drawLine(
            color =
                guideColor,

            start =
                Offset(
                    size.width,
                    size.height -
                            cornerLength
                ),

            end =
                Offset(
                    size.width,
                    size.height
                ),

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )
    }
}


/*
 * ================================================================
 * BOTTOM HINT
 * ================================================================
 */
@Composable
private fun LiveScanHint() {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(
                    alpha =
                        0.86f
                ),

        shape =
            RoundedCornerShape(
                18.dp
            )
    ) {

        Text(
            text =
                "Point at the paper title — capture is automatic",

            modifier =
                Modifier.padding(
                    horizontal =
                        16.dp,

                    vertical =
                        12.dp
                ),

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}


/*
 * ================================================================
 * PAPER FOUND
 * ================================================================
 */
@Composable
private fun LiveScanResultCard(
    paper: Paper,
    onOpenPaper: () -> Unit,
    onScanAgain: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                24.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(
                            alpha =
                                0.97f
                        )
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    8.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                text =
                    "Paper found",

                style =
                    MaterialTheme
                        .typography
                        .labelLarge,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    paper.title,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )


            if (
                paper.authors.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        paper.authors,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


            paper.year
                ?.let { year ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )


                    Text(
                        text =
                            year.toString(),

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Button(
                onClick =
                    onOpenPaper,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Open paper"
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            OutlinedButton(
                onClick =
                    onScanAgain,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Scan another"
                )
            }
        }
    }
}


/*
 * ================================================================
 * DEBUG PANEL
 * ================================================================
 */
@Composable
private fun LiveScanDebugPanel(
    statusMessage: String,
    highResCandidate: String,
    matchScore: Double?
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(
                    alpha =
                        0.95f
                ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        tonalElevation =
            3.dp
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Text(
                text =
                    "Scanner debug",

                style =
                    MaterialTheme
                        .typography
                        .labelLarge
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            DebugRow(
                label =
                    "State",

                value =
                    statusMessage
            )



            matchScore
                ?.let { score ->

                    DebugRow(
                        label =
                            "Title similarity (heuristic)",

                        value =
                            String.format(java.util.Locale.ROOT, "%.2f", score)
                    )
                }


            if (
                highResCandidate.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        "OCR",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(
                    text =
                        highResCandidate.take(
                            400
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}


/*
 * ================================================================
 * DEBUG ROW
 * ================================================================
 */
@Composable
private fun DebugRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        2.dp
                ),

        verticalAlignment =
            Alignment.Top
    ) {

        Text(
            text =
                "$label:",

            modifier =
                Modifier.width(
                    82.dp
                ),

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Text(
            text =
                value,

            modifier =
                Modifier.weight(
                    1f
                ),

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}


/*
 * ================================================================
 * ERROR CARD
 * ================================================================
 */
@Composable
private fun LiveScanErrorCard(
    message: String
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        color =
            MaterialTheme
                .colorScheme
                .errorContainer
                .copy(
                    alpha =
                        0.95f
                ),

        shape =
            RoundedCornerShape(
                16.dp
            )
    ) {

        Text(
            text =
                message,

            modifier =
                Modifier.padding(
                    12.dp
                ),

            style =
                MaterialTheme
                    .typography
                    .bodySmall,

            color =
                MaterialTheme
                    .colorScheme
                    .onErrorContainer
        )
    }
}

@Composable
private fun LiveScanZoomControl(
    zoomRatio: Float,
    minimumZoomRatio: Float,
    maximumZoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { onZoomChange(zoomRatio / 1.25f) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text("−", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Slider(
                    value = zoomRatio.coerceIn(minimumZoomRatio, maximumZoomRatio),
                    onValueChange = onZoomChange,
                    valueRange = minimumZoomRatio..maximumZoomRatio,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = { onZoomChange(zoomRatio * 1.25f) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text("+", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Text(
                    text = "%.1f×".format(zoomRatio),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(
                text = "Pinch or slide to zoom",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
