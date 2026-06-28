package com.diary.app.ui.covertheme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.CoverTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PresetCover(
    val name: String,
    val texturePath: String?,
    val fontFamily: String?,
    val accentColor: Long?
)

class CoverThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _themes = MutableStateFlow<List<CoverTheme>>(emptyList())
    val themes: StateFlow<List<CoverTheme>> = _themes.asStateFlow()

    private val _activeTheme = MutableStateFlow<CoverTheme?>(null)
    val activeTheme: StateFlow<CoverTheme?> = _activeTheme.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    fun applyTheme(preset: PresetCover) {
        viewModelScope.launch {
            dao.deactivateAllCoverThemes()
            val existing = _themes.value.firstOrNull { it.name == preset.name }
            if (existing != null) {
                dao.activateCoverTheme(existing.id)
            } else {
                val theme = CoverTheme(
                    name = preset.name,
                    texturePath = preset.texturePath,
                    fontFamily = preset.fontFamily,
                    accentColor = preset.accentColor,
                    isActive = true
                )
                dao.insertCoverTheme(theme)
            }
        }
    }

    fun deleteTheme(theme: CoverTheme) {
        viewModelScope.launch {
            dao.deleteCoverThemeById(theme.id)
        }
    }
}
