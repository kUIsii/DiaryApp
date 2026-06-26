package com.diary.app.ui.weeklyreport

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class WeeklyReportViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _report = MutableStateFlow<WeeklyReport?>(null)
    val report: StateFlow<WeeklyReport?> = _report.asStateFlow()

    fun loadCurrentWeek() {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        loadReport(weekStart, weekEnd)
    }

    fun loadReport(weekStart: LocalDate, weekEnd: LocalDate) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = weekEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val entries = dao.getPreviewsByDateRange(startMillis, endMillis)

            if (entries.isEmpty()) {
                _report.value = null
                return@launch
            }

            val tagStats = getTagStats(entries)
            val totalDurationMinutes = (dao.getTotalWritingDurationSeconds(startMillis, endMillis) ?: 0) / 60

            _report.value = buildReport(weekStart, weekEnd, entries, zone, tagStats, totalDurationMinutes)
        }
    }

    private suspend fun buildReport(
        weekStart: LocalDate,
        weekEnd: LocalDate,
        entries: List<DiaryPreview>,
        zone: ZoneId,
        tagStats: List<WeeklyReport.TagStat>,
        totalDurationMinutes: Int
    ): WeeklyReport {
        val totalWords = entries.sumOf { it.plainText.length }

        // Average mood
        val moods = entries.mapNotNull { it.moodLevel }
        val avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else null

        // Longest entry
        val longest = entries.maxByOrNull { it.plainText.length }

        // Active days (days with at least one entry)
        val activeDays = entries.map { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
        }.distinct().size

        // Daily word counts (7 days: Mon-Sun)
        val dailyWordCounts = (0 until 7).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            entries.filter { entry ->
                Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate() == date
            }.sumOf { it.plainText.length }
        }

        // Daily mood averages
        val dailyMoodAverages = (0 until 7).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val dayMoods = entries.filter { entry ->
                Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate() == date
            }.mapNotNull { it.moodLevel }
            if (dayMoods.isNotEmpty()) dayMoods.average().toFloat() else null
        }

        val formatter = DateTimeFormatter.ofPattern("M/d")
        val weekNumber = weekStart.dayOfYear / 7 + 1

        return WeeklyReport(
            year = weekStart.year,
            weekNumber = weekNumber,
            startDate = weekStart.format(formatter),
            endDate = weekEnd.format(formatter),
            totalEntries = entries.size,
            totalWords = totalWords,
            avgMood = avgMood,
            activeDays = activeDays,
            tags = tagStats,
            dailyWordCounts = dailyWordCounts,
            dailyMoodAverages = dailyMoodAverages,
            longestEntryTitle = longest?.title ?: "",
            longestWords = longest?.plainText?.length ?: 0,
            totalDurationMinutes = totalDurationMinutes
        )
    }

    private suspend fun getTagStats(entries: List<DiaryPreview>): List<WeeklyReport.TagStat> {
        val entryIds = entries.map { it.id }.toSet()
        val allPairs = dao.getAllDiaryTagPairsOnce()
        return allPairs
            .filter { it.diaryId in entryIds }
            .groupBy { it.tagId }
            .map { (_, pairs) ->
                WeeklyReport.TagStat(
                    name = pairs.first().name,
                    color = pairs.first().color,
                    count = pairs.size
                )
            }
            .sortedByDescending { it.count }
            .take(5) // Top 5 tags
    }
}
