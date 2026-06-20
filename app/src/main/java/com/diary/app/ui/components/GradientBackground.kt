package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
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
import com.diary.app.ui.theme.ClayDarkBackgroundEnd
import com.diary.app.ui.theme.ClayDarkBackgroundMid
import com.diary.app.ui.theme.ClayDarkBackgroundStart
import com.diary.app.ui.theme.ClayLightBackgroundEnd
import com.diary.app.ui.theme.ClayLightBackgroundMid
import com.diary.app.ui.theme.ClayLightBackgroundStart
import com.diary.app.ui.theme.OceanDarkBackgroundEnd
import com.diary.app.ui.theme.OceanDarkBackgroundMid
import com.diary.app.ui.theme.OceanDarkBackgroundStart
import com.diary.app.ui.theme.OceanLightBackgroundEnd
import com.diary.app.ui.theme.OceanLightBackgroundMid
import com.diary.app.ui.theme.OceanLightBackgroundStart
import com.diary.app.ui.theme.InkDarkBackgroundEnd
import com.diary.app.ui.theme.InkDarkBackgroundMid
import com.diary.app.ui.theme.InkDarkBackgroundStart
import com.diary.app.ui.theme.InkLightBackgroundEnd
import com.diary.app.ui.theme.InkLightBackgroundMid
import com.diary.app.ui.theme.InkLightBackgroundStart
import com.diary.app.ui.theme.PetalDarkBackgroundEnd
import com.diary.app.ui.theme.PetalDarkBackgroundMid
import com.diary.app.ui.theme.PetalDarkBackgroundStart
import com.diary.app.ui.theme.PetalLightBackgroundEnd
import com.diary.app.ui.theme.PetalLightBackgroundMid
import com.diary.app.ui.theme.PetalLightBackgroundStart
import com.diary.app.ui.theme.SandDarkBackgroundEnd
import com.diary.app.ui.theme.SandDarkBackgroundMid
import com.diary.app.ui.theme.SandDarkBackgroundStart
import com.diary.app.ui.theme.SandLightBackgroundEnd
import com.diary.app.ui.theme.SandLightBackgroundMid
import com.diary.app.ui.theme.SandLightBackgroundStart
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode

/**
 * A subtle dot-grid overlay that evokes notebook paper texture.
 * Uses drawWithCache for performance — dots are pre-computed and cached.
 */
@Composable
fun DotGridOverlay(modifier: Modifier = Modifier) {
    val mode = themeMode()
    val dotColor = if (mode.isDark()) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val spacing = 22.dp.toPx()
                val dotRadius = 1.2f.dp.toPx()
                val colCount = (size.width / spacing).toInt() + 1
                val rowCount = (size.height / spacing).toInt() + 1

                // Pre-compute dot positions into a path for efficient drawing
                val path = Path().apply {
                    for (col in 0 until colCount) {
                        for (row in 0 until rowCount) {
                            addOval(
                                androidx.compose.ui.geometry.Rect(
                                    center = Offset(col * spacing, row * spacing),
                                    radius = dotRadius
                                )
                            )
                        }
                    }
                }

                onDrawBehind {
                    drawPath(path, color = dotColor)
                }
            }
    )
}

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val mode = themeMode()

    val (start, mid, end) = when (mode) {
        ThemeMode.PURE_LIGHT -> Triple(LightBackgroundStart, LightBackgroundMid, LightBackgroundEnd)
        ThemeMode.PURE_DARK -> Triple(DarkBackgroundStart, DarkBackgroundMid, DarkBackgroundEnd)
        ThemeMode.MOSS_GREEN_LIGHT -> Triple(MossGreenLightBackgroundStart, MossGreenLightBackgroundMid, MossGreenLightBackgroundEnd)
        ThemeMode.MOSS_GREEN_DARK -> Triple(MossGreenDarkBackgroundStart, MossGreenDarkBackgroundMid, MossGreenDarkBackgroundEnd)
        ThemeMode.OCEAN_LIGHT -> Triple(OceanLightBackgroundStart, OceanLightBackgroundMid, OceanLightBackgroundEnd)
        ThemeMode.OCEAN_DARK -> Triple(OceanDarkBackgroundStart, OceanDarkBackgroundMid, OceanDarkBackgroundEnd)
        ThemeMode.PETAL_LIGHT -> Triple(PetalLightBackgroundStart, PetalLightBackgroundMid, PetalLightBackgroundEnd)
        ThemeMode.PETAL_DARK -> Triple(PetalDarkBackgroundStart, PetalDarkBackgroundMid, PetalDarkBackgroundEnd)
        ThemeMode.SAND_LIGHT -> Triple(SandLightBackgroundStart, SandLightBackgroundMid, SandLightBackgroundEnd)
        ThemeMode.SAND_DARK -> Triple(SandDarkBackgroundStart, SandDarkBackgroundMid, SandDarkBackgroundEnd)
        ThemeMode.CLAY_LIGHT -> Triple(ClayLightBackgroundStart, ClayLightBackgroundMid, ClayLightBackgroundEnd)
        ThemeMode.CLAY_DARK -> Triple(ClayDarkBackgroundStart, ClayDarkBackgroundMid, ClayDarkBackgroundEnd)
        ThemeMode.INK_LIGHT -> Triple(InkLightBackgroundStart, InkLightBackgroundMid, InkLightBackgroundEnd)
        ThemeMode.INK_DARK -> Triple(InkDarkBackgroundStart, InkDarkBackgroundMid, InkDarkBackgroundEnd)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(start, mid, end))
            )
    ) {
        DotGridOverlay()
        content()
    }
}
