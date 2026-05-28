package com.diary.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.DarkTextPrimary
import com.diary.app.ui.theme.DarkTextSecondary
import com.diary.app.ui.theme.DarkTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToEditor: (Long?) -> Unit
) {
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
                        color = DarkTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    EmptyState()
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "还没有日记",
                fontSize = 16.sp,
                color = DarkTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角 + 开始记录",
                fontSize = 13.sp,
                color = DarkTextTertiary
            )
        }
    }
}

@Composable
private fun DiaryCard(
    entry: DiaryEntry,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
    ) {
        Column {
            Text(
                text = formatDate(entry.createdAt),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPrimary
            )

            if (entry.plainText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.plainText,
                    fontSize = 14.sp,
                    color = DarkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            if (entry.mood != null) {
                Spacer(modifier = Modifier.height(10.dp))
                MoodTag(mood = entry.mood)
            }
        }
    }
}

@Composable
private fun MoodTag(mood: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 6.dp)
        ) {
            Text(
                text = mood,
                fontSize = 11.sp,
                color = DarkAccentStart,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
