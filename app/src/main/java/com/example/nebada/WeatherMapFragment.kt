package com.example.nebada

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nebada.databinding.FragmentWeatherMapBinding

class WeatherMapFragment : Fragment() {

    private var _binding: FragmentWeatherMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWeatherMap()
        setupButtons()
    }

    private fun setupWeatherMap() {
        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl("https://earth.nullschool.net/#current/wind/surface/level/orthographic=-233.04,37.16,1024/loc=127.024,37.532")
        }
    }

    private fun setupButtons() {
        binding.btnPrecipitation.setOnClickListener {
            loadWeatherLayer("precipitation")
        }

        binding.btnWind.setOnClickListener {
            loadWeatherLayer("wind")
        }

        binding.btnWaves.setOnClickListener {
            loadWeatherLayer("waves")
        }

        binding.btnTemperature.setOnClickListener {
            loadWeatherLayer("temperature")
        }

        binding.btnRefresh.setOnClickListener {
            binding.webViewMap.reload()
        }
    }

    private fun loadWeatherLayer(layer: String) {
        val url = when (layer) {
            "precipitation" -> "https://earth.nullschool.net/#current/wind/surface/level/orthographic=-233.04,37.16,1024/loc=127.024,37.532"
            "wind" -> "https://earth.nullschool.net/#current/wind/surface/level/orthographic=-233.04,37.16,1024/loc=127.024,37.532"
            "waves" -> "https://earth.nullschool.net/#current/ocean/primary/waves/orthographic=-233.04,37.16,1024/loc=127.024,37.532"
            "temperature" -> "https://earth.nullschool.net/#current/ocean/surface/currents/orthographic=-233.04,37.16,1024/loc=127.024,37.532"
            else -> "https://earth.nullschool.net/#current/wind/surface/level/orthographic=-233.04,37.16,1024/loc=127.024,37.532"
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.webViewMap.loadUrl(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}