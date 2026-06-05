package com.diary.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.diary.app.ui.theme.DarkBackgroundEnd
import com.diary.app.ui.theme.DarkBackgroundMid
import com.diary.app.ui.theme.DarkBackgroundStart
import com.diary.app.ui.theme.LightBackgroundEnd
import com.diary.app.ui.theme.LightBackgroundMid
import com.diary.app.ui.theme.LightBackgroundStart
import com.diary.app.ui.theme.MossGreenDarkBackgroundEnd
import com.diary.app.ui.theme.MossGreenDarkBackgroundMid
import com.diary.app.ui.theme.MossGreenDarkBackgroundStart
import com.diary.app.ui.theme.MossGreenLightBackgroundEnd
import com.diary.app.ui.theme.MossGreenLightBackgroundMid
import com.diary.app.ui.theme.MossGreenLightBackgroundStart
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.themeMode

/**
 * A subtle dot-grid overlay that evokes notebook paper texture.
 */
@Composable
fun DotGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacing = 22.dp.toPx()
        val dotRadius = 1.2f.dp.toPx()
        val dotColor = Color.Black.copy(alpha = 0.04f)
        val colCount = (size.width / spacing).toInt() + 1
        val rowCount = (size.height / spacing).toInt() + 1
        for (col in 0 until colCount) {
            for (row in 0 until rowCount) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(col * spacing, row * spacing)
                )
            }
        }
    }
}

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val mode = themeMode()
    val showGrid = mode.category == "green"

    val (start, mid, end) = when (mode) {
        ThemeMode.PURE_LIGHT -> Triple(LightBackgroundStart, LightBackgroundMid, LightBackgroundEnd)
        ThemeMode.PURE_DARK -> Triple(DarkBackgroundStart, DarkBackgroundMid, DarkBackgroundEnd)
        ThemeMode.MOSS_GREEN_LIGHT -> Triple(MossGreenLightBackgroundStart, MossGreenLightBackgroundMid, MossGreenLightBackgroundEnd)
        ThemeMode.MOSS_GREEN_DARK -> Triple(MossGreenDarkBackgroundStart, MossGreenDarkBackgroundMid, MossGreenDarkBackgroundEnd)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(start, mid, end))
            )
    ) {
        if (showGrid) {
            DotGridOverlay()
        }
        content()
    }
}
