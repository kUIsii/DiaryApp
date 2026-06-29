package com.diary.app.ui.voicerecording

fun buildVoiceMemoTitle(transcript: String): String {
    val cleaned = transcript.trim()
    return if (cleaned.isBlank()) "语音备忘录" else cleaned.take(20)
}

fun buildVoiceMemoDiaryContent(transcript: String): String {
    val escaped = transcript
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
    return "{\"ops\":[{\"insert\":\"$escaped\"}]}"
}

fun shouldOfferDiaryCreation(transcript: String): Boolean {
    return transcript.isNotBlank()
}
