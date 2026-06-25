package com.diary.app.data

/**
 * Unified achievement system data models.
 * Consolidates progress tracking and presentation metadata into a single
 * achievement system with 8 categories and 4 rarity tiers.
 */

// ── Category definitions ─────────────────────────────────────

enum class AchievementCategory(
    val displayName: String,
    val description: String,
    val iconEmoji: String
) {
    TIME("时间旅人", "收录一天当中的时间痕迹", ""),
    MOOD("情绪画师", "保存情绪流动留下的色阶", ""),
    WEATHER("风雨行者", "记录天气与心境交会的片段", ""),
    WRITING("文字匠人", "整理书写本身留下的手感与厚度", ""),
    HABIT("习惯先锋", "归档持续出现的日常节律", ""),
    EXPLORER("探险家", "发现少见但真实发生过的时刻", ""),
    COLLECTOR("收藏家", "把值得回看的内容收进个人目录", ""),
    LEGENDARY("传说藏品", "那些足以代表长期生活轨迹的珍藏", "")
}

// ── Tier definitions ─────────────────────────────────────────

enum class AchievementTier(
    val displayName: String,
    val stars: String,
    val tierInt: Int
) {
    COMMON("普通", "\u2605\u2606\u2606\u2606", 1),
    RARE("稀有", "\u2605\u2605\u2606\u2606", 2),
    EPIC("史诗", "\u2605\u2605\u2605\u2606", 3),
    LEGENDARY("传说", "\u2605\u2605\u2605\u2605", 4);

    companion object {
        fun fromInt(value: Int): AchievementTier = when (value) {
            1 -> COMMON
            2 -> RARE
            3 -> EPIC
            4 -> LEGENDARY
            else -> COMMON
        }
    }
}

// ── Achievement definition (in-memory, loaded from seed data) ──

data class AchievementDef(
    val key: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val iconEmoji: String,
    val flavorText: String,
    val target: Int = 1,
    val isHidden: Boolean = false,
    val imageRes: Int? = null
)

// ── Runtime achievement state (from database) ──

data class AchievementState(
    val key: String,
    val progress: Int,
    val unlocked: Boolean,
    val unlockedAt: Long?,
    val relatedEntryId: Long?
)

// ── Combined view model for UI ──

data class AchievementItem(
    val def: AchievementDef,
    val state: AchievementState
) {
    val isUnlocked: Boolean get() = state.unlocked
    val progressFraction: Float
        get() = if (def.target > 0) {
            (state.progress.toFloat() / def.target).coerceIn(0f, 1f)
        } else 0f
    val isHiddenLocked: Boolean get() = def.isHidden && !isUnlocked
}

// ── Achievement stats ──

data class AchievementStats(
    val unlockedCount: Int,
    val totalCount: Int,
    val categoryCounts: Map<AchievementCategory, Pair<Int, Int>> // unlocked, total
)
