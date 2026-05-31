package com.diary.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.biometric.BiometricHelper
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.lock.PinEntryScreen
import com.diary.app.ui.navigation.DiaryNavHost
import com.diary.app.ui.theme.DiaryAppTheme
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
        navigateTo.value = resolveNavigateTo(intent)
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

                val biometricLockEnabled = BiometricHelper.isBiometricLockEnabled(context)
                val pinLockEnabled = BiometricHelper.isPinLockEnabled(context)
                val lockEnabled = biometricLockEnabled || pinLockEnabled
                var isAuthenticated by remember { mutableStateOf(!lockEnabled) }
                // If both are enabled, default to PIN; otherwise use whichever is set
                var showPinScreen by remember { mutableStateOf(pinLockEnabled) }

                if (!isAuthenticated) {
                    GradientBackground {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (showPinScreen) {
                                // PIN entry screen with enhanced features
                                var pinError by remember { mutableStateOf(false) }

                                PinEntryScreen(
                                    title = "输入PIN码",
                                    subtitle = if (biometricLockEnabled) "或切换到生物识别" else "",
                                    hint = BiometricHelper.getPinHint(context),
                                    lockoutSeconds = BiometricHelper.getLockoutRemainingSeconds(context),
                                    onPinEntered = { pin ->
                                        if (BiometricHelper.verifyPin(context, pin)) {
                                            isAuthenticated = true
                                            pinError = false
                                        } else {
                                            pinError = true
                                            if (BiometricHelper.isLockedOut(context)) {
                                                Toast.makeText(context, "输入错误次数过多，请等待", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onBiometricClick = if (biometricLockEnabled) {
                                        { showPinScreen = false }
                                    } else null
                                )

                                // Version info at the bottom
                                val versionColor = if (themeMode == com.diary.app.ui.theme.ThemeMode.PURE_DARK) Color.White.copy(alpha = 0.5f) else Color(0xFF1A1A1A).copy(alpha = 0.5f)
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    fontSize = 11.sp,
                                    color = versionColor,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 32.dp)
                                )
                            } else {
                                // Biometric unlock screen (original)
                                // Entrance animation state
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { visible = true }

                                // Breathing animation for the lock icon
                                val infiniteTransition = rememberInfiniteTransition(label = "breathing")
                                val breathScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "breathScale"
                                )
                                // Entrance fade + scale
                                val entranceAlpha by animateFloatAsState(
                                    targetValue = if (visible) 1f else 0f,
                                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                    label = "entranceAlpha"
                                )
                                val entranceScale by animateFloatAsState(
                                    targetValue = if (visible) 1f else 0.92f,
                                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                    label = "entranceScale"
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(entranceAlpha)
                                        .scale(entranceScale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Breathing lock icon
                                    val biometricTextColor = if (themeMode == com.diary.app.ui.theme.ThemeMode.PURE_DARK) Color.White else Color(0xFF1A1A1A)
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = biometricTextColor,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .scale(breathScale)
                                    )

                                    Spacer(modifier = Modifier.height(28.dp))

                                    // App name
                                    Text(
                                        text = "日记本",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = biometricTextColor,
                                        letterSpacing = 4.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Subtitle
                                    Text(
                                        text = "记录生活的每一刻",
                                        fontSize = 14.sp,
                                        color = biometricTextColor.copy(alpha = 0.55f),
                                        letterSpacing = 2.sp
                                    )

                                    Spacer(modifier = Modifier.height(52.dp))

                                    // Gradient-bordered unlock button
                                    val buttonShape = RoundedCornerShape(20.dp)
                                    val gradientBrush = Brush.linearGradient(
                                        colors = listOf(
                                            biometricTextColor.copy(alpha = 0.6f),
                                            biometricTextColor.copy(alpha = 0.25f)
                                        )
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            BiometricHelper.showBiometricPrompt(
                                                activity = activity,
                                                onSuccess = { isAuthenticated = true },
                                                onError = { msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        shape = buttonShape,
                                        border = BorderStroke(1.dp, gradientBrush),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = biometricTextColor.copy(alpha = 0.12f),
                                            contentColor = biometricTextColor
                                        ),
                                        modifier = Modifier
                                            .padding(horizontal = 48.dp)
                                            .height(52.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.size(10.dp))
                                        Text(
                                            text = "解锁",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                // Switch to PIN button (only if PIN is also enabled)
                                if (pinLockEnabled) {
                                    val switchPinColor = if (themeMode == com.diary.app.ui.theme.ThemeMode.PURE_DARK) Color.White.copy(alpha = 0.5f) else Color(0xFF1A1A1A).copy(alpha = 0.5f)
                                    Text(
                                        text = "使用PIN码",
                                        fontSize = 13.sp,
                                        color = switchPinColor,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 64.dp)
                                            .clickable { showPinScreen = true }
                                    )
                                }

                                // Version info at the bottom
                                val biometricVersionColor = if (themeMode == com.diary.app.ui.theme.ThemeMode.PURE_DARK) Color.White.copy(alpha = 0.5f) else Color(0xFF1A1A1A).copy(alpha = 0.5f)
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    fontSize = 11.sp,
                                    color = biometricVersionColor,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 32.dp)
                                )
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
        navigateTo.value = resolveNavigateTo(intent)
    }

    private fun resolveNavigateTo(intent: Intent?): String? {
        if (intent?.action == "com.diary.app.NEW_DIARY") return "editor"
        if (intent?.action == "com.diary.app.QUICK_TODO") return "todo"
        return intent?.getStringExtra("navigate_to")
    }
}
