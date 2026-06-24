package com.diary.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 宠物记忆仓库
 * 管理宠物的记忆系统，包括纪念记忆和习惯记忆
 */
class PetMemoryRepository(private val petDao: PetDao) {

    companion object {
        // 纪念节点定义
        private val MILESTONE_ENTRIES = listOf(1, 10, 50, 100, 365)
        private val MILESTONE_STREAKS = listOf(7, 30, 100)

        // 记忆衰减周期（毫秒）
        private const val DECAY_INTERVAL_MS = 24 * 60 * 60 * 1000L // 每天衰减一次
        private const val DECAY_AMOUNT = 0.02f // 每次衰减值
    }

    /**
     * 保存记忆
     */
    suspend fun saveMemory(
        type: PetMemoryType,
        content: String,
        relatedEntryId: Long? = null,
        triggerText: String = ""
    ): Long {
        val memory = PetMemory(
            type = type.name,
            content = content,
            relatedEntryId = relatedEntryId,
            triggerText = triggerText,
            strength = 1.0f
        )
        return petDao.insertMemory(memory)
    }

    /**
     * 获取指定类型的记忆
     */
    suspend fun getMemoriesByType(type: PetMemoryType): List<PetMemory> {
        return petDao.getMemoriesByType(type.name)
    }

    /**
     * 获取最近的记忆
     */
    suspend fun getRecentMemories(limit: Int = 10): List<PetMemory> {
        return petDao.getRecentMemories(limit)
    }

    /**
     * 衰减旧记忆的强度
     * 应该在每次保存日记时调用
     */
    suspend fun decayMemories() {
        petDao.decayAllMemories(DECAY_AMOUNT)

        // 删除过期记忆（强度低于0.1且超过30天）
        val expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        petDao.deleteExpiredMemories(expireTime)
    }

    /**
     * 检查纪念节点并保存记忆
     * @param entryCount 总日记数
     * @param streakDays 连续记录天数
     * @param lastEntryTime 上次记录时间
     * @param petName 宠物名称
     * @return 触发的纪念文案，如果没有触发则返回null
     */
    suspend fun checkMilestones(
        entryCount: Int,
        streakDays: Int,
        lastEntryTime: Long,
        petName: String
    ): String? {
        // 检查日记数量里程碑
        for (milestone in MILESTONE_ENTRIES) {
            if (entryCount == milestone) {
                val content = "第${milestone}篇日记"
                val triggerText = generateEntryMilestoneText(milestone, petName)

                // 检查是否已记录过
                val exists = petDao.countMemoriesByContent(
                    PetMemoryType.MILESTONE.name,
                    "%第${milestone}篇%"
                )
                if (exists == 0) {
                    saveMemory(
                        type = PetMemoryType.MILESTONE,
                        content = content,
                        triggerText = triggerText
                    )
                    return triggerText
                }
            }
        }

        // 检查连续记录里程碑
        for (milestone in MILESTONE_STREAKS) {
            if (streakDays == milestone) {
                val content = "连续${milestone}天"
                val triggerText = generateStreakMilestoneText(milestone, petName)

                // 检查是否已记录过
                val exists = petDao.countMemoriesByContent(
                    PetMemoryType.MILESTONE.name,
                    "%连续${milestone}天%"
                )
                if (exists == 0) {
                    saveMemory(
                        type = PetMemoryType.MILESTONE,
                        content = content,
                        triggerText = triggerText
                    )
                    return triggerText
                }
            }
        }

        // 检查中断后回归
        if (lastEntryTime > 0) {
            val daysSinceLastEntry = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - lastEntryTime
            )
            if (daysSinceLastEntry >= 3) {
                // 中断3天以上后回归
                val content = "中断${daysSinceLastEntry}天后回归"
                val triggerText = "你回来了。${petName}一直在等你。"

                // 只在首次回归时触发（检查最近24小时内是否有回归记忆）
                val recentGapMemories = petDao.getRecentMemories(5)
                val hasRecentGap = recentGapMemories.any {
                    it.type == PetMemoryType.GAP.name &&
                    System.currentTimeMillis() - it.createdAt < TimeUnit.HOURS.toMillis(24)
                }

                if (!hasRecentGap) {
                    saveMemory(
                        type = PetMemoryType.GAP,
                        content = content,
                        triggerText = triggerText
                    )
                    return triggerText
                }
            }
        }

