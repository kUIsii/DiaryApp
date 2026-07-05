package com.diary.app.data.sync

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class SyncAccount(
    val phone: String,
    val token: String,
    val deviceId: String = ""
)

data class SyncAuthResponse(
    val token: String? = null,
    val message: String? = null,
    val error: String? = null
)

data class SyncBackupResponse(
    val message: String? = null,
    val error: String? = null,
    val data: Any? = null,
    val version: Int? = null
)

class CloudSyncManager(private val context: Context) {
    companion object {
        private const val DEFAULT_ENDPOINT = "https://diary-app-sync.2453759261.workers.dev"
        private const val PREFS_NAME = "cloud_sync"
        private const val KEY_PHONE = "phone"
        private const val KEY_TOKEN = "token"
        private const val KEY_ENDPOINT = "endpoint"
    }

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var endpoint: String
        get() = prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
        set(value) = prefs.edit().putString(KEY_ENDPOINT, value).apply()

    val savedPhone: String? get() = prefs.getString(KEY_PHONE, null)
    val savedToken: String? get() = prefs.getString(KEY_TOKEN, null)
    val isAuthenticated: Boolean get() = !savedToken.isNullOrBlank()

    private fun saveCredentials(phone: String, token: String) {
        prefs.edit().putString(KEY_PHONE, phone).putString(KEY_TOKEN, token).apply()
    }

    fun clearCredentials() {
        prefs.edit().remove(KEY_PHONE).remove(KEY_TOKEN).apply()
    }

    suspend fun register(phone: String, pin: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = httpPost("/api/register", mapOf("phone" to phone, "pin" to pin))
            val parsed = gson.fromJson(response, SyncAuthResponse::class.java)
            val token = parsed.token
            if (token.isNullOrBlank()) {
                Result.failure(Exception(parsed.error ?: "注册失败"))
            } else {
                saveCredentials(phone, token)
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(phone: String, pin: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = httpPost("/api/login", mapOf("phone" to phone, "pin" to pin))
            val parsed = gson.fromJson(response, SyncAuthResponse::class.java)
            val token = parsed.token
            if (token.isNullOrBlank()) {
                Result.failure(Exception(parsed.error ?: "登录失败"))
            } else {
                saveCredentials(phone, token)
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushBackup(data: Any): Result<String> = withContext(Dispatchers.IO) {
        val token = savedToken ?: return@withContext Result.failure(Exception("未登录"))
        try {
            val json = httpPost("/api/backup", mapOf("data" to data), token)
            val parsed = gson.fromJson(json, SyncBackupResponse::class.java)
            if (parsed.error != null) {
                Result.failure(Exception(parsed.error))
            } else {
                Result.success(parsed.message ?: "同步成功")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullBackup(): Result<String> = withContext(Dispatchers.IO) {
        val token = savedToken ?: return@withContext Result.failure(Exception("未登录"))
        try {
            val result = httpGet("/api/backup", token)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun httpPost(path: String, body: Any, token: String? = null): String {
        val conn = URL("$endpoint$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.setRequestProperty("Content-Type", "application/json")
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            conn.outputStream.use { os ->
                os.write(gson.toJson(body).toByteArray())
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return stream.use { it.reader().readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(path: String, token: String): String {
        val conn = URL("$endpoint$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.setRequestProperty("Authorization", "Bearer $token")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return stream.use { it.reader().readText() }
        } finally {
            conn.disconnect()
        }
    }
}
