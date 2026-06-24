package com.diary.app.ui.island

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import com.diary.app.data.AnimalBehavior
import com.diary.app.data.AnimalType
import com.diary.app.data.IslandAnimal
import com.diary.app.data.IslandDecoration
import com.diary.app.data.IslandEnvironment
import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// Color Palette
// ============================================================

private object IslandColors {
    val Sky = Color(0xFF7EC8E3)
    val SkyBottom = Color(0xFFB8E2F2)
    val Ocean = Color(0xFF45B7AA)
    val OceanDeep = Color(0xFF3A9E92)
    val Sand = Color(0xFFF5E6CA)
    val SandDark = Color(0xFFE8D5B0)
    val Grass = Color(0xFF8BC48A)
    val GrassDark = Color(0xFF6FA86E)
    val Wood = Color(0xFFC08B5C)
    val WoodDark = Color(0xFF9E7248)
    val Coral = Color(0xFFE87461)
    val CoralLight = Color(0xFFF09080)
    val CloudWhite = Color(0xFFFFFFFF)
    val WaveLight = Color(0xFF7DD4C8)
    val RoofRed = Color(0xFFE07060)
    val WindowYellow = Color(0xFFF5D76E)
    val TrunkBrown = Color(0xFF8B6540)
    val LeafDark = Color(0xFF6BAA6A)
    val LeafLight = Color(0xFFA0D49F)
    val StoneGray = Color(0xFFB0B8B4)
    val StoneDark = Color(0xFF8E9692)
}

// ============================================================
// Hit-test data structures
// ============================================================

/** A rectangular hit region mapped to a named island element. */
data class HitRegion(
    val id: String,
    val label: String,
    val rect: Rect,
    val kind: HitKind
)

enum class HitKind { BUILDING, ANIMAL, RESOURCE }

/** Info shown in the long-press overlay. */
data class IslandInfoCard(
    val title: String,
    val description: String,
    val kind: HitKind
)

// ============================================================
// Nurturing data models
// ============================================================

/** Collectible resource type on the island. */
enum class ResourceType(val displayName: String, val icon: String) {
    SUNLIGHT("日光", "*"),
    RAINWATER("雨水", "~"),
    STARLIGHT("星光", ".")
}

/** A single resource orb floating on the island. */
data class ResourceOrb(
    val type: ResourceType,
    val x: Float,       // 0..1 relative
    val y: Float,       // 0..1 relative
    val radius: Float = 0.02f
)

/** Building upgrade tier. */
data class BuildingLevel(
    val name: String,
    val tier: Int,
    val requiredResources: Map<ResourceType, Int> = emptyMap()
)

/** A visitor that can appear on the island. */
data class Visitor(
    val name: String,
    val message: String,
    val x: Float,
    val y: Float,
    val stayDuration: Int = 60  // seconds
)

// ============================================================
// Main composable
// ============================================================

@Composable
fun MinimalIslandCanvas(
    environment: IslandEnvironment,
    decorations: List<IslandDecoration>,
    activeAnimals: List<IslandAnimal> = emptyList(),
    modifier: Modifier = Modifier,
    onTapElement: ((HitRegion) -> Unit)? = null,
    onLongPressElement: ((HitRegion) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "minimal_island")

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing)
        ),
        label = "time"
    )

    val activeDecorations = remember(decorations) {
        decorations.filter { it.isUnlocked }
    }

    // Collect hit regions each frame for tap detection
    var hitRegions by remember { mutableStateOf(emptyList<HitRegion>()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeDecorations, activeAnimals) {
                detectTapGestures(
                    onTap = { offset ->
                        val hit = hitRegions.firstOrNull { region ->
                            region.rect.contains(offset)
                        }
                        if (hit != null) {
                            onTapElement?.invoke(hit)
                        }
                    },
                    onLongPress = { offset ->
                        val hit = hitRegions.firstOrNull { region ->
                            region.rect.contains(offset)
                        }
                        if (hit != null) {
                            onLongPressElement?.invoke(hit)
                        }
                    }
                )
            }
    ) {
        // Reset hit regions for this frame
        val newHits = mutableListOf<HitRegion>()

        // Layer 0: Sky
        drawSkyFlat(time)

        // Layer 1: Clouds
        drawCloudsFlat(time)

        // Layer 2: Ocean
        drawOceanFlat(time, environment.tranquility)

        // Layer 3: Island terrain
        drawTerrainFlat(environment.lushness)

        // Layer 4: Vegetation
        drawVegetationFlat(environment.lushness, environment.brightness)

        // Layer 5: Buildings with hit regions
        drawBuildingsFlat(activeDecorations, newHits, time)

        // Layer 6: Animals with hit regions
        drawAnimalsFlat(activeAnimals, newHits, time)

        hitRegions = newHits
    }
}

