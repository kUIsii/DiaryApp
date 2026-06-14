package com.diary.app.ui.home

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryPreview
import com.diary.app.data.TrashEntry
import com.diary.app.data.normalizeContentForExport
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

data class ReviewEntry(val label: String, val entry: DiaryPreview)

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

    val unreadNotificationCount: StateFlow<Int> = dao.getUnreadNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
    val onThisDayEntries: StateFlow<List<DiaryPreview>> = run {
        val now = LocalDate.now()
        dao.getOnThisDayPreviews(now.monthValue, now.dayOfMonth, now.year)
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
            val weekEntries = dao.getPreviewsByDateRange(weekStart, weekEnd)
            weekEntries.firstOrNull()?.let { results.add(ReviewEntry("一周前", it)) }

            // One month ago
            val monthAgo = now.minusMonths(1)
            val monthStart = monthAgo.atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEnd = monthAgo.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEntries = dao.getPreviewsByDateRange(monthStart, monthEnd)
            monthEntries.firstOrNull()?.let { results.add(ReviewEntry("一个月前", it)) }

            // Last year today
            val lastYearEntries = dao.getPreviewsByMonthDay(now.monthValue, now.dayOfMonth)
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
        _searchQuery,
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
            ids.forEach { id ->
                dao.toggleFavorite(id, true)
            }
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
            ids.forEach { id ->
                val entry = dao.getEntryByIdSafe(id) ?: return@forEach
                dao.insertTrashEntry(toTrashEntry(entry))
                dao.deleteEntryWithTags(entry)
            }
        }
    }

    suspend fun getRandomEntryId(): Long? = dao.getRandomEntryId()

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

    // Selected entries for the selected date
    val selectedEntries: StateFlow<List<DiaryPreview>> = combine(
        allEntries,
        _selectedDate
    ) { entries, date ->
        if (date == null) {
            emptyList()
        } else {
            entries.filter { entry ->
                val entryDate = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                entryDate == date
            }.sortedByDescending { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Feature: On This Day ---
    data class OnThisDayItem(val entry: DiaryPreview, val yearsAgo: Int)

    private val _onThisDayItem = MutableStateFlow<OnThisDayItem?>(null)
    val onThisDayItem: StateFlow<OnThisDayItem?> = _onThisDayItem

    private val prefs = application.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)

    init {
        loadOnThisDay()
    }

    private fun loadOnThisDay() {
        val app = getApplication<Application>() as DiaryApplication
        val features = app.experimentalFeatures.value
        if (!features.aiEnabled || !features.aiOnThisDay) return

        viewModelScope.launch {
            val now = LocalDate.now()
            val entries = dao.getPreviewsByMonthDay(now.monthValue, now.dayOfMonth)
            val historical = entries.filter { entry ->
                val entryDate = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                entryDate.year < now.year
            }
            if (historical.isEmpty()) return@launch

            // Find one that hasn't been shown in 7 days
            val shownKey = "on_this_day_shown_ids"
            val shownIds = prefs.getStringSet(shownKey, emptySet())?.toMutableSet() ?: mutableSetOf()
            val candidate = historical.firstOrNull { it.id.toString() !in shownIds }
                ?: historical.first() // If all shown, pick the most recent anyway

            val entryYear = Instant.ofEpochMilli(candidate.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().year
            _onThisDayItem.value = OnThisDayItem(candidate, now.year - entryYear)

            // Record as shown
            shownIds.add(candidate.id.toString())
            prefs.edit().putStringSet(shownKey, shownIds).apply()
        }
    }

    // --- AI Feature: Mood Trend ---
    data class MoodTrendState(val description: String, val visible: Boolean)

    val moodTrendState: StateFlow<MoodTrendState> = dayInfoMap
        .map { infoMap ->
            val app = getApplication<Application>() as DiaryApplication
            val features = app.experimentalFeatures.value
            if (!features.aiEnabled || !features.aiMoodTrend) {
                return@map MoodTrendState("", false)
            }

            val today = LocalDate.now()
            val recentDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val moodValues = recentDays.mapNotNull { infoMap[it]?.moodLevel }

            if (moodValues.size < 3) {
                return@map MoodTrendState("", false)
            }

            val avg = moodValues.average()
            val trend = if (moodValues.size >= 2) {
                val firstHalf = moodValues.take(moodValues.size / 2).average()
                val secondHalf = moodValues.drop(moodValues.size / 2).average()
                when {
                    secondHalf - firstHalf > 0.5 -> "rising"
                    firstHalf - secondHalf > 0.5 -> "falling"
                    else -> "stable"
                }
            } else "stable"

            val description = when {
                avg >= 4.5 && trend == "rising" -> "最近的你，状态不错。"
                avg >= 4.0 && trend == "stable" -> "这些天挺平稳的。"
                avg < 3.0 && trend == "falling" -> "最近有些不容易。"
                avg < 3.0 && trend == "rising" -> "在慢慢好起来。"
                else -> "起起落落，都是生活。"
            }
            MoodTrendState(description, true)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoodTrendState("", false))

    // --- AI Feature: Milestones ---
    data class MilestoneState(val count: Int, val message: String, val visible: Boolean)

    private val _milestone = MutableStateFlow<MilestoneState?>(null)
    val milestone: StateFlow<MilestoneState?> = _milestone

    private val milestoneThresholds = listOf(10, 50, 100, 200, 365, 500, 1000)

    private fun checkMilestones(totalEntries: Int) {
        val app = getApplication<Application>() as DiaryApplication
        val features = app.experimentalFeatures.value
        if (!features.aiEnabled || !features.aiMilestones) return

        val shownKey = "milestones_shown"
        val shownMilestones = prefs.getStringSet(shownKey, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

        val newMilestone = milestoneThresholds.firstOrNull { threshold ->
            totalEntries >= threshold && threshold !in shownMilestones
        }

        if (newMilestone != null) {
            val message = when (newMilestone) {
                10 -> "开始的10篇，每一笔都是种子。"
                50 -> "50篇了。你已经养成了一个习惯。"
                100 -> "第100篇日记。每一篇都值得。"
                200 -> "200篇。文字在替你记住时光。"
                365 -> "365篇。一年的时光，都在这里了。"
                500 -> "500篇。半个千的坚持。"
                1000 -> "1000篇。你的故事，已经是一部书了。"
                else -> "${newMilestone}篇了。"
            }
            _milestone.value = MilestoneState(newMilestone, message, true)

            // Record as shown
            val updated = shownMilestones.toMutableSet()
            updated.add(newMilestone)
            prefs.edit().putStringSet(shownKey, updated.map { it.toString() }.toSet()).apply()
        }
    }

    fun dismissMilestone() {
        _milestone.value = null
    }

    // Check milestones when entries change
    init {
        viewModelScope.launch {
            stats.collect { statsData ->
                checkMilestones(statsData.total)
            }
        }
    }
}
