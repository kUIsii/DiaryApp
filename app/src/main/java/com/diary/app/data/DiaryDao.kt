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

    // Lightweight queries (no content field) for list views - prevents OOM
    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries ORDER BY createdAt DESC")
    fun getAllPreviews(): Flow<List<DiaryPreview>>

    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries WHERE id = :id")
    suspend fun getPreviewById(id: Long): DiaryPreview?

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): DiaryEntry?

    @Query("""
        SELECT id, title,
        CASE WHEN instr(content, 'data:image/') > 0 AND length(content) > 262144 THEN '' ELSE content END AS content,
        plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt
        FROM diary_entries WHERE id = :id
    """)
    suspend fun getEntryByIdSafe(id: Long): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM diary_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM diary_tag_cross_ref")
    suspend fun deleteAllDiaryTags()

    @Query("DELETE FROM diary_images")
    suspend fun deleteAllImages()

    @Transaction
    suspend fun deleteEntryWithTags(entry: DiaryEntry) {
        deleteTagsForDiary(entry.id)
        deleteImagesForEntry(entry.id)
        deleteEntry(entry)
    }

    @Query("SELECT * FROM diary_entries WHERE plainText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchEntries(query: String): Flow<List<DiaryEntry>>

    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries WHERE plainText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchPreviews(query: String): Flow<List<DiaryPreview>>

    @Query("UPDATE diary_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM diary_entries WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoritePreviews(): Flow<List<DiaryPreview>>

    @Query("SELECT createdAt FROM diary_entries")
    fun getAllTimestamps(): Flow<List<Long>>

    // Tag queries
    @Query("SELECT * FROM tags ORDER BY isPreset DESC, name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
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

    // Random entry
    @Query("SELECT id FROM diary_entries ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomEntryId(): Long?

    // One-shot queries for export
    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    suspend fun getAllEntriesOnce(): List<DiaryEntry>

    @Query("SELECT COUNT(*) FROM diary_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getEntriesBatch(offset: Int, limit: Int): List<DiaryEntry>

    @Query("""
        SELECT id, title,
        CASE WHEN instr(content, 'data:image/') > 0 AND length(content) > 262144 THEN '' ELSE content END AS content,
        plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt
        FROM diary_entries ORDER BY createdAt DESC LIMIT :limit OFFSET :offset
    """)
    suspend fun getEntriesBatchForExport(offset: Int, limit: Int): List<DiaryEntry>

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

    @Query("""
        SELECT t.id as tagId, t.name, t.color, COUNT(*) as count
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
        GROUP BY t.id
        ORDER BY count DESC
    """)
    suspend fun getTagUsageOnce(): List<TagUsage>

    // Todo queries
    @Query("SELECT * FROM todo_items WHERE parentId IS NULL ORDER BY isPinned DESC, isCompleted ASC, priority DESC, sortOrder ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE parentId IS NULL AND dueDate >= :dayStart AND dueDate < :dayEnd ORDER BY isPinned DESC, isCompleted ASC, priority DESC, sortOrder ASC")
    fun getTodosForDay(dayStart: Long, dayEnd: Long): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE parentId = :parentId ORDER BY isCompleted ASC, sortOrder ASC, createdAt ASC")
    fun getSubTodos(parentId: Long): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE category = :category AND parentId IS NULL ORDER BY isPinned DESC, isCompleted ASC, priority DESC, sortOrder ASC, createdAt DESC")
    fun getTodosByCategory(category: String): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE tags LIKE '%' || :tag || '%' AND parentId IS NULL ORDER BY isPinned DESC, isCompleted ASC, priority DESC, sortOrder ASC, createdAt DESC")
    fun getTodosByTag(tag: String): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') AND parentId IS NULL ORDER BY isPinned DESC, isCompleted ASC, priority DESC, createdAt DESC")
    fun searchTodos(query: String): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE dueDate >= :dayStart AND dueDate < :dayEnd AND isCompleted = 0 AND parentId IS NULL ORDER BY priority DESC, dueDate ASC")
    fun getTodayTodos(dayStart: Long, dayEnd: Long): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND parentId IS NULL ORDER BY priority DESC, dueDate ASC")
    fun getUpcomingTodos(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("UPDATE todo_items SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun toggleTodo(id: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE todo_items SET isPinned = :isPinned WHERE id = :id")
    suspend fun pinTodo(id: Long, isPinned: Boolean)

    @Query("UPDATE todo_items SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int)

    @Query("UPDATE todo_items SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM todo_items WHERE isCompleted = 1 AND completedAt < :before")
    suspend fun deleteCompletedTodosBefore(before: Long)

    @Query("DELETE FROM todo_items WHERE parentId = :parentId")
    suspend fun deleteSubTodos(parentId: Long)

    // Todo reminder queries
    @Query("SELECT * FROM todo_items WHERE reminderTime IS NOT NULL AND reminderTime > :now AND isCompleted = 0 ORDER BY reminderTime ASC")
    suspend fun getPendingReminderTodos(now: Long = System.currentTimeMillis()): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoItem?

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND parentId IS NULL ORDER BY isPinned DESC, reminderTime ASC")
    fun getAllPendingTodos(): Flow<List<TodoItem>>

    // Widget queries
    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND parentId IS NULL ORDER BY isPinned DESC, priority DESC, dueDate ASC LIMIT :limit")
    suspend fun getTopPendingTodos(limit: Int = 10): List<TodoItem>

    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = 0 AND parentId IS NULL")
    fun getPendingTodoCount(): Flow<Int>

    // Recurring task queries
    @Query("SELECT * FROM todo_items WHERE recurringType != 'none' AND isCompleted = 1 AND parentId IS NULL")
    suspend fun getCompletedRecurringTodos(): List<TodoItem>

    // Habit record queries
    @Query("SELECT * FROM habit_records WHERE todoId = :todoId ORDER BY recordDate DESC")
    fun getHabitRecords(todoId: Long): Flow<List<HabitRecord>>

    @Query("SELECT * FROM habit_records WHERE todoId = :todoId AND recordDate = :recordDate LIMIT 1")
    suspend fun getHabitRecordForDay(todoId: Long, recordDate: Long): HabitRecord?

    @Query("SELECT * FROM habit_records WHERE todoId IN (:todoIds) AND recordDate BETWEEN :startDate AND :endDate")
    suspend fun getHabitRecordsInRange(todoIds: List<Long>, startDate: Long, endDate: Long): List<HabitRecord>

    @Query("SELECT * FROM habit_records WHERE todoId IN (:todoIds) AND recordDate BETWEEN :startDate AND :endDate")
    fun getHabitRecordsInRangeFlow(todoIds: List<Long>, startDate: Long, endDate: Long): Flow<List<HabitRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitRecord(record: HabitRecord): Long

    @Update
    suspend fun updateHabitRecord(record: HabitRecord)

    @Query("DELETE FROM habit_records WHERE id = :id")
    suspend fun deleteHabitRecordById(id: Long)

    @Query("DELETE FROM habit_records WHERE todoId = :todoId")
    suspend fun deleteHabitRecordsForTodo(todoId: Long)

    @Query("DELETE FROM habit_records WHERE todoId = :todoId AND recordDate = :recordDate")
    suspend fun deleteHabitRecordForDay(todoId: Long, recordDate: Long)

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

    @Query("""
        SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries
        WHERE id != :excludeId
          AND CAST(strftime('%m', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :month
          AND CAST(strftime('%d', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :day
          AND CAST(strftime('%Y', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) != :year
        ORDER BY createdAt DESC
    """)
    fun getOnThisDayPreviews(month: Int, day: Int, year: Int, excludeId: Long = -1): Flow<List<DiaryPreview>>

    // Get entries within a timestamp range (one-shot)
    @Query("SELECT * FROM diary_entries WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    suspend fun getEntriesByDateRange(start: Long, end: Long): List<DiaryEntry>

    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    suspend fun getPreviewsByDateRange(start: Long, end: Long): List<DiaryPreview>

    // Get entries matching month+day across all years (for review)
    @Query("""
        SELECT * FROM diary_entries
        WHERE CAST(strftime('%m', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :month
          AND CAST(strftime('%d', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :day
        ORDER BY createdAt DESC
    """)
    suspend fun getEntriesByMonthDay(month: Int, day: Int): List<DiaryEntry>

    @Query("""
        SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries
        WHERE CAST(strftime('%m', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :month
          AND CAST(strftime('%d', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) = :day
        ORDER BY createdAt DESC
    """)
    suspend fun getPreviewsByMonthDay(month: Int, day: Int): List<DiaryPreview>

    // Image queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: DiaryImage): Long

    @Query("SELECT * FROM diary_images WHERE entryId = :entryId ORDER BY sortOrder ASC")
    suspend fun getImagesForEntry(entryId: Long): List<DiaryImage>

    @Query("DELETE FROM diary_images WHERE entryId = :entryId")
    suspend fun deleteImagesForEntry(entryId: Long)

    @Query("SELECT * FROM diary_images")
    suspend fun getAllImages(): List<DiaryImage>

    @Query("SELECT * FROM diary_images")
    fun getAllImagesFlow(): Flow<List<DiaryImage>>

    // Trash queries
    @Query("SELECT * FROM trash_entries ORDER BY deletedAt DESC")
    fun getTrashEntries(): Flow<List<TrashEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashEntry(trashEntry: TrashEntry): Long

    @Query("DELETE FROM trash_entries WHERE id = :id")
    suspend fun deleteTrashEntryById(id: Long)

    // Recent locations
    @Query("""
        SELECT DISTINCT location, latitude, longitude
        FROM diary_entries
        WHERE location IS NOT NULL AND location != ''
        ORDER BY createdAt DESC
        LIMIT 10
    """)
    suspend fun getRecentLocations(): List<RecentLocation>

    @Query("DELETE FROM trash_entries WHERE deletedAt < :before")
    suspend fun deleteTrashEntriesBefore(before: Long)

    @Query("SELECT * FROM trash_entries WHERE id = :id")
    suspend fun getTrashEntryById(id: Long): TrashEntry?

    // CountDown queries
    @Query("SELECT * FROM countdown_items ORDER BY isPinned DESC, targetDate ASC")
    fun getAllCountDownItems(): Flow<List<CountDownItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCountDownItem(item: CountDownItem): Long

    @Update
    suspend fun updateCountDownItem(item: CountDownItem)

    @Query("DELETE FROM countdown_items WHERE id = :id")
    suspend fun deleteCountDownItem(id: Long)

    @Query("SELECT * FROM countdown_items WHERE id = :id")
    suspend fun getCountDownItemById(id: Long): CountDownItem?

    // Widget queries for CountDown
    @Query("SELECT * FROM countdown_items ORDER BY isPinned DESC, targetDate ASC LIMIT :limit")
    suspend fun getTopCountDownItems(limit: Int = 10): List<CountDownItem>

    // One-shot query for widget
    @Query("SELECT * FROM countdown_items ORDER BY isPinned DESC, targetDate ASC")
    suspend fun getAllCountDownItemsOnce(): List<CountDownItem>

    // Time Capsule queries
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

    @Query("SELECT * FROM time_capsules")
    suspend fun getAllCapsulesOnce(): List<TimeCapsule>

    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries")
    suspend fun getAllPreviewsOnce(): List<DiaryPreview>

    // 通知查询
    @Query("SELECT * FROM notifications WHERE isTrashed = 0 ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isTrashed = 0 AND type = :type ORDER BY createdAt DESC")
    fun getNotificationsByType(type: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET isTrashed = 1, trashedAt = :trashedAt WHERE id = :id")
    suspend fun trashNotification(id: String, trashedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isTrashed = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restoreNotification(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications WHERE isTrashed = 1 AND trashedAt < :before")
    suspend fun deleteTrashedNotificationsBefore(before: Long)

    @Query("SELECT COUNT(*) FROM notifications WHERE isTrashed = 0 AND isRead = 0")
    fun getUnreadNotificationCount(): Flow<Int>

    @Query("SELECT * FROM notifications WHERE isTrashed = 0 AND createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    fun getNotificationsByDateRange(start: Long, end: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotificationsOnce(): List<NotificationEntity>
}

// Lightweight projection without content field - used for list views to avoid OOM
data class DiaryPreview(
    val id: Long,
    val title: String,
    val plainText: String,
    val moodLevel: Int?,
    val weather: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

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

data class RecentLocation(
    val location: String,
    val latitude: Double?,
    val longitude: Double?
)
