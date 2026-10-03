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
    const val PAGE_SCROLL_MS = 400
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
    val GridSpacing = 24.dp
    val GridRowSpacing = 32.dp
    val TileCorner = 9.dp
    const val GridColumns = 5
    const val TileFocusScale = 1.08f
    val TileFocusLift = 2.dp
    val TileFocusElevation = 11.dp
    val TileNameGap = 11.5.dp
    val DockBottomMargin = 24.dp
    val DockPadding = 10.dp
    val DockSpacing = 14.dp
    val DockCorner = 20.dp
    val DockTileWidth = 120.dp
    const val ShelfAspectRatio = 1920f / 720f
    val ShelfPeek = 40.dp
    val Hairline = 0.5.dp
    val GlassShadowOffset = 6.dp
    val GlassShadowRadius = 16.dp
    val CapsuleShadowOffset = 4.dp
    val CapsuleShadowRadius = 12.dp
    val CapsuleTop = 24.dp
    val CapsuleEnd = 32.dp
    val CapsulePaddingStart = 13.dp
    val CapsulePadding = 4.dp
    val CapsuleSpacing = 9.dp
    val GearButton = 28.dp
    val GearIcon = 15.dp
    val GearFocusElevation = 6.dp
    val MenuWidth = 300.dp
    val MenuPadding = 14.dp
    val MenuCorner = 18.dp
    val MenuShadowOffset = 15.dp
    val MenuShadowRadius = 30.dp
    val MenuActionHeight = 38.dp
    val PillCorner = 9.dp
    val PillShadowOffset = 5.dp
    val PillShadowRadius = 12.dp
    val ButtonHeight = 36.dp
    val ButtonShadowOffset = 7.dp
    val ButtonShadowRadius = 16.dp
    const val ButtonFocusScale = 1.05f
    const val GearFocusScale = 1.08f
    val HeroTextBottom = 148.dp
    val HeroTextWidth = 450.dp
    val HeroTextSpacing = 10.dp
    val SettingsRowCorner = 8.dp
}
