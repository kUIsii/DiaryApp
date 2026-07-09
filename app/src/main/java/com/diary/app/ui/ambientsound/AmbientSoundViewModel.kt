package com.diary.app.ui.ambientsound

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.data.ambientsound.AmbientSoundDatabase
import com.diary.app.data.ambientsound.AudioCacheManager
import com.diary.app.data.ambientsound.AudioRepository
import com.diary.app.data.ambientsound.AudioTrack
import com.diary.app.data.ambientsound.FavoriteEntity
import com.diary.app.data.ambientsound.RecentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AmbientSoundState(
    val selectedCategoryId: String = "water",
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val volume: Float = 0.5f,
    val progress: Int = 0,
    val duration: Int = 0,
    val sleepRemainingSeconds: Int = 0,
    val favoriteIds: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val isPreparing: Boolean = false,
    val errorMessage: String? = null,
    val meanderEnabled: Boolean = false,
    val isFullscreenPlayerVisible: Boolean = false
)

class AmbientSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val player = AmbientSoundPlayer.getInstance().also {
        it.setOnPlayCallback { onPlaybackChanged() }
        it.setOnStopCallback { onPlaybackChanged() }
    }
    private val cacheManager = AudioCacheManager(application)
    private val dao = AmbientSoundDatabase.getInstance(application).dao()
    private val ctx = application
    private val prefs = application.getSharedPreferences("ambient_sound", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AmbientSoundState())
    val state: StateFlow<AmbientSoundState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val favIds = dao.getFavoriteIds().toSet()
            val recentIds = dao.getRecentIds()
            val savedVolume = prefs.getFloat("volume", 0.5f)
            val savedCategory = prefs.getString("category", "water") ?: "water"
            val savedMeander = prefs.getBoolean("meander_enabled", false)

            player.setVolume(savedVolume)
            _state.value = _state.value.copy(
                favoriteIds = favIds,
                recentIds = recentIds,
                volume = savedVolume,
                selectedCategoryId = savedCategory,
                meanderEnabled = savedMeander
            )

            if (savedMeander) {
                player.setMeanderEnabled(true)
            }

            val savedTrackId = prefs.getString("last_track_id", null)
            if (player.hasSession) {
                syncStateWithPlayer()
            } else {
                // 仅恢复上次曲目的元数据，保持暂停态，避免在应用启动时自动播放
                val track = savedTrackId?.let { AudioRepository.getTrack(it) }
                if (track != null) {
                    _state.value = _state.value.copy(
                        selectedCategoryId = track.categoryId,
                        currentTrack = track,
                        isPlaying = false
                    )
                }
            }
        }
    }

    private fun onPlaybackChanged() {
        syncStateWithPlayer()
        // 前台 Service 的 start/stop 必须在主线程执行
        val app = ctx
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (player.hasSession) AmbientSoundService.start(app) else AmbientSoundService.stop(app)
        }
    }

    private fun syncStateWithPlayer(
        recentIds: List<String> = _state.value.recentIds,
        selectedCategoryId: String = _state.value.selectedCategoryId
    ) {
        val snapshot = AmbientPlayerSnapshot(
            currentTrack = player.currentTrack,
            isPlaying = player.isPlaying,
            volume = player.currentVolume,
            duration = player.duration,
            progress = player.currentPosition,
            sleepRemainingSeconds = player.sleepRemainingSeconds(),
            meanderEnabled = player.isMeanderEnabled,
            hasSession = player.hasSession
        )
        _state.value = syncAmbientStateWithPlayer(
            state = _state.value.copy(
                recentIds = recentIds,
                selectedCategoryId = selectedCategoryId
            ),
            snapshot = snapshot
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun selectCategory(categoryId: String) {
        prefs.edit().putString("category", categoryId).apply()
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    fun play(track: AudioTrack) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                currentTrack = track,
                isPreparing = true,
                errorMessage = null
            )
            val result = cacheManager.prepare(track.id, track.audioUrl)

            if (_state.value.currentTrack?.id != track.id) return@launch
            _state.value = _state.value.copy(isPreparing = false)

            val file = result.getOrNull()
            if (file == null) {
                val e = result.exceptionOrNull()
                if (_state.value.currentTrack?.id != track.id) return@launch
                val msg = when {
                    e?.message?.contains("not found", true) == true -> "音频文件缺失"
                    else -> "无法加载音频"
                }
                _state.value = _state.value.copy(currentTrack = null, errorMessage = msg)
                return@launch
            }

            if (_state.value.currentTrack?.id != track.id) return@launch
            val playResult = withContext(Dispatchers.IO) { player.play(ctx, track, file) }
            if (_state.value.currentTrack?.id != track.id) return@launch

            val playError = playResult.exceptionOrNull()
            if (playError != null) {
                _state.value = _state.value.copy(
                    currentTrack = null,
                    errorMessage = "无法播放此音频"
                )
                return@launch
            }

            prefs.edit().putString("last_track_id", track.id).apply()
            dao.addRecent(RecentEntity(track.id))
            val recentIds = dao.getRecentIds()
            syncStateWithPlayer(
                recentIds = recentIds,
                selectedCategoryId = track.categoryId
            )
        }
    }

    fun togglePlay(track: AudioTrack) {
        val cur = _state.value.currentTrack
        if (cur == track && player.hasSession) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.resume()
            }
            syncStateWithPlayer()
        } else {
            play(track)
        }
    }

    fun toggleMeander() {
        val newEnabled = !_state.value.meanderEnabled
        player.setMeanderEnabled(newEnabled)
        prefs.edit().putBoolean("meander_enabled", newEnabled).apply()
        _state.value = _state.value.copy(meanderEnabled = newEnabled)
    }

    fun stop() {
        stopProgressUpdates()
        player.stop()
        prefs.edit().remove("last_track_id").apply()
        syncStateWithPlayer()
        _state.value = _state.value.copy(
            sleepRemainingSeconds = 0,
            isFullscreenPlayerVisible = false
        )
        AmbientSoundService.stop(ctx)
    }

    fun setVolume(volume: Float) {
        player.setVolume(volume)
        prefs.edit().putFloat("volume", volume).apply()
        _state.value = _state.value.copy(volume = volume)
    }

    fun seekTo(position: Int) {
        player.seekTo(position)
        syncStateWithPlayer()
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            val favs = _state.value.favoriteIds
            if (trackId in favs) {
                dao.removeFavorite(trackId)
                _state.value = _state.value.copy(favoriteIds = favs - trackId)
            } else {
                dao.addFavorite(FavoriteEntity(trackId))
                _state.value = _state.value.copy(favoriteIds = favs + trackId)
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            player.cancelSleepTimer()
            _state.value = _state.value.copy(sleepRemainingSeconds = 0)
            return
        }
        player.startSleepTimer(minutes)
        _state.value = _state.value.copy(sleepRemainingSeconds = player.sleepRemainingSeconds())
    }

    fun cancelSleepTimer() {
        player.cancelSleepTimer()
        _state.value = _state.value.copy(sleepRemainingSeconds = 0)
    }

    fun showFullscreenPlayer() {
        if (_state.value.currentTrack != null) {
            _state.value = _state.value.copy(isFullscreenPlayerVisible = true)
        }
    }

    fun hideFullscreenPlayer() {
        _state.value = _state.value.copy(isFullscreenPlayerVisible = false)
    }

    var progressUpdateJob: Job? = null

    fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (player.hasSession) {
                syncStateWithPlayer()
                if (player.isSleepExpired()) {
                    stop()
                    return@launch
                }
                delay(500)
            }
        }
    }

    fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdates()
    }
}
