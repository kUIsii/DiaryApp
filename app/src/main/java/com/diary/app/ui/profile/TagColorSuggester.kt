package com.diary.app.ui.profile

/**
 * Suggests colors for tags based on semantic meaning of tag names.
 * Maps common Chinese/English keywords to appropriate colors.
 */
object TagColorSuggester {

    private val keywordColorMap = mapOf(
        // 情绪相关
        "开心" to 0xFFFFD54F, "快乐" to 0xFFFFD54F, "happy" to 0xFFFFD54F,
        "难过" to 0xFF5C6BC0, "伤心" to 0xFF5C6BC0, "sad" to 0xFF5C6BC0,
        "生气" to 0xFFEF5350, "愤怒" to 0xFFEF5350, "angry" to 0xFFEF5350,
        "平静" to 0xFF4DB6AC, "冷静" to 0xFF4DB6AC, "calm" to 0xFF4DB6AC,
        "焦虑" to 0xFFFF8A65, "紧张" to 0xFFFF8A65, "anxious" to 0xFFFF8A65,
        "幸福" to 0xFFF06292, "love" to 0xFFF06292, "爱" to 0xFFF06292,

        // 天气相关
        "晴" to 0xFFFFB74D, "sunny" to 0xFFFFB74D, "阳光" to 0xFFFFB74D,
        "雨" to 0xFF64B5F6, "rain" to 0xFF64B5F6, "下雨" to 0xFF64B5F6,
        "雪" to 0xFFE0E0E0, "snow" to 0xFFE0E0E0, "下雪" to 0xFFE0E0E0,
        "阴" to 0xFF90A4AE, "cloudy" to 0xFF90A4AE, "多云" to 0xFF90A4AE,
        "风" to 0xFF80CBC4, "wind" to 0xFF80CBC4, "大风" to 0xFF80CBC4,

        // 活动相关
        "工作" to 0xFF42A5F5, "work" to 0xFF42A5F5, "上班" to 0xFF42A5F5,
        "学习" to 0xFF7E57C2, "study" to 0xFF7E57C2, "读书" to 0xFF7E57C2,
        "运动" to 0xFF66BB6A, "健身" to 0xFF66BB6A, "exercise" to 0xFF66BB6A,
        "旅行" to 0xFFFF7043, "旅游" to 0xFFFF7043, "travel" to 0xFFFF7043,
        "美食" to 0xFFFFA726, "吃饭" to 0xFFFFA726, "food" to 0xFFFFA726,
        "电影" to 0xFFAB47BC, "music" to 0xFFAB47BC, "音乐" to 0xFFAB47BC,
        "游戏" to 0xFF26C6DA, "game" to 0xFF26C6DA,
        "购物" to 0xFFEC407A, "shopping" to 0xFFEC407A,
        "睡觉" to 0xFF7986CB, "sleep" to 0xFF7986CB, "休息" to 0xFF7986CB,

        // 时间相关
        "生日" to 0xFFEF5350, "birthday" to 0xFFEF5350,
        "节日" to 0xFFE53935, "holiday" to 0xFFE53935,
        "周末" to 0xFF26A69A, "weekend" to 0xFF26A69A,
        "假期" to 0xFF29B6F6, "vacation" to 0xFF29B6F6,

        // 地点相关
        "家" to 0xFF8D6E63, "home" to 0xFF8D6E63,
        "学校" to 0xFF5C6BC0, "school" to 0xFF5C6BC0,
        "公司" to 0xFF78909C, "office" to 0xFF78909C,
        "医院" to 0xFFEF5350, "hospital" to 0xFFEF5350,

        // 人物相关
        "家人" to 0xFFA1887F, "family" to 0xFFA1887F,
        "朋友" to 0xFF4DB6AC, "friend" to 0xFF4DB6AC,
        "同事" to 0xFF90A4AE, "colleague" to 0xFF90A4AE,
        "恋人" to 0xFFF06292, "partner" to 0xFFF06292,

        // 健康相关
        "健康" to 0xFF66BB6A, "health" to 0xFF66BB6A,
        "生病" to 0xFFEF5350, "sick" to 0xFFEF5350,
        "感冒" to 0xFFFF8A65, "cold" to 0xFFFF8A65,

        // 其他
        "重要" to 0xFFEF5350, "important" to 0xFFEF5350,
        "紧急" to 0xFFFF5252, "urgent" to 0xFFFF5252,
        "灵感" to 0xFFAB47BC, "idea" to 0xFFAB47BC,
        "目标" to 0xFF42A5F5, "goal" to 0xFF42A5F5,
        "梦想" to 0xFF7E57C2, "dream" to 0xFF7E57C2,
        "日记" to 0xFF78909C, "diary" to 0xFF78909C,
        "随笔" to 0xFF90A4AE, "笔记" to 0xFF90A4AE,
    )

    /**
     * Suggest a color for a tag name based on keyword matching.
     * Returns null if no matching keyword is found.
     */
    fun suggestColor(tagName: String): Long? {
        val lowerName = tagName.lowercase().trim()
        // Try exact match first
        keywordColorMap[lowerName]?.let { return it }
        // Try partial match (tag name contains keyword)
        for ((keyword, color) in keywordColorMap) {
            if (lowerName.contains(keyword)) {
                return color
            }
        }
        return null
    }

    /**
     * Get a list of suggested colors for a tag name.
     * Returns up to 3 suggestions based on keyword matching.
     */
    fun suggestColors(tagName: String): List<Long> {
        val lowerName = tagName.lowercase().trim()
        val matches = mutableListOf<Long>()

        // Exact match first
        keywordColorMap[lowerName]?.let { matches.add(it) }

        // Partial matches
        for ((keyword, color) in keywordColorMap) {
            if (lowerName.contains(keyword) && color !in matches) {
                matches.add(color)
                if (matches.size >= 3) break
            }
        }

        return matches
    }

    /**
     * Generate a deterministic color from a tag name using hash.
     * Used as fallback when no keyword match is found.
     */
    fun generateColorFromName(tagName: String): Long {
        val hue = (tagName.hashCode().toLong() and 0xFFFFFFFFL) % 360
        val hsv = floatArrayOf(hue.toFloat(), 0.6f, 0.85f)
        return android.graphics.Color.HSVToColor(hsv).toLong() and 0xFFFFFFFFL
    }
}
