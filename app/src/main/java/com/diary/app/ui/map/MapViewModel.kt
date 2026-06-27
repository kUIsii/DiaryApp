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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

data class RouteStats(
    val totalDistanceKm: Double = 0.0,
    val uniqueCities: Int = 0,
    val uniqueLocations: Int = 0,
    val daySpan: Int = 0,
    val routePoints: List<MapMarker> = emptyList()
)

data class MapUiState(
    val isLoading: Boolean = true,
    val markers: List<MapMarker> = emptyList(),
    val selectedMarker: MapMarker? = null,
    val stats: MapStats = MapStats(),
    val error: String? = null,
    val isRouteMode: Boolean = false,
    val routeStats: RouteStats? = null
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

    fun toggleRouteMode() {
        val current = _uiState.value
        val newRouteMode = !current.isRouteMode
        val routeStats = if (newRouteMode) computeRouteStats(current.markers) else null
        _uiState.value = current.copy(isRouteMode = newRouteMode, routeStats = routeStats)
    }

    private fun computeRouteStats(markers: List<MapMarker>): RouteStats {
        if (markers.isEmpty()) return RouteStats()

        // Sort by creation time to form chronological route
        val sorted = markers.sortedBy { it.createdAt }

        // Deduplicate locations (keep first occurrence of each location)
        val visitedLocations = mutableListOf<MapMarker>()
        val seenLocations = mutableSetOf<String>()
        for (marker in sorted) {
            val locKey = "${marker.latitude},${marker.longitude}"
            if (locKey !in seenLocations) {
                seenLocations.add(locKey)
                visitedLocations.add(marker)
            }
        }

        // Calculate total distance
        var totalDistance = 0.0
        for (i in 1 until visitedLocations.size) {
            totalDistance += haversine(
                visitedLocations[i - 1].latitude, visitedLocations[i - 1].longitude,
                visitedLocations[i].latitude, visitedLocations[i].longitude
            )
        }

        // Count unique cities
        val cities = sorted.map { extractCityFromLocation(it.location) }
            .filter { it.isNotBlank() }
            .distinct()
            .size

        // Calculate day span
        val daySpan = if (sorted.size >= 2) {
            val firstDay = sorted.first().createdAt / (24 * 60 * 60 * 1000)
            val lastDay = sorted.last().createdAt / (24 * 60 * 60 * 1000)
            (lastDay - firstDay).toInt() + 1
        } else 1

        return RouteStats(
            totalDistanceKm = totalDistance,
            uniqueCities = cities,
            uniqueLocations = visitedLocations.size,
            daySpan = daySpan,
            routePoints = visitedLocations
        )
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

internal fun extractCityFromLocation(location: String): String {
    val parts = location.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return when {
        parts.size >= 3 -> parts[parts.size - 2]
        parts.size == 2 -> parts[1]
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
