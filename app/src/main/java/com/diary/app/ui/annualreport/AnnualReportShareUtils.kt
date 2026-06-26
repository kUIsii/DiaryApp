package com.diary.app.ui.annualreport

fun buildAnnualReportShareText(report: AnnualReport): String {
    return buildList {
        add("${report.year}年度日记报告")
        add("这一年写了 ${report.totalEntries} 篇日记，共 ${report.totalWords} 字。")
        add(
            "最长连续记录 ${report.longestStreak} 天，" +
                "最常在「${report.mostActiveTime}」写作，" +
                "最常在「${report.mostActiveDay}」动笔。"
        )

        if (report.topTags.isNotEmpty()) {
            val tagSummary = report.topTags
                .take(3)
                .joinToString("、") { "${it.name}(${it.count}次)" }
            add("最常用的标签：$tagSummary")
        }
    }.joinToString("\n")
}
