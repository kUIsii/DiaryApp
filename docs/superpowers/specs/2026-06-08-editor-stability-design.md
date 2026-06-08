# Editor Stability And Writing Focus Design

## Summary

This design updates the diary editor to prioritize stable writing over aggressive rich-text behavior. The current editor mixes Compose layout shifts, WebView viewport changes, and Quill auto-scrolling, which creates cursor jumps, hidden content, inconsistent list behavior, and unreliable image insertion.

The redesign keeps the current Compose + WebView + Quill architecture, but narrows responsibilities:

- Compose owns page structure, metadata layout, and editor mode visibility.
- The WebView editor owns cursor visibility, internal scrolling, media rendering, and selection restoration.
- The toolbar supports two explicit states: writing-focused mode and full editing mode.

## Goals

- Make writing feel stable when the keyboard opens, closes, or the user continues typing on long content.
- Prevent cursor jumps and preserve editing context when the user taps back into the editor or briefly leaves the page.
- Fix ordered-list continuation visibility and remove unexpected upward scrolling.
- Align bullet-list dots and checkbox markers with the ordered-list indentation baseline.
- Make inserted images render reliably instead of appearing as a broken placeholder or tiny icon.
- Rework metadata placement to match the requested layout:
  - Row 1: mood / weather / category
  - Row 2: location centered
- Add an explicit editor-visibility toggle so the user can write with minimal UI chrome while always keeping font-size controls available.

## Non-Goals

- Replacing Quill with a native Compose editor.
- Redesigning the note content model or stored Delta format.
- Adding brand-new rich text features beyond the requested mode toggle and stability fixes.
- Building persistent cross-session cursor restore for every draft revision. This pass only guarantees stable restore within the active editing session and predictable restore after common in-screen interactions.

## Current Problems

### Layout And Scroll Conflicts

The current screen changes bottom spacing from Compose while the WebView also resizes itself and tries to scroll the cursor into view. This creates multiple competing scroll systems:

- Compose updates bottom gap based on keyboard and toolbar state.
- The Web editor resizes itself with `visualViewport`.
- Quill selection and text-change callbacks trigger auto-scroll.

These overlapping behaviors create:

- ordered-list newline not scrolling down reliably
- upward jumps during continuous typing
- content seeming to disappear until the keyboard opens or the user manually scrolls
- unstable position after focus returns to the editor

### List Marker Misalignment

Bullet and checkbox lists use custom CSS overrides that do not match Quill’s ordered-list indentation and baseline positioning.

### Media Rendering Failure

Images are copied locally and inserted as `file://` URLs, but the display pipeline is brittle. Rendering can fail depending on how the editor loads the file resource and how Quill places the embed, leaving only a placeholder-like result.

### UI Density

The metadata chips, title, editor, keyboard, and toolbar compete for vertical space. The current toolbar always emphasizes editing tools even when the user only wants to write text.

## Proposed Approach

### 1. Single Scroll Owner

The editor WebView becomes the only owner of runtime writing scroll behavior.

Compose changes:

- Keep the page layout structurally stable while typing.
- Avoid repeated editor-height and bottom-gap oscillation while the keyboard is visible.
- Limit layout reactions to a small set of predictable state changes:
  - keyboard hidden
  - keyboard shown in writing-focused mode
  - keyboard shown in full editing mode

Web editor changes:

- Keep one internal cursor-visibility routine.
- Scroll only when the caret leaves a defined safe zone.
- Avoid “helpful” scrolling if the caret is already visible.
- Use the same scroll behavior for:
  - normal typing
  - ordered-list newline
  - bullet-list newline
  - checkbox toggling
  - media insertion
  - returning focus to the editor

### 2. Writing-Focused Vs Full Editing Modes

Add an explicit editor visibility toggle in the bottom tool area.

Modes:

- Writing-focused mode
  - hide format/list/insert/color groups
  - keep font-size controls visible
  - preserve a clean writing layout
- Full editing mode
  - show format/list/insert/color tools
  - keep font-size controls visible

Behavior:

- Default behavior favors writing-focused interaction when the user starts typing.
- If the user explicitly switches to full editing mode, that choice is preserved for the current editor session.
- Toggling modes must not reset content, selection, or scroll position.

### 3. Stable Editing Context Memory

Add a lightweight “editing context memory” layer instead of storing only a raw scroll number.

The editor tracks:

- last known selection index and length
- last known editor scroll top
- whether the restore was user-initiated or system-initiated
- a short-lived “restore lock” to prevent immediate counter-scroll after restoring

This context is used when:

- the user taps back into the editor
- the keyboard reopens
- a toolbar panel closes and returns focus
- media insertion finishes
- the screen temporarily loses focus and returns during the same session

Rules:

- Restore selection first, then restore visibility around that selection.
- Prefer the selection anchor over raw scrollTop if both are available.
- Do not force-scroll to the bottom after restore.
- Ignore stale restore attempts after major content changes that invalidate the old selection range.

### 4. Metadata Layout Update

Restructure metadata chips to match the requested order:

- First row:
  - mood
  - weather
  - category
- Second row:
  - location only

Layout rules:

- The location row is centered.
- Long location text truncates gracefully.
- The metadata area should occupy less visual weight while typing.
- Opening metadata panels should not destabilize the editor viewport.

### 5. Reliable Media Display

Keep local-file insertion, but harden the rendering chain.

Pipeline:

1. User selects an image.
2. App copies it into app-controlled media storage.
3. The WebView serves it through a reliable readable path.
4. The editor inserts a renderable image source.
5. The editor confirms visibility and keeps the caret in a predictable place after insertion.

