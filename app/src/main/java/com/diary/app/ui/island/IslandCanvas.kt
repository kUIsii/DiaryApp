package com.diary.app.ui.island

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.diary.app.data.AnimalBehavior
import com.diary.app.data.AnimalType
import com.diary.app.data.ComboDefinition
import com.diary.app.data.IslandAnimal
import com.diary.app.data.IslandDecoration
import com.diary.app.data.IslandDiscovery
import com.diary.app.data.IslandEnvironment
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IslandCanvas(
    environment: IslandEnvironment,
    decorations: List<IslandDecoration>,
    activeAnimals: List<IslandAnimal> = emptyList(),
    activeCombos: List<ComboDefinition> = emptyList(),
    activeRareElements: List<IslandDiscovery> = emptyList(),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "island_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing)
        ),
        label = "time"
    )

    val windmillBladeAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "windmill_blades"
    )

    val activeDecorations = remember(decorations) {
        decorations.filter { it.isUnlocked }
    }

    // 真实时间 -> timeOfDay (0.0=午夜, 0.25=日出, 0.5=正午, 0.75=日落)
    val timeOfDay = remember {
        val now = LocalTime.now()
        now.hour / 24f + now.minute / 1440f
    }

    // 预分配星星位置，避免每帧重新生成
    val starPositions = remember {
        List(40) { i ->
            val seed = i * 137.508 // 黄金角度分布
            Pair(
                ((seed * 0.618f) % 1f).toFloat(),
                ((seed * 0.381f) % 0.45f).toFloat() // 天空上半部分
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Layer 0: 天空背景 (基于真实时间)
        drawSkyLayer(environment.brightness, timeOfDay, time, starPositions)

        // Layer 0.5: 稀有元素特效（天空层）
        drawRareElementsSky(activeRareElements, time, timeOfDay)

        // Layer 1: 海洋 (带时间反光)
        drawOceanLayer(environment.tranquility, time, timeOfDay, activeCombos)

        // Layer 2: 岛屿地形
        drawTerrainLayer(environment.lushness)

        // Layer 3: 植被
        drawVegetationLayer(environment.lushness, environment.brightness)

        // Layer 4: 建筑/装饰
        drawBuildingLayer(activeDecorations, windmillBladeAngle, activeCombos, time)

        // Layer 5: 动物
        drawAnimalLayer(activeAnimals, activeDecorations, time, timeOfDay)

        // Layer 6: 特效 (增强天气粒子)
        drawEffectLayer(environment, time, activeCombos)

        // Layer 7: 稀有元素特效（前景）
        drawRareElementsForeground(activeRareElements, time)
    }
}

// --- 昼夜循环颜色工具 ---

/** 线性插值两个颜色 */
private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = c1.red + (c2.red - c1.red) * f,
        green = c1.green + (c2.green - c1.green) * f,
        blue = c1.blue + (c2.blue - c1.blue) * f,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * f
    )
}

/**
 * 根据 timeOfDay 计算天空渐变色（顶部 + 底部）
 * timeOfDay: 0.0=午夜, 0.25=日出, 0.5=正午, 0.75=日落
 */
private fun computeSkyColors(timeOfDay: Float): Pair<Color, Color> {
    // 关键时间点的天空颜色
    val nightTop = Color(0xFF0A0E27)       // 深蓝夜空
    val nightBottom = Color(0xFF1A1A3E)
    val sunriseTop = Color(0xFF4A6FA5)     // 日出过渡
    val sunriseBottom = Color(0xFFFFCC80)
    val dayTop = Color(0xFF4BA3E3)         // 白天蓝天
    val dayBottom = Color(0xFFB3E5FC)
    val sunsetTop = Color(0xFFFF8A65)      // 日落
    val sunsetBottom = Color(0xFF5D4037)

    val top: Color
    val bottom: Color

    when {
        // 0.0-0.21 (0-5h): 深蓝夜空
        timeOfDay < 0.21f -> {
            top = nightTop
            bottom = nightBottom
        }
        // 0.21-0.29 (5-7h): 日出渐变
        timeOfDay < 0.29f -> {
            val t = (timeOfDay - 0.21f) / 0.08f
            top = lerpColor(nightTop, sunriseTop, t)
            bottom = lerpColor(nightBottom, sunriseBottom, t)
        }
        // 0.29-0.33 (7-8h): 日出→白天
        timeOfDay < 0.33f -> {
            val t = (timeOfDay - 0.29f) / 0.04f
            top = lerpColor(sunriseTop, dayTop, t)
            bottom = lerpColor(sunriseBottom, dayBottom, t)
        }
        // 0.33-0.71 (8-17h): 白天蓝天
        timeOfDay < 0.71f -> {
            top = dayTop
            bottom = dayBottom
        }
        // 0.71-0.75 (17-18h): 白天→日落
        timeOfDay < 0.75f -> {
            val t = (timeOfDay - 0.71f) / 0.04f
            top = lerpColor(dayTop, sunsetTop, t)
            bottom = lerpColor(dayBottom, sunsetBottom, t)
        }
        // 0.75-0.79 (18-19h): 日落→夜晚
        timeOfDay < 0.79f -> {
            val t = (timeOfDay - 0.75f) / 0.04f
            top = lerpColor(sunsetTop, nightTop, t)
            bottom = lerpColor(sunsetBottom, nightBottom, t)
        }
        // 0.79-1.0 (19-24h): 深蓝夜空
        else -> {
            top = nightTop
            bottom = nightBottom
        }
    }

    return Pair(top, bottom)
}

/** 是否处于夜间 (19h-5h) */
private fun isNight(timeOfDay: Float): Boolean = timeOfDay > 0.79f || timeOfDay < 0.21f

/** 是否处于日出/日落时段 */
private fun isGoldenHour(timeOfDay: Float): Boolean =
    (timeOfDay in 0.21f..0.33f) || (timeOfDay in 0.71f..0.79f)

// Layer 0: 天空 (昼夜循环)
private fun DrawScope.drawSkyLayer(
    brightness: Float,
    timeOfDay: Float,
    time: Float,
    starPositions: List<Pair<Float, Float>>
) {
    val (skyTop, skyBottom) = computeSkyColors(timeOfDay)

    // 计算中间过渡色
    val skyMid = lerpColor(skyTop, skyBottom, 0.5f)

    // 天空渐变背景 (三色渐变更细腻)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyTop, skyMid, skyBottom),
            startY = 0f,
            endY = size.height * 0.6f
        ),
        size = Size(size.width, size.height * 0.6f)
    )

    // 星星 (夜间显示，带闪烁)
    if (isNight(timeOfDay)) {
        drawStars(starPositions, time)
    }

    // 云朵 (白天和傍晚显示)
    if (!isNight(timeOfDay)) {
        drawClouds(time, timeOfDay)
    }

    // 太阳/月亮沿椭圆轨迹移动
    drawCelestialBodies(timeOfDay, brightness)
}

