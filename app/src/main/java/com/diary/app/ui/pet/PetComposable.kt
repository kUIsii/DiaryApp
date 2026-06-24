package com.diary.app.ui.pet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.diary.app.data.PetGrowthStage
import com.diary.app.data.PetHiddenStateType
import com.diary.app.data.PetState
import com.diary.app.data.CombinationEffect
import kotlinx.coroutines.delay

/**
 * 情绪宠物 Canvas 绘制组件
 * 几何极简风 - 有机水滴形身体 + 表情系统 + 粒子特效
 */
@Composable
fun PetComposable(
    state: PetState,
    modifier: Modifier = Modifier,
    interactionType: InteractionType = InteractionType.NONE,
    interactionCounter: Int = 0,
    appearanceLevel: Int = 1,
    growthStage: PetGrowthStage = PetGrowthStage.JUVENILE,
    hiddenState: PetHiddenStateType? = null,
    activeEffects: List<CombinationEffect> = emptyList()
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 呼吸动画
    val breathDuration = when (state) {
        PetState.EXCITED -> 1000
        PetState.HAPPY -> 1500
        PetState.CALM -> 3000
        PetState.SLEEPY -> 4000
        PetState.TIRED -> 5000
        else -> 3000
    }

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(breathDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 眨眼动画 (0->1 循环，起始阶段触发闭眼)
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart
        )
    )

    // 弹跳动画
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 挤压拉伸动画 X轴 (squash: 弹起时变宽)
    val squashStretchX by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 挤压拉伸动画 Y轴 (stretch: 弹起时变矮)
    val squashStretchY by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 腹部呼吸 (比胸腔慢 15%, 幅度更大)
    val bellyBreathScale by infiniteTransition.animateFloat(
        initialValue = 0.975f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(breathDuration + 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 微妙的左右晃动 (呼吸周期的两倍，更缓慢)
    val swayOffset by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(breathDuration * 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 状态颜色
    val stateColor = when (state) {
        PetState.CALM -> Color(0xFF7BA7C9)
        PetState.HAPPY -> Color(0xFF7BC9A0)
        PetState.SLEEPY -> Color(0xFF8899AA)
        PetState.WORRIED -> Color(0xFFD99AB8)
        PetState.SAD -> Color(0xFF818CF8)
        PetState.EXCITED -> Color(0xFFA88BC9)
        PetState.CURIOUS -> Color(0xFFD4A06A)
        PetState.TIRED -> Color(0xFF9088A8)
    }

    // 身体形变参数
    val bodyWidthFactor = when (state) {
        PetState.HAPPY -> 1.05f
        PetState.EXCITED -> 0.95f
        PetState.TIRED -> 1.1f
        PetState.CURIOUS -> 0.98f
        else -> 1f
    }

    val bodyHeightFactor = when (state) {
        PetState.EXCITED -> 1.1f
        PetState.SAD -> 0.9f
        PetState.TIRED -> 0.85f
        PetState.SLEEPY -> 0.95f
        else -> 1f
    }

    // 成长阶段视觉参数
    val eyeRatio = when (growthStage) {
        PetGrowthStage.JUVENILE -> 0.4f   // 幼年期: 大眼睛40%
        PetGrowthStage.GROWING -> 0.3f    // 成长期: 中眼睛30%
        PetGrowthStage.MATURE -> 0.25f    // 成熟期: 正常眼睛25%
    }

    // 弹跳频率（幼年期更高）
    val bounceModifier = when (growthStage) {
        PetGrowthStage.JUVENILE -> 1.5f
        PetGrowthStage.GROWING -> 1f
        PetGrowthStage.MATURE -> 0.7f
    }

    // 根据成长阶段调整弹跳幅度
    val adjustedBounce = bounce * bounceModifier

    val offsetY = when (state) {
        PetState.HAPPY -> adjustedBounce
        PetState.EXCITED -> adjustedBounce * 1.5f
        else -> 0f
    }

    // ==================== 交互动画状态 ====================

    // 点击弹跳
    var bounceOffset by remember { mutableStateOf(0f) }
    val animBounce = remember { Animatable(0f) }
    LaunchedEffect(interactionCounter) {
        if (interactionType == InteractionType.TAP && interactionCounter > 0) {
            animBounce.snapTo(0f)
            animBounce.animateTo(
                targetValue = 0f,
                animationSpec = tween(100)
            ) {
                bounceOffset = value
            }
            animBounce.animateTo(
                targetValue = -20f,
                animationSpec = tween(150, easing = FastOutSlowInEasing)
            ) {
                bounceOffset = value
            }
            animBounce.animateTo(
                targetValue = 0f,
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) {
                bounceOffset = value
            }
            bounceOffset = 0f
        }
    }

    // 爱心粒子爆发
    val heartParticles = remember { mutableStateListOf<HeartParticle>() }
    var heartBurstProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(interactionCounter) {
        if (interactionType == InteractionType.GROOM && interactionCounter > 0) {
            heartParticles.clear()
            val count = 8
            for (i in 0 until count) {
                val angle = (360f / count) * i + (kotlin.random.Random.nextFloat() - 0.5f) * 30f
                val dist = 40f + kotlin.random.Random.nextFloat() * 40f
                heartParticles.add(
                    HeartParticle(
                        angle = angle,
                        distance = dist,
                        size = 4f + kotlin.random.Random.nextFloat() * 4f
                    )
                )
            }
            heartBurstProgress = 0f
            val duration = 600L
            val startTime = System.nanoTime()
            while (heartBurstProgress < 1f) {
                heartBurstProgress = ((System.nanoTime() - startTime) / 1_000_000f / duration).coerceAtMost(1f)
                delay(16)
            }
            heartParticles.clear()
            heartBurstProgress = 0f
        }
    }

    // 长按旋转
    var longPressRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(interactionCounter) {
        if (interactionType == InteractionType.LONG_PRESS && interactionCounter > 0) {
            longPressRotation = 0f
            val anim = Animatable(0f)
            anim.animateTo(
                targetValue = 360f,
                animationSpec = tween(500, easing = LinearEasing)
            ) {
                longPressRotation = value
            }
            longPressRotation = 0f
        }
    }

    // 动画数值
    val animatedScale by animateFloatAsState(
        targetValue = if (interactionType == InteractionType.TAP && interactionCounter > 0) 1.15f else 1f,
        animationSpec = tween(100)
    )

    val interactionScale = if (interactionType == InteractionType.TAP) animatedScale else 1f
    val interactionRotation = if (interactionType == InteractionType.LONG_PRESS) longPressRotation else 0f

    // ==================== 隐藏状态视觉效果 ====================

    // 隐藏状态颜色切换动画（时间旅人）
    val hiddenStateColorTransition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 隐藏状态粒子动画
    val hiddenParticleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 根据隐藏状态调整颜色
    val finalStateColor = when (hiddenState) {
        PetHiddenStateType.NIGHT_OWL -> Color(0xFF1A237E) // 深蓝色
        PetHiddenStateType.DEEP_DIVER -> {
            // 深蓝渐变
            val t = hiddenStateColorTransition
            Color(
                red = 0x1A * (1 - t) + 0x0D * t,
                green = 0x23 * (1 - t) + 0x47 * t,
                blue = 0x7E * (1 - t) + 0xA1 * t
            )
        }
        PetHiddenStateType.TIME_TRAVELER -> {
            // 颜色缓慢切换
            val t = hiddenStateColorTransition
            Color(
                red = stateColor.red * (1 - t) + Color(0xFF7B1FA2).red * t,
                green = stateColor.green * (1 - t) + Color(0xFF7B1FA2).green * t,
                blue = stateColor.blue * (1 - t) + Color(0xFF7B1FA2).blue * t
            )
        }
        else -> stateColor
    }

    // ==================== 成长阶段视觉效果 ====================

    // 进化动画 - stage变化时播放弹跳
    var evolutionScale by remember { mutableStateOf(1f) }
    var lastGrowthStage by remember { mutableStateOf(growthStage) }
    LaunchedEffect(growthStage) {
        if (growthStage != lastGrowthStage) {
            lastGrowthStage = growthStage
            // 进化弹跳动画
            val anim = Animatable(1f)
            anim.animateTo(1.3f, tween(200, easing = FastOutSlowInEasing)) { evolutionScale = value }
            anim.animateTo(0.9f, tween(150, easing = FastOutSlowInEasing)) { evolutionScale = value }
            anim.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) { evolutionScale = value }
            evolutionScale = 1f
        }
    }

    // 挤压拉伸变换参数（与弹跳同步）
    val scaleX = if (state == PetState.HAPPY || state == PetState.EXCITED) {
        squashStretchX
    } else 1f
    val scaleY = if (state == PetState.HAPPY || state == PetState.EXCITED) {
        squashStretchY
    } else 1f

    Canvas(modifier = modifier
        .size(160.dp)
        .scale(interactionScale * evolutionScale)
        .rotate(interactionRotation)
    ) {
        val centerX = size.width / 2
        val bodyCenterX = centerX + swayOffset  // 身体跟随晃动
        val centerY = size.height / 2 + offsetY
        val baseWidth = size.width * 0.4f * bodyWidthFactor
        val baseHeight = size.height * 0.45f * bodyHeightFactor

        // 绘制隐藏状态特殊效果（在身体之前）
        if (hiddenState != null) {
            drawHiddenStateEffects(
                bodyCenterX, centerY, baseWidth, baseHeight,
                hiddenState, hiddenParticleProgress, hiddenStateColorTransition, breathScale
            )
        }

        // 绘制外观装饰（小岛等级影响）
        if (appearanceLevel >= 10) {
            // Lv10-14: 身体光环效果
            drawHaloEffect(bodyCenterX, centerY, baseWidth, breathScale)
        }
        if (appearanceLevel >= 15) {
            // Lv15+: 翅膀装饰
            drawWings(bodyCenterX, centerY, baseWidth, baseHeight)
        }

        // 绘制身体（隐藏状态下使用特殊颜色，带胸腔/腹部差异化呼吸 + 挤压拉伸）
        drawOrganicBody(bodyCenterX, centerY, baseWidth, baseHeight, finalStateColor, breathScale, bellyBreathScale, scaleX, scaleY)

        // 成长阶段特殊效果
        if (growthStage == PetGrowthStage.GROWING) {
            // 成长期: 头顶呆毛
            drawHairAntenna(centerX, centerY, baseHeight, finalStateColor)
        }
        if (growthStage == PetGrowthStage.MATURE) {
            // 成熟期: 身体光晕
            drawGlowEffect(centerX, centerY, baseWidth, breathScale, finalStateColor)
        }

        // 绘制表情（使用成长阶段眼睛比例，隐藏状态下特殊眼睛）
        if (hiddenState == PetHiddenStateType.NIGHT_OWL) {
            // 夜猫子: 月牙眼
            drawCrescentMoonEyes(centerX, centerY - baseHeight * 0.1f, baseWidth * 0.38f, baseWidth * 0.09f)
        } else if (hiddenState == PetHiddenStateType.TREASURE_HUNTER) {
            // 宝藏猎人: 星星眼
            drawStarEyes(centerX, centerY - baseHeight * 0.1f, baseWidth * 0.38f, baseWidth * 0.09f)
        } else {
            drawPetExpression(centerX, centerY, baseWidth, baseHeight, state, blinkProgress, eyeRatio)
        }

        // 绘制状态特效
        drawStateEffects(centerX, centerY, baseWidth, state)

        // Lv5-9: 头顶小星星装饰
        if (appearanceLevel in 5..14) {
            drawStarDecoration(centerX, centerY, baseHeight)
        }

        // 绘制爱心粒子爆发
        if (heartParticles.isNotEmpty()) {
            drawHeartBurst(centerX, centerY, heartParticles.toList(), heartBurstProgress)
        }

        // 绘制称号组合效果
        if (activeEffects.isNotEmpty()) {
            drawCombinationEffects(centerX, centerY, baseWidth, baseHeight, activeEffects, breathScale)
        }
    }
}

/**
 * 爱心粒子数据
 */
private data class HeartParticle(
    val angle: Float,
    val distance: Float,
    val size: Float
)

private fun DrawScope.drawOrganicBody(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    color: Color,
    chestScale: Float,    // 胸腔缩放（上半身，幅度较小）
    bellyScale: Float,    // 腹部缩放（下半身，幅度更大）
    scaleX: Float = 1f,
    scaleY: Float = 1f
) {
    val avgBreath = (chestScale + bellyScale) / 2f
    val scaledWidth = width * avgBreath * scaleX
    val chestH = height * chestScale * scaleY   // 上半身高度
    val bellyH = height * bellyScale * scaleY   // 下半身高度
    val topY = centerY - chestH * 0.5f
    val bottomY = centerY + bellyH * 0.55f
    val leftX = centerX - scaledWidth
    val rightX = centerX + scaledWidth

    // 身体路径 - 有机水滴形 (胸腔/腹部差异化呼吸)
    val bodyPath = Path().apply {
        moveTo(centerX, topY + chestH * 0.15f)

        // 右上曲线 - 胸腔区域
        cubicTo(
            x1 = centerX + scaledWidth * 0.55f, y1 = topY - chestH * 0.02f,
            x2 = centerX + scaledWidth * 1.05f, y2 = centerY - chestH * 0.35f,
            x3 = rightX, y3 = centerY - chestH * 0.05f
        )

        // 右侧曲线 - 过渡到腹部
        cubicTo(
            x1 = rightX + scaledWidth * 0.05f, y1 = centerY + bellyH * 0.15f,
            x2 = centerX + scaledWidth * 0.95f, y2 = centerY + bellyH * 0.45f,
            x3 = centerX + scaledWidth * 0.75f, y3 = bottomY - bellyH * 0.08f
        )

        // 右下曲线 - 腹部区域
        cubicTo(
            x1 = centerX + scaledWidth * 0.55f, y1 = bottomY + bellyH * 0.03f,
            x2 = centerX + scaledWidth * 0.3f, y2 = bottomY + bellyH * 0.02f,
            x3 = centerX, y3 = bottomY
        )

        // 左下曲线 - 对称
        cubicTo(
            x1 = centerX - scaledWidth * 0.3f, y1 = bottomY + bellyH * 0.02f,
            x2 = centerX - scaledWidth * 0.55f, y2 = bottomY + bellyH * 0.03f,
            x3 = centerX - scaledWidth * 0.75f, y3 = bottomY - bellyH * 0.08f
        )

        // 左侧曲线 - 过渡到胸腔
        cubicTo(
            x1 = centerX - scaledWidth * 0.95f, y1 = centerY + bellyH * 0.45f,
            x2 = leftX - scaledWidth * 0.05f, y2 = centerY + bellyH * 0.15f,
            x3 = leftX, y3 = centerY - chestH * 0.05f
        )

        // 左上曲线 - 胸腔区域
        cubicTo(
            x1 = centerX - scaledWidth * 1.05f, y1 = centerY - chestH * 0.35f,
            x2 = centerX - scaledWidth * 0.55f, y2 = topY - chestH * 0.02f,
            x3 = centerX, y3 = topY + chestH * 0.15f
        )

        close()
    }

    // 地面投影 (更柔和的椭圆阴影)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.12f),
                Color.Black.copy(alpha = 0.04f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY + bellyH * 0.55f),
            radius = scaledWidth * 1.3f
        ),
        topLeft = Offset(centerX - scaledWidth * 1.3f, centerY + bellyH * 0.35f),
        size = androidx.compose.ui.geometry.Size(scaledWidth * 2.6f, bellyH * 0.4f)
    )

    // 身体边缘阴影 (用描边路径模拟)
    val edgeShadowPath = Path().apply {
        addPath(bodyPath)
    }
    drawPath(
        path = edgeShadowPath,
        color = Color.Black.copy(alpha = 0.06f),
        style = Stroke(width = 4f)
    )

    // 身体主体 - 渐变填充 (顶部亮 -> 中间饱和 -> 底部暗)
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 0.92f),
                color,
                color.copy(red = (color.red * 0.85f).coerceIn(0f, 1f),
                    green = (color.green * 0.85f).coerceIn(0f, 1f),
                    blue = (color.blue * 0.85f).coerceIn(0f, 1f))
            ),
            startY = topY,
            endY = bottomY
        )
    )

    // 主高光区域 (左上方，模拟光照方向)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(centerX - scaledWidth * 0.35f, centerY - chestH * 0.25f),
            radius = scaledWidth * 0.65f
        ),
        topLeft = Offset(centerX - scaledWidth * 0.7f, centerY - chestH * 0.55f),
        size = androidx.compose.ui.geometry.Size(scaledWidth * 1.2f, chestH * 0.6f)
    )

    // 次高光点 (右上小光斑)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(centerX + scaledWidth * 0.4f, centerY - chestH * 0.35f),
            radius = scaledWidth * 0.25f
        ),
        radius = scaledWidth * 0.25f,
        center = Offset(centerX + scaledWidth * 0.4f, centerY - chestH * 0.35f)
    )

    // 底部反光 (环境光反射)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY + bellyH * 0.25f),
            radius = scaledWidth * 0.5f
        ),
        topLeft = Offset(centerX - scaledWidth * 0.5f, centerY + bellyH * 0.05f),
        size = androidx.compose.ui.geometry.Size(scaledWidth * 1f, bellyH * 0.4f)
    )

    // 小脚 - 带渐变
    val footY = bottomY - 8f
    val footColorLeft = color.copy(alpha = 0.82f)
    val footColorRight = color.copy(alpha = 0.78f)
    drawOval(
        brush = Brush.horizontalGradient(
            colors = listOf(footColorLeft, footColorRight),
            startX = centerX - scaledWidth * 0.45f,
            endX = centerX - scaledWidth * 0.1f
        ),
        topLeft = Offset(centerX - scaledWidth * 0.45f, footY),
        size = androidx.compose.ui.geometry.Size(scaledWidth * 0.35f, 10f)
    )
    drawOval(
        brush = Brush.horizontalGradient(
            colors = listOf(footColorRight, footColorLeft),
            startX = centerX + scaledWidth * 0.1f,
            endX = centerX + scaledWidth * 0.45f
        ),
        topLeft = Offset(centerX + scaledWidth * 0.1f, footY),
        size = androidx.compose.ui.geometry.Size(scaledWidth * 0.35f, 10f)
    )
}

