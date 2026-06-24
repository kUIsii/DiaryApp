package com.diary.app.ui.pet

fun buildPetMoodCopy(stateLabel: String?, feedbackText: String): String {
    if (feedbackText.isNotBlank()) return feedbackText

    return when (stateLabel) {
        "开心" -> "你一来，空气都亮起来了。"
        "困倦" -> "夜深了，我们都慢一点。"
        "担心" -> "如果今天很累，也没关系，我在这里。"
        "难过" -> "今天如果有点沉，我会先陪你把心放下来。"
        "兴奋" -> "有新的故事要发生了，我已经准备好陪你一起看。"
        "好奇" -> "今晚的小世界有点微微发亮，我们去看看吧。"
        "疲惫" -> "先休息一下也很好，我会守着这片安静。"
        else -> "今晚也辛苦了，我会陪你慢慢安静下来。"
    }
}
