package com.diary.app.ui.readingcenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingCenterLogicTest {

    @Test
    fun `reading center content exposes continue reading quick action first`() {
        val content = buildReadingCenterContent(
            session = ReadingSessionSnapshot(
                diaryId = 12L,
                title = "昨夜的地铁",
                pageIndex = 3,
                totalPages = 8,
                themeName = "暖纸纹",
                lastReadAt = 1_000L,
                hasActiveFocus = false
            ),
            recentEntries = listOf("清晨散步", "书店角落"),
            completedFocusSessions = 2
        )

        assertEquals("继续阅读", content.heroActions.first().label)
    }

    @Test
    fun `session update keeps current page within total page bounds`() {
        val updated = updateReadingSessionPage(
            session = ReadingSessionSnapshot(pageIndex = 2, totalPages = 5),
            requestedPage = 8
        )

        assertEquals(4, updated.pageIndex)
    }

    @Test
    fun `focus summary prefers active reading title when session exists`() {
        val summary = buildReadingFocusSummary(
            session = ReadingSessionSnapshot(
                title = "雨后的公园",
                pageIndex = 1,
                totalPages = 4,
                hasActiveFocus = true
            ),
            selectedDuration = 25
        )

        assertTrue(summary.contains("雨后的公园"))
    }

    @Test
    fun `review summary emphasizes structure first for single entry`() {
        val summary = buildReadingReviewSummary(
            totalWords = 1200,
            paragraphCount = 7,
            headingCount = 3
        )

        assertTrue(summary.startsWith("这篇内容共有"))
    }

    @Test
    fun `reading theme preview description mentions active theme and reading space`() {
        val description = buildReadingThemePreviewDescription(
            themeName = "暖纸纹",
            isDefault = true
        )

        assertEquals("暖纸纹 · 当前默认阅读空间", description)
    }
}
