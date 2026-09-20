package com.example.papereyes.ui.library

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.papereyes.data.local.LibraryRepository
import com.example.papereyes.data.local.ProjectEntity
import com.example.papereyes.data.model.Paper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


private enum class LibrarySection {

    ALL_PAPERS,

    PROJECTS
}


@Composable
fun LibraryScreen(
    libraryRepository: LibraryRepository,
    onPaperClick: (Paper) -> Unit,
    onBack: () -> Unit,

    /*
     * We will wire this into a real Project screen in the next
     * milestone.
     *
     * Giving it a default value means MainActivity does not need
     * to change yet.
     */
    onProjectClick: (ProjectEntity) -> Unit = {}
) {

    val context =
        LocalContext.current


    val papers by
    libraryRepository.savedPapers.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )


    val projects by
    libraryRepository.projects.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )


    val coroutineScope =
        rememberCoroutineScope()


    var selectedSection by
    remember {

        mutableStateOf(
            LibrarySection.ALL_PAPERS
        )
    }


    var showCreateProjectDialog by
    remember {

        mutableStateOf(
            false
        )
    }


    /*
     * ================================================================
     * CREATE PROJECT DIALOG
     * ================================================================
     */
    if (
        showCreateProjectDialog
    ) {

        CreateProjectDialog(
            onDismiss = {

                showCreateProjectDialog =
                    false
            },

            onCreate = { projectName ->

                coroutineScope.launch {
                    try {
                        val projectId =
                            libraryRepository.createProject(
                                projectName
                            )

                        if (projectId != null) {
                            showCreateProjectDialog =
                                false
                        } else {
                            Toast.makeText(
                                context,
                                "Project name is empty or already in use",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Throwable) {
                        Toast.makeText(
                            context,
                            "Couldn't create project",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                horizontal = 20.dp
            )
    ) {

        /*
         * ============================================================
         * HEADER
         * ============================================================
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 12.dp,
                    bottom = 14.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * --------------------------------------------------------
             * BACK BUTTON
             * --------------------------------------------------------
             */
            Surface(
                modifier = Modifier
                    .size(
                        48.dp
                    )
                    .clickable {

                        onBack()
                    },

                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text =
                            "←",

                        fontSize =
                            24.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            Spacer(
                modifier = Modifier.size(
                    14.dp
                )
            )


            /*
             * --------------------------------------------------------
             * TITLE + COUNT
             * --------------------------------------------------------
             */
            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        "Library",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )


                when (
                    selectedSection
                ) {

                    LibrarySection.ALL_PAPERS -> {

                        if (
                            papers.isNotEmpty()
                        ) {

                            Text(
                                text =
                                    if (
                                        papers.size == 1
                                    ) {

                                        "1 saved paper"

                                    } else {

                                        "${papers.size} saved papers"
                                    },

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
                    }


                    LibrarySection.PROJECTS -> {

                        if (
                            projects.isNotEmpty()
                        ) {

                            Text(
                                text =
                                    if (
                                        projects.size == 1
                                    ) {

                                        "1 project"

                                    } else {

                                        "${projects.size} projects"
                                    },

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
                    }
                }
            }


            /*
             * --------------------------------------------------------
             * ADD PROJECT BUTTON
             * --------------------------------------------------------
             *
             * Only show this while viewing Projects.
             */
            if (
                selectedSection ==
                LibrarySection.PROJECTS
            ) {

                Surface(
                    modifier = Modifier
                        .size(
                            48.dp
                        )
                        .clickable {

                            showCreateProjectDialog =
                                true
                        },

                    shape =
                        CircleShape,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.Center
                    ) {

                        Text(
                            text =
                                "+",

                            fontSize =
                                26.sp,

                            fontWeight =
                                FontWeight.Medium,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        )
                    }
                }
            }
        }


        /*
         * ============================================================
         * ALL PAPERS / PROJECTS SELECTOR
         * ============================================================
         */
        LibrarySectionSelector(
            selectedSection =
                selectedSection,

            onSectionSelected = { section ->

                selectedSection =
                    section
            }
        )


        Spacer(
            modifier = Modifier.height(
                18.dp
            )
        )


        /*
         * ============================================================
         * CONTENT
         * ============================================================
         */
        when (
            selectedSection
        ) {

            LibrarySection.ALL_PAPERS -> {

                AllPapersContent(
                    papers =
                        papers,

                    onPaperClick =
                        onPaperClick
                )
            }


            LibrarySection.PROJECTS -> {

                ProjectsContent(
                    projects =
                        projects,

                    onProjectClick =
                        onProjectClick,

                    onCreateProject = {

                        showCreateProjectDialog =
                            true
                    }
                )
            }
        }
    }
}


/*
 * ====================================================================
 * SECTION SELECTOR
 * ====================================================================
 */
@Composable
private fun LibrarySectionSelector(
    selectedSection: LibrarySection,
    onSectionSelected: (LibrarySection) -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                14.dp
            ),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
    ) {

        Row(
            modifier =
                Modifier.padding(
                    4.dp
                )
        ) {

            LibrarySectionButton(
                modifier =
                    Modifier.weight(
                        1f
                    ),

                text =
                    "All Papers",

                selected =
                    selectedSection ==
                            LibrarySection.ALL_PAPERS,

                onClick = {

                    onSectionSelected(
                        LibrarySection.ALL_PAPERS
                    )
                }
            )


            LibrarySectionButton(
                modifier =
                    Modifier.weight(
                        1f
                    ),

                text =
                    "Projects",

                selected =
                    selectedSection ==
                            LibrarySection.PROJECTS,

                onClick = {

                    onSectionSelected(
                        LibrarySection.PROJECTS
                    )
                }
            )
        }
    }
}


/*
 * ====================================================================
 * SECTION BUTTON
 * ====================================================================
 */
@Composable
private fun LibrarySectionButton(
    modifier: Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .clickable {

                onClick()
            },

        shape =
            RoundedCornerShape(
                10.dp
            ),

        color =
            if (
                selected
            ) {

                MaterialTheme
                    .colorScheme
                    .primary

            } else {

                MaterialTheme
                    .colorScheme
                    .surfaceVariant
            }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    vertical = 10.dp
                ),

            horizontalArrangement =
                Arrangement.Center,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    text,

                style =
                    MaterialTheme
                        .typography
                        .labelLarge,

                fontWeight =
                    FontWeight.Medium,

                color =
                    if (
                        selected
                    ) {

                        MaterialTheme
                            .colorScheme
                            .onPrimary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            )
        }
    }
}


/*
 * ====================================================================
 * ALL PAPERS
 * ====================================================================
 */
@Composable
private fun AllPapersContent(
    papers: List<Paper>,
    onPaperClick: (Paper) -> Unit
) {

    if (
        papers.isEmpty()
    ) {

        EmptyPapersContent()

        return
    }


    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        items(
            items =
                papers,

            key = { paper ->

                paper.id
            }
        ) { paper ->

            LibraryPaperCard(
                paper =
                    paper,

                onClick = {

                    onPaperClick(
                        paper
                    )
                }
            )
        }


        item {

            Spacer(
                modifier = Modifier.height(
                    20.dp
                )
            )
        }
    }
}


