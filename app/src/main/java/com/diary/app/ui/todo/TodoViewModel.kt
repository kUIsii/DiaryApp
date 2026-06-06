package com.diary.app.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.TodoItem
import com.diary.app.reminder.TodoReminderManager
import com.diary.app.widget.TodoWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val context = application.applicationContext

    // Search and filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Main todo list - switches between search, category filter, tag filter, or all
    val allTodos: StateFlow<List<TodoItem>> = _searchQuery.flatMapLatest { query ->
        if (query.isNotBlank()) {
            dao.searchTodos(query)
        } else {
            _selectedCategory.flatMapLatest { category ->
                if (category != null) {
                    dao.getTodosByCategory(category)
                } else {
                    _selectedTag.flatMapLatest { tag ->
                        if (tag != null) {
                            dao.getTodosByTag(tag)
                        } else {
                            dao.getAllTodos()
                        }
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's todos for widget
    val todayTodos: StateFlow<List<TodoItem>> = run {
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.getTodayTodos(dayStart, dayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Pending todo count for widget
    val pendingTodoCount: StateFlow<Int> = dao.getPendingTodoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        TodoReminderManager.createNotificationChannel(context)
        TodoReminderManager.rescheduleAllPendingReminders(context)
        // Schedule daily summary at 8:00 AM
        TodoReminderManager.scheduleDailySummary(context, 8, 0)
    }

    private fun refreshWidget() {
        TodoWidgetProvider.updateAllWidgets(context)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
        _selectedTag.value = null
        _searchQuery.value = ""
    }

    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
        _selectedCategory.value = null
        _searchQuery.value = ""
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
    }

    fun addTodo(
        title: String,
        description: String = "",
        priority: Int = 0,
        dueDate: Long? = null,
        category: String = TodoItem.CATEGORY_TASK,
        reminderTime: Long? = null,
        tags: List<String> = emptyList(),
        parentId: Long? = null,
        recurringType: String = TodoItem.RECURRING_NONE
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = dao.insertTodo(
                TodoItem(
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    dueDate = dueDate,
                    category = category,
                    reminderTime = reminderTime,
                    tags = TodoItem.setTagList(tags),
                    parentId = parentId,
                    recurringType = recurringType
                )
            )
            reminderTime?.let { time ->
                if (time > System.currentTimeMillis()) {
                    TodoReminderManager.scheduleReminder(context, id, title.trim(), time)
                }
            }
            refreshWidget()
        }
    }

    fun addSubTodo(parentId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertTodo(
                TodoItem(
                    title = title.trim(),
                    parentId = parentId,
                    category = TodoItem.CATEGORY_TASK
                )
            )
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
            refreshWidget()
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
                // Handle recurring tasks
                if (todo.recurringType != TodoItem.RECURRING_NONE) {
                    createRecurringCopy(todo)
                }
            }
            refreshWidget()
        }
    }

    private suspend fun createRecurringCopy(original: TodoItem) {
        val nextDueDate = original.dueDate?.let { calculateNextDueDate(it, original.recurringType) }
        val nextReminderTime = original.reminderTime?.let { calculateNextDueDate(it, original.recurringType) }

        dao.insertTodo(
            original.copy(
                id = 0,
                isCompleted = false,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                dueDate = nextDueDate,
                reminderTime = nextReminderTime,
                progress = 0
            )
        )
    }

    private fun calculateNextDueDate(currentMillis: Long, recurringType: String): Long {
        val date = Instant.ofEpochMilli(currentMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val nextDate = when (recurringType) {
            TodoItem.RECURRING_DAILY -> date.plusDays(1)
            TodoItem.RECURRING_WEEKLY -> date.plusWeeks(1)
            TodoItem.RECURRING_MONTHLY -> date.plusMonths(1)
            TodoItem.RECURRING_YEARLY -> date.plusYears(1)
            else -> date.plusDays(1)
        }
        return nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun pinTodo(todo: TodoItem) {
        viewModelScope.launch {
            dao.pinTodo(todo.id, !todo.isPinned)
        }
    }

    fun updateProgress(todo: TodoItem, progress: Int) {
        viewModelScope.launch {
            dao.updateProgress(todo.id, progress.coerceIn(0, 100))
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            dao.deleteTodo(todo)
            dao.deleteSubTodos(todo.id)
            TodoReminderManager.cancelReminder(context, todo.id)
            refreshWidget()
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            dao.deleteCompletedTodosBefore(sevenDaysAgo)
            refreshWidget()
        }
    }

    fun getSubTodos(parentId: Long): StateFlow<List<TodoItem>> {
        return dao.getSubTodos(parentId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // ── New: Three-in-one methods ──

    fun addHabit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val weekField = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
            val weekNum = LocalDate.now().get(weekField.weekOfYear())
            dao.insertTodo(
                TodoItem(
                    title = name.trim(),
                    category = TodoItem.CATEGORY_GOAL,
                    description = "0,0,0,0,0,0,0",
                    tags = weekNum.toString(),
                    recurringType = TodoItem.RECURRING_WEEKLY
                )
            )
            refreshWidget()
        }
    }

    fun addMemo(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            dao.insertTodo(TodoItem(title = content.trim()))
            refreshWidget()
        }
    }

    fun addDeadline(content: String, deadlineMillis: Long) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val id = dao.insertTodo(
                TodoItem(title = content.trim(), dueDate = deadlineMillis)
            )
            TodoReminderManager.scheduleReminder(context, id, content.trim(), deadlineMillis)
            refreshWidget()
        }
    }

    fun toggleHabitDay(habit: TodoItem, dayIndex: Int, weekStart: LocalDate? = null) {
        viewModelScope.launch {
            val data = habit.description.split(",").map { it.trim() == "1" }.toMutableList()
            while (data.size < 7) data.add(false)
            if (dayIndex in 0..6) {
                data[dayIndex] = !data[dayIndex]
            }
            val weekField = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
            val weekNum = (weekStart ?: LocalDate.now()).get(weekField.weekOfYear())
            dao.updateTodo(
                habit.copy(
                    description = data.joinToString(",") { if (it) "1" else "0" },
                    tags = weekNum.toString()
                )
            )
            refreshWidget()
        }
    }

    // For widget - get top pending todos
    suspend fun getTopTodosForWidget(limit: Int = 10): List<TodoItem> {
        return dao.getTopPendingTodos(limit)
    }
}
