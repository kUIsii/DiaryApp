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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
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

private data class FocusTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)

private data class ToolGroup(
    val title: String,
    val subtitle: String,
    val badge: String,
    val items: List<FocusTool>
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
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)

    val toolsToRebuild = listOf(
        FocusTool(
            title = "标签管理",
            subtitle = "整理分类，减少后续检索负担",
            icon = Icons.Default.Tag,
            iconBg = sectionIconBg(0),
            iconTint = sectionIconTint(0),
            onClick = onNavigateToTagManagement
        ),
        FocusTool(
            title = "倒数日",
            subtitle = "把纪念日和节点放进真正可执行的列表",
            icon = Icons.Default.Timer,
            iconBg = sectionIconBg(1),
            iconTint = sectionIconTint(1),
            onClick = onNavigateToCountDown
        ),
        FocusTool(
            title = "日记地图",
            subtitle = "把位置回忆整理成地点目录",
            icon = Icons.Default.Map,
            iconBg = sectionIconBg(2),
            iconTint = sectionIconTint(2),
            onClick = onNavigateToDiaryMap
        )
    )

    val primaryTools = listOf(
        FocusTool("标签管理", "日记分类与标签维护", Icons.Default.Tag, sectionIconBg(0), sectionIconTint(0), onNavigateToTagManagement),
        FocusTool("倒数日", "重要日期与后续动作", Icons.Default.Timer, sectionIconBg(1), sectionIconTint(1), onNavigateToCountDown),
        FocusTool("日记地图", "地点目录与位置回顾", Icons.Default.Map, sectionIconBg(2), sectionIconTint(2), onNavigateToDiaryMap),
        FocusTool("AI 助手", "围绕记录内容做即时协助", Icons.Default.ChatBubbleOutline, sectionIconBg(3), sectionIconTint(3), onNavigateToAiAssistant)
    )

    val groups = listOf(
        ToolGroup(
            title = "整理与检索",
            subtitle = "围绕内容资产组织日记、图片和标签",
            badge = "3 项",
            items = listOf(
                FocusTool("标签管理", "统一主题、分类和筛选入口", Icons.Default.Tag, sectionIconBg(0), sectionIconTint(0), onNavigateToTagManagement),
                FocusTool("媒体库", "查看图片、视频和附件", Icons.Default.Collections, sectionIconBg(0), sectionIconTint(0), onNavigateToMediaLibrary),
                FocusTool("统计", "回看记录量和写作节奏", Icons.Default.BarChart, sectionIconBg(0), sectionIconTint(0), onNavigateToStats)
            )
        ),
        ToolGroup(
            title = "回看与安排",
            subtitle = "把回忆和重要节点整理成能继续行动的入口",
            badge = "4 项",
            items = listOf(
                FocusTool("倒数日", "纪念日、目标日和临近提醒", Icons.Default.Timer, sectionIconBg(1), sectionIconTint(1), onNavigateToCountDown),
                FocusTool("日记地图", "地点聚合与路线回顾", Icons.Default.Map, sectionIconBg(2), sectionIconTint(2), onNavigateToDiaryMap),
                FocusTool("时间胶囊", "写给未来的内容与开启节点", Icons.Default.MarkEmailUnread, sectionIconBg(1), sectionIconTint(1), onNavigateToTimeCapsule),
                FocusTool("随机回顾", "随机翻到一篇旧日记", Icons.Default.Shuffle, sectionIconBg(1), sectionIconTint(1), onNavigateToRandom)
            )
        ),
        ToolGroup(
            title = "智能与系统",
            subtitle = "保留必要入口，低频功能不抢主屏注意力",
            badge = "4 项",
            items = listOf(
                FocusTool("AI 助手", "对话式整理与写作支持", Icons.Default.ChatBubbleOutline, sectionIconBg(3), sectionIconTint(3), onNavigateToAiAssistant),
                FocusTool("AI 传记", "生成个人传记与阶段总结", Icons.Default.AutoAwesome, sectionIconBg(3), sectionIconTint(3), onNavigateToBiography),
                FocusTool("消息通知", "系统提醒、回顾与动态", Icons.Default.Notifications, sectionIconBg(3), sectionIconTint(3), onNavigateToNotifications),
                FocusTool("实验功能", "放置仍在验证的能力", Icons.Default.Tune, sectionIconBg(3), sectionIconTint(3), onNavigateToExperimental)
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
            ToolsHeroCard(textColor = textColor, textSecondary = textSecondary)
            ToolsPriorityCard(
                textColor = textColor,
                textSecondary = textSecondary,
                toolsToRebuild = toolsToRebuild
            )
            ToolsPrimarySection(primaryTools = primaryTools)

            groups.forEach { group ->
                ToolGroupCard(
                    group = group,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                )
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

@Composable
private fun ToolsHeroCard(
    textColor: Color,
    textSecondary: Color
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        innerPadding = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
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
                        color = textColor
                    )
                    Text(
                        text = "把高频动作和功能目录拆开，先给入口，再给结构。",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroSignal(
                    label = "右滑可回时间线",
                    textColor = textColor,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                HeroSignal(
                    label = "左滑可去待办",
                    textColor = textColor,
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroSignal(
    label: String,
    textColor: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToolsPriorityCard(
    textColor: Color,
    textSecondary: Color,
    toolsToRebuild: List<FocusTool>
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "当前优先重做",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = "这次把你最在意的几个工具功能抬到前面，不再让真正需要重做的页面藏在大杂烩入口里。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = textSecondary
            )
            toolsToRebuild.forEach { tool ->
                PriorityToolRow(tool = tool)
            }
        }
    }
}

@Composable
private fun PriorityToolRow(tool: FocusTool) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .clickable(onClick = tool.onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(
            icon = tool.icon,
            bg = tool.iconBg,
            tint = tool.iconTint,
            size = 38.dp,
            iconSize = 18.dp,
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
        Icon(
            imageVector = Icons.Default.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ToolsPrimarySection(primaryTools: List<FocusTool>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "常用入口",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            primaryTools.forEachIndexed { index, tool ->
                ToolRow(tool = tool)
                if (index != primaryTools.lastIndex) {
                    SettingDivider()
                }
            }
        }
    }
}

@Composable
private fun ToolGroupCard(
    group: ToolGroup,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = Icons.Default.FolderOpen,
                    bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    tint = MaterialTheme.colorScheme.primary,
                    size = 38.dp,
                    iconSize = 18.dp,
                    cornerRadius = 12.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = group.subtitle,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = textTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = group.badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }

            group.items.forEachIndexed { index, tool ->
                ToolRow(tool = tool)
                if (index != group.items.lastIndex) {
                    SettingDivider()
                }
            }
        }
    }
}

@Composable
private fun ToolRow(tool: FocusTool) {
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
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = tool.onClick
            )
            .padding(vertical = 12.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(
            icon = tool.icon,
            bg = tool.iconBg,
            tint = tool.iconTint,
            size = 38.dp,
            iconSize = 18.dp,
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, end = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

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
            red = p.red * 0.6f + gray.red * 0.4f,
            green = p.green * 0.6f + gray.green * 0.4f,
            blue = p.blue * 0.6f + gray.blue * 0.4f,
            alpha = 1f
        )
        else -> p
    }
}
