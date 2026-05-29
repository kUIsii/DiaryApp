package com.diary.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    duration: Int = 1000,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        )
    }

    Text(
        text = "$prefix${animatedValue.value.toInt()}$suffix",
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        style = style
    )
}

@Composable
fun AnimatedFloatCounter(
    targetValue: Float,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    decimals: Int = 1,
    duration: Int = 1000,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue,
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        )
    }

    val formatStr = "%.${decimals}f"
    Text(
        text = "$prefix${formatStr.format(animatedValue.value)}$suffix",
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        style = style
    )
}
