package com.diary.app.ui.tools

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.theme.themeMode
import com.diary.app.ui.theme.isDark

@OptIn(ExperimentalFoundationApi::class)
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
    val dark = themeMode().isDark()
    val bgColor = if (dark) Color(0xFF121212) else Color(0xFFFAFAF8)
    val textColor = if (dark) Color(0xFFE0E0E0) else Color(0xFF202020)
    val textSecondary = if (dark) Color(0xFF9E9E9E) else Color(0xFF757575)
    val dividerColor = if (dark) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ──
            item {
                Column(modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)) {
                    Text(
                        text = "工具",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = textColor,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "探索更多可能",
                        fontSize = 14.sp,
                        color = textSecondary,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            // ── 创作与记录 ──
            item {
                SectionHeader("创作与记录", textSecondary)
            }
            item {
                ToolItem(
                    icon = Icons.Default.BarChart,
                    label = "数据统计",
                    subtitle = "查看你的写作轨迹",
                    iconBg = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF4CAF50),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToStats
                )
            }
            item {
                ToolItem(
                    icon = Icons.Default.Collections,
                    label = "媒体库",
                    subtitle = "浏览所有图片和视频",
                    iconBg = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF2196F3),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToMediaLibrary
                )
            }
            item {
                ToolItem(
                    icon = Icons.Default.Tag,
                    label = "标签管理",
                    subtitle = "整理你的日记分类",
                    iconBg = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFFF9800),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToTagManagement
                )
            }

            // ── 回忆与探索 ──
            item {
                SectionHeader("回忆与探索", textSecondary, topPadding = 24)
            }
            item {
                ToolItem(
                    icon = Icons.Default.Timer,
                    label = "倒数日",
                    subtitle = "重要日期倒计时",
                    iconBg = Color(0xFFFCE4EC),
                    iconTint = Color(0xFFE91E63),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToCountDown
                )
            }
            item {
                ToolItem(
                    icon = Icons.Default.MarkEmailUnread,
                    label = "时间胶囊",
                    subtitle = "给未来的自己写信",
                    iconBg = Color(0xFFE8EAF6),
                    iconTint = Color(0xFF3F51B5),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToTimeCapsule
                )
            }
            item {
                ToolItem(
                    icon = Icons.Default.Map,
                    label = "日记地图",
                    subtitle = "在地图上回顾足迹",
                    iconBg = Color(0xFFE0F2F1),
                    iconTint = Color(0xFF009688),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToDiaryMap
                )
            }
            item {
                ToolItem(
                    icon = Icons.Default.Shuffle,
                    label = "随机回顾",
                    subtitle = "随机打开一篇日记",
                    iconBg = Color(0xFFF1F8E9),
                    iconTint = Color(0xFF8BC34A),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToRandom
                )
            }

            // ── AI 伙伴 ──
            item {
                SectionHeader("AI 伙伴", textSecondary, topPadding = 24)
            }
            item {
                ToolItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "AI 助手",
                    subtitle = "智能写作助手小墨",
                    iconBg = Color(0xFFF3E5F5),
                    iconTint = Color(0xFF9C27B0),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToAiAssistant
                )
            }
            item {
                ToolItem(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI 传记",
                    subtitle = "AI 生成个人传记",
                    iconBg = Color(0xFFFFF8E1),
                    iconTint = Color(0xFFFFC107),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToBiography
                )
            }

            // ── 其他 ──
            item {
                SectionHeader("其他", textSecondary, topPadding = 24)
            }
            item {
                ToolItem(
                    icon = Icons.Default.Notifications,
                    label = "消息通知",
                    subtitle = "查看系统通知和提醒",
                    iconBg = Color(0xFFEFEBE9),
                    iconTint = Color(0xFF795548),
                    textColor = textColor,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor,
                    onClick = onNavigateToNotifications
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToExperimental)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "实验性功能",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = textSecondary,
                        letterSpacing = 0.1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Beta",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF859B7A),
                        modifier = Modifier
                            .background(
                                Color(0xFF859B7A).copy(alpha = 0.12f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    textSecondary: Color,
    topPadding: Int = 0
) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = textSecondary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = topPadding.dp, bottom = 12.dp, start = 2.dp)
    )
}

@Composable
private fun ToolItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    textColor: Color,
    textSecondary: Color,
    dividerColor: Color,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor,
                    letterSpacing = 0.1.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textSecondary,
                    letterSpacing = 0.1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp)
                .height(0.5.dp)
                .background(dividerColor)
        )
    }
}