// ============================================================
// Layer 0: Sky -- soft gradient, flat style
// ============================================================

private fun DrawScope.drawSkyFlat(time: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(IslandColors.Sky, IslandColors.SkyBottom),
            startY = 0f,
            endY = size.height * 0.55f
        ),
        size = Size(size.width, size.height * 0.55f)
    )
}

// ============================================================
// Layer 1: Clouds -- overlapping circles
// ============================================================

private fun DrawScope.drawCloudsFlat(time: Float) {
    data class CloudDef(val baseX: Float, val baseY: Float, val scale: Float, val speed: Float)

    val clouds = listOf(
        CloudDef(0.12f, 0.07f, 1.0f, 0.25f),
        CloudDef(0.42f, 0.11f, 0.65f, 0.18f),
        CloudDef(0.72f, 0.05f, 0.8f, 0.22f),
        CloudDef(0.28f, 0.16f, 0.45f, 0.14f)
    )

    for (cloud in clouds) {
        val drift = sin(time * cloud.speed * 0.08f) * size.width * 0.02f
        val cx = cloud.baseX * size.width + drift
        val cy = cloud.baseY * size.height
        val s = cloud.scale
        val alpha = 0.45f + sin(time * 0.15f + cloud.baseX * 8f) * 0.1f

        val color = IslandColors.CloudWhite.copy(alpha = alpha)

        // 3 overlapping circles
        drawCircle(color = color, radius = 18f * s, center = Offset(cx - 16f * s, cy + 4f * s))
        drawCircle(color = color, radius = 22f * s, center = Offset(cx, cy))
        drawCircle(color = color, radius = 16f * s, center = Offset(cx + 18f * s, cy + 3f * s))
        // Top bump
        drawCircle(color = color, radius = 14f * s, center = Offset(cx + 2f * s, cy - 10f * s))
    }
}

// ============================================================
// Layer 2: Ocean -- gradient fill + single sine wave
// ============================================================

private fun DrawScope.drawOceanFlat(time: Float, tranquility: Float) {
    val oceanY = size.height * 0.55f
    val oceanHeight = size.height * 0.45f

    // Ocean fill
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(IslandColors.WaveLight, IslandColors.Ocean, IslandColors.OceanDeep),
            startY = oceanY,
            endY = size.height
        ),
        topLeft = Offset(0f, oceanY),
        size = Size(size.width, oceanHeight)
    )

    // Single sine wave near the coast
    val waveAmplitude = 5f + (1f - tranquility) * 6f
    val waveColor = IslandColors.WaveLight.copy(alpha = 0.55f)

    val wavePath = Path().apply {
        moveTo(0f, oceanY + 18f)
        for (x in 0..size.width.toInt() step 6) {
            val xF = x.toFloat()
            val y = oceanY + 18f + sin(xF * 0.012f + time * 0.4f) * waveAmplitude
            lineTo(xF, y)
        }
        lineTo(size.width, oceanY + 32f)
        lineTo(0f, oceanY + 32f)
        close()
    }
    drawPath(path = wavePath, color = waveColor, style = Fill)

    // Foam dots along the wave
    val foamColor = IslandColors.CloudWhite.copy(alpha = 0.4f)
    for (i in 0..15) {
        val dotX = size.width * (i / 15f) + sin(time * 0.3f + i * 0.9f) * 4f
        val dotY = oceanY + 18f + sin(dotX * 0.012f + time * 0.4f) * waveAmplitude
        val r = 1.5f + (i % 3) * 0.5f
        drawCircle(color = foamColor, radius = r, center = Offset(dotX, dotY))
    }
}

// ============================================================
// Layer 3: Terrain -- overlapping rounded rects
// ============================================================

