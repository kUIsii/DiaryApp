package com.diary.app.ui.stats

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryPreview
import com.diary.app.data.TagUsage
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodLabelForLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.diary.app.util.computeStreak
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
    val avgWritingMinutes: Int? = null,
)

enum class TrendDirection { UP, DOWN, FLAT }

enum class HeatmapRange(val days: Int) {
    ONE_MONTH(30),
    THREE_MONTHS(90),
    SIX_MONTHS(180),
    ONE_YEAR(365)
}

enum class WordCloudPeriod(val label: String) {
    MONTH("本月"),
    YEAR("今年"),
    ALL("全部")
}

data class MoodTrend(
    val recent30Avg: Double?,
    val previous30Avg: Double?,
    val direction: TrendDirection,
)

data class MoodWeatherInsight(
    val text: String,
    val moodLevel: Float,
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
    val isLoading: Boolean = true,
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
    val topWords: List<WordFrequency> = emptyList(),
    val wordCloudPeriod: WordCloudPeriod = WordCloudPeriod.MONTH,
    val isWordCloudLoading: Boolean = false,
    val isAiConfigured: Boolean = false,
    val heatmapData: List<HeatmapDay> = emptyList(),
    val heatmapRange: HeatmapRange = HeatmapRange.ONE_MONTH,
    val moodWeatherInsight: MoodWeatherInsight? = null,
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private data class WordCloudSnapshot(
        val period: WordCloudPeriod,
        val words: List<WordFrequency>,
        val isLoading: Boolean,
    )

    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val aiService = app.aiService
    private val _heatmapRange = MutableStateFlow(HeatmapRange.ONE_MONTH)
    private val _wordCloudPeriod = MutableStateFlow(WordCloudPeriod.MONTH)
    private val _aiWords = MutableStateFlow<List<WordFrequency>>(emptyList())
    private val _isWordCloudLoading = MutableStateFlow(false)
    private val prefs = application.getSharedPreferences("word_cloud_cache", android.content.Context.MODE_PRIVATE)

    fun setHeatmapRange(range: HeatmapRange) {
        _heatmapRange.value = range
    }

    fun setWordCloudPeriod(period: WordCloudPeriod) {
        if (_wordCloudPeriod.value != period) {
            _wordCloudPeriod.value = period
            loadAiWords()
        }
    }

    // Entry-dependent stats: only recomputes when entries, tags, or heatmap range change
    private val entryStatsFlow: Flow<StatsState> = combine(
        dao.getAllPreviews(),
        dao.getTagUsage(),
        _heatmapRange
    ) { entries, tagUsage, heatmapRange ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()

        val entryDates = entries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }
        val activeDates = entryDates.toSet()

        val streak = computeStreak(activeDates)

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

        val weatherDistribution = entries.mapNotNull { entry ->
            entry.weather?.takeIf { it.isNotBlank() }?.let { weather -> weather to entry }
        }
            .groupBy({ it.first }, { it.second })
            .map { (type, list) -> WeatherStat(type, list.size) }
            .sortedByDescending { it.count }

        val avgWritingMins = dao.getAverageWritingDurationSeconds()?.let { (it / 60).toInt() }

        StatsState(
            isLoading = false,
            totalEntries = entries.size,
            currentStreak = streak,
            thisMonthEntries = thisMonth,
            moodDistribution = moodDistribution,
            weatherDistribution = weatherDistribution,
            tagUsage = tagUsage,
            monthlyTrend = computeMonthlyTrend(entries, zone, now),
            writingHabit = computeWritingHabit(entries, zone, now, avgWritingMins),
            moodTrend = computeMoodTrend(entries, zone, now),
            wordStats = computeWordStats(entries),
            isAiConfigured = aiService.getActiveProvider() != null,
            heatmapData = buildHeatmapData(entryDates, now, heatmapRange.days),
            heatmapRange = heatmapRange,
            moodWeatherInsight = computeMoodWeatherCorrelation(entries),
        )
    }

    // Word cloud state: only recomputes when period, words, or loading state change
    private val wordCloudFlow: Flow<WordCloudSnapshot> = combine(
        _wordCloudPeriod,
        _aiWords,
        _isWordCloudLoading
    ) { period, words, isLoading ->
        WordCloudSnapshot(period, words, isLoading)
    }

    val state: StateFlow<StatsState> = combine(
        entryStatsFlow,
        wordCloudFlow
    ) { stats, wc ->
        stats.copy(
            topWords = wc.words,
            wordCloudPeriod = wc.period,
            isWordCloudLoading = wc.isLoading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    init {
        loadAiWords()
    }

    private fun loadAiWords() {
        // Always use local extraction - fast, deterministic, no AI dependency
        viewModelScope.launch {
            _isWordCloudLoading.value = true
            try {
                val period = _wordCloudPeriod.value
                val zone = ZoneId.systemDefault()
                val now = LocalDate.now()
                val entries = dao.getAllPreviews().first()
                val filteredEntries = entries.filter { entry ->
                    val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                    when (period) {
                        WordCloudPeriod.MONTH -> date.year == now.year && date.monthValue == now.monthValue
                        WordCloudPeriod.YEAR -> date.year == now.year
                        WordCloudPeriod.ALL -> true
                    }
                }
                val texts = filteredEntries.map { it.plainText }.filter { it.isNotBlank() }
                _aiWords.value = if (texts.isEmpty()) emptyList() else extractTopWords(texts, limit = 20)
            } finally {
                _isWordCloudLoading.value = false
            }
        }
    }


    suspend fun getEntriesForDate(date: LocalDate): List<DiaryPreview> {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return dao.getPreviewsByDateRange(startOfDay, endOfDay)
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
        now: LocalDate,
        avgWritingMinutes: Int? = null
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
            avgWritingMinutes = avgWritingMinutes,
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

        val recentAvg = recentEntries.mapNotNull { it.moodLevel }.average()
        val previousAvg = if (previousEntries.isNotEmpty())
            previousEntries.mapNotNull { it.moodLevel }.average()
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

    private fun computeMoodWeatherCorrelation(entries: List<DiaryPreview>): MoodWeatherInsight? {
        val withBoth = entries.filter { it.moodLevel != null && !it.weather.isNullOrBlank() }
        if (withBoth.size < 10) return null

        val byWeather = withBoth.groupBy { it.weather!! }
        if (byWeather.size < 2) return null

        val avgByWeather = byWeather.mapValues { (_, list) ->
            list.mapNotNull { it.moodLevel }.average()
        }

        val best = avgByWeather.maxByOrNull { it.value }!!
        val worst = avgByWeather.minByOrNull { it.value }!!

        val overallAvg = withBoth.mapNotNull { it.moodLevel }.average()
        val bestDiff = best.value - overallAvg
        val worstDiff = overallAvg - worst.value

        // Show the more notable correlation
        return if (bestDiff >= worstDiff && bestDiff >= 0.3) {
            MoodWeatherInsight(
                text = "${best.key}时心情最好，平均 ${"%.1f".format(best.value)} 分",
                moodLevel = best.value.toFloat(),
            )
        } else if (worstDiff >= 0.3) {
            MoodWeatherInsight(
                text = "${worst.key}时心情较低，平均 ${"%.1f".format(worst.value)} 分",
                moodLevel = worst.value.toFloat(),
            )
        } else {
            null // No notable correlation
        }
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

}

fun buildHeatmapData(entryDates: List<LocalDate>, now: LocalDate, days: Int): List<HeatmapDay> {
    val entryCountsByDate = entryDates.groupingBy { it }.eachCount()
    val result = mutableListOf<HeatmapDay>()
    for (i in (days - 1) downTo 0) {
        val date = now.minusDays(i.toLong())
        result.add(
            HeatmapDay(
                date = date,
                count = entryCountsByDate[date] ?: 0,
            )
        )
    }
    return result
}
