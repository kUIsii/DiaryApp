package com.diary.app.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
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
import com.diary.app.BuildConfig
import com.diary.app.DiaryApplication
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import kotlinx.coroutines.delay

private val AppearanceIconBg = Color(0x1A9C27B0)
private val AppearanceIconTint = Color(0xFF9C27B0)
private val DataIconBg = Color(0x1A2196F3)
private val DataIconTint = Color(0xFF2196F3)
private val PrivacyIconBg = Color(0x1AF44336)
private val PrivacyIconTint = Color(0xFFF44336)
private val AboutIconBg = Color(0x1A4CAF50)
private val AboutIconTint = Color(0xFF4CAF50)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val currentThemeMode by app.themeMode.collectAsState()

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = "设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SettingsStaggeredItem(index = 0, showContent = showContent) {
                    SettingsSectionHeader(title = "外观", icon = Icons.Default.Palette, color = AppearanceIconTint)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsStaggeredItem(index = 1, showContent = showContent) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        SettingsNavigateItem(
                            icon = Icons.Default.Palette,
                            title = "主题设置",
                            subtitle = "当前: ${currentThemeMode.label}",
                            iconBg = AppearanceIconBg,
                            iconTint = AppearanceIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsStaggeredItem(index = 2, showContent = showContent) {
                    SettingsSectionHeader(title = "数据管理", icon = Icons.Default.Backup, color = DataIconTint)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsStaggeredItem(index = 3, showContent = showContent) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Column {
                            SettingsNavigateItem(
                                icon = Icons.Default.Backup,
                                title = "备份管理",
                                subtitle = "自动备份、备份历史、恢复数据",
                                iconBg = DataIconBg,
                                iconTint = DataIconTint,
                                textColor = textColor,
                                textTertiary = textTertiary,
                                onClick = onNavigateToBackup
                            )
                            SettingsDivider()
                            SettingsNavigateItem(
                                icon = Icons.Default.Label,
                                title = "分类管理",
                                subtitle = "管理日记分类标签",
                                iconBg = DataIconBg,
                                iconTint = DataIconTint,
                                textColor = textColor,
                                textTertiary = textTertiary,
                                onClick = onNavigateToTagManagement
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsStaggeredItem(index = 4, showContent = showContent) {
                    SettingsSectionHeader(title = "隐私与安全", icon = Icons.Default.Security, color = PrivacyIconTint)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsStaggeredItem(index = 5, showContent = showContent) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        SettingsNavigateItem(
                            icon = Icons.Default.Lock,
                            title = "应用锁",
                            subtitle = "指纹、面部识别或PIN码保护",
                            iconBg = PrivacyIconBg,
                            iconTint = PrivacyIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsStaggeredItem(index = 6, showContent = showContent) {
                    SettingsSectionHeader(title = "关于", icon = Icons.Default.Info, color = AboutIconTint)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsStaggeredItem(index = 7, showContent = showContent) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(DarkAccentStart, DarkAccentEnd)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "日记本",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            SettingsNavigateItem(
                                icon = Icons.Default.SystemUpdate,
                                title = "检查更新",
                                subtitle = "检查是否有新版本",
                                iconBg = AboutIconBg,
                                iconTint = AboutIconTint,
                                textColor = textColor,
                                textTertiary = textTertiary
                            )
                            SettingsDivider()
                            SettingsNavigateItem(
                                icon = Icons.Default.History,
                                title = "更新日志",
                                subtitle = "查看历史版本记录",
                                iconBg = AboutIconBg,
                                iconTint = AboutIconTint,
                                textColor = textColor,
                                textTertiary = textTertiary,
                                onClick = onNavigateToChangelog
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "Made with ", fontSize = 12.sp, color = textTertiary)
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "心形图标",
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(text = " by Diary Team", fontSize = 12.sp, color = textTertiary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun SettingsNavigateItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    textColor: Color,
    textTertiary: Color,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "navScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null
            ) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, color = textColor)
            Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp))
        }
        if (onClick != null) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "进入",
                tint = textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    )
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .clip(RoundedCornerShape(0.5.dp))
                .background(color.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun SettingsStaggeredItem(
    index: Int,
    showContent: Boolean,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(showContent) {
        if (showContent) {
            delay(index * 60L)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "settingsStaggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(300),
        label = "settingsStaggerOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        content()
    }
}
