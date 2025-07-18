package com.example.nebada

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class MarineConditionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 해상특보를 위한 간단한 레이아웃 생성
        val view = TextView(requireContext()).apply {
            text = """
                🌊 해상특보 정보
                
                현재 특보: 없음
                
                예상 특보:
                • 강풍주의보 - 내일 오후 2시
                • 파고 1.5~2.5m 예상
                
                주의사항:
                • 소형 선박 운항 주의
                • 연안 조업 시 안전 확인 필수
            """.trimIndent()

            textSize = 16f
            setPadding(32, 32, 32, 32)
        }

        return view
    }
}