/** 绘制星星 (闪烁效果) */
private fun DrawScope.drawStars(starPositions: List<Pair<Float, Float>>, time: Float) {
    for ((i, pos) in starPositions.withIndex()) {
        val x = pos.first * size.width
        val y = pos.second * size.height
        // 每颗星星独立闪烁频率
        val twinkle = 0.3f + 0.7f * abs(sin(time * 0.5f + i * 2.1f))
        val starAlpha = twinkle * 0.8f

        // 星星核心
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = starAlpha),
            radius = 1.2f + (i % 3) * 0.4f,
            center = Offset(x, y)
        )

        // 微弱光晕
        if (i % 4 == 0) {
            drawCircle(
                color = Color(0xFFCCCCFF).copy(alpha = starAlpha * 0.3f),
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

/** 绘制云朵 (贝塞尔曲线有机形状) */
private fun DrawScope.drawClouds(time: Float, timeOfDay: Float) {
    val isGolden = isGoldenHour(timeOfDay)
    val cloudBaseAlpha = if (isGolden) 0.45f else 0.35f

    val cloudColor = if (isGolden) {
        Color(0xFFFFE0B2).copy(alpha = cloudBaseAlpha)
    } else {
        Color.White.copy(alpha = cloudBaseAlpha)
    }

    data class CloudDef(val baseX: Float, val baseY: Float, val scale: Float, val speed: Float)

    val clouds = listOf(
        CloudDef(0.15f, 0.08f, 1.0f, 0.3f),
        CloudDef(0.45f, 0.12f, 0.7f, 0.2f),
        CloudDef(0.75f, 0.06f, 0.85f, 0.25f),
        CloudDef(0.3f, 0.18f, 0.5f, 0.15f),
        CloudDef(0.85f, 0.15f, 0.6f, 0.35f)
    )

    for (cloud in clouds) {
        val driftX = sin(time * cloud.speed * 0.1f).toFloat() * size.width * 0.03f
        val cx = cloud.baseX * size.width + driftX
        val cy = cloud.baseY * size.height
        val s = cloud.scale

        val cloudAlpha = cloudBaseAlpha * (0.8f + 0.2f * sin(time * 0.2f + cloud.baseX * 10f).toFloat())
        val currentCloudColor = cloudColor.copy(alpha = cloudAlpha)

        // 使用贝塞尔曲线绘制云朵有机形状
        val cloudPath = Path().apply {
            // 底部基线
            moveTo(cx - 28f * s, cy + 8f * s)
            // 底部曲线
            cubicTo(
                cx - 20f * s, cy + 12f * s,
                cx - 5f * s, cy + 14f * s,
                cx + 10f * s, cy + 10f * s
            )
            cubicTo(
                cx + 20f * s, cy + 8f * s,
                cx + 28f * s, cy + 10f * s,
                cx + 30f * s, cy + 6f * s
            )
            // 右侧上升
            cubicTo(
                cx + 35f * s, cy + 2f * s,
                cx + 32f * s, cy - 5f * s,
                cx + 25f * s, cy - 8f * s
            )
            // 顶部右侧弧
            cubicTo(
                cx + 20f * s, cy - 14f * s,
                cx + 10f * s, cy - 18f * s,
                cx + 2f * s, cy - 12f * s
            )
            // 顶部中间凸起
            cubicTo(
                cx - 5f * s, cy - 16f * s,
                cx - 12f * s, cy - 14f * s,
                cx - 16f * s, cy - 10f * s
            )
            // 顶部左侧弧
            cubicTo(
                cx - 22f * s, cy - 12f * s,
                cx - 28f * s, cy - 8f * s,
                cx - 30f * s, cy - 4f * s
            )
            // 左侧下降
            cubicTo(
                cx - 34f * s, cy + 0f,
                cx - 32f * s, cy + 5f * s,
                cx - 28f * s, cy + 8f * s
            )
            close()
        }
        drawPath(path = cloudPath, color = currentCloudColor)

        // 云朵顶部高光层
        val highlightColor = if (isGolden) {
            Color.White.copy(alpha = cloudAlpha * 0.3f)
        } else {
            Color.White.copy(alpha = cloudAlpha * 0.25f)
        }
        val highlightPath = Path().apply {
            moveTo(cx - 15f * s, cy - 2f * s)
            cubicTo(
                cx - 10f * s, cy - 10f * s,
                cx + 0f, cy - 14f * s,
                cx + 10f * s, cy - 10f * s
            )
            cubicTo(
                cx + 15f * s, cy - 8f * s,
                cx + 12f * s, cy - 2f * s,
                cx + 5f * s, cy - 1f * s
            )
            cubicTo(
                cx + 0f, cy - 4f * s,
                cx - 8f * s, cy - 3f * s,
                cx - 15f * s, cy - 2f * s
            )
            close()
        }
        drawPath(path = highlightPath, color = highlightColor)
    }
}

/** 绘制太阳和月亮沿椭圆轨迹移动 */
private fun DrawScope.drawCelestialBodies(timeOfDay: Float, brightness: Float) {
    val centerX = size.width * 0.5f
    val centerY = size.height * 0.3f
    val ellipseA = size.width * 0.4f // 椭圆半长轴
    val ellipseB = size.height * 0.25f // 椭圆半短轴

    // 太阳: 在 5h-19h 之间可见 (timeOfDay 0.21-0.79)
    if (timeOfDay in 0.21f..0.79f) {
        // 将 0.21-0.79 映射到 0-Pi (太阳从东边升起到西边落下)
        val sunProgress = (timeOfDay - 0.21f) / 0.58f
        val sunAngle = sunProgress * Math.PI.toFloat()

        val sunX = centerX - cos(sunAngle.toDouble()).toFloat() * ellipseA
        val sunY = centerY + sin(sunAngle.toDouble()).toFloat() * ellipseB * 0.6f - ellipseB * 0.3f
        val sunRadius = 22f + brightness * 15f

        // 日出/日落时段太阳偏暖橙色
        val isGolden = isGoldenHour(timeOfDay)
        val sunColor = if (isGolden) {
            Color(0xFFFF9800).copy(alpha = 0.9f)
        } else {
            Color(0xFFFFEB3B).copy(alpha = 0.85f)
        }

        // 外层大气光晕
        val outerGlow = if (isGolden) {
            Color(0xFFFFCC80).copy(alpha = 0.12f)
        } else {
            Color(0xFFFFECB3).copy(alpha = 0.15f)
        }
        drawCircle(color = outerGlow, radius = sunRadius * 3.5f, center = Offset(sunX, sunY))

        // 中层光晕
        val glowColor = if (isGolden) {
            Color(0xFFFFCC80).copy(alpha = 0.3f)
        } else {
            Color(0xFFFFECB3).copy(alpha = 0.35f)
        }
        drawCircle(color = glowColor, radius = sunRadius * 2f, center = Offset(sunX, sunY))

        // 太阳光芒 (射线效果 - 12条放射状线条，长短交替)
        val rayCount = 12
        for (i in 0 until rayCount) {
            val rayAngle = (360f / rayCount) * i
            val rayRad = Math.toRadians(rayAngle.toDouble()).toFloat()
            val rayInner = sunRadius * 1.15f
            // 长短交替，增加韵律感
            val rayLength = when (i % 3) {
                0 -> sunRadius * 1.8f  // 长
                1 -> sunRadius * 1.4f  // 中
                else -> sunRadius * 1.6f // 中长
            }
            val rayOuter = sunRadius + rayLength
            // 渐变透明度
            val rayAlpha = 0.25f - (i % 2) * 0.05f
            drawLine(
                color = sunColor.copy(alpha = rayAlpha),
                start = Offset(sunX + cos(rayRad) * rayInner, sunY + sin(rayRad) * rayInner),
                end = Offset(sunX + cos(rayRad) * rayOuter, sunY + sin(rayRad) * rayOuter),
                strokeWidth = 2.5f - (i % 3) * 0.3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // 太阳主体
        drawCircle(color = sunColor, radius = sunRadius, center = Offset(sunX, sunY))

        // 内核渐变高光
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(sunX - sunRadius * 0.15f, sunY - sunRadius * 0.15f),
                radius = sunRadius * 0.8f
            ),
            radius = sunRadius * 0.8f,
            center = Offset(sunX - sunRadius * 0.15f, sunY - sunRadius * 0.15f)
        )
    }

    // 月亮: 在 19h-5h 之间可见 (timeOfDay > 0.79 或 < 0.21)
    if (isNight(timeOfDay)) {
        // 将夜间时间映射到月亮轨迹
        val moonProgress = if (timeOfDay >= 0.79f) {
            (timeOfDay - 0.79f) / 0.42f // 0.79 -> 1.0 映射到 0-0.5
        } else {
            0.5f + timeOfDay / 0.42f // 0 -> 0.21 映射到 0.5-1.0
        }
        val moonAngle = moonProgress * Math.PI.toFloat()

        val moonX = centerX - cos(moonAngle.toDouble()).toFloat() * ellipseA * 0.8f
        val moonY = centerY + sin(moonAngle.toDouble()).toFloat() * ellipseB * 0.4f - ellipseB * 0.2f
        val moonRadius = 16f

        // 月光外层光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE8EAF6).copy(alpha = 0.18f),
                    Color(0xFFE8EAF6).copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(moonX, moonY),
                radius = moonRadius * 3f
            ),
            radius = moonRadius * 3f,
            center = Offset(moonX, moonY)
        )

        // 月光内层光晕
        drawCircle(
            color = Color(0xFFE8EAF6).copy(alpha = 0.15f),
            radius = moonRadius * 2f,
            center = Offset(moonX, moonY)
        )

        // 月亮光芒 (柔和的放射状线条)
        val moonRayCount = 8
        for (i in 0 until moonRayCount) {
            val rayAngle = (360f / moonRayCount) * i
            val rayRad = Math.toRadians(rayAngle.toDouble()).toFloat()
            val rayInner = moonRadius * 1.2f
            val rayOuter = moonRadius * 1.8f + (i % 2) * moonRadius * 0.3f
            val rayAlpha = 0.1f + (i % 3) * 0.02f
            drawLine(
                color = Color(0xFFE8EAF6).copy(alpha = rayAlpha),
                start = Offset(moonX + cos(rayRad) * rayInner, moonY + sin(rayRad) * rayInner),
                end = Offset(moonX + cos(rayRad) * rayOuter, moonY + sin(rayRad) * rayOuter),
                strokeWidth = 1.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // 月亮本体
        drawCircle(
            color = Color(0xFFECEFF1),
            radius = moonRadius,
            center = Offset(moonX, moonY)
        )

        // 月球表面纹理 (多个小圆模拟陨石坑)
        drawCircle(
            color = Color(0xFFCFD8DC).copy(alpha = 0.2f),
            radius = moonRadius * 0.25f,
            center = Offset(moonX - 3f, moonY - 4f)
        )
        drawCircle(
            color = Color(0xFFCFD8DC).copy(alpha = 0.15f),
            radius = moonRadius * 0.18f,
            center = Offset(moonX + 5f, moonY + 2f)
        )
        drawCircle(
            color = Color(0xFFCFD8DC).copy(alpha = 0.12f),
            radius = moonRadius * 0.12f,
            center = Offset(moonX + 1f, moonY + 5f)
        )

        // 月牙暗面
        drawCircle(
            color = Color(0xFF1A237E).copy(alpha = 0.35f),
            radius = moonRadius * 0.75f,
            center = Offset(moonX + moonRadius * 0.35f, moonY - moonRadius * 0.15f)
        )

        // 月亮高光
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = moonRadius * 0.18f,
            center = Offset(moonX - moonRadius * 0.3f, moonY - moonRadius * 0.3f)
        )
    }
}

// Layer 1: 海洋 (带时间反光增强)
private fun DrawScope.drawOceanLayer(tranquility: Float, time: Float, timeOfDay: Float, activeCombos: List<ComboDefinition> = emptyList()) {
    val oceanY = size.height * 0.55f
    val oceanHeight = size.height * 0.45f

    // 根据时间段调整海洋颜色
    val isNightTime = isNight(timeOfDay)
    val isGolden = isGoldenHour(timeOfDay)

    val baseOceanColor = when {
        isNightTime -> Color(0xFF1A237E).copy(alpha = 0.7f + tranquility * 0.3f)
        isGolden -> Color(0xFFFF8A65).copy(alpha = 0.5f + tranquility * 0.3f)
        else -> Color(0xFF4FC3F7).copy(alpha = 0.6f + tranquility * 0.4f)
    }

    val lightOceanColor = when {
        isNightTime -> Color(0xFF283593).copy(alpha = 0.5f + tranquility * 0.4f)
        isGolden -> Color(0xFFFFCC80).copy(alpha = 0.5f + tranquility * 0.4f)
        else -> Color(0xFF81D4FA).copy(alpha = 0.5f + tranquility * 0.5f)
    }

    // 海洋背景
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(lightOceanColor, baseOceanColor),
            startY = oceanY,
            endY = size.height
        ),
        topLeft = Offset(0f, oceanY),
        size = Size(size.width, oceanHeight)
    )

    // 波浪动画 - 使用 time 驱动
    val waveOffset = time * 0.5f
    val waveAmplitude = 8f + (1f - tranquility) * 12f

    // 波浪颜色
    val waveColor = when {
        isGolden -> Color(0xFFFFCC80).copy(alpha = 0.5f)
        isNightTime -> Color(0xFF7986CB).copy(alpha = 0.3f)
        else -> Color(0xFFB3E5FC).copy(alpha = 0.5f)
    }

    // 第一层波浪 (主波浪)
    val wavePath = Path().apply {
        moveTo(0f, oceanY + 20f)
        for (x in 0..size.width.toInt() step 8) {
            val xFloat = x.toFloat()
            // 叠加两个正弦波创造更自然的波浪
            val y = oceanY + 20f + sin(xFloat * 0.015f + waveOffset).toFloat() * waveAmplitude +
                    sin(xFloat * 0.025f + waveOffset * 1.3f).toFloat() * waveAmplitude * 0.3f
            lineTo(xFloat, y)
        }
        lineTo(size.width, oceanY + 45f)
        lineTo(0f, oceanY + 45f)
        close()
    }
    drawPath(path = wavePath, color = waveColor, style = Fill)

    // 第二层波浪 (相位偏移，更小的振幅)
    val wave2Path = Path().apply {
        moveTo(0f, oceanY + 28f)
        for (x in 0..size.width.toInt() step 8) {
            val xFloat = x.toFloat()
            val y = oceanY + 28f + sin(xFloat * 0.01f + waveOffset * 0.7f + 2f).toFloat() * waveAmplitude * 0.55f +
                    cos(xFloat * 0.018f + waveOffset * 0.9f).toFloat() * waveAmplitude * 0.2f
            lineTo(xFloat, y)
        }
        lineTo(size.width, oceanY + 55f)
        lineTo(0f, oceanY + 55f)
        close()
    }
    drawPath(path = wave2Path, color = waveColor.copy(alpha = waveColor.alpha * 0.7f), style = Fill)

    // 第三层波浪 (远处细波)
    val wave3Color = when {
        isGolden -> Color(0xFFFFE0B2).copy(alpha = 0.35f)
        isNightTime -> Color(0xFF5C6BC0).copy(alpha = 0.2f)
        else -> Color(0xFFE1F5FE).copy(alpha = 0.35f)
    }
    val wave3Path = Path().apply {
        moveTo(0f, oceanY + 15f)
        for (x in 0..size.width.toInt() step 12) {
            val xFloat = x.toFloat()
            val y = oceanY + 15f + sin(xFloat * 0.02f + waveOffset * 0.4f + 4f).toFloat() * waveAmplitude * 0.3f
            lineTo(xFloat, y)
        }
        lineTo(size.width, oceanY + 35f)
        lineTo(0f, oceanY + 35f)
        close()
    }
    drawPath(path = wave3Path, color = wave3Color, style = Fill)

    // 第四层波浪 (近处大波浪，低频高振幅)
    val wave4Color = when {
        isGolden -> Color(0xFFFFCC80).copy(alpha = 0.4f)
        isNightTime -> Color(0xFF3949AB).copy(alpha = 0.25f)
        else -> Color(0xFF4FC3F7).copy(alpha = 0.45f)
    }
    val wave4Path = Path().apply {
        moveTo(0f, oceanY + 35f)
        for (x in 0..size.width.toInt() step 6) {
            val xFloat = x.toFloat()
            val y = oceanY + 35f + sin(xFloat * 0.008f + waveOffset * 0.6f + 1.5f).toFloat() * waveAmplitude * 0.6f +
                    cos(xFloat * 0.012f + waveOffset * 0.45f).toFloat() * waveAmplitude * 0.25f
            lineTo(xFloat, y)
        }
        lineTo(size.width, oceanY + 70f)
        lineTo(0f, oceanY + 70f)
        close()
    }
    drawPath(path = wave4Path, color = wave4Color.copy(alpha = wave4Color.alpha * 0.6f), style = Fill)

    // 水面高光线条 (细长的水平光线)
    val highlightColor = when {
        isGolden -> Color(0xFFFFF8E1).copy(alpha = 0.4f)
        isNightTime -> Color(0xFF9FA8DA).copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.25f)
    }
    for (i in 0..5) {
        val hlY = oceanY + 20f + i * 12f
        val hlPhase = sin(time * 0.4f + i * 0.8f).toFloat()
        val hlX = size.width * (0.15f + i * 0.12f) + hlPhase * 15f
        val hlWidth = 30f + sin(time * 0.3f + i * 1.2f).toFloat() * 10f
        drawLine(
            color = highlightColor,
            start = Offset(hlX - hlWidth / 2, hlY),
            end = Offset(hlX + hlWidth / 2, hlY),
            strokeWidth = 1f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    // 小浪花效果 (在波浪交汇处)
    val splashColor = Color.White.copy(alpha = 0.35f)
    for (i in 0..8) {
        val splashX = size.width * (0.08f + i * 0.12f)
        val splashPhase = (time * 2f + i * 1.3f) % 3f
        if (splashPhase < 1.5f) {
            // 浪花溅起
            val splashHeight = sin(splashPhase * Math.PI.toFloat()).toFloat() * 6f
            val splashAlpha = (1f - splashPhase / 1.5f) * 0.35f
            drawCircle(
                color = splashColor.copy(alpha = splashAlpha),
                radius = 2f + splashPhase * 0.5f,
                center = Offset(splashX, oceanY + 22f - splashHeight)
            )
            // 小水滴
            if (splashPhase > 0.3f) {
                drawCircle(
                    color = splashColor.copy(alpha = splashAlpha * 0.5f),
                    radius = 1f,
                    center = Offset(splashX + 3f, oceanY + 20f - splashHeight * 0.7f)
                )
                drawCircle(
                    color = splashColor.copy(alpha = splashAlpha * 0.4f),
                    radius = 0.8f,
                    center = Offset(splashX - 2f, oceanY + 19f - splashHeight * 0.5f)
                )
            }
        }
    }

    // 海岸线泡沫效果
    drawFoamEffect(oceanY, time, waveAmplitude)

    // 水面反光效果
    drawWaterReflection(timeOfDay, time, oceanY)

    // 静谧水岸组合效果: 水面倒映桥影
    if (activeCombos.any { it.id == "quiet_shore" }) {
        // 桥梁倒影 (半透明倒置)
        val bridgeX = size.width * 0.3f // 桥梁位置
        val reflectionY = oceanY + 25f
        val reflectionAlpha = 0.15f + sin(time * 0.3f).toFloat() * 0.05f

        // 倒影的桥面
        drawRect(
            color = Color(0xFFA1887F).copy(alpha = reflectionAlpha),
            topLeft = Offset(bridgeX - 35f, reflectionY),
            size = Size(70f, 4f)
        )
        // 倒影的柱子
        for (i in -30..30 step 20) {
            drawRect(
                color = Color(0xFF795548).copy(alpha = reflectionAlpha * 0.8f),
                topLeft = Offset(bridgeX + i - 1.5f, reflectionY + 4f),
                size = Size(3f, 8f)
            )
        }
        // 波纹扰动
        val rippleCount = 3
        for (i in 0 until rippleCount) {
            val rippleX = bridgeX + sin(time * 0.4f + i * 2f).toFloat() * 20f
            val rippleY = reflectionY + 10f + i * 5f
            val rippleRadius = 5f + sin(time * 0.5f + i).toFloat() * 2f
            drawCircle(
                color = Color(0xFFB3E5FC).copy(alpha = 0.1f),
                radius = rippleRadius,
                center = Offset(rippleX, rippleY),
                style = Stroke(width = 0.8f)
            )
        }
    }
}

/** 根据时间绘制水面反光 */
private fun DrawScope.drawWaterReflection(timeOfDay: Float, time: Float, oceanY: Float) {
    val isNightTime = isNight(timeOfDay)
    val isGolden = isGoldenHour(timeOfDay)

    when {
        // 日出日落金色反光
        isGolden -> {
            val reflectCount = 8
            for (i in 0 until reflectCount) {
                val rx = size.width * (0.15f + i * 0.1f)
                val ry = oceanY + 30f + sin(time * 0.3f + i * 1.2f).toFloat() * 8f
                val rAlpha = 0.2f + sin(time * 0.5f + i).toFloat() * 0.1f
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = rAlpha),
                    radius = 3f + sin(time * 0.4f + i * 0.8f).toFloat() * 1.5f,
                    center = Offset(rx, ry)
                )
            }
        }
        // 夜间月光反光
        isNightTime -> {
            val reflectCount = 5
            for (i in 0 until reflectCount) {
                val rx = size.width * (0.3f + i * 0.1f)
                val ry = oceanY + 25f + sin(time * 0.2f + i * 1.5f).toFloat() * 6f
                val rAlpha = 0.1f + sin(time * 0.3f + i * 2f).toFloat() * 0.08f
                drawCircle(
                    color = Color(0xFFCFD8DC).copy(alpha = rAlpha),
                    radius = 2f + sin(time * 0.25f + i).toFloat() * 1f,
                    center = Offset(rx, ry)
                )
            }
        }
        // 白天微弱阳光反光
        else -> {
            val reflectCount = 4
            for (i in 0 until reflectCount) {
                val rx = size.width * (0.25f + i * 0.15f)
                val ry = oceanY + 35f + sin(time * 0.35f + i).toFloat() * 5f
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = 0.12f),
                    radius = 2f,
                    center = Offset(rx, ry)
                )
            }
        }
    }
}

