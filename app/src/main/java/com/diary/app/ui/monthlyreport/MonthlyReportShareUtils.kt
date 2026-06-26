package com.diary.app.ui.monthlyreport

private val monthlyShareMoodLabels = listOf("", "很差", "不好", "一般", "不错", "很棒")

fun buildMonthlyReportShareText(report: MonthlyReport): String {
    val activeDays = report.dailyWordCounts.count { it > 0 }

    return buildList {
        add("${report.year}年${report.month}月日记月报")
        add("本月写了 ${report.totalEntries} 篇日记，共 ${report.totalWords} 字。")

        val activityLine = buildString {
            append("其中有 $activeDays 天留下了记录")
            if (report.totalDurationMinutes > 0) {
                append("，累计写作 ${report.totalDurationMinutes} 分钟")
            }
            append("。")
        }
        add(activityLine)

        report.avgMood?.let { mood ->
            val moodLabel = monthlyShareMoodLabels[mood.toInt().coerceIn(1, 5)]
            add("平均心情：${String.format("%.1f", mood)}（$moodLabel）")
        }

        report.mostActiveDay?.let { day ->
            add("最活跃的一天：${day}日")
        }

        if (report.tags.isNotEmpty()) {
            val tagSummary = report.tags
                .take(3)
                .joinToString("、") { "${it.name}(${it.count}次)" }
            add("常用标签：$tagSummary")
        }
    }.joinToString("\n")
}
