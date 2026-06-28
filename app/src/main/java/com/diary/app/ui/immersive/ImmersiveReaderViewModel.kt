package com.diary.app.ui.immersive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ImmersiveReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _entries = MutableStateFlow<List<DiaryPreview>>(emptyList())
    val entries: StateFlow<List<DiaryPreview>> = _entries

    private val _warmLightEnabled = MutableStateFlow(false)
    val warmLightEnabled: StateFlow<Boolean> = _warmLightEnabled

    private val _fontSize = MutableStateFlow(18)
    val fontSize: StateFlow<Int> = _fontSize

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            dao.getAllPreviews().collect { list ->
                _entries.value = list
            }
        }
    }

    fun toggleWarmLight() {
        _warmLightEnabled.value = !_warmLightEnabled.value
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size
    }
}
