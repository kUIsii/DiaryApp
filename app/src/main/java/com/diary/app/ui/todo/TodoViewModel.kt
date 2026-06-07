package com.diary.app.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.HabitRecord
import com.diary.app.data.Tag
import com.diary.app.data.TodoItem
import com.diary.app.reminder.TodoReminderManager
import com.diary.app.widget.TodoWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class HabitDayState(
    val date: LocalDate,
    val record: HabitRecord? = null,
    val isToday: Boolean = false
)

data class HabitItemUiState(
    val habit: TodoItem,
    val todayRecord: HabitRecord?,
    val streak: Int,
    val recentDays: List<HabitDayState>
)

data class HabitSummaryUiState(
    val total: Int = 0,
    val recordedToday: Int = 0,
    val diaryToday: Int = 0,
    val manualToday: Int = 0,
    val detailToday: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val context = application.applicationContext

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    val allTodos: StateFlow<List<TodoItem>> = _searchQuery.flatMapLatest { query ->
        if (query.isNotBlank()) {
            dao.searchTodos(query)
        } else {
            _selectedCategory.flatMapLatest { category ->
                if (category != null) {
                    dao.getTodosByCategory(category)
                } else {
                    _selectedTag.flatMapLatest { tag ->
                        if (tag != null) dao.getTodosByTag(tag) else dao.getAllTodos()
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTodos: StateFlow<List<TodoItem>> = run {
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.getTodayTodos(dayStart, dayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val pendingTodoCount: StateFlow<Int> = dao.getPendingTodoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allTags: StateFlow<List<Tag>> = dao.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHabitId = MutableStateFlow<Long?>(null)
    val selectedHabitId: StateFlow<Long?> = _selectedHabitId.asStateFlow()

    private val _selectedHabitMonth = MutableStateFlow(YearMonth.now())
    val selectedHabitMonth: StateFlow<YearMonth> = _selectedHabitMonth.asStateFlow()

    private val _selectedHabitDate = MutableStateFlow(LocalDate.now())
    val selectedHabitDate: StateFlow<LocalDate> = _selectedHabitDate.asStateFlow()

    private val _showHabitDetail = MutableStateFlow(false)
    val showHabitDetail: StateFlow<Boolean> = _showHabitDetail.asStateFlow()

    private val _showHabitRecordDialog = MutableStateFlow(false)
    val showHabitRecordDialog: StateFlow<Boolean> = _showHabitRecordDialog.asStateFlow()

    val habitUiState: StateFlow<List<HabitItemUiState>> = allTodos
        .flatMapLatest { todos ->
            buildHabitUiStatesFlow(todos.filter { it.category == TodoItem.CATEGORY_GOAL })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitSummary: StateFlow<HabitSummaryUiState> = habitUiState
        .map(::buildHabitSummaryUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitSummaryUiState())

    val selectedHabit: StateFlow<TodoItem?> = combine(allTodos, _selectedHabitId) { todos, habitId ->
        todos.firstOrNull { it.id == habitId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedHabitRecords: StateFlow<List<HabitRecord>> = selectedHabit
        .flatMapLatest { habit ->
            if (habit == null) flowOf(emptyList()) else dao.getHabitRecords(habit.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        TodoReminderManager.createNotificationChannel(context)
        TodoReminderManager.rescheduleAllPendingReminders(context)
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
            dao.deleteHabitRecordsForTodo(todo.id)
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

    fun addHabit(name: String, linkedTagIds: List<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insertTodo(
                TodoItem(
                    title = name.trim(),
                    category = TodoItem.CATEGORY_GOAL,
                    recurringType = TodoItem.RECURRING_DAILY,
                    linkedTagIds = TodoItem.setLinkedTagIds(linkedTagIds)
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
            val id = dao.insertTodo(TodoItem(title = content.trim(), dueDate = deadlineMillis))
            TodoReminderManager.scheduleReminder(context, id, content.trim(), deadlineMillis)
            refreshWidget()
        }
    }

    fun toggleHabitDay(habit: TodoItem, dayIndex: Int, weekStart: LocalDate? = null) {
        viewModelScope.launch {
            val baseDate = weekStart ?: LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
            val targetDate = baseDate.plusDays(dayIndex.toLong())
            val recordDate = targetDate.toEpochDay()
            val existing = dao.getHabitRecordForDay(habit.id, recordDate)
            if (existing == null) {
                dao.insertHabitRecord(
                    HabitRecord(
                        todoId = habit.id,
                        recordDate = recordDate,
                        source = HabitRecord.SOURCE_MANUAL,
                        summary = ""
                    )
                )
            } else if (existing.source != HabitRecord.SOURCE_DIARY) {
                dao.deleteHabitRecordForDay(habit.id, recordDate)
            }
            refreshWidget()
        }
    }

    fun openHabitDetail(habitId: Long, initialDate: LocalDate = LocalDate.now()) {
        _selectedHabitId.value = habitId
        _selectedHabitDate.value = initialDate
        _selectedHabitMonth.value = YearMonth.from(initialDate)
        _showHabitDetail.value = true
    }

    fun closeHabitDetail() {
        _showHabitDetail.value = false
    }

    fun showHabitRecordDialog(habitId: Long, date: LocalDate = LocalDate.now()) {
        _selectedHabitId.value = habitId
        _selectedHabitDate.value = date
        _selectedHabitMonth.value = YearMonth.from(date)
        _showHabitRecordDialog.value = true
    }

    fun hideHabitRecordDialog() {
        _showHabitRecordDialog.value = false
    }

    fun selectHabitDate(date: LocalDate) {
        _selectedHabitDate.value = date
        _selectedHabitMonth.value = YearMonth.from(date)
    }

    fun moveSelectedHabitMonth(delta: Long) {
        _selectedHabitMonth.value = _selectedHabitMonth.value.plusMonths(delta)
    }

    fun saveHabitQuickRecord(habitId: Long, date: LocalDate, summary: String, source: String) {
        if (summary.isBlank()) return
        viewModelScope.launch {
            val existing = dao.getHabitRecordForDay(habitId, date.toEpochDay())
            val now = System.currentTimeMillis()
            val target = if (existing == null) {
                HabitRecord(
                    todoId = habitId,
                    recordDate = date.toEpochDay(),
                    source = source,
                    summary = summary.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                existing.copy(
                    source = if (existing.source == HabitRecord.SOURCE_DIARY) existing.source else source,
                    summary = summary.trim(),
                    updatedAt = now
                )
            }
            dao.insertHabitRecord(target)
            refreshWidget()
        }
    }

    fun clearHabitRecordForDay(habitId: Long, date: LocalDate) {
        viewModelScope.launch {
            val existing = dao.getHabitRecordForDay(habitId, date.toEpochDay()) ?: return@launch
            if (existing.source != HabitRecord.SOURCE_DIARY) {
                dao.deleteHabitRecordForDay(habitId, date.toEpochDay())
                refreshWidget()
            }
        }
    }

    suspend fun getTopTodosForWidget(limit: Int = 10): List<TodoItem> {
        return dao.getTopPendingTodos(limit)
    }

    fun autoCompleteHabitsForDiary(diaryTagIds: List<Long>, diaryEntryId: Long? = null) {
        if (diaryTagIds.isEmpty()) return
        viewModelScope.launch {
            val today = LocalDate.now()
            val allHabits = dao.getAllTodos().first().filter { it.category == TodoItem.CATEGORY_GOAL }
            val todayPreview = diaryEntryId?.let { dao.getPreviewById(it) }

            allHabits.forEach { habit ->
                val habitLinkedTagIds = TodoItem.getLinkedTagIds(habit.linkedTagIds)
                if (habitLinkedTagIds.isNotEmpty() && habitLinkedTagIds.any { it in diaryTagIds }) {
                    val existing = dao.getHabitRecordForDay(habit.id, today.toEpochDay())
                    val now = System.currentTimeMillis()
                    val record = (existing ?: HabitRecord(
                        todoId = habit.id,
                        recordDate = today.toEpochDay(),
                        createdAt = now,
                        updatedAt = now
                    )).copy(
                        source = HabitRecord.SOURCE_DIARY,
                        summary = buildDiaryHabitSummary(todayPreview),
                        diaryEntryId = diaryEntryId,
                        updatedAt = now
                    )
                    dao.insertHabitRecord(record)
                }
            }
            refreshWidget()
        }
    }

    private fun buildHabitUiStatesFlow(habits: List<TodoItem>): Flow<List<HabitItemUiState>> = flow {
        if (habits.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val today = LocalDate.now()
        val records = dao.getHabitRecordsInRange(
            todoIds = habits.map { it.id },
            startDate = today.minusDays(45).toEpochDay(),
            endDate = today.toEpochDay()
        )
        val grouped = records.groupBy { it.todoId }
        emit(
            habits.map { habit ->
                buildHabitItemUiState(
                    habit = habit,
                    records = grouped[habit.id].orEmpty(),
                    today = today
                )
            }
        )
    }
}
