package com.example.papereyes.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun PaperEyesLandingSection(
    inputText: String,
    loading: Boolean,
    errorMessage: String?,
    onInputTextChange: (String) -> Unit,
    onIdentifyPaper: () -> Unit,
    onLiveScan: () -> Unit,
    onImportImage: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier.fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        PaperEyesHero()


        /*
         * ============================================================
         * PRIMARY APP FEATURES
         * ============================================================
         *
         * Scan, import and library have equal visual weight.
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            FeatureCard(
                title =
                    "Live Scan",

                subtitle =
                    "Camera",

                iconType =
                    FeatureIcon.Scan,

                onClick =
                    onLiveScan,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            FeatureCard(
                title =
                    "Import",

                subtitle =
                    "Image / screenshot",

                iconType =
                    FeatureIcon.Import,

                onClick =
                    onImportImage,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            FeatureCard(
                title =
                    "Library",

                subtitle =
                    "Papers & projects",

                iconType =
                    FeatureIcon.Library,

                onClick =
                    onOpenLibrary,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )


        ManualSearchCard(
            inputText =
                inputText,

            loading =
                loading,

            onInputTextChange =
                onInputTextChange,

            onIdentifyPaper =
                onIdentifyPaper
        )


        errorMessage
            ?.takeIf {

                it.isNotBlank()
            }
            ?.let { message ->

                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    color =
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                ) {

                    Text(
                        text =
                            message,

                        modifier =
                            Modifier.padding(
                                14.dp
                            ),

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onErrorContainer
                    )
                }
            }
    }
}


/*
 * ================================================================
 * BRAND HEADER
 * ================================================================
 */
@Composable
private fun PaperEyesHero() {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 2.dp,
                    bottom = 2.dp
                )
    ) {

        /*
         * ------------------------------------------------------------
         * APP NAME
         * ------------------------------------------------------------
         */
        Text(
            text =
                "PaperEyes",

            style =
                MaterialTheme
                    .typography
                    .headlineLarge,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )


        /*
         * Short, functional description rather than marketing copy.
         */
        Text(
            text =
                "Scan, search, and organize research papers.",

            style =
                MaterialTheme
                    .typography
                    .bodySmall,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(
                    22.dp
                )
        )


        /*
         * Direct description of the two main jobs of the app.
         */
        Text(
            text =
                "Find papers.\nOrganize by project.",

            style =
                MaterialTheme
                    .typography
                    .displaySmall
                    .copy(
                        lineHeight =
                            40.sp
                    ),

            fontWeight =
                FontWeight.SemiBold
        )


        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )
    }
}


/*
 * ================================================================
 * FEATURE CARDS
 * ================================================================
 */
private enum class FeatureIcon {

    Scan,

    Import,

    Library
}


