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

data class ReviewEntry(val label: String, val entry: DiaryEntry)

data class SearchFilters(
    val moodLevel: Int? = null,
    val weather: String? = null,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null
) {
    val isActive: Boolean get() = moodLevel != null || weather != null || dateRangeStart != null || dateRangeEnd != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTagFilter = MutableStateFlow<Long?>(null)
    val selectedTagFilter: StateFlow<Long?> = _selectedTagFilter

    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters

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

    // "On This Day" - entries from the same month+day in previous years
    val onThisDayEntries: StateFlow<List<DiaryEntry>> = run {
        val now = LocalDate.now()
        dao.getOnThisDayEntries(now.monthValue, now.dayOfMonth, now.year)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Review entries: last year today, one week ago, one month ago
    private val _reviewEntries = MutableStateFlow<List<ReviewEntry>>(emptyList())
    val reviewEntries: StateFlow<List<ReviewEntry>> = _reviewEntries

    init {
        loadReviewEntries()
    }

    private fun loadReviewEntries() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val results = mutableListOf<ReviewEntry>()

            // One week ago
            val weekAgo = now.minusWeeks(1)
            val weekStart = weekAgo.atStartOfDay(zone).toInstant().toEpochMilli()
            val weekEnd = weekAgo.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val weekEntries = dao.getEntriesByDateRange(weekStart, weekEnd)
            weekEntries.firstOrNull()?.let { results.add(ReviewEntry("一周前", it)) }

            // One month ago
            val monthAgo = now.minusMonths(1)
            val monthStart = monthAgo.atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEnd = monthAgo.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEntries = dao.getEntriesByDateRange(monthStart, monthEnd)
            monthEntries.firstOrNull()?.let { results.add(ReviewEntry("一个月前", it)) }

            // Last year today
            val lastYearEntries = dao.getEntriesByMonthDay(now.monthValue, now.dayOfMonth)
                .filter { entry ->
                    val entryDate = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                    entryDate.year < now.year
                }
            lastYearEntries.firstOrNull()?.let { results.add(ReviewEntry("去年今日", it)) }

            _reviewEntries.value = results
        }
    }

    fun refreshReview() {
        loadReviewEntries()
    }

    // Search history (in-memory, last 5)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

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

    // Mood trend for the last 7 days
    data class MoodDay(val date: LocalDate, val moodLevel: Int?)

    val moodTrend: StateFlow<List<MoodDay>> = dayInfoMap
        .map { infoMap ->
            val today = LocalDate.now()
            (6 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                MoodDay(date, infoMap[date]?.moodLevel)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly summary
    data class WeeklySummary(
        val entryCount: Int = 0,
        val avgMood: Float? = null,
        val totalWords: Int = 0,
        val daysWithEntries: Int = 0
    )

    val weeklySummary: StateFlow<WeeklySummary> = allEntries
        .map { entries ->
            val today = LocalDate.now()
            val weekStart = today.minusDays(6) // Last 7 days

            val weekEntries = entries.filter { entry ->
                val entryDate = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                !entryDate.isBefore(weekStart) && !entryDate.isAfter(today)
            }

            val moodLevels = weekEntries.mapNotNull { it.moodLevel }
            val avgMood = if (moodLevels.isNotEmpty()) moodLevels.average().toFloat() else null

            WeeklySummary(
                entryCount = weekEntries.size,
                avgMood = avgMood,
                totalWords = weekEntries.sumOf { it.plainText.length },
                daysWithEntries = weekEntries.map {
                    Instant.ofEpochMilli(it.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.distinct().size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklySummary())

    private val filteredEntries: StateFlow<List<DiaryEntry>> = combine(
        allEntries,
        _selectedDate,
        _searchQuery,
        _selectedTagFilter,
        tagsMap,
        _searchFilters
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val entries = args[0] as List<DiaryEntry>
        val date = args[1] as LocalDate?
        val query = args[2] as String
        val tagFilter = args[3] as Long?
        val tags = args[4] as Map<Long, List<TagInfo>>
        val filters = args[5] as SearchFilters

        entries.filter { entry ->
            val matchesDate = date == null || run {
                val entryDate = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                entryDate == date
            }
            val matchesQuery = query.isBlank() || entry.plainText.contains(query, ignoreCase = true)
            val matchesTag = tagFilter == null || (tags[entry.id]?.any { it.id == tagFilter } == true)
            val matchesMood = filters.moodLevel == null || entry.moodLevel == filters.moodLevel
            val matchesWeather = filters.weather == null || entry.weather == filters.weather
            val matchesDateRange = run {
                if (filters.dateRangeStart == null && filters.dateRangeEnd == null) true
                else {
                    val entryDate = Instant.ofEpochMilli(entry.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val afterStart = filters.dateRangeStart == null || !entryDate.isBefore(filters.dateRangeStart)
                    val beforeEnd = filters.dateRangeEnd == null || !entryDate.isAfter(filters.dateRangeEnd)
                    afterStart && beforeEnd
                }
            }
            matchesDate && matchesQuery && matchesTag && matchesMood && matchesWeather && matchesDateRange
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entries: StateFlow<List<DiaryEntry>> = combine(filteredEntries, _sortOrder) { filtered, sort ->
        sortEntries(filtered, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search result count (derived from entries since count is unchanged by sorting)
    val searchResultCount: StateFlow<Int> = entries
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        if (current.size > 5) current.removeAt(current.lastIndex)
        _recentSearches.value = current
    }

    fun clearSearchHistory() {
        _recentSearches.value = emptyList()
    }

    fun setTagFilter(tagId: Long?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tagId) null else tagId
    }

    fun setSearchFilters(filters: SearchFilters) {
        _searchFilters.value = filters
    }

    fun clearSearchFilters() {
        _searchFilters.value = SearchFilters()
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

    fun deleteEntryById(id: Long) {
        viewModelScope.launch {
            dao.deleteEntryById(id)
        }
    }

    private fun computeStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        var streak = 0
        // Start from the most recent date that has a diary entry
        var current = dates.maxOrNull() ?: return 0
        // Only count streak up to today
        val today = LocalDate.now()
        if (current.isAfter(today)) return 0
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
