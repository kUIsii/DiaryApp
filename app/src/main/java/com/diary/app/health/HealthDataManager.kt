package com.diary.app.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class DailyHealthData(
    val date: LocalDate,
    val steps: Long = 0,
    val heartRateAvg: Double = 0.0,
    val heartRateMin: Int = 0,
    val heartRateMax: Int = 0,
    val sleepMinutes: Long = 0,
    val caloriesBurned: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val exerciseMinutes: Long = 0
)

data class HealthInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val recommendation: String
)

enum class InsightType {
    SLEEP_QUALITY,
    HEART_RATE,
    ACTIVITY_LEVEL,
    CONSISTENCY,
    CORRELATION
}

class HealthDataManager(private val context: Context) {

    private var healthConnectClient: HealthConnectClient? = null

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )

        fun isAvailable(context: Context): Boolean {
            return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        }
    }

    init {
        if (isAvailable(context)) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return PERMISSIONS.all { it in granted }
    }

    fun getPermissionIntent(): Intent {
        val uri = Uri.parse("package:${context.packageName}")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.healthdata")
        }
    }

    suspend fun getDailyData(date: LocalDate): DailyHealthData {
        val client = healthConnectClient ?: return DailyHealthData(date)

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeFilter = TimeRangeFilter.between(startOfDay, endOfDay)

        return try {
            // Get steps
            val stepsResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeFilter
                )
            )
            val steps = stepsResponse[StepsRecord.COUNT_TOTAL] ?: 0L

            // Get heart rate
            val heartRateResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
            val heartRates = heartRateResponse.records.flatMap { it.samples.map { sample -> sample.beatsPerMinute } }
            val heartRateAvg = if (heartRates.isNotEmpty()) heartRates.average() else 0.0
            val heartRateMin = (heartRates.minOrNull() ?: 0).toInt()
            val heartRateMax = (heartRates.maxOrNull() ?: 0).toInt()

            // Get sleep
            val sleepResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
            val sleepMinutes = sleepResponse.records.sumOf { session ->
                val duration = java.time.Duration.between(session.startTime, session.endTime)
                duration.toMinutes()
            }

            // Get calories
            val caloriesResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = timeFilter
                )
            )
            val calories = caloriesResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

            // Get distance
            val distanceResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = timeFilter
                )
            )
            val distance = distanceResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0

            // Get exercise
            val exerciseResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
            val exerciseMinutes = exerciseResponse.records.sumOf { session ->
                val duration = java.time.Duration.between(session.startTime, session.endTime)
                duration.toMinutes()
            }

            DailyHealthData(
                date = date,
                steps = steps,
                heartRateAvg = heartRateAvg,
                heartRateMin = heartRateMin,
                heartRateMax = heartRateMax,
                sleepMinutes = sleepMinutes,
                caloriesBurned = calories,
                distanceMeters = distance,
                exerciseMinutes = exerciseMinutes
            )
        } catch (e: Exception) {
            Log.e("HealthData", "Error reading health data", e)
            DailyHealthData(date)
        }
    }

    suspend fun getDataRange(startDate: LocalDate, endDate: LocalDate): List<DailyHealthData> {
        val result = mutableListOf<DailyHealthData>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            result.add(getDailyData(currentDate))
            currentDate = currentDate.plusDays(1)
        }
        return result
    }

    suspend fun getWeeklyData(): List<DailyHealthData> {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        return getDataRange(weekAgo, today)
    }

    suspend fun getMonthlyData(): List<DailyHealthData> {
        val today = LocalDate.now()
        val monthAgo = today.minusDays(30)
        return getDataRange(monthAgo, today)
    }

    fun analyzeHealthData(data: List<DailyHealthData>): List<HealthInsight> {
        val insights = mutableListOf<HealthInsight>()

        if (data.isEmpty()) return insights

        // Sleep analysis
        val avgSleep = data.map { it.sleepMinutes }.average()
        when {
            avgSleep < 360 -> insights.add(
                HealthInsight(
                    InsightType.SLEEP_QUALITY,
                    "睡眠不足",
                    "近${data.size}天平均睡眠${String.format("%.1f", avgSleep / 60)}小时，低于推荐的7-8小时",
                    "建议调整作息，尽量在23点前入睡"
                )
            )
            avgSleep > 540 -> insights.add(
                HealthInsight(
                    InsightType.SLEEP_QUALITY,
                    "睡眠充足",
                    "近${data.size}天平均睡眠${String.format("%.1f", avgSleep / 60)}小时，睡眠质量良好",
                    "保持良好的睡眠习惯"
                )
            )
        }

        // Heart rate analysis
        val avgHeartRate = data.map { it.heartRateAvg }.filter { it > 0 }.average()
        if (avgHeartRate > 0) {
            when {
                avgHeartRate > 100 -> insights.add(
                    HealthInsight(
                        InsightType.HEART_RATE,
                        "心率偏高",
                        "近${data.size}天平均心率${avgHeartRate.toInt()}次/分，高于正常范围",
                        "建议适当运动，减少咖啡因摄入，如有不适请就医"
                    )
                )
                avgHeartRate < 60 -> insights.add(
                    HealthInsight(
                        InsightType.HEART_RATE,
                        "心率偏低",
                        "近${data.size}天平均心率${avgHeartRate.toInt()}次/分",
                        "运动员心率偏低是正常的，如有头晕等症状请就医"
                    )
                )
            }
        }

        // Activity analysis
        val avgSteps = data.map { it.steps }.average()
        when {
            avgSteps < 5000 -> insights.add(
                HealthInsight(
                    InsightType.ACTIVITY_LEVEL,
                    "活动量不足",
                    "近${data.size}天平均步数${avgSteps.toInt()}步，低于推荐的8000步",
                    "建议每天步行30分钟，可以分多次完成"
                )
            )
            avgSteps >= 10000 -> insights.add(
                HealthInsight(
                    InsightType.ACTIVITY_LEVEL,
                    "活动量充足",
                    "近${data.size}天平均步数${avgSteps.toInt()}步，保持得很好",
                    "继续保持，适当增加运动强度"
                )
            )
        }

        // Consistency analysis
        val daysWithSteps = data.count { it.steps > 0 }
        val consistency = daysWithSteps.toFloat() / data.size
        if (consistency < 0.5f) {
            insights.add(
                HealthInsight(
                    InsightType.CONSISTENCY,
                    "数据不完整",
                    "近${data.size}天只有${daysWithSteps}天有步数记录",
                    "确保手环/手表佩戴正确，保持设备连接"
                )
            )
        }

        return insights
    }

    fun formatSleepDuration(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}小时${mins}分钟"
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.1f公里", meters / 1000)
        } else {
            String.format("%.0f米", meters)
        }
    }
}
