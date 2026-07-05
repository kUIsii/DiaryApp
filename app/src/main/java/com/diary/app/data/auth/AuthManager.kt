package com.diary.app.data.auth

import android.content.Context
import com.diary.app.data.sync.CloudSyncManager
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

    val savedPhone: String? get() = prefs.getString(KEY_PHONE, null)
    val savedToken: String? get() = prefs.getString(KEY_TOKEN, null)
    val isLoggedIn: Boolean get() = !savedToken.isNullOrBlank()
    val isRegistered: Boolean get() = prefs.getBoolean(KEY_REGISTERED, false)

    fun restoreSession(): AuthUiState {
        val phone = savedPhone
        val token = savedToken
        return if (phone != null && token != null) {
            AuthUiState(state = AuthState.LOGGED_IN, phone = phone)
        } else if (isRegistered) {
            AuthUiState(state = AuthState.LOGGED_OUT)
        } else {
            AuthUiState(state = AuthState.LOGGED_OUT)
        }
    }

    suspend fun register(phone: String, pin: String): Result<AuthUiState> {
        if (phone.isBlank() || pin.length < 4) {
            return Result.failure(Exception("手机号或 PIN 格式不正确"))
        }
        saveLocalHash(phone, pin)
        val token = generateLocalToken(phone)
        saveAuth(phone, token)
        syncCloud(phone, pin)
        return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
    }

    suspend fun login(phone: String, pin: String): Result<AuthUiState> {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash != null) {
            if (hashPin(pin, phone) == storedHash) {
                val token = generateLocalToken(phone)
                saveAuth(phone, token)
                syncCloud(phone, pin)
                return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
            }
        }
        val cloudResult = syncManager.login(phone, pin)
        if (cloudResult.isSuccess) {
            saveLocalHash(phone, pin)
            saveAuth(phone, cloudResult.getOrThrow())
            return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        }
        return if (storedHash != null) {
            Result.failure(Exception("PIN 错误"))
        } else {
            Result.failure(Exception("未找到账号信息，请重新注册"))
        }
    }

    private suspend fun syncCloud(phone: String, pin: String) {
        try {
            val result = syncManager.register(phone, pin)
            if (result.isFailure) {
                syncManager.login(phone, pin)
            }
        } catch (_: Exception) { }
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
