package com.diary.app.ui.timeline

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.home.TagInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class FilterState(
    val selectedMoods: Set<Int> = emptySet(),
    val selectedWeathers: Set<String> = emptySet(),
    val selectedTagIds: Set<Long> = emptySet(),
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    private val allEntries: StateFlow<List<DiaryEntry>> = dao.getAllEntries()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tagsMap: StateFlow<Map<Long, List<TagInfo>>> = dao.getAllDiaryTagPairs()
        .map { pairs ->
            pairs.groupBy { it.diaryId }.mapValues { (_, tagPairs) ->
                tagPairs.map { TagInfo(it.tagId, it.name, Color(it.color)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // All unique tags for filter
    val allTags: StateFlow<List<TagInfo>> = dao.getAllDiaryTagPairs()
        .map { pairs ->
            pairs.distinctBy { it.tagId }.map { TagInfo(it.tagId, it.name, Color(it.color)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered and searched entries
    val entries: StateFlow<List<DiaryEntry>> = combine(
        allEntries,
        _searchQuery,
        _filterState,
        tagsMap
    ) { entries, query, filter, tags ->
        entries
            .sortedByDescending { it.createdAt }
            .filter { entry ->
                // Search filter
                val matchesSearch = query.isBlank() ||
                    entry.title.contains(query, ignoreCase = true) ||
                    entry.plainText.contains(query, ignoreCase = true)

                // Mood filter
                val matchesMood = filter.selectedMoods.isEmpty() ||
                    (entry.moodLevel != null && entry.moodLevel in filter.selectedMoods)

                // Weather filter
                val matchesWeather = filter.selectedWeathers.isEmpty() ||
                    (entry.weather != null && entry.weather in filter.selectedWeathers)

                // Tag filter
                val entryTags = tags[entry.id] ?: emptyList()
                val entryTagIds = entryTags.map { it.id }.toSet()
                val matchesTags = filter.selectedTagIds.isEmpty() ||
                    entryTagIds.intersect(filter.selectedTagIds).isNotEmpty()

                matchesSearch && matchesMood && matchesWeather && matchesTags
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleMoodFilter(moodLevel: Int) {
        _filterState.value = _filterState.value.copy(
            selectedMoods = if (moodLevel in _filterState.value.selectedMoods) {
                _filterState.value.selectedMoods - moodLevel
            } else {
                _filterState.value.selectedMoods + moodLevel
            }
        )
    }

    fun toggleWeatherFilter(weather: String) {
        _filterState.value = _filterState.value.copy(
            selectedWeathers = if (weather in _filterState.value.selectedWeathers) {
                _filterState.value.selectedWeathers - weather
            } else {
                _filterState.value.selectedWeathers + weather
            }
        )
    }

    fun toggleTagFilter(tagId: Long) {
        _filterState.value = _filterState.value.copy(
            selectedTagIds = if (tagId in _filterState.value.selectedTagIds) {
                _filterState.value.selectedTagIds - tagId
            } else {
                _filterState.value.selectedTagIds + tagId
            }
        )
    }

    fun clearFilters() {
        _filterState.value = FilterState()
    }

    fun hasActiveFilters(): Boolean {
        val filter = _filterState.value
        return filter.selectedMoods.isNotEmpty() ||
            filter.selectedWeathers.isNotEmpty() ||
            filter.selectedTagIds.isNotEmpty()
    }
}
