/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp

val AppleEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

object Motion {
    const val FOCUS_MS = 300
    const val LAYER_FADE_MS = 400
    const val HERO_CROSSFADE_MS = 1_400
    const val HERO_VIDEO_FADE_MS = 700
    const val HERO_DWELL_MS = 12_000L
    const val HERO_KEN_BURNS_MS = 16_000
    const val HERO_VIDEO_START_TIMEOUT_MS = 12_000L
    const val LONG_PRESS_MS = 450L
    const val SHELF_SCROLL_MS = 400
    const val SHELF_OPEN_DELAY_MS = 3_000L
    const val SHELF_EXPAND_MS = 420
    const val SHELF_FADE_MS = 350
}

object Dimens {
    val ScreenMarginH = 48.dp
    val GridTopMargin = 40.dp
    val GridSpacing = 16.dp
    val GridRowSpacing = 20.dp
    val TileCorner = 12.dp
    const val GridColumns = 5
    val DockBottomMargin = 24.dp
    val DockPadding = 14.dp
    val DockOuterMargin = ScreenMarginH - DockPadding
    const val ShelfAspectRatio = 1920f / 720f
    val ShelfPeek = 40.dp
    val GearSize = 36.dp
}
