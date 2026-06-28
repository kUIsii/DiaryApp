package com.diary.app.ui.monthlychallenge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.ChallengeDailyLog
import com.diary.app.data.MonthlyChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class MonthlyChallengeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _currentChallenge = MutableStateFlow<MonthlyChallenge?>(null)
    val currentChallenge: StateFlow<MonthlyChallenge?> = _currentChallenge.asStateFlow()

    private val _dailyLogs = MutableStateFlow<List<ChallengeDailyLog>>(emptyList())
    val dailyLogs: StateFlow<List<ChallengeDailyLog>> = _dailyLogs.asStateFlow()

    init {
        loadChallenge()
        ensureCurrentChallenge()
    }

    fun loadChallenge() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val challenge = dao.getMonthlyChallenge(now.year, now.monthValue)
            _currentChallenge.value = challenge
            challenge?.let {
                dao.getChallengeDailyLogs(it.id).collect { logs ->
                    _dailyLogs.value = logs
                }
            }
        }
    }

    fun toggleDay(date: Long) {
        viewModelScope.launch {
            val challenge = _currentChallenge.value ?: return@launch
            val existing = dao.getChallengeDailyLog(challenge.id, date)
            if (existing != null) {
                dao.updateChallengeDailyLog(existing.copy(completed = !existing.completed))
            } else {
                dao.insertChallengeDailyLog(ChallengeDailyLog(
                    challengeId = challenge.id,
                    date = date,
                    completed = true
                ))
            }
            // 更新完成天数
            val logs = dao.getChallengeDailyLogs(challenge.id).first()
            val completedCount = logs.count { it.completed }
            dao.updateMonthlyChallenge(challenge.copy(completedDays = completedCount))
        }
    }

    private fun ensureCurrentChallenge() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val existing = dao.getMonthlyChallenge(now.year, now.monthValue)
            if (existing == null) {
                val challenge = MonthlyChallenge(
                    title = "每日一张照片配文字",
                    description = "用镜头捕捉日常，用文字定格瞬间。完成率超过60%即算达成。",
                    year = now.year,
                    month = now.monthValue,
                    targetDays = 20,
                    status = "active"
                )
                dao.insertMonthlyChallenge(challenge)
                _currentChallenge.value = challenge
            }
        }
    }
}
