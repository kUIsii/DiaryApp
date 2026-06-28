package com.diary.app.ui.quarterlyreview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class QuarterlyData(
    val year: Int,
    val quarter: Int,
    val totalEntries: Int,
    val avgMood: Float,
    val topMood: Int,
    val totalWords: Int,
    val topTags: List<String>,
    val moodTrend: List<Float>
)

class QuarterlyReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _quarterlyData = MutableStateFlow<QuarterlyData?>(null)
    val quarterlyData: StateFlow<QuarterlyData?> = _quarterlyData

    init {
        loadCurrentQuarter()
    }

    fun loadCurrentQuarter() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val quarter = (now.monthValue - 1) / 3 + 1
            val startMonth = (quarter - 1) * 3 + 1
            val startDate = LocalDate.of(now.year, startMonth, 1)
            val endDate = startDate.plusMonths(3)

            val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val entries = dao.getPreviewsByDateRange(startMillis, endMillis)
            val moods = entries.mapNotNull { it.moodLevel }
            val avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else 0f
            val topMood = moods.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
            val totalWords = entries.sumOf { it.plainText.length }

            val tagUsage = dao.getTagUsageOnce()
            val topTags = tagUsage.sortedByDescending { it.count }.take(5).map { it.name }

            val moodTrend = entries.map { it.moodLevel?.toFloat() ?: 0f }

            _quarterlyData.value = QuarterlyData(
                year = now.year,
                quarter = quarter,
                totalEntries = entries.size,
                avgMood = avgMood,
                topMood = topMood,
                totalWords = totalWords,
                topTags = topTags,
                moodTrend = moodTrend
            )
        }
    }
}
