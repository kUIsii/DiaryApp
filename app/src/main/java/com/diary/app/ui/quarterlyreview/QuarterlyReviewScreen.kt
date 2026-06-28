package com.diary.app.ui.quarterlyreview

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@Composable
fun QuarterlyReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuarterlyReviewViewModel = viewModel()
) {
    val data by viewModel.quarterlyData.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                    text = "季度回顾",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val d = data!!

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "${d.year}年 第${d.quarter}季度",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "共 ${d.totalEntries} 篇日记",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${d.totalWords}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "总字数",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = moodEmoji(d.topMood),
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${"%.1f".format(d.avgMood)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "平均心情",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (d.topTags.isNotEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "热门标签",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            d.topTags.forEachIndexed { index, tag ->
                                Text(
                                    text = "${index + 1}. $tag",
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (d.moodTrend.isNotEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                text = "心情趋势",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MoodTrendChart(
                                moods = d.moodTrend,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun moodEmoji(mood: Int): String {
    return when (mood) {
        1 -> "😢"
        2 -> "😕"
        3 -> "😐"
        4 -> "😊"
        5 -> "😄"
        6 -> "🤩"
        else -> "😐"
    }
}

@Composable
private fun MoodTrendChart(
    moods: List<Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        if (moods.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val stepX = width / (moods.size - 1).coerceAtLeast(1)

        val path = Path()
        moods.forEachIndexed { index, mood ->
            val x = index * stepX
            val y = height - (mood / 6f) * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 2f)
        )

        moods.forEachIndexed { index, mood ->
            val x = index * stepX
            val y = height - (mood / 6f) * height
            drawCircle(
                color = primaryColor,
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}
