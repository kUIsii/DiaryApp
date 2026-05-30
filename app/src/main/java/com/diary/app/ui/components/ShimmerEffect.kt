package com.diary.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { ShimmerPlaceholder() }
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f)
    )

    Box(modifier = modifier) {
        content()
    }
}

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmerPlaceholder")
    val translateX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPlaceholderTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush)
    )
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerPlaceholder(height = 20.dp, cornerRadius = 4.dp)
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerPlaceholder(height = 14.dp, cornerRadius = 4.dp)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerPlaceholder(
                height = 14.dp,
                cornerRadius = 4.dp,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }
}
