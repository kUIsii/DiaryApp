package com.diary.app.ui.writingcenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingGrowthCenterLogicTest {

    @Test
    fun `default primary action is write`() {
        assertEquals(WritingGrowthPrimaryAction.WRITE, defaultPrimaryAction())
    }

    @Test
    fun `content model always includes four sections`() {
        val model = buildWritingGrowthCenterContent(
            latestEntryTitle = null,
            hasAiSupport = false,
            todayWordCount = 0,
            writingDaysThisWeek = 0,
            recentSedimentedContent = emptyList()
        )

        assertEquals(
            listOf("今日起点", "成长概览", "快捷入口", "最近沉淀"),
            model.sections.map { it.title }
        )
    }

    @Test
    fun `quick actions keep write continue and overview order`() {
        val model = buildWritingGrowthCenterContent(
            latestEntryTitle = "昨日日记",
            hasAiSupport = true,
            todayWordCount = 124,
            writingDaysThisWeek = 3,
            recentSedimentedContent = listOf("一个可复用的句子")
        )

        assertEquals(
            listOf("写一篇", "继续最近", "看成长概览"),
            model.sections.first { it.title == "快捷入口" }.quickActions.map { it.label }
        )
    }

    @Test
    fun `quick entry items route to the writing suite destinations`() {
        val model = buildWritingGrowthCenterContent(
            latestEntryTitle = "昨日日记",
            hasAiSupport = true,
            todayWordCount = 124,
            writingDaysThisWeek = 3,
            recentSedimentedContent = listOf("一个可复用的句子")
        )

        assertEquals(
            listOf(
                WritingGrowthItemTarget.WRITING_LAB,
                WritingGrowthItemTarget.WRITING_HINT,
                WritingGrowthItemTarget.SMALL_WINS
            ),
            model.sections.first { it.title == "快捷入口" }.items.map { it.target }
        )
    }

    @Test
    fun `fallback sediment content appears when recent list is empty`() {
        val model = buildWritingGrowthCenterContent(
            latestEntryTitle = null,
            hasAiSupport = false,
            todayWordCount = 0,
            writingDaysThisWeek = 0,
            recentSedimentedContent = emptyList()
        )

        assertTrue(model.sections.last().items.isNotEmpty())
        assertTrue(model.sections.last().items.all { it.target == WritingGrowthItemTarget.EDITOR })
    }
}
