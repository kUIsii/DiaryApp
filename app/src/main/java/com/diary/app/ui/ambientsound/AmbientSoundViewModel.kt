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
    val activeType: AmbientSoundType? = null,
    val volume: Float = 0.5f,
    val timerOption: TimerOption = TimerOption.OFF,
    val remainingSeconds: Int = 0,
    val isSleepFading: Boolean = false
)

class AmbientSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val player = AmbientSoundPlayer.getInstance().also {
        it.init(application)
        it.setOnPlayCallback { updateService() }
        it.setOnStopCallback { updateService() }
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
        val savedKey = prefs.getString("active_key", null) ?: return
        val type = AmbientSoundType.entries.find { it.key == savedKey } ?: return
        val savedVol = prefs.getFloat("volume", 0.5f)
        player.play(type, savedVol)
        _state.value = AmbientSoundState(activeType = type, volume = savedVol)
        updateService()
    }

    fun toggle(type: AmbientSoundType) {
        val cur = _state.value
        if (cur.activeType == type) {
            player.stop(type)
            _state.value = AmbientSoundState()
            updateService()
        } else {
            cur.activeType?.let { player.stop(it) }
            val vol = if (cur.activeType == type) cur.volume else 0.5f
            player.play(type, vol)
            _state.value = AmbientSoundState(activeType = type, volume = vol)
            updateService()
        }
    }

    fun setVolume(volume: Float) {
        val type = _state.value.activeType ?: return
        player.setVolume(type, volume)
        _state.value = _state.value.copy(volume = volume)
    }

    fun stop() {
        timerJob?.cancel()
        player.stopAll()
        _state.value = AmbientSoundState()
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
                    val t = _state.value.activeType ?: return@launch
                    val fraction = remaining.toFloat() / 120f
                    player.setVolume(t, _state.value.volume * fraction)
                }
                _state.value = _state.value.copy(remainingSeconds = remaining, isSleepFading = fading)
            }
            stop()
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
            if (s.activeType != null) {
                putString("active_key", s.activeType.key)
                putFloat("volume", s.volume)
            } else {
                remove("active_key")
                remove("volume")
            }
            apply()
        }
    }
}
