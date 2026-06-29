package com.diary.app.ui.personalyearbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class YearbookData(
    val year: Int,
    val totalEntries: Int,
    val totalWords: Int,
    val topMood: Int?,
    val bestMonth: String,
    val longestStreak: Int,
    val monthlyDistribution: List<Int>,
    val moodDistribution: Map<Int, Int>
)

class PersonalYearbookViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _yearbook = MutableStateFlow<YearbookData?>(null)
    val yearbook: StateFlow<YearbookData?> = _yearbook

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    fun generate(year: Int) {
        viewModelScope.launch {
            _isGenerating.value = true

            val entries = dao.getAllEntriesOnce()

            val yearEntries = entries.filter {
                val date = Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                date.year == year
            }

            val totalEntries = yearEntries.size
            val totalWords = yearEntries.sumOf { it.plainText.length }

            val monthlyDistribution = MutableList(12) { 0 }
            yearEntries.forEach {
                val month = Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().monthValue
                monthlyDistribution[month - 1]++
            }

            val moodDistribution = yearEntries.mapNotNull { it.moodLevel }.groupingBy { it }.eachCount()
            val topMood = moodDistribution.maxByOrNull { it.value }?.key

            val monthNames = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
            val bestMonthIndex = monthlyDistribution.indices.maxByOrNull { monthlyDistribution[it] } ?: 0
            val bestMonth = monthNames[bestMonthIndex]

            val daysOfYear = yearEntries.map {
                Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().dayOfYear
            }.distinct().sorted()

            var longestStreak = 0
            var currentStreak = 0
            var prev = -2
            for (day in daysOfYear) {
                if (day == prev + 1) {
                    currentStreak++
                } else {
                    currentStreak = 1
                }
                longestStreak = maxOf(longestStreak, currentStreak)
                prev = day
            }

            _yearbook.value = YearbookData(
                year = year,
                totalEntries = totalEntries,
                totalWords = totalWords,
                topMood = topMood,
                bestMonth = bestMonth,
                longestStreak = longestStreak,
                monthlyDistribution = monthlyDistribution,
                moodDistribution = moodDistribution
            )

            _isGenerating.value = false
        }
    }
}
