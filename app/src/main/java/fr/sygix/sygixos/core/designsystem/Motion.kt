/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AppleEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

object Motion {
    const val FOCUS_MS = 300
    const val PAGE_SCROLL_MS = 400
    const val HERO_CROSSFADE_MS = 1_400
    const val HERO_VIDEO_FADE_MS = 700
    const val HERO_DWELL_MS = 12_000L
    const val HERO_KEN_BURNS_MS = 10_000
    const val AMBIENT_PASS_MS = 24_000
    const val HERO_VIDEO_START_TIMEOUT_MS = 12_000L
    const val LONG_PRESS_MS = 450L
    const val SHELF_SCROLL_MS = 400
    const val SHELF_OPEN_DELAY_MS = 3_000L
    const val SHELF_PREPARE_DELAY_MS = 500L
    const val SHELF_EXPAND_MS = 420
    const val SHELF_FADE_MS = 350
    const val SHELF_KEN_BURNS_MS = 16_000
    const val SPLASH_MIN_MS = 600L
    const val SPLASH_VISUAL_CAP_MS = 2_000L
    const val SPLASH_CAP_MS = 5_000L
    const val SPLASH_APPEAR_MS = 300
    const val SPLASH_FADE_MS = 400
    const val SPLASH_FOCUS_WAIT_MS = 400L
}

object Dimens {
    val ScreenMarginH = 48.dp
    val GridTopMargin = 40.dp
    val GridSpacing = 24.dp
    val GridRowSpacing = 32.dp
    val TileCorner = 14.dp
    const val GridColumns = 5
    const val TileFocusScale = 1.08f
    val TileFocusLift = 2.dp
    val TileFocusElevation = 11.dp
    val DockBottomMargin = 24.dp
    val DockPadding = 10.dp
    val DockSpacing = 14.dp
    val DockCorner = 24.dp
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
    val CapsuleHeight = 36.dp
    val GearIcon = 15.dp
    val GearFocusElevation = 6.dp
    val Badge = 6.dp
    val MenuWidth = 300.dp
    val MenuPadding = 14.dp
    val MenuCorner = 26.dp
    val MenuHeaderInsetStart = 6.dp
    val MenuHeaderInsetTop = 2.dp
    val MenuThumbnailWidth = 56.dp
    val MenuThumbnailCorner = 10.dp
    val MenuShadowOffset = 15.dp
    val MenuShadowRadius = 30.dp
    val MenuActionHeight = 38.dp
    val PillCorner = 12.dp
    val PillShadowOffset = 5.dp
    val PillShadowRadius = 12.dp
    val ButtonHeight = 36.dp
    const val ButtonFocusScale = 1.05f
    val ButtonFocusElevation = 8.dp
    const val GearFocusScale = 1.08f
    val HeroTextBottom = 148.dp
    val HeroTextWidth = 450.dp
    val HeroTextSpacing = 10.dp
    val SettingsRowCorner = 14.dp
    val SettingsRowHeight = 48.dp
    val SettingsRowPadding = 14.dp
    val SettingsThumbWidth = 48.dp
    val SettingsThumbHeight = 27.dp
    val SettingsThumbCorner = 3.5.dp
    val ShelfCorner = 16.dp
    val HeroSourceIcon = 18.dp
    val HeroSourceIconCorner = 4.5.dp
    val QrSize = 120.dp
    val SplashMascot = 180.dp

    val Nested: List<NestedCorner> = listOf(
        NestedCorner("dock-tile", outer = DockCorner, inner = TileCorner, margin = DockPadding),
        NestedCorner("capsule-gear", outer = CapsuleHeight / 2, inner = GearButton / 2, margin = CapsulePadding),
        NestedCorner("menu-pill", outer = MenuCorner, inner = PillCorner, margin = MenuPadding),
        NestedCorner("menu-thumbnail", outer = MenuCorner, inner = MenuThumbnailCorner, margin = MenuPadding + MenuHeaderInsetTop),
        NestedCorner("settings-row-thumbnail", outer = SettingsRowCorner, inner = SettingsThumbCorner, margin = (SettingsRowHeight - SettingsThumbHeight) / 2),
    )
}

data class NestedCorner(val name: String, val outer: Dp, val inner: Dp, val margin: Dp)
