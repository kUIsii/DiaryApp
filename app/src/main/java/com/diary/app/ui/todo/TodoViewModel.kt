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

enum class HabitDialogSurface {
    NONE,
    DETAIL,
    RECORD
}

data class HabitDialogState(
    val selectedHabitId: Long? = null,
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val activeDialog: HabitDialogSurface = HabitDialogSurface.NONE,
    val returnToDetailAfterRecord: Boolean = false
)

internal fun openHabitDetailState(
    habitId: Long,
    initialDate: LocalDate = LocalDate.now()
): HabitDialogState {
    return HabitDialogState(
        selectedHabitId = habitId,
        selectedMonth = YearMonth.from(initialDate),
        selectedDate = initialDate,
        activeDialog = HabitDialogSurface.DETAIL
    )
}

internal fun openHabitRecordDialogState(
    current: HabitDialogState,
    habitId: Long,
    date: LocalDate = LocalDate.now()
): HabitDialogState {
    return HabitDialogState(
        selectedHabitId = habitId,
        selectedMonth = YearMonth.from(date),
        selectedDate = date,
        activeDialog = HabitDialogSurface.RECORD,
        returnToDetailAfterRecord = current.activeDialog == HabitDialogSurface.DETAIL
    )
}

internal fun dismissHabitDialogState(current: HabitDialogState): HabitDialogState {
    return if (current.activeDialog == HabitDialogSurface.RECORD && current.returnToDetailAfterRecord && current.selectedHabitId != null) {
        current.copy(
            activeDialog = HabitDialogSurface.DETAIL,
            returnToDetailAfterRecord = false
        )
    } else {
        current.copy(
            selectedHabitId = null,
            activeDialog = HabitDialogSurface.NONE,
            returnToDetailAfterRecord = false
        )
    }
}

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

    private val _habitDialogState = MutableStateFlow(HabitDialogState())
    val habitDialogState: StateFlow<HabitDialogState> = _habitDialogState.asStateFlow()
    val selectedHabitId: StateFlow<Long?> = habitDialogState
        .map { it.selectedHabitId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedHabitMonth: StateFlow<YearMonth> = habitDialogState
        .map { it.selectedMonth }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearMonth.now())

    val selectedHabitDate: StateFlow<LocalDate> = habitDialogState
        .map { it.selectedDate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now())

    val showHabitDetail: StateFlow<Boolean> = habitDialogState
        .map { it.activeDialog == HabitDialogSurface.DETAIL }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showHabitRecordDialog: StateFlow<Boolean> = habitDialogState
        .map { it.activeDialog == HabitDialogSurface.RECORD }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val habitUiState: StateFlow<List<HabitItemUiState>> = allTodos
        .flatMapLatest { todos ->
            buildHabitUiStatesFlow(todos.filter { it.category == TodoItem.CATEGORY_GOAL })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitSummary: StateFlow<HabitSummaryUiState> = habitUiState
        .map(::buildHabitSummaryUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitSummaryUiState())

    val selectedHabit: StateFlow<TodoItem?> = combine(allTodos, selectedHabitId) { todos, habitId ->
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
            applyTodoUpdate(todo)
        }
    }

    fun updateHabit(habitId: Long, name: String, linkedTagIds: List<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val existing = dao.getTodoById(habitId) ?: return@launch
            if (existing.category != TodoItem.CATEGORY_GOAL) return@launch

            applyTodoUpdate(
                existing.copy(
                    title = name.trim(),
                    linkedTagIds = TodoItem.setLinkedTagIds(linkedTagIds)
                )
            )
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

    private suspend fun applyTodoUpdate(todo: TodoItem) {
        dao.updateTodo(todo)
        if (todo.reminderTime != null && todo.reminderTime > System.currentTimeMillis() && !todo.isCompleted) {
            TodoReminderManager.scheduleReminder(context, todo.id, todo.title, todo.reminderTime)
        } else {
            TodoReminderManager.cancelReminder(context, todo.id)
        }
        refreshWidget()
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
        _habitDialogState.value = openHabitDetailState(habitId, initialDate)
    }

    fun closeHabitDetail() {
        _habitDialogState.value = dismissHabitDialogState(_habitDialogState.value)
    }

    fun showHabitRecordDialog(habitId: Long, date: LocalDate = LocalDate.now()) {
        _habitDialogState.value = openHabitRecordDialogState(
            current = _habitDialogState.value,
            habitId = habitId,
            date = date
        )
    }

    fun hideHabitRecordDialog() {
        _habitDialogState.value = dismissHabitDialogState(_habitDialogState.value)
    }

    fun selectHabitDate(date: LocalDate) {
        _habitDialogState.value = _habitDialogState.value.copy(
            selectedDate = date,
            selectedMonth = YearMonth.from(date)
        )
    }

    fun moveSelectedHabitMonth(delta: Long) {
        _habitDialogState.value = _habitDialogState.value.copy(
            selectedMonth = _habitDialogState.value.selectedMonth.plusMonths(delta)
        )
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

    private fun buildHabitUiStatesFlow(habits: List<TodoItem>): Flow<List<HabitItemUiState>> {
        if (habits.isEmpty()) return flowOf(emptyList())

        val today = LocalDate.now()
        return dao.getHabitRecordsInRangeFlow(
            todoIds = habits.map { it.id },
            startDate = today.minusDays(45).toEpochDay(),
            endDate = today.toEpochDay()
        ).map { records ->
            val grouped = records.groupBy { it.todoId }
            habits.map { habit ->
                buildHabitItemUiState(
                    habit = habit,
                    records = grouped[habit.id].orEmpty(),
                    today = today
                )
            }
        }
    }
}
