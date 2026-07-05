# LoginScreen UI Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade LoginScreen to match 7 theme families, add micro-interactions (Design Spells), dynamic gradient background (Shader Gradient), and polished card layout (Unicorn UI).

**Architecture:** Single-file change to `LoginScreen.kt`. Extract per-theme login specs into a new `LoginThemeSpec.kt` for clean separation. Leverage existing `GradientBackground`, `GlassCard`, and `ThemeFamily` infrastructure.

**Tech Stack:** Jetpack Compose, Material3, Compose Animation

---

## File Structure

- **Modify:** `app/src/main/java/com/diary/app/ui/login/LoginScreen.kt` — Full UI rewrite with animations + theme adaptation
- **Create:** `app/src/main/java/com/diary/app/ui/login/LoginThemeSpec.kt` — Per-theme color/animation specs for login

### Task 1: Create LoginThemeSpec

**Create:** `app/src/main/java/com/diary/app/ui/login/LoginThemeSpec.kt`

Per-theme login UI constants: input field accent color, button gradient colors, animation specs, decorative element type.

```kotlin
package com.diary.app.ui.login

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import com.diary.app.ui.theme.ThemeFamily

data class LoginThemeSpec(
    val inputAccent: Color,
    val inputGlow: Color,
    val buttonStart: Color,
    val buttonEnd: Color,
    val buttonPressedScale: Float,
    val buttonAnimSpec: Any, // SpringSpec or TweenSpec
    val decorElement: DecorElement
)

enum class DecorElement {
    NONE, HORIZONTAL_LINES, DOTS, WAVES, ELLIPSES, GRAIN, CROSS_HATCH, GRID_DOTS
}

fun loginThemeSpec(family: ThemeFamily): LoginThemeSpec = when (family) {
    ThemeFamily.BLUE -> LoginThemeSpec(
        inputAccent = Color(0xFF60A5FA),
        inputGlow = Color(0xFF3B82F6).copy(alpha = 0.15f),
        buttonStart = Color(0xFF3B82F6),
        buttonEnd = Color(0xFF1D4ED8),
        buttonPressedScale = 0.97f,
        buttonAnimSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
        decorElement = DecorElement.HORIZONTAL_LINES
    )
    ThemeFamily.GREEN -> LoginThemeSpec(
        inputAccent = Color(0xFF4ADE80),
        inputGlow = Color(0xFF22C55E).copy(alpha = 0.15f),
        buttonStart = Color(0xFF22C55E),
        buttonEnd = Color(0xFF15803D),
        buttonPressedScale = 0.96f,
        buttonAnimSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 250f),
        decorElement = DecorElement.DOTS
    )
    ThemeFamily.CYAN -> LoginThemeSpec(
        inputAccent = Color(0xFF22D3EE),
        inputGlow = Color(0xFF06B6D4).copy(alpha = 0.15f),
        buttonStart = Color(0xFF06B6D4),
        buttonEnd = Color(0xFF0891B2),
        buttonPressedScale = 0.97f,
        buttonAnimSpec = tween(durationMillis = 150),
        decorElement = DecorElement.WAVES
    )
    ThemeFamily.ROSE -> LoginThemeSpec(
        inputAccent = Color(0xFFFB7185),
        inputGlow = Color(0xFFF43F5E).copy(alpha = 0.15f),
        buttonStart = Color(0xFFF43F5E),
        buttonEnd = Color(0xFFBE123C),
        buttonPressedScale = 0.96f,
        buttonAnimSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 280f),
        decorElement = DecorElement.ELLIPSES
    )
    ThemeFamily.AMBER -> LoginThemeSpec(
        inputAccent = Color(0xFFFBBF24),
        inputGlow = Color(0xFFF59E0B).copy(alpha = 0.15f),
        buttonStart = Color(0xFFF59E0B),
        buttonEnd = Color(0xFFB45309),
        buttonPressedScale = 0.97f,
        buttonAnimSpec = tween(durationMillis = 200),
        decorElement = DecorElement.GRAIN
    )
    ThemeFamily.CLAY -> LoginThemeSpec(
        inputAccent = Color(0xFFA8A29E),
        inputGlow = Color(0xFF78716C).copy(alpha = 0.15f),
        buttonStart = Color(0xFF78716C),
        buttonEnd = Color(0xFF44403C),
        buttonPressedScale = 0.97f,
        buttonAnimSpec = tween(durationMillis = 180),
        decorElement = DecorElement.CROSS_HATCH
    )
    ThemeFamily.INK -> LoginThemeSpec(
        inputAccent = Color(0xFF94A3B8),
        inputGlow = Color(0xFF64748B).copy(alpha = 0.15f),
        buttonStart = Color(0xFF64748B),
        buttonEnd = Color(0xFF334155),
        buttonPressedScale = 0.98f,
        buttonAnimSpec = tween(durationMillis = 120),
        decorElement = DecorElement.GRID_DOTS
    )
}
```

