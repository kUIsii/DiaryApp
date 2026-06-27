package com.diary.app.ui.quietcompanion

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuietCompanionScreen(
    onNavigateBack: () -> Unit
) {
    var selectedCompanion by remember { mutableStateOf("flame") }
    
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
                    text = "安静陪伴",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 动画区域
            GlassCard(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                cornerRadius = 20.dp,
                innerPadding = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedCompanion) {
                        "flame" -> AnimatedFlame()
                        "cloud" -> AnimatedCloud()
                        "ripple" -> AnimatedRipple()
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 说明文字
            Text(
                text = "它不交互、不说话、不提醒。只是在那里，像一个安静做事的朋友。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 选择陪伴元素
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text("选择陪伴", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    CompanionOption("flame", "油灯火焰", selectedCompanion) { selectedCompanion = it }
                    CompanionOption("cloud", "飘过的云", selectedCompanion) { selectedCompanion = it }
                    CompanionOption("ripple", "水面涟漪", selectedCompanion) { selectedCompanion = it }
                }
            }
        }
    }
}

@Composable
private fun CompanionOption(key: String, name: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 14.sp)
        RadioButton(
            selected = selected == key,
            onClick = { onSelect(key) }
        )
    }
}

// 油灯火焰动画
@Composable
private fun AnimatedFlame() {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )
    val sway by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    
    Canvas(modifier = Modifier.size(120.dp)) {
        val centerX = size.width / 2 + sway
        val centerY = size.height * 0.6f
        
        // Outer glow
        drawCircle(
            Color(0xFFFF6F00).copy(alpha = 0.1f * flicker),
            radius = 50f * flicker,
            center = Offset(centerX, centerY)
        )
        
        // Middle flame
        drawCircle(
            Color(0xFFFF9800).copy(alpha = 0.3f * flicker),
            radius = 30f * flicker,
            center = Offset(centerX, centerY)
        )
        
        // Inner flame
        drawCircle(
            Color(0xFFFFEB3B).copy(alpha = 0.6f),
            radius = 18f * flicker,
            center = Offset(centerX, centerY - 5f)
        )
        
        // Core
        drawCircle(
            Color.White.copy(alpha = 0.8f),
            radius = 8f,
            center = Offset(centerX, centerY - 8f)
        )
    }
}

// 云朵飘过动画
@Composable
private fun AnimatedCloud() {
    val infiniteTransition = rememberInfiniteTransition(label = "cloud")
    val posX by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "posX"
    )
    val bob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val y = size.height / 2 + bob
        
        // Cloud shape (multiple circles)
        drawCloud(posX, y, 0.8f)
        drawCloud(posX - 150, y + 20, 0.5f)
        drawCloud(posX - 300, y - 10, 0.6f)
    }
}

private fun DrawScope.drawCloud(x: Float, y: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha * 0.6f)
    drawCircle(color, 20f, Offset(x, y))
    drawCircle(color, 25f, Offset(x + 20f, y - 5f))
    drawCircle(color, 22f, Offset(x + 40f, y))
    drawCircle(color, 18f, Offset(x + 55f, y + 3f))
}

// 水面涟漪动画
@Composable
private fun AnimatedRipple() {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    
    val r1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "r1"
    )
    val r2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseOutCubic, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "r2"
    )
    val r3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseOutCubic, delayMillis = 2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "r3"
    )
    
    Canvas(modifier = Modifier.size(160.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        
        drawRipple(center, r1)
        drawRipple(center, r2)
        drawRipple(center, r3)
        
        // Center dot
        drawCircle(
            Color(0xFF2196F3).copy(alpha = 0.6f),
            4f,
            center
        )
    }
}

private fun DrawScope.drawRipple(center: Offset, radius: Float) {
    if (radius <= 0) return
    val alpha = (1f - radius / 80f) * 0.4f
    drawCircle(
        Color(0xFF2196F3).copy(alpha = alpha),
        radius,
        center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
    )
}