private fun DrawScope.drawPetExpression(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    state: PetState,
    blinkProgress: Float,
    eyeRatio: Float = 0.3f
) {
    // 根据成长阶段调整眼睛大小
    val eyeRadius = width * 0.09f * (eyeRatio / 0.3f)
    val eyeSpacing = width * 0.38f
    val eyeY = centerY - height * 0.1f
    val eyebrowY = eyeY - eyeRadius * 2.5f

    // 绘制眉毛
    drawEyebrows(centerX, eyebrowY, eyeSpacing, state)

    // 绘制眼睛
    drawEyes(centerX, eyeY, eyeSpacing, eyeRadius, state, blinkProgress)

    // 绘制嘴巴
    val mouthY = centerY + height * 0.25f
    drawMouth(centerX, mouthY, width, state)

    // 腮红
    if (state == PetState.HAPPY || state == PetState.EXCITED) {
        drawCircle(
            color = Color(0xFFFF8A80).copy(alpha = 0.4f),
            radius = eyeRadius * 1.3f,
            center = Offset(centerX - eyeSpacing * 1.4f, centerY + height * 0.08f)
        )
        drawCircle(
            color = Color(0xFFFF8A80).copy(alpha = 0.4f),
            radius = eyeRadius * 1.3f,
            center = Offset(centerX + eyeSpacing * 1.4f, centerY + height * 0.08f)
        )
    }
}

