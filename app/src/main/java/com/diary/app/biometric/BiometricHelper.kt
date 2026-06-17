package com.diary.app.biometric

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.security.SecureRandom

object BiometricHelper {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
    private const val KEY_PIN_HASH = "pin_lock_hash"
    private const val KEY_PIN_SALT = "pin_lock_salt"
    private const val KEY_PIN_LOCK = "pin_lock_enabled"
    private const val KEY_PIN_HINT = "pin_hint"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "lockout_until"

    fun isLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_LOCK, false) || prefs.getBoolean(KEY_PIN_LOCK, false)
    }

    fun isBiometricLockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BIOMETRIC_LOCK, false)
    }

    fun isPinLockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PIN_LOCK, false)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BIOMETRIC_LOCK, enabled)
            .apply()
    }

    // PIN management
    fun setPin(context: Context, pin: String, hint: String = "") {
        val salt = generateSalt()
        val hash = hashPinWithSalt(pin, salt)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putBoolean(KEY_PIN_LOCK, true)
            .putString(KEY_PIN_HINT, hint)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    fun removePin(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_PIN_LOCK, false)
            .remove(KEY_PIN_HINT)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null)

        // Check lockout
        if (isLockedOut(context)) return false

        val computedHash = if (salt != null) hashPinWithSalt(pin, salt) else hashPin(pin)

        return if (computedHash == storedHash) {
            // Reset failed attempts on success
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .remove(KEY_LOCKOUT_UNTIL)
                .apply()
            // Migrate to salted hash if needed
            if (salt == null) {
                val newSalt = generateSalt()
                prefs.edit()
                    .putString(KEY_PIN_HASH, hashPinWithSalt(pin, newSalt))
                    .putString(KEY_PIN_SALT, newSalt)
                    .apply()
            }
            true
        } else {
            // Increment failed attempts
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()

            // Exponential lockout: 30s, 60s, 120s, 240s...
            if (attempts >= 5) {
                val lockoutDuration = 30000L * (1L shl minOf(attempts - 5, 6))
                prefs.edit()
                    .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + lockoutDuration)
                    .apply()
            }
            false
        }
    }

    fun hasPinSet(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PIN_HASH, null) != null
    }

    fun getPinHint(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PIN_HINT, "") ?: ""
    }

    fun setPinHint(context: Context, hint: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN_HINT, hint)
            .apply()
    }

    fun isLockedOut(context: Context): Boolean {
        val lockoutUntil = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LOCKOUT_UNTIL, 0)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getLockoutRemainingSeconds(context: Context): Int {
        val lockoutUntil = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LOCKOUT_UNTIL, 0)
        val remaining = ((lockoutUntil - System.currentTimeMillis()) / 1000).toInt()
        return if (remaining > 0) remaining else 0
    }

    fun getFailedAttempts(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPinWithSalt(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPin = salt + pin
        val bytes = digest.digest(saltedPin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁日记本")
            .setSubtitle("请验证身份")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
