该仓库采用混合式错误处理策略，结合了 Kotlin 的异常体系、密封类（Sealed Classes）以及 Result 类型，针对不同模块（AI 服务、数据备份/导入、UI 交互）设计了差异化的处理方式。

### 1. 核心模式与架构

*   **领域特定错误模型（Domain-Specific Errors）**：
    *   在 AI 模块中，定义了 `sealed class AiError : Exception()`（位于 `AiModels.kt`），用于封装所有与 AI 服务相关的错误。这种设计允许调用方通过 `when` 表达式进行 exhaustive checking（穷尽检查），精确区分 `NotConfigured`（未配置）、`RateLimited`（限流）、`ApiError`（API 错误）、`ParseError`（解析错误）和 `Unknown`（未知错误）。
    *   这种模式提高了代码的可读性和安全性，避免了通用的 `Exception` 捕获导致的逻辑模糊。

*   **Result 类型与函数式处理**：
    *   在业务逻辑层（如 `AiAssistantViewModel`），广泛使用 Kotlin 标准库的 `runCatching` 或自定义的 `Result` 返回类型。例如，`aiService.chat()` 返回 `Result<AiResponse>`，调用方通过 `getOrNull()` 和 `exceptionOrNull()` 安全地获取结果或错误信息，避免了 try-catch 块的嵌套。

*   **通用异常与常量消息**：
    *   在数据导入/导出模块（`DiaryImporter.kt`, `BackupManager.kt`），倾向于抛出通用的 `Exception`，但配合内部常量（如 `READ_BACKUP_ERROR_MESSAGE`, `INVALID_BACKUP_FORMAT_MESSAGE`）来提供明确的错误描述。这种方式简化了底层数据操作的代码，将错误语义化交给上层处理。

### 2. 关键文件与职责

*   **`app/src/main/java/com/diary/app/ai/AiModels.kt`**：
    *   定义了 `AiError` 密封类及其子类。这是仓库中唯一显式定义的层次化错误类型系统，体现了对 AI 服务稳定性的高度关注。
*   **`app/src/main/java/com/diary/app/ai/BaseHttpProvider.kt`**：
    *   实现了 HTTP 请求层面的错误转换。它将底层的 `HttpURLConnection` 响应码（如 401, 402, 429）映射为具体的 `AiError` 实例（如 `AiError.ApiError` 或 `AiError.RateLimited`），并在发生未知异常时包裹为 `AiError.Unknown`。
*   **`app/src/main/java/com/diary/app/ai/AiAssistantViewModel.kt`**：
    *   展示了 UI 层的错误消费模式。在 `sendMessage` 协程中，通过 `try-catch` 捕获异常，并根据异常类型（如 `SocketTimeoutException`）或消息内容生成用户友好的提示文本（如“等太久了，网络不太好”），最终将其作为一条特殊的助手消息插入聊天历史，实现了“错误即状态”的 UI 反馈。
*   **`app/src/main/java/com/diary/app/data/DiaryImporter.kt`**：
    *   定义了导入过程中的校验逻辑。通过 `parseBackupJson` 和 `validateBackupHasImportableData` 等函数，在解析失败或数据为空时抛出带有明确中文描述的 `Exception`，确保数据完整性问题能被快速定位。

### 3. 开发规范与建议

*   **优先使用密封类定义错误**：对于具有多种明确失败场景的模块（如网络请求、支付、权限检查），应效仿 `AiError` 的模式，定义 sealed class/error interface，以便编译器辅助检查所有分支。
*   **避免吞没异常**：在 `catch` 块中，除非有明确的恢复策略或日志记录，否则不应静默忽略异常。例如 `BackupManager` 中的某些 `catch (_: Exception)` 块仅记录了 `Log.w`，这在调试时可能掩盖潜在的文件系统问题。
*   **用户友好提示**：UI 层不应直接展示技术性的错误堆栈或原始异常消息。应像 `AiAssistantViewModel` 那样，将底层错误映射为用户可理解的自然语言提示。
*   **一致性改进方向**：目前数据层（Data Layer）多使用通用 `Exception`，而 AI 层使用密封类。建议在未来重构中，为数据备份、媒体处理等核心模块也引入类似的领域错误类型，以统一全站的错误处理风格。