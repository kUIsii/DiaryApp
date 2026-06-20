package com.diary.app.ui.countdown

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.CountDownItem
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun Long.toColor(): Color = Color(this.toULong())

@Composable
fun CountDownScreen(
    onNavigateBack: () -> Unit,
    viewModel: CountDownViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val editingItem by viewModel.editingItem.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<CountDownItem?>(null) }

    if (showDialog) {
        CountDownDialog(
            editingItem = editingItem,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { title, targetDate, isCountUp, color, isRepeatYearly, isPinned ->
                viewModel.saveItem(title, targetDate, isCountUp, color, isRepeatYearly, isPinned)
            }
        )
    }

    val currentItemToDelete = itemToDelete
    if (showDeleteDialog && currentItemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定删除“${currentItemToDelete.title}”吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(currentItemToDelete.id)
                        showDeleteDialog = false
                    }
                ) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            CountDownHeader(
                itemCount = items.size,
                onNavigateBack = onNavigateBack,
                onCreate = { viewModel.showAddDialog() }
            )

            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = "还没有倒数日",
                    subtitle = "点击右上角创建一个重要日期，把它从纪念变成可执行的计划。",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        CountDownOverviewCard(items = items, viewModel = viewModel)
                    }

                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val enterDelay = (index * 60).coerceAtMost(400)
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300, delayMillis = enterDelay)) +
                                slideInVertically(
                                    animationSpec = tween(300, delayMillis = enterDelay),
                                    initialOffsetY = { it / 5 }
                                )
                        ) {
                            CountDownItemCard(
                                item = item,
                                daysRemaining = viewModel.getDaysRemaining(item),
                                onClick = { viewModel.showEditDialog(item) },
                                onPin = { viewModel.togglePin(item) },
                                onDelete = {
                                    itemToDelete = item
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CountDownHeader(
    itemCount: Int,
    onNavigateBack: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "倒数日",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (itemCount > 0) "共 $itemCount 个计划与纪念" else "把重要日期整理成真正可执行的列表",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onCreate,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "添加",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CountDownOverviewCard(
    items: List<CountDownItem>,
    viewModel: CountDownViewModel
) {
    val pinnedCount = items.count { it.isPinned }
    val upcomingCount = items.count { viewModel.getDaysRemaining(it) >= 0 }
    val nearest = items.minByOrNull { kotlin.math.abs(viewModel.getDaysRemaining(it)) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "把重要日期和后续动作放在同一个节奏里。",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountDownPill(text = "进行中 $upcomingCount")
                CountDownPill(text = "已置顶 $pinnedCount")
                nearest?.let {
                    CountDownPill(text = "最近 ${it.title.take(6)}")
                }
            }
        }
    }
}

@Composable
private fun CountDownPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountDownItemCard(
    item: CountDownItem,
    daysRemaining: Long,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    val targetDate = Instant.ofEpochMilli(item.targetDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    val isPast = targetDate.isBefore(today)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 18.dp,
        innerPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.color.toColor().copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        daysRemaining == 0L -> "今"
                        isPast -> "过"
                        else -> "计"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.color.toColor()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isPinned) {
                        CountDownPill(text = "置顶")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetDate.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        daysRemaining == 0L -> "今天就是这一天"
                        isPast -> "已经过去 ${-daysRemaining} 天"
                        else -> "还有 $daysRemaining 天"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.color.toColor()
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onPin, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (item.isPinned) "取消置顶" else "置顶",
                        tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
