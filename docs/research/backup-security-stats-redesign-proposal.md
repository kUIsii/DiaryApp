# Backup / Security / Stats / Settings / CountDown / Tag / Update 重设计方案

> 基于对现有代码的深入分析，针对七大模块提出系统性改进方案。
> 技术栈：Jetpack Compose + Room (v13) + MVVM + Kotlin Coroutines/Flow

---

## 目录

1. [Backup 增强](#1-backup-增强)
2. [Export 增强](#2-export-增强)
3. [Security 增强](#3-security-增强)
4. [Stats 重构](#4-stats-重构)
5. [Settings 重构](#5-settings-重构)
6. [CountDown 增强](#6-countdown-增强)
7. [Tag 增强](#7-tag-增强)
8. [Update 改进](#8-update-改进)
9. [参考应用分析](#9-参考应用分析)
10. [数据库迁移计划](#10-数据库迁移计划)
11. [实施路线图](#11-实施路线图)

---

## 1. Backup 增强

### 1.1 现状分析

当前 `BackupManager` 是一个纯 `object` 单例，使用 SharedPreferences 存储配置，存在以下问题：

| 问题 | 影响 |
|------|------|
| 仅支持本地文件系统备份 | 换机/卸载即丢失 |
| 无加密，JSON 明文存储 | 隐私泄露风险 |
| 自动备份依赖 `shouldAutoBackup()` 在 app 启动时检查 | 若用户长时间不打开 app 则不触发 |
| 全量导出，每次备份全部日记 | 数据量大时耗时且浪费空间 |
| 备份历史记录存储在 SharedPreferences 中 | 超过几 MB 后性能下降 |
| `performAutoBackup` 异常被静默吞掉 | 用户无法感知备份失败 |

### 1.2 方案一：Google Drive 云备份

**优先级：高 | 工作量：大 (2-3 周)**

```
架构变更：

BackupManager (object)
  ├── LocalBackupManager   -- 保留现有本地逻辑
  ├── CloudBackupManager   -- 新增 Google Drive 逻辑
  ├── BackupScheduler       -- WorkManager 调度
  └── BackupEncryptor       -- AES-256-GCM 加密层
```

**核心实现思路：**

```kotlin
// 新增 CloudBackupManager
class CloudBackupManager(
    private val context: Context,
    private val driveClient: Drive // Google Drive REST API
) {
    // 使用 App Data Folder 或用户指定文件夹
    suspend fun uploadBackup(localFile: File, metadata: BackupMetadata): Result<String> {
        return try {
            val encryptedFile = BackupEncryptor.encrypt(localFile)
            val fileMetadata = File().apply {
                name = metadata.fileName
                parents = listOf("appDataFolder") // 隐藏目录，不污染用户 Drive
            }
            val content = FileContent("application/octet-stream", encryptedFile)
            val uploaded = driveClient.files().create(fileMetadata, content).execute()
            Result.success(uploaded.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBackups(): Result<List<CloudBackupInfo>> { ... }
    suspend fun downloadBackup(fileId: String): Result<File> { ... }
    suspend fun deleteBackup(fileId: String): Result<Unit> { ... }
}
```

**依赖引入：**
```kotlin
// build.gradle.kts
implementation("com.google.android.gms:play-services-auth:20.7.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20231105-2.0.0")
implementation("com.google.api-client:google-api-client-android:2.2.0")
implementation("com.google.http-client:google-http-client-gson:1.43.3")
```

**Google 登录流程：**
```kotlin
// 使用 Google Sign-In 获取 Drive scope
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestEmail()
    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
    .build()
```

### 1.3 方案二：WorkManager 调度自动备份

**优先级：高 | 工作量：中 (3-5 天)**

当前自动备份依赖 app 启动时检查 `shouldAutoBackup()`，这意味着用户如果几天不打开 app，自动备份就不会触发。改用 WorkManager 的 `PeriodicWorkRequest`：

```kotlin
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = DiaryDatabase.getDatabase(applicationContext).diaryDao()

        return try {
            val record = BackupManager.performAutoBackup(applicationContext, dao)
            if (record != null) {
                // 可选：上传到云端
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context, frequency: BackupFrequency) {
            val workManager = WorkManager.getInstance(context)

            if (frequency == BackupFrequency.DISABLED) {
                workManager.cancelUniqueWork("auto_backup")
                return
            }

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                frequency.days.toLong(), TimeUnit.DAYS
            )
                .setInitialDelay(frequency.days.toLong(), TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                "auto_backup",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
```

### 1.4 方案三：增量备份

**优先级：中 | 工作量：大 (2 周)**

当前每次备份都是全量 JSON 导出。增量备份思路：

```kotlin
// 在 DiaryEntry 表中添加 syncVersion 字段
// 每次备份后记录 version，下次只备份 version > lastBackupVersion 的条目

data class IncrementalBackup(
    val baseVersion: Long,      // 基于哪个全量备份
    val entries: List<ExportFile>,  // 仅变更的条目
    val deletedIds: List<Long>,     // 被删除的条目
    val tags: List<ExportTag>       // 标签全量（量小）
)
```

**实现要点：**
- 需要 `DiaryEntry` 新增 `syncVersion` 字段（INTEGER DEFAULT 0）
- 首次全量备份后记录 `lastBackupVersion`
- 后续只导出 `WHERE syncVersion > lastBackupVersion` 的条目
- 恢复时先恢复全量，再按顺序叠加增量

### 1.5 方案四：备份加密

**优先级：高 | 工作量：小 (2-3 天)**

```kotlin
object BackupEncryptor {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE)
        return keyGen.generateKey()
    }

    fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(data)
        // IV + encrypted data
        return iv + encrypted
    }

    fun decrypt(data: ByteArray, key: SecretKey): ByteArray {
        val iv = data.sliceArray(0 until IV_SIZE)
        val encrypted = data.sliceArray(IV_SIZE until data.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encrypted)
    }

    // Key 存储在 Android Keystore 中
    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val alias = "diary_backup_key"
        if (!keyStore.containsAlias(alias)) {
            val keyGen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
        return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
    }
}
```

### 1.6 备份模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| WorkManager 调度 | 高 | 3-5 天 | Phase 1 |
| 备份加密 | 高 | 2-3 天 | Phase 1 |
| 备份失败通知 | 高 | 1 天 | Phase 1 |
| Google Drive 云备份 | 高 | 2-3 周 | Phase 2 |
| 增量备份 | 中 | 2 周 | Phase 3 |

---

## 2. Export 增强

### 2.1 现状分析

当前 `DiaryExporter` 支持三种导出格式：

| 格式 | 实现 | 问题 |
|------|------|------|
| JSON | `export()` | 仅支持全量导出，无选择性导出 |
| Markdown | `exportAsMarkdown()` | 丢失富文本格式，仅 plainText |
| PNG 图片 | `exportAsImage()` | 使用 Canvas 绘制，无法渲染 HTML 格式内容 |

**核心问题：**
- 导出的 JSON 中 `content` 字段包含 HTML（来自 WebView 编辑器），但 Markdown 导出时丢弃了它，只用 `plainText`
- PNG 导出使用 Android Canvas 逐行绘制，无法处理 HTML 富文本中的加粗、列表、引用等
- 没有 PDF 导出（用户最常请求的格式）
- 没有批量导出单篇日记的能力
- 没有 DOCX 格式（分享给非技术用户时需要）

### 2.2 PDF 导出

**优先级：高 | 工作量：中 (1 周)**

使用 Android 内置 `PdfDocument` API 或引入 `iText`：

```kotlin
// 方案 A：Android 原生 PdfDocument + WebView 渲染
// 优点：无需额外依赖；缺点：排版控制有限

suspend fun exportAsPdf(context: Context, entry: DiaryEntry, tags: List<String>): String {
    // 1. 用 WebView 渲染 HTML
    val webView = WebView(context).apply {
        loadDataWithBaseURL(null, buildStyledHtml(entry, tags), "text/html", "UTF-8", null)
    }
    // 等待渲染完成
    delay(500)

    // 2. WebView 转 PDF
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val printAdapter = webView.createPrintDocumentAdapter("diary_${entry.id}")
    // ... 保存到文件

    return filePath
}

// 方案 B：引入 iText (更精细的排版控制)
// implementation("com.itextpdf:itext7-core:8.0.2")
// 优点：完全控制排版；缺点：库体积较大 (~5MB)
```

**推荐方案 A**，因为日记内容已经是 HTML，WebView 渲染最自然，且不增加 APK 体积。

```kotlin
private fun buildStyledHtml(entry: DiaryEntry, tags: List<String>): String {
    val dateText = Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault()).toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE))

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <style>
            @page { margin: 2cm; }
            body {
                font-family: "Noto Serif SC", "Source Han Serif SC", serif;
                color: #2D2D3A;
                line-height: 1.8;
                padding: 0;
            }
            .date { font-size: 18pt; font-weight: bold; margin-bottom: 8px; }
            .meta { font-size: 10pt; color: #6B6B80; margin-bottom: 16px; }
            .divider { border: none; border-top: 1px solid #E8E6E1; margin: 16px 0; }
            .tags { font-size: 10pt; color: #667EEA; margin-bottom: 16px; }
            .content { font-size: 12pt; }
            .content img { max-width: 100%; height: auto; }
            .footer { text-align: center; font-size: 9pt; color: #999; margin-top: 32px; }
        </style>
    </head>
    <body>
        <div class="date">$dateText</div>
        <div class="meta">
            ${entry.moodLevel?.let { "心情: ${moodLabelForLevel(it)}" } ?: ""}
            ${entry.weather?.let { " | 天气: ${weatherLabelFor(it)}" } ?: ""}
        </div>
        <hr class="divider">
        ${if (tags.isNotEmpty()) "<div class='tags'>${tags.joinToString(" · ") { "#$it" }}</div>" else ""}
        <div class="content">${entry.content}</div>
        <hr class="divider">
        <div class="footer">日记本</div>
    </body>
    </html>
    """.trimIndent()
}
```

### 2.3 HTML 导出

**优先级：中 | 工作量：小 (1-2 天)**

```kotlin
suspend fun exportAsHtml(context: Context, dao: DiaryDao): String {
    val entries = getAllEntries(dao)
    val tags = dao.getAllTagsOnce()
    val allDiaryTags = dao.getAllDiaryTags()
    val tagMap = tags.associateBy { it.id }
    val diaryTagMap = allDiaryTags.groupBy({ it.diaryId }, { tagMap[it.tagId]?.name ?: "" })

    val html = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html lang='zh-CN'><head><meta charset='UTF-8'>")
        appendLine("<title>日记本导出</title>")
        appendLine("<style>/* 自包含样式 */</style></head><body>")
        entries.forEach { entry ->
            val entryTags = diaryTagMap[entry.id] ?: emptyList()
            appendLine("<article>")
            appendLine("<h2>${formatDate(entry.createdAt)}</h2>")
            appendLine("<div class='meta'>${buildMetaLine(entry, entryTags)}</div>")
            appendLine("<div class='content'>${entry.content}</div>")
            appendLine("</article><hr>")
        }
        appendLine("</body></html>")
    }

    return saveToFile(context, "diary_export_${timestamp()}.html", html, "text/html")
}
```

### 2.4 DOCX 导出

**优先级：低 | 工作量：中 (1 周)**

引入 Apache POI 或使用轻量级 `docx-creator`：

```kotlin
// build.gradle.kts
implementation("org.apache.poi:poi-ooxml:5.2.5") // ~15MB，较重

// 替代方案：使用 HTML 转 DOCX（HTML 兼容 Word 打开）
// 将 .html 文件扩展名改为 .doc，Word 可直接打开
// 零依赖，效果可接受
fun exportAsDocCompat(context: Context, entry: DiaryEntry, tags: List<String>): String {
    val html = buildStyledHtml(entry, tags)
    return saveToFile(context, "diary_${entry.id}.doc", html, "application/msword")
}
```

**推荐方案：** HTML 转 .doc 兼容方案。Apache POI 体积太大，对于日记应用来说不值得。

### 2.5 批量选择性导出

**优先级：中 | 工作量：小 (2-3 天)**

在 BackupScreen 和 TimelineScreen 中增加多选模式：

```kotlin
// 新增导出选项 BottomSheet
@Composable
fun ExportOptionsSheet(
    selectedEntries: List<Long>,
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("导出 ${selectedEntries.size} 篇日记", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            ExportOption(format = ExportFormat.PDF, icon = Icons.Default.PictureAsPdf,
                title = "PDF 文件", subtitle = "适合打印和归档")
            ExportOption(format = ExportFormat.HTML, icon = Icons.Default.Code,
                title = "HTML 文件", subtitle = "可在浏览器中查看")
            ExportOption(format = ExportFormat.MARKDOWN, icon = Icons.Default.Description,
                title = "Markdown", subtitle = "纯文本格式")
            ExportOption(format = ExportFormat.JSON, icon = Icons.Default.DataObject,
                title = "JSON 备份", subtitle = "完整数据，可导入恢复")
        }
    }
}

enum class ExportFormat { PDF, HTML, MARKDOWN, JSON }
```

### 2.6 Export 模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| PDF 导出 (WebView) | 高 | 1 周 | Phase 1 |
| HTML 导出 | 中 | 1-2 天 | Phase 1 |
| 批量选择性导出 | 中 | 2-3 天 | Phase 2 |
| DOCX 导出 (HTML兼容) | 低 | 1 天 | Phase 3 |

---

## 3. Security 增强

### 3.1 现状分析

当前 `BiometricHelper` 实现：

| 特性 | 现状 | 问题 |
|------|------|------|
| PIN 存储 | SHA-256 哈希，存 SharedPreferences | 无盐值，彩虹表可破 |
| PIN 长度 | 4 位数字 | 仅 10000 种组合 |
| 锁定机制 | 5 次失败后锁定 30 秒 | 时间太短，暴力破解成本低 |
| 生物识别 | BiometricPrompt API | 实现正确 |
| 数据加密 | 无 | 数据库文件明文存储在 `/data/data/` |
| 锁定超时 | 无 | 每次进入 app 都需要解锁 |
| 隐私模式 | 无 | app 预览图可能泄露日记内容 |

### 3.2 方案一：锁定超时设置

**优先级：高 | 工作量：小 (1-2 天)**

用户需要在短时间内反复进出 app 时，每次都要输 PIN 很烦。增加超时选项：

```kotlin
enum class LockTimeout(val label: String, val durationMs: Long) {
    IMMEDIATE("立即", 0),
    ONE_MINUTE("1 分钟后", 60_000),
    FIVE_MINUTES("5 分钟后", 300_000),
    FIFTEEN_MINUTES("15 分钟后", 900_000),
    ONE_HOUR("1 小时后", 3_600_000);
}

// BiometricHelper 新增：
fun shouldRequireAuth(context: Context): Boolean {
    if (!isLockEnabled(context)) return false
    val lastUnlock = getPrefs(context).getLong(KEY_LAST_UNLOCK_TIME, 0)
    val timeout = getLockTimeout(context).durationMs
    return System.currentTimeMillis() - lastUnlock > timeout
}

fun onUnlockSuccess(context: Context) {
    getPrefs(context).edit()
        .putLong(KEY_LAST_UNLOCK_TIME, System.currentTimeMillis())
        .putInt(KEY_FAILED_ATTEMPTS, 0)
        .remove(KEY_LOCKOUT_UNTIL)
        .apply()
}
```

**UI 变更：** 在 SettingsScreen 的"隐私"分区增加"自动锁定超时"选项。

### 3.3 方案二：SQLCipher 数据库加密

**优先级：中 | 工作量：中 (3-5 天)**

即使 app 有 PIN 锁，root 设备或 adb 备份仍可读取数据库文件。SQLCipher 对整个数据库加密：

```kotlin
// build.gradle.kts
implementation("net.zetetic:android-database-sqlcipher:4.5.6")
implementation("androidx.sqlite:sqlite-ktx:2.4.0")

// DiaryDatabase.kt 修改
fun getDatabase(context: Context): DiaryDatabase {
    return INSTANCE ?: synchronized(this) {
        val passphrase = getOrCreateDatabaseKey(context)
        val factory = SupportFactory(passphrase)
        val instance = Room.databaseBuilder(
            context.applicationContext,
            DiaryDatabase::class.java,
            "diary_database"
        )
            .openHelperFactory(factory)  // 关键变更
            .addMigrations(/* ... */)
            .build()
        INSTANCE = instance
        instance
    }
}

// 数据库密钥存储在 Android Keystore
private fun getOrCreateDatabaseKey(context: Context): ByteArray {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    val alias = "diary_db_key"
    if (!keyStore.containsAlias(alias)) {
        // 生成 256-bit 随机密钥
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        // 用 Keystore 加密后存储到 SharedPreferences
        encryptAndStoreKey(context, key, alias)
    }
    return loadAndDecryptKey(context, alias)
}
```

**迁移注意：**
- 从明文数据库迁移到 SQLCipher 需要导出再导入
- 首次升级时：读取明文 DB -> 创建加密 DB -> 写入数据 -> 删除明文 DB
- 这个过程需要在 Migration 中完成，耗时可能较长
- **建议在用户确认后手动触发，而非静默迁移**

### 3.4 方案三：假 PIN（Decoy PIN）

**优先级：低 | 工作量：小 (1-2 天)**

在胁迫场景下，输入假 PIN 显示一个空的日记界面：

```kotlin
// BiometricHelper 新增
fun setDecoyPin(context: Context, pin: String) {
    val hash = hashPin(pin)
    getPrefs(context).edit()
        .putString(KEY_DECOY_PIN_HASH, hash)
        .apply()
}

fun verifyPinWithResult(context: Context, pin: String): PinResult {
    val prefs = getPrefs(context)
    if (isLockedOut(context)) return PinResult.LOCKED_OUT

    val realHash = prefs.getString(KEY_PIN_HASH, null)
    val decoyHash = prefs.getString(KEY_DECOY_PIN_HASH, null)

    return when {
        hashPin(pin) == realHash -> {
            resetFailedAttempts(context)
            PinResult.SUCCESS
        }
        decoyHash != null && hashPin(pin) == decoyHash -> {
            resetFailedAttempts(context)
            PinResult.DECOY  // 返回假界面
        }
        else -> {
            incrementFailedAttempts(context)
            PinResult.WRONG
        }
    }
}

enum class PinResult { SUCCESS, DECOY, WRONG, LOCKED_OUT }
```

**UI 变更：** MainActivity 收到 `DECOY` 结果后，导航到一个只有预设日记的"假主页"。

### 3.5 方案四：隐私模式

**优先级：中 | 工作量：小 (1 天)**

防止 app 预览图（最近任务卡片）泄露日记内容：

```kotlin
// MainActivity.kt
class MainActivity : FragmentActivity() {
    override fun onPause() {
        super.onPause()
        if (BiometricHelper.isLockEnabled(this)) {
            // 隐藏预览
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // 恢复预览（解锁后）
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
```

或者更简单地在 AndroidManifest.xml 中设置：
```xml
<activity
    android:name=".MainActivity"
    android:windowSoftInputMode="adjustResize">
    <!-- 在代码中动态设置 FLAG_SECURE -->
</activity>
```

### 3.6 方案五：PIN 安全增强

**优先级：中 | 工作量：小 (1 天)**

```kotlin
// 改进哈希：加盐 + PBKDF2 迭代
fun hashPin(pin: String, salt: ByteArray? = null): Pair<String, ByteArray> {
    val actualSalt = salt ?: ByteArray(16).also { SecureRandom().nextBytes(it) }
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(pin.toCharArray(), actualSalt, 100_000, 256)
    val hash = factory.generateSecret(spec).encoded
    return hash.joinToString("") { "%02x".format(it) } to actualSalt
}

// 存储格式：salt:hash
fun setPin(context: Context, pin: String, hint: String = "") {
    val (hash, salt) = hashPin(pin)
    val saltHex = salt.joinToString("") { "%02x".format(it) }
    getPrefs(context).edit()
        .putString(KEY_PIN_HASH, "$saltHex:$hash")
        .putBoolean(KEY_PIN_LOCK, true)
        .putString(KEY_PIN_HINT, hint)
        .apply()
}
```

**PIN 长度选项：** 支持 4 位 / 6 位 / 自定义长度

```kotlin
enum class PinLength(val digits: Int, val label: String) {
    FOUR(4, "4 位数字"),
    SIX(6, "6 位数字"),
    CUSTOM(-1, "自定义");
}
```

### 3.7 Security 模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| 锁定超时设置 | 高 | 1-2 天 | Phase 1 |
| 隐私模式 (FLAG_SECURE) | 中 | 1 天 | Phase 1 |
| PIN 加盐 PBKDF2 | 中 | 1 天 | Phase 1 |
| SQLCipher 加密 | 中 | 3-5 天 | Phase 2 |
| 6 位 PIN / 自定义长度 | 中 | 1 天 | Phase 2 |
| 假 PIN | 低 | 1-2 天 | Phase 3 |

---

## 4. Stats 重构

### 4.1 现状分析

当前 `StatsViewModel` 的 `StatsState` 包含：

```kotlin
data class StatsState(
    val totalEntries: Int,
    val currentStreak: Int,
    val thisMonthEntries: Int,
    val moodDistribution: List<MoodStat>,
    val weatherDistribution: List<WeatherStat>,
    val tagUsage: List<TagUsage>,
    val monthlyTrend: List<MonthTrend>,
    val writingHabit: WritingHabit?,
    val moodTrend: MoodTrend?,
    val wordStats: WordStats?,
    val heatmapData: List<HeatmapDay>,
    val heatmapRange: HeatmapRange, // 只有 ONE_MONTH(30天)
)
```

**问题：**

| 问题 | 详情 |
|------|------|
| 热力图仅 30 天 | 无法看到全年写作习惯，GitHub 风格的年热力图更直观 |
| 图表不可交互 | 无法点击查看某天/某月的详情 |
| 没有时间范围选择 | 只能看最近 6 个月趋势，无法查看特定年份 |
| 心情趋势算法粗糙 | 仅比较近 30 天 vs 前 30 天平均值 |
| 没有写作频率分析 | 缺少"最常写作的时间段"热力图 |
| 无比较分析 | 无法对比"本月 vs 上月"或"今年 vs 去年" |
| 统计指标不足 | 缺少：最长连续天数、平均每篇字数趋势、标签多样性指数 |

### 4.2 年热力图（GitHub 风格）

**优先级：高 | 工作量：中 (3-5 天)**

```kotlin
// 扩展 HeatmapRange
enum class HeatmapRange(val days: Int, val label: String) {
    ONE_MONTH(30, "近 30 天"),
    THREE_MONTHS(91, "近 3 个月"),
    SIX_MONTHS(182, "近半年"),
    ONE_YEAR(365, "近一年");
}

// 新增年热力图 Composable
@Composable
fun YearHeatmap(
    data: List<HeatmapDay>,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {}
) {
    // 按周分组，每列一周，共 53 列
    // 7 行代表周一到周日
    val weeks = data.chunkedByWeek()
    val maxCount = data.maxOfOrNull { it.count } ?: 1

    LazyRow(modifier = modifier) {
        items(weeks) { week ->
            Column {
                week.forEach { day ->
                    val intensity = day.count.toFloat() / maxCount.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(heatmapColor(intensity))
                            .clickable { onDayClick(day.date) }
                    )
                }
            }
        }
    }
}

// 热力图颜色：从浅到深
@Composable
private fun heatmapColor(intensity: Float): Color {
    val base = MaterialTheme.colorScheme.primary
    return when {
        intensity == 0f -> MaterialTheme.colorScheme.surfaceVariant
        intensity < 0.25f -> base.copy(alpha = 0.2f)
        intensity < 0.5f -> base.copy(alpha = 0.4f)
        intensity < 0.75f -> base.copy(alpha = 0.6f)
        else -> base.copy(alpha = 0.9f)
    }
}
```

### 4.3 交互式图表

**优先级：高 | 工作量：大 (1-2 周)**

当前图表使用 Compose Canvas 手绘，缺乏交互能力。引入图表库：

```kotlin
// 方案 A：Vico (专为 Compose 设计)
implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")

// 方案 B：自绘 + 点击检测（零依赖）
// 使用 Modifier.pointerInput + detectTapGestures
```

**推荐方案 B（自绘 + 点击检测）**，理由：
- 不增加依赖
- 与现有设计语言完全一致
- 日记 app 的图表需求简单，不需要复杂图表库

```kotlin
@Composable
fun InteractiveBarChart(
    data: List<MonthTrend>,
    onBarClick: (MonthTrend) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val maxCount = data.maxOfOrNull { it.count } ?: 1

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val barWidth = size.width / data.size
                    val index = (offset.x / barWidth).toInt()
                    if (index in data.indices) {
                        selectedIndex = index
                        onBarClick(data[index])
                    }
                }
            }
    ) {
        val barWidth = size.width / data.size * 0.6f
        val gap = size.width / data.size * 0.4f

        data.forEachIndexed { index, trend ->
            val barHeight = (trend.count.toFloat() / maxCount) * size.height * 0.8f
            val x = index * (barWidth + gap) + gap / 2
            val color = if (index == selectedIndex) {
                Color(0xFF667EEA)
            } else {
                Color(0xFF667EEA).copy(alpha = 0.6f)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}
```

### 4.4 心情趋势增强

**优先级：中 | 工作量：中 (3-5 天)**

```kotlin
// 新增更丰富的心情分析
data class EnhancedMoodTrend(
    val recent30Avg: Double?,
    val previous30Avg: Double?,
    val direction: TrendDirection,
    val monthlyMoods: List<MonthlyMood>,     // 月度心情曲线
    val weekdayMoods: List<WeekdayMood>,     // 每周各天平均心情
    val bestDay: LocalDate?,                  // 心情最好的一天
    val worstDay: LocalDate?,                 // 心情最差的一天
    val moodVolatility: Double,              // 心情波动指数 (0-1)
)

data class MonthlyMood(
    val month: String,
    val avgMood: Double,
    val entryCount: Int,
)

data class WeekdayMood(
    val dayOfWeek: Int,  // 1=Monday
    val dayName: String,
    val avgMood: Double,
)

// 心情波动指数计算
fun computeMoodVolatility(entries: List<DiaryPreview>): Double {
    val moods = entries.mapNotNull { it.moodLevel?.toDouble() }
    if (moods.size < 2) return 0.0
    val mean = moods.average()
    val variance = moods.map { (it - mean).pow(2) }.average()
    val stdDev = sqrt(variance)
    // 归一化到 0-1（心情范围 1-6，最大标准差约 2.5）
    return (stdDev / 2.5).coerceIn(0.0, 1.0)
}
```

### 4.5 写作习惯分析增强

**优先级：中 | 工作量：小 (2-3 天)**

```kotlin
data class EnhancedWritingHabit(
    val avgPerWeek: Double,
    val mostActiveDay: String,
    val mostActiveTime: String,
    val longestStreak: Int,              // 历史最长连续天数
    val currentStreak: Int,              // 当前连续天数
    val avgWordsPerEntry: Int,           // 平均字数
    val wordCountTrend: List<MonthWords>, // 月度字数趋势
    val writingTimeDistribution: List<TimeSlot>,  // 24小时写作分布
    val totalWritingDays: Int,           // 总写作天数
    val writingDayRatio: Double,         // 写作天数占比
)

data class TimeSlot(
    val hour: Int,       // 0-23
    val count: Int,
)

// 24 小时写作热力分布
fun computeHourlyDistribution(entries: List<DiaryPreview>): List<TimeSlot> {
    val hourCounts = entries.groupBy {
        Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).hour
    }.mapValues { it.value.size }

    return (0..23).map { hour ->
        TimeSlot(hour = hour, count = hourCounts[hour] ?: 0)
    }
}
```

### 4.6 比较分析

**优先级：低 | 工作量：小 (2-3 天)**

```kotlin
data class ComparisonStats(
    val currentPeriod: PeriodStats,
    val previousPeriod: PeriodStats,
    val changes: List<StatChange>,
)

data class PeriodStats(
    val label: String,           // "本月" / "上月"
    val entryCount: Int,
    val avgMood: Double?,
    val totalWords: Int,
    val topTags: List<String>,
    val writingDays: Int,
)

data class StatChange(
    val metric: String,          // "日记数量"
    val currentValue: Double,
    val previousValue: Double,
    val changePercent: Double,   // +25% / -10%
    val direction: TrendDirection,
)

// UI: 对比卡片
@Composable
fun ComparisonCard(comparison: ComparisonStats) {
    GlassCard {
        Column {
            Text("${comparison.currentPeriod.label} vs ${comparison.previousPeriod.label}")
            comparison.changes.forEach { change ->
                StatChangeRow(change)
            }
        }
    }
}
```

### 4.7 Stats 模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| 年热力图 (可滚动) | 高 | 3-5 天 | Phase 1 |
| 月度柱状图点击交互 | 高 | 2-3 天 | Phase 1 |
| 心情趋势折线图 | 中 | 3-5 天 | Phase 1 |
| 24 小时写作分布 | 中 | 2-3 天 | Phase 2 |
| 历史最长连续天数 | 中 | 1 天 | Phase 2 |
| 月度字数趋势 | 中 | 2 天 | Phase 2 |
| 比较分析 (本月 vs 上月) | 低 | 2-3 天 | Phase 3 |
| 心情波动指数 | 低 | 1 天 | Phase 3 |

---

## 5. Settings 重构

### 5.1 现状分析

当前 `SettingsScreen` 结构：

```
外 Appearance
  └─ 主题 (跳转)

数据
  ├─ 备份 (跳转 BackupScreen)
  └─ 分类管理 (跳转 TagManagementScreen)

隐私
  └─ 应用锁 (跳转)

关于
  ├─ 版本号
  ├─ 检查更新
  └─ 更新日志
```

**问题：**

| 问题 | 详情 |
|------|------|
| 设置项过少 | 整个页面只有 6 个可点击项，显得空旷 |
| 缺少搜索 | 设置项多后难以查找 |
| 主题选择是跳转而非内联 | 点击后跳到新页面，体验割裂 |
| 缺少字体大小设置 | 用户提到有 font size slider，但当前 SettingsScreen 中未体现 |
| 缺少语言设置 | 国际化支持不完整 |
| 无导入功能入口 | BackupScreen 有导出但没有导入入口 |
| 设置不可导出 | 换机时设置（主题、字号等）丢失 |

### 5.2 设置项重组

**优先级：高 | 工作量：中 (3-5 天)**

```kotlin
// 新的设置分组结构
enum class SettingsSection(
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    APPEARANCE("外观", Icons.Default.Palette, primaryColor),
    EDITOR("编辑器", Icons.Default.Edit, secondaryColor),
    DATA("数据管理", Icons.Default.Storage, tertiaryColor),
    PRIVACY("隐私与安全", Icons.Default.Security, errorColor),
    NOTIFICATIONS("通知", Icons.Default.Notifications, warningColor),
    ABOUT("关于", Icons.Default.Info, primaryColor);
}
```

**详细设置项：**

```
外观
  ├─ 主题模式 (浅色/深色/跟随系统) -- 内联 RadioGroup
  ├─ 主题色 (预设色板选择) -- 内联色块
  ├─ 字体大小 (slider: 12sp - 24sp)
  └─ 日历起始日 (周一/周日)

编辑器
  ├─ 默认字体 (衬线/无衬线)
  ├─ 自动保存间隔
  └─ 图片压缩质量

数据管理
  ├─ 备份与恢复 (跳转 BackupScreen)
  ├─ 导入日记 (JSON)
  ├─ 导出日记 (跳转 ExportOptions)
  ├─ 分类管理 (跳转 TagManagementScreen)
  ├─ 存储空间使用
  └─ 清除缓存

隐私与安全
  ├─ 应用锁 (跳转)
  ├─ 自动锁定超时
  ├─ 隐私模式 (FLAG_SECURE)
  └─ 数据库加密

通知
  ├─ 写作提醒 (跳转 ReminderManager)
  └─ 倒计时提醒

关于
  ├─ 版本号
  ├─ 检查更新
  ├─ 更新日志
  ├─ 隐私政策
  └─ 反馈与建议
```

### 5.3 主题选择内联化

**优先级：高 | 工作量：小 (1-2 天)**

当前点击"主题"会跳转到新页面。改为在设置页内直接选择：

```kotlin
@Composable
fun InlineThemeSelector(
    currentMode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("主题模式", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeOptionCard(
                    mode = mode,
                    isSelected = mode == currentMode,
                    onClick = { onModeChange(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        // 预览圆角矩形
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(mode.previewColor())
        )
        Spacer(Modifier.height(8.dp))
        Text(mode.label, fontSize = 13.sp)
    }
}
```

### 5.4 字体大小滑块

**优先级：中 | 工作量：小 (1 天)**

```kotlin
@Composable
fun FontSizeSlider(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("字体大小", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${currentSize.toInt()}sp", fontSize = 14.sp, color = textSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", fontSize = 12.sp, color = textTertiary)
            Slider(
                value = currentSize,
                onValueChange = onSizeChange,
                valueRange = 12f..24f,
                steps = 11,  // 12, 13, ..., 24
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("A", fontSize = 20.sp, color = textTertiary)
        }
        // 实时预览
        Text(
            "预览文字大小",
            fontSize = currentSize.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
```

### 5.5 存储空间管理

**优先级：中 | 工作量：小 (2 天)**

```kotlin
@Composable
fun StorageUsageSection(dao: DiaryDao, context: Context) {
    var usage by remember { mutableStateOf<StorageUsage?>(null) }

    LaunchedEffect(Unit) {
        usage = computeStorageUsage(context, dao)
    }

    usage?.let { data ->
        GlassCard {
            Column {
                Text("存储空间", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                // 进度条
                LinearProgressIndicator(
                    progress = { data.usedBytes.toFloat() / data.totalBytes },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                StorageRow("日记数据库", data.databaseSize)
                StorageRow("图片", data.imageSize)
                StorageRow("备份文件", data.backupSize)
                StorageRow("缓存", data.cacheSize)
            }
        }
    }
}

data class StorageUsage(
    val databaseSize: Long,
    val imageSize: Long,
    val backupSize: Long,
    val cacheSize: Long,
    val usedBytes: Long,
    val totalBytes: Long,
)

suspend fun computeStorageUsage(context: Context, dao: DiaryDao): StorageUsage {
    val dbFile = context.getDatabasePath("diary_database")
    val dbSize = if (dbFile.exists()) dbFile.length() else 0L

    val imageDir = File(context.filesDir, "images")
    val imageSize = imageDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    val backupDir = File(context.filesDir, "backups")
    val backupSize = backupDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    val cacheSize = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    val stat = StatFs(context.filesDir.path)
    val totalBytes = stat.totalBytes

    return StorageUsage(
        databaseSize = dbSize,
        imageSize = imageSize,
        backupSize = backupSize,
        cacheSize = cacheSize,
        usedBytes = dbSize + imageSize + backupSize,
        totalBytes = totalBytes,
    )
}
```

### 5.6 Settings 模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| 设置项重组 (分组) | 高 | 3-5 天 | Phase 1 |
| 主题选择内联化 | 高 | 1-2 天 | Phase 1 |
| 字体大小滑块 | 中 | 1 天 | Phase 1 |
| 存储空间管理 | 中 | 2 天 | Phase 2 |
| 设置导入/导出 | 低 | 2 天 | Phase 3 |
| 设置搜索 | 低 | 2-3 天 | Phase 3 |

---

## 6. CountDown 增强

### 6.1 现状分析

当前 `CountDownItem` 数据模型：

```kotlin
@Entity(tableName = "countdown_items")
data class CountDownItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: Long,
    val isCountUp: Boolean = false,
    val color: Long = 0xFF4A90D9,
    val isRepeatYearly: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

**现有 Widget：** `CountDownWidgetProvider` 使用 `RemoteViewsService` 实现列表 Widget。

**问题：**

| 问题 | 详情 |
|------|------|
| Widget 只有列表样式 | 缺少单个倒计时的大 Widget |
| 无到期提醒 | 倒计时归零时没有通知 |
| 重复类型有限 | 只有年度重复，缺少月度/周度/自定义间隔 |
| 无图标选择 | 只有颜色，没有图标区分不同倒计时 |
| 无分组/分类 | 所有倒计时混在一起 |
| Widget 更新不及时 | 依赖系统调度，可能延迟 |

### 6.2 倒计时到期通知

**优先级：高 | 工作量：中 (3-5 天)**

```kotlin
// 使用 AlarmManager 在目标日期触发通知
class CountDownReminderManager(private val context: Context) {

    fun scheduleReminder(item: CountDownItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CountDownReminderReceiver::class.java).apply {
            putExtra("countdown_id", item.id)
            putExtra("countdown_title", item.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = if (item.isRepeatYearly) {
            // 计算下一次年度重复日期
            getNextYearlyDate(item.targetDate)
        } else {
            item.targetDate
        }

        // 提前 1 天提醒（可配置）
        val reminderTime = triggerTime - 24 * 60 * 60 * 1000

        if (reminderTime > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    fun cancelReminder(itemId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CountDownReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, itemId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}

// BroadcastReceiver
class CountDownReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("countdown_id", 0)
        val title = intent.getStringExtra("countdown_title") ?: return

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(context, "countdown_channel")
            .setSmallIcon(R.drawable.ic_countdown)
            .setContentTitle("倒计时提醒")
            .setContentText("$title 即将到期")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, id.toInt(),
                    Intent(context, MainActivity::class.java).apply {
                        putExtra("navigate_to", "countdown")
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        notificationManager.notify(id.toInt(), notification)
    }
}
```

### 6.3 大尺寸单倒计时 Widget

**优先级：中 | 工作量：中 (3-5 天)**

当前只有列表 Widget。增加一个展示单个倒计时的大 Widget：

```xml
<!-- res/layout/widget_countdown_single.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:background="@drawable/widget_rounded_bg"
    android:padding="16dp">

    <TextView
        android:id="@+id/tv_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="@color/text_secondary" />

    <TextView
        android:id="@+id/tv_days"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="48sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary" />

    <TextView
        android:id="@+id/tv_unit"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="@color/text_secondary" />

    <ProgressBar
        android:id="@+id/progress"
        style="@android:style/Widget.ProgressBar.Horizontal"
        android:layout_width="match_parent"
        android:layout_height="4dp"
        android:layout_marginTop="8dp" />
</LinearLayout>
```

```kotlin
class CountDownSingleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val itemId = prefs.getLong("widget_$appWidgetId", -1)

        scope.launch {
            val db = DiaryDatabase.getDatabase(context)
            val item = db.diaryDao().getCountDownItemById(itemId) ?: return@launch

            val days = getDaysRemaining(item)
            val views = RemoteViews(context.packageName, R.layout.widget_countdown_single).apply {
                setTextViewText(R.id.tv_title, item.title)
                setTextViewText(R.id.tv_days, days.toString())
                setTextViewText(R.id.tv_unit, if (days == 1L) "天" else "天")
                // 设置颜色
                setInt(R.id.tv_days, "setTextColor", item.color.toInt())
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
```

### 6.4 更多重复类型

**优先级：中 | 工作量：小 (2 天)**

```kotlin
enum class RepeatType(val label: String) {
    NONE("不重复"),
    YEARLY("每年"),
    MONTHLY("每月"),
    WEEKLY("每周"),
    CUSTOM("自定义间隔");
}

// CountDownItem 扩展
@Entity(tableName = "countdown_items")
data class CountDownItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: Long,
    val isCountUp: Boolean = false,
    val color: Long = 0xFF4A90D9,
    val isRepeatYearly: Boolean = false,  // 保留兼容
    val repeatType: String = "NONE",      // 新增
    val customRepeatDays: Int = 0,        // 自定义间隔天数
    val icon: String = "timer",           // 新增：Material icon 名称
    val category: String = "",            // 新增：分组类别
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = false, // 新增：是否启用提醒
    val reminderDaysBefore: Int = 1,     // 新增：提前几天提醒
)

// 计算下一次重复日期
fun getNextRepeatDate(item: CountDownItem): LocalDate {
    val target = Instant.ofEpochMilli(item.targetDate)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()

    return when (RepeatType.valueOf(item.repeatType)) {
        RepeatType.NONE -> target
        RepeatType.YEARLY -> {
            var next = target.withYear(today.year)
            if (next.isBefore(today)) next = next.plusYears(1)
            next
        }
        RepeatType.MONTHLY -> {
            var next = target.withYear(today.year).withMonth(today.monthValue)
            if (next.isBefore(today)) next = next.plusMonths(1)
            next
        }
        RepeatType.WEEKLY -> {
            var next = target.with(today.temporal.IsoFields.WEEK_BASED_YEAR, today.year)
            while (next.isBefore(today)) next = next.plusWeeks(1)
            next
        }
        RepeatType.CUSTOM -> {
            var next = target
            while (next.isBefore(today)) next = next.plusDays(item.customRepeatDays.toLong())
            next
        }
    }
}
```

### 6.5 倒计时图标选择

**优先级：低 | 工作量：小 (1 天)**

```kotlin
val countdownIcons = listOf(
    "timer" to Icons.Default.Timer,
    "cake" to Icons.Default.Cake,
    "flight" to Icons.Default.Flight,
    "school" to Icons.Default.School,
    "work" to Icons.Default.Work,
    "favorite" to Icons.Default.Favorite,
    "celebration" to Icons.Default.Celebration,
    "fitness_center" to Icons.Default.FitnessCenter,
    "book" to Icons.Default.MenuBook,
    "travel" to Icons.Default.TravelExplore,
    "gift" to Icons.Default.CardGiftcard,
    "event" to Icons.Default.Event,
)
```

### 6.6 CountDown 模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| 到期通知提醒 | 高 | 3-5 天 | Phase 1 |
| 大尺寸单 Widget | 中 | 3-5 天 | Phase 2 |
| 更多重复类型 | 中 | 2 天 | Phase 2 |
| 图标选择 | 低 | 1 天 | Phase 2 |
| 分组/类别 | 低 | 2 天 | Phase 3 |

---

## 7. Tag 增强

### 7.1 现状分析

当前 Tag 模型极其简单：

```kotlin
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val isPreset: Boolean = false
)
```

`TagManagementScreen` 提供了基本的 CRUD + 备份/恢复功能。

**问题：**

| 问题 | 详情 |
|------|------|
| 无层级结构 | 所有标签平铺，无法分类管理（如"旅行 > 国内"） |
| 无图标 | 只有颜色圆点，缺乏视觉辨识度 |
| 无智能文件夹 | 无法按条件自动归类 |
| 无合并/重命名 | 删除重建会导致关联关系丢失 |
| 无排序 | 标签按创建顺序排列，无法自定义 |
| 无使用统计 | 不知道哪些标签用得多 |
| 颜色选择固定 12 色 | 用户可能需要更多颜色 |

### 7.2 标签层级结构

**优先级：中 | 工作量：大 (1-2 周)**

```kotlin
// 扩展 Tag 模型
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val icon: String = "",           // Material icon 名称
    val parentId: Long? = null,      // 父标签 ID
    val isPreset: Boolean = false,
    val sortOrder: Int = 0,          // 排序权重
    val createdAt: Long = System.currentTimeMillis(),
)

// 查询：获取标签树
@Query("""
    SELECT t.*, COUNT(dt.diaryId) as usageCount
    FROM tags t
    LEFT JOIN diary_tag_cross_ref dt ON t.id = dt.tagId
    GROUP BY t.id
    ORDER BY t.parentId NULLS FIRST, t.sortOrder, t.name
""")
fun getTagTree(): Flow<List<TagWithCount>>

data class TagWithCount(
    val id: Long,
    val name: String,
    val color: Long,
    val icon: String,
    val parentId: Long?,
    val isPreset: Boolean,
    val sortOrder: Int,
    val usageCount: Int,
)

// DAO：合并标签
@Query("UPDATE diary_tag_cross_ref SET tagId = :targetTagId WHERE tagId = :sourceTagId")
suspend fun mergeTagRefs(sourceTagId: Long, targetTagId: Long)

@Transaction
suspend fun mergeTags(sourceTagId: Long, targetTagId: Long) {
    mergeTagRefs(sourceTagId, targetTagId)
    deleteTagById(sourceTagId)
}

// DAO：重命名标签
@Query("UPDATE tags SET name = :newName WHERE id = :tagId")
suspend fun renameTag(tagId: Long, newName: String)
```

### 7.3 标签图标

**优先级：低 | 工作量：小 (1 天)**

```kotlin
val tagIconOptions = listOf(
    "" to Icons.Default.Label,           // 默认
    "work" to Icons.Default.Work,
    "home" to Icons.Default.Home,
    "school" to Icons.Default.School,
    "fitness" to Icons.Default.FitnessCenter,
    "restaurant" to Icons.Default.Restaurant,
    "travel" to Icons.Default.TravelExplore,
    "music" to Icons.Default.MusicNote,
    "book" to Icons.Default.MenuBook,
    "camera" to Icons.Default.CameraAlt,
    "heart" to Icons.Default.Favorite,
    "star" to Icons.Default.Star,
    "pet" to Icons.Default.Pets,
    "garden" to Icons.Default.Yard,
    "code" to Icons.Default.Code,
    "brush" to Icons.Default.Brush,
)

// TagChip UI 更新
@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    val icon = tagIconOptions.find { it.first == tag.icon }?.second ?: Icons.Default.Label
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(tag.color).copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(tag.color), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(tag.name, fontSize = 13.sp, color = Color(tag.color))
    }
}
```

### 7.4 智能文件夹

**优先级：低 | 工作量：中 (1 周)**

智能文件夹不是数据库中的实体，而是基于规则的动态查询：

```kotlin
data class SmartFolder(
    val id: Long,
    val name: String,
    val icon: String,
    val rules: List<FilterRule>,
    val sortOrder: Int,
)

sealed class FilterRule {
    data class HasTag(val tagId: Long) : FilterRule()
    data class MoodAbove(val level: Int) : FilterRule()
    data class MoodBelow(val level: Int) : FilterRule()
    data class HasLocation(val has: Boolean) : FilterRule()
    data class HasImages(val has: Boolean) : FilterRule()
    data class DateRange(val from: LocalDate, val to: LocalDate) : FilterRule()
    data class ContainsText(val keyword: String) : FilterRule()
    data class WordCountAbove(val count: Int) : FilterRule()
}

// 将规则转换为 SQL WHERE 子句
fun FilterRule.toSqlClause(): String = when (this) {
    is FilterRule.HasTag -> "e.id IN (SELECT diaryId FROM diary_tag_cross_ref WHERE tagId = $tagId)"
    is FilterRule.MoodAbove -> "e.moodLevel >= $level"
    is FilterRule.MoodBelow -> "e.moodLevel <= $level"
    is FilterRule.HasLocation -> if (has) "e.location IS NOT NULL AND e.location != ''" else "e.location IS NULL OR e.location = ''"
    // ...
}
```

### 7.5 标签合并 UI

**优先级：中 | 工作量：小 (2 天)**

```kotlin
@Composable
fun TagMergeDialog(
    tags: List<Tag>,
    onMerge: (sourceIds: List<Long>, targetId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSources by remember { mutableStateOf(setOf<Long>()) }
    var selectedTarget by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("合并标签") },
        text = {
            Column {
                Text("选择要合并的标签：", fontSize = 14.sp)
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedSources = if (tag.id in selectedSources)
                                selectedSources - tag.id else selectedSources + tag.id
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tag.id in selectedSources, onCheckedChange = null)
                        Text(tag.name)
                    }
                }

                if (selectedSources.size >= 2) {
                    Spacer(Modifier.height(16.dp))
                    Text("合并到：", fontSize = 14.sp)
                    tags.filter { it.id in selectedSources }.forEach { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedTarget = tag.id },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = tag.id == selectedTarget, onClick = null)
                            Text(tag.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = selectedTarget ?: return@TextButton
                    val sources = selectedSources.filter { it != target }
                    onMerge(sources, target)
                },
                enabled = selectedSources.size >= 2 && selectedTarget != null
            ) { Text("合并") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
```

### 7.6 更多颜色选项

**优先级：低 | 工作量：小 (1 天)**

```kotlin
// 扩展 presetColors 到 24 色，并支持自定义颜色
val presetColors = listOf(
    // 原有 12 色
    0xFF667EEA, 0xFFE74C3C, 0xFF2ECC71, 0xFFE67E22,
    0xFF9B59B6, 0xFF1ABC9C, 0xFFF1C40F, 0xFFE91E63,
    0xFF3498DB, 0xFF95A5A6, 0xFF34495E, 0xFFD35400,
    // 新增 12 色（更柔和的色调）
    0xFF7C4DFF, 0xFF00BCD4, 0xFF8BC34A, 0xFFFF9800,
    0xFF607D8B, 0xFFE91E63, 0xFF009688, 0xFFCDDC39,
    0xFF795548, 0xFFFF5722, 0xFF3F51B5, 0xFF00ACC1,
)

// 支持自定义颜色（使用 Compose ColorPicker 或简单的 HEX 输入）
@Composable
fun CustomColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    var hexInput by remember { mutableStateOf(selectedColor.toHexString()) }

    Column {
        OutlinedTextField(
            value = hexInput,
            onValueChange = { hex ->
                hexInput = hex
                hex.toLongOrNull(16)?.let { onColorSelected(it or 0xFF000000) }
            },
            label = { Text("自定义颜色 (HEX)") },
            prefix = { Text("#") },
            singleLine = true,
        )
        // 预览
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(selectedColor))
        )
    }
}
```

### 7.7 Tag 模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| 标签合并 | 中 | 2 天 | Phase 1 |
| 标签重命名 (已有) | - | 已实现 | - |
| 更多颜色 + 自定义 | 低 | 1 天 | Phase 2 |
| 标签图标 | 低 | 1 天 | Phase 2 |
| 标签排序 | 中 | 1 天 | Phase 2 |
| 标签层级结构 | 中 | 1-2 周 | Phase 3 |
| 智能文件夹 | 低 | 1 周 | Phase 3 |

---

## 8. Update 改进

### 8.1 现状分析

当前更新机制基于 GitHub Releases：
- `BuildConfig.GITHUB_OWNER` / `GITHUB_REPO` 配置
- 通过 GitHub API 检查最新 release
- 下载 APK 安装

**问题：**

| 问题 | 详情 |
|------|------|
| 全量 APK 下载 | 每次更新都是完整 APK (~15-20MB) |
| 无增量更新 | 小版本改动也要下载完整包 |
| 无 staged rollout | 无法灰度发布，bug 影响全部用户 |
| 无更新强制策略 | 用户可以永远跳过更新 |
| 更新检查时机 | 不清楚是启动时检查还是后台检查 |
| 无更新日志内联 | 用户需要跳转才能看到更新内容 |

### 8.2 增量更新 (Diff Update)

**优先级：低 | 工作量：大 (2-3 周)**

Android 原生不支持 APK diff，但可以通过以下方式实现：

```kotlin
// 方案 A：使用 bsdiff 生成补丁
// 服务端：bsdiff old.apk new.apk patch.patch
// 客户端：bspatch old.apk new.apk patch.patch

// 方案 B：使用 Google Play Core 的 In-App Updates API
// 仅适用于 Google Play 渠道

// 方案 C（推荐）：利用 GitHub Release 的 binary diff
// GitHub 不原生支持，但可以自行实现：
// 1. 上传新版本时同时上传 diff 补丁
// 2. 客户端下载 diff 而非全量
// 3. 本地合并生成新 APK

// 简化方案：只对 experimental 用户做增量
class DiffUpdater(private val context: Context) {
    suspend fun downloadPatch(currentVersion: String, targetVersion: String): File? {
        return try {
            val url = "https://github.com/$OWNER/$REPO/releases/download/$targetVersion/patch-${currentVersion}-${targetVersion}.bsdiff"
            downloadFile(url)
        } catch (e: Exception) {
            null // 回退到全量下载
        }
    }

    suspend fun applyPatch(currentApk: File, patchFile: File): File {
        val newApk = File(context.cacheDir, "update.apk")
        BsPatch.patch(currentApk.path, newApk.path, patchFile.path)
        return newApk
    }
}
```

**现实考虑：** 对于个人开发者项目，增量更新的实现和维护成本过高。**建议暂缓，优先使用 Google Play 的自动更新机制。**

### 8.3 Staged Rollout

**优先级：中 | 工作量：中 (3-5 天)**

通过 GitHub Releases 的 `prerelease` 标签实现灰度：

```kotlin
class StagedRolloutManager(private val context: Context) {

    fun shouldReceiveUpdate(release: GitHubRelease): Boolean {
        // 1. 检查是否为灰度发布
        val rolloutPercent = release.body.extractRolloutPercent()
        if (rolloutPercent >= 100) return true

        // 2. 基于设备 ID hash 决定是否在灰度范围内
        val deviceId = getDeviceIdHash()
        val bucket = deviceId % 100
        return bucket < rolloutPercent
    }

    private fun getDeviceIdHash(): Int {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return id.hashCode().absoluteValue % 100
    }

    private fun String.extractRolloutPercent(): Int {
        // 解析 release body 中的 rollout: 标记
        // 例如：<!-- rollout:25 --> 表示 25% 灰度
        val regex = """rollout:(\d+)""".toRegex()
        return regex.find(this)?.groupValues?.get(1)?.toIntOrNull() ?: 100
    }
}
```

**Release 流程变更：**
1. 发布 prerelease，body 中包含 `<!-- rollout:25 -->`
2. 25% 用户收到更新通知
3. 观察 crash rate，逐步提升到 50% -> 100%
4. 最终标记为正式 release

### 8.4 强制更新策略

**优先级：中 | 工作量：小 (2 天)**

```kotlin
data class UpdatePolicy(
    val minimumVersion: Int,    // 最低要求版本号
    val forceUpdate: Boolean,   // 是否强制
    val message: String,        // 提示信息
)

// 在 release body 中解析
// <!-- min-version:42 force:true message:"此版本已停用，请更新" -->

class UpdateChecker(private val context: Context) {
    suspend fun checkForUpdate(): UpdateResult {
        val latest = fetchLatestRelease() ?: return UpdateResult.NoUpdate
        val policy = parseUpdatePolicy(latest.body)
        val currentVersion = BuildConfig.VERSION_CODE

        return when {
            latest.versionCode <= currentVersion -> UpdateResult.NoUpdate
            policy.forceUpdate && currentVersion < policy.minimumVersion -> {
                UpdateResult.ForceUpdate(latest, policy.message)
            }
            else -> UpdateResult.OptionalUpdate(latest)
        }
    }
}

sealed class UpdateResult {
    object NoUpdate : UpdateResult()
    data class OptionalUpdate(val release: GitHubRelease) : UpdateResult()
    data class ForceUpdate(val release: GitHubRelease, val message: String) : UpdateResult()
}
```

```kotlin
// UI: 强制更新对话框（不可关闭）
@Composable
fun ForceUpdateDialog(release: GitHubRelease, message: String) {
    AlertDialog(
        onDismissRequest = { /* 不可关闭 */ },
        title = { Text("需要更新") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { downloadAndInstall(release) }) {
                Text("立即更新")
            }
        },
        dismissButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}
```

### 8.5 更新日志内联

**优先级：中 | 工作量：小 (1 天)**

在更新对话框中直接显示 changelog，而非跳转：

```kotlin
@Composable
fun UpdateAvailableDialog(
    release: GitHubRelease,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("发现新版本 ${release.tagName}") },
        text = {
            Column {
                Text("更新内容：", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                // Markdown 转 Text 显示
                val changelog = parseMarkdownToAnnotatedString(release.body)
                Text(
                    text = changelog,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) { Text("更新") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text("跳过此版本") }
                TextButton(onClick = onLater) { Text("稍后") }
            }
        }
    )
}
```

### 8.6 Update 模块优先级总结

| 功能 | 优先级 | 工作量 | 建议阶段 |
|------|--------|--------|----------|
| 更新日志内联 | 中 | 1 天 | Phase 1 |
| 强制更新策略 | 中 | 2 天 | Phase 1 |
| Staged rollout | 中 | 3-5 天 | Phase 2 |
| 增量更新 | 低 | 2-3 周 | Phase 3 (可选) |

---

## 9. 参考应用分析

### 9.1 Standard Notes

**定位：** 端到端加密笔记应用，强调隐私和简洁。

| 特性 | Standard Notes 做法 | 对我们的启发 |
|------|---------------------|-------------|
| 加密 | 端到端加密，服务器无法读取 | 我们至少应做本地 SQLCipher 加密 |
| 备份 | 自动加密备份到云端，支持导出加密 ZIP | 备份应默认加密 |
| 版本历史 | 每次编辑自动保存版本，可回溯 | 可考虑日记编辑历史（仅限本地） |
| 标签 | 支持嵌套标签 (tag.subtag) | 标签层级值得借鉴 |
| 编辑器 | Markdown 编辑器，支持富文本扩展 | 已有 HTML 编辑器，保持现状 |
| 跨平台 | Web/Desktop/Mobile 同步 | 暂不考虑，但数据格式应便于迁移 |

**值得借鉴的核心理念：**
- "隐私优先"：所有敏感操作默认加密
- "数据可迁移"：导出格式开放，不锁定用户
- "极简 UI"：设置项不多但每个都精心设计

### 9.2 Day One

**定位：** 最流行的日记应用之一，强调多媒体和回忆。

| 特性 | Day One 做法 | 对我们的启发 |
|------|-------------|-------------|
| 时间线 | 按时间线展示，支持地图视图 | 我们已有时间线，可加地图模式 |
| 多媒体 | 支持照片、视频、音频、手绘 | 暂不考虑视频/音频，但手绘值得做 |
| 统计 | "On This Day" 历史今天功能 | 高价值功能，实现简单 |
| 天气 | 自动记录天气 | 已有手动天气选择，可做自动 |
| 位置 | 自动记录位置 + 地图标记 | 已有位置功能，可增强地图展示 |
| Widget | 快速记录 Widget，每日提示 Widget | 快速记录 Widget 值得做 |
| Markdown | 支持 Markdown + 富文本 | 已有 HTML 编辑器 |
| 打印 | 支持打印精美日记 | PDF 导出可覆盖此需求 |
| IAP | 订阅制 ($34.99/年) | 我们免费，但可参考功能优先级 |

**值得借鉴的核心理念：**
- "On This Day"：低成本高情感价值
- 自动元数据（天气/位置/步数）：减少用户输入
- 精美导出/打印：满足用户的"实体化"需求

### 9.3 Journey (2024)

**定位：** Google Material Design 风格日记应用，强调跨平台同步。

| 特性 | Journey 做法 | 对我们的启发 |
|------|-------------|-------------|
| UI 设计 | Material Design 3，动态色彩 | 我们已用 Material 3，保持一致 |
| 统计 | 详细的情绪分析、写作统计 | 我们的统计需要增强交互性 |
| 模板 | 预设日记模板（感恩日记、梦境记录等） | 已有 TemplateManager，可扩展 |
| AI | AI 写作提示、情绪分析 | 可考虑本地 AI 提示（不依赖网络） |
| 目标 | 设定写作目标（每周 X 篇） | 高价值功能，实现简单 |
| 回顾 | 周回顾、月回顾、年度报告 | 已有 DiaryReviewScreen，可增强 |
| 同步 | Google Drive 同步 | 与我们的云备份方案一致 |
| 隐私 | 指纹/PIN + 应用锁 | 已有，可增强 |

**值得借鉴的核心理念：**
- 写作目标：激励用户坚持写日记
- AI 提示：降低写作门槛
- 回顾功能：让用户重新发现过去的美好

### 9.4 我们的优势

| 优势 | 说明 |
|------|------|
| 完全免费 | 无订阅、无广告、无 IAP |
| 本地优先 | 数据不上传，隐私有保障 |
| 轻量 | APK 小，启动快 |
| 自定义程度高 | 4 种主题 + 字体大小 + 标签系统 |
| 倒计时功能 | 市面日记 app 少有的差异化功能 |
| Todo/Habit | 集成待办和习惯追踪 |

---

## 10. 数据库迁移计划

### 10.1 当前数据库状态

- **版本：** 13
- **实体：** DiaryEntry, Tag, DiaryTag, TodoItem, TrashEntry, DiaryImage, CountDownItem, HabitRecord
- **迁移链：** 1 -> 2 -> 3 -> ... -> 13（共 12 次迁移）
- **降级策略：** `fallbackToDestructiveMigrationOnDowngrade()`

### 10.2 计划中的 Schema 变更

#### Migration 13 -> 14: CountDown 增强 + Tag 扩展

```kotlin
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // CountDown 新增字段
        db.execSQL("ALTER TABLE countdown_items ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'YEARLY'")
        db.execSQL("ALTER TABLE countdown_items ADD COLUMN customRepeatDays INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE countdown_items ADD COLUMN icon TEXT NOT NULL DEFAULT 'timer'")
        db.execSQL("ALTER TABLE countdown_items ADD COLUMN category TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE countdown_items ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE countdown_items ADD COLUMN reminderDaysBefore INTEGER NOT NULL DEFAULT 1")

        // Tag 新增字段
        db.execSQL("ALTER TABLE tags ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tags ADD COLUMN parentId INTEGER")
        db.execSQL("ALTER TABLE tags ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tags ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")

        // 更新 isRepeatYearly -> repeatType
        db.execSQL("UPDATE countdown_items SET repeatType = 'YEARLY' WHERE isRepeatYearly = 1")
        db.execSQL("UPDATE countdown_items SET repeatType = 'NONE' WHERE isRepeatYearly = 0")

        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_parentId ON tags (parentId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_countdown_items_category ON countdown_items (category)")
    }
}
```

#### Migration 14 -> 15: DiaryEntry syncVersion (增量备份支持)

```kotlin
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE diary_entries ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_syncVersion ON diary_entries (syncVersion)")
    }
}
```

#### Migration 15 -> 16: SmartFolder (智能文件夹)

```kotlin
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS smart_folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                icon TEXT NOT NULL DEFAULT 'folder',
                rules TEXT NOT NULL DEFAULT '[]',
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}
```

### 10.3 数据库版本规划

| 版本 | 变更内容 | 关联功能 |
|------|----------|----------|
| 13 (当前) | - | - |
| 14 | CountDown 扩展 + Tag 扩展 | CountDown 增强, Tag 增强 |
| 15 | DiaryEntry syncVersion | 增量备份 |
| 16 | SmartFolder 表 | 智能文件夹 |

**注意事项：**
- 每次迁移都应在 `DiaryDatabase.getDatabase()` 中注册
- 迁移脚本需要在真实设备上测试（尤其是 ALTER TABLE）
- `fallbackToDestructiveMigrationOnDowngrade()` 保留，但应在 UI 层提示用户
- 如果引入 SQLCipher，迁移 13->14 需要在加密前或加密过程中完成

### 10.4 SQLCipher 迁移特殊处理

如果决定引入 SQLCipher，需要特殊的迁移路径：

```kotlin
// 一次性迁移：明文 DB -> 加密 DB
suspend fun migrateToEncrypted(context: Context) {
    val plainDb = Room.databaseBuilder(context, DiaryDatabase::class.java, "diary_database")
        .addMigrations(/* 所有迁移 */)
        .build()

    val passphrase = BackupEncryptor.getOrCreateDatabaseKey()
    val factory = SupportFactory(passphrase)
    val encryptedDb = Room.databaseBuilder(context, DiaryDatabase::class.java, "diary_database_encrypted")
        .openHelperFactory(factory)
        .build()

    // 1. 导出所有数据
    val entries = plainDb.diaryDao().getAllEntriesOnce()
    val tags = plainDb.diaryDao().getAllTagsOnce()
    // ... 其他表

    // 2. 写入加密数据库
    entries.forEach { encryptedDb.diaryDao().insertEntry(it) }
    tags.forEach { encryptedDb.diaryDao().insertTag(it) }
    // ...

    // 3. 关闭明文数据库
    plainDb.close()

    // 4. 重命名文件
    val plainFile = context.getDatabasePath("diary_database")
    val encFile = context.getDatabasePath("diary_database_encrypted")
    val backupFile = context.getDatabasePath("diary_database_plain_backup")

    encFile.renameTo(context.getDatabasePath("diary_database"))
    plainFile.renameTo(backupFile) // 保留备份，确认无误后删除
}
```

---

## 11. 实施路线图

### Phase 1: 核心改进 (4-6 周)

**目标：** 解决最紧迫的问题，提升基础体验。

| 任务 | 工作量 | 优先级 | 依赖 |
|------|--------|--------|------|
| WorkManager 自动备份调度 | 3-5 天 | 高 | 无 |
| 备份加密 (AES-256-GCM) | 2-3 天 | 高 | 无 |
| 备份失败通知 | 1 天 | 高 | WorkManager |
| 锁定超时设置 | 1-2 天 | 高 | 无 |
| 隐私模式 (FLAG_SECURE) | 1 天 | 中 | 无 |
| PIN 加盐 PBKDF2 | 1 天 | 中 | 无 |
| PDF 导出 (WebView) | 1 周 | 高 | 无 |
| HTML 导出 | 1-2 天 | 中 | 无 |
| 年热力图 | 3-5 天 | 高 | 无 |
| 月度柱状图交互 | 2-3 天 | 高 | 无 |
| 设置项重组 | 3-5 天 | 高 | 无 |
| 主题选择内联化 | 1-2 天 | 高 | 设置重组 |
| 字体大小滑块 | 1 天 | 中 | 设置重组 |
| 倒计时到期通知 | 3-5 天 | 高 | 无 |
| 更新日志内联 | 1 天 | 中 | 无 |
| 强制更新策略 | 2 天 | 中 | 无 |
| 标签合并 | 2 天 | 中 | 无 |
| DB Migration 13->14 | 2-3 天 | 高 | CountDown + Tag 变更 |

**Phase 1 总工作量：约 4-6 周**

### Phase 2: 功能增强 (4-6 周)

**目标：** 增加差异化功能，提升用户体验。

| 任务 | 工作量 | 优先级 | 依赖 |
|------|--------|--------|------|
| Google Drive 云备份 | 2-3 周 | 高 | Phase 1 备份加密 |
| SQLCipher 数据库加密 | 3-5 天 | 中 | Phase 1 PIN 增强 |
| 心情趋势折线图 | 3-5 天 | 中 | Phase 1 图表交互 |
| 24 小时写作分布 | 2-3 天 | 中 | 无 |
| 历史最长连续天数 | 1 天 | 中 | 无 |
| 月度字数趋势 | 2 天 | 中 | 无 |
| 批量选择性导出 | 2-3 天 | 中 | Phase 1 PDF/HTML |
| 存储空间管理 | 2 天 | 中 | 无 |
| 大尺寸单 Widget | 3-5 天 | 中 | 无 |
| 更多重复类型 | 2 天 | 中 | Phase 1 DB Migration |
| 标签图标 | 1 天 | 低 | Phase 1 DB Migration |
| 标签排序 | 1 天 | 中 | Phase 1 DB Migration |
| 更多颜色 + 自定义 | 1 天 | 低 | 无 |
| Staged rollout | 3-5 天 | 中 | 无 |
| 6 位 PIN / 自定义长度 | 1 天 | 中 | 无 |
| DB Migration 14->15 | 1 天 | 中 | 增量备份 |

**Phase 2 总工作量：约 4-6 周**

### Phase 3: 高级功能 (4-8 周)

**目标：** 差异化竞争，打造独特价值。

| 任务 | 工作量 | 优先级 | 依赖 |
|------|--------|--------|------|
| 增量备份 | 2 周 | 中 | Phase 2 云备份 |
| 标签层级结构 | 1-2 周 | 中 | Phase 2 标签扩展 |
| 智能文件夹 | 1 周 | 低 | 标签层级 |
| 比较分析 | 2-3 天 | 低 | Phase 2 统计增强 |
| 心情波动指数 | 1 天 | 低 | 无 |
| 假 PIN | 1-2 天 | 低 | 无 |
| DOCX 导出 | 1 天 | 低 | 无 |
| 设置导入/导出 | 2 天 | 低 | 设置重组 |
| 设置搜索 | 2-3 天 | 低 | 设置重组 |
| 倒计时分组 | 2 天 | 低 | Phase 2 重复类型 |
| DB Migration 15->16 | 1 天 | 低 | 智能文件夹 |

**Phase 3 总工作量：约 4-8 周**

### 总体时间线

```
Month 1-2:  Phase 1 (核心改进)
Month 3-4:  Phase 2 (功能增强)
Month 5-6:  Phase 3 (高级功能)

每个 Phase 结束后发布新版本：
  Phase 1 结束 -> v2.70.0 (experimental)
  Phase 2 结束 -> v2.80.0 (experimental)
  Phase 3 结束 -> v3.0.0 (stable release)
```

### 版本号规划

| 版本 | 内容 | 里程碑 |
|------|------|--------|
| v2.65.x | Phase 1 全部功能 | 核心体验达标 |
| v2.70.x | Phase 2 全部功能 | 功能完整 |
| v2.80.x | Phase 3 全部功能 | 差异化竞争力 |
| v3.0.0 | 全部完成，stable release | 正式版 |

---

## 附录：依赖清单

### 新增依赖

```kotlin
// build.gradle.kts - 新增依赖

// Backup: Google Drive
implementation("com.google.android.gms:play-services-auth:20.7.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20231105-2.0.0")

// Backup: WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Security: SQLCipher
implementation("net.zetetic:android-database-sqlcipher:4.5.6")
implementation("androidx.sqlite:sqlite-ktx:2.4.0")

// Export: PDF (WebView 方案无额外依赖)
// Export: DOCX (HTML 兼容方案无额外依赖)

// Stats: 图表自绘，无额外依赖
// Update: 无额外依赖
```

### 移除依赖（可选）

```kotlin
// 如果不需要 Markdown 渲染（当前也没有用到）：
// implementation("io.noties.markwon:core:4.6.2")  // 如果存在的话
```

---

## 附录：风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Google Drive API 变更 | 云备份功能失效 | 抽象 CloudProvider 接口，支持多种后端 |
| SQLCipher 迁移失败 | 用户数据丢失 | 迁移前自动备份，保留明文 DB 备份 |
| WorkManager 在某些 ROM 上被杀 | 自动备份不触发 | 增加前台服务保活（可选） |
| WebView PDF 渲染在低端设备上慢 | 导出卡顿 | 限制并发，显示进度条 |
| 标签层级结构导致查询变慢 | 统计页面卡顿 | 增加索引，使用 Room 的 `@Transaction` |
| 增量备份恢复复杂度高 | 恢复失败 | 保留全量备份作为回退 |

---

> 本文档基于 2026-06-10 对代码库的分析编写。
> 数据库版本：13 | experimental 版本：2.61.42 | stable 版本：1.10.0