private fun DrawScope.drawEyebrows(
    centerX: Float,
    y: Float,
    spacing: Float,
    state: PetState
) {
    val browWidth = spacing * 0.32f
    val browColor = Color(0xFF4E342E)
    val browStroke = Stroke(width = 2.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)

    when (state) {
        PetState.HAPPY -> {
            // 微微上扬 - 贝塞尔曲线
            val leftBrow = Path().apply {
                moveTo(centerX - spacing - browWidth, y + 2f)
                quadraticBezierTo(
                    centerX - spacing, y - 2.5f,
                    centerX - spacing + browWidth, y - 1.5f
                )
            }
            drawPath(leftBrow, browColor, style = browStroke)

            val rightBrow = Path().apply {
                moveTo(centerX + spacing - browWidth, y - 1.5f)
                quadraticBezierTo(
                    centerX + spacing, y - 2.5f,
                    centerX + spacing + browWidth, y + 2f
                )
            }
            drawPath(rightBrow, browColor, style = browStroke)
        }
        PetState.EXCITED -> {
            // 大幅上扬 - 更夸张的弧度
            val leftBrow = Path().apply {
                moveTo(centerX - spacing - browWidth, y + 3f)
                quadraticBezierTo(
                    centerX - spacing, y - 6f,
                    centerX - spacing + browWidth, y - 4f
                )
            }
            drawPath(leftBrow, browColor, style = browStroke)

            val rightBrow = Path().apply {
                moveTo(centerX + spacing - browWidth, y - 4f)
                quadraticBezierTo(
                    centerX + spacing, y - 6f,
                    centerX + spacing + browWidth, y + 3f
                )
            }
            drawPath(rightBrow, browColor, style = browStroke)
        }
        PetState.SAD -> {
            // 下垂 - 内高外低的弧线
            val leftBrow = Path().apply {
                moveTo(centerX - spacing - browWidth, y + 3.5f)
                quadraticBezierTo(
                    centerX - spacing, y + 0.5f,
                    centerX - spacing + browWidth, y - 2f
                )
            }
            drawPath(leftBrow, browColor, style = browStroke)

            val rightBrow = Path().apply {
                moveTo(centerX + spacing - browWidth, y - 2f)
                quadraticBezierTo(
                    centerX + spacing, y + 0.5f,
                    centerX + spacing + browWidth, y + 3.5f
                )
            }
            drawPath(rightBrow, browColor, style = browStroke)
        }
        PetState.WORRIED -> {
            // 皱眉 - 内八字弧线
            val leftBrow = Path().apply {
                moveTo(centerX - spacing - browWidth, y + 2f)
                quadraticBezierTo(
                    centerX - spacing + browWidth * 0.3f, y - 2f,
                    centerX - spacing + browWidth, y + 1f
                )
            }
            drawPath(leftBrow, browColor, style = browStroke)

            val rightBrow = Path().apply {
                moveTo(centerX + spacing - browWidth, y + 1f)
                quadraticBezierTo(
                    centerX + spacing - browWidth * 0.3f, y - 2f,
                    centerX + spacing + browWidth, y + 2f
                )
            }
            drawPath(rightBrow, browColor, style = browStroke)
        }
        PetState.CURIOUS -> {
            // 一边高一边低 - 不对称弧线
            val leftBrow = Path().apply {
                moveTo(centerX - spacing - browWidth, y + 1.5f)
                quadraticBezierTo(
                    centerX - spacing, y - 3.5f,
                    centerX - spacing + browWidth, y - 3f
                )
            }
            drawPath(leftBrow, browColor, style = browStroke)

            val rightBrow = Path().apply {
                moveTo(centerX + spacing - browWidth, y + 2.5f)
                quadraticBezierTo(
                    centerX + spacing, y + 2f,
                    centerX + spacing + browWidth, y + 1f
                )
            }
            drawPath(rightBrow, browColor, style = browStroke)
        }
        else -> {
            // 平静 - 微微自然弧度
            val leftBrow = Path().apply {
                moveTo(centerX - spacing - browWidth, y + 0.5f)
                quadraticBezierTo(
                    centerX - spacing, y - 1f,
                    centerX - spacing + browWidth, y + 0.5f
                )
            }
            drawPath(leftBrow, browColor, style = browStroke)

            val rightBrow = Path().apply {
                moveTo(centerX + spacing - browWidth, y + 0.5f)
                quadraticBezierTo(
                    centerX + spacing, y - 1f,
                    centerX + spacing + browWidth, y + 0.5f
                )
            }
            drawPath(rightBrow, browColor, style = browStroke)
        }
    }
}

