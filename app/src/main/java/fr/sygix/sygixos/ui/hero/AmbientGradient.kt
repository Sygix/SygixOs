package fr.sygix.sygixos.ui.hero

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val Colors = listOf(Color(0xFF0E1A33), Color(0xFF101014), Color(0xFF1B1030))

@Composable
fun AmbientGradient(modifier: Modifier = Modifier, animated: Boolean = false) {
    val phase = if (animated) {
        rememberInfiniteTransition(label = "ambient").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "ambientPhase",
        ).value
    } else {
        0.35f
    }
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = Colors,
                        start = Offset(size.width * (0.2f * phase), 0f),
                        end = Offset(size.width, size.height * (1f - 0.3f * phase)),
                    ),
                )
            },
    )
}
