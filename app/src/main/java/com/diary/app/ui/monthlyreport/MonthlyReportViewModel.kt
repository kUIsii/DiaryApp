package com.diary.app.ui.monthlyreport

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MonthlyReportViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = AiServiceManager(application)

    private val _report = MutableStateFlow<MonthlyReport?>(null)
    val report: StateFlow<MonthlyReport?> = _report.asStateFlow()

    private val _comparison = MutableStateFlow<YearComparison?>(null)
    val comparison: StateFlow<YearComparison?> = _comparison.asStateFlow()

    private val _aiReview = MutableStateFlow<String?>(null)
    val aiReview: StateFlow<String?> = _aiReview.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    fun isAiAvailable(): Boolean = aiService.isAiEnabled()

    fun generateAiReview() {
        val currentReport = _report.value ?: return
        if (_aiLoading.value) return

        viewModelScope.launch {
            _aiLoading.value = true
            try {
                val prompt = buildString {
                    append("你是一个温暖的文字伙伴。请根据以下${currentReport.year}年${currentReport.month}月的日记数据，写一段2-3句的月度回顾。")
                    append("语气温和自然，像朋友之间的对话，不要提到AI或数据分析。")
                    append("这个月共写了${currentReport.totalEntries}篇日记，${currentReport.totalWords}字。")
                    if (currentReport.avgMood != null) {
                        val moodDesc = when {
                            currentReport.avgMood >= 4f -> "心情很好"
                            currentReport.avgMood >= 3f -> "心情平稳"
                            currentReport.avgMood >= 2f -> "心情有些低落"
                            else -> "心情不太好"
                        }
                        append("平均心情${String.format("%.1f", currentReport.avgMood)}，$moodDesc。")
                    }
                    if (currentReport.totalDurationMinutes > 0) {
                        append("总写作时长${currentReport.totalDurationMinutes}分钟。")
                    }
                    val activeDays = currentReport.dailyWordCounts.count { it > 0 }
                    append("有${activeDays}天在写日记。")
                }

                val result = aiService.chat(
                    aiRequest(
                        userMessage = prompt,
                        systemPrompt = "你是一个安静、温暖的文字伙伴。回复简洁自然，不要用引号、破折号或特殊符号。",
                        maxTokens = 128,
                        temperature = 0.8f
                    )
                )

                _aiReview.value = result.getOrNull()?.content?.trim()
                    ?: "这个月的记录很珍贵，继续写下去吧"
            } catch (_: Exception) {
                _aiReview.value = "这个月的记录很珍贵，继续写下去吧"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun loadReport(year: Int, month: Int) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val ym = YearMonth.of(year, month)
            val start = ym.atDay(1)
            val end = ym.plusMonths(1).atDay(1)
            val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = end.atStartOfDay(zone).toInstant().toEpochMilli()

            val entries = dao.getPreviewsByDateRange(startMillis, endMillis)

            if (entries.isEmpty()) {
                _report.value = null
                _comparison.value = null
                return@launch
            }

            val photoCount = getPhotoCount(entries)
            val tagStats = getTagStats(entries)
            val totalDurationMinutes = (dao.getTotalWritingDurationSeconds(startMillis, endMillis) ?: 0) / 60

            _report.value = buildReport(year, month, ym.lengthOfMonth(), entries, zone, photoCount, tagStats, totalDurationMinutes)

            // Load last year comparison
            loadComparison(year, month, entries, zone)
        }
    }

    private suspend fun loadComparison(year: Int, month: Int, currentEntries: List<DiaryPreview>, zone: ZoneId) {
        val lastYearYm = YearMonth.of(year - 1, month)
        val lyStart = lastYearYm.atDay(1)
        val lyEnd = lastYearYm.plusMonths(1).atDay(1)
        val lyStartMillis = lyStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val lyEndMillis = lyEnd.atStartOfDay(zone).toInstant().toEpochMilli()

        val lastYearEntries = dao.getPreviewsByDateRange(lyStartMillis, lyEndMillis)

        if (lastYearEntries.isEmpty()) {
            _comparison.value = null
            return
        }

        val currentWords = currentEntries.sumOf { it.plainText.length }
        val lastYearWords = lastYearEntries.sumOf { it.plainText.length }

        val currentMoods = currentEntries.mapNotNull { it.moodLevel }
        val currentAvgMood = if (currentMoods.isNotEmpty()) currentMoods.average().toFloat() else null
        val lastYearMoods = lastYearEntries.mapNotNull { it.moodLevel }
        val lastYearAvgMood = if (lastYearMoods.isNotEmpty()) lastYearMoods.average().toFloat() else null

        val moodDelta = if (currentAvgMood != null && lastYearAvgMood != null) {
            currentAvgMood - lastYearAvgMood
        } else null

        _comparison.value = YearComparison(
            lastYearEntryCount = lastYearEntries.size,
            lastYearAvgMood = lastYearAvgMood,
            lastYearTotalWords = lastYearWords,
            entryCountDelta = currentEntries.size - lastYearEntries.size,
            moodDelta = moodDelta,
            wordsDelta = currentWords - lastYearWords
        )
    }

    private suspend fun buildReport(
        year: Int,
        month: Int,
        daysInMonth: Int,
        entries: List<DiaryPreview>,
        zone: ZoneId,
        photoCount: Int,
        tagStats: List<MonthlyReport.TagStat>,
        totalDurationMinutes: Int
    ): MonthlyReport {
        val totalWords = entries.sumOf { it.plainText.length }

        // Average mood
        val moods = entries.mapNotNull { it.moodLevel }
        val avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else null

        // Happiest entry
        val happiest = entries.maxByOrNull { it.moodLevel ?: 0 }

        // Longest entry
        val longest = entries.maxByOrNull { it.plainText.length }

        // Night entries (0-6)
        val nightEntries = entries.count { entry ->
            val time = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalTime()
            time.hour in 0..5
        }

        // Earliest entry
        val earliest = entries.minByOrNull { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalTime().toSecondOfDay()
        }
        val earliestTime = earliest?.let {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime()
        }

        // Most active day (day of month with most entries)
        val dayCounts = entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate().dayOfMonth
        }.mapValues { it.value.size }
        val mostActiveDay = dayCounts.maxByOrNull { it.value }?.key

        // Daily word counts
        val dailyWordCounts = (1..daysInMonth).map { day ->
            entries.filter { entry ->
                Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate().dayOfMonth == day
            }.sumOf { it.plainText.length }
        }

        // Daily mood averages
        val dailyMoodAverages = (1..daysInMonth).map { day ->
            val dayMoods = entries.filter { entry ->
                Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate().dayOfMonth == day
            }.mapNotNull { it.moodLevel }
            if (dayMoods.isNotEmpty()) dayMoods.average().toFloat() else null
        }

        return MonthlyReport(
            year = year,
            month = month,
            totalEntries = entries.size,
            totalWords = totalWords,
            avgMood = avgMood,
            happiestEntryTitle = happiest?.title ?: "",
            happiestMood = happiest?.moodLevel ?: 0,
            longestEntryTitle = longest?.title ?: "",
            longestWords = longest?.plainText?.length ?: 0,
            nightEntries = nightEntries,
            earliestEntryTime = earliestTime,
            mostActiveDay = mostActiveDay,
            photoCount = photoCount,
            tags = tagStats,
            dailyWordCounts = dailyWordCounts,
            dailyMoodAverages = dailyMoodAverages,
            totalDurationMinutes = totalDurationMinutes
        )
    }

    private suspend fun getPhotoCount(entries: List<DiaryPreview>): Int {
        val entryIds = entries.map { it.id }
        return dao.getImagesForEntries(entryIds).size
    }

    private suspend fun getTagStats(entries: List<DiaryPreview>): List<MonthlyReport.TagStat> {
        val entryIds = entries.map { it.id }.toSet()
        val allPairs = dao.getAllDiaryTagPairsOnce()
        return allPairs
            .filter { it.diaryId in entryIds }
            .groupBy { it.tagId }
            .map { (_, pairs) ->
                MonthlyReport.TagStat(
                    name = pairs.first().name,
                    color = pairs.first().color,
                    count = pairs.size
                )
            }
            .sortedByDescending { it.count }
    }
}
