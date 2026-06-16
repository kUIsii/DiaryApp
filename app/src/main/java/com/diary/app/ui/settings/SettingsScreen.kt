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
import com.diary.app.ui.components.SectionHeader
import com.diary.app.ui.components.SettingDivider
import androidx.compose.ui.res.stringResource
import com.diary.app.R
import kotlinx.coroutines.delay

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
                        .size(48.dp)
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
                    text = stringResource(R.string.settings_title),
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
                    SectionHeader(title = stringResource(R.string.settings_appearance), icon = Icons.Default.Palette, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsStaggeredItem(index = 1, showContent = showContent) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        SettingsNavigateItem(
                            icon = Icons.Default.Palette,
                            title = stringResource(R.string.settings_theme),
                            subtitle = stringResource(R.string.settings_current_theme, currentThemeMode.label),
                            iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.primary,
                            textColor = textColor,
                            textTertiary = textTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsStaggeredItem(index = 2, showContent = showContent) {
                    SectionHeader(title = stringResource(R.string.settings_data), icon = Icons.Default.Backup, color = MaterialTheme.colorScheme.secondary)
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
                                title = stringResource(R.string.settings_backup),
                                subtitle = stringResource(R.string.settings_backup_desc),
                                iconBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.secondary,
                                textColor = textColor,
                                textTertiary = textTertiary,
                                onClick = onNavigateToBackup
                            )
                            SettingDivider()
                            SettingsNavigateItem(
                                icon = Icons.Default.Label,
                                title = stringResource(R.string.settings_tags),
                                subtitle = stringResource(R.string.settings_tags_desc),
                                iconBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.secondary,
                                textColor = textColor,
                                textTertiary = textTertiary,
                                onClick = onNavigateToTagManagement
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsStaggeredItem(index = 4, showContent = showContent) {
                    SectionHeader(title = stringResource(R.string.settings_privacy), icon = Icons.Default.Security, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsStaggeredItem(index = 5, showContent = showContent) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        SettingsNavigateItem(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.settings_app_lock),
                            subtitle = stringResource(R.string.settings_app_lock_desc),
                            iconBg = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.error,
                            textColor = textColor,
                            textTertiary = textTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsStaggeredItem(index = 6, showContent = showContent) {
                    SectionHeader(title = stringResource(R.string.settings_about), icon = Icons.Default.Info, color = MaterialTheme.colorScheme.primary)
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
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
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
                                title = stringResource(R.string.settings_check_update),
                                subtitle = stringResource(R.string.settings_check_update_desc),
                                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.primary,
                                textColor = textColor,
                                textTertiary = textTertiary
                            )
                            SettingDivider()
                            SettingsNavigateItem(
                                icon = Icons.Default.History,
                                title = stringResource(R.string.settings_changelog),
                                subtitle = stringResource(R.string.settings_changelog_desc),
                                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                iconTint = MaterialTheme.colorScheme.primary,
                                textColor = textColor,
                                textTertiary = textTertiary,
                                onClick = onNavigateToChangelog
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = stringResource(R.string.made_with_love), fontSize = 12.sp, color = textTertiary)
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "心形图标",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(text = stringResource(R.string.made_by), fontSize = 12.sp, color = textTertiary)
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
                .clip(RoundedCornerShape(12.dp))
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
