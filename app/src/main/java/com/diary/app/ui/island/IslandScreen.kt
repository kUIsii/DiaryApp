package com.diary.app.ui.island

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.CrossSystemManager
import com.diary.app.data.IslandDecoration
import com.diary.app.data.IslandDiscovery
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.nurturing.NurturingJourneyCard
import com.diary.app.ui.nurturing.buildIslandVisualState
import com.diary.app.ui.nurturing.buildNurturingJourneyState
import com.diary.app.ui.nurturing.islandArtRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandScreen(
    onBack: () -> Unit,
    onNavigateToTimeline: () -> Unit = {},
    onNavigateToPet: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    viewModel: IslandViewModel = viewModel()
) {
    val environment by viewModel.environment.collectAsState()
    val effectiveEnvironment by viewModel.effectiveEnvironment.collectAsState()
    val activeBuffs by viewModel.activeBuffs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val unlockedDecorations by viewModel.unlockedDecorations.collectAsState()
    val allDecorations by viewModel.allDecorations.collectAsState()
    val recentUpdate by viewModel.recentUpdate.collectAsState()
    val equippedIds by viewModel.equippedIds.collectAsState()
    val selectedDecoration by viewModel.selectedDecoration.collectAsState()
    val showLevelUp by viewModel.showLevelUp.collectAsState()
    val levelUpInfo by viewModel.levelUpInfo.collectAsState()
    val allDiscoveries by viewModel.allDiscoveries.collectAsState()
    val activeRareElements by viewModel.activeRareElements.collectAsState()
    val recentDiscovery by viewModel.recentDiscovery.collectAsState()
    val activeCombos by viewModel.activeCombos.collectAsState()
    val recentlyUnlockedCombo by viewModel.recentlyUnlockedCombo.collectAsState()
    val activeAnimals by viewModel.activeAnimals.collectAsState()
    val petState by CrossSystemManager.petState.collectAsState()
    val recentAchievementUnlock by CrossSystemManager.recentAchievementUnlock.collectAsState()
    val nextAchievementMilestone by CrossSystemManager.nextAchievementMilestone.collectAsState()
    val petStreakDays by CrossSystemManager.petStreakDays.collectAsState()

    val displayEnvironment = effectiveEnvironment ?: environment
    val islandLevel = profile?.level ?: 1
    val islandEntries = profile?.totalEntries ?: 0
    val islandStreak = profile?.streakDays ?: 0
    val islandVisualState = remember(
        islandLevel,
        petState,
        activeRareElements.size,
        activeBuffs.size,
        activeAnimals.size,
        recentAchievementUnlock
    ) {
        buildIslandVisualState(
            islandLevel = islandLevel,
            petState = petState,
            hasRareDiscovery = activeRareElements.isNotEmpty(),
            activeBuffCount = activeBuffs.size,
            activeAnimalsCount = activeAnimals.size,
            recentAchievementUnlock = recentAchievementUnlock
        )
    }
    val journeyState = remember(
        petState,
        islandLevel,
        recentAchievementUnlock,
        nextAchievementMilestone,
        activeRareElements.size,
        petStreakDays
    ) {
        buildNurturingJourneyState(
            petState = petState,
            islandLevel = islandLevel,
            recentAchievementUnlock = recentAchievementUnlock,
            hasRareDiscovery = activeRareElements.isNotEmpty(),
            nearMilestoneName = nextAchievementMilestone,
            streakDays = petStreakDays
        )
    }
    val explorationHeadline = islandVisualState.headline
    val islandNarrative = remember(displayEnvironment, activeBuffs, activeAnimals.size) {
        val brightnessCopy = when {
            (displayEnvironment?.brightness ?: 0f) >= 0.75f -> "灯光比平时更明净"
            (displayEnvironment?.brightness ?: 0f) <= 0.3f -> "云层压得很低，夜色更浓了"
            else -> "风和光都维持在温柔的中段"
        }
        val buffCopy = if (activeBuffs.isNotEmpty()) "，还叠着${activeBuffs.joinToString("、")}的余温" else ""
        "$brightnessCopy，已经有 $activeAnimals.size 个小生灵在活动$buffCopy。"
    }

    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var showScreenshotPreview by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var canvasBounds by remember { mutableStateOf<Rect?>(null) }
    var showDiscoveryScreen by remember { mutableStateOf(false) }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("心情小岛", fontWeight = FontWeight.Bold)
                            Text(
                                text = "探索舞台 · Lv.$islandLevel",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDiscoveryScreen = true }) {
                            Icon(Icons.Default.Favorite, contentDescription = "发现档案")
                        }
                        IconButton(onClick = onNavigateToTimeline) {
                            Icon(Icons.Default.History, contentDescription = "查看历史")
                        }
                        IconButton(onClick = {
                            val bounds = canvasBounds
                            if (bounds != null) {
                                val fullBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(fullBitmap)
                                view.draw(canvas)
                                val left = bounds.left.toInt().coerceAtLeast(0)
                                val top = bounds.top.toInt().coerceAtLeast(0)
                                val width = bounds.width.toInt().coerceAtMost(fullBitmap.width - left)
                                val height = bounds.height.toInt().coerceAtMost(fullBitmap.height - top)
                                if (width > 0 && height > 0) {
                                    capturedBitmap = Bitmap.createBitmap(fullBitmap, left, top, width, height)
                                    fullBitmap.recycle()
                                    showScreenshotPreview = true
                                } else {
                                    fullBitmap.recycle()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "分享小岛")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 30.dp,
                        enableShadow = true,
                        gradientColors = listOf(
                            Color(0xFF1B3041),
                            Color(0xFF234137),
                            Color(0xFF43526B)
                        ),
                        innerPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "今夜舞台",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD9C79E),
                                        letterSpacing = 1.1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = explorationHeadline,
                                        fontSize = 24.sp,
                                        lineHeight = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF4F0E7)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = islandNarrative,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = Color(0xFFE2DBCF).copy(alpha = 0.88f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0x1AFFF2CB))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Lv.$islandLevel",
                                        fontSize = 12.sp,
                                        color = Color(0xFFF6E7C0),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x26FFFFFF),
                                                Color(0x10F1ECE2)
                                            )
                                        )
                                    )
                                    .onGloballyPositioned { canvasBounds = it.boundsInWindow() },
                                contentAlignment = Alignment.Center
                            ) {
                                environment?.let { env ->
                                    IslandCanvas(
                                        environment = env,
                                        decorations = unlockedDecorations,
                                        activeAnimals = activeAnimals,
                                        activeRareElements = activeRareElements,
                                        activeCombos = activeCombos,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (recentUpdate != null) {
                                    GlassCard(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                                        cornerRadius = 18.dp,
                                        innerPadding = 12.dp
                                    ) {
                                        Text(
                                            text = recentUpdate ?: "",
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IslandActionChip(
                                    modifier = Modifier.weight(1f),
                                    title = "发现档案",
                                    subtitle = "${allDiscoveries.size} 条记录",
                                    icon = Icons.Default.Explore,
                                    onClick = { showDiscoveryScreen = true }
                                )
                                IslandActionChip(
                                    modifier = Modifier.weight(1f),
                                    title = "历史变化",
                                    subtitle = "回看它是怎么长大的",
                                    icon = Icons.Default.History,
                                    onClick = onNavigateToTimeline
                                )
                                IslandActionChip(
                                    modifier = Modifier.weight(1f),
                                    title = "当前秘密",
                                    subtitle = if (activeRareElements.isEmpty()) "今晚暂时安静" else "有 ${activeRareElements.size} 个稀有现象",
                                    icon = Icons.Default.Star,
                                    onClick = { showDiscoveryScreen = true }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IslandMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "记录回声",
                            value = "$islandEntries",
                            subtitle = "总日记数",
                            color = Color(0xFFD6B572)
                        )
                        IslandMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "连续生长",
                            value = "${islandStreak}天",
                            subtitle = "持续记录",
                            color = Color(0xFF8AD7B8)
                        )
                        IslandMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "陈列中的装饰",
                            value = "${equippedIds.size}/5",
                            subtitle = "当前布置",
                            color = Color(0xFF9EC7FF)
                        )
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        innerPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "环境读数",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "这些变化会被宠物状态和坚持记录一起悄悄改写。",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
                                    )
                                }
                                if (activeBuffs.isNotEmpty()) {
                                    Text(
                                        text = activeBuffs.joinToString(" · "),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            displayEnvironment?.let { env ->
                                AnimatedDimensionBar("茂盛度", env.lushness, Color(0xFF66BB6A))
                                AnimatedDimensionBar("明亮度", env.brightness, Color(0xFFFFD54F))
                                AnimatedDimensionBar("宁静度", env.tranquility, Color(0xFF4FC3F7))
                                AnimatedDimensionBar("温暖度", env.warmth, Color(0xFFFF8A65))
                            }
                        }
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        innerPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "探索导览",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Image(
                                painter = painterResource(id = islandArtRes(islandVisualState.artKey)),
                                contentDescription = "小岛装饰概念图",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(132.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IslandNarrativeRow(
                                title = "今夜建议",
                                value = islandVisualState.guidance
                            )
                            IslandNarrativeRow(
                                title = "稀有现象",
                                value = if (activeRareElements.isEmpty()) "今晚还没有新的稀有现象露面" else activeRareElements.joinToString("、") { it.message }
                            )
                            IslandNarrativeRow(
                                title = "活跃生灵",
                                value = if (activeAnimals.isEmpty()) "现在还很安静" else "有 ${activeAnimals.size} 个生灵在场景里活动"
                            )
                            IslandNarrativeRow(
                                title = "组合布置",
                                value = if (activeCombos.isEmpty()) "还没有激活特殊陈设组合" else activeCombos.joinToString("、") { it.name }
                            )
                        }
                    }

                    NurturingJourneyCard(
                        state = journeyState,
                        title = "看完这一圈以后",
                        onOpenPet = onNavigateToPet,
                        onOpenIsland = { showDiscoveryScreen = true },
                        onOpenAchievement = onNavigateToAchievements
                    )

                    if (allDecorations.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            innerPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "装饰收藏",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "把解锁的景物慢慢摆进这座岛，舞台会越来越像你自己的地方。",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
                                )
                                LazyColumn {
                                    items(allDecorations) { decoration ->
                                        DecorationItem(
                                            decoration = decoration,
                                            isEquipped = decoration.id in equippedIds,
                                            onClick = { viewModel.selectDecoration(decoration) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            AnimatedVisibility(
                visible = showLevelUp,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                levelUpInfo?.let { (oldLevel, newLevel) ->
                    LevelUpCard(
                        oldLevel = oldLevel,
                        newLevel = newLevel,
                        onDismiss = { viewModel.dismissLevelUp() }
                    )
                }
            }

            AnimatedVisibility(
                visible = recentlyUnlockedCombo != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                recentlyUnlockedCombo?.let { combo ->
                    ComboUnlockCard(
                        comboName = combo.name,
                        unlockMessage = combo.unlockMessage,
                        onDismiss = { viewModel.dismissComboUnlock() }
                    )
                }
            }

            AnimatedVisibility(
                visible = recentDiscovery != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                recentDiscovery?.let { discovery ->
                    DiscoveryNotificationCard(
                        discovery = discovery,
                        onDismiss = { viewModel.dismissDiscovery() }
                    )
                }
            }
        }
    }

    selectedDecoration?.let { decoration ->
        DecorationDetailDialog(
            decoration = decoration,
            isEquipped = decoration.id in equippedIds,
            equippedCount = equippedIds.size,
            onEquip = {
                viewModel.toggleDecoration(decoration.id)
                viewModel.selectDecoration(null)
            },
            onDismiss = { viewModel.selectDecoration(null) }
        )
    }

    if (showScreenshotPreview) {
        capturedBitmap?.let { bitmap ->
            val level = profile?.level ?: 1
            AlertDialog(
                onDismissRequest = { showScreenshotPreview = false },
                title = { Text(text = "截图预览", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "截图预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "我的心情小岛 Lv.$level",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            showScreenshotPreview = false
                            coroutineScope.launch {
                                shareScreenshot(context, bitmap, level)
                            }
                        }) {
                            Text("分享")
                        }
                        OutlinedButton(onClick = {
                            showScreenshotPreview = false
                            coroutineScope.launch {
                                val success = saveScreenshot(context, bitmap)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        if (success) "已保存到相册" else "保存失败",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }) {
                            Text("保存")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showScreenshotPreview = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }

    if (showDiscoveryScreen) {
        IslandDiscoveryScreen(
            discoveries = allDiscoveries,
            onBack = { showDiscoveryScreen = false }
        )
    }
}

@Composable
private fun IslandActionChip(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        innerPadding = 14.dp,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFF5D89D),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF3EFE4)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFFE0D8CB).copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun IslandMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        innerPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun IslandNarrativeRow(
    title: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AnimatedProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = animatedProgress)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun AnimatedDimensionBar(label: String, value: Float, color: Color) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = label
    )

    var flashAlpha by remember { mutableFloatStateOf(0f) }
    val animatedColor by animateColorAsState(
        targetValue = color.copy(alpha = 0.5f + flashAlpha * 0.5f),
        animationSpec = tween(durationMillis = 300),
        label = "color"
    )

    LaunchedEffect(value) {
        if (value > 0f) {
            flashAlpha = 1f
            delay(300)
            flashAlpha = 0f
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(60.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedValue.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(animatedColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(animatedValue * 100).toInt()}%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DecorationItem(
    decoration: IslandDecoration,
    isEquipped: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isEquipped -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        decoration.isUnlocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (decoration.type) {
                    "building" -> "B"
                    "animal" -> "A"
                    "vegetation" -> "V"
                    "effect" -> "E"
                    else -> "?"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isEquipped -> MaterialTheme.colorScheme.primary
                    decoration.isUnlocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = decoration.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    isEquipped -> MaterialTheme.colorScheme.primary
                    decoration.isUnlocked -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Text(
                text = when {
                    isEquipped -> "已陈列"
                    decoration.isUnlocked -> "已收藏"
                    else -> "Lv.${decoration.unlockLevel} 解锁"
                },
                fontSize = 12.sp,
                color = when {
                    isEquipped -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
        }
        if (isEquipped) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已装备",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        } else if (decoration.isUnlocked) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "已解锁",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DecorationDetailDialog(
    decoration: IslandDecoration,
    isEquipped: Boolean,
    equippedCount: Int,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (decoration.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (decoration.type) {
                        "building" -> "B"
                        "animal" -> "A"
                        "vegetation" -> "V"
                        "effect" -> "E"
                        else -> "?"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (decoration.isUnlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        },
        title = {
            Text(
                text = decoration.name,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (decoration.type) {
                        "building" -> "建筑"
                        "animal" -> "动物"
                        "vegetation" -> "植被"
                        "effect" -> "特效"
                        else -> "未知"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "解锁等级: Lv.${decoration.unlockLevel}",
                    fontSize = 14.sp,
                    color = if (decoration.isUnlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = decoration.unlockCondition,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                if (!decoration.isUnlocked) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "需要达到 Lv.${decoration.unlockLevel} 才能解锁",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            if (decoration.isUnlocked) {
                Button(
                    onClick = onEquip,
                    enabled = isEquipped || equippedCount < 5,
                    colors = if (isEquipped) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (isEquipped) "卸下" else "陈列")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private suspend fun shareScreenshot(context: Context, bitmap: Bitmap, level: Int) {
    withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "island_screenshots")
        cacheDir.mkdirs()
        val file = File(cacheDir, "island_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "我的心情小岛 Lv.$level")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享小岛截图"))
        }
    }
}

private suspend fun saveScreenshot(context: Context, bitmap: Bitmap): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val filename = "island_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DiaryApp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            uri?.let {
                resolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, values, null, null)
                }
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
private fun LevelUpCard(
    oldLevel: Int,
    newLevel: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "恭喜升级!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Lv.$oldLevel -> Lv.$newLevel",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ComboUnlockCard(
    comboName: String,
    unlockMessage: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "组合解锁: $comboName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = unlockMessage,
                    fontSize = 13.sp,
                    color = Color(0xFFBF360C).copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color(0xFFE65100).copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DiscoveryNotificationCard(
    discovery: IslandDiscovery,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "发现了新的秘密!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = discovery.message,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color(0xFF999999)
                )
            }
        }
    }
}
