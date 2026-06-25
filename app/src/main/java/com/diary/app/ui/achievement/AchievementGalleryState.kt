package com.diary.app.ui.achievement

import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementStats
import com.diary.app.data.AchievementTier

enum class AchievementGalleryFilter(val label: String) {
    ALL("全部"),
    UNLOCKED("已收录"),
    NEAR_COMPLETION("接近完成"),
    HIDDEN("隐藏线索")
}

data class AchievementHeroSummary(
    val headline: String,
    val supportingLine: String,
    val completionFraction: Float,
    val unlockedLegendaryCount: Int
)

data class AchievementGalleryCardState(
    val item: AchievementItem,
    val title: String,
    val description: String,
    val isConcealed: Boolean,
    val statusLabel: String
)

data class AchievementGalleryState(
    val hero: AchievementHeroSummary,
    val recentUnlocks: List<AchievementItem>,
    val nearCompletion: List<AchievementItem>,
    val filteredCards: List<AchievementGalleryCardState>
)

fun buildAchievementGalleryState(
    items: List<AchievementItem>,
    stats: AchievementStats,
    selectedCategory: AchievementCategory?,
    selectedTier: AchievementTier?,
    stateFilter: AchievementGalleryFilter
): AchievementGalleryState {
    return AchievementGalleryState(
        hero = buildAchievementHeroSummary(stats = stats, items = items),
        recentUnlocks = recentUnlocks(items = items),
        nearCompletion = nearCompletionItems(items = items),
        filteredCards = filterAchievementGalleryCards(
            items = items,
            selectedCategory = selectedCategory,
            selectedTier = selectedTier,
            stateFilter = stateFilter
        )
    )
}

fun buildAchievementHeroSummary(
    stats: AchievementStats,
    items: List<AchievementItem>
): AchievementHeroSummary {
    val recent = recentUnlocks(items, limit = 1).firstOrNull()
    val nextNear = nearCompletionItems(items, limit = 1, minimumProgressFraction = 0.5f).firstOrNull()
    val unlockedLegendaryCount = items.count { it.isUnlocked && it.def.tier == AchievementTier.LEGENDARY }
    val completionFraction = if (stats.totalCount > 0) {
        stats.unlockedCount.toFloat() / stats.totalCount.toFloat()
    } else {
        0f
    }

    val headline = when {
        recent != null -> "「${recent.def.name}」刚被收进你的生活收藏册"
        stats.unlockedCount == 0 -> "第一段生活痕迹，正等着被认真收好"
        unlockedLegendaryCount > 0 || completionFraction >= 0.7f ->
            "你的收藏册已经积累出清晰、稳定的生活年轮"
        else -> "零散的日常，正在慢慢整理成可回望的收藏"
    }

    val supportingLine = when {
        nextNear != null -> "离「${nextNear.def.name}」已经不远了，再写几笔就会留下新的页签。"
        stats.totalCount > 0 -> "目前已收录 ${stats.unlockedCount} / ${stats.totalCount} 项生活痕迹。"
        else -> "这里会在你继续记录以后，慢慢出现属于自己的目录。"
    }

    return AchievementHeroSummary(
        headline = headline,
        supportingLine = supportingLine,
        completionFraction = completionFraction.coerceIn(0f, 1f),
        unlockedLegendaryCount = unlockedLegendaryCount
    )
}

fun recentUnlocks(
    items: List<AchievementItem>,
    limit: Int = 5
): List<AchievementItem> {
    return items
        .asSequence()
        .filter { it.isUnlocked }
        .sortedWith(
            compareByDescending<AchievementItem> { it.state.unlockedAt ?: Long.MIN_VALUE }
                .thenByDescending { it.def.tier.tierInt }
        )
        .take(limit)
        .toList()
}

fun nearCompletionItems(
    items: List<AchievementItem>,
    limit: Int = 4,
    minimumProgressFraction: Float = 0.6f
): List<AchievementItem> {
    return items
        .asSequence()
        .filterNot { it.isUnlocked }
        .filterNot { it.isHiddenLocked }
        .filter { it.def.target > 0 }
        .filter { it.progressFraction >= minimumProgressFraction }
        .sortedWith(
            compareByDescending<AchievementItem> { it.progressFraction }
                .thenByDescending { it.def.tier.tierInt }
                .thenBy { it.def.target - it.state.progress }
        )
        .take(limit)
        .toList()
}

fun filterAchievementGalleryCards(
    items: List<AchievementItem>,
    selectedCategory: AchievementCategory? = null,
    selectedTier: AchievementTier? = null,
    stateFilter: AchievementGalleryFilter = AchievementGalleryFilter.ALL
): List<AchievementGalleryCardState> {
    val baseItems = items.filter { item ->
        val categoryMatches = selectedCategory == null || item.def.category == selectedCategory
        val tierMatches = selectedTier == null || item.def.tier == selectedTier
        categoryMatches && tierMatches
    }

    val filteredItems = when (stateFilter) {
        AchievementGalleryFilter.ALL ->
            baseItems.filterNot { it.isHiddenLocked }
                .sortedWith(defaultGallerySort())
        AchievementGalleryFilter.UNLOCKED ->
            baseItems.filter { it.isUnlocked }
                .sortedWith(
                    compareByDescending<AchievementItem> { it.state.unlockedAt ?: Long.MIN_VALUE }
                        .thenByDescending { it.def.tier.tierInt }
                )
        AchievementGalleryFilter.NEAR_COMPLETION ->
            nearCompletionItems(baseItems, limit = baseItems.size, minimumProgressFraction = 0.6f)
        AchievementGalleryFilter.HIDDEN ->
            baseItems.filter { it.def.isHidden }
                .sortedWith(defaultGallerySort())
    }

    return filteredItems.map(::toGalleryCardState)
}

private fun toGalleryCardState(item: AchievementItem): AchievementGalleryCardState {
    if (item.isHiddenLocked) {
        return AchievementGalleryCardState(
            item = item,
            title = "未公开藏品",
            description = "完成特定条件后，这条生活线索会显露完整内容。",
            isConcealed = true,
            statusLabel = buildAchievementStatusLabel(item)
        )
    }

    return AchievementGalleryCardState(
        item = item,
        title = item.def.name,
        description = item.def.description,
        isConcealed = false,
        statusLabel = buildAchievementStatusLabel(item)
    )
}

fun buildAchievementStatusLabel(item: AchievementItem): String {
    return when {
        item.isUnlocked -> "已达成"
        item.isHiddenLocked -> "隐藏线索"
        else -> "${(item.progressFraction * 100).toInt()}%"
    }
}

private fun defaultGallerySort(): Comparator<AchievementItem> {
    return compareByDescending<AchievementItem> { it.isUnlocked }
        .thenByDescending { it.progressFraction }
        .thenByDescending { it.def.tier.tierInt }
}
