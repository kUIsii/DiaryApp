package com.diary.app.ui.home

import com.diary.app.data.DiaryPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenLogicTest {

    @Test
    fun `todo shortcut resolves to todo destination instead of timeline`() {
        assertEquals(HomeShortcutDestination.TODO, resolveHomeShortcutDestination("todo"))
    }

    @Test
    fun `search empty state appears only when query has no results`() {
        val result = listOf(samplePreview(id = 1, title = "今天"))

        assertTrue(shouldShowSearchEmptyState(query = "旅行", results = emptyList()))
        assertFalse(shouldShowSearchEmptyState(query = "", results = emptyList()))
        assertFalse(shouldShowSearchEmptyState(query = "旅行", results = result))
    }

    @Test
    fun `browse sections hide while user is searching`() {
        assertTrue(shouldShowBrowseSections(query = ""))
        assertTrue(shouldShowBrowseSections(query = "  "))
        assertFalse(shouldShowBrowseSections(query = "旅行"))
    }

    @Test
    fun `home highlight state refresh replaces previous random review and on this day entries`() {
        val firstRandom = samplePreview(id = 1, title = "第一篇")
        val nextRandom = samplePreview(id = 2, title = "第二篇")
        val previous = HomeHighlightsState(
            randomEntry = firstRandom,
            onThisDayEntries = listOf(samplePreview(id = 3, title = "去年今天"))
        )

        val refreshed = refreshedHomeHighlightsState(
            previous = previous,
            randomEntry = nextRandom,
            onThisDayEntries = listOf(
                samplePreview(id = 4, title = "前年今天"),
                samplePreview(id = 5, title = "大前年今天")
            )
        )

        assertEquals(nextRandom, refreshed.randomEntry)
        assertEquals(listOf(4L, 5L), refreshed.onThisDayEntries.map { it.id })
    }

    private fun samplePreview(
        id: Long,
        title: String,
        plainText: String = ""
    ): DiaryPreview {
        return DiaryPreview(
            id = id,
            title = title,
            plainText = plainText,
            moodLevel = null,
            weather = null,
            location = null,
            latitude = null,
            longitude = null,
            isFavorite = false,
            createdAt = 0L,
            updatedAt = 0L
        )
    }
}
