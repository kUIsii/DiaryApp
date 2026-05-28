package com.diary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diary.app.ui.theme.DarkCardBackground
import com.diary.app.ui.theme.DarkCardBorder
import com.diary.app.ui.theme.LightCardBackground
import com.diary.app.ui.theme.LightCardBorder

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkCardBackground else LightCardBackground
    val borderColor = if (isDark) DarkCardBorder else LightCardBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
