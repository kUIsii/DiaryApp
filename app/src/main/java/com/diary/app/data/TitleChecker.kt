package com.diary.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 称号检查器接口
 */
interface TitleChecker {
    val key: String
    suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean
}

/**
 * 凌晨诗人：凌晨0-3点写过3篇以上日记
 */
class NightPoetChecker : TitleChecker {
    override val key = "night_poet"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val entries = diaryDao.getPreviewsByDateRange(thirtyDaysAgo, System.currentTimeMillis())
        val nightEntries = entries.filter { entry ->
            val hour = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).hour
            hour in 0..2
        }
        return nightEntries.size >= 3
    }
}

/**
 * 黎明记录者：凌晨3-5点写过日记
 */
class DawnRecorderChecker : TitleChecker {
    override val key = "dawn_recorder"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val entries = diaryDao.getPreviewsByDateRange(thirtyDaysAgo, System.currentTimeMillis())
        return entries.any { entry ->
            val hour = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).hour
            hour in 3..4
        }
    }
}

/**
 * 晨光之笔：5-7点写过5篇以上日记
 */
class MorningWriterChecker : TitleChecker {
    override val key = "morning_writer"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val entries = diaryDao.getPreviewsByDateRange(thirtyDaysAgo, System.currentTimeMillis())
        val morningEntries = entries.filter { entry ->
            val hour = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).hour
            hour in 5..6
        }
        return morningEntries.size >= 5
    }
}

/**
 * 午后漫想家：80%以上日记写于12-15点
 */
class AfternoonDreamerChecker : TitleChecker {
    override val key = "afternoon_dreamer"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        if (entries.size < 10) return false
        val afternoonEntries = entries.filter { entry ->
            val hour = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).hour
            hour in 12..14
        }
        return afternoonEntries.size.toFloat() / entries.size >= 0.8f
    }
}

/**
 * 夜猫子：70%以上日记写于22-2点
 */
class NightOwlChecker : TitleChecker {
    override val key = "night_owl"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        if (entries.size < 10) return false
        val nightEntries = entries.filter { entry ->
            val hour = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).hour
            hour in 22..23 || hour in 0..1
        }
        return nightEntries.size.toFloat() / entries.size >= 0.7f
    }
}

/**
 * 星期杀手：每个星期几都写过日记
 */
class WeekdayKillerChecker : TitleChecker {
    override val key = "weekday_killer"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        val weekdaysUsed = entries.map { entry ->
            Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .dayOfWeek
        }.toSet()
        return weekdaysUsed.size >= 7
    }
}

/**
 * 时光胶囊：连续12个月每月都写了日记
 */
class TimeCapsuleMasterChecker : TitleChecker {
    override val key = "time_capsule_master"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val now = LocalDate.now()
        for (i in 0 until 12) {
            val month = now.minusMonths(i.toLong())
            val start = month.withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val end = month.withDayOfMonth(month.lengthOfMonth())
                .atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val entries = diaryDao.getPreviewsByDateRange(start, end)
            if (entries.isEmpty()) return false
        }
        return true
    }
}

/**
 * 乐观主义者：连续10篇日记心情等级都在5以上
 */
class OptimistChecker : TitleChecker {
    override val key = "optimist"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        val recentEntries = entries.take(10)
        if (recentEntries.size < 10) return false
        return recentEntries.all { (it.moodLevel ?: 0) >= 5 }
    }
}

/**
 * 深度思考者：连续10篇日记心情等级都在2以下
 */
class DeepThinkerChecker : TitleChecker {
    override val key = "deep_thinker"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        val recentEntries = entries.take(10)
        if (recentEntries.size < 10) return false
        return recentEntries.all { (it.moodLevel ?: 0) in 1..2 }
    }
}

/**
 * 情绪调色板：使用过全部6种心情等级
 */
class MoodPaletteChecker : TitleChecker {
    override val key = "mood_palette"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        val moodsUsed = entries.mapNotNull { it.moodLevel }.toSet()
        return moodsUsed.size >= 6
    }
}

/**
 * 雨天收藏家：在雨天写了20篇以上日记
 */
class RainCollectorChecker : TitleChecker {
    override val key = "rain_collector"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        val rainEntries = entries.filter { it.weather == "雨天" }
        return rainEntries.size >= 20
    }
}

