package com.example.nebada.model

import java.util.Date

/**
 * 어종 시장 정보 데이터 모델
 */
data class FishMarketInfo(
    val fishName: String,                // 어종명
    val currentPrice: Double,            // 현재 가격 (원/kg)
    val previousPrice: Double,           // 전일 가격 (원/kg)
    val priceChange: Double,             // 가격 변동 (원)
    val priceChangePercent: Double,      // 가격 변동률 (%)
    val marketCondition: MarketCondition, // 시장 상황
    val supply: SupplyStatus,            // 공급 상태
    val lastUpdated: Date,               // 최종 업데이트 시간
    val marketLocation: String,          // 시장 위치
    val gradeInfo: String = "",          // 등급 정보
    val averageSize: String = "",        // 평균 크기
    val catchArea: String = ""           // 주요 어획 지역
)

/**
 * 시장 상황
 */
enum class MarketCondition(val displayName: String, val color: String) {
    STRONG("강세", "#FF4CAF50"),          // 녹색
    STABLE("보합", "#FF2196F3"),          // 파란색
    WEAK("약세", "#FFF44336"),            // 빨간색
    VOLATILE("불안정", "#FFFF9800")       // 주황색
}

/**
 * 공급 상태
 */
enum class SupplyStatus(val displayName: String, val color: String) {
    ABUNDANT("풍부", "#FF4CAF50"),        // 녹색
    NORMAL("보통", "#FF2196F3"),          // 파란색
    LIMITED("부족", "#FFFF9800"),         // 주황색
    SCARCE("매우 부족", "#FFF44336")      // 빨간색
}

/**
 * 가격 히스토리 데이터
 */
data class PriceHistory(
    val date: Date,
    val price: Double,
    val volume: Double = 0.0  // 거래량 (톤)
)

/**
 * 시장 뉴스/공지 데이터
 */
data class MarketNews(
    val id: String,
    val title: String,
    val content: String,
    val publishDate: Date,
    val category: NewsCategory,
    val importance: NewsImportance,
    val url: String? = null, // 원본 기사 URL
    val region: String = "전국" // 지역 정보 추가
)

enum class NewsCategory(val displayName: String) {
    PRICE("가격정보"),
    WEATHER("날씨영향"),
    REGULATION("규제정보"),
    MARKET("시장동향"),
    GENERAL("일반")
}

enum class NewsImportance(val displayName: String, val color: String) {
    HIGH("중요", "#FFF44336"),
    MEDIUM("보통", "#FFFF9800"),
    LOW("일반", "#FF757575")
}

/**
 * 어종별 상세 시장 정보
 */
data class DetailedMarketInfo(
    val fishMarketInfo: FishMarketInfo,
    val priceHistory: List<PriceHistory>,
    val similarFish: List<FishMarketInfo>,  // 유사 어종 정보
    val seasonalTrend: String,              // 계절별 동향
    val recommendation: String              // 거래 추천사항
)