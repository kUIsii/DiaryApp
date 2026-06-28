package com.diary.app.ui.voicerecording

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.VoiceMemo
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordingScreen(
    onNavigateBack: () -> Unit,
    diaryId: Long? = null,
    viewModel: VoiceRecordingViewModel = viewModel()
) {
    val context = LocalContext.current
    val savedMemos by viewModel.savedMemos.collectAsState()
    val isRecording by viewModel.voiceRecorder.isRecording.collectAsState()
    val isTranscribing by viewModel.voiceRecorder.isTranscribing.collectAsState()
    val transcription by viewModel.voiceRecorder.transcription.collectAsState()
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "语音备忘录",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recording area
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                innerPadding = 24.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Recording animation
                    if (isRecording) {
                        RecordingAnimation()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "录音中...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "转写中...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "点击按钮开始录音",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Control buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    if (hasPermission) {
                                        viewModel.voiceRecorder.startRecording()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.size(64.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                enabled = !isTranscribing
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "开始录音",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    val audioFile = viewModel.voiceRecorder.stopRecording()
                                    if (audioFile != null) {
                                        val duration = viewModel.voiceRecorder.getRecordingDuration()
                                        viewModel.saveRecording(
                                            audioFile = audioFile,
                                            durationSeconds = duration,
                                            transcript = transcription.takeIf { it.isNotBlank() },
                                            diaryId = diaryId
                                        )
                                    }
                                },
                                modifier = Modifier.size(64.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "停止录音",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    viewModel.voiceRecorder.cancelRecording()
                                },
                                modifier = Modifier.size(64.dp),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "取消录音",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        if (!isRecording && transcription.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.voiceRecorder.startTranscription()
                                },
                                modifier = Modifier.size(64.dp),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Icon(
                                    Icons.Default.Transcribe,
                                    contentDescription = "转写语音",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    
                    // Transcription display
                    if (transcription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp,
                            innerPadding = 12.dp
                        ) {
                            Column {
                                Text(
                                    text = "转写结果",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = transcription,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Saved memos list
            if (savedMemos.isNotEmpty()) {
                Text(
                    text = "已保存的备忘录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedMemos) { memo ->
                        VoiceMemoCard(
                            memo = memo,
                            onDelete = { viewModel.deleteMemo(memo.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val errorColor = MaterialTheme.colorScheme.error
    
    Canvas(modifier = Modifier.size(120.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // Outer pulse
        drawCircle(
            color = errorColor.copy(alpha = 0.2f),
            radius = 50f * scale,
            center = Offset(centerX, centerY)
        )
        
        // Inner circle
        drawCircle(
            color = errorColor,
            radius = 30f,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
private fun VoiceMemoCard(
    memo: VoiceMemo,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        innerPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${memo.durationSeconds}秒",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (memo.transcript != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = memo.transcript,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(memo.createdAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
