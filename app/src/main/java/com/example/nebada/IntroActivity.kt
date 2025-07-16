package com.example.nebada

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nebada.databinding.ActivityIntroBinding

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 메뉴 카드 클릭 리스너 설정
        setupMenuClickListeners()

        // 긴급상황 버튼 클릭 리스너
        binding.btnEmergency.setOnClickListener {
            // 긴급상황 처리
            // TODO: 긴급상황 신고 기능 구현
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

        // 시장정보 메뉴
        binding.cardMarketInfo.setOnClickListener {
            // 시장정보 Activity로 이동
            val intent = Intent(this, MarketInfoActivity::class.java)
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
}