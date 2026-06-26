package com.diary.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.util.StreakTier
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WritingPromptCard(
    state: HomeNewState,
    onShufflePrompt: () -> Unit,
    onCreateEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = state.greeting
    val streak = state.streakInfo
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = greeting.text + " " + greeting.emoji, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("今日写作提示", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = state.writingPrompt, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("点击开始写作", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onCreateEntry() }.padding(horizontal = 4.dp, vertical = 2.dp))
                    IconButton(onClick = onShufflePrompt, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, contentDescription = "换一个", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (streak.current > 0 || streak.longest > 0) { StreakInfoBar(streak = streak) }
    }
}

@Composable
private fun StreakInfoBar(streak: HomeStreakInfo, modifier: Modifier = Modifier) {
    val tierColor = when (streak.tier) { StreakTier.LEGENDARY -> Color(0xFFFFD700); StreakTier.DIAMOND -> Color(0xFF00BCD4); StreakTier.GOLD -> Color(0xFFFF9800); StreakTier.SILVER -> Color(0xFF90A4AE); StreakTier.BRONZE -> Color(0xFF795548); StreakTier.NONE -> MaterialTheme.colorScheme.primary }
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = tierColor, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(6.dp)); Column { Text(streak.current.toString() + " 天", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tierColor); Text("当前连续", fontSize = 11.sp, color = onSurface) } }
            if (streak.milestone != null) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(streak.milestone.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("下个里程碑", fontSize = 11.sp, color = onSurface) } }
            if (streak.longest > 0) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(streak.longest.toString() + " 天", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800)); Text("最长连续", fontSize = 11.sp, color = onSurface) } }
            if (streak.monthlyBest > 0) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(streak.monthlyBest.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary); Text("本月最佳", fontSize = 11.sp, color = onSurface) } }
        }
    }
}