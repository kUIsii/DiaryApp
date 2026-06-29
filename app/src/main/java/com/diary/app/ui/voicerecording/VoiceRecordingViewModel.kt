package com.diary.app.ui.voicerecording

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.VoiceMemo
import com.diary.app.voice.VoiceRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceRecordingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    val voiceRecorder = VoiceRecorder(application)
    
    private val _savedMemos = MutableStateFlow<List<VoiceMemo>>(emptyList())
    val savedMemos: StateFlow<List<VoiceMemo>> = _savedMemos
    
    private val _currentMemo = MutableStateFlow<VoiceMemo?>(null)
    val currentMemo: StateFlow<VoiceMemo?> = _currentMemo
    
    init {
        loadMemos()
    }
    
    fun loadMemos() {
        viewModelScope.launch {
            dao.getAllVoiceMemos().collect { memos ->
                _savedMemos.value = memos
            }
        }
    }
    
    fun saveRecording(audioFile: File, durationSeconds: Int, transcript: String?, diaryId: Long? = null) {
        viewModelScope.launch {
            val memo = VoiceMemo(
                diaryId = diaryId,
                audioPath = audioFile.absolutePath,
                durationSeconds = durationSeconds,
                transcript = transcript,
                createdAt = System.currentTimeMillis()
            )
            val id = dao.insertVoiceMemo(memo)
            _currentMemo.value = memo.copy(id = id)
            loadMemos()
        }
    }
    
    fun deleteMemo(memoId: Long) {
        viewModelScope.launch {
            val memo = _savedMemos.value.find { it.id == memoId }
            memo?.let {
                // 删除音频文件
                File(it.audioPath).delete()
                dao.deleteVoiceMemo(memoId)
                loadMemos()
            }
        }
    }

    fun updateTranscript(memo: VoiceMemo, newTranscript: String) {
        viewModelScope.launch {
            dao.updateVoiceMemo(memo.copy(transcript = newTranscript))
            loadMemos()
        }
    }

    fun createDiaryFromTranscript(memo: VoiceMemo) {
        viewModelScope.launch {
            val transcript = memo.transcript ?: return@launch
            val now = System.currentTimeMillis()
            val entry = DiaryEntry(
                title = transcript.take(50),
                content = "{\"ops\":[{\"insert\":\"${transcript.replace("\"", "\\\"").replace("\n", "\\n")}\"}]}",
                plainText = transcript,
                createdAt = now,
                updatedAt = now
            )
            val entryId = dao.insertEntry(entry)
            dao.updateVoiceMemo(memo.copy(diaryId = entryId))
            loadMemos()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
    }
}
