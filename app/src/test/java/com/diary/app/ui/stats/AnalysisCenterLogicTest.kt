package com.diary.app.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisCenterLogicTest {

    @Test
    fun `analysis center summary prefers the strongest available insight`() {
        val summary = buildAnalysisCenterSummary(
            totalEntries = 42,
            currentStreak = 9,
            thisMonthEntries = 11,
            moodTrend = AnalysisMoodTrend(
                recent30Avg = 4.2,
                previous30Avg = 3.4,
                direction = TrendDirection.UP
            ),
            writingHabit = AnalysisWritingHabit(
                avgPerWeek = 3.5,
                mostActiveDay = "周三",
                mostActiveTime = "晚上",
                avgWritingMinutes = 18
            ),
            moodWeatherInsight = AnalysisMoodWeatherInsight(
                text = "晴天时心情最好",
                moodLevel = 4.4f
            )
        )

        assertEquals("天气与心情", summary.primaryInsight.title)
        assertEquals("晴天时心情最好", summary.primaryInsight.text)
    }

    @Test
    fun `analysis center summary falls back cleanly when optional insights are missing`() {
        val summary = buildAnalysisCenterSummary(
            totalEntries = 0,
            currentStreak = 0,
            thisMonthEntries = 0,
            moodTrend = null,
            writingHabit = null,
            moodWeatherInsight = null
        )

        assertEquals("写作状态", summary.primaryInsight.title)
        assertEquals("你已经记录了 0 篇日记，当前连续 0 天", summary.primaryInsight.text)
        assertTrue(summary.secondaryInsights.isEmpty())
    }

    @Test
    fun `analysis center home exposes the correct section order`() {
        val home = buildAnalysisCenterHome(
            summary = buildAnalysisCenterSummary(
                totalEntries = 42,
                currentStreak = 9,
                thisMonthEntries = 11,
                moodTrend = null,
                writingHabit = null,
                moodWeatherInsight = null
            ),
            totalEntries = 42,
            currentStreak = 9,
            thisMonthEntries = 11,
            topInsights = emptyList(),
            deepDiveEntries = listOf("月度报告", "季度回顾", "年度报告")
        )

        assertEquals(listOf("摘要", "关键指标", "洞察", "深挖入口"), home.sectionOrder)
    }

    @Test
    fun `deep dive groups keep time reports together`() {
        val groups = buildDeepDiveGroups()

        assertEquals(listOf("时间报告", "心情洞察", "文本洞察", "结构关联"), groups.map { it.title })
        assertEquals(listOf("月度报告", "季度回顾", "年度报告", "个人年鉴"), groups.first().entries)
    }
}
