package com.diary.app.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.WarningColor
import kotlinx.coroutines.delay

@Composable
fun PinEntryScreen(
    title: String = "输入PIN码",
    subtitle: String = "",
    hint: String = "",
    onPinEntered: (String) -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    lockoutSeconds: Int = 0
) {
    var pin by remember { mutableStateOf("") }
    var errorShake by remember { mutableIntStateOf(0) }
    var showError by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var isLockedOut by remember { mutableStateOf(lockoutSeconds > 0) }
    var remainingSeconds by remember { mutableIntStateOf(lockoutSeconds) }
    val maxDigits = 4

    // Lockout countdown
    LaunchedEffect(isLockedOut) {
        if (isLockedOut) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            isLockedOut = false
        }
    }

    // Trigger callback when 4 digits entered
    LaunchedEffect(pin) {
        if (pin.length == maxDigits && !isLockedOut) {
            onPinEntered(pin)
            delay(300)
            pin = ""
        }
    }

    val shakeOffset by animateIntAsState(
        targetValue = when (errorShake) {
            0 -> 0
            1 -> -10
            2 -> 10
            3 -> -6
            4 -> 6
            else -> 0
        },
        animationSpec = tween(50),
        finishedListener = {
            if (errorShake in 1..4) errorShake++
            if (errorShake > 4) {
                errorShake = 0
                showError = false
            }
        },
        label = "shake"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock icon with glow effect
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PIN dots with error shake
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(start = shakeOffset.dp)
        ) {
            repeat(maxDigits) { index ->
                val filled = index < pin.length
                val isError = showError && pin.isNotEmpty()
                val dotScale by animateFloatAsState(
                    targetValue = if (filled) 1.1f else 0.85f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "dotScale"
                )

                val dotColor = when {
                    isError -> ErrorColor
                    filled -> Color.White
                    else -> Color.White.copy(alpha = 0.25f)
                }

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        // Error message
        AnimatedVisibility(
            visible = showError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "PIN码错误，请重试",
                fontSize = 13.sp,
                color = ErrorColor,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Lockout message
        AnimatedVisibility(
            visible = isLockedOut,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "已锁定，请等待 ${remainingSeconds}秒",
                fontSize = 13.sp,
                color = WarningColor,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Hint
        if (hint.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "提示：$hint",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Number pad
        val padItems = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("HINT", "0", "DEL")
        )

        padItems.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(vertical = 5.dp)
            ) {
                row.forEach { key ->
                    if (key == "HINT") {
                        // Hint button or empty space
                        if (hint.isNotBlank()) {
                            PinKey(
                                label = "?",
                                icon = Icons.Default.HelpOutline,
                                enabled = !isLockedOut,
                                onClick = { showHint = !showHint }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                    } else {
                        PinKey(
                            label = key,
                            icon = if (key == "DEL") Icons.Default.Backspace else null,
                            enabled = !isLockedOut,
                            onClick = {
                                when (key) {
                                    "DEL" -> {
                                        if (pin.isNotEmpty()) {
                                            pin = pin.dropLast(1)
                                            showError = false
                                        }
                                    }
                                    else -> {
                                        if (pin.length < maxDigits && !isLockedOut) {
                                            pin += key
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Biometric button
        if (onBiometricClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "使用生物识别",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onBiometricClick() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PinKey(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = tween(80),
        label = "keyScale"
    )

    val alpha = if (enabled) 1f else 0.3f

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (pressed) 0.2f else 0.1f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = 0.7f * alpha),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )
        }
    }

    // Reset pressed state
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}
