package com.diary.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY isPreset DESC, name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: Tag): Long

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM diary_tag_cross_ref WHERE diaryId = :diaryId")
    suspend fun getTagsForDiary(diaryId: Long): List<DiaryTag>

    @Query("""
        SELECT t.id, t.name, t.color, t.isPreset, t.parent_id, t.usage_count
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

    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries WHERE id IN (SELECT diaryId FROM diary_tag_cross_ref WHERE tagId = :tagId) ORDER BY createdAt DESC")
    fun getPreviewsByTag(tagId: Long): Flow<List<DiaryPreview>>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int

    @Query("SELECT * FROM tags WHERE isPreset = 1")
    suspend fun getPresetTags(): List<Tag>

    @Query("""
        SELECT dt.diaryId, t.id as tagId, t.name, t.color
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
    """)
    fun getAllDiaryTagPairs(): Flow<List<DiaryTagPair>>

    @Query("""
        SELECT dt.diaryId, t.id as tagId, t.name, t.color
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
    """)
    suspend fun getAllDiaryTagPairsOnce(): List<DiaryTagPair>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsOnce(): List<Tag>

    @Query("SELECT * FROM diary_tag_cross_ref")
    suspend fun getAllDiaryTags(): List<DiaryTag>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Query("UPDATE tags SET name = :name, color = :color WHERE id = :id")
    suspend fun updateTagById(id: Long, name: String, color: Long)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM diary_tag_cross_ref")
    suspend fun deleteAllDiaryTags()

    // Stats queries
    @Query("""
        SELECT t.id as tagId, t.name, t.color, COUNT(*) as count
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
        GROUP BY t.id
        ORDER BY count DESC
    """)
    fun getTagUsage(): Flow<List<TagUsage>>

    @Query("""
        SELECT t.id as tagId, t.name, t.color, COUNT(*) as count
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
        GROUP BY t.id
        ORDER BY count DESC
    """)
    suspend fun getTagUsageOnce(): List<TagUsage>

    // Tag Hierarchy
    @Query("SELECT * FROM tags WHERE parent_id = :parentId")
    fun getChildTags(parentId: Long): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE parent_id IS NULL ORDER BY isPreset DESC, name ASC")
    fun getRootTags(): Flow<List<Tag>>

    @Query("UPDATE tags SET parent_id = :parentId WHERE id = :tagId")
    suspend fun setTagParent(tagId: Long, parentId: Long?)

    @Query("UPDATE tags SET usage_count = (SELECT COUNT(*) FROM diary_tag_cross_ref WHERE tagId = :tagId) WHERE id = :tagId")
    suspend fun updateTagUsageCount(tagId: Long)

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY usage_count DESC")
    suspend fun searchTags(query: String): List<Tag>

    @Query("SELECT * FROM tags WHERE id IN (SELECT tagId FROM diary_tag_cross_ref WHERE diaryId IN (SELECT id FROM diary_entries WHERE createdAt >= :start AND createdAt < :end)) ORDER BY usage_count DESC")
    suspend fun getTagsUsedInRange(start: Long, end: Long): List<Tag>

    // Tag Merge
    @Query("UPDATE diary_tag_cross_ref SET tagId = :targetTagId WHERE tagId = :sourceTagId AND diaryId NOT IN (SELECT diaryId FROM diary_tag_cross_ref WHERE tagId = :targetTagId)")
    suspend fun reassignTags(sourceTagId: Long, targetTagId: Long)

    @Query("DELETE FROM diary_tag_cross_ref WHERE tagId = :sourceTagId")
    suspend fun deleteAllRefsForTag(sourceTagId: Long)

    @Query("UPDATE tags SET usage_count = (SELECT COUNT(*) FROM diary_tag_cross_ref WHERE tagId = :tagId) WHERE id = :tagId")
    suspend fun refreshUsageCount(tagId: Long)

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): Tag?
}
