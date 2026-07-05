package com.diary.app.data.auth

import android.content.Context
import com.diary.app.DiaryApplication
import com.diary.app.data.sync.CloudSyncManager
import com.diary.app.data.sync.SyncWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.diary.app.data.CapsuleTheme
import com.diary.app.data.ChatConversationEntity
import com.diary.app.data.ChatMessageEntity
import com.diary.app.data.CountDownItem
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryImage
import com.diary.app.data.DiaryTag
import com.diary.app.data.HabitRecord
import com.diary.app.data.NotificationEntity
import com.diary.app.data.Tag
import com.diary.app.data.TimeCapsule
import com.diary.app.data.TodoItem
import com.diary.app.data.TrashEntry
import java.security.MessageDigest

enum class AuthState {
    UNINITIALIZED, LOGGED_OUT, LOGGED_IN
}

data class AuthUiState(
    val state: AuthState = AuthState.UNINITIALIZED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val phone: String = ""
)

class AuthManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "diary_auth"
        private const val KEY_PHONE = "auth_phone"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REGISTERED = "auth_registered"
        private const val KEY_PIN_HASH = "auth_pin_hash"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val syncManager = CloudSyncManager(context)
    private val app = context.applicationContext as DiaryApplication
    private val gson = Gson()

    val savedPhone: String? get() = prefs.getString(KEY_PHONE, null)
    val savedToken: String? get() = prefs.getString(KEY_TOKEN, null)
    val isLoggedIn: Boolean get() = !savedToken.isNullOrBlank()
    val isRegistered: Boolean get() = prefs.getBoolean(KEY_REGISTERED, false)

    fun restoreSession(): AuthUiState {
        val phone = savedPhone
        val token = savedToken
        return if (phone != null && token != null) {
            AuthUiState(state = AuthState.LOGGED_IN, phone = phone)
        } else {
            AuthUiState(state = AuthState.LOGGED_OUT)
        }
    }

    suspend fun register(phone: String, pin: String): Result<AuthUiState> {
        if (phone.isBlank() || pin.length < 4) {
            return Result.failure(Exception("手机号或 PIN 格式不正确"))
        }
        val cloudResult = syncManager.register(phone, pin)
        return if (cloudResult.isSuccess) {
            saveLocalHash(phone, pin)
            saveAuth(phone, cloudResult.getOrThrow())
            pushData()
            Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        } else {
            saveLocalHash(phone, pin)
            val token = generateLocalToken(phone)
            saveAuth(phone, token)
            Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        }
    }

    suspend fun login(phone: String, pin: String): Result<AuthUiState> {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash != null && hashPin(pin, phone) == storedHash) {
            val token = generateLocalToken(phone)
            saveAuth(phone, token)
            pushData()
            return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        }
        val cloudResult = syncManager.login(phone, pin)
        if (cloudResult.isSuccess) {
            saveLocalHash(phone, pin)
            saveAuth(phone, cloudResult.getOrThrow())
            pushData()
            return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = phone))
        }
        return if (storedHash != null) {
            Result.failure(Exception("PIN 错误"))
        } else {
            Result.failure(Exception("未找到账号信息，请重新注册"))
        }
    }

    suspend fun changePhone(newPhone: String, newPin: String): Result<AuthUiState> {
        if (newPhone.isBlank() || newPin.length < 4) {
            return Result.failure(Exception("手机号或 PIN 格式不正确"))
        }
        val currentPhone = savedPhone ?: return Result.failure(Exception("请先登录"))

        var oldCloudJson: String? = null
        runCatching { oldCloudJson = syncManager.pullBackup().getOrNull() }

        val cloudResult = syncManager.register(newPhone, newPin)
        if (cloudResult.isSuccess) {
            if (oldCloudJson != null) {
                runCatching {
                    val mapType = object : TypeToken<Map<String, Any?>>() {}.type
                    val data: Map<String, Any?> = gson.fromJson(oldCloudJson, mapType)
                    restoreFromCloud(data)
                }
            }
            pushData()
            saveAuth(newPhone, cloudResult.getOrThrow())
            saveLocalHash(newPhone, newPin)
            return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = newPhone))
        }
        saveLocalHash(newPhone, newPin)
        val token = generateLocalToken(newPhone)
        saveAuth(currentPhone, token)
        return Result.success(AuthUiState(state = AuthState.LOGGED_IN, phone = currentPhone))
    }

    fun syncNow() {
        SyncWorker.syncOnce(app)
    }

    suspend fun pullFromCloud(): Result<String> {
        val result = syncManager.pullBackup()
        if (result.isFailure) return Result.failure(result.exceptionOrNull() ?: Exception("云端无数据"))
        return try {
            val json = result.getOrThrow()
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val data: Map<String, Any?> = gson.fromJson(json, mapType)
            restoreFromCloud(data)
            Result.success("从云端恢复成功")
        } catch (e: Exception) {
            Result.failure(Exception("数据解析失败: ${e.message}"))
        }
    }

    private suspend fun restoreFromCloud(data: Map<String, Any?>) {
        val dao = app.database.diaryDao()

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["diaries"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertEntry(DiaryEntry(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    title = t["title"] as? String ?: "",
                    content = t["content"] as? String ?: "",
                    plainText = t["plainText"] as? String ?: "",
                    moodLevel = (t["moodLevel"] as? Double)?.toInt(),
                    weather = t["weather"] as? String,
                    location = t["location"] as? String,
                    latitude = t["latitude"] as? Double,
                    longitude = t["longitude"] as? Double,
                    isFavorite = t["isFavorite"] as? Boolean ?: false,
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (t["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    writingDurationSeconds = (t["writingDurationSeconds"] as? Double)?.toInt()
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["tags"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertTag(Tag(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    name = t["name"] as? String ?: "",
                    color = (t["color"] as? Double)?.toLong() ?: 0L,
                    isPreset = t["isPreset"] as? Boolean ?: false
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["diaryTags"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertDiaryTag(DiaryTag(
                    diaryId = (t["diaryId"] as? Double)?.toLong() ?: 0,
                    tagId = (t["tagId"] as? Double)?.toLong() ?: 0
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["tasks"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertTodo(TodoItem(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    title = t["title"] as? String ?: "",
                    description = t["description"] as? String ?: "",
                    isCompleted = t["isCompleted"] as? Boolean ?: false,
                    priority = (t["priority"] as? Double)?.toInt() ?: 0,
                    dueDate = (t["dueDate"] as? Double)?.toLong(),
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    completedAt = (t["completedAt"] as? Double)?.toLong(),
                    sortOrder = (t["sortOrder"] as? Double)?.toInt() ?: 0,
                    category = t["category"] as? String ?: "task",
                    reminderTime = (t["reminderTime"] as? Double)?.toLong(),
                    tags = t["tags"] as? String ?: "",
                    parentId = (t["parentId"] as? Double)?.toLong(),
                    recurringType = t["recurringType"] as? String ?: "none",
                    progress = (t["progress"] as? Double)?.toInt() ?: 0,
                    isPinned = t["isPinned"] as? Boolean ?: false,
                    linkedTagIds = t["linkedTagIds"] as? String ?: ""
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["images"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertImage(DiaryImage(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    entryId = (t["entryId"] as? Double)?.toLong() ?: 0,
                    localPath = t["localPath"] as? String ?: "",
                    thumbPath = t["thumbPath"] as? String,
                    mediaName = t["mediaName"] as? String ?: "",
                    mediaRef = t["mediaRef"] as? String ?: "",
                    mimeType = t["mimeType"] as? String ?: "image/jpeg",
                    fileSize = (t["fileSize"] as? Double)?.toLong() ?: 0L,
                    sortOrder = (t["sortOrder"] as? Double)?.toInt() ?: 0,
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["habits"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertHabitRecord(HabitRecord(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    todoId = (t["todoId"] as? Double)?.toLong() ?: 0,
                    recordDate = (t["recordDate"] as? Double)?.toLong() ?: 0,
                    source = t["source"] as? String ?: HabitRecord.SOURCE_MANUAL,
                    summary = t["summary"] as? String ?: "",
                    diaryEntryId = (t["diaryEntryId"] as? Double)?.toLong(),
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (t["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["countdowns"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertCountDownItem(CountDownItem(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    title = t["title"] as? String ?: "",
                    targetDate = (t["targetDate"] as? Double)?.toLong() ?: 0,
                    isCountUp = t["isCountUp"] as? Boolean ?: false,
                    color = (t["color"] as? Double)?.toLong() ?: 0xFF4A90D9,
                    isRepeatYearly = t["isRepeatYearly"] as? Boolean ?: false,
                    isPinned = t["isPinned"] as? Boolean ?: false,
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["capsules"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertCapsule(TimeCapsule(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    title = t["title"] as? String ?: "",
                    content = t["content"] as? String ?: "",
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    unlockDate = (t["unlockDate"] as? Double)?.toLong() ?: 0,
                    isRead = t["isRead"] as? Boolean ?: false,
                    isOpened = t["isOpened"] as? Boolean ?: false,
                    theme = try { CapsuleTheme.valueOf(t["theme"] as? String ?: "NORMAL") } catch (e: Exception) { CapsuleTheme.NORMAL },
                    imageUri = t["imageUri"] as? String,
                    unlockHour = (t["unlockHour"] as? Double)?.toInt() ?: 0,
                    unlockMinute = (t["unlockMinute"] as? Double)?.toInt() ?: 0
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["trash"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertTrashEntry(TrashEntry(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    originalId = (t["originalId"] as? Double)?.toLong() ?: 0,
                    title = t["title"] as? String ?: "",
                    content = t["content"] as? String ?: "",
                    plainText = t["plainText"] as? String ?: "",
                    moodLevel = (t["moodLevel"] as? Double)?.toInt(),
                    weather = t["weather"] as? String,
                    location = t["location"] as? String,
                    latitude = t["latitude"] as? Double,
                    longitude = t["longitude"] as? Double,
                    isFavorite = t["isFavorite"] as? Boolean ?: false,
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: 0,
                    updatedAt = (t["updatedAt"] as? Double)?.toLong() ?: 0,
                    deletedAt = (t["deletedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["notifications"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertNotification(NotificationEntity(
                    id = t["id"] as? String ?: "",
                    type = t["type"] as? String ?: "",
                    title = t["title"] as? String ?: "",
                    subtitle = t["subtitle"] as? String ?: "",
                    iconType = t["iconType"] as? String ?: "",
                    colorHex = (t["colorHex"] as? Double)?.toLong() ?: 0L,
                    relatedId = (t["relatedId"] as? Double)?.toLong(),
                    isRead = t["isRead"] as? Boolean ?: false,
                    isTrashed = t["isTrashed"] as? Boolean ?: false,
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    trashedAt = (t["trashedAt"] as? Double)?.toLong()
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["conversations"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertConversation(ChatConversationEntity(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    title = t["title"] as? String ?: "新对话",
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (t["updatedAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                ))
            } }
        }

        gson.fromJson<List<Map<String, Any?>>>(gson.toJson(data["chatMessages"]), object : TypeToken<List<Map<String, Any?>>>() {}.type)?.let { items ->
            items.forEach { t -> runCatching {
                dao.insertChatMessage(ChatMessageEntity(
                    id = (t["id"] as? Double)?.toLong() ?: 0,
                    conversationId = (t["conversationId"] as? Double)?.toLong() ?: 0,
                    role = t["role"] as? String ?: "",
                    content = t["content"] as? String ?: "",
                    createdAt = (t["createdAt"] as? Double)?.toLong() ?: System.currentTimeMillis()
                ))
            } }
        }
    }

    private fun pushData() {
        SyncWorker.syncOnce(app)
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_PHONE)
            .remove(KEY_TOKEN)
            .remove(KEY_REGISTERED)
            .remove(KEY_PIN_HASH)
            .apply()
        syncManager.clearCredentials()
    }

    fun logoutAndClearData() {
        logout()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val dao = app.database.diaryDao()
            dao.deleteAllEntries()
            dao.deleteAllTags()
            dao.deleteAllDiaryTags()
            dao.deleteAllImages()
            dao.deleteAllTodos()
            dao.deleteAllHabitRecords()
            dao.deleteAllTrashEntries()
            dao.deleteAllCountDownItems()
            dao.deleteAllCapsules()
            dao.deleteAllNotifications()
            dao.deleteAllChatMessages()
            dao.deleteAllConversations()
        }
    }

    private fun saveAuth(phone: String, token: String) {
        prefs.edit()
            .putString(KEY_PHONE, phone)
            .putString(KEY_TOKEN, token)
            .putBoolean(KEY_REGISTERED, true)
            .apply()
    }

    private fun saveLocalHash(phone: String, pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin, phone)).apply()
    }

    private fun generateLocalToken(phone: String): String {
        val raw = "$phone:${System.currentTimeMillis()}:${Math.random()}"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, phone: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$phone:$pin".toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
