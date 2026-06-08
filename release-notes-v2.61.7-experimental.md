# v2.61.7-experimental

## 实验性更新

- 保留来自 `2.61.6-experimental` 的轻量待办滑动切换和收紧的日记编辑器元数据布局
- 新增一轮低风险稳定性加固，不改变用户界面流程
- 将首页过滤中脆弱的未检查类型转换替换为类型化快照组合
- 移除待办和编辑器相关流程中一些不必要的非空断言
- 增加 `docs/build-notes.md` 文档记录推荐的 Gradle/JDK 配置和当前本地单元测试 worker 问题

## 验证

- `:app:compileExperimentalDebugKotlin` 通过
- `:app:assembleExperimentalRelease` 通过

## 已知问题

- 在 Windows 环境下 Gradle 运行在 Java 21 时本地单元测试执行仍不稳定
- 建议：使用 JDK 17 运行 Gradle 任务，特别是单元测试
