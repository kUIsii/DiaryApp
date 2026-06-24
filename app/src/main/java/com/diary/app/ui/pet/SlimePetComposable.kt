package com.diary.app.ui.pet

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.diary.app.data.PetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * 史莱姆宠物 - 水滴形blob设计
 * 支持8种情绪状态、三阶段进化、触摸互动
 */

// 情绪状态
enum class SlimeMood {
    CALM, HAPPY, SLEEPY, WORRIED, SAD, EXCITED, CURIOUS, TIRED
}

// 成长阶段
enum class SlimeStage {
    JUVENILE,  // 0-30天：小水滴，大眼睛40%
    GROWTH,    // 31-90天：有腰线，头发
    MATURE     // 91+天：光滑形态，光晕
}

// 配色方案
data class SlimeColorScheme(
    val bodyColor: Color,
    val bodyColorDark: Color,
    val bodyColorLight: Color,
    val eyeColor: Color,
    val cheekColor: Color,
    val glowColor: Color
) {
    companion object {
        val default = SlimeColorScheme(
            bodyColor = Color(0xFF7EC8E3),
            bodyColorDark = Color(0xFF5BA3C9),
            bodyColorLight = Color(0xFFB8E4F0),
            eyeColor = Color(0xFF2D3436),
            cheekColor = Color(0xFFFFB6C1),
            glowColor = Color(0x407EC8E3)
        )

        val green = SlimeColorScheme(
            bodyColor = Color(0xFF81C784),
            bodyColorDark = Color(0xFF4CAF50),
            bodyColorLight = Color(0xFFC8E6C9),
            eyeColor = Color(0xFF2D3436),
            cheekColor = Color(0xFFFFAB91),
            glowColor = Color(0x4081C784)
        )

        val pink = SlimeColorScheme(
            bodyColor = Color(0xFFF48FB1),
            bodyColorDark = Color(0xFFEC407A),
            bodyColorLight = Color(0xFFF8BBD0),
            eyeColor = Color(0xFF2D3436),
            cheekColor = Color(0xFFFFCDD2),
            glowColor = Color(0x40F48FB1)
        )
    }
}

