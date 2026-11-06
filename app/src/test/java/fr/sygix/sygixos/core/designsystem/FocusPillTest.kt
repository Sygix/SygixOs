/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.test.runTest
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

    @Test
    fun `the pill shadow is built once and only faded by the focus progress`() = runTest {
        val paint = PillPaint(focused = false, selected = false, rest = Color.Transparent, restContent = SygixColors.OnDark)
        val shape = RoundedCornerShape(percent = 50)
        fun shadow() = Modifier.focusPill(paint, shape).foldIn(emptyList<Modifier.Element>()) { elements, element -> elements + element }.first()
        val rest = shadow()
        paint.focus.snapTo(0.5f)
        assertEquals(rest, shadow())
        paint.focus.snapTo(1f)
        assertEquals(rest, shadow())
    }
}
