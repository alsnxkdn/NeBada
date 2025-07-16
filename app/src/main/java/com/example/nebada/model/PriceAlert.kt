package com.example.nebada.model

import com.example.nebada.manager.AlertType
import java.util.Date

/**
 * 가격 알림 데이터 클래스
 */
data class PriceAlert(
    val id: String,
    val fishName: String,
    val targetPrice: Double,
    val alertType: AlertType,
    val isEnabled: Boolean,
    val createdDate: Date
)