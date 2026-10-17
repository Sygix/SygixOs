/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal data class NestedCorner(val name: String, val outer: Dp, val inner: Dp, val margin: Dp)

internal val NestedCorners: List<NestedCorner> = with(Dimens) {
    listOf(
        NestedCorner("dropdown-option", outer = DropdownCorner, inner = DropdownOptionCorner, margin = DropdownPadding),
        NestedCorner("card-badge", outer = TileCorner, inner = UpNextBadgeCorner, margin = UpNextBadgeInset),
        NestedCorner("menu-entry-icon", outer = PillCorner, inner = MenuEntryIconCorner, margin = (MenuActionHeight - MenuEntryIcon) / 2),
        NestedCorner("dock-tile", outer = DockCorner, inner = TileCorner, margin = DockPadding),
        NestedCorner("capsule-gear", outer = CapsuleHeight / 2, inner = GearButton / 2, margin = CapsulePadding),
        NestedCorner("menu-pill", outer = MenuCorner, inner = PillCorner, margin = MenuPadding),
        NestedCorner("menu-thumbnail", outer = MenuCorner, inner = MenuThumbnailCorner, margin = MenuPadding + MenuHeaderInsetTop),
        NestedCorner("settings-row-thumbnail", outer = SettingsRowCorner, inner = SettingsThumbCorner, margin = (SettingsRowHeight - SettingsThumbHeight) / 2),
        NestedCorner("setup-row", outer = OnboardingCorner, inner = SettingsRowCorner, margin = OnboardingPadding),
    )
}

class ConcentricCornersTest {

    @Test
    fun `every nested corner is the inner corner plus the margin between both edges`() {
        assertTrue(NestedCorners.isNotEmpty())
        NestedCorners.forEach { nested ->
            assertEquals(nested.name, (nested.inner + nested.margin).value, nested.outer.value, 0.001f)
            assertTrue(nested.name, nested.inner.value >= 0f)
        }
    }

    @Test
    fun `declared pairs cover every nesting of the home and settings`() {
        assertEquals(
            setOf("dock-tile", "capsule-gear", "menu-pill", "menu-thumbnail", "settings-row-thumbnail", "dropdown-option", "card-badge", "menu-entry-icon", "setup-row"),
            NestedCorners.map { it.name }.toSet(),
        )
    }

    @Test
    fun `validated corners are the owner's values`() {
        assertEquals(14.dp, Dimens.TileCorner)
        assertEquals(24.dp, Dimens.DockCorner)
        assertEquals(10.dp, Dimens.DockPadding)
        assertEquals(26.dp, Dimens.MenuCorner)
        assertEquals(12.dp, Dimens.PillCorner)
        assertEquals(14.dp, Dimens.MenuPadding)
        assertEquals(14.dp, Dimens.SettingsRowCorner)
    }

    @Test
    fun `capsule and gear are pills`() {
        assertEquals(Dimens.GearButton + Dimens.CapsulePadding * 2, Dimens.CapsuleHeight)
        val capsule = NestedCorners.first { it.name == "capsule-gear" }
        assertEquals(Dimens.CapsuleHeight / 2, capsule.outer)
        assertEquals(Dimens.GearButton / 2, capsule.inner)
    }
}
