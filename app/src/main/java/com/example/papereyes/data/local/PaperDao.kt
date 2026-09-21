package com.example.papereyes.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.normalizePaperDoi
import com.example.papereyes.data.model.normalizePaperTitle
import com.example.papereyes.data.model.withCanonicalIdentity
import kotlinx.coroutines.flow.Flow


data class PaperSaveResult(
    val paper: Paper,
    val inserted: Boolean
)

@Dao
abstract class PaperDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertPaperInternal(paper: Paper): Long

    @Update
    protected abstract suspend fun updatePaperInternal(paper: Paper)

    @Delete
    abstract suspend fun deletePaper(paper: Paper)

    @Query("SELECT * FROM papers ORDER BY savedAt DESC")
    abstract fun getAllPapers(): Flow<List<Paper>>

    @Query("SELECT * FROM papers WHERE identityKey = :identityKey LIMIT 1")
    abstract suspend fun findByIdentityKey(identityKey: String): Paper?

    @Query("SELECT * FROM papers ORDER BY savedAt DESC")
    protected abstract suspend fun loadIdentityCandidates(): List<Paper>

    /** SQLite LOWER is ASCII-oriented and TRIM does not collapse inner whitespace.
     * Use the same canonical function as identityKey on this rare upgrade path.
     * Exact identity remains indexed; this fallback is one bounded-by-library query,
     * not N+1 queries. Add an indexed normalized-title column when scale justifies it.
     */
    open suspend fun findAllByTitle(title: String): List<Paper> {
        val normalized = normalizePaperTitle(title)
        return loadIdentityCandidates().filter { normalizePaperTitle(it.title) == normalized }
    }

    @Query("SELECT * FROM papers WHERE id = :paperId LIMIT 1")
    abstract suspend fun findById(paperId: Int): Paper?

    /**
     * Race-safe find-or-insert. The unique identityKey index remains the final
     * database-level guard against concurrent duplicate saves.
     */
    @Transaction
    open suspend fun insertOrGetPaper(paper: Paper): PaperSaveResult {
        val candidate = paper.withCanonicalIdentity().copy(id = 0)

        findByIdentityKey(candidate.identityKey)?.let { existing ->
            return PaperSaveResult(existing, inserted = false)
        }

        val compatibleMatches = findAllByTitle(candidate.title)
            .filter { representsSamePaper(candidate, it) }

        // Never choose arbitrarily among multiple title-only candidates.
        val titleMatch = compatibleMatches.singleOrNull()

        if (titleMatch != null) {
            val candidateDoi = normalizePaperDoi(candidate.doi)
            val existingDoi = normalizePaperDoi(titleMatch.doi)

            val shouldUpgrade =
                (candidateDoi != null && existingDoi == null) ||
                    (titleMatch.year == null && candidate.year != null)

            if (shouldUpgrade) {
                val upgraded = titleMatch.copy(
                    title = candidate.title,
                    authors = candidate.authors.takeIf(String::isNotBlank)
                        ?: titleMatch.authors,
                    year = candidate.year ?: titleMatch.year,
                    doi = candidateDoi ?: titleMatch.doi,
                    url = candidate.url ?: titleMatch.url
                ).withCanonicalIdentity()

                updatePaperInternal(upgraded)
                return PaperSaveResult(upgraded, inserted = false)
            }

            return PaperSaveResult(titleMatch, inserted = false)
        }

        val rowId = insertPaperInternal(candidate)
        return PaperSaveResult(candidate.copy(id = rowId.toInt()), inserted = true)
    }

    private fun representsSamePaper(first: Paper, second: Paper): Boolean {
        val firstDoi = normalizePaperDoi(first.doi)
        val secondDoi = normalizePaperDoi(second.doi)

        if (firstDoi != null && secondDoi != null) {
            return firstDoi == secondDoi
        }

        if (normalizePaperTitle(first.title) != normalizePaperTitle(second.title)) {
            return false
        }

        // If both years are known, a disagreement is positive evidence that
        // identical titles refer to different papers.
        return first.year == null || second.year == null || first.year == second.year
    }
}
