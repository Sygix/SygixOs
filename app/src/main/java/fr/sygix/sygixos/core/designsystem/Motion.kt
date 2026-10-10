/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
    const val HERO_VISUAL_TIMEOUT_MS = 1_000L
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
    val SectionTitleGap = 10.dp
    val UpNextBadgeInset = 8.dp
    val UpNextBadgeCorner = 6.dp
    val UpNextTextInset = 12.dp
    val UpNextTextBottom = 11.dp
    val UpNextTextAboveProgress = 7.dp
    val UpNextPlaceholderIcon = 32.dp
    val UpNextErrorIcon = 20.dp
    val UpNextSkeletonTitle = DpSize(110.dp, 12.dp)
    val UpNextSkeletonSubtitle = DpSize(75.dp, 9.dp)
    const val CardScrimStart = 0.35f
    val ProgressHeight = 3.dp
    val MenuEntryPadding = 10.dp
    val MenuEntryIcon = 22.dp
    val MenuEntryIconCorner = 4.dp
    val MenuEntryGap = 10.dp
    val SettingsChevron = 14.dp
    val SettingsChevronGap = 6.dp
    val DropdownWidth = 240.dp
    val DropdownCorner = 16.dp
    val DropdownPadding = 6.dp
    val DropdownOptionGap = 2.dp
    val DropdownOffset = 4.dp
    val DropdownInsetEnd = 6.dp
    val DropdownOptionHeight = 32.dp
    val DropdownOptionCorner = 10.dp
    val DropdownOptionPaddingStart = 8.dp
    val DropdownOptionPaddingEnd = 12.dp
    val DropdownCheck = 14.dp
    val DropdownCheckGap = 6.dp
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
    val SettingsGroupTitleTop = 19.5.dp
    val SettingsGroupTitleBottom = 8.5.dp
    val StateDotGap = 5.dp
    val OnboardingWidth = 564.dp
    val OnboardingCorner = 28.dp
    val OnboardingPadding = 14.dp
    val OnboardingSpacing = 4.dp
    val OnboardingHeaderInsetH = 14.dp
    val OnboardingHeaderInsetTop = 14.dp
    val OnboardingHeaderInsetBottom = 12.dp
    val OnboardingHeaderSpacing = 6.dp
    val OnboardingMascot = 64.dp
    val OnboardingFooterTop = 10.dp
    val OnboardingButtonPaddingH = 20.dp
    val SettingsRowCorner = 14.dp
    val SettingsRowHeight = 48.dp
    val SettingsRowPadding = 14.dp
    val SettingsHeaderGap = 37.dp
    val SettingsThumbWidth = 48.dp
    val SettingsThumbHeight = 27.dp
    val SettingsThumbCorner = 3.5.dp
    val ShelfCorner = 16.dp
    val HeroSourceIcon = 18.dp
    val HeroSourceIconCorner = 4.5.dp
    val QrSize = 120.dp
    val SplashMascot = 180.dp

    val Nested: List<NestedCorner> = listOf(
        NestedCorner("dropdown-option", outer = DropdownCorner, inner = DropdownOptionCorner, margin = DropdownPadding),
        NestedCorner("card-badge", outer = TileCorner, inner = UpNextBadgeCorner, margin = UpNextBadgeInset),
        NestedCorner("menu-entry-icon", outer = PillCorner, inner = MenuEntryIconCorner, margin = (MenuActionHeight - MenuEntryIcon) / 2),
        NestedCorner("dock-tile", outer = DockCorner, inner = TileCorner, margin = DockPadding),
        NestedCorner("capsule-gear", outer = CapsuleHeight / 2, inner = GearButton / 2, margin = CapsulePadding),
        NestedCorner("menu-pill", outer = MenuCorner, inner = PillCorner, margin = MenuPadding),
        NestedCorner("menu-thumbnail", outer = MenuCorner, inner = MenuThumbnailCorner, margin = MenuPadding + MenuHeaderInsetTop),
        NestedCorner("settings-row-thumbnail", outer = SettingsRowCorner, inner = SettingsThumbCorner, margin = (SettingsRowHeight - SettingsThumbHeight) / 2),
        NestedCorner("onboarding-row", outer = OnboardingCorner, inner = SettingsRowCorner, margin = OnboardingPadding),
    )
}

data class NestedCorner(val name: String, val outer: Dp, val inner: Dp, val margin: Dp)
