package com.diary.app.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.IconCircle
import com.diary.app.ui.components.SettingDivider

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val sectionIndex: Int,
    val onClick: () -> Unit
)

private data class ToolSection(
    val title: String,
    val items: List<ToolEntry>
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
    onNavigateToExperimental: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onSwipeToTimeline: (() -> Unit)? = null,
    onSwipeToTodo: (() -> Unit)? = null
) {
    val sections = listOf(
        ToolSection(
            title = "整理",
            items = listOf(
                ToolEntry("标签管理", "分类管理", Icons.Default.Label, 0, onNavigateToTagManagement),
                ToolEntry("媒体库", "多媒体浏览", Icons.Default.Collections, 0, onNavigateToMediaLibrary),
                ToolEntry("统计", "写作趋势", Icons.Default.BarChart, 0, onNavigateToStats)
            )
        ),
        ToolSection(
            title = "回顾",
            items = listOf(
                ToolEntry("倒数日", "倒数日提醒", Icons.Default.Event, 1, onNavigateToCountDown),
                ToolEntry("日记地图", "地点回顾", Icons.Default.Explore, 1, onNavigateToDiaryMap),
                ToolEntry("时间胶囊", "未来信件", Icons.Default.MarkEmailUnread, 1, onNavigateToTimeCapsule),
                ToolEntry("随机回顾", "随机回看", Icons.Default.Shuffle, 1, onNavigateToRandom)
            )
        ),
        ToolSection(
            title = "其他",
            items = listOf(
                ToolEntry("AI 助手", "对话协助", Icons.Default.ChatBubbleOutline, 2, onNavigateToAiAssistant),
                ToolEntry("AI 传记", "阶段总结", Icons.Default.AutoAwesome, 2, onNavigateToBiography),
                ToolEntry("消息通知", "通知中心", Icons.Default.Notifications, 2, onNavigateToNotifications),
                ToolEntry("实验功能", "测试功能", Icons.Default.Tune, 2, onNavigateToExperimental)
            )
        )
    )

    GradientBackground {
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ToolsHeaderCard()
            ToolsDirectoryCard(sections = sections)
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun ToolsHeaderCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        innerPadding = 18.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(
                icon = Icons.Default.Widgets,
                bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                tint = MaterialTheme.colorScheme.primary,
                size = 40.dp,
                iconSize = 20.dp,
                cornerRadius = 12.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "工具",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ToolsDirectoryCard(sections: List<ToolSection>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            sections.forEachIndexed { sectionIndex, section ->
                DirectorySection(section = section, sectionIndex = sectionIndex)
                if (sectionIndex != sections.lastIndex) {
                    SettingDivider()
                }
            }
        }
    }
}

@Composable
private fun DirectorySection(section: ToolSection, sectionIndex: Int) {
    val sectionColor = sectionColorFor(sectionIndex)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = section.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        section.items.forEachIndexed { index, item ->
            ToolRow(tool = item, sectionColor = sectionColor)
            if (index != section.items.lastIndex) {
                SettingDivider()
            }
        }
    }
}

@Composable
private fun ToolRow(
    tool: ToolEntry,
    sectionColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "toolRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = tool.onClick
            )
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(
            icon = tool.icon,
            bg = sectionColor.copy(alpha = 0.10f),
            tint = sectionColor,
            size = 40.dp,
            iconSize = 18.dp,
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = tool.subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun sectionColorFor(sectionIndex: Int): Color {
    return when (sectionIndex) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
}
