package com.diary.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFE53935),
    contentColor: Color = Color.White
) {
    if (count <= 0) return

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "badgeScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .size(if (count > 9) 24.dp else 20.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = contentColor,
            fontSize = if (count > 9) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DotBadge(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE53935)
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
