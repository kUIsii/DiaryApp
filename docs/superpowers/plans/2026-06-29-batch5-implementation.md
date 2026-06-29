# Batch 5 Implementation Plan

**Goal:** Deeply expand 5 features with AI-driven intelligence → v2.77.00-experimental

**Architecture:** Each feature modifies existing Screen.kt + ViewModel.kt. Adds AI via AiServiceManager.chat(). Stores data in SharedPreferences + Gson. Uses DesignTokens.

---

## Feature 1: WritingLab

**Modify:** `ui/writinglab/WritingLabScreen.kt`, `ui/writinglab/WritingLabViewModel.kt`

AI expansions: style transfer (鲁迅/张爱玲/村上春树/etc), writing challenge generator (AI-personalized), rhetorical suggestions (inline hint dots), creative experiment templates

## Feature 2: EmotionArc

**Modify:** `ui/emotionarc/EmotionArcScreen.kt`, `ui/emotionarc/EmotionArcViewModel.kt`

AI expansions: emotion pattern discovery (weekly/weather/social cycles), trigger analysis (keyword→impact), 7-day emotion forecast, narrative summary, period comparison

## Feature 3: MemoryArt

**Modify:** `ui/memoryart/MemoryArtScreen.kt`, `ui/memoryart/MemoryArtViewModel.kt`

AI expansions: visual art description → Canvas programmatic rendering (watercolor/oil/sketch/abstract/ink), art evolution series for recurring themes, memory collage (composite from time range), AI prompt export (Midjourney/SD/DALL-E), art mood board

## Feature 4: WritingFingerprint

**Modify:** `ui/writingfingerprint/WritingFingerprintScreen.kt`, `ui/writingfingerprint/WritingFingerprintViewModel.kt`

AI expansions: 6-dimension style analysis (vocabulary/sentence/emotion/time/topic/rhetoric radar chart), style evolution timeline (clustered periods), comparative analysis, writing persona, writing health score

## Feature 5: Focus

**Modify:** `ui/focus/FocusScreen.kt`, `ui/focus/FocusViewModel.kt`

AI expansions: adaptive pre-session recommendations (duration/sound/goal), AI writing goal generation from recent topics, intelligent break suggestions (flow monitoring), post-session insights (summary/mood/highlight sentence), focus streak system

---

## Build & Release
- Fix compile errors
- Bump version to 2.77.00-experimental / versionCode 27700
- `assembleExperimentalRelease`
- Git: commit, tag v2.77.00-experimental, push
- `gh release create` with APK
