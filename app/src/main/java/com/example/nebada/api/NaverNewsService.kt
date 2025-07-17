package com.example.nebada.api

import com.example.nebada.model.NaverNewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 네이버 뉴스 검색 API 서비스
 */
interface NaverNewsService {

    @GET("v1/search/news.json")
    suspend fun searchNews(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int = 20,
        @Query("start") start: Int = 1,
        @Query("sort") sort: String = "date" // date: 최신순, sim: 정확도순
    ): Response<NaverNewsResponse>
}