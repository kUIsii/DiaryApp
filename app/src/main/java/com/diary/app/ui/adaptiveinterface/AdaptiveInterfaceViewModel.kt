package com.diary.app.ui.adaptiveinterface

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import com.diary.app.util.computeStreak
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class AdaptiveSuggestion(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val reason: String,
)

data class TimeRange(val start: Int, val end: Int)

data class LayoutPattern(
    val id: String,
    val timeRange: TimeRange,
    val dayOfWeek: Int,
    val posture: String,
    val widgetOrder: List<String>,
    val panelConfig: Map<String, Any> = emptyMap(),
    val usageCount: Int
)

data class TypographyConfig(
    val fontWeight: Int = 400,
    val contrast: Float = 1.0f,
    val backgroundOpacity: Float = 1.0f
)

data class LayoutSuggestionItem(
    val name: String,
    val timeContext: String,
    val confidence: Float,
    val widgetOrder: List<String>
)

enum class ScreenMode(val label: String) {
    PHONE("手机模式"),
    FOLDABLE_UNFOLDED("折叠屏展开"),
    TABLET("平板模式"),
    DEX("DeX桌面模式")
}

private data class WidgetInteraction(
    val widgetId: String,
    val timestamp: Long
)

class AdaptiveInterfaceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val prefs = application.getSharedPreferences("adaptive_interface", Context.MODE_PRIVATE)
    private val patternsPrefs = application.getSharedPreferences("layout_patterns", Context.MODE_PRIVATE)
    private val typographyPrefs = application.getSharedPreferences("adaptive_typography", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val aiService = (application as DiaryApplication).aiService

    private val _adaptiveEnabled = MutableStateFlow(prefs.getBoolean("adaptiveEnabled", false))
    val adaptiveEnabled: StateFlow<Boolean> = _adaptiveEnabled.asStateFlow()

    private val _autoNightMode = MutableStateFlow(prefs.getBoolean("autoNightMode", false))
    val autoNightMode: StateFlow<Boolean> = _autoNightMode.asStateFlow()

    private val _compactMode = MutableStateFlow(prefs.getBoolean("compactMode", false))
    val compactMode: StateFlow<Boolean> = _compactMode.asStateFlow()

    private val _totalEntries = MutableStateFlow(0)
    val totalEntries: StateFlow<Int> = _totalEntries.asStateFlow()

    private val _thisMonthEntries = MutableStateFlow(0)
    val thisMonthEntries: StateFlow<Int> = _thisMonthEntries.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _suggestions = MutableStateFlow<List<AdaptiveSuggestion>>(emptyList())
    val suggestions: StateFlow<List<AdaptiveSuggestion>> = _suggestions.asStateFlow()

    private val _currentScreenMode = MutableStateFlow(detectScreenMode())
    val currentScreenMode: StateFlow<ScreenMode> = _currentScreenMode.asStateFlow()

    private val _layoutPatterns = MutableStateFlow(loadLayoutPatterns())
    val layoutPatterns: StateFlow<List<LayoutPattern>> = _layoutPatterns.asStateFlow()

    private val _typographyConfig = MutableStateFlow(loadTypographyConfig())
    val typographyConfig: StateFlow<TypographyConfig> = _typographyConfig.asStateFlow()

    private val _autoDetectTypography = MutableStateFlow(typographyPrefs.getBoolean("autoDetect", false))
    val autoDetectTypography: StateFlow<Boolean> = _autoDetectTypography.asStateFlow()

    private val _layoutSuggestions = MutableStateFlow<List<LayoutSuggestionItem>>(emptyList())
    val layoutSuggestions: StateFlow<List<LayoutSuggestionItem>> = _layoutSuggestions.asStateFlow()

    private val _isPredictingLayout = MutableStateFlow(false)
    val isPredictingLayout: StateFlow<Boolean> = _isPredictingLayout.asStateFlow()

    private val _screenModeSuggestions = MutableStateFlow<List<String>>(emptyList())
    val screenModeSuggestions: StateFlow<List<String>> = _screenModeSuggestions.asStateFlow()

    fun setAdaptiveEnabled(v: Boolean) {
        _adaptiveEnabled.value = v
        prefs.edit().putBoolean("adaptiveEnabled", v).apply()
        if (v) predictLayouts()
    }

    fun setAutoNightMode(v: Boolean) {
        _autoNightMode.value = v
        prefs.edit().putBoolean("autoNightMode", v).apply()
    }

    fun setCompactMode(v: Boolean) {
        _compactMode.value = v
        prefs.edit().putBoolean("compactMode", v).apply()
    }

    fun refreshScreenMode() {
        _currentScreenMode.value = detectScreenMode()
        updateScreenModeSuggestions()
    }

    private fun detectScreenMode(): ScreenMode {
        val config = getApplication<DiaryApplication>().resources.configuration
        val widthDp = config.screenWidthDp
        val isDesktop = (config.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_DESK
        return when {
            isDesktop -> ScreenMode.DEX
            widthDp >= 840 -> ScreenMode.TABLET
            widthDp >= 600 -> ScreenMode.FOLDABLE_UNFOLDED
            else -> ScreenMode.PHONE
        }
    }

    private fun updateScreenModeSuggestions() {
        _screenModeSuggestions.value = when (_currentScreenMode.value) {
            ScreenMode.PHONE -> listOf("建议使用单一竖排布局，方便单手操作", "推荐启用自适应功能获得更好体验")
            ScreenMode.FOLDABLE_UNFOLDED -> listOf("建议使用双栏布局，左侧列表右侧详情", "大屏空间适合同时查看日历和日记")
            ScreenMode.TABLET -> listOf("建议开启侧边栏，充分利用大屏空间", "可以考虑使用三栏布局展示更多信息")
            ScreenMode.DEX -> listOf("建议使用多面板仪表盘布局", "窗口化操作适合同时打开多个功能")
        }
    }

    fun recordLayoutUsage(widgetOrder: List<String>, posture: String) {
        val now = LocalDate.now()
        val hour = LocalTime.now().hour
        val dayOfWeek = now.dayOfWeek.value

        val patterns = _layoutPatterns.value.toMutableList()
        val existingIndex = patterns.indexOfFirst {
            it.posture == posture && it.dayOfWeek == dayOfWeek &&
            hour in it.timeRange.start..it.timeRange.end
        }
        if (existingIndex >= 0) {
            val p = patterns[existingIndex]
            patterns[existingIndex] = p.copy(usageCount = p.usageCount + 1)
        } else {
            patterns.add(
                LayoutPattern(
                    id = "lp_${System.currentTimeMillis()}",
                    timeRange = TimeRange(hour, hour),
                    dayOfWeek = dayOfWeek,
                    posture = posture,
                    widgetOrder = widgetOrder,
                    usageCount = 1
                )
            )
        }
        _layoutPatterns.value = patterns
        saveLayoutPatterns(patterns)
    }

    private fun loadLayoutPatterns(): List<LayoutPattern> {
        val json = patternsPrefs.getString("patterns", "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<LayoutPattern>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLayoutPatterns(patterns: List<LayoutPattern>) {
        patternsPrefs.edit().putString("patterns", gson.toJson(patterns)).apply()
    }

    private fun loadTypographyConfig(): TypographyConfig {
        val json = typographyPrefs.getString("config", null) ?: return TypographyConfig()
        return try {
            gson.fromJson(json, TypographyConfig::class.java)
        } catch (e: Exception) {
            TypographyConfig()
        }
    }

    private fun saveTypographyConfig(config: TypographyConfig) {
        typographyPrefs.edit().putString("config", gson.toJson(config)).apply()
    }

    fun setTypographyConfig(config: TypographyConfig) {
        _typographyConfig.value = config
        saveTypographyConfig(config)
        if (!_autoDetectTypography.value) {
            recordManualAdjustment(config)
        }
    }

    fun setAutoDetectTypography(v: Boolean) {
        _autoDetectTypography.value = v
        typographyPrefs.edit().putBoolean("autoDetect", v).apply()
        if (v) autoDetectFromContext()
    }

    fun autoDetectFromContext() {
        val hour = LocalTime.now().hour
        val brightness = getScreenBrightness()
        val isNight = hour < 6 || hour >= 18
        val lowBrightness = brightness < 100

        _typographyConfig.value = TypographyConfig(
            fontWeight = if (isNight || lowBrightness) 500 else 400,
            contrast = if (isNight || lowBrightness) 1.0f else 0.85f,
            backgroundOpacity = if (isNight) 0.92f else 0.95f
        )
        saveTypographyConfig(_typographyConfig.value)
    }

    private fun getScreenBrightness(): Int {
        return try {
            val cr = getApplication<DiaryApplication>().contentResolver
            Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            200
        }
    }

    fun getScreenBrightnessPercent(): Int {
        return (getScreenBrightness() / 2.55f).toInt().coerceIn(0, 100)
    }

    private fun recordManualAdjustment(config: TypographyConfig) {
        val adjustments = getManualAdjustments().toMutableList()
        adjustments.add(config)
        if (adjustments.size > 20) {
            adjustments.removeAt(0)
        }
        typographyPrefs.edit().putString("manual_adjustments", gson.toJson(adjustments)).apply()
    }

    private fun getManualAdjustments(): List<TypographyConfig> {
        val json = typographyPrefs.getString("manual_adjustments", "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<TypographyConfig>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun applySuggestion(suggestion: LayoutSuggestionItem) {
        recordLayoutUsage(suggestion.widgetOrder, _currentScreenMode.value.name.lowercase())
        val patterns = _layoutPatterns.value.toMutableList()
        val existing = patterns.indexOfFirst {
            it.widgetOrder == suggestion.widgetOrder
        }
        if (existing >= 0) {
            val p = patterns[existing]
            patterns[existing] = p.copy(usageCount = p.usageCount + 1)
        }
        _layoutPatterns.value = patterns
        saveLayoutPatterns(patterns)
    }

    fun dismissSuggestion(index: Int) {
        val list = _layoutSuggestions.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _layoutSuggestions.value = list
        }
    }

    fun predictLayouts() {
        if (!_adaptiveEnabled.value) return
        viewModelScope.launch {
            _isPredictingLayout.value = true
            try {
                if (aiService.isAiEnabled()) {
                    val suggestions = predictFromAi()
                    if (suggestions.isNotEmpty()) {
                        _layoutSuggestions.value = suggestions
                        return@launch
                    }
                }
                _layoutSuggestions.value = generateHeuristicSuggestions()
            } finally {
                _isPredictingLayout.value = false
            }
        }
    }

    private suspend fun predictFromAi(): List<LayoutSuggestionItem> = withContext(Dispatchers.IO) {
        val hour = LocalTime.now().hour
        val dayOfWeek = LocalDate.now().dayOfWeek.value
        val season = getSeason()
        val mode = _currentScreenMode.value.label

        val systemPrompt = "你是一个自适应界面布局助手。根据用户的使用数据，推荐1-3个最合适的布局方案。" +
            "请严格以JSON数组格式返回，每个元素包含name(布局名称), context(适用场景), confidence(0-1), widgets(widget列表)。" +
            "可用widget: diary_list(日记列表), calendar(日历), stats(统计), tags(标签), search(搜索), writing(写作区), mood(心情), quick_entry(快速记录)。"

        val userMessage = "当前时间: ${hour}时, 星期${dayOfWeek}, 季节: $season, 屏幕模式: $mode。" +
            "总日记数: ${_totalEntries.value}, 本月: ${_thisMonthEntries.value}, 连续: ${_currentStreak.value}天。" +
            "请推荐适合当前场景的布局方案。"

        try {
            val result = aiService.chat(
                AiRequest(
                    messages = listOf(
                        AiMessage("system", systemPrompt),
                        AiMessage("user", userMessage)
                    ),
                    temperature = 0.3f,
                    maxTokens = 1024
                )
            )
            result.getOrNull()?.let { parseAiLayoutResponse(it.content) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseAiLayoutResponse(json: String): List<LayoutSuggestionItem> {
        val cleaned = json.replace("```json", "").replace("```", "").trim()
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list: List<Map<String, Any>> = gson.fromJson(cleaned, type) ?: return emptyList()
            list.mapNotNull { item ->
                val name = item["name"] as? String ?: return@mapNotNull null
                val context = item["context"] as? String ?: ""
                val confidence = (item["confidence"] as? Double)?.toFloat()?.coerceIn(0f, 1f) ?: 0.5f
                val widgets = (item["widgets"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                LayoutSuggestionItem(name, context, confidence, widgets)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getSeason(): String {
        val month = LocalDate.now().monthValue
        return when (month) {
            in 3..5 -> "春季"
            in 6..8 -> "夏季"
            in 9..11 -> "秋季"
            else -> "冬季"
        }
    }

    private fun generateHeuristicSuggestions(): List<LayoutSuggestionItem> {
        val hour = LocalTime.now().hour
        val dayOfWeek = LocalDate.now().dayOfWeek.value
        val suggestions = mutableListOf<LayoutSuggestionItem>()
        val isWeekend = dayOfWeek >= 6
        val isNight = hour < 6 || hour >= 22
        val isEvening = hour >= 18 && hour < 22
        val isMorning = hour >= 6 && hour < 12

        if (isNight || isEvening) {
            suggestions.add(
                LayoutSuggestionItem(
                    name = "夜间写作布局",
                    timeContext = if (isNight) "深夜写作时间" else "傍晚放松时间",
                    confidence = if (isNight) 0.85f else 0.7f,
                    widgetOrder = listOf("writing", "mood", "quick_entry")
                )
            )
        }
        if (isMorning || isWeekend) {
            suggestions.add(
                LayoutSuggestionItem(
                    name = "回顾总结布局",
                    timeContext = if (isMorning) "早晨回顾" else "周末总结",
                    confidence = if (isWeekend) 0.8f else 0.65f,
                    widgetOrder = listOf("diary_list", "stats", "calendar", "tags")
                )
            )
        }
        suggestions.add(
            LayoutSuggestionItem(
                name = "完整功能布局",
                timeContext = "日常使用",
                confidence = 0.6f,
                widgetOrder = listOf("writing", "diary_list", "calendar", "stats", "tags")
            )
        )
        return suggestions
    }

    init {
        viewModelScope.launch {
            val allEntries = dao.getAllEntriesOnce()
            val zone = ZoneId.systemDefault()
            val now = LocalDate.now()

            _totalEntries.value = dao.getEntryCount()

            _thisMonthEntries.value = allEntries.count { entry ->
                val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                date.year == now.year && date.monthValue == now.monthValue
            }

            val dates = allEntries.map {
                Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            }.toSet()
            _currentStreak.value = computeStreak(dates)

            val suggestionsList = mutableListOf<AdaptiveSuggestion>()

            val nightEntries = allEntries.count {
                val hour = Instant.ofEpochMilli(it.createdAt).atZone(zone).hour
                hour in 0..5 || hour >= 22
            }
            if (nightEntries >= 3) {
                suggestionsList.add(
                    AdaptiveSuggestion(
                        icon = Icons.Default.DarkMode,
                        title = "开启夜间模式",
                        description = "根据您的写作时间，建议在深夜自动切换为暗色主题",
                        reason = "$nightEntries 篇日记写在深夜，适合启用自动夜间模式",
                    )
                )
            }

            if (allEntries.size >= 15) {
                val avgLength = allEntries.filter { it.plainText.isNotBlank() }.let { entries ->
                    if (entries.isEmpty()) 0 else entries.sumOf { it.plainText.length } / entries.size
                }
                if (avgLength >= 200) {
                    suggestionsList.add(
                        AdaptiveSuggestion(
                            icon = Icons.Default.Timer,
                            title = "开启紧凑模式",
                            description = "单篇内容较长时，紧凑模式可提升浏览效率",
                            reason = "平均每篇 ${avgLength} 字，内容密度较高",
                        )
                    )
                }
            }

            if (_currentStreak.value >= 3) {
                suggestionsList.add(
                    AdaptiveSuggestion(
                        icon = Icons.Default.Home,
                        title = "开启自适应界面",
                        description = "根据使用习惯自动调整界面布局和功能排布",
                        reason = "已连续写作 ${_currentStreak.value} 天，活跃度适合自适应优化",
                    )
                )
            }

            _suggestions.value = suggestionsList

            updateScreenModeSuggestions()
            if (_autoDetectTypography.value) {
                autoDetectFromContext()
            }
            if (_adaptiveEnabled.value) {
                predictLayouts()
            }
        }
    }
}
