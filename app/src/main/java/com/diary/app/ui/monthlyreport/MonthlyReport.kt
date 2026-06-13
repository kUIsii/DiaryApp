package com.diary.app.ui.monthlyreport

import java.time.LocalTime

data class MonthlyReport(
    val year: Int,
    val month: Int,
    val totalEntries: Int,
    val totalWords: Int,
    val avgMood: Float?,
    val happiestEntryTitle: String,
    val happiestMood: Int,
    val longestEntryTitle: String,
    val longestWords: Int,
    val nightEntries: Int,
    val earliestEntryTime: LocalTime?,
    val mostActiveDay: Int?,
    val photoCount: Int,
    val tags: List<TagStat>,
    val dailyWordCounts: List<Int>,
) {
    data class TagStat(val name: String, val color: Long, val count: Int)
}
