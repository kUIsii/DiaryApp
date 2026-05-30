package com.diary.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode(val label: String) {
    PURE_LIGHT("浅色模式"),
    PURE_DARK("深色模式")
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.PURE_LIGHT }

@Composable
fun themeMode(): ThemeMode = LocalThemeMode.current

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.PURE_LIGHT -> false
    ThemeMode.PURE_DARK -> true
}
