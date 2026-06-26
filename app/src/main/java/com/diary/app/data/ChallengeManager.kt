package com.diary.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Manages weekly and monthly challenges for the achievement system.
 * Challenges are time-limited goals that reset periodically.
 */
object ChallengeManager {

    /**
     * Get the start of the current week (Monday).
     */
    fun getWeekStart(date: LocalDate = LocalDate.now()): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    /**
     * Get the start of the current month.
     */
    fun getMonthStart(date: LocalDate = LocalDate.now()): LocalDate {
        return date.withDayOfMonth(1)
    }

    /**
     * Get the end of the current week (Sunday).
     */
    fun getWeekEnd(date: LocalDate = LocalDate.now()): LocalDate {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }

    /**
     * Get the end of the current month.
     */
    fun getMonthEnd(date: LocalDate = LocalDate.now()): LocalDate {
        return date.withDayOfMonth(date.lengthOfMonth())
    }

    /**
     * Generate weekly challenges based on current date.
     * Returns a list of challenges with their targets.
     */
    fun getWeeklyChallenges(date: LocalDate = LocalDate.now()): List<Challenge> {
        val weekNumber = date.dayOfYear / 7  // Simple week identifier

        return listOf(
            Challenge(
                id = "weekly_entries_${weekNumber}",
                name = "本周写作目标",
                description = "本周写3篇日记",
                type = ChallengeType.WEEKLY,
                target = 3,
                category = ChallengeCategory.WRITING
            ),
            Challenge(
                id = "weekly_words_${weekNumber}",
                name = "文字达人",
                description = "本周累计写1000字",
                type = ChallengeType.WEEKLY,
                target = 1000,
                category = ChallengeCategory.WRITING
            ),
            Challenge(
                id = "weekly_moods_${weekNumber}",
                name = "情绪记录",
                description = "本周记录3种不同心情",
                type = ChallengeType.WEEKLY,
                target = 3,
                category = ChallengeCategory.MOOD
            ),
            Challenge(
                id = "weekly_weather_${weekNumber}",
                name = "天气观察员",
                description = "本周记录2种不同天气",
                type = ChallengeType.WEEKLY,
                target = 2,
                category = ChallengeCategory.WRITING
            )
        )
    }

    /**
     * Generate monthly challenges based on current date.
     */
    fun getMonthlyChallenges(date: LocalDate = LocalDate.now()): List<Challenge> {
        val monthKey = "${date.year}_${date.monthValue}"

        return listOf(
            Challenge(
                id = "monthly_entries_${monthKey}",
                name = "月度写作",
                description = "本月写15篇日记",
                type = ChallengeType.MONTHLY,
                target = 15,
                category = ChallengeCategory.WRITING
            ),
            Challenge(
                id = "monthly_words_${monthKey}",
                name = "万字作家",
                description = "本月累计写5000字",
                type = ChallengeType.MONTHLY,
                target = 5000,
                category = ChallengeCategory.WRITING
            ),
            Challenge(
                id = "monthly_tags_${monthKey}",
                name = "标签探索者",
                description = "本月使用5个不同标签",
                type = ChallengeType.MONTHLY,
                target = 5,
                category = ChallengeCategory.EXPLORATION
            ),
            Challenge(
                id = "monthly_long_${monthKey}",
                name = "深度写作",
                description = "本月写一篇超过1000字的日记",
                type = ChallengeType.MONTHLY,
                target = 1000,
                category = ChallengeCategory.WRITING
            ),
            Challenge(
                id = "monthly_streak_${monthKey}",
                name = "坚持之星",
                description = "本月连续写7天",
                type = ChallengeType.MONTHLY,
                target = 7,
                category = ChallengeCategory.STREAK
            )
        )
    }

    /**
     * Calculate progress for a specific challenge based on diary data.
     */
    suspend fun calculateChallengeProgress(
        challenge: Challenge,
        diaryDao: DiaryDao,
        tagDao: TagDao,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): Int {
        val now = LocalDate.now()
        val start = when (challenge.type) {
            ChallengeType.WEEKLY -> getWeekStart(now)
            ChallengeType.MONTHLY -> getMonthStart(now)
        }
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return when (challenge.category) {
            ChallengeCategory.WRITING -> {
                when {
                    challenge.name.contains("写作目标") || challenge.name.contains("月度写作") -> {
                        diaryDao.getEntryCountInRange(startMillis, endMillis)
                    }
                    challenge.name.contains("文字") || challenge.name.contains("万字") -> {
                        diaryDao.getWordCountInRange(startMillis, endMillis).toInt()
                    }
                    challenge.name.contains("深度") -> {
                        diaryDao.getMaxWordCountInRange(startMillis, endMillis)
                    }
                    challenge.name.contains("天气") -> {
                        diaryDao.getDistinctWeatherCountInRange(startMillis, endMillis)
                    }
                    else -> 0
                }
            }
            ChallengeCategory.MOOD -> {
                diaryDao.getDistinctMoodCountInRange(startMillis, endMillis)
            }
            ChallengeCategory.EXPLORATION -> {
                when {
                    challenge.name.contains("标签") -> {
                        diaryDao.getDistinctTagCountInRange(startMillis, endMillis)
                    }
                    else -> 0
                }
            }
            ChallengeCategory.STREAK -> {
                // Calculate streak within the challenge period
                val timestamps = diaryDao.getTimestampsInRange(startMillis, endMillis)
                val dates = timestamps.map {
                    java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                }.toSet()
                computeMaxConsecutiveDays(dates, start, now)
            }
        }
    }

    /**
     * Compute maximum consecutive days with entries within a date range.
     */
    private fun computeMaxConsecutiveDays(
        dates: Set<LocalDate>,
        start: LocalDate,
        end: LocalDate
    ): Int {
        var maxStreak = 0
        var currentStreak = 0
        var date = start

        while (!date.isAfter(end)) {
            if (date in dates) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 0
            }
            date = date.plusDays(1)
        }

        return maxStreak
    }
}

enum class ChallengeType {
    WEEKLY,
    MONTHLY
}

enum class ChallengeCategory {
    WRITING,
    MOOD,
    EXPLORATION,
    STREAK
}

data class Challenge(
    val id: String,
    val name: String,
    val description: String,
    val type: ChallengeType,
    val target: Int,
    val category: ChallengeCategory
)

data class ChallengeProgress(
    val challenge: Challenge,
    val progress: Int,
    val isCompleted: Boolean
)