/*
 * ====================================================================
 * EMPTY PAPER LIBRARY
 * ====================================================================
 */
@Composable
private fun EmptyPapersContent() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 40.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                "No saved papers yet",

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            fontWeight =
                FontWeight.Medium
        )


        Spacer(
            modifier = Modifier.height(
                6.dp
            )
        )


        Text(
            text =
                "Papers you save will appear here.",

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
}


/*
 * ====================================================================
 * PROJECTS
 * ====================================================================
 */
@Composable
private fun ProjectsContent(
    projects: List<ProjectEntity>,
    onProjectClick: (ProjectEntity) -> Unit,
    onCreateProject: () -> Unit
) {

    if (
        projects.isEmpty()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 40.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "No projects yet",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Medium
            )


            Spacer(
                modifier = Modifier.height(
                    6.dp
                )
            )


            Text(
                text =
                    "Create a project to organize related papers.",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            Spacer(
                modifier = Modifier.height(
                    18.dp
                )
            )


            Surface(
                modifier =
                    Modifier.clickable {

                        onCreateProject()
                    },

                shape =
                    RoundedCornerShape(
                        12.dp
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            ) {

                Text(
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 11.dp
                        ),

                    text =
                        "Create project",

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onPrimary
                )
            }
        }


        return
    }


    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        items(
            items =
                projects,

            key = { project ->

                project.id
            }
        ) { project ->

            ProjectCard(
                project =
                    project,

                onClick = {

                    onProjectClick(
                        project
                    )
                }
            )
        }


        item {

            Spacer(
                modifier = Modifier.height(
                    20.dp
                )
            )
        }
    }
}


