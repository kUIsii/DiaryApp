package com.diary.app.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.R
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.formatEntryTime
import com.diary.app.ui.components.formatWordCount
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.stats.WordCloud
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun StatsScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToMonthlyReport: () -> Unit = {},
    onNavigateToQuarterlyReview: () -> Unit = {},
    onNavigateToPersonalYearbook: () -> Unit = {},
    viewModel: StatsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEntries by remember { mutableStateOf<List<DiaryPreview>>(emptyList()) }
    var isLoadingEntries by remember { mutableStateOf(false) }

    selectedDate?.let { date ->
        DayEntriesDialog(
            date = date,
            entries = selectedEntries,
            isLoading = isLoadingEntries,
            onDismiss = {
                selectedDate = null
                selectedEntries = emptyList()
            },
            onNavigateToDetail = onNavigateToDetail
        )
    }

    GradientBackground {
        when {
            state.isLoading -> LoadingState()
            state.totalEntries == 0 -> {
                EmptyState(
                    icon = Icons.Default.SelfImprovement,
                    title = "还没有统计内容",
                    subtitle = "开始记录几篇日记后，这里会出现写作趋势、心情和习惯摘要",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item {
                        StatsPageHeader(totalEntries = state.totalEntries)
                    }

                    item {
                        MonthlyReportEntryCard(onClick = onNavigateToMonthlyReport)
                    }

                    item {
                        QuarterlyReviewEntryCard(onClick = onNavigateToQuarterlyReview)
                    }

                    item {
                        PersonalYearbookEntryCard(onClick = onNavigateToPersonalYearbook)
                    }

                    item {
                        StatsHeroSection(state = state)
                    }

                    if (state.heatmapData.isNotEmpty()) {
                        item {
                            StatsSectionCard(
                                title = "记录热力图",
                                subtitle = "查看最近 ${state.heatmapRange.days} 天的记录密度，点按某一天可查看当天日记"
                            ) {
                                // Range selector inside the card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HeatmapRange.entries.forEach { range ->
                                        val rangeLabel = when (range) {
                                            HeatmapRange.ONE_MONTH -> "30天"
                                            HeatmapRange.THREE_MONTHS -> "3月"
                                            HeatmapRange.SIX_MONTHS -> "6月"
                                            HeatmapRange.ONE_YEAR -> "1年"
                                        }
                                        RangeChip(
                                            label = rangeLabel,
                                            selected = state.heatmapRange == range,
                                            onClick = { viewModel.setHeatmapRange(range) }
                                        )
                                    }
                                }

                                DiaryHeatmap(
                                    data = state.heatmapData,
                                    range = state.heatmapRange,
                                    onDayClick = { date ->
                                        selectedDate = date
                                        isLoadingEntries = true
                                        scope.launch {
                                            selectedEntries = viewModel.getEntriesForDate(date)
                                            isLoadingEntries = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        StatsSectionCard(
                            title = "月度趋势",
                            subtitle = "聚焦最近 6 个月，快速判断你的记录节奏是否稳定"
                        ) {
                            MonthlyTrendChart(state.monthlyTrend)
                        }
                    }

                    state.moodTrend?.let { moodTrend ->
                        item {
                            StatsSectionCard(
                                title = "心情变化",
                                subtitle = "对比最近 30 天与前 30 天的平均心情变化"
                            ) {
                                MoodTrendRow(moodTrend)
                            }
                        }
                    }

                    state.writingHabit?.let { habit ->
                        item {
                            StatsSectionCard(
                                title = "写作习惯",
                                subtitle = "从频率、活跃日期和常用时间段看你的记录方式"
                            ) {
                                WritingHabitSection(habit)
                            }
                        }
                    }

                    item {
                        StatsSectionCard(
                            title = "心情分布",
                            subtitle = "心情记录越完整，后续趋势分析越有参考价值"
                        ) {
                            if (state.moodDistribution.any { it.count > 0 }) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.moodDistribution.forEach { mood ->
                                        MoodBar(
                                            label = mood.label,
                                            count = mood.count,
                                            color = mood.color,
                                            maxCount = state.moodDistribution.maxOfOrNull { it.count } ?: 1,
                                            level = mood.level
                                        )
                                    }
                                }
                            } else {
                                InlineEmptyHint("还没有足够的心情数据")
                            }
                        }
                    }

                    state.moodWeatherInsight?.let { insight ->
                        item {
                            MoodWeatherInsightCard(insight)
                        }
                    }

                    item {
                        WordCloudSection(
                            state = state,
                            onPeriodChange = { viewModel.setWordCloudPeriod(it) },
                            onWordClick = { word ->
                                viewModel.analyzeContent(word)
                            }
                        )
                    }

                    if (state.weatherDistribution.isNotEmpty() || state.tagUsage.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (state.weatherDistribution.isNotEmpty()) {
                                    GlassCard(
                                        modifier = Modifier.weight(1f),
                                        cornerRadius = 22.dp,
                                        innerPadding = 16.dp
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            SectionHeader(
                                                title = "天气统计",
                                                subtitle = "哪些天气最常出现在你的记录里"
                                            )
                                            state.weatherDistribution.take(5).forEach { weather ->
                                                val (icon, tint) = weatherIconFor(weather.type)
                                                WeatherRow(
                                                    type = weather.type,
                                                    count = weather.count,
                                                    icon = icon,
                                                    tint = tint
                                                )
                                            }
                                        }
                                    }
                                }

                                if (state.tagUsage.isNotEmpty()) {
                                    GlassCard(
                                        modifier = Modifier.weight(1f),
                                        cornerRadius = 22.dp,
                                        innerPadding = 16.dp
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            SectionHeader(
                                                title = "标签使用",
                                                subtitle = "高频主题能帮助你识别最近关注点"
                                            )
                                            val maxTagCount = state.tagUsage.maxOfOrNull { it.count } ?: 1
                                            state.tagUsage
                                                .sortedByDescending { it.count }
                                                .take(6)
                                                .forEach { tag ->
                                                    TagRow(
                                                        name = tag.name,
                                                        color = Color(tag.color),
                                                        count = tag.count,
                                                        maxCount = maxTagCount
                                                    )
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    // AI Analysis Bottom Sheet
    if (state.analysisQuery.isNotEmpty()) {
        AnalysisBottomSheet(
            query = state.analysisQuery,
            result = state.analysisResult,
            isAnalyzing = state.isAnalyzing,
            onDismiss = { viewModel.dismissAnalysis() }
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(34.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DayEntriesDialog(
    date: LocalDate,
    entries: List<DiaryPreview>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = date.format(formatter),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isLoading) "正在加载当天记录" else "共 ${entries.size} 篇日记",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            when {
                isLoading -> {
                    Text(
                        text = "正在整理当天内容…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                entries.isEmpty() -> {
                    Text(
                        text = "这一天还没有日记",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        entries.forEach { entry ->
                            GlassCard(
                                cornerRadius = 16.dp,
                                innerPadding = 12.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formatEntryTime(entry.createdAt),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        entry.moodLevel?.let { level ->
                                            val iconData = moodIconForLevel(level)
                                            Icon(
                                                imageVector = iconData.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = iconData.tint
                                            )
                                        }
                                    }
                                    Text(
                                        text = entry.title.ifBlank { "未命名日记" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (entry.plainText.isNotBlank()) {
                                        Text(
                                            text = entry.plainText.take(90) + if (entry.plainText.length > 90) "..." else "",
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "查看",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .clickable {
                                                onNavigateToDetail(entry.id)
                                                onDismiss()
                                            }
                                            .padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun StatsPageHeader(totalEntries: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "统计",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (totalEntries == 0) "记录你的写作轨迹" else "共 $totalEntries 篇日记",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MonthlyReportEntryCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        innerPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "月度报告",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "查看本月的写作统计、心情趋势和标签分析",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun QuarterlyReviewEntryCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        innerPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "季度回顾",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "以季度为单位，回顾写作趋势与心情变化",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PersonalYearbookEntryCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        innerPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "个人年鉴",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "将一年的日记精华汇编成可导出的精美文档",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatsHeroSection(
    state: StatsState
) {
    val avgWords = state.wordStats?.avgWordsPerEntry ?: 0
    val totalWords = state.wordStats?.totalWords ?: 0
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        innerPadding = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Hero number + stats in one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total entries - hero number
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${state.totalEntries}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 40.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "篇",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (totalWords > 0) {
                        Text(
                            text = "累计 ${formatWordCount(totalWords)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Compact stats column
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    CompactStatChip(label = "平均", value = "${avgWords}字")
                    CompactStatChip(label = "本月", value = "${state.thisMonthEntries}篇")
                    CompactStatChip(label = "连续", value = "${state.currentStreak}天")
                }
            }

        }
    }
}

@Composable
private fun CompactStatChip(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content
        )
    }
}

@Composable
private fun WordCloudSection(
    state: StatsState,
    onPeriodChange: (WordCloudPeriod) -> Unit,
    onWordClick: ((String) -> Unit)? = null
) {
    StatsSectionCard(
        title = "词云",
        subtitle = "从日记中提取的高频词，反映你的关注点"
    ) {
        // Period selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WordCloudPeriod.entries.forEach { period ->
                val selected = state.wordCloudPeriod == period
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                        .clickable { onPeriodChange(period) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = period.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.isWordCloudLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )
            }
        } else if (state.topWords.isNotEmpty()) {
            WordCloud(
                words = state.topWords,
                primaryColor = MaterialTheme.colorScheme.primary,
                secondaryColor = MaterialTheme.colorScheme.tertiary,
                onWordClick = onWordClick
            )
        } else {
            InlineEmptyHint("该时间段暂无足够数据")
        }
    }
}

@Composable
private fun StatsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(title = title, subtitle = subtitle)
            content()
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InlineEmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoodWeatherInsightCard(insight: MoodWeatherInsight) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "天气与心情",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = insight.text,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Per-weather average mood bars
            if (insight.perWeatherAverages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    insight.perWeatherAverages.entries
                        .sortedByDescending { it.value }
                        .forEach { (weather, avgMood) ->
                            val (icon, tint) = weatherIconFor(weather)
                            val progress = (avgMood / 6f).coerceIn(0f, 1f)
                            val barColor = moodColorForLevel(avgMood.toInt().coerceIn(1, 6))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = weather,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(40.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(barColor)
                                    )
                                }
                                Text(
                                    text = "%.1f".format(avgMood),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(24.dp)
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun MoodTrendRow(moodTrend: MoodTrend) {
    val recent = moodTrend.recent30Avg
    val previous = moodTrend.previous30Avg
    val (icon, accent, title) = when (moodTrend.direction) {
        TrendDirection.UP -> Triple(Icons.Default.TrendingUp, MaterialTheme.colorScheme.primary, "整体更积极")
        TrendDirection.DOWN -> Triple(Icons.Default.TrendingDown, MaterialTheme.colorScheme.error, "波动偏低")
        TrendDirection.FLAT -> Triple(Icons.Default.TrendingFlat, MaterialTheme.colorScheme.tertiary, "基本稳定")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildString {
                    append("最近 30 天平均心情 ")
                    append(recent?.let { String.format("%.1f", it) } ?: "--")
                    append("，前 30 天 ")
                    append(previous?.let { String.format("%.1f", it) } ?: "--")
                },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthlyTrendChart(data: List<MonthTrend>) {
    if (data.isEmpty()) {
        InlineEmptyHint("还没有足够的月度数据")
        return
    }

    val maxCount = data.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..3) {
                    val y = size.height * i / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, month ->
                    val fraction = month.count.toFloat() / maxCount
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = index * 80,
                            easing = FastOutSlowInEasing
                        ),
                        label = "monthBar"
                    )
                    val isCurrentMonth = index == data.lastIndex

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${month.count}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(animatedFraction.coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                .background(
                                    if (isCurrentMonth) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.55f),
                                                primaryColor
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.14f),
                                                primaryColor.copy(alpha = 0.34f)
                                            )
                                        )
                                    }
                                )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, month ->
                Text(
                    text = month.month,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = if (index == data.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index == data.lastIndex) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WritingHabitSection(habit: WritingHabit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HabitCard(
            icon = Icons.Default.Edit,
            label = "周均频率",
            value = "${String.format("%.1f", habit.avgPerWeek)} 篇",
            modifier = Modifier.weight(1f)
        )
        HabitCard(
            icon = Icons.Default.Weekend,
            label = "最活跃日期",
            value = habit.mostActiveDay,
            modifier = Modifier.weight(1f)
        )
        HabitCard(
            icon = Icons.Default.Schedule,
            label = "常写时段",
            value = habit.mostActiveTime,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HabitCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        innerPadding = 14.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MoodBar(
    label: String,
    count: Int,
    color: Color,
    maxCount: Int,
    level: Int
) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "moodBar"
    )
    val iconData = moodIconForLevel(level)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = iconData.icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = iconData.tint
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(38.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.45f), color)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherRow(
    type: String,
    count: Int,
    icon: ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = type,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count 篇",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TagRow(
    name: String,
    color: Color,
    count: Int,
    maxCount: Int
) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "tagBar"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(56.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.35f), color)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DiaryHeatmap(
    data: List<HeatmapDay>,
    range: HeatmapRange,
    onDayClick: (LocalDate) -> Unit
) {
    if (data.isEmpty()) {
        InlineEmptyHint("当前范围内还没有统计数据")
        return
    }

    if (range.days <= 30) {
        MonthlyHeatmap(data = data, onDayClick = onDayClick)
    } else {
        CompactHeatmap(data = data, onDayClick = onDayClick)
    }

    Spacer(modifier = Modifier.height(10.dp))

    val activeDays = data.count { it.count > 0 }
    val totalDays = data.size.coerceAtLeast(1)
    val coverage = (activeDays * 100f / totalDays).roundToInt()
    Text(
        text = "最近 ${range.days} 天里，有 $activeDays 天写了日记，记录覆盖率 $coverage%",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MonthlyHeatmap(
    data: List<HeatmapDay>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val weeks = remember(data) {
        val firstDate = data.first().date
        val daysToPad = (firstDate.dayOfWeek.value - 1 + 7) % 7
        (List(daysToPad) { null } + data).chunked(7)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (index in 0 until 7) {
                    val day = week.getOrNull(index)
                    if (day == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                        )
                    } else {
                        val primary = MaterialTheme.colorScheme.primary
                        val background = when {
                            day.count == 0 -> surfaceVariant.copy(alpha = 0.40f)
                            day.count == 1 -> primary.copy(alpha = 0.20f)
                            day.count == 2 -> primary.copy(alpha = 0.40f)
                            day.count == 3 -> primary.copy(alpha = 0.60f)
                            day.count == 4 -> primary.copy(alpha = 0.80f)
                            else -> primary
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(background)
                                .clickable { onDayClick(day.date) }
                                .semantics {
                                    contentDescription = "${day.date.monthValue}月${day.date.dayOfMonth}日，${day.count} 篇日记"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${day.date.dayOfMonth}",
                                fontSize = 11.sp,
                                fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                                color = if (day.count > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        HeatmapLegend()
    }
}

@Composable
private fun CompactHeatmap(
    data: List<HeatmapDay>,
    onDayClick: (LocalDate) -> Unit
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    // Organize data into weeks (Mon=0 ... Sun=6), each week is a column
    val weeks = remember(data) {
        val firstDate = data.first().date
        val startOffset = (firstDate.dayOfWeek.value - 1 + 7) % 7
        val padded = List(startOffset) { null } + data.map { it }
        padded.chunked(7)
    }

    // Find month boundaries for labels
    val monthLabels = remember(data) {
        val labels = mutableListOf<Pair<Int, String>>()
        var prevMonth = -1
        weeks.forEachIndexed { weekIdx, week ->
            val firstDay = week.firstOrNull { it != null }
            if (firstDay != null && firstDay.date.monthValue != prevMonth) {
                prevMonth = firstDay.date.monthValue
                labels.add(weekIdx to "${firstDay.date.monthValue}月")
            }
        }
        labels
    }

    val cellSize = 12.dp
    val cellGap = 3.dp
    val labelWidth = 20.dp

    Column {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            // Day-of-week labels
            Column(
                modifier = Modifier.width(labelWidth),
                verticalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                for (d in 0 until 7) {
                    Box(
                        modifier = Modifier.height(cellSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (d % 2 == 0) { // Show Mon, Wed, Fri
                            Text(
                                text = listOf("一", "二", "三", "四", "五", "六", "日")[d],
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Grid columns
            weeks.forEach { week ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(cellGap),
                    modifier = Modifier.width(cellSize)
                ) {
                    for (dayIndex in 0 until 7) {
                        val day = week.getOrNull(dayIndex)
                        if (day == null) {
                            Spacer(modifier = Modifier.size(cellSize))
                        } else {
                            val background = when {
                                day.count == 0 -> surfaceVariant.copy(alpha = 0.30f)
                                day.count == 1 -> primary.copy(alpha = 0.20f)
                                day.count == 2 -> primary.copy(alpha = 0.40f)
                                day.count == 3 -> primary.copy(alpha = 0.60f)
                                day.count == 4 -> primary.copy(alpha = 0.80f)
                                else -> primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(background)
                                    .clickable { onDayClick(day.date) }
                                    .semantics {
                                        contentDescription = "${day.date.monthValue}月${day.date.dayOfMonth}日，${day.count} 篇"
                                    }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(cellGap))
            }
        }

        // Month labels below
        Box(modifier = Modifier.padding(start = labelWidth)) {
            Row {
                monthLabels.forEach { (weekIdx, label) ->
                    val offsetWeeks = weekIdx
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = (offsetWeeks * 15).dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HeatmapLegend()
    }
}

@Composable
private fun HeatmapLegend() {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            surfaceVariant.copy(alpha = 0.40f),
            primary.copy(alpha = 0.20f),
            primary.copy(alpha = 0.38f),
            primary.copy(alpha = 0.58f),
            primary.copy(alpha = 0.78f),
            primary
        ).forEachIndexed { index, color ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "少  →  多",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalysisBottomSheet(
    query: String,
    result: String?,
    isAnalyzing: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI 分析",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            // Query tag
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "关键词: $query",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Content
            when {
                isAnalyzing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "正在分析你的日记...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                result != null -> {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Text(
                            text = result,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
