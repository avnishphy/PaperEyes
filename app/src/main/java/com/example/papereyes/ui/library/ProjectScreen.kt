package com.example.papereyes.ui.library

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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


@Composable
fun ProjectScreen(
    project: ProjectEntity,
    libraryRepository: LibraryRepository,
    onPaperClick: (Paper) -> Unit,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current


    val coroutineScope =
        rememberCoroutineScope()


    val projectWithPapers by
    libraryRepository
        .observeProjectWithPapers(
            project.id
        )
        .collectAsStateWithLifecycle(
            initialValue = null
        )


    val allLibraryPapers by
    libraryRepository
        .savedPapers
        .collectAsStateWithLifecycle(
            initialValue = emptyList()
        )


    val projectPapers =
        projectWithPapers
            ?.papers
            .orEmpty()


    /*
     * ================================================================
     * ADD-PAPERS SCREEN STATE
     * ================================================================
     */
    var showAddPapersScreen by
    remember(
        project.id
    ) {

        mutableStateOf(
            false
        )
    }


    /*
     * Only stores NEW papers selected during this add operation.
     *
     * Papers already in the project are shown checked, but are not
     * placed in this set.
     */
    var selectedNewPaperIds by
    remember(
        project.id
    ) {

        mutableStateOf<Set<Int>>(
            emptySet()
        )
    }


    /*
     * When the full-screen picker is open, Android system Back should
     * return to the project instead of leaving the project entirely.
     */
    BackHandler(
        enabled =
            showAddPapersScreen
    ) {

        selectedNewPaperIds =
            emptySet()


        showAddPapersScreen =
            false
    }


    /*
     * ================================================================
     * FULL-SCREEN LIBRARY PICKER
     * ================================================================
     */
    if (
        showAddPapersScreen
    ) {

        AddPapersFromLibraryScreen(
            project =
                project,

            allLibraryPapers =
                allLibraryPapers,

            existingProjectPapers =
                projectPapers,

            selectedNewPaperIds =
                selectedNewPaperIds,

            onPaperSelectionChanged = { paperId, selected ->

                selectedNewPaperIds =
                    if (
                        selected
                    ) {

                        selectedNewPaperIds +
                                paperId

                    } else {

                        selectedNewPaperIds -
                                paperId
                    }
            },

            onBack = {

                selectedNewPaperIds =
                    emptySet()


                showAddPapersScreen =
                    false
            },

            onAddSelected = {

                coroutineScope.launch {
                    try {
                        val addedCount =
                            libraryRepository
                                .addSavedPapersToProject(
                                    projectId =
                                        project.id,

                                    paperIds =
                                        selectedNewPaperIds
                                )

                        Toast.makeText(
                            context,
                            when (addedCount) {
                                0 -> "No papers added"
                                1 -> "1 paper added"
                                else -> "$addedCount papers added"
                            },
                            Toast.LENGTH_SHORT
                        ).show()

                        selectedNewPaperIds =
                            emptySet()

                        showAddPapersScreen =
                            false
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Throwable) {
                        Toast.makeText(
                            context,
                            "Couldn't add papers to project",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )


        return
    }


    /*
     * ================================================================
     * NORMAL PROJECT SCREEN
     * ================================================================
     */
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
                    bottom = 18.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * Back
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
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )


            /*
             * Project title
             */
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
                            .headlineMedium,

                    fontWeight =
                        FontWeight.SemiBold,

                    maxLines =
                        2,

                    overflow =
                        TextOverflow.Ellipsis
                )


                Text(
                    text =
                        when (
                            projectPapers.size
                        ) {

                            0 ->
                                "No papers"

                            1 ->
                                "1 paper"

                            else ->
                                "${projectPapers.size} papers"
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


            /*
             * Add-papers shortcut.
             */
            Surface(
                modifier = Modifier
                    .size(
                        48.dp
                    )
                    .clickable(
                        enabled =
                            allLibraryPapers.isNotEmpty()
                    ) {

                        selectedNewPaperIds =
                            emptySet()


                        showAddPapersScreen =
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


        /*
         * ============================================================
         * EMPTY PROJECT
         * ============================================================
         */
        if (
            projectPapers.isEmpty()
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
                        "No papers in this project yet",

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Medium
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        if (
                            allLibraryPapers.isEmpty()
                        ) {

                            "Nothing in this state yet. Save a paper first."

                        } else {

                            "Nothing in this state yet. Add papers from your Library."
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


                if (
                    allLibraryPapers.isNotEmpty()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )


                    Button(
                        onClick = {

                            selectedNewPaperIds =
                                emptySet()


                            showAddPapersScreen =
                                true
                        }
                    ) {

                        Text(
                            text =
                                "Add Papers from Library"
                        )
                    }
                }
            }

        } else {

            /*
             * ========================================================
             * PAPERS ALREADY IN THIS PROJECT
             * ========================================================
             */
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
                        projectPapers,

                    key = { paper ->

                        paper.id
                    }
                ) { paper ->

                    ProjectPaperCard(
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
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )
                }
            }
        }
    }
}


/*
 * ====================================================================
 * FULL-SCREEN ADD PAPERS VIEW
 * ====================================================================
 */
@Composable
private fun AddPapersFromLibraryScreen(
    project: ProjectEntity,
    allLibraryPapers: List<Paper>,
    existingProjectPapers: List<Paper>,
    selectedNewPaperIds: Set<Int>,
    onPaperSelectionChanged: (
        paperId: Int,
        selected: Boolean
    ) -> Unit,
    onBack: () -> Unit,
    onAddSelected: () -> Unit
) {

    val existingPaperIds =
        existingProjectPapers
            .map {

                it.id
            }
            .toSet()


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
                    bottom = 6.dp
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


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        "Add papers",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Text(
                    text =
                        project.name,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

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


        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )


        /*
         * Small explanation.
         */
        Text(
            text =
                when (
                    allLibraryPapers.size
                ) {

                    0 ->
                        "No papers in Library"

                    1 ->
                        "1 paper in Library"

                    else ->
                        "${allLibraryPapers.size} papers in Library"
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


        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )


        /*
         * ============================================================
         * COMPLETE LIBRARY LIST
         * ============================================================
         */
        if (
            allLibraryPapers.isEmpty()
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        ),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text =
                        "Your Library is empty.",

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
            }

        } else {

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                items(
                    items =
                        allLibraryPapers,

                    key = { paper ->

                        paper.id
                    }
                ) { paper ->

                    val alreadyInProject =
                        paper.id in
                                existingPaperIds


                    val newlySelected =
                        paper.id in
                                selectedNewPaperIds


                    LibrarySelectionCard(
                        paper =
                            paper,

                        checked =
                            alreadyInProject ||
                                    newlySelected,

                        alreadyInProject =
                            alreadyInProject,

                        onCheckedChange = { checked ->

                            if (
                                !alreadyInProject
                            ) {

                                onPaperSelectionChanged(
                                    paper.id,
                                    checked
                                )
                            }
                        }
                    )
                }


                item {

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }
            }
        }


        /*
         * ============================================================
         * ADD BUTTON
         * ============================================================
         */
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 12.dp,
                    bottom = 12.dp
                ),

            enabled =
                selectedNewPaperIds.isNotEmpty(),

            onClick = {

                onAddSelected()
            }
        ) {

            Text(
                text =
                    when (
                        selectedNewPaperIds.size
                    ) {

                        0 ->
                            "Add Selected"

                        1 ->
                            "Add 1 Paper"

                        else ->
                            "Add ${selectedNewPaperIds.size} Papers"
                    }
            )
        }
    }
}


