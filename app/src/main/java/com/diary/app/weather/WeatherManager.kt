package com.diary.app.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class CurrentWeather(
    val city: String,
    val weather: String,
    val temperature: String,
    val windDirection: String,
    val windPower: String,
    val humidity: String,
    val reportTime: String,
    val fetchedAt: Long
)

object WeatherManager {
    private const val TAG = "WeatherManager"
    private const val PREFS_NAME = "diary_weather_prefs"
    private const val CACHE_DURATION_MS = 3 * 60 * 60 * 1000L // 3 hours

    suspend fun fetchWeather(context: Context): CurrentWeather? = withContext(Dispatchers.IO) {
        try {
            val location = getLocation(context) ?: return@withContext null
            val weather = callOpenMeteoApi(location.first, location.second, location.third)
            if (weather != null) {
                saveToCache(context, weather)
            }
            weather
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather", e)
            null
        }
    }

    suspend fun getCachedWeather(context: Context): CurrentWeather? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fetchedAt = prefs.getLong("weather_fetched_at", 0)
            if (fetchedAt == 0L) return@withContext null

            CurrentWeather(
                city = prefs.getString("weather_city", "") ?: "",
                weather = prefs.getString("weather_weather", "") ?: "",
                temperature = prefs.getString("weather_temperature", "") ?: "",
                windDirection = prefs.getString("weather_wind_direction", "") ?: "",
                windPower = prefs.getString("weather_wind_power", "") ?: "",
                humidity = prefs.getString("weather_humidity", "") ?: "",
                reportTime = prefs.getString("weather_report_time", "") ?: "",
                fetchedAt = fetchedAt
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isCacheStale(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fetchedAt = prefs.getLong("weather_fetched_at", 0)
        return System.currentTimeMillis() - fetchedAt > CACHE_DURATION_MS
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
    }

    fun mapAmapWeatherToType(description: String): String {
        return when {
            description.contains("晴") -> "晴天"
            description.contains("多云") -> "多云"
            description.contains("阴") -> "阴天"
            description.contains("雷") -> "雷暴"
            description.contains("雪") -> "雪天"
            description.contains("雨") -> "雨天"
            description.contains("风") || description.contains("台风") -> "大风"
            description.contains("雾") || description.contains("霾") -> "阴天"
            else -> "晴天"
        }
    }

    /**
     * Get location coordinates and city name.
     * Tries GPS first, then falls back to IP geolocation.
     * Returns Triple(latitude, longitude, cityName) or null.
     */
    private fun getLocation(context: Context): Triple<Double, Double, String>? {
        // Try GPS-based location if permission is available
        if (hasLocationPermission(context)) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                for (provider in providers) {
                    try {
                        if (!locationManager.isProviderEnabled(provider)) continue
                        val location = locationManager.getLastKnownLocation(provider) ?: continue
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea
                            if (!city.isNullOrBlank()) {
                                Log.d(TAG, "Resolved location via GPS: $city (${location.latitude}, ${location.longitude})")
                                return Triple(location.latitude, location.longitude, city)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "GPS provider $provider failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "GPS location failed", e)
            }
        }

        // Fallback: Use a default location (Beijing) if GPS fails
        Log.w(TAG, "Using default location (Beijing)")
        return Triple(39.9042, 116.4074, "北京")
    }

    /**
     * Call Open-Meteo API for weather data.
     * Free, no API key required.
     */
    private fun callOpenMeteoApi(latitude: Double, longitude: Double, city: String): CurrentWeather? {
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=$latitude&longitude=$longitude" +
                        "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m" +
                        "&timezone=auto"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }

            try {
                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    Log.w(TAG, "Open-Meteo API returned $responseCode")
                    return null
                }

                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val current = json.optJSONObject("current") ?: return null

                val temp = current.optDouble("temperature_2m", 0.0)
                val humidity = current.optInt("relative_humidity_2m", 0)
                val weatherCode = current.optInt("weather_code", 0)
                val windSpeed = current.optDouble("wind_speed_10m", 0.0)
                val windDirection = current.optInt("wind_direction_10m", 0)

                val weatherDesc = weatherCodeToDescription(weatherCode)
                val windDirStr = windDirectionToChinese(windDirection)
                val now = System.currentTimeMillis()

                Log.d(TAG, "Weather: $city, $weatherDesc, ${temp}°C")

                return CurrentWeather(
                    city = city,
                    weather = weatherDesc,
                    temperature = temp.toInt().toString(),
                    windDirection = windDirStr,
                    windPower = "${windSpeed.toInt()}km/h",
                    humidity = "$humidity%",
                    reportTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.CHINA).format(java.util.Date(now)),
                    fetchedAt = now
                )
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call Open-Meteo API", e)
            return null
        }
    }

    /**
     * Convert WMO weather code to Chinese description.
     */
    private fun weatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "晴"
            1 -> "晴间多云"
            2 -> "多云"
            3 -> "阴"
            45, 48 -> "雾"
            51, 53, 55 -> "小雨"
            56, 57 -> "冻雨"
            61 -> "小雨"
            63 -> "中雨"
            65 -> "大雨"
            66, 67 -> "冻雨"
            71 -> "小雪"
            73 -> "中雪"
            75 -> "大雪"
            77 -> "雪粒"
            80 -> "阵雨"
            81 -> "中雨"
            82 -> "暴雨"
            85, 86 -> "阵雪"
            95 -> "雷暴"
            96, 99 -> "雷暴冰雹"
            else -> "晴"
        }
    }

    /**
     * Convert wind direction degrees to Chinese direction.
     */
    private fun windDirectionToChinese(degrees: Int): String {
        return when {
            degrees < 22.5 || degrees >= 337.5 -> "北风"
            degrees < 67.5 -> "东北风"
            degrees < 112.5 -> "东风"
            degrees < 157.5 -> "东南风"
            degrees < 202.5 -> "南风"
            degrees < 247.5 -> "西南风"
            degrees < 292.5 -> "西风"
            else -> "西北风"
        }
    }

    private fun saveToCache(context: Context, weather: CurrentWeather) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("weather_city", weather.city)
            putString("weather_weather", weather.weather)
            putString("weather_temperature", weather.temperature)
            putString("weather_wind_direction", weather.windDirection)
            putString("weather_wind_power", weather.windPower)
            putString("weather_humidity", weather.humidity)
            putString("weather_report_time", weather.reportTime)
            putLong("weather_fetched_at", weather.fetchedAt)
            apply()
        }
    }
}
