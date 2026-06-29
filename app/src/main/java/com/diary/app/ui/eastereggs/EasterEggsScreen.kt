package com.diary.app.ui.eastereggs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.EasterEgg
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasterEggsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EasterEggsViewModel = viewModel()
) {
    val discoveredEggs by viewModel.discoveredEggs.collectAsState()
    val showCelebration by viewModel.showCelebration.collectAsState()
    val discoveredIds = discoveredEggs.map { it.eggId }.toSet()
    val allEggs = viewModel.allEggs

    var gridMode by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("全部") }
    var selectedEgg by remember { mutableStateOf<EasterEgg?>(null) }

    val filteredEggs = remember(allEggs, discoveredIds, filter) {
        allEggs.filter { egg ->
            when (filter) {
                "已解锁" -> egg.id in discoveredIds
                "未解锁" -> egg.id !in discoveredIds
                else -> true
            }
        }
    }

    if (showCelebration != null) {
        CelebrationDialog(egg = showCelebration!!, onDismiss = { viewModel.dismissCelebration() })
    }

    if (selectedEgg != null) {
        EggDetailSheet(egg = selectedEgg!!, onDismiss = { selectedEgg = null })
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "隐藏彩蛋", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            ProgressCard(discoveredEggs = discoveredEggs, allEggs = allEggs)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    listOf("全部", "已解锁", "未解锁").forEach { label ->
                        FilterChip(
                            selected = filter == label,
                            onClick = { filter = label },
                            label = { Text(label, fontSize = DesignTokens.FontSmall) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                }
                IconButton(onClick = { gridMode = !gridMode }) {
                    Icon(
                        if (gridMode) Icons.Default.ViewList else Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.IconLarge)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (gridMode) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredEggs, key = { it.id }) { egg ->
                        val discovered = egg.id in discoveredIds
                        val dEgg = discoveredEggs.find { it.eggId == egg.id }
                        GridEggItem(
                            egg = egg,
                            discovered = discovered,
                            onClick = { if (discovered && dEgg != null) selectedEgg = dEgg }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredEggs, key = { it.id }) { egg ->
                        val discovered = egg.id in discoveredIds
                        val dEgg = discoveredEggs.find { it.eggId == egg.id }
                        EasterEggItem(
                            egg = egg,
                            discovered = discovered,
                            onClick = { if (discovered && dEgg != null) selectedEgg = dEgg }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(discoveredEggs: List<EasterEgg>, allEggs: List<EasterEggDefinition>) {
    val discoveredIds = discoveredEggs.map { it.eggId }.toSet()
    val rarityGroups = allEggs.groupBy { it.rarity }
    val rarityLabels = mapOf("普通" to "普通", "稀有" to "稀有", "传说" to "传说")
    val rarityColors = mapOf("普通" to Color(0xFF9E9E9E), "稀有" to Color(0xFFFFD700), "传说" to Color(0xFFFF8C00))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${discoveredEggs.size}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("已解锁", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${allEggs.size}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("总计", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Divider()
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rarityGroups.forEach { (rarity, eggs) ->
                    val unlocked = eggs.count { it.id in discoveredIds }
                    val color = rarityColors[rarity] ?: Color.Gray
                    val label = rarityLabels[rarity] ?: rarity
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$unlocked/${eggs.size}",
                            fontSize = DesignTokens.FontBody,
                            fontWeight = FontWeight.Medium,
                            color = if (unlocked == eggs.size) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = label,
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EasterEggItem(egg: EasterEggDefinition, discovered: Boolean, onClick: () -> Unit) {
    val borderModifier = if (discovered) {
        when (egg.rarity) {
            "稀有" -> Modifier.border(1.dp, Color(0xFFFFD700), RoundedCornerShape(DesignTokens.CornerLarge))
            "传说" -> Modifier
                .shadow(6.dp, RoundedCornerShape(DesignTokens.CornerLarge), spotColor = Color(0xFFFF8C00).copy(alpha = 0.4f))
                .border(1.dp, Color(0xFFFF8C00), RoundedCornerShape(DesignTokens.CornerLarge))
            else -> Modifier
        }
    } else Modifier

    GlassCard(
        modifier = Modifier.fillMaxWidth().then(borderModifier),
        onClick = if (discovered) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (discovered) egg.title else "???",
                    fontSize = DesignTokens.FontBody,
                    fontWeight = FontWeight.Medium,
                    color = if (discovered) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = if (discovered) egg.description else "尚未发现",
                    fontSize = DesignTokens.FontSmall,
                    color = if (discovered) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            if (discovered) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = when (egg.rarity) {
                        "稀有" -> Color(0xFFFFD700)
                        "传说" -> Color(0xFFFF8C00)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(DesignTokens.IconMedium)
                )
            }
        }
    }
}

@Composable
private fun GridEggItem(egg: EasterEggDefinition, discovered: Boolean, onClick: () -> Unit) {
    val borderModifier = if (discovered) {
        when (egg.rarity) {
            "稀有" -> Modifier.border(1.dp, Color(0xFFFFD700), RoundedCornerShape(DesignTokens.CornerMedium))
            "传说" -> Modifier
                .shadow(6.dp, RoundedCornerShape(DesignTokens.CornerMedium), spotColor = Color(0xFFFF8C00).copy(alpha = 0.4f))
                .border(1.dp, Color(0xFFFF8C00), RoundedCornerShape(DesignTokens.CornerMedium))
            else -> Modifier
        }
    } else Modifier

    GlassCard(
        modifier = Modifier.size(80.dp).then(borderModifier),
        innerPadding = DesignTokens.SpacingXs,
        onClick = if (discovered) onClick else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (discovered) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = when (egg.rarity) {
                            "稀有" -> Color(0xFFFFD700)
                            "传说" -> Color(0xFFFF8C00)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(DesignTokens.IconLarge)
                    )
                    Text(
                        text = egg.title.take(4),
                        fontSize = DesignTokens.FontSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Text(
                    text = "?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun CelebrationDialog(egg: EasterEgg, onDismiss: () -> Unit) {
    val bgAlpha = remember { Animatable(0f) }
    val trophyScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    val particles = remember {
        List(30) {
            val angle = Random.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val r = Random.nextFloat() * 100f + 20f
            Particle(
                x = cos(angle) * r + 100f,
                y = sin(angle) * r + 100f,
                size = Random.nextFloat() * 6f + 3f,
                color = listOf(-3348224, -32832, -38037, 5233655, -8280188).random()
            )
        }
    }

    LaunchedEffect(Unit) {
        bgAlpha.animateTo(0.7f, animationSpec = tween(300))
        delay(200)
        trophyScale.animateTo(1f, animationSpec = tween(500))
        delay(400)
        textAlpha.animateTo(1f, animationSpec = tween(800))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = bgAlpha.value)
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(DesignTokens.SpacingXxl)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer(
                            scaleX = trophyScale.value,
                            scaleY = trophyScale.value,
                            rotationZ = 360f * (1f - trophyScale.value)
                        ),
                    tint = Color(0xFFFFD700)
                )

                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                Canvas(modifier = Modifier.size(200.dp)) {
                    if (trophyScale.value >= 1f) {
                        particles.forEach { p ->
                            drawCircle(
                                color = Color(p.color),
                                radius = p.size * (1f + (1f - textAlpha.value) * 0.5f),
                                center = Offset(p.x, p.y)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                Box(modifier = Modifier.graphicsLayer(alpha = textAlpha.value)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "解锁彩蛋!",
                            fontSize = DesignTokens.FontHeadline,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Text(
                            text = egg.title,
                            fontSize = DesignTokens.FontLarge,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Text(
                            text = egg.description,
                            fontSize = DesignTokens.FontBody,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        if (textAlpha.value >= 1f) {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
                            TextButton(onClick = onDismiss) {
                                Text("太棒了!", color = Color.White, fontSize = DesignTokens.FontMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EggDetailSheet(egg: EasterEgg, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateStr = remember(egg.triggeredAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(egg.triggeredAt))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = DesignTokens.CornerXLarge, topEnd = DesignTokens.CornerXLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.SpacingXxl)
                .padding(bottom = DesignTokens.SpacingXxl)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            Text(
                text = egg.title,
                fontSize = DesignTokens.FontTitle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(
                text = egg.description,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            Divider()
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("触发时间", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dateStr, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("触发条件", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(egg.title, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int
)
