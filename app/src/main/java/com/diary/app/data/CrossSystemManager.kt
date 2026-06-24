package com.diary.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 跨系统联动管理器 - 在宠物/小岛/称号 ViewModel 之间传递状态
 * 使用 Kotlin object 单例，通过 Application 级别共享
 */
object CrossSystemManager {

    // ==================== 宠物 -> 小岛 ====================

    /** 宠物当前状态（供小岛读取以调整氛围） */
    private val _petState = MutableStateFlow(PetState.CALM)
    val petState: StateFlow<PetState> = _petState.asStateFlow()

    fun updatePetState(state: PetState) {
        _petState.value = state
    }

    // ==================== 小岛 -> 宠物 ====================

    /** 小岛等级（供宠物外观装饰显示） */
    private val _islandLevel = MutableStateFlow(1)
    val islandLevel: StateFlow<Int> = _islandLevel.asStateFlow()

    fun updateIslandLevel(level: Int) {
        _islandLevel.value = level
    }

    // ==================== 称号 -> 宠物 ====================

    /** 称号解锁事件流 */
    private val _titleUnlockEvents = MutableSharedFlow<TitleUnlockEvent>(extraBufferCapacity = 64)
    val titleUnlockEvents: SharedFlow<TitleUnlockEvent> = _titleUnlockEvents.asSharedFlow()

    fun emitTitleUnlock(tier: Int, titleName: String) {
        _titleUnlockEvents.tryEmit(TitleUnlockEvent(tier, titleName))
    }

    // ==================== 成就 -> 全系统 ====================

    /** 最近解锁的成就名 */
    private val _recentAchievementUnlock = MutableStateFlow<String?>(null)
    val recentAchievementUnlock: StateFlow<String?> = _recentAchievementUnlock.asStateFlow()

    fun updateRecentAchievementUnlock(name: String?) {
        _recentAchievementUnlock.value = name
    }

    /** 最接近完成的成就名 */
    private val _nextAchievementMilestone = MutableStateFlow<String?>(null)
    val nextAchievementMilestone: StateFlow<String?> = _nextAchievementMilestone.asStateFlow()

    fun updateNextAchievementMilestone(name: String?) {
        _nextAchievementMilestone.value = name
    }

    /** 宠物连续记录天数 */
    private val _petStreakDays = MutableStateFlow(0)
    val petStreakDays: StateFlow<Int> = _petStreakDays.asStateFlow()

    fun updatePetStreakDays(days: Int) {
        _petStreakDays.value = days.coerceAtLeast(0)
    }

    /** 小岛当前激活的稀有现象数量 */
    private val _activeRareDiscoveryCount = MutableStateFlow(0)
    val activeRareDiscoveryCount: StateFlow<Int> = _activeRareDiscoveryCount.asStateFlow()

    fun updateActiveRareDiscoveryCount(count: Int) {
        _activeRareDiscoveryCount.value = count.coerceAtLeast(0)
    }
}

/**
 * 称号解锁事件数据
 */
data class TitleUnlockEvent(
    val tier: Int,          // 1=普通, 2=稀有, 3=传说
    val titleName: String
)
