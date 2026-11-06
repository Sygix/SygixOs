/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Immutable
data class PillColors(
    val container: Color,
    val content: Color,
    val secondary: Color,
    val tertiary: Color,
)

fun pillColors(
    focus: Float,
    selection: Float = 0f,
    rest: Color = Color.Transparent,
    restContent: Color = SygixColors.OnDark,
): PillColors = PillColors(
    container = pillContainer(focus, selection, rest),
    content = pillContent(focus, selection, restContent),
    secondary = pillSecondary(focus),
    tertiary = pillTertiary(focus),
)

private fun pillContainer(focus: Float, selection: Float, rest: Color) = blend(blend(rest, SygixColors.PillSelected, selection), SygixColors.PillFocus, focus)

private fun pillContent(focus: Float, selection: Float, rest: Color) = blend(blend(rest, SygixColors.OnDark, selection), SygixColors.OnPill, focus)

private fun pillSecondary(focus: Float) = blend(SygixColors.OnDarkSecondary, SygixColors.OnPillSecondary, focus)

private fun pillTertiary(focus: Float) = blend(SygixColors.OnDarkSecondary, SygixColors.OnPillTertiary, focus)

private fun blend(from: Color, to: Color, fraction: Float): Color = when {
    fraction <= 0f -> from
    fraction >= 1f -> to
    else -> lerp(from, to, fraction)
}

@Stable
class PillPaint internal constructor(focused: Boolean, selected: Boolean, private val rest: Color, private val restContent: Color) {
    internal val focus = Animatable(if (focused) 1f else 0f)
    internal val selection = Animatable(if (selected) 1f else 0f)

    val container = ColorProducer { pillContainer(focus.value, selection.value, rest) }
    val content = ColorProducer { pillContent(focus.value, selection.value, restContent) }
    val secondary = ColorProducer { pillSecondary(focus.value) }
    val tertiary = ColorProducer { pillTertiary(focus.value) }

    val emphasized by derivedStateOf { focus.value >= 0.5f }

    val lift: Float get() = focus.value
}

@Composable
fun rememberPillPaint(
    focused: Boolean,
    selected: Boolean = false,
    rest: Color = Color.Transparent,
    restContent: Color = SygixColors.OnDark,
): PillPaint {
    val navigation = LocalFastNavigation.current
    val paint = remember(rest, restContent) { PillPaint(focused, selected, rest, restContent) }
    LaunchedEffect(paint, focused) { paint.focus.focusTo(if (focused) 1f else 0f, navigation.active) }
    LaunchedEffect(paint, selected) { paint.selection.focusTo(if (selected) 1f else 0f, navigation.active) }
    return paint
}

private val PillShadow = Shadow(radius = Dimens.PillShadowRadius, color = SygixColors.PillShadow, offset = DpOffset(0.dp, Dimens.PillShadowOffset))

fun Modifier.focusPill(paint: PillPaint, shape: Shape): Modifier = this
    .fadingDropShadow(shape, PillShadow, paint::lift)
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawOutline(outline, paint.container()) }
    }

@Composable
fun FocusText(
    text: String,
    style: TextStyle,
    color: ColorProducer,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    BasicText(text, modifier, style = style, overflow = overflow, softWrap = softWrap, maxLines = maxLines, color = color)
}
