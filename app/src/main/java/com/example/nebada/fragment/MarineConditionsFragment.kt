package com.example.nebada.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebada.adapter.WeatherWarningAdapter
import com.example.nebada.databinding.FragmentMarineConditionsBinding
import com.example.nebada.manager.RealWeatherManager
import com.example.nebada.model.WeatherWarning
import com.example.nebada.model.TyphoonItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MarineConditionsFragment : Fragment() {

    private var _binding: FragmentMarineConditionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var weatherManager: RealWeatherManager
    private lateinit var warningAdapter: WeatherWarningAdapter
    private val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarineConditionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weatherManager = RealWeatherManager(requireContext())
        setupRecyclerView()
        setupSwipeRefresh()
        loadMarineConditions()
    }

    private fun setupRecyclerView() {
        warningAdapter = WeatherWarningAdapter { warning ->
            showWarningDetail(warning)
        }

        binding.recyclerViewWarnings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = warningAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadMarineConditions()
        }
    }

    private fun loadMarineConditions() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true

                // 기상특보 정보 로드
                val warnings = weatherManager.getWeatherWarnings()
                warningAdapter.submitList(warnings)
                updateWaringSummary(warnings)

                // 태풍 정보 로드
                val typhoons = weatherManager.getTyphoonInfo()
                updateTyphoonInfo(typhoons)

                // 해상 상태 요약
                updateSeaConditionSummary()

                binding.tvLastUpdate.text = "마지막 업데이트: ${dateTimeFormat.format(Date())}"

                if (warnings.isEmpty() && typhoons.isEmpty()) {
                    binding.layoutNoWarnings.visibility = View.VISIBLE
                    binding.recyclerViewWarnings.visibility = View.GONE
                } else {
                    binding.layoutNoWarnings.visibility = View.GONE
                    binding.recyclerViewWarnings.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "해상 조건 정보 로드 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateWaringSummary(warnings: List<WeatherWarning>) {
        val activeWarnings = warnings.filter { it.validTo.after(Date()) }

        binding.apply {
            tvWarningCount.text = "${activeWarnings.size}건"

            val highSeverityCount = activeWarnings.count {
                it.severity == com.example.nebada.model.WarningSeverity.HIGH ||
                        it.severity == com.example.nebada.model.WarningSeverity.VERY_HIGH
            }

            if (highSeverityCount > 0) {
                tvWarningStatus.text = "⚠️ 주의 필요"
                tvWarningStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
            } else if (activeWarnings.isNotEmpty()) {
                tvWarningStatus.text = "⚡ 주의 관찰"
                tvWarningStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            } else {
                tvWarningStatus.text = "✅ 양호"
                tvWarningStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }

            // 특보 지역 요약
            val affectedAreas = activeWarnings.flatMap { it.affectedAreas }.distinct()
            if (affectedAreas.isNotEmpty()) {
                tvAffectedAreas.text = "영향 지역: ${affectedAreas.joinToString(", ")}"
                tvAffectedAreas.visibility = View.VISIBLE
            } else {
                tvAffectedAreas.visibility = View.GONE
            }
        }
    }

    private fun updateTyphoonInfo(typhoons: List<TyphoonItem>) {
        binding.apply {
            if (typhoons.isNotEmpty()) {
                val latestTyphoon = typhoons.maxByOrNull { it.tmFc }
                latestTyphoon?.let { typhoon ->
                    tvTyphoonName.text = "🌀 태풍 ${typhoon.typName}"
                    tvTyphoonLocation.text = "위치: ${typhoon.typLoc}"
                    tvTyphoonIntensity.text = "중심기압: ${typhoon.typPress}hPa, 최대풍속: ${typhoon.typWs}m/s"
                    tvTyphoonDirection.text = "이동: ${typhoon.typDir} ${typhoon.typSpeed}km/h"

                    layoutTyphoonInfo.visibility = View.VISIBLE

                    // 태풍 위험도 판단
                    val pressure = typhoon.typPress.toIntOrNull() ?: 1013
                    val windSpeed = typhoon.typWs.toIntOrNull() ?: 0

                    val riskLevel = when {
                        pressure < 950 || windSpeed > 50 -> "매우 위험"
                        pressure < 980 || windSpeed > 35 -> "위험"
                        pressure < 1000 || windSpeed > 25 -> "주의"
                        else -> "관찰"
                    }

                    val riskColor = when (riskLevel) {
                        "매우 위험" -> "#9C27B0"
                        "위험" -> "#F44336"
                        "주의" -> "#FF9800"
                        else -> "#2196F3"
                    }

                    tvTyphoonRisk.text = "위험도: $riskLevel"
                    tvTyphoonRisk.setTextColor(android.graphics.Color.parseColor(riskColor))
                }
            } else {
                layoutTyphoonInfo.visibility = View.GONE
            }
        }
    }

    private fun updateSeaConditionSummary() {
        lifecycleScope.launch {
            try {
                // 주요 해역별 현재 상태 요약
                val seaConditions = mutableListOf<String>()

                val easternSea = weatherManager.getCurrentWeather(37.5, 129.5) // 동해
                val westernSea = weatherManager.getCurrentWeather(36.5, 126.0) // 서해
                val southernSea = weatherManager.getCurrentWeather(34.8, 128.0) // 남해
                val jejuSea = weatherManager.getCurrentWeather(33.5, 126.5) // 제주

                listOf(
                    "동해" to easternSea,
                    "서해" to westernSea,
                    "남해" to southernSea,
                    "제주" to jejuSea
                ).forEach { (name, weather) ->
                    val condition = weatherManager.calculateFishingCondition(weather)
                    seaConditions.add("$name: ${condition.displayName}")
                }

                binding.tvSeaConditionSummary.text = seaConditions.joinToString(" | ")

            } catch (e: Exception) {
                binding.tvSeaConditionSummary.text = "해상 상태 정보 로드 중..."
            }
        }
    }

    private fun showWarningDetail(warning: WeatherWarning) {
        val detailDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("${warning.type.displayName} ${warning.severity.displayName}")
            .setMessage("""
                발효시간: ${dateTimeFormat.format(warning.validFrom)}
                해제예정: ${dateTimeFormat.format(warning.validTo)}
                
                영향지역: ${warning.affectedAreas.joinToString(", ")}
                
                상세내용:
                ${warning.description}
                
                ⚠️ 해당 지역 조업 시 각별히 주의하시기 바랍니다.
            """.trimIndent())
            .setPositiveButton("확인", null)
            .setNeutralButton("상세 정보") { _, _ ->
                // 추가 상세 정보나 관련 링크 열기
                Toast.makeText(requireContext(), "기상청 홈페이지에서 자세한 정보를 확인하세요", Toast.LENGTH_LONG).show()
            }
            .create()

        detailDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}