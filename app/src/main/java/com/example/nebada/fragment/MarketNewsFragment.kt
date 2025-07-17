// app/src/main/java/com/example/nebada/fragment/MarketNewsFragment.kt
package com.example.nebada.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebada.adapter.MarketNewsAdapter
import com.example.nebada.databinding.FragmentMarketNewsBinding
import com.example.nebada.manager.LocationManager
import com.example.nebada.manager.MarketInfoManager
import com.example.nebada.model.MarketNews
import com.example.nebada.model.NewsCategory
import kotlinx.coroutines.launch

class MarketNewsFragment : Fragment() {

    private var _binding: FragmentMarketNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var marketManager: MarketInfoManager
    private lateinit var locationManager: LocationManager
    private lateinit var adapter: MarketNewsAdapter

    private var allNews: List<MarketNews> = emptyList()
    private var filteredNews: List<MarketNews> = emptyList()

    private var selectedCategory: String = "전체"
    private var selectedRegion: String = "전체"
    private var autoSelectedRegion: String? = null

    // 위치 권한 요청 런처
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted && coarseLocationGranted) {
            getCurrentLocationAndSetRegion()
        } else {
            showLocationPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketManager = MarketInfoManager(requireContext())
        locationManager = LocationManager(requireContext())

        setupRecyclerView()
        setupSpinners()
        setupSwipeRefresh()

        // GPS 기반 지역 자동 선택 시도
        requestLocationAndSetRegion()

        loadMarketNews()
    }

    private fun setupRecyclerView() {
        adapter = MarketNewsAdapter { news ->
            openNewsUrl(news.url)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MarketNewsFragment.adapter
        }
    }

    private fun setupSpinners() {
        // 카테고리 스피너 설정
        val categoryItems = listOf(
            "전체", "가격정보", "날씨영향", "규제정보", "시장동향", "일반"
        )
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryItems)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        // 지역 스피너 설정
        val regionItems = listOf(
            "전체", "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
        )
        val regionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, regionItems)
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRegion.adapter = regionAdapter

        // 스피너 리스너 설정
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categoryItems[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerRegion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRegion = regionItems[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadMarketNews()
        }
    }

    /**
     * 위치 권한 요청 및 지역 설정
     */
    private fun requestLocationAndSetRegion() {
        when {
            locationManager.hasLocationPermission() -> {
                getCurrentLocationAndSetRegion()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                showLocationPermissionRationaleDialog()
            }
            else -> {
                requestLocationPermissions()
            }
        }
    }

    /**
     * 위치 권한 요청
     */
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * 위치 권한 설명 다이얼로그
     */
    private fun showLocationPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("위치 권한 필요")
            .setMessage("현재 지역의 뉴스를 자동으로 선택하려면 위치 권한이 필요합니다. 권한을 허용하시겠습니까?")
            .setPositiveButton("허용") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("취소") { _, _ ->
                Toast.makeText(requireContext(), "수동으로 지역을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 위치 권한 거부 다이얼로그
     */
    private fun showLocationPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("위치 권한 필요")
            .setMessage("지역 뉴스 자동 선택을 위해 설정에서 위치 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("취소") { _, _ ->
                Toast.makeText(requireContext(), "수동으로 지역을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 현재 위치를 가져와서 지역 설정
     */
    private fun getCurrentLocationAndSetRegion() {
        if (!locationManager.isGpsEnabled()) {
            showGpsDisabledDialog()
            return
        }

        // 로딩 상태 표시
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            locationManager.getCurrentRegion { region ->
                binding.swipeRefreshLayout.isRefreshing = false

                if (region != null && region != "전국") {
                    autoSelectedRegion = region
                    setRegionSpinner(region)
                    Toast.makeText(
                        requireContext(),
                        "📍 현재 위치: $region (자동 선택됨)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "현재 위치를 확인할 수 없어 전체 지역으로 설정합니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * GPS 비활성화 다이얼로그
     */
    private fun showGpsDisabledDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("GPS 비활성화")
            .setMessage("위치 서비스가 비활성화되어 있습니다. GPS를 활성화하시겠습니까?")
            .setPositiveButton("설정") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("취소") { _, _ ->
                Toast.makeText(requireContext(), "수동으로 지역을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 지역 스피너 설정
     */
    private fun setRegionSpinner(region: String) {
        val regionItems = listOf(
            "전체", "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
        )

        val index = regionItems.indexOf(region)
        if (index >= 0) {
            binding.spinnerRegion.setSelection(index)
        }
    }

    private fun loadMarketNews() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true

                val news = marketManager.getMarketNews()
                allNews = news
                applyFilters()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "뉴스를 불러오는데 실패했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun applyFilters() {
        filteredNews = allNews.filter { news ->
            val categoryMatch = selectedCategory == "전체" ||
                    getCategoryDisplayName(news.category) == selectedCategory

            val regionMatch = selectedRegion == "전체" || news.region == selectedRegion

            categoryMatch && regionMatch
        }

        adapter.submitList(filteredNews)
        updateEmptyState()
        updateFilterStatus()
    }

    private fun getCategoryDisplayName(category: NewsCategory): String {
        return when (category) {
            NewsCategory.PRICE -> "가격정보"
            NewsCategory.WEATHER -> "날씨영향"
            NewsCategory.REGULATION -> "규제정보"
            NewsCategory.MARKET -> "시장동향"
            NewsCategory.GENERAL -> "일반"
        }
    }

    private fun updateEmptyState() {
        if (filteredNews.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    private fun updateFilterStatus() {
        val statusText = when {
            selectedCategory == "전체" && selectedRegion == "전체" ->
                "전체 뉴스 ${filteredNews.size}개 표시 중"
            selectedCategory == "전체" -> {
                val locationIcon = if (selectedRegion == autoSelectedRegion) "📍 " else ""
                "${locationIcon}${selectedRegion} 지역 뉴스 ${filteredNews.size}개 표시 중"
            }
            selectedRegion == "전체" ->
                "${selectedCategory} 뉴스 ${filteredNews.size}개 표시 중"
            else -> {
                val locationIcon = if (selectedRegion == autoSelectedRegion) "📍 " else ""
                "${locationIcon}${selectedRegion} ${selectedCategory} 뉴스 ${filteredNews.size}개 표시 중"
            }
        }

        binding.tvFilterStatus.text = statusText
    }

    /**
     * 뉴스 URL을 웹브라우저에서 열기
     */
    private fun openNewsUrl(url: String?) {
        if (url.isNullOrBlank()) {
            Toast.makeText(requireContext(), "기사 링크를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
                Toast.makeText(requireContext(), "뉴스 원문을 여는 중...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "웹브라우저를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "링크를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정에서 돌아온 경우 권한 상태 다시 확인
        if (locationManager.hasLocationPermission() && autoSelectedRegion == null) {
            getCurrentLocationAndSetRegion()
        }
    }
    /**
     * 외부에서 호출 가능한 새로고침 메소드
     */
    fun refreshNews() {
        loadMarketNews()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}