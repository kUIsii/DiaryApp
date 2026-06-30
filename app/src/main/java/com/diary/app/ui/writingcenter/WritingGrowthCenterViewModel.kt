package com.diary.app.ui.writingcenter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WritingGrowthCenterUiState(
    val isLoading: Boolean = true,
    val isAiAvailable: Boolean = false,
    val latestEntryId: Long? = null,
    val latestEntryTitle: String? = null,
    val todayWordCount: Int = 0,
    val writingDaysThisWeek: Int = 0,
    val recentSedimentedContent: List<String> = emptyList(),
    val content: WritingGrowthCenterContent = buildWritingGrowthCenterContent(
        latestEntryTitle = null,
        hasAiSupport = false,
        todayWordCount = 0,
        writingDaysThisWeek = 0,
        recentSedimentedContent = emptyList()
    )
)

class WritingGrowthCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()

    private val _uiState = MutableStateFlow(WritingGrowthCenterUiState())
    val uiState: StateFlow<WritingGrowthCenterUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val previews = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
            val todayWordCount = withContext(Dispatchers.IO) {
                val entries = dao.getAllEntriesOnce()
                entries.filter { isToday(it.createdAt) }.sumOf { it.plainText.length }
            }
            val thisWeek = previews.count { it.createdAt >= System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
            val sediment = buildSedimentContent(previews)
            val content = buildWritingGrowthCenterContent(
                latestEntryTitle = previews.firstOrNull()?.title,
                hasAiSupport = app.aiService.isAiEnabled(),
                todayWordCount = todayWordCount,
                writingDaysThisWeek = thisWeek,
                recentSedimentedContent = sediment
            )
            _uiState.value = WritingGrowthCenterUiState(
                isLoading = false,
                isAiAvailable = app.aiService.isAiEnabled(),
                latestEntryId = previews.firstOrNull()?.id,
                latestEntryTitle = previews.firstOrNull()?.title,
                todayWordCount = todayWordCount,
                writingDaysThisWeek = thisWeek,
                recentSedimentedContent = sediment,
                content = content
            )
        }
    }

    private fun buildSedimentContent(previews: List<DiaryPreview>): List<String> {
        return previews.take(4).map { preview ->
            val snippet = preview.plainText.trim().replace("\n", " ").take(80)
            if (snippet.isBlank()) preview.title else "${preview.title} · $snippet"
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val dayStart = now - (now % (24 * 60 * 60 * 1000))
        return timestamp >= dayStart
    }
}
