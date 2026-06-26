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
import com.diary.app.ui.profile.expandTagFilterNames
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
import com.diary.app.ui.stats.GoalProgress
import com.diary.app.ui.stats.computeGoalProgress
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
    private val _filterTagNames = MutableStateFlow<Set<String>>(emptySet())
    val filterTagNames: StateFlow<Set<String>> = _filterTagNames
    private val _filterLocationQuery = MutableStateFlow<String?>(null)
    val filterLocationQuery: StateFlow<String?> = _filterLocationQuery
    private val _filterWordCountRange = MutableStateFlow<SearchWordCountRange?>(null)
    val filterWordCountRange: StateFlow<SearchWordCountRange?> = _filterWordCountRange
    private val _showFilters = MutableStateFlow(false)
    val showFilters: StateFlow<Boolean> = _showFilters

    // Search suggestions (tag names from TagDao)
    val searchSuggestions: StateFlow<List<String>> = tagDao.getAllTags()
        .map { tags -> tags.map { it.name }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allEntries: StateFlow<List<DiaryPreview>> = dao.getAllPreviews()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationSuggestions: StateFlow<List<String>> = allEntries
        .map { entries ->
            entries.mapNotNull { entry -> entry.location?.trim()?.takeIf(String::isNotBlank) }
                .distinct()
                .sorted()
        }
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

    val goalProgress: StateFlow<List<GoalProgress>> = allEntries
        .map { entries ->
            val zone = ZoneId.systemDefault()
            val now = LocalDate.now()
            val goals = try { dao.getActiveGoalsOnce() } catch (_: Exception) { emptyList() }
            computeGoalProgress(goals, entries, zone, now)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phase 4b: Greeting + Writing Prompt + Enhanced Streak Info
    private val _homeNewState = MutableStateFlow(HomeNewState())
    val homeNewState: StateFlow<HomeNewState> = _homeNewState
    private val _writingPrompt = MutableStateFlow(WRITING_PROMPT_STRINGS.random())

    fun shuffleWritingPrompt() {
        _writingPrompt.value = WRITING_PROMPT_STRINGS.random()
        _homeNewState.value = _homeNewState.value.copy(writingPrompt = _writingPrompt.value)
    }

    init {
        viewModelScope.launch {
            combine(entryDates, stats) { dates, s ->
                val weatherText = _currentWeather.value?.let { w ->
                    when {
                        w.weather.contains("雨") -> "外面在下雨"
                        w.weather.contains("雪") -> "外面在下雪"
                        w.weather.contains("晴") -> "今天阳光不错"
                        w.weather.contains("阴") -> "今天天色阴沉"
                        w.weather.contains("风") -> "外面风挺大"
                        w.weather.contains("雾") -> "外面有雾"
                        else -> null
                    }
                }
                val streakText = if (s.streak >= 3) "你已经连续写了${s.streak}天" else null
                HomeNewState(
                    greeting = HomeGreeting.smart(weatherText, streakText),
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

    private val activeSearchFilters: StateFlow<HomeSearchParams> = combine(
        combine(
            _filterMoodLevels,
            _filterWeatherTypes,
            _filterFavoritesOnly,
            _filterDateRange
        ) { moods, weather, favorites, dates ->
            HomeSearchParams(
                query = "",
                moods = moods,
                weather = weather,
                favorites = favorites,
                dates = dates
            )
        },
        combine(
            _filterTagNames,
            _filterLocationQuery,
            _filterWordCountRange,
            allTags
        ) { tagNames, locationQuery, wordCountRange, allTagsList ->
            HomeSearchParams(
                query = "",
                moods = emptySet(),
                weather = emptySet(),
                favorites = false,
                dates = null,
                tagNames = expandTagFilterNames(tagNames, allTagsList),
                locationQuery = locationQuery,
                wordCountRange = wordCountRange
            )
        }
    ) { base, extras ->
        base.copy(
            tagNames = extras.tagNames,
            locationQuery = extras.locationQuery,
            wordCountRange = extras.wordCountRange
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeSearchParams(
            query = "",
            moods = emptySet(),
            weather = emptySet(),
            favorites = false,
            dates = null
        )
    )

    // Search results across all dates (for homepage search bar) with advanced filters
    val searchResults: StateFlow<List<DiaryPreview>> = combine(
        debouncedSearchQuery,
        activeSearchFilters,
        tagsMap
    ) { query, activeFilters, entryTags ->
        Pair(
            activeFilters.copy(query = query),
            entryTags.mapValues { (_, tags) -> tags.map { it.name }.toSet() }
        )
    }.flatMapLatest { params ->
        val searchParams = params.first
        val entryTags = params.second
        if (searchParams.query.isBlank() &&
            searchParams.moods.isEmpty() &&
            searchParams.weather.isEmpty() &&
            !searchParams.favorites &&
            searchParams.dates == null &&
            searchParams.tagNames.isEmpty() &&
            searchParams.locationQuery.isNullOrBlank() &&
            searchParams.wordCountRange == null
        ) {
            flowOf(emptyList())
        } else if (searchParams.query.isBlank()) {
            // Filter-only mode: use all entries and filter in memory
            allEntries.map { entries ->
                entries.filter { entry ->
                    matchesHomeSearchFilters(entry, searchParams, entryTags[entry.id].orEmpty())
                }
            }
        } else {
            dao.searchPreviews(searchParams.query).map { results ->
                results.filter { entry ->
                    matchesHomeSearchFilters(entry, searchParams, entryTags[entry.id].orEmpty())
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun toggleFilterTag(tagName: String) {
        val current = _filterTagNames.value.toMutableSet()
        if (tagName in current) current.remove(tagName) else current.add(tagName)
        _filterTagNames.value = current
    }

    fun setFilterLocation(location: String?) {
        _filterLocationQuery.value = location?.trim()?.takeIf(String::isNotBlank)
    }

    fun setFilterWordCountRange(range: SearchWordCountRange?) {
        _filterWordCountRange.value = if (_filterWordCountRange.value == range) null else range
    }

    fun toggleShowFilters() {
        _showFilters.value = !_showFilters.value
    }

    fun clearAllFilters() {
        _filterMoodLevels.value = emptySet()
        _filterWeatherTypes.value = emptySet()
        _filterFavoritesOnly.value = false
        _filterDateRange.value = null
        _filterTagNames.value = emptySet()
        _filterLocationQuery.value = null
        _filterWordCountRange.value = null
    }

    // AI-powered smart search: parse natural language query into filters
    private val _smartSearchParsing = MutableStateFlow(false)
    val smartSearchParsing: StateFlow<Boolean> = _smartSearchParsing
    private val _smartSearchDescription = MutableStateFlow<String?>(null)
    val smartSearchDescription: StateFlow<String?> = _smartSearchDescription

    fun parseSmartSearch(query: String) {
        if (query.length < 4 || _smartSearchParsing.value) return
        _smartSearchParsing.value = true
        viewModelScope.launch {
            try {
                val app = getApplication<DiaryApplication>()
                val result = app.aiService.parseSearchQuery(query)
                val moods = mutableSetOf<Int>()
                val weathers = mutableSetOf<String>()
                var favorites = false
                var dateStart: Long? = null
                var dateEnd: Long? = null
                val descriptions = mutableListOf<String>()
                val parsedQuery = resolveSmartSearchQuery(query, result["keywords"].orEmpty())

                result["mood"]?.let { moodStr ->
                    val level = parseSmartSearchMoodLevel(moodStr)
                    if (level != null) {
                        moods.add(level)
                        val moodName = when (level) {
                            1 -> "很低落"
                            2 -> "低落"
                            3 -> "平静"
                            4 -> "开心"
                            5 -> "非常开心"
                            else -> "心情等级$level"
                        }
                        descriptions.add("心情: $moodName")
                    }
                }
                result["weather"]?.let { w ->
                    if (w.isNotBlank()) {
                        weathers.add(w)
                        descriptions.add("天气: $w")
                    }
                }
                result["favorite"]?.let { f ->
                    if (f == "true") {
                        favorites = true
                        descriptions.add("仅收藏")
                    }
                }
                result["dateStart"]?.let { start ->
                    result["dateEnd"]?.let { end ->
                        dateStart = start.toLongOrNull()
                        dateEnd = end.toLongOrNull()
                        if (dateStart != null && dateEnd != null) {
                            descriptions.add("时间范围已筛选")
                        }
                    }
                }

                if (moods.isNotEmpty() || weathers.isNotEmpty() || favorites || dateStart != null) {
                    _searchQuery.value = parsedQuery
                    commitSearch(parsedQuery)
                    _filterMoodLevels.value = moods
                    _filterWeatherTypes.value = weathers
                    _filterFavoritesOnly.value = favorites
                    _filterDateRange.value = if (dateStart != null && dateEnd != null) Pair(dateStart!!, dateEnd!!) else null
                    _smartSearchDescription.value = descriptions.joinToString(", ")
                } else {
                    _smartSearchDescription.value = null
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeViewModel", "Smart search parse failed", e)
                _smartSearchDescription.value = null
            } finally {
                _smartSearchParsing.value = false
            }
        }
    }

    fun clearSmartSearchDescription() {
        _smartSearchDescription.value = null
    }

    val hasActiveFilters: StateFlow<Boolean> = activeSearchFilters.map { filters ->
        filters.moods.isNotEmpty() ||
            filters.weather.isNotEmpty() ||
            filters.favorites ||
            filters.dates != null ||
            filters.tagNames.isNotEmpty() ||
            !filters.locationQuery.isNullOrBlank() ||
            filters.wordCountRange != null
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

data class HomeGreeting(val text: String) {
    companion object {
        fun now(): HomeGreeting {
            val hour = java.time.LocalTime.now().hour
            return when {
                hour < 6 -> HomeGreeting("夜深了")
                hour < 9 -> HomeGreeting("早上好")
                hour < 12 -> HomeGreeting("上午好")
                hour < 14 -> HomeGreeting("中午好")
                hour < 18 -> HomeGreeting("下午好")
                hour < 22 -> HomeGreeting("晚上好")
                else -> HomeGreeting("夜深了")
            }
        }

        fun smart(weatherHint: String?, streakHint: String?): HomeGreeting {
            val base = now()
            val parts = mutableListOf<String>()
            if (weatherHint != null) parts.add(weatherHint)
            if (streakHint != null) parts.add(streakHint)
            val suffix = if (parts.isNotEmpty()) "，${parts.joinToString("，")}" else ""
            return HomeGreeting(base.text + suffix)
        }
    }
}

internal fun parseSmartSearchMoodLevel(raw: String): Int? {
    val level = raw.toIntOrNull() ?: return null
    return level.takeIf { it in 1..5 }
}

internal fun resolveSmartSearchQuery(originalQuery: String, parsedKeywords: String): String {
    val normalized = parsedKeywords.trim()
    return normalized.ifBlank { originalQuery }
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

// Prompt categories for smarter selection
enum class PromptType { REVIEW, GUIDE, SEASON, MOOD, CREATIVE, REFLECT }

data class WritingPrompt(val text: String, val type: PromptType)

val WRITING_PROMPTS = listOf(
    // 回顾型
    WritingPrompt("今天你最感动的一件事是什么？", PromptType.REVIEW),
    WritingPrompt("今天你笑得最开心的时刻是什么？", PromptType.REVIEW),
    WritingPrompt("写一写你今天的一个小发现。", PromptType.REVIEW),
    WritingPrompt("今天的饭菜里，最让你满意的是哪一道？", PromptType.REVIEW),
    WritingPrompt("今天有什么事让你感到平静？", PromptType.REVIEW),
    WritingPrompt("今天最开心的一句话是什么？", PromptType.REVIEW),
    WritingPrompt("今天的一个小细节，将来可能会忘记。", PromptType.REVIEW),
    WritingPrompt("今天你对谁说了谁也没说的话？", PromptType.REVIEW),
    WritingPrompt("今天有什么事让你感到惊喜？", PromptType.REVIEW),
    WritingPrompt("记录一下今天的天气和你的心情。", PromptType.REVIEW),
    // 引导型
    WritingPrompt("如果明天是你的最后一天，你想做什么？", PromptType.GUIDE),
    WritingPrompt("写一写你最近在学习的一件事。", PromptType.GUIDE),
    WritingPrompt("写一写你心里最想感谢的一个人。", PromptType.GUIDE),
    WritingPrompt("描述一个你梦想去的地方。", PromptType.GUIDE),
    WritingPrompt("写一写你的一个小目标，和如何实现它。", PromptType.GUIDE),
    WritingPrompt("你的桌面/手机壁纸是什么？为什么选这张？", PromptType.GUIDE),
    WritingPrompt("写一写你最近在做的一个改变。", PromptType.GUIDE),
    WritingPrompt("描述你理想中的一天。", PromptType.GUIDE),
    WritingPrompt("如果你能与古人对话，你想问什么？", PromptType.GUIDE),
    WritingPrompt("写一写你最近的一个梳理（工作/生活/思想）。", PromptType.GUIDE),
    // 季节型
    WritingPrompt("今天的天气，让你想到了什么？", PromptType.SEASON),
    WritingPrompt("窗外的风景有什么变化？", PromptType.SEASON),
    WritingPrompt("这个季节让你想起了什么回忆？", PromptType.SEASON),
    WritingPrompt("今天的空气闻起来像什么？", PromptType.SEASON),
    // 情绪型
    WritingPrompt("如果你的心情是一种颜色，今天是什么颜色？", PromptType.MOOD),
    WritingPrompt("今天有让你烦恼的事吗？写下来会好一些。", PromptType.MOOD),
    WritingPrompt("你现在最想做的一件事是什么？", PromptType.MOOD),
    WritingPrompt("今天什么事情让你感到满足？", PromptType.MOOD),
    WritingPrompt("写一写你最近的一个小小愿望。", PromptType.MOOD),
    // 创意型
    WritingPrompt("用三句话描述今天的自己。", PromptType.CREATIVE),
    WritingPrompt("给五年后的自己写一句话。", PromptType.CREATIVE),
    WritingPrompt("如果你是一本书，今天是哪一章？", PromptType.CREATIVE),
    WritingPrompt("今天的声音里，最特别的是什么？", PromptType.CREATIVE),
    WritingPrompt("用一个词概括今天。", PromptType.CREATIVE),
    // 反思型
    WritingPrompt("今天的事情，三年后的你会怎么看？", PromptType.REFLECT),
    WritingPrompt("如果回到过去的一天，你想回到哪一天？", PromptType.REFLECT),
    WritingPrompt("你最近在看什么书/电影/剧？写一写感受。", PromptType.REFLECT),
    WritingPrompt("今天你做了什么运动？记录一下身体的感受。", PromptType.REFLECT),
    WritingPrompt("描述一个最近让你感到骄傲的小事。", PromptType.REFLECT),
    WritingPrompt("写一写你最近的一个思考。", PromptType.REFLECT),
    WritingPrompt("今天有谁让你感到温暖？", PromptType.REFLECT),
    WritingPrompt("如果你能改变今天的一件事，会是什么？", PromptType.REFLECT),
    WritingPrompt("写一写你最近学到的一个道理。", PromptType.REFLECT),
    // 额外补充
    WritingPrompt("今天你闻到了什么好闻的味道？", PromptType.REVIEW),
    WritingPrompt("最近有没有什么歌一直循环播放？", PromptType.CREATIVE),
    WritingPrompt("今天你帮助了谁？或者谁帮助了你？", PromptType.REVIEW),
    WritingPrompt("如果今天是一种味道，会是什么？", PromptType.CREATIVE),
    WritingPrompt("写一写你最近的一个小习惯。", PromptType.REFLECT),
    WritingPrompt("今天你在手机上花了多少时间？值得吗？", PromptType.REFLECT),
    WritingPrompt("描述一下你现在坐着的地方。", PromptType.REVIEW),
    WritingPrompt("如果可以 teleport，你现在想去哪里？", PromptType.CREATIVE)
)

// Backward-compatible string list
val WRITING_PROMPT_STRINGS = WRITING_PROMPTS.map { it.text }

data class HomeNewState(
    val greeting: HomeGreeting = HomeGreeting.now(),
    val writingPrompt: String = WRITING_PROMPT_STRINGS.random(),
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