private fun DrawScope.drawTerrainFlat(lushness: Float) {
    val baseY = size.height * 0.46f

    // Sand base -- large rounded rect
    drawRoundRect(
        color = IslandColors.Sand,
        topLeft = Offset(size.width * 0.08f, baseY + 10f),
        size = Size(size.width * 0.84f, size.height * 0.22f),
        cornerRadius = CornerRadius(40f, 20f)
    )

    // Sand darker edge
    drawRoundRect(
        color = IslandColors.SandDark,
        topLeft = Offset(size.width * 0.12f, baseY + 28f),
        size = Size(size.width * 0.76f, size.height * 0.14f),
        cornerRadius = CornerRadius(30f, 16f)
    )

    // Grass layer on top -- overlapping rounded rect
    val grassAlpha = 0.5f + lushness * 0.5f
    drawRoundRect(
        color = IslandColors.Grass.copy(alpha = grassAlpha),
        topLeft = Offset(size.width * 0.14f, baseY),
        size = Size(size.width * 0.72f, size.height * 0.16f),
        cornerRadius = CornerRadius(35f, 18f)
    )

    // Second grass bump (overlapping, offset right)
    drawRoundRect(
        color = IslandColors.GrassDark.copy(alpha = grassAlpha * 0.85f),
        topLeft = Offset(size.width * 0.3f, baseY - 6f),
        size = Size(size.width * 0.45f, size.height * 0.14f),
        cornerRadius = CornerRadius(28f, 14f)
    )
}

// ============================================================
// Layer 4: Vegetation -- circle-on-rect trees
// ============================================================

private fun DrawScope.drawVegetationFlat(lushness: Float, brightness: Float) {
    val treeCount = (lushness * 4).toInt() + 1
    val baseY = size.height * 0.42f

    for (i in 0 until treeCount) {
        val x = size.width * (0.22f + i * 0.14f)
        val y = baseY + (i % 2) * 8f

        if (i % 3 == 0) {
            // Coniferous: stacked triangles
            drawConiferousTree(x, y, lushness)
        } else {
            // Deciduous: circle on rectangle
            drawDeciduousTree(x, y, lushness, brightness)
        }
    }

    // Flowers if lushness is high enough
    if (lushness > 0.5f) {
        val flowerColors = listOf(
            IslandColors.Coral,
            IslandColors.CoralLight,
            IslandColors.WindowYellow
        )
        val count = ((lushness - 0.5f) * 10).toInt()
        for (i in 0 until count) {
            val fx = size.width * (0.2f + i * 0.08f)
            val fy = baseY + 22f + (i % 3) * 10f
            val fc = flowerColors[i % flowerColors.size]
            drawCircle(color = fc, radius = 3f, center = Offset(fx, fy))
            drawCircle(
                color = IslandColors.CloudWhite.copy(alpha = 0.4f),
                radius = 1.5f,
                center = Offset(fx - 0.5f, fy - 0.5f)
            )
        }
    }
}

/** Deciduous tree: circle crown on rectangular trunk. */
private fun DrawScope.drawDeciduousTree(x: Float, y: Float, lushness: Float, brightness: Float) {
    val trunkH = 18f + lushness * 8f
    val crownR = 14f + lushness * 6f

    // Trunk
    drawRoundRect(
        color = IslandColors.TrunkBrown,
        topLeft = Offset(x - 3f, y),
        size = Size(6f, trunkH)
    )

    // Crown -- main circle
    val crownColor = IslandColors.Grass.copy(alpha = 0.7f + brightness * 0.3f)
    drawCircle(color = IslandColors.LeafDark.copy(alpha = 0.5f), radius = crownR + 3f, center = Offset(x, y - crownR * 0.3f))
    drawCircle(color = crownColor, radius = crownR, center = Offset(x, y - crownR * 0.3f))
    drawCircle(color = IslandColors.LeafLight.copy(alpha = 0.45f), radius = crownR * 0.55f, center = Offset(x - crownR * 0.2f, y - crownR * 0.55f))
}

/** Coniferous tree: stacked triangles. */
private fun DrawScope.drawConiferousTree(x: Float, y: Float, lushness: Float) {
    val layers = 3
    val layerH = 14f + lushness * 4f
    val baseW = 20f + lushness * 6f

    // Trunk
    drawRoundRect(
        color = IslandColors.TrunkBrown,
        topLeft = Offset(x - 2.5f, y + layerH),
        size = Size(5f, 12f)
    )

    // Triangles from bottom to top
    for (i in 0 until layers) {
        val w = baseW - i * 5f
        val ty = y + (layers - 1 - i) * (layerH * 0.6f)
        val color = if (i == layers - 1) IslandColors.LeafLight.copy(alpha = 0.8f) else IslandColors.LeafDark.copy(alpha = 0.7f)

        val tri = Path().apply {
            moveTo(x, ty - layerH * 0.6f)
            lineTo(x - w / 2, ty + layerH * 0.3f)
            lineTo(x + w / 2, ty + layerH * 0.3f)
            close()
        }
        drawPath(path = tri, color = color, style = Fill)
    }
}

