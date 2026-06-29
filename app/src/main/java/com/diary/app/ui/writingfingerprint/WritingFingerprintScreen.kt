package com.diary.app.ui.writingfingerprint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WritingFingerprintScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTextMicroscope: () -> Unit = {},
    viewModel: WritingFingerprintViewModel = viewModel()
) {
    val analysis by viewModel.analysis.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "\u5199\u4F5C\u6307\u7EB9", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)
            ) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { viewModel.setTimeRange(range) },
                        label = { Text(range.label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (analysis == null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Text("\u6B63\u5728\u5206\u6790\u5199\u4F5C\u98CE\u683C...", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val data = analysis!!

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "\u4F60\u7684\u5199\u4F5C\u6307\u7EB9",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                            FingerprintVisualization(
                                avgSentenceLength = data.avgSentenceLength,
                                vocabularyRichness = data.vocabularyRichness,
                                punctuationRatio = data.punctuationRatio,
                                modifier = Modifier.size(200.dp)
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                            Text(
                                text = data.styleLabel,
                                fontSize = DesignTokens.FontTitle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

                            CreativityGauge(score = data.creativityScore)
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "\u98CE\u683C\u7279\u5F81",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            FingerprintStatRow(
                                "\u5E73\u5747\u53E5\u957F",
                                "${"%.1f".format(data.avgSentenceLength)} \u5B57",
                                data.comparison?.let { "${if (it.avgSentenceLengthChange >= 0) "+" else ""}${"%.1f".format(it.avgSentenceLengthChange)}" }
                            )
                            FingerprintStatRow(
                                "\u7528\u8BCD\u4E30\u5BCC\u5EA6",
                                "${"%.0f".format(data.vocabularyRichness * 100)}%",
                                data.comparison?.let { "${if (it.vocabularyRichnessChange >= 0) "+" else ""}${"%.1f".format(it.vocabularyRichnessChange * 100)}%" }
                            )
                            FingerprintStatRow("\u5E73\u5747\u8BCD\u957F", "${"%.1f".format(data.avgWordLength)} \u5B57", null)
                            FingerprintStatRow("\u6807\u70B9\u5BC6\u5EA6", "${"%.1f".format(data.punctuationRatio * 100)}%", null)
                            FingerprintStatRow(
                                "\u6BB5\u843D\u6570",
                                "${data.paragraphCount}",
                                data.comparison?.let { "${if (it.totalEntriesChange >= 0) "+" else ""}${it.totalEntriesChange}" }
                            )
                            if (data.comparison != null) {
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                                Text(
                                    text = "\u4E0E\u4E0A\u4E00\u5468\u671F\u5BF9\u6BD4",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (data.dailyWordCounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "\u5199\u4F5C\u8D8B\u52BF",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                WordCountTrendChart(
                                    dailyData = data.dailyWordCounts,
                                    modifier = Modifier.fillMaxWidth().height(160.dp)
                                )
                            }
                        }
                    }

                    if (data.timeDistribution.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "\u5199\u4F5C\u65F6\u95F4\u5206\u5E03",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                TimeDistributionChart(
                                    distribution = data.timeDistribution,
                                    modifier = Modifier.fillMaxWidth().height(120.dp)
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf("\u65E9\u6668", "\u4E0B\u5348", "\u665A\u4E0A", "\u6DF1\u591C").forEach { label ->
                                        Text(
                                            text = "$label ${data.timeDistribution[label] ?: 0}\u7BC7",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "\u98CE\u683C\u6F14\u53D8",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text(
                                text = data.evolutionNote,
                                fontSize = DesignTokens.FontSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToTextMicroscope),
                            innerPadding = DesignTokens.SpacingMd
                        ) {
                            Column {
                                Text(
                                    text = "\u6587\u5B57\u663E\u5FAE\u955C",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "\u6DF1\u5EA6\u5206\u6790\u6587\u5B57\u8BCD\u6C47\u4E0E\u8BED\u6CD5\u7279\u5F81",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
private fun CreativityGauge(score: Int) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFF8BC34A)
        score >= 40 -> Color(0xFFFFC107)
        score >= 20 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val sweep = (score / 100f) * 270f
            drawArc(
                color = surfaceVariant,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawArc(
                color = scoreColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$score",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = "/100",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "\u521B\u4F5C\u529B\u8BC4\u5206",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WordCountTrendChart(
    dailyData: List<Pair<Long, Int>>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val maxCount = dailyData.maxOfOrNull { it.second } ?: 1
    val minCount = dailyData.minOfOrNull { it.second } ?: 0
    val range = (maxCount - minCount).coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val stepX = size.width / (dailyData.size.coerceAtLeast(2) - 1).coerceAtLeast(1)
        val topPadding = 8f
        val bottomPadding = 8f
        val chartHeight = size.height - topPadding - bottomPadding

        val path = Path()
        dailyData.forEachIndexed { i, (_, count) ->
            val x = i * stepX
            val y = topPadding + chartHeight * (1f - (count - minCount).toFloat() / range)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = primaryColor.copy(alpha = 0.6f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        if (dailyData.size <= 14) {
            dailyData.forEachIndexed { i, (_, count) ->
                val x = i * stepX
                val y = topPadding + chartHeight * (1f - (count - minCount).toFloat() / range)
                drawCircle(primaryColor, radius = 3f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun TimeDistributionChart(
    distribution: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFFFFB74D),
        Color(0xFF81C784),
        Color(0xFF64B5F6),
        Color(0xFFCE93D8)
    )
    val items = listOf("\u65E9\u6668", "\u4E0B\u5348", "\u665A\u4E0A", "\u6DF1\u591C")
    val maxVal = (items.maxOfOrNull { distribution[it] ?: 0 } ?: 1).coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val barWidth = size.width / (items.size * 2f + 1f)
        val gap = barWidth
        val chartHeight = size.height - 20f

        items.forEachIndexed { i, label ->
            val count = distribution[label] ?: 0
            val barHeight = (count.toFloat() / maxVal) * chartHeight
            val x = gap + i * (barWidth + gap)
            val y = chartHeight - barHeight

            drawRoundRect(
                color = colors[i % colors.size].copy(alpha = 0.7f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight.coerceAtLeast(1f)),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
private fun FingerprintVisualization(
    avgSentenceLength: Float,
    vocabularyRichness: Float,
    punctuationRatio: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2 * 0.8f

        val rings = 10
        for (i in 1..rings) {
            val radius = maxRadius * i / rings
            val alpha = 0.1f + (i.toFloat() / rings) * 0.3f
            val distortion = (avgSentenceLength / 50f).coerceIn(0.5f, 1.5f)
            drawCircle(
                color = primaryColor.copy(alpha = alpha),
                radius = radius * distortion,
                center = Offset(centerX, centerY)
            )
        }

        val lines = 36
        for (i in 0 until lines) {
            val angle = i * 10 * Math.PI / 180
            val lengthVariation = vocabularyRichness * 50f + 30f
            val length = lengthVariation + (i % 5) * 15f
            val startX = centerX + 30f * cos(angle).toFloat()
            val startY = centerY + 30f * sin(angle).toFloat()
            val endX = centerX + (30f + length) * cos(angle).toFloat()
            val endY = centerY + (30f + length) * sin(angle).toFloat()
            drawLine(
                primaryColor.copy(alpha = 0.3f),
                Offset(startX, startY),
                Offset(endX, endY),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun FingerprintStatRow(label: String, value: String, changeStr: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (changeStr != null) {
                val isPositive = changeStr.startsWith("+")
                Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                Text(
                    text = changeStr,
                    fontSize = 11.sp,
                    color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}
