// app/src/main/java/com/example/nebada/manager/RealTimeFishMarketManager.kt
package com.example.nebada.manager

import android.content.Context
import android.util.Log
import com.example.nebada.api.FishMarketApiService
import com.example.nebada.model.FishAuctionItem
import com.example.nebada.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RealTimeFishMarketManager(private val context: Context) {

    private val apiService: FishMarketApiService = ApiClient.fishMarketService
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    /**
     * 실시간 경매 정보 가져오기
     */
    suspend fun getRealTimeAuctionData(
        region: String? = null,
        fishType: String? = null
    ): List<FishAuctionItem> {
        return withContext(Dispatchers.IO) {
            try {
                val today = dateFormat.format(Date())

                val response = if (region != null) {
                    apiService.getFishAuctionByRegion(
                        serviceKey = ApiClient.API_KEY,
                        region = region,
                        numOfRows = 100
                    )
                } else {
                    apiService.getFishAuctionData(
                        serviceKey = ApiClient.API_KEY,
                        dealingDate = today,
                        productName = fishType,
                        numOfRows = 100
                    )
                }

                if (response.isSuccessful) {
                    val auctionData = response.body()
                    if (auctionData?.response?.header?.resultCode == "00") {
                        Log.d("FishMarket", "성공적으로 ${auctionData.response.body.items.size}개 데이터 조회")
                        auctionData.response.body.items
                    } else {
                        Log.e("FishMarket", "API 응답 오류: ${auctionData?.response?.header?.resultMsg}")
                        getDummyData() // 실패 시 더미 데이터 반환
                    }
                } else {
                    Log.e("FishMarket", "HTTP 오류: ${response.code()}")
                    getDummyData() // 실패 시 더미 데이터 반환
                }

            } catch (e: Exception) {
                Log.e("FishMarket", "네트워크 오류: ${e.message}")
                getDummyData() // 오류 시 더미 데이터 반환
            }
        }
    }

    /**
     * 특정 어종의 최신 가격 정보
     */
    suspend fun getFishPriceInfo(fishName: String, region: String? = null): List<FishAuctionItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFishAuctionData(
                    serviceKey = ApiClient.API_KEY,
                    productName = fishName,
                    marketName = getMarketNameByRegion(region),
                    numOfRows = 20
                )

                if (response.isSuccessful && response.body()?.response?.header?.resultCode == "00") {
                    response.body()!!.response.body.items
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("FishMarket", "어종별 가격 조회 오류: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * 지역별 시장명 매핑
     */
    private fun getMarketNameByRegion(region: String?): String? {
        return when (region) {
            "부산" -> "부산공동어시장"
            "인천" -> "인천종합어시장"
            "울산" -> "울산공동어시장"
            "목포" -> "목포수협공판장"
            "여수" -> "여수수협공판장"
            "포항" -> "포항공동어시장"
            "강릉" -> "강릉수협공판장"
            "속초" -> "속초수협공판장"
            else -> null
        }
    }

    /**
     * 더미 데이터 (API 실패 시 사용)
     */
    private fun getDummyData(): List<FishAuctionItem> {
        val today = dateFormat.format(Date())
        return listOf(
            FishAuctionItem(
                marketName = "부산공동어시장",
                corporationName = "부산수협",
                institutionName = "부산수산업협동조합",
                dealingDate = today,
                productName = "고등어",
                speciesName = "고등어",
                gradeName = "특급",
                qualityName = "활어",
                bidPrice = 15000,
                dealingQuantity = 1000.0,
                dealingAmount = 15000000,
                standardFormName = "1kg",
                dealingUnitName = "kg"
            ),
            FishAuctionItem(
                marketName = "부산공동어시장",
                corporationName = "부산수협",
                institutionName = "부산수산업협동조합",
                dealingDate = today,
                productName = "갈치",
                speciesName = "갈치",
                gradeName = "1급",
                qualityName = "선어",
                bidPrice = 25000,
                dealingQuantity = 500.0,
                dealingAmount = 12500000,
                standardFormName = "1kg",
                dealingUnitName = "kg"
            ),
            FishAuctionItem(
                marketName = "인천종합어시장",
                corporationName = "인천수협",
                institutionName = "인천수산업협동조합",
                dealingDate = today,
                productName = "농어",
                speciesName = "농어",
                gradeName = "특급",
                qualityName = "활어",
                bidPrice = 30000,
                dealingQuantity = 300.0,
                dealingAmount = 9000000,
                standardFormName = "1kg",
                dealingUnitName = "kg"
            )
        )
    }
}