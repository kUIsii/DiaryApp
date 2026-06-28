package com.diary.app.ui.memoryart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MemoryArtScreen(
    diaryId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: MemoryArtViewModel = viewModel()
) {
    val artConfig by viewModel.artConfig.collectAsState()
    
    LaunchedEffect(diaryId) {
        diaryId?.let { viewModel.generateArt(it) }
    }
    
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                    text = "记忆艺术",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { diaryId?.let { viewModel.generateArt(it) } }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重新生成")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (artConfig == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val config = artConfig!!
                
                // Art canvas
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(16.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            
                            config.shapes.forEach { shape ->
                                val x = shape.x * width
                                val y = shape.y * height
                                val size = shape.size * width
                                val color = Color(shape.color).copy(alpha = shape.alpha)
                                
                                when (shape.type) {
                                    ShapeType.CIRCLE -> {
                                        drawCircle(
                                            color = color,
                                            radius = size / 2,
                                            center = Offset(x, y)
                                        )
                                    }
                                    ShapeType.SQUARE -> {
                                        rotate(shape.rotation, pivot = Offset(x, y)) {
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(x - size / 2, y - size / 2),
                                                size = androidx.compose.ui.geometry.Size(size, size)
                                            )
                                        }
                                    }
                                    ShapeType.TRIANGLE -> {
                                        val path = Path().apply {
                                            moveTo(x, y - size / 2)
                                            lineTo(x - size / 2, y + size / 2)
                                            lineTo(x + size / 2, y + size / 2)
                                            close()
                                        }
                                        rotate(shape.rotation, pivot = Offset(x, y)) {
                                            drawPath(path, color)
                                        }
                                    }
                                    ShapeType.LINE -> {
                                        val endX = x + cos(Math.toRadians(shape.rotation.toDouble())).toFloat() * size
                                        val endY = y + sin(Math.toRadians(shape.rotation.toDouble())).toFloat() * size
                                        drawLine(
                                            color = color,
                                            start = Offset(x, y),
                                            end = Offset(endX, endY),
                                            strokeWidth = 3f
                                        )
                                    }
                                    ShapeType.ARC -> {
                                        drawArc(
                                            color = color,
                                            startAngle = shape.rotation,
                                            sweepAngle = 180f,
                                            useCenter = false,
                                            topLeft = Offset(x - size / 2, y - size / 2),
                                            size = androidx.compose.ui.geometry.Size(size, size),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Color palette
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "色彩方案",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            config.colorPalette.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Color(color),
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "关于记忆艺术",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "每篇日记都会生成独特的抽象艺术。颜色基于心情选择，形状基于文字内容生成。这是属于你的视觉记忆。",
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
