package com.diary.app.ui.home

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiInsight
import com.diary.app.ai.InsightGenerator
import com.diary.app.data.DiaryEntry
import com.diary.app.weather.CurrentWeather
import com.diary.app.weather.WeatherManager
import com.diary.app.data.DiaryPreview
import com.diary.app.data.TrashEntry
import com.diary.app.data.normalizeContentForExport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun toTrashEntry(entry: DiaryEntry): TrashEntry {
    return TrashEntry(
        originalId = entry.id,
        title = entry.title,
        content = normalizeContentForExport(entry.content),
        plainText = entry.plainText,
        moodLevel = entry.moodLevel,
        weather = entry.weather,
        location = entry.location,
        latitude = entry.latitude,
        longitude = entry.longitude,
        isFavorite = entry.isFavorite,
        createdAt = entry.createdAt,
        updatedAt = entry.updatedAt
    )
}

data class TagInfo(val id: Long, val name: String, val color: Color)

data class HomeStats(val total: Int, val streak: Int, val thisMonth: Int)

data class DayInfo(
    val moodLevel: Int?,
    val weather: String?,
    val accentMoodLevel: Int? = null,
    val hasMixedMoods: Boolean = false,
    val entryCount: Int = 0
)

data class SearchFilters(
    val moodLevel: Int? = null,
    val weather: String? = null,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null
) {
    val isActive: Boolean get() = moodLevel != null || weather != null || dateRangeStart != null || dateRangeEnd != null
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val debouncedSearchQuery = _searchQuery.debounce(300L)

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

    // Image map: entryId -> first image localPath (original quality)
    val imageMap: StateFlow<Map<Long, String>> = dao.getAllImagesFlow()
        .map { images ->
            images.groupBy { it.entryId }
                .mapValues { (_, entryImages) ->
                    entryImages.minByOrNull { it.sortOrder }?.localPath ?: ""
                }
                .filter { it.value.isNotBlank() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // All images map: entryId -> list of localPaths (for image selection)
    val allImagesMap: StateFlow<Map<Long, List<String>>> = dao.getAllImagesFlow()
        .map { images ->
            images.groupBy { it.entryId }
                .mapValues { (_, entryImages) ->
                    entryImages.sortedBy { it.sortOrder }.map { it.localPath }
                }
                .filter { it.value.isNotEmpty() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val unreadNotificationCount: StateFlow<Int> = dao.getUnreadNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _aiInsight = MutableStateFlow<AiInsight?>(null)
    val aiInsight: StateFlow<AiInsight?> = _aiInsight

    private val _currentWeather = MutableStateFlow<CurrentWeather?>(null)
    val currentWeather: StateFlow<CurrentWeather?> = _currentWeather

    val entryDates: StateFlow<Set<LocalDate>> = dao.getAllTimestamps()
        .map { timestamps ->
            timestamps.map { ts ->
                Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
            }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Search history (in-memory, last 5)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    private val allEntries: StateFlow<List<DiaryPreview>> = dao.getAllPreviews()
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
                    val latestEntry = dayEntries.first()
                    val moodSummary = buildCalendarMoodSummary(
                        moodLevels = dayEntries.mapNotNull { it.moodLevel },
                        entryCount = dayEntries.size
                    )
                    DayInfo(
                        moodLevel = moodSummary.primaryMoodLevel ?: latestEntry.moodLevel,
                        weather = latestEntry.weather,
                        accentMoodLevel = moodSummary.accentMoodLevel,
                        hasMixedMoods = moodSummary.hasMixedMoods,
                        entryCount = moodSummary.entryCount
                    )
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

    private data class EntryFilterSnapshot(
        val entries: List<DiaryPreview>,
        val date: LocalDate?,
        val query: String,
        val tagFilter: Long?,
        val tags: Map<Long, List<TagInfo>>
    )

    private val filteredEntries: StateFlow<List<DiaryPreview>> = combine(
        allEntries,
        _selectedDate,
        debouncedSearchQuery,
        _selectedTagFilter,
        tagsMap
    ) { entries, date, query, tagFilter, tags ->
        EntryFilterSnapshot(
            entries = entries,
            date = date,
            query = query,
            tagFilter = tagFilter,
            tags = tags
        )
    }.combine(_searchFilters) { snapshot, filters ->
        val entries = snapshot.entries
        val date = snapshot.date
        val query = snapshot.query
        val tagFilter = snapshot.tagFilter
        val tags = snapshot.tags

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

    val entries: StateFlow<List<DiaryPreview>> = combine(filteredEntries, _sortOrder) { filtered, sort ->
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

    // Search results across all dates (for homepage search bar)
    val searchResults: StateFlow<List<DiaryPreview>> = debouncedSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else dao.searchPreviews(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun toggleFavorite(entry: DiaryPreview) {
        viewModelScope.launch {
            dao.toggleFavorite(entry.id, !entry.isFavorite)
        }
    }

    fun favoriteEntries(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            dao.batchSetFavorite(ids.toList(), true)
        }
    }

    fun deleteEntry(entry: DiaryPreview) {
        viewModelScope.launch {
            // Fetch full entry (with content) for trash
            val fullEntry = dao.getEntryByIdSafe(entry.id) ?: return@launch
            val trashEntry = toTrashEntry(fullEntry)
            dao.insertTrashEntry(trashEntry)
            dao.deleteEntryWithTags(fullEntry)
        }
    }

    fun deleteEntryById(id: Long) {
        viewModelScope.launch {
            val entry = dao.getEntryByIdSafe(id)
            if (entry != null) {
                dao.insertTrashEntry(toTrashEntry(entry))
                dao.deleteEntryWithTags(entry)
            }
        }
    }

    fun deleteEntries(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val entries = dao.getEntriesByIdsSafe(ids.toList())
            if (entries.isNotEmpty()) {
                val trashEntries = entries.map { toTrashEntry(it) }
                dao.insertTrashEntries(trashEntries)
                dao.deleteEntriesWithTags(entries)
            }
        }
    }

    suspend fun getRandomEntryId(): Long? = dao.getRandomEntryId()

    suspend fun getEntryPreview(id: Long): DiaryPreview? = dao.getPreviewById(id)

    suspend fun getOnThisDayPreviews(): List<DiaryPreview> {
        val today = LocalDate.now()
        return dao.getPreviewsByMonthDay(today.monthValue, today.dayOfMonth)
            .filter { entry ->
                val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                entryDate.year < today.year
            }
    }

    fun loadInsight() {
        val app = getApplication<DiaryApplication>()
        val features = app.experimentalFeatures.value
        if (!features.aiInsightCardEnabled) return
        viewModelScope.launch {
            try {
                _aiInsight.value = InsightGenerator.generate(app, dao, app.aiService)
            } catch (e: Exception) { android.util.Log.e("HomeViewModel", "Failed to load AI insight", e) }
        }
    }

    fun loadWeather() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val cached = WeatherManager.getCachedWeather(context)
                if (cached != null) {
                    _currentWeather.value = cached
                }
                if (WeatherManager.isCacheStale(context)) {
                    val fresh = WeatherManager.fetchWeather(context)
                    if (fresh != null) _currentWeather.value = fresh
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeViewModel", "Failed to load weather", e)
            }
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

    private fun sortEntries(entries: List<DiaryPreview>, sort: SortOrder): List<DiaryPreview> {
        return when (sort) {
            SortOrder.NEWEST -> entries.sortedByDescending { it.createdAt }
            SortOrder.OLDEST -> entries.sortedBy { it.createdAt }
            SortOrder.BEST_MOOD -> entries.sortedByDescending { it.moodLevel ?: 0 }
            SortOrder.FAVORITES -> entries.sortedWith(
                compareByDescending<DiaryPreview> { it.isFavorite }.thenByDescending { it.createdAt }
            )
        }
    }

    // Selected entries for the selected date - uses DB query instead of client-side filtering
    val selectedEntries: StateFlow<List<DiaryPreview>> = _selectedDate
        .flatMapLatest { date ->
            if (date == null) {
                flowOf(emptyList())
            } else {
                val zone = ZoneId.systemDefault()
                val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                dao.getPreviewsByDateRangeFlow(startOfDay, endOfDay)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

}
