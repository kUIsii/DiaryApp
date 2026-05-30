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
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.DarkCardBackground
import com.diary.app.ui.theme.DarkCardBorder
import com.diary.app.ui.theme.LightAccentEnd
import com.diary.app.ui.theme.LightAccentStart
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
        ThemeMode.WARM_ROSE -> {
            if (dark) com.diary.app.ui.theme.WarmRoseDarkSurfaceVariant to Color(0x33E0CCC8)
            else com.diary.app.ui.theme.WarmRoseSurfaceVariant to Color(0x80E8D5CF)
        }
        ThemeMode.GRADIENT, ThemeMode.SYSTEM -> {
            if (dark) DarkCardBackground to DarkCardBorder
            else LightCardBackground to LightCardBorder
        }
    }

    // Dark mode: 1.5dp border for more visibility; light mode: 1dp
    val borderWidth = if (dark) 1.5.dp else 1.dp

    // Shadow logic: explicit shadow, or subtle shadow in light gradient/system mode
    val shadowElevation = when {
        enableShadow -> if (dark) 8.dp else 4.dp
        !dark && mode != ThemeMode.PURE_LIGHT -> 2.dp
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

@Composable
fun GlassCardElevated(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    innerPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        enableShadow = true,
        innerPadding = innerPadding,
        content = content
    )
}

@Composable
fun GlassCardAccent(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 2.dp,
    innerPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val mode = themeMode()
    val dark = mode.isDark()

    val backgroundColor = when (mode) {
        ThemeMode.PURE_LIGHT -> PureLightCardBackground
        ThemeMode.PURE_DARK -> PureDarkCardBackground
        ThemeMode.WARM_ROSE -> {
            if (dark) com.diary.app.ui.theme.WarmRoseDarkSurfaceVariant else com.diary.app.ui.theme.WarmRoseSurfaceVariant
        }
        ThemeMode.GRADIENT, ThemeMode.SYSTEM -> {
            if (dark) DarkCardBackground else LightCardBackground
        }
    }

    val accentColors = if (dark) {
        listOf(DarkAccentStart, DarkAccentEnd)
    } else {
        listOf(LightAccentStart, LightAccentEnd)
    }

    // Subtle shadow in light gradient/system mode
    val shadowElevation = if (!dark && mode != ThemeMode.PURE_LIGHT) 2.dp else 0.dp

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(shadowElevation, shape)
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, Brush.linearGradient(accentColors), shape)
    ) {
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
