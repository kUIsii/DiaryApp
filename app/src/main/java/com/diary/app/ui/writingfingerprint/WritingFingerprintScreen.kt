package com.diary.app.ui.writingfingerprint

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingFingerprintScreen(
    onNavigateBack: () -> Unit
) {
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
                    text = "写作指纹",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "你的写作指纹",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Fingerprint visualization
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        
                        // Draw concentric circles with varying patterns
                        for (i in 1..10) {
                            val radius = size.minDimension / 2 * i / 10
                            val color = Color(0xFF2196F3).copy(alpha = 0.1f + i * 0.05f)
                            drawCircle(color, radius, Offset(centerX, centerY))
                        }
                        
                        // Draw pattern lines
                        for (i in 0 until 36) {
                            val angle = i * 10 * Math.PI / 180
                            val length = 50f + (i % 5) * 20f
                            val startX = centerX + 30f * kotlin.math.cos(angle).toFloat()
                            val startY = centerY + 30f * kotlin.math.sin(angle).toFloat()
                            val endX = centerX + (30f + length) * kotlin.math.cos(angle).toFloat()
                            val endY = centerY + (30f + length) * kotlin.math.sin(angle).toFloat()
                            drawLine(
                                Color(0xFF2196F3).copy(alpha = 0.3f),
                                Offset(startX, startY),
                                Offset(endX, endY),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "风格特征",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FingerprintStatRow("写作节奏", "稳定型")
                    FingerprintStatRow("用词偏好", "感性词汇")
                    FingerprintStatRow("句式特点", "长短交错")
                    FingerprintStatRow("情感表达", "内敛含蓄")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "风格演变",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "过去6个月，你的写作风格从「简洁直接」逐渐转向「细腻丰富」。用词丰富度提升了15%。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FingerprintStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
