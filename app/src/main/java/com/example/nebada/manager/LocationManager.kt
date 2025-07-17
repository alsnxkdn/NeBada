package com.example.nebada.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

class LocationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private val geocoder = Geocoder(context, Locale.getDefault())

    /**
     * 현재 위치 권한이 있는지 확인
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * GPS가 활성화되어 있는지 확인
     */
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
    }

    /**
     * 현재 위치를 가져와서 지역명으로 변환
     */
    suspend fun getCurrentRegion(callback: (String?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        try {
            // 마지막으로 알려진 위치 가져오기
            val location = getLastKnownLocation()
            if (location != null) {
                val region = getRegionFromLocation(location.latitude, location.longitude)
                callback(region)
            } else {
                // 실시간 위치 요청
                requestCurrentLocation { lat, lng ->
                    if (lat != null && lng != null) {
                        // 메인 스레드에서 실행
                        CoroutineScope(Dispatchers.Main).launch {
                            val region = getRegionFromLocation(lat, lng)
                            callback(region)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
        } catch (e: Exception) {
            callback(null)
        }
    }

    /**
     * 마지막으로 알려진 위치 가져오기
     */
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            val gpsLocation = locationManager.getLastKnownLocation(AndroidLocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(AndroidLocationManager.NETWORK_PROVIDER)

            when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 실시간 위치 요청
     */
    private fun requestCurrentLocation(callback: (Double?, Double?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null, null)
            return
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                callback(location.latitude, location.longitude)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {
                callback(null, null)
            }
        }

        try {
            when {
                locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) -> {
                    locationManager.requestLocationUpdates(
                        AndroidLocationManager.GPS_PROVIDER,
                        0L,
                        0f,
                        locationListener
                    )
                }
                locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER) -> {
                    locationManager.requestLocationUpdates(
                        AndroidLocationManager.NETWORK_PROVIDER,
                        0L,
                        0f,
                        locationListener
                    )
                }
                else -> {
                    callback(null, null)
                }
            }

            // 10초 후에도 위치를 못 가져오면 취소
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                locationManager.removeUpdates(locationListener)
                callback(null, null)
            }, 10000)

        } catch (e: Exception) {
            callback(null, null)
        }
    }

    /**
     * 위도, 경도를 지역명으로 변환
     */
    private suspend fun getRegionFromLocation(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]

                    // 시/도 정보 추출
                    val adminArea = address.adminArea // 시/도
                    val locality = address.locality // 시/군/구

                    // 지역명 매핑
                    return@withContext when {
                        adminArea?.contains("서울") == true -> "서울"
                        adminArea?.contains("부산") == true -> "부산"
                        adminArea?.contains("대구") == true -> "대구"
                        adminArea?.contains("인천") == true -> "인천"
                        adminArea?.contains("광주") == true -> "광주"
                        adminArea?.contains("대전") == true -> "대전"
                        adminArea?.contains("울산") == true -> "울산"
                        adminArea?.contains("세종") == true -> "세종"
                        adminArea?.contains("경기") == true -> "경기"
                        adminArea?.contains("강원") == true -> "강원"
                        adminArea?.contains("충청북도") == true || adminArea?.contains("충북") == true -> "충북"
                        adminArea?.contains("충청남도") == true || adminArea?.contains("충남") == true -> "충남"
                        adminArea?.contains("전라북도") == true || adminArea?.contains("전북") == true -> "전북"
                        adminArea?.contains("전라남도") == true || adminArea?.contains("전남") == true -> "전남"
                        adminArea?.contains("경상북도") == true || adminArea?.contains("경북") == true -> "경북"
                        adminArea?.contains("경상남도") == true || adminArea?.contains("경남") == true -> "경남"
                        adminArea?.contains("제주") == true -> "제주"
                        else -> locality ?: "전국"
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 특정 지역이 현재 위치 근처인지 확인
     */
    suspend fun isNearRegion(targetRegion: String): Boolean {
        return try {
            val currentRegion = getCurrentRegionSync()
            currentRegion == targetRegion
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 동기적으로 현재 지역 가져오기
     */
    private suspend fun getCurrentRegionSync(): String? {
        return withContext(Dispatchers.IO) {
            val location = getLastKnownLocation()
            if (location != null) {
                getRegionFromLocation(location.latitude, location.longitude)
            } else {
                null
            }
        }
    }
    /**
     * 상세한 위치 정보 가져오기
     */
    suspend fun getCurrentLocationDetails(callback: (LocationDetails?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        try {
            // 마지막으로 알려진 위치 가져오기
            val location = getLastKnownLocation()
            if (location != null) {
                val address = getAddressFromLocation(location.latitude, location.longitude)
                val locationDetails = LocationDetails(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address ?: "위치 정보 없음",
                    accuracy = location.accuracy
                )
                callback(locationDetails)
            } else {
                // 실시간 위치 요청
                requestCurrentLocationDetails { lat, lng, accuracy ->
                    if (lat != null && lng != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val address = getAddressFromLocation(lat, lng)
                            val locationDetails = LocationDetails(
                                latitude = lat,
                                longitude = lng,
                                address = address ?: "위치 정보 없음",
                                accuracy = accuracy ?: 0f
                            )
                            callback(locationDetails)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
        } catch (e: Exception) {
            callback(null)
        }
    }
    /**
     * 실시간 상세 위치 요청
     */
    private fun requestCurrentLocationDetails(callback: (Double?, Double?, Float?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null, null, null)
            return
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                callback(location.latitude, location.longitude, location.accuracy)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {
                callback(null, null, null)
            }
        }

        try {
            when {
                locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) -> {
                    locationManager.requestLocationUpdates(
                        AndroidLocationManager.GPS_PROVIDER,
                        0L,
                        0f,
                        locationListener
                    )
                }
                locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER) -> {
                    locationManager.requestLocationUpdates(
                        AndroidLocationManager.NETWORK_PROVIDER,
                        0L,
                        0f,
                        locationListener
                    )
                }
                else -> {
                    callback(null, null, null)
                }
            }

            // 10초 후에도 위치를 못 가져오면 취소
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                locationManager.removeUpdates(locationListener)
                callback(null, null, null)
            }, 10000)

        } catch (e: Exception) {
            callback(null, null, null)
        }
    }

    /**
     * 위도, 경도를 상세 주소로 변환
     */
    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]

                    // 상세 주소 조합
                    val addressParts = mutableListOf<String>()

                    // 시/도
                    address.adminArea?.let { addressParts.add(it) }

                    // 시/군/구
                    address.locality?.let {
                        if (!addressParts.contains(it)) {
                            addressParts.add(it)
                        }
                    }

                    // 구/읍/면
                    address.subLocality?.let {
                        if (!addressParts.contains(it)) {
                            addressParts.add(it)
                        }
                    }

                    // 도로명 또는 지번 주소
                    address.thoroughfare?.let {
                        addressParts.add(it)
                    } ?: address.featureName?.let {
                        if (it != address.subLocality) {
                            addressParts.add(it)
                        }
                    }

                    // 건물번호
                    address.subThoroughfare?.let {
                        addressParts.add(it)
                    }

                    // 어획지역 특화: 바다 관련 키워드 추가
                    val fullAddress = addressParts.joinToString(" ")

                    return@withContext when {
                        fullAddress.contains("해안") || fullAddress.contains("항") || fullAddress.contains("포구") -> {
                            "$fullAddress 앞바다"
                        }
                        addressParts.size >= 2 -> {
                            "${addressParts.take(2).joinToString(" ")} 앞바다"
                        }
                        else -> fullAddress.ifEmpty { "현재 위치" }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 상세 위치 정보 데이터 클래스
     */
    data class LocationDetails(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val accuracy: Float
    )
}