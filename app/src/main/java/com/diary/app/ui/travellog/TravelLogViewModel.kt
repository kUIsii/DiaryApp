package com.diary.app.ui.travellog

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import com.diary.app.data.DiaryPreview
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Trip(
    val id: Long,
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val description: String = "",
    val entryIds: List<Long> = emptyList(),
    val isManual: Boolean = false
)

data class TripEntry(
    val id: Long,
    val title: String,
    val plainText: String,
    val moodLevel: Int?,
    val location: String?,
    val createdAt: Long
)

data class TripStats(
    val entryCount: Int = 0,
    val moodAverage: Float = 0f,
    val totalWordCount: Int = 0,
    val dateRange: String = ""
)

data class TravelLogUiState(
    val trips: List<Trip> = emptyList(),
    val groupedEntries: List<TripEntry> = emptyList(),
    val ungroupedEntries: List<TripEntry> = emptyList(),
    val selectedTrip: Trip? = null,
    val tripStats: TripStats = TripStats(),
    val tripEntries: List<TripEntry> = emptyList(),
    val isCreating: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isGeneratingSummary: Boolean = false,
    val aiSummary: String = "",
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingTrip: Trip? = null,
    val showDeleteConfirm: Boolean = false,
    val deleteTargetTrip: Trip? = null,
    val message: String = "",
    val showDetail: Boolean = false,
    val availableEntries: List<TripEntry> = emptyList(),
    val suggestedEntries: List<TripEntry> = emptyList()
)

class TravelLogViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val gson = Gson()
    private val prefs = application.getSharedPreferences("travel_log", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TravelLogUiState())
    val uiState: StateFlow<TravelLogUiState> = _uiState.asStateFlow()

    private var allPreviews: List<DiaryPreview> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val previews = withContext(Dispatchers.IO) { dao.getEntriesWithLocation() }
                allPreviews = previews
                val entries = previews
                    .filter { it.location?.isNotBlank() == true }
                    .map { it.toTripEntry() }

                val savedTrips = loadSavedTrips()
                val autoTrips = autoClusterTrips(entries, savedTrips)

                val allTrips = (savedTrips + autoTrips)
                    .distinctBy { it.id }
                    .sortedByDescending { it.endDate }

                val ungrouped = findUngroupedEntries(entries, allTrips)

                _uiState.value = _uiState.value.copy(
                    trips = allTrips,
                    groupedEntries = entries,
                    ungroupedEntries = ungrouped,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("TravelLog", "Error loading data", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            availableEntries = _uiState.value.ungroupedEntries
        )
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun showEditDialog(trip: Trip) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingTrip = trip,
            availableEntries = _uiState.value.ungroupedEntries
        )
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            editingTrip = null
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = "")
    }

    fun createTrip(
        name: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        description: String,
        selectedEntryIds: List<Long>
    ) {
        viewModelScope.launch {
            val tripId = System.currentTimeMillis()
            val trip = Trip(
                id = tripId,
                name = name,
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                description = description,
                entryIds = selectedEntryIds,
                isManual = true
            )
            val savedTrips = loadSavedTrips().toMutableList()
            savedTrips.add(trip)
            saveTrips(savedTrips)

            val allTrips = (savedTrips + autoClusterTrips(_uiState.value.groupedEntries, savedTrips))
                .distinctBy { it.id }
                .sortedByDescending { it.endDate }

            val ungrouped = findUngroupedEntries(_uiState.value.groupedEntries, allTrips)

            _uiState.value = _uiState.value.copy(
                trips = allTrips,
                ungroupedEntries = ungrouped,
                showCreateDialog = false
            )
        }
    }

    fun showTripDetail(trip: Trip) {
        val entries = _uiState.value.groupedEntries.filter { it.id in trip.entryIds }
            .sortedByDescending { it.createdAt }
        val stats = computeStats(trip, entries)
        val suggested = findSuggestedEntries(trip)
        val cachedSummary = getCachedSummary(trip.id)

        _uiState.value = _uiState.value.copy(
            selectedTrip = trip,
            tripStats = stats,
            tripEntries = entries,
            showDetail = true,
            aiSummary = cachedSummary ?: "",
            isGeneratingSummary = false,
            suggestedEntries = suggested
        )
    }

    fun hideTripDetail() {
        _uiState.value = _uiState.value.copy(
            selectedTrip = null,
            showDetail = false,
            aiSummary = ""
        )
    }

    fun addEntriesToTrip(tripId: Long, entryIds: List<Long>) {
        viewModelScope.launch {
            val savedTrips = loadSavedTrips().toMutableList()
            val idx = savedTrips.indexOfFirst { it.id == tripId }
            if (idx >= 0) {
                val trip = savedTrips[idx]
                val otherEntryIds = savedTrips.filter { it.id != tripId }.flatMap { it.entryIds }.toSet()
                val validEntryIds = entryIds.filter { it !in otherEntryIds }
                val skippedCount = entryIds.size - validEntryIds.size
                if (skippedCount > 0) {
                    _uiState.value = _uiState.value.copy(
                        message = "${skippedCount} 篇日记已被其他行程收录，已跳过"
                    )
                }
                if (validEntryIds.isEmpty()) return@launch
                val merged = (trip.entryIds + validEntryIds).distinct()
                val updated = trip.copy(
                    entryIds = merged,
                    startDate = minOf(trip.startDate, _uiState.value.groupedEntries.filter { it.id in merged }.minOf { it.createdAt }),
                    endDate = maxOf(trip.endDate, _uiState.value.groupedEntries.filter { it.id in merged }.maxOf { it.createdAt })
                )
                savedTrips[idx] = updated
                saveTrips(savedTrips)

                val allTrips = (savedTrips + autoClusterTrips(_uiState.value.groupedEntries, savedTrips))
                    .distinctBy { it.id }
                    .sortedByDescending { it.endDate }

                val ungrouped = findUngroupedEntries(_uiState.value.groupedEntries, allTrips)

                _uiState.value = _uiState.value.copy(
                    trips = allTrips,
                    ungroupedEntries = ungrouped
                )

                if (_uiState.value.selectedTrip?.id == tripId) {
                    showTripDetail(updated)
                }
            }
        }
    }

    fun removeEntryFromTrip(tripId: Long, entryId: Long) {
        viewModelScope.launch {
            val savedTrips = loadSavedTrips().toMutableList()
            val idx = savedTrips.indexOfFirst { it.id == tripId }
            if (idx >= 0) {
                val trip = savedTrips[idx]
                val updated = trip.copy(entryIds = trip.entryIds - entryId)
                savedTrips[idx] = updated
                saveTrips(savedTrips)

                val allTrips = (savedTrips + autoClusterTrips(_uiState.value.groupedEntries, savedTrips))
                    .distinctBy { it.id }
                    .sortedByDescending { it.endDate }

                val ungrouped = findUngroupedEntries(_uiState.value.groupedEntries, allTrips)

                _uiState.value = _uiState.value.copy(
                    trips = allTrips,
                    ungroupedEntries = ungrouped
                )

                if (_uiState.value.selectedTrip?.id == tripId) {
                    showTripDetail(updated)
                }
            }
        }
    }

    fun requestDeleteTrip(trip: Trip) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            deleteTargetTrip = trip
        )
    }

    fun confirmDeleteTrip() {
        val trip = _uiState.value.deleteTargetTrip ?: return
        deleteTrip(trip.id)
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            deleteTargetTrip = null
        )
    }

    fun cancelDeleteTrip() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            deleteTargetTrip = null
        )
    }

    fun editTrip(
        tripId: Long,
        name: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        description: String
    ) {
        viewModelScope.launch {
            val savedTrips = loadSavedTrips().toMutableList()
            val idx = savedTrips.indexOfFirst { it.id == tripId }
            if (idx >= 0) {
                savedTrips[idx] = savedTrips[idx].copy(
                    name = name,
                    destination = destination,
                    startDate = startDate,
                    endDate = endDate,
                    description = description
                )
                saveTrips(savedTrips)

                val allTrips = (savedTrips + autoClusterTrips(_uiState.value.groupedEntries, savedTrips))
                    .distinctBy { it.id }
                    .sortedByDescending { it.endDate }

                val ungrouped = findUngroupedEntries(_uiState.value.groupedEntries, allTrips)

                _uiState.value = _uiState.value.copy(
                    trips = allTrips,
                    ungroupedEntries = ungrouped,
                    showEditDialog = false,
                    editingTrip = null
                )

                if (_uiState.value.selectedTrip?.id == tripId) {
                    showTripDetail(savedTrips[idx])
                }
            } else {
                _uiState.value = _uiState.value.copy(showEditDialog = false, editingTrip = null)
            }
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            val savedTrips = loadSavedTrips().toMutableList()
            savedTrips.removeAll { it.id == tripId }
            saveTrips(savedTrips)

            val allTrips = (savedTrips + autoClusterTrips(_uiState.value.groupedEntries, savedTrips))
                .distinctBy { it.id }
                .sortedByDescending { it.endDate }

            val ungrouped = findUngroupedEntries(_uiState.value.groupedEntries, allTrips)

            _uiState.value = _uiState.value.copy(
                trips = allTrips,
                ungroupedEntries = ungrouped,
                showDetail = false,
                selectedTrip = null
            )
        }
    }

    fun generateSummary() {
        val trip = _uiState.value.selectedTrip ?: return
        val entries = _uiState.value.tripEntries
        if (entries.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingSummary = true, aiSummary = "")
            try {
                if (!app.aiService.isAiEnabled()) {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingSummary = false,
                        aiSummary = "请先在设置中配置 AI 服务"
                    )
                    return@launch
                }

                val context = buildTripContext(trip, entries)
                val systemPrompt = "你是一个旅行日记助手。请根据用户提供的行程信息和日记内容，生成一段简洁优美的旅行总结。突出旅行的亮点、心情变化和难忘时刻。控制在 300 字以内，用纯文本。"
                val userPrompt = "以下是我的旅行记录，请帮我写一段总结：\n\n$context"

                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(
                            AiMessage("system", systemPrompt),
                            AiMessage("user", userPrompt)
                        ),
                        temperature = 0.7f,
                        maxTokens = 500
                    )
                )

                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    cacheSummary(trip.id, reply)
                    _uiState.value = _uiState.value.copy(
                        isGeneratingSummary = false,
                        aiSummary = reply
                    )
                } else {
                    val ex = result.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isGeneratingSummary = false,
                        aiSummary = "生成失败：${ex?.message ?: "未知错误"}"
                    )
                }
            } catch (e: Exception) {
                Log.e("TravelLog", "Summary generation failed", e)
                _uiState.value = _uiState.value.copy(
                    isGeneratingSummary = false,
                    aiSummary = "生成失败，请稍后重试"
                )
            }
        }
    }

    private fun computeStats(trip: Trip, entries: List<TripEntry>): TripStats {
        val moodValues = entries.mapNotNull { it.moodLevel }
        val moodAvg = if (moodValues.isNotEmpty()) moodValues.sum().toFloat() / moodValues.size else 0f
        val wordCount = entries.sumOf { it.plainText.length }
        val dateRange = "${formatDate(trip.startDate)} - ${formatDate(trip.endDate)}"
        return TripStats(
            entryCount = entries.size,
            moodAverage = moodAvg,
            totalWordCount = wordCount,
            dateRange = dateRange
        )
    }

    private fun buildTripContext(trip: Trip, entries: List<TripEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("行程名称：${trip.name}")
        sb.appendLine("目的地：${trip.destination}")
        sb.appendLine("时间：${formatDate(trip.startDate)} 至 ${formatDate(trip.endDate)}")
        sb.appendLine("日记数量：${entries.size} 篇")
        sb.appendLine()
        sb.appendLine("日记内容：")
        for (entry in entries.sortedBy { it.createdAt }) {
            val date = formatDate(entry.createdAt)
            val mood = entry.moodLevel?.let { moodLabel(it) } ?: ""
            sb.appendLine("[$date]${if (mood.isNotEmpty()) " ($mood)" else ""} ${entry.title}: ${entry.plainText.take(200)}")
        }
        return sb.toString()
    }

    private fun findSuggestedEntries(trip: Trip): List<TripEntry> {
        val allEntries = _uiState.value.groupedEntries
        val usedIds = trip.entryIds.toSet()

        return allEntries
            .filter { it.id !in usedIds }
            .filter { entry ->
                val sameLocation = entry.location == trip.destination
                val inDateRange = entry.createdAt in trip.startDate..trip.endDate
                sameLocation || inDateRange
            }
            .sortedByDescending { it.createdAt }
            .take(10)
    }

    private fun DiaryPreview.toTripEntry() = TripEntry(
        id = id,
        title = title,
        plainText = plainText,
        moodLevel = moodLevel,
        location = location,
        createdAt = createdAt
    )

    private fun autoClusterTrips(
        entries: List<TripEntry>,
        savedTrips: List<Trip>
    ): List<Trip> {
        val savedEntryIds = savedTrips.flatMap { it.entryIds }.toSet()
        val remaining = entries.filter { it.id !in savedEntryIds }

        return remaining
            .groupBy { it.location ?: "" }
            .filterKeys { it.isNotBlank() }
            .flatMap { (location, group) ->
                group.sortedBy { it.createdAt }.let { sorted ->
                    val clusters = mutableListOf<List<TripEntry>>()
                    var current = mutableListOf(sorted.first())
                    for (i in 1 until sorted.size) {
                        val gap = sorted[i].createdAt - sorted[i - 1].createdAt
                        if (gap > 7L * 24 * 60 * 60 * 1000) {
                            clusters.add(current)
                            current = mutableListOf(sorted[i])
                        } else {
                            current.add(sorted[i])
                        }
                    }
                    if (current.isNotEmpty()) clusters.add(current)
                    clusters.mapIndexed { index, cluster ->
                        val suffix = if (index == 0) "" else "${index + 1}"
                        Trip(
                            id = -(location.hashCode().toLong() and 0x7FFFFFFF) * 1000L + index,
                            name = "${location}$suffix",
                            destination = location,
                            startDate = cluster.minOf { it.createdAt },
                            endDate = cluster.maxOf { it.createdAt },
                            entryIds = cluster.map { it.id },
                            isManual = false
                        )
                    }
                }
            }
    }

    private fun findUngroupedEntries(
        entries: List<TripEntry>,
        trips: List<Trip>
    ): List<TripEntry> {
        val usedIds = trips.flatMap { it.entryIds }.toSet()
        return entries.filter { it.id !in usedIds }
            .sortedByDescending { it.createdAt }
    }

    private fun loadSavedTrips(): List<Trip> {
        val json = prefs.getString("trips", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Trip>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTrips(trips: List<Trip>) {
        val manualTrips = trips.filter { it.isManual }
        prefs.edit().putString("trips", gson.toJson(manualTrips)).apply()
    }

    private fun formatDate(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy/M/d", java.util.Locale.CHINA)
        return sdf.format(java.util.Date(millis))
    }

    fun moodLabel(level: Int): String {
        return when (level) {
            1 -> "低落"
            2 -> "忧郁"
            3 -> "平静"
            4 -> "愉快"
            5 -> "开心"
            6 -> "兴奋"
            else -> ""
        }
    }

    private fun getCachedSummary(tripId: Long): String? {
        return prefs.getString("summary_$tripId", null)
    }

    private fun cacheSummary(tripId: Long, summary: String) {
        prefs.edit().putString("summary_$tripId", summary).apply()
    }
}
