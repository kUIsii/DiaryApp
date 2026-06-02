package com.diary.app.ui.stats

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import com.diary.app.data.TagUsage
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodLabelForLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class MoodStat(
    val level: Int,
    val count: Int,
    val label: String,
    val color: Color,
)

data class WeatherStat(
    val type: String,
    val count: Int,
)

data class MonthTrend(
    val month: String,
    val count: Int,
)

data class WritingHabit(
    val avgPerWeek: Double,
    val mostActiveDay: String,
    val mostActiveTime: String,
)

enum class TrendDirection { UP, DOWN, FLAT }

enum class HeatmapRange(val days: Int) {
    ONE_MONTH(30),
    THREE_MONTHS(90),
    SIX_MONTHS(180)
}

data class MoodTrend(
    val recent30Avg: Double?,
    val previous30Avg: Double?,
    val direction: TrendDirection,
)

data class WordStats(
    val totalWords: Int,
    val avgWordsPerEntry: Int,
)

data class HeatmapDay(
    val date: LocalDate,
    val count: Int,
)

data class StatsState(
    val totalEntries: Int = 0,
    val currentStreak: Int = 0,
    val thisMonthEntries: Int = 0,
    val moodDistribution: List<MoodStat> = emptyList(),
    val weatherDistribution: List<WeatherStat> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
    val monthlyTrend: List<MonthTrend> = emptyList(),
    val writingHabit: WritingHabit? = null,
    val moodTrend: MoodTrend? = null,
    val wordStats: WordStats? = null,
    val heatmapData: List<HeatmapDay> = emptyList(),
    val heatmapRange: HeatmapRange = HeatmapRange.SIX_MONTHS,
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val _heatmapRange = MutableStateFlow(HeatmapRange.THREE_MONTHS)

    fun toggleHeatmapRange() {
        _heatmapRange.value = when (_heatmapRange.value) {
            HeatmapRange.ONE_MONTH -> HeatmapRange.THREE_MONTHS
            HeatmapRange.THREE_MONTHS -> HeatmapRange.SIX_MONTHS
            HeatmapRange.SIX_MONTHS -> HeatmapRange.ONE_MONTH
        }
    }

    fun setHeatmapRange(range: HeatmapRange) {
        _heatmapRange.value = range
    }

    val state: StateFlow<StatsState> = combine(
        dao.getAllPreviews(),
        dao.getTagUsage(),
        _heatmapRange
    ) { entries, tagUsage, heatmapRange ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()

        val dates = entries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }.toSet()

        val streak = calculateStreak(dates)

        val thisMonth = entries.count {
            val date = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            date.year == now.year && date.monthValue == now.monthValue
        }

        val moodDistribution = (1..6).map { level ->
            val count = entries.count { it.moodLevel == level }
            MoodStat(
                level = level,
                count = count,
                label = moodLabelForLevel(level),
                color = moodColorForLevel(level),
            )
        }

        val weatherDistribution = entries.filter { !it.weather.isNullOrBlank() }
            .groupBy { it.weather!! }
            .map { (type, list) -> WeatherStat(type, list.size) }
            .sortedByDescending { it.count }

        StatsState(
            totalEntries = entries.size,
            currentStreak = streak,
            thisMonthEntries = thisMonth,
            moodDistribution = moodDistribution,
            weatherDistribution = weatherDistribution,
            tagUsage = tagUsage,
            monthlyTrend = computeMonthlyTrend(entries, zone, now),
            writingHabit = computeWritingHabit(entries, zone, now),
            moodTrend = computeMoodTrend(entries, zone, now),
            wordStats = computeWordStats(entries),
            heatmapData = computeHeatmapData(dates, now, heatmapRange.days),
            heatmapRange = heatmapRange,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    private fun calculateStreak(dates: Set<LocalDate>): Int {
        var streak = 0
        var current = LocalDate.now()
        while (dates.contains(current)) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    private fun computeMonthlyTrend(
        entries: List<DiaryPreview>,
        zone: ZoneId,
        now: LocalDate
    ): List<MonthTrend> {
        val result = mutableListOf<MonthTrend>()
        for (i in 5 downTo 0) {
            val month = now.minusMonths(i.toLong())
            val count = entries.count {
                val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
                d.year == month.year && d.monthValue == month.monthValue
            }
            result.add(
                MonthTrend(
                    month = "${month.monthValue}月",
                    count = count,
                )
            )
        }
        return result
    }

    private fun computeWritingHabit(
        entries: List<DiaryPreview>,
        zone: ZoneId,
        now: LocalDate
    ): WritingHabit? {
        if (entries.isEmpty()) return null

        // Average entries per week
        val earliest = entries.minOf { it.createdAt }
        val earliestDate = Instant.ofEpochMilli(earliest).atZone(zone).toLocalDate()
        val weeks = java.time.temporal.ChronoUnit.WEEKS.between(earliestDate, now).coerceAtLeast(1)
        val avgPerWeek = entries.size.toDouble() / weeks

        // Most active day of week
        val dayCounts = entries.groupBy {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().dayOfWeek
        }.mapValues { it.value.size }
        val mostActiveDay = dayCounts.maxByOrNull { it.value }?.key
            ?.getDisplayName(TextStyle.SHORT, Locale.CHINESE) ?: ""

        // Most active time of day
        val timeCounts = entries.groupBy { entry ->
            val hour = Instant.ofEpochMilli(entry.createdAt).atZone(zone).hour
            when {
                hour in 6..11 -> "上午"
                hour in 12..17 -> "下午"
                hour in 18..22 -> "晚上"
                else -> "深夜"
            }
        }.mapValues { it.value.size }
        val mostActiveTime = timeCounts.maxByOrNull { it.value }?.key ?: ""

        return WritingHabit(
            avgPerWeek = avgPerWeek,
            mostActiveDay = mostActiveDay,
            mostActiveTime = mostActiveTime,
        )
    }

    private fun computeMoodTrend(
        entries: List<DiaryPreview>,
        zone: ZoneId,
        now: LocalDate
    ): MoodTrend? {
        val recent30 = now.minusDays(30)
        val previous30 = now.minusDays(60)

        val recentEntries = entries.filter {
            val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            d.isAfter(recent30) && !d.isAfter(now) && it.moodLevel != null
        }
        val previousEntries = entries.filter {
            val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            d.isAfter(previous30) && !d.isAfter(recent30) && it.moodLevel != null
        }

        if (recentEntries.isEmpty()) return null

        val recentAvg = recentEntries.map { it.moodLevel!! }.average()
        val previousAvg = if (previousEntries.isNotEmpty())
            previousEntries.map { it.moodLevel!! }.average()
        else null

        val direction = if (previousAvg != null) {
            val diff = recentAvg - previousAvg
            when {
                diff > 0.5 -> TrendDirection.UP
                diff < -0.5 -> TrendDirection.DOWN
                else -> TrendDirection.FLAT
            }
        } else {
            TrendDirection.FLAT
        }

        return MoodTrend(
            recent30Avg = recentAvg,
            previous30Avg = previousAvg,
            direction = direction,
        )
    }

    private fun computeWordStats(entries: List<DiaryPreview>): WordStats? {
        if (entries.isEmpty()) return null
        val totalWords = entries.sumOf { it.plainText.length }
        val avgWords = totalWords / entries.size
        return WordStats(
            totalWords = totalWords,
            avgWordsPerEntry = avgWords,
        )
    }

    private fun computeHeatmapData(entryDates: Set<LocalDate>, now: LocalDate, days: Int): List<HeatmapDay> {
        val result = mutableListOf<HeatmapDay>()
        for (i in (days - 1) downTo 0) {
            val date = now.minusDays(i.toLong())
            result.add(
                HeatmapDay(
                    date = date,
                    count = if (date in entryDates) 1 else 0,
                )
            )
        }
        return result
    }

}
