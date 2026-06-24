package com.diary.app.ui.title

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.CrossSystemManager
import com.diary.app.data.TitleDefinition
import com.diary.app.data.TitleManager
import com.diary.app.data.TitleProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TitleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as DiaryApplication).database
    private val titleDao = database.titleDao()
    private val diaryDao = database.diaryDao()

    // 所有称号定义
    private val _allDefinitions = MutableStateFlow<List<TitleDefinition>>(emptyList())
    val allDefinitions: StateFlow<List<TitleDefinition>> = _allDefinitions.asStateFlow()

    // 已解锁的称号
    private val _unlockedTitles = MutableStateFlow<List<TitleDefinition>>(emptyList())
    val unlockedTitles: StateFlow<List<TitleDefinition>> = _unlockedTitles.asStateFlow()

    // 当前选中的分类
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // 解锁进度 (key -> Pair<current, target>)
    private val _progressMap = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val progressMap: StateFlow<Map<String, Pair<Int, Int>>> = _progressMap.asStateFlow()

    // 最近解锁的称号（用于动画展示）
    private val _recentlyUnlocked = MutableStateFlow<TitleDefinition?>(null)
    val recentlyUnlocked: StateFlow<TitleDefinition?> = _recentlyUnlocked.asStateFlow()

    // 称号配置
    private val _titleProfile = MutableStateFlow<TitleProfile?>(null)
    val titleProfile: StateFlow<TitleProfile?> = _titleProfile.asStateFlow()

    // 详情弹窗选中的称号
    private val _selectedTitleForDetail = MutableStateFlow<TitleDefinition?>(null)
    val selectedTitleForDetail: StateFlow<TitleDefinition?> = _selectedTitleForDetail.asStateFlow()

    // 解锁时间映射 (titleKey -> unlockTimestamp)
    private val _unlockTimeMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val unlockTimeMap: StateFlow<Map<String, Long>> = _unlockTimeMap.asStateFlow()

    init {
        loadTitles()
    }

    private fun loadTitles() {
        viewModelScope.launch {
            titleDao.getAllDefinitions().collect { defs ->
                _allDefinitions.value = defs
            }
        }
        viewModelScope.launch {
            titleDao.getUnlockedTitles().collect { unlocked ->
                _unlockedTitles.value = unlocked
            }
        }
        viewModelScope.launch {
            titleDao.getTitleProfile().collect { profile ->
                _titleProfile.value = profile
            }
        }
        viewModelScope.launch {
            titleDao.getAllUserTitles().collect { userTitles ->
                _unlockTimeMap.value = userTitles.associate { it.titleKey to it.unlockedAt }
            }
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    /**
     * 全量扫描并检查未解锁的称号
     */
    fun checkAllTitles() {
        viewModelScope.launch {
            val newlyUnlocked = TitleManager.checkAll(diaryDao, titleDao)
            // 展示最近解锁的称号动画
            if (newlyUnlocked.isNotEmpty()) {
                _recentlyUnlocked.value = newlyUnlocked.last()
            }
            // 通知宠物系统 - 称号解锁触发宠物反应
            for (title in newlyUnlocked) {
                CrossSystemManager.emitTitleUnlock(title.tier, title.name)
            }
        }
    }

    /**
     * 加载指定称号的解锁进度
     */
    fun loadProgress(key: String) {
        viewModelScope.launch {
            val progress = TitleManager.getProgress(key, diaryDao)
            _progressMap.value = _progressMap.value + (key to progress)
        }
    }

    /**
     * 设置当前展示的称号
     */
    fun setActiveTitle(titleKey: String?) {
        viewModelScope.launch {
            val current = titleProfile.value
            val profile = TitleProfile(
                id = 1,
                activeTitleKey = titleKey,
                showTitleOnHome = current?.showTitleOnHome ?: false,
                showTitleOnEntry = current?.showTitleOnEntry ?: false
            )
            titleDao.setTitleProfile(profile)
        }
    }

    /**
     * 清除最近解锁的称号（动画播放完毕后调用）
     */
    fun clearRecentlyUnlocked() {
        _recentlyUnlocked.value = null
    }

    /**
     * 获取分类下已解锁/总数
     */
    fun getCategoryProgress(category: String?): Pair<Int, Int> {
        val all = allDefinitions.value.filter {
            category == null || it.category == category
        }
        val unlocked = unlockedTitles.value.filter {
            category == null || it.category == category
        }
        return unlocked.size to all.size
    }

    /**
     * 显示称号详情弹窗
     */
    fun showTitleDetail(title: TitleDefinition) {
        // 如果未解锁，自动加载进度
        val isUnlocked = title.key in unlockedTitles.value.map { it.key }
        if (!isUnlocked) {
            loadProgress(title.key)
        }
        _selectedTitleForDetail.value = title
    }

    /**
     * 关闭称号详情弹窗
     */
    fun dismissTitleDetail() {
        _selectedTitleForDetail.value = null
    }
}
