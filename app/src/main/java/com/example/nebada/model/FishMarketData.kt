package com.example.nebada.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 한국수산회 실시간 경매 정보 데이터 모델
 */
data class FishAuctionResponse(
    @SerializedName("response")
    val response: AuctionResponseData
)

data class AuctionResponseData(
    @SerializedName("header")
    val header: ResponseHeader,
    @SerializedName("body")
    val body: AuctionBody
)

data class ResponseHeader(
    @SerializedName("resultCode")
    val resultCode: String,
    @SerializedName("resultMsg")
    val resultMsg: String
)

data class AuctionBody(
    @SerializedName("items")
    val items: List<FishAuctionItem>,
    @SerializedName("numOfRows")
    val numOfRows: Int,
    @SerializedName("pageNo")
    val pageNo: Int,
    @SerializedName("totalCount")
    val totalCount: Int
)

data class FishAuctionItem(
    @SerializedName("whsal_mrkt_nm")
    val marketName: String,           // 도매시장명
    @SerializedName("cpr_mrkt_nm")
    val corporationName: String,      // 법인명
    @SerializedName("cpr_instt_nm")
    val institutionName: String,      // 법인기관명
    @SerializedName("delng_de")
    val dealingDate: String,          // 거래일자
    @SerializedName("prdlst_nm")
    val productName: String,          // 품목명
    @SerializedName("spcies_nm")
    val speciesName: String,          // 품종명
    @SerializedName("delng_prut_nm")
    val gradeName: String,            // 등급명
    @SerializedName("std_qlity_nm")
    val qualityName: String,          // 표준품질명
    @SerializedName("sbid_pric")
    val bidPrice: Int,                // 경매가격(원)
    @SerializedName("delng_qy")
    val dealingQuantity: Double,      // 거래량(kg)
    @SerializedName("delng_am")
    val dealingAmount: Long,          // 거래금액(원)
    @SerializedName("std_frmlc_nm")
    val standardFormName: String,     // 표준규격명
    @SerializedName("delng_unit_nm")
    val dealingUnitName: String       // 거래단위명
) {
    // 추가 계산 필드들
    fun getPricePerKg(): Int {
        return if (dealingQuantity > 0) {
            (dealingAmount / dealingQuantity).toInt()
        } else bidPrice
    }

    fun getFormattedPrice(): String {
        return String.format("%,d원/kg", getPricePerKg())
    }

    fun getFormattedQuantity(): String {
        return String.format("%.1fkg", dealingQuantity)
    }

    fun getFormattedAmount(): String {
        return String.format("%,d원", dealingAmount)
    }
}