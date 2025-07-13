package com.example.nebada

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
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
            navigateToMainActivity("catch_record")
        }
        
        // 시장정보 메뉴
        binding.cardMarketInfo.setOnClickListener {
            navigateToMainActivity("market_info")
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
            navigateToMainActivity("record_catch")
        }
    }

    private fun navigateToMainActivity(section: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("section", section)
        startActivity(intent)
        finish() // 인트로 화면 종료
    }
} 