package com.diary.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MapMarker(
    val id: Long,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val location: String,
    val createdAt: Long,
    val moodLevel: Int?
)

data class MapUiState(
    val isLoading: Boolean = true,
    val markers: List<MapMarker> = emptyList(),
    val selectedMarker: MapMarker? = null,
    val error: String? = null
)

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val entries = withContext(Dispatchers.IO) { dao.getEntriesWithLocation() }
                val markers = entries.mapNotNull { entry ->
                    val lat = entry.latitude ?: return@mapNotNull null
                    val lon = entry.longitude ?: return@mapNotNull null
                    MapMarker(
                        id = entry.id,
                        title = entry.title.ifBlank { "无标题" },
                        latitude = lat,
                        longitude = lon,
                        location = entry.location ?: "",
                        createdAt = entry.createdAt,
                        moodLevel = entry.moodLevel
                    )
                }
                _uiState.value = MapUiState(isLoading = false, markers = markers)
            } catch (e: Exception) {
                _uiState.value = MapUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun selectMarker(marker: MapMarker?) {
        _uiState.value = _uiState.value.copy(selectedMarker = marker)
    }
}
