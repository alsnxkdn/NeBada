package com.example.nebada.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 위치 기반 날씨 정보 관리 클래스
 */
class LocationWeatherManager(
    private val context: Context,
    private val fragment: Fragment? = null
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        // Fragment가 제공된 경우 권한 요청 런처 설정
        fragment?.let { frag ->
            permissionLauncher = frag.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                if (fineLocationGranted || coarseLocationGranted) {
                    // 권한이 승인되면 위치 정보 갱신
                    // 이는 일반적으로 콜백을 통해 처리됩니다
                }
            }
        }
    }

    /**
     * 위치 권한 확인
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 위치 권한 요청
     */
    fun requestLocationPermission() {
        permissionLauncher?.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * 현재 위치 가져오기
     */
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resumeWithException(SecurityException("위치 권한이 필요합니다"))
            return@suspendCancellableCoroutine
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000L // 10초
            ).apply {
                setMaxUpdateDelayMillis(5000L) // 5초 최대 지연
                setMaxUpdates(1) // 한 번만 업데이트
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    if (location != null && continuation.isActive) {
                        continuation.resume(location)
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    super.onLocationAvailability(locationAvailability)
                    if (!locationAvailability.isLocationAvailable && continuation.isActive) {
                        continuation.resumeWithException(Exception("위치 정보를 사용할 수 없습니다"))
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }

            // 마지막 알려진 위치 먼저 시도
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                if (lastLocation != null && continuation.isActive) {
                    // 마지막 위치가 최근 것이면 사용 (5분 이내)
                    val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000
                    if (lastLocation.time > fiveMinutesAgo) {
                        continuation.resume(lastLocation)
                        return@addOnSuccessListener
                    }
                }

                // 새로운 위치 요청
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            }.addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }

            // 취소 처리
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }

        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * 위치 기반 최적 해역 찾기
     */
    fun findNearestSeaArea(location: Location): com.example.nebada.model.SeaArea {
        return com.example.nebada.model.SeaAreas.findNearest(
            location.latitude,
            location.longitude
        )
    }

    /**
     * 두 지점 간 거리 계산 (km)
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * 위치가 해상인지 확인
     */
    fun isMarineLocation(latitude: Double, longitude: Double): Boolean {
        // 한반도 주변 해역 범위 확인
        val koreaSeaBounds = listOf(
            // 동해
            Triple(34.0, 40.0, 128.0 to 132.0),
            // 서해
            Triple(34.0, 39.0, 124.0 to 127.0),
            // 남해
            Triple(32.0, 36.0, 126.0 to 130.0)
        )

        return koreaSeaBounds.any { (minLat, maxLat, lonRange) ->
            latitude >= minLat && latitude <= maxLat &&
                    longitude >= lonRange.first && longitude <= lonRange.second
        }
    }

    /**
     * 격자 좌표 변환 (기상청 API용)
     */
    fun convertToGrid(latitude: Double, longitude: Double): Pair<Int, Int> {
        // 기상청 격자 변환 공식
        val RE = 6371.00877 // 지구 반경(km)
        val GRID = 5.0 // 격자 간격(km)
        val SLAT1 = 30.0 // 투영 위도1(degree)
        val SLAT2 = 60.0 // 투영 위도2(degree)
        val OLON = 126.0 // 기준점 경도(degree)
        val OLAT = 38.0 // 기준점 위도(degree)
        val XO = 43 // 기준점 X좌표(GRID)
        val YO = 136 // 기준점 Y좌표(GRID)

        val DEGRAD = Math.PI / 180.0
        val RADDEG = 180.0 / Math.PI

        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
        var ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
        ro = re * sf / Math.pow(ro, sn)

        var ra = Math.tan(Math.PI * 0.25 + latitude * DEGRAD * 0.5)
        ra = re * sf / Math.pow(ra, sn)
        val theta = longitude * DEGRAD - olon
        val x = Math.floor(ra * Math.sin(theta * sn) + XO + 0.5)
        val y = Math.floor(ro - ra * Math.cos(theta * sn) + YO + 0.5)

        return Pair(x.toInt(), y.toInt())
    }
}