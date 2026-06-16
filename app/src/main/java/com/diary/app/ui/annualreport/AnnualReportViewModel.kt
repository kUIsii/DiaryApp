package com.diary.app.ui.annualreport

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import com.diary.app.data.DiaryImage
import com.diary.app.data.TagUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class WeatherMood(val weather: String, val avgMood: Float, val count: Int)

data class AnnualReport(
    val year: Int,
    val totalEntries: Int,
    val totalWords: Int,
    val monthlyMood: List<Float?>,
    val monthlyCount: List<Int>,
    val nightEntries: Int,
    val latestEntryTime: LocalTime?,
    val latestEntryTitle: String,
    val longestEntryTitle: String,
    val longestWords: Int,
    val longestEntryDate: LocalDate?,
    val longestSilenceDays: Int,
    val silenceStart: LocalDate?,
    val silenceEnd: LocalDate?,
    val happiestEntryTitle: String,
    val happiestMood: Int,
    val happiestDate: LocalDate?,
    val mostActiveTime: String,
    val mostActiveDay: String,
    val timeDistribution: List<Int>,
    val weatherMood: List<WeatherMood>,
    val topTags: List<TagUsage>,
    val firstEntryDate: LocalDate?,
    val longestStreak: Int,
    // 新增字段：增加更多个性化元素
    val photoCount: Int, // 总照片数
    val mostPhotosInSingleEntry: Int, // 单篇最多照片数
    val mostPhotosEntryTitle: String, // 照片最多的日记标题
    val mostPhotosEntryDate: LocalDate?, // 照片最多的日记日期
    val earliestEntryTime: LocalTime?, // 最早写作时间
    val earliestEntryTitle: String, // 最早写作的日记标题
    val mostProductiveDate: LocalDate?, // 最多产的日期
    val mostProductiveCount: Int // 最多产日期的篇数
)

class AnnualReportViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _report = MutableStateFlow<AnnualReport?>(null)
    val report: StateFlow<AnnualReport?> = _report.asStateFlow()

    fun loadReport(year: Int) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val allPreviews = dao.getAllPreviewsOnce()
            val yearEntries = allPreviews.filter { entry ->
                val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                date.year == year
            }

            if (yearEntries.isEmpty()) {
                _report.value = null
                return@launch
            }

            val tagUsage = dao.getTagUsageOnce()

            // 获取照片统计数据
            val photoStats = getPhotoStatsForYear(yearEntries, zone)

            _report.value = buildReport(year, yearEntries, tagUsage, zone, photoStats)
        }
    }

    private fun buildReport(
        year: Int,
        entries: List<DiaryPreview>,
        tagUsage: List<TagUsage>,
        zone: ZoneId,
        photoStats: PhotoStats
    ): AnnualReport {
        val sorted = entries.sortedBy { it.createdAt }
        val dates = entries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }.toSet()

        // Total words
        val totalWords = entries.sumOf { it.plainText.length }

        // Monthly mood (12 months)
        val monthlyMood = (1..12).map { month ->
            val monthEntries = entries.filter { entry ->
                val d = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                d.monthValue == month
            }
            val moods = monthEntries.mapNotNull { it.moodLevel }
            if (moods.isNotEmpty()) moods.average().toFloat() else null
        }

        // Monthly count
        val monthlyCount = (1..12).map { month ->
            entries.count { entry ->
                val d = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                d.monthValue == month
            }
        }

        // Night entries (after midnight, before 6am)
        val nightEntries = entries.count { entry ->
            val time = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalTime()
            time.hour in 0..5
        }

        // Latest entry (latest time of day)
        val latestByTime = entries.maxByOrNull { entry ->
            val time = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalTime()
            time.toSecondOfDay()
        }
        val latestTime = latestByTime?.let {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime()
        }

        // Earliest entry (earliest time of day)
        val earliestByTime = entries.minByOrNull { entry ->
            val time = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalTime()
            time.toSecondOfDay()
        }
        val earliestTime = earliestByTime?.let {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime()
        }

        // Longest entry
        val longest = entries.maxByOrNull { it.plainText.length }
        val longestDate = longest?.let {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }

        // Most productive date (most entries in a single day)
        val dailyEntries = entries.groupBy {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }
        val mostProductive = dailyEntries.maxByOrNull { it.value.size }
        val mostProductiveDate = mostProductive?.key
        val mostProductiveCount = mostProductive?.value?.size ?: 0

        // Longest silence
        val sortedDates = dates.sorted()
        var maxSilence = 0
        var silenceStart: LocalDate? = null
        var silenceEnd: LocalDate? = null
        for (i in 1 until sortedDates.size) {
            val gap = java.time.temporal.ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i]).toInt()
            if (gap > maxSilence) {
                maxSilence = gap
                silenceStart = sortedDates[i - 1]
                silenceEnd = sortedDates[i]
            }
        }

        // Happiest day
        val happiest = entries.maxByOrNull { it.moodLevel ?: 0 }
        val happiestDate = happiest?.let {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }

        // Most active time distribution (6 periods)
        val timeDistribution = listOf(
            entries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime().hour in 6..11 },
            entries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime().hour in 12..13 },
            entries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime().hour in 14..17 },
            entries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime().hour in 18..21 },
            entries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime().hour in 22..23 },
            entries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime().hour in 0..5 }
        )
        val timeLabels = listOf("上午", "中午", "下午", "傍晚", "深夜", "凌晨")
        val mostActiveTimeIdx = timeDistribution.indices.maxByOrNull { timeDistribution[it] } ?: 0
        val mostActiveTime = timeLabels[mostActiveTimeIdx]

        // Most active day of week
        val dayCounts = entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate().dayOfWeek.value
        }.mapValues { it.value.size }
        val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val mostActiveDayIdx = (dayCounts.maxByOrNull { it.value }?.key ?: 1) - 1
        val mostActiveDay = dayLabels[mostActiveDayIdx.coerceIn(0, 6)]

        // Weather mood
        val weatherGroups = entries.groupBy { it.weather ?: "" }.filter { it.key.isNotBlank() }
        val weatherMood = weatherGroups.map { (weather, group) ->
            val moods = group.mapNotNull { it.moodLevel }
            WeatherMood(
                weather = weather,
                avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else 0f,
                count = group.size
            )
        }.sortedByDescending { it.count }.take(5)

        // Top tags
        val topTags = tagUsage.sortedByDescending { it.count }.take(5)

        // First entry date
        val firstEntryDate = sorted.firstOrNull()?.let {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }

        // Longest streak
        val longestStreak = computeLongestStreak(dates)

        return AnnualReport(
            year = year,
            totalEntries = entries.size,
            totalWords = totalWords,
            monthlyMood = monthlyMood,
            monthlyCount = monthlyCount,
            nightEntries = nightEntries,
            latestEntryTime = latestTime,
            latestEntryTitle = latestByTime?.title ?: "",
            longestEntryTitle = longest?.title ?: "",
            longestWords = longest?.plainText?.length ?: 0,
            longestEntryDate = longestDate,
            longestSilenceDays = maxSilence,
            silenceStart = silenceStart,
            silenceEnd = silenceEnd,
            happiestEntryTitle = happiest?.title ?: "",
            happiestMood = happiest?.moodLevel ?: 0,
            happiestDate = happiestDate,
            mostActiveTime = mostActiveTime,
            mostActiveDay = mostActiveDay,
            timeDistribution = timeDistribution,
            weatherMood = weatherMood,
            topTags = topTags,
            firstEntryDate = firstEntryDate,
            longestStreak = longestStreak,
            // 新增字段
            photoCount = photoStats.photoCount,
            mostPhotosInSingleEntry = photoStats.mostPhotosInSingleEntry,
            mostPhotosEntryTitle = photoStats.mostPhotosEntryTitle,
            mostPhotosEntryDate = photoStats.mostPhotosEntryDate,
            earliestEntryTime = earliestTime,
            earliestEntryTitle = earliestByTime?.title ?: "",
            mostProductiveDate = mostProductiveDate,
            mostProductiveCount = mostProductiveCount
        )
    }

    // 异步获取照片统计数据
    private suspend fun getPhotoStatsForYear(
        entries: List<DiaryPreview>,
        zone: ZoneId
    ): PhotoStats {
        val entryIds = entries.map { it.id }
        val allImages = dao.getImagesForEntries(entryIds)
        val imagesByEntry = allImages.groupBy { it.entryId }

        var totalPhotos = allImages.size
        var maxPhotosInSingleEntry = 0
        var maxPhotosEntryTitle = ""
        var maxPhotosEntryDate: LocalDate? = null

        for (entryId in entryIds) {
            val count = imagesByEntry[entryId]?.size ?: 0
            if (count > maxPhotosInSingleEntry) {
                maxPhotosInSingleEntry = count
                val entry = entries.find { it.id == entryId }
                if (entry != null) {
                    maxPhotosEntryTitle = entry.title
                    maxPhotosEntryDate = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                }
            }
        }

        return PhotoStats(
            photoCount = totalPhotos,
            mostPhotosInSingleEntry = maxPhotosInSingleEntry,
            mostPhotosEntryTitle = maxPhotosEntryTitle,
            mostPhotosEntryDate = maxPhotosEntryDate
        )
    }

    // 照片统计数据类
    private data class PhotoStats(
        val photoCount: Int,
        val mostPhotosInSingleEntry: Int,
        val mostPhotosEntryTitle: String,
        val mostPhotosEntryDate: LocalDate?
    )

    private fun computeLongestStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.sorted()
        var maxStreak = 1
        var currentStreak = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                currentStreak++
                if (currentStreak > maxStreak) maxStreak = currentStreak
            } else {
                currentStreak = 1
            }
        }
        return maxStreak
    }
}
