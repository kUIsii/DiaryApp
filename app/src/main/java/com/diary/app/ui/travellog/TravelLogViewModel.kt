package com.diary.app.ui.travellog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TripGroup(
    val id: Int,
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val entryCount: Int
)

class TravelLogViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _trips = MutableStateFlow<List<TripGroup>>(emptyList())
    val trips: StateFlow<List<TripGroup>> = _trips

    init {
        viewModelScope.launch {
            val entries = dao.getEntriesWithLocation()
            val grouped = entries
                .filter { it.location?.isNotBlank() == true }
                .groupBy { it.location ?: "" }
                .map { (location, group) ->
                    TripGroup(
                        id = location.hashCode(),
                        name = location,
                        destination = location,
                        startDate = group.minOf { it.createdAt },
                        endDate = group.maxOf { it.createdAt },
                        entryCount = group.size
                    )
                }
                .sortedByDescending { it.endDate }
            _trips.value = grouped
        }
    }
}
