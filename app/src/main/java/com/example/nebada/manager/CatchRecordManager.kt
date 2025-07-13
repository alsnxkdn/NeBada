package com.example.nebada.manager

import android.content.Context
import android.content.SharedPreferences
import com.example.nebada.model.CatchRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * 어획 기록 관리 클래스
 * SharedPreferences를 사용하여 데이터를 로컬에 저장
 */
class CatchRecordManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("catch_records", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CATCH_RECORDS = "catch_records_list"
        private const val KEY_LAST_ID = "last_catch_id"
    }

    /**
     * 새로운 어획 기록 추가
     */
    fun addCatchRecord(record: CatchRecord): String {
        val records = getAllRecords().toMutableList()
        val newId = generateNewId()
        val newRecord = record.copy(
            id = newId,
            totalValue = record.calculateTotalValue()
        )

        records.add(newRecord)
        saveRecords(records)
        return newId
    }

    /**
     * 어획 기록 수정
     */
    fun updateCatchRecord(record: CatchRecord): Boolean {
        val records = getAllRecords().toMutableList()
        val index = records.indexOfFirst { it.id == record.id }

        return if (index != -1) {
            val updatedRecord = record.copy(totalValue = record.calculateTotalValue())
            records[index] = updatedRecord
            saveRecords(records)
            true
        } else {
            false
        }
    }

    /**
     * 어획 기록 삭제
     */
    fun deleteCatchRecord(id: String): Boolean {
        val records = getAllRecords().toMutableList()
        val removed = records.removeIf { it.id == id }

        if (removed) {
            saveRecords(records)
        }
        return removed
    }

    /**
     * 모든 어획 기록 조회
     */
    fun getAllRecords(): List<CatchRecord> {
        val json = prefs.getString(KEY_CATCH_RECORDS, "[]")
        val type = object : TypeToken<List<CatchRecord>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * 특정 ID의 어획 기록 조회
     */
    fun getRecordById(id: String): CatchRecord? {
        return getAllRecords().find { it.id == id }
    }

    /**
     * 날짜별 어획 기록 조회
     */
    fun getRecordsByDate(date: Date): List<CatchRecord> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = dateFormat.format(date)

        return getAllRecords().filter {
            dateFormat.format(it.date) == targetDate
        }
    }

    /**
     * 어종별 어획 기록 조회
     */
    fun getRecordsByFishType(fishType: String): List<CatchRecord> {
        return getAllRecords().filter { it.fishType == fishType }
    }

    /**
     * 기간별 어획 기록 조회
     */
    fun getRecordsByDateRange(startDate: Date, endDate: Date): List<CatchRecord> {
        return getAllRecords().filter {
            it.date >= startDate && it.date <= endDate
        }
    }

    /**
     * 통계 데이터 계산
     */
    fun getStatistics(): CatchStatistics {
        val records = getAllRecords()

        return CatchStatistics(
            totalRecords = records.size,
            totalWeight = records.sumOf { it.weight },
            totalQuantity = records.sumOf { it.quantity },
            totalValue = records.sumOf { it.totalValue },
            averageWeight = if (records.isNotEmpty()) records.sumOf { it.weight } / records.size else 0.0,
            mostCaughtFish = records.groupBy { it.fishType }
                .maxByOrNull { it.value.size }?.key ?: "",
            fishTypeCount = records.map { it.fishType }.distinct().size
        )
    }

    /**
     * 월별 통계 데이터
     */
    fun getMonthlyStatistics(year: Int, month: Int): CatchStatistics {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startDate = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val records = getRecordsByDateRange(startDate, endDate)

        return CatchStatistics(
            totalRecords = records.size,
            totalWeight = records.sumOf { it.weight },
            totalQuantity = records.sumOf { it.quantity },
            totalValue = records.sumOf { it.totalValue },
            averageWeight = if (records.isNotEmpty()) records.sumOf { it.weight } / records.size else 0.0,
            mostCaughtFish = records.groupBy { it.fishType }
                .maxByOrNull { it.value.size }?.key ?: "",
            fishTypeCount = records.map { it.fishType }.distinct().size
        )
    }

    private fun generateNewId(): String {
        val lastId = prefs.getInt(KEY_LAST_ID, 0) + 1
        prefs.edit().putInt(KEY_LAST_ID, lastId).apply()
        return "catch_$lastId"
    }

    private fun saveRecords(records: List<CatchRecord>) {
        val json = gson.toJson(records)
        prefs.edit().putString(KEY_CATCH_RECORDS, json).apply()
    }

    /**
     * 모든 데이터 삭제 (주의: 복구 불가능)
     */
    fun clearAllRecords() {
        prefs.edit().clear().apply()
    }
}

/**
 * 어획 통계 데이터 클래스
 */
data class CatchStatistics(
    val totalRecords: Int,
    val totalWeight: Double,
    val totalQuantity: Int,
    val totalValue: Double,
    val averageWeight: Double,
    val mostCaughtFish: String,
    val fishTypeCount: Int
)