package com.diary.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        ThemeMode.WARM_ROSE -> modifier
            .fillMaxSize()
            .background(if (dark) com.diary.app.ui.theme.WarmRoseDarkBackground else com.diary.app.ui.theme.WarmRoseBackground)
        ThemeMode.OCEAN_BLUE -> modifier
            .fillMaxSize()
            .background(if (dark) com.diary.app.ui.theme.OceanBlueDarkBackground else com.diary.app.ui.theme.OceanBlueBackground)
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