/** 绘制海岸线泡沫效果 */
private fun DrawScope.drawFoamEffect(oceanY: Float, time: Float, waveAmplitude: Float) {
    val foamColor = Color.White.copy(alpha = 0.4f)
    val foamY = oceanY + 18f // 泡沫位于海浪顶部

    // 沿海岸线分布的泡沫点
    for (i in 0..25) {
        val baseX = size.width * (i / 25f)
        // 泡沫随波浪起伏
        val wavePhase = sin(baseX * 0.015f + time * 0.5f).toFloat()
        val foamOffsetY = wavePhase * waveAmplitude * 0.5f
        val foamX = baseX + sin(time * 0.3f + i * 0.7f).toFloat() * 3f
        val foamCurrentY = foamY + foamOffsetY

        // 泡沫圆点 (大小随机变化)
        val foamSize = 2f + (i % 3) * 1.5f + sin(time * 0.4f + i * 1.2f).toFloat() * 1f
        val foamAlpha = 0.25f + sin(time * 0.5f + i * 0.9f).toFloat() * 0.15f

        drawCircle(
            color = foamColor.copy(alpha = foamAlpha),
            radius = foamSize,
            center = Offset(foamX, foamCurrentY)
        )

        // 部分泡沫带小气泡群
        if (i % 3 == 0) {
            // 主气泡
            drawCircle(
                color = foamColor.copy(alpha = foamAlpha * 0.6f),
                radius = foamSize * 0.5f,
                center = Offset(foamX + 4f, foamCurrentY - 2f)
            )
            // 小气泡
            drawCircle(
                color = foamColor.copy(alpha = foamAlpha * 0.4f),
                radius = foamSize * 0.3f,
                center = Offset(foamX + 6f, foamCurrentY - 3f)
            )
            drawCircle(
                color = foamColor.copy(alpha = foamAlpha * 0.3f),
                radius = foamSize * 0.25f,
                center = Offset(foamX + 3f, foamCurrentY - 4f)
            )
        }

        // 部分泡沫带细小水珠
        if (i % 4 == 1) {
            val dropCount = 2 + (i % 2)
            for (d in 0 until dropCount) {
                val dropX = foamX + (d - 1) * 2f + sin(time + d) * 1f
                val dropY = foamCurrentY - 3f - d * 1.5f
                drawCircle(
                    color = foamColor.copy(alpha = foamAlpha * 0.35f),
                    radius = 0.6f,
                    center = Offset(dropX, dropY)
                )
            }
        }
    }
}

