package com.diary.app.ui.travellog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// PullToRefresh not available in this Compose BOM
import androidx.compose.material3.rememberDatePickerState
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelLogScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: TravelLogViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.showDetail && state.selectedTrip != null) {
                TripDetailView(
                    state = state,
                    viewModel = viewModel,
                    onBack = { viewModel.hideTripDetail() }
                )
            } else {
                TripListView(
                    state = state,
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }

    val context = LocalContext.current
    if (state.message.isNotEmpty()) {
        LaunchedEffect(state.message) {
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    if (state.showCreateDialog) {
        CreateTripDialog(
            availableEntries = state.availableEntries,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, dest, start, end, desc, ids ->
                viewModel.createTrip(name, dest, start, end, desc, ids)
            }
        )
    }

    if (state.showEditDialog && state.editingTrip != null) {
        EditTripDialog(
            trip = state.editingTrip!!,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { name, dest, start, end, desc ->
                viewModel.editTrip(state.editingTrip!!.id, name, dest, start, end, desc)
            }
        )
    }

    if (state.showDeleteConfirm && state.deleteTargetTrip != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteTrip() },
            title = { Text("删除行程", fontWeight = FontWeight.Bold) },
            text = { Text("确定删除行程 ${state.deleteTargetTrip!!.name}？") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteTrip() }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteTrip() }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripListView(
    state: TravelLogUiState,
    viewModel: TravelLogViewModel,
    onNavigateBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = "旅行日志",
                onNavigateBack = onNavigateBack,
                action = {
                    Row {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        IconButton(onClick = { viewModel.showCreateDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "创建行程")
                        }
                    }
                }
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (state.trips.isEmpty()) {
                        item {
                            EmptyTripsState()
                        }
                    } else {
                        item {
                            Text(
                                text = if (state.ungroupedEntries.isNotEmpty())
                                    "共 ${state.trips.size} 次行程，${state.ungroupedEntries.size} 篇未归类"
                                else
                                    "共 ${state.trips.size} 次行程",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(state.trips, key = { it.id }) { trip ->
                            TripCard(
                                trip = trip,
                                entries = state.groupedEntries.filter { e -> e.id in trip.entryIds },
                                onClick = { viewModel.showTripDetail(trip) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTripsState() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 32.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "还没有旅行记录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "给日记添加位置信息，或手动创建行程",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    entries: List<TripEntry>,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("M/d", Locale.getDefault()) }
    val moods = entries.mapNotNull { it.moodLevel }
    val avgMood = if (moods.isNotEmpty()) moods.sum().toFloat() / moods.size else 0f
    val moodText = when {
        avgMood >= 4.5f -> "愉快"
        avgMood >= 3.0f -> "平静"
        avgMood > 0f -> "低沉"
        else -> ""
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Flight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    trip.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        trip.destination,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${trip.entryIds.size} 篇",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        dateFormat.format(Date(trip.startDate)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (moodText.isNotEmpty()) {
                    Text(
                        moodText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TripDetailView(
    state: TravelLogUiState,
    viewModel: TravelLogViewModel,
    onBack: () -> Unit
) {
    val trip = state.selectedTrip ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = trip.name,
            onNavigateBack = onBack,
            action = {
                Row {
                    if (trip.isManual) {
                        IconButton(onClick = { viewModel.showEditDialog(trip) }) {
                            Icon(Icons.Default.EditNote, contentDescription = "编辑行程")
                        }
                    }
                    IconButton(onClick = { viewModel.requestDeleteTrip(trip) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除行程")
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                TripStatsHeader(
                    trip = trip,
                    stats = state.tripStats
                )
            }

            if (state.tripEntries.isNotEmpty()) {
                item {
                    Text(
                        "日记时间线",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(state.tripEntries, key = { it.id }) { entry ->
                    TripEntryItem(
                        entry = entry,
                        onRemove = if (trip.isManual) {
                            { viewModel.removeEntryFromTrip(trip.id, entry.id) }
                        } else null,
                        viewModel = viewModel
                    )
                }
            }

            item {
                AiSummarySection(
                    summary = state.aiSummary,
                    isGenerating = state.isGeneratingSummary,
                    onGenerate = { viewModel.generateSummary() }
                )
            }

            if (state.suggestedEntries.isNotEmpty() && trip.isManual) {
                item {
                    SuggestedEntriesSection(
                        entries = state.suggestedEntries,
                        tripId = trip.id,
                        viewModel = viewModel
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TripStatsHeader(
    trip: Trip,
    stats: TripStats
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/M/d", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    trip.destination,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${dateFormat.format(Date(trip.startDate))} - ${dateFormat.format(Date(trip.endDate))}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trip.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    trip.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.MenuBook,
                    label = "日记",
                    value = "${stats.entryCount} 篇"
                )
                StatItem(
                    icon = Icons.Default.Timeline,
                    label = "字数",
                    value = if (stats.totalWordCount >= 1000) "${stats.totalWordCount / 1000}k" else "${stats.totalWordCount}"
                )
                StatItem(
                    icon = Icons.Default.Schedule,
                    label = "日期跨度",
                    value = stats.dateRange
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TripEntryItem(
    entry: TripEntry,
    onRemove: (() -> Unit)?,
    viewModel: TravelLogViewModel
) {
    val dateFormat = remember { SimpleDateFormat("M/d HH:mm", Locale.getDefault()) }
    val moodLabel = entry.moodLevel?.let { viewModel.moodLabel(it) } ?: ""

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        innerPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        dateFormat.format(Date(entry.createdAt)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (moodLabel.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            moodLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "移出行程",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSummarySection(
    summary: String,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI 旅行总结",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (summary.isNotBlank()) {
                Text(
                    summary,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 21.sp
                )
            }

            if (isGenerating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "正在生成总结...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (summary.isBlank() && !isGenerating) {
                OutlinedButton(
                    onClick = onGenerate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("生成总结")
                }
            }
        }
    }
}

@Composable
private fun SuggestedEntriesSection(
    entries: List<TripEntry>,
    tripId: Long,
    viewModel: TravelLogViewModel
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "推荐添加的日记",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val selectedIds = remember { mutableStateOf(setOf<Long>()) }

            for (entry in entries) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIds.value = if (entry.id in selectedIds.value) {
                                selectedIds.value - entry.id
                            } else {
                                selectedIds.value + entry.id
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = entry.id in selectedIds.value,
                        onCheckedChange = {
                            selectedIds.value = if (entry.id in selectedIds.value) {
                                selectedIds.value - entry.id
                            } else {
                                selectedIds.value + entry.id
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.title,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.location != null) {
                            Text(
                                entry.location,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedIds.value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addEntriesToTrip(tripId, selectedIds.value.toList())
                        selectedIds.value = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加选中的 ${selectedIds.value.size} 篇日记")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTripDialog(
    trip: Trip,
    onDismiss: () -> Unit,
    onSave: (name: String, destination: String, startDate: Long, endDate: Long, description: String) -> Unit
) {
    var name by remember { mutableStateOf(trip.name) }
    var destination by remember { mutableStateOf(trip.destination) }
    var description by remember { mutableStateOf(trip.description) }
    var startDate by remember { mutableStateOf(trip.startDate) }
    var endDate by remember { mutableStateOf(trip.endDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/M/d", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("编辑行程", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("行程名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("目的地") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dateFormat.format(Date(startDate)), fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dateFormat.format(Date(endDate)), fontSize = 13.sp)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("行程描述（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && destination.isNotBlank()) {
                        onSave(name, destination, startDate, endDate, description)
                    }
                },
                enabled = name.isNotBlank() && destination.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showStartPicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = it }
                    showEndPicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripDialog(
    availableEntries: List<TripEntry>,
    onDismiss: () -> Unit,
    onCreate: (name: String, destination: String, startDate: Long, endDate: Long, description: String, entryIds: List<Long>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy/M/d", Locale.getDefault()) }
    val selectedIds = remember { mutableStateOf(setOf<Long>()) }

    val filterStartDate = remember(availableEntries) { mutableStateOf<Long?>(null) }
    val filterEndDate = remember(availableEntries) { mutableStateOf<Long?>(null) }

    val filteredEntries = remember(availableEntries, filterStartDate.value, filterEndDate.value) {
        val fsd = filterStartDate.value
        val fed = filterEndDate.value
        availableEntries.filter { entry ->
            val matchesStart = fsd == null || entry.createdAt >= fsd
            val matchesEnd = fed == null || entry.createdAt <= fed
            matchesStart && matchesEnd
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("创建行程", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("行程名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("目的地") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dateFormat.format(Date(startDate)), fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dateFormat.format(Date(endDate)), fontSize = 13.sp)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("行程描述（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                if (availableEntries.isNotEmpty()) {
                    item {
                        Divider()
                        Text(
                            "选择日记",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    filterStartDate.value = null
                                    filterEndDate.value = null
                                    selectedIds.value = emptySet()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("清除筛选", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    selectedIds.value = filteredEntries.map { it.id }.toSet()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("全选", fontSize = 12.sp)
                            }
                        }
                    }

                    items(filteredEntries, key = { it.id }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds.value = if (entry.id in selectedIds.value) {
                                        selectedIds.value - entry.id
                                    } else {
                                        selectedIds.value + entry.id
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = entry.id in selectedIds.value,
                                onCheckedChange = {
                                    selectedIds.value = if (entry.id in selectedIds.value) {
                                        selectedIds.value - entry.id
                                    } else {
                                        selectedIds.value + entry.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (entry.location != null) {
                                    Text(
                                        entry.location,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && destination.isNotBlank()) {
                        onCreate(name, destination, startDate, endDate, description, selectedIds.value.toList())
                    }
                },
                enabled = name.isNotBlank() && destination.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showStartPicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = it }
                    showEndPicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


