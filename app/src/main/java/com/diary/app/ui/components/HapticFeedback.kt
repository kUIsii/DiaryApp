package com.diary.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Provides haptic feedback for important interactions.
 * Use this composable to get access to haptic feedback functions.
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val haptic = LocalHapticFeedback.current
    return remember {
        HapticFeedbackHelper(haptic)
    }
}

class HapticFeedbackHelper(
    private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    /** Light feedback for button clicks and selections */
    fun click() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** Medium feedback for successful operations (save, create) */
    fun success() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** Heavy feedback for destructive operations (delete) */
    fun warning() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Error feedback for failed operations */
    fun error() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
