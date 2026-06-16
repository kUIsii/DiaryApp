package com.diary.app.ui.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.health.DailyHealthData
import com.diary.app.health.HealthDataManager
import com.diary.app.health.HealthInsight
import com.diary.app.health.InsightType
import com.diary.app.health.SensorHealthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HealthUiState(
    val isAvailable: Boolean = false,
    val useSensorFallback: Boolean = false,
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
    private val sensorManager = SensorHealthManager(application)

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        val healthConnectAvailable = HealthDataManager.isAvailable(getApplication())

        if (healthConnectAvailable) {
            // Use Health Connect (Google Play Services)
            _uiState.value = _uiState.value.copy(isAvailable = true, useSensorFallback = false)
            checkPermission()
        } else if (sensorManager.isAvailable) {
            // Fallback: use step counter sensor (works on Huawei)
            _uiState.value = _uiState.value.copy(
                isAvailable = true,
                useSensorFallback = true,
                hasPermission = true
            )
            sensorManager.startListening()
            loadData()
        } else {
            // Nothing available
            _uiState.value = _uiState.value.copy(isAvailable = false)
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
                if (_uiState.value.useSensorFallback) {
                    // Sensor-based data (steps only)
                    loadSensorData()
                } else {
                    // Health Connect data (full)
                    loadHealthConnectData()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    private suspend fun loadSensorData() {
        // Save current steps before reading
        sensorManager.saveTodaySteps()

        val today = LocalDate.now()
        val todayData = sensorManager.getDailyData(today)

        // Build weekly data from saved historical data
        val weeklyData = (0..6).map { daysAgo ->
            sensorManager.getDailyData(today.minusDays(daysAgo.toLong()))
        }.reversed()

        // Build monthly data
        val monthlyData = (0..29).map { daysAgo ->
            sensorManager.getDailyData(today.minusDays(daysAgo.toLong()))
        }.reversed()

        val insights = generateSensorInsights(todayData, weeklyData)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            todayData = todayData,
            weeklyData = weeklyData,
            monthlyData = monthlyData,
            insights = insights
        )
    }

    private suspend fun loadHealthConnectData() {
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
    }

    private fun generateSensorInsights(
        today: DailyHealthData,
        weekly: List<DailyHealthData>
    ): List<HealthInsight> {
        val insights = mutableListOf<HealthInsight>()

        val validDays = weekly.filter { it.steps > 0 }
        if (validDays.isNotEmpty()) {
            val avgSteps = validDays.map { it.steps }.average()
            when {
                avgSteps < 5000 -> insights.add(
                    HealthInsight(
                        InsightType.ACTIVITY_LEVEL,
                        "活动量不足",
                        "近${validDays.size}天平均步数${avgSteps.toInt()}步",
                        "建议每天步行30分钟"
                    )
                )
                avgSteps >= 10000 -> insights.add(
                    HealthInsight(
                        InsightType.ACTIVITY_LEVEL,
                        "活动量充足",
                        "近${validDays.size}天平均步数${avgSteps.toInt()}步，保持得很好",
                        "继续保持"
                    )
                )
            }
        }

        // Add note about sensor limitations
        insights.add(
            HealthInsight(
                InsightType.CONSISTENCY,
                "数据说明",
                "当前使用手机传感器计步，仅记录步数数据",
                "连接智能手表可获取更多健康数据"
            )
        )

        return insights
    }

    fun getPermissionIntent() = healthManager.getPermissionIntent()
    fun formatSleepDuration(minutes: Long) = healthManager.formatSleepDuration(minutes)
    fun formatDistance(meters: Double) = healthManager.formatDistance(meters)

    override fun onCleared() {
        super.onCleared()
        sensorManager.saveTodaySteps()
        sensorManager.stopListening()
    }
}
