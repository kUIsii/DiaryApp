package com.diary.app.ui.achievement

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.diary.app.data.AchievementItem
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AchievementDetailScreen(
    achievementKey: String,
    viewModel: AchievementViewModel,
    onNavigateBack: () -> Unit
) {
    val allItems by viewModel.allItems.collectAsState()
    val item = allItems.find { it.def.key == achievementKey }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("成就未找到", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val context = LocalContext.current
    val imageRes = context.resources.getIdentifier("achievement_${item.def.key}", "drawable", context.packageName)
    val isLocked = !item.isUnlocked
    val scrollState = rememberScrollState()
    val tierCol = tierColor(item.def.tier)
    val catCol = categoryColor(item.def.category)
    var showFullScreenImage by remember { mutableStateOf(false) }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    // Full screen image dialog
    if (showFullScreenImage && imageRes != 0) {
        Dialog(
            onDismissRequest = { showFullScreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullScreenImage = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = item.def.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullScreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "成就详情",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable(enabled = imageRes != 0) { showFullScreenImage = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRes != 0) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = item.def.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp).padding(32.dp)
                        )
                    }

                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "未解锁",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tier
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = tierCol.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(tierCol)
                            )
                            Text(
                                text = item.def.tier.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = tierCol
                            )
                        }
                    }

                    // Category
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = catCol.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(catCol)
                            )
                            Text(
                                text = item.def.category.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = catCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = if (isHiddenLocked(item)) "???" else item.def.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = if (isHiddenLocked(item)) "这是一个隐藏成就，达成后才能看到具体内容。" else item.def.description,
                    fontSize = 13.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    innerPadding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "进度",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                            Text(
                                text = "${item.state.progress} / ${item.def.target}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = item.progressFraction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (item.isUnlocked) MaterialTheme.colorScheme.primary else tierCol,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        if (item.isUnlocked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已达成",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                item.state.unlockedAt?.let { time ->
                                    Text(
                                        text = " · ${formatTime(time)}",
                                        fontSize = 11.sp,
                                        color = textTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // Flavor text
                if (item.isUnlocked && item.def.flavorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        innerPadding = 14.dp
                    ) {
                        Text(
                            text = item.def.flavorText,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Surface(
    shape: RoundedCornerShape,
    color: Color,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Surface(
        shape = shape,
        color = color,
        content = content
    )
}

private fun isHiddenLocked(item: AchievementItem): Boolean {
    return item.def.isHidden && !item.isUnlocked
}

private fun formatTime(timestamp: Long): String {
    return try {
        SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}
