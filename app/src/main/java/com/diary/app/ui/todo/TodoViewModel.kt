package com.diary.app.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val allTodos: StateFlow<List<TodoItem>> = dao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTodo(title: String, priority: Int = 0, dueDate: Long? = null, category: String = TodoItem.CATEGORY_TASK) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertTodo(
                TodoItem(
                    title = title.trim(),
                    priority = priority,
                    dueDate = dueDate,
                    category = category
                )
            )
        }
    }

    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch {
            dao.updateTodo(todo)
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
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            dao.deleteTodo(todo)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            dao.deleteCompletedTodosBefore(sevenDaysAgo)
        }
    }
}
