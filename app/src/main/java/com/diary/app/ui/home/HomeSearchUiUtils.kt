package com.diary.app.ui.home

import com.diary.app.data.DiaryPreview

enum class SearchSuggestionType {
    HISTORY,
    TAG,
    LOCATION
}

data class HomeSearchSuggestion(
    val value: String,
    val type: SearchSuggestionType
)

enum class SearchWordCountRange {
    SHORT,
    MEDIUM,
    LONG
}

data class HomeSearchParams(
    val query: String,
    val moods: Set<Int>,
    val weather: Set<String>,
    val favorites: Boolean,
    val dates: Pair<Long, Long>?,
    val tagNames: Set<String> = emptySet(),
    val locationQuery: String? = null,
    val wordCountRange: SearchWordCountRange? = null
)

fun buildHomeSearchSuggestions(
    query: String,
    recentSearches: List<String>,
    tagSuggestions: List<String>,
    locationSuggestions: List<String>,
    limit: Int = 8
): List<HomeSearchSuggestion> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) {
        return recentSearches
            .take(limit)
            .map { HomeSearchSuggestion(it, SearchSuggestionType.HISTORY) }
    }

    val normalizedQuery = trimmed.lowercase()
    val seen = linkedSetOf<String>()
    val suggestions = mutableListOf<HomeSearchSuggestion>()

    fun append(values: List<String>, type: SearchSuggestionType) {
        values.forEach { value ->
            if (suggestions.size >= limit) return@forEach
            val normalizedValue = value.trim()
            if (normalizedValue.isBlank()) return@forEach
            if (!normalizedValue.lowercase().contains(normalizedQuery)) return@forEach
            val dedupeKey = normalizedValue.lowercase()
            if (seen.add(dedupeKey)) {
                suggestions += HomeSearchSuggestion(normalizedValue, type)
            }
        }
    }

    append(recentSearches, SearchSuggestionType.HISTORY)
    append(tagSuggestions, SearchSuggestionType.TAG)
    append(locationSuggestions, SearchSuggestionType.LOCATION)
    return suggestions
}

fun matchesHomeSearchFilters(
    entry: DiaryPreview,
    params: HomeSearchParams,
    entryTagNames: Set<String>
): Boolean {
    if (params.moods.isNotEmpty() && entry.moodLevel !in params.moods) return false
    if (params.weather.isNotEmpty() && entry.weather !in params.weather) return false
    if (params.favorites && !entry.isFavorite) return false
    if (params.dates != null) {
        val (start, end) = params.dates
        if (entry.createdAt < start || entry.createdAt >= end) return false
    }
    if (params.tagNames.isNotEmpty() && params.tagNames.none { it in entryTagNames }) return false
    val locationQuery = params.locationQuery?.trim().orEmpty()
    if (locationQuery.isNotBlank()) {
        val location = entry.location.orEmpty()
        if (!location.contains(locationQuery, ignoreCase = true)) return false
    }
    params.wordCountRange?.let { range ->
        val wordCount = estimateEntryWordCount(entry.plainText)
        val matchesRange = when (range) {
            SearchWordCountRange.SHORT -> wordCount in 1..499
            SearchWordCountRange.MEDIUM -> wordCount in 500..2000
            SearchWordCountRange.LONG -> wordCount > 2000
        }
        if (!matchesRange) return false
    }
    return true
}

fun estimateEntryWordCount(text: String): Int {
    return text.filterNot(Char::isWhitespace).length
}
