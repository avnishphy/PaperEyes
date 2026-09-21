package com.example.papereyes.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenAlexApi {
    @GET("works")
    suspend fun searchWorks(
        @Query("search") search: String,
        @Query("per_page") perPage: Int = 5,
        @Query("select") select: String = "id,doi,title,publication_year,authorships,abstract_inverted_index"
    ): OpenAlexResponse
}

data class OpenAlexResponse(val results: List<OpenAlexWork>?)
data class OpenAlexWork(
    val id: String?, val doi: String?, val title: String?,
    @SerializedName("publication_year") val publicationYear: Int?,
    val authorships: List<OpenAlexAuthorship>?,
    @SerializedName("abstract_inverted_index") val abstractInvertedIndex: Map<String, List<Int>>?
)
data class OpenAlexAuthorship(val author: OpenAlexAuthor?)
data class OpenAlexAuthor(@SerializedName("display_name") val displayName: String?)
