package com.diary.app.ui.adaptiveinterface

import android.app.Application
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.util.computeStreak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AdaptiveSuggestion(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val reason: String,
)

class AdaptiveInterfaceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val prefs = application.getSharedPreferences("adaptive_interface", Context.MODE_PRIVATE)

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

    fun setAdaptiveEnabled(v: Boolean) {
        _adaptiveEnabled.value = v
        prefs.edit().putBoolean("adaptiveEnabled", v).apply()
    }

    fun setAutoNightMode(v: Boolean) {
        _autoNightMode.value = v
        prefs.edit().putBoolean("autoNightMode", v).apply()
    }

    fun setCompactMode(v: Boolean) {
        _compactMode.value = v
        prefs.edit().putBoolean("compactMode", v).apply()
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
        }
    }
}
