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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.diary.app.ui.components.ScreenTopBar
import com.diary.app.ui.theme.DesignTokens
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
private fun sectionColor(key: String): Color {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val t = MaterialTheme.colorScheme.tertiary
    val v = MaterialTheme.colorScheme.onSurfaceVariant
    // Color is tied to the section's meaning, not its position,
    // so it stays stable even if sections are reordered.
    return when (key) {
        "analysis" -> p
        "ai" -> t
        "memory" -> s
        "immersion" -> p
        "tools" -> v
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
    onNavigateToWritingCoach: () -> Unit = {},
    onNavigateToWritingFingerprint: () -> Unit = {},
    onNavigateToFocusMode: () -> Unit = {},
    onNavigateToAmbientSound: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onMainScreenSwipe: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val featuresState = app?.experimentalFeatures?.collectAsState()
    val features = featuresState?.value ?: ExperimentalFeaturesState()
    val isAiConfigured = com.diary.app.ai.AiConfigStore.isConfigured(context)

    var expandedSection by remember { mutableStateOf<String?>(null) }

    val sections = listOf(
        ToolSection(
            key = "analysis",
            icon = Icons.Default.TrendingUp,
            title = "\u6570\u636E\u5206\u6790",
            subtitle = "\u7EDF\u8BA1\u00B7\u6210\u5C31\u00B7\u56DE\u987E",
            items = listOf(
                ToolItem(Icons.Default.BarChart, "\u6570\u636E\u603B\u89C8", "\u7EDF\u8BA1\u3001\u70ED\u529B\u56FE\u4E0E\u5199\u4F5C\u5206\u6790", onNavigateToStats),
                ToolItem(Icons.Default.Spellcheck, "\u5199\u4F5C\u5206\u6790", "\u98CE\u683C\u7279\u5F81\u4E0E\u6587\u5B57\u663E\u5FAE\u955C", onNavigateToWritingFingerprint),
                ToolItem(Icons.Default.EmojiEvents, "\u6210\u5C31", "\u5DF2\u89E3\u9501\u7684\u80CC\u666F\u4E0E\u52C7\u7AE0\u5899", onNavigateToAchievements),
                ToolItem(Icons.Default.Shuffle, "\u968F\u673A\u56DE\u987E", "\u91CD\u6E29\u65E7\u65E5\u8BB0", onNavigateToRandom),
            )
        ),
        ToolSection(
            key = "ai",
            icon = Icons.Default.SmartToy,
            title = "AI \u667A\u80FD",
            subtitle = if (isAiConfigured) "\u5DF2\u914D\u7F6E" else "\u672A\u914D\u7F6E",
            items = listOf(
                ToolItem(Icons.Default.Key, "AI \u914D\u7F6E", if (isAiConfigured) "\u5DF2\u914D\u7F6E" else "\u914D\u7F6E API \u5BC6\u94A5", onNavigateToAiManagement),
                ToolItem(Icons.Default.Chat, "AI \u5BF9\u8BDD", "\u667A\u80FD\u52A9\u624B\u4E0E\u8FC7\u53BB\u5BF9\u8BDD", onNavigateToAiAssistant),
                ToolItem(Icons.Default.School, "\u5199\u4F5C\u6559\u7EC3", "\u5199\u4F5C\u7EDF\u8BA1\u4E0E AI \u6307\u5BFC", onNavigateToWritingCoach),
                ToolItem(Icons.Default.Person, "AI \u4F20\u8BB0", "\u751F\u6210\u4E2A\u4EBA\u4F20\u8BB0", onNavigateToBiography),
            )
        ),
        ToolSection(
            key = "memory",
            icon = Icons.Default.History,
            title = "\u56DE\u5FC6\u65C5\u7A0B",
            subtitle = "\u80F6\u56CA\u00B7\u5012\u8BA1\u65F6\u00B7\u5730\u56FE",
            items = listOf(
                ToolItem(Icons.Default.Timer, "\u65F6\u95F4\u80F6\u56CA", "\u7ED9\u672A\u6765\u7684\u81EA\u5DF1\u5199\u4FE1", onNavigateToTimeCapsule),
                ToolItem(Icons.Default.CalendarMonth, "\u5012\u6570\u65E5", "\u91CD\u8981\u65E5\u5B50\u5012\u8BA1\u65F6", onNavigateToCountDown),
                ToolItem(Icons.Default.Map, "\u65E5\u8BB0\u5730\u56FE", "\u8DB3\u8FF9\u56DE\u987E\u4E0E\u65C5\u884C", onNavigateToDiaryMap),
            )
        ),
        ToolSection(
            key = "immersion",
            icon = Icons.Default.Headphones,
            title = "\u6C89\u6D78",
            subtitle = "\u4E13\u6CE8\u00B7\u73AF\u5883\u97F3",
            items = listOf(
                ToolItem(Icons.Default.Timer, "\u4E13\u6CE8\u6A21\u5F0F", "\u4E13\u6CE8\u5199\u4F5C\u4E0E\u756A\u8304\u949F", onNavigateToFocusMode),
                ToolItem(Icons.Default.Headphones, "\u8212\u7F13\u73AF\u5883\u97F3", "\u6C89\u6D78\u5F0F\u5199\u4F5C\u80CC\u666F\u97F3", onNavigateToAmbientSound),
            )
        ),
        ToolSection(
            key = "tools",
            icon = Icons.Default.Build,
            title = "\u7CFB\u7EDF\u5DE5\u5177",
            subtitle = "\u6807\u7B7E\u00B7\u5A92\u4F53\u00B7\u5B58\u50A8",
            items = listOf(
                ToolItem(Icons.Default.Label, "\u6807\u7B7E\u7BA1\u7406", "\u6574\u7406\u65E5\u8BB0\u5206\u7C7B", onNavigateToTagManagement),
                ToolItem(Icons.Default.Collections, "\u5A92\u4F53\u5E93", "\u6D4F\u89C8\u56FE\u7247\u548C\u89C6\u9891", onNavigateToMediaLibrary),
                ToolItem(Icons.Default.Storage, "\u5B58\u50A8\u7BA1\u7406", "\u7F13\u5B58\u00B7\u6570\u636E\u00B7\u5907\u4EFD\u7BA1\u7406", onNavigateToStorage),
                ToolItem(Icons.Default.Notifications, "\u901A\u77E5\u7BA1\u7406", "\u63A7\u5236\u5404\u7C7B\u63A8\u9001\u901A\u77E5", onNavigateToNotifications),
                ToolItem(Icons.Default.Delete, "\u56DE\u6536\u7AD9", "\u5DF2\u5220\u9664\u7684\u65E5\u8BB0", onNavigateToTrash),
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
                ScreenTopBar(title = "\u5DE5\u5177")

                Spacer(modifier = Modifier.height(DesignTokens.TopBarGap))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = DesignTokens.PageMargin),
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
    sectionIndex: Int,
    isAiConfigured: Boolean,
    features: ExperimentalFeaturesState,
    app: DiaryApplication?
) {
    val c = sectionColor(section.key)
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
                    Text(section.subtitle, fontSize = 11.sp, color = textSecondary, modifier = Modifier.padding(top = 1.dp))
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "\u6536\u8D77" else "\u5C55\u5F00",
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
                            textSecondary = textSecondary,
                            onClick = item.onClick
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
    textSecondary: Color,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
            Text(subtitle, fontSize = 11.sp, color = textSecondary, modifier = Modifier.padding(top = 1.dp))
        }
    }
}
