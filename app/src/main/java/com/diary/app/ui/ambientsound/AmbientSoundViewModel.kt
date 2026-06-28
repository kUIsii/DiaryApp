package com.diary.app.ui.ambientsound

import android.app.Application
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
    val presets: List<SoundPreset> = emptyList()
)

class AmbientSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val player = AmbientSoundPlayer.getInstance().also { it.init(application) }
    private val ctx = application

    private val _state = MutableStateFlow(AmbientSoundState(presets = PresetStorage.load(application)))
    val state: StateFlow<AmbientSoundState> = _state.asStateFlow()
    private var timerJob: Job? = null

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

    fun stopAll() {
        timerJob?.cancel()
        player.stopAll()
        _state.value = _state.value.copy(activeSounds = emptySet(), volumes = emptyMap(), remainingSeconds = 0, isSleepFading = false)
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

    fun applyPreset(preset: SoundPreset) {
        stopAll()
        for (type in preset.toActiveTypesSet()) {
            player.play(type, preset.toVolumesMap()[type] ?: 0.5f)
        }
        _state.value = _state.value.copy(
            activeSounds = preset.toActiveTypesSet(), volumes = preset.toVolumesMap(),
            presets = PresetStorage.load(ctx)
        )
        updateService()
    }

    fun saveCurrentPreset(name: String) {
        val s = _state.value
        if (s.activeSounds.isEmpty()) return
        PresetStorage.save(ctx, SoundPreset(
            name = name,
            activeTypes = s.activeSounds.map { it.key }.sorted(),
            volumes = s.activeSounds.associate { it.key to (s.volumes[it]?.toDouble() ?: 0.5) }
        ))
        _state.value = s.copy(presets = PresetStorage.load(ctx))
    }

    fun deletePreset(name: String) {
        PresetStorage.delete(ctx, name)
        _state.value = _state.value.copy(presets = PresetStorage.load(ctx))
    }

    private fun updateService() {
        if (player.isAnyPlaying) AmbientSoundService.start(ctx) else AmbientSoundService.stop(ctx)
    }

    override fun onCleared() { super.onCleared(); timerJob?.cancel() }
}
