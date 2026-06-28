package com.diary.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 称号系统统一管理器
 * 负责注册所有检查器，触发检查，管理解锁流程
 */
object TitleManager {

    // 所有组合定义
    private val combinations: List<TitleCombination> = listOf(
        TitleCombination(
            id = "knowledge_master",
            name = "知识渊博",
            description = "同时拥有书虫和思考者称号，获得智慧光环",
            requiredTitles = listOf("bookworm", "deep_thinker"),
            effectType = CombinationEffect.WISDOM_AURA
        ),
        TitleCombination(
            id = "emotion_master",
            name = "情绪大师",
            description = "同时拥有乐观者和共情者称号，获得温暖光环",
            requiredTitles = listOf("optimist", "empath"),
            effectType = CombinationEffect.WARM_GLOW
        ),
        TitleCombination(
            id = "adventurer",
            name = "冒险家",
            description = "同时拥有旅行者和探索者称号，获得冒险徽章",
            requiredTitles = listOf("traveler", "explorer"),
            effectType = CombinationEffect.ADVENTURE_BADGE
        ),
        TitleCombination(
            id = "persistence",
            name = "坚持不懈",
            description = "同时拥有连续记录7天和连续记录30天称号，获得坚持光环",
            requiredTitles = listOf("daily_writer", "hundred_days"),
            effectType = CombinationEffect.PERSISTENCE_AURA
        )
    )

    // 所有检查器注册
    private val checkers: Map<String, TitleChecker> = mapOf(
        // 时间旅人
        "night_poet" to NightPoetChecker(),
        "dawn_recorder" to DawnRecorderChecker(),
        "morning_writer" to MorningWriterChecker(),
        "afternoon_dreamer" to AfternoonDreamerChecker(),
        "night_owl" to NightOwlChecker(),
        "weekday_killer" to WeekdayKillerChecker(),
        "time_capsule_master" to TimeCapsuleMasterChecker(),
        // 情绪画师
        "optimist" to OptimistChecker(),
        "deep_thinker" to DeepThinkerChecker(),
        "mood_palette" to MoodPaletteChecker(),
        // 风雨行者
        "rain_collector" to RainCollectorChecker(),
        "all_weather" to AllWeatherChecker(),
        // 文字匠人
        "thousand_words" to ThousandWordsChecker(),
        "brief_master" to BriefMasterChecker(),
        "collector" to CollectorChecker(),
        "fifty_thousand_words" to FiftyThousandWordsChecker(),
        // 习惯先锋
        "daily_writer" to DailyWriterChecker(),
        "hundred_days" to HundredDaysChecker(),
        "returnee" to ReturneeChecker(),
        // 隐藏彩蛋
        "time_traveler" to TimeTravelerChecker(),
        "new_year_eve" to NewYearEveChecker(),
        "midnight_bell" to MidnightBellChecker(),
        "first_echo" to FirstEchoChecker()
    )

    /**
     * 保存日记后实时检查
     * 只检查与本篇日记直接相关的称号，不做全量扫描
     * 返回新解锁的称号列表
     */
    suspend fun checkOnEntrySaved(
        entry: DiaryEntry,
        diaryDao: DiaryDao,
        titleDao: TitleDao
    ): List<TitleDefinition> = withContext(Dispatchers.IO) {
        val newlyUnlocked = mutableListOf<TitleDefinition>()

        // 检查与时间相关的称号
        val timeCheckers = listOf(
            "night_poet", "dawn_recorder", "morning_writer",
            "afternoon_dreamer", "night_owl", "weekday_killer",
            "time_capsule_master", "time_traveler", "new_year_eve", "midnight_bell"
        )

        for (key in timeCheckers) {
            val existing = titleDao.getUserTitle(key)
            if (existing == null) {
                val checker = checkers[key] ?: continue
                if (checker.check(diaryDao, titleDao)) {
                    titleDao.insertUserTitle(
                        UserTitle(
                            titleKey = key,
                            unlockedAt = System.currentTimeMillis(),
                            relatedEntryId = entry.id
                        )
                    )
                    val definition = titleDao.getDefinition(key)
                    if (definition != null) newlyUnlocked.add(definition)
                }
            }
        }

        // 检查与内容/习惯相关的称号
        val contentCheckers = listOf(
            "thousand_words", "brief_master", "mood_palette",
            "rain_collector", "all_weather", "optimist", "deep_thinker",
            "collector", "fifty_thousand_words", "daily_writer",
            "hundred_days", "returnee", "first_echo"
        )

        for (key in contentCheckers) {
            val existing = titleDao.getUserTitle(key)
            if (existing == null) {
                val checker = checkers[key] ?: continue
                if (checker.check(diaryDao, titleDao)) {
                    titleDao.insertUserTitle(
                        UserTitle(
                            titleKey = key,
                            unlockedAt = System.currentTimeMillis(),
                            relatedEntryId = entry.id
                        )
                    )
                    val definition = titleDao.getDefinition(key)
                    if (definition != null) newlyUnlocked.add(definition)
                }
            }
        }

        newlyUnlocked
    }

