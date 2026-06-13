package com.diary.app.ui.capsule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.TimeCapsule
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCapsuleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToRead: (Long) -> Unit,
    viewModel: TimeCapsuleViewModel = viewModel()
) {
    val capsules by viewModel.capsules.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var capsuleToDelete by remember { mutableStateOf<TimeCapsule?>(null) }

    val now = remember { LocalDate.now() }
    val locked = remember(capsules, now) {
        capsules.filter { capsule ->
            val unlockDate = Instant.ofEpochMilli(capsule.unlockDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            unlockDate.isAfter(now)
        }
    }
    val unlocked = remember(capsules, now) {
        capsules.filter { capsule ->
            val unlockDate = Instant.ofEpochMilli(capsule.unlockDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            !unlockDate.isAfter(now)
        }
    }

    val currentCapsuleToDelete = capsuleToDelete
    if (showDeleteDialog && currentCapsuleToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这封时间胶囊吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCapsule(currentCapsuleToDelete)
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    GradientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("时间胶囊") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToCreate) {
                            Icon(Icons.Default.Add, contentDescription = "写一封")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            }
        ) { innerPadding ->
            if (capsules.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyState(
                        icon = Icons.Default.Lock,
                        title = "还没有时间胶囊",
                        subtitle = "写一封信给未来的自己\n到了约定的日子再打开"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    if (locked.isNotEmpty()) {
                        item {
                            Text(
                                text = "未到期",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        itemsIndexed(
                            items = locked,
                            key = { _, item -> item.id }
                        ) { index, capsule ->
                            val enterDelay = (index * 60).coerceAtMost(400)
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(300, delayMillis = enterDelay)) +
                                        slideInVertically(
                                            animationSpec = tween(300, delayMillis = enterDelay),
                                            initialOffsetY = { it / 5 }
                                        )
                            ) {
                                CapsuleCard(
                                    capsule = capsule,
                                    isLocked = true,
                                    daysRemaining = daysUntilUnlock(capsule),
                                    onClick = {},
                                    onDelete = {
                                        capsuleToDelete = capsule
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }

                    if (unlocked.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "已到期",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        itemsIndexed(
                            items = unlocked,
                            key = { _, item -> item.id }
                        ) { index, capsule ->
                            val enterDelay = ((index + locked.size) * 60).coerceAtMost(400)
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(300, delayMillis = enterDelay)) +
                                        slideInVertically(
                                            animationSpec = tween(300, delayMillis = enterDelay),
                                            initialOffsetY = { it / 5 }
                                        )
                            ) {
                                CapsuleCard(
                                    capsule = capsule,
                                    isLocked = false,
                                    daysRemaining = null,
                                    onClick = { onNavigateToRead(capsule.id) },
                                    onDelete = {
                                        capsuleToDelete = capsule
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CapsuleCard(
    capsule: TimeCapsule,
    isLocked: Boolean,
    daysRemaining: Int?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(capsule) {
        val created = Instant.ofEpochMilli(capsule.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        "${created.year}年${created.monthValue}月${created.dayOfMonth}日写"
    }
    val unlockText = remember(capsule) {
        val unlock = Instant.ofEpochMilli(capsule.unlockDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        "${unlock.year}年${unlock.monthValue}月${unlock.dayOfMonth}日解锁"
    }

    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = capsule.title,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLocked) {
                        "$dateText · 还有 $daysRemaining 天"
                    } else {
                        "$dateText · $unlockText"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (!isLocked) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun daysUntilUnlock(capsule: TimeCapsule): Int {
    val now = LocalDate.now()
    val unlockDate = Instant.ofEpochMilli(capsule.unlockDate)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(now, unlockDate).toInt().coerceAtLeast(0)
}
