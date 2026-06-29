package com.diary.app.ui.emotionarc

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens

@Composable
fun EmotionArcScreen(
    diaryId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: EmotionArcViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(diaryId) {
        viewModel.loadData(diaryId)
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
                .verticalScroll(rememberScrollState())
        ) {
            PageHeader(title = state.title, onNavigateBack = onNavigateBack)

            Spacer(Modifier.height(DesignTokens.SpacingLg))

            PeriodSelector(
                selected = state.selectedPeriod,
                onSelect = { viewModel.updatePeriod(it) }
            )

            Spacer(Modifier.height(DesignTokens.SpacingLg))

            when {
                state.isLoading -> LoadingSkeleton()
                state.error != null -> ErrorDisplay(state.error!!)
                else -> {
                    ChartSection(
                        dailyEmotions = state.dailyEmotions,
                        forecast = state.analysis?.forecast
                    )

                    Spacer(Modifier.height(DesignTokens.SpacingLg))

                    AiAnalysisSection(
                        isAnalyzing = state.isAiAnalyzing,
                        analysis = state.analysis,
                        isAiEnabled = state.isAiEnabled,
                        comparisonText = state.comparisonText,
                        onCompare = { viewModel.comparePeriods(30, 60) }
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.SpacingXxl))
        }
    }
}

