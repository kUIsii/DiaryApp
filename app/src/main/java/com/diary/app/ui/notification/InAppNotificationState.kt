package com.diary.app.ui.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class InAppNotification(
    val id: String,
    val title: String,
    val subtitle: String
)

class InAppNotificationState {
    var currentNotification by mutableStateOf<InAppNotification?>(null)
        private set

    fun show(notification: InAppNotification) {
        currentNotification = notification
    }

    fun dismiss() {
        currentNotification = null
    }
}
