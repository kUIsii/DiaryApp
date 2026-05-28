package com.diary.app.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _currentEntry = MutableStateFlow<DiaryEntry?>(null)
    val currentEntry = _currentEntry.asStateFlow()

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _currentEntry.value = dao.getEntryById(id)
        }
    }

    fun saveEntry(title: String, content: String, plainText: String, diaryId: Long?): Long {
        return kotlinx.coroutines.runBlocking {
            if (diaryId != null) {
                val existing = dao.getEntryById(diaryId)
                if (existing != null) {
                    val updated = existing.copy(
                        title = title,
                        content = content,
                        plainText = plainText,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.updateEntry(updated)
                    diaryId
                } else {
                    dao.insertEntry(
                        DiaryEntry(
                            title = title,
                            content = content,
                            plainText = plainText
                        )
                    )
                }
            } else {
                dao.insertEntry(
                    DiaryEntry(
                        title = title,
                        content = content,
                        plainText = plainText
                    )
                )
            }
        }
    }
}
