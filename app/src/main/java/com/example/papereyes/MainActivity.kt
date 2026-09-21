package com.example.papereyes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.papereyes.data.local.LibraryRepository
import com.example.papereyes.data.local.PaperDatabase
import com.example.papereyes.data.local.ProjectEntity
import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.evidence.ScanSubject
import com.example.papereyes.ui.detail.PaperDetailScreen
import com.example.papereyes.ui.library.LibraryScreen
import com.example.papereyes.ui.library.ProjectScreen
import com.example.papereyes.ui.live.LiveScanScreen
import com.example.papereyes.ui.scan.ScanScreen
import com.example.papereyes.ui.theme.PaperEyesTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        /*
         * ============================================================
         * DATABASE
         * ============================================================
         */
        val database =
            PaperDatabase.getDatabase(
                applicationContext
            )


        val libraryRepository =
            LibraryRepository(
                database = database
            )


        /*
         * ============================================================
         * COMPOSE
         * ============================================================
         */
        setContent {

            PaperEyesTheme {

                /*
                 * ----------------------------------------------------
                 * TOP-LEVEL NAVIGATION STATE
                 * ----------------------------------------------------
                 *
                 * "scan" is effectively the PaperEyes home screen.
                 *
                 * We deliberately keep the simple state-based
                 * navigation rather than introducing Navigation
                 * Compose at this stage.
                 */
                var currentScreen by rememberSaveable {

                    mutableStateOf(
                        "scan"
                    )
                }


                /*
                 * Currently opened paper detail.
                 *
                 * This can be reached from:
                 *
                 * - Home/manual search
                 * - Live Scan
                 * - Library / All Papers
                 * - Project
                 */
                var selectedPaper by remember {

                    mutableStateOf<Paper?>(
                        null
                    )
                }


                /*
                 * Currently opened project.
                 *
                 * Keeping this state set while a paper detail is open
                 * allows:
                 *
                 * Project → Paper Detail → Back → Project
                 */
                var selectedProject by remember {

                    mutableStateOf<ProjectEntity?>(
                        null
                    )
                }


                /*
                 * Whether the Live Scan camera screen is open.
                 */
                var liveScanOpen by rememberSaveable {

                    mutableStateOf(
                        false
                    )
                }

                var liveScanSubjectName by rememberSaveable {
                    mutableStateOf(ScanSubject.JOURNAL_PAPER.name)
                }


                /*
                 * ----------------------------------------------------
                 * PAPER DETAIL BACK
                 * ----------------------------------------------------
                 */
                val closePaperDetail: () -> Unit = {

                    selectedPaper =
                        null
                }


                /*
                 * ====================================================
                 * SYSTEM BACK HANDLING
                 * ====================================================
                 *
                 * Navigation hierarchy:
                 *
                 * Paper Detail
                 *      ↓
                 * previous underlying screen
                 *
                 * Project
                 *      ↓
                 * Library
                 *
                 * Library
                 *      ↓
                 * Home
                 *
                 * Live Scan handles its own BackHandler internally.
                 */
                if (
                    selectedPaper != null
                ) {

                    /*
                     * If selectedProject is still set, closing the paper
                     * exposes ProjectScreen again.
                     *
                     * Otherwise we return to the Library/Home screen
                     * that was already underneath.
                     */
                    BackHandler {

                        closePaperDetail()
                    }

                } else if (
                    selectedProject != null
                ) {

                    /*
                     * Project → Library
                     */
                    BackHandler {

                        selectedProject =
                            null
                    }

                } else if (
                    currentScreen == "library" &&
                    !liveScanOpen
                ) {

                    /*
                     * Library → Home
                     */
                    BackHandler {

                        currentScreen =
                            "scan"
                    }
                }


                /*
                 * ====================================================
                 * APP SURFACE
                 * ====================================================
                 */
                Surface(
                    modifier =
                        Modifier.fillMaxSize(),

                    color =
                        MaterialTheme
                            .colorScheme
                            .background
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                    ) {

                        /* Keep the underlying screen composed while paper
                         * detail is open so scan/reference results survive Back. */
                        when {
                            /*
                             * ========================================
                             * LIVE SCAN
                             * ========================================
                             */
                            liveScanOpen -> {

                                LiveScanScreen(
                                    libraryRepository = libraryRepository,

                                    detailOpen = selectedPaper != null,

                                    onBack = {

                                        liveScanOpen =
                                            false
                                    },

                                    onPaperClick = { paper ->

                                        selectedPaper =
                                            paper
                                    }
                                )
                            }


                            /*
                             * ========================================
                             * PROJECT
                             * ========================================
                             *
                             * selectedProject stays populated while a
                             * paper from this project is open.
                             *
                             * Therefore:
                             *
                             * Project
                             * → Paper Detail
                             * → Back
                             * → Project
                             */
                            selectedProject != null -> {

                                ProjectScreen(
                                    project =
                                        selectedProject!!,

                                    libraryRepository =
                                        libraryRepository,

                                    onPaperClick = { paper ->

                                        selectedPaper =
                                            paper
                                    },

                                    onBack = {

                                        selectedProject =
                                            null
                                    }
                                )
                            }


                            /*
                             * ========================================
                             * HOME / SCAN SCREEN
                             * ========================================
                             */
                            currentScreen == "scan" -> {

                                ScanScreen(
                                    libraryRepository =
                                        libraryRepository,

                                    onPaperClick = { paper ->

                                        /*
                                         * Make sure a stale project
                                         * cannot affect Back behavior
                                         * when a paper is opened from
                                         * Home.
                                         */
                                        selectedProject =
                                            null


                                        selectedPaper =
                                            paper
                                    },

                                    onLiveScanClick = { subject ->

                                        /*
                                         * Live Scan is not associated
                                         * with a project.
                                         */
                                        selectedProject =
                                            null


                                        liveScanSubjectName =
                                            subject.name


                                        liveScanOpen =
                                            true
                                    },

                                    onLibraryClick = {

                                        /*
                                         * Enter the Library root.
                                         */
                                        selectedProject =
                                            null


                                        currentScreen =
                                            "library"
                                    }
                                )
                            }


                            /*
                             * ========================================
                             * LIBRARY
                             * ========================================
                             */
                            currentScreen == "library" -> {

                                LibraryScreen(
                                    libraryRepository =
                                        libraryRepository,

                                    onPaperClick = { paper ->

                                        /*
                                         * Paper opened from All Papers.
                                         *
                                         * Make sure Back returns to the
                                         * Library root rather than an
                                         * old project.
                                         */
                                        selectedProject =
                                            null


                                        selectedPaper =
                                            paper
                                    },

                                    onBack = {

                                        selectedProject =
                                            null


                                        currentScreen =
                                            "scan"
                                    },

                                    onProjectClick = { project ->

                                        selectedProject =
                                            project
                                    }
                                )
                            }
                        }

                        selectedPaper?.let { paper ->
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                PaperDetailScreen(
                                    paper = paper,
                                    libraryRepository = libraryRepository,
                                    onBack = closePaperDetail
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
