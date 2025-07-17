// app/src/main/java/com/example/nebada/model/MarketNewsItem.kt
package com.example.nebada.model

data class MarketNewsItem(
    val id: String,
    val title: String,
    val content: String,
    val region: String,
    val date: String,
    val imageUrl: String? = null,
    val category: String = "일반",
    val isRealTime: Boolean = false,
    val price: Int = 0,
    val fishType: String = ""
) {
    fun getFormattedPrice(): String {
        return if (price > 0) {
            String.format("%,d원/kg", price)
        } else {
            "가격 정보 없음"
        }
    }

    fun getCategoryColor(): Int {
        return when (category) {
            "실시간경매" -> 0xFF4CAF50.toInt() // 녹색
            "테스트데이터" -> 0xFF9E9E9E.toInt() // 회색
            "시장분석" -> 0xFF2196F3.toInt() // 파란색
            "기상정보" -> 0xFF9C27B0.toInt() // 보라색
            else -> 0xFF757575.toInt() // 기본 회색
        }
    }

    fun getCategoryIcon(): String {
        return when (category) {
            "실시간경매" -> "🔴"
            "테스트데이터" -> "🔧"
            "시장분석" -> "📊"
            "기상정보" -> "🌊"
            else -> "📰"
        }
    }
}