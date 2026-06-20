package com.diary.app.ui.achievement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.Achievement
import com.diary.app.data.AchievementManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as DiaryApplication).database
    private val achievementDao = db.achievementDao()
    private val diaryDao = db.diaryDao()

    val achievements: StateFlow<List<Achievement>> = achievementDao.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unlockedCount: StateFlow<Int> = achievementDao.getUnlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = achievementDao.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Re-check achievements when screen opens
        viewModelScope.launch {
            AchievementManager.checkAndUnlock(achievementDao, diaryDao, application)
        }
    }
}
