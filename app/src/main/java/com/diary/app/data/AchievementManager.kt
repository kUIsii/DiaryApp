package com.diary.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AchievementManager {

    data class AchievementDef(
        val key: String,
        val name: String,
        val description: String,
        val iconName: String,
        val target: Int
    )

    val ACHIEVEMENTS = listOf(
        AchievementDef("first_entry", "初出茅庐", "写下第一篇日记", "Edit", 1),
        AchievementDef("entries_10", "笔耕不辍", "累计写下 10 篇日记", "MenuBook", 10),
        AchievementDef("entries_50", "日记达人", "累计写下 50 篇日记", "EmojiEvents", 50),
        AchievementDef("entries_100", "百篇里程碑", "累计写下 100 篇日记", "Diamond", 100),
        AchievementDef("streak_7", "一周坚持", "连续 7 天写日记", "LocalFireDepartment", 7),
        AchievementDef("streak_30", "月度坚持", "连续 30 天写日记", "Star", 30),
        AchievementDef("words_10000", "万字作者", "累计写作 10,000 字", "EditNote", 10000),
        AchievementDef("words_100000", "十万字巨匠", "累计写作 100,000 字", "LibraryBooks", 100000),
        AchievementDef("moods_5", "情绪丰富", "使用 5 种不同心情", "Palette", 5),
        AchievementDef("all_weather", "风雨无阻", "在所有天气类型下写过日记", "Cloud", 6),
        AchievementDef("night_writer", "夜猫子", "在凌晨 0-5 点写日记", "DarkMode", 1),
        AchievementDef("early_bird", "早起鸟", "在早上 5-7 点写日记", "WbSunny", 1),
        AchievementDef("favorite_1", "收藏家", "收藏第一篇日记", "Favorite", 1),
        AchievementDef("favorites_10", "珍藏满满", "收藏 10 篇日记", "AutoAwesome", 10),
        AchievementDef("tags_5", "标签达人", "创建 5 个标签", "Label", 5),
        AchievementDef("images_10", "图文并茂", "在日记中添加 10 张图片", "Image", 10),
    )

    fun initializeAchievements(scope: CoroutineScope, dao: AchievementDao) {
        scope.launch(Dispatchers.IO) {
            try {
                val existing = dao.getAllAchievements().first()
                val existingKeys = existing.map { it.key }.toSet()
                val newAchievements = ACHIEVEMENTS
                    .filter { it.key !in existingKeys }
                    .map { def ->
                        Achievement(
                            key = def.key,
                            name = def.name,
                            description = def.description,
                            iconName = def.iconName,
                            target = def.target
                        )
                    }
                if (newAchievements.isNotEmpty()) {
                    dao.insertAll(newAchievements)
                }
            } catch (e: Exception) {
                android.util.Log.w("AchievementManager", "Failed to initialize achievements", e)
            }
        }
    }

    suspend fun checkAndUnlock(
        dao: AchievementDao,
        diaryDao: DiaryDao,
        context: Context
    ) {
        val allEntries = diaryDao.getAllPreviews().first()
        val totalEntries = allEntries.size
        val totalWords = allEntries.sumOf { it.plainText.length }
        val streak = computeStreak(allEntries)
        val uniqueMoods = allEntries.mapNotNull { it.moodLevel }.distinct().size
        val uniqueWeathers = allEntries.mapNotNull { it.weather }.distinct().size
        val favoriteCount = allEntries.count { it.isFavorite }
        val imageCount = diaryDao.getAllImages().size
        val tagCount = diaryDao.getAllTagsOnce().size

        // Check each achievement
        checkUnlock(dao, "first_entry", totalEntries)
        checkUnlock(dao, "entries_10", totalEntries)
        checkUnlock(dao, "entries_50", totalEntries)
        checkUnlock(dao, "entries_100", totalEntries)
        checkUnlock(dao, "streak_7", streak)
        checkUnlock(dao, "streak_30", streak)
        checkUnlock(dao, "words_10000", totalWords)
        checkUnlock(dao, "words_100000", totalWords)
        checkUnlock(dao, "moods_5", uniqueMoods)
        checkUnlock(dao, "all_weather", uniqueWeathers)
        checkUnlock(dao, "favorite_1", favoriteCount)
        checkUnlock(dao, "favorites_10", favoriteCount)
        checkUnlock(dao, "tags_5", tagCount)
        checkUnlock(dao, "images_10", imageCount)

        // Time-based achievements
        val now = java.time.LocalTime.now()
        checkUnlock(dao, "night_writer", if (now.hour in 0..5) 1 else 0)
        checkUnlock(dao, "early_bird", if (now.hour in 5..7) 1 else 0)
    }

    private suspend fun checkUnlock(dao: AchievementDao, key: String, currentValue: Int) {
        val achievement = dao.getByKey(key) ?: return
        if (achievement.unlockedAt != null) return // already unlocked

        if (currentValue >= achievement.target) {
            dao.unlock(key, System.currentTimeMillis(), currentValue)
        } else if (currentValue > achievement.progress) {
            dao.updateProgress(key, currentValue)
        }
    }

    private fun computeStreak(entries: List<DiaryPreview>): Int {
        if (entries.isEmpty()) return 0
        val dates = entries
            .map {
                java.time.Instant.ofEpochMilli(it.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            }
            .distinct()
            .sortedDescending()

        var streak = 1
        for (i in 0 until dates.size - 1) {
            if (dates[i].minusDays(1) == dates[i + 1]) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}
