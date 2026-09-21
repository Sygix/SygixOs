package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val FOCUS_SCALE = 1.1f

/**
 * Focus tvOS : zoom 1.1x, ombre douce, courbe Apple. À placer AVANT la cible
 * de focus (clickable/focusable) qui doit suivre dans la chaîne. `enabled`
 * retire l'élément de la recherche de focus quand sa couche n'est pas active.
 */
fun Modifier.tvFocus(
    onFocused: (Boolean) -> Unit = {},
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) FOCUS_SCALE else 1f,
        animationSpec = tween(Motion.FOCUS_MS, easing = AppleEasing),
        label = "focusScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (focused) 16.dp else 0.dp,
        animationSpec = tween(Motion.FOCUS_MS, easing = AppleEasing),
        label = "focusShadow",
    )
    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .focusProperties { canFocus = enabled }
        .onFocusChanged { focused = it.isFocused; onFocused(it.isFocused) }
        .scale(scale)
        .shadow(elevation, RoundedCornerShape(Dimens.TileCorner), clip = false, ambientColor = Color.Black, spotColor = Color.Black)
}

/** requestFocus() lève si le nœud n'est pas encore attaché : on renvoie false plutôt que de planter. */
fun FocusRequester.tryRequestFocus(): Boolean = runCatching { requestFocus(); true }.getOrDefault(false)
