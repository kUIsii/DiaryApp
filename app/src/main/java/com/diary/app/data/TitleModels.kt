package com.diary.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 称号定义表 — 预定义的35+个称号
 */
@Entity(
    tableName = "title_definitions",
    indices = [Index(value = ["category"])]
)
data class TitleDefinition(
    @PrimaryKey val key: String,
    val name: String,
    val description: String,
    val category: String,           // time/mood/weather/writing/habit/hidden
    val iconName: String,           // Material Icon 名称
    val tier: Int = 1,              // 1=普通, 2=稀有, 3=传说
    val isHidden: Boolean = false,  // 是否隐藏称号
    val flavorText: String = ""     // 趣味描述
)

/**
 * 用户获得的称号表
 */
@Entity(
    tableName = "user_titles",
    indices = [Index(value = ["titleKey"], unique = true)]
)
data class UserTitle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleKey: String,
    val unlockedAt: Long,
    val relatedEntryId: Long? = null
)

/**
 * 称号展示配置表 — 单用户，固定ID=1
 */
@Entity(tableName = "title_profile")
data class TitleProfile(
    @PrimaryKey val id: Long = 1,
    val activeTitleKey: String? = null,
    val showTitleOnHome: Boolean = false,
    val showTitleOnEntry: Boolean = false
)

/**
 * 称号组合定义 - 同时装备多个特定称号时触发的特殊效果
 */
data class TitleCombination(
    val id: String,                    // 组合ID
    val name: String,                  // 组合名称
    val description: String,           // 组合描述
    val requiredTitles: List<String>,  // 需要的称号key列表
    val effectType: CombinationEffect  // 效果类型
)

/**
 * 组合效果类型
 */
enum class CombinationEffect {
    WISDOM_AURA,      // 智慧光环 - 头顶出现小灯泡
    WARM_GLOW,        // 温暖光环 - 暖黄色光晕
    ADVENTURE_BADGE,  // 冒险徽章 - 身体周围出现小星星
    PERSISTENCE_AURA  // 坚持光环 - 金色光环
}

/**
 * 当前激活的组合状态
 */
data class ActiveCombination(
    val combination: TitleCombination,
    val activatedAt: Long
)
