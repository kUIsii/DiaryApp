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
}
