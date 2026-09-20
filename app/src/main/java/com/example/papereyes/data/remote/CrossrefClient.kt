package com.example.papereyes.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object CrossrefClient {

    private const val BASE_URL =
        "https://api.crossref.org/"


    val api: CrossrefApi by lazy {

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
                CrossrefApi::class.java
            )
    }
}
