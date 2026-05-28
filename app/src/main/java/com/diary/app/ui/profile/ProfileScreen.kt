package com.diary.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.BuildConfig
import com.diary.app.DiaryApplication
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateDialog
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.DarkTextPrimary
import com.diary.app.ui.theme.DarkTextSecondary
import com.diary.app.ui.theme.DarkTextTertiary
import com.diary.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val currentThemeMode by app.themeMode.collectAsState()
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf("") }
    var updateNotes by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    if (showUpdateDialog) {
        UpdateDialog(
            versionName = updateVersion,
            releaseNotes = updateNotes,
            isDownloading = isDownloading,
            onConfirm = {
                isDownloading = true
                val fileName = "DiaryApp-v$updateVersion.apk"
                scope.launch {
                    ApkInstaller.downloadAndInstall(context, updateUrl, fileName)
                        .collect { state ->
                            when (state) {
                                is DownloadState.Completed -> {
                                    isDownloading = false
                                    showUpdateDialog = false
                                }
                                is DownloadState.Failed -> {
                                    isDownloading = false
                                    showUpdateDialog = false
                                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                }
            },
            onDismiss = { showUpdateDialog = false }
        )
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "我的",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Theme settings
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeMenu = !showThemeMenu }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "主题设置",
                            fontSize = 16.sp,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = currentThemeMode.label,
                            fontSize = 14.sp,
                            color = DarkTextTertiary
                        )
                    }

                    if (showThemeMenu) {
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        app.setThemeMode(mode)
                                        showThemeMenu = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (currentThemeMode == mode) DarkAccentStart
                                            else DarkTextTertiary.copy(alpha = 0.3f)
                                        )
                                )
                                Text(
                                    text = mode.label,
                                    fontSize = 14.sp,
                                    color = DarkTextSecondary,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check for updates
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isChecking) {
                                isChecking = true
                                scope.launch {
                                    val result = UpdateChecker.checkForUpdate(
                                        BuildConfig.VERSION_NAME
                                    )
                                    isChecking = false
                                    if (result != null) {
                                        updateVersion = result.versionName
                                        updateNotes = result.releaseNotes
                                        updateUrl = result.downloadUrl
                                        showUpdateDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "已是最新版本",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "检查更新",
                        fontSize = 16.sp,
                        color = DarkTextPrimary
                    )
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = DarkAccentStart,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    } else {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            fontSize = 14.sp,
                            color = DarkTextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "版本 ${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = DarkTextTertiary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