/**
 * 万事皆记：在所有天气类型下都写过日记
 */
class AllWeatherChecker : TitleChecker {
    override val key = "all_weather"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        val weatherTypes = entries.mapNotNull { it.weather }.toSet()
        return weatherTypes.containsAll(setOf("晴天", "多云", "阴天", "雨天", "雷暴", "大风"))
    }
}

/**
 * 千字长文：单篇日记超过1000字
 */
class ThousandWordsChecker : TitleChecker {
    override val key = "thousand_words"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        return entries.any { it.plainText.length > 1000 }
    }
}

/**
 * 微言大义：少于50字但被收藏了
 */
class BriefMasterChecker : TitleChecker {
    override val key = "brief_master"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        return entries.any {
            it.plainText.length < 50 && it.isFavorite
        }
    }
}

/**
 * 收藏鉴赏家：收藏了20篇以上自己的日记
 */
class CollectorChecker : TitleChecker {
    override val key = "collector"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        return entries.count { it.isFavorite } >= 20
    }
}

/**
 * 万字耕耘者：累计写作超过5万字
 */
class FiftyThousandWordsChecker : TitleChecker {
    override val key = "fifty_thousand_words"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        val totalChars = entries.sumOf { it.plainText.length }
        return totalChars >= 50000
    }
}

/**
 * 日更达人：连续30天每天写日记
 */
class DailyWriterChecker : TitleChecker {
    override val key = "daily_writer"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val now = LocalDate.now()
        for (i in 0 until 30) {
            val date = now.minusDays(i.toLong())
            val start = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val end = date.atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val entries = diaryDao.getPreviewsByDateRange(start, end)
            if (entries.isEmpty()) return false
        }
        return true
    }
}

/**
 * 百日坚持：连续100天每天写日记
 */
class HundredDaysChecker : TitleChecker {
    override val key = "hundred_days"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val now = LocalDate.now()
        for (i in 0 until 100) {
            val date = now.minusDays(i.toLong())
            val start = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val end = date.atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val entries = diaryDao.getPreviewsByDateRange(start, end)
            if (entries.isEmpty()) return false
        }
        return true
    }
}

/**
 * 回归者：断写超过30天后重新开始写日记
 */
class ReturneeChecker : TitleChecker {
    override val key = "returnee"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        if (entries.size < 2) return false

        // 按时间排序
        val sorted = entries.sortedByDescending { it.createdAt }

        // 找到最近的间隔
        for (i in 0 until sorted.size - 1) {
            val gap = ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(sorted[i + 1].createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
                Instant.ofEpochMilli(sorted[i].createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            )
            if (gap > 30) return true
        }
        return false
    }
}

/**
 * 时间旅行者：在1月1日写日记（隐藏）
 */
class TimeTravelerChecker : TitleChecker {
    override val key = "time_traveler"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        return entries.any { entry ->
            val date = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            date.monthValue == 1 && date.dayOfMonth == 1
        }
    }
}

/**
 * 跨年守夜人：在12月31日23:00之后写日记（隐藏）
 */
class NewYearEveChecker : TitleChecker {
    override val key = "new_year_eve"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        return entries.any { entry ->
            val dateTime = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
            dateTime.monthValue == 12 && dateTime.dayOfMonth == 31 && dateTime.hour >= 23
        }
    }
}

/**
 * 午夜钟声：在0:00-0:10之间写日记（隐藏）
 */
class MidnightBellChecker : TitleChecker {
    override val key = "midnight_bell"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        return entries.any { entry ->
            val dateTime = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
            dateTime.hour == 0 && dateTime.minute <= 10
        }
    }
}

/**
 * 首篇回响：为第一篇日记添加了图片或标签（隐藏）
 */
class FirstEchoChecker : TitleChecker {
    override val key = "first_echo"
    override suspend fun check(diaryDao: DiaryDao, titleDao: TitleDao): Boolean {
        val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
        if (entries.isEmpty()) return false
        val firstEntry = entries.minByOrNull { it.createdAt } ?: return false
        // 检查是否有图片
        val images = diaryDao.getImagesForEntry(firstEntry.id)
        if (images.isNotEmpty()) return true
        // 检查是否有标签
        val tags = diaryDao.getTagsForDiary(firstEntry.id)
        return tags.isNotEmpty()
    }
}
