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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    ONE_MONTH(30)
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
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val aiService = app.aiService
    private val _heatmapRange = MutableStateFlow(HeatmapRange.ONE_MONTH)
    private val _wordCloudPeriod = MutableStateFlow(WordCloudPeriod.MONTH)
    private val _aiWords = MutableStateFlow<List<WordFrequency>>(emptyList())
    private val _isWordCloudLoading = MutableStateFlow(false)

    fun setHeatmapRange(range: HeatmapRange) {
        _heatmapRange.value = range
    }

    fun setWordCloudPeriod(period: WordCloudPeriod) {
        if (_wordCloudPeriod.value != period) {
            _wordCloudPeriod.value = period
            loadAiWords()
        }
    }

    val state: StateFlow<StatsState> = combine(
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

        val streak = calculateStreak(activeDates)

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

        StatsState(
            isLoading = false,
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
            topWords = _aiWords.value,
            wordCloudPeriod = _wordCloudPeriod.value,
            isWordCloudLoading = _isWordCloudLoading.value,
            isAiConfigured = aiService.getActiveProvider() != null,
            heatmapData = buildHeatmapData(entryDates, now, heatmapRange.days),
            heatmapRange = heatmapRange,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    init {
        loadAiWords()
    }

    private fun loadAiWords() {
        if (aiService.getActiveProvider() == null) return
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
                if (texts.isEmpty()) {
                    _aiWords.value = emptyList()
                    _isWordCloudLoading.value = false
                    return@launch
                }

                val combinedText = texts.takeLast(50).joinToString("\n---\n")
                val prompt = """请从以下日记文本中提取20个最有代表性的关键词，反映作者关注的主题、情感、活动。
返回JSON数组格式：[{"word":"关键词","weight":权重}]
权重范围1-10，越重要越大。只返回JSON，不要其他文字。

日记文本：
${combinedText.take(3000)}"""

                val request = aiRequest(
                    userMessage = prompt,
                    temperature = 0.3f,
                    maxTokens = 512
                )

                val result = aiService.chat(request, useCache = false)
                result.onSuccess { response ->
                    try {
                        val json = response.content.trim()
                            .removePrefix("```json").removeSuffix("```")
                            .removePrefix("```").removeSuffix("```")
                            .trim()
                        val parsed = parseWordCloudJson(json)
                        _aiWords.value = parsed
                    } catch (e: Exception) {
                        _aiWords.value = extractTopWords(texts, limit = 30)
                    }
                }
                result.onFailure {
                    _aiWords.value = extractTopWords(texts, limit = 30)
                }
            } catch (e: Exception) {
                _aiWords.value = emptyList()
            } finally {
                _isWordCloudLoading.value = false
            }
        }
    }

    private fun parseWordCloudJson(json: String): List<WordFrequency> {
        val gson = com.google.gson.Gson()
        val type = com.google.gson.reflect.TypeToken.getParameterized(
            List::class.java, Map::class.java, String::class.java, Any::class.java
        ).type
        val list: List<Map<String, Any>> = gson.fromJson(json, type) ?: return emptyList()
        return list.mapNotNull { map ->
            val word = map["word"] as? String ?: return@mapNotNull null
            val weight = (map["weight"] as? Number)?.toInt() ?: 5
            WordFrequency(word, weight)
        }.sortedByDescending { it.count }
    }

    suspend fun getEntriesForDate(date: LocalDate): List<DiaryPreview> {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return dao.getPreviewsByDateRange(startOfDay, endOfDay)
    }

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

    private fun computeWordStats(entries: List<DiaryPreview>): WordStats? {
        if (entries.isEmpty()) return null
        val totalWords = entries.sumOf { it.plainText.length }
        val avgWords = totalWords / entries.size
        return WordStats(
            totalWords = totalWords,
            avgWordsPerEntry = avgWords,
        )
    }

    private fun computeTopWords(entries: List<DiaryPreview>): List<WordFrequency> {
        if (entries.isEmpty()) return emptyList()
        val texts = entries.map { it.plainText }.filter { it.isNotBlank() }
        return extractTopWords(texts, limit = 40)
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
