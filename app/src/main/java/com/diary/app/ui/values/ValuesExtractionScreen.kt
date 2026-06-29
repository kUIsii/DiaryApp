package com.diary.app.ui.values

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.data.ExtractedValue
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValuesExtractionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (Long) -> Unit = {},
    viewModel: ValuesViewModel = viewModel()
) {
    val values by viewModel.values.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val lastAnalysisTime by viewModel.lastAnalysisTime.collectAsState()
    val trends by viewModel.trends.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()
    val radarMode by viewModel.radarMode.collectAsState()
    val evidenceMap by viewModel.evidenceMap.collectAsState()

    var expandedValue by remember { mutableStateOf<String?>(null) }
    var evidenceSheetValue by remember { mutableStateOf<String?>(null) }

    if (evidenceSheetValue != null) {
        EvidenceBottomSheet(
            category = evidenceSheetValue!!,
            evidenceMap = evidenceMap,
            viewModel = viewModel,
            onDismiss = { evidenceSheetValue = null },
            onNavigateToEntry = onNavigateToEntry
        )
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "价值观", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "AI从你的日记中提取的价值观",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text(
                        text = "我们嘴上说在乎的东西和实际写下来的东西往往不一样。日记是诚实的。",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastAnalysisTime > 0) {
                    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
                    Text(
                        text = "上次分析: ${fmt.format(Date(lastAnalysisTime))}",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier)
                }
                TextButton(
                    onClick = { viewModel.triggerAiAnalysis() },
                    enabled = !aiLoading
                ) {
                    if (aiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(DesignTokens.IconSmall),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("AI 重新分析", fontSize = DesignTokens.FontSmall)
                }
            }

            if (aiLoading) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                ShimmerSection()
            }

            if (values.isNotEmpty()) {
                RadarChart(
                    values = values,
                    radarMode = radarMode,
                    onToggleMode = { viewModel.toggleRadarMode() },
                    trends = trends
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            }

            if (conflicts.isNotEmpty()) {
                Text(
                    text = "价值观矛盾",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = DesignTokens.SpacingSm)
                )
                conflicts.forEach { conflict ->
                    ConflictCard(conflict)
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            }

            Text(
                text = "你最在乎的 (${values.size})",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = DesignTokens.SpacingSm)
            )

            if (values.isEmpty() && !aiLoading) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "还没有提取到价值观，多写几篇日记后系统会自动分析。",
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!aiLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    items(values) { value ->
                        val isExpanded = expandedValue == value.category
                        val description = when (value.category) {
                            "家庭" -> "你花最多时间记录与家人的相处"
                            "成长" -> "你频繁提到学习和自我提升"
                            "健康" -> "运动和饮食是你日记的常客"
                            "友情" -> "你珍惜与朋友的每一次聚会"
                            "事业" -> "工作成就和职业发展是你关注的重点"
                            "兴趣" -> "你享受爱好带来的乐趣和放松"
                            else -> value.evidence.take(50)
                        }
                        ValueItem(
                            value = value,
                            description = description,
                            isExpanded = isExpanded,
                            trendPoints = trends[value.category] ?: emptyList(),
                            onToggle = { expandedValue = if (isExpanded) null else value.category },
                            onEvidence = { evidenceSheetValue = value.category }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(conflict: ValueConflict) {
    GlassCard(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = DesignTokens.SpacingSm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = conflict.left,
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "⇄",
                fontSize = DesignTokens.FontMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = conflict.right,
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Text(
            text = conflict.reason,
            fontSize = DesignTokens.FontSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CanvasTrendChart(points: List<TrendPoint>) {
    if (points.size < 2) return
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
        .padding(vertical = 8.dp)) {
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val maxScore = points.maxOf { it.score }.coerceAtLeast(0.1f)
        val path = Path()
        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val y = size.height - (pt.score / maxScore * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val y = size.height - (pt.score / maxScore * size.height)
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun RadarChart(
    values: List<ExtractedValue>,
    radarMode: String,
    onToggleMode: () -> Unit,
    trends: Map<String, List<TrendPoint>>
) {
    val topValues = values.sortedByDescending { it.confidence }.take(6)
    if (topValues.size < 3) return

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val lineColor = MaterialTheme.colorScheme.primary
    val monthlyColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onToggleMode) {
                Text(
                    if (radarMode == "current") "月度对比" else "当前",
                    fontSize = DesignTokens.FontSmall
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) - 40.dp.toPx()
            val n = topValues.size
            val angleStep = (2 * Math.PI / n).toFloat()
            val paint = android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }

            for (level in 1..4) {
                val r = radius * level / 4
                val path = Path()
                topValues.forEachIndexed { i, _ ->
                    val angle = -Math.PI / 2 + i * angleStep
                    val x = centerX + r * kotlin.math.cos(angle).toFloat()
                    val y = centerY + r * kotlin.math.sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = gridColor, style = Stroke(1.dp.toPx()))
            }

            topValues.forEachIndexed { i, _ ->
                val angle = -Math.PI / 2 + i * angleStep
                val x = centerX + radius * kotlin.math.cos(angle).toFloat()
                val y = centerY + radius * kotlin.math.sin(angle).toFloat()
                drawLine(gridColor, Offset(centerX, centerY), Offset(x, y), strokeWidth = 1.dp.toPx())

                val labelX = centerX + (radius + 20.dp.toPx()) * kotlin.math.cos(angle).toFloat()
                val labelY = centerY + (radius + 20.dp.toPx()) * kotlin.math.sin(angle).toFloat()
                drawContext.canvas.nativeCanvas.drawText(
                    topValues[i].category.take(2),
                    labelX,
                    labelY + 5.dp.toPx(),
                    paint
                )
            }

            val currentPath = Path()
            topValues.forEachIndexed { i, v ->
                val angle = -Math.PI / 2 + i * angleStep
                val r = radius * v.confidence
                val x = centerX + r * kotlin.math.cos(angle).toFloat()
                val y = centerY + r * kotlin.math.sin(angle).toFloat()
                if (i == 0) currentPath.moveTo(x, y) else currentPath.lineTo(x, y)
            }
            currentPath.close()
            drawPath(currentPath, color = lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            topValues.forEachIndexed { i, v ->
                val angle = -Math.PI / 2 + i * angleStep
                val r = radius * v.confidence
                val x = centerX + r * kotlin.math.cos(angle).toFloat()
                val y = centerY + r * kotlin.math.sin(angle).toFloat()
                drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
            }

            if (radarMode == "monthly") {
                val monthlyPath = Path()
                topValues.forEachIndexed { i, v ->
                    val trendPts = trends[v.category]
                    val avgScore = trendPts?.takeLast(30)?.map { it.score }?.average()?.toFloat() ?: v.confidence
                    val angle = -Math.PI / 2 + i * angleStep
                    val r = radius * avgScore.coerceIn(0f, 1f)
                    val x = centerX + r * kotlin.math.cos(angle).toFloat()
                    val y = centerY + r * kotlin.math.sin(angle).toFloat()
                    if (i == 0) monthlyPath.moveTo(x, y) else monthlyPath.lineTo(x, y)
                }
                monthlyPath.close()
                drawPath(monthlyPath, color = monthlyColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun ValueItem(
    value: ExtractedValue,
    description: String,
    isExpanded: Boolean,
    trendPoints: List<TrendPoint>,
    onToggle: () -> Unit,
    onEvidence: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = value.value, fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Medium)
                        if (trendPoints.size >= 2) {
                            val last = trendPoints.last().score
                            val prev = trendPoints[trendPoints.size - 2].score
                            val arrow = if (last >= prev) "↑" else "↓"
                            val color = if (last >= prev) Color(0xFF4CAF50) else Color(0xFFE53935)
                            Text(
                                text = "$arrow ${(last * 100).toInt()}%",
                                fontSize = DesignTokens.FontSmall,
                                color = color
                            )
                        }
                    }
                }
                Text(
                    text = "${(value.confidence * 100).toInt()}%",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 26.dp)
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Divider()
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                if (trendPoints.size >= 2) {
                    Text(
                        text = "变化趋势",
                        fontSize = DesignTokens.FontSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    CanvasTrendChart(points = trendPoints)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Divider()
                }
                TextButton(
                    onClick = onEvidence,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text("查看证据日记", fontSize = DesignTokens.FontSmall)
                }
            }
        }
    }
}

@Composable
private fun ShimmerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)) {
        repeat(3) {
            ShimmerCard()
        }
    }
}

@Composable
private fun ShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmerValues")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EvidenceBottomSheet(
    category: String,
    evidenceMap: Map<String, List<Long>>,
    viewModel: ValuesViewModel,
    onDismiss: () -> Unit,
    onNavigateToEntry: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val dao = (context.applicationContext as com.diary.app.DiaryApplication).database.diaryDao()
    val diaryIds = evidenceMap[category] ?: emptyList()
    var diaryPreviews by remember { mutableStateOf<List<DiaryPreview>>(emptyList()) }

    LaunchedEffect(diaryIds) {
        diaryPreviews = dao.getPreviewsByIds(diaryIds)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(DesignTokens.SpacingLg)) {
            Text(
                text = "「$category」相关日记",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            if (diaryPreviews.isEmpty()) {
                Text(
                    text = "未找到相关日记",
                    fontSize = DesignTokens.FontBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                diaryPreviews.forEach { preview ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = DesignTokens.SpacingSm),
                        onClick = { onNavigateToEntry(preview.id) }
                    ) {
                        Column {
                            Text(
                                text = preview.title.ifBlank { "无标题" },
                                fontSize = DesignTokens.FontBody,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preview.plainText.take(100),
                                fontSize = DesignTokens.FontSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                            Text(
                                text = fmt.format(Date(preview.createdAt)),
                                fontSize = DesignTokens.FontCaption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
