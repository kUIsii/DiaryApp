package com.diary.app.ui.login

import androidx.compose.ui.graphics.Color
import com.diary.app.ui.theme.ThemeFamily

enum class DecorElement {
    NONE, HORIZONTAL_LINES, DOTS, WAVES, ELLIPSES, GRAIN, CROSS_HATCH, GRID_DOTS
}

data class LoginThemeSpec(
    val inputAccent: Color,
    val inputGlow: Color,
    val buttonStart: Color,
    val buttonEnd: Color,
    val buttonPressedScale: Float,
    val decorElement: DecorElement
)

fun loginThemeSpec(family: ThemeFamily): LoginThemeSpec = when (family) {
    ThemeFamily.BLUE -> LoginThemeSpec(
        inputAccent = Color(0xFF60A5FA),
        inputGlow = Color(0xFF3B82F6).copy(alpha = 0.15f),
        buttonStart = Color(0xFF3B82F6),
        buttonEnd = Color(0xFF1D4ED8),
        buttonPressedScale = 0.97f,
        decorElement = DecorElement.HORIZONTAL_LINES
    )
    ThemeFamily.GREEN -> LoginThemeSpec(
        inputAccent = Color(0xFF4ADE80),
        inputGlow = Color(0xFF22C55E).copy(alpha = 0.15f),
        buttonStart = Color(0xFF22C55E),
        buttonEnd = Color(0xFF15803D),
        buttonPressedScale = 0.96f,
        decorElement = DecorElement.DOTS
    )
    ThemeFamily.CYAN -> LoginThemeSpec(
        inputAccent = Color(0xFF22D3EE),
        inputGlow = Color(0xFF06B6D4).copy(alpha = 0.15f),
        buttonStart = Color(0xFF06B6D4),
        buttonEnd = Color(0xFF0891B2),
        buttonPressedScale = 0.97f,
        decorElement = DecorElement.WAVES
    )
    ThemeFamily.ROSE -> LoginThemeSpec(
        inputAccent = Color(0xFFFB7185),
        inputGlow = Color(0xFFF43F5E).copy(alpha = 0.15f),
        buttonStart = Color(0xFFF43F5E),
        buttonEnd = Color(0xFFBE123C),
        buttonPressedScale = 0.96f,
        decorElement = DecorElement.ELLIPSES
    )
    ThemeFamily.AMBER -> LoginThemeSpec(
        inputAccent = Color(0xFFFBBF24),
        inputGlow = Color(0xFFF59E0B).copy(alpha = 0.15f),
        buttonStart = Color(0xFFF59E0B),
        buttonEnd = Color(0xFFB45309),
        buttonPressedScale = 0.97f,
        decorElement = DecorElement.GRAIN
    )
    ThemeFamily.CLAY -> LoginThemeSpec(
        inputAccent = Color(0xFFA8A29E),
        inputGlow = Color(0xFF78716C).copy(alpha = 0.15f),
        buttonStart = Color(0xFF78716C),
        buttonEnd = Color(0xFF44403C),
        buttonPressedScale = 0.97f,
        decorElement = DecorElement.CROSS_HATCH
    )
    ThemeFamily.INK -> LoginThemeSpec(
        inputAccent = Color(0xFF94A3B8),
        inputGlow = Color(0xFF64748B).copy(alpha = 0.15f),
        buttonStart = Color(0xFF64748B),
        buttonEnd = Color(0xFF334155),
        buttonPressedScale = 0.98f,
        decorElement = DecorElement.GRID_DOTS
    )
}
