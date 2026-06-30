package com.diary.app.ui.writinglab

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryEntry
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

data class WritingLabOverview(
    val activeExperimentProgressText: String,
    val completedExperimentsText: String,
    val practiceLoopsText: String,
    val nextStepTitle: String,
    val nextStepDescription: String,
    val fallbackCoverageText: String
)

enum class WritingLabTab { EXPERIMENTS, STYLE_TRANSFER, CHALLENGES, RHETORICAL, TEMPLATES }

fun buildWritingLabOverview(
    activeExperiment: com.diary.app.data.WritingExperiment?,
    participations: List<ExperimentParticipation>,
    completedExperiments: List<com.diary.app.data.WritingExperiment>,
    styleHistory: List<WritingExperiment>,
    challengeStreak: Int,
    completedChallenges: Int,
    rhetoricalSuggestions: List<RhetoricalSuggestion>,
    templates: List<CreativeTemplate>
): WritingLabOverview {
    val activeTotalDays = activeExperiment?.let {
        experimentPresets.find { preset -> preset.title == it.title }?.days ?: 7
    } ?: 0
    val activeProgress = if (activeTotalDays > 0) "${participations.size}/$activeTotalDays 天" else "暂无进行中的实验"
    val loopParts = buildList {
        if (styleHistory.isNotEmpty()) add("风格转换 ${styleHistory.size} 次")
        if (challengeStreak > 0 || completedChallenges > 0) add("挑战连胜 $challengeStreak 天，累计 $completedChallenges 次")
        if (rhetoricalSuggestions.isNotEmpty()) add("修辞建议 ${rhetoricalSuggestions.size} 条")
        if (templates.isNotEmpty()) add("模板库 ${templates.size} 个")
    }
    val practiceLoops = if (loopParts.isNotEmpty()) loopParts.joinToString(" · ") else "还没有形成闭环，先完成一个实验或一次转换"
    val nextStepTitle = when {
        activeExperiment != null && participations.size < activeTotalDays -> "继续当前实验"
        styleHistory.isEmpty() -> "先完成一次风格转换"
        rhetoricalSuggestions.isEmpty() -> "先做一次修辞分析"
        else -> "把结果写回日记"
    }
    val nextStepDescription = when {
        activeExperiment != null && participations.size < activeTotalDays ->
            "今天补一条 ${activeExperiment.title} 记录，完成 $activeProgress。"
        styleHistory.isEmpty() ->
            "输入一段 50 到 120 字的文字，先把改写结果保存下来。"
        rhetoricalSuggestions.isEmpty() ->
            "贴上一段最近写过的内容，先看见可优化的句子。"
        else ->
            "选一个练习结果，立刻把它用到下一篇日记里。"
    }
    return WritingLabOverview(
        activeExperimentProgressText = activeProgress,
        completedExperimentsText = "已完成 ${completedExperiments.size} 个实验",
        practiceLoopsText = practiceLoops,
        nextStepTitle = nextStepTitle,
        nextStepDescription = nextStepDescription,
        fallbackCoverageText = "AI 关闭时仍可使用：风格转换、写作挑战、修辞建议、创意模板"
    )
}

fun buildLocalStyleTransferResult(originalText: String, style: String): String {
    val trimmed = originalText.trim()
    val sample = buildStyleSample(trimmed, style)
    val guidance = when (style) {
        "鲁迅风格" -> "保留判断力，语气更锋利一点。"
        "张爱玲风格" -> "把细节写得更具体，情绪更含蓄。"
        "村上春树风格" -> "用安静、克制的节奏推动句子。"
        "古诗风格" -> "尝试用分行和意象组织语句。"
        "简洁风格" -> "删掉重复表达，只留下最重要的信息。"
        "华丽风格" -> "给句子加一点画面感和转折。"
        else -> "先保留原意，再调整语气和节奏。"
    }
    return buildString {
        appendLine("${style}练习版")
        appendLine("改写提示：$guidance")
        appendLine("参考草稿：")
        appendLine(sample)
        appendLine()
        appendLine("你可以继续补一版更贴近自己语气的改写。")
    }.trim()
}

