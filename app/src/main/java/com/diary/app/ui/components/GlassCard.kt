package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diary.app.ui.theme.DarkCardBackground
import com.diary.app.ui.theme.DarkCardBorder
import com.diary.app.ui.theme.LightCardBackground
import com.diary.app.ui.theme.LightCardBorder
import com.diary.app.ui.theme.PureDarkCardBackground
import com.diary.app.ui.theme.PureDarkCardBorder
import com.diary.app.ui.theme.PureLightCardBackground
import com.diary.app.ui.theme.PureLightCardBorder
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val mode = themeMode()
    val dark = mode.isDark()

    val (backgroundColor, borderColor) = when (mode) {
        ThemeMode.PURE_LIGHT -> PureLightCardBackground to PureLightCardBorder
        ThemeMode.PURE_DARK -> PureDarkCardBackground to PureDarkCardBorder
        ThemeMode.GRADIENT, ThemeMode.SYSTEM -> {
            if (dark) DarkCardBackground to DarkCardBorder
            else LightCardBackground to LightCardBorder
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
