# Batch 5: 5 Features AI Deep Expansion Design

**Version:** v2.77.00-experimental (planned)
**Date:** 2026-06-29
**Status:** Draft

---

## AI Philosophy (applies to all)

AI is the **intelligent engine** that transforms static features into adaptive, insightful tools. Each feature uses AI not just for content generation, but for:
- **Pattern recognition** — discovering what the data means
- **Adaptive behavior** — changing how the feature works based on context
- **Proactive insight** — showing users what they didn't know they needed to see

---

## Feature 1: WritingLab (写作实验室)

**Current state:** Basic writing experiments UI, 55% complete.

### Deep Expansion

#### AI Style Transfer
- User writes a paragraph, then selects a target style:
  - 鲁迅风格 / 张爱玲风格 / 村上春树风格 / 古诗风格 / 简洁风格 / 华丽风格
- AI rewrites the content in the selected style while preserving meaning
- Show side-by-side comparison (original vs rewritten)
- User can rate the result (有用/一般/不适用) — ratings train future style matching

#### Writing Challenge Generator
- AI generates personalized writing challenges based on user's writing history:
  - "今天用 200 字描述你窗外的声音" (if user rarely writes sensory details)
  - "尝试写一段完全不用'我'的日记" (if user writes very self-focused)
  - "用第三人称写今天发生的事" (if user always writes in first person)
- Challenges adapt based on completion rate and past challenge performance
- Streak tracking for consecutive challenge completions

#### AI Rhetorical Suggestions
- After user finishes a paragraph, AI analyzes and suggests:
  - 修辞建议: "这里可以用一个比喻来增强表现力"
  - 结构建议: "这一段可以分成两段，节奏会更好"
  - 词汇建议: "你用'快乐'开头了三个句子，可以换成'愉悦''开心'"
- Shown as inline hint dots (not intrusive popups)
- User can tap to expand, apply, or dismiss

#### Creative Experiment Templates
- AI generates experiment templates based on user's writing patterns:
  - 感官日记: focus on 5 senses
  - 对话日记: write as script
  - 倒叙日记: start from end
  - 诗歌日记: convert entry to poem
- Each template comes with example from past entries (AI-adapted)

### Data Model

```kotlin
data class WritingExperiment(
    val id: String,
    val type: String,           // style_transfer/challenge/suggestion/template
    val originalText: String?,
    val resultText: String?,
    val metadata: Map<String, String>,
    val rating: Int?,           // 0-5, null if not rated
    val createdAt: Long
)
```

Storage: SharedPreferences + Gson.

---

## Feature 2: EmotionArc (情绪弧线)

**Current state:** Basic mood chart, 65% complete.

### Deep Expansion

#### AI Emotion Pattern Discovery
- AI analyzes emotion data across user-selected time range and discovers patterns:
  - Weekly cycles: "你周一的情绪平均比周五低 0.8"
  - Weather correlation: "下雨天你的情绪评分比晴天低 0.5"
  - Social correlation: "提到'朋友'的日记，情绪平均高 1.2"
  - Seasonal patterns: "每年 3 月你的情绪波动最大"
- Presented as insight cards below the emotion chart

#### Emotion Trigger Analysis
- For any selected day/week, AI reads the diary text and identifies possible triggers:
  - 工作事件 / 人际关系 / 健康 / 天气 / 特殊日期
- Shows: "这天你提了 3 次'加班'，情绪从早上的 7 分降到晚上的 3 分"
- Aggregated across time: "过去 30 天，'加班'相关的日记情绪平均低 1.5 分"

#### Predictive Emotion Forecast
- AI predicts next 7 days' emotion based on:
  - Historical patterns (same day of week, same season)
  - Upcoming events (from calendar/todo if available)
  - Recent emotion trajectory
- Shows as dashed line on chart extending 7 days ahead
- Confidence indicator (high/medium/low)

#### Narrative Summary Generation
- For any selected period (week/month/quarter/year), AI generates:
  - 2-3 sentence emotional narrative
  - Key turning points
  - Overall trend description
- Example: "这周你的情绪像过山车——周二的低落是因为工作压力，周四的朋友聚会让你回升，周末趋于平稳。"

#### Emotion Arc Comparison
- Compare two periods side by side:
  - Same month, different years
  - Before/after a life event
  - User-selected custom ranges
- AI highlights: "今年 3 月比去年同期的情绪稳定多了，波动幅度减少了 35%"

### Data Model

