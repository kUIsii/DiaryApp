package com.diary.app.ui.gentlenotification

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GentleNotificationState(
    val selectedSound: String = "water_drop",
    val volume: Float = 0.7f,
    val isEnabled: Boolean = true
)

class GentleNotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("gentle_notification", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<GentleNotificationState> = _state.asStateFlow()

    init {
        _state.value = loadState()
    }

    private fun loadState(): GentleNotificationState {
        return GentleNotificationState(
            selectedSound = prefs.getString("sound", "water_drop") ?: "water_drop",
            volume = prefs.getFloat("volume", 0.7f),
            isEnabled = prefs.getBoolean("enabled", true)
        )
    }

    fun setSound(sound: String) {
        prefs.edit().putString("sound", sound).apply()
        _state.value = _state.value.copy(selectedSound = sound)
    }

    fun setVolume(volume: Float) {
        prefs.edit().putFloat("volume", volume).apply()
        _state.value = _state.value.copy(volume = volume)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
        _state.value = _state.value.copy(isEnabled = enabled)
    }
}
