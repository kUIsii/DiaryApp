package com.diary.app.ui.tools

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

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
    onNavigateToAiAssistant: () -> Unit = {}
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 创作与记录 — primary color
                ToolSection(
                    title = "创作与记录",
                    colorIndex = 0,
                    textColor = textColor,
                    textSecondary = textSecondary
                ) {
                    ToolItem(
                        icon = Icons.Default.BarChart,
                        label = "数据统计",
                        subtitle = "查看你的写作轨迹",
                        colorIndex = 0,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToStats
                    )
                    ToolItem(
                        icon = Icons.Default.Collections,
                        label = "媒体库",
                        subtitle = "浏览所有图片和视频",
                        colorIndex = 0,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToMediaLibrary
                    )
                    ToolItem(
                        icon = Icons.Default.Tag,
                        label = "标签管理",
                        subtitle = "整理你的日记分类",
                        colorIndex = 0,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToTagManagement
                    )
                }

                // 回忆与探索 — secondary color
                ToolSection(
                    title = "回忆与探索",
                    colorIndex = 1,
                    textColor = textColor,
                    textSecondary = textSecondary
                ) {
                    ToolItem(
                        icon = Icons.Default.Timer,
                        label = "倒数日",
                        subtitle = "重要日期倒计时",
                        colorIndex = 1,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToCountDown
                    )
                    ToolItem(
                        icon = Icons.Default.MarkEmailUnread,
                        label = "时间胶囊",
                        subtitle = "给未来的自己写信",
                        colorIndex = 1,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToTimeCapsule
                    )
                    ToolItem(
                        icon = Icons.Default.Map,
                        label = "日记地图",
                        subtitle = "在地图上回顾足迹",
                        colorIndex = 1,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToDiaryMap
                    )
                    ToolItem(
                        icon = Icons.Default.Shuffle,
                        label = "随机回顾",
                        subtitle = "随机打开一篇日记",
                        colorIndex = 1,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToRandom
                    )
                }

                // AI 伙伴 — tertiary color
                ToolSection(
                    title = "AI 伙伴",
                    colorIndex = 2,
                    textColor = textColor,
                    textSecondary = textSecondary
                ) {
                    ToolItem(
                        icon = Icons.Default.ChatBubbleOutline,
                        label = "AI 助手",
                        subtitle = "智能写作助手小墨",
                        colorIndex = 2,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToAiAssistant
                    )
                    ToolItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "AI 传记",
                        subtitle = "AI 生成个人传记",
                        colorIndex = 2,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToBiography
                    )
                }

                // 其他 — muted primary
                ToolSection(
                    title = "其他",
                    colorIndex = 3,
                    textColor = textColor,
                    textSecondary = textSecondary
                ) {
                    ToolItem(
                        icon = Icons.Default.Notifications,
                        label = "消息通知",
                        subtitle = "查看系统通知和提醒",
                        colorIndex = 3,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToNotifications
                    )
                    ExperimentalEntry(
                        colorIndex = 3,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToExperimental
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ToolSection(
    title: String,
    colorIndex: Int,
    textColor: Color,
    textSecondary: Color,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column {
            // Section title
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 0.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun ToolItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    colorIndex: Int,
    textColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "toolItemBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with themed background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sectionIconBg(colorIndex)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = sectionIconTint(colorIndex),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
private fun ExperimentalEntry(
    colorIndex: Int,
    textColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "experimentalBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with themed background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sectionIconBg(colorIndex)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = sectionIconTint(colorIndex),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "实验性功能",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "Beta",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = sectionIconTint(colorIndex),
            modifier = Modifier
                .background(
                    sectionIconBg(colorIndex),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