```kotlin
data class EmotionAnalysis(
    val id: String,
    val periodStart: Long,
    val periodEnd: Long,
    val patterns: List<EmotionPattern>,
    val triggers: List<EmotionTrigger>,
    val forecast: List<ForecastPoint>?,
    val narrativeSummary: String?,
    val comparisonId: String?
)

data class EmotionPattern(
    val type: String,     // weekly_cycle/weather/social/seasonal
    val description: String,
    val confidence: Float,
    val relatedEntryIds: List<Long>
)

data class EmotionTrigger(
    val keyword: String,
    val impact: Float,     // -2.0 to +2.0
    val frequency: Int,
    val examples: List<Pair<Long, String>>  // entryId, snippet
)
```

Storage: SharedPreferences + Gson.

---

## Feature 3: MemoryArt (记忆艺术)

**Current state:** Basic art generation trigger, 68% complete.

### Deep Expansion

#### AI Visual Art Generation from Diary Content
- User selects an entry or a theme
- AI reads the content and generates a visual art description (prompt)
- Renders art using Canvas (programmatic generation based on AI parameters):
  - Color palette extracted from mood + content keywords
  - Shapes/patterns based on content themes
  - Composition based on emotional arc of the entry
- Multiple art styles: 水彩 / 油画 / 素描 / 抽象 / 水墨

#### Art Evolution Series
- For recurring themes (e.g., "每次写到旅行"), AI generates a series of artworks
- Shows evolution over time: "你对旅行的感受从'兴奋'变成了'怀念'"
- Each artwork in the series has slightly different palette/composition reflecting mood change
- Series displayed as a scrollable gallery

#### Memory Collage
- AI selects multiple entries from a time range
- Extracts key elements from each: colors, emotions, keywords
- Generates a single composite artwork representing the entire period
- Elements layered with varying opacity based on emotional intensity
- User can tap any element to see which entry it came from

#### AI Art Prompt Export
- User can export the AI-generated art description as a prompt
- Compatible formats: Midjourney / Stable Diffusion / DALL-E prompt text
- Copy to clipboard, user can use in external AI art tools
- Prompt includes: style, composition, colors, mood, key elements

#### Art Mood Board
- All generated artworks shown in a grid
- Filterable by: time range, mood, style, theme
- Each artwork shows: source entry excerpt, date, mood
- User can favorite artworks, export as PDF collection

### Data Model

```kotlin
data class MemoryArtwork(
    val id: String,
    val entryIds: List<Long>,
    val style: String,         // watercolor/oil/sketch/abstract/ink
    val palette: List<String>, // hex colors
    val composition: ArtComposition,
    val aiPrompt: String,
    val promptFormat: String,  // midjourney/stablediffusion/dalle
    val isFavorite: Boolean,
    val seriesId: String?,     // null if standalone
    val createdAt: Long
)

data class ArtComposition(
    val shapes: List<ArtShape>,
    val layout: String,      // radial/grid/flow/symmetric
    val intensity: Float      // 0.0 - 1.0
)
```

Storage: SharedPreferences + Gson.

---

## Feature 4: WritingFingerprint (写作指纹)

**Current state:** Basic style stats, 78% complete.

### Deep Expansion

#### AI Style Analysis Dimensions
- AI analyzes entire writing history across these dimensions:
  - 词汇丰富度 (vocabulary richness) — unique words / total words
  - 句式复杂度 (sentence complexity) — average sentence length, clause count
  - 情感表达 (emotional expression) — emotional vocabulary ratio
  - 时间视角 (temporal perspective) — past/present/future tense distribution
  - 主题偏好 (topic preference) — recurring themes and topics
  - 修辞使用 (rhetorical devices) — metaphor, analogy usage frequency
- Each dimension shown as a radar chart, with trend arrow

#### Writing Style Evolution Timeline
- AI divides writing history into style periods (clustered by style similarity)
- Each period has: date range, distinctive characteristics, sample entry
- Timeline visualization: colored segments with transition points
- AI explains what changed: "2025 年 6 月后，你的句子变短了，词汇更口语化——是因为开始写工作日记吗？"

#### Comparative Style Analysis
- User selects a subset of entries (e.g., by tag, by time range)
- AI compares the subset against the overall corpus:
  - "写旅行日记时，你的句子比平时长 20%，用了更多感官词汇"
  - "工作相关的日记中，你的情绪词汇种类减少了 50%"
- Shown as highlighted differences in the radar chart overlay

#### AI Writing Persona
- Based on all dimensions, AI generates a writing persona description:
  - "你是一个细腻的观察者，擅长用比喻描述日常。你的句子像溪流一样自然，但在写压力时会变得急促简短。"
- Persona updates as writing evolves (user can view historical personas)
- Persona shared as a card that can be exported

