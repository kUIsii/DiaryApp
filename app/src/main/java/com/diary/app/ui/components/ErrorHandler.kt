package com.diary.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class ErrorType {
    ERROR, WARNING, INFO
}

@Composable
fun ErrorMessage(
    message: String,
    type: ErrorType = ErrorType.ERROR,
    onDismiss: () -> Unit = {},
    autoDismiss: Boolean = true,
    autoDismissDelay: Long = 3000L
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(message) {
        if (autoDismiss) {
            delay(autoDismissDelay)
            visible = false
            delay(300) // Wait for animation
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        val backgroundColor = when (type) {
            ErrorType.ERROR -> com.diary.app.ui.theme.ErrorColor.copy(alpha = 0.9f)
            ErrorType.WARNING -> com.diary.app.ui.theme.WarningColor.copy(alpha = 0.9f)
            ErrorType.INFO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        }

        val icon = when (type) {
            ErrorType.ERROR -> Icons.Default.Error
            ErrorType.WARNING -> Icons.Default.Warning
            ErrorType.INFO -> Icons.Default.Warning
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        visible = false
                        onDismiss()
                    }
            )
        }
    }
}

@Composable
fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit = {},
    autoDismiss: Boolean = true,
    autoDismissDelay: Long = 2000L
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(message) {
        if (autoDismiss) {
            delay(autoDismissDelay)
            visible = false
            delay(300)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(com.diary.app.ui.theme.SuccessColor.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        visible = false
                        onDismiss()
                    }
            )
        }
    }
}
