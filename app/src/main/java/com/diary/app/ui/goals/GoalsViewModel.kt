package com.diary.app.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.Goal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val goals: StateFlow<List<Goal>> = dao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _subGoals = MutableStateFlow<Map<Long, List<Goal>>>(emptyMap())
    val subGoals: StateFlow<Map<Long, List<Goal>>> = _subGoals

    private val _expandedGoals = MutableStateFlow<Set<Long>>(emptySet())
    val expandedGoals: StateFlow<Set<Long>> = _expandedGoals

    fun addGoal(title: String, description: String, parentId: Long? = null) {
        viewModelScope.launch {
            val goal = Goal(
                title = title,
                description = description,
                parentId = parentId,
                createdAt = System.currentTimeMillis()
            )
            dao.insertGoal(goal)
            parentId?.let { loadSubGoals(it) }
        }
    }

    fun updateProgress(id: Long, progress: Int) {
        viewModelScope.launch {
            dao.updateGoalProgress(id, progress.coerceIn(0, 100))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            dao.deleteGoal(goal.id)
            goal.parentId?.let { loadSubGoals(it) }
        }
    }

    fun toggleExpanded(goalId: Long) {
        val current = _expandedGoals.value.toMutableSet()
        if (goalId in current) {
            current.remove(goalId)
        } else {
            current.add(goalId)
            loadSubGoals(goalId)
        }
        _expandedGoals.value = current
    }

    private fun loadSubGoals(parentId: Long) {
        viewModelScope.launch {
            dao.getSubGoals(parentId).collect { subGoalList ->
                _subGoals.value = _subGoals.value.toMutableMap().apply {
                    put(parentId, subGoalList)
                }
            }
        }
    }
}
