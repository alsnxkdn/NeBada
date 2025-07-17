// app/src/main/java/com/example/nebada/manager/VoiceRecognitionManager.kt
package com.example.nebada.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import java.util.*

class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    interface VoiceRecognitionCallback {
        fun onSpeechStart()
        fun onSpeechEnd()
        fun onSpeechResult(text: String)
        fun onSpeechError(error: String)
        fun onPermissionRequired()
    }

    /**
     * 음성 인식 권한이 있는지 확인
     */
    fun hasRecordAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 음성 인식 기능이 사용 가능한지 확인
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * 음성 인식 시작
     */
    fun startListening(callback: VoiceRecognitionCallback) {
        if (!hasRecordAudioPermission()) {
            callback.onPermissionRequired()
            return
        }

        if (!isSpeechRecognitionAvailable()) {
            callback.onSpeechError("음성 인식 기능을 사용할 수 없습니다")
            return
        }

        if (isListening) {
            stopListening()
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                callback.onSpeechStart()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                callback.onSpeechEnd()
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 오류"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식할 수 없습니다"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기가 사용 중입니다"
                    SpeechRecognizer.ERROR_SERVER -> "서버 오류"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 시간 초과"
                    else -> "알 수 없는 오류"
                }
                callback.onSpeechError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    callback.onSpeechResult(recognizedText)
                } else {
                    callback.onSpeechError("음성을 인식할 수 없습니다")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "어획 정보를 말씀해주세요")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    /**
     * 음성 인식 중지
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    /**
     * 현재 음성 인식 중인지 확인
     */
    fun isListening(): Boolean {
        return isListening
    }

    /**
     * 리소스 정리
     */
    fun destroy() {
        stopListening()
    }

    /**
     * 음성 텍스트를 어획 데이터로 파싱
     */
    fun parseVoiceToFishingData(voiceText: String): FishingVoiceData {
        val text = voiceText.lowercase(Locale.getDefault())

        return FishingVoiceData(
            fishType = extractFishType(text),
            weight = extractWeight(text),
            quantity = extractQuantity(text),
            location = extractLocation(text),
            weather = extractWeather(text),
            method = extractMethod(text),
            price = extractPrice(text), // 가격 추출 추가
            notes = voiceText // 원본 텍스트도 메모로 저장
        )
    }

    private fun extractFishType(text: String): String? {
        val fishTypes = listOf(
            "고등어", "갈치", "삼치", "전갱이", "아지", "멸치",
            "정어리", "참돔", "농어", "광어", "가자미", "우럭",
            "볼락", "조기", "갑오징어", "문어", "주꾸미", "새우", "게"
        )

        return fishTypes.find { fishType ->
            text.contains(fishType)
        }
    }

    private fun extractWeight(text: String): Double? {
        val weightRegex = """(\d+(?:\.\d+)?)\s*(?:킬로|키로|킬로그램|kg|키로그램)""".toRegex()
        val match = weightRegex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractQuantity(text: String): Int? {
        val quantityRegex = """(\d+)\s*(?:마리|개|미)""".toRegex()
        val match = quantityRegex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLocation(text: String): String? {
        val locations = listOf(
            "앞바다", "근해", "동해", "서해", "남해", "제주",
            "부산", "인천", "목포", "포항", "울산", "여수"
        )

        return locations.find { location ->
            text.contains(location)
        }
    }

    private fun extractWeather(text: String): String? {
        val weathers = listOf(
            "맑음", "흐림", "비", "바람", "태풍", "안개", "눈"
        )

        return weathers.find { weather ->
            text.contains(weather)
        }
    }

    private fun extractMethod(text: String): String? {
        val methods = listOf(
            "그물", "낚시", "통발", "선망", "정치망", "트롤", "연승"
        )

        return methods.find { method ->
            text.contains(method)
        }
    }

    /**
     * 가격 추출 기능 (새로 추가)
     */
    private fun extractPrice(text: String): Double? {
        // 다양한 가격 표현 패턴을 인식
        val pricePatterns = listOf(
            // "킬로당 오만원", "키로당 50000원"
            """킬로당\s*([가-힣0-9]+)\s*원""".toRegex(),
            """키로당\s*([가-힣0-9]+)\s*원""".toRegex(),
            """킬로그램당\s*([가-힣0-9]+)\s*원""".toRegex(),

            // "가격 오만원", "단가 50000원"
            """(?:가격|단가|값)\s*([가-힣0-9]+)\s*원""".toRegex(),

            // "오만원", "50000원", "5만원"
            """([가-힣0-9]+)\s*원""".toRegex(),

            // "오만", "50000" (원 없이)
            """(?:가격|단가|값)?\s*([가-힣0-9]+)(?:\s*원)?""".toRegex()
        )

        for (pattern in pricePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val priceString = match.groupValues[1].trim()

                // 숫자로 된 가격인 경우 먼저 확인
                val numericPrice = priceString.replace(",", "").toDoubleOrNull()
                if (numericPrice != null && numericPrice > 0) {
                    return when {
                        text.contains("만원") && numericPrice < 100 -> numericPrice * 10000
                        text.contains("천원") && numericPrice < 1000 -> numericPrice * 1000
                        else -> numericPrice
                    }
                }

                // 한글 숫자로 된 가격인 경우
                val koreanPrice = convertKoreanNumberToDouble(priceString)
                if (koreanPrice != null && koreanPrice > 0) {
                    return koreanPrice
                }
            }
        }

        return null
    }

    /**
     * 한글 숫자를 숫자로 변환 (개선된 버전)
     */
    private fun convertKoreanNumberToDouble(koreanNumber: String): Double? {
        try {
            // 기본 한글 숫자 매핑
            val koreanToNumber = mapOf(
                "영" to 0, "공" to 0, "제로" to 0,
                "일" to 1, "한" to 1, "하나" to 1,
                "이" to 2, "두" to 2, "둘" to 2,
                "삼" to 3, "세" to 3, "셋" to 3,
                "사" to 4, "네" to 4, "넷" to 4,
                "오" to 5, "다섯" to 5,
                "육" to 6, "여섯" to 6,
                "칠" to 7, "일곱" to 7,
                "팔" to 8, "여덟" to 8,
                "구" to 9, "아홉" to 9,
                "십" to 10, "열" to 10,
                "백" to 100, "천" to 1000, "만" to 10000, "억" to 100000000
            )

            var input = koreanNumber.toLowerCase().trim()

            // 특별한 케이스들 먼저 처리
            when (input) {
                "오만", "오만원", "5만", "5만원" -> return 50000.0
                "십만", "십만원", "10만", "10만원" -> return 100000.0
                "이십만", "20만", "20만원" -> return 200000.0
                "삼십만", "30만", "30만원" -> return 300000.0
                "사십만", "40만", "40만원" -> return 400000.0
                "육십만", "60만", "60만원" -> return 600000.0
                "칠십만", "70만", "70만원" -> return 700000.0
                "팔십만", "80만", "80만원" -> return 800000.0
                "구십만", "90만", "90만원" -> return 900000.0
                "백만", "100만", "100만원" -> return 1000000.0
            }

            // "만원", "천원" 제거
            input = input.replace("원", "").trim()

            var result = 0.0
            var temp = 0.0
            var currentNumber = 0.0

            var i = 0
            while (i < input.length) {
                val char = input[i].toString()

                // 숫자인 경우
                if (char.matches(Regex("[0-9]"))) {
                    currentNumber = currentNumber * 10 + char.toInt()
                    i++
                    continue
                }

                // 한글 숫자 처리
                val value = koreanToNumber[char] ?: 0

                when (value) {
                    in 1..9 -> {
                        currentNumber = value.toDouble()
                    }
                    10 -> { // 십
                        if (currentNumber == 0.0) currentNumber = 1.0
                        temp += currentNumber * 10
                        currentNumber = 0.0
                    }
                    100 -> { // 백
                        if (currentNumber == 0.0) currentNumber = 1.0
                        temp += currentNumber * 100
                        currentNumber = 0.0
                    }
                    1000 -> { // 천
                        if (currentNumber == 0.0) currentNumber = 1.0
                        temp += currentNumber * 1000
                        currentNumber = 0.0
                    }
                    10000 -> { // 만
                        if (temp == 0.0 && currentNumber == 0.0) {
                            currentNumber = 1.0
                        }
                        result += (temp + currentNumber) * 10000
                        temp = 0.0
                        currentNumber = 0.0
                    }
                    100000000 -> { // 억
                        if (temp == 0.0 && currentNumber == 0.0) {
                            currentNumber = 1.0
                        }
                        result += (temp + currentNumber) * 100000000
                        temp = 0.0
                        currentNumber = 0.0
                    }
                }
                i++
            }

            result += temp + currentNumber
            return if (result > 0) result else null

        } catch (e: Exception) {
            // 예외 발생 시 패턴 매칭으로 특별한 케이스들 처리
            return when (koreanNumber.toLowerCase().replace("원", "").trim()) {
                "오만" -> 50000.0
                "십만" -> 100000.0
                "이십만" -> 200000.0
                "삼십만" -> 300000.0
                "사십만" -> 400000.0
                "오십만" -> 500000.0
                "육십만" -> 600000.0
                "칠십만" -> 700000.0
                "팔십만" -> 800000.0
                "구십만" -> 900000.0
                "백만" -> 1000000.0
                else -> null
            }
        }
    }

    /**
     * 음성으로 인식된 어획 데이터 클래스 (가격 필드 추가)
     */
    data class FishingVoiceData(
        val fishType: String? = null,
        val weight: Double? = null,
        val quantity: Int? = null,
        val location: String? = null,
        val weather: String? = null,
        val method: String? = null,
        val price: Double? = null, // 가격 필드 추가
        val notes: String = ""
    )
}