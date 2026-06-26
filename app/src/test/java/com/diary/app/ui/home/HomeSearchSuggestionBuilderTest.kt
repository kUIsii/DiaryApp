package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSearchSuggestionBuilderTest {

    @Test
    fun `blank query shows recent searches first`() {
        val suggestions = buildHomeSearchSuggestions(
            query = "",
            recentSearches = listOf("旅行", "工作复盘"),
            tagSuggestions = listOf("成长"),
            locationSuggestions = listOf("杭州")
        )

        assertEquals(
            listOf(
                HomeSearchSuggestion("旅行", SearchSuggestionType.HISTORY),
                HomeSearchSuggestion("工作复盘", SearchSuggestionType.HISTORY)
            ),
            suggestions
        )
    }

    @Test
    fun `typed query merges history tags and locations without duplicates`() {
        val suggestions = buildHomeSearchSuggestions(
            query = "ha",
            recentSearches = listOf("hangzhou trip"),
            tagSuggestions = listOf("happy", "hangzhou trip"),
            locationSuggestions = listOf("Hangzhou", "Harbin")
        )

        assertEquals(
            listOf(
                HomeSearchSuggestion("hangzhou trip", SearchSuggestionType.HISTORY),
                HomeSearchSuggestion("happy", SearchSuggestionType.TAG),
                HomeSearchSuggestion("Hangzhou", SearchSuggestionType.LOCATION),
                HomeSearchSuggestion("Harbin", SearchSuggestionType.LOCATION)
            ),
            suggestions
        )
    }
}
