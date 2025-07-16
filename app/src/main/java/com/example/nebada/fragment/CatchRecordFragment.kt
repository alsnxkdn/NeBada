package com.example.nebada.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nebada.databinding.FragmentCatchRecordBinding
import com.example.nebada.manager.CatchRecordManager
import com.example.nebada.model.CatchRecord
import java.text.SimpleDateFormat
import java.util.*

class CatchRecordFragment : Fragment() {

    private var _binding: FragmentCatchRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var catchManager: CatchRecordManager
    private var selectedDate: Date = Date()
    private var editingRecordId: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 어종 목록
    private val fishTypes = arrayOf(
        "고등어", "갈치", "삼치", "전갱이", "아지", "멸치",
        "정어리", "참돔", "농어", "광어", "가자미", "우럭",
        "볼락", "조기", "갑오징어", "문어", "주꾸미", "새우",
        "게", "전복", "기타"
    )

    // 어법 목록
    private val fishingMethods = arrayOf(
        "그물어업", "낚시어업", "통발어업", "선망어업",
        "정치망어업", "트롤어업", "연승어업", "기타"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatchRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catchManager = CatchRecordManager(requireContext())
        setupSpinners()
        setupDateTimePickers()
        setupButtons()

        // 수정 모드인지 확인
        arguments?.getString("record_id")?.let { recordId ->
            editingRecordId = recordId
            loadRecordForEdit(recordId)
        }

        // 초기 날짜/시간 설정
        updateDateTimeDisplay()
    }

    private fun setupSpinners() {
        // 어종 스피너 - 수정된 ID 사용
        val fishTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fishTypes)
        fishTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFishType.adapter = fishTypeAdapter

        // 어법 스피너 - 수정된 ID 사용
        val methodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fishingMethods)
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMethod.adapter = methodAdapter
    }

    private fun setupDateTimePickers() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveCatchRecord()
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnGetCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.time = selectedDate
                newCalendar.set(year, month, dayOfMonth)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance()
                newCalendar.time = selectedDate
                newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCalendar.set(Calendar.MINUTE, minute)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateTimeDisplay() {
        binding.tvSelectedDate.text = dateFormat.format(selectedDate)
        binding.tvSelectedTime.text = timeFormat.format(selectedDate)
    }

    private fun getCurrentLocation() {
        // GPS 위치 가져오기 (실제 구현 시 위치 권한 요청 필요)
        // 여기서는 예시 위치 설정
        binding.etLatitude.setText("35.1796")
        binding.etLongitude.setText("129.0756")
        binding.etLocation.setText("부산 남구 앞바다")

        Toast.makeText(requireContext(), "현재 위치를 가져왔습니다", Toast.LENGTH_SHORT).show()
    }

    private fun saveCatchRecord() {
        // 입력 검증
        if (!validateInput()) {
            return
        }

        // 스피너에서 선택된 값 가져오기 (수정된 부분)
        val selectedFishType = fishTypes[binding.spinnerFishType.selectedItemPosition]
        val selectedMethod = fishingMethods[binding.spinnerMethod.selectedItemPosition]

        val record = CatchRecord(
            id = editingRecordId ?: "",
            fishType = selectedFishType,
            weight = binding.etWeight.text.toString().toDoubleOrNull() ?: 0.0,
            quantity = binding.etQuantity.text.toString().toIntOrNull() ?: 0,
            location = binding.etLocation.text.toString(),
            latitude = binding.etLatitude.text.toString().toDoubleOrNull() ?: 0.0,
            longitude = binding.etLongitude.text.toString().toDoubleOrNull() ?: 0.0,
            date = selectedDate,
            weather = binding.etWeather.text.toString(),
            waterTemp = binding.etWaterTemp.text.toString().toDoubleOrNull() ?: 0.0,
            depth = binding.etDepth.text.toString().toDoubleOrNull() ?: 0.0,
            method = selectedMethod,
            notes = binding.etNotes.text.toString(),
            price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        )

        val success = if (editingRecordId != null) {
            catchManager.updateCatchRecord(record)
        } else {
            catchManager.addCatchRecord(record)
            true
        }

        if (success) {
            Toast.makeText(
                requireContext(),
                if (editingRecordId != null) "어획 기록이 수정되었습니다" else "어획 기록이 저장되었습니다",
                Toast.LENGTH_SHORT
            ).show()
            parentFragmentManager.popBackStack()
        } else {
            Toast.makeText(requireContext(), "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput(): Boolean {
        if (binding.etWeight.text.toString().isEmpty()) {
            binding.etWeight.error = "무게를 입력해주세요"
            return false
        }

        if (binding.etQuantity.text.toString().isEmpty()) {
            binding.etQuantity.error = "수량을 입력해주세요"
            return false
        }

        if (binding.etLocation.text.toString().isEmpty()) {
            binding.etLocation.error = "어획 위치를 입력해주세요"
            return false
        }

        return true
    }

    private fun loadRecordForEdit(recordId: String) {
        val record = catchManager.getRecordById(recordId)
        record?.let {
            binding.etWeight.setText(it.weight.toString())
            binding.etQuantity.setText(it.quantity.toString())
            binding.etLocation.setText(it.location)
            binding.etLatitude.setText(it.latitude.toString())
            binding.etLongitude.setText(it.longitude.toString())
            binding.etWeather.setText(it.weather)
            binding.etWaterTemp.setText(it.waterTemp.toString())
            binding.etDepth.setText(it.depth.toString())
            binding.etNotes.setText(it.notes)
            binding.etPrice.setText(it.price.toString())

            selectedDate = it.date
            updateDateTimeDisplay()

            // 스피너 선택 (수정된 부분)
            val fishTypeIndex = fishTypes.indexOf(it.fishType)
            if (fishTypeIndex >= 0) {
                binding.spinnerFishType.setSelection(fishTypeIndex)
            }

            val methodIndex = fishingMethods.indexOf(it.method)
            if (methodIndex >= 0) {
                binding.spinnerMethod.setSelection(methodIndex)
            }

            // 저장 버튼 텍스트 변경
            binding.btnSave.text = "수정하기"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}