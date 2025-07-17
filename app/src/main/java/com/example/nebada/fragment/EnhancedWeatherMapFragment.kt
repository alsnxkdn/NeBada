package com.example.nebada.fragment

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nebada.databinding.FragmentWeatherMapBinding
import com.example.nebada.manager.RealWeatherManager
import com.example.nebada.manager.LocationWeatherManager
import com.example.nebada.model.WeatherData
import com.example.nebada.model.SeaAreas
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EnhancedWeatherMapFragment : Fragment() {

    private var _binding: FragmentWeatherMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var weatherManager: RealWeatherManager
    private lateinit var locationManager: LocationWeatherManager
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    private var currentLocation: Location? = null
    private var currentWeatherData: WeatherData? = null
    private var seaAreaWeatherData: Map<String, WeatherData> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weatherManager = RealWeatherManager(requireContext())
        locationManager = LocationWeatherManager(requireContext(), this)

        setupWebView()
        setupMapControls()
        checkLocationAndLoadWeather()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewMap.apply {
            webViewClient = WebViewClient()
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }

            // JavaScript 인터페이스 추가
            addJavascriptInterface(WebAppInterface(), "Android")
        }
    }

    private fun checkLocationAndLoadWeather() {
        if (locationManager.hasLocationPermission()) {
            loadLocationBasedWeather()
        } else {
            // 권한이 없으면 기본 위치로 날씨 로드
            loadDefaultWeatherData()

            // 권한 요청
            locationManager.requestLocationPermission()
        }
    }

    private fun loadLocationBasedWeather() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // 현재 위치 가져오기
                currentLocation = locationManager.getCurrentLocation()

                currentLocation?.let { location ->
                    // 현재 위치 기반 날씨 정보 로드
                    currentWeatherData = weatherManager.getCurrentWeather(
                        location.latitude,
                        location.longitude
                    )

                    updateWeatherDisplay(currentWeatherData!!)

                    // 가장 가까운 해역 찾기
                    val nearestSeaArea = locationManager.findNearestSeaArea(location)

                    Toast.makeText(
                        requireContext(),
                        "현재 위치: ${nearestSeaArea.name} 인근",
                        Toast.LENGTH_SHORT
                    ).show()
                } ?: run {
                    // GPS 위치를 가져올 수 없으면 기본 위치 사용
                    loadDefaultWeatherData()
                }

                // 모든 해역의 날씨 데이터 로드
                loadAllSeaAreaWeather()

                // 지도 로드
                loadEnhancedWeatherMap()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "위치 기반 날씨 로드 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                loadDefaultWeatherData()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadDefaultWeatherData() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // 서울/인천 기준 날씨 로드
                currentWeatherData = weatherManager.getCurrentWeather()
                updateWeatherDisplay(currentWeatherData!!)

                loadAllSeaAreaWeather()
                loadEnhancedWeatherMap()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "기본 날씨 로드 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun loadAllSeaAreaWeather() {
        val weatherDataMap = mutableMapOf<String, WeatherData>()

        SeaAreas.areas.forEach { area ->
            try {
                val areaWeather = weatherManager.getCurrentWeather(area.latitude, area.longitude)
                weatherDataMap[area.name] = areaWeather
            } catch (e: Exception) {
                // 개별 해역 실패는 무시
            }
        }

        seaAreaWeatherData = weatherDataMap
    }

    private fun loadEnhancedWeatherMap() {
        val currentLat = currentLocation?.latitude ?: 37.5
        val currentLon = currentLocation?.longitude ?: 127.0

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>실시간 해상 기상정보</title>
                <style>
                    body { 
                        margin: 0; 
                        padding: 0; 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                        background: #f0f8ff;
                        overflow: hidden;
                    }
                    .map-container { 
                        position: relative; 
                        width: 100%; 
                        height: 100vh; 
                        overflow: hidden;
                    }
                    .weather-overlay { 
                        position: absolute; 
                        top: 10px; 
                        left: 10px; 
                        right: 10px;
                        background: rgba(255,255,255,0.95);
                        padding: 12px;
                        border-radius: 12px;
                        z-index: 1000;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                    }
                    .legend {
                        position: absolute;
                        bottom: 60px;
                        left: 10px;
                        background: rgba(255,255,255,0.95);
                        padding: 12px;
                        border-radius: 12px;
                        font-size: 11px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                        max-width: 200px;
                        z-index: 1000;
                    }
                    .weather-layer {
                        position: absolute;
                        width: 100%;
                        height: 100%;
                        pointer-events: none;
                        opacity: 0.7;
                        transition: opacity 0.3s ease;
                    }
                    .layer-precipitation { 
                        background: radial-gradient(circle at 60% 40%, rgba(0,100,255,0.4) 20%, transparent 50%),
                                   radial-gradient(circle at 80% 70%, rgba(0,150,255,0.3) 15%, transparent 40%);
                        display: none;
                    }
                    .layer-wind { display: none; }
                    .layer-waves { display: none; }
                    .layer-temperature { display: none; }
                    
                    .sea-area {
                        position: absolute;
                        background: rgba(255,255,255,0.9);
                        border: 2px solid #0066cc;
                        border-radius: 8px;
                        padding: 8px;
                        font-size: 11px;
                        text-align: center;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        min-width: 80px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.15);
                    }
                    .sea-area:hover {
                        background: rgba(0,102,204,0.9);
                        color: white;
                        transform: scale(1.1);
                        z-index: 100;
                    }
                    .sea-area .area-name {
                        font-weight: bold;
                        margin-bottom: 4px;
                    }
                    .sea-area .weather-info {
                        font-size: 10px;
                        line-height: 1.2;
                    }
                    
                    .current-location {
                        position: absolute;
                        width: 20px;
                        height: 20px;
                        background: #ff4444;
                        border: 3px solid white;
                        border-radius: 50%;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                        z-index: 200;
                        animation: pulse 2s infinite;
                    }
                    @keyframes pulse {
                        0% { transform: scale(1); opacity: 1; }
                        50% { transform: scale(1.2); opacity: 0.7; }
                        100% { transform: scale(1); opacity: 1; }
                    }
                    
                    .typhoon-marker {
                        position: absolute;
                        width: 40px;
                        height: 40px;
                        border: 3px solid #ff0000;
                        border-radius: 50%;
                        background: rgba(255,0,0,0.1);
                        border-style: dashed;
                        animation: typhoon-spin 3s linear infinite;
                        z-index: 150;
                    }
                    @keyframes typhoon-spin {
                        from { transform: rotate(0deg); }
                        to { transform: rotate(360deg); }
                    }
                    
                    .warning-indicator {
                        position: absolute;
                        top: -5px;
                        right: -5px;
                        background: #ff4444;
                        color: white;
                        border-radius: 50%;
                        width: 18px;
                        height: 18px;
                        font-size: 10px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        animation: warning-blink 1s ease-in-out infinite alternate;
                        z-index: 101;
                    }
                    @keyframes warning-blink {
                        from { opacity: 1; }
                        to { opacity: 0.5; }
                    }
                    
                    .wind-arrow {
                        position: absolute;
                        z-index: 120;
                        pointer-events: none;
                    }
                    .wind-arrow line {
                        stroke: #0066cc;
                        stroke-width: 2;
                        marker-end: url(#arrowhead);
                    }
                    .wind-arrow text {
                        font-size: 8px;
                        fill: #0066cc;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="map-container">
                    <!-- 상단 정보 오버레이 -->
                    <div class="weather-overlay">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <h3 style="margin: 0; color: #0066cc;">🌊 실시간 해상 기상정보</h3>
                            <div style="font-size: 11px; color: #666; text-align: right;">
                                <div>기상청 · 해수부 제공</div>
                                <div id="update-time">${dateFormat.format(Date())}</div>
                                <div id="location-info" style="color: #0066cc; font-weight: bold;">
                                    ${if (currentLocation != null) "GPS 위치 기반" else "기본 지역 기준"}
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <!-- 한반도 해상 지도 SVG -->
                    <svg width="100%" height="100%" viewBox="0 0 400 600" style="background: linear-gradient(to bottom, #87CEEB 0%, #e6f3ff 100%);">
                        <!-- 바다 배경 -->
                        <rect width="400" height="600" fill="#e6f3ff"/>
                        
                        <!-- 한반도 본토 -->
                        <path d="M120 80 Q180 70 240 90 L280 130 Q290 180 270 230 L250 280 Q240 330 220 380 L190 430 Q170 450 150 430 L100 380 Q80 330 90 280 L100 230 Q110 180 120 130 Z" 
                              fill="#f5f5f5" stroke="#999" stroke-width="2"/>
                        
                        <!-- 제주도 -->
                        <ellipse cx="150" cy="500" rx="25" ry="15" fill="#f5f5f5" stroke="#999" stroke-width="1"/>
                        
                        <!-- 울릉도 -->
                        <circle cx="320" cy="150" r="8" fill="#f5f5f5" stroke="#999" stroke-width="1"/>
                        
                        <!-- 독도 -->
                        <circle cx="340" cy="140" r="3" fill="#f5f5f5" stroke="#999" stroke-width="1"/>
                        
                        <!-- 해역 구분선 -->
                        <line x1="200" y1="50" x2="200" y2="200" stroke="#ccc" stroke-width="1" stroke-dasharray="5,5"/>
                        <line x1="50" y1="250" x2="350" y2="250" stroke="#ccc" stroke-width="1" stroke-dasharray="5,5"/>
                        <line x1="200" y1="350" x2="200" y2="550" stroke="#ccc" stroke-width="1" stroke-dasharray="5,5"/>
                        
                        <!-- 날씨 레이어들 -->
                        <g class="weather-layer layer-precipitation" id="precipitation-layer">
                            <ellipse cx="180" cy="180" rx="40" ry="60" fill="rgba(0,100,255,0.4)"/>
                            <ellipse cx="250" cy="300" rx="35" ry="45" fill="rgba(0,150,255,0.3)"/>
                            <text x="170" y="190" font-size="10" fill="white" font-weight="bold">강수</text>
                        </g>
                        
                        <g class="weather-layer layer-wind" id="wind-layer">
                            <!-- 바람 벡터 화살표들 -->
                            <g class="wind-arrow" transform="translate(120,120)">
                                <line x1="0" y1="0" x2="20" y2="-10" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="25" y="-5" font-size="8" fill="#0066cc">12m/s</text>
                            </g>
                            <g class="wind-arrow" transform="translate(280,120)">
                                <line x1="0" y1="0" x2="15" y2="-15" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="18" y="-10" font-size="8" fill="#0066cc">15m/s</text>
                            </g>
                            <g class="wind-arrow" transform="translate(200,350)">
                                <line x1="0" y1="0" x2="25" y2="0" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="28" y="5" font-size="8" fill="#0066cc">8m/s</text>
                            </g>
                        </g>
                        
                        <g class="weather-layer layer-waves" id="wave-layer">
                            <!-- 파고 패턴 -->
                            <pattern id="wavePattern" patternUnits="userSpaceOnUse" width="20" height="20">
                                <path d="M0,10 Q5,5 10,10 Q15,15 20,10" stroke="#0099cc" fill="none" stroke-width="1"/>
                            </pattern>
                            <rect x="50" y="100" width="100" height="80" fill="url(#wavePattern)" opacity="0.6"/>
                            <rect x="250" y="200" width="120" height="100" fill="url(#wavePattern)" opacity="0.6"/>
                        </g>
                        
                        <g class="weather-layer layer-temperature" id="temperature-layer">
                            <!-- 수온 그라디언트 -->
                            <defs>
                                <linearGradient id="tempGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                                    <stop offset="0%" style="stop-color:#ff6b6b;stop-opacity:0.4" />
                                    <stop offset="50%" style="stop-color:#ffd93d;stop-opacity:0.4" />
                                    <stop offset="100%" style="stop-color:#6bcf7f;stop-opacity:0.4" />
                                </linearGradient>
                            </defs>
                            <rect x="0" y="0" width="400" height="600" fill="url(#tempGradient)"/>
                        </g>
                        
                        <!-- 화살표 마커 정의 -->
                        <defs>
                            <marker id="arrowhead" markerWidth="10" markerHeight="7" 
                                    refX="9" refY="3.5" orient="auto">
                                <polygon points="0 0, 10 3.5, 0 7" fill="#0066cc"/>
                            </marker>
                        </defs>
                    </svg>
                    
                    <!-- 현재 위치 표시 -->
                    <div class="current-location" id="current-location" style="display: none;"></div>
                    
                    <!-- 해역별 정보 표시 -->
                    <div class="sea-area" style="top: 100px; left: 60px;" onclick="showAreaInfo('동해북부')" data-area="동해북부">
                        <div class="area-name">동해북부</div>
                        <div class="weather-info" id="weather-donghae-north">
                            <div>로딩중...</div>
                        </div>
                    </div>
                    
                    <div class="sea-area" style="top: 200px; left: 300px;" onclick="showAreaInfo('동해중부')" data-area="동해중부">
                        <div class="area-name">동해중부</div>
                        <div class="weather-info" id="weather-donghae-center">
                            <div>로딩중...</div>
                        </div>
                    </div>
                    
                    <div class="sea-area" style="top: 320px; left: 290px;" onclick="showAreaInfo('동해남부')" data-area="동해남부">
                        <div class="area-name">동해남부</div>
                        <div class="weather-info" id="weather-donghae-south">
                            <div>로딩중...</div>
                        </div>
                    </div>
                    
                    <div class="sea-area" style="top: 180px; left: 30px;" onclick="showAreaInfo('서해중부')" data-area="서해중부">
                        <div class="area-name">서해중부</div>
                        <div class="weather-info" id="weather-seohae-center">
                            <div>로딩중...</div>
                        </div>
                    </div>
                    
                    <div class="sea-area" style="top: 300px; left: 50px;" onclick="showAreaInfo('서해남부')" data-area="서해남부">
                        <div class="area-name">서해남부</div>
                        <div class="weather-info" id="weather-seohae-south">
                            <div>로딩중...</div>
                        </div>
                    </div>
                    
                    <div class="sea-area" style="top: 380px; left: 200px;" onclick="showAreaInfo('남해동부')" data-area="남해동부">
                        <div class="area-name">남해동부</div>
                        <div class="weather-info" id="weather-namhae-east">
                            <div>로딩중...</div>
                        </div>
                    </div>
                    
                    <div class="sea-area" style="top: 420px; left: 120px;" onclick="showAreaInfo('남해서부')" data-area="남해서부">
                        <div class="area-name">남해서부</div>
                        <div class="weather-info" id="weather-namhae-west">
                            <div>로딩중...</div>
                        </div>
                    </div>
                    
                    <div class="sea-area" style="top: 480px; left: 120px;" onclick="showAreaInfo('제주북부')" data-area="제주북부">
                        <div class="area-name">제주북부</div>
                        <div class="weather-info" id="weather-jeju-north">
                            <div>로딩중...</div>
                        </div>
                        <div class="warning-indicator" style="display: none;" id="warning-jeju">!</div>
                    </div>
                    
                    <!-- 태풍 표시 (있는 경우) -->
                    <div class="typhoon-marker" style="display: none;" id="typhoon-marker">
                        <div style="position: absolute; top: -30px; left: -20px; font-size: 10px; font-weight: bold; color: #ff0000; white-space: nowrap;">
                            <div id="typhoon-name">태풍 정보</div>
                        </div>
                    </div>
                    
                    <!-- 범례 -->
                    <div class="legend">
                        <div style="font-weight: bold; margin-bottom: 8px; color: #0066cc;">범례</div>
                        <div style="margin: 3px 0; display: flex; align-items: center;">
                            <span style="display: inline-block; width: 15px; height: 10px; background: rgba(0,100,255,0.4); margin-right: 5px; border-radius: 2px;"></span>
                            강수 예상 지역
                        </div>
                        <div style="margin: 3px 0; display: flex; align-items: center;">
                            <span style="display: inline-block; width: 15px; height: 2px; background: #0066cc; margin-right: 5px;"></span>
                            바람 방향/세기
                        </div>
                        <div style="margin: 3px 0; display: flex; align-items: center;">
                            <span style="display: inline-block; width: 15px; height: 10px; border: 2px dashed #ff0000; margin-right: 5px; border-radius: 50%;"></span>
                            태풍/저기압
                        </div>
                        <div style="margin: 3px 0; display: flex; align-items: center;">
                            <span style="color: #0066cc; margin-right: 5px;">📊</span>
                            해역별 정보
                        </div>
                        <div style="margin: 3px 0; display: flex; align-items: center;">
                            <span style="color: #ff4444; margin-right: 5px;">!</span>
                            기상특보 발효
                        </div>
                        <div style="margin: 3px 0; display: flex; align-items: center;">
                            <span style="display: inline-block; width: 12px; height: 12px; background: #ff4444; border: 2px solid white; border-radius: 50%; margin-right: 5px;"></span>
                            현재 위치
                        </div>
                    </div>
                </div>
                
                <script>
                    let activeLayerss = new Set();
                    
                    // 레이어 토글 함수
                    function toggleLayer(layerName) {
                        const layer = document.getElementById(layerName + '-layer');
                        if (layer) {
                            const isVisible = layer.style.display !== 'none';
                            layer.style.display = isVisible ? 'none' : 'block';
                            
                            if (isVisible) {
                                activeLayerss.delete(layerName);
                            } else {
                                activeLayerss.add(layerName);
                            }
                        }
                    }
                    
                    // 해역 정보 표시
                    function showAreaInfo(areaName) {
                        if (typeof Android !== 'undefined') {
                            Android.onAreaClicked(areaName);
                        }
                    }
                    
                    // 실시간 업데이트 시뮬레이션
                    setInterval(function() {
                        document.getElementById('update-time').textContent = new Date().toLocaleString('ko-KR', {
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit'
                        });
                    }, 60000);
                    
                    // 해역 데이터 업데이트 함수
                    function updateAreaWeather(areaName, weatherData) {
                        const areaId = areaName.toLowerCase().replace(/\s+/g, '-');
                        const element = document.getElementById('weather-' + areaId);
                        if (element && weatherData) {
                            element.innerHTML = 
                                '<div>' + weatherData.condition + ' ' + weatherData.temperature + '°C</div>' +
                                '<div>파고 ' + weatherData.waveHeight + 'm</div>' +
                                '<div>풍속 ' + weatherData.windSpeed + 'm/s</div>';
                            
                            // 조업 적합도에 따른 색상 변경
                            const areaElement = element.closest('.sea-area');
                            if (areaElement) {
                                let borderColor = '#0066cc';
                                if (weatherData.windSpeed >= 15 || weatherData.waveHeight >= 3.0) {
                                    borderColor = '#ff4444';
                                } else if (weatherData.windSpeed >= 10 || weatherData.waveHeight >= 2.0) {
                                    borderColor = '#ff9800';
                                } else {
                                    borderColor = '#4caf50';
                                }
                                areaElement.style.borderColor = borderColor;
                            }
                        }
                    }
                    
                    // 현재 위치 표시
                    function showCurrentLocation(lat, lon) {
                        const marker = document.getElementById('current-location');
                        if (marker) {
                            // 위경도를 픽셀 좌표로 변환
                            const x = ((lon - 124) / 8) * 400;
                            const y = ((39 - lat) / 9) * 600;
                            
                            marker.style.left = x + 'px';
                            marker.style.top = y + 'px';
                            marker.style.display = 'block';
                            
                            // 위치 정보 업데이트
                            document.getElementById('location-info').textContent = 
                                'GPS: ' + lat.toFixed(3) + ', ' + lon.toFixed(3);
                        }
                    }
                    
                    // 태풍 위치 업데이트
                    function updateTyphoonPosition(lat, lon, name, intensity) {
                        const marker = document.getElementById('typhoon-marker');
                        const nameElement = document.getElementById('typhoon-name');
                        
                        if (marker && nameElement) {
                            // 위경도를 픽셀 좌표로 변환하여 위치 업데이트
                            const x = ((lon - 124) / 8) * 400;
                            const y = ((39 - lat) / 9) * 600;
                            
                            marker.style.left = x + 'px';
                            marker.style.top = y + 'px';
                            marker.style.display = 'block';
                            
                            nameElement.innerHTML = 
                                '<div>태풍 ' + name + '</div>' +
                                '<div style="font-size: 8px;">' + intensity + '</div>';
                        }
                    }
                    
                    // 특보 표시
                    function showWarning(areaName, hasWarning) {
                        const areaId = areaName.toLowerCase().replace(/\s+/g, '-');
                        const warning = document.getElementById('warning-' + areaId);
                        if (warning) {
                            warning.style.display = hasWarning ? 'flex' : 'none';
                        }
                    }
                    
                    // 지도 초기화 완료 신호
                    setTimeout(function() {
                        if (typeof Android !== 'undefined') {
                            Android.onMapReady();
                        }
                    }, 1000);
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.webViewMap.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun setupMapControls() {
        binding.apply {
            btnPrecipitation.setOnClickListener {
                toggleMapLayer("precipitation")
                btnPrecipitation.isSelected = !btnPrecipitation.isSelected
                updateButtonStyle(btnPrecipitation)
            }

            btnWind.setOnClickListener {
                toggleMapLayer("wind")
                btnWind.isSelected = !btnWind.isSelected
                updateButtonStyle(btnWind)
            }

            btnWaves.setOnClickListener {
                toggleMapLayer("waves")
                btnWaves.isSelected = !btnWaves.isSelected
                updateButtonStyle(btnWaves)
            }

            btnTemperature.setOnClickListener {
                toggleMapLayer("temperature")
                btnTemperature.isSelected = !btnTemperature.isSelected
                updateButtonStyle(btnTemperature)
            }

            btnRefresh.setOnClickListener {
                checkLocationAndLoadWeather()
            }
        }
    }

    private fun updateButtonStyle(button: android.widget.Button) {
        if (button.isSelected) {
            button.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
            button.setTextColor(android.graphics.Color.WHITE)
        } else {
            button.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            button.setTextColor(android.graphics.Color.parseColor("#666666"))
        }
    }

    private fun toggleMapLayer(layerType: String) {
        val script = "toggleLayer('$layerType');"
        binding.webViewMap.evaluateJavascript(script, null)
    }

    private fun updateWeatherDisplay(weather: WeatherData) {
        binding.apply {
            tvCurrentWeather.text = "${weather.condition} ${weather.temperature}°C"
            tvWindInfo.text = "${weather.windDirection} ${String.format("%.1f", weather.windSpeed)}m/s"
            tvWaveHeight.text = "${String.format("%.1f", weather.waveHeight)}m"
            tvVisibility.text = "${String.format("%.1f", weather.visibility)}km"
            tvPressure.text = "${String.format("%.1f", weather.pressure)}hPa"
            tvHumidity.text = "${weather.humidity}%"
        }

        // 지도에 현재 위치 표시
        currentLocation?.let { location ->
            val script = "showCurrentLocation(${location.latitude}, ${location.longitude});"
            binding.webViewMap.evaluateJavascript(script, null)
        }

        // 해역별 날씨 데이터를 지도에 업데이트
        updateMapWithWeatherData()
    }

    private fun updateMapWithWeatherData() {
        seaAreaWeatherData.forEach { (areaName, weatherData) ->
            val script = """
                updateAreaWeather('$areaName', {
                    condition: '${weatherData.condition}',
                    temperature: ${weatherData.temperature},
                    waveHeight: '${String.format("%.1f", weatherData.waveHeight)}',
                    windSpeed: '${String.format("%.1f", weatherData.windSpeed)}'
                });
            """.trimIndent()

            binding.webViewMap.evaluateJavascript(script, null)
        }
    }

    /**
     * JavaScript 인터페이스
     */
    inner class WebAppInterface {
        @JavascriptInterface
        fun onAreaClicked(areaName: String) {
            requireActivity().runOnUiThread {
                showAreaDetailDialog(areaName)
            }
        }

        @JavascriptInterface
        fun onMapReady() {
            requireActivity().runOnUiThread {
                // 지도 로딩 완료 후 추가 설정
                updateMapWithWeatherData()

                // 태풍 정보 업데이트
                lifecycleScope.launch {
                    try {
                        val typhoons = weatherManager.getTyphoonInfo()
                        if (typhoons.isNotEmpty()) {
                            val latestTyphoon = typhoons.first()
                            val script = """
                                updateTyphoonPosition(
                                    ${latestTyphoon.typLat}, 
                                    ${latestTyphoon.typLon}, 
                                    '${latestTyphoon.typName}',
                                    '${latestTyphoon.typPress}hPa'
                                );
                            """.trimIndent()
                            binding.webViewMap.evaluateJavascript(script, null)
                        }
                    } catch (e: Exception) {
                        // 태풍 정보 로드 실패는 무시
                    }
                }
            }
        }
    }

    private fun showAreaDetailDialog(areaName: String) {
        val weatherData = seaAreaWeatherData[areaName]

        if (weatherData != null) {
            val fishingCondition = weatherManager.calculateFishingCondition(weatherData)

            val message = """
                🌊 ${areaName} 상세 정보
                
                📍 위치: ${weatherData.location}
                🌤️ 날씨: ${weatherData.condition}
                🌡️ 기온: ${weatherData.temperature}°C
                💨 바람: ${weatherData.windDirection} ${String.format("%.1f", weatherData.windSpeed)}m/s
                🌊 파고: ${String.format("%.1f", weatherData.waveHeight)}m
                👁️ 시정: ${String.format("%.1f", weatherData.visibility)}km
                
                🎣 조업 적합도: ${fishingCondition.displayName} (${fishingCondition.score}점)
                
                ${when (fishingCondition) {
                com.example.nebada.model.FishingCondition.EXCELLENT -> "⭐ 조업에 매우 좋은 조건입니다."
                com.example.nebada.model.FishingCondition.GOOD -> "✅ 조업에 좋은 조건입니다."
                com.example.nebada.model.FishingCondition.FAIR -> "⚠️ 보통 조건입니다. 안전에 주의하세요."
                com.example.nebada.model.FishingCondition.POOR -> "⚠️ 조업이 어려운 조건입니다."
                com.example.nebada.model.FishingCondition.DANGEROUS -> "🚨 조업 위험 조건입니다. 출항을 자제하세요."
            }}
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("해역 상세 정보")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .setNeutralButton("예보 보기") { _, _ ->
                    // 상세 예보 화면으로 이동
                    parentFragmentManager.beginTransaction()
                        .replace(android.R.id.content, WeatherDetailsFragment())
                        .addToBackStack(null)
                        .commit()
                }
                .show()
        } else {
            Toast.makeText(requireContext(), "$areaName 정보를 불러오는 중입니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}