private fun DrawScope.drawEyes(
    centerX: Float,
    y: Float,
    spacing: Float,
    radius: Float,
    state: PetState,
    blinkProgress: Float
) {
    val eyeColor = Color(0xFF37474F)
    val pupilColor = Color(0xFF1A1A2E)
    val irisColor = Color(0xFF2E4057)

    when (state) {
        PetState.SLEEPY, PetState.TIRED -> {
            // 眯眼 - 带微妙弧度的线条
            val sleepPath = Path().apply {
                moveTo(centerX - spacing - radius, y)
                quadraticBezierTo(
                    centerX - spacing, y + 1.5f,
                    centerX - spacing + radius, y
                )
            }
            drawPath(sleepPath, eyeColor, style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            val sleepPath2 = Path().apply {
                moveTo(centerX + spacing - radius, y)
                quadraticBezierTo(
                    centerX + spacing, y + 1.5f,
                    centerX + spacing + radius, y
                )
            }
            drawPath(sleepPath2, eyeColor, style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        PetState.CURIOUS -> {
            // 不对称眼睛
            val leftSize = radius * 0.85f
            val rightSize = radius * 1.25f

            // 左眼
            drawCircle(eyeColor, leftSize, Offset(centerX - spacing, y))
            drawCircle(irisColor, leftSize * 0.65f, Offset(centerX - spacing, y))
            drawCircle(pupilColor, leftSize * 0.38f, Offset(centerX - spacing, y))
            // 高光
            drawCircle(Color.White, leftSize * 0.28f, Offset(centerX - spacing + leftSize * 0.2f, y - leftSize * 0.25f))
            drawCircle(Color.White.copy(alpha = 0.5f), leftSize * 0.12f, Offset(centerX - spacing - leftSize * 0.15f, y + leftSize * 0.15f))

            // 右眼
            drawCircle(eyeColor, rightSize, Offset(centerX + spacing, y))
            drawCircle(irisColor, rightSize * 0.65f, Offset(centerX + spacing, y))
            drawCircle(pupilColor, rightSize * 0.38f, Offset(centerX + spacing, y))
            // 高光
            drawCircle(Color.White, rightSize * 0.28f, Offset(centerX + spacing + rightSize * 0.2f, y - rightSize * 0.25f))
            drawCircle(Color.White.copy(alpha = 0.5f), rightSize * 0.12f, Offset(centerX + spacing - rightSize * 0.15f, y + rightSize * 0.15f))
        }
        else -> {
            // 标准眼睛
            val eyeSize = when (state) {
                PetState.HAPPY, PetState.EXCITED -> radius * 1.2f
                PetState.SAD -> radius * 0.85f
                else -> radius
            }

            // 眨眼效果 - 带 smoothstep 眼皮过渡
            val blinkAmount = when {
                blinkProgress < 0.05f -> {
                    // 快速闭合
                    val t = blinkProgress / 0.05f
                    1f - t * 0.9f
                }
                blinkProgress < 0.12f -> {
                    // 保持闭合
                    0.1f
                }
                blinkProgress < 0.22f -> {
                    // 平滑睁开 (smoothstep easing)
                    val t = ((blinkProgress - 0.12f) / 0.10f).coerceIn(0f, 1f)
                    val easedT = t * t * (3f - 2f * t)
                    0.1f + easedT * 0.9f
                }
                else -> 1f
            }

            val eyeHalfWidth = eyeSize
            val eyeHalfHeight = eyeSize * blinkAmount

            // 眼睛主体 - 带微弱渐变
            val eyeGradient = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4A6070),
                    eyeColor
                ),
                center = Offset(centerX - spacing, y),
                radius = eyeHalfWidth
            )
            drawOval(
                brush = eyeGradient,
                topLeft = Offset(centerX - spacing - eyeHalfWidth, y - eyeHalfHeight),
                size = androidx.compose.ui.geometry.Size(eyeHalfWidth * 2, eyeHalfHeight * 2)
            )
            drawOval(
                brush = eyeGradient,
                topLeft = Offset(centerX + spacing - eyeHalfWidth, y - eyeHalfHeight),
                size = androidx.compose.ui.geometry.Size(eyeHalfWidth * 2, eyeHalfHeight * 2)
            )

            // 瞳孔 + 虹膜 (眨眼时隐藏)
            if (blinkAmount > 0.5f) {
                val pupilSize = eyeSize * 0.35f
                val irisSize = eyeSize * 0.55f

                // 左眼虹膜
                drawCircle(irisColor, irisSize, Offset(centerX - spacing, y + eyeSize * 0.05f))
                // 左眼瞳孔
                drawCircle(pupilColor, pupilSize, Offset(centerX - spacing, y + eyeSize * 0.05f))

                // 右眼虹膜
                drawCircle(irisColor, irisSize, Offset(centerX + spacing, y + eyeSize * 0.05f))
                // 右眼瞳孔
                drawCircle(pupilColor, pupilSize, Offset(centerX + spacing, y + eyeSize * 0.05f))
            }

            // 高光 (多层，眨眼时缩小)
            if (blinkAmount > 0.5f) {
                // 主高光
                val hlSize = eyeSize * 0.22f
                drawCircle(Color.White, hlSize, Offset(centerX - spacing + eyeSize * 0.2f, y - eyeSize * 0.22f))
                drawCircle(Color.White, hlSize, Offset(centerX + spacing + eyeSize * 0.2f, y - eyeSize * 0.22f))

                // 次高光 (小光斑)
                val hl2Size = eyeSize * 0.09f
                drawCircle(Color.White.copy(alpha = 0.55f), hl2Size, Offset(centerX - spacing - eyeSize * 0.12f, y + eyeSize * 0.15f))
                drawCircle(Color.White.copy(alpha = 0.55f), hl2Size, Offset(centerX + spacing - eyeSize * 0.12f, y + eyeSize * 0.15f))
            }

            // 微妙的睫毛效果 (上边缘)
            if (blinkAmount > 0.7f) {
                val lashColor = Color(0xFF2A3A4A)
                // 左眼睫毛
                drawLine(
                    color = lashColor,
                    start = Offset(centerX - spacing - eyeSize * 0.6f, y - eyeSize * 0.7f),
                    end = Offset(centerX - spacing - eyeSize * 0.3f, y - eyeSize * 0.85f),
                    strokeWidth = 1.5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = lashColor,
                    start = Offset(centerX - spacing + eyeSize * 0.1f, y - eyeSize * 0.8f),
                    end = Offset(centerX - spacing + eyeSize * 0.35f, y - eyeSize * 0.85f),
                    strokeWidth = 1.5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // 右眼睫毛
                drawLine(
                    color = lashColor,
                    start = Offset(centerX + spacing - eyeSize * 0.35f, y - eyeSize * 0.85f),
                    end = Offset(centerX + spacing - eyeSize * 0.1f, y - eyeSize * 0.8f),
                    strokeWidth = 1.5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = lashColor,
                    start = Offset(centerX + spacing + eyeSize * 0.3f, y - eyeSize * 0.85f),
                    end = Offset(centerX + spacing + eyeSize * 0.6f, y - eyeSize * 0.7f),
                    strokeWidth = 1.5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawMouth(
    centerX: Float,
    y: Float,
    width: Float,
    state: PetState
) {
    val mouthWidth = width * 0.25f
    val mouthPath = Path()
    val mouthColor = Color(0xFF5D4037)

    when (state) {
        PetState.HAPPY, PetState.EXCITED -> {
            // D形微笑
            mouthPath.moveTo(centerX - mouthWidth, y)
            mouthPath.quadraticBezierTo(centerX, y + mouthWidth * 0.7f, centerX + mouthWidth, y)
            mouthPath.lineTo(centerX + mouthWidth, y + 2f)
            mouthPath.quadraticBezierTo(centerX, y + mouthWidth * 0.5f, centerX - mouthWidth, y + 2f)
            mouthPath.close()
            drawPath(mouthPath, mouthColor)
        }
        PetState.SAD -> {
            // 倒U形
            mouthPath.moveTo(centerX - mouthWidth * 0.7f, y + 4f)
            mouthPath.quadraticBezierTo(centerX, y - mouthWidth * 0.3f, centerX + mouthWidth * 0.7f, y + 4f)
            drawPath(mouthPath, mouthColor, style = Stroke(width = 2.5f))
        }
        PetState.WORRIED -> {
            // 波浪线
            mouthPath.moveTo(centerX - mouthWidth * 0.5f, y + 2f)
            mouthPath.quadraticBezierTo(centerX - mouthWidth * 0.15f, y + 6f, centerX, y + 2f)
            mouthPath.quadraticBezierTo(centerX + mouthWidth * 0.15f, y - 2f, centerX + mouthWidth * 0.5f, y + 2f)
            drawPath(mouthPath, mouthColor, style = Stroke(width = 2f))
        }
        PetState.CURIOUS -> {
            // 小圆圈"o"
            drawCircle(mouthColor.copy(alpha = 0.8f), mouthWidth * 0.2f, Offset(centerX, y + 3f))
        }
        PetState.SLEEPY -> {
            // 哈欠椭圆
            drawOval(mouthColor, Offset(centerX - mouthWidth * 0.25f, y), androidx.compose.ui.geometry.Size(mouthWidth * 0.5f, mouthWidth * 0.35f))
        }
        PetState.TIRED -> {
            // 短横线
            drawLine(mouthColor, Offset(centerX - mouthWidth * 0.3f, y + 3f), Offset(centerX + mouthWidth * 0.3f, y + 3f), 2.5f)
        }
        else -> {
            // 平静微笑
            mouthPath.moveTo(centerX - mouthWidth * 0.5f, y)
            mouthPath.quadraticBezierTo(centerX, y + mouthWidth * 0.3f, centerX + mouthWidth * 0.5f, y)
            drawPath(mouthPath, mouthColor, style = Stroke(width = 2.5f))
        }
    }
}

private fun DrawScope.drawStateEffects(
    centerX: Float,
    centerY: Float,
    width: Float,
    state: PetState
) {
    when (state) {
        PetState.EXCITED -> {
            // 星星特效 - 带光晕和大小变化
            val starColor = Color(0xFFFFD700)
            for (i in 0..5) {
                val angle = i * 60f
                val distance = width * 1.5f
                val x = centerX + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance
                val y = centerY - width * 0.3f + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance * 0.5f
                val starSize = 3.5f + (i % 2) * 2f
                drawStar(x, y, starSize, starColor)
            }
            // 额外的小星星装饰
            for (i in 0..2) {
                val angle = i * 120f + 30f
                val distance = width * 1.8f
                val x = centerX + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance
                val y = centerY - width * 0.2f + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance * 0.4f
                drawStar(x, y, 2f, starColor.copy(alpha = 0.6f))
            }
        }
        PetState.HAPPY -> {
            // 爱心特效 - 带大小渐变
            val heartColor = Color(0xFFFF80AB)
            for (i in 0..3) {
                val x = centerX - width * 0.8f + i * width * 0.5f
                val y = centerY - width * 0.9f - i * 10f
                val heartSize = 4f + (i % 2) * 2f
                drawHeart(x, y, heartSize, heartColor)
            }
        }
        PetState.SAD -> {
            // 雨滴特效 - 带间距变化
            val rainColor = Color(0xFF90CAF9)
            for (i in 0..2) {
                val x = centerX - width * 0.4f + i * width * 0.4f
                val y = centerY - width * 0.9f - (i % 2) * 5f
                drawRainDrop(x, y, rainColor)
            }
        }
        PetState.WORRIED -> {
            // 汗滴 - 带微妙的大小变化
            drawRainDrop(centerX + width * 0.55f, centerY - width * 0.7f, Color(0xFF90CAF9))
            drawRainDrop(centerX + width * 0.7f, centerY - width * 0.55f, Color(0xFF90CAF9).copy(alpha = 0.6f))
        }
        PetState.CURIOUS -> {
            // 问号 - 带光晕
            drawQuestionMark(centerX + width * 0.6f, centerY - width * 0.8f, Color(0xFFD4A06A))
        }
        PetState.SLEEPY -> {
            // Zzz - 带渐隐效果
            drawZzz(centerX + width * 0.6f, centerY - width * 0.5f, Color(0xFF8899AA))
        }
        PetState.TIRED -> {
            // 叹气气泡 - 带渐变和光晕
            val bubbleColor = Color(0xFF9088A8)
            // 大气泡
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bubbleColor.copy(alpha = 0.5f),
                        bubbleColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(centerX + width * 0.5f, centerY - width * 0.3f),
                    radius = 8f
                ),
                radius = 8f,
                center = Offset(centerX + width * 0.5f, centerY - width * 0.3f)
            )
            // 小气泡
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bubbleColor.copy(alpha = 0.35f),
                        bubbleColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(centerX + width * 0.7f, centerY - width * 0.5f),
                    radius = 5f
                ),
                radius = 5f,
                center = Offset(centerX + width * 0.7f, centerY - width * 0.5f)
            )
            // 微小气泡
            drawCircle(
                color = bubbleColor.copy(alpha = 0.2f),
                radius = 2.5f,
                center = Offset(centerX + width * 0.85f, centerY - width * 0.65f)
            )
        }
        else -> {}
    }
}

private fun DrawScope.drawStar(x: Float, y: Float, size: Float, color: Color) {
    // 星星光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.25f),
                color.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = size * 2.2f
        ),
        radius = size * 2.2f,
        center = Offset(x, y)
    )

    // 星星主体
    val path = Path().apply {
        moveTo(x, y - size)
        lineTo(x + size * 0.28f, y - size * 0.28f)
        lineTo(x + size, y)
        lineTo(x + size * 0.28f, y + size * 0.28f)
        lineTo(x, y + size)
        lineTo(x - size * 0.28f, y + size * 0.28f)
        lineTo(x - size, y)
        lineTo(x - size * 0.28f, y - size * 0.28f)
        close()
    }
    drawPath(path, color)

    // 星星中心高光
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = size * 0.25f,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawHeart(x: Float, y: Float, size: Float, color: Color) {
    // 爱心光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.2f),
                Color.Transparent
            ),
            center = Offset(x, y - size * 0.15f),
            radius = size * 1.5f
        ),
        radius = size * 1.5f,
        center = Offset(x, y - size * 0.15f)
    )

    // 爱心主体 - 更精致的形状
    val path = Path().apply {
        moveTo(x, y + size * 0.35f)
        // 左侧曲线
        cubicTo(
            x - size * 0.05f, y + size * 0.1f,
            x - size * 0.55f, y - size * 0.15f,
            x - size * 0.55f, y - size * 0.45f
        )
        cubicTo(
            x - size * 0.55f, y - size * 0.78f,
            x - size * 0.1f, y - size * 0.75f,
            x, y - size * 0.45f
        )
        // 右侧曲线
        cubicTo(
            x + size * 0.1f, y - size * 0.75f,
            x + size * 0.55f, y - size * 0.78f,
            x + size * 0.55f, y - size * 0.45f
        )
        cubicTo(
            x + size * 0.55f, y - size * 0.15f,
            x + size * 0.05f, y + size * 0.1f,
            x, y + size * 0.35f
        )
        close()
    }

    // 渐变填充
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color,
                color.copy(alpha = 0.85f)
            ),
            startY = y - size * 0.6f,
            endY = y + size * 0.3f
        )
    )

    // 高光
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = size * 0.12f,
        center = Offset(x - size * 0.2f, y - size * 0.35f)
    )
}

