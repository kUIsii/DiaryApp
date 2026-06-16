package com.diary.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ai.AssistantMessage
import com.diary.app.ai.ConversationInfo

@Composable
fun EditorAiPanel(
    visible: Boolean,
    messages: List<AssistantMessage>,
    loading: Boolean,
    conversations: List<ConversationInfo>,
    currentConversationId: Long,
    onSendMessage: (String) -> Unit,
    onResend: ((String) -> Unit)? = null,
    onSwitchConversation: (Long) -> Unit,
    onCreateNewConversation: () -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onDismiss: () -> Unit,
    quotedText: String? = null
) {
    var showHistory by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .heightIn(min = 300.dp, max = 420.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History button
                    IconButton(
                        onClick = { showHistory = !showHistory },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "历史对话",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "小墨助手",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (conversations.size > 1) "${conversations.size} 个对话" else "帮你构思润色",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // New conversation button
                    IconButton(
                        onClick = {
                            onCreateNewConversation()
                            showHistory = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新对话",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 0.5.dp
                )

                // History panel or Messages
                if (showHistory) {
                    // Conversation list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(conversations) { _, conv ->
                            ConversationHistoryItem(
                                conversation = conv,
                                isSelected = conv.id == currentConversationId,
                                onClick = {
                                    onSwitchConversation(conv.id)
                                    showHistory = false
                                },
                                onDelete = { onDeleteConversation(conv.id) }
                            )
                        }
                    }
                } else {
                    // Messages
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "有什么想聊的？",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                        itemsIndexed(messages) { index, message ->
                            EditorChatBubble(
                                message = message,
                                onResend = if (!message.isUser && (message.content.contains("网络不太好") || message.content.contains("稍后再聊"))) {
                                    {
                                        val lastUserMessage = messages
                                            .take(index)
                                            .lastOrNull { it.isUser }
                                        if (lastUserMessage != null) {
                                            onResend?.invoke(lastUserMessage.content)
                                        }
                                    }
                                } else null
                            )
                        }
                        if (loading) {
                            item {
                                Row(
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "思考中...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Input area
                EditorAiInput(
                    onSendMessage = onSendMessage,
                    quotedText = quotedText
                )
            }
        }
    }
}

@Composable
private fun ConversationHistoryItem(
    conversation: ConversationInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val timeAgo = getTimeAgo(conversation.updatedAt)
            Text(
                text = timeAgo,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "删除",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun EditorChatBubble(
    message: AssistantMessage,
    onResend: (() -> Unit)? = null
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }
        // Resend button for timeout errors
        if (onResend != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(onClick = onResend)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "重新发送",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorAiInput(
    onSendMessage: (String) -> Unit,
    quotedText: String? = null
) {
    var inputText by remember { mutableStateOf("") }
    var showQuoted by remember { mutableStateOf(false) }

    LaunchedEffect(quotedText) {
        if (!quotedText.isNullOrBlank()) {
            inputText = "「$quotedText」\n"
            showQuoted = true
        }
    }

    Column {
        if (showQuoted && quotedText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "引用: ${quotedText.take(60)}${if (quotedText.length > 60) "..." else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            thickness = 0.5.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "问问小墨...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = false,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                        showQuoted = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "发送",
                    modifier = Modifier.size(16.dp),
                    tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            }
        }
    }
}
