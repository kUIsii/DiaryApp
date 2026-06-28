package com.diary.app.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.diary.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherAlert(
    val province: String,
    val city: String,
    val level: String,
    val type: String,
    val text: String
)

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
    val dailyForecast: List<DailyForecast> = emptyList(),
    val alerts: List<WeatherAlert> = emptyList()
)

object WeatherManager {
    private const val TAG = "WeatherManager"
    private const val PREFS_NAME = "diary_weather_prefs"
    private const val CACHE_DURATION_MS = 1 * 60 * 60 * 1000L

    suspend fun fetchWeather(context: Context): CurrentWeather? = withContext(Dispatchers.IO) {
        try {
            val (adcode, cityName) = getAdcode(context) ?: return@withContext null
            val weather = callAmapWeatherApi(adcode, cityName) ?: return@withContext null
            saveToCache(context, weather)
            weather
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather", e); null
        }
    }

    suspend fun getCachedWeather(context: Context): CurrentWeather? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fetchedAt = prefs.getLong("weather_fetched_at", 0)
            if (fetchedAt == 0L) return@withContext null
            fun jsonArr(key: String): JSONArray? = prefs.getString(key, null)?.let { try { JSONArray(it) } catch (_: Exception) { null } }

            val hourlyList = jsonArr("weather_hourly_forecast")?.let { arr ->
                (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
                    HourlyForecast(o.optString("time",""), o.optString("weather",""), o.optString("temperature",""), o.optInt("weatherCode",0)) }
            } ?: emptyList()

            val dailyList = jsonArr("weather_daily_forecast")?.let { arr ->
                (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
                    DailyForecast(o.optString("date",""), o.optString("dayOfWeek",""), o.optString("weather",""),
                        o.optString("tempMax",""), o.optString("tempMin",""), o.optInt("weatherCode",0)) }
            } ?: emptyList()

            val alertsList = jsonArr("weather_alerts")?.let { arr ->
                (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
                    WeatherAlert(o.optString("province",""), o.optString("city",""), o.optString("level",""),
                        o.optString("type",""), o.optString("text","")) }
            } ?: emptyList()

