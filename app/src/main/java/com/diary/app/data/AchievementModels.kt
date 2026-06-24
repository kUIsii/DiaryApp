package com.diary.app.data

/**
 * Unified achievement system data models.
 * Merges the old Achievement (progress tracking) and TitleDefinition (categories/tiers/flavor text)
 * systems into a single unified achievement system with 8 categories and 4 rarity tiers.
 */

// ── Category definitions ─────────────────────────────────────

enum class AchievementCategory(
    val displayName: String,
    val description: String,
    val iconEmoji: String
) {
    TIME("时间旅人", "记录时间的痕迹", "\uD83D\uDCC5"),
    MOOD("情绪画师", "捕捉情感的色彩", "\uD83C\uDFA8"),
    WEATHER("风雨行者", "穿越天气的旅程", "\u26C5"),
    WRITING("文字匠人", "锤炼文字的技艺", "\u270D\uFE0F"),
    HABIT("习惯先锋", "坚持的力量", "\uD83D\uDD25"),
    EXPLORER("探险家", "发现隐藏的世界", "\uD83E\uDDED"),
    COLLECTOR("收藏家", "珍藏记忆的宝库", "\uD83D\uDCDC"),
    LEGENDARY("传说徽章", "最高荣耀", "\uD83C\uDFC6")
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
    val isHidden: Boolean = false
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
