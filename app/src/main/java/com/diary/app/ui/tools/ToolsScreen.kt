package com.diary.app.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.IconCircle
import com.diary.app.ui.components.SettingDivider

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val meta: String,
    val icon: ImageVector,
    val paletteIndex: Int,
    val onClick: () -> Unit
)

private data class ToolSection(
    val title: String,
    val subtitle: String,
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
    val pinnedTools = listOf(
        ToolEntry(
            title = "标签管理",
            subtitle = "先处理命名、分类和重复标签",
            meta = "这轮重做的重点入口",
            icon = Icons.Default.Tag,
            paletteIndex = 0,
            onClick = onNavigateToTagManagement
        ),
        ToolEntry(
            title = "倒数日",
            subtitle = "继续整理纪念日、计划日和提醒",
            meta = "更适合做成真实列表页",
            icon = Icons.Default.Event,
            paletteIndex = 1,
            onClick = onNavigateToCountDown
        ),
        ToolEntry(
            title = "日记地图",
            subtitle = "按地点回顾记录，不把地图做成展示页",
            meta = "地点目录和回顾路线都放这里",
            icon = Icons.Default.Explore,
            paletteIndex = 2,
            onClick = onNavigateToDiaryMap
        )
    )

    val recentTools = listOf(
        ToolEntry(
            title = "标签管理",
            subtitle = "刚整理过 3 个重复标签",
            meta = "继续把颜色和命名统一掉",
            icon = Icons.Default.Label,
            paletteIndex = 0,
            onClick = onNavigateToTagManagement
        ),
        ToolEntry(
            title = "倒数日",
            subtitle = "最近节点在 2 天后",
            meta = "补一个提醒时间会更完整",
            icon = Icons.Default.Event,
            paletteIndex = 1,
            onClick = onNavigateToCountDown
        )
    )

    val sections = listOf(
        ToolSection(
            title = "整理内容",
            subtitle = "把已有记录整理得更好找，而不是把工具页做成展示墙。",
            items = listOf(
                ToolEntry("标签管理", "统一标签、分类和筛选入口", "重点重做页面", Icons.Default.Label, 0, onNavigateToTagManagement),
                ToolEntry("媒体库", "查看图片、视频和附件", "暂时保留当前布局", Icons.Default.Collections, 0, onNavigateToMediaLibrary),
                ToolEntry("统计", "回看写作趋势与热力图", "本轮先保留现状", Icons.Default.BarChart, 0, onNavigateToStats)
            )
        ),
        ToolSection(
            title = "回顾时间",
            subtitle = "把节点、地点和未来内容整理成一条可继续行动的线。",
            items = listOf(
                ToolEntry("倒数日", "纪念日、计划日和提醒都放这里", "重点重做页面", Icons.Default.Event, 1, onNavigateToCountDown),
                ToolEntry("日记地图", "地点聚合与路线回顾", "重点重做页面", Icons.Default.Explore, 2, onNavigateToDiaryMap),
                ToolEntry("时间胶囊", "写给未来的内容与开启节点", "低频能力，适合留在这一组", Icons.Default.MarkEmailUnread, 1, onNavigateToTimeCapsule),
                ToolEntry("随机回顾", "随机翻到一篇旧日记", "适合轻量回顾", Icons.Default.Shuffle, 1, onNavigateToRandom)
            )
        ),
        ToolSection(
            title = "助手与系统",
            subtitle = "让低频能力自然退后，不再抢主屏注意力。",
            items = listOf(
                ToolEntry("AI 助手", "围绕当前记录提供对话协助", "保留真实功能入口", Icons.Default.ChatBubbleOutline, 3, onNavigateToAiAssistant),
                ToolEntry("AI 传记", "生成个人传记与阶段总结", "更适合放在二级层", Icons.Default.AutoAwesome, 3, onNavigateToBiography),
                ToolEntry("消息通知", "提醒、回顾与系统动态", "系统相关入口", Icons.Default.Notifications, 3, onNavigateToNotifications),
                ToolEntry("实验功能", "仍在验证中的能力入口", "不要出现在首屏重点区", Icons.Default.Tune, 3, onNavigateToExperimental)
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
            PinnedToolsCard(tools = pinnedTools)
            RecentToolsCard(tools = recentTools)
            sections.forEach { section ->
                ToolSectionCard(section = section)
            }
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = Icons.Default.Widgets,
                    bg = sectionIconBg(0),
                    tint = sectionIconTint(0),
                    size = 44.dp,
                    iconSize = 20.dp,
                    cornerRadius = 14.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "工具",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "把最常继续处理的入口放前面，其余功能按真实使用路径收进目录。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderHint(text = "右滑回时间轴", modifier = Modifier.weight(1f))
                HeaderHint(text = "左滑去待办", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeaderHint(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PinnedToolsCard(tools: List<ToolEntry>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "优先处理",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "这一屏只放你明确点名要重做的工具，先帮你缩小选择范围。",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            tools.forEachIndexed { index, tool ->
                ToolRow(tool = tool, emphasizeBackground = true, highlightMeta = true)
                if (index != tools.lastIndex) {
                    SettingDivider()
                }
            }
        }
    }
}

@Composable
private fun RecentToolsCard(tools: List<ToolEntry>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "继续刚才的处理",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "给一点点状态感，但不让工具页变成一面信息板。",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            tools.forEachIndexed { index, tool ->
                CompactToolRow(tool = tool)
                if (index != tools.lastIndex) {
                    SettingDivider()
                }
            }
        }
    }
}

@Composable
private fun ToolSectionCard(section: ToolSection) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = when (section.title) {
                        "整理内容" -> Icons.Default.PushPin
                        "回顾时间" -> Icons.Default.Event
                        else -> Icons.Default.AutoAwesome
                    },
                    bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    tint = MaterialTheme.colorScheme.primary,
                    size = 38.dp,
                    iconSize = 18.dp,
                    cornerRadius = 12.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = section.subtitle,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            section.items.forEachIndexed { index, item ->
                ToolRow(tool = item)
                if (index != section.items.lastIndex) {
                    SettingDivider()
                }
            }
        }
    }
}

