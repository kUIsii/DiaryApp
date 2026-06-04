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

@OptIn(ExperimentalMaterial3Api::class)
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

    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${itemToDelete!!.title}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(itemToDelete!!.id)
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
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("倒数日") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.showAddDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "添加")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Add,
                        title = "暂无倒数日",
                        subtitle = "点击右上角按钮添加你的第一个倒数日"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

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
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.color.toColor().copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (daysRemaining == 0L) "!" else if (isPast) "-" else "+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.color.toColor()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = when {
                        daysRemaining == 0L -> "今天"
                        isPast -> "已过 ${-daysRemaining} 天"
                        else -> "还有 $daysRemaining 天"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.color.toColor()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    if (item.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "已置顶",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDelete() }
                    )
                }
            }
        }
    }
}
