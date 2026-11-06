/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusPillTest {

    @Test
    fun `focused pill is light with dark content`() {
        val colors = pillColors(focus = 1f, selection = 1f, rest = SygixColors.PillRest)
        assertEquals(SygixColors.PillFocus, colors.container)
        assertEquals(SygixColors.OnPill, colors.content)
        assertEquals(SygixColors.OnPillSecondary, colors.secondary)
        assertEquals(SygixColors.OnPillTertiary, colors.tertiary)
    }

    @Test
    fun `selected pill without focus is a discreet grey with white content`() {
        val colors = pillColors(focus = 0f, selection = 1f)
        assertEquals(SygixColors.PillSelected, colors.container)
        assertEquals(Color.White, colors.content)
    }

    @Test
    fun `resting pill keeps its rest color and white content`() {
        assertEquals(Color.Transparent, pillColors(focus = 0f).container)
        val unhideAll = pillColors(focus = 0f, rest = SygixColors.PillRest)
        assertEquals(SygixColors.PillRest, unhideAll.container)
        assertEquals(Color.White, unhideAll.content)
    }
}
