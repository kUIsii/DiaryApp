package com.diary.app.ui.weeklyreport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.storage.formatFileSize

@Composable
fun WeeklyReportScreen(
    onNavigateBack: () -> Unit,
    onShare: (WeeklyReport?) -> Unit = {},
    viewModel: WeeklyReportViewModel = viewModel()
) {
    val report by viewModel.report.collectAsState()

    // Load current week on first composition
    if (report == null) {
        viewModel.loadCurrentWeek()
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "周报",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onShare(report) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (report == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "本周还没有日记",
                        fontSize = 16.sp,
                        color = textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // Week header
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${report!!.year}年第${report!!.weekNumber}周",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "${report!!.startDate} - ${report!!.endDate}",
                                    fontSize = 14.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    // Stats summary
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Edit,
                                label = "日记",
                                value = "${report!!.totalEntries}篇",
                                color = Color(0xFF6366F1)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.EmojiEvents,
                                label = "写作天数",
                                value = "${report!!.activeDays}天",
                                color = Color(0xFF10B981)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Timer,
                                label = "总字数",
                                value = formatWordCount(report!!.totalWords),
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }

                    // Mood chart
                    if (report!!.dailyMoodAverages.any { it != null }) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "心情走势",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MoodChart(
                                        dailyMoods = report!!.dailyMoodAverages,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Word count chart
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "每日字数",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                WordCountChart(
                                    dailyCounts = report!!.dailyWordCounts,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                            }
                        }
                    }

                    // Top tags
                    if (report!!.tags.isNotEmpty()) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "常用标签",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    report!!.tags.forEach { tag ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(tag.color))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tag.name,
                                                fontSize = 14.sp,
                                                color = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${tag.count}篇",
                                                fontSize = 13.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Longest entry
                    if (report!!.longestEntryTitle.isNotEmpty()) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "最长日记",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = report!!.longestEntryTitle,
                                        fontSize = 15.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = "${report!!.longestWords}字",
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoodChart(
    dailyMoods: List<Float?>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val dayWidth = width / 6

        val points = dailyMoods.mapIndexed { index, mood ->
            if (mood != null) {
                Offset(
                    x = index * dayWidth,
                    y = height - ((mood - 1) / 4f) * height
                )
            } else null
        }

        // Draw line
        val path = Path()
        var firstPoint = true
        points.forEach { point ->
            if (point != null) {
                if (firstPoint) {
                    path.moveTo(point.x, point.y)
                    firstPoint = false
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Draw dots
        points.forEach { point ->
            if (point != null) {
                drawCircle(
                    color = primaryColor,
                    radius = 5f,
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = point
                )
            }
        }
    }
}

@Composable
private fun WordCountChart(
    dailyCounts: List<Int>,
    modifier: Modifier = Modifier
) {
    val barColor = Color(0xFF6366F1)
    val maxCount = dailyCounts.maxOrNull() ?: 1

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / 7 * 0.6f
        val gap = width / 7

        dailyCounts.forEachIndexed { index, count ->
            val barHeight = if (maxCount > 0) (count.toFloat() / maxCount) * height else 0f
            val x = index * gap + (gap - barWidth) / 2

            drawRect(
                color = barColor.copy(alpha = 0.3f),
                topLeft = Offset(x, height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}

private fun formatWordCount(count: Int): String {
    return when {
        count < 1000 -> "${count}字"
        count < 10000 -> String.format("%.1f千字", count / 1000.0)
        else -> String.format("%.1f万字", count / 10000.0)
    }
}
