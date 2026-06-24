# DiaryApp Home, Tools, Theme, and Preview Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework the app's home screen, tools screen, and theme system so the core product feels intentional and usable, while fixing the preview HTML so multiple phone mockups render clearly without overlap.

**Architecture:** Keep the existing Compose navigation and screen boundaries, but replace the surface composition inside `HomeScreen` and `ToolsScreen` with denser, more structured layouts. Extend the theme model with additional theme modes and wire them through `ThemePreferences`, `Theme.kt`, and the shared background/card primitives so the new colors affect the whole app consistently. Update the preview HTML/CSS to render a horizontally scrollable phone comparison grid with independent light/dark and theme controls.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, shared theme utilities, static HTML/CSS/JavaScript preview pages.

---

### Task 1: Extend theme modes and palette wiring

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/theme/ThemeMode.kt`
- Modify: `app/src/main/java/com/diary/app/ui/theme/ThemePreferences.kt`
- Modify: `app/src/main/java/com/diary/app/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/diary/app/ui/components/GradientBackground.kt`
- Modify: `app/src/main/java/com/diary/app/ui/components/GlassCard.kt`
- Modify: `app/src/main/java/com/diary/app/DiaryApplication.kt`
- Modify: `app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Add new theme modes and migration mapping**

```kotlin
enum class ThemeMode(val label: String, val category: String = "blue") {
    PURE_LIGHT("Fog Blue Light", "blue"),
    PURE_DARK("Fog Blue Dark", "blue"),
    MOSS_GREEN_LIGHT("Moss Green Light", "green"),
    MOSS_GREEN_DARK("Moss Green Dark", "green"),
    OCEAN_LIGHT("Ocean Light", "cyan"),
    OCEAN_DARK("Ocean Dark", "cyan"),
    PETAL_LIGHT("Petal Light", "rose"),
    PETAL_DARK("Petal Dark", "rose"),
    SAND_LIGHT("Sand Light", "amber"),
    SAND_DARK("Sand Dark", "amber")
}
```

```kotlin
private fun migrateOldTheme(context: Context): ThemeMode {
    val prefs = context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
    val oldName = prefs.getString(KEY_THEME_MODE, null)

    val newMode = when (oldName) {
        null -> ThemeMode.PURE_LIGHT
        "PURE_LIGHT" -> ThemeMode.PURE_LIGHT
        "PURE_DARK" -> ThemeMode.PURE_DARK
        "MOSS_GREEN_LIGHT" -> ThemeMode.MOSS_GREEN_LIGHT
        "MOSS_GREEN_DARK" -> ThemeMode.MOSS_GREEN_DARK
        "SYSTEM" -> ThemeMode.PURE_LIGHT
        "GRADIENT" -> ThemeMode.PURE_LIGHT
        "WARM_ROSE" -> ThemeMode.PETAL_LIGHT
        "OCEAN_BLUE" -> ThemeMode.OCEAN_LIGHT
        else -> ThemeMode.PURE_LIGHT
    }

    setThemeMode(context, newMode)
    return newMode
}
```

- [ ] **Step 2: Add color schemes and extended color sets for each new mode**

```kotlin
private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF99F6E4),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    error = ErrorColor,
    background = Color(0xFFF0FDFF),
    onBackground = Color(0xFF103D42),
    surface = Color.White,
    onSurface = Color(0xFF16363A),
    surfaceVariant = Color(0xFFE0F7FA),
    onSurfaceVariant = Color(0xFF4B6B70),
)
```

Reuse the same pattern for `OceanDarkColorScheme`, `PetalLightColorScheme`, `PetalDarkColorScheme`, `SandLightColorScheme`, and `SandDarkColorScheme`, then add matching `ExtendedColors` blocks with distinct gradient starts and ends.

- [ ] **Step 3: Route theme mode selection through the app state**

```kotlin
fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    ThemePreferences.setThemeMode(this, mode)
}
```

Keep the existing app-wide `themeMode` flow and ensure all new enum values survive relaunch and theme switching.

- [ ] **Step 4: Update theme pickers to expose the new presets**

```kotlin
val themeOptions = listOf(
    ThemeMode.PURE_LIGHT,
    ThemeMode.PURE_DARK,
    ThemeMode.MOSS_GREEN_LIGHT,
    ThemeMode.MOSS_GREEN_DARK,
    ThemeMode.OCEAN_LIGHT,
    ThemeMode.OCEAN_DARK,
    ThemeMode.PETAL_LIGHT,
    ThemeMode.PETAL_DARK,
    ThemeMode.SAND_LIGHT,
    ThemeMode.SAND_DARK
)
```

Use the existing theme selection UI patterns so light/dark is still a visible toggle, but users can choose more palettes instead of only two.

- [ ] **Step 5: Verify theme-backed surfaces use the selected palette**

Run: `./gradlew :app:testDebugUnitTest`
Expected: Theme-related tests still pass and all callers compile against the expanded enum.

### Task 2: Redesign the home screen structure

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/home/CalendarView.kt`
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/components/GlassCard.kt`
- Modify: `app/src/main/java/com/diary/app/ui/components/EmptyState.kt`

