# 免费与低成本 AI API 服务调研报告

**文档版本：** v1.0
**调研日期：** 2026-06-14

---

## 目录

1. [调研概述](#一调研概述)
2. [国内平台](#二国内平台)
3. [国际平台](#三国际平台)
4. [开源模型托管](#四开源模型托管)
5. [聚合平台](#五聚合平台)
6. [综合评估对比](#六综合评估对比)
7. [推荐方案](#七推荐方案)
8. [代码示例](#八代码示例)
9. [注意事项](#九注意事项)

---

## 一、调研概述

本报告对当前主流的免费或低成本 AI 对话 API 服务进行全面调研，覆盖国内平台、国际平台、开源模型托管和聚合平台四大类。

**评估维度：**
- 免费额度（每日/每月调用次数）
- 支持的模型列表
- 响应速度
- 中文支持质量
- API 兼容性（是否兼容 OpenAI 格式）
- 注册和使用门槛
- 稳定性和可靠性

---

## 二、国内平台

### 1. 硅基流动 SiliconFlow（强烈推荐）

**官网：** https://siliconflow.cn

硅基流动是国内最值得关注的模型聚合平台，提供多个完全免费的模型。

**免费模型：**

| 模型 | 来源 | 说明 |
|------|------|------|
| GLM-Z1-9B-0414 | 智谱 | 完全免费 |
| GLM-4-9B-0414 | 智谱 | 完全免费 |
| Qwen3.5-4B | 阿里通义 | 完全免费 |
| Hunyuan-MT-7B | 腾讯 | 完全免费 |
| Nex-N2-Pro | 397B MoE | 限时免费 |
| bge-m3 / bge-reranker-v2-m3 | 嵌入模型 | 完全免费 |

**低价模型：**

| 模型 | 输入价格 | 输出价格 |
|------|---------|---------|
| DeepSeek-V4-Flash | ¥1.00/M tokens | ¥2.00/M tokens |
| Qwen3.6-35B-A3B | ¥0.40/M tokens | ¥3.20/M tokens |
| Step-3.5-Flash | ¥0.70/M tokens | ¥2.10/M tokens |

**优势：**
- 完全兼容 OpenAI 格式
- 国内手机号注册，无需海外支付
- 聚合国内外主流模型
- 价格透明

---

### 2. 智谱 AI（GLM 系列）

**官网：** https://open.bigmodel.cn

**主要模型：**
- GLM-5.1 (Pro)：最新旗舰模型
- GLM-4.5V：视觉理解模型
- GLM-4.5-Air：轻量版
- GLM-4-32B-0414：开源版本

**免费额度：** 新用户注册后有免费调用额度。在硅基流动上，GLM-Z1-9B 和 GLM-4-9B 完全免费。

**中文支持：** 优秀，GLM 系列专门为中文场景优化。

---

### 3. 阿里云百炼（通义千问 Qwen 系列）

**官网：** https://bailian.console.aliyun.com

**主要模型：**
- qwen3.7-max：最强能力
- qwen3.7-plus：均衡能力
- qwen3.6-flash：成本优先
- 第三方模型：deepseek-v4-pro、kimi-k2.6、glm-5.1 等

**免费额度：** 新用户有免费试用额度。硅基流动上 Qwen3.5-4B 完全免费。

---

### 4. 深度求索 DeepSeek

**官网：** https://platform.deepseek.com

**主要模型：**
- DeepSeek-V4-Pro：1.6T 总参数，49B 激活参数，1M 上下文
- DeepSeek-V4-Flash：284B 参数轻量版，1M 上下文

**定价（每百万 tokens）：**

| 模型 | 输入（缓存命中） | 输入（缓存未命中） | 输出 |
|------|----------------|------------------|------|
| DeepSeek-V4-Flash | $0.0028 | $0.14 | $0.28 |
| DeepSeek-V4-Pro | $0.003625 | $0.435 | $0.87 |

**API 兼容性：** 完全兼容 OpenAI 和 Anthropic 格式。

---

### 5. 月之暗面 Kimi

**官网：** https://platform.kimi.com

**主要模型：**
- Kimi K2.7 Code：最强代码模型
- Kimi K2.6：多模态模型
- Moonshot V1：经典生成模型

**特点：** 长上下文和多模态能力著称。

---

### 6. 百度文心一言（千帆平台）

**官网：** https://cloud.baidu.com/doc/WENXINWORKSHOP

**免费额度：** 新用户注册并实名认证后赠送 20 元代金券。

---

### 7. 讯飞星火

**官网：** https://xinghuo.xfyun.cn

**特点：** 语音识别和中文处理方面有优势。

---

### 8. 火山引擎豆包（字节跳动）

**官网：** https://www.volcengine.com/product/ark

**特点：** 豆包大模型服务，支持 Seedance 视频生成。

---

## 三、国际平台

### 1. Groq（速度之王）

**官网：** https://groq.com

**主要模型与定价（每百万 tokens）：**

| 模型 | 速度 | 输入 | 输出 |
|------|------|------|------|
| GPT OSS 20B 128k | 1,000 TPS | $0.075 | $0.30 |
| GPT OSS 120B 128k | 500 TPS | $0.15 | $0.60 |
| Llama 4 Scout 128k | 594 TPS | $0.11 | $0.34 |
| Qwen3 32B 131k | 662 TPS | $0.29 | $0.59 |
| Llama 3.3 70B 128k | 394 TPS | $0.59 | $0.79 |
| Llama 3.1 8B 128k | 840 TPS | $0.05 | $0.08 |

**免费额度：** 提供免费 API Key，有速率限制。

**特点：** 号称比 OpenAI 和 Anthropic 快 20 倍。

---

### 2. Google Gemini

**官网：** https://ai.google.dev

**主要模型与定价（每百万 tokens）：**

| 模型 | 输入 | 输出 |
|------|------|------|
| Gemini 3.5 Flash | $1.50 | $9.00 |
| Gemini 3.1 Flash-Lite | $0.25 | $1.50 |
| Gemini 2.5 Pro | $1.25 | $10.00 |
| Gemini 2.5 Flash | $0.30 | $2.50 |
| Gemini 2.5 Flash-Lite | $0.10 | $0.40 |

**免费层：** 多个模型提供免费使用，有速率限制。

---

### 3. Together AI

**官网：** https://www.together.ai

**部分模型定价（每百万 tokens）：**

| 模型 | 输入 | 输出 |
|------|------|------|
| LFM2 24B A2B | $0.03 | $0.12 |
| gpt-oss-20B | $0.05 | $0.20 |
| Gemma 3n E4B | $0.06 | $0.12 |
| MiniMax M3 | $0.30 | $1.20 |
| DeepSeek V4 Pro | $2.10 | $4.40 |

---

### 4. Fireworks AI

**官网：** https://fireworks.ai

**免费额度：** 新用户获得 $1 免费额度。

**特点：** 400+ 模型，缓存输入 token 默认 50% 折扣。

---

### 5. Cerebras（极速推理）

**官网：** https://cerebras.ai

**免费层：** 访问所有模型，推理速度号称比 OpenAI 快 20 倍。

---

## 四、开源模型托管

### 1. Hugging Face Inference API

**官网：** https://huggingface.co/inference-api

**免费额度：** 有速率限制的免费层，适合开发和测试。

**支持模型：** 数千个开源模型。

---

### 2. 魔搭 ModelScope

**官网：** https://modelscope.cn

**特点：** 阿里巴巴达摩院推出的模型社区，以中文模型为主。

---

## 五、聚合平台

### OpenRouter

**官网：** https://openrouter.ai

**特点：**
- 统一 API 接口，兼容 OpenAI 格式
- 聚合多家提供商的模型
- 部分模型免费使用
- base_url：`https://openrouter.ai/api/v1`

---

## 六、综合评估对比

| 平台 | 免费额度 | 中文支持 | OpenAI 兼容 | 注册门槛 | 稳定性 | 响应速度 |
|------|---------|---------|------------|---------|--------|---------|
| **硅基流动** | 多个免费模型 | 优秀 | 完全兼容 | 低（国内手机） | 高 | 快 |
| **智谱 AI** | 有免费额度 | 优秀 | 兼容 | 低 | 高 | 快 |
| **阿里百炼** | 有免费额度 | 优秀 | 兼容 | 中（实名） | 高 | 快 |
| **DeepSeek** | 价格极低 | 优秀 | 完全兼容 | 低 | 中 | 中 |
| **Kimi** | 有免费额度 | 优秀 | 兼容 | 低 | 高 | 快 |
| **百度千帆** | 20 元代金券 | 优秀 | 部分兼容 | 中（实名） | 高 | 快 |
| **Groq** | 免费 Key | 中等 | 兼容 | 高（海外） | 高 | 极快 |
| **Google Gemini** | 慷慨免费层 | 良好 | 有兼容层 | 高（海外） | 高 | 快 |
| **Together AI** | 有免费额度 | 中等 | 兼容 | 高（海外） | 高 | 快 |
| **Fireworks AI** | $1 免费额度 | 中等 | 兼容 | 高（海外） | 高 | 快 |
| **Cerebras** | 免费层 | 中等 | 兼容 | 高（海外） | 中 | 极快 |
| **OpenRouter** | 部分免费模型 | 取决于模型 | 完全兼容 | 中 | 中 | 中 |

---

## 七、推荐方案

### 方案一：最佳免费方案（推荐国内用户）

**组合：硅基流动免费模型 + 智谱免费额度 + 百度 20 元代金券**

- **主力模型：** 硅基流动上的 GLM-Z1-9B、GLM-4-9B、Qwen3.5-4B（完全免费）
- **增强模型：** 智谱 GLM-4.5-Air（免费额度内）
- **备用模型：** 百度千帆 ERNIE（20 元代金券）
- **成本：** 0 元
- **适用场景：** 日常对话、轻量级应用、开发测试

### 方案二：最佳性价比方案

**组合：DeepSeek V4-Flash + 硅基流动低价模型 + Google Gemini 免费层**

- **主力模型：** DeepSeek-V4-Flash（约 ¥1/M 输入 tokens）
- **增强模型：** 硅基流动 Qwen3.6-35B-A3B（¥0.40/M 输入）
- **备用模型：** Google Gemini 2.5 Flash 免费层
- **月成本估算：** 日均 10 万 token 约 ¥3-5/月
- **适用场景：** 中等使用量的应用、个人项目

### 方案三：最稳定的方案

**组合：阿里百炼 + 智谱 AI + Groq**

- **主力模型：** 阿里百炼 qwen3.6-flash（企业级稳定性）
- **增强模型：** 智谱 GLM-5.1（国内顶级中文能力）
- **高速模型：** Groq Llama 3.3 70B（极速推理）
- **月成本估算：** 中等使用量约 ¥20-50/月
- **适用场景：** 生产环境、企业应用

---

## 八、代码示例

### 1. OpenAI 兼容格式通用调用方式

```kotlin
// Retrofit 接口定义
interface AiApi {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}

// 请求数据类
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 1000
)

data class Message(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

// 使用示例
suspend fun chatWithAi(
    provider: AiProvider,
    apiKey: String,
    userMessage: String
): String {
    val api = Retrofit.Builder()
        .baseUrl(provider.baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AiApi::class.java)

    val request = ChatRequest(
        model = provider.defaultModel,
        messages = listOf(
            Message("system", "你是一个有用的助手。"),
            Message("user", userMessage)
        )
    )

    val response = api.chatCompletions("Bearer $apiKey", request)
    return response.choices.first().message.content
}
```

### 2. 硅基流动免费模型调用示例

```kotlin
// 配置
val siliconFlowProvider = AiProvider(
    baseUrl = "https://api.siliconflow.cn/v1",
    defaultModel = "THUDM/glm-z1-9b-chat",
    displayName = "硅基流动"
)

// 调用
val response = chatWithAi(
    provider = siliconFlowProvider,
    apiKey = "your-siliconflow-api-key",
    userMessage = "用中文解释什么是机器学习"
)
```

### 3. 多平台故障转移方案

```kotlin
class AiServiceWithFallback(
    private val providers: List<AiProviderConfig>
) {
    suspend fun chat(messages: List<Message>): String {
        for (provider in providers) {
            try {
                return chatWithAi(provider, messages)
            } catch (e: Exception) {
                Log.w("AiService", "${provider.name} failed: ${e.message}")
                continue
            }
        }
        throw Exception("所有平台均不可用")
    }
}

// 配置多个平台，按优先级排序
val aiService = AiServiceWithFallback(
    listOf(
        AiProviderConfig("硅基流动", "key1", "THUDM/glm-z1-9b-chat"),
        AiProviderConfig("DeepSeek", "key2", "deepseek-v4-flash"),
        AiProviderConfig("智谱", "key3", "glm-4-flash")
    )
)
```

### 4. cURL 调用示例

```bash
# 硅基流动（免费模型）
curl https://api.siliconflow.cn/v1/chat/completions \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "THUDM/glm-z1-9b-chat",
    "messages": [{"role": "user", "content": "你好"}]
  }'

# DeepSeek
curl https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-v4-flash",
    "messages": [{"role": "user", "content": "你好"}]
  }'

# Groq
curl https://api.groq.com/openai/v1/chat/completions \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama-3.3-70b-versatile",
    "messages": [{"role": "user", "content": "你好"}]
  }'
```

---

## 九、注意事项

1. **免费额度可能随时调整：** 各平台的免费政策会变化，建议定期查看官方文档。

2. **速率限制：** 免费层通常有 RPM（每分钟请求数）和 TPM（每分钟 token 数）限制。

3. **数据隐私：** 免费层的内容可能被用于模型训练（如 Google Gemini），敏感数据请使用付费层。

4. **网络访问：** 国际平台在国内访问可能不稳定。

5. **API Key 安全：** 切勿将 API Key 硬编码在前端代码中，应使用 Android Keystore 加密存储。

---

## 十、总结

**国内用户首选：硅基流动**

- 提供多个完全免费的模型
- 国内手机号直接注册
- 完全兼容 OpenAI 格式
- 聚合国内外主流模型

**追求极致性价比：DeepSeek**

- V4-Flash 定价极具竞争力（约 ¥1/M 输入 tokens）
- 支持 1M 上下文和思考模式

**需要极速响应：Groq 或 Cerebras**

- 1000+ TPS 推理速度
- 需要海外账号

**需要最稳定服务：阿里百炼 + 智谱**

- 企业级服务保障
- 适合生产环境
