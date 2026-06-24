package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 心情小岛数据模型
 */

/**
 * 隐藏发现类型枚举
 */
enum class DiscoveryType {
    RARE_ELEMENT,   // 稀有元素
    SEASONAL,       // 季节性场景
    EGG,            // 彩蛋
    ACHIEVEMENT     // 成就
}

/**
 * 稀有元素枚举
 */
enum class RareElement(
    val id: String,
    val displayName: String,
    val message: String,
    val durationMinutes: Int  // -1 表示永久
) {
    WEREWOLF(
        id = "werewolf",
        displayName = "月圆之夜",
        message = "你发现了月圆之夜的秘密...",
        durationMinutes = 10
    ),
    RAINBOW_BRIDGE(
        id = "rainbow_bridge",
        displayName = "彩虹桥",
        message = "连续的好心情，带来了小岛的彩虹",
        durationMinutes = 30
    ),
    ELF_LIGHT(
        id = "elf_light",
        displayName = "精灵之光",
        message = "100篇日记的力量，唤醒了沉睡的精灵",
        durationMinutes = 20
    ),
    MEMORY_TREE(
        id = "memory_tree",
        displayName = "记忆树",
        message = "一年的陪伴，小岛长出了记忆之树",
        durationMinutes = -1  // 永久
    ),
    EASTER_EGG(
        id = "easter_egg",
        displayName = "思念彩蛋",
        message = "你发现了隐藏的思念彩蛋",
        durationMinutes = 1440  // 1天
    ),
    FIREWORKS(
        id = "fireworks",
        displayName = "新年烟花",
        message = "新年快乐！小岛为你绽放烟花",
        durationMinutes = 5
    ),
    AURORA(
        id = "aurora",
        displayName = "极光",
        message = "宁静的夜晚，极光降临小岛",
        durationMinutes = 30
    )
}

/**
 * 小岛环境维度
 */
