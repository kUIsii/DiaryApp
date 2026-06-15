package com.diary.app.ui.capsule

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.diary.app.data.CapsuleTheme
import com.diary.app.data.TimeCapsule
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.theme.AnimationConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Capsule display state
private enum class CapsuleState {
    LOCKED,   // Not yet due
    SEALED,   // Due but not opened
    OPENED    // Opened and viewed
}

private fun getCapsuleState(capsule: TimeCapsule, now: LocalDate): CapsuleState {
    val unlockDate = Instant.ofEpochMilli(capsule.unlockDate)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        unlockDate.isAfter(now) -> CapsuleState.LOCKED
        !capsule.isOpened -> CapsuleState.SEALED
        else -> CapsuleState.OPENED
    }
}

@Composable
private fun capsuleThemeColor(theme: CapsuleTheme): Color {
    return when (theme) {
        CapsuleTheme.NORMAL -> MaterialTheme.colorScheme.primary
        CapsuleTheme.BIRTHDAY -> Color(0xFFE8A0BF)
        CapsuleTheme.NEW_YEAR -> Color(0xFFE07070)
        CapsuleTheme.GRADUATION -> Color(0xFF9B8EBA)
        CapsuleTheme.TRAVEL -> Color(0xFF78B8B0)
        CapsuleTheme.LOVE -> Color(0xFFD99AB8)
        CapsuleTheme.DREAM -> Color(0xFFA88BC9)
    }
}

