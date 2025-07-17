package com.example.nebada.api

import com.example.nebada.model.NewsApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * NewsAPI 서비스 인터페이스
 */
interface NewsApiService {

    @GET("v2/everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("apiKey") apiKey: String,
        @Query("language") language: String = "ko",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<NewsApiResponse>

    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("apiKey") apiKey: String,
        @Query("country") country: String = "kr",
        @Query("category") category: String? = null,
        @Query("pageSize") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): Response<NewsApiResponse>
}