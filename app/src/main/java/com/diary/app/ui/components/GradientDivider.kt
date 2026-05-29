package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.LightAccentEnd
import com.diary.app.ui.theme.LightAccentStart
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode

@Composable
fun GradientDivider(
    modifier: Modifier = Modifier,
    height: Dp = 1.dp,
    paddingHorizontal: Dp = 0.dp,
    gradientColors: List<Color>? = null
) {
    val mode = themeMode()
    val dark = mode.isDark()

    val colors = gradientColors ?: if (dark) {
        listOf(
            DarkAccentStart.copy(alpha = 0.1f),
            DarkAccentEnd.copy(alpha = 0.3f),
            DarkAccentStart.copy(alpha = 0.1f)
        )
    } else {
        listOf(
            LightAccentStart.copy(alpha = 0.1f),
            LightAccentEnd.copy(alpha = 0.3f),
            LightAccentStart.copy(alpha = 0.1f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = paddingHorizontal)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Brush.horizontalGradient(colors))
    )
}

@Composable
fun SubtleDivider(
    modifier: Modifier = Modifier,
    height: Dp = 0.5.dp,
    color: Color = Color.Gray.copy(alpha = 0.15f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color)
    )
}
