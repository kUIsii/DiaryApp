package com.diary.app.ui.nurturing

import com.diary.app.data.AchievementTier
import com.diary.app.data.PetState

enum class PetArtKey {
    CALM,
    WORRIED,
    CELEBRATION
}

data class PetSceneVisualState(
    val artKey: PetArtKey,
    val sceneLabel: String,
    val companionHint: String
)

fun buildPetSceneVisualState(
    petState: PetState,
    recentAchievementUnlock: String?,
    islandLevel: Int
): PetSceneVisualState {
    val unlock = recentAchievementUnlock?.takeIf { it.isNotBlank() }
    if (unlock != null && petState in listOf(PetState.HAPPY, PetState.EXCITED, PetState.CURIOUS)) {
        return PetSceneVisualState(
            artKey = PetArtKey.CELEBRATION,
            sceneLabel = "刚刚替你庆祝过新的珍藏",
            companionHint = "它还在为「$unlock」兴奋，尾端的微光比平时更亮。"
        )
    }

    return when (petState) {
        PetState.WORRIED, PetState.SAD, PetState.TIRED -> PetSceneVisualState(
            artKey = PetArtKey.WORRIED,
            sceneLabel = "今晚它更想靠近一点",
            companionHint = "先轻轻碰一碰，再带它看看小岛，气氛会慢慢缓下来。"
        )

        else -> PetSceneVisualState(
            artKey = PetArtKey.CALM,
            sceneLabel = if (islandLevel >= 10) "它已经习惯在这片夜色里等你" else "它正在慢慢熟悉你的归来时间",
            companionHint = "先和它打个招呼，再去岛上转一圈，会更容易触发新的回应。"
        )
    }
}

enum class IslandArtKey {
    TREEHOUSE,
    SECRET_GLOW
}

data class IslandVisualState(
    val artKey: IslandArtKey,
    val headline: String,
    val guidance: String
)

fun buildIslandVisualState(
    islandLevel: Int,
    petState: PetState,
    hasRareDiscovery: Boolean,
    activeBuffCount: Int,
    activeAnimalsCount: Int,
    recentAchievementUnlock: String?
): IslandVisualState {
    val recentUnlock = recentAchievementUnlock?.takeIf { it.isNotBlank() }
    if (hasRareDiscovery) {
        return IslandVisualState(
            artKey = IslandArtKey.SECRET_GLOW,
            headline = "稀有现象正在把夜色往更深处牵引",
            guidance = buildString {
                append("先去看发现档案，")
                append("今夜已经有 $activeAnimalsCount 个生灵在响应变化")
                if (recentUnlock != null) {
                    append("，而且「$recentUnlock」留下的回响还没散掉")
                }
                append("。")
            }
        )
    }

    val calmIslandHint = when {
        islandLevel >= 12 && activeBuffCount > 0 -> "连续记录正在让舞台质感变得更完整，去试着补一件新的陈列。"
        petState == PetState.WORRIED || petState == PetState.SAD -> "宠物今晚有点安静，带它来看一圈灯光和水面，会更容易恢复。"
        else -> "再记录一点日常，或者换一件装饰，小岛就会继续往前生长。"
    }

    return IslandVisualState(
        artKey = IslandArtKey.TREEHOUSE,
        headline = if (islandLevel >= 10) "这片小岛已经开始像一座真正的夜间栖居地" else "它还在长大，但已经有了会让人想停留的角落",
        guidance = calmIslandHint
    )
}

enum class AchievementArtKey {
    RARE_GALLERY,
    LEGENDARY_SHOWCASE
}

data class AchievementVisualState(
    val artKey: AchievementArtKey,
    val heroLine: String,
    val emphasisTier: AchievementTier,
    val nextActionHint: String
)

fun buildAchievementVisualState(
    unlockedCount: Int,
    totalCount: Int,
    legendaryUnlockedCount: Int,
    nearMilestoneName: String?,
    petState: PetState,
    islandLevel: Int
): AchievementVisualState {
    val nearMilestone = nearMilestoneName?.takeIf { it.isNotBlank() }
    val completion = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f

    if (legendaryUnlockedCount > 0) {
        return AchievementVisualState(
            artKey = AchievementArtKey.LEGENDARY_SHOWCASE,
            heroLine = "你的收藏馆已经有了可以镇场的传说展品",
            emphasisTier = AchievementTier.LEGENDARY,
            nextActionHint = nearMilestone?.let { "下一件最值得追的是「$it」，把它补齐会让展柜层次更完整。" }
                ?: "去宠物和小岛里再制造一些回响，新的传说藏品会更快出现。"
        )
    }

    val emotionalLead = when (petState) {
        PetState.HAPPY, PetState.EXCITED -> "宠物现在正处在很适合解锁新陈列的状态"
        PetState.WORRIED, PetState.SAD -> "先照顾一下宠物，再回来补收藏，节奏会更舒服"
        else -> "今晚适合稳稳推进一件快完成的藏品"
    }

    val islandLead = if (islandLevel >= 10) "小岛等级已经能支撑更稀有的陈设奖励。" else "先把小岛继续养起来，成就馆也会跟着变厚。"

    return AchievementVisualState(
        artKey = if (completion >= 0.45f) AchievementArtKey.LEGENDARY_SHOWCASE else AchievementArtKey.RARE_GALLERY,
        heroLine = if (completion >= 0.45f) "你的收藏馆正在从纪念册变成真正的展厅" else "每一枚新徽章，都在让这座私人收藏馆更像样",
        emphasisTier = if (completion >= 0.45f) AchievementTier.EPIC else AchievementTier.RARE,
        nextActionHint = nearMilestone?.let { "$emotionalLead，优先去完成「$it」。$islandLead" }
            ?: "$emotionalLead，先从最近接近完成的那一项开始。$islandLead"
    )
}
