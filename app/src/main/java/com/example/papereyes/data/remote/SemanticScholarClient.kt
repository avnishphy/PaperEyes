package com.example.papereyes.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object SemanticScholarClient {

    private const val BASE_URL =
        "https://api.semanticscholar.org/graph/v1/"

    /*
     * Semantic Scholar is the interactive fast path for title scans. Keep its
     * timeout intentionally short; the normal resolver can fall back to
     * Crossref/arXiv instead of making the camera UI wait on a stalled call.
     */
    private val interactiveHttpClient by lazy {
        ScholarlyHttpClient.client
            .newBuilder()
            .connectTimeout(1_500, TimeUnit.MILLISECONDS)
            .readTimeout(2_000, TimeUnit.MILLISECONDS)
            .writeTimeout(1_500, TimeUnit.MILLISECONDS)
            .callTimeout(2_500, TimeUnit.MILLISECONDS)
            .build()
    }

    val api: SemanticScholarApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(interactiveHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(SemanticScholarApi::class.java)
    }
}
