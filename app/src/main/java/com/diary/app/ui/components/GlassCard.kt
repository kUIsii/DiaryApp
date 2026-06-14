package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diary.app.ui.theme.MossGreenDarkCardBackground
import com.diary.app.ui.theme.MossGreenDarkCardBorder
import com.diary.app.ui.theme.MossGreenLightCardBackground
import com.diary.app.ui.theme.MossGreenLightCardBorder
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
    cornerRadius: Dp = 16.dp,
    enableShadow: Boolean = false,
    gradientColors: List<Color>? = null,
    innerPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val mode = themeMode()
    val dark = mode.isDark()

    val (backgroundColor, borderColor) = when (mode) {
        ThemeMode.PURE_LIGHT -> PureLightCardBackground to PureLightCardBorder
        ThemeMode.PURE_DARK -> PureDarkCardBackground to PureDarkCardBorder
        ThemeMode.MOSS_GREEN_LIGHT -> MossGreenLightCardBackground to MossGreenLightCardBorder
        ThemeMode.MOSS_GREEN_DARK -> MossGreenDarkCardBackground to MossGreenDarkCardBorder
    }

    // Dark mode: 1.5dp border for more visibility; light mode: 1dp
    val borderWidth = if (dark) 1.5.dp else 1.2.dp

    val shadowElevation = when {
        enableShadow -> if (dark) 8.dp else 4.dp
        else -> 0.dp
    }

    val shape = RoundedCornerShape(cornerRadius)

    val backgroundModifier = if (gradientColors != null) {
        Modifier.background(Brush.linearGradient(gradientColors), shape)
    } else {
        Modifier.background(backgroundColor, shape)
    }

    Box(
        modifier = modifier
            .shadow(shadowElevation, shape)
            .clip(shape)
            .then(backgroundModifier)
            .border(borderWidth, borderColor, shape)
    ) {
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
