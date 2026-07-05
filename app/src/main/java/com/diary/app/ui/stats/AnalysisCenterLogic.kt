package com.diary.app.ui.stats

fun buildAnalysisCenterSummary(
    totalEntries: Int,
    currentStreak: Int,
    thisMonthEntries: Int,
    moodTrend: AnalysisMoodTrend?,
    writingHabit: AnalysisWritingHabit?,
    moodWeatherInsight: AnalysisMoodWeatherInsight?
): AnalysisCenterSummary {
    val insights = mutableListOf<AnalysisCenterInsight>()

    moodWeatherInsight?.let {
        insights += AnalysisCenterInsight(
            title = "天气与心情",
            text = it.text,
            priority = 0
        )
    }

    moodTrend?.let {
        val trendText = when (it.direction) {
            TrendDirection.UP -> "最近 30 天心情在上升"
            TrendDirection.DOWN -> "最近 30 天心情在下降"
            TrendDirection.FLAT -> "最近 30 天心情比较稳定"
        }
        insights += AnalysisCenterInsight(
            title = "心情趋势",
            text = trendText,
            priority = 1
        )
    }

    writingHabit?.let {
        insights += AnalysisCenterInsight(
            title = "写作习惯",
            text = "你更常在 ${it.mostActiveTime} 写作，主要活跃在 ${it.mostActiveDay}",
            priority = 2
        )
    }

    if (thisMonthEntries > 0) {
        insights += AnalysisCenterInsight(
            title = "本月活跃",
            text = "本月已经记录了 $thisMonthEntries 篇",
            priority = 3
        )
    }

    val fallback = AnalysisCenterInsight(
        title = "写作状态",
        text = "你已经记录了 $totalEntries 篇日记，当前连续 $currentStreak 天",
        priority = 99
    )

    val primary = insights.minByOrNull { it.priority } ?: fallback
    val secondaries = insights.filterNot { it == primary }

    return AnalysisCenterSummary(
        primaryInsight = primary,
        secondaryInsights = secondaries
    )
}

fun buildAnalysisCenterHome(
    summary: AnalysisCenterSummary,
    totalEntries: Int,
    currentStreak: Int,
    thisMonthEntries: Int,
    topInsights: List<AnalysisCenterInsight>,
    deepDiveEntries: List<String> = emptyList()
): AnalysisCenterHome {
    return AnalysisCenterHome(
        summary = summary,
        sectionOrder = if (deepDiveEntries.isEmpty()) {
            listOf("摘要", "关键指标", "洞察")
        } else {
            listOf("摘要", "关键指标", "洞察", "深挖入口")
        },
        keyMetrics = listOf(
            AnalysisCenterMetric("总记录", totalEntries.toString()),
            AnalysisCenterMetric("连续天数", currentStreak.toString()),
            AnalysisCenterMetric("本月记录", thisMonthEntries.toString())
        ),
        topInsights = topInsights
    )
}

data class DeepDiveGroup(
    val title: String,
    val entries: List<String>
)

fun buildDeepDiveGroups(): List<DeepDiveGroup> {
    return listOf(
        DeepDiveGroup("时间报告", listOf("月度报告", "季度回顾", "年度报告", "个人年鉴")),
        DeepDiveGroup("心情洞察", listOf("情绪分析", "情绪雷达")),
        DeepDiveGroup("文本洞察", listOf("写作分析", "文字显微镜")),
        DeepDiveGroup("结构关联", listOf("条目图谱"))
    )
}

fun defaultAnalysisCenterSummary(): AnalysisCenterSummary {
    return AnalysisCenterSummary(
        primaryInsight = AnalysisCenterInsight(
            title = "写作状态",
            text = "开始记录后，这里会显示最重要的趋势和洞察",
            priority = 99
        ),
        secondaryInsights = emptyList()
    )
}

fun defaultAnalysisCenterHome(): AnalysisCenterHome {
    return AnalysisCenterHome(
        summary = defaultAnalysisCenterSummary(),
        sectionOrder = listOf("摘要", "关键指标", "洞察"),
        keyMetrics = emptyList(),
        topInsights = emptyList()
    )
}
