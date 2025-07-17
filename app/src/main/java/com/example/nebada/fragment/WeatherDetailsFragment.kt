package com.example.nebada.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebada.adapter.WeatherForecastAdapter
import com.example.nebada.databinding.FragmentWeatherDetailsBinding
import com.example.nebada.manager.RealWeatherManager
import com.example.nebada.model.WeatherData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherDetailsFragment : Fragment() {

    private var _binding: FragmentWeatherDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var weatherManager: RealWeatherManager
    private lateinit var forecastAdapter: WeatherForecastAdapter
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weatherManager = RealWeatherManager(requireContext())
        setupRecyclerView()
        setupSwipeRefresh()
        loadDetailedWeatherData()
    }

    private fun setupRecyclerView() {
        forecastAdapter = WeatherForecastAdapter { weather ->
            showWeatherDetail(weather)
        }

        binding.recyclerViewForecast.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = forecastAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadDetailedWeatherData()
        }
    }

    private fun loadDetailedWeatherData() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true

                // 현재 날씨 정보
                val currentWeather = weatherManager.getCurrentWeather()
                updateCurrentWeatherDisplay(currentWeather)

                // 24시간 예보
                val forecast = weatherManager.getSeaForecast()
                forecastAdapter.submitList(forecast)

                // 조업 적합도 계산
                val fishingCondition = weatherManager.calculateFishingCondition(currentWeather)
                updateFishingConditionDisplay(fishingCondition, currentWeather)

                binding.tvLastUpdate.text = "마지막 업데이트: ${dateFormat.format(Date())}"

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "상세 기상정보 로드 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateCurrentWeatherDisplay(weather: WeatherData) {
        binding.apply {
            // 현재 날씨 헤더
            tvCurrentCondition.text = weather.condition
            tvCurrentTemp.text = "${weather.temperature}°C"
            tvLocation.text = weather.location

            // 상세 정보 그리드
            tvWindDirection.text = weather.windDirection
            tvWindSpeed.text = "${String.format("%.1f", weather.windSpeed)}m/s"
            tvWaveHeight.text = "${String.format("%.1f", weather.waveHeight)}m"
            tvVisibility.text = "${String.format("%.1f", weather.visibility)}km"
            tvPressure.text = "${String.format("%.1f", weather.pressure)}hPa"
            tvHumidity.text = "${weather.humidity}%"

            weather.waterTemp?.let {
                tvWaterTemp.text = "${String.format("%.1f", it)}°C"
                layoutWaterTemp.visibility = View.VISIBLE
            } ?: run {
                layoutWaterTemp.visibility = View.GONE
            }
        }
    }

    private fun updateFishingConditionDisplay(condition: com.example.nebada.model.FishingCondition, weather: WeatherData) {
        binding.apply {
            tvFishingCondition.text = condition.displayName
            tvFishingCondition.setTextColor(android.graphics.Color.parseColor(condition.color))
            tvFishingScore.text = "조업 적합도: ${condition.score}점"

            // 조업 권고사항
            val recommendation = when (condition) {
                com.example.nebada.model.FishingCondition.EXCELLENT ->
                    "⭐ 조업에 매우 좋은 조건입니다. 안전하게 조업하세요."
                com.example.nebada.model.FishingCondition.GOOD ->
                    "✅ 조업에 좋은 조건입니다. 기상 변화를 주의하세요."
                com.example.nebada.model.FishingCondition.FAIR ->
                    "⚠️ 보통 조건입니다. 안전장비를 점검하고 조업하세요."
                com.example.nebada.model.FishingCondition.POOR ->
                    "⚠️ 조업에 어려운 조건입니다. 신중히 판단하세요."
                com.example.nebada.model.FishingCondition.DANGEROUS ->
                    "🚨 조업 위험 조건입니다. 출항을 자제하세요."
            }

            tvRecommendation.text = recommendation

            // 위험 요소 분석
            val riskFactors = mutableListOf<String>()

            if (weather.windSpeed >= 15) {
                riskFactors.add("⚠️ 강풍 (${String.format("%.1f", weather.windSpeed)}m/s)")
            }
            if (weather.waveHeight >= 3.0) {
                riskFactors.add("🌊 높은 파고 (${String.format("%.1f", weather.waveHeight)}m)")
            }
            if (weather.visibility < 5) {
                riskFactors.add("🌫️ 낮은 시정 (${String.format("%.1f", weather.visibility)}km)")
            }
            if (weather.condition.contains("비") || weather.condition.contains("눈")) {
                riskFactors.add("🌧️ 강수 (${weather.condition})")
            }

            if (riskFactors.isNotEmpty()) {
                tvRiskFactors.text = "주의사항:\n${riskFactors.joinToString("\n")}"
                layoutRiskFactors.visibility = View.VISIBLE
            } else {
                layoutRiskFactors.visibility = View.GONE
            }
        }
    }

    private fun showWeatherDetail(weather: WeatherData) {
        val detailDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("${weather.location} 상세정보")
            .setMessage("""
                시간: ${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(weather.date)}
                날씨: ${weather.condition}
                기온: ${weather.temperature}°C
                풍향: ${weather.windDirection}
                풍속: ${String.format("%.1f", weather.windSpeed)}m/s
                파고: ${String.format("%.1f", weather.waveHeight)}m
                시정: ${String.format("%.1f", weather.visibility)}km
                기압: ${String.format("%.1f", weather.pressure)}hPa
                습도: ${weather.humidity}%
                ${weather.waterTemp?.let { "수온: ${String.format("%.1f", it)}°C" } ?: ""}
            """.trimIndent())
            .setPositiveButton("확인", null)
            .create()

        detailDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}