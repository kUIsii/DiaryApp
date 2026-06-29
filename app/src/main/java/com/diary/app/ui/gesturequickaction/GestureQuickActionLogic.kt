package com.diary.app.ui.gesturequickaction

data class GestureExecutionPreview(
    val route: String?,
    val label: String,
    val canExecute: Boolean,
    val note: String
)

fun resolveGestureExecutionPreview(actionName: String): GestureExecutionPreview {
    return when (actionName) {
        "新建日记" -> GestureExecutionPreview(
            route = "editor",
            label = "打开日记编辑器",
            canExecute = true,
            note = "将直接进入编辑器，开始记录。"
        )
        "快速签到" -> GestureExecutionPreview(
            route = "quick_checkin",
            label = "打开快速签到",
            canExecute = true,
            note = "适合记录当下心情和一句话。"
        )
        "打开搜索" -> GestureExecutionPreview(
            route = "semantic_search",
            label = "打开语义搜索",
            canExecute = true,
            note = "用于快速检索旧日记。"
        )
        "打开统计" -> GestureExecutionPreview(
            route = "stats",
            label = "打开统计页",
            canExecute = true,
            note = "查看写作和情绪变化。"
        )
        "打开收藏" -> GestureExecutionPreview(
            route = "favorites",
            label = "打开收藏页",
            canExecute = true,
            note = "快速访问常看内容。"
        )
        "打开待办" -> GestureExecutionPreview(
            route = "todo",
            label = "打开待办页",
            canExecute = true,
            note = "查看待处理事项。"
        )
        "打开时间线" -> GestureExecutionPreview(
            route = "timeline",
            label = "打开时间线",
            canExecute = true,
            note = "按时间回到上下文。"
        )
        "打开语音记录" -> GestureExecutionPreview(
            route = "voice_recording",
            label = "打开语音备忘录",
            canExecute = true,
            note = "适合口述补充记录。"
        )
        "打开专注模式" -> GestureExecutionPreview(
            route = "focus_mode",
            label = "打开专注模式",
            canExecute = true,
            note = "减少干扰，进入沉浸写作。"
        )
        "打开环境音" -> GestureExecutionPreview(
            route = "ambient_sound",
            label = "打开环境音",
            canExecute = true,
            note = "搭配记录时的场景氛围。"
        )
        "打开那年今日" -> GestureExecutionPreview(
            route = "personal_yearbook",
            label = "打开个人年鉴",
            canExecute = true,
            note = "回看同日同刻的内容。"
        )
        "打开AI助手" -> GestureExecutionPreview(
            route = "ai_assistant",
            label = "打开 AI 助手",
            canExecute = true,
            note = "用于补写、总结或追问。"
        )
        "打开工具箱" -> GestureExecutionPreview(
            route = "tools",
            label = "打开工具页",
            canExecute = true,
            note = "返回功能集合中心。"
        )
        "打开设置" -> GestureExecutionPreview(
            route = "adaptive_interface",
            label = "打开自适应界面",
            canExecute = true,
            note = "查看界面适配能力。"
        )
        "打开编辑草稿" -> GestureExecutionPreview(
            route = "editor",
            label = "打开编辑草稿",
            canExecute = true,
            note = "继续编辑未完成内容。"
        )
        else -> GestureExecutionPreview(
            route = null,
            label = actionName,
            canExecute = false,
            note = "当前还没有接入这个动作。"
        )
    }
}