        return null
    }

    /**
     * 记录并分析写作习惯
     * @param entryTime 日记写作时间
     * @param petName 宠物名称
     * @return 习惯触发文案，如果没有变化则返回null
     */
    suspend fun analyzeAndSaveHabit(
        entryTime: Long,
        petName: String
    ): String? {
        // 获取最近30篇日记的时间分布
        val recentMemories = petDao.getMemoriesByType(PetMemoryType.HABIT.name)

        // 解析现有的习惯数据
        val habitData = parseHabitData(recentMemories)

        // 添加新的写作时间
        val hour = getHourOfDay(entryTime)
        habitData.add(hour)

        // 只保留最近30个数据点
        while (habitData.size > 30) {
            habitData.removeAt(0)
        }

        // 分析习惯类型
        val habitType = determineHabitType(habitData)

        // 保存习惯记忆
        val content = JSONObject().apply {
            put("habitType", habitType.name)
            put("description", habitType.description)
            put("dataPoints", JSONArray(habitData))
        }.toString()

        val triggerText = generateHabitText(habitType, petName)

        // 查找并更新或创建习惯记忆
        val existingHabit = petDao.getStrongestMemory(PetMemoryType.HABIT.name)
        if (existingHabit != null) {
            // 更新现有习惯记忆
            petDao.updateMemoryStrength(
                memoryId = existingHabit.id,
                newStrength = 1.0f
            )
            // 更新内容
            val updatedMemory = existingHabit.copy(
                content = content,
                triggerText = triggerText
            )
            petDao.insertMemory(updatedMemory)
        } else {
            saveMemory(
                type = PetMemoryType.HABIT,
                content = content,
                triggerText = triggerText
            )
        }

        return triggerText
    }

    /**
     * 获取当前的习惯记忆文案
     */
    suspend fun getCurrentHabitText(petName: String): String? {
        val habitMemory = petDao.getStrongestMemory(PetMemoryType.HABIT.name) ?: return null
        val habitData = parseHabitData(listOf(habitMemory))

        if (habitData.size < 5) return null // 数据不足

        val habitType = determineHabitType(habitData)
        return generateHabitText(habitType, petName)
    }

    /**
     * 获取最近的纪念记忆文案
     */
    suspend fun getRecentMilestoneText(): String? {
        val milestoneMemory = petDao.getStrongestMemory(PetMemoryType.MILESTONE.name)
        return milestoneMemory?.triggerText
    }

    // ==================== 私有方法 ====================

    /**
     * 解析习惯数据
     */
    private fun parseHabitData(memories: List<PetMemory>): MutableList<Int> {
        val data = mutableListOf<Int>()
        for (memory in memories) {
            try {
                val json = JSONObject(memory.content)
                val dataArray = json.getJSONArray("dataPoints")
                for (i in 0 until dataArray.length()) {
                    data.add(dataArray.getInt(i))
                }
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }
        return data
    }

    /**
     * 获取小时数
     */
    private fun getHourOfDay(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    /**
     * 判断习惯类型
     */
    private fun determineHabitType(hours: List<Int>): HabitType {
        if (hours.isEmpty()) return HabitType.SCATTERED

        val total = hours.size.toFloat()

        // 统计各时段分布
        val nightOwlCount = hours.count { it in 22..23 || it in 0..1 }.toFloat()
        val earlyBirdCount = hours.count { it in 5..7 }.toFloat()
        val afternoonCount = hours.count { it in 12..14 }.toFloat()

        // 检查是否集中在某个时段
        if (nightOwlCount / total >= 0.7f) return HabitType.NIGHT_OWL
        if (earlyBirdCount / total >= 0.7f) return HabitType.EARLY_BIRD
        if (afternoonCount / total >= 0.7f) return HabitType.AFTERNOON

        // 检查是否规律（标准差小）
        if (hours.size >= 5) {
            val avg = hours.average()
            val variance = hours.map { (it - avg) * (it - avg) }.average()
            val stdDev = Math.sqrt(variance)

            if (stdDev < 1.5) return HabitType.REGULAR
        }

        return HabitType.SCATTERED
    }

    /**
     * 生成纪念文案
     */
    private fun generateEntryMilestoneText(milestone: Int, petName: String): String {
        return when (milestone) {
            1 -> "这是我们的第一次见面~"
            10 -> "已经写了10篇日记了呢！"
            50 -> "50篇！你坚持下来了！"
            100 -> "100篇！${petName}好感动！"
            365 -> "一整年！${petName}陪你走过了365天！"
            else -> "又达成了一个里程碑！"
        }
    }

    /**
     * 生成连续记录文案
     */
    private fun generateStreakMilestoneText(streak: Int, petName: String): String {
        return when (streak) {
            7 -> "一周不落！${petName}看到了你的坚持！"
            30 -> "30天！你太厉害了！${petName}为你骄傲！"
            100 -> "100天连续记录！${petName}都不敢相信！"
            else -> "连续记录${streak}天了！"
        }
    }

    /**
     * 生成习惯文案
     */
    private fun generateHabitText(habitType: HabitType, petName: String): String {
        return when (habitType) {
            HabitType.NIGHT_OWL -> "又熬夜写日记啦？夜猫子~"
            HabitType.EARLY_BIRD -> "早起写日记，好习惯！"
            HabitType.AFTERNOON -> "午休时间写日记呢~"
            HabitType.SCATTERED -> "你的写作时间很灵活呢"
            HabitType.REGULAR -> "你每天都差不多这个时候写，好规律！"
        }
    }
}
