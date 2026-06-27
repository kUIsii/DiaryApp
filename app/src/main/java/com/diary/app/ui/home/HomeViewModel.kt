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
import com.diary.app.util.computeStreak
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

data class HomeHighlightsState(
    val randomEntry: DiaryPreview? = null,
    val onThisDayEntries: List<DiaryPreview> = emptyList()
)

internal fun refreshedHomeHighlightsState(
    previous: HomeHighlightsState,
    randomEntry: DiaryPreview?,
    onThisDayEntries: List<DiaryPreview>
): HomeHighlightsState {
    return previous.copy(
        randomEntry = randomEntry,
        onThisDayEntries = onThisDayEntries
    )
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

    private val _isWeatherEnabled = MutableStateFlow(false)
    val isWeatherEnabled: StateFlow<Boolean> = _isWeatherEnabled

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

    private val _highlightRefreshNonce = MutableStateFlow(System.currentTimeMillis())

    private val allEntries: StateFlow<List<DiaryPreview>> = dao.getAllPreviews()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeHighlights: StateFlow<HomeHighlightsState> = combine(allEntries, _highlightRefreshNonce) { entries, nonce ->
        val today = LocalDate.now()
        val onThisDayEntries = entries.filter { entry ->
            val entryDate = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            entryDate.monthValue == today.monthValue &&
                entryDate.dayOfMonth == today.dayOfMonth &&
                entryDate.year < today.year
        }
        val randomEntry = if (entries.isEmpty()) {
            null
        } else {
            entries[java.util.Random(nonce).nextInt(entries.size)]
        }
        refreshedHomeHighlightsState(
            previous = HomeHighlightsState(),
            randomEntry = randomEntry,
            onThisDayEntries = onThisDayEntries
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeHighlightsState())

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

    // Entries grouped by date (for day pager)
    val entriesByDate: StateFlow<Map<LocalDate, List<DiaryPreview>>> = allEntries
        .map { entries ->
            entries.groupBy { entry ->
                Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
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

    fun refreshHomeHighlights() {
        _highlightRefreshNonce.value = System.currentTimeMillis()
    }

    fun setTagFilter(tagId: Long?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tagId) null else tagId
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

    fun enableWeather(onRequestPermission: () -> Unit) {
        val context = getApplication<Application>()
        _isWeatherEnabled.value = true
        if (WeatherManager.hasLocationPermission(context)) {
            loadWeather()
        } else {
            onRequestPermission()
        }
    }

    fun disableWeather() {
        _isWeatherEnabled.value = false
        _currentWeather.value = null
    }

    fun autoLoadWeather() {
        val context = getApplication<Application>()
        if (WeatherManager.hasLocationPermission(context)) {
            _isWeatherEnabled.value = true
            loadWeather()
        }
    }

    fun loadWeatherWithPermissionCheck(onRequestPermission: () -> Unit) {
        val context = getApplication<Application>()
        // First try cache
        viewModelScope.launch {
            val cached = WeatherManager.getCachedWeather(context)
            if (cached != null) {
                _currentWeather.value = cached
            }
        }
        // Check permission and fetch if granted
        if (WeatherManager.hasLocationPermission(context)) {
            loadWeather()
        } else {
            onRequestPermission()
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
