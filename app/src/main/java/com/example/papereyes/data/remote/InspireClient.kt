package com.example.papereyes.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object InspireClient {

    private const val BASE_URL =
        "https://inspirehep.net/api/"


    val api: InspireApi by lazy {

        Retrofit.Builder()
            .baseUrl(
                BASE_URL
            )
            .client(
                ScholarlyHttpClient.client
            )
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(
                InspireApi::class.java
            )
    }
}
