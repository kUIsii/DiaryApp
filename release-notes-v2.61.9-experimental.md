# v2.61.9-experimental

## 实验性更新

- 打包来自当前 `experiment/v2-redesign` 分支的最新低风险加固工作
- 保留近期的待办滑动 / 日记编辑器 UI 细化
- 进一步减少编辑器、标签管理、待办、倒计时、更新日志和统计流程中的强制解包可空状态
- 持续提升可维护性，不有意改变现有用户界面行为

## 验证

- `:app:compileExperimentalDebugKotlin` 通过
- `:app:assembleExperimentalRelease` 通过

## 已知问题

- 本机在 Java 21 下运行 Gradle 时本地单元测试仍不可靠
- 建议：使用 JDK 17 运行 Gradle 测试任务
