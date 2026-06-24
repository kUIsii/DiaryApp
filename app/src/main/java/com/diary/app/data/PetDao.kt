package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    // 获取宠物配置
    @Query("SELECT * FROM pet_profile WHERE id = 1")
    fun getPetProfile(): Flow<PetProfile?>

    // 获取宠物配置（非Flow）
    @Query("SELECT * FROM pet_profile WHERE id = 1")
    suspend fun getPetProfileOnce(): PetProfile?

    // 设置/更新宠物配置
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPetProfile(profile: PetProfile)

    // 获取宠物性格
    @Query("SELECT * FROM pet_personality WHERE id = 1")
    fun getPersonality(): Flow<PetPersonality?>

    // 获取宠物性格（非Flow）
    @Query("SELECT * FROM pet_personality WHERE id = 1")
    suspend fun getPersonalityOnce(): PetPersonality?

    // 设置/更新宠物性格
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPersonality(personality: PetPersonality)

    // 插入状态记录
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStateRecord(record: PetStateRecord)

    // 获取最近的状态记录
    @Query("SELECT * FROM pet_states ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentStates(limit: Int = 10): List<PetStateRecord>

    // 获取指定时间之后的状态记录
    @Query("SELECT * FROM pet_states WHERE created_at >= :since ORDER BY created_at ASC")
    suspend fun getStatesSince(since: Long): List<PetStateRecord>

    // 获取状态记录数量
    @Query("SELECT COUNT(*) FROM pet_states")
    suspend fun getStateCount(): Int

    // 更新宠物状态
    @Query("UPDATE pet_profile SET current_state = :state, last_interaction = :time WHERE id = 1")
    suspend fun updateState(state: String, time: Long = System.currentTimeMillis())

    // 更新连续记录天数
    @Query("UPDATE pet_profile SET streak_days = :streak, last_entry_time = :time WHERE id = 1")
    suspend fun updateStreak(streak: Int, time: Long = System.currentTimeMillis())

    // 增加好感度
    @Query("UPDATE pet_profile SET affection = affection + :amount WHERE id = 1")
    suspend fun addAffection(amount: Int = 1)

    // 获取不同状态种类数量
    @Query("SELECT COUNT(DISTINCT state) FROM pet_states")
    suspend fun getDistinctStateCount(): Int

    // 更新成长阶段
    @Query("UPDATE pet_profile SET growth_stage = :stage, evolved_at = :evolvedAt WHERE id = 1")
    suspend fun updateGrowthStage(stage: String, evolvedAt: Long?)

    // 更新已发现的隐藏状态
    @Query("UPDATE pet_profile SET discovered_hidden_states = :hiddenStates WHERE id = 1")
    suspend fun updateDiscoveredHiddenStates(hiddenStates: String)

    // ==================== 宠物记忆 ====================

    // 保存记忆
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: PetMemory): Long

    // 获取指定类型的记忆
    @Query("SELECT * FROM pet_memory WHERE type = :type ORDER BY strength DESC, created_at DESC")
    suspend fun getMemoriesByType(type: String): List<PetMemory>

    // 获取最近的记忆
    @Query("SELECT * FROM pet_memory ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 10): List<PetMemory>

    // 获取最强的记忆（用于触发文案）
    @Query("SELECT * FROM pet_memory WHERE type = :type ORDER BY strength DESC LIMIT 1")
    suspend fun getStrongestMemory(type: String): PetMemory?

    // 更新记忆强度
    @Query("UPDATE pet_memory SET strength = :newStrength, last_activated_at = :activatedAt WHERE id = :memoryId")
    suspend fun updateMemoryStrength(memoryId: Long, newStrength: Float, activatedAt: Long = System.currentTimeMillis())

    // 衰减所有记忆强度
    @Query("UPDATE pet_memory SET strength = MAX(0.1, strength - :decayAmount) WHERE strength > 0.1")
    suspend fun decayAllMemories(decayAmount: Float = 0.05f)

    // 删除过期记忆（强度低于阈值且超过30天）
    @Query("DELETE FROM pet_memory WHERE strength <= 0.1 AND created_at < :expireTime")
    suspend fun deleteExpiredMemories(expireTime: Long)

    // 检查是否已存在相同类型的纪念记忆
    @Query("SELECT COUNT(*) FROM pet_memory WHERE type = :type AND content LIKE :contentPattern")
    suspend fun countMemoriesByContent(type: String, contentPattern: String): Int

    // ==================== 隐藏状态 ====================

    // 插入隐藏状态记录
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenState(hiddenState: PetHiddenState): Long

    // 获取所有隐藏状态
    @Query("SELECT * FROM pet_hidden_states ORDER BY first_discovered_at ASC")
    suspend fun getAllHiddenStates(): List<PetHiddenState>

    // 获取已发现的隐藏状态数量
    @Query("SELECT COUNT(*) FROM pet_hidden_states")
    suspend fun getDiscoveredHiddenStateCount(): Int

    // 更新隐藏状态激活次数
    @Query("UPDATE pet_hidden_states SET activation_count = activation_count + 1, is_active = :isActive WHERE state_type = :stateType")
    suspend fun updateHiddenStateActivation(stateType: String, isActive: Boolean)

    // 获取指定类型的隐藏状态
    @Query("SELECT * FROM pet_hidden_states WHERE state_type = :stateType LIMIT 1")
    suspend fun getHiddenStateByType(stateType: String): PetHiddenState?

    // 停用所有隐藏状态
    @Query("UPDATE pet_hidden_states SET is_active = 0")
    suspend fun deactivateAllHiddenStates()

    // ==================== 隐藏状态检测查询 ====================

    // 获取某天的日记数量
    @Query("SELECT COUNT(*) FROM diary_entries WHERE createdAt >= :dayStart AND createdAt < :dayEnd")
    suspend fun getEntryCountForDay(dayStart: Long, dayEnd: Long): Int

    // 获取某天的所有日记（用于时间旅人检测）
    @Query("SELECT * FROM diary_entries WHERE createdAt >= :dayStart AND createdAt < :dayEnd ORDER BY createdAt ASC")
    suspend fun getEntriesForDay(dayStart: Long, dayEnd: Long): List<DiaryEntry>

    // 获取最近N天的情绪状态记录（用于暖心守护者检测）
    @Query("SELECT * FROM pet_states WHERE created_at >= :since ORDER BY created_at DESC")
    suspend fun getStateRecordsSince(since: Long): List<PetStateRecord>
}
