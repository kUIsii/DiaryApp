package com.diary.app.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.TodoItem
import com.diary.app.reminder.TodoReminderManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val context = application.applicationContext

    val allTodos: StateFlow<List<TodoItem>> = dao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        TodoReminderManager.createNotificationChannel(context)
        TodoReminderManager.rescheduleAllPendingReminders(context)
    }

    fun addTodo(title: String, priority: Int = 0, dueDate: Long? = null, category: String = TodoItem.CATEGORY_TASK, reminderTime: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = dao.insertTodo(
                TodoItem(
                    title = title.trim(),
                    priority = priority,
                    dueDate = dueDate,
                    category = category,
                    reminderTime = reminderTime
                )
            )
            reminderTime?.let { time ->
                if (time > System.currentTimeMillis()) {
                    TodoReminderManager.scheduleReminder(context, id, title.trim(), time)
                }
            }
        }
    }

    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch {
            dao.updateTodo(todo)
            // Update reminder
            if (todo.reminderTime != null && todo.reminderTime > System.currentTimeMillis() && !todo.isCompleted) {
                TodoReminderManager.scheduleReminder(context, todo.id, todo.title, todo.reminderTime)
            } else {
                TodoReminderManager.cancelReminder(context, todo.id)
            }
        }
    }

    fun toggleTodo(todo: TodoItem) {
        viewModelScope.launch {
            val nowCompleted = !todo.isCompleted
            dao.toggleTodo(
                id = todo.id,
                completed = nowCompleted,
                completedAt = if (nowCompleted) System.currentTimeMillis() else null
            )
            if (nowCompleted) {
                TodoReminderManager.cancelReminder(context, todo.id)
            }
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            dao.deleteTodo(todo)
            TodoReminderManager.cancelReminder(context, todo.id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            dao.deleteCompletedTodosBefore(sevenDaysAgo)
        }
    }
}
