package com.example.papereyes.data.remote

import retrofit2.Retrofit

object ArxivClient {
    private const val BASE_URL = "https://export.arxiv.org/"

    val api: ArxivApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(ScholarlyHttpClient.client)
            .build()
            .create(ArxivApi::class.java)
    }
}
