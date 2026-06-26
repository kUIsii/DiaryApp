package com.diary.app.ui.monthlyreport

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

private val moodLabels = listOf("", "很差", "不好", "一般", "不错", "很棒")
private val monthNames = listOf("", "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    year: Int,
    month: Int,
    onNavigateBack: () -> Unit,
    onShare: (MonthlyReport?) -> Unit = {},
    viewModel: MonthlyReportViewModel = viewModel()
) {
    val report by viewModel.report.collectAsState()
    val comparison by viewModel.comparison.collectAsState()
    val aiReview by viewModel.aiReview.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    LaunchedEffect(year, month) {
        viewModel.loadReport(year, month)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { Text("${monthNames[month]}报告") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onShare(report) }) {
                            Icon(Icons.Default.Share, contentDescription = "分享")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                    )
                )
            }
        ) { innerPadding ->
            val currentReport = report
            if (currentReport == null) {
                EmptyState(innerPadding)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding() - 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    HeaderSection(currentReport)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Writing overview
                    WritingOverviewCard(currentReport)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mood trend line chart
                    MoodTrendChart(currentReport)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Word count bar chart
                    WordCountBarChart(currentReport)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tag distribution
                    TagStatsCard(currentReport)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Year comparison
                    YearComparisonCard(currentReport, comparison)

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Monthly Review
                    AiReviewCard(
                        report = currentReport,
                        aiReview = aiReview,
                        isLoading = aiLoading,
                        isAiAvailable = viewModel.isAiAvailable(),
                        onGenerate = { viewModel.generateAiReview() }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(innerPadding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "这个月还没有日记",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "写几篇日记后再来看看吧",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ==================== Header ====================
@Composable
private fun HeaderSection(report: MonthlyReport) {
    val animatedEntries by animateIntAsState(
        targetValue = report.totalEntries,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "headerEntries"
    )

    GlassCard(
        cornerRadius = 20.dp,
        innerPadding = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${report.year}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = monthNames[report.month],
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 2.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "共 $animatedEntries 篇",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val activeDays = report.dailyWordCounts.count { it > 0 }
                Text(
                    text = "${activeDays} 天有写作",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==================== Writing Overview ====================
@Composable
private fun WritingOverviewCard(report: MonthlyReport) {
    ReportCard(
        title = "写作概览",
        subtitle = "这个月的写作数据",
        icon = Icons.Default.Speed
    ) {
        val avgWords = if (report.totalEntries > 0) report.totalWords / report.totalEntries else 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OverviewStat("总字数", formatCount(report.totalWords), MaterialTheme.colorScheme.primary)
            OverviewStat("平均每篇", "${avgWords}字", Color(0xFF66BB6A))
            OverviewStat("总时长", formatDuration(report.totalDurationMinutes), Color(0xFF42A5F5))
        }

        if (report.avgMood != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Mood,
                    contentDescription = null,
                    tint = getMoodColor(report.avgMood),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "平均心情 ${String.format("%.1f", report.avgMood)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = moodLabels[report.avgMood.toInt().coerceIn(1, 5)],
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = getMoodColor(report.avgMood)
                )
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ==================== Mood Trend Line Chart ====================
@Composable
private fun MoodTrendChart(report: MonthlyReport) {
    ReportCard(
        title = "心情趋势",
        subtitle = "每日平均心情变化",
        icon = Icons.Default.Mood
    ) {
        val hasData = report.dailyMoodAverages.any { it != null }

        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有心情记录",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            var selectedDay by remember { mutableStateOf<Int?>(null) }

            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(start = 28.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
                        .pointerInput(report.dailyMoodAverages) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val x = down.position.x
                                val days = report.dailyMoodAverages.size
                                if (days > 0) {
                                    val xStep = size.width.toFloat() / (days - 1).coerceAtLeast(1)
                                    val dayIndex = (x / xStep).toInt().coerceIn(0, days - 1)
                                    selectedDay = if (report.dailyMoodAverages[dayIndex] != null) dayIndex else null
                                }
                            }
                        }
                ) {
                val width = size.width
                val height = size.height
                val days = report.dailyMoodAverages.size
                if (days == 0) return@Canvas

                val xStep = width / (days - 1).coerceAtLeast(1)
                val yPadding = 8f

                // Grid lines for mood levels
                for (level in 1..5) {
                    val y = height - yPadding - (height - 2 * yPadding) * (level - 1) / 4f
                    drawLine(
                        color = surfaceVariant.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // Build points for non-null values
                val points = report.dailyMoodAverages.mapIndexedNotNull { i, mood ->
                    mood?.let {
                        Offset(
                            x = i * xStep,
                            y = height - yPadding - (height - 2 * yPadding) * (it - 1f) / 4f
                        )
                    }
                }

                if (points.size >= 2) {
                    // Smooth bezier path
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cpX = (prev.x + curr.x) / 2f
                            cubicTo(cpX, prev.y, cpX, curr.y, curr.x, curr.y)
                        }
                    }

                    // Gradient fill below the line
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, height)
                        lineTo(points.first().x, height)
                        close()
                    }

                    val avgMood = report.avgMood ?: 3f
                    val lineColor = getMoodColor(avgMood)

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.25f),
                                lineColor.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw the line
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Data points
                    points.forEach { p ->
                        drawCircle(
                            color = lineColor,
                            radius = 4f,
                            center = p
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2f,
                            center = p
                        )
                    }

                    // Selected day indicator
                    val sel = selectedDay
                    if (sel != null && sel < points.size) {
                        val sp = points[sel]
                        drawLine(
                            color = lineColor.copy(alpha = 0.3f),
                            start = Offset(sp.x, 0f),
                            end = Offset(sp.x, height),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 7f,
                            center = sp
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = sp
                        )
                    }
                }
            }

            // Tooltip overlay
            val sel = selectedDay
            if (sel != null && sel < report.dailyMoodAverages.size) {
                val moodVal = report.dailyMoodAverages[sel]
                if (moodVal != null) {
                    val dayNum = sel + 1
                    val label = moodLabels[moodVal.toInt().coerceIn(1, 5)]
                    val tooltipText = "${dayNum}日 ${String.format("%.1f", moodVal)} $label"
                    Text(
                        text = tooltipText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = getMoodColor(moodVal),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labelCount = minOf(7, report.dailyMoodAverages.size)
                val step = if (labelCount > 1) (report.dailyMoodAverages.size - 1) / (labelCount - 1) else 1
                for (i in 0 until labelCount) {
                    val day = i * step + 1
                    Text(
                        text = "${day}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ==================== Word Count Bar Chart ====================
@Composable
private fun WordCountBarChart(report: MonthlyReport) {
    ReportCard(
        title = "写作分布",
        subtitle = "每日字数",
        icon = Icons.Default.CalendarMonth
    ) {
        val maxWords = report.dailyWordCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 4.dp)
        ) {
            val barCount = report.dailyWordCounts.size
            if (barCount == 0) return@Canvas

            val totalWidth = size.width
            val barSpacing = 2f
            val barWidth = (totalWidth - barSpacing * (barCount - 1)) / barCount
            val maxBarHeight = size.height - 8f

            report.dailyWordCounts.forEachIndexed { i, words ->
                val barHeight = if (words > 0) {
                    (words.toFloat() / maxWords * maxBarHeight).coerceAtLeast(2f)
                } else 0f

                val x = i * (barWidth + barSpacing)
                val y = size.height - barHeight

                // Color intensity based on word count
                val intensity = (words.toFloat() / maxWords).coerceIn(0f, 1f)
                val barColor = Color(0xFF5C9EAD).copy(alpha = 0.3f + 0.7f * intensity)

                if (barHeight > 0) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelCount = minOf(7, report.dailyWordCounts.size)
            val step = if (labelCount > 1) (report.dailyWordCounts.size - 1) / (labelCount - 1) else 1
            for (i in 0 until labelCount) {
                Text(
                    text = "${i * step + 1}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val totalDays = report.dailyWordCounts.count { it > 0 }
        Text(
            text = "本月 $totalDays 天有写作",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        if (report.mostActiveDay != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${report.mostActiveDay} 日写得最多",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== Tag Stats ====================
@Composable
private fun TagStatsCard(report: MonthlyReport) {
    ReportCard(
        title = "标签统计",
        subtitle = "这个月使用的标签",
        icon = Icons.Default.Tag
    ) {
        if (report.tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有使用过标签",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            val topTags = report.tags.take(5)
            topTags.forEachIndexed { i, tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${i + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.width(24.dp)
                    )
                    val tagColor = Color(tag.color)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tagColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = tag.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = tagColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${tag.count} 次",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ==================== Year Comparison ====================
@Composable
private fun YearComparisonCard(report: MonthlyReport, comparison: YearComparison?) {
    ReportCard(
        title = "与去年同期对比",
        subtitle = "${report.year - 1}年${report.month}月 vs ${report.year}年${report.month}月",
        icon = Icons.Default.CompareArrows
    ) {
        if (comparison == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "去年同期无记录",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ComparisonRow(
                    label = "篇数",
                    lastYear = comparison.lastYearEntryCount.toString(),
                    thisYear = report.totalEntries.toString(),
                    delta = comparison.entryCountDelta,
                    deltaText = formatDelta(comparison.entryCountDelta),
                    isPositiveGood = true
                )
                if (comparison.lastYearAvgMood != null && report.avgMood != null && comparison.moodDelta != null) {
                    ComparisonRow(
                        label = "平均心情",
                        lastYear = String.format("%.1f", comparison.lastYearAvgMood),
                        thisYear = String.format("%.1f", report.avgMood),
                        delta = comparison.moodDelta,
                        deltaText = formatDeltaFloat(comparison.moodDelta),
                        isPositiveGood = true
                    )
                }
                ComparisonRow(
                    label = "总字数",
                    lastYear = formatWordCount(comparison.lastYearTotalWords),
                    thisYear = formatWordCount(report.totalWords),
                    delta = comparison.wordsDelta,
                    deltaText = formatDelta(comparison.wordsDelta),
                    isPositiveGood = true
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    lastYear: String,
    thisYear: String,
    delta: Number,
    deltaText: String,
    isPositiveGood: Boolean
) {
    val deltaValue = delta.toFloat()
    val isPositive = deltaValue > 0
    val isZero = deltaValue == 0f
    val deltaColor = when {
        isZero -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        (isPositive && isPositiveGood) || (!isPositive && !isPositiveGood) -> Color(0xFF4CAF50)
        else -> Color(0xFFE57373)
    }
    val arrow = when {
        isZero -> ""
        isPositive -> "\u2191"
        else -> "\u2193"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lastYear,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "  \u2192  ",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = thisYear,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$arrow$deltaText",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = deltaColor
            )
        }
    }
}

// ==================== AI Review ====================
@Composable
private fun AiReviewCard(
    report: MonthlyReport,
    aiReview: String?,
    isLoading: Boolean,
    isAiAvailable: Boolean,
    onGenerate: () -> Unit
) {
    ReportCard(
        title = "AI 月度回顾",
        subtitle = "智能写作分析",
        icon = Icons.Default.AutoAwesome
    ) {
        if (!isAiAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请先在设置中配置 AI 服务",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else if (aiReview != null) {
            Text(
                text = aiReview,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "正在生成回顾...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "生成月度回顾",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==================== Helpers ====================
private fun getMoodColor(mood: Float): Color {
    return when {
        mood >= 4.5f -> Color(0xFFFF7043)
        mood >= 3.5f -> Color(0xFFFFB74D)
        mood >= 2.5f -> Color(0xFF78909C)
        mood >= 1.5f -> Color(0xFF5C6BC0)
        else -> Color(0xFF7986CB)
    }
}

private fun formatCount(words: Int): String {
    return when {
        words >= 10000 -> String.format("%.1fw", words / 10000f)
        words >= 1000 -> String.format("%.1fk", words / 1000f)
        else -> words.toString()
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes >= 60 -> "${minutes / 60}h${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "--"
    }
}

private fun formatDelta(value: Int): String {
    return if (value >= 0) "+$value" else value.toString()
}

private fun formatDeltaFloat(value: Float): String {
    return if (value >= 0) String.format("+%.1f", value) else String.format("%.1f", value)
}

private fun formatWordCount(words: Int): String {
    return if (words >= 1000) String.format("%.1fk", words / 1000f) else words.toString()
}

// ==================== Shared Report Card ====================
@Composable
private fun ReportCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    GlassCard(
        cornerRadius = 20.dp,
        innerPadding = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            content()
        }
    }
}
