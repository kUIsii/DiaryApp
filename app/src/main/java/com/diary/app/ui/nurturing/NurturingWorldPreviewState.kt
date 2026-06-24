package com.diary.app.ui.nurturing

data class NurturingWorldPreviewState(
    val headline: String,
    val petSnippet: String,
    val islandSnippet: String,
    val collectionSnippet: String
)

fun buildNurturingWorldPreview(
    petName: String?,
    petStateLabel: String?,
    petMessage: String?,
    islandLevel: Int,
    islandMoodLabel: String?,
    recentTitle: String?
): NurturingWorldPreviewState {
    val safePetName = petName?.takeIf { it.isNotBlank() }
    val normalizedMessage = petMessage?.takeIf { it.isNotBlank() }
    val normalizedState = petStateLabel?.takeIf { it.isNotBlank() }
    val normalizedIslandMood = islandMoodLabel?.takeIf { it.isNotBlank() }
    val normalizedRecentTitle = recentTitle?.takeIf { it.isNotBlank() }

    val headline = if (safePetName != null) {
        "${safePetName}正在等你"
    } else {
        "养成世界正在慢慢生长"
    }

    val petSnippet = normalizedMessage
        ?: normalizedState?.let { "今天的陪伴精灵有一点$it，去看看它吧" }
        ?: "去看看你的陪伴精灵今天状态如何"

    val islandSnippet = "${normalizedIslandMood ?: "夜色浮动"} · Lv.$islandLevel"
    val collectionSnippet = if (normalizedRecentTitle != null) {
        "最近珍藏：$normalizedRecentTitle"
    } else {
        "今晚也许会有新的珍藏出现"
    }

    return NurturingWorldPreviewState(
        headline = headline,
        petSnippet = petSnippet,
        islandSnippet = islandSnippet,
        collectionSnippet = collectionSnippet
    )
}