/*
 * ====================================================================
 * LIBRARY PAPER SELECTION CARD
 * ====================================================================
 */
@Composable
private fun LibrarySelectionCard(
    paper: Paper,
    checked: Boolean,
    alreadyInProject: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled =
                    !alreadyInProject
            ) {

                onCheckedChange(
                    !checked
                )
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
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),

            verticalAlignment =
                Alignment.Top
        ) {

            Checkbox(
                checked =
                    checked,

                enabled =
                    !alreadyInProject,

                onCheckedChange = { selected ->

                    onCheckedChange(
                        selected
                    )
                }
            )


            Spacer(
                modifier =
                    Modifier.size(
                        10.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        paper.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,

                    fontWeight =
                        FontWeight.SemiBold,

                    maxLines =
                        3,

                    overflow =
                        TextOverflow.Ellipsis
                )


                if (
                    paper.authors.isNotBlank()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )


                    Text(
                        text =
                            paper.authors,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

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


                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                if (
                    alreadyInProject
                ) {

                    Text(
                        text =
                            "Already in project",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        fontWeight =
                            FontWeight.Medium
                    )

                } else {

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

                        Text(
                            text =
                                metadata.joinToString(
                                    "  •  "
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
    }
}


/*
 * ====================================================================
 * PROJECT PAPER CARD
 * ====================================================================
 */
@Composable
private fun ProjectPaperCard(
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
                            12.dp
                        )
                )


                Text(
                    text =
                        metadata.joinToString(
                            "  •  "
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
