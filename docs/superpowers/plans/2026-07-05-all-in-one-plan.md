# All-in-One Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Performance optimization + timeline expand + todo cleanup + profile account section + release

**Architecture:** Five independent subsystems that can be worked on in any order, but build verification must happen at the end.

**Tech Stack:** Jetpack Compose, Material3, Android, Electron (desktop)

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `app/build.gradle.kts` | Modify | Compose BOM version bump + versionCode/versionName bump |
| `app/.../components/GradientBackground.kt` | Modify | Pause infinite animation when not visible |
| `app/.../navigation/DiaryNavHost.kt` | Modify | Simplify nav transition animations + add ChangePin route |
| `app/.../timeline/TimelineScreen.kt` | Modify | Remove month collapsing, always expand all |
| `app/.../todo/TodoScreen.kt` | Modify | Remove CloudSyncManager, pushToCloud, auth dialog, sync buttons |
| `app/.../profile/ProfileScreen.kt` | Modify | Add "账号" collapsible section with change PIN + logout |
| `app/.../profile/ChangePinScreen.kt` | Create | Change PIN UI (old PIN → new PIN → confirm) |
| `app/.../data/auth/AuthManager.kt` | Modify | Add `changePin(oldPin, newPin)` method |
| `desktop/src/renderer/renderer.js` | Modify | Add account section to desktop profile |
| (APK output) | Build | `app/.../debug/app-experimental-debug.apk` |

---

## Task 1: Performance — Compose BOM upgrade

**Files:** `app/build.gradle.kts`

- [ ] **Step 1: Read current build.gradle.kts**

Run: `Read app/build.gradle.kts` (already available in context)

Current versions:
```kotlin
compose-bom = "2023.10.01"
kotlinCompilerExtension = "1.5.5"
```

- [ ] **Step 2: Update BOM version**

Set BOM to `2025.01.01` (newest stable as of early 2025, safe upgrade path for existing APIs):
```kotlin
compose-bom = "2025.01.01"
```
For Kotlin Compiler Extension, the current Kotlin version needs to match. Check the Kotlin version in `build.gradle.kts` (project-level).

- [ ] **Step 3: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Revert if build fails**

If the BOM upgrade causes compilation errors due to API changes, revert to `2024.06.00` and retry.

---

## Task 2: Performance — GradientBackground infinite animation

**Files:** `app/src/main/java/com/diary/app/ui/components/GradientBackground.kt`

**Problem:** Line 305 creates an `infiniteRepeatable` animation that runs forever on every screen using GradientBackground, causing constant recomposition.

**Fix:** Use `rememberInfiniteTransition` but check page visibility via `LifecycleResumeEffect`.

- [ ] **Step 1: Add imports**

Add to imports:
```kotlin
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
```

- [ ] **Step 2: Wrap infinite animation with lifecycle-aware pause**

Replace lines 305-314:
```kotlin
    val transition = rememberInfiniteTransition()
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(driftDuration(mode.category), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgDrift"
    )
```

With:
```kotlin
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAtLeastResumed by remember { mutableStateOf(true) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.observeAsState(Lifecycle.Event.ON_RESUME)
    }
    // Simplified: just slow down the animation to reduce redraws
    val transition = rememberInfiniteTransition()
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(driftDuration(mode.category) * 3, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgDrift"
    )
```

Actually simpler approach — just make the animation 3x slower so it's less expensive (fewer color recalculations per second). The visual difference is negligible at 30s vs 90s cycles.

Change:
```kotlin
driftDuration(theme)  // 25-40 seconds
```
→ multiply by 3:
```kotlin
driftDuration(theme) * 3  // 75-120 seconds
```

- [ ] **Step 3: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 3: Performance — Calendar swipe optimization

**Files:** `app/src/main/java/com/diary/app/ui/home/CalendarView.kt`

**Problem:** HorizontalPager renders MonthView which draws every day cell with mood gradients, weather icons, and entry counts. This can lag on swipe.

**Fix:** Reduce `beyondBoundsPageCount` from 1 to 0, and cache day info calculations.

- [ ] **Step 1: Read CalendarView.kt lines 326-332**

Find the HorizontalPager section.

- [ ] **Step 2: Optimize page rendering**

Change:
```kotlin
beyondBoundsPageCount = 1
```
to:
```kotlin
beyondBoundsPageCount = 0
```

Also optimize the MonthView to use `remember` for computing each day cell's colors:

