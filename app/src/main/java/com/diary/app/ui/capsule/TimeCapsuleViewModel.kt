package com.diary.app.ui.capsule

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.CapsuleTheme
import com.diary.app.data.DiaryMediaManager
import com.diary.app.data.TimeCapsule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimeCapsuleViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()

    val capsules: StateFlow<List<TimeCapsule>> = dao.getAllCapsules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCapsule(
        title: String,
        content: String,
        unlockDate: Long,
        theme: CapsuleTheme = CapsuleTheme.NORMAL,
        imageUri: String? = null,
        unlockHour: Int = 0,
        unlockMinute: Int = 0
    ) {
        viewModelScope.launch {
            dao.insertCapsule(
                TimeCapsule(
                    title = title,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    unlockDate = unlockDate,
                    theme = theme,
                    imageUri = imageUri,
                    unlockHour = unlockHour,
                    unlockMinute = unlockMinute
                )
            )
        }
    }

    fun deleteCapsule(capsule: TimeCapsule) {
        viewModelScope.launch {
            dao.deleteCapsule(capsule)
        }
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            dao.markCapsuleRead(id)
        }
    }

    fun markOpened(id: Long) {
        viewModelScope.launch {
            dao.markCapsuleOpened(id)
        }
    }

    suspend fun getCapsuleById(id: Long): TimeCapsule? = dao.getCapsuleById(id)

    fun importImage(uri: Uri): String? {
        val media = DiaryMediaManager.importImage(app, uri)
        return media?.displayRef
    }
}
