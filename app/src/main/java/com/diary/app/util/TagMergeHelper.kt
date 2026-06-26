package com.diary.app.util

import com.diary.app.data.Tag

/**
 * Helper for detecting similar tags and suggesting merges.
 * Uses edit distance, containment, and semantic similarity.
 */
object TagMergeHelper {

    /**
     * Find groups of similar tags that might be candidates for merging.
     * Returns groups of tags where each group contains similar tags.
     */
    fun findSimilarTagGroups(tags: List<Tag>, threshold: Double = 0.6): List<List<Tag>> {
        if (tags.size < 2) return emptyList()

        val groups = mutableListOf<MutableList<Tag>>()
        val used = mutableSetOf<Long>()

        for (i in tags.indices) {
            if (tags[i].id in used) continue

            val group = mutableListOf(tags[i])
            used.add(tags[i].id)

            for (j in i + 1 until tags.size) {
                if (tags[j].id in used) continue

                if (areSimilar(tags[i].name, tags[j].name, threshold)) {
                    group.add(tags[j])
                    used.add(tags[j].id)
                }
            }

            if (group.size > 1) {
                groups.add(group)
            }
        }

        return groups
    }

    /**
     * Check if two tag names are similar enough to consider merging.
     */
    fun areSimilar(name1: String, name2: String, threshold: Double = 0.6): Boolean {
        val n1 = name1.lowercase().trim()
        val n2 = name2.lowercase().trim()

        // Exact match
        if (n1 == n2) return true

        // Containment check
        if (n1.contains(n2) || n2.contains(n1)) return true

        // Edit distance similarity
        val editSimilarity = 1.0 - (editDistance(n1, n2).toDouble() / maxOf(n1.length, n2.length))
        if (editSimilarity >= threshold) return true

        // Pinyin similarity (for Chinese characters)
        if (arePinyinSimilar(n1, n2)) return true

        return false
    }

    /**
     * Calculate Levenshtein edit distance between two strings.
     */
    private fun editDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
                }
            }
        }

        return dp[m][n]
    }

    /**
     * Check if two strings might be similar in pinyin.
     * Simple heuristic: check for common pinyin patterns.
     */
    private fun arePinyinSimilar(s1: String, s2: String): Boolean {
        // Common homophone pairs in Chinese
        val homophoneGroups = listOf(
            setOf("旅行", "旅游", "出行"),
            setOf("工作", "上班", "办公"),
            setOf("学习", "读书", "看书"),
            setOf("运动", "健身", "锻炼"),
            setOf("美食", "吃饭", "饮食"),
            setOf("开心", "快乐", "高兴"),
            setOf("难过", "伤心", "悲伤"),
            setOf("日记", "日志", "笔记"),
            setOf("灵感", "创意", "想法"),
            setOf("家人", "家庭", "亲人"),
            setOf("朋友", "好友", "闺蜜"),
            setOf("同事", "同僚", "伙伴"),
            setOf("生日", "诞辰", "生辰"),
            setOf("假期", "放假", "休假"),
            setOf("周末", "周日", "星期天"),
            setOf("购物", "逛街", "买东西"),
            setOf("电影", "影院", "看电影"),
            setOf("音乐", "听歌", "歌曲"),
            setOf("游戏", "玩", "打游戏"),
            setOf("睡觉", "睡眠", "休息"),
            setOf("早起", "早睡", "早安"),
            setOf("晚安", "晚睡", "熬夜"),
        )

        for (group in homophoneGroups) {
            if (s1 in group && s2 in group) return true
        }

        return false
    }

    /**
     * Get a similarity score between two tag names (0.0 to 1.0).
     */
    fun similarityScore(name1: String, name2: String): Double {
        val n1 = name1.lowercase().trim()
        val n2 = name2.lowercase().trim()

        if (n1 == n2) return 1.0
        if (n1.contains(n2) || n2.contains(n1)) return 0.9

        val editSimilarity = 1.0 - (editDistance(n1, n2).toDouble() / maxOf(n1.length, n2.length))

        // Boost score for homophones
        if (arePinyinSimilar(n1, n2)) {
            return maxOf(editSimilarity, 0.8)
        }

        return editSimilarity
    }

    /**
     * Suggest which tag to keep when merging a group of similar tags.
     * Prefers: most used > longest name > first alphabetically.
     */
    fun suggestKeepTag(tags: List<Tag>): Tag {
        return tags.maxWith(
            compareBy<Tag> { it.usageCount }
                .thenBy { it.name.length }
                .thenBy { it.name }
        )
    }
}
