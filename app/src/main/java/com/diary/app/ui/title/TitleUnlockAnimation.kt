package com.diary.app.ui.title

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.TitleDefinition
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Particle data ──────────────────────────────────────────────────────

private enum class ParticleType { DOT, STAR, FRAGMENT }

private data class Particle(
    val offsetX: Float,   // normalised direction X (-1..1)
    val offsetY: Float,   // normalised direction Y (-1..1)
    val size: Float,
    val color: Color,
    val speed: Float,     // 0.4-2.5
    val rotationSpeed: Float,
    val initialRotation: Float,
    val type: ParticleType
)

// ── Main composable ────────────────────────────────────────────────────

@Composable
fun TitleUnlockAnimation(
    title: TitleDefinition,
    onDismiss: () -> Unit
) {
    val mainScale = remember { Animatable(0f) }
    val mainAlpha = remember { Animatable(0f) }

    // ── Tier colours ────────────────────────────────────────────────

    val tierColor = when (title.tier) {
        3 -> Color(0xFFFFD700)
        2 -> Color(0xFF7C4DFF)
        else -> Color(0xFF448AFF)
    }

    val tierGlow = when (title.tier) {
        3 -> listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFF6F00))
        2 -> listOf(Color(0xFF7C4DFF), Color(0xFF651FFF), Color(0xFF6200EA))
        else -> listOf(Color(0xFF448AFF), Color(0xFF2979FF), Color(0xFF2962FF))
    }

    // ── Phased animation state ──────────────────────────────────────

    var flashAlphaTarget by remember { mutableStateOf(0.8f) }
    var iconScaleTarget by remember { mutableStateOf(0f) }
    var visibleCharCount by remember { mutableIntStateOf(0) }
    var flavorTextVisible by remember { mutableStateOf(false) }
    var particlesStarted by remember { mutableStateOf(false) }

    // ── Smooth animated values driven by targets ────────────────────

    val flashAlphaAnim by animateFloatAsState(
        targetValue = flashAlphaTarget,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "flashAlpha"
    )

    val iconScaleAnim by animateFloatAsState(
        targetValue = iconScaleTarget,
        animationSpec = tween(
            durationMillis = if (title.tier == 3) 450 else 350,
            easing = FastOutSlowInEasing
        ),
        label = "iconScale"
    )

    // ── Infinite transitions for continuous effects ──────────────────

    val infiniteTransition = rememberInfiniteTransition(label = "unlockLoop")

    // Pulsing ring: two rings with offset phases
    val pulseRing1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val pulseRing2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 750),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    // Particle cycle: 0 -> 1 repeats every 3 s
    val particleCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleCycle"
    )

    // ── Particles (generated once per tier) ─────────────────────────

    val particles = remember(title.tier) {
        generateParticles(
            count = when (title.tier) { 3 -> 56; 2 -> 36; else -> 18 },
            tier = title.tier
        )
    }

    // ── Phase sequencer ─────────────────────────────────────────────

    val unlockText = "称号解锁!"

    LaunchedEffect(Unit) {
        // Phase 1 (0-200 ms): white flash overlay fades out
        flashAlphaTarget = 0.8f
        delay(50)
        flashAlphaTarget = 0f

        // Phase 2 (100-500 ms): icon pops from 0 -> 1.15 -> 1.0
        delay(50)
        iconScaleTarget = 1.15f
        delay(200)
        iconScaleTarget = 1.0f

        // Extra legendary bounce
        if (title.tier == 3) {
            delay(80)
            iconScaleTarget = 1.08f
            delay(100)
            iconScaleTarget = 1.0f
        }

        // Phase 3 (300-800 ms): title text typewriter
        delay(120)
        for (i in 1..unlockText.length) {
            visibleCharCount = i
            delay(60)
        }

        // Phase 4 (400-1000 ms): flavor text fade in
        delay(60)
        flavorTextVisible = true

        // Phase 5 (continuous): particles start
        delay(30)
        particlesStarted = true

        // Main entrance
        mainAlpha.animateTo(1f, tween(300))
        mainScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )

        // Auto-dismiss after 3 s
        delay(3000)
        mainAlpha.animateTo(0f, tween(500))
        onDismiss()
    }

    // ── UI ──────────────────────────────────────────────────────────

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(tween(500)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(300)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f * mainAlpha.value))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // ── Particle layer ──────────────────────────────────────
            if (particlesStarted) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawAllParticles(
                        particles = particles,
                        cycle = particleCycle,
                        cx = size.width / 2f,
                        cy = size.height / 2f
                    )
                }
            }

            // ── White flash overlay ─────────────────────────────────
            if (flashAlphaAnim > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlphaAnim))
                )
            }

            // ── Content ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .scale(mainScale.value)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Icon + glow + pulsing ring ──────────────────────
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing ring 1
                    RingHalo(
                        tierColor = tierColor,
                        progress = pulseRing1,
                        baseSize = 120.dp
                    )
                    // Pulsing ring 2 (offset phase)
                    RingHalo(
                        tierColor = tierColor,
                        progress = pulseRing2,
                        baseSize = 120.dp
                    )

                    // Glow background
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = tierGlow.map { it.copy(alpha = 0.3f) },
                                    radius = 120f
                                )
                            )
                    )

                    // Icon (phase-animated scale)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(iconScaleAnim)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(tierGlow)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── "称号解锁!" with typewriter reveal ──────────────
                if (visibleCharCount > 0) {
                    Text(
                        text = unlockText.substring(0, visibleCharCount),
                        fontSize = 14.sp,
                        color = tierColor,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Title name ──────────────────────────────────────
                Text(
                    text = title.name,
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Tier badge ──────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(title.tier) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(16.dp)
                        )
                        if (it < title.tier - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (title.tier) {
                            3 -> "传说级"
                            2 -> "稀有级"
                            else -> "普通级"
                        },
                        fontSize = 12.sp,
                        color = tierColor.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Flavor text (fade-in) ───────────────────────────
                if (title.flavorText.isNotBlank()) {
                    FlavorText(text = title.flavorText, visible = flavorTextVisible)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Dismiss hint ────────────────────────────────────
                Text(
                    text = "点击任意位置关闭",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ── Pulsing ring halo ──────────────────────────────────────────────────

@Composable
private fun RingHalo(tierColor: Color, progress: Float, baseSize: androidx.compose.ui.unit.Dp) {
    val ringScale = 0.8f + progress * 0.7f       // 0.8 -> 1.5
    val ringAlpha = (1f - progress) * 0.5f        // 0.5 -> 0
    Box(
        modifier = Modifier
            .size(baseSize)
            .scale(ringScale)
            .clip(CircleShape)
            .background(tierColor.copy(alpha = ringAlpha * 0.3f))
    )
}

// ── Flavor text with smooth fade-in ────────────────────────────────────

@Composable
private fun FlavorText(text: String, visible: Boolean) {
    val targetAlpha by animateFloatAsState(
        targetValue = if (visible) 0.8f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "flavorAlpha"
    )
    if (targetAlpha > 0.01f) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = targetAlpha),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ── Particle generation ────────────────────────────────────────────────

private fun generateParticles(count: Int, tier: Int): List<Particle> {
    val rng = Random(System.nanoTime())
    return List(count) {
        // Random direction on unit circle
        val angle = rng.nextDouble() * 2.0 * Math.PI
        val dirX = cos(angle).toFloat()
        val dirY = sin(angle).toFloat()

        val speed = 0.4f + rng.nextFloat() * 2.1f
        val size = 2f + rng.nextFloat() * 6f
        val rotSpeed = -180f + rng.nextFloat() * 360f
        val initRot = rng.nextFloat() * 360f

        val type = when (tier) {
            3 -> {
                val r = rng.nextFloat()
                when {
                    r < 0.35f -> ParticleType.STAR
                    r < 0.65f -> ParticleType.FRAGMENT
                    else -> ParticleType.DOT
                }
            }
            2 -> if (rng.nextFloat() < 0.45f) ParticleType.STAR else ParticleType.DOT
            else -> ParticleType.DOT
        }

        val color = pickParticleColor(tier, rng)

        Particle(
            offsetX = dirX,
            offsetY = dirY,
            size = size,
            color = color,
            speed = speed,
            rotationSpeed = rotSpeed,
            initialRotation = initRot,
            type = type
        )
    }
}

private fun pickParticleColor(tier: Int, rng: Random): Color = when (tier) {
    3 -> {
        val colors = listOf(
            Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF6B6B),
            Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0)
        )
        colors[rng.nextInt(colors.size)]
    }
    2 -> {
        val colors = listOf(Color(0xFF7C4DFF), Color(0xFFB388FF), Color(0xFFE040FB))
        colors[rng.nextInt(colors.size)]
    }
    else -> {
        val colors = listOf(Color(0xFF448AFF), Color(0xFF82B1FF), Color(0xFF40C4FF))
        colors[rng.nextInt(colors.size)]
    }
}

// ── Canvas drawing helpers ─────────────────────────────────────────────

private fun DrawScope.drawAllParticles(
    particles: List<Particle>,
    cycle: Float,
    cx: Float,
    cy: Float
) {
    val maxDist = size.width * 0.38f

    for (p in particles) {
        // Distance from center grows with cycle
        val dist = cycle * maxDist * p.speed
        val px = cx + p.offsetX * dist
        val py = cy + p.offsetY * dist

        // Fade out towards end of cycle
        val alpha = when {
            cycle < 0.15f -> cycle / 0.15f          // fade in
            cycle > 0.65f -> (1f - cycle) / 0.35f   // fade out
            else -> 1f
        } * 0.85f

        // Scale: appear -> full -> shrink
        val scaleFactor = when {
            cycle < 0.1f -> cycle / 0.1f
            cycle > 0.75f -> (1f - cycle) / 0.25f
            else -> 1f
        }
        val s = p.size * scaleFactor.coerceIn(0f, 1f)
        if (s < 0.3f) continue

        val rotation = p.initialRotation + cycle * 360f * (p.rotationSpeed / 360f)

        when (p.type) {
            ParticleType.DOT -> {
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = s,
                    center = Offset(px, py)
                )
            }
            ParticleType.STAR -> {
                drawStarShape(Offset(px, py), s, p.color.copy(alpha = alpha), rotation)
            }
            ParticleType.FRAGMENT -> {
                drawShardShape(Offset(px, py), s, p.color.copy(alpha = alpha), rotation)
            }
        }
    }
}

private fun DrawScope.drawStarShape(center: Offset, r: Float, color: Color, rotation: Float) {
    val outerR = r
    val innerR = r * 0.4f
    val path = Path()
    rotate(rotation, pivot = center) {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outerR else innerR
            val angle = Math.toRadians(i * 36.0 - 90.0)
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

private fun DrawScope.drawShardShape(center: Offset, r: Float, color: Color, rotation: Float) {
    val path = Path()
    rotate(rotation, pivot = center) {
        path.moveTo(center.x, center.y - r)
        path.lineTo(center.x - r * 0.7f, center.y + r * 0.6f)
        path.lineTo(center.x + r * 0.7f, center.y + r * 0.6f)
        path.close()
        drawPath(path, color)
    }
}
