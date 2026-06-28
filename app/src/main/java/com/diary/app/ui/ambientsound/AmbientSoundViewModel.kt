package com.diary.app.ui.ambientsound

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AmbientSoundState(
    val isPreparing: Boolean = true,
    val currentType: AmbientSoundType? = null,
    val isPlaying: Boolean = false,
    val volume: Float = 0.5f
)

class AmbientSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val player = AmbientSoundPlayer(application)

    private val _state = MutableStateFlow(AmbientSoundState())
    val state: StateFlow<AmbientSoundState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AmbientSoundType.entries.forEach { type ->
                    player.getOrCreateAudioFile(type)
                }
            }
            _state.value = _state.value.copy(isPreparing = false)
        }
    }

    fun toggle(type: AmbientSoundType) {
        val current = _state.value
        if (current.currentType == type && current.isPlaying) {
            player.stop()
            _state.value = current.copy(currentType = null, isPlaying = false)
        } else {
            player.play(type, current.volume)
            _state.value = current.copy(currentType = type, isPlaying = true)
        }
    }

    fun setVolume(volume: Float) {
        player.setVolume(volume)
        _state.value = _state.value.copy(volume = volume)
    }

    override fun onCleared() {
        super.onCleared()
        player.stop()
    }
}
