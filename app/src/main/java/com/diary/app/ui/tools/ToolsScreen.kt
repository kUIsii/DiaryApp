package com.diary.app.ui.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.rotate
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
        0 -> p; 1 -> s; 2 -> t; 3 -> p; 4 -> s; else -> t
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
    onNavigateToEntryGraph: () -> Unit = {},
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

    val sections = listOf(
        ToolSection(
            key = "create",
            icon = Icons.Default.Create,
            title = "\u521B\u4F5C\u8BB0\u5F55",
            subtitle = "\u7B7E\u5230\u00B7\u8BED\u97F3\u00B7\u5199\u4F5C",
            items = listOf(
                ToolItem(Icons.Default.Mic, "\u8BED\u97F3\u7EAA\u5F55", "\u5F55\u97F3\u5E76\u8F6C\u5199\u6587\u5B57", onNavigateToVoiceRecording),
                ToolItem(Icons.Default.Lock, "\u9501\u5C4F\u5FEB\u5199", "\u4E0D\u89E3\u9501\u5FEB\u901F\u8BB0\u5F55", onNavigateToLockScreenQuickWrite),
                ToolItem(Icons.Default.PanTool, "\u5FEB\u6377\u64CD\u4F5C", "\u81EA\u5B9A\u4E49\u624B\u52BF\u52A8\u4F5C", onNavigateToGestureQuickAction),
                ToolItem(Icons.Default.AutoFixHigh, "\u5199\u4F5C\u5DE5\u5177", "\u7075\u611F\u00B7\u6559\u7EC3\u00B7\u5B9E\u9A8C\u5BA4", onNavigateToWritingLab),
                ToolItem(Icons.Default.AutoStories, "\u5199\u4F5C\u6559\u7EC3", "\u5199\u4F5C\u6559\u7EC3\u4E0E\u6307\u5BFC", onNavigateToWritingCoach),
                ToolItem(Icons.Default.Favorite, "\u5C0F\u786E\u5E78", "\u8BB0\u5F55\u751F\u6D3B\u7684\u5C0F\u786E\u5E78", onNavigateToSmallWins),
            )
        ),
        ToolSection(
            key = "reading",
            icon = Icons.Default.MenuBook,
            title = "\u6C89\u6D78\u9605\u8BFB",
            subtitle = "\u9605\u8BFB\u00B7\u7EB2\u8981\u00B7\u4E3B\u9898",
            items = listOf(
                ToolItem(Icons.Default.MenuBook, "\u6C89\u6D78\u9605\u8BFB", "\u4E13\u6CE8\u65E0\u5E72\u6270\u9605\u8BFB/\u7F16\u8F91", onNavigateToImmersiveReader),
                ToolItem(Icons.Default.Info, "\u5199\u4F5C\u63D0\u793A", "\u5B9E\u65F6\u5199\u4F5C\u5EFA\u8BAE\u4E0E\u63D0\u793A", onNavigateToWritingHint),
                ToolItem(Icons.Default.Collections, "\u7EB2\u8981\u89C6\u56FE", "\u6D4F\u89C8\u65E5\u8BB0\u7EB2\u8981\u00B7\u5FEB\u901F\u5BFC\u822A", onNavigateToOutlineView),
                ToolItem(Icons.Default.Palette, "\u5C01\u9762\u4E3B\u9898", "\u81EA\u5B9A\u4E49\u65E5\u8BB0\u5C01\u9762\u98CE\u683C", onNavigateToCoverTheme),
                ToolItem(Icons.Default.Timer, "\u4E13\u6CE8\u6A21\u5F0F", "\u4E13\u6CE8\u4E0E\u6C89\u6D78\u9605\u8BFB", onNavigateToFocusMode),
            )
        ),
        ToolSection(
            key = "analysis",
            icon = Icons.Default.TrendingUp,
            title = "\u6570\u636E\u6D1E\u5BDF",
            subtitle = "\u7EDF\u8BA1\u00B7\u60C5\u7EEA\u00B7\u8BED\u4E49",
            items = listOf(
                ToolItem(Icons.Default.BarChart, "\u6570\u636E\u603B\u89C8", "\u7EDF\u8BA1\u62A5\u544A\u00B7\u5B63\u5EA6\u00B7\u5E74\u9274", onNavigateToStats),
                ToolItem(Icons.Default.Favorite, "\u60C5\u7EEA\u5206\u6790", "\u60C5\u7EEA\u9884\u6D4B\u4E0E\u96F7\u8FBE\u56FE", onNavigateToEmotionForecast),
                ToolItem(Icons.Default.Edit, "\u5199\u4F5C\u5206\u6790", "\u98CE\u683C\u7279\u5F81\u4E0E\u6587\u5B57\u663E\u5FAE\u955C", onNavigateToWritingFingerprint),
                ToolItem(Icons.Default.Search, "\u8BED\u4E49\u641C\u7D22", "\u5168\u6587\u8BED\u4E49\u68C0\u7D22", onNavigateToSemanticSearch),
                ToolItem(Icons.Default.DateRange, "\u5B63\u5EA6\u56DE\u987E", "\u5B63\u5EA6\u5199\u4F5C\u62A5\u544A\u4E0E\u56DE\u987E", onNavigateToQuarterlyReview),
                ToolItem(Icons.Default.Share, "\u51B3\u7B56\u5206\u6790", "\u91CD\u5927\u51B3\u7B56\u56DE\u987E\u4E0E\u5206\u6790", onNavigateToDecisionAnalysis),
                ToolItem(Icons.Default.Edit, "\u4EF7\u503C\u89C2\u63D0\u53D6", "\u4ECE\u65E5\u8BB0\u4E2D\u63D0\u53D6\u4E2A\u4EBA\u4EF7\u503C\u89C2", onNavigateToValuesExtraction),
                ToolItem(Icons.Default.Share, "\u6761\u76EE\u5173\u8054\u56FE\u8C31", "\u65E5\u8BB0\u5173\u8054\u53EF\u89C6\u5316\u56FE\u8C31", onNavigateToEntryGraph),
            )
        ),
        ToolSection(
            key = "ai",
            icon = Icons.Default.AutoStories,
            title = "AI \u667A\u80FD",
            subtitle = if (isAiConfigured) "\u5DF2\u914D\u7F6E" else "\u672A\u914D\u7F6E",
            items = listOf(
                ToolItem(Icons.Default.Key, "AI \u914D\u7F6E", if (isAiConfigured) "\u5DF2\u914D\u7F6E" else "\u914D\u7F6E API \u5BC6\u94A5", onNavigateToAiManagement),
                ToolItem(Icons.Default.Forum, "AI \u5BF9\u8BDD", "\u667A\u80FD\u52A9\u624B\u4E0E\u8FC7\u53BB\u5BF9\u8BDD", onNavigateToAiAssistant),
                ToolItem(Icons.Default.Person, "AI \u4F20\u8BB0", "\u751F\u6210\u4E2A\u4EBA\u4F20\u8BB0", onNavigateToBiography),
            )
        ),
        ToolSection(
            key = "memory",
            icon = Icons.Default.History,
            title = "\u56DE\u5FC6\u65C5\u7A0B",
            subtitle = "\u80F6\u56CA\u00B7\u5730\u56FE\u00B7\u5012\u6570\u65E5",
            items = listOf(
                ToolItem(Icons.Default.AccessTime, "\u65F6\u95F4\u80F6\u56CA", "\u7ED9\u672A\u6765\u7684\u81EA\u5DF1\u5199\u4FE1", onNavigateToTimeCapsule),
                ToolItem(Icons.Default.Schedule, "\u5012\u6570\u65E5", "\u91CD\u8981\u65E5\u5B50\u5012\u8BA1\u65F6", onNavigateToCountDown),
                ToolItem(Icons.Default.Map, "\u65E5\u8BB0\u5730\u56FE", "\u8DB3\u8FF9\u56DE\u987E\u4E0E\u65C5\u884C", onNavigateToDiaryMap),
                ToolItem(Icons.Default.DirectionsWalk, "\u65C5\u884C\u65E5\u5FD7", "\u65C5\u9014\u4E2D\u7684\u89C1\u95FB\u4E0E\u8BB0\u5F55", onNavigateToTravelLog),
                ToolItem(Icons.Default.Place, "\u5730\u70B9\u8BB0\u5FC6", "\u6BCF\u4E2A\u5730\u70B9\u627F\u8F7D\u7684\u56DE\u5FC6", onNavigateToLocationMemories),
                ToolItem(Icons.Default.Bookmark, "\u8BB0\u5FC6\u951A\u70B9", "\u5730\u70B9\u89E6\u53D1\u56DE\u5FC6", onNavigateToMemoryAnchors),
                ToolItem(Icons.Default.MusicNote, "\u573A\u666F\u73AF\u5883\u97F3", "\u6C89\u6D78\u5F0F\u5199\u4F5C\u80CC\u666F\u97F3", onNavigateToAmbientSound),
                ToolItem(Icons.Default.Shuffle, "\u968F\u673A\u56DE\u987E", "\u91CD\u6E29\u65E7\u65E5\u8BB0", onNavigateToRandom),
                ToolItem(Icons.Default.DateRange, "\u4E2A\u4EBA\u5E74\u9274", "\u4E00\u5E74\u7684\u56DE\u5FC6\u4E0E\u6210\u957F\u62A5\u544A", onNavigateToPersonalYearbook),
            )
        ),
        ToolSection(
            key = "tools",
            icon = Icons.Default.Build,
            title = "\u7CFB\u7EDF\u5DE5\u5177",
            subtitle = "\u6807\u7B7E\u00B7\u5A92\u4F53\u00B7\u6210\u5C31",
            items = listOf(
                ToolItem(Icons.Default.Label, "\u6807\u7B7E\u7BA1\u7406", "\u6574\u7406\u65E5\u8BB0\u5206\u7C7B", onNavigateToTagManagement),
                ToolItem(Icons.Default.Collections, "\u5A92\u4F53\u5E93", "\u6D4F\u89C8\u56FE\u7247\u548C\u89C6\u9891", onNavigateToMediaLibrary),
                ToolItem(Icons.Default.Star, "\u76EE\u6807\u00B7\u52C7\u7AE0", "\u76EE\u6807\u00B7\u52C7\u7AE0\u00B7\u6311\u6218", onNavigateToGoals),
                ToolItem(Icons.Default.EmojiEvents, "\u6210\u5C31", "\u5DF2\u89E3\u9501\u7684\u80CC\u666F\u4E0E\u52C7\u7AE0\u5899", onNavigateToAchievements),
                ToolItem(Icons.Default.Storage, "\u5B58\u50A8\u7BA1\u7406", "\u7F13\u5B58\u00B7\u6570\u636E\u00B7\u5907\u4EFD\u7BA1\u7406", onNavigateToStorage),
                ToolItem(Icons.Default.Notifications, "\u901A\u77E5\u7BA1\u7406", "\u63A7\u5236\u5404\u7C7B\u63A8\u9001\u901A\u77E5", onNavigateToNotifications),
                ToolItem(Icons.Default.Delete, "\u56DE\u6536\u7AD9", "\u5DF2\u5220\u9664\u7684\u65E5\u8BB0", onNavigateToTrash),
                ToolItem(Icons.Default.Star, "\u6708\u5EA6\u6311\u6218", "\u6BCF\u6708\u5199\u4F5C\u6311\u6218\u4E0E\u76EE\u6807", onNavigateToMonthlyChallenge),
                ToolItem(Icons.Default.EmojiEvents, "\u8FDE\u7EED\u4FDD\u62A4", "\u4FDD\u62A4\u8FDE\u7EED\u5199\u4F5C\u8BB0\u5F55", onNavigateToStreakShield),
                ToolItem(Icons.Default.Favorite, "\u9690\u85CF\u5F69\u86CB", "\u6709\u8DA3\u7684\u9690\u85CF\u529F\u80FD\u4E0E\u5F69\u86CB", onNavigateToEasterEggs),
                ToolItem(Icons.Default.Notifications, "\u6E29\u67D4\u901A\u77E5", "\u6E29\u67D4\u7684\u63D0\u9192\u65B9\u5F0F", onNavigateToGentleNotification),
                ToolItem(Icons.Default.Settings, "\u81EA\u9002\u5E94\u754C\u9762", "\u81EA\u5B9A\u4E49\u754C\u9762\u00B7\u9690\u85CF\u529F\u80FD", onNavigateToAdaptiveInterface),
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
                    text = "\u5DE5\u5177",
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
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrowRotation"
    )

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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = section.icon, contentDescription = null, tint = c, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(section.subtitle, fontSize = 11.sp, color = textTertiary, modifier = Modifier.padding(top = 1.dp))
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    modifier = Modifier.size(24.dp).rotate(arrowRotation),
                    tint = textSecondary
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
                    section.items.forEachIndexed { i, item ->
                        if (i > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
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
                        Spacer(modifier = Modifier.height(2.dp))
                        AiFeatureToggleRow(
                            icon = Icons.Default.Lightbulb,
                            title = "AI \u6D1E\u5BDF\u5361\u7247",
                            subtitle = "\u9996\u9875\u5076\u73B0\u8F7B\u91CF AI \u63D0\u793A",
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
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
