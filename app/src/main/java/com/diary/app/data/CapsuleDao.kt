package com.diary.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CapsuleDao {
    @Insert
    suspend fun insertCapsule(capsule: TimeCapsule): Long

    @Delete
    suspend fun deleteCapsule(capsule: TimeCapsule)

    @Query("SELECT * FROM time_capsules ORDER BY unlockDate DESC")
    fun getAllCapsules(): Flow<List<TimeCapsule>>

    @Query("SELECT * FROM time_capsules WHERE id = :id")
    suspend fun getCapsuleById(id: Long): TimeCapsule?

    @Query("UPDATE time_capsules SET isRead = 1 WHERE id = :id")
    suspend fun markCapsuleRead(id: Long)

    @Query("UPDATE time_capsules SET isOpened = 1 WHERE id = :id")
    suspend fun markCapsuleOpened(id: Long)

    @Query("SELECT * FROM time_capsules")
    suspend fun getAllCapsulesOnce(): List<TimeCapsule>

    @Query("DELETE FROM time_capsules")
    suspend fun deleteAllCapsules()
}
