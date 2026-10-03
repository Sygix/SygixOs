/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusPillTest {

    @Test
    fun `focused pill is light with dark content and lifted`() {
        val colors = pillColors(focused = true, selected = true, rest = SygixColors.PillRest)
        assertEquals(SygixColors.PillFocus, colors.container)
        assertEquals(SygixColors.OnPill, colors.content)
        assertEquals(SygixColors.OnPillSecondary, colors.secondary)
        assertTrue(colors.lifted)
    }

    @Test
    fun `selected pill without focus is a discreet grey with white content`() {
        val colors = pillColors(focused = false, selected = true)
        assertEquals(SygixColors.PillSelected, colors.container)
        assertEquals(Color.White, colors.content)
        assertFalse(colors.lifted)
    }

    @Test
    fun `resting pill keeps its rest color and white content`() {
        assertEquals(Color.Transparent, pillColors(focused = false).container)
        val unhideAll = pillColors(focused = false, rest = SygixColors.PillRest)
        assertEquals(SygixColors.PillRest, unhideAll.container)
        assertEquals(Color.White, unhideAll.content)
        assertFalse(unhideAll.lifted)
    }
}
