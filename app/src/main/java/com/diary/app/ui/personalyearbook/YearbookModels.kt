package com.diary.app.ui.personalyearbook

data class YearbookStats(
    val totalEntries: Int,
    val totalWords: Int,
    val topMood: Int?,
    val bestMonth: String,
    val longestStreak: Int,
    val monthlyDistribution: List<Int>,
    val moodDistribution: Map<Int, Int>
)

data class EmotionPoint(
    val entryId: Long,
    val value: Float
)

data class NarrativeArc(
    val title: String,
    val entries: List<Long>,
    val turningPoint: Long,
    val emotionTrajectory: List<EmotionPoint>,
    val summary: String
)

data class MetaphorPhase(
    val period: String,
    val description: String
)

data class MonthHighlight(
    val month: Int,
    val entryId: Long,
    val entryTitle: String,
    val reason: String
)

data class YearbookData(
    val year: Int,
    val arcs: List<NarrativeArc>,
    val monthHighlights: List<MonthHighlight>,
    val metaphor: String,
    val metaphorEvolution: List<MetaphorPhase>,
    val topPhotos: List<String>,
    val stats: YearbookStats
)

data class TimelineEvent(
    val entryId: Long,
    val date: String,
    val title: String,
    val type: String
)
