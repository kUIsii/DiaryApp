package com.diary.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Computes the consecutive writing streak from a set of entry dates.
 *
 * Logic:
 * - Find the most recent entry date (max date)
 * - If max date is older than yesterday (i.e., more than 1 day before today), streak is 0
 * - Otherwise, count consecutive days backward from the max date
 */
fun computeStreak(dates: Set<LocalDate>): Int {
    if (dates.isEmpty()) return 0
    val today = LocalDate.now()
    val maxDate = dates.maxOrNull() ?: return 0
    // If the most recent entry is more than 1 day old, streak is broken
    if (maxDate.isBefore(today.minusDays(1))) return 0
    var streak = 0
    var current = maxDate
    while (current in dates) {
        streak++
        current = current.minusDays(1)
    }
    return streak
}

/**
 * Computes streak with freeze support.
 * Frozen days count as "written" for streak continuity.
 */
fun computeStreakWithFreezes(
    dates: Set<LocalDate>,
    freezeDates: Set<LocalDate>
): Int {
    if (dates.isEmpty()) return 0
    val today = LocalDate.now()
    val allWritten = dates + freezeDates
    val maxDate = allWritten.maxOrNull() ?: return 0
    if (maxDate.isBefore(today.minusDays(1))) return 0
    var streak = 0
    var current = maxDate
    while (current in allWritten) {
        if (current in dates) streak++
        current = current.minusDays(1)
    }
    return streak
}

/**
 * Computes the longest streak ever from a set of entry dates.
 * Returns Pair(longestStreak, Pair(startDate, endDate)).
 */
fun computeLongestStreak(dates: Set<LocalDate>): Pair<Int, Pair<LocalDate, LocalDate>?> {
    if (dates.isEmpty()) return 0 to null
    val sorted = dates.sorted()
    var bestStreak = 1
    var bestStart = sorted.first()
    var bestEnd = sorted.first()
    var currentStreak = 1
    var currentStart = sorted.first()

    for (i in 1 until sorted.size) {
        if (sorted[i] == sorted[i - 1].plusDays(1)) {
            currentStreak++
        } else {
            if (currentStreak > bestStreak) {
                bestStreak = currentStreak
                bestStart = currentStart
                bestEnd = sorted[i - 1]
            }
            currentStreak = 1
            currentStart = sorted[i]
        }
    }
    if (currentStreak > bestStreak) {
        bestStreak = currentStreak
        bestStart = currentStart
        bestEnd = sorted.last()
    }
    return bestStreak to Pair(bestStart, bestEnd)
}

/**
 * Detects streak milestones from current streak.
 * Returns the milestone threshold if reached, null otherwise.
 */
fun detectStreakMilestone(streak: Int): Int? {
    val milestones = listOf(3, 7, 14, 30, 50, 100, 200, 365)
    return milestones.lastOrNull { it <= streak }
}

/**
 * Returns the visual tier for a streak count.
 */
fun streakTier(streak: Int): StreakTier = when {
    streak >= 365 -> StreakTier.LEGENDARY
    streak >= 100 -> StreakTier.DIAMOND
    streak >= 30 -> StreakTier.GOLD
    streak >= 7 -> StreakTier.SILVER
    streak >= 3 -> StreakTier.BRONZE
    else -> StreakTier.NONE
}

enum class StreakTier { NONE, BRONZE, SILVER, GOLD, DIAMOND, LEGENDARY }

/**
 * Computes monthly streak leaderboard from entry timestamps.
 * Returns map of YearMonth to streak count for that month.
 */
fun computeMonthlyLeaderboard(
    timestamps: List<Long>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<YearMonth, Int> {
    val datesByMonth = timestamps
        .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        .groupBy { YearMonth.from(it) }

    return datesByMonth.mapValues { (_, dates) ->
        computeStreak(dates.toSet())
    }
}

/**
 * Computes the best streak for a given year.
 */
fun computeYearlyBestStreak(
    timestamps: List<Long>,
    year: Int,
    zone: ZoneId = ZoneId.systemDefault()
): Int {
    val yearDates = timestamps
        .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        .filter { it.year == year }
        .toSet()
    return computeStreak(yearDates)
}

/**
 * Computes current streak considering today and freeze dates.
 * If today has no entry but a freeze is available, streak continues.
 */
fun computeStreakWithTodayFreeze(
    dates: Set<LocalDate>,
    freezeDates: Set<LocalDate>,
    todayFreezeUsed: Boolean
): Int {
    val today = LocalDate.now()
    val effectiveDates = if (todayFreezeUsed) dates + today else dates
    return computeStreakWithFreezes(effectiveDates, freezeDates)
}
