package com.example.nebada.fragment

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
import com.example.nebada.manager.WeatherManager
import com.example.nebada.model.WeatherData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherMapFragment : Fragment() {

    private var _binding: FragmentWeatherMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var weatherManager: WeatherManager
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

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

        weatherManager = WeatherManager(requireContext())

        setupWebView()
        setupWeatherInfo()
        setupMapControls()
        loadWeatherData()
    }

    private fun setupWebView() {
        binding.webViewMap.apply {
            webViewClient = WebViewClient()
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
            }

            // 날씨 지도 HTML 로드
            loadWeatherMap()
        }
    }

    private fun loadWeatherMap() {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>날씨 지도</title>
                <style>
                    body { margin: 0; padding: 0; font-family: Arial, sans-serif; }
                    .map-container { position: relative; width: 100%; height: 100vh; }
                    .weather-overlay { 
                        position: absolute; 
                        top: 10px; 
                        left: 10px; 
                        right: 10px;
                        background: rgba(255,255,255,0.9);
                        padding: 10px;
                        border-radius: 8px;
                        z-index: 1000;
                    }
                    .legend {
                        position: absolute;
                        bottom: 10px;
                        left: 10px;
                        background: rgba(255,255,255,0.9);
                        padding: 10px;
                        border-radius: 8px;
                        font-size: 12px;
                    }
                    .weather-layer {
                        position: absolute;
                        width: 100%;
                        height: 100%;
                        pointer-events: none;
                    }
                    .precipitation { background: linear-gradient(rgba(0,0,255,0.3), rgba(0,0,255,0.7)); }
                    .wind-arrows { background-image: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHZpZXdCb3g9IjAgMCAyMCAyMCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEwIDJMMTYgMThINCIgc3Ryb2tlPSJibGFjayIgc3Ryb2tlLXdpZHRoPSIyIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4K'); }
                </style>
            </head>
            <body>
                <div class="map-container">
                    <div class="weather-overlay">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <h3 style="margin: 0;">🌊 한반도 해상 기상정보</h3>
                            <div style="font-size: 12px; color: #666;">
                                업데이트: <span id="update-time">${dateFormat.format(Date())}</span>
                            </div>
                        </div>
                    </div>
                    
                    <!-- 한국 지도 SVG -->
                    <svg width="100%" height="100%" viewBox="0 0 400 500" style="background: #e6f3ff;">
                        <!-- 한반도 윤곽 -->
                        <path d="M150 50 Q200 40 250 60 L280 100 Q290 150 270 200 L260 250 Q250 300 230 350 L200 400 Q180 420 160 400 L120 350 Q100 300 110 250 L120 200 Q130 150 140 100 Z" 
                              fill="#f0f0f0" stroke="#999" stroke-width="2"/>
                        
                        <!-- 주요 도시 표시 -->
                        <circle cx="200" cy="120" r="3" fill="#ff4444"/>
                        <text x="205" y="125" font-size="10" fill="#333">서울</text>
                        
                        <circle cx="160" cy="380" r="3" fill="#ff4444"/>
                        <text x="165" y="385" font-size="10" fill="#333">부산</text>
                        
                        <circle cx="120" cy="200" r="3" fill="#ff4444"/>
                        <text x="125" y="205" font-size="10" fill="#333">인천</text>
                        
                        <circle cx="100" cy="450" r="3" fill="#ff4444"/>
                        <text x="105" y="455" font-size="10" fill="#333">제주</text>
                        
                        <!-- 바람 화살표 -->
                        <g id="wind-arrows">
                            <g transform="translate(180,100)">
                                <line x1="0" y1="0" x2="15" y2="-10" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="17" y="-5" font-size="8" fill="#0066cc">15m/s</text>
                            </g>
                            <g transform="translate(140,200)">
                                <line x1="0" y1="0" x2="10" y2="-15" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="12" y="-10" font-size="8" fill="#0066cc">12m/s</text>
                            </g>
                            <g transform="translate(200,300)">
                                <line x1="0" y1="0" x2="20" y2="0" stroke="#0066cc" stroke-width="2" marker-end="url(#arrowhead)"/>
                                <text x="22" y="5" font-size="8" fill="#0066cc">8m/s</text>
                            </g>
                        </g>
                        
                        <!-- 강수 영역 -->
                        <ellipse cx="220" cy="180" rx="40" ry="60" fill="rgba(0,100,255,0.3)" opacity="0.7"/>
                        <ellipse cx="160" cy="280" rx="30" ry="40" fill="rgba(0,150,255,0.4)" opacity="0.7"/>
                        
                        <!-- 태풍/저기압 -->
                        <circle cx="300" cy="200" r="25" fill="none" stroke="#ff0000" stroke-width="3" stroke-dasharray="5,5"/>
                        <text x="280" y="195" font-size="10" fill="#ff0000" font-weight="bold">태풍</text>
                        <text x="275" y="210" font-size="8" fill="#ff0000">KHANUN</text>
                        
                        <!-- 파고 정보 -->
                        <g id="wave-height">
                            <text x="320" y="120" font-size="10" fill="#006699" font-weight="bold">동해</text>
                            <text x="320" y="135" font-size="8" fill="#006699">파고: 2.5~4.0m</text>
                            
                            <text x="50" y="200" font-size="10" fill="#006699" font-weight="bold">서해</text>
                            <text x="50" y="215" font-size="8" fill="#006699">파고: 1.0~2.0m</text>
                            
                            <text x="250" y="350" font-size="10" fill="#006699" font-weight="bold">남해</text>
                            <text x="250" y="365" font-size="8" fill="#006699">파고: 1.5~3.0m</text>
                        </g>
                        
                        <!-- 화살표 마커 정의 -->
                        <defs>
                            <marker id="arrowhead" markerWidth="10" markerHeight="7" 
                                    refX="9" refY="3.5" orient="auto">
                                <polygon points="0 0, 10 3.5, 0 7" fill="#0066cc"/>
                            </marker>
                        </defs>
                    </svg>
                    
                    <div class="legend">
                        <div style="font-weight: bold; margin-bottom: 5px;">범례</div>
                        <div style="margin: 2px 0;">
                            <span style="display: inline-block; width: 15px; height: 10px; background: rgba(0,100,255,0.3); margin-right: 5px;"></span>
                            강수 예상 지역
                        </div>
                        <div style="margin: 2px 0;">
                            <span style="display: inline-block; width: 15px; height: 2px; background: #0066cc; margin-right: 5px;"></span>
                            바람 방향/세기
                        </div>
                        <div style="margin: 2px 0;">
                            <span style="display: inline-block; width: 15px; height: 10px; border: 2px dashed #ff0000; margin-right: 5px;"></span>
                            태풍/저기압
                        </div>
                        <div style="margin: 2px 0;">
                            <span style="color: #006699;">●</span> 파고 정보
                        </div>
                    </div>
                </div>
                
                <script>
                    // 실시간 업데이트 시뮬레이션
                    setInterval(function() {
                        document.getElementById('update-time').textContent = new Date().toLocaleString();
                    }, 60000);
                    
                    // 터치/클릭 이벤트 처리
                    document.addEventListener('click', function(e) {
                        // 지도 클릭 시 해당 지역 상세 정보 표시
                        const x = e.clientX;
                        const y = e.clientY;
                        console.log('Clicked at:', x, y);
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.webViewMap.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun setupWeatherInfo() {
        // 현재 날씨 정보 표시
        binding.apply {
            tvCurrentWeather.text = "⛅ 구름많음 23°C"
            tvWindInfo.text = "🌬️ 남동풍 12m/s"
            tvWaveHeight.text = "🌊 파고 1.5~2.5m"
            tvVisibility.text = "👁️ 시정 15km"
        }
    }

    private fun setupMapControls() {
        binding.apply {
            // 지도 레이어 토글 버튼들
            btnPrecipitation.setOnClickListener {
                toggleMapLayer("precipitation")
            }

            btnWind.setOnClickListener {
                toggleMapLayer("wind")
            }

            btnWaves.setOnClickListener {
                toggleMapLayer("waves")
            }

            btnTemperature.setOnClickListener {
                toggleMapLayer("temperature")
            }

            // 새로고침 버튼
            btnRefresh.setOnClickListener {
                loadWeatherData()
            }
        }
    }

    private fun toggleMapLayer(layerType: String) {
        // WebView에 JavaScript 실행하여 레이어 토글
        val script = """
            (function() {
                // 레이어 토글 로직
                var layer = document.querySelector('.weather-layer.$layerType');
                if (layer) {
                    layer.style.display = layer.style.display === 'none' ? 'block' : 'none';
                } else {
                    // 레이어 생성 및 표시
                    var newLayer = document.createElement('div');
                    newLayer.className = 'weather-layer $layerType';
                    document.querySelector('.map-container').appendChild(newLayer);
                }
            })();
        """.trimIndent()

        binding.webViewMap.evaluateJavascript(script, null)
    }

    private fun loadWeatherData() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val weatherData = weatherManager.getCurrentWeather()
                updateWeatherDisplay(weatherData)

                // 예보 데이터 로드
                val forecast = weatherManager.getSeaForecast()
                updateForecastDisplay(forecast)

                Toast.makeText(requireContext(), "날씨 정보 업데이트 완료", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "날씨 정보 로드 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateWeatherDisplay(weather: WeatherData) {
        binding.apply {
            tvCurrentWeather.text = "${weather.condition} ${weather.temperature}°C"
            tvWindInfo.text = "🌬️ ${weather.windDirection} ${weather.windSpeed}m/s"
            tvWaveHeight.text = "🌊 파고 ${weather.waveHeight}m"
            tvVisibility.text = "👁️ 시정 ${weather.visibility}km"
            tvPressure.text = "📊 ${weather.pressure}hPa"
            tvHumidity.text = "💧 습도 ${weather.humidity}%"
        }
    }

    private fun updateForecastDisplay(forecast: List<WeatherData>) {
        // 예보 정보를 WebView 지도에 반영
        val forecastScript = """
            (function() {
                // 예보 데이터를 지도에 업데이트
                document.getElementById('update-time').textContent = '${dateFormat.format(Date())}';
                
                // 추가적인 예보 시각화 로직
            })();
        """.trimIndent()

        binding.webViewMap.evaluateJavascript(forecastScript, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}