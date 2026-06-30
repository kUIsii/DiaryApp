package com.diary.app.ui.writingcenter

import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.cleanPreviewText
import com.diary.app.ui.components.formatWordCountWithUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WritingGrowthPrimaryAction { WRITE, CONTINUE, OVERVIEW }

enum class WritingGrowthItemTarget {
    NONE,
    EDITOR,
    WRITING_COACH,
    WRITING_LAB,
    WRITING_HINT,
    SMALL_WINS
}

data class WritingGrowthAction(
    val label: String,
    val description: String,
    val target: WritingGrowthPrimaryAction
)

data class WritingGrowthItem(
    val title: String,
    val summary: String,
    val metadata: String? = null,
    val target: WritingGrowthItemTarget = WritingGrowthItemTarget.NONE
)

data class WritingGrowthSection(
    val title: String,
    val subtitle: String,
    val items: List<WritingGrowthItem> = emptyList(),
    val quickActions: List<WritingGrowthAction> = emptyList()
)

data class WritingGrowthCenterContent(
    val sections: List<WritingGrowthSection>
)

fun defaultPrimaryAction(): WritingGrowthPrimaryAction = WritingGrowthPrimaryAction.WRITE

fun buildWritingGrowthCenterContent(
    latestEntryTitle: String?,
    hasAiSupport: Boolean,
    todayWordCount: Int,
    writingDaysThisWeek: Int,
    recentSedimentedContent: List<String>
): WritingGrowthCenterContent {
    val todaySection = WritingGrowthSection(
        title = "今日起点",
        subtitle = latestEntryTitle?.let { "接着上次的写作气口继续" } ?: "从一句想法开始也很好",
        items = listOf(
            WritingGrowthItem(
                title = latestEntryTitle ?: "还没有最近草稿",
                summary = if (latestEntryTitle != null) "继续上一次写到一半的内容" else "先写下今天最想留下的一件事",
                metadata = if (latestEntryTitle != null) "最近标题" else "本地兜底",
                target = WritingGrowthItemTarget.EDITOR
            )
        )
    )

    val growthSection = WritingGrowthSection(
        title = "成长概览",
        subtitle = if (hasAiSupport) "本地统计和 AI 建议都可用" else "离线也能看见当前写作状态",
        items = listOf(
            WritingGrowthItem("今日字数", formatWordCountWithUnit(todayWordCount), "今天", WritingGrowthItemTarget.WRITING_COACH),
            WritingGrowthItem("本周写作天数", "${writingDaysThisWeek}天", "近7天", WritingGrowthItemTarget.WRITING_COACH)
        )
    )

    val quickActions = listOf(
        WritingGrowthAction("写一篇", "直接进入编辑器", WritingGrowthPrimaryAction.WRITE),
        WritingGrowthAction("继续最近", "接着上一段写", WritingGrowthPrimaryAction.CONTINUE),
        WritingGrowthAction("看成长概览", "查看写作状态", WritingGrowthPrimaryAction.OVERVIEW)
    )

    val quickEntrySection = WritingGrowthSection(
        title = "快捷入口",
        subtitle = "先写，再看，再决定下一步",
        quickActions = quickActions,
        items = listOf(
            WritingGrowthItem("写作实验室", "做风格实验和创作练习", "实验", WritingGrowthItemTarget.WRITING_LAB),
            WritingGrowthItem("写作灵感", "快速找到今天能写什么", "提示", WritingGrowthItemTarget.WRITING_HINT),
            WritingGrowthItem("小确幸", "把值得珍惜的小片段记下来", "沉淀", WritingGrowthItemTarget.SMALL_WINS)
        )
    )

    val sedimentItems = if (recentSedimentedContent.isNotEmpty()) {
        recentSedimentedContent.take(4).mapIndexed { index, item ->
            WritingGrowthItem(
                title = "沉淀 ${index + 1}",
                summary = item,
                metadata = "已整理",
                target = WritingGrowthItemTarget.EDITOR
            )
        }
    } else {
        listOf(
            WritingGrowthItem(
                title = "本地兜底内容",
                summary = "从今天的一个细节开始：天气、情绪、对话或一个没说出口的念头。",
                metadata = "离线可用",
                target = WritingGrowthItemTarget.EDITOR
            ),
            WritingGrowthItem(
                title = "可复用句子",
                summary = "今天不必完整，只要留下一个未来还能读懂的片段。",
                metadata = "离线可用",
                target = WritingGrowthItemTarget.EDITOR
            )
        )
    }

    val sedimentSection = WritingGrowthSection(
        title = "最近沉淀",
        subtitle = "从旧内容里捞出可继续生长的片段",
        items = sedimentItems
    )

    return WritingGrowthCenterContent(
        sections = listOf(todaySection, growthSection, quickEntrySection, sedimentSection)
    )
}

fun buildWritingGrowthCenterFromPreviews(
    previews: List<DiaryPreview>,
    hasAiSupport: Boolean
): WritingGrowthCenterContent {
    val latest = previews.firstOrNull()
    val thisWeek = previews.count { it.createdAt >= System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val sediment = previews.take(3).map { preview ->
        cleanPreviewText(preview.plainText).take(120).ifBlank { preview.title }
    }
    return buildWritingGrowthCenterContent(
        latestEntryTitle = latest?.title,
        hasAiSupport = hasAiSupport,
        todayWordCount = previews.take(1).sumOf { cleanPreviewText(it.plainText).length },
        writingDaysThisWeek = thisWeek.coerceAtLeast(0),
        recentSedimentedContent = sediment
    )
}