// Layer 2: 岛屿地形
private fun DrawScope.drawTerrainLayer(lushness: Float) {
    val terrainY = size.height * 0.45f
    val terrainHeight = size.height * 0.25f

    // 岛屿主体 - 使用多段贝塞尔曲线实现更自然的海岸线
    val terrainPath = Path().apply {
        moveTo(0f, terrainY + terrainHeight)
        // 左侧海岸线 - 多段贝塞尔
        cubicTo(
            size.width * 0.08f, terrainY + terrainHeight * 0.8f,
            size.width * 0.12f, terrainY + terrainHeight * 0.5f,
            size.width * 0.18f, terrainY + terrainHeight * 0.3f
        )
        cubicTo(
            size.width * 0.22f, terrainY + terrainHeight * 0.15f,
            size.width * 0.28f, terrainY - 10f,
            size.width * 0.35f, terrainY + 5f
        )
        // 中间起伏
        cubicTo(
            size.width * 0.42f, terrainY + 18f,
            size.width * 0.46f, terrainY - 15f,
            size.width * 0.5f, terrainY + 8f
        )
        // 右侧海岸线
        cubicTo(
            size.width * 0.54f, terrainY + 25f,
            size.width * 0.6f, terrainY + 12f,
            size.width * 0.68f, terrainY + 22f
        )
        cubicTo(
            size.width * 0.75f, terrainY + 30f,
            size.width * 0.82f, terrainY + 18f,
            size.width * 0.88f, terrainY + terrainHeight * 0.6f
        )
        cubicTo(
            size.width * 0.92f, terrainY + terrainHeight * 0.75f,
            size.width * 0.96f, terrainY + terrainHeight * 0.85f,
            size.width, terrainY + terrainHeight
        )
        close()
    }

    // 土壤渐变
    drawPath(
        path = terrainPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF9E8E7E).copy(alpha = 0.85f),
                Color(0xFF8D6E63).copy(alpha = 0.8f),
                Color(0xFF6D4C41).copy(alpha = 0.75f)
            ),
            startY = terrainY - 10f,
            endY = terrainY + terrainHeight
        ),
        style = Fill
    )

    // 沙滩纹理 (小点点 - 更密集更自然)
    val sandColor = Color(0xFFD7CCC8).copy(alpha = 0.35f)
    val sandDarkColor = Color(0xFFBCAAA4).copy(alpha = 0.25f)
    for (i in 0..50) {
        val dotX = size.width * (i / 50f) + sin(i * 2.3f) * 12f
        val dotY = terrainY + terrainHeight * 0.5f + sin(i * 1.7f) * terrainHeight * 0.35f
        val dotSize = 1f + (i % 4) * 0.4f
        val dotColor = if (i % 3 == 0) sandDarkColor else sandColor
        drawCircle(
            color = dotColor,
            radius = dotSize,
            center = Offset(dotX, dotY)
        )
    }

    // 草地 - 渐变填充
    val grassColor = Color(0xFF66BB6A).copy(alpha = 0.5f + lushness * 0.5f)
    val grassPath = Path().apply {
        moveTo(0f, terrainY + terrainHeight)
        // 左侧草地边缘
        cubicTo(
            size.width * 0.1f, terrainY + terrainHeight * 0.7f,
            size.width * 0.15f, terrainY + terrainHeight * 0.4f,
            size.width * 0.2f, terrainY + 15f
        )
        // 中间起伏
        cubicTo(
            size.width * 0.28f, terrainY + 5f,
            size.width * 0.35f, terrainY - 8f,
            size.width * 0.42f, terrainY + 10f
        )
        cubicTo(
            size.width * 0.48f, terrainY + 20f,
            size.width * 0.52f, terrainY + 5f,
            size.width * 0.58f, terrainY + 18f
        )
        // 右侧草地
        cubicTo(
            size.width * 0.65f, terrainY + 30f,
            size.width * 0.72f, terrainY + 20f,
            size.width * 0.8f, terrainY + terrainHeight * 0.5f
        )
        cubicTo(
            size.width * 0.88f, terrainY + terrainHeight * 0.7f,
            size.width * 0.95f, terrainY + terrainHeight * 0.85f,
            size.width, terrainY + terrainHeight
        )
        close()
    }
    drawPath(
        path = grassPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                grassColor,
                grassColor.copy(alpha = grassColor.alpha * 0.85f),
                grassColor.copy(alpha = grassColor.alpha * 0.6f)
            ),
            startY = terrainY - 10f,
            endY = terrainY + terrainHeight * 0.8f
        ),
        style = Fill
    )

    // 草地边缘草叶细节
    if (lushness > 0.3f) {
        val bladeColor = Color(0xFF4CAF50).copy(alpha = 0.5f + lushness * 0.3f)
        val bladeColor2 = Color(0xFF81C784).copy(alpha = 0.4f + lushness * 0.3f)
        for (i in 0..20) {
            val bladeX = size.width * (0.08f + i * 0.045f)
            val bladeBaseY = terrainY + 15f + sin(bladeX * 0.012f) * 10f
            val bladeHeight = 6f + lushness * 10f + sin(i * 1.5f) * 4f
            val bladeSway = sin(i * 0.8f) * 2.5f
            val bladeWidth = 1.2f + (i % 2) * 0.4f

            // 主草叶
            drawLine(
                color = bladeColor,
                start = Offset(bladeX, bladeBaseY),
                end = Offset(bladeX + bladeSway, bladeBaseY - bladeHeight),
                strokeWidth = bladeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            // 次草叶 (更短更细)
            if (i % 2 == 0) {
                drawLine(
                    color = bladeColor2,
                    start = Offset(bladeX + 2f, bladeBaseY),
                    end = Offset(bladeX + 2f + bladeSway * 0.7f, bladeBaseY - bladeHeight * 0.6f),
                    strokeWidth = bladeWidth * 0.7f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

// Layer 3: 植被
private fun DrawScope.drawVegetationLayer(lushness: Float, brightness: Float) {
    val baseX = size.width * 0.3f
    val baseY = size.height * 0.4f

    val treeCount = (lushness * 5).toInt() + 1

    for (i in 0 until treeCount) {
        val x = baseX + i * (size.width * 0.1f)
        val y = baseY + (i % 2) * 20f
        val treeHeight = 40f + lushness * 30f

        // 树干 - 带粗细变化
        val trunkPath = Path().apply {
            moveTo(x - 3.5f, y + treeHeight * 0.4f)
            lineTo(x - 2.5f, y)
            lineTo(x + 2.5f, y)
            lineTo(x + 3.5f, y + treeHeight * 0.4f)
            close()
        }
        drawPath(
            path = trunkPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6D4C41),
                    Color(0xFF795548),
                    Color(0xFF8D6E63)
                ),
                startY = y,
                endY = y + treeHeight * 0.4f
            )
        )

        // 树冠 - 多层圆形叠加，创造有机形状和深度感
        val crownColor = Color(0xFF4CAF50).copy(alpha = 0.6f + brightness * 0.4f)
        val darkCrown = Color(0xFF388E3C).copy(alpha = 0.5f + brightness * 0.3f)
        val lightCrown = Color(0xFF66BB6A).copy(alpha = 0.55f + brightness * 0.35f)
        val deepCrown = Color(0xFF2E7D32).copy(alpha = 0.4f + brightness * 0.2f)

        // 最底层阴影
        drawCircle(
            color = deepCrown,
            radius = treeHeight * 0.35f,
            center = Offset(x, y - treeHeight * 0.1f)
        )
        // 底层暗色
        drawCircle(
            color = darkCrown,
            radius = treeHeight * 0.32f,
            center = Offset(x, y - treeHeight * 0.15f)
        )
        // 主体
        drawCircle(
            color = crownColor,
            radius = treeHeight * 0.28f,
            center = Offset(x, y - treeHeight * 0.25f)
        )
        // 左侧
        drawCircle(
            color = crownColor,
            radius = treeHeight * 0.2f,
            center = Offset(x - treeHeight * 0.15f, y - treeHeight * 0.18f)
        )
        // 右侧
        drawCircle(
            color = crownColor,
            radius = treeHeight * 0.22f,
            center = Offset(x + treeHeight * 0.15f, y - treeHeight * 0.2f)
        )
        // 左上小丛
        drawCircle(
            color = crownColor.copy(alpha = crownColor.alpha * 0.8f),
            radius = treeHeight * 0.16f,
            center = Offset(x - treeHeight * 0.1f, y - treeHeight * 0.32f)
        )
        // 右上小丛
        drawCircle(
            color = crownColor.copy(alpha = crownColor.alpha * 0.85f),
            radius = treeHeight * 0.18f,
            center = Offset(x + treeHeight * 0.12f, y - treeHeight * 0.3f)
        )
        // 顶部亮色
        drawCircle(
            color = lightCrown,
            radius = treeHeight * 0.15f,
            center = Offset(x - treeHeight * 0.05f, y - treeHeight * 0.35f)
        )
        // 最顶部高光
        drawCircle(
            color = Color(0xFFA5D6A7).copy(alpha = 0.35f + brightness * 0.2f),
            radius = treeHeight * 0.08f,
            center = Offset(x + treeHeight * 0.02f, y - treeHeight * 0.38f)
        )

        // 树叶纹理感 (细小的叶脉线条)
        if (lushness > 0.4f) {
            val leafLineColor = Color(0xFF388E3C).copy(alpha = 0.15f)
            for (j in 0..4) {
                val leafAngle = (j * 72f + i * 30f)
                val leafRad = Math.toRadians(leafAngle.toDouble()).toFloat()
                val leafLen = treeHeight * 0.12f
                val leafCx = x + cos(leafRad) * treeHeight * 0.1f
                val leafCy = y - treeHeight * 0.25f + sin(leafRad) * treeHeight * 0.08f
                drawLine(
                    color = leafLineColor,
                    start = Offset(leafCx, leafCy),
                    end = Offset(leafCx + cos(leafRad) * leafLen, leafCy + sin(leafRad) * leafLen * 0.5f),
                    strokeWidth = 0.8f
                )
            }
        }
    }

    // 花朵 - 带花瓣细节
    if (lushness > 0.5f) {
        val flowerCount = ((lushness - 0.5f) * 12).toInt()
        val flowerColors = listOf(
            Color(0xFFFF80AB),
            Color(0xFFCE93D8),
            Color(0xFFFFB74D),
            Color(0xFFFFF176),
            Color(0xFF80DEEA),
            Color(0xFFFFAB91)
        )
        for (i in 0 until flowerCount) {
            val x = baseX + i * (size.width * 0.07f) + 50f
            val y = baseY + 30f + (i % 3) * 18f
            val flowerColor = flowerColors[i % flowerColors.size]
            val flowerSize = 2f + (i % 3) * 0.5f

            // 花瓣 (5-6片，更自然的形状)
            val petalCount = 5 + (i % 2)
            for (p in 0 until petalCount) {
                val petalAngle = (360f / petalCount) * p
                val petalRad = Math.toRadians(petalAngle.toDouble()).toFloat()
                val petalDist = flowerSize * 1.5f
                val petalX = x + cos(petalRad) * petalDist
                val petalY = y + sin(petalRad) * petalDist

                // 花瓣形状 (用椭圆)
                val petalW = flowerSize * 0.9f
                val petalH = flowerSize * 1.3f
                drawOval(
                    color = flowerColor.copy(alpha = 0.75f),
                    topLeft = Offset(petalX - petalW, petalY - petalH),
                    size = Size(petalW * 2, petalH * 2)
                )
                // 花瓣高光
                drawOval(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(petalX - petalW * 0.5f, petalY - petalH * 0.5f),
                    size = Size(petalW, petalH)
                )
            }
            // 花心
            drawCircle(
                color = Color(0xFFFFF176),
                radius = flowerSize * 0.7f,
                center = Offset(x, y)
            )
            // 花心高光
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = flowerSize * 0.3f,
                center = Offset(x - flowerSize * 0.15f, y - flowerSize * 0.15f)
            )
        }
    }
}

// Layer 4: 建筑/装饰
private fun DrawScope.drawBuildingLayer(
    decorations: List<IslandDecoration>,
    windmillBladeAngle: Float,
    activeCombos: List<ComboDefinition>,
    time: Float
) {
    val comboIds = activeCombos.map { it.id }

    decorations.filter { it.type == "building" }.forEach { decoration ->
        val x = size.width * decoration.posX
        val y = size.height * decoration.posY

        // 统一阴影
        drawShadow(x, y, 30f, 10f)

        when (decoration.name) {
            "小木屋" -> {
                drawCabin(x, y, hasWarmHomeCombo = "warm_home" in comboIds, time = time)
            }
            "灯塔" -> drawLighthouse(x, y, hasWatchtowerCombo = "watchtower" in comboIds, time = time)
            "风车" -> drawWindmill(x, y, windmillBladeAngle, hasWindValleyCombo = "wind_valley" in comboIds)
            "桥梁" -> drawBridge(x, y, hasWatchtowerCombo = "watchtower" in comboIds, time = time)
            "喷泉" -> drawFountain(x, y)
            "守护者雕像" -> drawStatue(x, y)
            else -> drawGenericBuilding(x, y)
        }
    }
}

private fun DrawScope.drawCabin(x: Float, y: Float, hasWarmHomeCombo: Boolean = false, time: Float = 0f) {
    // 温馨家园组合效果: 小木屋周围花丛 + 暖黄灯光
    if (hasWarmHomeCombo) {
        // 绘制周围的花丛
        val flowerColors = listOf(
            Color(0xFFFF80AB),
            Color(0xFFFF80AB).copy(alpha = 0.8f),
            Color(0xFFCE93D8),
            Color(0xFFCE93D8).copy(alpha = 0.8f),
            Color(0xFFFFB74D),
            Color(0xFFFFB74D).copy(alpha = 0.8f)
        )
        for (i in 0..11) {
            val angle = i * 30f
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val dist = 35f + (i % 3) * 8f
            val fx = x + cos(rad) * dist
            val fy = y + 5f + sin(rad) * dist * 0.4f
            val flowerColor = flowerColors[i % flowerColors.size]
            val pulse = 0.7f + sin(time * 0.5f + i * 0.8f).toFloat() * 0.3f
            drawCircle(
                color = flowerColor.copy(alpha = pulse),
                radius = 4f + (i % 2) * 2f,
                center = Offset(fx, fy)
            )
        }
        // 飘过的蝴蝶
        for (i in 0..1) {
            val butterflyX = x + sin(time * 0.3f + i * 3.14f).toFloat() * 40f
            val butterflyY = y - 30f + cos(time * 0.4f + i * 2f).toFloat() * 15f
            val butterflyAlpha = 0.5f + sin(time * 0.6f + i).toFloat() * 0.3f
            drawOval(
                color = Color(0xFFCE93D8).copy(alpha = butterflyAlpha),
                topLeft = Offset(butterflyX - 3f, butterflyY - 2f),
                size = Size(6f, 4f)
            )
        }
    }

    // 屋顶 (三角形)
    val roofPath = Path().apply {
        moveTo(x - 25f, y - 20f)
        lineTo(x, y - 50f)
        lineTo(x + 25f, y - 20f)
        close()
    }
    drawPath(roofPath, color = Color(0xFFE57373), style = Fill)

    // 屋顶纹理线条
    val roofLineColor = Color(0xFFEF9A9A).copy(alpha = 0.4f)
    for (i in 1..3) {
        val roofY = y - 20f - i * 7f
        val roofLeftX = x - 25f + i * 6f
        val roofRightX = x + 25f - i * 6f
        drawLine(
            color = roofLineColor,
            start = Offset(roofLeftX, roofY),
            end = Offset(roofRightX, roofY),
            strokeWidth = 1f
        )
    }

    // 烟囱
    drawRect(
        color = Color(0xFF8D6E63),
        topLeft = Offset(x + 12f, y - 45f),
        size = Size(8f, 15f)
    )
    // 烟囱顶部
    drawRect(
        color = Color(0xFF6D4C41),
        topLeft = Offset(x + 11f, y - 47f),
        size = Size(10f, 3f)
    )

    // 烟囱烟雾动画 (3缕烟雾，贝塞尔曲线)
    val smokeColor = Color(0xFFE0E0E0).copy(alpha = 0.3f)
    for (i in 0..2) {
        val smokePhase = (time * 0.8f + i * 2f) % 4f
        if (smokePhase < 3f) {
            val smokeX = x + 16f
            val smokeBaseY = y - 47f
            val smokeRise = smokePhase * 12f
            val smokeDrift = sin(smokePhase * 1.5f + i) * 5f
            val smokeAlpha = (1f - smokePhase / 3f) * 0.25f

            val smokePath = Path().apply {
                moveTo(smokeX, smokeBaseY)
                cubicTo(
                    smokeX + smokeDrift * 0.3f, smokeBaseY - smokeRise * 0.3f,
                    smokeX - smokeDrift * 0.5f, smokeBaseY - smokeRise * 0.6f,
                    smokeX + smokeDrift, smokeBaseY - smokeRise
                )
            }
            drawPath(
                path = smokePath,
                color = smokeColor.copy(alpha = smokeAlpha),
                style = Stroke(width = 2.5f - smokePhase * 0.3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }

    // 墙壁
    drawRect(
        color = Color(0xFFD7CCC8),
        topLeft = Offset(x - 20f, y - 20f),
        size = Size(40f, 30f)
    )

    // 木纹线条 (水平纹理)
    val woodLineColor = Color(0xFFBCAAA4).copy(alpha = 0.3f)
    for (i in 0..4) {
        val woodY = y - 18f + i * 6f
        drawLine(
            color = woodLineColor,
            start = Offset(x - 19f, woodY),
            end = Offset(x + 19f, woodY),
            strokeWidth = 0.8f
        )
    }
    // 竖向木纹
    for (i in 0..2) {
        val woodX = x - 12f + i * 12f
        drawLine(
            color = woodLineColor,
            start = Offset(woodX, y - 19f),
            end = Offset(woodX, y + 9f),
            strokeWidth = 0.6f
        )
    }

    // 门
    drawRect(
        color = Color(0xFF8D6E63),
        topLeft = Offset(x - 5f, y - 5f),
        size = Size(10f, 15f)
    )
    // 门把手
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = 1.2f,
        center = Offset(x + 3f, y + 4f)
    )

    // 窗户 (温馨家园组合: 暖黄灯光 + 更亮)
    val windowColor = if (hasWarmHomeCombo) {
        Color(0xFFFFAB40) // 暖黄色
    } else {
        Color(0xFFFFF176)
    }
    // 左窗户
    drawRect(
        color = windowColor,
        topLeft = Offset(x - 17f, y - 15f),
        size = Size(9f, 9f)
    )
    // 左窗户窗格 (十字分隔)
    drawLine(
        color = Color(0xFF8D6E63),
        start = Offset(x - 12.5f, y - 15f),
        end = Offset(x - 12.5f, y - 6f),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0xFF8D6E63),
        start = Offset(x - 17f, y - 10.5f),
        end = Offset(x - 8f, y - 10.5f),
        strokeWidth = 1f
    )
    // 窗户边框
    drawRect(
        color = Color(0xFF8D6E63),
        topLeft = Offset(x - 17f, y - 15f),
        size = Size(9f, 9f),
        style = Stroke(width = 1f)
    )

    // 右窗户
    drawRect(
        color = windowColor,
        topLeft = Offset(x + 8f, y - 15f),
        size = Size(9f, 9f)
    )
    // 右窗户窗格
    drawLine(
        color = Color(0xFF8D6E63),
        start = Offset(x + 12.5f, y - 15f),
        end = Offset(x + 12.5f, y - 6f),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0xFF8D6E63),
        start = Offset(x + 8f, y - 10.5f),
        end = Offset(x + 17f, y - 10.5f),
        strokeWidth = 1f
    )
    // 窗户边框
    drawRect(
        color = Color(0xFF8D6E63),
        topLeft = Offset(x + 8f, y - 15f),
        size = Size(9f, 9f),
        style = Stroke(width = 1f)
    )

    // 温馨家园组合: 窗户光晕
    if (hasWarmHomeCombo) {
        drawCircle(
            color = Color(0xFFFFAB40).copy(alpha = 0.2f),
            radius = 15f,
            center = Offset(x - 12.5f, y - 10.5f)
        )
        drawCircle(
            color = Color(0xFFFFAB40).copy(alpha = 0.2f),
            radius = 15f,
            center = Offset(x + 12.5f, y - 10.5f)
        )
    }
}

private fun DrawScope.drawLighthouse(x: Float, y: Float, hasWatchtowerCombo: Boolean = false, time: Float = 0f) {
    // 守望灯塔组合效果: 灯塔光芒照亮桥面
    if (hasWatchtowerCombo) {
        // 扩大光晕范围
        val beamAngle = time * 0.3f
        for (i in 0..2) {
            val beamX = x + cos(beamAngle + i * 2.094f).toFloat() * 80f
            val beamY = y - 58f + sin(beamAngle + i * 2.094f).toFloat() * 20f
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = 0.15f),
                radius = 25f,
                center = Offset(beamX, beamY)
            )
        }
    }

    // 主体 (细长梯形)
    val bodyPath = Path().apply {
        moveTo(x - 8f, y + 10f)
        lineTo(x + 8f, y + 10f)
        lineTo(x + 6f, y - 55f)
        lineTo(x - 6f, y - 55f)
        close()
    }
    drawPath(bodyPath, color = Color(0xFFE0E0E0), style = Fill)

    // 灯塔纹理线条
    val towerLineColor = Color(0xFFBDBDBD).copy(alpha = 0.3f)
    for (i in 1..4) {
        val lineY = y + 10f - i * 15f
        drawLine(
            color = towerLineColor,
            start = Offset(x - 7.5f + i * 0.3f, lineY),
            end = Offset(x + 7.5f - i * 0.3f, lineY),
            strokeWidth = 0.8f
        )
    }

    // 红色条纹
    drawRect(
        color = Color(0xFFE57373),
        topLeft = Offset(x - 7f, y - 15f),
        size = Size(14f, 8f)
    )
    drawRect(
        color = Color(0xFFE57373),
        topLeft = Offset(x - 6f, y - 40f),
        size = Size(12f, 8f)
    )

    // 顶部平台
    drawRect(
        color = Color(0xFF9E9E9E),
        topLeft = Offset(x - 8f, y - 57f),
        size = Size(16f, 3f)
    )

    // 顶部圆灯
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = 8f,
        center = Offset(x, y - 60f)
    )
    // 光晕
    drawCircle(
        color = Color(0xFFFFD54F).copy(alpha = 0.25f),
        radius = 18f,
        center = Offset(x, y - 60f)
    )
    // 灯光放射线
    for (i in 0..5) {
        val rayAngle = (360f / 6) * i + time * 30f
        val rayRad = Math.toRadians(rayAngle.toDouble()).toFloat()
        drawLine(
            color = Color(0xFFFFD54F).copy(alpha = 0.15f),
            start = Offset(x + cos(rayRad) * 9f, y - 60f + sin(rayRad) * 9f),
            end = Offset(x + cos(rayRad) * 16f, y - 60f + sin(rayRad) * 16f),
            strokeWidth = 1.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

private fun DrawScope.drawWindmill(x: Float, y: Float, bladeAngle: Float, hasWindValleyCombo: Boolean = false) {
    // 底座
    val basePath = Path().apply {
        moveTo(x - 12f, y + 10f)
        lineTo(x + 12f, y + 10f)
        lineTo(x + 8f, y - 35f)
        lineTo(x - 8f, y - 35f)
        close()
    }
    drawPath(basePath, color = Color(0xFFBCAAA4), style = Fill)

    // 顶部
    drawCircle(
        color = Color(0xFF8D6E63),
        radius = 6f,
        center = Offset(x, y - 35f)
    )

    // 4片叶片
    val bladeLength = 30f
    val centerPt = Offset(x, y - 35f)

    for (i in 0..3) {
        val angle = bladeAngle + i * 90f
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val tipX = centerPt.x + sin(rad) * bladeLength
        val tipY = centerPt.y - sin(rad + Math.PI / 2f).toFloat() * bladeLength
        drawLine(
            color = Color(0xFFD7CCC8),
            start = centerPt,
            end = Offset(tipX, tipY),
            strokeWidth = 4f
        )
    }

    // 风之谷组合效果: 风车带动树叶飘落
    if (hasWindValleyCombo) {
        val leafColors = listOf(
            Color(0xFF66BB6A).copy(alpha = 0.7f),
            Color(0xFF81C784).copy(alpha = 0.65f),
            Color(0xFFA5D6A7).copy(alpha = 0.6f)
        )
        for (i in 0..5) {
            val leafPhase = bladeAngle * 0.5f + i * 1.5f
            val leafX = x + cos(leafPhase * 0.1f).toFloat() * (30f + i * 10f)
            val leafY = y - 20f + (leafPhase * 0.5f) % 60f
            val leafColor = leafColors[i % leafColors.size]
            val leafSize = 3f + (i % 2) * 2f
            drawCircle(
                color = leafColor,
                radius = leafSize,
                center = Offset(leafX, leafY)
            )
        }
    }
}

private fun DrawScope.drawBridge(x: Float, y: Float, hasWatchtowerCombo: Boolean = false, time: Float = 0f) {
    // 水平木板
    drawRect(
        color = Color(0xFFA1887F),
        topLeft = Offset(x - 35f, y - 3f),
        size = Size(70f, 6f)
    )

    // 木板纹理 (线条)
    for (i in -30..30 step 10) {
        drawRect(
            color = Color(0xFF8D6E63),
            topLeft = Offset(x + i - 1f, y - 3f),
            size = Size(2f, 6f)
        )
    }

    // 小柱子
    for (i in -30..30 step 20) {
        drawRect(
            color = Color(0xFF795548),
            topLeft = Offset(x + i - 1.5f, y - 3f),
            size = Size(3f, 12f)
        )
    }

    // 守望灯塔组合效果: 灯塔光芒照亮桥面
    if (hasWatchtowerCombo) {
        val lightPulse = 0.3f + sin(time * 0.5f).toFloat() * 0.1f
        for (i in -25..25 step 10) {
            val lightX = x + i.toFloat()
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = lightPulse),
                radius = 4f,
                center = Offset(lightX, y)
            )
        }
    }
}

