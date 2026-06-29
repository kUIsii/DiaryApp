package com.diary.app.ui.locationmemories

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryPreview
import com.diary.app.data.LocationMemory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocationMoodAnalysis(
    val location: String,
    val dominantMood: String,
    val correlationStrength: String,
    val analysis: String
)

data class StorylinePeriod(
    val period: String,
    val moodTrend: String,
    val keyEvents: List<String>,
    val insight: String
)

class LocationMemoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = (application as DiaryApplication).aiService
    private val prefs = application.getSharedPreferences("location_memories", Context.MODE_PRIVATE)
    private val gson = Gson()

    val memories: StateFlow<List<LocationMemory>> = dao.getAllLocationMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _geoEnabled = MutableStateFlow(true)
    val geoEnabled: StateFlow<Boolean> = _geoEnabled.asStateFlow()

    private val _notifyRadius = MutableStateFlow(100f)
    val notifyRadius: StateFlow<Float> = _notifyRadius.asStateFlow()

    private val _selectedLocation = MutableStateFlow<String?>(null)
    val selectedLocation: StateFlow<String?> = _selectedLocation.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _isStorylineLoading = MutableStateFlow(false)
    val isStorylineLoading: StateFlow<Boolean> = _isStorylineLoading.asStateFlow()

    private val _isAddDialogVisible = MutableStateFlow(false)
    val isAddDialogVisible: StateFlow<Boolean> = _isAddDialogVisible.asStateFlow()

    private val _diariesByLocation = MutableStateFlow<Map<String, List<DiaryPreview>>>(emptyMap())
    val diariesByLocation: StateFlow<Map<String, List<DiaryPreview>>> = _diariesByLocation.asStateFlow()

    private val _moodAnalysis = MutableStateFlow<List<LocationMoodAnalysis>>(emptyList())
    val moodAnalysis: StateFlow<List<LocationMoodAnalysis>> = _moodAnalysis.asStateFlow()

    private val _storyline = MutableStateFlow<List<StorylinePeriod>>(emptyList())
    val storyline: StateFlow<List<StorylinePeriod>> = _storyline.asStateFlow()

    init {
        _geoEnabled.value = prefs.getBoolean("geo_reminder_enabled", true)
        _notifyRadius.value = prefs.getFloat("geo_reminder_radius", 100f)
        loadCachedAnalysis()
        loadDiaryData()
    }

    private fun loadCachedAnalysis() {
        val json = prefs.getString("location_mood_analysis", null) ?: return
        try {
            val type = object : TypeToken<List<LocationMoodAnalysis>>() {}.type
            _moodAnalysis.value = gson.fromJson(json, type)
        } catch (_: Exception) {}
    }

    private fun loadDiaryData() {
        viewModelScope.launch {
            dao.getAllLocationMemories().collect { list ->
                val ids = list.map { it.diaryId }.distinct().filter { it > 0 }
                if (ids.isEmpty()) {
                    _diariesByLocation.value = emptyMap()
                    return@collect
                }
                val previews = dao.getPreviewsByIds(ids)
                val grouped = mutableMapOf<String, MutableList<DiaryPreview>>()
                for (mem in list) {
                    val name = mem.locationName ?: "(${mem.latitude}, ${mem.longitude})"
                    val preview = previews.find { it.id == mem.diaryId }
                    if (preview != null) {
                        grouped.getOrPut(name) { mutableListOf() }.add(preview)
                    }
                }
                _diariesByLocation.value = grouped
            }
        }
    }

    fun toggleGeoEnabled() {
        _geoEnabled.value = !_geoEnabled.value
        prefs.edit().putBoolean("geo_reminder_enabled", _geoEnabled.value).apply()
    }

    fun updateRadius(radius: Float) {
        _notifyRadius.value = radius
        prefs.edit().putFloat("geo_reminder_radius", radius).apply()
    }

    fun selectLocation(name: String?) {
        _selectedLocation.value = name
        if (name != null) {
            _storyline.value = emptyList()
            loadStoryline(name)
        }
    }

    fun showAddDialog() { _isAddDialogVisible.value = true }
    fun hideAddDialog() { _isAddDialogVisible.value = false }

    fun analyzeMoodCorrelation() {
        if (!aiService.isAiEnabled()) return
        _isAiLoading.value = true
        viewModelScope.launch {
            try {
                val data = _diariesByLocation.value.map { (loc, entries) ->
                    mapOf(
                        "location" to loc,
                        "moods" to entries.mapNotNull { it.moodLevel },
                        "summaries" to entries.map { it.plainText.take(100) }
                    )
                }
                val prompt = "分析以下地点列表和对应的日记摘要+心情：${gson.toJson(data)}。找出地点与情绪之间的关联模式。输出JSON格式：[{location, dominantMood, correlationStrength, analysis}]。只输出JSON数组。"
                val request = aiRequest(prompt, "你是一个数据分析师。分析地点与情绪关联。")
                aiService.chat(request).onSuccess { response ->
                    val type = object : TypeToken<List<LocationMoodAnalysis>>() {}.type
                    val result: List<LocationMoodAnalysis> = gson.fromJson(response.content, type)
                    _moodAnalysis.value = result
                    prefs.edit().putString("location_mood_analysis", gson.toJson(_moodAnalysis.value)).apply()
                }
            } catch (_: Exception) {}
            _isAiLoading.value = false
        }
    }

    private fun loadStoryline(locationName: String) {
        if (!aiService.isAiEnabled()) return
        _isStorylineLoading.value = true
        viewModelScope.launch {
            try {
                val entries = _diariesByLocation.value[locationName].orEmpty()
                val diaryText = entries.joinToString("\n---\n") { e ->
                    "[${e.createdAt}] 心情=${e.moodLevel} ${e.plainText.take(200)}"
                }
                val prompt = "以下是在[$locationName]写的一系列日记：\n$diaryText\n请生成故事线。输出JSON格式：[{period, moodTrend, keyEvents:[], insight}]。只输出JSON。"
                val request = aiRequest(prompt, "你是一个故事分析师。生成地点故事线。")
                aiService.chat(request).onSuccess { response ->
                    val type = object : TypeToken<List<StorylinePeriod>>() {}.type
                    val result: List<StorylinePeriod> = gson.fromJson(response.content, type)
                    _storyline.value = result
                }
            } catch (_: Exception) {}
            _isStorylineLoading.value = false
        }
    }

    fun addLocation(name: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            dao.insertLocationMemory(
                LocationMemory(
                    diaryId = 0,
                    latitude = lat,
                    longitude = lng,
                    radiusMeters = _notifyRadius.value,
                    locationName = name
                )
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { dao.deleteLocationMemory(id) }
    }

    fun getLocationStats(): List<LocationStats> {
        return _diariesByLocation.value.map { (name, entries) ->
            val moods = entries.mapNotNull { it.moodLevel }
            LocationStats(
                name = name,
                visitCount = entries.size,
                totalDiaries = entries.size,
                avgMood = if (moods.isNotEmpty()) moods.sum().toFloat() / moods.size else 0f
            )
        }
    }

    fun getMoodDistribution(): Map<Int, Int> {
        val all = _diariesByLocation.value.values.flatten()
        return all.mapNotNull { it.moodLevel }.groupBy { it }.mapValues { it.value.size }
    }

    fun getMoodLabel(mood: Int): String = when (mood) {
        1 -> "沮丧"
        2 -> "低落"
        3 -> "一般"
        4 -> "不错"
        5 -> "开心"
        6 -> "兴奋"
        else -> "未知"
    }
}

data class LocationStats(
    val name: String,
    val visitCount: Int,
    val totalDiaries: Int,
    val avgMood: Float
)
