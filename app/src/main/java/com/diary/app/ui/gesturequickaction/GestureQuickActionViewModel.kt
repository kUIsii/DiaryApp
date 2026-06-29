package com.diary.app.ui.gesturequickaction

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GestureQuickActionState(
    val mappings: Map<String, String> = emptyMap(),
    val stats: Map<String, Int> = emptyMap(),
    val totalActivations: Int = 0,
    val lastUsedDate: String = "",
    val mostUsedGesture: String = "",
    val aiSuggestions: List<AiSuggestion>? = null,
    val isAiAnalyzing: Boolean = false
)

data class AiSuggestion(
    val gesture: String,
    val fromAction: String,
    val toAction: String,
    val reason: String
)

class GestureQuickActionViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("gesture_actions", Context.MODE_PRIVATE)
    private val statsPrefs = application.getSharedPreferences("gesture_stats", Context.MODE_PRIVATE)

    val gestureOptions = listOf(
        "双击", "三击", "长按", "上滑", "下滑", "左滑", "右滑", "双指点击", "双指滑动", "摇晃"
    )

    val actionOptions = listOf(
        "新建日记", "快速签到", "打开搜索", "打开统计", "打开收藏", "打开待办",
        "打开日历", "打开地图", "打开时间线", "随机回顾", "打开AI助手",
        "打开语音记录", "打开专注模式", "打开环境音", "打开那年今日",
        "打开工具箱", "打开设置", "打开编辑草稿", "无操作"
    )

    val gestureIndexMap: Map<String, Int> = gestureOptions.mapIndexed { i, g -> g to i }.toMap()

    private val cachedMappings = mutableMapOf<String, String>()
    private val cachedStats = mutableMapOf<String, Int>()
    private var initialized = false

    private val _state = MutableStateFlow(GestureQuickActionState())
    val state: StateFlow<GestureQuickActionState> = _state.asStateFlow()

    private val _resetDialog = MutableStateFlow(false)
    val resetDialog: StateFlow<Boolean> = _resetDialog.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        if (initialized) return
        initialized = true
        cachedMappings.putAll(loadMappings())
        cachedStats.putAll(loadStats())
        updateStateFromCache()
    }

    private fun updateStateFromCache() {
        val total = cachedStats.values.sum()
        val lastUsed = statsPrefs.getString("last_used_date", "") ?: ""
        val mostUsed = cachedStats.maxByOrNull { it.value }?.key ?: ""
        _state.value = _state.value.copy(
            mappings = cachedMappings.toMap(), stats = cachedStats.toMap(),
            totalActivations = total, lastUsedDate = lastUsed, mostUsedGesture = mostUsed
        )
    }

    private fun loadMappings(): Map<String, String> {
        val m = mutableMapOf<String, String>()
        gestureOptions.forEach { g ->
            val saved = prefs.getString("gesture_$g", null)
            val action = when {
                saved != null && saved in actionOptions -> saved
                saved != null -> "无操作"
                else -> defaultAction(g)
            }
            m[g] = action
        }
        return m.toMap()
    }

    private fun loadStats(): Map<String, Int> {
        val m = mutableMapOf<String, Int>()
        gestureOptions.forEach { g -> m[g] = statsPrefs.getInt("count_$g", 0) }
        return m.toMap()
    }

    private fun defaultAction(gesture: String): String = when (gesture) {
        "双击" -> "新建日记"
        "三击" -> "打开搜索"
        "长按" -> "快速签到"
        "上滑" -> "打开那年今日"
        "下滑" -> "随机回顾"
        "左滑" -> "打开收藏"
        "右滑" -> "打开时间线"
        "双指点击" -> "打开AI助手"
        "双指滑动" -> "打开统计"
        "摇晃" -> "打开设置"
        else -> "无操作"
    }

    fun setAction(gesture: String, action: String) {
        cachedMappings[gesture] = action
        prefs.edit().putString("gesture_$gesture", action).apply()
        recordUsage(gesture)
        _state.value = _state.value.copy(aiSuggestions = null)
    }

    fun recordUsage(gesture: String) {
        val count = (cachedStats[gesture] ?: 0) + 1
        cachedStats[gesture] = count
        statsPrefs.edit().putInt("count_$gesture", count).apply()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        statsPrefs.edit().putString("last_used_date", date).apply()
        updateStateFromCache()
    }

    fun executeAction(actionName: String, context: Context) {
        val msg = when (actionName) {
            "新建日记" -> "将打开日记编辑器"
            "快速签到" -> "将打开快速签到"
            "打开搜索" -> "将打开搜索"
            else -> "执行: $actionName"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun requestAiSuggestions() {
        val app = getApplication<DiaryApplication>()
        if (!app.aiService.isAiEnabled()) return
        _state.value = _state.value.copy(isAiAnalyzing = true)
        viewModelScope.launch {
            val stats = _state.value.stats
            val mappings = _state.value.mappings
            val prompt = buildString {
                appendLine("我正在使用日记应用，以下是手势操作配置和使用统计：")
                appendLine()
                gestureOptions.forEach { g ->
                    val a = mappings[g] ?: "无操作"
                    val c = stats[g] ?: 0
                    appendLine("- $g -> $a（使用${c}次）")
                }
                appendLine()
                appendLine("请分析使用模式，给出最多3个手势映射优化建议。")
                appendLine("格式（每行一条）：手势|建议动作|理由")
                appendLine("示例：双击|快速签到|双击最便捷，映射高频操作可提升效率")
            }
            val request = AiRequest(
                messages = listOf(
                    AiMessage("system", "你是一个手机使用体验优化专家，擅长分析用户操作习惯并提出改进建议。请用中文简洁回答。"),
                    AiMessage("user", prompt)
                ),
                temperature = 0.3f,
                maxTokens = 512
            )
            val result = app.aiService.chat(request)
            result.onSuccess { response ->
                val suggestions = parseSuggestions(response.content)
                _state.value = _state.value.copy(aiSuggestions = suggestions, isAiAnalyzing = false)
            }.onFailure {
                _state.value = _state.value.copy(isAiAnalyzing = false)
            }
        }
    }

    private fun parseSuggestions(content: String): List<AiSuggestion>? {
        val lines = content.lines().filter { it.contains("|") }
        if (lines.isNotEmpty()) {
            val result = lines.mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    AiSuggestion(
                        gesture = parts[0].trim(),
                        fromAction = cachedMappings[parts[0].trim()] ?: "",
                        toAction = parts[1].trim(),
                        reason = parts[2].trim()
                    )
                } else null
            }
            if (result.isNotEmpty()) return result
        }
        val suggestionLines = content.lines().filter { it.contains("建议:") }
        if (suggestionLines.isNotEmpty()) {
            val result = suggestionLines.mapNotNull { line ->
                val text = line.substringAfter("建议:").trim()
                val parts = text.split("->").map { it.trim() }
                if (parts.size >= 2) {
                    AiSuggestion(
                        gesture = parts[0],
                        fromAction = cachedMappings[parts[0]] ?: "",
                        toAction = parts[1],
                        reason = ""
                    )
                } else null
            }
            if (result.isNotEmpty()) return result
        }
        val mostUsed = cachedStats.maxByOrNull { it.value }?.key ?: return null
        val assignedAction = cachedMappings[mostUsed] ?: "无操作"
        return listOf(
            AiSuggestion(
                gesture = mostUsed,
                fromAction = assignedAction,
                toAction = "新建日记",
                reason = "最常用的手势建议绑定到新建日记以提升效率"
            )
        )
    }

    fun applyAiSuggestion(suggestion: AiSuggestion) {
        setAction(suggestion.gesture, suggestion.toAction)
        _state.value = _state.value.copy(aiSuggestions = null)
    }

    fun dismissAiSuggestions() {
        _state.value = _state.value.copy(aiSuggestions = null)
    }

    fun showResetDialog() { _resetDialog.value = true }
    fun dismissResetDialog() { _resetDialog.value = false }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        statsPrefs.edit().clear().apply()
        cachedMappings.clear()
        cachedStats.clear()
        initialized = false
        _resetDialog.value = false
        loadAll()
    }
}
