package com.diary.app.ui.home

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TagInfo(val id: Long, val name: String, val color: Color)

data class HomeStats(val total: Int, val streak: Int, val thisMonth: Int)

data class DayInfo(val moodLevel: Int?, val weather: String?)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTagFilter = MutableStateFlow<Long?>(null)
    val selectedTagFilter: StateFlow<Long?> = _selectedTagFilter

    enum class SortOrder(val label: String) {
        NEWEST("最新优先"),
        OLDEST("最早优先"),
        BEST_MOOD("心情最好"),
        FAVORITES("收藏优先")
    }

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    val tagsMap: StateFlow<Map<Long, List<TagInfo>>> = dao.getAllDiaryTagPairs()
        .map { pairs ->
            pairs.groupBy { it.diaryId }.mapValues { (_, tagPairs) ->
                tagPairs.map { TagInfo(it.tagId, it.name, Color(it.color)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allTags: StateFlow<List<com.diary.app.data.Tag>> = dao.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entryDates: StateFlow<Set<LocalDate>> = dao.getAllTimestamps()
        .map { timestamps ->
            timestamps.map { ts ->
                Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
            }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val allEntries: StateFlow<List<DiaryEntry>> = dao.getAllEntries()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayInfoMap: StateFlow<Map<LocalDate, DayInfo>> = allEntries
        .map { entries ->
            entries
                .sortedByDescending { it.createdAt }
                .groupBy {
                    Instant.ofEpochMilli(it.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                .mapValues { (_, dayEntries) ->
                    val entry = dayEntries.first()
                    DayInfo(entry.moodLevel, entry.weather)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val stats: StateFlow<HomeStats> = combine(allEntries, entryDates) { entries, dates ->
        val now = LocalDate.now()
        val streak = computeStreak(dates)
        val thisMonth = entries.count { entry ->
            val entryDate = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            entryDate.monthValue == now.monthValue && entryDate.year == now.year
        }
        HomeStats(entries.size, streak, thisMonth)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeStats(0, 0, 0))

    private val filteredEntries: StateFlow<List<DiaryEntry>> = combine(
        allEntries,
        _selectedDate,
        _searchQuery,
        _selectedTagFilter,
        tagsMap
    ) { entries, date, query, tagFilter, tags ->
        entries.filter { entry ->
            val matchesDate = date == null || run {
                val entryDate = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                entryDate == date
            }
            val matchesQuery = query.isBlank() || entry.plainText.contains(query, ignoreCase = true)
            val matchesTag = tagFilter == null || (tags[entry.id]?.any { it.id == tagFilter } == true)
            matchesDate && matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entries: StateFlow<List<DiaryEntry>> = combine(filteredEntries, _sortOrder) { filtered, sort ->
        sortEntries(filtered, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTagFilter(tagId: Long?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tagId) null else tagId
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(entry: DiaryEntry) {
        viewModelScope.launch {
            dao.toggleFavorite(entry.id, !entry.isFavorite)
        }
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

    fun loadTagsForEntries(@Suppress("UNUSED_PARAMETER") entries: List<DiaryEntry>) {
        // Tags are now loaded reactively via getAllDiaryTagPairs() Flow.
        // This method is kept for call-site compatibility but is a no-op.
    }

    private fun computeStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        var streak = 0
        var current = LocalDate.now()
        while (current in dates) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    private fun sortEntries(entries: List<DiaryEntry>, sort: SortOrder): List<DiaryEntry> {
        return when (sort) {
            SortOrder.NEWEST -> entries.sortedByDescending { it.createdAt }
            SortOrder.OLDEST -> entries.sortedBy { it.createdAt }
            SortOrder.BEST_MOOD -> entries.sortedByDescending { it.moodLevel ?: 0 }
            SortOrder.FAVORITES -> entries.sortedWith(
                compareByDescending<DiaryEntry> { it.isFavorite }.thenByDescending { it.createdAt }
            )
        }
    }
}
