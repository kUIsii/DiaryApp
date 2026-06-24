package com.diary.app.data

import java.time.Instant
import java.time.ZoneId

/**
 * 宠物状态机
 * 根据时间、内容情绪、互动频率决定宠物状态
 */
object PetStateMachine {

    // 成长阶段进化条件
    private const val JUVENILE_MIN_DAYS = 31
    private const val JUVENILE_MIN_INTERACTIONS = 20
    private const val JUVENILE_MIN_STATES = 3

    private const val GROWING_MIN_DAYS = 91
    private const val GROWING_MIN_INTERACTIONS = 60
    private const val GROWING_MIN_AFFECTION = 100

    private val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * 根据日记内容和时间决定宠物状态
     */
    fun determineState(
        content: String,
        plainText: String,
        moodLevel: Int?,
        createdAt: Long = System.currentTimeMillis(),
        streakDays: Int = 0,
        lastEntryTime: Long = 0
    ): PetState {
        val hour = Instant.ofEpochMilli(createdAt)
            .atZone(ZoneId.systemDefault()).hour

        // 1. 检查长期未记录
        val daysSinceLastEntry = if (lastEntryTime > 0) {
            (createdAt - lastEntryTime) / DAY_MS
        } else {
            Long.MAX_VALUE
        }

        if (daysSinceLastEntry > 7) {
            return PetState.WORRIED
        }

        // 2. 检查时间维度
        if (hour == 23 || hour in 0..2) {
            return PetState.SLEEPY
        }

        // 3. 检查心情维度
        when (moodLevel) {
            1, 2 -> {
                // 沮丧/低落
                val stressWords = listOf("压力", "焦虑", "紧张", "担心", "害怕", "烦躁")
                val hasStress = stressWords.any { plainText.contains(it) }
                return if (hasStress) PetState.WORRIED else PetState.SAD
            }
            3 -> {
                // 平静
                return PetState.CALM
            }
            4 -> {
                // 开心
                return PetState.HAPPY
            }
            5, 6 -> {
                // 愉快/兴奋
                return PetState.EXCITED
            }
        }

        // 4. 检查内容情绪
        val positiveWords = listOf("开心", "快乐", "高兴", "喜欢", "爱", "感谢", "美好", "幸福")
        val negativeWords = listOf("难过", "伤心", "失望", "无聊", "累", "烦", "糟糕")
        val curiousWords = listOf("发现", "学习", "尝试", "新", "有趣", "好奇", "探索")

        val positiveCount = positiveWords.count { plainText.contains(it) }
        val negativeCount = negativeWords.count { plainText.contains(it) }
        val curiousCount = curiousWords.count { plainText.contains(it) }

        if (positiveCount > negativeCount && positiveCount > 0) {
            return PetState.HAPPY
        }
        if (negativeCount > positiveCount && negativeCount > 0) {
            return PetState.SAD
        }
        if (curiousCount > 0) {
            return PetState.CURIOUS
        }

        // 5. 连续记录奖励
        if (streakDays >= 7) {
            return PetState.HAPPY
        }

        // 6. 默认状态
        return PetState.CALM
    }

    /**
     * 检查并更新成长阶段
     * @return 如果发生了进化，返回新的阶段；否则返回当前阶段
     */
    suspend fun checkGrowthStage(
        profile: PetProfile,
        petDao: PetDao
    ): PetGrowthStage {
        val currentStage = PetGrowthStage.fromName(profile.growthStage)
        val now = System.currentTimeMillis()

        // 计算宠物年龄（天数）- 基于最后互动时间的近似
        // 如果没有进化时间，使用当前时间作为近似创建时间
        val createdAtApprox = profile.evolvedAt ?: now
        val daysAlive = ((now - createdAtApprox) / DAY_MS).toInt()

        // 获取互动数据
        val totalInteractions = petDao.getStateCount()
        val distinctStates = petDao.getDistinctStateCount()
        val discoveredHiddenCount = profile.discoveredHiddenStates.let { json ->
            json.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .count { it.isNotEmpty() }
        }

        return when (currentStage) {
            PetGrowthStage.JUVENILE -> {
                // 幼年期 -> 成长期: 至少31天 + 20次互动 + 3种状态
                if (daysAlive >= JUVENILE_MIN_DAYS &&
                    totalInteractions >= JUVENILE_MIN_INTERACTIONS &&
                    distinctStates >= JUVENILE_MIN_STATES
                ) {
                    petDao.updateGrowthStage(PetGrowthStage.GROWING.name, now)
                    PetGrowthStage.GROWING
                } else {
                    PetGrowthStage.JUVENILE
                }
            }
            PetGrowthStage.GROWING -> {
                // 成长期 -> 成熟期: 至少91天 + 60次互动 + 1个隐藏状态 + 好感度>=100
                if (daysAlive >= GROWING_MIN_DAYS &&
                    totalInteractions >= GROWING_MIN_INTERACTIONS &&
                    discoveredHiddenCount >= 1 &&
                    profile.affection >= GROWING_MIN_AFFECTION
                ) {
                    petDao.updateGrowthStage(PetGrowthStage.MATURE.name, now)
                    PetGrowthStage.MATURE
                } else {
                    PetGrowthStage.GROWING
                }
            }
            PetGrowthStage.MATURE -> PetGrowthStage.MATURE
        }
    }

