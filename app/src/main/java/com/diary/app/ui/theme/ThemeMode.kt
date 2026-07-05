package com.diary.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeFamily(val key: String, val label: String) {
    BLUE("blue", "闆捐摑"),
    GREEN("green", "鑻旂豢"),
    CYAN("cyan", "娴锋疆"),
    ROSE("rose", "闄剁矇"),
    AMBER("amber", "娌欓噾"),
    CLAY("clay", "闄跺湡"),
    INK("ink", "澧ㄨ摑")
}

enum class ThemeMode(
    val label: String,
    val category: ThemeFamily
) {
    PURE_LIGHT("闆捐摑娴呰壊", ThemeFamily.BLUE),
    PURE_DARK("闆捐摑娣辫壊", ThemeFamily.BLUE),
    MOSS_GREEN_LIGHT("鑻旂豢娴呰壊", ThemeFamily.GREEN),
    MOSS_GREEN_DARK("鑻旂豢娣辫壊", ThemeFamily.GREEN),
    OCEAN_LIGHT("娴锋疆娴呰壊", ThemeFamily.CYAN),
    OCEAN_DARK("娴锋疆娣辫壊", ThemeFamily.CYAN),
    PETAL_LIGHT("闄剁矇娴呰壊", ThemeFamily.ROSE),
    PETAL_DARK("闄剁矇娣辫壊", ThemeFamily.ROSE),
    SAND_LIGHT("娌欓噾娴呰壊", ThemeFamily.AMBER),
    SAND_DARK("娌欓噾娣辫壊", ThemeFamily.AMBER),
    CLAY_LIGHT("闄跺湡娴呰壊", ThemeFamily.CLAY),
    CLAY_DARK("闄跺湡娣辫壊", ThemeFamily.CLAY),
    INK_LIGHT("澧ㄨ摑娴呰壊", ThemeFamily.INK),
    INK_DARK("澧ㄨ摑娣辫壊", ThemeFamily.INK);

    val familyKey: String
        get() = category.key
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
    ThemeMode.OCEAN_LIGHT -> false
    ThemeMode.OCEAN_DARK -> true
    ThemeMode.PETAL_LIGHT -> false
    ThemeMode.PETAL_DARK -> true
    ThemeMode.SAND_LIGHT -> false
    ThemeMode.SAND_DARK -> true
    ThemeMode.CLAY_LIGHT -> false
    ThemeMode.CLAY_DARK -> true
    ThemeMode.INK_LIGHT -> false
    ThemeMode.INK_DARK -> true
}

fun ThemeMode.isDarkStatic(): Boolean = when (this) {
    ThemeMode.PURE_LIGHT -> false
    ThemeMode.PURE_DARK -> true
    ThemeMode.MOSS_GREEN_LIGHT -> false
    ThemeMode.MOSS_GREEN_DARK -> true
    ThemeMode.OCEAN_LIGHT -> false
    ThemeMode.OCEAN_DARK -> true
    ThemeMode.PETAL_LIGHT -> false
    ThemeMode.PETAL_DARK -> true
    ThemeMode.SAND_LIGHT -> false
    ThemeMode.SAND_DARK -> true
    ThemeMode.CLAY_LIGHT -> false
    ThemeMode.CLAY_DARK -> true
    ThemeMode.INK_LIGHT -> false
    ThemeMode.INK_DARK -> true
}

fun resolveThemeModeName(rawName: String?): ThemeMode = when (rawName) {
    null, "" -> ThemeMode.PURE_LIGHT
    ThemeMode.PURE_LIGHT.name, "SYSTEM", "GRADIENT" -> ThemeMode.PURE_LIGHT
    ThemeMode.PURE_DARK.name -> ThemeMode.PURE_DARK
    ThemeMode.MOSS_GREEN_LIGHT.name, "MOSS_GREEN" -> ThemeMode.MOSS_GREEN_LIGHT
    ThemeMode.MOSS_GREEN_DARK.name -> ThemeMode.MOSS_GREEN_DARK
    ThemeMode.OCEAN_LIGHT.name, "OCEAN_BLUE" -> ThemeMode.OCEAN_LIGHT
    ThemeMode.OCEAN_DARK.name -> ThemeMode.OCEAN_DARK
    ThemeMode.PETAL_LIGHT.name, "WARM_ROSE" -> ThemeMode.PETAL_LIGHT
    ThemeMode.PETAL_DARK.name -> ThemeMode.PETAL_DARK
    ThemeMode.SAND_LIGHT.name -> ThemeMode.SAND_LIGHT
    ThemeMode.SAND_DARK.name -> ThemeMode.SAND_DARK
    ThemeMode.CLAY_LIGHT.name, "CLAY_PAPER" -> ThemeMode.CLAY_LIGHT
    ThemeMode.CLAY_DARK.name -> ThemeMode.CLAY_DARK
    ThemeMode.INK_LIGHT.name, "INK_SLATE" -> ThemeMode.INK_LIGHT
    ThemeMode.INK_DARK.name -> ThemeMode.INK_DARK
    else -> runCatching { ThemeMode.valueOf(rawName) }.getOrDefault(ThemeMode.PURE_LIGHT)
}

fun resolveThemeModeFamily(rawName: String?): ThemeFamily = resolveThemeModeName(rawName).category
