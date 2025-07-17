package com.example.nebada.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nebada.databinding.ItemWeatherWarningBinding
import com.example.nebada.model.WeatherWarning
import java.text.SimpleDateFormat
import java.util.*

class WeatherWarningAdapter(
    private val onItemClick: (WeatherWarning) -> Unit
) : ListAdapter<WeatherWarning, WeatherWarningAdapter.WarningViewHolder>(DiffCallback()) {

    private val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningViewHolder {
        val binding = ItemWeatherWarningBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WarningViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WarningViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WarningViewHolder(
        private val binding: ItemWeatherWarningBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(warning: WeatherWarning) {
            binding.apply {
                // 특보 유형 아이콘
                tvWarningIcon.text = getWarningIcon(warning.type)

                // 특보 제목
                tvWarningTitle.text = warning.title

                // 심각도
                tvSeverity.text = warning.severity.displayName
                tvSeverity.setTextColor(Color.parseColor(warning.severity.color))

                // 발효/해제 시간
                tvValidTime.text = "발효: ${dateTimeFormat.format(warning.validFrom)}\n" +
                        "해제: ${dateTimeFormat.format(warning.validTo)}"

                // 영향 지역
                tvAffectedAreas.text = "영향지역: ${warning.affectedAreas.joinToString(", ")}"

                // 상세 설명
                tvDescription.text = warning.description

                // 카드 배경색 (심각도에 따라)
                val backgroundColor = when (warning.severity) {
                    com.example.nebada.model.WarningSeverity.VERY_HIGH -> Color.parseColor("#FCE4EC")
                    com.example.nebada.model.WarningSeverity.HIGH -> Color.parseColor("#FFEBEE")
                    com.example.nebada.model.WarningSeverity.MEDIUM -> Color.parseColor("#FFF3E0")
                    com.example.nebada.model.WarningSeverity.LOW -> Color.parseColor("#F1F8E9")
                }
                root.setCardBackgroundColor(backgroundColor)

                // 만료된 특보는 흐리게 표시
                val isExpired = warning.validTo.before(Date())
                root.alpha = if (isExpired) 0.6f else 1.0f

                // 클릭 이벤트
                root.setOnClickListener { onItemClick(warning) }
            }
        }

        private fun getWarningIcon(type: com.example.nebada.model.WarningType): String {
            return when (type) {
                com.example.nebada.model.WarningType.TYPHOON -> "🌀"
                com.example.nebada.model.WarningType.STRONG_WIND -> "💨"
                com.example.nebada.model.WarningType.HIGH_WAVES -> "🌊"
                com.example.nebada.model.WarningType.HEAVY_RAIN -> "🌧️"
                com.example.nebada.model.WarningType.FOG -> "🌫️"
                com.example.nebada.model.WarningType.DRY_WARNING -> "🔥"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<WeatherWarning>() {
        override fun areItemsTheSame(oldItem: WeatherWarning, newItem: WeatherWarning): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WeatherWarning, newItem: WeatherWarning): Boolean {
            return oldItem == newItem
        }
    }
}