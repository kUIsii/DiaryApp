package com.diary.app.ui.nurturing

import com.diary.app.data.PetState

enum class NurturingRouteTarget {
    PET,
    ISLAND,
    ACHIEVEMENT
}

data class NurturingJourneyStep(
    val title: String,
    val detail: String,
    val target: NurturingRouteTarget
)

data class NurturingJourneyState(
    val headline: String,
    val summary: String,
    val primaryTarget: NurturingRouteTarget,
    val steps: List<NurturingJourneyStep>
)

fun buildNurturingJourneyState(
    petState: PetState,
    islandLevel: Int,
    recentAchievementUnlock: String?,
    hasRareDiscovery: Boolean,
    nearMilestoneName: String?,
    streakDays: Int
): NurturingJourneyState {
    val recentUnlock = recentAchievementUnlock?.takeIf { it.isNotBlank() }
    val milestone = nearMilestoneName?.takeIf { it.isNotBlank() }

    if (recentUnlock != null) {
        return NurturingJourneyState(
            headline = "刚得到的「$recentUnlock」，值得让整个夜晚都知道",
            summary = "先把新珍藏摆进成就殿堂，再去看看宠物和小岛会发生什么回响。",
            primaryTarget = NurturingRouteTarget.ACHIEVEMENT,
            steps = listOf(
                NurturingJourneyStep(
                    title = "把新珍藏摆进展柜",
                    detail = "先去成就殿堂确认「$recentUnlock」已经入藏。",
                    target = NurturingRouteTarget.ACHIEVEMENT
                ),
                NurturingJourneyStep(
                    title = "回去看看宠物反应",
                    detail = "它通常会先替你高兴，是最直接的情绪反馈。",
                    target = NurturingRouteTarget.PET
                ),
                NurturingJourneyStep(
                    title = "再去岛上收回响",
                    detail = if (hasRareDiscovery) "今晚岛上还有稀有现象，刚好一起看。" else "新的收藏常常会让小岛气氛更亮一点。",
                    target = NurturingRouteTarget.ISLAND
                )
            )
        )
    }

    if (petState == PetState.WORRIED || petState == PetState.SAD || petState == PetState.TIRED) {
        return NurturingJourneyState(
            headline = "今晚先把陪伴感补回来，后面的成长会顺很多",
            summary = milestone?.let { "先安抚它，再去岛上走一圈，最后顺手推进「$it」。" }
                ?: "先安抚它，再去岛上走一圈，整个夜晚会慢慢恢复到能继续成长的节奏。",
            primaryTarget = NurturingRouteTarget.PET,
            steps = listOf(
                NurturingJourneyStep(
                    title = "先安抚陪伴精灵",
                    detail = "轻触、投喂或看一眼记忆回响，先让它重新放松下来。",
                    target = NurturingRouteTarget.PET
                ),
                NurturingJourneyStep(
                    title = "带它去岛上散一圈",
                    detail = "环境变化能帮它缓下来，也容易触发新的回应。",
                    target = NurturingRouteTarget.ISLAND
                ),
                NurturingJourneyStep(
                    title = "再补一件快完成的收藏",
                    detail = milestone?.let { "今晚最适合顺手推进「$it」。" } ?: "情绪平稳后，再去成就馆看差一点点的目标。",
                    target = NurturingRouteTarget.ACHIEVEMENT
                )
            )
        )
    }

    if (hasRareDiscovery) {
        return NurturingJourneyState(
            headline = "岛上的新现象，正适合拿来扩大小世界的层次",
            summary = "先去看稀有现象，再回宠物和成就馆收掉这一轮回响。",
            primaryTarget = NurturingRouteTarget.ISLAND,
            steps = listOf(
                NurturingJourneyStep(
                    title = "先去看稀有现象",
                    detail = "稀有状态不会每次都出现，先把今晚的变化看掉最划算。",
                    target = NurturingRouteTarget.ISLAND
                ),
                NurturingJourneyStep(
                    title = "把发现带回给宠物",
                    detail = "看完小岛后，宠物的反馈通常会更鲜活一些。",
                    target = NurturingRouteTarget.PET
                ),
                NurturingJourneyStep(
                    title = "检查有没有新收藏可拿",
                    detail = milestone?.let { "尤其留意「$it」这种快完成目标。" } ?: "很多发现都会顺手推进成就馆里的收藏。",
                    target = NurturingRouteTarget.ACHIEVEMENT
                )
            )
        )
    }

    val islandLead = if (islandLevel >= 10 || streakDays >= 7) {
        "今晚已经有足够的积累，适合顺着这一圈继续扩张小世界。"
    } else {
        "先把连续感养起来，这个世界的变化会更明显。"
    }

    return NurturingJourneyState(
        headline = "这一晚最适合稳稳推进一轮完整养成",
        summary = milestone?.let { "$islandLead 现在优先目标是「$it」。" } ?: islandLead,
        primaryTarget = if (milestone != null) NurturingRouteTarget.ACHIEVEMENT else NurturingRouteTarget.ISLAND,
        steps = listOf(
            NurturingJourneyStep(
                title = "先看今晚的岛上变化",
                detail = "先确认环境、装饰和生灵有没有新的细节。",
                target = NurturingRouteTarget.ISLAND
            ),
            NurturingJourneyStep(
                title = "再和宠物互动一下",
                detail = "它会把这一轮变化变成更直接的陪伴反馈。",
                target = NurturingRouteTarget.PET
            ),
            NurturingJourneyStep(
                title = "最后收一件收藏进度",
                detail = milestone?.let { "优先推进「$it」，最容易收获成就感。" } ?: "去成就馆看看哪件藏品已经接近完成。",
                target = NurturingRouteTarget.ACHIEVEMENT
            )
        )
    )
}
