// app/src/main/java/com/example/nebada/IntroActivity.kt 수정
package com.example.nebada

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nebada.databinding.ActivityIntroBinding
import com.example.nebada.manager.LocationManager

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = LocationManager(this)

        // 메뉴 카드 클릭 리스너 설정
        setupMenuClickListeners()
        updateLocationStatus()

        // 긴급상황 버튼 클릭 리스너
        binding.btnEmergency.setOnClickListener {
            // 긴급상황 처리
            // TODO: 긴급상황 신고 기능 구현
        }
    }

    private fun updateLocationStatus() {
        // GPS 상태에 따라 UI 업데이트
        if (locationManager.hasLocationPermission() && locationManager.isGpsEnabled()) {
            // GPS 사용 가능한 상태 표시는 각 기능에서 처리
        }
    }

    private fun setupMenuClickListeners() {
        // 항해 메뉴
        binding.cardNavigation.setOnClickListener {
            navigateToMainActivity("navigation")
        }

        // 어획관리 메뉴
        binding.cardCatchRecord.setOnClickListener {
            // 어획관리 Activity로 직접 이동
            val intent = Intent(this, CatchManagementActivity::class.java)
            startActivity(intent)
        }

        // 시장정보 메뉴 (GPS 상태 체크 추가)
        binding.cardMarketInfo.setOnClickListener {
            // 시장정보 Activity로 이동
            val intent = Intent(this, MarketInfoActivity::class.java)

            // GPS 사용 가능 여부를 인텐트에 추가
            if (locationManager.hasLocationPermission() && locationManager.isGpsEnabled()) {
                Toast.makeText(this, "📍 위치 기반 지역 뉴스를 제공합니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "💡 위치 권한을 허용하면 지역 뉴스를 자동으로 볼 수 있습니다", Toast.LENGTH_LONG).show()
            }

            startActivity(intent)
        }

        // 어민 커뮤니티 메뉴
        binding.cardCommunity.setOnClickListener {
            navigateToMainActivity("community")
        }

        // 도구 메뉴
        binding.cardTools.setOnClickListener {
            navigateToMainActivity("tools")
        }

        // 설정 메뉴
        binding.cardSettings.setOnClickListener {
            navigateToMainActivity("settings")
        }

        // 빠른 액션 버튼들
        binding.btnStartFishing.setOnClickListener {
            navigateToMainActivity("start_fishing")
        }

        binding.btnCheckWeather.setOnClickListener {
            navigateToMainActivity("check_weather")
        }

        binding.btnRecordCatch.setOnClickListener {
            // 어획 기록 화면으로 직접 이동
            val intent = Intent(this, CatchManagementActivity::class.java)
            intent.putExtra("direct_to_record", true)
            startActivity(intent)
        }
    }

    private fun navigateToMainActivity(section: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("section", section)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateLocationStatus()
    }
}