@Composable
fun SlimePetComposable(
    state: PetState,
    mood: SlimeMood = SlimeMood.CALM,
    stage: SlimeStage = SlimeStage.JUVENILE,
    colorScheme: SlimeColorScheme = SlimeColorScheme.default,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    // 动画状态
    val infiniteTransition = rememberInfiniteTransition(label = "slime")

    // 呼吸动画
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // 弹跳动画
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // 摇晃动画
    val swayX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    // 交互状态
    var isPressed by remember { mutableStateOf(false) }
    var isPoked by remember { mutableStateOf(false) }
    var pokeOffset by remember { mutableStateOf(0f) }

    // 压扁拉伸动画
    val squashStretch by animateFloatAsState(
        targetValue = if (isPoked) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "squash"
    )

    // 粒子效果
    var particles by remember { mutableStateOf(listOf<SlimeParticle>()) }

    // 协程作用域
    val scope = rememberCoroutineScope()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isPoked = true
                        // 添加弹跳粒子
                        particles = particles + SlimeParticle(
                            x = size.width / 2f,
                            y = size.height * 0.6f,
                            type = ParticleType.BOUNCE
                        )
                        onTap()
                        // 重置压扁
                        scope.launch {
                            delay(300)
                            isPoked = false
                        }
                    },
                    onDoubleTap = {
                        particles = particles + SlimeParticle(
                            x = size.width / 2f,
                            y = size.height * 0.4f,
                            type = ParticleType.STAR
                        )
                        onDoubleTap()
                    },
                    onLongPress = {
                        isPressed = true
                        particles = particles + SlimeParticle(
                            x = size.width / 2f,
                            y = size.height * 0.5f,
                            type = ParticleType.HEART
                        )
                        onLongPress()
                        scope.launch {
                            delay(500)
                            isPressed = false
                        }
                    }
                )
            }
    ) {
        val centerX = size.width / 2
        val centerY = size.height * 0.55f
        val bodyWidth = size.width * 0.35f
        val bodyHeight = size.height * 0.4f

        // 更新粒子
        particles = particles.filter { it.update() }

        // 绘制粒子
        particles.forEach { particle ->
            particle.draw(this, colorScheme)
        }

        // 绘制光晕（成熟期）
        if (stage == SlimeStage.MATURE) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.glowColor, Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = bodyWidth * 1.5f
                ),
                radius = bodyWidth * 1.5f,
                center = Offset(centerX, centerY)
            )
        }

        // 绘制身体（水滴形）
        val bodyPath = Path().apply {
            moveTo(centerX, centerY - bodyHeight)
            // 右侧曲线
            cubicTo(
                centerX + bodyWidth * 0.8f, centerY - bodyHeight * 0.8f,
                centerX + bodyWidth, centerY - bodyHeight * 0.2f,
                centerX + bodyWidth * 0.9f, centerY + bodyHeight * 0.3f
            )
            // 右下曲线
            cubicTo(
                centerX + bodyWidth * 0.8f, centerY + bodyHeight * 0.8f,
                centerX + bodyWidth * 0.3f, centerY + bodyHeight,
                centerX, centerY + bodyHeight * 0.9f
            )
            // 左下曲线
            cubicTo(
                centerX - bodyWidth * 0.3f, centerY + bodyHeight,
                centerX - bodyWidth * 0.8f, centerY + bodyHeight * 0.8f,
                centerX - bodyWidth * 0.9f, centerY + bodyHeight * 0.3f
            )
            // 左侧曲线
            cubicTo(
                centerX - bodyWidth, centerY - bodyHeight * 0.2f,
                centerX - bodyWidth * 0.8f, centerY - bodyHeight * 0.8f,
                centerX, centerY - bodyHeight
            )
            close()
        }

        // 身体渐变（上亮下暗）
        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    colorScheme.bodyColorLight,
                    colorScheme.bodyColor,
                    colorScheme.bodyColorDark
                ),
                startY = centerY - bodyHeight,
                endY = centerY + bodyHeight
            )
        )

        // 高光
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(centerX - bodyWidth * 0.3f, centerY - bodyHeight * 0.4f),
                radius = bodyWidth * 0.3f
            ),
            radius = bodyWidth * 0.3f,
            center = Offset(centerX - bodyWidth * 0.3f, centerY - bodyHeight * 0.4f)
        )

        // 次高光
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(centerX + bodyWidth * 0.4f, centerY - bodyHeight * 0.2f),
                radius = bodyWidth * 0.15f
            ),
            radius = bodyWidth * 0.15f,
            center = Offset(centerX + bodyWidth * 0.4f, centerY - bodyHeight * 0.2f)
        )

        // 绘制眼睛
        val eyeWidth = bodyWidth * 0.25f
        val eyeHeight = bodyHeight * 0.35f
        val eyeY = centerY - bodyHeight * 0.15f
        val eyeSpacing = bodyWidth * 0.35f

        // 左眼
        drawOval(
            color = Color.White,
            topLeft = Offset(centerX - eyeSpacing - eyeWidth / 2, eyeY - eyeHeight / 2),
            size = Size(eyeWidth, eyeHeight)
        )
        // 右眼
        drawOval(
            color = Color.White,
            topLeft = Offset(centerX + eyeSpacing - eyeWidth / 2, eyeY - eyeHeight / 2),
            size = Size(eyeWidth, eyeHeight)
        )

        // 眼珠
        val pupilSize = eyeWidth * 0.5f
        val pupilOffset = when (mood) {
            SlimeMood.CURIOUS -> Offset(3f, -2f)
            SlimeMood.WORRIED -> Offset(0f, 2f)
            SlimeMood.SAD -> Offset(0f, 3f)
            else -> Offset(0f, 0f)
        }

        drawCircle(
            color = colorScheme.eyeColor,
            radius = pupilSize,
            center = Offset(centerX - eyeSpacing + pupilOffset.x, eyeY + pupilOffset.y)
        )
        drawCircle(
            color = colorScheme.eyeColor,
            radius = pupilSize,
            center = Offset(centerX + eyeSpacing + pupilOffset.x, eyeY + pupilOffset.y)
        )

        // 眼睛高光
        drawCircle(
            color = Color.White,
            radius = pupilSize * 0.3f,
            center = Offset(centerX - eyeSpacing - 2f, eyeY - 3f)
        )
        drawCircle(
            color = Color.White,
            radius = pupilSize * 0.3f,
            center = Offset(centerX + eyeSpacing - 2f, eyeY - 3f)
        )

        // 眨眼状态
        if (mood == SlimeMood.SLEEPY || mood == SlimeMood.TIRED) {
            drawLine(
                color = colorScheme.eyeColor,
                start = Offset(centerX - eyeSpacing - eyeWidth * 0.3f, eyeY),
                end = Offset(centerX - eyeSpacing + eyeWidth * 0.3f, eyeY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = colorScheme.eyeColor,
                start = Offset(centerX + eyeSpacing - eyeWidth * 0.3f, eyeY),
                end = Offset(centerX + eyeSpacing + eyeWidth * 0.3f, eyeY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // 绘制腮红
        if (mood == SlimeMood.HAPPY || mood == SlimeMood.EXCITED || isPressed) {
            drawCircle(
                color = colorScheme.cheekColor.copy(alpha = 0.5f),
                radius = eyeWidth * 0.4f,
                center = Offset(centerX - eyeSpacing - eyeWidth * 0.6f, eyeY + eyeHeight * 0.5f)
            )
            drawCircle(
                color = colorScheme.cheekColor.copy(alpha = 0.5f),
                radius = eyeWidth * 0.4f,
                center = Offset(centerX + eyeSpacing + eyeWidth * 0.6f, eyeY + eyeHeight * 0.5f)
            )
        }

        // 绘制嘴巴
        val mouthY = centerY + bodyHeight * 0.1f
        val mouthPath = Path().apply {
            when (mood) {
                SlimeMood.HAPPY, SlimeMood.EXCITED -> {
                    // 微笑
                    moveTo(centerX - bodyWidth * 0.15f, mouthY)
                    quadraticBezierTo(centerX, mouthY + bodyHeight * 0.15f, centerX + bodyWidth * 0.15f, mouthY)
                }
                SlimeMood.SAD, SlimeMood.WORRIED -> {
                    // 悲伤
                    moveTo(centerX - bodyWidth * 0.12f, mouthY + bodyHeight * 0.08f)
                    quadraticBezierTo(centerX, mouthY - bodyHeight * 0.05f, centerX + bodyWidth * 0.12f, mouthY + bodyHeight * 0.08f)
                }
                SlimeMood.SLEEPY -> {
                    // 打哈欠
                    addOval(
                        Rect(
                            centerX - bodyWidth * 0.08f,
                            mouthY - bodyHeight * 0.05f,
                            centerX + bodyWidth * 0.08f,
                            mouthY + bodyHeight * 0.1f
                        )
                    )
                }
                else -> {
                    // 平静 - 小嘴
                    moveTo(centerX - bodyWidth * 0.08f, mouthY)
                    quadraticBezierTo(centerX, mouthY + bodyHeight * 0.05f, centerX + bodyWidth * 0.08f, mouthY)
                }
            }
        }
        drawPath(
            path = mouthPath,
            color = colorScheme.eyeColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // 绘制装饰（成长期和成熟期）
        if (stage != SlimeStage.JUVENILE) {
            // 头发/装饰
            val hairPath = Path().apply {
                moveTo(centerX - bodyWidth * 0.2f, centerY - bodyHeight * 0.85f)
                quadraticBezierTo(centerX, centerY - bodyHeight * 1.1f, centerX + bodyWidth * 0.2f, centerY - bodyHeight * 0.85f)
            }
            drawPath(
                path = hairPath,
                color = colorScheme.bodyColorDark,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        // 成熟期光环
        if (stage == SlimeStage.MATURE) {
            val haloPath = Path().apply {
                addArc(
                    oval = Rect(
                        centerX - bodyWidth * 1.1f,
                        centerY - bodyHeight * 1.2f,
                        centerX + bodyWidth * 1.1f,
                        centerY + bodyHeight * 0.5f
                    ),
                    startAngleDegrees = -150f,
                    sweepAngleDegrees = -60f
                )
            }
            drawPath(
                path = haloPath,
                color = colorScheme.glowColor.copy(alpha = 0.6f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        // 绘制特殊表情
        when (mood) {
            SlimeMood.CURIOUS -> {
                // 问号
                val questionX = centerX + bodyWidth * 0.5f
                val questionY = centerY - bodyHeight * 0.7f
                drawCircle(
                    color = colorScheme.eyeColor.copy(alpha = 0.6f),
                    radius = 4f,
                    center = Offset(questionX, questionY)
                )
                drawCircle(
                    color = colorScheme.eyeColor.copy(alpha = 0.6f),
                    radius = 2f,
                    center = Offset(questionX, questionY + 12f)
                )
            }
            SlimeMood.EXCITED -> {
                // 星星
                val starX = centerX + bodyWidth * 0.5f
                val starY = centerY - bodyHeight * 0.6f
                drawStar(
                    center = Offset(starX, starY),
                    color = Color(0xFFFFD700),
                    size = 8f
                )
            }
            SlimeMood.SAD -> {
                // 泪滴
                drawCircle(
                    color = Color(0xFF4FC3F7).copy(alpha = 0.7f),
                    radius = 3f,
                    center = Offset(centerX - eyeSpacing + eyeWidth * 0.3f, eyeY + eyeHeight * 0.6f)
                )
            }
            else -> {}
        }
    }
}

// 粒子系统
data class SlimeParticle(
    val x: Float,
    val y: Float,
    val type: ParticleType,
    var life: Float = 1f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f
) {
    init {
        when (type) {
            ParticleType.BOUNCE -> {
                velocityX = (Math.random() - 0.5).toFloat() * 5f
                velocityY = -(Math.random() * 8 + 4).toFloat()
            }
            ParticleType.STAR -> {
                velocityX = (Math.random() - 0.5).toFloat() * 3f
                velocityY = -(Math.random() * 6 + 3).toFloat()
            }
            ParticleType.HEART -> {
                velocityX = (Math.random() - 0.5).toFloat() * 2f
                velocityY = -(Math.random() * 4 + 2).toFloat()
            }
        }
    }

    fun update(): Boolean {
        x + velocityX
        y + velocityY
        velocityY += 0.2f // 重力
        life -= 0.02f
        return life > 0
    }

    fun draw(scope: DrawScope, colorScheme: SlimeColorScheme) {
        val alpha = life.coerceIn(0f, 1f)
        when (type) {
            ParticleType.BOUNCE -> {
                scope.drawCircle(
                    color = colorScheme.bodyColorLight.copy(alpha = alpha),
                    radius = 4f * life,
                    center = Offset(x, y)
                )
            }
            ParticleType.STAR -> {
                scope.drawStar(
                    center = Offset(x, y),
                    color = Color(0xFFFFD700).copy(alpha = alpha),
                    size = 6f * life
                )
            }
            ParticleType.HEART -> {
                scope.drawCircle(
                    color = Color(0xFFFF6B6B).copy(alpha = alpha),
                    radius = 5f * life,
                    center = Offset(x, y)
                )
            }
        }
    }
}

enum class ParticleType {
    BOUNCE, STAR, HEART
}

// 绘制星星
private fun DrawScope.drawStar(center: Offset, color: Color, size: Float) {
    val path = Path().apply {
        val outerRadius = size
        val innerRadius = size * 0.4f
        val points = 5

        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = Math.toRadians((i * 180.0 / points - 90))
            val pointX = center.x + radius * cos(angle).toFloat()
            val pointY = center.y + radius * sin(angle).toFloat()

            if (i == 0) moveTo(pointX, pointY)
            else lineTo(pointX, pointY)
        }
        close()
    }
    drawPath(path, color)
}