private fun DrawScope.drawRainDrop(x: Float, y: Float, color: Color) {
    // 雨滴光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(x, y + 4f),
            radius = 8f
        ),
        radius = 8f,
        center = Offset(x, y + 4f)
    )

    // 雨滴主体 - 更精细的形状
    val path = Path().apply {
        moveTo(x, y)
        cubicTo(
            x + 1.5f, y + 2.5f,
            x + 3.5f, y + 5f,
            x + 2f, y + 7f
        )
        quadraticBezierTo(x, y + 9f, x - 2f, y + 7f)
        cubicTo(
            x - 3.5f, y + 5f,
            x - 1.5f, y + 2.5f,
            x, y
        )
        close()
    }

    // 渐变填充
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 0.9f),
                color
            ),
            startY = y,
            endY = y + 8f
        )
    )

    // 高光
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = 1.2f,
        center = Offset(x - 0.8f, y + 3f)
    )
}

private fun DrawScope.drawQuestionMark(x: Float, y: Float, color: Color) {
    // 问号主体
    val path = Path().apply {
        moveTo(x, y - 8f)
        quadraticBezierTo(x + 5f, y - 8f, x + 5f, y - 3f)
        quadraticBezierTo(x + 5f, y + 2f, x, y + 2f)
        lineTo(x, y + 5f)
    }
    drawPath(path, color, style = Stroke(width = 2.5f))
    // 问号点
    drawCircle(color, 2f, Offset(x, y + 8f))
}

