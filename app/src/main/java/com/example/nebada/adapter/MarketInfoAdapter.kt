package com.example.nebada.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nebada.databinding.ItemMarketInfoBinding
import com.example.nebada.model.FishMarketInfo
import com.example.nebada.model.MarketCondition
import com.example.nebada.model.SupplyStatus
import java.text.NumberFormat
import java.util.*

class MarketInfoAdapter(
    private val onItemClick: (FishMarketInfo) -> Unit
) : ListAdapter<FishMarketInfo, MarketInfoAdapter.MarketInfoViewHolder>(DiffCallback()) {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketInfoViewHolder {
        val binding = ItemMarketInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MarketInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarketInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MarketInfoViewHolder(
        private val binding: ItemMarketInfoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(marketInfo: FishMarketInfo) {
            binding.apply {
                // 어종명
                tvFishName.text = marketInfo.fishName

                // 현재 가격
                tvCurrentPrice.text = "${numberFormat.format(marketInfo.currentPrice.toInt())}원/kg"

                // 가격 변동
                val priceChangeText = when {
                    marketInfo.priceChange > 0 -> "+${numberFormat.format(marketInfo.priceChange.toInt())}"
                    marketInfo.priceChange < 0 -> numberFormat.format(marketInfo.priceChange.toInt())
                    else -> "0"
                }
                tvPriceChange.text = "$priceChangeText (${String.format("%.1f", marketInfo.priceChangePercent)}%)"

                // 가격 변동 색상
                val changeColor = when {
                    marketInfo.priceChange > 0 -> Color.parseColor("#F44336") // 빨간색 (상승)
                    marketInfo.priceChange < 0 -> Color.parseColor("#2196F3") // 파란색 (하락)
                    else -> Color.parseColor("#757575") // 회색 (보합)
                }
                tvPriceChange.setTextColor(changeColor)

                // 시장 상황
                tvMarketCondition.text = marketInfo.marketCondition.displayName
                tvMarketCondition.setTextColor(Color.parseColor(marketInfo.marketCondition.color))

                // 공급 상태
                tvSupplyStatus.text = marketInfo.supply.displayName
                tvSupplyStatus.setTextColor(Color.parseColor(marketInfo.supply.color))

                // 시장 위치
                tvMarketLocation.text = marketInfo.marketLocation

                // 등급 정보
                if (marketInfo.gradeInfo.isNotEmpty()) {
                    tvGradeInfo.text = marketInfo.gradeInfo
                    tvGradeInfo.visibility = android.view.View.VISIBLE
                } else {
                    tvGradeInfo.visibility = android.view.View.GONE
                }

                // 어획 지역
                if (marketInfo.catchArea.isNotEmpty()) {
                    tvCatchArea.text = marketInfo.catchArea
                    tvCatchArea.visibility = android.view.View.VISIBLE
                } else {
                    tvCatchArea.visibility = android.view.View.GONE
                }

                // 클릭 이벤트
                root.setOnClickListener { onItemClick(marketInfo) }

                // 카드 색상 (시장 상황에 따라)
                val cardBackgroundColor = when (marketInfo.marketCondition) {
                    MarketCondition.STRONG -> Color.parseColor("#E8F5E8")
                    MarketCondition.WEAK -> Color.parseColor("#FFEBEE")
                    MarketCondition.VOLATILE -> Color.parseColor("#FFF3E0")
                    else -> Color.parseColor("#F5F5F5")
                }
                root.setCardBackgroundColor(cardBackgroundColor)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FishMarketInfo>() {
        override fun areItemsTheSame(oldItem: FishMarketInfo, newItem: FishMarketInfo): Boolean {
            return oldItem.fishName == newItem.fishName
        }

        override fun areContentsTheSame(oldItem: FishMarketInfo, newItem: FishMarketInfo): Boolean {
            return oldItem == newItem
        }
    }
}