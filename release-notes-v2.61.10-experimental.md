# v2.61.10-experimental

## 实验性更新

- 优化日记编辑器的文字颜色同步、复选框/无序列表行为，以及键盘/工具栏区域上方的光标可见性
- 减少正常保存后的误报草稿恢复提示，收紧草稿清除流程并在成功保存后清除匹配的草稿列表项
- 收紧日记头部元数据布局，长标签独占一行，移除可见的灵感入口使记录视图更简洁
- 更新待办标签页的滑动切换为更轻量的滑动淡入淡出效果，减少"翻页"感

## 验证

- `:app:compileExperimentalDebugKotlin` 通过
- `:app:assembleExperimentalRelease` 通过

## 已知问题

- 本机在 Java 21 下运行 Gradle 时本地单元测试仍不可靠，存在 `docs/build-notes.md` 中记录的 Gradle worker 启动失败问题
- 建议：使用 JDK 17 运行 Gradle 测试任务以获得可靠的本地单元测试执行
