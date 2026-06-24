package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 宠物成长阶段
 */
enum class PetGrowthStage(val displayName: String) {
    JUVENILE("幼年期"),
    GROWING("成长期"),
    MATURE("成熟期");

    companion object {
        fun fromName(name: String): PetGrowthStage = try {
            valueOf(name)
        } catch (e: Exception) {
            JUVENILE
        }
    }
}

/**
 * 宠物状态枚举
 */
enum class PetState(val displayName: String) {
    CALM("平静"),
    HAPPY("开心"),
    SLEEPY("困倦"),
    WORRIED("担心"),
    SAD("难过"),
    EXCITED("兴奋"),
    CURIOUS("好奇"),
    TIRED("疲惫")
}

/**
 * 宠物性格维度（简化大五人格）
 */
@Entity(tableName = "pet_personality")
data class PetPersonality(
    @PrimaryKey val id: Long = 1,

    // 外向性：社交、聚会、朋友相关
    @ColumnInfo(name = "extraversion")
    val extraversion: Float = 0.5f,

    // 开放性：学习、尝试、新体验相关
    @ColumnInfo(name = "openness")
    val openness: Float = 0.5f,

    // 尽责性：计划、完成、目标相关
    @ColumnInfo(name = "conscientiousness")
    val conscientiousness: Float = 0.5f,

    // 宜人性：帮助、感谢、包容相关
    @ColumnInfo(name = "agreeableness")
    val agreeableness: Float = 0.5f,

    // 情绪稳定性：焦虑词汇 vs 平静词汇
    @ColumnInfo(name = "emotional_stability")
    val emotionalStability: Float = 0.5f,

    // 更新时间
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 宠物状态记录
 */
@Entity(
    tableName = "pet_states",
    indices = [androidx.room.Index(value = ["entry_id"])]
)
data class PetStateRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 关联的日记ID
    @ColumnInfo(name = "entry_id")
    val entryId: Long,

    // 宠物状态
    @ColumnInfo(name = "state")
    val state: String,

    // 触发原因
    @ColumnInfo(name = "trigger")
    val trigger: String,

    // 反馈文案
    @ColumnInfo(name = "feedback_text")
    val feedbackText: String,

    // 创建时间
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 宠物配置（单例）
 */
@Entity(tableName = "pet_profile")
data class PetProfile(
    @PrimaryKey val id: Long = 1,

    // 宠物名称（用户可自定义）
    @ColumnInfo(name = "name")
    val name: String = "小记",

    // 当前状态
    @ColumnInfo(name = "current_state")
    val currentState: String = PetState.CALM.name,

    // 好感度（基于互动频率）
    @ColumnInfo(name = "affection")
    val affection: Int = 0,

    // 最后互动时间
    @ColumnInfo(name = "last_interaction")
    val lastInteraction: Long = System.currentTimeMillis(),

    // 上次记录时间
    @ColumnInfo(name = "last_entry_time")
    val lastEntryTime: Long = 0,

    // 连续记录天数
    @ColumnInfo(name = "streak_days")
    val streakDays: Int = 0,

    // 成长阶段
    @ColumnInfo(name = "growth_stage")
    val growthStage: String = PetGrowthStage.JUVENILE.name,

    // 进化时间
    @ColumnInfo(name = "evolved_at")
    val evolvedAt: Long? = null,

    // 已发现的隐藏状态（JSON 数组）
    @ColumnInfo(name = "discovered_hidden_states")
    val discoveredHiddenStates: String = "[]"
)

/**
 * 反馈触发类型
 */
enum class FeedbackTrigger {
    FIRST_ENTRY,         // 首次写日记
    STREAK_3_DAYS,       // 连续记录3天
    POSITIVE_CONTENT,    // 积极内容
    NEGATIVE_CONTENT,    // 消极内容
    LATE_NIGHT,          // 深夜记录
    LONG_ABSENCE,        // 长期未记录
    STRESS_CONTENT,      // 压力内容
    GOAL_ACHIEVED,       // 目标达成
    NEW_TOPIC,           // 新话题
    DAILY_GREETING       // 每日问候
}

/**
 * 宠物记忆类型
 */
enum class PetMemoryType(val displayName: String) {
    HABIT("习惯记忆"),
    EMOTION("情绪记忆"),
    MILESTONE("纪念记忆"),
    TOPIC("主题记忆"),
    GAP("断档记忆")
}

/**
 * 习惯类型枚举
 */
enum class HabitType(val displayName: String, val description: String) {
    NIGHT_OWL("夜猫子", "大部分写作在22:00-02:00"),
    EARLY_BIRD("早起鸟", "大部分写作在05:00-08:00"),
    AFTERNOON("午后党", "大部分写作在12:00-15:00"),
    SCATTERED("碎片型", "写作时间分散，无明显集中时段"),
    REGULAR("规律型", "每天固定时间写作")
}

/**
 * 宠物记忆
 */
@Entity(
    tableName = "pet_memory",
    indices = [Index(value = ["type"])]
)
data class PetMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 记忆类型
    @ColumnInfo(name = "type")
    val type: String,

    // 记忆内容（JSON）
    @ColumnInfo(name = "content")
    val content: String,

    // 记忆强度（0.0-1.0）
    @ColumnInfo(name = "strength")
    val strength: Float = 0.5f,

    // 关联的日记ID（可选）
    @ColumnInfo(name = "related_entry_id")
    val relatedEntryId: Long? = null,

    // 触发文案
    @ColumnInfo(name = "trigger_text")
    val triggerText: String = "",

    // 创建时间
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // 最近激活时间
    @ColumnInfo(name = "last_activated_at")
    val lastActivatedAt: Long = System.currentTimeMillis()
)

/**
 * 宠物隐藏状态类型
 */
enum class PetHiddenStateType(val displayName: String) {
    NIGHT_OWL("夜猫子"),
    TREASURE_HUNTER("宝藏猎人"),
    WARM_GUARDIAN("暖心守护者"),
    DEEP_DIVER("深海潜水员"),
    TIME_TRAVELER("时间旅人")
}

/**
 * 宠物隐藏状态记录
 */
@Entity(
    tableName = "pet_hidden_states",
    indices = [Index(value = ["state_type"])]
)
data class PetHiddenState(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 隐藏状态类型
    @ColumnInfo(name = "state_type")
    val stateType: String,

    // 首次发现时间
    @ColumnInfo(name = "first_discovered_at")
    val firstDiscoveredAt: Long = System.currentTimeMillis(),

    // 是否当前激活
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    // 激活次数
    @ColumnInfo(name = "activation_count")
    val activationCount: Int = 0
)
