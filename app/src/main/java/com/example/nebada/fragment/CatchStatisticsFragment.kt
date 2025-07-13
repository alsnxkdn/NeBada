package com.example.nebada.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nebada.databinding.FragmentCatchStatisticsBinding
import com.example.nebada.manager.CatchRecordManager
import java.text.NumberFormat
import java.util.*

class CatchStatisticsFragment : Fragment() {

    private var _binding: FragmentCatchStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var catchManager: CatchRecordManager
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatchStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catchManager = CatchRecordManager(requireContext())
        loadStatistics()
        setupMonthlyStats()
    }

    private fun loadStatistics() {
        val stats = catchManager.getStatistics()
        val records = catchManager.getAllRecords()

        binding.apply {
            // 전체 통계
            tvTotalRecords.text = "${stats.totalRecords}회"
            tvTotalWeight.text = "${numberFormat.format(stats.totalWeight)}kg"
            tvTotalQuantity.text = "${stats.totalQuantity}마리"
            tvTotalValue.text = "${numberFormat.format(stats.totalValue.toInt())}원"

            // 평균 정보
            tvAverageWeight.text = "${String.format("%.1f", stats.averageWeight)}kg"
            tvMostCaughtFish.text = if (stats.mostCaughtFish.isNotEmpty()) stats.mostCaughtFish else "없음"
            tvFishTypeCount.text = "${stats.fishTypeCount}종"

            // 평균 가격
            val avgPrice = if (stats.totalWeight > 0) stats.totalValue / stats.totalWeight else 0.0
            tvAveragePrice.text = "${numberFormat.format(avgPrice.toInt())}원/kg"

            // 어종별 통계
            loadFishTypeStatistics(records)

            // 최근 활동
            loadRecentActivity(records)
        }
    }

    private fun loadFishTypeStatistics(records: List<com.example.nebada.model.CatchRecord>) {
        val fishTypeStats = records.groupBy { it.fishType }
            .map { (fishType, recordList) ->
                FishTypeStat(
                    fishType = fishType,
                    count = recordList.size,
                    totalWeight = recordList.sumOf { it.weight },
                    totalValue = recordList.sumOf { it.totalValue }
                )
            }
            .sortedByDescending { it.totalValue }
            .take(5)

        // 상위 5개 어종 통계 표시
        binding.layoutFishTypeStats.removeAllViews()
        fishTypeStats.forEach { stat ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_2, binding.layoutFishTypeStats, false)

            val title = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
            val subtitle = itemView.findViewById<android.widget.TextView>(android.R.id.text2)

            title.text = "${stat.fishType} (${stat.count}회)"
            subtitle.text = "${numberFormat.format(stat.totalWeight)}kg • ${numberFormat.format(stat.totalValue.toInt())}원"

            binding.layoutFishTypeStats.addView(itemView)
        }
    }

    private fun loadRecentActivity(records: List<com.example.nebada.model.CatchRecord>) {
        val recentRecords = records.sortedByDescending { it.date }.take(3)

        binding.layoutRecentActivity.removeAllViews()

        if (recentRecords.isEmpty()) {
            val emptyView = android.widget.TextView(requireContext()).apply {
                text = "최근 어획 기록이 없습니다"
                textSize = 14f
                setTextColor(android.graphics.Color.GRAY)
                setPadding(16, 16, 16, 16)
            }
            binding.layoutRecentActivity.addView(emptyView)
        } else {
            recentRecords.forEach { record ->
                val itemView = LayoutInflater.from(requireContext())
                    .inflate(android.R.layout.simple_list_item_2, binding.layoutRecentActivity, false)

                val title = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
                val subtitle = itemView.findViewById<android.widget.TextView>(android.R.id.text2)

                val dateFormat = java.text.SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                title.text = "${record.fishType} ${numberFormat.format(record.weight)}kg"
                subtitle.text = "${dateFormat.format(record.date)} • ${record.location}"

                binding.layoutRecentActivity.addView(itemView)
            }
        }
    }

    private fun setupMonthlyStats() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        val monthlyStats = catchManager.getMonthlyStatistics(currentYear, currentMonth)

        binding.apply {
            tvCurrentMonth.text = "${currentMonth}월 통계"
            tvMonthlyRecords.text = "${monthlyStats.totalRecords}회"
            tvMonthlyWeight.text = "${numberFormat.format(monthlyStats.totalWeight)}kg"
            tvMonthlyValue.text = "${numberFormat.format(monthlyStats.totalValue.toInt())}원"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class FishTypeStat(
        val fishType: String,
        val count: Int,
        val totalWeight: Double,
        val totalValue: Double
    )
}