    /**
     * 获取成长阶段中文标签
     */
    fun getGrowthStageLabel(stage: PetGrowthStage): String {
        return stage.displayName
    }

    /**
     * 获取当前阶段已持续天数
     */
    fun getDaysInStage(evolvedAt: Long?, currentStage: PetGrowthStage): Int {
        if (currentStage == PetGrowthStage.JUVENILE && evolvedAt == null) return 0
        val stageStartTime = evolvedAt ?: return 0
        val now = System.currentTimeMillis()
        return ((now - stageStartTime) / DAY_MS).toInt()
    }

    /**
     * 获取当前阶段所需总天数（用于进度显示）
     */
    fun getRequiredDaysForStage(stage: PetGrowthStage): Int {
        return when (stage) {
            PetGrowthStage.JUVENILE -> JUVENILE_MIN_DAYS
            PetGrowthStage.GROWING -> GROWING_MIN_DAYS
            PetGrowthStage.MATURE -> Int.MAX_VALUE
        }
    }

    /**
     * 获取进化提示文案
     */
    suspend fun getEvolutionHint(
        profile: PetProfile,
        petDao: PetDao
    ): String? {
        val currentStage = PetGrowthStage.fromName(profile.growthStage)
        val now = System.currentTimeMillis()
        val createdAtApprox = profile.evolvedAt ?: now
        val daysAlive = ((now - createdAtApprox) / DAY_MS).toInt()
        val totalInteractions = petDao.getStateCount()
        val distinctStates = petDao.getDistinctStateCount()
        val discoveredHiddenCount = profile.discoveredHiddenStates.let { json ->
            json.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .count { it.isNotEmpty() }
        }

        return when (currentStage) {
            PetGrowthStage.JUVENILE -> {
                val missing = mutableListOf<String>()
                if (daysAlive < JUVENILE_MIN_DAYS) missing.add("再记录${JUVENILE_MIN_DAYS - daysAlive}天")
                if (totalInteractions < JUVENILE_MIN_INTERACTIONS) missing.add("再互动${JUVENILE_MIN_INTERACTIONS - totalInteractions}次")
                if (distinctStates < JUVENILE_MIN_STATES) missing.add("体验${JUVENILE_MIN_STATES - distinctStates}种新状态")
                if (missing.isEmpty()) "即将进化为成长期!"
                else "进化条件: ${missing.joinToString(", ")}"
            }
            PetGrowthStage.GROWING -> {
                val missing = mutableListOf<String>()
                if (daysAlive < GROWING_MIN_DAYS) missing.add("再记录${GROWING_MIN_DAYS - daysAlive}天")
                if (totalInteractions < GROWING_MIN_INTERACTIONS) missing.add("再互动${GROWING_MIN_INTERACTIONS - totalInteractions}次")
                if (discoveredHiddenCount < 1) missing.add("发现1个隐藏状态")
                if (profile.affection < GROWING_MIN_AFFECTION) missing.add("好感度还需${GROWING_MIN_AFFECTION - profile.affection}")
                if (missing.isEmpty()) "即将进化为成熟期!"
                else "进化条件: ${missing.joinToString(", ")}"
            }
            PetGrowthStage.MATURE -> null
        }
    }

    /**
     * 获取状态对应的动画关键词
     */
    fun getStateAnimation(state: PetState): String {
        return when (state) {
            PetState.CALM -> "idle"
            PetState.HAPPY -> "happy_bounce"
            PetState.SLEEPY -> "yawn"
            PetState.WORRIED -> "worried_shake"
            PetState.SAD -> "sad_down"
            PetState.EXCITED -> "excited_jump"
            PetState.CURIOUS -> "curious_tilt"
            PetState.TIRED -> "tired_lay"
        }
    }

