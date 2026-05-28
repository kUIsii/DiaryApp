package com.diary.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.diary.app.ui.navigation.DiaryNavHost
import com.diary.app.ui.theme.DiaryAppTheme
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaryAppTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateVersion by remember { mutableStateOf("") }
                var updateNotes by remember { mutableStateOf("") }
                var updateUrl by remember { mutableStateOf("") }
                var isDownloading by remember { mutableStateOf(false) }
                var hasChecked by remember { mutableStateOf(false) }

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

                LaunchedEffect(Unit) {
                    if (!hasChecked) {
                        hasChecked = true
                        val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                        if (result != null) {
                            updateVersion = result.versionName
                            updateNotes = result.releaseNotes
                            updateUrl = result.downloadUrl
                            showUpdateDialog = true
                        }
                    }
                }

                DiaryNavHost()
            }
        }
    }
}
