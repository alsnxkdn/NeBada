package com.example.nebada.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebada.adapter.MarketNewsAdapter
import com.example.nebada.databinding.FragmentMarketNewsBinding
import com.example.nebada.manager.MarketInfoManager
import kotlinx.coroutines.launch

class MarketNewsFragment : Fragment() {

    private var _binding: FragmentMarketNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var marketManager: MarketInfoManager
    private lateinit var adapter: MarketNewsAdapter

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
        setupSwipeRefresh()
        loadMarketNews()
    }

    private fun setupRecyclerView() {
        adapter = MarketNewsAdapter { news ->
            // 뉴스 상세 보기 (추후 구현)
            Toast.makeText(requireContext(), news.title, Toast.LENGTH_SHORT).show()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MarketNewsFragment.adapter
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
                adapter.submitList(news)

                // 빈 상태 처리
                if (news.isEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}