Add a `derivedStateOf` wrapper around `dayInfoMap` lookups if not already present. This is a reading pass — the agent should check if month/day views already use `remember` for their computation.

- [ ] **Step 3: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 4: Performance — Nav transition simplification

**Files:** `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`

**Problem:** Bottom tab transitions use slide + fade (280ms) which can feel laggy.

**Fix:** Change to simple fade-only transitions at 150ms.

- [ ] **Step 1: Read DiaryNavHost.kt nav transitions (lines ~280-305)**

Find the `enterTransition`, `exitTransition`, `popEnterTransition`, `popExitTransition` blocks.

- [ ] **Step 2: Simplify bottom tab transitions**

Replace the bottom tab transitions with fade-only:
```kotlin
enterTransition = {
    fadeIn(animationSpec = tween(150))
}
exitTransition = {
    fadeOut(animationSpec = tween(100))
}
popEnterTransition = {
    fadeIn(animationSpec = tween(100))
}
popExitTransition = {
    fadeOut(animationSpec = tween(100))
}
```

Also simplify sub-page transitions (lines ~138-156): reduce tween from 280ms to 150ms.

- [ ] **Step 3: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 5: Timeline — Default expand all months

**Files:** `app/src/main/java/com/diary/app/ui/timeline/TimelineScreen.kt`

**Problem:** Lines 175-179 only set the current month to expanded by default. Other months show as collapsed headers.

**Fix:** Always expand all months — skip all collapsing logic entirely.

- [ ] **Step 1: Read TimelineScreen.kt lines 165-213 (month grouping + timelineItems build)**

- [ ] **Step 2: Remove collapsing logic**

Replace lines 175-213 with always-expanded logic:

```kotlin
    // Build flat list of items for LazyColumn with stable keys (always expanded)
    data class TimelineItem(val key: String, val month: YearMonth? = null, val date: LocalDate? = null, val entry: DiaryPreview? = null)

    val timelineItems = remember(entries) {
        buildList<TimelineItem> {
            monthGroups.forEach { (month, monthEntries) ->
                add(TimelineItem(key = "month_$month", month = month))
                val monthDateGroups = monthEntries.groupBy { entry ->
                    Instant.ofEpochMilli(entry.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.toSortedMap(compareByDescending { it })
                monthDateGroups.forEach { (date, dayEntries) ->
                    add(TimelineItem(key = "day_$date", date = date))
                    dayEntries.forEachIndexed { _, entry ->
                        add(TimelineItem(
                            key = "entry_${entry.id}",
                            entry = entry,
                            date = date
                        ))
                    }
                }
            }
        }
    }
```

Remove `expandedMonths` state variable, `import mutableStateMapOf`.

- [ ] **Step 3: Update the timeline rendering**

Remove the `"collapsed_month"` branch in the `when` block (lines 366-373).
Remove the collapse/expand buttons from `"expanded_month"` — change it to just show month header without collapse button.
Also remove unused parameters like `onCollapse`, `onClick` in the collapsed month header composable calls.

The `"expanded_month"` case should just render the month header label, not a clickable collapse button.

- [ ] **Step 4: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 6: Todo — Cleanup manual sync

**Files:** `app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt`

**Delete these items:**
1. Import `com.diary.app.data.sync.CloudSyncManager` (line 75)
2. `cloudSyncManager = remember { CloudSyncManager(todoContext) }` (line 135)
3. State variables: `syncStatus`, `showAuthDialog`, `phoneInput`, `pinInput`, `isSyncing` (lines 138-142)
4. Whole `pushToCloud()` function (lines 144-160)
5. Whole auth dialog block (lines 282-337)
6. Parameters passed to `TodoAssistantPanel`: `syncStatus`, `isSyncing`, `onPushSync`, `onCopySyncPayload` (lines 483-493)
7. The "推送同步" and "复制" buttons in `TodoAssistantPanel` (lines 1106-1117)
8. Unused imports: `LocalClipboardManager`, `AnnotatedString`, `Gson` related

- [ ] **Step 1: Remove CloudSyncManager import and usage**

Delete line 75: `import com.diary.app.data.sync.CloudSyncManager`
Delete line 135: `val cloudSyncManager = remember { CloudSyncManager(todoContext) }`
Delete lines 138-142: `syncStatus`, `showAuthDialog`, `phoneInput`, `pinInput`, `isSyncing`
Delete lines 144-160: `pushToCloud()` function
Delete lines 282-337: `if (showAuthDialog) { ... }` block

