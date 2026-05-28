package com.diary.app.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.DarkTextPrimary
import com.diary.app.ui.theme.DarkTextSecondary
import com.diary.app.ui.theme.DarkTextTertiary

@Composable
fun UpdateDialog(
    versionName: String,
    releaseNotes: String,
    isDownloading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        containerColor = Color(0xFF1E1E2E),
        title = {
            Text(
                text = "发现新版本 v$versionName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary
            )
        },
        text = {
            Column {
                if (releaseNotes.isNotBlank()) {
                    Text(
                        text = releaseNotes,
                        fontSize = 14.sp,
                        color = DarkTextSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = DarkAccentStart,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "正在下载...",
                            fontSize = 13.sp,
                            color = DarkTextTertiary
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = "立即更新",
                        color = DarkAccentStart,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "稍后",
                        color = DarkTextTertiary
                    )
                }
            }
        }
    )
}
