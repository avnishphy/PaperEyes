package com.example.papereyes.ui.scan

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.papereyes.BuildConfig
import com.example.papereyes.data.local.LibraryRepository
import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.ResolutionStatus
import com.example.papereyes.domain.PaperInputType
import com.example.papereyes.domain.PaperResolver
import com.example.papereyes.ocr.TextRecognizerService
import com.example.papereyes.ui.common.toUserFacingMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun ScanScreen(
    libraryRepository: LibraryRepository,
    onPaperClick: (Paper) -> Unit,
    onLiveScanClick: () -> Unit,
    onLibraryClick: () -> Unit = {}
) {

    /*
     * ================================================================
     * STATE
     * ================================================================
     */

    var inputText by remember {
        mutableStateOf("")
    }


    var rawOcrText by remember {
        mutableStateOf("")
    }


    var results by remember {
        mutableStateOf<List<Paper>>(
            emptyList()
        )
    }


    var loading by remember {
        mutableStateOf(false)
    }


    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }


    var resolutionStatus by remember { mutableStateOf(ResolutionStatus.NOT_FOUND) }

    var detectedType by remember {
        mutableStateOf<PaperInputType?>(null)
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


    val textRecognizer =
        remember {
            TextRecognizerService()
        }


    val coroutineScope =
        rememberCoroutineScope()


    val context =
        LocalContext.current


    /*
     * ================================================================
     * OCR SERVICE CLEANUP
     * ================================================================
     */
    DisposableEffect(
        textRecognizer
    ) {

        onDispose {

            textRecognizer.close()
        }
    }


    /*
     * ================================================================
     * MANUAL / TEXT RESOLUTION
     * ================================================================
     */
    suspend fun resolveText(
        text: String
    ) {

        val cleaned =
            text.trim()


        if (cleaned.isBlank()) {
            return
        }


        loading =
            true


        errorMessage =
            null


        results =
            emptyList()


        detectedType =
            null


        try {

            val result =
                resolver.resolve(
                    cleaned
                )


            detectedType =
                result.inputType


            resolutionStatus = result.status
            results =
                result.papers


            if (
                result.papers.isEmpty()
            ) {

                errorMessage =
                    "No matching papers found."
            }

        } catch (
            exception: CancellationException
        ) {

            throw exception

        } catch (
            exception: Exception
        ) {

            errorMessage =
                exception.toUserFacingMessage(
                    "Unable to identify paper."
                )

        } finally {

            loading =
                false
        }
    }


    /*
     * ================================================================
     * IMAGE PICKER
     * ================================================================
     */
    val imagePicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .PickVisualMedia()
        ) { uri ->

            if (uri == null) {

                return@rememberLauncherForActivityResult
            }


            coroutineScope.launch {

                loading =
                    true


                errorMessage =
                    null


                results =
                    emptyList()


                detectedType =
                    null


                rawOcrText =
                    ""


                try {

                    /*
                     * ------------------------------------------------
                     * OCR
                     * ------------------------------------------------
                     */
                    val ocrResult =
                        withContext(
                            Dispatchers.IO
                        ) {

                            textRecognizer
                                .recognizeImage(
                                    context =
                                        context,

                                    uri =
                                        uri
                                )
                        }


                    rawOcrText =
                        ocrResult.rawText


                    val bestQuery =
                        ocrResult
                            .bestQuery
                            .trim()


                    inputText =
                        bestQuery


                    if (
                        bestQuery.isBlank() && ocrResult.evidence.fingerprints.isEmpty()
                    ) {

                        errorMessage =
                            if (ocrResult.rawText.isBlank()) "No readable text found in image."
                            else "Text was read, but no reliable paper identity was found. Try the title or journal header."

                        return@launch
                    }


                    /*
                     * ------------------------------------------------
                     * PAPER RESOLUTION
                     * ------------------------------------------------
                     */
                    val result =
                        resolver.resolveEvidence(ocrResult.evidence)


                    detectedType =
                        result.inputType


                    resolutionStatus = result.status
                    results =
                        result.papers


                    if (
                        result.papers.isEmpty()
                    ) {

                        errorMessage =
                            if (result.status == ResolutionStatus.PROVIDERS_UNAVAILABLE)
                                "Scholarly services are unavailable or rate-limited. Try again later."
                            else "Could not identify confidently. Try the title or journal header."
                    }

                } catch (
                    exception: CancellationException
                ) {

                    throw exception

                } catch (
                    exception: Exception
                ) {

                    android.util.Log.e(
                        "PaperEyesImport",
                        "Import failed",
                        exception
                    )

                    errorMessage =
                        exception.toUserFacingMessage(
                            "Could not read image."
                        )
                } finally {

                    loading =
                        false
                }
            }
        }


    /*
     * ================================================================
     * SCREEN
     * ================================================================
     *
     * One LazyColumn handles:
     *
     * - landing page
     * - OCR information
     * - detected input type
     * - results
     *
     * This avoids nested scrolling between the landing content and
     * paper results.
     */
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start =
                    20.dp,

                end =
                    20.dp,

                top =
                    20.dp,

                bottom =
                    20.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        /*
         * ============================================================
         * LANDING PAGE
         * ============================================================
         */
        item {

            PaperEyesLandingSection(
                inputText =
                    inputText,

                loading =
                    loading,

                /*
                 * Error is displayed below separately so that the
                 * landing component does not duplicate it.
                 */
                errorMessage =
                    null,

                onInputTextChange = { newValue ->

                    inputText =
                        newValue


                    /*
                     * Clear an old error once the user starts editing.
                     */
                    if (
                        errorMessage != null
                    ) {

                        errorMessage =
                            null
                    }
                },

                /*
                 * ----------------------------------------------------
                 * MANUAL IDENTIFICATION
                 * ----------------------------------------------------
                 */
                onIdentifyPaper = {

                    if (
                        inputText.isNotBlank() &&
                        !loading
                    ) {

                        coroutineScope.launch {

                            resolveText(
                                inputText
                            )
                        }
                    }
                },

                /*
                 * ----------------------------------------------------
                 * LIVE SCAN
                 * ----------------------------------------------------
                 */
                onLiveScan =
                    onLiveScanClick,

                /*
                 * ----------------------------------------------------
                 * IMPORT IMAGE
                 * ----------------------------------------------------
                 */
                onImportImage = {

                    if (!loading) {

                        imagePicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia
                                    .ImageOnly
                            )
                        )
                    }
                },

                /*
                 * ----------------------------------------------------
                 * LIBRARY
                 * ----------------------------------------------------
                 *
                 * This is optional for now so existing MainActivity
                 * code still compiles.
                 */
                onOpenLibrary =
                    onLibraryClick
            )
        }


        /*
         * ============================================================
         * ERROR
         * ============================================================
         */
        errorMessage
            ?.takeIf {

                it.isNotBlank()
            }
            ?.let { message ->

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                message,

                            modifier =
                                Modifier.padding(
                                    14.dp
                                ),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }
            }


        /*
         * ============================================================
         * INPUT TYPE
         * ============================================================
         */
        item {
            Text("Images are processed on-device. Selected identifiers, titles, or short phrases are sent to scholarly services for lookup.",
                style = MaterialTheme.typography.bodySmall)
        }

        detectedType
            ?.let { type ->

                item {

                    val label =
                        when (type) {

                            PaperInputType.DOI ->
                                "DOI detected"

                            PaperInputType.ARXIV ->
                                "arXiv identifier detected"

                            PaperInputType.JOURNAL_CITATION ->
                                "Journal citation detected"

                            PaperInputType.TITLE_OR_OCR ->
                                "Title search"

                            PaperInputType.INTERIOR_TEXT ->
                                "Interior-page phrase search — experimental"
                        }


                    Text(
                        text =
                            label,

                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }


        /*
         * ============================================================
         * OCR DEBUG / FEEDBACK
         * ============================================================
         *
         * Raw OCR is useful during development but can contain page text,
         * so it is deliberately absent from release builds.
         */
        if (
            BuildConfig.DEBUG &&
            rawOcrText.isNotBlank()
        ) {

            item {

                OcrDetectedCard(
                    rawOcrText =
                        rawOcrText
                )
            }
        }


        /*
         * ============================================================
         * RESULTS HEADER
         * ============================================================
         */
        if (
            results.isNotEmpty()
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(
                    text =
                        if (
                            results.size == 1
                        ) {

                            if (resolutionStatus == ResolutionStatus.VERIFIED) "Paper identified"
                            else "Possible match — confirm before saving"

                        } else {

                            "${results.size} possible matches"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
            }
        }


        /*
         * ============================================================
         * PAPER RESULTS
         * ============================================================
         */
        itemsIndexed(
            items =
                results,

            key = { index, paper ->

                "${paper.identityKey}:$index"
            }
        ) { _, paper ->

            PaperResultCard(
                paper =
                    paper,

                onClick = {

                    onPaperClick(
                        paper
                    )
                },

                onSave = {

                    coroutineScope.launch {

                        try {
                            val saved =
                                libraryRepository
                                    .savePaper(
                                        paper
                                    )

                            Toast
                                .makeText(
                                    context,
                                    if (saved) {
                                        "Paper saved"
                                    } else {
                                        "Already in library"
                                    },
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Throwable) {
                            Toast
                                .makeText(
                                    context,
                                    "Couldn't save paper",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                }
            )
        }
    }
}


/*
 * ================================================================
 * OCR RESULT CARD
 * ================================================================
 */
@Composable
private fun OcrDetectedCard(
    rawOcrText: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(
                text =
                    "Text recognized from image",

                style =
                    MaterialTheme
                        .typography
                        .labelLarge
            )


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Text(
                text =
                    rawOcrText.take(
                        500
                    ),

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
    }
}


/*
 * ================================================================
 * PAPER RESULT CARD
 * ================================================================
 */
@Composable
fun PaperResultCard(
    paper: Paper,
    onClick: () -> Unit,
    onSave: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {

                    onClick()
                },

        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(
                    20.dp
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            /*
             * --------------------------------------------------------
             * TITLE
             * --------------------------------------------------------
             */
            Text(
                text =
                    paper.title,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )


            /*
             * --------------------------------------------------------
             * AUTHORS
             * --------------------------------------------------------
             */
            if (
                paper.authors.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
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


            /*
             * --------------------------------------------------------
             * YEAR
             * --------------------------------------------------------
             */
            paper.year
                ?.let { year ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Text(
                        text =
                            year.toString(),

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


            /*
             * --------------------------------------------------------
             * DOI
             * --------------------------------------------------------
             */
            paper.doi
                ?.takeIf {

                    it.isNotBlank()
                }
                ?.let { doi ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            "DOI: $doi",

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


            /*
             * --------------------------------------------------------
             * SAVE
             * --------------------------------------------------------
             */
            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            Button(
                onClick =
                    onSave
            ) {

                Text(
                    "Save paper"
                )
            }
        }
    }
}