fun buildLocalChallenge(entries: List<DiaryEntry>): WritingChallenge {
    val recentText = entries.firstOrNull()?.plainText?.trim().orEmpty()
    val keyword = recentText.split(Regex("\\s+")).firstOrNull { it.length >= 2 }
        ?: recentText.takeIf { it.isNotBlank() }?.take(6)
        ?: "今天"
    val (text, reason) = when {
        recentText.length >= 80 -> {
            "用三段话重写今天的内容：事实、感受、想法" to "先把记录拆成三层，帮助你练习结构"
        }
        recentText.isNotBlank() -> {
            "围绕「$keyword」写一段 120 字的小记" to "用一个具体线索扩展观察"
        }
        else -> {
            "今天写 200 字，描述一个你忽略的小细节" to "没有历史输入时，先练习细节观察"
        }
    }
    return WritingChallenge(text, reason, System.currentTimeMillis())
}

fun buildLocalRhetoricalSuggestions(text: String): List<RhetoricalSuggestion> {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return emptyList()
    val sentenceCount = trimmed.split(Regex("[。！？!?\\n]+")).count { it.isNotBlank() }
    val commaCount = trimmed.count { it == '，' || it == ',' }
    val suggestions = mutableListOf<RhetoricalSuggestion>()
    if (trimmed.length < 80) {
        suggestions.add(
            RhetoricalSuggestion(
                type = "词汇建议",
                text = "这段文字偏短，可以补一个感官细节。",
                originalText = trimmed.take(30),
                suggestion = "加入一个看到、听到或触到的具体细节，让画面更立体。"
            )
        )
    }
    if (sentenceCount <= 2 || commaCount >= 4) {
        suggestions.add(
            RhetoricalSuggestion(
                type = "结构建议",
                text = "这段文字适合拆成更清楚的层次。",
                originalText = trimmed.take(30),
                suggestion = "把事实、感受和结论分成 2 到 3 句来写。"
            )
        )
    }
    if (trimmed.contains("然后") || trimmed.contains("但是") || trimmed.contains("所以")) {
        suggestions.add(
            RhetoricalSuggestion(
                type = "修辞建议",
                text = "转折词很多时，可以用更具体的动作推进。",
                originalText = trimmed.take(30),
                suggestion = "把抽象的连接词换成动作或画面，节奏会更自然。"
            )
        )
    }
    if (suggestions.isEmpty()) {
        suggestions.add(
            RhetoricalSuggestion(
                type = "修辞建议",
                text = "这段文字已经顺畅，可以尝试替换一个动词。",
                originalText = trimmed.take(30),
                suggestion = "把最常用的动词换成更具体的表达，提升画面感。"
            )
        )
    }
    return suggestions.take(3)
}

fun buildLocalTemplateFallback(): List<CreativeTemplate> = listOf(
    CreativeTemplate("t1", "感官日记", "用五感描述你今天的环境", "我看到窗外的树影摇曳，听到远处传来的汽车声...", "感官日记"),
    CreativeTemplate("t2", "对话日记", "以对话形式记录今天的交流", "A: 今天过得怎么样？ B: 还不错，今天完成了一个项目。", "对话日记"),
    CreativeTemplate("t3", "倒叙日记", "从今晚开始倒着写到今早", "此刻躺在床上，回想今天发生的一切...", "倒叙日记"),
    CreativeTemplate("t4", "诗歌日记", "用诗的形式记录今天", "清晨的一缕光 / 照亮了书桌一角 / 新的一天开始了", "诗歌日记")
)

