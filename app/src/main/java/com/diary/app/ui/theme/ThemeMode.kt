package com.diary.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode(val label: String, val category: String = "blue") {
    PURE_LIGHT("浅色", "blue"),
    PURE_DARK("深色", "blue"),
    MOSS_GREEN_LIGHT("浅色", "green"),
    MOSS_GREEN_DARK("深色", "green")
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.PURE_LIGHT }

@Composable
fun themeMode(): ThemeMode = LocalThemeMode.current

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.PURE_LIGHT -> false
    ThemeMode.PURE_DARK -> true
    ThemeMode.MOSS_GREEN_LIGHT -> false
    ThemeMode.MOSS_GREEN_DARK -> true
}

fun ThemeMode.isDarkStatic(): Boolean = when (this) {
    ThemeMode.PURE_LIGHT -> false
    ThemeMode.PURE_DARK -> true
    ThemeMode.MOSS_GREEN_LIGHT -> false
    ThemeMode.MOSS_GREEN_DARK -> true
}
