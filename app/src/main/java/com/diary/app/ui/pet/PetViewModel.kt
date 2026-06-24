package com.diary.app.ui.pet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.FeedbackGenerator
import com.diary.app.data.FeedbackStyle
import com.diary.app.data.FeedbackTrigger
import com.diary.app.data.PetGrowthStage
import com.diary.app.data.PetHiddenStateType
import com.diary.app.data.PetMemoryRepository
import com.diary.app.data.PetPersonality
import com.diary.app.data.PetPersonalityAnalyzer
import com.diary.app.data.PetProfile
import com.diary.app.data.PetState
import com.diary.app.data.PetStateRecord
import com.diary.app.data.PetStateMachine
import com.diary.app.data.SentimentAnalyzer
import com.diary.app.data.CrossSystemManager
import com.diary.app.data.PetAiGenerator
import com.diary.app.data.TitleUnlockEvent
import com.diary.app.data.TitleManager
import com.diary.app.data.ActiveCombination
import com.diary.app.data.CombinationEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 宠物交互类型
 */
enum class InteractionType {
    NONE, TAP, GROOM, FED, LONG_PRESS
}

/**
 * 心情历史日期数据
 */
data class MoodDayData(
    val dayLabel: String,
    val state: PetState?,
    val isToday: Boolean
)

/**
 * 心情分布数据
 */
