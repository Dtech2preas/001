package com.dtech.music.network

import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

object OkHttpSingleton {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