private fun DrawScope.drawFountain(x: Float, y: Float) {
    // 底座水池
    drawOval(
        color = Color(0xFF90CAF9),
        topLeft = Offset(x - 20f, y - 5f),
        size = Size(40f, 12f)
    )

    // 柱子
    drawRect(
        color = Color(0xFFBDBDBD),
        topLeft = Offset(x - 3f, y - 35f),
        size = Size(6f, 30f)
    )
    // 柱子纹理
    val pillarLineColor = Color(0xFF9E9E9E).copy(alpha = 0.3f)
    for (i in 0..2) {
        val pillarY = y - 33f + i * 10f
        drawLine(
            color = pillarLineColor,
            start = Offset(x - 2.5f, pillarY),
            end = Offset(x + 2.5f, pillarY),
            strokeWidth = 0.6f
        )
    }

    // 底座装饰边
    drawRect(
        color = Color(0xFF9E9E9E),
        topLeft = Offset(x - 22f, y - 6f),
        size = Size(44f, 2f)
    )

    // 顶部水花 (多层次)
    // 中心水柱
    drawRect(
        color = Color(0xFFB3E5FC).copy(alpha = 0.6f),
        topLeft = Offset(x - 1.5f, y - 42f),
        size = Size(3f, 10f)
    )
    // 水花散落
    for (i in 0..5) {
        val dropAngle = (360f / 6) * i
        val dropRad = Math.toRadians(dropAngle.toDouble()).toFloat()
        val dropDist = 6f + (i % 2) * 2f
        val dropX = x + cos(dropRad) * dropDist
        val dropY = y - 38f - sin(dropRad) * dropDist * 0.5f
        drawCircle(
            color = Color(0xFFB3E5FC).copy(alpha = 0.65f),
            radius = 2.5f + (i % 2) * 0.5f,
            center = Offset(dropX, dropY)
        )
        // 小水滴
        drawCircle(
            color = Color(0xFFE1F5FE).copy(alpha = 0.4f),
            radius = 1f,
            center = Offset(dropX + cos(dropRad) * 3f, dropY - 2f)
        )
    }
}

private fun DrawScope.drawStatue(x: Float, y: Float) {
    // 底座
    drawRect(
        color = Color(0xFF9E9E9E),
        topLeft = Offset(x - 10f, y - 5f),
        size = Size(20f, 10f)
    )

    // 身体 (简化几何形)
    val bodyPath = Path().apply {
        moveTo(x - 8f, y - 5f)
        lineTo(x + 8f, y - 5f)
        lineTo(x + 5f, y - 35f)
        lineTo(x - 5f, y - 35f)
        close()
    }
    drawPath(bodyPath, color = Color(0xFFBDBDBD), style = Fill)

    // 头部
    drawCircle(
        color = Color(0xFFBDBDBD),
        radius = 6f,
        center = Offset(x, y - 40f)
    )

    // 星形装饰
    drawStar(x, y - 30f, 5f, Color(0xFFFFD54F))
}

private fun DrawScope.drawGenericBuilding(x: Float, y: Float) {
    drawRect(
        color = Color(0xFFBCAAA4),
        topLeft = Offset(x - 15f, y - 20f),
        size = Size(30f, 25f)
    )
    drawRect(
        color = Color(0xFFE57373),
        topLeft = Offset(x - 18f, y - 28f),
        size = Size(36f, 12f)
    )
}

// Layer 5: 动物 (行为系统)
private fun DrawScope.drawAnimalLayer(
    activeAnimals: List<IslandAnimal>,
    decorations: List<IslandDecoration>,
    time: Float,
    timeOfDay: Float
) {
    activeAnimals.forEach { animal ->
        val px = size.width * animal.x
        val py = size.height * animal.y

        when (animal.type) {
            AnimalType.BIRD -> drawBirdWithBehavior(px, py, animal.behavior, time, animal.scale, animal.alpha)
            AnimalType.BUTTERFLY -> drawButterflyWithBehavior(px, py, animal.behavior, time, animal.alpha)
            AnimalType.SQUIRREL -> drawSquirrelWithBehavior(px, py, animal.behavior, time, animal.alpha)
            AnimalType.OWL -> drawOwlWithBehavior(px, py, animal.behavior, time, animal.alpha)
            AnimalType.CAT -> drawCatWithBehavior(px, py, animal.behavior, time, animal.flipX, animal.alpha)
            AnimalType.FROG -> drawFrogWithBehavior(px, py, animal.behavior, time, animal.alpha)
            AnimalType.FIREFLY -> drawFireflyWithBehavior(px, py, time, animal.scale)
        }
    }

    // 巨龙仍从装饰列表绘制
    decorations.filter { it.type == "animal" && it.name == "巨龙" }.forEach { decoration ->
        val baseX = size.width * decoration.posX
        val baseY = size.height * decoration.posY
        drawDragon(baseX, baseY, time)
    }
}

// --- 巨龙绘制 ---

private fun DrawScope.drawDragon(x: Float, y: Float, time: Float) {
    // 阴影
    drawShadow(x, y + 15f, 35f, 10f)

    // 身体
    drawOval(
        color = Color(0xFF7B1FA2).copy(alpha = 0.8f),
        topLeft = Offset(x - 20f, y - 5f),
        size = Size(40f, 18f)
    )

    // 头部
    drawCircle(
        color = Color(0xFF7B1FA2).copy(alpha = 0.8f),
        radius = 10f,
        center = Offset(x + 22f, y)
    )

    // 眼睛
    drawCircle(color = Color(0xFFFFD54F), radius = 3f, center = Offset(x + 25f, y - 2f))
    drawCircle(color = Color(0xFF212121), radius = 1.5f, center = Offset(x + 25f, y - 2f))

    // 翅膀 (动画)
    val wingSpread = 12f + sin(time * 0.7f).toFloat() * 5f
    val leftWingPath = Path().apply {
        moveTo(x - 5f, y - 5f)
        quadraticBezierTo(x - 15f, y - 5f - wingSpread, x - 20f, y - 8f)
    }
    drawPath(leftWingPath, color = Color(0xFF9C27B0).copy(alpha = 0.7f), style = Stroke(width = 3f))

    val rightWingPath = Path().apply {
        moveTo(x + 5f, y - 5f)
        quadraticBezierTo(x + 15f, y - 5f - wingSpread, x + 20f, y - 8f)
    }
    drawPath(rightWingPath, color = Color(0xFF9C27B0).copy(alpha = 0.7f), style = Stroke(width = 3f))

    // 火焰
    drawCircle(
        color = Color(0xFFFF6F00).copy(alpha = 0.4f),
        radius = 5f,
        center = Offset(x + 30f, y + 2f)
    )
}

// --- 小鸟行为绘制 ---

private fun DrawScope.drawBirdWithBehavior(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, scale: Float, alpha: Float
) {
    val s = scale
    val birdColor = Color(0xFF5D4037).copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.FLYING -> {
            val animX = x + sin(time * 0.3f).toFloat() * size.width * 0.05f * s
            val animY = y + sin(time * 0.5f).toFloat() * 8f * s
            val wingFlap = sin(time * 2f).toFloat() * 8f * s

            drawShadow(animX, animY, 12f * s, 4f)

            val leftWing = Path().apply {
                moveTo(animX, animY)
                quadraticBezierTo(animX - 8f * s, animY - 10f * s - wingFlap, animX - 15f * s, animY - 2f * s)
            }
            drawPath(leftWing, color = birdColor, style = Stroke(width = 2.5f * s))

            val rightWing = Path().apply {
                moveTo(animX, animY)
                quadraticBezierTo(animX + 8f * s, animY - 10f * s - wingFlap, animX + 15f * s, animY - 2f * s)
            }
            drawPath(rightWing, color = birdColor, style = Stroke(width = 2.5f * s))

            drawOval(
                color = birdColor,
                topLeft = Offset(animX - 3f * s, animY - 2f * s),
                size = Size(6f * s, 4f * s)
            )
        }
        AnimalBehavior.HOPPING -> {
            val hopPhase = (time * 2f) % (2 * Math.PI.toFloat())
            val hopY = if (hopPhase < Math.PI.toFloat()) -sin(hopPhase.toDouble()).toFloat() * 6f * s else 0f
            val animX = x + sin(time * 0.4f).toFloat() * size.width * 0.02f * s
            val animY = y + hopY

            drawShadow(animX, y + 4f, 8f * s, 3f)

            drawOval(color = birdColor, topLeft = Offset(animX - 4f * s, animY - 3f * s), size = Size(8f * s, 6f * s))
            drawCircle(color = birdColor, radius = 3f * s, center = Offset(animX + 4f * s, animY - 3f * s))
            drawLine(
                color = Color(0xFFFFB74D).copy(alpha = alpha),
                start = Offset(animX + 7f * s, animY - 3f * s),
                end = Offset(animX + 10f * s, animY - 2f * s),
                strokeWidth = 1.5f
            )
        }
        AnimalBehavior.RESTING, AnimalBehavior.SLEEPING -> {
            val floatY = y + sin(time * 0.2f).toFloat() * 2f * s

            drawOval(color = birdColor, topLeft = Offset(x - 4f * s, floatY - 3f * s), size = Size(8f * s, 6f * s))
            drawCircle(color = birdColor, radius = 3f * s, center = Offset(x + 3f * s, floatY - 3f * s))

            if (behavior == AnimalBehavior.SLEEPING) {
                drawLine(
                    color = Color(0xFF212121).copy(alpha = alpha * 0.5f),
                    start = Offset(x + 4f * s, floatY - 3.5f * s),
                    end = Offset(x + 6f * s, floatY - 3.5f * s),
                    strokeWidth = 1f
                )
            }
        }
        AnimalBehavior.HIDING -> {
            val hideAlpha = alpha * 0.4f
            drawOval(
                color = birdColor.copy(alpha = hideAlpha),
                topLeft = Offset(x - 3f * s, y - 2f * s),
                size = Size(6f * s, 4f * s)
            )
        }
        else -> {
            drawOval(color = birdColor, topLeft = Offset(x - 4f * s, y - 3f * s), size = Size(8f * s, 6f * s))
        }
    }
}

// --- 蝴蝶行为绘制 ---

