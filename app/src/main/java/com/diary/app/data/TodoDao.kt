package com.diary.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
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

    @Query("SELECT * FROM todo_items WHERE reminderTime IS NOT NULL AND reminderTime > :now AND isCompleted = 0 ORDER BY reminderTime ASC")
    suspend fun getPendingReminderTodos(now: Long = System.currentTimeMillis()): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoItem?

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND parentId IS NULL ORDER BY isPinned DESC, reminderTime ASC")
    fun getAllPendingTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND parentId IS NULL ORDER BY isPinned DESC, priority DESC, dueDate ASC LIMIT :limit")
    suspend fun getTopPendingTodos(limit: Int = 10): List<TodoItem>

    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = 0 AND parentId IS NULL")
    fun getPendingTodoCount(): Flow<Int>

    @Query("SELECT * FROM todo_items")
    suspend fun getAllTodosOnce(): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE recurringType != 'none' AND isCompleted = 1 AND parentId IS NULL")
    suspend fun getCompletedRecurringTodos(): List<TodoItem>

    @Query("DELETE FROM todo_items")
    suspend fun deleteAllTodos()

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

    @Query("SELECT * FROM habit_records")
    suspend fun getAllHabitRecordsOnce(): List<HabitRecord>

    @Query("DELETE FROM habit_records")
    suspend fun deleteAllHabitRecords()
}
