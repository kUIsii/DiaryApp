package com.diary.app.ui.writinglab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WritingLabScreen(onNavigateBack: () -> Unit, viewModel: WritingLabViewModel = viewModel()) {
    val activeExperiment by viewModel.activeExperiment.collectAsState()
    val participations by viewModel.participations.collectAsState()
    val completedExperiments by viewModel.completedExperiments.collectAsState()
    val showPicker by viewModel.showPresetPicker.collectAsState()
    var inputText by remember { mutableStateOf("") }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "写作工坊", onNavigateBack = onNavigateBack)
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showPicker) {
                    Text("选择一个实验开始", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                    experimentPresets.forEach { preset ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(preset.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(preset.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${preset.days}天 · ${preset.rules}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = { viewModel.startExperiment(preset) },
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("开始实验") }
                            }
                        }
                    }
                }

                activeExperiment?.let { exp ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("本周实验", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(exp.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(exp.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(exp.rules, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        val totalDays = experimentPresets.find { it.title == exp.title }?.days ?: 7
                        val progress = participations.size.toFloat() / totalDays.toFloat()
                        
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val strokeWidth = 8.dp.toPx()
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                drawArc(
                                    color = Color.Gray.copy(alpha = 0.2f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = primaryColor,
                                    startAngle = -90f,
                                    sweepAngle = progress * 360f,
                                    useCenter = false,
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("已完成 ${participations.size}/${totalDays} 天", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${(progress * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        val hasWrittenToday = participations.any { p ->
                            val cal = Calendar.getInstance()
                            val partCal = Calendar.getInstance().apply { timeInMillis = p.completedAt }
                            cal.get(Calendar.DAY_OF_YEAR) == partCal.get(Calendar.DAY_OF_YEAR) &&
                            cal.get(Calendar.YEAR) == partCal.get(Calendar.YEAR)
                        }
                        if (!hasWrittenToday && participations.size < totalDays) {
                            Text("今天还没有写作，记得来完成今日记录",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                        }
                        if (participations.size < totalDays) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = inputText, onValueChange = { inputText = it },
                                placeholder = { Text("写下今天的实验记录...") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(12.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { if (inputText.isNotBlank()) { viewModel.logParticipation(inputText.trim()); inputText = "" } },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("提交第${participations.size + 1}天记录") }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            val totalChars = participations.sumOf { it.note.length }
                            val firstDate = remember(participations) {
                                SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(participations.firstOrNull()?.completedAt ?: exp.startDate))
                            }
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                innerPadding = 20.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFFFFD700)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("恭喜完成实验！", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("获得「${exp.badgeName}」徽章", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("实验统计", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("写作天数：${participations.size} 天", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("总字数：${totalChars} 字", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("开始日期：$firstDate", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (completedExperiments.isNotEmpty()) {
                    Text("过往实验", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    completedExperiments.forEach { exp ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(exp.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("已完成", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
