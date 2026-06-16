package com.diary.app.ui.annualreport

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val moodLabels = listOf("", "很差", "不好", "一般", "不错", "很棒")
private val monthLabels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
private val timeLabels = listOf("上午", "中午", "下午", "傍晚", "深夜", "凌晨")
private val weatherEmoji = mapOf(
    "晴" to "☀", "多云" to "☁", "阴" to "☁", "雨" to "🌧",
    "雪" to "❄", "雾" to "🌫", "风" to "🌬"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnnualReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnnualReportViewModel = viewModel()
) {
    val report by viewModel.report.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadReport(LocalDate.now().year)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { Text("年度报告") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                            text = "还没有日记数据",
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
            } else {
                val pagerState = rememberPagerState(pageCount = { 15 }) // 更新页数为15

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> CoverCard(currentReport)
                            1 -> MoodJourneyCard(currentReport)
                            2 -> ProductiveMonthCard(currentReport)
                            3 -> NightWriterCard(currentReport)
                            4 -> LongestEntryCard(currentReport)
                            5 -> SilenceCard(currentReport)
                            6 -> HappiestDayCard(currentReport)
                            7 -> WritingHabitCard(currentReport)
                            8 -> EarlyBirdCard(currentReport) // 新增：早期鸟卡片
                            9 -> WeatherMoodCard(currentReport)
                            10 -> MostProductiveDayCard(currentReport) // 新增：最多产的一天
                            11 -> TagStyleCard(currentReport)
                            12 -> PhotoStoryCard(currentReport) // 新增：照片故事卡片
                            13 -> MilestoneCard(currentReport)
                            14 -> EndingCard(currentReport)
                        }
                    }

                    // Page indicator with improved visual design
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(15) { index ->
                            val isSelected by animateFloatAsState(
                                targetValue = if (pagerState.currentPage == index) 1f else 0.4f,
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                label = "indicatorScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(6.dp * isSelected)
                                    .background(
                                        if (pagerState.currentPage == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .scale(isSelected, isSelected)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Card 0: Cover ====================
@Composable
private fun CoverCard(report: AnnualReport) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    // 添加计数动画
    val animatedTotalEntries by animateIntAsState(
        targetValue = report.totalEntries,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "animatedTotalEntries"
    )

    val animatedTotalWords by animateIntAsState(
        targetValue = report.totalWords,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "animatedTotalWords"
    )

    val animatedLongestStreak by animateIntAsState(
        targetValue = report.longestStreak,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "animatedLongestStreak"
    )

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${report.year}",
                fontSize = 72.sp,
                fontWeight = FontWeight.Thin,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                letterSpacing = 8.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "年度报告",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                StatColumn(label = "篇日记", value = animatedTotalEntries.toString())
                StatColumn(
                    label = "万字",
                    value = String.format("%.1f", animatedTotalWords / 10000f)
                )
                StatColumn(label = "天连续", value = animatedLongestStreak.toString())
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// ==================== Card 1: Mood Journey ====================
@Composable
private fun MoodJourneyCard(report: AnnualReport) {
    CardScaffold(
        title = "心情旅程",
        subtitle = "一年中的情绪变化",
        icon = Icons.Default.Mood
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

        val validData = report.monthlyMood.mapIndexed { i, v -> i to v }
            .filter { it.second != null }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            if (validData.size < 2) return@Canvas

            val points = validData.mapNotNull { (i, mood) ->
                mood?.let { m ->
                    Offset(
                        x = size.width * i / 11f,
                        y = size.height * (1f - (m - 1f) / 4f)
                    )
                }
            }

            // Draw grid lines
            for (i in 1..4) {
                val y = size.height * (1f - i / 5f)
                drawLine(
                    color = surfaceVariant.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            // Draw line with animation
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(path, primary, style = Stroke(width = 3f))

            // Draw animated dots
            points.forEach { p ->
                drawCircle(color = primary, radius = 5f, center = p)
                drawCircle(color = Color.White, radius = 2.5f, center = p)
            }
        }

        // Y-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1月", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Text("6月", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Text("12月", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mood legend
        val avgMood = report.monthlyMood.filterNotNull().let { if (it.isNotEmpty()) it.average() else 0.0 }
        if (avgMood > 0) {
            Text(
                text = "全年平均心情：${moodLabels[avgMood.toInt().coerceIn(1, 5)]}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            val maxMood = report.monthlyMood.filterNotNull().maxOrNull()?.toInt() ?: 1
            Text(
                text = "最高月度心情：${moodLabels[maxMood.coerceIn(1, 5)]}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== Card 2: Productive Month ====================
@Composable
private fun ProductiveMonthCard(report: AnnualReport) {
    CardScaffold(
        title = "最多产的月份",
        subtitle = "每月写作篇数",
        icon = Icons.Default.TrendingUp
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val maxCount = report.monthlyCount.maxOrNull()?.coerceAtLeast(1) ?: 1

        // 使用LaunchedEffect和animateFloatAsState来实现柱状图动画
        val animationProgress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "animationProgress"
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 8.dp)
        ) {
            val barWidth = size.width / 15f
            val gap = (size.width - barWidth * 12) / 11f

            report.monthlyCount.forEachIndexed { i, count ->
                val targetHeight = (count.toFloat() / maxCount) * (size.height - 24f)
                val currentHeight = targetHeight * animationProgress

                val barHeight = currentHeight
                val x = i * (barWidth + gap)
                val y = size.height - barHeight

                drawRoundRect(
                    color = if (count == report.monthlyCount.maxOrNull()) primary
                    else primary.copy(alpha = 0.4f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1月", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text("6月", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text("12月", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        val maxMonth = report.monthlyCount.indexOf(report.monthlyCount.maxOrNull())
        Text(
            text = "最高产的是 ${monthLabels[maxMonth]}，写了 ${report.monthlyCount[maxMonth]} 篇",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
        )
    }
}

// ==================== Card 3: Night Writer ====================
@Composable
private fun NightWriterCard(report: AnnualReport) {
    CardScaffold(
        title = "深夜写作者",
        subtitle = "凌晨 0-6 点的写作",
        icon = Icons.Default.NightsStay
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val ratio = if (report.totalEntries > 0) report.nightEntries * 100f / report.totalEntries else 0f

        // 为圆弧图表添加动画
        val animatedRatio by animateFloatAsState(
            targetValue = ratio,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            label = "animatedRatio"
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                // Background arc
                drawArc(
                    color = primary.copy(alpha = 0.12f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 12f)
                )
                // Animated value arc
                drawArc(
                    color = primary,
                    startAngle = 135f,
                    sweepAngle = 270f * (animatedRatio / 100f).coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 12f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${report.nightEntries}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
                Text(
                    text = "篇",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (report.latestEntryTime != null) {
            Text(
                text = "最晚写到了 ${report.latestEntryTime.hour}:${String.format("%02d", report.latestEntryTime.minute)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            if (report.latestEntryTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "「${report.latestEntryTitle}」",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==================== Card 4: Longest Entry ====================
@Composable
private fun LongestEntryCard(report: AnnualReport) {
    CardScaffold(
        title = "最长的一篇",
        subtitle = "你写得最多的一天",
        icon = Icons.Default.ShortText
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val formatter = remember { DateTimeFormatter.ofPattern("M月d日") }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawCircle(
                    color = primary.copy(alpha = 0.1f),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = primary.copy(alpha = 0.05f),
                    radius = size.minDimension / 2 + 8f,
                    style = Stroke(width = 2f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = report.longestWords.toString(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
                Text(
                    text = "字",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (report.longestEntryTitle.isNotBlank()) {
            Text(
                text = "「${report.longestEntryTitle}」",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (report.longestEntryDate != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.longestEntryDate.format(formatter),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== Card 5: Longest Silence ====================
@Composable
private fun SilenceCard(report: AnnualReport) {
    CardScaffold(
        title = "最沉默的日子",
        subtitle = "两次写日记之间的最长间隔",
        icon = Icons.Default.Timer
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val formatter = remember { DateTimeFormatter.ofPattern("M月d日") }

        if (report.longestSilenceDays == 0) {
            Text(
                text = "你全年都在坚持记录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${report.longestSilenceDays}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = primary
                    )
                    Text(
                        text = "天",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (report.silenceStart != null && report.silenceEnd != null) {
                Text(
                    text = "${report.silenceStart.format(formatter)} — ${report.silenceEnd.format(formatter)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==================== Card 6: Happiest Day ====================
@Composable
private fun HappiestDayCard(report: AnnualReport) {
    CardScaffold(
        title = "最开心的一天",
        subtitle = "心情最好的那篇日记",
        icon = Icons.Default.Mood
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val formatter = remember { DateTimeFormatter.ofPattern("M月d日") }

        // Animated emoji scale
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "★",
                fontSize = 56.sp,
                color = primary.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (report.happiestEntryTitle.isNotBlank()) {
            Text(
                text = "「${report.happiestEntryTitle}」",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (report.happiestDate != null) {
                Text(
                    text = report.happiestDate.format(formatter),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "心情：${moodLabels[report.happiestMood.coerceIn(1, 5)]}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ==================== Card 7: Writing Habit ====================
@Composable
private fun WritingHabitCard(report: AnnualReport) {
    CardScaffold(
        title = "写作习惯",
        subtitle = "你最爱在什么时候写日记",
        icon = Icons.Default.CalendarMonth
    ) {
        val primary = MaterialTheme.colorScheme.primary

        // Time distribution bars
        val maxTime = report.timeDistribution.maxOrNull()?.coerceAtLeast(1) ?: 1

        report.timeDistribution.forEachIndexed { i, count ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeLabels[i],
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.width(36.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .background(primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(count.toFloat() / maxTime)
                            .fillMaxSize()
                            .background(
                                if (count == report.timeDistribution.maxOrNull()) primary
                                else primary.copy(alpha = 0.45f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.End
                )
            }
            if (i < report.timeDistribution.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "最常在「${report.mostActiveTime}」写作",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "最喜欢在「${report.mostActiveDay}」动笔",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
        )
    }
}

// ==================== Card 8: Early Bird ====================
@Composable
private fun EarlyBirdCard(report: AnnualReport) {
    CardScaffold(
        title = "你的清晨时光",
        subtitle = "你最早开始记录的一天",
        icon = Icons.Default.WbSunny
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val formatter = remember { DateTimeFormatter.ofPattern("M月d日") }

        if (report.earliestEntryTime != null) {
            // Time visualization
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    // Background arc
                    drawArc(
                        color = primary.copy(alpha = 0.12f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${report.earliestEntryTime.hour}:${String.format("%02d", report.earliestEntryTime.minute)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                    Text(
                        text = "起床就写日记",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "比大多数人醒得都早呢！",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (report.earliestEntryTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "那天是 「${report.earliestEntryTitle}」",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = "暂无记录",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== Card 9: Tag Style ====================
@Composable
private fun TagStyleCard(report: AnnualReport) {
    CardScaffold(
        title = "标签风格",
        subtitle = "你最常使用的标签",
        icon = Icons.Default.Tag
    ) {
        val primary = MaterialTheme.colorScheme.primary

        if (report.topTags.isEmpty()) {
            Text(
                text = "还没有使用过标签",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            report.topTags.forEachIndexed { i, tag ->
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
                        color = primary.copy(alpha = 0.5f),
                        modifier = Modifier.width(24.dp)
                    )
                    val tagColor = Color(tag.color.toULong())
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

// ==================== Card 9: Weather & Mood ====================
@Composable
private fun WeatherMoodCard(report: AnnualReport) {
    CardScaffold(
        title = "天气与心情",
        subtitle = "不同天气下的情绪",
        icon = Icons.Default.WbSunny
    ) {
        val primary = MaterialTheme.colorScheme.primary

        if (report.weatherMood.isEmpty()) {
            Text(
                text = "还没有足够的天气数据",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            report.weatherMood.forEach { wm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = weatherEmoji[wm.weather] ?: "·",
                        fontSize = 18.sp,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = wm.weather,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(wm.avgMood / 5f)
                                .fillMaxSize()
                                .background(primary.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%.1f", wm.avgMood),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "(${wm.count})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// ==================== Card 10: Most Productive Day ====================
@Composable
private fun MostProductiveDayCard(report: AnnualReport) {
    CardScaffold(
        title = "最忙碌的一天",
        subtitle = "你写得最多的一天",
        icon = Icons.Default.LocalFireDepartment
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val formatter = remember { DateTimeFormatter.ofPattern("M月d日") }

        if (report.mostProductiveCount > 1) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = report.mostProductiveCount.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = primary
                    )
                    Text(
                        text = "篇日记",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (report.mostProductiveDate != null) {
                Text(
                    text = "${report.mostProductiveDate.format(formatter)} 这一天",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "一定有很多事情值得记录吧",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = "每天平均一篇，稳步前行",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== Card 12: Photo Story ====================
@Composable
private fun PhotoStoryCard(report: AnnualReport) {
    CardScaffold(
        title = "照片记忆",
        subtitle = "你拍摄的回忆片段",
        icon = Icons.Default.Collections
    ) {
        val primary = MaterialTheme.colorScheme.primary

        if (report.photoCount > 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Show total photos
                Text(
                    text = report.photoCount.toString(),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    color = primary
                )
                Text(
                    text = "张照片",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (report.mostPhotosInSingleEntry > 1) {
                    Text(
                        text = "单篇最多 ${report.mostPhotosInSingleEntry} 张",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    if (report.mostPhotosEntryTitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "来自 「${report.mostPhotosEntryTitle}」",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (report.photoCount > 0) {
                    Text(
                        text = "用镜头记录每一个瞬间",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "今年还没有拍照日记",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "试试为日记配上照片吧",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ==================== Card 13: Milestone ====================
@Composable
private fun MilestoneCard(report: AnnualReport) {
    CardScaffold(
        title = "里程碑",
        subtitle = "这一年的重要节点",
        icon = Icons.Default.EmojiEvents
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val formatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日") }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MilestoneRow(
                label = "第一次动笔",
                value = report.firstEntryDate?.format(formatter) ?: "无"
            )
            MilestoneRow(label = "写了几篇", value = "${report.totalEntries} 篇")
            MilestoneRow(
                label = "写了多少字",
                value = "${report.totalWords.toLocaleString()} 字"
            )
            MilestoneRow(label = "最长连续记录", value = "${report.longestStreak} 天")
            if (report.nightEntries > 0) {
                MilestoneRow(label = "深夜写作", value = "${report.nightEntries} 次")
            }
        }
    }
}

@Composable
private fun MilestoneRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==================== Card 11: Ending ====================
@Composable
private fun EndingCard(report: AnnualReport) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = primary.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "这是你的${report.year}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "每一篇日记都是生活留下的痕迹",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "继续记录，明年见",
                fontSize = 15.sp,
                color = primary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== Shared Card Scaffold ====================
@Composable
private fun CardScaffold(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable BoxScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        GlassCard(
            cornerRadius = 20.dp,
            innerPadding = 24.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    // Card header with enhanced styling
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Card content with enhanced styling
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content()
                    }
                }
            }
        }
    }
}

private fun Int.toLocaleString(): String {
    return this.toString().reversed().chunked(3).joinToString(",").reversed()
}
