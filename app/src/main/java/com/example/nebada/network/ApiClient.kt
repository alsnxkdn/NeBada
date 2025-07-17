// app/src/main/java/com/example/nebada/network/ApiClient.kt
package com.example.nebada.network

import com.example.nebada.api.FishMarketApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // 공공데이터포털 API 기본 URL
    private const val BASE_URL = "https://apis.data.go.kr/"

    // 공공데이터포털에서 발급받은 API 키 (실제 키로 교체 필요)
    const val API_KEY = "YOUR_API_KEY_HERE"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val fishMarketService: FishMarketApiService = retrofit.create(FishMarketApiService::class.java)
}