package com.diary.app.ui.ambientsound

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.data.ambientsound.AmbientSoundDatabase
import com.diary.app.data.ambientsound.AudioCacheManager
import com.diary.app.data.ambientsound.AudioRepository
import com.diary.app.data.ambientsound.AudioTrack
import com.diary.app.data.ambientsound.RecentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class AmbientSoundState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val errorMessage: String? = null,
    val recentIds: List<String> = emptyList()
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

    // ── 同步加载初始数据，避免异步导致排序跳变 ──
    private val initRecents: List<String>

    init {
        val recents = runBlocking(Dispatchers.IO) {
            dao.getRecentIds()
        }
        initRecents = recents
        _state.value = _state.value.copy(recentIds = recents)
    }

    private fun onPlaybackChanged() {
        viewModelScope.launch { syncStateWithPlayer() }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (player.hasSession) AmbientSoundService.start(ctx) else AmbientSoundService.stop(ctx)
        }
    }

    private suspend fun syncStateWithPlayer() {
        _state.value = _state.value.copy(
            currentTrack = player.currentTrack,
            isPlaying = player.isPlaying
        )
    }

    // ── 播放 ──
    fun play(track: AudioTrack) {
        // 先彻底重置状态，防止 stop 残留的异步回调干扰本次播放流程
        _state.value = AmbientSoundState(currentTrack = track, isPlaying = false, isPreparing = true, recentIds = _state.value.recentIds)
        viewModelScope.launch {
            val result = cacheManager.prepare(track.id, track.audioUrl)
            // 用本地变量锁定目标曲目，不受异步 syncStateWithPlayer 覆盖影响
            val targetId = track.id
            if (_state.value.currentTrack?.id != targetId) return@launch

            val file = result.getOrNull()
            if (file == null) {
                _state.value = _state.value.copy(currentTrack = null, isPreparing = false,
                    errorMessage = "无法加载音频")
                return@launch
            }

            val playResult = withContext(Dispatchers.IO) { player.play(ctx, track, file) }
            if (_state.value.currentTrack?.id != targetId) return@launch

            if (playResult.isFailure) {
                _state.value = _state.value.copy(currentTrack = null, isPreparing = false,
                    errorMessage = "无法播放此音频")
                return@launch
            }

            _state.value = _state.value.copy(isPreparing = false)
            prefs.edit().putString("last_track_id", track.id).apply()
            dao.addRecent(RecentEntity(track.id))
            _state.value = _state.value.copy(recentIds = dao.getRecentIds())
            syncStateWithPlayer()
        }
    }

    // ── 暂停/续播 ──
    fun togglePlay(track: AudioTrack) {
        val cur = _state.value.currentTrack
        if (cur?.id == track.id && player.hasSession) {
            // 同一曲目有活跃 session：暂停或续播
            if (player.isPlaying) {
                player.pause()
            } else {
                player.resume()
            }
            viewModelScope.launch { syncStateWithPlayer() }
        } else {
            play(track)
        }
    }

    // ── 上一首/下一首 ──
    fun playNext() {
        val all = AudioRepository.getAllTracks()
        val cur = _state.value.currentTrack ?: return
        val idx = all.indexOfFirst { it.id == cur.id }
        val next = if (idx >= 0 && idx < all.lastIndex) all[idx + 1] else all.first()
        play(next)
    }

    fun playPrev() {
        val all = AudioRepository.getAllTracks()
        val cur = _state.value.currentTrack ?: return
        val idx = all.indexOfFirst { it.id == cur.id }
        val prev = if (idx > 0) all[idx - 1] else all.last()
        play(prev)
    }

    fun stop() {
        stopProgressUpdates()
        // 先重置状态为"完全停止"，防止 player.stop() 内部回调覆盖
        _state.value = AmbientSoundState(recentIds = _state.value.recentIds)
        player.stop()
        prefs.edit().remove("last_track_id").apply()
        AmbientSoundService.stop(ctx)
    }

    private var progressUpdateJob: Job? = null

    fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (player.hasSession) {
                syncStateWithPlayer()
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
