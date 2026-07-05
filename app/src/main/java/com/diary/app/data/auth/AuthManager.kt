package com.diary.app.data.auth

import android.content.Context
import com.diary.app.data.sync.CloudSyncManager

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
        val result = syncManager.register(phone, pin)
        return result.fold(
            onSuccess = { token ->
                saveAuth(phone, token)
                Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }

    suspend fun login(phone: String, pin: String): Result<AuthUiState> {
        val result = syncManager.login(phone, pin)
        return result.fold(
            onSuccess = { token ->
                saveAuth(phone, token)
                Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_PHONE)
            .remove(KEY_TOKEN)
            .remove(KEY_REGISTERED)
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
}
