package com.example.papereyes.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object SemanticScholarClient {

    private const val BASE_URL =
        "https://api.semanticscholar.org/graph/v1/"


    val api: SemanticScholarApi by lazy {

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
                SemanticScholarApi::class.java
            )
    }
}
