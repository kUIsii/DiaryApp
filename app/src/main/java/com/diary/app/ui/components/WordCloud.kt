package com.diary.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.stats.WordFrequency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordCloud(
    words: List<WordFrequency>,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) return

    val maxCount = words.maxOf { it.count }
    val minCount = words.minOf { it.count }
    val countRange = (maxCount - minCount).coerceAtLeast(1)

    // Animation
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) { visible = true }

    // Color palette for words
    val wordColors = listOf(
        Color(0xFF667eea),
        Color(0xFF764ba2),
        Color(0xFFf093fb),
        Color(0xFFf5576c),
        Color(0xFF4facfe),
        Color(0xFF00f2fe),
        Color(0xFF43e97b),
        Color(0xFF38f9d7),
        Color(0xFFfa709a),
        Color(0xFFfee140)
    )

    Column(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "词云",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "点击查看详情",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.take(30).forEachIndexed { index, wordFreq ->
                val normalizedCount = (wordFreq.count - minCount).toFloat() / countRange
                val fontSize = (12 + normalizedCount * 16).sp
                val color = wordColors[index % wordColors.size]

                // Staggered animation
                val itemAlpha by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = index * 30,
                        easing = FastOutSlowInEasing
                    ),
                    label = "itemAlpha_$index"
                )

                WordItem(
                    word = wordFreq.word,
                    count = wordFreq.count,
                    fontSize = fontSize,
                    color = color,
                    alpha = itemAlpha,
                    onClick = { onWordClick(wordFreq.word) }
                )
            }
        }
    }
}

@Composable
private fun WordItem(
    word: String,
    count: Int,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    alpha: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Text(
            text = word,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.9f)
        )
    }
}
