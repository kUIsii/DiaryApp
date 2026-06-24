package com.diary.app.data

import kotlinx.coroutines.flow.Flow

/**
 * 小岛数据仓库
 */
class IslandRepository(private val islandDao: IslandDao) {

    /**
     * 获取小岛环境
     */
    fun getEnvironment(): Flow<IslandEnvironment?> = islandDao.getEnvironment()

    /**
     * 获取小岛配置
     */
    fun getProfile(): Flow<IslandProfile?> = islandDao.getProfile()

    /**
     * 获取所有装饰
     */
    fun getAllDecorations(): Flow<List<IslandDecoration>> = islandDao.getAllDecorations()

    /**
     * 获取已解锁的装饰
     */
    fun getUnlockedDecorations(): Flow<List<IslandDecoration>> = islandDao.getUnlockedDecorations()

    /**
     * 获取已解锁的组合
     */
    fun getUnlockedCombos(): Flow<List<IslandCombo>> = islandDao.getUnlockedCombos()

    /**
     * 所有组合配方定义
     */
    private val comboDefinitions = listOf(
        ComboDefinition(
            id = "warm_home",
            name = "温馨家园",
            requiredDecorations = listOf("cabin", "flowers"),
            effectDescription = "小木屋周围出现花丛，窗户灯光变暖",
            unlockMessage = "有了花的陪伴，小屋更加温馨"
        ),
        ComboDefinition(
            id = "watchtower",
            name = "守望灯塔",
            requiredDecorations = listOf("lighthouse", "bridge"),
            effectDescription = "灯塔光芒照亮桥面，夜间可见",
            unlockMessage = "灯塔为归途的人指引方向"
        ),
        ComboDefinition(
            id = "eco_paradise",
            name = "生态乐园",
            requiredDecorations = listOf("tree", "fountain", "grass"),
            effectDescription = "出现蝴蝶群和小鸟",
            unlockMessage = "这里成了小动物们的乐园"
        ),
        ComboDefinition(
            id = "wind_valley",
            name = "风之谷",
            requiredDecorations = listOf("windmill", "tree"),
            effectDescription = "风车带动树叶飘落",
            unlockMessage = "风带来了远方的消息"
        ),
        ComboDefinition(
            id = "quiet_shore",
            name = "静谧水岸",
            requiredDecorations = listOf("fountain", "bridge"),
            effectDescription = "水面倒映桥影",
            unlockMessage = "水面如镜，映照着宁静"
        )
    )

