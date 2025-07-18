package com.example.nebada

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class WeatherRadarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_forecast)

        setupToolbar()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            // 기본으로 날씨 지도 화면 표시
            showFragment(WeatherMapFragment())
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "날씨 레이더"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation?.setOnItemSelectedListener { item ->
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