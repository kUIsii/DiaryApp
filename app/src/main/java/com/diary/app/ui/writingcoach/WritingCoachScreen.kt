package com.diary.app.ui.writingcoach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.WritingCoach
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DesignTokens
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingCoachScreen(
    onNavigateBack: () -> Unit,
    viewModel: WritingCoachViewModel = viewModel()
) {
    val analysis by viewModel.analysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val currentStats by viewModel.currentStats.collectAsState()
    val previousStats by viewModel.previousStats.collectAsState()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiEnabled = viewModel.aiEnabled
    val dailyWordGoal by viewModel.dailyWordGoal.collectAsState()
    val weeklyDayGoal by viewModel.weeklyDayGoal.collectAsState()
    val todayWordCount by viewModel.todayWordCount.collectAsState()
    val thisWeekWritingDays by viewModel.thisWeekWritingDays.collectAsState()
    val trendChartData by viewModel.trendChartData.collectAsState()
    val selectedTrendMetric by viewModel.selectedTrendMetric.collectAsState()
    val hourDistribution by viewModel.hourDistribution.collectAsState()
    val aiSuggestions by viewModel.aiSuggestions.collectAsState()

    var aiExpanded by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.analyze()
    }

    if (showGoalSheet) {
        GoalBottomSheet(
            dailyWordGoal = dailyWordGoal,
            weeklyDayGoal = weeklyDayGoal,
            onDailyGoalChange = { viewModel.setDailyWordGoal(it) },
            onWeeklyGoalChange = { viewModel.setWeeklyDayGoal(it) },
            onDismiss = { showGoalSheet = false }
        )
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "写作教练",
                    fontSize = DesignTokens.FontHeadline,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { viewModel.analyze() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重新分析")
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (analysis == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据，请先写几篇日记", fontSize = DesignTokens.FontBody)
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    AiAnalysisCard(
                        aiAnalysisResult = aiAnalysisResult,
                        isAiLoading = isAiLoading,
                        aiEnabled = aiEnabled,
                        expanded = aiExpanded,
                        onToggle = { aiExpanded = !aiExpanded },
                        onAnalyze = { viewModel.requestAiAnalysis() }
                    )

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    TimeRangeChips(
                        selected = selectedTimeRange,
                        onSelect = { viewModel.selectTimeRange(it) }
                    )

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.CornerLarge,
                        innerPadding = DesignTokens.SpacingLg
                    ) {
                        Column {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(DesignTokens.IconLarge)
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text(
                                text = analysis!!.writingTimePattern,
                                fontSize = DesignTokens.FontTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                            Text(
                                text = "基于${analysis!!.totalEntries}篇日记分析",
                                fontSize = DesignTokens.FontBody,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.CornerLarge,
                        innerPadding = DesignTokens.SpacingLg
                    ) {
                        Column {
                            Text(
                                "写作统计",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                            StatRowWithTrend(
                                label = "平均字数",
                                value = "${(currentStats?.avgWordCount ?: analysis!!.avgWordCount).roundToInt()}字",
                                trendPercent = calcTrend(
                                    currentStats?.avgWordCount ?: analysis!!.avgWordCount,
                                    previousStats?.avgWordCount
                                )
                            )
                            StatRowWithTrend(
                                label = "平均句长",
                                value = "${(currentStats?.avgSentenceLength ?: analysis!!.avgSentenceLength).roundToInt()}字",
                                trendPercent = calcTrend(
                                    currentStats?.avgSentenceLength ?: analysis!!.avgSentenceLength,
                                    previousStats?.avgSentenceLength
                                )
                            )
                            StatRowWithTrend(
                                label = "词汇丰富度",
                                value = "${((currentStats?.vocabularyRichness ?: analysis!!.vocabularyRichness) * 100).roundToInt()}%",
                                trendPercent = calcTrend(
                                    currentStats?.vocabularyRichness ?: analysis!!.vocabularyRichness,
                                    previousStats?.vocabularyRichness
                                )
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                            Text(
                                "写作时段",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            HourDistributionChart(hourDistribution)
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.CornerLarge,
                        innerPadding = DesignTokens.SpacingLg
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "写作目标",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = { showGoalSheet = true }
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "目标设置",
                                        modifier = Modifier.size(DesignTokens.IconMedium)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                            Text(
                                "每日字数目标",
                                fontSize = DesignTokens.FontBody,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                            val wordProgress = (todayWordCount.toFloat() / dailyWordGoal).coerceAtMost(1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(wordProgress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                            Text(
                                "今日已写 ${todayWordCount} / ${dailyWordGoal} 字",
                                fontSize = DesignTokens.FontSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                            Text(
                                "每周天数目标",
                                fontSize = DesignTokens.FontBody,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                                for (i in 1..7) {
                                    val filled = i <= thisWeekWritingDays
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (filled) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$i",
                                            fontSize = DesignTokens.FontSmall,
                                            color = if (filled) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                            Text(
                                "本周已写 ${thisWeekWritingDays}/7 天",
                                fontSize = DesignTokens.FontSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.CornerLarge,
                        innerPadding = DesignTokens.SpacingLg
                    ) {
                        Column {
                            Text(
                                "进步趋势",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                                TrendMetric.entries.forEach { metric ->
                                    FilterChip(
                                        selected = selectedTrendMetric == metric,
                                        onClick = { viewModel.selectTrendMetric(metric) },
                                        label = { Text(metric.label, fontSize = DesignTokens.FontSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                            if (trendChartData.size < 2) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "数据不足",
                                        fontSize = DesignTokens.FontBody,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                TrendLineChart(
                                    data = trendChartData,
                                    metric = selectedTrendMetric
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    if (analysis!!.topRepeatedWords.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = DesignTokens.CornerLarge,
                            innerPadding = DesignTokens.SpacingLg
                        ) {
                            Column {
                                Text(
                                    "常用词汇",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                                analysis!!.topRepeatedWords.take(5).forEach { (word, count) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 44.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(word, fontSize = DesignTokens.FontBody)
                                        Text(
                                            "${count}次",
                                            fontSize = DesignTokens.FontBody,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    if (analysis!!.emotionDistribution.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = DesignTokens.CornerLarge,
                            innerPadding = DesignTokens.SpacingLg
                        ) {
                            Column {
                                Text(
                                    "情绪分布",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                                analysis!!.emotionDistribution.forEach { (mood, count) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 44.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(mood, fontSize = DesignTokens.FontBody)
                                        Text(
                                            "${count}篇",
                                            fontSize = DesignTokens.FontBody,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    if (aiSuggestions.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = DesignTokens.CornerLarge,
                            innerPadding = DesignTokens.SpacingLg
                        ) {
                            Column {
                                Text(
                                    "写作建议",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                                aiSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = DesignTokens.SpacingXs)
                                    ) {
                                        val tagColor = if (suggestion.isAiGenerated)
                                            Color(0xFF7B1FA2)
                                        else
                                            Color.Gray
                                        val tagText = if (suggestion.isAiGenerated) "AI 建议" else "系统建议"
                                        Box(
                                            modifier = Modifier
                                                .padding(end = DesignTokens.SpacingSm)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(tagColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                tagText,
                                                fontSize = DesignTokens.FontCaption,
                                                color = tagColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            suggestion.text,
                                            fontSize = DesignTokens.FontBody,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
private fun AiAnalysisCard(
    aiAnalysisResult: AiAnalysisResult?,
    isAiLoading: Boolean,
    aiEnabled: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAnalyze: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .heightIn(min = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Text(
                        "AI 写作分析",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "收起" else "展开"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    if (!aiEnabled) {
                        Text(
                            "AI 分析暂不可用",
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (isAiLoading) {
                        AiShimmer()
                    } else if (aiAnalysisResult != null) {
                        AiResultItem("写作风格", aiAnalysisResult.writingStyle)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        AiResultItem("情感轨迹", aiAnalysisResult.emotionTrack)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        AiResultItem("主题偏好", aiAnalysisResult.themePreference)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        AiResultItem("一句话点评", aiAnalysisResult.summary)
                    } else {
                        Button(
                            onClick = onAnalyze,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("开始 AI 分析")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiResultItem(label: String, value: String) {
    Column {
        Text(
            label,
            fontSize = DesignTokens.FontSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
        Text(
            if (value.isNotBlank()) value else "暂无数据",
            fontSize = DesignTokens.FontBody,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AiShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "aiShimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Column {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeChips(
    selected: TimeRange,
    onSelect: (TimeRange) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
        TimeRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(range.label, fontSize = DesignTokens.FontBody) }
            )
        }
    }
}

@Composable
private fun StatRowWithTrend(
    label: String,
    value: String,
    trendPercent: Float?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = DesignTokens.FontBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trendPercent != null) {
                val arrowColor = when {
                    trendPercent > 0f -> Color(0xFF4CAF50)
                    trendPercent < 0f -> Color(0xFFE53935)
                    else -> Color.Gray
                }
                val arrow = when {
                    trendPercent > 0f -> "\u2191"
                    trendPercent < 0f -> "\u2193"
                    else -> "\u2014"
                }
                Text(
                    "$arrow ${kotlin.math.abs(trendPercent).roundToInt()}%",
                    fontSize = DesignTokens.FontSmall,
                    color = arrowColor,
                    modifier = Modifier.padding(end = DesignTokens.SpacingSm)
                )
            }
            Text(
                text = value,
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HourDistributionChart(hours: List<Int>) {
    val maxCount = hours.max().coerceAtLeast(1)
    val segmentLabels = listOf("凌晨", "上午", "下午", "晚上", "深夜")
    val segmentColors = listOf(
        Color(0xFF1A237E),
        Color(0xFFFF9800),
        Color(0xFF1976D2),
        Color(0xFF7B1FA2),
        Color(0xFF1A237E)
    )

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val barWidth = size.width / 24f
            hours.forEachIndexed { hour, count ->
                val barHeight = (count.toFloat() / maxCount) * (size.height - 8f)
                val x = hour * barWidth
                val y = size.height - barHeight
                val color = when (hour) {
                    in 0..5 -> Color(0xFF1A237E)
                    in 6..11 -> Color(0xFFFF9800)
                    in 12..17 -> Color(0xFF1976D2)
                    in 18..22 -> Color(0xFF7B1FA2)
                    else -> Color(0xFF1A237E)
                }
                drawRect(color, Offset(x, y), Size(barWidth - 1f, barHeight.coerceAtLeast(0f)))
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            segmentLabels.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(segmentColors[i])
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                    Text(
                        label,
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendLineChart(
    data: List<WritingWeeklyStats>,
    metric: TrendMetric
) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val dotFill = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
    ) {
        val values = data.map { stat ->
            when (metric) {
                TrendMetric.WORDS -> stat.avgWordCount
                TrendMetric.VOCAB -> stat.vocabularyRichness
                TrendMetric.SENTENCE -> stat.avgSentenceLength
            }
        }
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).coerceAtLeast(0.1f)

        val padLeft = 44f
        val padRight = 16f
        val padTop = 16f
        val padBottom = 36f
        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padTop - padBottom

        val points = data.mapIndexed { i, stat ->
            val v = when (metric) {
                TrendMetric.WORDS -> stat.avgWordCount
                TrendMetric.VOCAB -> stat.vocabularyRichness
                TrendMetric.SENTENCE -> stat.avgSentenceLength
            }
            val x = padLeft + if (data.size > 1) (i.toFloat() / (data.size - 1)) * chartW else chartW / 2f
            val y = padTop + chartH - ((v - minV) / range) * chartH
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(lineColor, points[i], points[i + 1], strokeWidth = 3f)
        }

        points.forEach { pt ->
            drawCircle(lineColor, 5f, pt)
            drawCircle(dotFill, 3f, pt)
        }

        val ySteps = 4
        for (i in 0..ySteps) {
            val y = padTop + (i.toFloat() / ySteps) * chartH
            val label = (minV + (range * (ySteps - i).toFloat() / ySteps)).let {
                if (metric == TrendMetric.VOCAB) String.format("%.0f%%", it * 100) else String.format("%.0f", it)
            }
            drawLine(gridColor, Offset(padLeft, y), Offset(size.width - padRight, y), strokeWidth = 1f)
            val textResult = textMeasurer.measure(
                text = label,
                style = TextStyle(fontSize = 10.sp, color = Color.Gray)
            )
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(0f, y - textResult.size.height / 2f)
            )
        }

        val labelInterval = if (data.size > 6) data.size / 4 else 1
        data.forEachIndexed { i, stat ->
            if (i % labelInterval.coerceAtLeast(1) == 0 || i == data.size - 1) {
                val x = padLeft + if (data.size > 1) (i.toFloat() / (data.size - 1)) * chartW else chartW / 2f
                val textResult = textMeasurer.measure(
                    text = stat.weekStart,
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(x - textResult.size.width / 2f, size.height - padBottom + 8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalBottomSheet(
    dailyWordGoal: Int,
    weeklyDayGoal: Int,
    onDailyGoalChange: (Int) -> Unit,
    onWeeklyGoalChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.SpacingLg)
        ) {
            Text(
                "写作目标设置",
                fontWeight = FontWeight.Bold,
                fontSize = DesignTokens.FontLarge
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            Text(
                "每日字数目标：${dailyWordGoal}字",
                fontSize = DesignTokens.FontBody
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Slider(
                value = dailyWordGoal.toFloat(),
                onValueChange = { onDailyGoalChange(it.roundToInt()) },
                valueRange = 50f..1000f,
                steps = 18
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            Text(
                "每周写作天数目标：${weeklyDayGoal}天",
                fontSize = DesignTokens.FontBody
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Slider(
                value = weeklyDayGoal.toFloat(),
                onValueChange = { onWeeklyGoalChange(it.roundToInt()) },
                valueRange = 1f..7f,
                steps = 5
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("完成")
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }
    }
}

private fun calcTrend(current: Float, previous: Float?): Float? {
    if (previous == null || previous == 0f) return null
    return ((current - previous) / previous) * 100f
}
