package com.diary.app.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

data class ToolItem(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val onClick: () -> Unit
)

data class ExperimentalItem(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val onClick: () -> Unit
)

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
    onNavigateToExperimental: () -> Unit = {}
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val toolItems = listOf(
        ToolItem(Icons.Default.BarChart, "数据统计", "查看写作数据和趋势", onNavigateToStats),
        ToolItem(Icons.Default.Collections, "媒体库", "浏览所有图片和视频", onNavigateToMediaLibrary),
        ToolItem(Icons.Default.Timer, "倒数日", "重要日期倒计时", onNavigateToCountDown),
        ToolItem(Icons.Default.MarkEmailUnread, "时间胶囊", "给未来的自己写信", onNavigateToTimeCapsule),
        ToolItem(Icons.Default.Shuffle, "随机回顾", "随机打开一篇日记", onNavigateToRandom)
    )

    val experimentalItems = listOf(
        ExperimentalItem(Icons.Default.Map, "日记地图", "在地图上查看日记位置", onNavigateToDiaryMap),
        ExperimentalItem(Icons.Default.AutoAwesome, "AI 传记", "AI 生成个人传记", onNavigateToBiography),
        ExperimentalItem(Icons.Default.Sell, "标签管理", "管理日记分类标签", onNavigateToTagManagement)
    )

    GradientBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "工具",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = textColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "探索更多实用功能",
                    fontSize = 14.sp,
                    color = textSecondary
                )
            }

            // Featured tools - 2x2 grid style
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Featured item 1 - Data Stats (larger)
                    FeaturedToolCard(
                        icon = Icons.Default.BarChart,
                        label = "数据统计",
                        description = "查看写作数据和趋势",
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.15f),
                                primaryColor.copy(alpha = 0.05f)
                            )
                        ),
                        onClick = onNavigateToStats,
                        modifier = Modifier.weight(1f)
                    )
                    // Featured item 2 - Media Library (larger)
                    FeaturedToolCard(
                        icon = Icons.Default.Collections,
                        label = "媒体库",
                        description = "浏览所有图片和视频",
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.15f),
                                secondaryColor.copy(alpha = 0.05f)
                            )
                        ),
                        onClick = onNavigateToMediaLibrary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Other tools - horizontal scroll or list
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    toolItems.drop(2).forEach { item ->
                        ToolListItem(
                            icon = item.icon,
                            label = item.label,
                            description = item.description,
                            onClick = item.onClick
                        )
                    }
                }
            }

            // Experimental section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "实验性功能",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }

            // Experimental items
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    experimentalItems.forEach { item ->
                        ExperimentalListItem(
                            icon = item.icon,
                            label = item.label,
                            description = item.description,
                            onClick = item.onClick
                        )
                    }
                }
            }

            // More experimental button
            item {
                MoreExperimentalButton(onClick = onNavigateToExperimental)
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FeaturedToolCard(
    icon: ImageVector,
    label: String,
    description: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        cornerRadius = 20.dp,
        innerPadding = 16.dp,
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ToolListItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ExperimentalListItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Beta",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MoreExperimentalButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Science,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "查看全部实验性功能",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
