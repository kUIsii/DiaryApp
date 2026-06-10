package com.diary.app.ui.home

data class CalendarMoodSummary(
    val primaryMoodLevel: Int?,
    val accentMoodLevel: Int?,
    val hasMixedMoods: Boolean,
    val entryCount: Int
)

internal fun buildCalendarMoodSummary(
    moodLevels: List<Int>,
    entryCount: Int = moodLevels.size
): CalendarMoodSummary {
    val orderedMoodLevels = moodLevels
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<Int, Int>> { it.value }
                .thenByDescending { it.key }
        )
        .map { it.key }

    return CalendarMoodSummary(
        primaryMoodLevel = orderedMoodLevels.firstOrNull(),
        accentMoodLevel = orderedMoodLevels.getOrNull(1),
        hasMixedMoods = orderedMoodLevels.size > 1,
        entryCount = entryCount
    )
}
