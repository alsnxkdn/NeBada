package com.example.nebada.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nebada.databinding.ItemCatchRecordBinding
import com.example.nebada.model.CatchRecord
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CatchRecordAdapter(
    private val onItemClick: (CatchRecord) -> Unit,
    private val onEditClick: (CatchRecord) -> Unit,
    private val onDeleteClick: (CatchRecord) -> Unit
) : ListAdapter<CatchRecord, CatchRecordAdapter.CatchRecordViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatchRecordViewHolder {
        val binding = ItemCatchRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CatchRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatchRecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CatchRecordViewHolder(
        private val binding: ItemCatchRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: CatchRecord) {
            binding.apply {
                // 기본 정보
                tvFishType.text = record.fishType
                tvDateTime.text = dateFormat.format(record.date)
                tvLocation.text = record.location

                // 수량 및 무게
                tvWeight.text = "${numberFormat.format(record.weight)}kg"
                tvQuantity.text = "${record.quantity}마리"

                // 가격 정보
                if (record.totalValue > 0) {
                    tvTotalValue.text = "${numberFormat.format(record.totalValue.toInt())}원"
                    tvTotalValue.visibility = android.view.View.VISIBLE
                } else {
                    tvTotalValue.visibility = android.view.View.GONE
                }

                // 어법
                if (record.method.isNotEmpty()) {
                    tvMethod.text = record.method
                    tvMethod.visibility = android.view.View.VISIBLE
                } else {
                    tvMethod.visibility = android.view.View.GONE
                }

                // 날씨 정보
                if (record.weather.isNotEmpty()) {
                    tvWeather.text = record.weather
                    tvWeather.visibility = android.view.View.VISIBLE
                } else {
                    tvWeather.visibility = android.view.View.GONE
                }

                // 클릭 이벤트
                root.setOnClickListener { onItemClick(record) }
                btnEdit.setOnClickListener { onEditClick(record) }
                btnDelete.setOnClickListener { onDeleteClick(record) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CatchRecord>() {
        override fun areItemsTheSame(oldItem: CatchRecord, newItem: CatchRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CatchRecord, newItem: CatchRecord): Boolean {
            return oldItem == newItem
        }
    }
}