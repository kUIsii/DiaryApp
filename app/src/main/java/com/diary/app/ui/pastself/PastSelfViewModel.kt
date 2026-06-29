package com.diary.app.ui.pastself

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PastSelfSession(
    val id: String,
    val focusPeriodStart: Long,
    val focusPeriodEnd: Long,
    val topic: String? = null,
    val observations: List<AIObservation> = emptyList(),
    val debateConfig: DebateConfig? = null,
    val letters: List<TimeLetter> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class AIObservation(
    val id: String,
    val content: String,
    val sourceEntries: List<Long> = emptyList(),
    val type: String = "observation"
)

data class TimeLetter(
    val id: String,
    val direction: String,
    val content: String,
    val sourcePeriodStart: Long = 0L,
    val sourcePeriodEnd: Long = 0L,
    val userEdits: List<String> = emptyList()
)

data class DebateConfig(
    val period1Start: Long,
    val period1End: Long,
    val period2Start: Long,
    val period2End: Long,
    val topic: String
)

data class GrowthPoint(
    val date: Long,
    val label: String,
    val intensity: Float
)

data class GrowthTopic(
    val topic: String,
    val points: List<GrowthPoint>,
    val trend: String
)

class PastSelfViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val gson = Gson()
    private val prefs = application.getSharedPreferences("past_self_sessions", Application.MODE_PRIVATE)

    private val _observations = MutableStateFlow<List<AIObservation>>(emptyList())
    val observations: StateFlow<List<AIObservation>> = _observations.asStateFlow()

    private val _debateMode = MutableStateFlow(false)
    val debateMode: StateFlow<Boolean> = _debateMode.asStateFlow()

    private val _letters = MutableStateFlow<List<TimeLetter>>(emptyList())
    val letters: StateFlow<List<TimeLetter>> = _letters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSession = MutableStateFlow<PastSelfSession?>(null)
    val currentSession: StateFlow<PastSelfSession?> = _currentSession.asStateFlow()

    private val _growthTopics = MutableStateFlow<List<GrowthTopic>>(emptyList())
    val growthTopics: StateFlow<List<GrowthTopic>> = _growthTopics.asStateFlow()

    private val _growthLoading = MutableStateFlow(false)
    val growthLoading: StateFlow<Boolean> = _growthLoading.asStateFlow()

    private val _selectedPeriod = MutableStateFlow<Pair<Long, Long>?>(null)
    val selectedPeriod: StateFlow<Pair<Long, Long>?> = _selectedPeriod.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) { prefs.getString("current_session", null) }
            if (json != null) {
                try {
                    val session = gson.fromJson(json, PastSelfSession::class.java)
                    if (session.createdAt > System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) {
                        _currentSession.value = session
                        _observations.value = session.observations
                        _letters.value = session.letters
                        _debateMode.value = session.debateConfig != null
                        _selectedPeriod.value = session.focusPeriodStart to session.focusPeriodEnd
                    }
                } catch (e: Exception) {
                    Log.w("PastSelfViewModel", "Failed to restore session", e)
                }
            }
        }
    }

    private fun saveSession() {
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            val updated = session.copy(
                observations = _observations.value,
                letters = _letters.value
            )
            _currentSession.value = updated
            withContext(Dispatchers.IO) {
                prefs.edit().putString("current_session", gson.toJson(updated)).apply()
            }
        }
    }

    fun selectPeriod(start: Long, end: Long) {
        _selectedPeriod.value = start to end
        val session = PastSelfSession(
            id = UUID.randomUUID().toString(),
            focusPeriodStart = start,
            focusPeriodEnd = end
        )
        _currentSession.value = session
        _observations.value = emptyList()
        _letters.value = emptyList()
        _debateMode.value = false
        generateInitialObservation(start, end)
    }

    private fun generateInitialObservation(start: Long, end: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { dao.getEntriesByDateRange(start, end) }
            if (entries.isEmpty()) {
                _observations.value = listOf(
                    AIObservation(
                        id = UUID.randomUUID().toString(),
                        content = "这段时间没有找到日记记录。也许是一个安静的时期——你愿意聊聊那段时间发生了什么吗？",
                        type = "observation"
                    )
                )
                _isLoading.value = false
                return@launch
            }

            val dateFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
            val entriesSummary = entries.take(30).joinToString("\n") { e ->
                "【${e.id}】${dateFmt.format(Date(e.createdAt))}: ${e.title}\n${e.plainText.take(200)}"
            }

            val systemPrompt = """你是一个基于日记的主动观察AI。用户选择了某个时间段，你阅读了这些日记后，发起一段有洞察力的观察，而不是等待用户提问。

要求：
- 以"我翻看了那段时间的日记"或类似语气开头，自然引出观察
- 观察必须具体引用日记中的内容，在提及处标注【条目ID】
- 不要笼统概括，要指出具体的矛盾、反复出现的主题、情绪变化或未解决的疑问
- 如果发现某个话题戛然而止，可以说"你提到xxx但没往下说"
- 语气温和、有同理心，像朋友在翻看旧日记后说的话
- 回复长度适中，200-400字
- 最后可以自然地问一个开放性问题

时间范围内的日记：
$entriesSummary"""

            try {
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(AiMessage("system", systemPrompt)),
                        maxTokens = 1024,
                        temperature = 0.8f
                    ),
                    useCache = false
                )
                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    val sourceIds = entries.take(10).map { it.id }
                    _observations.value = listOf(
                        AIObservation(
                            id = UUID.randomUUID().toString(),
                            content = reply,
                            sourceEntries = sourceIds,
                            type = "observation"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("PastSelfViewModel", "AI observation failed", e)
                _observations.value = listOf(
                    AIObservation(
                        id = UUID.randomUUID().toString(),
                        content = "读了这段时间的${entries.size}篇日记，有些感触想和你聊聊。不过AI暂时不在线，你可以先说说你想聊什么。",
                        type = "observation"
                    )
                )
            }
            _isLoading.value = false
            saveSession()
        }
    }

    fun sendResponse(text: String) {
        val session = _currentSession.value ?: return
        val period = session.focusPeriodStart to session.focusPeriodEnd
        _isLoading.value = true

        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { dao.getEntriesByDateRange(period.first, period.second) }
            val dateFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
            val entriesSummary = entries.take(20).joinToString("\n") { e ->
                "【${e.id}】${dateFmt.format(Date(e.createdAt))}: ${e.title}\n${e.plainText.take(200)}"
            }

            val history = _observations.value.joinToString("\n") { "${it.type}: ${it.content}" }

            val systemPrompt = """你是一个基于日记的对话AI。用户在和一个特定的时间段的自己对话。

时间范围内的日记：
$entriesSummary

之前的观察和对话：
$history

要求：
- 每一次回复都必须基于日记中的具体内容
- 引用具体条目时标注【条目ID】
- 保持自然的对话语气，像朋友聊天
- 如果用户的回答和日记内容有出入，可以温和地指出
- 回答长度200-300字
- 最后可以追问或提出新的观察"""

            try {
                val messages = listOf(
                    AiMessage("system", systemPrompt),
                    AiMessage("user", text)
                )
                val result = app.aiService.chat(
                    AiRequest(messages = messages, maxTokens = 1024, temperature = 0.8f),
                    useCache = false
                )
                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    val sourceIds = entries.take(8).map { it.id }
                    _observations.value = _observations.value + AIObservation(
                        id = UUID.randomUUID().toString(),
                        content = reply,
                        sourceEntries = sourceIds,
                        type = "observation"
                    )
                }
            } catch (e: Exception) {
                Log.e("PastSelfViewModel", "Response failed", e)
            }
            _isLoading.value = false
            saveSession()
        }
    }

    fun toggleDebateMode() {
        _debateMode.value = !_debateMode.value
    }

    fun startDebate(period1: Pair<Long, Long>, period2: Pair<Long, Long>, topic: String) {
        _isLoading.value = true
        _debateMode.value = true
        val session = _currentSession.value
        _currentSession.value = session?.copy(debateConfig = DebateConfig(
            period1Start = period1.first, period1End = period1.second,
            period2Start = period2.first, period2End = period2.second,
            topic = topic
        ))

        viewModelScope.launch {
            val entries1 = withContext(Dispatchers.IO) { dao.getEntriesByDateRange(period1.first, period1.second) }
            val entries2 = withContext(Dispatchers.IO) { dao.getEntriesByDateRange(period2.first, period2.second) }
            val dateFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)

            val summary1 = entries1.take(15).joinToString("\n") { e ->
                "【${e.id}】${dateFmt.format(Date(e.createdAt))}: ${e.plainText.take(150)}"
            }
            val summary2 = entries2.take(15).joinToString("\n") { e ->
                "【${e.id}】${dateFmt.format(Date(e.createdAt))}: ${e.plainText.take(150)}"
            }

            val systemPrompt = """你正在主持一场跨越时间的辩论。两个AI角色分别基于用户两个不同时期的日记构建。

话题：$topic

时期1的日记：
$summary1

时期2的日记：
$summary2

要求：
- 以"关于这个话题，两个时期的你有不同的想法"开头
- 分别呈现两个时期的观点，必须引用具体日记内容【条目ID】
- 指出观点之间的矛盾、变化或延续
- 最后提出问题让用户参与讨论
- 200-400字"""

            try {
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(AiMessage("system", systemPrompt)),
                        maxTokens = 1024,
                        temperature = 0.85f
                    ),
                    useCache = false
                )
                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    _observations.value = _observations.value + AIObservation(
                        id = UUID.randomUUID().toString(),
                        content = reply,
                        sourceEntries = (entries1.take(5) + entries2.take(5)).map { it.id },
                        type = "debate_point"
                    )
                }
            } catch (e: Exception) {
                Log.e("PastSelfViewModel", "Debate failed", e)
            }
            _isLoading.value = false
            saveSession()
        }
    }

    fun interject(observationId: String, note: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            val period = session.focusPeriodStart to session.focusPeriodEnd
            val entries = withContext(Dispatchers.IO) { dao.getEntriesByDateRange(period.first, period.second) }
            val dateFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
            val entriesSummary = entries.take(20).joinToString("\n") { e ->
                "【${e.id}】${dateFmt.format(Date(e.createdAt))}: ${e.plainText.take(200)}"
            }

            val systemPrompt = """用户对之前的观察提出了不同意见：$note

请重新审视日记内容，并给出回应：
- 如果用户说得对，承认并调整观察
- 如果日记中有证据支持之前的观察，温和地指出
- 始终引用具体条目【条目ID】
- 语气诚恳，不固执

日记：
$entriesSummary"""

            try {
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(AiMessage("system", systemPrompt)),
                        maxTokens = 1024,
                        temperature = 0.7f
                    ),
                    useCache = false
                )
                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    _observations.value = _observations.value + AIObservation(
                        id = UUID.randomUUID().toString(),
                        content = reply,
                        sourceEntries = entries.take(5).map { it.id },
                        type = "observation"
                    )
                }
            } catch (e: Exception) {
                Log.e("PastSelfViewModel", "Interjection failed", e)
            }
            _isLoading.value = false
            saveSession()
        }
    }

    fun generateLetter(direction: String) {
        val session = _currentSession.value ?: return
        val period = session.focusPeriodStart to session.focusPeriodEnd
        _isLoading.value = true

        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { dao.getEntriesByDateRange(period.first, period.second) }
            val dateFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
            val entriesSummary = entries.sortedBy { it.createdAt }.joinToString("\n") { e ->
                "【${e.id}】${dateFmt.format(Date(e.createdAt))}: ${e.title}\n${e.plainText.take(200)}"
            }

            val prompt = if (direction == "past_to_present") {
                """你是一个基于日记内容的写信AI。请以过去的"他/她"的口吻，给现在的用户写一封信。

时间范围内的日记：
$entriesSummary

要求：
- 模仿日记中的写作风格和用词习惯
- 内容基于日记中的真实事件和感受
- 语气符合当时的心境
- 引用具体事件时标注【条目ID】
- 写一封真挚的信，200-500字
- 署名可以写"过去的你""""
            } else {
                """你是一个基于日记内容的写信AI。请以现在的用户的口吻，给未来的自己写一封信。

时间范围内的日记（供参考过去的经历）：
$entriesSummary

要求：
- 基于用户现在的状态和过去的经历写给未来
- 语气真诚自然
- 可以提到现在关心的事、未完成的计划、对未来的期待
- 引用具体事件时标注【条目ID】
- 200-500字
- 署名可以写"现在的你""""
            }

            try {
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(AiMessage("system", prompt)),
                        maxTokens = 1024,
                        temperature = 0.85f
                    ),
                    useCache = false
                )
                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    val letter = TimeLetter(
                        id = UUID.randomUUID().toString(),
                        direction = direction,
                        content = reply,
                        sourcePeriodStart = period.first,
                        sourcePeriodEnd = period.second
                    )
                    _letters.value = _letters.value + letter
                }
            } catch (e: Exception) {
                Log.e("PastSelfViewModel", "Letter generation failed", e)
            }
            _isLoading.value = false
            saveSession()
        }
    }

    fun editLetter(letterId: String, newContent: String) {
        _letters.value = _letters.value.map { letter ->
            if (letter.id == letterId) {
                letter.copy(
                    content = newContent,
                    userEdits = letter.userEdits + newContent
                )
            } else letter
        }
        saveSession()
    }

    fun analyzeGrowth(topic: String) {
        _growthLoading.value = true
        viewModelScope.launch {
            try {
                val allEntries = withContext(Dispatchers.IO) { dao.getAllEntriesOnce() }
                val dateFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)

                val entriesText = allEntries.sortedBy { it.createdAt }.joinToString("\n") { e ->
                    "【${e.id}】${dateFmt.format(Date(e.createdAt))}: ${e.title}\n${e.plainText.take(150)}"
                }

                val systemPrompt = """分析日记中关于"$topic"的提及情况。

日记：
$entriesText

以JSON格式返回（不要其他文字）：
{
  "points": [
    {"entryId": 数字, "date": "yyyy年M月d日", "label": "简要描述该处提及", "intensity": 0.0到1.0的浮点数}
  ],
  "trend": "总结变化趋势，100字以内"
}
只返回JSON，不要其他内容。"""

                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(AiMessage("system", systemPrompt)),
                        maxTokens = 2048,
                        temperature = 0.3f
                    ),
                    useCache = false
                )
                val reply = result.getOrNull()?.content?.trim() ?: "{}"
                val json = reply.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
                try {
                    val parsed = gson.fromJson(json, Map::class.java)
                    val pointsRaw = parsed["points"] as? List<Map<String, Any>> ?: emptyList()
                    val points = pointsRaw.mapNotNull { p ->
                        val entryId = (p["entryId"] as? Double)?.toLong() ?: return@mapNotNull null
                        val label = p["label"] as? String ?: ""
                        val intensity = (p["intensity"] as? Double)?.toFloat() ?: 0.5f
                        val entry = allEntries.find { it.id == entryId }
                        GrowthPoint(
                            date = entry?.createdAt ?: 0L,
                            label = label,
                            intensity = intensity
                        )
                    }.sortedBy { it.date }
                    val trend = parsed["trend"] as? String ?: ""
                    _growthTopics.value = _growthTopics.value + GrowthTopic(topic, points, trend)
                } catch (e: Exception) {
                    Log.e("PastSelfViewModel", "Failed to parse growth JSON", e)
                }
            } catch (e: Exception) {
                Log.e("PastSelfViewModel", "Growth analysis failed", e)
            }
            _growthLoading.value = false
        }
    }
}
