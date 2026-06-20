package com.diary.app.ui.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.IconCircle
import com.diary.app.ui.components.SettingDivider

// Section icon colors — same approach as ProfileScreen
@Composable
private fun sectionIconBg(index: Int): Color {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val t = MaterialTheme.colorScheme.tertiary
    return when (index) {
        0 -> p.copy(alpha = 0.12f)
        1 -> s.copy(alpha = 0.13f)
        2 -> t.copy(alpha = 0.12f)
        3 -> p.copy(alpha = 0.08f)
        else -> p.copy(alpha = 0.10f)
    }
}

@Composable
private fun sectionIconTint(index: Int): Color {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val t = MaterialTheme.colorScheme.tertiary
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    return when (index) {
        0 -> p
        1 -> s
        2 -> t
        3 -> Color(
            (p.red * 0.6f + gray.red * 0.4f),
            (p.green * 0.6f + gray.green * 0.4f),
            (p.blue * 0.6f + gray.blue * 0.4f),
            1f
        )
        else -> p
    }
}

@Composable
fun ToolsScreen(
    onNavigateToStats: () -> Unit = {},
    onNavigateToMediaLibrary: () -> Unit = {},
    onNavigateToCountDown: () -> Unit = {},
    onNavigateToTimeCapsule: () -> Unit = {},
    onNavigateToRandom: () -> Unit = {},
    onNavigateToDiaryMap: () -> Unit = {},
    onNavigateToBiography: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToExperimental: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onSwipeToTimeline: (() -> Unit)? = null,
    onSwipeToTodo: (() -> Unit)? = null
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)

    // Expanded state for each section
    var expandedSection by remember { mutableStateOf<String?>(null) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Text(
                text = "工具",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(onSwipeToTimeline, onSwipeToTodo) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDrag += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                when {
                                    totalDrag >= 72f -> onSwipeToTimeline?.invoke()
                                    totalDrag <= -72f -> onSwipeToTodo?.invoke()
                                }
                            }
                        )
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 创作与记录
                CollapsibleSection(
                    icon = Icons.Default.Edit,
                    iconBg = sectionIconBg(0),
                    iconTint = sectionIconTint(0),
                    title = "创作与记录",
                    subtitle = "数据统计、媒体库、标签管理",
                    isExpanded = expandedSection == "create",
                    onToggle = { expandedSection = if (expandedSection == "create") null else "create" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ClickableToolRow(
                        icon = Icons.Default.BarChart,
                        iconBg = sectionIconBg(0),
                        iconTint = sectionIconTint(0),
                        title = "数据统计",
                        subtitle = "查看你的写作轨迹",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToStats
                    )
                    SettingDivider()
                    ClickableToolRow(
                        icon = Icons.Default.Collections,
                        iconBg = sectionIconBg(0),
                        iconTint = sectionIconTint(0),
                        title = "媒体库",
                        subtitle = "浏览所有图片和视频",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToMediaLibrary
                    )
                    SettingDivider()
                    ClickableToolRow(
                        icon = Icons.Default.Tag,
                        iconBg = sectionIconBg(0),
                        iconTint = sectionIconTint(0),
                        title = "标签管理",
                        subtitle = "整理你的日记分类",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToTagManagement
                    )
                }

                // 回忆与探索
                CollapsibleSection(
                    icon = Icons.Default.Search,
                    iconBg = sectionIconBg(1),
                    iconTint = sectionIconTint(1),
                    title = "回忆与探索",
                    subtitle = "倒数日、时间胶囊、日记地图、随机回顾",
                    isExpanded = expandedSection == "explore",
                    onToggle = { expandedSection = if (expandedSection == "explore") null else "explore" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ClickableToolRow(
                        icon = Icons.Default.Timer,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = "倒数日",
                        subtitle = "重要日期倒计时",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToCountDown
                    )
                    SettingDivider()
                    ClickableToolRow(
                        icon = Icons.Default.MarkEmailUnread,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = "时间胶囊",
                        subtitle = "给未来的自己写信",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToTimeCapsule
                    )
                    SettingDivider()
                    ClickableToolRow(
                        icon = Icons.Default.Map,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = "日记地图",
                        subtitle = "在地图上回顾足迹",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToDiaryMap
                    )
                    SettingDivider()
                    ClickableToolRow(
                        icon = Icons.Default.Shuffle,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = "随机回顾",
                        subtitle = "随机打开一篇日记",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToRandom
                    )
                }

                // AI 伙伴
                CollapsibleSection(
                    icon = Icons.Default.Memory,
                    iconBg = sectionIconBg(2),
                    iconTint = sectionIconTint(2),
                    title = "AI 伙伴",
                    subtitle = "AI 助手、AI 传记",
                    isExpanded = expandedSection == "ai",
                    onToggle = { expandedSection = if (expandedSection == "ai") null else "ai" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ClickableToolRow(
                        icon = Icons.Default.ChatBubbleOutline,
                        iconBg = sectionIconBg(2),
                        iconTint = sectionIconTint(2),
                        title = "AI 助手",
                        subtitle = "智能写作助手小墨",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToAiAssistant
                    )
                    SettingDivider()
                    ClickableToolRow(
                        icon = Icons.Default.AutoAwesome,
                        iconBg = sectionIconBg(2),
                        iconTint = sectionIconTint(2),
                        title = "AI 传记",
                        subtitle = "AI 生成个人传记",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToBiography
                    )
                }

                // 其他
                CollapsibleSection(
                    icon = Icons.Default.Notifications,
                    iconBg = sectionIconBg(3),
                    iconTint = sectionIconTint(3),
                    title = "其他",
                    subtitle = "消息通知、实验性功能",
                    isExpanded = expandedSection == "other",
                    onToggle = { expandedSection = if (expandedSection == "other") null else "other" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ClickableToolRow(
                        icon = Icons.Default.Notifications,
                        iconBg = sectionIconBg(3),
                        iconTint = sectionIconTint(3),
                        title = "消息通知",
                        subtitle = "查看系统通知和提醒",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToNotifications
                    )
                    SettingDivider()
                    ClickableToolRow(
                        icon = Icons.Default.LocationOn,
                        iconBg = sectionIconBg(3),
                        iconTint = sectionIconTint(3),
                        title = "实验性功能",
                        subtitle = "Beta",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToExperimental
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// --- Collapsible Section ---

@Composable
private fun CollapsibleSection(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Column {
            // Header row
            val headerInteraction = remember { MutableInteractionSource() }
            val headerPressed by headerInteraction.collectIsPressedAsState()
            val headerBg by animateColorAsState(
                targetValue = if (headerPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "headerBg"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(headerBg)
                    .clickable(interactionSource = headerInteraction, indication = null) { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(icon = icon, bg = iconBg, tint = iconTint)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(subtitle, fontSize = 11.sp, color = textTertiary, modifier = Modifier.padding(top = 1.dp))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(250, delayMillis = 50)),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

// --- Clickable Tool Row ---

@Composable
private fun ClickableToolRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    textTertiary: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "rowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(icon = icon, bg = iconBg, tint = iconTint)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
            Text(subtitle, fontSize = 11.sp, color = textTertiary, modifier = Modifier.padding(top = 1.dp))
        }
    }
}
