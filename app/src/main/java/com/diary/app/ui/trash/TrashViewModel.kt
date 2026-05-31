package com.diary.app.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.TrashEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val entries: StateFlow<List<TrashEntry>> = dao.getTrashEntries()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreEntry(trashEntry: TrashEntry) {
        viewModelScope.launch {
            // Create a new diary entry from the trash entry
            val restoredEntry = DiaryEntry(
                id = 0, // Auto-generate new ID
                title = trashEntry.title,
                content = trashEntry.content,
                plainText = trashEntry.plainText,
                moodLevel = trashEntry.moodLevel,
                weather = trashEntry.weather,
                location = trashEntry.location,
                latitude = trashEntry.latitude,
                longitude = trashEntry.longitude,
                isFavorite = trashEntry.isFavorite,
                createdAt = trashEntry.createdAt,
                updatedAt = trashEntry.updatedAt
            )
            dao.insertEntry(restoredEntry)
            dao.deleteTrashEntryById(trashEntry.id)
        }
    }

    fun deleteEntryForever(trashEntry: TrashEntry) {
        viewModelScope.launch {
            dao.deleteTrashEntryById(trashEntry.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            // Delete all trash entries
            entries.value.forEach { entry ->
                dao.deleteTrashEntryById(entry.id)
            }
        }
    }
}