private fun DrawScope.drawButterflyWithBehavior(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    when (behavior) {
        AnimalBehavior.FLYING -> {
            val animX = x + sin(time * 0.4f).toFloat() * size.width * 0.03f
            val animY = y + sin(time * 0.6f).toFloat() * 6f
            val wingFlap = sin(time * 3f).toFloat() * 0.3f + 0.7f

            drawShadow(animX, animY + 10f, 8f, 3f)

            drawLine(
                color = Color(0xFF5D4037).copy(alpha = alpha),
                start = Offset(animX, animY - 5f),
                end = Offset(animX, animY + 5f),
                strokeWidth = 1.5f
            )

            val wingW = 8f * wingFlap
            drawOval(
                color = Color(0xFFCE93D8).copy(alpha = 0.8f * alpha),
                topLeft = Offset(animX - wingW, animY - 5f),
                size = Size(wingW, 10f)
            )
            drawOval(
                color = Color(0xFFBA68C8).copy(alpha = 0.6f * alpha),
                topLeft = Offset(animX - wingW * 0.7f, animY),
                size = Size(wingW * 0.7f, 7f)
            )

            drawOval(
                color = Color(0xFFCE93D8).copy(alpha = 0.8f * alpha),
                topLeft = Offset(animX, animY - 5f),
                size = Size(wingW, 10f)
            )
            drawOval(
                color = Color(0xFFBA68C8).copy(alpha = 0.6f * alpha),
                topLeft = Offset(animX, animY),
                size = Size(wingW * 0.7f, 7f)
            )
        }
        AnimalBehavior.RESTING -> {
            drawOval(
                color = Color(0xFFCE93D8).copy(alpha = 0.5f * alpha),
                topLeft = Offset(x - 2f, y - 4f),
                size = Size(4f, 8f)
            )
            drawLine(
                color = Color(0xFF5D4037).copy(alpha = alpha),
                start = Offset(x, y - 4f),
                end = Offset(x, y + 4f),
                strokeWidth = 1f
            )
        }
        AnimalBehavior.HIDING -> { /* 不可见 */ }
        else -> {
            drawOval(color = Color(0xFFCE93D8).copy(alpha = 0.6f * alpha), topLeft = Offset(x - 5f, y - 4f), size = Size(5f, 8f))
            drawOval(color = Color(0xFFCE93D8).copy(alpha = 0.6f * alpha), topLeft = Offset(x, y - 4f), size = Size(5f, 8f))
        }
    }
}

// --- 松鼠行为绘制 ---

private fun DrawScope.drawSquirrelWithBehavior(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    val bodyColor = Color(0xFFA1887F).copy(alpha = alpha)
    val tailColor = Color(0xFFBCAAA4).copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.CLIMBING -> {
            val climbY = y + sin(time * 0.3f).toFloat() * 8f

            drawShadow(x, climbY + 8f, 12f, 4f)
            drawOval(color = bodyColor, topLeft = Offset(x - 7f, climbY - 8f), size = Size(14f, 12f))
            drawCircle(color = bodyColor, radius = 5f, center = Offset(x + 5f, climbY - 10f))
            drawCircle(color = Color(0xFF8D6E63).copy(alpha = alpha), radius = 2.5f, center = Offset(x + 7f, climbY - 15f))
            drawCircle(color = Color(0xFF8D6E63).copy(alpha = alpha), radius = 2.5f, center = Offset(x + 10f, climbY - 14f))
            val tailPath = Path().apply {
                moveTo(x - 7f, climbY)
                quadraticBezierTo(x - 18f, climbY - 18f, x - 10f, climbY - 25f)
                quadraticBezierTo(x - 2f, climbY - 15f, x - 7f, climbY)
            }
            drawPath(tailPath, color = tailColor, style = Fill)
        }
        AnimalBehavior.SLEEPING -> {
            drawOval(color = bodyColor.copy(alpha = alpha * 0.6f), topLeft = Offset(x - 6f, y - 6f), size = Size(12f, 10f))
            val tailPath = Path().apply {
                moveTo(x - 5f, y - 2f)
                quadraticBezierTo(x - 12f, y - 12f, x - 6f, y - 14f)
                quadraticBezierTo(x, y - 8f, x - 5f, y - 2f)
            }
            drawPath(tailPath, color = tailColor.copy(alpha = alpha * 0.5f), style = Fill)
        }
        AnimalBehavior.HIDING -> {
            drawOval(color = bodyColor.copy(alpha = alpha * 0.3f), topLeft = Offset(x - 4f, y - 4f), size = Size(8f, 7f))
        }
        else -> {
            val idleX = x + sin(time * 0.5f).toFloat() * 3f
            drawShadow(idleX, y + 8f, 12f, 4f)
            drawOval(color = bodyColor, topLeft = Offset(idleX - 7f, y - 8f), size = Size(14f, 12f))
            drawCircle(color = bodyColor, radius = 5f, center = Offset(idleX + 5f, y - 10f))
            drawCircle(color = Color(0xFF8D6E63).copy(alpha = alpha), radius = 2.5f, center = Offset(idleX + 7f, y - 15f))
            drawCircle(color = Color(0xFF8D6E63).copy(alpha = alpha), radius = 2.5f, center = Offset(idleX + 10f, y - 14f))
            val tailPath = Path().apply {
                moveTo(idleX - 7f, y)
                quadraticBezierTo(idleX - 15f, y - 15f, idleX - 8f, y - 20f)
                quadraticBezierTo(idleX - 2f, y - 12f, idleX - 7f, y)
            }
            drawPath(tailPath, color = tailColor, style = Fill)
        }
    }
}

// --- 猫头鹰行为绘制 ---

