package com.example.nebada

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nebada.databinding.ActivityWeatherForecastBinding
import com.example.nebada.fragment.WeatherMapFragment
import com.example.nebada.fragment.WeatherDetailsFragment
import com.example.nebada.fragment.MarineConditionsFragment

class WeatherForecastActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherForecastBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            // 기본으로 날씨 지도 화면 표시
            showFragment(WeatherMapFragment())
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "날씨 예보"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather_map -> {
                    showFragment(WeatherMapFragment())
                    true
                }
                R.id.nav_weather_details -> {
                    showFragment(WeatherDetailsFragment())
                    true
                }
                R.id.nav_marine_conditions -> {
                    showFragment(MarineConditionsFragment())
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