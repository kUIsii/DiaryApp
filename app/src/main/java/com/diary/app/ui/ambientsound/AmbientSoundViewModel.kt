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
    OFF(0, "关闭"),
    MIN_15(15, "15分钟"),
    MIN_30(30, "30分钟"),
    MIN_60(60, "60分钟"),
    MIN_90(90, "90分钟")
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
        val current = _state.value
        val newActive: Set<AmbientSoundType>
        val newVolumes: Map<AmbientSoundType, Float>
        if (type in current.activeSounds) {
            player.stop(type)
            newActive = current.activeSounds - type
            newVolumes = current.volumes - type
        } else {
            val vol = current.volumes[type] ?: 0.5f
            player.play(type, vol)
            newActive = current.activeSounds + type
            newVolumes = current.volumes + (type to vol)
        }
        _state.value = current.copy(activeSounds = newActive, volumes = newVolumes)
        updateService()
    }

    fun setVolume(type: AmbientSoundType, volume: Float) {
        player.setVolume(type, volume)
        _state.value = _state.value.copy(volumes = _state.value.volumes + (type to volume))
    }

    fun stopAll() {
        player.stopAll()
        timerJob?.cancel()
        _state.value = _state.value.copy(
            activeSounds = emptySet(), volumes = emptyMap(),
            remainingSeconds = 0, isSleepFading = false
        )
        AmbientSoundService.stop(ctx)
    }

    fun setTimer(option: TimerOption) {
        timerJob?.cancel()
        val s = _state.value
        if (option == TimerOption.OFF) {
            _state.value = s.copy(timerOption = TimerOption.OFF, remainingSeconds = 0, isSleepFading = false)
            return
        }
        val totalSec = option.minutes * 60
        _state.value = s.copy(timerOption = option, remainingSeconds = totalSec, isSleepFading = false)

        timerJob = viewModelScope.launch {
            var remaining = totalSec
            val fadeStart = 120
            val initialVolumes = s.volumes.toMap()
            while (remaining > 0) {
                delay(1000)
                remaining--
                val fading = remaining <= fadeStart && remaining > 0
                if (fading) {
                    val fraction = remaining.toFloat() / fadeStart
                    for ((type, vol) in initialVolumes) {
                        val faded = vol * fraction
                        player.setVolume(type, faded)
                    }
                }
                _state.value = _state.value.copy(
                    remainingSeconds = remaining,
                    isSleepFading = fading
                )
            }
            stopAll()
        }
    }

    fun applyPreset(preset: SoundPreset) {
        stopAll()
        val types = preset.toActiveTypesSet()
        val volumes = preset.toVolumesMap()
        for (type in types) {
            val vol = volumes[type] ?: 0.5f
            player.play(type, vol)
        }
        _state.value = _state.value.copy(
            activeSounds = types, volumes = volumes, presets = PresetStorage.load(ctx)
        )
        updateService()
    }

    fun saveCurrentPreset(name: String) {
        val s = _state.value
        if (s.activeSounds.isEmpty()) return
        val preset = SoundPreset(
            name = name,
            activeTypes = s.activeSounds.map { it.key }.sorted(),
            volumes = s.activeSounds.associate { it.key to (s.volumes[it]?.toDouble() ?: 0.5) }
        )
        PresetStorage.save(ctx, preset)
        _state.value = s.copy(presets = PresetStorage.load(ctx))
    }

    fun deletePreset(name: String) {
        PresetStorage.delete(ctx, name)
        _state.value = _state.value.copy(presets = PresetStorage.load(ctx))
    }

    private fun updateService() {
        if (player.isAnyPlaying) {
            AmbientSoundService.start(ctx)
        } else {
            AmbientSoundService.stop(ctx)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
