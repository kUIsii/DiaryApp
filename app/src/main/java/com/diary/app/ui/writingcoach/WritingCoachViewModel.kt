package com.diary.app.ui.writingcoach

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.WritingCoach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WritingCoachViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    
    private val _analysis = MutableStateFlow<WritingCoach.WritingAnalysis?>(null)
    val analysis: StateFlow<WritingCoach.WritingAnalysis?> = _analysis
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun analyze() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entries = dao.getAllEntriesOnce()
                _analysis.value = WritingCoach.analyzeWritingPatterns(entries)
            } catch (e: Exception) {
                _analysis.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}
