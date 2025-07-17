package com.example.nebada.model

import com.google.gson.annotations.SerializedName

/**
 * 네이버 뉴스 검색 API 응답 모델
 */
data class NaverNewsResponse(
    @SerializedName("lastBuildDate")
    val lastBuildDate: String,
    @SerializedName("total")
    val total: Int,
    @SerializedName("start")
    val start: Int,
    @SerializedName("display")
    val display: Int,
    @SerializedName("items")
    val items: List<NaverNewsItem>
)

/**
 * 네이버 뉴스 아이템 모델
 */
data class NaverNewsItem(
    @SerializedName("title")
    val title: String,
    @SerializedName("originallink")
    val originallink: String,
    @SerializedName("link")
    val link: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("pubDate")
    val pubDate: String
)