package com.example.nebada.model

import java.util.Date

/**
 * 어획 기록 데이터 모델
 */
data class CatchRecord(
    val id: String = "",
    val fishType: String = "",           // 어종
    val weight: Double = 0.0,            // 무게 (kg)
    val quantity: Int = 0,               // 수량 (마리)
    val location: String = "",           // 어획 위치
    val latitude: Double = 0.0,          // 위도
    val longitude: Double = 0.0,         // 경도
    val date: Date = Date(),             // 어획 날짜
    val weather: String = "",            // 날씨 상태
    val waterTemp: Double = 0.0,         // 수온 (°C)
    val depth: Double = 0.0,             // 수심 (m)
    val method: String = "",             // 어법 (그물, 낚시 등)
    val notes: String = "",              // 기타 메모
    val price: Double = 0.0,             // 판매 가격 (원/kg)
    val totalValue: Double = 0.0         // 총 가치 (원)
) {
    fun calculateTotalValue(): Double {
        return weight * price
    }
}