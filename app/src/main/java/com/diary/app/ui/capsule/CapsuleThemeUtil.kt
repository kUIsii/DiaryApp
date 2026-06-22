package com.diary.app.ui.capsule

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.diary.app.data.CapsuleTheme

@Composable
fun capsuleThemeColor(theme: CapsuleTheme): Color {
    return when (theme) {
        CapsuleTheme.NORMAL -> MaterialTheme.colorScheme.primary
        CapsuleTheme.BIRTHDAY -> Color(0xFFE8A0BF)
        CapsuleTheme.NEW_YEAR -> Color(0xFFE07070)
        CapsuleTheme.GRADUATION -> Color(0xFF9B8EBA)
        CapsuleTheme.TRAVEL -> Color(0xFF78B8B0)
        CapsuleTheme.LOVE -> Color(0xFFD99AB8)
        CapsuleTheme.DREAM -> Color(0xFFA88BC9)
    }
}
