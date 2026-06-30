package com.diary.app.ui.writingfingerprint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingFingerprintScreen(
    onNavigateBack: () -> Unit,
    viewModel: WritingFingerprintViewModel = viewModel()
) {
    val analysis by viewModel.analysis.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "\u5199\u4F5C\u6307\u7EB9", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)
            ) {
                TimeRange.entries.forEach { range ->
                    val selected = selectedRange == range
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.setTimeRange(range) }
                            .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range.label,
                            fontSize = DesignTokens.FontBody,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (analysis == null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Text(
                            "\u6B63\u5728\u5206\u6790\u5199\u4F5C\u98CE\u683C...",
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val data = analysis!!

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    if (data.dimensions.isNotEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "\u516D\u7EF4\u5199\u4F5C\u5206\u6790",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                RadarChart(
                                    dimensions = data.dimensions,
                                    previousDimensions = data.previousDimensions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                data.previousDimensions?.let { prev ->
                                    Text(
                                        text = "\u4E0E\u4E0A\u671F\u5BF9\u6BD4",
                                        fontSize = DesignTokens.FontSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    if (availableTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "\u5BF9\u6BD4\u5206\u6790",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                Text(
                                    text = "\u9009\u62E9\u6807\u7B7E\uFF0C\u5BF9\u6BD4\u8BE5\u6807\u7B7E\u65E5\u8BB0\u4E0E\u5176\u4ED6\u65E5\u8BB0\u7684\u98CE\u683C\u5DEE\u5F02",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                                            .background(
                                                if (selectedTagId == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                            .clickable { viewModel.setComparativeTag(null) }
                                            .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm)
                                    ) {
                                        Text(
                                            text = "\u5168\u90E8",
                                            fontSize = 14.sp,
                                            fontWeight = if (selectedTagId == null) FontWeight.Medium else FontWeight.Normal,
                                            color = if (selectedTagId == null) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    availableTags.take(6).forEach { tag ->
                                        val tagSelected = selectedTagId == tag.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                                                .background(
                                                    if (tagSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else Color.Transparent
                                                )
                                                .clickable { viewModel.setComparativeTag(tag.id) }
                                                .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm)
                                        ) {
                                            Text(
                                                text = tag.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (tagSelected) FontWeight.Medium else FontWeight.Normal,
                                                color = if (tagSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (data.comparativeInsight != null) {
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                    Text(
                                        text = data.comparativeInsight,
                                        fontSize = DesignTokens.FontSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "\u5199\u4F5C\u4EBA\u683C",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            if (aiLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                                    Text(
                                        text = "\u6B63\u5728\u5206\u6790...",
                                        fontSize = DesignTokens.FontSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = data.persona,
                                    fontSize = DesignTokens.FontBody,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = DesignTokens.FontBody.value.sp * 1.6f
                                )
                                if (data.aiEnabled) {
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                    Text(
                                        text = "\u6765\u81EA AI \u6DF1\u5EA6\u5206\u6790",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (aiError != null) {
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                Text(
                                    text = aiError!!,
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (data.healthScore != null) {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "\u5199\u4F5C\u5065\u5EB7\u5EA6",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                HealthScoreGauge(score = data.healthScore)
                            }
                        }
                    }

                    if (data.stylePeriods.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "\u98CE\u683C\u6F14\u53D8",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                StyleTimeline(
                                    periods = data.stylePeriods,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                data.stylePeriods.forEachIndexed { index, period ->
                                    StylePeriodCard(
                                        index = index,
                                        period = period,
                                        isLast = index == data.stylePeriods.lastIndex
                                    )
                                }
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
private fun RadarChart(
    dimensions: Map<String, Float>,
    previousDimensions: Map<String, Float>? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    val keys = listOf(
        "\u8BCD\u6C47\u4E30\u5BCC\u5EA6",
        "\u53E5\u5F0F\u590D\u6742\u5EA6",
        "\u60C5\u611F\u8868\u8FBE",
        "\u65F6\u95F4\u89C6\u89D2",
        "\u4E3B\u9898\u504F\u597D",
        "\u4FEE\u8F9E\u4F7F\u7528"
    )

    val gridColor = surfaceVariant.copy(alpha = 0.4f)
    val axisColor = surfaceVariant.copy(alpha = 0.3f)
    val dataFillColor = primaryColor.copy(alpha = 0.1f)
    val dataStrokeColor = primaryColor.copy(alpha = 0.3f)
    val dotColor = primaryColor
    val prevStrokeColor = secondaryColor.copy(alpha = 0.3f)
    val onSurfaceArgb = onSurface.toArgbInt()

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = min(size.width, size.height) / 2 * 0.65f
        val sides = 6
        val angleStep = Math.PI * 2 / sides
        val offset = -Math.PI / 2

        val labelRadius = radius * 1.2f

        val gridLevels = 5
        for (level in 1..gridLevels) {
            val r = radius * level / gridLevels
            val gridPath = Path()
            for (i in 0 until sides) {
                val angle = offset + i * angleStep
                val x = centerX + r * cos(angle).toFloat()
                val y = centerY + r * sin(angle).toFloat()
                if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
            }
            gridPath.close()
            drawPath(
                path = gridPath,
                color = surfaceVariant.copy(alpha = 0.4f),
                style = Stroke(width = 1f)
            )
        }

        for (i in 0 until sides) {
            val angle = offset + i * angleStep
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            drawLine(
                color = surfaceVariant.copy(alpha = 0.3f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1f
            )
        }

        val dataPath = Path()
        keys.forEachIndexed { i, key ->
            val value = (dimensions[key] ?: 0.5f).coerceIn(0f, 1f)
            val angle = offset + i * angleStep
            val r = radius * value
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(
            path = dataPath,
            color = primaryColor.copy(alpha = 0.3f),
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = dataPath,
            color = primaryColor.copy(alpha = 0.1f)
        )

        keys.forEachIndexed { i, key ->
            val value = (dimensions[key] ?: 0.5f).coerceIn(0f, 1f)
            val angle = offset + i * angleStep
            val r = radius * value
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            drawCircle(primaryColor, radius = 4f, center = Offset(x, y))
        }

        if (previousDimensions != null) {
            val prevPath = Path()
            keys.forEachIndexed { i, key ->
                val value = (previousDimensions[key] ?: 0.5f).coerceIn(0f, 1f)
                val angle = offset + i * angleStep
                val r = radius * value
                val x = centerX + r * cos(angle).toFloat()
                val y = centerY + r * sin(angle).toFloat()
                if (i == 0) prevPath.moveTo(x, y) else prevPath.lineTo(x, y)
            }
            prevPath.close()
            drawPath(
                path = prevPath,
                color = secondaryColor.copy(alpha = 0.3f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 4f)))
            )
        }

        keys.forEachIndexed { i, key ->
            val angle = offset + i * angleStep
            val lx = centerX + labelRadius * cos(angle).toFloat()
            val ly = centerY + labelRadius * sin(angle).toFloat()
            val value = (dimensions[key] ?: 0.5f).coerceIn(0f, 1f)
            val labelText = "$key\n${"%.0f".format(value * 100)}%"

            val paint = android.graphics.Paint().apply {
                color = onSurfaceArgb
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            val lines = labelText.split("\n")
            var lineY = ly - (lines.size - 1) * 12f
            for (line in lines) {
                drawContext.canvas.nativeCanvas.drawText(line, lx, lineY, paint)
                lineY += 26f
            }

            if (previousDimensions != null) {
                val prevVal = (previousDimensions[key] ?: 0.5f).coerceIn(0f, 1f)
                val diff = value - prevVal
                if (kotlin.math.abs(diff) > 0.02f) {
                    val arrowColor = if (diff > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val arrowPaint = android.graphics.Paint().apply {
                        setColor(arrowColor.toArgbInt())
                        textSize = 18f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val arrow = if (diff > 0) "\u2191" else "\u2193"
                    drawContext.canvas.nativeCanvas.drawText(
                        arrow, lx, ly + 16f + 26f, arrowPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthScoreGauge(score: WritingHealthScore) {
    val scoreColor = when {
        score.score >= 80 -> Color(0xFF4CAF50)
        score.score >= 60 -> Color(0xFF8BC34A)
        score.score >= 40 -> Color(0xFFFFC107)
        score.score >= 20 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweep = (score.score / 100f) * 270f
                    drawArc(
                        color = surfaceVariantColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${score.score}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "/100",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.width(160.dp)) {
                ScoreBar("\u4E00\u81F4\u6027", score.consistency, Color(0xFF64B5F6))
                Spacer(modifier = Modifier.height(6.dp))
                ScoreBar("\u591A\u6837\u6027", score.diversity, Color(0xFF81C784))
                Spacer(modifier = Modifier.height(6.dp))
                ScoreBar("\u60C5\u611F\u6DF1\u5EA6", score.emotionalDepth, Color(0xFFFFB74D))
                Spacer(modifier = Modifier.height(6.dp))
                ScoreBar("\u81EA\u6211\u53CD\u601D", score.selfReflection, Color(0xFFCE93D8))
            }
        }

        if (score.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                score.tips.forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                        Text(
                            text = tip,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${"%.0f".format(value * 100)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun StyleTimeline(
    periods: List<StylePeriod>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF64B5F6),
        Color(0xFF81C784),
        Color(0xFFFFB74D),
        Color(0xFFCE93D8),
        Color(0xFF4DB6AC)
    )
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    if (periods.isEmpty()) return

    val totalRange = periods.last().endDate - periods.first().startDate
    if (totalRange <= 0) return

    Canvas(modifier = modifier) {
        val barY = size.height / 2
        val barHeight = 12f
        val startX = 0f
        val endX = size.width

        drawLine(
            color = surfaceVariantColor.copy(alpha = 0.3f),
            start = Offset(startX, barY + barHeight / 2),
            end = Offset(endX, barY + barHeight / 2),
            strokeWidth = 2f
        )

        periods.forEachIndexed { i, period ->
            val segmentStart = startX + ((period.startDate - periods.first().startDate).toFloat() / totalRange) * (endX - startX)
            val segmentEnd = startX + ((period.endDate - periods.first().startDate).toFloat() / totalRange) * (endX - startX)
            val width = (segmentEnd - segmentStart).coerceAtLeast(4f)

            val segmentColor = colors[i % colors.size]

            drawRoundRect(
                color = segmentColor.copy(alpha = 0.8f),
                topLeft = Offset(segmentStart, barY),
                size = Size(width, barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            val labelY = barY + barHeight + 16f
                            val paint = android.graphics.Paint().apply {
                                setColor(onSurfaceColor.toArgbInt())
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }

            val label = period.label.take(4)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                segmentStart + width / 2,
                labelY + 14f,
                paint
            )
        }
    }
}

@Composable
private fun StylePeriodCard(
    index: Int,
    period: StylePeriod,
    isLast: Boolean
) {
    val colors = listOf(
        Color(0xFF64B5F6),
        Color(0xFF81C784),
        Color(0xFFFFB74D),
        Color(0xFFCE93D8),
        Color(0xFF4DB6AC)
    )
    val color = colors[index % colors.size]

    val dateFormat = remember { SimpleDateFormat("yyyy\u5E74M\u6708d\u65E5", Locale.CHINESE) }

    Row(modifier = Modifier.padding(vertical = DesignTokens.SpacingXs)) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.7f))
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = period.label,
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${dateFormat.format(Date(period.startDate))} - ${dateFormat.format(Date(period.endDate))}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = period.characteristics.joinToString(" | "),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
    if (!isLast) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
    }
}

private fun Color.toArgbInt(): Int {
    return ((alpha * 255).toInt() and 0xFF shl 24) or
           ((red * 255).toInt() and 0xFF shl 16) or
           ((green * 255).toInt() and 0xFF shl 8) or
           ((blue * 255).toInt() and 0xFF)
}
