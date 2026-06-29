package com.diary.app.ui.decisions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import com.diary.app.data.Decision
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DecisionStats(
    val total: Int = 0,
    val resolved: Int = 0,
    val pending: Int = 0,
    val averageSatisfaction: Float = 0f
)

class DecisionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val app = application as DiaryApplication

    private val _decisions = MutableStateFlow<List<Decision>>(emptyList())
    val decisions: StateFlow<List<Decision>> = _decisions.asStateFlow()

    private val _stats = MutableStateFlow(DecisionStats())
    val stats: StateFlow<DecisionStats> = _stats.asStateFlow()

    private val _aiInsight = MutableStateFlow<String?>(null)
    val aiInsight: StateFlow<String?> = _aiInsight.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isAiExtracting = MutableStateFlow(false)
    val isAiExtracting: StateFlow<Boolean> = _isAiExtracting.asStateFlow()

    private val _extractionProgress = MutableStateFlow(0)
    val extractionProgress: StateFlow<Int> = _extractionProgress.asStateFlow()

    private val _extractionTotal = MutableStateFlow(0)
    val extractionTotal: StateFlow<Int> = _extractionTotal.asStateFlow()

    private val _extractionMessage = MutableStateFlow<String?>(null)
    val extractionMessage: StateFlow<String?> = _extractionMessage.asStateFlow()

    private var hasScanned = false

    init {
        loadDecisions()
        viewModelScope.launch {
            val list = dao.getAllDecisions().first()
            if (list.isEmpty()) {
                scanForDecisions()
            }
        }
    }

    fun loadDecisions() {
        viewModelScope.launch {
            dao.getAllDecisions().collect { list ->
                _decisions.value = list
                computeStats(list)
            }
        }
    }

    private fun computeStats(list: List<Decision>) {
        val total = list.size
        val resolved = list.count { it.outcome != null && it.outcome.isNotBlank() }
        val pending = total - resolved
        var satSum = 0
        var satCount = 0
        for (d in list) {
            val (_, sat) = decodeOutcome(d.outcome)
            if (sat != null) {
                satSum += sat
                satCount++
            }
        }
        val avgSat = if (satCount > 0) satSum.toFloat() / satCount else 0f
        _stats.value = DecisionStats(total, resolved, pending, avgSat)
    }

    fun addRichDecision(
        title: String,
        context: String,
        options: String,
        chosenOption: String,
        concerns: String,
        followUpAt: Long?
    ) {
        viewModelScope.launch {
            dao.insertDecision(
                Decision(
                    diaryId = 0L,
                    title = title,
                    context = context,
                    options = options,
                    chosenOption = chosenOption,
                    concerns = concerns,
                    madeAt = System.currentTimeMillis(),
                    followUpAt = followUpAt
                )
            )
        }
    }

    fun updateDecisionOutcome(id: Long, outcomeText: String?, satisfaction: Int?) {
        viewModelScope.launch {
            val current = _decisions.value.find { it.id == id } ?: return@launch
            dao.updateDecision(current.copy(outcome = encodeOutcome(outcomeText, satisfaction)))
        }
    }

    fun updateDecisionFull(
        id: Long,
        title: String,
        context: String,
        options: String,
        chosenOption: String,
        concerns: String,
        followUpAt: Long?
    ) {
        viewModelScope.launch {
            val current = _decisions.value.find { it.id == id } ?: return@launch
            dao.updateDecision(
                current.copy(
                    title = title,
                    context = context,
                    options = options,
                    chosenOption = chosenOption,
                    concerns = concerns,
                    followUpAt = followUpAt
                )
            )
        }
    }

    fun deleteDecision(id: Long) {
        viewModelScope.launch {
            app.database.openHelper.writableDatabase.delete(
                "decisions",
                "id = ?",
                arrayOf(id.toString())
            )
            loadDecisions()
        }
    }

    fun analyzeDecisions() {
        if (!app.aiService.isAiEnabled()) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            _aiInsight.value = null
            val list = _decisions.value
            if (list.isEmpty()) {
                _isAnalyzing.value = false
                return@launch
            }
            val prompt = buildString {
                appendLine("以下是用户的决策记录，请分析决策模式并给出洞察和建议：\n")
                list.forEachIndexed { i, d ->
                    val (outcomeText, sat) = decodeOutcome(d.outcome)
                    appendLine("【决策${i + 1}】${d.title}")
                    appendLine("背景：${d.context.take(200)}")
                    if (d.options.isNotBlank()) {
                        val opts = d.options.split("\n").filter { it.isNotBlank() }
                        appendLine("选项：${opts.joinToString("、")}")
                    }
                    if (d.chosenOption.isNotBlank()) appendLine("选择：${d.chosenOption}")
                    if (d.concerns.isNotBlank()) appendLine("顾虑：${d.concerns.take(200)}")
                    if (outcomeText != null) appendLine("结果：${outcomeText.take(200)}")
                    if (sat != null) appendLine("满意度：${sat}/5")
                    appendLine("---")
                }
                append("\n请分析：1) 决策风格特点 2) 模式与趋势 3) 改进建议")
            }
            val result = app.aiService.chat(
                AiRequest(
                    messages = listOf(AiMessage("user", prompt)),
                    temperature = 0.7f,
                    maxTokens = 600
                )
            )
            _aiInsight.value = result.getOrNull()?.content ?: "分析失败"
            _isAnalyzing.value = false
        }
    }

    fun aiExtractDecisions() {
        if (!app.aiService.isAiEnabled()) return
        viewModelScope.launch {
            _isAiExtracting.value = true
            _extractionProgress.value = 0
            _extractionTotal.value = 0
            _extractionMessage.value = null
            val existing = dao.getAllDecisions().first()
            val existingDiaryIds = existing.map { it.diaryId }.toSet()
            val entries = dao.getAllEntriesOnce()
                .filter { it.id !in existingDiaryIds && it.plainText.length > 50 }
                .take(10)

            val total = entries.size
            _extractionTotal.value = total
            var successCount = 0
            var skipCount = 0

            for ((index, entry) in entries.withIndex()) {
                _extractionProgress.value = ((index + 1) * 100) / maxOf(total, 1)
                val prompt = """从以下日记中提取决策信息（如果没有决策相关信息，回复"无"）：
日记：${entry.plainText.take(500)}

如果有决策信息，请按如下格式回复：
标题：...
背景：...
选项：...
选择：...
顾虑：..."""
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(AiMessage("user", prompt)),
                        temperature = 0.3f,
                        maxTokens = 300
                    )
                )
                val content = result.getOrNull()?.content?.trim()
                if (content == null || content == "无" || content.length < 10) {
                    skipCount++
                    continue
                }

                val title = extractLine(content, "标题") ?: entry.title.take(30) + "..."
                val context = extractLine(content, "背景") ?: entry.plainText.take(200)
                val options = extractLine(content, "选项") ?: ""
                val chosen = extractLine(content, "选择") ?: ""
                val concerns = extractLine(content, "顾虑") ?: ""

                dao.insertDecision(
                    Decision(
                        diaryId = entry.id,
                        title = title,
                        context = context,
                        options = options,
                        chosenOption = chosen,
                        concerns = concerns,
                        madeAt = entry.createdAt
                    )
                )
                successCount++
            }
            _extractionProgress.value = 100
            val message = "扫描完成：找到 ${successCount} 个决策，跳过 ${skipCount} 篇"
            _extractionMessage.value = message
            _isAiExtracting.value = false
            delay(5000)
            _extractionMessage.value = null
        }
    }

    private suspend fun scanForDecisions() {
        if (hasScanned) return
        hasScanned = true
        val entries = dao.getAllEntriesOnce()
        val existing = dao.getAllDecisions().first()
        val existingDiaryIds = existing.map { it.diaryId }.toSet()
        val decisionKeywords = listOf("决定", "选择", "考虑", "纠结", "最终", "下定决心", "想了很久")
        entries.forEach { entry ->
            if (entry.id in existingDiaryIds) return@forEach
            val text = entry.plainText
            if (text.isBlank()) return@forEach
            val matchedKeyword = decisionKeywords.firstOrNull { text.contains(it) }
            if (matchedKeyword != null && text.length > 50) {
                dao.insertDecision(
                    Decision(
                        diaryId = entry.id,
                        title = text.take(30).replace("\n", " ") + "...",
                        context = text.take(200),
                        madeAt = entry.createdAt
                    )
                )
            }
        }
    }

    private fun extractLine(text: String, prefix: String): String? {
        val lines = text.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith(prefix)) {
                return trimmed.removePrefix(prefix)
                    .trimStart('：', ':').trim()
                    .takeIf { it.isNotBlank() }
            }
        }
        return null
    }
}

fun encodeOutcome(text: String?, satisfaction: Int?): String? {
    if (text == null && satisfaction == null) return null
    if (satisfaction == null) return text
    val map = mapOf("t" to (text ?: ""), "s" to satisfaction)
    return Gson().toJson(map)
}

fun decodeOutcome(outcome: String?): Pair<String?, Int?> {
    if (outcome == null) return null to null
    return try {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = Gson().fromJson(outcome, type)
        map["t"] as? String to (map["s"] as? Double)?.toInt()
    } catch (_: Exception) {
        outcome to null
    }
}
