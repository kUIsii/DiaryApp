package com.diary.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diary.app.ui.theme.MossGreenDarkCardBackground
import com.diary.app.ui.theme.MossGreenDarkCardBorder
import com.diary.app.ui.theme.MossGreenLightCardBackground
import com.diary.app.ui.theme.MossGreenLightCardBorder
import com.diary.app.ui.theme.ClayDarkCardBackground
import com.diary.app.ui.theme.ClayDarkCardBorder
import com.diary.app.ui.theme.ClayLightCardBackground
import com.diary.app.ui.theme.ClayLightCardBorder
import com.diary.app.ui.theme.InkDarkCardBackground
import com.diary.app.ui.theme.InkDarkCardBorder
import com.diary.app.ui.theme.InkLightCardBackground
import com.diary.app.ui.theme.InkLightCardBorder
import com.diary.app.ui.theme.OceanDarkCardBackground
import com.diary.app.ui.theme.OceanDarkCardBorder
import com.diary.app.ui.theme.OceanLightCardBackground
import com.diary.app.ui.theme.OceanLightCardBorder
import com.diary.app.ui.theme.PetalDarkCardBackground
import com.diary.app.ui.theme.PetalDarkCardBorder
import com.diary.app.ui.theme.PetalLightCardBackground
import com.diary.app.ui.theme.PetalLightCardBorder
import com.diary.app.ui.theme.PureDarkCardBackground
import com.diary.app.ui.theme.PureDarkCardBorder
import com.diary.app.ui.theme.PureLightCardBackground
import com.diary.app.ui.theme.PureLightCardBorder
import com.diary.app.ui.theme.SandDarkCardBackground
import com.diary.app.ui.theme.SandDarkCardBorder
import com.diary.app.ui.theme.SandLightCardBackground
import com.diary.app.ui.theme.SandLightCardBorder
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
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val mode = themeMode()
    val dark = mode.isDark()

    val (backgroundColor, borderColor) = when (mode) {
        ThemeMode.PURE_LIGHT -> PureLightCardBackground to PureLightCardBorder
        ThemeMode.PURE_DARK -> PureDarkCardBackground to PureDarkCardBorder
        ThemeMode.MOSS_GREEN_LIGHT -> MossGreenLightCardBackground to MossGreenLightCardBorder
        ThemeMode.MOSS_GREEN_DARK -> MossGreenDarkCardBackground to MossGreenDarkCardBorder
        ThemeMode.OCEAN_LIGHT -> OceanLightCardBackground to OceanLightCardBorder
        ThemeMode.OCEAN_DARK -> OceanDarkCardBackground to OceanDarkCardBorder
        ThemeMode.PETAL_LIGHT -> PetalLightCardBackground to PetalLightCardBorder
        ThemeMode.PETAL_DARK -> PetalDarkCardBackground to PetalDarkCardBorder
        ThemeMode.SAND_LIGHT -> SandLightCardBackground to SandLightCardBorder
        ThemeMode.SAND_DARK -> SandDarkCardBackground to SandDarkCardBorder
        ThemeMode.CLAY_LIGHT -> ClayLightCardBackground to ClayLightCardBorder
        ThemeMode.CLAY_DARK -> ClayDarkCardBackground to ClayDarkCardBorder
        ThemeMode.INK_LIGHT -> InkLightCardBackground to InkLightCardBorder
        ThemeMode.INK_DARK -> InkDarkCardBackground to InkDarkCardBorder
    }

    val borderWidth = if (dark) 1.5.dp else 1.2.dp

    val shadowElevation = when {
        enableShadow -> if (dark) 8.dp else 4.dp
        else -> 0.dp
    }

    val shape = RoundedCornerShape(cornerRadius)

    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = tween(100),
        label = "cardPress"
    )

    val backgroundModifier = if (gradientColors != null) {
        Modifier.background(Brush.linearGradient(gradientColors), shape)
    } else {
        Modifier.background(backgroundColor, shape)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(shadowElevation, shape)
            .clip(shape)
            .then(backgroundModifier)
            .then(clickableModifier)
            .border(borderWidth, borderColor, shape)
    ) {
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
