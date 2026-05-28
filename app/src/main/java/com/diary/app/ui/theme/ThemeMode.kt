package com.diary.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    PURE_LIGHT("纯白模式"),
    PURE_DARK("纯黑模式"),
    GRADIENT("渐变模式")
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

@Composable
fun themeMode(): ThemeMode = LocalThemeMode.current

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.PURE_LIGHT -> false
    ThemeMode.PURE_DARK -> true
    ThemeMode.GRADIENT -> isSystemInDarkTheme()
}

@Composable
fun isGradientMode(): Boolean = themeMode() == ThemeMode.GRADIENT
