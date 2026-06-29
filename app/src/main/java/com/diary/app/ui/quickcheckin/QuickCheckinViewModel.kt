package com.diary.app.ui.quickcheckin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.QuickCheckin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuickCheckinViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _selectedMood = MutableStateFlow<Int?>(null)
    val selectedMood: StateFlow<Int?> = _selectedMood

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    private val _photoUri = MutableStateFlow<String?>(null)
    val photoUri: StateFlow<String?> = _photoUri

    private val _checkins = MutableStateFlow<List<QuickCheckin>>(emptyList())
    val checkins: StateFlow<List<QuickCheckin>> = _checkins.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllQuickCheckins().collect { items ->
                _checkins.value = items
            }
        }
    }

    fun setMood(mood: Int) {
        _selectedMood.value = mood
    }

    fun setText(value: String) {
        _text.value = value
    }

    fun setPhotoUri(uri: String?) {
        _photoUri.value = uri
    }

    fun clearDraft() {
        _selectedMood.value = null
        _text.value = ""
        _photoUri.value = null
    }

    fun submit(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            if (!shouldEnableQuickCheckinSubmit(_selectedMood.value, _text.value, _photoUri.value)) {
                onComplete(false)
                return@launch
            }
            runCatching {
                dao.insertQuickCheckin(
                    QuickCheckin(
                        moodLevel = _selectedMood.value,
                        photoUri = _photoUri.value,
                        text = _text.value.trim(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                clearDraft()
                onComplete(true)
            }.onFailure {
                onComplete(false)
            }
        }
    }
}
