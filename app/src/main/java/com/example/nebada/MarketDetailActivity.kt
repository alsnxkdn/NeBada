package com.example.nebada

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nebada.databinding.ActivityMarketDetailBinding
import com.example.nebada.manager.MarketInfoManager
import com.example.nebada.model.DetailedMarketInfo
import com.example.nebada.model.FishMarketInfo
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MarketDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMarketDetailBinding
    private lateinit var marketManager: MarketInfoManager
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        marketManager = MarketInfoManager(this)

        val fishName = intent.getStringExtra("fish_name") ?: ""
        if (fishName.isEmpty()) {
            Toast.makeText(this, "어종 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar(fishName)
        loadDetailedInfo(fishName)
    }

    private fun setupToolbar(fishName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "$fishName 시장정보"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadDetailedInfo(fishName: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.scrollView.visibility = View.GONE

                val detailedInfo = marketManager.getDetailedMarketInfo(fishName)
                displayDetailedInfo(detailedInfo)

            } catch (e: Exception) {
                Toast.makeText(
                    this@MarketDetailActivity,
                    "상세 정보를 불러오는데 실패했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE
            }
        }
    }

    private fun displayDetailedInfo(detailedInfo: DetailedMarketInfo) {
        val marketInfo = detailedInfo.fishMarketInfo

        binding.apply {
            // 기본 정보
            tvFishName.text = marketInfo.fishName
            tvCurrentPrice.text = "${numberFormat.format(marketInfo.currentPrice.toInt())}원/kg"
            tvLastUpdated.text = "마지막 업데이트: ${timeFormat.format(marketInfo.lastUpdated)}"

            // 가격 변동 정보
            displayPriceChange(marketInfo)

            // 시장 상황과 공급 상태
            tvMarketCondition.text = marketInfo.marketCondition.displayName
            tvMarketCondition.setTextColor(Color.parseColor(marketInfo.marketCondition.color))

            tvSupplyStatus.text = marketInfo.supply.displayName
            tvSupplyStatus.setTextColor(Color.parseColor(marketInfo.supply.color))

            // 상세 정보
            tvMarketLocation.text = marketInfo.marketLocation
            tvGradeInfo.text = marketInfo.gradeInfo
            tvAverageSize.text = marketInfo.averageSize
            tvCatchArea.text = marketInfo.catchArea

            // 계절별 동향
            tvSeasonalTrend.text = detailedInfo.seasonalTrend

            // 거래 추천사항
            tvRecommendation.text = detailedInfo.recommendation

            // 가격 히스토리
            displayPriceHistory(detailedInfo)

            // 유사 어종 정보
            displaySimilarFish(detailedInfo.similarFish)
        }
    }

    private fun displayPriceChange(marketInfo: FishMarketInfo) {
        binding.apply {
            val priceChangeText = when {
                marketInfo.priceChange > 0 -> "+${numberFormat.format(marketInfo.priceChange.toInt())}"
                marketInfo.priceChange < 0 -> numberFormat.format(marketInfo.priceChange.toInt())
                else -> "0"
            }

            tvPriceChange.text = "${priceChangeText}원"
            tvPriceChangePercent.text = "(${String.format("%.1f", marketInfo.priceChangePercent)}%)"

            val changeColor = when {
                marketInfo.priceChange > 0 -> Color.parseColor("#F44336") // 빨간색
                marketInfo.priceChange < 0 -> Color.parseColor("#2196F3") // 파란색
                else -> Color.parseColor("#757575") // 회색
            }

            tvPriceChange.setTextColor(changeColor)
            tvPriceChangePercent.setTextColor(changeColor)

            // 변동 화살표
            val arrowText = when {
                marketInfo.priceChange > 0 -> "▲"
                marketInfo.priceChange < 0 -> "▼"
                else -> "━"
            }
            tvPriceArrow.text = arrowText
            tvPriceArrow.setTextColor(changeColor)
        }
    }

    private fun displayPriceHistory(detailedInfo: DetailedMarketInfo) {
        val history = detailedInfo.priceHistory.takeLast(7) // 최근 7일

        binding.layoutPriceHistory.removeAllViews()

        history.forEach { priceData ->
            val itemView = layoutInflater.inflate(
                android.R.layout.simple_list_item_2,
                binding.layoutPriceHistory,
                false
            )

            val title = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
            val subtitle = itemView.findViewById<android.widget.TextView>(android.R.id.text2)

            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            title.text = dateFormat.format(priceData.date)
            subtitle.text = "${numberFormat.format(priceData.price.toInt())}원/kg"

            binding.layoutPriceHistory.addView(itemView)
        }
    }

    private fun displaySimilarFish(similarFish: List<FishMarketInfo>) {
        binding.layoutSimilarFish.removeAllViews()

        similarFish.forEach { fish ->
            val itemView = layoutInflater.inflate(
                android.R.layout.simple_list_item_2,
                binding.layoutSimilarFish,
                false
            )

            val title = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
            val subtitle = itemView.findViewById<android.widget.TextView>(android.R.id.text2)

            title.text = fish.fishName
            subtitle.text = "${numberFormat.format(fish.currentPrice.toInt())}원/kg (${fish.marketCondition.displayName})"

            // 클릭 시 해당 어종의 상세 정보로 이동
            itemView.setOnClickListener {
                finish()
                val intent = intent
                intent.putExtra("fish_name", fish.fishName)
                startActivity(intent)
            }

            binding.layoutSimilarFish.addView(itemView)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        marketManager.destroy()
    }
}