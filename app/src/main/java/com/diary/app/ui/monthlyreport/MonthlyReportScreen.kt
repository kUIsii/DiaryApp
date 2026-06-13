package com.diary.app.ui.monthlyreport

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
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MonthlyReportScreen(
    year: Int,
    month: Int,
    onNavigateBack: () -> Unit,
    viewModel: MonthlyReportViewModel = viewModel()
) {
    val report by viewModel.report.collectAsState()

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
            } else {
                val pageCount = 7
                val pagerState = rememberPagerState(pageCount = { pageCount })

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
                            1 -> MoodOverviewCard(currentReport)
                            2 -> DailyWordCountCard(currentReport)
                            3 -> LongestEntryCard(currentReport)
                            4 -> NightWriterCard(currentReport)
                            5 -> TagStatsCard(currentReport)
                            6 -> EndingCard(currentReport)
                        }
                    }

                    // Page indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pageCount) { index ->
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
private fun CoverCard(report: MonthlyReport) {
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
                fontSize = 48.sp,
                fontWeight = FontWeight.Thin,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = monthNames[report.month],
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "月度报告",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                StatColumn(label = "篇日记", value = animatedTotalEntries.toString())
                StatColumn(
                    label = "字",
                    value = String.format("%.1fk", animatedTotalWords / 1000f)
                )
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

// ==================== Card 1: Mood Overview ====================
@Composable
private fun MoodOverviewCard(report: MonthlyReport) {
    CardScaffold(
        title = "心情概览",
        subtitle = "这个月的情绪",
        icon = Icons.Default.Mood
    ) {
        val primary = MaterialTheme.colorScheme.primary

        if (report.avgMood != null) {
            val animatedMood by animateFloatAsState(
                targetValue = report.avgMood,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                label = "animatedMood"
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    drawArc(
                        color = primary.copy(alpha = 0.12f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12f)
                    )
                    drawArc(
                        color = primary,
                        startAngle = 135f,
                        sweepAngle = 270f * (animatedMood / 5f).coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = 12f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f", report.avgMood),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                    Text(
                        text = moodLabels[report.avgMood.toInt().coerceIn(1, 5)],
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (report.happiestEntryTitle.isNotBlank()) {
                Text(
                    text = "最开心的一篇",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "心情：${moodLabels[report.happiestMood.coerceIn(1, 5)]}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有心情记录",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==================== Card 2: Daily Word Count ====================
@Composable
private fun DailyWordCountCard(report: MonthlyReport) {
    CardScaffold(
        title = "写作习惯",
        subtitle = "每日字数变化",
        icon = Icons.Default.CalendarMonth
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

        val maxWords = report.dailyWordCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            val points = report.dailyWordCounts.mapIndexed { i, words ->
                Offset(
                    x = size.width * i / (report.dailyWordCounts.size - 1).coerceAtLeast(1).toFloat(),
                    y = size.height * (1f - words.toFloat() / maxWords)
                )
            }

            // Grid lines
            for (i in 1..4) {
                val y = size.height * (1f - i / 5f)
                drawLine(
                    color = surfaceVariant.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(path, primary, style = Stroke(width = 3f))
            }

            points.forEach { p ->
                drawCircle(color = primary, radius = 4f, center = p)
                drawCircle(color = Color.White, radius = 2f, center = p)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1日", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            val mid = report.dailyWordCounts.size / 2
            Text("${mid}日", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Text("${report.dailyWordCounts.size}日", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        val totalDays = report.dailyWordCounts.count { it > 0 }
        Text(
            text = "本月有 $totalDays 天在写日记",
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

// ==================== Card 3: Longest Entry ====================
@Composable
private fun LongestEntryCard(report: MonthlyReport) {
    CardScaffold(
        title = "最长的一篇",
        subtitle = "这个月写得最多的一天",
        icon = Icons.Default.ShortText
    ) {
        val primary = MaterialTheme.colorScheme.primary

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
    }
}

// ==================== Card 4: Night Writer ====================
@Composable
private fun NightWriterCard(report: MonthlyReport) {
    CardScaffold(
        title = "深夜写作者",
        subtitle = "凌晨 0-6 点的写作",
        icon = Icons.Default.NightsStay
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val ratio = if (report.totalEntries > 0) report.nightEntries * 100f / report.totalEntries else 0f

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
                drawArc(
                    color = primary.copy(alpha = 0.12f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 12f)
                )
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

        if (report.earliestEntryTime != null) {
            Text(
                text = "最早写到了 ${report.earliestEntryTime.hour}:${String.format("%02d", report.earliestEntryTime.minute)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else if (report.nightEntries > 0) {
            Text(
                text = "有 ${report.nightEntries} 篇写到了深夜",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "这个月没有深夜写作",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== Card 5: Tag Stats ====================
@Composable
private fun TagStatsCard(report: MonthlyReport) {
    CardScaffold(
        title = "标签统计",
        subtitle = "这个月使用的标签",
        icon = Icons.Default.Tag
    ) {
        val primary = MaterialTheme.colorScheme.primary

        if (report.tags.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有使用过标签",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            report.tags.forEachIndexed { i, tag ->
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

// ==================== Card 6: Ending ====================
@Composable
private fun EndingCard(report: MonthlyReport) {
    val primary = MaterialTheme.colorScheme.primary

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
                text = "这是你的${monthNames[report.month]}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "共 ${report.totalEntries} 篇日记",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "继续记录下一个月吧",
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

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content()
                    }
                }
            }
        }
    }
}
