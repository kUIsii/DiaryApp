package com.diary.app.ui.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.health.DailyHealthData
import com.diary.app.health.HealthDataManager
import com.diary.app.health.HealthInsight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HealthUiState(
    val isAvailable: Boolean = false,
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0=今日, 1=本周, 2=本月
    val todayData: DailyHealthData? = null,
    val weeklyData: List<DailyHealthData> = emptyList(),
    val monthlyData: List<DailyHealthData> = emptyList(),
    val insights: List<HealthInsight> = emptyList(),
    val error: String? = null
)

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val healthManager = HealthDataManager(application)

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        val available = HealthDataManager.isAvailable(getApplication())
        _uiState.value = _uiState.value.copy(isAvailable = available)
        if (available) {
            checkPermission()
        }
    }

    private fun checkPermission() {
        viewModelScope.launch {
            val hasPermission = healthManager.hasAllPermissions()
            _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
            if (hasPermission) {
                loadData()
            }
        }
    }

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        loadData()
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val today = LocalDate.now()
                val todayData = healthManager.getDailyData(today)
                val weeklyData = healthManager.getWeeklyData()
                val monthlyData = healthManager.getMonthlyData()
                val insights = healthManager.analyzeHealthData(weeklyData)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    todayData = todayData,
                    weeklyData = weeklyData,
                    monthlyData = monthlyData,
                    insights = insights
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun getPermissionIntent() = healthManager.getPermissionIntent()
    fun formatSleepDuration(minutes: Long) = healthManager.formatSleepDuration(minutes)
    fun formatDistance(meters: Double) = healthManager.formatDistance(meters)
}
