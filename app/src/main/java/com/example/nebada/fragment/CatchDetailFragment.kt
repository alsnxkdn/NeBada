package com.example.nebada.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nebada.databinding.FragmentCatchDetailBinding
import com.example.nebada.manager.CatchRecordManager
import com.example.nebada.model.CatchRecord
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CatchDetailFragment : Fragment() {

    private var _binding: FragmentCatchDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var catchManager: CatchRecordManager
    private var catchRecord: CatchRecord? = null

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catchManager = CatchRecordManager(requireContext())

        // 전달받은 record_id로 데이터 로드
        val recordId = arguments?.getString("record_id")
        if (recordId != null) {
            loadCatchRecord(recordId)
        } else {
            Toast.makeText(requireContext(), "잘못된 접근입니다", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        setupButtons()
    }

    private fun loadCatchRecord(recordId: String) {
        catchRecord = catchManager.getRecordById(recordId)

        catchRecord?.let { record ->
            displayCatchRecord(record)
        } ?: run {
            Toast.makeText(requireContext(), "어획 기록을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun displayCatchRecord(record: CatchRecord) {
        binding.apply {
            // 기본 정보
            tvFishType.text = record.fishType
            tvDateTime.text = dateFormat.format(record.date)
            tvLocation.text = record.location

            // 수량 및 무게
            tvWeight.text = "${numberFormat.format(record.weight)}kg"
            tvQuantity.text = "${record.quantity}마리"

            // 가격 정보
            if (record.price > 0) {
                tvPrice.text = "${numberFormat.format(record.price.toInt())}원/kg"
                tvTotalValue.text = "${numberFormat.format(record.totalValue.toInt())}원"
                layoutPrice.visibility = View.VISIBLE
            } else {
                layoutPrice.visibility = View.GONE
            }

            // 위치 정보
            if (record.latitude != 0.0 && record.longitude != 0.0) {
                tvCoordinates.text = "위도: ${String.format("%.6f", record.latitude)}, 경도: ${String.format("%.6f", record.longitude)}"
                layoutCoordinates.visibility = View.VISIBLE
            } else {
                layoutCoordinates.visibility = View.GONE
            }

            // 환경 정보
            setupEnvironmentInfo(record)

            // 어법
            if (record.method.isNotEmpty()) {
                tvMethod.text = record.method
                layoutMethod.visibility = View.VISIBLE
            } else {
                layoutMethod.visibility = View.GONE
            }

            // 메모
            if (record.notes.isNotEmpty()) {
                tvNotes.text = record.notes
                layoutNotes.visibility = View.VISIBLE
            } else {
                layoutNotes.visibility = View.GONE
            }
        }
    }

    private fun setupEnvironmentInfo(record: CatchRecord) {
        val environmentInfo = mutableListOf<String>()

        if (record.weather.isNotEmpty()) {
            environmentInfo.add("날씨: ${record.weather}")
        }

        if (record.waterTemp > 0) {
            environmentInfo.add("수온: ${record.waterTemp}°C")
        }

        if (record.depth > 0) {
            environmentInfo.add("수심: ${numberFormat.format(record.depth)}m")
        }

        if (environmentInfo.isNotEmpty()) {
            binding.tvEnvironment.text = environmentInfo.joinToString(" • ")
            binding.layoutEnvironment.visibility = View.VISIBLE
        } else {
            binding.layoutEnvironment.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            editRecord()
        }

        binding.btnDelete.setOnClickListener {
            deleteRecord()
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun editRecord() {
        catchRecord?.let { record ->
            val fragment = CatchRecordFragment().apply {
                arguments = Bundle().apply {
                    putString("record_id", record.id)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun deleteRecord() {
        catchRecord?.let { record ->
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("어획 기록 삭제")
                .setMessage("'${record.fishType}' 어획 기록을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    val success = catchManager.deleteCatchRecord(record.id)
                    if (success) {
                        Toast.makeText(requireContext(), "어획 기록이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}