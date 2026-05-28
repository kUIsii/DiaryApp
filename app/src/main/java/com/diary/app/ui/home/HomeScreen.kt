package com.diary.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToEditor: (Long?) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val entryDates by viewModel.entryDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "日记",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    CalendarView(
                        entryDates = entryDates,
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            viewModel.selectDate(if (date == selectedDate) null else date)
                        }
                    )
                }

                if (selectedDate != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedDate!!.monthValue}月${selectedDate!!.dayOfMonth}日的日记",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "查看全部",
                                fontSize = 13.sp,
                                color = onSurfaceVariant,
                                modifier = Modifier.clickable { viewModel.selectDate(null) }
                            )
                        }
                    }
                }

                if (entries.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(entries) { entry ->
                        DiaryCard(entry = entry, onClick = { onNavigateToEditor(entry.id) })
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp)
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(DarkAccentStart, DarkAccentEnd)))
                    .clickable { onNavigateToEditor(null) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "还没有日记", fontSize = 16.sp, color = onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "点击右下角 + 开始记录", fontSize = 13.sp, color = onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun DiaryCard(entry: DiaryEntry, onClick: () -> Unit) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column {
            Text(
                text = formatDate(entry.createdAt),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = onBackground
            )
            if (entry.plainText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.plainText,
                    fontSize = 14.sp,
                    color = onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
            if (entry.moodLevel != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val moodLabels = arrayOf("", "沮丧", "低落", "平静", "开心", "愉快", "兴奋")
                Text(
                    text = "心情 ${moodLabels[entry.moodLevel.coerceIn(1, 6)]}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.getDefault())
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter)
}
