package com.diary.app.ui.island

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import com.diary.app.data.IslandDecoration
import com.diary.app.data.IslandDiscovery
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

    // 隐藏发现系统
    val allDiscoveries by viewModel.allDiscoveries.collectAsState()
    val activeRareElements by viewModel.activeRareElements.collectAsState()
    val recentDiscovery by viewModel.recentDiscovery.collectAsState()

    // 组合效果系统
    val activeCombos by viewModel.activeCombos.collectAsState()
    val recentlyUnlockedCombo by viewModel.recentlyUnlockedCombo.collectAsState()

    // 动物行为系统
    val activeAnimals by viewModel.activeAnimals.collectAsState()

    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var showScreenshotPreview by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var canvasBounds by remember { mutableStateOf<Rect?>(null) }
    var showDiscoveryScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("心情小岛", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lv.${profile?.level ?: 1}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 发现档案按钮
                    IconButton(onClick = { showDiscoveryScreen = true }) {
                        Icon(Icons.Default.Favorite, contentDescription = "发现档案")
                    }
                    // 查看历史按钮
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 小岛展示区
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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

                    // 更新消息
                    if (recentUpdate != null) {
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopCenter),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = recentUpdate ?: "",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 经验条
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "经验值",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${profile?.experience ?: 0} / ${com.diary.app.data.MoodEnvironmentMapper.getExperienceForLevel((profile?.level ?: 1) + 1)}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = if (profile != null) {
                            val currentExp = profile!!.experience
                            val nextLevelExp = com.diary.app.data.MoodEnvironmentMapper.getExperienceForLevel(profile!!.level + 1)
                            currentExp.toFloat() / nextLevelExp
                        } else {
                            0f
                        }
                        AnimatedProgressBar(progress = progress.coerceIn(0f, 1f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 统计信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Star,
                        title = "日记总数",
                        value = "${profile?.totalEntries ?: 0}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Favorite,
                        title = "连续记录",
                        value = "${profile?.streakDays ?: 0}天",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Info,
                        title = "已装备装饰",
                        value = "${equippedIds.size}/5",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 环境维度
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "环境状态",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            // 显示buff状态
                            if (activeBuffs.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    activeBuffs.forEach { buff ->
                                        Text(
                                            text = buff,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 使用有效环境（包含宠物状态修正和buff）
                        val displayEnv = effectiveEnvironment ?: environment
                        displayEnv?.let { env ->
                            AnimatedDimensionBar("茂盛度", env.lushness, Color(0xFF66BB6A))
                            AnimatedDimensionBar("明亮度", env.brightness, Color(0xFFFFD54F))
                            AnimatedDimensionBar("宁静度", env.tranquility, Color(0xFF4FC3F7))
                            AnimatedDimensionBar("温暖度", env.warmth, Color(0xFFFF8A65))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 装饰列表
                if (allDecorations.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "装饰收藏",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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
            }
        }

        // 升级提示卡片 (放在最外层 Box 中，避免 ColumnScope 冲突)
        androidx.compose.animation.AnimatedVisibility(
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

        // 组合解锁提示卡片
        androidx.compose.animation.AnimatedVisibility(
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

        // 发现通知卡片
        androidx.compose.animation.AnimatedVisibility(
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

    // 装饰详情弹窗
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

    // 截图预览弹窗
    if (showScreenshotPreview) {
        capturedBitmap?.let { bitmap ->
            val level = profile?.level ?: 1
            AlertDialog(
                onDismissRequest = { showScreenshotPreview = false },
                title = {
                    Text(text = "截图预览", fontWeight = FontWeight.Bold)
                },
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

    // 发现档案界面
    if (showDiscoveryScreen) {
        IslandDiscoveryScreen(
            discoveries = allDiscoveries,
            onBack = { showDiscoveryScreen = false }
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

    // 颜色闪烁效果
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
private fun InfoCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
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
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
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
                    isEquipped -> "已装备"
                    decoration.isUnlocked -> "已解锁"
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
                // 装饰类型
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

                // 解锁等级
                Text(
                    text = "解锁等级: Lv.${decoration.unlockLevel}",
                    fontSize = 14.sp,
                    color = if (decoration.isUnlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 解锁条件
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
                    colors = if (isEquipped) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    ) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (isEquipped) "卸下" else "装备")
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
        } catch (e: Exception) {
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

/**
 * 组合解锁提示卡片
 */
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // 暖橙色背景
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

/**
 * 发现通知卡片
 */
@Composable
private fun DiscoveryNotificationCard(
    discovery: IslandDiscovery,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)  // 暖橙色背景
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
