# v2.61.8-experimental

## 实验性更新

- 包含最新的 UI 优化，来自近期的待办滑动和日记编辑器细化工作
- 包含 `2.61.7-experimental` 之后新增的低风险加固提交
- 减少待办、倒计时、更新日志、统计、首页和编辑器相关流程中的多个可空状态崩溃路径
- 保持当前主题和交互设计不变，同时提升维护安全性

## 验证

- `:app:compileExperimentalDebugKotlin` 通过
- `:app:assembleExperimentalRelease` 通过

## 已知问题

- 在 Windows 环境下 Gradle 运行在 Java 21 时本地单元测试执行仍不稳定
- 建议：使用 JDK 17 运行本地测试任务
