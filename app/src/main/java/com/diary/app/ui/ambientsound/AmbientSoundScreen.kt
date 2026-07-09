@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.ambientsound

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.diary.app.data.ambientsound.AudioCategory
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

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val categories = AudioRepository.categories
    val tracks = orderAmbientTracksForDisplay(
        tracks = AudioRepository.getTracks(state.selectedCategoryId),
        favoriteIds = state.favoriteIds,
        recentIds = state.recentIds
    )
    val category = categories.find { it.id == state.selectedCategoryId }

    LaunchedEffect(state.currentTrack) {
        if (state.currentTrack != null) {
            viewModel.startProgressUpdates()
        } else {
            viewModel.stopProgressUpdates()
            viewModel.hideFullscreenPlayer()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopProgressUpdates() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isFullscreenPlayerVisible && state.currentTrack != null) {
            FullscreenPlayer(
                track = state.currentTrack!!,
                isPlaying = state.isPlaying,
                volume = state.volume,
                sleepRemaining = state.sleepRemainingSeconds,
                isFavorite = state.currentTrack!!.id in state.favoriteIds,
                backgroundImageUrl = category?.backgroundImageUrl,
                categoryId = category?.id,
                onTogglePlay = { viewModel.togglePlay(state.currentTrack!!) },
                onStop = { viewModel.stop() },
                onVolumeChange = { viewModel.setVolume(it) },
                onToggleFavorite = { viewModel.toggleFavorite(state.currentTrack!!.id) },
                onSleepTimer = { viewModel.startSleepTimer(it) },
                onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                onBack = { viewModel.hideFullscreenPlayer() }
            )
        } else {
            BrowseView(
                categories = categories,
                selectedCategoryId = state.selectedCategoryId,
                tracks = tracks,
                currentTrack = state.currentTrack,
                isPlaying = state.isPlaying,
                volume = state.volume,
                favoriteIds = state.favoriteIds,
                recentIds = state.recentIds,
                sleepRemaining = state.sleepRemainingSeconds,
                isPreparing = state.isPreparing,
                meanderEnabled = state.meanderEnabled,
                onSelectCategory = { viewModel.selectCategory(it) },
                onTogglePlay = { viewModel.togglePlay(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTrackClick = { track ->
                    if (track == state.currentTrack) {
                        viewModel.showFullscreenPlayer()
                    } else {
                        viewModel.togglePlay(track)
                    }
                },
                onVolumeChange = { viewModel.setVolume(it) },
                onSleepTimer = { viewModel.startSleepTimer(it) },
                onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                onStop = { viewModel.stop() },
                onToggleMeander = { viewModel.toggleMeander() },
                onNavigateBack = onNavigateBack,
                snackbarHostState = snackbarHostState
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
    volume: Float,
    favoriteIds: Set<String>,
    recentIds: List<String>,
    sleepRemaining: Int,
    isPreparing: Boolean,
    meanderEnabled: Boolean,
    onSelectCategory: (String) -> Unit,
    onTogglePlay: (AudioTrack) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTrackClick: (AudioTrack) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onStop: () -> Unit,
    onToggleMeander: () -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val accent = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var showSleepDialog by remember { mutableStateOf(false) }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("选择定时时长") },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        TextButton(
                            onClick = {
                                onSleepTimer(minutes)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${minutes} 分钟")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PageHeader(
                title = "场景环境音",
                onNavigateBack = onNavigateBack
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp, vertical = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "chips") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = cat.id == selectedCategoryId,
                                onClick = { onSelectCategory(cat.id) },
                                label = { Text(cat.name, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.2f),
                                    selectedLabelColor = accent
                                )
                            )
                        }
                    }
                }

                items(tracks, key = { it.id }) { track ->
                    TrackCard(
                        track = track,
                        isActive = track.id == currentTrack?.id,
                        isPlaying = isPlaying && track.id == currentTrack?.id,
                        isFavorite = track.id in favoriteIds,
                        isRecent = track.id in recentIds,
                        isPreparing = isPreparing && track.id == currentTrack?.id,
                        accent = accent,
                        onPlay = { onTogglePlay(track) },
                        onFavorite = { onToggleFavorite(track.id) },
                        onClick = { onTrackClick(track) }
                    )
                }
            }

            if (currentTrack != null) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null,
                                tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = volume,
                                onValueChange = onVolumeChange,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                    inactiveTrackColor = surfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${(volume * 100).toInt()}%", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onToggleMeander() }
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null,
                                    tint = if (meanderEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (meanderEnabled) "律动中" else "律动",
                                    fontSize = 13.sp,
                                    color = if (meanderEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (sleepRemaining > 0) onCancelSleepTimer() else showSleepDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null,
                                    tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (sleepRemaining > 0) "${sleepRemaining / 60}:${(sleepRemaining % 60).toString().padStart(2, '0')}"
                                    else "睡眠定时",
                                    fontSize = 13.sp, color = accent
                                )
                            }
                            Button(
                                onClick = onStop,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("停止", fontSize = 12.sp)
                            }
                        }
                    }
                }
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
    isRecent: Boolean,
    isPreparing: Boolean,
    accent: Color,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        enableShadow = isPlaying,
        innerPadding = 0.dp,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(accent.copy(alpha = 0.08f))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(if (isPlaying) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(if (isPlaying) accent.copy(alpha = 0.15f) else surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(track.imageUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isPlaying) {
                            Box(modifier = Modifier.matchParentSize().background(accent.copy(alpha = 0.2f)))
                        }
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null,
                            tint = if (isPlaying) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (isPreparing) {
                            "加载中…"
                        } else {
                            buildAmbientTrackSupportingText(
                                baseSubtitle = track.subtitle,
                                trackId = track.id,
                                favoriteIds = if (isFavorite) setOf(track.id) else emptySet(),
                                recentIds = if (isRecent) listOf(track.id) else emptyList()
                            )
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) accent else accent.copy(alpha = 0.1f))
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenPlayer(
    track: AudioTrack,
    isPlaying: Boolean,
    volume: Float,
    sleepRemaining: Int,
    isFavorite: Boolean,
    backgroundImageUrl: String?,
    categoryId: String?,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val overlay = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)

    var showSleepDialog by remember { mutableStateOf(false) }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("选择定时时长") },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        TextButton(
                            onClick = {
                                onSleepTimer(minutes)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${minutes} 分钟")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(backgroundImageUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(
                when (categoryId) {
                    "water" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    "forest" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )                .then(Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))))
        }
        Box(modifier = Modifier.fillMaxSize().background(overlay))

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = textPrimary)
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (track.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(track.imageUrl).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.size(240.dp).clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(240.dp).clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null,
                        tint = textSecondary, modifier = Modifier.size(72.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                track.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            Text("音量", fontSize = 12.sp, color = textSecondary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                Slider(
                    value = volume, onValueChange = onVolumeChange, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = accent, activeTrackColor = accent,
                        inactiveTrackColor = textSecondary.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.clickable {
                if (sleepRemaining > 0) onCancelSleepTimer() else showSleepDialog = true
            }) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (sleepRemaining > 0) "${sleepRemaining / 60}:${(sleepRemaining % 60).toString().padStart(2, '0')}"
                    else "睡眠定时",
                    fontSize = 12.sp, color = accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onTogglePlay, shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null, tint = onAccent, modifier = Modifier.size(32.dp)
                    )
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("停止")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