@Composable
fun TimeCapsuleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToRead: (Long) -> Unit,
    viewModel: TimeCapsuleViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val capsules by viewModel.capsules.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var capsuleToDelete by remember { mutableStateOf<TimeCapsule?>(null) }

    val now = remember { LocalDate.now() }

    // Group capsules by state
    val locked = remember(capsules, now) {
        capsules.filter { getCapsuleState(it, now) == CapsuleState.LOCKED }
    }
    val sealed = remember(capsules, now) {
        capsules.filter { getCapsuleState(it, now) == CapsuleState.SEALED }
    }
    val opened = remember(capsules, now) {
        capsules.filter { getCapsuleState(it, now) == CapsuleState.OPENED }
    }

    // Delete dialog
    val currentCapsuleToDelete = capsuleToDelete
    if (showDeleteDialog && currentCapsuleToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这封时间胶囊吗？") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    viewModel.deleteCapsule(currentCapsuleToDelete)
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "时间胶囊",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onNavigateToCreate,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "写一封",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (capsules.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyState(
                        icon = Icons.Default.Mail,
                        title = "还没有时间胶囊",
                        subtitle = "写一封信给未来的自己\n到了约定的日子再打开",
                        action = {
                            Button(
                                onClick = onNavigateToCreate,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("写第一封")
                            }
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary card
                    item {
                        SummaryCard(
                            total = capsules.size,
                            lockedCount = locked.size,
                            sealedCount = sealed.size,
                            openedCount = opened.size,
                            onCreate = onNavigateToCreate
                        )
                    }

                    // Sealed section (unopened letters)
                    if (sealed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "未拆的信",
                                count = sealed.size,
                                accent = Color(0xFF6750A4)
                            )
                        }
                        itemsIndexed(
                            items = sealed,
                            key = { _, item -> item.id }
                        ) { index, capsule ->
                            SealedCapsuleCard(
                                capsule = capsule,
                                index = index,
                                onClick = {
                                    haptic.click()
                                    onNavigateToRead(capsule.id)
                                },
                                onDelete = {
                                    capsuleToDelete = capsule
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                    // Locked section (not yet due)
                    if (locked.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "等待中",
                                count = locked.size,
                                accent = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        itemsIndexed(
                            items = locked,
                            key = { _, item -> item.id }
                        ) { index, capsule ->
                            LockedCapsuleCard(
                                capsule = capsule,
                                index = index,
                                onDelete = {
                                    capsuleToDelete = capsule
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                    // Opened section
                    if (opened.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "已打开",
                                count = opened.size,
                                accent = MaterialTheme.colorScheme.primary
                            )
                        }
                        itemsIndexed(
                            items = opened,
                            key = { _, item -> item.id }
                        ) { index, capsule ->
                            OpenedCapsuleCard(
                                capsule = capsule,
                                index = index,
                                onClick = { onNavigateToRead(capsule.id) },
                                onDelete = {
                                    capsuleToDelete = capsule
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    total: Int,
    lockedCount: Int,
    sealedCount: Int,
    openedCount: Int,
    onCreate: () -> Unit
) {
    GlassCard(
        cornerRadius = 22.dp,
        innerPadding = 18.dp,
        modifier = Modifier.fillMaxWidth(),
        enableShadow = true
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "时间胶囊",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sealedCount > 0) "你有 $sealedCount 封未拆的信"
                        else "把某一刻留给未来再打开",
                        fontSize = 13.sp,
                        color = if (sealedCount > 0) Color(0xFF6750A4)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricBlock(label = "总数", value = total.toString(), modifier = Modifier.weight(1f))
                MetricBlock(label = "等待中", value = lockedCount.toString(), modifier = Modifier.weight(1f))
                MetricBlock(label = "未拆", value = sealedCount.toString(), modifier = Modifier.weight(1f))
                MetricBlock(label = "已打开", value = openedCount.toString(), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("写一封给未来的自己")
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accent, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// Sealed capsule card - the key mystery element
@Composable
private fun SealedCapsuleCard(
    capsule: TimeCapsule,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sealed")

    // Pulsing glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val themeColor = capsuleThemeColor(capsule.theme)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
    ) {
        // Glow background
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
                .alpha(glowAlpha * 0.3f)
                .blur(20.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
        )

        GlassCard(
            cornerRadius = 20.dp,
            innerPadding = 16.dp,
            enableShadow = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Envelope icon with glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.15f),
                                    themeColor.copy(alpha = 0.08f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mail,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "你有一封未拆的信",
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击拆开这封来自过去的信",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// Locked capsule card - waiting for unlock date
@Composable
private fun LockedCapsuleCard(
    capsule: TimeCapsule,
    index: Int,
    onDelete: () -> Unit
) {
    val now = remember { LocalDate.now() }
    val unlockDate = remember(capsule) {
        Instant.ofEpochMilli(capsule.unlockDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val daysRemaining = remember(capsule, now) {
        ChronoUnit.DAYS.between(now, unlockDate).toInt().coerceAtLeast(0)
    }
    val createdText = remember(capsule) {
        val created = Instant.ofEpochMilli(capsule.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        "${created.year}年${created.monthValue}月${created.dayOfMonth}日"
    }

    GlassCard(
        cornerRadius = 20.dp,
        innerPadding = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Lock icon
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(12.dp).size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "一封来自过去的信",
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetadataPill(text = createdText)
                    Spacer(modifier = Modifier.width(8.dp))
                    MetadataPill(text = "$daysRemaining 天后解锁")
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// Opened capsule card - normal display
@Composable
private fun OpenedCapsuleCard(
    capsule: TimeCapsule,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val createdText = remember(capsule) {
        val created = Instant.ofEpochMilli(capsule.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        "${created.year}年${created.monthValue}月${created.dayOfMonth}日"
    }
    val unlockText = remember(capsule) {
        val unlock = Instant.ofEpochMilli(capsule.unlockDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        "${unlock.year}年${unlock.monthValue}月${unlock.dayOfMonth}日"
    }
    val previewText = remember(capsule.content) {
        capsule.content.trim().lineSequence().firstOrNull().orEmpty().ifBlank { "没有正文预览" }
    }
    val themeColor = capsuleThemeColor(capsule.theme)

    GlassCard(
        cornerRadius = 20.dp,
        innerPadding = 16.dp,
        enableShadow = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Theme color bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(themeColor, themeColor.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(1.5.dp)
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = capsule.title,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = previewText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Image thumbnail if exists
                if (!capsule.imageUri.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    AsyncImage(
                        model = capsule.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataPill(text = createdText)
                Spacer(modifier = Modifier.width(8.dp))
                MetadataPill(text = "解锁于 $unlockText")
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
        )
    }
}
