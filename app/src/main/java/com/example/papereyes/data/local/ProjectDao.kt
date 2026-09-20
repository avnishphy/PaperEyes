package com.example.papereyes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


@Dao
interface ProjectDao {

    /*
     * ================================================================
     * PROJECTS
     * ================================================================
     */

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertProject(
        project: ProjectEntity
    ): Long


    @Query(
        """
        SELECT * FROM projects
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun getAllProjects(): Flow<List<ProjectEntity>>


    @Query(
        """
        SELECT * FROM projects
        WHERE id = :projectId
        LIMIT 1
        """
    )
    suspend fun findProjectById(
        projectId: Int
    ): ProjectEntity?


    @Query(
        """
        SELECT id
        FROM projects
        WHERE id IN (:projectIds)
        """
    )
    suspend fun findExistingProjectIds(
        projectIds: List<Int>
    ): List<Int>


    @Query(
        """
        UPDATE OR IGNORE projects
        SET name = :name
        WHERE id = :projectId
        """
    )
    suspend fun renameProject(
        projectId: Int,
        name: String
    ): Int


    @Query(
        """
        DELETE FROM projects
        WHERE id = :projectId
        """
    )
    suspend fun deleteProject(
        projectId: Int
    ): Int


    /*
     * ================================================================
     * PAPER ↔ PROJECT MEMBERSHIP
     * ================================================================
     */

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun addPaperToProject(
        crossRef: PaperProjectCrossRef
    ): Long


    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun addPaperToProjects(
        crossRefs: List<PaperProjectCrossRef>
    ): List<Long>


    @Query(
        """
        SELECT id
        FROM papers
        WHERE id IN (:paperIds)
        """
    )
    suspend fun findExistingPaperIds(
        paperIds: List<Int>
    ): List<Int>


    /*
     * Add several already-saved papers to one project atomically.
     *
     * Compared with calling addPaperToProject() from the UI once per paper,
     * this performs one project lookup, one paper-ID validation query, and
     * one batched insert inside a single Room transaction.
     *
     * Stale paper IDs are ignored, duplicate memberships are ignored by the
     * composite primary key, and the returned Int is the number of NEW
     * memberships created.
     */
    @Transaction
    suspend fun addPaperIdsToProject(
        projectId: Int,
        paperIds: List<Int>
    ): Int {

        if (
            paperIds.isEmpty()
        ) {

            return 0
        }


        findProjectById(
            projectId
        )
            ?: return 0


        val existingPaperIds =
            findExistingPaperIds(
                paperIds.distinct()
            )


        if (
            existingPaperIds.isEmpty()
        ) {

            return 0
        }


        val results =
            addPaperToProjects(
                existingPaperIds.map { paperId ->

                    PaperProjectCrossRef(
                        paperId = paperId,
                        projectId = projectId
                    )
                }
            )


        return results.count { rowId ->
            rowId != -1L
        }
    }


    @Query(
        """
        DELETE FROM paper_project_cross_ref
        WHERE paperId = :paperId
        AND projectId = :projectId
        """
    )
    suspend fun removePaperFromProject(
        paperId: Int,
        projectId: Int
    ): Int


    @Query(
        """
        DELETE FROM paper_project_cross_ref
        WHERE paperId = :paperId
        """
    )
    suspend fun removeAllProjectsFromPaper(
        paperId: Int
    )


    @Query(
        """
        SELECT projectId
        FROM paper_project_cross_ref
        WHERE paperId = :paperId
        """
    )
    suspend fun getProjectIdsForPaper(
        paperId: Int
    ): List<Int>


    /*
     * Replace all project memberships for one paper atomically.
     */
    @Transaction
    suspend fun replaceProjectsForPaper(
        paperId: Int,
        projectIds: List<Int>
    ) {

        removeAllProjectsFromPaper(
            paperId
        )


        if (
            projectIds.isNotEmpty()
        ) {

            addPaperToProjects(
                projectIds.map { projectId ->

                    PaperProjectCrossRef(
                        paperId = paperId,
                        projectId = projectId
                    )
                }
            )
        }
    }


    /*
     * ================================================================
     * PROJECT → PAPERS
     * ================================================================
     */

    @Transaction
    @Query(
        """
        SELECT * FROM projects
        WHERE id = :projectId
        LIMIT 1
        """
    )
    fun observeProjectWithPapers(
        projectId: Int
    ): Flow<ProjectWithPapers?>


    /*
     * ================================================================
     * PAPER → PROJECTS
     * ================================================================
     */

    @Transaction
    @Query(
        """
        SELECT * FROM papers
        WHERE id = :paperId
        LIMIT 1
        """
    )
    fun observePaperWithProjects(
        paperId: Int
    ): Flow<PaperWithProjects?>
}