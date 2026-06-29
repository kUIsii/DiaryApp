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
import android.media.MediaPlayer
import androidx.compose.ui.text.style.TextOverflow
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
    
    var playingMemoId by remember { mutableStateOf<Long?>(null) }
    var editingMemoId by remember { mutableStateOf<Long?>(null) }
    var editingText by remember { mutableStateOf("") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val onPlayPause: (VoiceMemo) -> Unit = { memo ->
        if (playingMemoId == memo.id) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            playingMemoId = null
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(memo.audioPath)
                setOnPreparedListener { start() }
                prepareAsync()
                setOnCompletionListener {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    playingMemoId = null
                }
            }
            playingMemoId = memo.id
        }
    }
    
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
            Text(
                text = "已保存的备忘录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (savedMemos.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    innerPadding = 24.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "还没有录音，点击下方按钮开始录制",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedMemos) { memo ->
                        VoiceMemoCard(
                            memo = memo,
                            isPlaying = playingMemoId == memo.id,
                            editingMemoId = editingMemoId,
                            editingText = editingText,
                            onPlay = { onPlayPause(memo) },
                            onEditStart = {
                                editingMemoId = memo.id
                                editingText = memo.transcript ?: ""
                            },
                            onEditChange = { editingText = it },
                            onEditSave = {
                                if (editingText.isNotBlank()) {
                                    viewModel.updateTranscript(memo, editingText)
                                }
                                editingMemoId = null
                                editingText = ""
                            },
                            onEditCancel = {
                                editingMemoId = null
                                editingText = ""
                            },
                            onDelete = { viewModel.deleteMemo(memo.id) },
                            onCreateDiary = { viewModel.createDiaryFromTranscript(memo) }
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

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}分${s}秒" else "${s}秒"
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
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
    isPlaying: Boolean,
    editingMemoId: Long?,
    editingText: String,
    onPlay: () -> Unit,
    onEditStart: () -> Unit,
    onEditChange: (String) -> Unit,
    onEditSave: () -> Unit,
    onEditCancel: () -> Unit,
    onDelete: () -> Unit,
    onCreateDiary: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val fileSize = remember(memo.audioPath) { File(memo.audioPath).length() }
    val isEditing = editingMemoId == memo.id
    val transcriptText = memo.transcript
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        innerPadding = 12.dp,
        onClick = { if (!isEditing) onPlay() }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(20.dp),
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = formatDuration(memo.durationSeconds),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatFileSize(fileSize),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row {
                    if (transcriptText != null && transcriptText.isNotBlank() && !isEditing) {
                        IconButton(onClick = onCreateDiary) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "创建日记",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editingText,
                    onValueChange = onEditChange,
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onEditSave) {
                        Text("保存")
                    }
                    OutlinedButton(onClick = onEditCancel) {
                        Text("取消")
                    }
                }
            } else if (transcriptText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transcriptText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditStart) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑转写",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(Date(memo.createdAt)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
