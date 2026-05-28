package com.diary.app.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.BuildConfig
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("~~(.+?)~~"), "$1")
        .replace(Regex("`(.+?)`"), "$1")
        .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1")
        .replace(Regex("^\\s*\\n", RegexOption.MULTILINE), "\n")
        .trim()
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
    val accent = MaterialTheme.colorScheme.primary

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
                Text(
                    text = "更新日志",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(80.dp))
                        CircularProgressIndicator(color = accent)
                        Text(
                            text = "加载中...",
                            fontSize = 14.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(80.dp))
                        Text(text = error!!, fontSize = 14.sp, color = textSecondary)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(releases) { release ->
                            ReleaseItem(release, textColor, textSecondary, accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseItem(
    release: ChangelogRelease,
    textColor: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color
) {
    val version = release.tagName.removePrefix("v")
    val isCurrent = version == BuildConfig.VERSION_NAME
    val dateStr = release.publishedAt?.take(10) ?: ""

    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v$version",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前版本",
                        fontSize = 11.sp,
                        color = accent,
                        modifier = Modifier
                            .padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (dateStr.isNotBlank()) {
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            if (release.name.isNullOrBlank() || release.name == "v$version") {
                // No extra title
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = release.name,
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            if (!release.body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stripMarkdown(release.body),
                    fontSize = 13.sp,
                    color = textSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
