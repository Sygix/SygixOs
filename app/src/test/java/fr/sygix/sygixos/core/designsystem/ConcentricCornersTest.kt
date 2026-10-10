/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcentricCornersTest {

    @Test
    fun `every nested corner is the inner corner plus the margin between both edges`() {
        assertTrue(Dimens.Nested.isNotEmpty())
        Dimens.Nested.forEach { nested ->
            assertEquals(nested.name, (nested.inner + nested.margin).value, nested.outer.value, 0.001f)
            assertTrue(nested.name, nested.inner.value >= 0f)
        }
    }

    @Test
    fun `declared pairs cover every nesting of the home and settings`() {
        assertEquals(
            setOf("dock-tile", "capsule-gear", "menu-pill", "menu-thumbnail", "settings-row-thumbnail", "dropdown-option", "card-badge", "menu-entry-icon"),
            Dimens.Nested.map { it.name }.toSet(),
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
        val capsule = Dimens.Nested.first { it.name == "capsule-gear" }
        assertEquals(Dimens.CapsuleHeight / 2, capsule.outer)
        assertEquals(Dimens.GearButton / 2, capsule.inner)
    }
}
