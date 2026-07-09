package com.diary.app.ui.focus

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.FocusSession
import com.diary.app.data.ambientsound.AudioCacheManager
import com.diary.app.data.ambientsound.AudioRepository
import com.diary.app.ui.ambientsound.AmbientSoundPlayer
import com.diary.app.ui.ambientsound.AmbientSoundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusModeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _remainingSeconds = MutableStateFlow(25 * 60) // 25 minutes default
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private val _selectedDuration = MutableStateFlow(25)
    val selectedDuration: StateFlow<Int> = _selectedDuration

    private val _selectedSound = MutableStateFlow<String?>(null)
    val selectedSound: StateFlow<String?> = _selectedSound

    private val _completedSessions = MutableStateFlow<List<FocusSession>>(emptyList())
    val completedSessions: StateFlow<List<FocusSession>> = _completedSessions

    private var timerJob: Job? = null
    private var currentSessionId: Long? = null

    // 复用场景环境音的同一播放器与曲目，避免两套互不连通的声音系统
    private val player = AmbientSoundPlayer.getInstance()
    private val cacheManager = AudioCacheManager(app)
    private var focusSoundActive = false

    init {
        loadSessions()
    }

    private fun soundTrackId(sound: String?): String? = when (sound) {
        "rain" -> "rain_thunder"
        "cafe" -> "sea_waves"
        "whitenoise" -> "stream_flow"
        else -> null
    }

    fun setDuration(minutes: Int) {
        _selectedDuration.value = minutes
        if (!_isRunning.value) {
            _remainingSeconds.value = minutes * 60
        }
    }

    fun setSound(sound: String?) {
        _selectedSound.value = sound
        if (_isRunning.value) applyFocusSound()
    }

    private fun applyFocusSound() {
        val trackId = soundTrackId(_selectedSound.value)
        if (trackId == null) {
            stopFocusSound()
            return
        }
        val track = AudioRepository.getTrack(trackId) ?: return
        viewModelScope.launch {
            val prep = withContext(Dispatchers.IO) { cacheManager.prepare(track.id, null) }
            if (!prep.isSuccess || _selectedSound.value != trackId || !_isRunning.value) return@launch
            val file = prep.getOrNull() ?: return@launch
            withContext(Dispatchers.IO) { player.play(app, track, file) }
            focusSoundActive = true
            AmbientSoundService.start(app)
        }
    }

    private fun stopFocusSound() {
        if (focusSoundActive) {
            player.stop()
            AmbientSoundService.stop(app)
            focusSoundActive = false
        }
    }

    fun startSession() {
        if (_isRunning.value) return

        val session = FocusSession(
            startTime = System.currentTimeMillis(),
            durationMinutes = _selectedDuration.value,
            ambientSound = _selectedSound.value
        )

        viewModelScope.launch {
            val id = dao.insertFocusSession(session)
            currentSessionId = id
        }

        _remainingSeconds.value = _selectedDuration.value * 60
        _isRunning.value = true

        applyFocusSound()

        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value--
            }
            completeSession()
        }
    }

    fun pauseSession() {
        timerJob?.cancel()
        _isRunning.value = false
        if (focusSoundActive) player.pause()
    }

    fun resumeSession() {
        if (_remainingSeconds.value <= 0) return
        _isRunning.value = true
        if (focusSoundActive) player.resume()

        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value--
            }
            completeSession()
        }
    }

    fun stopSession() {
        timerJob?.cancel()
        _isRunning.value = false
        stopFocusSound()
        _remainingSeconds.value = _selectedDuration.value * 60
        currentSessionId?.let { id ->
            viewModelScope.launch {
                dao.completeFocusSession(
                    id = id,
                    endTime = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis()
                )
            }
        }
        currentSessionId = null
    }

    private fun completeSession() {
        _isRunning.value = false
        stopFocusSound()
        currentSessionId?.let { id ->
            viewModelScope.launch {
                dao.completeFocusSession(
                    id = id,
                    endTime = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis()
                )
                loadSessions()
            }
        }
        _remainingSeconds.value = _selectedDuration.value * 60
        currentSessionId = null
    }

    private fun loadSessions() {
        viewModelScope.launch {
            dao.getAllFocusSessions().collect { sessions ->
                _completedSessions.value = sessions
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopFocusSound()
    }
}
