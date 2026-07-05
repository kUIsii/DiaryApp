package com.diary.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.diary.app.ui.theme.ClayDarkBackgroundEnd
import com.diary.app.ui.theme.ClayDarkBackgroundMid
import com.diary.app.ui.theme.ClayDarkBackgroundStart
import com.diary.app.ui.theme.ClayLightBackgroundEnd
import com.diary.app.ui.theme.ClayLightBackgroundMid
import com.diary.app.ui.theme.ClayLightBackgroundStart
import com.diary.app.ui.theme.DarkBackgroundEnd
import com.diary.app.ui.theme.DarkBackgroundMid
import com.diary.app.ui.theme.DarkBackgroundStart
import com.diary.app.ui.theme.InkDarkBackgroundEnd
import com.diary.app.ui.theme.InkDarkBackgroundMid
import com.diary.app.ui.theme.InkDarkBackgroundStart
import com.diary.app.ui.theme.InkLightBackgroundEnd
import com.diary.app.ui.theme.InkLightBackgroundMid
import com.diary.app.ui.theme.InkLightBackgroundStart
import com.diary.app.ui.theme.LightBackgroundEnd
import com.diary.app.ui.theme.LightBackgroundMid
import com.diary.app.ui.theme.LightBackgroundStart
import com.diary.app.ui.theme.MossGreenDarkBackgroundEnd
import com.diary.app.ui.theme.MossGreenDarkBackgroundMid
import com.diary.app.ui.theme.MossGreenDarkBackgroundStart
import com.diary.app.ui.theme.MossGreenLightBackgroundEnd
import com.diary.app.ui.theme.MossGreenLightBackgroundMid
import com.diary.app.ui.theme.MossGreenLightBackgroundStart
import com.diary.app.ui.theme.OceanDarkBackgroundEnd
import com.diary.app.ui.theme.OceanDarkBackgroundMid
import com.diary.app.ui.theme.OceanDarkBackgroundStart
import com.diary.app.ui.theme.OceanLightBackgroundEnd
import com.diary.app.ui.theme.OceanLightBackgroundMid
import com.diary.app.ui.theme.OceanLightBackgroundStart
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
import com.diary.app.ui.theme.ThemeFamily
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode
import kotlin.math.sin

// --- Per-theme gradient drift ---

private fun driftDuration(theme: ThemeFamily): Int = when (theme) {
    ThemeFamily.BLUE -> 30000
    ThemeFamily.GREEN -> 25000
    ThemeFamily.CYAN -> 35000
    ThemeFamily.ROSE -> 28000
    ThemeFamily.AMBER -> 32000
    ThemeFamily.CLAY -> 40000
    ThemeFamily.INK -> 30000
}

private fun driftColor(base: Color, progress: Float, theme: ThemeFamily): Color {
    val p = (progress - 0.5f) * 2f
    val a = 0.03f
    return when (theme) {
        ThemeFamily.BLUE -> base.copy(
            red = (base.red + p * a * 0.5f).coerceIn(0f, 1f),
            green = (base.green + p * a * 0.7f).coerceIn(0f, 1f),
            blue = (base.blue + p * a).coerceIn(0f, 1f)
        )
        ThemeFamily.GREEN -> base.copy(
            red = (base.red + p * a * 0.6f).coerceIn(0f, 1f),
            green = (base.green + p * a).coerceIn(0f, 1f),
            blue = (base.blue + p * a * 0.5f).coerceIn(0f, 1f)
        )
        ThemeFamily.CYAN -> base.copy(
            red = (base.red + p * a * 0.4f).coerceIn(0f, 1f),
            green = (base.green + p * a).coerceIn(0f, 1f),
            blue = (base.blue + p * a * 0.9f).coerceIn(0f, 1f)
        )
        ThemeFamily.ROSE -> base.copy(
            red = (base.red + p * a).coerceIn(0f, 1f),
            green = (base.green + p * a * 0.5f).coerceIn(0f, 1f),
            blue = (base.blue + p * a * 0.4f).coerceIn(0f, 1f)
        )
        ThemeFamily.AMBER -> base.copy(
            red = (base.red + p * a).coerceIn(0f, 1f),
            green = (base.green + p * a * 0.8f).coerceIn(0f, 1f),
            blue = (base.blue + p * a * 0.3f).coerceIn(0f, 1f)
        )
        ThemeFamily.CLAY -> base.copy(
            red = (base.red + p * a * 0.9f).coerceIn(0f, 1f),
            green = (base.green + p * a * 0.6f).coerceIn(0f, 1f),
            blue = (base.blue + p * a * 0.5f).coerceIn(0f, 1f)
        )
        ThemeFamily.INK -> base.copy(
            red = (base.red + p * a * 0.7f).coerceIn(0f, 1f),
            green = (base.green + p * a * 0.6f).coerceIn(0f, 1f),
            blue = (base.blue + p * a).coerceIn(0f, 1f)
        )
    }
}

// --- Per-theme texture overlay ---

