package com.diary.app.ui.streakshield

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ShieldItem(
    val id: String = UUID.randomUUID().toString(),
    val source: String = "monthly",
    val obtainedAt: Long = System.currentTimeMillis(),
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    val savedDate: Long? = null,
    val triggerType: String? = null
)

data class ShieldAchievement(
    val id: String,
    val title: String,
    val description: String,
    val unlockedAt: Long = System.currentTimeMillis()
)

data class ShieldHistoryGroup(
    val month: String,
    val items: List<ShieldItem>
)

class StreakShieldViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val prefs = application.getSharedPreferences("streak_shield", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    private val _monthGrid = MutableStateFlow<List<Pair<Int?, Boolean>>>(emptyList())
    val monthGrid: StateFlow<List<Pair<Int?, Boolean>>> = _monthGrid.asStateFlow()

    private val _inventory = MutableStateFlow<List<ShieldItem>>(emptyList())
    val inventory: StateFlow<List<ShieldItem>> = _inventory.asStateFlow()

    private val _history = MutableStateFlow<List<ShieldHistoryGroup>>(emptyList())
    val history: StateFlow<List<ShieldHistoryGroup>> = _history.asStateFlow()

    private val _achievements = MutableStateFlow<List<ShieldAchievement>>(emptyList())
    val achievements: StateFlow<List<ShieldAchievement>> = _achievements.asStateFlow()

    private val _autoProtectEnabled = MutableStateFlow(true)
    val autoProtectEnabled: StateFlow<Boolean> = _autoProtectEnabled.asStateFlow()

    private val _notifyEnabled = MutableStateFlow(true)
    val notifyEnabled: StateFlow<Boolean> = _notifyEnabled.asStateFlow()

    init {
        loadSettings()
        loadInventory()
        loadAchievements()
        viewModelScope.launch(Dispatchers.IO) {
            loadStreakData()
            checkMonthlyGrant()
            autoDetectAndProtect()
            buildHistory()
            checkAchievements()
        }
    }

    private fun loadSettings() {
        _autoProtectEnabled.value = prefs.getBoolean("shield_auto_protect", true)
        _notifyEnabled.value = prefs.getBoolean("shield_notify", true)
    }

    private fun loadInventory() {
        val json = prefs.getString("shield_inventory", null) ?: return
        val type = object : TypeToken<List<ShieldItem>>() {}.type
        _inventory.value = try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    private fun saveInventory() {
        prefs.edit().putString("shield_inventory", gson.toJson(_inventory.value)).apply()
    }

    private fun loadAchievements() {
        val json = prefs.getString("shield_achievements", null) ?: return
        val type = object : TypeToken<List<ShieldAchievement>>() {}.type
        _achievements.value = try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    private fun saveAchievements() {
        prefs.edit().putString("shield_achievements", gson.toJson(_achievements.value)).apply()
    }

    private suspend fun loadStreakData() {
        _currentStreak.value = prefs.getInt("current_streak", 0)
        _longestStreak.value = prefs.getInt("longest_streak", 0)
        calculateStreak()
    }

    private suspend fun calculateStreak() {
        val entries = dao.getAllEntriesOnce()
        val today = LocalDate.now()
        val dates = entries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }.distinct().toSet()

        var current = 0
        var day = today
        while (dates.contains(day)) {
            current++
            day = day.minusDays(1)
        }
        _currentStreak.value = current

        val sorted = dates.sorted()
        val longest = if (sorted.isEmpty()) 0 else {
            var maxStreak = 1
            var run = 1
            for (i in 1 until sorted.size) {
                if (sorted[i].toEpochDay() == sorted[i - 1].toEpochDay() + 1) {
                    run++
                } else {
                    if (run > maxStreak) maxStreak = run
                    run = 1
                }
            }
            if (run > maxStreak) { maxStreak = run }; maxStreak
        }
        _longestStreak.value = longest

        prefs.edit()
            .putInt("current_streak", _currentStreak.value)
            .putInt("longest_streak", _longestStreak.value)
            .apply()

        buildMonthGrid(dates)
    }

    private fun buildMonthGrid(entryDates: Set<LocalDate>) {
        val today = LocalDate.now()
        val firstDay = today.withDayOfMonth(1)
        val daysInMonth = today.lengthOfMonth()
        val firstDayOfWeek = firstDay.dayOfWeek.value
        val grid = mutableListOf<Pair<Int?, Boolean>>()
        for (i in 1 until firstDayOfWeek) {
            grid.add(null to false)
        }
        for (d in 1..daysInMonth) {
            val date = today.withDayOfMonth(d)
            grid.add(d to entryDates.contains(date))
        }
        while (grid.size < 35) {
            grid.add(null to false)
        }
        _monthGrid.value = grid
    }

    private suspend fun checkMonthlyGrant() {
        val monthKey = "shield_monthly_granted_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))}"
        if (prefs.getBoolean(monthKey, false)) return
        val item = ShieldItem(source = "monthly")
        val list = _inventory.value.toMutableList()
        list.add(item)
        _inventory.value = list
        saveInventory()
        prefs.edit().putBoolean(monthKey, true).apply()
    }

    private suspend fun autoDetectAndProtect() {
        if (!_autoProtectEnabled.value) return
        val today = LocalDate.now()
        val checkKey = "auto_protect_checked_${today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}"
        if (prefs.getBoolean(checkKey, false)) return
        val yesterday = today.minusDays(1)
        val startOfDay = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (dao.getPreviewsByDateRange(startOfDay, endOfDay).isEmpty()) {
            val available = _inventory.value.firstOrNull { !it.isUsed }
            if (available != null) {
                val endOfYesterday = yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
                val updated = available.copy(isUsed = true, usedAt = System.currentTimeMillis(), savedDate = endOfYesterday, triggerType = "auto")
                val list = _inventory.value.toMutableList()
                val idx = list.indexOfFirst { it.id == available.id }
                if (idx >= 0) {
                    list[idx] = updated
                    _inventory.value = list
                    saveInventory()
                }
                val protectedSet = getProtectedDates().toMutableSet()
                protectedSet.add(yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                prefs.edit().putString("protected_dates", gson.toJson(protectedSet)).apply()
            }
        }
        prefs.edit().putBoolean(checkKey, true).apply()
    }

    private fun getProtectedDates(): Set<String> {
        val json = prefs.getString("protected_dates", null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptySet() }
    }

    private fun buildHistory() {
        val used = _inventory.value.filter { it.isUsed }.sortedByDescending { it.usedAt ?: 0L }
        val groups = mutableListOf<ShieldHistoryGroup>()
        var currentMonth = ""
        var currentItems = mutableListOf<ShieldItem>()
        for (item in used) {
            val usedDate = item.usedAt?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: continue
            val month = usedDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            if (month != currentMonth && currentItems.isNotEmpty()) {
                groups.add(ShieldHistoryGroup(currentMonth, currentItems.toList()))
                currentItems = mutableListOf()
            }
            currentMonth = month
            currentItems.add(item)
        }
        if (currentItems.isNotEmpty()) {
            groups.add(ShieldHistoryGroup(currentMonth, currentItems))
        }
        _history.value = groups
    }

    private fun checkAchievements() {
        val usedCount = _inventory.value.count { it.isUsed }
        val totalCount = _inventory.value.size
        val existing = _achievements.value.map { it.id }.toSet()
        val newList = _achievements.value.toMutableList()

        if (usedCount >= 1 && !existing.contains("first_use")) {
            newList.add(ShieldAchievement("first_use", "初次守护", "第一次使用保护罩"))
        }
        if (usedCount >= 3 && !existing.contains("three_uses")) {
            newList.add(ShieldAchievement("three_uses", "守护达人", "累计使用3次保护罩"))
        }
        if (totalCount >= 6 && !existing.contains("six_shields")) {
            newList.add(ShieldAchievement("six_shields", "全副武装", "获得6个保护罩"))
        }
        if (_currentStreak.value >= 30 && !existing.contains("streak_30")) {
            newList.add(ShieldAchievement("streak_30", "自强不息", "连续30天无需使用保护罩"))
        }

        _achievements.value = newList
        saveAchievements()
    }

    fun toggleAutoProtect(enabled: Boolean) {
        _autoProtectEnabled.value = enabled
        prefs.edit().putBoolean("shield_auto_protect", enabled).apply()
    }

    fun toggleNotify(enabled: Boolean) {
        _notifyEnabled.value = enabled
        prefs.edit().putBoolean("shield_notify", enabled).apply()
    }

    fun useShield(itemId: String) {
        val list = _inventory.value.toMutableList()
        val idx = list.indexOfFirst { it.id == itemId }
        if (idx < 0 || list[idx].isUsed) return
        val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        list[idx] = list[idx].copy(isUsed = true, usedAt = System.currentTimeMillis(), savedDate = todayEnd, triggerType = "manual")
        _inventory.value = list
        saveInventory()
        buildHistory()
        checkAchievements()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            loadStreakData()
            buildHistory()
            checkAchievements()
        }
    }
}
