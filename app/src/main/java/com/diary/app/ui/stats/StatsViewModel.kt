package com.diary.app.ui.stats

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.TagUsage
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodLabelForLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

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

data class MoodTrend(
    val recent30Avg: Double?,
    val previous30Avg: Double?,
    val direction: TrendDirection,
)

data class WordStats(
    val totalWords: Int,
    val avgWordsPerEntry: Int,
)

data class MoodPoint(val date: LocalDate, val level: Int)

data class WordFrequency(
    val word: String,
    val count: Int
)

data class DailyWordCount(
    val date: LocalDate,
    val wordCount: Int
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
    val moodTrendPoints: List<MoodPoint> = emptyList(),
    val wordFrequency: List<WordFrequency> = emptyList(),
    val dailyWordCounts: List<DailyWordCount> = emptyList(),
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val state: StateFlow<StatsState> = combine(
        dao.getAllEntries(),
        dao.getTagUsage()
    ) { entries, tagUsage ->
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
            moodTrendPoints = computeMoodTrendPoints(entries, zone, now),
            wordFrequency = computeWordFrequency(entries),
            dailyWordCounts = computeDailyWordCounts(entries, zone, now),
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
        entries: List<DiaryEntry>,
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
        entries: List<DiaryEntry>,
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
        entries: List<DiaryEntry>,
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

    private fun computeWordStats(entries: List<DiaryEntry>): WordStats? {
        if (entries.isEmpty()) return null
        val totalWords = entries.sumOf { it.plainText.length }
        val avgWords = totalWords / entries.size
        return WordStats(
            totalWords = totalWords,
            avgWordsPerEntry = avgWords,
        )
    }

    private fun computeMoodTrendPoints(
        entries: List<DiaryEntry>,
        zone: ZoneId,
        now: LocalDate
    ): List<MoodPoint> {
        val start = now.minusDays(29)
        val entriesWithMood = entries.filter {
            val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            !d.isBefore(start) && !d.isAfter(now) && it.moodLevel != null && it.moodLevel in 1..6
        }
        // Group by date, take average mood per day, round to nearest int
        return entriesWithMood
            .groupBy {
                Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            }
            .map { (date, list) ->
                val avg = list.map { it.moodLevel!! }.average().roundToInt().coerceIn(1, 6)
                MoodPoint(date, avg)
            }
            .sortedBy { it.date }
    }

    private fun computeWordFrequency(entries: List<DiaryEntry>): List<WordFrequency> {
        // Common stop words to filter out
        val stopWords = setOf(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "他", "她", "它", "们", "那", "被", "从", "把", "让", "用", "对",
            "为", "以", "但", "而", "如果", "虽然", "所以", "因为", "这个", "那个", "什么",
            "怎么", "哪", "吗", "吧", "呢", "啊", "呀", "哦", "嗯", "哈", "啦",
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "need", "dare", "to", "of",
            "in", "for", "on", "with", "at", "by", "from", "as", "into", "through",
            "during", "before", "after", "above", "below", "between", "out", "off",
            "over", "under", "again", "further", "then", "once", "i", "me", "my",
            "we", "our", "you", "your", "he", "him", "his", "she", "her", "it",
            "its", "they", "them", "their", "this", "that", "these", "those",
            "and", "but", "or", "nor", "not", "so", "yet", "both", "either",
            "neither", "each", "every", "all", "any", "few", "more", "most",
            "other", "some", "such", "no", "only", "own", "same", "than", "too",
            "very", "just", "because", "if", "when", "where", "how", "what",
            "which", "who", "whom", "while", "although", "since", "until"
        )

        // Extract words from all entries
        val wordCounts = mutableMapOf<String, Int>()
        entries.forEach { entry ->
            val text = entry.plainText
            // Split by common delimiters
            val words = text.split(Regex("[\\s,.;:!?，。；：！？、\\-\\(\\)（）\\[\\]【】\"\"''\\n\\r]+"))
                .filter { it.length >= 2 } // Only keep words with 2+ chars
                .map { it.lowercase().trim() }
                .filter { it.isNotBlank() && it !in stopWords }

            words.forEach { word ->
                wordCounts[word] = (wordCounts[word] ?: 0) + 1
            }
        }

        return wordCounts.entries
            .sortedByDescending { it.value }
            .take(50) // Top 50 words
            .map { WordFrequency(it.key, it.value) }
    }

    private fun computeDailyWordCounts(
        entries: List<DiaryEntry>,
        zone: ZoneId,
        now: LocalDate
    ): List<DailyWordCount> {
        val start = now.minusDays(364) // Last 365 days
        val dailyCounts = mutableMapOf<LocalDate, Int>()

        entries.forEach { entry ->
            val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
            if (!date.isBefore(start) && !date.isAfter(now)) {
                dailyCounts[date] = (dailyCounts[date] ?: 0) + entry.plainText.length
            }
        }

        return dailyCounts.entries
            .map { DailyWordCount(it.key, it.value) }
            .sortedBy { it.date }
    }

}