private fun DrawScope.drawOwlWithBehavior(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    val bodyColor = Color(0xFF8D6E63).copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.FLYING -> {
            val flyX = x + sin(time * 0.2f).toFloat() * size.width * 0.06f
            val flyY = y + sin(time * 0.3f).toFloat() * 5f
            val wingSpread = 15f + sin(time * 1.5f).toFloat() * 8f

            drawShadow(flyX, flyY + 12f, 16f, 5f)
            drawOval(color = bodyColor, topLeft = Offset(flyX - 8f, flyY - 5f), size = Size(16f, 18f))
            drawCircle(color = bodyColor, radius = 8f, center = Offset(flyX, flyY - 10f))

            val leftWing = Path().apply {
                moveTo(flyX - 6f, flyY)
                quadraticBezierTo(flyX - 15f, flyY - wingSpread, flyX - 22f, flyY - 5f)
            }
            drawPath(leftWing, color = Color(0xFF6D4C41).copy(alpha = alpha), style = Stroke(width = 3f))

            val rightWing = Path().apply {
                moveTo(flyX + 6f, flyY)
                quadraticBezierTo(flyX + 15f, flyY - wingSpread, flyX + 22f, flyY - 5f)
            }
            drawPath(rightWing, color = Color(0xFF6D4C41).copy(alpha = alpha), style = Stroke(width = 3f))

            drawCircle(color = Color(0xFFFFF176).copy(alpha = alpha), radius = 4f, center = Offset(flyX - 4f, flyY - 11f))
            drawCircle(color = Color(0xFFFFF176).copy(alpha = alpha), radius = 4f, center = Offset(flyX + 4f, flyY - 11f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 2f, center = Offset(flyX - 4f, flyY - 11f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 2f, center = Offset(flyX + 4f, flyY - 11f))
        }
        AnimalBehavior.SLEEPING -> {
            val nodY = y + sin(time * 0.15f).toFloat() * 2f

            drawShadow(x, nodY + 10f, 14f, 5f)
            drawOval(color = bodyColor, topLeft = Offset(x - 8f, nodY - 5f), size = Size(16f, 18f))
            drawCircle(color = bodyColor, radius = 8f, center = Offset(x, nodY - 10f))

            val closedEye = Path().apply {
                moveTo(x - 7f, nodY - 11f)
                quadraticBezierTo(x - 4f, nodY - 9f, x - 1f, nodY - 11f)
            }
            drawPath(closedEye, color = Color(0xFF4E342E).copy(alpha = alpha), style = Stroke(width = 1.5f))
            val closedEye2 = Path().apply {
                moveTo(x + 1f, nodY - 11f)
                quadraticBezierTo(x + 4f, nodY - 9f, x + 7f, nodY - 11f)
            }
            drawPath(closedEye2, color = Color(0xFF4E342E).copy(alpha = alpha), style = Stroke(width = 1.5f))

            drawOwlEars(x, nodY, alpha)
            drawCircle(color = Color(0xFFFFB74D).copy(alpha = alpha), radius = 2f, center = Offset(x, nodY - 8f))
        }
        AnimalBehavior.HIDING -> {
            drawOval(color = bodyColor.copy(alpha = alpha * 0.3f), topLeft = Offset(x - 5f, y - 3f), size = Size(10f, 12f))
        }
        else -> {
            drawShadow(x, y + 10f, 14f, 5f)
            drawOval(color = bodyColor, topLeft = Offset(x - 8f, y - 5f), size = Size(16f, 18f))
            drawCircle(color = bodyColor, radius = 8f, center = Offset(x, y - 10f))
            drawCircle(color = Color(0xFFFFF176).copy(alpha = alpha), radius = 4f, center = Offset(x - 4f, y - 11f))
            drawCircle(color = Color(0xFFFFF176).copy(alpha = alpha), radius = 4f, center = Offset(x + 4f, y - 11f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 2f, center = Offset(x - 4f, y - 11f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 2f, center = Offset(x + 4f, y - 11f))
            drawOwlEars(x, y, alpha)
            drawCircle(color = Color(0xFFFFB74D).copy(alpha = alpha), radius = 2f, center = Offset(x, y - 8f))
        }
    }
}

private fun DrawScope.drawOwlEars(x: Float, y: Float, alpha: Float) {
    val earColor = Color(0xFF8D6E63).copy(alpha = alpha)
    val leftEar = Path().apply {
        moveTo(x - 6f, y - 17f)
        lineTo(x - 9f, y - 25f)
        lineTo(x - 1f, y - 17f)
        close()
    }
    drawPath(leftEar, color = earColor, style = Fill)

    val rightEar = Path().apply {
        moveTo(x + 6f, y - 17f)
        lineTo(x + 9f, y - 25f)
        lineTo(x + 1f, y - 17f)
        close()
    }
    drawPath(rightEar, color = earColor, style = Fill)
}

// --- 猫咪行为绘制 ---

private fun DrawScope.drawCatWithBehavior(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, flipX: Boolean, alpha: Float
) {
    val bodyColor = Color(0xFF757575).copy(alpha = alpha)
    val bellyColor = Color(0xFFBDBDBD).copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.HUNTING -> {
            val huntX = x + sin(time * 1.5f).toFloat() * size.width * 0.04f
            val huntY = y + abs(sin(time * 1.2f)).toFloat() * 3f

            drawShadow(huntX, huntY + 8f, 10f, 3f)
            drawOval(color = bodyColor, topLeft = Offset(huntX - 8f, huntY - 4f), size = Size(16f, 8f))
            drawCircle(color = bodyColor, radius = 5f, center = Offset(huntX + 8f, huntY - 2f))
            val earSign = if (flipX) -1 else 1
            drawCatEars(huntX + 8f * earSign, huntY - 2f, alpha)
            drawCircle(color = Color(0xFF4CAF50).copy(alpha = alpha), radius = 2.5f, center = Offset(huntX + 10f, huntY - 3f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 1.5f, center = Offset(huntX + 10f, huntY - 3f))
            val tailPath = Path().apply {
                moveTo(huntX - 8f, huntY - 2f)
                quadraticBezierTo(huntX - 15f, huntY - 20f, huntX - 10f, huntY - 25f)
            }
            drawPath(tailPath, color = bodyColor, style = Stroke(width = 2.5f))
        }
        AnimalBehavior.RESTING -> {
            val curlPhase = sin(time * 0.2f).toFloat() * 2f
            drawOval(color = bodyColor, topLeft = Offset(x - 6f, y - 5f + curlPhase), size = Size(12f, 10f))
            drawCircle(color = bodyColor, radius = 4f, center = Offset(x + 3f, y - 3f + curlPhase))
            val tailPath = Path().apply {
                moveTo(x - 5f, y + 2f + curlPhase)
                quadraticBezierTo(x - 10f, y - 5f + curlPhase, x - 3f, y - 7f + curlPhase)
            }
            drawPath(tailPath, color = bodyColor, style = Stroke(width = 2f))
            drawCircle(color = Color(0xFF616161).copy(alpha = alpha * 0.7f), radius = 2f, center = Offset(x + 5f, y - 7f + curlPhase))
        }
        AnimalBehavior.SLEEPING -> {
            val breathY = sin(time * 0.4f).toFloat() * 1.5f
            drawOval(color = bodyColor, topLeft = Offset(x - 6f, y - 4f + breathY), size = Size(12f, 8f))
            drawCircle(color = bodyColor, radius = 4f, center = Offset(x + 3f, y - 2f + breathY))
            drawLine(
                color = Color(0xFF424242).copy(alpha = alpha * 0.6f),
                start = Offset(x + 4f, y - 3f + breathY),
                end = Offset(x + 6f, y - 3f + breathY),
                strokeWidth = 1f
            )
        }
        AnimalBehavior.HIDING -> {
            drawOval(color = bodyColor.copy(alpha = alpha * 0.4f), topLeft = Offset(x - 4f, y - 3f), size = Size(8f, 6f))
        }
        else -> {
            val stretchX = x + sin(time * 0.3f).toFloat() * 3f
            drawShadow(stretchX, y + 6f, 10f, 3f)
            drawOval(color = bodyColor, topLeft = Offset(stretchX - 8f, y - 4f), size = Size(16f, 8f))
            drawOval(color = bellyColor, topLeft = Offset(stretchX - 4f, y - 2f), size = Size(8f, 5f))
            drawCircle(color = bodyColor, radius = 5f, center = Offset(stretchX + 8f, y - 2f))
            drawCatEars(stretchX + 8f, y - 2f, alpha)
            drawLine(
                color = Color(0xFF4CAF50).copy(alpha = alpha),
                start = Offset(stretchX + 9f, y - 3f),
                end = Offset(stretchX + 11f, y - 3f),
                strokeWidth = 1.5f
            )
            val tailPath = Path().apply {
                moveTo(stretchX - 8f, y)
                quadraticBezierTo(stretchX - 14f, y - 8f, stretchX - 10f, y - 12f)
            }
            drawPath(tailPath, color = bodyColor, style = Stroke(width = 2f))
        }
    }
}

private fun DrawScope.drawCatEars(x: Float, y: Float, alpha: Float) {
    val earColor = Color(0xFF616161).copy(alpha = alpha)
    val leftEar = Path().apply {
        moveTo(x - 4f, y - 4f)
        lineTo(x - 6f, y - 10f)
        lineTo(x - 1f, y - 4f)
        close()
    }
    drawPath(leftEar, color = earColor, style = Fill)

    val rightEar = Path().apply {
        moveTo(x + 1f, y - 4f)
        lineTo(x + 4f, y - 10f)
        lineTo(x + 5f, y - 4f)
        close()
    }
    drawPath(rightEar, color = earColor, style = Fill)
}

// --- 青蛙行为绘制 ---

private fun DrawScope.drawFrogWithBehavior(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    val bodyColor = Color(0xFF66BB6A).copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.CALLING -> {
            val puff = sin(time * 2f).toFloat() * 2f
            val bodyW = 10f + puff
            val bodyH = 8f + puff * 0.5f

            drawShadow(x, y + 6f, 10f, 3f)
            drawOval(color = bodyColor, topLeft = Offset(x - bodyW / 2, y - bodyH / 2), size = Size(bodyW, bodyH))
            drawCircle(color = bodyColor, radius = 4f, center = Offset(x, y - bodyH / 2 - 2f))
            drawCircle(color = Color(0xFFFDD835).copy(alpha = alpha), radius = 2.5f, center = Offset(x - 3f, y - bodyH / 2 - 3f))
            drawCircle(color = Color(0xFFFDD835).copy(alpha = alpha), radius = 2.5f, center = Offset(x + 3f, y - bodyH / 2 - 3f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 1.2f, center = Offset(x - 3f, y - bodyH / 2 - 3f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 1.2f, center = Offset(x + 3f, y - bodyH / 2 - 3f))

            val soundPhase = (time * 3f) % (2 * Math.PI.toFloat())
            for (i in 0..1) {
                val ringPhase = (soundPhase + i * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                val ringR = 5f + ringPhase * 3f
                val ringAlpha = (1f - ringPhase / (2 * Math.PI.toFloat())) * 0.3f * alpha
                if (ringAlpha > 0f) {
                    drawCircle(
                        color = Color(0xFFA5D6A7).copy(alpha = ringAlpha),
                        radius = ringR,
                        center = Offset(x, y - bodyH / 2 - 5f),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }
        AnimalBehavior.HIDING -> {
            val eyeY = y - 2f
            drawCircle(color = Color(0xFFFDD835).copy(alpha = alpha * 0.7f), radius = 2f, center = Offset(x - 3f, eyeY))
            drawCircle(color = Color(0xFFFDD835).copy(alpha = alpha * 0.7f), radius = 2f, center = Offset(x + 3f, eyeY))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha * 0.7f), radius = 1f, center = Offset(x - 3f, eyeY))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha * 0.7f), radius = 1f, center = Offset(x + 3f, eyeY))
            for (i in -2..2) {
                val grassX = x + i * 4f
                drawLine(
                    color = Color(0xFF4CAF50).copy(alpha = 0.5f * alpha),
                    start = Offset(grassX, y + 3f),
                    end = Offset(grassX + (i % 2) * 2f, y - 5f),
                    strokeWidth = 2f
                )
            }
        }
        else -> {
            drawShadow(x, y + 6f, 10f, 3f)
            drawOval(color = bodyColor, topLeft = Offset(x - 5f, y - 4f), size = Size(10f, 8f))
            drawCircle(color = bodyColor, radius = 4f, center = Offset(x, y - 6f))
            drawCircle(color = Color(0xFFFDD835).copy(alpha = alpha), radius = 2f, center = Offset(x - 2.5f, y - 7f))
            drawCircle(color = Color(0xFFFDD835).copy(alpha = alpha), radius = 2f, center = Offset(x + 2.5f, y - 7f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 1f, center = Offset(x - 2.5f, y - 7f))
            drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 1f, center = Offset(x + 2.5f, y - 7f))
        }
    }
}

// --- 萤火虫行为绘制 ---

private fun DrawScope.drawFireflyWithBehavior(
    x: Float, y: Float, time: Float, scale: Float
) {
    val s = scale
    val fireflyX = x + sin(time * 0.2f + x * 0.1f).toFloat() * 8f * s
    val fireflyY = y + cos(time * 0.3f + y * 0.1f).toFloat() * 6f * s
    val pulse = 0.5f + sin(time * 0.8f + x * 0.5f).toFloat() * 0.5f

    drawCircle(
        color = Color(0xFFFFEB3B).copy(alpha = 0.15f * pulse),
        radius = 10f * s,
        center = Offset(fireflyX, fireflyY)
    )
    drawCircle(
        color = Color(0xFFFFEB3B).copy(alpha = 0.6f * pulse),
        radius = 2.5f * s,
        center = Offset(fireflyX, fireflyY)
    )
    drawCircle(
        color = Color(0xFFFFFFFF).copy(alpha = 0.4f * pulse),
        radius = 1f * s,
        center = Offset(fireflyX, fireflyY)
    )
}
// Layer 6: 特效 (增强天气粒子)
private fun DrawScope.drawEffectLayer(
    environment: IslandEnvironment,
    time: Float,
    activeCombos: List<ComboDefinition> = emptyList()
) {
    val (_, _, brightness, tranquility, warmth, _) = environment

    // 雨滴粒子 (brightness < 0.3)
    if (brightness < 0.3f) {
        drawRainEnhanced(time)
    }

    // 风效 + 飘落树叶 (tranquility < 0.3)
    if (tranquility < 0.3f) {
        drawWindEnhanced(time)
    }

    // 阳光光斑 (brightness > 0.7 && warmth > 0.6)
    if (brightness > 0.7f && warmth > 0.6f) {
        drawSunlightEnhanced(time)
    }

    // 萤火虫光点 (lushness > 0.7)
    if (environment.lushness > 0.7f) {
        drawFireflies(time)
    }

    // 生态乐园组合效果: 额外蝴蝶和小鸟
    if (activeCombos.any { it.id == "eco_paradise" }) {
        drawComboExtraAnimals(time)
    }
}

/** 增强雨天效果: 更多雨滴 + 地面溅水 */
private fun DrawScope.drawRainEnhanced(time: Float) {
    val rainColor = Color(0xFF90CAF9).copy(alpha = 0.35f)

    // 主雨滴 (增加数量到 25)
    for (i in 0..24) {
        val rainX = size.width * (i * 0.04f + 0.02f)
        val rainOffset = (time * 140f + i * 50f) % (size.height * 0.7f)
        val rainLen = 10f + (i % 3) * 4f
        drawLine(
            color = rainColor,
            start = Offset(rainX, rainOffset),
            end = Offset(rainX - 2f, rainOffset + rainLen),
            strokeWidth = 1.5f
        )
    }

    // 地面溅水效果 (小圆圈 + 扩散环)
    val splashY = size.height * 0.52f // 地面线附近
    for (i in 0..8) {
        val splashX = size.width * (0.1f + i * 0.1f)
        val splashPhase = (time * 3f + i * 1.7f) % 2f // 0-2 循环
        if (splashPhase < 1f) {
            // 小水花核心
            val splashRadius = splashPhase * 3f
            drawCircle(
                color = Color(0xFFB3E5FC).copy(alpha = (1f - splashPhase) * 0.4f),
                radius = splashRadius,
                center = Offset(splashX, splashY)
            )
        } else {
            // 扩散环
            val ringPhase = splashPhase - 1f
            val ringRadius = ringPhase * 8f
            drawCircle(
                color = Color(0xFFB3E5FC).copy(alpha = (1f - ringPhase) * 0.25f),
                radius = ringRadius,
                center = Offset(splashX, splashY),
                style = Stroke(width = 1f)
            )
        }
    }
}

/** 增强风天效果: 风线 + 飘落树叶 */
private fun DrawScope.drawWindEnhanced(time: Float) {
    val windColor = Color(0xFFB0BEC5).copy(alpha = 0.2f)

    // 风线
    for (i in 0..5) {
        val windY = size.height * (0.3f + i * 0.08f)
        val windOffset = (time * 60f + i * 40f) % (size.width + 100f) - 50f
        drawLine(
            color = windColor,
            start = Offset(windOffset, windY),
            end = Offset(windOffset + 25f, windY),
            strokeWidth = 2f
        )
    }

    // 飘落树叶 (小椭圆 + 旋转)
    val leafColors = listOf(
        Color(0xFF66BB6A).copy(alpha = 0.7f),
        Color(0xFF81C784).copy(alpha = 0.65f),
        Color(0xFFA5D6A7).copy(alpha = 0.6f),
        Color(0xFFFFCC80).copy(alpha = 0.55f)
    )
    for (i in 0..7) {
        val leafBaseX = size.width * (0.1f + i * 0.12f)
        val leafPhase = time * 0.8f + i * 2.3f
        // 树叶从上方飘落，带水平漂移
        val leafX = leafBaseX + sin(leafPhase * 0.7f).toFloat() * size.width * 0.06f
        val leafY = (size.height * 0.1f + (leafPhase * 40f) % (size.height * 0.45f))
        val leafRotation = leafPhase * 2f // 旋转角度
        val leafColor = leafColors[i % leafColors.size]

        // 用椭圆模拟树叶，通过 sin/cos 做简单旋转效果
        val rotCos = cos(leafRotation.toDouble()).toFloat()
        val rotSin = sin(leafRotation.toDouble()).toFloat()
        // 用两条交叉线段模拟旋转的椭圆叶片
        val leafW = 5f
        val leafH = 2.5f
        drawOval(
            color = leafColor,
            topLeft = Offset(leafX - leafW, leafY - leafH),
            size = Size(leafW * 2, leafH * 2)
        )
        // 叶脉线
        drawLine(
            color = leafColor.copy(alpha = leafColor.alpha * 0.5f),
            start = Offset(leafX - leafW * rotCos, leafY - leafH * rotSin),
            end = Offset(leafX + leafW * rotCos, leafY + leafH * rotSin),
            strokeWidth = 0.8f
        )
    }
}

/** 增强阳光光斑效果: 更多光斑 + 缓慢漂浮 */
private fun DrawScope.drawSunlightEnhanced(time: Float) {
    for (i in 0..10) {
        val spotBaseX = size.width * (0.1f + i * 0.08f)
        val spotBaseY = size.height * (0.08f + (i % 3) * 0.05f)
        // 缓慢漂浮
        val spotX = spotBaseX + sin(time * 0.15f + i * 1.3f).toFloat() * size.width * 0.02f
        val spotY = spotBaseY + cos(time * 0.2f + i * 0.9f).toFloat() * size.height * 0.015f
        val spotRadius = 3f + sin(time * 0.25f + i * 0.7f).toFloat() * 2f
        val spotAlpha = 0.2f + sin(time * 0.3f + i * 1.1f).toFloat() * 0.15f
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = spotAlpha),
            radius = spotRadius,
            center = Offset(spotX, spotY)
        )
    }
}

private fun DrawScope.drawFireflies(time: Float) {
    for (i in 0..6) {
        val baseX = size.width * (0.15f + i * 0.12f)
        val baseY = size.height * (0.35f + (i % 3) * 0.1f)
        val fireflyX = baseX + sin(time * 0.2f + i * 2f).toFloat() * 8f
        val fireflyY = baseY + cos(time * 0.3f + i * 1.5f).toFloat() * 6f
        val pulse = 0.5f + sin(time * 0.8f + i * 3f).toFloat() * 0.5f

        // 光晕
        drawCircle(
            color = Color(0xFFFFEB3B).copy(alpha = 0.15f * pulse),
            radius = 10f,
            center = Offset(fireflyX, fireflyY)
        )
        // 核心
        drawCircle(
            color = Color(0xFFFFEB3B).copy(alpha = 0.6f * pulse),
            radius = 2.5f,
            center = Offset(fireflyX, fireflyY)
        )
    }
}

/** 生态乐园组合效果: 额外蝴蝶和小鸟 */
private fun DrawScope.drawComboExtraAnimals(time: Float) {
    // 额外蝴蝶 (3只)
    for (i in 0..2) {
        val butterflyX = size.width * (0.35f + i * 0.15f) + sin(time * 0.3f + i * 2f).toFloat() * 20f
        val butterflyY = size.height * (0.3f + (i % 2) * 0.1f) + cos(time * 0.4f + i * 1.5f).toFloat() * 10f
        val butterflyAlpha = 0.5f + sin(time * 0.5f + i).toFloat() * 0.3f

        // 身体
        drawLine(
            color = Color(0xFF5D4037).copy(alpha = butterflyAlpha),
            start = Offset(butterflyX, butterflyY - 3f),
            end = Offset(butterflyX, butterflyY + 3f),
            strokeWidth = 1f
        )
        // 翅膀
        drawOval(
            color = Color(0xFF81D4FA).copy(alpha = butterflyAlpha * 0.8f),
            topLeft = Offset(butterflyX - 5f, butterflyY - 4f),
            size = Size(5f, 8f)
        )
        drawOval(
            color = Color(0xFF81D4FA).copy(alpha = butterflyAlpha * 0.8f),
            topLeft = Offset(butterflyX, butterflyY - 4f),
            size = Size(5f, 8f)
        )
    }

    // 额外小鸟 (2只)
    for (i in 0..1) {
        val birdX = size.width * (0.2f + i * 0.5f) + sin(time * 0.2f + i * 3f).toFloat() * size.width * 0.04f
        val birdY = size.height * (0.2f + i * 0.08f) + cos(time * 0.3f + i * 2f).toFloat() * 5f
        val birdAlpha = 0.6f + sin(time * 0.4f + i).toFloat() * 0.2f

        // 左翅
        val leftWingPath = Path().apply {
            moveTo(birdX, birdY)
            quadraticBezierTo(birdX - 6f, birdY - 8f, birdX - 12f, birdY - 2f)
        }
        drawPath(leftWingPath, color = Color(0xFF8D6E63).copy(alpha = birdAlpha), style = Stroke(width = 2f))

        // 右翅
        val rightWingPath = Path().apply {
            moveTo(birdX, birdY)
            quadraticBezierTo(birdX + 6f, birdY - 8f, birdX + 12f, birdY - 2f)
        }
        drawPath(rightWingPath, color = Color(0xFF8D6E63).copy(alpha = birdAlpha), style = Stroke(width = 2f))
    }
}

// --- 辅助函数 ---

private fun DrawScope.drawShadow(x: Float, y: Float, radius: Float, offsetY: Float) {
    drawOval(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(x - radius, y + offsetY),
        size = Size(radius * 2, offsetY)
    )
}

private fun DrawScope.drawStar(x: Float, y: Float, radius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 5) {
        val angle = Math.toRadians((-90 + i * 72).toDouble()).toFloat()
        val px = x + cos(angle) * radius
        val py = y + sin(angle) * radius
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)

        val innerAngle = Math.toRadians((-90 + i * 72 + 36).toDouble()).toFloat()
        val innerPx = x + cos(innerAngle) * radius * 0.4f
        val innerPy = y + sin(innerAngle) * radius * 0.4f
        path.lineTo(innerPx, innerPy)
    }
    path.close()
    drawPath(path, color = color, style = Fill)
}

