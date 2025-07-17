package com.example.nebada.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebada.adapter.MarketNewsAdapter
import com.example.nebada.databinding.FragmentMarketNewsBinding
import com.example.nebada.manager.MarketInfoManager
import com.example.nebada.model.MarketNews
import com.example.nebada.model.NewsCategory
import kotlinx.coroutines.launch

class MarketNewsFragment : Fragment() {

    private var _binding: FragmentMarketNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var marketManager: MarketInfoManager
    private lateinit var adapter: MarketNewsAdapter

    private var allNews: List<MarketNews> = emptyList()
    private var filteredNews: List<MarketNews> = emptyList()

    private var selectedCategory: String = "전체"
    private var selectedRegion: String = "전체"

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
        setupRecyclerView()
        setupSpinners()
        setupSwipeRefresh()
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
            selectedCategory == "전체" ->
                "${selectedRegion} 지역 뉴스 ${filteredNews.size}개 표시 중"
            selectedRegion == "전체" ->
                "${selectedCategory} 뉴스 ${filteredNews.size}개 표시 중"
            else ->
                "${selectedRegion} ${selectedCategory} 뉴스 ${filteredNews.size}개 표시 중"
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
            // URL이 http:// 또는 https://로 시작하지 않으면 https:// 추가
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 웹브라우저가 설치되어 있는지 확인
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}