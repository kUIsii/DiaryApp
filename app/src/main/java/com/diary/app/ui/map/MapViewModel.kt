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
    val moodLevel: Int?,
    val plainText: String = ""
)

data class MapStats(
    val totalEntries: Int = 0,
    val uniqueLocations: Int = 0,
    val citiesVisited: Int = 0,
    val firstEntryDate: Long? = null,
    val lastEntryDate: Long? = null
)

data class MapUiState(
    val isLoading: Boolean = true,
    val markers: List<MapMarker> = emptyList(),
    val selectedMarker: MapMarker? = null,
    val stats: MapStats = MapStats(),
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
                        moodLevel = entry.moodLevel,
                        plainText = entry.plainText
                    )
                }

                val stats = computeMapStats(markers)

                _uiState.value = MapUiState(
                    isLoading = false,
                    markers = markers,
                    stats = stats
                )
            } catch (e: Exception) {
                _uiState.value = MapUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun selectMarker(marker: MapMarker?) {
        _uiState.value = _uiState.value.copy(selectedMarker = marker)
    }

    private fun extractCity(location: String): String = extractCityFromLocation(location)
}

internal fun extractCityFromLocation(location: String): String {
    val parts = location.split(",").map { it.trim() }
    return when {
        parts.size >= 2 -> parts[parts.size - 2]
        parts.size == 1 -> parts[0]
        else -> ""
    }
}

internal fun computeMapStats(markers: List<MapMarker>): MapStats {
    val uniqueLocations = markers.map { it.location }.filter { it.isNotBlank() }.distinct().size
    val cities = markers.map { extractCityFromLocation(it.location) }.filter { it.isNotBlank() }.distinct().size
    val firstDate = markers.minByOrNull { it.createdAt }?.createdAt
    val lastDate = markers.maxByOrNull { it.createdAt }?.createdAt
    return MapStats(
        totalEntries = markers.size,
        uniqueLocations = uniqueLocations,
        citiesVisited = cities,
        firstEntryDate = firstDate,
        lastEntryDate = lastDate
    )
}