- [ ] **Step 2: Update TodoAssistantPanel call**

Change lines 483-493 from:
```kotlin
onPushSync = { pushToCloud() },
onCopySyncPayload = { ... },
syncStatus = syncStatus,
isSyncing = isSyncing,
```
Remove these parameters entirely. Also remove `import com.diary.app.ui.platform.LocalClipboardManager` and `import androidx.compose.ui.text.AnnotatedString`.

- [ ] **Step 3: Remove sync buttons from TodoAssistantPanel**

In `TodoAssistantPanel` (lines 1106-1117), delete the Row containing push sync and copy buttons:
```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    TextButton(onClick = onPushSync, enabled = !isSyncing) {
        ...
    }
    TextButton(onClick = onCopySyncPayload) {
        ...
    }
}
```

Also remove `isSyncing`, `onPushSync`, `onCopySyncPayload`, `syncStatus` parameters from the `TodoAssistantPanel` composable signature.

- [ ] **Step 4: Remove unused imports**

Remove:
```kotlin
import com.diary.app.data.sync.CloudSyncManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
```

- [ ] **Step 5: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 7: AuthManager — Add changePin method

**Files:** `app/src/main/java/com/diary/app/data/auth/AuthManager.kt`

- [ ] **Step 1: Add changePin method**

Add after the `login` method (before the private helpers):

```kotlin
fun changePin(oldPin: String, newPin: String): Result<Unit> {
    val phone = savedPhone ?: return Result.failure(Exception("未登录"))
    if (newPin.length < 4) {
        return Result.failure(Exception("新 PIN 至少4位"))
    }
    val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return Result.failure(Exception("未设置密码"))
    val oldHash = hashPin(oldPin, phone)
    if (oldHash != storedHash) {
        return Result.failure(Exception("旧 PIN 错误"))
    }
    val newHash = hashPin(newPin, phone)
    prefs.edit().putString(KEY_PIN_HASH, newHash).apply()
    return Result.success(Unit)
}
```

Note: `hashPin` is already a private method in AuthManager.

