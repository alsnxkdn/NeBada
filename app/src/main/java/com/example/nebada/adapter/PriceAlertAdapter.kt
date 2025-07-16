package com.example.nebada.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nebada.databinding.ItemPriceAlertBinding
import com.example.nebada.model.PriceAlert
import com.example.nebada.manager.AlertType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PriceAlertAdapter(
    private val onDeleteClick: (PriceAlert) -> Unit,
    private val onToggleClick: (PriceAlert) -> Unit
) : ListAdapter<PriceAlert, PriceAlertAdapter.PriceAlertViewHolder>(DiffCallback()) {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceAlertViewHolder {
        val binding = ItemPriceAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PriceAlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PriceAlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PriceAlertViewHolder(
        private val binding: ItemPriceAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: PriceAlert) {
            binding.apply {
                // 어종명
                tvFishName.text = alert.fishName

                // 목표 가격
                tvTargetPrice.text = "${numberFormat.format(alert.targetPrice.toInt())}원/kg"

                // 알림 조건
                tvAlertCondition.text = when (alert.alertType) {
                    AlertType.PRICE_ABOVE -> "가격이 목표가 이상일 때"
                    AlertType.PRICE_BELOW -> "가격이 목표가 이하일 때"
                    AlertType.SUPPLY_CHANGE -> "공급 상태 변화 시"
                }

                // 생성일
                tvCreatedDate.text = "생성일: ${dateFormat.format(alert.createdDate)}"

                // 활성화 상태
                switchEnabled.isChecked = alert.isEnabled
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != alert.isEnabled) {
                        onToggleClick(alert)
                    }
                }

                // 삭제 버튼
                btnDelete.setOnClickListener {
                    onDeleteClick(alert)
                }

                // 비활성화 상태일 때 투명도 조정
                root.alpha = if (alert.isEnabled) 1.0f else 0.6f
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PriceAlert>() {
        override fun areItemsTheSame(oldItem: PriceAlert, newItem: PriceAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PriceAlert, newItem: PriceAlert): Boolean {
            return oldItem == newItem
        }
    }
}