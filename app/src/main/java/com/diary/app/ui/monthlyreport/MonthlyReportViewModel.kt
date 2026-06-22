package com.diary.app.ui.monthlyreport

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
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

    private val _report = MutableStateFlow<MonthlyReport?>(null)
    val report: StateFlow<MonthlyReport?> = _report.asStateFlow()

    private val _comparison = MutableStateFlow<YearComparison?>(null)
    val comparison: StateFlow<YearComparison?> = _comparison.asStateFlow()

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

            _report.value = buildReport(year, month, ym.lengthOfMonth(), entries, zone, photoCount, tagStats)

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
        tagStats: List<MonthlyReport.TagStat>
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
            dailyWordCounts = dailyWordCounts
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
