package com.diary.app.ui.quarterlyreview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun QuarterlyReviewScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToEntry: (Long) -> Unit = {},
    viewModel: QuarterlyReviewViewModel = viewModel()
) {
    val data by viewModel.quarterlyData.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentQuarter by viewModel.currentQuarter.collectAsState()
    val isComparisonMode by viewModel.isComparisonMode.collectAsState()
    val compareYear by viewModel.compareYear.collectAsState()
    val compareQuarter by viewModel.compareQuarter.collectAsState()
    val compareData by viewModel.compareData.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val aiSummaryLoading by viewModel.aiSummaryLoading.collectAsState()
    val aiSummaryCountLeft by viewModel.aiSummaryCountLeft.collectAsState()
    val expandedSections by viewModel.expandedSections.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DesignTokens.SpacingLg)
        ) {
            TopBar(
                onNavigateBack = onNavigateBack,
                year = currentYear,
                quarter = currentQuarter,
                canGoNext = currentYear < viewModel.maxQuarter.first ||
                        (currentYear == viewModel.maxQuarter.first && currentQuarter < viewModel.maxQuarter.second),
                isComparisonMode = isComparisonMode,
                isCompareActive = isComparisonMode,
                onPrevQuarter = viewModel::switchToPrevQuarter,
                onNextQuarter = viewModel::switchToNextQuarter,
                onToggleCompare = viewModel::toggleComparisonMode
            )

            if (isComparisonMode) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                CompareSelectorRow(
                    year = compareYear,
                    quarter = compareQuarter,
                    canGoNext = compareYear < viewModel.maxQuarter.first ||
                            (compareYear == viewModel.maxQuarter.first && compareQuarter < viewModel.maxQuarter.second),
                    onPrev = viewModel::switchCompareToPrevQuarter,
                    onNext = viewModel::switchCompareToNextQuarter
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Crossfade(
                targetState = data?.let { Pair(it.year, it.quarter) },
                animationSpec = tween(300),
                label = "quarterCrossfade"
            ) {
                val d = data
                if (d == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (d.totalEntries == 0) {
                    EmptyState()
                } else {
                    Content(
                        data = d,
                        compareData = compareData,
                        isComparisonMode = isComparisonMode,
                        aiSummary = aiSummary,
                        aiSummaryLoading = aiSummaryLoading,
                        aiSummaryCountLeft = aiSummaryCountLeft,
                        expandedSections = expandedSections,
                        onToggleSection = viewModel::toggleSection,
                        onGenerateAiSummary = viewModel::generateAiSummary,
                        onNavigateToEntry = onNavigateToEntry
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onNavigateBack: () -> Unit,
    year: Int,
    quarter: Int,
    canGoNext: Boolean,
    isComparisonMode: Boolean,
    isCompareActive: Boolean,
    onPrevQuarter: () -> Unit,
    onNextQuarter: () -> Unit,
    onToggleCompare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onPrevQuarter,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "上一季度",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${year}年 第${quarter}季度",
                fontSize = DesignTokens.FontTitle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onNextQuarter,
                enabled = canGoNext,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "下一季度",
                    tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        IconButton(
            onClick = onToggleCompare,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = if (isComparisonMode) "退出对比" else "对比模式",
                tint = if (isComparisonMode) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompareSelectorRow(
    year: Int,
    quarter: Int,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "对比",
            fontSize = DesignTokens.FontSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))

        IconButton(
            onClick = onPrev,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "上一季度",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "${year}年 第${quarter}季度",
            fontSize = DesignTokens.FontBody,
            fontWeight = FontWeight.Medium
        )

        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "下一季度",
                tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "该季度还没有日记",
            fontSize = DesignTokens.FontBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Content(
    data: QuarterlyData,
    compareData: QuarterlyData?,
    isComparisonMode: Boolean,
    aiSummary: String?,
    aiSummaryLoading: Boolean,
    aiSummaryCountLeft: Int,
    expandedSections: Set<String>,
    onToggleSection: (String) -> Unit,
    onGenerateAiSummary: () -> Unit,
    onNavigateToEntry: (Long) -> Unit
) {
    AiSummaryCard(
        summary = aiSummary,
        loading = aiSummaryLoading,
        countLeft = aiSummaryCountLeft,
        onGenerate = onGenerateAiSummary
    )

    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

    if (data.highlights.isNotEmpty()) {
        HighlightsCard(
            highlights = data.highlights,
            onNavigateToEntry = onNavigateToEntry
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
    }

    if (isComparisonMode && compareData != null) {
        ComparisonHeaderCard(data, compareData)
    } else {
        QuarterHeaderCard(data)
    }

    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

    if (isComparisonMode && compareData != null) {
        ComparisonStatsRow(data, compareData)
    } else {
        StatsRow(data)
    }

    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

    if (data.totalEntries > 0) {
        ExpandableSectionCard(
            title = "篇数详情",
            isExpanded = expandedSections.contains("entries_detail"),
            onToggle = { onToggleSection("entries_detail") }
        ) {
            EntriesDetail(data)
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

        ExpandableSectionCard(
            title = "心情详情",
            isExpanded = expandedSections.contains("mood_detail"),
            onToggle = { onToggleSection("mood_detail") }
        ) {
            MoodDetail(data)
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

        ExpandableSectionCard(
            title = "写作习惯",
            isExpanded = expandedSections.contains("writing_habits"),
            onToggle = { onToggleSection("writing_habits") }
        ) {
            WritingHabits(data)
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
    }

    if (data.topTags.isNotEmpty()) {
        TagsCard(data.topTags)
        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
    }

    if (data.moodTrend.isNotEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = DesignTokens.CornerLarge,
            innerPadding = DesignTokens.SpacingLg
        ) {
            Column {
                Text(
                    text = "心情趋势",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                MoodTrendChart(
                    details = data.moodTrendDetails,
                    compareDetails = if (isComparisonMode) compareData?.moodTrendDetails else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

@Composable
private fun AiSummaryCard(
    summary: String?,
    loading: Boolean,
    countLeft: Int,
    onGenerate: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI 季度总结",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(DesignTokens.IconSmall),
                        strokeWidth = 2.dp
                    )
                } else if (summary == null && countLeft > 0) {
                    Text(
                        text = "生成 ($countLeft)",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onGenerate() }
                    )
                } else if (summary != null && countLeft > 0) {
                    Text(
                        text = "重新生成 ($countLeft)",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onGenerate() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            when {
                loading -> {
                    Text(
                        text = "AI 正在分析你的季度日记……",
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                summary != null -> {
                    Text(
                        text = summary,
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                countLeft <= 0 -> {
                    Text(
                        text = "本季度生成次数已用完",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = "点击「生成」获取 AI 季度总结",
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightsCard(
    highlights: List<Highlight>,
    onNavigateToEntry: (Long) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            Text(
                text = "季度亮点",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            highlights.forEach { h ->
                val dateStr = Instant.ofEpochMilli(h.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MM月dd日"))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToEntry(h.entryId) }
                        .padding(vertical = DesignTokens.SpacingXs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = h.title.ifEmpty { "无标题" },
                            fontSize = DesignTokens.FontBody,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateStr,
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(DesignTokens.CornerSmall)
                            )
                            .padding(horizontal = DesignTokens.SpacingSm, vertical = 2.dp)
                    ) {
                        Text(
                            text = h.reason,
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (h != highlights.last()) {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                    Divider(
                        modifier = Modifier.padding(vertical = DesignTokens.SpacingXs / 2),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuarterHeaderCard(data: QuarterlyData) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            Text(
                text = "${data.year}年 第${data.quarter}季度",
                fontSize = DesignTokens.FontTitle,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(
                text = "共 ${data.totalEntries} 篇日记",
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComparisonHeaderCard(base: QuarterlyData, compare: QuarterlyData) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "基准",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${base.year}年 第${base.quarter}季度",
                    fontSize = DesignTokens.FontBody,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${base.totalEntries} 篇",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "VS",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "对比",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${compare.year}年 第${compare.quarter}季度",
                    fontSize = DesignTokens.FontBody,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${compare.totalEntries} 篇",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatsRow(data: QuarterlyData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)
    ) {
        GlassCard(
            modifier = Modifier.weight(1f),
            cornerRadius = DesignTokens.CornerLarge,
            innerPadding = DesignTokens.SpacingLg
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${data.totalWords}",
                    fontSize = DesignTokens.FontTitle,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Text(
                    text = "总字数",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        GlassCard(
            modifier = Modifier.weight(1f),
            cornerRadius = DesignTokens.CornerLarge,
            innerPadding = DesignTokens.SpacingLg
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${"%.1f".format(data.avgMood)}",
                    fontSize = DesignTokens.FontTitle,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Text(
                    text = "平均心情",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = moodLabelSimple(data.topMood),
                    fontSize = DesignTokens.FontCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ComparisonStatsRow(base: QuarterlyData, compare: QuarterlyData) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            ComparisonStatItem(
                label = "总字数",
                baseValue = "${base.totalWords}",
                compareValue = "${compare.totalWords}",
                baseColor = Color(0xFF2196F3),
                compareColor = Color(0xFFFF8C00)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            ComparisonStatItem(
                label = "平均心情",
                baseValue = "${"%.1f".format(base.avgMood)}",
                compareValue = "${"%.1f".format(compare.avgMood)}",
                baseColor = Color(0xFF2196F3),
                compareColor = Color(0xFFFF8C00)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            ComparisonStatItem(
                label = "篇数",
                baseValue = "${base.totalEntries}",
                compareValue = "${compare.totalEntries}",
                baseColor = Color(0xFF2196F3),
                compareColor = Color(0xFFFF8C00)
            )
        }
    }
}

@Composable
private fun ComparisonStatItem(
    label: String,
    baseValue: String,
    compareValue: String,
    baseColor: Color,
    compareColor: Color
) {
    val baseNum = baseValue.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 0f
    val compareNum = compareValue.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 0f
    val pctChange = if (compareNum != 0f) {
        ((baseNum - compareNum) / compareNum * 100).toInt()
    } else 0
    val arrow = if (pctChange >= 0) "\u2191" else "\u2193"
    val changeColor = if (pctChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

    Column {
        Text(
            text = label,
            fontSize = DesignTokens.FontSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = baseValue,
                    fontSize = DesignTokens.FontLarge,
                    fontWeight = FontWeight.Bold,
                    color = baseColor
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$arrow${abs(pctChange)}%",
                    fontSize = DesignTokens.FontBody,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = compareValue,
                    fontSize = DesignTokens.FontLarge,
                    fontWeight = FontWeight.Bold,
                    color = compareColor
                )
            }
        }
    }
}

@Composable
private fun ExpandableSectionCard(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg,
        onClick = onToggle
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isExpanded) "收起" else "展开",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    content()
                }
            }
        }
    }
}

@Composable
private fun EntriesDetail(data: QuarterlyData) {
    Column {
        data.monthlyDistribution.forEach { (month, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${month}月",
                    fontSize = DesignTokens.FontBody
                )
                Text(
                    text = "$count 篇",
                    fontSize = DesignTokens.FontBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "日均篇数",
                fontSize = DesignTokens.FontBody
            )
            Text(
                text = "${"%.1f".format(data.dailyAvgEntries)}",
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "最长连续天数",
                fontSize = DesignTokens.FontBody
            )
            Text(
                text = "${data.longestStreak} 天",
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoodDetail(data: QuarterlyData) {
    Column {
        data.monthlyMood.forEach { (month, mood) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${month}月平均",
                    fontSize = DesignTokens.FontBody
                )
                Text(
                    text = "${"%.1f".format(mood)} (${moodLabelSimple(mood.toInt())})",
                    fontSize = DesignTokens.FontBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "情绪变化评级",
                fontSize = DesignTokens.FontBody
            )
            Text(
                text = data.moodRating,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WritingHabits(data: QuarterlyData) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "最爱时段",
                fontSize = DesignTokens.FontBody
            )
            Text(
                text = data.favoriteHourRange,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "平均时长",
                fontSize = DesignTokens.FontBody
            )
            Text(
                text = "${data.avgDurationMinutes} 分钟",
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "周末 vs 工作日",
                fontSize = DesignTokens.FontBody
            )
            val ratioDesc = when {
                data.weekendRatio > 1.2f -> "周末更活跃"
                data.weekendRatio < 0.8f -> "工作日更活跃"
                else -> "分布均匀"
            }
            Text(
                text = ratioDesc,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TagsCard(tags: List<String>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconMedium)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                Text(
                    text = "热门标签",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            tags.forEachIndexed { index, tag ->
                Text(
                    text = "${index + 1}. $tag",
                    fontSize = DesignTokens.FontBody,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MoodTrendChart(
    details: List<MoodTrendPoint>,
    compareDetails: List<MoodTrendPoint>? = null,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf("line") }
    var tooltipPoint by remember { mutableStateOf<MoodTrendPoint?>(null) }
    var compareTooltip by remember { mutableStateOf<MoodTrendPoint?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val compareColor = Color(0xFFFF8C00)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { chartMode = if (chartMode == "line") "bar" else "line" },
                modifier = Modifier.size(44.dp)
            ) {
                Text(
                    text = if (chartMode == "line") "柱状" else "折线",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(modifier = modifier) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(details, compareDetails) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            var closestDist = Float.MAX_VALUE
                            var closest: MoodTrendPoint? = null
                            var isCompare = false
                            val stepX = if (details.size > 1) width / (details.size - 1) else width

                            details.forEachIndexed { index, pt ->
                                val x = index * stepX
                                val y = height - (pt.mood / 6f) * height
                                val dist = kotlin.math.sqrt(
                                    (offset.x - x) * (offset.x - x) + (offset.y - y) * (offset.y - y)
                                )
                                if (dist < closestDist) {
                                    closestDist = dist
                                    closest = pt
                                    isCompare = false
                                }
                            }

                            if (compareDetails != null && compareDetails.size > 1) {
                                val cStepX = width / (compareDetails.size - 1)
                                compareDetails.forEachIndexed { index, pt ->
                                    val x = index * cStepX
                                    val y = height - (pt.mood / 6f) * height
                                    val dist = kotlin.math.sqrt(
                                        (offset.x - x) * (offset.x - x) + (offset.y - y) * (offset.y - y)
                                    )
                                    if (dist < closestDist) {
                                        closestDist = dist
                                        closest = pt
                                        isCompare = true
                                    }
                                }
                            }

                            if (closestDist < 60f && closest != null) {
                                if (isCompare) {
                                    compareTooltip = closest
                                    tooltipPoint = null
                                } else {
                                    tooltipPoint = closest
                                    compareTooltip = null
                                }
                            } else {
                                tooltipPoint = null
                                compareTooltip = null
                            }
                        }
                    }
            ) {
                if (details.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val stepX = if (details.size > 1) width / (details.size - 1) else width

                if (chartMode == "line") {
                    val path = Path()
                    details.forEachIndexed { index, pt ->
                        val x = index * stepX
                        val y = height - (pt.mood / 6f) * height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    val fillPath = Path().apply {
                        moveTo(0f, height)
                        details.forEachIndexed { index, pt ->
                            val x = index * stepX
                            val y = height - (pt.mood / 6f) * height
                            lineTo(x, y)
                        }
                        lineTo(width, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                primaryColor.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 2.5f)
                    )

                    details.forEachIndexed { index, pt ->
                        val x = index * stepX
                        val y = height - (pt.mood / 6f) * height
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }

                    if (compareDetails != null && compareDetails.size > 1) {
                        val cStepX = width / (compareDetails.size - 1)
                        val cPath = Path()
                        compareDetails.forEachIndexed { index, pt ->
                            val x = index * cStepX
                            val y = height - (pt.mood / 6f) * height
                            if (index == 0) cPath.moveTo(x, y) else cPath.lineTo(x, y)
                        }

                        drawPath(
                            path = cPath,
                            color = compareColor,
                            style = Stroke(width = 2.5f)
                        )

                        compareDetails.forEachIndexed { index, pt ->
                            val x = index * cStepX
                            val y = height - (pt.mood / 6f) * height
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = compareColor,
                                radius = 4f,
                                center = Offset(x, y)
                            )
                        }

                        drawLine(
                            color = compareColor,
                            start = Offset(4.dp.toPx(), 4.dp.toPx()),
                            end = Offset(24.dp.toPx(), 4.dp.toPx()),
                            strokeWidth = 2.5f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "对比",
                            28.dp.toPx(),
                            10.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.rgb(255, 140, 0)
                                textSize = 24f
                            }
                        )

                        drawLine(
                            color = primaryColor,
                            start = Offset(4.dp.toPx(), 26.dp.toPx()),
                            end = Offset(24.dp.toPx(), 26.dp.toPx()),
                            strokeWidth = 2.5f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "基准",
                            28.dp.toPx(),
                            32.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.rgb(33, 150, 243)
                                textSize = 24f
                            }
                        )
                    }
                } else {
                    val barWidth = (stepX * 0.6f).coerceAtMost(width.toFloat() / details.size * 0.8f)
                    details.forEachIndexed { index, pt ->
                        val barHeight = (pt.mood / 6f) * height
                        val left = index * stepX + (stepX - barWidth) / 2
                        val top = height - barHeight
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }

                    if (compareDetails != null && compareDetails.size > 1) {
                        val cStepX = width / (compareDetails.size - 1)
                        val cBarWidth = (cStepX * 0.6f).coerceAtMost(width.toFloat() / compareDetails.size * 0.8f)
                        compareDetails.forEachIndexed { index, pt ->
                            val barHeight = (pt.mood / 6f) * height
                            val left = index * cStepX + (cStepX - cBarWidth) / 2
                            val top = height - barHeight
                            drawRoundRect(
                                color = compareColor,
                                topLeft = Offset(left, top),
                                size = Size(cBarWidth, barHeight),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                    }
                }

                val dateStep = maxOf(1, details.size / 7)
                details.forEachIndexed { index, pt ->
                    if (index % dateStep == 0) {
                        val x = index * stepX
                        val dateStr = pt.date.format(DateTimeFormatter.ofPattern("MM/dd"))
                        drawContext.canvas.nativeCanvas.drawText(
                            dateStr,
                            x,
                            height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            val tooltipText = tooltipPoint?.let { pt ->
                val dateStr = pt.date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                "$dateStr  ${moodLabelSimple(pt.mood.toInt())}  ${pt.title.take(12)}"
            } ?: compareTooltip?.let { pt ->
                val dateStr = pt.date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                "[对比] $dateStr  ${moodLabelSimple(pt.mood.toInt())}  ${pt.title.take(12)}"
            }

            if (tooltipText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                            RoundedCornerShape(DesignTokens.CornerSmall)
                        )
                        .padding(horizontal = DesignTokens.SpacingSm, vertical = DesignTokens.SpacingXs)
                ) {
                    Text(
                        text = tooltipText,
                        fontSize = DesignTokens.FontCaption,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}

private fun moodLabelSimple(mood: Int): String {
    return when (mood) {
        1 -> "沮丧"
        2 -> "低落"
        3 -> "平静"
        4 -> "开心"
        5 -> "愉快"
        6 -> "兴奋"
        else -> "平静"
    }
}
