package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IslandDao {
    // 获取小岛环境
    @Query("SELECT * FROM island_environment WHERE id = 1")
    fun getEnvironment(): Flow<IslandEnvironment?>

    // 获取小岛环境（非Flow）
    @Query("SELECT * FROM island_environment WHERE id = 1")
    suspend fun getEnvironmentOnce(): IslandEnvironment?

    // 设置/更新小岛环境
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setEnvironment(environment: IslandEnvironment)

    // 更新环境维度
    @Query("""
        UPDATE island_environment
        SET lushness = :lushness,
            brightness = :brightness,
            tranquility = :tranquility,
            warmth = :warmth,
            updated_at = :updatedAt
        WHERE id = 1
    """)
    suspend fun updateEnvironment(
        lushness: Float,
        brightness: Float,
        tranquility: Float,
        warmth: Float,
        updatedAt: Long = System.currentTimeMillis()
    )

    // 获取小岛配置
    @Query("SELECT * FROM island_profile WHERE id = 1")
    fun getProfile(): Flow<IslandProfile?>

    // 获取小岛配置（非Flow）
    @Query("SELECT * FROM island_profile WHERE id = 1")
    suspend fun getProfileOnce(): IslandProfile?

    // 设置/更新小岛配置
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProfile(profile: IslandProfile)

    // 增加经验值
    @Query("UPDATE island_profile SET experience = experience + :amount WHERE id = 1")
    suspend fun addExperience(amount: Int)

    // 更新等级
    @Query("UPDATE island_profile SET level = :level WHERE id = 1")
    suspend fun updateLevel(level: Int)

    // 更新连续记录天数
    @Query("UPDATE island_profile SET streak_days = :streak, last_entry_time = :time WHERE id = 1")
    suspend fun updateStreak(streak: Int, time: Long = System.currentTimeMillis())

    // 增加日记总数
    @Query("UPDATE island_profile SET total_entries = total_entries + 1 WHERE id = 1")
    suspend fun incrementEntryCount()

    // 获取装饰列表
    @Query("SELECT * FROM island_decorations ORDER BY unlock_level ASC")
    fun getAllDecorations(): Flow<List<IslandDecoration>>

    // 获取已解锁的装饰
    @Query("SELECT * FROM island_decorations WHERE is_unlocked = 1")
    fun getUnlockedDecorations(): Flow<List<IslandDecoration>>

    // 获取已解锁的装饰（非Flow）
    @Query("SELECT * FROM island_decorations WHERE is_unlocked = 1")
    suspend fun getUnlockedDecorationsOnce(): List<IslandDecoration>

    // 更新装饰解锁状态
    @Query("UPDATE island_decorations SET is_unlocked = 1 WHERE id = :id")
    suspend fun unlockDecoration(id: String)

    // 插入装饰定义
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDecoration(decoration: IslandDecoration)

    // 批量插入装饰定义
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDecorations(decorations: List<IslandDecoration>)

    // 更新已装备的装饰列表
    @Query("UPDATE island_profile SET active_decorations = :activeDecorations WHERE id = 1")
    suspend fun updateActiveDecorations(activeDecorations: String)

    // 插入环境更新记录
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUpdate(update: IslandUpdate)

    // 获取最近的环境更新
    @Query("SELECT * FROM island_updates ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentUpdates(limit: Int = 10): List<IslandUpdate>

    // ==================== 隐藏发现系统 ====================

    // 插入发现记录
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDiscovery(discovery: IslandDiscovery): Long

    // 获取所有发现
    @Query("SELECT * FROM island_discoveries ORDER BY discovered_at DESC")
    fun getAllDiscoveries(): Flow<List<IslandDiscovery>>

    // 获取所有发现（非Flow）
    @Query("SELECT * FROM island_discoveries ORDER BY discovered_at DESC")
    suspend fun getAllDiscoveriesOnce(): List<IslandDiscovery>

    // 检查是否已发现某个元素
    @Query("SELECT EXISTS(SELECT 1 FROM island_discoveries WHERE discovery_key = :key LIMIT 1)")
    suspend fun hasDiscovered(key: String): Boolean

    // 获取特定类型的发现
    @Query("SELECT * FROM island_discoveries WHERE discovery_type = :type")
    suspend fun getDiscoveriesByType(type: String): List<IslandDiscovery>

    // 获取当前激活的稀有元素（未过期的）
    @Query("SELECT * FROM island_discoveries WHERE discovery_type = 'rare_element' AND (expires_at = -1 OR expires_at > :currentTime)")
    suspend fun getActiveRareElements(currentTime: Long = System.currentTimeMillis()): List<IslandDiscovery>

    // 删除过期的发现
    @Query("DELETE FROM island_discoveries WHERE discovery_type = 'rare_element' AND expires_at > 0 AND expires_at < :currentTime")
    suspend fun cleanupExpiredDiscoveries(currentTime: Long = System.currentTimeMillis())

    // ==================== 组合效果系统 ====================

    // 获取已解锁的组合
    @Query("SELECT * FROM island_combos WHERE isUnlocked = 1")
    fun getUnlockedCombos(): Flow<List<IslandCombo>>

    // 获取已解锁的组合（非Flow）
    @Query("SELECT * FROM island_combos WHERE isUnlocked = 1")
    suspend fun getUnlockedCombosOnce(): List<IslandCombo>

    // 解锁组合
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombo(combo: IslandCombo)

    // 检查组合是否已解锁
    @Query("SELECT EXISTS(SELECT 1 FROM island_combos WHERE comboId = :comboId AND isUnlocked = 1 LIMIT 1)")
    suspend fun isComboUnlocked(comboId: String): Boolean

    // ==================== 历史时间线系统 ====================

    // 插入时间线事件
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTimelineEvent(event: IslandTimelineEvent): Long

    // 获取所有时间线事件（按时间倒序）
    @Query("SELECT * FROM island_timeline_events ORDER BY event_time DESC")
    fun getAllTimelineEvents(): Flow<List<IslandTimelineEvent>>

    // 获取所有时间线事件（非Flow）
    @Query("SELECT * FROM island_timeline_events ORDER BY event_time DESC")
    suspend fun getAllTimelineEventsOnce(): List<IslandTimelineEvent>

    // 按类型筛选时间线事件
    @Query("SELECT * FROM island_timeline_events WHERE event_type = :type ORDER BY event_time DESC")
    suspend fun getTimelineEventsByType(type: String): List<IslandTimelineEvent>

    // 获取时间线事件总数
    @Query("SELECT COUNT(*) FROM island_timeline_events")
    suspend fun getTimelineEventCount(): Int

    // 检查是否已存在某类事件（用于去重，如首次记录）
    @Query("SELECT EXISTS(SELECT 1 FROM island_timeline_events WHERE event_type = :type AND message = :message LIMIT 1)")
    suspend fun hasTimelineEvent(type: String, message: String): Boolean
}
