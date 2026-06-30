package com.diary.app.ui.stats

data class AnalysisCenterInsight(
    val title: String,
    val text: String,
    val priority: Int,
)

data class AnalysisCenterSummary(
    val primaryInsight: AnalysisCenterInsight,
    val secondaryInsights: List<AnalysisCenterInsight>,
)

data class AnalysisCenterMetric(
    val label: String,
    val value: String,
)

data class AnalysisCenterHome(
    val summary: AnalysisCenterSummary,
    val sectionOrder: List<String>,
    val keyMetrics: List<AnalysisCenterMetric>,
    val topInsights: List<AnalysisCenterInsight>,
)

data class AnalysisMoodTrend(
    val recent30Avg: Double?,
    val previous30Avg: Double?,
    val direction: TrendDirection,
)

data class AnalysisWritingHabit(
    val avgPerWeek: Double,
    val mostActiveDay: String,
    val mostActiveTime: String,
    val avgWritingMinutes: Int? = null,
)

data class AnalysisMoodWeatherInsight(
    val text: String,
    val moodLevel: Float,
    val bestWeather: String = "",
    val worstWeather: String = "",
    val bestAvgMood: Float = 0f,
    val worstAvgMood: Float = 0f,
    val overallAvgMood: Float = 0f,
    val perWeatherAverages: Map<String, Float> = emptyMap(),
)
