package com.diary.app.data.auth

import android.content.Context
import com.diary.app.DiaryApplication
import com.diary.app.data.sync.CloudSyncManager
import com.diary.app.data.sync.SyncWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest

enum class AuthState {
    UNINITIALIZED, LOGGED_OUT, LOGGED_IN
}

data class AuthUiState(
    val state: AuthState = AuthState.UNINITIALIZED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val phone: String = ""
)

class AuthManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "diary_auth"
        private const val KEY_PHONE = "auth_phone"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REGISTERED = "auth_registered"
        private const val KEY_PIN_HASH = "auth_pin_hash"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val syncManager = CloudSyncManager(context)
    private val app = context.applicationContext as DiaryApplication
    private val gson = Gson()

    val savedPhone: String? get() = prefs.getString(KEY_PHONE, null)
    val savedToken: String? get() = prefs.getString(KEY_TOKEN, null)
    val isLoggedIn: Boolean get() = !savedToken.isNullOrBlank()
    val isRegistered: Boolean get() = prefs.getBoolean(KEY_REGISTERED, false)

    fun restoreSession(): AuthUiState {
        val phone = savedPhone
        val token = savedToken
        return if (phone != null && token != null) {
            AuthUiState(state = AuthState.LOGGED_IN, phone = phone)
        } else {
            AuthUiState(state = AuthState.LOGGED_OUT)
        }
    }

    suspend fun register(phone: String, pin: String): Result<AuthUiState> {
        if (phone.isBlank() || pin.length < 4) {
            return Result.failure(Exception("手机号或 PIN 格式不正确"))
        }
        val cloudResult = syncManager.register(phone, pin)
        return if (cloudResult.isSuccess) {
            saveLocalHash(phone, pin)
            saveAuth(phone, cloudResult.getOrThrow())
            pushData()
            Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        } else {
            saveLocalHash(phone, pin)
            val token = generateLocalToken(phone)
            saveAuth(phone, token)
            Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        }
    }

    suspend fun login(phone: String, pin: String): Result<AuthUiState> {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash != null && hashPin(pin, phone) == storedHash) {
            val token = generateLocalToken(phone)
            saveAuth(phone, token)
            pushData()
            return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        }
        val cloudResult = syncManager.login(phone, pin)
        if (cloudResult.isSuccess) {
            saveLocalHash(phone, pin)
            saveAuth(phone, cloudResult.getOrThrow())
            pushData()
            return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        }
        return if (storedHash != null) {
            Result.failure(Exception("PIN 错误"))
        } else {
            Result.failure(Exception("未找到账号信息，请重新注册"))
        }
    }

    suspend fun changePhone(newPhone: String, newPin: String): Result<AuthUiState> {
        if (newPhone.isBlank() || newPin.length < 4) {
            return Result.failure(Exception("手机号或 PIN 格式不正确"))
        }
        val currentPhone = savedPhone ?: return Result.failure(Exception("请先登录"))
        val cloudResult = syncManager.register(newPhone, newPin)
        if (cloudResult.isSuccess) {
            pushData()
            saveAuth(newPhone, cloudResult.getOrThrow())
            saveLocalHash(newPhone, newPin)
            return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = newPhone))
        }
        saveLocalHash(newPhone, newPin)
        val token = generateLocalToken(newPhone)
        saveAuth(currentPhone, token)
        return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = currentPhone))
    }

    fun syncNow() {
        SyncWorker.syncOnce(app)
    }

    suspend fun pullFromCloud(): Result<String> {
        val result = syncManager.pullBackup()
        if (result.isFailure) return Result.failure(result.exceptionOrNull() ?: Exception("云端无数据"))
        return try {
            val json = result.getOrThrow()
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val data: Map<String, Any?> = gson.fromJson(json, mapType)
            restoreFromCloud(data)
            Result.success("从云端恢复成功")
        } catch (e: Exception) {
            Result.failure(Exception("数据解析失败: ${e.message}"))
        }
    }

    private suspend fun restoreFromCloud(data: Map<String, Any?>) {
        val dao = app.database.diaryDao()
        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["tasks"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { tasks ->
            tasks.forEach { t ->
                runCatching {
                    val item = com.diary.app.data.TodoItem(
                        title = t["title"] as? String ?: "",
                        isCompleted = t["isCompleted"] as? Boolean ?: false,
                        priority = (t["priority"] as? Double)?.toInt() ?: 0,
                        dueDate = (t["dueDate"] as? Double)?.toLong(),
                        category = t["category"] as? String ?: "task",
                        tags = t["tags"] as? String ?: "",
                        sortOrder = (t["sortOrder"] as? Double)?.toInt() ?: 0
                    )
                    dao.insertTodo(item)
                }
            }
        }
    }

    private fun pushData() {
        SyncWorker.syncOnce(app)
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_PHONE)
            .remove(KEY_TOKEN)
            .remove(KEY_REGISTERED)
            .remove(KEY_PIN_HASH)
            .apply()
        syncManager.clearCredentials()
    }

    private fun saveAuth(phone: String, token: String) {
        prefs.edit()
            .putString(KEY_PHONE, phone)
            .putString(KEY_TOKEN, token)
            .putBoolean(KEY_REGISTERED, true)
            .apply()
    }

    private fun saveLocalHash(phone: String, pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin, phone)).apply()
    }

    private fun generateLocalToken(phone: String): String {
        val raw = "$phone:${System.currentTimeMillis()}:${Math.random()}"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, phone: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$phone:$pin".toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
