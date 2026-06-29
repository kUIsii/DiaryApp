# Batch 4: 5 Features Deep Expansion Implementation Plan

> **For agentic workers:** Each feature is independent → work in parallel. Use `subagent-driven-development` or direct implementation. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Deeply expand 5 features with AI-driven intelligence, completing v2.76.00-experimental.

**Architecture:** Each feature modifies its existing Screen.kt + ViewModel.kt, adds AI via AiServiceManager.chat(), stores data in SharedPreferences + Gson (no Room). ChatWithPastSelf is new (create directory + nav entry).

**Tech Stack:** Jetpack Compose (BOM 2023.10.01), SharedPreferences + Gson, AiServiceManager, DesignTokens

**Constraints:**
- Compose BOM 2023.10.01 — no HorizontalDivider, PullToRefreshBox, FilterChip, LazyColumn.maxHeight; use Divider, Box, heightIn(max=)
- Touch targets >= 44dp, font body >= 14sp, small >= 11sp
- All UI text in Chinese
- No emojis in code or UI
- Use GlassCard/GradientBackground/PageHeader from ui/components/
- AI via `AiServiceManager(application).chat(request)` — check `isAiEnabled()` first
- Data storage: SharedPreferences + Gson (not Room)
- No flavor-specific version overrides in build.gradle.kts

---

## Feature 1: LockscreenQuickWrite — AI Deep Expansion

**Files (modify):**
- `app/src/main/java/com/diary/app/ui/lockscreenquickwrite/LockScreenQuickWriteScreen.kt`
- `app/src/main/java/com/diary/app/ui/lockscreenquickwrite/LockScreenQuickWriteViewModel.kt`

**Key changes:**
- Add AI classification after write (pause >2s with >10 chars → classify as 灵感/待办/今日感想/梦境/摘录)
- Add follow-up suggestion button (based on classification + content)
- Add mood-driven contextual prompt on lockscreen before typing
- Add smart linking to recent entries (similarity check)
- Migrate from raw JSONArray/JSONObject to Gson
- Use DesignTokens for all spacing/fonts
- Store as `List<QuickWriteEntry>` in `quick_notes` SharedPreferences

---

## Feature 2: AdaptiveInterface — AI Deep Expansion

**Files (modify):**
- `app/src/main/java/com/diary/app/ui/adaptiveinterface/AdaptiveInterfaceScreen.kt`
- `app/src/main/java/com/diary/app/ui/adaptiveinterface/AdaptiveInterfaceViewModel.kt`

**Key changes:**
- Add AI layout learning engine (track time/day/activity patterns → cluster into layouts)
- Add context-aware widget reordering suggestion section
- Add adaptive typography/contrast controls (auto-detection + manual override learning)
- Add screen mode optimization (phone/foldable/tablet/DeX presets)
- Store `LayoutPattern` list in SharedPreferences
- Use DesignTokens

---

## Feature 3: PersonalYearbook — AI Deep Expansion

**Files (modify):**
- `app/src/main/java/com/diary/app/ui/personalyearbook/PersonalYearbookScreen.kt`
- `app/src/main/java/com/diary/app/ui/personalyearbook/PersonalYearbookViewModel.kt`
- `app/src/main/java/com/diary/app/ui/personalyearbook/PdfExporter.kt`

**Key changes:**
- Add AI narrative arc extraction (3-7 arcs per year: work, relationships, hobbies, growth)
- Add AI "day of the month" selection (content density + emotional significance + impact)
- Add personalized year metaphor generation
- Add visual timeline with mood bars + event markers
- Add AI photo curation (score photos by quality + memory value + diversity)
- Store `YearbookData` (arcs, highlights, metaphor, photos) in SharedPreferences
- Use DesignTokens, update PDF export with new content

---

## Feature 4: AnnualReviewStory — AI Deep Expansion

**Files (modify):**
- `app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt`
- `app/src/main/java/com/diary/app/ui/annualreport/AnnualReportViewModel.kt`

**Key changes:**
- Add multi-chapter narrative generation (3-7 chapters mimicking user's writing style)
- Add pattern discovery cards ("你知道吗？" — user-missed patterns)
- Add cross-year comparison (if prior year data exists)
- Add interactive features: user annotations, expand insights, redirect focus
- Add AI "blind spot" detection (infer from writing gaps)
- Store `AnnualStory` data in SharedPreferences
- Use DesignTokens

---

## Feature 5: ChatWithPastSelf — NEW Feature

**Files (create):**
- `app/src/main/java/com/diary/app/ui/pastself/` (new directory)
- `app/src/main/java/com/diary/app/ui/pastself/PastSelfScreen.kt`
- `app/src/main/java/com/diary/app/ui/pastself/PastSelfViewModel.kt`

**Files (modify):**
- `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt` — add Screen.PastSelf route + nav entry

**Implementation:**
- Screen: date range picker → AI generates proactive observation → user responds → cross-time debate mode → letter to past/future self
- ViewModel: reads diary entries for selected period, calls AiServiceManager for observations, manages debate personas, persists sessions via SharedPreferences + Gson
- All AI responses must cite specific entries
- No generic chat mode — every interaction grounded in diary data

---

## Build & Release

- Fix compile errors
- Bump version to 2.76.00-experimental / versionCode 27600
- `assembleExperimentalRelease`
- Git tag + commit + push
- `gh release create` with APK
