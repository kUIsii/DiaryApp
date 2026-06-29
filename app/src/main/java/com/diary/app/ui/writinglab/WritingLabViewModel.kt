package com.diary.app.ui.writinglab

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest
import com.diary.app.data.ExperimentParticipation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ExperimentPreset(val title: String, val description: String, val rules: String, val badgeName: String, val days: Int)

val experimentPresets = listOf(
    ExperimentPreset("三句话日记", "每天只用三句话记录今天", "严格限制三句话，不超过150字", "精炼笔者", 7),
    ExperimentPreset("感恩日记", "每天记录三件感恩的事", "写下今天让你感恩的三件事，可长可短", "感恩之心", 7),
    ExperimentPreset("观察日记", "每天仔细观察一个事物", "描述你今天注意到的一个细节：一片叶子、一个人的表情、一杯茶的颜色", "敏锐之眼", 5),
    ExperimentPreset("情绪日记", "记录今天的情绪波动", "今天经历了哪些情绪？什么触发了它们？对自己诚实。", "情绪洞察", 7),
    ExperimentPreset("回忆日记", "每天回忆一个过去的片段", "写下一段过去的记忆，可以是很久以前的，也可以是昨天的", "时光旅人", 5)
)

val styleTransferOptions = listOf(
    "鲁迅风格", "张爱玲风格", "村上春树风格", "古诗风格", "简洁风格", "华丽风格"
)

data class WritingExperiment(
    val id: String, val type: String, val originalText: String?,
    val resultText: String?, val metadata: Map<String, String>,
    val rating: Int?, val createdAt: Long
)

data class WritingChallenge(
    val text: String, val reason: String, val createdAt: Long
)

data class RhetoricalSuggestion(
    val type: String, val text: String, val originalText: String?,
    val suggestion: String, val isApplied: Boolean = false
)

data class CreativeTemplate(
    val id: String, val title: String, val description: String,
    val example: String, val type: String
)

enum class WritingLabTab { EXPERIMENTS, STYLE_TRANSFER, CHALLENGES, RHETORICAL, TEMPLATES }

class WritingLabViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = AiServiceManager(application)
    private val gson = Gson()
    private val prefs = application.getSharedPreferences("writinglab", Context.MODE_PRIVATE)

    private val _activeExperiment = MutableStateFlow<com.diary.app.data.WritingExperiment?>(null)
    val activeExperiment: StateFlow<com.diary.app.data.WritingExperiment?> = _activeExperiment.asStateFlow()

    private val _participations = MutableStateFlow<List<ExperimentParticipation>>(emptyList())
    val participations: StateFlow<List<ExperimentParticipation>> = _participations.asStateFlow()

    private val _completedExperiments = MutableStateFlow<List<com.diary.app.data.WritingExperiment>>(emptyList())
    val completedExperiments: StateFlow<List<com.diary.app.data.WritingExperiment>> = _completedExperiments.asStateFlow()

    private val _showPresetPicker = MutableStateFlow(false)
    val showPresetPicker: StateFlow<Boolean> = _showPresetPicker.asStateFlow()

    private val _currentTab = MutableStateFlow(WritingLabTab.EXPERIMENTS)
    val currentTab: StateFlow<WritingLabTab> = _currentTab.asStateFlow()

    // Style Transfer
    private val _aiInputText = MutableStateFlow("")
    val aiInputText: StateFlow<String> = _aiInputText.asStateFlow()

    private val _selectedStyle = MutableStateFlow(styleTransferOptions[0])
    val selectedStyle: StateFlow<String> = _selectedStyle.asStateFlow()

    private val _styleResult = MutableStateFlow<String?>(null)
    val styleResult: StateFlow<String?> = _styleResult.asStateFlow()

    private val _isStyleLoading = MutableStateFlow(false)
    val isStyleLoading: StateFlow<Boolean> = _isStyleLoading.asStateFlow()

    private val _currentRating = MutableStateFlow<Int?>(null)
    val currentRating: StateFlow<Int?> = _currentRating.asStateFlow()

    private val _styleHistory = MutableStateFlow<List<WritingExperiment>>(emptyList())
    val styleHistory: StateFlow<List<WritingExperiment>> = _styleHistory.asStateFlow()

    // Writing Challenge
    private val _currentChallenge = MutableStateFlow<WritingChallenge?>(null)
    val currentChallenge: StateFlow<WritingChallenge?> = _currentChallenge.asStateFlow()

    private val _challengeStreak = MutableStateFlow(0)
    val challengeStreak: StateFlow<Int> = _challengeStreak.asStateFlow()

    private val _isChallengeLoading = MutableStateFlow(false)
    val isChallengeLoading: StateFlow<Boolean> = _isChallengeLoading.asStateFlow()

    private val _completedChallenges = MutableStateFlow(0)
    val completedChallenges: StateFlow<Int> = _completedChallenges.asStateFlow()

    // Rhetorical Suggestions
    private val _rhetoricalInput = MutableStateFlow("")
    val rhetoricalInput: StateFlow<String> = _rhetoricalInput.asStateFlow()

    private val _rhetoricalSuggestions = MutableStateFlow<List<RhetoricalSuggestion>>(emptyList())
    val rhetoricalSuggestions: StateFlow<List<RhetoricalSuggestion>> = _rhetoricalSuggestions.asStateFlow()

    private val _isRhetoricalLoading = MutableStateFlow(false)
    val isRhetoricalLoading: StateFlow<Boolean> = _isRhetoricalLoading.asStateFlow()

    private val _showRhetoricalDots = MutableStateFlow(false)
    val showRhetoricalDots: StateFlow<Boolean> = _showRhetoricalDots.asStateFlow()

    private val _expandedSuggestionIndex = MutableStateFlow<Int?>(null)
    val expandedSuggestionIndex: StateFlow<Int?> = _expandedSuggestionIndex.asStateFlow()

    // Creative Templates
    private val _templates = MutableStateFlow<List<CreativeTemplate>>(emptyList())
    val templates: StateFlow<List<CreativeTemplate>> = _templates.asStateFlow()

    private val _isTemplateLoading = MutableStateFlow(false)
    val isTemplateLoading: StateFlow<Boolean> = _isTemplateLoading.asStateFlow()

    init {
        loadExperiments()
        loadStyleHistory()
        loadChallengeState()
        loadTemplates()
    }

    fun setTab(tab: WritingLabTab) { _currentTab.value = tab }

    // Style Transfer
    fun setAiInputText(text: String) { _aiInputText.value = text }
    fun setSelectedStyle(style: String) { _selectedStyle.value = style }

    fun setRating(rating: Int?) {
        _currentRating.value = rating
        val history = _styleHistory.value.toMutableList()
        if (history.isNotEmpty()) {
            history[0] = history[0].copy(rating = rating)
            _styleHistory.value = history
            prefs.edit().putString("style_history", gson.toJson(history)).apply()
        }
    }

    fun performStyleTransfer() {
        val text = _aiInputText.value.trim()
        if (text.isEmpty()) return
        _isStyleLoading.value = true
        _styleResult.value = null
        _currentRating.value = null

        viewModelScope.launch {
            if (!aiService.isAiEnabled()) {
                _isStyleLoading.value = false
                return@launch
            }
            val style = _selectedStyle.value
            val systemPrompt = "你是一个写作风格转换助手。将以下内容改写成${style}风格，保持原意不变。只返回改写后的内容，不要加解释。"
            val request = aiRequest(userMessage = text, systemPrompt = systemPrompt, temperature = 0.7f, maxTokens = 512)
            try {
                val result = aiService.chat(request)
                val content = result.getOrNull()?.content?.trim()
                if (content != null) {
                    _styleResult.value = content
                    saveStyleRecord(text, content, style)
                }
            } catch (_: Exception) { }
            _isStyleLoading.value = false
        }
    }

    fun clearStyleResult() {
        _styleResult.value = null
        _aiInputText.value = ""
        _currentRating.value = null
    }

    private fun saveStyleRecord(original: String, result: String, style: String) {
        val record = WritingExperiment(
            id = System.currentTimeMillis().toString(),
            type = "style_transfer",
            originalText = original,
            resultText = result,
            metadata = mapOf("style" to style),
            rating = null,
            createdAt = System.currentTimeMillis()
        )
        val history = _styleHistory.value.toMutableList()
        history.add(0, record)
        _styleHistory.value = history
        prefs.edit().putString("style_history", gson.toJson(history)).apply()
    }

    private fun loadStyleHistory() {
        val json = prefs.getString("style_history", null) ?: return
        try {
            val type = object : TypeToken<List<WritingExperiment>>() {}.type
            _styleHistory.value = gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { }
    }

    // Writing Challenge
    fun generateChallenge() {
        _isChallengeLoading.value = true
        viewModelScope.launch {
            if (!aiService.isAiEnabled()) {
                _currentChallenge.value = WritingChallenge(
                    "今天用 200 字描述窗外的声音", "尝试关注听觉细节", System.currentTimeMillis()
                )
                _isChallengeLoading.value = false
                return@launch
            }
            val recentEntries = dao.getAllEntries().first().take(10)
            val entriesText = if (recentEntries.isNotEmpty()) {
                recentEntries.joinToString("\n") { it.plainText.take(100) }
            } else "暂无近期日记"

            val systemPrompt = """你是一个写作挑战生成器。基于用户的写作历史，生成一个个性化的写作挑战。
用户的近期内容:
$entriesText

分析用户的写作习惯（是否缺乏感官描述、是否总是用第一人称、是否用词单一等），然后生成一个针对性的挑战。
请用以下JSON格式返回:
{"text": "挑战内容", "reason": "生成理由"}"""
            val request = aiRequest(userMessage = "请根据我的写作习惯生成一个写作挑战。", systemPrompt = systemPrompt, temperature = 0.8f, maxTokens = 256)
            try {
                val result = aiService.chat(request)
                val content = result.getOrNull()?.content?.trim()
                if (content != null) {
                    try {
                        val json = gson.fromJson(content, Map::class.java)
                        val text = json["text"] as? String ?: "用新的方式写今天的日记"
                        val reason = json["reason"] as? String ?: "尝试不同的写作视角"
                        _currentChallenge.value = WritingChallenge(text, reason, System.currentTimeMillis())
                    } catch (_: Exception) {
                        _currentChallenge.value = WritingChallenge("今天用 200 字描述窗外的声音", "尝试关注听觉细节", System.currentTimeMillis())
                    }
                }
            } catch (_: Exception) {
                _currentChallenge.value = WritingChallenge("用第三人称写今天的事", "尝试不同的叙述视角", System.currentTimeMillis())
            }
            _isChallengeLoading.value = false
        }
    }

    fun completeChallenge() {
        _challengeStreak.value++
        _completedChallenges.value++
        saveChallengeState()
        _currentChallenge.value = null
    }

    fun skipChallenge() {
        _challengeStreak.value = 0
        saveChallengeState()
        _currentChallenge.value = null
    }

    private fun saveChallengeState() {
        val data = mapOf("streak" to _challengeStreak.value, "total" to _completedChallenges.value)
        prefs.edit().putString("challenge_state", gson.toJson(data)).apply()
    }

    private fun loadChallengeState() {
        val json = prefs.getString("challenge_state", null) ?: return
        try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val data: Map<String, Int> = gson.fromJson(json, type)
            _challengeStreak.value = data["streak"] ?: 0
            _completedChallenges.value = data["total"] ?: 0
        } catch (_: Exception) { }
    }

    // Rhetorical Suggestions
    fun setRhetoricalInput(text: String) { _rhetoricalInput.value = text }
    fun setExpandedSuggestionIndex(index: Int?) { _expandedSuggestionIndex.value = index }

    fun analyzeRhetorical() {
        val text = _rhetoricalInput.value.trim()
        if (text.isEmpty()) return
        _isRhetoricalLoading.value = true
        _rhetoricalSuggestions.value = emptyList()
        _showRhetoricalDots.value = false
        _expandedSuggestionIndex.value = null

        viewModelScope.launch {
            if (!aiService.isAiEnabled()) return@launch

            val systemPrompt = """你是一个写作助手。分析以下段落，给出修辞、结构和词汇方面的改进建议。
请严格按以下JSON数组格式返回，最多返回3条:
[{"type": "修辞建议", "text": "建议内容", "originalText": "原文片段", "suggestion": "修改建议"}]
type只能是"修辞建议"、"结构建议"或"词汇建议"之一。"""
            val request = aiRequest(userMessage = text, systemPrompt = systemPrompt, temperature = 0.5f, maxTokens = 512)
            try {
                val result = aiService.chat(request)
                val content = result.getOrNull()?.content?.trim()
                if (content != null) {
                    try {
                        val type = object : TypeToken<List<Map<String, String>>>() {}.type
                        val list: List<Map<String, String>> = gson.fromJson(content, type)
                        _rhetoricalSuggestions.value = list.map {
                            RhetoricalSuggestion(
                                type = it["type"] ?: "修辞建议",
                                text = it["text"] ?: "",
                                originalText = it["originalText"],
                                suggestion = it["suggestion"] ?: ""
                            )
                        }
                    } catch (_: Exception) {
                        _rhetoricalSuggestions.value = listOf(
                            RhetoricalSuggestion("修辞建议", "这里可以用比喻增强表现力", null, "尝试添加一个生动的比喻"),
                            RhetoricalSuggestion("结构建议", "这一段可以分两段", null, "分开后逻辑更清晰"),
                            RhetoricalSuggestion("词汇建议", "'快乐'用了三次，换'愉悦''开心'", null, "使用近义词丰富表达")
                        )
                    }
                    _showRhetoricalDots.value = _rhetoricalSuggestions.value.isNotEmpty()
                }
            } catch (_: Exception) { }
            _isRhetoricalLoading.value = false
        }
    }

    fun applySuggestion(index: Int) {
        val suggestions = _rhetoricalSuggestions.value.toMutableList()
        if (index in suggestions.indices) {
            suggestions[index] = suggestions[index].copy(isApplied = true)
            _rhetoricalSuggestions.value = suggestions
        }
    }

    fun dismissSuggestion(index: Int) {
        val suggestions = _rhetoricalSuggestions.value.toMutableList()
        if (index in suggestions.indices) {
            suggestions.removeAt(index)
            _rhetoricalSuggestions.value = suggestions
            if (suggestions.isEmpty()) _showRhetoricalDots.value = false
        }
    }

    // Creative Templates
    fun generateTemplates() {
        _isTemplateLoading.value = true
        viewModelScope.launch {
            if (!aiService.isAiEnabled()) {
                loadFallbackTemplates()
                _isTemplateLoading.value = false
                return@launch
            }
            val recentEntries = dao.getAllEntries().first().take(20)
            val entriesText = if (recentEntries.isNotEmpty()) {
                recentEntries.joinToString("\n---\n") {
                    "日期: ${it.createdAt}\n内容: ${it.plainText.take(150)}"
                }
            } else "暂无近期日记"

            val systemPrompt = """你是一个创意写作模板生成器。基于用户的日记历史，生成4个创意写作模板。
每个模板要结合用户的写作特点，并从一个过往日记中生成示例。

用户的近期日记:
$entriesText

请严格按以下JSON数组格式返回:
[{"title": "模板标题", "description": "模板说明", "example": "基于用户日记的示例内容", "type": "类型"}]
类型必须是: 感官日记, 对话日记, 倒叙日记, 诗歌日记 之一。"""
            val request = aiRequest(userMessage = "请根据我的日记生成创意写作模板。", systemPrompt = systemPrompt, temperature = 0.8f, maxTokens = 800)
            try {
                val result = aiService.chat(request)
                val content = result.getOrNull()?.content?.trim()
                if (content != null) {
                    try {
                        val type = object : TypeToken<List<Map<String, String>>>() {}.type
                        val list: List<Map<String, String>> = gson.fromJson(content, type)
                        _templates.value = list.mapIndexed { i, m ->
                            CreativeTemplate(
                                id = "template_$i",
                                title = m["title"] ?: "",
                                description = m["description"] ?: "",
                                example = m["example"] ?: "",
                                type = m["type"] ?: "感官日记"
                            )
                        }
                        saveTemplates()
                    } catch (_: Exception) {
                        loadFallbackTemplates()
                    }
                }
            } catch (_: Exception) {
                loadFallbackTemplates()
            }
            _isTemplateLoading.value = false
        }
    }

    private fun loadFallbackTemplates() {
        _templates.value = listOf(
            CreativeTemplate("t1", "感官日记", "用五感描述你今天的环境", "我看到窗外的树影摇曳，听到远处传来的汽车声...", "感官日记"),
            CreativeTemplate("t2", "对话日记", "以对话形式记录今天的交流", "A: 今天过得怎么样？ B: 还不错，今天完成了一个项目。", "对话日记"),
            CreativeTemplate("t3", "倒叙日记", "从今晚开始倒着写到今早", "此刻躺在床上，回想今天发生的一切...", "倒叙日记"),
            CreativeTemplate("t4", "诗歌日记", "用诗的形式记录今天", "清晨的一缕光 / 照亮了书桌一角 / 新的一天开始了", "诗歌日记")
        )
    }

    private fun saveTemplates() {
        prefs.edit().putString("templates", gson.toJson(_templates.value)).apply()
    }

    private fun loadTemplates() {
        val json = prefs.getString("templates", null) ?: return
        try {
            val type = object : TypeToken<List<CreativeTemplate>>() {}.type
            _templates.value = gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { }
    }

    // Existing methods
    fun loadExperiments() {
        viewModelScope.launch {
            dao.getAllWritingExperiments().collect { experiments ->
                val active = experiments.firstOrNull { it.status == "active" }
                _activeExperiment.value = active
                _completedExperiments.value = experiments.filter { it.status == "completed" || it.status == "expired" }
                if (active != null) {
                    _participations.value = dao.getExperimentParticipations(active.id).first()
                }
                _showPresetPicker.value = experiments.none { it.status == "active" || it.status == "upcoming" }
            }
        }
    }

    fun startExperiment(preset: ExperimentPreset) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.insertWritingExperiment(com.diary.app.data.WritingExperiment(
                title = preset.title, description = preset.description,
                rules = preset.rules, badgeName = preset.badgeName,
                startDate = now, endDate = now + preset.days * 24 * 60 * 60 * 1000L,
                status = "active"
            ))
            _showPresetPicker.value = false
        }
    }

    fun logParticipation(note: String) {
        viewModelScope.launch {
            val exp = _activeExperiment.value ?: return@launch
            val dayNum = _participations.value.size + 1
            dao.insertExperimentParticipation(ExperimentParticipation(
                experimentId = exp.id, diaryId = null,
                dayNumber = dayNum, note = note
            ))
            if (dayNum >= (experimentPresets.find { it.title == exp.title }?.days ?: 7)) {
                val updated = dao.getActiveWritingExperiment()?.takeIf { it.id == exp.id }
                if (updated != null) {
                    dao.updateWritingExperiment(updated.copy(status = "completed", completedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun dismissPresetPicker() { _showPresetPicker.value = false }
}