/*
 * ====================================================================
 * PROJECT CARD
 * ====================================================================
 */
@Composable
private fun ProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                onClick()
            },

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    1.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 18.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        project.name,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                /*
                 * We will replace this with an actual paper count
                 * when the Project screen/query is added.
                 */
                Text(
                    text =
                        "Project",

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


            Text(
                text =
                    "›",

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


/*
 * ====================================================================
 * CREATE PROJECT DIALOG
 * ====================================================================
 */
@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {

    var projectName by
    remember {

        mutableStateOf(
            ""
        )
    }


    AlertDialog(
        onDismissRequest = {

            onDismiss()
        },

        title = {

            Text(
                text =
                    "New project"
            )
        },

        text = {

            Column {

                Text(
                    text =
                        "Create a project to group related papers.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                OutlinedTextField(
                    modifier =
                        Modifier.fillMaxWidth(),

                    value =
                        projectName,

                    onValueChange = {

                        projectName =
                            it
                    },

                    label = {

                        Text(
                            text =
                                "Project name"
                        )
                    },

                    singleLine =
                        true
                )
            }
        },

        confirmButton = {

            TextButton(
                enabled =
                    projectName
                        .trim()
                        .isNotEmpty(),

                onClick = {

                    val cleanedName =
                        projectName.trim()


                    if (
                        cleanedName.isNotBlank()
                    ) {

                        onCreate(
                            cleanedName
                        )
                    }
                }
            ) {

                Text(
                    text =
                        "Create"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = {

                    onDismiss()
                }
            ) {

                Text(
                    text =
                        "Cancel"
                )
            }
        }
    )
}


/*
 * ====================================================================
 * PAPER CARD
 * ====================================================================
 *
 * Keep the paper card visual structure that is already working well.
 */
@Composable
private fun LibraryPaperCard(
    paper: Paper,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                onClick()
            },

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 16.dp
                )
        ) {

            /*
             * --------------------------------------------------------
             * TITLE
             * --------------------------------------------------------
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.Top
            ) {

                Text(
                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    text =
                        paper.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,

                    maxLines =
                        3,

                    overflow =
                        TextOverflow.Ellipsis
                )


                Spacer(
                    modifier =
                        Modifier.size(
                            12.dp
                        )
                )


                Text(
                    text =
                        "›",

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


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
                            .onSurfaceVariant,

                    maxLines =
                        2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }


            /*
             * --------------------------------------------------------
             * METADATA
             * --------------------------------------------------------
             */
            val metadataParts =
                buildList {

                    paper.year?.let { year ->

                        add(
                            year.toString()
                        )
                    }


                    paper.doi
                        ?.takeIf {

                            it.isNotBlank()
                        }
                        ?.let { doi ->

                            add(
                                doi
                            )
                        }
                }


            if (
                metadataParts.isNotEmpty()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                Text(
                    text =
                        metadataParts.joinToString(
                            separator = "  •  "
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}