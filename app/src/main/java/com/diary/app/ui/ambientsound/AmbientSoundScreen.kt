@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.ambientsound

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.diary.app.data.ambientsound.AudioRepository
import com.diary.app.data.ambientsound.AudioTrack
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.PageHeader

@Composable
fun AmbientSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientSoundViewModel
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(state.currentTrack) {
        if (state.currentTrack != null) {
            viewModel.startProgressUpdates()
        } else {
            viewModel.stopProgressUpdates()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopProgressUpdates() }
    }

    val tracks = orderAmbientTracksForDisplay(
        tracks = AudioRepository.getAllTracks()
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                PageHeader(title = "场景环境音", onNavigateBack = onNavigateBack)

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        TrackCard(
                            track = track,
                            isActive = track.id == state.currentTrack?.id,
                            isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                            isPreparing = state.isPreparing && track.id == state.currentTrack?.id,
                            accent = MaterialTheme.colorScheme.primary,
                            onPlay = { viewModel.togglePlay(track) }
                        )
                    }
                }

                // 迷你播放器 —— 只在有播放 session 时显示
                AnimatedVisibility(
                    visible = state.currentTrack != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    val track = state.currentTrack ?: return@AnimatedVisibility
                    MiniPlayer(
                        track = track,
                        isPlaying = state.isPlaying,
                        isPreparing = state.isPreparing,
                        onTogglePlay = { viewModel.togglePlay(track) },
                        onNext = { viewModel.playNext() },
                        onPrev = { viewModel.playPrev() },
                        onClose = { viewModel.stop() }
                    )
                }
            }
        }
    }
}

// ── 迷你播放器 ──
@Composable
private fun MiniPlayer(
    track: AudioTrack,
    isPlaying: Boolean,
    isPreparing: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        cornerRadius = 16.dp,
        enableShadow = true,
        innerPadding = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面小图
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (track.imageRes != 0) {
                    Image(painter = painterResource(id = track.imageRes),
                        contentDescription = null, modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                }
            }
            Spacer(Modifier.width(10.dp))
            // 曲名
            Text(track.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            // 控制
            IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = null,
                    tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isPreparing) Icons.Default.MusicNote else if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = accent, modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = null,
                    tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── 曲目卡片 ──
@Composable
private fun TrackCard(
    track: AudioTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    isPreparing: Boolean,
    accent: Color,
    onPlay: () -> Unit
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        enableShadow = isPlaying,
        innerPadding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().clickable { onPlay() }) {
            if (isPlaying) {
                Box(modifier = Modifier.matchParentSize().background(accent.copy(alpha = 0.06f)))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(if (isPlaying) accent.copy(alpha = 0.12f) else surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.imageRes != 0) {
                        Image(painter = painterResource(id = track.imageRes),
                            contentDescription = null, modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null,
                            tint = if (isPlaying) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        color = if (isActive) accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (isPreparing) "加载中..." else track.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
