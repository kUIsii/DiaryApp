package com.diary.app.ui.ambientsound

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerOption(val minutes: Int, val label: String) {
    OFF(0, "关闭"), MIN_15(15, "15分"), MIN_30(30, "30分"), MIN_60(60, "60分"), MIN_90(90, "90分")
}

data class AmbientSoundState(
    val activeSounds: Set<AmbientSoundType> = emptySet(),
    val volumes: Map<AmbientSoundType, Float> = emptyMap(),
    val timerOption: TimerOption = TimerOption.OFF,
    val remainingSeconds: Int = 0,
    val isSleepFading: Boolean = false,
    val masterVolume: Float = 1f,
    val meanderEnabled: Boolean = false
)

class AmbientSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val player = AmbientSoundPlayer.getInstance().also {
        it.init(application)
        it.setOnPlayCallback { updateService() }
        it.setOnStopCallback { updateService() }
        it.setOnStateChangeCallback { syncStateFromPlayer() }
    }
    private val ctx = application
    private val prefs = application.getSharedPreferences("ambient_sound", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AmbientSoundState())
    val state: StateFlow<AmbientSoundState> = _state.asStateFlow()
    private var timerJob: Job? = null

    init {
        restoreState()
    }

    private fun restoreState() {
        val savedKeys = prefs.getStringSet("active_keys", emptySet()) ?: emptySet()
        if (savedKeys.isEmpty()) return
        val savedVolumes = mutableMapOf<AmbientSoundType, Float>()
        val savedMaster = prefs.getFloat("master_volume", 1f)
        val savedMeander = prefs.getBoolean("meander_enabled", false)
        for (key in savedKeys) {
            val type = AmbientSoundType.entries.find { it.key == key } ?: continue
            savedVolumes[type] = prefs.getFloat("vol_$key", 0.5f)
        }
        player.setMasterVolume(savedMaster)
        player.setMeanderEnabled(savedMeander)
        for ((type, vol) in savedVolumes) player.play(type, vol)
        _state.value = AmbientSoundState(
            activeSounds = savedVolumes.keys,
            volumes = savedVolumes,
            masterVolume = savedMaster,
            meanderEnabled = savedMeander
        )
        updateService()
    }

    private fun syncStateFromPlayer() {
        val cur = _state.value
        _state.value = cur.copy(
            activeSounds = player.getActiveTypes(),
            masterVolume = player.masterVolume,
            meanderEnabled = player.meanderEnabled
        )
    }

    fun toggle(type: AmbientSoundType) {
        val cur = _state.value
        if (type in cur.activeSounds) {
            player.stop(type)
            _state.value = cur.copy(activeSounds = cur.activeSounds - type, volumes = cur.volumes - type)
        } else {
            val vol = cur.volumes[type] ?: 0.5f
            player.play(type, vol)
            _state.value = cur.copy(activeSounds = cur.activeSounds + type, volumes = cur.volumes + (type to vol))
        }
        updateService()
    }

    fun setVolume(type: AmbientSoundType, volume: Float) {
        player.setVolume(type, volume)
        _state.value = _state.value.copy(volumes = _state.value.volumes + (type to volume))
    }

    fun setMasterVolume(v: Float) {
        player.setMasterVolume(v)
        _state.value = _state.value.copy(masterVolume = v)
    }

    fun toggleMeander() {
        player.toggleMeander()
        _state.value = _state.value.copy(meanderEnabled = !_state.value.meanderEnabled)
    }

    fun stopAll() {
        timerJob?.cancel()
        player.stopAll()
        _state.value = AmbientSoundState(masterVolume = player.masterVolume)
        AmbientSoundService.stop(ctx)
    }

    fun setTimer(option: TimerOption) {
        timerJob?.cancel()
        if (option == TimerOption.OFF) {
            _state.value = _state.value.copy(timerOption = TimerOption.OFF, remainingSeconds = 0, isSleepFading = false)
            return
        }
        val totalSec = option.minutes * 60
        _state.value = _state.value.copy(timerOption = option, remainingSeconds = totalSec, isSleepFading = false)
        timerJob = viewModelScope.launch {
            var remaining = totalSec
            while (remaining > 0) {
                delay(1000); remaining--
                val fading = remaining <= 120 && remaining > 0
                if (fading) {
                    val fraction = remaining.toFloat() / 120f
                    for (type in _state.value.activeSounds) player.setVolume(type, (_state.value.volumes[type] ?: 0.5f) * fraction)
                }
                _state.value = _state.value.copy(remainingSeconds = remaining, isSleepFading = fading)
            }
            stopAll()
        }
    }

    private fun updateService() {
        if (player.isAnyPlaying) AmbientSoundService.start(ctx) else AmbientSoundService.stop(ctx)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        val s = _state.value
        prefs.edit().apply {
            putStringSet("active_keys", s.activeSounds.map { it.key }.toSet())
            for ((type, vol) in s.volumes) putFloat("vol_${type.key}", vol)
            putFloat("master_volume", s.masterVolume)
            putBoolean("meander_enabled", s.meanderEnabled)
            apply()
        }
    }
}
