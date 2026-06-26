package com.diary.app.ui.annualreport

import com.diary.app.data.TagUsage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AnnualReportShareUtilsTest {

    @Test
    fun `annual share text includes key stats and top tags`() {
        val report = AnnualReport(
            year = 2026,
            totalEntries = 120,
            totalWords = 54321,
            monthlyMood = emptyList(),
            monthlyCount = emptyList(),
            nightEntries = 18,
            latestEntryTime = LocalTime.of(23, 40),
            latestEntryTitle = "",
            longestEntryTitle = "",
            longestWords = 0,
            longestEntryDate = null,
            longestSilenceDays = 0,
            silenceStart = null,
            silenceEnd = null,
            happiestEntryTitle = "",
            happiestMood = 5,
            happiestDate = null,
            mostActiveTime = "深夜",
            mostActiveDay = "周三",
            timeDistribution = emptyList(),
            weatherMood = emptyList(),
            topTags = listOf(
                TagUsage(tagId = 1, name = "成长", color = 0L, count = 8),
                TagUsage(tagId = 2, name = "旅行", color = 0L, count = 5)
            ),
            firstEntryDate = LocalDate.of(2026, 1, 3),
            longestStreak = 12,
            photoCount = 0,
            mostPhotosInSingleEntry = 0,
            mostPhotosEntryTitle = "",
            mostPhotosEntryDate = null,
            earliestEntryTime = LocalTime.of(6, 20),
            earliestEntryTitle = "",
            mostProductiveDate = null,
            mostProductiveCount = 0
        )

        assertEquals(
            """
            2026年度日记报告
            这一年写了 120 篇日记，共 54321 字。
            最长连续记录 12 天，最常在「深夜」写作，最常在「周三」动笔。
            最常用的标签：成长(8次)、旅行(5次)
            """.trimIndent(),
            buildAnnualReportShareText(report)
        )
    }

    @Test
    fun `annual share text omits tag line when there are no tags`() {
        val report = AnnualReport(
            year = 2026,
            totalEntries = 2,
            totalWords = 300,
            monthlyMood = emptyList(),
            monthlyCount = emptyList(),
            nightEntries = 0,
            latestEntryTime = null,
            latestEntryTitle = "",
            longestEntryTitle = "",
            longestWords = 0,
            longestEntryDate = null,
            longestSilenceDays = 0,
            silenceStart = null,
            silenceEnd = null,
            happiestEntryTitle = "",
            happiestMood = 0,
            happiestDate = null,
            mostActiveTime = "上午",
            mostActiveDay = "周一",
            timeDistribution = emptyList(),
            weatherMood = emptyList(),
            topTags = emptyList(),
            firstEntryDate = null,
            longestStreak = 1,
            photoCount = 0,
            mostPhotosInSingleEntry = 0,
            mostPhotosEntryTitle = "",
            mostPhotosEntryDate = null,
            earliestEntryTime = null,
            earliestEntryTitle = "",
            mostProductiveDate = null,
            mostProductiveCount = 0
        )

        assertEquals(
            """
            2026年度日记报告
            这一年写了 2 篇日记，共 300 字。
            最长连续记录 1 天，最常在「上午」写作，最常在「周一」动笔。
            """.trimIndent(),
            buildAnnualReportShareText(report)
        )
    }
}