// ============================================================
// Layer 5: Buildings -- triangle roof + rectangle walls
// ============================================================

private fun DrawScope.drawBuildingsFlat(
    decorations: List<IslandDecoration>,
    hitRegions: MutableList<HitRegion>,
    time: Float
) {
    decorations.filter { it.type == "building" }.forEach { decoration ->
        val x = size.width * decoration.posX
        val y = size.height * decoration.posY

        when (decoration.name) {
            "小木屋" -> drawMinimalCabin(x, y, time)
            "灯塔" -> drawMinimalLighthouse(x, y, time)
            "风车" -> drawMinimalWindmill(x, y, time)
            "桥梁" -> drawMinimalBridge(x, y)
            "喷泉" -> drawMinimalFountain(x, y, time)
            else -> drawMinimalBuilding(x, y)
        }

        hitRegions.add(
            HitRegion(
                id = decoration.id,
                label = decoration.name,
                rect = Rect(x - 30f, y - 55f, x + 30f, y + 15f),
                kind = HitKind.BUILDING
            )
        )
    }
}

private fun DrawScope.drawMinimalCabin(x: Float, y: Float, time: Float) {
    // Walls
    drawRoundRect(
        color = IslandColors.Sand,
        topLeft = Offset(x - 20f, y - 22f),
        size = Size(40f, 32f),
        cornerRadius = CornerRadius(3f)
    )

    // Roof triangle
    val roof = Path().apply {
        moveTo(x - 26f, y - 22f)
        lineTo(x, y - 48f)
        lineTo(x + 26f, y - 22f)
        close()
    }
    drawPath(path = roof, color = IslandColors.Coral, style = Fill)

    // Door
    drawRoundRect(
        color = IslandColors.Wood,
        topLeft = Offset(x - 5f, y - 6f),
        size = Size(10f, 16f),
        cornerRadius = CornerRadius(5f, 5f)
    )

    // Window
    drawRoundRect(
        color = IslandColors.WindowYellow,
        topLeft = Offset(x + 9f, y - 16f),
        size = Size(8f, 8f),
        cornerRadius = CornerRadius(2f)
    )

    // Chimney
    drawRoundRect(
        color = IslandColors.WoodDark,
        topLeft = Offset(x + 10f, y - 44f),
        size = Size(6f, 12f)
    )

    // Smoke puffs (animated)
    val smokeColor = IslandColors.CloudWhite.copy(alpha = 0.35f)
    for (i in 0..2) {
        val phase = (time * 0.6f + i * 2f) % 4f
        if (phase < 2.5f) {
            val sy = y - 44f - phase * 10f
            val sx = x + 13f + sin(phase * 1.2f + i) * 3f
            val alpha = (1f - phase / 2.5f) * 0.3f
            drawCircle(color = smokeColor.copy(alpha = alpha), radius = 3f + phase, center = Offset(sx, sy))
        }
    }
}

private fun DrawScope.drawMinimalLighthouse(x: Float, y: Float, time: Float) {
    // Body
    drawRoundRect(
        color = IslandColors.StoneGray,
        topLeft = Offset(x - 7f, y - 52f),
        size = Size(14f, 62f),
        cornerRadius = CornerRadius(3f)
    )

    // Red stripes
    drawRoundRect(
        color = IslandColors.Coral,
        topLeft = Offset(x - 7f, y - 38f),
        size = Size(14f, 8f)
    )
    drawRoundRect(
        color = IslandColors.Coral,
        topLeft = Offset(x - 7f, y - 18f),
        size = Size(14f, 8f)
    )

    // Lamp
    drawCircle(color = IslandColors.WindowYellow, radius = 8f, center = Offset(x, y - 56f))

    // Light glow (pulsing)
    val glowAlpha = 0.15f + sin(time * 0.5f) * 0.08f
    drawCircle(color = IslandColors.WindowYellow.copy(alpha = glowAlpha), radius = 22f, center = Offset(x, y - 56f))
}

