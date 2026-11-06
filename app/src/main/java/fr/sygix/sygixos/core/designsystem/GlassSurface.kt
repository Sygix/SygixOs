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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

val GlassActive = SemanticsPropertyKey<Boolean>("GlassActive")
var SemanticsPropertyReceiver.glassActive by GlassActive

internal const val GlassMaterialTag = "glass-material"

private val GlassBlurRadius = 28.dp
private const val GlassSaturation = 1.7f

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

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.DockCorner),
    active: Boolean = true,
    look: GlassLook = GlassLook.Dock,
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = LocalHazeState.current
    val backdrop = LocalGlassBackdrop.current?.takeIf { it.ready }
    val anchor = remember { GlassAnchor() }
    val glass = (haze != null || backdrop != null) && active
    val style = remember(look.tint, look.fallback) { frostedStyle(look.tint, look.fallback) }
    Box(
        modifier = modifier
            .semantics { glassActive = glass }
            .then(if (shadow) Modifier.dropShadow(shape, Shadow(radius = look.shadowRadius, color = look.shadow, offset = DpOffset(0.dp, look.shadowOffset))) else Modifier)
            .border(Dimens.Hairline, SygixColors.GlassBorder, shape),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .testTag(GlassMaterialTag)
                .then(
                    if (backdrop != null && active) {
                        Modifier.backdropGlass(backdrop, anchor, shape, look.tint, look.fallback)
                    } else if (haze != null && active) {
                        Modifier
                            .clip(shape)
                            .hazeBlur(HazeInput.Sources(haze), style, HazePerformanceMode.Performance)
                    } else {
                        Modifier.background(look.fallback, shape)
                    },
                )
                .glassHighlight(shape, look.highlight, look.lowlight),
        )
        content()
    }
}

private fun frostedStyle(tint: Color, fallback: Color): HazeBlurStyle = HazeBlurStyle {
    blurRadius(GlassBlurRadius)
    noiseFactor(0f)
    colorEffects(
        listOf(
            HazeColorEffect.colorFilter(ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GlassSaturation) })),
            HazeColorEffect.tint(tint),
        ),
    )
    fallbackColorEffect(HazeColorEffect.tint(fallback))
}

internal fun Modifier.glassButton(
    shape: Shape,
    backdrop: GlassBackdrop?,
    anchor: GlassAnchor,
    focus: () -> Float,
): Modifier = this
    .semantics { glassActive = backdrop != null }
    .then(
        if (backdrop != null) {
            Modifier.backdropGlass(backdrop, anchor, shape, SygixColors.GlassTint, SygixColors.ButtonRest)
        } else {
            Modifier.background(SygixColors.ButtonRest, shape)
        },
    )
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val rim = Stroke(Dimens.Hairline.toPx())
        onDrawBehind {
            val p = focus()
            if (p > 0f) drawOutline(outline, SygixColors.OnDark.copy(alpha = p))
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
