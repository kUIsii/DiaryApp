package com.diary.app.ui.locationmemories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.LocationMemory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationMemoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val memories: StateFlow<List<LocationMemory>> = dao.getAllLocationMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMemory(id: Long) {
        viewModelScope.launch { dao.deleteLocationMemory(id) }
    }
}
