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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.DiaryApplication
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.IconCircle
import com.diary.app.ui.components.SettingDivider
import com.diary.app.ui.experimental.ExperimentalFeaturesState

private data class ToolItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

private data class ToolSection(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val items: List<ToolItem>
)

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
        4 -> s.copy(alpha = 0.10f)
        else -> p.copy(alpha = 0.10f)
    }
}

@Composable
private fun sectionIconTint(index: Int): Color {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val t = MaterialTheme.colorScheme.tertiary
    return when (index) {
        0 -> p
        1 -> s
        2 -> t
        3 -> p
        4 -> s
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
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToAiManagement: () -> Unit = {},
    onNavigateToSmallWins: () -> Unit = {},
    onNavigateToQuickCheckin: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToWritingCoach: () -> Unit = {},
    onNavigateToVoiceRecording: () -> Unit = {},
    onNavigateToFocusMode: () -> Unit = {},
    onNavigateToImmersiveReader: () -> Unit = {},
    onNavigateToQuarterlyReview: () -> Unit = {},
    onNavigateToMemoryAnchors: () -> Unit = {},
    onNavigateToWritingFingerprint: () -> Unit = {},
    onNavigateToEmotionForecast: () -> Unit = {},
    onNavigateToRelationshipTracking: () -> Unit = {},
    onNavigateToDecisionAnalysis: () -> Unit = {},
    onNavigateToValuesExtraction: () -> Unit = {},
    onNavigateToWritingLab: () -> Unit = {},
    onNavigateToEasterEggs: () -> Unit = {},
    onNavigateToMonthlyChallenge: () -> Unit = {},
    onNavigateToStreakShield: () -> Unit = {},
    onNavigateToGentleNotification: () -> Unit = {},
    onNavigateToOutlineView: () -> Unit = {},
    onNavigateToCoverTheme: () -> Unit = {},
    onNavigateToSemanticSearch: () -> Unit = {},
    onNavigateToWritingHint: () -> Unit = {},
    onNavigateToAmbientSound: () -> Unit = {},
    onNavigateToGestureQuickAction: () -> Unit = {},
    onNavigateToLockScreenQuickWrite: () -> Unit = {},
    onNavigateToAdaptiveInterface: () -> Unit = {},
    onNavigateToPersonalYearbook: () -> Unit = {},
    onNavigateToTravelLog: () -> Unit = {},
    onNavigateToLocationMemories: () -> Unit = {},
    onNavigateToDiaryTalk: () -> Unit = {},
    onMainScreenSwipe: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)

    val featuresState = app?.experimentalFeatures?.collectAsState()
    val features = featuresState?.value ?: ExperimentalFeaturesState()

    var expandedSection by remember { mutableStateOf<String?>(null) }
    val isAiConfigured = com.diary.app.ai.AiConfigStore.isConfigured(context)

    val sections = listOf(
        ToolSection(
            key = "create",
            icon = Icons.Default.Edit,
            title = "创作记录",
            subtitle = "快速签到、语音备忘、写作工具",
            items = listOf(
                ToolItem(Icons.Default.Edit, "快速签到", "三秒完成心情记录", onNavigateToQuickCheckin),
                ToolItem(Icons.Default.Mic, "语音备忘录", "录音并自动转写为文字", onNavigateToVoiceRecording),
                ToolItem(Icons.Default.Lock, "锁屏快写", "未解锁快速记录灵感", onNavigateToLockScreenQuickWrite),
                ToolItem(Icons.Default.PanTool, "手势快捷操作", "自定义手势快速执行操作", onNavigateToGestureQuickAction),
                ToolItem(Icons.Default.Favorite, "小确幸", "记录每天的小胜利", onNavigateToSmallWins),
                ToolItem(Icons.Default.AutoAwesome, "写作灵感", "上下文感知写作提示", onNavigateToWritingHint),
                ToolItem(Icons.Default.SmartToy, "写作实验室", "实验性写作工具", onNavigateToWritingLab),
            )
        ),
        ToolSection(
            key = "analysis",
            icon = Icons.Default.Search,
            title = "数据分析",
            subtitle = "统计、情绪分析、语义搜索",
            items = listOf(
                ToolItem(Icons.Default.BarChart, "数据统计", "查看你的写作轨迹", onNavigateToStats),
                ToolItem(Icons.Default.Home, "情绪预报", "预测未来情绪走向", onNavigateToEmotionForecast),
                ToolItem(Icons.Default.Edit, "写作指纹", "分析写作风格特征", onNavigateToWritingFingerprint),
                ToolItem(Icons.Default.Person, "关系追踪", "追踪人物关系变化", onNavigateToRelationshipTracking),
                ToolItem(Icons.Default.Widgets, "决策追踪", "追踪决策与结果", onNavigateToDecisionAnalysis),
                ToolItem(Icons.Default.Favorite, "价值观", "提取日记中的价值观", onNavigateToValuesExtraction),
                ToolItem(Icons.Default.Search, "语义搜索", "基于语义的全文搜索", onNavigateToSemanticSearch),
                ToolItem(Icons.Default.EmojiEvents, "成就", "查看你的里程碑勋章", onNavigateToAchievements),
                ToolItem(Icons.Default.CalendarMonth, "季度回顾", "季度数据统计", onNavigateToQuarterlyReview),
                ToolItem(Icons.Default.CalendarMonth, "个人年鉴", "年度精华汇编", onNavigateToPersonalYearbook),
            )
        ),
        ToolSection(
            key = "ai",
            icon = Icons.Default.AutoAwesome,
            title = "AI 智能",
            subtitle = if (isAiConfigured) "已配置 API" else "未配置 API",
            items = listOf(
                ToolItem(Icons.Default.Key, "AI 配置", if (isAiConfigured) "已配置" else "点击配置 AI 密钥", onNavigateToAiManagement),
                ToolItem(Icons.Default.ChatBubbleOutline, "AI 助手", "智能写作助手小墨", onNavigateToAiAssistant),
                ToolItem(Icons.Default.AutoAwesome, "AI 传记", "AI 生成个人传记", onNavigateToBiography),
                ToolItem(Icons.Default.AutoAwesome, "与过去的自己对话", "基于日记的 AI 对话", onNavigateToDiaryTalk),
                ToolItem(Icons.Default.AutoAwesome, "写作教练", "AI 分析写作习惯", onNavigateToWritingCoach),
            )
        ),
        ToolSection(
            key = "memory",
            icon = Icons.Default.Map,
            title = "回忆旅程",
            subtitle = "倒数日、地图、回忆",
            items = listOf(
                ToolItem(Icons.Default.Timer, "倒数日", "重要日期倒计时", onNavigateToCountDown),
                ToolItem(Icons.Default.MarkEmailUnread, "时间胶囊", "给未来的自己写信", onNavigateToTimeCapsule),
                ToolItem(Icons.Default.Map, "日记地图", "在地图上回顾足迹", onNavigateToDiaryMap),
                ToolItem(Icons.Default.Shuffle, "随机回顾", "随机打开一篇日记", onNavigateToRandom),
                ToolItem(Icons.Default.Label, "记忆锚点", "关联日记中的重复元素", onNavigateToMemoryAnchors),
                ToolItem(Icons.Default.MusicNote, "场景环境音", "沉浸式写作背景音", onNavigateToAmbientSound),
                ToolItem(Icons.Default.LocationOn, "地点触发回忆", "到达旧地时提醒", onNavigateToLocationMemories),
                ToolItem(Icons.Default.Flight, "旅行日志", "旅行专用记录模式", onNavigateToTravelLog),
                ToolItem(Icons.Default.Image, "封面主题", "日记封面样式", onNavigateToCoverTheme),
                ToolItem(Icons.Default.EmojiEvents, "隐藏彩蛋", "发现日记中的彩蛋", onNavigateToEasterEggs),
            )
        ),
        ToolSection(
            key = "system",
            icon = Icons.Default.Widgets,
            title = "系统管理",
            subtitle = "标签、存储、挑战",
            items = listOf(
                ToolItem(Icons.Default.Label, "标签管理", "整理你的日记分类", onNavigateToTagManagement),
                ToolItem(Icons.Default.Collections, "媒体库", "浏览所有图片和视频", onNavigateToMediaLibrary),
                ToolItem(Icons.Default.Memory, "存储管理", "查看存储空间使用情况", onNavigateToStorage),
                ToolItem(Icons.Default.Backup, "备份", "数据备份与恢复", {}),
                ToolItem(Icons.Default.Notifications, "消息通知", "查看系统通知和提醒", onNavigateToNotifications),
                ToolItem(Icons.Default.Article, "大纲视图", "时间线式浏览日记", onNavigateToOutlineView),
                ToolItem(Icons.Default.Timer, "专注模式", "番茄钟与专注写作", onNavigateToFocusMode),
                ToolItem(Icons.Default.Article, "沉浸阅读", "电子书风格阅读日记", onNavigateToImmersiveReader),
                ToolItem(Icons.Default.BarChart, "目标追踪", "分解目标追踪进度", onNavigateToGoals),
                ToolItem(Icons.Default.CalendarMonth, "月度挑战", "每月写作挑战", onNavigateToMonthlyChallenge),
                ToolItem(Icons.Default.EmojiEvents, "连续保护罩", "保持写作连续记录", onNavigateToStreakShield),
                ToolItem(Icons.Default.AutoAwesome, "自适应界面", "智能调整界面布局", onNavigateToAdaptiveInterface),
                ToolItem(Icons.Default.Notifications, "温柔通知", "非侵入式提醒", onNavigateToGentleNotification),
                ToolItem(Icons.Default.Home, "安静陪伴", "无声陪伴模式", {}),
                ToolItem(Icons.Default.Image, "环境感知主题", "根据环境切换主题", {}),
                ToolItem(Icons.Default.Delete, "回收站", "已删除的日记", {}),
            )
        )
    )

    GradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onMainScreenSwipe) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = { onMainScreenSwipe?.invoke(totalDrag) }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                    sections.forEachIndexed { index, section ->
                        CollapsibleSection(
                            icon = section.icon,
                            iconBg = sectionIconBg(index),
                            iconTint = sectionIconTint(index),
                            title = section.title,
                            subtitle = section.subtitle,
                            isExpanded = expandedSection == section.key,
                            onToggle = { expandedSection = if (expandedSection == section.key) null else section.key },
                            textColor = textColor,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary
                        ) {
                            section.items.forEachIndexed { itemIndex, item ->
                                if (itemIndex > 0) SettingDivider()
                                ClickableToolRow(
                                    icon = item.icon,
                                    iconBg = sectionIconBg(index),
                                    iconTint = sectionIconTint(index),
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    textColor = textColor,
                                    textTertiary = textTertiary,
                                    onClick = item.onClick
                                )
                            }
                            if (section.key == "ai") {
                                SettingDivider()
                                AiFeatureToggleRow(
                                    icon = Icons.Default.Lightbulb,
                                    title = "AI 洞察卡片",
                                    subtitle = "首页偶尔出现轻量的 AI 提示",
                                    checked = features.aiInsightCardEnabled && isAiConfigured,
                                    enabled = isAiConfigured,
                                    onCheckedChange = { app?.setAiInsightCardEnabled(it) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

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

@Composable
private fun AiFeatureToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (enabled) textSecondary else textSecondary.copy(alpha = 0.5f),
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 3.dp, end = 8.dp)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            )
        )
    }
}
