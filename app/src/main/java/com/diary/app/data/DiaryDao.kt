package com.diary.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT * FROM diary_entries WHERE plainText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchEntries(query: String): Flow<List<DiaryEntry>>

    @Query("UPDATE diary_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

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

    @Query("""
        SELECT dt.diaryId, t.id as tagId, t.name, t.color
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
    """)
    fun getAllDiaryTagPairs(): Flow<List<DiaryTagPair>>

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

    // Todo queries
    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, priority DESC, sortOrder ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE dueDate >= :dayStart AND dueDate < :dayEnd ORDER BY isCompleted ASC, priority DESC, sortOrder ASC")
    fun getTodosForDay(dayStart: Long, dayEnd: Long): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("UPDATE todo_items SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun toggleTodo(id: Long, completed: Boolean, completedAt: Long?)

    @Query("DELETE FROM todo_items WHERE isCompleted = 1 AND completedAt < :before")
    suspend fun deleteCompletedTodosBefore(before: Long)

    // "On This Day" - get entries from the same month+day in previous years
    // We use SQLite strftime to extract month and day from the epoch timestamp
    @Query("""
        SELECT * FROM diary_entries
        WHERE id != :excludeId
          AND CAST(strftime('%m', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :month
          AND CAST(strftime('%d', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :day
          AND CAST(strftime('%Y', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) != :year
        ORDER BY createdAt DESC
    """)
    fun getOnThisDayEntries(month: Int, day: Int, year: Int, excludeId: Long = -1): Flow<List<DiaryEntry>>

    // Get entries within a timestamp range (one-shot)
    @Query("SELECT * FROM diary_entries WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    suspend fun getEntriesByDateRange(start: Long, end: Long): List<DiaryEntry>

    // Get entries matching month+day across all years (for review)
    @Query("""
        SELECT * FROM diary_entries
        WHERE CAST(strftime('%m', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :month
          AND CAST(strftime('%d', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :day
        ORDER BY createdAt DESC
    """)
    suspend fun getEntriesByMonthDay(month: Int, day: Int): List<DiaryEntry>
}

data class TagUsage(
    val tagId: Long,
    val name: String,
    val color: Long,
    val count: Int
)

data class DiaryTagPair(
    val diaryId: Long,
    val tagId: Long,
    val name: String,
    val color: Long
)
