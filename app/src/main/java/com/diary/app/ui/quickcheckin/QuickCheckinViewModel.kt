package com.diary.app.ui.quickcheckin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.QuickCheckin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuickCheckinViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _selectedMood = MutableStateFlow<Int?>(null)
    val selectedMood: StateFlow<Int?> = _selectedMood

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    fun setMood(mood: Int) {
        _selectedMood.value = mood
    }

    fun setText(value: String) {
        _text.value = value
    }

    fun submit() {
        viewModelScope.launch {
            val checkin = QuickCheckin(
                moodLevel = _selectedMood.value,
                text = _text.value,
                createdAt = System.currentTimeMillis()
            )
            dao.insertQuickCheckin(checkin)
        }
    }
}
