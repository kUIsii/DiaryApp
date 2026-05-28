package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.diary.app.ui.theme.DarkBackgroundEnd
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
                listOf(DarkBackgroundStart, DarkBackgroundEnd)
            } else {
                listOf(LightBackgroundStart, LightBackgroundMid, LightBackgroundEnd)
            }
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = colors))
        }
    }

    Box(modifier = backgroundModifier) {
        content()
    }
}
