package com.example.papereyes.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ArxivApi {
    @Headers("Accept: application/atom+xml")
    @GET("api/query")
    suspend fun queryById(
        @Query("id_list") arxivId: String
    ): ResponseBody
}
