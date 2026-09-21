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
}

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