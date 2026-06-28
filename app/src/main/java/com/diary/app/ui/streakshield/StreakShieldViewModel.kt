package com.diary.app.ui.streakshield

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.StreakShield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakShieldViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _currentShield = MutableStateFlow<StreakShield?>(null)
    val currentShield: StateFlow<StreakShield?> = _currentShield.asStateFlow()

    private val _isUsed = MutableStateFlow(false)
    val isUsed: StateFlow<Boolean> = _isUsed.asStateFlow()

    init {
        loadShield()
    }

    fun loadShield() {
        viewModelScope.launch {
            val monthKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val shield = dao.getStreakShieldForMonth(monthKey)
            _currentShield.value = shield
            _isUsed.value = shield?.isUsed == true
        }
    }

    fun activateShield(savedDate: Long) {
        viewModelScope.launch {
            val monthKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val existing = dao.getStreakShieldForMonth(monthKey)
            if (existing != null && !existing.isUsed) {
                dao.updateStreakShield(existing.copy(
                    isUsed = true,
                    usedAt = System.currentTimeMillis(),
                    savedDate = savedDate
                ))
                _currentShield.value = existing.copy(isUsed = true, usedAt = System.currentTimeMillis(), savedDate = savedDate)
                _isUsed.value = true
            } else if (existing == null) {
                val newShield = StreakShield(
                    month = monthKey,
                    usedAt = System.currentTimeMillis(),
                    savedDate = savedDate,
                    isUsed = true
                )
                dao.insertStreakShield(newShield)
                _currentShield.value = newShield
                _isUsed.value = true
            }
        }
    }
}