// ==================== 隐藏发现系统 - 稀有元素视觉效果 ====================

/**
 * 绘制稀有元素天空层特效
 */
private fun DrawScope.drawRareElementsSky(
    activeRareElements: List<IslandDiscovery>,
    time: Float,
    timeOfDay: Float
) {
    for (discovery in activeRareElements) {
        val key = discovery.discoveryKey
        // 狼人剪影: 月光增强
        if (key.startsWith("werewolf_")) {
            drawWerewolfMoonGlow(time, timeOfDay)
        }
        // 彩虹桥: 天空彩虹
        if (key == "rainbow_bridge") {
            drawRainbowBridge(time)
        }
        // 极光: 天空彩色波纹
        if (key.startsWith("aurora_")) {
            drawAurora(time)
        }
        // 烟花: 粒子爆炸效果
        if (key.startsWith("fireworks_")) {
            drawFireworks(time)
        }
    }
}

/**
 * 绘制稀有元素前景特效
 */
private fun DrawScope.drawRareElementsForeground(
    activeRareElements: List<IslandDiscovery>,
    time: Float
) {
    for (discovery in activeRareElements) {
        val key = discovery.discoveryKey
        // 精灵之光: 漂浮的光点
        if (key == "elf_light") {
            drawElfLights(time)
        }
        // 记忆树: 特殊大树 + 发光叶子
        if (key == "memory_tree") {
            drawMemoryTree(time)
        }
    }
}

/**
 * 狼人剪影: 月光增强效果
 */
private fun DrawScope.drawWerewolfMoonGlow(time: Float, timeOfDay: Float) {
    if (!isNight(timeOfDay)) return

    val centerX = size.width * 0.5f
    val moonY = size.height * 0.15f

    // 增强的月光光晕
    val glowPulse = 0.3f + sin(time * 0.2f).toFloat() * 0.1f
    drawCircle(
        color = Color(0xFFE8EAF6).copy(alpha = glowPulse),
        radius = 80f,
        center = Offset(centerX, moonY)
    )

    // 狼人剪影（简化几何形状）
    val silhouetteX = size.width * 0.65f
    val silhouetteY = size.height * 0.4f
    val silhouetteAlpha = 0.4f + sin(time * 0.3f).toFloat() * 0.1f

    // 身体
    val bodyPath = Path().apply {
        moveTo(silhouetteX, silhouetteY)
        lineTo(silhouetteX - 15f, silhouetteY + 30f)
        lineTo(silhouetteX + 15f, silhouetteY + 30f)
        close()
    }
    drawPath(bodyPath, color = Color.Black.copy(alpha = silhouetteAlpha), style = Fill)

    // 头部
    drawCircle(
        color = Color.Black.copy(alpha = silhouetteAlpha),
        radius = 10f,
        center = Offset(silhouetteX, silhouetteY - 12f)
    )

    // 耳朵
    val leftEarPath = Path().apply {
        moveTo(silhouetteX - 6f, silhouetteY - 18f)
        lineTo(silhouetteX - 10f, silhouetteY - 30f)
        lineTo(silhouetteX - 2f, silhouetteY - 18f)
        close()
    }
    drawPath(leftEarPath, color = Color.Black.copy(alpha = silhouetteAlpha), style = Fill)

    val rightEarPath = Path().apply {
        moveTo(silhouetteX + 6f, silhouetteY - 18f)
        lineTo(silhouetteX + 10f, silhouetteY - 30f)
        lineTo(silhouetteX + 2f, silhouetteY - 18f)
        close()
    }
    drawPath(rightEarPath, color = Color.Black.copy(alpha = silhouetteAlpha), style = Fill)

    // 发光的眼睛
    drawCircle(
        color = Color(0xFFFFEB3B).copy(alpha = 0.8f),
        radius = 2f,
        center = Offset(silhouetteX - 4f, silhouetteY - 12f)
    )
    drawCircle(
        color = Color(0xFFFFEB3B).copy(alpha = 0.8f),
        radius = 2f,
        center = Offset(silhouetteX + 4f, silhouetteY - 12f)
    )
}

/**
 * 彩虹桥: 七彩弧线
 */
private fun DrawScope.drawRainbowBridge(time: Float) {
    val centerX = size.width * 0.5f
    val centerY = size.height * 0.35f
    val radius = size.width * 0.35f

    val rainbowColors = listOf(
        Color(0xFFFF0000).copy(alpha = 0.3f),  // 红
        Color(0xFFFF9900).copy(alpha = 0.3f),  // 橙
        Color(0xFFFFFF00).copy(alpha = 0.3f),  // 黄
        Color(0xFF00FF00).copy(alpha = 0.3f),  // 绿
        Color(0xFF0099FF).copy(alpha = 0.3f),  // 蓝
        Color(0xFF6600CC).copy(alpha = 0.3f),  // 靛
        Color(0xFF9900FF).copy(alpha = 0.3f)   // 紫
    )

    val arcAlpha = 0.5f + sin(time * 0.2f).toFloat() * 0.2f

    for ((index, color) in rainbowColors.withIndex()) {
        val arcRadius = radius - index * 8f
        val arcPath = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(
                    center = Offset(centerX, centerY),
                    radius = arcRadius
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f
            )
        }
        drawPath(
            path = arcPath,
            color = color.copy(alpha = arcAlpha),
            style = Stroke(width = 6f)
        )
    }
}

/**
 * 精灵之光: 漂浮的光点
 */
private fun DrawScope.drawElfLights(time: Float) {
    val lightCount = 15
    for (i in 0 until lightCount) {
        val baseX = size.width * (0.1f + (i * 0.06f))
        val baseY = size.height * (0.2f + (i % 5) * 0.1f)

        // 漂浮路径
        val x = baseX + sin(time * 0.3f + i * 1.5f).toFloat() * 20f
        val y = baseY + cos(time * 0.4f + i * 2f).toFloat() * 15f

        // 脉冲效果
        val pulse = 0.5f + sin(time * 0.8f + i * 0.7f).toFloat() * 0.5f

        // 外层光晕
        drawCircle(
            color = Color(0xFFE1F5FE).copy(alpha = 0.1f * pulse),
            radius = 15f,
            center = Offset(x, y)
        )

        // 中层光晕
        drawCircle(
            color = Color(0xFFB3E5FC).copy(alpha = 0.2f * pulse),
            radius = 8f,
            center = Offset(x, y)
        )

        // 核心光点
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = 0.8f * pulse),
            radius = 3f,
            center = Offset(x, y)
        )
    }
}

/**
 * 记忆树: 特殊大树 + 发光叶子
 */
private fun DrawScope.drawMemoryTree(time: Float) {
    val treeX = size.width * 0.5f
    val treeBaseY = size.height * 0.55f
    val treeHeight = 120f

    // 树干（比普通树更粗更长）
    drawRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(treeX - 8f, treeBaseY - treeHeight * 0.6f),
        size = Size(16f, treeHeight * 0.6f)
    )

    // 树根（延伸到地面）
    val rootPath = Path().apply {
        moveTo(treeX - 8f, treeBaseY)
        quadraticBezierTo(treeX - 20f, treeBaseY + 10f, treeX - 30f, treeBaseY + 5f)
        moveTo(treeX + 8f, treeBaseY)
        quadraticBezierTo(treeX + 20f, treeBaseY + 10f, treeX + 30f, treeBaseY + 5f)
    }
    drawPath(rootPath, color = Color(0xFF5D4037), style = Stroke(width = 4f))

    // 树冠（多层圆形，发光效果）
    val crownColors = listOf(
        Color(0xFF81C784).copy(alpha = 0.7f),
        Color(0xFF66BB6A).copy(alpha = 0.8f),
        Color(0xFF4CAF50).copy(alpha = 0.9f)
    )

    for ((index, color) in crownColors.withIndex()) {
        val crownY = treeBaseY - treeHeight * 0.7f - index * 15f
        val crownRadius = 40f - index * 5f

        // 发光效果
        val glowAlpha = 0.2f + sin(time * 0.3f + index).toFloat() * 0.1f
        drawCircle(
            color = Color(0xFFA5D6A7).copy(alpha = glowAlpha),
            radius = crownRadius + 15f,
            center = Offset(treeX, crownY)
        )

        drawCircle(
            color = color,
            radius = crownRadius,
            center = Offset(treeX, crownY)
        )
    }

    // 发光叶子（漂浮效果）
    for (i in 0..8) {
        val leafPhase = time * 0.5f + i * 1.2f
        val leafX = treeX + sin(leafPhase).toFloat() * 50f
        val leafY = treeBaseY - treeHeight * 0.5f + cos(leafPhase * 0.7f).toFloat() * 30f

        val leafPulse = 0.4f + sin(time * 0.6f + i).toFloat() * 0.3f
        drawCircle(
            color = Color(0xFFC8E6C9).copy(alpha = leafPulse),
            radius = 4f,
            center = Offset(leafX, leafY)
        )
    }
}

/**
 * 烟花: 粒子爆炸效果
 */
private fun DrawScope.drawFireworks(time: Float) {
    val fireworkColors = listOf(
        Color(0xFFFF5252),  // 红
        Color(0xFFFFD740),  // 黄
        Color(0xFF69F0AE),  // 绿
        Color(0xFF40C4FF),  // 蓝
        Color(0xFFE040FB)   // 紫
    )

    // 多个烟花爆炸点
    val explosionCount = 5
    for (e in 0 until explosionCount) {
        val explosionX = size.width * (0.2f + e * 0.15f)
        val explosionY = size.height * (0.1f + (e % 3) * 0.08f)
        val explosionPhase = (time * 0.4f + e * 1.5f) % 4f  // 0-4 循环

        if (explosionPhase < 2f) {
            // 爆炸扩散阶段
            val spread = explosionPhase * 40f
            val particleCount = 12
            val color = fireworkColors[e % fireworkColors.size]

            for (i in 0 until particleCount) {
                val angle = i * (360f / particleCount)
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                val particleX = explosionX + cos(rad) * spread
                val particleY = explosionY + sin(rad) * spread

                val particleAlpha = (1f - explosionPhase / 2f) * 0.8f
                val particleRadius = 3f - explosionPhase * 0.5f

                drawCircle(
                    color = color.copy(alpha = particleAlpha),
                    radius = particleRadius,
                    center = Offset(particleX, particleY)
                )
            }

            // 中心闪光
            val flashAlpha = (1f - explosionPhase / 2f) * 0.6f
            drawCircle(
                color = Color.White.copy(alpha = flashAlpha),
                radius = 8f - explosionPhase * 2f,
                center = Offset(explosionX, explosionY)
            )
        } else {
            // 火星飘落阶段
            val fallPhase = explosionPhase - 2f
            val particleCount = 8
            val color = fireworkColors[e % fireworkColors.size]

            for (i in 0 until particleCount) {
                val angle = i * (360f / particleCount)
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                val spread = 80f - fallPhase * 15f
                val particleX = explosionX + cos(rad) * spread + sin(time + i).toFloat() * 5f
                val particleY = explosionY + sin(rad) * spread + fallPhase * 20f  // 重力下落

                val particleAlpha = (1f - fallPhase / 2f) * 0.5f

                drawCircle(
                    color = color.copy(alpha = particleAlpha),
                    radius = 2f,
                    center = Offset(particleX, particleY)
                )
            }
        }
    }
}

/**
 * 极光: 天空彩色波纹
 */
private fun DrawScope.drawAurora(time: Float) {
    val auroraColors = listOf(
        Color(0xFF00E5FF).copy(alpha = 0.15f),  // 青
        Color(0xFF00E676).copy(alpha = 0.12f),  // 绿
        Color(0xFF7C4DFF).copy(alpha = 0.10f),  // 紫
        Color(0xFFFF4081).copy(alpha = 0.08f)   // 粉
    )

    for ((index, color) in auroraColors.withIndex()) {
        val bandY = size.height * (0.08f + index * 0.04f)
        val waveAmplitude = 30f + index * 10f
        val waveOffset = time * 0.15f + index * 1.5f

        val auroraPath = Path().apply {
            moveTo(0f, bandY)
            for (x in 0..size.width.toInt() step 8) {
                val xFloat = x.toFloat()
                val y = bandY + sin(xFloat * 0.008f + waveOffset).toFloat() * waveAmplitude
                lineTo(xFloat, y)
            }
            for (x in size.width.toInt() downTo 0 step 8) {
                val xFloat = x.toFloat()
                val y = bandY + 40f + sin(xFloat * 0.008f + waveOffset + 1f).toFloat() * waveAmplitude * 0.6f
                lineTo(xFloat, y)
            }
            close()
        }

        drawPath(
            path = auroraPath,
            color = color,
            style = Fill
        )
    }

    // 额外的光点闪烁
    for (i in 0..5) {
        val x = size.width * (0.15f + i * 0.14f)
        val y = size.height * (0.1f + (i % 3) * 0.03f)
        val pulse = 0.3f + sin(time * 0.5f + i * 2f).toFloat() * 0.3f
        drawCircle(
            color = Color(0xFFB2FF59).copy(alpha = pulse * 0.4f),
            radius = 6f,
            center = Offset(x, y)
        )
    }
}
