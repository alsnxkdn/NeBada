package com.example.nebada.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nebada.databinding.ItemMarketNewsBinding
import com.example.nebada.model.MarketNews
import java.text.SimpleDateFormat
import java.util.*

class MarketNewsAdapter(
    private val onItemClick: (MarketNews) -> Unit
) : ListAdapter<MarketNews, MarketNewsAdapter.NewsViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemMarketNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(
        private val binding: ItemMarketNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: MarketNews) {
            binding.apply {
                // 제목
                tvNewsTitle.text = news.title

                // 내용 (요약)
                tvNewsContent.text = if (news.content.length > 100) {
                    "${news.content.substring(0, 100)}..."
                } else {
                    news.content
                }

                // 발행일시
                tvPublishDate.text = dateFormat.format(news.publishDate)

                // 카테고리와 지역 정보
                tvCategory.text = news.category.displayName

                // 지역 정보 추가 (카테고리 옆에 표시)
                val categoryAndRegion = if (news.region != "전국") {
                    "${news.category.displayName} • ${news.region}"
                } else {
                    news.category.displayName
                }
                tvCategory.text = categoryAndRegion

                // 중요도에 따른 색상
                val importanceColor = Color.parseColor(news.importance.color)
                tvImportance.text = news.importance.displayName
                tvImportance.setTextColor(importanceColor)

                // 카테고리별 배경 색상
                val categoryColor = when (news.category) {
                    com.example.nebada.model.NewsCategory.PRICE -> Color.parseColor("#E3F2FD")
                    com.example.nebada.model.NewsCategory.WEATHER -> Color.parseColor("#F3E5F5")
                    com.example.nebada.model.NewsCategory.REGULATION -> Color.parseColor("#FFEBEE")
                    com.example.nebada.model.NewsCategory.MARKET -> Color.parseColor("#E8F5E8")
                    else -> Color.parseColor("#F5F5F5")
                }
                root.setCardBackgroundColor(categoryColor)

                // 클릭 이벤트 - 뉴스 객체를 전달
                root.setOnClickListener { onItemClick(news) }

                // URL이 있을 때만 클릭 가능하도록 시각적 피드백
                if (news.url.isNullOrBlank()) {
                    root.alpha = 0.7f
                    root.isClickable = false
                    root.foreground = null
                } else {
                    root.alpha = 1.0f
                    root.isClickable = true
                    root.foreground = android.graphics.drawable.ColorDrawable(
                        android.graphics.Color.parseColor("#1000FF00")
                    )

                    // 클릭 가능함을 나타내는 추가 표시
                    tvNewsTitle.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, android.R.drawable.ic_menu_view, 0
                    )
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MarketNews>() {
        override fun areItemsTheSame(oldItem: MarketNews, newItem: MarketNews): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MarketNews, newItem: MarketNews): Boolean {
            return oldItem == newItem
        }
    }
}