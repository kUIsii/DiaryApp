package com.diary.app.ui.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.Tag
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.isDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiaryDetailScreen(
    diaryId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val themeMode by app.themeMode.collectAsState()
    val isDark = themeMode.isDark()

    val viewModel: DiaryDetailViewModel = viewModel()
    val entry by viewModel.entry.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(diaryId) {
        viewModel.loadEntry(diaryId)
    }

    LaunchedEffect(themeMode) {
        webView?.evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = textSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val shareText = viewModel.getShareText()
                    if (shareText != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            putExtra(Intent.EXTRA_SUBJECT, "日记 - ${viewModel.getDateTitle()}")
                        }
                        context.startActivity(Intent.createChooser(intent, "分享日记"))
                    }
                }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onNavigateToEditor(diaryId) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "编辑",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            entry?.let { currentEntry ->
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: date, time, mood, weather
                    DetailHeader(
                        entry = currentEntry,
                        textColor = textColor,
                        textSecondary = textSecondary
                    )

                    // Tags
                    if (tags.isNotEmpty()) {
                        DetailTags(tags = tags)
                    }

                    // Content WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                setBackgroundColor(0)
                                loadUrl("file:///android_asset/viewer.html")
                                webView = this
                                post {
                                    evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
                                    if (currentEntry.content.isNotBlank()) {
                                        val escaped = currentEntry.content
                                            .replace("\\", "\\\\")
                                            .replace("'", "\\'")
                                            .replace("\n", "\\n")
                                            .replace("\r", "")
                                        evaluateJavascript("setContent('$escaped')", null)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )

                    // Bottom timestamps
                    DetailTimestamps(
                        createdAt = currentEntry.createdAt,
                        updatedAt = currentEntry.updatedAt,
                        textSecondary = textSecondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(
    entry: DiaryEntry,
    textColor: Color,
    textSecondary: Color
) {
    val entryDate = Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val entryTime = Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault()).toLocalTime()
    val dateText = "${entryDate.year}年${entryDate.monthValue}月${entryDate.dayOfMonth}日"
    val dayOfWeek = entryDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.CHINESE))
    val timeText = entryTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Date
        Text(
            text = dateText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$dayOfWeek $timeText",
                fontSize = 14.sp,
                color = textSecondary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Mood icon
            if (entry.moodLevel != null) {
                val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                val moodLabel = moodLabelForLevel(entry.moodLevel)
                Icon(
                    imageVector = moodIcon,
                    contentDescription = moodLabel,
                    tint = moodTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = moodLabel,
                    fontSize = 13.sp,
                    color = moodTint,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Weather icon
            if (entry.weather != null) {
                val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = entry.weather,
                    tint = weatherTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = entry.weather,
                    fontSize = 13.sp,
                    color = weatherTint,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTags(tags: List<Tag>) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            val tagColor = Color(tag.color)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(tagColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = tag.name,
                    fontSize = 13.sp,
                    color = tagColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailTimestamps(
    createdAt: Long,
    updatedAt: Long,
    textSecondary: Color
) {
    val createdText = formatFullTimestamp(createdAt)
    val updatedText = formatFullTimestamp(updatedAt)
    val isEdited = updatedAt - createdAt > 60_000 // More than 1 minute difference

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "创建于 $createdText",
            fontSize = 12.sp,
            color = textSecondary.copy(alpha = 0.6f)
        )
        if (isEdited) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "修改于 $updatedText",
                fontSize = 12.sp,
                color = textSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

private data class IconWithTint(val icon: ImageVector, val tint: Color)

private fun moodIconForLevel(level: Int): IconWithTint {
    return when (level.coerceIn(1, 6)) {
        1 -> IconWithTint(Icons.Default.MoodBad, Color(0xFFE57373))
        2 -> IconWithTint(Icons.Default.SentimentDissatisfied, Color(0xFFFFB74D))
        3 -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
        4 -> IconWithTint(Icons.Default.Mood, Color(0xFFAED581))
        5 -> IconWithTint(Icons.Default.SentimentSatisfied, Color(0xFF81C784))
        6 -> IconWithTint(Icons.Default.SentimentVerySatisfied, Color(0xFF4FC3F7))
        else -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
    }
}

private fun moodLabelForLevel(level: Int): String {
    return when (level.coerceIn(1, 6)) {
        1 -> "沮丧"
        2 -> "低落"
        3 -> "平静"
        4 -> "开心"
        5 -> "愉快"
        6 -> "兴奋"
        else -> "平静"
    }
}

private fun weatherIconFor(weather: String?): IconWithTint {
    return when (weather) {
        "晴", "晴天" -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
        "多云" -> IconWithTint(Icons.Default.Cloud, Color(0xFF90A4AE))
        "阴", "阴天" -> IconWithTint(Icons.Default.CloudQueue, Color(0xFF78909C))
        "雨", "雨天" -> IconWithTint(Icons.Default.Umbrella, Color(0xFF64B5F6))
        "雷", "雷暴" -> IconWithTint(Icons.Default.Thunderstorm, Color(0xFFBA68C8))
        "风", "大风" -> IconWithTint(Icons.Default.Air, Color(0xFF80CBC4))
        else -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
    }
}

private fun formatFullTimestamp(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINESE))
}
