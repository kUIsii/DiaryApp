package com.diary.app.ui.capsule

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.diary.app.data.CapsuleTheme
import com.diary.app.data.DiaryPreview
import com.diary.app.data.TimeCapsule
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.components.rememberHapticFeedback
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadCapsuleScreen(
    capsuleId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TimeCapsuleViewModel
) {
    val haptic = rememberHapticFeedback()
    val context = LocalContext.current

    var capsule by remember { mutableStateOf<TimeCapsule?>(null) }
    var memoryEntry by remember { mutableStateOf<DiaryPreview?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Opening ceremony state
    var isOpened by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    // Animation values
    val envelopeScale by animateFloatAsState(
        targetValue = if (isAnimating) 1.1f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "envelopeScale"
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isOpened) 0f else 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isOpened) 1f else 0f,
        animationSpec = tween(400, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )
    val contentOffsetY by animateFloatAsState(
        targetValue = if (isOpened) 0f else 100f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "contentOffsetY"
    )

    // Trigger opening after animation
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            kotlinx.coroutines.delay(300)
            isOpened = true
        }
    }

    // Load capsule
    LaunchedEffect(capsuleId) {
        capsule = viewModel.getCapsuleById(capsuleId)
        capsule?.let { c ->
            isOpened = c.isOpened
            memoryEntry = viewModel.getDiaryNearCreation(c.createdAt)
        }
    }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除后将无法恢复这封时间胶囊") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    capsule?.let { viewModel.deleteCapsule(it) }
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    val currentCapsule = capsule

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { Text("来自过去的信") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (isOpened) {
                            IconButton(onClick = {
                                currentCapsule?.let { capsule ->
                                    val shareText = buildString {
                                        appendLine("【时间胶囊】${capsule.title}")
                                        appendLine()
                                        appendLine(capsule.content)
                                        appendLine()
                                        appendLine("---")
                                        appendLine("来自「日记本」App")
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享胶囊"))
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "分享")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                    )
                )
            }
        ) { innerPadding ->
            if (currentCapsule != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Envelope overlay (initial state)
                    if (!isOpened) {
                        val infiniteTransition = rememberInfiniteTransition(label = "envelope")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.7f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(overlayAlpha)
                                .clickable {
                                    haptic.success()
                                    isAnimating = true
                                    viewModel.markOpened(currentCapsule.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Glow effect
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .alpha(glowAlpha)
                                    .blur(40.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                capsuleThemeColor(currentCapsule.theme).copy(alpha = 0.5f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )

                            // Envelope icon
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.scale(envelopeScale)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.size(120.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Mail,
                                            contentDescription = null,
                                            tint = capsuleThemeColor(currentCapsule.theme),
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = "点击拆开",
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "这封信来自 ${formatTimestamp(currentCapsule.createdAt)}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Content (after opening)
                    AnimatedVisibility(
                        visible = isOpened,
                        enter = fadeIn(tween(500)) + slideInVertically(
                            tween(600, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 4 }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Title
                            Text(
                                text = currentCapsule.title,
                                fontSize = 26.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Time info
                            Text(
                                text = "写于 ${formatTimestamp(currentCapsule.createdAt)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
                                    .height(1.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                        RoundedCornerShape(0.5.dp)
                                    )
                            )

                            // Image if exists
                            if (!currentCapsule.imageUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = currentCapsule.imageUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }

                            // Content
                            Text(
                                text = currentCapsule.content,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Memory card from diary near creation date
                            memoryEntry?.let { entry ->
                                Spacer(modifier = Modifier.height(8.dp))
                                MemoryCard(entry = entry)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
}

@Composable
private fun MemoryCard(entry: DiaryPreview) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "当时的记忆",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Text(
                    text = formatTimestamp(entry.createdAt).take(10),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Weather
                entry.weather?.takeIf { it.isNotBlank() }?.let { weather ->
                    val (icon, tint) = weatherIconFor(weather)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = weather,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mood
                entry.moodLevel?.let { level ->
                    val (icon, tint) = moodIconForLevel(level)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Location
            entry.location?.takeIf { it.isNotBlank() }?.let { location ->
                Text(
                    text = location,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
