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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as DiaryApplication).database
    private val repository = AchievementRepository(db.achievementDao(), db.diaryDao())

    val allItems: StateFlow<List<AchievementItem>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<AchievementStats> = repository.getStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            AchievementStats(0, 0, emptyMap()))

    private val _selectedCategory = MutableStateFlow<AchievementCategory?>(null)
    val selectedCategory: StateFlow<AchievementCategory?> = _selectedCategory

    private val _selectedTier = MutableStateFlow<AchievementTier?>(null)
    val selectedTier: StateFlow<AchievementTier?> = _selectedTier

    private val _selectedAchievement = MutableStateFlow<AchievementItem?>(null)
    val selectedAchievement: StateFlow<AchievementItem?> = _selectedAchievement

    val filteredItems: StateFlow<List<AchievementItem>> = combine(
        allItems, _selectedCategory, _selectedTier
    ) { items, cat, tier ->
        items.filter { item ->
            if (item.isHiddenLocked) return@filter false
            val catMatch = cat == null || item.def.category == cat
            val tierMatch = tier == null || item.def.tier == tier
            catMatch && tierMatch
        }.sortedWith(compareByDescending<AchievementItem> { it.isUnlocked }.thenByDescending { it.def.tier.tierInt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                repository.initialize()
                repository.checkAndUnlock()
            }
        }
    }

    fun selectCategory(category: AchievementCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun selectTier(tier: AchievementTier?) {
        _selectedTier.value = if (_selectedTier.value == tier) null else tier
    }

    fun showAchievementDetail(item: AchievementItem) {
        _selectedAchievement.value = item
    }

    fun dismissAchievementDetail() {
        _selectedAchievement.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.checkAndUnlock() }
        }
    }

    fun getCategoryProgress(category: AchievementCategory): Pair<Int, Int> {
        val items = allItems.value.filter { it.def.category == category }
        return items.count { it.isUnlocked } to items.size
    }

    fun getTierProgress(tier: AchievementTier): Pair<Int, Int> {
        val items = allItems.value.filter { it.def.tier == tier }
        return items.count { it.isUnlocked } to items.size
    }
}
