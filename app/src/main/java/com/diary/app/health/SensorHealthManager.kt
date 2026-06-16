package com.diary.app.health

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId

/**
 * Fallback health data manager using Android's built-in step counter sensor.
 * Works on Huawei and other devices without Google Play Services.
 * Only provides step counting - no sleep/heart rate/calories data.
 */
class SensorHealthManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs: SharedPreferences = context.getSharedPreferences("sensor_health", Context.MODE_PRIVATE)

    private var initialSteps: Float = -1f
    private var currentSteps: Float = 0f
    private var lastBootSteps: Float = 0f

    val isAvailable: Boolean
        get() = stepCounterSensor != null

    fun startListening() {
        stepCounterSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            // Restore last known boot steps
            lastBootSteps = prefs.getFloat("last_boot_steps", 0f)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0]

            // Detect device reboot (steps since boot reset)
            if (totalStepsSinceBoot < lastBootSteps) {
                // Device was rebooted, save accumulated steps
                val accumulated = prefs.getFloat("accumulated_steps", 0f) + lastBootSteps
                prefs.edit().putFloat("accumulated_steps", accumulated).apply()
            }
            lastBootSteps = totalStepsSinceBoot
            prefs.edit().putFloat("last_boot_steps", lastBootSteps).apply()

            if (initialSteps < 0) {
                initialSteps = totalStepsSinceBoot
            }
            currentSteps = totalStepsSinceBoot
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Get today's step count. Returns -1 if sensor data not yet available.
     */
    fun getTodaySteps(): Long {
        if (initialSteps < 0) return -1
        val accumulated = prefs.getFloat("accumulated_steps", 0f)
        return (accumulated + currentSteps).toLong()
    }

    /**
     * Get step count for a specific date. Since the step counter sensor only
     * provides cumulative data since last boot, we can only accurately track
     * today's steps. For historical data, return what we saved.
     */
    fun getStepsForDate(date: LocalDate): Long {
        val today = LocalDate.now()
        if (date == today) {
            return getTodaySteps()
        }
        // Return saved historical data if available
        val key = "steps_${date}"
        return prefs.getLong(key, -1L)
    }

    /**
     * Save today's step count (call this periodically or when app goes to background)
     */
    fun saveTodaySteps() {
        val today = LocalDate.now()
        val steps = getTodaySteps()
        if (steps >= 0) {
            prefs.edit().putLong("steps_$today", steps).apply()
        }
    }

    /**
     * Build a DailyHealthData from sensor data.
     * Only steps are available; other metrics will be zero.
     */
    fun getDailyData(date: LocalDate): DailyHealthData {
        val steps = getStepsForDate(date)
        return DailyHealthData(
            date = date,
            steps = if (steps >= 0) steps else 0,
            heartRateAvg = 0.0,
            heartRateMin = 0,
            heartRateMax = 0,
            sleepMinutes = 0,
            caloriesBurned = 0.0,
            distanceMeters = 0.0,
            exerciseMinutes = 0
        )
    }

    /**
     * Check if we have any sensor data available
     */
    fun hasData(): Boolean {
        return getTodaySteps() >= 0
    }

    companion object {
        /**
         * Check if step counter sensor is available on this device
         */
        fun isStepCounterAvailable(context: Context): Boolean {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        }
    }
}
