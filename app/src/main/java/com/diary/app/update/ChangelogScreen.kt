package com.diary.app.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.BuildConfig
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.InfoColor
import com.diary.app.ui.theme.SuccessColor
import com.diary.app.ui.theme.WarningColor
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "· ")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("~~(.+?)~~"), "$1")
        .replace(Regex("`(.+?)`"), "$1")
        .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1")
        .replace(Regex("^\\s*\\n", RegexOption.MULTILINE), "\n")
        .trim()
}

private data class ParsedLine(val text: String, val isHeader: Boolean)

private fun parseBodyLines(body: String): List<ParsedLine> {
    return body.lines().map { line ->
        val trimmed = line.trim()
        when {
            trimmed.matches(Regex("^#{1,6}\\s+.*")) -> {
                val headerText = trimmed.replace(Regex("^#{1,6}\\s+"), "")
                ParsedLine(headerText, true)
            }
            else -> ParsedLine(stripMarkdown(trimmed), false)
        }
    }.filter { it.text.isNotBlank() }
}

private fun categoryIconForHeader(header: String): Pair<ImageVector, Color> {
    val lower = header.lowercase()
    return when {
        lower.contains("feature") || lower.contains("new") || lower.contains("新增") || lower.contains("功能") ->
            Icons.Default.AutoAwesome to SuccessColor
        lower.contains("fix") || lower.contains("bug") || lower.contains("修复") || lower.contains("fixed") ->
            Icons.Default.BugReport to ErrorColor
        lower.contains("improve") || lower.contains("enhance") || lower.contains("优化") || lower.contains("update") ->
            Icons.Default.Update to InfoColor
        lower.contains("breaking") || lower.contains("重大") || lower.contains("important") ->
            Icons.Default.NewReleases to WarningColor
        else -> Icons.Default.AutoAwesome to InfoColor
    }
}

data class ChangelogRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String?,
    val body: String?,
    @SerializedName("published_at") val publishedAt: String?
)

@Composable
fun ChangelogScreen(onNavigateBack: () -> Unit) {
    var releases by remember { mutableStateOf<List<ChangelogRelease>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().readText()
                    releases = Gson().fromJson(json, Array<ChangelogRelease>::class.java).toList()
                } else {
                    error = "加载失败"
                }
            } catch (e: Exception) {
                error = "网络连接失败"
            }
            isLoading = false
        }
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
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "更新日志",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> ChangelogLoadingState(textSecondary)
                error != null -> ChangelogErrorState(error!!, textSecondary)
                releases.isEmpty() -> ChangelogEmptyState(textColor, textSecondary)
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                        itemsIndexed(releases) { index, release ->
                            AnimatedReleaseItem(
                                index = index,
                                release = release,
                                textColor = textColor,
                                textSecondary = textSecondary
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedReleaseItem(
    index: Int,
    release: ChangelogRelease,
    textColor: Color,
    textSecondary: Color
) {
    var visible by remember { mutableStateOf(false) }
    val delayMs = (index * 50L).coerceAtMost(500L)

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    animationSpec = tween(400),
                    initialOffsetY = { it / 5 }
                )
    ) {
        ReleaseItem(release, textColor, textSecondary)
    }
}

@Composable
private fun ReleaseItem(
    release: ChangelogRelease,
    textColor: Color,
    textSecondary: Color
) {
    val version = release.tagName.removePrefix("v")
    val isCurrent = version == BuildConfig.VERSION_NAME
    val dateStr = release.publishedAt?.take(10) ?: ""

    GlassCard(
        cornerRadius = 18.dp,
        innerPadding = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Top accent strip
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(DarkAccentStart, DarkAccentEnd)
                            )
                        )
                )
            }

            Column(modifier = Modifier.padding(18.dp)) {
                // Version badge + date row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Version badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(DarkAccentStart, DarkAccentEnd)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "v$version",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "当前版本",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (dateStr.isNotBlank()) {
                        Text(
                            text = dateStr,
                            fontSize = 12.sp,
                            color = textSecondary.copy(alpha = 0.7f)
                        )
                    }
                }

                // Release title
                val releaseTitle = release.name?.takeIf { it.isNotBlank() && it != "v$version" }
                if (releaseTitle != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = releaseTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }

                // Parsed body lines
                if (!release.body.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val parsedLines = parseBodyLines(release.body)
                    parsedLines.forEach { parsedLine ->
                        if (parsedLine.isHeader) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CategoryHeaderRow(parsedLine.text)
                        } else {
                            BulletRow(parsedLine.text, textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeaderRow(text: String) {
    val (icon, tint) = categoryIconForHeader(text)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun BulletRow(text: String, textSecondary: Color) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Text(
            text = "\u00B7",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = textSecondary,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun ChangelogLoadingState(textSecondary: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载更新历史...",
            fontSize = 14.sp,
            color = textSecondary
        )
    }
}

@Composable
private fun ChangelogEmptyState(textColor: Color, textSecondary: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "暂无更新记录",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "新版本发布后会在这里显示",
            fontSize = 14.sp,
            color = textSecondary
        )
    }
}

@Composable
private fun ChangelogErrorState(error: String, textSecondary: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        GlassCard(
            cornerRadius = 16.dp,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = ErrorColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