private fun buildStyleSample(originalText: String, style: String): String {
    val compact = originalText.replace(Regex("\\s+"), " ").trim()
    if (compact.isBlank()) return "请先输入一段原文，再开始练习。"
    return when (style) {
        "鲁迅风格" -> "事情并没有结束。$compact 只是开始。"
        "张爱玲风格" -> "那一刻，$compact，像旧窗上的一层薄光。"
        "村上春树风格" -> "然后，$compact。风从门缝里轻轻经过。"
        "古诗风格" -> "晨光入纸，$compact，句句可成章。"
        "简洁风格" -> compact.split(Regex("[。！？!?]+")).firstOrNull { it.isNotBlank() }?.take(80)?.plus("。")
            ?: compact.take(80)
        "华丽风格" -> "在这一段里，$compact，像一束慢慢展开的光。"
        else -> compact.take(120)
    }
}

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
        if (!aiService.isAiEnabled()) {
            ensureLocalFallbackContent()
        }
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
            val style = _selectedStyle.value
            if (!aiService.isAiEnabled()) {
                val fallback = buildLocalStyleTransferResult(text, style)
                _styleResult.value = fallback
                saveStyleRecord(text, fallback, style, source = "local")
                _isStyleLoading.value = false
                return@launch
            }
            val systemPrompt = "你是一个写作风格转换助手。将以下内容改写成${style}风格，保持原意不变。只返回改写后的内容，不要加解释。"
            val request = aiRequest(userMessage = text, systemPrompt = systemPrompt, temperature = 0.7f, maxTokens = 512)
            try {
                val result = aiService.chat(request)
                val content = result.getOrNull()?.content?.trim()
                if (content != null) {
                    _styleResult.value = content
                    saveStyleRecord(text, content, style, source = "ai")
                } else {
                    val fallback = buildLocalStyleTransferResult(text, style)
                    _styleResult.value = fallback
                    saveStyleRecord(text, fallback, style, source = "local")
                }
            } catch (_: Exception) {
                val fallback = buildLocalStyleTransferResult(text, style)
                _styleResult.value = fallback
                saveStyleRecord(text, fallback, style, source = "local")
            }
            _isStyleLoading.value = false
        }
    }

    fun clearStyleResult() {
        _styleResult.value = null
        _aiInputText.value = ""
        _currentRating.value = null
    }

    private fun saveStyleRecord(original: String, result: String, style: String, source: String) {
        val record = WritingExperiment(
            id = System.currentTimeMillis().toString(),
            type = "style_transfer",
            originalText = original,
            resultText = result,
            metadata = mapOf("style" to style, "source" to source),
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
            val recentEntries = dao.getAllEntries().first().take(10)
            if (!aiService.isAiEnabled()) {
                _currentChallenge.value = buildLocalChallenge(recentEntries)
                _isChallengeLoading.value = false
                return@launch
            }
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
                        _currentChallenge.value = buildLocalChallenge(recentEntries)
                    }
                }
            } catch (_: Exception) {
                _currentChallenge.value = buildLocalChallenge(recentEntries)
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
            if (!aiService.isAiEnabled()) {
                val fallback = buildLocalRhetoricalSuggestions(text)
                _rhetoricalSuggestions.value = fallback
                _showRhetoricalDots.value = fallback.isNotEmpty()
                _isRhetoricalLoading.value = false
                return@launch
            }

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
                        _rhetoricalSuggestions.value = buildLocalRhetoricalSuggestions(text)
                    }
                    _showRhetoricalDots.value = _rhetoricalSuggestions.value.isNotEmpty()
                }
            } catch (_: Exception) {
                val fallback = buildLocalRhetoricalSuggestions(text)
                _rhetoricalSuggestions.value = fallback
                _showRhetoricalDots.value = fallback.isNotEmpty()
            }
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

    private fun ensureLocalFallbackContent() {
        if (_templates.value.isEmpty()) {
            loadFallbackTemplates()
        }
        if (_currentChallenge.value == null) {
            _currentChallenge.value = buildLocalChallenge(emptyList())
        }
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
