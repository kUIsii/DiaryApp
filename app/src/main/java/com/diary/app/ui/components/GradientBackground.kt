package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.diary.app.ui.theme.DarkBackgroundEnd
import com.diary.app.ui.theme.DarkBackgroundStart
import com.diary.app.ui.theme.LightBackgroundEnd
import com.diary.app.ui.theme.LightBackgroundMid
import com.diary.app.ui.theme.LightBackgroundStart

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) {
        listOf(DarkBackgroundStart, DarkBackgroundEnd)
    } else {
        listOf(LightBackgroundStart, LightBackgroundMid, LightBackgroundEnd)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = colors)
            )
    ) {
        content()
    }
}
