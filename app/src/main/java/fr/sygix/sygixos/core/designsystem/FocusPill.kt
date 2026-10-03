/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Immutable
data class PillColors(
    val container: Color,
    val content: Color,
    val secondary: Color,
    val tertiary: Color,
    val lifted: Boolean,
)

fun pillColors(focused: Boolean, selected: Boolean = false, rest: Color = Color.Transparent): PillColors = when {
    focused -> PillColors(SygixColors.PillFocus, SygixColors.OnPill, SygixColors.OnPillSecondary, SygixColors.OnPillTertiary, lifted = true)
    selected -> PillColors(SygixColors.PillSelected, SygixColors.OnDark, SygixColors.OnDarkSecondary, SygixColors.OnDarkSecondary, lifted = false)
    else -> PillColors(rest, SygixColors.OnDark, SygixColors.OnDarkSecondary, SygixColors.OnDarkSecondary, lifted = false)
}

@Composable
fun animatedPillColors(focused: Boolean, selected: Boolean = false, rest: Color = Color.Transparent): PillColors {
    val target = pillColors(focused, selected, rest)
    val spec = tween<Color>(Motion.FOCUS_MS, easing = AppleEasing)
    val container by animateColorAsState(target.container, spec, label = "pillContainer")
    val content by animateColorAsState(target.content, spec, label = "pillContent")
    val secondary by animateColorAsState(target.secondary, spec, label = "pillSecondary")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "pillTertiary")
    return PillColors(container, content, secondary, tertiary, target.lifted)
}

fun Modifier.focusPill(colors: PillColors, shape: Shape): Modifier = this
    .then(
        if (colors.lifted) {
            Modifier.dropShadow(shape, Shadow(radius = Dimens.PillShadowRadius, color = SygixColors.PillShadow, offset = DpOffset(0.dp, Dimens.PillShadowOffset)))
        } else {
            Modifier
        },
    )
    .background(colors.container, shape)
