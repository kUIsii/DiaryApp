package com.diary.app.ui.capsule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.TimeCapsule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimeCapsuleViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val capsules: StateFlow<List<TimeCapsule>> = dao.getAllCapsules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCapsule(title: String, content: String, unlockDate: Long) {
        viewModelScope.launch {
            dao.insertCapsule(
                TimeCapsule(
                    title = title,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    unlockDate = unlockDate
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

    suspend fun getCapsuleById(id: Long): TimeCapsule? = dao.getCapsuleById(id)
}