private fun DrawScope.drawZzz(x: Float, y: Float, color: Color) {
    // Z字母
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x + 6f, y)
        lineTo(x, y + 6f)
        lineTo(x + 6f, y + 6f)
    }
    drawPath(path, color, style = Stroke(width = 2f))

    // 小z
    drawPath(Path().apply {
        moveTo(x + 8f, y - 3f)
        lineTo(x + 12f, y - 3f)
        lineTo(x + 8f, y + 1f)
        lineTo(x + 12f, y + 1f)
    }, color.copy(alpha = 0.7f), style = Stroke(width = 1.5f))

    // 更小z
    drawPath(Path().apply {
        moveTo(x + 14f, y - 6f)
        lineTo(x + 17f, y - 6f)
        lineTo(x + 14f, y - 3f)
        lineTo(x + 17f, y - 3f)
    }, color.copy(alpha = 0.5f), style = Stroke(width = 1f))
}

private fun DrawScope.drawHeartBurst(
    centerX: Float,
    centerY: Float,
    particles: List<HeartParticle>,
    progress: Float
) {
    val heartColor = Color(0xFFFF80AB)
    for (particle in particles) {
        val angleRad = Math.toRadians(particle.angle.toDouble()).toFloat()
        val distance = particle.distance * progress
        val x = centerX + kotlin.math.cos(angleRad) * distance
        val y = centerY + kotlin.math.sin(angleRad) * distance - progress * 30f
        val alpha = (1f - progress).coerceIn(0f, 1f)
        drawHeart(x, y, particle.size, heartColor.copy(alpha = alpha))
    }
}

// ==================== 小岛等级外观装饰 ====================

/**
 * 头顶小星星装饰 - Lv5-9
 */
private fun DrawScope.drawStarDecoration(
    centerX: Float,
    centerY: Float,
    height: Float
) {
    val starColor = Color(0xFFFFD700)
    val topY = centerY - height * 0.5f - 15f
    drawStar(centerX, topY, 6f, starColor)
    drawStar(centerX - 12f, topY + 5f, 4f, starColor.copy(alpha = 0.7f))
    drawStar(centerX + 12f, topY + 5f, 4f, starColor.copy(alpha = 0.7f))
}

/**
 * 身体光环效果 - Lv10-14
 */
private fun DrawScope.drawHaloEffect(
    centerX: Float,
    centerY: Float,
    width: Float,
    breathScale: Float
) {
    val haloRadius = width * 1.6f * breathScale
    val haloColor = Color(0xFFFFD700).copy(alpha = 0.15f)
    drawCircle(
        color = haloColor,
        radius = haloRadius,
        center = Offset(centerX, centerY)
    )
    val innerHaloColor = Color(0xFFFFD700).copy(alpha = 0.08f)
    drawCircle(
        color = innerHaloColor,
        radius = haloRadius * 0.7f,
        center = Offset(centerX, centerY)
    )
}

