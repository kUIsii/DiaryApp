// 这是一个示例脚本，用于演示如何与 Hermes 中文文档 MCP 服务器通信
// 使用 JSON-RPC 2.0 协议

async function queryHermesDocs() {
  const endpoint = 'https://mcp.hermesagent.org.cn/v1';

  // 示例请求：获取服务器功能信息
  const request = {
    jsonrpc: "2.0",
    id: 1,
    method: "capabilities/list",
    params: {}
  };

  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    });

    const result = await response.json();
    console.log('Response from Hermes Docs MCP server:', result);
  } catch (error) {
    console.error('Error communicating with MCP server:', error);
  }
}

console.log('Hermes 中文文档 MCP 服务器测试脚本');
console.log('要使用此脚本，请在支持 JavaScript 的环境中运行');
console.log('服务器端点: https://mcp.hermesagent.org.cn/v1');
console.log('传输协议: JSON-RPC 2.0 over HTTP');

// 注意：此脚本仅为演示目的，在 Claude Code 环境中可能无法直接执行外部请求
// 实际使用时，Claude Code 应该自动识别并使用配置中的 MCP 服务器