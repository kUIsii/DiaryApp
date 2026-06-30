package com.diary.app.ui.readingcenter

enum class ReadingCenterTarget {
    IMMERSIVE_READER,
    FOCUS_MODE,
    OUTLINE_VIEW
}

data class ReadingSessionSnapshot(
    val diaryId: Long? = null,
    val title: String? = null,
    val previewText: String? = null,
    val pageIndex: Int = 0,
    val totalPages: Int = 0,
    val paragraphIndex: Int = 0,
    val lastReadAt: Long? = null,
    val themeName: String? = null,
    val hasActiveFocus: Boolean = false,
    val bookmarkParagraphs: List<Int> = emptyList()
)

data class ReadingCenterHeroAction(
    val label: String,
    val description: String,
    val target: ReadingCenterTarget
)

data class ReadingCenterFeatureItem(
    val title: String,
    val summary: String,
    val target: ReadingCenterTarget
)

data class ReadingCenterOverviewItem(
    val label: String,
    val value: String
)

data class ReadingCenterContent(
    val heroTitle: String,
    val heroSummary: String,
    val heroActions: List<ReadingCenterHeroAction>,
    val featureItems: List<ReadingCenterFeatureItem>,
    val recentEntryTitles: List<String>,
    val overviewItems: List<ReadingCenterOverviewItem>
)

fun buildReadingCenterContent(
    session: ReadingSessionSnapshot,
    recentEntries: List<String>,
    completedFocusSessions: Int
): ReadingCenterContent {
    val hasSession = session.diaryId != null
    val title = session.title?.takeIf { it.isNotBlank() } ?: "今天的阅读空间"
    val progress = if (session.totalPages > 0) {
        "第 ${session.pageIndex + 1} / ${session.totalPages} 页"
    } else {
        "还没有保存阅读进度"
    }

    val heroActions = if (hasSession) {
        listOf(
            ReadingCenterHeroAction("继续阅读", "回到《$title》并接上当前进度", ReadingCenterTarget.IMMERSIVE_READER),
            ReadingCenterHeroAction("进入专注", "围绕这篇内容开始一段安静阅读", ReadingCenterTarget.FOCUS_MODE),
            ReadingCenterHeroAction("阅读复盘", "读完后回看结构和重点段落", ReadingCenterTarget.OUTLINE_VIEW)
        )
    } else {
        listOf(
            ReadingCenterHeroAction("开始阅读", "从最近的内容里选一篇进入沉浸阅读", ReadingCenterTarget.IMMERSIVE_READER),
            ReadingCenterHeroAction("进入专注", "先用一段专注时间把阅读氛围搭起来", ReadingCenterTarget.FOCUS_MODE)
        )
    }

    val featureItems = listOf(
        ReadingCenterFeatureItem(
            title = "沉浸阅读",
            summary = if (hasSession) "已经记住当前进度和主题，可直接续读" else "用更克制的排版进入正文",
            target = ReadingCenterTarget.IMMERSIVE_READER
        ),
        ReadingCenterFeatureItem(
            title = "专注模式",
            summary = if (session.hasActiveFocus) "当前已有专注会话，回去继续即可" else "让倒计时围绕当前阅读内容工作",
            target = ReadingCenterTarget.FOCUS_MODE
        ),
        ReadingCenterFeatureItem(
            title = "阅读复盘",
            summary = "先看结构，再看重点段落和情绪变化",
            target = ReadingCenterTarget.OUTLINE_VIEW
        )
    )

    val overviewItems = listOf(
        ReadingCenterOverviewItem("当前进度", if (hasSession) progress else "尚未开始"),
        ReadingCenterOverviewItem("当前主题", session.themeName ?: "默认阅读空间"),
        ReadingCenterOverviewItem("最近专注", "${completedFocusSessions} 次"),
        ReadingCenterOverviewItem("书签段落", "${session.bookmarkParagraphs.size} 处")
    )

    return ReadingCenterContent(
        heroTitle = if (hasSession) "继续《$title》" else "先选一篇值得继续读的内容",
        heroSummary = if (hasSession) {
            "上次停在 $progress，${if (session.hasActiveFocus) "专注状态仍在保留。" else "可以直接回到正文。"}"
        } else {
            "阅读中心会帮你把阅读、专注、复盘和主题放回同一条路径里。"
        },
        heroActions = heroActions,
        featureItems = featureItems,
        recentEntryTitles = recentEntries.take(4),
        overviewItems = overviewItems
    )
}

fun updateReadingSessionPage(
    session: ReadingSessionSnapshot,
    requestedPage: Int
): ReadingSessionSnapshot {
    val bounded = requestedPage.coerceIn(0, (session.totalPages - 1).coerceAtLeast(0))
    return session.copy(
        pageIndex = bounded,
        paragraphIndex = bounded,
        lastReadAt = System.currentTimeMillis()
    )
}

fun buildReadingFocusSummary(
    session: ReadingSessionSnapshot,
    selectedDuration: Int
): String {
    val title = session.title ?: "当前阅读内容"
    val progress = if (session.totalPages > 0) {
        "${session.pageIndex + 1}/${session.totalPages}"
    } else {
        "未开始"
    }
    return "围绕《$title》专注 ${selectedDuration} 分钟，当前进度 $progress。"
}

fun buildReadingReviewSummary(
    totalWords: Int,
    paragraphCount: Int,
    headingCount: Int
): String {
    return "这篇内容共有 ${totalWords} 字，分成 ${paragraphCount} 段，识别出 ${headingCount} 个结构节点。"
}

fun buildReadingThemePreviewDescription(
    themeName: String,
    isDefault: Boolean
): String {
    return if (isDefault) {
        "$themeName · 当前默认阅读空间"
    } else {
        "$themeName · 预览当前阅读空间"
    }
}
