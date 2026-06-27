package com.diary.app.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.Goal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val goals: StateFlow<List<Goal>> = dao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addGoal(title: String, description: String) {
        viewModelScope.launch {
            val goal = Goal(
                title = title,
                description = description,
                createdAt = System.currentTimeMillis()
            )
            dao.insertGoal(goal)
        }
    }

    fun updateProgress(id: Long, progress: Int) {
        viewModelScope.launch {
            dao.updateGoalProgress(id, progress.coerceIn(0, 100))
        }
    }
}
