/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

val GlassActive = SemanticsPropertyKey<Boolean>("GlassActive")
var SemanticsPropertyReceiver.glassActive by GlassActive

val GlassPrecomputed = SemanticsPropertyKey<Boolean>("GlassPrecomputed")
var SemanticsPropertyReceiver.glassPrecomputed by GlassPrecomputed

@Immutable
data class GlassLook(
    val tint: Color,
    val shadow: Color,
    val shadowOffset: Dp,
    val shadowRadius: Dp,
    val fallback: Color = SygixColors.GlassFallback,
    val highlight: Color = SygixColors.GlassHighlight,
    val lowlight: Color = SygixColors.GlassLowlight,
) {
    companion object {
        val Dock = GlassLook(SygixColors.GlassTint, SygixColors.GlassShadow, Dimens.GlassShadowOffset, Dimens.GlassShadowRadius)
        val Capsule = GlassLook(SygixColors.CapsuleTint, SygixColors.CapsuleShadow, Dimens.CapsuleShadowOffset, Dimens.CapsuleShadowRadius)
        val Menu = GlassLook(SygixColors.MenuTint, SygixColors.MenuShadow, Dimens.MenuShadowOffset, Dimens.MenuShadowRadius, SygixColors.MenuFallback, SygixColors.MenuHighlight, Color.Transparent)
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.DockCorner),
    active: Boolean = true,
    look: GlassLook = GlassLook.Dock,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = LocalHazeState.current
    val backdrop = LocalGlassBackdrop.current?.takeIf { it.ready }
    val anchor = remember { GlassAnchor() }
    val glass = (haze != null || backdrop != null) && active
    val style = remember(shape, look.tint) {
        GlassStyle.regular.then {
            shape(shape)
            tint(look.tint)
        }
    }
    Box(
        modifier = modifier
            .semantics {
                glassActive = glass
                glassPrecomputed = backdrop != null && active
            }
            .dropShadow(shape, Shadow(radius = look.shadowRadius, color = look.shadow, offset = DpOffset(0.dp, look.shadowOffset)))
            .border(Dimens.Hairline, SygixColors.GlassBorder, shape)
            .then(
                if (backdrop != null && active) {
                    Modifier.backdropGlass(backdrop, anchor, shape, look.tint, look.fallback)
                } else if (haze != null && active) {
                    Modifier.hazeGlass(
                        input = HazeInput.Sources(haze),
                        style = style,
                        performanceMode = HazePerformanceMode.Performance,
                    )
                } else {
                    Modifier.background(look.fallback, shape)
                },
            )
            .glassHighlight(shape, look.highlight, look.lowlight),
        content = content,
    )
}

fun Modifier.glassRim(shape: Shape, focus: () -> Float = { 0f }): Modifier = this
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val rim = Stroke(Dimens.Hairline.toPx())
        onDrawBehind {
            val p = focus()
            drawOutline(outline, lerp(SygixColors.ButtonRest, SygixColors.OnDark, p))
            drawOutline(outline, lerp(SygixColors.ButtonRim, SygixColors.ButtonFocusRim, p), style = rim)
        }
    }
    .innerShadow(shape) {
        radius = 0f
        color = SygixColors.ButtonHighlight
        alpha = 1f - focus()
        offset = Offset(0f, Dimens.Hairline.toPx())
    }

private fun Modifier.glassHighlight(shape: Shape, top: Color, bottom: Color): Modifier = this
    .innerShadow(shape, Shadow(radius = 0.dp, color = top, offset = DpOffset(0.dp, Dimens.Hairline)))
    .then(
        if (bottom.alpha > 0f) {
            Modifier.innerShadow(shape, Shadow(radius = 0.dp, color = bottom, offset = DpOffset(0.dp, -Dimens.Hairline)))
        } else {
            Modifier
        },
    )
