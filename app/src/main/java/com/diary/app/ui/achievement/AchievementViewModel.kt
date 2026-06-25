package com.diary.app.ui.achievement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementRepository
import com.diary.app.data.AchievementStats
import com.diary.app.data.AchievementTier
import com.diary.app.data.CrossSystemManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as DiaryApplication).database
    private val repository = AchievementRepository(db.achievementDao(), db.diaryDao(), db.tagDao(), db.mediaDao())

    val allItems: StateFlow<List<AchievementItem>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<AchievementStats> = repository.getStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            AchievementStats(0, 0, emptyMap()))

    private val _selectedCategory = MutableStateFlow<AchievementCategory?>(null)
    val selectedCategory: StateFlow<AchievementCategory?> = _selectedCategory

    private val _selectedTier = MutableStateFlow<AchievementTier?>(null)
    val selectedTier: StateFlow<AchievementTier?> = _selectedTier

    private val _selectedStateFilter = MutableStateFlow(AchievementGalleryFilter.ALL)
    val selectedStateFilter: StateFlow<AchievementGalleryFilter> = _selectedStateFilter

    private val _selectedAchievement = MutableStateFlow<AchievementItem?>(null)
    val selectedAchievement: StateFlow<AchievementItem?> = _selectedAchievement

    private val _isFilterExpanded = MutableStateFlow(false)
    val isFilterExpanded: StateFlow<Boolean> = _isFilterExpanded

    val galleryState: StateFlow<AchievementGalleryState> = combine(
        allItems,
        stats,
        _selectedCategory,
        _selectedTier,
        _selectedStateFilter
    ) { items, galleryStats, cat, tier, stateFilter ->
        buildAchievementGalleryState(
            items = items,
            stats = galleryStats,
            selectedCategory = cat,
            selectedTier = tier,
            stateFilter = stateFilter
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AchievementGalleryState(
            hero = buildAchievementHeroSummary(
                stats = AchievementStats(0, 0, emptyMap()),
                items = emptyList()
            ),
            recentUnlocks = emptyList(),
            nearCompletion = emptyList(),
            filteredCards = emptyList()
        )
    )

    val filteredItems: StateFlow<List<AchievementItem>> = galleryState
        .map { state -> state.filteredCards.map { it.item } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                repository.initialize()
                repository.checkAndUnlock()
            }
        }
        viewModelScope.launch {
            allItems.collect { items ->
                val latestUnlocked = items
                    .filter { it.isUnlocked }
                    .maxByOrNull { it.state.unlockedAt ?: 0L }
                val nextMilestone = items
                    .filterNot { it.isUnlocked }
                    .maxByOrNull { it.progressFraction }
                CrossSystemManager.updateRecentAchievementUnlock(latestUnlocked?.def?.name)
                CrossSystemManager.updateNextAchievementMilestone(nextMilestone?.def?.name)
            }
        }
    }

    fun selectCategory(category: AchievementCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun selectTier(tier: AchievementTier?) {
        _selectedTier.value = if (_selectedTier.value == tier) null else tier
    }

    fun selectStateFilter(filter: AchievementGalleryFilter) {
        _selectedStateFilter.value = if (_selectedStateFilter.value == filter) {
            AchievementGalleryFilter.ALL
        } else {
            filter
        }
    }

    fun showAchievementDetail(item: AchievementItem) {
        _selectedAchievement.value = item
    }

    fun dismissAchievementDetail() {
        _selectedAchievement.value = null
    }

    fun toggleFilter() {
        _isFilterExpanded.value = !_isFilterExpanded.value
    }

    fun collapseFilter() {
        _isFilterExpanded.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.checkAndUnlock() }
        }
    }

    fun getHiddenAchievementCount(): Int {
        return allItems.value.count { it.def.isHidden && !it.isUnlocked }
    }

    fun getCategoryProgress(category: AchievementCategory): Pair<Int, Int> {
        val items = allItems.value.filter { it.def.category == category }
        return items.count { it.isUnlocked } to items.size
    }

    fun getTierProgress(tier: AchievementTier): Pair<Int, Int> {
        val items = allItems.value.filter { it.def.tier == tier }
        return items.count { it.isUnlocked } to items.size
    }

    fun getActiveFilterCount(): Int {
        var count = 0
        if (_selectedStateFilter.value != AchievementGalleryFilter.ALL) count++
        if (_selectedCategory.value != null) count++
        if (_selectedTier.value != null) count++
        return count
    }
}
