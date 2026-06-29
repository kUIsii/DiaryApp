# Batch 4: 5 Features Deep Expansion Design

**Version:** v2.76.00-experimental (planned)
**Date:** 2026-06-29
**Status:** Draft

---

## AI Integration Philosophy (applies to all 5 features)

AI is NOT used to:
- Generate static content and display it (that's `Stats`)
- Provide a generic chat interface

AI IS used to:
- **Infer intent**: understand what the user is doing and why, then act on it
- **Make connections**: link across diary entries, time periods, and feature boundaries
- **Proactively surface insights**: show the user things they didn't ask for but will find valuable
- **Drive behavior**: not just generate text, but change how the feature works based on context

---

## Feature 1: LockscreenQuickWrite (锁屏快写)

**Current state:** Basic text input on lockscreen, saves as diary entry. 30% complete.

### Deep Expansion

#### Quick Capture with AI Classification
- After user finishes writing (or pauses >2s with >10 chars), AI automatically classifies:
  - `灵感` (inspiration) → link to WritingLab
  - `待办` (todo) → suggest creating a TodoItem
  - `今日感想` (daily thought) → save as diary entry with mood detection
  - `梦境` (dream) → add dream tag, suggest memory art generation
  - `摘录` (quote) → save to quote collection
- Classification result shown as a subtle chip below input, user can tap to change

#### Intelligent Follow-up Suggestions
- Based on classification + content, AI suggests next action as a single button:
  - Todo detected: "要设一个提醒吗？" → opens time picker
  - Emotional content: "今天心情似乎不太好，需要写一封安慰信给自己吗？"
  - Action item: "需要我帮你跟踪这件事吗？"
- Suggestions appear 1 at a time, dismissible

#### Mood-Driven Lockscreen Adaptation
- AI infers mood from recent entries (last 3 days) and time of day
- On lockscreen, shows subtle contextual prompt before user starts typing:
  - Morning + happy trend: "早上好 ☀️ 今天有什么新想法？"
  - Evening + stressed: "辛苦了，记下来就放下吧"
- Prompt changes based on real-time AI mood analysis, not hardcoded schedule

#### Smart Linking
- After saving, AI checks if content relates to any recent entry (last 7 days)
- If match found (>0.65 similarity), shows: "这段内容和周三的日记有关联，要放在一起吗？"
- If user confirms, creates a cross-reference in SharedPreferences

### Data Model

```kotlin
data class QuickWriteEntry(
    val id: Long,
    val content: String,
    val category: String,       // AI-classified: inspiration/todo/daily/dream/quote
    val mood: Float,            // -1.0 to 1.0
    val linkedEntryId: Long?,   // cross-reference
    val followUpAction: String?, // reminder/track/letter
    val createdAt: Long
)
```

Storage: SharedPreferences + Gson (List<QuickWriteEntry>), no Room migration.

### Key Behaviors
- Touch targets >= 44dp
- Font >= 14sp
- AI classification: use AiServiceManager.chat() with structured prompt
- Classification must complete within 2s or fall back to "daily"
- Follow-up suggestion shown as Material3 Card inside the lockscreen overlay

---

## Feature 2: AdaptiveInterface (自适应界面)

**Current state:** Basic foldable detection, 20% complete.

### Deep Expansion

#### AI-Powered Layout Learning
- AI tracks: time of day, day of week, recent activities, screen size/posture
- Learns which UI panels the user uses most at different times
- On foldable unfold: AI predicts next action and pre-positions panels
  - E.g., morning: calendar left + diary list right; evening: stats left + writing right
- No fixed breakpoints — AI clusters usage patterns into 3-5 layouts

#### Context-Aware Widget Prioritization
- On home screen, AI reorders widgets based on:
  - Recent feature usage (last 48h weighted)
  - Seasonal relevance (e.g., year-end review in December)
  - Emotional state (e.g., show mood map if sadness trend detected)
- User can lock a widget to a position (exempt from AI reordering)

#### Adaptive Typography & Contrast
- AI detects ambient light via time + screen brightness sensor
- Automatically adjusts font weight, contrast, and background opacity
- Learning: if user manually adjusts, AI records the context and adjusts proactively next time
- Minimum font body: 14sp, minimum contrast ratio: 4.5:1

#### Screen Mode Optimization
- Phone: full-featured single column
- Foldable (unfolded): multi-column with AI-predicted panel arrangement
- Tablet: persistent sidebar + content area, AI chooses sidebar widgets
- Desktop mode (DeX): windowed multi-panel, AI arranges as dashboard

### Data Model

```kotlin
data class LayoutPattern(
    val id: String,
    val timeRange: Pair<Int, Int>,    // hour range
    val dayOfWeek: Int,               // 0=all, 1-7=specific
    val posture: String,              // folded/unfolded/tablet/dex
    val widgetOrder: List<String>,    // widget IDs
    val panelConfig: Map<String, Any>,
    val usageCount: Int
)
```

Storage: SharedPreferences + Gson.

### Key Behaviors
- All layout changes animated (500ms fade-in)
- User can override any AI decision with 1 tap
- AI learning stored locally, never uploaded
- New device posture detected via WindowInsets or configuration change

---

## Feature 3: PersonalYearbook (个人年鉴)

**Current state:** Basic grid of entries, 30% complete.

### Deep Expansion

#### AI Narrative Arc Extraction
- AI reads all entries for the year and identifies 3-7 narrative arcs:
  - Work/career changes
  - Relationship developments
  - Interest/hobby shifts
  - Personal growth themes
- Each arc gets: title, key entries, turning point, emotional trajectory
- Arcs presented as chapters in the yearbook

#### AI "Day of the Month" Selection
- For each month, AI picks the most representative entry based on:
  - Content density (longest, most detailed)
  - Emotional significance (extreme positive/negative)
  - Impact (how many later entries reference it)
  - Diversity (spread across different topics)
- Not random, not just longest — truly meaningful selection

#### Personalized Year Metaphor
- AI generates: "你的这一年像 _____" (Your year was like _____)
  - Based on overall narrative arc, writing style, emotional journey
  - Examples: "一部成长小说" (a coming-of-age novel), "一次长途旅行" (a long journey), "一幅拼图" (a puzzle)
  - Changes as user reads through the yearbook (metaphor evolves)
- Shown on yearbook cover and each chapter page

#### Visual Timeline
- Horizontal scrolling timeline with:
  - Monthly mood color bars (gradient from red to blue)
  - Key event markers (AI-extracted: trips, achievements, relationship changes)
  - Writing frequency heatmap
- Tap on any point to expand entry

#### AI Photo Curation
- If entry has photos, AI scores them for:
  - Quality (clarity, composition via basic heuristics)
  - Memory value (uniqueness, emotional weight from text)
  - Diversity (not 10 photos of same meal)
- Top 12 photos selected for yearbook gallery

### Data Model

```kotlin
data class YearbookData(
    val year: Int,
    val arcs: List<NarrativeArc>,
    val monthHighlights: List<MonthHighlight>,
    val metaphor: String,
    val metaphorEvolution: List<Pair<String, String>>, // chapter -> metaphor
    val topPhotos: List<String>,
    val stats: YearbookStats
)

data class NarrativeArc(
    val title: String,
    val entries: List<Long>,
    val turningPoint: Long,
    val emotionTrajectory: List<Pair<Long, Float>>
)

data class MonthHighlight(
    val month: Int,
    val entryId: Long,
    val reason: String,          // AI-generated reason for selection
    val mood: Float,
    val photos: List<String>
)
```

Storage: SharedPreferences + Gson (regenerated each year, cached).

### Key Behaviors
- AI analysis runs on first open of the year, cached
- If new entries added, AI re-analyzes incrementally
- Yearbook exportable as text/markdown
- Touch targets >= 44dp, font >= 14sp

---

## Feature 4: AnnualReviewStory (年度回顾故事)

**Current state:** Basic stat aggregation, 60% complete.

### Deep Expansion

#### Multi-Chapter Narrative Generation
- AI generates 3-7 chapters based on narrative arcs from Yearbook
- Each chapter:
  - Title (AI-generated, evocative)
  - Summary (2-3 paragraphs in the user's writing style)
  - Key entries (linked, tappable)
  - Emotional arc visualization (mini sparkline)
- Writing style mimics the user's own patterns (vocabulary, sentence length, tone)

#### Pattern Discovery
- AI surfaces patterns the user likely missed:
  - "你每次换工作前，都会更多地写到'自由'这个词"
  - "今年你提到母亲的次数比去年少了 60%"
  - "你的最佳写作时间是晚上 10-11 点，平均多了 200 字"
- Presented as "你知道吗？" cards throughout the story

#### Cross-Year Comparison
- If prior year data exists, AI compares:
  - Emotional vocabulary distribution
  - Topic frequency changes
  - Writing volume trends
  - Relationship/social mentions
- Visualized as side-by-side charts within the story

#### Interactive Story Experience
- Not a static report — user can:
  - Interject: tap a paragraph to write a "2026 年的注释" (note from the future)
  - Expand: tap "展开更多" on any insight to see supporting entries
  - Redirect: "这个月其实更重要的是..." to add context
  - User annotations saved and fed back into next year's analysis
- AI adapts future analysis based on user corrections

#### AI "Blind Spot" Detection
- AI identifies periods with low writing and infers possible reasons
- Shows: "10月5日-15日你没有写日记，但你16日写了'终于结束了'——那段时间发生了什么？"
- Not accusatory, gently curious — framed as observation

### Data Model

```kotlin
data class AnnualStory(
    val year: Int,
    val chapters: List<StoryChapter>,
    val patterns: List<DiscoveredPattern>,
    val crossYearInsights: List<CrossYearInsight>?,
    val userAnnotations: List<UserAnnotation>,
    val blindSpotNotes: List<BlindSpot>
)
```

Storage: SharedPreferences + Gson.

### Key Behaviors
- AI generation may take 5-15s, show skeleton loading
- Each chapter generated independently for parallel loading
- User annotations persisted and used in future analysis
- Export as markdown with embedded links to entries

---

## Feature 5: ChatWithPastSelf (与过去的自己对话)

**Current state:** Basic AI chat with diary context, 40% complete.

### Deep Expansion

#### Proactive Observation (not reactive Q&A)
- When user opens a past month, AI **initiates** with an observation:
  - "那个月你反复提到要换工作，最后你做了决定吗？"
  - "我看到你 3 月 15 号刚开始学吉他，到 6 月就没再提了，现在还在弹吗？"
- Based on reading the month's entries holistically, not just the last one
- Each observation surfaces a **narrative gap** that invites response

#### Cross-Time Debate
- User can pick two time periods: "让 2024 年 1 月的我和 2024 年 12 月的我对话"
- AI builds two personas based on each period's entries
- Personas debate a topic (e.g., "要不要换工作") using actual arguments from entries
- User watches the debate, can interject with "其实我现在觉得..."
- AI incorporates the interjection into both personas' understanding

#### Inference from Silence
- AI notices gaps and inferential connections:
  - "你 4 月 10 号的日记说'终于决定了'，但没有说是什么决定。后来 4 月 20 号你提到了新公司。—— 那个决定是换工作吗？"
  - "你这周写得比平时少，通常这种情况发生在你压力大的时候，需要聊聊吗？"
- Presented as gentle questions, not accusations

#### Growth Visualization
- For any topic, user can ask: "我在这件事上的看法怎么变化的？"
- AI traces mentions across time, showing evolution chart
- Extracts: initial position → key events → changed views → current position
- Shows specific quotes at each stage

#### "Letter to Past/Future Self"
- AI generates a letter from past self (using their writing style) to present
- Or from present to future, based on current concerns and past patterns
- User can edit the letter, AI adjusts future versions based on edits
- Letters saved and available in yearbook

### Data Model

```kotlin
data class PastSelfSession(
    val id: String,
    val focusPeriod: Pair<Long, Long>,  // time range
    val topic: String?,
    val observations: List<AIObservation>,
    val debateConfig: DebateConfig?,
    val letters: List<TimeLetter>,
    val createdAt: Long
)

data class AIObservation(
    val content: String,
    val sourceEntries: List<Long>,
    val type: String   // observation/debate_point/inference/growth
)

data class TimeLetter(
    val direction: String,    // past_to_present / present_to_future
    val content: String,
    val sourcePeriod: Pair<Long, Long>,
    val userEdits: List<String>  // edit history
)
```

Storage: SharedPreferences + Gson.

### Key Behaviors
- All AI responses must cite specific entries as source
- User can always say "这个不对" and AI adjusts its understanding
- Sessions persisted for 30 days, then summarized into yearbook
- No generic chat mode — every interaction is grounded in diary data

---

## Implementation Plan

### Shared Infrastructure (all features)
- SharedPreferences + Gson for all new data (no Room migrations)
- AiServiceManager.chat() with structured prompts
- Fallback behavior if AI unavailable (graceful degradation)
- All UI text in Chinese

### Development Order
Features are independent → can be developed in parallel (5 agents).

### Verification Criteria
- Each feature compiles without errors
- AI integration works (AiServiceManager available)
- UI meets minimum touch targets (44dp) and font sizes (14sp)
- Feature can be navigated to from the main app
- No Room migrations added

### Version Bump
- Version name: 2.76.00-experimental
- Version code: 27600
