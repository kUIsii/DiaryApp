package com.diary.app.ui.monthlyreport

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class MonthlyReportShareUtilsTest {

    @Test
    fun `monthly share text includes key stats and top tags`() {
        val report = MonthlyReport(
            year = 2026,
            month = 6,
            totalEntries = 8,
            totalWords = 12345,
            avgMood = 4.2f,
            happiestEntryTitle = "",
            happiestMood = 5,
            longestEntryTitle = "",
            longestWords = 0,
            nightEntries = 2,
            earliestEntryTime = LocalTime.of(7, 30),
            mostActiveDay = 18,
            photoCount = 0,
            tags = listOf(
                MonthlyReport.TagStat(name = "工作", color = 0L, count = 3),
                MonthlyReport.TagStat(name = "旅行", color = 0L, count = 2)
            ),
            dailyWordCounts = listOf(100, 0, 200, 0, 300, 400, 0, 500),
            dailyMoodAverages = emptyList(),
            totalDurationMinutes = 90
        )

        assertEquals(
            """
            2026年6月日记月报
            本月写了 8 篇日记，共 12345 字。
            其中有 5 天留下了记录，累计写作 90 分钟。
            平均心情：4.2（不错）
            最活跃的一天：18日
            常用标签：工作(3次)、旅行(2次)
            """.trimIndent(),
            buildMonthlyReportShareText(report)
        )
    }

    @Test
    fun `monthly share text omits optional sections when data is missing`() {
        val report = MonthlyReport(
            year = 2026,
            month = 1,
            totalEntries = 1,
            totalWords = 20,
            avgMood = null,
            happiestEntryTitle = "",
            happiestMood = 0,
            longestEntryTitle = "",
            longestWords = 0,
            nightEntries = 0,
            earliestEntryTime = null,
            mostActiveDay = null,
            photoCount = 0,
            tags = emptyList(),
            dailyWordCounts = listOf(20),
            dailyMoodAverages = emptyList(),
            totalDurationMinutes = 0
        )

        assertEquals(
            """
            2026年1月日记月报
            本月写了 1 篇日记，共 20 字。
            其中有 1 天留下了记录。
            """.trimIndent(),
            buildMonthlyReportShareText(report)
        )
    }
}
