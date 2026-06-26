package com.diary.app.ui.weeklyreport

fun buildWeeklyReportShareText(report: WeeklyReport): String {
    val lines = mutableListOf<String>()
    lines += "${report.year}年第${report.weekNumber}周日记周报"
    lines += "本周写了 ${report.totalEntries} 篇日记，共 ${report.totalWords} 字。"

    val activityLine = buildString {
        append("其中有 ${report.activeDays} 天留下了记录")
        if (report.totalDurationMinutes > 0) {
            append("，累计写作 ${report.totalDurationMinutes} 分钟")
        }
        append("。")
    }
    lines += activityLine

    report.avgMood?.let { mood ->
        lines += "平均心情：${String.format("%.1f", mood)}"
    }

    if (report.longestEntryTitle.isNotBlank() && report.longestWords > 0) {
        lines += "最长的一篇：${report.longestEntryTitle}（${report.longestWords}字）"
    }

    if (report.tags.isNotEmpty()) {
        val topTags = report.tags.take(3).joinToString("、") { "${it.name}(${it.count}次)" }
        lines += "常用标签：$topTags"
    }

    return lines.joinToString("\n")
}
