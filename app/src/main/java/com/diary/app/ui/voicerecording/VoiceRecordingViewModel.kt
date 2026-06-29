package com.diary.app.ui.voicerecording

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.VoiceMemo
import com.diary.app.voice.VoiceRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
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

    private var memoCollectorJob: Job? = null
    
    init {
        observeMemos()
    }
    
    private fun observeMemos() {
        if (memoCollectorJob?.isActive == true) return
        memoCollectorJob = viewModelScope.launch {
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
        }
    }
    
    fun deleteMemo(memoId: Long) {
        viewModelScope.launch {
            val memo = _savedMemos.value.find { it.id == memoId }
            memo?.let {
                // 删除音频文件
                File(it.audioPath).delete()
                dao.deleteVoiceMemo(memoId)
            }
        }
    }

    fun updateTranscript(memo: VoiceMemo, newTranscript: String) {
        viewModelScope.launch {
            dao.updateVoiceMemo(memo.copy(transcript = newTranscript))
        }
    }

    fun createDiaryFromTranscript(memo: VoiceMemo) {
        viewModelScope.launch {
            val transcript = memo.transcript ?: return@launch
            createDiaryFromTranscriptText(transcript) { entryId ->
                if (entryId != null) {
                    viewModelScope.launch {
                        dao.updateVoiceMemo(memo.copy(diaryId = entryId))
                    }
                }
            }
        }
    }

    fun createDiaryFromTranscriptText(transcript: String, onComplete: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            if (!shouldOfferDiaryCreation(transcript)) {
                onComplete(null)
                return@launch
            }
            val id = try {
                val now = System.currentTimeMillis()
                val entry = DiaryEntry(
                    title = buildVoiceMemoTitle(transcript),
                    content = buildVoiceMemoDiaryContent(transcript),
                    plainText = transcript,
                    createdAt = now,
                    updatedAt = now
                )
                withContext(Dispatchers.IO) {
                    dao.insertEntry(entry)
                }
            } catch (_: Exception) {
                null
            }
            onComplete(id)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
    }
}
