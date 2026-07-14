package com.diary.app.ui.ambientsound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard

@Composable
fun AmbientSoundMiniBar(
    state: AmbientSoundState,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visible = state.currentTrack != null
    val track = state.currentTrack
    val isPlaying = state.isPlaying
    val isPreparing = state.isPreparing
    val accent = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            cornerRadius = 16.dp,
            enableShadow = true,
            innerPadding = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 封面小图
                Box(
                    modifier = Modifier.size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (track?.imageRes != 0 && track?.imageRes != null) {
                        Image(
                            painter = painterResource(id = track.imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                // 曲名
                Text(
                    text = track?.name ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // skip-prev
                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // 播放/暂停
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isPreparing) Icons.Default.PlayArrow
                        else if (isPlaying) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // skip-next
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // 关闭（替代"停止"）
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
