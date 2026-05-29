package com.diary.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE plainText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchEntries(query: String): Flow<List<DiaryEntry>>

    @Query("SELECT createdAt FROM diary_entries")
    fun getAllTimestamps(): Flow<List<Long>>

    // Tag queries
    @Query("SELECT * FROM tags ORDER BY isPreset DESC, name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM diary_tag_cross_ref WHERE diaryId = :diaryId")
    suspend fun getTagsForDiary(diaryId: Long): List<DiaryTag>

    @Query("""
        SELECT t.id, t.name, t.color, t.isPreset
        FROM tags t
        INNER JOIN diary_tag_cross_ref dt ON t.id = dt.tagId
        WHERE dt.diaryId = :diaryId
    """)
    suspend fun getTagInfoForDiary(diaryId: Long): List<Tag>

    @Query("DELETE FROM diary_tag_cross_ref WHERE diaryId = :diaryId")
    suspend fun deleteTagsForDiary(diaryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryTag(diaryTag: DiaryTag)

    @Query("SELECT * FROM diary_entries WHERE id IN (SELECT diaryId FROM diary_tag_cross_ref WHERE tagId = :tagId) ORDER BY createdAt DESC")
    fun getEntriesByTag(tagId: Long): Flow<List<DiaryEntry>>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int

    @Query("SELECT * FROM tags WHERE isPreset = 1")
    suspend fun getPresetTags(): List<Tag>

    // One-shot queries for export
    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    suspend fun getAllEntriesOnce(): List<DiaryEntry>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsOnce(): List<Tag>

    @Query("SELECT * FROM diary_tag_cross_ref")
    suspend fun getAllDiaryTags(): List<DiaryTag>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Query("UPDATE tags SET name = :name, color = :color WHERE id = :id")
    suspend fun updateTagById(id: Long, name: String, color: Long)

    // Stats queries
    @Query("""
        SELECT t.id as tagId, t.name, t.color, COUNT(*) as count
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
        GROUP BY t.id
        ORDER BY count DESC
    """)
    fun getTagUsage(): Flow<List<TagUsage>>
}

data class TagUsage(
    val tagId: Long,
    val name: String,
    val color: Long,
    val count: Int
)
