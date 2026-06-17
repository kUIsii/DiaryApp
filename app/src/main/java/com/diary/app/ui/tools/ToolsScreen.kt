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
    val accentColor = MaterialTheme.colorScheme.primary

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 创作与记录
                ToolSection(
                    title = "创作与记录",
                    textColor = textColor,
                    textSecondary = textSecondary,
                    accentColor = accentColor
                ) {
                    ToolItem(
                        icon = Icons.Default.BarChart,
                        label = "数据统计",
                        subtitle = "查看你的写作轨迹",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToStats
                    )
                    ToolItem(
                        icon = Icons.Default.Collections,
                        label = "媒体库",
                        subtitle = "浏览所有图片和视频",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToMediaLibrary
                    )
                    ToolItem(
                        icon = Icons.Default.Tag,
                        label = "标签管理",
                        subtitle = "整理你的日记分类",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToTagManagement
                    )
                }

                // 回忆与探索
                ToolSection(
                    title = "回忆与探索",
                    textColor = textColor,
                    textSecondary = textSecondary,
                    accentColor = accentColor
                ) {
                    ToolItem(
                        icon = Icons.Default.Timer,
                        label = "倒数日",
                        subtitle = "重要日期倒计时",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToCountDown
                    )
                    ToolItem(
                        icon = Icons.Default.MarkEmailUnread,
                        label = "时间胶囊",
                        subtitle = "给未来的自己写信",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToTimeCapsule
                    )
                    ToolItem(
                        icon = Icons.Default.Map,
                        label = "日记地图",
                        subtitle = "在地图上回顾足迹",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToDiaryMap
                    )
                    ToolItem(
                        icon = Icons.Default.Shuffle,
                        label = "随机回顾",
                        subtitle = "随机打开一篇日记",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToRandom
                    )
                }

                // AI 伙伴
                ToolSection(
                    title = "AI 伙伴",
                    textColor = textColor,
                    textSecondary = textSecondary,
                    accentColor = accentColor
                ) {
                    ToolItem(
                        icon = Icons.Default.ChatBubbleOutline,
                        label = "AI 助手",
                        subtitle = "智能写作助手小墨",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToAiAssistant
                    )
                    ToolItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "AI 传记",
                        subtitle = "AI 生成个人传记",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToBiography
                    )
                }

                // 其他
                ToolSection(
                    title = "其他",
                    textColor = textColor,
                    textSecondary = textSecondary,
                    accentColor = accentColor
                ) {
                    ToolItem(
                        icon = Icons.Default.Notifications,
                        label = "消息通知",
                        subtitle = "查看系统通知和提醒",
                        accentColor = accentColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onClick = onNavigateToNotifications
                    )
                    ExperimentalEntry(
                        accentColor = accentColor,
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
    textColor: Color,
    textSecondary: Color,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column {
        // Section title - bold, larger
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        // Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun ToolItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    accentColor: Color,
    textColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "toolItemScale"
    )

    Row(
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
            )
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with solid background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ExperimentalEntry(
    accentColor: Color,
    textColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "experimentalScale"
    )

    Row(
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
            )
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with solid background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = accentColor,
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
            color = accentColor,
            modifier = Modifier
                .background(
                    accentColor.copy(alpha = 0.1f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
