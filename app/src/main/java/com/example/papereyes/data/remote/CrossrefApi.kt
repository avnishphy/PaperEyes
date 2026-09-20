package com.example.papereyes.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


private const val CROSSREF_SELECT_FIELDS =
    "DOI,title,author,URL,container-title,short-container-title,volume,issue,page,article-number,published-print,published-online,published"


interface CrossrefApi {

    @GET("works")
    suspend fun searchWorks(
        @Query("query.bibliographic")
        query: String? = null,

        @Query("query.container-title")
        containerTitle: String? = null,

        @Query("filter")
        filter: String? = null,

        @Query("rows")
        rows: Int = 5,

        /*
         * PaperEyes only needs the bibliographic fields below. Asking
         * Crossref to omit unrelated metadata reduces response size and JSON
         * parsing work, which matters on repeated OCR lookups.
         */
        @Query("select")
        select: String = CROSSREF_SELECT_FIELDS
    ): CrossrefResponse


    @GET("works/{doi}")
    suspend fun getWorkByDoi(
        @Path("doi")
        doi: String
    ): CrossrefSingleResponse
}


data class CrossrefResponse(
    val message: CrossrefMessage?
)


data class CrossrefSingleResponse(
    val message: CrossrefItem?
)


data class CrossrefMessage(
    val items: List<CrossrefItem>?
)


data class CrossrefItem(
    val title: List<String>?,
    val author: List<CrossrefAuthor>?,

    @SerializedName("DOI")
    val doi: String?,

    @SerializedName("URL")
    val url: String?,

    @SerializedName("container-title")
    val containerTitle: List<String>?,

    @SerializedName("short-container-title")
    val shortContainerTitle: List<String>?,

    val volume: String?,
    val issue: String?,
    val page: String?,

    @SerializedName("article-number")
    val articleNumber: String?,

    @SerializedName("published-print")
    val publishedPrint: CrossrefDate?,

    @SerializedName("published-online")
    val publishedOnline: CrossrefDate?,

    val published: CrossrefDate?
)


data class CrossrefAuthor(
    val given: String?,
    val family: String?
)


data class CrossrefDate(
    @SerializedName("date-parts")
    val dateParts: List<List<Int>>?
)
