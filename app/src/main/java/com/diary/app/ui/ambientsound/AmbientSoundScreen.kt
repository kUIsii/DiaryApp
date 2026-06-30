@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.ambientsound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.diary.app.data.ambientsound.AudioCategory
import com.diary.app.data.ambientsound.AudioRepository
import com.diary.app.data.ambientsound.AudioTrack
import com.diary.app.ui.components.PageHeader

@Composable
fun AmbientSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientSoundViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    val categories = AudioRepository.categories
    val tracks = AudioRepository.getTracks(state.selectedCategoryId)
    val category = categories.find { it.id == state.selectedCategoryId }

    LaunchedEffect(state.currentTrack) {
        if (state.currentTrack != null) {
            viewModel.startProgressUpdates()
        } else {
            showFullPlayer = false
            viewModel.stopProgressUpdates()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopProgressUpdates() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showFullPlayer && state.currentTrack != null) {
            FullscreenPlayer(
                track = state.currentTrack!!,
                isPlaying = state.isPlaying,
                volume = state.volume,
                progress = state.progress,
                duration = state.duration,
                sleepRemaining = state.sleepRemainingSeconds,
                isFavorite = state.currentTrack!!.id in state.favoriteIds,
                backgroundImageUrl = category?.backgroundImageUrl,
                onTogglePlay = { viewModel.togglePlay(state.currentTrack!!) },
                onStop = { viewModel.stop(); showFullPlayer = false },
                onSeek = { viewModel.seekTo(it) },
                onVolumeChange = { viewModel.setVolume(it) },
                onToggleFavorite = { viewModel.toggleFavorite(state.currentTrack!!.id) },
                onSleepTimer = { viewModel.startSleepTimer(it) },
                onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                onBack = { showFullPlayer = false }
            )
        } else {
            BrowseView(
                categories = categories,
                selectedCategoryId = state.selectedCategoryId,
                tracks = tracks,
                currentTrack = state.currentTrack,
                isPlaying = state.isPlaying,
                favoriteIds = state.favoriteIds,
                isDownloading = state.isDownloading,
                onSelectCategory = { viewModel.selectCategory(it) },
                onTogglePlay = { viewModel.togglePlay(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTrackClick = { showFullPlayer = true },
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
private fun BrowseView(
    categories: List<AudioCategory>,
    selectedCategoryId: String,
    tracks: List<AudioTrack>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    isDownloading: Boolean,
    onSelectCategory: (String) -> Unit,
    onTogglePlay: (AudioTrack) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTrackClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "\u573A\u666F\u73AF\u5883\u97F3",
            onNavigateBack = onNavigateBack
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = cat.id == selectedCategoryId,
                    onClick = { onSelectCategory(cat.id) },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackCard(
                    track = track,
                    isActive = track.id == currentTrack?.id,
                    isPlaying = isPlaying && track.id == currentTrack?.id,
                    isFavorite = track.id in favoriteIds,
                    isDownloading = isDownloading && track.id == currentTrack?.id,
                    onPlay = { onTogglePlay(track) },
                    onFavorite = { onToggleFavorite(track.id) },
                    onClick = { if (track.id == currentTrack?.id) onTrackClick() }
                )
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: AudioTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context).data(track.imageUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFE07070) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                track.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FullscreenPlayer(
    track: AudioTrack,
    isPlaying: Boolean,
    volume: Float,
    progress: Int,
    duration: Int,
    sleepRemaining: Int,
    isFavorite: Boolean,
    backgroundImageUrl: String?,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(0xFFCCA090)
    val bgOverlay = Color(0xCC1C1511)

    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(backgroundImageUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(bgOverlay))

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFF2E3DA))
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFE07070) else Color(0xFFF2E3DA)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AsyncImage(
                model = ImageRequest.Builder(context).data(track.imageUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.size(240.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                track.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF2E3DA)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "\u8FDB\u5EA6",
                fontSize = 12.sp,
                color = Color(0xFF9A8579)
            )
            Slider(
                value = if (duration > 0) progress.toFloat() / duration else 0f,
                onValueChange = { onSeek((it * duration).toInt()) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color(0xFF9A8579).copy(alpha = 0.3f)
                )
            )
            Text(
                "${progress / 60000}:${(progress / 1000) % 60}/" +
                        "${duration / 60000}:${(duration / 1000) % 60}",
                fontSize = 11.sp,
                color = Color(0xFF9A8579)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "\u97F3\u91CF",
                fontSize = 12.sp,
                color = Color(0xFF9A8579)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF9A8579), modifier = Modifier.size(16.dp))
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = Color(0xFF9A8579).copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (sleepRemaining > 0) onCancelSleepTimer() else onSleepTimer(30)
                    }
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (sleepRemaining > 0) "${sleepRemaining / 60}:${sleepRemaining % 60}" else "\u7761\u7720\u5B9A\u65F6",
                        fontSize = 12.sp,
                        color = accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color(0xFFF2E3DA), modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = onTogglePlay,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF1C1511),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color(0xFFF2E3DA), modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
