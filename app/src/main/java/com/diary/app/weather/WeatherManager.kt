package com.diary.app.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.diary.app.BuildConfig
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
    private const val WEATHER_API_URL = "https://restapi.amap.com/v3/weather/weatherInfo"
    private const val CACHE_DURATION_MS = 3 * 60 * 60 * 1000L // 3 hours

    suspend fun fetchWeather(context: Context): CurrentWeather? = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.AMAP_API_KEY
            if (apiKey.isBlank()) {
                Log.w(TAG, "AMAP_API_KEY is empty")
                return@withContext null
            }

            // Get city name from location
            val cityName = getCityName(context) ?: return@withContext null

            // Call weather API
            val weather = callWeatherApi(apiKey, cityName)
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
            description.contains("雨") -> "雨天"
            description.contains("雪") -> "雨天"
            description.contains("风") || description.contains("台风") -> "大风"
            description.contains("雾") || description.contains("霾") -> "阴天"
            else -> "晴天"
        }
    }

    private fun getCityName(context: Context): String? {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "No location permission")
                return null
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)

            for (provider in providers) {
                try {
                    val location = locationManager.getLastKnownLocation(provider) ?: continue
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        // Use locality (city) first, then adminArea (province)
                        val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        if (!city.isNullOrBlank()) {
                            Log.d(TAG, "Resolved city: $city")
                            return city
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get city from provider $provider", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get city name", e)
        }
        return null
    }

    private fun callWeatherApi(apiKey: String, city: String): CurrentWeather? {
        try {
            val url = URL("$WEATHER_API_URL?key=$apiKey&city=${city}&extensions=base&output=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }

            try {
                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    Log.w(TAG, "Weather API returned $responseCode")
                    return null
                }

                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)

                if (json.getString("status") != "1") {
                    Log.w(TAG, "Weather API error: ${json.optString("info", "unknown")}")
                    return null
                }

                val lives = json.optJSONArray("lives")
                if (lives == null || lives.length() == 0) {
                    Log.w(TAG, "Weather API returned empty lives array")
                    return null
                }

                val live = lives.getJSONObject(0)
                val now = System.currentTimeMillis()

                return CurrentWeather(
                    city = live.optString("city", city),
                    weather = live.optString("weather", ""),
                    temperature = live.optString("temperature", ""),
                    windDirection = live.optString("winddirection", ""),
                    windPower = live.optString("windpower", ""),
                    humidity = live.optString("humidity", ""),
                    reportTime = live.optString("reporttime", ""),
                    fetchedAt = now
                )
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call weather API", e)
            return null
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
