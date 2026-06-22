package com.diary.app.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCapsule: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToMonthlyReport: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToAnnualReport: () -> Unit = {},
    viewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Mark all notifications as read when entering the screen
    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationTopBar(
                showTrash = uiState.showTrash,
                onNavigateBack = onNavigateBack,
                onToggleTrash = { viewModel.toggleTrashView() }
            )

            if (uiState.showTrash) {
                TrashView(
                    trashedItems = uiState.trashedNotifications,
                    onRestore = { viewModel.restoreNotification(it) },
                    onPermanentDelete = { viewModel.permanentDeleteNotification(it) },
                    onEmptyTrash = { viewModel.emptyTrash() },
                    onNavigateToCapsule = onNavigateToCapsule,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToMonthlyReport = onNavigateToMonthlyReport,
                    onNavigateToAnnualReport = onNavigateToAnnualReport
                )
            } else {
                // 主内容区，支持左右滑切换分类
                var categoryDragTotal by remember { mutableStateOf(0f) }
                val categories = NotificationCategory.entries

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { categoryDragTotal = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    categoryDragTotal += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = 80f
                                    val currentIndex = categories.indexOf(uiState.selectedCategory)
                                    if (categoryDragTotal < -threshold && currentIndex < categories.lastIndex) {
                                        viewModel.selectCategory(categories[currentIndex + 1])
                                    } else if (categoryDragTotal > threshold && currentIndex > 0) {
                                        viewModel.selectCategory(categories[currentIndex - 1])
                                    }
                                    categoryDragTotal = 0f
                                }
                            )
                        }
                ) {
                // 分类 Tab
                CategoryTabRow(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) }
                )

                // 筛选后的通知
                val filteredNotifications = if (uiState.selectedCategory == NotificationCategory.ALL) {
                    uiState.notifications
                } else {
                    uiState.notifications.filter { it.category == uiState.selectedCategory }
                }

                if (filteredNotifications.isEmpty()) {
                    EmptyNotificationsView(category = uiState.selectedCategory)
                } else {
                    NotificationList(
                        notifications = filteredNotifications,
                        onTrash = { viewModel.trashNotification(it) },
                        onNavigateToCapsule = onNavigateToCapsule,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToMonthlyReport = onNavigateToMonthlyReport,
                        onNavigateToAnnualReport = onNavigateToAnnualReport
                    )
                }
                } // end swipe Column
            }
        }
    }
}

