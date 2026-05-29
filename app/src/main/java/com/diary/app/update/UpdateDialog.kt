package com.diary.app.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateDialog(
    versionName: String,
    releaseNotes: String,
    isDownloading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text(
                text = "发现新版本 v$versionName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column {
                if (releaseNotes.isNotBlank()) {
                    Text(
                        text = releaseNotes,
                        fontSize = 14.sp,
                        color = textSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "正在下载...",
                            fontSize = 13.sp,
                            color = textTertiary
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
                        color = accentColor,
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
                        color = textTertiary
                    )
                }
            }
        }
    )
}
