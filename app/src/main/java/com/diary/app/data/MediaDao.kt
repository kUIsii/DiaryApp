package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: DiaryImage): Long

    @Query("SELECT * FROM diary_images WHERE entryId = :entryId ORDER BY sortOrder ASC")
    suspend fun getImagesForEntry(entryId: Long): List<DiaryImage>

    @Query("SELECT * FROM diary_images WHERE entryId IN (:entryIds)")
    suspend fun getImagesForEntries(entryIds: List<Long>): List<DiaryImage>

    @Query("DELETE FROM diary_images WHERE entryId = :entryId")
    suspend fun deleteImagesForEntry(entryId: Long)

    @Query("SELECT * FROM diary_images")
    suspend fun getAllImages(): List<DiaryImage>

    @Query("SELECT * FROM diary_images")
    fun getAllImagesFlow(): Flow<List<DiaryImage>>

    @Query("DELETE FROM diary_images")
    suspend fun deleteAllImages()

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM diary_images")
    suspend fun getTotalImageFileSize(): Long

    @Query("SELECT COUNT(*) FROM diary_images")
    suspend fun getImageCount(): Int
}
