package com.diary.app.ui.lockscreenquickwrite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class QuickWriteEntry(
    val id: Long,
    val content: String,
    val category: String,
    val mood: Float,
    val linkedEntryId: Long?,
    val followUpAction: String?,
    val createdAt: Long
)

enum class NoteSortMode { TIME_DESC, TIME_ASC, CATEGORY }

val NOTE_CATEGORIES = listOf("快速笔记", "灵感", "待办", "今日感想", "梦境", "摘录")

class LockScreenQuickWriteViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("quick_notes_v2", 0)
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = AiServiceManager(application)
    private val gson = Gson()

    private val _notes = MutableStateFlow(loadNotes())
    val notes: StateFlow<List<QuickWriteEntry>> = _notes.asStateFlow()

    private val _sortMode = MutableStateFlow(NoteSortMode.TIME_DESC)
    val sortMode: StateFlow<NoteSortMode> = _sortMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _aiCategory = MutableStateFlow<String?>(null)
    val aiCategory: StateFlow<String?> = _aiCategory.asStateFlow()

    private val _isClassifying = MutableStateFlow(false)
    val isClassifying: StateFlow<Boolean> = _isClassifying.asStateFlow()

    private val _contextualPrompt = MutableStateFlow<String?>(null)
    val contextualPrompt: StateFlow<String?> = _contextualPrompt.asStateFlow()

    private val _followUpSuggestion = MutableStateFlow<String?>(null)
    val followUpSuggestion: StateFlow<String?> = _followUpSuggestion.asStateFlow()

    private val _followUpActionType = MutableStateFlow<String?>(null)
    val followUpActionType: StateFlow<String?> = _followUpActionType.asStateFlow()

    private val _smartLinkSuggestion = MutableStateFlow<SmartLinkSuggestion?>(null)
    val smartLinkSuggestion: StateFlow<SmartLinkSuggestion?> = _smartLinkSuggestion.asStateFlow()

    private var classifyJob: Job? = null

    init {
        loadContextualPrompt()
    }

    private fun loadNotes(): List<QuickWriteEntry> {
        val json = prefs.getString("notes", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<QuickWriteEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun saveNotes(notes: List<QuickWriteEntry>) {
        prefs.edit().putString("notes", gson.toJson(notes)).apply()
    }

    fun addNote(content: String, category: String = "快速笔记"): Long? {
        if (content.isBlank()) return null
        val id = System.currentTimeMillis()
        val effectiveCategory = _aiCategory.value?.takeIf { category == "快速笔记" } ?: category
        val entry = QuickWriteEntry(
            id = id,
            content = content.trim(),
            category = effectiveCategory,
            mood = 0f,
            linkedEntryId = null,
            followUpAction = if (_followUpActionType.value != null) _followUpActionType.value else null,
            createdAt = id
        )
        val list = listOf(entry) + _notes.value
        _notes.value = list
        saveNotes(list)
        _aiCategory.value = null
        _followUpSuggestion.value = null
        _followUpActionType.value = null
        checkSmartLink(content, id)
        generateFollowUp(content, category)
        return id
    }

    fun setSortMode(mode: NoteSortMode) { _sortMode.value = mode }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun deleteNote(note: QuickWriteEntry) {
        val list = _notes.value.toMutableList().apply { remove(note) }
        _notes.value = list
        saveNotes(list)
    }

    fun syncToDiary(note: QuickWriteEntry, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                dao.insertEntry(
                    DiaryEntry(
                        title = note.content.take(50),
                        content = buildDiaryContentFromQuickWrite(note.content),
                        plainText = note.content,
                        createdAt = note.createdAt,
                        updatedAt = note.createdAt
                    )
                )
                deleteNote(note)
                onComplete(true)
            } catch (_: Exception) { onComplete(false) }
        }
    }

    fun syncAllToDiary(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val current = _notes.value.toList()
                for (note in current) {
                    dao.insertEntry(
                        DiaryEntry(
                            title = note.content.take(50),
                            content = buildDiaryContentFromQuickWrite(note.content),
                            plainText = note.content,
                            createdAt = note.createdAt,
                            updatedAt = note.createdAt
                        )
                    )
                }
                _notes.value = emptyList()
                prefs.edit().putString("notes", "[]").apply()
                onComplete(true)
            } catch (_: Exception) { onComplete(false) }
        }
    }

    fun onTextChanged(text: String) {
        classifyJob?.cancel()
        if (text.length > 10) {
            classifyJob = viewModelScope.launch {
                delay(2000)
                _isClassifying.value = true
                val category = classifyContent(text)
                if (category != null) {
                    _aiCategory.value = category
                }
                _isClassifying.value = false
            }
        } else {
            _aiCategory.value = null
        }
    }

    fun dismissFollowUp() {
        _followUpSuggestion.value = null
        _followUpActionType.value = null
    }

    fun acceptSmartLink(quickWriteId: Long, linkedEntryId: Long) {
        val updated = _notes.value.map {
            if (it.id == quickWriteId) it.copy(linkedEntryId = linkedEntryId) else it
        }
        _notes.value = updated
        saveNotes(updated)
        _smartLinkSuggestion.value = null
    }

    fun dismissSmartLink() {
        _smartLinkSuggestion.value = null
    }

    private suspend fun classifyContent(content: String): String? {
        if (!aiService.isAiEnabled()) return null
        val systemPrompt = "你是一个日记助手。请将以下内容分类为：灵感、待办、今日感想、梦境、摘录。只返回分类名称，不要其他文字。"
        val request = aiRequest(userMessage = content, systemPrompt = systemPrompt, maxTokens = 10, temperature = 0.3f)
        return try {
            val result = aiService.chat(request, useCache = false)
            val category = result.getOrNull()?.content?.trim()
            if (category in listOf("灵感", "待办", "今日感想", "梦境", "摘录")) category else null
        } catch (_: Exception) { null }
    }

    private fun loadContextualPrompt() {
        viewModelScope.launch {
            if (!aiService.isAiEnabled()) return@launch
            val threeDaysAgo = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L
            val recentEntries = dao.getEntriesByDateRange(threeDaysAgo, System.currentTimeMillis())
            if (recentEntries.isEmpty()) return@launch

            val entriesText = recentEntries.take(5).joinToString("\n") {
                val dateLabel = SimpleDateFormat("MM/dd", Locale.CHINESE).format(Date(it.createdAt))
                "时间: $dateLabel, 心情: ${it.moodLevel ?: "未记录"}, 内容: ${it.plainText.take(100)}"
            }

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeContext = when {
                hour < 12 -> "早上"
                hour < 18 -> "下午"
                else -> "晚上"
            }

            val systemPrompt = "你是日记助手。基于以下最近日记的情绪分析，用一句话生成一个${timeContext}的问候语，鼓励用户写日记。保持简短温暖。只返回问候语。"
            val request = aiRequest(userMessage = entriesText, systemPrompt = systemPrompt, maxTokens = 30, temperature = 0.7f)
            try {
                val result = aiService.chat(request)
                _contextualPrompt.value = result.getOrNull()?.content?.trim()
            } catch (_: Exception) { }
        }
    }

    private fun generateFollowUp(content: String, category: String) {
        viewModelScope.launch {
            if (!aiService.isAiEnabled() || content.length < 15) return@launch
            val systemPrompt = "你是日记助手。分析以下内容，判断最适合的跟进动作。如果涉及待办事项返回\"提醒\"，如果表达较强情绪返回\"安慰\"，如果涉及需要跟踪的事情返回\"跟踪\"，否则不返回任何内容。只返回一个词。"
            val request = aiRequest(userMessage = content, systemPrompt = systemPrompt, maxTokens = 10, temperature = 0.3f)
            try {
                val result = aiService.chat(request)
                val action = result.getOrNull()?.content?.trim() ?: return@launch
                when (action) {
                    "提醒" -> {
                        _followUpSuggestion.value = "要设一个提醒吗？"
                        _followUpActionType.value = "reminder"
                    }
                    "安慰" -> {
                        _followUpSuggestion.value = "今天心情似乎不太好，需要写一封安慰信给自己吗？"
                        _followUpActionType.value = "comfort"
                    }
                    "跟踪" -> {
                        _followUpSuggestion.value = "需要我帮你跟踪这件事吗？"
                        _followUpActionType.value = "track"
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun checkSmartLink(content: String, newEntryId: Long) {
        viewModelScope.launch {
            if (!aiService.isAiEnabled()) return@launch
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val recentEntries = dao.getEntriesByDateRange(sevenDaysAgo, System.currentTimeMillis())
            if (recentEntries.isEmpty()) return@launch

            val entriesContext = recentEntries.take(5).joinToString("\n---\n") {
                "ID: ${it.id}\n日期: ${SimpleDateFormat("MM/dd", Locale.CHINESE).format(Date(it.createdAt))}\n内容: ${it.plainText.take(200)}"
            }

            val prompt = "以下是最近日记和一段新内容。如果新内容与某篇日记主题相关，返回该日记ID。如果不相关，返回\"无\"。\n\n最近日记:\n$entriesContext\n\n新内容:$content"
            val systemPrompt = "比较两段文字的主题相关性。如果相关返回日记ID数字，不相关返回\"无\"。"
            val request = aiRequest(userMessage = prompt, systemPrompt = systemPrompt, maxTokens = 20, temperature = 0.3f)
            try {
                val result = aiService.chat(request)
                val response = result.getOrNull()?.content?.trim() ?: return@launch
                if (response == "无") return@launch
                val id = response.filter { it.isDigit() }.toLongOrNull() ?: return@launch
                val entry = recentEntries.find { it.id == id } ?: return@launch
                val dayLabel = SimpleDateFormat("E", Locale.CHINESE).format(Date(entry.createdAt))
                _smartLinkSuggestion.value = SmartLinkSuggestion(
                    quickWriteId = newEntryId,
                    linkedEntryId = id,
                    message = "这段内容和${dayLabel}的日记有关联，要放在一起吗？"
                )
            } catch (_: Exception) { }
        }
    }
}
