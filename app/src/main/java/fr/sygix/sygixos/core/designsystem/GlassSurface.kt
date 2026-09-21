package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** État Haze partagé : les couches de fond s'y enregistrent, les surfaces verre le floutent. Null = repli translucide sans flou. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

private val FallbackGlass = Color(0xCC17171E)
private val GlassBackdrop = Color(0xFF0B0B10)

/**
 * Liquid Glass : flou d'arrière-plan réel (RenderEffect), teinte claire, grain,
 * liseré spéculaire et reflet lent qui balaie la surface.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = LocalHazeState.current
    val sheen by rememberInfiniteTransition(label = "glassSheen").animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart),
        label = "glassSheenX",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (haze != null) {
                    Modifier.hazeEffect(state = haze) {
                        backgroundColor = GlassBackdrop
                        blurRadius = 28.dp
                        noiseFactor = 0.04f
                        tints = listOf(HazeTint(Color.White.copy(alpha = 0.12f)))
                        fallbackTint = HazeTint(FallbackGlass)
                        inputScale = HazeInputScale.Auto
                    }
                } else {
                    Modifier.background(FallbackGlass)
                },
            )
            .drawWithContent {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        endY = size.height * 0.5f,
                    ),
                )
                val x = sheen * size.width
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.09f), Color.Transparent),
                        start = Offset(x - size.height, 0f),
                        end = Offset(x + size.height, size.height),
                    ),
                )
                drawContent()
            }
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f))),
                shape,
            ),
        content = content,
    )
}
