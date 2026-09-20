package com.example.papereyes.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


object ScholarlyHttpClient {

    val client: OkHttpClient by lazy {

        OkHttpClient.Builder()
            .connectTimeout(
                10,
                TimeUnit.SECONDS
            )
            .readTimeout(
                15,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                10,
                TimeUnit.SECONDS
            )
            .callTimeout(
                20,
                TimeUnit.SECONDS
            )
            .retryOnConnectionFailure(
                true
            )
            .addInterceptor { chain ->

                val original = chain.request()
                val requestBuilder = original
                    .newBuilder()
                    .header(
                        "User-Agent",
                        "PaperEyes/1.0 (Android; https://github.com/avnishphy/PaperEyes)"
                    )

                if (original.header("Accept") == null) {
                    requestBuilder.header("Accept", "application/json")
                }

                val request = requestBuilder.build()


                chain.proceed(
                    request
                )
            }
            .build()
    }
}