/**
 * 翅膀装饰 - Lv15+
 */
private fun DrawScope.drawWings(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float
) {
    val wingColor = Color.White.copy(alpha = 0.6f)
    val wingHeight = height * 0.4f
    val wingY = centerY - height * 0.1f

    // 左翅膀
    val leftWing = Path().apply {
        moveTo(centerX - width * 0.6f, wingY)
        cubicTo(
            x1 = centerX - width * 1.2f, y1 = wingY - wingHeight,
            x2 = centerX - width * 1.5f, y2 = wingY - wingHeight * 0.3f,
            x3 = centerX - width * 0.8f, y3 = wingY + wingHeight * 0.3f
        )
        close()
    }
    drawPath(leftWing, wingColor)

    // 右翅膀
    val rightWing = Path().apply {
        moveTo(centerX + width * 0.6f, wingY)
        cubicTo(
            x1 = centerX + width * 1.2f, y1 = wingY - wingHeight,
            x2 = centerX + width * 1.5f, y2 = wingY - wingHeight * 0.3f,
            x3 = centerX + width * 0.8f, y3 = wingY + wingHeight * 0.3f
        )
        close()
    }
    drawPath(rightWing, wingColor)

    // 翅膀高光
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = 3f,
        center = Offset(centerX - width * 1.0f, wingY - wingHeight * 0.5f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = 3f,
        center = Offset(centerX + width * 1.0f, wingY - wingHeight * 0.5f)
    )
}

// ==================== 成长阶段装饰 ====================

/**
 * 成长期: 头顶呆毛
 */
private fun DrawScope.drawHairAntenna(
    centerX: Float,
    centerY: Float,
    height: Float,
    bodyColor: Color
) {
    val topY = centerY - height * 0.5f
    val antennaColor = bodyColor.copy(alpha = 0.9f)

    // 呆毛主体 - 一条弯曲的线
    val antennaPath = Path().apply {
        moveTo(centerX, topY - 5f)
        quadraticBezierTo(
            centerX + 12f, topY - 25f,
            centerX + 8f, topY - 35f
        )
    }
    drawPath(antennaPath, antennaColor, style = Stroke(width = 3f))

    // 呆毛顶端小圆点
    drawCircle(
        color = antennaColor,
        radius = 4f,
        center = Offset(centerX + 8f, topY - 35f)
    )
}

/**
 * 成熟期: 身体光晕
 */
private fun DrawScope.drawGlowEffect(
    centerX: Float,
    centerY: Float,
    width: Float,
    breathScale: Float,
    bodyColor: Color
) {
    val glowRadius = width * 1.8f * breathScale

    // 外层光晕
    drawCircle(
        color = bodyColor.copy(alpha = 0.08f),
        radius = glowRadius,
        center = Offset(centerX, centerY)
    )

    // 中层光晕
    drawCircle(
        color = bodyColor.copy(alpha = 0.12f),
        radius = glowRadius * 0.7f,
        center = Offset(centerX, centerY)
    )

    // 内层光晕
    drawCircle(
        color = bodyColor.copy(alpha = 0.18f),
        radius = glowRadius * 0.4f,
        center = Offset(centerX, centerY)
    )
}

// ==================== 隐藏状态视觉效果 ====================

/**
 * 绘制隐藏状态的特殊效果
 */
private fun DrawScope.drawHiddenStateEffects(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    hiddenState: PetHiddenStateType,
    particleProgress: Float,
    colorTransition: Float,
    breathScale: Float
) {
    when (hiddenState) {
        PetHiddenStateType.NIGHT_OWL -> {
            // 夜猫子: 头顶小月亮
            drawMoon(centerX, centerY - height * 0.7f, 8f)
        }
        PetHiddenStateType.TREASURE_HUNTER -> {
            // 宝藏猎人: 身体周围漂浮小光点
            drawFloatingLightParticles(centerX, centerY, width, particleProgress)
        }
        PetHiddenStateType.WARM_GUARDIAN -> {
            // 暖心守护者: 暖黄色光晕
            drawWarmGlowEffect(centerX, centerY, width, breathScale)
        }
        PetHiddenStateType.DEEP_DIVER -> {
            // 深海潜水员: 小气泡
            drawBubbles(centerX, centerY, width, particleProgress)
        }
        PetHiddenStateType.TIME_TRAVELER -> {
            // 时间旅人: 重影效果
            drawGhostEffect(centerX, centerY, width, height, colorTransition)
        }
    }
}

/**
 * 夜猫子: 绘制月牙形眼睛
 */
private fun DrawScope.drawCrescentMoonEyes(
    centerX: Float,
    y: Float,
    spacing: Float,
    radius: Float
) {
    val eyeColor = Color(0xFF1A237E)

    // 左眼月牙
    drawCrescentMoon(centerX - spacing, y, radius, eyeColor)
    // 右眼月牙
    drawCrescentMoon(centerX + spacing, y, radius, eyeColor)
}

/**
 * 绘制月牙形状
 */
private fun DrawScope.drawCrescentMoon(x: Float, y: Float, radius: Float, color: Color) {
    val path = Path().apply {
        // 外圆弧
        addArc(
            oval = androidx.compose.ui.geometry.Rect(
                left = x - radius,
                top = y - radius,
                right = x + radius,
                bottom = y + radius
            ),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 360f
        )
        // 内圆弧（挖空部分）- 使用反方向
        addArc(
            oval = androidx.compose.ui.geometry.Rect(
                left = x - radius * 0.3f,
                top = y - radius * 0.9f,
                right = x + radius * 0.7f,
                bottom = y + radius * 0.9f
            ),
            startAngleDegrees = -90f,
            sweepAngleDegrees = -360f
        )
    }
    drawPath(path, color)
}

/**
 * 夜猫子: 绘制头顶月亮
 */
private fun DrawScope.drawMoon(x: Float, y: Float, radius: Float) {
    val moonColor = Color(0xFFFFF176)

    // 月亮主体
    drawCrescentMoon(x, y, radius, moonColor)

    // 月亮高光
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = radius * 0.3f,
        center = Offset(x - radius * 0.2f, y - radius * 0.3f)
    )
}

/**
 * 宝藏猎人: 绘制星星眼睛
 */
private fun DrawScope.drawStarEyes(
    centerX: Float,
    y: Float,
    spacing: Float,
    radius: Float
) {
    val eyeColor = Color(0xFFFFD700)

    // 左眼星星
    drawStar(centerX - spacing, y, radius * 1.5f, eyeColor)
    // 右眼星星
    drawStar(centerX + spacing, y, radius * 1.5f, eyeColor)

    // 星星高光
    drawCircle(Color.White, radius * 0.3f, Offset(centerX - spacing, y - radius * 0.3f))
    drawCircle(Color.White, radius * 0.3f, Offset(centerX + spacing, y - radius * 0.3f))
}

/**
 * 宝藏猎人: 绘制漂浮光点
 */
