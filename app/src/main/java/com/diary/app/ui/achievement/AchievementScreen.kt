package com.diary.app.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.Achievement
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

private fun iconForName(name: String): ImageVector = when (name) {
    "AutoFixHigh" -> Icons.Default.AutoFixHigh
    "HistoryEdu" -> Icons.Default.HistoryEdu
    "MilitaryTech" -> Icons.Default.MilitaryTech
    "Whatshot" -> Icons.Default.Whatshot
    "TrendingUp" -> Icons.Default.TrendingUp
    "DateRange" -> Icons.Default.DateRange
    "TextSnippet" -> Icons.Default.TextSnippet
    "AutoStories" -> Icons.Default.AutoStories
    "SentimentSatisfied" -> Icons.Default.SentimentSatisfied
    "Thunderstorm" -> Icons.Default.Thunderstorm
    "NightsStay" -> Icons.Default.NightsStay
    "LightMode" -> Icons.Default.LightMode
    "BookmarkAdded" -> Icons.Default.BookmarkAdded
    "CollectionsBookmark" -> Icons.Default.CollectionsBookmark
    "NewLabel" -> Icons.Default.NewLabel
    "PhotoLibrary" -> Icons.Default.PhotoLibrary
    else -> Icons.Default.Star
}

@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    onNavigateBack: () -> Unit
) {
    val achievements by viewModel.achievements.collectAsState()
    val unlockedCount by viewModel.unlockedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "成就",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(40.dp))
            }

            // Progress header
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已解锁",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$unlockedCount / $totalCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }

            // Achievement grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementCard(achievement = achievement)
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val isUnlocked = achievement.unlockedAt != null
    val progress = if (achievement.target > 0) {
        achievement.progress.toFloat() / achievement.target
    } else 0f

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        innerPadding = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForName(achievement.iconName),
                    contentDescription = achievement.name,
                    tint = if (isUnlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = achievement.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Description
            Text(
                text = achievement.description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            if (!isUnlocked) {
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${achievement.progress} / ${achievement.target}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    text = "已解锁",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
