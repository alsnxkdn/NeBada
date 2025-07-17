package com.example.nebada.model

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * 기상청 단기예보 API 응답 모델
 */
data class KmaForecastResponse(
    @SerializedName("response")
    val response: KmaResponse
)

data class KmaResponse(
    @SerializedName("header")
    val header: KmaHeader,
    @SerializedName("body")
    val body: KmaBody?
)

data class KmaHeader(
    @SerializedName("resultCode")
    val resultCode: String,
    @SerializedName("resultMsg")
    val resultMsg: String
)

data class KmaBody(
    @SerializedName("dataType")
    val dataType: String,
    @SerializedName("items")
    val items: KmaItems,
    @SerializedName("pageNo")
    val pageNo: Int,
    @SerializedName("numOfRows")
    val numOfRows: Int,
    @SerializedName("totalCount")
    val totalCount: Int
)

data class KmaItems(
    @SerializedName("item")
    val item: List<KmaForecastItem>
)

data class KmaForecastItem(
    @SerializedName("baseDate")
    val baseDate: String,        // 발표일자
    @SerializedName("baseTime")
    val baseTime: String,        // 발표시각
    @SerializedName("category")
    val category: String,        // 자료구분문자
    @SerializedName("fcstDate")
    val fcstDate: String,        // 예보일자
    @SerializedName("fcstTime")
    val fcstTime: String,        // 예보시각
    @SerializedName("fcstValue")
    val fcstValue: String,       // 예보값
    @SerializedName("nx")
    val nx: Int,                 // 예보지점 X좌표
    @SerializedName("ny")
    val ny: Int                  // 예보지점 Y좌표
)

/**
 * 해양수산부 해양정보 API 응답 모델
 */
data class MarineInfoResponse(
    @SerializedName("result")
    val result: MarineResult
)

data class MarineResult(
    @SerializedName("meta")
    val meta: MarineMeta,
    @SerializedName("data")
    val data: List<MarineData>
)

data class MarineMeta(
    @SerializedName("totalCount")
    val totalCount: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("count")
    val count: Int
)

data class MarineData(
    @SerializedName("record_time")
    val recordTime: String,      // 관측시간
    @SerializedName("obs_post_name")
    val obsPostName: String,     // 관측소명
    @SerializedName("water_temp")
    val waterTemp: String?,      // 수온
    @SerializedName("air_temp")
    val airTemp: String?,        // 기온
    @SerializedName("wind_dirct")
    val windDirct: String?,      // 풍향
    @SerializedName("wind_speed")
    val windSpeed: String?,      // 풍속
    @SerializedName("wave_height")
    val waveHeight: String?,     // 파고
    @SerializedName("air_press")
    val airPress: String?,       // 기압
    @SerializedName("humidity")
    val humidity: String?        // 습도
)

/**
 * 태풍정보 API 응답 모델
 */
data class TyphoonInfoResponse(
    @SerializedName("response")
    val response: TyphoonResponse
)

data class TyphoonResponse(
    @SerializedName("header")
    val header: KmaHeader,
    @SerializedName("body")
    val body: TyphoonBody?
)

data class TyphoonBody(
    @SerializedName("items")
    val items: TyphoonItems
)

data class TyphoonItems(
    @SerializedName("item")
    val item: List<TyphoonItem>
)

data class TyphoonItem(
    @SerializedName("typSeq")
    val typSeq: String,          // 태풍번호
    @SerializedName("typName")
    val typName: String,         // 태풍명
    @SerializedName("typLat")
    val typLat: String,          // 위도
    @SerializedName("typLon")
    val typLon: String,          // 경도
    @SerializedName("typLoc")
    val typLoc: String,          // 위치
    @SerializedName("typDir")
    val typDir: String,          // 진행방향
    @SerializedName("typSpeed")
    val typSpeed: String,        // 진행속도
    @SerializedName("typPress")
    val typPress: String,        // 중심기압
    @SerializedName("typWs")
    val typWs: String,           // 최대풍속
    @SerializedName("typWsWd")
    val typWsWd: String,         // 강풍반경
    @SerializedName("tmFc")
    val tmFc: String,            // 발표시각
    @SerializedName("tmSeq")
    val tmSeq: String            // 시간구분
)

/**
 * 어선 안전조업 API 응답 모델
 */
data class FishingSafetyResponse(
    @SerializedName("response")
    val response: SafetyResponse
)

data class SafetyResponse(
    @SerializedName("header")
    val header: KmaHeader,
    @SerializedName("body")
    val body: SafetyBody?
)

data class SafetyBody(
    @SerializedName("items")
    val items: SafetyItems
)

data class SafetyItems(
    @SerializedName("item")
    val item: List<SafetyItem>
)

data class SafetyItem(
    @SerializedName("announceTime")
    val announceTime: String,     // 발표시각
    @SerializedName("seaArea")
    val seaArea: String,          // 해역명
    @SerializedName("warningType")
    val warningType: String,      // 특보종류
    @SerializedName("warningLevel")
    val warningLevel: String,     // 특보등급
    @SerializedName("windWave")
    val windWave: String,         // 풍랑정보
    @SerializedName("visibility")
    val visibility: String,       // 시정정보
    @SerializedName("remarks")
    val remarks: String?          // 비고
)

