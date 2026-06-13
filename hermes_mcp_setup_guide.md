# Hermes Agent 中文文档 MCP 服务器配置说明

## 配置摘要
我们已在您的 Claude Code 设置中成功添加了 Hermes 中文文档 MCP 服务器。

**服务器信息:**
- 名称: Hermes 中文文档 MCP Server
- 地址: https://mcp.hermesagent.org.cn/v1
- 协议: JSON-RPC 2.0 over HTTP
- 认证: 无需 API Key 或登录
- 功能: 提供 Hermes Agent 中文文档服务

## 如何使用
1. 重启 Claude Code 会话以使配置生效
2. 重启后，Claude 将能够访问 Hermes Agent 中文文档
3. 您可以直接询问关于 Hermes Agent 的安装和使用问题

## 故障排除
如果遇到问题，请检查:
- 确保您的网络连接可以访问 https://mcp.hermesagent.org.cn/v1
- 检查 Claude Code 日志中是否有 MCP 服务器连接错误
- 如果配置未生效，请确认 Claude Code 已完全重启

## 关于 Hermes Agent
Hermes Agent 是一个 AI 辅助工具，通过 MCP (Model Context Protocol) 协议提供中文文档服务，帮助用户更好地理解和使用 AI 相关功能。

## 安装指导
要安装和使用 Hermes Agent，一般步骤如下：
1. 确认您的开发环境满足基本要求
2. 通过适当的包管理器安装 Hermes Agent
3. 配置必要的参数
4. 启动服务并验证连接
5. 在 Claude Code 中开始使用

## 注意事项
- 此 MCP 服务器是专门用于中文文档查询的
- 服务器目前版本为 0.1.0
- 所有通信均采用安全的 HTTPS 协议