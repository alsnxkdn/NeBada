package com.example.nebada.model

import com.google.gson.annotations.SerializedName

/**
 * NewsAPI 응답 모델
 */
data class NewsApiResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("totalResults")
    val totalResults: Int,
    @SerializedName("articles")
    val articles: List<NewsApiArticle>
)

/**
 * NewsAPI 기사 모델
 */
data class NewsApiArticle(
    @SerializedName("source")
    val source: NewsApiSource,
    @SerializedName("author")
    val author: String?,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("url")
    val url: String,
    @SerializedName("urlToImage")
    val urlToImage: String?,
    @SerializedName("publishedAt")
    val publishedAt: String,
    @SerializedName("content")
    val content: String?
)

/**
 * NewsAPI 소스 모델
 */
data class NewsApiSource(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String
)