package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC, key ASC")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE unlockedAt IS NOT NULL ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: Achievement)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<Achievement>)

    @Update
    suspend fun update(achievement: Achievement)

    @Query("UPDATE achievements SET unlockedAt = :unlockedAt, progress = :progress WHERE key = :key")
    suspend fun unlock(key: String, unlockedAt: Long, progress: Int)

    @Query("UPDATE achievements SET progress = :progress WHERE key = :key AND unlockedAt IS NULL")
    suspend fun updateProgress(key: String, progress: Int)

    @Query("SELECT COUNT(*) FROM achievements WHERE unlockedAt IS NOT NULL")
    fun getUnlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM achievements")
    fun getTotalCount(): Flow<Int>

    // Unified achievement system queries

    @Query("SELECT * FROM achievements ORDER BY category, tier DESC, key ASC")
    fun getAllUnified(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE category = :category ORDER BY tier DESC, key ASC")
    fun getByCategory(category: String): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE tier = :tier ORDER BY category, key ASC")
    fun getByTier(tier: Int): Flow<List<Achievement>>

    @Query("SELECT COUNT(*) FROM achievements WHERE unlockedAt IS NOT NULL AND category = :category")
    fun getUnlockedCountByCategory(category: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM achievements WHERE category = :category")
    fun getTotalCountByCategory(category: String): Flow<Int>

    @Query("UPDATE achievements SET progress = :progress WHERE key = :key")
    suspend fun setProgress(key: String, progress: Int)

    @Query("UPDATE achievements SET category = :category, tier = :tier, iconEmoji = :iconEmoji, flavorText = :flavorText, isHidden = :isHidden, target = :target WHERE key = :key")
    suspend fun updateMetadata(key: String, category: String, tier: Int, iconEmoji: String, flavorText: String, isHidden: Boolean, target: Int)
}
