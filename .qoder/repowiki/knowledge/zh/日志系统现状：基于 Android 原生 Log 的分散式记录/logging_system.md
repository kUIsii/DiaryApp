## 1. 系统/方法概述
该仓库目前**未引入第三方日志框架**（如 Timber、Kermit 或 SLF4J），而是完全依赖 **Android 原生 `android.util.Log`** 进行日志输出。日志记录呈现**分散式**特征，直接嵌入在业务逻辑类（ViewModel、Manager、Worker）中，缺乏统一的日志门面或全局配置。

## 2. 关键文件与包
核心日志调用分布在以下模块：
- **应用入口**: `app/src/main/java/com/diary/app/DiaryApplication.kt` — 负责 SDK 初始化异常及启动预热失败的记录。
- **数据层**: `app/src/main/java/com/diary/app/data/BackupManager.kt`, `DiaryDatabase.kt` — 记录备份频率解析错误、迁移失败及数据库损坏时的紧急备份路径。
- **AI 模块**: `app/src/main/java/com/diary/app/ai/AiAssistantViewModel.kt`, `AiServiceManager.kt` — 记录聊天请求失败及上下文构建异常。
- **后台任务**: `app/src/main/java/com/diary/app/data/BackupWorker.kt`, `TrashCleanupWorker.kt` — 记录自动备份和垃圾清理任务的执行状态。

## 3. 架构与约定
- **标签策略 (Tagging)**: 采用**类名或功能模块名**作为 Log Tag（例如 `"BackupManager"`, `"AiAssistant"`, `"DiaryDatabase"`）。这种方式便于在 Logcat 中通过 Tag 过滤特定模块的输出。
- **级别使用**: 
  - `Log.e`: 用于捕获严重的运行时异常（如 API 调用失败、数据库迁移崩溃）。
  - `Log.w`: 用于非阻断性错误或状态回退（如配置解析失败、文件读取异常）。
  - 暂未发现 `Log.d` 或 `Log.i` 的大规模使用，表明当前日志主要用于**故障排查**而非流程追踪。
- **异常处理**: 普遍采用 `try-catch` 块包裹潜在风险操作，并在 `catch` 分支中记录异常堆栈。

## 4. 开发者应遵循的规则
- **禁止硬编码敏感信息**: 由于 `android.util.Log` 在 Release 模式下默认不会自动剥离，严禁在日志中打印用户隐私、API Key 或完整日记内容。
- **统一 Tag 命名**: 建议继续使用**当前类名**或**所属子系统名称**作为 Tag，保持全局一致性以便于调试。
- **异常堆栈记录**: 在记录 `Exception` 时，务必使用三参数重载 `Log.e(tag, message, throwable)` 以保留完整的堆栈跟踪信息。
- **未来演进建议**: 随着项目复杂度增加，建议引入 **Timber** 等轻量级日志库，以实现全局日志开关控制、自动 Tag 注入以及 Release 环境下的日志静默。