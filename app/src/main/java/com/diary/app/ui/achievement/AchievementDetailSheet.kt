package com.diary.app.ui.achievement

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementTier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CommonColor = Color(0xFF78909C)
private val RareColor = Color(0xFF42A5F5)
private val EpicColor = Color(0xFFAB47BC)
private val LegendaryColor = Color(0xFFFFC107)

private fun tierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.COMMON -> CommonColor
    AchievementTier.RARE -> RareColor
    AchievementTier.EPIC -> EpicColor
    AchievementTier.LEGENDARY -> LegendaryColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailSheet(item: AchievementItem, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tierColor = tierColor(item.def.tier)
    val context = LocalContext.current

    val unlockTimeText = remember(item.state.unlockedAt) {
        item.state.unlockedAt?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.height(24.dp))

            AchievementBadgeLarge(
                achievementKey = item.def.key,
                category = item.def.category,
                tier = item.def.tier,
                isUnlocked = item.isUnlocked,
                name = if (item.isHiddenLocked) "???" else item.def.name
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (item.isHiddenLocked) "???" else item.def.name,
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (item.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(4.dp))
            TierBadge(tier = item.def.tier)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (item.isHiddenLocked) "完成特定条件解锁此隐藏成就" else item.def.description,
                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center, lineHeight = 22.sp
            )

            if (item.isUnlocked && item.def.flavorText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.def.flavorText, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    lineHeight = 20.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!item.isUnlocked) {
                val animProgress by animateFloatAsState(targetValue = item.progressFraction, animationSpec = tween(600), label = "progress")
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "解锁进度", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(text = "${item.state.progress} / ${item.def.target}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = animProgress,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = tierColor, trackColor = tierColor.copy(alpha = 0.12f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val remaining = (item.def.target - item.state.progress).coerceAtLeast(0)
                    Text(text = "还差 $remaining 达成", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            if (item.isUnlocked && unlockTimeText != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "解锁时间", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(text = unlockTimeText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            if (item.isUnlocked) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = {
                        val shareText = buildString {
                            append("我在日记APP达成了成就: ${item.def.name}\n")
                            append("${item.def.description}\n")
                            if (item.def.flavorText.isNotBlank()) append("\"${item.def.flavorText}\"\n")
                            append("${item.def.tier.displayName} | ${item.def.category.displayName}")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享成就"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分享成就")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AchievementBadgeLarge(
    achievementKey: String,
    category: AchievementCategory,
    tier: AchievementTier,
    isUnlocked: Boolean,
    name: String = ""
) {
    val imageRes = rememberAchievementImageRes(achievementKey)
    val backgroundBrush = if (isUnlocked) {
        when (tier) {
            AchievementTier.COMMON -> Brush.linearGradient(listOf(Color(0xFF90A4AE), Color(0xFF78909C)))
            AchievementTier.RARE -> Brush.linearGradient(listOf(Color(0xFF82B1FF), Color(0xFF42A5F5)))
            AchievementTier.EPIC -> Brush.linearGradient(listOf(Color(0xFFCE93D8), Color(0xFFAB47BC)))
            AchievementTier.LEGENDARY -> Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFC107), Color(0xFFFFA000)))
        }
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)))
    }

    val badgeSize = if (isUnlocked && imageRes != null) 120.dp else 80.dp
    val cornerRadiusValue = if (isUnlocked && imageRes != null) 24 else 20

    // Celebration glow animation for unlocked achievements
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowScale"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow ring for unlocked achievements
        if (isUnlocked) {
            val glowColor = when (tier) {
                AchievementTier.COMMON -> Color(0xFF90A4AE)
                AchievementTier.RARE -> Color(0xFF42A5F5)
                AchievementTier.EPIC -> Color(0xFFAB47BC)
                AchievementTier.LEGENDARY -> Color(0xFFFFC107)
            }
            Box(
                modifier = Modifier
                    .size((badgeSize + 24.dp) * glowScale)
                    .clip(RoundedCornerShape((cornerRadiusValue + 8).dp))
                    .background(glowColor.copy(alpha = glowAlpha * 0.18f))
            )
        }

        Box(
            modifier = Modifier.size(badgeSize).clip(RoundedCornerShape(cornerRadiusValue.dp)).background(backgroundBrush),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked && imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(cornerRadiusValue.dp))
                )
            } else if (isUnlocked) {
                AchievementIcon(
                    achievementKey = achievementKey,
                    category = category,
                    tier = tier,
                    isUnlocked = true,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                LockIcon(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun TierBadge(tier: AchievementTier) {
    val color = tierColor(tier)
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = tier.displayName, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
