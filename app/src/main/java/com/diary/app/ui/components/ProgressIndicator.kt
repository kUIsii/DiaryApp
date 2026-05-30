package com.diary.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    gradientColors: List<Color> = listOf(
        com.diary.app.ui.theme.PrimaryIndigo,
        com.diary.app.ui.theme.SecondaryViolet
    ),
    backgroundColor: Color = Color.Gray.copy(alpha = 0.1f),
    animationDuration: Int = 1000,
    label: String? = null,
    labelColor: Color = Color.Unspecified
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "circularProgress"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size
            val arcSize = canvasSize.copy(
                width = canvasSize.width - strokeWidth.toPx(),
                height = canvasSize.height - strokeWidth.toPx()
            )
            val topLeft = androidx.compose.ui.geometry.Offset(
                x = strokeWidth.toPx() / 2,
                y = strokeWidth.toPx() / 2
            )

            // Background arc
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Progress arc with gradient
            drawArc(
                brush = Brush.sweepGradient(gradientColors),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (label != null) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = labelColor
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }
    }
}

@Composable
fun LinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    gradientColors: List<Color> = listOf(
        com.diary.app.ui.theme.PrimaryIndigo,
        com.diary.app.ui.theme.SecondaryViolet
    ),
    backgroundColor: Color = Color.Gray.copy(alpha = 0.1f),
    animationDuration: Int = 1000
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "linearProgress"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height

        // Background
        drawRoundRect(
            color = backgroundColor,
            size = this.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                canvasHeight / 2,
                canvasHeight / 2
            )
        )

        // Progress
        drawRoundRect(
            brush = Brush.horizontalGradient(gradientColors),
            size = this.size.copy(width = canvasWidth * animatedProgress),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                canvasHeight / 2,
                canvasHeight / 2
            )
        )
    }
}