data class MoodDistributionItem(
    val state: PetState,
    val count: Int
)

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as DiaryApplication).database
    private val petDao = database.petDao()
    private val diaryDao = database.diaryDao()
    private val memoryRepository = PetMemoryRepository(petDao)
    private val aiGenerator = PetAiGenerator(application)

    // 宠物配置
    private val _petProfile = MutableStateFlow<PetProfile?>(null)
    val petProfile: StateFlow<PetProfile?> = _petProfile.asStateFlow()

    // 宠物性格
    private val _personality = MutableStateFlow<PetPersonality?>(null)
    val personality: StateFlow<PetPersonality?> = _personality.asStateFlow()

    // 当前状态
    private val _currentState = MutableStateFlow(PetState.CALM)
    val currentState: StateFlow<PetState> = _currentState.asStateFlow()

    // 反馈文案
    private val _feedbackText = MutableStateFlow("")
    val feedbackText: StateFlow<String> = _feedbackText.asStateFlow()

    // 最近的反馈触发类型
    private val _lastTrigger = MutableStateFlow<FeedbackTrigger?>(null)
    val lastTrigger: StateFlow<FeedbackTrigger?> = _lastTrigger.asStateFlow()

    // 交互类型（用于驱动 UI 动画）
    private val _interactionType = MutableStateFlow(InteractionType.NONE)
    val interactionType: StateFlow<InteractionType> = _interactionType.asStateFlow()

    // 交互计数器（每次交互递增，触发 LaunchedEffect）
    private val _interactionCounter = MutableStateFlow(0)
    val interactionCounter: StateFlow<Int> = _interactionCounter.asStateFlow()

    // 饱食度（内存态，不持久化）
    private var _isFull = false

    // 心情历史（最近7天）
    private val _moodHistory = MutableStateFlow<List<MoodDayData>>(emptyList())
    val moodHistory: StateFlow<List<MoodDayData>> = _moodHistory.asStateFlow()

    // 心情分布
    private val _moodDistribution = MutableStateFlow<List<MoodDistributionItem>>(emptyList())
    val moodDistribution: StateFlow<List<MoodDistributionItem>> = _moodDistribution.asStateFlow()

    // 记忆触发文案
    private val _memoryTrigger = MutableStateFlow<String?>(null)
    val memoryTrigger: StateFlow<String?> = _memoryTrigger.asStateFlow()

    // 成长阶段
    private val _growthStage = MutableStateFlow(PetGrowthStage.JUVENILE)
    val growthStage: StateFlow<PetGrowthStage> = _growthStage.asStateFlow()

    // 进化提示
    private val _evolutionHint = MutableStateFlow<String?>(null)
    val evolutionHint: StateFlow<String?> = _evolutionHint.asStateFlow()

    // 上一次成长阶段（用于检测进化动画）
    private var previousStage: PetGrowthStage = PetGrowthStage.JUVENILE

    // 当前激活的隐藏状态
    private val _activeHiddenState = MutableStateFlow<PetHiddenStateType?>(null)
    val activeHiddenState: StateFlow<PetHiddenStateType?> = _activeHiddenState.asStateFlow()

    // 已发现的隐藏状态数量
    private val _discoveredHiddenCount = MutableStateFlow(0)
    val discoveredHiddenCount: StateFlow<Int> = _discoveredHiddenCount.asStateFlow()

    // 当前激活的称号组合
    private val _activeCombinations = MutableStateFlow<List<ActiveCombination>>(emptyList())
    val activeCombinations: StateFlow<List<ActiveCombination>> = _activeCombinations.asStateFlow()

    // 当前激活的组合效果（用于绘制）
    private val _activeEffects = MutableStateFlow<List<CombinationEffect>>(emptyList())
    val activeEffects: StateFlow<List<CombinationEffect>> = _activeEffects.asStateFlow()

    // 组合通知文本
    private val _combinationNotification = MutableStateFlow<String?>(null)
    val combinationNotification: StateFlow<String?> = _combinationNotification.asStateFlow()

    init {
        loadPetData()
        loadMoodHistory()
        loadMemoryTrigger()
        loadHiddenStateData()
        listenForTitleUnlocks()
        loadCombinations()
    }

    private fun loadPetData() {
        viewModelScope.launch {
            petDao.getPetProfile().collect { profile ->
                _petProfile.value = profile
                profile?.let {
                    _currentState.value = try {
                        PetState.valueOf(it.currentState)
                    } catch (e: Exception) {
                        PetState.CALM
                    }
                    _growthStage.value = PetGrowthStage.fromName(it.growthStage)
                    previousStage = _growthStage.value
                    // 发布宠物状态给小岛系统
                    CrossSystemManager.updatePetState(_currentState.value)
                }
            }
        }
        viewModelScope.launch {
            petDao.getPersonality().collect { personality ->
                _personality.value = personality
            }
        }
        // 加载进化提示
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            _evolutionHint.value = PetStateMachine.getEvolutionHint(profile, petDao)
        }
    }

    /**
     * 加载记忆触发文案
     */
    private fun loadMemoryTrigger() {
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            val habitText = memoryRepository.getCurrentHabitText(profile.name)
            if (habitText != null) {
                _memoryTrigger.value = habitText
            }
        }
    }

    /**
     * 加载隐藏状态数据
     */
    private fun loadHiddenStateData() {
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            val discoveredJson = profile.discoveredHiddenStates
            val discoveredSet = try {
                discoveredJson.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }
                    .toSet()
            } catch (e: Exception) {
                emptySet()
            }
            _discoveredHiddenCount.value = discoveredSet.size

            // 加载当前激活的隐藏状态
            val activeStates = petDao.getAllHiddenStates().filter { it.isActive }
            if (activeStates.isNotEmpty()) {
                try {
                    _activeHiddenState.value = PetHiddenStateType.valueOf(activeStates.first().stateType)
                } catch (e: Exception) {
                    _activeHiddenState.value = null
                }
            }
        }
    }

    /**
     * 加载称号组合数据
     */
    private fun loadCombinations() {
        viewModelScope.launch {
            val titleDao = database.titleDao()
            val combinations = TitleManager.detectActiveCombinations(titleDao)
            _activeCombinations.value = combinations
            _activeEffects.value = combinations.map { it.combination.effectType }
        }
    }

    /**
     * 加载心情历史数据（最近7天）
     */
    private fun loadMoodHistory() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000
            val sevenDaysAgo = now - 7 * dayMs

            val records = petDao.getStatesSince(sevenDaysAgo)

            // 按天分组，每天保留最新一条
            val latestPerDay = mutableMapOf<Long, PetStateRecord>()
            for (record in records) {
                val dayKey = record.createdAt / dayMs
                val existing = latestPerDay[dayKey]
                if (existing == null || record.createdAt > existing.createdAt) {
                    latestPerDay[dayKey] = record
                }
            }

            // 构建7天数据
            val dayLabels = arrayOf("日", "一", "二", "三", "四", "五", "六")
            val result = (6 downTo 0).map { i ->
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = now
                    add(java.util.Calendar.DAY_OF_YEAR, -i)
                }
                val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val label = "周${dayLabels[dayOfWeek - 1]}"
                val dayKey = (now - i * dayMs) / dayMs

                val record = latestPerDay[dayKey]
                val state = record?.let {
                    try { PetState.valueOf(it.state) } catch (e: Exception) { null }
                }

                MoodDayData(label, state, i == 0)
            }

            _moodHistory.value = result

            // 构建分布数据
            val distMap = mutableMapOf<PetState, Int>()
            for (record in records) {
                try {
                    val state = PetState.valueOf(record.state)
                    distMap[state] = (distMap[state] ?: 0) + 1
                } catch (e: Exception) { }
            }

            _moodDistribution.value = PetState.values().mapNotNull { state ->
                val count = distMap[state] ?: return@mapNotNull null
                MoodDistributionItem(state, count)
            }.sortedByDescending { it.count }
        }
    }

    /**
     * 监听称号解锁事件 - 称号解锁触发宠物反应
     */
    private fun listenForTitleUnlocks() {
        viewModelScope.launch {
            CrossSystemManager.titleUnlockEvents.collect { event ->
                onTitleUnlocked(event)
            }
        }
    }

    /**
     * 处理称号解锁事件
     */
    private fun onTitleUnlocked(event: TitleUnlockEvent) {
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch

            when (event.tier) {
                3 -> {
                    // 传说级 - 宠物进入 EXCITED 状态
                    petDao.updateState(PetState.EXCITED.name, System.currentTimeMillis())
                    _currentState.value = PetState.EXCITED
                    CrossSystemManager.updatePetState(PetState.EXCITED)
                    _feedbackText.value = "你的称号「${event.titleName}」让${profile.name}好兴奋！"
                }
                2 -> {
                    // 稀有级 - 宠物进入 HAPPY 状态
                    petDao.updateState(PetState.HAPPY.name, System.currentTimeMillis())
                    _currentState.value = PetState.HAPPY
                    CrossSystemManager.updatePetState(PetState.HAPPY)
                    _feedbackText.value = "你的称号「${event.titleName}」让${profile.name}很开心！"
                }
                else -> {
                    // 普通级 - 好感度 +5
                    petDao.addAffection(5)
                    val updatedProfile = petDao.getPetProfileOnce()
                    _petProfile.value = updatedProfile
                    _feedbackText.value = "你的称号「${event.titleName}」让${profile.name}好感度提升了！"
                }
            }

            // 刷新组合状态
            refreshCombinations()
        }
    }

    /**
     * 刷新称号组合状态
     */
    fun refreshCombinations() {
        viewModelScope.launch {
            val titleDao = database.titleDao()
            val oldCombinations = _activeCombinations.value
            val newCombinations = TitleManager.detectActiveCombinations(titleDao)

            // 检查是否有新激活的组合
            val newActivated = newCombinations.filter { new ->
                oldCombinations.none { it.combination.id == new.combination.id }
            }

            _activeCombinations.value = newCombinations
            _activeEffects.value = newCombinations.map { it.combination.effectType }

            // 显示新激活组合的通知
            if (newActivated.isNotEmpty()) {
                val notification = newActivated.joinToString("、") { it.combination.name }
                _combinationNotification.value = "组合效果激活：$notification"
            }
        }
    }

    /**
     * 清除组合通知
     */
    fun clearCombinationNotification() {
        _combinationNotification.value = null
    }

    /**
     * 处理日记保存后的宠物更新
     */
    fun onEntrySaved(
        entryId: Long,
        plainText: String,
        moodLevel: Int?,
        createdAt: Long
    ) {
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            val personality = petDao.getPersonalityOnce() ?: return@launch

            // 1. 确定新状态
            val newState = PetStateMachine.determineState(
                content = plainText,
                plainText = plainText,
                moodLevel = moodLevel,
                createdAt = createdAt,
                streakDays = profile.streakDays,
                lastEntryTime = profile.lastEntryTime
            )

            // 2. 生成反馈
            val style = PetPersonalityAnalyzer.getFeedbackStyle(personality)
            val trigger = determineTrigger(plainText, moodLevel, createdAt, profile)
            val feedback = FeedbackGenerator.generate(trigger, style, profile.name)

            // 3. 更新性格
            val updatedPersonality = PetPersonalityAnalyzer.updatePersonality(personality, plainText)

            // 4. 更新连续记录天数
            val newStreak = calculateStreak(profile.lastEntryTime, createdAt)

            // 5. 保存到数据库
            petDao.updateState(newState.name, createdAt)
            petDao.updateStreak(newStreak, createdAt)
            petDao.addAffection(1)
            petDao.setPersonality(updatedPersonality)
            petDao.insertStateRecord(
                PetStateRecord(
                    entryId = entryId,
                    state = newState.name,
                    trigger = trigger.name,
                    feedbackText = feedback
                )
            )

            // 6. 更新UI
            _currentState.value = newState
            _feedbackText.value = feedback
            _lastTrigger.value = trigger
            _personality.value = updatedPersonality

            // 发布宠物状态给小岛系统
            CrossSystemManager.updatePetState(newState)

            // 刷新心情历史
            loadMoodHistory()

            // 6.5 AI 生成个性化反应（异步，成功则替换模板反馈）
            launch {
                val aiReaction = aiGenerator.generateEntryReaction(
                    diaryContent = plainText,
                    moodLevel = moodLevel ?: 3,
                    petName = profile.name,
                    petPersonality = personality.let {
                        PetPersonalityAnalyzer.getFeedbackStyle(it).name
                    },
                    streakDays = newStreak,
                    entryCount = petDao.getStateCount()
                )
                if (aiReaction != null) {
                    _feedbackText.value = aiReaction
                }
            }

            // 7. 记忆系统处理
            processMemorySystem(entryId, profile, newStreak, createdAt)

            // 8. 检查成长阶段进化
            val updatedProfile = petDao.getPetProfileOnce() ?: return@launch
            val newStage = PetStateMachine.checkGrowthStage(updatedProfile, petDao)
            _growthStage.value = newStage
            _petProfile.value = petDao.getPetProfileOnce()
            _evolutionHint.value = PetStateMachine.getEvolutionHint(
                petDao.getPetProfileOnce() ?: updatedProfile, petDao
            )

            // 如果发生进化，显示特殊反馈
            if (newStage != previousStage) {
                previousStage = newStage
                val stageLabel = PetStateMachine.getGrowthStageLabel(newStage)
                _feedbackText.value = "${updatedProfile.name}进化为${stageLabel}了!"
            }

            // 9. 检测隐藏状态
            checkAndActivateHiddenState(plainText, moodLevel, createdAt, entryId)
        }
    }

    /**
     * 检测并激活隐藏状态
     */
    private suspend fun checkAndActivateHiddenState(
        plainText: String,
        moodLevel: Int?,
        createdAt: Long,
        entryId: Long
    ) {
        // 停用所有隐藏状态
        petDao.deactivateAllHiddenStates()

        // 检测隐藏状态触发
        val result = PetStateMachine.checkHiddenState(
            plainText = plainText,
            moodLevel = moodLevel,
            createdAt = createdAt,
            entryId = entryId,
            petDao = petDao,
            diaryDao = diaryDao
        )

        // 更新已发现的隐藏状态数量
        val profile = petDao.getPetProfileOnce()
        if (profile != null) {
            val discoveredJson = profile.discoveredHiddenStates
            val discoveredSet = try {
                discoveredJson.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }
                    .toMutableSet()
            } catch (e: Exception) {
                mutableSetOf()
            }

            // 添加新发现的隐藏状态
            for (newState in result.newlyDiscovered) {
                discoveredSet.add(newState.name)

                // 保存隐藏状态记录
                petDao.insertHiddenState(
                    com.diary.app.data.PetHiddenState(
                        stateType = newState.name,
                        firstDiscoveredAt = System.currentTimeMillis(),
                        isActive = true,
                        activationCount = 1
                    )
                )
            }

            // 更新 PetProfile 中的已发现隐藏状态
            val updatedJson = "[${discoveredSet.joinToString(",") { "\"$it\"" }}]"
            petDao.updateDiscoveredHiddenStates(updatedJson)
            _discoveredHiddenCount.value = discoveredSet.size
        }

        // 激活触发的隐藏状态
        if (result.triggeredStates.isNotEmpty()) {
            val activeState = result.triggeredStates.first()
            petDao.updateHiddenStateActivation(activeState.name, true)
            _activeHiddenState.value = activeState
        } else {
            _activeHiddenState.value = null
        }
    }

    /**
     * 处理记忆系统
     */
    private fun processMemorySystem(
        entryId: Long,
        profile: PetProfile,
        newStreak: Int,
        createdAt: Long
    ) {
        viewModelScope.launch {
            // 衰减旧记忆
            memoryRepository.decayMemories()

            // 获取总日记数（通过状态记录数量估算）
            val totalEntries = petDao.getStateCount() + 1 // +1 包含当前这篇

            // 检查纪念里程碑
            val milestoneText = memoryRepository.checkMilestones(
                entryCount = totalEntries,
                streakDays = newStreak,
                lastEntryTime = profile.lastEntryTime,
                petName = profile.name
            )

            if (milestoneText != null) {
                _memoryTrigger.value = milestoneText

                // AI 生成个性化里程碑文案（异步替换）
                launch {
                    val aiMilestone = aiGenerator.generateMilestoneMessage(
                        petName = profile.name,
                        milestoneType = "entry_count",
                        milestoneValue = totalEntries
                    )
                    if (aiMilestone != null) {
                        _memoryTrigger.value = aiMilestone
                        _feedbackText.value = aiMilestone
                    }
                }
                return@launch
            }

            // 分析并保存写作习惯
            val habitText = memoryRepository.analyzeAndSaveHabit(
                entryTime = createdAt,
                petName = profile.name
            )

            if (habitText != null) {
                _memoryTrigger.value = habitText
            }
        }
    }

    /**
     * 获取每日问候
     */
    fun getDailyGreeting() {
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            val personality = petDao.getPersonalityOnce() ?: return@launch

            val style = PetPersonalityAnalyzer.getFeedbackStyle(personality)
            val feedback = FeedbackGenerator.generate(
                FeedbackTrigger.DAILY_GREETING,
                style,
                profile.name
            )
            _feedbackText.value = feedback
            _lastTrigger.value = FeedbackTrigger.DAILY_GREETING

            // AI 生成个性化问候（异步替换）
            launch {
                val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val daysSinceLastEntry = if (profile.lastEntryTime > 0) {
                    ((System.currentTimeMillis() - profile.lastEntryTime) / (24 * 60 * 60 * 1000)).toInt()
                } else 999

                val aiGreeting = aiGenerator.generateDailyGreeting(
                    petName = profile.name,
                    hourOfDay = hourOfDay,
                    lastEntryDaysAgo = daysSinceLastEntry,
                    petMood = _currentState.value.name
                )
                if (aiGreeting != null) {
                    _feedbackText.value = aiGreeting
                }
            }
        }
    }

    /**
     * 更新宠物名称
     */
    fun updatePetName(name: String) {
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            petDao.setPetProfile(profile.copy(name = name))
        }
    }

    /**
     * 确定触发类型
     */
    private fun determineTrigger(
        plainText: String,
        moodLevel: Int?,
        createdAt: Long,
        profile: PetProfile
    ): FeedbackTrigger {
        // 检查是否是首次写日记
        if (profile.lastEntryTime == 0L) {
            return FeedbackTrigger.FIRST_ENTRY
        }

        // 检查连续记录天数
        if (profile.streakDays >= 2) {
            return FeedbackTrigger.STREAK_3_DAYS
        }

        // 检查长期未记录
        val daysSinceLastEntry = (createdAt - profile.lastEntryTime) / (24 * 60 * 60 * 1000)
        if (daysSinceLastEntry > 7) {
            return FeedbackTrigger.LONG_ABSENCE
        }

        // 检查深夜记录
        if (SentimentAnalyzer.isLateNight(createdAt)) {
            return FeedbackTrigger.LATE_NIGHT
        }

        // 检查压力内容
        if (SentimentAnalyzer.hasStressContent(plainText)) {
            return FeedbackTrigger.STRESS_CONTENT
        }

        // 检查心情
        when (moodLevel) {
            1, 2 -> return FeedbackTrigger.NEGATIVE_CONTENT
            5, 6 -> return FeedbackTrigger.POSITIVE_CONTENT
        }

        // 检查内容情感
        val sentiment = SentimentAnalyzer.analyze(plainText)
        if (sentiment > 0.3f) return FeedbackTrigger.POSITIVE_CONTENT
        if (sentiment < -0.3f) return FeedbackTrigger.NEGATIVE_CONTENT

        return FeedbackTrigger.DAILY_GREETING
    }

    /**
     * 计算连续记录天数
     */
    private fun calculateStreak(lastEntryTime: Long, currentTime: Long): Int {
        if (lastEntryTime == 0L) return 1

        val daysBetween = (currentTime - lastEntryTime) / (24 * 60 * 60 * 1000)
        return when {
            daysBetween <= 1 -> _petProfile.value?.streakDays?.plus(1) ?: 1
            daysBetween <= 2 -> _petProfile.value?.streakDays?.plus(1) ?: 1
            else -> 1
        }
    }

    // ==================== 触摸交互 ====================

    /**
     * 根据性格获取交互反馈文案
     */
    private fun getInteractionMessage(
        style: FeedbackStyle,
        lively: List<String>,
        curious: List<String>,
        encouraging: List<String>,
        warm: List<String>,
        calm: List<String>
    ): String {
        val messages = when (style) {
            FeedbackStyle.LIVELY -> lively
            FeedbackStyle.CURIOUS -> curious
            FeedbackStyle.ENCOURAGING -> encouraging
            FeedbackStyle.WARM -> warm
            FeedbackStyle.CALM -> calm
        }
        return messages.random()
    }

    /**
     * 点击宠物 - 好感度+1，随机反馈文案
     */
    fun onPetTapped() {
        viewModelScope.launch {
            petDao.addAffection(1)
            val profile = petDao.getPetProfileOnce() ?: return@launch
            _petProfile.value = profile

            val personality = _personality.value
            val style = if (personality != null) PetPersonalityAnalyzer.getFeedbackStyle(personality) else FeedbackStyle.CALM
            _feedbackText.value = getInteractionMessage(
                style,
                lively = listOf("太棒啦！", "好开心！", "嘿嘿~再摸摸~", "耶！"),
                curious = listOf("哇，这是什么？", "让我想想~", "嗯？又来啦？", "哦？"),
                encouraging = listOf("继续加油！", "你做得很棒！", "嗯，很好~", "继续保持~"),
                warm = listOf("有你真好~", "我一直陪着你~", "嗯，我在~", "摸摸~"),
                calm = listOf("嗯，一切都好~", "慢慢来~", "我在~", "嗯。")
            )
        }
        _interactionType.value = InteractionType.TAP
        _interactionCounter.value++
    }

    /**
     * 左右滑动梳毛 - 好感度+2
     */
    fun onPetGroomed() {
        viewModelScope.launch {
            petDao.addAffection(2)
            val profile = petDao.getPetProfileOnce() ?: return@launch
            _petProfile.value = profile

            val personality = _personality.value
            val style = if (personality != null) PetPersonalityAnalyzer.getFeedbackStyle(personality) else FeedbackStyle.CALM
            _feedbackText.value = getInteractionMessage(
                style,
                lively = listOf("好舒服~！", "毛毛亮了！", "超棒的~！", "太爽了~！"),
                curious = listOf("原来梳毛是这种感觉~", "嗯~好神奇~", "毛毛变顺了呢~"),
                encouraging = listOf("梳得很仔细呢~", "谢谢你的照顾~", "你真的很贴心~"),
                warm = listOf("好温暖~", "有你在真好~", "好舒服~谢谢你~"),
                calm = listOf("好舒服~", "嗯，谢谢~", "毛毛顺了~")
            )
        }
        _interactionType.value = InteractionType.GROOM
        _interactionCounter.value++
    }

    /**
     * 从上往下拖拽喂食 - 恢复饱食度
     */
    fun onPetFed() {
        if (_isFull) {
            _feedbackText.value = "已经很饱了~"
            return
        }
        _isFull = true
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            _petProfile.value = profile

            val personality = _personality.value
            val style = if (personality != null) PetPersonalityAnalyzer.getFeedbackStyle(personality) else FeedbackStyle.CALM
            _feedbackText.value = getInteractionMessage(
                style,
                lively = listOf("好吃~！太好吃了~！", "吃饱了~精力满满~！", "谢谢你~！"),
                curious = listOf("这是什么好吃的~？", "嗯~味道不错~", "好新奇~"),
                encouraging = listOf("吃饱了才有力气~", "谢谢你的照顾~", "嗯，吃饱了~"),
                warm = listOf("你对我真好~", "有你真好~", "吃饱了好幸福~"),
                calm = listOf("吃饱了~", "嗯，谢谢~", "好吃~")
            )
        }
        _interactionType.value = InteractionType.FED
        _interactionCounter.value++
    }

    /**
     * 长按撒娇 - 旋转动画
     */
    fun onPetLongPressed() {
        viewModelScope.launch {
            val profile = petDao.getPetProfileOnce() ?: return@launch
            _petProfile.value = profile

            val personality = _personality.value
            val style = if (personality != null) PetPersonalityAnalyzer.getFeedbackStyle(personality) else FeedbackStyle.CALM
            _feedbackText.value = getInteractionMessage(
                style,
                lively = listOf("讨厌~别闹~嘻嘻~", "哎呀~好晕~", "哈哈哈~停下来~"),
                curious = listOf("这是在做什么~？", "好奇怪的感觉~", "嗯？？"),
                encouraging = listOf("别闹啦~", "好了好了~", "嗯，你在撒娇呢~"),
                warm = listOf("讨厌~但很喜欢~", "别闹了啦~", "嘻嘻~"),
                calm = listOf("嗯~别闹~", "好了~", "嗯。")
            )
        }
        _interactionType.value = InteractionType.LONG_PRESS
        _interactionCounter.value++
    }
}
