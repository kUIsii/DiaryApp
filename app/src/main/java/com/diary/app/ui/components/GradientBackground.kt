package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

    val backgroundModifier = when (mode) {
        ThemeMode.PURE_LIGHT -> modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LightBackgroundStart, LightBackgroundMid, LightBackgroundEnd)
                )
            )
        ThemeMode.PURE_DARK -> modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackgroundStart, DarkBackgroundMid, DarkBackgroundEnd)
                )
            )
    }

    Box(modifier = backgroundModifier) {
        content()
    }
}
