package com.diary.app.ui.emotionforecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionForecastScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEmotionRadar: () -> Unit = {},
    viewModel: EmotionForecastViewModel = viewModel()
) {
    val forecast by viewModel.forecast.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "\u60C5\u7EEA\u9884\u62A5", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (isLoading) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Text(
                            "\u6B63\u5728\u5206\u6790\u60C5\u7EEA\u6A21\u5F0F...",
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (errorMsg != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMsg!!,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        OutlinedButton(
                            onClick = { viewModel.generateForecast() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(DesignTokens.IconMedium))
                            Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                            Text("\u91CD\u65B0\u5206\u6790")
                        }
                    }
                }
            } else if (forecast != null) {
                val data = forecast!!

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                Text(
                                    text = "\u660E\u65E5\u9884\u62A5",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                                Text(
                                    text = data.forecastLabel,
                                    fontSize = DesignTokens.FontHeadline,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                Text(
                                    text = "\u57FA\u4E8E\u4F60\u6700\u8FD1\u7BC7\u65E5\u8BB0\u7684\u60C5\u7EEA\u8D70\u52BF",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                        if (data.dailyMoods.isNotEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(
                                        text = "\u60C5\u7EEA\u8D8B\u52BF",
                                        fontSize = DesignTokens.FontMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                    MoodTrendChart(
                                        dailyMoods = data.dailyMoods,
                                        modifier = Modifier.fillMaxWidth().height(150.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        }

                        if (data.weeklySummary.isNotBlank()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(
                                        text = "\u6BCF\u5468\u60C5\u7EEA\u603B\u7ED3",
                                        fontSize = DesignTokens.FontMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                    Text(
                                        text = data.weeklySummary,
                                        fontSize = DesignTokens.FontBody,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "\u5206\u6790\u4F9D\u636E",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                data.reasons.forEach { reason ->
                                    ForecastReason(reason.reason, reason.impact)
                                }
                            }
                        }

                        if (data.triggers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(
                                        text = "\u60C5\u7EEA\u89E6\u53D1\u5668",
                                        fontSize = DesignTokens.FontMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                    data.triggers.forEach { trigger ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = trigger.trigger,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                color = when (trigger.correlation) {
                                                    "\u504F\u9AD8" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                    "\u504F\u4F4E" -> Color(0xFFF44336).copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                },
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = trigger.correlation,
                                                    fontSize = 12.sp,
                                                    color = when (trigger.correlation) {
                                                        "\u504F\u9AD8" -> Color(0xFF4CAF50)
                                                        "\u504F\u4F4E" -> Color(0xFFF44336)
                                                        else -> MaterialTheme.colorScheme.primary
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (data.suggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(
                                        text = "\u5EFA\u8BAE\u884C\u52A8",
                                        fontSize = DesignTokens.FontMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                    data.suggestions.forEachIndexed { i, suggestion ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ".",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(20.dp)
                                            )
                                            Text(
                                                text = suggestion,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                        if (data.calendarData.isNotEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(
                                        text = "\u60C5\u7EEA\u65E5\u5386",
                                        fontSize = DesignTokens.FontMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                    MoodCalendar(
                                        calendarData = data.calendarData,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "\u6E29\u99A8\u63D0\u793A",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                Text(
                                    text = "\u60C5\u7EEA\u9884\u62A5\u4E0D\u662F\u9884\u6D4B\u672A\u6765\uFF0C\u800C\u662F\u5E2E\u52A9\u4F60\u89C9\u5BDF\u81EA\u5DF1\u7684\u60C5\u7EEA\u6A21\u5F0F\u3002\u65E0\u8BBA\u660E\u5929\u611F\u89C9\u5982\u4F55\uFF0C\u90FD\u662F\u6B63\u5E38\u7684\u3002",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToEmotionRadar),
                            innerPadding = DesignTokens.SpacingMd
                        ) {
                            Column {
                                Text(
                                    text = "\u60C5\u7EEA\u96F7\u8FBE",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "\u591A\u7EF4\u5EA6\u60C5\u7EEA\u53EF\u89C6\u5316\u5206\u6790",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodTrendChart(
    dailyMoods: List<DailyMood>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val moodColors = listOf(
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFFFFC107),
        Color(0xFF8BC34A),
        Color(0xFF4CAF50),
        Color(0xFF2196F3)
    )

    Column {
        Canvas(modifier = modifier) {
            val stepX = size.width / (dailyMoods.size.coerceAtLeast(2) - 1).coerceAtLeast(1)
            val topPadding = 8f
            val bottomPadding = 8f
            val chartHeight = size.height - topPadding - bottomPadding

            val path = Path()
            dailyMoods.forEachIndexed { i, dailyMood ->
                val x = i * stepX
                val y = topPadding + chartHeight * (1f - (dailyMood.moodLevel - 1) / 5f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.6f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            dailyMoods.forEachIndexed { i, dailyMood ->
                val x = i * stepX
                val y = topPadding + chartHeight * (1f - (dailyMood.moodLevel - 1) / 5f)
                val color = moodColors[(dailyMood.moodLevel - 1).coerceIn(0, 5)]
                drawCircle(color, radius = 5f, center = Offset(x, y))
                drawCircle(Color.White, radius = 3f, center = Offset(x, y))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyMoods.forEach { dailyMood ->
                Text(
                    text = dailyMood.date.format(DateTimeFormatter.ofPattern("M/d")),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MoodCalendar(
    calendarData: List<CalendarDay>,
    modifier: Modifier = Modifier
) {
    val moodColors = listOf(
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFFFFC107),
        Color(0xFF8BC34A),
        Color(0xFF4CAF50),
        Color(0xFF2196F3)
    )

    val cellSize = 14.dp
    val cellGap = 3.dp
    val daysPerWeek = 7

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("\u4E00", "\u4E8C", "\u4E09", "\u56DB", "\u4E94", "\u516D", "\u65E5").forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.width(cellSize),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))

        val weeks = calendarData.chunked(daysPerWeek)
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                val dayOfWeek = week.firstOrNull()?.date?.dayOfWeek?.value ?: 1
                val startPadding = (dayOfWeek - 1).coerceIn(0, 6)
                if (startPadding > 0) {
                    Spacer(modifier = Modifier.width(cellSize * startPadding + cellGap * startPadding))
                }
                week.forEach { day ->
                    val color = day.moodLevel?.let { moodColors[(it - 1).coerceIn(0, 5)] }
                        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(cellGap))
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)
        ) {
            Text("\u5C11", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            (1..6).forEach { level ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = moodColors[(level - 1).coerceIn(0, 5)],
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
            Text("\u591A", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun ForecastReason(reason: String, impact: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = reason, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = impact,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
