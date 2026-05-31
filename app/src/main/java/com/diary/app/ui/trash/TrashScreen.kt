package com.diary.app.ui.trash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.TrashEntry
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: TrashViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showEmptyDialog by remember { mutableStateOf(false) }
    var entryToRestore by remember { mutableStateOf<TrashEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<TrashEntry?>(null) }

    // Empty trash confirmation
    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("清空回收站") },
            text = { Text("确定要永久删除所有 ${entries.size} 条日记吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    viewModel.emptyTrash()
                    showEmptyDialog = false
                }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Restore confirmation
    if (entryToRestore != null) {
        AlertDialog(
            onDismissRequest = { entryToRestore = null },
            title = { Text("恢复日记") },
            text = { Text("确定要恢复这篇日记吗？") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.click()
                    entryToRestore?.let { viewModel.restoreEntry(it) }
                    entryToRestore = null
                }) {
                    Text("恢复", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToRestore = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete forever confirmation
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("永久删除") },
            text = { Text("确定要永久删除这篇日记吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    entryToDelete?.let { viewModel.deleteEntryForever(it) }
                    entryToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Page header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onNavigateBack() }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "回收站",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "保留30天后自动删除",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (entries.isNotEmpty()) {
                            Text(
                                text = "清空",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { showEmptyDialog = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Entries
                if (entries.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    itemsIndexed(
                        items = entries,
                        key = { _, entry -> entry.id }
                    ) { index, entry ->
                        val enterDelay = (index * 60).coerceAtMost(400)
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300, delayMillis = enterDelay)) +
                                    slideInVertically(
                                        animationSpec = tween(300, delayMillis = enterDelay),
                                        initialOffsetY = { it / 5 }
                                    )
                        ) {
                            TrashEntryCard(
                                entry = entry,
                                onRestore = { entryToRestore = entry },
                                onDeleteForever = { entryToDelete = entry }
                            )
                        }
                    }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "回收站为空",
                fontSize = 16.sp,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "删除的日记会在这里保留30天",
                fontSize = 13.sp,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TrashEntryCard(
    entry: TrashEntry,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "entryCardScale"
    )

    // Calculate days remaining
    val daysRemaining = remember(entry) {
        val deletedDate = Instant.ofEpochMilli(entry.deletedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val expiryDate = deletedDate.plusDays(30)
        val today = java.time.LocalDate.now()
        ChronoUnit.DAYS.between(today, expiryDate).toInt().coerceAtLeast(0)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDeletedTime(entry.deletedAt),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Days remaining
                Text(
                    text = "${daysRemaining}天后删除",
                    fontSize = 12.sp,
                    color = if (daysRemaining <= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Title if present
            if (entry.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Text preview
            if (entry.plainText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.plainText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            // Bottom info: mood + weather + actions
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry.moodLevel != null) {
                    val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                    Icon(
                        imageVector = moodIcon,
                        contentDescription = "心情",
                        tint = moodTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = moodLabelForLevel(entry.moodLevel),
                        fontSize = 12.sp,
                        color = moodTint,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                if (entry.weather != null) {
                    val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "天气",
                        tint = weatherTint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "恢复",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onRestore() }
                    )
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "永久删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDeleteForever() }
                    )
                }
            }
        }
    }
}

private fun formatDeletedTime(timestamp: Long): String {
    val deletedDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    val daysBetween = ChronoUnit.DAYS.between(deletedDate, today).toInt()

    return when {
        daysBetween == 0 -> "今天删除"
        daysBetween == 1 -> "昨天删除"
        daysBetween < 7 -> "${daysBetween}天前删除"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("M月d日")
            "${deletedDate.format(formatter)} 删除"
        }
    }
}
