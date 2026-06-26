package com.diary.app.ui.weeklyreport

data class WeeklyReport(
    val year: Int,
    val weekNumber: Int,
    val startDate: String,
    val endDate: String,
    val totalEntries: Int,
    val totalWords: Int,
    val avgMood: Float?,
    val activeDays: Int,
    val tags: List<TagStat>,
    val dailyWordCounts: List<Int>,
    val dailyMoodAverages: List<Float?>,
    val longestEntryTitle: String,
    val longestWords: Int,
    val totalDurationMinutes: Int
) {
    data class TagStat(
        val name: String,
        val color: Long,
        val count: Int
    )
}