            CurrentWeather(
                city = prefs.getString("weather_city","") ?: "",
                weather = prefs.getString("weather_weather","") ?: "",
                temperature = prefs.getString("weather_temperature","") ?: "",
                windDirection = prefs.getString("weather_wind_direction","") ?: "",
                windPower = prefs.getString("weather_wind_power","") ?: "",
                humidity = prefs.getString("weather_humidity","") ?: "",
                reportTime = prefs.getString("weather_report_time","") ?: "",
                fetchedAt = fetchedAt,
                feelsLike = prefs.getString("weather_feels_like","") ?: "",
                uvIndex = prefs.getString("weather_uv_index","") ?: "",
                hourlyForecast = hourlyList,
                dailyForecast = dailyList,
                alerts = alertsList
            )
        } catch (_: Exception) { null }
    }

    fun isCacheStale(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return System.currentTimeMillis() - prefs.getLong("weather_fetched_at", 0) > CACHE_DURATION_MS
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun mapAmapWeatherToType(description: String): String = when {
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

    private fun getAdcode(context: Context): Pair<String, String>? {
        if (hasLocationPermission(context)) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
                    try {
                        if (!locationManager.isProviderEnabled(provider)) continue
                        val location = locationManager.getLastKnownLocation(provider) ?: continue
                        val apiKey = BuildConfig.AMAP_API_KEY
                        if (apiKey.isNotBlank()) {
                            val conn = URL("https://restapi.amap.com/v3/geocode/regeo?key=$apiKey&location=${location.longitude},${location.latitude}&extensions=base")
                                .openConnection() as HttpURLConnection
                            conn.requestMethod = "GET"; conn.connectTimeout = 10000; conn.readTimeout = 10000
                            try {
                                if (conn.responseCode == 200) {
                                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                                    if (json.optString("status") == "1") {
                                        val ac = json.optJSONObject("regeocode")?.optJSONObject("addressComponent")
                                        val adcode = ac?.optString("adcode") ?: ""
                                        val city = ac?.optString("city") ?: ""
                                        val district = ac?.optString("district") ?: ""
                                        if (adcode.isNotBlank()) {
                                            val name = if (city.isNotBlank() && district.isNotBlank() && city != district) "$city$district" else city.ifBlank { district }.ifBlank { "未知" }
                                            return Pair(adcode, name)
                                        }
                                    }
                                }
                            } finally { conn.disconnect() }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        return Pair("110101", "北京市东城区")
    }

    private fun callAmapWeatherApi(adcode: String, cityName: String): CurrentWeather? {
        val apiKey = BuildConfig.AMAP_API_KEY
        if (apiKey.isBlank()) { Log.e(TAG, "AMAP API key is empty"); return null }
        try {
            val baseUrl = "https://restapi.amap.com/v3/weather/weatherInfo"

            val realtimeBody = run {
                val c = URL("$baseUrl?key=$apiKey&city=$adcode&extensions=base").openConnection() as HttpURLConnection
                c.requestMethod = "GET"; c.connectTimeout = 10000; c.readTimeout = 10000
                try { if (c.responseCode != 200) return null; c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
            }
            val realtimeJson = JSONObject(realtimeBody)
            if (realtimeJson.optString("status") != "1") return null
            val lives = realtimeJson.optJSONArray("lives") ?: return null
            if (lives.length() == 0) return null
            val live = lives.getJSONObject(0)
            val now = System.currentTimeMillis()

            val forecastBody = run {
                val c = URL("$baseUrl?key=$apiKey&city=$adcode&extensions=all").openConnection() as HttpURLConnection
                c.requestMethod = "GET"; c.connectTimeout = 10000; c.readTimeout = 10000
                try { if (c.responseCode != 200) return null; c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
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
                        val dateStr = cast.optString("date","")
                        val weekNum = cast.optInt("week",0)
                        dailyList.add(DailyForecast(dateStr, if (weekNum in 1..7) dayNames[weekNum] else "",
                            cast.optString("dayweather",""), cast.optString("daytemp",""),
                            cast.optString("nighttemp",""), weatherDescriptionToCode(cast.optString("dayweather",""))))
                        if (i == 0) {
                            val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                            hourlyList.add(HourlyForecast(String.format("%02d:00",h), live.optString("weather",""),
                                live.optString("temperature",""), weatherDescriptionToCode(live.optString("weather",""))))
                            hourlyList.add(HourlyForecast(String.format("%02d:00",(h+3)%24),
                                cast.optString("dayweather",""), cast.optString("daytemp",""), weatherDescriptionToCode(cast.optString("dayweather",""))))
                            hourlyList.add(HourlyForecast(String.format("%02d:00",(h+6)%24),
                                cast.optString("nightweather",""), cast.optString("nighttemp",""), weatherDescriptionToCode(cast.optString("nightweather",""))))
                        }
                    }
                }
            }

            // Parse real weather warnings from AMAP
            val alertsList = mutableListOf<WeatherAlert>()
            val warnings = forecastJson.optJSONArray("warnings")
            if (warnings != null) {
                for (i in 0 until warnings.length()) {
                    val w = warnings.getJSONObject(i)
                    alertsList.add(WeatherAlert(
                        province = w.optString("province",""),
                        city = w.optString("city",""),
                        level = w.optString("level",""),
                        type = w.optString("type",""),
                        text = w.optString("text","")
                    ))
                }
            }

            Log.d(TAG, "Weather: $cityName, ${live.optString("weather","")}, ${live.optString("temperature","")}C, ${alertsList.size} alerts")

            return CurrentWeather(
                city = cityName,
                weather = live.optString("weather",""),
                temperature = live.optString("temperature",""),
                windDirection = "${live.optString("winddirection","")}风",
                windPower = "${live.optString("windpower","")}级",
                humidity = "${live.optString("humidity","")}%",
                reportTime = live.optString("reporttime",""),
                fetchedAt = now,
                hourlyForecast = hourlyList,
                dailyForecast = dailyList,
                alerts = alertsList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call AMAP Weather API", e); return null
        }
    }

    private fun weatherDescriptionToCode(description: String): Int = when {
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

    private fun saveToCache(context: Context, weather: CurrentWeather) {
        fun toJsonArr(list: List<HourlyForecast>): String = JSONArray().apply {
            list.forEach { h -> put(JSONObject().apply {
                put("time", h.time); put("weather", h.weather); put("temperature", h.temperature); put("weatherCode", h.weatherCode) }) }
        }.toString()
        fun dailyToJsonArr(list: List<DailyForecast>): String = JSONArray().apply {
            list.forEach { d -> put(JSONObject().apply {
                put("date", d.date); put("dayOfWeek", d.dayOfWeek); put("weather", d.weather)
                put("tempMax", d.tempMax); put("tempMin", d.tempMin); put("weatherCode", d.weatherCode) }) }
        }.toString()
        fun alertToJsonArr(list: List<WeatherAlert>): String = JSONArray().apply {
            list.forEach { a -> put(JSONObject().apply {
                put("province", a.province); put("city", a.city); put("level", a.level)
                put("type", a.type); put("text", a.text) }) }
        }.toString()

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
            putString("weather_hourly_forecast", toJsonArr(weather.hourlyForecast))
            putString("weather_daily_forecast", dailyToJsonArr(weather.dailyForecast))
            putString("weather_alerts", alertToJsonArr(weather.alerts))
            apply()
        }
    }
}
