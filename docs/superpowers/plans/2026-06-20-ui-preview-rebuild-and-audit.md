# UI Preview Rebuild And Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the broken UI preview system into a consistent, high-coverage comparison set for homepage, tools, and tool-function pages, while also auditing and fixing obvious compile and UI integrity issues in the current app code.

**Architecture:** Keep one shared preview foundation in `design-previews` with a single CSS and theme-control script, then rebuild corrupted preview pages on top of that foundation with a mixed matrix: two visual directions and multiple layout routes per page family. In parallel, run targeted Android compile verification on the actual variants and apply only safe, minimal fixes for broken text, compile errors, or obviously wrong UI semantics.

**Tech Stack:** Static HTML/CSS/JS, Android Jetpack Compose, Gradle, Kotlin, PowerShell, lightweight verification scripts

---

### Task 1: Rebuild The Preview Foundation

**Files:**
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\ui-preview-system.css`
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\preview-controls.js`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\preview-controls.js`

- [ ] **Step 1: Define the missing shared preview primitives**

Add or normalize shared styles for:
- floating action button (`.fab`)
- layout rails / control groups
- screen annotations / comparison headings
- reusable metric, list, and empty-state blocks
- both mobile-friendly and desktop comparison layouts

The goal is that homepage, tools, and function pages can all render from one base stylesheet without page-local patching for standard chrome.

- [ ] **Step 2: Unify theme application in the shared script**

Ensure `preview-controls.js` becomes the only theme/mode controller for rebuilt pages. It should:
- support `fog/moss/ocean/petal/sand/clay/ink`
- apply light/dark variables to both `:root` and every `.screen`
- synchronize active button states
- avoid page-specific branching except narrow backward-safe normalization like `lake -> ocean`

- [ ] **Step 3: Verify the shared foundation by inspection**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\preview-controls.js'
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\ui-preview-system.css'
```

Expected:
- one unified theme controller
- shared `.fab` support exists
- no duplicated page-specific theme switching logic remains necessary for rebuilt pages

### Task 2: Rebuild The Homepage Preview Matrix

**Files:**
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\home-refresh-preview.html`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\home-refresh-preview.html`

- [ ] **Step 1: Replace the corrupted homepage preview with fresh UTF-8 content**

Rebuild the file from scratch rather than patching damaged text. The rebuilt page must:
- keep the real product skeleton: date summary, optional AI insight, calendar selection, selected-date header, day-entry list, empty state, FAB
- include multiple homepage layout routes
- keep copy entirely readable Chinese

- [ ] **Step 2: Implement the mixed comparison matrix**

Create 4-6 homepage options across two visual directions:
- direction A: close to current app temperament
- direction B: stronger hierarchy and finish

Within those directions, vary layout choices such as:
- date header density
- AI insight prominence
- calendar block weight
- list card density
- multi-select / empty-state expression

- [ ] **Step 3: Remove inline duplicate theme switching**

The homepage preview must rely on `preview-controls.js`, not a custom embedded mode/theme controller.

- [ ] **Step 4: Verify the rebuilt homepage preview**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\home-refresh-preview.html'
```

Expected:
- no `�`
- no garbled Chinese such as `棣栭〉`, `娴呰壊`
- shared script referenced once

### Task 3: Expand Tools And Function Preview Matrices

**Files:**
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-focused-preview.html`
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-function-suite.html`
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\index.html`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-focused-preview.html`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-function-suite.html`

- [ ] **Step 1: Expand tools page options under one clean system**

Preserve the real IA:
- quick access
- grouped directory
- `创作与整理 / 回忆与探索 / AI 与系统` style grouping

Produce 4-6 options that vary:
- quick access vs directory emphasis
- density
- recent-task or high-frequency surfacing
- low-frequency / experimental containment

- [ ] **Step 2: Rebuild the corrupted tool-function page**

Replace the damaged file with a fresh UTF-8 comparison suite that covers at least:
- statistics
- media library
- tag management
- countdown
- diary map
- AI assistant

The emphasis should stay on real page layout rather than decorative wrapping.

- [ ] **Step 3: Refresh the preview index**

Update `index.html` so it accurately describes the new comparison matrix and links to the rebuilt pages as the main entry point.

- [ ] **Step 4: Verify tools and function preview pages**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-focused-preview.html'
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-function-suite.html'
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\index.html'
```

Expected:
- readable Chinese throughout
- no local duplicate theme scripts in rebuilt pages
- clear mixed proposal framing for user selection

### Task 4: Audit And Fix App Compile / Text Integrity Issues

**Files:**
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\home\HomeScreen.kt`
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\home\CalendarView.kt`
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\tools\ToolsScreen.kt`
- Modify: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\navigation\DiaryNavHost.kt`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\home\HomeScreen.kt`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\home\CalendarView.kt`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\tools\ToolsScreen.kt`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\app\src\main\java\com\diary\app\ui\navigation\DiaryNavHost.kt`

- [ ] **Step 1: Identify the actual debug compile tasks**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
.\gradlew.bat :app:tasks --all --console=plain
```

Expected:
- find the real Kotlin compile tasks for `stableDebug` and `experimentalDebug`

- [ ] **Step 2: Compile the first real variant and capture failures**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
.\gradlew.bat :app:compileStableDebugKotlin --console=plain
```

Expected:
- either exit 0
- or concrete compile errors with file/line evidence

- [ ] **Step 3: Compile the second real variant and capture failures**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
.\gradlew.bat :app:compileExperimentalDebugKotlin --console=plain
```

Expected:
- either exit 0
- or concrete compile errors with file/line evidence

- [ ] **Step 4: Apply only safe minimal fixes**

If compile or UI integrity problems are directly caused by current broken copy / malformed strings / obviously wrong semantics, fix only the necessary files. Priority fixes:
- garbled Chinese strings in core navigation and page UI
- malformed Kotlin string literals
- obvious semantic icon mismatch where trivial to correct
- compile blockers in touched files

- [ ] **Step 5: Re-run the affected compiles**

Run the same variant compile commands again and record the fresh result.

### Task 5: Final Verification

**Files:**
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\index.html`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\home-refresh-preview.html`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-focused-preview.html`
- Test: `C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-function-suite.html`

- [ ] **Step 1: Verify preview files are readable and structurally consistent**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\index.html'
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\home-refresh-preview.html'
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-focused-preview.html'
Get-Content -Raw -Encoding UTF8 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\tools-function-suite.html'
```

Expected:
- readable Chinese
- shared CSS / JS usage
- multiple routes available for comparison

- [ ] **Step 2: Verify no corruption markers remain in rebuilt preview pages**

Run:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Select-String -Path 'C:\Users\陈仕杰\Desktop\DiaryApp\design-previews\*.html' -Pattern '�|棣栭〉|娴呰壊|宸ュ叿鍔熻兘椤甸瑙'
```

Expected:
- rebuilt key pages should not match those corruption markers

- [ ] **Step 3: Summarize user-facing outcomes**

Prepare a concise summary covering:
- rebuilt preview matrix
- where to open it
- which app-side issues were fixed
- what remains for the next filtering round
