package com.diary.app.ui.readingcenter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReadingCenterUiState(
    val isLoading: Boolean = true,
    val session: ReadingSessionSnapshot = ReadingSessionSnapshot(),
    val recentEntries: List<DiaryPreview> = emptyList(),
    val completedFocusSessions: Int = 0,
    val content: ReadingCenterContent = buildReadingCenterContent(
        session = ReadingSessionSnapshot(),
        recentEntries = emptyList(),
        completedFocusSessions = 0
    )
)

class ReadingCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val sessionStore = app.readingSessionStore

    private val _uiState = MutableStateFlow(ReadingCenterUiState())
    val uiState: StateFlow<ReadingCenterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.session.collect { session ->
                val current = _uiState.value
                _uiState.value = current.copy(
                    session = session,
                    content = buildReadingCenterContent(
                        session = session,
                        recentEntries = current.recentEntries.map { it.title.ifBlank { "未命名内容" } },
                        completedFocusSessions = current.completedFocusSessions
                    )
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val recentEntries = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce().take(6) }
            val focusSessions = withContext(Dispatchers.IO) { dao.getAllFocusSessions().first() }
            val session = sessionStore.session.value

            if (session.diaryId == null) {
                recentEntries.firstOrNull()?.let { sessionStore.setEntry(it) }
            }

            _uiState.value = ReadingCenterUiState(
                isLoading = false,
                session = sessionStore.session.value,
                recentEntries = recentEntries,
                completedFocusSessions = focusSessions.count { it.completedAt != null },
                content = buildReadingCenterContent(
                    session = sessionStore.session.value,
                    recentEntries = recentEntries.map { it.title.ifBlank { "未命名内容" } },
                    completedFocusSessions = focusSessions.count { it.completedAt != null }
                )
            )
        }
    }

    fun prepareReadingEntry(preview: DiaryPreview) {
        sessionStore.setEntry(preview)
    }
}
