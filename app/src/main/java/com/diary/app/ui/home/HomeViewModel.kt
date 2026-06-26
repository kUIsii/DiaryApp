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
import com.diary.app.data.TagDao
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
import com.diary.app.util.computeLongestStreak
import com.diary.app.util.detectStreakMilestone
import com.diary.app.util.streakTier
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val tagDao = (application as DiaryApplication).database.tagDao()

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

    // Search history (persisted, last 10)
    private val searchPrefs = application.getSharedPreferences("search_prefs", android.content.Context.MODE_PRIVATE)
    private val _recentSearches = MutableStateFlow<List<String>>(loadSearchHistory())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    // Advanced search filters
    private val _filterMoodLevels = MutableStateFlow<Set<Int>>(emptySet())
    val filterMoodLevels: StateFlow<Set<Int>> = _filterMoodLevels
    private val _filterWeatherTypes = MutableStateFlow<Set<String>>(emptySet())
    val filterWeatherTypes: StateFlow<Set<String>> = _filterWeatherTypes
    private val _filterFavoritesOnly = MutableStateFlow(false)
    val filterFavoritesOnly: StateFlow<Boolean> = _filterFavoritesOnly
    private val _filterDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val filterDateRange: StateFlow<Pair<Long, Long>?> = _filterDateRange
    private val _showFilters = MutableStateFlow(false)
    val showFilters: StateFlow<Boolean> = _showFilters

    // Search suggestions (tag names from TagDao)
    val searchSuggestions: StateFlow<List<String>> = tagDao.getAllTags()
        .map { tags -> tags.map { it.name }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Phase 4b: Greeting + Writing Prompt + Enhanced Streak Info
    private val _homeNewState = MutableStateFlow(HomeNewState())
    val homeNewState: StateFlow<HomeNewState> = _homeNewState
    private val _writingPrompt = MutableStateFlow(WRITING_PROMPTS.random())

    fun shuffleWritingPrompt() {
        _writingPrompt.value = WRITING_PROMPTS.random()
        _homeNewState.value = _homeNewState.value.copy(writingPrompt = _writingPrompt.value)
    }

    init {
        viewModelScope.launch {
            combine(entryDates, stats) { dates, s ->
                HomeNewState(
                    greeting = HomeGreeting.now(),
                    writingPrompt = _writingPrompt.value,
                    streakInfo = HomeStreakInfo(
                        current = s.streak,
                        longest = computeLongestStreak(dates).first,
                        longestRange = computeLongestStreak(dates).second,
                        availableFreezes = 0,
                        milestone = detectStreakMilestone(s.streak),
                        tier = streakTier(s.streak),
                        monthlyBest = s.streak,
                        yearlyBest = s.streak
                    )
                )
            }.collect { _homeNewState.value = it }
        }
    }

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Search results across all dates (for homepage search bar) with advanced filters
    val searchResults: StateFlow<List<DiaryPreview>> = combine(
        debouncedSearchQuery, _filterMoodLevels, _filterWeatherTypes, _filterFavoritesOnly, _filterDateRange
    ) { query, moods, weather, favs, dates ->
        SearchParams(query, moods, weather, favs, dates)
    }.flatMapLatest { params ->
        if (params.query.isBlank() && params.moods.isEmpty() && params.weather.isEmpty() && !params.favorites && params.dates == null) {
            flowOf(emptyList())
        } else if (params.query.isBlank()) {
            // Filter-only mode: use all entries and filter in memory
            allEntries.map { entries ->
                entries.filter { entry ->
                    matchFilters(entry, params)
                }
            }
        } else {
            dao.searchPreviews(params.query).map { results ->
                results.filter { entry -> matchFilters(entry, params) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun matchFilters(entry: DiaryPreview, params: SearchParams): Boolean {
        if (params.moods.isNotEmpty() && entry.moodLevel !in params.moods) return false
        if (params.weather.isNotEmpty() && entry.weather !in params.weather) return false
        if (params.favorites && !entry.isFavorite) return false
        if (params.dates != null) {
            val (start, end) = params.dates
            if (entry.createdAt < start || entry.createdAt >= end) return false
        }
        return true
    }

    private data class SearchParams(
        val query: String,
        val moods: Set<Int>,
        val weather: Set<String>,
        val favorites: Boolean,
        val dates: Pair<Long, Long>?
    )

    fun commitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        if (current.size > 10) current.removeAt(current.lastIndex)
        _recentSearches.value = current
        saveSearchHistory(current)
    }

    fun clearSearchHistory() {
        _recentSearches.value = emptyList()
        searchPrefs.edit().remove("search_history").apply()
    }

    private fun loadSearchHistory(): List<String> {
        val json = searchPrefs.getString("search_history", null) ?: return emptyList()
        return try {
            org.json.JSONArray(json).let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveSearchHistory(history: List<String>) {
        val arr = org.json.JSONArray()
        history.forEach { arr.put(it) }
        searchPrefs.edit().putString("search_history", arr.toString()).apply()
    }

    // ── Advanced filter methods ──

    fun toggleFilterMood(level: Int) {
        val current = _filterMoodLevels.value.toMutableSet()
        if (level in current) current.remove(level) else current.add(level)
        _filterMoodLevels.value = current
    }

    fun toggleFilterWeather(weather: String) {
        val current = _filterWeatherTypes.value.toMutableSet()
        if (weather in current) current.remove(weather) else current.add(weather)
        _filterWeatherTypes.value = current
    }

    fun toggleFilterFavorites() {
        _filterFavoritesOnly.value = !_filterFavoritesOnly.value
    }

    fun setFilterDateRange(start: Long?, end: Long?) {
        _filterDateRange.value = if (start != null && end != null) Pair(start, end) else null
    }

    fun toggleShowFilters() {
        _showFilters.value = !_showFilters.value
    }

    fun clearAllFilters() {
        _filterMoodLevels.value = emptySet()
        _filterWeatherTypes.value = emptySet()
        _filterFavoritesOnly.value = false
        _filterDateRange.value = null
    }

    val hasActiveFilters: StateFlow<Boolean> = combine(
        _filterMoodLevels, _filterWeatherTypes, _filterFavoritesOnly, _filterDateRange
    ) { moods, weather, favs, dates ->
        moods.isNotEmpty() || weather.isNotEmpty() || favs || dates != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

// ═══ Phase 4b: Writing prompt, greeting, streak enhancement ═══

data class HomeGreeting(val text: String, val emoji: String) {
    companion object {
        fun now(): HomeGreeting {
            val hour = java.time.LocalTime.now().hour
            return when {
                hour < 6 -> HomeGreeting("夜深了", "\uD83C\uDF19")
                hour < 9 -> HomeGreeting("早上好", "\uD83C\uDF05")
                hour < 12 -> HomeGreeting("上午好", "\u2600\uFE0F")
                hour < 14 -> HomeGreeting("中午好", "\uD83C\uDF1E")
                hour < 18 -> HomeGreeting("下午好", "\uD83C\uDF24\uFE0F")
                hour < 22 -> HomeGreeting("晚上好", "\uD83C\uDF19")
                else -> HomeGreeting("夜深了", "\uD83C\uDF03")
            }
        }
    }
}

data class HomeStreakInfo(
    val current: Int,
    val longest: Int,
    val longestRange: Pair<java.time.LocalDate, java.time.LocalDate>?,
    val availableFreezes: Int,
    val milestone: Int?,
    val tier: com.diary.app.util.StreakTier,
    val monthlyBest: Int,
    val yearlyBest: Int
)

val WRITING_PROMPTS = listOf(
    "\u4ECA\u5929\u4F60\u6700\u611F\u6FC0\u52A8\u7684\u4E00\u4EF6\u4E8B\u662F\u4EC0\u4E48\uFF1F",
    "\u5982\u679C\u660E\u5929\u662F\u4F60\u7684\u6700\u540E\u4E00\u5929\uFF0C\u4F60\u60F3\u505A\u4EC0\u4E48\uFF1F",
    "\u5199\u4E00\u5199\u4F60\u6700\u8FD1\u5728\u5B66\u4E60\u7684\u4E00\u4EF6\u4E8B\u3002",
    "\u4ECA\u5929\u4F60\u7B11\u5F97\u6700\u5F00\u5FC3\u7684\u65F6\u523B\u662F\u4EC0\u4E48\uFF1F",
    "\u5199\u4E00\u5199\u4F60\u5FC3\u91CC\u6700\u60F3\u611F\u8C22\u7684\u4E00\u4E2A\u4EBA\u3002",
    "\u4ECA\u5929\u7684\u5929\u6C14\uFF0C\u8BA9\u4F60\u60F3\u5230\u4E86\u4EC0\u4E48\uFF1F",
    "\u5982\u679C\u4F60\u53EF\u4EE5\u56DE\u5230\u8FC7\u53BB\u7684\u4E00\u5929\uFF0C\u4F60\u60F3\u56DE\u5230\u54EA\u4E00\u5929\uFF1F",
    "\u5199\u4E00\u5199\u4F60\u4ECA\u5929\u7684\u4E00\u4E2A\u5C0F\u53D1\u73B0\u3002",
    "\u4F60\u6700\u8FD1\u5728\u770B\u4EC0\u4E48\u4E66/\u7535\u5F71/\u5267\uFF1F\u5199\u4E00\u5199\u611F\u53D7\u3002",
    "\u4ECA\u5929\u4F60\u505A\u4E86\u4EC0\u4E48\u8FD0\u52A8\uFF1F\u8BB0\u5F55\u4E00\u4E0B\u8EAB\u4F53\u7684\u611F\u53D7\u3002",
    "\u4ECA\u5929\u7684\u996D\u83DC\u91CC\uFF0C\u6700\u8BA9\u4F60\u6EE1\u610F\u7684\u662F\u54EA\u4E00\u9053\uFF1F",
    "\u63CF\u8FF0\u4E00\u4E2A\u4F60\u68A6\u60F3\u53BB\u7684\u5730\u65B9\u3002",
    "\u4ECA\u5929\u6709\u4EC0\u4E48\u4E8B\u8BA9\u4F60\u611F\u5230\u5E73\u9759\uFF1F",
    "\u5199\u4E00\u5199\u4F60\u7684\u4E00\u4E2A\u5C0F\u5C0F\u7684\u613F\u671B\u3002",
    "\u4ECA\u5929\u4F60\u5BF9\u8C01\u8BF4\u4E86\u8C01\u4E5F\u6CA1\u8BF4\u7684\u8BDD\uFF1F",
    "\u5199\u4E00\u5199\u4F60\u7684\u4E00\u4E2A\u5C0F\u76EE\u6807\uFF0C\u548C\u5982\u4F55\u5B9E\u73B0\u5B83\u3002",
    "\u4F60\u7684\u684C\u9762/\u624B\u673A\u58C1\u7EB8\u662F\u4EC0\u4E48\uFF1F\u4E3A\u4EC0\u4E48\u9009\u8FD9\u5F20\uFF1F",
    "\u4ECA\u5929\u6709\u4EC0\u4E48\u4E8B\u8BA9\u4F60\u611F\u5230\u5CF7\u5F02\uFF1F",
    "\u5199\u4E00\u5199\u4F60\u6700\u8FD1\u5728\u505A\u7684\u4E00\u4E2A\u6539\u53D8\u3002",
    "\u4ECA\u5929\u7684\u4E00\u4E2A\u5C0F\u7EC6\u8282\uFF0C\u5C06\u6765\u53EF\u80FD\u4F1A\u5FD8\u8BB0\u3002",
    "\u5982\u679C\u4F60\u80FD\u4E0E\u53E4\u4EBA\u5BF9\u8BDD\uFF0C\u4F60\u60F3\u95EE\u4EC0\u4E48\uFF1F",
    "\u4ECA\u5929\u7684\u4E8B\u60C5\uFF0C\u4E09\u5E74\u540E\u7684\u4F60\u4F1A\u600E\u4E48\u770B\uFF1F",
    "\u5199\u4E00\u5199\u4F60\u6700\u8FD1\u7684\u4E00\u4E2A\u68B3\u7406\uFF08\u5DE5\u4F5C/\u751F\u6D3B/\u601D\u60F3\uFF09\u3002",
    "\u63CF\u8FF0\u4F60\u7406\u60F3\u4E2D\u7684\u4E00\u5929\u3002",
    "\u4ECA\u5929\u6700\u5F00\u5FC3\u7684\u4E00\u53E5\u8BDD\u662F\u4EC0\u4E48\uFF1F"
)

data class HomeNewState(
    val greeting: HomeGreeting = HomeGreeting.now(),
    val writingPrompt: String = WRITING_PROMPTS.random(),
    val streakInfo: HomeStreakInfo = HomeStreakInfo(0, 0, null, 0, null, com.diary.app.util.StreakTier.NONE, 0, 0)
)

private fun loadStreakInfo(
    dao: com.diary.app.data.DiaryDao,
    dates: Set<java.time.LocalDate>,
    timestamps: List<Long>
): HomeStreakInfo {
    val current = com.diary.app.util.computeStreak(dates)
    val (longest, range) = com.diary.app.util.computeLongestStreak(dates)
    val milestone = com.diary.app.util.detectStreakMilestone(current)
    val tier = com.diary.app.util.streakTier(current)
    val monthlyBest = com.diary.app.util.computeYearlyBestStreak(timestamps, java.time.LocalDate.now().year)
    return HomeStreakInfo(current, longest, range, 0, milestone, tier, monthlyBest, 0)
}