private fun DrawScope.drawMinimalWindmill(x: Float, y: Float, time: Float) {
    // Body
    val body = Path().apply {
        moveTo(x - 10f, y + 8f)
        lineTo(x + 10f, y + 8f)
        lineTo(x + 6f, y - 32f)
        lineTo(x - 6f, y - 32f)
        close()
    }
    drawPath(path = body, color = IslandColors.StoneGray, style = Fill)

    // Hub
    drawCircle(color = IslandColors.WoodDark, radius = 5f, center = Offset(x, y - 32f))

    // 4 blades
    val bladeLen = 26f
    for (i in 0..3) {
        val angle = time * 20f + i * 90f
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val tx = x + sin(rad) * bladeLen
        val ty = y - 32f - cos(rad) * bladeLen

        val blade = Path().apply {
            moveTo(x, y - 32f)
            lineTo(tx, ty)
        }
        drawPath(path = blade, color = IslandColors.Sand, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f))
    }
}

private fun DrawScope.drawMinimalBridge(x: Float, y: Float) {
    // Deck
    drawRoundRect(
        color = IslandColors.Wood,
        topLeft = Offset(x - 32f, y - 2f),
        size = Size(64f, 5f),
        cornerRadius = CornerRadius(2f)
    )

    // Posts
    for (i in -24..24 step 16) {
        drawRoundRect(
            color = IslandColors.WoodDark,
            topLeft = Offset(x + i - 1.5f, y - 2f),
            size = Size(3f, 10f)
        )
    }
}

private fun DrawScope.drawMinimalFountain(x: Float, y: Float, time: Float) {
    // Basin
    drawOval(
        color = IslandColors.WaveLight.copy(alpha = 0.6f),
        topLeft = Offset(x - 18f, y - 3f),
        size = Size(36f, 10f)
    )

    // Pillar
    drawRoundRect(
        color = IslandColors.StoneGray,
        topLeft = Offset(x - 2.5f, y - 28f),
        size = Size(5f, 25f)
    )

    // Water drops (animated)
    val dropColor = IslandColors.WaveLight.copy(alpha = 0.7f)
    for (i in 0..4) {
        val angle = (360f / 5) * i + time * 30f
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val dx = x + cos(rad) * 8f
        val dy = y - 30f - sin(rad) * 4f
        drawCircle(color = dropColor, radius = 2f, center = Offset(dx, dy))
    }
}

private fun DrawScope.drawMinimalBuilding(x: Float, y: Float) {
    drawRoundRect(
        color = IslandColors.StoneGray,
        topLeft = Offset(x - 14f, y - 18f),
        size = Size(28f, 28f),
        cornerRadius = CornerRadius(3f)
    )
    val roof = Path().apply {
        moveTo(x - 18f, y - 18f)
        lineTo(x, y - 34f)
        lineTo(x + 18f, y - 18f)
        close()
    }
    drawPath(path = roof, color = IslandColors.Coral, style = Fill)
}

// ============================================================
// Layer 6: Animals -- 2-3 circles composed together
// ============================================================

private fun DrawScope.drawAnimalsFlat(
    animals: List<IslandAnimal>,
    hitRegions: MutableList<HitRegion>,
    time: Float
) {
    animals.forEach { animal ->
        val px = size.width * animal.x
        val py = size.height * animal.y

        when (animal.type) {
            AnimalType.BIRD -> drawFlatBird(px, py, animal.behavior, time, animal.scale, animal.alpha)
            AnimalType.BUTTERFLY -> drawFlatButterfly(px, py, animal.behavior, time, animal.alpha)
            AnimalType.SQUIRREL -> drawFlatSquirrel(px, py, animal.behavior, time, animal.alpha)
            AnimalType.OWL -> drawFlatOwl(px, py, animal.behavior, time, animal.alpha)
            AnimalType.CAT -> drawFlatCat(px, py, animal.behavior, time, animal.alpha)
            AnimalType.FROG -> drawFlatFrog(px, py, animal.behavior, time, animal.alpha)
            AnimalType.FIREFLY -> drawFlatFirefly(px, py, time, animal.scale)
        }

        val label = when (animal.type) {
            AnimalType.BIRD -> "小鸟"
            AnimalType.BUTTERFLY -> "蝴蝶"
            AnimalType.SQUIRREL -> "松鼠"
            AnimalType.OWL -> "猫头鹰"
            AnimalType.CAT -> "猫咪"
            AnimalType.FROG -> "青蛙"
            AnimalType.FIREFLY -> "萤火虫"
        }

        hitRegions.add(
            HitRegion(
                id = "animal_${animal.type.name}",
                label = label,
                rect = Rect(px - 20f, py - 25f, px + 20f, py + 10f),
                kind = HitKind.ANIMAL
            )
        )
    }
}

// --- Flat bird: 2 circles (body + head) ---