@Composable
private fun PeriodSelector(selected: PeriodType, onSelect: (PeriodType) -> Unit) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
            .background(bgColor)
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PeriodType.values().forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(period) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (period) {
                        PeriodType.WEEK -> "近一周"
                        PeriodType.MONTH -> "近一月"
                        PeriodType.QUARTER -> "近三月"
                    },
                    fontSize = if (isSelected) DesignTokens.FontBody else DesignTokens.FontSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartSection(
    dailyEmotions: List<DailyEmotion>,
    forecast: List<ForecastPoint>?
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Text("情绪变化曲线", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(DesignTokens.SpacingMd))

        EmotionArcChart(
            dailyEmotions = dailyEmotions,
            forecast = forecast,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(Modifier.height(DesignTokens.SpacingMd))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("积极", Color(0xFF4CAF50))
            LegendItem("中性", Color(0xFFFFC107))
            LegendItem("消极", Color(0xFFE53935))
        }

        if (!forecast.isNullOrEmpty()) {
            Spacer(Modifier.height(DesignTokens.SpacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(Color(0xFF9C27B0))
                )
                Spacer(Modifier.width(DesignTokens.SpacingXs))
                Text("预测", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmotionArcChart(
    dailyEmotions: List<DailyEmotion>,
    forecast: List<ForecastPoint>?,
    modifier: Modifier = Modifier
) {
    val positiveColor = Color(0xFF4CAF50)
    val neutralColor = Color(0xFFFFC107)
    val negativeColor = Color(0xFFE53935)
    val forecastColor = Color(0xFF9C27B0)

    Canvas(modifier = modifier) {
        val totalPoints = dailyEmotions.size + (forecast?.size ?: 0)
        if (totalPoints == 0) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 24f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(padding, padding + chartHeight / 2),
            end = Offset(width - padding, padding + chartHeight / 2),
            strokeWidth = 1f
        )

        fun xPos(index: Int) = padding + (index.toFloat() / (totalPoints - 1).coerceAtLeast(1)) * chartWidth
        fun yPos(emotion: Float) = padding + (1f - emotion.coerceIn(0f, 1f)) * chartHeight

        val realPath = Path()
        dailyEmotions.forEachIndexed { index, de ->
            val x = xPos(index)
            val y = yPos(de.emotion)
            if (index == 0) realPath.moveTo(x, y) else realPath.lineTo(x, y)
        }
        drawPath(
            path = realPath,
            color = neutralColor,
            style = Stroke(width = 3f)
        )

        dailyEmotions.forEachIndexed { index, de ->
            val x = xPos(index)
            val y = yPos(de.emotion)
            val color = when {
                de.emotion > 0.6f -> positiveColor
                de.emotion < 0.4f -> negativeColor
                else -> neutralColor
            }
            drawCircle(color = color, radius = 5f, center = Offset(x, y))
        }

        if (!forecast.isNullOrEmpty()) {
            val forecastPath = Path()
            forecast.forEachIndexed { index, fp ->
                val x = xPos(dailyEmotions.size + index)
                val y = yPos(fp.emotion)
                if (index == 0) forecastPath.moveTo(x, y) else forecastPath.lineTo(x, y)
            }
            drawPath(
                path = forecastPath,
                color = forecastColor,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )

            forecast.forEachIndexed { index, fp ->
                val x = xPos(dailyEmotions.size + index)
                val y = yPos(fp.emotion)
                drawCircle(
                    color = forecastColor.copy(alpha = 0.5f),
                    radius = 4f,
                    center = Offset(x, y)
                )
                val confColor = when {
                    fp.confidence >= 0.7f -> positiveColor
                    fp.confidence >= 0.4f -> neutralColor
                    else -> negativeColor
                }
                drawCircle(
                    color = confColor.copy(alpha = 0.6f),
                    radius = 2f,
                    center = Offset(x, y - 10f)
                )
            }
        }
    }
}

@Composable
private fun AiAnalysisSection(
    isAnalyzing: Boolean,
    analysis: EmotionAnalysis?,
    isAiEnabled: Boolean,
    comparisonText: String?,
    onCompare: () -> Unit
) {
    if (isAnalyzing) {
        AnalysisSkeleton()
        return
    }

    val a = analysis ?: return

    if (a.patterns.isNotEmpty()) {
        Spacer(Modifier.height(DesignTokens.SpacingLg))
        Text("情绪模式", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(DesignTokens.SpacingSm))

        a.patterns.forEach { pattern ->
            PatternCard(pattern)
            Spacer(Modifier.height(DesignTokens.SpacingSm))
        }
    }

    if (a.triggers.isNotEmpty()) {
        Spacer(Modifier.height(DesignTokens.SpacingLg))
        Text("情绪触发分析", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(DesignTokens.SpacingSm))

        a.triggers.forEach { trigger ->
            TriggerCard(trigger)
            Spacer(Modifier.height(DesignTokens.SpacingSm))
        }
    }

    if (!a.narrativeSummary.isNullOrBlank()) {
        Spacer(Modifier.height(DesignTokens.SpacingLg))
        Text("叙事总结", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(DesignTokens.SpacingSm))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = DesignTokens.CornerLarge,
            innerPadding = DesignTokens.SpacingLg
        ) {
            Text(
                text = a.narrativeSummary,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (comparisonText != null) {
        Spacer(Modifier.height(DesignTokens.SpacingLg))
        Text("时期对比", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(DesignTokens.SpacingSm))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = DesignTokens.CornerLarge,
            innerPadding = DesignTokens.SpacingLg
        ) {
            Text(
                text = comparisonText,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (isAiEnabled) {
        Spacer(Modifier.height(DesignTokens.SpacingLg))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(DesignTokens.CornerMedium))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCompare
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "对比上个月",
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun PatternCard(pattern: EmotionPattern) {
    val typeColor = when (pattern.type) {
        "weekly" -> Color(0xFF4CAF50)
        "weather" -> Color(0xFF2196F3)
        "social" -> Color(0xFFFF9800)
        "seasonal" -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(typeColor)
            )
            Spacer(Modifier.width(DesignTokens.SpacingSm))
            Text(
                text = when (pattern.type) {
                    "weekly" -> "周期性"
                    "weather" -> "天气影响"
                    "social" -> "社交影响"
                    "seasonal" -> "季节性"
                    else -> "模式"
                },
                fontSize = DesignTokens.FontSmall,
                color = typeColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(pattern.confidence * 100).toInt()}%",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(DesignTokens.SpacingSm))
        Text(
            text = pattern.description,
            fontSize = DesignTokens.FontBody
        )
    }
}

@Composable
private fun TriggerCard(trigger: EmotionTrigger) {
    val impactColor = if (trigger.impact >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = trigger.keyword,
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(DesignTokens.SpacingSm))
            Text(
                text = "${if (trigger.impact >= 0) "+" else ""}${"%.1f".format(trigger.impact)}",
                fontSize = DesignTokens.FontMedium,
                color = impactColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "出现${trigger.frequency}次",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        trigger.examples.take(2).forEach { (_, snippet) ->
            Spacer(Modifier.height(DesignTokens.SpacingXs))
            Text(
                text = "\"$snippet\"",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AnalysisSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column {
        Text("情绪模式", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(DesignTokens.SpacingSm))
        repeat(3) {
            SkeletonBlock(alpha, 72.dp)
            Spacer(Modifier.height(DesignTokens.SpacingSm))
        }
        Spacer(Modifier.height(DesignTokens.SpacingLg))
        Text("叙事总结", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(DesignTokens.SpacingSm))
        SkeletonBlock(alpha, 80.dp)
    }
}

@Composable
private fun SkeletonBlock(alpha: Float, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
private fun LoadingSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    SkeletonBlock(alpha, 200.dp)
    Spacer(Modifier.height(DesignTokens.SpacingLg))
    repeat(2) { SkeletonBlock(alpha, 100.dp); Spacer(Modifier.height(DesignTokens.SpacingSm)) }
}

@Composable
private fun ErrorDisplay(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.SpacingXxl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = DesignTokens.FontBody,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(DesignTokens.SpacingXs))
        Text(
            text = label,
            fontSize = DesignTokens.FontSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
