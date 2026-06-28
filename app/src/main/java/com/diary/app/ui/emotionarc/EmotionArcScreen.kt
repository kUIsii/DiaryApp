package com.diary.app.ui.emotionarc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EmotionArcScreen(
    diaryId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: EmotionArcViewModel = viewModel()
) {
    val arcData by viewModel.arcData.collectAsState()
    
    LaunchedEffect(diaryId) {
        diaryId?.let { viewModel.loadDiary(it) }
    }
    
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "情绪弧线",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (arcData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val data = arcData!!
                
                // Title
                Text(
                    text = data.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Emotion arc chart
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "情绪变化曲线",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        EmotionArcChart(
                            points = data.emotionPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendItem("积极", Color(0xFF4CAF50))
                            LegendItem("中性", Color(0xFFFFC107))
                            LegendItem("消极", Color(0xFFE53935))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Emotion points list
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "情绪节点",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        data.emotionPoints.forEachIndexed { index, point ->
                            EmotionPointItem(point)
                            if (index < data.emotionPoints.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionArcChart(
    points: List<EmotionPoint>,
    modifier: Modifier = Modifier
) {
    val positiveColor = Color(0xFF4CAF50)
    val neutralColor = Color(0xFFFFC107)
    val negativeColor = Color(0xFFE53935)
    
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val padding = 20f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2
        
        // Draw background grid
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(padding, padding + chartHeight / 2),
            end = Offset(width - padding, padding + chartHeight / 2),
            strokeWidth = 1f
        )
        
        // Draw emotion curve
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = padding + (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartWidth
            val y = padding + (1f - point.emotion) * chartHeight
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = neutralColor,
            style = Stroke(width = 3f)
        )
        
        // Draw points
        points.forEachIndexed { index, point ->
            val x = padding + (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartWidth
            val y = padding + (1f - point.emotion) * chartHeight
            
            val color = when {
                point.emotion > 0.6f -> positiveColor
                point.emotion < 0.4f -> negativeColor
                else -> neutralColor
            }
            
            drawCircle(
                color = color,
                radius = 6f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun EmotionPointItem(point: EmotionPoint) {
    val color = when {
        point.emotion > 0.6f -> Color(0xFF4CAF50)
        point.emotion < 0.4f -> Color(0xFFE53935)
        else -> Color(0xFFFFC107)
    }
    
    val label = when {
        point.emotion > 0.6f -> "积极"
        point.emotion < 0.4f -> "消极"
        else -> "中性"
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point.label,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                text = "位置 ${point.position + 1}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
