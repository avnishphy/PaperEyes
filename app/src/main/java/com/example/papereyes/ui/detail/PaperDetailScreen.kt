package com.example.papereyes.ui.detail

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.papereyes.util.ExternalLinkOpener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


@Composable
fun PaperDetailScreen(
    paper: Paper,
    libraryRepository: LibraryRepository,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current


    val coroutineScope =
        rememberCoroutineScope()


    val projects by
    libraryRepository.projects.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )


    var isSaved by
    remember(
        paper
    ) {

        mutableStateOf(
            false
        )
    }


    var stateLoaded by
    remember(
        paper
    ) {

        mutableStateOf(
            false
        )
    }


    /*
     * Actual saved project memberships.
     */
    var selectedProjectIds by
    remember(
        paper
    ) {

        mutableStateOf<Set<Int>>(
            emptySet()
        )
    }


    /*
     * Temporary checkbox state while the dialog is open.
     */
    var draftProjectIds by
    remember(
        paper
    ) {

        mutableStateOf<Set<Int>>(
            emptySet()
        )
    }


    var showProjectDialog by
    remember {

        mutableStateOf(
            false
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
     * LOAD SAVED STATE
     * ================================================================
     */
    LaunchedEffect(
        paper
    ) {
        try {
            isSaved =
                libraryRepository
                    .isPaperSaved(
                        paper
                    )

            selectedProjectIds =
                libraryRepository
                    .getProjectIdsForPaper(
                        paper
                    )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            Toast.makeText(
                context,
                "Couldn't load library state",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            stateLoaded =
                true
        }
    }


    /*
     * ================================================================
     * PROJECT SELECTION DIALOG
     * ================================================================
     */
    if (
        showProjectDialog
    ) {

        ProjectSelectionDialog(
            projects =
                projects,

            selectedProjectIds =
                draftProjectIds,

            onSelectionChanged = { projectId, selected ->

                draftProjectIds =
                    if (
                        selected
                    ) {

                        draftProjectIds +
                                projectId

                    } else {

                        draftProjectIds -
                                projectId
                    }
            },

            onDismiss = {

                showProjectDialog =
                    false
            },

            onNewProject = {

                showProjectDialog =
                    false

                showCreateProjectDialog =
                    true
            },

            onSave = {

                coroutineScope.launch {
                    try {
                        libraryRepository
                            .setProjectsForPaper(
                                paper =
                                    paper,

                                projectIds =
                                    draftProjectIds
                            )

                        selectedProjectIds =
                            draftProjectIds

                        /*
                         * Assigning to projects automatically saves
                         * an unsaved paper.
                         */
                        isSaved =
                            true

                        Toast.makeText(
                            context,
                            when {
                                selectedProjectIds.isEmpty() ->
                                    "Project assignments updated"
                                selectedProjectIds.size == 1 ->
                                    "Added to 1 project"
                                else ->
                                    "Added to ${selectedProjectIds.size} projects"
                            },
                            Toast.LENGTH_SHORT
                        ).show()

                        showProjectDialog =
                            false
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Throwable) {
                        Toast.makeText(
                            context,
                            "Couldn't update project assignments",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }


    if (
        showCreateProjectDialog
    ) {

        CreateProjectDialog(
            onDismiss = {

                showCreateProjectDialog =
                    false

                showProjectDialog =
                    true
            },

            onCreate = { projectName ->

                coroutineScope.launch {
                    try {
                        val projectId =
                            libraryRepository.createProject(
                                projectName
                            )

                        if (projectId != null) {
                            draftProjectIds =
                                draftProjectIds + projectId

                            showCreateProjectDialog =
                                false

                            showProjectDialog =
                                true
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
                    bottom = 20.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

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
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )


            Text(
                text =
                    "Paper",

                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,

                fontWeight =
                    FontWeight.SemiBold
            )
        }


        /*
         * ============================================================
         * PAPER METADATA
         * ============================================================
         */
        Text(
            text =
                paper.title,

            style =
                MaterialTheme
                    .typography
                    .headlineSmall,

            fontWeight =
                FontWeight.SemiBold
        )


        if (
            paper.authors.isNotBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    paper.authors,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }


        val metadata =
            buildList {

                paper.year?.let {

                    add(
                        it.toString()
                    )
                }


                paper.doi
                    ?.takeIf {

                        it.isNotBlank()
                    }
                    ?.let {

                        add(
                            it
                        )
                    }
            }


        if (
            metadata.isNotEmpty()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            Text(
                text =
                    metadata.joinToString(
                        separator = "  •  "
                    ),

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
         * ============================================================
         * PROJECTS
         * ============================================================
         */
        Spacer(
            modifier =
                Modifier.height(
                    28.dp
                )
        )


        Text(
            text =
                "Projects",

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
                    8.dp
                )
        )


        val assignedProjects =
            projects.filter { project ->

                project.id in
                        selectedProjectIds
            }


        if (
            assignedProjects.isEmpty()
        ) {

            Text(
                text =
                    "Not assigned to any project.",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

        } else {

            assignedProjects
                .forEach { project ->

                    ProjectAssignmentRow(
                        project =
                            project
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )
                }
        }


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),

                onClick = {

                    draftProjectIds =
                        selectedProjectIds


                    showProjectDialog =
                        true
                }
            ) {

                Text(
                    text =
                        if (
                            selectedProjectIds.isEmpty()
                        ) {

                            "Add to Projects"

                        } else {

                            "Manage Projects"
                        }
                )
            }


        /*
         * ============================================================
         * LIBRARY ACTION
         * ============================================================
         */
        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )


        if (
            stateLoaded
        ) {

            if (
                isSaved
            ) {

                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        coroutineScope.launch {
                            try {
                                val removed =
                                    libraryRepository
                                        .removePaper(
                                            paper
                                        )

                                if (removed) {
                                    isSaved =
                                        false

                                    /*
                                     * CASCADE removed all project
                                     * memberships as well.
                                     */
                                    selectedProjectIds =
                                        emptySet()

                                    Toast.makeText(
                                        context,
                                        "Removed from library",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Throwable) {
                                Toast.makeText(
                                    context,
                                    "Couldn't remove paper",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {

                    Text(
                        text =
                            "Remove from Library"
                    )
                }

            } else {

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        coroutineScope.launch {
                            try {
                                val saved =
                                    libraryRepository
                                        .savePaper(
                                            paper
                                        )

                                isSaved =
                                    true

                                Toast.makeText(
                                    context,
                                    if (saved) {
                                        "Paper saved"
                                    } else {
                                        "Already in library"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Throwable) {
                                Toast.makeText(
                                    context,
                                    "Couldn't save paper",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {

                    Text(
                        text =
                            "Save to Library"
                    )
                }
            }
        }


        /*
         * ============================================================
         * OPEN PAPER
         * ============================================================
         */
        paper.url
            ?.takeIf { url ->

                ExternalLinkOpener
                    .isSupportedWebUrl(
                        url
                    )
            }
            ?.let { url ->

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        val opened =
                            ExternalLinkOpener
                                .open(
                                    context = context,
                                    rawUrl = url
                                )


                        if (
                            !opened
                        ) {

                            Toast.makeText(
                                context,
                                "No browser is available to open this link.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {

                    Text(
                        text =
                            "Open Paper Link"
                    )
                }
            }
    }
}


/*
 * ====================================================================
 * ASSIGNED PROJECT ROW
 * ====================================================================
 */
@Composable
private fun ProjectAssignmentRow(
    project: ProjectEntity
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                10.dp
            ),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
    ) {

        Text(
            modifier =
                Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 9.dp
                ),

            text =
                project.name,

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            fontWeight =
                FontWeight.Medium
        )
    }
}


/*
 * ====================================================================
 * PROJECT MULTI-SELECT DIALOG
 * ====================================================================
 */
@Composable
private fun ProjectSelectionDialog(
    projects: List<ProjectEntity>,
    selectedProjectIds: Set<Int>,
    onSelectionChanged: (
        projectId: Int,
        selected: Boolean
    ) -> Unit,
    onNewProject: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {

    AlertDialog(
        onDismissRequest = {

            onDismiss()
        },

        title = {

            Text(
                text =
                    "Add to projects"
            )
        },

        text = {

            if (
                projects.isEmpty()
            ) {

                Text(
                    text =
                        "No projects have been created yet."
                )

            } else {

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max = 320.dp
                            )
                ) {

                    items(
                        items =
                            projects,

                        key = { project ->

                            project.id
                        }
                    ) { project ->

                        val checked =
                            project.id in
                                    selectedProjectIds


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {

                                    onSelectionChanged(
                                        project.id,
                                        !checked
                                    )
                                }
                                .padding(
                                    vertical = 4.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked =
                                    checked,

                                onCheckedChange = { selected ->

                                    onSelectionChanged(
                                        project.id,
                                        selected
                                    )
                                }
                            )


                            Spacer(
                                modifier =
                                    Modifier.size(
                                        8.dp
                                    )
                            )


                            Text(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                text =
                                    project.name,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyLarge,

                                maxLines =
                                    2,

                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = {

                    onSave()
                }
            ) {

                Text(
                    text =
                        "Save"
                )
            }
        },

        dismissButton = {
            Row {
                TextButton(
                    onClick = onNewProject
                ) {
                    Text(
                        text =
                            "New project"
                    )
                }

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
        }
    )
}


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
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    "New project"
            )
        },
        text = {
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
        },
        confirmButton = {
            TextButton(
                enabled =
                    projectName.isNotBlank(),
                onClick = {
                    onCreate(
                        projectName
                    )
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
                onClick = onDismiss
            ) {
                Text(
                    text =
                        "Cancel"
                )
            }
        }
    )
}
