package com.diary.app.ui.tools

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
private fun sectionColor(index: Int): Color {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val t = MaterialTheme.colorScheme.tertiary
    return when (index) {
        0 -> p; 1 -> s; 2 -> t; 3 -> p; else -> s
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
    onNavigateToTrash: () -> Unit = {},
    onMainScreenSwipe: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = textSecondary.copy(alpha = 0.82f)

    val featuresState = app?.experimentalFeatures?.collectAsState()
    val features = featuresState?.value ?: ExperimentalFeaturesState()
    val isAiConfigured = com.diary.app.ai.AiConfigStore.isConfigured(context)

    var expandedSection by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val sections = listOf(
        ToolSection(
            key = "create",
            icon = Icons.Default.BorderColor,
            title = "创作记录",
            subtitle = "签到·语音·写作",
            items = listOf(
                ToolItem(Icons.Default.NoteAlt, "快速签到", "心情记录与小确幸", onNavigateToQuickCheckin),
                ToolItem(Icons.Default.Mic, "语音备忘录", "录音并转写文字", onNavigateToVoiceRecording),
                ToolItem(Icons.Default.Lock, "锁屏快写", "不解锁快速记录", onNavigateToLockScreenQuickWrite),
                ToolItem(Icons.Default.PanTool, "手势操作", "自定义手势动作", onNavigateToGestureQuickAction),
                ToolItem(Icons.Default.AutoFixHigh, "写作工坊", "灵感·教练·实验", onNavigateToWritingLab),
            )
        ),
        ToolSection(
            key = "analysis",
            icon = Icons.Default.BarChart,
            title = "数据分析",
            subtitle = "统计·情绪·语义",
            items = listOf(
                ToolItem(Icons.Default.BarChart, "数据总览", "统计报告·季度·年鉴", onNavigateToStats),
                ToolItem(Icons.Default.Mood, "情绪分析", "预测情绪走势", onNavigateToEmotionForecast),
                ToolItem(Icons.Default.Edit, "写作指纹", "分析写作风格特征", onNavigateToWritingFingerprint),
                ToolItem(Icons.Default.Group, "关系追踪", "追踪人物关系变化", onNavigateToRelationshipTracking),
                ToolItem(Icons.Default.Search, "语义搜索", "全文语义检索", onNavigateToSemanticSearch),
            )
        ),
        ToolSection(
            key = "ai",
            icon = Icons.Default.SmartToy,
            title = "AI 智能",
            subtitle = if (isAiConfigured) "已配置" else "未配置",
            items = listOf(
                ToolItem(Icons.Default.Key, "AI 配置", if (isAiConfigured) "已配置" else "配置 API 密钥", onNavigateToAiManagement),
                ToolItem(Icons.Default.SmartToy, "AI 助手", "智能写作助手小墨", onNavigateToAiAssistant),
                ToolItem(Icons.Default.AutoStories, "AI 传记", "生成个人传记", onNavigateToBiography),
                ToolItem(Icons.Default.Forum, "与过去对话", "基于日记的 AI 对话", onNavigateToDiaryTalk),
            )
        ),
        ToolSection(
            key = "memory",
            icon = Icons.Default.History,
            title = "回忆旅程",
            subtitle = "倒数日·地图·环境音",
            items = listOf(
                ToolItem(Icons.Default.Timer, "倒数日", "重要日期倒计时", onNavigateToCountDown),
                ToolItem(Icons.Default.MarkEmailUnread, "时间胶囊", "给未来的自己写信", onNavigateToTimeCapsule),
                ToolItem(Icons.Default.Map, "日记地图", "足迹回顾与旅行", onNavigateToDiaryMap),
                ToolItem(Icons.Default.Shuffle, "随机回顾", "重温旧日记", onNavigateToRandom),
                ToolItem(Icons.Default.MusicNote, "场景环境音", "沉浸式写作背景音", onNavigateToAmbientSound),
            )
        ),
        ToolSection(
            key = "tools",
            icon = Icons.Default.Build,
            title = "系统工具",
            subtitle = "标签·阅读·成就",
            items = listOf(
                ToolItem(Icons.Default.Label, "标签管理", "整理日记分类", onNavigateToTagManagement),
                ToolItem(Icons.Default.Collections, "媒体库", "浏览图片和视频", onNavigateToMediaLibrary),
                ToolItem(Icons.Default.MenuBook, "沉浸阅读", "阅读与专注模式", onNavigateToImmersiveReader),
                ToolItem(Icons.Default.Star, "成就中心", "勋章·挑战·连续", onNavigateToAchievements),
                ToolItem(Icons.Default.Delete, "回收站", "已删除的日记", onNavigateToTrash),
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
                            totalDrag += dragAmount; change.consume()
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(sections) { section ->
                        SectionCard(
                            section = section,
                            isExpanded = expandedSection == section.key,
                            onToggle = {
                                expandedSection = if (expandedSection == section.key) null else section.key
                            },
                            textColor = textColor,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary,
                            sectionIndex = sections.indexOf(section),
                            isAiConfigured = isAiConfigured,
                            features = features,
                            app = app
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: ToolSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color,
    sectionIndex: Int,
    isAiConfigured: Boolean,
    features: ExperimentalFeaturesState,
    app: DiaryApplication?
) {
    val c = sectionColor(sectionIndex)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Column {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (pressed) c.copy(alpha = 0.06f) else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(icon = section.icon, bg = c.copy(alpha = 0.12f), tint = c)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(section.subtitle, fontSize = 11.sp, color = textTertiary, modifier = Modifier.padding(top = 1.dp))
                }
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
                    section.items.forEachIndexed { i, item ->
                        if (i > 0) SettingDivider()
                        ToolRow(
                            icon = item.icon,
                            tint = c,
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
                            subtitle = "首页偶现轻量 AI 提示",
                            checked = features.aiInsightCardEnabled && isAiConfigured,
                            enabled = isAiConfigured,
                            onCheckedChange = { app?.setAiInsightCardEnabled(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    textTertiary: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) tint.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(icon = icon, bg = tint.copy(alpha = 0.12f), tint = tint)
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
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = if (enabled) textColor else textColor.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = if (enabled) textSecondary else textSecondary.copy(alpha = 0.5f), lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp, end = 8.dp))
        }
        Switch(checked = checked, onCheckedChange = if (enabled) onCheckedChange else null,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        )
    }
}
