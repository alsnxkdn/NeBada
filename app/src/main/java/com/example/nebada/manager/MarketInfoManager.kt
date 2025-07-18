package com.example.nebada.manager

import android.content.Context
import android.text.Html
import e
import com.example.nebada.model.*
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * 시장정보 관리 클래스
 * 네이버 뉴스 검색 API를 사용하여 실제 뉴스를 가져옵니다.
 */
class MarketInfoManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val naverNewsService: NaverNewsService

    // 네이버 개발자 센터에서 발급받은 클라이언트 ID와 시크릿
    private val clientId = "3zRVeeCLVji5_ZIXRZd0"
    private val clientSecret = "9fnxSqcwgl"

    init {
        // OkHttp 클라이언트 생성 (로깅 포함)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openapi.naver.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        naverNewsService = retrofit.create(NaverNewsService::class.java)
    }

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
     * 시장 뉴스 조회 (네이버 뉴스 API 사용) - 최신 1달치
     */
    suspend fun getMarketNews(): List<MarketNews> = withContext(Dispatchers.IO) {
        try {
            println("네이버 뉴스 API 호출 시작...")

            val fishingKeywords = listOf(
                "수산업", "어업", "어시장"
            )

            val allNews = mutableListOf<MarketNews>()

            // 여러 키워드로 검색하여 다양한 뉴스 수집
            fishingKeywords.take(3).forEach { keyword ->
                try {
                    println("키워드 '$keyword'로 검색 중...")

                    val response = naverNewsService.searchNews(
                        clientId = clientId,
                        clientSecret = clientSecret,
                        query = keyword,
                        display = 10,
                        start = 1,
                        sort = "date"
                    )

                    println("API 응답 코드: ${response.code()}")
                    println("API 응답 성공: ${response.isSuccessful}")

                    if (response.isSuccessful) {
                        val newsResponse = response.body()
                        val items = newsResponse?.items ?: emptyList()

                        println("가져온 뉴스 개수: ${items.size}")

                        items.forEach { item ->
                            try {
                                val publishDate = parseNaverNewsDate(item.pubDate)

                                // 1달 이내 뉴스만 필터링
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.MONTH, -1)
                                val oneMonthAgo = calendar.time

                                if (publishDate.after(oneMonthAgo)) {
                                    val cleanTitle = Html.fromHtml(item.title, Html.FROM_HTML_MODE_LEGACY).toString()
                                    val cleanDescription = Html.fromHtml(item.description, Html.FROM_HTML_MODE_LEGACY).toString()

                                    val category = determineNewsCategory(cleanTitle, cleanDescription)
                                    val importance = determineImportance(cleanTitle, cleanDescription)

                                    val news = MarketNews(
                                        id = "naver_news_${System.currentTimeMillis()}_${Random.nextInt(10000)}",
                                        title = cleanTitle,
                                        content = cleanDescription,
                                        publishDate = publishDate,
                                        category = category,
                                        importance = importance,
                                        url = if (item.originallink.isNotEmpty()) item.originallink else item.link,
                                        region = extractRegion(cleanTitle, cleanDescription)
                                    )

                                    allNews.add(news)
                                    println("뉴스 추가: ${cleanTitle}")
                                }
                            } catch (e: Exception) {
                                println("뉴스 파싱 오류: ${e.message}")
                            }
                        }
                    } else {
                        println("API 호출 실패: ${response.code()} - ${response.message()}")
                        // 오류 응답 본문 출력
                        val errorBody = response.errorBody()?.string()
                        println("오류 내용: $errorBody")
                    }

                    // API 호출 간격 조절
                    delay(100)
                } catch (e: Exception) {
                    println("키워드 '$keyword' 검색 오류: ${e.message}")
                    e.printStackTrace()
                }
            }

            println("총 수집된 뉴스 개수: ${allNews.size}")

            // 중복 제거 및 정렬
            val finalNews = allNews.distinctBy { it.title }
                .sortedByDescending { it.publishDate }
                .take(20)

            println("최종 뉴스 개수: ${finalNews.size}")

            if (finalNews.isEmpty()) {
                println("실제 뉴스가 없어서 더미 데이터 반환")
                getFallbackNews()
            } else {
                finalNews
            }

        } catch (e: Exception) {
            println("전체 API 호출 오류: ${e.message}")
            e.printStackTrace()
            // 전체 API 호출 실패 시 더미 데이터 반환
            getFallbackNews()
        }
    }

    /**
     * 네이버 뉴스 날짜 파싱
     */
    private fun parseNaverNewsDate(dateString: String): Date {
        return try {
            // 네이버 뉴스 날짜 형식: "Mon, 15 Jan 2024 14:30:00 +0900"
            val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            formatter.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    /**
     * 뉴스 카테고리 결정
     */
    private fun determineNewsCategory(title: String, description: String): NewsCategory {
        val text = "$title $description".lowercase()

        return when {
            text.contains("가격") || text.contains("시세") || text.contains("값") ||
                    text.contains("수익") || text.contains("소득") -> NewsCategory.PRICE
            text.contains("날씨") || text.contains("태풍") || text.contains("기상") ||
                    text.contains("폭우") || text.contains("해일") -> NewsCategory.WEATHER
            text.contains("규제") || text.contains("법") || text.contains("정책") ||
                    text.contains("제도") || text.contains("허가") -> NewsCategory.REGULATION
            text.contains("시장") || text.contains("거래") || text.contains("판매") ||
                    text.contains("유통") || text.contains("경매") -> NewsCategory.MARKET
            else -> NewsCategory.GENERAL
        }
    }

    /**
     * 뉴스 중요도 결정
     */
    private fun determineImportance(title: String, description: String): NewsImportance {
        val text = "$title $description".lowercase()

        return when {
            text.contains("급등") || text.contains("폭락") || text.contains("비상") ||
                    text.contains("위기") || text.contains("긴급") -> NewsImportance.HIGH
            text.contains("상승") || text.contains("하락") || text.contains("변동") ||
                    text.contains("주의") -> NewsImportance.MEDIUM
            else -> NewsImportance.LOW
        }
    }

    /**
     * 뉴스에서 지역 정보 추출 (개선된 버전)
     */
    private fun extractRegion(title: String, description: String): String {
        val text = "$title $description"

        // 광역시/도 우선 매칭
        val regions = mapOf(
            "서울" to listOf("서울", "서울시", "서울특별시", "강남", "강북", "종로", "중구", "성동", "광진", "동대문", "중랑", "성북", "강북", "도봉", "노원", "은평", "서대문", "마포", "양천", "강서", "구로", "금천", "영등포", "동작", "관악", "서초", "강남", "송파", "강동"),
            "부산" to listOf("부산", "부산시", "부산광역시", "해운대", "사하", "금정", "강서", "연제", "수영", "사상", "기장", "중구", "서구", "동구", "영도", "부산진", "동래", "남구", "북구"),
            "대구" to listOf("대구", "대구시", "대구광역시", "중구", "동구", "서구", "남구", "북구", "수성", "달서", "달성"),
            "인천" to listOf("인천", "인천시", "인천광역시", "중구", "동구", "미추홀", "연수", "남동", "부평", "계양", "서구", "강화", "옹진"),
            "광주" to listOf("광주", "광주시", "광주광역시", "동구", "서구", "남구", "북구", "광산"),
            "대전" to listOf("대전", "대전시", "대전광역시", "동구", "중구", "서구", "유성", "대덕"),
            "울산" to listOf("울산", "울산시", "울산광역시", "중구", "남구", "동구", "북구", "울주"),
            "세종" to listOf("세종", "세종시", "세종특별자치시"),
            "경기" to listOf("경기", "경기도", "수원", "성남", "의정부", "안양", "부천", "광명", "평택", "동두천", "안산", "고양", "과천", "구리", "남양주", "오산", "시흥", "군포", "의왕", "하남", "용인", "파주", "이천", "안성", "김포", "화성", "광주", "양주", "포천", "여주", "연천", "가평", "양평"),
            "강원" to listOf("강원", "강원도", "춘천", "원주", "강릉", "동해", "태백", "속초", "삼척", "홍천", "횡성", "영월", "평창", "정선", "철원", "화천", "양구", "인제", "고성", "양양"),
            "충북" to listOf("충북", "충청북도", "청주", "충주", "제천", "보은", "옥천", "영동", "증평", "진천", "괴산", "음성", "단양"),
            "충남" to listOf("충남", "충청남도", "천안", "공주", "보령", "아산", "서산", "논산", "계룡", "당진", "금산", "부여", "서천", "청양", "홍성", "예산", "태안"),
            "전북" to listOf("전북", "전라북도", "전주", "군산", "익산", "정읍", "남원", "김제", "완주", "진안", "무주", "장수", "임실", "순창", "고창", "부안"),
            "전남" to listOf("전남", "전라남도", "목포", "여수", "순천", "나주", "광양", "담양", "곡성", "구례", "고흥", "보성", "화순", "장흥", "강진", "해남", "영암", "무안", "함평", "영광", "장성", "완도", "진도", "신안"),
            "경북" to listOf("경북", "경상북도", "포항", "경주", "김천", "안동", "구미", "영주", "영천", "상주", "문경", "경산", "군위", "의성", "청송", "영양", "영덕", "청도", "고령", "성주", "칠곡", "예천", "봉화", "울진", "울릉"),
            "경남" to listOf("경남", "경상남도", "창원", "진주", "통영", "사천", "김해", "밀양", "거제", "양산", "의령", "함안", "창녕", "고성", "남해", "하동", "산청", "함양", "거창", "합천"),
            "제주" to listOf("제주", "제주도", "제주시", "서귀포")
        )

        // 지역명 매칭
        for ((region, keywords) in regions) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    return region
                }
            }
        }

        return "전국"
    }

    /**
     * 대체 뉴스 데이터 (API 실패 시) - 네이버 뉴스 형식
     */
    private fun getFallbackNews(): List<MarketNews> {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        return listOf(
            MarketNews(
                id = "fallback_001",
                title = "고등어 가격 급등세, 어민들 '깜짝 수익'",
                content = "최근 고등어 가격이 평년 대비 30% 이상 오르면서 어민들의 소득이 크게 증가하고 있다. 어획량 감소와 수요 증가가 주요 원인으로 분석된다.",
                publishDate = Date(now - oneDay),
                category = NewsCategory.PRICE,
                importance = NewsImportance.HIGH,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=101",
                region = "전국"
            ),
            MarketNews(
                id = "fallback_002",
                title = "태풍 '카눈' 영향으로 동해안 어업 전면 중단",
                content = "제10호 태풍 '카눈'의 영향으로 동해안 일대 어업이 전면 중단되면서 수산물 공급에 차질이 예상된다고 해양수산부가 발표했다.",
                publishDate = Date(now - 2 * oneDay),
                category = NewsCategory.WEATHER,
                importance = NewsImportance.HIGH,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=102",
                region = "강원"
            ),
            MarketNews(
                id = "fallback_003",
                title = "정부, 어민 소득 안정화 지원책 발표",
                content = "정부가 어업인의 소득 안정화를 위한 종합 지원책을 발표했다. 어업공제 지원 확대와 연료비 보조 등이 주요 내용이다.",
                publishDate = Date(now - 3 * oneDay),
                category = NewsCategory.REGULATION,
                importance = NewsImportance.MEDIUM,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=100",
                region = "서울"
            ),
            MarketNews(
                id = "fallback_004",
                title = "부산 자갈치시장 디지털 전환 본격화",
                content = "부산의 대표 수산시장인 자갈치시장이 디지털 전환을 본격화한다. 온라인 주문 시스템과 배송 서비스 도입이 핵심이다.",
                publishDate = Date(now - 5 * oneDay),
                category = NewsCategory.MARKET,
                importance = NewsImportance.MEDIUM,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=101",
                region = "부산"
            ),
            MarketNews(
                id = "fallback_005",
                title = "제주 넙치 양식업 생산량 역대 최고치",
                content = "제주 지역 넙치 양식업체들이 올해 생산량에서 역대 최고치를 기록할 것으로 전망된다. 우수한 수질과 기술 개발이 주효했다.",
                publishDate = Date(now - 7 * oneDay),
                category = NewsCategory.MARKET,
                importance = NewsImportance.LOW,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=101",
                region = "제주"
            ),
            MarketNews(
                id = "fallback_006",
                title = "인천 연안 새우 양식 스마트팜 기술 확산",
                content = "인천 지역 연안에서 ICT 기술을 활용한 새우 양식 스마트팜이 확산되고 있다. 생산성 향상과 품질 개선 효과가 입증되고 있다.",
                publishDate = Date(now - 10 * oneDay),
                category = NewsCategory.MARKET,
                importance = NewsImportance.LOW,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=105",
                region = "인천"
            ),
            MarketNews(
                id = "fallback_007",
                title = "경남 통영 굴 양식업 호황",
                content = "경남 통영 지역의 굴 양식업이 올해 대풍년을 맞았다. 우수한 수질과 날씨 조건이 맞아떨어져 생산량이 크게 증가했다.",
                publishDate = Date(now - 12 * oneDay),
                category = NewsCategory.MARKET,
                importance = NewsImportance.MEDIUM,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=101",
                region = "경남"
            ),
            MarketNews(
                id = "fallback_008",
                title = "전남 목포 수산물 유통센터 현대화",
                content = "전남 목포시가 수산물 유통센터 현대화 사업을 추진한다. 냉동 저장 시설과 물류 시스템이 대폭 개선될 예정이다.",
                publishDate = Date(now - 15 * oneDay),
                category = NewsCategory.REGULATION,
                importance = NewsImportance.HIGH,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=100",
                region = "전남"
            ),
            MarketNews(
                id = "fallback_009",
                title = "울산 동구 어항 인프라 개선 완료",
                content = "울산 동구 지역 어항의 인프라 개선 사업이 완료됐다. 어선 접안 시설과 하역 장비가 최신 시설로 교체됐다.",
                publishDate = Date(now - 18 * oneDay),
                category = NewsCategory.REGULATION,
                importance = NewsImportance.MEDIUM,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=100",
                region = "울산"
            ),
            MarketNews(
                id = "fallback_010",
                title = "강원 속초 오징어 가격 폭등",
                content = "강원 속초 지역의 오징어 가격이 지난달 대비 40% 급등했다. 어획량 감소와 관광객 증가가 주요 원인으로 분석된다.",
                publishDate = Date(now - 20 * oneDay),
                category = NewsCategory.PRICE,
                importance = NewsImportance.HIGH,
                url = "https://news.naver.com/main/main.naver?mode=LSD&mid=shm&sid1=101",
                region = "강원"
            )
        ).sortedByDescending { it.publishDate }
    }

    /**
     * 가격 알림 설정
     */
    fun setPriceAlert(fishName: String, targetPrice: Double, alertType: AlertType) {
        // 실제 구현 시 알림 서비스 연동
        // 현재는 예시 구현
        println("가격 알림 설정: $fishName, 목표가격: $targetPrice, 타입: $alertType")
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