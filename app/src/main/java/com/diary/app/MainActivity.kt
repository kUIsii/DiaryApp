package com.diary.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.biometric.BiometricHelper
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.navigation.DiaryNavHost
import com.diary.app.ui.theme.DiaryAppTheme
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateDialog
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val navigateTo = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        navigateTo.value = intent?.getStringExtra("navigate_to")
        val app = application as DiaryApplication
        setContent {
            val themeMode by app.themeMode.collectAsState()

            DiaryAppTheme(themeMode = themeMode) {
                val context = LocalContext.current
                val activity = this@MainActivity
                val scope = rememberCoroutineScope()
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateVersion by remember { mutableStateOf("") }
                var updateNotes by remember { mutableStateOf("") }
                var updateUrl by remember { mutableStateOf("") }
                var isDownloading by remember { mutableStateOf(false) }
                var hasChecked by remember { mutableStateOf(false) }
                val pendingNavigation by navigateTo

                val lockEnabled = BiometricHelper.isLockEnabled(context)
                var isAuthenticated by remember { mutableStateOf(!lockEnabled) }

                if (!isAuthenticated) {
                    GradientBackground {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "日记本",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "请验证身份以继续",
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(40.dp))

                            Button(
                                onClick = {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = { isAuthenticated = true },
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = "解锁", fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    if (showUpdateDialog) {
                        UpdateDialog(
                            versionName = updateVersion,
                            releaseNotes = updateNotes,
                            isDownloading = isDownloading,
                            onConfirm = {
                                isDownloading = true
                                val fileName = "DiaryApp-v$updateVersion.apk"
                                scope.launch {
                                    try {
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
                                    } catch (e: Exception) {
                                        isDownloading = false
                                        showUpdateDialog = false
                                        Toast.makeText(context, "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDismiss = { showUpdateDialog = false }
                        )
                    }

                    LaunchedEffect(Unit) {
                        if (!hasChecked) {
                            hasChecked = true
                            try {
                                val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                                if (result != null) {
                                    updateVersion = result.versionName
                                    updateNotes = result.releaseNotes
                                    updateUrl = result.downloadUrl
                                    showUpdateDialog = true
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }

                    DiaryNavHost(
                        navigateTo = pendingNavigation,
                        onNavigateHandled = { navigateTo.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigateTo.value = intent.getStringExtra("navigate_to")
    }
}