private fun DrawScope.drawFlatBird(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, scale: Float, alpha: Float
) {
    val s = scale
    val color = IslandColors.Wood.copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.FLYING -> {
            val ax = x + sin(time * 0.3f) * size.width * 0.04f * s
            val ay = y + sin(time * 0.5f) * 5f * s

            // Body
            drawOval(color = color, topLeft = Offset(ax - 5f * s, ay - 3f * s), size = Size(10f * s, 6f * s))
            // Head
            drawCircle(color = color, radius = 4f * s, center = Offset(ax + 5f * s, ay - 2f * s))
            // Beak
            drawCircle(color = IslandColors.Coral.copy(alpha = alpha), radius = 1.5f * s, center = Offset(ax + 9f * s, ay - 2f * s))
            // Wing
            val wingY = sin(time * 2.5f) * 4f * s
            drawOval(
                color = IslandColors.WoodDark.copy(alpha = alpha * 0.7f),
                topLeft = Offset(ax - 3f * s, ay - 4f * s + wingY),
                size = Size(8f * s, 3f * s)
            )
        }
        AnimalBehavior.SLEEPING -> {
            drawOval(color = color, topLeft = Offset(x - 4f * s, y - 2f * s), size = Size(8f * s, 5f * s))
            drawCircle(color = color, radius = 3f * s, center = Offset(x + 3f * s, y - 2f * s))
        }
        else -> {
            drawOval(color = color, topLeft = Offset(x - 5f * s, y - 3f * s), size = Size(10f * s, 6f * s))
            drawCircle(color = color, radius = 4f * s, center = Offset(x + 5f * s, y - 2f * s))
            drawCircle(color = IslandColors.Coral.copy(alpha = alpha), radius = 1.5f * s, center = Offset(x + 9f * s, y - 2f * s))
        }
    }
}

// --- Flat butterfly: 2 wing circles + body line ---

private fun DrawScope.drawFlatButterfly(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    when (behavior) {
        AnimalBehavior.FLYING -> {
            val ax = x + sin(time * 0.4f) * size.width * 0.02f
            val ay = y + sin(time * 0.6f) * 4f
            val wingOpen = 0.5f + sin(time * 3f) * 0.3f

            val wingColor = Color(0xFFCE93D8).copy(alpha = 0.75f * alpha)
            drawCircle(color = wingColor, radius = 6f * wingOpen, center = Offset(ax - 5f, ay))
            drawCircle(color = wingColor, radius = 6f * wingOpen, center = Offset(ax + 5f, ay))
            drawLine(color = IslandColors.TrunkBrown.copy(alpha = alpha), start = Offset(ax, ay - 4f), end = Offset(ax, ay + 4f), strokeWidth = 1.5f)
        }
        else -> {
            val wingColor = Color(0xFFCE93D8).copy(alpha = 0.5f * alpha)
            drawCircle(color = wingColor, radius = 4f, center = Offset(x - 4f, y))
            drawCircle(color = wingColor, radius = 4f, center = Offset(x + 4f, y))
            drawLine(color = IslandColors.TrunkBrown.copy(alpha = alpha), start = Offset(x, y - 3f), end = Offset(x, y + 3f), strokeWidth = 1f)
        }
    }
}

// --- Flat squirrel: body oval + tail circle ---

private fun DrawScope.drawFlatSquirrel(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    val bodyColor = Color(0xFFA1887F).copy(alpha = alpha)
    val tailColor = IslandColors.Sand.copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.CLIMBING -> {
            val cy = y + sin(time * 0.3f) * 6f
            drawOval(color = bodyColor, topLeft = Offset(x - 6f, cy - 5f), size = Size(12f, 10f))
            drawCircle(color = bodyColor, radius = 4f, center = Offset(x + 4f, cy - 7f))
            // Tail
            drawCircle(color = tailColor, radius = 5f, center = Offset(x - 8f, cy - 4f))
        }
        AnimalBehavior.SLEEPING -> {
            drawOval(color = bodyColor.copy(alpha = alpha * 0.6f), topLeft = Offset(x - 5f, y - 4f), size = Size(10f, 8f))
            drawCircle(color = tailColor.copy(alpha = alpha * 0.5f), radius = 4f, center = Offset(x - 7f, y - 3f))
        }
        else -> {
            drawOval(color = bodyColor, topLeft = Offset(x - 6f, y - 5f), size = Size(12f, 10f))
            drawCircle(color = bodyColor, radius = 4f, center = Offset(x + 4f, y - 7f))
            drawCircle(color = tailColor, radius = 5f, center = Offset(x - 8f, y - 3f))
        }
    }
}

