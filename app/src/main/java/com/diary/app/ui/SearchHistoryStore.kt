package com.diary.app.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SearchHistoryStore {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_SEMANTIC_HISTORY = "search_history_semantic"
    private const val KEY_HOME_HISTORY = "search_history_home"
    private const val MAX_ITEMS = 5
    private val gson = Gson()

    fun getSemanticHistory(context: Context): List<String> {
        return getHistory(context, KEY_SEMANTIC_HISTORY)
    }

    fun setSemanticHistory(context: Context, history: List<String>) {
        saveHistory(context, KEY_SEMANTIC_HISTORY, history)
    }

    fun getHomeHistory(context: Context): List<String> {
        return getHistory(context, KEY_HOME_HISTORY)
    }

    fun setHomeHistory(context: Context, history: List<String>) {
        saveHistory(context, KEY_HOME_HISTORY, history)
    }

    fun addToHistory(current: List<String>, query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return current
        val mutable = current.toMutableList()
        mutable.remove(trimmed)
        mutable.add(0, trimmed)
        if (mutable.size > MAX_ITEMS) mutable.removeAt(mutable.lastIndex)
        return mutable
    }

    private fun getHistory(context: Context, key: String): List<String> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(context: Context, key: String, history: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, gson.toJson(history))
            .apply()
    }
}
