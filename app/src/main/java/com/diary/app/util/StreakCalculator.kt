package com.diary.app.util

import java.time.LocalDate

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
