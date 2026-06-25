package com.diary.app.data

import com.diary.app.util.computeStreak
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val diaryDao: DiaryDao,
    private val tagDao: TagDao,
    private val mediaDao: MediaDao
) {
    fun getAllItems(): Flow<List<AchievementItem>> {
        return achievementDao.getAllUnified().map { achievements ->
            achievements.map { ach ->
                val def = UnifiedAchievementSeedData.byKey[ach.key] ?: AchievementDef(
                    key = ach.key, name = ach.name, description = ach.description,
                    category = AchievementCategory.entries.find { it.name == ach.category } ?: AchievementCategory.WRITING,
                    tier = AchievementTier.fromInt(ach.tier),
                    iconEmoji = ach.iconEmoji.ifEmpty { "\u2B50" },
                    flavorText = ach.flavorText, target = ach.target
                )
                AchievementItem(def = def, state = AchievementState(
                    key = ach.key, progress = ach.progress,
                    unlocked = ach.unlockedAt != null, unlockedAt = ach.unlockedAt, relatedEntryId = null
                ))
            }
        }
    }

    fun getStats(): Flow<AchievementStats> {
        return achievementDao.getAllUnified().map { achievements ->
            val total = achievements.size
            val unlocked = achievements.count { it.unlockedAt != null }
            val categoryCounts = AchievementCategory.entries.associateWith { cat ->
                val catAch = achievements.filter { it.category == cat.name }
                catAch.count { it.unlockedAt != null } to catAch.size
            }
            AchievementStats(unlockedCount = unlocked, totalCount = total, categoryCounts = categoryCounts)
        }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val existing = achievementDao.getAllUnified().first()
        val existingKeys = existing.map { it.key }.toSet()
        val newAchievements = UnifiedAchievementSeedData.allAchievements
            .filter { it.key !in existingKeys }
            .map { def ->
                Achievement(key = def.key, name = def.name, description = def.description,
                    iconName = "Star", category = def.category.name, tier = def.tier.tierInt,
                    iconEmoji = def.iconEmoji, flavorText = def.flavorText,
                    isHidden = def.isHidden, target = def.target, progress = 0)
            }
        if (newAchievements.isNotEmpty()) { achievementDao.insertAll(newAchievements) }
        for (def in UnifiedAchievementSeedData.allAchievements) {
            if (def.key in existingKeys) {
                achievementDao.updateMetadata(def.key, def.category.name, def.tier.tierInt,
                    def.iconEmoji, def.flavorText, def.isHidden, def.target)
            }
        }
    }

    suspend fun checkAndUnlock() = withContext(Dispatchers.IO) {
        val allEntries = diaryDao.getAllPreviews().first()
        val totalEntries = allEntries.size
        val totalWords = allEntries.sumOf { it.plainText.length }
        val dates = allEntries.map { Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
        val streak = computeStreak(dates)
        val uniqueMoods = allEntries.mapNotNull { it.moodLevel }.distinct().size
        val uniqueWeathers = allEntries.mapNotNull { it.weather }.distinct().size
        val favoriteCount = allEntries.count { it.isFavorite }
        val imageCount = mediaDao.getAllImages().size
        val tagCount = tagDao.getAllTagsOnce().size
        val zone = ZoneId.systemDefault()
        val nightEntries = allEntries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).hour in 0..4 }
        val earlyEntries = allEntries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).hour in 5..6 }
        val dawnEntries = allEntries.count { Instant.ofEpochMilli(it.createdAt).atZone(zone).hour in 3..4 }
        val weekdays = allEntries.map { Instant.ofEpochMilli(it.createdAt).atZone(zone).dayOfWeek.value }.toSet().size
        val recentMoods = allEntries.take(20).mapNotNull { it.moodLevel }
        val highMoodStreak = countConsecutiveFromEnd(recentMoods) { it >= 5 }
        val lowMoodStreak = countConsecutiveFromEnd(recentMoods) { it <= 2 }
        val calmMoodStreak = countConsecutiveFromEnd(recentMoods) { it in 3..4 }
        val maxWordsInEntry = allEntries.maxOfOrNull { it.plainText.length } ?: 0
        val hasShortFavorite = allEntries.any { it.isFavorite && it.plainText.length < 50 }
        val rainCount = allEntries.count { it.weather == "\u96E8\u5929" }
        val snowCount = allEntries.count { it.weather == "\u96EA\u5929" }
        val stormCount = allEntries.count { it.weather == "\u5927\u98CE" || it.weather == "\u98CE\u66B4" }
        val sunnyCount = allEntries.count { it.weather == "\u6674\u5929" }
        val updates = mapOf(
            "first_entry" to totalEntries, "entries_10" to totalEntries, "entries_50" to totalEntries, "entries_100" to totalEntries,
            "words_10000" to totalWords, "words_100000" to totalWords, "tags_5" to tagCount, "images_10" to imageCount,
            "streak_7" to streak, "streak_30" to streak, "daily_writer" to streak, "hundred_days" to streak,
            "night_writer" to nightEntries, "early_bird" to earlyEntries, "night_poet" to nightEntries,
            "dawn_recorder" to dawnEntries, "morning_writer" to earlyEntries, "weekday_killer" to weekdays,
            "moods_5" to uniqueMoods, "mood_palette" to uniqueMoods, "optimist" to highMoodStreak,
            "deep_thinker" to lowMoodStreak, "calm_sea" to calmMoodStreak, "all_weather" to uniqueWeathers,
            "rain_collector" to rainCount, "snow_writer" to snowCount, "storm_writer" to stormCount, "sunny_recorder" to sunnyCount,
            "favorite_1" to favoriteCount, "favorites_10" to favoriteCount, "collector" to favoriteCount,
            "thousand_words" to if (maxWordsInEntry >= 1000) 1 else 0, "brief_master" to if (hasShortFavorite) 1 else 0,
            "photo_diary" to if (imageCount >= 3) 1 else 0, "fifty_thousand_words" to totalWords,
            "returnee" to if (streak > 0 && totalEntries > 1) 1 else 0,
            "flash_writer" to 0, "deep_writer" to 0, "twin_stars" to 0,
            "legendary_entries_500" to totalEntries, "legendary_streak_365" to streak, "legendary_words_million" to totalWords
        )

        // Get or create milestone tag
        val milestoneTag = tagDao.getTagByName("\u91CC\u7A0B\u7891")
            ?: run {
                val tagId = tagDao.insertTag(Tag(name = "\u91CC\u7A0B\u7891", color = 0xFF4CAF50))
                Tag(id = tagId, name = "\u91CC\u7A0B\u7891", color = 0xFF4CAF50)
            }

        val today = java.time.LocalDate.now()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        for ((key, value) in updates) {
            val achievement = achievementDao.getByKey(key) ?: continue
            if (achievement.unlockedAt != null) continue
            val def = UnifiedAchievementSeedData.byKey[key]
            val target = def?.target ?: achievement.target
            if (value >= target) {
                achievementDao.unlock(key, System.currentTimeMillis(), value)
                // Create milestone diary entry
                if (def != null) {
                    createMilestoneDiary(def, milestoneTag, todayStart, todayEnd)
                }
            }
            else if (value > achievement.progress) { achievementDao.setProgress(key, value) }
        }
    }

    private suspend fun createMilestoneDiary(
        def: AchievementDef,
        tag: com.diary.app.data.Tag,
        todayStart: Long,
        todayEnd: Long
    ) {
        // Check if milestone diary already exists today
        val existingEntries = diaryDao.getPreviewsByDateRange(todayStart, todayEnd)
        val alreadyExists = existingEntries.any { it.title?.startsWith("\u91CC\u7A0B\u7891: ${def.name}") == true }
        if (alreadyExists) return

        val now = System.currentTimeMillis()
        val entry = DiaryEntry(
            title = "\u91CC\u7A0B\u7891: ${def.name}",
            plainText = "\u6210\u5C31\u89E3\u9501: ${def.name}\n${def.description}\n\n${def.flavorText}",
            content = "<p>\u6210\u5C31\u89E3\u9501: ${def.name}</p><p>${def.description}</p><p>${def.flavorText}</p>",
            moodLevel = 5,
            weather = null,
            location = null,
            latitude = null,
            longitude = null,
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            writingDurationSeconds = 0
        )
        val entryId = diaryDao.insertEntry(entry)
        tagDao.insertDiaryTag(DiaryTag(diaryId = entryId, tagId = tag.id))
    }

    private fun <T> countConsecutiveFromEnd(list: List<T>, predicate: (T) -> Boolean): Int {
        var count = 0
        for (item in list.reversed()) { if (predicate(item)) count++ else break }
        return count
    }
}
