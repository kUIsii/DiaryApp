package com.diary.app.ui.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

data class ToolItem(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val iconTint: androidx.compose.ui.graphics.Color? = null,
    val onClick: () -> Unit
)

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
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    val toolItems = listOf(
        ToolItem(Icons.Default.BarChart, "数据统计", "查看写作数据和趋势", onClick = onNavigateToStats),
        ToolItem(Icons.Default.Collections, "媒体库", "浏览所有图片和视频", onClick = onNavigateToMediaLibrary),
        ToolItem(Icons.Default.Timer, "倒数日", "重要日期倒计时", onClick = onNavigateToCountDown),
        ToolItem(Icons.Default.MarkEmailUnread, "时间胶囊", "给未来的自己写信", onClick = onNavigateToTimeCapsule),
        ToolItem(Icons.Default.Shuffle, "随机回顾", "随机打开一篇日记", onClick = onNavigateToRandom),
        ToolItem(Icons.Default.Tag, "标签管理", "管理日记分类标签", onClick = onNavigateToTagManagement),
        ToolItem(Icons.Default.Notifications, "消息通知", "查看系统通知和提醒", onClick = onNavigateToNotifications),
        ToolItem(Icons.Default.ChatBubbleOutline, "AI 助手", "智能写作助手小墨", iconTint = MaterialTheme.colorScheme.tertiary, onClick = onNavigateToAiAssistant)
    )

    val experimentalItems = listOf(
        ToolItem(Icons.Default.Map, "日记地图", "在地图上查看日记足迹", onClick = onNavigateToDiaryMap),
        ToolItem(Icons.Default.AutoAwesome, "AI 传记", "AI 生成个人传记", onClick = onNavigateToBiography)
    )

    GradientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "工具",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor
                    )
                    Text(
                        text = "探索更多实用功能",
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                }
            }

            // Core tools - horizontal cards
            items(toolItems, key = { it.label }) { item ->
                androidx.compose.foundation.layout.Box(modifier = Modifier.animateItemPlacement()) {
                    ToolRowCard(
                        icon = item.icon,
                        label = item.label,
                        description = item.description,
                        iconTint = item.iconTint ?: primaryColor,
                        onClick = item.onClick
                    )
                }
            }

            // Experimental section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "实验性功能",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Beta",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            items(experimentalItems) { item ->
                ToolRowCard(
                    icon = item.icon,
                    label = item.label,
                    description = item.description,
                    iconTint = item.iconTint ?: MaterialTheme.colorScheme.tertiary,
                    onClick = item.onClick
                )
            }

            // More experimental button
            item {
                GlassCard(
                    cornerRadius = 16.dp,
                    innerPadding = 0.dp,
                    onClick = onNavigateToExperimental,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "查看全部实验性功能",
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ToolRowCard(
    icon: ImageVector,
    label: String,
    description: String,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 14.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
