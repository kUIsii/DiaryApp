package com.diary.app.ui.writingfingerprint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingFingerprintScreen(
    onNavigateBack: () -> Unit,
    viewModel: WritingFingerprintViewModel = viewModel()
) {
    val analysis by viewModel.analysis.collectAsState()

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
                    text = "写作指纹",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (analysis == null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在分析写作风格...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val data = analysis!!

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "你的写作指纹",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FingerprintVisualization(
                            avgSentenceLength = data.avgSentenceLength,
                            vocabularyRichness = data.vocabularyRichness,
                            punctuationRatio = data.punctuationRatio,
                            modifier = Modifier.size(200.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = data.styleLabel,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "风格特征",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FingerprintStatRow("平均句长", "${"%.1f".format(data.avgSentenceLength)} 字")
                        FingerprintStatRow("用词丰富度", "${"%.0f".format(data.vocabularyRichness * 100)}%")
                        FingerprintStatRow("平均词长", "${"%.1f".format(data.avgWordLength)} 字")
                        FingerprintStatRow("标点密度", "${"%.1f".format(data.punctuationRatio * 100)}%")
                        FingerprintStatRow("段落数", "${data.paragraphCount}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "风格演变",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = data.evolutionNote,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FingerprintVisualization(
    avgSentenceLength: Float,
    vocabularyRichness: Float,
    punctuationRatio: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2 * 0.8f

        val rings = 10
        for (i in 1..rings) {
            val radius = maxRadius * i / rings
            val alpha = 0.1f + (i.toFloat() / rings) * 0.3f
            val distortion = (avgSentenceLength / 50f).coerceIn(0.5f, 1.5f)
            drawCircle(
                color = primaryColor.copy(alpha = alpha),
                radius = radius * distortion,
                center = Offset(centerX, centerY)
            )
        }

        val lines = 36
        for (i in 0 until lines) {
            val angle = i * 10 * Math.PI / 180
            val lengthVariation = vocabularyRichness * 50f + 30f
            val length = lengthVariation + (i % 5) * 15f
            val startX = centerX + 30f * cos(angle).toFloat()
            val startY = centerY + 30f * sin(angle).toFloat()
            val endX = centerX + (30f + length) * cos(angle).toFloat()
            val endY = centerY + (30f + length) * sin(angle).toFloat()
            drawLine(
                primaryColor.copy(alpha = 0.3f),
                Offset(startX, startY),
                Offset(endX, endY),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun FingerprintStatRow(label: String, value: String) {
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
