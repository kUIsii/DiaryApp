package com.diary.app.ui.textmicroscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextMicroscopeScreen(
    onNavigateBack: () -> Unit,
    viewModel: TextMicroscopeViewModel = viewModel()
) {
    val analysis by viewModel.analysis.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.analyzeAllEntries()
    }
    
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "文字显微镜",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (analysis == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val data = analysis!!
                
                // 总览
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("总字数", data.totalChars.toString())
                        StatItem("总句数", data.totalSentences.toString())
                        StatItem("总段落", data.totalParagraphs.toString())
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 高频词
                if (data.topWords.isNotEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                text = "高频词 TOP 10",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val maxCount = data.topWords.first().second
                            data.topWords.forEach { (word, count) ->
                                WordFrequencyItem(word, count, maxCount)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 写作特征
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "写作特征",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StatRow("平均句长", String.format("%.1f 字", data.avgSentenceLength))
                        StatRow("平均词长", String.format("%.1f 字", data.avgWordLength))
                        StatRow("用词丰富度", String.format("%.0f%%", data.vocabularyRichness * 100))
                        StatRow("标点密度", String.format("%.1f%%", data.punctuationRatio * 100))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 句长分布图
                if (data.sentenceLengths.isNotEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                text = "句长波动图",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "每句话的字数变化，反映写作节奏",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SentenceLengthChart(
                                lengths = data.sentenceLengths.take(100),
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WordFrequencyItem(word: String, count: Int, maxCount: Int) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = word, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = "$count 次", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = count.toFloat() / maxCount.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SentenceLengthChart(
    lengths: List<Int>,
    modifier: Modifier = Modifier
) {
    if (lengths.isEmpty()) return
    
    val maxLen = lengths.maxOrNull() ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (lengths.size - 1).coerceAtLeast(1)
        
        // Draw line
        val path = Path()
        lengths.forEachIndexed { index, len ->
            val x = index * stepX
            val y = height - (len.toFloat() / maxLen) * height * 0.8f - height * 0.1f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color.Transparent)
        drawPath(
            path,
            primaryColor.copy(alpha = 0.2f),
            style = Stroke(width = 2f)
        )
        
        // Draw points
        lengths.forEachIndexed { index, len ->
            val x = index * stepX
            val y = height - (len.toFloat() / maxLen) * height * 0.8f - height * 0.1f
            drawCircle(primaryColor, 3f, Offset(x, y))
        }
    }
}