private fun DrawScope.drawFloatingLightParticles(
    centerX: Float,
    centerY: Float,
    width: Float,
    progress: Float
) {
    val particleCount = 8
    val particleColor = Color(0xFFFFD700)

    for (i in 0 until particleCount) {
        val angle = (360f / particleCount) * i + progress * 360f
        val distance = width * (1.2f + 0.3f * kotlin.math.sin(progress * Math.PI * 2 + i).toFloat())
        val x = centerX + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance
        val y = centerY + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance * 0.6f
        val size = 2f + kotlin.math.sin(progress * Math.PI * 2 + i * 0.5f).toFloat() * 1.5f
        val alpha = 0.5f + 0.5f * kotlin.math.sin(progress * Math.PI * 2 + i).toFloat()

        drawCircle(
            color = particleColor.copy(alpha = alpha),
            radius = size,
            center = Offset(x, y)
        )
    }
}

/**
 * 暖心守护者: 绘制暖黄色光晕
 */
private fun DrawScope.drawWarmGlowEffect(
    centerX: Float,
    centerY: Float,
    width: Float,
    breathScale: Float
) {
    val glowRadius = width * 2.0f * breathScale
    val warmColor = Color(0xFFFFC107)

    // 外层暖黄光晕
    drawCircle(
        color = warmColor.copy(alpha = 0.1f),
        radius = glowRadius,
        center = Offset(centerX, centerY)
    )

    // 中层暖黄光晕
    drawCircle(
        color = warmColor.copy(alpha = 0.15f),
        radius = glowRadius * 0.7f,
        center = Offset(centerX, centerY)
    )

    // 内层暖黄光晕
    drawCircle(
        color = warmColor.copy(alpha = 0.2f),
        radius = glowRadius * 0.4f,
        center = Offset(centerX, centerY)
    )
}

/**
 * 深海潜水员: 绘制气泡
 */
private fun DrawScope.drawBubbles(
    centerX: Float,
    centerY: Float,
    width: Float,
    progress: Float
) {
    val bubbleColor = Color(0xFF81D4FA)

    for (i in 0..5) {
        val angle = (60f * i) + progress * 360f
        val distance = width * (1.0f + 0.5f * i * 0.2f)
        val x = centerX + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance
        val y = centerY - size.height * 0.3f - i * 12f - progress * 20f
        val size = 3f + i * 1.5f
        val alpha = (0.6f - i * 0.1f).coerceAtLeast(0.2f)

        // 气泡主体
        drawCircle(
            color = bubbleColor.copy(alpha = alpha),
            radius = size,
            center = Offset(x, y)
        )

        // 气泡高光
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.8f),
            radius = size * 0.3f,
            center = Offset(x - size * 0.2f, y - size * 0.2f)
        )
    }
}

/**
 * 时间旅人: 绘制重影效果
 */
private fun DrawScope.drawGhostEffect(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    colorTransition: Float
) {
    // 左侧重影
    drawCircle(
        color = Color(0xFF7B1FA2).copy(alpha = 0.15f),
        radius = width * 0.8f,
        center = Offset(centerX - width * 0.3f, centerY + 5f)
    )

    // 右侧重影
    drawCircle(
        color = Color(0xFF00BCD4).copy(alpha = 0.15f),
        radius = width * 0.8f,
        center = Offset(centerX + width * 0.3f, centerY - 5f)
    )
}

// ==================== 称号组合效果 ====================

/**
 * 绘制称号组合效果
 */
private fun DrawScope.drawCombinationEffects(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    effects: List<CombinationEffect>,
    breathScale: Float
) {
    for (effect in effects) {
        when (effect) {
            CombinationEffect.WISDOM_AURA -> drawWisdomAura(centerX, centerY, height, breathScale)
            CombinationEffect.WARM_GLOW -> drawWarmGlow(centerX, centerY, width, breathScale)
            CombinationEffect.ADVENTURE_BADGE -> drawAdventureBadge(centerX, centerY, width, height)
            CombinationEffect.PERSISTENCE_AURA -> drawPersistenceAura(centerX, centerY, width, breathScale)
        }
    }
}

/**
 * 智慧光环 - 头顶出现小灯泡
 */
private fun DrawScope.drawWisdomAura(
    centerX: Float,
    centerY: Float,
    height: Float,
    breathScale: Float
) {
    val bulbY = centerY - height * 0.7f - 10f
    val bulbColor = Color(0xFFFFEB3B)

    // 灯泡光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bulbColor.copy(alpha = 0.3f),
                bulbColor.copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = Offset(centerX, bulbY),
            radius = 20f * breathScale
        ),
        radius = 20f * breathScale,
        center = Offset(centerX, bulbY)
    )

    // 灯泡主体
    drawCircle(
        color = bulbColor,
        radius = 8f,
        center = Offset(centerX, bulbY)
    )

    // 灯泡高光
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = 3f,
        center = Offset(centerX - 2f, bulbY - 2f)
    )

    // 灯泡底座
    drawRect(
        color = Color(0xFF9E9E9E),
        topLeft = Offset(centerX - 4f, bulbY + 6f),
        size = androidx.compose.ui.geometry.Size(8f, 4f)
    )
}

/**
 * 温暖光环 - 暖黄色光晕
 */
private fun DrawScope.drawWarmGlow(
    centerX: Float,
    centerY: Float,
    width: Float,
    breathScale: Float
) {
    val glowRadius = width * 2.2f * breathScale
    val warmColor = Color(0xFFFFC107)

    // 外层暖黄光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                warmColor.copy(alpha = 0.15f),
                warmColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(centerX, centerY)
    )

    // 中层暖黄光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                warmColor.copy(alpha = 0.2f),
                warmColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = glowRadius * 0.7f
        ),
        radius = glowRadius * 0.7f,
        center = Offset(centerX, centerY)
    )

    // 内层暖黄光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                warmColor.copy(alpha = 0.25f),
                warmColor.copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = glowRadius * 0.4f
        ),
        radius = glowRadius * 0.4f,
        center = Offset(centerX, centerY)
    )
}

/**
 * 冒险徽章 - 身体周围出现小星星
 */
private fun DrawScope.drawAdventureBadge(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float
) {
    val starColor = Color(0xFFFFD700)
    val starCount = 6
    val radius = width * 1.4f

    for (i in 0 until starCount) {
        val angle = (360f / starCount) * i
        val x = centerX + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * radius
        val y = centerY + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * radius * 0.6f
        val starSize = 4f + (i % 2) * 2f

        drawStar(x, y, starSize, starColor)
    }
}

/**
 * 坚持光环 - 金色光环
 */
private fun DrawScope.drawPersistenceAura(
    centerX: Float,
    centerY: Float,
    width: Float,
    breathScale: Float
) {
    val auraRadius = width * 1.8f * breathScale
    val goldColor = Color(0xFFFFD700)

    // 外层金色光环
    drawCircle(
        color = goldColor.copy(alpha = 0.12f),
        radius = auraRadius,
        center = Offset(centerX, centerY)
    )

    // 中层金色光环
    drawCircle(
        color = goldColor.copy(alpha = 0.18f),
        radius = auraRadius * 0.75f,
        center = Offset(centerX, centerY)
    )

    // 内层金色光环
    drawCircle(
        color = goldColor.copy(alpha = 0.25f),
        radius = auraRadius * 0.5f,
        center = Offset(centerX, centerY)
    )

    // 金色光环边缘
    drawCircle(
        color = goldColor.copy(alpha = 0.3f),
        radius = auraRadius * 0.9f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2f)
    )
}