@Entity(tableName = "island_environment")
data class IslandEnvironment(
    @PrimaryKey val id: Long = 1,

    // 茂盛度（植被覆盖）0.0~1.0
    @ColumnInfo(name = "lushness")
    val lushness: Float = 0.3f,

    // 明亮度（光照氛围）0.0~1.0
    @ColumnInfo(name = "brightness")
    val brightness: Float = 0.5f,

    // 宁静度（水面/风）0.0~1.0
    @ColumnInfo(name = "tranquility")
    val tranquility: Float = 0.5f,

    // 温暖度（色调温度）0.0~1.0
    @ColumnInfo(name = "warmth")
    val warmth: Float = 0.5f,

    // 更新时间
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 小岛配置（单例）
 */
@Entity(tableName = "island_profile")
data class IslandProfile(
    @PrimaryKey val id: Long = 1,

    // 小岛等级
    @ColumnInfo(name = "level")
    val level: Int = 1,

    // 经验值
    @ColumnInfo(name = "experience")
    val experience: Int = 0,

    // 总日记数
    @ColumnInfo(name = "total_entries")
    val totalEntries: Int = 0,

    // 连续记录天数
    @ColumnInfo(name = "streak_days")
    val streakDays: Int = 0,

    // 上次记录时间
    @ColumnInfo(name = "last_entry_time")
    val lastEntryTime: Long = 0,

    // 解锁的装饰列表（JSON格式）
    @ColumnInfo(name = "unlocked_decorations")
    val unlockedDecorations: String = "[]",

    // 当前展示的装饰列表
    @ColumnInfo(name = "active_decorations")
    val activeDecorations: String = "[]"
)

/**
 * 小岛装饰定义
 */
@Entity(tableName = "island_decorations")
data class IslandDecoration(
    @PrimaryKey
    val id: String,

    // 装饰名称
    @ColumnInfo(name = "name")
    val name: String,

    // 解锁等级
    @ColumnInfo(name = "unlock_level")
    val unlockLevel: Int,

    // 解锁条件描述
    @ColumnInfo(name = "unlock_condition")
    val unlockCondition: String,

    // 装饰类型
    @ColumnInfo(name = "type")
    val type: String,  // building, animal, effect, vegetation

    // 图层
    @ColumnInfo(name = "layer")
    val layer: Int,  // 0-6, 对应天空到特效

    // 相对位置 X (0.0~1.0)
    @ColumnInfo(name = "pos_x")
    val posX: Float = 0.5f,

    // 相对位置 Y (0.0~1.0)
    @ColumnInfo(name = "pos_y")
    val posY: Float = 0.5f,

    // 是否已解锁
    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false
)

/**
 * 小岛环境更新记录
 */
@Entity(
    tableName = "island_updates",
    indices = [androidx.room.Index(value = ["entry_id"])]
)
data class IslandUpdate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 关联的日记ID
    @ColumnInfo(name = "entry_id")
    val entryId: Long,

    // 环境变化
    @ColumnInfo(name = "lushness_delta")
    val lushnessDelta: Float = 0f,

    @ColumnInfo(name = "brightness_delta")
    val brightnessDelta: Float = 0f,

    @ColumnInfo(name = "tranquility_delta")
    val tranquilityDelta: Float = 0f,

    @ColumnInfo(name = "warmth_delta")
    val warmthDelta: Float = 0f,

    // 获得的经验值
    @ColumnInfo(name = "experience_gained")
    val experienceGained: Int = 0,

    // 创建时间
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 隐藏发现记录
 */
@Entity(tableName = "island_discoveries")
data class IslandDiscovery(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 发现类型
    @ColumnInfo(name = "discovery_type")
    val discoveryType: String,  // "rare_element" | "seasonal" | "egg" | "achievement"

    // 唯一标识
    @ColumnInfo(name = "discovery_key")
    val discoveryKey: String,

    // 发现时间
    @ColumnInfo(name = "discovered_at")
    val discoveredAt: Long = System.currentTimeMillis(),

    // 发现时的文案
    @ColumnInfo(name = "message")
    val message: String,

    // 过期时间（-1表示永久）
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long = -1L,

    // 元数据（JSON格式）
    @ColumnInfo(name = "metadata")
    val metadata: String? = null
)

/**
 * 季节性场景枚举
 */
enum class SeasonalScene(
    val id: String,
    val displayName: String,
    val message: String,
    val season: Int  // 1=春, 2=夏, 3=秋, 4=冬
) {
    CHERRY_BLOSSOM("cherry_blossom", "樱花飘落", "春风带来了粉色的花瓣", 1),
    QINGMING_RAIN("qingming_rain", "清明雨", "细雨如丝，思念绵绵", 1),
    FIREFLY_SEA("firefly_sea", "萤火虫海", "夏夜的草丛中，萤火虫翩翩起舞", 2),
    THUNDER_RAIN("thunder_rain", "雷阵雨", "午后的雷雨，洗涤了小岛", 2),
    RED_LEAVES("red_leaves", "红叶雨", "秋天的红叶，像燃烧的记忆", 3),
    HARVEST_FRUITS("harvest_fruits", "丰收果实", "果实累累，小岛迎来了收获", 3),
    FIRST_SNOW("first_snow", "初雪", "冬天的第一片雪花，轻轻落下", 4),
    CHRISTMAS_LIGHTS("christmas_lights", "圣诞彩灯", "小木屋亮起了温暖的彩灯", 4),
    SPRING_FESTIVAL("spring_festival", "春节灯笼", "红灯笼高高挂，新年到了", 4)
}

/**
 * 动物类型
 */
enum class AnimalType {
    BIRD,       // 小鸟
    BUTTERFLY,  // 蝴蝶
    SQUIRREL,   // 松鼠
    OWL,        // 猫头鹰
    CAT,        // 猫咪
    FROG,       // 青蛙
    FIREFLY     // 萤火虫群
}

/**
 * 动物行为状态
 */
enum class AnimalBehavior {
    IDLE,       // 静止
    FLYING,     // 飞翔
    HOPPING,    // 跳跃
    CLIMBING,   // 攀爬
    SLEEPING,   // 睡觉
    HIDING,     // 躲避（雨天/大风）
    HUNTING,    // 追逐（猫追蝴蝶）
    RESTING,    // 休息
    CALLING,    // 鸣叫（青蛙）
    GLOWING     // 发光飞舞（萤火虫）
}

/**
 * 运行时动物实例
 */
data class IslandAnimal(
    val type: AnimalType,
    val behavior: AnimalBehavior,
    val x: Float,
    val y: Float,
    val flipX: Boolean = false,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val extraData: String = ""
)

/**
 * 装饰组合定义
 */
data class ComboDefinition(
    val id: String,
    val name: String,
    val requiredDecorations: List<String>,
    val effectDescription: String,
    val unlockMessage: String
)

/**
 * 装饰组合解锁记录
 */
@Entity(tableName = "island_combos")
data class IslandCombo(
    @PrimaryKey
    val id: String,
    val comboId: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)

/**
 * 小岛历史时间线事件类型
 */
enum class TimelineEventType {
    FIRST_ENTRY,       // 首次记录
    LEVEL_UP,          // 等级提升
    DECORATION_UNLOCK, // 装饰解锁
    ANIMAL_ARRIVE,     // 动物到来
    COMBO_ACTIVATE,    // 组合激活
    RARE_DISCOVERY,    // 隐藏发现
    SEASON_CHANGE      // 季节变化
}

/**
 * 小岛历史时间线事件
 */
@Entity(
    tableName = "island_timeline_events",
    indices = [androidx.room.Index(value = ["event_time"])]
)
data class IslandTimelineEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 事件类型
    @ColumnInfo(name = "event_type")
    val eventType: String,  // TimelineEventType.name

    // 事件消息
    @ColumnInfo(name = "message")
    val message: String,

    // 事件时间
    @ColumnInfo(name = "event_time")
    val eventTime: Long = System.currentTimeMillis(),

    // 关联的日记ID（可选）
    @ColumnInfo(name = "related_entry_id")
    val relatedEntryId: Long? = null,

    // 元数据（JSON格式，可选）
    @ColumnInfo(name = "metadata")
    val metadata: String? = null
)

/**
 * 装饰组合视觉增强类型
 */
enum class ComboVisualEffect {
    WARM_CABIN,      // 温馨家园: 小木屋周围花丛 + 暖黄灯光
    LIT_LAMP,        // 守望灯塔: 灯塔光芒照亮桥面
    EXTRA_ANIMALS,   // 生态乐园: 额外蝴蝶和小鸟
    FALLING_LEAVES,  // 风之谷: 风车带动树叶飘落
    WATER_REFLECTION // 静谧水岸: 水面倒影
}
