package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FOCUS_SCALE = 1.07f
private const val GLOW_ALPHA = 0.5f
private const val GLOW_SPREAD = 0.14f

fun Modifier.tvFocus(
    onFocused: (Boolean) -> Unit = {},
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    glow: Color = Color.White,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(Motion.FOCUS_MS, easing = AppleEasing),
        label = "focusProgress",
    )
    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .focusProperties { canFocus = enabled }
        .onFocusChanged { focused = it.isFocused; onFocused(it.isFocused) }
        .scale(1f + (FOCUS_SCALE - 1f) * progress)
        .drawBehind {
            if (progress > 0.01f) {
                val spread = size.width * GLOW_SPREAD
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = GLOW_ALPHA * progress), glow.copy(alpha = 0f)),
                        center = center,
                        radius = size.width * 0.5f + spread,
                    ),
                    topLeft = Offset(-spread, -spread),
                    size = Size(size.width + spread * 2, size.height + spread * 2),
                )
            }
        }
}

fun Modifier.tvClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    var pressJob by remember { mutableStateOf<Job?>(null) }
    var longFired by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    this
        .onKeyEvent { e ->
            val select = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
            if (!select) return@onKeyEvent false
            when (e.type) {
                KeyEventType.KeyDown -> {
                    val native = e.nativeKeyEvent
                    if (native.repeatCount == 0) {
                        pressed = true
                        longFired = false
                        pressJob?.cancel()
                        pressJob = longClick?.let { long ->
                            scope.launch {
                                delay(Motion.LONG_PRESS_MS)
                                longFired = true
                                long()
                            }
                        }
                    } else if (native.isLongPress && !longFired && longClick != null) {
                        pressJob?.cancel()
                        longFired = true
                        longClick?.invoke()
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    if (!pressed) return@onKeyEvent false
                    pressed = false
                    pressJob?.cancel()
                    if (!longFired) click()
                    longFired = false
                    true
                }
                else -> false
            }
        }
        .onFocusChanged { if (!it.isFocused) { pressJob?.cancel(); longFired = false; pressed = false } }
        .focusable()
}

fun FocusRequester.tryRequestFocus(): Boolean = runCatching { requestFocus(); true }.getOrDefault(false)
