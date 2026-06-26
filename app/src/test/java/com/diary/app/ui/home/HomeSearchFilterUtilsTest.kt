package com.diary.app.ui.home

import com.diary.app.data.DiaryPreview
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSearchFilterUtilsTest {

    private val sampleEntry = DiaryPreview(
        id = 7,
        title = "杭州旅行",
        plainText = "今天在西湖边散步，记录了很多细节",
        moodLevel = 4,
        weather = "晴",
        location = "杭州",
        latitude = null,
        longitude = null,
        isFavorite = true,
        createdAt = 1_720_000_000_000,
        updatedAt = 1_720_000_000_000
    )

    @Test
    fun `advanced filters require matching tag location and word count`() {
        val params = HomeSearchParams(
            query = "",
            moods = setOf(4),
            weather = setOf("晴"),
            favorites = true,
            dates = null,
            tagNames = setOf("旅行"),
            locationQuery = "杭",
            wordCountRange = SearchWordCountRange.SHORT
        )

        assertTrue(
            matchesHomeSearchFilters(
                entry = sampleEntry,
                params = params,
                entryTagNames = setOf("旅行", "散步")
            )
        )
    }

    @Test
    fun `advanced filters reject entries outside requested word count band`() {
        val params = HomeSearchParams(
            query = "",
            moods = emptySet(),
            weather = emptySet(),
            favorites = false,
            dates = null,
            tagNames = emptySet(),
            locationQuery = null,
            wordCountRange = SearchWordCountRange.LONG
        )

        assertFalse(
            matchesHomeSearchFilters(
                entry = sampleEntry,
                params = params,
                entryTagNames = emptySet()
            )
        )
    }
}
