package com.diary.app.ui.quarterlyreview

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class MoodTrendPoint(
    val date: LocalDate,
    val mood: Float,
    val title: String
)

data class Highlight(
    val entryId: Long,
    val title: String,
    val date: Long,
    val reason: String
)

data class QuarterlyData(
    val year: Int,
    val quarter: Int,
    val totalEntries: Int,
    val avgMood: Float,
    val topMood: Int,
    val totalWords: Int,
    val topTags: List<String>,
    val moodTrend: List<Float>,
    val moodTrendDetails: List<MoodTrendPoint>,
    val monthlyDistribution: List<Pair<Int, Int>>,
    val dailyAvgEntries: Float,
    val longestStreak: Int,
    val monthlyMood: List<Pair<Int, Float>>,
    val moodRating: String,
    val favoriteHourRange: String,
    val avgDurationMinutes: Int,
    val weekendRatio: Float,
    val highlights: List<Highlight>
)

class QuarterlyReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = (application as DiaryApplication).aiService
    private val sp = application.getSharedPreferences("quarterly_review", Context.MODE_PRIVATE)

    val maxQuarter = LocalDate.now().let {
        Pair(it.year, (it.monthValue - 1) / 3 + 1)
    }

    private val _quarterlyData = MutableStateFlow<QuarterlyData?>(null)
    val quarterlyData: StateFlow<QuarterlyData?> = _quarterlyData

    private val _currentYear = MutableStateFlow(LocalDate.now().year)
    private val _currentQuarter = MutableStateFlow((LocalDate.now().monthValue - 1) / 3 + 1)
    val currentYear: StateFlow<Int> = _currentYear
    val currentQuarter: StateFlow<Int> = _currentQuarter

    private val _isComparisonMode = MutableStateFlow(false)
    val isComparisonMode: StateFlow<Boolean> = _isComparisonMode

    private val _compareYear = MutableStateFlow(LocalDate.now().year)
    private val _compareQuarter = MutableStateFlow((LocalDate.now().monthValue - 1) / 3 + 1)
    val compareYear: StateFlow<Int> = _compareYear
    val compareQuarter: StateFlow<Int> = _compareQuarter

    private val _compareData = MutableStateFlow<QuarterlyData?>(null)
    val compareData: StateFlow<QuarterlyData?> = _compareData

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary

    private val _aiSummaryLoading = MutableStateFlow(false)
    val aiSummaryLoading: StateFlow<Boolean> = _aiSummaryLoading

    private val _aiSummaryCountLeft = MutableStateFlow(3)
    val aiSummaryCountLeft: StateFlow<Int> = _aiSummaryCountLeft

    private val _expandedSections = MutableStateFlow<Set<String>>(emptySet())
    val expandedSections: StateFlow<Set<String>> = _expandedSections

    init {
        loadQuarter(_currentYear.value, _currentQuarter.value)
    }

    fun loadQuarter(year: Int, quarter: Int) {
        _currentYear.value = year
        _currentQuarter.value = quarter
        viewModelScope.launch {
            _quarterlyData.value = computeQuarterlyData(year, quarter)
            loadAiSummary(year, quarter)
            if (_isComparisonMode.value) {
                _compareData.value = computeQuarterlyData(_compareYear.value, _compareQuarter.value)
            }
        }
    }

    fun switchToPrevQuarter() {
        val (y, q) = prevQuarter(_currentYear.value, _currentQuarter.value)
        loadQuarter(y, q)
    }

    fun switchToNextQuarter() {
        val (y, q) = nextQuarter(_currentYear.value, _currentQuarter.value)
        if (y < maxQuarter.first || (y == maxQuarter.first && q <= maxQuarter.second)) {
            loadQuarter(y, q)
        }
    }

    fun loadCompareQuarter(year: Int, quarter: Int) {
        _compareYear.value = year
        _compareQuarter.value = quarter
        viewModelScope.launch {
            _compareData.value = computeQuarterlyData(year, quarter)
        }
    }

    fun switchCompareToPrevQuarter() {
        val (y, q) = prevQuarter(_compareYear.value, _compareQuarter.value)
        loadCompareQuarter(y, q)
    }

    fun switchCompareToNextQuarter() {
        val (y, q) = nextQuarter(_compareYear.value, _compareQuarter.value)
        if (y < maxQuarter.first || (y == maxQuarter.first && q <= maxQuarter.second)) {
            loadCompareQuarter(y, q)
        }
    }

    fun toggleComparisonMode() {
        _isComparisonMode.value = !_isComparisonMode.value
        if (_isComparisonMode.value && _compareData.value == null) {
            val (y, q) = prevQuarter(_currentYear.value, _currentQuarter.value)
            loadCompareQuarter(y, q)
        }
    }

    fun toggleSection(section: String) {
        val current = _expandedSections.value.toMutableSet()
        if (current.contains(section)) current.remove(section) else current.add(section)
        _expandedSections.value = current
    }

    fun generateAiSummary() {
        val data = _quarterlyData.value ?: return
        val countKey = "ai_summary_count_${data.year}_${data.quarter}"
        val count = sp.getInt(countKey, 0)
        if (count >= 3) return
        if (!aiService.isAiEnabled()) return

        _aiSummaryLoading.value = true
        viewModelScope.launch {
            val userMessage = "用户该季度写了${data.totalEntries}篇日记，主要标签${data.topTags.joinToString("、")}，平均心情${"%.1f".format(data.avgMood)}，总字数${data.totalWords}。"
            val systemPrompt = "你是一个日记分析助手。根据用户的季度日记摘要，写一段100字左右的季度总结。包含写作概况、情绪洞察、亮点时刻、鼓励话语。语气温暖亲切。"
            val request = aiRequest(userMessage = userMessage, systemPrompt = systemPrompt)
            aiService.chat(request).onSuccess { response ->
                val summary = response.content
                val cacheKey = "ai_quarterly_summary_${data.year}_${data.quarter}"
                sp.edit().putString(cacheKey, summary).apply()
                sp.edit().putInt(countKey, count + 1).apply()
                _aiSummary.value = summary
                _aiSummaryCountLeft.value = 2 - count
                _aiSummaryLoading.value = false
            }.onFailure {
                _aiSummaryLoading.value = false
            }
        }
    }

    private fun loadAiSummary(year: Int, quarter: Int) {
        val cached = sp.getString("ai_quarterly_summary_${year}_${quarter}", null)
        val count = sp.getInt("ai_summary_count_${year}_${quarter}", 0)
        _aiSummary.value = cached
        _aiSummaryCountLeft.value = (3 - count).coerceAtLeast(0)
        _aiSummaryLoading.value = false
    }

    private fun prevQuarter(year: Int, quarter: Int): Pair<Int, Int> {
        return if (quarter == 1) Pair(year - 1, 4) else Pair(year, quarter - 1)
    }

    private fun nextQuarter(year: Int, quarter: Int): Pair<Int, Int> {
        return if (quarter == 4) Pair(year + 1, 1) else Pair(year, quarter + 1)
    }

    private suspend fun computeQuarterlyData(year: Int, quarter: Int): QuarterlyData {
        val (startMillis, endMillis) = quarterDateRange(year, quarter)
        val entries = dao.getEntriesByDateRange(startMillis, endMillis)
        val moods = entries.mapNotNull { it.moodLevel }
        val avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else 0f
        val topMood = moods.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
        val totalWords = entries.sumOf { it.plainText.length }

        val tagUsage = dao.getTagUsageOnce()
        val topTags = tagUsage.sortedByDescending { it.count }.take(5).map { it.name }

        val sortedEntries = entries.sortedBy { it.createdAt }
        val moodTrend = sortedEntries.map { it.moodLevel?.toFloat() ?: 0f }
        val moodTrendDetails = sortedEntries.map { e ->
            val ldt = Instant.ofEpochMilli(e.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            MoodTrendPoint(
                date = ldt.toLocalDate(),
                mood = e.moodLevel?.toFloat() ?: 0f,
                title = e.title
            )
        }

        val monthlyDistribution = monthlyDistribution(entries)
        val dailyAvgEntries = if (entries.isNotEmpty()) entries.size / 90f else 0f
        val longestStreak = calculateLongestStreak(entries)
        val monthlyMood = monthlyMoodAvg(entries)
        val moodRating = calculateMoodRating(monthlyMood)
        val favoriteHourRange = calculateFavoriteHourRange(entries)
        val durations = entries.mapNotNull { it.writingDurationSeconds }
        val avgDurationMinutes = if (durations.isNotEmpty()) durations.average().toInt() / 60 else 0
        val weekendRatio = calculateWeekendRatio(entries)
        val highlights = extractHighlights(entries)

        return QuarterlyData(
            year = year,
            quarter = quarter,
            totalEntries = entries.size,
            avgMood = avgMood,
            topMood = topMood,
            totalWords = totalWords,
            topTags = topTags,
            moodTrend = moodTrend,
            moodTrendDetails = moodTrendDetails,
            monthlyDistribution = monthlyDistribution,
            dailyAvgEntries = dailyAvgEntries,
            longestStreak = longestStreak,
            monthlyMood = monthlyMood,
            moodRating = moodRating,
            favoriteHourRange = favoriteHourRange,
            avgDurationMinutes = avgDurationMinutes,
            weekendRatio = weekendRatio,
            highlights = highlights
        )
    }

    private fun quarterDateRange(year: Int, quarter: Int): Pair<Long, Long> {
        val startMonth = (quarter - 1) * 3 + 1
        val startDate = LocalDate.of(year, startMonth, 1)
        val endDate = startDate.plusMonths(3)
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Pair(startMillis, endMillis)
    }

    private fun monthlyDistribution(entries: List<com.diary.app.data.DiaryEntry>): List<Pair<Int, Int>> {
        return entries.groupBy {
            Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime().monthValue
        }.mapValues { it.value.size }
            .entries.sortedBy { it.key }
            .map { Pair(it.key, it.value) }
    }

    private fun calculateLongestStreak(entries: List<com.diary.app.data.DiaryEntry>): Int {
        if (entries.isEmpty()) return 0
        val dates = entries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }.distinct().sorted()
        var maxStreak = 1
        var currentStreak = 1
        for (i in 1 until dates.size) {
            if (ChronoUnit.DAYS.between(dates[i - 1], dates[i]) == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        return maxStreak
    }

    private fun monthlyMoodAvg(entries: List<com.diary.app.data.DiaryEntry>): List<Pair<Int, Float>> {
        return entries.groupBy {
            Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime().monthValue
        }.mapValues { group ->
            val moods = group.value.mapNotNull { it.moodLevel }
            if (moods.isNotEmpty()) moods.average().toFloat() else 0f
        }.entries.sortedBy { it.key }
            .map { Pair(it.key, it.value) }
    }

    private fun calculateMoodRating(monthlyMood: List<Pair<Int, Float>>): String {
        if (monthlyMood.size < 2) return "总体稳定"
        val moods = monthlyMood.map { it.second }
        val variance = moods.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average()
        val trend = moods.last() - moods.first()
        return when {
            variance > 1.0f -> "波动较大"
            trend > 0.5f -> "持续变好"
            trend < -0.5f -> "需要关注"
            else -> "总体稳定"
        }
    }

    private fun calculateFavoriteHourRange(entries: List<com.diary.app.data.DiaryEntry>): String {
        if (entries.isEmpty()) return "--"
        val hours = entries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime().hour
        }
        val hourCounts = hours.groupBy { it }.mapValues { it.value.size }
        val bestHour = hourCounts.maxByOrNull { it.value }?.key ?: return "--"
        val start = (bestHour / 2) * 2
        return "${start.toString().padStart(2, '0')}:00-${(start + 2).toString().padStart(2, '0')}:00"
    }

    private fun calculateWeekendRatio(entries: List<com.diary.app.data.DiaryEntry>): Float {
        if (entries.isEmpty()) return 0f
        var weekendCount = 0
        var weekdayCount = 0
        entries.forEach {
            val dayOfWeek = Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime().dayOfWeek
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                weekendCount++
            } else {
                weekdayCount++
            }
        }
        val weekendDays = 26f
        val weekdayDays = 64f
        val weekendRate = weekendCount / weekendDays
        val weekdayRate = weekdayCount / weekdayDays
        return if (weekdayRate > 0) weekendRate / weekdayRate else 0f
    }

    private fun extractHighlights(entries: List<com.diary.app.data.DiaryEntry>): List<Highlight> {
        if (entries.isEmpty()) return emptyList()
        val sortedByWordCount = entries.sortedByDescending { it.plainText.length }
        val sortedByMoodDesc = entries.sortedByDescending { it.moodLevel ?: 0 }
        val sortedByMoodAsc = entries.sortedBy { it.moodLevel ?: 0 }

        val longest = sortedByWordCount.firstOrNull()
        val happiest = sortedByMoodDesc.firstOrNull()
        val saddestLong = sortedByMoodAsc.firstOrNull { (it.moodLevel ?: 0) <= 2 && it.plainText.length >= 50 }
            ?: sortedByMoodAsc.firstOrNull { (it.moodLevel ?: 0) <= 2 }

        val result = mutableListOf<Highlight>()
        longest?.let {
            result.add(Highlight(it.id, it.title.take(20), it.createdAt, "字数最多"))
        }
        happiest?.let {
            if (result.none { h -> h.entryId == it.id }) {
                result.add(Highlight(it.id, it.title.take(20), it.createdAt, "心情最佳"))
            }
        }
        saddestLong?.let {
            if (result.none { h -> h.entryId == it.id }) {
                result.add(Highlight(it.id, it.title.take(20), it.createdAt, "深度倾诉"))
            }
        }
        return result
    }
}