- [ ] **Step 1: Rebuild the top section around date-first navigation**

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = greeting, style = MaterialTheme.typography.headlineLarge)
        Text(text = dateStr, style = MaterialTheme.typography.bodyMedium)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeHeaderAction(...)
        HomeHeaderAction(...)
    }
}
```

Keep the greeting, date, and unread/AI actions, but tighten the spacing and remove any visual noise that competes with the calendar and entry list.

- [ ] **Step 2: Make the calendar and entry list read as one flow**

```kotlin
GlassCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 22.dp
) {
    CalendarView(...)
}
```

Place the calendar, selected-date header, and entry cards in a single vertical rhythm so the user always understands that the top controls lead into the day content.

- [ ] **Step 3: Add a compact summary strip for the selected day**

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
    SummaryPill(label = "记录", value = entryCount.toString())
    SummaryPill(label = "图片", value = imageCount.toString())
    SummaryPill(label = "标签", value = tagCount.toString())
}
```

This keeps the home screen informative without turning it into a dashboard.

- [ ] **Step 4: Preserve empty state and multi-select behavior**

Keep the existing multi-select reset logic, delete confirmation, favorites action, and empty-date state, but move them into a cleaner card stack so they remain visible without fighting the list layout.

- [ ] **Step 5: Add/adjust tests for the home state builders**

Run: `./gradlew :app:testDebugUnitTest --tests com.diary.app.ui.home.*`
Expected: Existing home tests pass and any layout/state refactor keeps selection and empty-state logic intact.

### Task 3: Redesign the tools screen and tool-level entry hierarchy

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/experimental/ExperimentalFeaturesScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Replace the current collapsible list with high-priority and grouped sections**

```kotlin
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    HeroToolCard(...)
    ToolClusterCard(title = "创造与记录", ...)
    ToolClusterCard(title = "回忆与探索", ...)
    ToolClusterCard(title = "AI 伙伴", ...)
    ToolClusterCard(title = "系统与实验", ...)
}
```

This keeps the four original meanings, but changes the layout from “accordion list” to “clear entry system.”

- [ ] **Step 2: Promote the most-used tools to a separate fast-access row**

```kotlin
LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    item { FastToolChip(...) }
    item { FastToolChip(...) }
    item { FastToolChip(...) }
    item { FastToolChip(...) }
}
```

Put stats, media library, countdown, and AI assistant up top so the tools page no longer hides the highest-frequency actions.

- [ ] **Step 3: Keep each tool row dense but readable**

```kotlin
@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String? = null,
    onClick: () -> Unit
)
```

Ensure there is one clear title, one clear description, and optional metadata, with no oversized block text.

- [ ] **Step 4: Preserve swipe behavior and navigation callbacks**

Keep `onSwipeToTimeline` and `onSwipeToTodo`, but make sure the new structure does not break the gesture recognizer or navigation call sites.

- [ ] **Step 5: Verify tool navigation targets still compile**

Run: `./gradlew :app:testDebugUnitTest --tests com.diary.app.*`
Expected: No broken references after the screen-level refactor.

### Task 4: Rebuild the HTML preview pages so the phone mockups do not overlap

**Files:**
- Modify: `design-previews/index.html`
- Modify: `design-previews/ui-preview-system.css`
- Modify: `design-previews/home-refresh-preview.html`
- Modify: `design-previews/tools-focused-preview.html`
- Modify: `design-previews/tools-function-suite.html`
- Modify: `design-previews/tools-lab-expanded.html`

- [ ] **Step 1: Change the phone grid from a fixed narrow column to a true scrollable comparison rail**

```css
.phone-grid {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(360px, 392px);
  gap: 28px;
  overflow-x: auto;
  padding-bottom: 16px;
}
```

The goal is to let several phones share the same theme choice while still showing different layout strategies.

- [ ] **Step 2: Keep one theme control bar for all phones**

```html
<div class="preview-controls">
  <button data-mode="light">Light</button>
  <button data-mode="dark">Dark</button>
  <button data-theme="fog">Fog Blue</button>
  <button data-theme="moss">Moss Green</button>
  <button data-theme="ocean">Ocean</button>
  <button data-theme="petal">Petal</button>
  <button data-theme="sand">Sand</button>
</div>
```

Each phone uses the same selected mode/theme so the comparison is about layout, not duplicated theme content.

- [ ] **Step 3: Redesign the home preview around real day content**

Show the header, calendar, selected-day summary, and entry list in one continuous phone screen so the preview explains the screen at a glance.

- [ ] **Step 4: Redesign the tools preview around grouped entry systems**

Show a hero summary, a fast-access row, and grouped clusters rather than a pure list of cards.

- [ ] **Step 5: Verify preview pages load locally without clipped or overlapping phone frames**

Open `design-previews/index.html` in the browser and confirm the grid scrolls horizontally, the phones stay separated, and the light/dark toggle updates all frames consistently.

### Task 5: Update release notes and version metadata

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `docs/README.md`
- Create: `release-notes-v2.64.51-experimental.md`

- [ ] **Step 1: Bump version metadata for the new release**

```kotlin
versionCode = 26451
versionName = "2.64.51-experimental"
```

- [ ] **Step 2: Write a short release note that names the visible changes**

```md
# DiaryApp v2.64.51-experimental

- Reworked home screen layout
- Rebuilt tools page navigation hierarchy
- Expanded theme presets
- Fixed design preview layout overlap
```

- [ ] **Step 3: Update docs/README with the new preview and release location**

Keep this concise and focused on where to find the updated design previews and release note.

- [ ] **Step 4: Run the full verification set**

Run:
`./gradlew :app:testDebugUnitTest`
`./gradlew :app:assembleExperimentalDebug`

Expected: tests pass and the experimental build assembles successfully.