    /**
     * 获取状态对应的颜色
     */
    fun getStateColor(state: PetState): Long {
        return when (state) {
            PetState.CALM -> 0xFF4FC3F7      // 浅蓝
            PetState.HAPPY -> 0xFFFFD54F     // 金黄
            PetState.SLEEPY -> 0xFFCE93D8    // 淡紫
            PetState.WORRIED -> 0xFFFFAB91   // 橙红
            PetState.SAD -> 0xFF90A4AE       // 灰蓝
            PetState.EXCITED -> 0xFFFF8A65   // 亮橙
            PetState.CURIOUS -> 0xFF81C784   // 浅绿
            PetState.TIRED -> 0xFFBDBDBD     // 灰色
        }
    }

    /**
     * 隐藏状态检测结果
     */
    data class HiddenStateCheckResult(
        val triggeredStates: List<PetHiddenStateType>,
        val newlyDiscovered: List<PetHiddenStateType>
    )

    /**
     * 检测隐藏状态触发
     * @return 触发的隐藏状态列表和新发现的隐藏状态列表
     */
    suspend fun checkHiddenState(
        plainText: String,
        moodLevel: Int?,
        createdAt: Long,
        entryId: Long,
        petDao: PetDao,
        diaryDao: DiaryDao
    ): HiddenStateCheckResult {
        val triggeredStates = mutableListOf<PetHiddenStateType>()
        val newlyDiscovered = mutableListOf<PetHiddenStateType>()

        // 获取已发现的隐藏状态
        val profile = petDao.getPetProfileOnce() ?: return HiddenStateCheckResult(emptyList(), emptyList())
        val discoveredJson = profile.discoveredHiddenStates
        val discoveredSet = parseDiscoveredHiddenStates(discoveredJson)

        // 1. 检测夜猫子: 连续3天在23:00-02:00写日记
        if (checkNightOwl(createdAt, diaryDao)) {
            triggeredStates.add(PetHiddenStateType.NIGHT_OWL)
            if (!discoveredSet.contains(PetHiddenStateType.NIGHT_OWL.name)) {
                newlyDiscovered.add(PetHiddenStateType.NIGHT_OWL)
            }
        }

        // 2. 检测宝藏猎人: 单篇日记包含3个以上之前没用过的关键词
        if (checkTreasureHunter(plainText, entryId, diaryDao)) {
            triggeredStates.add(PetHiddenStateType.TREASURE_HUNTER)
            if (!discoveredSet.contains(PetHiddenStateType.TREASURE_HUNTER.name)) {
                newlyDiscovered.add(PetHiddenStateType.TREASURE_HUNTER)
            }
        }

        // 3. 检测暖心守护者: 用户连续3天情绪值 > 0.5
        if (checkWarmGuardian(createdAt, petDao)) {
            triggeredStates.add(PetHiddenStateType.WARM_GUARDIAN)
            if (!discoveredSet.contains(PetHiddenStateType.WARM_GUARDIAN.name)) {
                newlyDiscovered.add(PetHiddenStateType.WARM_GUARDIAN)
            }
        }

        // 4. 检测深海潜水员: 单篇日记超过800字
        if (checkDeepDiver(plainText)) {
            triggeredStates.add(PetHiddenStateType.DEEP_DIVER)
            if (!discoveredSet.contains(PetHiddenStateType.DEEP_DIVER.name)) {
                newlyDiscovered.add(PetHiddenStateType.DEEP_DIVER)
            }
        }

        // 5. 检测时间旅人: 同一天写了2篇以上日记，时间跨度 > 4小时
        if (checkTimeTraveler(createdAt, petDao)) {
            triggeredStates.add(PetHiddenStateType.TIME_TRAVELER)
            if (!discoveredSet.contains(PetHiddenStateType.TIME_TRAVELER.name)) {
                newlyDiscovered.add(PetHiddenStateType.TIME_TRAVELER)
            }
        }

        return HiddenStateCheckResult(triggeredStates, newlyDiscovered)
    }