    /**
     * 切换装饰的装备状态
     * 返回更新后的已装备装饰列表
     */
    suspend fun toggleDecoration(decorationId: String): List<String> {
        val profile = islandDao.getProfileOnce() ?: IslandProfile()
        val currentActive = try {
            val raw = profile.activeDecorations
            if (raw.isBlank() || raw == "[]") emptyList()
            else raw.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }

        val newActive = if (decorationId in currentActive) {
            currentActive - decorationId
        } else {
            if (currentActive.size >= 5) currentActive else currentActive + decorationId
        }

        val jsonStr = "[${newActive.joinToString(",") { "\"$it\"" }}]"
        islandDao.updateActiveDecorations(jsonStr)
        return newActive
    }

    /**
     * 获取已装备的装饰ID列表
     */
    suspend fun getActiveDecorationIds(): List<String> {
        val profile = islandDao.getProfileOnce() ?: IslandProfile()
        return try {
            val raw = profile.activeDecorations
            if (raw.isBlank() || raw == "[]") emptyList()
            else raw.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 处理日记保存后的小岛更新
     */
    suspend fun onEntrySaved(
        entryId: Long,
        plainText: String,
        moodLevel: Int?,
        weather: String?,
        createdAt: Long
    ): IslandUpdateResult {
        // 获取当前环境
        val currentEnv = islandDao.getEnvironmentOnce() ?: IslandEnvironment()
        val currentProfile = islandDao.getProfileOnce() ?: IslandProfile()

        // 计算环境变化
        val (delta, baseExp) = MoodEnvironmentMapper.mapMoodToEnvironment(
            moodLevel = moodLevel,
            currentEnvironment = currentEnv,
            weather = weather,
            plainText = plainText
        )

        // 应用连续记录倍率
        val streakMultiplier = MoodEnvironmentMapper.getStreakMultiplier(currentProfile.streakDays)
        val totalExp = (baseExp * streakMultiplier).toInt()

        // 更新环境
        val newLushness = (currentEnv.lushness + delta.lushness).coerceIn(0f, 1f)
        val newBrightness = (currentEnv.brightness + delta.brightness).coerceIn(0f, 1f)
        val newTranquility = (currentEnv.tranquility + delta.tranquility).coerceIn(0f, 1f)
        val newWarmth = (currentEnv.warmth + delta.warmth).coerceIn(0f, 1f)

        islandDao.updateEnvironment(
            lushness = newLushness,
            brightness = newBrightness,
            tranquility = newTranquility,
            warmth = newWarmth,
            updatedAt = createdAt
        )

        // 更新经验值和等级
        val newExp = currentProfile.experience + totalExp
        val expForNextLevel = MoodEnvironmentMapper.getExperienceForLevel(currentProfile.level + 1)
        val newLevel = if (newExp >= expForNextLevel) currentProfile.level + 1 else currentProfile.level

        // 计算连续记录天数
        val daysSinceLastEntry = if (currentProfile.lastEntryTime > 0) {
            (createdAt - currentProfile.lastEntryTime) / (24 * 60 * 60 * 1000)
        } else {
            Long.MAX_VALUE
        }
        val newStreak = when {
            daysSinceLastEntry <= 1 -> currentProfile.streakDays + 1
            daysSinceLastEntry <= 2 -> currentProfile.streakDays + 1
            else -> 1
        }

        // 更新配置
        islandDao.setProfile(
            currentProfile.copy(
                level = newLevel,
                experience = newExp,
                totalEntries = currentProfile.totalEntries + 1,
                streakDays = newStreak,
                lastEntryTime = createdAt
            )
        )

        // 检查新解锁的装饰
        val newlyUnlocked = checkNewDecorations(newLevel)

        // --- 记录时间线事件 ---
        // 首次记录
        if (currentProfile.totalEntries == 0) {
            recordTimelineEvent(
                type = TimelineEventType.FIRST_ENTRY,
                message = "小岛苏醒了",
                relatedEntryId = entryId
            )
        }
        // 等级提升
        if (newLevel > currentProfile.level) {
            recordTimelineEvent(
                type = TimelineEventType.LEVEL_UP,
                message = "小岛达到了Lv.${newLevel}",
                relatedEntryId = entryId,
                metadata = "{\"oldLevel\":${currentProfile.level},\"newLevel\":${newLevel}}"
            )
        }
        // 装饰解锁
        for (decoration in newlyUnlocked) {
            val isAnimal = decoration.type == "animal"
            recordTimelineEvent(
                type = if (isAnimal) TimelineEventType.ANIMAL_ARRIVE else TimelineEventType.DECORATION_UNLOCK,
                message = if (isAnimal) "一只${decoration.name}来到了小岛" else "新装饰: ${decoration.name}来到小岛",
                relatedEntryId = entryId
            )
        }

        // 记录更新
        val update = IslandUpdate(
            entryId = entryId,
            lushnessDelta = delta.lushness,
            brightnessDelta = delta.brightness,
            tranquilityDelta = delta.tranquility,
            warmthDelta = delta.warmth,
            experienceGained = totalExp
        )
        islandDao.insertUpdate(update)

        return IslandUpdateResult(
            update = update,
            newLevel = newLevel,
            newlyUnlockedDecorations = newlyUnlocked,
            experienceGained = totalExp
        )
    }

    /**
     * 检查新解锁的装饰
     */
    private suspend fun checkNewDecorations(currentLevel: Int): List<IslandDecoration> {
        val allDecorations = islandDao.getUnlockedDecorationsOnce()
        val newUnlocked = mutableListOf<IslandDecoration>()

        // 获取所有装饰定义
        val decorationDefinitions = getAllDecorationDefinitions()

        for (decoration in decorationDefinitions) {
            if (decoration.unlockLevel <= currentLevel && !allDecorations.any { it.id == decoration.id }) {
                islandDao.unlockDecoration(decoration.id)
                newUnlocked.add(decoration.copy(isUnlocked = true))
            }
        }

        return newUnlocked
    }

    /**
     * 初始化装饰数据
     */
    suspend fun initializeDecorations() {
        val decorations = getAllDecorationDefinitions()
        islandDao.insertDecorations(decorations)
    }

    /**
     * 获取所有装饰定义
     */
    private fun getAllDecorationDefinitions(): List<IslandDecoration> {
        return listOf(
            // 建筑
            IslandDecoration("cabin", "小木屋", 5, "达到5级解锁", "building", 4, 0.5f, 0.6f),
            IslandDecoration("lighthouse", "灯塔", 15, "连续7天记录", "building", 4, 0.8f, 0.4f),
            IslandDecoration("bridge", "桥梁", 20, "达到20级解锁", "building", 4, 0.3f, 0.7f),
            IslandDecoration("fountain", "喷泉", 25, "使用5种心情", "building", 4, 0.6f, 0.5f),
            IslandDecoration("statue", "守护者雕像", 40, "连续30天记录", "building", 4, 0.5f, 0.3f),
            IslandDecoration("windmill", "风车", 16, "达到16级解锁", "building", 4, 0.25f, 0.45f),

            // 动物
            IslandDecoration("bird", "小鸟", 3, "达到3级解锁", "animal", 5, 0.2f, 0.2f),
            IslandDecoration("butterfly", "蝴蝶", 8, "积极心情解锁", "animal", 5, 0.7f, 0.3f),
            IslandDecoration("squirrel", "松鼠", 12, "达到12级解锁", "animal", 5, 0.4f, 0.5f),
            IslandDecoration("owl", "猫头鹰", 18, "夜间写作解锁", "animal", 5, 0.8f, 0.2f),
            IslandDecoration("dragon", "巨龙", 50, "连续100天记录", "animal", 5, 0.5f, 0.1f),

            // 植被
            IslandDecoration("flowers", "花海", 6, "达到6级解锁", "vegetation", 3, 0.3f, 0.6f),
            IslandDecoration("tree", "大树", 10, "达到10级解锁", "vegetation", 3, 0.7f, 0.5f),
            IslandDecoration("grass", "草地", 2, "达到2级解锁", "vegetation", 3, 0.5f, 0.8f),

            // 特效
            IslandDecoration("rainbow", "彩虹", 30, "达到30级解锁", "effect", 6, 0.5f, 0.1f),
            IslandDecoration("fireflies", "萤火虫", 22, "达到22级解锁", "effect", 6, 0.3f, 0.4f),
            IslandDecoration("aurora", "极光", 35, "达到35级解锁", "effect", 6, 0.5f, 0.05f),

            // 特殊装饰（连续记录解锁）
            IslandDecoration("honor_flagpole", "荣誉旗杆", 99, "连续30天记录解锁", "building", 4, 0.5f, 0.4f)
        )
    }

    /**
     * 获取所有组合定义
     */
    fun getAllComboDefinitions(): List<ComboDefinition> = comboDefinitions

    /**
     * 检查当前装备的装饰触发的组合
     * 返回新解锁的组合列表
     */
    suspend fun checkCombos(equippedIds: List<String>): List<ComboDefinition> {
        val alreadyUnlocked = islandDao.getUnlockedCombosOnce()
        val newlyUnlocked = mutableListOf<ComboDefinition>()

        for (combo in comboDefinitions) {
            val isUnlocked = alreadyUnlocked.any { it.comboId == combo.id && it.isUnlocked }
            if (isUnlocked) continue

            // 检查是否拥有所有需要的装饰
            val hasAll = combo.requiredDecorations.all { it in equippedIds }
            if (hasAll) {
                islandDao.insertCombo(
                    IslandCombo(
                        id = combo.id,
                        comboId = combo.id,
                        isUnlocked = true,
                        unlockedAt = System.currentTimeMillis()
                    )
                )
                newlyUnlocked.add(combo)
                // 记录时间线事件
                recordTimelineEvent(
                    type = TimelineEventType.COMBO_ACTIVATE,
                    message = "组合效果: ${combo.name}被激活"
                )
            }
        }

        return newlyUnlocked
    }

    // ==================== 隐藏发现系统 ====================

    /**
     * 获取所有发现记录
     */
    fun getAllDiscoveries(): Flow<List<IslandDiscovery>> = islandDao.getAllDiscoveries()

    /**
     * 获取当前激活的稀有元素
     */
    suspend fun getActiveRareElements(): List<IslandDiscovery> {
        islandDao.cleanupExpiredDiscoveries()
        return islandDao.getActiveRareElements()
    }

    /**
     * 检查是否已发现某个元素
     */
    suspend fun hasDiscovered(key: String): Boolean = islandDao.hasDiscovered(key)

    /**
     * 记录发现
     */
    suspend fun recordDiscovery(
        type: DiscoveryType,
        key: String,
        message: String,
        durationMinutes: Int = -1,
        metadata: String? = null
    ): Long? {
        if (islandDao.hasDiscovered(key)) return null

        val expiresAt = if (durationMinutes > 0) {
            System.currentTimeMillis() + durationMinutes * 60 * 1000L
        } else {
            -1L  // 永久
        }

        val discovery = IslandDiscovery(
            discoveryType = type.name.lowercase(),
            discoveryKey = key,
            message = message,
            expiresAt = expiresAt,
            metadata = metadata
        )
        return islandDao.insertDiscovery(discovery)
    }

    /**
     * 在日记保存时检测隐藏发现触发条件
     * 返回新发现的列表
     */
    suspend fun checkDiscoveries(
        plainText: String,
        moodLevel: Int?,
        weather: String?,
        createdAt: Long
    ): List<IslandDiscovery> {
        val newDiscoveries = mutableListOf<IslandDiscovery>()
        val profile = islandDao.getProfileOnce() ?: IslandProfile()
        val environment = islandDao.getEnvironmentOnce() ?: IslandEnvironment()

        // 清理过期发现
        islandDao.cleanupExpiredDiscoveries()

        // 检测各种稀有元素触发条件
        checkRareElementTriggers(
            profile = profile,
            environment = environment,
            plainText = plainText,
            moodLevel = moodLevel,
            weather = weather,
            createdAt = createdAt,
            newDiscoveries = newDiscoveries
        )

        // 检测季节性场景
        checkSeasonalTriggers(
            weather = weather,
            createdAt = createdAt,
            newDiscoveries = newDiscoveries
        )

        return newDiscoveries
    }

    /**
     * 检测稀有元素触发条件
     */
    private suspend fun checkRareElementTriggers(
        profile: IslandProfile,
        environment: IslandEnvironment,
        plainText: String,
        moodLevel: Int?,
        weather: String?,
        createdAt: Long,
        newDiscoveries: MutableList<IslandDiscovery>
    ) {
        val now = java.time.LocalDateTime.now()
        val hour = now.hour

        // 狼人剪影: 满月 + 夜间23:00-1:00
        if (hour in 23..23 || hour in 0..0) {
            if (isFullMoon(now.toLocalDate())) {
                val discovery = recordDiscovery(
                    type = DiscoveryType.RARE_ELEMENT,
                    key = "werewolf_${now.toLocalDate()}",
                    message = RareElement.WEREWOLF.message,
                    durationMinutes = RareElement.WEREWOLF.durationMinutes
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == "werewolf_${now.toLocalDate()}" })
                    recordTimelineEvent(
                        type = TimelineEventType.RARE_DISCOVERY,
                        message = "发现了: ${RareElement.WEREWOLF.displayName}"
                    )
                }
            }
        }

        // 彩虹桥: 连续7天好心情
        if (profile.streakDays >= 7 && moodLevel != null && moodLevel >= 4) {
            if (!islandDao.hasDiscovered("rainbow_bridge")) {
                val discovery = recordDiscovery(
                    type = DiscoveryType.RARE_ELEMENT,
                    key = "rainbow_bridge",
                    message = RareElement.RAINBOW_BRIDGE.message,
                    durationMinutes = RareElement.RAINBOW_BRIDGE.durationMinutes
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == "rainbow_bridge" })
                    recordTimelineEvent(
                        type = TimelineEventType.RARE_DISCOVERY,
                        message = "发现了: ${RareElement.RAINBOW_BRIDGE.displayName}"
                    )
                }
            }
        }

        // 精灵之光: 累计记录100篇
        if (profile.totalEntries >= 100) {
            if (!islandDao.hasDiscovered("elf_light")) {
                val discovery = recordDiscovery(
                    type = DiscoveryType.RARE_ELEMENT,
                    key = "elf_light",
                    message = RareElement.ELF_LIGHT.message,
                    durationMinutes = RareElement.ELF_LIGHT.durationMinutes
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == "elf_light" })
                    recordTimelineEvent(
                        type = TimelineEventType.RARE_DISCOVERY,
                        message = "发现了: ${RareElement.ELF_LIGHT.displayName}"
                    )
                }
            }
        }

        // 记忆树: 连续记录365天
        if (profile.streakDays >= 365) {
            if (!islandDao.hasDiscovered("memory_tree")) {
                val discovery = recordDiscovery(
                    type = DiscoveryType.RARE_ELEMENT,
                    key = "memory_tree",
                    message = RareElement.MEMORY_TREE.message,
                    durationMinutes = -1  // 永久
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == "memory_tree" })
                    recordTimelineEvent(
                        type = TimelineEventType.RARE_DISCOVERY,
                        message = "发现了: ${RareElement.MEMORY_TREE.displayName}"
                    )
                }
            }
        }

        // 彩蛋: 清明节 + 写下"思念"
        if (isQingming(now.toLocalDate()) && plainText.contains("思念")) {
            val today = now.toLocalDate().toString()
            val discovery = recordDiscovery(
                type = DiscoveryType.EGG,
                key = "easter_egg_$today",
                message = RareElement.EASTER_EGG.message,
                durationMinutes = 1440  // 1天
            )
            if (discovery != null) {
                newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == "easter_egg_$today" })
                recordTimelineEvent(
                    type = TimelineEventType.RARE_DISCOVERY,
                    message = "发现了: ${RareElement.EASTER_EGG.displayName}"
                )
            }
        }

        // 烟花: 新年钟声敲响 (1月1日 0:00-0:05)
        if (now.month == java.time.Month.JANUARY && now.dayOfMonth == 1 && hour == 0 && now.minute < 5) {
            val today = now.toLocalDate().toString()
            val discovery = recordDiscovery(
                type = DiscoveryType.RARE_ELEMENT,
                key = "fireworks_$today",
                message = RareElement.FIREWORKS.message,
                durationMinutes = RareElement.FIREWORKS.durationMinutes
            )
            if (discovery != null) {
                newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == "fireworks_$today" })
                recordTimelineEvent(
                    type = TimelineEventType.RARE_DISCOVERY,
                    message = "发现了: ${RareElement.FIREWORKS.displayName}"
                )
            }
        }

        // 极光: 冬季 + 宁静度>0.9 + 夜间
        if (isWinter(now.toLocalDate()) && environment.tranquility > 0.9f && isNightTime(hour)) {
            val nightKey = "aurora_${now.toLocalDate()}"
            val discovery = recordDiscovery(
                type = DiscoveryType.RARE_ELEMENT,
                key = nightKey,
                message = RareElement.AURORA.message,
                durationMinutes = RareElement.AURORA.durationMinutes
            )
            if (discovery != null) {
                newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == nightKey })
                recordTimelineEvent(
                    type = TimelineEventType.RARE_DISCOVERY,
                    message = "发现了: ${RareElement.AURORA.displayName}"
                )
            }
        }
    }

    /**
     * 检测季节性场景
     */
    private suspend fun checkSeasonalTriggers(
        weather: String?,
        createdAt: Long,
        newDiscoveries: MutableList<IslandDiscovery>
    ) {
        val now = java.time.LocalDateTime.now()
        val month = now.monthValue
        val dayOfMonth = now.dayOfMonth

        // 春季 (3-5月)
        if (month in 3..5) {
            // 樱花飘落: 3-4月
            if (month in 3..4) {
                val key = "cherry_blossom_${now.toLocalDate()}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.CHERRY_BLOSSOM.message,
                    durationMinutes = 1440
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }

            // 清明雨: 4月初 (4月4-6日)
            if (month == 4 && dayOfMonth in 4..6 && weather == "rain") {
                val key = "qingming_rain_${now.toLocalDate()}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.QINGMING_RAIN.message,
                    durationMinutes = 1440
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }
        }

        // 夏季 (6-8月)
        if (month in 6..8) {
            // 萤火虫海: 7月夜晚
            if (month == 7 && isNightTime(now.hour)) {
                val key = "firefly_sea_${now.toLocalDate()}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.FIREFLY_SEA.message,
                    durationMinutes = 720  // 12小时
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }

            // 雷阵雨: 夏季午后 (14-17点)
            if (month in 6..8 && now.hour in 14..17 && weather == "rain") {
                val key = "thunder_rain_${now.toLocalDate()}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.THUNDER_RAIN.message,
                    durationMinutes = 120
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }
        }

        // 秋季 (9-11月)
        if (month in 9..11) {
            // 红叶雨: 10月
            if (month == 10) {
                val key = "red_leaves_${now.toLocalDate()}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.RED_LEAVES.message,
                    durationMinutes = 1440
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }

            // 丰收果实: 10-11月
            if (month in 10..11) {
                val key = "harvest_fruits_${now.toLocalDate()}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.HARVEST_FRUITS.message,
                    durationMinutes = 1440
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }
        }

        // 冬季 (12-2月)
        if (month in listOf(12, 1, 2)) {
            // 初雪: 12月首次降雪
            if (month == 12 && dayOfMonth <= 10 && weather == "snow") {
                val key = "first_snow_${now.year}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.FIRST_SNOW.message,
                    durationMinutes = 1440
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }

            // 圣诞彩灯: 12月24-25日
            if (month == 12 && dayOfMonth in 24..25) {
                val key = "christmas_lights_${now.toLocalDate()}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.CHRISTMAS_LIGHTS.message,
                    durationMinutes = 2880  // 2天
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }

            // 春节灯笼: 春节前后 (大约1月20-2月15日)
            if ((month == 1 && dayOfMonth >= 20) || (month == 2 && dayOfMonth <= 15)) {
                val key = "spring_festival_${now.year}"
                val discovery = recordDiscovery(
                    type = DiscoveryType.SEASONAL,
                    key = key,
                    message = SeasonalScene.SPRING_FESTIVAL.message,
                    durationMinutes = 4320  // 3天
                )
                if (discovery != null) {
                    newDiscoveries.add(islandDao.getAllDiscoveriesOnce().first { it.discoveryKey == key })
                }
            }
        }
    }

    /**
     * 判断是否满月（简化算法）
     */
    private fun isFullMoon(date: java.time.LocalDate): Boolean {
        val knownFullMoon = java.time.LocalDate.of(2024, 1, 25)
        val daysSinceKnown = java.time.temporal.ChronoUnit.DAYS.between(knownFullMoon, date)
        val lunarCycle = 29.5
        val phase = (daysSinceKnown % lunarCycle) / lunarCycle
        return phase < 0.05 || phase > 0.95 || (phase in 0.45..0.55)
    }

    /**
     * 判断是否清明节（4月4-6日）
     */
    private fun isQingming(date: java.time.LocalDate): Boolean {
        return date.month == java.time.Month.APRIL && date.dayOfMonth in 4..6
    }

    /**
     * 判断是否冬季（12-2月）
     */
    private fun isWinter(date: java.time.LocalDate): Boolean {
        return date.month in listOf(java.time.Month.DECEMBER, java.time.Month.JANUARY, java.time.Month.FEBRUARY)
    }

    /**
     * 判断是否夜间（19点-5点）
     */
    private fun isNightTime(hour: Int): Boolean {
        return hour >= 19 || hour < 5
    }

    // ==================== 历史时间线系统 ====================

    /**
     * 记录时间线事件
     */
    suspend fun recordTimelineEvent(
        type: TimelineEventType,
        message: String,
        relatedEntryId: Long? = null,
        metadata: String? = null
    ): Long? {
        // 去重：首次记录只记录一次
        if (type == TimelineEventType.FIRST_ENTRY && islandDao.hasTimelineEvent(type.name, message)) {
            return null
        }
        val event = IslandTimelineEvent(
            eventType = type.name,
            message = message,
            relatedEntryId = relatedEntryId,
            metadata = metadata
        )
        return islandDao.insertTimelineEvent(event)
    }

    /**
     * 获取所有时间线事件
     */
    fun getAllTimelineEvents() = islandDao.getAllTimelineEvents()

    /**
     * 按类型筛选时间线事件
     */
    suspend fun getTimelineEventsByType(type: TimelineEventType) =
        islandDao.getTimelineEventsByType(type.name)
}

/**
 * 小岛更新结果
 */
data class IslandUpdateResult(
    val update: IslandUpdate,
    val newLevel: Int,
    val newlyUnlockedDecorations: List<IslandDecoration>,
    val experienceGained: Int
)
