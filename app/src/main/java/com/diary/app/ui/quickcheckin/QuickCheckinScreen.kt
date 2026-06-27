package com.diary.app.ui.quickcheckin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCheckinScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuickCheckinViewModel = viewModel()
) {
    val selectedMood by viewModel.selectedMood.collectAsState()
    val text by viewModel.text.collectAsState()

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
                Text(
                    text = "快速签到",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mood selector
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "现在的心情",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val moods = listOf(
                            1 to "😢",
                            2 to "😕",
                            3 to "😐",
                            4 to "😊",
                            5 to "😄",
                            6 to "🤩"
                        )
                        moods.forEach { (level, emoji) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 32.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedMood == level)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text input
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "一句话记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = viewModel::setText,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("今天发生了什么...") },
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit button
            Button(
                onClick = {
                    viewModel.submit()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedMood != null || text.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("完成签到")
            }
        }
    }
}
