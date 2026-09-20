package com.example.papereyes.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

private val PAPER_TITLE_WHITESPACE_REGEX = Regex("\\s+")

@Entity(
    tableName = "papers",
    indices = [Index(value = ["identityKey"], unique = true)]
)
data class Paper(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val authors: String,
    val year: Int?,
    val doi: String?,
    val url: String?,
    val savedAt: Long = System.currentTimeMillis(),
    /** Stable canonical identity used by the unique database index. */
    @ColumnInfo(defaultValue = "''")
    val identityKey: String = buildPaperIdentityKey(
        doi = doi,
        title = title,
        year = year
    )
)

fun normalizePaperDoi(doi: String?): String? =
    ScholarlyIdentifiers.normalizeDoi(doi)

fun normalizePaperTitle(title: String): String =
    title
        .trim()
        .lowercase(Locale.ROOT)
        .replace(PAPER_TITLE_WHITESPACE_REGEX, " ")

/**
 * DOI is authoritative when available. Without a DOI, year is part of the
 * fallback identity so two genuinely different papers with the same title in
 * different years are not forced into one row.
 */
fun buildPaperIdentityKey(
    doi: String?,
    title: String,
    year: Int? = null
): String {
    val normalizedDoi = normalizePaperDoi(doi)
    return if (normalizedDoi != null) {
        "doi:$normalizedDoi"
    } else {
        "title:${normalizePaperTitle(title)}|year:${year ?: "unknown"}"
    }
}

fun Paper.withCanonicalIdentity(): Paper {
    val canonicalDoi = normalizePaperDoi(doi)

    return copy(
        doi = canonicalDoi,
        identityKey = buildPaperIdentityKey(
            doi = canonicalDoi,
            title = title,
            year = year
        )
    )
}