### Task 2: Rewrite LoginScreen

**Modify:** `app/src/main/java/com/diary/app/ui/login/LoginScreen.kt`

Full rewrite with:
1. **Entrance animation** — title, input fields, button stagger in (slideUp + fadeIn, 100ms apart)
2. **Per-theme colors** — input accent, button gradient, glow via `loginThemeSpec()`
3. **Button press animation** — spring scale down on press using `animateFloatAsState`
4. **Mode switch animation** — register/login text crossfade
5. **Input glow** — focused input gets a subtle colored shadow/glow overlay
6. **Decorative element** — subtle background pattern based on theme (using same Canvas drawing approach as GradientBackground)

```kotlin
package com.diary.app.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.auth.AuthManager
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.ThemeFamily
import com.diary.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoggedIn: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf(authManager.savedPhone ?: "") }
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRegisterMode by remember { mutableStateOf(!authManager.isRegistered) }
    var showPin by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }

    // Staggered entrance animation
    LaunchedEffect(Unit) {
        delay(100)
        entered = true
    }

    val titleAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(400)
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 24f,
        animationSpec = tween(400)
    )

    val inputAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(400, delayMillis = 100)
    )
    val inputOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 20f,
        animationSpec = tween(400, delayMillis = 100)
    )

    val buttonAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(400, delayMillis = 200)
    )
    val buttonOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 16f,
        animationSpec = tween(400, delayMillis = 200)
    )

    val family = com.diary.app.ui.theme.currentThemeFamily()
    val spec = loginThemeSpec(family)

    GradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    // Theme decorative pattern (subtle)
                    val w = size.width
                    val h = size.height
                    val paint = android.graphics.Paint().apply {
                        color = spec.inputAccent.copy(alpha = 0.03f).hashCode()
                        strokeWidth = 1f
                        style = android.graphics.Paint.Style.STROKE
                    }
                    onDrawBehind {
                        when (spec.decorElement) {
                            DecorElement.HORIZONTAL_LINES -> {
                                for (y in 0 until h.toInt() step 60) {
                                    drawLine(spec.inputAccent.copy(alpha = 0.02f), Offset(0f, y.toFloat()), Offset(w, y.toFloat()))
                                }
                            }
                            DecorElement.DOTS -> {
                                for (x in 0 until w.toInt() step 40) {
                                    for (y in 0 until h.toInt() step 40) {
                                        drawCircle(spec.inputAccent.copy(alpha = 0.015f), 1.5f, Offset(x.toFloat(), y.toFloat()))
                                    }
                                }
                            }
                            DecorElement.WAVES -> {
                                val wavePath = Path()
                                for (wave in 0..3) {
                                    wavePath.reset()
                                    val baseY = h * 0.15f + wave * h * 0.25f
                                    wavePath.moveTo(0f, baseY)
                                    for (x in 0 until w.toInt() step 10) {
                                        wavePath.lineTo(x.toFloat(), baseY + kotlin.math.sin(x.toDouble() * 0.02 + wave).toFloat() * 20f)
                                    }
                                    drawPath(wavePath, spec.inputAccent.copy(alpha = 0.015f), style = Stroke(1f))
                                }
                            }
                            DecorElement.ELLIPSES -> {
                                for (i in 0..4) {
                                    drawOval(
                                        spec.inputAccent.copy(alpha = 0.012f),
                                        topLeft = Offset(i * w / 5f, h * 0.2f),
                                        size = Size(w * 0.15f, h * 0.08f)
                                    )
                                }
                            }
                            DecorElement.GRAIN -> {
                                // noise dots at random-ish positions
                                val seed = 42
                                for (i in 0 until 80) {
                                    val px = ((i * 137 + seed) % w.toInt()).toFloat()
                                    val py = ((i * 251 + seed) % h.toInt()).toFloat()
                                    drawCircle(spec.inputAccent.copy(alpha = 0.01f), 1f, Offset(px, py))
                                }
                            }
                            DecorElement.CROSS_HATCH -> {
                                for (i in 0 until w.toInt() step 40) {
                                    drawLine(spec.inputAccent.copy(alpha = 0.015f), Offset(i.toFloat(), 0f), Offset(i.toFloat() + 20f, h), 0.5f)
                                }
                                for (i in 0 until w.toInt() step 40) {
                                    drawLine(spec.inputAccent.copy(alpha = 0.015f), Offset(i.toFloat() + 20f, 0f), Offset(i.toFloat(), h), 0.5f)
                                }
                            }
                            DecorElement.GRID_DOTS -> {
                                for (x in 0 until w.toInt() step 36) {
                                    for (y in 0 until h.toInt() step 36) {
                                        drawCircle(spec.inputAccent.copy(alpha = 0.018f), 1.2f, Offset(x.toFloat(), y.toFloat()))
                                    }
                                }
                            }
                            DecorElement.NONE -> {}
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title with entrance animation
                Column(
                    modifier = Modifier
                        .alpha(titleAlpha)
                        .offset(y = titleOffset.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DiaryApp",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "你的私人日记空间",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Input fields with entrance animation
                Column(
                    modifier = Modifier
                        .alpha(inputAlpha)
                        .offset(y = inputOffset.dp)
                ) {
                    PhoneInput(
                        value = phone,
                        onValueChange = { phone = it; errorMessage = null },
                        accentColor = spec.inputAccent,
                        glowColor = spec.inputGlow
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PinInput(
                        value = pin,
                        onValueChange = { pin = it; errorMessage = null },
                        showPin = showPin,
                        onToggleVisibility = { showPin = !showPin },
                        accentColor = spec.inputAccent,
                        glowColor = spec.inputGlow,
                        onDone = {
                            if (phone.isNotBlank() && pin.length >= 4) {
                                scope.launch { doLogin(authManager, phone, pin, isRegisterMode, { isLoading = it; errorMessage = null }, { onLoggedIn() }, { errorMessage = it }) }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Button with entrance animation
                Column(
                    modifier = Modifier
                        .alpha(buttonAlpha)
                        .offset(y = buttonOffset.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoginButton(
                        text = if (isRegisterMode) "注册并登录" else "登录",
                        enabled = phone.isNotBlank() && pin.length >= 4 && !isLoading,
                        isLoading = isLoading,
                        spec = spec,
                        onClick = {
                            scope.launch { doLogin(authManager, phone, pin, isRegisterMode, { isLoading = it; errorMessage = null }, { onLoggedIn() }, { errorMessage = it }) }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { isRegisterMode = !isRegisterMode; errorMessage = null }) {
                        Crossfade(
                            targetState = isRegisterMode,
                            animationSpec = tween(200)
                        ) { registerMode ->
                            Text(
                                text = if (registerMode) "已有账号？点击登录" else "没有账号？点击注册",
                                fontSize = 13.sp,
                                color = spec.inputAccent
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneInput(
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    glowColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label = "glowAlpha"
    )

    Box {
        if (isFocused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = (-4).dp, vertical = (-4).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(glowColor.copy(alpha = glowAlpha))
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("手机号") },
            singleLine = true,
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                cursorColor = accentColor,
                focusedLabelColor = accentColor
            )
        )
    }
}

@Composable
private fun PinInput(
    value: String,
    onValueChange: (String) -> Unit,
    showPin: Boolean,
    onToggleVisibility: () -> Unit,
    accentColor: Color,
    glowColor: Color,
    onDone: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label = "glowAlpha"
    )

    Box {
        if (isFocused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = (-4).dp, vertical = (-4).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(glowColor.copy(alpha = glowAlpha))
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("PIN (至少4位)") },
            singleLine = true,
            interactionSource = interactionSource,
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = onDone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                cursorColor = accentColor,
                focusedLabelColor = accentColor
            ),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPin) "隐藏PIN" else "显示PIN",
                        tint = if (isFocused) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun LoginButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    spec: LoginThemeSpec,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) spec.buttonPressedScale else 1f,
        animationSpec = spec.buttonAnimSpec as? androidx.compose.animation.core.SpringSpec<Float>
            ?: androidx.compose.animation.core.spring(),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(scale)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    awaitPointerEvent()
                    pressed = true
                    awaitPointerEvent()
                    pressed = false
                }
            },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(listOf(spec.buttonStart, spec.buttonEnd)),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
```

Add a `currentThemeFamily()` utility to get current theme family:

Insert into `app/src/main/java/com/diary/app/ui/theme/ThemeMode.kt`:
```kotlin
@Composable
fun currentThemeFamily(): ThemeFamily {
    val themeMode = com.diary.app.ui.theme.LocalThemeMode.current
    return themeMode.family
}
```

This requires `LocalThemeMode` to be provided. Check if it already exists — if not, add:
```kotlin
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.PURE_LIGHT }
```
in `ThemeMode.kt` and set it in `DiaryAppTheme` via `CompositionLocalProvider`.

### Task 3: Build & Test

- [ ] **Build APK**

Run: `.\gradlew.bat :app:assembleExperimentalDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Commit**

```bash
git add -A
git commit -m "feat: login UI upgrade with 7 theme adaptation and micro-interactions"
```
