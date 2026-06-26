package com.diary.app.ui.weeklyreport

import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyReportShareUtilsTest {

    @Test
    fun `weekly share text includes key stats and top tags`() {
        val report = WeeklyReport(
            year = 2026,
            weekNumber = 26,
            startDate = "6/24",
            endDate = "6/30",
            totalEntries = 6,
            totalWords = 4321,
            avgMood = 4.0f,
            activeDays = 5,
            tags = listOf(
                WeeklyReport.TagStat(name = "工作", color = 0L, count = 3),
                WeeklyReport.TagStat(name = "散步", color = 0L, count = 2)
            ),
            dailyWordCounts = listOf(100, 0, 300, 400, 500, 600, 700),
            dailyMoodAverages = emptyList(),
            longestEntryTitle = "周末总结",
            longestWords = 1200,
            totalDurationMinutes = 75
        )

        assertEquals(
            """
            2026年第26周日记周报
            本周写了 6 篇日记，共 4321 字。
            其中有 5 天留下了记录，累计写作 75 分钟。
            平均心情：4.0
            最长的一篇：周末总结（1200字）
            常用标签：工作(3次)、散步(2次)
            """.trimIndent(),
            buildWeeklyReportShareText(report)
        )
    }

    @Test
    fun `weekly share text omits optional sections when data is missing`() {
        val report = WeeklyReport(
            year = 2026,
            weekNumber = 26,
            startDate = "6/24",
            endDate = "6/30",
            totalEntries = 1,
            totalWords = 20,
            avgMood = null,
            activeDays = 1,
            tags = emptyList(),
            dailyWordCounts = listOf(20),
            dailyMoodAverages = emptyList(),
            longestEntryTitle = "",
            longestWords = 0,
            totalDurationMinutes = 0
        )

        assertEquals(
            """
            2026年第26周日记周报
            本周写了 1 篇日记，共 20 字。
            其中有 1 天留下了记录。
            """.trimIndent(),
            buildWeeklyReportShareText(report)
        )
    }
}
