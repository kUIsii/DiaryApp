package com.diary.app.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.FocusSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    init {
        loadSessions()
    }

    fun setDuration(minutes: Int) {
        _selectedDuration.value = minutes
        if (!_isRunning.value) {
            _remainingSeconds.value = minutes * 60
        }
    }

    fun setSound(sound: String?) {
        _selectedSound.value = sound
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
    }

    fun resumeSession() {
        if (_remainingSeconds.value <= 0) return
        _isRunning.value = true

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
}