// --- Flat owl: body oval + 2 eye circles + ear triangles ---

private fun DrawScope.drawFlatOwl(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    val bodyColor = IslandColors.WoodDark.copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.SLEEPING -> {
            drawOval(color = bodyColor, topLeft = Offset(x - 7f, y - 4f), size = Size(14f, 14f))
            drawCircle(color = bodyColor, radius = 7f, center = Offset(x, y - 8f))
            // Closed eyes
            drawLine(color = IslandColors.TrunkBrown.copy(alpha = alpha * 0.6f), start = Offset(x - 5f, y - 9f), end = Offset(x - 1f, y - 9f), strokeWidth = 1.5f)
            drawLine(color = IslandColors.TrunkBrown.copy(alpha = alpha * 0.6f), start = Offset(x + 1f, y - 9f), end = Offset(x + 5f, y - 9f), strokeWidth = 1.5f)
            // Ears
            val earL = Path().apply { moveTo(x - 5f, y - 14f); lineTo(x - 7f, y - 20f); lineTo(x - 1f, y - 14f); close() }
            val earR = Path().apply { moveTo(x + 1f, y - 14f); lineTo(x + 7f, y - 20f); lineTo(x + 5f, y - 14f); close() }
            drawPath(path = earL, color = bodyColor, style = Fill)
            drawPath(path = earR, color = bodyColor, style = Fill)
        }
        else -> {
            drawOval(color = bodyColor, topLeft = Offset(x - 7f, y - 4f), size = Size(14f, 14f))
            drawCircle(color = bodyColor, radius = 7f, center = Offset(x, y - 8f))
            // Eyes
            drawCircle(color = IslandColors.WindowYellow.copy(alpha = alpha), radius = 3.5f, center = Offset(x - 3f, y - 9f))
            drawCircle(color = IslandColors.WindowYellow.copy(alpha = alpha), radius = 3.5f, center = Offset(x + 3f, y - 9f))
            drawCircle(color = IslandColors.TrunkBrown.copy(alpha = alpha), radius = 1.8f, center = Offset(x - 3f, y - 9f))
            drawCircle(color = IslandColors.TrunkBrown.copy(alpha = alpha), radius = 1.8f, center = Offset(x + 3f, y - 9f))
            // Ears
            val earL = Path().apply { moveTo(x - 5f, y - 14f); lineTo(x - 7f, y - 20f); lineTo(x - 1f, y - 14f); close() }
            val earR = Path().apply { moveTo(x + 1f, y - 14f); lineTo(x + 7f, y - 20f); lineTo(x + 5f, y - 14f); close() }
            drawPath(path = earL, color = bodyColor, style = Fill)
            drawPath(path = earR, color = bodyColor, style = Fill)
        }
    }
}

// --- Flat cat: body oval + head circle + 2 ear triangles ---

private fun DrawScope.drawFlatCat(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    val bodyColor = IslandColors.StoneGray.copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.SLEEPING -> {
            drawOval(color = bodyColor, topLeft = Offset(x - 6f, y - 4f), size = Size(12f, 8f))
            drawCircle(color = bodyColor, radius = 4f, center = Offset(x + 4f, y - 3f))
            // Closed eyes
            drawLine(color = IslandColors.TrunkBrown.copy(alpha = alpha * 0.5f), start = Offset(x + 2f, y - 4f), end = Offset(x + 4f, y - 4f), strokeWidth = 1f)
            drawLine(color = IslandColors.TrunkBrown.copy(alpha = alpha * 0.5f), start = Offset(x + 5f, y - 4f), end = Offset(x + 7f, y - 4f), strokeWidth = 1f)
        }
        AnimalBehavior.HUNTING -> {
            val hx = x + sin(time * 1.5f) * size.width * 0.03f
            drawOval(color = bodyColor, topLeft = Offset(hx - 7f, y - 3f), size = Size(14f, 7f))
            drawCircle(color = bodyColor, radius = 4.5f, center = Offset(hx + 7f, y - 2f))
            // Eyes (alert)
            drawCircle(color = IslandColors.Grass.copy(alpha = alpha), radius = 2f, center = Offset(hx + 8f, y - 3f))
            drawCircle(color = IslandColors.TrunkBrown.copy(alpha = alpha), radius = 1f, center = Offset(hx + 8f, y - 3f))
            // Ears
            val earL = Path().apply { moveTo(hx + 4f, y - 5f); lineTo(hx + 3f, y - 10f); lineTo(hx + 6f, y - 5f); close() }
            val earR = Path().apply { moveTo(hx + 7f, y - 5f); lineTo(hx + 8f, y - 10f); lineTo(hx + 10f, y - 5f); close() }
            drawPath(path = earL, color = bodyColor, style = Fill)
            drawPath(path = earR, color = bodyColor, style = Fill)
        }
        else -> {
            drawOval(color = bodyColor, topLeft = Offset(x - 7f, y - 3f), size = Size(14f, 7f))
            drawCircle(color = bodyColor, radius = 4.5f, center = Offset(x + 7f, y - 2f))
            // Ears
            val earL = Path().apply { moveTo(x + 4f, y - 5f); lineTo(x + 3f, y - 10f); lineTo(x + 6f, y - 5f); close() }
            val earR = Path().apply { moveTo(x + 7f, y - 5f); lineTo(x + 8f, y - 10f); lineTo(x + 10f, y - 5f); close() }
            drawPath(path = earL, color = bodyColor, style = Fill)
            drawPath(path = earR, color = bodyColor, style = Fill)
            // Tail
            val tail = Path().apply {
                moveTo(x - 7f, y)
                quadraticBezierTo(x - 14f, y - 8f, x - 10f, y - 12f)
            }
            drawPath(path = tail, color = bodyColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
        }
    }
}

