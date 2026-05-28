package com.diary.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 深色主题配色 - 参考截图
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),      // 浅蓝色 - 主色调
    onPrimary = Color(0xFF1A1A1A),    // 深色文字
    primaryContainer = Color(0xFF1E1E1E),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF757575),    // 灰色 - 次要元素
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFBDBDBD),
    tertiary = Color(0xFFE0E0E0),    // 浅灰色 - 强调
    onTertiary = Color(0xFF1A1A1A),
    background = Color(0xFF121212),   // 深色背景
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),      // 卡片背景
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFCF6679),
    onError = Color(0xFF1A1A1A),
)

// 浅色主题配色
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),      // 蓝色
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF757575),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF424242),
    tertiary = Color(0xFF616161),
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF616161),
    error = Color(0xFFB00020),
    onError = Color.White,
)

@Composable
fun DiaryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
