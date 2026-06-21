package com.diary.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class WordFrequency(
    val word: String,
    val count: Int
)

/**
 * Extracts top words from a list of plain text strings.
 * Handles both Chinese and English text.
 */
fun extractTopWords(texts: List<String>, limit: Int = 50): List<WordFrequency> {
    val stopWords = setOf(
        // Common Chinese stop words
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
        "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
        "自己", "这", "他", "她", "它", "们", "那", "里", "为", "什么", "怎么", "吗",
        "吧", "啊", "呢", "嗯", "哦", "哈", "呀", "啦", "哎", "唉", "嗨",
        "可以", "已经", "还是", "就是", "不是", "但是", "因为", "所以", "如果",
        "这个", "那个", "一些", "觉得", "知道", "时候", "现在", "今天", "明天",
        "昨天", "真的", "一下", "一些", "一直", "一样", "可能", "需要", "应该",
        // Multi-character particles and fillers
        "了吗", "的了", "是的", "好吧", "嗯嗯", "哈哈", "呢吧", "了啊",
        "的吗", "了吗", "了呢", "啊啊", "嘻嘻", "嘿嘿",
        // Common English stop words
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of",
        "with", "by", "is", "am", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
        "may", "might", "can", "shall", "i", "you", "he", "she", "it", "we", "they",
        "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their",
        "this", "that", "these", "those", "not", "no", "so", "if", "then", "than",
        "too", "very", "just", "about", "up", "out", "all", "from", "get", "got"
    )

    val wordCounts = mutableMapOf<String, Int>()

    texts.forEach { text ->
        // Extract Chinese words (2-4 chars) and English words
        val chinesePattern = Regex("[\\u4e00-\\u9fff]{2,4}")
        val englishPattern = Regex("[a-zA-Z]{3,}")

        chinesePattern.findAll(text).forEach { match ->
            val word = match.value
            if (word !in stopWords) {
                wordCounts[word] = (wordCounts[word] ?: 0) + 1
            }
        }

        englishPattern.findAll(text).forEach { match ->
            val word = match.value.lowercase()
            if (word !in stopWords) {
                wordCounts[word] = (wordCounts[word] ?: 0) + 1
            }
        }
    }

    return wordCounts.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { WordFrequency(it.key, it.value) }
}

/**
 * A word cloud composable that renders words in a spiral layout.
 * Words are sized and colored based on their frequency.
 */
@Composable
fun WordCloud(
    words: List<WordFrequency>,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color.Unspecified,
    secondaryColor: Color = Color.Unspecified
) {
    if (words.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val maxCount = remember(words) { words.maxOf { it.count } }
    val minCount = remember(words) { words.minOf { it.count } }

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        val placements = calculateWordPlacements(
            words = words,
            maxCount = maxCount,
            minCount = minCount,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            textMeasurer = textMeasurer,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            centerX = centerX,
            centerY = centerY
        )

        placements.forEach { placement ->
            drawText(
                textLayoutResult = placement.layoutResult,
                topLeft = Offset(
                    placement.x - placement.layoutResult.size.width / 2f,
                    placement.y - placement.layoutResult.size.height / 2f
                )
            )
        }
    }
}

private data class WordPlacement(
    val x: Float,
    val y: Float,
    val layoutResult: androidx.compose.ui.text.TextLayoutResult
)

private fun calculateWordPlacements(
    words: List<WordFrequency>,
    maxCount: Int,
    minCount: Int,
    primaryColor: Color,
    secondaryColor: Color,
    textMeasurer: TextMeasurer,
    canvasWidth: Float = 600f,
    canvasHeight: Float = 440f,
    centerX: Float = canvasWidth / 2f,
    centerY: Float = canvasHeight / 2f
): List<WordPlacement> {
    val placements = mutableListOf<WordPlacement>()
    val occupiedAreas = mutableListOf<androidx.compose.ui.geometry.Rect>()

    words.forEachIndexed { index, word ->
        val normalizedCount = if (maxCount > minCount) {
            (word.count - minCount).toFloat() / (maxCount - minCount)
        } else {
            0.5f
        }
        val fontSize = 11 + (normalizedCount * 21)

        val color = if (primaryColor == Color.Unspecified || secondaryColor == Color.Unspecified) {
            Color(0xFF333333).copy(alpha = 0.6f + normalizedCount * 0.4f)
        } else {
            blendColors(primaryColor, secondaryColor, normalizedCount)
        }

        val style = TextStyle(
            fontSize = fontSize.sp,
            fontWeight = if (normalizedCount > 0.5f) FontWeight.Bold else FontWeight.Medium,
            color = color
        )

        val layoutResult = textMeasurer.measure(word.word, style)
        val width = layoutResult.size.width.toFloat()
        val height = layoutResult.size.height.toFloat()

        val position = findNonOverlappingPosition(
            width = width,
            height = height,
            occupiedAreas = occupiedAreas,
            centerX = centerX,
            centerY = centerY
        )

        placements.add(WordPlacement(position.x, position.y, layoutResult))
        occupiedAreas.add(
            androidx.compose.ui.geometry.Rect(
                left = position.x - width / 2f,
                top = position.y - height / 2f,
                right = position.x + width / 2f,
                bottom = position.y + height / 2f
            )
        )
    }

    return placements
}

private fun findNonOverlappingPosition(
    width: Float,
    height: Float,
    occupiedAreas: List<androidx.compose.ui.geometry.Rect>,
    centerX: Float,
    centerY: Float
): Offset {
    var angle = 0f
    val maxAttempts = 200

    repeat(maxAttempts) {
        val radius = angle * 8f
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle) * 0.6f

        val candidateRect = androidx.compose.ui.geometry.Rect(
            left = x - width / 2f,
            top = y - height / 2f,
            right = x + width / 2f,
            bottom = y + height / 2f
        )

        if (occupiedAreas.none { it.overlaps(candidateRect) }) {
            return Offset(x, y)
        }

        angle += 0.3f
    }

    return Offset(centerX + (occupiedAreas.size * 10f) % 200f, centerY)
}

private fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
    return Color(
        red = color1.red * (1 - ratio) + color2.red * ratio,
        green = color1.green * (1 - ratio) + color2.green * ratio,
        blue = color1.blue * (1 - ratio) + color2.blue * ratio,
        alpha = 0.7f + ratio * 0.3f
    )
}
