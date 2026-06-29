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
        plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt, writing_duration_seconds
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

    @Query("DELETE FROM todo_items")
    suspend fun deleteAllTodos()

    @Query("DELETE FROM habit_records")
    suspend fun deleteAllHabitRecords()

    @Query("DELETE FROM trash_entries")
    suspend fun deleteAllTrashEntries()

    @Query("DELETE FROM countdown_items")
    suspend fun deleteAllCountDownItems()

    @Query("DELETE FROM time_capsules")
    suspend fun deleteAllCapsules()

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()

    @Query("DELETE FROM chat_conversations")
    suspend fun deleteAllConversations()

    @Transaction
    suspend fun deleteEntryWithTags(entry: DiaryEntry) {
        deleteTagsForDiary(entry.id)
        deleteImagesForEntry(entry.id)
        deleteEntry(entry)
    }

    @Transaction
    suspend fun deleteEntriesWithTags(entries: List<DiaryEntry>) {
        entries.forEach { entry ->
            deleteTagsForDiary(entry.id)
            deleteImagesForEntry(entry.id)
            deleteEntry(entry)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashEntries(trashEntries: List<TrashEntry>)

    @Query("""
        SELECT id, title,
        CASE WHEN instr(content, 'data:image/') > 0 AND length(content) > 262144 THEN '' ELSE content END AS content,
        plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt, writing_duration_seconds
        FROM diary_entries WHERE id IN (:ids)
    """)
    suspend fun getEntriesByIdsSafe(ids: List<Long>): List<DiaryEntry>

    @Query("""
        SELECT * FROM diary_entries
        WHERE plainText LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
           OR id IN (SELECT dt.diaryId FROM diary_tag_cross_ref dt INNER JOIN tags t ON dt.tagId = t.id WHERE t.name LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<DiaryEntry>>

    @Query("""
        SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries
        WHERE plainText LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
           OR id IN (SELECT dt.diaryId FROM diary_tag_cross_ref dt INNER JOIN tags t ON dt.tagId = t.id WHERE t.name LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchPreviews(query: String): Flow<List<DiaryPreview>>

    @Query("""
        SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries
        WHERE plainText LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
           OR id IN (SELECT dt.diaryId FROM diary_tag_cross_ref dt INNER JOIN tags t ON dt.tagId = t.id WHERE t.name LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    suspend fun searchPreviewsOnce(query: String): List<DiaryPreview>

    @Query("UPDATE diary_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE diary_entries SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun batchSetFavorite(ids: List<Long>, isFavorite: Boolean)

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

    @Query("""
        SELECT dt.diaryId, t.id as tagId, t.name, t.color
        FROM diary_tag_cross_ref dt
        INNER JOIN tags t ON dt.tagId = t.id
    """)
    suspend fun getAllDiaryTagPairsOnce(): List<DiaryTagPair>

    @Query("""
        SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt
        FROM diary_entries WHERE id IN (:ids)
    """)
    suspend fun getPreviewsByIds(ids: List<Long>): List<DiaryPreview>

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
        plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt, writing_duration_seconds
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

    // One-shot query for backup
    @Query("SELECT * FROM todo_items")
    suspend fun getAllTodosOnce(): List<TodoItem>

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

    // One-shot query for backup
    @Query("SELECT * FROM habit_records")
    suspend fun getAllHabitRecordsOnce(): List<HabitRecord>

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

    @Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    fun getPreviewsByDateRangeFlow(start: Long, end: Long): Flow<List<DiaryPreview>>

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

    @Query("SELECT * FROM diary_images WHERE entryId IN (:entryIds)")
    suspend fun getImagesForEntries(entryIds: List<Long>): List<DiaryImage>

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

    @Query("""
        SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt
        FROM diary_entries
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        ORDER BY createdAt DESC
    """)
    suspend fun getEntriesWithLocation(): List<DiaryPreview>

    @Query("DELETE FROM trash_entries WHERE deletedAt < :before")
    suspend fun deleteTrashEntriesBefore(before: Long)

    @Query("SELECT * FROM trash_entries WHERE id = :id")
    suspend fun getTrashEntryById(id: Long): TrashEntry?

    // One-shot query for backup
    @Query("SELECT * FROM trash_entries")
    suspend fun getAllTrashEntriesOnce(): List<TrashEntry>

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

    @Query("UPDATE time_capsules SET isOpened = 1 WHERE id = :id")
    suspend fun markCapsuleOpened(id: Long)

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

    // Chat messages
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getChatMessagesByConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentChatMessages(conversationId: Long, limit: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    suspend fun getAllChatMessagesOnce(): List<ChatMessageEntity>

    @Insert
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteChatMessagesByConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getChatMessageCount(conversationId: Long): Int

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId AND id IN (SELECT id FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC LIMIT :count)")
    suspend fun deleteOldestChatMessages(conversationId: Long, count: Int)

    // Chat conversations
    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversationsOnce(): List<ChatConversationEntity>

    @Insert
    suspend fun insertConversation(conversation: ChatConversationEntity): Long

    @Query("UPDATE chat_conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTime(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("UPDATE chat_messages SET conversationId = :newConversationId WHERE conversationId = 0")
    suspend fun migrateOldMessages(newConversationId: Long)

    // Storage management queries
    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM diary_images")
    suspend fun getTotalImageFileSize(): Long

    @Query("SELECT COUNT(*) FROM diary_images")
    suspend fun getImageCount(): Int

    // Writing duration stats
    @Query("SELECT AVG(writing_duration_seconds) FROM diary_entries WHERE writing_duration_seconds IS NOT NULL")
    suspend fun getAverageWritingDurationSeconds(): Double?

    @Query("SELECT SUM(writing_duration_seconds) FROM diary_entries WHERE createdAt >= :start AND createdAt < :end AND writing_duration_seconds IS NOT NULL")
    suspend fun getTotalWritingDurationSeconds(start: Long, end: Long): Int?

    // Small wins queries
    @Query("SELECT * FROM small_wins WHERE recordDate = :date ORDER BY createdAt DESC")
    fun getSmallWinsByDate(date: Long): Flow<List<SmallWin>>

    @Query("SELECT * FROM small_wins WHERE recordDate >= :startDate AND recordDate <= :endDate ORDER BY recordDate DESC, createdAt DESC")
    fun getSmallWinsByDateRange(startDate: Long, endDate: Long): Flow<List<SmallWin>>

    @Query("SELECT * FROM small_wins ORDER BY recordDate DESC, createdAt DESC")
    fun getAllSmallWins(): Flow<List<SmallWin>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSmallWin(smallWin: SmallWin): Long

    @Query("DELETE FROM small_wins WHERE id = :id")
    suspend fun deleteSmallWin(id: Long)

    @Query("SELECT COUNT(*) FROM small_wins WHERE recordDate = :date")
    suspend fun getSmallWinCountByDate(date: Long): Int

    // Quick checkins
    @Query("SELECT * FROM quick_checkins ORDER BY createdAt DESC")
    fun getAllQuickCheckins(): Flow<List<QuickCheckin>>

    @Query("SELECT * FROM quick_checkins WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    fun getQuickCheckinsByDateRange(start: Long, end: Long): Flow<List<QuickCheckin>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuickCheckin(checkin: QuickCheckin): Long

    @Query("DELETE FROM quick_checkins WHERE id = :id")
    suspend fun deleteQuickCheckin(id: Long)

    // Goals
    @Query("SELECT * FROM goals WHERE parentId IS NULL ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE parentId = :parentId ORDER BY createdAt ASC")
    fun getSubGoals(parentId: Long): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("UPDATE goals SET progress = :progress WHERE id = :id")
    suspend fun updateGoalProgress(id: Long, progress: Int)

    // Diary summaries
    @Query("SELECT * FROM diary_summaries WHERE diaryId = :diaryId")
    suspend fun getSummaryForDiary(diaryId: Long): DiarySummary?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiarySummary(summary: DiarySummary): Long

    @Query("DELETE FROM diary_summaries WHERE diaryId = :diaryId")
    suspend fun deleteDiarySummary(diaryId: Long)

    // Focus sessions
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllFocusSessions(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFocusSession(session: FocusSession): Long

    @Query("UPDATE focus_sessions SET endTime = :endTime, completedAt = :completedAt WHERE id = :id")
    suspend fun completeFocusSession(id: Long, endTime: Long, completedAt: Long)

    // Cover themes
    @Query("SELECT * FROM cover_themes ORDER BY createdAt DESC")
    fun getAllCoverThemes(): Flow<List<CoverTheme>>

    @Query("SELECT * FROM cover_themes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCoverTheme(): CoverTheme?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverTheme(theme: CoverTheme): Long

    @Query("UPDATE cover_themes SET isActive = 0")
    suspend fun deactivateAllCoverThemes()

    @Query("UPDATE cover_themes SET isActive = 1 WHERE id = :id")
    suspend fun activateCoverTheme(id: Long)

    @Update
    suspend fun updateCoverTheme(theme: CoverTheme)

    @Query("DELETE FROM cover_themes WHERE id = :id")
    suspend fun deleteCoverThemeById(id: Long)

    // Diary embeddings
    @Query("SELECT * FROM diary_embeddings WHERE diaryId = :diaryId")
    suspend fun getEmbeddingForDiary(diaryId: Long): DiaryEmbedding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEmbedding(embedding: DiaryEmbedding): Long

    @Query("SELECT * FROM diary_embeddings")
    suspend fun getAllDiaryEmbeddings(): List<DiaryEmbedding>

    // Location memories
    @Query("SELECT * FROM location_memories ORDER BY createdAt DESC")
    fun getAllLocationMemories(): Flow<List<LocationMemory>>

    @Query("SELECT * FROM location_memories WHERE diaryId = :diaryId")
    suspend fun getLocationMemoriesForDiary(diaryId: Long): List<LocationMemory>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLocationMemory(memory: LocationMemory): Long

    @Query("DELETE FROM location_memories WHERE id = :id")
    suspend fun deleteLocationMemory(id: Long)

    // Voice memos
    @Query("SELECT * FROM voice_memos ORDER BY createdAt DESC")
    fun getAllVoiceMemos(): Flow<List<VoiceMemo>>

    @Query("SELECT * FROM voice_memos WHERE diaryId = :diaryId")
    suspend fun getVoiceMemosForDiary(diaryId: Long): List<VoiceMemo>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVoiceMemo(memo: VoiceMemo): Long

    @Update
    suspend fun updateVoiceMemo(memo: VoiceMemo)

    @Query("DELETE FROM voice_memos WHERE id = :id")
    suspend fun deleteVoiceMemo(id: Long)

    // Emotion radar
    @Query("SELECT * FROM emotion_radar WHERE diaryId = :diaryId")
    suspend fun getEmotionRadarForDiary(diaryId: Long): EmotionRadar?

    @Query("SELECT * FROM emotion_radar ORDER BY createdAt DESC")
    fun getAllEmotionRadars(): Flow<List<EmotionRadar>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmotionRadar(radar: EmotionRadar): Long

    // Memory anchors
    @Query("SELECT * FROM memory_anchors ORDER BY createdAt DESC")
    fun getAllMemoryAnchors(): Flow<List<MemoryAnchor>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMemoryAnchor(anchor: MemoryAnchor): Long

    @Query("DELETE FROM memory_anchors WHERE id = :id")
    suspend fun deleteMemoryAnchor(id: Long)

    // Anchor relations
    @Query("SELECT * FROM anchor_relations WHERE anchorId = :anchorId ORDER BY createdAt DESC")
    fun getAnchorRelations(anchorId: Long): Flow<List<AnchorRelation>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnchorRelation(relation: AnchorRelation): Long

    // Writing fingerprints
    @Query("SELECT * FROM writing_fingerprints WHERE diaryId = :diaryId")
    suspend fun getWritingFingerprintForDiary(diaryId: Long): WritingFingerprint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWritingFingerprint(fingerprint: WritingFingerprint): Long

    // Decisions
    @Query("SELECT * FROM decisions ORDER BY madeAt DESC")
    fun getAllDecisions(): Flow<List<Decision>>

    @Query("SELECT * FROM decisions WHERE diaryId = :diaryId")
    suspend fun getDecisionsForDiary(diaryId: Long): List<Decision>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDecision(decision: Decision): Long

    @Update
    suspend fun updateDecision(decision: Decision)

    // Extracted values
    @Query("SELECT * FROM extracted_values ORDER BY confidence DESC")
    fun getAllExtractedValues(): Flow<List<ExtractedValue>>

    @Query("SELECT * FROM extracted_values WHERE category = :category")
    fun getExtractedValuesByCategory(category: String): Flow<List<ExtractedValue>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtractedValue(value: ExtractedValue): Long

    // Writing experiments
    @Query("SELECT * FROM writing_experiments ORDER BY startDate DESC")
    fun getAllWritingExperiments(): Flow<List<WritingExperiment>>

    @Query("SELECT * FROM writing_experiments WHERE status = 'active' LIMIT 1")
    suspend fun getActiveWritingExperiment(): WritingExperiment?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWritingExperiment(experiment: WritingExperiment): Long

    @Update
    suspend fun updateWritingExperiment(experiment: WritingExperiment)

    // Experiment participations
    @Query("SELECT * FROM experiment_participations WHERE experimentId = :experimentId ORDER BY dayNumber ASC")
    fun getExperimentParticipations(experimentId: Long): Flow<List<ExperimentParticipation>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExperimentParticipation(participation: ExperimentParticipation): Long

    // Monthly challenges
    @Query("SELECT * FROM monthly_challenges ORDER BY year DESC, month DESC")
    fun getAllMonthlyChallenges(): Flow<List<MonthlyChallenge>>

    @Query("SELECT * FROM monthly_challenges WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getMonthlyChallenge(year: Int, month: Int): MonthlyChallenge?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMonthlyChallenge(challenge: MonthlyChallenge): Long

    @Update
    suspend fun updateMonthlyChallenge(challenge: MonthlyChallenge)

    // Challenge daily logs
    @Query("SELECT * FROM challenge_daily_logs WHERE challengeId = :challengeId ORDER BY date DESC")
    fun getChallengeDailyLogs(challengeId: Long): Flow<List<ChallengeDailyLog>>

    @Query("SELECT * FROM challenge_daily_logs WHERE challengeId = :challengeId AND date = :date LIMIT 1")
    suspend fun getChallengeDailyLog(challengeId: Long, date: Long): ChallengeDailyLog?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChallengeDailyLog(log: ChallengeDailyLog): Long

    @Update
    suspend fun updateChallengeDailyLog(log: ChallengeDailyLog)

    // Streak shields
    @Query("SELECT * FROM streak_shields WHERE month = :month LIMIT 1")
    suspend fun getStreakShieldForMonth(month: String): StreakShield?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStreakShield(shield: StreakShield): Long

    @Update
    suspend fun updateStreakShield(shield: StreakShield)

    // Easter eggs
    @Query("SELECT * FROM easter_eggs ORDER BY triggeredAt DESC")
    fun getAllEasterEggs(): Flow<List<EasterEgg>>

    @Query("SELECT * FROM easter_eggs WHERE eggId = :eggId LIMIT 1")
    suspend fun getEasterEgg(eggId: String): EasterEgg?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEasterEgg(egg: EasterEgg): Long
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



