package com.diary.app.ui.capsule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
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
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
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
                        subtitle = "写一封信给未来的自己\n到了约定的日子再打开",
                        action = {
                            Button(
                                onClick = onNavigateToCreate,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("写第一封")
                            }
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SummaryCard(
                            total = capsules.size,
                            lockedCount = locked.size,
                            unlockedCount = unlocked.size,
                            onCreate = onNavigateToCreate
                        )
                    }

                    if (locked.isNotEmpty()) {
                        item { SectionHeader(title = "未到期", count = locked.size, accent = MaterialTheme.colorScheme.tertiary) }
                        itemsIndexed(
                            items = locked,
                            key = { _, item -> item.id }
                        ) { index, capsule ->
                            val enterDelay = (index * 60).coerceAtMost(400)
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(280, delayMillis = enterDelay)) +
                                        slideInVertically(
                                            animationSpec = tween(280, delayMillis = enterDelay),
                                            initialOffsetY = { it / 6 }
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
                        item { SectionHeader(title = "已到期", count = unlocked.size, accent = MaterialTheme.colorScheme.primary) }
                        itemsIndexed(
                            items = unlocked,
                            key = { _, item -> item.id }
                        ) { index, capsule ->
                            val enterDelay = ((index + locked.size) * 60).coerceAtMost(400)
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(280, delayMillis = enterDelay)) +
                                        slideInVertically(
                                            animationSpec = tween(280, delayMillis = enterDelay),
                                            initialOffsetY = { it / 6 }
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

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    total: Int,
    lockedCount: Int,
    unlockedCount: Int,
    onCreate: () -> Unit
) {
    GlassCard(
        cornerRadius = 22.dp,
        innerPadding = 18.dp,
        modifier = Modifier.fillMaxWidth(),
        enableShadow = true
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "时间胶囊",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "把某一刻留给未来再打开",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricBlock(label = "总数", value = total.toString())
                MetricBlock(label = "未到期", value = lockedCount.toString())
                MetricBlock(label = "已到期", value = unlockedCount.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("写一封给未来的自己")
            }
        }
    }
}

@Composable
private fun RowScope.MetricBlock(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    accent: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accent, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CapsuleCard(
    capsule: TimeCapsule,
    isLocked: Boolean,
    daysRemaining: Int?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val createdText = remember(capsule) {
        val created = Instant.ofEpochMilli(capsule.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        "${created.year}年${created.monthValue}月${created.dayOfMonth}日"
    }
    val unlockText = remember(capsule) {
        val unlock = Instant.ofEpochMilli(capsule.unlockDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        "${unlock.year}年${unlock.monthValue}月${unlock.dayOfMonth}日"
    }

    GlassCard(
        cornerRadius = 20.dp,
        innerPadding = 16.dp,
        enableShadow = true,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (!isLocked) onClick() },
                onLongClick = onDelete
            )
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isLocked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isLocked) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isLocked) {
                    Text(
                        text = "一封来自过去的信",
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "到期后才能查看内容",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    val previewText = remember(capsule.content) {
                        capsule.content.trim().lineSequence().firstOrNull().orEmpty().ifBlank { "没有正文预览" }
                    }
                    Text(
                        text = capsule.title,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = previewText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetadataPill(text = createdText)
                    Spacer(modifier = Modifier.width(8.dp))
                    MetadataPill(text = if (isLocked) "${daysRemaining ?: 0} 天后解锁" else "已解锁")
                }
                if (!isLocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "解锁于 $unlockText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }

            if (!isLocked) {
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
        )
    }
}

private fun daysUntilUnlock(capsule: TimeCapsule): Int {
    val now = LocalDate.now()
    val unlockDate = Instant.ofEpochMilli(capsule.unlockDate)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(now, unlockDate).toInt().coerceAtLeast(0)
}
