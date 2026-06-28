package com.diary.app.ui.ambientsound

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SoundPreset(
    val name: String,
    val activeTypes: List<String>,
    val volumes: Map<String, Double>  // type key -> volume
) {
    fun toActiveTypesSet(): Set<AmbientSoundType> =
        activeTypes.mapNotNull { s -> AmbientSoundType.entries.find { it.key == s } }.toSet()

    fun toVolumesMap(): Map<AmbientSoundType, Float> =
        volumes.mapKeys { (k, _) -> AmbientSoundType.entries.find { it.key == k }!! }
            .mapValues { (_, v) -> v.toFloat() }
}

object PresetStorage {
    private const val PREFS_NAME = "ambient_sound_presets"
    private const val KEY_PRESETS = "presets"
    private const val MAX_PRESETS = 10

    private val gson = Gson()

    fun load(context: Context): List<SoundPreset> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PRESETS, null) ?: return defaultPresets()
        val type = object : TypeToken<List<SoundPreset>>() {}.type
        return try {
            val saved: List<SoundPreset> = gson.fromJson(json, type)
            (saved + defaultPresets()).distinctBy { it.name }
        } catch (_: Exception) {
            defaultPresets()
        }
    }

    fun save(context: Context, preset: SoundPreset) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = load(context).toMutableList()
        val existing = list.indexOfFirst { it.name == preset.name }
        if (existing >= 0) list[existing] = preset else list.add(preset)
        val trimmed = list.takeLast(MAX_PRESETS)
        prefs.edit().putString(KEY_PRESETS, gson.toJson(trimmed)).apply()
    }

    fun delete(context: Context, name: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = load(context).filter { it.name != name }
        prefs.edit().putString(KEY_PRESETS, gson.toJson(list)).apply()
    }

    private fun defaultPresets(): List<SoundPreset> = listOf(
        SoundPreset("雨天学习", listOf("white_noise", "rain"), mapOf("white_noise" to 0.4, "rain" to 0.6)),
        SoundPreset("森林冥想", listOf("forest", "ocean"), mapOf("forest" to 0.6, "ocean" to 0.4)),
        SoundPreset("咖啡厅工作", listOf("cafe", "white_noise"), mapOf("cafe" to 0.5, "white_noise" to 0.3))
    )
}
