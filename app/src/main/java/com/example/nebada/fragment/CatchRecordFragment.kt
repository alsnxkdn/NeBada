// app/src/main/java/com/example/nebada/fragment/CatchRecordFragment.kt
package com.example.nebada.fragment

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nebada.databinding.FragmentCatchRecordBinding
import com.example.nebada.manager.CatchRecordManager
import com.example.nebada.manager.LocationManager
import com.example.nebada.manager.VoiceRecognitionManager
import com.example.nebada.model.CatchRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CatchRecordFragment : Fragment(), VoiceRecognitionManager.VoiceRecognitionCallback {

    private var _binding: FragmentCatchRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var catchManager: CatchRecordManager
    private lateinit var voiceManager: VoiceRecognitionManager
    private lateinit var locationManager: LocationManager
    private var selectedDate: Date = Date()
    private var editingRecordId: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 현재 음성 입력 대상 필드
    private var currentVoiceInputField: VoiceInputField? = null

    enum class VoiceInputField {
        FISH_TYPE, WEIGHT, QUANTITY, LOCATION, WEATHER, METHOD, PRICE, NOTES, ALL_DATA
    }

    // 권한 요청 런처들
    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceInput()
        } else {
            Toast.makeText(requireContext(), "음성 인식 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // 권한이 허용되면 위치 정보 가져오기
                getCurrentLocationInfo()
            }
            else -> {
                Toast.makeText(requireContext(), "위치 권한을 허용해야 GPS 기능을 사용할 수 있습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        voiceManager = VoiceRecognitionManager(requireContext())
        locationManager = LocationManager(requireContext())

        setupSpinners()
        setupDateTimePickers()
        setupButtons()
        setupVoiceInputButtons()

        // 수정 모드인지 확인
        arguments?.getString("record_id")?.let { recordId ->
            editingRecordId = recordId
            loadRecordForEdit(recordId)
        } ?: run {
            // 새 기록 작성 모드일 때 자동으로 GPS 위치 가져오기
            autoGetCurrentLocation()
        }

        // 초기 날짜/시간 설정
        updateDateTimeDisplay()
    }

    /**
     * 화면 진입 시 자동으로 현재 위치 가져오기
     */
    private fun autoGetCurrentLocation() {
        when {
            locationManager.hasLocationPermission() -> {
                // 권한이 있으면 바로 위치 정보 가져오기
                getCurrentLocationInfo()
            }
            else -> {
                // 권한이 없으면 요청
                requestLocationPermission()
            }
        }
    }

    /**
     * 위치 권한 요청
     */
    private fun requestLocationPermission() {
        when {
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // 권한 설명 다이얼로그 표시
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("위치 권한 필요")
                    .setMessage("어획 위치를 자동으로 입력하려면 위치 권한이 필요합니다.")
                    .setPositiveButton("허용") { _, _ ->
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    .setNegativeButton("취소") { _, _ ->
                        Toast.makeText(requireContext(), "수동으로 위치를 입력해주세요", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
            else -> {
                // 권한 요청
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    /**
     * GPS를 이용해서 현재 위치 정보 가져오기
     */
    private fun getCurrentLocationInfo() {
        if (!locationManager.hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        if (!locationManager.isGpsEnabled()) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("GPS 비활성화")
                .setMessage("위치 서비스가 비활성화되어 있습니다. 설정에서 위치 서비스를 활성화해주세요.")
                .setPositiveButton("확인", null)
                .show()
            return
        }

        // 로딩 표시
        binding.tvLocationStatus.visibility = View.VISIBLE
        binding.tvLocationStatus.text = "📍 GPS로 위치 정보를 가져오는 중..."

        // 위치 정보 가져오기
        lifecycleScope.launch {
            try {
                locationManager.getCurrentLocationDetails { locationInfo ->
                    if (locationInfo != null) {
                        // UI 업데이트
                        binding.etLocation.setText(locationInfo.address)
                        binding.etLatitude.setText(locationInfo.latitude.toString())
                        binding.etLongitude.setText(locationInfo.longitude.toString())

                        binding.tvLocationStatus.text = "✅ 위치 정보가 자동으로 입력되었습니다"

                        // 2초 후 상태 메시지 숨기기
                        binding.tvLocationStatus.postDelayed({
                            binding.tvLocationStatus.visibility = View.GONE
                        }, 2000)

                        Toast.makeText(
                            requireContext(),
                            "현재 위치: ${locationInfo.address}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        binding.tvLocationStatus.text = "❌ 위치 정보를 가져올 수 없습니다"
                        binding.tvLocationStatus.postDelayed({
                            binding.tvLocationStatus.visibility = View.GONE
                        }, 2000)

                        Toast.makeText(
                            requireContext(),
                            "위치 정보를 가져올 수 없습니다. 수동으로 입력해주세요",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                binding.tvLocationStatus.text = "❌ 위치 정보 오류"
                binding.tvLocationStatus.postDelayed({
                    binding.tvLocationStatus.visibility = View.GONE
                }, 2000)

                Toast.makeText(
                    requireContext(),
                    "위치 정보 오류: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupVoiceInputButtons() {
        // 전체 데이터 음성 입력 버튼
        binding.btnVoiceInputAll.setOnClickListener {
            currentVoiceInputField = VoiceInputField.ALL_DATA
            requestVoiceInput()
        }

        // 각 필드별 음성 입력 버튼들
        binding.btnVoiceFishType.setOnClickListener {
            currentVoiceInputField = VoiceInputField.FISH_TYPE
            requestVoiceInput()
        }

        binding.btnVoiceWeight.setOnClickListener {
            currentVoiceInputField = VoiceInputField.WEIGHT
            requestVoiceInput()
        }

        binding.btnVoiceQuantity.setOnClickListener {
            currentVoiceInputField = VoiceInputField.QUANTITY
            requestVoiceInput()
        }

        binding.btnVoiceLocation.setOnClickListener {
            currentVoiceInputField = VoiceInputField.LOCATION
            requestVoiceInput()
        }

        binding.btnVoiceWeather.setOnClickListener {
            currentVoiceInputField = VoiceInputField.WEATHER
            requestVoiceInput()
        }

        binding.btnVoiceMethod.setOnClickListener {
            currentVoiceInputField = VoiceInputField.METHOD
            requestVoiceInput()
        }

        binding.btnVoicePrice.setOnClickListener {
            currentVoiceInputField = VoiceInputField.PRICE
            requestVoiceInput()
        }

        binding.btnVoiceNotes.setOnClickListener {
            currentVoiceInputField = VoiceInputField.NOTES
            requestVoiceInput()
        }
    }

    private fun requestVoiceInput() {
        when {
            voiceManager.hasRecordAudioPermission() -> {
                startVoiceInput()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.RECORD_AUDIO
            ) -> {
                showVoicePermissionRationale()
            }
            else -> {
                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun showVoicePermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("음성 인식 권한 필요")
            .setMessage("어획 정보를 음성으로 입력하려면 마이크 권한이 필요합니다.")
            .setPositiveButton("허용") { _, _ ->
                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun startVoiceInput() {
        if (!voiceManager.isSpeechRecognitionAvailable()) {
            Toast.makeText(requireContext(), "음성 인식 기능을 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        voiceManager.startListening(this)
    }

    // VoiceRecognitionCallback 구현
    override fun onSpeechStart() {
        binding.tvVoiceStatus.text = "🎤 음성을 듣고 있습니다..."
        binding.tvVoiceStatus.visibility = View.VISIBLE

        // 음성 입력 중임을 표시
        currentVoiceInputField?.let { field ->
            when (field) {
                VoiceInputField.ALL_DATA -> binding.btnVoiceInputAll.text = "🎤 듣는 중..."
                VoiceInputField.FISH_TYPE -> binding.btnVoiceFishType.text = "🎤"
                VoiceInputField.WEIGHT -> binding.btnVoiceWeight.text = "🎤"
                VoiceInputField.QUANTITY -> binding.btnVoiceQuantity.text = "🎤"
                VoiceInputField.LOCATION -> binding.btnVoiceLocation.text = "🎤"
                VoiceInputField.WEATHER -> binding.btnVoiceWeather.text = "🎤"
                VoiceInputField.METHOD -> binding.btnVoiceMethod.text = "🎤"
                VoiceInputField.PRICE -> binding.btnVoicePrice.text = "🎤"
                VoiceInputField.NOTES -> binding.btnVoiceNotes.text = "🎤"
            }
        }
    }

    override fun onSpeechEnd() {
        binding.tvVoiceStatus.text = "음성 인식 중..."
        resetVoiceButtons()
    }

    override fun onSpeechResult(text: String) {
        binding.tvVoiceStatus.visibility = View.GONE
        resetVoiceButtons()

        currentVoiceInputField?.let { field ->
            when (field) {
                VoiceInputField.ALL_DATA -> {
                    // 전체 데이터 파싱
                    val voiceData = voiceManager.parseVoiceToFishingData(text)
                    applyVoiceDataToForm(voiceData)
                    Toast.makeText(requireContext(), "음성 데이터가 입력되었습니다", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // 개별 필드 입력
                    applySingleFieldVoiceInput(field, text)
                }
            }
        }
    }

    override fun onSpeechError(error: String) {
        binding.tvVoiceStatus.visibility = View.GONE
        resetVoiceButtons()
        Toast.makeText(requireContext(), "음성 인식 오류: $error", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionRequired() {
        voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun resetVoiceButtons() {
        binding.btnVoiceInputAll.text = "🎤 음성으로 전체 입력"
        binding.btnVoiceFishType.text = "🎤"
        binding.btnVoiceWeight.text = "🎤"
        binding.btnVoiceQuantity.text = "🎤"
        binding.btnVoiceLocation.text = "🎤"
        binding.btnVoiceWeather.text = "🎤"
        binding.btnVoiceMethod.text = "🎤"
        binding.btnVoicePrice.text = "🎤"
        binding.btnVoiceNotes.text = "🎤"
    }

    private fun applyVoiceDataToForm(voiceData: VoiceRecognitionManager.FishingVoiceData) {
        // 어종 설정
        voiceData.fishType?.let { fishType ->
            val index = fishTypes.indexOf(fishType)
            if (index >= 0) {
                binding.spinnerFishType.setSelection(index)
            }
        }

        // 무게 설정
        voiceData.weight?.let { weight ->
            binding.etWeight.setText(weight.toString())
        }

        // 수량 설정
        voiceData.quantity?.let { quantity ->
            binding.etQuantity.setText(quantity.toString())
        }

        // 위치 설정
        voiceData.location?.let { location ->
            binding.etLocation.setText(location)
        }

        // 날씨 설정
        voiceData.weather?.let { weather ->
            binding.etWeather.setText(weather)
        }

        // 어법 설정
        voiceData.method?.let { method ->
            val methodIndex = fishingMethods.indexOfFirst { it.contains(method) }
            if (methodIndex >= 0) {
                binding.spinnerMethod.setSelection(methodIndex)
            }
        }

        // 가격 설정
        voiceData.price?.let { price ->
            binding.etPrice.setText(price.toString())
        }

        // 메모에 원본 음성 텍스트 추가
        if (voiceData.notes.isNotEmpty()) {
            val currentNotes = binding.etNotes.text.toString()
            val newNotes = if (currentNotes.isEmpty()) {
                "[음성입력] ${voiceData.notes}"
            } else {
                "$currentNotes\n[음성입력] ${voiceData.notes}"
            }
            binding.etNotes.setText(newNotes)
        }
    }

    private fun applySingleFieldVoiceInput(field: VoiceInputField, text: String) {
        when (field) {
            VoiceInputField.FISH_TYPE -> {
                val voiceData = voiceManager.parseVoiceToFishingData(text)
                voiceData.fishType?.let { fishType ->
                    val index = fishTypes.indexOf(fishType)
                    if (index >= 0) {
                        binding.spinnerFishType.setSelection(index)
                        Toast.makeText(requireContext(), "어종: $fishType", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "인식된 어종을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            VoiceInputField.WEIGHT -> {
                val voiceData = voiceManager.parseVoiceToFishingData(text)
                voiceData.weight?.let { weight ->
                    binding.etWeight.setText(weight.toString())
                    Toast.makeText(requireContext(), "무게: ${weight}kg", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "무게 정보를 인식할 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            VoiceInputField.QUANTITY -> {
                val voiceData = voiceManager.parseVoiceToFishingData(text)
                voiceData.quantity?.let { quantity ->
                    binding.etQuantity.setText(quantity.toString())
                    Toast.makeText(requireContext(), "수량: ${quantity}마리", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "수량 정보를 인식할 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            VoiceInputField.LOCATION -> {
                binding.etLocation.setText(text)
                Toast.makeText(requireContext(), "위치: $text", Toast.LENGTH_SHORT).show()
            }
            VoiceInputField.WEATHER -> {
                binding.etWeather.setText(text)
                Toast.makeText(requireContext(), "날씨: $text", Toast.LENGTH_SHORT).show()
            }
            VoiceInputField.METHOD -> {
                val voiceData = voiceManager.parseVoiceToFishingData(text)
                voiceData.method?.let { method ->
                    val methodIndex = fishingMethods.indexOfFirst { it.contains(method) }
                    if (methodIndex >= 0) {
                        binding.spinnerMethod.setSelection(methodIndex)
                        Toast.makeText(requireContext(), "어법: $method", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "인식된 어법을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            VoiceInputField.PRICE -> {
                val voiceData = voiceManager.parseVoiceToFishingData(text)
                voiceData.price?.let { price ->
                    binding.etPrice.setText(price.toString())
                    Toast.makeText(requireContext(), "가격: ${price.toInt()}원/kg", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "가격 정보를 인식할 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            VoiceInputField.NOTES -> {
                val currentNotes = binding.etNotes.text.toString()
                val newNotes = if (currentNotes.isEmpty()) {
                    text
                } else {
                    "$currentNotes\n$text"
                }
                binding.etNotes.setText(newNotes)
                Toast.makeText(requireContext(), "메모가 추가되었습니다", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    private fun setupSpinners() {
        // 어종 스피너
        val fishTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fishTypes)
        fishTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFishType.adapter = fishTypeAdapter

        // 어법 스피너
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

        // 수동 위치 가져오기 버튼 (기존 기능 유지)
        binding.btnGetCurrentLocation.setOnClickListener {
            getCurrentLocationInfo()
        }
    }

    // 나머지 메소드들은 기존과 동일...
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

    private fun saveCatchRecord() {
        if (!validateInput()) {
            return
        }

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

            val fishTypeIndex = fishTypes.indexOf(it.fishType)
            if (fishTypeIndex >= 0) {
                binding.spinnerFishType.setSelection(fishTypeIndex)
            }

            val methodIndex = fishingMethods.indexOf(it.method)
            if (methodIndex >= 0) {
                binding.spinnerMethod.setSelection(methodIndex)
            }

            binding.btnSave.text = "수정하기"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceManager.destroy()
        _binding = null
    }
}