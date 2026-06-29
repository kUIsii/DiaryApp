package com.diary.app.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ai.AssistantMessage
import com.diary.app.ai.AiAssistantViewModel
import com.diary.app.ai.ConversationInfo
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val lastTokens by viewModel.lastTokens.collectAsState()
    val lastModel by viewModel.lastModel.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drawer header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "对话列表",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                viewModel.createNewConversation()
                                scope.launch { drawerState.close() }
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "新对话",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Conversation list
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(conversations) { _, conv ->
                            ConversationItem(
                                conversation = conv,
                                isSelected = conv.id == currentConversationId,
                                onClick = {
                                    viewModel.switchConversation(conv.id)
                                    scope.launch { drawerState.close() }
                                },
                                onDelete = {
                                    viewModel.deleteConversation(conv.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        // Main content
        GradientBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    cornerRadius = 22.dp,
                    innerPadding = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "小墨",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = when {
                                    lastTokens > 0 -> {
                                        val modelShort = lastModel.substringAfter("/", "").take(15)
                                        "${conversations.size} 个对话 · $modelShort · $lastTokens tokens"
                                    }
                                    conversations.size > 1 -> "${conversations.size} 个对话"
                                    else -> "你的日记助手"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearHistory() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "清空",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "对话列表",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "提示：你也可以和过去的自己对话——在对话中说'回顾2024年5月'即可",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            EmptyStateView()
                        }
                    }

                    itemsIndexed(messages) { index, msg ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
                        ) {
                            ChatBubble(
                                message = msg,
                                onResend = if (!msg.isUser && msg.content.contains("网络不太好")) {
                                    { _ ->
                                        val lastUserMessage = messages
                                            .take(index)
                                            .lastOrNull { it.isUser }
                                        if (lastUserMessage != null) {
                                            viewModel.sendMessage(lastUserMessage.content)
                                        }
                                    }
                                } else null
                            )
                        }
                    }

                    if (loading) {
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "思考中...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    cornerRadius = 22.dp,
                    innerPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "说点什么...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText.trim())
                                        inputText = ""
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText.trim())
                                    inputText = ""
                                    keyboardController?.hide()
                                }
                            },
                            enabled = inputText.isNotBlank() && !loading,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !loading) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "发送",
                                tint = if (inputText.isNotBlank() && !loading) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else
            Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(conversation.updatedAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AssistantMessage, onResend: ((String) -> Unit)? = null) {
    val isUser = message.isUser
    val isTimeoutError = !isUser && (message.content.contains("网络不太好") || message.content.contains("稍后再聊"))

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Surface(
                shape = if (isUser)
                    RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)
                else
                    RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp),
                color = if (isUser)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isUser) 0.dp else 1.dp,
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isUser)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        // Resend button for timeout errors
        if (isTimeoutError && onResend != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Spacer(modifier = Modifier.width(34.dp))
                Text(
                    text = "点击重试",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onResend(message.content) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "你好，我是小墨",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "你的日记助手，会围绕记录继续聊下去\n想整理思路、续写内容或回顾这段时间，都可以直接开口",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}
