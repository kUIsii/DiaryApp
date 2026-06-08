# v2.61.6-experimental

## 实验性更新

- 优化待办页面滑动切换为更轻量的水平滑动带淡入淡出效果
- 收紧日记编辑器的元数据标签、天气/心情/标签/位置面板和文字尺寸
- 将写作提示入口移至紧凑的"灵感"操作，使头部更简洁
- 修复浅色和深色主题下编辑器透明背景层叠问题
- 增加低风险的工具/测试覆盖用于选中名称摘要
- 规范 Room 迁移参数命名便于维护

## 验证

- `:app:compileExperimentalDebugKotlin` 通过
- `:app:assembleExperimentalRelease` 通过

## 已知问题

- `testExperimentalDebugUnitTest` 在 Windows + JDK 21 环境下被本地 Gradle Test Executor / worker 启动问题阻塞，非业务断言失败
