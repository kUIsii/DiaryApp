package com.diary.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class WritingGuideQuestion(
    val question: String,
    val hint: String = ""
)

val writingGuideQuestions = listOf(
    WritingGuideQuestion("今天发生了什么让你想记录的事?", "可以是一件小事、一个瞬间、或一个想法"),
    WritingGuideQuestion("你现在的感受如何?", "开心、平静、焦虑、感恩...任何感受都可以"),
    WritingGuideQuestion("有没有什么特别的细节想记住?", "一个画面、一句话、一种味道..."),
    WritingGuideQuestion("这件事对你意味着什么?", "不需要很深的思考，随意聊聊就好")
)

@Composable
fun AiWritingGuideDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (List<String>) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateOf(List(writingGuideQuestions.size) { "" }) }
    var currentInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "写作引导",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "回答几个问题，帮你开启今天的日记",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                writingGuideQuestions.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index <= currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = !isGenerating, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    // Current question
                    Text(
                        text = writingGuideQuestions[currentStep].question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (writingGuideQuestions[currentStep].hint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = writingGuideQuestions[currentStep].hint,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input field
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("随便写点...") },
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            TextButton(onClick = {
                                currentStep--
                                currentInput = answers.value[currentStep]
                            }) {
                                Text("上一步")
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("跳过，直接写")
                            }
                        }

                        Row {
                            // Skip this question
                            if (currentStep < writingGuideQuestions.lastIndex) {
                                TextButton(onClick = {
                                    val newAnswers = answers.value.toMutableList()
                                    newAnswers[currentStep] = ""
                                    answers.value = newAnswers
                                    currentStep++
                                    currentInput = ""
                                }) {
                                    Text("跳过", fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            val isLast = currentStep == writingGuideQuestions.lastIndex
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        val newAnswers = answers.value.toMutableList()
                                        newAnswers[currentStep] = currentInput
                                        answers.value = newAnswers
                                        if (isLast) {
                                            onGenerate(answers.value)
                                        } else {
                                            currentStep++
                                            currentInput = ""
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isLast) "生成日记" else "下一步",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    if (isLast) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Generating state
            AnimatedVisibility(visible = isGenerating, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在帮你构思日记...",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
