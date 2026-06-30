package com.diary.app.ui.immersive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class FontType(val displayName: String) {
    DEFAULT("默认"),
    SERIF("衬线"),
    MONOSPACE("等宽")
}

class ImmersiveReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val sessionStore = app.readingSessionStore

    private val _entries = MutableStateFlow<List<DiaryPreview>>(emptyList())
    val entries: StateFlow<List<DiaryPreview>> = _entries

    private val _warmLightEnabled = MutableStateFlow(false)
    val warmLightEnabled: StateFlow<Boolean> = _warmLightEnabled

    private val _darkModeEnabled = MutableStateFlow(false)
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled

    private val _fontSize = MutableStateFlow(18)
    val fontSize: StateFlow<Int> = _fontSize

    private val _fontType = MutableStateFlow(FontType.DEFAULT)
    val fontType: StateFlow<FontType> = _fontType

    private val _sessionStartTime = MutableStateFlow(System.currentTimeMillis())
    val sessionStartTime: StateFlow<Long> = _sessionStartTime

    private val _sessionReadCount = MutableStateFlow(0)
    val sessionReadCount: StateFlow<Int> = _sessionReadCount

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private var timerJob: Job? = null

    init {
        loadEntries()
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                _elapsedSeconds.value = (System.currentTimeMillis() - _sessionStartTime.value) / 1000
                delay(1000)
            }
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            dao.getAllPreviews().collect { list ->
                _entries.value = list
            }
        }
    }

    fun updateReadingSelection(preview: DiaryPreview, pageIndex: Int, totalPages: Int) {
        sessionStore.setEntry(preview, requestedPage = pageIndex)
        sessionStore.updatePage(pageIndex, totalPages)
    }

    fun toggleWarmLight() {
        _warmLightEnabled.value = !_warmLightEnabled.value
    }

    fun toggleDarkMode() {
        _darkModeEnabled.value = !_darkModeEnabled.value
    }

    fun cycleFontType() {
        _fontType.value = when (_fontType.value) {
            FontType.DEFAULT -> FontType.SERIF
            FontType.SERIF -> FontType.MONOSPACE
            FontType.MONOSPACE -> FontType.DEFAULT
        }
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size.coerceIn(14, 28)
    }

    fun trackPageRead() {
        _sessionReadCount.value = _sessionReadCount.value + 1
    }

    fun resetSession() {
        _sessionStartTime.value = System.currentTimeMillis()
        _sessionReadCount.value = 0
        _elapsedSeconds.value = 0L
        startTimer()
    }
}