/**
 * 통합 날씨 데이터 모델
 */
data class WeatherData(
    val condition: String,        // 날씨상태
    val temperature: Int,         // 온도
    val windDirection: String,    // 풍향
    val windSpeed: Double,        // 풍속
    val waveHeight: Double,       // 파고
    val visibility: Double,       // 시정
    val pressure: Double,         // 기압
    val humidity: Int,            // 습도
    val waterTemp: Double?,       // 수온
    val date: Date,              // 관측/예보 시간
    val location: String,         // 위치
    val latitude: Double = 0.0,   // 위도
    val longitude: Double = 0.0   // 경도
)

/**
 * 기상특보 정보
 */
data class WeatherWarning(
    val id: String,
    val type: WarningType,
    val title: String,
    val description: String,
    val severity: WarningSeverity,
    val affectedAreas: List<String>,
    val validFrom: Date,
    val validTo: Date,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

enum class WarningType(val displayName: String, val code: String) {
    TYPHOON("태풍", "01"),
    STRONG_WIND("강풍", "02"),
    HIGH_WAVES("풍랑", "03"),
    HEAVY_RAIN("호우", "04"),
    FOG("해무", "05"),
    DRY_WARNING("건조", "06")
}

enum class WarningSeverity(val displayName: String, val color: String) {
    LOW("약함", "#FFEB3B"),
    MEDIUM("보통", "#FF9800"),
    HIGH("강함", "#F44336"),
    VERY_HIGH("매우강함", "#9C27B0")
}

/**
 * 조업 적합도
 */
enum class FishingCondition(val displayName: String, val score: Int, val color: String) {
    EXCELLENT("매우좋음", 90, "#4CAF50"),
    GOOD("좋음", 75, "#8BC34A"),
    FAIR("보통", 60, "#FFEB3B"),
    POOR("나쁨", 40, "#FF9800"),
    DANGEROUS("위험", 20, "#F44336")
}

/**
 * 해역 정보
 */
data class SeaArea(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val gridX: Int,
    val gridY: Int,
    val obsPostId: String? = null    // 관측소 ID
)

/**
 * 예정된 기상 관측소 위치
 */
object SeaAreas {
    val areas = listOf(
        SeaArea("동해중부", 37.7, 129.4, 134, 108, "DT_0025"),
        SeaArea("동해남부", 35.8, 129.4, 134, 91, "DT_0026"),
        SeaArea("서해중부", 36.5, 126.0, 55, 99, "DT_0027"),
        SeaArea("서해남부", 34.8, 125.4, 51, 85, "DT_0028"),
        SeaArea("남해동부", 35.0, 128.7, 127, 87, "DT_0029"),
        SeaArea("남해서부", 34.3, 126.8, 73, 82, "DT_0030"),
        SeaArea("제주북부", 33.8, 126.5, 69, 76, "DT_0031"),
        SeaArea("제주남부", 33.2, 126.5, 69, 70, "DT_0032")
    )

    fun findByName(name: String): SeaArea? {
        return areas.find { it.name.contains(name) || name.contains(it.name) }
    }

    fun findNearest(latitude: Double, longitude: Double): SeaArea {
        return areas.minByOrNull { area ->
            kotlin.math.sqrt(
                kotlin.math.pow(area.latitude - latitude, 2.0) +
                        kotlin.math.pow(area.longitude - longitude, 2.0)
            )
        } ?: areas.first()
    }
}

/**
 * 기상 카테고리 코드 정의
 */
object WeatherCategories {
    const val POP = "POP"     // 강수확률
    const val PTY = "PTY"     // 강수형태
    const val PCP = "PCP"     // 1시간 강수량
    const val REH = "REH"     // 습도
    const val SNO = "SNO"     // 1시간 신적설
    const val SKY = "SKY"     // 하늘상태
    const val TMP = "TMP"     // 1시간 기온
    const val TMN = "TMN"     // 일 최저기온
    const val TMX = "TMX"     // 일 최고기온
    const val UUU = "UUU"     // 풍속(동서성분)
    const val VVV = "VVV"     // 풍속(남북성분)
    const val WAV = "WAV"     // 파고
    const val VEC = "VEC"     // 풍향
    const val WSD = "WSD"     // 풍속
}

/**
 * 하늘 상태 코드
 */
object SkyConditions {
    const val CLEAR = "1"         // 맑음
    const val PARTLY_CLOUDY = "3" // 구름많음
    const val CLOUDY = "4"        // 흐림

    fun getDescription(code: String): String {
        return when (code) {
            CLEAR -> "맑음"
            PARTLY_CLOUDY -> "구름많음"
            CLOUDY -> "흐림"
            else -> "알 수 없음"
        }
    }
}

/**
 * 강수 형태 코드
 */
object PrecipitationTypes {
    const val NONE = "0"          // 없음
    const val RAIN = "1"          // 비
    const val RAIN_SNOW = "2"     // 비/눈
    const val SNOW = "3"          // 눈
    const val SHOWER = "4"        // 소나기

    fun getDescription(code: String): String {
        return when (code) {
            NONE -> "없음"
            RAIN -> "비"
            RAIN_SNOW -> "비/눈"
            SNOW -> "눈"
            SHOWER -> "소나기"
            else -> "알 수 없음"
        }
    }
}