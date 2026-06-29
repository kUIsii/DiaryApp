package com.diary.app.ui.annualreport

import java.time.LocalDate

data class StoryChapter(
    val title: String,
    val summary: String,
    val entryIds: List<Long>,
    val emotionSparkline: List<Float>,
    val style: String
)

data class DiscoveredPattern(
    val id: String,
    val description: String,
    val type: String,
    val relatedEntryIds: List<Long>,
    val significance: Float
)

data class CrossYearInsight(
    val dimension: String,
    val currentYearValue: Any,
    val priorYearValue: Any,
    val changePercent: Float,
    val description: String
)

data class UserAnnotation(
    val id: String,
    val chapterTitle: String,
    val paragraphIndex: Int,
    val note: String,
    val createdAt: Long
)

data class BlindSpot(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val inferredReason: String,
    val followUpEntryId: Long?,
    val confidence: Float
)

data class AnnualStory(
    val year: Int,
    val chapters: List<StoryChapter>,
    val patterns: List<DiscoveredPattern>,
    val crossYearInsights: List<CrossYearInsight>?,
    val userAnnotations: List<UserAnnotation>,
    val blindSpotNotes: List<BlindSpot>
) {
    companion object {
        fun empty(year: Int) = AnnualStory(
            year = year,
            chapters = emptyList(),
            patterns = emptyList(),
            crossYearInsights = null,
            userAnnotations = emptyList(),
            blindSpotNotes = emptyList()
        )
    }
}

data class AiContextBundle(
    val year: Int,
    val totalEntries: Int,
    val totalWords: Int,
    val monthlyMood: List<Float?>,
    val monthlyCount: List<Int>,
    val topTags: List<String>,
    val topWords: List<String>,
    val sampleEntries: List<SampleEntry>,
    val longestSilenceDays: Int,
    val silencePeriod: String,
    val nightEntryRatio: Float,
    val mostActiveTime: String,
    val weatherDistribution: List<String>,
    val priorYearExists: Boolean,
    val priorYearTotalEntries: Int = 0,
    val priorYearTotalWords: Int = 0,
    val priorYearTopTags: List<String> = emptyList()
)

data class SampleEntry(
    val id: Long,
    val title: String,
    val date: String,
    val plainText: String,
    val moodLevel: Int?,
    val weather: String?
)