// region 标题栏
@Composable
private fun NotificationTopBar(
    showTrash: Boolean,
    onNavigateBack: () -> Unit,
    onToggleTrash: () -> Unit
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
        Text(
            text = if (showTrash) "回收站" else "通知",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onToggleTrash,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (showTrash) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else Color.Transparent
                )
        ) {
            Icon(
                imageVector = if (showTrash) Icons.Default.Notifications else Icons.Default.RestoreFromTrash,
                contentDescription = if (showTrash) "返回通知" else "打开回收站",
                tint = if (showTrash) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// endregion

// region 分类 Tab

@Composable
private fun CategoryTabRow(
    selectedCategory: NotificationCategory,
    onCategorySelected: (NotificationCategory) -> Unit
) {
    val categories = NotificationCategory.entries
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            CategoryTab(
                label = category.label,
                isSelected = isSelected,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// endregion

// region 通知列表

@Composable
private fun NotificationList(
    notifications: List<NotificationItem>,
    onTrash: (String) -> Unit,
    onNavigateToCapsule: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToMonthlyReport: (Int, Int) -> Unit,
    onNavigateToAnnualReport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        itemsIndexed(
            items = notifications,
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
                NotificationCardWithMenu(
                    item = item,
                    onTrash = { onTrash(item.id) },
                    onClick = {
                        when (item) {
                            is CapsuleUnlockNotification -> onNavigateToCapsule(item.capsule.id)
                            is OnThisDayNotification -> onNavigateToDetail(item.entry.id)
                            is MonthlyReportNotification -> onNavigateToMonthlyReport(item.year, item.month)
                            is AnnualReportNotification -> onNavigateToAnnualReport()
                            else -> {}
                        }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// endregion

// region 通知卡片

@Composable
private fun SwipeableNotificationCard(
    item: NotificationItem,
    onTrash: () -> Unit,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val threshold = -200f

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 背景：删除图标
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }

        // 前景：通知卡片
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = offsetX }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX + dragAmount).coerceIn(-300f, 0f)
                            offsetX = newOffset
                        },
                        onDragEnd = {
                            if (offsetX < threshold) {
                                onTrash()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        }
                    )
                }
        ) {
            NotificationCardWithMenu(
                item = item,
                onTrash = onTrash,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun NotificationCardWithMenu(
    item: NotificationItem,
    onTrash: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clickable = item is CapsuleUnlockNotification ||
            item is OnThisDayNotification ||
            item is MonthlyReportNotification ||
            item is AnnualReportNotification

    Box {
        NotificationCard(
            item = item,
            onClick = if (clickable) onClick else ({}),
            clickable = clickable,
            onLongClick = { showMenu = true },
            onClose = onTrash
        )

        // 长按菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("移到回收站") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    showMenu = false
                    onTrash()
                }
            )
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    clickable: Boolean,
    onLongClick: () -> Unit,
    onClose: () -> Unit
) {
    val (icon, iconColor, title, subtitle) = getNotificationStyle(item)
    val timeText = formatTimestamp(item.timestamp)

    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = clickable, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 28.dp)
            ) {
                // 左侧图标
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.padding(10.dp).size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                // 中间文字
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                // 右侧时间
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            // 右上角关闭按钮
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// endregion

// region 回收站
@Composable
private fun TrashView(
    trashedItems: List<NotificationItem>,
    onRestore: (String) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onNavigateToCapsule: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToMonthlyReport: (Int, Int) -> Unit,
    onNavigateToAnnualReport: () -> Unit
) {
    if (trashedItems.isEmpty()) {
        EmptyTrashView()
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                itemsIndexed(
                    items = trashedItems,
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
                        TrashedNotificationCard(
                            item = item,
                            onRestore = { onRestore(item.id) },
                            onPermanentDelete = { onPermanentDelete(item.id) },
                            onClick = {
                                when (item) {
                                    is CapsuleUnlockNotification -> onNavigateToCapsule(item.capsule.id)
                                    is OnThisDayNotification -> onNavigateToDetail(item.entry.id)
                                    is MonthlyReportNotification -> onNavigateToMonthlyReport(item.year, item.month)
                                    is AnnualReportNotification -> onNavigateToAnnualReport()
                                    else -> {}
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // 底部清空按钮
            if (trashedItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onEmptyTrash) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "清空回收站",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashedNotificationCard(
    item: NotificationItem,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, iconColor, title, subtitle) = getNotificationStyle(item)
    val clickable = item is CapsuleUnlockNotification ||
            item is OnThisDayNotification ||
            item is MonthlyReportNotification ||
            item is AnnualReportNotification

    Box {
        GlassCard(
            cornerRadius = 16.dp,
            innerPadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(10.dp).size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("恢复") },
                leadingIcon = {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                onClick = {
                    showMenu = false
                    onRestore()
                }
            )
            DropdownMenuItem(
                text = { Text("永久删除") },
                leadingIcon = {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    showMenu = false
                    onPermanentDelete()
                }
            )
        }
    }
}

// endregion

// region 空状态
@Composable
private fun EmptyNotificationsView(category: NotificationCategory) {
    val (title, subtitle) = when (category) {
        NotificationCategory.ALL -> "暂时没有新消息" to "有值得关注的事会出现在这里"
        NotificationCategory.MONTHLY_REPORT -> "没有月报通知" to "当月有日记数据时会生成月报"
        NotificationCategory.ANNUAL_REPORT -> "没有年报通知" to "每年12月25日后会生成年度报告"
        NotificationCategory.TIME_CAPSULE -> "没有胶囊通知" to "时间胶囊到期时会出现在这里"
        NotificationCategory.MILESTONE -> "没有里程碑通知" to "达成写作里程碑时会通知你"
        NotificationCategory.ON_THIS_DAY -> "没有今日回顾" to "往年今天写的日记会出现在这里"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            icon = Icons.Default.Notifications,
            title = title,
            subtitle = subtitle
        )
    }
}

@Composable
private fun EmptyTrashView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            icon = Icons.Default.Delete,
            title = "回收站是空的",
            subtitle = "删除的通知会暂时保留在这里"
        )
    }
}

// endregion

// region 辅助函数

private fun getNotificationStyle(item: NotificationItem): NotificationStyle {
    return when (item) {
        is CapsuleUnlockNotification -> NotificationStyle(
            icon = Icons.Default.LockOpen,
            iconColor = Color(0xFF6750A4),
            title = "时间胶囊已到期",
            subtitle = "你有一封信到了可以打开的日子"
        )
        is OnThisDayNotification -> NotificationStyle(
            icon = Icons.Default.History,
            iconColor = Color(0xFF7B61FF),
            title = "${item.yearsAgo} 年前的今天",
            subtitle = if (item.entry.title.isNotBlank()) item.entry.title
            else item.entry.plainText.take(40)
        )
        is MilestoneNotification -> NotificationStyle(
            icon = Icons.Default.EmojiEvents,
            iconColor = Color(0xFFD4A017),
            title = item.title,
            subtitle = item.subtitle
        )
        is StreakNotification -> NotificationStyle(
            icon = Icons.Default.LocalFireDepartment,
            iconColor = Color(0xFFE86833),
            title = "连续写作 ${item.days} 天",
            subtitle = "坚持记录，保持习惯"
        )
        is AnnualReportNotification -> NotificationStyle(
            icon = Icons.Default.Assessment,
            iconColor = Color(0xFF4A90E2),
            title = "${item.year} 年度报告已生成",
            subtitle = "回顾过去一年的点滴记录，点击查看年度总结"
        )
        is MonthlyReportNotification -> NotificationStyle(
            icon = Icons.Default.Assessment,
            iconColor = Color(0xFF4A90E2),
            title = "${item.month}月写作报告",
            subtitle = "本月写了 ${item.entryCount} 篇日记，共 ${item.wordCount} 字"
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val now = Instant.now().atZone(ZoneId.systemDefault())

    return when {
        zonedDateTime.toLocalDate() == now.toLocalDate() -> {
            zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        zonedDateTime.year == now.year -> {
            zonedDateTime.format(DateTimeFormatter.ofPattern("M月d日"))
        }
        else -> {
            zonedDateTime.format(DateTimeFormatter.ofPattern("yy/M/d"))
        }
    }
}

private data class NotificationStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String
)

// endregion