@Composable
private fun CompactToolRow(tool: ToolEntry) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = spring(stiffness = 720f),
        label = "compactToolRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = tool.onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(
            icon = tool.icon,
            bg = sectionIconBg(tool.paletteIndex),
            tint = sectionIconTint(tool.paletteIndex),
            size = 36.dp,
            iconSize = 17.dp,
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = tool.subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = tool.meta,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ToolRow(
    tool: ToolEntry,
    emphasizeBackground: Boolean = false,
    highlightMeta: Boolean = false
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
            .background(
                when {
                    isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    emphasizeBackground -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                    else -> Color.Transparent
                }
            )
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
            bg = sectionIconBg(tool.paletteIndex),
            tint = sectionIconTint(tool.paletteIndex),
            size = 40.dp,
            iconSize = 18.dp,
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = tool.subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
            Text(
                text = tool.meta,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = if (highlightMeta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 5.dp)
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
private fun sectionIconBg(index: Int): Color {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    return when (index) {
        0 -> primary.copy(alpha = 0.12f)
        1 -> secondary.copy(alpha = 0.13f)
        2 -> tertiary.copy(alpha = 0.12f)
        3 -> primary.copy(alpha = 0.08f)
        else -> primary.copy(alpha = 0.10f)
    }
}

@Composable
private fun sectionIconTint(index: Int): Color {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    return when (index) {
        0 -> primary
        1 -> secondary
        2 -> tertiary
        3 -> Color(
            red = primary.red * 0.6f + gray.red * 0.4f,
            green = primary.green * 0.6f + gray.green * 0.4f,
            blue = primary.blue * 0.6f + gray.blue * 0.4f,
            alpha = 1f
        )
        else -> primary
    }
}