- [ ] **Step 2: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 8: ChangePinScreen — New page

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/profile/ChangePinScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt` (add route)

- [ ] **Step 1: Create ChangePinScreen.kt**

```kotlin
package com.diary.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.auth.AuthManager
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(
    authManager: AuthManager,
    onNavigateBack: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("修改登录密码") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it; errorMessage = null; successMessage = null },
                    label = { Text("旧 PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it; errorMessage = null; successMessage = null },
                    label = { Text("新 PIN (至少4位)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it; errorMessage = null; successMessage = null },
                    label = { Text("确认新 PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (newPin != confirmPin) {
                            errorMessage = "两次输入的 PIN 不一致"
                        } else if (newPin.length < 4) {
                            errorMessage = "新 PIN 至少4位"
                        } else {
                            authManager.changePin(oldPin, newPin).fold(
                                onSuccess = { successMessage = "密码修改成功" },
                                onFailure = { errorMessage = it.message }
                            )
                        }
                    },
                    enabled = oldPin.isNotBlank() && newPin.length >= 4 && confirmPin.length >= 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("确认修改", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Add route to DiaryNavHost.kt**

Find the `Screen` sealed class and add:
```kotlin
data object ChangePin : Screen("change_pin", "修改登录密码", Icons.Default.Lock)
```

Wait — Screen uses objects not data objects. Check existing pattern (lines 158-215):
```kotlin
object Home : Screen("home", "首页", Icons.Default.Home)
```
So add:
```kotlin
object ChangePin : Screen("change_pin", "修改登录密码", Icons.Default.Lock)
```

Then add the composable route in the NavHost section (before or after the profile composable):
```kotlin
composable(
    route = Screen.ChangePin.route,
    enterTransition = { fadeIn(tween(150)) },
    exitTransition = { fadeOut(tween(100)) }
) {
    ChangePinScreen(
        authManager = authManager,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

Note: The `authManager` variable is likely already available in the `DiaryNavHost` composable. Check if it's being created or passed in. If not, create it from context:
```kotlin
val context = LocalContext.current
val authManager = remember { AuthManager(context) }
```

- [ ] **Step 3: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 9: ProfileScreen — Add account section

**Files:** `app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt`

**Goal:** Add a collapsible "账号" section at the top of ProfileScreen, with:
1. "修改登录密码" row → navigates to ChangePinScreen
2. "退出登录" row → confirmation dialog → logs out → returns to login screen

- [ ] **Step 1: Read ProfileScreen.kt structure**

Read the file fully to understand:
- How collapsible sections work (pattern: `var isXxxExpanded by remember`, `AnimatedVisibility`, etc.)
- How navigation callbacks are passed (`onNavigateTo*`)
- Current imports and patterns

- [ ] **Step 2: Add auth state + callbacks**

Add to ProfileScreen parameter list:
```kotlin
authManager: AuthManager? = null,
onNavigateToChangePin: (() -> Unit)? = null,
onLogout: (() -> Unit)? = null,
```

- [ ] **Step 3: Add account section**

Insert after the header section and before the existing Appearance section:

```kotlin
// Account section
var isAccountExpanded by remember { mutableStateOf(true) }
// ... collapsible section with "修改登录密码" and "退出登录" rows
// Use the same pattern as existing sections (ClickableSettingRow)
```

For logout, add a confirmation dialog:
```kotlin
var showLogoutDialog by remember { mutableStateOf(false) }
if (showLogoutDialog) {
    AlertDialog(
        onDismissRequest = { showLogoutDialog = false },
        title = { Text("退出登录") },
        text = { Text("确定退出登录吗？退出后需要重新输入手机号和 PIN 才能使用。") },
        confirmButton = {
            TextButton(onClick = {
                authManager?.logout()
                showLogoutDialog = false
                onLogout?.invoke()
            }) { Text("确定退出") }
        },
        dismissButton = {
            TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
        }
    )
}
```

- [ ] **Step 4: Update DiaryNavHost.kt profile composable**

Pass `authManager`, `onNavigateToChangePin`, and `onLogout` to the ProfileScreen composable.

For `onLogout`, navigate to login screen (clear the back stack):
```kotlin
onLogout = {
    navController.navigate(Screen.Home.route) {
        popUpTo(0) { inclusive = true }
    }
    // Trigger MainActivity to show LoginScreen again
}
```

Actually, since login is handled by `MainActivity.kt` checking auth state, the logout just needs to clear auth data and the app will naturally redirect to login on next recomposition. But for immediate feedback, we should signal the activity to refresh.

The simplest approach: after `authManager.logout()`, navigate to home and let the auth check in MainActivity handle the redirect. But MainActivity's auth check is `LaunchedEffect` based — it won't re-trigger automatically.

Better approach: use a shared auth state or just navigate to a special "logout" route that MainActivity watches.

Simplest for now: just call `authManager.logout()` and `onNavigateHome` which navigates to home. The user will see the home screen briefly before auth check kicks in on next resume.

Actually, let me check how MainActivity handles this.

- [ ] **Step 5: Build to verify**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

---

## Task 10: Desktop — Add account section

**Files:** `desktop/src/renderer/renderer.js`

- [ ] **Step 1: Read desktop renderer.js profile section**

Find the profile/settings page and add "修改登录密码" and "退出登录" buttons.

For logout on desktop:
```javascript
function logoutAccount() {
    if (confirm('确定退出登录吗？')) {
        ipcRenderer.invoke('desktop:sync-logout')
        // Refresh UI
        renderProfile()
    }
}
```

- [ ] **Step 2: Build desktop to verify**

Run: `cd desktop && npm run build` (or however the desktop build works)
Expected: BUILD SUCCESSFUL

---

## Task 11: Bump version & build release APK

- [ ] **Step 1: Update version in build.gradle.kts**

```kotlin
versionCode = 27814
versionName = "2.78.13-experimental"
```

- [ ] **Step 2: Build APK**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify APK exists**

Check: `app/build/outputs/apk/experimental/debug/app-experimental-debug.apk` exists

- [ ] **Step 4: Commit all changes**

```bash
git add -A
git commit -m "feat: performance optimization, timeline expand, todo cleanup, profile account section"
```

- [ ] **Step 5: Push and create Release**

```bash
git push origin experiment/v2-redesign
gh release create v2.78.13-experimental --title "v2.78.13-experimental" --notes "性能优化 + 时间线默认展开 + 待办清理 + 账号管理" "app/build/outputs/apk/experimental/debug/app-experimental-debug.apk" --target experiment/v2-redesign
```

Expected: Release URL like `https://github.com/kUIsii/DiaryApp/releases/tag/v2.78.13-experimental`
