package com.diary.app.ui.values

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.ExtractedValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TrendPoint(val date: String, val score: Float)

data class ValueConflict(val left: String, val right: String, val reason: String)

class ValuesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = (application as DiaryApplication).aiService
    private val sp = application.getSharedPreferences("values_extraction", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _values = MutableStateFlow<List<ExtractedValue>>(emptyList())
    val values: StateFlow<List<ExtractedValue>> = _values.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _lastAnalysisTime = MutableStateFlow(sp.getLong("last_ai_analysis_time", 0L))
    val lastAnalysisTime: StateFlow<Long> = _lastAnalysisTime.asStateFlow()

    private val _trends = MutableStateFlow<Map<String, List<TrendPoint>>>(emptyMap())
    val trends: StateFlow<Map<String, List<TrendPoint>>> = _trends.asStateFlow()

    private val _conflicts = MutableStateFlow<List<ValueConflict>>(emptyList())
    val conflicts: StateFlow<List<ValueConflict>> = _conflicts.asStateFlow()

    private val _radarMode = MutableStateFlow("current")
    val radarMode: StateFlow<String> = _radarMode.asStateFlow()

    private val _evidenceMap = MutableStateFlow<Map<String, List<Long>>>(emptyMap())
    val evidenceMap: StateFlow<Map<String, List<Long>>> = _evidenceMap.asStateFlow()

    init {
        loadValues()
        extractValues()
        loadTrends()
        loadConflicts()
        loadEvidenceMap()
        checkAutoAnalysis()
    }

    fun loadValues() {
        viewModelScope.launch {
            dao.getAllExtractedValues().collect { list ->
                _values.value = list
            }
        }
    }

    fun toggleRadarMode() {
        _radarMode.value = if (_radarMode.value == "current") "monthly" else "current"
    }

    private fun loadTrends() {
        val json = sp.getString("value_trends", null) ?: return
        try {
            val type = object : TypeToken<Map<String, List<TrendPoint>>>() {}.type
            _trends.value = gson.fromJson(json, type)
        } catch (_: Exception) {}
    }

    private fun loadConflicts() {
        val json = sp.getString("value_conflicts", null) ?: return
        try {
            val type = object : TypeToken<List<ValueConflict>>() {}.type
            _conflicts.value = gson.fromJson(json, type)
        } catch (_: Exception) {}
    }

    private fun loadEvidenceMap() {
        val json = sp.getString("value_evidence_map", null) ?: return
        try {
            val type = object : TypeToken<Map<String, List<Long>>>() {}.type
            _evidenceMap.value = gson.fromJson(json, type)
        } catch (_: Exception) {}
    }

    private fun checkAutoAnalysis() {
        val last = sp.getLong("last_ai_analysis_time", 0L)
        if (System.currentTimeMillis() - last > 7 * 24 * 60 * 60 * 1000L) {
            triggerAiAnalysis()
        }
    }

    fun triggerAiAnalysis() {
        if (_aiLoading.value || !aiService.isAiEnabled()) return
        _aiLoading.value = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
            val entries = dao.getEntriesByDateRange(thirtyDaysAgo, now)
            if (entries.isEmpty()) {
                _aiLoading.value = false
                return@launch
            }

            val summaries = entries.joinToString("\n---\n") { e ->
                val preview = e.plainText.take(200)
                "标题:${e.title}\n情绪:${e.moodLevel ?: "未知"}\n正文:$preview"
            }

            val systemPrompt = "你是一名心理学专家，从以下日记中提取用户的核心价值观。每篇日记分析：1)体现了什么价值观 2)情绪倾向 3)证据句子。返回JSON: [{category, value, evidence, confidence}]。类别不限于：家庭/成长/健康/友情/事业/兴趣/自由/创造/安全感/意义感。额外返回 contradictions 字段: [{left, right, reason}] 表示价值观矛盾。"
            val userMessage = "以下是近30天日记摘要：\n$summaries"

            val result = aiService.chat(aiRequest(userMessage, systemPrompt, maxTokens = 2048))
            result.onSuccess { response ->
                parseAndMerge(response.content)
                sp.edit().putLong("last_ai_analysis_time", now).apply()
                _lastAnalysisTime.value = now
            }
            _aiLoading.value = false
        }
    }

    private suspend fun parseAndMerge(jsonStr: String) {
        try {
            val json = gson.fromJson(jsonStr, Map::class.java)
            val aiCategories = (json["categories"] as? List<Map<String, Any>>) ?: (tryParseFlat(jsonStr))

            val aiValues = mutableMapOf<String, ExtractedValue>()
            val conflicts = mutableListOf<ValueConflict>()
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            aiCategories.forEach { item ->
                val cat = item["category"]?.toString() ?: return@forEach
                val value = item["value"]?.toString() ?: cat
                val evidence = item["evidence"]?.toString() ?: ""
                val confidence = (item["confidence"] as? Number)?.toFloat() ?: 0.5f
                val mergedConfidence = (confidence * 0.8f).coerceIn(0f, 1f)
                aiValues[cat] = ExtractedValue(category = cat, value = value, evidence = evidence, confidence = mergedConfidence, updatedAt = System.currentTimeMillis())
            }

            val contradictions = json["contradictions"] as? List<Map<String, String>>
            contradictions?.forEach { c ->
                conflicts.add(ValueConflict(
                    left = c["left"] ?: "",
                    right = c["right"] ?: "",
                    reason = c["reason"] ?: ""
                ))
            }

            sp.edit().putString("value_conflicts", gson.toJson(conflicts)).apply()
            _conflicts.value = conflicts

            viewModelScope.launch {
                val keywordValues = extractKeywordValues()
                val merged = mutableMapOf<String, ExtractedValue>()

                keywordValues.forEach { (cat, ev) ->
                    val kwConf = (ev.confidence * 0.3f).coerceIn(0f, 1f)
                    merged[cat] = ev.copy(confidence = kwConf)
                }

                aiValues.forEach { (cat, ev) ->
                    val existing = merged[cat]
                    if (existing != null) {
                        val combined = (ev.confidence + existing.confidence).coerceAtMost(1f)
                        merged[cat] = ev.copy(
                            confidence = combined,
                            evidence = if (ev.evidence.isNotBlank()) ev.evidence else existing.evidence
                        )
                    } else {
                        merged[cat] = ev
                    }
                }

                merged.forEach { (_, ev) ->
                    dao.insertExtractedValue(ev)
                }

                val trendPoints = merged.mapValues { (_, ev) ->
                    TrendPoint(date = todayStr, score = ev.confidence)
                }
                val oldTrends = _trends.value.toMutableMap()
                trendPoints.forEach { (cat, pt) ->
                    val list = oldTrends.getOrDefault(cat, emptyList()).toMutableList()
                    list.add(pt)
                    oldTrends[cat] = list
                }
                _trends.value = oldTrends
                sp.edit().putString("value_trends", gson.toJson(oldTrends)).apply()

                val evidenceEntries = mutableMapOf<String, MutableList<Long>>()
                merged.forEach { (cat, ev) ->
                    val ids = extractDiaryIdsForCategory(cat)
                    if (ids.isNotEmpty()) {
                        evidenceEntries[cat] = ids.toMutableList()
                    }
                }
                if (evidenceEntries.isNotEmpty()) {
                    val oldEvidence = _evidenceMap.value.toMutableMap()
                    evidenceEntries.forEach { (cat, ids) ->
                        oldEvidence[cat] = ids
                    }
                    _evidenceMap.value = oldEvidence
                    sp.edit().putString("value_evidence_map", gson.toJson(oldEvidence)).apply()
                }
            }
        } catch (_: Exception) {}
    }

    private fun tryParseFlat(jsonStr: String): List<Map<String, Any>> {
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            gson.fromJson(jsonStr, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun extractDiaryIdsForCategory(category: String): List<Long> {
        val keywords = mapOf(
            "家庭" to listOf("家", "爸妈", "孩子", "家人", "妈妈", "爸爸", "老公", "老婆"),
            "成长" to listOf("学习", "进步", "提升", "读书", "课程", "成长", "努力"),
            "健康" to listOf("运动", "健身", "跑步", "饮食", "睡眠", "锻炼", "养生"),
            "友情" to listOf("朋友", "聚会", "聊天", "闺蜜", "兄弟", "同事"),
            "事业" to listOf("工作", "项目", "职业", "升职", "创业", "事业"),
            "兴趣" to listOf("爱好", "画画", "音乐", "旅行", "摄影", "写作")
        )
        val kw = keywords[category] ?: return emptyList()
        return runCatching {
            val entries = dao.getAllEntriesOnce()
            entries.filter { e -> kw.any { e.plainText.contains(it) } }
                .map { it.id }
                .take(5)
        }.getOrDefault(emptyList())
    }

    private suspend fun extractKeywordValues(): Map<String, ExtractedValue> {
        val entries = dao.getAllEntriesOnce()
        if (entries.isEmpty()) return emptyMap()

        val valueCategories = mapOf(
            "家庭" to listOf("家", "爸妈", "孩子", "家人", "妈妈", "爸爸", "老公", "老婆"),
            "成长" to listOf("学习", "进步", "提升", "读书", "课程", "成长", "努力"),
            "健康" to listOf("运动", "健身", "跑步", "饮食", "睡眠", "锻炼", "养生"),
            "友情" to listOf("朋友", "聚会", "聊天", "闺蜜", "兄弟", "同事"),
            "事业" to listOf("工作", "项目", "职业", "升职", "创业", "事业"),
            "兴趣" to listOf("爱好", "画画", "音乐", "旅行", "摄影", "写作")
        )

        val result = mutableMapOf<String, ExtractedValue>()
        valueCategories.forEach { (category, keywords) ->
            var mentionCount = 0
            val evidence = mutableListOf<String>()
            entries.forEach { entry ->
                val text = entry.plainText
                val matches = keywords.count { text.contains(it) }
                if (matches > 0) {
                    mentionCount += matches
                    if (evidence.size < 3) {
                        evidence.add(text.take(50))
                    }
                }
            }
            if (mentionCount > 0) {
                val confidence = (mentionCount.toFloat() / entries.size).coerceAtMost(1f)
                result[category] = ExtractedValue(
                    category = category,
                    value = category,
                    evidence = evidence.joinToString("|"),
                    confidence = confidence
                )
            }
        }
        return result
    }

    private fun extractValues() {
        viewModelScope.launch {
            val existingValues = dao.getAllExtractedValues().first()
            if (existingValues.isNotEmpty()) return@launch
            val entries = dao.getAllEntriesOnce()
            if (entries.size < 5) return@launch

            val keywordValues = extractKeywordValues()
            keywordValues.forEach { (_, ev) ->
                dao.insertExtractedValue(ev)
            }
        }
    }
}
