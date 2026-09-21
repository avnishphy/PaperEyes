package com.example.papereyes.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.papereyes.data.model.Paper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaperDatabaseTest {
    private lateinit var database: PaperDatabase
    private lateinit var paperDao: PaperDao
    private lateinit var projectDao: ProjectDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PaperDatabase::class.java
        ).allowMainThreadQueries().build()
        paperDao = database.paperDao()
        projectDao = database.projectDao()
    }

    @After fun tearDown() = database.close()

    @Test
    fun canonicalTitleUpgradePreservesProjectMembership() = runBlocking {
        val first = paperDao.insertOrGetPaper(paper("Generalized  Parton Distributions", 2023, null))
        val repository = LibraryRepository(database)
        val projectId = repository.createProject("Identity regression")!!
        repository.addPaperToProject(first.paper, projectId)
        val upgraded = paperDao.insertOrGetPaper(paper("Generalized Parton Distributions", 2023, "10.1000/upgrade-space"))
        assertFalse(upgraded.inserted)
        assertEquals(first.paper.id, upgraded.paper.id)
        assertEquals(setOf(projectId), repository.getProjectIdsForPaper(upgraded.paper))
        assertEquals(1, paperDao.getAllPapers().first().size)
    }

    @Test
    fun concurrentDuplicateSavesRemainOneRow() = runBlocking {
        val results = (1..20).map {
            async(Dispatchers.IO) { paperDao.insertOrGetPaper(paper("Concurrent", 2025, "10.1000/concurrent")) }
        }.awaitAll()
        assertEquals(1, results.map { it.paper.id }.distinct().size)
        assertEquals(1, paperDao.getAllPapers().first().size)
    }

    @Test
    fun doiUrlAndBareDoiCollapseToOnePaper() = runBlocking {
        val first = paperDao.insertOrGetPaper(paper("Same", 2025, "10.1000/ABC"))
        val second = paperDao.insertOrGetPaper(paper("Same", 2025, "https://doi.org/10.1000/abc"))

        assertTrue(first.inserted)
        assertFalse(second.inserted)
        assertEquals(first.paper.id, second.paper.id)
        assertEquals(1, paperDao.getAllPapers().first().size)
    }

    @Test
    fun sameTitleDifferentKnownYearsRemainDistinct() = runBlocking {
        val first = paperDao.insertOrGetPaper(paper("Shared Title", 2023, null))
        val second = paperDao.insertOrGetPaper(paper("Shared Title", 2024, null))

        assertTrue(first.inserted)
        assertTrue(second.inserted)
        assertEquals(2, paperDao.getAllPapers().first().size)
    }

    @Test
    fun incompleteTitleOnlyRowUpgradesWhenDoiArrives() = runBlocking {
        val first = paperDao.insertOrGetPaper(paper("Upgrade Me", null, null))
        val second = paperDao.insertOrGetPaper(paper("Upgrade Me", 2025, "10.1000/upgrade"))

        assertEquals(first.paper.id, second.paper.id)
        assertFalse(second.inserted)
        assertEquals("10.1000/upgrade", second.paper.doi)
        assertEquals(2025, second.paper.year)
        assertEquals(1, paperDao.getAllPapers().first().size)
    }

    @Test
    fun differentExplicitDoisNeverMergeOnTitle() = runBlocking {
        val first = paperDao.insertOrGetPaper(paper("Same Title", 2025, "10.1000/one"))
        val second = paperDao.insertOrGetPaper(paper("Same Title", 2025, "10.1000/two"))

        assertTrue(first.inserted)
        assertTrue(second.inserted)
        assertEquals(2, paperDao.getAllPapers().first().size)
    }

    @Test
    fun projectAssignmentFiltersStaleIdsAndCascadeRemovesMembership() = runBlocking {
        val repository = LibraryRepository(database)
        val projectId = repository.createProject("GPD")!!
        val candidate = paper("Project Paper", 2025, "10.1000/project")

        repository.setProjectsForPaper(candidate, setOf(projectId, 999_999))
        val saved = repository.getSavedPaper(candidate)!!
        assertEquals(setOf(projectId), repository.getProjectIdsForPaper(saved))

        repository.removePaper(saved)
        assertTrue(projectDao.getProjectIdsForPaper(saved.id).isEmpty())
    }


    @Test
    fun ambiguousTitleWithoutDoiDoesNotArbitrarilyMergeExplicitDoiRows() = runBlocking {
        paperDao.insertOrGetPaper(paper("Ambiguous", 2025, "10.1000/one"))
        paperDao.insertOrGetPaper(paper("Ambiguous", 2025, "10.1000/two"))

        val result = paperDao.insertOrGetPaper(paper("Ambiguous", 2025, null))

        assertTrue(result.inserted)
        assertEquals(3, paperDao.getAllPapers().first().size)
    }

    @Test
    fun projectNamesAreCaseInsensitiveUnique() = runBlocking {
        val repository = LibraryRepository(database)

        assertTrue(repository.createProject("GPD") != null)
        assertEquals(null, repository.createProject("gpd"))
        assertEquals(1, projectDao.getAllProjects().first().size)
    }

    @Test
    fun deletingProjectCascadesMembershipWithoutDeletingPaper() = runBlocking {
        val repository = LibraryRepository(database)
        val projectId = repository.createProject("Temporary")!!
        val candidate = paper("Keep Paper", 2025, "10.1000/keep")

        repository.setProjectsForPaper(candidate, setOf(projectId))
        val saved = repository.getSavedPaper(candidate)!!
        assertEquals(setOf(projectId), repository.getProjectIdsForPaper(saved))

        assertTrue(repository.deleteProject(projectId))
        assertTrue(repository.getProjectIdsForPaper(saved).isEmpty())
        assertTrue(repository.isPaperSaved(saved))
    }

    private fun paper(title: String, year: Int?, doi: String?) = Paper(
        title = title,
        authors = "A. Author",
        year = year,
        doi = doi,
        url = doi?.let { "https://doi.org/$it" }
    )
}
