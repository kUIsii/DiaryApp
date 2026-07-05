package com.diary.app.ui.todo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.diary.app.data.sync.CloudSyncManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.data.HabitRecord
import com.diary.app.data.Tag
import com.diary.app.data.TodoItem
import com.diary.app.ui.experimental.orderMemoItemsForDisplay
import com.diary.app.ui.experimental.orderTodoItemsForDisplay
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.SuccessColor
import com.diary.app.ui.theme.WarningColor
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TodoTab(val label: String) { HABIT("打卡"), MEMO("备忘"), DEADLINE("待办") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = viewModel(),
    onMainScreenSwipe: ((Float) -> Unit)? = null
) {
    val app = LocalContext.current.applicationContext as? DiaryApplication ?: return
    val haptic = rememberHapticFeedback()
    val experimentalFeatures by app.experimentalFeatures.collectAsState()
    val allTodos by viewModel.allTodos.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val habitUiState by viewModel.habitUiState.collectAsState()
    val habitSummary by viewModel.habitSummary.collectAsState()
    val selectedHabit by viewModel.selectedHabit.collectAsState()
    val selectedHabitRecords by viewModel.selectedHabitRecords.collectAsState()
    val selectedHabitMonth by viewModel.selectedHabitMonth.collectAsState()
    val selectedHabitDate by viewModel.selectedHabitDate.collectAsState()
    val showHabitDetail by viewModel.showHabitDetail.collectAsState()
    val showHabitRecordDialog by viewModel.showHabitRecordDialog.collectAsState()
    val todayThree by viewModel.todayThree.collectAsState()

    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val todoContext = LocalContext.current
    val cloudSyncManager = remember { CloudSyncManager(todoContext) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("桌面同步就绪") }
    var showAuthDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

    fun pushToCloud() {
        if (!cloudSyncManager.isAuthenticated) {
            showAuthDialog = true
            return
        }
        isSyncing = true
        scope.launch {
            val gson = com.google.gson.Gson()
            @Suppress("UNCHECKED_CAST")
            val payload = gson.fromJson(viewModel.buildDesktopSyncPayloadJson(), Map::class.java) as Map<String, Any>
            cloudSyncManager.pushBackup(payload).fold(
                onSuccess = { syncStatus = "云端同步成功" },
                onFailure = { syncStatus = "同步失败: ${it.message}" }
            )
            isSyncing = false
        }
    }

    val currentTab = TodoTab.entries[currentPageIndex]
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val habitItems = allTodos.filter { it.category == TodoItem.CATEGORY_GOAL }
    val memoItems = orderMemoItemsForDisplay(
        items = allTodos.filter { it.category != TodoItem.CATEGORY_GOAL && it.dueDate == null },
        keepCompletedInPlace = experimentalFeatures.keepCompletedItemsInPlace
    )
    val deadlineItems = orderTodoItemsForDisplay(
        items = allTodos.filter { it.dueDate != null }.sortedBy { it.dueDate ?: Long.MAX_VALUE },
        keepCompletedInPlace = experimentalFeatures.keepCompletedItemsInPlace
    )

    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var editingHabit by remember { mutableStateOf<TodoItem?>(null) }
    var deletingTodo by remember { mutableStateOf<TodoItem?>(null) }

    TodoDialogs(
        allTags = allTags,
        currentTabLabel = currentTab.label,
        textColor = textColor,
        textSecondary = textSecondary,
        deletingTodo = deletingTodo,
        editingTodo = editingTodo,
        editingHabit = editingHabit,
        showAddDialog = showAddDialog,
        isMultiSelectMode = isMultiSelectMode,
        selectedIds = selectedIds,
        selectedHabit = selectedHabit,
        selectedHabitRecords = selectedHabitRecords,
        selectedHabitMonth = selectedHabitMonth,
        selectedHabitDate = selectedHabitDate,
        showHabitDetail = showHabitDetail,
        showHabitRecordDialog = showHabitRecordDialog,
        onDismissDelete = { deletingTodo = null },
        onConfirmDelete = { target ->
            haptic.warning()
            viewModel.deleteTodo(target)
            deletingTodo = null
        },
        onDismissMultiDelete = {
            isMultiSelectMode = false
            selectedIds = emptySet()
        },
        onConfirmMultiDelete = {
            haptic.warning()
            selectedIds.forEach { id ->
                allTodos.find { it.id == id }?.let { viewModel.deleteTodo(it) }
            }
            isMultiSelectMode = false
            selectedIds = emptySet()
        },
        onDismissEditTodo = { editingTodo = null },
        onSaveEditTodo = { updatedTodo ->
            viewModel.updateTodo(updatedTodo)
            editingTodo = null
        },
        onDismissEditHabit = { editingHabit = null },
        onSaveEditHabit = { updatedHabit, linkedTagIds ->
            viewModel.updateHabit(
                habitId = updatedHabit.id,
                name = updatedHabit.title,
                linkedTagIds = linkedTagIds
            )
            editingHabit = null
        },
        onDismissAddDialog = { showAddDialog = false },
        onAddHabit = { name, linkedTagIds ->
            viewModel.addHabit(name, linkedTagIds)
            showAddDialog = false
        },
        onAddMemo = { content ->
            viewModel.addMemo(content)
            showAddDialog = false
        },
        onAddDeadline = { content, deadline ->
            viewModel.addDeadline(content, deadline)
            showAddDialog = false
        },
        onDismissHabitDetail = { viewModel.closeHabitDetail() },
        onEditSelectedHabit = { editingHabit = selectedHabit },
        onChangeHabitMonth = { delta -> viewModel.moveSelectedHabitMonth(delta) },
        onSelectHabitDate = { date -> viewModel.selectHabitDate(date) },
        onSaveHabitQuickRecord = { summaryText ->
            selectedHabit?.let { habit ->
                viewModel.saveHabitQuickRecord(
                    habitId = habit.id,
                    date = selectedHabitDate,
                    summary = summaryText,
                    source = HabitRecord.SOURCE_MANUAL
                )
            }
        },
        onOpenHabitRecordDialog = {
            selectedHabit?.let { habit ->
                viewModel.showHabitRecordDialog(habit.id, selectedHabitDate)
            }
        },
        onClearHabitRecord = {
            selectedHabit?.let { habit ->
                viewModel.clearHabitRecordForDay(habit.id, selectedHabitDate)
            }
        },
        onDismissHabitRecordDialog = { viewModel.hideHabitRecordDialog() },
        onSaveHabitDetailRecord = { summaryText ->
            selectedHabit?.let { habit ->
                viewModel.saveHabitQuickRecord(
                    habitId = habit.id,
                    date = selectedHabitDate,
                    summary = summaryText,
                    source = HabitRecord.SOURCE_DETAIL
                )
                viewModel.hideHabitRecordDialog()
            }
        }
    )

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("云同步绑定") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("绑定手机号后可推送到云端", fontSize = 12.sp, color = textSecondary)
                    TextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("手机号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN (至少4位)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (phoneInput.isNotBlank() && pinInput.length >= 4) {
                            isSyncing = true
                            scope.launch(Dispatchers.IO) {
                                cloudSyncManager.login(phoneInput, pinInput).fold(
                                    onSuccess = {
                                        val gson = com.google.gson.Gson()
                                        @Suppress("UNCHECKED_CAST")
                                        val payload = gson.fromJson(viewModel.buildDesktopSyncPayloadJson(), Map::class.java) as Map<String, Any>
                                        cloudSyncManager.pushBackup(payload).fold(
                                            onSuccess = { syncStatus = "绑定成功，已同步到云端" },
                                            onFailure = { syncStatus = "同步失败: ${it.message}" }
                                        )
                                    },
                                    onFailure = { syncStatus = "绑定失败: ${it.message}" }
                                )
                                isSyncing = false
                            }
                        }
                        showAuthDialog = false
                    },
                    enabled = phoneInput.isNotBlank() && pinInput.length >= 4 && !isSyncing
                ) {
                    if (isSyncing) Text("绑定中...") else Text("绑定并同步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) { Text("取消") }
            }
        )
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            val tabBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TodoTab.entries.forEachIndexed { idx, tab ->
                    val isActive = currentTab == tab
                    val count = when (tab) {
                        TodoTab.HABIT -> habitItems.size
                        TodoTab.MEMO -> memoItems.count { !it.isCompleted }
                        TodoTab.DEADLINE -> deadlineItems.count { !it.isCompleted }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (!isActive) Modifier.border(1.dp, tabBorderColor, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                currentPageIndex = idx
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab.label,
                                fontSize = 15.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary else textSecondary
                            )
                            if (count > 0) {
                                Text(
                                    text = "$count 项",
                                    fontSize = 11.sp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .pointerInput(currentPageIndex) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    totalDrag <= -48f && currentPageIndex < TodoTab.entries.lastIndex -> {
                                        currentPageIndex += 1
                                    }
                                    totalDrag >= 48f && currentPageIndex > 0 -> {
                                        currentPageIndex -= 1
                                    }
                                    totalDrag <= -48f && currentPageIndex == TodoTab.entries.lastIndex -> {
                                        onMainScreenSwipe?.invoke(totalDrag)
                                    }
                                    totalDrag >= 48f && currentPageIndex == 0 -> {
                                        onMainScreenSwipe?.invoke(totalDrag)
                                    }
                                }
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = currentPageIndex,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val enter = slideInHorizontally(
                            animationSpec = tween(durationMillis = 210, easing = LinearOutSlowInEasing),
                            initialOffsetX = { fullWidth ->
                                if (forward) fullWidth / 10 else -fullWidth / 10
                            }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 190, easing = LinearOutSlowInEasing),
                            initialAlpha = 0.78f
                        )
                        val exit = slideOutHorizontally(
                            animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing),
                            targetOffsetX = { fullWidth ->
                                if (forward) -fullWidth / 24 else fullWidth / 24
                            }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing)
                        )
                        enter togetherWith exit
                    },
                    label = "todo_tab_cutover"
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (TodoTab.entries[page]) {
                            TodoTab.HABIT -> HabitTab(
                                habits = habitUiState,
                                summary = habitSummary,
                                viewModel = viewModel,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onAdd = { showAddDialog = true },
                                onDeleteRequest = { deletingTodo = it }
                            )
                            TodoTab.MEMO -> MemoTab(
                                items = memoItems,
                                viewModel = viewModel,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onAdd = { showAddDialog = true },
                                onDeleteRequest = { deletingTodo = it },
                                onEdit = { editingTodo = it },
                                isMultiSelectMode = isMultiSelectMode,
                                selectedIds = selectedIds,
                                onToggleSelection = { id ->
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                },
                                onMultiSelectModeChange = { mode ->
                                    isMultiSelectMode = mode
                                    if (!mode) selectedIds = emptySet()
                                }
                            )
                            TodoTab.DEADLINE -> DeadlineTab(
                                items = deadlineItems,
                                todayThree = todayThree,
                                viewModel = viewModel,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onAdd = { showAddDialog = true },
                                syncStatus = syncStatus,
                                isSyncing = isSyncing,
                                onCapture = { text ->
                                    viewModel.captureMobileTasks(text)
                                    syncStatus = "已写入快速捕获"
                                },
                                onCopySyncPayload = {
                                    clipboard.setText(AnnotatedString(viewModel.buildDesktopSyncPayloadJson()))
                                    syncStatus = "已复制桌面同步 JSON"
                                },
                                onPushSync = { pushToCloud() },
                                onDeleteRequest = { deletingTodo = it },
                                onEdit = { editingTodo = it },
                                isMultiSelectMode = isMultiSelectMode,
                                selectedIds = selectedIds,
                                onToggleSelection = { id ->
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                },
                                onMultiSelectModeChange = { mode ->
                                    isMultiSelectMode = mode
                                    if (!mode) selectedIds = emptySet()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun HabitTab(
    habits: List<HabitItemUiState>,
    summary: HabitSummaryUiState,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit
) {
    if (habits.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Repeat,
            title = "还没有打卡项",
            subtitle = "添加一个简单的每日项目，之后写一句话或用日记自动打卡",
            modifier = Modifier.fillMaxSize()
        ) {
            AddButton(onClick = onAdd)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HabitSummaryCard(summary = summary, textColor = textColor, textSecondary = textSecondary)
        }

        items(habits, key = { it.habit.id }) { item ->
            HabitCard(
                item = item,
                textColor = textColor,
                textSecondary = textSecondary,
                onOpen = { viewModel.openHabitDetail(item.habit.id) },
                onQuickCheckIn = {
                    if (item.todayRecord == null) {
                        viewModel.showHabitRecordDialog(item.habit.id)
                    } else {
                        viewModel.openHabitDetail(item.habit.id)
                    }
                },
                onLongPress = { onDeleteRequest(item.habit) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            AddButton(onClick = onAdd)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HabitSummaryCard(
    summary: HabitSummaryUiState,
    textColor: Color,
    textSecondary: Color
) {
    val progress = if (summary.total == 0) 0f else summary.recordedToday.toFloat() / summary.total.toFloat()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${summary.recordedToday}/${summary.total}",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = "今日打卡",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "今天完成 ${summary.recordedToday} 项",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = when {
                        summary.recordedToday == 0 -> "今天还没有打卡"
                        summary.recordedToday == summary.total -> "今天已经全部完成"
                        else -> "还差 ${summary.total - summary.recordedToday} 项"
                    },
                    fontSize = 12.sp,
                    color = textSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    item: HabitItemUiState,
    textColor: Color,
    textSecondary: Color,
    onOpen: () -> Unit,
    onQuickCheckIn: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val statusText = item.todayRecord?.let { HabitRecord.sourceLabel(it.source) } ?: "未打卡"
    val summaryText = item.todayRecord?.summary?.takeIf { it.isNotBlank() } ?: "今天还没有记录"
    val statusColor = when (item.todayRecord?.source) {
        HabitRecord.SOURCE_DIARY -> MaterialTheme.colorScheme.primary
        HabitRecord.SOURCE_DETAIL -> SuccessColor
        HabitRecord.SOURCE_MANUAL -> MaterialTheme.colorScheme.secondary
        else -> textSecondary
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        cornerRadius = 16.dp,
        innerPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.habit.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (item.todayRecord != null) statusColor.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                                .border(
                                    width = if (item.todayRecord != null) 1.dp else 0.dp,
                                    color = if (item.todayRecord != null) statusColor.copy(alpha = 0.35f) else Color.Transparent,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (item.todayRecord != null) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Text(
                                    text = if (item.todayRecord != null) "已打卡" else statusText,
                                    fontSize = 10.sp,
                                    fontWeight = if (item.todayRecord != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (item.todayRecord != null) statusColor else textSecondary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = if (item.streak > 0) "${item.streak} 天" else "未开始",
                    fontSize = 10.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summaryText,
                    fontSize = 11.sp,
                    color = if (item.todayRecord == null) textSecondary.copy(alpha = 0.9f) else textColor.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextButton(onClick = onQuickCheckIn) {
                    Text(if (item.todayRecord == null) "记录" else "详情", fontSize = 12.sp)
                }
            }

            HabitRecentStrip(days = item.recentDays, textSecondary = textSecondary)
        }
    }
}

@Composable
private fun HabitRecentStrip(
    days: List<HabitDayState>,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            val tint = when (day.record?.source) {
                HabitRecord.SOURCE_DIARY -> MaterialTheme.colorScheme.primary
                HabitRecord.SOURCE_DETAIL -> SuccessColor
                HabitRecord.SOURCE_MANUAL -> MaterialTheme.colorScheme.secondary
                else -> textSecondary.copy(alpha = 0.36f)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(if (day.isToday) 12.dp else 10.dp))
                        .background(
                            when {
                                day.record != null -> tint.copy(alpha = if (day.isToday) 0.2f else 0.12f)
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            }
                        )
                        .border(
                            width = if (day.isToday) 1.dp else 0.dp,
                            color = if (day.isToday) tint.copy(alpha = 0.35f) else Color.Transparent,
                            shape = RoundedCornerShape(if (day.isToday) 12.dp else 10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        fontSize = 12.sp,
                        fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (day.record != null) tint else textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (day.isToday) "今" else day.date.format(DateTimeFormatter.ofPattern("E")),
                    fontSize = 9.sp,
                    color = textSecondary.copy(alpha = 0.88f),
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun MemoTab(
    items: List<TodoItem>,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit,
    onEdit: (TodoItem) -> Unit,
    isMultiSelectMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onMultiSelectModeChange: (Boolean) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Check,
            title = "还没有备忘",
            subtitle = "随手记下怕忘的小事",
            modifier = Modifier.fillMaxSize()
        ) {
            AddButton(onClick = onAdd)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(items, key = { it.id }) { item ->
                MemoItem(
                    item = item,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onToggle = { viewModel.toggleTodo(item) },
                    onDelete = { onDeleteRequest(item) },
                    onEdit = { onEdit(item) },
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = item.id in selectedIds,
                    onToggleSelection = { onToggleSelection(item.id) },
                    onLongPress = {
                        if (!isMultiSelectMode) {
                            onMultiSelectModeChange(true)
                            onToggleSelection(item.id)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AddButton(onClick = onAdd)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoItem(
    item: TodoItem,
    textColor: Color,
    textSecondary: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) onToggleSelection() else onEdit()
                },
                onLongClick = onLongPress
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable { if (isMultiSelectMode) onToggleSelection() else onToggle() }
                .background(
                    when {
                        isMultiSelectMode && isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        item.isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = 1.5.dp,
                    color = when {
                        isMultiSelectMode && isSelected -> MaterialTheme.colorScheme.primary
                        item.isCompleted -> MaterialTheme.colorScheme.primary
                        else -> textSecondary.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if ((isMultiSelectMode && isSelected) || item.isCompleted) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.title,
            fontSize = 15.sp,
            color = if (item.isCompleted) textSecondary.copy(alpha = 0.6f) else textColor,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!isMultiSelectMode) {
            Icon(
                Icons.Default.Edit,
                "编辑",
                tint = textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp).clickable(onClick = onEdit)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                "删除",
                tint = textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp).clickable(onClick = onDelete)
            )
        }
    }
}

@Composable
private fun DeadlineTab(
    items: List<TodoItem>,
    todayThree: List<TodayThreeItem>,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    syncStatus: String,
    isSyncing: Boolean,
    onCapture: (String) -> Unit,
    onCopySyncPayload: () -> Unit,
    onPushSync: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit,
    onEdit: (TodoItem) -> Unit,
    isMultiSelectMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onMultiSelectModeChange: (Boolean) -> Unit
) {
    val today = remember { LocalDate.now() }
    var captureText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            TodoAssistantPanel(
                todayThree = todayThree,
                captureText = captureText,
                onCaptureTextChange = { captureText = it },
                syncStatus = syncStatus,
                isSyncing = isSyncing,
                textColor = textColor,
                textSecondary = textSecondary,
                onCapture = {
                    onCapture(captureText)
                    captureText = ""
                },
                onCopySyncPayload = onCopySyncPayload,
                onPushSync = onPushSync
            )
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = "还没有待办",
                    subtitle = "添加带截止日期的事项，或用上方快速捕获一次写入多条",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AddButton(onClick = onAdd)
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                DeadlineItem(
                    item = item,
                    today = today,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onToggle = { viewModel.toggleTodo(item) },
                    onDelete = { onDeleteRequest(item) },
                    onEdit = { onEdit(item) },
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = item.id in selectedIds,
                    onToggleSelection = { onToggleSelection(item.id) },
                    onLongPress = {
                        if (!isMultiSelectMode) {
                            onMultiSelectModeChange(true)
                            onToggleSelection(item.id)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            AddButton(onClick = onAdd)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TodoAssistantPanel(
    todayThree: List<TodayThreeItem>,
    captureText: String,
    onCaptureTextChange: (String) -> Unit,
    syncStatus: String,
    isSyncing: Boolean,
    textColor: Color,
    textSecondary: Color,
    onCapture: () -> Unit,
    onCopySyncPayload: () -> Unit,
    onPushSync: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今日三件事",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = if (todayThree.isEmpty()) "AI 会从待办里挑出今天最该推进的 3 项" else "按截止、置顶和优先级自动排序",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onPushSync, enabled = !isSyncing) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("推送同步", fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = onCopySyncPayload) {
                        Text("复制", fontSize = 12.sp)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                todayThree.take(3).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.task.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.reason,
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (todayThree.isEmpty()) {
                    Text(
                        text = "待办足够后，这里会变成你的手机端执行清单。",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            TextField(
                value = captureText,
                onValueChange = onCaptureTextChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text("!! 今天 18:30 完成桌面端同步 #desktop\n! 明天 整理发布清单 #release") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = syncStatus,
                    fontSize = 11.sp,
                    color = textSecondary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onCapture,
                    enabled = captureText.isNotBlank()
                ) {
                    Text("快速捕获")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeadlineItem(
    item: TodoItem,
    today: LocalDate,
    textColor: Color,
    textSecondary: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit
) {
    val dueDate = item.dueDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val daysLeft = dueDate?.let { it.toEpochDay() - today.toEpochDay() } ?: 0
    val isOverdue = daysLeft < 0 && !item.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { if (isMultiSelectMode) onToggleSelection() else onToggle() },
                onLongClick = onLongPress
            )
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    isOverdue -> ErrorColor.copy(alpha = 0.06f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (!item.isCompleted && dueDate != null && !isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isOverdue -> ErrorColor.copy(alpha = 0.15f)
                            daysLeft <= 1 -> WarningColor.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isOverdue -> "过期"
                        daysLeft == 0L -> "今天"
                        daysLeft == 1L -> "明天"
                        else -> "${daysLeft}天"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isOverdue -> ErrorColor
                        daysLeft <= 1 -> WarningColor
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = item.title,
            fontSize = 15.sp,
            color = if (item.isCompleted) textSecondary.copy(alpha = 0.6f) else textColor,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (dueDate != null && !isMultiSelectMode) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${dueDate.monthValue}/${dueDate.dayOfMonth}",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.65f)
            )
        }

        if (!isMultiSelectMode) {
            Icon(
                Icons.Default.Edit,
                "编辑",
                tint = textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp).clickable(onClick = onEdit)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                "删除",
                tint = textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp).clickable(onClick = onDelete)
            )
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Add,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "添加",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
