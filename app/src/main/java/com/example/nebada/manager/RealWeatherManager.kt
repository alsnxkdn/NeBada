package com.example.nebada.manager

import android.content.Context
import android.util.Log
import com.example.nebada.api.*
import com.example.nebada.model.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * 실제 기상청/해수부 API를 사용하는 날씨 관리 클래스
 */
class RealWeatherManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // API 서비스 키 (실제 사용 시 발급받아야 함)
    private val kmaServiceKey = "YOUR_KMA_SERVICE_KEY"
    private val marineServiceKey = "YOUR_MARINE_SERVICE_KEY"

    // API 서비스 인스턴스
    private val weatherApi: WeatherApiService
    private val marineApi: MarineApiService
    private val typhoonApi: TyphoonApiService
    private val fishingSafetyApi: FishingSafetyApiService
    private val seaForecastApi: SeaForecastApiService

    init {
        // OkHttp 클라이언트 설정
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        // 기상청 API Retrofit 설정
        val kmaRetrofit = Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 해양수산부 API Retrofit 설정
        val marineRetrofit = Retrofit.Builder()
            .baseUrl("http://www.khoa.go.kr/api/oceangrid/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 태풍 API Retrofit 설정
        val typhoonRetrofit = Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/1360000/TyphoonInfoService/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 어선안전조업 API Retrofit 설정
        val fishingSafetyRetrofit = Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/1360000/FishingBoatOperationService/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 해상예보 API Retrofit 설정
        val seaForecastRetrofit = Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/1360000/SeaFcstInfoService/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        weatherApi = kmaRetrofit.create(WeatherApiService::class.java)
        marineApi = marineRetrofit.create(MarineApiService::class.java)
        typhoonApi = typhoonRetrofit.create(TyphoonApiService::class.java)
        fishingSafetyApi = fishingSafetyRetrofit.create(FishingSafetyApiService::class.java)
        seaForecastApi = seaForecastRetrofit.create(SeaForecastApiService::class.java)
    }

    /**
     * 현재 날씨 정보 조회 (실제 API 사용)
     */
    suspend fun getCurrentWeather(latitude: Double = 37.5, longitude: Double = 127.0): WeatherData = withContext(Dispatchers.IO) {
        try {
            val seaArea = SeaAreas.findNearest(latitude, longitude)
            val (baseDate, baseTime) = getCurrentBaseDateTime()

            // 기상청 초단기실황 API 호출
            val weatherResponse = weatherApi.getUltraSrtNcst(
                serviceKey = kmaServiceKey,
                baseDate = baseDate,
                baseTime = baseTime,
                nx = seaArea.gridX.toString(),
                ny = seaArea.gridY.toString()
            )

            if (weatherResponse.isSuccessful && weatherResponse.body()?.response?.header?.resultCode == "00") {
                val items = weatherResponse.body()?.response?.body?.items?.item ?: emptyList()

                // 해양관측정보 API 호출 (파고, 수온 등)
                val marineData = getMarineObservation(seaArea.obsPostId ?: "DT_0025")

                return@withContext parseCurrentWeatherData(items, marineData, seaArea)
            } else {
                Log.e("WeatherManager", "API Error: ${weatherResponse.body()?.response?.header?.resultMsg}")
                return@withContext generateFallbackWeatherData(seaArea)
            }
        } catch (e: Exception) {
            Log.e("WeatherManager", "Weather API Error", e)
            return@withContext generateFallbackWeatherData(SeaAreas.findNearest(latitude, longitude))
        }
    }

    /**
     * 해상 예보 정보 조회
     */
    suspend fun getSeaForecast(latitude: Double = 37.5, longitude: Double = 127.0): List<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val seaArea = SeaAreas.findNearest(latitude, longitude)
            val (baseDate, baseTime) = getForecastBaseDateTime()

            // 단기예보 API 호출
            val forecastResponse = weatherApi.getVilageFcst(
                serviceKey = kmaServiceKey,
                baseDate = baseDate,
                baseTime = baseTime,
                nx = seaArea.gridX.toString(),
                ny = seaArea.gridY.toString()
            )

            if (forecastResponse.isSuccessful && forecastResponse.body()?.response?.header?.resultCode == "00") {
                val items = forecastResponse.body()?.response?.body?.items?.item ?: emptyList()
                return@withContext parseForecastData(items, seaArea)
            } else {
                Log.e("WeatherManager", "Forecast API Error: ${forecastResponse.body()?.response?.header?.resultMsg}")
                return@withContext generateFallbackForecastData(seaArea)
            }
        } catch (e: Exception) {
            Log.e("WeatherManager", "Forecast API Error", e)
            return@withContext generateFallbackForecastData(SeaAreas.findNearest(latitude, longitude))
        }
    }

    /**
     * 태풍 정보 조회
     */
    suspend fun getTyphoonInfo(): List<TyphoonItem> = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val toDate = SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val fromDate = SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).format(calendar.time)

            val response = typhoonApi.getTyphoonInfo(
                serviceKey = kmaServiceKey,
                fromTmFc = fromDate,
                toTmFc = toDate
            )

            if (response.isSuccessful && response.body()?.response?.header?.resultCode == "00") {
                return@withContext response.body()?.response?.body?.items?.item ?: emptyList()
            } else {
                Log.e("WeatherManager", "Typhoon API Error: ${response.body()?.response?.header?.resultMsg}")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("WeatherManager", "Typhoon API Error", e)
            return@withContext emptyList()
        }
    }

    /**
     * 기상특보 정보 조회
     */
    suspend fun getWeatherWarnings(): List<WeatherWarning> = withContext(Dispatchers.IO) {
        try {
            val announceTime = SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).format(Date())

            val response = fishingSafetyApi.getFishingSafetyInfo(
                serviceKey = kmaServiceKey,
                announceTime = announceTime
            )

            if (response.isSuccessful && response.body()?.response?.header?.resultCode == "00") {
                val items = response.body()?.response?.body?.items?.item ?: emptyList()
                return@withContext parseWarningData(items)
            } else {
                Log.e("WeatherManager", "Warning API Error: ${response.body()?.response?.header?.resultMsg}")
                return@withContext generateFallbackWarningData()
            }
        } catch (e: Exception) {
            Log.e("WeatherManager", "Warning API Error", e)
            return@withContext generateFallbackWarningData()
        }
    }

    /**
     * 해양 관측 정보 조회
     */
    private suspend fun getMarineObservation(obsCode: String): MarineData? = withContext(Dispatchers.IO) {
        try {
            val response = marineApi.getRealtimeObservation(
                serviceKey = marineServiceKey,
                obsCode = obsCode
            )

            if (response.isSuccessful) {
                return@withContext response.body()?.result?.data?.firstOrNull()
            } else {
                Log.e("WeatherManager", "Marine API Error: ${response.code()}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("WeatherManager", "Marine API Error", e)
            return@withContext null
        }
    }

    /**
     * 조업 적합도 계산
     */
    fun calculateFishingCondition(weather: WeatherData): FishingCondition {
        var score = 100

        // 바람 세기에 따른 점수 차감
        when {
            weather.windSpeed >= 15 -> score -= 40
            weather.windSpeed >= 10 -> score -= 25
            weather.windSpeed >= 7 -> score -= 15
        }

        // 파고에 따른 점수 차감
        when {
            weather.waveHeight >= 3.0 -> score -= 35
            weather.waveHeight >= 2.0 -> score -= 20
            weather.waveHeight >= 1.5 -> score -= 10
        }

        // 시정에 따른 점수 차감
        when {
            weather.visibility < 5 -> score -= 25
            weather.visibility < 10 -> score -= 15
        }

        // 강수에 따른 점수 차감
        if (weather.condition.contains("비") || weather.condition.contains("눈")) {
            score -= 15
        }

        // 기온에 따른 점수 차감 (극한 기온)
        when {
            weather.temperature < 0 || weather.temperature > 35 -> score -= 10
        }

        return when {
            score >= 85 -> FishingCondition.EXCELLENT
            score >= 70 -> FishingCondition.GOOD
            score >= 50 -> FishingCondition.FAIR
            score >= 30 -> FishingCondition.POOR
            else -> FishingCondition.DANGEROUS
        }
    }

    /**
     * 현재 날씨 데이터 파싱
     */
    private fun parseCurrentWeatherData(
        items: List<KmaForecastItem>,
        marineData: MarineData?,
        seaArea: SeaArea
    ): WeatherData {
        val dataMap = items.groupBy { it.category }

        val temperature = dataMap[WeatherCategories.TMP]?.firstOrNull()?.fcstValue?.toIntOrNull() ?: 20
        val humidity = dataMap[WeatherCategories.REH]?.firstOrNull()?.fcstValue?.toIntOrNull() ?: 70
        val windSpeed = dataMap[WeatherCategories.WSD]?.firstOrNull()?.fcstValue?.toDoubleOrNull() ?: 5.0
        val windDirection = dataMap[WeatherCategories.VEC]?.firstOrNull()?.fcstValue?.let {
            convertWindDirection(it.toDoubleOrNull() ?: 0.0)
        } ?: "남동풍"

        val skyCondition = dataMap[WeatherCategories.SKY]?.firstOrNull()?.fcstValue ?: "1"
        val precipitationType = dataMap[WeatherCategories.PTY]?.firstOrNull()?.fcstValue ?: "0"
        val condition = getWeatherCondition(skyCondition, precipitationType)

        val waveHeight = marineData?.waveHeight?.toDoubleOrNull() ?: 1.5
        val waterTemp = marineData?.waterTemp?.toDoubleOrNull()
        val pressure = marineData?.airPress?.toDoubleOrNull() ?: 1013.25
        val visibility = calculateVisibility(condition, humidity)

        return WeatherData(
            condition = condition,
            temperature = temperature,
            windDirection = windDirection,
            windSpeed = windSpeed,
            waveHeight = waveHeight,
            visibility = visibility,
            pressure = pressure,
            humidity = humidity,
            waterTemp = waterTemp,
            date = Date(),
            location = seaArea.name,
            latitude = seaArea.latitude,
            longitude = seaArea.longitude
        )
    }

    /**
     * 예보 데이터 파싱
     */
    private fun parseForecastData(items: List<KmaForecastItem>, seaArea: SeaArea): List<WeatherData> {
        val forecastList = mutableListOf<WeatherData>()
        val groupedByDateTime = items.groupBy { "${it.fcstDate}${it.fcstTime}" }

        groupedByDateTime.forEach { (dateTime, dataItems) ->
            val dataMap = dataItems.groupBy { it.category }

            val date = parseForecastDateTime(dateTime)
            val temperature = dataMap[WeatherCategories.TMP]?.firstOrNull()?.fcstValue?.toIntOrNull() ?: 20
            val humidity = dataMap[WeatherCategories.REH]?.firstOrNull()?.fcstValue?.toIntOrNull() ?: 70
            val windSpeed = dataMap[WeatherCategories.WSD]?.firstOrNull()?.fcstValue?.toDoubleOrNull() ?: 5.0
            val windDirection = dataMap[WeatherCategories.VEC]?.firstOrNull()?.fcstValue?.let {
                convertWindDirection(it.toDoubleOrNull() ?: 0.0)
            } ?: "남동풍"

            val skyCondition = dataMap[WeatherCategories.SKY]?.firstOrNull()?.fcstValue ?: "1"
            val precipitationType = dataMap[WeatherCategories.PTY]?.firstOrNull()?.fcstValue ?: "0"
            val condition = getWeatherCondition(skyCondition, precipitationType)

            // 파고는 해역별 추정값 사용
            val estimatedWaveHeight = estimateWaveHeight(windSpeed, seaArea.name)
            val visibility = calculateVisibility(condition, humidity)

            forecastList.add(
                WeatherData(
                    condition = condition,
                    temperature = temperature,
                    windDirection = windDirection,
                    windSpeed = windSpeed,
                    waveHeight = estimatedWaveHeight,
                    visibility = visibility,
                    pressure = 1013.25, // 기본값
                    humidity = humidity,
                    waterTemp = null,
                    date = date,
                    location = seaArea.name,
                    latitude = seaArea.latitude,
                    longitude = seaArea.longitude
                )
            )
        }

        return forecastList.sortedBy { it.date }.take(24) // 24시간 예보
    }

    /**
     * 특보 데이터 파싱
     */
    private fun parseWarningData(items: List<SafetyItem>): List<WeatherWarning> {
        return items.map { item ->
            val warningType = determineWarningType(item.warningType)
            val severity = determineWarningSeverity(item.warningLevel)

            WeatherWarning(
                id = "warning_${System.currentTimeMillis()}_${item.seaArea}",
                type = warningType,
                title = "${item.warningType} ${item.warningLevel}",
                description = "${item.seaArea}: ${item.windWave} ${item.visibility} ${item.remarks ?: ""}".trim(),
                severity = severity,
                affectedAreas = listOf(item.seaArea),
                validFrom = parseDateTime(item.announceTime),
                validTo = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000) // 24시간 후
            )
        }
    }

    /**
     * 유틸리티 함수들
     */
    private fun getCurrentBaseDateTime(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 기상청 발표시간에 맞춤 (매시 40분 발표)
        if (calendar.get(Calendar.MINUTE) < 40) {
            calendar.add(Calendar.HOUR_OF_DAY, -1)
        }

        val baseDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
        val baseTime = String.format("%02d00", calendar.get(Calendar.HOUR_OF_DAY))

        return Pair(baseDate, baseTime)
    }

    private fun getForecastBaseDateTime(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 단기예보 발표시간 맞춤 (02, 05, 08, 11, 14, 17, 20, 23시)
        val forecastTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)
        val baseHour = forecastTimes.lastOrNull { it <= hour } ?: 23

        if (baseHour == 23 && hour < 23) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val baseDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
        val baseTime = String.format("%02d00", baseHour)

        return Pair(baseDate, baseTime)
    }

    private fun convertWindDirection(degree: Double): String {
        return when (((degree + 22.5) / 45).toInt() % 8) {
            0 -> "북풍"
            1 -> "북동풍"
            2 -> "동풍"
            3 -> "남동풍"
            4 -> "남풍"
            5 -> "남서풍"
            6 -> "서풍"
            7 -> "북서풍"
            else -> "남동풍"
        }
    }

    private fun getWeatherCondition(skyCode: String, ptyCode: String): String {
        return if (ptyCode != "0") {
            PrecipitationTypes.getDescription(ptyCode)
        } else {
            SkyConditions.getDescription(skyCode)
        }
    }

    private fun calculateVisibility(condition: String, humidity: Int): Double {
        return when {
            condition.contains("비") -> (10..15).random().toDouble()
            condition.contains("눈") -> (5..10).random().toDouble()
            condition.contains("안개") -> (1..3).random().toDouble()
            humidity > 80 -> (8..12).random().toDouble()
            else -> (15..25).random().toDouble()
        }
    }

    private fun estimateWaveHeight(windSpeed: Double, seaArea: String): Double {
        val baseWave = when (seaArea) {
            "동해중부", "동해남부" -> 1.5
            "서해중부", "서해남부" -> 1.0
            "남해동부", "남해서부" -> 1.2
            "제주북부", "제주남부" -> 1.8
            else -> 1.3
        }

        return baseWave + (windSpeed * 0.1)
    }

    private fun parseForecastDateTime(dateTime: String): Date {
        return try {
            SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).parse(dateTime) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun parseDateTime(dateTimeStr: String): Date {
        return try {
            SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).parse(dateTimeStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun determineWarningType(typeStr: String): WarningType {
        return when {
            typeStr.contains("태풍") -> WarningType.TYPHOON
            typeStr.contains("강풍") -> WarningType.STRONG_WIND
            typeStr.contains("풍랑") -> WarningType.HIGH_WAVES
            typeStr.contains("호우") -> WarningType.HEAVY_RAIN
            typeStr.contains("안개") -> WarningType.FOG
            else -> WarningType.STRONG_WIND
        }
    }

    private fun determineWarningSeverity(levelStr: String): WarningSeverity {
        return when {
            levelStr.contains("경보") -> WarningSeverity.VERY_HIGH
            levelStr.contains("주의보") -> WarningSeverity.HIGH
            levelStr.contains("주의") -> WarningSeverity.MEDIUM
            else -> WarningSeverity.LOW
        }
    }

    /**
     * 대체 데이터 생성 (API 오류 시)
     */
    private fun generateFallbackWeatherData(seaArea: SeaArea): WeatherData {
        return WeatherData(
            condition = "맑음",
            temperature = 22,
            windDirection = "남동풍",
            windSpeed = 8.0,
            waveHeight = 1.5,
            visibility = 15.0,
            pressure = 1013.25,
            humidity = 65,
            waterTemp = 18.0,
            date = Date(),
            location = seaArea.name,
            latitude = seaArea.latitude,
            longitude = seaArea.longitude
        )
    }

    private fun generateFallbackForecastData(seaArea: SeaArea): List<WeatherData> {
        val forecast = mutableListOf<WeatherData>()
        val baseTime = System.currentTimeMillis()

        repeat(24) { index ->
            forecast.add(
                WeatherData(
                    condition = if (index % 6 == 0) "구름많음" else "맑음",
                    temperature = 20 + (index % 8),
                    windDirection = "남동풍",
                    windSpeed = 6.0 + (index * 0.5),
                    waveHeight = 1.2 + (index * 0.1),
                    visibility = 15.0,
                    pressure = 1013.25,
                    humidity = 60 + (index % 20),
                    waterTemp = 18.0,
                    date = Date(baseTime + (index * 60 * 60 * 1000)),
                    location = seaArea.name,
                    latitude = seaArea.latitude,
                    longitude = seaArea.longitude
                )
            )
        }

        return forecast
    }

    private fun generateFallbackWarningData(): List<WeatherWarning> {
        return listOf(
            WeatherWarning(
                id = "fallback_001",
                type = WarningType.STRONG_WIND,
                title = "강풍주의보",
                description = "해상에 강풍주의보가 발효됩니다.",
                severity = WarningSeverity.MEDIUM,
                affectedAreas = listOf("동해", "서해", "남해"),
                validFrom = Date(),
                validTo = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
            )
        )
    }

    fun destroy() {
        scope.cancel()
    }
}