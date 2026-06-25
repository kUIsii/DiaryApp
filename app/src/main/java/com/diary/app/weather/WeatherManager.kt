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

data class HourlyForecast(
    val time: String,
    val weather: String,
    val temperature: String,
    val weatherCode: Int
)

data class DailyForecast(
    val date: String,
    val dayOfWeek: String,
    val weather: String,
    val tempMax: String,
    val tempMin: String,
    val weatherCode: Int
)

data class CurrentWeather(
    val city: String,
    val weather: String,
    val temperature: String,
    val windDirection: String,
    val windPower: String,
    val humidity: String,
    val reportTime: String,
    val fetchedAt: Long,
    val feelsLike: String = "",
    val uvIndex: String = "",
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList()
)

object WeatherManager {
    private const val TAG = "WeatherManager"
    private const val PREFS_NAME = "diary_weather_prefs"
    private const val CACHE_DURATION_MS = 1 * 60 * 60 * 1000L // 1 hour

    suspend fun fetchWeather(context: Context): CurrentWeather? = withContext(Dispatchers.IO) {
        try {
            val locationInfo = getAdcode(context) ?: return@withContext null
            val (adcode, cityName) = locationInfo
            val weather = callAmapWeatherApi(adcode, cityName)
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

            val hourlyJson = prefs.getString("weather_hourly_forecast", null)
            val hourlyList = if (!hourlyJson.isNullOrBlank()) {
                try {
                    val arr = org.json.JSONArray(hourlyJson)
                    (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        HourlyForecast(
                            time = obj.optString("time", ""),
                            weather = obj.optString("weather", ""),
                            temperature = obj.optString("temperature", ""),
                            weatherCode = obj.optInt("weatherCode", 0)
                        )
                    }
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            val dailyJson = prefs.getString("weather_daily_forecast", null)
            val dailyList = if (!dailyJson.isNullOrBlank()) {
                try {
                    val arr = org.json.JSONArray(dailyJson)
                    (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        DailyForecast(
                            date = obj.optString("date", ""),
                            dayOfWeek = obj.optString("dayOfWeek", ""),
                            weather = obj.optString("weather", ""),
                            tempMax = obj.optString("tempMax", ""),
                            tempMin = obj.optString("tempMin", ""),
                            weatherCode = obj.optInt("weatherCode", 0)
                        )
                    }
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            CurrentWeather(
                city = prefs.getString("weather_city", "") ?: "",
                weather = prefs.getString("weather_weather", "") ?: "",
                temperature = prefs.getString("weather_temperature", "") ?: "",
                windDirection = prefs.getString("weather_wind_direction", "") ?: "",
                windPower = prefs.getString("weather_wind_power", "") ?: "",
                humidity = prefs.getString("weather_humidity", "") ?: "",
                reportTime = prefs.getString("weather_report_time", "") ?: "",
                fetchedAt = fetchedAt,
                feelsLike = prefs.getString("weather_feels_like", "") ?: "",
                uvIndex = prefs.getString("weather_uv_index", "") ?: "",
                hourlyForecast = hourlyList,
                dailyForecast = dailyList
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
     * Get adcode for current location.
     * Uses AMAP geocoding API to get accurate adcode.
     */
    private fun getAdcode(context: Context): Pair<String, String>? {
        if (hasLocationPermission(context)) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                for (provider in providers) {
                    try {
                        if (!locationManager.isProviderEnabled(provider)) continue
                        val location = locationManager.getLastKnownLocation(provider) ?: continue

                        // Use AMAP geocoding API for accurate adcode
                        val apiKey = BuildConfig.AMAP_API_KEY
                        if (apiKey.isNotBlank()) {
                            val geocodeUrl = URL(
                                "https://restapi.amap.com/v3/geocode/regeo" +
                                        "?key=$apiKey" +
                                        "&location=${location.longitude},${location.latitude}" +
                                        "&extensions=base"
                            )
                            val conn = (geocodeUrl.openConnection() as HttpURLConnection).apply {
                                requestMethod = "GET"
                                connectTimeout = 10000
                                readTimeout = 10000
                            }

                            try {
                                val code = conn.responseCode
                                if (code == 200) {
                                    val body = conn.inputStream.bufferedReader().readText()
                                    val json = JSONObject(body)
                                    if (json.optString("status") == "1") {
                                        val regeocode = json.optJSONObject("regeocode")
                                        val addressComponent = regeocode?.optJSONObject("addressComponent")
                                        val adcode = addressComponent?.optString("adcode") ?: ""
                                        val city = addressComponent?.optString("city") ?: ""
                                        val district = addressComponent?.optString("district") ?: ""

                                        if (adcode.isNotBlank()) {
                                            val cityName = if (city.isNotBlank() && district.isNotBlank() && city != district) {
                                                "$city$district"
                                            } else {
                                                city.ifBlank { district }.ifBlank { "未知" }
                                            }
                                            Log.d(TAG, "AMAP geocoding: $adcode ($cityName)")
                                            return Pair(adcode, cityName)
                                        }
                                    }
                                }
                            } finally {
                                conn.disconnect()
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

        // Fallback: Beijing
        Log.w(TAG, "Using default location (Beijing)")
        return Pair("110101", "北京市东城区")
    }

    /**
     * Call AMAP Weather API.
     * Real-time weather: extensions=base
     * Forecast: extensions=all
     */
    private fun callAmapWeatherApi(adcode: String, cityName: String): CurrentWeather? {
        val apiKey = BuildConfig.AMAP_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "AMAP API key is empty")
            return null
        }

        try {
            // Fetch real-time weather
            val baseUrl = "https://restapi.amap.com/v3/weather/weatherInfo"
            val realtimeUrl = URL("$baseUrl?key=$apiKey&city=$adcode&extensions=base")
            val realtimeConn = (realtimeUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }

            val realtimeBody = try {
                val code = realtimeConn.responseCode
                if (code != 200) {
                    Log.w(TAG, "AMAP realtime API returned $code")
                    return null
                }
                realtimeConn.inputStream.bufferedReader().readText()
            } finally {
                realtimeConn.disconnect()
            }

            val realtimeJson = JSONObject(realtimeBody)
            if (realtimeJson.optString("status") != "1") {
                Log.w(TAG, "AMAP realtime API error: ${realtimeJson.optString("info")}")
                return null
            }

            val lives = realtimeJson.optJSONArray("lives")
            if (lives == null || lives.length() == 0) {
                Log.w(TAG, "No lives data in response")
                return null
            }

            val live = lives.getJSONObject(0)
            val city = live.optString("city", "")
            val weather = live.optString("weather", "")
            val temperature = live.optString("temperature", "")
            val windDirection = live.optString("winddirection", "")
            val windPower = live.optString("windpower", "")
            val humidity = live.optString("humidity", "")
            val reportTime = live.optString("reporttime", "")
            val now = System.currentTimeMillis()

            // Fetch forecast
            val forecastUrl = URL("$baseUrl?key=$apiKey&city=$adcode&extensions=all")
            val forecastConn = (forecastUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }

            val forecastBody = try {
                val code = forecastConn.responseCode
                if (code != 200) {
                    Log.w(TAG, "AMAP forecast API returned $code")
                    return null
                }
                forecastConn.inputStream.bufferedReader().readText()
            } finally {
                forecastConn.disconnect()
            }

            val forecastJson = JSONObject(forecastBody)
            val forecasts = forecastJson.optJSONArray("forecasts")
            val dailyList = mutableListOf<DailyForecast>()
            val hourlyList = mutableListOf<HourlyForecast>()

            if (forecasts != null && forecasts.length() > 0) {
                val forecast = forecasts.getJSONObject(0)
                val casts = forecast.optJSONArray("casts")
                if (casts != null) {
                    val dayNames = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
                    for (i in 0 until casts.length()) {
                        val cast = casts.getJSONObject(i)
                        val dateStr = cast.optString("date", "")
                        val weekNum = cast.optInt("week", 0)
                        val dayOfWeek = if (weekNum in 1..7) dayNames[weekNum] else ""

                        dailyList.add(DailyForecast(
                            date = dateStr,
                            dayOfWeek = dayOfWeek,
                            weather = cast.optString("dayweather", ""),
                            tempMax = cast.optString("daytemp", ""),
                            tempMin = cast.optString("nighttemp", ""),
                            weatherCode = weatherDescriptionToCode(cast.optString("dayweather", ""))
                        ))

                        // Create day/night hourly entries
                        if (i == 0) {
                            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                            hourlyList.add(HourlyForecast(
                                time = String.format("%02d:00", hour),
                                weather = weather,
                                temperature = temperature,
                                weatherCode = weatherDescriptionToCode(weather)
                            ))
                            hourlyList.add(HourlyForecast(
                                time = String.format("%02d:00", (hour + 3) % 24),
                                weather = cast.optString("dayweather", ""),
                                temperature = cast.optString("daytemp", ""),
                                weatherCode = weatherDescriptionToCode(cast.optString("dayweather", ""))
                            ))
                            hourlyList.add(HourlyForecast(
                                time = String.format("%02d:00", (hour + 6) % 24),
                                weather = cast.optString("nightweather", ""),
                                temperature = cast.optString("nighttemp", ""),
                                weatherCode = weatherDescriptionToCode(cast.optString("nightweather", ""))
                            ))
                        }
                    }
                }
            }

            Log.d(TAG, "Weather: $cityName, $weather, ${temperature}°C")

            return CurrentWeather(
                city = cityName,
                weather = weather,
                temperature = temperature,
                windDirection = "${windDirection}风",
                windPower = "${windPower}级",
                humidity = "$humidity%",
                reportTime = reportTime,
                fetchedAt = now,
                hourlyForecast = hourlyList,
                dailyForecast = dailyList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call AMAP Weather API", e)
            return null
        }
    }

    /**
     * Convert Chinese weather description to a numeric code for icon mapping.
     */
    private fun weatherDescriptionToCode(description: String): Int {
        return when {
            description.contains("晴") -> 0
            description.contains("多云") -> 2
            description.contains("阴") -> 3
            description.contains("雷") -> 95
            description.contains("小雨") -> 61
            description.contains("中雨") -> 63
            description.contains("大雨") -> 65
            description.contains("暴雨") -> 82
            description.contains("小雪") -> 71
            description.contains("中雪") -> 73
            description.contains("大雪") -> 75
            description.contains("雾") -> 45
            description.contains("霾") -> 48
            description.contains("风") -> 1
            else -> 0
        }
    }

    private fun saveToCache(context: Context, weather: CurrentWeather) {
        val hourlyArray = org.json.JSONArray()
        weather.hourlyForecast.forEach { h ->
            org.json.JSONObject().apply {
                put("time", h.time)
                put("weather", h.weather)
                put("temperature", h.temperature)
                put("weatherCode", h.weatherCode)
            }.let { hourlyArray.put(it) }
        }

        val dailyArray = org.json.JSONArray()
        weather.dailyForecast.forEach { d ->
            org.json.JSONObject().apply {
                put("date", d.date)
                put("dayOfWeek", d.dayOfWeek)
                put("weather", d.weather)
                put("tempMax", d.tempMax)
                put("tempMin", d.tempMin)
                put("weatherCode", d.weatherCode)
            }.let { dailyArray.put(it) }
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("weather_city", weather.city)
            putString("weather_weather", weather.weather)
            putString("weather_temperature", weather.temperature)
            putString("weather_wind_direction", weather.windDirection)
            putString("weather_wind_power", weather.windPower)
            putString("weather_humidity", weather.humidity)
            putString("weather_report_time", weather.reportTime)
            putLong("weather_fetched_at", weather.fetchedAt)
            putString("weather_feels_like", weather.feelsLike)
            putString("weather_uv_index", weather.uvIndex)
            putString("weather_hourly_forecast", hourlyArray.toString())
            putString("weather_daily_forecast", dailyArray.toString())
            apply()
        }
    }
}
