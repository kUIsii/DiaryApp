package com.diary.app.ui.emotionradar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionRadarScreen(
    onNavigateBack: () -> Unit,
    diaryId: Long? = null,
    viewModel: EmotionRadarViewModel = viewModel()
) {
    val radars by viewModel.radars.collectAsState()
    val vitality by viewModel.vitality.collectAsState()
    val calmness by viewModel.calmness.collectAsState()
    val happiness by viewModel.happiness.collectAsState()
    val gratitude by viewModel.gratitude.collectAsState()
    val socialConnection by viewModel.socialConnection.collectAsState()
    
    LaunchedEffect(diaryId) {
        diaryId?.let { viewModel.loadRadarForDiary(it) }
    }
    
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
                    text = "情绪雷达",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (diaryId != null) {
                    TextButton(onClick = {
                        viewModel.saveRadar(diaryId)
                        onNavigateBack()
                    }) {
                        Text("保存")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 雷达图
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (diaryId != null) "当前情绪" else "近期情绪概览",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RadarChart(
                        modifier = Modifier.size(220.dp),
                        vitality = vitality,
                        calmness = calmness,
                        happiness = happiness,
                        gratitude = gratitude,
                        socialConnection = socialConnection
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 图例
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RadarLegendItem("活力", Color(0xFF4CAF50))
                        RadarLegendItem("平静", Color(0xFF2196F3))
                        RadarLegendItem("快乐", Color(0xFFFFC107))
                        RadarLegendItem("感恩", Color(0xFFFF9800))
                        RadarLegendItem("社交", Color(0xFF9C27B0))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 五维滑杆
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text("调整情绪维度", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    DimensionSlider("活力", vitality, Color(0xFF4CAF50), viewModel::setVitality)
                    DimensionSlider("平静", calmness, Color(0xFF2196F3), viewModel::setCalmness)
                    DimensionSlider("快乐", happiness, Color(0xFFFFC107), viewModel::setHappiness)
                    DimensionSlider("感恩", gratitude, Color(0xFFFF9800), viewModel::setGratitude)
                    DimensionSlider("社交", socialConnection, Color(0xFF9C27B0), viewModel::setSocialConnection)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 近期趋势
            if (radars.size >= 2) {
                val avg = viewModel.getRecentAverage(7)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text("近7天平均", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        avg.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(value * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(32.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        Text("${(value * 100).toInt()}%", fontSize = 11.sp, modifier = Modifier.width(36.dp))
    }
}

@Composable
private fun RadarChart(
    modifier: Modifier = Modifier,
    vitality: Float,
    calmness: Float,
    happiness: Float,
    gratitude: Float,
    socialConnection: Float
) {
    val values = listOf(vitality, calmness, happiness, gratitude, socialConnection)
    val colors = listOf(
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFFFFC107),
        Color(0xFFFF9800),
        Color(0xFF9C27B0)
    )
    val labels = listOf("活力", "平静", "快乐", "感恩", "社交")
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.75f
        
        // Draw grid (5 levels)
        for (level in 1..5) {
            val r = radius * level / 5
            val path = Path()
            for (i in 0 until 5) {
                val angle = (i * 72 - 90) * Math.PI / 180
                val x = centerX + r * cos(angle).toFloat()
                val y = centerY + r * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Color.Gray.copy(alpha = 0.15f), style = Stroke(width = 1f))
        }
        
        // Draw axes
        for (i in 0 until 5) {
            val angle = (i * 72 - 90) * Math.PI / 180
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            drawLine(Color.Gray.copy(alpha = 0.2f), Offset(centerX, centerY), Offset(x, y), strokeWidth = 1f)
        }
        
        // Draw data area
        val dataPath = Path()
        for (i in values.indices) {
            val angle = (i * 72 - 90) * Math.PI / 180
            val r = radius * values[i]
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, Color(0xFF2196F3).copy(alpha = 0.15f))
        drawPath(dataPath, Color(0xFF2196F3).copy(alpha = 0.6f), style = Stroke(width = 2f))
        
        // Draw data points
        for (i in values.indices) {
            val angle = (i * 72 - 90) * Math.PI / 180
            val r = radius * values[i]
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            drawCircle(colors[i], 5f, Offset(x, y))
        }
    }
}

@Composable
private fun RadarLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
