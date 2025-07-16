package com.example.nebada.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.nebada.MarketDetailActivity
import com.example.nebada.adapter.MarketInfoAdapter
import com.example.nebada.databinding.FragmentMarketListBinding
import com.example.nebada.manager.MarketInfoManager
import com.example.nebada.model.FishMarketInfo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MarketListFragment : Fragment() {

    private var _binding: FragmentMarketListBinding? = null
    private val binding get() = _binding!!

    private lateinit var marketManager: MarketInfoManager
    private lateinit var adapter: MarketInfoAdapter
    private val dateFormat = SimpleDateFormat("HH:mm 업데이트", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketManager = MarketInfoManager(requireContext())
        setupRecyclerView()
        setupSwipeRefresh()
        loadMarketData()
    }

    private fun setupRecyclerView() {
        adapter = MarketInfoAdapter { marketInfo ->
            // 상세 화면으로 이동
            val intent = Intent(requireContext(), MarketDetailActivity::class.java)
            intent.putExtra("fish_name", marketInfo.fishName)
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MarketListFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadMarketData()
        }
    }

    private fun loadMarketData() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true

                val marketData = marketManager.getMarketInfo()
                adapter.submitList(marketData)

                // 마지막 업데이트 시간 표시
                binding.tvLastUpdate.text = "마지막 업데이트: ${dateFormat.format(Date())}"

                // 빈 상태 처리
                if (marketData.isEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "시장정보를 불러오는데 실패했습니다: ${e.message}",
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

    override fun onDestroy() {
        super.onDestroy()
        if (::marketManager.isInitialized) {
            marketManager.destroy()
        }
    }
}