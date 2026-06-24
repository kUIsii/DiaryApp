package com.diary.app.ui.island

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.AnimalBehavior
import com.diary.app.data.AnimalType
import com.diary.app.data.ComboDefinition
import com.diary.app.data.CrossSystemManager
import com.diary.app.data.IslandAnimal
import com.diary.app.data.IslandDecoration
import com.diary.app.data.IslandDiscovery
import com.diary.app.data.IslandEnvironment
import com.diary.app.data.IslandProfile
import com.diary.app.data.IslandRepository
import com.diary.app.data.IslandTimelineEvent
import com.diary.app.data.PetState
import com.diary.app.data.TimelineEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IslandViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as DiaryApplication).database
    private val islandDao = database.islandDao()
    private val repository = IslandRepository(islandDao)

    // 小岛环境
    private val _environment = MutableStateFlow<IslandEnvironment?>(null)
    val environment: StateFlow<IslandEnvironment?> = _environment.asStateFlow()

    // 小岛配置
    private val _profile = MutableStateFlow<IslandProfile?>(null)
    val profile: StateFlow<IslandProfile?> = _profile.asStateFlow()

    // 已解锁的装饰
    private val _unlockedDecorations = MutableStateFlow<List<IslandDecoration>>(emptyList())
    val unlockedDecorations: StateFlow<List<IslandDecoration>> = _unlockedDecorations.asStateFlow()

    // 所有装饰
    private val _allDecorations = MutableStateFlow<List<IslandDecoration>>(emptyList())
    val allDecorations: StateFlow<List<IslandDecoration>> = _allDecorations.asStateFlow()

    // 最近的环境更新
    private val _recentUpdate = MutableStateFlow<String?>(null)
    val recentUpdate: StateFlow<String?> = _recentUpdate.asStateFlow()

    // 已装备的装饰ID列表
    private val _equippedIds = MutableStateFlow<List<String>>(emptyList())
    val equippedIds: StateFlow<List<String>> = _equippedIds.asStateFlow()

    // 选中的装饰（用于弹窗）
    private val _selectedDecoration = MutableStateFlow<IslandDecoration?>(null)
    val selectedDecoration: StateFlow<IslandDecoration?> = _selectedDecoration.asStateFlow()

    // 上一次的等级（用于检测升级）
    private var previousLevel: Int = 1

    // 升级提示是否显示
    private val _showLevelUp = MutableStateFlow(false)
    val showLevelUp: StateFlow<Boolean> = _showLevelUp.asStateFlow()

    // 升级信息
    private val _levelUpInfo = MutableStateFlow<Pair<Int, Int>?>(null)
    val levelUpInfo: StateFlow<Pair<Int, Int>?> = _levelUpInfo.asStateFlow()

    // ==================== 跨系统联动: 有效环境 ====================

    // 基础环境（数据库值）
    private val _baseEnvironment = MutableStateFlow<IslandEnvironment?>(null)

    // 有效环境（基础 + 宠物状态修正 + 连续记录buff）
    private val _effectiveEnvironment = MutableStateFlow<IslandEnvironment?>(null)
    val effectiveEnvironment: StateFlow<IslandEnvironment?> = _effectiveEnvironment.asStateFlow()

    // 当前激活的buff列表
    private val _activeBuffs = MutableStateFlow<List<String>>(emptyList())
    val activeBuffs: StateFlow<List<String>> = _activeBuffs.asStateFlow()

    // 当前活跃的动物列表
    private val _activeAnimals = MutableStateFlow<List<IslandAnimal>>(emptyList())
    val activeAnimals: StateFlow<List<IslandAnimal>> = _activeAnimals.asStateFlow()

    // 当前时间的小时段 (0-23)
    private val _currentHour = MutableStateFlow(java.time.LocalTime.now().hour)
    val currentHour: StateFlow<Int> = _currentHour.asStateFlow()

    // ==================== 隐藏发现系统 ====================

    // 所有发现记录
    private val _allDiscoveries = MutableStateFlow<List<IslandDiscovery>>(emptyList())
    val allDiscoveries: StateFlow<List<IslandDiscovery>> = _allDiscoveries.asStateFlow()

    // 当前激活的稀有元素
    private val _activeRareElements = MutableStateFlow<List<IslandDiscovery>>(emptyList())
    val activeRareElements: StateFlow<List<IslandDiscovery>> = _activeRareElements.asStateFlow()

    // 最近新发现（用于通知）
    private val _recentDiscovery = MutableStateFlow<IslandDiscovery?>(null)
    val recentDiscovery: StateFlow<IslandDiscovery?> = _recentDiscovery.asStateFlow()

    // ==================== 组合效果系统 ====================

    // 当前激活的组合列表
    private val _activeCombos = MutableStateFlow<List<ComboDefinition>>(emptyList())
    val activeCombos: StateFlow<List<ComboDefinition>> = _activeCombos.asStateFlow()

    // 所有组合定义
    private val _allComboDefinitions = MutableStateFlow<List<ComboDefinition>>(emptyList())
    val allComboDefinitions: StateFlow<List<ComboDefinition>> = _allComboDefinitions.asStateFlow()

    // 最近解锁的组合（用于通知）
    private val _recentlyUnlockedCombo = MutableStateFlow<ComboDefinition?>(null)
    val recentlyUnlockedCombo: StateFlow<ComboDefinition?> = _recentlyUnlockedCombo.asStateFlow()

    // ==================== 历史时间线系统 ====================

    // 所有时间线事件
    private val _timelineEvents = MutableStateFlow<List<IslandTimelineEvent>>(emptyList())
    val timelineEvents: StateFlow<List<IslandTimelineEvent>> = _timelineEvents.asStateFlow()

    // 当前筛选的事件类型（null表示全部）
    private val _timelineFilter = MutableStateFlow<TimelineEventType?>(null)
    val timelineFilter: StateFlow<TimelineEventType?> = _timelineFilter.asStateFlow()

    // 筛选后的时间线事件
    private val _filteredTimelineEvents = MutableStateFlow<List<IslandTimelineEvent>>(emptyList())
    val filteredTimelineEvents: StateFlow<List<IslandTimelineEvent>> = _filteredTimelineEvents.asStateFlow()

    init {
        loadIslandData()
        listenToPetState()
        listenToEquippedDecorations()
        viewModelScope.launch {
            repository.initializeDecorations()
        }
        // 加载组合定义
        _allComboDefinitions.value = repository.getAllComboDefinitions()
        // 加载发现记录
        loadDiscoveries()
        // 加载时间线事件
        loadTimelineEvents()
    }

    private fun loadIslandData() {
        viewModelScope.launch {
            repository.getEnvironment().collect { env ->
                _baseEnvironment.value = env
                _environment.value = env
                recomputeEffectiveEnvironment()
            }
        }
        viewModelScope.launch {
            repository.getProfile().collect { profile ->
                // 检测等级变化
                if (profile != null && previousLevel > 0 && profile.level > previousLevel) {
                    _levelUpInfo.value = Pair(previousLevel, profile.level)
                    _showLevelUp.value = true
                }
                if (profile != null) {
                    previousLevel = profile.level
                    // 发布小岛等级给宠物系统
                    CrossSystemManager.updateIslandLevel(profile.level)

                    // 检查连续记录>=30天解锁荣誉旗杆
                    checkStreakDecoration(profile.streakDays)
                }
                _profile.value = profile
                recomputeEffectiveEnvironment()
            }
        }
        viewModelScope.launch {
            repository.getUnlockedDecorations().collect { decorations ->
                _unlockedDecorations.value = decorations
            }
        }
        viewModelScope.launch {
            repository.getAllDecorations().collect { decorations ->
                _allDecorations.value = decorations
            }
        }
        // 加载已装备的装饰
        viewModelScope.launch {
            _equippedIds.value = repository.getActiveDecorationIds()
        }
    }

    /**
     * 加载发现记录
     */
    private fun loadDiscoveries() {
        viewModelScope.launch {
            repository.getAllDiscoveries().collect { discoveries ->
                _allDiscoveries.value = discoveries
            }
        }
        viewModelScope.launch {
            _activeRareElements.value = repository.getActiveRareElements()
        }
    }

    /**
     * 加载时间线事件
     */
    private fun loadTimelineEvents() {
        viewModelScope.launch {
            repository.getAllTimelineEvents().collect { events ->
                _timelineEvents.value = events
                applyTimelineFilter(_timelineFilter.value)
            }
        }
    }

    /**
     * 设置时间线筛选类型
     */
    fun setTimelineFilter(type: TimelineEventType?) {
        _timelineFilter.value = type
        applyTimelineFilter(type)
    }

    private fun applyTimelineFilter(type: TimelineEventType?) {
        val all = _timelineEvents.value
        _filteredTimelineEvents.value = if (type == null) all
        else all.filter { it.eventType == type.name }
    }

    /**
     * 监听宠物状态变化 - 调整小岛氛围
     */
    private fun listenToPetState() {
        viewModelScope.launch {
            CrossSystemManager.petState.collect {
                recomputeEffectiveEnvironment()
            }
        }
    }

    /**
     * 监听已装备装饰变化 - 重新计算动物
     */
    private fun listenToEquippedDecorations() {
        viewModelScope.launch {
            _equippedIds.collect {
                computeActiveAnimals()
            }
        }
    }

    /**
     * 重新计算有效环境（基础 + 宠物状态修正 + 连续记录buff）
     */
    private fun recomputeEffectiveEnvironment() {
        val base = _baseEnvironment.value ?: return
        val petState = CrossSystemManager.petState.value
        val streak = _profile.value?.streakDays ?: 0

        var brightnessDelta = 0f
        var warmthDelta = 0f
        var tranquilityDelta = 0f
        val buffs = mutableListOf<String>()

        // 宠物状态对氛围的影响
        when (petState) {
            PetState.HAPPY, PetState.EXCITED -> {
                brightnessDelta += 0.1f
                warmthDelta += 0.05f
            }
            PetState.SAD, PetState.WORRIED -> {
                brightnessDelta -= 0.1f
                tranquilityDelta -= 0.05f
            }
            PetState.SLEEPY -> {
                tranquilityDelta += 0.1f
            }
            else -> {}
        }

        // 连续记录天数 buff
        if (streak >= 7) {
            brightnessDelta += 0.15f
            buffs.add("勤奋之光")
        }

        _effectiveEnvironment.value = base.copy(
            brightness = (base.brightness + brightnessDelta).coerceIn(0f, 1f),
            warmth = (base.warmth + warmthDelta).coerceIn(0f, 1f),
            tranquility = (base.tranquility + tranquilityDelta).coerceIn(0f, 1f)
        )
        _activeBuffs.value = buffs
        computeActiveAnimals()
    }

    /**
     * 检查连续记录天数解锁特殊装饰
     */
    private suspend fun checkStreakDecoration(streakDays: Int) {
        if (streakDays >= 30) {
            val unlocked = islandDao.getUnlockedDecorationsOnce()
            if (unlocked.none { it.id == "honor_flagpole" }) {
                islandDao.unlockDecoration("honor_flagpole")
            }
        }
    }

    /**
     * 选中装饰（显示详情弹窗）
     */
    fun selectDecoration(decoration: IslandDecoration?) {
        _selectedDecoration.value = decoration
    }

    /**
     * 切换装饰的装备状态
     */
    fun toggleDecoration(decorationId: String) {
        viewModelScope.launch {
            val newActive = repository.toggleDecoration(decorationId)
            _equippedIds.value = newActive
            // 检查组合效果
            checkCombos(newActive)
        }
    }

    /**
     * 检查当前装备是否触发组合效果
     */
    private suspend fun checkCombos(equippedIds: List<String>) {
        val newlyUnlocked = repository.checkCombos(equippedIds)
        if (newlyUnlocked.isNotEmpty()) {
            // 更新激活的组合列表
            _activeCombos.value = repository.getAllComboDefinitions().filter { combo ->
                equippedIds.containsAll(combo.requiredDecorations)
            }
            // 通知最近解锁的组合（取第一个）
            _recentlyUnlockedCombo.value = newlyUnlocked.first()
        } else {
            // 更新激活的组合列表
            _activeCombos.value = repository.getAllComboDefinitions().filter { combo ->
                equippedIds.containsAll(combo.requiredDecorations)
            }
        }
    }

    /**
     * 关闭组合解锁通知
     */
    fun dismissComboUnlock() {
        _recentlyUnlockedCombo.value = null
    }

    /**
     * 关闭升级提示
     */
    fun dismissLevelUp() {
        _showLevelUp.value = false
        _levelUpInfo.value = null
    }

    /**
     * 处理日记保存后的小岛更新
     */
    fun onEntrySaved(
        entryId: Long,
        plainText: String,
        moodLevel: Int?,
        weather: String?,
        createdAt: Long
    ) {
        viewModelScope.launch {
            val result = repository.onEntrySaved(
                entryId = entryId,
                plainText = plainText,
                moodLevel = moodLevel,
                weather = weather,
                createdAt = createdAt
            )

            // 检测隐藏发现
            val newDiscoveries = repository.checkDiscoveries(
                plainText = plainText,
                moodLevel = moodLevel,
                weather = weather,
                createdAt = createdAt
            )

            // 显示更新消息
            val messages = mutableListOf<String>()
            messages.add("经验值 +${result.experienceGained}")

            if (result.newLevel > (_profile.value?.level ?: 1)) {
                messages.add("升级到 Lv.${result.newLevel}!")
            }

            if (result.newlyUnlockedDecorations.isNotEmpty()) {
                val names = result.newlyUnlockedDecorations.joinToString("、") { it.name }
                messages.add("解锁新装饰: $names")
            }

            // 检查新发现
            if (newDiscoveries.isNotEmpty()) {
                _recentDiscovery.value = newDiscoveries.first()
                _activeRareElements.value = repository.getActiveRareElements()
            }

            if (messages.isNotEmpty()) {
                _recentUpdate.value = messages.joinToString("\n")
            }
        }
    }

    /**
     * 清除最近的更新消息
     */
    fun clearRecentUpdate() {
        _recentUpdate.value = null
    }

    /**
     * 关闭发现通知
     */
    fun dismissDiscovery() {
        _recentDiscovery.value = null
    }

    /**
     * 刷新稀有元素状态
     */
    fun refreshActiveRareElements() {
        viewModelScope.launch {
            _activeRareElements.value = repository.getActiveRareElements()
        }
    }

    /**
     * 计算当前活跃的动物列表
     * 根据装饰拥有情况、环境维度、时间和天气决定哪些动物出现及其行为
     */
    fun computeActiveAnimals() {
        val env = _effectiveEnvironment.value ?: _baseEnvironment.value ?: return
        val equippedIds = _equippedIds.value
        val hour = java.time.LocalTime.now().hour
        _currentHour.value = hour
        val isNight = hour >= 20 || hour < 5
        val isOwlActive = hour in 20..23 || hour in 0..3

        val animals = mutableListOf<IslandAnimal>()

        // 检查可用的建筑和植被
        val hasCabin = equippedIds.contains("cabin")
        val hasLighthouse = equippedIds.contains("lighthouse")
        val hasFountain = equippedIds.contains("fountain")
        val hasFlowers = equippedIds.contains("flowers")
        val hasTree = equippedIds.contains("tree")

        // 天气条件
        val isRaining = env.brightness < 0.3f
        val isWindy = env.tranquility < 0.3f

        // === 小鸟 ===
        if (equippedIds.contains("bird")) {
            val isFlock = env.lushness > 0.7f
            val behavior = when {
                isRaining || isWindy -> AnimalBehavior.HIDING
                isNight && hasLighthouse -> AnimalBehavior.RESTING // 栖息在灯塔
                isNight -> AnimalBehavior.SLEEPING
                isFlock -> AnimalBehavior.FLYING // 鸟群飞翔
                else -> AnimalBehavior.HOPPING // 树间跳跃
            }
            val birdX = if (isNight && hasLighthouse) 0.8f else 0.2f
            val birdY = if (isNight && hasLighthouse) 0.25f else 0.18f
            animals.add(
                IslandAnimal(
                    type = AnimalType.BIRD,
                    behavior = behavior,
                    x = birdX,
                    y = birdY,
                    alpha = if (isNight && !hasLighthouse) 0.4f else 1f
                )
            )
            // 茂盛度>0.7时添加额外的鸟群
            if (isFlock && !isNight && !isRaining && !isWindy) {
                for (i in 1..2) {
                    animals.add(
                        IslandAnimal(
                            type = AnimalType.BIRD,
                            behavior = AnimalBehavior.FLYING,
                            x = birdX + i * 0.08f,
                            y = birdY - i * 0.04f,
                            scale = 0.7f,
                            alpha = 0.7f
                        )
                    )
                }
            }
        }

        // === 蝴蝶 ===
        if (equippedIds.contains("butterfly")) {
            val behavior = when {
                isRaining || isWindy -> AnimalBehavior.HIDING
                isNight -> AnimalBehavior.RESTING // 停在叶子上
                hasFlowers -> AnimalBehavior.FLYING // 在花丛中飞舞
                else -> AnimalBehavior.IDLE
            }
            animals.add(
                IslandAnimal(
                    type = AnimalType.BUTTERFLY,
                    behavior = behavior,
                    x = 0.7f,
                    y = if (isNight) 0.42f else 0.28f,
                    alpha = if (isNight) 0.6f else 1f
                )
            )
        }

        // === 松鼠 ===
        if (equippedIds.contains("squirrel")) {
            val behavior = when {
                isRaining || isWindy -> AnimalBehavior.HIDING
                isNight -> AnimalBehavior.SLEEPING // 树洞中休息
                hasTree -> AnimalBehavior.CLIMBING // 在树上攀爬
                else -> AnimalBehavior.IDLE
            }
            animals.add(
                IslandAnimal(
                    type = AnimalType.SQUIRREL,
                    behavior = behavior,
                    x = if (hasTree) 0.72f else 0.4f,
                    y = if (hasTree) 0.38f else 0.48f,
                    alpha = if (isNight) 0.5f else 1f
                )
            )
        }

        // === 猫头鹰 ===
        if (equippedIds.contains("owl")) {
            val behavior = when {
                isRaining || isWindy -> AnimalBehavior.HIDING
                isOwlActive -> AnimalBehavior.FLYING // 睁眼巡视
                else -> AnimalBehavior.SLEEPING // 白天睡觉
            }
            val owlX = if (hasCabin) 0.52f else 0.8f
            val owlY = if (hasCabin) 0.38f else 0.15f
            animals.add(
                IslandAnimal(
                    type = AnimalType.OWL,
                    behavior = behavior,
                    x = owlX,
                    y = owlY,
                    alpha = if (isOwlActive) 1f else 0.5f
                )
            )
        }

        // === 猫咪 ===
        if (equippedIds.contains("cat")) {
            val hasButterfly = equippedIds.contains("butterfly")
            val behavior = when {
                isRaining || isWindy -> AnimalBehavior.HIDING
                !isNight && hasButterfly -> AnimalBehavior.HUNTING // 追蝴蝶
                isNight && hasCabin -> AnimalBehavior.RESTING // 窗台蜷缩
                isNight -> AnimalBehavior.SLEEPING
                else -> AnimalBehavior.IDLE // 晒太阳
            }
            animals.add(
                IslandAnimal(
                    type = AnimalType.CAT,
                    behavior = behavior,
                    x = if (isNight && hasCabin) 0.53f else 0.45f,
                    y = if (isNight && hasCabin) 0.52f else 0.5f,
                    flipX = !isNight,
                    alpha = if (isNight && !hasCabin) 0.5f else 1f
                )
            )
        }

        // === 青蛙 ===
        if (equippedIds.contains("frog")) {
            val behavior = when {
                isRaining && hasFountain -> AnimalBehavior.CALLING // 雨天+喷泉鸣叫
                isRaining -> AnimalBehavior.CALLING
                isNight && hasFountain -> AnimalBehavior.CALLING
                else -> AnimalBehavior.HIDING // 躲在草丛
            }
            animals.add(
                IslandAnimal(
                    type = AnimalType.FROG,
                    behavior = behavior,
                    x = if (hasFountain) 0.62f else 0.35f,
                    y = 0.55f,
                    alpha = if (behavior == AnimalBehavior.HIDING) 0.4f else 1f
                )
            )
        }

        // === 萤火虫群 ===
        if (equippedIds.contains("fireflies")) {
            if (isNight && env.lushness > 0.7f) {
                for (i in 0..5) {
                    animals.add(
                        IslandAnimal(
                            type = AnimalType.FIREFLY,
                            behavior = AnimalBehavior.GLOWING,
                            x = 0.15f + i * 0.12f,
                            y = 0.35f + (i % 3) * 0.1f,
                            scale = 0.8f + (i % 3) * 0.2f
                        )
                    )
                }
            }
        }

        _activeAnimals.value = animals
    }
}
