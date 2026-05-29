package com.diary.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.diary.app.ui.theme.DarkBackgroundEnd
import com.diary.app.ui.theme.DarkBackgroundMid
import com.diary.app.ui.theme.DarkBackgroundStart
import com.diary.app.ui.theme.LightBackgroundEnd
import com.diary.app.ui.theme.LightBackgroundMid
import com.diary.app.ui.theme.LightBackgroundStart
import com.diary.app.ui.theme.PureDarkBackground
import com.diary.app.ui.theme.PureLightBackground
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val mode = themeMode()
    val dark = mode.isDark()

    val backgroundModifier = when (mode) {
        ThemeMode.PURE_LIGHT -> modifier
            .fillMaxSize()
            .background(PureLightBackground)
        ThemeMode.PURE_DARK -> modifier
            .fillMaxSize()
            .background(PureDarkBackground)
        ThemeMode.GRADIENT, ThemeMode.SYSTEM -> {
            val colors = if (dark) {
                listOf(DarkBackgroundStart, DarkBackgroundMid, DarkBackgroundEnd)
            } else {
                listOf(LightBackgroundStart, LightBackgroundMid, LightBackgroundEnd)
            }
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = colors))
        }
    }

    Box(modifier = backgroundModifier) {
        if (mode == ThemeMode.GRADIENT || mode == ThemeMode.SYSTEM) {
            NoiseTexture(dark = dark)
        }
        content()
    }
}

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val mode = themeMode()
    val dark = mode.isDark()

    // Pure modes use solid background without animation
    if (mode == ThemeMode.PURE_LIGHT) {
        Box(modifier = modifier.fillMaxSize().background(PureLightBackground)) {
            content()
        }
        return
    }
    if (mode == ThemeMode.PURE_DARK) {
        Box(modifier = modifier.fillMaxSize().background(PureDarkBackground)) {
            content()
        }
        return
    }

    // 4 gradient color schemes for cycling
    val gradientSets = if (dark) {
        listOf(
            listOf(Color(0xFF080810), Color(0xFF0C0C1E)),
            listOf(Color(0xFF0A0A1E), Color(0xFF101030)),
            listOf(Color(0xFF08101A), Color(0xFF0C1A2C)),
            listOf(Color(0xFF100A1A), Color(0xFF1A0C2C))
        )
    } else {
        listOf(
            listOf(Color(0xFFF0F1F5), Color(0xFFEAECF3), Color(0xFFF2F0F5)),
            listOf(Color(0xFFF0F5FF), Color(0xFFE8F0FF), Color(0xFFF0F3FF)),
            listOf(Color(0xFFF0FFF5), Color(0xFFE8FFF0), Color(0xFFF0FFF3)),
            listOf(Color(0xFFFFF0F5), Color(0xFFFFE8F0), Color(0xFFFFF0F3))
        )
    }

    val infiniteTransition = rememberInfiniteTransition()
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = gradientSets.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 20000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val index = progress.toInt().coerceIn(0, gradientSets.size - 1)
    val nextIndex = (index + 1) % gradientSets.size
    val fraction = progress - progress.toInt()

    // Interpolate between current and next gradient set
    val currentGradient = gradientSets[index].zip(gradientSets[nextIndex]) { a, b ->
        lerp(a, b, fraction)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = currentGradient))
    ) {
        NoiseTexture(dark = dark)
        content()
    }
}

@Composable
private fun NoiseTexture(dark: Boolean) {
    val noiseAlpha = if (dark) 0.03f else 0.02f
    val noiseColor = if (dark) Color.White else Color.Black

    // Deterministic noise points generated once
    val noisePoints = remember {
        val random = kotlin.random.Random(42L)
        List(300) {
            Triple(random.nextFloat(), random.nextFloat(), random.nextFloat() * 0.5f + 0.5f)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        noisePoints.forEach { (rx, ry, alphaFactor) ->
            drawCircle(
                color = noiseColor.copy(alpha = noiseAlpha * alphaFactor),
                radius = 2f,
                center = Offset(rx * w, ry * h)
            )
        }
    }
}
