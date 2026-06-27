package com.diary.app.ui.writingcoach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.WritingCoach
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingCoachScreen(
    onNavigateBack: () -> Unit,
    viewModel: WritingCoachViewModel = viewModel()
) {
    val analysis by viewModel.analysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.analyze()
    }
    
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "写作教练",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { viewModel.analyze() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重新分析")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (analysis == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据，请先写几篇日记")
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // 写作类型
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = analysis!!.writingTimePattern,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "基于${analysis!!.totalEntries}篇日记分析",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 统计数据
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                "写作统计",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            StatRow("平均字数", "${analysis!!.avgWordCount.toInt()}字")
                            StatRow("平均句长", "${analysis!!.avgSentenceLength.toInt()}字")
                            StatRow("词汇丰富度", "${(analysis!!.vocabularyRichness * 100).toInt()}%")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 高频词
                    if (analysis!!.topRepeatedWords.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            innerPadding = 16.dp
                        ) {
                            Column {
                                Text(
                                    "常用词汇",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                analysis!!.topRepeatedWords.take(5).forEach { (word, count) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(word, fontSize = 14.sp)
                                        Text(
                                            "${count}次",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 情绪分布
                    if (analysis!!.emotionDistribution.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            innerPadding = 16.dp
                        ) {
                            Column {
                                Text(
                                    "情绪分布",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                analysis!!.emotionDistribution.forEach { (mood, count) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(mood, fontSize = 14.sp)
                                        Text(
                                            "${count}篇",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 建议
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                "写作建议",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            analysis!!.suggestions.forEachIndexed { index, suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        "${index + 1}.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        suggestion,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
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
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
