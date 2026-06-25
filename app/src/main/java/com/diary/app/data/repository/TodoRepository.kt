package com.diary.app.data.repository

import com.diary.app.data.HabitRecord
import com.diary.app.data.TodoDao
import com.diary.app.data.TodoItem
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao) {

    fun getAllTodos(): Flow<List<TodoItem>> = dao.getAllTodos()
    fun getTodosByCategory(category: String) = dao.getTodosByCategory(category)
    fun getTodosByTag(tag: String) = dao.getTodosByTag(tag)
    fun searchTodos(query: String) = dao.searchTodos(query)
    fun getTodayTodos(start: Long, end: Long) = dao.getTodayTodos(start, end)
    fun getPendingTodoCount(): Flow<Int> = dao.getPendingTodoCount()
    fun getAllPendingTodos() = dao.getAllPendingTodos()

    suspend fun insertTodo(todo: TodoItem): Long = dao.insertTodo(todo)
    suspend fun updateTodo(todo: TodoItem) = dao.updateTodo(todo)
    suspend fun deleteTodo(todo: TodoItem) = dao.deleteTodo(todo)
    suspend fun getTodoById(id: Long): TodoItem? = dao.getTodoById(id)
    suspend fun toggleTodo(id: Long, completed: Boolean, completedAt: Long?) = dao.toggleTodo(id, completed, completedAt)
    suspend fun pinTodo(id: Long, isPinned: Boolean) = dao.pinTodo(id, isPinned)
    suspend fun updateSortOrder(id: Long, sortOrder: Int) = dao.updateSortOrder(id, sortOrder)
    suspend fun deleteCompletedTodosBefore(before: Long) = dao.deleteCompletedTodosBefore(before)

    fun getSubTodos(parentId: Long) = dao.getSubTodos(parentId)
    suspend fun deleteSubTodos(parentId: Long) = dao.deleteSubTodos(parentId)
    suspend fun getPendingReminderTodos(now: Long) = dao.getPendingReminderTodos(now)
    suspend fun getAllTodosOnce() = dao.getAllTodosOnce()

    fun getHabitRecords(todoId: Long) = dao.getHabitRecords(todoId)
    suspend fun getHabitRecordForDay(todoId: Long, recordDate: Long) = dao.getHabitRecordForDay(todoId, recordDate)
    suspend fun insertHabitRecord(record: HabitRecord): Long = dao.insertHabitRecord(record)
    suspend fun deleteHabitRecordsForTodo(todoId: Long) = dao.deleteHabitRecordsForTodo(todoId)
    suspend fun getAllHabitRecordsOnce() = dao.getAllHabitRecordsOnce()

    suspend fun getTopPendingTodos(limit: Int = 10) = dao.getTopPendingTodos(limit)
}