Implementation direction:

- Prefer a WebView-readable app-local URI strategy that does not depend on fragile inline Base64 for large media.
- Ensure file access and resource interception are consistent for editor embeds.
- Add image load success/failure hooks in the page so the UI can react if the asset does not render.
- Compress oversized images before insertion to reduce memory pressure and layout instability.

## Component-Level Design

### Compose Screen (`EditorScreen.kt`)

Responsibilities after redesign:

- Owns high-level layout and editor mode state.
- Owns metadata arrangement and panel visibility.
- Owns media-picker launchers and handoff into the Web editor.
- Avoids micro-managing live cursor scroll.

Changes:

- Introduce a `editorMode` state with values similar to `WritingFocus` and `FullEditing`.
- Replace the current toolbar show/hide assumptions with explicit mode-driven rendering.
- Reduce bottom-gap recalculation churn to stable presets.
- Rework metadata chip rows to `mood/weather/category` + centered `location`.
- Ensure WebView focus restoration uses a dedicated restore path instead of generic `focusEditor()`.

### Toolbar (`EditorToolbar.kt`)

Responsibilities after redesign:

- Always show font-size controls.
- Provide a clear mode toggle for “editor tools visible” vs “writing focus”.
- Show advanced editing controls only in full editing mode.

Changes:

- Add a dedicated toggle action for editor visibility.
- Preserve font-size control in both modes.
- Avoid reflow patterns that unnecessarily steal height from the editor during typing.

### Web Editor (`editor.html`)

Responsibilities after redesign:

- Own caret-safe scrolling.
- Own selection/context memory.
- Own stable list styling.
- Own media embed rendering and post-insert viewport recovery.

Changes:

- Replace current scroll heuristics with one guarded visibility algorithm.
- Add context-memory helpers:
  - save selection context
  - restore selection context
  - guarded focus restore
- Normalize list CSS so bullet and checkbox markers align with ordered-list structure.
- Add image load bookkeeping and reliable post-insert caret placement.
- Minimize side effects in `selection-change` and `text-change`.

## Data And Event Flow

### Typing Flow

1. User types in Quill.
2. Quill emits content change.
3. Editor updates plain text and save signals.
4. Editor checks whether caret is outside the safe zone.
5. Editor scrolls only if needed.
6. Compose does not perform extra live scroll correction.

### Focus Return Flow

1. User taps back into the editor or closes a toolbar panel.
2. Compose requests a guarded focus restore.
3. Web editor restores the last valid selection context.
4. Web editor ensures the caret is visible without jumping to unrelated content.

### Image Insert Flow

1. Compose picker returns a URI.
2. App copies/compresses the image into local editor media storage.
3. Compose passes a safe image source into the editor.
4. Editor inserts the embed at the current selection.
5. Editor waits for image load completion.
6. Editor restores a valid caret position after the embed and scrolls minimally if needed.

## Error Handling

- If selection restoration points past the current document length, clamp to the nearest valid range.
- If media copy fails, do not insert a broken embed; surface a user-visible failure message instead.
- If an image resource cannot be read by WebView, fail fast and log the path/URI type for debugging.
- If image load never completes, keep the document usable and place the caret after the attempted insert without forcing erratic scrolling.
- If the editor loses format state during guarded restore, prefer stable selection/visibility over immediate toolbar synchronization.

## Testing Strategy

### Unit-Level / Local Logic

- Add or extend tests for editor utility logic where extraction is practical:
  - bottom-gap preset mapping
  - selection clamp rules
  - editor mode persistence for the current session

### Manual / Integration Verification

Primary scenarios:

- New diary, plain paragraph typing with keyboard open
- Edit existing diary with long content
- Ordered list: repeated newline continuation stays visible
- Bullet list marker alignment matches ordered list baseline
- Checkbox list marker alignment matches ordered list baseline
- Tapping checkbox toggles state without jumping viewport
- Insert image and confirm the image is visibly rendered
- Continue typing after image insertion without cursor jump
- Open and close advanced editor tools without losing current writing position
- Tap out and back into the editor and confirm selection/viewport stability
- Keyboard hide/show cycles do not make content appear missing
- Metadata row layout matches the requested arrangement

## Rollout Plan

1. Stabilize Web editor scroll ownership and selection restore behavior.
2. Add writing-focused/full-editing mode toggle and keep font controls persistent.
3. Update metadata row layout.
4. Harden media insertion and rendering.
5. Run targeted manual verification on the scenarios above.

## Risks And Mitigations

- Risk: Quill selection events may still fire in ways that re-trigger unwanted scroll.
  - Mitigation: add guarded restore flags and suppress redundant auto-scroll windows.
- Risk: WebView local media loading differs across Android versions.
  - Mitigation: use one explicit supported URI/resource strategy and verify through WebView interception.
- Risk: reducing Compose-side reactions could expose existing assumptions in toolbar behavior.
  - Mitigation: keep bottom spacing preset-driven and verify mode transitions separately from typing.

## Acceptance Criteria

- The editor no longer unexpectedly jumps upward during ordinary writing.
- Ordered lists continue onto a new line while keeping the caret visible.
- Content no longer appears to “disappear” behind keyboard or layout changes during common editing flows.
- Bullet and checkbox markers visually align with the list text in the same way as ordered lists.
- Inserted images render visibly in the document immediately after insertion.
- The user can switch between writing-focused and full editing modes without losing content, cursor position, or scroll stability.
- Font-size controls remain available in both modes.
- Metadata layout matches the requested two-row structure with centered location.
- Tapping back into the editor or returning from small interruptions restores the current writing context predictably.