    /**
     * 解析已发现的隐藏状态JSON
     */
    private fun parseDiscoveredHiddenStates(json: String): Set<String> {
        return try {
            json.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * 检测夜猫子: 连续3天在23:00-02:00写日记
     */
    private suspend fun checkNightOwl(createdAt: Long, diaryDao: DiaryDao): Boolean {
        val hour = Instant.ofEpochMilli(createdAt)
            .atZone(ZoneId.systemDefault()).hour

        // 当前必须在23:00-02:00
        if (hour != 23 && hour !in 0..2) return false

        // 检查最近3天是否都在这个时间段写日记
        val dayMs = 24L * 60 * 60 * 1000
        val calendar = java.util.Calendar.getInstance()

        for (daysAgo in 0..2) {
            calendar.timeInMillis = createdAt
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            val dayStart = calendar.apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + dayMs

            val entries = diaryDao.getEntriesByDateRange(dayStart, dayEnd)
            if (entries.isEmpty()) return false

            // 检查这一天是否有在23:00-02:00写的日记
            val hasNightEntry = entries.any { entry ->
                val entryHour = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault()).hour
                entryHour == 23 || entryHour in 0..2
            }
            if (!hasNightEntry) return false
        }

        return true
    }

    /**
     * 检测宝藏猎人: 单篇日记包含3个以上之前没用过的关键词
     */
    private suspend fun checkTreasureHunter(plainText: String, currentEntryId: Long, diaryDao: DiaryDao): Boolean {
        // 定义关键词类别
        val keywordCategories = mapOf(
            "食物" to listOf("吃", "美食", "餐厅", "做饭", "烹饪", "烘焙", "零食", "甜点"),
            "运动" to listOf("跑步", "健身", "游泳", "瑜伽", "骑行", "爬山", "散步"),
            "工作" to listOf("加班", "项目", "会议", "报告", "会议", "deadline", "工作"),
            "学习" to listOf("学习", "考试", "看书", "阅读", "课程", "培训", "知识"),
            "社交" to listOf("朋友", "聚会", "聊天", "见面", "约会", "社交"),
            "旅行" to listOf("旅行", "景点", "飞机", "酒店", "旅游", "出发"),
            "创作" to listOf("画画", "写作", "音乐", "摄影", "手工", "创作"),
            "自然" to listOf("公园", "花园", "植物", "动物", "天气", "风景")
        )

        // 获取当前日记中的关键词
        val currentKeywords = mutableSetOf<String>()
        for ((category, words) in keywordCategories) {
            for (word in words) {
                if (plainText.contains(word)) {
                    currentKeywords.add(category)
                }
            }
        }

        if (currentKeywords.size < 3) return false

        // 获取之前所有日记的关键词（排除当前这篇）
        val allEntries = diaryDao.getAllEntriesOnce().filter { it.id != currentEntryId }
        val usedKeywords = mutableSetOf<String>()

        for (entry in allEntries) {
            for ((category, words) in keywordCategories) {
                for (word in words) {
                    if (entry.plainText?.contains(word) == true) {
                        usedKeywords.add(category)
                    }
                }
            }
        }

        // 检查是否有3个以上新关键词
        val newKeywords = currentKeywords.filter { !usedKeywords.contains(it) }
        return newKeywords.size >= 3
    }

    /**
     * 检测暖心守护者: 用户连续3天情绪值 > 0.5
     */
    private suspend fun checkWarmGuardian(createdAt: Long, petDao: PetDao): Boolean {
        val dayMs = 24L * 60 * 60 * 1000
        val calendar = java.util.Calendar.getInstance()

        for (daysAgo in 0..2) {
            calendar.timeInMillis = createdAt
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            val dayStart = calendar.apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + dayMs

            // 获取这一天的状态记录
            val records = petDao.getStateRecordsSince(dayStart).filter {
                it.createdAt < dayEnd
            }

            if (records.isEmpty()) return false

            // 检查这一天的情绪是否 > 0.5（映射为状态）
            val hasPositiveMood = records.any { record ->
                val state = try {
                    PetState.valueOf(record.state)
                } catch (e: Exception) {
                    PetState.CALM
                }
                // HAPPY, EXCITED, CURIOUS 视为积极情绪
                state == PetState.HAPPY || state == PetState.EXCITED || state == PetState.CURIOUS
            }

            if (!hasPositiveMood) return false
        }

        return true
    }

    /**
     * 检测深海潜水员: 单篇日记超过800字
     */
    private fun checkDeepDiver(plainText: String): Boolean {
        return plainText.length > 800
    }

    /**
     * 检测时间旅人: 同一天写了2篇以上日记，时间跨度 > 4小时
     */
    private suspend fun checkTimeTraveler(createdAt: Long, petDao: PetDao): Boolean {
        val dayMs = 24L * 60 * 60 * 1000
        val calendar = java.util.Calendar.getInstance()

        calendar.timeInMillis = createdAt
        val dayStart = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + dayMs

        // 获取这一天的所有日记
        val entriesForDay = petDao.getEntriesForDay(dayStart, dayEnd)

        if (entriesForDay.size < 2) return false

        // 检查时间跨度是否 > 4小时
        val timeSpanHours = (entriesForDay.last().createdAt - entriesForDay.first().createdAt) / (60 * 60 * 1000.0)
        return timeSpanHours > 4.0
    }
}
