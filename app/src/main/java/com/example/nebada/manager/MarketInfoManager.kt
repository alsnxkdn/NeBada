package com.example.nebada.manager

import android.content.Context
import com.example.nebada.model.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

/**
 * 시장정보 관리 클래스
 * 실제 구현 시에는 API를 통해 실시간 데이터를 가져와야 합니다.
 */
class MarketInfoManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 주요 어종 목록
    private val majorFishTypes = listOf(
        "고등어", "갈치", "삼치", "전갱이", "멸치", "정어리",
        "참돔", "농어", "광어", "가자미", "우럭", "볼락",
        "조기", "갑오징어", "문어", "주꾸미", "새우", "게"
    )

    // 시장 위치 목록
    private val markets = listOf(
        "부산공동어시장", "인천공동어시장", "목포수산시장",
        "포항죽도시장", "울산공동어시장", "제주동문시장"
    )

    /**
     * 실시간 시장정보 조회
     */
    suspend fun getMarketInfo(): List<FishMarketInfo> = withContext(Dispatchers.IO) {
        delay(1000) // 네트워크 지연 시뮬레이션

        majorFishTypes.map { fishName ->
            generateMarketInfo(fishName)
        }
    }

    /**
     * 특정 어종의 상세 정보 조회
     */
    suspend fun getDetailedMarketInfo(fishName: String): DetailedMarketInfo = withContext(Dispatchers.IO) {
        delay(500)

        val marketInfo = generateMarketInfo(fishName)
        val priceHistory = generatePriceHistory(marketInfo.currentPrice)
        val similarFish = majorFishTypes
            .filter { it != fishName }
            .shuffled()
            .take(3)
            .map { generateMarketInfo(it) }

        DetailedMarketInfo(
            fishMarketInfo = marketInfo,
            priceHistory = priceHistory,
            similarFish = similarFish,
            seasonalTrend = generateSeasonalTrend(fishName),
            recommendation = generateRecommendation(marketInfo)
        )
    }

    /**
     * 시장 뉴스 조회
     */
    suspend fun getMarketNews(): List<MarketNews> = withContext(Dispatchers.IO) {
        delay(500)

        listOf(
            MarketNews(
                id = "news_001",
                title = "고등어 가격 급등, 공급 부족 영향",
                content = "최근 어획량 감소로 인해 고등어 가격이 전주 대비 15% 상승했습니다.",
                publishDate = Date(),
                category = NewsCategory.PRICE,
                importance = NewsImportance.HIGH
            ),
            MarketNews(
                id = "news_002",
                title = "태풍 영향으로 일시적 출항 금지",
                content = "기상악화로 인한 출항 금지 조치가 내려져 수산물 공급에 차질이 예상됩니다.",
                publishDate = Date(System.currentTimeMillis() - 3600000), // 1시간 전
                category = NewsCategory.WEATHER,
                importance = NewsImportance.MEDIUM
            ),
            MarketNews(
                id = "news_003",
                title = "새우 양식장 생산량 증가",
                content = "올해 새우 양식장 생산량이 작년 대비 20% 증가하여 가격 안정화가 예상됩니다.",
                publishDate = Date(System.currentTimeMillis() - 7200000), // 2시간 전
                category = NewsCategory.MARKET,
                importance = NewsImportance.LOW
            )
        )
    }

    /**
     * 가격 알림 설정
     */
    fun setPriceAlert(fishName: String, targetPrice: Double, alertType: AlertType) {
        // 실제 구현 시 알림 서비스 연동
        // 현재는 예시 구현
    }

    private fun generateMarketInfo(fishName: String): FishMarketInfo {
        val basePrice = when (fishName) {
            "고등어" -> 8000.0
            "갈치" -> 15000.0
            "삼치" -> 12000.0
            "전갱이" -> 6000.0
            "멸치" -> 5000.0
            "정어리" -> 4000.0
            "참돔" -> 25000.0
            "농어" -> 20000.0
            "광어" -> 18000.0
            "가자미" -> 10000.0
            "우럭" -> 16000.0
            "볼락" -> 14000.0
            "조기" -> 13000.0
            "갑오징어" -> 11000.0
            "문어" -> 22000.0
            "주꾸미" -> 9000.0
            "새우" -> 30000.0
            "게" -> 35000.0
            else -> 10000.0
        }

        val priceVariation = (Random.nextDouble() - 0.5) * 0.3 // ±15% 변동
        val currentPrice = basePrice * (1 + priceVariation)
        val previousPrice = currentPrice * (1 + (Random.nextDouble() - 0.5) * 0.1) // ±5% 변동
        val priceChange = currentPrice - previousPrice
        val priceChangePercent = (priceChange / previousPrice) * 100

        return FishMarketInfo(
            fishName = fishName,
            currentPrice = currentPrice,
            previousPrice = previousPrice,
            priceChange = priceChange,
            priceChangePercent = priceChangePercent,
            marketCondition = when {
                priceChangePercent > 3 -> MarketCondition.STRONG
                priceChangePercent < -3 -> MarketCondition.WEAK
                kotlin.math.abs(priceChangePercent) > 1 -> MarketCondition.VOLATILE
                else -> MarketCondition.STABLE
            },
            supply = SupplyStatus.values()[Random.nextInt(SupplyStatus.values().size)],
            lastUpdated = Date(),
            marketLocation = markets[Random.nextInt(markets.size)],
            gradeInfo = when (Random.nextInt(3)) {
                0 -> "특급"
                1 -> "상급"
                else -> "중급"
            },
            averageSize = "${Random.nextInt(15, 35)}cm",
            catchArea = when (Random.nextInt(4)) {
                0 -> "동해"
                1 -> "서해"
                2 -> "남해"
                else -> "제주"
            }
        )
    }

    private fun generatePriceHistory(currentPrice: Double): List<PriceHistory> {
        val history = mutableListOf<PriceHistory>()
        val calendar = Calendar.getInstance()

        // 최근 30일 데이터 생성
        repeat(30) { day ->
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val variation = (Random.nextDouble() - 0.5) * 0.15 // ±7.5% 변동
            val price = currentPrice * (1 + variation)
            val volume = Random.nextDouble() * 50 + 10 // 10-60톤

            history.add(PriceHistory(
                date = calendar.time.clone() as Date,
                price = price,
                volume = volume
            ))
        }

        return history.reversed() // 오래된 것부터 정렬
    }

    private fun generateSeasonalTrend(fishName: String): String {
        return when (fishName) {
            "고등어" -> "가을~겨울철이 제철로 가격이 하락하는 추세입니다."
            "갈치" -> "늦가을부터 겨울까지가 성어기로 품질이 좋아집니다."
            "새우" -> "여름철 양식 새우 출하로 가격이 안정됩니다."
            else -> "계절에 따른 가격 변동이 있으니 시기를 고려한 판매를 권장합니다."
        }
    }

    private fun generateRecommendation(marketInfo: FishMarketInfo): String {
        return when {
            marketInfo.marketCondition == MarketCondition.STRONG && marketInfo.supply == SupplyStatus.LIMITED ->
                "현재 강세장이며 공급이 부족한 상황입니다. 판매하기 좋은 시기입니다."
            marketInfo.marketCondition == MarketCondition.WEAK && marketInfo.supply == SupplyStatus.ABUNDANT ->
                "약세장이며 공급이 풍부한 상황입니다. 가격 회복을 기다리는 것이 좋겠습니다."
            marketInfo.priceChangePercent > 5 ->
                "가격이 급등한 상황입니다. 빠른 판매를 고려해보세요."
            else ->
                "안정적인 시장 상황입니다. 적절한 시기에 판매하시면 됩니다."
        }
    }

    fun destroy() {
        scope.cancel()
    }
}

enum class AlertType {
    PRICE_ABOVE,  // 가격 이상
    PRICE_BELOW,  // 가격 이하
    SUPPLY_CHANGE // 공급 상태 변화
}