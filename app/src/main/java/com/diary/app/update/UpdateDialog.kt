package com.diary.app.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart

@Composable
fun UpdateDialog(
    versionName: String,
    releaseNotes: String,
    isDownloading: Boolean,
    downloadProgress: Float = -1f,
    isForceUpdate: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = { if (!isDownloading && !isForceUpdate) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(200)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    initialScale = 0.92f
                ),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f)
            ) {
                GlassCard(
                    cornerRadius = 20.dp,
                    enableShadow = true,
                    innerPadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Gradient accent bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(DarkAccentStart, DarkAccentEnd)
                                    )
                                )
                        )

                        Column(modifier = Modifier.padding(24.dp)) {
                            // Header with icon
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(DarkAccentStart, DarkAccentEnd)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "发现新版本",
                                        fontSize = 13.sp,
                                        color = textSecondary,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "v$versionName",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }

                            // Release notes
                            if (releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.4f
                                            )
                                        )
                                        .verticalScroll(rememberScrollState())
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = releaseNotes,
                                        fontSize = 13.sp,
                                        color = textSecondary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            // Download progress
                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(20.dp))
                                GlassDownloadProgress(progress = downloadProgress)
                            }

                            // Action buttons
                            if (!isDownloading) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Dismiss button (hidden for force update)
                                    if (!isForceUpdate) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { onDismiss() }
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "稍后提醒",
                                                fontSize = 15.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }

                                    // Confirm button
                                    Box(
                                        modifier = Modifier
                                            .then(if (isForceUpdate) Modifier.fillMaxWidth() else Modifier.weight(1f))
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(DarkAccentStart, DarkAccentEnd)
                                                )
                                            )
                                            .clickable { onConfirm() }
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isForceUpdate) "立即更新（必须）" else "立即更新",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassDownloadProgress(progress: Float = -1f) {
    val accentGradient = Brush.horizontalGradient(
        listOf(DarkAccentStart, DarkAccentEnd)
    )
    val displayProgress = if (progress in 0f..1f) progress else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = displayProgress,
        animationSpec = tween(durationMillis = 300),
        label = "downloadProgress"
    )
    val percentText = if (progress in 0f..1f) {
        "${(progress * 100).toInt()}%"
    } else {
        "准备中..."
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentGradient)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "正在下载更新包 $percentText",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
