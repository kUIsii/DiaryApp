package com.diary.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.diary.app.ui.components.GlassCard

@Composable
fun ShareFormatDialog(
    onDismiss: () -> Unit,
    onShareText: () -> Unit,
    onShareImage: () -> Unit,
    onShareHtml: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                cornerRadius = 20.dp,
                enableShadow = true,
                innerPadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Gradient accent bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "分享日记",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "选择分享格式",
                            fontSize = 13.sp,
                            color = textSecondary.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        ShareFormatOption(
                            icon = Icons.Default.TextFields,
                            title = "文本",
                            description = "纯文本，适合微信、短信等",
                            onClick = onShareText,
                            textColor = textColor,
                            textSecondary = textSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ShareFormatOption(
                            icon = Icons.Default.Image,
                            title = "图片",
                            description = "长图卡片，适合朋友圈、微博",
                            onClick = onShareImage,
                            textColor = textColor,
                            textSecondary = textSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ShareFormatOption(
                            icon = Icons.Default.Code,
                            title = "HTML",
                            description = "完整页面，含图片，浏览器打开",
                            onClick = onShareHtml,
                            textColor = textColor,
                            textSecondary = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareFormatOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    textColor: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = textSecondary.copy(alpha = 0.6f)
            )
        }
    }
}
