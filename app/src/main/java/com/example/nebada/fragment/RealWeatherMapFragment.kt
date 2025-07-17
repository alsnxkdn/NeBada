package com.example.nebada.fragment

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nebada.databinding.FragmentWeatherMapBinding
import com.example.nebada.manager.RealWeatherManager
import com.example.nebada.model.WeatherData
import com.example.nebada.model.SeaAreas
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RealWeatherMapFragment : Fragment() {

    private var _binding: FragmentWeatherMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var weatherManager: RealWeatherManager
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    private var currentLocation: Location? = null
    private var currentWeatherData: WeatherData? = null
    private var forecastData: List<WeatherData> = emptyList()

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

        setupWebView()
        setupMapControls()
        loadRealWeatherData()
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
        }
    }

    private fun loadRealWeatherMap() {
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
                    .layer-wind { 
                        background-image: 
                            url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHZpZXdCb3g9IjAgMCAyMCAyMCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEwIDJMMTYgMThINCIgc3Ryb2tlPSIjMDA2NmNjIiBzdHJva2Utd2lkdGg9IjIiIGZpbGw9IndoaXRlIi8+Cjwvc3ZnPgo=');
                        background-repeat: repeat;
                        background-size: 40px 40px;
                        display: none;
                    }
                    .layer-waves {
                        background: linear-gradient(45deg, 
                            rgba(0,150,200,0.2) 25%, transparent 25%, transparent 75%, rgba(0,150,200,0.2) 75%),
                            linear-gradient(-45deg, 
                            rgba(0,150,200,0.2) 25%, transparent 25%, transparent 75%, rgba(0,150,200,0.2) 75%);
                        background-size: 30px 30px;
                        display: none;
                    }
                    .layer-temperature {
                        background: linear-gradient(to bottom, 
                            rgba(255,100,100,0.3) 0%, 
                            rgba(255,200,100,0.3) 50%, 
                            rgba(100,150,255,0.3) 100%);
                        display: none;
                    }
                    .sea-area {
                        position: absolute;
                        background: rgba(255,255,255,0.9);
                        border: 2px solid #0066cc;
                        border-radius: 8px;
                        padding: 6px;
                        font-size: 10px;
                        text-align: center;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        min-width: 60px;
                    }
                    .sea-area:hover {
                        background: rgba(0,102,204,0.9);
                        color: white;
                        transform: scale(1.1);
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
                    }
                    @keyframes typhoon-spin {
                        from { transform: rotate(0deg); }
                        to { transform: rotate(360deg); }
                    }
                    .warning-indicator {
                        position: absolute;
                        top: 0;
                        right: 0;
                        background: #ff4444;
                        color: white;
                        border-radius: 50%;
                        width: 20px;
                        height: 20px;
                        font-size: 12px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        animation: warning-blink 1s ease-in-out infinite alternate;
                    }
                    @keyframes warning-blink {
                        from { opacity: 1; }
                        to { opacity: 0.5; }
                    }
                </style>
            </head>
            <body>
                <div class="map-container">
                    <!-- 상단 정보 오버레이 -->
                    <div class="weather-overlay">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <h3 style="margin: 0; color: #0066cc;">🌊 실시간 해상 기상정보</h3>
                            <div style="font-size: 11px; color: #666;">
                                <div>기상청 제공</div>
                                <div id="update-time">${dateFormat.format(Date())}</div>
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
                        
                        <!-- 해역 구분선 -->
                        <line x1="200" y1="50" x2="200" y2="200" stroke="#ccc" stroke-width="1" stroke-dasharray="5,5"/>
                        <line x1="50" y1="250" x2="350" y2="250" stroke="#ccc" stroke-width="1" stroke-dasharray="5,5"/>
                        <line x1="200" y1="350" x2="200" y2="550" stroke="#ccc" stroke-width="1" stroke-dasharray="5,5"/>
                        
                        <!-- 날씨 레이어들 -->
                        <g class="weather-layer layer-precipitation">
                            <ellipse cx="180" cy="180" rx="40" ry="60" fill="rgba(0,100,255,0.4)"/>
                            <ellipse cx="250" cy="300" rx="35" ry="45" fill="rgba(0,150,255,0.3)"/>
                        </g>
                        
                        <g class="weather-layer layer-wind">
                            <!-- 바람 벡터 화살표들 -->
                            <g transform="translate(120,120)">
                                <line x1="0" y1="0" x2="20" y2="-10" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="25" y="-5" font-size="8" fill="#0066cc">12m/s</text>
                            </g>
                            <g transform="translate(280,120)">
                                <line x1="0" y1="0" x2="15" y2="-15" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="18" y="-10" font-size="8" fill="#0066cc">15m/s</text>
                            </g>
                            <g transform="translate(200,350)">
                                <line x1="0" y1="0" x2="25" y2="0" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="28" y="5" font-size="8" fill="#0066cc">8m/s</text>
                            </g>
                        </g>
                        
                        <g class="weather-layer layer-waves">
                            <!-- 파고 정보는 CSS 패턴으로 표시 -->
                        </g>
                        
                        <g class="weather-layer layer-temperature">
                            <!-- 수온 정보는 CSS 그라디언트로 표시 -->
                        </g>
                        
                        <!-- 화살표 마커 정의 -->
                        <defs>
                            <marker id="arrowhead" markerWidth="10" markerHeight="7" 
                                    refX="9" refY="3.5" orient="auto">
                                <polygon points="0 0, 10 3.5, 0 7" fill="#0066cc"/>
                            </marker>
                        </defs>
                    </svg>
                    
                    <!-- 해역별 정보 표시 -->
                    <div class="sea-area" style="top: 100px; left: 60px;" onclick="showAreaInfo('동해북부')">
                        <div style="font-weight: bold;">동해북부</div>
                        <div id="area-donghae-north">파고 2.5m</div>
                    </div>
                    
                    <div class="sea-area" style="top: 200px; left: 300px;" onclick="showAreaInfo('동해중부')">
                        <div style="font-weight: bold;">동해중부</div>
                        <div id="area-donghae-center">파고 3.0m</div>
                    </div>
                    
                    <div class="sea-area" style="top: 320px; left: 290px;" onclick="showAreaInfo('동해남부')">
                        <div style="font-weight: bold;">동해남부</div>
                        <div id="area-donghae-south">파고 2.0m</div>
                    </div>
                    
                    <div class="sea-area" style="top: 180px; left: 30px;" onclick="showAreaInfo('서해중부')">
                        <div style="font-weight: bold;">서해중부</div>
                        <div id="area-seohae-center">파고 1.5m</div>
                    </div>
                    
                    <div class="sea-area" style="top: 300px; left: 50px;" onclick="showAreaInfo('서해남부')">
                        <div style="font-weight: bold;">서해남부</div>
                        <div id="area-seohae-south">파고 1.0m</div>
                    </div>
                    
                    <div class="sea-area" style="top: 380px; left: 200px;" onclick="showAreaInfo('남해동부')">
                        <div style="font-weight: bold;">남해동부</div>
                        <div id="area-namhae-east">파고 2.5m</div>
                    </div>
                    
                    <div class="sea-area" style="top: 420px; left: 120px;" onclick="showAreaInfo('남해서부')">
                        <div style="font-weight: bold;">남해서부</div>
                        <div id="area-namhae-west">파고 1.8m</div>
                    </div>
                    
                    <div class="sea-area" style="top: 480px; left: 120px;" onclick="showAreaInfo('제주도')">
                        <div style="font-weight: bold;">제주도</div>
                        <div id="area-jeju">파고 3.5m</div>
                        <div class="warning-indicator">!</div>
                    </div>
                    
                    <!-- 태풍 표시 (있는 경우) -->
                    <div class="typhoon-marker" style="top: 150px; right: 50px;" id="typhoon-marker">
                        <div style="position: absolute; top: -25px; left: -10px; font-size: 10px; font-weight: bold; color: #ff0000;">
                            태풍 KHANUN
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
                    </div>
                </div>
                
                <script>
                    // 레이어 토글 함수
                    function toggleLayer(layerName) {
                        const layer = document.querySelector('.layer-' + layerName);
                        if (layer) {
                            const isVisible = layer.style.display !== 'none';
                            layer.style.display = isVisible ? 'none' : 'block';
                        }
                    }
                    
                    // 해역 정보 표시
                    function showAreaInfo(areaName) {
                        Android.onAreaClicked(areaName);
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
                    function updateAreaData(areaId, data) {
                        const element = document.getElementById('area-' + areaId);
                        if (element) {
                            element.innerHTML = data;
                        }
                    }
                    
                    // 태풍 위치 업데이트
                    function updateTyphoonPosition(lat, lon, name) {
                        const marker = document.getElementById('typhoon-marker');
                        if (marker) {
                            // 위경도를 픽셀 좌표로 변환하여 위치 업데이트
                            const x = (lon - 124) * 8;
                            const y = (39 - lat) * 12;
                            marker.style.left = x + 'px';
                            marker.style.top = y + 'px';
                            marker.querySelector('div').textContent = '태풍 ' + name;
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.webViewMap.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun setupMapControls() {
        binding.apply {
            // 지도 레이어 토글 버튼들
            btnPrecipitation.setOnClickListener {
                toggleMapLayer("precipitation")
                btnPrecipitation.isSelected = !btnPrecipitation.isSelected
            }

            btnWind.setOnClickListener {
                toggleMapLayer("wind")
                btnWind.isSelected = !btnWind.isSelected
            }

            btnWaves.setOnClickListener {
                toggleMapLayer("waves")
                btnWaves.isSelected = !btnWaves.isSelected
            }

            btnTemperature.setOnClickListener {
                toggleMapLayer("temperature")
                btnTemperature.isSelected = !btnTemperature.isSelected
            }

            // 새로고침 버튼
            btnRefresh.setOnClickListener {
                loadRealWeatherData()
            }
        }
    }

    private fun toggleMapLayer(layerType: String) {
        val script = "toggleLayer('$layerType');"
        binding.webViewMap.evaluateJavascript(script, null)
    }

    private fun loadRealWeatherData() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // 현재 날씨 정보 로드
                currentWeatherData = weatherManager.getCurrentWeather()
                updateWeatherDisplay(currentWeatherData!!)

                // 예보 데이터 로드
                forecastData = weatherManager.getSeaForecast()

                // 지도 로드
                loadRealWeatherMap()

                // 해역별 실시간 데이터 업데이트
                updateSeaAreaData()

                Toast.makeText(requireContext(), "실시간 기상정보 업데이트 완료", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "기상정보 로드 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun updateSeaAreaData() {
        SeaAreas.areas.forEach { area ->
            try {
                val areaWeather = weatherManager.getCurrentWeather(area.latitude, area.longitude)
                val fishingCondition = weatherManager.calculateFishingCondition(areaWeather)

                val areaInfo = """
                    <div style="color: ${fishingCondition.color};">
                        ${areaWeather.condition} ${areaWeather.temperature}°C
                    </div>
                    <div>파고 ${String.format("%.1f", areaWeather.waveHeight)}m</div>
                    <div>풍속 ${String.format("%.1f", areaWeather.windSpeed)}m/s</div>
                """.trimIndent()

                val areaId = area.name.replace(" ", "-").lowercase()
                val script = "updateAreaData('$areaId', '$areaInfo');"
                binding.webViewMap.evaluateJavascript(script, null)

            } catch (e: Exception) {
                // 개별 해역 데이터 로드 실패는 무시
            }
        }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}