#### Writing Health Score
- AI computes a weekly writing health score based on:
  - 一致性 (consistency) — writing frequency vs personal baseline
  - 多样性 (diversity) — vocabulary and topic variety
  - 情感深度 (emotional depth) — emotional vocabulary usage
  - 自我反思 (self-reflection) — reflective language usage
- Score shown as a gauge (0-100)
- Tips: "这周的写作健康分 82，比上周高 5 分。你尝试了新的话题，继续保持！"

### Data Model

```kotlin
data class WritingFingerprintAnalysis(
    val id: String,
    val analyzedAt: Long,
    val dimensions: Map<String, Float>,  // dimension -> score 0-100
    val stylePeriods: List<StylePeriod>,
    val persona: String,
    val healthScore: WritingHealthScore?
)

data class StylePeriod(
    val startDate: Long,
    val endDate: Long,
    val characteristics: List<String>,
    val sampleEntryId: Long,
    val label: String
)

data class WritingHealthScore(
    val score: Int,
    val consistency: Float,
    val diversity: Float,
    val emotionalDepth: Float,
    val selfReflection: Float,
    val tips: List<String>
)
```

Storage: SharedPreferences + Gson.

---

## Feature 5: Focus (专注写作模式)

**Current state:** Basic timer + do-not-disturb, 78% complete.

### Deep Expansion

#### AI Adaptive Environment
- Before starting a session, AI recommends:
  - 时长: based on recent writing sessions and time of day
  - 背景音: based on emotional state (calm/focused/energetic from diary analysis)
  - 目标: personalized writing goal ("今天写 300 字关于....")
- Recommendations shown as configurable pre-session screen
- User can accept, modify, or decline each recommendation

#### AI Writing Goal Generation
- Based on recent diary topics and uncompleted entries, AI generates specific writing goals:
  - "上次提到和朋友的误会，今天想继续写吗？"
  - "你最近 3 天都没写日记了，要不要从写下今天的三件小事开始？"
- Goals are specific, achievable, and personalized
- Past goal completion rate tracked and used for future suggestions

#### Intelligent Break Suggestions
- During a session, AI monitors:
  - Writing flow (time between keypresses, words per minute)
  - Session duration
  - Emotional tone of what's being written
- Suggests breaks when:
  - Writing speed drops significantly (writer's block)
  - User has been writing >45 minutes
  - Emotional content becomes very intense
- Break suggestion includes: "休息 5 分钟，喝杯水" or "试试换个角度写这段"

#### Post-Session Insights
- After session ends, AI generates:
  - 写作总结: "你用了 25 分钟写了 480 字，比平时快 15%"
  - 情绪变化: "开头有些烦躁，写到中段时平静下来了"
  - 亮点句子: AI picks the best sentence from the session
- Shown as a session recap card

#### Focus Streak System
- AI tracks and encourages streaks:
  - Daily writing streak (existing)
  - Focus session streak (new): consecutive days with at least one focus session
  - Goal completion streak: consecutive completed goals
- Streak milestones trigger AI-generated encouragement messages
- Weekly focus report: "这周你完成了 5 次专注写作，总共 2 小时 15 分钟"

### Data Model

```kotlin
data class FocusSession(
    val id: String,
    val plannedDuration: Int,    // minutes
    val actualDuration: Int,     // minutes
    val wordCount: Int,
    val goalText: String?,
    val ambientSoundType: String?,
    val breaks: List<Long>,      // break timestamps
    val aiInsight: String?,
    val moodTrend: String?,
    val completedGoal: Boolean,
    val createdAt: Long
)
```

Storage: SharedPreferences + Gson.

---

## Implementation Plan

### Version
- Version name: 2.77.00-experimental
- Version code: 27700

### Shared Infrastructure
- SharedPreferences + Gson for all new data
- AiServiceManager.chat() with structured prompts
- Fallback behavior when AI unavailable
- All UI text in Chinese
- DesignTokens for spacing/fonts
- GlassCard/GradientBackground/PageHeader from components
- Touch targets >= 44dp, font body >= 14sp

### Files to Modify
- `ui/writinglab/WritingLabScreen.kt` + `WritingLabViewModel.kt` (create or expand)
- `ui/emotionarc/EmotionArcScreen.kt` + `EmotionArcViewModel.kt`
- `ui/memoryart/MemoryArtScreen.kt` + `MemoryArtViewModel.kt`
- `ui/writingfingerprint/WritingFingerprintScreen.kt` + `WritingFingerprintViewModel.kt`
- `ui/focus/FocusScreen.kt` + `FocusViewModel.kt`