// --- Flat frog: body oval + 2 eye circles on top ---

private fun DrawScope.drawFlatFrog(
    x: Float, y: Float, behavior: AnimalBehavior, time: Float, alpha: Float
) {
    val bodyColor = IslandColors.Grass.copy(alpha = alpha)

    when (behavior) {
        AnimalBehavior.CALLING -> {
            val puff = sin(time * 2f) * 2f
            val bw = 12f + puff
            drawOval(color = bodyColor, topLeft = Offset(x - bw / 2, y - 3f), size = Size(bw, 7f))
            // Eyes
            drawCircle(color = IslandColors.WindowYellow.copy(alpha = alpha), radius = 2.5f, center = Offset(x - 3f, y - 5f))
            drawCircle(color = IslandColors.WindowYellow.copy(alpha = alpha), radius = 2.5f, center = Offset(x + 3f, y - 5f))
            drawCircle(color = IslandColors.TrunkBrown.copy(alpha = alpha), radius = 1.2f, center = Offset(x - 3f, y - 5f))
            drawCircle(color = IslandColors.TrunkBrown.copy(alpha = alpha), radius = 1.2f, center = Offset(x + 3f, y - 5f))
        }
        else -> {
            drawOval(color = bodyColor, topLeft = Offset(x - 6f, y - 3f), size = Size(12f, 7f))
            drawCircle(color = IslandColors.WindowYellow.copy(alpha = alpha), radius = 2.5f, center = Offset(x - 3f, y - 5f))
            drawCircle(color = IslandColors.WindowYellow.copy(alpha = alpha), radius = 2.5f, center = Offset(x + 3f, y - 5f))
            drawCircle(color = IslandColors.TrunkBrown.copy(alpha = alpha), radius = 1.2f, center = Offset(x - 3f, y - 5f))
            drawCircle(color = IslandColors.TrunkBrown.copy(alpha = alpha), radius = 1.2f, center = Offset(x + 3f, y - 5f))
        }
    }
}

// --- Flat firefly: single glowing circle ---

private fun DrawScope.drawFlatFirefly(x: Float, y: Float, time: Float, scale: Float) {
    val pulse = 0.4f + sin(time * 1.5f) * 0.3f
    val r = 3f * scale

    // Glow
    drawCircle(
        color = IslandColors.WindowYellow.copy(alpha = pulse * 0.25f),
        radius = r * 4f,
        center = Offset(x, y)
    )
    // Core
    drawCircle(
        color = IslandColors.WindowYellow.copy(alpha = pulse),
        radius = r,
        center = Offset(x, y)
    )
    // Bright center
    drawCircle(
        color = IslandColors.CloudWhite.copy(alpha = pulse * 0.7f),
        radius = r * 0.4f,
        center = Offset(x, y)
    )
}

// ============================================================
// Standalone preview composable (no interaction needed)
// ============================================================

@Composable
fun MinimalIslandPreview(
    environment: IslandEnvironment = IslandEnvironment(),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "preview")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing)
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSkyFlat(time)
        drawCloudsFlat(time)
        drawOceanFlat(time, environment.tranquility)
        drawTerrainFlat(environment.lushness)
        drawVegetationFlat(environment.lushness, environment.brightness)
    }
}
