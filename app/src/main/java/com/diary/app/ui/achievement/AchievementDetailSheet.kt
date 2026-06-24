package com.diary.app.ui.achievement

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.height(24.dp))

            AchievementBadgeLarge(emoji = item.def.iconEmoji, tier = item.def.tier, isUnlocked = item.isUnlocked)

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
                text = if (item.isHiddenLocked) "\u5B8C\u6210\u7279\u5B9A\u6761\u4EF6\u89E3\u9501\u6B64\u9690\u85CF\u6210\u5C31" else item.def.description,
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
                        Text(text = "\u89E3\u9501\u8FDB\u5EA6", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text(text = "\u8FD8\u5DEE $remaining \u8FBE\u6210", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            if (item.isUnlocked && unlockTimeText != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "\u89E3\u9501\u65F6\u95F4", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(text = unlockTimeText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AchievementBadgeLarge(emoji: String, tier: AchievementTier, isUnlocked: Boolean) {
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

    Box(
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Text(text = if (isUnlocked) emoji else "\uD83D\uDD12", fontSize = 36.sp)
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
