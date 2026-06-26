package com.diary.app.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureConfigStore {

    private const val PREFS_NAME = "secure_config_store"
    private const val FALLBACK_PREFS_NAME = "secure_config_store_fallback"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"

    fun getString(context: Context, key: String): String? {
        return prefs(context).getString(key, null)
    }

    fun setString(context: Context, key: String, value: String?) {
        prefs(context).edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return prefs(context).getBoolean(key, defaultValue)
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return prefs(context).getInt(key, defaultValue)
    }

    fun setInt(context: Context, key: String, value: Int) {
        prefs(context).edit().putInt(key, value).apply()
    }

    fun getLong(context: Context, key: String, defaultValue: Long = 0L): Long {
        return prefs(context).getLong(key, defaultValue)
    }

    fun setLong(context: Context, key: String, value: Long) {
        prefs(context).edit().putLong(key, value).apply()
    }

    fun remove(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }

    private fun prefs(context: Context): SharedPreferences {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyGenParameterSpec(
                        KeyGenParameterSpec.Builder(
                            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build()
                    )
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } else {
                context.applicationContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }.getOrElse {
            android.util.Log.w(
                "SecureConfigStore",
                "Falling back to non-encrypted prefs; provider=$KEYSTORE_PROVIDER transformation=$AES_TRANSFORMATION",
                it
            )
            context.applicationContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}
