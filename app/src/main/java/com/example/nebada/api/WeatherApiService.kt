package com.example.nebada.api

import com.example.nebada.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 기상청 단기예보 API 서비스
 */
interface WeatherApiService {

    /**
     * 초단기실황조회
     */
    @GET("getUltraSrtNcst")
    suspend fun getUltraSrtNcst(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 1000,
        @Query("dataType") dataType: String = "JSON",
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: String,
        @Query("ny") ny: String
    ): Response<KmaForecastResponse>

    /**
     * 초단기예보조회
     */
    @GET("getUltraSrtFcst")
    suspend fun getUltraSrtFcst(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 1000,
        @Query("dataType") dataType: String = "JSON",
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: String,
        @Query("ny") ny: String
    ): Response<KmaForecastResponse>

    /**
     * 단기예보조회
     */
    @GET("getVilageFcst")
    suspend fun getVilageFcst(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 1000,
        @Query("dataType") dataType: String = "JSON",
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: String,
        @Query("ny") ny: String
    ): Response<KmaForecastResponse>
}

/**
 * 해양수산부 해양관측정보 API 서비스
 */
interface MarineApiService {

    /**
     * 실시간 해양관측정보
     */
    @GET("ObsReal")
    suspend fun getRealtimeObservation(
        @Query("ServiceKey") serviceKey: String,
        @Query("PageNo") pageNo: Int = 1,
        @Query("NumOfRows") numOfRows: Int = 100,
        @Query("ResultType") resultType: String = "json",
        @Query("ObsCode") obsCode: String
    ): Response<MarineInfoResponse>

    /**
     * 시간별 해양관측정보
     */
    @GET("ObsHour")
    suspend fun getHourlyObservation(
        @Query("ServiceKey") serviceKey: String,
        @Query("PageNo") pageNo: Int = 1,
        @Query("NumOfRows") numOfRows: Int = 100,
        @Query("ResultType") resultType: String = "json",
        @Query("ObsCode") obsCode: String,
        @Query("Date") date: String
    ): Response<MarineInfoResponse>
}

/**
 * 기상청 태풍정보 API 서비스
 */
interface TyphoonApiService {

    /**
     * 태풍정보 조회
     */
    @GET("getTyphoonInfo")
    suspend fun getTyphoonInfo(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "JSON",
        @Query("fromTmFc") fromTmFc: String,
        @Query("toTmFc") toTmFc: String
    ): Response<TyphoonInfoResponse>

    /**
     * 태풍예보 조회
     */
    @GET("getTyphoonFcst")
    suspend fun getTyphoonForecast(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "JSON",
        @Query("typSeq") typSeq: String
    ): Response<TyphoonInfoResponse>
}

/**
 * 기상청 어선안전조업 특보 API 서비스
 */
interface FishingSafetyApiService {

    /**
     * 어선안전조업 특보 조회
     */
    @GET("getFishingInfo")
    suspend fun getFishingSafetyInfo(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "JSON",
        @Query("announceTime") announceTime: String
    ): Response<FishingSafetyResponse>

    /**
     * 특보구역 조회
     */
    @GET("getWarningArea")
    suspend fun getWarningArea(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "JSON"
    ): Response<FishingSafetyResponse>
}

/**
 * 기상청 해상예보 API 서비스
 */
interface SeaForecastApiService {

    /**
     * 해상예보 조회
     */
    @GET("getSeaFcst")
    suspend fun getSeaForecast(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "JSON",
        @Query("regId") regId: String,
        @Query("tmFc") tmFc: String
    ): Response<KmaForecastResponse>

    /**
     * 중기해상예보 조회
     */
    @GET("getMidSeaFcst")
    suspend fun getMidSeaForecast(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("dataType") dataType: String = "JSON",
        @Query("regId") regId: String,
        @Query("tmFc") tmFc: String
    ): Response<KmaForecastResponse>
}