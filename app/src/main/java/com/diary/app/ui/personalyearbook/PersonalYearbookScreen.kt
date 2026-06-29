package com.diary.app.ui.personalyearbook

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import java.time.Year
import kotlin.math.roundToInt

@Composable
fun PersonalYearbookScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: PersonalYearbookViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedYear by remember { mutableStateOf(Year.now().value) }
    val yearbook by viewModel.yearbook.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val showSkeleton by viewModel.showSkeleton.collectAsState()
    val aiPhase by viewModel.aiAnalysisPhase.collectAsState()
    val timelineEvents by viewModel.timelineEvents.collectAsState()

    LaunchedEffect(exportResult) {
        if (exportResult != null) {
            if (exportResult!!.startsWith("导出失败")) {
                android.widget.Toast.makeText(context, exportResult, android.widget.Toast.LENGTH_LONG).show()
            } else {
                viewModel.sharePDF(context)
            }
            viewModel.clearExportResult()
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "个人年鉴", onNavigateBack = onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "将一年的日记精华汇编成可导出的精美文档",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                YearSelectorSection(
                    selectedYear = selectedYear,
                    isGenerating = isGenerating,
                    generatingPhase = aiPhase,
                    onYearChange = { selectedYear = it },
                    onGenerate = { viewModel.generate(selectedYear) }
                )

                if (isGenerating && showSkeleton) {
                    SkeletonLoadingSection(phase = aiPhase)
                }

                if (yearbook != null && !isGenerating) {
                    val data = yearbook!!

                    MetaphorSection(data.metaphor, data.metaphorEvolution)

                    StatsSection(data)

                    if (data.arcs.isNotEmpty()) {
                        NarrativeArcsSection(data.arcs, onNavigateToDetail)
                    }

                    if (data.monthHighlights.isNotEmpty()) {
                        MonthHighlightsSection(data.monthHighlights, onNavigateToDetail)
                    }

            if (data.stats.monthlyDistribution.isNotEmpty()) {
                    TimelineSection(
                        monthlyDistribution = data.stats.monthlyDistribution,
                        events = timelineEvents
                    )
                }

                    if (data.topPhotos.isNotEmpty()) {
                        PhotoGallerySection(data.topPhotos)
                    }

                    ExportSection(
                        isExporting = isExporting,
                        onExportPdf = { viewModel.exportPDF() },
                        onShare = { viewModel.sharePDF(context) }
                    )
                } else if (yearbook == null && !isGenerating) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        innerPadding = 24.dp
                    ) {
                        Text(
                            text = "选择年份并点击生成",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun YearSelectorSection(
    selectedYear: Int,
    isGenerating: Boolean,
    generatingPhase: String,
    onYearChange: (Int) -> Unit,
    onGenerate: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "选择年份",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val years = (2018..Year.now().value).toList()
                years.forEach { year ->
                    val isSel = year == selectedYear
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { onYearChange(year) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = year.toString(),
                            fontSize = 14.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (isGenerating) Icons.Default.AutoAwesome else Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGenerating) "生成中..." else "生成 $selectedYear 年鉴")
            }
            if (isGenerating && generatingPhase.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SkeletonLoadingSection(phase: String) {
    val alpha by animateFloatAsState(
        targetValue = 0.6f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "skeletonAlpha"
    )
    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor)
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(phase, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetaphorSection(metaphor: String, evolution: List<MetaphorPhase>) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (metaphor.isNotEmpty()) {
                Text(
                    text = metaphor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
            if (evolution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                evolution.forEach { phase ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = phase.period,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = phase.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSection(data: YearbookData) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Column {
            Text(
                text = "${data.year} 年鉴预览",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (data.stats.topMood != null) {
                Text(
                    text = "年度最常情绪：${moodLabel(data.stats.topMood)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            YearbookStatCard(
                icon = Icons.Default.BarChart,
                label = "总日记数",
                value = "${data.stats.totalEntries} 篇"
            )
            Spacer(modifier = Modifier.height(8.dp))
            YearbookStatCard(
                icon = Icons.Default.Favorite,
                label = "总字数",
                value = "%,d 字".format(data.stats.totalWords)
            )
            Spacer(modifier = Modifier.height(8.dp))
            YearbookStatCard(
                icon = Icons.Default.EmojiEvents,
                label = "最佳月份",
                value = data.stats.bestMonth
            )
            Spacer(modifier = Modifier.height(8.dp))
            YearbookStatCard(
                icon = Icons.Default.CalendarMonth,
                label = "最长连续",
                value = "${data.stats.longestStreak} 天"
            )
        }
    }
}

@Composable
private fun NarrativeArcsSection(arcs: List<NarrativeArc>, onNavigateToDetail: (Long) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "年度叙事脉络",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            arcs.forEach { arc ->
                ChapterCard(arc = arc, onClick = { onNavigateToDetail(arc.turningPoint) })
                if (arc != arcs.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(arc: NarrativeArc, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = arc.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${arc.entries.size} 篇",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = arc.summary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            if (arc.emotionTrajectory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                EmotionTrajectoryBar(arc.emotionTrajectory)
            }
        }
    }
}

@Composable
private fun EmotionTrajectoryBar(trajectory: List<EmotionPoint>) {
    val moodColors = listOf(
        0xFF90CAF9.toInt(), 0xFF81D4FA.toInt(), 0xFFA5D6A7.toInt(),
        0xFFFFF59D.toInt(), 0xFFFFCC80.toInt(), 0xFFEF9A9A.toInt()
    )
    Canvas(
        modifier = Modifier.fillMaxWidth().height(20.dp)
    ) {
        if (trajectory.isEmpty()) return@Canvas
        val barWidth = size.width / trajectory.size
        trajectory.forEachIndexed { i, point ->
            val colorIndex = (point.value.roundToInt() - 1).coerceIn(0, 5)
            drawRoundRect(
                color = Color(moodColors[colorIndex]),
                topLeft = Offset(i * barWidth + 1f, 2f),
                size = Size(barWidth - 2f, size.height - 4f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

@Composable
private fun MonthHighlightsSection(highlights: List<MonthHighlight>, onNavigateToDetail: (Long) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "每月精选",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            highlights.forEach { highlight ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDetail(highlight.entryId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${highlight.month}月",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = highlight.entryTitle.ifEmpty { "无标题" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = highlight.reason,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TimelineSection(
    monthlyDistribution: List<Int>,
    events: List<TimelineEvent>
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "年度时间线",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val maxVal = (monthlyDistribution.maxOrNull() ?: 1).coerceAtLeast(1)
            val barColors = listOf(
                Color(0xFFEF9A9A), Color(0xFFFFAB91), Color(0xFFFFCC80),
                Color(0xFFFFF59D), Color(0xFFC5E1A5), Color(0xFFA5D6A7),
                Color(0xFF80CBC4), Color(0xFF81D4FA), Color(0xFF90CAF9),
                Color(0xFFB39DDB), Color(0xFFCE93D8), Color(0xFFF48FB1)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(monthlyDistribution.size) { monthIndex ->
                    val count = monthlyDistribution[monthIndex]
                    val monthEvents = events.filter { event ->
                        try {
                            val parts = event.date.split("-")
                            parts.size >= 2 && parts[1].toInt() == monthIndex + 1
                        } catch (_: Exception) { false }
                    }
                    MonthTimelineColumn(
                        monthIndex = monthIndex,
                        count = count,
                        maxVal = maxVal,
                        barColor = barColors[monthIndex],
                        events = monthEvents
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthTimelineColumn(
    monthIndex: Int,
    count: Int,
    maxVal: Int,
    barColor: Color,
    events: List<TimelineEvent>
) {
    val monthLabels = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")

    val barHeightFraction = if (maxVal > 0) count.toFloat() / maxVal else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        Text(
            text = monthLabels[monthIndex],
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(16.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height((barHeightFraction * 100).dp.coerceAtLeast(2.dp))
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }

        events.forEach { event ->
            val dotColor = when (event.type) {
                "trip" -> Color(0xFF4CAF50)
                "achievement" -> Color(0xFFFFC107)
                "life_change" -> Color(0xFF2196F3)
                "celebration" -> Color(0xFFE91E63)
                "health" -> Color(0xFFF44336)
                "relationship" -> Color(0xFF9C27B0)
                else -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun PhotoGallerySection(photos: List<String>) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "年度精选照片",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI 从 ${photos.size} 张照片中精选",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 120.dp)
            ) {
                items(photos.take(12)) { path ->
                    PhotoThumbnail(path = path)
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(path: String) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = java.io.File(path),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ExportSection(
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onShare: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onExportPdf,
                modifier = Modifier.weight(1f),
                enabled = !isExporting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (isExporting) Icons.Default.AutoAwesome else Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isExporting) "导出中..." else "导出 PDF", fontSize = 13.sp)
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("分享", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun YearbookStatCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

private fun moodLabel(level: Int?): String = when (level) {
    1 -> "沮丧"
    2 -> "低落"
    3 -> "一般"
    4 -> "不错"
    5 -> "开心"
    6 -> "兴奋"
    else -> "暂无数据"
}
