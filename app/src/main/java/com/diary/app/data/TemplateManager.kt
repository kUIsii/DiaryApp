package com.diary.app.data

object TemplateManager {

    fun getAllTemplates(): List<DiaryTemplate> = templates

    fun getTemplatesByCategory(category: TemplateCategory): List<DiaryTemplate> =
        templates.filter { it.category == category }

    fun getTemplateById(id: String): DiaryTemplate? =
        templates.find { it.id == id }

    fun getCategories(): List<TemplateCategory> = TemplateCategory.entries

    private val templates = listOf(
        // 日常
        DiaryTemplate(
            id = "daily_record",
            name = "每日记录",
            icon = "today",
            content = buildString {
                appendLine("天气：")
                appendLine("心情：")
                appendLine()
                appendLine("三件好事：")
                appendLine("1. ")
                appendLine("2. ")
                appendLine("3. ")
                appendLine()
                appendLine("今日感悟：")
            },
            category = TemplateCategory.DAILY
        ),
        DiaryTemplate(
            id = "gratitude",
            name = "感恩日记",
            icon = "favorite",
            content = buildString {
                appendLine("今天感恩的三件事：")
                appendLine("1. ")
                appendLine("2. ")
                appendLine("3. ")
                appendLine()
                appendLine("今天的小确幸：")
                appendLine()
                appendLine("明天的期待：")
            },
            category = TemplateCategory.DAILY
        ),

        // 情感
        DiaryTemplate(
            id = "emotion",
            name = "情绪日记",
            icon = "mood",
            content = buildString {
                appendLine("当前情绪：")
                appendLine("情绪强度：/10")
                appendLine()
                appendLine("情绪描述：")
                appendLine()
                appendLine("可能的原因：")
                appendLine()
                appendLine("应对方法：")
                appendLine()
                appendLine("想对自己说的话：")
            },
            category = TemplateCategory.EMOTIONAL
        ),
        DiaryTemplate(
            id = "self_reflection",
            name = "自我反思",
            icon = "psychology",
            content = buildString {
                appendLine("今天做得好的地方：")
                appendLine()
                appendLine("可以改进的地方：")
                appendLine()
                appendLine("学到了什么：")
                appendLine()
                appendLine("明天想尝试的事：")
            },
            category = TemplateCategory.EMOTIONAL
        ),

        // 创意
        DiaryTemplate(
            id = "free_writing",
            name = "自由写作",
            icon = "edit",
            content = "",
            category = TemplateCategory.CREATIVE
        ),
        DiaryTemplate(
            id = "reading_note",
            name = "读书笔记",
            icon = "menu_book",
            content = buildString {
                appendLine("书名：")
                appendLine("作者：")
                appendLine()
                appendLine("精彩摘录：")
                appendLine()
                appendLine("我的感想：")
                appendLine()
                appendLine("推荐指数：/5")
            },
            category = TemplateCategory.CREATIVE
        ),

        // 旅行
        DiaryTemplate(
            id = "travel",
            name = "旅行日记",
            icon = "flight",
            content = buildString {
                appendLine("目的地：")
                appendLine("同行者：")
                appendLine()
                appendLine("今天的经历：")
                appendLine()
                appendLine("最喜欢的瞬间：")
                appendLine()
                appendLine("美食记录：")
                appendLine()
                appendLine("花费记录：")
            },
            category = TemplateCategory.TRAVEL
        ),

        // 工作
        DiaryTemplate(
            id = "work_review",
            name = "工作总结",
            icon = "work",
            content = buildString {
                appendLine("今日完成：")
                appendLine("- ")
                appendLine()
                appendLine("遇到的问题：")
                appendLine()
                appendLine("解决方案：")
                appendLine()
                appendLine("明日计划：")
                appendLine("- ")
                appendLine()
                appendLine("学到了什么：")
            },
            category = TemplateCategory.WORK
        )
    )
}
