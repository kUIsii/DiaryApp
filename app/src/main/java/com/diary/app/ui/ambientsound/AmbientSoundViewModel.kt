package com.diary.app.ui.ambientsound

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.data.ambientsound.AmbientSoundDatabase
import com.diary.app.data.ambientsound.AudioCacheManager
import com.diary.app.data.ambientsound.AudioRepository
import com.diary.app.data.ambientsound.AudioTrack
import com.diary.app.data.ambientsound.FavoriteEntity
import com.diary.app.data.ambientsound.RecentEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AmbientSoundState(
    val selectedCategoryId: String = "sleep",
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
    val meanderEnabled: Boolean = false
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
            val savedCategory = prefs.getString("category", "sleep") ?: "sleep"
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
            if (savedTrackId != null) {
                val track = AudioRepository.getTrack(savedTrackId)
                if (track != null) {
                    _state.value = _state.value.copy(
                        selectedCategoryId = track.categoryId,
                        isPreparing = true
                    )
                    val result = cacheManager.prepare(track.id, track.audioUrl)
                    _state.value = _state.value.copy(isPreparing = false)
                    result.onSuccess { file ->
                        player.play(ctx, track, file)
                        dao.addRecent(RecentEntity(track.id))
                        val recent = dao.getRecentIds()
                        _state.value = _state.value.copy(
                            currentTrack = track,
                            isPlaying = true,
                            recentIds = recent,
                            progress = 0,
                            duration = player.duration
                        )
                    }
                }
            }
        }
    }

    private fun onPlaybackChanged() {
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            currentTrack = player.currentTrack
        )
        if (player.hasSession) AmbientSoundService.start(ctx) else AmbientSoundService.stop(ctx)
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
            _state.value = _state.value.copy(isPreparing = true, errorMessage = null)
            val result = cacheManager.prepare(track.id, track.audioUrl)
            _state.value = _state.value.copy(isPreparing = false)

            result.onSuccess { file ->
                player.play(ctx, track, file)
                prefs.edit().putString("last_track_id", track.id).apply()
                dao.addRecent(RecentEntity(track.id))
                val recentIds = dao.getRecentIds()
                _state.value = _state.value.copy(
                    currentTrack = track,
                    isPlaying = true,
                    recentIds = recentIds,
                    progress = 0,
                    duration = player.duration
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    errorMessage = "无法加载音频，请重试"
                )
            }
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
            _state.value = _state.value.copy(isPlaying = player.isPlaying)
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
        player.stop()
        prefs.edit().remove("last_track_id").apply()
        _state.value = _state.value.copy(
            currentTrack = null,
            isPlaying = false,
            sleepRemainingSeconds = 0
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
        sleepTimerJob?.cancel()
        player.startSleepTimer(minutes)
        sleepTimerJob = viewModelScope.launch {
            while (player.sleepRemainingSeconds() > 0) {
                _state.value = _state.value.copy(sleepRemainingSeconds = player.sleepRemainingSeconds())
                delay(1000)
            }
            if (player.isSleepExpired()) {
                stop()
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        player.cancelSleepTimer()
        _state.value = _state.value.copy(sleepRemainingSeconds = 0)
    }

    var progressUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null

    fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    _state.value = _state.value.copy(
                        progress = player.currentPosition,
                        duration = player.duration
                    )
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