@Composable
private fun BackgroundOverlay(modifier: Modifier = Modifier) {
    val mode = themeMode()
    val dotColor = if (mode.isDark()) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f)
    val textureColor = if (mode.isDark()) Color.White.copy(alpha = 0.025f) else Color.Black.copy(alpha = 0.02f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val spacing = 22.dp.toPx()
                val dotRadius = 1.2f.dp.toPx()
                val colCount = (size.width / spacing).toInt() + 1
                val rowCount = (size.height / spacing).toInt() + 1

                val dotsPath = Path().apply {
                    for (col in 0 until colCount) {
                        for (row in 0 until rowCount) {
                            addOval(
                                Rect(center = Offset(col * spacing, row * spacing), radius = dotRadius)
                            )
                        }
                    }
                }

                val texturePath = when (mode.category) {
                    ThemeFamily.BLUE -> {
                        val px24 = 24.dp.toPx()
                        Path().apply {
                            var y = px24
                            while (y < size.height) {
                                moveTo(0f, y)
                                lineTo(size.width, y)
                                y += px24
                            }
                        }
                    }
                    ThemeFamily.GREEN -> {
                        val sp = 32.dp.toPx()
                        val rand = java.util.Random(42)
                        Path().apply {
                            for (col in 0 until (size.width / sp).toInt()) {
                                for (row in 0 until (size.height / sp).toInt()) {
                                    val cx = col * sp + rand.nextFloat() * 15.dp.toPx()
                                    val cy = row * sp + rand.nextFloat() * 15.dp.toPx()
                                    val r = 1.5f.dp.toPx() + rand.nextFloat() * 2.dp.toPx()
                                    addOval(Rect(center = Offset(cx, cy), radius = r))
                                }
                            }
                        }
                    }
                    ThemeFamily.CYAN -> {
                        val sp = 28.dp.toPx()
                        val amp = 6.dp.toPx()
                        Path().apply {
                            var y = sp
                            while (y < size.height) {
                                moveTo(0f, y)
                                var x = 0f
                                while (x < size.width) {
                                    lineTo(x, y + amp * sin(x * 0.05f))
                                    x += 2.dp.toPx()
                                }
                                y += sp
                            }
                        }
                    }
                    ThemeFamily.ROSE -> {
                        val sp = 36.dp.toPx()
                        val rx = 4.dp.toPx()
                        val ry = 2.5f.dp.toPx()
                        Path().apply {
                            for (col in 1 until (size.width / sp).toInt() step 2) {
                                for (row in 1 until (size.height / sp).toInt() step 2) {
                                    val cx = col * sp
                                    val cy = row * sp
                                    addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
                                }
                            }
                        }
                    }
                    ThemeFamily.AMBER -> {
                        val sp = 6.dp.toPx()
                        val rand = java.util.Random(77)
                        Path().apply {
                            for (col in 0 until (size.width / sp).toInt()) {
                                for (row in 0 until (size.height / sp).toInt()) {
                                    if (rand.nextFloat() < 0.15f) {
                                        val cx = col * sp + rand.nextFloat() * 3.dp.toPx()
                                        val cy = row * sp + rand.nextFloat() * 3.dp.toPx()
                                        addOval(Rect(center = Offset(cx, cy), radius = 0.5f.dp.toPx()))
                                    }
                                }
                            }
                        }
                    }
                    ThemeFamily.CLAY -> {
                        val sp = 36.dp.toPx()
                        Path().apply {
                            var start = -size.height
                            while (start < size.width + size.height) {
                                moveTo(start, 0f)
                                lineTo(start + size.height, size.height)
                                start += sp
                            }
                            start = -size.height
                            while (start < size.width + size.height) {
                                moveTo(start, 0f)
                                lineTo(start - size.height, size.height)
                                start += sp
                            }
                        }
                    }
                    ThemeFamily.INK -> {
                        val sp = 28.dp.toPx()
                        val dotR = 1.5f.dp.toPx()
                        Path().apply {
                            for (col in 0 until (size.width / sp).toInt()) {
                                for (row in 0 until (size.height / sp).toInt()) {
                                    addOval(Rect(center = Offset(col * sp, row * sp), radius = dotR))
                                }
                            }
                        }
                    }
                }

                onDrawBehind {
                    drawPath(dotsPath, color = dotColor)
                    drawPath(texturePath, color = textureColor)
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

    // Smooth theme transition: animate base colors when theme changes
    val isFirstComposition = remember { mutableStateOf(true) }
    LaunchedEffect(mode) {
        if (isFirstComposition.value) isFirstComposition.value = false
    }
    val transitionAnimSpec = if (isFirstComposition.value) tween<Color>(0) else tween<Color>(1500)

    val animatedStart by animateColorAsState(
        targetValue = start, animationSpec = transitionAnimSpec, label = "themeStart"
    )
    val animatedMid by animateColorAsState(
        targetValue = mid, animationSpec = transitionAnimSpec, label = "themeMid"
    )
    val animatedEnd by animateColorAsState(
        targetValue = end, animationSpec = transitionAnimSpec, label = "themeEnd"
    )

    val transition = rememberInfiniteTransition()
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(driftDuration(mode.category), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgDrift"
    )

    val driftedStart = remember(animatedStart, drift, mode.category) { driftColor(animatedStart, drift, mode.category) }
    val driftedMid = remember(animatedMid, drift, mode.category) { driftColor(animatedMid, drift, mode.category) }
    val driftedEnd = remember(animatedEnd, drift, mode.category) { driftColor(animatedEnd, drift, mode.category) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(driftedStart, driftedMid, driftedEnd))
            )
    ) {
        BackgroundOverlay()
        content()
    }
}
