package com.example.nebada.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebada.adapter.PriceAlertAdapter
import com.example.nebada.databinding.FragmentPriceAlertsBinding
import com.example.nebada.manager.AlertType
import com.example.nebada.manager.MarketInfoManager
import com.example.nebada.model.PriceAlert

class PriceAlertsFragment : Fragment() {

    private var _binding: FragmentPriceAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var marketManager: MarketInfoManager
    private lateinit var adapter: PriceAlertAdapter
    private val alerts = mutableListOf<PriceAlert>()

    // 주요 어종 목록
    private val fishTypes = arrayOf(
        "고등어", "갈치", "삼치", "전갱이", "멸치", "정어리",
        "참돔", "농어", "광어", "가자미", "우럭", "볼락",
        "조기", "갑오징어", "문어", "주꾸미", "새우", "게"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPriceAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketManager = MarketInfoManager(requireContext())
        setupRecyclerView()
        setupFab()
        loadPriceAlerts()
    }

    private fun setupRecyclerView() {
        adapter = PriceAlertAdapter(
            onDeleteClick = { alert ->
                deletePriceAlert(alert)
            },
            onToggleClick = { alert ->
                togglePriceAlert(alert)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PriceAlertsFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAddAlert.setOnClickListener {
            showAddAlertDialog()
        }
    }

    private fun showAddAlertDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.example.nebada.R.layout.dialog_add_price_alert, null)

        val spinnerFish = dialogView.findViewById<Spinner>(com.example.nebada.R.id.spinner_fish)
        val spinnerAlertType = dialogView.findViewById<Spinner>(com.example.nebada.R.id.spinner_alert_type)
        val etTargetPrice = dialogView.findViewById<EditText>(com.example.nebada.R.id.et_target_price)

        // 어종 스피너 설정
        val fishAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fishTypes)
        fishAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFish.adapter = fishAdapter

        // 알림 타입 스피너 설정
        val alertTypes = arrayOf("가격 이상", "가격 이하")
        val alertTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, alertTypes)
        alertTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAlertType.adapter = alertTypeAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("가격 알림 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val fishName = fishTypes[spinnerFish.selectedItemPosition]
                val alertType = if (spinnerAlertType.selectedItemPosition == 0)
                    AlertType.PRICE_ABOVE else AlertType.PRICE_BELOW
                val targetPriceText = etTargetPrice.text.toString()

                if (targetPriceText.isBlank()) {
                    Toast.makeText(requireContext(), "목표 가격을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val targetPrice = targetPriceText.toDoubleOrNull()
                if (targetPrice == null || targetPrice <= 0) {
                    Toast.makeText(requireContext(), "올바른 가격을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addPriceAlert(fishName, targetPrice, alertType)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun addPriceAlert(fishName: String, targetPrice: Double, alertType: AlertType) {
        val alert = PriceAlert(
            id = "alert_${System.currentTimeMillis()}",
            fishName = fishName,
            targetPrice = targetPrice,
            alertType = alertType,
            isEnabled = true,
            createdDate = java.util.Date()
        )

        alerts.add(alert)
        marketManager.setPriceAlert(fishName, targetPrice, alertType)

        adapter.submitList(alerts.toList())
        updateEmptyState()

        Toast.makeText(
            requireContext(),
            "$fishName 가격 알림이 추가되었습니다",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun deletePriceAlert(alert: PriceAlert) {
        AlertDialog.Builder(requireContext())
            .setTitle("알림 삭제")
            .setMessage("${alert.fishName} 가격 알림을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                alerts.remove(alert)
                adapter.submitList(alerts.toList())
                updateEmptyState()
                Toast.makeText(requireContext(), "알림이 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun togglePriceAlert(alert: PriceAlert) {
        val index = alerts.indexOf(alert)
        if (index != -1) {
            alerts[index] = alert.copy(isEnabled = !alert.isEnabled)
            adapter.submitList(alerts.toList())

            val status = if (alerts[index].isEnabled) "활성화" else "비활성화"
            Toast.makeText(requireContext(), "알림이 ${status}되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPriceAlerts() {
        // 실제 구현 시에는 저장된 알림 목록을 불러와야 함
        // 현재는 예시 데이터
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (alerts.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}