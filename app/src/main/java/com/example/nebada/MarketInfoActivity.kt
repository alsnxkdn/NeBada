package com.example.nebada

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nebada.databinding.ActivityMarketInfoBinding
import com.example.nebada.fragment.MarketListFragment
import com.example.nebada.fragment.MarketNewsFragment
import com.example.nebada.fragment.PriceAlertsFragment

class MarketInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMarketInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            // 기본으로 시장정보 목록 화면 표시
            showFragment(MarketListFragment())
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "실시간 시장정보"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_market_list -> {
                    showFragment(MarketListFragment())
                    true
                }
                R.id.nav_market_news -> {
                    showFragment(MarketNewsFragment())
                    true
                }
                R.id.nav_price_alerts -> {
                    showFragment(PriceAlertsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}