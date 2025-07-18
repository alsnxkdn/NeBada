package com.example.nebada

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class WeatherDetailsFragment : Fragment() {

    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_weather_details, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadWeatherDetails(view)
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout?.setOnRefreshListener {
            view?.let { loadWeatherDetails(it) }
        }
    }

    private fun loadWeatherDetails(view: View) {
        // 실제 구현에서는 날씨 API를 호출해야 함
        // 현재는 더미 데이터로 설정
        view.findViewById<TextView>(R.id.tv_location)?.text = "한반도 근해"
        view.findViewById<TextView>(R.id.tv_current_condition)?.text = "맑음"
        view.findViewById<TextView>(R.id.tv_current_temp)?.text = "23°C"
        view.findViewById<TextView>(R.id.tv_wind_direction)?.text = "남동풍"
        view.findViewById<TextView>(R.id.tv_wind_speed)?.text = "12.0m/s"
        view.findViewById<TextView>(R.id.tv_wave_height)?.text = "1.5m"
        view.findViewById<TextView>(R.id.tv_visibility)?.text = "15.0km"
        view.findViewById<TextView>(R.id.tv_pressure)?.text = "1013.2hPa"
        view.findViewById<TextView>(R.id.tv_humidity)?.text = "65%"
        view.findViewById<TextView>(R.id.tv_last_update)?.text = "14:30"
        view.findViewById<TextView>(R.id.tv_fishing_condition)?.text = "좋음"
        view.findViewById<TextView>(R.id.tv_fishing_score)?.text = "조업 적합도: 75점"
        view.findViewById<TextView>(R.id.tv_recommendation)?.text = "✅ 조업에 좋은 조건입니다. 기상 변화를 주의하세요."

        // 위험 요소가 있을 경우에만 표시
        val hasRiskFactors = false // 실제로는 날씨 데이터에 따라 결정
        val layoutRiskFactors = view.findViewById<View>(R.id.layout_risk_factors)
        if (hasRiskFactors) {
            layoutRiskFactors?.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tv_risk_factors)?.text = "주의사항:\n⚠️ 강풍 (15.2m/s)"
        } else {
            layoutRiskFactors?.visibility = View.GONE
        }

        swipeRefreshLayout?.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        swipeRefreshLayout = null
    }
}