@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    iconType: FeatureIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        onClick =
            onClick,

        modifier =
            modifier.height(
                126.dp
            ),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .padding(
                        14.dp
                    )
                    .fillMaxWidth()
        ) {

            FeatureIconBox(
                type =
                    iconType
            )


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            Text(
                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .titleSmall,

                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )


            Text(
                text =
                    subtitle,

                style =
                    MaterialTheme
                        .typography
                        .labelSmall,

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
 * FEATURE ICON
 * ================================================================
 */
@Composable
private fun FeatureIconBox(
    type: FeatureIcon
) {

    val primary =
        MaterialTheme
            .colorScheme
            .primary


    Surface(
        shape =
            RoundedCornerShape(
                10.dp
            ),

        color =
            MaterialTheme
                .colorScheme
                .secondaryContainer
    ) {

        Canvas(
            modifier =
                Modifier
                    .size(
                        34.dp
                    )
                    .padding(
                        8.dp
                    )
        ) {

            val stroke =
                1.8.dp.toPx()


            when (
                type
            ) {

                /*
                 * ----------------------------------------------------
                 * SCAN
                 * ----------------------------------------------------
                 *
                 * Four scan corners.
                 */
                FeatureIcon.Scan -> {

                    val corner =
                        size.width *
                                0.32f


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset(
                                0f,
                                corner
                            ),

                        end =
                            Offset.Zero,

                        strokeWidth =
                            stroke
                    )


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset.Zero,

                        end =
                            Offset(
                                corner,
                                0f
                            ),

                        strokeWidth =
                            stroke
                    )


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset(
                                size.width -
                                        corner,
                                0f
                            ),

                        end =
                            Offset(
                                size.width,
                                0f
                            ),

                        strokeWidth =
                            stroke
                    )


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset(
                                size.width,
                                0f
                            ),

                        end =
                            Offset(
                                size.width,
                                corner
                            ),

                        strokeWidth =
                            stroke
                    )


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset(
                                0f,
                                size.height -
                                        corner
                            ),

                        end =
                            Offset(
                                0f,
                                size.height
                            ),

                        strokeWidth =
                            stroke
                    )


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset(
                                0f,
                                size.height
                            ),

                        end =
                            Offset(
                                corner,
                                size.height
                            ),

                        strokeWidth =
                            stroke
                    )


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset(
                                size.width -
                                        corner,
                                size.height
                            ),

                        end =
                            Offset(
                                size.width,
                                size.height
                            ),

                        strokeWidth =
                            stroke
                    )


                    drawLine(
                        color =
                            primary,

                        start =
                            Offset(
                                size.width,
                                size.height -
                                        corner
                            ),

                        end =
                            Offset(
                                size.width,
                                size.height
                            ),

                        strokeWidth =
                            stroke
                    )
                }


                /*
                 * ----------------------------------------------------
                 * IMPORT
                 * ----------------------------------------------------
                 *
                 * Simple image frame.
                 */
                FeatureIcon.Import -> {

                    drawRect(
                        color =
                            primary,

                        topLeft =
                            Offset(
                                size.width *
                                        0.12f,

                                size.height *
                                        0.18f
                            ),

                        size =
                            Size(
                                size.width *
                                        0.76f,

                                size.height *
                                        0.64f
                            ),

                        style =
                            Stroke(
                                width =
                                    stroke
                            )
                    )


                    drawCircle(
                        color =
                            primary,

                        radius =
                            size.width *
                                    0.09f,

                        center =
                            Offset(
                                size.width *
                                        0.65f,

                                size.height *
                                        0.38f
                            )
                    )
                }


                /*
                 * ----------------------------------------------------
                 * LIBRARY
                 * ----------------------------------------------------
                 */
                FeatureIcon.Library -> {

                    val xs =
                        size.width *
                                0.22f


                    val xe =
                        size.width *
                                0.78f


                    listOf(
                        0.30f,
                        0.50f,
                        0.70f
                    ).forEach { y ->

                        drawLine(
                            color =
                                primary,

                            start =
                                Offset(
                                    xs,
                                    size.height *
                                            y
                                ),

                            end =
                                Offset(
                                    xe,
                                    size.height *
                                            y
                                ),

                            strokeWidth =
                                stroke,

                            cap =
                                StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}


/*
 * ================================================================
 * SEARCH
 * ================================================================
 */
@Composable
private fun ManualSearchCard(
    inputText: String,
    loading: Boolean,
    onInputTextChange: (String) -> Unit,
    onIdentifyPaper: () -> Unit
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
                        .surfaceContainerLow
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
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
             * SECTION TITLE
             * --------------------------------------------------------
             */
            Text(
                text =
                    "Search manually",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            /*
             * --------------------------------------------------------
             * SEARCH FIELD
             * --------------------------------------------------------
             */
            OutlinedTextField(
                value =
                    inputText,

                onValueChange =
                    onInputTextChange,

                modifier =
                    Modifier.fillMaxWidth(),

                label = {

                    Text(
                        text =
                            "Title, DOI, arXiv ID, or URL"
                    )
                },

                minLines =
                    2,

                maxLines =
                    5,

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                keyboardOptions =
                    KeyboardOptions(
                        imeAction =
                            ImeAction.Search
                    ),

                keyboardActions =
                    KeyboardActions(
                        onSearch = {

                            if (
                                inputText.isNotBlank() &&
                                !loading
                            ) {

                                onIdentifyPaper()
                            }
                        }
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            /*
             * --------------------------------------------------------
             * SEARCH BUTTON
             * --------------------------------------------------------
             */
            Button(
                onClick =
                    onIdentifyPaper,

                enabled =
                    inputText.isNotBlank() &&
                            !loading,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),

                shape =
                    RoundedCornerShape(
                        16.dp
                    )
            ) {

                if (
                    loading
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                20.dp
                            ),

                        strokeWidth =
                            2.dp
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
                            )
                    )


                    Text(
                        text =
                            "Searching…"
                    )

                } else {

                    Text(
                        text =
                            "Search"
                    )
                }
            }
        }
    }
}