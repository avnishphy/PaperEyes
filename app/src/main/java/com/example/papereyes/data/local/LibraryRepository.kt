package com.example.papereyes.data.local

import androidx.room.withTransaction
import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.normalizePaperDoi
import com.example.papereyes.data.model.normalizePaperTitle
import com.example.papereyes.data.model.withCanonicalIdentity
import kotlinx.coroutines.flow.Flow


class LibraryRepository(
    private val database: PaperDatabase
) {

    private val paperDao = database.paperDao()
    private val projectDao = database.projectDao()

    /*
     * ================================================================
     * OBSERVABLE LIBRARY STATE
     * ================================================================
     */

    val savedPapers: Flow<List<Paper>> =
        paperDao.getAllPapers()


    val projects: Flow<List<ProjectEntity>> =
        projectDao.getAllProjects()


    /*
     * ================================================================
     * PAPERS
     * ================================================================
     */

    suspend fun savePaper(
        paper: Paper
    ): Boolean {

        val result =
            paperDao.insertOrGetPaper(
                paper
                    .withCanonicalIdentity()
                    .copy(
                        id = 0,
                        savedAt = System.currentTimeMillis()
                    )
            )


        return result.inserted
    }


    suspend fun isPaperSaved(
        paper: Paper
    ): Boolean {

        return findSavedVersion(
            paper
        ) != null
    }


    suspend fun getSavedPaper(
        paper: Paper
    ): Paper? {

        return findSavedVersion(
            paper
        )
    }


    suspend fun removePaper(
        paper: Paper
    ): Boolean {

        val savedPaper =
            findSavedVersion(
                paper
            )
                ?: return false


        /*
         * Project membership rows are deleted automatically
         * through the foreign-key CASCADE.
         */
        paperDao.deletePaper(
            savedPaper
        )


        return true
    }


    /*
     * ================================================================
     * PROJECTS
     * ================================================================
     */

    suspend fun createProject(
        name: String
    ): Int? {

        val cleanedName =
            name.trim()


        if (
            cleanedName.isBlank()
        ) {

            return null
        }


        val rowId =
            projectDao.insertProject(
                ProjectEntity(
                    name = cleanedName
                )
            )


        if (
            rowId == -1L
        ) {

            return null
        }


        return rowId.toInt()
    }


    suspend fun renameProject(
        projectId: Int,
        newName: String
    ): Boolean {

        val cleanedName =
            newName.trim()


        if (
            cleanedName.isBlank()
        ) {

            return false
        }


        return projectDao.renameProject(
            projectId = projectId,
            name = cleanedName
        ) > 0
    }


    suspend fun deleteProject(
        projectId: Int
    ): Boolean {

        return projectDao.deleteProject(
            projectId
        ) > 0
    }


    /*
     * ================================================================
     * PAPER ↔ PROJECT
     * ================================================================
     */

    suspend fun addPaperToProject(
        paper: Paper,
        projectId: Int
    ): Boolean = database.withTransaction {

        val project = projectDao.findProjectById(projectId)
            ?: return@withTransaction false

        val paperId = getOrSavePaperId(paper)

        projectDao.addPaperToProject(
            PaperProjectCrossRef(
                paperId = paperId,
                projectId = project.id
            )
        ) != -1L
    }


    /*
     * Add several papers that are already present in the Library to one
     * project. This is the fast path used by the project picker.
     *
     * The DAO performs the membership update with one INSERT ... SELECT
     * statement instead of one repository/DAO round trip per paper.
     *
     * Returns the number of NEW memberships created.
     */
    suspend fun addSavedPapersToProject(
        projectId: Int,
        paperIds: Collection<Int>
    ): Int {

        val ids =
            paperIds
                .asSequence()
                .filter { paperId ->
                    paperId > 0
                }
                .distinct()
                .toList()


        if (
            ids.isEmpty()
        ) {

            return 0
        }


        return projectDao.addPaperIdsToProject(
            projectId = projectId,
            paperIds = ids
        )
    }


    suspend fun removePaperFromProject(
        paperId: Int,
        projectId: Int
    ): Boolean {

        return projectDao.removePaperFromProject(
            paperId = paperId,
            projectId = projectId
        ) > 0
    }


    /*
     * Return all project IDs currently containing this paper.
     *
     * Unsaved papers naturally return an empty set.
     */
    suspend fun getProjectIdsForPaper(
        paper: Paper
    ): Set<Int> {

        val savedPaper =
            findSavedVersion(
                paper
            )
                ?: return emptySet()


        return projectDao
            .getProjectIdsForPaper(
                savedPaper.id
            )
            .toSet()
    }


    /*
     * Replace the complete set of projects assigned to a paper.
     *
     * If the paper has not been saved yet, assigning it to a project
     * automatically saves it first.
     */
    suspend fun setProjectsForPaper(
        paper: Paper,
        projectIds: Set<Int>
    ) = database.withTransaction {

        val requestedProjectIds = projectIds
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .toList()

        val validProjectIds =
            if (requestedProjectIds.isEmpty()) {
                emptyList()
            } else {
                projectDao.findExistingProjectIds(requestedProjectIds)
            }

        val paperId = getOrSavePaperId(paper)

        projectDao.replaceProjectsForPaper(
            paperId = paperId,
            projectIds = validProjectIds
        )
    }


    /*
     * ================================================================
     * RELATION QUERIES
     * ================================================================
     */

    fun observeProjectWithPapers(
        projectId: Int
    ): Flow<ProjectWithPapers?> {

        return projectDao.observeProjectWithPapers(
            projectId
        )
    }


    fun observePaperWithProjects(
        paperId: Int
    ): Flow<PaperWithProjects?> {

        return projectDao.observePaperWithProjects(
            paperId
        )
    }


    /*
     * ================================================================
     * INTERNAL HELPERS
     * ================================================================
     */

    private suspend fun getOrSavePaperId(
        paper: Paper
    ): Int {

        val result =
            paperDao.insertOrGetPaper(
                paper
                    .withCanonicalIdentity()
                    .copy(
                        id = 0,
                        savedAt = System.currentTimeMillis()
                    )
            )


        return result.paper.id
    }


    private suspend fun findSavedVersion(
        paper: Paper
    ): Paper? {

        val candidate =
            paper.withCanonicalIdentity()


        paperDao.findByIdentityKey(
            candidate.identityKey
        )
            ?.let {
                return it
            }


        /*
         * Compatibility path for a paper that was previously saved without
         * a DOI and is later resolved with one, or vice versa.
         */
        val candidateDoi = normalizePaperDoi(candidate.doi)

        val compatible = paperDao.findAllByTitle(candidate.title)
            .filter { saved ->
                val savedDoi = normalizePaperDoi(saved.doi)
                val doiCompatible =
                    candidateDoi == null || savedDoi == null || candidateDoi == savedDoi
                val yearCompatible =
                    candidate.year == null || saved.year == null || candidate.year == saved.year

                doiCompatible &&
                        yearCompatible &&
                        normalizePaperTitle(candidate.title) == normalizePaperTitle(saved.title)
            }

        return compatible.singleOrNull()
    }
}
