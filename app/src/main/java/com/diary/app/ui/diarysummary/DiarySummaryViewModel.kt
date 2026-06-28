package com.diary.app.ui.diarysummary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.DiarySummarizer
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiarySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiarySummaryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val context = application.applicationContext
    
    private val _entriesWithoutSummary = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entriesWithoutSummary: StateFlow<List<DiaryEntry>> = _entriesWithoutSummary
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress
    
    private val _totalToProcess = MutableStateFlow(0)
    val totalToProcess: StateFlow<Int> = _totalToProcess
    
    init {
        loadEntriesWithoutSummary()
    }
    
    fun loadEntriesWithoutSummary() {
        viewModelScope.launch {
            val allEntries = dao.getAllEntriesOnce()
            val entriesWithoutSummary = allEntries.filter { entry ->
                val summary = dao.getSummaryForDiary(entry.id)
                summary == null && entry.plainText.length > 100
            }
            _entriesWithoutSummary.value = entriesWithoutSummary.take(50) // 限制数量
        }
    }
    
    fun generateSummaries() {
        viewModelScope.launch {
            _isGenerating.value = true
            _progress.value = 0
            _totalToProcess.value = _entriesWithoutSummary.value.size
            
            val entries = _entriesWithoutSummary.value
            
            entries.forEachIndexed { index, entry ->
                val summary = DiarySummarizer.generateSummary(context, entry)
                if (summary != null) {
                    val diarySummary = DiarySummary(
                        diaryId = entry.id,
                        summary = summary,
                        createdAt = System.currentTimeMillis()
                    )
                    dao.insertDiarySummary(diarySummary)
                }
                _progress.value = index + 1
            }
            
            _isGenerating.value = false
            loadEntriesWithoutSummary()
        }
    }
    
    fun generateSummaryForEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            val summary = DiarySummarizer.generateSummary(context, entry)
            if (summary != null) {
                val diarySummary = DiarySummary(
                    diaryId = entry.id,
                    summary = summary,
                    createdAt = System.currentTimeMillis()
                )
                dao.insertDiarySummary(diarySummary)
                loadEntriesWithoutSummary()
            }
        }
    }
}
