package fr.sygix.sygixos.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/** État Haze partagé : les couches de fond s'y enregistrent, les surfaces verre les réfractent. Null = repli translucide. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

private val FallbackGlass = Color(0xCC17171E)
private val GlassBackdrop = Color(0xFF0B0B10)

/**
 * Liquid Glass : matériau verre de Haze 2 (réfraction des bords, reflet spéculaire,
 * légère aberration chromatique) au-dessus des couches enregistrées comme sources.
 * Sans état Haze ou quand la surface est invisible, repli translucide sans flou.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    /** false quand la surface est invisible : ni verre ni animation (économie GPU). */
    active: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = LocalHazeState.current
    if (haze != null && active) {
        Box(
            modifier = modifier.hazeGlass(
                input = HazeInput.Sources(haze),
                style = GlassStyle.regular.then {
                    shape(shape)
                    backgroundColor(GlassBackdrop)
                    tint(Color.White.copy(alpha = 0.08f))
                    specularIntensity(0.7f)
                    edgeSoftness(2.dp)
                    chromaticAberrationStrength(0.06f)
                    alpha(0.96f)
                },
            ),
            content = content,
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(FallbackGlass)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))),
                    shape,
                ),
            content = content,
        )
    }
}
