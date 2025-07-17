package com.example.nebada.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nebada.databinding.ItemWeatherForecastBinding
import com.example.nebada.model.WeatherData
import java.text.SimpleDateFormat
import java.util.*

class WeatherForecastAdapter(
    private val onItemClick: (WeatherData) -> Unit
) : ListAdapter<WeatherData, WeatherForecastAdapter.ForecastViewHolder>(DiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemWeatherForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ForecastViewHolder(
        private val binding: ItemWeatherForecastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(weather: WeatherData) {
            binding.apply {
                // 시간 정보
                val now = Date()
                val isToday = dateFormat.format(weather.date) == dateFormat.format(now)

                if (isToday) {
                    tvTime.text = timeFormat.format(weather.date)
                } else {
                    tvTime.text = "${dateFormat.format(weather.date)}\n${timeFormat.format(weather.date)}"
                }

                // 날씨 아이콘 및 상태
                tvWeatherIcon.text = getWeatherIcon(weather.condition)
                tvCondition.text = weather.condition

                // 기온
                tvTemperature.text = "${weather.temperature}°C"

                // 바람 정보
                tvWindInfo.text = "${weather.windDirection}\n${String.format("%.1f", weather.windSpeed)}m/s"

                // 파고
                tvWaveHeight.text = "${String.format("%.1f", weather.waveHeight)}m"

                // 강수확률 (임시로 습도 기반 계산)
                val precipitationProb = when {
                    weather.condition.contains("비") -> (80..100).random()
                    weather.condition.contains("구름") -> (20..40).random()
                    weather.humidity > 80 -> (weather.humidity - 60).coerceAtMost(100)
                    else -> (0..20).random()
                }
                tvPrecipitation.text = "${precipitationProb}%"

                // 시정
                tvVisibility.text = "${String.format("%.1f", weather.visibility)}km"

                // 배경색 설정 (시간대별)
                val hour = Calendar.getInstance().apply { time = weather.date }.get(Calendar.HOUR_OF_DAY)
                val backgroundColor = when (hour) {
                    in 6..11 -> android.graphics.Color.parseColor("#FFF9C4")  // 아침 - 연한 노랑
                    in 12..17 -> android.graphics.Color.parseColor("#E3F2FD") // 낮 - 연한 파랑
                    in 18..20 -> android.graphics.Color.parseColor("#FFECB3") // 저녁 - 연한 주황
                    else -> android.graphics.Color.parseColor("#F3E5F5")      // 밤 - 연한 보라
                }
                root.setCardBackgroundColor(backgroundColor)

                // 클릭 이벤트
                root.setOnClickListener { onItemClick(weather) }

                // 위험도에 따른 테두리 표시
                val windRisk = weather.windSpeed >= 15
                val waveRisk = weather.waveHeight >= 3.0
                val visibilityRisk = weather.visibility < 5

                when {
                    windRisk || waveRisk || visibilityRisk -> {
                        root.strokeColor = android.graphics.Color.parseColor("#F44336")
                        root.strokeWidth = 4
                    }
                    weather.windSpeed >= 10 || weather.waveHeight >= 2.0 -> {
                        root.strokeColor = android.graphics.Color.parseColor("#FF9800")
                        root.strokeWidth = 2
                    }
                    else -> {
                        root.strokeColor = android.graphics.Color.parseColor("#4CAF50")
                        root.strokeWidth = 1
                    }
                }
            }
        }

        private fun getWeatherIcon(condition: String): String {
            return when {
                condition.contains("맑음") -> "☀️"
                condition.contains("구름적음") -> "🌤️"
                condition.contains("구름많음") -> "⛅"
                condition.contains("흐림") -> "☁️"
                condition.contains("비") -> "🌧️"
                condition.contains("소나기") -> "🌦️"
                condition.contains("눈") -> "❄️"
                condition.contains("안개") -> "🌫️"
                condition.contains("번개") -> "⛈️"
                else -> "🌤️"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<WeatherData>() {
        override fun areItemsTheSame(oldItem: WeatherData, newItem: WeatherData): Boolean {
            return oldItem.date == newItem.date && oldItem.location == newItem.location
        }

        override fun areContentsTheSame(oldItem: WeatherData, newItem: WeatherData): Boolean {
            return oldItem == newItem
        }
    }
}