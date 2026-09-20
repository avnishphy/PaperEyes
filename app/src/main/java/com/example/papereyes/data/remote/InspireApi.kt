package com.example.papereyes.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query


interface InspireApi {

    /*
     * Example:
     *
     * q = "j Phys.Rev.D,112,034009 and jy 2025"
     */
    @GET("literature")
    suspend fun searchLiterature(
        @Query("q")
        query: String,

        @Query("size")
        size: Int = 5
    ): InspireSearchResponse
}


data class InspireSearchResponse(
    val hits: InspireHits?
)


data class InspireHits(
    val total: Int?,
    val hits: List<InspireHit>?
)


data class InspireHit(
    val metadata: InspireMetadata?,
    val links: InspireLinks?
)


data class InspireLinks(
    val self: String?
)


data class InspireMetadata(

    val titles: List<InspireTitle>?,

    val authors: List<InspireAuthor>?,

    @SerializedName("publication_info")
    val publicationInfo: List<InspirePublicationInfo>?,

    val dois: List<InspireDoi>?,

    @SerializedName("arxiv_eprints")
    val arxivEprints: List<InspireArxivEprint>?
)


data class InspireTitle(
    val title: String?
)


data class InspireAuthor(

    @SerializedName("full_name")
    val fullName: String?
)


data class InspirePublicationInfo(

    @SerializedName("journal_title")
    val journalTitle: String?,

    @SerializedName("journal_volume")
    val journalVolume: String?,

    @SerializedName("journal_issue")
    val journalIssue: String?,

    val year: Int?,

    /*
     * Article ID, e.g.:
     *
     * 034009
     * 262001
     * P05001
     */
    val artid: String?,

    @SerializedName("page_start")
    val pageStart: String?,

    @SerializedName("page_end")
    val pageEnd: String?
)


data class InspireDoi(
    val value: String?
)


data class InspireArxivEprint(
    val value: String?
)