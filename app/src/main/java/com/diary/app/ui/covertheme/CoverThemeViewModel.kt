package com.diary.app.ui.covertheme

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.CoverTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PresetCover(
    val name: String,
    val texturePath: String?,
    val fontFamily: String?,
    val accentColor: Long?
)

data class CustomCoverTheme(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bgColor: String,
    val textColor: String,
    val accentColor: String,
    val textureName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)

data class ThemePalette(
    val bgColor: String,
    val textColor: String,
    val accentColor: String,
    val textureName: String
)

val presetColors = listOf(
    "#F5F0E1", "#E8D5C4", "#D4A574", "#B89860",
    "#B89080", "#7BA06E", "#5A9EA0", "#4D6AA8",
    "#C48880", "#8B7E74", "#A3B59A", "#E0C8B0"
)

val textureOptions = listOf(
    "无" to "",
    "纸纹" to "paper_warm",
    "水彩" to "watercolor",
    "几何" to "geometric"
)

class CoverThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val sessionStore = app.readingSessionStore
    private val sp = application.getSharedPreferences("cover_theme", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _themes = MutableStateFlow<List<CoverTheme>>(emptyList())
    val themes: StateFlow<List<CoverTheme>> = _themes.asStateFlow()

    private val _activeTheme = MutableStateFlow<CoverTheme?>(null)
    val activeTheme: StateFlow<CoverTheme?> = _activeTheme.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("全部")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _aiRecommendations = MutableStateFlow<List<String>>(emptyList())
    val aiRecommendations: StateFlow<List<String>> = _aiRecommendations.asStateFlow()

    private val _customThemes = MutableStateFlow<List<CustomCoverTheme>>(emptyList())
    val customThemes: StateFlow<List<CustomCoverTheme>> = _customThemes.asStateFlow()

    private val _usageCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usageCounts: StateFlow<Map<String, Int>> = _usageCounts.asStateFlow()

    private val _defaultThemeName = MutableStateFlow<String?>(null)
    val defaultThemeName: StateFlow<String?> = _defaultThemeName.asStateFlow()

    val categories = listOf("全部", "简约", "自然", "复古", "现代", "自定义")

    private val presetCategoryMap = mapOf(
        "素雅白" to "简约",
        "暖纸纹" to "复古",
        "水墨风" to "复古",
        "苔藓绿" to "自然",
        "沙金褐" to "自然",
        "陶土棕" to "自然",
        "海潮蓝" to "现代",
        "玫瑰粉" to "现代",
        "墨水蓝" to "现代"
    )

    val presets = listOf(
        PresetCover("素雅白", null, "serif", null),
        PresetCover("暖纸纹", "paper_warm", "serif", 0xFFD4A574),
        PresetCover("水墨风", "ink_brush", "serif", 0xFF4D6AA8),
        PresetCover("苔藓绿", "moss", "serif", 0xFF7BA06E),
        PresetCover("沙金褐", "sand", "serif", 0xFFB89860),
        PresetCover("陶土棕", "clay", "serif", 0xFFB89080),
        PresetCover("海潮蓝", "ocean", "sans-serif", 0xFF5A9EA0),
        PresetCover("玫瑰粉", "petal", "serif", 0xFFC48880),
        PresetCover("墨水蓝", "ink", "serif", 0xFF4D6AA8),
    )

    init {
        loadThemes()
        loadCustomThemes()
        loadUsageCounts()
        loadDefaultTheme()
        loadAiRecommendations()
    }

    fun loadThemes() {
        viewModelScope.launch {
            dao.getAllCoverThemes().collect { list ->
                _themes.value = list
                _activeTheme.value = list.firstOrNull { it.isActive }
                _isLoading.value = false
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun getCategoryForPreset(name: String): String {
        return presetCategoryMap[name] ?: "简约"
    }

    fun getFilteredPresets(): List<PresetCover> {
        val cat = _selectedCategory.value
        if (cat == "全部" || cat == "自定义") return presets
        return presets.filter { presetCategoryMap[it.name] == cat }
    }

    fun getFilteredCustomThemes(): List<CustomCoverTheme> {
        val cat = _selectedCategory.value
        if (cat == "全部" || cat == "自定义") return _customThemes.value
        return emptyList()
    }

    fun applyPresetTheme(preset: PresetCover, setAsDefault: Boolean = false) {
        viewModelScope.launch {
            dao.deactivateAllCoverThemes()
            val existing = _themes.value.firstOrNull { it.name == preset.name }
            if (existing != null) {
                dao.activateCoverTheme(existing.id)
                if (setAsDefault) setDefaultThemeName(preset.name)
            } else {
                dao.insertCoverTheme(
                    CoverTheme(
                        name = preset.name,
                        texturePath = preset.texturePath,
                        fontFamily = preset.fontFamily,
                        accentColor = preset.accentColor,
                        isActive = true
                    )
                )
                if (setAsDefault) setDefaultThemeName(preset.name)
            }
            sessionStore.setTheme(preset.name)
            incrementUsage(preset.name)
        }
    }

    fun applyCustomTheme(theme: CustomCoverTheme, setAsDefault: Boolean = false) {
        viewModelScope.launch {
            dao.deactivateAllCoverThemes()
            val existing = _themes.value.firstOrNull { it.name == theme.name }
            val accentValue = try {
                ("FF" + theme.accentColor.replace("#", "")).toLong(16)
            } catch (_: Exception) { null }
            if (existing != null) {
                dao.activateCoverTheme(existing.id)
            } else {
                dao.insertCoverTheme(
                    CoverTheme(
                        name = theme.name,
                        texturePath = theme.textureName,
                        fontFamily = null,
                        accentColor = accentValue,
                        isActive = true
                    )
                )
            }
            if (setAsDefault) setDefaultThemeName(theme.name)
            val updated = _customThemes.value.map {
                if (it.id == theme.id) it.copy(isActive = true) else it.copy(isActive = false)
            }
            _customThemes.value = updated
            saveCustomThemes(updated)
            sessionStore.setTheme(theme.name)
            incrementUsage(theme.name)
        }
    }

    fun applyThemeFromRoom(theme: CoverTheme, setAsDefault: Boolean = false) {
        viewModelScope.launch {
            dao.deactivateAllCoverThemes()
            dao.activateCoverTheme(theme.id)
            if (setAsDefault) setDefaultThemeName(theme.name)
            sessionStore.setTheme(theme.name)
            incrementUsage(theme.name)
        }
    }

    private fun saveCustomThemes(themes: List<CustomCoverTheme>) {
        sp.edit().putString("custom_cover_themes", gson.toJson(themes)).apply()
    }

    private fun loadCustomThemes() {
        val json = sp.getString("custom_cover_themes", null) ?: return
        try {
            val type = object : TypeToken<List<CustomCoverTheme>>() {}.type
            val list: List<CustomCoverTheme> = gson.fromJson(json, type)
            _customThemes.value = list
        } catch (_: Exception) {}
    }

    fun saveCustomTheme(theme: CustomCoverTheme) {
        val updated = _customThemes.value + theme
        _customThemes.value = updated
        saveCustomThemes(updated)
        sp.edit().putString("theme_colors_${theme.id}", gson.toJson(
            ThemePalette(theme.bgColor, theme.textColor, theme.accentColor, theme.textureName)
        )).apply()
    }

    fun deleteCustomTheme(id: String) {
        val updated = _customThemes.value.filter { it.id != id }
        _customThemes.value = updated
        saveCustomThemes(updated)
        sp.edit().remove("theme_colors_$id").apply()
    }

    private fun loadUsageCounts() {
        val json = sp.getString("theme_usage_count", null) ?: return
        try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val map: Map<String, Int> = gson.fromJson(json, type)
            _usageCounts.value = map
        } catch (_: Exception) {}
    }

    private fun saveUsageCounts() {
        sp.edit().putString("theme_usage_count", gson.toJson(_usageCounts.value)).apply()
    }

    fun incrementUsage(name: String) {
        val current = _usageCounts.value.toMutableMap()
        current[name] = (current[name] ?: 0) + 1
        _usageCounts.value = current
        saveUsageCounts()
    }

    fun getUsageCount(name: String): Int = _usageCounts.value[name] ?: 0

    fun getMostUsedTheme(): String? = _usageCounts.value.maxByOrNull { it.value }?.value?.let {
        if (it > 0) _usageCounts.value.maxByOrNull { it.value }?.key else null
    }

    private fun loadDefaultTheme() {
        val name = sp.getString("default_theme_name", null)
        _defaultThemeName.value = name
    }

    private fun setDefaultThemeName(name: String?) {
        _defaultThemeName.value = name
        sp.edit().putString("default_theme_name", name).apply()
    }

    fun toggleDefaultTheme(name: String?, isDefault: Boolean) {
        if (isDefault) setDefaultThemeName(name) else setDefaultThemeName(null)
    }

    private fun loadAiRecommendations() {
        viewModelScope.launch {
            val lastTime = sp.getLong("last_theme_recommendation_time", 0L)
            val now = System.currentTimeMillis()
            if (now - lastTime < 24 * 60 * 60 * 1000) {
                val json = sp.getString("ai_theme_recommendation", null) ?: return@launch
                try {
                    val type = object : TypeToken<List<String>>() {}.type
                    val list: List<String> = gson.fromJson(json, type)
                    _aiRecommendations.value = list
                } catch (_: Exception) {}
            } else {
                fetchAiRecommendations()
            }
        }
    }

    private fun fetchAiRecommendations() {
        viewModelScope.launch {
            if (!app.aiService.isAiEnabled()) return@launch
            try {
                val entries = dao.getAllEntriesOnce()
                val moods = entries.mapNotNull { it.moodLevel }.take(10).joinToString(",")
                val tags = entries.take(5).map { it.title }.filter { it.isNotBlank() }.joinToString(",")
                val request = aiRequest(
                    userMessage = "用户近期日记情绪：$moods，标签：$tags",
                    systemPrompt = "根据用户近期日记数据，从以下主题列表中推荐最合适的3个：素雅白、暖纸纹、水墨风、苔藓绿、沙金褐、陶土棕、海潮蓝、玫瑰粉、墨水蓝。返回JSON数组格式"
                )
                app.aiService.chat(request).onSuccess { response ->
                    try {
                        val type = object : TypeToken<List<String>>() {}.type
                        val list: List<String> = gson.fromJson(response.content, type)
                        _aiRecommendations.value = list.take(3)
                        sp.edit().putString("ai_theme_recommendation", gson.toJson(list.take(3))).apply()
                        sp.edit().putLong("last_theme_recommendation_time", System.currentTimeMillis()).apply()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteTheme(theme: CoverTheme) {
        viewModelScope.launch {
            dao.deleteCoverThemeById(theme.id)
        }
    }
}
