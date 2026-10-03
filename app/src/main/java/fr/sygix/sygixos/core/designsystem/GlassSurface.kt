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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

val GlassActive = SemanticsPropertyKey<Boolean>("GlassActive")
var SemanticsPropertyReceiver.glassActive by GlassActive

@Immutable
data class GlassLook(
    val tint: Color,
    val shadow: Color,
    val shadowOffset: Dp,
    val shadowRadius: Dp,
) {
    companion object {
        val Dock = GlassLook(SygixColors.GlassTint, SygixColors.GlassShadow, Dimens.GlassShadowOffset, Dimens.GlassShadowRadius)
        val Capsule = GlassLook(SygixColors.CapsuleTint, SygixColors.CapsuleShadow, Dimens.CapsuleShadowOffset, Dimens.CapsuleShadowRadius)
        val Menu = GlassLook(SygixColors.MenuTint, SygixColors.MenuShadow, Dimens.MenuShadowOffset, Dimens.MenuShadowRadius)
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.DockCorner),
    active: Boolean = true,
    look: GlassLook = GlassLook.Dock,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = LocalHazeState.current
    val glass = haze != null && active
    val style = remember(shape, look.tint) {
        GlassStyle.regular.then {
            shape(shape)
            tint(look.tint)
        }
    }
    Box(
        modifier = modifier
            .semantics { glassActive = glass }
            .dropShadow(shape, Shadow(radius = look.shadowRadius, color = look.shadow, offset = DpOffset(0.dp, look.shadowOffset)))
            .border(Dimens.Hairline, SygixColors.GlassBorder, shape)
            .then(
                if (haze != null && active) {
                    Modifier.hazeGlass(
                        input = HazeInput.Sources(haze),
                        style = style,
                        performanceMode = HazePerformanceMode.Performance,
                    )
                } else {
                    Modifier.background(SygixColors.GlassFallback, shape)
                },
            )
            .glassHighlight(shape, SygixColors.GlassHighlight, SygixColors.GlassLowlight),
        content = content,
    )
}

fun Modifier.glassRim(shape: Shape, fill: Color = SygixColors.ButtonRest): Modifier = this
    .border(Dimens.Hairline, SygixColors.ButtonRim, shape)
    .background(fill, shape)
    .glassHighlight(shape, SygixColors.ButtonHighlight, Color.Transparent)

private fun Modifier.glassHighlight(shape: Shape, top: Color, bottom: Color): Modifier = this
    .innerShadow(shape, Shadow(radius = 0.dp, color = top, offset = DpOffset(0.dp, Dimens.Hairline)))
    .then(
        if (bottom.alpha > 0f) {
            Modifier.innerShadow(shape, Shadow(radius = 0.dp, color = bottom, offset = DpOffset(0.dp, -Dimens.Hairline)))
        } else {
            Modifier
        },
    )
