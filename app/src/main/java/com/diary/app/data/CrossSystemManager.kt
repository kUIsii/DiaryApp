package com.diary.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 成就系统共享状态。
 */
object CrossSystemManager {

    private val _recentAchievementUnlock = MutableStateFlow<String?>(null)
    val recentAchievementUnlock: StateFlow<String?> = _recentAchievementUnlock.asStateFlow()

    fun updateRecentAchievementUnlock(name: String?) {
        _recentAchievementUnlock.value = name
    }

    private val _nextAchievementMilestone = MutableStateFlow<String?>(null)
    val nextAchievementMilestone: StateFlow<String?> = _nextAchievementMilestone.asStateFlow()

    fun updateNextAchievementMilestone(name: String?) {
        _nextAchievementMilestone.value = name
    }
}
