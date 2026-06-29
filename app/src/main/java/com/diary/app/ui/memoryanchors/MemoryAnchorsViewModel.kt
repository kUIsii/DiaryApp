package com.diary.app.ui.memoryanchors

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.AnchorRelation
import com.diary.app.data.DiaryEntry
import com.diary.app.data.MemoryAnchor
import com.diary.app.ai.aiRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AnchorWithDetails(
    val anchor: MemoryAnchor,
    val relatedCount: Int,
    val diaryEntry: DiaryEntry?
)

data class AiRelationResult(
    val diaryId: Long,
    val relevanceScore: Float,
    val reason: String
)

data class AiRecommendation(
    val diaryId: Long,
    val suggestedTopic: String,
    val reason: String
)

data class AnchorStats(
    val totalAnchors: Int = 0,
    val avgRelations: Float = 0f,
    val mostActiveAnchor: String = "",
    val topicFrequencies: Map<String, Int> = emptyMap()
)

enum class AnchorSortMode { RELATIONS_DESC, CREATED_DESC }

enum class AnchorViewMode { LIST, NETWORK, NARRATIVE }

class MemoryAnchorsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val app = application as DiaryApplication
    private val gson = Gson()
    private val sp = application.getSharedPreferences("memory_anchors", Context.MODE_PRIVATE)

    private val _anchors = MutableStateFlow<List<AnchorWithDetails>>(emptyList())
    val anchors: StateFlow<List<AnchorWithDetails>> = _anchors.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(AnchorSortMode.CREATED_DESC)
    val sortMode: StateFlow<AnchorSortMode> = _sortMode.asStateFlow()

    private val _viewMode = MutableStateFlow(AnchorViewMode.LIST)
    val viewMode: StateFlow<AnchorViewMode> = _viewMode.asStateFlow()

    private val _filteredAnchors = MutableStateFlow<List<AnchorWithDetails>>(emptyList())
    val filteredAnchors: StateFlow<List<AnchorWithDetails>> = _filteredAnchors.asStateFlow()

    private val _recommendations = MutableStateFlow<List<AiRecommendation>>(emptyList())
    val recommendations: StateFlow<List<AiRecommendation>> = _recommendations.asStateFlow()

    private val _selectedAnchor = MutableStateFlow<AnchorWithDetails?>(null)
    val selectedAnchor: StateFlow<AnchorWithDetails?> = _selectedAnchor.asStateFlow()

    private val _relationsForDetail = MutableStateFlow<List<AnchorRelation>>(emptyList())
    val relationsForDetail: StateFlow<List<AnchorRelation>> = _relationsForDetail.asStateFlow()

    private val _relatedEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val relatedEntries: StateFlow<List<DiaryEntry>> = _relatedEntries.asStateFlow()

    private val _narrativeText = MutableStateFlow<String?>(null)
    val narrativeText: StateFlow<String?> = _narrativeText.asStateFlow()

    private val _stats = MutableStateFlow(AnchorStats())
    val stats: StateFlow<AnchorStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAnchors()
        checkForRecommendations()
    }

    fun loadAnchors() {
        viewModelScope.launch {
            dao.getAllMemoryAnchors().collect { anchorList ->
                val details = anchorList.map { anchor ->
                    val entry = dao.getEntryById(anchor.diaryId)
                    val relations = dao.getAnchorRelations(anchor.id).first()
                    AnchorWithDetails(anchor, relations.size, entry)
                }
                _anchors.value = details
                applyFilters()
                computeStats(details)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setSortMode(mode: AnchorSortMode) {
        _sortMode.value = mode
        applyFilters()
    }

    fun setViewMode(mode: AnchorViewMode) {
        _viewMode.value = mode
        if (mode == AnchorViewMode.NARRATIVE && _narrativeText.value == null) {
            generateNarrative()
        }
    }

    private fun applyFilters() {
        val query = _searchQuery.value.lowercase()
        var list = _anchors.value
        if (query.isNotBlank()) {
            list = list.filter { it.anchor.topic.lowercase().contains(query) }
        }
        list = when (_sortMode.value) {
            AnchorSortMode.RELATIONS_DESC -> list.sortedByDescending { it.relatedCount }
            AnchorSortMode.CREATED_DESC -> list.sortedByDescending { it.anchor.createdAt }
        }
        _filteredAnchors.value = list
    }

    private fun computeStats(details: List<AnchorWithDetails>) {
        if (details.isEmpty()) {
            _stats.value = AnchorStats()
            return
        }
        val total = details.size
        val avg = details.map { it.relatedCount }.average().toFloat()
        val mostActive = details.maxByOrNull { it.relatedCount }
        val freq = details.groupBy { it.anchor.topic }.mapValues { it.value.size }
        _stats.value = AnchorStats(
            totalAnchors = total,
            avgRelations = avg,
            mostActiveAnchor = mostActive?.anchor?.topic ?: "",
            topicFrequencies = freq
        )
    }

    fun selectAnchor(anchorWithDetails: AnchorWithDetails?) {
        _selectedAnchor.value = anchorWithDetails
        if (anchorWithDetails != null) {
            viewModelScope.launch {
                val relations = dao.getAnchorRelations(anchorWithDetails.anchor.id).first()
                _relationsForDetail.value = relations
                _relatedEntries.value = relations.mapNotNull { dao.getEntryById(it.diaryId) }
            }
        } else {
            _relationsForDetail.value = emptyList()
            _relatedEntries.value = emptyList()
            _narrativeText.value = null
        }
    }

    fun addAnchor(topic: String, description: String, diaryId: Long) {
        viewModelScope.launch {
            val anchor = MemoryAnchor(
                diaryId = diaryId,
                topic = topic,
                description = description
            )
            val id = dao.insertMemoryAnchor(anchor)
            findRelatedDiariesWithAI(id, topic, description)
            _showAddDialog.value = false
        }
    }

    fun deleteAnchor(anchorId: Long) {
        viewModelScope.launch {
            dao.deleteMemoryAnchor(anchorId)
        }
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    private suspend fun findRelatedDiariesWithAI(anchorId: Long, topic: String, description: String) {
        if (!app.aiService.isAiEnabled()) return
        val allEntries = dao.getAllEntriesOnce()
        if (allEntries.isEmpty()) return

        val lastScanTime = sp.getLong("anchor_semantic_cache", 0L)
        val entriesToScan = if (lastScanTime == 0L) {
            allEntries
        } else {
            allEntries.filter { it.createdAt > lastScanTime }
        }
        if (entriesToScan.isEmpty()) return

        val diaryText = entriesToScan.joinToString("\n---\n") { "${it.id}:${it.title} ${it.plainText.take(500)}" }
        val userMsg = "锚点主题：$topic，描述：$description。扫描以下日记，找出与主题语义相关的日记及关联强度(0-1)。返回JSON: [{diaryId, relevanceScore, reason}]\n\n$diaryText"

        val result = app.aiService.chat(aiRequest(userMessage = userMsg, maxTokens = 2048))
        result.onSuccess { response ->
            try {
                val cleaned = response.content
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()
                val json = cleaned.substringAfter("[").substringBeforeLast("]").let { "[$it]" }
                val type = object : TypeToken<List<AiRelationResult>>() {}.type
                val relations: List<AiRelationResult> = gson.fromJson(json, type)
                relations.forEach { rel ->
                    if (rel.relevanceScore > 0.3f) {
                        dao.insertAnchorRelation(
                            AnchorRelation(
                                anchorId = anchorId,
                                diaryId = rel.diaryId,
                                relevanceScore = rel.relevanceScore
                            )
                        )
                    }
                }
                sp.edit().putLong("anchor_semantic_cache", System.currentTimeMillis()).apply()
            } catch (_: Exception) {}
        }
    }

    fun checkForRecommendations() {
        viewModelScope.launch {
            val lastRec = sp.getLong("last_anchor_recommendation", 0L)
            val oneWeek = 7 * 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastRec < oneWeek) return@launch
            if (!app.aiService.isAiEnabled()) return@launch

            val allEntries = dao.getAllEntriesOnce()
            if (allEntries.size < 3) return@launch

            val diaryText = allEntries.joinToString("\n---\n") { "${it.id}:${it.title} ${it.plainText.take(200)}" }
            val userMsg = "从以下日记中识别可能出现人生转折点、重要决定或深刻反思的日记。返回JSON: [{diaryId, suggestedTopic, reason}]\n\n$diaryText"

            val result = app.aiService.chat(aiRequest(userMessage = userMsg, maxTokens = 2048))
            result.onSuccess { response ->
                try {
                    val cleaned = response.content
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()
                    val json = cleaned.substringAfter("[").substringBeforeLast("]").let { "[$it]" }
                    val type = object : TypeToken<List<AiRecommendation>>() {}.type
                    val recs: List<AiRecommendation> = gson.fromJson(json, type)
                    _recommendations.value = recs
                    sp.edit().putLong("last_anchor_recommendation", System.currentTimeMillis()).apply()
                } catch (_: Exception) {}
            }
        }
    }

    fun confirmRecommendation(rec: AiRecommendation) {
        viewModelScope.launch {
            val entry = dao.getEntryById(rec.diaryId)
            if (entry != null) {
                val anchor = MemoryAnchor(
                    diaryId = rec.diaryId,
                    topic = rec.suggestedTopic,
                    description = rec.reason
                )
                val id = dao.insertMemoryAnchor(anchor)
                findRelatedDiariesWithAI(id, rec.suggestedTopic, rec.reason)
            }
            _recommendations.value = _recommendations.value.filter { it != rec }
        }
    }

    fun dismissRecommendation(rec: AiRecommendation) {
        _recommendations.value = _recommendations.value.filter { it != rec }
    }

    fun dismissAllRecommendations() {
        _recommendations.value = emptyList()
    }

    fun generateNarrative() {
        viewModelScope.launch {
            val anchorsList = dao.getAllMemoryAnchors().first()
            if (anchorsList.size < 2) {
                _narrativeText.value = "需要至少2个锚点才能生成综合叙事"
                return@launch
            }
            if (!app.aiService.isAiEnabled()) {
                _narrativeText.value = "AI 服务未配置"
                return@launch
            }
            _isLoading.value = true
            val names = anchorsList.joinToString("、") { it.topic }
            val userMsg = "我有以下记忆锚点：$names。请根据这些主题，生成一段简短的个人叙事，将这些事件串联起来，讲述一个连贯的人生故事。"
            val result = app.aiService.chat(aiRequest(userMessage = userMsg, maxTokens = 1024))
            result.onSuccess { response ->
                _narrativeText.value = response.content
            }.onFailure {
                _narrativeText.value = "生成叙事失败"
            }
            _isLoading.value = false
        }
    }
}
