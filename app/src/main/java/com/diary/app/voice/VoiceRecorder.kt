package com.diary.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 语音录入管理器 - 支持录音和语音转文字
 */
class VoiceRecorder(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentAudioFile: File? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording
    
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing
    
    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription
    
    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration
    
    private var recordingStartTime: Long = 0
    
    companion object {
        private const val TAG = "VoiceRecorder"
    }
    
    /**
     * 检查是否有录音权限
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 开始录音
     */
    fun startRecording(): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "No recording permission")
            return false
        }
        
        if (_isRecording.value) {
            Log.w(TAG, "Already recording")
            return false
        }
        
        try {
            // 创建音频文件
            val audioDir = File(context.filesDir, "voice_memos")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentAudioFile = File(audioDir, "memo_$timestamp.m4a")
            
            // 初始化 MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(currentAudioFile?.absolutePath)
                
                prepare()
                start()
            }
            
            _isRecording.value = true
            recordingStartTime = System.currentTimeMillis()
            
            Log.d(TAG, "Recording started: ${currentAudioFile?.absolutePath}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorder()
            return false
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecording(): File? {
        if (!_isRecording.value) {
            Log.w(TAG, "Not recording")
            return null
        }
        
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            
            _isRecording.value = false
            
            Log.d(TAG, "Recording stopped: ${currentAudioFile?.absolutePath}")
            return currentAudioFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            releaseRecorder()
            return null
        }
    }
    
    /**
     * 取消录音
     */
    fun cancelRecording() {
        if (!_isRecording.value) return
        
        try {
            mediaRecorder?.apply {
                reset()
                release()
            }
            mediaRecorder = null
            
            // 删除临时文件
            currentAudioFile?.delete()
            currentAudioFile = null
            
            _isRecording.value = false
            
            Log.d(TAG, "Recording cancelled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording", e)
            releaseRecorder()
        }
    }
    
    /**
     * 开始语音转文字
     */
    fun startTranscription() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }
        
        if (_isTranscribing.value) {
            Log.w(TAG, "Already transcribing")
            return
        }
        
        _isTranscribing.value = true
        _transcription.value = ""
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // 音量变化回调
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // 缓冲区接收回调
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }
            
            override fun onError(error: Int) {
                Log.e(TAG, "Recognition error: $error")
                _isTranscribing.value = false
            }
            
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _transcription.value = matches[0]
                    Log.d(TAG, "Transcription: ${matches[0]}")
                }
                _isTranscribing.value = false
            }
            
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _transcription.value = matches[0]
                }
            }
            
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {
                // 事件回调
            }
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
        
        Log.d(TAG, "Transcription started")
    }
    
    /**
     * 停止语音转文字
     */
    fun stopTranscription() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isTranscribing.value = false
        
        Log.d(TAG, "Transcription stopped")
    }
    
    /**
     * 获取录音文件路径
     */
    fun getAudioFilePath(): String? {
        return currentAudioFile?.absolutePath
    }
    
    /**
     * 获取录音时长（秒）
     */
    fun getRecordingDuration(): Int {
        return if (_isRecording.value) {
            ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
        } else {
            0
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        releaseRecorder()
        stopTranscription()
    }
    
    private fun releaseRecorder() {
        try {
            mediaRecorder?.apply {
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        }
        mediaRecorder = null
        _isRecording.value = false
    }
}
