package com.example.papereyes.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SemanticScholarApi {

    @GET("paper/{paperId}")
    suspend fun getPaper(
        @Path("paperId")
        paperId: String,

        @Query("fields")
        fields: String =
            "title,authors,year,url,externalIds"
    ): SemanticScholarPaper

    /**
     * Semantic Scholar's title-match endpoint returns the single closest title
     * match and is intended for interactive title retrieval.
     */
    @GET("paper/search/match")
    suspend fun searchPaperMatch(
        @Query("query")
        query: String,

        @Query("fields")
        fields: String =
            "title,authors,year,url,externalIds"
    ): SemanticScholarMatchResponse
}

data class SemanticScholarMatchResponse(
    val data: List<SemanticScholarPaperMatch>?
)

data class SemanticScholarPaperMatch(
    val matchScore: Double?,
    val title: String?,
    val year: Int?,
    val url: String?,
    val authors: List<SemanticScholarAuthor>?,
    val externalIds: SemanticScholarExternalIds?
)

data class SemanticScholarPaper(
    val title: String?,
    val year: Int?,
    val url: String?,
    val authors: List<SemanticScholarAuthor>?,
    val externalIds: SemanticScholarExternalIds?
)

data class SemanticScholarAuthor(
    val name: String?
)

data class SemanticScholarExternalIds(

    @SerializedName("DOI")
    val doi: String?,

    @SerializedName("ArXiv")
    val arxiv: String?
)
