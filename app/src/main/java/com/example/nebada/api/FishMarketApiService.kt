package com.example.nebada.api

import com.example.nebada.model.FishAuctionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FishMarketApiService {

    /**
     * 한국수산회 실시간 경매 정보 조회
     */
    @GET("1360000/FshItmCtgMrkAucPrcInq/getFshItmCtgMrkAucPrcInq")
    suspend fun getFishAuctionData(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "json",
        @Query("whsal_mrkt_nm") marketName: String? = null,      // 도매시장명 (선택)
        @Query("prdlst_nm") productName: String? = null,         // 품목명 (선택)
        @Query("delng_de") dealingDate: String? = null           // 거래일자 (YYYYMMDD, 선택)
    ): Response<FishAuctionResponse>

    /**
     * 특정 지역의 경매 정보 조회
     */
    suspend fun getFishAuctionByRegion(
        serviceKey: String,
        region: String,
        pageNo: Int = 1,
        numOfRows: Int = 50
    ): Response<FishAuctionResponse> {
        val marketName = when (region) {
            "부산" -> "부산공동어시장"
            "인천" -> "인천종합어시장"
            "울산" -> "울산공동어시장"
            "목포" -> "목포수협공판장"
            "여수" -> "여수수협공판장"
            "포항" -> "포항공동어시장"
            else -> null
        }

        return getFishAuctionData(
            serviceKey = serviceKey,
            pageNo = pageNo,
            numOfRows = numOfRows,
            marketName = marketName
        )
    }
}