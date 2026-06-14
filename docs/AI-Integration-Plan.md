# 日记 App AI 接入方案

**文档版本：** v1.0
**更新日期：** 2026-06-14

---

## 目录

1. [项目现状分析](#一项目现状分析)
2. [AI 功能规划](#二ai-功能规划)
3. [竞品分析](#三竞品分析)
4. [技术架构设计](#四技术架构设计)
5. [实施路线图](#五实施路线图)
6. [商业模式](#六商业模式)

---

## 一、项目现状分析

当前 app 已有的数据基础非常适合接入 AI：

| 数据类型 | 说明 | AI 应用价值 |
|---------|------|------------|
| **情绪数据** | 6 级情绪量表（沮丧/低落/平静/开心/愉快/兴奋） | 情绪分析、趋势预测 |
| **文本数据** | Quill.js delta JSON + plainText 双轨存储 | NLP 处理、语义理解 |
| **元数据** | 天气、地理位置、标签、时间戳 | 上下文分析、模式识别 |
| **习惯数据** | HabitRecord 关联日记 | 习惯-情绪关联分析 |
| **统计体系** | 热力图、月度趋势、情绪趋势 | 数据洞察增强 |

---

## 二、AI 功能规划

### 2.1 写作辅助

#### 智能续写

**场景：** 用户写到一半卡住，AI 根据前文语境生成 2-3 个续写建议。

**技术实现：** 将 plainText 发送到 LLM API，要求以相同风格续写 50-100 字。

**工作量：** 1 周

#### 一键润色

**场景：** 用户写完日记，点击"润色"按钮，AI 提供修改建议。支持"微调"和"重写"两种力度。

**技术实现：** 需要处理 Quill.js delta JSON 的格式保留问题。可以先只处理 plainText，返回后做 diff 对比展示。

**工作量：** 1-2 周

#### 风格转换

**场景：** 提供几种预设风格：书信体（给未来的自己）、诗歌体、第三人称叙事、极简风。

**技术实现：** 每种风格需要独立的 prompt 模板。

**工作量：** 1 周

#### 语音日记转文字优化

**场景：** 语音输入的 ASR 转写结果自动纠错、加标点、适度书面化。

**技术实现：** LLM 最擅长的任务之一，设定"最小改动原则"。

**工作量：** 3 天

### 2.2 情感分析

#### AI 情绪识别（自动标签）

**场景：** AI 根据日记内容自动判断情绪等级，与用户自评形成对比校准。

**技术实现：** 基于现有 6 级体系做映射，支持多标签或置信度分布。

**工作量：** 2 周

#### 情绪预警

**场景：** 连续 3-5 天情绪持续低迷时，温和提醒用户关注心理状态。

**技术实现：** 时序情感分析 + 趋势检测，措辞必须温和、非诊断性。

**工作量：** 3 周

#### 情绪洞察报告

**场景：** 每月生成"情绪月报"，包含情绪主题词、关键转折点、关联分析。

**技术实现：** 对一个月的日记做聚合分析，生成叙事性总结。

**工作量：** 2 周

#### 写作时的情绪陪伴

**场景：** 编辑器中实时感知内容基调，检测到负面情绪时显示柔和提示。

**技术实现：** 轻量级情感分类 + 预设陪伴文案库。

**工作量：** 3 天

### 2.3 内容理解

#### 自动标签推荐

**场景：** 写完日记后，AI 自动推荐 2-3 个标签，用户一键采纳。

**技术实现：** TF-IDF + LLM 混合方案，复用现有 Tag + DiaryTag 体系。

**工作量：** 1 周

#### 智能摘要

**场景：** 长篇日记（>500字）自动生成 1-2 句摘要，显示在时间线列表。

**技术实现：** LLM 基础能力，prompt 要求"用日记主人的语气"总结。

**工作量：** 1 周

#### 语义搜索

**场景：** 用户搜索"那次旅行"、"和妈妈的对话"，AI 理解语义返回相关日记。

**技术实现：** 对所有日记做 embedding 向量化存储，用 Room 存储 embedding blob。

**工作量：** 2 周

#### 自动提取关键事件

**场景：** AI 从日记中自动提取人名、地点、事件，建立"生活事件索引"。

**技术实现：** 中文 NER（命名实体识别），可能需要 fine-tuning。

**工作量：** 3 周（困难）

### 2.4 智能回顾

#### 时间胶囊增强

**场景：** AI 自动在生日、新年等节点生成"时光胶囊"，基于历史日记精华摘录。

**技术实现：** 对接日历系统 + 日记内容聚合。

**工作量：** 2 周

#### 相似日记推荐

**场景：** 阅读某篇日记时，显示相似日记：同季节、同情绪、同人物/地点。

**技术实现：** embedding 相似度 + 元数据过滤。

**工作量：** 2 周

#### 智能日记回顾卡片

**场景：** 每天推送一张"回顾卡片"，AI 从历史日记提炼一段话加温暖点评。

**技术实现：** 从历史日记选有代表性片段，生成有温度的点评。

**工作量：** 1 周

#### 主题式回顾

**场景：** 用户选择主题（"旅行"、"成长"、"友情"），AI 按主题聚合生成回顾。

**技术实现：** 需要 LLM 做语义层面的主题识别。

**工作量：** 3 周（困难）

### 2.5 数据洞察

#### 生活模式识别

**场景：** AI 分析长期数据，识别"每月月初情绪偏低"、"天气好时写日记更积极"等模式。

**技术实现：** 多维度数据交叉分析，谨慎表述相关性 vs 因果性。

**工作量：** 持续迭代

#### 习惯关联分析

**场景：** 分析习惯与情绪的关联：坚持运动时情绪是否更好？

**技术实现：** 时序关联分析，复用 HabitRecord 数据。

**工作量：** 2 周

#### 写作成长追踪

**场景：** 分析长期写作变化：词汇量增长、句子长度变化、表达复杂度。

**技术实现：** NLP 基础指标（词汇丰富度、句长分布、新词比例）。

**工作量：** 2 周

### 2.6 创意功能

#### AI 配图生成

**场景：** 根据日记内容生成配图，支持水彩、插画、极简线条等风格。

**技术实现：** 接入图像生成 API（Stable Diffusion、DALL-E）。

**工作量：** 2 周

#### 日记配诗

**场景：** AI 根据日记内容生成小诗，支持现代诗、五言绝句、七言律诗。

**技术实现：** LLM 创意写作能力。

**工作量：** 1 周

#### 故事续写 / 平行日记

**场景：** AI 以"平行宇宙"视角续写不同版本的日记。

**技术实现：** 基于日记内容做创意续写。

**工作量：** 3 天

#### 声音明信片

**场景：** 将日记转化为语音，配背景音乐，生成 1-2 分钟"声音明信片"。

**技术实现：** TTS（Azure TTS、ElevenLabs）。

**工作量：** 2 周

---

## 三、竞品分析

### 3.1 竞品 AI 功能对比

| 产品 | AI 功能 | 定价 | 特点 |
|------|---------|------|------|
| **Day One** | AI 写作助手、智能提示 | 订阅制 | 设计精美，隐私保护强 |
| **Notion AI** | Agent、自定义代理、会议笔记 | 按 credits 计费 | 功能全面，团队协作强 |
| **Obsidian** | Copilot 插件、Vault QA、Agent 模式 | 免费+付费 | 数据隐私最佳，高度可定制 |
| **Reflect Notes** | 语音转录、大纲生成、语法修正 | 订阅制 | 简洁专注，端到端加密 |
| **Mem.ai** | Smart Write、AI Thought Partner | 订阅制 | AI 原生设计 |
| **Flomo** | 语音输入、AI 洞察、相关笔记、MCP | PRO 会员 | 简单轻量，理念先进 |

### 3.2 核心趋势

| 趋势 | 说明 |
|------|------|
| AI 写作辅助 | 帮助用户改善写作质量、提供灵感 |
| 智能搜索与检索 | 自然语言搜索、语义理解 |
| 内容关联与洞察 | 自动发现笔记间联系、挖掘隐藏模式 |
| 语音转文字 | 降低记录门槛，提升效率 |
| 隐私优先 AI | 本地处理、数据不出设备 |

### 3.3 差异化建议

**核心定位：AI 驱动的个人成长伙伴**

区别于其他日记 App 的"记录工具"定位，聚焦于"帮助用户通过日记实现个人成长"。

**差异化功能：**

| 功能 | 差异化点 |
|------|----------|
| 情绪智能分析 | 不只是记录，而是帮助用户理解情绪模式 |
| 成长洞察报告 | 将日记从记录工具转变为成长工具 |
| 智能回顾 | 有策略的回顾，而非随机推荐 |
| 隐私优先 AI | 建立用户信任，最大竞争优势 |

---

## 四、技术架构设计

### 4.1 整体架构

```
┌─────────────────────────────────────────────────┐
│                  UI Layer (Compose)              │
│  EditorScreen │ StatsScreen │ TimelineScreen     │
├─────────────────────────────────────────────────┤
│               AI Feature Layer                  │
│  WritingAI │ EmotionAI │ ContentAI │ CreativeAI │
├─────────────────────────────────────────────────┤
│              AI Service Abstraction              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ Local LLM│  │ Cloud API│  │ Embedding│      │
│  │ (ONNX)   │  │ (GPT等)  │  │ (BGE)    │      │
│  └──────────┘  └──────────┘  └──────────┘      │
├─────────────────────────────────────────────────┤
│            Data Layer (Room + Vector DB)         │
│  DiaryEntry │ Tag │ HabitRecord │ AI_Embedding  │
└─────────────────────────────────────────────────┘
```

### 4.2 AI Service 抽象层设计

```kotlin
interface AiService {
    suspend fun chat(messages: List<ChatMessage>): String
    suspend fun streamChat(messages: List<ChatMessage>): Flow<String>
    suspend fun generateEmbedding(text: String): FloatArray
}

// 云端 API 实现
class CloudAiService(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : AiService {
    // 使用 OkHttp/Retrofit 调用 OpenAI 兼容 API
}

// 本地模型实现
class LocalAiService(
    private val context: Context
) : AiService {
    // 使用 ONNX Runtime 加载量化模型
}
```

### 4.3 多模型提供商支持

```kotlin
enum class AiProvider(
    val baseUrl: String,
    val defaultModel: String,
    val displayName: String
) {
    SILICONFLOW(
        "https://api.siliconflow.cn/v1",
        "THUDM/glm-z1-9b-chat",
        "硅基流动"
    ),
    DEEPSEEK(
        "https://api.deepseek.com",
        "deepseek-v4-flash",
        "DeepSeek"
    ),
    ZHIPU(
        "https://open.bigmodel.cn/api/paas/v4",
        "glm-4-flash",
        "智谱 AI"
    ),
    OPENROUTER(
        "https://openrouter.ai/api/v1",
        "auto",
        "OpenRouter"
    ),
    CUSTOM(
        "",  // 用户自定义
        "",
        "自定义"
    )
}
```

### 4.4 API Key 安全存储

```kotlin
// 使用 Android Keystore 加密存储
class SecureApiKeyStorage(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "ai_api_keys",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(provider: AiProvider, apiKey: String) {
        prefs.edit().putString(provider.name, apiKey).apply()
    }

    fun getApiKey(provider: AiProvider): String? {
        return prefs.getString(provider.name, null)
    }
}
```

### 4.5 流式响应处理

```kotlin
class StreamingChatManager {
    private val _responseFlow = MutableStateFlow("")
    val responseFlow: StateFlow<String> = _responseFlow.asStateFlow()

    suspend fun streamChat(messages: List<ChatMessage>) {
        _responseFlow.value = ""

        aiService.streamChat(messages).collect { chunk ->
            _responseFlow.value += chunk
        }
    }
}
```

### 4.6 对话历史管理

```kotlin
@Entity(tableName = "ai_conversations")
data class AiConversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diaryId: Long,  // 关联的日记
    val feature: String,  // 功能类型（润色、续写、摘要等）
    val messages: String,  // JSON 序列化的对话历史
    val createdAt: Long = System.currentTimeMillis()
)
```

### 4.7 Embedding 存储

```kotlin
@Entity(tableName = "ai_embeddings")
data class AiEmbedding(
    @PrimaryKey val diaryId: Long,
    val embedding: ByteArray,  // 序列化的向量
    val model: String,  // 使用的 embedding 模型
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 4.8 用户配置界面

```kotlin
@Composable
fun AiSettingsScreen(viewModel: AiSettingsViewModel) {
    Column {
        // AI 功能开关
        SwitchSetting(
            title = "智能续写",
            description = "根据前文语境生成续写建议",
            checked = settings.writingEnabled,
            onToggle = { viewModel.toggleWriting(it) }
        )

        // 模型提供商选择
        DropdownSetting(
            title = "AI 服务提供商",
            options = AiProvider.entries.map { it.displayName },
            selected = settings.provider.displayName,
            onSelect = { viewModel.setProvider(it) }
        )

        // API Key 输入
        SecureInputSetting(
            title = "API Key",
            value = settings.apiKey,
            onSave = { viewModel.saveApiKey(it) }
        )

        // 用量统计
        UsageStats(
            totalCalls = stats.totalCalls,
            totalTokens = stats.totalTokens,
            estimatedCost = stats.estimatedCost
        )
    }
}
```

### 4.9 隐私保护方案

#### 数据分级

| 级别 | 处理方式 | 适用场景 |
|------|---------|---------|
| **纯本地处理** | 优先 | 情绪基础分类、关键词提取、文本统计 |
| **脱敏后处理** | 折中 | 人名替换、地点泛化后发送云端 |
| **云端处理** | 需授权 | 文本生成、语义搜索、图像生成 |

#### 脱敏处理示例

```
原文：今天和小明去了星巴克，聊了很多关于升职的事情
脱敏：今天和[朋友]去了[咖啡店]，聊了很多关于[工作]的事情
```

#### 数据控制权

- 每个 AI 功能独立开关
- 提供"AI 数据使用记录"
- 支持"仅处理最近 N 天的数据"
- 一键清除 AI 生成的所有缓存和索引

---

## 五、实施路线图

### P0 - 第一阶段（1-2 个月）

| 功能 | 工作量 | 理由 |
|------|--------|------|
| 一键润色 | 1-2 周 | 用户感知强，技术简单 |
| 自动标签推荐 | 1 周 | 解决用户痛点 |
| 智能摘要 | 1 周 | 基础能力，多处可复用 |
| 写作时情绪陪伴 | 3 天 | 差异化功能，实现简单 |
| AI 设置页面 | 1 周 | 基础设施 |

### P1 - 第二阶段（2-3 个月）

| 功能 | 工作量 | 理由 |
|------|--------|------|
| 智能续写 | 1 周 | 写作辅助核心功能 |
| AI 情绪识别 | 2 周 | 情感分析基础 |
| 相似日记推荐 | 2 周 | 增强回顾体验 |
| 语义搜索 | 2 周 | 提升检索体验 |
| 智能日记回顾卡片 | 1 周 | 提升日活和留存 |

### P2 - 第三阶段（3-6 个月）

| 功能 | 工作量 | 理由 |
|------|--------|------|
| 情绪预警 | 3 周 | 高价值但高风险 |
| 情绪洞察报告 | 2 周 | 增强年度/月度报告 |
| AI 配图生成 | 2 周 | 创意功能 |
| 时间胶囊增强 | 2 周 | 扩展现有功能 |
| 日记配诗 | 1 周 | 轻量创意功能 |

### P3 - 长期愿景（6 个月以上）

| 功能 | 工作量 | 理由 |
|------|--------|------|
| 生活模式识别 | 持续迭代 | 需要大量数据积累 |
| 自动提取关键事件 | 3 周 | NER 准确率需提升 |
| 主题式回顾 | 3 周 | 需要深度语义理解 |
| 习惯关联分析 | 2 周 | 需要足够习惯数据 |
| 写作成长追踪 | 2 周 | 需要长期数据积累 |

---

## 六、商业模式

AI 功能天然适合订阅制：

| 层级 | 功能 | 价格建议 |
|------|------|---------|
| **免费层** | 自动标签、智能摘要、写作提示增强 | 免费 |
| **Pro 层** | 语义搜索、情绪洞察报告、AI 配图、风格转换 | ¥15/月 |
| **用量限制** | 免费用户每天 10 次 AI 调用，Pro 无限制 | - |

---

## 七、关键注意事项

1. **中文 NLP 特殊考虑**
   - 分词：选择适合口语化文本的分词工具（jieba、pkuseg）
   - 情绪词典：基于中文情绪词典（HowNet、大连理工大学情感词典）
   - 文化敏感性：中文语境下情绪表达更含蓄，AI 需理解

2. **推荐技术栈**
   - 中文 embedding：BGE-large-zh 或 M3E-large
   - 中文情绪分析：Chinese-RoBERTa-wwm fine-tuning
   - 本地推理：ONNX Runtime 或 TensorFlow Lite

3. **避免的功能陷阱**
   - AI 过度介入：AI 应辅助而非替代用户表达
   - 功能臃肿：保持简单专注，避免变成 Notion
   - 隐私牺牲：隐私是底线
   - 复杂配置：应开箱即用
