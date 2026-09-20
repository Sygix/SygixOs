package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val FOCUS_SCALE = 1.1f
private const val FOCUS_ANIM_MS = 300

fun Modifier.tvFocus(onFocused: (Boolean) -> Unit = {}): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) FOCUS_SCALE else 1f,
        animationSpec = tween(FOCUS_ANIM_MS),
        label = "focusScale",
    )
    this
        .onFocusChanged { focused = it.isFocused; onFocused(it.isFocused) }
        .scale(scale)
        .padding(if (focused) 0.dp else 6.dp)
        .shadow(if (focused) 16.dp else 0.dp, ambientColor = androidx.compose.ui.graphics.Color.Black)
}