    /**
     * 全量扫描所有称号
     * 在打开称号页面或应用启动时调用
     */
    suspend fun checkAll(
        diaryDao: DiaryDao,
        titleDao: TitleDao
    ): List<TitleDefinition> = withContext(Dispatchers.IO) {
        val newlyUnlocked = mutableListOf<TitleDefinition>()

        for ((key, checker) in checkers) {
            val existing = titleDao.getUserTitle(key)
            if (existing == null) {
                if (checker.check(diaryDao, titleDao)) {
                    titleDao.insertUserTitle(
                        UserTitle(
                            titleKey = key,
                            unlockedAt = System.currentTimeMillis()
                        )
                    )
                    val definition = titleDao.getDefinition(key)
                    if (definition != null) newlyUnlocked.add(definition)
                }
            }
        }

        newlyUnlocked
    }

    /**
     * 检测当前激活的称号组合
     * 返回所有满足条件的组合列表
     */
    suspend fun detectActiveCombinations(
        titleDao: TitleDao
    ): List<ActiveCombination> = withContext(Dispatchers.IO) {
        val activeCombinations = mutableListOf<ActiveCombination>()
        val userTitles = titleDao.getAllUserTitlesOnce().map { it.titleKey }.toSet()

        for (combination in combinations) {
            val hasAllTitles = combination.requiredTitles.all { it in userTitles }
            if (hasAllTitles) {
                activeCombinations.add(
                    ActiveCombination(
                        combination = combination,
                        activatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        activeCombinations
    }

    /**
     * 获取所有组合定义
     */
    fun getAllCombinations(): List<TitleCombination> = combinations

    /**
     * 检查指定组合是否激活
     */
    suspend fun isCombinationActive(
        combinationId: String,
        titleDao: TitleDao
    ): Boolean = withContext(Dispatchers.IO) {
        val combination = combinations.find { it.id == combinationId } ?: return@withContext false
        val userTitles = titleDao.getAllUserTitlesOnce().map { it.titleKey }.toSet()
        combination.requiredTitles.all { it in userTitles }
    }

    /**
     * 获取称号的解锁进度（用于UI展示）
     * 返回当前值和目标值
     */
    suspend fun getProgress(
        key: String,
        diaryDao: DiaryDao
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        when (key) {
            "night_poet" -> {
                val entries = diaryDao.getPreviewsByDateRange(
                    System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                    System.currentTimeMillis()
                )
                val count = entries.count { entry ->
                    val hour = java.time.Instant.ofEpochMilli(entry.createdAt)
                        .atZone(java.time.ZoneId.systemDefault()).hour
                    hour in 0..2
                }
                count to 3
            }
            "rain_collector" -> {
                val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
                val count = entries.count { it.weather == "雨天" }
                count to 20
            }
            "collector" -> {
                val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
                val count = entries.count { it.isFavorite }
                count to 20
            }
            "fifty_thousand_words" -> {
                val entries = diaryDao.getPreviewsByDateRange(0, System.currentTimeMillis())
                val total = entries.sumOf { it.plainText.length }
                total to 50000
            }
            "daily_writer" -> {
                val now = java.time.LocalDate.now()
                var streak = 0
                for (i in 0 until 365) {
                    val date = now.minusDays(i.toLong())
                    val start = date.atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    val end = date.atTime(23, 59, 59)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    val entries = diaryDao.getPreviewsByDateRange(start, end)
                    if (entries.isEmpty()) break
                    streak++
                }
                streak to 30
            }
            "hundred_days" -> {
                val now = java.time.LocalDate.now()
                var streak = 0
                for (i in 0 until 365) {
                    val date = now.minusDays(i.toLong())
                    val start = date.atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    val end = date.atTime(23, 59, 59)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    val entries = diaryDao.getPreviewsByDateRange(start, end)
                    if (entries.isEmpty()) break
                    streak++
                }
                streak to 100
            }
            else -> 0 to 1
        }
    }
}
